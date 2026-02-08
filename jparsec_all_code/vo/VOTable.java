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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

import cds.savot.model.FieldSet;
import cds.savot.model.ResourceSet;
import cds.savot.model.SavotData;
import cds.savot.model.SavotField;
import cds.savot.model.SavotResource;
import cds.savot.model.SavotTD;
import cds.savot.model.SavotTR;
import cds.savot.model.SavotTable;
import cds.savot.model.SavotTableData;
import cds.savot.model.SavotVOTable;
import cds.savot.model.TDSet;
import cds.savot.model.TRSet;
import cds.savot.model.TableSet;
import cds.savot.pull.SavotPullEngine;
import cds.savot.pull.SavotPullParser;
import cds.savot.writer.SavotWriter;
import jparsec.io.FileIO;
import jparsec.util.JPARSECException;
import jparsec.util.Version;

/**
 * Contains methods to read and write Virtual Observatory (VO) tables.<BR>
 *
 * Here is an example of how to create a VO table.<BR>
 *
 * <pre>
 * // Metadata for the fields
 * VOTableMeta fieldMeta[] = new VOTableMeta[] {
 *		new VOTableMeta("column 1", "1", "description of c1", "float", "3", "4", "ucd c1", "unit c1"),
 *		new VOTableMeta("column 2", "2", "description of c2", "string", "3", "4", "ucd c2", "unit c2"),
 *		new VOTableMeta("column 3", "3", "description of c3", "double", "3", "4", "ucd c3", "unit c3"),
 *		new VOTableMeta("column 4", "4", "description of c4", "something", "3", "4", "ucd c4", "unit c4")
 * };
 *
 * // Metadata for the resource
 * VOTableMeta resourceMeta = new VOTableMeta("resource 1", "r1", "description of resource 1");
 *
 * // Metada for the table
 * VOTableMeta tableMeta = new VOTableMeta("table 1", "t1", "description of table 1");
 *
 * // Table as string, to be created in VO format
 * String table = "r1c1   r1c2   r1c3   r1c4" + FileIO.getLineSeparator();
 * table += "r2c1   r2c2   r2c3   r2c4" + FileIO.getLineSeparator();
 * table += "r3c1   r3c2   r3c3   r3c4" + FileIO.getLineSeparator();
 * table += "r4c1   r4c2   r4c3   r4c4" + FileIO.getLineSeparator();
 *
 * // Create table
 * SavotVOTable s = VOTable.createVOTable(table, " ", resourceMeta, tableMeta, fieldMeta);
 *
 * // Print table as string
 * System.out.println(VOTable.toString(s));
 * </pre>
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class VOTable implements Serializable {
	private static final long serialVersionUID = 1L;

	private SavotVOTable votable;
	private String votableString;

	/**
	 * Constructor using a VO table.
	 * @param v VO Table.
	 * @throws JPARSECException If an error occurs.
	 */
	public VOTable(SavotVOTable v)
	throws JPARSECException {
		this.votable = v;
		this.votableString = toString(v);
	}
	/**
	 * Constructor using a string.
	 * @param v VO Table as string.
	 * @throws JPARSECException If an error occurs.
	 */
	public VOTable(String v)
	throws JPARSECException {
		this.votable = toVOTable(v);
		this.votableString = v;
	}
	/**
	 * Constructor using an input stream.
	 * @param v VO Table input stream.
	 * @throws JPARSECException If an error occurs.
	 */
	public VOTable(InputStream v)
	throws JPARSECException {
		this.votable = toVOTable(v);
		this.votableString = toString(this.votable);
	}
    /**
     * Creates a VO table from a given table.
     * @param table Table to use.
     * @param separator Separator for elements in the table.
     * @param resourceMeta Metadata for the resource. Only name, id,
     * and description will be considered.
     * @param tableMeta Metadata for the table, commonly equal
     * or similar to the resource metadata. Only name, id,
     * and description will be considered.
     * @param fieldMeta Metadata for the fields.
     * @throws JPARSECException If an error occurs.
     */
	public VOTable (String table, String separator, VOTableMeta resourceMeta,
			VOTableMeta tableMeta, VOTableMeta fieldMeta[])
	throws JPARSECException {
		this.votable = createVOTable(table, separator, resourceMeta, tableMeta, fieldMeta);
		this.votableString = toString(this.votable);
	}
	/**
	 * Returns the VO table as a string.
	 * @return The VO Table.
	 */
	public String getVOTableAsString()
	{
		return this.votableString;
	}
	/**
	 * Returns the VO table as a VO table object.
	 * @return The VO Table.
	 */
	public SavotVOTable getVOTable()
	{
		return this.votable;
	}

	/**
	 * Transforms a string representation of a VO Table into a VO Table object.
	 * @param votable VO table as string.
	 * @return VO Table.
	 */
	public static SavotVOTable toVOTable(String votable)
	{
		ByteArrayInputStream source = new ByteArrayInputStream(votable.getBytes());

		SavotPullParser sb = new SavotPullParser(source, SavotPullEngine.FULL, "UTF-8");

		SavotVOTable sv = sb.getVOTable();

		return sv;
	}

	/**
	 * Transforms a string representation of a VO Table into a VO Table object.
	 * @param stream Input stream for the VO table.
	 * @return VO Table.
	 */
	public static SavotVOTable toVOTable(InputStream stream)
	{
		SavotPullParser sb = new SavotPullParser(stream, SavotPullEngine.FULL, "UTF-8");

		SavotVOTable sv = sb.getVOTable();

		return sv;
	}

	/**
	 * Holds the VO table specification link.
	 */
    public static final String VOTABLE_XMLNS = "http://www.ivoa.net/xml/VOTable/v1.1";

    /**
     * Creates a VO table from a given table.
     * @param table Table to use.
     * @param separator Separator for elements in the table.
     * @param resourceMeta Metadata for the resource. Only name, id,
     * and description will be considered.
     * @param tableMeta Metadata for the table, commonly equal
     * or similar to the resource metadata. Only name, id,
     * and description will be considered.
     * @param fieldMeta Metadata for the fields.
     * @return The VO table.
     */
	public static SavotVOTable createVOTable(String table, String separator, VOTableMeta resourceMeta,
			VOTableMeta tableMeta, VOTableMeta fieldMeta[])
	{
		TableSet tableset = new TableSet();

        SavotTable savotTable = buildTable(table, separator, tableMeta, fieldMeta);
        tableset.addItem(savotTable);

        SavotResource resource = new SavotResource();
        resource.setTables(tableset);
        resource.setName(resourceMeta.name);
        resource.setDescription(resourceMeta.description);
        resource.setId(resourceMeta.id);
        ResourceSet resources = new ResourceSet();
        resources.addItem(resource);

        SavotVOTable votable = new SavotVOTable();
        votable.setResources(resources);
        votable.setXmlns(VOTABLE_XMLNS);
        votable.setDescription("Created by "+Version.PACKAGE_NAME+" "+Version.VERSION_ID+" on "+(new Date().toString()));

        return votable;
	}

    /**
     * Creates a VO table from a given set of tables.
     * @param table Tables to use.
     * @param separator Separator for elements in the table.
     * @param resourceMeta Metadata for the resource. Only name, id,
     * and description will be considered.
     * @param tableMeta Metadata for the table, commonly equal
     * or similar to the resource metadata. Only name, id,
     * and description will be considered.
     * @param fieldMeta Metadata for the fields, set to a Vector.
     * @return The VO table.
     */
	public static SavotVOTable createVOTableSet(String table[], String separator, VOTableMeta resourceMeta[],
			VOTableMeta tableMeta[], ArrayList<VOTableMeta[]> fieldMeta)
	{
		TableSet tableset = new TableSet();

        ResourceSet resources = new ResourceSet();
		for (int i=0; i<table.length; i++)
		{
	        SavotTable savotTable = buildTable(table[i], separator, tableMeta[i], (VOTableMeta[]) fieldMeta.get(i));
	        tableset.addItem(savotTable);

	        SavotResource resource = new SavotResource();
	        resource.setTables(tableset);
	        resource.setName(resourceMeta[i].name);
	        resource.setDescription(resourceMeta[i].description);
	        resource.setId(resourceMeta[i].id);

	        resources.addItem(resource);
		}

        SavotVOTable votable = new SavotVOTable();
        votable.setResources(resources);
        votable.setXmlns(VOTABLE_XMLNS);
        votable.setDescription("Created by "+Version.PACKAGE_NAME+" "+Version.VERSION_ID+" on "+(new Date().toString()));

        return votable;
	}

    private static SavotTable buildTable(String table, String separator, VOTableMeta tableMeta, VOTableMeta meta[]) {

        FieldSet fieldset = buildFieldSet(meta);
        SavotData data = buildTableData(table, separator);
        SavotTable savotTable = new SavotTable();
        savotTable.setName(tableMeta.name);
        savotTable.setDescription(tableMeta.description);
        savotTable.setId(tableMeta.id);
        savotTable.setFields(fieldset);
        savotTable.setData(data);

        return savotTable;
    }

    private static SavotData buildTableData(String table, String separator) {
        TRSet rowset = buildRowSet(table, separator);

        SavotData data = new SavotData();
        SavotTableData tabledata = new SavotTableData();

        tabledata.setTRs(rowset);
        data.setTableData(tabledata);
        return data;
    }

    private static FieldSet buildFieldSet(VOTableMeta meta[]) {
        FieldSet fieldset = new FieldSet();

        if (meta != null) {
	        for (int i=0; i<meta.length; i++)
	        {
	            SavotField field = new SavotField();
	            field.setName(meta[i].name);
	            field.setDataType(meta[i].datatype);
	            field.setDescription(meta[i].description);
	            field.setId(meta[i].id);
	            field.setPrecision(meta[i].precision);
	            field.setUcd(meta[i].ucd);
	            field.setUnit(meta[i].unit);
	            field.setWidth(meta[i].width);
	            field.setArraySize(meta[i].arraysize);
	            field.setRef(meta[i].ref);
	            fieldset.addItem(field);
	        }
        }

        return fieldset;
    }

    private static TRSet buildRowSet(String table, String separator) {
        TRSet rowset = new TRSet();

        String tab[] = jparsec.graph.DataSet.toStringArray(table, FileIO.getLineSeparator());
        for (int row = 0; row < tab.length; row++) {
        	int n = FileIO.getNumberOfFields(tab[row], separator, true);
            TDSet tdset = new TDSet();
            for (int column = 0; column < n; column++) {
            	String x = FileIO.getField(column+1, tab[row], separator, true);

                SavotTD td = new SavotTD();
                td.setContent(x);
                tdset.addItem(td);
            }
            SavotTR tr = new SavotTR();
            tr.setTDSet(tdset);

            rowset.addItem(tr);
        }

        return rowset;
    }

    /**
     * Transforms a VO table into a string.
     * @param votable VO table.
     * @return String.
     * @throws JPARSECException If an error occurs.
     */
    public static String toString(SavotVOTable votable)
    throws JPARSECException {
    	SavotWriter writer = new SavotWriter();
    	String out = "";
        try {
        	ByteArrayOutputStream stream = new ByteArrayOutputStream();
            writer.generateDocument(votable, stream);
            stream.flush();
            stream.close();

            out = stream.toString();
        } catch (FileNotFoundException e) {
        	throw new JPARSECException("File not found.", e);
        } catch (IOException e) {
        	throw new JPARSECException("IO error.", e);
        }

        return out;
    }
}
