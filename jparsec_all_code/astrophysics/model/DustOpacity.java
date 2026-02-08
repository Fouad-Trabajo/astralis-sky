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

import java.awt.Color;
import java.awt.Shape;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import jparsec.graph.ChartElement;
import jparsec.graph.ChartSeriesElement;
import jparsec.graph.CreateChart;
import jparsec.graph.DataSet;
import jparsec.graph.JPARSECStroke;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.io.Zip;
import jparsec.math.Constant;
import jparsec.math.Derivation;
import jparsec.math.Interpolation;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;

/**
 * A model of dust absorption based on the work by B. T. Draine. For reference see
 * Draine, ApJ 636, 1114-1120 (2006).
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class DustOpacity implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * The power law slope of the size distribution, coefficient p of Draine.
	 * Typical value is 3.5. dn/da = A * (a/a_max) ^ (-p), where n is the number
	 * of grains of a given size, A is a constant, a is the grain radius, and
	 * a_max is the maximum size of the grains in the distribution.
	 */
	public double sizeDistributionCoefficient;

	/**
	 * Maximum size for the grains in microns. For PAH the maximum allowed value
	 * is 0.001 microns, and the minimum is 0.4 nm. For the other grain types
	 * the limit is 10000 microns, and the minimum 0.1 microns. For PAH it is not
	 * allowed to use sizes greater than 0.001 microns.
	 */
	public double sizeMax;

	/**
	 * Selects the type of grains. ID Constants defined in this class.
	 */
	public int grainType;

	/**
	 * Sets the density of the grain in g/cm^3. The value can be set to a
	 * default one depending on the grain ID.
	 */
	public double grainDensity;

	/**
	 * Sets the abundance fraction for this grain.
	 */
	public double abundanceFraction;

	/**
	 * The set of opacities as a result of solving the model.
	 */
	private double dustModel[];

	/**
	 * Default constructor;
	 */
	public DustOpacity()
	{
		this.sizeDistributionCoefficient = this.sizeMax = 0.0;
	}

	/**
	 * Explicit constructor without density. Density is set to a
	 * default value.
	 *
	 * @param type Grain ID constant.
	 * @param p Size distribution, 2.5, 3, or 3.5.
	 * @param max Maximum size, 0.1, 1, ... 10000.
	 * @param abundance Abundance fraction, 0 to 1.
	 * @throws JPARSECException If the grain type does not exist.
	 */
	public DustOpacity(int type, double p, double max, double abundance) throws JPARSECException
	{
		this(type, p, max, 0.0, abundance);
	}

	/**
	 * Explicit constructor.
	 *
	 * @param type Grain ID constant.
	 * @param p Size distribution, 2.5, 3, or 3.5.
	 * @param max Maximum size, 0.1, 1, ... 10000.
	 * @param density Density.
	 * @param abundance Abundance fraction, 0 to 1.
	 */
	public DustOpacity(int type, double p, double max, double density, double abundance)
	{
		this.grainType = type;
		this.sizeDistributionCoefficient = p;
		this.sizeMax = max;
		this.grainDensity = density;
		this.abundanceFraction = abundance;
	}

	/**
	 * ID constant for astronomical silicate grains. Draine &amp; Lee 1984 and Laor &amp;
	 * Draine 1993.
	 */
	public static final int GRAIN_ASTRONOMICAL_SILICATE = 0;

	/**
	 * ID constant for smoothed UV astronomical silicate grains. Draine &amp; Lee
	 * 1984, Laor &amp; Draine 1993, and Weingartner &amp; Draine 2000.
	 */
	public static final int GRAIN_SMOOTHED_UV_ASTRONOMICAL_SILICATE = 1;

	/**
	 * ID constant for graphite grains. Draine &amp; Lee 1984 and Laor &amp; Draine
	 * 1993.
	 */
	public static final int GRAIN_GRAPHITE = 2;

	/**
	 * ID constant for water ice grains. Warren 1984, improved in 1995 by Bo-Cai Gao,
	 * Steve Warren, and Warren Wiscombe.
	 */
	public static final int GRAIN_WATER_ICE = 3;

	/**
	 * ID constant for silicon carbide grains. Laor &amp; Draine 1993.
	 */
	public static final int GRAIN_SILICON_CARBIDE = 4;

	/**
	 * ID constant for PAH neutral grains. Li &amp; Draine 2001.
	 */
	public static final int GRAIN_PAH_CARBONACEOUS_NEUTRAL = 5;

	/**
	 * ID constant for PAH ion grains. Li &amp; Draine 2001.
	 */
	public static final int GRAIN_PAH_CARBONACEOUS_ION = 6;

	/**
	 * ID constant for a mixture of other grains.
	 */
	public static final int GRAIN_MIXTURE = 7;

	/**
	 * The different grains.
	 */
	public static final String AVAILABLE_GRAINS[] = new String[] {"Astronomical silicate",
		"Smoothed UV astronomical silicate", "Graphite", "Water ice"}; //, "Silicon carbide"};
	/**
	 * The different available size distribution coefficient.
	 */
	public static final String AVAILABLE_P[] = new String[] {"2.5", "3.0", "3.5"};
	/**
	 * Available grain maximum sizes in microns.
	 */
	public static final String AVAILABLE_SIZES[] = new String[] {"0.1", "1", "10" ,"100", "1000", "10000"};

	/**
	 * Gets the refractive index from certain file name. This method is intended to be
	 * used with the files contained in the dust jar file.
	 *
	 * @param wavelength Wavelenth in microns, between 0.001 and 1000.
	 * @param fileName Name of the file to read, with extension but without path.
	 * @return The real and imaginary parts of the function, in microns.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[] getRefractiveIndex(double wavelength, String fileName) throws JPARSECException
	{
		String filePath = FileIO.DATA_DUST_DRAINE_DIRECTORY;
		if (fileName.equals("")) return new double[] {0.0, 0.0};
		filePath += fileName;

		ArrayList<String> v = ReadFile.readResource(filePath);
		ArrayList<Double> sizes = new ArrayList<Double>();
		ArrayList<Double> wavelengths = new ArrayList<Double>();
		ArrayList<ArrayList<Double>> realCoefficients = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> imgCoefficients = new ArrayList<ArrayList<Double>>();

		boolean wavelengthsRead = false;

		int iceWater = 0;
		if (this.grainType == DustOpacity.GRAIN_WATER_ICE) iceWater = -2;

		for (int i = 0; i < v.size() - 1; i++)
		{
			String line = v.get(i);
			String next = v.get(i + 1);

			if (next.toLowerCase().startsWith("w(micron)"))
			{
				i = i + 2;
				double grainSize = 0.0;
				try {
					grainSize = Double.parseDouble(FileIO.getField(1, line, " ", true));
				} catch (Exception e) {	}
				sizes.add(grainSize);

				ArrayList<Double> rcoefs = new ArrayList<Double>();
				ArrayList<Double> icoefs = new ArrayList<Double>();
				do
				{
					line = (v.get(i)).trim();
					if (!line.equals(""))
					{
						if (!wavelengthsRead)
						{
							double wave = Double.parseDouble(FileIO.getField(1, line, " ", true));
							wavelengths.add(wave);
						}
						rcoefs.add(Double.parseDouble(FileIO.getField(4+iceWater, line, " ", true)));
						icoefs.add(Double.parseDouble(FileIO.getField(5+iceWater, line, " ", true)));
					}
					i++;
				} while (!line.equals("") && i < v.size());
				wavelengthsRead = true;
				i--;
				realCoefficients.add(rcoefs);
				imgCoefficients.add(icoefs);
			}
		}

		double w[] = DataSet.arrayListToDoubleArray(wavelengths);
		//double s[] = DataSet.arrayListToDoubleArray(sizes);

		ArrayList<double[]> ordered = DataSet.sortInCrescent(w, DataSet.arrayListToDoubleArray(realCoefficients.get(0)), true);
		double wavesOrdered[] = ordered.get(0);
		double coefsOrdered[] = ordered.get(1);
		double real = 1.0 + Interpolation.linearInterpolation(wavesOrdered, coefsOrdered, wavelength, true);

		ordered = DataSet.sortInCrescent(w, DataSet.arrayListToDoubleArray(imgCoefficients.get(0)), true);
		wavesOrdered = ordered.get(0);
		coefsOrdered = ordered.get(1);
		double img = Interpolation.linearInterpolation(wavesOrdered, coefsOrdered, wavelength, true);

		return new double[] {real, img};
	}

	/**
	 * Gets the refractive index for certain grain. Currently unavailable for
	 * PAH and graphite.
	 *
	 * @param wavelength Wavelenth in microns, between 0.001 and 1000.
	 * @return The real and imaginary parts of the function, in microns.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[] getRefractiveIndex(double wavelength) throws JPARSECException
	{
		String filePath = FileIO.DATA_DUST_DRAINE_DIRECTORY;
		String fileName = this.getRefractiveIndexFileName();
		if (fileName.equals("")) return new double[] {0.0, 0.0};
		filePath += fileName;

		ArrayList<String> v = ReadFile.readResource(filePath);
		ArrayList<Double> sizes = new ArrayList<Double>();
		ArrayList<Double> wavelengths = new ArrayList<Double>();
		ArrayList<ArrayList<Double>> realCoefficients = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> imgCoefficients = new ArrayList<ArrayList<Double>>();

		boolean wavelengthsRead = false;

		int iceWater = 0;
		if (this.grainType == DustOpacity.GRAIN_WATER_ICE) iceWater = -2;

		for (int i = 0; i < v.size() - 1; i++)
		{
			String line = v.get(i);
			String next = v.get(i + 1);

			if (next.toLowerCase().startsWith("w(micron)"))
			{
				i = i + 2;
				double grainSize = Double.parseDouble(FileIO.getField(1, line, " ", true));
				sizes.add(grainSize);

				ArrayList<Double> rcoefs = new ArrayList<Double>();
				ArrayList<Double> icoefs = new ArrayList<Double>();
				do
				{
					line = (v.get(i)).trim();
					if (!line.equals(""))
					{
						if (!wavelengthsRead)
						{
							double wave = Double.parseDouble(FileIO.getField(1, line, " ", true));
							wavelengths.add(wave);
						}
						rcoefs.add(Double.parseDouble(FileIO.getField(4+iceWater, line, " ", true)));
						icoefs.add(Double.parseDouble(FileIO.getField(5+iceWater, line, " ", true)));
					}
					i++;
				} while (!line.equals("") && i < v.size());
				wavelengthsRead = true;
				i--;
				realCoefficients.add(rcoefs);
				imgCoefficients.add(icoefs);
			}
		}

		double w[] = DataSet.arrayListToDoubleArray(wavelengths);
		//double s[] = DataSet.arrayListToDoubleArray(sizes);

		ArrayList<double[]> ordered = DataSet.sortInCrescent(w, DataSet.arrayListToDoubleArray(realCoefficients.get(0)), true);
		double wavesOrdered[] = ordered.get(0);
		double coefsOrdered[] = ordered.get(1);
		double real = 1.0 + Interpolation.linearInterpolation(wavesOrdered, coefsOrdered, wavelength, true);

		ordered = DataSet.sortInCrescent(w, DataSet.arrayListToDoubleArray(imgCoefficients.get(0)), true);
		wavesOrdered = ordered.get(0);
		coefsOrdered = ordered.get(1);
		double img = Interpolation.linearInterpolation(wavesOrdered, coefsOrdered, wavelength, true);

		return new double[] {real, img};
	}

	private String getRefractiveIndexFileName() throws JPARSECException
	{
		int grain = this.grainType;
		String name = "";
		switch (grain)
		{
		case DustOpacity.GRAIN_ASTRONOMICAL_SILICATE:
			name = "eps_Sil";
			break;
		case DustOpacity.GRAIN_WATER_ICE:
			name = "Warren95WaterIce";
			break;
		case DustOpacity.GRAIN_SILICON_CARBIDE:
			name = "eps_SiC";
			break;
		case DustOpacity.GRAIN_SMOOTHED_UV_ASTRONOMICAL_SILICATE:
			name = "eps_suvSil";
			break;
		default:
			throw new JPARSECException("invalid grain.");
		}

		return name + ".txt";
	}

	/**
	 * Returns the name of the grain.
	 * @return Grain name.
	 * @throws JPARSECException If an error occurs.
	 */
	public String getDustName() throws JPARSECException
	{
		int grain = this.grainType;
		String name = "";
		switch (grain)
		{
		case DustOpacity.GRAIN_ASTRONOMICAL_SILICATE:
			name = Translate.translate(DustOpacity.AVAILABLE_GRAINS[grain]);
			break;
		case DustOpacity.GRAIN_GRAPHITE:
			name = Translate.translate(DustOpacity.AVAILABLE_GRAINS[grain]);
			break;
		case DustOpacity.GRAIN_WATER_ICE:
			name = Translate.translate(DustOpacity.AVAILABLE_GRAINS[grain]);
			break;
		case DustOpacity.GRAIN_PAH_CARBONACEOUS_ION:
			name = Translate.translate(407);
			break;
		case DustOpacity.GRAIN_PAH_CARBONACEOUS_NEUTRAL:
			name = Translate.translate(809);
			break;
		case DustOpacity.GRAIN_SILICON_CARBIDE:
			name = Translate.translate(DustOpacity.AVAILABLE_GRAINS[grain]);
			break;
		case DustOpacity.GRAIN_SMOOTHED_UV_ASTRONOMICAL_SILICATE:
			name = Translate.translate(DustOpacity.AVAILABLE_GRAINS[grain]);
			break;
		case DustOpacity.GRAIN_MIXTURE:
			name = Translate.translate(409);
			break;
		default:
			throw new JPARSECException("invalid grain.");
		}

		return name;
	}

	/**
	 * Obtains the default density for a given grain. For reference see for
	 * example Weingartner &amp; Draine 2000.
	 *
	 * @return Density in g/cm^3.
	 * @throws JPARSECException If the grain does not exist.
	 */
	public double getDustDefaultDensity() throws JPARSECException
	{
		int grain = this.grainType;
		double density = 0.0;
		switch (grain)
		{
		case DustOpacity.GRAIN_ASTRONOMICAL_SILICATE:
			density = 3.5;
			break;
		case DustOpacity.GRAIN_GRAPHITE:
			density = 2.24;
			break;
		case DustOpacity.GRAIN_WATER_ICE:
			density = 0.92;
			break;
		case DustOpacity.GRAIN_PAH_CARBONACEOUS_ION:
			density = 2.24;
			break;
		case DustOpacity.GRAIN_PAH_CARBONACEOUS_NEUTRAL:
			density = 2.24;
			break;
		case DustOpacity.GRAIN_SILICON_CARBIDE:
			density = 3.22;
			break;
		case DustOpacity.GRAIN_SMOOTHED_UV_ASTRONOMICAL_SILICATE:
			density = 3.5;
			break;
		default:
			throw new JPARSECException("invalid grain.");
		}

		return density;
	}

	/**
	 * Obtains the default sublimation temperature for a given grain.
	 *
	 * @return Sublimation temperature in K.
	 * @throws JPARSECException If the grain does not exist.
	 */
	public double getDustDefaultSublimationTemperature() throws JPARSECException
	{
		switch (this.grainType)
		{
		case DustOpacity.GRAIN_GRAPHITE:
			return 2500.0;
		case DustOpacity.GRAIN_WATER_ICE:
			return 150.0;
		case DustOpacity.GRAIN_ASTRONOMICAL_SILICATE:
		case DustOpacity.GRAIN_PAH_CARBONACEOUS_ION:
		case DustOpacity.GRAIN_PAH_CARBONACEOUS_NEUTRAL:
		case DustOpacity.GRAIN_SILICON_CARBIDE:
		case DustOpacity.GRAIN_SMOOTHED_UV_ASTRONOMICAL_SILICATE:
			return 1500.0;
		default:
			throw new JPARSECException("invalid grain.");
		}
	}

	/**
	 * Solves a dust model returning the set of opacities. This method is not
	 * recommended due to the high computer time that requires.
	 * @param dust Dust model consisting of a set of dust opacity intances.
	 * @param waves Set of wavelength in microns.<P>
	 * Due to computing cost, this method is not recommended. Use
	 * values from tabulated tables instead.
	 * @param np Number of points to use when integrating in size. Recommended
	 * values are from 500 to 5000 to get an accurate result.
	 * @return The dust model.
	 * @throws JPARSECException If an error occurs.
	 */
	public static DustOpacity[] solveDustModel(DustOpacity dust[], double waves[], int np)
	throws JPARSECException {

		if (dust.length == 1 && dust[0].grainType == DustOpacity.GRAIN_MIXTURE && dust[0].dustModel.length == waves.length) {
			return dust;
		}

		DustOpacity newModel[] = new DustOpacity[dust.length];
		for (int j=0; j<dust.length; j++)
		{
			newModel[j] = (DustOpacity) dust[j].clone();
			newModel[j].dustModel = new double[waves.length];
		}

		for (int i=0; i<waves.length; i++)
		{
			for (int j=0; j<dust.length; j++)
			{
				double k[] = newModel[j].getMieCoefficients(waves[i], np);
				newModel[j].dustModel[i] = k[MieTheory.INDEX_OF_ABSORPTION_COEFFICIENT];
				if (newModel[j].dustModel[i] < 0.0) newModel[j].dustModel[i] = 0.0;
			}
		}

		return newModel;
	}

	/**
	 * Obtains the cross sections for absorption, scattering, and extinction of dust
	 * for a given size distribution. An integration
	 * is performed from 5E-3 microns up to the given maximum dust radius. This
	 * method ussually requires a a lot of computing time.<P>
	 *
	 * For graphite the 1/3-2/3 approximation is used. See Draine and Malhotra 1993.
	 *
	 * @param wavelength Wavelenth in microns, between 0.001 and 1000.
	 * @param np Number of points to use when integrating in size. Recommended
	 * values are from 500 to 5000 to get an accurate result.
	 * @return Opacity, extinction, scattering, backscattering, mean average of
	 * cos(theta). Units are cm^2/g.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[] getMieCoefficients(double wavelength, int np) throws JPARSECException
	{
		double k[];
		if (this.grainType == DustOpacity.GRAIN_PAH_CARBONACEOUS_ION ||
				this.grainType == DustOpacity.GRAIN_PAH_CARBONACEOUS_NEUTRAL)
			throw new JPARSECException("cannot apply Mie theory to this grain.");

		if (this.grainType == DustOpacity.GRAIN_GRAPHITE)
		{
			String fileName = "callindex.out_CpaD03_0.01.txt";
			if (this.sizeMax > 0.1) fileName = "callindex.out_CpaD03_0.10.txt";
			double refractiveIndexPa[] = this.getRefractiveIndex(wavelength, fileName);

			double kPa[] = MieTheory.getMieCoefficients(this, wavelength, refractiveIndexPa, np);

			fileName = "callindex.out_CpeD03_0.01.txt";
			if (this.sizeMax > 0.1) fileName = "callindex.out_CpeD03_0.10.txt";
			double refractiveIndexPe[] = this.getRefractiveIndex(wavelength, fileName);

			double kPe[] = MieTheory.getMieCoefficients(this, wavelength, refractiveIndexPe, np);

			// Use 1/3 - 2/3 approximation. See Draine and Malhotra 1993.
			k = new double[kPa.length];
			for (int i=0; i<kPa.length; i++)
			{
				k[i] = (kPa[i] + 2.0 * kPe[i]) / 3.0;
			}
		} else {
			double refractiveIndex[] = this.getRefractiveIndex(wavelength);
			k = MieTheory.getMieCoefficients(this, wavelength, refractiveIndex, np);
		}
		return k;
	}

	/**
	 * Solves a dust model returning the set of opacities. This method uses
	 * tabulated values from tables.
	 * @param dust Dust model consisting of a set of dust opacity intances.
	 * @param waves Set of wavelength in microns.
	 * @param interpolate True to interpolate in 3d between the maximum grain size and the
	 * grain size distribution slope along all available range of values. If false and the input
	 * does not match any of the available values, an error will be produced.
	 * @return The dust model.
	 * @throws JPARSECException If an error occurs.
	 */
	public static DustOpacity[] solveDustModelUsingTabulatedValues(DustOpacity dust[], double waves[],
			boolean interpolate)
	throws JPARSECException {

		DustOpacity newModel[] = new DustOpacity[dust.length];
		for (int j=0; j<dust.length; j++)
		{
			newModel[j] = (DustOpacity) dust[j].clone();
			newModel[j].dustModel = new double[waves.length];
		}

		for (int i=0; i<waves.length; i++)
		{
			for (int j=0; j<dust.length; j++)
			{
				newModel[j].dustModel[i] = 0.0;
			}
			for (int j=0; j<dust.length; j++)
			{
				newModel[j].dustModel[i] += newModel[j].getAbsorptionCoefficientUsingTabulatedValues(waves[i], interpolate);
			}
		}

		return newModel;
	}

	/**
	 * Solves a dust model returning the set of opacities. This method uses
	 * tabulated values from tables, and the default set of wavelengths.
	 * @param interpolate True to interpolate in 3d between the maximum grain size and the
	 * grain size distribution slope along all available range of values. If false and the input
	 * does not match any of the available values, an error will be produced.
	 * @throws JPARSECException If an error occurs.
	 */
	public void solveDustModelUsingTabulatedValues(boolean interpolate)
	throws JPARSECException {
		double waves[] = this.getWavelengthsUsingTabulatedValues();
		this.dustModel = new double[waves.length];
		for (int i=0; i<waves.length; i++)
		{
			this.dustModel[i] = this.getAbsorptionCoefficientUsingTabulatedValues(waves[i], interpolate);
		}
	}

	/**
	 * Solves a dust model returning the set of opacities. This method uses
	 * tabulated values from tables (see {@linkplain #getWavelengthsUsingTabulatedValues()}).
	 * @param waves Set of wavelength in microns.
	 * @param interpolate True to interpolate in 3d between the maximum grain size and the
	 * grain size distribution slope along all available range of values. If false and the input
	 * does not match any of the available values, an error will be produced.
	 * @throws JPARSECException If an error occurs.
	 */
	public void solveDustModelUsingTabulatedValues(double waves[], boolean interpolate)
	throws JPARSECException {
		this.dustModel = new double[waves.length];
		for (int i=0; i<waves.length; i++)
		{
			this.dustModel[i] = this.getAbsorptionCoefficientUsingTabulatedValues(waves[i], interpolate);
		}
	}

	/**
	 * Returns the absorption coefficient for the current instance for a
	 * given wavelength index. A previous model should be defined and solved
	 * before calling this method.
	 * @param index Index of the array of wavelengths.
	 * @return Dust opacity.
	 */
	public double getDustModelAtIndex(int index)
	{
		return this.dustModel[index];
	}

	/**
	 * Returns the absorption coefficient for the current instance for all
	 * wavelength indexes. A previous model should be defined and solved
	 * before calling this method.
	 * @return Dust opacities.
	 */
	public double[] getDustModel()
	{
		return this.dustModel;
	}

	/**
	 * Solves a given dust model by setting the set of opacities. Adequate for
	 * tabulated values from tables.
	 * @param opacities Opacitiy array for a given set of wavelengths.
	 */
	public void solveDustModel(double opacities[])
	{
		this.dustModel = opacities;
	}

	/**
	 * Obtains the set of wavelengths used to create the tables of opacities and
	 * other tabulated values. 100 points in log scale between 0.1 and 1000 microns.
	 * @return Set of wavelengths in microns.
	 */
	public double[] getWavelengthsUsingTabulatedValues()
	{
		double minWave = 0.1, maxWave = 1000;
		int npLambda = 100;
		double wavelengths[] = DataSet.getSetOfValues(minWave, maxWave, npLambda, true);

		return wavelengths;
	}

	/**
	 * Gets the absorption coefficient from tabulated tables. The opacity
	 * is only available for {@linkplain DustOpacity#sizeMax} = 0.1, 1, 10, 100,
	 * 1000, and 10000 microns. Available values of {@linkplain DustOpacity#sizeDistributionCoefficient}
	 * are 2.5, 3.0, and 3.5. Wavelength should be between 1 and 1000 microns.<P>
	 *
	 * Exceptions: for astronomical silicate the grain maximum size 100000 microns is
	 * also available.
	 *
	 * @param wave Wavelength in microns.
	 * @param interpolate True to interpolate in 3d between the maximum grain size and the
	 * grain size distribution slope along all available range of values. If false and the input
	 * does not match any of the available values, an error will be produced.
	 * @return Absorption coefficient.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getAbsorptionCoefficientUsingTabulatedValues(double wave, boolean interpolate)
	throws JPARSECException {
		double availableSizes[] = new double[] {0.1, 1, 10, 100, 1000, 10000};
		double availableSizeDistributions[] = new double[] {3.5, 3.0, 2.5};

		String availableS = "", availableSD = "";
		boolean ok1 = false;
		for (int i=0; i<availableSizes.length; i++)
		{
			if (this.sizeMax == availableSizes[i]) ok1 = true;
			availableS += availableSizes[i];
			if (i<(availableSizes.length-1)) availableS += ", ";
		}
		if (!ok1 && this.sizeMax == 100000 && this.grainType == DustOpacity.GRAIN_ASTRONOMICAL_SILICATE)
			ok1 = true;


		if (!ok1 && !interpolate) throw new JPARSECException("grain maximum size not available. " +
				"Size max ("+this.sizeMax+") should be one of "+availableS+".");

		if (interpolate && (this.sizeMax < DataSet.getMinimumValue(availableSizes) ||
				this.sizeMax > DataSet.getMaximumValue(availableSizes)))
			throw new JPARSECException("grain maximum size not available. " +
				"Size max ("+this.sizeMax+") should be between "+DataSet.getMinimumValue(availableSizes)+
				" and "+DataSet.getMaximumValue(availableSizes)+".");

		boolean ok2 = false;
		for (int i=0; i<availableSizeDistributions.length; i++)
		{
			if (this.sizeDistributionCoefficient == availableSizeDistributions[i]) ok2 = true;
			availableSD += availableSizeDistributions[i];
			if (i<(availableSizeDistributions.length-1)) availableSD += ", ";
		}

		if (!ok2 && !interpolate) throw new JPARSECException("grain size distribution coefficient not available. " +
				"Size distribution coefficient " +
						"(" + this.sizeDistributionCoefficient+ ") should be one of "+availableSD+".");

		if (interpolate && (this.sizeDistributionCoefficient < DataSet.getMinimumValue(availableSizeDistributions) ||
				this.sizeDistributionCoefficient > DataSet.getMaximumValue(availableSizeDistributions)))
			throw new JPARSECException("grain size distribution coefficient not available. " +
					"Size distribution coefficient " +
							"(" + this.sizeDistributionCoefficient+ ") should be between "+DataSet.getMinimumValue(availableSizeDistributions)+"" +
									" and "+DataSet.getMaximumValue(availableSizeDistributions)+".");


		double k = 0.0;
		if (ok2)
		{
			String jarpath = FileIO.DATA_DUST_DIRECTORY + this.getTabulatedFileName();
			ArrayList<String> v = ReadFile.readResource(jarpath);
			ArrayList<Double> waves = new ArrayList<Double>();
			ArrayList<Double> coefs = new ArrayList<Double>();
			ArrayList<Double> coefs2 = new ArrayList<Double>();
			int field = 3 + (int) Math.log10(this.sizeMax);
			if (!ok1) {
				int field2 = field + 1;
				if (this.sizeMax < 1.0) {
					field2 = field - 1;
					int field3 = field2;
					field2 = field;
					field = field3;
				}
				for (int i=0; i<v.size(); i++)
				{
					String line = v.get(i);
					if (!line.startsWith("!")) {
						waves.add(Double.parseDouble(FileIO.getField(1, line, " ", true)));
						coefs.add(Double.parseDouble(FileIO.getField(field, line, " ", true)));
						coefs2.add(Math.pow(10.0, field-3.0));
						waves.add(Double.parseDouble(FileIO.getField(1, line, " ", true)));
						coefs.add(Double.parseDouble(FileIO.getField(field2, line, " ", true)));
						coefs2.add(Math.pow(10.0, field2-3.0));
					}
				}
				double w[] = DataSet.arrayListToDoubleArray(waves);
				double c[] = DataSet.arrayListToDoubleArray(coefs);
				double c2[] = DataSet.arrayListToDoubleArray(coefs2);

				Interpolation i = new Interpolation(w, c, c2, true);
				if (wave > 999) {
					wave = 999;
					JPARSECException.addWarning("3d interpolation for wavelength "+(int) wave+" microns will be done for 1 mm, since there's no available opacities beyond this wavelength.");
					wave = 999;
				}
				k = i.linearInterpolation3d(wave, this.sizeMax);
			} else {
				for (int i=0; i<v.size(); i++)
				{
					String line = v.get(i);
					if (!line.startsWith("!")) {
						waves.add(Double.parseDouble(FileIO.getField(1, line, " ", true)));
						coefs.add(Double.parseDouble(FileIO.getField(field, line, " ", true)));
					}
				}
				double w[] = DataSet.arrayListToDoubleArray(waves);
				double c[] = DataSet.arrayListToDoubleArray(coefs);

				Interpolation i = new Interpolation(w, c, true);
				k = i.linearInterpolationInLogScale(wave);
			}
		} else {
			if (!ok1) throw new JPARSECException("simultaneous interpolation in size distribution slope, grain size, and wavelength is not supported.");

			DustOpacity dust = (DustOpacity) this.clone();
			dust.sizeDistributionCoefficient = 3.0;
			String jarpath = FileIO.DATA_DUST_DIRECTORY + dust.getTabulatedFileName();
			ArrayList<String> v = ReadFile.readResource(jarpath);
			ArrayList<Double> waves = new ArrayList<Double>();
			ArrayList<Double> coefs = new ArrayList<Double>();
			ArrayList<Double> coefs2 = new ArrayList<Double>();
			int field = 3 + (int) Math.log10(this.sizeMax);
			for (int i=0; i<v.size(); i++)
			{
				String line = v.get(i);
				if (!line.startsWith("!")) {
					waves.add(Double.parseDouble(FileIO.getField(1, line, " ", true)));
					coefs.add(Double.parseDouble(FileIO.getField(field, line, " ", true)));
					coefs2.add(dust.sizeDistributionCoefficient);
				}
			}

			if (this.sizeDistributionCoefficient < 3.0) {
				dust.sizeDistributionCoefficient = 2.5;
			} else {
				dust.sizeDistributionCoefficient = 3.5;
			}
			jarpath = FileIO.DATA_DUST_DIRECTORY + dust.getTabulatedFileName();
			ArrayList<String> v2 = ReadFile.readResource(jarpath);
			for (int i=0; i<v2.size(); i++)
			{
				String line = v2.get(i);
				if (!line.startsWith("!")) {
					waves.add(Double.parseDouble(FileIO.getField(1, line, " ", true)));
					coefs.add(Double.parseDouble(FileIO.getField(field, line, " ", true)));
					coefs2.add(dust.sizeDistributionCoefficient);
				}
			}
			double w[] = DataSet.arrayListToDoubleArray(waves);
			double c[] = DataSet.arrayListToDoubleArray(coefs);
			double c2[] = DataSet.arrayListToDoubleArray(coefs2);

			Interpolation i = new Interpolation(w, c, c2, true);
			k = i.linearInterpolation3d(wave, this.sizeDistributionCoefficient);
		}

		return k;
	}

	/**
	 * Gets the extinction coefficient from tabulated tables. The extinction
	 * is only available for {@linkplain DustOpacity#sizeMax} = 0.1, 1, 10, 100,
	 * 1000, and 10000 microns. Available values of {@linkplain DustOpacity#sizeDistributionCoefficient}
	 * are 2.5, 3.0, and 3.5. Wavelength should be between 1 and 1000 microns.<P>
	 *
	 * Exceptions: for astronomical silicate the grain maximum size 100000 microns is
	 * also available.
	 *
	 * @param wave Wavelength in microns.
	 * @param interpolate True to interpolate in 3d between the maximum grain size and the
	 * grain size distribution slope along all available range of values. If false and the input
	 * does not match any of the available values, an error will be produced.
	 * @return Extinction coefficient.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getExtinctionCoefficientUsingTabulatedValues(double wave, boolean interpolate)
	throws JPARSECException {
		double availableSizes[] = new double[] {0.1, 1, 10, 100, 1000, 10000};
		double availableSizeDistributions[] = new double[] {3.5, 3.0, 2.5};

		String availableS = "", availableSD = "";
		boolean ok = false;
		for (int i=0; i<availableSizes.length; i++)
		{
			if (this.sizeMax == availableSizes[i]) ok = true;
			availableS += availableSizes[i];
			if (i<(availableSizes.length-1)) availableS += ", ";
		}
		if (!ok && this.sizeMax == 100000 && this.grainType == DustOpacity.GRAIN_ASTRONOMICAL_SILICATE)
			ok = true;


		if (!ok && !interpolate) throw new JPARSECException("grain maximum size not available. " +
				"Size max ("+this.sizeMax+") should be one of "+availableS+".");

		if (interpolate && (this.sizeMax < DataSet.getMinimumValue(availableSizes) ||
				this.sizeMax > DataSet.getMaximumValue(availableSizes)))
			throw new JPARSECException("grain maximum size not available. " +
				"Size max ("+this.sizeMax+") should be between "+DataSet.getMinimumValue(availableSizes)+
				" and "+DataSet.getMaximumValue(availableSizes)+".");

		ok = false;
		for (int i=0; i<availableSizeDistributions.length; i++)
		{
			if (this.sizeDistributionCoefficient == availableSizeDistributions[i]) ok = true;
			availableSD += availableSizeDistributions[i];
			if (i<(availableSizeDistributions.length-1)) availableSD += ", ";
		}

		if (!ok && !interpolate) throw new JPARSECException("grain size distribution coefficient not available. " +
				"Size distribution coefficient " +
						"(" + this.sizeDistributionCoefficient+ ") should be one of "+availableSD+".");

		if (interpolate && (this.sizeDistributionCoefficient < DataSet.getMinimumValue(availableSizeDistributions) ||
				this.sizeDistributionCoefficient > DataSet.getMaximumValue(availableSizeDistributions)))
			throw new JPARSECException("grain size distribution coefficient not available. " +
					"Size distribution coefficient " +
							"(" + this.sizeDistributionCoefficient+ ") should be between "+DataSet.getMinimumValue(availableSizeDistributions)+"" +
									" and "+DataSet.getMaximumValue(availableSizeDistributions)+".");


		String fn = DataSet.replaceAll(this.getTabulatedFileName(), "abs.txt", "ext.txt", false);
		String jarpath = FileIO.DATA_DUST_DIRECTORY + fn;
		ArrayList<String> v = ReadFile.readResource(jarpath);
		ArrayList<Double> waves = new ArrayList<Double>();
		ArrayList<Double> coefs = new ArrayList<Double>();
		int field = 3 + (int) Math.log10(this.sizeMax);
		for (int i=0; i<v.size(); i++)
		{
			String line = v.get(i);
			if (!line.startsWith("!")) {
				waves.add(Double.parseDouble(FileIO.getField(1, line, " ", true)));
				coefs.add(Double.parseDouble(FileIO.getField(field, line, " ", true)));
			}
		}
		double w[] = DataSet.arrayListToDoubleArray(waves);
		double c[] = DataSet.arrayListToDoubleArray(coefs);

		Interpolation i = new Interpolation(w, c, true);
		double k = i.linearInterpolationInLogScale(wave);
		return k;

	}

	public String getTabulatedFileName() throws JPARSECException
	{
		int grain = this.grainType;
		String name = "";
		switch (grain)
		{
		case DustOpacity.GRAIN_SMOOTHED_UV_ASTRONOMICAL_SILICATE:
			name = "SilicateUVsmooth"+this.sizeDistributionCoefficient;
			break;
		case DustOpacity.GRAIN_ASTRONOMICAL_SILICATE:
			name = "Silicate"+this.sizeDistributionCoefficient;
			break;
		case DustOpacity.GRAIN_GRAPHITE:
			name = "Graphite"+this.sizeDistributionCoefficient;
			break;
		case DustOpacity.GRAIN_WATER_ICE:
			name = "Ice"+this.sizeDistributionCoefficient;
			break;
		case DustOpacity.GRAIN_SILICON_CARBIDE:
			name = "Carbide"+this.sizeDistributionCoefficient;
			break;
		default:
			throw new JPARSECException("invalid grain, table unavailable.");
		}

		return name + "abs.txt";
	}

	/**
	 * Gets the dust spectral index. The opacity
	 * is only available for {@linkplain DustOpacity#sizeMax} = 1, 10, 100,
	 * 1000, and 10000 microns. Available values of {@linkplain DustOpacity#sizeDistributionCoefficient}
	 * are 2.5, 3.0, and 3.5. Wavelength should be between 1 and 1000 microns.<P>
	 *
	 * Exceptions: for astronomical silicate the grain maximum size 100000 microns is
	 * also available.
	 *
	 * @param wave Wavelength in microns.
	 * @param interpolate True to interpolate in 3d between the maximum grain size and the
	 * grain size distribution slope along all available range of values. If false and the input
	 * does not match any of the available values, an error will be produced.
	 * @return Dust spectral index.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getSpectralIndexUsingTabulatedValues(double wave, boolean interpolate)
	throws JPARSECException {
		double wmin = wave - 1;
		double wmax = wave + 1;
		if (wmin < 1) {
			wmin = 1;
			wmax = 1.1;
		}
		if (wmax > 1000) {
			wmin = 999.9;
			wmax = 1000;
		}

		double kmin = this.getAbsorptionCoefficientUsingTabulatedValues(wmin, interpolate);
		double kmax = this.getAbsorptionCoefficientUsingTabulatedValues(wmax, interpolate);

		double nulogMin = Math.log(Constant.SPEED_OF_LIGHT * 1E6 / wmin);
		double nulogMax = Math.log(Constant.SPEED_OF_LIGHT * 1E6 / wmax);

		return (Math.log(kmax) - Math.log(kmin)) / (nulogMax - nulogMin);
	}

	/**
	 * Gets the dust opacity and spectral index. Opacity is calculated for
	 * both wavelengths provided, while the beta between both wavelengths.
	 *
	 * @param wmin Wavelength in microns.
	 * @param wmax Wavelength in microns.
	 * @param np Number of sizes in the integration. From 500 to 5000
	 * recommended.
	 * @return Dust opacity for the first and second wavelengths, and
	 * spectral index in the third component.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[] getOpacityAndSpectralIndex(double wmin, double wmax, int np)
	throws JPARSECException {

		DustOpacity d[] = DustOpacity.solveDustModel(new DustOpacity[] {(DustOpacity) this.clone()}, new double[] {wmin,
				wmax}, np);

		double kmin = d[0].getDustModelAtIndex(0); // .getAbsoptionCoefficientUsingTabulatedValues(wmin);
		double kmax = d[0].getDustModelAtIndex(1); //this.getAbsoptionCoefficientUsingTabulatedValues(wmax);

		double nulogMin = Math.log(Constant.SPEED_OF_LIGHT * 1E6 / wmin);
		double nulogMax = Math.log(Constant.SPEED_OF_LIGHT * 1E6 / wmax);

		return new double[] {
				d[0].getDustModelAtIndex(0),
				d[0].getDustModelAtIndex(1),
				(Math.log(kmax) - Math.log(kmin)) / (nulogMax - nulogMin)
				};
	}

	/**
	 * Returns a chart with the opacity as function of wavelength. Also
	 * creates a chart with beta as function of wavelength. Opacities are
	 * taken from tabulated tables, so they are only available for certain
	 * set of input parameters.
	 *
	 * @param minWave Minimum wavelength for the chart in microns. Equal or
	 *        greater than 1 microns.
	 * @param maxWave Maximum wavelength for the chart in microns. Equal or
	 *        lower than 1000 microns.
	 * @param interpolate True to interpolate in 3d between the maximum grain size and the
	 * grain size distribution slope along all available range of values. If false and the input
	 * does not match any of the available values, an error will be produced.
	 * @return The opacity and beta charts.
	 * @throws JPARSECException If an error occurs.
	 */
	public CreateChart[] getOpacityAndBetaCharts(double minWave, double maxWave,
			boolean interpolate) throws JPARSECException
	{
		double waves[] = DataSet.getSetOfValues(minWave, maxWave, 100, true);
		double opacities[] = new double[waves.length];

		for (int i = 0; i < waves.length; i++)
		{
			opacities[i] = getAbsorptionCoefficientUsingTabulatedValues(waves[i], interpolate);
		}

		String name = getDustName();

		CreateChart ch = CreateChart.createSimpleChart(waves, opacities,
				Translate.translate(447)+" " + name + " ("+Translate.translate(448)+" "+this.sizeMax+" @mum)", Translate.translate(419)+" (@mum)", Translate.translate(339)+" (cm^{2} g^{-1})",
				Translate.translate(447)+" " + name, true);

		// Beta chart
		int interpOrder = 3;
		double betas[] = new double[waves.length - 2 * interpOrder];
		double betawaves[] = new double[waves.length - 2 * interpOrder];
		double opacitieslog[] = new double[opacities.length];
		double nulog[] = new double[opacities.length];
		for (int i = 0; i < waves.length; i++)
		{
			opacitieslog[i] = Math.log(opacities[i]);
			nulog[i] = Math.log(Constant.SPEED_OF_LIGHT * 1E6 / waves[i]);
		}
		ArrayList<double[]> ordered = DataSet.sortInCrescent(nulog, opacitieslog, true);
		nulog = ordered.get(0);
		opacitieslog = ordered.get(1);
		Derivation der = new Derivation(nulog, opacitieslog);
		for (int i = interpOrder; i < waves.length - interpOrder; i++)
		{
			betas[i - interpOrder] = der.Lagrange(nulog[i], interpOrder);
			betawaves[i - interpOrder] = 1E6 * Constant.SPEED_OF_LIGHT / Math.exp(nulog[i]);
		}

		CreateChart ch2 = CreateChart.createSimpleChart(betawaves, betas,
				Translate.translate(449)+" " + name + " ("+Translate.translate(448)+" "+this.sizeMax+" @mum)", Translate.translate(419)+" (@mum)", Translate.translate(450), name + " "+Translate.translate(450).toLowerCase(), true);
		return new CreateChart[] {ch, ch2};
	}

	/**
	 * Returns a chart with the opacity as function of wavelength for 2 grains. Also
	 * creates a chart with beta as function of wavelength. Opacities are
	 * taken from tabulated tables, so they are only available for certain
	 * set of input parameters.
	 *
	 * @param dust1 First dust model.
	 * @param dust2 Second dust model.
	 * @param minWave Minimum wavelength for the chart in microns. Equal or
	 *        greater than 1 microns.
	 * @param maxWave Maximum wavelength for the chart in microns. Equal or
	 *        lower than 1000 microns.
	 * @param showMixture True to show also the results for the mixture of grains.
	 * @param interpolate True to interpolate in 3d between the maximum grain size and the
	 * grain size distribution slope along all available range of values. If false and the input
	 * does not match any of the available values, an error will be produced.
	 * @return The opacity and beta charts.
	 * @throws JPARSECException If an error occurs.
	 */
	public static CreateChart[] getOpacityAndBetaCharts(DustOpacity dust1,
			DustOpacity dust2, double minWave, double maxWave, boolean showMixture,
			boolean interpolate) throws JPARSECException
	{
		double waves[] = DataSet.getSetOfValues(minWave, maxWave, 100, true);
		double opacities1[] = new double[waves.length];
		double opacities2[] = new double[waves.length];

		for (int i = 0; i < waves.length; i++)
		{
			opacities1[i] = dust1.getAbsorptionCoefficientUsingTabulatedValues(waves[i], interpolate);
			opacities2[i] = dust2.getAbsorptionCoefficientUsingTabulatedValues(waves[i], interpolate);
		}

		double total[] = new double[opacities1.length];
		dust1.dustModel = opacities1;
		dust2.dustModel = opacities2;
		for (int i=0; i<total.length; i++)
		{
			total[i] = DustOpacity.getTotalOpacity(new DustOpacity[] {dust1, dust2}, i, 0.0);
		}

		String name1 = dust1.getDustName();
		String name2 = dust2.getDustName();


		CreateChart ch = null;
		if (showMixture) {
			ch = DustOpacity.getChartWithThreeDataSets(waves, opacities1, waves,
				opacities2, waves, total, name1, name2, Translate.translate(400), Translate.translate(451)+" "+dust1.sizeMax+" @mum",
				Translate.translate(419)+" (@mum)", Translate.translate(339)+" (cm^{2} g^{-1})", true, true, 600, 600);
		} else {
			ch = DustOpacity.getChartWithTwoDataSets(waves, opacities1, waves,
					opacities2, name1, name2, Translate.translate(451)+" "+dust1.sizeMax+" @mum",
					Translate.translate(419)+" (@mum)", Translate.translate(339)+" (cm^{2} g^{-1})", true, true, 600, 600);
		}

		// Beta chart
		int interpOrder = 3;
		double betas1[] = new double[waves.length - 2 * interpOrder];
		double betas2[] = new double[waves.length - 2 * interpOrder];
		double betasT[] = new double[waves.length - 2 * interpOrder];
		double betawaves[] = new double[waves.length - 2 * interpOrder];
		double opacitieslog1[] = new double[opacities1.length];
		double opacitieslog2[] = new double[opacities2.length];
		double opacitieslogT[] = new double[opacities2.length];
		double nulog[] = new double[opacities1.length];
		for (int i = 0; i < waves.length; i++)
		{
			opacitieslog1[i] = Math.log(opacities1[i]);
			opacitieslog2[i] = Math.log(opacities2[i]);
			opacitieslogT[i] = Math.log(total[i]);
			nulog[i] = Math.log(Constant.SPEED_OF_LIGHT * 1E6 / waves[i]);
		}
		ArrayList<double[]> ordered1 = DataSet.sortInCrescent(nulog, opacitieslog1, true);
		ArrayList<double[]> ordered2 = DataSet.sortInCrescent(nulog, opacitieslog2, true);
		ArrayList<double[]> orderedT = DataSet.sortInCrescent(nulog, opacitieslogT, true);
		nulog = ordered1.get(0);
		opacitieslog1 = ordered1.get(1);
		opacitieslog2 = ordered2.get(1);
		opacitieslogT = orderedT.get(1);
		Derivation der1 = new Derivation(nulog, opacitieslog1);
		Derivation der2 = new Derivation(nulog, opacitieslog2);
		Derivation derT = new Derivation(nulog, opacitieslogT);
		for (int i = interpOrder; i < waves.length - interpOrder; i++)
		{
			betas1[i - interpOrder] = der1.Lagrange(nulog[i], interpOrder);
			betas2[i - interpOrder] = der2.Lagrange(nulog[i], interpOrder);
			betasT[i - interpOrder] = derT.Lagrange(nulog[i], interpOrder);
			betawaves[i - interpOrder] = 1E6 * Constant.SPEED_OF_LIGHT / Math.exp(nulog[i]);
		}

		CreateChart ch2 = null;
		if (showMixture) {
			ch2 = DustOpacity.getChartWithThreeDataSets(betawaves, betas1, betawaves,
				betas2, betawaves, betasT, name1, name2, Translate.translate(400), Translate.translate(452)+" "+dust1.sizeMax+" @mum",
				Translate.translate(419)+" (@mum)", Translate.translate(450), true, true, 600, 600);
		} else {
			ch2 = DustOpacity.getChartWithTwoDataSets(betawaves, betas1, betawaves,
					betas2, name1, name2, Translate.translate(452)+" "+dust1.sizeMax+" @mum",
					Translate.translate(419)+" (@mum)", Translate.translate(450), true, true, 600, 600);
		}

		return new CreateChart[] {ch, ch2};
	}

	private static CreateChart getChartWithTwoDataSets(double x1[], double y1[], double x2[], double y2[],
			String label1, String label2, String title, String xlabel, String ylabel, boolean
			logScaleX, boolean logScaleY, int width, int height)
	throws JPARSECException {
		Shape shapeI = ChartSeriesElement.SHAPE_DIAMOND, shapeS = ChartSeriesElement.SHAPE_CIRCLE;

		ChartSeriesElement seriesI = new ChartSeriesElement(x1, y1, null, null, label1, true,
				Color.BLUE, shapeI, ChartSeriesElement.REGRESSION.NONE);
		seriesI.showLines = false;
		seriesI.showShapes = true;
		ChartSeriesElement seriesS = new ChartSeriesElement(x2, y2, null, null, label2, true,
				Color.RED, shapeS, ChartSeriesElement.REGRESSION.NONE);
		seriesS.showLines = false;
		seriesS.showShapes = true;

		ChartSeriesElement series[] = new ChartSeriesElement[] {seriesI, seriesS};

		ChartElement chart = new ChartElement(series, ChartElement.TYPE.XY_CHART, ChartElement.SUBTYPE.XY_SCATTER,
				title, xlabel, ylabel, false, width, height);
		chart.showErrorBars = false;
		chart.xTickLabels = ChartElement.TICK_LABELS.LOGARITHM_VALUES;
		chart.yTickLabels = ChartElement.TICK_LABELS.LOGARITHM_VALUES;

		chart.xAxisInLogScale = logScaleX;
		chart.yAxisInLogScale = logScaleY;

		return new CreateChart(chart);
	}

	private static CreateChart getChartWithThreeDataSets(double x1[], double y1[], double x2[], double y2[],
			double x3[], double y3[],
			String label1, String label2, String label3, String title, String xlabel, String ylabel, boolean
			logScaleX, boolean logScaleY, int width, int height)
	throws JPARSECException {
		Shape shapeI = ChartSeriesElement.SHAPE_DIAMOND, shapeS = ChartSeriesElement.SHAPE_CIRCLE,
			shapeT = ChartSeriesElement.SHAPE_SQUARE;

		ChartSeriesElement seriesI = new ChartSeriesElement(x1, y1, null, null, label1, true,
				Color.BLUE, shapeI, ChartSeriesElement.REGRESSION.NONE);
		seriesI.showLines = false;
		seriesI.showShapes = true;
		ChartSeriesElement seriesS = new ChartSeriesElement(x2, y2, null, null, label2, true,
				Color.RED, shapeS, ChartSeriesElement.REGRESSION.NONE);
		seriesS.showLines = false;
		seriesS.showShapes = true;

		ChartSeriesElement seriesT = new ChartSeriesElement(x3, y3, null, null, label3, true,
				Color.BLACK, shapeT, ChartSeriesElement.REGRESSION.NONE);
		seriesS.showLines = false;
		seriesS.showShapes = true;

		ChartSeriesElement series[] = new ChartSeriesElement[] {seriesI, seriesS, seriesT};

		ChartElement chart = new ChartElement(series, ChartElement.TYPE.XY_CHART, ChartElement.SUBTYPE.XY_SCATTER,
				title, xlabel, ylabel, false, width, height);
		chart.showErrorBars = false;
		chart.xTickLabels = ChartElement.TICK_LABELS.LOGARITHM_VALUES;
		chart.yTickLabels = ChartElement.TICK_LABELS.LOGARITHM_VALUES;

		chart.xAxisInLogScale = logScaleX;
		chart.yAxisInLogScale = logScaleY;

		return new CreateChart(chart);
	}

	/**
	 * Obtains the total opacity of a given mixture of grains.
	 * @param dust Array of dust components.
	 * @param index Index of the desired wavelength.
	 * @param T Temperature of the region in K. If the temperature is
	 * greater than the dust sublimation temperature (for certain grain type),
	 * that grain will not be considered in the calculations.
	 * @return The opacity of the mixture, in cm^2/g.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double getTotalOpacity(DustOpacity dust[], int index, double T)
	throws JPARSECException {
		double k = 0.0, sum = 0.0;
		for (int i=0; i<dust.length; i++)
		{
			if (T <= dust[i].getDustDefaultSublimationTemperature())
			{
				k += dust[i].dustModel[index] * dust[i].abundanceFraction;
			}
			sum += dust[i].abundanceFraction;
		}
		k = k / sum;
		return k;
	}

	/**
	 * Transforms Hz to microns.
	 *
	 * @param nu Frequency in Hz.
	 * @return Wavelength in microns.
	 */
	public double Hz2micron(double nu)
	{
		double wave = 1E6 * Constant.SPEED_OF_LIGHT / nu;
		return wave;
	}


	/**
	 * To clone the object.
	 */
	@Override
	public Object clone()
	{
		DustOpacity d = new DustOpacity();
		d.abundanceFraction = this.abundanceFraction;
		if (dustModel != null) d.dustModel = this.dustModel.clone();
		d.grainDensity = this.grainDensity;
		d.grainType = this.grainType;
		d.sizeDistributionCoefficient = this.sizeDistributionCoefficient;
		d.sizeMax = this.sizeMax;
		return d;
	}
	/**
	 * Checks if two instances contains the same data.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof DustOpacity)) return false;

		DustOpacity that = (DustOpacity) o;

		if (Double.compare(that.sizeDistributionCoefficient, sizeDistributionCoefficient) != 0) return false;
		if (Double.compare(that.sizeMax, sizeMax) != 0) return false;
		if (grainType != that.grainType) return false;
		if (Double.compare(that.grainDensity, grainDensity) != 0) return false;
		if (Double.compare(that.abundanceFraction, abundanceFraction) != 0) return false;

		return Arrays.equals(dustModel, that.dustModel);
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		temp = Double.doubleToLongBits(sizeDistributionCoefficient);
		result = (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(sizeMax);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + grainType;
		temp = Double.doubleToLongBits(grainDensity);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(abundanceFraction);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (dustModel != null ? Arrays.hashCode(dustModel) : 0);
		return result;
	}

	/**
	 * Transforms a set of {@linkplain DustOpacity} objects into a sole
	 * object, equivalent to the corresponding mixture of grains.<P>
	 *
	 * Grain maximum size and size distribution, as well as the size of the
	 * array of opacities, are taken from the first element in the mixture.
	 * All the elements should have the same grain maximum size, size
	 * distribution, and opacities for the same array of wavelengths. Otherwise
	 * the mixture will not be suitable for calculations.
	 *
	 * @param dustModel A set of models for a given dust mixture.
	 * @return An unique model for the mixture.
	 * @throws JPARSECException If an error occurs.
	 */
	public static DustOpacity tranformDustModelToSingleGrain(DustOpacity[] dustModel)
	throws JPARSECException {
		DustOpacity dust = new DustOpacity();
		dust.grainType = DustOpacity.GRAIN_MIXTURE;

		int l = dustModel.length;
		double density = 0.0, abundance = 0.0;
		for (int i=0; i<l; i++)
		{
			density += dustModel[i].grainDensity * dustModel[i].abundanceFraction;
			abundance += dustModel[i].abundanceFraction;
		}
		dust.grainDensity = density / abundance;
		dust.abundanceFraction = 1.0;
		dust.sizeMax = dustModel[0].sizeMax;
		dust.sizeDistributionCoefficient = dustModel[0].sizeDistributionCoefficient;

		dust.dustModel = new double[dustModel[0].dustModel.length];
		l = dust.dustModel.length;
		for (int i=0; i<l; i++)
		{
			dust.dustModel[i] = DustOpacity.getTotalOpacity(dustModel, i, 0.0);
		}

		return dust;
	}

	/**
	 * Obtains the opacity and spectral index charts for a given grain and
	 * wavelength. The charts are created for different grain size
	 * distribution coefficients and different values of the maximum
	 * grain size. This method should be used with caution since it
	 * could spend a lot of computing time.
	 * <P>
	 * The maximum wavelength is used for the opacity chart (unless false is given
	 * in the max param), while the
	 * spectral index is calculated between the minimum and the maximum
	 * provided wavelengths.
	 *
	 * @param grain The grain ID constant.
	 * @param npoints The number of points between 1 and 1000 microns to
	 * use in the chart.
	 * @param np The number of sizes to integrate.
	 * @param wmin The minimum wavelength for the charts.
	 * @param wmax The maximum wavelength for the charts.
	 * @param max True to use the maximum wavelength for the opacity, false for
	 * the minimum wavelength.
	 * @return The charts for the opacity and dust spectral index.
	 * @throws JPARSECException If an error occurs.
	 */
	public static CreateChart[] getDustModelChart(int grain, int npoints, int np, double wmin,
			double wmax, boolean max)
	throws JPARSECException {
		double sizes[] = DataSet.getSetOfValues(1, 10000, npoints, true);

		Color color[] = new Color[] {Color.RED, Color.GREEN, Color.BLUE};
		JPARSECStroke stroke[] = new JPARSECStroke[] {
				JPARSECStroke.STROKE_LINES_LARGE,
				JPARSECStroke.STROKE_LINES_MEDIUM,
				JPARSECStroke.STROKE_LINES_SHORT,
				};
		ChartSeriesElement series_k[] = new ChartSeriesElement[3];
		ChartSeriesElement series_beta[] = new ChartSeriesElement[3];
		int serie = -1;
		String grainName = (new DustOpacity(grain, 2.5, 1, 1)).getDustName();
		double x[] = new double[npoints];
		double y_k[] = new double[npoints];
		double y_beta[] = new double[npoints];

		for (double p=2.5; p<=3.5; p = p + 0.5)
		{
			for (int size = 0; size < sizes.length; size ++)
			{
				DustOpacity d = new DustOpacity(grain, p, sizes[size], 1.0);
				double val[] = d.getOpacityAndSpectralIndex(wmin, wmax, np);
				x[size] = sizes[size];
				y_k[size] = val[1];
				if (!max) y_k[size] = val[0];
				y_beta[size] = val[2];
			}

			serie ++;
			ChartSeriesElement chartSeries_k = new ChartSeriesElement(
					x.clone(),
					y_k.clone(), null, null,
					"p = "+p, true, color[serie], ChartSeriesElement.SHAPE_CIRCLE,
					ChartSeriesElement.REGRESSION.NONE);
			chartSeries_k.showLines = true;
			chartSeries_k.showShapes = false;
			chartSeries_k.stroke = stroke[serie];
			series_k[serie] = chartSeries_k.clone();
			ChartSeriesElement chartSeries_beta = new ChartSeriesElement(
					x.clone(),
					y_beta.clone(), null, null,
					"p = "+p, true, color[serie], ChartSeriesElement.SHAPE_CIRCLE,
					ChartSeriesElement.REGRESSION.NONE);
			chartSeries_beta.showLines = true;
			chartSeries_beta.showShapes = false;
			chartSeries_beta.stroke = stroke[serie];
			series_beta[serie] = chartSeries_beta.clone();
		}

		ChartElement chart_k = new ChartElement(series_k, ChartElement.TYPE.XY_CHART, ChartElement.SUBTYPE.XY_SCATTER,
				Translate.translate(447)+" "+grainName.toLowerCase(), "a_{max} (@mum)",
				"k_{@nu} (cm^{2} g^{-1})", false, 500, 550);
		chart_k.xAxisInLogScale = true;
		ChartElement chart_beta = new ChartElement(series_beta, ChartElement.TYPE.XY_CHART, ChartElement.SUBTYPE.XY_SCATTER,
				Translate.translate(449)+" "+grainName.toLowerCase(), "a_{max} (@mum)",
				"@beta", false, 500, 550);
		chart_beta.xAxisInLogScale = true;
		CreateChart ch[] = new CreateChart[] {new CreateChart(chart_k), new CreateChart(chart_beta)};
		return ch;
	}

	/**
	 * Obtains the opacity and spectral index charts for a given grain mixture and
	 * wavelength. The charts are created for different grain size
	 * distribution coefficients and different values of the maximum
	 * grain size. This method should be used with caution since it
	 * could spend a lot of computing time.
	 * <P>
	 * The maximum wavelength is used for the opacity chart (unless false is given
	 * in the max param, in that case the minimum wavelength), while the
	 * spectral index is calculated between the minimum and the maximum
	 * provided wavelengths.
	 *
	 * @param mixture The grain mixture.
	 * @param npoints The number of points between 1 and 10000 microns to
	 * use for the grain maximum size.
	 * @param np The number of sizes to integrate.
	 * @param wmin The minimum wavelength for the charts.
	 * @param wmax The maximum wavelength for the charts.
	 * @param max True to use the maximum wavelength for the opacity, false for
	 * the minimum wavelength.
	 * @return The charts for the opacity and dust spectral index.
	 * @throws JPARSECException If an error occurs.
	 */
	public static CreateChart[] getDustModelChart(DustOpacity[] mixture, int npoints, int np, double wmin,
			double wmax, boolean max)
	throws JPARSECException {
		double sizes[] = DataSet.getSetOfValues(1, 10000, npoints, true);

		Color color[] = new Color[] {Color.RED, Color.GREEN, Color.BLUE};
		JPARSECStroke stroke[] = new JPARSECStroke[] {
				JPARSECStroke.STROKE_LINES_LARGE,
				JPARSECStroke.STROKE_LINES_MEDIUM,
				JPARSECStroke.STROKE_LINES_SHORT,
				};
		ChartSeriesElement series_k[] = new ChartSeriesElement[3];
		ChartSeriesElement series_beta[] = new ChartSeriesElement[3];
		int serie = -1;
		String grainName = "mixture of ";
		for (int i=0; i<mixture.length; i++) {
			grainName += mixture[i].getDustName();
			if (i<mixture.length-1) grainName += ", ";
		}
		double x[] = new double[npoints];
		double y_k[] = new double[npoints];
		double y_beta[] = new double[npoints];

		for (double p=2.5; p<=3.5; p = p + 0.5)
		{
			for (int size = 0; size < sizes.length; size ++)
			{
				for (int i=0; i<mixture.length; i++) {
					mixture[i] = new DustOpacity(mixture[i].grainType, p, sizes[size], mixture[i].abundanceFraction);
					mixture[i].solveDustModelUsingTabulatedValues(new double[] {wmin, wmax}, true);
				}
				mixture = DustOpacity.solveDustModel(mixture, new double[] {wmin, wmax}, 1000);
				DustOpacity d = DustOpacity.tranformDustModelToSingleGrain(mixture);
				double val[] = d.getOpacityAndSpectralIndex(wmin, wmax, np);
				x[size] = sizes[size];
				y_k[size] = val[1];
				if (!max) y_k[size] = val[0];
				y_beta[size] = val[2];
			}

			serie ++;
			ChartSeriesElement chartSeries_k = new ChartSeriesElement(
					x.clone(),
					y_k.clone(), null, null,
					"p = "+p, true, color[serie], ChartSeriesElement.SHAPE_CIRCLE,
					ChartSeriesElement.REGRESSION.NONE);
			chartSeries_k.showLines = true;
			chartSeries_k.showShapes = false;
			chartSeries_k.stroke = stroke[serie];
			series_k[serie] = chartSeries_k.clone();
			ChartSeriesElement chartSeries_beta = new ChartSeriesElement(
					x.clone(),
					y_beta.clone(), null, null,
					"p = "+p, true, color[serie], ChartSeriesElement.SHAPE_CIRCLE,
					ChartSeriesElement.REGRESSION.NONE);
			chartSeries_beta.showLines = true;
			chartSeries_beta.showShapes = false;
			chartSeries_beta.stroke = stroke[serie];
			series_beta[serie] = chartSeries_beta.clone();
		}

		ChartElement chart_k = new ChartElement(series_k, ChartElement.TYPE.XY_CHART, ChartElement.SUBTYPE.XY_SCATTER,
				Translate.translate(447)+" "+grainName, "a_{max} (@mum)",
				"k_{@nu} (cm^{2} g^{-1})", false, 500, 550);
		chart_k.xAxisInLogScale = true;
		ChartElement chart_beta = new ChartElement(series_beta, ChartElement.TYPE.XY_CHART, ChartElement.SUBTYPE.XY_SCATTER,
				Translate.translate(449)+" "+grainName, "a_{max} (@mum)",
				"@beta", false, 500, 550);
		chart_beta.xAxisInLogScale = true;
		CreateChart ch[] = new CreateChart[] {new CreateChart(chart_k), new CreateChart(chart_beta)};
		return ch;
	}

	/**
	 * Returns the characteristic name of this instance.
	 * @return The characteristic name, currently equal to the dust grain name,
	 * the max size, the size distribution coefficient, and the relative abundance.
	 */
	public String getInstanceName()
	{
		String name = "";
		try {
			name = this.getDustName() + "(r="+this.sizeMax+", p="+this.sizeDistributionCoefficient+", X="+this.abundanceFraction+")";
		} catch (Exception exc) {};
		return name;
	}

	private static ArrayList<String> oss;
	/**
	 * Returns the opacity of dust following Ossenkopf and Henning 1994.
	 * @param icyMantles Type of mantles. Can be 'NORMAL' (default), 'THICK', or 'THIN'.
	 * @param wave Wavelength in microns.
	 * @return The opacity of the dust in cm^2/g.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double getOssenkopfOpacity(String icyMantles, double wave) throws JPARSECException {
		if (oss == null) {
			String ossFile = FileIO.LIBRARY_ROOT_NAME + Zip.ZIP_SEPARATOR + "astrophysics" + 
					Zip.ZIP_SEPARATOR + "model" + Zip.ZIP_SEPARATOR + "Ossenkopf.txt";
			oss = ReadFile.readResource(ossFile);
		}

		int init = 6;
		double[] ix = new double[oss.size()-init];
		double[] iy = new double[oss.size()-init];

		int dust_field = 6;
		if (icyMantles != null) {
			if (icyMantles.equals("THIN"))
				dust_field = 3;
			if (icyMantles.equals("THICK"))
				dust_field = 9;
		}

		int field1 = dust_field;
		for (int i = init; i < oss.size(); i++)
		{
			ix[i-init] = Double.parseDouble(FileIO.getField(1, oss.get(i), " ", true));

			// Apply linear interpolation in log scale
			double k1 = Double.parseDouble(FileIO.getField(field1, oss.get(i), " ", true));

			iy[i-init] = k1;
		}
		Interpolation interp = new Interpolation(ix, iy, true);
		double opacity = 0.0;
		try {
			opacity = interp.linearInterpolation(wave);
			if (opacity < 0.0) opacity = 0.0;
		} catch (Exception exc)
		{
			exc.printStackTrace();
		}

		return opacity;
	}
}
