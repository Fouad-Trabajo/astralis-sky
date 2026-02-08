/*
 * This file is part of JPARSEC library.
 *
 * (C) Copyright 2006-2011 by T. Alonso Albi - OAN (Spain).
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
package jparsec.vo;

import java.awt.Color;
import java.awt.Shape;
import java.util.ArrayList;

import jparsec.graph.ChartElement;
import jparsec.graph.ChartSeriesElement;
import jparsec.graph.CreateChart;
import jparsec.graph.DataSet;
import jparsec.graph.JPARSECStroke;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;

/**
 * A class to access Siess et al. 2000 evolutionary tracks.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Siess {

	/**
	 * ID constant for the position of the spectral type in a Siess record.
	 */
	public static final int RECORD_SPECTRAL_TYPE = 1;
	/**
	 * ID constant for the position of the luminosity in a Siess record.
	 */
	public static final int RECORD_LUMINOSITY = 2;
	/**
	 * ID constant for the position of the radius in a Siess record.
	 */
	public static final int RECORD_RADIUS = 3;
	/**
	 * ID constant for the position of the effective temperature in a Siess record.
	 */
	public static final int RECORD_EFFECTIVE_TEMPERATURE = 4;
	/**
	 * ID constant for the position of the mass in a Siess record.
	 */
	public static final int RECORD_MASS = 5;
	/**
	 * ID constant for the position of the age in a Siess record.
	 */
	public static final int RECORD_AGE = 6;

	/**
	 * Obtains stellar parameters of a pre-main sequence star using Siess evolutionary tracks.
	 * <P>
	 * Output is a Siess record, that contains (in the first 12 elements) the following
	 * information separated by blank spaces:<P>
	 *
	 * Spectral type.<BR>
	 * Luminosity in solar units.<BR>
	 * Radius in solar radii.<BR>
	 * Effective temperature in K.<BR>
	 * Mass in solar masses.<BR>
	 * Age in years.<BR>
	 * Bolometric magnitude.<BR>
	 * Bolometric correction.<BR>
	 * B-V color in Cousins system.<BR>
	 * V-R color in Cousins system.<BR>
	 * R-I color in Cousins system.<BR>
	 * V-I color in Cousins system.<P>
	 *
	 * Adequate constants are defined in this class to access the fields of a Siess record.
	 * For more information visit L. Siess webpage at http://www-astro.ulb.ac.be/~siess/index.html.
	 *
	 * @param metallicity Metalliticy, from 0.01 to 0.04. 0.02 = solar metallicity.
	 * @param effectiveTemperature Effective temperature in K.
	 * @param luminosity Luminosity in solar units.
	 * @return Siess record, or empty string if the data cannot be found in the grid of Siess.
	 * @throws JPARSECException If an error occur.
	 */
	public static String getStellarParametersFromSiessEvolutionaryTracks(double metallicity,
			double effectiveTemperature, double luminosity)
	throws JPARSECException {
		// Use Siess evolutionary tracks
		double distanceModulus = 0.0; // No effect on stellar mass / radius
		String query = jparsec.vo.GeneralQuery.getQueryToSiessModels(metallicity, effectiveTemperature, luminosity, distanceModulus);
		String out = jparsec.vo.GeneralQuery.query(query);

		String outArray[] = jparsec.graph.DataSet.toStringArray(out, jparsec.io.FileIO.getLineSeparator());
		boolean nextLine = false;
		String record = "";
		for (int i=0; i<outArray.length; i++)
		{
			if (nextLine) {
				record = outArray[i];
				break;
			}
			if (outArray[i].trim().startsWith("ST")) nextLine = true;
		}
		return record;
	}

	/**
	 * Holds available masses for the pre-main sequence tracks.
	 */
	public static final String AVAILABLE_MASSES[] = new String[] {
		"0.1", "0.13", "0.16", "0.2", "0.25", "0.3", "0.4", "0.5", "0.6", "0.7",
		"0.8", "0.9", "1.0", "1.1", "1.2", "1.3", "1.4", "1.5", "1.6", "1.7", "1.8",
		"1.9", "2.0", "2.2", "2.5", "2.7", "3.0", "3.5", "4.0", "5.0", "6.0", "7.0"
	};

	/**
	 * Holds available metalicities for pre-main sequence tracks.
	 */
	public static final String AVAILABLE_Z[] = new String[] {"02"};

	/**
	 * ID constant for field model number.
	 */
	public static final int FIELD_MODEL_NUMBER = 1;
	/**
	 * ID constant for field phase.
	 */
	public static final int FIELD_PHASE = 2;
	/**
	 * ID constant for field luminosity, solar units.
	 */
	public static final int FIELD_LUMINOSITY = 3;
	/**
	 * ID constant for field bolometric magnitude.
	 */
	public static final int FIELD_BOLOMETRIC_MAGNITUDE = 4;
	/**
	 * ID constant for field effective radius of photosphere, solar units.
	 */
	public static final int FIELD_EFFECTIVE_RADIUS = 5;
	/**
	 * ID constant for field star radius at optical depth = 0.005, solar units.
	 */
	public static final int FIELD_STAR_RADIUS = 6;
	/**
	 * ID constant for field effective temperature, K.
	 */
	public static final int FIELD_EFFECTIVE_TEMPERATURE = 7;
	/**
	 * ID constant for field volumic mass at photosphere, in cgs.
	 */
	public static final int FIELD_EFFECTIVE_DENSITY = 8;
	/**
	 * ID constant for field gravity decimal logarithm of cgs.
	 */
	public static final int FIELD_LOG10_GRAVITY = 9;
	/**
	 * ID constant for field mass, solar units.
	 */
	public static final int FIELD_MASS = 10;
	/**
	 * ID constant for field age, years.
	 */
	public static final int FIELD_AGE = 11;

	private static final String FIELD_LABELS[] = new String[] {
		"", "Model number", "Phase", "Luminosity (L_{@SUN})", "M_{bol}", "R_{eff} (R_{@SUN})",
		"R (R_{@SUN})", "T_{eff} (K)", "@RHO (g cm^{-3})", "LOG_{10} g (cgs)",
		"M (M_{@SUN})", "Age (yr)"
	};

	/**
	 * ID constant for number of lines to skip from the header.
	 */
	public static final int SKIP_LINES = 3;

	/**
	 * ID constant for phase pre-main sequence.
	 */
	public static final int PHASE_PRE_MS = 1;
	/**
	 * ID constant for phase main sequence.
	 */
	public static final int PHASE_MS = 2;
	/**
	 * ID constant for phase post-main sequence.
	 */
	public static final int PHASE_POST_MS = 3;
	/**
	 * ID constant for all phases.
	 */
	public static final int PHASE_ALL = 0;

	/**
	 * The stellar mass.
	 */
	public double mass;
	private String data[];
	/**
	 * Constructor for a given stellar mass. Metalicity is automatically set to 0.02.
	 * @param mass The star mass.
	 * @throws JPARSECException If an error occurs.
	 */
	public Siess(String mass)
	throws JPARSECException {
		int index = DataSet.getIndex(Siess.AVAILABLE_MASSES, mass);
		if (index < 0) throw new JPARSECException("mass unavailable.");
		this.mass = Double.parseDouble(mass);
		String name = "m"+mass+"z"+"02"+".hrd";
		String file[] = DataSet.arrayListToStringArray(ReadFile.readResource(FileIO.DATA_SIESS_DIRECTORY+name));
		file = DataSet.eliminateRowFromTable(file, 1);
		file = DataSet.eliminateRowFromTable(file, 1);
		file = DataSet.eliminateRowFromTable(file, 1);
		this.data = file;
	}

	/**
	 * Returns the set of models.
	 * @return The set of models.
	 */
	public String[] getModels()
	{
		return this.data;
	}

	/**
	 * Returns a chart with the evolutionary tracks. Metalicity is automatically set to 0.02.
	 * @param minMass Minimum mass for the tracks to be drawn.
	 * @param maxMass Maximum mass.
	 * @param phase Phase/s to be charted. Constants defined in this class.
	 * @param xField X field to be charted. Constants defined in this class.
	 * @param yField Y field to be charted. Constants defined in this class.
	 * @param width Width of the chart.
	 * @param height Height of the chart.
	 * @return The chart.
	 * @throws JPARSECException If an error occurs.
	 */
	public static CreateChart getTrack(double minMass, double maxMass, int phase,
			int xField, int yField, int width, int height)
	throws JPARSECException {
		double masses[] = DataSet.toDoubleValues(Siess.AVAILABLE_MASSES);
		int n = 0;
		for (int i=0; i<masses.length; i++)
		{
			if (masses[i] >= minMass && masses[i] <= maxMass) n++;
		}

		Shape shape[] = new Shape[] {
				ChartSeriesElement.SHAPE_CIRCLE,
				ChartSeriesElement.SHAPE_DIAMOND,
				ChartSeriesElement.SHAPE_SQUARE,
				ChartSeriesElement.SHAPE_TRIANGLE_UP,
				ChartSeriesElement.SHAPE_TRIANGLE_RIGHT,
				ChartSeriesElement.SHAPE_TRIANGLE_LEFT,
				ChartSeriesElement.SHAPE_TRIANGLE_DOWN
		};
		Color color[] = new Color[] {
				Color.BLACK,
				Color.DARK_GRAY,
				Color.GRAY,
				Color.LIGHT_GRAY,
				Color.RED,
				Color.GREEN,
				Color.BLUE,
				Color.CYAN,
				Color.ORANGE,
				Color.MAGENTA,
				Color.PINK,
				Color.YELLOW
		};

		ChartSeriesElement series[] = new ChartSeriesElement[n];
		n = -1;
		for (int i=0; i<masses.length; i++)
		{
			if (masses[i] >= minMass && masses[i] <= maxMass) {
				n++;

				String name = "m"+Siess.AVAILABLE_MASSES[i]+"z"+Siess.AVAILABLE_Z[0]+".hrd";
				String file[] = DataSet.arrayListToStringArray(ReadFile.readResource(FileIO.DATA_SIESS_DIRECTORY+name));

				ArrayList<String> vx = new ArrayList<String>();
				ArrayList<String> vy = new ArrayList<String>();
				for (int j=Siess.SKIP_LINES; j<file.length; j++)
				{
					int ph = Integer.parseInt(FileIO.getField(Siess.FIELD_PHASE, file[j], " ", true));
					if (ph == phase || phase == Siess.PHASE_ALL) {
						vx.add(FileIO.getField(xField, file[j], " ", true));
						vy.add(FileIO.getField(yField, file[j], " ", true));
					}
				}
				String x[] = DataSet.arrayListToStringArray(vx);
				String y[] = DataSet.arrayListToStringArray(vy);

				Color c = Color.BLACK;
				if (n < color.length) c = color[n];
				Shape s = ChartSeriesElement.SHAPE_CIRCLE;
				if (n < shape.length) s = shape[n];

				series[n] = new ChartSeriesElement(x, y, null, null, Siess.AVAILABLE_MASSES[i], true,
						c, s, ChartSeriesElement.REGRESSION.NONE);
				series[n].showLines = true;
				series[n].showErrorBars = false;
				series[n].stroke = JPARSECStroke.STROKE_DEFAULT_LINE;
				series[n].showShapes = false;
			}
		}

		ChartElement chart = new ChartElement(series, ChartElement.TYPE.XY_CHART, ChartElement.SUBTYPE.XY_SCATTER,
				Translate.translate(415), Translate.translate(Siess.FIELD_LABELS[xField]), Translate.translate(Siess.FIELD_LABELS[yField]), false, width, height);
		chart.showErrorBars = false;
		chart.xTickLabels = ChartElement.TICK_LABELS.LOGARITHM_VALUES;
		chart.yTickLabels = ChartElement.TICK_LABELS.LOGARITHM_VALUES;

		chart.xAxisInLogScale = false;
		chart.yAxisInLogScale = false;
		return new CreateChart(chart);
	}

	/**
	 * Returns a chart with the evolutionary tracks. Metalicity is automatically set to 0.02.
	 * @param inputMasses Set of masses for the tracks. Must be available to be shown.
	 * @param phase Phase/s to be charted. Constants defined in this class.
	 * @param xField X field to be charted. Constants defined in this class.
	 * @param yField Y field to be charted. Constants defined in this class.
	 * @param width Width of the chart.
	 * @param height Height of the chart.
	 * @return The chart.
	 * @throws JPARSECException If an error occurs.
	 */
	public static CreateChart getTrack(double[] inputMasses, int phase,
			int xField, int yField, int width, int height)
	throws JPARSECException {
		double masses[] = DataSet.toDoubleValues(Siess.AVAILABLE_MASSES);
		int n = 0;
		inputMasses = DataSet.sortInCrescent(inputMasses, null, true).get(0);
		for (int i=0; i<masses.length; i++)
		{
			for (int j=0; j<inputMasses.length; j++)
			{
				if (Math.abs(masses[i]-inputMasses[j]) < 0.05) {
					n++;
					break;
				}
			}
		}

		Shape shape[] = new Shape[] {
				ChartSeriesElement.SHAPE_CIRCLE,
				ChartSeriesElement.SHAPE_DIAMOND,
				ChartSeriesElement.SHAPE_SQUARE,
				ChartSeriesElement.SHAPE_TRIANGLE_UP,
				ChartSeriesElement.SHAPE_TRIANGLE_RIGHT,
				ChartSeriesElement.SHAPE_TRIANGLE_LEFT,
				ChartSeriesElement.SHAPE_TRIANGLE_DOWN
		};
		Color color[] = new Color[] {
				Color.BLACK,
				Color.DARK_GRAY,
				Color.GRAY,
				Color.LIGHT_GRAY,
				Color.RED,
				Color.GREEN,
				Color.BLUE,
				Color.CYAN,
				Color.ORANGE,
				Color.MAGENTA,
				Color.PINK,
				Color.YELLOW
		};

		ChartSeriesElement series[] = new ChartSeriesElement[n];
		n = -1;
		for (int i=0; i<masses.length; i++)
		{
			for (int k=0; k<inputMasses.length; k++)
			{
				if (Math.abs(masses[i]-inputMasses[k]) < 0.05) {
					n++;

					String name = "m"+Siess.AVAILABLE_MASSES[i]+"z"+Siess.AVAILABLE_Z[0]+".hrd";
					String file[] = DataSet.arrayListToStringArray(ReadFile.readResource(FileIO.DATA_SIESS_DIRECTORY+name));

					ArrayList<String> vx = new ArrayList<String>();
					ArrayList<String> vy = new ArrayList<String>();
					for (int j=Siess.SKIP_LINES; j<file.length; j++)
					{
						int ph = Integer.parseInt(FileIO.getField(Siess.FIELD_PHASE, file[j], " ", true));
						if (ph == phase || phase == Siess.PHASE_ALL) {
							vx.add(FileIO.getField(xField, file[j], " ", true));
							vy.add(FileIO.getField(yField, file[j], " ", true));
						}
					}
					String x[] = DataSet.arrayListToStringArray(vx);
					String y[] = DataSet.arrayListToStringArray(vy);

					Color c = Color.BLACK;
					if (n < color.length) c = color[n];
					Shape s = ChartSeriesElement.SHAPE_CIRCLE;
					if (n < shape.length) s = shape[n];

					series[n] = new ChartSeriesElement(x, y, null, null, Siess.AVAILABLE_MASSES[i], true,
							c, s, ChartSeriesElement.REGRESSION.NONE);
					series[n].showLines = true;
					series[n].showErrorBars = false;
					series[n].stroke = JPARSECStroke.STROKE_DEFAULT_LINE;
					series[n].showShapes = false;
					series[n].pointersAngle = ChartSeriesElement.POINTER_ANGLE.DOWNWARDS;
					series[n].pointersLabelOffsetFactor = 1f;

					break;
				}
			}
		}

		ChartElement chart = new ChartElement(series, ChartElement.TYPE.XY_CHART, ChartElement.SUBTYPE.XY_SCATTER,
				Translate.translate(415), Translate.translate(Siess.FIELD_LABELS[xField]), Translate.translate(Siess.FIELD_LABELS[yField]), false, width, height);
		chart.showErrorBars = false;
		chart.xTickLabels = ChartElement.TICK_LABELS.LOGARITHM_VALUES;
		chart.yTickLabels = ChartElement.TICK_LABELS.LOGARITHM_VALUES;

		chart.xAxisInLogScale = true;
		chart.yAxisInLogScale = true;
		return new CreateChart(chart);
	}

	/**
	 * Adds a new series to show points in the tracks.
	 * @param ch The tracks chart.
	 * @param name The name of the new series.
	 * @param x The x values.
	 * @param y The y values.
	 * @param dx The x errors.
	 * @param dy The y errors.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void addSeriesToTrack(CreateChart ch, String name, String x[], String y[], double dx[], double dy[])
	throws JPARSECException {
		ChartSeriesElement series = new ChartSeriesElement(x, y, dx, dy, name, true,
				Color.BLACK, ChartSeriesElement.SHAPE_CIRCLE, ChartSeriesElement.REGRESSION.NONE);
		series.showLines = false;
		series.showErrorBars = true;
		series.showShapes = true;

		ch.getChartElement().addSeries(series);
		ch = new CreateChart(ch.getChartElement());
	}

	/**
	 * Returns all the evolutionary tracks close to certain values of luminosity and effective
	 * temperature, given also the maximum allowed change in both parameters.
	 * @param luminosity Luminosity in solar units.
	 * @param lumTolerance Tolerance in luminosity.
	 * @param effectiveTemperature Effective temperature in K.
	 * @param tempTolerance Tolerance in effective temperature.
	 * @return The tracks as given by Siess models, or null if there is none.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String[] getClosestEvolutionaryTracks(double luminosity, double lumTolerance,
			double effectiveTemperature, double tempTolerance)
	throws JPARSECException {
		String tracks[] = null;
		ArrayList<String> v = new ArrayList<String>();
		for (int i=0; i<Siess.AVAILABLE_MASSES.length; i++)
		{
			Siess s = new Siess(Siess.AVAILABLE_MASSES[i]);
			String data[] = s.getModels();
			for (int j = 0; j<data.length; j++)
			{
				double difL = Math.abs(Double.parseDouble(FileIO.getField(Siess.FIELD_LUMINOSITY, data[j], " ", true)) - luminosity);
				double difT = Math.abs(Double.parseDouble(FileIO.getField(Siess.FIELD_EFFECTIVE_TEMPERATURE, data[j], " ", true)) - effectiveTemperature);
				if (difL < lumTolerance && difT < tempTolerance) v.add(data[j]);
			}
		}
		if (v.size() > 0)
			tracks = DataSet.arrayListToStringArray(v);
		return tracks;
	}
}
