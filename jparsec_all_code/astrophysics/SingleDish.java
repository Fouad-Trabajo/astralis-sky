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

import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.io.UnixSpecialCharacter;
import jparsec.math.CGSConstant;
import jparsec.math.Constant;
import jparsec.math.Interpolation;
import jparsec.util.JPARSECException;

/**
 * Provides useful calculations for single dish antennas.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class SingleDish {

	/**
	 * Antenna diameter in m.
	 */
	public double diameter;
	/**
	 * Aperture efficiency.
	 */
	public double aperEff;
	/**
	 * Source elevation in radians. Useless for a space telescope.
	 */
	public double elev;
	/**
	 * Atmospheric opacity at zenit. Set to zero for a space telescope.
	 */
	public double tau;
	/**
	 * Telescope name.
	 */
	public String name;
	/**
	 * Telescope ID constant.
	 */
	private TELESCOPE id;

	/**
	 * The set of telescopes.
	 */
	public enum TELESCOPE {
		/** ID constant for IRAM 30m. */
		IRAM30m,
		/** ID constant for JCMT. */
		JCMT,
		/** ID constant for HIFI. */
		HIFI,
		/** ID constant for ODIN. */
		ODIN,
		/** ID constant for APEX. */
		APEX
	};

	private String data[];

	/**
	 * Constructor for a given telescope.
	 * @param id The telescope ID constant.
	 * @throws JPARSECException If an error occurs.
	 */
	public SingleDish(TELESCOPE id)
	throws JPARSECException {
		this.id = id;
		this.readData();
		this.aperEff = 0.64;
		this.tau = 0.0;
		this.elev = 0.0;
	}

	/**
	 * Sets the telescope data as the content of
	 * a file in the CASSIS software format.
	 * @param data The data for the telescope.
	 */
	public void setData(String data[])
	{
		this.data = data;
	}

	private void readData()
	throws JPARSECException {
		String path = FileIO.DATA_TELESCOPES_DIRECTORY;
		String file = "";
		switch (this.id)
		{
		case APEX:
			file = "apex";
			break;
		case IRAM30m:
			file = "iram";
			break;
		case ODIN:
			file = "odin";
			break;
		case HIFI:
			file = "hifi";
			break;
		case JCMT:
			file = "jcmt";
			break;
		}
		this.name = file.toUpperCase();
		this.data = DataSet.arrayListToStringArray(ReadFile.readResource(path+file));
		this.diameter = Double.parseDouble(data[1]);
	}

	/**
	 * Returns the K to Jy transform coefficient in antenna temperature.
	 * Antenna diameter, source elevation, opacity, and aperture efficiency must be defined.
	 * @param freq Frequency in GHz.
	 * @return The K to Jy transform coefficient.
	 */
	public double getJyKInTa(double freq)
	{
		double A = 0.0;
		if (elev != 0.0) A = 1.0 / Math.sin(elev);
		double Tap = 1.0 * Math.exp(-tau*A);
		double Ageom = Math.PI * Math.pow(diameter * 100.0 / 2.0, 2.0);
		double Aeff = aperEff * Ageom;
		double JyKInTa = (2.0 * CGSConstant.BOLTZMANN_CONSTANT * Tap / Aeff) * Constant.ERG_S_CM2_HZ_TO_JY;

		return JyKInTa;
	}

	/**
	 * Returns the K to Jy transform coefficient in main beam temperature.
	 * @param freq Frequency in GHz.
	 * @return The K to Jy transform coefficient.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getJyKInTmb(double freq)
	throws JPARSECException {
		double JyKInTa = this.getJyKInTa(freq);
		double JyKInTmb = JyKInTa * this.getBeamEfficiency(freq);

		return JyKInTmb;
	}

	/**
	 * Returns the beam efficiency for a given frequency. This factor (beff)
	 * can be used to transform emission given in antenna temperature (Ta)
	 * to main beam temperature (Tmb) with the formula Tmb = Ta / beff.
	 * @param freq Frequency in GHz.
	 * @return The beam efficiency.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getBeamEfficiency(double freq)
	throws JPARSECException {
		int n = data.length - 3;
		double x_val[] = new double[n];
		double y_val[] = new double[n];
		for (int i=0; i<n; i++)
		{
			x_val[i] = 0.001 * Double.parseDouble(FileIO.getField(1, data[3+i], UnixSpecialCharacter.UNIX_SPECIAL_CHARACTER.TAB.value, true));
			y_val[i] = Double.parseDouble(FileIO.getField(2, data[3+i], UnixSpecialCharacter.UNIX_SPECIAL_CHARACTER.TAB.value, true));
		}
		Interpolation interp = new Interpolation(x_val, y_val, false);
		return interp.linearInterpolation(freq);
	}

	/**
	 * Returns main beam brightness temperature from brightness temperature.
	 * @param Tb Brightness temperature.
	 * @param fwhmSource Source solid angle.
	 * @param fwhmBeam Main beam solid angle.
	 * @return The main beam temperature.
	 */
	public static double getTmbFromTb(double Tb, double fwhmSource, double fwhmBeam)
	{
		return Tb * (fwhmSource * fwhmSource) / (fwhmSource * fwhmSource + fwhmBeam * fwhmBeam);
	}
	/**
	 * Returns brightness temperature from main beam brightness temperature.
	 * @param Tmb Main beam brightness temperature.
	 * @param fwhmSource Source solid angle.
	 * @param fwhmBeam Main beam solid angle.
	 * @return The brightness temperature.
	 */
	public static double getTbFromTmb(double Tmb, double fwhmSource, double fwhmBeam)
	{
		return Tmb * (fwhmSource*fwhmSource + fwhmBeam*fwhmBeam) / (fwhmSource * fwhmSource);
	}

	/**
	 * Returns the beam size.
	 * @param freq Frequency in GHz.
	 * @return Beam size in arcseconds.
	 */
	public double getBeam(double freq) {
		return 74000.0 / (freq * this.diameter);
	}
}
