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
package jparsec.astronomy;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;

import java.util.Arrays;

import jparsec.astronomy.Star.LUMINOSITY_CLASS;
import jparsec.graph.ChartElement;
import jparsec.graph.ChartSeriesElement;
import jparsec.graph.CreateChart;
import jparsec.graph.DataSet;
import jparsec.graph.chartRendering.AWTGraphics;
import jparsec.math.Constant;
import jparsec.math.DoubleVector;
import jparsec.math.FastMath;
import jparsec.math.LinearFit;
import jparsec.math.LinearFit.MAX_CORRELATION;
import jparsec.math.MeasureElement;
import jparsec.util.JPARSECException;

/**
 * This class allows to work with HR diagrams and to fit or calibrate those coming
 * from observations of star clusters.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class HRDiagram {

	private double T[], Mv[];
	private String source;
	private double dist;
	private HRDiagram fittedHR = null;
	private MeasureElement turnOff[] = null;

	/**
	 * Constructor for an HR diagram using experimental data.
	 * @param T The set of effective temperatures.
	 * @param m The set of apparent magnitudes in V band.
	 * @param source The source name.
	 */
	public HRDiagram(double T[], double m[], String source) {
		dist = 0; // not fitted
		this.T = T.clone();
		this.Mv = m.clone();
		this.source = source;
	}

	/**
	 * Creates a synthetic HR diagram with absolute magnitude as
	 * function of effective temperature.
	 * @param showGiants True to show also giant stars.
	 * @param showSuperGiants True to show also supergiant stars.
	 * Giant stars must be shown or this flag will have no effect.
	 * @param np Number of points in the diagram. 100 will use 100
	 * points for main sequence stars, another 100 for giants, and
	 * 100 more for supergiants.
	 * @param minTef The minimum effective temperature for the points.
	 * @param maxTef The maximum effective temperature for the points.
	 * @throws JPARSECException If an error occurs.
	 */
	public HRDiagram(boolean showGiants, boolean showSuperGiants, int np, int minTef, int maxTef) throws JPARSECException {
		if (maxTef <= 3000 || maxTef > 50000 || minTef > maxTef) throw new JPARSECException("Maximum Teff must be 50000 K or less, and greater than 3000 K.");
		source = null;
		int n = np;
		if (showGiants) {
			n += np;
			if (showSuperGiants) n += np;
			if (maxTef < 3400) n = 2*np;
		}
		T = new double[n];
		Mv = new double[n];

		Star.LUMINOSITY_CLASS lclass = LUMINOSITY_CLASS.MAIN_SEQUENCE_V;
		for (int i=0; i<n; i++) {
			if (i < np) {
				T[i] = minTef + (maxTef - minTef) * Math.random(); // to 30000, but 50000 is also possible
				Mv[i] = Star.getStarAbsoluteMagnitude(Star.getStarBminusV(T[i], lclass), lclass);
				continue;
			}
			double minT = Math.max(3400, minTef);
			if (i < 2*np && maxTef > 3400) {
				lclass = LUMINOSITY_CLASS.GIANTS_III;
				T[i] = minT + (Math.min(5000, maxTef) - minT) * Math.random();
				Mv[i] = Star.getStarAbsoluteMagnitude(Star.getStarBminusV(T[i], lclass), lclass);
				continue;
			}
			lclass = LUMINOSITY_CLASS.SUPERGIANTS_I;
			T[i] = minT + (Math.min(30000, maxTef) - minT) * Math.random();
			Mv[i] = Star.getStarAbsoluteMagnitude(Star.getStarBminusV(T[i], lclass), lclass);
		}
		dist = 10; // 10 pc distance for absolute magnitude = apparent magnitude
	}

	/**
	 * Returns a chart with the HR diagram.
	 * @param invert True to inver X and y axes.
	 * @return The chart.
	 * @throws JPARSECException If an error occurs.
	 */
	public CreateChart getChart(boolean invert) throws JPARSECException {
		String title = "Theoretical HR diagram";
		if (source != null) title = "HR diagram of " + source;
		int ss = ChartSeriesElement.getShapeSize();
		ChartSeriesElement.setShapeSize(1);
		ChartSeriesElement s = new ChartSeriesElement(T, Mv, null, null, source == null ? title:source, true, Color.BLACK,
				ChartSeriesElement.SHAPE_CIRCLE, ChartSeriesElement.REGRESSION.NONE);
		//s.shape = ChartSeriesElement.SHAPE_EMPTY;
		//s.showShapes = false;
		ChartSeriesElement.setShapeSize(2);
		if (fittedHR == null) {
			ChartElement chart = new ChartElement(new ChartSeriesElement[] {s}, ChartElement.TYPE.XY_CHART, ChartElement.SUBTYPE.XY_SCATTER,
					title, "Effective temperature (K)", source == null ? "Absolute visual magnitude" : "Apparent visual magnitude", false, 800, 600);
			if (turnOff != null && turnOff.length == 2 && turnOff[0] != null && turnOff[1] != null) s.pointers = new String[] {"("+(turnOff[0].getValue()+1500)+", "+turnOff[1].getValue()+") @LEFT@REDTurn-off point at ("+turnOff[0].toString(true)+", "+turnOff[1].toString(true)+")"};
			if (invert) chart.xAxisInverted = chart.yAxisInverted = true;
			ChartSeriesElement.setShapeSize(ss);
			return new CreateChart(chart);
		}
		ChartSeriesElement s2 = fittedHR.getChart(invert).getSeries(0);
		s2.color = Color.BLUE;
		s2.shape = ChartSeriesElement.SHAPE_CIRCLE;
		//s2.showShapes = false;
		ChartSeriesElement.setShapeSize(ss);

		ChartElement chart = new ChartElement(new ChartSeriesElement[] {s, s2}, ChartElement.TYPE.XY_CHART, ChartElement.SUBTYPE.XY_SCATTER,
				title, "Effective temperature (K)", source == null ? "Absolute visual magnitude" : "Apparent visual magnitude", false, 800, 600);
		if (turnOff != null && turnOff.length == 2 && turnOff[0] != null && turnOff[1] != null) s.pointers = new String[] {"("+(turnOff[0].getValue()+1500)+", "+turnOff[1].getValue()+") @LEFT@REDTurn-off point at ("+turnOff[0].toString(true)+", "+turnOff[1].toString(true)+")"};
		if (invert) chart.xAxisInverted = chart.yAxisInverted = true;
		return new CreateChart(chart);
	}

	/**
	 * Fits this HR diagram with another one, setting the value of the distance.
	 * @param hr The HR diagram to fit to.
	 * @param minMag Minimum value of the visual magnitude for the region where the
	 * main sequence starts and extends below for fainter stars.
	 * @param tolerance The tolerance or desired quality of the fit in magnitudes.
	 * @throws JPARSECException If an error occurs.
	 */
	public void fitDistance(HRDiagram hr, double minMag, double tolerance) throws JPARSECException {
		if (tolerance <= 0) throw new JPARSECException("Tolerance must be greater than 0.");
		ArrayList<double[]> data = DataSet.subDatasetFromYMinimum(T, Mv, null, null, minMag);
		double minT = DataSet.getMinimumValue(hr.T);
		double maxT = DataSet.getMaximumValue(hr.T);
		data = DataSet.subDatasetFromXMaximum(data.get(0), data.get(1), null, null, maxT);
		data = DataSet.subDatasetFromXMinimum(data.get(0), data.get(1), null, null, minT);
		double T[] = data.get(0), Mv[] = data.get(1);

		double ymax = DataSet.getMaximumValue(Mv);
		double ymin = DataSet.getMinimumValue(Mv);
		double ymax1 = DataSet.getMaximumValue(hr.Mv);
		double ymin1 = DataSet.getMinimumValue(hr.Mv);

		double dymax = ymax1 - ymax, dymin = ymin1 - ymin;
		if (dymax < dymin) {
			double tmp = dymax;
			dymax = dymin;
			dymin = tmp;
		}

		double minDisp = -1, bestdy = -1;
		for (double dy = dymin; dy <= dymax; dy = dy + tolerance) {
			DoubleVector dv = new DoubleVector(Mv);
			double newM[] = dv.plus(dy).getArray();

			double disp = getDispersion(hr, T, newM);
			if (disp < minDisp || minDisp == -1) {
				minDisp = disp;
				bestdy = dy;
			}
		}
		if (minDisp != -1) {
			dist = hr.dist -10 + Star.distance(0, bestdy);
			fittedHR = hr.clone();
			DoubleVector dv = new DoubleVector(fittedHR.Mv);
			double newM[] = dv.plus(-bestdy).getArray();
			fittedHR.Mv = newM;
		}
	}

	private double getDispersion(HRDiagram hr, double newT[], double[] newM) {
		boolean used[] = new boolean[hr.T.length];
		double totalDisp = 0;
		for (int i=0; i<newT.length; i++) {
			int closest = -1;
			double mind = -1;
			for (int j=0; j<hr.T.length; j++) {
				if (used[j]) continue;
				double d = FastMath.hypot(hr.Mv[j] - newM[i], hr.T[j] - newT[i]);
				if (d < mind || mind == -1) {
					mind = d;
					closest = j;
				}
			}
			if (closest == -1) break;
			used[closest] = true;

			totalDisp += mind;
		}
		return totalDisp;
	}

	/**
	 * Returns the distance to the source.
	 * @return Distance in pc.
	 */
	public double getDistance() {
		return dist;
	}

	/**
	 * Compute the turnOff point of the cluster.
	 * @param minT Minimum temperature of the region with the main sequence.
	 * @param maxT Maximum temperature of the region with the main sequence.
	 * @param minMag Minimum value of the magnitude of the region with the main sequence.
	 * @param maxMag Maximum value of the magnitude of the region within the main sequence.
	 * @param tolerance Tolerance or desired error of the fitting in magnitudes.
	 * @throws JPARSECException If an error occurs.
	 */
	public void fitTurnOffPoint(double minT, double maxT, double minMag, double maxMag, double tolerance) throws JPARSECException {
		ArrayList<double[]> data = DataSet.subDatasetFromYMaximum(T, Mv, null, null, maxMag+1);
		data = DataSet.subDatasetFromXMaximum(data.get(0), data.get(1), null, null, maxT);
		data = DataSet.subDatasetFromXMinimum(data.get(0), data.get(1), null, null, minT);
		LinearFit lf = new LinearFit(data.get(0), data.get(1));
		turnOff = lf.getMaximumCorrelation(MAX_CORRELATION.TAKE_POINTS_WITH_Y_GREATER, minMag, maxMag, tolerance);
	}

	/**
	 * Returns the turn-off point magnitude.
	 * @return Turn-off point.
	 */
	public MeasureElement getTurnOffPointMagnitude() {
		return turnOff[0];
	}

	/**
	 * Returns the turn-off point effective temperature.
	 * @return Teff of the turn-off point.
	 */
	public MeasureElement getTurnOffPointTemperature() {
		return turnOff[1];
	}

	/**
	 * Returns if this instance is equals to another.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof HRDiagram)) return false;

		HRDiagram hrDiagram = (HRDiagram) o;

		if (Double.compare(hrDiagram.dist, dist) != 0) return false;
		if (!Arrays.equals(T, hrDiagram.T)) return false;
		if (!Arrays.equals(Mv, hrDiagram.Mv)) return false;
		if (source != null ? !source.equals(hrDiagram.source) : hrDiagram.source != null) return false;
		if (fittedHR != null ? !fittedHR.equals(hrDiagram.fittedHR) : hrDiagram.fittedHR != null) return false;

		return Arrays.equals(turnOff, hrDiagram.turnOff);
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		result = T != null ? Arrays.hashCode(T) : 0;
		result = 31 * result + (Mv != null ? Arrays.hashCode(Mv) : 0);
		result = 31 * result + (source != null ? source.hashCode() : 0);
		temp = Double.doubleToLongBits(dist);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (fittedHR != null ? fittedHR.hashCode() : 0);
		result = 31 * result + (turnOff != null ? Arrays.hashCode(turnOff) : 0);
		return result;
	}

	/**
	 * Clones this instance.
	 */
	public HRDiagram clone() {
		HRDiagram hr = new HRDiagram(T.clone(), Mv.clone(), source);
		hr.dist = dist;
		hr.fittedHR = null;
		if (fittedHR != null) hr.fittedHR = this.fittedHR.clone();
		return hr;
	}

	/**
	 * Draws the different branches of the HR diagram to a Graphics2D device. The Graphics
	 * instance should be previosuly 'prepared' using the method {@linkplain CreateChart#prepareGraphics2D(Graphics2D, boolean)}.
	 * @param g A 'prepared' Graphics2D instance of an image where the HR diagram was previosuly drawn using the method
	 * of this instance.
	 * @param mainSequence Color for the main sequence branch, or null to skip drawing it.
	 * @param giant Color for the giant branch, or null to skip drawing it.
	 * @param sgiant Color for the super giant branch, or null to skip drawing it.
	 * @param whiteDwarf Color for the white dwarf branch, or null to skip drawing it.
	 * @throws JPARSECException Thrown in case of error when rotating the shape of the white dwarf branch, but should never happen.
	 */
	public void renderHRbranches(Graphics2D g, Color mainSequence, Color giant, Color sgiant,
			Color whiteDwarf) throws JPARSECException {
		GeneralPath path = new GeneralPath();
		if (mainSequence != null) {
			path.moveTo(30896.793, -4.597942);
			path.curveTo(30611.154, -4.5704546, 30162.291, -4.542968, 29876.65, -4.4605064);
			path.curveTo(29386.982, -4.405532, 28938.12, -4.323071, 28244.424, -4.2680964);
			path.curveTo(27795.56, -4.1581483, 27101.863, -4.130661, 26530.584, -4.020713);
			path.curveTo(25959.305, -3.9382515, 25510.441, -3.828303, 24898.355, -3.7733288);
			path.curveTo(24286.271, -3.6633804, 23837.408, -3.553432, 23347.74, -3.4434836);
			path.curveTo(22694.848, -3.251074, 22409.209, -3.1686127, 21674.707, -3.0861514);
			path.curveTo(21062.621, -3.00369, 20532.146, -2.948716, 20083.283, -2.7288191);
			path.curveTo(19593.615, -2.6188707, 19144.754, -2.5089223, 18655.084, -2.3439999);
			path.curveTo(18124.611, -2.2890255, 17512.525, -2.2065642, 17186.08, -2.0691288);
			path.curveTo(16696.412, -1.9866675, 16125.132, -1.9316933, 15431.435, -1.7392836);
			path.curveTo(14941.767, -1.6568223, 14452.098, -1.5468739, 14044.041, -1.4369255);
			path.curveTo(13309.539, -1.2445159, 12779.064, -1.0521061, 12166.9795, -0.94215775);
			path.curveTo(11554.894, -0.7772352, 11024.42, -0.5848255, 10493.945, -0.44739002);
			path.curveTo(9800.249, -0.20000613, 9310.58, 0.07486484, 8698.495, 0.40471002);
			path.curveTo(8168.021, 0.679581, 7923.187, 0.9269649, 7719.158, 1.2293229);
			path.curveTo(7270.296, 1.6141423, 6943.85, 1.8890133, 6372.5703, 2.383781);
			path.curveTo(6005.3193, 2.7686005, 5515.651, 3.2633681, 5270.8164, 3.6207004);
			path.curveTo(4903.5654, 4.00552, 4658.731, 4.4728003, 4169.063, 5.0500298);
			path.curveTo(4005.84, 5.5447974, 3720.2002, 6.204488, 3638.5889, 6.616794);
			path.curveTo(3516.1716, 7.0565877, 3271.3374, 7.5513554, 3148.9204, 7.9636617);
			path.curveTo(2944.8918, 8.595865, 2740.8635, 9.200582, 2618.4463, 9.915246);
			path.curveTo(2414.418, 10.519962, 2332.8064, 10.987243, 2373.6123, 11.509498);
			path.curveTo(2373.6123, 12.279137, 2496.0293, 12.663956, 2618.4463, 13.158724);
			path.curveTo(2904.0862, 13.543543, 3434.5603, 13.57103, 3883.4229, 13.516056);
			path.curveTo(4128.2573, 13.076262, 4169.063, 12.581494, 4291.48, 12.004265);
			path.curveTo(4291.48, 11.2896, 4373.0913, 10.767345, 4536.314, 10.327552);
			path.curveTo(4577.1196, 9.667862, 4781.1484, 8.9531975, 5066.788, 8.348481);
			path.curveTo(5270.8164, 7.7437654, 5434.0396, 7.0565877, 5882.902, 6.341923);
			path.curveTo(6290.959, 5.9021297, 6494.9873, 5.3523874, 6739.822, 4.85762);
			path.curveTo(7107.0728, 4.2803907, 7351.907, 3.81311, 7719.158, 3.4282908);
			path.curveTo(8168.021, 3.1259327, 8820.912, 2.6036777, 9269.774, 2.3288069);
			path.curveTo(9800.249, 1.9989617, 10371.528, 1.6966037, 11065.226, 1.3117843);
			path.curveTo(11636.505, 1.0369133, 12003.757, 0.7070681, 12534.23, 0.40471002);
			path.curveTo(13268.733, -0.007596448, 13513.567, -0.22749323, 14166.458, -0.5298513);
			path.curveTo(14778.544, -0.722261, 15431.435, -0.88718355, 16043.5205, -1.0521061);
			path.curveTo(17063.662, -1.1070803, 18043.0, -1.2720029, 18736.695, -1.3819513);
			path.curveTo(19716.033, -1.6568223, 20368.924, -1.7667707, 21144.232, -2.0416417);
			path.curveTo(22001.152, -2.2340515, 22776.46, -2.4539483, 23306.934, -2.6188707);
			path.curveTo(24204.66, -2.8387675, 24816.744, -3.00369, 25673.664, -3.0861514);
			path.curveTo(26571.39, -3.1960998, 27305.893, -3.251074, 28162.812, -3.3885095);
			path.curveTo(29305.371, -3.5809193, 29754.234, -3.6633804, 30325.514, -3.8008158);
			path.quadTo(30815.182, -3.9932256, 30855.988, -4.130661);
			path.closePath();
			g.setColor(mainSequence);
			g.fill(path);
		}

		if (giant != null) {
			path = new GeneralPath();
			path.moveTo(6879.3047, 1.908919);
			path.curveTo(7042.8096, 1.6752554, 6920.181, 1.4156291, 6797.5522, 1.0781151);
			path.curveTo(6552.2954, 0.8444515, 6347.9146, 0.6367505, 5939.153, 0.42904952);
			path.curveTo(5571.267, 0.325199, 5326.0103, 0.16942328, 5244.258, -0.11616557);
			path.curveTo(5080.753, -0.5834928, 4999.0005, -0.8171564, 4590.239, -1.0248574);
			path.curveTo(4099.7246, -1.1287079, 3650.087, -1.1287079, 3323.0776, -1.1806331);
			path.curveTo(2955.192, -1.258521, 2669.0586, -1.05082, 2423.8018, -0.7133059);
			path.curveTo(2342.0493, -0.5315675, 2546.4302, -0.32386655, 2709.9348, -0.22001606);
			path.curveTo(2996.068, -0.16809082, 3200.449, 0.013647541, 3404.8298, 0.16942328);
			path.curveTo(3527.4585, 0.48097476, 3854.4678, 0.71463835, 3854.4678, 0.8704141);
			path.curveTo(4263.2295, 1.1819656, 4467.6104, 1.2079282, 5121.6294, 1.5454423);
			path.curveTo(5407.762, 1.7271806, 5816.5244, 1.9348817, 6184.4097, 1.908919);
			path.lineTo(6593.1714, 1.8569938);
			path.closePath();
			g.setColor(giant);
			g.fill(path);
		}

		if (sgiant != null) {
			path = new GeneralPath();
			path.moveTo(2750.0176, -5.6377497);
			path.curveTo(2869.2405, -5.4445252, 3028.204, -5.3479133, 3425.6135, -5.202995);
			path.curveTo(3624.3184, -5.0822296, 4101.2095, -4.9856176, 4737.0645, -4.889005);
			path.curveTo(5094.733, -4.8648524, 5611.365, -4.816546, 6008.775, -4.647475);
			path.curveTo(6565.148, -4.502557, 7081.7803, -4.4059443, 7757.376, -4.285179);
			path.curveTo(8115.0444, -4.236873, 8631.677, -4.164414, 8949.6045, -4.188567);
			path.curveTo(9267.532, -4.261026, 9744.423, -4.3093324, 10062.351, -4.4300976);
			path.curveTo(10261.056, -4.5267096, 10618.724, -4.671628, 10936.651, -4.889005);
			path.curveTo(11214.838, -4.9614644, 11691.7295, -5.0822296, 12128.88, -5.202995);
			path.curveTo(12685.253, -5.2996073, 13281.367, -5.4203725, 13837.74, -5.4686785);
			path.curveTo(14712.041, -5.5652905, 15069.709, -5.6135964, 15665.823, -5.661903);
			path.curveTo(16063.232, -5.758515, 16778.57, -5.806821, 17652.871, -5.9034333);
			path.curveTo(18765.617, -5.9517393, 19480.953, -6.0000453, 20434.736, -6.0000453);
			path.curveTo(20911.627, -6.0725045, 21825.668, -6.0725045, 22620.488, -6.0966578);
			path.curveTo(23256.342, -6.1932697, 24249.865, -6.1691165, 25004.943, -6.265729);
			path.curveTo(25998.467, -6.265729, 27230.438, -6.289882, 27747.068, -6.314035);
			path.curveTo(28303.441, -6.362341, 28780.334, -6.338188, 29575.152, -6.483106);
			path.curveTo(29813.598, -6.5555654, 29932.82, -6.6763306, 29734.115, -6.74879);
			path.curveTo(29336.707, -6.772943, 28899.557, -6.74879, 28382.924, -6.772943);
			path.curveTo(27786.81, -6.821249, 27190.695, -6.797096, 26395.877, -6.821249);
			path.curveTo(25720.281, -6.821249, 25283.13, -6.772943, 24766.498, -6.772943);
			path.curveTo(23812.717, -6.772943, 23097.379, -6.797096, 22262.818, -6.797096);
			path.curveTo(21428.26, -6.772943, 20752.664, -6.74879, 19798.88, -6.7246366);
			path.curveTo(19123.285, -6.74879, 18288.725, -6.7246366, 17493.906, -6.700484);
			path.curveTo(16858.05, -6.700484, 16142.715, -6.6763306, 15467.118, -6.6763306);
			path.curveTo(14473.595, -6.6280246, 13639.035, -6.6280246, 12645.512, -6.6763306);
			path.curveTo(11850.693, -6.700484, 10896.91, -6.797096, 9744.423, -6.893708);
			path.curveTo(8790.641, -6.893708, 7876.599, -6.942014, 6962.557, -6.942014);
			path.curveTo(5889.552, -6.942014, 5094.733, -6.942014, 4498.619, -6.942014);
			path.curveTo(3942.2458, -6.845402, 3306.3909, -6.821249, 3067.945, -6.700484);
			path.curveTo(2670.5356, -6.5797186, 2511.572, -6.289882, 2471.831, -6.0966578);
			path.quadTo(2591.054, -6.0000453, 2789.7585, -5.734362);
			path.closePath();
			g.setColor(sgiant);
			g.fill(path);
		}

		if (whiteDwarf != null) {
			Shape s = new Ellipse2D.Double(6000, 7, 24000, 2);
			s = AWTGraphics.rotateShape(g, s, 25 * Constant.DEG_TO_RAD);
			g.setColor(whiteDwarf);
			g.fill(s);
		}
	}
}
