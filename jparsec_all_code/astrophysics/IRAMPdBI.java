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

import jparsec.math.CGSConstant;
import jparsec.math.Constant;
import jparsec.util.JPARSECException;

/**
 * A class to perform some useful calculations on IRAM instruments.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class IRAMPdBI {

	/**
	 * K to Jy transform coefficient at 1.3 mm.
	 */
	public static double K_TO_Jy_1_3mm = 35.0;
	/**
	 * K to Jy transform coefficient at 2 mm.
	 */
	public static double K_TO_Jy_2mm = 29.0;
	/**
	 * K to Jy transform coefficient at 3 mm.
	 */
	public static double K_TO_Jy_3mm = 22.0;

	/**
	 * Efficiency factor at 1.3 mm.
	 */
	public static double eta_1_3mm = 0.8;
	/**
	 * Efficiency factor at 2 mm.
	 */
	public static double eta_2mm = 0.85;
	/**
	 * Efficiency factor at 3 mm.
	 */
	public static double eta_3mm = 0.9;

	/**
	 * System temperature below 110 GHz.
	 */
	public static double Tsys_below_110GHz = 100;
	/**
	 * System temperature at 115 GHz.
	 */
	public static double Tsys_at_115GHz = 170;
	/**
	 * System temperature at 150 GHz.
	 */
	public static double Tsys_at_150GHz = 150;
	/**
	 * System temperature at 230 GHz.
	 */
	public static double Tsys_at_230GHz = 200;

	/**
	 * Number of antennas.
	 */
	public static double Na = 6.0;

	/**
	 * 1.3 mm constant.
	 */
	public static final double WAVE_1_3MM = 1.3;
	/**
	 * 2 mm constant.
	 */
	public static final double WAVE_2MM = 2.0;
	/**
	 * 3 mm constant.
	 */
	public static final double WAVE_3MM = 3.0;

	/**
	 * Wavelength in mm.
	 */
	public double wave;
	/**
	 * Configuration to observe.
	 */
	public String configuration;
	/**
	 * Integration time in hours.
	 */
	public double time;
	/**
	 * Number of polarizations.
	 */
	public int Npol;
	/**
	 * Spectral Bandwidth in Hz.
	 */
	public double B;
	/**
	 * K to Jy transform coefficient.
	 */
	public double Jpk;
	/**
	 * System temperature in K.
	 */
	public double Tsys;
	/**
	 * Efficiency factor.
	 */
	public double eta;
	/**
	 * Number of configurations.
	 */
	public int Nc;

	/**
	 * Constructor.
	 * @param wave The wavelength.
	 * @param configuration The configuration.
	 * @param time integration time.
	 * @param Npol Number of polarizations.
	 * @param B Spectral bandwidth.
	 * @throws JPARSECException If an error occurs.
	 */
	public IRAMPdBI(double wave, String configuration, double time, int Npol, double B)
	throws JPARSECException {
		this.wave = wave;
		this.configuration = configuration;
		this.time = time;
		this.Npol = Npol;
		this.B = B;
		Nc = configuration.length();
		double nu =(Constant.SPEED_OF_LIGHT / (wave / 1000.0)) / 1.0E9;
		Tsys = IRAMPdBI.Tsys_at_115GHz;
		if (nu < 100.0) Tsys = IRAMPdBI.Tsys_below_110GHz;
		if (nu > 130.0) Tsys = IRAMPdBI.Tsys_at_150GHz;
		if (nu > 190.0) Tsys = IRAMPdBI.Tsys_at_230GHz;
		if (wave == IRAMPdBI.WAVE_1_3MM) {
			Jpk = IRAMPdBI.K_TO_Jy_1_3mm;
			eta = IRAMPdBI.eta_1_3mm;
		}
		if (wave == IRAMPdBI.WAVE_2MM) {
			Jpk = IRAMPdBI.K_TO_Jy_2mm;
			eta = IRAMPdBI.eta_2mm;
		}
		if (wave == IRAMPdBI.WAVE_3MM) {
			Jpk = IRAMPdBI.K_TO_Jy_3mm;
			eta = IRAMPdBI.eta_3mm;
		}
	}

	/**
	 * Returns the RMS in the PdBI.
	 * @return The rms value.
	 */
	public double getRMS_PdBI()
	{
		double sigma = Jpk * Tsys / (eta * Math.sqrt(Na * (Na - 1) * Nc * time * 3600.0 * B * Npol));
		return sigma;
	}

	/**
	 * Returns the factor to transform from K to Jy in an image.
	 * @param lambda Wavelength in mm.
	 * @param bmajor Major axis of the beam in arcseconds (major diameter of the beam).
	 * @param bminor Minor axis of the beam in arcseconds (minor diameter of the beam).
	 * @return K to Jy transform coefficient.
	 */
	public static double getFactorKToJy(double lambda, double bmajor, double bminor)
	{
		double nu = Constant.SPEED_OF_LIGHT / (lambda * 0.001);
		double factor = 1.0E-26 * CGSConstant.SPEED_OF_LIGHT * CGSConstant.SPEED_OF_LIGHT / 
				(2.0 * CGSConstant.BOLTZMANN_CONSTANT * nu * nu);
		factor = factor / ((bmajor * bminor) * Constant.ARCSEC_TO_RAD * Constant.ARCSEC_TO_RAD);
		return 0.001 / factor;
		//double factor = 265.0 * (bmajor * bminor / 3600.0) / (lambda * lambda);
		//return factor;
	}

	/**
	 * Set the constants for a specific year.
	 * @param year The year.
	 * @throws JPARSECException If the year is not supported.
	 */
	public static void setConstants(int year)
	throws JPARSECException {
		switch (year)
		{
		case 2008:
			K_TO_Jy_1_3mm = 35.0;
			K_TO_Jy_2mm = 29.0;
			K_TO_Jy_3mm = 22.0;
			eta_1_3mm = 0.6;
			eta_2mm = 0.8;
			eta_3mm = 0.9;
			Tsys_below_110GHz = 100;
			Tsys_at_115GHz = 180;
			Tsys_at_150GHz = 180;
			Tsys_at_230GHz = 250;
			Na = 5.0;
			// 2 GHz continuum, 40 KHz 2.5 MHz lines. T = 1.5*Ton. Sun avoidance 45 deg.
			break;
		case 2009:
			K_TO_Jy_1_3mm = 35.0;
			K_TO_Jy_2mm = 29.0;
			K_TO_Jy_3mm = 22.0;
			eta_1_3mm = 0.8;
			eta_2mm = 0.85;
			eta_3mm = 0.9;
			Tsys_below_110GHz = 100;
			Tsys_at_115GHz = 170;
			Tsys_at_150GHz = 150;
			Tsys_at_230GHz = 200;
			Na = 6.0;
			// 2 GHz continuum, 40 KHz 2.5 MHz lines. T = 1.6*Ton. 2 MHz for lines to 4 GHZ for continuum using WideX
			// Sun avoidance 35 deg
			break;
		default:
			throw new JPARSECException("unsupported year.");
		}
	}

	/**
	 * Returns the wavelength in mm.
	 * @param freq Frequency in GHz.
	 * @return Wavelength in mm.
	 */
	public static double getWavelength(double freq) {
		return Constant.SPEED_OF_LIGHT * 1.0E3 / (freq * 1.0E9);
	}
}
