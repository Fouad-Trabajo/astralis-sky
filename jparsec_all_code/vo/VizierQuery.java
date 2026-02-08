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

import java.io.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;

import org.w3c.dom.Document;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import cds.savot.model.*;

import jparsec.math.Constant;
import jparsec.io.FileIO;
import jparsec.io.HTMLReport;
import jparsec.io.LATEXReport;
import jparsec.io.HTMLReport.SIZE;
import jparsec.io.HTMLReport.STYLE;
import jparsec.io.ReadFile;
import jparsec.observer.LocationElement;
import jparsec.util.JPARSECException;
import jparsec.vo.GeneralQuery;
import jparsec.vo.VizierElement;

/**
 * A class to call Vizier to retrieve information of astronomical catalogs.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class VizierQuery implements Serializable {
	private static final long serialVersionUID = 1L;

	private String catalogName, objectName;
	private double radius;
	private boolean coneSearch = true;

	/**
	 * Constructor for a given cone search query.
	 * @param object The object.
	 * @param catalog The catalog. An empty string means a search in all catalogs.
	 * @param radius Radius of the search in arcseconds.
	 */
	public VizierQuery(String object, String catalog, double radius)
	{
		this.catalogName = catalog;
		this.objectName = object;
		this.radius = radius;
		coneSearch = true;
	}

	/**
	 * Constructor for a given query.
	 * @param object The object.
	 * @param catalog The catalog. An empty string means a search in all catalogs.
	 * @param radius Radius of the search in arcseconds.
	 * @param coneSearch True for a cone search (default), false for a box search type.
	 */
	public VizierQuery(String object, String catalog, double radius, boolean coneSearch)
	{
		this.catalogName = catalog;
		this.objectName = object;
		this.radius = radius;
		this.coneSearch = coneSearch;
	}

	/**
	 * Perform the query.
	 * @throws JPARSECException If an error occurs.
	 */
	public void query()
	throws JPARSECException {
		results = query(this.objectName, this.catalogName, this.radius, this.coneSearch);
	}

	/**
	 * Transforms the results of a query from Document into a String.
	 * @param doc Document.
	 * @return String representation.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String toString(Document doc) throws JPARSECException{
		try {
	        TransformerFactory tFactory = TransformerFactory.newInstance();
	        Transformer transformer = tFactory.newTransformer();
	        DOMSource source = new DOMSource(doc);
	        StringWriter sw=new StringWriter();
	        StreamResult result = new StreamResult(sw);
	        transformer.transform(source, result);
	        String xmlString=sw.toString();
	        return xmlString;
		} catch (Exception e)
		{
			throw new JPARSECException(e);
		}
    }

	/**
	 * Transforms the results of a query into a Document.
	 * @param votable Query results.
	 * @return Document.
	 * @throws JPARSECException If an error occurs.
	 */
	public static Document toDocument(String votable)
	throws JPARSECException {
		javax.xml.parsers.DocumentBuilderFactory factory =
	        javax.xml.parsers.DocumentBuilderFactory.newInstance();
	    factory.setNamespaceAware(true);
	    javax.xml.parsers.DocumentBuilder builder = null;
	    try {
	        builder = factory.newDocumentBuilder();
	    }
	    catch (javax.xml.parsers.ParserConfigurationException ex) {
	    	throw new JPARSECException("Cannot create a document builder.", ex);
	    }
	    try {
		    InputStream is = new java.io.ByteArrayInputStream(votable.getBytes());
		    org.w3c.dom.Document doc = builder.parse(is);
		    is.close();
		    return doc;
	    } catch (Exception exc) {
	    	throw new JPARSECException("Cannot build document object.", exc);
	    }
	}

	private String results;

	/**
	 * Transforms the results of a query into a VO Table.
	 * @return The VO Table.
	 */
	public SavotVOTable toVOTable()
	{
		SavotVOTable votable = toVOTable(results);
		return votable;
	}

	/**
	 * Reads an external VO Table, useful if no Internet
	 * Connection is available.
	 * @param is The stream.
	 * @throws JPARSECException If an error occurs.
	 */
	public void readExternalVOTable(InputStream is)
	throws JPARSECException {
		VOTable vo = new VOTable(is);
		results = vo.getVOTableAsString();
	}

	/**
	 * Reads an external VO Table, useful if no Internet
	 * Connection is available.
	 * @param votable The VO table.
	 */
	public void readExternalVOTable(String votable)
	{
		results = votable;
	}

	/**
	 * Returns the VO Table if it is already read.
	 * @return The VO Table, or null.
	 */
	public String getVOTableAsString()
	{
		return results;
	}

	/**
	 * Transforms the results of a query into a VO Table.
	 * @param votable Query results.
	 * @return VO Table.
	 */
	public static SavotVOTable toVOTable(String votable)
	{
		SavotVOTable sv = VOTable.toVOTable(votable);
		String description = sv.getDescription();
		String array[] = jparsec.graph.DataSet.toStringArray(votable, FileIO.getLineSeparator());
		String sourceName = "", sourceRadius = "";
		for (int i=0; i<array.length; i++)
		{
			if (array[i].startsWith(" -c=")) sourceName = array[i].substring(4).trim();
			if (array[i].startsWith(" -c.r=")) sourceRadius = array[i].substring(6).trim();
		}
		String queryDescription = "";
		if (!sourceName.equals("")) queryDescription = sourceName + FileIO.getLineSeparator();
		if (!sourceRadius.equals("")) queryDescription += sourceRadius + FileIO.getLineSeparator();
		if (!queryDescription.equals("")) queryDescription = FileIO.getLineSeparator() + JPARSEC_QUERY_DATA_ID +
			FileIO.getLineSeparator() + queryDescription;
		sv.setDescription(description + queryDescription);
		return sv;
	}

	private static final String JPARSEC_QUERY_DATA_ID = "JPARSEC-QUERY-DATA";

	/**
	 * Reads an VO Table.
	 * @param sourcePos Source position, used to check for distances in the catalog records.
	 * Can be null.
	 * @return A set of Vizier elements with the read catalogs and data.
	 * @throws JPARSECException If an error occurs.
	 */
	public VizierElement[] readVOTable(LocationElement sourcePos)
	throws JPARSECException {
		SavotVOTable votable = toVOTable(results);
		return readVOTable(votable, sourcePos);
	}

	/**
	 * Reads an VO Table.
	 * @param sv Input VO Table.
	 * @param sourcePos Source position, used to check for distances in the catalog records.
	 * Can be null.
	 * @return A set of Vizier elements with the read catalogs and data.
	 * @throws JPARSECException If an error occurs.
	 */
	public static VizierElement[] readVOTable(SavotVOTable sv, LocationElement sourcePos)
	throws JPARSECException {
		ArrayList<VizierElement> out = new ArrayList<VizierElement>();

		// For each resource
        for (int l = 0; l < sv.getResources().getItemCount(); l++)
        {
          SavotResource currentResource = (SavotResource)(sv.getResources().getItemAt(l));
          String catalog = currentResource.getName().trim();

          VizierElement vizier = VizierElement.getVizierElement(catalog);
          if (!catalog.equals("") && vizier != null)
          {
	          // For each table of the current resource
	          for (int m = 0; m < currentResource.getTableCount(); m++)
	          {
	        	  SavotTable table = (SavotTable) currentResource.getTables().getItemAt(m);

	        	  //vizier.catalogName = table.getName();
	        	  vizier.catalogDescription = table.getDescription();
	        	  if (vizier.catalogDescription.indexOf(FileIO.getLineSeparator()) > 0) vizier.catalogDescription =
	        			  vizier.catalogDescription.substring(0, vizier.catalogDescription.indexOf(FileIO.getLineSeparator()));
	        	  vizier.data = new ArrayList<String[]>();
	        	  vizier.dataFields = new String[table.getFields().getItemCount()];
	        	  vizier.unit = new String[table.getFields().getItemCount()];
	        	  vizier.setTableIndex(m);

	        	  for (int i = 0; i < vizier.dataFields.length; i++) {
	            	vizier.dataFields[i] = ((SavotField) (table.getFields().getItemAt(i))).getName();
	            	vizier.unit[i] = ((SavotField) (table.getFields().getItemAt(i))).getUnit();
	        	  }

	        	  // Get all the rows of the table
	        		  TRSet tr = null;
		        	  try { tr = table.getData().getTableData().getTRs(); } catch (Exception exc) {}
		        	  if (tr == null) { // No data
		        		  // JPARSECException.addWarning("Could not read TRSet, table "+(m+1)+" in catalog "+vizier.catalogName);
		        		  continue;
		        	  }

		        	  if (tr != null) {
			            // For each row
			            for (int i = 0; i < tr.getItemCount(); i++) {

			              // Get all the data of the row
			              TDSet theTDs = ((SavotTR) tr.getItemAt(i)).getTDs();

			              // Check coordinates
				          double dist = 0.0;
				          if (sourcePos != null) {
					          LocationElement loc = CDSQuery.transformVizierCoordinatesToJ2000(vizier, theTDs);
					          dist = LocationElement.getAngularDistance(loc, sourcePos) * Constant.RAD_TO_ARCSEC;
					          if (dist > 0.5 * vizier.beam) {
					           	String d = ""+(((int) (dist*10.0)) / 10.0);
					           	String b = ""+(((int) (vizier.beam*10.0)) / 10.0);
					           	String msg = "the distance of record "+i+" in catalog "+vizier.catalogName+" from the source ("+d+") is greater than half the beam/resolution of the instrument ("+b+"). This record will be skipped.";
					           	JPARSECException.addWarning(msg);
					          } else {
						          if (dist > 0.25 * vizier.beam) {
							          	String d = ""+(((int) (dist*10.0)) / 10.0);
							           	String b = ""+(((int) (vizier.beam*10.0)) / 10.0);
							           	String msg = "the distance of record "+i+" in catalog "+vizier.catalogName+" from the source ("+d+") is greater than 1/4 the beam/resolution of the instrument ("+b+").";
							           	JPARSECException.addWarning(msg);
							      }
					          }
				          }

				          if (sourcePos == null || dist < 0.5 * vizier.beam || dist == 0.0)
				          {
				              String[] record = new String[vizier.dataFields.length];

				              // For each data of the row
				              for (int j = 0; j < vizier.dataFields.length; j++) {
			            		  record[j] = ((SavotTD) theTDs.getItemAt(j)).getContent();
				              }

				              vizier.data.add(record);
				          }
			            }
		        	  }
	  	        if (vizier.data.size() > 0) out.add(vizier.clone());
	        }

          }
        }
        VizierElement[] vout = new VizierElement[out.size()];
        for (int i=0; i<vout.length;i++)
        {
        	vout[i] = out.get(i);
        }
        return vout;
	}

	/**
	 * Reads an VO Table and creates an HTML file.
	 * @param sourcePos Source position, used to check for distances in the catalog records.
	 * Can be null.
	 * @param allCatalogs True to report all catalogs, false to report only catalogs directly
	 * supported by JPARSEC when obtaining fluxes for example.
	 * @return HTML code.
	 * @throws JPARSECException If an error occurs.
	 */
	public String createHTMLFromVOTable(LocationElement sourcePos, boolean allCatalogs)
	throws JPARSECException {
		SavotVOTable votable = toVOTable(results);
		return createHTMLFromVOTable(votable, sourcePos, allCatalogs);
	}
	/**
	 * Reads an VO Table and creates a LATEX file.
	 * @param sourcePos Source position, used to check for distances in the catalog records.
	 * Can be null.
	 * @param allCatalogs True to report all catalogs, false to report only catalogs directly
	 * supported by JPARSEC when obtaining fluxes for example.
	 * @return LATEX code.
	 * @throws JPARSECException If an error occurs.
	 */
	public String createLATEXFromVOTable(LocationElement sourcePos, boolean allCatalogs)
	throws JPARSECException {
		SavotVOTable votable = toVOTable(results);
		return createLATEXFromVOTable(votable, sourcePos, allCatalogs);
	}

	/**
	 * Reads an VO Table and creates an HTML file.
	 * @param sv Input VO Table.
	 * @param sourcePos Source position, used to check for distances in the catalog records.
	 * Can be null.
	 * @param allCatalogs True to report all catalogs, false to report only catalogs directly
	 * supported by JPARSEC when obtaining fluxes for example.
	 * @return HTML code.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String createHTMLFromVOTable(SavotVOTable sv, LocationElement sourcePos,
			boolean allCatalogs)
	throws JPARSECException {
		String description = sv.getDescription();
		int queryData = description.indexOf(JPARSEC_QUERY_DATA_ID);
		String query = "";
		if (queryData > 0) {
			String[] data = jparsec.graph.DataSet.toStringArray(description.substring(queryData), FileIO.getLineSeparator());
			query = " for "+data[2]+" arcseconds around object "+data[1];
		}

		HTMLReport html = new HTMLReport();
		html.writeHeader("Vizier Query Results");
		html.beginBody();
		html.beginCenter();
		html.setTextColor(HTMLReport.COLOR_RED);
		html.writeMainTitle("Vizier Query Results"+query);
		html.setTextColor(HTMLReport.COLOR_BLACK);
		html.endCenter();
		html.writeBigSkip();

		// For each resource
        for (int l = 0; l < sv.getResources().getItemCount(); l++)
        {
          SavotResource currentResource = (SavotResource)(sv.getResources().getItemAt(l));
          String catalog = currentResource.getName().trim();

          VizierElement vizier = VizierElement.getVizierElement(catalog);
          boolean nullVizier = false;
          if (vizier == null) nullVizier = true;
          if (!catalog.equals("") && vizier != null || allCatalogs)
          {
              if (vizier == null) vizier = new VizierElement(catalog, catalog, currentResource.getDescription().trim(), 0, null, null);

	          // For each table of the current resource
	          for (int m = 0; m < currentResource.getTableCount(); m++)
	          {
	        	  SavotTable table = (SavotTable) currentResource.getTables().getItemAt(m);

	        	  //vizier.catalogName = table.getName();
	        	  vizier.catalogDescription = table.getDescription();
	        	  if (vizier.catalogDescription.indexOf(FileIO.getLineSeparator()) > 0) vizier.catalogDescription =
	        			  vizier.catalogDescription.substring(0, vizier.catalogDescription.indexOf(FileIO.getLineSeparator()));
	        	  vizier.data = new ArrayList<String[]>();
	        	  vizier.dataFields = new String[table.getFields().getItemCount()];
	        	  vizier.unit = new String[table.getFields().getItemCount()];

	        	  for (int i = 0; i < vizier.dataFields.length; i++) {
	            	vizier.dataFields[i] = ((SavotField) (table.getFields().getItemAt(i))).getName();
	            	vizier.unit[i] = ((SavotField) (table.getFields().getItemAt(i))).getUnit();
	        	  }

	        	  // Get all the rows of the table
        		  TRSet tr = null;
	        	  try { tr = table.getData().getTableData().getTRs(); } catch (Exception exc) {}
	        	  if (tr == null) { // No data
	        		  // JPARSECException.addWarning("Could not read TRSet, table "+(m+1)+" in catalog "+vizier.catalogName);
	        		  continue;
	        	  }

	            if (tr != null) {
		            // For each row
		            for (int i = 0; i < tr.getItemCount(); i++) {

		              // Get all the data of the row
		              TDSet theTDs = ((SavotTR) tr.getItemAt(i)).getTDs();

		              // Check coordinates
			          double dist = 0.0;
		              if (!nullVizier)
		              {
				          if (sourcePos != null) {
				        	  LocationElement loc = CDSQuery.transformVizierCoordinatesToJ2000(vizier, theTDs);
					          dist = LocationElement.getAngularDistance(loc, sourcePos) * Constant.RAD_TO_ARCSEC;
					          if (dist > 0.5 * vizier.beam) {
					           	String d = ""+(((int) (dist*10.0)) / 10.0);
					           	String b = ""+(((int) (vizier.beam*10.0)) / 10.0);
					           	String msg = "the distance of this record from the source ("+d+") is greater than the beam/resolution of the instrument ("+b+"). This record will be skipped.";
					           	JPARSECException.addWarning(msg);
					          } else {
						          if (dist > 0.25 * vizier.beam) {
							          	String d = ""+(((int) (dist*10.0)) / 10.0);
							           	String b = ""+(((int) (vizier.beam*10.0)) / 10.0);
							           	String msg = "the distance of this record from the source ("+d+") is greater than half the beam/resolution of the instrument ("+b+").";
							           	JPARSECException.addWarning(msg);
							      }
					          }
				          }
		              }

			          if (sourcePos == null || dist < 0.5 * vizier.beam || dist == 0.0)
			          {
			              String[] record = new String[vizier.dataFields.length];

			              // For each data of the row
			              for (int j = 0; j < vizier.dataFields.length; j++) {
		            		  record[j] = ((SavotTD) theTDs.getItemAt(j)).getContent();
			              }

			              vizier.data.add(record);
			          }
		            }
	            }
		        if (vizier.data.size() > 0) {
		        	vizier.setTableIndex(m);
	        		int n = (vizier.data.get(0)).length;
	        		String width = "100%";
	        		String bgcolor ="00ffff", align = "center", colspan = ""+n;
	        		html.writeTableHeader(1, 3, 0, width);
	        		html.writeRowInTable(new String[] {vizier.catalogName}, bgcolor, align, colspan);
	        		html.writeRowInTable(new String[] {vizier.catalogDescription}, bgcolor, align, colspan);

	        		String columns[] = new String[n];
	        		String alt[] = new String[n];
	        		for (int j = 0; j < n; j++)
	        		{
	        			String meaning = "";
	        			alt[j] = VOTableUtils.getFieldDescriptionInVOTable(sv, vizier.catalogName, j, m);
	        			if (!meaning.equals("")) alt[j] += " ["+meaning+"]";
	        			columns[j] = VOTableUtils.getFieldNameInVOTable(sv, vizier.catalogName, j, m);
	        		}
	        		html.setTextStyle(STYLE.BOLD);
	        		html.writeRowInTable(columns, alt, null, align + " NOWRAP", null);
	        		html.setTextStyle(STYLE.PLAIN);

		        	for (int i=0; i<vizier.data.size(); i++)
		        	{
		        		String[] record = vizier.data.get(i);
		        		for (int j = 0; j < record.length; j++)
		        		{
			        		boolean putLink = false;
			        		String link = "";
			        		if (vizier.links.length > 0) {
			        			for (int k=0; k<vizier.links.length; k++)
			        			{
			        				String fieldName = FileIO.getField(1, vizier.links[k], " ", true);
			        				int p = vizier.getFieldPosition(fieldName);
			        				link = FileIO.getRestAfterField(1, vizier.links[k], " ", true);
			        				for (int kk=0; kk<vizier.fields.length; kk++)
			        				{
			        					String f = "<"+FileIO.getField(1, vizier.fields[m][kk], " ", true)+">";
			        					int fp = link.indexOf(f);
			        					if (fp >= 0) {
			        						String r = record[kk];
			        						if (f.equals("<SOURCE>")) r = r.replaceAll(" ", "");
			        						link = link.replaceAll(f, r);
			        					}
			        				}
			        				if (p == j) {
			        					link = link.replaceAll(" ", "%20");
			        					int searchRadius = link.indexOf("SEARCH_RADIUS");
			        					if (searchRadius >=0) link = link.replaceAll("<SEARCH_RADIUS>", ""+vizier.beam);
			    		        		putLink = true;
			    		        		break;
			        				}
			        			}
			        		}
			        		if (putLink) record[j] = html.writeLink(link, record[j]);
		        		}
		        		html.writeRowInTable(record, null, align + " NOWRAP", null);
		        	}
		        	html.endTable();
		        	html.writeSmallSkip();
		        }
	        }
          }
        }
        html.writeBigSkip();
        html.writeHorizontalLine();
        html.setTextSize(SIZE.VERY_SMALL);
        html.writeParagraph("Automatically generated by JPARSEC package on "+(new Date().toString()));
        html.writeSmallSkip();
        html.writeParagraph("Credits:");
        String des = sv.getDescription();
        int a = des.indexOf(JPARSEC_QUERY_DATA_ID);
        if (a > 0) des = des.substring(0, a).trim();
        html.writeParagraph(des);
        SavotInfo info = (SavotInfo) (sv.getInfos().getItemAt(2));
        String cat = info.getContent();
        html.writeParagraph(cat);
        html.endBody();
        html.endDocument();
        return html.getCode();
	}

	/**
	 * Reads an VO Table and creates a LATEX file.
	 * @param sv Input VO Table.
	 * @param sourcePos Source position, used to check for distances in the catalog records.
	 * Can be null.
	 * @param allCatalogs True to report all catalogs, false to report only catalogs directly
	 * supported by JPARSEC when obtaining fluxes for example.
	 * @return LATEX code.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String createLATEXFromVOTable(SavotVOTable sv, LocationElement sourcePos,
			boolean allCatalogs)
	throws JPARSECException {
		String description = sv.getDescription();
		int queryData = description.indexOf(JPARSEC_QUERY_DATA_ID);
		String query = "";
		if (queryData > 0) {
			String[] data = jparsec.graph.DataSet.toStringArray(description.substring(queryData), FileIO.getLineSeparator());
			query = " for "+data[2]+" arcseconds around object "+data[1];
		}

		LATEXReport latex = new LATEXReport();
		latex.writeHeader("Vizier Query Results");
		latex.beginBody();
		latex.beginCenter();
		latex.setTextColor(LATEXReport.COLOR_RED);
		latex.writeMainTitle("Vizier Query Results"+query);
		latex.setTextColor(LATEXReport.COLOR_BLACK);
		latex.endCenter();
		latex.writeBigSkip();

		// For each resource
        for (int l = 0; l < sv.getResources().getItemCount(); l++)
        {

          SavotResource currentResource = (SavotResource)(sv.getResources().getItemAt(l));
          String catalog = currentResource.getName().trim();

          VizierElement vizier = VizierElement.getVizierElement(catalog);
          boolean nullVizier = false;
          if (vizier == null) nullVizier = true;
          if (!catalog.equals("") && vizier != null || allCatalogs)
          {
              if (vizier == null) vizier = new VizierElement(catalog, catalog, currentResource.getDescription().trim(), 0, null, null);

              // For each table of the current resource
	          for (int m = 0; m < currentResource.getTableCount(); m++)
	          {
	        	  SavotTable table = (SavotTable) currentResource.getTables().getItemAt(m);

	        	  //vizier.catalogName = table.getName();
	        	  vizier.catalogDescription = table.getDescription();
	        	  if (vizier.catalogDescription.indexOf(FileIO.getLineSeparator()) > 0) vizier.catalogDescription =
	        			  vizier.catalogDescription.substring(0, vizier.catalogDescription.indexOf(FileIO.getLineSeparator()));
	        	  vizier.data = new ArrayList<String[]>();
	        	  vizier.dataFields = new String[table.getFields().getItemCount()];
	        	  vizier.unit = new String[table.getFields().getItemCount()];

	        	  for (int i = 0; i < vizier.dataFields.length; i++) {
	            	vizier.dataFields[i] = ((SavotField) (table.getFields().getItemAt(i))).getName();
	            	vizier.unit[i] = ((SavotField) (table.getFields().getItemAt(i))).getUnit();
	        	  }

	        	  // Get all the rows of the table
        		  TRSet tr = null;
	        	  try { tr = table.getData().getTableData().getTRs(); } catch (Exception exc) {}
	        	  if (tr == null) { // No data
	        		  // JPARSECException.addWarning("Could not read TRSet, table "+(m+1)+" in catalog "+vizier.catalogName);
	        		  continue;
	        	  }

	            // For each row
	            if (tr != null) {
		            for (int i = 0; i < tr.getItemCount(); i++) {

		              // Get all the data of the row
		              TDSet theTDs = ((SavotTR) tr.getItemAt(i)).getTDs();

		              // Check coordinates
			          double dist = 0.0;
		              if (!nullVizier)
		              {
				          if (sourcePos != null) {
				        	  LocationElement loc = CDSQuery.transformVizierCoordinatesToJ2000(vizier, theTDs);
					          dist = LocationElement.getAngularDistance(loc, sourcePos) * Constant.RAD_TO_ARCSEC;
					          if (dist > 0.5 * vizier.beam) {
					           	String d = ""+(((int) (dist*10.0)) / 10.0);
					           	String b = ""+(((int) (vizier.beam*10.0)) / 10.0);
					           	String msg = "the distance of this record from the source ("+d+") is greater than the beam/resolution of the instrument ("+b+"). This record will be skipped.";
					           	JPARSECException.addWarning(msg);
					          } else {
						          if (dist > 0.25 * vizier.beam) {
							          	String d = ""+(((int) (dist*10.0)) / 10.0);
							           	String b = ""+(((int) (vizier.beam*10.0)) / 10.0);
							           	String msg = "the distance of this record from the source ("+d+") is greater than half the beam/resolution of the instrument ("+b+").";
							           	JPARSECException.addWarning(msg);
							      }
					          }
				          }
		              }

			          if (sourcePos == null || dist < 0.5 * vizier.beam || dist == 0.0)
			          {
			              String[] record = new String[vizier.dataFields.length];

			              // For each data of the row
			              for (int j = 0; j < vizier.dataFields.length; j++) {
		            		  record[j] = ((SavotTD) theTDs.getItemAt(j)).getContent();
			              }

			              vizier.data.add(record);
			          }
		            }
	            }
		        if (vizier.data.size() > 0) {
		        	vizier.setTableIndex(m);
	        		int n = (vizier.data.get(0)).length;
	        		String width = "100%";
	        		String bgcolor ="00ffff", align = "center", colspan = ""+n;
	        		latex.writeTableHeader(1, 3, 0, width);
	        		latex.writeRowInTable(new String[] {vizier.catalogName}, bgcolor, align, colspan);
	        		latex.writeRowInTable(new String[] {vizier.catalogDescription}, bgcolor, align, colspan);

	        		String columns[] = new String[n];
	        		String alt[] = new String[n];
	        		for (int j = 0; j < n; j++)
	        		{
	        			String meaning = "";
	        			alt[j] = VOTableUtils.getFieldDescriptionInVOTable(sv, vizier.catalogName, j, m);
	        			if (!meaning.equals("")) alt[j] += " ["+meaning+"]";
	        			columns[j] = VOTableUtils.getFieldNameInVOTable(sv, vizier.catalogName, j, m);
	        		}
	        		latex.setTextStyle(STYLE.BOLD);
	        		latex.writeRowInTable(columns, alt, null, align + " NOWRAP", null);
	        		latex.setTextStyle(STYLE.PLAIN);

		        	for (int i=0; i<vizier.data.size(); i++)
		        	{
		        		String[] record = vizier.data.get(i);
		        		for (int j = 0; j < record.length; j++)
		        		{
			        		boolean putLink = false;
			        		String link = "";
			        		if (vizier.links.length > 0) {
			        			for (int k=0; k<vizier.links.length; k++)
			        			{
			        				String fieldName = FileIO.getField(1, vizier.links[k], " ", true);
			        				int p = vizier.getFieldPosition(fieldName);
			        				link = FileIO.getRestAfterField(1, vizier.links[k], " ", true);
			        				for (int kk=0; kk<vizier.fields.length; kk++)
			        				{
			        					String f = "<"+FileIO.getField(1, vizier.fields[m][kk], " ", true)+">";
			        					int fp = link.indexOf(f);
			        					if (fp >= 0) {
			        						String r = record[kk];
			        						if (f.equals("<SOURCE>")) r = r.replaceAll(" ", "");
			        						link = link.replaceAll(f, r);
			        					}
			        				}
			        				if (p == j) {
			        					link = link.replaceAll(" ", "%20");
			        					int searchRadius = link.indexOf("SEARCH_RADIUS");
			        					if (searchRadius >=0) link = link.replaceAll("<SEARCH_RADIUS>", ""+vizier.beam);
			    		        		putLink = true;
			    		        		break;
			        				}
			        			}
			        		}
			        		if (putLink) record[j] = latex.writeLink(link, record[j]);
		        		}
		        		latex.writeRowInTable(record, null, align + " NOWRAP", null);
		        	}
		        	latex.endTable();
		        	latex.writeSmallSkip();
		        }
          	}
          }
        }
        latex.writeBigSkip();
        latex.writeHorizontalLine();
        latex.writeSmallTextLine("Automatically generated by JPARSEC package on "+(new Date().toString()));
        latex.writeSmallSkip();
        latex.writeSmallTextLine("Credits:");
        String des = sv.getDescription();
        int a = des.indexOf(JPARSEC_QUERY_DATA_ID);
        if (a > 0) des = des.substring(0, a).trim();
        latex.writeSmallTextLine(des);
        SavotInfo info = (SavotInfo) (sv.getInfos().getItemAt(2));
        String cat = info.getContent();
        latex.writeSmallTextLine(cat);
        latex.endBody();
        latex.endDocument();
        return latex.getCode();
	}

	/**
	 * Performs a cone search query to Vizier.
	 * @param targetName Target name, to be resolved by Simbad.
	 * @param catalogName Catalog name (author or name). Can be null to query all available.
	 * @param radius Radius to search in arcseconds.
	 * @return Server output as a String.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String query(String targetName, String catalogName, double radius)
	throws JPARSECException {
		return query(targetName, catalogName, radius, false);
	}

	/**
	 * Performs a query to Vizier.
	 * @param targetName Target name, to be resolved by Simbad.
	 * @param catalogName Catalog name (author or name). Can be null to query all available.
	 * @param radius Radius to search in arcseconds.
	 * @param coneSearch True for a cone search, false for a box search.
	 * @return Server output as a String.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String query(String targetName, String catalogName, double radius, boolean coneSearch)
	throws JPARSECException {

		try {
			if (catalogName == null) catalogName = "";
			String table = "";
			targetName = URLEncoder.encode(targetName, ReadFile.ENCODING_UTF_8);
			// New method, faster and without using VO services, but (maybe) unstable
//			if (catalogName.trim().equals("")) {
				String type = "r";
				if (!coneSearch) type = "b";
				String q = "http://vizier.u-strasbg.fr/viz-bin/votable?-to=-4cb&-from=-1&-this=-4c&-source="+catalogName.trim()+"&-out.max=50000&-out.form=VOTable&-c="+targetName+"&-c.eq=J2000&-oc.form=dec&-c.r=+"+radius+"&-c.u=arcsec&-c.geom="+type+"&-out.add=_r&-out.add=_RA*-c.eq%2C_DE*-c.eq&-sort=_r&-out.add=_RA*-c.eq%2C_DE*-c.eq";
				table = GeneralQuery.query(q, 1000*60);
				return table;
//			} else {
/*			cds.vizier.VizieRQuery vq = new cds.vizier.VizieRQuery();
			Vector list = new Vector();
			String unit = "arcsec", tauthor = null, extra = null;
			int mode = 1;
			vq.submit(targetName, ""+radius, unit, tauthor, extra, mode, list);
//			}

			return DataSet.vectorToString(list);
*/		} catch (Exception e)
		{
			throw new JPARSECException(e);
		}
	}
}
