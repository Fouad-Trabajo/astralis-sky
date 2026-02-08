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
package jparsec.graph;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.UnsupportedEncodingException;

import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.io.image.Picture;

import org.stathissideris.ascii2image.core.ConversionOptions;
import org.stathissideris.ascii2image.core.ProcessingOptions;
import org.stathissideris.ascii2image.graphics.BitmapRenderer;
import org.stathissideris.ascii2image.graphics.Diagram;
import org.stathissideris.ascii2image.text.TextGrid;

/**
 * A quick way to call ditaa.
 * @author T. Alonso Albi - OAN (Spain)
 */
public class CreateDitaaChart {

	/** The diagram. */
	public String diagram;

	private boolean antialiasing = true, shadows = true, round = true, separateCommonEdges = true, transparent = true;
	private String encoding = ReadFile.ENCODING_UTF_8;
	private Color background = Color.WHITE;
	private float scale = 1f;

	/**
	 * Constructor for a quick diagram.
	 * @param d The diagram data.
	 */
	public CreateDitaaChart(String d) {
		diagram = d;
	}

	/**
	 * Constructor for a quick diagram from a set of boxes. The method
	 * requires a basic layout for the chart, including the set of boxes
	 * and the connection lines between them. These connection lines
	 * should also be defined (from where they start respect the box, N
	 * for north, S for South, E for left, and so on) in the corresponding
	 * {@linkplain DitaaBoxElement} object. A layout example is given next:<BR>
	 * <pre>
	 *     0-------|
	 *   |-----v   1-
	 *   2-3-4-5-6-||7
	 *     |-=-&lt;--=--
	 *             89
	 * </pre>
	 * The numbers represents the box locations. The lines represents the
	 * connections between the boxes. The symbols &lt; &gt; ^ v can be used to force
	 * the position of an arrow mark. The symbol = can be used to show a dashed
	 * line in horizontal orientation, in vertical it would be :.<BR>
	 * A box can also be a single text message without a box around. In this
	 * case the N S E and W attributes for the connection lines starting from
	 * that box will be interpreted as the desired alignment for the text.
	 * @param diagram The simplified layout of the diagram. Integer values
	 * like 0, 1 represents the box index. After 9 you can use
	 * A, B, C, ...
	 * @param db The set of boxes objects. Box with index 0
	 * is identified with box 0 in the diagram array.
	 * @param w Width of each box.
	 * @param h Height of each box.
	 */
	public CreateDitaaChart(String diagram[], DitaaBoxElement db[], int w, int h) {
		String out[] = new String[diagram.length * h];
		String space = DataSet.repeatString(" ", w);
		String arrow = DataSet.repeatString("-", w);
		String arrow2rd = space.substring(0, w/2) + "/" + DataSet.repeatString("-", space.substring(w/2 + 1).length());
		String arrow2lu = DataSet.repeatString("-", space.substring(0, w/2).length()) + "/" +  space.substring(w/2 + 1);
		String arrow2ru = space.substring(0, w/2) + "\\" + DataSet.repeatString("-", space.substring(w/2 + 1).length());
		String arrow2ld = DataSet.repeatString("-", space.substring(0, w/2).length()) + "\\" +  space.substring(w/2 + 1);
		int max = 0;
		for (int i=0; i<diagram.length; i++) {
			if (diagram[i].length() > max) max = diagram[i].length();
		}
		String line = DataSet.repeatString(" ", max * w);
		for (int i=0; i<out.length; i++) {
			out[i] = line;
		}
		for (int i=0; i<diagram.length; i++) {
			int index = i * h;
			for (int j=0; j<diagram[i].length(); j++) {
				String s = diagram[i].substring(j, j + 1);
				if (s.equals(" ")) {
					continue;
				} else {
					if (s.equals("-") || s.equals(">") || s.equals("<") || s.equals("=")) {
						if (j < diagram[i].length() - 1) {
							String ns = diagram[i].substring(j + 1, j + 2);
							if (isVal(ns)) {
								out[index + h / 2] = out[index + h / 2].substring(0, j * w) + arrow.substring(0, arrow.length()-1) + ">" + out[index + h / 2].substring(j * w + w);
								continue;
							}
						}
						String myarrow = DataSet.replaceAll(out[index + h / 2].substring(j * w, j * w + w), " ", "-", true);
						if (s.equals(">")) myarrow =  myarrow.substring(0, myarrow.length() / 2) + ">" + myarrow.substring(myarrow.length()/2 + 1);
						if (s.equals("<")) myarrow =  myarrow.substring(0, myarrow.length() / 2) + "<" + myarrow.substring(myarrow.length()/2 + 1);
						if (s.equals("=")) myarrow =  myarrow.substring(0, myarrow.length() / 2) + "=" + myarrow.substring(myarrow.length()/2 + 1);
						String valid = "-=<>|^v";
						boolean nextValid = false, previousValid = false;
						if (j > 0 && (isVal(diagram[i].substring(j - 1, j)) || valid.indexOf(diagram[i].substring(j - 1, j)) >= 0)) previousValid = true;
						if (j < diagram[i].length() - 1 && (isVal(diagram[i].substring(j + 1, j + 2)) || valid.indexOf(diagram[i].substring(j + 1, j + 2)) >= 0)) nextValid = true;
						if (nextValid && !previousValid) {
							out[index + h / 2] = out[index + h / 2].substring(0, j * w) + space.substring(0, w/2) + myarrow.substring(w/2) + out[index + h / 2].substring(j * w + w);
						} else {
							if (previousValid && !nextValid) {
								out[index + h / 2] = out[index + h / 2].substring(0, j * w) + myarrow.substring(0, w/2+1) + space.substring(0, w/2) + out[index + h / 2].substring(j * w + w);
							} else {
								out[index + h / 2] = out[index + h / 2].substring(0, j * w) + myarrow + out[index + h / 2].substring(j * w + w);
							}
						}
					} else {
						if (s.equals("|") || s.equals("^") || s.equals("v") || s.equals(":")) {
							if (j < diagram[i].length() - 1) {
								String ns = diagram[i].substring(j + 1, j + 2);
								if (ns.equals("-")) {
									if (i > 0) {
										ns = diagram[i-1].substring(j, j + 1);
										if (isVal(ns) || ns.equals("|") || ns.equals("^") || ns.equals("v") || ns.equals(":")) {
											out[index + h / 2] = out[index + h / 2].substring(0, j * w) + arrow2ru + out[index + h / 2].substring(j * w + w);
											out = completeVerticalLine(out, j * w + w / 2, i * h, index + h / 2 - 1, s.equals("^"), s.equals("v"));
											continue;
										}
									}
									if (i < diagram.length - 1) {
										ns = diagram[i+1].substring(j, j + 1);
										if (isVal(ns) || ns.equals("|") || ns.equals("^") || ns.equals("v") || ns.equals(":")) {
											out[index + h / 2] = out[index + h / 2].substring(0, j * w) + arrow2rd + out[index + h / 2].substring(j * w + w);
											out = completeVerticalLine(out, j * w + w / 2, index + h / 2 + 1, (i + 1) * h - 1, s.equals("^"), s.equals("v"));
											continue;
										}
									}
								}
							}

/*							String arrow2lu2 = arrow2lu, arrow2ld2 = arrow2ld, arrow2rd2 = arrow2rd, arrow2ru2 = arrow2ru;
							if (s.equals("^")) {
								arrow2lu2 =  arrow2lu2.substring(0, arrow2lu2.length() / 2) + "^" + arrow2lu2.substring(arrow2lu2.length()/2 + 1);
								arrow2ld2 =  arrow2ld2.substring(0, arrow2ld2.length() / 2) + "^" + arrow2ld2.substring(arrow2ld2.length()/2 + 1);
								arrow2rd2 =  arrow2rd2.substring(0, arrow2rd2.length() / 2) + "^" + arrow2rd2.substring(arrow2rd2.length()/2 + 1);
								arrow2ru2 =  arrow2ru2.substring(0, arrow2ru2.length() / 2) + "^" + arrow2ru2.substring(arrow2ru2.length()/2 + 1);
							}
							if (s.equals("v")) {
								arrow2lu2 =  arrow2lu2.substring(0, arrow2lu2.length() / 2) + "v" + arrow2lu2.substring(arrow2lu2.length()/2 + 1);
								arrow2ld2 =  arrow2ld2.substring(0, arrow2ld2.length() / 2) + "v" + arrow2ld2.substring(arrow2ld2.length()/2 + 1);
								arrow2rd2 =  arrow2rd2.substring(0, arrow2rd2.length() / 2) + "v" + arrow2rd2.substring(arrow2rd2.length()/2 + 1);
								arrow2ru2 =  arrow2ru2.substring(0, arrow2ru2.length() / 2) + "v" + arrow2ru2.substring(arrow2ru2.length()/2 + 1);
							}
							if (s.equals(":")) {
								arrow2lu2 =  arrow2lu2.substring(0, arrow2lu2.length() / 2) + ":" + arrow2lu2.substring(arrow2lu2.length()/2 + 1);
								arrow2ld2 =  arrow2ld2.substring(0, arrow2ld2.length() / 2) + ":" + arrow2ld2.substring(arrow2ld2.length()/2 + 1);
								arrow2rd2 =  arrow2rd2.substring(0, arrow2rd2.length() / 2) + ":" + arrow2rd2.substring(arrow2rd2.length()/2 + 1);
								arrow2ru2 =  arrow2ru2.substring(0, arrow2ru2.length() / 2) + ":" + arrow2ru2.substring(arrow2ru2.length()/2 + 1);
							}
*/
							if (j > 0) {
								String ns = diagram[i].substring(j - 1, j);
								if (ns.equals("-")) {
									if (i > 0) {
										ns = diagram[i-1].substring(j, j + 1);
										if (isVal(ns) || ns.equals("|") || ns.equals("^") || ns.equals("v") || ns.equals(":")) {
											out[index + h / 2] = out[index + h / 2].substring(0, j * w) + arrow2lu + out[index + h / 2].substring(j * w + w);
											out = completeVerticalLine(out, j * w + w / 2, i * h, index + h / 2 - 1, s.equals("^"), s.equals("v"));
											continue;
										}
									}
									if (i < diagram.length - 1) {
										ns = diagram[i+1].substring(j, j + 1);
										if (isVal(ns) || ns.equals("|") || ns.equals("^") || ns.equals("v") || ns.equals(":")) {
											out[index + h / 2] = out[index + h / 2].substring(0, j * w) + arrow2ld + out[index + h / 2].substring(j * w + w);
											out = completeVerticalLine(out, j * w + w / 2, index + h / 2 + 1, (i + 1) * h - 1, s.equals("^"), s.equals("v"));
											continue;
										}
									}
								}
							}
							if (i > 0) {
								String ns = diagram[i-1].substring(j, j + 1);
								if (ns.equals("-") || ns.equals("|") || ns.equals("^") || ns.equals("v")) {
									out = completeVerticalLine(out, j * w + w / 2, i * h - h / 2, index + h / 2, false, false);
									if (ns.equals("-")) {
										if (j == 0 || (j < diagram[i-1].length()-1 && diagram[i-1].substring(j+1, j+2).equals("-")))
											out[i * h - h / 2 - 1] = out[i * h - h / 2 - 1].substring(0, j * w) + arrow2rd + out[i * h - h / 2 - 1].substring(j * w + w);
										if (j == diagram[i-1].length()-1 || (j > 0 && diagram[i-1].substring(j-1, j).equals("-")))
											out[i * h - h / 2 - 1] = out[i * h - h / 2 - 1].substring(0, j * w) + arrow2ld + out[i * h - h / 2 - 1].substring(j * w + w);
									}
								}
							}
							if (i < diagram.length - 1) {
								String ns = diagram[i+1].substring(j, j + 1);
								if (ns.equals("-") || ns.equals("|") || ns.equals("^") || ns.equals("v")) {
									if (ns.equals("-")) {
										out = completeVerticalLine(out, j * w + w / 2, index + h / 2, (i + 1) * h + h / 2 - 1, false, false);
										if (j == 0 || (j < diagram[i+1].length()-1 && diagram[i+1].substring(j+1, j+2).equals("-")))
											out[(i + 1) * h + h / 2] = out[(i + 1) * h + h / 2].substring(0, j * w) + arrow2ru + out[(i + 1) * h + h / 2].substring(j * w + w);
										if (j == diagram[i+1].length()-1 || (j > 0 && diagram[i+1].substring(j-1, j).equals("-")))
											out[(i + 1) * h + h / 2] = out[(i + 1) * h + h / 2].substring(0, j * w) + arrow2lu + out[(i + 1) * h + h / 2].substring(j * w + w);
									} else {
										out = completeVerticalLine(out, j * w + w / 2, index, (i + 1) * h - 1, false, false);
									}
								}
							}
						} else {
							int element = Character.digit(s.charAt(0), Character.MAX_RADIX);
							String box[] = db[element].toString(w, h);
							for (int k=0; k<box.length; k++) {
								out[index + k] = out[index + k].substring(0, j * w) + box[k] + out[index + k].substring(j * w + w);
							}
						}
					}
				}
			}
		}

		// Eliminate excesive white spaces and blank lines
		max = 0;
		for (int i=0; i<out.length; i++) {
			int l = DataSet.rtrim(out[i]).length();
			if (l > max) max = l;
		}
		for (int i=out.length-1; i>=0; i--) {
			if (out[i].trim().equals("")) {
				try {
					out = DataSet.eliminateRowFromTable(out, 1 + i);
				} catch (Exception e) { }
				continue;
			}
			out[i] = out[i].substring(0, max);
		}

		this.diagram = DataSet.toString(out, FileIO.getLineSeparator());
	}

