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

import cds.savot.model.*;

import jparsec.util.JPARSECException;

/**
 * A class to access data in a VO Table.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class VOTableUtils
{
	// private constructor so that this class cannot be instantiated.
	private VOTableUtils() {}

	/**
	 * Returns the name of a field in certain VO table.
	 * @param sv VO table.
	 * @param catalogName Name of the catalog to analize.
	 * @param index Index number for the field, from 0 to the number of fields minus 1.
	 * @param tableIndex Index for the table, 0 unless the catalog contains more than 1 and
	 * the first one is not the one where the field is located.
	 * @return Field name. Empty string if catalog is not found.
	 * @throws JPARSECException If index is out of range.
	 */
	public static String getFieldNameInVOTable(SavotVOTable sv, String catalogName, int index, int tableIndex)
	throws JPARSECException {
		String name = "";

		// For each resource
        for (int l = 0; l < sv.getResources().getItemCount(); l++)
        {
          SavotResource currentResource = (SavotResource)(sv.getResources().getItemAt(l));
          String catalog = currentResource.getName().trim();

          if (catalog.toLowerCase().equals(catalogName.toLowerCase())) {
		  	  SavotTable svot = (SavotTable) currentResource.getTables().getItemAt(tableIndex);
		  	  int n = svot.getFields().getItemCount();

		  	  if (index >= n) throw new JPARSECException("index out of range.");

			  SavotField sp = (SavotField) svot.getFields().getItemAt(index);
			  name = sp.getName();
			  break;
          }
        }

        return name;
	}
	/**
	 * Returns the datatype of a field in certain VO table.
	 * @param sv VO table.
	 * @param catalogName Name of the catalog to analize.
	 * @param index Index number for the field, from 0 to the number of fields minus 1.
	 * @param tableIndex Index for the table, 0 unless the catalog contains more than 1 and
	 * the first one is not the one where the field is located.
	 * @return Field datatype. Empty string if catalog is not found.
	 * @throws JPARSECException If index is out of range.
	 */
	public static String getFieldDataTypeInVOTable(SavotVOTable sv, String catalogName, int index, int tableIndex)
	throws JPARSECException {
		String name = "";

		// For each resource
        for (int l = 0; l < sv.getResources().getItemCount(); l++)
        {
          SavotResource currentResource = (SavotResource)(sv.getResources().getItemAt(l));
          String catalog = currentResource.getName().trim();

          if (catalog.toLowerCase().equals(catalogName.toLowerCase())) {
		  	  SavotTable svot = (SavotTable) currentResource.getTables().getItemAt(tableIndex);
		  	  int n = svot.getFields().getItemCount();

		  	  if (index >= n) throw new JPARSECException("index out of range.");

			  SavotField sp = (SavotField) svot.getFields().getItemAt(index);
			  name = sp.getDataType();
			  break;
          }
        }

        return name;
	}
	/**
	 * Returns the description of a field in certain VO table.
	 * @param sv VO table.
	 * @param catalogName Name of the catalog to analyze.
	 * @param index Index number for the field, from 0 to the number of fields minus 1.
	 * @param tableIndex Index for the table, 0 unless the catalog contains more than 1 and
	 * the first one is not the one where the field is located.
	 * @return Field description. Empty string if catalog is not found.
	 * @throws JPARSECException If index is out of range.
	 */
	public static String getFieldDescriptionInVOTable(SavotVOTable sv, String catalogName, int index,
			int tableIndex)
	throws JPARSECException {
		String name = "";

		// For each resource
        for (int l = 0; l < sv.getResources().getItemCount(); l++)
        {
          SavotResource currentResource = (SavotResource)(sv.getResources().getItemAt(l));
          String catalog = currentResource.getName().trim();

          if (catalog.toLowerCase().equals(catalogName.toLowerCase())) {
		  	  SavotTable svot = (SavotTable) currentResource.getTables().getItemAt(tableIndex);
		  	  int n = svot.getFields().getItemCount();

		  	  if (index >= n) throw new JPARSECException("index out of range.");

			  SavotField sp = (SavotField) svot.getFields().getItemAt(index);
			  name = sp.getDescription();
			  break;
          }
        }

        return name;
	}
	/**
	 * Returns the id of a field in certain VO table.
	 * @param sv VO table.
	 * @param catalogName Name of the catalog to analize.
	 * @param index Index number for the field, from 0 to the number of fields minus 1.
	 * @param tableIndex Index for the table, 0 unless the catalog contains more than 1 and
	 * the first one is not the one where the field is located.
	 * @return Field id. Empty string if catalog is not found.
	 * @throws JPARSECException If index is out of range.
	 */
	public static String getFieldIDInVOTable(SavotVOTable sv, String catalogName, int index, int tableIndex)
	throws JPARSECException {
		String name = "";

		// For each resource
        for (int l = 0; l < sv.getResources().getItemCount(); l++)
        {
          SavotResource currentResource = (SavotResource)(sv.getResources().getItemAt(l));
          String catalog = currentResource.getName().trim();

          if (catalog.toLowerCase().equals(catalogName.toLowerCase())) {
		  	  SavotTable svot = (SavotTable) currentResource.getTables().getItemAt(tableIndex);
		  	  int n = svot.getFields().getItemCount();

		  	  if (index >= n) throw new JPARSECException("index out of range.");

			  SavotField sp = (SavotField) svot.getFields().getItemAt(index);
			  name = sp.getId();
			  break;
          }
        }

        return name;
	}
	/**
	 * Returns the precission of a field in certain VO table.
	 * @param sv VO table.
	 * @param catalogName Name of the catalog to analize.
	 * @param index Index number for the field, from 0 to the number of fields minus 1.
	 * @param tableIndex Index for the table, 0 unless the catalog contains more than 1 and
	 * the first one is not the one where the field is located.
	 * @return Field precission. Empty string if catalog is not found.
	 * @throws JPARSECException If index is out of range.
	 */
	public static String getFieldPrecissionInVOTable(SavotVOTable sv, String catalogName, int index, int tableIndex)
	throws JPARSECException {
		String name = "";

		// For each resource
        for (int l = 0; l < sv.getResources().getItemCount(); l++)
        {
          SavotResource currentResource = (SavotResource)(sv.getResources().getItemAt(l));
          String catalog = currentResource.getName().trim();

          if (catalog.toLowerCase().equals(catalogName.toLowerCase())) {
		  	  SavotTable svot = (SavotTable) currentResource.getTables().getItemAt(tableIndex);
		  	  int n = svot.getFields().getItemCount();

		  	  if (index >= n) throw new JPARSECException("index out of range.");

			  SavotField sp = (SavotField) svot.getFields().getItemAt(index);
			  name = sp.getPrecision();
			  break;
          }
        }

        return name;
	}
	/**
	 * Returns the ref of a field in certain VO table.
	 * @param sv VO table.
	 * @param catalogName Name of the catalog to analize.
	 * @param index Index number for the field, from 0 to the number of fields minus 1.
	 * @param tableIndex Index for the table, 0 unless the catalog contains more than 1 and
	 * the first one is not the one where the field is located.
	 * @return Field ref. Empty string if catalog is not found.
	 * @throws JPARSECException If index is out of range.
	 */
	public static String getFieldRefInVOTable(SavotVOTable sv, String catalogName, int index, int tableIndex)
	throws JPARSECException {
		String name = "";

		// For each resource
        for (int l = 0; l < sv.getResources().getItemCount(); l++)
        {
          SavotResource currentResource = (SavotResource)(sv.getResources().getItemAt(l));
          String catalog = currentResource.getName().trim();

          if (catalog.toLowerCase().equals(catalogName.toLowerCase())) {
		  	  SavotTable svot = (SavotTable) currentResource.getTables().getItemAt(tableIndex);
		  	  int n = svot.getFields().getItemCount();

		  	  if (index >= n) throw new JPARSECException("index out of range.");

			  SavotField sp = (SavotField) svot.getFields().getItemAt(index);
			  name = sp.getRef();
			  break;
          }
        }

        return name;
	}
	/**
	 * Returns the type of a field in certain VO table.
	 * @param sv VO table.
	 * @param catalogName Name of the catalog to analize.
	 * @param index Index number for the field, from 0 to the number of fields minus 1.
	 * @param tableIndex Index for the table, 0 unless the catalog contains more than 1 and
	 * the first one is not the one where the field is located.
	 * @return Field type. Empty string if catalog is not found.
	 * @throws JPARSECException If index is out of range.
	 */
	public static String getFieldTypeInVOTable(SavotVOTable sv, String catalogName, int index, int tableIndex)
	throws JPARSECException {
		String name = "";

		// For each resource
        for (int l = 0; l < sv.getResources().getItemCount(); l++)
        {
          SavotResource currentResource = (SavotResource)(sv.getResources().getItemAt(l));
          String catalog = currentResource.getName().trim();

          if (catalog.toLowerCase().equals(catalogName.toLowerCase())) {
		  	  SavotTable svot = (SavotTable) currentResource.getTables().getItemAt(tableIndex);
		  	  int n = svot.getFields().getItemCount();

		  	  if (index >= n) throw new JPARSECException("index out of range.");

			  SavotField sp = (SavotField) svot.getFields().getItemAt(index);
			  name = sp.getType();
			  break;
          }
        }

        return name;
	}
	/**
	 * Returns the UCD code of a field in certain VO table.
	 * @param sv VO table.
	 * @param catalogName Name of the catalog to analize.
	 * @param index Index number for the field, from 0 to the number of fields minus 1.
	 * @param tableIndex Index for the table, 0 unless the catalog contains more than 1 and
	 * the first one is not the one where the field is located.
	 * @return Field UCD. Empty string if catalog is not found.
	 * @throws JPARSECException If index is out of range.
	 */
	public static String getFieldUCDInVOTable(SavotVOTable sv, String catalogName, int index, int tableIndex)
	throws JPARSECException {
		String name = "";

		// For each resource
        for (int l = 0; l < sv.getResources().getItemCount(); l++)
        {
          SavotResource currentResource = (SavotResource)(sv.getResources().getItemAt(l));
          String catalog = currentResource.getName().trim();

          if (catalog.toLowerCase().equals(catalogName.toLowerCase())) {
		  	  SavotTable svot = (SavotTable) currentResource.getTables().getItemAt(tableIndex);
		  	  int n = svot.getFields().getItemCount();

		  	  if (index >= n) throw new JPARSECException("index out of range.");

			  SavotField sp = (SavotField) svot.getFields().getItemAt(index);
			  name = sp.getUcd();
			  break;
          }
        }

        return name;
	}
	/**
	 * Returns the unit of a field in certain VO table.
	 * @param sv VO table.
	 * @param catalogName Name of the catalog to analize.
	 * @param index Index number for the field, from 0 to the number of fields minus 1.
	 * @param tableIndex Index for the table, 0 unless the catalog contains more than 1 and
	 * the first one is not the one where the field is located.
	 * @return Field unit. Empty string if catalog is not found.
	 * @throws JPARSECException If index is out of range.
	 */
	public static String getFieldUnitInVOTable(SavotVOTable sv, String catalogName, int index, int tableIndex)
	throws JPARSECException {
		String name = "";

		// For each resource
        for (int l = 0; l < sv.getResources().getItemCount(); l++)
        {
          SavotResource currentResource = (SavotResource)(sv.getResources().getItemAt(l));
          String catalog = currentResource.getName().trim();

          if (catalog.toLowerCase().equals(catalogName.toLowerCase())) {
		  	  SavotTable svot = (SavotTable) currentResource.getTables().getItemAt(tableIndex);
		  	  int n = svot.getFields().getItemCount();

		  	  if (index >= n) throw new JPARSECException("index out of range.");

			  SavotField sp = (SavotField) svot.getFields().getItemAt(index);
			  name = sp.getUnit();
			  break;
          }
        }

        return name;
	}
	/**
	 * Returns the Utype of a field in certain VO table.
	 * @param sv VO table.
	 * @param catalogName Name of the catalog to analize.
	 * @param index Index number for the field, from 0 to the number of fields minus 1.
	 * @param tableIndex Index for the table, 0 unless the catalog contains more than 1 and
	 * the first one is not the one where the field is located.
	 * @return Field Utype. Empty string if catalog is not found.
	 * @throws JPARSECException If index is out of range.
	 */
	public static String getFieldUTypeInVOTable(SavotVOTable sv, String catalogName, int index, int tableIndex)
	throws JPARSECException {
		String name = "";

		// For each resource
        for (int l = 0; l < sv.getResources().getItemCount(); l++)
        {
          SavotResource currentResource = (SavotResource)(sv.getResources().getItemAt(l));
          String catalog = currentResource.getName().trim();

          if (catalog.toLowerCase().equals(catalogName.toLowerCase())) {
		  	  SavotTable svot = (SavotTable) currentResource.getTables().getItemAt(tableIndex);
		  	  int n = svot.getFields().getItemCount();

		  	  if (index >= n) throw new JPARSECException("index out of range.");

			  SavotField sp = (SavotField) svot.getFields().getItemAt(index);
			  name = sp.getUtype();
			  break;
          }
        }

        return name;
	}
	/**
	 * Returns the width of a field in certain VO table.
	 * @param sv VO table.
	 * @param catalogName Name of the catalog to analize.
	 * @param index Index number for the field, from 0 to the number of fields minus 1.
	 * @param tableIndex Index for the table, 0 unless the catalog contains more than 1 and
	 * the first one is not the one where the field is located.
	 * @return Field width. Empty string if catalog is not found.
	 * @throws JPARSECException If index is out of range.
	 */
	public static String getFieldWidthInVOTable(SavotVOTable sv, String catalogName, int index, int tableIndex)
	throws JPARSECException {
		String name = "";

		// For each resource
        for (int l = 0; l < sv.getResources().getItemCount(); l++)
        {
          SavotResource currentResource = (SavotResource)(sv.getResources().getItemAt(l));
          String catalog = currentResource.getName().trim();

          if (catalog.toLowerCase().equals(catalogName.toLowerCase())) {
		  	  SavotTable svot = (SavotTable) currentResource.getTables().getItemAt(tableIndex);
		  	  int n = svot.getFields().getItemCount();

		  	  if (index >= n) throw new JPARSECException("index out of range.");

			  SavotField sp = (SavotField) svot.getFields().getItemAt(index);
			  name = sp.getWidth();
			  break;
          }
        }

        return name;
	}
	/**
	 * Returns the width of a field in certain VO table.
	 * @param sv VO table.
	 * @param catalogName Name of the catalog to analize.
	 * @param tableIndex Index for the table, 0 unless the catalog contains more than 1 and
	 * the first one is not the one where the field is located.
	 * @return Field count. -1 if catalog is not found.
	 */
	public static int getFieldCountInVOTable(SavotVOTable sv, String catalogName, int tableIndex)
	{
		int n = -1;

		// For each resource
        for (int l = 0; l < sv.getResources().getItemCount(); l++)
        {
          SavotResource currentResource = (SavotResource)(sv.getResources().getItemAt(l));
          String catalog = currentResource.getName().trim();

          if (catalog.toLowerCase().equals(catalogName.toLowerCase())) {
		  	  SavotTable svot = (SavotTable) currentResource.getTables().getItemAt(tableIndex);
		  	  n = svot.getFields().getItemCount();
		  	  break;
          }
        }

        return n;
	}
}
