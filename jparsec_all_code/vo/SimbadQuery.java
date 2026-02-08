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

import java.io.Serializable;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.StringTokenizer;

import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.math.Constant;
import jparsec.util.JPARSECException;

/**
 * A class to solve objects using Simbad.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class SimbadQuery implements Serializable {
	private static final long serialVersionUID = 1L;

	private String query;
	/**
	 * Constructor to query for a given object.
	 * @param name Object.
	 */
	public SimbadQuery(String name)
	{
		this.query = name;
	}
	/**
	 * Perform the query.
	 * @return Response from server as a Simbad object.
	 * @throws JPARSECException If an error occurs.
	 */
	public SimbadElement query()
	throws JPARSECException {
		SimbadElement q = query(this.query);
		return q;
	}

	/**
	 * Call simbad and obtain data for an object.
	 * @param name Object name.
	 * @return Simbad object, null if it is not found.
	 * @throws JPARSECException If an error occurs.
	 */
	public static SimbadElement query(String name)
	throws JPARSECException {
		try {
			String qs = "http://cdsweb.u-strasbg.fr/cgi-bin/nph-sesame/?"+URLEncoder.encode(name, ReadFile.ENCODING_UTF_8);
			return parsePlainTextFromSimbad(GeneralQuery.query(qs, 
					jparsec.util.Configuration.QUERY_TIMEOUT));
		} catch (Exception e)
		{
			e.printStackTrace();
			throw new JPARSECException("Simbad could not solve "+name, e);
		}
	}

	private static SimbadElement parsePlainTextFromSimbad(String text)
	{
		if (text == null) return null;
		if (text.equals("")) return null;
		if (text.indexOf("#=V=VizieR") >= 0) return null; // Usually wrong data when result contains VizieR and not #=S=Simbad

		SimbadElement oe = new SimbadElement();

		oe.spectralType = findHeader(text, "%S ");
		if (oe.spectralType != null) {
			if (oe.spectralType.length() > 2) oe.spectralType = oe.spectralType.substring(0, 2);
			if (oe.spectralType.endsWith(".")) oe.spectralType = oe.spectralType.substring(0,1)+"0";
		}

		oe.type = findHeader(text, "%T ");
		String mb = findHeader(text, "%M.B ");
		String mv = findHeader(text, "%M.V ");
		if (mb != null && mv != null) {
			oe.bMinusV = (float) (DataSet.parseDouble(FileIO.getField(1, mb, " ", true)) - DataSet.parseDouble(FileIO.getField(1, mv, " ", true)));
		} else {
			oe.bMinusV = SimbadElement.B_MINUS_V_UNAVAILABLE;
		}

		String pos = findHeader(text, "%J ");
		if (pos == null) return null;
		oe.rightAscension = oe.declination = 0;
		try {
			oe.rightAscension = DataSet.parseDouble(FileIO.getField(1, pos, " ", true)) * Constant.DEG_TO_RAD;
		} catch (Exception exc) {
			try {
				JPARSECException.addWarning("Could not parse right ascension from Simbad: "+FileIO.getField(1, pos, " ", true));
			} catch (JPARSECException e) {
				exc.printStackTrace();
			}
		}
		try {
			oe.declination = DataSet.parseDouble(FileIO.getField(2, pos, " ", true)) * Constant.DEG_TO_RAD;
		} catch (Exception exc) {
			try {
				JPARSECException.addWarning("Could not parse declination from Simbad: "+FileIO.getField(2, pos, " ", true));
			} catch (JPARSECException e) {
				exc.printStackTrace();
			}
		}
		String name = FileIO.getField(1, findHeader(text, "# "), "#", true).trim();
		oe.name = name;
		oe.otherNames = findHeaders(text, "%I ");

		String mov = findHeader(text, "%P ");
		String rad = findHeader(text, "%V z");
		if (mov != null) {
			oe.properMotionRA = (float) (DataSet.parseDouble(FileIO.getField(1, mov, " ", true)) * 0.001 / (Constant.RAD_TO_ARCSEC * Math.cos(oe.declination)));
			oe.properMotionDEC = (float) (DataSet.parseDouble(FileIO.getField(2, mov, " ", true)) * 0.001 / Constant.RAD_TO_ARCSEC);
		}
		if (rad != null) {
			oe.properMotionRadialV = (float) (DataSet.parseDouble(FileIO.getField(1, rad, " ", true)) * Constant.SPEED_OF_LIGHT / 1000.0);
		} else {
			rad = findHeader(text, "%V v");
			if (rad != null)
				oe.properMotionRadialV = (float) (DataSet.parseDouble(FileIO.getField(1, rad, " ", true)));
		}

		String par = findHeader(text, "%X ");
		if (par != null)
			oe.parallax = Math.abs((float) (DataSet.parseDouble(FileIO.getField(1, par, " ", true))));

		return oe;
	}

	private static String findHeader(String text, String header)
	{
		String out = null;
		StringTokenizer tok = new StringTokenizer(text, FileIO.getLineSeparator());
		while(tok.hasMoreTokens())
		{
			String a = tok.nextToken();
			if (a.startsWith(header)) out = a.substring(header.length()).trim();
		}

		if (out != null && out.startsWith(":")) out = out.substring(1).trim();
		if (out != null && out.startsWith("~")) out = out.substring(1).trim();
		return out;
	}
	private static String[] findHeaders(String text, String header)
	{
		ArrayList<String> v = new ArrayList<String>();
		StringTokenizer tok = new StringTokenizer(text, FileIO.getLineSeparator());
		while(tok.hasMoreTokens())
		{
			String a = tok.nextToken();
			if (a.startsWith(header)) v.add(a.substring(header.length()).trim());
		}

		if (v.size() < 1) return null;
		return DataSet.arrayListToStringArray(v);
	}
}
