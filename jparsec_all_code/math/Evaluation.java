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

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import jparsec.graph.ChartElement;
import jparsec.graph.ChartSeriesElement;
import jparsec.graph.CreateChart;
import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.util.JPARSECException;

/**
 * A class for evaluating functions into numbers. Functions should be expressed
 * in the Java mathematical notation. This is the only class in JPARSEC
 * that currently requires JRE 1.6 to be used.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Evaluation implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * The script engine manager.
	 */
	private transient ScriptEngineManager mgr;

	/**
	 * The script engine.
	 */
	private transient ScriptEngine jsEngine;

	private String function;
	private String[] variables;
	private transient boolean configured = false;

	/**
	 * Default constructor.
	 */
	public Evaluation()
	{
		mgr = new ScriptEngineManager();
		jsEngine = mgr.getEngineByName("javascript");
	}

	/**
	 * Full constructor.
	 * @param func Function expression.
	 * @param var Variables as a set of name + blank space + expression.
	 */
	public Evaluation(String func, String[] var)
	{
		function = func;
		if (var != null) variables = var.clone();

		mgr = new ScriptEngineManager();
		jsEngine = mgr.getEngineByName("javascript");
	}

	/**
	 * Evaluates user's expression to it's numerical result (symbolic
	 * calculations), using javascript. Requires jre1.6.
	 * @param function The function.
	 * @param variables The variables.
	 * @return The value of the function.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double evaluate(String function, String[] variables)
	throws JPARSECException {
		Evaluation eval = new Evaluation(function, variables);
		return eval.evaluate();
	}

	/**
	 * Evaluates user's expression to it's numerical result (symbolic
	 * calculations), using javascript. Requires jre1.6.
	 * @return The value of the function.
	 * @throws JPARSECException If an error occurs.
	 */
	public double evaluate()
	throws JPARSECException {
		if (configured) return evaluateMathExpression(function, variables);

		String f = configureUserFunction(function);
		configured = true;
		return evaluateMathExpression(f, variables);
	}

	/**
	 * Obtain result of an operation using Java scripting.
	 *
	 * @param expression Math expression in Java format.
	 * @return Result of the operation.
	 * @throws JPARSECException If an error occurs.
	 */
	public double evaluateMathExpression(String expression) throws JPARSECException
	{
		this.function = expression;
		try {
			return (Double) jsEngine.eval("var xx = " + expression + "; " + "xx;");
		} catch (ScriptException ex) {
			throw new JPARSECException(ex);
		}
	}

	/**
	 * Obtain result of an operation using Java scripting.
	 *
	 * @param expression Expression in Java format.
	 * @return Result of the operation.
	 * @throws JPARSECException If an error occurs.
	 */
	public Object evaluateExpression(String expression) throws JPARSECException
	{
		this.function = expression;
		try {
			return jsEngine.eval("var xx = " + expression + "; " + "xx;");
		} catch (ScriptException ex) {
			throw new JPARSECException(ex);
		}
	}

	/**
	 * Obtain result of an operation using Java scripting. The expression must be a
	 * pure Java representation of the operation to perform, no change is made to this
	 * input.
	 *
	 * @param expression Expression in Java format.
	 * @return Result of the operation.
	 * @throws JPARSECException If an error occurs.
	 */
	public Object evaluatePureJavaExpression(String expression) throws JPARSECException
	{
		try {
			return jsEngine.eval(expression);
		} catch (ScriptException ex) {
			throw new JPARSECException(ex);
		}
	}

	/**
	 * Obtain result of an operation using Java scripting.
	 *
	 * @param expression Math expression in Java format.
	 * @param variables Array of variables and values. Format is variable name,
	 *        an space, and it's value.
	 * @return Result of the operation.
	 * @throws JPARSECException If an error occurs.
	 */
	public double evaluateMathExpression(String expression, String variables[]) throws JPARSECException
	{
		this.function = expression;
		this.variables = null;

		if (variables != null) {
			this.variables = variables.clone();
			for (String var : variables) {
				String name = FileIO.getField(1, var, " ", true);
				String value = FileIO.getField(2, var, " ", true);
				jsEngine.put(name, new Double(value));
			}
		}

		try {
			return ((Double) jsEngine.eval("var xx = 0.0 + " + expression + "; " + "xx;")).doubleValue();
		} catch (ScriptException ex) {
			throw new JPARSECException(ex);
		}
	}

	/**
	 * Resets the value of a given variable to another value in this instance.
	 * Later you can use {@linkplain #evaluate()} to evaluate an expression faster
	 * compared to creating the object again.
	 * @param name Variable name.
	 * @param value New value.
	 */
	public void resetVariable(String name, double value) {
		int index = DataSet.getIndexStartingWith(variables, name+" ");
		if (index >= 0) {
			jsEngine.put(name, value);
			variables[index] = name + " " + value;
		}
	}

	/**
	 * Resets the values of the set of variables.
	 * Later you can use {@linkplain #evaluate()} to evaluate an expression faster
	 * compared to creating the object again.
	 * @param variables The variables.
	 */
	public void resetVariables(String variables[]) {
		this.variables = null;
		if (variables != null) {
			this.variables = variables.clone();
			for (String var : variables) {
				String name = FileIO.getField(1, var, " ", true);
				String value = FileIO.getField(2, var, " ", true);
				jsEngine.put(name, new Double(value));
			}
		}
	}

	/**
	 * Obtain result of a function using Java scripting. Function can be
	 * dependent of at most three variables, called x, y, and z.
	 *
	 * @param function Math function f(x, y, z) in Java format.
	 * @param x Value of x.
	 * @param y Value of y.
	 * @param z Value of z.
	 * @return Result of the function.
	 * @throws JPARSECException If an error occurs.
	 */
	public double evaluateMathFunction(String function, double x, double y, double z)
	throws JPARSECException {
		this.function = function;
		this.variables = new String[] {"x "+x, "y "+y, "z "+z};
		jsEngine.put("x", x);
		jsEngine.put("y", y);
		jsEngine.put("z", z);

		try {
			return (Double) jsEngine.eval("var xx = " + function + "; " + "xx;");
		} catch (ScriptException ex) {
			throw new JPARSECException(ex);
		}
	}

	/**
	 * Configures math operations and logical expressions to be able to evaluate
	 * them with Java Scripting. This method is only called by {@linkplain Evaluation#evaluate()},
	 * in the rest of cases it should be called explicitly.
	 *
	 * @param f Function.
	 * @return Same function, ready to evaluate.
	 */
	public static String configureUserFunction(String f)
	{
		f = setLogicalExpressions(f, "<");
		f = setLogicalExpressions(f, ">");

		return f;
	}

	private static String setLogicalExpressions(String function, String logical_operator)
	{
		String f = function;
		int m = f.lastIndexOf(logical_operator);
		if (m > 0)
		{
			do
			{
				int p = -1;
				int level = 0;
				for (int i = m - 1; i >= 0; i--)
				{
					String a = f.substring(i, i + 1);
					if (a.equals(")"))
						level++;
					if (a.equals("("))
					{
						level--;
						if (level < 0)
						{
							p = i;
							break;
						}
					}
				}
				level = 0;
				for (int i = p + 1; i < f.length(); i++)
				{
					String a = f.substring(i, i + 1);
					if (a.equals("("))
						level++;
					if (a.equals(")"))
					{
						level--;
						if (level < 0)
						{
							level = i;
							break;
						}
					}
				}
				String logical = f.substring(p, level + 1);
				f = f.replace(logical, logical.substring(0, logical.length() - 1) + " ? 1:0)");
				m = f.substring(0, m - 1).lastIndexOf(logical_operator);
			} while (m > 0);
		}

		return f;
	}

	/**
	 * This method reads a given file and interpolates the values. The function
	 * string should have tags like /file (file name), /separator (separator
	 * string), and /operation (operation expression).
	 * <P>
	 * To refer to certain column in the file, the operation should have
	 * expressions like COLUMN18/COLUMN02 = column 18 divided by column 2.
	 * COLUMN in uppercase, and the number using two digits. The /operation tag
	 * should have two elements separated by two spaces: the operation
	 * expression for the x array of values, and the operation expression for
	 * the y array. x array should be radial distances in AU, while y array the
	 * corresponding value of the magnitude.
	 * <P>
	 * Default separator (if it is not mencioned) in an space character. File
	 * path should be set starting from Core directory. The file can contain
	 * comments if the line starts with "!".
	 * <P>
	 * This method can be applied to these functions: abundance, velocity,
	 * temperature, and density.
	 *
	 * @param interp_point Interpolation x point.
	 * @param dir Main directory, root for /FILE tag.
	 * @return The adequate value.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public double interpolateFile(double interp_point, String dir) throws JPARSECException
	{
		ArrayList<String> vector = new ArrayList<String>();

		int file_pos = function.indexOf("/file");
		if (file_pos < 0)
			file_pos = function.indexOf("/FILE");
		String file = function.substring(file_pos + 5).trim();
		int tag = file.indexOf("/SEPARATOR");
		if (tag < 0)
			tag = file.indexOf("/separator");
		int tag2 = file.indexOf("/operation");
		if (tag2 < 0)
			tag2 = file.indexOf("/OPERATION");
		if ((tag2 < tag && tag2 > 0) || (tag < 0 && tag2 >= 0))
			tag = tag2;
		if (tag > 0)
			file = file.substring(0, tag).trim();

		String separator = " ";
		int sep_pos = function.indexOf("/separator");
		if (sep_pos < 0)
			sep_pos = function.indexOf("/SEPARATOR");
		if (sep_pos >= 0)
		{
			separator = function.substring(sep_pos + 10).trim();
			tag = separator.indexOf("/FILE");
			if (tag < 0)
				tag = separator.indexOf("/file");
			tag2 = separator.indexOf("/operation");
			if (tag2 < 0)
				tag2 = separator.indexOf("/OPERATION");
			if ((tag2 < tag && tag2 > 0) || (tag < 0 && tag2 >= 0))
				tag = tag2;
			if (tag > 0)
				separator = separator.substring(0, tag).trim();
		}

		int oper_pos = function.indexOf("/operation");
		if (oper_pos < 0)
			sep_pos = function.indexOf("/OPERATION");
		String operation = function.substring(oper_pos + 10).trim();
		tag = operation.indexOf("/SEPARATOR");
		if (tag < 0)
			tag = operation.indexOf("/separator");
		tag2 = operation.indexOf("/file");
		if (tag2 < 0)
			tag2 = operation.indexOf("/FILE");
		if ((tag2 < tag && tag2 > 0) || (tag < 0 && tag2 >= 0))
			tag = tag2;
		if (tag > 0)
			operation = operation.substring(0, tag).trim();

		String fich = file;
		// Lets read the entries
		try {
			// Set URL to input file
			URL url;
			try
			{
				url = new URL(dir + fich);
			} catch (IOException e3)
			{
				throw new JPARSECException(e3);
			}

			URLConnection Connection = url.openConnection();
			InputStream is = Connection.getInputStream();
			BufferedReader dis = new BufferedReader(new InputStreamReader(is));

			String line;
			while ((line = dis.readLine()) != null)
			{

				if (!line.startsWith("!"))
				{
					vector.add(line.trim());
				}

			}

			dis.close();

			int size = vector.size();
			double x_val[] = new double[size];
			double y_val[] = new double[size];

			for (int i = 0; i < size; i++)
			{
				String my_line = vector.get(i);
				int sep = operation.indexOf("  ");
				String function = operation.substring(0, sep).trim();
				int c = function.indexOf("COLUMN");
				while (c >= 0)
				{
					int column = Integer.parseInt(function.substring(c + 6, c + 8));
					String value = FileIO.getField(column, my_line, separator, true);
					function = function.substring(0, c) + value + function.substring(c + 8);
					c = function.indexOf("COLUMN");
				}
				Evaluation eval = new Evaluation(function, null);
				x_val[i] = eval.evaluate();

				function = operation.substring(sep).trim();
				c = function.indexOf("COLUMN");
				while (c >= 0)
				{
					int column = Integer.parseInt(function.substring(c + 6, c + 8));
					String value = FileIO.getField(column, my_line, separator, true);
					function = function.substring(0, c) + value + function.substring(c + 8);
					c = function.indexOf("COLUMN");
				}
				eval = new Evaluation(function, null);
				y_val[i] = eval.evaluate();
			}

			// Now lets re-order the points in x crescent order
			ArrayList<double[]> v = DataSet.sortInCrescent(x_val, y_val, true);
			double ordered_x[] = v.get(0);
			double ordered_y[] = v.get(1);

			// Set interpolation point and obtain result
			//double x_point = interp_point;
			Interpolation interp = new Interpolation(ordered_x, ordered_y, false);
			return interp.splineInterpolation(interp_point);
		} catch (FileNotFoundException e2)
		{
			throw new JPARSECException("file not found " + fich, e2);
		} catch (IOException e3)
		{
			throw new JPARSECException(e3);
		}
	}

	/**
	 * Evaluates a file and returns the operation desired.
	 * <P>
	 * A /FILE tag is needed to set the input file from the working directory.
	 * Also A COLUMNxx (xx being the column number) is convenient. Math
	 * operations are allowed.
	 *
	 * @param dir URL of the working directory which serves as the root directory to get
	 *        the file path.
	 * @return The adequate value.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public double evaluateFromFile(String dir) throws JPARSECException
	{
		double result = 0.0;

		int a = function.toUpperCase().indexOf("/FILE");
		String file_and_column = function.substring(a + 6).trim();
		int f = file_and_column.toUpperCase().indexOf(" ");
		String file = file_and_column.substring(0, f).trim();
		a = file_and_column.toUpperCase().lastIndexOf("COLUMN");

		do
		{
			int column = Integer.parseInt(file_and_column.substring(a + 6, a + 8).trim());
			String line;

			// Set URL to input file
			URL url;
			try
			{
				url = new URL(dir + file);
			} catch (IOException e3)
			{
				throw new JPARSECException(e3);
			}

			// Lets read the catalog entries
			try
			{
				URLConnection Connection = url.openConnection();
				InputStream is = Connection.getInputStream();
				BufferedReader dis = new BufferedReader(new InputStreamReader(is));

				int done = 0;
				while ((line = dis.readLine()) != null && done == 0)
				{

					if (!line.startsWith("!"))
					{
						result = -Double.parseDouble(FileIO.getField(column, line, "  ", true));
						done = 1;
					}
				}
			} catch (FileNotFoundException e0)
			{
				throw new JPARSECException("file/path not found " + dir + file, e0);
			} catch (IOException e1)
			{
				throw new JPARSECException(e1);
			}

			file_and_column = file_and_column.substring(0, a) + Double.toString(result).trim();
			a = file_and_column.toUpperCase().lastIndexOf("COLUMN");

		} while (a >= 0);

		String operation = file_and_column.substring(f).trim();
		Evaluation eval = new Evaluation(operation, null);
		return eval.evaluate();
	}

	/**
	 * Evaluates a file and returns the operation desired.
	 * <P>
	 * A /FILE tag is needed to set the input file from the working directory.
	 * Also A COLUMNxx (xx being the column number) is convenient. Math
	 * operations are allowed.
	 *
	 * @param comment Any line starting with any of the characters in this string will be skipped.
	 * @param sep The separator for the different fields in the COLUMN tag.
	 * @return The adequate value.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public double evaluateFromFile(String comment, String sep) throws JPARSECException
	{
		double result = 0.0;

		int a = function.toUpperCase().indexOf("/FILE");
		String file_and_column = function.substring(a + 6).trim();
		int f = file_and_column.toUpperCase().indexOf(" ");
		String file = file_and_column.substring(0, f).trim();
		a = file_and_column.toUpperCase().lastIndexOf("COLUMN");

		do
		{
			int column = Integer.parseInt(file_and_column.substring(a + 6, a + 8).trim());
			String line;

			// Set URL to input file
			URL url;
			try
			{
				url = new URL(file);
			} catch (IOException e3)
			{
				throw new JPARSECException(e3);
			}

			// Lets read the catalog entries
			try
			{
				URLConnection Connection = url.openConnection();
				InputStream is = Connection.getInputStream();
				BufferedReader dis = new BufferedReader(new InputStreamReader(is));

				int done = 0;
				while ((line = dis.readLine()) != null && done == 0)
				{
					line = line.trim();
					if (!comment.contains(line.substring(0, 1)))
					{
						result = -Double.parseDouble(FileIO.getField(column, line, sep, true));
						done = 1;
					}
				}
			} catch (FileNotFoundException e0)
			{
				throw new JPARSECException("file/path not found " + file, e0);
			} catch (IOException e1)
			{
				throw new JPARSECException(e1);
			}

			file_and_column = file_and_column.substring(0, a) + Double.toString(result).trim();
			a = file_and_column.toUpperCase().lastIndexOf("COLUMN");

		} while (a >= 0);

		String operation = file_and_column.substring(f).trim();
		Evaluation eval = new Evaluation(operation, null);
		double val = eval.evaluate();

		return val;
	}

	private static Evaluation ev;

	/**
	 * Transforms a NativeArray (Java Script) to string array.
	 *
	 * @param n NativeArray (sun.org.mozilla.javascript.internal.NativeArray).
	 * @return String array
	 * @throws JPARSECException In case the input object is not a NativeArray or
	 * cannot be read at all.
	 */
	public static String[] nativeArrayToStringArray(Object n) throws JPARSECException
	{
		if (ev == null) ev = new Evaluation();
		ev.function = "n.length;";
		ev.jsEngine.put("n", n);
		double out;
		try {
			out = (Double) ev.jsEngine.eval(ev.function);
		} catch (Exception exc) {
			throw new JPARSECException("Cannot obtain native array size", exc);
		}

		String arr[] = new String[(int) out];

		for (int i = 0; i < arr.length; i++)
		{
			try {
				arr[i] = ev.jsEngine.eval("n["+i+"];").toString();
			} catch (Exception exc) {
				throw new JPARSECException("Cannot eval native array value for index "+i, exc);
			}
		}

		return arr;
	}

	/**
	 * Transforms a NativeArray (Java Script) to double array.
	 *
	 * @param n NativeArray (sun.org.mozilla.javascript.internal.NativeArray).
	 * @return Double array.
	 * @throws JPARSECException In case the input object is not a NativeArray or
	 * cannot be read at all.
	 */
	public static double[] nativeArrayToDoubleArray(Object n) throws JPARSECException
	{
		if (ev == null) ev = new Evaluation();
		ev.function = "n.length;";
		ev.jsEngine.put("n", n);
		double out;
		try {
			out = (Double) ev.jsEngine.eval(ev.function);
		} catch (Exception exc) {
			throw new JPARSECException("Cannot obtain native array size", exc);
		}

		double arr[] = new double[(int) out];

		for (int i = 0; i < arr.length; i++)
		{
			try {
				arr[i] = (Double) ev.jsEngine.eval("n["+i+"];");
			} catch (Exception exc) {
				throw new JPARSECException("Cannot eval native array value for index "+i, exc);
			}
		}

		return arr;
	}

	/**
	 * Returns the evaluation function.
	 * @return The function.
	 */
	public String getFunction() {
		return function;
	}
	/**
	 * Return the variables set.
	 * @return The variables
	 */
	public String[] getVariables() {
		return variables;
	}

	/**
	 * Returns a chart showing the evaluated data.
	 * @param var The name of the variable to modify, for instance "x".
	 * @param min Minimum value of the variable.
	 * @param max Maximum value of the variable.
	 * @param np Number of points.
	 * @return The chart.
	 * @throws JPARSECException If an error occurs.
	 */
	public CreateChart getChart(String var, double min, double max, int np) throws JPARSECException {
		double dy[] = new double[np], dx[] = new double[dy.length];
		String[] vars = this.variables.clone();
		int index = DataSet.getIndexStartingWith(variables, var+" ");
		for (int i=0; i<dy.length; i++) {
			dx[i] = min + (max - min) * i / (dy.length - 1.0);
			variables[index] = var + " " + dx[i];
			dy[i] = this.evaluate();
		}
		ChartSeriesElement chartSeries1 = new ChartSeriesElement(dx,
				dy, null, null, "Y", true, Color.RED, ChartSeriesElement.SHAPE_CIRCLE,
				ChartSeriesElement.REGRESSION.NONE);

		ChartSeriesElement series[] = new ChartSeriesElement[] {chartSeries1};
		ChartElement chart = new ChartElement(series, ChartElement.TYPE.XY_CHART,
				ChartElement.SUBTYPE.XY_SCATTER,
				"X, Y",
				"X", "Y", false, 800, 600);
		CreateChart ch = new CreateChart(chart);
		variables = vars;
		return ch;
	}
}
