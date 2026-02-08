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


import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import jparsec.graph.DataSet;
import jparsec.io.ApplicationLauncher;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;

/**
 * Provides basic logging capabilities. Each log is optionally written to a log file with
 * a path defined by a static constant {@linkplain Logger#configFile}. The maximum
 * size in MB can also be configured (before writting the first log entry).
 * The log is initialized automatically in the first call to write a log entry.
 * The log entry has a very simple format with the date (format yyyy-MMM-dd HH:mm:ss),
 * the process identifier (typically process id number@host), the method including the
 * class where the entry is originated, and the message.
 * @author T. Alonso Albi - OAN (Spain).
 * @version 1.0
 * @since 1.85
 */
public class Logger
{
	// private constructor so that this class cannot be instantiated.
	private Logger() {}

	/**
	 * The basic levels of a given log message.
	 */
	public enum LEVEL {
		/** A trace message of lowest importance. */
		TRACE_LEVEL1,
		/** A trace message of medium importance. */
		TRACE_LEVEL2,
		/** A trace message of high importance. */
		TRACE_LEVEL3,
		/** A message with user configuration data. The priority of this
		 * kind of message is between {@linkplain LEVEL#INFO} and
		 * {@linkplain LEVEL#TRACE_LEVEL3}.
		 * Internally JPARSEC does not use information or configuration messages,
		 * but only trace messages (and warnings/errors). */
		CONFIG,
		/** An informative message, with a priority just below the warning level.
		 * Internally JPARSEC does not use information or configuration messages,
		 * but only trace messages (and warnings/errors). */
		INFO,
		/** A warning to the user. */
		WARNING,
		/** A message indicating an error. */
		ERROR
	};

	/** Holds the local path of the log file, default is null to avoid writing to a file. */
	public static String configFile = null; //"/tmp/log.txt";
	/** Holds the maximum size of the log file in MB, default is 20. */
	public static int configFileMaxSizeMB = 20;
	/** Set to false (default is true) to avoid reporting internal log messages from JPARSEC library. */
	public static boolean reportJPARSECLogs = true;

	private static FileHandler fh = null;
	private static java.util.logging.Logger logger = java.util.logging.Logger.getLogger("JPARSEC");

	private static boolean ENABLE_LOGGING = true;

	/**
	 * Enables the logging system. Logging is enabled by default.
	 */
	public static void enableLogging() {
		ENABLE_LOGGING = true;
	}
	/**
	 * Disables the logging system.
	 */
	public static void disableLogging() {
		ENABLE_LOGGING = false;
	}

	/**
	 * Writes a message to the log. In case the logger is not
	 * started, it is automatically done.
	 * @param level The level of the message.
	 * @param message The message.
	 */
	public static void log(LEVEL level, String message) {
		Logger.log(level, message, JPARSECException.getLastMethodName());
	}

	/**
	 * Writes a message to the log. In case the logger is not
	 * started, it is automatically done.
	 * @param level The level of the message.
	 * @param message The message.
	 * @param lastMethod The name of the method that originates the log message.
	 */
	public static void log(LEVEL level, String message, String lastMethod) {
		if (!ENABLE_LOGGING) return;

		if (fh == null) try { Logger.startup(); } catch (Exception exc) {
			if (!Configuration.APPLET_MODE) exc.printStackTrace(); // In applet mode it is started, but Logger level cannot be adjusted
			return;
		}

		String pid = ApplicationLauncher.getProcessID();

		LogRecord record = new LogRecord(Level.INFO, message);
		record.setSourceClassName("("+pid+")");
		record.setSourceMethodName(lastMethod);

		if (!reportJPARSECLogs && record.getSourceMethodName().trim().toLowerCase().startsWith("jparsec.")) return;

		switch (level) {
		case TRACE_LEVEL1:
			record.setLevel(Level.FINEST);
			break;
		case TRACE_LEVEL2:
			record.setLevel(Level.FINER);
			break;
		case TRACE_LEVEL3:
			record.setLevel(Level.FINE);
			break;
		case CONFIG:
			record.setLevel(Level.CONFIG);
			break;
		case WARNING:
			record.setLevel(Level.WARNING);
			break;
		case ERROR:
			record.setLevel(Level.SEVERE);
			break;
		case INFO:
			break;
		}
		logger.log(record);
	}

