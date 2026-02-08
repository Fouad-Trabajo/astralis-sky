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
package jparsec.vo;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;

import javax.imageio.ImageIO;

import jparsec.ephem.Functions;
import jparsec.ephem.Target.TARGET;
import jparsec.graph.DataSet;
import jparsec.io.ApplicationLauncher;
import jparsec.io.ApplicationLauncher.OS;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.io.WriteFile;
import jparsec.observer.LocationElement;
import jparsec.util.JPARSECException;

/**
 * A class to send queries to any web service.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class GeneralQuery implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36";

	private String query;
	/**
	 * Constructor for a given query.
	 * @param query The query.
	 */
	public GeneralQuery(String query)
	{
		this.query = query;
	}
	/**
	 * Perform the query.
	 * @return Response from server.
	 * @throws JPARSECException If an error occurs.
	 */
	public String query()
	throws JPARSECException {
		String q = query(this.query);
		return q;
	}
	/**
	 * Perform the query for a binary file. An attempt to read the whole file is
	 * done (whatever its size). If the size of the file cannot be known, then
	 * only the first 10 MB are read.
	 * @param path Path of the file to create. If null no file will be created.
	 * @return The content type header of the file.
	 * @throws JPARSECException If an error occurs.
	 */
	public String queryFile(String path)
	throws JPARSECException {
		String q = queryFile(this.query, path);
		return q;
	}
	/**
	 * Perform the query for a binary file. An attempt to read the whole file is
	 * done (whatever its size). If the size of the file cannot be known, then
	 * only the first 10 MB are read.
	 * @return The contents of the file.
	 * @throws JPARSECException If an error occurs.
	 */
	public byte[] queryFileContents()
	throws JPARSECException {
		return queryFileContents(this.query, 30000);
	}
	/**
	 * Perform the query for an image. Only certain image types as supported, like
	 * .jpg, .png, or .gif. For .fits or other types use
	 * {@linkplain GeneralQuery#queryFile(String, String)}.
	 * @return Image from server.
	 * @throws JPARSECException If an error occurs.
	 */
	public BufferedImage queryImage()
	throws JPARSECException {
		BufferedImage q = queryImage(this.query);
		return q;
	}

	/**
	 * ID constant for Moon shaded relief map.
	 */
	public static final String USGS_MOON_SHADED_RELIEF = "lunar_sr";
	/**
	 * ID constant for Moon lidar relief map.
	 */
	public static final String USGS_MOON_LIDAR_RELIEF = "lunar_lidar";
	/**
	 * ID constant for Moon Clementine natural color map.
	 */
	public static final String USGS_MOON_CLEMENTINE_NATURAL = "moon_clementine_bw";
	/**
	 * ID constant for IO Galileo color map.
	 */
	public static final String USGS_IO_GALILEO_COLOR = "io_galileo_color";
	/**
	 * ID constant for Mars Viking color map.
	 */
	public static final String USGS_MARS_VIKING_COLOR = "mars_viking_color";
	/**
	 * ID constant for Mars Viking color merged map.
	 */
	public static final String USGS_MARS_VIKING_MERGED = "mars_viking_merged";
	/**
	 * ID constant for Mars MOLA digital terrain model.
	 */
	public static final String USGS_MARS_MOLA = "mars_mgs_mola_topo";
	/**
	 * ID constant for Venus topography map from Magellan.
	 */
	public static final String USGS_VENUS_TOPOGRAPHY = "venus_gtdr";
	/**
	 * ID constant for Venus radar aperture map from Magellan.
	 */
	public static final String USGS_VENUS_RADAR_MAP = "venus_magellan_fmap_leftlook";
	/**
	 * ID constant for Callisto map from Galileo.
	 */
	public static final String USGS_CALLISTO="callisto_galileo_bw";
	/**
	 * ID constant for Europa map from Galileo.
	 */
	public static final String USGS_EUROPA="europa_galileo_bw";
	/**
	 * ID constant for Ganymede map from Galileo.
	 */
	public static final String USGS_GANYMEDE="ganymede_galileo_color";
	/**
	 * ID constant for Enceladus map.
	 */
	//public static final String USGS_ENCELADUS="enceladus_bw";
	/**
	 * ID constant for Tethys map.
	 */
	//public static final String USGS_TETHYS="tethys_bw";
	/**
	 * ID constant for Dione map.
	 */
	//public static final String USGS_DIONE="dione_bw";
	/**
	 * ID constant for Rhea map from Voyager.
	 */
	//public static final String USGS_RHEA="rhea_voyager_bwm";
	/**
	 * ID constant for Iapetus map.
	 */
	//public static final String USGS_IAPETUS="iapetus_bw";

	/**
	 * Creates a query to the USGS planetary map server.
	 * @param targetBody Target body, constants defined in class {@linkplain TARGET}, currently only Mars,
	 * the Moon, and Io are supported.
	 * @param width Image width.
	 * @param height image height.
	 * @param box Box of bounding coordinates to display, in the form (west limit,
	 * south limit, east limit, north limit).
	 * @param dataset Dataset id constant. Constants defined in this class. Set to
	 * null to use a default dataset.
	 * @return The query.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String getQueryToUSGSAstroGeologyMapServer(TARGET targetBody, int width, int height, double box[],
			String dataset)
	throws JPARSECException {
		String layer = "SRS=IAU2000:49911";
		switch (targetBody)
		{
		case Io:
		case Europa:
		case Ganymede:
		case Callisto:
		case VENUS:
			layer = "SRS=EPSG:4326&LAYERS=";
			if (dataset == null) {
				layer += USGS_IO_GALILEO_COLOR;
			} else {
				layer += dataset;
			}
			break;
		case Moon:
			layer = "SRS=IAU2000:30110&LAYERS=";
			if (dataset == null) {
				layer += USGS_MOON_CLEMENTINE_NATURAL;
			} else {
				layer += dataset;
			}
			break;
		case MARS:
			layer = "SRS=IAU2000:49911&LAYERS=";
			if (dataset == null) {
				layer += USGS_MARS_VIKING_COLOR;
			} else {
				layer += dataset;
			}
			break;
		default:
			layer += "&LAYERS="+dataset;
			//throw new JPARSECException("unsupported target.");
		}
		String query = "http://www.mapaplanet.org/explorer-bin/imageMaker.cgi?VERSION=1.1.1&REQUEST=GetMap&FORMAT=image/jpeg&";
		query += "map="+targetBody.getName()+"&BBOX="+box[0]+","+box[1]+","+box[2]+","+box[3]+"&WIDTH="+width+"&HEIGHT="+height+"&"+layer;
		return query;
	}

	/**
	 * Returns a query suitable for obtaining pre-main sequence star properties using Siess models.<BR>
	 * Reference:<BR>
	 * Siess et al. 1997, A&amp;A 324, 556.
	 * Siess L., Dufour E., Forestini M. 2000, A&amp;A, 358, 593.
	 *
	 * @param metallicity Star metallicity. Valid values are 0.01, 0.02, 0.03, and 0.04,
	 * being 0.02 the solar metallicity.
	 * @param Teff Effective temperature in K.
	 * @param lum Luminosity in solar units.
	 * @param distanceModulus Distance modulus. 0 to avoid correcting extinction.
	 * @return The query.
	 */
	public static String getQueryToSiessModels(double metallicity, double Teff, double lum, double distanceModulus)
	{
		String query = "http://www-astro.ulb.ac.be/Starevol/cgi/hrdfind.cgi?xtype=0&ytype=0&error=n&output=n&table=1&dist="+distanceModulus;
		int metal = (int) (metallicity * 100.0 + 0.5);
		String m = "0"+metal;
		query += "&metallicity="+m;
		query += "&xcoor="+Teff;
		query += "&ycoor="+lum;
		return query;
	}

	/**
	 * The set of SDSS plates for the query.
	 */
	public enum SDSS_PLATE {
		/** Visible light. HST Phase 2, 1.7" resolution. */
		VISIBLE ("phase2_gsc2"),
		/** Blue light. POSS2/UKSTU Blue, 1.0" resolution in North, 1.7" in South.  */
		BLUE ("poss2ukstu_blue"),
		/** Red light. POSS2/UKSTU Red, 1.0" resolution. */
		RED ("poss2ukstu_red"),
		/** Infrared light. POSS2/UKSTU IR, 1.0" resolution. */
		INFRARED ("poss2ukstu_ir");

		private String type;

		private SDSS_PLATE(String t) {
			this.type = t;
		}

		/**
		 * Returns the type.
		 * @return The type to use in the query.
		 */
		public String getType() {
			return type;
		}
	};

	/**
	 * Returns the query string for Sloan Digital Sky Survey.
	 * @param loc Equatorial location of the object.
	 * @param plate plate type.
	 * @param field field of view in arcminutes. From 0 to 60.0;
	 * @param fitsFormat True to return a fits file, false for a gif one.
	 * @return The query.
	 * @throws JPARSECException If the input is invalid.
	 * @deprecated DSS queries can also be done using SkyView.
	 */
	@Deprecated
	public static String getQueryToSDSS(LocationElement loc, SDSS_PLATE plate, double field, boolean fitsFormat)
	throws JPARSECException{
		String format = "gif";
		if (fitsFormat) format = "fits";

		if (!format.toLowerCase().startsWith("g") && !format.equals("f"))
			throw new JPARSECException("invalid format "+format+".");

		String ra = Functions.formatRA(loc.getLongitude());
		String dec = Functions.formatDEC(loc.getLatitude());
		String queryRa = Functions.getHoursFromFormattedRA(ra);
		queryRa += "+" + Functions.getMinutesFromFormattedRA(ra);
		queryRa += "+" + Functions.getSecondsFromFormattedRA(ra);
		String queryDec = Functions.getDegreesFromFormattedDEC(dec);
		queryDec += "+" + Functions.getMinutesFromFormattedDEC(dec);
		queryDec += "+" + Functions.getSecondsFromFormattedDEC(dec);
		if (queryDec.startsWith("-")) {
			queryDec = DataSet.replaceAll(queryDec, "-", "%", true);
		} else {
			if (queryDec.startsWith("+")) {
				queryDec = DataSet.replaceAll(queryDec, "+", "%2B", true);
			} else {
				queryDec = "%2B"+queryDec;
			}
		}
		queryRa = DataSet.replaceAll(queryRa, ",", ".", false);
		queryDec = DataSet.replaceAll(queryDec, ",", ".", false);
		String query = "http://archive.stsci.edu/cgi-bin/dss_search?";
		query += "v="+plate.getType()+"&r="+queryRa+"&d="+queryDec+"&e=J2000&h="+field+"&w="+field+"&f="+format;
		query += "&c=none&fov=NONE&v3=";
		return query;
	}

	/**
	 * Perform the query with a default timeout of 30s.
	 * @param query Query to call.
	 * @return Response from server.
	 * @throws JPARSECException If an error occurs.
	 */
    public static String query(String query)
    throws JPARSECException
    {
    	return GeneralQuery.query(query, 30000);
    }

	/**
	 * Perform the query.
	 * @param query Query to call.
	 * @param timeout Timeout in milliseconds. If the connection waits
	 * for data for more than this time an exception will be thrown.
	 * @return Response from server.
	 * @throws JPARSECException If an error occurs.
	 */
    public static String query(String query, int timeout)
    throws JPARSECException
    {
    	return GeneralQuery.query(query, "UTF-8", timeout);
    }

	/**
	 * Perform the query.
	 * @param query Query to call.
	 * @param charset The charset to use to interpret the response.
	 * @param timeout Timeout in milliseconds. If the connection waits
	 * for data for more than this time an exception will be thrown.
	 * A timeout of &lt;=0 disables any possible download and raises an error.
	 * @return Response from server.
	 * @throws JPARSECException If an error occurs.
	 */
    public static String query(String query, String charset, int timeout)
    throws JPARSECException
    {
    	try {
    		if (timeout <= 0) throw new Exception("Cannot download with timeout <= 0");

			URL urlObject = new URL(query);
			URLConnection con = urlObject.openConnection();
			con.setRequestProperty
			  ( "User-Agent", USER_AGENT );

			con.setConnectTimeout(timeout);
			con.setReadTimeout(timeout);
			// Get the response
	        BufferedReader in = new BufferedReader(
	                      new InputStreamReader(
	                      con.getInputStream(), charset));
			String inputLine;

			StringBuffer output = new StringBuffer(1000);
			while ((inputLine = in.readLine()) != null)
			{
				output.append(inputLine + FileIO.getLineSeparator());
			}
			in.close();

			return output.toString();
    	} catch (Exception e)
    	{
    		if (//!(e instanceof SocketTimeoutException) && 
    				query.startsWith("https") && ApplicationLauncher.getOperatingSystem() != OS.WINDOWS) {
    			try {
    				String f1[] = FileIO.getFiles(FileIO.getTemporalDirectory());
	    			Process pr = ApplicationLauncher.executeCommand("wget -T 5 "+query, null, new File(FileIO.getTemporalDirectory()));
	    			pr.waitFor();
    				String f2[] = FileIO.getFiles(FileIO.getTemporalDirectory());
    				if (f2.length == f1.length+1) {
    					for (int i=0; i<f2.length; i++) {
    						int index = DataSet.getIndex(f1, f2[i]);
    						if (index < 0) {
    							String out = DataSet.arrayListToString(ReadFile.readAnyExternalFile(f2[i], charset));
    							FileIO.deleteFile(f2[i]);
    							return out;
    						}
    					}
    				}
    			} catch (Exception e2) { e2.printStackTrace(); }
    		}
    		throw new JPARSECException(e);
    	}
    }

	/**
	 * Perform the query for a remote file. An attempt to read the whole file is
	 * done (whatever its size). If the size of the file cannot be known, then
	 * only the first 10 MB are read. The default timeout is 30s.
	 * @param query Query to call.
	 * @param fileName Path of the file to create. If null no file will be created.
	 * @return The content type header of the file.
	 * @throws JPARSECException If an error occurs.
	 */
    public static String queryFile(String query, String fileName)
    throws JPARSECException
    {
    	return GeneralQuery.queryFile(query, fileName, 30000);
    }

	/**
	 * Perform the query for a remote file. An attempt to read the whole file is
	 * done (whatever its size). If the size of the file cannot be known, then
	 * only the first 10 MB are read. The default timeout is 30s.
	 * @param query Query to call.
	 * @param fileName Path of the file to create. If null no file will be created.
	 * @param timeout Timeout in milliseconds. If the connection waits
	 * for data for more than this time an exception will be thrown.
	 * @return The content type header of the file, or null if a file exists in
	 * the path provided.
	 * @throws JPARSECException If an error occurs.
	 */
    public static String queryFileIfDoesNotExist(String query, String fileName, int timeout)
    throws JPARSECException
    {
    	if (!FileIO.exists(fileName))
    		return GeneralQuery.queryFile(query, fileName, timeout);
    	return null;
    }

	/**
	 * Perform the query for a binary file. An attempt to read the whole file is
	 * done (whatever its size). If the size of the file cannot be known, then
	 * only the first 10 MB are read.
	 * @param query Query to call.
	 * @param fileName Path of the file to create. If null no file will be created.
	 * @param timeout Timeout in milliseconds. If the connection waits
	 * for data for more than this time an exception will be thrown.
	 * A timeout of &lt;=0 disables any possible download and raises an error.
	 * @return The content type header of the file.
	 * @throws JPARSECException If an error occurs.
	 */
    public static String queryFile(String query, String fileName, int timeout)
    throws JPARSECException
    {
    	try {
    		if (timeout <= 0) throw new JPARSECException("Cannot download with timeout <= 0");

			URL urlObject = new URL(query);
			URLConnection con = urlObject.openConnection();
			con.setRequestProperty
			  ( "User-Agent", USER_AGENT );
			con.setConnectTimeout(timeout);
			con.setReadTimeout(timeout);

		    if (fileName == null) return con.getContentType();

			// Get the response
			InputStream in = new BufferedInputStream( con.getInputStream());
			int contentLength = con.getContentLength();
			if (contentLength < 1) contentLength = 1024 * 1024 * 10;
		    byte[] data = new byte[contentLength];
		    int totalBytesRead = 0;
		    int bytesRead = 0;
		    int offset = 0;
		    while (offset < contentLength) {
		      bytesRead = in.read(data, offset, data.length - offset);
		      if (bytesRead == -1)
		        break;
		      offset += bytesRead;
		      totalBytesRead += bytesRead;
		    }
		    in.close();

		    if (totalBytesRead < 1) return con.getContentType();

		    FileOutputStream out = new FileOutputStream(new File(fileName));
		    out.write(data, 0, totalBytesRead);
		    out.flush();
		    out.close();

		    return con.getContentType();
    	} catch (Exception e)
    	{
    		if (//!(e instanceof SocketTimeoutException) && 
    				query.startsWith("https") && ApplicationLauncher.getOperatingSystem() == OS.LINUX && 
    				fileName != null) {
    			try {
    				String f1[] = FileIO.getFiles(FileIO.getTemporalDirectory());
	    			Process pr = ApplicationLauncher.executeCommand("wget -T 5 "+query, null, new File(FileIO.getTemporalDirectory()));
	    			pr.waitFor();
    				String f2[] = FileIO.getFiles(FileIO.getTemporalDirectory());
    				if (f2.length == f1.length+1) {
    					for (int i=0; i<f2.length; i++) {
    						int index = DataSet.getIndex(f1, f2[i]);
    						if (index < 0) {
    							String charset = ReadFile.ENCODING_UTF_8;
    							String out = DataSet.arrayListToString(ReadFile.readAnyExternalFile(f2[i], charset));
    							FileIO.deleteFile(f2[i]);
    							WriteFile.writeAnyExternalFile(fileName, out, charset);
    							return "txt";
    						}
    					}
    				}
    			} catch (Exception e2) { e2.printStackTrace(); }
    		}
    		throw new JPARSECException(e);
    	}
    }

	/**
	 * Perform the query for a binary file. An attempt to read the whole file is
	 * done (whatever its size). If the size of the file cannot be known, then
	 * only the first 10 MB are read.
	 * @param query Query to call.
	 * @param timeout Timeout in milliseconds. If the connection waits
	 * for data for more than this time an exception will be thrown.
	 * A timeout of &lt;=0 disables any possible download and raises an error.
	 * @return The content of the file.
	 * @throws JPARSECException If an error occurs.
	 */
    public static byte[] queryFileContents(String query, int timeout)
    throws JPARSECException
    {
    	try {
    		if (timeout <= 0) throw new JPARSECException("Cannot download with timeout <= 0");

			URL urlObject = new URL(query);
			URLConnection con = urlObject.openConnection();
			con.setRequestProperty
			  ( "User-Agent", USER_AGENT );
			con.setConnectTimeout(timeout);
			con.setReadTimeout(timeout);

			// Get the response
			InputStream in = new BufferedInputStream( con.getInputStream());
			int contentLength = con.getContentLength();
			if (contentLength < 1) contentLength = 1024 * 1024 * 10;
		    byte[] data = new byte[contentLength];
		    // int totalBytesRead = 0;
		    int bytesRead = 0;
		    int offset = 0;
		    while (offset < contentLength) {
		      bytesRead = in.read(data, offset, data.length - offset);
		      if (bytesRead == -1)
		        break;
		      offset += bytesRead;
		      // totalBytesRead += bytesRead;
		    }
		    in.close();

		    return data;
    	} catch (Exception e)
    	{
    		throw new JPARSECException(e);
    	}
    }

	/**
	 * Perform the query for a text file. An attempt to read the whole file is
	 * done (whatever its size). If the size of the file cannot be known, then
	 * only the first 10 MB are read.
	 * @param query Query to call.
	 * @param encoding The charset.
	 * @param timeout Timeout in milliseconds. If the connection waits
	 * for data for more than this time an exception will be thrown.
	 * A timeout of &lt;=0 disables any possible download and raises an error.
	 * @return The text file, or null if nothing is read.
	 * @throws JPARSECException If an error occurs.
	 */
    public static String queryTextFile(String query, String encoding, int timeout)
    throws JPARSECException
    {
    	try {
    		if (timeout <= 0) throw new JPARSECException("Cannot download with timeout <= 0");

			URL urlObject = new URL(query);
			URLConnection con = urlObject.openConnection();
			con.setRequestProperty
			  ( "User-Agent", USER_AGENT );
			con.setConnectTimeout(timeout);
			con.setReadTimeout(timeout);

			// Get the response
			InputStream in = new BufferedInputStream( con.getInputStream());
			int contentLength = con.getContentLength();
			if (contentLength < 1) contentLength = 1024 * 1024 * 10;
		    byte[] data = new byte[contentLength];
		    int totalBytesRead = 0;
		    int bytesRead = 0;
		    int offset = 0;
		    while (offset < contentLength) {
		      bytesRead = in.read(data, offset, data.length - offset);
		      if (bytesRead == -1)
		        break;
		      offset += bytesRead;
		      totalBytesRead += bytesRead;
		    }
		    in.close();

		    if (totalBytesRead < 1) return null;

		    String out = new String(data, encoding);

		    return out;
    	} catch (Exception e)
    	{
    		if (//!(e instanceof SocketTimeoutException) && 
    				query.startsWith("https") && ApplicationLauncher.getOperatingSystem() == OS.LINUX) {
    			try {
    				String f1[] = FileIO.getFiles(FileIO.getTemporalDirectory());
	    			Process pr = ApplicationLauncher.executeCommand("wget -T 5 "+query, null, new File(FileIO.getTemporalDirectory()));
	    			pr.waitFor();
    				String f2[] = FileIO.getFiles(FileIO.getTemporalDirectory());
    				if (f2.length == f1.length+1) {
    					for (int i=0; i<f2.length; i++) {
    						int index = DataSet.getIndex(f1, f2[i]);
    						if (index < 0) {
    							String out = DataSet.arrayListToString(ReadFile.readAnyExternalFile(f2[i], encoding));
    							FileIO.deleteFile(f2[i]);
    							return out;
    						}
    					}
    				}
    			} catch (Exception e2) { e2.printStackTrace(); }
    		}
    		throw new JPARSECException(e);
    	}
    }

	/**
	 * Perform the query for an object. An attempt to read the whole file is
	 * done (whatever its size). If the size of the file cannot be known, then
	 * only the first 10 MB are read. The default timeout is 30s.
	 * @param query Query to call.
	 * @return The object
	 * @throws JPARSECException If an error occurs.
	 */
    public static Object queryObject(String query)
    throws JPARSECException
    {
    	return GeneralQuery.queryObject(query, 30000);
    }

	/**
	 * Perform the query for an object. An attempt to read the whole file is
	 * done (whatever its size). If the size of the file cannot be known, then
	 * only the first 10 MB are read.
	 * @param query Query to call.
	 * @param timeout Timeout in milliseconds. If the connection waits
	 * for data for more than this time an exception will be thrown.
	 * A timeout of &lt;=0 disables any possible download and raises an error.
	 * @return The object
	 * @throws JPARSECException If an error occurs.
	 */
    public static Object queryObject(String query, int timeout)
    throws JPARSECException
    {
    	try {
       		if (timeout <= 0) throw new JPARSECException("Cannot download with timeout <= 0");

       	 	URL urlObject = new URL(query);
			URLConnection con = urlObject.openConnection();
			con.setRequestProperty
			  ( "User-Agent", USER_AGENT );
			con.setConnectTimeout(timeout);
			con.setReadTimeout(timeout);

			// Get the response
			InputStream in = new BufferedInputStream( con.getInputStream());

			int contentLength = con.getContentLength();
			if (contentLength < 1) contentLength = 1024 * 1024 * 10;
		    byte[] data = new byte[contentLength];
		    int totalBytesRead = 0;
		    int bytesRead = 0;
		    int offset = 0;
		    while (offset < contentLength) {
		      bytesRead = in.read(data, offset, data.length - offset);
		      if (bytesRead == -1)
		        break;
		      offset += bytesRead;
		      totalBytesRead += bytesRead;
		    }
		    in.close();

		    if (totalBytesRead < 1) return con.getContentType();

		      ObjectInputStream inStream = new ObjectInputStream(new ByteArrayInputStream(data, 0, totalBytesRead));
		      Object obj = inStream.readObject();
		      inStream.close();

		    return obj;
    	} catch (Exception e)
    	{
    		throw new JPARSECException(e);
    	}
    }

	/**
	 * Perform the query for an image. Only certain image types as supported, like
	 * .jpg, .png, or .gif. For .fits or other types use
	 * {@linkplain GeneralQuery#queryFile(String, String)}. The default timeout is 30s.
	 * @param query Query to call.
	 * @return Image from server.
	 * @throws JPARSECException If an error occurs.
	 */
    public static BufferedImage queryImage(String query)
    throws JPARSECException
    {
    	return GeneralQuery.queryImage(query, 30000);
    }

	/**
	 * Perform the query for an image. Only certain image types as supported, like
	 * .jpg, .png, or .gif. For .fits or other types use
	 * {@linkplain GeneralQuery#queryFile(String, String)}.
	 * @param query Query to call.
	 * @param timeout Timeout in milliseconds. If the connection waits
	 * for data for more than this time an exception will be thrown.
	 * A timeout of &lt;=0 disables any possible download and raises an error.
	 * @return Image from server.
	 * @throws JPARSECException If an error occurs.
	 */
    public static BufferedImage queryImage(String query, int timeout)
    throws JPARSECException
    {
    	try {
       		if (timeout <= 0) throw new JPARSECException("Cannot download with timeout <= 0");

       	 	URL urlObject = new URL(query);
			URLConnection con = urlObject.openConnection();
			con.setRequestProperty
			  ( "User-Agent", USER_AGENT );
			con.setConnectTimeout(timeout);
			con.setReadTimeout(timeout);

			// Get the response
			InputStream is = new BufferedInputStream( con.getInputStream());
			BufferedImage image = ImageIO.read(is);
			is.close();

			return image;
    	} catch (Exception e)
    	{
    		if (//!(e instanceof SocketTimeoutException) && 
    				query.startsWith("https") && ApplicationLauncher.getOperatingSystem() == OS.LINUX) {
    			try {
    				String f1[] = FileIO.getFiles(FileIO.getTemporalDirectory());
	    			Process pr = ApplicationLauncher.executeCommand("wget -T 5 "+query, null, new File(FileIO.getTemporalDirectory()));
	    			pr.waitFor();
    				String f2[] = FileIO.getFiles(FileIO.getTemporalDirectory());
    				if (f2.length == f1.length+1) {
    					for (int i=0; i<f2.length; i++) {
    						int index = DataSet.getIndex(f1, f2[i]);
    						if (index < 0) {
    							BufferedImage image = ImageIO.read(new File(f2[i]));
    							FileIO.deleteFile(f2[i]);
    							return image;
    						}
    					}
    				}
    			} catch (Exception e2) { e2.printStackTrace(); }
    		}
    		throw new JPARSECException(e);
    	}
    }

    /**
     * The set of projections for Skyview queries.
     */
    public enum SKYVIEW_PROJECTION {
	    /** Cartesian projection for SkyView. */
	    CARTESIAN,
	    /** Global sinusoidal projection for SkyView. */
	    GLOBAL_SINUSOIDAL,
	    /** Gnomonic (or tangent) projection for SkyView. */
	    GNOMONIC,
	    /** Stereographic projection for SkyView. */
	    STEREOGRAPHIC,
	    /** Orthographic (sine) projection for SkyView. */
	    ORTHOGRAPHIC,
	    /** Aitoff projection for SkyView. */
	    AITOFF,
	    /** Zenithal equal area projection for SkyView. */
	    ZENITHAL_EQUAL_AREA
    };

    /**
     * Projections for SkyView.
     */
    public static final String[] SKYVIEW_PROJECTIONS = new String[] {"Car (Cartesian)", "Sfl (Global sinusoidal)", "Tan (Gnomonic)",
    	"Stg (Stereographic)", "Sin (Orthographic)", "Ait (Aitoff)", "Zea (Zenithal equal area)"};

    /**
     * The set of coordinate types for Skyview queries.
     */
    public enum SKYVIEW_COORDINATE {
	    /** J2000 coordinates for SkyView. */
	    EQUATORIAL_J2000 ,
	    /** Ecliptic 2000 coordinates for SkyView. */
	    ECLIPTIC_2000,
	    /** Galactic coordinates for SkyView. */
	    GALACTIC
    };

    /**
     * Coordinate types for SkyView.
     */
    public static final String[] SKYVIEW_COORDINATES = new String[] {"J2000", "E2000", "Gal"};

    /**
     * The set of color tables for Skyview queries.
     */
    public enum SKYVIEW_LUT_TABLE {
	    /** Fire color table for SkyView. */
	    FIRE,
	    /** Ice color table for SkyView. */
	    ICE,
	    /** Red color table for SkyView. */
	    RED,
	    /** Green color table for SkyView. */
	    GREEN,
	    /** Blue color table for SkyView. */
	    BLUE,
	    /** Gray color table for SkyView. */
	    GRAY
    };

    /**
     * Color tables for SkyView.
     */
    public static final String[] SKYVIEW_LUT = new String[] {"Fire", "Ice", "Red", "Green", "Blue", "Gray"};

    /**
     * The set of intensity scales for Skyview queries.
     */
    public enum SKYVIEW_INTENSITY_SCALE {
	    /** Linear scaling for the levels in SkyView images. */
	    LINEAR,
	    /** Log scaling for the levels in SkyView images. */
	    LOG,
	    /** Square root scaling for the levels in SkyView images.*/
	    SQRT
    };

    /**
     * Scaling types for the levels in SkyView images.
     */
    public static final String[] SKYVIEW_SCALING = new String[] {"Linear", "Log", "Sqrt"};

    /**
     * The set of surveys for Skyview queries.
     */
     public enum SKYVIEW_SURVEY {
    	/** Bonn 1420 MHz Survey survey for SkyView. */
    	BONN_1420MHZ,
    	/** HI All-Sky Continuum Survey survey for SkyView. */
    	HI_480MHZ,
    	/** 4850 MHz Survey - GB6/PMN survey for SkyView. */
    	GB6_4850MHZ,
    	/** LABOCA Extended Chandra Deep Field South Submillimetre Survey survey for SkyView. */
    	CDFSLESS,
    	/** CO Galactic Plane Survey survey for SkyView. */
    	CO2D,
    	/** Effelsberg-Bonn HI Survey survey for SkyView. */
    	EBHIS,
    	/** FIRST survey for SkyView. */
    	FIRST,
    	/** GOODS North Observations with the VLA survey for SkyView. */
    	GOODSNVLA,
    	/** GTEE 0035 MHz Radio survey survey for SkyView. */
    	GTEE_0035MHZ,
    	/** Dickey and Lockman HI map survey for SkyView. */
    	NH,
    	/** NVSS survey for SkyView. */
    	NVSS,
    	/** VLA Survey of SDSS Stripe 82 survey for SkyView. */
    	STRIPE82VLA,
    	/** Sydney University Molonglo Sky Survey survey for SkyView. */
    	SUMSS,
    	/** GMRT 150 MHz All-sky Radio Survey: First Alternative Data Release survey for SkyView. */
    	TGSS,
    	/** VLA Low-frequency Sky Survey survey for SkyView. */
    	VLSSR,
    	/** Westerbork Northern Sky Survey survey for SkyView. */
    	WENSS,
    	/** Planck 353 GHz Survey survey for SkyView. */
    	PLANCK353,
    	/** Planck 217 GHz Survey survey for SkyView. */
    	PLANCK217,
    	/** Planck 143 GHz Survey survey for SkyView. */
    	PLANCK143,
    	/** Planck 545 GHz Survey survey for SkyView. */
    	PLANCK545,
    	/** Planck 030 GHz Survey survey for SkyView. */
    	PLANCK030,
    	/** Planck 100 GHz Survey survey for SkyView. */
    	PLANCK100,
    	/** Planck 070 GHz Survey survey for SkyView. */
    	PLANCK070,
    	/** Planck 857 GHz Survey survey for SkyView. */
    	PLANCK857,
    	/** Planck 044 GHz Survey survey for SkyView. */
    	PLANCK044,
    	/** Two Micron All Sky Survey (H-Band) survey for SkyView. */
    	H_2MASS,
    	/** Two Micron All Sky Survey (J-Band) survey for SkyView. */
    	J_2MASS,
    	/** Two Micron All Sky Survey (K-Band) survey for SkyView. */
    	K_2MASS,
    	/** AKARI N160  survey for SkyView. */
    	AKARIN160,
    	/** AKARI N60  survey for SkyView. */
    	AKARIN60,
    	/** AKARI WIDE-L  survey for SkyView. */
    	AKARIWIDE_L,
    	/** AKARI WIDE-S  survey for SkyView. */
    	AKARIWIDE_S,
    	/** Cosmic Background Explorer DIRBE Annual Average Map survey for SkyView. */
    	COBEAAM,
    	/** Cosmic Background Explorer DIRBE Zodi-Subtracted Mission Average survey for SkyView. */
    	COBEZSMA,
    	/** 2nd Digitized Sky Survey-Near Infrared survey for SkyView. */
    	DSS2IR,
    	/** GOODS Herschel 100 micron, DR1 data release  survey for SkyView. */
    	GOODSHERSCHEL1,
    	/** GOODS Herschel 160 micron, DR1 data release  survey for SkyView. */
    	GOODSHERSCHEL2,
    	/** GOODS Herschel 250 micron, DR1 data release  survey for SkyView. */
    	GOODSHERSCHEL3,
    	/** GOODS Herschel 350 micron, DR1 data release  survey for SkyView. */
    	GOODSHERSCHEL4,
    	/** GOODS Herschel 500 micron, DR1 data release  survey for SkyView. */
    	GOODSHERSCHEL5,
    	/** Spitzer IRAC GOODS 3.6 micron data, channel 1  survey for SkyView. */
    	GOODSIRAC1,
    	/** Spitzer IRAC GOODS 4.5 micron data, channel 2  survey for SkyView. */
    	GOODSIRAC2,
    	/** Spitzer IRAC GOODS 5.8 micron data, channel 3  survey for SkyView. */
    	GOODSIRAC3,
    	/** Spitzer IRAC GOODS 8.0 micron data, channel 4  survey for SkyView. */
    	GOODSIRAC4,
    	/** Southern GOODS Field: VLT ISAAC Observations, H band survey for SkyView. */
    	GOODSISAACH,
    	/** Southern GOODS Field: VLT ISAAC Observations, J band survey for SkyView. */
    	GOODSISAACJ,
    	/** Southern GOODS Field: VLT ISAAC Observations, KS band survey for SkyView. */
    	GOODSISAACKS,
    	/** Spitzer MIPS GOODS 24 Micron Data  survey for SkyView. */
    	GOODSMIPS,
    	/** GOODS NICMOS Survey survey for SkyView. */
    	GOODSNICMOS,
    	/** The Hawaii Hubble Deep Field North: Band I survey for SkyView. */
    	HAWAIIHDFI,
    	/** The Hawaii Hubble Deep Field North: Band Z survey for SkyView. */
    	HAWAIIHDFZ,
    	/** VLT ISAAC Ks Observations of the Southern Hubble Ultradeep Field survey for SkyView. */
    	HUDFISAAC,
    	/** IRAS Sky Survey Atlas: 100 micron survey for SkyView. */
    	IRAS100,
    	/** IRAS Sky Survey Atlas: 12 micron survey for SkyView. */
    	IRAS12,
    	/** IRAS Sky Survey Atlas: 25 micron survey for SkyView. */
    	IRAS25,
    	/** IRAS Sky Survey Atlas: 60 micron survey for SkyView. */
    	IRAS60,
    	/** Improved Reprocessing of the IRAS Survey: 100 survey for SkyView. */
    	IRIS100,
    	/** Improved Reprocessing of the IRAS Survey: 12 survey for SkyView. */
    	IRIS12,
    	/** Improved Reprocessing of the IRAS Survey: 25 survey for SkyView. */
    	IRIS25,
    	/** Improved Reprocessing of the IRAS Survey: 60 survey for SkyView. */
    	IRIS60,
    	/** Schlegel, Finkbeiner and Davis 100 Micron survey survey for SkyView. */
    	SFD100M,
    	/** Schlegel, Finkbeiner and Davis dust map survey survey for SkyView. */
    	SFDDUST,
    	/** UKIRT Infrared Deep Survey H-band survey for SkyView. */
    	UKIDSS_H,
    	/** UKIRT Infrared Deep Survey J-band survey for SkyView. */
    	UKIDSS_J,
    	/** UKIRT Infrared Deep Survey K-band survey for SkyView. */
    	UKIDSS_K,
    	/** UKIRT Infrared Deep Survey Y-band survey for SkyView. */
    	UKIDSS_Y,
    	/** WISE 3.4 Micron All-Sky Survey: All-WISE data release survey for SkyView. */
    	WISEW1,
    	/** WISE 4.6 Micron All-Sky Survey: All-WISE data release survey for SkyView. */
    	WISEW2,
    	/** WISE 12 Micron All-Sky Survey: All-WISE data release survey for SkyView. */
    	WISEW3,
    	/** WISE 22 Micron All-Sky Survey: All-WISE data release survey for SkyView. */
    	WISEW4,
    	/** WMAP Nine Year K-Band survey for SkyView. */
    	WMAPK,
    	/** WMAP Nine Year Ka-Band survey for SkyView. */
    	WMAPKA,
    	/** WMAP Nine Year Q-Band survey for SkyView. */
    	WMAPQ,
    	/** WMAP Nine Year V-Band survey for SkyView. */
    	WMAPV,
    	/** WMAP Nine Year W-Band survey for SkyView. */
    	WMAPW,
    	/** WMAP Nine Year Galaxy Removed survey for SkyView. */
    	WMAPILC,
    	/** Original Digitized Sky Survey survey for SkyView. */
    	DSSOLD,
    	/** First Digitized Sky Survey: Blue Plates  survey for SkyView. */
    	DSS1B,
    	/** First Digitized Sky Survey: Red Plates survey for SkyView. */
    	DSS1R,
    	/** 2nd Digitized Sky Survey (Blue) survey for SkyView. */
    	DSS2B,
    	/** 2nd Digitized Sky Survey (Red) survey for SkyView. */
    	DSS2R,
    	/** GOODS HST ACS B Filter survey for SkyView. */
    	GOODSACSB,
    	/** GOODS HST ACS I Filter survey for SkyView. */
    	GOODSACSI,
    	/** GOODS HST ACS V Filter survey for SkyView. */
    	GOODSACSV,
    	/** GOODS HST ACS Z Filter survey for SkyView. */
    	GOODSACSZ,
    	/** Southern GOODS Field: VLT VIMOS Observations, R band survey for SkyView. */
    	GOODSVIMOSR,
    	/** H-alpha Full Sky Map survey for SkyView. */
    	HALPHA,
    	/** The Hawaii Hubble Deep Field North: Band B survey for SkyView. */
    	HAWAIIHDFB,
    	/** The Hawaii Hubble Deep Field North: Band R survey for SkyView. */
    	HAWAIIHDFR,
    	/** The Hawaii Hubble Deep Field North: Band V0201 survey for SkyView. */
    	HAWAIIHDFV0201,
    	/** The Hawaii Hubble Deep Field North: Band V0401 survey for SkyView. */
    	HAWAIIHDFV0401,
    	/** Mellinger All Sky Mosaic: Blue survey for SkyView. */
    	MELLINGER_B,
    	/** Mellinger All Sky Mosaic: Green survey for SkyView. */
    	MELLINGER_G,
    	/** Mellinger All Sky Mosaic: Red survey for SkyView. */
    	MELLINGER_R,
    	/** Near-Earth Asteriod Tracking System Archive survey for SkyView. */
    	NEAT,
    	/** Sloan Digitized Sky Survey g-band survey for SkyView. */
    	SDSSG,
    	/** Sloan Digitized Sky Survey i-band survey for SkyView. */
    	SDSSI,
    	/** Sloan Digitized Sky Survey r-band survey for SkyView. */
    	SDSSR,
    	/** Sloan Digitized Sky Survey u-band survey for SkyView. */
    	SDSSU,
    	/** Sloan Digitized Sky Survey z-band survey for SkyView. */
    	SDSSZ,
    	/** Sloan Digitized Sky Survey g-band survey for SkyView. */
    	SDSSDR7G,
    	/** Sloan Digitized Sky Survey i-band survey for SkyView. */
    	SDSSDR7I,
    	/** Sloan Digitized Sky Survey r-band survey for SkyView. */
    	SDSSDR7R,
    	/** Sloan Digitized Sky Survey u-band survey for SkyView. */
    	SDSSDR7U,
    	/** Sloan Digitized Sky Survey z-band survey for SkyView. */
    	SDSSDR7Z,
    	/** The Southern H-Alpha Sky Survey Atlas: Continuum   survey for SkyView. */
    	SHASSA_C,
    	/** The Southern H-Alpha Sky Survey Atlas: Continuum-Corrected   survey for SkyView. */
    	SHASSA_CC,
    	/** The Southern H-Alpha Sky Survey Atlas: H-Alpha   survey for SkyView. */
    	SHASSA_H,
    	/** The Southern H-Alpha Sky Survey Atlas: Smoothed   survey for SkyView. */
    	SHASSA_SM,
    	/** Extreme Ultraviolet Explorer: 171 A survey for SkyView. */
    	EUVE171,
    	/** Extreme Ultraviolet Explorer: 405 A survey for SkyView. */
    	EUVE405,
    	/** Extreme Ultraviolet Explorer: 555 A survey for SkyView. */
    	EUVE555,
    	/** Extreme Ultraviolet Explorer: 83 A survey for SkyView. */
    	EUVE83,
    	/** Galaxy Explorer All Sky Survey: Far UV survey for SkyView. */
    	GALEXFAR,
    	/** Galaxy Explorer All Sky Survey: Near UV survey for SkyView. */
    	GALEXNEAR,
    	/** Southern GOODS Field: VLT VIMOS Observations, U band survey for SkyView. */
    	GOODSVIMOSU,
    	/** The Hawaii Hubble Deep Field North: Band U survey for SkyView. */
    	HAWAIIHDFU,
    	/** ROSAT Wide Field Camera: F1 survey for SkyView. */
    	WFCF1,
    	/** ROSAT Wide Field Camera: F2 survey for SkyView. */
    	WFCF2,
    	/** Swift BAT 70 Month All-Sky Survey: 14-20 keV: flux survey for SkyView. */
    	BAT_FLUX_1,
    	/** Swift BAT 70 Month All-Sky Survey: 14-20 keV: snr survey for SkyView. */
    	BAT_SNR_1,
    	/** Swift BAT 70 Month All-Sky Survey: 20-24 keV: flux survey for SkyView. */
    	BAT_FLUX_2,
    	/** Swift BAT 70 Month All-Sky Survey: 20-24 keV: snr survey for SkyView. */
    	BAT_SNR_2,
    	/** Swift BAT 70 Month All-Sky Survey: 24-35 keV: flux survey for SkyView. */
    	BAT_FLUX_3,
    	/** Swift BAT 70 Month All-Sky Survey: 24-35 keV: snr survey for SkyView. */
    	BAT_SNR_3,
    	/** Swift BAT 70 Month All-Sky Survey: 35-50 keV: flux survey for SkyView. */
    	BAT_FLUX_4,
    	/** Swift BAT 70 Month All-Sky Survey: 35-50 keV: snr survey for SkyView. */
    	BAT_SNR_4,
    	/** Swift BAT 70 Month All-Sky Survey: 50-75 keV: flux survey for SkyView. */
    	BAT_FLUX_5,
    	/** Swift BAT 70 Month All-Sky Survey: 50-75 keV: snr survey for SkyView. */
    	BAT_SNR_5,
    	/** Swift BAT 70 Month All-Sky Survey: 75-100 keV: flux survey for SkyView. */
    	BAT_FLUX_6,
    	/** Swift BAT 70 Month All-Sky Survey: 75-100 keV: snr survey for SkyView. */
    	BAT_SNR_6,
    	/** Swift BAT 70 Month All-Sky Survey: 100-150 keV: flux survey for SkyView. */
    	BAT_FLUX_7,
    	/** Swift BAT 70 Month All-Sky Survey: 100-150 keV: snr survey for SkyView. */
    	BAT_SNR_7,
    	/** Swift BAT 70 Month All-Sky Survey: 150-195 keV: flux survey for SkyView. */
    	BAT_FLUX_8,
    	/** Swift BAT 70 Month All-Sky Survey: 150-195 keV: snr survey for SkyView. */
    	BAT_SNR_8,
    	/** Swift BAT 70 Month All-Sky Survey: 14-195 keV: snr survey for SkyView. */
    	BAT_SNR_SUM,
    	/** GOODS Chandra ACIS: Full band (0.5-8 keV) survey for SkyView. */
    	GOODSACISFB,
    	/** GOODS Chandra ACIS: Hard band (2-8 keV) survey for SkyView. */
    	GOODSACISHB,
    	/** GOODS Chandra ACIS: Soft band (0.5-2 keV) survey for SkyView. */
    	GOODSACISSB,
    	/** GRANAT/SIGMA Flux survey for SkyView. */
    	GRANAT_SIGMA_FLUX,
    	/** GRANAT/SIGMA Significance survey for SkyView. */
    	GRANAT_SIGMA_SIG,
    	/** HEAO 1A survey for SkyView. */
    	HEAO1A,
    	/** ROSAT High Resolution Image Pointed Observations Mosaic: Intensity survey for SkyView. */
    	HRIINT,
    	/** INTEGRAL/Spectral Imager Galactic Center Survey survey for SkyView. */
    	INTEGRALSPI_GC,
    	/** Nine Year INTEGRAL IBIS 17-35 keV Galactic Plane Survey: Exposure survey for SkyView. */
    	INTGAL17_35EXP,
    	/** Nine Year INTEGRAL IBIS 17-35 keV Galactic Plane Survey: Flux survey for SkyView. */
    	INTGAL17_35FLUX,
    	/** Nine Year INTEGRAL IBIS 17-35 keV Galactic Plane Survey: Significance survey for SkyView. */
    	INTGAL17_35SIG,
    	/** Nine Year INTEGRAL IBIS 17-60 keV Galactic Plane Survey: Exposure survey for SkyView. */
    	INTGAL17_60EXP,
    	/** Nine Year INTEGRAL IBIS 17-60 keV Galactic Plane Survey: Flux survey for SkyView. */
    	INTGAL17_60FLUX,
    	/** Nine Year INTEGRAL IBIS 17-60 keV Galactic Plane Survey: Significance survey for SkyView. */
    	INTGAL17_60SIG,
    	/** Nine Year INTEGRAL IBIS 35-80 keV Galactic Plane Survey: Exposure survey for SkyView. */
    	INTGAL35_80EXP,
    	/** Nine Year INTEGRAL IBIS 35-80 keV Galactic Plane Survey: Flux survey for SkyView. */
    	INTGAL35_80FLUX,
    	/** Nine Year INTEGRAL IBIS 35-80 keV Galactic Plane Survey: Significance survey for SkyView. */
    	INTGAL35_80SIG,
    	/** PSPC summed pointed observations, 1 degree cutoff, Counts survey for SkyView. */
    	PSPC1CNT,
    	/** PSPC summed pointed observations, 1 degree cutoff, Exposure survey for SkyView. */
    	PSPC1EXP,
    	/** PSPC summed pointed observations, 1 degree cutoff, Intensity survey for SkyView. */
    	PSPC1INT,
    	/** PSPC summed pointed observations, 2 degree cutoff, Counts survey for SkyView. */
    	PSPC2CNT,
    	/** PSPC summed pointed observations, 2 degree cutoff, Exposure survey for SkyView. */
    	PSPC2EXP,
    	/** PSPC summed pointed observations, 2 degree cutoff, Intensity survey for SkyView. */
    	PSPC2INT,
    	/** PSPC summed pointed observations, 0.6 degree cutoff, Counts survey for SkyView. */
    	PSPC0_6CNT,
    	/** PSPC summed pointed observations, 0.6 degree cutoff, Exposure survey for SkyView. */
    	PSPC0_6EXP,
    	/** PSPC summed pointed observations, 0.6 degree cutoff, Intensity survey for SkyView. */
    	PSPC0_6INT,
    	/** ROSAT All-Sky X-ray Survey Hard Band: Counts survey for SkyView. */
    	RASSHBC,
    	/** ROSAT All-Sky X-ray Survey Broad Band: Counts survey for SkyView. */
    	RASSBBC,
    	/** ROSAT All-Sky X-ray Survey Soft Band: Counts survey for SkyView. */
    	RASSSBC,
    	/** ROSAT All-Sky X-ray Survey Hard Band: Intensity survey for SkyView. */
    	RASSHBI,
    	/** ROSAT All-Sky X-ray Survey Broad Band: Intensity survey for SkyView. */
    	RASSBBI,
    	/** ROSAT All-Sky X-ray Survey Soft Band: Intensity survey for SkyView. */
    	RASSSBI,
    	/** ROSAT All-Sky X-ray Background Survey: Band 1 survey for SkyView. */
    	RASSBCK1,
    	/** ROSAT All-Sky X-ray Background Survey: Band 2 survey for SkyView. */
    	RASSBCK2,
    	/** ROSAT All-Sky X-ray Background Survey: Band 3 survey for SkyView. */
    	RASSBCK3,
    	/** ROSAT All-Sky X-ray Background Survey: Band 4 survey for SkyView. */
    	RASSBCK4,
    	/** ROSAT All-Sky X-ray Background Survey: Band 5 survey for SkyView. */
    	RASSBCK5,
    	/** ROSAT All-Sky X-ray Background Survey: Band 6 survey for SkyView. */
    	RASSBCK6,
    	/** ROSAT All-Sky X-ray Background Survey: Band 7 survey for SkyView. */
    	RASSBCK7,
    	/** RXTE Allsky 3-8 keV Flux survey for SkyView. */
    	RXTE3_8K_FLUX,
    	/** RXTE Allsky 3-8 keV Significance survey for SkyView. */
    	RXTE3_8K_SIG,
    	/** RXTE Allsky 8-20 keV Flux survey for SkyView. */
    	RXTE8_20K_FLUX,
    	/** RXTE Allsky 8-20 keV Significance survey for SkyView. */
    	RXTE8_20K_SIG,
    	/** RXTE Allsky 3-20 keV Flux survey for SkyView. */
    	RXTE3_20K_FLUX,
    	/** RXTE Allsky 3-20 keV Significance survey for SkyView. */
    	RXTE3_20K_SIG,
    	/** CGRO Compton Telescope: 3 channel data survey for SkyView. */
    	COMPTEL,
    	/** Energetic Gamma-Ray Event Telescope: Hard survey for SkyView. */
    	EGRETHARD,
    	/** Energetic Gamma-Ray Event Telescope: Soft survey for SkyView. */
    	EGRETSOFT,
    	/** Energetic Gamma-Ray Event Telescope: 10 channel data survey for SkyView. */
    	EGRET3D,
    	/** Fermi Map: Band 1 survey for SkyView. */
    	FERMI1,
    	/** Fermi Map: Band 2 survey for SkyView. */
    	FERMI2,
    	/** Fermi Map: Band 3 survey for SkyView. */
    	FERMI3,
    	/** Fermi Map: Band 4 survey for SkyView. */
    	FERMI4,
    	/** Fermi Map: Band 5 survey for SkyView. */
    	FERMI5,
     };
    /**
     * Surveys for SkyView.
     */
     public static final String[] SKYVIEW_SURVEYS = new String[] {"BONN_1420MHZ", "HI_480MHZ", "GB6_4850MHZ", "CDFSLESS", "CO2D", "EBHIS", "FIRST", "GOODSNVLA", "GTEE_0035MHZ", "NH", "NVSS", "STRIPE82VLA", "SUMSS", "TGSS", "VLSSR", "WENSS", "PLANCK353", "PLANCK217", "PLANCK143", "PLANCK545", "PLANCK030", "PLANCK100", "PLANCK070", "PLANCK857", "PLANCK044", "H_2MASS", "J_2MASS", "K_2MASS", "AKARIN160", "AKARIN60", "AKARIWIDE_L", "AKARIWIDE_S", "COBEAAM", "COBEZSMA", "DSS2IR", "GOODSHERSCHEL1", "GOODSHERSCHEL2", "GOODSHERSCHEL3", "GOODSHERSCHEL4", "GOODSHERSCHEL5", "GOODSIRAC1", "GOODSIRAC2", "GOODSIRAC3", "GOODSIRAC4", "GOODSISAACH", "GOODSISAACJ", "GOODSISAACKS", "GOODSMIPS", "GOODSNICMOS", "HAWAIIHDFI", "HAWAIIHDFZ", "HUDFISAAC", "IRAS100", "IRAS12", "IRAS25", "IRAS60", "IRIS100", "IRIS12", "IRIS25", "IRIS60", "SFD100M", "SFDDUST", "UKIDSS_H", "UKIDSS_J", "UKIDSS_K", "UKIDSS_Y", "WISEW1", "WISEW2", "WISEW3", "WISEW4", "WMAPK", "WMAPKA", "WMAPQ", "WMAPV", "WMAPW", "WMAPILC", "DSSOLD", "DSS1B", "DSS1R", "DSS2B", "DSS2R", "GOODSACSB", "GOODSACSI", "GOODSACSV", "GOODSACSZ", "GOODSVIMOSR", "HALPHA", "HAWAIIHDFB", "HAWAIIHDFR", "HAWAIIHDFV0201", "HAWAIIHDFV0401", "MELLINGER_B", "MELLINGER_G", "MELLINGER_R", "NEAT", "SDSSG", "SDSSI", "SDSSR", "SDSSU", "SDSSZ", "SDSSDR7G", "SDSSDR7I", "SDSSDR7R", "SDSSDR7U", "SDSSDR7Z", "SHASSA_C", "SHASSA_CC", "SHASSA_H", "SHASSA_SM", "EUVE171", "EUVE405", "EUVE555", "EUVE83", "GALEXFAR", "GALEXNEAR", "GOODSVIMOSU", "HAWAIIHDFU", "WFCF1", "WFCF2", "BAT_FLUX_1", "BAT_SNR_1", "BAT_FLUX_2", "BAT_SNR_2", "BAT_FLUX_3", "BAT_SNR_3", "BAT_FLUX_4", "BAT_SNR_4", "BAT_FLUX_5", "BAT_SNR_5", "BAT_FLUX_6", "BAT_SNR_6", "BAT_FLUX_7", "BAT_SNR_7", "BAT_FLUX_8", "BAT_SNR_8", "BAT_SNR_SUM", "GOODSACISFB", "GOODSACISHB", "GOODSACISSB", "GRANAT_SIGMA_FLUX", "GRANAT_SIGMA_SIG", "HEAO1A", "HRIINT", "INTEGRALSPI_GC", "INTGAL17_35EXP", "INTGAL17_35FLUX", "INTGAL17_35SIG", "INTGAL17_60EXP", "INTGAL17_60FLUX", "INTGAL17_60SIG", "INTGAL35_80EXP", "INTGAL35_80FLUX", "INTGAL35_80SIG", "PSPC1CNT", "PSPC1EXP", "PSPC1INT", "PSPC2CNT", "PSPC2EXP", "PSPC2INT", "PSPC0_6CNT", "PSPC0_6EXP", "PSPC0_6INT", "RASSHBC", "RASSBBC", "RASSSBC", "RASSHBI", "RASSBBI", "RASSSBI", "RASSBCK1", "RASSBCK2", "RASSBCK3", "RASSBCK4", "RASSBCK5", "RASSBCK6", "RASSBCK7", "RXTE3_8K_FLUX", "RXTE3_8K_SIG", "RXTE8_20K_FLUX", "RXTE8_20K_SIG", "RXTE3_20K_FLUX", "RXTE3_20K_SIG", "COMPTEL", "EGRETHARD", "EGRETSOFT", "EGRET3D", "FERMI1", "FERMI2", "FERMI3", "FERMI4", "FERMI5", };

     /**
      * Main suggested surveys for SkyView as an array of length 6: Gamma rays, X Rays, UV, optical, IR, Radio.
      */
     public static final String[] SKYVIEW_SUGGESTED_SURVEYS = new String[] {SKYVIEW_SURVEYS[SKYVIEW_SURVEY.EGRETHARD.ordinal()], SKYVIEW_SURVEYS[SKYVIEW_SURVEY.PSPC1CNT.ordinal()],
    	 SKYVIEW_SURVEYS[SKYVIEW_SURVEY.EUVE83.ordinal()], SKYVIEW_SURVEYS[SKYVIEW_SURVEY.DSSOLD.ordinal()], SKYVIEW_SURVEYS[SKYVIEW_SURVEY.K_2MASS.ordinal()],
    	 SKYVIEW_SURVEYS[SKYVIEW_SURVEY.BONN_1420MHZ.ordinal()]};
     // Last 2 could also be IRIS100 and FIRST

    /**
     * Returns the query string for SkyView.
     * @param name Object name to be solved by Simbad or NED. You can also set as source the RA and
     * DEC (2 values) in degrees.
     * @param survey Survey.
     * @param field Field of view in degrees.
     * @param width Width (or height, the same) of the image in pixels.
     * @param invert True to show inverted levels.
     * @param grid True to show the grid.
     * @param scoord Coordinate type. May be null (J2000).
     * @param sproj Projection type. May be null (TAN).
     * @param slut LUT color table. May be null (Gray image).
     * @param sscale Scale for the levels. May be null (Log).
     * @param catalog Name of one or several Vizier catalogs (separated by comma) to request
     * and draw sources. Can be null.
     * @param contours Draw image contours from one survey on top of another. A value of the
     * contour setting may comprise up to 5 colon separated fields. The first is the survey
     * from which the contours are to be drawn. The second is the scaling of the contours,
     * either 'Log', 'Sqrt' or 'Linear' (default Linear). The third value is the number of
     * contours (default 4) which is followed by values for the first and last contours. One,
     * two, three or all five values may be specified. More than one survey can be contoured
     * by using comma as a separator. Example: 408mhz:Log:6:1:1000. Can be null.
     * @return The query for a PNG image.
     * @throws JPARSECException if the name or the survey is null.
     */
    public static String getQueryToSkyView(String name, SKYVIEW_SURVEY survey, double field, int width, boolean invert, boolean grid,
    		SKYVIEW_COORDINATE scoord, SKYVIEW_PROJECTION sproj, SKYVIEW_LUT_TABLE slut, SKYVIEW_INTENSITY_SCALE sscale,
    		String catalog, String contours)
    throws JPARSECException {
    	if (name == null || survey == null) throw new JPARSECException("invalid input.");
		name = DataSet.replaceAll(name, " ", "+", true);
    	String position = "Position="+name;
    	String s = "Survey="+SKYVIEW_SURVEYS[survey.ordinal()];
    	String coordinates = "", projection = "", inver = "", lu = "", rgb = "", scaling = "";

    	String coord = null, proj = null, lut = null, scale = null;
    	if (scoord != null) coord = SKYVIEW_COORDINATES[scoord.ordinal()];
    	if (sproj != null) proj = SKYVIEW_PROJECTIONS[sproj.ordinal()];
    	if (slut != null) lut = SKYVIEW_LUT[slut.ordinal()];
    	if (sscale != null) scale = SKYVIEW_SCALING[sscale.ordinal()];

    	if (coord != null && !coord.equals("")) coordinates = "Coordinates="+coord+"&";
    	if (proj != null && !proj.equals("")) projection = "Projection="+FileIO.getField(1, proj, " ", true)+"&";
    	if (lut != null && !lut.equals("")) {
    		lu = "lut="+lut+"&";
    		if (lut.toLowerCase().equals("gray")) lu = "";
    	}
    	if (invert) inver = "invert=0&";
    	String retur = "Return=PNG&";
    	if (scale != null && !scale.equals("")) scaling = "Scaling="+scale+"&";
    	String fieldView = "Size="+field+"&";
    	String size = "Pixels="+width;
    	if (field == 360.0) {
    		fieldView = "Size=360,180&";
    		size = "Pixels="+width+","+(width/2)+"&";
    	}
    	String gri = "";
    	if (grid) gri = "grid&gridlabels&";
    	String cat = "";
    	if (catalog != null && !catalog.equals("")) cat = "catalog="+catalog+"&catalogIDs&";
    	String con = "";
    	if (contours != null && !contours.equals("")) con = "contour="+contours+"&";

		String query = "https://skyview.gsfc.nasa.gov/cgi-bin/images?";
		query += position+"&"+s+"&"+coordinates+projection+retur+scaling+rgb+inver+lu+gri+cat+con+fieldView+size;
		return query;
    }
}
