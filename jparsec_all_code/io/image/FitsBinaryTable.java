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
package jparsec.io.image;

import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.io.image.HeaderElement.FIELD_TYPE;
import jparsec.util.JPARSECException;
import jparsec.util.Logger;
import jparsec.util.Logger.LEVEL;
import nom.tam.fits.*;

/**
 * A class to manipulate binary tables in .fits files.
 * One of the main methods contain a program to write fits files
 * that conforms to ALMA Test Interferometer Raw Data Format.<P>
 * Please note this class is old and experimental, and several bugs
 * where found in the ALMA specification during its development.<P>
 * Byte and complex arrays are not supported.
 * @author T. Alonso-Albi - OAN (Spain)
 * @version 1.0
 */
public class FitsBinaryTable
{
	// private constructor so that this class cannot be instantiated.
	private FitsBinaryTable() {}

	/**
	 * Parses a given set of columns to produce a header.
	 * @param columns Columns with key + space + format + space + value + space(s) + / + comment.
	 * Comment is optional.
	 * @return The header.
	 */
	public static HeaderElement[] parseHeader(String columns[])
	{
		HeaderElement[] header = new HeaderElement[columns.length];
		for (int i=0; i<columns.length; i++)
		{
			String key = FileIO.getField(1, columns[i], " ", true);
			String format = FileIO.getField(2, columns[i], " ", true);
			String value = FileIO.getRestAfterField(2, columns[i], " ", true);
			String comment = "";
			int bar = value.indexOf("/ ");
			if (bar > 0 && bar < value.length()-1) {
				comment = value.substring(bar+1).trim();
				value = value.substring(0, bar).trim();
			}
			header[i] = new HeaderElement(key, value, comment);
			header[i].format = format;
		}
		return header;

	}

	/**
	 * Creates an HDU with a binary table.
	 * @param header The header for the table, excluding
	 * parameters that depends on the table. Standard parameters to use
	 * are EXTNAME for the name of the table, and TABLEREV for the revision
	 * number (format version) of the table.
	 * @param table The table with elements ordered as [rows][columns]. The first row
	 * should contain the names for the different columns.
	 * @return The HDU.
	 * @throws FitsException Thrown by nom.tam.fits in case of error
	 * initializing a binary table object.
	 * @throws JPARSECException In case the header or table contains invalid data.
	 */
	public static BasicHDU createBinaryTable(HeaderElement header[], String table[][]) throws FitsException, JPARSECException {
		HeaderElement[][] t = new HeaderElement[table.length-1][table[0].length];
		for (int i=0; i<t.length; i++) {
			HeaderElement[] row = new HeaderElement[t[0].length];
			for (int j=0; j<t[0].length; j++) {
				row[j] = new HeaderElement(table[0][j], table[i+1][j], "");
				row[j].format = FIELD_TYPE.STRING_A.id;
			}
			t[i] = row;
		}
		return createBinaryTable(header, t);
	}

	/**
	 * Creates an HDU with an Ascii table.
	 * @param header The header for the table, excluding
	 * parameters that depends on the table. Standard parameters to use
	 * are EXTNAME for the name of the table, and TABLEREV for the revision
	 * number (format version) of the table.
	 * @param table The table with elements ordered as [rows][columns]. The first row
	 * should contain the names for the different columns.
	 * @return The HDU.
	 * @throws FitsException Thrown by nom.tam.fits in case of error
	 * initializing a binary table object.
	 * @throws JPARSECException In case the header or table contains invalid data.
	 */
	public static BasicHDU createAsciiTable(HeaderElement header[], String table[][]) throws FitsException, JPARSECException {
		HeaderElement[][] t = new HeaderElement[table.length-1][table[0].length];
		for (int i=0; i<t.length; i++) {
			HeaderElement[] row = new HeaderElement[t[0].length];
			for (int j=0; j<t[0].length; j++) {
				row[j] = new HeaderElement(table[0][j], table[i+1][j], "");
				row[j].format = FIELD_TYPE.STRING_A.id;
			}
			t[i] = row;
		}
		return createAsciiTable(header, t);
	}

