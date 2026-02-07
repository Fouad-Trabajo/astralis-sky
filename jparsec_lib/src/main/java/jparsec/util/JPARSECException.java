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
package jparsec.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.util.StringTokenizer;

import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.time.AstroDate;
import jparsec.util.Logger.LEVEL;

/**
 * An exception is thrown if the caller attempts to pass an invalid value,
 * or the results of a calculation generate an invalid operation.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class JPARSECException extends Exception implements Serializable
{
	private static final long serialVersionUID = 1L;

	// Exception fields
	private String header;
	private String description;
	private Throwable exception;

	// Warning elements
	private static String warnings = "";
	private static boolean treatWarningsAsErrors = false;

	/** Set to true to disable the processing of warning messages. Default value is false. */
	public static boolean DISABLE_WARNINGS = false;

	/**
	 * Returns warnings.
	 *
	 * @return Description.
	 */
	public static String getWarnings()
	{
		return warnings;
	}

	/**
	 * Set the value of the warnings string.
	 *
	 * @param warns Warnings, set to empty string to reset.
	 */
	public static void setWarnings(String warns)
	{
		if (warns == null) warns = "";
		warnings = warns;
	}

	/**
	 * Adds a warning. Care should be taken in order to avoid sending too much
	 * warnings, that could freeze the execution.
	 *
	 * @param warning Warning to add.
	 * @throws JPARSECException Thrown if {@linkplain JPARSECException#treatWarningsAsErrors} is set to true.
	 */
	public static void addWarning(String warning) throws JPARSECException
	{
		if (DISABLE_WARNINGS) return;

		String date = "(" + (new AstroDate()).toString() + ") ";
		String header = JPARSECException.getLastMethodName() + ": ";
		String w = header + warning;

		String search = ") " + w + FileIO.getLineSeparator(); // end of date to next entry sep
		if (warnings.indexOf(search) < 0) {
			warnings += date + w + FileIO.getLineSeparator();
			Logger.log(LEVEL.WARNING, warning, JPARSECException.getLastMethodName());
		} else {
			// Substitute the repeated warning with the one updated, with latest date
			String a[] = DataSet.toStringArray(warnings, FileIO.getLineSeparator());
			int i = DataSet.getIndexContaining(a, w);
			if (i >= 0) warnings = DataSet.replaceOne(warnings, a[i], date + w, 1);
		}

		if (treatWarningsAsErrors)
			throw new JPARSECException("(Warning) " + getWarnings());
	}

	/**
	 * Sets whether to treat warning as errors or not.
	 *
	 * @param warningsAsErrors True for treating warnings as errors.
	 */
	public static void treatWarningsAsErrors(boolean warningsAsErrors)
	{
		treatWarningsAsErrors = warningsAsErrors;
	}

	/**
	 * Returns whether warnings are considered as errors or not.
	 * @return True or false.
	 */
	public static boolean isTreatWarningsAsErrors()
	{
		return treatWarningsAsErrors;
	}

	/**
	 * Clear warnings.
	 */
	public static void clearWarnings()
	{
		warnings = "";
	}

	/**
	 * Returns error description.
	 *
	 * @return Description.
	 */
	public String getMessage()
	{
		return description;
	}

	/**
	 * Sets error description.
	 * @param newDescription New description for the error.
	 */
	public void setDescription(String newDescription)
	{
		description = newDescription;
	}

	/**
	 * Returns header.
	 *
	 * @return Header.
	 */
	public String getHeader()
	{
		return header;
	}

	/**
	 * Sets the header.
	 *
	 * @param newHeader The new header.
	 */
	public void setHeader(String newHeader)
	{
		header = newHeader;
	}

	/**
	 * Returns the stack trace.
	 * @return The stack trace message.
	 */
	public String getTrace()
	{
		return JPARSECException.getTrace(this.getStackTrace());
	}

	/**
	 * Returns the stack trace message.
	 * @param trace The stack trace.
	 * @return The stack trace message.
	 */
	public static String getTrace(StackTraceElement trace[])
	{
		String out = "";
		for (int i = 0; i < trace.length; i++)
		{
			out += trace[i].toString() + FileIO.getLineSeparator();
		}
		return out;
	}

	/**
	 * Returns the throwable.
	 */
	public Throwable getCause()
	{
		return exception;
	}

	/**
	 * Returns the details of the error.
	 * @return Details, or null.
	 */
	public String getStackTraceDetails() {
		if (exception == null) return null;

		Writer sw = new StringWriter();
		PrintWriter pr = new PrintWriter(sw);
		exception.printStackTrace(pr);
		return sw.toString();
	}
	/**
	 * Constructs the appropriate exception with the specified string.
	 *
	 * @param message String exception message.
	 */
	public JPARSECException(String message)
	{
		String header = JPARSECException.getLastMethodName() + ": ";
		setHeader(header);
		setDescription(message);
	}

	/**
	 * Constructs the appropriate exception with the specified options.
	 *
	 * @param exc Exception thrown.
	 */
	public JPARSECException(Throwable exc)
	{
		this(null, exc);
	}

	/**
	 * Constructs the appropriate exception with the specified options.
	 *
	 * @param message String exception message.
	 * @param exc Exception thrown.
	 */
	public JPARSECException(String message, Throwable exc)
	{
		Exception jpe;
		if (message == null) {
			jpe = new Exception(exc);
		} else {
			jpe = new Exception(message, exc);
		}
		exception = jpe.getCause();

		String header = JPARSECException.getLastMethodName() + ": ";
		setHeader(header);
		setDescription(jpe.getLocalizedMessage());
		this.setStackTrace(jpe.getStackTrace());
	}

	/**
	 * Shows the exception message.
	 *
	 * @param ve Exception object.
	 */
	public static void showException(JPARSECException ve)
	{
		String pid = ManagementFactory.getRuntimeMXBean().getName();
		System.err.println("Exception in " + pid + " at "+ ve.getHeader() + ve.getMessage());
		System.err.println(ve.getTrace());
	}

	/**
	 * Shows the warning messages.
	 */
	public static void showWarnings()
	{
		String warnings = getWarnings();
		if (!warnings.equals(""))
		{
			System.out.println(warnings);
		}
	}

	/**
	 * Gives an unique code for certain error or warning depending on the string
	 * description message. It can be used to avoid confusions when constructing
	 * warnings and exceptions by giving only a message description, since such
	 * code can be cached with this method before launching certain exception
	 * with a given message. Also it allows to avoid duplication of warnings.
	 *
	 * @param error_or_warning Description message.
	 * @return Code for the error/warning.
	 */
	public static int getCode(String error_or_warning)
	{
		int code = 0;

		for (int i = 0; i < error_or_warning.length(); i++)
		{
			char c = error_or_warning.charAt(i);

			code += c;
		}
		return code;
	}

	/**
	 * Returns the name of the method which is being executed.
	 *
	 * @return Method name.
	 */
	public static String getCurrentMethodName()
	{
		String stackTrace = JPARSECException.getCurrentTrace();
		StringTokenizer tok = new StringTokenizer(stackTrace, "\n");
		String l = tok.nextToken(); // java.lang.Throwable
		l = tok.nextToken(); // 'at ...getCurrentTrace
		l = tok.nextToken(); // 'at ...getCurrentMethodName
		l = tok.nextToken(); // caller
		// Parse line 3
		tok = new StringTokenizer(l.trim(), " <(");
		String t = tok.nextToken(); // 'at'
		t = tok.nextToken(); // '...<caller to getCurrentRoutine>'

		return t;
	}

	/**
	 * Returns the trace of a program execution at this moment.
	 * @return The trace.
	 */
	public static String getCurrentTrace() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(baos);
		(new Throwable()).printStackTrace(pw);
		pw.flush();
		String stackTrace = baos.toString();
		pw.close();
		return stackTrace;
	}

	/**
	 * Returns the name of the method that called the one in execution.
	 *
	 * @return Method name.
	 */
	public static String getLastMethodName()
	{
		try {
			String stackTrace = JPARSECException.getCurrentTrace();
			StringTokenizer tok = new StringTokenizer(stackTrace, "\n");
			String l = tok.nextToken(); // java.lang.Throwable
			l = tok.nextToken(); // 'at ...getCurrentTrace
			l = tok.nextToken(); // 'at ...getLastMethodName
			l = tok.nextToken(); // 'at ...addWarning
			l = tok.nextToken(); // caller
			// Parse line 3
			tok = new StringTokenizer(l.trim(), " <(");
			String t = tok.nextToken(); // 'at'
			t = tok.nextToken(); // '...<caller to getCurrentRoutine>'
			return t;
		} catch (Exception exc) {
			return ""; // Happens in Android
		}
	}

	/**
	 * Shows the exception message.
	 */
	public void showException()
	{
		System.err.println("Exception in " + this.getHeader() + this.getMessage());
		this.printStackTrace();
	}

	/**
	 * Transforms a set of stack trace elements into an string array.
	 * @param trace The input trace.
	 * @return The output array.
	 */
	public static String[] toStringArray(StackTraceElement[] trace)
	{
		String out[] = new String[trace.length];
		for (int i = 0; i < trace.length; i++)
		{
			out[i] = trace[i].toString();
		}
		return out;
	}
}
