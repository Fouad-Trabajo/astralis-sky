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

import java.awt.Color;
import java.util.ArrayList;

import java.util.Arrays;
import jparsec.ephem.Functions;
import jparsec.graph.ChartElement3D;
import jparsec.graph.ChartSeriesElement3D;
import jparsec.graph.CreateChart3D;
import jparsec.graph.DataSet;
import jparsec.math.matrix.Matrix;
import jparsec.math.matrix.SingularValueDecomposition;
import jparsec.util.JPARSECException;

/**
 * A class to hold the results of a PCA (Principal Component Analysis) calculation.
 * Internal implementation is based on singular value decomposition, using matrices.
 * The implemented follows the tutorial at
 * http://www.ce.yildiz.edu.tr/personal/songul/file/1097/principal_components.pdf.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class PCAElement {

	private SingularValueDecomposition svd;
	private final double[][] originalData;

	/**
	 * Constructor for a matrix of data. First index of the array
	 * should have as size the number of independent variables, the
	 * second their values for each of them.
	 * @param data The data.
	 * @throws JPARSECException If an error occurs.
	 */
	public PCAElement(double[][] data) throws JPARSECException {
		originalData = DataSet.cloneArray(data);
		init(originalData);
	}

	/**
	 * Constructor for a list of variables. Each double array in
	 * the list will contain the values for each independent variable.
	 * Length should be the same for all them.
	 * @param data The data.
	 * @throws JPARSECException If an error occurs.
	 */
	public PCAElement(ArrayList<double[]> data) throws JPARSECException {
		originalData = DataSet.toDoubleArray(data);
		init(originalData);
	}

	private void init(double[][] m0) throws JPARSECException {
		double m[][] = DataSet.cloneArray(this.originalData);
		for (int i=0; i<m.length; i++) {
			double mv = Functions.sumComponents(originalData[i]) / originalData[i].length;
			for (int j=0; j<m[i].length; j++) {
				m[i][j] -= mv;
			}
		}
		Matrix matrix = new Matrix(m);
		svd = matrix.svd();
	}

	/**
	 * Returns the original data for the constructor in this instance.
	 * @return Original data.
	 */
	public double[][] getOriginalData() {
		return originalData;
	}

	/**
	 * Returns the instance of {@linkplain SingularValueDecomposition} used
	 * in the analysis.
	 * @return The instance of {@linkplain SingularValueDecomposition}.
	 */
	public SingularValueDecomposition getSingularValueDecomposition() {
		return svd;
	}

	/**
	 * Returns the singular values.
	 * @return Singular values.
	 */
	public double[] getSingularValues() {
		return svd.getSingularValues().clone();
	}

	/**
	 * Returns the singular vectors.
	 * @return Singular vectors.
	 */
	public double[][] getSingularVectors() {
		return DataSet.cloneArray(svd.getU().transpose().getArray());
	}

	/**
	 * Reproduces the original data up to certain level
	 * given the value of the components to use.
	 * @param n Number of components to use to reproduce
	 * original data. 1 for principal component, 2 for
	 * 2 components, and so on.
	 * @return The data reproduced.
	 * @throws JPARSECException If the number of components
	 * is outside range 1 - number of singular values.
	 */
	public double[][] reproduceOriginalData(int n) throws JPARSECException {
		if (n < 1 || n > svd.rank()) throw new JPARSECException("The number of components must be between 1-"+svd.rank());

		Matrix fdata = new Matrix(getNewValues());
		double v[][] = this.getSingularVectors();
		for (int i=n; i<v.length; i++) {
			for (int j=0; j<v[i].length; j++) {
				v[i][j] = 0;
			}
		}
		Matrix sv = (new Matrix(v)); //.transpose();
		v = (sv.times(fdata)).getArray();
		for (int i=0; i<v.length; i++) {
			double mv = Functions.sumComponents(originalData[i]) / originalData[i].length;
			for (int j=0; j<v[i].length; j++) {
				v[i][j] += mv;
			}
		}
		return v;
	}

	/**
	 * Returns a 3d chart for the first 3 dimensions in the data reproduced to
	 * certain number of components n.
	 * In case of 2d input z will be set to 0s.
	 * @param n Number of components to use to reproduce
	 * original data. 1 for principal component, 2 for
	 * 2 components, and so on.
	 * @param title Chart title.
	 * @param xLabel Label for x axis.
	 * @param yLabel Label for y axis.
	 * @param zLabel Label for z axis.
	 * @param leyend Leyend for the data.
	 * @return The chart.
	 * @throws JPARSECException If an error occurs.
	 */
	public CreateChart3D reproduceOriginalDataAsChart(int n, String title, String xLabel, String yLabel, String zLabel, String leyend) throws JPARSECException {
		return this.reproduceOriginalDataAsChart(n, title, xLabel, yLabel, zLabel, leyend, 0, 1, 2);
	}

	/**
	 * Returns a 3d chart for 3 selected dimensions in the data reproduced to
	 * certain number of components n.
	 * @param n Number of components to use to reproduce
	 * original data. 1 for principal component, 2 for
	 * 2 components, and so on.
	 * @param title Chart title.
	 * @param xLabel Label for x axis.
	 * @param yLabel Label for y axis.
	 * @param zLabel Label for z axis.
	 * @param leyend Leyend for the data.
	 * @param xIndex Index of the column for the variable to show in x axis, 0 is the first.
	 * @param yIndex Index of the column for the variable to show in y axis, 0 is the first.
	 * @param zIndex Index of the column for the variable to show in z axis, 0 is the first.
	 * @return The chart.
	 * @throws JPARSECException If an error occurs.
	 */
	public CreateChart3D reproduceOriginalDataAsChart(int n, String title, String xLabel, String yLabel, String zLabel, String leyend,
			int xIndex, int yIndex, int zIndex) throws JPARSECException {
		double newData[][] = this.reproduceOriginalData(n);
		double z[] = new double[newData[0].length];
		if (newData.length > zIndex) z = newData[zIndex];
		ChartSeriesElement3D series[] = new ChartSeriesElement3D[] {new ChartSeriesElement3D(newData[xIndex], newData[yIndex], z, leyend)};
		ChartElement3D chart = new ChartElement3D(series, title, xLabel, yLabel, zLabel);
		chart.showToolbar = true;
		chart.showLegend = false;
		chart.showTitle = false;
		chart.series[0].isBarPlot = true;
		if (title != null && !title.equals("")) chart.showTitle = true;
		if (leyend != null && !leyend.equals("")) chart.showLegend = true;
		//chart.showGridX = chart.showGridY = chart.showGridZ = false;
		CreateChart3D c = new CreateChart3D(chart);
		c.setSameScale();
		return c;
	}

	/**
	 * Clones this instance. In case of error null is returned.
	 */
	public PCAElement clone() {
		try {
			return new PCAElement(this.originalData);
		} catch (Exception exc) {
			return null;
		}
	}

	/**
	 * Checks if this instance is equals to another object or not.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof PCAElement)) return false;

		PCAElement that = (PCAElement) o;

		if (svd != null ? !svd.equals(that.svd) : that.svd != null) return false;

		return Arrays.deepEquals(originalData, that.originalData);
	}

	@Override
	public int hashCode() {
		int result = svd != null ? svd.hashCode() : 0;
		result = 31 * result + (originalData != null ? Arrays.deepHashCode(originalData) : 0);
		return result;
	}

	/**
	 * Returns a 3d chart for the first 3 dimensions in the input data.
	 * In case of 2d input z will be set to 0s.
	 * @param title Chart title.
	 * @param xLabel Label for x axis.
	 * @param yLabel Label for y axis.
	 * @param zLabel Label for z axis.
	 * @param leyend Leyend for the data.
	 * @return The chart.
	 * @throws JPARSECException If an error occurs.
	 */
	public CreateChart3D getChart(String title, String xLabel, String yLabel, String zLabel, String leyend) throws JPARSECException {
		return this.getChart(title, xLabel, yLabel, zLabel, leyend, 0, 1, 2);
	}

	/**
	 * Returns a 3d chart for 3 selected variables in the input data.
	 * In case of 2d input z will be set to 0s.
	 * @param title Chart title.
	 * @param xLabel Label for x axis.
	 * @param yLabel Label for y axis.
	 * @param zLabel Label for z axis.
	 * @param leyend Leyend for the data.
	 * @param xIndex Index of the column for the variable to show in x axis, 0 is the first.
	 * @param yIndex Index of the column for the variable to show in y axis, 0 is the first.
	 * @param zIndex Index of the column for the variable to show in z axis, 0 is the first.
	 * @return The chart.
	 * @throws JPARSECException If an error occurs.
	 */
	public CreateChart3D getChart(String title, String xLabel, String yLabel, String zLabel, String leyend,
			int xIndex, int yIndex, int zIndex) throws JPARSECException {
		double z[] = new double[originalData[0].length];
		if (originalData.length > zIndex) z = originalData[zIndex];
		double v[][] = this.getSingularVectors();
		ChartSeriesElement3D series[] = new ChartSeriesElement3D[1 + v.length];
		series[0] = new ChartSeriesElement3D(originalData[xIndex], originalData[yIndex], z, leyend);
		double m[] = new double[v.length];
		for (int i=0; i<v.length; i++) {
			m[i] = Functions.sumComponents(originalData[i]) / originalData[i].length;
		}
		double xv[][] = new double[v.length][2];
		double yv[][] = new double[v.length][2];
		double zv[][] = new double[v.length][2];
		int index = 0;
		for (int i=0; i<v.length; i++) {
			if (i != xIndex && i != yIndex && i != zIndex) continue;

			double mm[] = m.clone();
			for (int j=0; j<m.length; j++) {
				mm[j] += v[i][j];
			}
			xv[i] = new double[] {m[xIndex], mm[xIndex]};
			yv[i] = new double[] {m[yIndex], mm[yIndex]};
			if (originalData.length > zIndex) zv[i] = new double[] {m[zIndex], mm[zIndex]};

			index ++;
			series[index] = new ChartSeriesElement3D(xv[i], yv[i], zv[i], "");
			series[index].drawLines = true;
			series[index].color = Color.RED;
		}

		series = (ChartSeriesElement3D[]) DataSet.getSubArray(series, 0, index);

		ChartElement3D chart = new ChartElement3D(series, title, xLabel, yLabel, zLabel);
		chart.showToolbar = true;
		chart.showLegend = false;
		chart.showTitle = false;
		if (title != null && !title.equals("")) chart.showTitle = true;
		if (leyend != null && !leyend.equals("")) chart.showLegend = true;
		//chart.showGridX = chart.showGridY = chart.showGridZ = false;
		CreateChart3D c = new CreateChart3D(chart);
		c.setSameScale();
		return c;
	}

	/**
	 * Returns the new values of the input data for a given axis
	 * along the corresponding singular vector.
	 * @param index The index of the singular vector.
	 * @return The new values for that axis.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[] getNewValues(int index) throws JPARSECException {
		return getNewValues()[index];
	}

	/**
	 * Returns the new values of the input data for all axes.
	 * @return The new values.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[][] getNewValues() throws JPARSECException {
		double v[][] = this.getSingularVectors();
		double m[][] = DataSet.cloneArray(this.originalData);
		double mv[] = new double[v.length];
		for (int i=0; i<v.length; i++) {
			mv[i] = Functions.sumComponents(originalData[i]) / originalData[i].length;
			for (int j=0; j<m[i].length; j++) {
				m[i][j] -= mv[i];
			}
		}
		Matrix mean = (new Matrix(m));
		Matrix sv = (new Matrix(v)).transpose();
		Matrix data = sv.times(mean);
		return data.getArray();
	}
}
