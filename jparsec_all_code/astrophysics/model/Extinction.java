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
package jparsec.astrophysics.model;

import jparsec.astronomy.Star;
import jparsec.astronomy.Star.LUMINOSITY_CLASS;
import jparsec.util.JPARSECException;

import java.io.Serializable;

/**
 * A class to model the extinction towards the ISM.<P>
 *
 * The model determines the total absorption at wavelengths
 * between 0.10 and 3.33 um using the algorithm developed by
 * Cardelli, Clayton, and Mathis (1989)
 * <a href="http://adsabs.harvard.edu/cgi-bin/nph-bib_query?1989ApJ...345..245C&db_key=AST"> [ApJ 345 245] </a>.<P>
 *
 * Code based on the original Doug Welch's 'Excellent' Absorption
 * Law Calculator in Javascript.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Extinction implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * The ratio of total to selective absorption at V. <BR>
	 * This parameters is correlated with the average grain size.
	 * Commonly accepted values are 3.1 for the
	 * Milky Way and 3.3 for the LMC.
	 */
	public double rv;
	/**
	 * The total absorption in mags at V.
	 */
	public double av;

	private double observedBV = 0.0;
	private String sp = "";
	private boolean main = true;

	 /**
	  * Set to true (default value) to enable the extinction
	  */
	 public boolean enableExtinction = true;

	/**
	 * The constructor.
	 * @param r The rv parameter.
	 * @param a The av parameter.
	 */
	public Extinction(double r, double a)
	{
		this.rv = r;
		this.av = a;
	}

	/**
	 * Standard constructor for the Milky Way using
	 * a given hydrogen column density.
	 * @param nH The column density in cm^-2.
	 */
	public Extinction(double nH)
	{
		this.rv = 3.1;
		this.av = Extinction.getAv(nH);
	}

	/**
	 * Constructor for a given B-V color index.
	 * @param rv The value for rv parameter.
	 * @param BminusV The color index.
	 * @param spectralType The spectral type as it is ussually given: OBAFGKM
	 * followed by the subtype number.
	 * @param mainSequence True for a main sequence star, false for a giants (class III).
	 * @throws JPARSECException If an error occurs.
	 */
	public Extinction(double rv, double BminusV, String spectralType, boolean mainSequence)
	throws JPARSECException {
		LUMINOSITY_CLASS lclass = LUMINOSITY_CLASS.MAIN_SEQUENCE_V;
		if (!mainSequence) lclass = LUMINOSITY_CLASS.GIANTS_III;
		double bv = Star.getStarBminusV(Star.getEffectiveTemperature(spectralType), lclass);
		observedBV = BminusV;
		this.sp = spectralType;
		this.main = mainSequence;
		bv = BminusV - bv;
		this.rv = rv;
		this.av = rv * bv;
	}

	/**
	 * Clones this instance.
	 */
	@Override
	public Object clone()
	{
		Extinction e = new Extinction(this.rv, this.av);
		e.enableExtinction = this.enableExtinction;
		e.main = this.main;
		e.maximumWavelengthToComputeExtinction = this.maximumWavelengthToComputeExtinction;
		e.observedBV = this.observedBV;
		e.sp = this.sp;
		return e;
	}
	/**
	 * Checks if this instance is equals to another.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Extinction)) return false;

		Extinction that = (Extinction) o;

		if (Double.compare(that.rv, rv) != 0) return false;
		if (Double.compare(that.av, av) != 0) return false;
		if (Double.compare(that.observedBV, observedBV) != 0) return false;
		if (main != that.main) return false;
		if (enableExtinction != that.enableExtinction) return false;
		if (Double.compare(that.maximumWavelengthToComputeExtinction, maximumWavelengthToComputeExtinction) != 0)
			return false;

		return !(sp != null ? !sp.equals(that.sp) : that.sp != null);
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		temp = Double.doubleToLongBits(rv);
		result = (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(av);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(observedBV);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (sp != null ? sp.hashCode() : 0);
		result = 31 * result + (main ? 1 : 0);
		result = 31 * result + (enableExtinction ? 1 : 0);
		temp = Double.doubleToLongBits(maximumWavelengthToComputeExtinction);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
	/**
	 * Returns the observed B minus V.
	 * @return The B-V color.
	 */
	public double getObservedBminusV()
	{
		return this.observedBV;
	}
	/**
	 * Returns the spectral type.
	 * @return The spectral type.
	 */
	public String getSpectralType()
	{
		return this.sp;
	}
	/**
	 * Returns if the star belongs to the Main Sequence.
	 * @return True or false.
	 */
	public boolean isMainSequence()
	{
		return this.main;
	}

	/**
	 * Holds maximum wavelength to compute extinction in microns. Default value
	 * is 3.33 microns. If it is increased (not more than 5.0 microns), the Rieke and
	 * Lebofski 1985 law already integrated in the Cardelli 1989 parameterization will be
	 * extrapolated up to this wavelength.
	 */
	public double maximumWavelengthToComputeExtinction = 3.33;

	/**
	 * Compute the total absorption at certain wavelength.
	 * @param wv Wavelength in microns.
	 * @return The A_lambda parameter (0 if {@linkplain #enableExtinction} is false).
	 * @throws JPARSECException If the wavelength is out of range.
	 */
	public double compute(double wv)
	throws JPARSECException {
		if (!enableExtinction) return 0.0;

		   double x = 1.0 / wv, xx = 0.0;
		   double ax = 0.0, bx = 0.0, y = 0.0, fa = 0.0, fb = 0.0;

		   if (wv < 0.1 || wv > maximumWavelengthToComputeExtinction)
			   throw new JPARSECException("Wavelength should be between 0.1 and "+maximumWavelengthToComputeExtinction+" microns.");

		   if (x < 1.1) {
		      ax =  0.574 * Math.pow(x, 1.61);
		      bx = -0.527 * Math.pow(x, 1.61);
		   }

		  if (x >= 1.1 && x < 3.3) {
		      y = (x - 1.82);
		      ax = 1.0 + (0.17699 - 0.50447 * y) * y;
		      ax -= 0.02427 * Math.pow(y, 3.0);
		      ax += 0.72085 * Math.pow(y, 4.0);
		      ax += 0.01979 * Math.pow(y, 5.0);
		      ax -= 0.77530 * Math.pow(y, 6.0);
		      ax += 0.32999 * Math.pow(y, 7.0);

		      bx = 1.41338 * y;
		      bx += 2.28305 * Math.pow(y, 2.0);
		      bx += 1.07233 * Math.pow(y, 3.0);
		      bx -= 5.38434 * Math.pow(y, 4.0);
		      bx -= 0.62251 * Math.pow(y, 5.0);
		      bx += 5.30260 * Math.pow(y, 6.0);
		      bx -= 2.09002 * Math.pow(y, 7.0);

		  }

		  if (x >=3.3 && x < 8.0) {
		      if (x >= 5.9 && x < 8.0) {
		    	  xx = (x - 5.9);
		    	  fa = Math.pow(xx, 2.0) * (-0.04473 - 0.009779 * xx);
		    	  fb = Math.pow(xx, 2.0) * ( 0.2130  + 0.1207 * xx);
		      } else {
		    	  fa = 0.0;
		    	  fb = 0.0;
		      }

		      ax = 1.752 - 0.316*x;
		      ax -= 0.104 / (Math.pow((x - 4.67), 2.0) + 0.341);
		      ax += fa;

		      bx = -3.090 + 1.825*x;
		      bx += 1.206 / (Math.pow((x - 4.62), 2.0) + 0.263);
		      bx += fb;
		  }

		  if (x >= 8.0 && x <= 10.0) {
		      xx = (x - 8.0);

		      ax = -1.073 - 0.628 * xx + 0.137 * Math.pow(xx, 2.0);
		      ax -= 0.070 * Math.pow(xx, 3.0);

		      bx = 13.670 + 4.257 * xx - 0.420 * Math.pow(xx, 2.0);
		      bx += 0.374 * Math.pow(xx, 3.0);
		  }

		  double al = (ax + bx / rv) * av;

		  return al;
	}

	/**
	 * Returns the selective extinction E(B-V) = A(B) - A(v).
	 * It is just {@linkplain Extinction#av}/{@linkplain Extinction#rv}.
	 * @return The selective extinction.
	 */
	public double getEBminusV()
	{
		return av / rv;
	}

	/**
	 * Returns the extinction Av (to be used as input in the
	 * instance of {@linkplain Extinction}) assuming a column
	 * density of hydrogen.<P>
	 * This method assumes certain relationship between the gas
	 * and the dust in the Milky Way, supported by UV studies by
	 * Bohlin et al. 1978, Diplas et al. 1994, and Predehl et al. 1995.
	 * Values for nH / Av (atoms cm^-2 mag^-1) range from 5.8E21 of
	 * the first reference to 1.8E21 of the last one, which is the value
	 * used here.
	 *
	 * @param nH in atoms cm^-2.
	 * @return The total extinction A in V band.
	 */
	public static double getAv(double nH)
	{
		return nH / (1.8 * 1.0E21);
	}
}