	/**
	 * Creates an HDU with a binary table.
	 * @param header The header for the table, excluding
	 * parameters that depends on the table. Standard parameters to use
	 * are EXTNAME for the name of the table, and TABLEREV for the revision
	 * number (format version) of the table.
	 * @param table The table. table.length is the number of rows,
	 * and table[0].length the columns for the first row.
	 * @return The HDU.
	 * @throws FitsException Thrown by nom.tam.fits in case of error
	 * initializing a binary table object.
	 * @throws JPARSECException In case the header or table contains invalid data.
	 */
	public static BasicHDU createBinaryTable(HeaderElement header[],
			HeaderElement table[][]) throws FitsException, JPARSECException {
		if (table == null) return FitsIO.createHDU(null, header);

		int nrow = table.length;
		int ncol = table[0].length;
		BinaryTable binaryTable = new BinaryTable();
		for (int i=0; i<ncol; i++)
		{
			String data[] = new String[nrow];
			for (int j=0; j<nrow; j++)
			{
				String value = table[j][i].getAsString();
				data[j] = value;
			}
			// Parse to different datatypes
			FIELD_TYPE format = FitsBinaryTable.getColumnFormat(table[0][i]);
			int length = FitsBinaryTable.getColumnLength(table[0][i]);
			int ndim = getColumnDimensions(table[0][i]);
			int dim[] = new int[ndim];
			for (int l=0; l<ndim; l++)
			{
				dim[l] = getColumnDimension(table[0][i], l+1);
			}

			try {
				for (int k=0; k<data.length; k++)
				{
					if (data[k].length()>length && length>0 && ndim == 0)
						Logger.log(LEVEL.WARNING, "Error inserting column "+data[k]+". Its size ("+data[k].length()+") is greater than limiting value of "+length+".");
				}
				if (ndim > 0) throw new JPARSECException("This keyword will need further processing after following switch clause.");
				if (format == null) throw new JPARSECException("Column format "+table[0][i].format+" is unknown.");
				switch (format)
				{
					case INTEGER_J:
						int dataJ[] = new int[data.length];
						for (int k=0; k<data.length; k++)
						{
							dataJ[k] = Integer.parseInt(data[k]);
						}
						binaryTable.addColumn(dataJ);
						break;
					case DOUBLE_D:
						double dataD[] = new double[data.length];
						for (int k=0; k<data.length; k++)
						{
							dataD[k] = DataSet.parseDouble(data[k]);
						}
						binaryTable.addColumn(dataD);
						break;
					case FLOAT_E:
						float dataE[] = new float[data.length];
						for (int k=0; k<data.length; k++)
						{
							dataE[k] = DataSet.parseFloat(data[k]);
						}
						binaryTable.addColumn(dataE);
						break;
					case BOOLEAN_L:
						boolean dataB[] = new boolean[data.length];
						for (int k=0; k<data.length; k++)
						{
							dataB[k] = Boolean.parseBoolean(data[k]);
						}
						binaryTable.addColumn(dataB);
						break;
					case SHORT_I:
						short dataS[] = new short[data.length];
						for (int k=0; k<data.length; k++)
						{
							dataS[k] = Short.parseShort(data[k]);
						}
						binaryTable.addColumn(dataS);
						break;
					default:
						binaryTable.addColumn(data);
				}
			} catch (Exception e)
			{
				try {
					Object column[] = new Object[data.length];
					for (int ii=0; ii<data.length; ii++)
					{
						String values[] = evaluateExpression(data[ii]);

						int[][][] objJ3 = null;
						double[][][] objD3 = null;
						float[][][] objE3 = null;
						String[][][] objS3 = null;
						boolean[][][] objB3 = null;
						int[][] objJ2 = null;
						double[][] objD2 = null;
						float[][] objE2 = null;
						String[][] objS2 = null;
						boolean[][] objB2 = null;
						int[] objJ1 = null;
						double[] objD1 = null;
						float[] objE1 = null;
						short[] objX1 = null;
						short[][] objX2 = null;
						short[][][] objX3 = null;
						String[] objS1 = null;
						boolean[] objB1 = null;
						int index;
						switch (ndim)
						{
						case 3:
							objB3 = new boolean[dim[2]][dim[1]][dim[0]];
							objD3 = new double[dim[2]][dim[1]][dim[0]];
							objJ3 = new int[dim[2]][dim[1]][dim[0]];
							objE3 = new float[dim[2]][dim[1]][dim[0]];
							objS3 = new String[dim[2]][dim[1]][dim[0]];
							objX3 = new short[dim[2]][dim[1]][dim[0]];
							index = -1;
							for (int n=0; n<dim[2]; n++)
							{
								for (int m=0; m<dim[1]; m++)
								{
									for (int l=0; l<dim[0]; l++)
									{
										index ++;
										try { objB3[n][m][l] = new Boolean(values[index]); } catch (Exception exc) {}
										try { objD3[n][m][l] = new Double(values[index]); } catch (Exception exc) {}
										try { objE3[n][m][l] = new Float(values[index]); } catch (Exception exc) {}
										try { objX3[n][m][l] = new Short(values[index]); } catch (Exception exc) {}
										try { objJ3[n][m][l] = new Integer((int) (DataSet.parseDouble(values[index]) + 0.5)); } catch (Exception exc) {}
										try { objS3[n][m][l] = DataSet.replaceAll(values[index], "\"", "", true); } catch (Exception exc) {}
									}
								}
							}
							break;
						case 2:
							objB2 = new boolean[dim[1]][dim[0]];
							objD2 = new double[dim[1]][dim[0]];
							objJ2 = new int[dim[1]][dim[0]];
							objE2 = new float[dim[1]][dim[0]];
							objX2 = new short[dim[1]][dim[0]];
							objS2 = new String[dim[1]][dim[0]];
							index = -1;
							for (int m=0; m<dim[1]; m++)
							{
								for (int l=0; l<dim[0]; l++)
								{
									index ++;
									try { objB2[m][l] = new Boolean(values[index]); } catch (Exception exc) {}
									try { objD2[m][l] = new Double(values[index]); } catch (Exception exc) {}
									try { objJ2[m][l] = new Integer((int) (DataSet.parseDouble(values[index]) + 0.5)); } catch (Exception exc) {}
									try { objE2[m][l] = new Float(values[index]); } catch (Exception exc) {}
									try { objX2[m][l] = new Short(values[index]); } catch (Exception exc) {}
									try { objS2[m][l] = DataSet.replaceAll(values[index], "\"", "", true); } catch (Exception exc) {}
								}
							}
							break;
						case 1:
							objB1 = new boolean[dim[0]];
							objD1 = new double[dim[0]];
							objJ1 = new int[dim[0]];
							objE1 = new float[dim[0]];
							objS1 = new String[dim[0]];
							objX1 = new short[dim[0]];
							for (int l=0; l<dim[0]; l++)
							{
								try { objB1[l] = new Boolean(values[l]); } catch (Exception exc) {}
								try { objD1[l] = new Double(values[l]); } catch (Exception exc) {}
								try { objJ1[l] = new Integer((int) (DataSet.parseDouble(values[l]) +0.5)); } catch (Exception exc) {}
								try { objE1[l] = new Float(values[l]); } catch (Exception exc) {}
								try { objS1[l] = DataSet.replaceAll(values[l], "\"", "", true); } catch (Exception exc) {}
								try { objX1[l] = new Short(values[l]); } catch (Exception exc) {}
							}
							break;
						}

						switch (format)
						{
						case BOOLEAN_L:
							if (ii==0 && ndim == 1) column = new boolean[data.length][];
							if (ii==0 && ndim == 2) column = new boolean[data.length][][];
							if (ii==0 && ndim == 3) column = new boolean[data.length][][][];
							if (ndim == 1)  column[ii] = objB1;
							if (ndim == 2) column[ii] = objB2;
							if (ndim > 2) column[ii] = objB3;
							break;
						case STRING_A:
							if (ii==0 && ndim == 1) column = new String[data.length][];
							if (ii==0 && ndim == 2) column = new String[data.length][][];
							if (ii==0 && ndim == 3) column = new String[data.length][][][];
							if (ndim == 1)  column[ii] = objS1;
							if (ndim == 2) column[ii] = objS2;
							if (ndim > 2) column[ii] = objS3;
							break;
						case INTEGER_J:
							if (ii==0 && ndim == 1) column = new int[data.length][];
							if (ii==0 && ndim == 2) column = new int[data.length][][];
							if (ii==0 && ndim == 3) column = new int[data.length][][][];
							if (ndim == 1)  column[ii] = objJ1;
							if (ndim == 2) column[ii] = objJ2;
							if (ndim > 2) column[ii] = objJ3;
							break;
						case DOUBLE_D:
							if (ii==0 && ndim == 1) column = new double[data.length][];
							if (ii==0 && ndim == 2) column = new double[data.length][][];
							if (ii==0 && ndim == 3) column = new double[data.length][][][];
							if (ndim == 1)  column[ii] = objD1;
							if (ndim == 2) column[ii] = objD2;
							if (ndim > 2) column[ii] = objD3;
							break;
						case FLOAT_E:
							if (ii==0 && ndim == 1) column = new float[data.length][];
							if (ii==0 && ndim == 2) column = new float[data.length][][];
							if (ii==0 && ndim == 3) column = new float[data.length][][][];
							if (ndim == 1)  column[ii] = objE1;
							if (ndim == 2) column[ii] = objE2;
							if (ndim > 2) column[ii] = objE3;
							break;
						case SHORT_I:
							if (ii==0 && ndim == 1) column = new short[data.length][];
							if (ii==0 && ndim == 2) column = new short[data.length][][];
							if (ii==0 && ndim == 3) column = new short[data.length][][][];
							if (ndim == 1)  column[ii] = objX1;
							if (ndim == 2) column[ii] = objX2;
							if (ndim > 2) column[ii] = objX3;
							break;
						default:
							throw new JPARSECException("Cannot recognize column format '"+format+"'.");
						}
					}

					binaryTable.addColumn(column);
				} catch (Exception exc)
				{
					throw new JPARSECException(exc);
				}
			}
		}
		BasicHDU bhdu = Fits.makeHDU(binaryTable);
		Header h = bhdu.getHeader();
		completeHeader(h, header);

		// Set columns names (TTYPE and TUNIT keywords)
		BinaryTableHDU bthdu = (BinaryTableHDU) bhdu;
		for (int i=0; i<ncol; i++)
		{
			String comment = table[0][i].comment;
			String unit = "";
			int unitPos = comment.lastIndexOf(" in ");
			int lastSpace = comment.lastIndexOf(" ");
			if (unitPos > 0 && lastSpace < (unitPos+4)) {
				unit = comment.substring(unitPos+4);
				comment = comment.substring(0, unitPos);
			}
			bthdu.setColumnName(i, table[0][i].key, comment);
			if (!unit.equals("")) bthdu.addValue("TUNIT"+(i+1), unit, "");
		}

		// Move binary table header entries to the end
		// (probably not needed, just for esthetic)
		h = bthdu.getHeader();
		int tf = h.getIntValue("TFIELDS");
		String card[][] = new String[4][tf];
		for (int i=0; i<tf; i++)
		{
			card[0][i] = removeCard(h, "TTYPE"+(i+1));
			card[1][i] = removeCard(h, "TFORM"+(i+1));
			card[2][i] = removeCard(h, "TUNIT"+(i+1));
			card[3][i] = removeCard(h, "TDIM"+(i+1));
		}
		for (int i=0; i<tf; i++)
		{
			if (!card[0][i].equals("")) addLine(h, card[0][i]);
			if (!card[1][i].equals("")) addLine(h, card[1][i]);
			if (!card[2][i].equals("")) addLine(h, card[2][i]);
			if (!card[3][i].equals("")) addLine(h, card[3][i]);
		}

		return bhdu;
	}

