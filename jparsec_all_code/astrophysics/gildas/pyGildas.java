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

import java.io.File;

import jparsec.graph.DataSet;
import jparsec.io.ApplicationLauncher;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.io.WriteFile;
import jparsec.util.Configuration;
import jparsec.util.JPARSECException;

/**
 * This class allows GILDAS (or any other program) to be run and commanded
 * in background. To simply launch applications use instead {@linkplain ApplicationLauncher} class.<BR>
 * When commanding GILDAS, there are two possibilities to use a given script.
 * One is to provide the contents of the script file as a string array. In this case
 * the contents will be save to a temporary file called .jparsec2 (GILDAS script), to be executed
 * automatically with the @ command contained in the temporary file .jparsec (Python script). With this
 * strategy the FOR and IF instructions will work. The script in .jparsec2 will be
 * identical to the input script, except that the modifier /global will be added to
 * define-like instructions (if necessary) to make any variable accessible from outside the script.
 * The second case is to use the @ command directly to execute the desired
 * script. IF and FOR will work, but to return the value of a given variable from that
 * script it should be defined with the modifier /global manually. The returned
 * variable can be of type double, float, integer, or character, with just a value
 * (not an array).
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class pyGildas {

	private String[] exportVariables;
	private String[] sourceVariables;
	private String[] lastScript;
	private String out[], variables[];

	/**
	 * Working directory;
	 */
	public String workingDir;
	/**
	 * Script to execute;
	 */
	public String[] script;
	/**
	 * Program to launch. For GILDAS use any of
	 * pysic, pyclass, pygreg, pyastro, pymapping.
	 */
	public String program;

	/**
	 * Constructor to launch GILDAS. GAG root and exec paths are taken
	 * from the corresponding constants in {@linkplain Configuration} class.
	 * @param workDir Working directory.
	 * @param prog Program to launch, pyclass, pygreg, pyastro, pymapping,
	 * pysic, or any other.
	 * @throws JPARSECException If Gildas GAG root and exec variables are not
	 * properly configured.
	 */
	public pyGildas(String workDir, String prog) throws JPARSECException
	{
		if (Configuration.PATH_GILDAS_GAG_EXEC == null || Configuration.PATH_GILDAS_GAG_ROOT == null)
			throw new JPARSECException("Gildas root and exec variables are not configured.");
		exportVariables = new String[] {"GAG_ROOT_DIR="+Configuration.PATH_GILDAS_GAG_ROOT, "GAG_EXEC_SYSTEM="+Configuration.PATH_GILDAS_GAG_EXEC};
		sourceVariables = new String[] {"$GAG_ROOT_DIR/etc/bash_profile"};
		workingDir = workDir;
		if (workingDir.endsWith(FileIO.getFileSeparator())) workingDir = workingDir.substring(0, workingDir.length()-1);
		program = prog;
		this.script = new String[] {
				"#!/bin/bash",
				"export "+exportVariables[0],
				"export "+exportVariables[1],
				"source "+sourceVariables[0],
				"cd "+workingDir,
				"python <<-exit",
				"import "+program,
				program+".setgdict(globals())"
				};
		lastScript = null;
	}

	/**
	 * Constructor to launch GILDAS.
	 * @param gagRoot Path of the GAG_ROOT_DIR environment variable.
	 * @param gagExec Path of the GAG_EXEC_SYSTEM environment variable.
	 * @param workDir Working directory.
	 * @param prog Program to launch, pyclass, pygreg, pyastro, pymapping,
	 * pysic, or any other.
	 */
	public pyGildas(String gagRoot, String gagExec, String workDir, String prog)
	{
		exportVariables = new String[] {"GAG_ROOT_DIR="+gagRoot, "GAG_EXEC_SYSTEM="+gagExec};
		sourceVariables = new String[] {"$GAG_ROOT_DIR/etc/bash_profile"};
		workingDir = workDir;
		if (workingDir.endsWith(FileIO.getFileSeparator())) workingDir = workingDir.substring(0, workingDir.length()-1);
		program = prog;
		this.script = new String[] {
				"#!/bin/bash",
				"export "+exportVariables[0],
				"export "+exportVariables[1],
				"source "+sourceVariables[0],
				"cd "+workingDir,
				"python <<-exit",
				"import "+program,
				program+".setgdict(globals())"
				};
		lastScript = null;
	}

	/**
	 * Constructor to launch GILDAS.
	 * @param configFile Path of the configuration file where GAG_ROOT_DIR and GAG_EXEC_SYSTEM
	 * environment variables are set, following instructions given by Gildas during compilation.
	 * @param workDir Working directory.
	 * @param prog Program to launch, pyclass, pygreg, pyastro, pymapping,
	 * pysic, or any other.
	 * @throws JPARSECException If the environment variables are not found in the configuration file.
	 */
	public pyGildas(String configFile, String workDir, String prog) throws JPARSECException
	{
		String file[] = DataSet.arrayListToStringArray(ReadFile.readAnyExternalFile(configFile));
		String gagRoot = null, gagExec = null;
		for (int i=0; i<file.length; i++) {
			file[i] = file[i].trim();
			int i1 = file[i].indexOf("export GAG_ROOT_DIR");
			if (i1 == 0) gagRoot = FileIO.getRestAfter(file[i], "=".trim());
			int i2 = file[i].indexOf("export GAG_EXEC_SYSTEM");
			if (i2 == 0) gagExec = FileIO.getRestAfter(file[i], "=").trim();
		}
		if (gagRoot == null || gagExec == null) throw new JPARSECException("Environment variables" +
				"are not configured properly.");

		exportVariables = new String[] {"GAG_ROOT_DIR="+gagRoot, "GAG_EXEC_SYSTEM="+gagExec};
		sourceVariables = new String[] {"$GAG_ROOT_DIR/etc/bash_profile"};
		workingDir = workDir;
		if (workingDir.endsWith(FileIO.getFileSeparator())) workingDir = workingDir.substring(0, workingDir.length()-1);
		program = prog;
		this.script = new String[] {
				"#!/bin/bash",
				"export "+exportVariables[0],
				"export "+exportVariables[1],
				"source "+sourceVariables[0],
				"cd "+workingDir,
				"python <<-exit",
				"import "+program,
				program+".setgdict(globals())"
				};
		lastScript = null;
	}

	/**
	 * Executes a script. The script files are named .jparsec, and .jparsec2 (created
	 * only if necessary) and are saved in the working directory during the execution.
	 * After that they are deleted.
	 * @param s The script.
	 * @param params Parameters of the input script, starting from '&amp;1'.
	 * @param variables Names of the variables to be retrieved.
	 * @return The contents of the output and error streams, followed by the values
	 * of the desired variables, if any. Note there are additional methods to handle the
	 * output more comfortably.
	 * @throws JPARSECException If an error occurs.
	 */
	public String[] executeScript(String s[], String params[], String[] variables)
	throws JPARSECException {
		String out[], script[] = new String[] {};
		try {
			String fileName = ".jparsec";
			String dir = workingDir;
			String fullFileName = dir+"/"+fileName;

			script = this.script;
			String ss[] = adaptScript(s, params);
			boolean secondFile = false;
			if (ss.length > 1 || !ss[0].trim().startsWith(program+".comm('@")) {
				WriteFile.writeAnyExternalFile(fullFileName+"2", ss);
				s = new String[] {"@.jparsec2"};
				ss = adaptScript(s, params);
				secondFile = true;
			}
			script = DataSet.addStringArray(script, ss);
			if (variables != null) {
				this.variables = variables.clone();
				String var[] = variables.clone();
				for (int i=0; i<var.length; i++)
				{
					var[i] = "print "+var[i];
				}
				script = DataSet.addStringArray(script, var);
			}
			script = DataSet.addStringArray(script, new String[] {"exit()", "exit"});
			lastScript = script;

			// Adjust script for pyGildas after dec10c ...
			int index = DataSet.getIndex(script, "python <<-exit");
			int a = 0;
			if (script[script.length-1].equals("exit")) a = 1;
			String pyscript[] = DataSet.getSubArray(script, index+1, script.length-1-a);
			script = DataSet.getSubArray(script, 0, index);
			script[script.length-1] = "python .py";
			WriteFile.writeAnyExternalFile(FileIO.getDirectoryFromPath(fullFileName)+".py", pyscript);

			WriteFile.writeAnyExternalFile(fullFileName, script);
			Process p1 = ApplicationLauncher.executeCommand("chmod +x "+fileName, null, new File(dir));
			p1.waitFor();
			
			String ln = "./"+fileName;
			Process p = ApplicationLauncher.executeCommand(ln, null, new File(dir));
			p.waitFor();
			String error = ApplicationLauncher.getConsoleErrorOutputFromProcess(p);
			String output = ApplicationLauncher.getConsoleOutputFromProcess(p);
			FileIO.deleteFile(fullFileName);
			if (secondFile) FileIO.deleteFile(fullFileName+"2");

			FileIO.deleteFile(FileIO.getDirectoryFromPath(fullFileName)+".py");

			out = new String[] {output, error};
			if (variables != null && output != null) {
				String o[] = DataSet.toStringArray(output, FileIO.getLineSeparator());
				String v = "";
				for (int i=0; i<o.length; i++)
				{
					if (o[i].startsWith("<Sic")) v += o[i+1]+" ";
				}
				int n = FileIO.getNumberOfFields(v, " ", true);
				if (n >= variables.length) {
					int step = n - variables.length;
					// This is to fix a problem in recent versions of Gildas/Python
					if (step > 0 && v.startsWith("array")) {
						v = v.substring(6);
						v = v.substring(0, v.indexOf("dtype"));
						v = v.substring(0, v.lastIndexOf(","));
						n = FileIO.getNumberOfFields(v, " ", true);
						step = n - variables.length;
					}
					String var[] = new String[variables.length];
					for (int i=0; i<var.length; i++)
					{
						var[i] = FileIO.getField(i + step + 1, v, " ", true);
					}
					out = DataSet.addStringArray(out, var);
				} else {
					if (error != null && (error.toLowerCase().indexOf("importerror")>=0 || error.toLowerCase().indexOf("no module")>=0))
						throw new JPARSECException("Gildas could not be executed. Details: "+error);

					throw new JPARSECException("inconsistent number of variables to be retrieved. Error: "+error);
				}
			}
		} catch (Exception exc)
		{
			throw new JPARSECException(exc);
		}
		this.out = null;
		if (out != null) this.out = out.clone();
		return out;
	}

	private String[] adaptScript(String ss[], String params[])
	throws JPARSECException {
		if (ss == null) return ss;
		String s[] = ss.clone();
		String emptyRows = "";
		for (int i=0; i<s.length; i++)
		{
//			s[i] = DataSet.replaceAll(s[i], "'", "\"", true);
			if (params != null) {
				for (int j=params.length-1; j>=0;  j--)
				{
					s[i] = DataSet.replaceAll(s[i], "&"+(j+1), params[j], true);
				}
			}
			if (s[i].trim().toLowerCase().startsWith("def")) {
				String a = s[i].trim().toLowerCase();
				if (!a.endsWith(" /global") && !a.endsWith(" /globa") && !a.endsWith(" /glob")
						&& !a.endsWith(" /glo") && !a.endsWith(" /gl") && !a.endsWith(" /g")) s[i] += " /global";
			}
			if (s[i].equals("") || s[i].trim().startsWith("!")) {
				emptyRows += " "+(1+i);
			}
		}

		emptyRows = emptyRows.trim();
		if (!emptyRows.equals("")) {
			int n = FileIO.getNumberOfFields(emptyRows, " ", true);
			for (int i=n-1; i>=0; i--)
			{
				int row = Integer.parseInt(FileIO.getField(i+1, emptyRows, " ", true));
				s = DataSet.eliminateRowFromTable(s, row);
			}
		}

		if (s[s.length-1].trim().toLowerCase().startsWith("exit")) s = DataSet.eliminateRowFromTable(s, s.length);

		if (s.length == 1 && s[0].trim().startsWith("@")) {
			s[0] = program+".comm('"+s[0];
			if (params != null) {
				for (int j=0; j<params.length;  j++)
				{
					s[0] += " "+params[j];
				}
			}
			s[0] += "')";
		}
		return s;
	}

	/**
	 * Returns the last script executed.
	 * @return The script;
	 */
	public String[] getLastScriptExecuted()
	{
		return lastScript;
	}

	/**
	 * Returns the console output from the process.
	 * @return Console output;
	 */
	public String getConsoleOutput()
	{
		return out[0];
	}
	/**
	 * Returns the error output from the process.
	 * @return The error output.
	 */
	public String getErrorOutput()
	{
		return out[1];
	}
	/**
	 * Returns the value of a GILDAS variable given its name.
	 * @param name Variable name.
	 * @return Variable value, or null if it is not found or an error
	 * is thrown when executing the script.
	 */
	public String getVariableValue(String name)
	{
		String out = null;
		if (this.out.length > 2) {
			for (int i=2; i<this.out.length; i++)
			{
				if (this.variables[i-2].equals(name)) out = this.out[i];
			}
		}
		return out;
	}
}
