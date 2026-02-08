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

import java.io.Serializable;

import jparsec.graph.DataSet;
import jparsec.math.Complex;
import jparsec.math.Constant;
import jparsec.math.Integration;
import jparsec.util.JPARSECException;

/**
 * An implementation of the Mie theory.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class MieTheory implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Particle size in physical units.
	 */
	public double grainSize;
	 /**
	  * Wavelength in the same units as grain size.
	  */
	public double lambda;
	/**
	 * Complex refractive index of the grain for the input wavelength.
	 */
	public Complex cxref;
	/**
	 * Number of angles to compute. Greater than 1, and as large as possible,
	 * but must be lower than a limits of 1000. A value of 10 adequate.
	 */
	public int nang;

	/**
	 * (Incid. E perp. to scatt. plane, scatt. E perp. to scatt. plane).
	 * 2*(nang-1) compounds.
	 */
	private Complex[] cxs1;
	/**
	 * (incid. E parr. to scatt. plane, scatt. E parr. to scatt. plane).
	 * 2*(nang-1) compounds.
	 */
	private Complex[] cxs2;

	/**
	 * C_ext/(pi*a*a) = efficiency factor for extinction.
	 */
	private double qext;
	/**
	 * C_sca/(pi*a*a) = efficiency factor for scattering.
	 */
	private double qsca;
	/**
	 * 4*pi*(dC_sca/domega)/(pi*a*a) = backscattering efficiency.
	 */
	private double qback;
	/**
	 * Mean average of cos(theta) for scattering.
	 */
	private double gsca;
	/**
	 * C_abs/(pi*a*a) = efficiency factor for absorption.
	 */
	private double qabs;

	private static final Complex CXONE = new Complex(1.0, 0.0);

	/**
	 * Index for absorption coefficient in the output array of the
	 * Mie theory.
	 */
	public static final int INDEX_OF_ABSORPTION_COEFFICIENT = 0;
	/**
	 * Index for extinction coefficient in the output array of the
	 * Mie theory.
	 */
	public static final int INDEX_OF_EXTINCTION_COEFFICIENT = 1;
	/**
	 * Index for scattering coefficient in the output array of the
	 * Mie theory.
	 */
	public static final int INDEX_OF_SCATTERING_COEFFICIENT = 2;
	/**
	 * Index for backscattering coefficient in the output array of the
	 * Mie theory.
	 */
	public static final int INDEX_OF_BACKSCATTERING_COEFFICIENT = 3;
	/**
	 * Index for cosine average coefficient in the output array of the
	 * Mie theory.
	 */
	public static final int INDEX_OF_COSINE_AVERAGE_COEFFICIENT = 4;

	/**
	 * Empty constructor.
	 */
	public MieTheory() {}

	/**
	 * Explicit constructor. This constructor already calls {@linkplain MieTheory#applyBohrenHuffmanMieTheory()} to obtain
	 * the coefficients.
	 * @param a Particle size in physical units.
	 * @param lambda Wavelength in the same units.
	 * @param cxref Complex refractive index of the grain for the input wavelength.
	 * @param nang Number of angles to compute. Greater than 1, and as large as possible,
	 * but must be lower than a limits of 1000.
	 * @throws JPARSECException If an error occurs.
	 */
	public MieTheory(double a, double lambda, Complex cxref, int nang)
	throws JPARSECException {
		this.grainSize = a;
		this.lambda = lambda;
		this.cxref = cxref;
		this.nang = nang;

		this.applyBohrenHuffmanMieTheory();
	}

	/**
	 * Bohren-Huffman implementation of Mie theory, to calculate scattering
	 * and absorption by a homogenous isotropic sphere.<P>
	 *
	 * Original program taken from Bohren and Huffman (1983), Appendix A.
	 * Modified by B. T. Draine, Princeton Univ. Obs., 90/10/26 in order
	 * to compute the mean value of cos(theta).
	 *
	 * @throws JPARSECException If an error occurs.
	 */
	public void applyBohrenHuffmanMieTheory()
	throws JPARSECException
	{
		double x = 2.0 * Math.PI * grainSize / lambda;

		double mx = cxref.abs() * x;
		if (mx > 1000.0 || mx < 0.001)
			JPARSECException.addWarning("Mie theory should not be applied when |m|*x is outside " +
					"interval 0.001 - 1000, being m the refractive index and x = 2 PI a / lambda.");

	/* .. Array Arguments .. */
		cxs1 = new Complex[2*nang];
		cxs2 = new Complex[2*nang];
	/* .. Local Scalars ..*/
	      Complex cxan = new Complex(), cxan1 = new Complex(), cxbn = new Complex(),
	      	cxbn1 = new Complex(), cxxi, cxy, cxxi1; // cxxi0
	      Complex cxtemp;
	      double apsi, apsi1, chi, chi0, chi1, dang, fn, p, pii,
	        rn, t, theta, xstop, ymod;  // apsi0
	      double  dn, dx, psi, psi0, psi1;
	      int  j, jj, n, nmx, nn, nstop;
	/* .. Local Arrays ..*/
	      double amu[] = new double [nang+1];
	      double pi[] = new double [nang+1];
	      double pi0[] = new double [nang+1];
	      double pi1[] = new double [nang+1];
	      double tau[] = new double [nang+1];

	      if (nang > 1000 || nang < 2) {
	        throw new JPARSECException("number of angles cannot be lower than 2 or larger than 1000.");
	      }

	      pii = 4.0*Math.atan(1.0);
	      dx = x;
	      cxy = (new Complex(x,0.0)).multiply(cxref);

	/* Series expansion terminated after NSTOP terms */
	      xstop = x + 4.0*Math.pow(x,0.3333) + 2.0;
	      nstop = (int) xstop;
	      ymod = cxy.abs();
	      nmx = nstop;
	      if (ymod > xstop) nmx = (int) ymod;
	      nmx += 15;

	      Complex cxd[] = new Complex[nmx+1];


	      dang = .50*pii/ (float)(nang-1);
	      for (j = 1; j<=nang; j++) {

	        theta = (float)(j-1)*dang;
	        amu[j] = Math.cos(theta);
	      }

	/* Logarithmic derivative D(J) calculated by downward recurrence
	    beginning with initial value (0.,0.) at J=NMX */

	      cxd[nmx] = new Complex(0.0,0.0);
	      nn = nmx - 1;

	      for (n = 1; n<= nn; n++) {
	        rn = nmx - n + 1;
	/*        cxd(nmx-n) = (rn/cxy) - (1.0/(cxd(nmx-n+1)+rn/cxy)) */
	        cxtemp=cxd[nmx-n+1].add((new Complex(rn,0.0)).div(cxy));
	        cxtemp=CXONE.div(cxtemp);
	        cxd[nmx-n]=((new Complex(rn,0.0)).div(cxy)).substract(cxtemp);
	      }

	      for ( j = 1; j <= nang; j++) {
	        pi0[j] = 0.0;
	        pi1[j] = 1.0;
	      }
	      nn = 2*nang - 1;
	      for(j = 1; j<= nn; j++) {
	        cxs1[j] = new Complex(0.0,0.0);
	        cxs2[j] = new Complex(0.0,0.0);
	      }

	/* Riccati-Bessel functions with real argument X
	    calculated by upward recurrence */

	      psi0 = Math.cos(dx);
	      psi1 = Math.sin(dx);
	      chi0 = -Math.sin(x);
	      chi1 = Math.cos(x);
//	      apsi0 = psi0;
	      apsi1 = psi1;
//	      cxxi0 = new Complex(apsi0,-chi0);
	      cxxi1 = new Complex(apsi1,-chi1);
	      qsca = 0.0;
	      gsca = 0.0;

	    for ( n = 1; n <= nstop; n++) {

	        dn = n;
	        rn = n;
	        fn = (2.0*rn+1.0)/(rn*(rn+1.0));
	        psi = (2.0*dn-1.0)*psi1/dx - psi0;
	        apsi = psi;
	        chi = (2.0*rn-1.0)*chi1/x - chi0;
	        cxxi = new Complex(apsi,-chi);
	/* Store previous values of AN and BN for use
	    in computation of g=mean of cos(theta) */
	        if (n>1) {
	          cxan1 = cxan;
	          cxbn1 = cxbn;
	        }

	/* Compute AN and BN:*/
	/*        cxan = (cxd(n)/cxref+rn/x)*apsi - apsi1; */

	        cxan=cxd[n].div(cxref);
	        cxan=cxan.add(new Complex(rn/x,0.0));
	        cxan=cxan.multiply(new Complex(apsi,0.0));
	        cxan=cxan.substract(new Complex(apsi1,0.0));

	/*        cxan = cxan/((cxd(n)/cxref+rn/x)*cxxi-cxxi1); */
	        cxtemp=cxd[n].div(cxref);
	        cxtemp=cxtemp.add(new Complex(rn/x,0.0));
	        cxtemp=cxtemp.multiply(cxxi);
	        cxtemp=cxtemp.substract(cxxi1);
	        cxan=cxan.div(cxtemp);

	/*        cxbn = (cxref*cxd(n)+rn/x)*apsi - apsi1; */
	        cxbn=cxref.multiply(cxd[n]);
	        cxbn=cxbn.add(new Complex(rn/x,0.0));
	        cxbn=cxbn.multiply(new Complex(apsi,0.0));
	        cxbn=cxbn.substract(new Complex(apsi1,0.0));
	/*        cxbn = cxbn/((cxref*cxd(n)+rn/x)*cxxi-cxxi1); */
	        cxtemp=cxref.multiply(cxd[n]);
	        cxtemp=cxtemp.add(new Complex(rn/x,0.0));
	        cxtemp=cxtemp.multiply(cxxi);
	        cxtemp=cxtemp.substract(cxxi1);
	        cxbn=cxbn.div(cxtemp);

	/* Augment sums for qsca and g=<cos(theta)> */
	/*        qsca = qsca + (2.*rn+1.)*(cabs(cxan)**2+cabs(cxbn)**2); */
	 qsca += (2.*rn+1.)*(cxan.abs()*cxan.abs()+cxbn.abs()*cxbn.abs());
	 gsca += ((2.*rn+1.)/(rn*(rn+1.)))*(cxan.real*cxbn.real+cxan.imaginary*cxbn.imaginary);

	        if (n>1) {
	          gsca += ((rn-1.)*(rn+1.)/rn)*(cxan1.real*cxan.real+
	           cxan1.imaginary*cxan.imaginary+cxbn1.real*cxbn.real+cxbn1.imaginary*cxbn.imaginary);
	        }

	        for ( j = 1; j<= nang; j++) {
	          jj = 2*nang - j;
	          pi[j] = pi1[j];
	          tau[j] = rn*amu[j]*pi[j] - (rn+1.0)*pi0[j];
	          p = Math.pow(-1.0,n-1);
	/*          cxs1[j] = cxs1[j] + fn*(cxan*pi[j]+cxbn*tau[j]); */
	          cxtemp=cxan.multiply(new Complex(pi[j],0.0));
	          cxtemp=cxtemp.add(cxbn.multiply(new Complex(tau[j],0.0)));
	          cxtemp=(new Complex(fn,0.0)).multiply(cxtemp);
	          cxs1[j]=cxs1[j].add(cxtemp);
	          t = Math.pow(-1.0,n);
	/*          cxs2[j] = cxs2[j] + fn*(cxan*tau[j]+cxbn*pi[j]); */
	          cxtemp=cxan.multiply(new Complex(tau[j],0.0));
	          cxtemp=cxtemp.add(cxbn.multiply(new Complex(pi[j],0.0)));
	          cxtemp=(new Complex(fn,0.0)).multiply(cxtemp);
	          cxs2[j]=cxs2[j].add(cxtemp);

	          if (j!=jj) {
	/*            cxs1[jj] = cxs1[jj] + fn*(cxan*pi(j)*p+cxbn*tau(j)*t);*/
	            cxtemp=cxan.multiply(new Complex(pi[j]*p,0.0));
	            cxtemp=cxtemp.add(cxbn.multiply(new Complex(tau[j]*t,0.0)));
	            cxtemp=(new Complex(fn,0.0)).multiply(cxtemp);
	            cxs1[jj]=cxs1[jj].add(cxtemp);

	/*            cxs2[jj] = cxs2[jj] + fn*(cxan*tau(j)*t+cxbn*pi(j)*p); */
	            cxtemp=cxan.multiply(new Complex(tau[j]*t,0.0));
	            cxtemp=cxtemp.add(cxbn.multiply(new Complex(pi[j]*p,0.0)));
	            cxtemp=(new Complex(fn,0.0)).multiply(cxtemp);
	            cxs2[jj]=cxs2[jj].add(cxtemp);
	          }
	        }

	        psi0 = psi1;
	        psi1 = psi;
	        apsi1 = psi1;
	        chi0 = chi1;
	        chi1 = chi;
	        cxxi1 = new Complex(apsi1,-chi1);

	/*  For each angle J, compute pi_n+1
	    from PI = pi_n , PI0 = pi_n-1 */

	        for ( j = 1; j<= nang; j++) {
	          pi1[j] = ((2.*rn+1.)*amu[j]*pi[j]-(rn+1.)*pi0[j])/rn;
	          pi0[j] = pi[j];
	        }
	      } /*end of big for */

	/*  Have summed sufficient terms.
	     Now compute qsca,qext,qback,and gsca */
	      gsca = 2.* gsca/ qsca;
	      qsca = (2.0/(x*x))* qsca;
	      qext = (4.0/(x*x))*cxs1[1].real;
	      qback = (4.0/(x*x))*cxs1[2*nang-1].abs()*cxs1[2*nang-1].abs();
	      qabs = qext - qsca;

	      return;

	}

	/**
	 * Obtains the cross sections for absorption, scattering, and extinction of dust
	 * for a given size distribution. An integration
	 * is performed from 5E-3 microns up to the given maximum dust radius. This
	 * method usually requires a a lot of computing time.
	 *
	 * @param dust Dust properties.
	 * @param wavelength Wavelength in microns, between 0.001 and 1000.
	 * @param refractiveIndex The adequate refractive index for the current grain.
	 * @param np Number of points to use when integrating in size. Recommended
	 * values are from 500 to 5000 to get an accurate result.
	 * @return Opacity, extinction, scattering, backscattering, mean average of
	 * cos(theta). Units are cm2/g.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double[] getMieCoefficients(DustOpacity dust,
			double wavelength, double[] refractiveIndex,
			int np) throws JPARSECException
	{
		Complex cxref = new Complex(refractiveIndex[0], refractiveIndex[1]);

		double sizeMin = 5.0E-3 * Constant.MICRON_TO_CM;
		double sizeMax = dust.sizeMax * Constant.MICRON_TO_CM;
		double sizeStep = (sizeMax - sizeMin) / (np-1.0);
		double size = sizeMin - sizeStep;
		double waveCM = wavelength * Constant.MICRON_TO_CM;

		// See Draine, ApJ 636, 1114-1120 (2006), section 4, equation 4.
		double integralUpY_Qabs[] = new double[np];
		double integralUpY_Qext[] = new double[np];
		double integralUpY_Qsca[] = new double[np];
		double integralUpY_Qbsca[] = new double[np];
		double integralUpY_Qg[] = new double[np];
		double integralDownY[] = new double[np];
		double integralX[] = new double[np];

		String warn = "";
		String warns = JPARSECException.getWarnings();
		JPARSECException.clearWarnings();

		for (int i=0; i<np; i++)
		{
			size += sizeStep;
			MieTheory mt = new MieTheory(size, waveCM, cxref, 10);
			if (warn.equals("")) warn = JPARSECException.getWarnings();
			JPARSECException.clearWarnings();

			double dnda = Math.pow(size, -dust.sizeDistributionCoefficient);
			double mass = 4.0 * Math.PI * Math.pow(size, 3.0) * dust.grainDensity / 3.0;

			integralUpY_Qabs[i] = dnda * mt.qabs * Math.PI * size * size;
			integralUpY_Qext[i] = dnda * mt.qext * Math.PI * size * size;
			integralUpY_Qsca[i] = dnda * mt.qsca * Math.PI * size * size;
			integralUpY_Qbsca[i] = dnda * mt.qback * Math.PI * size * size;
			integralUpY_Qg[i] = dnda * mt.gsca * Math.PI * size * size;
			integralDownY[i] = dnda * mass;
			integralX[i] = size;
		}
		JPARSECException.setWarnings(warns);
		if (!warn.equals("")) JPARSECException.addWarning(warn);

		// Perform integrations
		double xmin = DataSet.getMinimumValue(integralX);
		double xmax = DataSet.getMaximumValue(integralX);
		double step = (xmax - xmin) / (np * 10.0);
		double integralUp_Qabs = Integration.simpleIntegrationForSortedX(integralX, integralUpY_Qabs, xmin, xmax, step);
		double integralUp_Qext = Integration.simpleIntegrationForSortedX(integralX, integralUpY_Qext, xmin, xmax, step);
		double integralUp_Qsca = Integration.simpleIntegrationForSortedX(integralX, integralUpY_Qsca, xmin, xmax, step);
		double integralUp_Qbsca = Integration.simpleIntegrationForSortedX(integralX, integralUpY_Qbsca, xmin, xmax, step);
		double integralUp_Qg = Integration.simpleIntegrationForSortedX(integralX, integralUpY_Qg, xmin, xmax, step);
		double integralDown = Integration.simpleIntegrationForSortedX(integralX, integralDownY, xmin, xmax, step);

		// Obtain k
		double k_Qabs = integralUp_Qabs / integralDown;
		double k_Qext = integralUp_Qext / integralDown;
		double k_Qsca = integralUp_Qsca / integralDown;
		double k_Qbsca = integralUp_Qbsca / integralDown;
		double k_Qg = integralUp_Qg / integralDown;

		return new double[] {k_Qabs, k_Qext, k_Qsca, k_Qbsca, k_Qg};
	}

	/**
	 * Obtains the current value of the absorption coefficient.
	 * @return Absorption coefficient, dimensionless.
	 */
	public double getQabs()
	{
		return this.qabs;
	}
	/**
	 * Obtains the current value of the extinction coefficient.
	 * @return Extinction coefficient, dimensionless.
	 */
	public double getQext()
	{
		return this.qext;
	}
	/**
	 * Obtains the current value of the scattering coefficient.
	 * @return Scattering coefficient, dimensionless.
	 */
	public double getQsca()
	{
		return this.qsca;
	}
	/**
	 * Obtains the current value of the backscattering coefficient.
	 * @return Backscattering coefficient, dimensionless.
	 */
	public double getQbsca()
	{
		return this.qback;
	}
	/**
	 * Obtains the current value of the mean average of cos(theta).
	 * @return Mean average of cos(theta), dimensionless.
	 */
	public double getQg()
	{
		return this.gsca;
	}
	/**
	 * Obtains incident E perpendicular to scattering plane. 2*({@linkplain MieTheory#nang}-1)
	 * compounds.
	 * @return E perpendicular to scattering plane.
	 */
	public Complex[] getEPerpendicular()
	{
		return this.cxs1;
	}
	/**
	 * Obtains incident E parallel to scattering plane. 2*({@linkplain MieTheory#nang}-1)
	 * compounds.
	 * @return E parallel to scattering plane.
	 */
	public Complex[] getEParallel()
	{
		return this.cxs2;
	}

	/**
	 * Obtains the albedo.
	 * @return Albedo.
	 */
	public double getAlbedo()
	{
	    return (1.0 - qabs / qext);
	}
	/**
	 * Obtains the albedo.
	 * @param qabs Absorption coefficient.
	 * @param qext Extinction coefficient.
	 * @return Albedo.
	 */
	public static double getAlbedo(double qabs, double qext)
	{
	    return (1.0 - qabs / qext);
	}
}
