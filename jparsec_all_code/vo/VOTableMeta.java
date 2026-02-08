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

/**
 * A class to set metadata for VO tables.<P>
 *
 * Metadata are atributes to resources, tables, or fields in the VO Table.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class VOTableMeta implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID constant for float datatype.
	 */
	public static final String DATATYPE_FLOAT = "float";
	/**
	 * ID constant for short datatype.
	 */
	public static final String DATATYPE_SHORT = "short";
	/**
	 * ID constant for integer datatype.
	 */
	public static final String DATATYPE_INT = "int";
	/**
	 * ID constant for character datatype.
	 */
	public static final String DATATYPE_CHAR = "char";
	/**
	 * ID constant for double datatype.
	 */
	public static final String DATATYPE_DOUBLE = "double";

	/**
	 * ID constant for magnitude unit.
	 */
	public static final String UNIT_MAG = "mag";
	/**
	 * ID constant for degree unit.
	 */
	public static final String UNIT_DEG = "deg";
	/**
	 * ID constant for milliarcsecond unit.
	 */
	public static final String UNIT_MAS = "mas";
	/**
	 * ID constant for milliarcsecond per year unit.
	 */
	public static final String UNIT_MAS_BY_YEAR = "mas/yr";
	/**
	 * ID constant for pixel unit.
	 */
	public static final String UNIT_PIX = "pix";
	/**
	 * ID constant for Jansky unit.
	 */
	public static final String UNIT_JY = "Jy";
	/**
	 * ID constant for kilometer per second unit.
	 */
	public static final String UNIT_KMS = "km/s";
	/**
	 * ID constant for Kelvin unit.
	 */
	public static final String UNIT_K = "K";
	/**
	 * ID constant for second unit.
	 */
	public static final String UNIT_S = "s";

	/**
	 * Name.
	 */
	public String name;
	/**
	 * UCD code.
	 */
	public String ucd;
	/**
	 * Unit.
	 */
	public String unit;
	/**
	 * Data type.
	 */
	public String datatype;
	/**
	 * Array size.
	 */
	public String arraysize;
	/**
	 * Description.
	 */
	public String description;
	/**
	 * ID.
	 */
	public String id;
	/**
	 * Precission in decimal places (as an int value).
	 */
	public String precision;
	/**
	 * Width or field length in characters.
	 */
	public String width;
	/**
	 * Reference field, for example J2000 for equatorial coordinates.
	 */
	public String ref;
	/**
	 * Simple constructor adequate for tables and resources.
	 * @param name Name.
	 * @param id ID.
	 * @param description Description.
	 */
	public VOTableMeta(String name, String id, String description)
	{
		this.name = name;
		this.id = id;
		this.description = description;
	}
	private VOTableMeta() {}
	/**
	 * Constructor adequate for fields.
	 * @param name Name.
	 * @param id ID.
	 * @param description Description.
	 * @param datatype Data type. Some constants defined in this class.
	 * @param precision Precision.
	 * @param width Width.
	 * @param ucd UCD code.
	 * @param unit Unit. Some constants defined in this class.
	 */
	public VOTableMeta(String name, String id, String description, String datatype, String precision,
			String width, String ucd, String unit)
	{
		this.name = name;
		this.id = id;
		this.description = description;
		this.datatype = datatype;
		this.precision = precision;
		this.width = width;
		this.ucd = ucd;
		this.unit = unit;
	}

	/**
	 * Returns true if the input object is equals to this instance.
	 */
	@Override
	public boolean equals (Object o)
	{
		if (this == o) return true;
		if (!(o instanceof VOTableMeta)) return false;

		VOTableMeta that = (VOTableMeta) o;

		if (name != null ? !name.equals(that.name) : that.name != null) return false;
		if (ucd != null ? !ucd.equals(that.ucd) : that.ucd != null) return false;
		if (unit != null ? !unit.equals(that.unit) : that.unit != null) return false;
		if (datatype != null ? !datatype.equals(that.datatype) : that.datatype != null) return false;
		if (arraysize != null ? !arraysize.equals(that.arraysize) : that.arraysize != null) return false;
		if (description != null ? !description.equals(that.description) : that.description != null) return false;
		if (id != null ? !id.equals(that.id) : that.id != null) return false;
		if (precision != null ? !precision.equals(that.precision) : that.precision != null) return false;
		if (width != null ? !width.equals(that.width) : that.width != null) return false;

		return !(ref != null ? !ref.equals(that.ref) : that.ref != null);
	}

	@Override
	public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + (ucd != null ? ucd.hashCode() : 0);
		result = 31 * result + (unit != null ? unit.hashCode() : 0);
		result = 31 * result + (datatype != null ? datatype.hashCode() : 0);
		result = 31 * result + (arraysize != null ? arraysize.hashCode() : 0);
		result = 31 * result + (description != null ? description.hashCode() : 0);
		result = 31 * result + (id != null ? id.hashCode() : 0);
		result = 31 * result + (precision != null ? precision.hashCode() : 0);
		result = 31 * result + (width != null ? width.hashCode() : 0);
		result = 31 * result + (ref != null ? ref.hashCode() : 0);
		return result;
	}

	/**
	 * Clones this instance.
	 */
	@Override
	public Object clone()
	{
		VOTableMeta v = new VOTableMeta();

		v.arraysize = this.arraysize;
		v.datatype = this.datatype;
		v.description = this.description;
		v.id = this.id;
		v.name = this.name;
		v.precision = this.precision;
		v.ref = this.ref;
		v.ucd = this.ucd;
		v.unit = this.unit;
		v.width = this.width;
		return v;
	}
}