	private static void addLine(Header h, String s) throws HeaderCardException {
		s = s.trim();
		if (s.equals("")) {
			h.addValue("", "", "");
			return;
		}

		String comment = "";
		String key = FileIO.getField(1, s, "=", false).trim();
		String val = FileIO.getField(2, s, "=", false).trim();
		if (val.indexOf("/") > 0) comment = val.substring(val.indexOf("/")+1);
		if (val.indexOf("'") >= 0) val = val.substring(val.indexOf("'")+1, val.lastIndexOf("'"));

		h.addValue(key, val, comment);
	}

	/**
	 * Creates an HDU with an Ascii table.
	 * @param header The header for the table, excluding
	 * parameters that depends on the table. Standard parameters to use
	 * are EXTNAME for the name of the table, and TABLEREV for the revision
	 * number (format version) of the table.
	 * @param table The table. table.length is the number of rows,
	 * and table[0].length the columns for the first row.
	 * @return The HDU.
	 * @throws FitsException Thrown by nom.tam.fits in case of error
	 * initializing a binary table object.
	 * @throws JPARSECException In case the header or table contains invalid data.
	 */
	public static BasicHDU createAsciiTable(HeaderElement header[],
			HeaderElement table[][]) throws FitsException, JPARSECException {
		if (table == null) return FitsIO.createHDU(null, header);

		int nrow = table.length;
		int ncol = table[0].length;
		AsciiTable binaryTable = new AsciiTable();
		for (int i=0; i<ncol; i++)
		{
			String data[] = new String[nrow];
			for (int j=0; j<nrow; j++)
			{
				String value = table[j][i].getAsString();
				data[j] = value;
			}
			// Parse to different datatypes
			FIELD_TYPE format = FitsBinaryTable.getColumnFormat(table[0][i]);
			int length = FitsBinaryTable.getColumnLength(table[0][i]);
			int ndim = getColumnDimensions(table[0][i]);
			int dim[] = new int[ndim];
			for (int l=0; l<ndim; l++)
			{
				dim[l] = getColumnDimension(table[0][i], l+1);
			}

			try {
				for (int k=0; k<data.length; k++)
				{
					if (data[k].length()>length && length>0 && ndim == 0)
						Logger.log(LEVEL.WARNING, "Error inserting column "+data[k]+". Its size ("+data[k].length()+") is greater than limiting value of "+length+".");
				}
				if (ndim > 0) throw new JPARSECException("This keyword will need further processing after following switch clause.");
				if (format == null) throw new JPARSECException("Column format "+table[0][i].format+" is unknown.");
				switch (format)
				{
					case INTEGER_J:
						int dataJ[] = new int[data.length];
						for (int k=0; k<data.length; k++)
						{
							dataJ[k] = Integer.parseInt(data[k]);
						}
						binaryTable.addColumn(dataJ);
						break;
					case DOUBLE_D:
						double dataD[] = new double[data.length];
						for (int k=0; k<data.length; k++)
						{
							dataD[k] = DataSet.parseDouble(data[k]);
						}
						binaryTable.addColumn(dataD);
						break;
					case FLOAT_E:
						float dataE[] = new float[data.length];
						for (int k=0; k<data.length; k++)
						{
							dataE[k] = DataSet.parseFloat(data[k]);
						}
						binaryTable.addColumn(dataE);
						break;
					case BOOLEAN_L:
						boolean dataB[] = new boolean[data.length];
						for (int k=0; k<data.length; k++)
						{
							dataB[k] = Boolean.parseBoolean(data[k]);
						}
						binaryTable.addColumn(dataB);
						break;
					case SHORT_I:
						short dataS[] = new short[data.length];
						for (int k=0; k<data.length; k++)
						{
							dataS[k] = Short.parseShort(data[k]);
						}
						binaryTable.addColumn(dataS);
						break;
					default:
						binaryTable.addColumn(data);
				}
			} catch (Exception e)
			{
				try {
					Object column[] = new Object[data.length];
					for (int ii=0; ii<data.length; ii++)
					{
						String values[] = evaluateExpression(data[ii]);

						int[][][] objJ3 = null;
						double[][][] objD3 = null;
						float[][][] objE3 = null;
						String[][][] objS3 = null;
						boolean[][][] objB3 = null;
						int[][] objJ2 = null;
						double[][] objD2 = null;
						float[][] objE2 = null;
						String[][] objS2 = null;
						boolean[][] objB2 = null;
						int[] objJ1 = null;
						double[] objD1 = null;
						float[] objE1 = null;
						short[] objX1 = null;
						short[][] objX2 = null;
						short[][][] objX3 = null;
						String[] objS1 = null;
						boolean[] objB1 = null;
						int index;
						switch (ndim)
						{
						case 3:
							objB3 = new boolean[dim[2]][dim[1]][dim[0]];
							objD3 = new double[dim[2]][dim[1]][dim[0]];
							objJ3 = new int[dim[2]][dim[1]][dim[0]];
							objE3 = new float[dim[2]][dim[1]][dim[0]];
							objS3 = new String[dim[2]][dim[1]][dim[0]];
							objX3 = new short[dim[2]][dim[1]][dim[0]];
							index = -1;
							for (int n=0; n<dim[2]; n++)
							{
								for (int m=0; m<dim[1]; m++)
								{
									for (int l=0; l<dim[0]; l++)
									{
										index ++;
										try { objB3[n][m][l] = new Boolean(values[index]); } catch (Exception exc) {}
										try { objD3[n][m][l] = new Double(values[index]); } catch (Exception exc) {}
										try { objE3[n][m][l] = new Float(values[index]); } catch (Exception exc) {}
										try { objX3[n][m][l] = new Short(values[index]); } catch (Exception exc) {}
										try { objJ3[n][m][l] = new Integer((int) (DataSet.parseDouble(values[index]) + 0.5)); } catch (Exception exc) {}
										try { objS3[n][m][l] = DataSet.replaceAll(values[index], "\"", "", true); } catch (Exception exc) {}
									}
								}
							}
							break;
						case 2:
							objB2 = new boolean[dim[1]][dim[0]];
							objD2 = new double[dim[1]][dim[0]];
							objJ2 = new int[dim[1]][dim[0]];
							objE2 = new float[dim[1]][dim[0]];
							objX2 = new short[dim[1]][dim[0]];
							objS2 = new String[dim[1]][dim[0]];
							index = -1;
							for (int m=0; m<dim[1]; m++)
							{
								for (int l=0; l<dim[0]; l++)
								{
									index ++;
									try { objB2[m][l] = new Boolean(values[index]); } catch (Exception exc) {}
									try { objD2[m][l] = new Double(values[index]); } catch (Exception exc) {}
									try { objJ2[m][l] = new Integer((int) (DataSet.parseDouble(values[index]) + 0.5)); } catch (Exception exc) {}
									try { objE2[m][l] = new Float(values[index]); } catch (Exception exc) {}
									try { objX2[m][l] = new Short(values[index]); } catch (Exception exc) {}
									try { objS2[m][l] = DataSet.replaceAll(values[index], "\"", "", true); } catch (Exception exc) {}
								}
							}
							break;
						case 1:
							objB1 = new boolean[dim[0]];
							objD1 = new double[dim[0]];
							objJ1 = new int[dim[0]];
							objE1 = new float[dim[0]];
							objS1 = new String[dim[0]];
							objX1 = new short[dim[0]];
							for (int l=0; l<dim[0]; l++)
							{
								try { objB1[l] = new Boolean(values[l]); } catch (Exception exc) {}
								try { objD1[l] = new Double(values[l]); } catch (Exception exc) {}
								try { objJ1[l] = new Integer((int) (DataSet.parseDouble(values[l]) +0.5)); } catch (Exception exc) {}
								try { objE1[l] = new Float(values[l]); } catch (Exception exc) {}
								try { objS1[l] = DataSet.replaceAll(values[l], "\"", "", true); } catch (Exception exc) {}
								try { objX1[l] = new Short(values[l]); } catch (Exception exc) {}
							}
							break;
						}

						switch (format)
						{
						case BOOLEAN_L:
							if (ii==0 && ndim == 1) column = new boolean[data.length][];
							if (ii==0 && ndim == 2) column = new boolean[data.length][][];
							if (ii==0 && ndim == 3) column = new boolean[data.length][][][];
							if (ndim == 1)  column[ii] = objB1;
							if (ndim == 2) column[ii] = objB2;
							if (ndim > 2) column[ii] = objB3;
							break;
						case STRING_A:
							if (ii==0 && ndim == 1) column = new String[data.length][];
							if (ii==0 && ndim == 2) column = new String[data.length][][];
							if (ii==0 && ndim == 3) column = new String[data.length][][][];
							if (ndim == 1)  column[ii] = objS1;
							if (ndim == 2) column[ii] = objS2;
							if (ndim > 2) column[ii] = objS3;
							break;
						case INTEGER_J:
							if (ii==0 && ndim == 1) column = new int[data.length][];
							if (ii==0 && ndim == 2) column = new int[data.length][][];
							if (ii==0 && ndim == 3) column = new int[data.length][][][];
							if (ndim == 1)  column[ii] = objJ1;
							if (ndim == 2) column[ii] = objJ2;
							if (ndim > 2) column[ii] = objJ3;
							break;
						case DOUBLE_D:
							if (ii==0 && ndim == 1) column = new double[data.length][];
							if (ii==0 && ndim == 2) column = new double[data.length][][];
							if (ii==0 && ndim == 3) column = new double[data.length][][][];
							if (ndim == 1)  column[ii] = objD1;
							if (ndim == 2) column[ii] = objD2;
							if (ndim > 2) column[ii] = objD3;
							break;
						case FLOAT_E:
							if (ii==0 && ndim == 1) column = new float[data.length][];
							if (ii==0 && ndim == 2) column = new float[data.length][][];
							if (ii==0 && ndim == 3) column = new float[data.length][][][];
							if (ndim == 1)  column[ii] = objE1;
							if (ndim == 2) column[ii] = objE2;
							if (ndim > 2) column[ii] = objE3;
							break;
						case SHORT_I:
							if (ii==0 && ndim == 1) column = new short[data.length][];
							if (ii==0 && ndim == 2) column = new short[data.length][][];
							if (ii==0 && ndim == 3) column = new short[data.length][][][];
							if (ndim == 1)  column[ii] = objX1;
							if (ndim == 2) column[ii] = objX2;
							if (ndim > 2) column[ii] = objX3;
							break;
						default:
							throw new JPARSECException("Cannot recognize column format '"+format+"'.");
						}
					}

					binaryTable.addColumn(column);
				} catch (Exception exc)
				{
					throw new JPARSECException(exc);
				}
			}
		}
		BasicHDU bhdu = Fits.makeHDU(binaryTable);
		Header h = bhdu.getHeader();
		completeHeader(h, header);

		// Set columns names (TTYPE and TUNIT keywords)
		AsciiTableHDU bthdu = (AsciiTableHDU) bhdu;
		for (int i=0; i<ncol; i++)
		{
			String comment = table[0][i].comment;
			String unit = "";
			int unitPos = comment.lastIndexOf(" in ");
			int lastSpace = comment.lastIndexOf(" ");
			if (unitPos > 0 && lastSpace < (unitPos+4)) {
				unit = comment.substring(unitPos+4);
				comment = comment.substring(0, unitPos);
			}
			bthdu.setColumnName(i, table[0][i].key, comment);
			if (!unit.equals("")) bthdu.addValue("TUNIT"+(i+1), unit, "");
		}

		// Move binary table header entries to the end
		// (probably not needed, just for esthetic)
		h = bthdu.getHeader();
		int tf = h.getIntValue("TFIELDS");
		String card[][] = new String[4][tf];
		for (int i=0; i<tf; i++)
		{
			card[0][i] = removeCard(h, "TTYPE"+(i+1));
			card[1][i] = removeCard(h, "TFORM"+(i+1));
			card[2][i] = removeCard(h, "TUNIT"+(i+1));
			card[3][i] = removeCard(h, "TDIM"+(i+1));
		}
		for (int i=0; i<tf; i++)
		{
			if (!card[0][i].equals("")) addLine(h, card[0][i]);
			if (!card[1][i].equals("")) addLine(h, card[1][i]);
			if (!card[2][i].equals("")) addLine(h, card[2][i]);
			if (!card[3][i].equals("")) addLine(h, card[3][i]);
		}

		return bhdu;
	}