	private static boolean isVal(String ns) {
		if (!ns.equals("|") && !ns.equals(" ") && !ns.equals("-") && DataSet.isDoubleFastCheck(ns)) return true;
		return false;
	}

	private static String[] completeVerticalLine(String out[], int px, int py0, int py1, boolean arrowUp, boolean arrowDown) {
		for (int i=py0; i<= py1; i++) {
			String s = out[i].substring(0, px) + "|";
			if (arrowUp && i == py0) s = out[i].substring(0, px) + "^";
			if (arrowDown && i == py1) s = out[i].substring(0, px) + "v";
			if (out[i].length() > px) s += out[i].substring(px + 1);
			out[i] = s;
		}
		return out;
	}

	/**
	 * Renders a diagram to an image.
	 * @return The image.
	 */
	public BufferedImage ditaaRenderImage() {
		TextGrid grid = new TextGrid();

		try {
			if(!grid.initialiseWithText(diagram, null)){
				System.err.println("Cannot initialize data");
			}
		} catch (UnsupportedEncodingException e1){
			System.err.println("Error: "+e1.getMessage());
			System.exit(1);
		}

		ConversionOptions co = new ConversionOptions();
		co.setDebug(false);

		co.renderingOptions.setDropShadows(this.shadows);
		co.renderingOptions.setAntialias(this.antialiasing);
		co.renderingOptions.setScale(this.scale);
		Color col = new Color(background.getRed(), background.getGreen(), background.getBlue(), background.getAlpha());
		if (transparent) col = new Color(background.getRed(), background.getGreen(), background.getBlue(), 0);
		//co.renderingOptions.setBackgroundColor(col);

		ProcessingOptions pr = new ProcessingOptions();
		pr.setAllCornersAreRound(this.round);
		pr.setPerformSeparationOfCommonEdges(this.separateCommonEdges);
		pr.setCharacterEncoding(encoding);

		Diagram diagram = new Diagram(grid, co, pr);

		BufferedImage image = new BufferedImage(diagram.getWidth(), diagram.getHeight(),
				transparent ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.setColor(col);
		g.fillRect(0, 0, image.getWidth(), image.getHeight());
		g.dispose();
		return (BufferedImage) new BitmapRenderer().render(diagram, image, co.renderingOptions);
	}

	/**
	 * Renders a diagram to an external Graphics object.
	 * @param g2 The graphics object.
	 */
/*	public void draw(Graphics2D g2) {
		TextGrid grid = new TextGrid();

		try {
			if(!grid.initialiseWithText(diagram, null)){
				System.err.println("Cannot initialize data");
			}
		} catch (UnsupportedEncodingException e1){
			System.err.println("Error: "+e1.getMessage());
			System.exit(1);
		}

		ConversionOptions co = new ConversionOptions();
		co.setDebug(false);
		co.renderingOptions.setDropShadows(this.shadows);
		co.renderingOptions.setAntialias(this.antialiasing);
		co.renderingOptions.setScale(this.scale);
		co.processingOptions.setAllCornersAreRound(this.round);
		co.processingOptions.setPerformSeparationOfCommonEdges(this.separateCommonEdges);
		co.processingOptions.setCharacterEncoding(encoding);
		Color col = new Color(background.getRed(), background.getGreen(), background.getBlue(), background.getAlpha());
		if (transparent) col = new Color(background.getRed(), background.getGreen(), background.getBlue(), 0);
		co.renderingOptions.setBackgroundColor(col);
		Diagram diagram = new Diagram(grid, co);

		new BitmapRenderer().render(diagram, g2, co.renderingOptions);
	} */

	/**
	 * Sets if antialiasing should be used. Default is true.
	 * @param a True or false.
	 */
	public void setAntialiasing(boolean a) {
		this.antialiasing = a;
	}

	/**
	 * Sets if the chart should be transparent. Default is true.
	 * @param a True or false.
	 */
	public void setTransparent(boolean a) {
		this.transparent = a;
	}

	/**
	 * Sets if shadows should be drawn. Default is true.
	 * @param a True or false.
	 */
	public void setDrawShadows(boolean a) {
		this.shadows = a;
	}

	/**
	 * Sets if round corners should be used. Default is true.
	 * @param a True or false.
	 */
	public void setRoundCorners(boolean a) {
		this.round = a;
	}

	/**
	 * Sets if common edges should be separated. Default is true.
	 * @param a True or false.
	 */
	public void setSeparateCommonEdges(boolean a) {
		this.separateCommonEdges = a;
	}

	/**
	 * Sets the scaling for the chart, greater values would produce
	 * bigger images. Default value is 1.0f. Any value lower than 1
	 * used here will produce bad quality images compared to the
	 * result of getting the image with scale = 1 and resizing it
	 * using the methods provided in class {@linkplain Picture}.
	 * @param s The scaling factor.
	 */
	public void setScale(float s) {
		this.scale = s;
	}

	/**
	 * Sets the character encoding. UTF-8 is the default value.
	 * @param e The encoding.
	 */
	public void setEncoding(String e) {
		this.encoding = e;
	}

	/**
	 * Sets the background color for the diagram.
	 * @param col The color.
	 */
	public void setBackground(Color col) {
		this.background = col;
	}
}
