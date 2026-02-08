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
package jparsec.astrophysics;

import java.io.*;
import java.util.Arrays;

import jparsec.astronomy.Photometry;
import jparsec.astrophysics.gildas.Spectrum30m;
import jparsec.astrophysics.gildas.SpectrumLine;
import jparsec.astrophysics.gildas.Spectrum30m.XUNIT;
import jparsec.graph.CreateChart;
import jparsec.graph.DataSet;
import jparsec.io.*;
import jparsec.math.Constant;
import jparsec.math.Interpolation;
import jparsec.math.MeasureElement;
import jparsec.math.Table;
import jparsec.util.*;

/**
 * This class holds data like spectra or SEDs.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Spectrum implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Holds the spectrum/SED. The set of points
	 * should be given as flux as function of channel
	 * number to later create a Gildas spectrum.
	 */
	public FluxElement[] spectrum;

	/**
	 * Right ascension of the source in radians.
	 */
	public double ra;
	/**
	 * Declination of the source in radians.
	 */
	public double dec;
	/**
	 * Epoch of coordinates as a Julian day.
	 */
	public double epochJD;
	/**
	 * Source name.
	 */
	public String source;
	/**
	 * Observing time as a JD in UT.
	 */
	public double observingTimeJD;
	/**
	 * Reference channel.
	 */
	public double referenceChannel;
	/**
	 * Reference frequency in MHz.
	 */
	public double referenceFrequency;
	/**
	 * Image frequency in MHz.
	 */
	public double imgFrequency;
	/**
	 * Reference velocity in km/s.
	 */
	public double referenceVelocity;
	/**
	 * Velocity resolution in km/(s channel).
	 */
	public double velocityResolution;
	/**
	 * Spectrum rms.
	 */
	public double sigmaRMS;
	/**
	 * Scan number.
	 */
	public int scanNumber;
	/**
	 * Observation number.
	 */
	public int observationNumber;
	/**
	 * Backend.
	 */
	public String backend;
	/**
	 * Line name.
	 */
	public String line;
	/**
	 * Offset X, arcsec.
	 */
	public double offsetX;
	/**
	 * Offset Y, arcsec.
	 */
	public double offsetY;
	/**
	 * Integration time, s.
	 */
	public double integrationTime;
	/**
	 * Beam efficiency.
	 */
	public double beamEfficiency;

	/**
	 * Simple constructor.
	 * @param s The SED/spectrum points. The set of points
	 * should be given as flux as function of channel number
	 * to later create a Gildas spectrum.
	 */
	public Spectrum(FluxElement s[])
	{
		if (s != null) this.spectrum = s.clone();
	}

	/**
	 * Constructor for a set of values representing the spectrum,
	 * as intensities as function of channel number channel &gt;= 1.
	 * @param m The intensities.
	 * @throws JPARSECException If an error occurs.
	 */
	public Spectrum(MeasureElement m[]) throws JPARSECException {
		this(new Table(m));
	}

	/**
	 * Constructor for a Table object. Intensities are taken from the
	 * Table object, while channel number &gt;= 1 (ordered as in the Table
	 * following the indexes) is taken as x coordinate.
	 * @param table The table.
	 * @throws JPARSECException If it has more than 1 dimension.
	 */
	public Spectrum(Table table) throws JPARSECException {
		if (table.getDimensions() > 1) throw new JPARSECException("Table must have 1 dimension.");
		double v[][][] = table.getValues();
		double dv[][][] = table.getErrors();

		int n = v[0][0].length;
		spectrum = new FluxElement[n];
		for (int i=0; i<n; i++) {
			spectrum[i] = new FluxElement(new MeasureElement(i+1, 0, null), new MeasureElement(v[0][0][i], dv[0][0][i], table.getUnit()));
		}
	}

	/**
	 * Constructor for a spectrum defined as a string table.
	 * @param s The table.
	 * @param separator Table separator.
	 * @param xColumn The x column in the table, starting from 1.
	 * @param yColumn The y column in the table
	 * @param dxColumn The dx column in the table. A null value will produce 0 as result.
	 * @param dyColumn The dy column in the table. A null value will produce 0 as result.
	 * @param xUnit x units. To later create a Gildas spectrum set the x unit to null and
	 * use channel number for the x input data in the table, although velocity/frequency
	 * are also supported and conversion is done automatically.
	 * @param yUnit y units.
	 * @throws JPARSECException If the table contains no data or the input columns are invalid.
	 */
	public Spectrum(String s[], String separator, int xColumn, int yColumn, Integer dxColumn, Integer dyColumn,
			String xUnit, String yUnit) throws JPARSECException
	{
		if (s == null || s.length == 0) throw new JPARSECException("Input table must contain valid data.");
		boolean skip = false;
		String newSep = DataSet.replaceAll(separator, "  ", " ", true);
		if (newSep.equals(" ")) skip = true;
		int n = FileIO.getNumberOfFields(s[0], separator, skip);
		if (xColumn < 1 || yColumn < 1 || xColumn > n || yColumn > n) throw new JPARSECException("x and y columns must be between 1 and "+n+".");

		this.spectrum = new FluxElement[s.length];
		for (int i=0; i<s.length; i++)
		{
			String xval = FileIO.getField(xColumn, s[i], separator, skip);
			double dxval = 0.0;
			if (dxColumn != null) dxval = DataSet.parseDouble(FileIO.getField(dxColumn.intValue(), s[i], separator, skip).trim());
			MeasureElement x = new MeasureElement(xval, dxval, xUnit);

			String yval = FileIO.getField(yColumn, s[i], separator, skip);
			double dyval = 0.0;
			if (dyColumn != null) dyval = DataSet.parseDouble(FileIO.getField(dyColumn.intValue(), s[i], separator, skip).trim());
			MeasureElement y = new MeasureElement(yval, dyval, yUnit);

			this.spectrum[i] = new FluxElement(x, y);
		}
	}

	/**
	 * Returns the current x values as an array.
	 * @param newUnit Output unit in standard conventions. Some constants defined
	 * in {@linkplain MeasureElement}. Can be null for the same input unit.
	 * @return The array.
	 * @throws JPARSECException If an error occurs.
	 */
	public String[] getXs(String newUnit)
	throws JPARSECException {
		if (newUnit == null) newUnit = spectrum[0].x.unit;
		String out[] = new String[this.spectrum.length];
		for (int i=0; i<out.length; i++)
		{
			out[i] = this.spectrum[i].getX(newUnit).value;
		}
		return out;
	}

	/**
	 * Returns the current y values as an array.
	 * @param newUnit Output unit in standard conventions. Some constants defined
	 * in {@linkplain MeasureElement}. Can be null for the same input unit.
	 * @return The array.
	 * @throws JPARSECException If an error occurs.
	 */
	public String[] getYs(String newUnit)
	throws JPARSECException {
		if (newUnit == null) newUnit = spectrum[0].y.unit;
		String out[] = new String[this.spectrum.length];
		for (int i=0; i<out.length; i++)
		{
			out[i] = this.spectrum[i].getY(newUnit).value;
		}
		return out;
	}

	/**
	 * Returns the current x error values as an array.
	 * @param newUnit Output unit in standard conventions. Some constants defined
	 * in {@linkplain MeasureElement}. Can be null for the same input unit.
	 * @return The array.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[] getXErrors(String newUnit)
	throws JPARSECException {
		if (newUnit == null) newUnit = spectrum[0].x.unit;
		double out[] = new double[this.spectrum.length];
		for (int i=0; i<out.length; i++)
		{
			out[i] = this.spectrum[i].getX(newUnit).error;
		}
		return out;
	}

	/**
	 * Returns the current y error values as an array.
	 * @param newUnit Output unit in standard conventions. Some constants defined
	 * in {@linkplain MeasureElement}. Can be null for the same input unit.
	 * @return The array.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[] getYErrors(String newUnit)
	throws JPARSECException {
		if (newUnit == null) newUnit = spectrum[0].y.unit;
		double out[] = new double[this.spectrum.length];
		for (int i=0; i<out.length; i++)
		{
			out[i] = this.spectrum[i].getY(newUnit).error;
		}
		return out;
	}

	/**
	 * Clones this instance.
	 */
	@Override
	public Spectrum clone()
	{
		FluxElement f[] = new FluxElement[spectrum.length];
		for (int i=0; i<f.length; i++) {
			f[i] = spectrum[i].clone();
		}
		Spectrum s = new Spectrum(f);
		s.backend = this.backend;
		s.beamEfficiency = this.beamEfficiency;
		s.dec = this.dec;
		s.epochJD = this.epochJD;
		s.integrationTime = this.integrationTime;
		s.line = this.line;
		s.observationNumber = this.observationNumber;
		s.observingTimeJD = this.observingTimeJD;
		s.offsetX = this.offsetX;
		s.offsetY = this.offsetY;
		s.ra = this.ra;
		s.referenceChannel = this.referenceChannel;
		s.referenceVelocity = this.referenceVelocity;
		s.referenceFrequency = this.referenceFrequency;
		s.imgFrequency = this.imgFrequency;
		s.scanNumber = this.scanNumber;
		s.sigmaRMS = this.sigmaRMS;
		s.source = this.source;
		s.velocityResolution = this.velocityResolution;
		return s;
	}

	/**
	 * Check is this instance is equals to another.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Spectrum)) return false;

		Spectrum spectrum1 = (Spectrum) o;

		if (Double.compare(spectrum1.ra, ra) != 0) return false;
		if (Double.compare(spectrum1.dec, dec) != 0) return false;
		if (Double.compare(spectrum1.epochJD, epochJD) != 0) return false;
		if (Double.compare(spectrum1.observingTimeJD, observingTimeJD) != 0) return false;
		if (Double.compare(spectrum1.referenceChannel, referenceChannel) != 0) return false;
		if (Double.compare(spectrum1.referenceFrequency, referenceFrequency) != 0) return false;
		if (Double.compare(spectrum1.imgFrequency, imgFrequency) != 0) return false;
		if (Double.compare(spectrum1.referenceVelocity, referenceVelocity) != 0) return false;
		if (Double.compare(spectrum1.velocityResolution, velocityResolution) != 0) return false;
		if (Double.compare(spectrum1.sigmaRMS, sigmaRMS) != 0) return false;
		if (scanNumber != spectrum1.scanNumber) return false;
		if (observationNumber != spectrum1.observationNumber) return false;
		if (Double.compare(spectrum1.offsetX, offsetX) != 0) return false;
		if (Double.compare(spectrum1.offsetY, offsetY) != 0) return false;
		if (Double.compare(spectrum1.integrationTime, integrationTime) != 0) return false;
		if (Double.compare(spectrum1.beamEfficiency, beamEfficiency) != 0) return false;
		if (!Arrays.equals(spectrum, spectrum1.spectrum)) return false;
		if (source != null ? !source.equals(spectrum1.source) : spectrum1.source != null) return false;
		if (backend != null ? !backend.equals(spectrum1.backend) : spectrum1.backend != null) return false;

		return !(line != null ? !line.equals(spectrum1.line) : spectrum1.line != null);
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		result = spectrum != null ? Arrays.hashCode(spectrum) : 0;
		temp = Double.doubleToLongBits(ra);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(dec);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(epochJD);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (source != null ? source.hashCode() : 0);
		temp = Double.doubleToLongBits(observingTimeJD);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(referenceChannel);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(referenceFrequency);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(imgFrequency);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(referenceVelocity);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(velocityResolution);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(sigmaRMS);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + scanNumber;
		result = 31 * result + observationNumber;
		result = 31 * result + (backend != null ? backend.hashCode() : 0);
		result = 31 * result + (line != null ? line.hashCode() : 0);
		temp = Double.doubleToLongBits(offsetX);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(offsetY);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(integrationTime);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(beamEfficiency);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	/**
	 * Returns a perfect Gaussian spectrum given the parameters of the Gaussian.
	 * @param np The number of points in the spectrum.
	 * @param line The fit to the line.
	 * @return The spectrum. The main parameters like reference channel and velocity,
	 * rest frequency, and velocity resolution are set.
	 */
	public static Spectrum getGaussianSpectrum(int np, SpectrumLine line) {
		return Spectrum.getGaussianSpectrum(line.vel, line.peakT, line.width, np, line.freq, 4*line.width, 0);
	}

	/**
	 * Returns a Gaussian spectrum given the parameters of the Gaussian.
	 * @param v The central velocity, km/s.
	 * @param t The peak temperature, K.
	 * @param w The width of the Gaussian, km/s. The spectrum will have
	 * velocity limits from v - 2 * w to w + 2 * w.
	 * @param np The number of points in the spectrum.
	 * @param nu0 The central frequency, MHz.
	 * @param totalW The total width of the spectrum in the velocity axis, km/s.
	 * @param noiseLevel The level of noise. Set to 0 for a perfect Gaussian. The noise
	 * will be a random number between +/- noiseLevel.
	 * @return The spectrum. The main parameters like reference channel and velocity,
	 * rest frequency, and velocity resolution are set.
	 */
	public static Spectrum getGaussianSpectrum(double v, double t, double w, int np, double nu0,
			double totalW, double noiseLevel) {
		FluxElement fluxes[] = new FluxElement[np];

		double v0 = v - totalW / 2.0;
		double vf = v + totalW / 2.0;
		double velRes = (vf - v0) / (np - 1.0);
		double refVel = v, refChannel = np/2.0;
		for (int j=0; j<np; j++)
		{
			double tmb = (t / Math.exp(0.5 * Math.pow((1.0 + j - refChannel) / (w / (velRes * 2.0 * Math.sqrt(2.0 * Math.log(2.0)))), 2.0)));
			MeasureElement mx = new MeasureElement(j+1, 0, null);
			MeasureElement my = new MeasureElement(tmb + (Math.random() - 0.5) * 2.0 * noiseLevel, 0, MeasureElement.UNIT_Y_K);
			fluxes[j] = new FluxElement(mx, my);
		}
		Spectrum sp = new Spectrum(fluxes);
		sp.referenceChannel = refChannel;
		sp.referenceVelocity = refVel;
		sp.referenceFrequency = nu0;
		sp.velocityResolution = velRes;
		return sp;
	}

	/**
	 * Returns this spectrum as a Table object in 1d.
	 * @return The table object.
	 */
	public Table getAsTable() {
		MeasureElement m[] = new MeasureElement[spectrum.length];
		for (int i=0; i<m.length; i++) {
			m[i] = spectrum[i].y;
		}
		return new Table(m);
	}

    /**
     * Returns a chart with certain spectrum.
     * @param width Chart width in pixels.
     * @param height Chart height in pixels.
     * @param xUnit The unit for the x axis.
     * @return The chart.
     * @throws JPARSECException If an error occurs.
     */
    public CreateChart getChart(int width, int height, XUNIT xUnit)
    throws JPARSECException {
    	if (this.spectrum[0].y.unit != null && !this.spectrum[0].y.unit.equals("K")) {
	    	Spectrum scopy = this.clone();
	    	for (int i=0; i<scopy.spectrum.length; i++) {
	    		scopy.spectrum[i].y.unit = null;
	    	}
	    	Spectrum30m s30m = new Spectrum30m(scopy);
	    	CreateChart ch = s30m.getChart(width, height, xUnit);
	    	ch.getChartElement().yLabel = "";
	    	if (!this.spectrum[0].y.unit.equals("")) ch.getChartElement().yLabel = Translate.translate(810) + " ("+this.spectrum[0].y.unit+")";
	    	return ch;
    	}
    	Spectrum30m s30m = new Spectrum30m(this);
    	return s30m.getChart(width, height, xUnit);
    }

	/**
	 * Returns the transmitancy spectrum of a given filter.
	 * @param filterID The filter.
	 * @param np The number of points in the output spectrum.
	 * @return The spectrum.
	 * @throws JPARSECException If the filter does not exist.
	 */
	public static Spectrum getFilterSpectrum(Photometry.FILTER filterID, int np) throws JPARSECException {
		double w[] = Photometry.getFilterWavelengths(filterID), s[] = Photometry.getFilterTransmitancy(filterID);
		FluxElement f[] = new FluxElement[np];
		double toMicron = 1;
		if (filterID.name().startsWith("JOHNSON")) toMicron = 1e-4;
		int minX = DataSet.getIndexOfMinimum(w), maxX = DataSet.getIndexOfMaximum(w);
		double maxNu = Constant.SPEED_OF_LIGHT / (w[minX] * toMicron); // MHz
		double minNu = Constant.SPEED_OF_LIGHT / (w[maxX] * toMicron); // MHz
	
		Interpolation interp = new Interpolation(w, s, false);
		double newNu[] = new double[f.length];
		for (int i=0; i<f.length; i++) {
			newNu[i] = minNu + ((maxNu - minNu) * i) / (f.length - 1.0);
			double l = Constant.SPEED_OF_LIGHT / (newNu[i] * toMicron);
			f[i] = new FluxElement(new MeasureElement(i+1, 0, null), new MeasureElement(interp.splineInterpolation(l), 0, ""));
		}
		Spectrum sp = new Spectrum(f);
		int refCh = f.length/2;
		sp.referenceChannel = 1+refCh;
		sp.referenceVelocity = 0;
		sp.referenceFrequency = newNu[refCh];
		double dnu = newNu[refCh+1] - sp.referenceFrequency;
		sp.velocityResolution = -Constant.SPEED_OF_LIGHT * 0.001 * dnu / sp.referenceFrequency;
		sp.source = filterID.name();
		sp.line = filterID.name();
		return sp;
	}
}
