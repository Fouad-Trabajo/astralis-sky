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

import java.io.Serializable;
import java.util.ArrayList;

import jparsec.util.JPARSECException;

/**
 * A class to find roots using Newton-Raphson and midpoint methods.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Root implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Empty constructor.
	 */
	public Root()
	{
		this.functionImaginaryTermDerivative = this.functionImaginaryTerm =
			this.functionRealTermDerivative = this.functionRealTerm = "";
	}

	/**
	 * Constructor for a real function.
	 *
	 * @param realf Real function.
	 */
	public Root(String realf)
	{
		this.functionRealTerm = realf;
	}

	/**
	 * Constructor for a real function.
	 *
	 * @param realf Real function.
	 * @param drealf Real function derivative.
	 */
	public Root(String realf, String drealf)
	{
		this.functionRealTerm = realf;
		this.functionRealTermDerivative = drealf;
	}

	/**
	 * Constructor for a complex function.
	 *
	 * @param realf Real part.
	 * @param drealf Real part derivative.
	 * @param imgf Imaginary part.
	 * @param dimgf Imaginary part derivative.
	 */
	public Root(String realf, String drealf, String imgf, String dimgf)
	{
		this.functionRealTerm = realf;
		this.functionRealTermDerivative = drealf;
		this.functionImaginaryTermDerivative = dimgf;
		this.functionImaginaryTerm = imgf;
	}

	/**
	 * Hold the real part of the function f(x) in Java format.
	 */
	public String functionRealTerm;

	/**
	 * Hold the imaginary part of the function f(x) in Java format.
	 */
	public String functionImaginaryTerm;

	/**
	 * Hold the real derivative of the function f(x) in Java format.
	 */
	public String functionRealTermDerivative;

	/**
	 * Hold the imaginary derivative of the function f(x) in Java format.
	 */
	public String functionImaginaryTermDerivative;

	/**
	 * Evaluates a function of two variables, x and y.
	 *
	 * @param f Function expression.
	 * @param x Value of x.
	 * @param y Value of y.
	 * @return Function result.
	 * @throws JPARSECException If an error occurs.
	 */
	public double evaluateFunction(String f, double x, double y)
	throws JPARSECException {
		if (f == null) return 0; // For the imaginary term when working only with real functions
		if (f.equals("")) throw new JPARSECException("undefined function.");
		return Evaluation.evaluate(f, new String[] {"x "+x, "y "+y});
	}

	/**
	 * Obtains a root by applying Newton Raphson. From Basic Scientific
	 * Subroutines, F. R. Ruckdeschel.
	 *
	 * @param E Tolerance (max. error desired). Must be slightly greater than
	 *        0.0.
	 * @param x0 Real part of initial search point.
	 * @param y0 Imaginary part of initial search point, only if the function
	 *        has an imaginary compound.
	 * @param limit_of_iterations Maximum number of iterations allowed.
	 * @return Array with real part, imaginary part (of the root), and number of
	 *         iterations used, or null if the limit of iterations is too low.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[] getRootByNewtonRaphson(double E, double x0, double y0, int limit_of_iterations)
	throws JPARSECException {
		// Number of iterations
		int k = 0;

		double dif = E + 1.0;
		do
		{
			k++;
			double x = x0;
			double y = y0;
			double u = evaluateFunction(functionRealTerm, x, y);
			double v = evaluateFunction(functionImaginaryTerm, x, y);
			double u1 = evaluateFunction(functionRealTermDerivative, x, y);
			double u2 = evaluateFunction(functionImaginaryTermDerivative, x, y);
			double a = u1 * u1 + u2 * u2;
			if (a != 0)
			{
				x = x0 + (v * u2 - u * u1) / a;
				y = y0 - (v * u1 + u * u2) / a;
				dif = Math.sqrt((x - x0) * (x - x0) + (y - y0) * (y - y0));
				x0 = x;
				y0 = y;
			} else
			{
				// The derivatives are zero. We have to change slightly the
				// initial search point
				dif = E + 1.0;
				x0 = x0 + E;
			}
		} while (dif > E && k < limit_of_iterations);

		if (k >= limit_of_iterations) return null;
		return new double[] { x0, y0, k };
	}

	/**
	 * Obtain the roots of a real function, using a bisection like algorithm. No
	 * derivative is necessary.
	 *
	 * @param E Precision in the roots.
	 * @param xi Initial search point.
	 * @param xf End search point.
	 * @param step Step between search points.
	 * @return All the roots found.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[] getRootsRealFunction(double E, double xi, double xf, double step)
	throws JPARSECException {
		ArrayList<Double> v = new ArrayList<Double>();
		double y0 = this.evaluateFunction(this.functionRealTerm, xi, 0.0);
		if (y0 == 0.0)
			v.add(new Double(xi));
		for (double x = xi + step; x <= xf; x = x + step)
		{
			double y = this.evaluateFunction(this.functionRealTerm, x, 0.0);
			if (y == 0.0)
			{
				v.add(new Double(x));
			} else
			{
				double ratio = y0 / y;
				if (ratio < 0.0)
				{
					v.add(new Double(this.getPreciseRoot(x - step, x, this.functionRealTerm, E)));
				}
			}
			y0 = y;
		}

		double roots[] = new double[v.size()];
		for (int i = 0; i < v.size(); i++)
		{
			roots[i] = (v.get(i)).doubleValue();
		}

		return roots;
	}

	private double getPreciseRoot(double xi, double xf, String f, double E)
	throws JPARSECException {
		double x = 0.0, y = E;
		double yi = this.evaluateFunction(f, xi, 0.0);
		double yf = this.evaluateFunction(f, xf, 0.0);
		do
		{
			x = (xi + xf) * 0.5;
			y = this.evaluateFunction(f, x, 0.0);
			double ratioi = y / yi;
			double ratiof = y / yf;
			if (ratioi > 0.0)
			{
				xi = x;
				yi = y;
			} else
			{
				xf = x;
			}
			if (ratiof > 0.0)
			{
				xf = x;
				yf = y;
			} else
			{
				xi = x;
			}
		} while (Math.abs(y) > E);
		return x;
	}

	/**
	 * Transforms root result to string representation taking into account the
	 * reached precision. Usually the original root double precision value
	 * will be much better than the rounded value, so this method in not very
	 * useful.
	 *
	 * @param root Root value.
	 * @param error Maximum error.
	 * @return Root value properly rounded.
	 */
	public String toString(double root, double error)
	{
		MeasureElement me = new MeasureElement(root, error, "");
		return me.toString().trim();
	}
}
