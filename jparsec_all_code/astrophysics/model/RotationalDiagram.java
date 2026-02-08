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

import jparsec.graph.ChartElement;
import jparsec.graph.ChartSeriesElement;
import jparsec.graph.ChartSeriesElement.REGRESSION;
import jparsec.graph.CreateChart;
import jparsec.graph.DataSet;
import jparsec.io.CatalogRead;
import jparsec.math.CGSConstant;
import jparsec.math.Constant;
import jparsec.math.Interpolation;
import jparsec.math.LinearFit;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;

/**
 * A class to perform calculations using LTE for rotational diagrams.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class RotationalDiagram implements Serializable
{
	private static final long serialVersionUID = 1L;

	 /**
	  * Molecule string as given from JPL or COLOGNE catalogs.
	  */
	public String mol;
	 /**
	  * Transition string as given from JPL or COLOGNE catalogs.
	  */
	public String[] tran;
	/**
	 * True if JPL catalog is used, false for COLOGNE.
	 */
	public boolean jpl_cat;
	/**
	 * True to use spline interpolation, false for linear interpolation.
	 */
	public boolean spline;
	/**
	 * Line area in K km/s.
	 */
	public double[] area;
	/**
	 * Area error.
	 */
	public double[] area_error;
	/**
	 * Rotational temperature in K.
	 */
	public double forceTrot;

	/**
	 * Width of the line. If more than one transition for a given molecule
	 * exists at frequency around width/2 from the transition frequency, the
	 * intensity will be automatically scaled properly.
	 */
	public double[] width = null;

	/**
	 * Value of the partition function for the molecule at the rotational
	 * temperature.
	 */
	public double partitionFunction;

	/**
	 * Holds column density in cm^-2 after a calculation is done.
	 */
	private double columnDensity;

	/**
	 * Holds column density error in cm^-2 after a calculation is done.
	 */
	private double columnDensityError;

	/**
	 * Array of x position in K of points for the rotational diagram, after the calculation is done.
	 */
	private double[] x;

	/**
	 * Array of y position (Log Nu/gu) of points for the rotational diagram, after the calculation is done.
	 */
	private double[] y;

	/**
	 * Array of dx error of points for the rotational diagram, after the calculation is done.
	 */
	private double[] dx;

	/**
	 * Array of dy errors of points for the rotational diagram. Can also be used
	 * the array resulting from {@linkplain LinearFit}.
	 */
	private double[] dy;

	/**
	 * Array of labels (upper transition - lower transition) for the rotational
	 * diagram, after a calculation is done.
	 */
	private String[] label;

	/**
	 * Rotational temperature in K, after a calculation is done.
	 */
	private double trot;

	/**
	 * Rotational temperature error in K, after a calculation is done.
	 */
	private double trotError;

	/** Internal title to identify the rotational diagram. */
	public String title = "";
	/** Optional String to set additional data about the LTE predicted areas. */
	public String predictedAreas;
	/** Set to true if beam filling was used in the input data, so that the areas are corrected
	 * if {@link RotationalDiagram#obtainAreaFromFit(String)} is called. This flag is only
	 * used in that function, other calculations will not change. */
	public boolean beamFilling = false;

	/**
	 * ID for an input width to disable any averaging for the transitions
	 * found around a given frequency.
	 */
	public static final double WIDTH_NO_AVERAGE = -1;

	/** Name of the molecule and transitions in the catalog. */
	public String molname, tranname[];

	/** Labels for the chart, default value if it is null. */
	public String chartTitle, chartXLabel, chartYLabel;

	/**
	 * Empty constructor.
	 */
	public RotationalDiagram() {}

	/**
	 * Explicit constructor for one point.
	 *
	 * @param mol_name Molecule name as given by JPL or COLOGNE catalogs.
	 * @param tran_name Transition name as given by JPL or COLOGNE catalogs.
	 * @param jpl_cat True if JPL catalog is used, false for COLOGNE.
	 * @param spline True to use spline interpolation, false for linear
	 *        interpolation.
	 * @param area Line area in K km/s.
	 * @param area_error Area error.
	 * @param Trot Rotational temperature in K.
	 * @param width Width of the line.
	 * @throws JPARSECException If an error occurs.
	 */
	public RotationalDiagram(String mol_name, String tran_name, boolean jpl_cat, boolean spline, double area,
			double area_error, double Trot, double width) throws JPARSECException
	{
		molname = mol_name;
		tranname = new String[] {tran_name};

		String mol = CatalogRead.getMolecule(mol_name, jpl_cat);
		String tran = CatalogRead.getTransition(tran_name, mol_name, jpl_cat);

		this.mol = mol;
		this.tran = new String[] {tran};
		this.jpl_cat = jpl_cat;
		this.spline = spline;
		this.area = new double[] {area};
		this.area_error = new double[] {area_error};
		this.forceTrot = Trot;
		this.width = new double[] {width};

		this.applyLTEForOnePoint();
	}

	/**
	 * Explicit general constructor.
	 *
	 * @param mol_name Molecule name as given by JPL or COLOGNE catalogs.
	 * @param tran_names Transition name array as given by JPL or COLOGNE catalogs.
	 * @param jpl_cat True if JPL catalog is used, false for COLOGNE.
	 * @param spline True to use spline interpolation, false for linear
	 *        interpolation.
	 * @param area Array of line areas in K km/s.
	 * @param area_error Array of area error.
	 * @param width Width of the lines.
	 * @throws JPARSECException If an error occurs.
	 */
	public RotationalDiagram(String mol_name, String tran_names[], boolean jpl_cat, boolean spline, double area[],
			double area_error[], double width[]) throws JPARSECException
	{
		molname = mol_name;
		tranname = tran_names;

		String mol = CatalogRead.getMolecule(mol_name, jpl_cat);
		String tran[] = new String[tran_names.length];
		for (int i=0; i<tran.length; i++)
		{
			tran[i] = CatalogRead.getTransition(tran_names[i], mol_name, jpl_cat);
		}

		this.mol = mol;
		this.tran = tran;
		this.jpl_cat = jpl_cat;
		this.spline = spline;
		this.area = area;
		this.area_error = area_error;
		this.width = width;

		this.applyLTE();
	}

	/**
	 * Explicit constructor for one point.
	 *
	 * @param mol_name Molecule name as given by JPL or COLOGNE catalogs.
	 * @param tran_name Transition name as given by JPL or COLOGNE catalogs.
	 * @param jpl_cat True if JPL catalog is used, false for COLOGNE.
	 * @param spline True to use spline interpolation, false for linear
	 *        interpolation.
	 * @param area Line area in K km/s.
	 * @param area_error Area error.
	 * @param Trot Rotational temperature in K.
	 * @throws JPARSECException If an error occurs.
	 */
	public RotationalDiagram(String mol_name, String tran_name, boolean jpl_cat, boolean spline, double area,
			double area_error, double Trot) throws JPARSECException
	{
		molname = mol_name;
		tranname = new String[] {tran_name};

		String mol = CatalogRead.getMolecule(mol_name, jpl_cat);
		String tran = CatalogRead.getTransition(tran_name, mol_name, jpl_cat);

		this.mol = mol;
		this.tran = new String[] {tran};
		this.jpl_cat = jpl_cat;
		this.spline = spline;
		this.area = new double[] {area};
		this.area_error = new double[] {area_error};
		this.forceTrot = Trot;

		this.applyLTEForOnePoint();
	}

	/**
	 * Explicit general constructor.
	 *
	 * @param mol_name Molecule name as given by JPL or COLOGNE catalogs.
	 * @param tran_names Transition name array as given by JPL or COLOGNE catalogs.
	 * @param jpl_cat True if JPL catalog is used, false for COLOGNE.
	 * @param spline True to use spline interpolation, false for linear
	 *        interpolation.
	 * @param area Array of line areas in K km/s.
	 * @param area_error Array of area error.
	 * @throws JPARSECException If an error occurs.
	 */
	public RotationalDiagram(String mol_name, String tran_names[], boolean jpl_cat, boolean spline, double area[],
			double area_error[]) throws JPARSECException
	{
		molname = mol_name;
		tranname = tran_names;

		String mol = CatalogRead.getMolecule(mol_name, jpl_cat);
		String tran[] = new String[tran_names.length];
		for (int i=0; i<tran.length; i++)
		{
			tran[i] = CatalogRead.getTransition(tran_names[i], mol_name, jpl_cat);
		}

		this.mol = mol;
		this.tran = tran;
		this.jpl_cat = jpl_cat;
		this.spline = spline;
		this.area = area;
		this.area_error = area_error;

		this.applyLTE();
	}

	/**
	 * Obtains column density and it's error supposing a value of the rotational
	 * temperature and applying LTE.
	 *
	 * @throws JPARSECException If an error occurs.
	 */
	private void applyLTEForOnePoint() throws JPARSECException
	{
		if (tran == null || tran.length < 1) throw new JPARSECException("One point at least is required.");

		// Obtain the values from the catalog - transition file
		double frec = 1.0E+6 * DataSet.parseDouble(tran[0].substring(0, 13).trim());
		double gu = DataSet.parseDouble(tran[0].substring(41, 44).trim());
		double tr = 300.0; // Default reference temperature for the intensity
		double rint = Math.pow(10.0, DataSet.parseDouble(tran[0].substring(21, 29).trim()));
		double engl = Constant.CM_TO_K * DataSet.parseDouble(tran[0].substring(31, 41).trim());
		double frec_err = 1.0E+6 * DataSet.parseDouble(tran[0].substring(13, 21).trim());
		double part = 0.0;

		// Obtain the values from the catalog - molecule file
		if (jpl_cat)
		{
			part = Math.pow(10.0, DataSet.parseDouble(mol.substring(26, 33).trim()));
		} else
		{
			part = Math.pow(10.0, DataSet.parseDouble(mol.substring(64, 77).trim()));
		}

		double scaleFactor = 1.0;
		if (width != null) {
			if (width[0] != RotationalDiagram.WIDTH_NO_AVERAGE) {
				String[] trans = CatalogRead.getTransitions(tranname[0], molname, jpl_cat, width[0]);
				double rintTotal = 0;
				for (int i=0; i<trans.length; i++) {
					rintTotal += Math.pow(10.0, DataSet.parseDouble(trans[i].substring(21, 29).trim()));
				}
				scaleFactor = rint / rintTotal;
			}
		}

		// Calculate everything
		double engu = frec * Constant.HZ_TO_K + engl;
		double fac = Math.exp(-engl / tr) - Math.exp(-engu / tr);
		double abis = rint * Math.pow(frec * 1.0E-6, 2.0) * part * 2.7964 * 1E-16 / (gu * fac);
		double w = Math.abs(area[0]) * scaleFactor * 1.0E+5;
		double error = area_error[0] * scaleFactor * 1.0E+5 * 8.0 * Math.PI * CGSConstant.BOLTZMANN_CONSTANT * frec * frec / (gu * abis * CGSConstant.PLANCK_CONSTANT * CGSConstant.SPEED_OF_LIGHT * CGSConstant.SPEED_OF_LIGHT * CGSConstant.SPEED_OF_LIGHT);
		double rnu = 8.0 * Math.PI * CGSConstant.BOLTZMANN_CONSTANT * frec * frec * w / (gu * abis * CGSConstant.PLANCK_CONSTANT * CGSConstant.SPEED_OF_LIGHT * CGSConstant.SPEED_OF_LIGHT * CGSConstant.SPEED_OF_LIGHT);

		// Obtain Nt
		double Nt = 0.0;
		double dNt = 0.0;
		double y_point = 0.0;
		double dy_point = 0.0;

		x = new double[1];
		y = new double[1];
		dx = new double[1];
		dy = new double[1];
		label = new String[1];

		x[0] = engu;
		y[0] = Math.log(rnu);
		dx[0] = frec_err * Constant.HZ_TO_K;
		dy[0] = error / rnu;

		// set point label
		int n_sup = Integer.parseInt(tran[0].substring(55, 57).trim());
		int n_inf = Integer.parseInt(tran[0].substring(67, 69).trim());
		label[0] = "" + n_sup + "-" + n_inf;
		if (area[0] < 0)
			label[0] += "*";

		double m = -1.0 / forceTrot;
		double forceTrot_error = 0.0;
		double dm = Math.abs(forceTrot_error / (forceTrot * forceTrot));
		double n = y[0] - m * x[0];
		double dn = Math.sqrt(dy[0] * dy[0] + m * m * dx[0] * dx[0] + x[0] * x[0] * dm * dm);

		double s_part_y[] = new double[7];

		/* Now we have to interpolate to obtain Nt (column density) to this Trot
		 * value. First we obtain the spin partition function from the catalog
		 * format - molecule file
		 */
		for (int i = 0; i < 7; i++)
		{

			/* We first obtain the partition function values from the JPL or the
			 * COLOGNE catalog. Note that we only use temperatures <= 300 K in
			 * the cologne catalog, and that in this case the use of the
			 * molecules "32504 *CH3OH, vt=0,1" & "33502 *C-13-H3OH, vt=0,1" will
			 * cause numerical exception, since this molecules have currently no
			 * partition values in the catalog
			 */
			if (jpl_cat)
			{
				s_part_y[i] = Math.pow(10.0, DataSet.parseDouble(mol.substring(26 + 7 * i, 33 + 7 * i).trim()));

			} else
			{
				s_part_y[i] = Math.pow(10.0, DataSet.parseDouble(mol.substring(64 + 13 * i, 77 + 13 * i).trim()));
			}
		}
		double s_part_x[] = new double[]
		{ 300.0, 225.0, 150.0, 75.0, 37.5, 18.75, 9.375 };

		// Now, if the Trot is acceptable, we interpolate with the user desired
		// method
		if (forceTrot < 500.0)
			y_point = s_part_y[0];
		if (forceTrot > 0.0 && forceTrot < 9.375)
			y_point = s_part_y[6];
		if (forceTrot <= 300.0 && forceTrot >= 9.375)
		{
			// Re-order points
			ArrayList<double[]> v = DataSet.sortInCrescent(s_part_x, s_part_y, true);
			s_part_x = v.get(0);
			s_part_y = v.get(1);

			// First we use some variables to estimate the error in the
			// interpolation
			double x_point = forceTrot + forceTrot_error;
			double y_point1 = 0.0, y_point2 = 0.0;
			y_point = 0.0;
			Interpolation interp = new Interpolation(s_part_x, s_part_y, true);
			if (x_point <= 300.0)
			{
				if (spline)
				{
					y_point = interp.splineInterpolation(x_point);
				} else
				{
					y_point = interp.linearInterpolation(x_point);
				}
				y_point1 = y_point;
			}
			x_point = forceTrot - forceTrot_error;

			y_point2 = y_point;
			if (x_point >= 9.375)
			{
				if (spline)
				{
					y_point = interp.splineInterpolation(x_point);
				} else
				{
					y_point = interp.linearInterpolation(x_point);
				}
				y_point2 = y_point;
			}

			// Now we interpolate in Trot
			x_point = forceTrot;
			if (spline)
			{
				y_point = interp.splineInterpolation(x_point);
			} else
			{
				y_point = interp.linearInterpolation(x_point);
			}

			// Now we estimate the error in the interpolation process
			x_point = Math.abs(y_point - y_point1);
			dy_point = Math.abs(y_point - y_point2);
			if (x_point > dy_point)
				dy_point = x_point;
		}

		// Obtain Nt from the interpolation and from the fit
		Nt = y_point * Math.pow(Math.E, n);
		dNt = Math.sqrt(Math.pow(y_point * dn * Math.pow(Math.E, n), 2.0) + Math.pow(dy_point * Math.pow(Math.E, n),
				2.0));

		columnDensity = Nt;
		columnDensityError = dNt;
		trot = forceTrot;
		trotError = 0.0;
		partitionFunction = y_point;
	}

	/**
	 * Obtains column density and it's error applying LTE.
	 *
	 * @throws JPARSECException If an error occurs.
	 */
	private void applyLTE() throws JPARSECException
	{
		int nfit = tran.length;
		if (nfit < 2) {
			applyLTEForOnePoint();
			return;
		}

		x = new double[nfit];
		y = new double[nfit];
		dx = new double[nfit];
		dy = new double[nfit];
		label = new String[nfit];
		for (int i = 0; i < nfit; i++)
		{
			// Obtain the values from the catalog - transition file
			double frec = 1.0E+6 * DataSet.parseDouble(tran[i].substring(0, 13).trim());
			double gu = DataSet.parseDouble(tran[i].substring(41, 44).trim());
			double tr = 300.0; // Default reference temperature for the intensity
			double rint = Math.pow(10.0, DataSet.parseDouble(tran[i].substring(21, 29).trim()));
			double engl = Constant.CM_TO_K * DataSet.parseDouble(tran[i].substring(31, 41).trim());
			double frec_err = 1.0E+6 * DataSet.parseDouble(tran[i].substring(13, 21).trim());
			double part = 0.0;

			// Obtain the values from the catalog - molecule file
			if (jpl_cat)
			{
				part = Math.pow(10.0, DataSet.parseDouble(mol.substring(26, 33).trim()));
			} else
			{
				part = Math.pow(10.0, DataSet.parseDouble(mol.substring(64, 77).trim()));
			}

			double scaleFactor = 1.0;
			if (width != null) {
				if (width[i] != RotationalDiagram.WIDTH_NO_AVERAGE) {
					String[] trans = CatalogRead.getTransitions(tranname[i], molname, jpl_cat, width[i]);
					double rintTotal = 0;
					for (int ii=0; ii<trans.length; ii++) {
						rintTotal += Math.pow(10.0, DataSet.parseDouble(trans[ii].substring(21, 29).trim()));
					}
					scaleFactor = rint / rintTotal;
				}
			}

			// Calculate everything
			double engu = frec * Constant.HZ_TO_K + engl;
			double fac = Math.exp(-engl / tr) - Math.exp(-engu / tr);
			double abis = rint * Math.pow(frec * 1.0E-6, 2.0) * part * 2.7964 * 1E-16 / (gu * fac);
			double w = Math.abs(area[i]) * scaleFactor * 1.0E+5;
			double error = area_error[i] * scaleFactor * 1.0E+5 * 8.0 * Math.PI * CGSConstant.BOLTZMANN_CONSTANT * frec * frec / (gu * abis * CGSConstant.PLANCK_CONSTANT * CGSConstant.SPEED_OF_LIGHT * CGSConstant.SPEED_OF_LIGHT * CGSConstant.SPEED_OF_LIGHT);
			double rnu = 8.0 * Math.PI * CGSConstant.BOLTZMANN_CONSTANT * frec * frec * w / (gu * abis * CGSConstant.PLANCK_CONSTANT * CGSConstant.SPEED_OF_LIGHT * CGSConstant.SPEED_OF_LIGHT * CGSConstant.SPEED_OF_LIGHT);

			// Add to fit if necessary
			x[i] = engu;
			y[i] = Math.log(rnu);

			dx[i] = frec_err * Constant.HZ_TO_K;
			dy[i] = error / rnu;

			// set point label
			int n_sup = Integer.parseInt(tran[i].substring(55, 57).trim());
			int n_inf = Integer.parseInt(tran[i].substring(67, 69).trim());
			label[i] = "" + n_sup + "-" + n_inf;
			if (area[i] < 0)
				label[i] += " (model)";

		}

		// Linear fit with 2 or more points
		LinearFit myfit = null;
		if (nfit > 1)
		{
			myfit = new LinearFit(x, y, dx, dy);
			myfit.linearFit();
		}

		// Obtain Trot and Nt from the fit
		double Nt = 0.0;
		double dNt = 0.0;
		double y_point = 0.0;
		double dy_point = 0.0;
		if (nfit > 1)
		{
			trot = -1.0 / myfit.slope;
			trotError = Math.abs(myfit.dslope / (myfit.slope * myfit.slope));

			double s_part_y[] = new double[7];

			/* Now we have to interpolate to obtain Nt (column density) to this
			 * Trot value. Firts we obtain the spin partition function from the
			 * catalog format - molecule file
			 */
			for (int i = 0; i < 7; i++)
			{

				/* We first obtain the partition function values from the jpl or
				 * the cologne catalog. Note that we only use temperatures
				 * <= 300 K in the cologne catalog, and that in this case the
				 * use of the molecules "32504 *CH3OH, vt=0,1" &
				 * "33502 *C-13-H3OH, vt=0,1" will cause numerical exception,
				 * since this molecules have currently no partition values in
				 * the catalog
				 */
				if (jpl_cat)
				{
					s_part_y[i] = Math.pow(10.0, DataSet.parseDouble(mol.substring(26 + 7 * i, 33 + 7 * i).trim()));

				} else
				{
					s_part_y[i] = Math.pow(10.0, DataSet.parseDouble(mol.substring(64 + 13 * i, 77 + 13 * i).trim()));
				}
			}
			double s_part_x[] = new double[]
			{ 300.0, 225.0, 150.0, 75.0, 37.5, 18.75, 9.375 };

			Nt = 0.0;

			// Now, if the Trot is acceptable, we interpolate with the user
			// desired method
			if (trot < 500.0)
				y_point = s_part_y[0];
			if (trot > 0.0 && trot < 9.375)
				y_point = s_part_y[6];
			if (trot <= 300.0 && trot >= 9.375)
			{
				// Re-order points
				ArrayList<double[]> v = DataSet.sortInCrescent(s_part_x, s_part_y, true);
				s_part_x = v.get(0);
				s_part_y = v.get(1);

				// First we use some variables to estimate the error in the
				// interpolation
				double x_point = trot + trotError, y_point1 = 0.0, y_point2 = 0.0;
				y_point = 0.0;
				Interpolation interp = new Interpolation(s_part_x, s_part_y, true);
				if (x_point <= 300.0)
				{
					if (spline)
					{
						y_point = interp.splineInterpolation(x_point);
					} else
					{
						y_point = interp.linearInterpolation(x_point);
					}
					y_point1 = y_point;
				}
				x_point = trot - trotError;

				y_point2 = y_point;
				if (x_point >= 9.375)
				{
					if (spline)
					{
						y_point = interp.splineInterpolation(x_point);
					} else
					{
						y_point = interp.linearInterpolation(x_point);
					}
					y_point2 = y_point;
				}

				// Now we interpolate in Trot
				x_point = trot;
				if (spline)
				{
					y_point = interp.splineInterpolation(x_point);
				} else
				{
					y_point = interp.linearInterpolation(x_point);
				}

				// Now we estimate the error in the interpolation process
				x_point = Math.abs(y_point - y_point1);
				dy_point = Math.abs(y_point - y_point2);
				if (x_point > dy_point)
					dy_point = x_point;
			}

			// Obtain Nt from the interpolation and from the fit
			Nt = y_point * Math.pow(Math.E, myfit.valueInXEqualToZero);
			dNt = Math.sqrt(Math.pow(y_point * myfit.dvalueInXEqualToZero * Math.pow(Math.E,
					myfit.valueInXEqualToZero), 2.0) + Math.pow(dy_point * Math.pow(Math.E,
					myfit.valueInXEqualToZero), 2.0));

			columnDensity = Nt;
			columnDensityError = dNt;
			partitionFunction = y_point;
		}
	}

	/**
	 * Returns the predicted area of a given transition assuming
	 * LTE, using the results from the fit to the rotational diagram.
	 * @param tran The transition data.
	 * @return The area in K km/s.
	 */
	public double obtainAreaFromFit(String tran) {
		String f = tran.substring(0, 13).trim();
		double frec = 1.0E+6 * DataSet.parseDouble(f);
		double engl = Constant.CM_TO_K * DataSet.parseDouble(tran.substring(31, 41).trim());
		double engu = frec * Constant.HZ_TO_K + engl;
		String g = tran.substring(41, 44).trim();
		double gu = DataSet.parseDouble(g);
		double rint = Math.pow(10.0, DataSet.parseDouble(tran.substring(21, 29).trim()));

		double slope = -1.0 / this.trot;
		double y0 = Math.log(this.columnDensity / partitionFunction);
		double y = y0 + engu * slope;
		double rnu = Math.pow(Math.E, y);

		double scaleFactor = 1.0;
		double tr = 300.0; // Default reference temperature for the intensity
		double part = 0.0;
		// Obtain the values from the catalog - molecule file
		if (jpl_cat)
		{
			part = Math.pow(10.0, DataSet.parseDouble(mol.substring(26, 33).trim()));
		} else
		{
			part = Math.pow(10.0, DataSet.parseDouble(mol.substring(64, 77).trim()));
		}

		double fac = Math.exp(-engl / tr) - Math.exp(-engu / tr);
		double abis = rint * Math.pow(frec * 1.0E-6, 2.0) * part * 2.7964 * 1E-16 / (gu * fac);
		double w = rnu / (8.0 * Math.PI * CGSConstant.BOLTZMANN_CONSTANT * frec * frec / (gu * abis * CGSConstant.PLANCK_CONSTANT * CGSConstant.SPEED_OF_LIGHT * CGSConstant.SPEED_OF_LIGHT * CGSConstant.SPEED_OF_LIGHT));
		double area = w / (scaleFactor * 1.0E+5);

		if (this.beamFilling) {
			int nfit = this.tran.length;
			if (nfit > 1) {
				double max = -1;
				for (int i = 0; i < nfit; i++) {
					double freq = 1.0E+6 * DataSet.parseDouble(this.tran[i].substring(0, 13).trim());
					if (freq > max || max == -1) max = freq;
				}

				double factor = (max * max) / (frec * frec);
				area /= factor;
			}
		}

		return area;
	}


	/**
	 * Returns the maximum frequency in this rotational diagram. When
	 * beam filling factor is applied, the area in each point is
	 * multiplied by (freq/max)^2, where freq is the frequency at that
	 * transition and max is the maximum frequency (=&gt; x (minimum beam/beam)^2).
	 * @return Maximum frequency in GHz.
	 */
	public double getMaximumFrequency() {
		double max = -1;
		for (int i = 0; i < tran.length; i++) {
			double freq = 1.0E+6 * DataSet.parseDouble(this.tran[i].substring(0, 13).trim());
			if (freq > max || max == -1) max = freq;
		}
		return max * 1.0E-9;
	}

	/**
	 * Obtains an upper limit to the abundance of certain molecule.
	 *
	 * @param beam_area Beam area in arcseconds^2.
	 * @param distance Distance to object in pc.
	 * @param distance_error Error in distance.
	 * @param disk_mass Mass of the disk in solar masses.
	 * @param disk_mass_error Error in the mass of the disk.
	 * @return An array with the upper limit to abundance (relative to H2 = 1),
	 *         and it's error.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[] obtainUpperLimitToAbundance(double beam_area, double distance,
			double distance_error, double disk_mass, double disk_mass_error) throws JPARSECException
	{
		double X = this.columnDensity * beam_area * Math.pow(distance * Constant.AU * 1.0E5, 2.0) * Constant.H2_MASS / (disk_mass * Constant.SUN_MASS);
		double dX = Math.sqrt(Math.pow(X * this.columnDensityError / this.columnDensity, 2.0) + Math.pow(
				X * disk_mass_error / disk_mass, 2.0) + Math.pow(X * distance_error * 2.0 / distance, 2.0));

		return new double[]	{ X, dX };
	}

	/**
	 * Obtains an upper limit to the abundance of certain molecule.
	 *
	 * @param beam_area Beam area in arcseconds^2.
	 * @param distance Distance to object in pc.
	 * @param distance_error Error in distance.
	 * @param abundance Abundance of the molecule.
	 * @return An array with the upper limit to mass and it's error.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[] obtainUpperLimitToMass(double beam_area, double distance,
			double distance_error, double abundance) throws JPARSECException
	{
		double mass = this.columnDensity * beam_area * Math.pow(distance * Constant.AU * 1.0E5, 2.0) * Constant.H2_MASS / (abundance * Constant.SUN_MASS);
		double dmass = Math.sqrt(Math.pow(mass * this.columnDensityError / this.columnDensity, 2.0) + Math
				.pow(mass * distance_error * 2.0 / distance, 2.0));

		return new double[]	{ mass, dmass };
	}

	/**
	 * Obtains column density.
	 * @return Column density in cm^-2.
	 */
	public double getColumnDensity()
	{
		return this.columnDensity;
	}
	/**
	 * Obtains column density error.
	 * @return Column density error in cm^-2.
	 */
	public double getColumnDensityError()
	{
		return this.columnDensityError;
	}
	/**
	 * Obtains x errors for the rotational diagram.
	 * @return X errors.
	 */
	public double[] getXErrors()
	{
		return this.dx;
	}
	/**
	 * Obtains y errors for the rotational diagram.
	 * @return Y errors.
	 */
	public double[] getYErrors()
	{
		return this.dy;
	}
	/**
	 * Obtains x for the rotational diagram.
	 * @return X values in K.
	 */
	public double[] getX()
	{
		return this.x;
	}
	/**
	 * Obtains y for the rotational diagram.
	 * @return Y values, Log(Nu/gu).
	 */
	public double[] getY()
	{
		return this.y;
	}
	/**
	 * Obtains the rotational temperature.
	 * @return Rotational temperature in K.
	 */
	public double getTrot()
	{
		return this.trot;
	}
	/**
	 * Obtains the rotational temperature error.
	 * @return Rotational temperature error in K.
	 */
	public double getTrotError()
	{
		return this.trotError;
	}
	/**
	 * Partition function of the molecule.
	 * @return Partition function.
	 */
	public double getPartitionFunction()
	{
		return this.partitionFunction;
	}
	/**
	 * Obtains the labels for the points in the rotational diagram.
	 * @return Labels, defined as upper transition - lower transition.
	 */
	public String[] getLabels()
	{
		return this.label;
	}

	private ChartSeriesElement[] getSeries(boolean logScaleY)
	throws JPARSECException {
		int n = 1;
		String labels[] = this.getLabels();
		double x[] = this.getX();
		double y[] = this.getY();
		double dx[] = this.getXErrors();
		double dy[] = this.getYErrors();
		ArrayList<String> v = new ArrayList<String>();
		v.add(labels[0]);

		Shape shapes[] = new Shape[] {ChartSeriesElement.SHAPE_CIRCLE, ChartSeriesElement.SHAPE_DIAMOND,
				ChartSeriesElement.SHAPE_ELLIPSE, ChartSeriesElement.SHAPE_SQUARE, ChartSeriesElement.SHAPE_TRIANGLE_DOWN,
				ChartSeriesElement.SHAPE_TRIANGLE_UP, ChartSeriesElement.SHAPE_TRIANGLE_LEFT, ChartSeriesElement.SHAPE_TRIANGLE_RIGHT};
		Color colors[] = new Color[] {Color.BLACK, Color.BLUE, Color.RED, Color.LIGHT_GRAY, Color.GREEN,
				Color.MAGENTA, Color.YELLOW, Color.ORANGE};

		boolean justOnePoint = false;
		if (labels.length > 1) {
			for (int i=1; i<labels.length; i++)
			{
				String labeli = labels[i];
				boolean isNew = true;
				for (int j=0; j<i; j++)
				{
					if (labels[j].equals(labeli)) isNew = false;
				}
				if (isNew) {
					n++;
					v.add(labeli);
				}
			}
		} else {
			justOnePoint = false;
		}
		int nseries = n;

		ChartSeriesElement series[] = new ChartSeriesElement[nseries+1];
		for (int label=0; label<n; label ++)
		{
			String labelToSearch = v.get(label);
			int nn = 0;
			for (int i=0; i<this.x.length; i++)
			{
				if (labels[i].equals(labelToSearch)) {
					nn++;
				}
			}
			double xval[] = new double[nn];
			double yval[] = new double[nn];
			double dxval[] = new double[nn];
			double dyval[] = new double[nn];
			nn = -1;
			for (int i=0; i<this.x.length; i++)
			{
				if (labels[i].equals(labelToSearch)) {
					nn++;
					xval[nn] = x[i];
					yval[nn] = y[i];
					dxval[nn] = dx[i];
					dyval[nn] = dy[i];
					if (logScaleY) {
						yval[nn] = Math.pow(10.0, y[i]);
						dyval[nn] = yval[nn] * dy[i];
					}
				}
			}
			int nForShapeAndColor = label;
			if (label >= shapes.length) nForShapeAndColor = shapes.length - 1;
			series[label] = new ChartSeriesElement(xval, yval, dxval, dyval, labelToSearch, true,
					colors[nForShapeAndColor], shapes[nForShapeAndColor], ChartSeriesElement.REGRESSION.NONE);
			series[label].showLines = false;

			if (justOnePoint) {
				double xAux = xval[0] * 2.0;
				yval[0] = (-1.0 / this.getTrot()) * (xAux - xval[0]) + yval[0];
				xval[0] = xAux;
				dxval[0] = 0.0;
				dyval[0] = 0.0;
				if (logScaleY) {
					yval[nn] = Math.pow(10.0, y[label]);
				}

				series[label+1] = new ChartSeriesElement(xval, yval, dxval, dyval, labelToSearch, false,
						colors[nForShapeAndColor], shapes[nForShapeAndColor], ChartSeriesElement.REGRESSION.NONE);
				//series[label+1].showErrorBars = false;
				series[label+1].showShapes = false;
			}

		}

		if (logScaleY) {
			for (int i=0; i<x.length; i++)
			{
				y[i] = Math.pow(10.0, y[i]);
				dy[i] = y[i] * dy[i];
			}
		}

		series[nseries] = new ChartSeriesElement(x, y, dx, dy, "all", false,
				colors[0], shapes[0], ChartSeriesElement.REGRESSION.LINEAR);
		//series[nseries].showErrorBars = false;
		series[nseries].showShapes = false;
		if (x.length == 1) series[nseries].regressionType = REGRESSION.NONE;

		return series;
	}

	/**
	 * Obtains a chart with the rotational diagram.
	 * @param title The default title of the chart.
	 * @param width Chart width.
	 * @param height Chart height.
	 * @param logScaleX True for log scale in x axis.
	 * @param logScaleY True fot log scale in y axis.
	 * @return The chart.
	 * @throws JPARSECException If an error occurs.
	 */
	public CreateChart getChart(String title, int width, int height, boolean logScaleX, boolean logScaleY)
	throws JPARSECException {
		if (this.chartTitle != null) title = chartTitle;

		String labelY = "LOG(Nu/gu)";
		if (logScaleY) labelY = "Nu/gu";
		if (this.chartYLabel != null) labelY = chartYLabel;

		String labelX = Translate.translate(414)+" (K)";
		if (this.chartXLabel != null) labelX = chartXLabel;

		ChartElement chart = new ChartElement(this.getSeries(logScaleY), ChartElement.TYPE.XY_CHART, ChartElement.SUBTYPE.XY_SCATTER,
				title, labelX, labelY, false, width, height);
		//chart.showErrorBars = false;
		chart.xTickLabels = ChartElement.TICK_LABELS.LOGARITHM_VALUES;
		chart.yTickLabels = ChartElement.TICK_LABELS.LOGARITHM_VALUES;

		chart.xAxisInLogScale = logScaleX;
		chart.yAxisInLogScale = logScaleY;

		return new CreateChart(chart);
	}
}
