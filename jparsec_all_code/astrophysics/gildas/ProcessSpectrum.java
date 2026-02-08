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
package jparsec.astrophysics.gildas;

import java.awt.Color;
import java.util.ArrayList;
import java.util.TreeMap;

import jparsec.astrophysics.Spectrum;
import jparsec.ephem.Functions;
import jparsec.graph.ChartSeriesElement;
import jparsec.graph.DataSet;
import jparsec.io.CatalogRead;
import jparsec.io.FileIO;
import jparsec.io.image.HeaderElement;
import jparsec.math.Constant;
import jparsec.math.FastMath;
import jparsec.math.MeasureElement;
import jparsec.math.Regression;
import jparsec.util.DataBase;
import jparsec.util.JPARSECException;
import jparsec.util.Logger;
import jparsec.util.Logger.LEVEL;
import jparsec.vo.GeneralQuery;

/**
 * Spectrum reduction. This class contains methods to process a given
 * spectrum and return a set of Gaussians. There are also general methods
 * called 'reduceSpectrum' that performs the reduction process in a fully
 * automatic way. This class can be used in a static way to quickly reduce a
 * spectrum or with an instance in case more control of the reduction
 * process is desired.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class ProcessSpectrum {

	/** The array of residuals after the spectrum is fitted. */
	private double residualV[];
	/** The array of processed spectrum in a given moment, observations at startup and reduced at the end. */
	public double v[];

	/** Holds velocity resolution. */
	private double vres;
	/** Holds the spectrum. */
	private Spectrum30m spectrum;
	/**
	 * Minimum value to consider a given channel to be possibly a bad channel.
	 * Default value is -100, which means that bad channels should have
	 * intensities below this value.
	 */
	public static double minimumValueToConsiderBadChannel = -100;
	/**
	 * List of impossible molecules (those will never be found in
	 * astronomy), empty by default. Note you should insert the list
	 * separated by comma, with the exact name you will find in JPL
	 * and/or COLOGNE catalogs. These molecules will be skipped when
	 * identifying lines with {@linkplain #identifyLine(double, double, double, double, boolean, boolean, boolean, boolean)}.
	 */
	public static String IMPOSSIBLE_MOLECULES = "";
	/**
	 * The sigma factor, 3.0 by default.
	 */
	public static double TIMES_SIGMA = 3.0;

	/**
	 * Sets the maximum number of iterations when computing fitting Gaussians. Default value
	 * is 3000, but could be too high.
	 */
	public static int maximumNumberOfIterationsForNelderAndMeadSimplexInRegressionClass = 3000;

	/**
	 * Constructor for a 30m spectrum.
	 * @param s The spectrum.
	 * @throws JPARSECException If the velocity resolution cannot be read.
	 */
	public ProcessSpectrum(Spectrum30m s) throws JPARSECException {
		spectrum = s.clone();
		v = DataSet.toDoubleArray(fix(s.getSpectrumData().clone()));
		vres = ((HeaderElement) s.get(Gildas30m.VEL_RESOL)).getAsDouble();
	}

	/**
	 * Constructor for a 30m spectrum.
	 * @param s The spectrum.
	 * @throws JPARSECException If an error occurs.
	 */
	public ProcessSpectrum(Spectrum s) throws JPARSECException {
		spectrum = new Spectrum30m(s);
		v = DataSet.toDoubleArray(fix(spectrum.getSpectrumData().clone()));
		vres = ((HeaderElement) spectrum.get(Gildas30m.VEL_RESOL)).getAsDouble();
	}

	/**
	 * Removes bad channels.
	 * @throws JPARSECException If an error occurs.
	 */
	private String removeBadChannels() throws JPARSECException {
		String out="";
		boolean badChannelFound = false;
		double s = getSigma(v);
		do {
			badChannelFound = false;
			int mi = DataSet.getIndexOfMaximum(v);
			double max = v[mi];
			double meanAround = getMeanAround(mi, 2);
			if (max > s * 3.0 && max > meanAround * 20.0) {
				out += ","+mi+"="+v[mi];
				v[mi] = meanAround;
				badChannelFound = true;
				s = getSigma(v);
			}
		} while (badChannelFound);
		s = getSigma(v);
		do {
			badChannelFound = false;
			int mi = DataSet.getIndexOfMinimum(v);
			double min = Math.abs(v[mi]);
			double meanAround = Math.abs(getMeanAround(mi, 2));
			if (min > s * 3.0 && min > meanAround * 3.0) {
				out += ","+mi+"="+v[mi];
				v[mi] = meanAround;
				badChannelFound = true;
				s = getSigma(v);
			}
		} while (badChannelFound);
		return out;
	}
	/**
	 * Fits the greatest line with a Gaussian within a given range of channels.
	 * @return Coefficients of the fit, 8 values: Gaussian x
	 * central position (km/s), Gaussian width (km/s), Gaussian peak (K), Gaussian
	 * area (K km/s), and their errors. Additional values given are the minimum
	 * channel (at v = v0 - w), the maximum channel value, and the frequency in MHz.
	 * @throws JPARSECException If an error occurs.
	 */
	private double[] fitGreatestLine(double[] vi, double sigma, int i00, int i11) throws JPARSECException {
		// Some baseline is needed for a correct fit
		int i0 = i00, i1 = i11;
		int di = i1 - i0;
		if (di < 2) di = 2;
		i0 = i0 - 2*di;
		i1 = i1 + 2*di;
		if (i0<0) i0 = 0;
		if (i1>vi.length-1) i1 = vi.length-1;

		double vv[] = DataSet.getSubArray(vi, i0, i1).clone(); //vi.clone();
		int basen = 100;
		double moreBaseline[] = DataSet.getSetOfValues(0.0, 0.0, basen, false);
		vv = DataSet.addDoubleArray(vv, moreBaseline);
		vv = DataSet.addDoubleArray(moreBaseline, vv);

		double max = DataSet.getMaximumValue(vv), min = DataSet.getMinimumValue(vv);
		boolean negativeLine = false;
		if (min < 0 && max >= 0 && -min > 1.5*max && -min > TIMES_SIGMA * sigma) {
			negativeLine = true;
			for (int i=0; i<vv.length; i++) {
				vv[i] = -vv[i];
			}
		}

		double xArray[] = DataSet.getSetOfValues(1.0, vv.length, vv.length, false);
        double sdArray[] = DataSet.getSetOfValues(sigma, sigma, vv.length, false); // Use sigma as the error in each point
        fitAGaussian(xArray, vv, sdArray);
        double v[] = fitx; // mean, sd, y_scale
        double dv[] = fitdx;
        v[0] += i0 - basen;
        double sqrttwopi = Math.sqrt(Constant.TWO_PI);
        MeasureElement me1 = new MeasureElement(v[1] * sqrttwopi, dv[1] * sqrttwopi, MeasureElement.UNIT_Y_K);
        MeasureElement me2 = new MeasureElement(v[2], dv[2], MeasureElement.UNIT_Y_K);
        me2.divide(me1);
		double g = cte;
		double v0 = Math.abs(vres);
        double out[] = new double[] {
        		getSpectrum().getVelocity(v[0]), v[1] * g * v0, me2.getValue(), v[2] * v0, // v, w, T, A
        		Math.abs(dv[0] * v0), Math.abs(dv[1] * g * v0), Math.abs(me2.error), Math.abs(dv[2] * v0), // errors

        		//getSpectrum().getChannel(getSpectrum().getVelocity(v[0])-v[1]*g*v0*3),
        		//getSpectrum().getChannel(getSpectrum().getVelocity(v[0])+v[1]*g*v0*3),
        		i00, i11,
        		getSpectrum().getFrequency(v[0])
        };

        // The library by flanagan sometimes produces some NaN when calculations are not consistent. This occurs
        // with lines close to 3 sigma. Also, sometimes fit is ok but errors are unrealistic (too little).
        // In these cases, correct the values based on physical arguments.
        if (new Double(out[5]).equals(Double.NaN) && !new Double(out[6]).equals(Double.NaN) && !new Double(out[7]).equals(Double.NaN)) {
    		MeasureElement area = new MeasureElement(out[3], out[7], "");
    		MeasureElement peak = new MeasureElement(out[2], out[6], "");
    		area.divide(peak);
    		out[5] = area.error / 1.064467;
        }
        if (new Double(out[6]).equals(Double.NaN) && !new Double(out[5]).equals(Double.NaN) && !new Double(out[7]).equals(Double.NaN)) {
    		MeasureElement area = new MeasureElement(out[3], out[7], "");
    		MeasureElement width = new MeasureElement(out[1], out[5], "");
    		area.divide(width);
    		out[6] = area.error / 1.064467;
        }
        if (new Double(out[7]).equals(Double.NaN) && !new Double(out[6]).equals(Double.NaN) && !new Double(out[5]).equals(Double.NaN)) {
    		MeasureElement width = new MeasureElement(out[1], out[5], "");
    		MeasureElement peak = new MeasureElement(out[2], out[6], "");
    		width.multiply(peak);
    		out[7] = width.error * 1.064467;
        }

        double minAreaError = sigma * Math.sqrt(Math.abs(out[1] / vres));
        double minPeakError = sigma;
		MeasureElement area = new MeasureElement(out[3], minAreaError, "");
		MeasureElement peak = new MeasureElement(out[2], minPeakError, "");
		area.divide(peak);
		double minWidthError = Math.abs(area.error) / 1.064467; // w

        if (new Double(out[4]).equals(Double.NaN)) out[4] = Math.abs(vres) * 0.5; // v
        if (new Double(out[6]).equals(Double.NaN) || out[6] < minPeakError) out[6] = minPeakError; // t
        if (new Double(out[7]).equals(Double.NaN) || out[7] < minAreaError) out[7] = minAreaError; // a
        if (new Double(out[5]).equals(Double.NaN) || out[5] < minWidthError) out[5] = minWidthError; // w

        if (negativeLine) {
        	out[2] = -Math.abs(out[2]);
        	out[3] = -Math.abs(out[3]);
        }

        out[4] = Math.abs(out[4]);
        out[5] = Math.abs(out[5]);
        out[6] = Math.abs(out[6]);
        out[7] = Math.abs(out[7]);
        return out;
	}

	/**
	 * Returns the input spectrum instance.
	 * @return The spectrum.
	 */
	public Spectrum30m getSpectrum() {
		return spectrum;
	}

	/**
	 * Returns the processed spectrum.
	 * @return The processed spectrum.
	 */
	public double[] getProcessedSpectrum() {
		return v;
	}
	/**
	 * Returns the original spectrum.
	 * @return The original spectrum.
	 */
	public float[] getOriginalSpectrum() {
		return spectrum.getSpectrumData();
	}
	/**
	 * Returns the residual spectrum after fitting lines.
	 * Should be only white noise.
	 * @return The residual spectrum.
	 */
	public double[] getResidualSpectrum() {
		return residualV;
	}

	private double getMeanAround(int mi, int p) {
		double meanAround = 0.0;
		int n = 0;
		for (int i=mi-1;i>=mi-p; i--) {
			if (i >= 0) {
				meanAround += v[i];
				n++;
			}
		}
		for (int i=mi+1;i<=mi+p; i++) {
			if (i <v.length) {
				meanAround += v[i];
				n++;
			}
		}
		meanAround /= n;
		return meanAround;
	}

	/**
	 * Returns an average of the data around a given position.
	 * @param v The data.
	 * @param mi Index of the central position.
	 * @param p Number of positions to consider beyond and before the
	 * previous central position to calculate an average value.
	 * @return Average value.
	 */
	public static double getMeanAround(double v[], int mi, int p) {
		double meanAround = 0.0;
		int n = 0;
		for (int i=mi-1;i>=mi-p; i--) {
			if (i >= 0) {
				meanAround += v[i];
				n++;
			}
		}
		for (int i=mi+1;i<=mi+p; i++) {
			if (i <v.length) {
				meanAround += v[i];
				n++;
			}
		}
		meanAround /= n;
		return meanAround;
	}

	/**
	 * Returns an average of the data around a given position.
	 * @param v The data.
	 * @param mi Index of the central position.
	 * @param p Number of positions to consider beyond and before the
	 * previous central position to calculate an average value.
	 * @return Average value.
	 */
	public static float getMeanAround(float v[], int mi, int p) {
		float meanAround = 0.0f;
		int n = 0;
		for (int i=mi-1;i>=mi-p; i--) {
			if (i >= 0) {
				meanAround += v[i];
				n++;
			}
		}
		for (int i=mi+1;i<=mi+p; i++) {
			if (i <v.length) {
				meanAround += v[i];
				n++;
			}
		}
		meanAround /= n;
		return meanAround;
	}

	/**
	 * Returns the mean deviation of the spectrum in the input array.
	 * @param v Input spectrum.
	 * @return The mean deaviation, defined as the square root of the squares of the
	 * differences between each point and the arithmetic mean value, divided by the
	 * number of points, all inside the square root.
	 */
	public static double getSigma(double[] v) {
		double m = getMean(v, 0, v.length-1);
		double s = 0.0;
		for (int i=0; i<v.length; i++) {
			s += FastMath.pow(v[i]-m, 2.0);
		}
		return Math.sqrt(s/v.length);
	}

	private double getSigma(double[] v, double meanV) {
		double s = 0.0;
		for (int i=0; i<v.length; i++) {
			s += FastMath.pow(v[i]-meanV, 2.0);
		}
		return Math.sqrt(s/v.length);
	}
	// Obtain mean value of the spectrum between two positions
	/**
	 * Returns an average of the data between two positions.
	 * @param v The data.
	 * @param i0 Index of the first position.
	 * @param i1 Index of the last position.
	 * @return Average value.
	 */
	public static double getMean(double[] v, int i0, int i1) {
		double sum = 0.0;
		if (i0 < 0) i0 = 0;
		if (i1 >= v.length) i1 = v.length-1;
		for (int i=i0;i<=i1;i++) {
			if (new Double(v[i]).equals(Double.NaN)) v[i] = 0;
			sum += v[i];
		}
		sum = sum / (i1+1.0-i0);
		return sum;
	}

	/**
	 * Returns a series with the Gaussian fit of a line.
	 * @param g Values of the Gaussian parameters.
	 * @param xUnit Unit for x axis.
	 * @return The series.
	 * @throws JPARSECException If an error occurs.
	 */
	public ChartSeriesElement getGaussianFit(double g[], Spectrum30m.XUNIT xUnit) throws JPARSECException {
		String x[] = new String[v.length];
		String y[] = new String[v.length];
		double g0 = getSpectrum().getChannel(g[0]);

		// Limit the output series to the relevant region
		double g0min = getSpectrum().getChannel(g[0]-g[1]*10);
		double g0max = getSpectrum().getChannel(g[0]+g[1]*10);
		int imin = (int) (g0min+0.5);
		int imax = (int) (g0max+0.5);
		if (imin > imax) {
			int i0 = imin;
			imin = imax;
			imax = i0;
		}
		int di = imax - imin;
		if (di < 10) {
			di = (10 - di) / 2;
			if (imin > di) {
				imin = imin - di;
				imax = imax + di;
			}
		}
		if (imin < 1) imin = 1;
		if (imax > v.length) imax = v.length;

		for (int i=imin; i<=imax; i++) {
			double c = i;
			if (xUnit == Spectrum30m.XUNIT.VELOCITY_KMS) c = getSpectrum().getVelocity(c);
			if (xUnit == Spectrum30m.XUNIT.FREQUENCY_MHZ) c = getSpectrum().getFrequency(c);
			if (xUnit == Spectrum30m.XUNIT.VELOCITY_KMS_CORRECTED) c = getSpectrum().getCorrectedVelocity(c);
			x[i-1] = ""+c;
			y[i-1] = ""+(g[2] / Math.exp(0.5 * FastMath.pow((i - g0) / (g[1] / (vres * cte)), 2.0)));
		}

		x = DataSet.getSubArray(x, imin-1, imax-1);
		y = DataSet.getSubArray(y, imin-1, imax-1);

		ChartSeriesElement s = new ChartSeriesElement(x, y, null, null, "", true, Color.GREEN, ChartSeriesElement.SHAPE_CIRCLE, ChartSeriesElement.REGRESSION.NONE);
		s.showShapes = false;
		s.showLines = true;
		return s;
	}

	/**
	 * Returns a series with the Gaussian fit of a line, where
	 * each value of index i represents the intensity for channel i+1
	 * (first channel is 1 at index 0).
	 * @param g Values of the Gaussian parameters.
	 * @param xUnit Unit for x axis.
	 * @return The series.
	 * @throws JPARSECException If an error occurs.
	 */
	public ChartSeriesElement getFullGaussianFit(double g[], Spectrum30m.XUNIT xUnit) throws JPARSECException {
		double x[] = new double[v.length];
		double y[] = new double[v.length];
		double g0 = getSpectrum().getChannel(g[0]);

		for (int i=1; i<= v.length; i++) {
			double c = i;
			if (xUnit == Spectrum30m.XUNIT.VELOCITY_KMS) c = getSpectrum().getVelocity(c);
			if (xUnit == Spectrum30m.XUNIT.FREQUENCY_MHZ) c = getSpectrum().getFrequency(c);
			if (xUnit == Spectrum30m.XUNIT.VELOCITY_KMS_CORRECTED) c = getSpectrum().getCorrectedVelocity(c);
			x[i-1] = c;
			y[i-1] = (g[2] / Math.exp(0.5 * FastMath.pow((i - g0) / (g[1] / (vres * cte)), 2.0)));
		}

		ChartSeriesElement s = new ChartSeriesElement(x, y, null, null, "", true, Color.GREEN, ChartSeriesElement.SHAPE_CIRCLE, ChartSeriesElement.REGRESSION.NONE);
		s.showShapes = false;
		s.showLines = true;
		return s;
	}

	/**
	 * Returns a set of y values (intensities) for the given line, where
	 * each value of index i represents the intensity for channel i+1
	 * (first channel is 1 at index 0).
	 * @param g Values of the Gaussian parameters.
	 * @return The intensities.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[] getFullGaussianFit(double g[]) throws JPARSECException {
		double y[] = new double[v.length];
		double g0 = getSpectrum().getChannel(g[0]);

		for (int i=1; i<= v.length; i++) { // i a channel number, starts at 1
			y[i-1] = (g[2] / Math.exp(0.5 * FastMath.pow((i - g0) / (g[1] / (vres * cte)), 2.0)));
		}

		return y;
	}

	private static final double cte = 2.0 * Math.sqrt(2.0 * Math.log(2.0));

	/**
	 * Removes a line from a spectrum.
	 * @param v Input/output spectrum as an array.
	 * @param g Parameters of the Gaussian.
	 */
	public void removeLine(double v[], double g[]) {
		double g0 = getSpectrum().getChannel(g[0]);
		for (int i=0; i<v.length; i++) {
			double y = (g[2] / Math.exp(0.5 * FastMath.pow((1.0+i - g0) / (g[1] / (vres * cte)), 2.0)));
			v[i] -= y;
		}
	}

	/**
	 * Removes a line from the current spectrum being reduced.
	 * @param line The line.
	 */
	public void removeLine(SpectrumLine line) {
		double g[] = line.getGaussianParameters();
		double g0 = getSpectrum().getChannel(g[0]);
		for (int i=0; i<v.length; i++) {
			double y = (g[2] / Math.exp(0.5 * FastMath.pow((1.0+i - g0) / (g[1] / (vres * cte)), 2.0)));
			v[i] -= y;
		}
	}

	/**
	 * Clips a set of lines in the reduced spectrum of this instance. The clipping
	 * is done between the minimum and maximum channels for the lines as given in
	 * the line object, or computed from the position and width in case previous
	 * values are 0. An intensity is computed from a smoothed spectrum before and
	 * after each line, and then the intensity within the line is set to a linear
	 * interpolation between both values.
	 * @param line The set of lines to clip.
	 * @throws JPARSECException If an error occurs.
	 */
	public void clipLines(SpectrumLine line[]) throws JPARSECException {
		if (line == null || line.length == 0) return;
		for (int i=0; i<line.length; i++) {
			removeLine(line[i]);
		}
		double v[] = reduceResiduals(5, null);
		for (int i=0; i<line.length; i++) {
			int min = line[i].minChannel, max = line[i].maxChannel;
			if (min == max && min == 0) {
				double g[] = line[i].getGaussianParameters();
				min = (int) getSpectrum().getChannel(g[0] - g[1] * 2);
				max = (int) getSpectrum().getChannel(g[0] + g[1] * 2);
				if (min > max) {
					int tmp = min;
					min = max;
					max = tmp;
				}
				if (min < 0) min = 0;
				if (max >= v.length) max = v.length-1;
			}
			double minV = 0, maxV = 0;
			if (min > 0) minV = v[min-1];
			if (max < v.length - 1) maxV = v[max+1];
			for (int c = min; c >= max; c++) {
				v[c] = minV + (maxV - minV) * (c - min) / (double) (max - min);
			}
		}
	}

	/**
	 * Removes a line from a spectrum, only near its peak.
	 * @param v Input/output spectrum as an array.
	 * @param g Parameters of the Gaussian.
	 * @param t Number of time the width of the line is
	 * considered to remove the spectrum data around.
	 */
	public void removeLine(double v[], double g[], double t) {
		double g0 = getSpectrum().getChannel(g[0]);
		double g1 = getSpectrum().getChannel(g[0]+g[1]);
		int i0 = (int) g0 - (int)Math.abs((g1-g0)*t);
		int i1 = (int) g0 + (int)Math.abs((g1-g0)*t);
		if (i0 < 0) i0 = 0;
		if (i1 >= v.length) i1 = v.length-1;
		for (int i=i0; i<=i1; i++) {
			double y = (g[2] / Math.exp(0.5 * FastMath.pow((1.0+i - g0) / (g[1] / (vres * cte)), 2.0)));
			v[i] -= y;
		}
	}

	private double[] getLineLimits(double v[], double sigma) throws JPARSECException {
		int m = DataSet.getIndexOfMaximum(v), n = m+1;
		if (n >= v.length-1) return null;
		double max = v[m], next = v[n];
		double dif = next - max;
		do {
			n ++;
			next = v[n];
			dif = next - v[n-1];
			if (Math.abs(max-v[n]) > 8*sigma && dif >= 0.0) break;
		} while((dif < 0.0 || Math.abs(max-v[n]) < 3*sigma) && n<v.length-1);
		// FIXME Now Math.abs(max-v[n]) < 3*sigma, before I put v[n] > sigma, which seems a bad idea
		int end = n + 1;// + 5;
		if (end >= v.length) end = v.length - 1;

		n = m-1;
		if (n < 1) return null;
		double prev = v[n];
		dif = prev - max;
		do {
			n --;
			prev = v[n];
			dif = prev - v[n+1];
			if (Math.abs(max-v[n]) > 8*sigma && dif >= 0.0) break;
		} while((dif < 0.0 || Math.abs(max-v[n]) < 3*sigma) && n>0);
		int init = n; // - 5;
		if (init < 0) init = 0;

		return new double[] {max, init, end};
	}

	/**
	 * Fits the lines in a spectrum. The spectrum should be previously
	 * reduced. The method {@linkplain #getLines(ArrayList)} can later be used to return
	 * a set of {@linkplain SpectrumLine} objects with the fitted lines.
	 * @param eliminateNoise True to substract the residual spectrum to the
	 * processed spectrum.
	 * @return A list with the parameters of the Gaussian lines. Each record
	 * contains an array with the following values: Gaussian x
	 * central position (km/s), Gaussian width (km/s), Gaussian peak (K), Gaussian
	 * area (K km/s), and their errors. Additional values given are the minimum
	 * channel (at v = v0 - w), the maximum channel value, and the frequency in MHz.
	 * @throws JPARSECException If an error occurs.
	 */
	public ArrayList<double[]> fitLines(boolean eliminateNoise) throws JPARSECException {
		return fitLines(eliminateNoise, -1);
	}

	/**
	 * Transforms a set of lines fitted (returned as a set of double arrays)
	 * to a set of {@linkplain SpectrumLine} objects.
	 * @param fittedLines The lines fitted, returned as arrays of doubles.
	 * @return The set of {@linkplain SpectrumLine} objects.
	 */
	public SpectrumLine[] getLines(ArrayList<double[]> fittedLines) {
		if (fittedLines == null) return null;
		SpectrumLine line[] = new SpectrumLine[fittedLines.size()];
		for (int i=0; i<line.length; i++) {
			line[i] = new SpectrumLine(fittedLines.get(i));
		}
		return line;
	}

	/**
	 * Fits the lines in a spectrum. The spectrum should be previously
	 * reduced. The method {@linkplain #getLines(ArrayList)} can later be used to return
	 * a set of {@linkplain SpectrumLine} objects with the fitted lines.
	 * @param eliminateNoise True to substract the residual spectrum to the
	 * processed spectrum.
	 * @param maxN Maximum number of lines to return, -1 to return all of them.
	 * @return A list with the parameters of the Gaussian lines. Each record
	 * contains an array with the following values: Gaussian x
	 * central position (km/s), Gaussian width (km/s), Gaussian peak (K), Gaussian
	 * area (K km/s), and their errors. Additional values given are the minimum
	 * channel (at v = v0 - w), the maximum channel value, and the frequency in MHz.
	 * @throws JPARSECException If an error occurs.
	 */
	public ArrayList<double[]> fitLines(boolean eliminateNoise, int maxN) throws JPARSECException {
		ArrayList<double[]> al = new ArrayList<double[]>();
		if (maxN == 0) return al;
		double v[] = this.v.clone(); //fix(this.v.clone());

		double badfit[] = DataSet.getSetOfValues(0.0, 0.0, v.length, false);

		boolean lineUnderNoise = false;
		do {
			double sigma = getSigma(v);
			double[] clonv = v.clone();
			double limits1[] = this.getLineLimits(v, sigma);
			double limits2[] = this.getLineLimits(Functions.scalarProduct(v, -1.0), sigma);
			if (limits1 == null && limits2 == null) break;
			double limits[] = limits1;
			if (limits == null) limits = limits2;
			if (limits1 != null && limits2 != null) {
				limits = limits1;
				if (Math.abs(limits2[0]) > Math.abs(limits1[0])) limits = limits2;
			}
			double max = Math.abs(limits[0]);
			int init = (int) limits[1];
			int end = (int) limits[2];

			for (int i=0;i<init;i++) {
				v[i] = 0;
			}
			for (int i=end;i<v.length;i++) {
				v[i] = 0;
			}

			double p[] = null;
			try {
				p = this.fitGreatestLine(v, sigma, init, end);

				if (Math.abs(p[2]) < max && Math.abs(p[2]) < TIMES_SIGMA * sigma) {
					// Update sigma
					double vcopy[] = v.clone();
					removeLine(vcopy, p);
					sigma = getSigma(vcopy);
					p = this.fitGreatestLine(v, sigma, init, end);
				}

				// FIXME Now Math.min(Math.abs(p[2]), max), before it was max only
				if (Math.min(Math.abs(p[2]), max) < TIMES_SIGMA * sigma) {
					lineUnderNoise = true;
				} else {
					double g0 = getSpectrum().getChannel(p[0]);
		 			double y = (p[2] / Math.exp(0.5 * FastMath.pow(((int)(g0+0.5) - g0) / (p[1] / (vres * cte)), 2.0)));
		 			y = Math.abs(y);
					if (y < TIMES_SIGMA * sigma || p[1]*4 < Math.abs(vres)) lineUnderNoise = true;
					if (p[2] == 0) lineUnderNoise = true;
					if (!lineUnderNoise || (max > TIMES_SIGMA * sigma)) {
						if (y > sigma) { // Check absorption lines
							double clonev1[] = v.clone();
							removeLine(v, p);
							double clonev2[] = v.clone();
							double s1 = this.getSigma(clonev1, 0.0), s2 = this.getSigma(clonev2, 0.0);
							if (s2 > s1) { // Sometimes the returned line is inconsistent and makes the fit worse
								v = clonev1;
								lineUnderNoise = true;
								for (int i=init; i<end; i++) {
									badfit[i] += v[i];
									v[i] = 0.0;
								}
							}
						} else {
							for (int i=init; i<end; i++) {
								badfit[i] += v[i];
								v[i] = 0.0;
							}
						}
						if (!lineUnderNoise) al.add(p);
						lineUnderNoise = false;
					}
				}
				if (p[2] == 0) lineUnderNoise = true;
			} catch (Exception exc) {
				lineUnderNoise = true;
			}

			for (int i=0;i<init;i++) {
				v[i] += clonv[i];
			}
			for (int i=end;i<v.length;i++) {
				v[i] += clonv[i];
			}

		} while (!lineUnderNoise && (al.size() < maxN || maxN < 0));

		for (int i=0; i<v.length; i++) {
			v[i] += badfit[i];
		}

		residualV = v;
		if (eliminateNoise)	{
			this.v = Functions.substract(this.v, residualV);
		}
		return al;
	}

	/**
	 * Fits the lines in a spectrum. The spectrum should be previously
	 * reduced. Sigma value is obtained automatically. The method
	 * {@linkplain #getLines(ArrayList)} can later be used to return
	 * a set of {@linkplain SpectrumLine} objects with the fitted lines.
	 * @param c0 Initial channel.
	 * @param c1 Final channel.
	 * @param yMin Minimum y value. In case some value in the spectrum is below
	 * this one, that channel will not be taken into account in the fit.
	 * @param yMax Maximum y value. In case some value in the spectrum is above
	 * this one, that channel will not be taken into account in the fit.
	 * @param eliminateLine True to subtract the line to the current
	 * spectrum.
	 * @return A list with the parameters of the Gaussian lines.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[] fitLineBetweenChannels(int c0, int c1, double yMin, double yMax, boolean eliminateLine) throws JPARSECException {
		double[] al = null;
		double v[] = this.v.clone(); //fix(this.v.clone());
		double sigma = getSigma(v);

		if (c0 < 0) c0 = 0;
		if (c0 > v.length-1) c0 = v.length-1;
		if (c1 < 0) c1 = 0;
		if (c1 > v.length-1) c1 = v.length-1;

		for (int i=0;i<c0;i++) {
			v[i] = 0;
		}
		for (int i=c1;i<v.length;i++) {
			v[i] = 0;
		}

		double clonev[] = v.clone();
		if (yMax != yMin) {
			// Disable yMin to fit absorption lines easily
			yMin = DataSet.getMinimumValue(v);
			yMin -= Math.abs(yMin);
			for (int i=c0;i<c1;i++) {
				if (v[i] > yMax || v[i] < yMin) v[i] = 0;
			}
		}

		al = this.fitGreatestLine(v, sigma, c0, c1);

		removeLine(v, al);

		for (int i=0;i<c0;i++) {
			v[i] += this.v[i];
		}
		for (int i=c1;i<v.length;i++) {
			v[i] += this.v[i];
		}

		if (yMax != yMin) {
			for (int i=c0;i<c1;i++) {
				if (clonev[i] > yMax || clonev[i] < yMin) v[i] += this.v[i];
			}
		}
		if (eliminateLine) removeLine(this.v, al);
		residualV = v;
		return al;
	}

	/**
	 * Fits the lines in a spectrum. The spectrum should be previously
	 * reduced. The method {@linkplain #getLines(ArrayList)} can later be used to return
	 * a set of {@linkplain SpectrumLine} objects with the fitted lines.
	 * @param sigma The sigma value of the spectrum. This affects the errors
	 * of the Gaussian parameters.
	 * @param c0 Initial channel.
	 * @param c1 Final channel.
	 * @param yMin Minimum y value. In case some value in the spectrum is below
	 * this one, that channel will not be taken into account in the fit.
	 * @param yMax Maximum y value. In case some value in the spectrum is above
	 * this one, that channel will not be taken into account in the fit.
	 * @param eliminateLine True to substract the line to the current
	 * spectrum.
	 * @return A list with the parameters of the Gaussian lines.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[] fitLineBetweenChannels(double sigma, int c0, int c1, double yMin, double yMax, boolean eliminateLine) throws JPARSECException {
		double[] al = null;
		double v[] = this.v.clone(); //fix(this.v.clone());

		if (c0 < 0) c0 = 0;
		if (c0 > v.length-1) c0 = v.length-1;
		if (c1 < 0) c1 = 0;
		if (c1 > v.length-1) c1 = v.length-1;

		for (int i=0;i<c0;i++) {
			v[i] = 0;
		}
		for (int i=c1;i<v.length;i++) {
			v[i] = 0;
		}

		double clonev[] = v.clone();
		if (yMax != yMin) {
			// Disable yMin to fit absorption lines easily
			yMin = DataSet.getMinimumValue(v);
			yMin -= Math.abs(yMin);
			for (int i=c0;i<c1;i++) {
				if (v[i] > yMax || v[i] < yMin) v[i] = 0;
			}
		}
		al = this.fitGreatestLine(v, sigma, c0, c1);

		removeLine(v, al);

		for (int i=0;i<c0;i++) {
			v[i] += this.v[i];
		}
		for (int i=c1;i<v.length;i++) {
			v[i] += this.v[i];
		}

		if (yMax != yMin) {
			for (int i=c0;i<c1;i++) {
				if (clonev[i] > yMax || clonev[i] < yMin) v[i] += this.v[i];
			}
		}
		if (eliminateLine) removeLine(this.v, al);
		residualV = v;
		return al;
	}

	// Fix bad channels
	private float[] fix(float[] v) {
		for (int i=0; i<v.length; i++) {
			if (Float.isNaN(v[i]) || v[i] == -1000) {
				if (i < v.length-1 && (Float.isNaN(v[i]+1) || v[i+1] == -1000)) {
					while((Float.isNaN(v[i]) || (i < v.length-1 && v[i+1] == -1000)) && i < v.length) {
						v[i] = 0;
						i++;
						if (i == v.length) break;
					}
					continue;
				}
				if (v[i] == -1000) {
					v[i] = 0;
				} else {
					v[i] = getMeanAround(v, i, 1);
					if (Float.isNaN(v[i])) v[i] = 0;
				}
			}
		}

		if (v.length > 30000) return v;

		for (int i=0; i<v.length; i++) {
			if (v[i] < minimumValueToConsiderBadChannel && i > 0) {
				int j = i;
				do {
					j++;
					if (j >= v.length-1) {
						j = v.length-1;
						break;
					}
				} while(v[j] < minimumValueToConsiderBadChannel);
				if (v[j] < minimumValueToConsiderBadChannel && j == v.length - 1) {
					for (j=i; j<v.length; j++) {
						v[j] = 0.0f;
					}
					break;
				} else {
					for (int k=i; k<j; k++) {
						v[k] = (float) (v[i-1]+(v[j]-v[i-1])*(k-i+1.0)/(double)(j-i+1.0));
					}
				}
			} else {
				if (v[i] < minimumValueToConsiderBadChannel) v[i] = 0;
			}
		}
		return v;
	}

	/**
	 * Corrects the 'level 0' of the spectrum in case there is
	 * a significant amount of continuum. This correction is done
	 * automatically.
	 */
	public void fixLevel0() {
		double m = getMeanAround(v, v.length/2, v.length/2+1);
//		double s = getSigma(v);

//		if (Math.abs(m) > TIMES_SIGMA * s) { // FIXME TIMES_SIGMA wasn't here at the beggining, I put it after and then decided to eliminate it again
			for (int i=0; i<v.length; i++) {
				v[i] -= m;
			}
//		}
	}

	/**
	 * Smoothes the residual spectrum (an invisible array stored internally in this instance)
	 * and returns the result of subtracting it to a given input array. This method should be
	 * applied to the residuals after the reduction is done. It is not used in this
	 * class, it is intended to be used for advanced reduction externally.
	 * @param n Number of channels to smooth the residual array.
	 * @param data The spectrum array where the smoothed residuals should be reduced.
	 * @return The flattened spectrum, input data - smoothed residuals. In case the data
	 * array is null the returned array is the smoothed residuals.
	 * @throws JPARSECException If an error occurs.
	 */
 	public double[] reduceResiduals(int n, double data[]) throws JPARSECException {
 		if (residualV == null) residualV = v.clone();

 		if (n == 0) return residualV.clone();

 		String badChannels = this.removeBadChannels();
       	int nAverage = n;
       	double out[] = new double[v.length];
		for (int i=0; i<v.length; i++) {
			if (nAverage < 0) {
				out[i] = 0;
			} else {
				int badChannel = badChannels.indexOf(","+i+"=");
				int min = Math.max(i-nAverage, 0);
				int max = Math.min(v.length-1, i+nAverage);
				double minV = getMean(v, min, max);
				if (badChannel >= 0) {
					out[i] = residualV[i] - minV;
				} else {
					out[i] = minV;
				}
			}
		}

		if (data != null) out = Functions.substract(data, out);
		return out;
	}

	private static boolean areSimilar(double v1, double v2) {
		boolean similar = false;
		if (Math.abs(v1-v2) < 1) similar = true;
		return similar;
	}

	/**
	 * Sums spectra and returns the mean of them. This method will
	 * average all scans with the same rest frequency. This frequency
	 * corresponds to the first scan found to satisfy the conditions
	 * or parameters set in this method. For a given particular
	 * frequency use instead {@linkplain #sumSpectra(Gildas30m[], String, String, double, double, String, double)}.
	 * @param f Set of 30m files.
	 * @param s The source name to search for.
	 * @param m The molecule to search for.
	 * @param searchOffset1 The RA offset to search for.
	 * @param searchOffset2 The DEC offset to search for.
	 * @param t The telescope to search for.
	 * @return The spectrum.
	 * @throws JPARSECException If an error occurs.
	 */
	public static Spectrum30m sumSpectra(Gildas30m[] f, String s, String m, double searchOffset1, double searchOffset2,
			String t) throws JPARSECException {
		return sumSpectra(f, s, m, searchOffset1, searchOffset2, t, -1);
	}

	/**
	 * Sums spectra and returns the mean of them.
	 * @param f Set of 30m files.
	 * @param s The source name to search for.
	 * @param m The molecule to search for.
	 * @param searchOffset1 The RA offset to search for.
	 * @param searchOffset2 The DEC offset to search for.
	 * @param t The telescope to search for.
	 * @param restFreq Rest frequency of the scans to average in MHz.
	 * Set to 0 or lower to compute it automatically, averaging all
	 * spectra for the same frequency as that of the first scan
	 * found that satisfies the other conditions.
	 * @return The spectrum.
	 * @throws JPARSECException If an error occurs.
	 */
	public static Spectrum30m sumSpectra(Gildas30m[] f, String s, String m, double searchOffset1, double searchOffset2,
			String t, double restFreq) throws JPARSECException {
		boolean spectrumFound = false;
		double data[] = null;
		int ndata = 0;
		double vres = 0.0, refchan = 0.0, vref = 0.0, rfreq = 0.0, ifreq = 0.0;
		int num = 0, scan = 0;
		Spectrum30m sp = null;
		double intTimeNormalize = -1, intTimeTotal = 0;
		for (int i=0; i<f.length; i++) {
			try {
				Gildas30m g30m = f[i];
				if (g30m != null) {
		        	final int list[] = g30m.getListOfSpectrums(true);

		        	for (int index=0; index<list.length; index++) {
		            	Spectrum30m s30m = g30m.getSpectrum(list[index]);
		            	HeaderElement header[] = s30m.getHeader().getHeaderParameters();

		            	double off1 = header[SpectrumHeader30m.HEADER.OFFSET1.ordinal()].getAsDouble() * Constant.RAD_TO_ARCSEC;
		            	double off2 = header[SpectrumHeader30m.HEADER.OFFSET2.ordinal()].getAsDouble() * Constant.RAD_TO_ARCSEC;
		            	String line = header[SpectrumHeader30m.HEADER.LINE.ordinal()].getAsString().trim().toUpperCase();
		            	String source = header[SpectrumHeader30m.HEADER.SOURCE.ordinal()].getAsString().trim().toUpperCase();
		            	String teles = header[SpectrumHeader30m.HEADER.TELES.ordinal()].getAsString().trim().toUpperCase();

		            	if (s.equals(source) && areSimilar(off1, searchOffset1) && areSimilar(off2, searchOffset2)
		            			&& t.equals(teles) && m.equals(line) &&
		            			(areSimilar(s30m.getReferenceFrequency(), restFreq) || restFreq <= 0)) {
		            		ndata++;
		            		if (ndata == 1 && restFreq <= 0) restFreq = s30m.getReferenceFrequency();
		            		if (spectrumFound) {
		            			double[] moreData = DataSet.toDoubleArray(s30m.getSpectrumData());
		            			double intTime = ((HeaderElement) s30m.get(Gildas30m.INTEG)).getAsDouble();
		            			if (intTime != 0 && intTimeNormalize != 0 && intTime != intTimeNormalize)
		            				moreData = Functions.scalarProduct(moreData, intTimeNormalize / intTime);
		            			intTimeTotal += intTime;

		            			data = Functions.sumVectors(data, moreData);
		            		} else {
		            			intTimeNormalize = ((HeaderElement) s30m.get(Gildas30m.INTEG)).getAsDouble();
		            			intTimeTotal += intTimeNormalize;

		            			data = DataSet.toDoubleArray(s30m.getSpectrumData());
		            			sp = s30m;
			            		spectrumFound = true;
			            		vres = ((HeaderElement) s30m.get(Gildas30m.VEL_RESOL)).getAsDouble();
			            		refchan = ((HeaderElement) s30m.get(Gildas30m.REF_CHAN)).getAsDouble();
			            		vref = ((HeaderElement) s30m.get(Gildas30m.REF_VEL)).getAsDouble();
			                   	rfreq = ((HeaderElement) s30m.get(Gildas30m.REF_FREQ)).getAsDouble();
			                   	ifreq = ((HeaderElement) s30m.get(Gildas30m.IMAGE)).getAsDouble();
			                   	num = Integer.parseInt("0"+header[SpectrumHeader30m.HEADER.NUM.ordinal()].getAsString());
			                   	scan = Integer.parseInt("0"+header[SpectrumHeader30m.HEADER.SCAN.ordinal()].getAsString());
		            		}
		        		}
		        	}
				}
			} catch (Exception e) {
				e.printStackTrace();
				Logger.log(LEVEL.ERROR, "Found an error when adding a spectrum. Message was "+e.getLocalizedMessage());
			}
		}

		if (ndata == 0) return null;

		if (ndata > 1) {
			num = scan = 0;
			for (int i=0; i<data.length; i++) {
				data [i] /= (double) ndata;
			}
		} else {
			ProcessSpectrum ps = new ProcessSpectrum(sp);
			data = ps.getProcessedSpectrum();
		}

		TreeMap<String,HeaderElement> map = new TreeMap<String,HeaderElement>();
        map.put(new String(Gildas30m.INTEG), new HeaderElement((float) intTimeTotal, Gildas30m.INTEG_DESC));
        map.put(new String(Gildas30m.VEL_RESOL), new HeaderElement(vres, Gildas30m.VEL_RESOL_DESC));
        map.put(new String(Gildas30m.REF_CHAN), new HeaderElement(refchan, Gildas30m.REF_CHAN_DESC));
        map.put(new String(Gildas30m.REF_VEL), new HeaderElement(vref, Gildas30m.REF_VEL_DESC));
        map.put(new String(Gildas30m.REF_FREQ), new HeaderElement(rfreq, Gildas30m.REF_FREQ_DESC));
        map.put(new String(Gildas30m.IMAGE), new HeaderElement(ifreq, Gildas30m.IMAGE_DESC));
        map.put(new String(Gildas30m.SOURCE), new HeaderElement(s, Gildas30m.SOURCE_DESC));
        map.put(new String(Gildas30m.TELES), new HeaderElement(t, Gildas30m.TELES_DESC));
        map.put(new String(Gildas30m.LINE), new HeaderElement(m, Gildas30m.LINE_DESC));

        try {
        map.put(new String(Gildas30m.LAMBDA), new HeaderElement(sp.get(Gildas30m.LAMBDA).getAsDouble(), Gildas30m.LAMBDA_DESC));
        map.put(new String(Gildas30m.BETA), new HeaderElement(sp.get(Gildas30m.BETA).getAsDouble(), Gildas30m.BETA_DESC));
        map.put(new String(Gildas30m.LAMBDA_OFF), new HeaderElement(sp.get(Gildas30m.LAMBDA_OFF).getAsFloat(), Gildas30m.LAMBDA_OFF_DESC));
        map.put(new String(Gildas30m.BETA_OFF), new HeaderElement(sp.get(Gildas30m.BETA_OFF).getAsFloat(), Gildas30m.BETA_OFF_DESC));
        map.put(new String(Gildas30m.EPOCH), new HeaderElement(sp.get(Gildas30m.EPOCH).getAsFloat(), Gildas30m.EPOCH_DESC));
        map.put(new String(Gildas30m.PROJECTION), new HeaderElement(sp.get(Gildas30m.PROJECTION).getAsInt(), Gildas30m.PROJECTION_DESC));
        map.put(new String(Gildas30m.DOPPLER), new HeaderElement(sp.get(Gildas30m.DOPPLER).getAsDouble(), Gildas30m.DOPPLER_DESC));
        } catch (Exception exc) {}
        HeaderElement pheader[] = sp.getHeader().getHeaderParameters();
        map.put(new String(Gildas30m.TYPEC), pheader[10]);
        map.put(new String(Gildas30m.KIND), pheader[11]);

        HeaderElement aobj[] = new HeaderElement[15];
        aobj[0] = new HeaderElement(num, Gildas30m.NUM_DESC);
        aobj[1] = new HeaderElement(0, Gildas30m.BLOCK_DESC);
        aobj[2] = new HeaderElement("0", Gildas30m.VERSION_DESC);
        aobj[3] = new HeaderElement(s, Gildas30m.SOURCE_DESC);
        aobj[4] = new HeaderElement(m, Gildas30m.LINE_DESC);
        aobj[5] = new HeaderElement(t, Gildas30m.TELES_DESC);
        aobj[6] = new HeaderElement(0, Gildas30m.LDOBS_DESC);
        aobj[7] = new HeaderElement(0, Gildas30m.LDRED_DESC);
        aobj[8] = new HeaderElement(searchOffset1 * Constant.ARCSEC_TO_RAD, Gildas30m.OFF1_DESC);
        aobj[9] = new HeaderElement(searchOffset2 * Constant.ARCSEC_TO_RAD, Gildas30m.OFF2_DESC);
        aobj[10] = pheader[10];
        aobj[11] = pheader[11];
        aobj[12] = new HeaderElement(0, Gildas30m.QUALITY_DESC);
        aobj[13] = new HeaderElement(scan, Gildas30m.SCAN_DESC);
        aobj[14] = new HeaderElement(0, Gildas30m.POSA_DESC);
        SpectrumHeader30m header = new SpectrumHeader30m(aobj);
		Spectrum30m s30m = new Spectrum30m(map, header, DataSet.toFloatArray(data));
		return s30m;
	}

	/**
	 * Clears all transitions read so that they are forced to be read again for a new set
	 * of parameters in the line search process.
	 */
	public static void clearTransitionsForLineIdentification() {
		DataBase.addData("processSpectrum", null, false);
	}
	/**
	 * Identify a line given some data. Transitions are stored in memory for the parameters
	 * given below, so in case they change the method {@link ProcessSpectrum#clearTransitionsForLineIdentification()}
	 * should be called first. The field {@linkplain #IMPOSSIBLE_MOLECULES} can optionally be used to insert
	 * between commas the names of all molecules you would like to skip when identifying lines.
	 * @param freq Frequency (approximate) of the line in MHz.
	 * @param width Width of the line in MHz.
	 * @param maxT Maximum temperature of the upper transition. Set to 0 to avoid
	 * this condition.
	 * @param maxrint Minimum value of the rint parameter of the transition, which
	 * is associated with the intensity of the line. Set to 0 to avoid this condition, required
	 * for instance to obtain recombination lines in the output.
	 * @param jpl True for JPL catalog, false for COLOGNE.
	 * @param splatalogue True to call Splatalogue using web service and to return a simple list of species found.
	 * Setting this as true will make previous jpl flag useless.
	 * @param onlyAtmospheric Set to true to return only possible atmospheric lines. This is only allowed for splatalogue.
	 * @param onlyPossibleButStrangeLines Set to true to return only possible ISM lines, but less probable. In case
	 * onlyAtmospheric is true this flag will be ignored.
	 * @return The set of transitions at freqs freq +/- width/2.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String[] identifyLine(double freq, double width, double maxT, double maxrint, boolean jpl, boolean splatalogue, boolean onlyAtmospheric, boolean onlyPossibleButStrangeLines) throws JPARSECException {
		if (splatalogue) {
			// [Molecule: frequency (MHz) and error, Log(intensity), lower state (LS) degeneracy, LS energy (K), upper state (US) energy (K), quantum numbers (QN) format, LS QN, US QN]
			double from = freq - width*0.5, to = freq + width*0.5;
			String ert = "";
			if (maxT > 0.0) ert = ""+maxT;
			String intens = "";
			if (maxrint != 0.0) intens = ""+maxrint;
			String noAtm = "true", noPotential = "true", noProbable = "true", noKnown = "false";
			String query = "http://www.cv.nrao.edu/php/splat/c.php?from="+from+"&to="+to+"&frequency_units=MHz&data_version=v2.0&displayLovas=true&displaySLAIM=true&displayJPL=true&displayCDMS=true&displayToyaMA=true&displayOSU=true&displayRecomb=true&displayLisa=true&displayRFI=true&ls1=true&ls5=true&el1=true&energy_range_to="+ert+"&energy_range_type=eu_k&lill_cdms_jpl="+intens+"&no_atmospheric="+noAtm+"&no_potential="+noPotential+"&no_probable="+noProbable; //+"&known="+noKnown;
			if (onlyAtmospheric) {
				noAtm = "false";
				noPotential = "true";
				noProbable = "true";
				noKnown = "true";
				query = "http://www.cv.nrao.edu/php/splat/c.php?from="+from+"&to="+to+"&frequency_units=MHz&data_version=v2.0&displayLovas=true&displaySLAIM=true&displayJPL=true&displayCDMS=true&displayToyaMA=true&displayOSU=true&displayRecomb=true&displayLisa=true&displayRFI=true&ls1=true&ls5=true&el1=true&energy_range_to="+ert+"&energy_range_type=eu_k&lill_cdms_jpl="+intens+"&no_potential="+noPotential+"&no_probable="+noProbable+"&known="+noKnown; //+"&no_atmosferic="+noAtm;
			} else {
				if (onlyPossibleButStrangeLines) {
					noAtm = "true";
					noKnown = "true";
					noPotential = "false";
					noProbable = "false";
					query = "http://www.cv.nrao.edu/php/splat/c.php?from="+from+"&to="+to+"&frequency_units=MHz&data_version=v2.0&displayLovas=true&displaySLAIM=true&displayJPL=true&displayCDMS=true&displayToyaMA=true&displayOSU=true&displayRecomb=true&displayLisa=true&displayRFI=true&ls1=true&ls5=true&el1=true&energy_range_to="+ert+"&energy_range_type=eu_k&lill_cdms_jpl="+intens+"&no_atmospheric="+noAtm+"&known="+noKnown; //+"&no_potential="+noPotential+"&no_probable="+noProbable;
				}
			}
			String out[] = DataSet.toStringArray(GeneralQuery.query(query), FileIO.getLineSeparator());
			boolean inside = false;
			String out2[] = new String[0];
			for (int i=0; i<out.length; i++) {
				if (out[i].indexOf("class=\"results\"") > 0) {
					inside = true;
					continue;
				}
				if (inside && out[i].indexOf("</table") > 0) break;
				if (inside && out[i].indexOf("<tr") > 0) {
					// counter, species, chemical name, freq MHz (Err), quantum numbers, Intensity JPL/CDMS, Intensity LOVAS/AST, E_l (cm-1), Catalog
					String data[] = new String[10];
					boolean err = false;
					for (int j = 1; j<= 10; j++) {
						if (i+j >= out.length || out[i+j].indexOf("</table>") >= 0) {
							err = true;
							break;
						}
						data[j-1] = out[i+j].trim();
						data[j-1] = DataSet.replaceAll(data[j-1], "<i>", "", true);
						data[j-1] = DataSet.replaceAll(data[j-1], "</i>", "", true);
						data[j-1] = DataSet.replaceAll(data[j-1], "<b>", "", true);
						data[j-1] = DataSet.replaceAll(data[j-1], "</b>", "", true);
						data[j-1] = DataSet.replaceAll(data[j-1], "<td>", "", true);
						data[j-1] = DataSet.replaceAll(data[j-1], "</td>", "", true);
						data[j-1] = DataSet.replaceAll(data[j-1], "&nbsp;", "", true);
						data[j-1] = DataSet.replaceAll(data[j-1], "<sub>", "_{", true);
						data[j-1] = DataSet.replaceAll(data[j-1], "<aub>", "_{", true); // Bug in splatalogue in CH3CHO line
						data[j-1] = DataSet.replaceAll(data[j-1], "</sub>", "}", true);
						data[j-1] = DataSet.replaceAll(data[j-1], "<sup>", "^{", true);
						data[j-1] = DataSet.replaceAll(data[j-1], "<font color=\"red\">", "", true);
						data[j-1] = DataSet.replaceAll(data[j-1], "<font face=monospace>", "", true);
						data[j-1] = DataSet.replaceAll(data[j-1], "</font>", "", true);
						data[j-1] = DataSet.replaceAll(data[j-1], "</sup>", "}", true);
						int a = data[j-1].indexOf(">");
//						System.out.println(j+"/"+out[i+j]+"/"+data[j-1]+"/"+a);
						if (a > 0) {
							data[j-1] = data[j-1].substring(a+1);
							a = data[j-1].indexOf("<");
							if (a >= 0) data[j-1] = data[j-1].substring(0, a);
						}
					}
					if (err) continue;
					intens = data[6];
					if (intens.equals("")) intens = data[7];
					if (!intens.equals("")) intens += " ("+data[9]+")";
					String sep = "  |  ";
					String engu = "";
					String freqs = data[3];
					if (freqs.equals("")) freqs = data[4];
					String name = data[1]+" "+data[5];
					name = DataSet.replaceAll(name, "( ", "(", true);
					name = DataSet.replaceAll(name, "  ", " ", true);
					name = DataSet.replaceAll(name, "= ", "=", true);
					try {
						name += "   freq="+freqs.substring(0, freqs.indexOf(" "));
						double enguv = DataSet.parseDouble(freqs.substring(0, freqs.indexOf(" "))) * 1.0E6 * Constant.HZ_TO_K + Double.parseDouble(data[8]);
						engu = ""+(float)enguv;
					} catch (Exception exc) {}

					//int g = name.indexOf("-"), gg = name.lastIndexOf("-");
					//if (g == gg) name = DataSet.replaceAll(name, "-", "$\\rightarrow$", true);
					out2 = DataSet.addStringArray(out2, new String[] {
							data[1] + ":  " + freqs + sep + intens + sep + "-" + sep + data[8] + sep + engu + sep + data[5] + sep + name
					});
					i += 10;
				}
			}
			if (out2.length == 0) return new String[] {};
			return DataSet.getSubArray(out2, 1, out2.length-1);
		}

		ArrayList<String> mol;
		if (jpl) {
			mol = CatalogRead.readJPLcatalog();
		} else {
			mol = CatalogRead.readCOLOGNEcatalog();
		}

		boolean save = false;
		String tran[][] = (String[][]) DataBase.getData("processSpectrum", false);
		if (tran == null) {
			tran = new String[mol.size()][];
			save = true;
		}
		String possibleTrans[] = new String[] {};
		String t[];
		String imposs[] = null;
		if (!IMPOSSIBLE_MOLECULES.equals("")) imposs = DataSet.toStringArray(IMPOSSIBLE_MOLECULES, ",", true);
		for (int i=0; i<mol.size(); i++) {
			if (imposs != null) {
				int impID = DataSet.getIndexStartingWith(imposs, mol.get(i));
				if (impID >= 0) continue;
			}
			try {
				if (save) {
					t = CatalogRead.getTransitions(0, mol.get(i), jpl,
							width, maxT, maxrint);
					tran[i] = t;
				} else {
					t = tran[i];
				}
				if (t != null && t.length > 0) {
					for (int k=0; k<t.length; k++) {
						double frec = Double.parseDouble(t[k].substring(0, 13).trim());
						if (Math.abs(frec-freq) < width*0.5) {
							String data = t[k]; //t[k].substring(0, 13).trim() + " / "+t[k].substring(55).trim();
							String f = data.substring(0, 13).trim();
							String fe = data.substring(13, 21).trim();
							String gu = data.substring(41, 44).trim();
							String rint = data.substring(21, 29).trim();
							double engl = Constant.CM_TO_K * Double.parseDouble(data.substring(31, 41).trim());
							double engu = Double.parseDouble(f) * 1.0E6 * Constant.HZ_TO_K + engl;
							int qf = Integer.parseInt(data.substring(51, 55).trim());
							String ql = data.substring(55, 67).trim();
							String qu = data.substring(67).trim();
							data = ""+f+" +/- "+fe+" | "+rint+" | "+gu+" | "+(float) engl+" | "+(float)engu+" | "+qf+" | "+ql+" | "+qu;
							if (jpl) {
								possibleTrans = DataSet.addStringArray(possibleTrans, new String[] {mol.get(i).substring(0, 20).trim()+": "+data});
							} else {
								possibleTrans = DataSet.addStringArray(possibleTrans, new String[] {mol.get(i).substring(0, 32).trim()+": "+data});
							}
						}
					}
				}
			} catch (Exception e) {
				Logger.log(LEVEL.ERROR, "Found an error when identifying lines. Message was "+e.getLocalizedMessage());
			}
		}

		DataBase.addData("processSpectrum", tran, false);

		return possibleTrans;
	}

	private double fitx[], fitdx[];
	private void fitAGaussian(double x[], double y[], double w[]) throws JPARSECException {
		Regression reg = new Regression(x, y, w);
		reg.setNmax(maximumNumberOfIterationsForNelderAndMeadSimplexInRegressionClass);
		try {
			reg.gaussian();
			fitx = reg.getBestEstimates();
			fitdx = reg.getBestEstimatesErrors();
		} catch (Exception exc) {
			fitx = new double[] {0, 0, 0};
			fitdx = new double[] {0, 0, 0};
		}
	}

	/**
	 * Performs a quick and simple gaussian fit to a set of data.
	 * @param x The x values.
	 * @param y The y values for the gaussian profile.
	 * @param sigma The error or 1-sigma level of the data.
	 * @return x position of the gaussian center, width of the gaussian, peak, and area,
	 * referred to the units for the x axis. Another 4 values for their respective errors.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double[] fitGaussian(double x[], double y[], double sigma) throws JPARSECException {
        double w[] = DataSet.getSetOfValues(sigma, sigma, x.length, false); // Use sigma as the error in each point
		Regression reg = new Regression(x, y, w);
		reg.setNmax(maximumNumberOfIterationsForNelderAndMeadSimplexInRegressionClass);
		reg.gaussian();

		double[] fitx = reg.getBestEstimates();
		double[] fitdx = reg.getBestEstimatesErrors();
        double v[] = fitx; // mean, sd, y_scale
        double dv[] = fitdx;
        double sqrttwopi = Math.sqrt(Constant.TWO_PI);
        MeasureElement me1 = new MeasureElement(v[1] * sqrttwopi, dv[1] * sqrttwopi, MeasureElement.UNIT_Y_K);
        MeasureElement me2 = new MeasureElement(v[2], dv[2], MeasureElement.UNIT_Y_K);
        me2.divide(me1);
		double g = cte;
		double v0 = 1; //Math.abs(vres);
        double out[] = new double[] {
        		v[0], v[1] * g * v0, me2.getValue(), v[2] * v0, // x, w, T, A
        		Math.abs(dv[0] * v0), Math.abs(dv[1] * g * v0), Math.abs(me2.error), Math.abs(dv[2] * v0), // errors
        };
        return out;
	}

	/**
	 * Performs a quick and simple gaussian fit to a set of data.
	 * @param x The x values.
	 * @param y The y values for the gaussian profile.
	 * @param sigma The error or 1-sigma level of the data.
	 * @return x position of the gaussian center, width of the gaussian, peak, and area,
	 * referred to the units for the x axis. Another 4 values for their respective errors.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double[] fitGaussian(double x[], double y[], double sigma[]) throws JPARSECException {
		Regression reg = new Regression(x, y, sigma);
		reg.setNmax(maximumNumberOfIterationsForNelderAndMeadSimplexInRegressionClass);
		reg.gaussian();

		double[] fitx = reg.getBestEstimates();
		double[] fitdx = reg.getBestEstimatesErrors();
        double v[] = fitx; // mean, sd, y_scale
        double dv[] = fitdx;
        double sqrttwopi = Math.sqrt(Constant.TWO_PI);
        MeasureElement me1 = new MeasureElement(v[1] * sqrttwopi, dv[1] * sqrttwopi, MeasureElement.UNIT_Y_K);
        MeasureElement me2 = new MeasureElement(v[2], dv[2], MeasureElement.UNIT_Y_K);
        me2.divide(me1);
		double g = cte;
		double v0 = 1; //Math.abs(vres);
        double out[] = new double[] {
        		v[0], v[1] * g * v0, me2.getValue(), v[2] * v0, // x, w, T, A
        		Math.abs(dv[0] * v0), Math.abs(dv[1] * g * v0), Math.abs(me2.error), Math.abs(dv[2] * v0), // errors
        };
        return out;
	}

	/**
	 * A very simple reduction method that passes a baseline only.
	 * @param n Number of channels to smooth around a given one (+/- n) to reduce a given channel to 0.
	 * @param lineChannelStart Channel number (index) where the line of interest starts.
	 * Set to -1 no use no line window.
	 * @param lineChannelEnd Channel number where the line ends. Set to -1 no use no line
	 * window.
	 */
	public void reduceSpectrum(int n, int lineChannelStart, int lineChannelEnd) {
		double mean[] = new double[v.length];
		for (int i=n; i<v.length-1-n; i++) {
			mean[i] = getMean(v, i-n, i+n);
		}
		for (int i=0;i<=n;i++) {
			mean[i] = mean[n];
			mean[v.length-1-n+i] = mean[v.length-n-2];
		}
		for (int i=0;i<v.length;i++) {
			if (i < lineChannelStart || i > lineChannelEnd) {
				v[i] = v[i] - mean[i];
			} else {
				double slope = (v[lineChannelEnd] - v[lineChannelStart]) / ((double) lineChannelEnd - (double) lineChannelStart);
				double val = v[lineChannelStart] + slope * (i - lineChannelStart);
				v[i] = v[i] - val;
			}
		}
	}

	/**
	 * A very simple reduction method that passes a linear baseline only. Adequate for large OTFs.
	 * @param n Number of channels to smooth around a given one (+/- n) to reduce a given channel to 0.
	 * Only the first and last averaged values are used to fit a line and substract the baseline.
	 */
	public void reduceSpectrumLinearBaseline(int n) {
		double mean0 = getMean(v, 0, n+n);
		double meanf = getMean(v, v.length-1-2*n, v.length-1);
		double slope = (meanf - mean0) / ((double) (v.length-1-n) - (double) n);
		double val = mean0 - slope * n;
		for (int i=0;i<v.length;i++) {
			v[i] = v[i] - (slope * i + val);
		}
	}

	private static double getRMS(Spectrum30m s, SpectrumLine[] sl) throws JPARSECException {
    	ProcessSpectrum pss = new ProcessSpectrum(s);
    	double datas[] = pss.getProcessedSpectrum();
    	for (int j=0; j<sl.length; j++) {
			if (sl[j].enabled) {
				double l[] = new double[] {
						sl[j].vel, sl[j].width, sl[j].peakT, sl[j].area,
						sl[j].velError, sl[j].widthError, sl[j].peakTError, sl[j].areaError
				};
				pss.removeLine(datas, l);
			}
    	}
		pss.v = datas;
		return ProcessSpectrum.getSigma(datas);
	}

	/**
	 * Reduces the spectrum in a fully automatic way.
	 * @param spectrum The spectrum.
	 * @param maxN Maximum number of lines to return, -1 to return all of them.
	 * @return The set of lines found, or null if none is found.
	 * @throws JPARSECException If an error occurs.
	 */
	public static SpectrumLine[] reduceSpectrum(Spectrum spectrum, int maxN) throws JPARSECException {
		return reduceSpectrum(new Spectrum30m(spectrum), maxN);
	}

	/**
	 * Reduces the spectrum in a fully automatic way.
	 * @param spectrum The spectrum.
	 * @param maxN Maximum number of lines to return, -1 to return all of them.
	 * @return The set of lines found, or null if none is found.
	 * @throws JPARSECException If an error occurs.
	 */
	public static SpectrumLine[] reduceSpectrum(Spectrum30m spectrum, int maxN) throws JPARSECException {
		return (SpectrumLine[]) reduce(spectrum, maxN)[0];
	}

	/**
	 * Reduces the spectrum in a fully automatic way.
	 * @param spectrum The spectrum.
	 * @return The set of lines found, or null if none is found.
	 * @throws JPARSECException If an error occurs.
	 */
	public static SpectrumLine[] reduceSpectrum(Spectrum spectrum) throws JPARSECException {
		return reduceSpectrum(new Spectrum30m(spectrum), -1);
	}

	/**
	 * Reduces the spectrum in a fully automatic way.
	 * @param spectrum The spectrum.
	 * @return The set of lines found, or null if none is found.
	 * @throws JPARSECException If an error occurs.
	 */
	public static SpectrumLine[] reduceSpectrum(Spectrum30m spectrum) throws JPARSECException {
		return (SpectrumLine[]) reduce(spectrum, -1)[0];
	}

	/**
	 * Reduces the spectrum in a fully automatic way and return the process instance.
	 * @param spectrum The spectrum.
	 * @param maxN Maximum number of lines to return, -1 to return all of them.
	 * @return The instance of the processed spectrum. The process will be complete
	 * only in case at least one line is found.
	 * @throws JPARSECException If an error occurs.
	 */
	public static ProcessSpectrum reduceSpectrumAndReturnProcessSpectrum(Spectrum30m spectrum, int maxN) throws JPARSECException {
		return (ProcessSpectrum) reduce(spectrum, maxN)[1];
	}

	/**
	 * Reduces the spectrum in a fully automatic way and return the process instance.
	 * @param spectrum The spectrum.
	 * @return The instance of the processed spectrum. The process will be complete
	 * only in case at least one line is found.
	 * @throws JPARSECException If an error occurs.
	 */
	public static ProcessSpectrum reduceSpectrumAndReturnProcessSpectrum(Spectrum30m spectrum) throws JPARSECException {
		return (ProcessSpectrum) reduce(spectrum, -1)[1];
	}

	private static int getNITER(SpectrumLine sl[]) throws JPARSECException {
		int itm = 10;
	    if (sl.length > 40) itm = 3;
	    if (sl.length > 80) itm = 2; // Never 1, should be 2 at least
	    if (sl.length > 150) {
	    	itm = 0;
	    	JPARSECException.addWarning("The automatic iteration process to accurately fit lines was disabled for this spectrum since it contains "+sl.length+" > 150 lines.");
	    }
	    return itm;
	}
	private static Object[] reduce(Spectrum30m sp, int maxN) throws JPARSECException {
		Spectrum30m spectrum = sp.clone();
		ProcessSpectrum ps = new ProcessSpectrum(spectrum);
		ps.fixLevel0();
		SpectrumLine[] sl = null;
		ArrayList<double[]> lines = ps.fitLines(false, maxN);
		int sindex = 0, smoothValue = 10;
		if (lines.size() > 0) {
			sl = new SpectrumLine[lines.size()];
			for (int i=0; i<lines.size(); i++) {
				double line[] = lines.get(i);
				sl[i] = new SpectrumLine(line);
				double chanMin = line[8], chanMax = line[9];
				if (chanMin > chanMax) {
					double tmp = chanMin;
					chanMin = chanMax;
					chanMax = tmp;
				}
				sl[i].minChannel = (int) chanMin;
				sl[i].maxChannel = (int) chanMax;
				sl[i].yMin = -1;
				sl[i].yMax = -1;
				sl[i].fitted = true;
				sl[i].lineIndex = i + sl.length;
				sl[i].spectrumIndex = sindex;
				sl[i].label = "Fit to line "+(sl[i].lineIndex+1);
				sl[i].labelForChartID = sl[i].label;
			}
		}

		ps = new ProcessSpectrum(spectrum);
		ps.fixLevel0();
		if (sl != null) {
			double data[] = ps.getProcessedSpectrum().clone();

			// Fit again the lines and try to reduce rms
			if (sl.length > 1) {
		    	double vx[] = new double[sl.length];
		    	double vy[] = new double[sl.length];
			    for (int i=0; i<sl.length; i++) {
			    	vx[i] = Math.abs(sl[i].peakT);
			    	vy[i] = i;
			    }
			    ArrayList<double[]> list = DataSet.sortInDescent(vx, vy, false);
			    vy = list.get(1);
			    SpectrumLine sl0[] = sl.clone();
			    double rms0 = getRMS(spectrum, sl);
			    int itm = getNITER(sl);
			    for (int iter=0; iter<itm; iter ++) {
			    	boolean repeat = false;
				    for (int i=0; i< sl.length; i++) {
				    	int lindex = (int) vy[i];
				    	if (sl[lindex].enabled) {
					    	ProcessSpectrum pss = new ProcessSpectrum(spectrum);
							pss.fixLevel0();
					    	double datas[] = pss.getProcessedSpectrum();
					    	for (int j=0; j<sl.length; j++) {
					    		if (j != lindex) {
									if (sl[j].enabled) {
										double l[] = new double[] {
												sl[j].vel, sl[j].width, sl[j].peakT, sl[j].area,
												sl[j].velError, sl[j].widthError, sl[j].peakTError, sl[j].areaError
										};
										pss.removeLine(datas, l);
									}
					    		}
					    	}
							pss.v = datas;

							double l[] = pss.fitLineBetweenChannels(sl[lindex].minChannel, sl[lindex].maxChannel, sl[lindex].yMin, sl[lindex].yMax, false);

			    			sl[lindex].vel = l[0];
			    			sl[lindex].velError = l[4];
			    			sl[lindex].width = l[1];
			    			sl[lindex].widthError = l[5];
			    			sl[lindex].peakT = l[2];
			    			sl[lindex].peakTError = l[6];
			    			sl[lindex].area = l[3];
			    			sl[lindex].areaError = l[7];
							sl[lindex].freq = l[10];

							double max = Math.abs(l[2]);
							//ChartSeriesElement series = ps.getGaussianFit(l, XUNIT.VELOCITY_KMS);
							//max = DataSet.getMaximumValue(DataSet.toDoubleValues(series.yValues));
							//if (l[2] < 0.0 ) max = Math.abs(DataSet.getMinimumValue(DataSet.toDoubleValues(series.yValues)));
							if (max < ProcessSpectrum.TIMES_SIGMA * rms0 && sl[lindex].enabled) {
								sl[lindex].deleted = true;
								sl[lindex].enabled = false;
								i = 0;
								iter = 0;
								repeat = true;
								break;
							}
				    	}
				    }
				    double rms = getRMS(spectrum, sl);
				    if (rms < rms0 || repeat) {
				    	rms0 = rms;
				    	sl0 = sl.clone();
				    } else {
				    	sl = sl0;
				    	break;
				    }
			    }
			}

			for (int i=0; i<sl.length; i++) {
				if (sl[i].enabled) {
					double l[] = new double[] {
							sl[i].vel, sl[i].width, sl[i].peakT, sl[i].area,
							sl[i].velError, sl[i].widthError, sl[i].peakTError, sl[i].areaError
					};
					ps.removeLine(ps.v, l);
				}
			}
			double out[] = ps.reduceResiduals(smoothValue, data);
			spectrum.setSpectrumData(DataSet.toFloatArray(out));
			ps = new ProcessSpectrum(spectrum);
			ps.fixLevel0();

			double vcopy[] = ps.v.clone();
			for (int i=0; i<sl.length; i++) {
				ps.fitLineBetweenChannels(sl[i].minChannel, sl[i].maxChannel,
						-1, -1, true);
			}
			double sigma = ProcessSpectrum.getSigma(ps.v);
			ps.v = vcopy;
			for (int i=0; i<sl.length; i++) {
				double line[] = ps.fitLineBetweenChannels(sigma, sl[i].minChannel, sl[i].maxChannel,
						-1, -1, true);
				sl[i].fitted = true;
				sl[i].vel = line[0];
				sl[i].velError = line[4];
				sl[i].width = line[1];
				sl[i].widthError = line[5];
				sl[i].peakT = line[2];
				sl[i].peakTError = line[6];
				sl[i].area = line[3];
				sl[i].areaError = line[7];
				sl[i].lineIndex = i;
				sl[i].spectrumIndex = sindex;
				sl[i].freq = line[10];
				String label = "Fit to line "+(i+1); //identifyLine(spectrum, sl[i]);
				if (label != null) sl[i].label = label;
				sl[i].labelForChartID = sl[i].label;
			}

			// Fit again the lines and try to reduce rms
			if (sl.length > 1) {
		    	double vx[] = new double[sl.length];
		    	double vy[] = new double[sl.length];
			    for (int i=0; i<sl.length; i++) {
			    	vx[i] = Math.abs(sl[i].peakT);
			    	vy[i] = i;
			    }
			    ArrayList<double[]> list = DataSet.sortInDescent(vx, vy, false);
			    vy = list.get(1);
			    SpectrumLine sl0[] = sl.clone();
			    double rms0 = getRMS(spectrum, sl);
			    int itm = getNITER(sl);
			    for (int iter=0; iter<itm; iter ++) {
			    	boolean repeat = false;
				    for (int i=0; i< sl.length; i++) {
				    	int lindex = (int) vy[i];
				    	if (sl[lindex].enabled) {
					    	ProcessSpectrum pss = new ProcessSpectrum(spectrum);
							pss.fixLevel0();
					    	double datas[] = pss.getProcessedSpectrum();
					    	for (int j=0; j<sl.length; j++) {
					    		if (j != lindex) {
									if (sl[j].enabled) {
										double l[] = new double[] {
												sl[j].vel, sl[j].width, sl[j].peakT, sl[j].area,
												sl[j].velError, sl[j].widthError, sl[j].peakTError, sl[j].areaError
										};
										pss.removeLine(datas, l);
									}
					    		}
					    	}
							pss.v = datas;

							double l[] = pss.fitLineBetweenChannels(sigma, sl[lindex].minChannel, sl[lindex].maxChannel, sl[lindex].yMin, sl[lindex].yMax, false);

			    			sl[lindex].vel = l[0];
			    			sl[lindex].velError = l[4];
			    			sl[lindex].width = l[1];
			    			sl[lindex].widthError = l[5];
			    			sl[lindex].peakT = l[2];
			    			sl[lindex].peakTError = l[6];
			    			sl[lindex].area = l[3];
			    			sl[lindex].areaError = l[7];
							sl[lindex].freq = l[10];

							ps = pss;

							double max = Math.abs(l[2]);
							if (max < ProcessSpectrum.TIMES_SIGMA * rms0 && sl[lindex].enabled) {
								sl[lindex].deleted = true;
								sl[lindex].enabled = false;
								i = 0;
								iter = 0;
								repeat = true;
								break;
							}
				    	}
				    }
				    double rms = getRMS(spectrum, sl);
				    if (rms < rms0 || repeat) {
				    	rms0 = rms;
				    	sl0 = sl.clone();
				    } else {
				    	sl = sl0;
				    	break;
				    }
			    }
			}
		}
		return new Object[] {sl, ps};
	}
}