	/**
	 * Removes a key and returns it card.
	 * @param h The header.
	 * @param key The key to search for.
	 * @return The card, or empty string if key is not found.
	 */
	private static String removeCard(Header h, String key)
	{
		String card = "";
		boolean exist = h.containsKey(key);
		if (exist) {
			int nc = h.getNumberOfCards();
			int keyN = -1;
			for (int i=0; i<nc; i++)
			{
				String k = h.getKey(i);
				if (k.equals(key)) {
					keyN = i;
					break;
				}
			}
			if (keyN < 0) {
				Logger.log(LEVEL.TRACE_LEVEL2, "Key "+key+" not found!");
			} else {
				card = h.getCard(keyN);
				h.deleteKey(h.getKey(keyN));
			}
		}
		return card;
	}

	/**
	 * Creates a fits header adding the cards from a given {@linkplain HeaderElement}
	 * object.
	 * @param header The header cards to add.
	 * @return The header.
	 * @throws JPARSECException If an error occurs.
	 * @throws HeaderCardException If the header cannot be modified.
	 */
	public static Header createHeader(HeaderElement header[])
	throws JPARSECException, HeaderCardException {
		return completeHeader(new Header(), header);
	}

	/**
	 * Completes a fits header adding the cards from a given {@linkplain HeaderElement}
	 * object.
	 * @param h The fits header, empty or non-empty.
	 * @param header The header cards to add.
	 * @return The header.
	 * @throws JPARSECException If an error occurs.
	 * @throws HeaderCardException If the header cannot be modified.
	 */
	public static Header completeHeader(Header h, HeaderElement header[])
	throws JPARSECException, HeaderCardException {
		if (header == null || header.length == 0) return h;
		for (int i=0; i<header.length; i++)
		{
			FIELD_TYPE format = FitsBinaryTable.getColumnFormat(header[i]);
			if (format == null)
				throw new JPARSECException("Column format "+header[i].format+" in header keyword "+header[i].key+" is unknown.");

			int length = FitsBinaryTable.getColumnLength(header[i]);
			double val = 0.0;
			try {
				val = header[i].getAsLong();
				h.addValue(header[i].key, (long) val, header[i].comment);
			} catch (Exception e)
			{
				try {
					val = header[i].getAsDouble();
					h.addValue(header[i].key, val, header[i].comment);
				} catch (Exception e2) {
					try {
						if (!header[i].getAsString().equals("T") && !header[i].getAsString().equals("F") &&
								!header[i].getAsString().equals("true") && !header[i].getAsString().equals("false"))
							throw new Exception("Not a boolean");
						boolean b = true;
						if (header[i].getAsString().equals("F") || header[i].getAsString().equals("false")) b = false;
						h.addValue(header[i].key, b, header[i].comment);
					} catch (Exception e3) {
						if (header[i].getAsString().length()>length && length>0)
							Logger.log(LEVEL.WARNING, "Error inserting column "+header[i].getAsString()+". Its size ("+header[i].getAsString().length()+") is greater than limiting value of "+length+".");
						h.addValue(header[i].key, header[i].getAsString(), header[i].comment);
					}
				}
			}
		}
		return h;
	}

