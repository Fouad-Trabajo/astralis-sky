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
package jparsec.astrophysics.gildas;

import java.awt.Color;
import java.io.Serializable;

import jparsec.ephem.Functions;
import jparsec.graph.DataSet;
import jparsec.io.LATEXReport;
import jparsec.math.MeasureElement;
import jparsec.io.HTMLReport.STYLE;
import jparsec.util.JPARSECException;

/**
 * A class to hold data for a fitted line.
 * @author T. Alonso Albi - OAN (Spain)
 */
public class SpectrumLine implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Minimum and maximum channels where the line is within.
	 */
	public int minChannel = -1, maxChannel = -1;
	/**
	 * Maximum and minimum y limits to do the fit. Set to same
	 * values to use the entire range.
	 */
	public double yMin = -1, yMax = -1;

	/** Frequency of the line in MHz. */
	public double freq = -1;
	/**
	 * Area of the line, K km/s.
	 */
	public double area = -1;
	/**
	 * Area error of the line, K km/s.
	 */
	public double areaError = -1;
	/**
	 * Peak temperature of the line, K.
	 */
	public double peakT = -1;
	/**
	 * Peak temperature uncertainty of the line, K.
	 */
	public double peakTError = -1;
	/**
	 * Velocity of the central position of the line, km/s.
	 */
	public double vel = -1;
	/**
	 * Error in the estimate of the central velocity, km/s.
	 */
	public double velError = -1;
	/**
	 * Width of the Gaussian in km/s.
	 */
	public double width = -1;
	/**
	 * Error in the width of the line, km/s.
	 */
	public double widthError = -1;

	/**
	 * Line label.
	 */
	public String label = "";

	/** Special label used for advanced charting with Gildas. */
	public String labelForChartID = "";

	/**
	 * True to consider this line in the fit and output it
	 * in the results.
	 */
	public boolean enabled = true;
	/**
	 * True if the line is already fitted accurately, after
	 * reducing the spectrum.
	 */
	public boolean fitted = false;
	/**
	 * True to consider this line as deleted.
	 */
	public boolean deleted = false;

	/**
	 * Holds the title of the rotational diagrams that this line belongs to, if any.
	 */
	public String diagrotIDs[] = null;

	/**
	 * The index of this line in the set of spectra.
	 */
	public int spectrumIndex = -1;
	/**
	 * The index of this line in the set of lines for this spectrum.
	 */
	public int lineIndex = -1;

	/** Color index. -1 to set it automatically. */
	public int colorIndex = -1;

	/** List of colors. */
	public static final Color col[] = new Color[] {
			new Color(30, 30, 30), Color.RED, Color.GREEN, Color.BLUE, Color.ORANGE, Color.MAGENTA,
			Color.PINK, Color.YELLOW, Color.CYAN, Color.GRAY
	};
	/** The names of the colors in the list. */
	public static final String colors[] = new String[] {"Black", "Red", "Green", "Blue", "Orange", "Magenta",
		"Pink", "Yellow", "Cyan", "Gray"};

	/** Clones this instance. */
	public SpectrumLine clone() {
		SpectrumLine s = new SpectrumLine(getGaussianParameters());
		s.deleted = this.deleted;
		s.enabled = this.enabled;
		s.fitted = this.fitted;
		s.label = this.label;
		s.labelForChartID = this.labelForChartID;
		s.lineIndex = this.lineIndex;
		s.spectrumIndex = this.spectrumIndex;
		s.yMax = this.yMax;
		s.yMin = this.yMin;
		s.colorIndex = this.colorIndex;
		if (this.diagrotIDs != null) s.diagrotIDs = this.diagrotIDs;
		return s;
	}

	/** Returns a string object with the parameters of the Gaussian. */
	@Override
	public String toString() {
		MeasureElement v = new MeasureElement(vel, velError, MeasureElement.UNIT_X_KMS);
		MeasureElement w = new MeasureElement(width, widthError, MeasureElement.UNIT_X_KMS);
		MeasureElement p = new MeasureElement(peakT, peakTError, MeasureElement.UNIT_Y_K);
		MeasureElement a = new MeasureElement(area, areaError, "K*km/s");

		String s = "vel = "+v.toString()+", width = "+w.toString()+", peak = "+p.toString()+", area = "+a.toString();

		return s;
	}

	/**
	 * Generates a Latex table with the results of the line fitting. Lines are sorted
	 * by increasing order of frequencies.
	 * @param sl0 Lines fitted.
	 * @param title Table title.
	 * @return Table code.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String getLatexTable(SpectrumLine sl0[], String title) throws JPARSECException {
		LATEXReport latex = new LATEXReport();
		String columns[] = new String[] {
				"Line", "Central vel.", "Width", "Peak", "Area", "Observed freq.", "Line ID"
		};
		latex.setTextStyle(STYLE.BOLD);
		latex.writeLongTableHeader(title, "lllllll");
		int length0 = latex.getCurrentCode().indexOf("\\begin{longtable}");
		latex.writeRowInTable(columns, null, null, null);
		columns = new String[] {
				"Number", "(km/s)", "(km/s)", "(K)", "(K km/s)", "(MHz)", ""
		};
		latex.writeRowInTable(columns, null, null, null);
		latex.setTextStyle(STYLE.PLAIN);
		latex.writeHorizontalLine();

		double vels[] = new double[sl0.length];
		for (int i=0; i<sl0.length; i++)
		{
			vels[i] = sl0[i].vel;
		}
		Object sl[] = DataSet.sortInDescent(sl0, vels);
		for (int i=0; i< sl.length; i++)
		{
			SpectrumLine sline = (SpectrumLine) sl[i];
			if (sline.enabled) {
				double line[] = new double[] {
						sline.vel, sline.width, sline.peakT, sline.area,
						sline.velError, sline.widthError, sline.peakTError, sline.areaError
				};

				MeasureElement me1 = new MeasureElement(line[0], line[4], "");
				MeasureElement me2 = new MeasureElement(line[1], line[5], "");
				MeasureElement me3 = new MeasureElement(line[2], line[6], "");
				MeasureElement me4 = new MeasureElement(line[3], line[7], "");

				String lIDs = fixLabelForLatex(sline.label);
				columns = new String[] {
						""+(i+1),
						latex.formatSymbols(me1.toString()),
						latex.formatSymbols(me2.toString()),
						latex.formatSymbols(me3.toString()),
						latex.formatSymbols(me4.toString()),
						""+Functions.formatValue(sline.freq, 3),
						lIDs
				};
				latex.writeRowInTable(columns, null, null, null, false);
			}
		}
		latex.endLongTable(null);

		return latex.getCurrentCode().substring(length0);
	}

	private static String fixLabelForLatex(String legend) {
		legend = DataSet.replaceAll(legend, "@LEFT_CENTER_ROTATED", "", true);
		legend = DataSet.replaceAll(legend, "@RIGHT_CENTER_ROTATED", "", true);
		legend = DataSet.replaceAll(legend, "@LEFT_UP_ROTATED", "", true);
		legend = DataSet.replaceAll(legend, "@RIGHT_UP_ROTATED", "", true);
		legend = DataSet.replaceAll(legend, "@LEFT_DOWN_ROTATED", "", true);
		legend = DataSet.replaceAll(legend, "@RIGHT_DOWN_ROTATED", "", true);
		legend = DataSet.replaceAll(legend, "@LEFT_CENTER", "", true);
		legend = DataSet.replaceAll(legend, "@RIGHT_CENTER", "", true);
		legend = DataSet.replaceAll(legend, "@UP_CENTER_ROTATED", "", true);
		legend = DataSet.replaceAll(legend, "@UP_CENTER", "", true);
		legend = DataSet.replaceAll(legend, "@UP_LEFT", "", true);
		legend = DataSet.replaceAll(legend, "@UP_RIGHT", "", true);
		legend = DataSet.replaceAll(legend, "@RED", "", true);
		legend = DataSet.replaceAll(legend, "@GREEN", "", true);
		legend = DataSet.replaceAll(legend, "@BLUE", "", true);
		legend = DataSet.replaceAll(legend, "@BLACK", "", true);
		legend = DataSet.replaceAll(legend, "@YELLOW", "", true);
		legend = DataSet.replaceAll(legend, "@ORANGE", "", true);
		legend = DataSet.replaceAll(legend, "@PINK", "", true);
		legend = DataSet.replaceAll(legend, "@GRAY", "", true);
		legend = DataSet.replaceAll(legend, "@WHITE", "", true);
		legend = DataSet.replaceAll(legend, "@CYAN", "", true);
		legend = DataSet.replaceAll(legend, "@MAGENTA", "", true);
		legend = DataSet.replaceAll(legend, "@NEWLINE", " ", true);
		legend = DataSet.replaceAll(legend, "@PREVIOUSLINE", " ", true);

		// Fix also super/sub scripts that needs the $ symbol
		String s = "^{", ss = "$^{";
		int n = legend.indexOf(s), nn = legend.indexOf(ss);
		if (n >= 0 && nn<0) {
			while(n >= 0 && nn < 0) {
				String s2 = legend.substring(n);
				int n2 = s2.indexOf("}");
				s2 = s2.substring(0, n2+1)+"$"+s2.substring(n2+1);
				legend = legend.substring(0, n) + "$" + s2;

				legend = DataSet.replaceAll(legend, ss, "@@@", true);
				n = legend.indexOf(s);
				nn = legend.indexOf(ss);
			};
			legend = DataSet.replaceAll(legend, "@@@", ss, true);
		}

		s = "_{";
		ss = "$_{";
		n = legend.indexOf(s);
		nn = legend.indexOf(ss);
		if (n >= 0 && nn<0) {
			while(n >= 0 && nn < 0) {
				String s2 = legend.substring(n);
				int n2 = s2.indexOf("}");
				s2 = s2.substring(0, n2+1)+"$"+s2.substring(n2+1);
				legend = legend.substring(0, n) + "$" + s2;

				legend = DataSet.replaceAll(legend, ss, "@@@", true);
				n = legend.indexOf(s);
				nn = legend.indexOf(ss);
			};
			legend = DataSet.replaceAll(legend, "@@@", ss, true);
		}
		return legend;
	}

	/**
	 * Returns the Gaussian parameters of this line in the same way as the method
	 * {@link ProcessSpectrum#fitLines(boolean)}.
	 * @return Coefficients of the fit, 8 values: Gaussian x
	 * central position (km/s), Gaussian width (km/s), Gaussian peak (K), Gaussian
	 * area (K km/s), and their errors. Additional values given are the minimum
	 * channel (at v = v0 - w), the maximum channel value, and the frequency in MHz.
	 */
	public double[] getGaussianParameters() {
        double out[] = new double[] {
        		vel, width, peakT, area,
        		velError, widthError, peakTError, areaError,
        		minChannel, maxChannel, freq
        };
        return out;
	}

	/**
	 * Constructs a spectrum line object by giving a set
	 * of Gaussian Parameters resulting from {@linkplain ProcessSpectrum#fitLines(boolean)}.
	 * @param lineParam Line parameters.
	 */
	public SpectrumLine(double lineParam[]) {
		vel = lineParam[0];
		width = lineParam[1];
		peakT = lineParam[2];
		area = lineParam[3];
		velError = lineParam[4];
		widthError = lineParam[5];
		peakTError = lineParam[6];
		areaError = lineParam[7];
		if (lineParam.length > 8) minChannel = (int) lineParam[8];
		if (lineParam.length > 9) maxChannel = (int) lineParam[9];
		if (lineParam.length > 10) freq = lineParam[10];
	}
}
