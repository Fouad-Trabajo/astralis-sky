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
package jparsec.math;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.swing.Icon;
import javax.swing.JLabel;

import be.ugent.caagt.jmathtex.TeXConstants;
import be.ugent.caagt.jmathtex.TeXFormula;
import jparsec.graph.DataSet;

/**
 * A class to create equations using JMathTex library.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class LATEXFormula implements Serializable {

	private static final long serialVersionUID = 1L;

	private int style;
	private int size;
	private String expression;
	private String code;
	private TeXFormula formula;

	/**
	 * ID constant for style display (default recommended).
	 */
	public final static int STYLE_DISPLAY = TeXConstants.STYLE_DISPLAY;
	/**
	 * ID constant for style text.
	 */
	public final static int STYLE_TEXT = TeXConstants.STYLE_TEXT;

	/**
	 * ID constant for big size.
	 */
	public final static int SIZE_BIG = 25;

	/**
	 * ID constant for huge size.
	 */
	public final static int SIZE_HUGE = 50;

	/**
	 * ID constant for normal size.
	 */
	public final static int SIZE_NORMAL = 15;

	/**
	 * ID constant for little size.
	 */
	public final static int SIZE_LITTLE = 10;

	/**
	 * Empty constructor.
	 */
	public LATEXFormula ()
	{
		this.size = SIZE_BIG;
		this.style = STYLE_DISPLAY;
		this.expression = "";
	}

	/**
	 * Constructor for a given equation.
	 * @param expression The equation.
	 */
	public LATEXFormula (String expression)
	{
		this.expression = expression;
		this.code = this.parseExpression();
		this.size = SIZE_BIG;
		this.style = STYLE_DISPLAY;
        formula = new TeXFormula(this.code);
	}

	/**
	 * Constructor for a given equation.
	 * @param expression The equation.
	 * @param size The size. Some constants defined in this class.
	 */
	public LATEXFormula (String expression, int size)
	{
		this.expression = expression;
		this.code = this.parseExpression();
		this.size = size;
		this.style = STYLE_DISPLAY;
        formula = new TeXFormula(this.code);
	}

	/**
	 * Constructor for a given equation.
	 * @param expression The equation.
	 * @param style The style. ID Constants defined in this class.
	 * @param size The size. Some constants defined in this class.
	 */
	public LATEXFormula (String expression, int style, int size)
	{
		this.expression = expression;
		this.code = this.parseExpression();
		this.size = size;
		this.style = style;
        formula = new TeXFormula(this.code);
	}

	/**
	 * Returns the LATEX code of the formula.
	 * @return The code.
	 */
	public String getCode()
	{
		return this.code;
	}
	/**
	 * Returns the style.
	 * @return The style.
	 */
	public int getStyle() {
		return style;
	}
	/**
	 * Returns the size.
	 * @return The size.
	 */
	public int getSize() {
		return size;
	}

	/**
	 * Sets the LATEX code of the formula.
	 * @param code The LATEX code.
	 */
	public void setCode(String code)
	{
		this.code = code;
        formula = new TeXFormula(this.code);
	}

	/**
	 * Returns for formula object.
	 * @return The formula object.
	 */
	public TeXFormula getFormula() {
		return formula;
	}

	/**
	 * Returns the formula as an image.
	 * @return The image.
	 */
	public BufferedImage getAsImage()
	{
        Icon icon = formula.createTeXIcon(this.style, this.size);
        int w = icon.getIconWidth(), h = icon.getIconHeight();
        if (w < 1) w = 1;
        if (h < 1) h = 1;
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        icon.paintIcon(new JLabel(), g2, 0, 0); // component can't be null
        return image;
	}

	/**
	 * Returns the formula as a non-transparent image.
	 * @return The image.
	 */
	public BufferedImage getAsNonTransparentImage() {
        Icon icon = formula.createTeXIcon(this.style, this.size);
        BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        icon.paintIcon(new JLabel(), g2, 0, 0); // component can't be null
        g2.dispose();
        return image;
	}

	/**
	 * Draw the formula to a Graphics device.
	 * @param g The Graphics device.
	 */
	public void draw(Graphics g)
	{
        Icon icon = formula.createTeXIcon(this.style, this.size);
        icon.paintIcon(new JLabel(), g, 0, 0); // component can't be null
	}

	private String parse(String element, String code)
	{
		String le = element.toLowerCase();
		String ue = element.toUpperCase();
		code = DataSet.replaceAll(code, "\\"+le, "jparsec", true);
		code = DataSet.replaceAll(code, "\\"+ue, "jparsec", true);
		code = DataSet.replaceAll(code, le, "jparsec", true);
		code = DataSet.replaceAll(code, ue, "jparsec", true);
		code = DataSet.replaceAll(code, "jparsec", "\\"+le, true);
		return code;
	}

	private String parseExpression()
	{
		String code = this.expression;

		code = this.parse("pi", code);
		code = this.parse("sqrt", code);

		code = DataSet.replaceAll(code, "(", "{", true);
		code = DataSet.replaceAll(code, ")", "}", true);

		code = DataSet.replaceAll(code, " ", "\\nbsp", true);

		code = this.parse("cos", code);
		code = DataSet.replaceAll(code, "\\cos", "\\mathrm{cos}", true);
		code = this.parse("sin", code);
		code = DataSet.replaceAll(code, "\\sin", "\\mathrm{sin}", true);
		code = this.parse("tan", code);
		code = DataSet.replaceAll(code, "\\tan", "\\mathrm{tan}", true);

		code = DataSet.replaceAll(code, "[", "(", true);
		code = DataSet.replaceAll(code, "]", ")", true);

		return code;
	}

	/**
	 * Returns the LATEX code to draw an integral.
	 * @param from Lower bound of the integral.
	 * @param to Upper bound of the integral.
	 * @return The LATEX code.
	 */
	public static String getIntegral(String from, String to)
	{
		String out = "\\int_{"+from+"}^{"+to+"}";
		return out;
	}

	/**
	 * Returns the LATEX code for a fraction.
	 * @param up Upper component of the fraction.
	 * @param down Lower component.
	 * @return The LATEX code.
	 */
	public static String getFraction(String up, String down)
	{
		String out = "\\frac{"+up+"}{"+down+"}";
		return out;
	}

	/**
	 * Returns a given expression between parentesis.
	 * @param in Input expression.
	 * @return The output.
	 */
	public static String getBetweenParentesis(String in)
	{
		String out = "["+in+"]";
		return out;
	}


	private void writeObject(ObjectOutputStream out)
	throws IOException {
		out.writeObject(expression);
		out.writeInt(size);
		out.writeInt(style);
	}
	private void readObject(ObjectInputStream in)
	throws IOException, ClassNotFoundException {
		expression = (String) in.readObject();
		size = in.readInt();
		style = in.readInt();
		this.code = this.parseExpression();
        formula = new TeXFormula(this.code);
 	}
}