	/**
	 * Returns column length given its format.
	 * @param h The header.
	 * @return The length, or 0 if it is not specified.
	 */
	public static int getColumnLength(HeaderElement h)
	{
		String format = h.format;
		int length = 0;
		if (format == null) return length;
		String formatCodes = "JADELICMXB";
		for (int ii=0; ii<format.length(); ii++)
		{
			int p = formatCodes.indexOf(format.substring(ii,ii+1));
			if (p>=0) break;
			length++;
		}
		if (length > 0) return Integer.parseInt(format.substring(0, length));
		return length;
	}

	/**
	 * Returns column format.
	 * @param h The header.
	 * @return The field format.
	 */
	public static FIELD_TYPE getColumnFormat(HeaderElement h)
	{
		String format = h.format;
		if (format == null) return null;
		String formatCodes = "JADELICMXB";
		FIELD_TYPE f = null;
		for (int ii=0; ii<format.length(); ii++)
		{
			int p = formatCodes.indexOf(format.substring(ii,ii+1));
			if (p>=0) {
				format = format.substring(ii,ii+1);
				return FIELD_TYPE.getFieldTypeFromID(format);
			}
		}
		return f;
	}

	/**
	 * Returns number of dimensions.
	 * @param i The header object.
	 * @return The number of dimensions as an integer, 0 means just a value.
	 */
	public static int getColumnDimensions(HeaderElement i)
	{
		if (i.format == null) return 0;
		String format = i.format;
		int pi = format.indexOf("(");
		int pf = format.indexOf(")");
		int d = 0;
		if (pi>=0 && pf>=0) {
			String f = format.substring(pi+1, pf);
			d = FileIO.getNumberOfFields(f, ",", true);
		}

		return d;
	}