	/**
	 * Starts the logger using the log file and maximum size
	 * selected. The default level of the global logger is information,
	 * and all messages for the file handler.
	 * @throws SecurityException If an error occurs.
	 * @throws IOException If an error occurs.
	 */
	private static void startup()
	        throws SecurityException, IOException {
		if (configFile != null) {
		    fh = new FileHandler(
		    		configFile, //pattern
		    		configFileMaxSizeMB * 1024 * 1024, //limit
		    		1, // count
		    		true); //append
		    fh.setLevel(Level.ALL); // level
		    fh.setFormatter(new MySimpleFormatter()); //formatter

			logger.addHandler(fh);
		}

		setLoggerLevel(LEVEL.INFO);
	}

	/**
	 * Returns the Logger object.
	 * @return The Logger.
	 */
	public static java.util.logging.Logger getLogger() {
		return logger;
	}
	
	/**
	 * Sets the logging level of the global logger. The default value is the
	 * information level.
	 * @param level The minimum level to report logs. Null will turn off the logger.
	 */
	public static void setLoggerLevel(LEVEL level) {
		if (level == null) {
			logger.setLevel(Level.OFF);
			return;
		}
		Level l = null;
		switch (level) {
		case TRACE_LEVEL1:
			l = Level.FINEST;
			break;
		case TRACE_LEVEL2:
			l = Level.FINER;
			break;
		case TRACE_LEVEL3:
			l = Level.FINE;
			break;
		case CONFIG:
			l = Level.CONFIG;
			break;
		case WARNING:
			l = Level.WARNING;
			break;
		case ERROR:
			l = Level.SEVERE;
			break;
		case INFO:
			l = Level.INFO;
			break;
		}
		logger.setLevel(l);
	}
	/**
	 * Sets the logging level of the file handler. The default value
	 * at startup will write all messages to the file.
	 * @param level The minimum level to report logs
	 * to the file. Null will turn off the logger.
	 * @throws IOException  If the logger cannot be initialized.
	 * @throws SecurityException  If the logger cannot be initialized.
	 */
	public static void setFileHandlerLevel(LEVEL level) throws SecurityException, IOException {
		if (fh == null) {
			startup();
			if (fh == null) return;
		}
		if (level == null) {
			fh.setLevel(Level.OFF);
			return;
		}
		Level l = null;
		switch (level) {
		case TRACE_LEVEL1:
			l = Level.FINEST;
			break;
		case TRACE_LEVEL2:
			l = Level.FINER;
			break;
		case TRACE_LEVEL3:
			l = Level.FINE;
			break;
		case CONFIG:
			l = Level.CONFIG;
			break;
		case WARNING:
			l = Level.WARNING;
			break;
		case ERROR:
			l = Level.SEVERE;
			break;
		case INFO:
			l = Level.INFO;
			break;
		}
		fh.setLevel(l);
	}

	/**
	 * Disables the output of the logging to the file.
	 */
	public static void disableWritingToFile() {
		if (logger.getHandlers().length > 0 && fh != null)
			logger.removeHandler(fh);
	}
	/**
	 * Enables the output of the logging to the file.
	 * @throws Exception If the file handler must be created
	 * and this process fails.
	 */
	public static void enableWritingToFile() throws Exception {
		if (logger.getHandlers().length == 0) {
			if (fh == null && configFile != null) {
			    fh = new FileHandler(
			    		configFile, //pattern
			    		configFileMaxSizeMB * 1024 * 1024, //limit
			    		1, // count
			    		true); //append
			    fh.setLevel(Level.ALL); // level
			    fh.setFormatter(new MySimpleFormatter()); //formatter
			}

			if (fh != null) logger.addHandler(fh);
		}
	}

	/**
	 * Removes the log file.
	 * @return True if it is deleted successfully.
	 */
	public static boolean removeLogFile() {
		File file = new File(configFile);
		if (file.exists()) return file.delete();
		return false;
	}

	/**
	 * Returns the contents of the log file.
	 * @return The contents, or null if it is not found.
	 * @throws JPARSECException If an error occurs reading the log file.
	 */
	public static String[] getLogFileContents() throws JPARSECException {
		File file = new File(configFile);
		if (file.exists()) return DataSet.arrayListToStringArray(ReadFile.readAnyExternalFile(configFile));
		return null;
	}
	
	public static class MySimpleFormatter extends Formatter {
	    public String format(LogRecord record) {
	    	String out = calcDate(record.getMillis());
	    	out += " "+record.getSourceClassName()+", "+record.getSourceMethodName()+": "+record.getMessage()+FileIO.getLineSeparator();
	        return out;
	    }

	    private String calcDate(long millisecs)
	    {
	    	SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
	    	Date resultdate = new Date(millisecs);
	    	return date_format.format(resultdate);
	    }
	}
}

