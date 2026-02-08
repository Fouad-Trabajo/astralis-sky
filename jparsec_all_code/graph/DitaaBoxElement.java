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

import jparsec.graph.DataSet;
import jparsec.io.FileIO;

/**
 * A handy class to store boxes for a ditaa diagram.
 * @author T. Alonso Albi - OAN (Spain)
 */
public class DitaaBoxElement {

	/** Box text. */
	public String text[];
	/** Box color. */
	public String color;
	/** Box type. */
	public TYPE type;
	/** Connections for this box: NSEW for filled points,
	 * and nesw for non-filled in each of the 4 possible
	 * directions. */
	public String connect;
	/** True for dashed shape. */
	public boolean dashed;

	/**
	 * The different types of shapes.
	 */
	public enum TYPE {
		SIMPLE(""),
		DOCUMENT("d"),
		INPUT_OUTPUT("io"),
		STORAGE("s"),
		QUESTION("c"),
		ELLIPSE("o"),
		MANUAL_OPERATION("mo"),
		TRAPEZOID("tr"),
		NO_BOX("");

		/** The string identifier for this type. */
		public String id = "";

		/**
		 * Constructor.
		 * @param s Identifier.
		 */
		private TYPE(String s) {
			id = " ";
			if (!s.equals("")) this.id = "{" + s + "}";
		}

	};

	/**
	 * Constructor for a given shape. For 'no box' type the color and dashed
	 * properties are ignored, and the box connections are used to set the
	 * text alignment.
	 * @param t The text inside the shape.
	 * @param col The color.
	 * @param tp The box type.
	 * @param c Box connections.
	 * @param d True for dashed.
	 */
	public DitaaBoxElement(String t[], String col, TYPE tp, String c, boolean d) {
		this.text = t;
		this.color = col;
		this.type = tp;
		this.dashed = d;
		this.connect = c;
	}

	/**
	 * Returns a string representation of this box.
	 * @param w Width in characters of the box.
	 * @param h Height in characters of the box.
	 * @return The string array.
	 */
	public String[] toString(int w, int h) {
		int py = (h - text.length) / 2;
		int max = 0;
		for (int i=0; i<text.length; i++) {
			if (text[i].length() > max) max = text[i].length();
		}
		int px = (int) (0.5 + (w - 2 - max) / 2.0);
		if (px < 0) px = 0;

		String out[] = new String[h];
		String line = DataSet.repeatString(" ", w);
		for (int i=0; i<out.length; i++) {
			out[i] = line;
		}
		if (type == TYPE.NO_BOX) {
			String s = DataSet.repeatString(" ", px) + this.text[0];
			if (s.length() > w) s = s.trim();
			if (s.length() > w) s = s.substring(0, w);
			s = FileIO.addSpacesAfterAString(s, w);
			if (this.connect.toLowerCase().indexOf("e") >= 0)
				s = FileIO.addSpacesAfterAString(this.text[0], w);
			if (this.connect.toLowerCase().indexOf("w") >= 0)
				s = FileIO.addSpacesBeforeAString(this.text[0], w);
			if (this.connect.toLowerCase().indexOf("s") >= 0) {
				out[out.length-1] = s;
				return out;
			}
			if (this.connect.toLowerCase().indexOf("n") >= 0) {
				out[0] = s;
				return out;
			}
			return new String[] {s};
		}

		String boxLeftRight = "|" + DataSet.repeatString(" ", w - 2) + "|";
		String boxUpDown = "+" + DataSet.repeatString("-", w - 2) + "+";
		String boxLeftRightC = boxLeftRight;
		String boxUpDownC1 = boxUpDown;
		String boxUpDownC2 = boxUpDown;
		if (this.connect.toLowerCase().indexOf("e") >= 0) {
			String con = "+";
			if (this.connect.indexOf("e") >= 0) con = "*";
			boxLeftRightC = con + boxLeftRightC.substring(1);
		}
		if (this.connect.toLowerCase().indexOf("w") >= 0) {
			String con = "+";
			if (this.connect.indexOf("w") >= 0) con = "*";
			boxLeftRightC = boxLeftRightC.substring(0, boxLeftRightC.length() - 1) + con;
		}
		if (this.connect.toLowerCase().indexOf("n") >= 0) {
			String con = "+";
			if (this.connect.indexOf("n") >= 0) con = "*";
			int p = boxUpDownC1.length() / 2;
			boxUpDownC1 = boxUpDownC1.substring(0, p) + con + boxUpDownC1.substring(p + 1);
		}
		if (this.connect.toLowerCase().indexOf("s") >= 0) {
			String con = "+";
			if (this.connect.indexOf("s") >= 0) con = "*";
			int p = boxUpDownC2.length() / 2;
			boxUpDownC2 = boxUpDownC2.substring(0, p) + con + boxUpDownC2.substring(p + 1);
		}
		boolean type = false, color = false;
		for (int i=0; i<h; i++) {
			out[i] = boxLeftRight;
			if (i == h/2) out[i] = boxLeftRightC;
			if (i == 0) out[i] = boxUpDownC1;
			if (i == h-1) out[i] = boxUpDownC2;
			if (i >= py) {
				int index = i - py;
				if (index < text.length)
					out[i] = DataSet.replaceOne(out[i], DataSet.repeatString(" ", text[index].length() + px), DataSet.repeatString(" ", px) + this.text[index], 1);
			}
			if (!type) {
				String s = out[i];
				out[i] = DataSet.replaceOne(out[i], DataSet.repeatString(" ", this.type.id.length()), this.type.id, 1);
				if (!s.equals(out[i])) type = true;
			}
			if (!color) {
				String s = out[i];
				out[i] = DataSet.replaceOne(out[i], DataSet.repeatString(" ", this.color.length()), this.color, 1);
				if (!s.equals(out[i])) color = true;
			}
		}
		if (dashed) out[0] = DataSet.replaceOne(out[0], "-", "=", 1);

		return out;
	}
}