	/**
	 * Returns number of dimensions.
	 * @param i The header object.
	 * @param dim Dimension number, starting from 1.
	 * @return The number elements in that dimension.
	 */
	public static int getColumnDimension(HeaderElement i, int dim)
	{
		if (i.format == null) return 0;
		String format = i.format;
		int pi = format.indexOf("(");
		int pf = format.indexOf(")");
		int d = 0;
		if (pi>=0 && pf>=0) {
			String f = format.substring(pi+1, pf);
			d = Integer.parseInt(FileIO.getField(dim, f, ",", true));
		}

		return d;
	}

	private static String[] evaluateExpression(String expression) throws JPARSECException
	{
		expression = DataSet.replaceAll(expression, "{", "", true);
		expression = DataSet.replaceAll(expression, "}", "", true);
		expression = DataSet.replaceAll(expression, "[]", "", true);
		expression = DataSet.replaceAll(expression, "{", "(", true);
		expression = DataSet.replaceAll(expression, "}", ")", true);
		expression = DataSet.replaceAll(expression, "new", "", false);
		expression = DataSet.replaceAll(expression, "float", "", false);
		expression = DataSet.replaceAll(expression, "double", "", false);
		expression = DataSet.replaceAll(expression, "long", "", false);
		expression = DataSet.replaceAll(expression, "int", "", false);
		expression = DataSet.replaceAll(expression, "short", "", false);
		expression = DataSet.replaceAll(expression, "String", "", false);
//		expression = DataSet.replaceAll(expression, "\"", "'");
		int n = FileIO.getNumberOfFields(expression, ",", true);
		String val[] = new String[n];
		for (int i=0; i<n; i++)
		{
			val[i] = FileIO.getField(i+1, expression, ",", true);
		}
		return val;
	}

