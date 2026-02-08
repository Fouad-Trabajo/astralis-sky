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

import jparsec.graph.ChartSeriesElement;
import jparsec.graph.DataSet;
import jparsec.util.JPARSECException;

/**
 * A class for fitting data to a straight line in a very rigorous way.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class LinearFit implements Serializable
{
	private static final long serialVersionUID = 1L;

	private double x[], y[], dx[], dy[];

	/**
	 * Default constructor.
	 * @param x X values.
	 * @param y Y values.
	 * @param dx DX values.
	 * @param dy DY values.
	 */
	public LinearFit(double x[], double y[], double dx[], double dy[])	{
		if (x != null) this.x = x.clone();
		if (y != null) this.y = y.clone();
		if (dx != null) this.dx = dx.clone();
		if (dy != null) this.dy = dy.clone();
	}

	/**
	 * Default constructor with no error bars.
	 * @param x X values.
	 * @param y Y values.
	 */
	public LinearFit(double x[], double y[])	{
		if (x != null) this.x = x.clone();
		if (y != null) this.y = y.clone();
		this.dx = null;
		this.dy = null;
	}

	/**
	 * Constructor that forces the slope to certain value.
	 * @param x X values.
	 * @param y Y values.
	 * @param dx DX values.
	 * @param dy DY values.
	 * @param slope Slope to force.
	 */
	public LinearFit(double x[], double y[], double dx[], double dy[], double slope)	{
		if (x != null) this.x = x.clone();
		if (y != null) this.y = y.clone();
		if (dx != null) this.dx = dx.clone();
		if (dy != null) this.dy = dy.clone();
		forceSlopeToValue = slope;
		forceSlope = true;
	}

	/**
	 * Correlation factor. 1 means a perfect fit.
	 */
	public double correlation = 0.0;

	/**
	 * Slope parameter for a linear fit.
	 */
	public double slope = 0.0;

	/**
	 * Value of the fitting straight line in x = 0.
	 */
	public double valueInXEqualToZero = 0.0;

	/**
	 * Slope error.
	 */
	public double dslope = 0.0;

	/**
	 * Error in value for x = 0.
	 */
	public double dvalueInXEqualToZero = 0.0;

	/**
	 * Individual estimated fit errors in each point y.
	 */
	public double[] fittingErrors;

	/**
	 * Value of slope and value_in_x_equal_to_zero when the fit is invalid.
	 */
	public static final double INVALID_FIT = -1E5;

	/**
	 * Value of the slope to be imposed.
	 */
	public double forceSlopeToValue = 0.0;

	/**
	 * True to force the value of the slope to certain vale.
	 */
	public boolean forceSlope = false;

	/**
	 * Linear fit of data. Results set to static constants and arrays. Number of
	 * points should be 2 or more, or 2 with a forced value of the slope parameter.
	 *
	 * @return The series representing the linear fit.
	 * @throws JPARSECException If the number of points is lower than 2 and the slope
	 * is not forced.
	 */
	public ChartSeriesElement linearFit() throws JPARSECException
	{
		if (x.length < 1)
		{
			this.slope = LinearFit.INVALID_FIT;
			this.valueInXEqualToZero = LinearFit.INVALID_FIT;
			throw new JPARSECException("the number of points should be 2 or more.");
		}
		if (x.length == 1)
		{
			if (this.forceSlope)
			{
				slope = this.forceSlopeToValue;
			} else
			{
				throw new JPARSECException("the number of points should be 2 or more.");
			}
			this.dslope = 0.0;
			this.valueInXEqualToZero = 0.0;
			this.dvalueInXEqualToZero = 0.0;
			this.valueInXEqualToZero = y[0] - this.evaluateFittingFunction(x[0]);
			JPARSECException.addWarning("fit adjusted using the default slope value.");
		} else
		{
			// Variable declarations
			double sumx, sumy, sumxy, sumx2, sumx2y, sum1, sum2, sum3, dxm, dym;
			double xm, ym, s;

			// Set variables to null. I know it is not necessary, but ...
			sumx = 0.0;
			sumy = 0.0;
			sumxy = 0.0;
			sumx2 = 0.0;
			sumx2y = 0.0;
			sum1 = 0.0;
			sum2 = 0.0;
			sum3 = 0.0;
			dxm = 0.0;
			dym = 0.0;

			// Sums and mean values
			int nfit = x.length;
			fittingErrors = new double[nfit];
			for (int i = 0; i < nfit; i++)
			{
				sumx = sumx + x[i];
				sumy = sumy + y[i];
				sumxy = sumxy + x[i] * y[i];
				sumx2 = sumx2 + x[i] * x[i];
				sumx2y = sumx2y + x[i] * x[i] * y[i];
				if (dx != null)
					dxm = dxm + dx[i] * dx[i];
				if (dy != null)
					dym = dym + dy[i] * dy[i];
			}

			xm = sumx / nfit;
			dxm = Math.sqrt(dxm);
			ym = sumy / nfit;
			dym = Math.sqrt(dym);

			for (int i = 0; i < nfit; i++)
			{
				sum1 = sum1 + (x[i] - xm) * (x[i] - xm);
				sum2 = sum2 + (y[i] - ym) * (y[i] - ym);
			}

			// Calculate slope, y0, errors, correlation, and vertical fitting
			// errors
			s = nfit * sumx2 - sumx * sumx;
			if (s == 0.0)
			{
				// PROBLEM, infinite slope. Approximating
				s = s + .0000000001;
				JPARSECException
						.addWarning("infinite slope found in fit. Value will be approximated, so fit results should be taken with caution.");
			}

			slope = (nfit * sumxy - sumx * sumy) / s;
			if (this.forceSlope)
				slope = this.forceSlopeToValue;
			valueInXEqualToZero = ym - slope * xm;
			for (int i = 0; i < nfit; i++)
			{
				fittingErrors[i] = 0.0;
				sum3 = sum3 + (y[i] - (slope * x[i] + valueInXEqualToZero)) * (y[i] - (slope * x[i] + valueInXEqualToZero));
			}

			if (sum1 == 0.0)
			{
				// PROBLEM, infinite slope error. Approximating
				sum1 = sum1 + .0000000001;
				JPARSECException
						.addWarning("infinite slope error found in fit. Value will be approximated.");
			}

			// Of course, we can only calculate slope, y0, and vertical fitting
			// errors if we have more than 2 points
			dslope = 0.0;
			dvalueInXEqualToZero = 0.0;
			if (nfit > 2)
			{
				dslope = Math.sqrt(sum3 / (sum1 * (nfit - 2.0)));
				dvalueInXEqualToZero = Math.sqrt((1.0 / nfit + xm * xm / sum1) * sum3 / (nfit - 2));
				for (int i = 0; i < nfit; i++)
				{
					fittingErrors[i] = Math
							.sqrt(dvalueInXEqualToZero * dvalueInXEqualToZero + (x[i] - xm) * (x[i] - xm) * dslope * dslope);
				}
			}

			// If we have two points, we can estimate the errors
			if (nfit == 2)
			{
				double dmy = 0.0;
				if (dy != null)
					dmy = dy[0] + dy[1];
				double dmx = 0.0;
				if (dx != null)
					dmx = dx[0] + dx[1];
				double dm1 = (y[0] - y[1] + dmy) / (x[0] - x[1] - dmx);
				double dm2 = (y[0] - y[1] - dmy) / (x[0] - x[1] + dmx);

				dslope = Math.abs(slope - dm1);
				dm1 = Math.abs(slope - dm2);
				if (dm1 > dslope)
					dslope = dm1;

				double dn1 = y[0] - (slope + dslope) * x[0];
				dvalueInXEqualToZero = y[0] - (slope - dslope) * x[0];
				dvalueInXEqualToZero = Math.abs(dvalueInXEqualToZero - dn1);
				dn1 = y[1] - (slope + dslope) * x[1];
				dn1 = y[1] - (slope - dslope) * x[1] - dn1;
				dn1 = Math.abs(dn1);

				// Method 2: dn is very little
				// double dn1 = Math.abs(dy_fit[0]*dy_fit[0] +
				// m*m*dx_fit[0]*dx_fit[0] + x_fit[0]*x_fit[0]*dm*dm);
				// dn = Math.abs(dy_fit[1]*dy_fit[1] + m*m*dx_fit[1]*dx_fit[1] +
				// x_fit[1]*x_fit[1]*dm*dm);

				if (dn1 > dvalueInXEqualToZero)
					dvalueInXEqualToZero = dn1;
			}

			// Calculate correlation
			correlation = 1.0;
			if (!(sum2 == 0.0))
				correlation = Math.abs(slope * Math.sqrt(sum1 / sum2));
		}

		ChartSeriesElement series = new ChartSeriesElement(
				x, y, dx, dy,
				"", true, Color.BLACK, ChartSeriesElement.SHAPE_CIRCLE,
				ChartSeriesElement.REGRESSION.LINEAR);
		series.regressionType.setEquationValues(new double[] {valueInXEqualToZero, slope}, new double[] {dvalueInXEqualToZero, dslope});
		series.regressionType.setShowEquation(true);
		series.regressionType.setShowRegression(true);
		return series;
	}

	/**
	 * Evaluate fitting line in some point. A fit should previously be done.
	 *
	 * @param x Point to evaluate.
	 * @return Result of evaluation. 0.0 if fit is invalid.
	 */
	public double evaluateFittingFunction(double x)
	{
		if (this.isInvalid())
			return 0.0;

		double y = valueInXEqualToZero + x * slope;

		return y;
	}

	/**
	 * Evaluate abscissa in some point. A fit should previously be done.
	 *
	 * @param y Y point.
	 * @return x for f(x) = y. 0.0 if fit is invalid.
	 */
	public double evaluateAbcissa(double y)
	{
		if (this.isInvalid())
			return 0.0;

		double x = (y - valueInXEqualToZero) / slope;

		return x;
	}

	/**
	 * Evaluate fitting error in some point. A fit should previously be done.
	 *
	 * @param x Point to evaluate.
	 * @return Result of evaluation. 0.0 if fit is invalid.
	 */
	public double evaluateFittingFunctionError(double x)
	{
		if (this.isInvalid())
			return 0.0;

		double dy = Math.sqrt(Math.pow(dvalueInXEqualToZero, 2.0) + Math.pow(x * dslope, 2.0));

		return dy;
	}

	/**
	 * Checks if the fit is invalid.
	 *
	 * @return True if it is invalid.
	 */
	public boolean isInvalid()
	{
		boolean invalid = false;
		if (this.slope == LinearFit.INVALID_FIT && this.valueInXEqualToZero == LinearFit.INVALID_FIT)
			invalid = true;
		return invalid;
	}

	/**
	 * Clear all forced variables, setting them to false and invalid_fit value.
	 */
	public void clear()
	{
		this.forceSlope = false;
		this.forceSlopeToValue = LinearFit.INVALID_FIT;
	}

	/**
	 * Obtains error points, defined as those points in the fit that lies
	 * outside the fitting line, taking into account the errors in the
	 * slope and y value in x equal to 0.
	 *
	 * @param useDiagonal True to use the diagonal to check for errors in the
	 * rectangle error region of each point, false to use individual lengths
	 * of x and y errors.
	 * @param useFittingErrors True to use fitting errors for y axis, false to
	 *        use original y errors. False is commonly used here.
	 * @return An array of integer values representing the point indexes that
	 *         lies outside the fitting line. Array is ordered by descent order
	 *         of error, so the first point will be the worst fitted one. You can then
	 *         eliminate it, repeat the fit, and check for more points outside the new
	 *         fit. Null is returned if no point lies outside the fit.
	 */
	public int[] getInvalidPoints(boolean useDiagonal, boolean useFittingErrors)
	{
		double dx[] = DataSet.getSetOfValues(0.0, 0.0, x.length, false);
		double dy[] = DataSet.getSetOfValues(0.0, 0.0, x.length, false);
		if (this.dx != null) dx = this.dx;
		if (this.dy != null) dy = this.dy;

		int np = x.length;
		double erx[] = new double[np];
		double ery[] = new double[np];
		int p = -1;

		double alfa = Math.atan(this.slope);
		for (int i = 0; i < x.length; i++)
		{
			// Distance point - fitting line
			double dir = this.evaluateFittingFunction(x[i]) - y[i];
			dir = dir / Math.sqrt(this.slope * this.slope + 1.0);

			// Distance in each axis
			double diry = Math.abs(dir * Math.cos(alfa));
			double dirx = Math.abs(dir * Math.sin(alfa));

			// Apply criteria
			if (useDiagonal && useFittingErrors)
			{
				if (dirx * diry > dx[i] * this.fittingErrors[i])
				{
					p++;
					erx[p] = i;
					ery[p] = dx[i] * this.fittingErrors[i] - dirx * diry;
				}
			}
			if (useDiagonal && !useFittingErrors)
			{
				if (dirx * diry > dx[i] * dy[i])
				{
					p++;
					erx[p] = i;
					ery[p] = dx[i] * dy[i] - dirx * diry;
				}
			}
			if (!useDiagonal && useFittingErrors)
			{
				if (dirx > dx[i] || diry > this.fittingErrors[i])
				{
					p++;
					erx[p] = i;
					ery[p] = dx[i] - dirx;
					if ((this.fittingErrors[i] - diry) > ery[p])
						ery[p] = this.fittingErrors[i] - diry;
				}
			}
			if (!useDiagonal && !useFittingErrors)
			{
				if (dirx > dx[i] || diry > dy[i])
				{
					p++;
					erx[p] = i;
					ery[p] = dx[i] - dirx;
					if ((dy[i] - diry) > ery[p])
						ery[p] = dy[i] - diry;
				}
			}
		}

		if (p <= 0) return null;

		// Copy to an adequate sized array
		double erxok[] = new double[p];
		double eryok[] = new double[p];
		for (int i = 0; i < p; i++)
		{
			erxok[i] = erx[i];
			eryok[i] = Math.abs(ery[i]);
		}

		// Order by crescent order of error
		ArrayList<double[]> v = DataSet.sortInCrescent(eryok, erxok, false);
		ery = v.get(0);
		erx = v.get(1);

		// Obtain points in descent order of error
		int errorp[] = new int[p];
		for (int i = 0; i < p; i++)
		{
			errorp[i] = (int) erx[p - 1 - i];
		}

		return errorp;
	}

	/**
	 * Obtains error points, defined as those points in the fit that lies
	 * outside the fitting line, taking into account the errors in the
	 * slope and y value in x equal to 0. This method returns a value
	 * proportional to the discrepancies between the fitting line and the point
	 * location.
	 *
	 * @param useDiagonal True to use the diagonal to check for errors in the
	 * rectangle error region of each point, false to use individual lengths
	 * of x and y errors.
	 * @param useFittingErrors True to use fitting errors for y axis, false to
	 *        use original y errors. False is commonly used here.
	 * @return An array of double values representing the errors of the points that
	 *         lies outside the fitting line. First value corresponds to the point with
	 *         the maximum fitting error.
	 */
	public double[] getInvalidPointsErrors(boolean useDiagonal, boolean useFittingErrors)
	{
		double dx[] = DataSet.getSetOfValues(0.0, 0.0, x.length, false);
		double dy[] = DataSet.getSetOfValues(0.0, 0.0, x.length, false);
		if (this.dx != null) dx = this.dx;
		if (this.dy != null) dy = this.dy;

		int np = x.length;
		double erx[] = new double[np];
		double ery[] = new double[np];
		int p = -1;

		double alfa = Math.atan(this.slope);
		for (int i = 0; i < x.length; i++)
		{
			// Distance point - fitting line
			double dir = this.evaluateFittingFunction(x[i]) - y[i];
			dir = dir / Math.sqrt(this.slope * this.slope + 1.0);

			// Distance in each axis
			double diry = Math.abs(dir * Math.cos(alfa));
			double dirx = Math.abs(dir * Math.sin(alfa));

			// Apply criteria
			if (useDiagonal && useFittingErrors)
			{
				if (dirx * diry > dx[i] * this.fittingErrors[i])
				{
					p++;
					erx[p] = i;
					ery[p] = dx[i] * this.fittingErrors[i] - dirx * diry;
				}
			}
			if (useDiagonal && !useFittingErrors)
			{
				if (dirx * diry > dx[i] * dy[i])
				{
					p++;
					erx[p] = i;
					ery[p] = dx[i] * dy[i] - dirx * diry;
				}
			}
			if (!useDiagonal && useFittingErrors)
			{
				if (dirx > dx[i] || diry > this.fittingErrors[i])
				{
					p++;
					erx[p] = i;
					ery[p] = dx[i] - dirx;
					if ((this.fittingErrors[i] - diry) > ery[p])
						ery[p] = this.fittingErrors[i] - diry;
				}
			}
			if (!useDiagonal && !useFittingErrors)
			{
				if (dirx > dx[i] || diry > dy[i])
				{
					p++;
					erx[p] = i;
					ery[p] = dx[i] - dirx;
					if ((dy[i] - diry) > ery[p])
						ery[p] = dy[i] - diry;
				}
			}
		}

		if (p <= 0) return null;

		// Copy to an adequate sized array
		double erxok[] = new double[p];
		double eryok[] = new double[p];
		for (int i = 0; i < p; i++)
		{
			erxok[i] = erx[i];
			eryok[i] = Math.abs(ery[i]);
		}

		// Order by crescent order of error
		ArrayList<double[]> v = DataSet.sortInCrescent(eryok, erxok, false);
		ery = v.get(0);
		erx = v.get(1);

		// Obtain points in descent order of error
		double errorp[] = new double[p];
		for (int i = 0; i < p; i++)
		{
			errorp[i] = ery[p - 1 - i];
		}

		return errorp;
	}

	/**
	 * The set of possible variable to modify during the search
	 * for maximum correlation.
	 */
	public enum MAX_CORRELATION {
		/** Take x values greater than a given value provided to the maximum correlation search method . */
		TAKE_POINTS_WITH_X_GREATER,
		/** Take x values lower than a given value provided to the maximum correlation search method . */
		TAKE_POINTS_WITH_X_LOWER,
		/** Take y values greater than a given value provided to the maximum correlation search method . */
		TAKE_POINTS_WITH_Y_GREATER,
		/** Take y values lower than a given value provided to the maximum correlation search method . */
		TAKE_POINTS_WITH_Y_LOWER
	};

	/**
	 * Searches for the maximum correlation in the input data by modifying the range of that data
	 * (x or y values) and considering only points with x or y greater or lower than a given range of values.
	 * @param mc A value indication which variable will be used to modify the range of the input
	 * data contained in this instance. In case of using, for instance, to take points with y greater than
	 * a given value, the search for maximum correlation will take all points which y coordinate greater
	 * than the value specified in the next input values, for which an interation will be made.
	 * @param minV The minimum value for the variable to be modified. In the previous example, the first
	 * y value to be used to take, in the first iteration, all points with y greater than this value.
	 * @param maxV The maximum value for the variable to be modified. In the previous example, the last
	 * y value to be used to take, in the last iteration, all points with y greater than this value.
	 * @param step The step in the search to go from the minimum to the maximum values.
	 * @return Two objects representing the measured point of the maximum correlation in x axis, and in
	 * y axis.
	 * @throws JPARSECException If an error occurs.
	 */
	public MeasureElement[] getMaximumCorrelation(MAX_CORRELATION mc, double minV, double maxV, double step)
			throws JPARSECException {
		double maxC = -1, m1Error = step * 0.5;
		MeasureElement m1 = null, m2 = null;
		for (double value = minV; value <= maxV; value += step) {
			ArrayList<double[]> data = null;
			switch (mc) {
			case TAKE_POINTS_WITH_X_GREATER:
				data = DataSet.subDatasetFromXMinimum(x, y, dx, dy, value);
				break;
			case TAKE_POINTS_WITH_X_LOWER:
				data = DataSet.subDatasetFromXMaximum(x, y, dx, dy, value);
				break;
			case TAKE_POINTS_WITH_Y_GREATER:
				data = DataSet.subDatasetFromYMinimum(x, y, dx, dy, value);
				break;
			case TAKE_POINTS_WITH_Y_LOWER:
				data = DataSet.subDatasetFromYMaximum(x, y, dx, dy, value);
				break;
			}
			LinearFit lf = new LinearFit(data.get(0), data.get(1));
			lf.linearFit();
			if (lf.correlation > maxC || maxC == -1) {
				m1 = new MeasureElement(value, m1Error, null);
				maxC = lf.correlation;
				if (mc == MAX_CORRELATION.TAKE_POINTS_WITH_Y_GREATER || mc == MAX_CORRELATION.TAKE_POINTS_WITH_Y_LOWER) {
					data = DataSet.subDatasetFromYMinimum(data.get(0), data.get(1), null, null, value-m1Error);
					data = DataSet.subDatasetFromYMaximum(data.get(0), data.get(1), null, null, value+m1Error);
					m2 = null;
					if (data.get(0) != null && data.get(0).length > 0) {
						MeanValue mean = new MeanValue(data.get(0), null);
						m2 = mean.getMeasuredAverageValue();
					}
				} else {
					data = DataSet.subDatasetFromXMinimum(data.get(0), data.get(1), null, null, value-m1Error);
					data = DataSet.subDatasetFromXMaximum(data.get(0), data.get(1), null, null, value+m1Error);
					m2 = null;
					if (data.get(1) != null && data.get(1).length > 0) {
						MeanValue mean = new MeanValue(data.get(1), null);
						m2 = mean.getMeasuredAverageValue();
					}
				}
			}
		}
		if (mc == MAX_CORRELATION.TAKE_POINTS_WITH_Y_GREATER || mc == MAX_CORRELATION.TAKE_POINTS_WITH_Y_LOWER) {
			return new MeasureElement[] {m2, m1};
		} else {
			return new MeasureElement[] {m1, m2};
		}
	}
}
