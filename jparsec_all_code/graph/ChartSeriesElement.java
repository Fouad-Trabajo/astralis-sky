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

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.Arrays;

import jparsec.math.GenericFit;
import jparsec.math.LinearFit;
import jparsec.math.MeasureElement;
import jparsec.math.Polynomial;
import jparsec.math.Regression;
import jparsec.util.JPARSECException;

/**
 * Creates a series for later use in JFreeChart.<P>
 *
 * In this object the legends can be
 * encoded following the instructions given in {@linkplain TextLabel} class. This
 * provides some possibilities like to dynamically change color/size, include superscript
 * or subscript text, or greek letters.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class ChartSeriesElement implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Default empty constructor.
	 */
	public ChartSeriesElement()
	{
		shapeSize = SHAPE_SIZE;
	}

	/**
	 * Simple constructor.
	 *
	 * @param x X values.
	 * @param y Y Values.
	 * @param legend Legend.
	 * @param show_legend True to show legend.
	 * @throws JPARSECException If an error occurs.
	 */
	public ChartSeriesElement(String x[], String y[], String legend, boolean show_legend) throws JPARSECException
	{
		this.xValues = x;
		this.yValues = y;
		this.legend = legend;
		this.showLegend = show_legend;

		try {
			double minimumY = DataSet.getMinimumValue((double[]) DataSet.getDoubleValuesIncludingLimits(y).get(0));
			this.yMinimumValue = minimumY;
		} catch (Exception exc) {}

		shape = ChartSeriesElement.SHAPE_CIRCLE;
		shapeSize = SHAPE_SIZE;
		regressionType = REGRESSION.NONE;
	}

	/**
	 * Constructor adequate for an x-y chart with bar errors.
	 *
	 * @param x X values.
	 * @param y Y Values.
	 * @param dx X error values.
	 * @param dy Y error values.
	 * @param legend Legend.
	 * @param show_legend True to show legend.
	 * @param color Color.
	 * @param shape Shape.
	 * @param regression Regression type.
	 * @throws JPARSECException If an error occurs.
	 */
	public ChartSeriesElement(String x[], String y[], double dx[], double dy[], String legend, boolean show_legend,
			Paint color, Shape shape, REGRESSION regression) throws JPARSECException
	{
		this.xValues = x;
		this.yValues = y;
		this.dxValues = dx;
		this.dyValues = dy;
		this.legend = legend;
		this.showLegend = show_legend;
		this.color = color;
		this.shape = shape;
		this.regressionType = regression;

		try {
			double minimumY = DataSet.getMinimumValue((double[]) DataSet.getDoubleValuesIncludingLimits(y).get(0));
			this.yMinimumValue = minimumY;
		} catch (Exception exc) {}

		shapeSize = SHAPE_SIZE;
	}

	/**
	 * Constructor adequate for an x-y chart with bar errors.
	 *
	 * @param x X values.
	 * @param y Y Values.
	 * @param dx X error values.
	 * @param dy Y error values.
	 * @param legend Legend.
	 * @param show_legend True to show legend.
	 * @param color Color.
	 * @param shape Shape.
	 * @param regression Regression type.
	 * @throws JPARSECException If an error occurs.
	 */
	public ChartSeriesElement(double x[], double y[], double dx[], double dy[], String legend, boolean show_legend,
			Paint color, Shape shape, REGRESSION regression) throws JPARSECException
	{
		this.xValues = DataSet.toStringValues(x);
		this.yValues = DataSet.toStringValues(y);
		this.dxValues = dx;
		this.dyValues = dy;
		this.legend = legend;
		this.showLegend = show_legend;
		this.color = color;
		this.shape = shape;
		this.regressionType = regression;

		try {
			double minimumY = DataSet.getMinimumValue(y);
			this.yMinimumValue = minimumY;
		} catch (Exception exc) {}

		shapeSize = SHAPE_SIZE;
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
	 * Array of dx values.
	 */
	public double[] dxValues;

	/**
	 * Array of dy values.
	 */
	public double[] dyValues;

	/**
	 * Legend.
	 */
	public String legend;

	/**
	 * True (default) to show a legend. In pie charts is ignored, leyend is always shown.
	 */
	public boolean showLegend = true;

	/**
	 * Selects the color gradient of the shapes/lines.
	 */
	public Paint color;

	/**
	 * Colors of the different sections in pie charts.
	 */
	public Paint[] colorsForPieCharts;

	/**
	 * Set to true to use an user defined array of colors in a pie chart.
	 */
	public boolean useCustomColorsInPieCharts = false;

	/**
	 * Shape of the points.
	 */
	public Shape shape;

	/**
	 * To show point shapes or not.
	 */
	public boolean showShapes = true;

	/**
	 * To draw lines between each point or not. No effect in category charts nor
	 * pie charts.
	 */
	public boolean showLines = false;

	/**
	 * To set the regression type. Constants defined in this class and in class
	 * GenericFit. If you use GenericFit you should previously check that the
	 * data can be fitted with a given function.
	 */
	public REGRESSION regressionType = REGRESSION.NONE;

	/**
	 * Holds the size of the shapes.
	 */
	public int shapeSize = SHAPE_SIZE;

	/**
	 * Sets the size of the limit arrows, 30 pixels as default.
	 */
	public int sizeOfArrowInLimits = 30;

	/**
	 * Sets the stroke for the regression line.
	 */
	public JPARSECStroke stroke = JPARSECStroke.STROKE_DEFAULT_LINE;

	/**
	 * Sets the text to pointers in an x-y chart. Format is point number (in the
	 * order defined by the x array, being 1 the first point) plus the text to
	 * write. Pointers and labels are set automatically using the angle defined
	 * in another parameter (the pointers angle), although the value can be set in
	 * each pointer using the '@LEFT', '@RIGHT', '@UP', and '@DOWN' commands at
	 * the beginning of the pointer text. The command '@CENTERx', where x is a
	 * number between 1 and 9, can also be used to locate the text relative to the
	 * arrow when exporting to Gildas format.<P>
	 * Another posible format to set a pointer in a given location (not towards
	 * a given point in the chart) is to replace the point number with
	 * '(x,y)' (without quotes), being x and y the pointer location in physical units.
	 */
	public String pointers[] = new String[0];

	/**
	 * Sets an angle in radians to point the pointers. Can be set to
	 * one of the constants defined in this class. Default value is
	 * up.
	 */
	public POINTER_ANGLE pointersAngle = POINTER_ANGLE.UPWARDS;

	/**
	 * To show or not arrows in pointers, yes by default.
	 */
	public boolean showArrowInPointers = true;

	/**
	 * True (default) to show error bars.
	 */
	public boolean showErrorBars = true;

	/**
	 * Sets the offset position factor for pointer labels. Should be between 0 and 2. 1
	 * by default.
	 */
	public float pointersLabelOffsetFactor = 1.0f;

	/**
	 * Sets minimum value of x. Useful for charts in log scale where error bars
	 * can reach negative values.
	 */
	public double xMinimumValue = 1E-10;

	/**
	 * Sets minimum value of y. Useful for charts in log scale where error bars
	 * can reach negative values.
	 */
	public double yMinimumValue = 0.1;

	/**
	 * True to enable (show) this series, false to hide it.
	 * This is only supported on xy charts.
	 */
	public boolean enable = true;

	/**
	 * Returns x values as doubles.
	 * @return X values as doubles.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[] getXs() throws JPARSECException {
		return DataSet.toDoubleValues(xValues);
	}

	/**
	 * Returns y values as doubles.
	 * @return Y values as doubles.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[] getYs() throws JPARSECException {
		return DataSet.toDoubleValues(yValues);
	}
	
	/**
	 * Set the x values as doubles.
	 * @param x X values as doubles.
	 * @throws JPARSECException If an error occurs.
	 */
	public void setXs(double x[]) throws JPARSECException {
		xValues = DataSet.toStringValues(x);
	}

	/**
	 * Set the y values as doubles.
	 * @param y Y values as doubles.
	 * @throws JPARSECException If an error occurs.
	 */
	public void setYs(double y[]) throws JPARSECException {
		yValues = DataSet.toStringValues(y);
	}

	/**
	 * Holds some colors.
	 */
	public static final Color[] COLORS = new Color[] {Color.BLACK, Color.BLUE, Color.CYAN,
		Color.DARK_GRAY, Color.GRAY, Color.GREEN, Color.LIGHT_GRAY, Color.MAGENTA, Color.ORANGE,
		Color.PINK, Color.RED, Color.WHITE, Color.YELLOW};
	/**
	 * Holds some colors.
	 */
	public static final String[] COLORS_MEANINGS = new String[] {"Black", "Blue", "Cyan", "Dark gray",
		"Gray", "Green", "Light gray", "Magenta", "Orange", "Pink", "Red", "White", "Yellow"};

	/**
	 * The set of locations for the pointers.
	 */
	public enum POINTER_ANGLE {
		/** ID constant to point the pointers to the chart center. */
		TO_CENTER,
		/** ID constant to point the pointers outside the chart. */
		TO_OUTSIDE,
		/** ID constant to point the pointers towards up. */
		UPWARDS,
		/** ID constant to point the pointers towards down. */
		DOWNWARDS,
		/** ID constant to point the pointers towards left. */
		LEFTWARDS,
		/** ID constant to point the pointers towards right. */
		RIGHTWARDS,
		/**
		 * ID constant to point the pointers trying to avoid superimposed strings between the
		 * pointers in each point. The algorithm used for this is not very efficient.
		 */
		AVOID_SUPERIMPOSED_STRINGS
	};

	/**
	 * Pointer angle meanings.
	 */
	public static final String[] POINTER_ANGLES_MEANINGS = new String[] {"Up", "Down", "Left", "Right",
		"To center", "To outside", "Automatic"};

	/**
	 * The regression types. They are used only for x-y charts.
	 */
	public enum REGRESSION {
		/** ID Constant for no regression. */
		NONE,
		/** ID Constant for linear regression. */
		LINEAR,
		/** ID Constant for a polynomial regression. Polynomial degree is 2 by default. */
		POLYNOMIAL,
		/** ID Constant for spline interpolation (no regression). */
		SPLINE_INTERPOLATION,
		/** ID Constant for linear interpolation (no regression). */
		LINEAR_INTERPOLATION,
		/** ID constant for a general regression fit using {@linkplain GenericFit} class.
		 * The functions must be set after selecting this option. */
		GENERIC_FIT,
		/** ID constant for a general regression fit using {@linkplain Regression} class.
		 * The custom function and initial estimates must be set after selecting this option. */
		REGRESSION_CUSTOM;

		private boolean showEquation = false;
		private boolean showRegression = true;
		private String equation = "";
		private double eqVal[], eqErr[];
		private boolean fromLinearFit = false;
		private int polynomialDegree = 2;
		private String[] genericFit = null;
		private int[] color = null;
		private double initialEstimates[];

		/**
		 * Sets the custom function used to fit the data in case the
		 * custom regression fit option is selected. See {@linkplain Regression} class.
		 * @param function The function.
		 * @param initialEstimates The initial estimates for the fitting parameters.
		 */
		public void setCustomRegressionFitFunctions(String function, double initialEstimates[]) {
			genericFit = null;
			if (function != null) genericFit = new String[] {function};
			this.initialEstimates = initialEstimates;
		}

		/**
		 * Sets the generic functions used to fit the data in case the
		 * generic fit option is selected. They should be 3. See {@linkplain GenericFit} class.
		 * @param functions The functions.
		 */
		public void setGenericFitFunctions(String functions[]) {
			genericFit = null;
			if (functions != null) genericFit = functions.clone();
		}

		/**
		 * Returns the functions for a generic fit.
		 * @return The fitting functions.
		 */
		public String[] getGenericFitFunctions() {
			if (genericFit == null) return null;
			return genericFit.clone();
		}

		/**
		 * Returns the functions for a custom regression fit.
		 * @return The fitting function.
		 */
		public String getCustomRegressionFitFunction() {
			if (genericFit == null) return null;
			return genericFit[0];
		}

		/**
		 * Returns the initial estimates for the fitting parameters.
		 * @return The initial estimates.
		 */
		public double[] getCustomRegressionFitInitialEstimates() {
			return this.initialEstimates;
		}

		/**
		 * Returns if the fitting formula should be shown or not.
		 * @return True or false.
		 */
		public boolean getShowEquation() {
			return showEquation;
		}

		/**
		 * Sets if the fitting formula should be shown or not.
		 * @param show True or false.
		 */
		public void setShowEquation(boolean show) {
			showEquation = show;
		}

		/**
		 * Returns if the regression series should be shown or not.
		 * @return True or false.
		 */
		public boolean getShowRegression() {
			return showRegression;
		}

		/**
		 * Sets if the regression series should be shown or not.
		 * @param show True or false.
		 */
		public void setShowRegression(boolean show) {
			showRegression = show;
		}

		/**
		 * Returns the equation string.
		 * @return The equation.
		 */
		public String getEquation() {
			return equation;
		}

		/**
		 * Sets a custom value for the equation string.
		 * @param eq The equation.
		 */
		public void setEquation(String eq) {
			equation = eq;
		}

		/**
		 * Returns the set of values representing the fit. For a line
		 * the values will be y for x = 0, and the slope, for a polynomial
		 * its coefficients.
		 * @return The values for the fitting formula.
		 */
		public double[] getEquationValues() {
			return eqVal;
		}

		/**
		 * Sets the values for the coefficients of the fitting formula.
		 * The equation formula is modified only if it is null or empty string
		 * (and not a generic custom fitting formula).
		 * @param val The coefficients for the fitting formula. For a line
		 * the values will be y for x = 0, and the slope (use better the
		 * method for a linear fit instance in this case), for a polynomial
		 * its coefficients.
		 * @param err The coefficients for the errors.
		 */
		public void setEquationValues(double[] val, double[] err) {
			eqVal = val;
			eqErr = err;
			setEquation();
		}

		/**
		 * Returns the set of values representing the errors of the fitting values.
		 * For a line the values will be dy for x = 0, and the slope error, for a
		 * polynomial the errors of its coefficients.
		 * @return The values for the errors of the parameters of the fitting formula.
		 */
		public double[] getEquationValuesErrors() {
			return eqErr;
		}

		/**
		 * Sets the fitting values from a linear fit instance.
		 * The equation formula is modified only if it is null or empty string
		 * (and not a generic custom fitting formula).
		 * @param fit The instance for a fit to a straight line.
		 */
		public void setEquationFromLinearFit(LinearFit fit) {
			eqVal = new double[] {fit.valueInXEqualToZero, fit.slope};
			eqErr = new double[] {fit.dvalueInXEqualToZero, fit.dslope};
			fromLinearFit = true;
			setEquation();
		}
		/**
		 * Returns the linear fit instance for the fitting formula. In
		 * case the values were not set using a linear fit instance,
		 * this method will return null.
		 * @return The linear fit instance, or null if no one was used
		 * to define the fit.
		 */
		public LinearFit getLinearFit() {
			if (!fromLinearFit) return null;
			LinearFit fit = new LinearFit(null, null);
			fit.slope = eqVal[1];
			fit.dslope = eqErr[1];
			fit.valueInXEqualToZero = eqVal[0];
			fit.dvalueInXEqualToZero = eqErr[0];
			return fit;
		}

		/**
		 * Returns the degree of the polynomial for the fit.
		 * Note this value is not necessarily related to the number of
		 * coefficients in the parameters of the fit, since this value
		 * can be changed.
		 * @return The polynomial degree.
		 */
		public int getPolynomialDegree() {
			return polynomialDegree;
		}

		/**
		 * Sets the degree of the polynomial for the fit. It should be
		 * set before the chart is created.
		 * @param degree The degree of the fitting polynomial.
		 */
		public void setPolynomialDegree(int degree) {
			this.polynomialDegree = degree;
		}

		/**
		 * Returns a polynomial instance representing the fitting formula.
		 * @return The fitting polynomial.
		 */
		public Polynomial getPolynomialFit() {
			if (eqVal == null) return null;
			Polynomial p = new Polynomial(eqVal);
			return p;
		}

		/**
		 * Returns a {@linkplain GenericFit} instance representing the fitting formula.
		 * @return The fitting formula.
		 */
		public GenericFit getGenericFit() {
			if (eqVal == null) return null;
			GenericFit p = new GenericFit(null, null, genericFit[0], genericFit[1], genericFit[2]);
			return p;
		}

		/**
		 * Returns true if the regression was performed, false
		 * otherwise. More exactly, this method will return true
		 * if the equation values are not null.
		 * @return True or false.
		 */
		public boolean regressionDone() {
			if (eqVal == null) return false;
			return true;
		}

		private void setEquation() {
			if (equation != null) {
				if (!equation.equals("")) return;
			}

			String eq = "";
			if (eqVal != null) {
				for (int i=0; i<eqVal.length; i++) {
					MeasureElement m = new MeasureElement(eqVal[i], eqErr[i], "");
					String x = " x^{"+i+"}";
					if (i == 0) x = "";
					if (i == 1) x = " x";
					if (i > 0) eq += " ";
					String mm = m.toString(true);
					if (mm.startsWith("-")) {
						mm = "- " + mm.substring(1);
					} else {
						if (i > 0) mm = "+ " + mm;
					}
					eq += mm + x+"";
				}
				eq = DataSet.replaceAll(eq, "(0.0)", "", true);
				eq = DataSet.replaceAll(eq, "(NaN)", "", true);
			}

			switch (this) {
			case NONE:
				equation = "";
				break;
			case LINEAR:
			case POLYNOMIAL:
				equation = eq;
				break;
			case LINEAR_INTERPOLATION:
				equation = "Linear interpolation";
				break;
			case SPLINE_INTERPOLATION:
				equation = "Spline interpolation";
				break;
			default:
				break;
			}
		}

		/**
		 * Returns the color to show the regression. Default color is the same
		 * as the series to apply the regression.
		 * @return The color, or null if it is not set.
		 */
		public Color getColor() {
			if (color == null) return null;
			return new Color(color[0], color[1], color[2], color[3]);
		}
		/**
		 * Sets the color of the regression. Default color is the same
		 * as the series to apply the regression.
		 * @param col The color.
		 */
		public void setColor(Color col) {
			color = new int[] {col.getRed(), col.getGreen(), col.getBlue(), col.getAlpha()};
		}

		/**
		 * Resets all fields to their default values for this enum
		 * constant.
		 */
		public void clear() {
			showEquation = false;
			showRegression = true;
			equation = "";
			eqVal = null;
			eqErr = null;
			fromLinearFit = false;
			polynomialDegree = 2;
			genericFit = null;
			color = null;
			initialEstimates = null;
		}

		/**
		 * Resets the regression to force it to be calculated again.
		 */
		public void clearRegression() {
			eqVal = null;
			eqErr = null;
			genericFit = null;
			color = null;
			initialEstimates = null;
			equation = "";
		}
	};

	/**
	 * Holds types of regressions.
	 */
	public static final String[] REGRESSION_TYPES = new String[] {"None", "Linear", "Polynomial", "Spline interpolation",
		"Linear interpolation", "Generic fit", "Custom regression"};

	/**
	 * Gets the pointers from a string. They are defined as the point number
	 * plus an space and the text to write as the pointer. Instead of the point
	 * number it is possible to insert the point position in the format
	 * (x, y).
	 *
	 * @param comment Input string.
	 * @return Array of pointers.
	 */
	public static String[] getPointers(String comment)
	{
		int pointer = comment.indexOf("<POINTER>");
		if (pointer < 0)
			return new String[0];

		int np = 1;
		String com = comment;
		do
		{
			com = com.substring(pointer + 1);
			pointer = com.indexOf("<POINTER>");
			if (pointer > 0)
				np++;
		} while (pointer > 0);

		String out[] = new String[np];

		int pos = 0;
		com = comment;
		for (int i = 0; i < np; i++)
		{
			com = com.substring(pos);
			pointer = com.indexOf("<POINTER>");
			int pointer_end = com.indexOf("</POINTER>");
			out[i] = com.substring(pointer + 9, pointer_end).trim();
			pos = pointer_end + 1;
		}

		return out;
	}

	/**
	 * Sets the size of the shapes. 3 is the default size.
	 * @param size Size.
	 */
	public static void setShapeSize(int size)
	{
		SHAPE_SIZE = size;

		SHAPE_SQUARE = new Rectangle2D.Double(-SHAPE_SIZE, -SHAPE_SIZE, SHAPE_SIZE * 2,
				SHAPE_SIZE * 2);
		SHAPE_CIRCLE = new Ellipse2D.Double(-SHAPE_SIZE, -SHAPE_SIZE, SHAPE_SIZE * 2,
				SHAPE_SIZE * 2);

		SHAPE_TRIANGLE_UP = new Polygon(new int[]
		{ 0, SHAPE_SIZE, -SHAPE_SIZE }, new int[]
		{ -SHAPE_SIZE, SHAPE_SIZE, SHAPE_SIZE }, 3);

		SHAPE_DIAMOND = new Polygon(new int[]
		{ 0, SHAPE_SIZE, 0, -SHAPE_SIZE }, new int[]
		{ -SHAPE_SIZE, 0, SHAPE_SIZE, 0 }, 4);

		SHAPE_HORIZONTAL_RECTANGLE = new Rectangle2D.Double(-SHAPE_SIZE, -SHAPE_SIZE * 0.5,
				SHAPE_SIZE * 2, SHAPE_SIZE);

		SHAPE_TRIANGLE_DOWN = new Polygon(new int[]
		{ -SHAPE_SIZE, SHAPE_SIZE, 0 }, new int[]
		{ -SHAPE_SIZE, -SHAPE_SIZE, SHAPE_SIZE }, 3);

		SHAPE_ELLIPSE = new Ellipse2D.Double(-SHAPE_SIZE, -SHAPE_SIZE * 0.5, SHAPE_SIZE * 2,
				SHAPE_SIZE);

		SHAPE_TRIANGLE_RIGHT = new Polygon(new int[]
		{ -SHAPE_SIZE, SHAPE_SIZE, -SHAPE_SIZE }, new int[]
		{ -SHAPE_SIZE, 0, SHAPE_SIZE }, 3);

		SHAPE_VERTICAL_RECTANGLE = new Rectangle2D.Double(-SHAPE_SIZE * 0.5, -SHAPE_SIZE,
				SHAPE_SIZE * 0.5, SHAPE_SIZE * 2);
		SHAPE_TRIANGLE_LEFT = new Polygon(new int[]
		{ -SHAPE_SIZE, SHAPE_SIZE, SHAPE_SIZE }, new int[]
		{ 0, -SHAPE_SIZE, SHAPE_SIZE }, 3);

		SHAPE_STAR = ChartSeriesElement.shapeFor(2, SHAPE_SIZE*4, SHAPE_SIZE*4);
		SHAPE_STAR2 = ChartSeriesElement.shapeFor(4, SHAPE_SIZE*4, SHAPE_SIZE*4);
		SHAPE_PENTAGON = ChartSeriesElement.shapeFor(3, SHAPE_SIZE*4, SHAPE_SIZE*4);
		SHAPE_CRUX = ChartSeriesElement.shapeFor(0, SHAPE_SIZE*4, SHAPE_SIZE*4);

		SHAPES = new Shape[] {SHAPE_CIRCLE, SHAPE_SQUARE, SHAPE_DIAMOND, SHAPE_TRIANGLE_UP,
			SHAPE_TRIANGLE_DOWN, SHAPE_TRIANGLE_LEFT, SHAPE_TRIANGLE_RIGHT, SHAPE_HORIZONTAL_RECTANGLE,
			SHAPE_VERTICAL_RECTANGLE, SHAPE_ELLIPSE, SHAPE_POINT,
			SHAPE_STAR, SHAPE_STAR2, SHAPE_PENTAGON, SHAPE_CRUX};
	}

	/**
	 * Default size of the shapes = 3.
	 */
	public static final int SHAPE_DEFAULT_SIZE = 3;

	/**
	 * Size of the shapes, 3 as default.
	 */
	private static int SHAPE_SIZE = SHAPE_DEFAULT_SIZE;

	/**
	 * Returns the current size of the shapes.
	 * @return The shape size.
	 */
	public static int getShapeSize()
	{
		return SHAPE_SIZE;
	}

	/**
	 * Square.
	 */
	public static Shape SHAPE_SQUARE = new Rectangle2D.Double(-SHAPE_SIZE, -SHAPE_SIZE, SHAPE_SIZE * 2,
			SHAPE_SIZE * 2);

	/**
	 * Circle.
	 */
	public static Shape SHAPE_CIRCLE = new Ellipse2D.Double(-SHAPE_SIZE, -SHAPE_SIZE, SHAPE_SIZE * 2,
			SHAPE_SIZE * 2);

	/**
	 * Triangle towards up.
	 */
	public static Shape SHAPE_TRIANGLE_UP = new Polygon(new int[]
	{ 0, SHAPE_SIZE, -SHAPE_SIZE }, new int[]
	{ -SHAPE_SIZE, SHAPE_SIZE, SHAPE_SIZE }, 3);

	/**
	 * Diamond.
	 */
	public static Shape SHAPE_DIAMOND = new Polygon(new int[]
	{ 0, SHAPE_SIZE, 0, -SHAPE_SIZE }, new int[]
	{ -SHAPE_SIZE, 0, SHAPE_SIZE, 0 }, 4);

	/**
	 * Rectangle.
	 */
	public static Shape SHAPE_HORIZONTAL_RECTANGLE = new Rectangle2D.Double(-SHAPE_SIZE, -SHAPE_SIZE * 0.5,
			SHAPE_SIZE * 2, SHAPE_SIZE);

	/**
	 * Triangle towards down.
	 */
	public static Shape SHAPE_TRIANGLE_DOWN = new Polygon(new int[]
	{ -SHAPE_SIZE, SHAPE_SIZE, 0 }, new int[]
	{ -SHAPE_SIZE, -SHAPE_SIZE, SHAPE_SIZE }, 3);

	/**
	 * Ellipse.
	 */
	public static Shape SHAPE_ELLIPSE = new Ellipse2D.Double(-SHAPE_SIZE, -SHAPE_SIZE * 0.5, SHAPE_SIZE * 2,
			SHAPE_SIZE);

	/**
	 * Triangle towards right.
	 */
	public static Shape SHAPE_TRIANGLE_RIGHT = new Polygon(new int[]
	{ -SHAPE_SIZE, SHAPE_SIZE, -SHAPE_SIZE }, new int[]
	{ -SHAPE_SIZE, 0, SHAPE_SIZE }, 3);

	/**
	 * Rectangle.
	 */
	public static Shape SHAPE_VERTICAL_RECTANGLE = new Rectangle2D.Double(-SHAPE_SIZE * 0.5, -SHAPE_SIZE,
			SHAPE_SIZE * 0.5, SHAPE_SIZE * 2);

	/**
	 * Triangle towards left.
	 */
	public static Shape SHAPE_TRIANGLE_LEFT = new Polygon(new int[]
	{ -SHAPE_SIZE, SHAPE_SIZE, SHAPE_SIZE }, new int[]
	{ 0, -SHAPE_SIZE, SHAPE_SIZE }, 3);

	/**
	 * Point.
	 */
	public static final Shape SHAPE_POINT = new Ellipse2D.Double(-0.5, -0.5, 1.0, 1.0);

	/**
	 * Empty shape.
	 */
	public static final Shape SHAPE_EMPTY = new Ellipse2D.Double(0.0, 0.0, 0.0, 0.0);

	/**
	 * Star shape.
	 */
	public static Shape SHAPE_STAR = ChartSeriesElement.shapeFor(2, SHAPE_SIZE*4, SHAPE_SIZE*4);

	/**
	 * Star2 shape.
	 */
	public static Shape SHAPE_STAR2 = ChartSeriesElement.shapeFor(4, SHAPE_SIZE*4, SHAPE_SIZE*4);

	/**
	 * Pentagon shape.
	 */
	public static Shape SHAPE_PENTAGON = ChartSeriesElement.shapeFor(3, SHAPE_SIZE*4, SHAPE_SIZE*4);

	/**
	 * Crux shape.
	 */
	public static Shape SHAPE_CRUX = ChartSeriesElement.shapeFor(0, SHAPE_SIZE*4, SHAPE_SIZE*4);

	/**
	 * Holds shapes types.
	 */
	public static final String[] SHAPES_TYPES = new String[] {"Circle", "Square", "Diamond", "Triangle up",
		"Triangle down", "Triangle left", "Triangle right", "Horizontal rectangle", "Vertical rectangle",
		"Ellipse", "Point", "Star", "Star2", "Pentagon", "Crux"};
	/**
	 * Holds shapes types.
	 */
	public static Shape[] SHAPES = new Shape[] {SHAPE_CIRCLE, SHAPE_SQUARE, SHAPE_DIAMOND, SHAPE_TRIANGLE_UP,
		SHAPE_TRIANGLE_DOWN, SHAPE_TRIANGLE_LEFT, SHAPE_TRIANGLE_RIGHT, SHAPE_HORIZONTAL_RECTANGLE,
		SHAPE_VERTICAL_RECTANGLE, SHAPE_ELLIPSE, SHAPE_POINT, SHAPE_STAR, SHAPE_STAR2, SHAPE_PENTAGON, SHAPE_CRUX};


	/**
	 * To clone the object.
	 */
	@Override
	public ChartSeriesElement clone()
	{
		ChartSeriesElement c = new ChartSeriesElement();
		c.color = this.color;
		if (this.colorsForPieCharts != null) c.colorsForPieCharts = this.colorsForPieCharts.clone();
		if (dxValues != null) c.dxValues = this.dxValues.clone();
		if (dyValues != null) c.dyValues = this.dyValues.clone();
		c.legend = this.legend;
		c.xMinimumValue = this.xMinimumValue;
		c.yMinimumValue = this.yMinimumValue;
		if (pointers != null) c.pointers = this.pointers.clone();
		c.pointersAngle = this.pointersAngle;
		c.pointersLabelOffsetFactor = this.pointersLabelOffsetFactor;
		c.regressionType = this.regressionType;
		c.shape = this.shape;
		c.shapeSize = this.shapeSize;
		c.showArrowInPointers = this.showArrowInPointers;
		c.showErrorBars = this.showErrorBars;
		c.showLegend = this.showLegend;
		c.showLines = this.showLines;
		c.showShapes = this.showShapes;
		c.sizeOfArrowInLimits = this.sizeOfArrowInLimits;
		if (stroke != null) c.stroke = this.stroke.clone();
		c.useCustomColorsInPieCharts = this.useCustomColorsInPieCharts;
		if (xValues != null) c.xValues = this.xValues.clone();
		if (yValues != null) c.yValues = this.yValues.clone();
		try { c.enable = this.enable; } catch (Exception exc) {}
		return c;
	}
	/**
	 * Returns true if the input object is equals to this chart object.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ChartSeriesElement)) return false;

		ChartSeriesElement that = (ChartSeriesElement) o;

		if (showLegend != that.showLegend) return false;
		if (useCustomColorsInPieCharts != that.useCustomColorsInPieCharts) return false;
		if (showShapes != that.showShapes) return false;
		if (showLines != that.showLines) return false;
		if (shapeSize != that.shapeSize) return false;
		if (sizeOfArrowInLimits != that.sizeOfArrowInLimits) return false;
		if (showArrowInPointers != that.showArrowInPointers) return false;
		if (showErrorBars != that.showErrorBars) return false;
		if (Float.compare(that.pointersLabelOffsetFactor, pointersLabelOffsetFactor) != 0) return false;
		if (Double.compare(that.xMinimumValue, xMinimumValue) != 0) return false;
		if (Double.compare(that.yMinimumValue, yMinimumValue) != 0) return false;
		if (enable != that.enable) return false;

		if (!Arrays.equals(xValues, that.xValues)) return false;

		if (!Arrays.equals(yValues, that.yValues)) return false;
		if (!Arrays.equals(dxValues, that.dxValues)) return false;
		if (!Arrays.equals(dyValues, that.dyValues)) return false;
		if (legend != null ? !legend.equals(that.legend) : that.legend != null) return false;
		if (color != null ? !color.equals(that.color) : that.color != null) return false;

		if (!Arrays.equals(colorsForPieCharts, that.colorsForPieCharts)) return false;
		if (shape != null ? !shape.equals(that.shape) : that.shape != null) return false;
		if (regressionType != that.regressionType) return false;
		if (stroke != null ? !stroke.equals(that.stroke) : that.stroke != null) return false;

		if (!Arrays.equals(pointers, that.pointers)) return false;

		return pointersAngle == that.pointersAngle;
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		result = xValues != null ? Arrays.hashCode(xValues) : 0;
		result = 31 * result + (yValues != null ? Arrays.hashCode(yValues) : 0);
		result = 31 * result + (dxValues != null ? Arrays.hashCode(dxValues) : 0);
		result = 31 * result + (dyValues != null ? Arrays.hashCode(dyValues) : 0);
		result = 31 * result + (legend != null ? legend.hashCode() : 0);
		result = 31 * result + (showLegend ? 1 : 0);
		result = 31 * result + (color != null ? color.hashCode() : 0);
		result = 31 * result + (colorsForPieCharts != null ? Arrays.hashCode(colorsForPieCharts) : 0);
		result = 31 * result + (useCustomColorsInPieCharts ? 1 : 0);
		result = 31 * result + (shape != null ? shape.hashCode() : 0);
		result = 31 * result + (showShapes ? 1 : 0);
		result = 31 * result + (showLines ? 1 : 0);
		result = 31 * result + (regressionType != null ? regressionType.hashCode() : 0);
		result = 31 * result + shapeSize;
		result = 31 * result + sizeOfArrowInLimits;
		result = 31 * result + (stroke != null ? stroke.hashCode() : 0);
		result = 31 * result + (pointers != null ? Arrays.hashCode(pointers) : 0);
		result = 31 * result + (pointersAngle != null ? pointersAngle.hashCode() : 0);
		result = 31 * result + (showArrowInPointers ? 1 : 0);
		result = 31 * result + (showErrorBars ? 1 : 0);
		result = 31 * result + (pointersLabelOffsetFactor != +0.0f ? Float.floatToIntBits(pointersLabelOffsetFactor) : 0);
		temp = Double.doubleToLongBits(xMinimumValue);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(yMinimumValue);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (enable ? 1 : 0);
		return result;
	}

	/**
	 * Tests whether two shapes are similar or not.
	 * @param s1 One shape.
	 * @param s2 Another shape.
	 * @return True if they are similar.
	 */
	public static boolean equalShapes(java.awt.Shape s1, java.awt.Shape s2)
	{
		Rectangle2D r1 = s1.getBounds2D();
		Rectangle2D r2 = s2.getBounds2D();
		int minx1 = (int) (r1.getMinX() - 1);
		int minx2 = (int) (r2.getMinX() - 1);
		int maxx1 = (int) (r1.getMaxX() + 1);
		int maxx2 = (int) (r2.getMaxX() + 1);
		int minx = minx1;
		if (minx2 < minx1) minx = minx2;
		int maxx = maxx1;
		if (maxx2 > maxx1) maxx = maxx2;

		int miny1 = (int) (r1.getMinY() - 1);
		int miny2 = (int) (r2.getMinY() - 1);
		int maxy1 = (int) (r1.getMaxY() + 1);
		int maxy2 = (int) (r2.getMaxY() + 1);
		int miny = miny1;
		if (miny2 < miny1) miny = miny2;
		int maxy = maxy1;
		if (maxy2 > maxy1) maxy = maxy2;

		boolean equal = true;
		for (int i=minx*2; i<=maxx*2; i++)
		{
			for (int j=miny*2; j<=maxy*2; j++)
			{
				if (s1.contains(i, j) && !s2.contains(i, j)) {
					equal = false;
					break;
				}
				if (!s1.contains(i, j) && s2.contains(i, j)) {
					equal = false;
					break;
				}
			}
		}
		return equal;
	}

	/**
	 * Returns the characteristic name of this instance.
	 * @return The characteristic name, currently equals to the legend.
	 */
	public String getInstanceName()
	{
		return this.legend;
	}

	private static Polygon shapeFor(int shape, int boxX, int boxY) {
        if (shape==0) {
            Polygon p = new Polygon();
            int unit = 7*boxX/54;
            p.addPoint(-unit,-unit);
            p.addPoint(-unit,-3*unit);
            p.addPoint(unit,-3*unit);
            p.addPoint(unit,-unit);
            p.addPoint(3*unit,-unit);
            p.addPoint(3*unit,unit);
            p.addPoint(unit,unit);
            p.addPoint(unit,3*unit);
            p.addPoint(-unit,3*unit);
            p.addPoint(-unit,unit);
            p.addPoint(-3*unit,unit);
            p.addPoint(-3*unit,-unit);
            return p;

        } else if (shape == 1) {
            int unit = 7*boxX/22;
            Polygon p = new Polygon();
            p.addPoint(-unit, -unit);
            p.addPoint(unit, -unit);
            p.addPoint(unit, unit);
            p.addPoint(-unit, unit);
            return p;
        } else if (shape == 2) {
            double angle = Math.PI/2;
            double angleStep = Math.PI/5;
            double radius = Math.sqrt(Math.pow((7*boxX/22),2)+Math.pow((7*boxY/22),2));
            Polygon p = new Polygon();
            for (int i = 0; i < 10 ; i++) {
                int x = (int) (Math.cos(angle)*radius);
                int y = (int) (Math.sin(angle)*radius);
                if (i%2 == 0) {
                    x = 11*x/23;
                    y = 11*y/23;

                }
                p.addPoint(x,y);
                angle += angleStep;
            }
            return p;
        } else if (shape == 3) {
            double angle = -Math.PI/2;
            double angleStep = 2*Math.PI/5;
            double radius = Math.sqrt(Math.pow((7*boxX/22),2)+Math.pow((7*boxY/22),2));
            Polygon p = new Polygon();
            for (int i = 0; i < 5 ; i++) {
                int x = (int) (Math.cos(angle)*radius);
                int y = (int) (Math.sin(angle)*radius);

                p.addPoint(x,y);
                angle += angleStep;
            }
            return p;
        } else if (shape == 4) {
            double angle = Math.PI/2;
            double angleStep = Math.PI/4;
            double radius = Math.sqrt(Math.pow((7*boxX/22),2)+Math.pow((7*boxY/22),2));
            Polygon p = new Polygon();
            for (int i = 0; i < 8 ; i++) {
                int x = (int) (Math.cos(angle)*radius);
                int y = (int) (Math.sin(angle)*radius);
                if (i%2 == 0) {
                    x = 11*x/23;
                    y = 11*y/23;

                }
                p.addPoint(x,y);
                angle += angleStep;
            }
            return p;
        } else if (shape == 5) {
            int unitx = 8*boxX/22;
            int unity = 3*unitx/4;
            Polygon p = new Polygon();
            p.addPoint(-unitx,-unity+3);
            p.addPoint(-unitx+3, -unity);
            p.addPoint(unitx-3, -unity);
            p.addPoint(unitx, -unity+3);
            p.addPoint(unitx, unity-3);
            p.addPoint(unitx-3, unity);
            p.addPoint(-unitx+3, unity);
            p.addPoint(-unitx, unity-3);
            return p;
        } else if (shape == 6) {
            int unitx = boxX/14;
            int unity = 5*unitx;
            Polygon p = new Polygon();
            p.addPoint(-unitx,-unity);
            p.addPoint(5*unitx, -unity);
            p.addPoint(unitx, unity);
            p.addPoint(-5*unitx, unity);
            return p;
        }

        return null;
    }

	/**
	 * Returns a set of series for a given set of values for x, y, and their errors. The array
	 * of legends is used to identify to which series a given point belongs, so the number
	 * of series produced depends on the number of different elements in this array.
	 * @param x The array of x values.
	 * @param y The array of y values.
	 * @param dx The array of x errors. Can be null.
	 * @param dy The array of y errors. Can be null.
	 * @param legends The array of legends for each point.
	 * @param changeShape True to change the shape for each series, false to change the color.
	 * @return The array of series.
	 * @throws JPARSECException If an error occurs.
	 */
	public static ChartSeriesElement[] getSeriesFromDataSets(String x[], String y[], double dx[], double dy[], String legends[], boolean changeShape)
	throws JPARSECException {
		String legend[] = DataSet.getDifferentElements(legends);
		ChartSeriesElement series[] = new ChartSeriesElement[legend.length];
		for (int i=0; i<legend.length; i++)
		{
			int indexes[] = DataSet.getRepeatedElements(legends, legend[i]);
			String vx[] = new String[indexes.length];
			String vy[] = new String[indexes.length];
			double vdx[] = new double[indexes.length];
			double vdy[] = new double[indexes.length];
			if (dx == null) vdx = null;
			if (dy == null) vdy = null;
			for (int j=0; j<indexes.length; j++)
			{
				vx[j] =x[indexes[j]];
				vy[j] =y[indexes[j]];
				if (dx != null) vdx[j] =dx[indexes[j]];
				if (dy != null) vdy[j] =dy[indexes[j]];
			}

			Color col = Color.BLACK;
			Shape shape = ChartSeriesElement.SHAPE_CIRCLE;
			if (changeShape) {
				int index = i % SHAPES.length;
				shape = SHAPES[index];
			} else {
				int index = i % COLORS.length;
				col = COLORS[index];
			}

			series[i] = new ChartSeriesElement(
					vx, vy, vdx, vdy,
					legend[i], true, col, shape,
					ChartSeriesElement.REGRESSION.NONE);
		}
		return series;
	}

	/**
	 * Return the index of certain shape in the array of available ones.
	 * @param shape A shape.
	 * @return The index of the shape which is equal to this one, or -1 if no
	 * match is found.
	 */
	public static int getShape(Shape shape)
	{
		int index = -1;
		for (int i=0; i<ChartSeriesElement.SHAPES.length; i++)
		{
			if (ChartSeriesElement.equalShapes(shape, ChartSeriesElement.SHAPES[i])) {
				index = i;
				break;
			}
		}
		return index;
	}
}