	/**
	 * Transforms a binary table into a String table containing the names of the
	 * columns in the first row, and the data for each row after the first one.
	 * @param bt The binary table.
	 * @return A string table with data as [rows][columns].
	 * @throws JPARSECException If the input HDU is not a binary table.
	 * @throws FitsException In case nom.tam.fits throws an error reading the data.
	 */
	public static String[][] getBinaryTable(BasicHDU bt) throws JPARSECException, FitsException {
		if (!FitsIO.isBinaryTable(bt)) throw new JPARSECException("Input HDU is not a binary table!");

    	BinaryTableHDU bintable = (BinaryTableHDU) bt;
    	String[][] table = new String[bintable.getNRows()+1][bintable.getNCols()];
    	for (int j=0; j<bintable.getNCols(); j++) {
    		table[0][j] = bintable.getColumnName(j);
    	}
    	for (int j=0; j<bintable.getNRows(); j++) {
    		Object[] data = bintable.getRow(j);
    		for (int k=0; k<data.length; k++) {
    			table[j+1][k] = data[k].toString();
    		}
    	}
		return table;
	}

	/**
	 * Returns the object in the binary table at a specific row and column.
	 * @param bt The binary table.
	 * @param row The row index.
	 * @param column the column index.
	 * @return The object at the specific position.
	 * @throws JPARSECException If the input HDU is not a binary table.
	 * @throws FitsException In case nom.tam.fits throws an error reading the data.
	 */
	public static Object getBinaryTableElement(BasicHDU bt, int row, int column) throws JPARSECException, FitsException {
		if (!FitsIO.isBinaryTable(bt)) throw new JPARSECException("Input HDU is not a binary table!");

    	BinaryTableHDU bintable = (BinaryTableHDU) bt;
		return bintable.getRow(row)[column];
	}

	/**
	 * Transforms a binary table into a String table containing the names of the
	 * columns in the first row, and the data for each row after the first one.
	 * @param bt The binary table.
	 * @return A string table with data as [rows][columns].
	 * @throws JPARSECException If the input HDU is not a binary table.
	 * @throws FitsException In case nom.tam.fits throws an error reading the data.
	 */
	public static String[][] getAsciiTable(BasicHDU bt) throws JPARSECException, FitsException {
		if (!FitsIO.isAsciiTable(bt)) throw new JPARSECException("Input HDU is not an Ascii table!");

    	AsciiTableHDU bintable = (AsciiTableHDU) bt;
    	String[][] table = new String[bintable.getNRows()+1][bintable.getNCols()];
    	for (int j=0; j<bintable.getNCols(); j++) {
    		table[0][j] = bintable.getColumnName(j);
    	}
    	for (int j=0; j<bintable.getNRows(); j++) {
    		Object[] data = bintable.getRow(j);
    		for (int k=0; k<data.length; k++) {
    			table[j+1][k] = ((String[]) data[k])[0];
    		}
    	}
		return table;
	}
}

