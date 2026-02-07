/*
 * This file is part of JPARSEC library.
 *
 * (C) Copyright 2006-2011 by T. Alonso Albi - OAN (Spain).
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

import java.io.Serializable;

import jparsec.ephem.Functions;
import jparsec.graph.DataSet;
import jparsec.util.JPARSECException;

/**
 * Performs ponderation operation.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class MeanValue implements Serializable
{
	private static final long serialVersionUID = 1L;

	private double zx[], dzx[];
	/**
	 * Constructor for a set of measures.
	 * @param x Values of the magnitudes.
	 * @param dx Values of the errors in each measurement. Can be null.
	 */
	public MeanValue(double x[], double dx[])
	{
		zx = x.clone();
		if (dx != null) dzx = dx.clone();
	}

	/**
	 * Constructor for a set of measures.
	 * @param x The set of measurements.
	 * @throws JPARSECException If an error occurs.
	 */
	public MeanValue(MeasureElement x[]) throws JPARSECException
	{
		zx = new double[] {x.length};
		dzx = new double[] {x.length};
		for (int i=0; i< zx.length; i++) {
			zx[i] = x[i].getValue();
			dzx[i] = x[i].error;
		}
	}

	private double mean, meanError, absError, gaussError;
	private int minNumberMeasures;

	/**
	 * Returns most probable value of the magnitude based on each measurement and their errors.
	 * Method {@link #ponderate()} must be called first.
	 * @return Most probable value.
	 */
	public MeasureElement getMeasuredMeanValue() { return new MeasureElement(mean, meanError, null); }

	/**
	 * Returns most probable value of the magnitude based on each measurement and their errors.
	 * Method {@link #ponderate()} must be called first.
	 * @return Most probable value.
	 */
	public double getMeanValue() { return mean; }
	/**
	 * Returns most probable error of the magnitude based on each measurement and their errors.
	 * Method {@link #ponderate()} must be called first.
	 * @return Most probable error.
	 */
	public double getMeanError() { return meanError; }
	/**
	 * Returns the absolute error.
	 * Method {@link #ponderate()} must be called first.
	 * @return Absolute error.
	 */
	public double getAbsoluteError() { return absError; }
	/**
	 * Returns the approximate minimum number of measures recommended to obtain a valid value.
	 * Method {@link #ponderate()} must be called first.
	 * @return Minimum number of measures.
	 */
	public int getMinimumNumberOfMeasuresRecommended() { return minNumberMeasures; }
	/**
	 * Returns the Gaussian error of a distribution that approximately matches the input values.
	 * Method {@link #ponderate()} must be called first.
	 * @return Gaussian error.
	 */
	public double getGaussianError() { return gaussError; }

	/**
	 * Calculates the most probable value of the magnitude based on each measurement and their errors.
	 * @throws JPARSECException If an error occurs.
	 */
	public void ponderate()
	throws JPARSECException {
		if (zx.length == 1)
		{
			mean = zx[0];
			meanError = dzx[0];
			absError = 0;
			gaussError = 0;
			minNumberMeasures = 0;
			return;
		}

		// Check errors
		int num = zx.length;
		int zeroErr = 0;
		for (int i = 0; i < num; i++) {
			if (dzx[i] == 0.0) zeroErr ++;
		}
		if (zeroErr == num) {
			mean = getAverageValue();
			meanError = 0;
			absError = 0;
			gaussError = 0;
			minNumberMeasures = 0;
			return;
		}

		// Initialize and get sums
		double sum1 = 0, sum2 = 0, maxzx = -1E300, minzx = 1E300;
		double sumzx = 0, sum3 = 0, cuenta = 0, sum4 = 0;
		gaussError = 0;
		maxzx = DataSet.getMaximumValue(zx);
		minzx = DataSet.getMinimumValue(zx);
		for (int i = 0; i < num; i++)
		{
			if (dzx[i] == 0.0) {
				JPARSECException.addWarning("point number "+(i+1)+" has null error (exact!?). This point will be skipped when calculating mean average.");
			} else {
				sumzx = sumzx + zx[i];
				sum1 = sum1 + (zx[i] / (dzx[i] * dzx[i]));
				sum2 = sum2 + (1.0 / (dzx[i] * dzx[i]));
				sum4 = sum4 + dzx[i] * dzx[i];
			}
		}

		// Calculate everything
		mean = sum1 / sum2;
		meanError = 1.0 / Math.sqrt(sum2);
		double d = maxzx - minzx;
		double t = 100 * d * num / sumzx;
		for (int i = 0; i < num; i++)
		{
			sum3 = sum3 + Math.pow(zx[i] - (sumzx / num), 2.0); // Standard deviation
		}
		absError = d;
		minNumberMeasures = 3; // Minimum number of measurements to get a correct value
		if (t >= 2 && t < 8)
		{
			minNumberMeasures = 6;
			absError = d / 4.0;
		}
		if (t >= 8 && t < 15)
		{
			minNumberMeasures = 15;
			absError = Math.sqrt(sum3 / (num - 1));
		}
		if (t >= 15)
			minNumberMeasures = 50;

		// Use a gaussian distribution to account for the absolute error,
		// imposing a probability of 68.3% to find the correct measurement.
		if (t >= 15)
		{
			double cota = Math.floor(.999999999 + num * 68.3 / 100);
			if (cota < num)
			{
				double x = sumzx / num;
				double h = 0;
				double prec = .001; // Arbitrary precission
				do
				{
					cuenta = 0;
					double sgn = 1.0;
					if (x != 0.0)
						sgn = x / Math.abs(x);
					h = h + prec * sgn;
					for (int i = 0; i < num; i++)
					{
						if (zx[i] >= (x - h) && zx[i] <= (x + h))
							cuenta++;
					}
					if (cuenta == 0) prec *= 2;
				} while (cuenta < cota);
				gaussError = h;
			}
		}
	}

	/**
	 * Returns arithmetic mean value of a set of values, defined as the
	 * 0.5 * (maximum + minimum).
	 * @return Mean arithmetic value.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getMeanArithmeticValue()
	throws JPARSECException {
		double m = (DataSet.getMaximumValue(zx) + DataSet.getMinimumValue(zx)) * 0.5;
		return m;
	}

	/**
	 * Returns the mean value of a set of values, defined as the
	 * sum of the components divided by the number of them.
	 * @return Mean value.
	 */
	public double getAverageValue()
	{
		return Functions.sumComponents(zx) / zx.length;
	}

	/**
	 * Returns the median of the input data. In an array of 10
	 * elements this is equivalent to sort them and return the 5th
	 * element.
	 * Errors in the measures are not taken into account.
	 * @return The median.
	 */
	public double getMedian()
	{
		return DataSet.getKthSmallestValue (zx, zx.length, zx.length/2);
	}

	/**
	 * Kappa-sigma clipping algorithm. We start with a robust estimator: the median.
	 * Then, we eliminate the values that are more than sigmas away from
	 * the median. For the remaining values, we calculate the mean and variance,
	 * and iterate (with the mean replacing the median) until no values are eliminated
	 * or the number of iterations is reached. This algorithm is relatively slow but
	 * works well for input arrays of length 10-15.
	 * Errors in the measures are not taken into account.
	 * @param sigmas Number of sigmas from the median/mean, positive. Less sigmas would
	 * take less values.
	 * @param iter Maximum number of iterations. Set to 0 or negative to
	 * iterate until convergency.
	 * @return The result of the sigma clipping average.
	 */
	public double getAverageUsingKappaSigmaClipping(double sigmas, int iter)
	{
		int i, k = 0, r = 1, n = zx.length;
	  	double sum = 0.0, sumsq = 0.0;
	  	double m, s;

	  	for (i=0; i<n; i++) {
	  		sum += zx[i];
	  		sumsq += zx[i] * zx[i];
	  	}
	  	s = Math.abs(sigmas * (sumsq / n - (sum / n) * (sum / n)));
	  	m = DataSet.getKthSmallestValue (zx, zx.length, zx.length/2);
	  	double olds = -1;
	  	
	  	do {
	  		iter --;
	  		sum = 0.0;
	  		sumsq = 0.0;
	  		r = k;
	  		k = 0;
	  		for (i=0; i<n; i++) {
	  			if (zx[i] >= m - s && zx[i] <= m + s) {
	  				sum += zx[i];
	  				sumsq += zx[i]*zx[i];
	  				k++;
	  			}
	  		}
	  		if (k == 0) break;
	  		m = sum / k;
	  		s = Math.abs(sigmas * (sumsq / k - m * m));
	  		if (k == 1 || (olds != -1 && olds == s)) break;
	  		if (iter == 0) break;
	  		olds = s;
	  	} while (k != r);

	  	return m;
	}
	
	/**
	 * Returns the values that lie outside the acceptable range for a sigma-clipping.
	 * @param sigmas Number of sigmas from the median/mean, positive. Less sigmas would
	 * take less values.
	 * @param iter Maximum number of iterations. Set to 0 or negative to
	 * iterate until convergency.
	 * @return The bad values, or null.
	 */
	public double[] getBadValuesUsingKappaSigmaClipping(double sigmas, int iter)
	{
		int i, k = 0, r = 1, n = zx.length;
	  	double sum = 0.0, sumsq = 0.0;
	  	double m, s;

	  	boolean valid[] = new boolean[n];
	  	for (i=0; i<n; i++) {
	  		sum += zx[i];
	  		sumsq += zx[i] * zx[i];
	  		valid[i] = true;
	  	}
	  	s = Math.abs(sigmas * (sumsq / n - (sum / n)*(sum / n)));
	  	m = DataSet.getKthSmallestValue (zx, zx.length, (int)(zx.length/2.0));
	  	double olds = -1;
	  	
	  	do {
	  		iter --;
	  		sum = 0.0;
	  		sumsq = 0.0;
	  		r = k;
	  		k = 0;
	  		for (i=0; i<n; i++) {
	  			valid[i] = false;
	  			if (zx[i] >= m - s && zx[i] <= m + s) {
		  			valid[i] = true;
	  				sum += zx[i];
	  				sumsq += zx[i]*zx[i];
	  				k++;
	  			}
	  		}
	  		if (k == 0) {
	  			valid[0] = true;
	  			zx[0] = m;
	  			k = 1;
	  			break;
	  		}
	  		m = sum / k;
	  		s = Math.abs(sigmas * (sumsq / k - m*m));
	  		if (k == 1 || (olds != -1 && olds == s)) break;
	  		if (iter == 0) break;
	  		olds = s;
	  	} while (k != r);
	  	
	  	double out[] = new double[0];
	  	for (int in=0;in<valid.length;in++) {
	  		if (!valid[in]) out = DataSet.addDoubleArray(out, new double[] {zx[in]});
	  	}
	  	if (out.length == 0) return null;
	  	return out;
	}

	/**
	 * Returns the dispersion in the input values respect the
	 * average of the values.
	 * @return Math.sqrt(SUM((x_i - mean)^2) / n);
	 */
	public double getDispersion() {
		double d = 0, mean = this.getAverageValue();
		for (int i=0; i<zx.length; i++) {
			d += FastMath.pow(zx[i] - mean, 2.0);
		}
		return Math.sqrt(d / zx.length);
	}

	/**
	 * Returns a simple average value using the average value as
	 * measure, and the dispersion as error. To call method
	 * {@link #ponderate()} is not required.
	 * @return Average value and dispersion.
	 */
	public MeasureElement getMeasuredAverageValue() {
		return new MeasureElement(getAverageValue(), getDispersion(), null);
	}
}
