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

import java.io.*;
import java.util.Arrays;

import jparsec.graph.chartRendering.AWTGraphics;

/**
 * A class to support basic strokes, but adding serialization support. The set of strokes are (dotted
 * points or lines) have widths of 1.5 pixels (normal strokes) or 0.5 (thin ones). A method in
 * class {@linkplain AWTGraphics} allows to convert a JPARSEC stroke into a BasicStroke Java
 * object and the opposite.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class JPARSECStroke implements Serializable {

	private static final long serialVersionUID = 1L;

	private float lineWidth;
	private int endCap;
	private int lineJoin;
	private float miterLimit;
	private float[] dashArray;
	private float dashPhase;

	/**
	 * Constructor for a stroke.
	 * @param s Stroke.
	 */
	private JPARSECStroke (JPARSECStroke s)
	{
		this.lineWidth = s.getLineWidth();
		this.endCap = s.getEndCap();
		this.lineJoin = s.getLineJoin();
		this.miterLimit = s.getMiterLimit();
		this.dashArray = s.getDashArray().clone();
		this.dashPhase = s.getDashPhase();
	}

	/**
	 * Constructor for a stroke with a given line width.
	 * @param s Stroke.
	 * @param w Line width.
	 */
	public JPARSECStroke (JPARSECStroke s, float w)
	{
		this(s);
		float array[] = dashArray;
		if (array.length == 2 && array[0] < w) array[0] = w;
		lineWidth = w;
	}

	/**
	 * Constructor for a stroke with a given pattern.
	 * @param s Stroke.
	 * @param pattern Pattern.
	 * @param phase Phase.
	 */
	public JPARSECStroke (JPARSECStroke s, float[] pattern, float phase)
	{
		this(s);
		this.dashArray = pattern;
		this.dashPhase = phase;
	}

	/**
	 * The constructor.
	 * @param lineWidth Line width.
	 * @param endCap End cap.
	 * @param lineJoin Line join.
	 * @param miterLimit Miter limit.
	 * @param dashArray Dash array.
	 * @param dashPhase Dash phase
	 */
	public JPARSECStroke(float lineWidth, int endCap, int lineJoin,
			float miterLimit, float[] dashArray, float dashPhase) {
        this.lineWidth = lineWidth;
        this.endCap = endCap;
        this.lineJoin = lineJoin;
        this.miterLimit = miterLimit;
        this.dashArray = null;
        if (dashArray != null) this.dashArray = dashArray.clone();
        this.dashPhase = dashPhase;
	}

	private static final float SIZE = 1.5f;
	private static final float SIZE_THIN = 0.5f;
	private static final float SIZE_THICK = 4f;
	private static final int JOIN_ROUND = 1;
	private static final int CAP_BUTT = 0;

	/**
	 * Stroke to represent lines as highly spaced points.
	 */
	public static final JPARSECStroke STROKE_POINTS_HIGH_SPACE = new JPARSECStroke(SIZE, CAP_BUTT,
			JOIN_ROUND, 1.0f, new float[]
			{ SIZE, 12.0f }, 0.0f);

	/**
	 * Stroke to represent lines as medium spaced points.
	 */
	public static final JPARSECStroke STROKE_POINTS_MEDIUM_SPACE = new JPARSECStroke(SIZE, CAP_BUTT,
			JOIN_ROUND, 1.0f, new float[]
			{ SIZE, 6.0f }, 0.0f);

	/**
	 * Stroke to represent lines as low spaced points.
	 */
	public static final JPARSECStroke STROKE_POINTS_LOW_SPACE = new JPARSECStroke(SIZE, CAP_BUTT,
			JOIN_ROUND, 1.0f, new float[]
			{ SIZE, 4.0f }, 0.0f);

	/**
	 * Stroke to represent lines as medium spaced short lines.
	 */
	public static final JPARSECStroke STROKE_LINES_SHORT = new JPARSECStroke(SIZE, CAP_BUTT,
			JOIN_ROUND, 1.0f, new float[]
			{ 2f, 6.0f }, 0.0f);

	/**
	 * Stroke to represent lines as medium spaced medium lines.
	 */
	public static final JPARSECStroke STROKE_LINES_MEDIUM = new JPARSECStroke(SIZE, CAP_BUTT,
			JOIN_ROUND, 1.0f, new float[]
			{ 6f, 6.0f }, 0.0f);

	/**
	 * Stroke to represent lines as medium spaced large lines.
	 */
	public static final JPARSECStroke STROKE_LINES_LARGE = new JPARSECStroke(SIZE, CAP_BUTT,
			JOIN_ROUND, 1.0f, new float[]
			{ 10f, 6.0f }, 0.0f);

	/**
	 * Stroke to represent lines as they are.
	 */
	public static final JPARSECStroke STROKE_DEFAULT_LINE = new JPARSECStroke(SIZE, CAP_BUTT,
			JOIN_ROUND, 1.0f, new float[] { 10f, 0f }, 10.0f);

	/**
	 * Stroke to represent lines as highly spaced points.
	 */
	public static final JPARSECStroke STROKE_POINTS_HIGH_SPACE_THIN = new JPARSECStroke(SIZE_THIN, CAP_BUTT,
			JOIN_ROUND, 1.0f, new float[]
			{ SIZE_THIN, 12.0f }, 0.0f);

	/**
	 * Stroke to represent lines as medium spaced points.
	 */
	public static final JPARSECStroke STROKE_POINTS_MEDIUM_SPACE_THIN = new JPARSECStroke(SIZE_THIN, CAP_BUTT,
			JOIN_ROUND, 1.0f, new float[]
			{ SIZE_THIN, 6.0f }, 0.0f);

	/**
	 * Stroke to represent lines as low spaced points.
	 */
	public static final JPARSECStroke STROKE_POINTS_LOW_SPACE_THIN = new JPARSECStroke(SIZE_THIN, CAP_BUTT,
			JOIN_ROUND, 1.0f, new float[]
			{ SIZE_THIN, 4.0f }, 0.0f);

	/**
	 * Stroke to represent lines as medium spaced short lines.
	 */
	public static final JPARSECStroke STROKE_LINES_SHORT_THIN = new JPARSECStroke(SIZE_THIN, CAP_BUTT,
			JOIN_ROUND, 1.0f, new float[]
			{ 2f, 6.0f }, 0.0f);

	/**
	 * Stroke to represent lines as medium spaced medium lines.
	 */
	public static final JPARSECStroke STROKE_LINES_MEDIUM_THIN = new JPARSECStroke(SIZE_THIN, CAP_BUTT,
			JOIN_ROUND, 1.0f, new float[]
			{ 6f, 6.0f }, 0.0f);

	/**
	 * Stroke to represent lines as medium spaced large lines.
	 */
	public static final JPARSECStroke STROKE_LINES_LARGE_THIN = new JPARSECStroke(SIZE_THIN, CAP_BUTT,
			JOIN_ROUND, 1.0f, new float[]
			{ 10f, 6.0f }, 0.0f);

	/**
	 * Stroke to represent lines as they are.
	 */
	public static final JPARSECStroke STROKE_DEFAULT_LINE_THIN = new JPARSECStroke(SIZE_THIN, CAP_BUTT,
			JOIN_ROUND, 1.0f, new float[]
			{ 10f, 0f }, 0.0f);

	/**
	 * Stroke to represent lines as highly spaced points.
	 */
	public static final JPARSECStroke STROKE_POINTS_HIGH_SPACE_THICK = new JPARSECStroke(SIZE_THICK, CAP_BUTT,
			JOIN_ROUND, 1.0f, new float[]
			{ SIZE_THIN, 12.0f }, 0.0f);

	/**
	 * Stroke to represent lines as medium spaced points.
	 */
	public static final JPARSECStroke STROKE_POINTS_MEDIUM_SPACE_THICK = new JPARSECStroke(SIZE_THICK, CAP_BUTT,
			JOIN_ROUND, 1.0f, new float[]
			{ SIZE_THIN, 6.0f }, 0.0f);

	/**
	 * Stroke to represent lines as low spaced points.
	 */
	public static final JPARSECStroke STROKE_POINTS_LOW_SPACE_THICK = new JPARSECStroke(SIZE_THICK, CAP_BUTT,
			JOIN_ROUND, 1.0f, new float[]
			{ SIZE_THIN, 4.0f }, 0.0f);

	/**
	 * Stroke to represent lines as medium spaced short lines.
	 */
	public static final JPARSECStroke STROKE_LINES_SHORT_THICK = new JPARSECStroke(SIZE_THICK, CAP_BUTT,
			JOIN_ROUND, 1.0f, new float[]
			{ 2f, 6.0f }, 0.0f);

	/**
	 * Stroke to represent lines as medium spaced medium lines.
	 */
	public static final JPARSECStroke STROKE_LINES_MEDIUM_THICK = new JPARSECStroke(SIZE_THICK, CAP_BUTT,
			JOIN_ROUND, 1.0f, new float[]
			{ 6f, 6.0f }, 0.0f);

	/**
	 * Stroke to represent lines as medium spaced large lines.
	 */
	public static final JPARSECStroke STROKE_LINES_LARGE_THICK = new JPARSECStroke(SIZE_THICK, CAP_BUTT,
			JOIN_ROUND, 1.0f, new float[]
			{ 10f, 6.0f }, 0.0f);

	/**
	 * Stroke to represent lines as they are.
	 */
	public static final JPARSECStroke STROKE_DEFAULT_LINE_THICK = new JPARSECStroke(SIZE_THICK, CAP_BUTT,
			JOIN_ROUND, 1.0f, new float[]
			{ 10f, 0f }, 0.0f);
	/**
	 * Hold the names of the line strokes.
	 */
	public static final String STROKES_TYPES[] = new String[] {"Continuum", "Continuum thin", "Line large", "Line medium",
		"Line short", "Points short", "Points medium", "Points large", "No line"};
	/**
	 * Hold types of line strokes.
	 */
	public static final JPARSECStroke[] STROKES = new JPARSECStroke[] {STROKE_DEFAULT_LINE, STROKE_DEFAULT_LINE_THIN, STROKE_LINES_LARGE,
		STROKE_LINES_MEDIUM, STROKE_LINES_SHORT, STROKE_POINTS_LOW_SPACE, STROKE_POINTS_MEDIUM_SPACE, STROKE_POINTS_HIGH_SPACE};

	private void writeObject(ObjectOutputStream out)
	throws IOException {
		out.writeFloat(this.lineWidth);
		out.writeInt(this.endCap);
		out.writeInt(this.lineJoin);
		out.writeFloat(this.miterLimit);
		out.writeObject(this.dashArray);
		out.writeFloat(this.dashPhase);
	}
	private void readObject(ObjectInputStream in)
	throws IOException, ClassNotFoundException {
        float width = in.readFloat();
        int cap = in.readInt();
        int join = in.readInt();
        float miterLimit = in.readFloat();
        float[] dash = (float[]) in.readObject();
        float dashPhase = in.readFloat();

        this.lineWidth = width;
        this.endCap = cap;
        this.lineJoin = join;
        this.miterLimit = miterLimit;
        this.dashArray = dash;
        this.dashPhase = dashPhase;
 	}
	/**
	 * Clones this instance.
	 */
	@Override
	public JPARSECStroke clone()
	{
        return new JPARSECStroke(this.lineWidth, this.endCap, this.lineJoin, this.miterLimit, this.dashArray, this.dashPhase);
	}
	/**
	 * Returns true if the input object is equals to this stroke.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof JPARSECStroke)) return false;

		JPARSECStroke that = (JPARSECStroke) o;

		if (Float.compare(that.lineWidth, lineWidth) != 0) return false;
		if (endCap != that.endCap) return false;
		if (lineJoin != that.lineJoin) return false;
		if (Float.compare(that.miterLimit, miterLimit) != 0) return false;
		if (Float.compare(that.dashPhase, dashPhase) != 0) return false;

		return Arrays.equals(dashArray, that.dashArray);
	}

	@Override
	public int hashCode() {
		int result = (lineWidth != +0.0f ? Float.floatToIntBits(lineWidth) : 0);
		result = 31 * result + endCap;
		result = 31 * result + lineJoin;
		result = 31 * result + (miterLimit != +0.0f ? Float.floatToIntBits(miterLimit) : 0);
		result = 31 * result + (dashArray != null ? Arrays.hashCode(dashArray) : 0);
		result = 31 * result + (dashPhase != +0.0f ? Float.floatToIntBits(dashPhase) : 0);
		return result;
	}

	/**
	 * Returns the dash array.
	 * @return Dash array.
	 */
	public float[] getDashArray() {
		return this.dashArray;
	}

	/**
	 * Returns the dash phase.
	 * @return Dash phase.
	 */
	public float getDashPhase() {
		return this.dashPhase;
	}

	/**
	 * Returns the miter limits.
	 * @return Miter limit.
	 */
	public float getMiterLimit() {
		return this.miterLimit;
	}

	/**
	 * Returns the line join.
	 * @return Line join.
	 */
	public int getLineJoin() {
		return this.lineJoin;
	}

	/**
	 * Returns the end cap.
	 * @return End cap.
	 */
	public int getEndCap() {
		return this.endCap;
	}

	/**
	 * Returns the line width.
	 * @return Line width.
	 */
	public float getLineWidth() {
		return this.lineWidth;
	}

	/**
	 * Returns if this stroke represents a continuous line.
	 * More exactly, it returns true if the dash array
	 * has length 2 and the second value is 0.
	 * @return True or false.
	 */
	public boolean isContinuousLine() {
		if (dashArray == null) return true;
		if (dashArray.length != 2) return false;
		return this.dashArray[1] == 0;
	}
}
