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

import java.util.Arrays;

import jparsec.util.*;
import jparsec.math.*;
import jparsec.io.image.ImageSplineTransform;

import java.awt.*;
import java.io.Serializable;

/**
 * Creates a grid chart element to be later drawn by NOAA SGT graphic library, or SurfacePlotter.<P>
 *
 * In this object the labels for x, y and z axes, as well as the title can be
 * encoded following the instructions given in {@linkplain TextLabel} class. This
 * provides some possibilities like to change color/size, include superscript
 * or subscript text, or greek letters.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class GridChartElement implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Default empty constructor.
	 */
	public GridChartElement()	{	}

	/**
	 * Simple constructor for a raster contour chart.
	 *
	 * @param title Title.
	 * @param x_label X axis label.
	 * @param y_label Y axis label
	 * @param legend The legend.
	 * @param colorModel Color model ID.
	 * @param limits The limits for the x and y axes.
	 * @param data The data.
	 * @param levels The levels to show as contours.
	 * @param width Chart width. Height is set automatically.
	 * @throws JPARSECException If an error occurs.
	 */
	public GridChartElement(String title, String x_label,
			String y_label, String legend, COLOR_MODEL colorModel,
			double[] limits, Double data[][], double levels[], int width)
	throws JPARSECException {
		this.xLabel = x_label;
		this.yLabel = y_label;
		this.title = title;
		this.subTitle = "";
		if (width <= 100) throw new JPARSECException("size cannot be below 100 pixels.");
		this.imageWidth = width;
		this.imageHeight = width;
		this.legend = legend;
		this.colorModel = colorModel;
		this.setColorModel();
		this.limits = limits;
		this.levels = levels;
		this.type = GridChartElement.TYPE.RASTER;
		if (levels != null) type = TYPE.RASTER_CONTOUR;
		this.data = data;
		ocultLevels = false;
		levelsOrientation = WEDGE_ORIENTATION.VERTICAL_RIGHT;
		levelsBorderStyle = GridChartElement.WEDGE_BORDER.NO_BORDER;
	}

	/**
	 * Full constructor.
	 *
	 * @param title Title.
	 * @param x_label X axis label.
	 * @param y_label Y axis label
	 * @param legend The legend.
	 * @param colorModel Color model ID.
	 * @param colorResolution Color model resolution.
	 * @param type Chart type.
	 * @param limits The limits for the different axes.
	 * @param data The data.
	 * @param levels The levels to show as contours.
	 * @param width Chart width. Height is set automatically.
	 * @throws JPARSECException If an error occurs.
	 */
	public GridChartElement(String title, String x_label,
			String y_label, String legend, COLOR_MODEL colorModel,
			int colorResolution, TYPE type,
			double[] limits, Double data[][], double levels[], int width)
	throws JPARSECException {
		this.xLabel = x_label;
		this.yLabel = y_label;
		this.title = title;
		this.subTitle = "";
		if (width <= 100) throw new JPARSECException("size cannot be below 100 pixels.");
		this.imageWidth = width;
		this.imageHeight = width;
		this.legend = legend;
		this.colorModel = colorModel;
		this.colorModelResolution = colorResolution;
		this.setColorModel();
		this.limits = limits;
		this.levels = levels;
		this.type = type;
		this.data = data;
		ocultLevels = false;
		levelsOrientation = WEDGE_ORIENTATION.VERTICAL_RIGHT;
		levelsBorderStyle = GridChartElement.WEDGE_BORDER.NO_BORDER;
	}

	/**
	 * Simple constructor for a raster contour chart.
	 *
	 * @param title Title.
	 * @param x_label X axis label.
	 * @param y_label Y axis label
	 * @param legend The legend.
	 * @param colorModel Color model ID.
	 * @param limits The limits for the x and y axes.
	 * @param data The data.
	 * @param levels The levels to show as contours.
	 * @param width Chart width. Height is set automatically.
	 * @throws JPARSECException If an error occurs.
	 */
	public GridChartElement(String title, String x_label,
			String y_label, String legend, COLOR_MODEL colorModel,
			double[] limits, double data[][], double levels[], int width)
	throws JPARSECException {
		this.xLabel = x_label;
		this.yLabel = y_label;
		this.title = title;
		this.subTitle = "";
		if (width <= 100) throw new JPARSECException("size cannot be below 100 pixels.");
		this.imageWidth = width;
		this.imageHeight = width;
		this.legend = legend;
		this.colorModel = colorModel;
		this.setColorModel();
		this.limits = limits;
		this.levels = levels;
		this.type = GridChartElement.TYPE.RASTER;
		if (levels != null) type = TYPE.RASTER_CONTOUR;
		ocultLevels = false;
		levelsOrientation = WEDGE_ORIENTATION.VERTICAL_RIGHT;
		levelsBorderStyle = GridChartElement.WEDGE_BORDER.NO_BORDER;
		this.data = GridChartElement.ObjectToDoubleArray(data);
	}

	/**
	 * Full constructor.
	 *
	 * @param title Title.
	 * @param x_label X axis label.
	 * @param y_label Y axis label
	 * @param legend The legend.
	 * @param colorModel Color model ID.
	 * @param colorResolution Color model resolution.
	 * @param type Chart type.
	 * @param limits The limits for the different axes.
	 * @param data The data.
	 * @param levels The levels to show as contours.
	 * @param width Chart width. Height is set automatically.
	 * @throws JPARSECException If an error occurs.
	 */
	public GridChartElement(String title, String x_label,
			String y_label, String legend, COLOR_MODEL colorModel,
			int colorResolution, TYPE type,
			double[] limits, double data[][], double levels[], int width)
	throws JPARSECException {
		this.xLabel = x_label;
		this.yLabel = y_label;
		this.title = title;
		this.subTitle = "";
		if (width <= 100) throw new JPARSECException("size cannot be below 100 pixels.");
		this.imageWidth = width;
		this.imageHeight = width;
		this.legend = legend;
		this.colorModel = colorModel;
		this.colorModelResolution = colorResolution;
		this.setColorModel();
		this.limits = limits;
		this.levels = levels;
		this.type = type;
		this.data = GridChartElement.ObjectToDoubleArray(data);
		ocultLevels = false;
		levelsOrientation = WEDGE_ORIENTATION.VERTICAL_RIGHT;
		levelsBorderStyle = GridChartElement.WEDGE_BORDER.NO_BORDER;
	}


	/**
	 * Data to be drawn in red. Valid values range from 0 to 255.
	 */
	int[] red;
	/**
	 * Data to be drawn in green. Valid values range from 0 to 255.
	 */
	int[] green;
	/**
	 * Data to be drawn in blue. Valid values range from 0 to 255.
	 */
	int[] blue;

	/**
	 * Selects the color model to apply.
	 */
	public COLOR_MODEL colorModel;

	/**
	 * Label for x axis.
	 */
	public String xLabel;

	/**
	 * Label for y axis.
	 */
	public String yLabel;

	/**
	 * The legend for the 3rd variable.
	 */
	public String legend;
	/**
	 * Title.
	 */
	public String title;

	/**
	 * Title.
	 */
	public String subTitle;

	/**
	 * Chart type
	 */
	public TYPE type;

	/**
	 * X size of the chart in pixels.
	 */
	public int imageWidth;

	/**
	 * Y size of the chart in pixels.
	 */
	public int imageHeight;

	/**
	 * Holds limits as (physical) minimum/initial x, maximum/final x,
	 * minimum/initial y, maximum/final y.
	 */
	public double[] limits;

	/**
	 * The levels to show as contours.
	 */
	public double[] levels;

	/**
	 * The data to be displayed, ordered as [x][y] from bottom-left corner.
	 */
	public Double[][] data;
	/**
	 * Sets the number of levels, 64 by default.
	 */
	public int colorModelResolution = 64;

	/**
	 * True (default false) to invert x axis.
	 */
	public boolean invertXaxis = false;
	/**
	 * True (default false) to invert y axis.
	 */
	public boolean invertYaxis = false;

	/**
	 * Holds the opacity of the surface, only taken into account
	 * when creating the chart with VISAD.
	 */
	public OPACITY opacity = OPACITY.TRANSPARENT;

	/**
	 * Sets the text of pointers in a grid chart. There are two possible
	 * formats:<BR>
	 * 1. Physical position
	 * of the origin point of the arrow (x and y), physical position of
	 * the destination point, and the text.
	 * For example 0 0 1 1 Arrow from (0, 0) to (1, 1).<BR>
	 * 2. Physical position
	 * of the point (x and y), type of mark, size of mark in physical units,
	 * and label text. Types of marks supported are C (circle outline) and CF
	 * (filled circle). For example 0 0 C 10 Circle at (0, 0) with diameter = 10.<BR>
	 */
	public String pointers[] = new String[0];

	/**
	 * The set of possible opacities, if this chart is rendered using VISAD.
	 */
	public enum OPACITY {
		/** ID constant for a transparent surface. */
		TRANSPARENT,
		/** ID constant for an opaque surface. */
		OPAQUE,
		/** ID constant for a semi-transparent surface. */
		SEMI_TRANSPARENT,
		/** ID constant for an increasing opacity with the independent variable. */
		VARIABLE_WITH_Z
	};

	/**
	 * To clone the object.
	 */
	@Override
	public GridChartElement clone()
	{
		GridChartElement c = null;
		try {
			double li[] = limits;
			if (limits != null) li = limits.clone();
			double le[] = levels;
			if (levels != null) le = levels.clone();
			Double da[][] = data;
			if (data != null) da = data.clone();

			c = new GridChartElement(this.title, this.xLabel,
			this.yLabel, this.legend, this.colorModel, this.colorModelResolution,
			this.type, li, da, le, this.imageWidth);
		} catch (Exception e) {
			e.printStackTrace();
		}
		c.subTitle = this.subTitle;
		c.imageHeight = this.imageHeight;
		c.invertXaxis = this.invertXaxis;
		c.invertYaxis = this.invertYaxis;
		c.opacity = this.opacity;
		c.levelsOrientation = this.levelsOrientation;
		c.ocultLevels = this.ocultLevels;
		c.levelsBorderStyle = this.levelsBorderStyle;
		c.ocultLevelLabels = this.ocultLevelLabels;
		c.significantDigits = this.significantDigits;
		if (this.pointers != null) c.pointers = this.pointers.clone();
		return c;
	}

	/**
	 * Returns true if the input object is equals to this chart object.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof GridChartElement)) return false;

		GridChartElement that = (GridChartElement) o;

		if (imageWidth != that.imageWidth) return false;
		if (imageHeight != that.imageHeight) return false;
		if (colorModelResolution != that.colorModelResolution) return false;
		if (invertXaxis != that.invertXaxis) return false;
		if (invertYaxis != that.invertYaxis) return false;
		if (ocultLevels != that.ocultLevels) return false;
		if (ocultLevelLabels != that.ocultLevelLabels) return false;
		if (!Arrays.equals(red, that.red)) return false;
		if (!Arrays.equals(green, that.green)) return false;
		if (!Arrays.equals(blue, that.blue)) return false;
		if (colorModel != that.colorModel) return false;
		if (xLabel != null ? !xLabel.equals(that.xLabel) : that.xLabel != null) return false;
		if (yLabel != null ? !yLabel.equals(that.yLabel) : that.yLabel != null) return false;
		if (legend != null ? !legend.equals(that.legend) : that.legend != null) return false;
		if (title != null ? !title.equals(that.title) : that.title != null) return false;
		if (subTitle != null ? !subTitle.equals(that.subTitle) : that.subTitle != null) return false;
		if (type != that.type) return false;
		if (!Arrays.equals(limits, that.limits)) return false;
		if (!Arrays.equals(levels, that.levels)) return false;
		if (!Arrays.deepEquals(data, that.data)) return false;
		if (opacity != that.opacity) return false;
		// Probably incorrect - comparing Object[] arrays with Arrays.equals
		if (!Arrays.equals(pointers, that.pointers)) return false;
		if (levelsOrientation != that.levelsOrientation) return false;
		if (significantDigits != that.significantDigits) return false;

		return levelsBorderStyle == that.levelsBorderStyle;
	}

	@Override
	public int hashCode() {
		int result = red != null ? Arrays.hashCode(red) : 0;
		result = 31 * result + (green != null ? Arrays.hashCode(green) : 0);
		result = 31 * result + (blue != null ? Arrays.hashCode(blue) : 0);
		result = 31 * result + (colorModel != null ? colorModel.hashCode() : 0);
		result = 31 * result + (xLabel != null ? xLabel.hashCode() : 0);
		result = 31 * result + (yLabel != null ? yLabel.hashCode() : 0);
		result = 31 * result + (legend != null ? legend.hashCode() : 0);
		result = 31 * result + (title != null ? title.hashCode() : 0);
		result = 31 * result + (subTitle != null ? subTitle.hashCode() : 0);
		result = 31 * result + (type != null ? type.hashCode() : 0);
		result = 31 * result + imageWidth;
		result = 31 * result + imageHeight;
		result = 31 * result + (limits != null ? Arrays.hashCode(limits) : 0);
		result = 31 * result + (levels != null ? Arrays.hashCode(levels) : 0);
		result = 31 * result + (data != null ? Arrays.deepHashCode(data) : 0);
		result = 31 * result + colorModelResolution;
		result = 31 * result + (invertXaxis ? 1 : 0);
		result = 31 * result + (invertYaxis ? 1 : 0);
		result = 31 * result + (opacity != null ? opacity.hashCode() : 0);
		result = 31 * result + (pointers != null ? Arrays.hashCode(pointers) : 0);
		result = 31 * result + (ocultLevels ? 1 : 0);
		result = 31 * result + (ocultLevelLabels ? 1 : 0);
		result = 31 * result + significantDigits;
		result = 31 * result + (levelsOrientation != null ? levelsOrientation.hashCode() : 0);
		result = 31 * result + (levelsBorderStyle != null ? levelsBorderStyle.hashCode() : 0);
		return result;
	}

	/**
	 * The different chart types for SGT library.
	 */
	public enum TYPE {
		/** ID constant for a raster chart. */
		RASTER,
		/** ID constant for an area fill chart. */
		AREA_FILL,
		/** ID constant for a contour chart. */
		CONTOUR,
		/** ID constant for a raster contour chart. */
		RASTER_CONTOUR,
		/** ID constant for an area fill contour chart. */
		AREA_FILL_CONTOUR
	};

	/**
	 * The different color models for SGT library.
	 */
	public enum COLOR_MODEL {
		/** ID constant for black to white color model. */
		BLACK_TO_WHITE,
		/** ID constant for white to black color model. */
		WHITE_TO_BLACK,
		/** ID constant for red to blue color model. */
		RED_TO_BLUE,
		/** ID constant for blue to red color model. */
		BLUE_TO_RED
	};

    private static final int[] DEFAULT_RED =
    {  0,  0,  0,  0,  0,  0,  0,  0,
       0,  0,  0,  0,  0,  0,  0,  0,
       0,  0,  0,  0,  0,  0,  0,  0,
       0,  7, 23, 39, 55, 71, 87,103,
       119,135,151,167,183,199,215,231,
       247,255,255,255,255,255,255,255,
       255,255,255,255,255,255,255,255,
       255,246,228,211,193,175,158,140};
    private static final int[] DEFAULT_GREEN =
    {  0,  0,  0,  0,  0,  0,  0,  0,
       0, 11, 27, 43, 59, 75, 91,107,
       123,139,155,171,187,203,219,235,
       251,255,255,255,255,255,255,255,
       255,255,255,255,255,255,255,255,
       255,247,231,215,199,183,167,151,
       135,119,103, 87, 71, 55, 39, 23,
       7,  0,  0,  0,  0,  0,  0,  0};
    private static final int[] DEFAULT_BLUE =
    {  127,143,159,175,191,207,223,239,
       255,255,255,255,255,255,255,255,
       255,255,255,255,255,255,255,255,
       255,247,231,215,199,183,167,151,
       135,119,103, 87, 71, 55, 39, 23,
       7,  0,  0,  0,  0,  0,  0,  0,
       0,  0,  0,  0,  0,  0,  0,  0,
       0,  0,  0,  0,  0,  0,  0,  0};

    /**
     * Returns the color model array for the red color. The size 
     * depends on the color model resolution and the intensity, from 0 
     * to 255, represents the red color component for the RGB color to 
     * be used to represent the intensities in the chart from minimum 
     * to maximum.
     * @return Color model array for red.
     */
    public int[] getColorModelRedArray() { return red; }
    
    /**
     * Returns the color model array for the green color. The size 
     * depends on the color model resolution and the intensity, from 0 
     * to 255, represents the green color component for the RGB color to 
     * be used to represent the intensities in the chart from minimum 
     * to maximum.
     * @return Color model array for red.
     */
    public int[] getColorModelGreenArray() { return green; }
    
    /**
     * Returns the color model array for the bblue color. The size 
     * depends on the color model resolution and the intensity, from 0 
     * to 255, represents the blue color component for the RGB color to 
     * be used to represent the intensities in the chart from minimum 
     * to maximum.
     * @return Color model array for red.
     */
    public int[] getColorModelBlueArray() { return blue; }
    
	private void setColorModel()
	throws JPARSECException {
		this.red = new int[colorModelResolution];
		this.green = new int[colorModelResolution];
		this.blue = new int[colorModelResolution];

		switch (this.colorModel)
		{
		case BLACK_TO_WHITE:
		    for (int i=0; i<colorModelResolution; i++)
		    {
		    	red[i] = green[i] = blue[i] = 1 + i * 254/(colorModelResolution-1);
		    }
			break;
		case WHITE_TO_BLACK:
		    for (int i=0; i<colorModelResolution; i++)
		    {
		    	red[i] = green[i] = blue[i] = 1 + 254 - i * 254/(colorModelResolution-1);
		    }
			break;
		case RED_TO_BLUE:
			blue = DEFAULT_RED;
			green = DEFAULT_GREEN;
			red = DEFAULT_BLUE;
			if (colorModelResolution != 64)
			{
				this.red = new int[colorModelResolution];
				this.green = new int[colorModelResolution];
				this.blue = new int[colorModelResolution];
			    for (int i=0; i<colorModelResolution; i++)
			    {
			    	red[i] = 255 - i * 255 / (colorModelResolution-1);
			    	blue[i] = i * 255 / (colorModelResolution-1);
			    	green[i] = 255 - (int) (255.0 * Math.abs(((double) i - ((colorModelResolution-1.0)/2.0)) / (((colorModelResolution-1.0)/2.0))));
			    }
			}
			break;
		case BLUE_TO_RED:
			blue = DEFAULT_BLUE;
			green = DEFAULT_GREEN;
			red = DEFAULT_RED;
			if (colorModelResolution != 64)
			{
				this.red = new int[colorModelResolution];
				this.green = new int[colorModelResolution];
				this.blue = new int[colorModelResolution];
			    for (int i=0; i<colorModelResolution; i++)
			    {
			    	blue[i] = 255-i * 255/(colorModelResolution-1);
			    	red[i] = i * 255/(colorModelResolution-1);
			    	green[i] = 255 - (int) Math.abs(255.0 * (((double) i - (colorModelResolution-1.0)/2.0) / ((colorModelResolution-1.0)/2.0)));
			    }
			}
			break;
		default:
			throw new JPARSECException("invalid color model.");
		}
	}

	/**
	 * Inverts the color model.
	 * @throws JPARSECException If an error occurs.
	 */
	public void invertColorModel()
	throws JPARSECException {
		int c = this.colorModel.ordinal();
		if (c == GridChartElement.COLOR_MODEL.BLACK_TO_WHITE.ordinal()) {
			colorModel = GridChartElement.COLOR_MODEL.WHITE_TO_BLACK;
		}
		if (c == GridChartElement.COLOR_MODEL.WHITE_TO_BLACK.ordinal()) {
			colorModel = GridChartElement.COLOR_MODEL.BLACK_TO_WHITE;
		}
		if (c == GridChartElement.COLOR_MODEL.BLUE_TO_RED.ordinal()) {
			colorModel = GridChartElement.COLOR_MODEL.RED_TO_BLUE;
		}
		if (c == GridChartElement.COLOR_MODEL.RED_TO_BLUE.ordinal()) {
			colorModel = GridChartElement.COLOR_MODEL.BLUE_TO_RED;
		}
		setColorModel();
	}

	/**
	 * Flips the x and y axes so that the chart will appear as rotated 90 degrees.
	 * @throws JPARSECException If an error occurs.
	 */
	public void invertXYaxes() throws JPARSECException {
		data = ObjectToDoubleArray((double[][]) DataSet.flip2dArray(ObjectToDoubleArray(this.data), true, false, false));
		String xl = this.xLabel;
		xLabel = yLabel;
		yLabel = xl;
		if (limits != null) this.limits = new double[] {limits[2], limits[3], limits[0], limits[1]};
		if (pointers != null) {
			for (int i=0; i<pointers.length; i++) {
				String values[] = DataSet.toStringArray(pointers[i], " ", true);
				String v0 = values[0], v2 = values[2];
				values[0] = values[1];
				values[1] = v0;
				values[2] = values[3];
				values[3] = v2;
				pointers[i] = DataSet.toString(values, " ");
			}
		}
	}

	/**
	 * Returns the maximum value of the data array.
	 * @return Maximum value.
	 */
	public double getMaximum()
	{
		double max = Double.NaN;
		boolean start = false;
		for (int i=0; i<this.data.length; i++)
		{
			for (int j=0; j<this.data[i].length; j++)
			{
				if (this.data[i][j] != null) {
					double val = this.data[i][j];
					if (Double.isInfinite(val) || Double.isNaN(val)) continue;
					if (val > max || !start) {
						start = true;
						max = val;
					}
				}
			}
		}
		return max;
	}
	/**
	 * Returns the minimum value of the data array.
	 * @return Minimum value.
	 */
	public double getMinimum()
	{
		double min = Double.NaN;
		boolean start = false;
		for (int i=0; i<this.data.length; i++)
		{
			for (int j=0; j<this.data[i].length; j++)
			{
				if (this.data[i][j] != null) {
					double val = this.data[i][j];
					if (Double.isInfinite(val) || Double.isNaN(val)) continue;
					if (val < min || !start) {
						start = true;
						min = val;
					}
				}
			}
		}
		return min;
	}

	/**
	 * Returns the index of a given color.
	 * @param c The color.
	 * @return The index, from 0 to {@linkplain GridChartElement#colorModelResolution}-1,
	 * or -1 if the color is not found.
	 */
	public int getColorIndex(Color c)
	{
		int index = -1;
		int red = c.getRed(), blue = c.getBlue(), green = c.getGreen();
		for (int i=0; i<this.colorModelResolution; i++)
		{
			if (red == this.red[i] && green == this.green[i] && blue == this.blue[i])
				index = i;
		}
		return index;
	}

	/**
	 * Resamples the image data to a different resolution using a high-quality 2d spline
     * interpolation method (function {@linkplain ImageSplineTransform#resize(int, int)}).
	 * @param w New width.
	 * @param h New height.
	 * @throws JPARSECException If an error occurs.
	 */
	public void resample(int w, int h) throws JPARSECException
	{
		ImageSplineTransform t = new ImageSplineTransform(GridChartElement.ObjectToDoubleArray(this.data));
		t.resize(w, h);
		data = GridChartElement.ObjectToDoubleArray(t.getImage());
	}

	/**
	 * Clips the current chart to a given set of new limits located inside the current ones.
	 * The new chart has the same grid size, everything is interpolated.
	 * @param newLimits The new limits (min x, max x, min y, max y).
	 * @throws JPARSECException If an error occurs.
	 */
	public void clip(double newLimits[]) throws JPARSECException
	{
		double x0 = (newLimits[0] - limits[0]) / (limits[1] - limits[0]);
		double xf = (newLimits[1] - limits[0]) / (limits[1] - limits[0]);
		double y0 = (newLimits[2] - limits[2]) / (limits[3] - limits[2]);
		double yf = (newLimits[3] - limits[2]) / (limits[3] - limits[2]);
		Double newData[][] = new Double[data.length][data[0].length];
		ImageSplineTransform t = new ImageSplineTransform(GridChartElement.ObjectToDoubleArray(this.data));
		for (int i=0; i<data.length; i++) {
			double px = x0 * (data.length - 1.0) + ((xf - x0) * i);
			for (int j=0; j<data[0].length; j++) {
				double py = y0 * (data[0].length - 1.0) + ((yf - y0) * j);
				newData[i][j] = Double.NaN;
				try { newData[i][j] = t.interpolate(px, py); } catch (Exception exc) {}
			}
		}
		data = newData;
		this.limits = newLimits.clone();
	}

	/**
	 * Returns the intensity at certain position using 2d spline interpolation.
	 * Note the returned value will not agree
	 * exactly with the contour lines generated by SGT, but the difference
	 * should be always small and inside the level of uncertainty expected.
	 * @param x X position in physical units.
	 * @param y Y position in physical units.
	 * @return The intensity.
	 */
	public double getIntensityAt(double x, double y)
	{
		ImageSplineTransform t = new ImageSplineTransform(GridChartElement.ObjectToDoubleArray(data));
		int pointsX = this.data.length;
		int pointsY = this.data[0].length;
		double px = (x - this.limits[0]) * (pointsX - 1.0) / (this.limits[1] - this.limits[0]);
		double py = (y - this.limits[2]) * (pointsY - 1.0) / (this.limits[3] - this.limits[2]);
		double data = 0.0;
		try {
			data = t.interpolate(px, py);
		} catch (Exception exc) {}
		return data;
	}

	/**
	 * Returns an array of data from a given dataset. The dataset must define a
	 * rectangular surface.
	 * @param x The x values.
	 * @param y The y values.
	 * @param z The values in the 3rd axis.
	 * @return The data.
	 * @throws JPARSECException If an error occurs.
	 */
	public static Double[][] getDataFromDataSet(double x[], double y[], double z[])
	throws JPARSECException {
		double limits[] = GridChartElement.getLimitsFromDataSet(x, y);
		double stepX = -1, stepY = -1;
		for (int k=0; k<x.length; k++)
		{
			if (x[k] != limits[0] && ((Math.abs(x[k]-limits[0]) < stepX) || stepX == -1)) stepX = Math.abs(x[k]-limits[0]);
			if (x[k] != limits[1] && ((Math.abs(x[k]-limits[1]) < stepX) || stepX == -1)) stepX = Math.abs(x[k]-limits[1]);
		}
		for (int k=0; k<y.length; k++)
		{
			if (y[k] != limits[2] && ((Math.abs(y[k]-limits[2]) < stepY) || stepY == -1)) stepY = Math.abs(y[k]-limits[2]);
			if (y[k] != limits[3] && ((Math.abs(y[k]-limits[3]) < stepY) || stepY == -1)) stepY = Math.abs(y[k]-limits[3]);
		}

		int nx = 1 + (int) ((limits[1] - limits[0]) / stepX);
		int ny = 1 + (int) ((limits[3] - limits[2]) / stepY);
		Double data[][] = new Double[nx][ny];
		for (int i=0; i<nx; i++)
		{
			double minX = limits[0] + (i-0.5) * stepX;
			double maxX = limits[0] + (i+0.5) * stepX;
			for (int j=0; j<ny; j++)
			{
				double minY = limits[2] + (j-0.5) * stepY;
				double maxY = limits[2] + (j+0.5) * stepY;

				for (int k=0; k<x.length; k++)
				{
					if (x[k] > minX && x[k] < maxX && y[k] > minY && y[k] < maxY) data[i][ny-1-j] = z[k];
				}
			}
		}
		return data;
	}

	/**
	 * Returns the set of limits from a given dataset.
	 * @param x The x values.
	 * @param y The y values.
	 * @return The limits.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double[] getLimitsFromDataSet(double x[], double y[])
	throws JPARSECException {
		double limits[] = new double[4];
		limits[0] = DataSet.getMinimumValue(x);
		limits[1] = DataSet.getMaximumValue(x);
		limits[2] = DataSet.getMinimumValue(y);
		limits[3] = DataSet.getMaximumValue(y);
		return limits;
	}

	/**
	 * Creates sample data from a given function.
	 * @param f The function in Java format, f(x, y).
	 * @param x Array with the desired values of x.
	 * @param y Array with the desired values of y.
	 * @return The data.
	 * @throws JPARSECException If an error occurs.
	 */
	public static Double[][] createDataFromFunction(String f, double x[], double y[])
	throws JPARSECException {
		Double out[][] = new Double[x.length][y.length];
		for (int j=0; j<y.length; j++)
		{
			double py = y[j];
			for (int i=0; i<x.length; i++)
			{
				double px = x[i];
				Evaluation eval = new Evaluation(f, new String[] {"x "+px, "y "+py});
				out[i][j] = eval.evaluate();
			}
		}
		return out;
	}

	/**
	 * Transforms a Double[][] object to double[][].
	 * @param data Input object.
	 * @return Output object.
	 */
	public static double[][] ObjectToDoubleArray(Double data[][])
	{
		double out[][] = new double[data.length][data[0].length];
		for (int i=0; i<data.length; i++)
		{
			for (int j=0; j<data[0].length; j++)
			{
				out[i][j] = data[i][j];
			}
		}
		return out;
	}

	/**
	 * Transforms a Double[][] object to double[][].
	 * @param data Input object.
	 * @param blanking Value for pixels with NaN, to fill possible
	 * holes of data in the input image.
	 * @return Output object.
	 */
	public static double[][] ObjectToDoubleArray(Double data[][], double blanking)
	{
		double out[][] = new double[data.length][data[0].length];
		for (int i=0; i<data.length; i++)
		{
			for (int j=0; j<data[0].length; j++)
			{
				out[i][j] = data[i][j];
				if (Double.isNaN(out[i][j]) || Double.isInfinite(out[i][j])) out[i][j] = blanking;
			}
		}
		return out;
	}

	/**
	 * Transforms a double[][] object to Double[][].
	 * @param data Input object.
	 * @return Output object.
	 */
	public static Double[][] ObjectToDoubleArray(double data[][])
	{
		Double out[][] = new Double[data.length][data[0].length];
		for (int i=0; i<data.length; i++)
		{
			for (int j=0; j<data[0].length; j++)
			{
				out[i][j] = data[i][j];
			}
		}
		return out;
	}

	/**
	 * Creates an array of values given the init value, end value, and the step.
	 * @param min Minimum value.
	 * @param max Maximum value.
	 * @param step Step.
	 * @return The array.
	 */
	public static double[] createData(double min, double max, double step)
	{
		int n = 1 + (int) (0.5 + (max - min) / step);
		double out[] = new double[n];
		for (int i=0; i<out.length; i++)
		{
			out[i] = min + (double) i * step;
		}
		return out;
	}

	/**
	 * Creates a grid series given an irregular set of points that lies on it.
	 * This method uses an interpolation algorithm similar to a discrete
	 * Fourier transform, but more adequate to properly treat the 'holes'.
	 * If the points contain no holes and are sampled regularly it is recommended
	 * to use 2d spline interpolation with class {@linkplain ImageSplineTransform}.
	 * @param points The array of points (x, y, z).
	 * @param n The number of points in the x and y axis of the
	 * output regular profile.
	 * @return The series.
	 * @throws JPARSECException If an error occurs.
	 */
	public static GridChartElement getSurfaceFromPoints(DoubleVector points[], int n)
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

		GridChartElement out = new GridChartElement("", "", "", "",
				GridChartElement.COLOR_MODEL.BLACK_TO_WHITE,
				GridChartElement.getLimitsFromDataSet(outX, outY),
				data, null, 600
				);
		return out;
	}

	/**
	 * Set to true to ocultate the level colors.
	 */
	public boolean ocultLevels;
	/**
	 * Set to true to ocultate the level labels inside the chart.
	 */
	public boolean ocultLevelLabels = false;
	
	/**
	 * The number of significant digits in the labels of the levels for
	 * raster contour charts. Default is 3.
	 */
	public int significantDigits = 3;
	
	/**
	 * Set wedge orientation.
	 */
	public WEDGE_ORIENTATION levelsOrientation;
	/**
	 * Set wedge border style.
	 */
	public WEDGE_BORDER levelsBorderStyle = WEDGE_BORDER.NO_BORDER;

	/**
	 * The possible orientations for the wedge.
	 */
	public enum WEDGE_ORIENTATION {
		/** ID constant for horizontal orientation of levels, with position down. */
		HORIZONTAL_BOTTOM,
		/** ID constant for horizontal orientation of levels, with position up. */
		HORIZONTAL_TOP,
		/** ID constant for vertical orientation of levels, with position right. */
		VERTICAL_RIGHT
	}

	/**
	 * The possible borders for the wedge.
	 */
	public enum WEDGE_BORDER {
		/** ID constant for plain borders in levels. */
		PLAIN,
		/** ID constant for no borders in levels. */
		NO_BORDER,
		/** ID constant for raised borders in levels. This is currently not working. */
		RAISED
	}
}
