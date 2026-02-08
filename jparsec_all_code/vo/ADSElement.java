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

import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.util.JPARSECException;

/**
 * A class to manipulate ADS articles.
 * @author T. Alonso Albi - OAN (Spain)
 * @since version 1.0
 */
public class ADSElement implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Publication year.
	 */
	public int year;
	/**
	 * Publication journal name, in abbreviated form.
	 */
	public String journal;
	/**
	 * Volume number or type (not always a number).
	 */
	public String volume;
	/**
	 * Publication type, like Letter. Only the first character is used.
	 */
	public String publicationType;
	/**
	 * Page number.
	 */
	public int page;
	/**
	 * Author name.
	 */
	public String author;
	/**
	 * Paper title. This data is sometimes not available.
	 */
	public String title;
	
	private String bibTex;

	/**
	 * Constructor for a bibtex reference.
	 * @param year Year.
	 * @param journal Journal.
	 * @param volume Volume.
	 * @param publicationType Publication.
	 * @param page Page.
	 * @param author Author.
	 */
	public ADSElement(int year, String journal, int volume, String publicationType, int page, String author)
	{
		this.year = year;
		this.journal = journal;
		this.volume = ""+volume;
		this.publicationType = publicationType;
		this.page = page;
		this.author = author;
	}

	/**
	 * Constructs an ADS object from a bibtex reference.
	 * @param bibtex The bibtex entry, for instance 2011sf2a.conf..413A.
	 */
	public ADSElement(String bibtex) {
		int year = Integer.parseInt(bibtex.substring(0, 4));
		String journal = bibtex.substring(4, 9);
		String vol = bibtex.substring(9, 13);
		String publicationType = bibtex.substring(13, 14);
		String p = bibtex.substring(14, 18);
		String author = bibtex.substring(18, 19);
		vol = DataSet.replaceAll(vol, ".", "", true);
		journal = DataSet.replaceAll(journal, ".", "", true);
		p = DataSet.replaceAll(p, ".", "", true);
		publicationType = DataSet.replaceAll(publicationType, ".", "", true);
		int page = Integer.parseInt(p);
		if (publicationType.equals(".")) publicationType = null;

		this.year = year;
		this.journal = journal;
		this.volume = vol;
		this.publicationType = publicationType;
		this.page = page;
		this.author = author;
	}
	/**
	 * Obtains the bib code or the closest possible match
	 * if no full information is available.
	 * @return Bib code.
	 */
	public String getBibCode()
	{
		if (journal != null) {
			journal = FileIO.addSpacesAfterAString(journal, 5);
			journal = DataSet.replaceAll(journal, " ", ".", false);
		} else {
			journal = "";
		}
		String volumeS = "";
		volumeS = FileIO.addSpacesBeforeAString(""+volume, 4);
		volumeS = DataSet.replaceAll(volumeS, " ", ".", false);
		if (volumeS.equals("..ph")) volumeS = ".ph.";
		String pageS = "";
		pageS = FileIO.addSpacesBeforeAString(""+page, 4);
		pageS = DataSet.replaceAll(pageS, " ", ".", false);
		if (journal.toLowerCase().equals("arxiv")) pageS = DataSet.replaceAll(pageS, ".", "0", false);
		if (publicationType == null || publicationType.equals("")) {
			publicationType = ".";
		}
		if (author != null) {
			author = author.substring(0, 1);
		} else {
			author = "";
		}

		String bib = ""+year;
		if (!journal.equals("")) {
			bib += journal;
			if (!volumeS.equals("")) {
				bib += volumeS;
//				if (!publicationType.equals("")) {
					bib += publicationType.substring(0, 1);
					if (!pageS.equals("")) {
						bib += pageS;
						if (!author.equals("")) bib += author;
					}
//				}
			}
		}
		return bib;
	}

	/**
	 * Returns bibtex entry for the article. Requires a call
	 * to {@linkplain ADSQuery}, but only the first time is called.
	 * @return BibTex entry.
	 * @throws JPARSECException If an error occurs.
	 */
	public String getBibTexEntry()
	throws JPARSECException {
		if (bibTex == null)
		{
	    	try {
	    		String query = ADSQuery.ADS_HARVARD_URL;
	    		query += ADSQuery.addParameter(ADSQuery.PARAMETER.BIBCODE, this.getBibCode());
	    		query += "&" +ADSQuery.addParameter(ADSQuery.PARAMETER.DATATYPE, ADSQuery.DATATYPE.BIBTEX.getType());

	    		String out = ADSQuery.query(query);
	    		String o[] = DataSet.toStringArray(out, FileIO.getLineSeparator());
	    		this.bibTex = "";
	    		boolean begin = false;
	    		for (int i=0; i<o.length; i++)
	    		{
	    			if (o[i].toLowerCase().startsWith("@article")) begin = true;
	    			if (begin) this.bibTex += o[i] + FileIO.getLineSeparator();
	    			if (o[i].toLowerCase().startsWith("}")) {
	    				break;
	    			}
	    		}
	    	} catch (Exception e)
	    	{
	    		throw new JPARSECException("Error retieving bibtex entry.", e);
	    	}
		}

		return bibTex;
	}

	/**
	 * Obtains the abstract for this bibtex instance.
	 * @return Abstract.
	 * @throws JPARSECException If an error occurs.
	 */
	public String getAbstract()
	throws JPARSECException {
		String abs = "";
		String query = ADSQuery.ADS_HARVARD_ABSTRACTS_URL;

		query += this.getBibCode();
   		String out = ADSQuery.query(query);
   		String o[] = DataSet.toStringArray(out, FileIO.getLineSeparator());
   		boolean begin = false;
   		for (int i=0; i<o.length; i++)
   		{
   			if (begin && (o[i].startsWith("<hr>") || o[i].indexOf("table") >= 0)) break;
   			if (begin) abs += o[i] + FileIO.getLineSeparator();
   			if (o[i].indexOf("Abstract</h3>") >= 0) begin = true;
   		}
   		return abs;
	}

	/**
	 * Obtains the abstract for this bibtex instance.
	 * @param bibEntry The bibliographic entry.
	 * @return Abstract.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String getAbstract(String bibEntry)
	throws JPARSECException {
		String abs = "";
		String query = ADSQuery.ADS_HARVARD_ABSTRACTS_URL;

		query += bibEntry;
   		String out = ADSQuery.query(query);
   		String o[] = DataSet.toStringArray(out, FileIO.getLineSeparator());
   		boolean begin = false;
   		for (int i=0; i<o.length; i++)
   		{
   			if (begin && (o[i].startsWith("<hr>") || o[i].indexOf("table") >= 0)) break;
   			if (begin) abs += o[i] + FileIO.getLineSeparator();
   			if (o[i].indexOf("Abstract</h3>") >= 0) begin = true;
   		}
   		return abs;
	}

	/**
	 * Obtains the article for this bibtex object. If the article requires
	 * subscription, no file will be created.
	 * @param fileName Name of the .pdf file to create.
	 * @param timeout Timeout in milliseconds. If the connection waits
	 * for data for more than this time an exception will be thrown.
	 * @throws JPARSECException If an error occurs, for example if no full .pdf
	 * article is available.
	 */
	public void getArticle(String fileName, int timeout)
	throws JPARSECException {
		String query = "http://adsabs.harvard.edu/cgi-bin/nph-data_query?bibcode=";
		query += DataSet.replaceAll(this.getBibCode(), "&", "%26", true);
		query += "&db_key=AST&link_type=ARTICLE";

		GeneralQuery.queryFile(query, fileName, timeout);
	}

	/**
	 * ID constant for A&amp;A journal.
	 */
	public static final String JOURNAL_ASTRONOMY_AND_ASTROPHYSICS = "A&A";
	/**
	 * ID constant for ApJ journal.
	 */
	public static final String JOURNAL_ASTROPHYSICAL_JOURNAL = "ApJ";
	/**
	 * ID constant for ApJSS journal.
	 */
	public static final String JOURNAL_ASTROPHYSICAL_JOURNAL_SUPPLEMENT_SERIES = "ApJS";
	/**
	 * ID constant for AJ journal.
	 */
	public static final String JOURNAL_ASTRONOMICAL_JOURNAL = "AJ";
	/**
	 * ID constant for MNRAS journal.
	 */
	public static final String JOURNAL_MNRAS = "MNRAS";
	/**
	 * ID constant for Ap&amp;SS journal.
	 */
	public static final String JOURNAL_ASTRPHYSICS_AND_SPACE_SCIENCE = "Ap&SS";

	/**
	 * ID constant for a letter publication.
	 */
	public static final String PUBLICATION_TYPE_LETTER = "L";
	/**
	 * ID constant for an article.
	 */
	public static final String PUBLICATION_TYPE_ARTICLE = ".";

	private ADSElement() {}

	/**
	 * Clones this instance.
	 */
	@Override
	public ADSElement clone()
	{
		ADSElement s = new ADSElement();
		s.author = this.author;
		s.bibTex = this.bibTex;
		s.journal = this.journal;
		s.page = this.page;
		s.publicationType = this.publicationType;
		s.volume = this.volume;
		s.year = this.year;
		s.title = this.title;
		return s;
	}
	/**
	 * Returns true if the input object is equals to this instance.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ADSElement)) return false;

		ADSElement that = (ADSElement) o;

		if (year != that.year) return false;
		if (page != that.page) return false;
		if (journal != null ? !journal.equals(that.journal) : that.journal != null) return false;
		if (volume != null ? !volume.equals(that.volume) : that.volume != null) return false;
		if (title != null ? !title.equals(that.title) : that.title != null) return false;
		if (publicationType != null ? !publicationType.equals(that.publicationType) : that.publicationType != null)
			return false;
		if (author != null ? !author.equals(that.author) : that.author != null) return false;

		return !(bibTex != null ? !bibTex.equals(that.bibTex) : that.bibTex != null);
	}

	@Override
	public int hashCode() {
		int result = year;
		result = 31 * result + (title != null ? title.hashCode() : 0);
		result = 31 * result + (journal != null ? journal.hashCode() : 0);
		result = 31 * result + (volume != null ? volume.hashCode() : 0);
		result = 31 * result + (publicationType != null ? publicationType.hashCode() : 0);
		result = 31 * result + page;
		result = 31 * result + (author != null ? author.hashCode() : 0);
		result = 31 * result + (bibTex != null ? bibTex.hashCode() : 0);
		return result;
	}
}
