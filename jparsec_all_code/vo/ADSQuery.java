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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;

import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.util.JPARSECException;

/**
 * A class to send queries to ADS abstracts service.<BR>
 *
 * A simple example to retrieve a bibtex entry is:<BR>
 *
 *     	try {<BR>
 *     		query = ADSQuery.ADS_HARVARD_URL;<BR>
 *    		query += ADSQuery.addParameter(ADSQuery.PARAMETER.BIBCODE, "2007A&amp;A...470..625D");<BR>
 *    		query += "&amp;" +ADSQuery.addParameter(ADSQuery.PARAMETER.DATATYPE, ADSQuery.DATATYPE.BIBTEX.getType());<BR>
 *<BR>
 *    		String out = ADSQuery.query(query);<BR>
 *    		System.out.println(out);<BR>
 *    	} catch (Exception e)<BR>
 *   	{<BR>
 *    		e.printStackTrace();<BR>
 *    	}<BR>
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class ADSQuery implements Serializable {
	private static final long serialVersionUID = 1L;

	private String query;
	/**
	 * Constructor for a given query.
	 * @param query The query.
	 */
	public ADSQuery(String query)
	{
		this.query = query;
	}
	/**
	 * Constructor for a default query.
	 */
	public ADSQuery()
	{
		this.query = ADS_HARVARD_URL;
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
	 * Adds a parameter to the query.
	 * @param parameter Parameter ID constant.
	 * @param value Value of the parameter.
	 * @throws JPARSECException If an error occurs.
	 */
	public void addParameterToQuery(PARAMETER parameter, String value)
	throws JPARSECException {
		String add = addParameter(parameter, value);
		if (!this.query.endsWith("?")) add = "&"+add;
		this.query += add;
	}

	/**
	 * Returns the adequate string to add the author.
	 * @param value Author.
	 * @throws JPARSECException If an error occurs.
	 */
	public void addAuthor(String value)
	throws JPARSECException {
		addParameterToQuery(ADSQuery.PARAMETER.AUTHOR, value);
	}
	/**
	 * Returns the adequate string to add the abstract.
	 * @param value Abstract.
	 * @throws JPARSECException If an error occurs.
	 */
	public void addAbstract(String value)
	throws JPARSECException {
		addParameterToQuery(ADSQuery.PARAMETER.ABSTRACT, value);
	}
	/**
	 * Returns the adequate string to add the bibcode.
	 * @param value Bibcode.
	 * @throws JPARSECException If an error occurs.
	 */
	public void addBibCode(String value)
	throws JPARSECException {
		addParameterToQuery(ADSQuery.PARAMETER.BIBCODE, value);
	}
	/**
	 * Returns the adequate string to add the datatype.
	 * @param value Datatype.
	 * @throws JPARSECException If an error occurs.
	 */
	public void addDataType(String value)
	throws JPARSECException {
		addParameterToQuery(ADSQuery.PARAMETER.DATATYPE, value);
	}
	/**
	 * Returns the adequate string to add the end year.
	 * @param value End year.
	 * @throws JPARSECException If an error occurs.
	 */
	public void addEndYear(String value)
	throws JPARSECException {
		addParameterToQuery(ADSQuery.PARAMETER.END_YEAR, value);
	}
	/**
	 * Returns the adequate string to add the start year.
	 * @param value Start year.
	 * @throws JPARSECException If an error occurs.
	 */
	public void addStartYear(String value)
	throws JPARSECException {
		addParameterToQuery(ADSQuery.PARAMETER.START_YEAR, value);
	}
	/**
	 * Returns the adequate string to add the object.
	 * @param value Object.
	 * @throws JPARSECException If an error occurs.
	 */
	public void addObject(String value)
	throws JPARSECException {
		addParameterToQuery(ADSQuery.PARAMETER.OBJECT, value);
	}
	/**
	 * Returns the adequate string to add the Title.
	 * @param value Title.
	 * @throws JPARSECException If an error occurs.
	 */
	public void addTitle(String value)
	throws JPARSECException {
		addParameterToQuery(ADSQuery.PARAMETER.TITLE, value);
	}

    /**
     * URL for ADS service in Harvard University.
     */
	public static final String ADS_HARVARD_URL = "http://adsabs.harvard.edu/cgi-bin/nph-abs_connect?";
	/**
     * URL for the abstracts of the ADS service in Harvard University.
	 */
	public static final String ADS_HARVARD_ABSTRACTS_URL = "http://adsabs.harvard.edu/abs/";
	/**
	 * The set of parameters for the query.
	 */
	public enum PARAMETER {
		/** ID code for author name parameter search. */
		AUTHOR,
		/** ID code for title name parameter search. */
		TITLE,
		/** ID code for abstract words parameter search. */
		ABSTRACT,
		/** ID code for datatype parameter. */
		DATATYPE,
		/** ID code for start year parameter search. */
		START_YEAR,
		/** ID code for end year parameter search. */
		END_YEAR,
		/** ID code for bibcode parameter search. */
		BIBCODE,
		/** ID code for object name parameter search. */
		OBJECT
	};

	/**
	 * The set of data types for the query.
	 */
	public enum DATATYPE {
		/** ID code for html returning datatype. */
		HTML ("HTML"),
		/** ID code for plain text returning datatype. */
		TEXT ("PLAINTEXT"),
		/** ID code for bibtex returning datatype. */
		BIBTEX ("BIBTEX"),
		/** ID code for votable returning datatype. */
		VOTABLE ("VOTABLE"),
		/** ID code for portable returning datatype. */
		PORTABLE ("PORTABLE");

		private String type;

		private DATATYPE(String t) {
			this.type = t;
		}

		/**
		 * Returns the type ID code.
		 * @return The type code to use in the query.
		 */
		public String getType() {
			return type;
		}
	};

	/**
	 * Allows to add a parameter to the query.<BR>
	 * From the second parameter in necessary to add the ampersand '&amp;' to separate
	 * the different parameters.
	 * @param parameter Parameter ID.
	 * @param value Parameter value.
	 * @return Code for the added parameter.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String addParameter(PARAMETER parameter, String value)
	throws JPARSECException {
		String name = "";
		switch (parameter)
		{
		case AUTHOR:
			name = "author";
			break;
		case TITLE:
			name = "title";
			break;
		case ABSTRACT:
			name = "text";
			break;
		case DATATYPE:
			name = "data_type";
			break;
		case START_YEAR:
			name = "start_year";
			break;
		case END_YEAR:
			name = "end_year";
			break;
		case BIBCODE:
			name = "bibcode";
			value = DataSet.replaceAll(value, "&", "%26", true);
			break;
		case OBJECT:
			name = "object";
			break;
		default:
			throw new JPARSECException("invalid parameter.");
		}
		name += "="+value;
		return name;
	}

	/**
	 * Perform the query.
	 * @param query Query to call.
	 * @return Response from server.
	 * @throws JPARSECException If an error occurs.
	 */
    public static String query(String query)
    throws JPARSECException
    {
		String output = "";
    	try {
			URL urlObject = new URL(query);
			URLConnection con = urlObject.openConnection();
			con.setRequestProperty
			  ( "User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)" );

			// Get the response
	        BufferedReader in = new BufferedReader(
	                      new InputStreamReader(
	                      con.getInputStream()));
			String inputLine;

			while ((inputLine = in.readLine()) != null)
			{
				output += inputLine + FileIO.getLineSeparator();
			}
			in.close();
    	} catch (Exception e)
    	{
    		throw new JPARSECException(e);
    	}

		return output;
    }

    /**
     * Returns a query to retrieve all articles for a given author.
     * @param authorName Author name, for instance "Alonso-Albi, T.".
     * @param year0 The initial year for the search, for instance 2000.
     * @param yearf The final year for the search.
     * @return The string query.
     * @throws JPARSECException If an error occurs.
     */
    public static String getAuthorQuery(String authorName, int year0, int yearf) throws JPARSECException {
    	try {
			String author = URLEncoder.encode(authorName, ReadFile.ENCODING_UTF_8);
			String query = "http://adsabs.harvard.edu/cgi-bin/nph-abs_connect?db_key=AST&db_key=PRE&qform=AST&arxiv_sel=astro-ph&arxiv_sel=cond-mat&arxiv_sel=cs&arxiv_sel=gr-qc&arxiv_sel=hep-ex&arxiv_sel=hep-lat&arxiv_sel=hep-ph&arxiv_sel=hep-th&arxiv_sel=math&arxiv_sel=math-ph&arxiv_sel=nlin&arxiv_sel=nucl-ex&arxiv_sel=nucl-th&arxiv_sel=physics&arxiv_sel=quant-ph&arxiv_sel=q-bio&sim_query=YES&ned_query=YES&adsobj_query=YES&aut_logic=OR&obj_logic=OR&author="+author+"&object=&start_mon=&start_year="+year0+"&end_mon=&end_year="+yearf+"&ttl_logic=OR&title=&txt_logic=OR&text=&nr_to_return=200&start_nr=1&jou_pick=ALL&ref_stems=&data_and=ALL&group_and=ALL&start_entry_day=&start_entry_mon=&start_entry_year=&end_entry_day=&end_entry_mon=&end_entry_year=&min_score=&sort=SCORE&data_type=BIBTEX&aut_syn=YES&ttl_syn=YES&txt_syn=YES&aut_wt=1.0&obj_wt=1.0&ttl_wt=0.3&txt_wt=3.0&aut_wgt=YES&obj_wgt=YES&ttl_wgt=YES&txt_wgt=YES&ttl_sco=YES&txt_sco=YES&version=1";
			return query;
    	} catch (Exception exc) {
    		throw new JPARSECException("Could not create the query", exc);
    	}
    }

    /**
     * Returns the ADS elements for all the articles of a given author and year range, including 
     * the titles of the Papers.
     * @param authorName Author name, for instance "Alonso-Albi, T.".
     * @param year0 The initial year for the search, for instance 2000.
     * @param yearf The final year for the search.
     * @return The ADS objects representing the articles.
     * @throws JPARSECException If an error occurs.
     */    
    public static ADSElement[] getArticles(String authorName, int year0, int yearf) throws JPARSECException {
		String out = ADSQuery.query(ADSQuery.getAuthorQuery(authorName, year0, yearf));
		
		// Parse the output to get the abstracts and download the PDFs	
		String o[] = DataSet.toStringArray(out, FileIO.getLineSeparator(), true);
		ArrayList<Object> list = new ArrayList<Object>();
		ADSElement ads = null;
		for (int i=0; i<o.length; i++) {
			if (o[i].trim().startsWith("@")) {
				int n = o[i].indexOf("{");
				String abs = o[i].substring(n+1);
				if (abs.endsWith(",")) abs = abs.substring(0, abs.length()-1);
				ads = new ADSElement(abs);
			}
			if (o[i].trim().startsWith("title") && ads != null) {
				String title = o[i].substring(o[i].indexOf("{")+1);
				title = title.substring(0, title.lastIndexOf("}"));
				ads.title = title;
				list.add(ads);
				ads = null;
			}
		}
		return (ADSElement[]) DataSet.toObjectArray(list);
    }
}
