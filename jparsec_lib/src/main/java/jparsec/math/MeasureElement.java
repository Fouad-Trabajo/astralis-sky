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
package jparsec.math;

import java.io.Serializable;

import jparsec.astronomy.PhotometricBandElement;
import jparsec.astronomy.Photometry;
import jparsec.ephem.Functions;
import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.math.Converter;
import jparsec.math.FastMath;
import jparsec.util.JPARSECException;

/**
 * The most simple representation of a measure: a value, its error, and the unit.<P>
 * A value can be defined as an upper or lower limit if desired. To
 * do that use the prefix '&lt;' or '&gt;' respectively.<BR>
 * Common operations are allowed with two measures. If the units are different,
 * the unit of the second measure is transformed to the unit of the main one. Error
 * calculations are based on standard procedures, using derivatives.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class MeasureElement implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * The value.
	 */
	public String value;
	/**
	 * The error.
	 */
	public double error;
	/**
	 * The unit.
	 */
	public String unit;

	/**
	 * Simple constructor.
	 * @param x Value.
	 * @param dx Error.
	 * @param unit Unit.
	 */
	public MeasureElement(String x, double dx, String unit)
	{
		this.value = x;
		this.error = dx;
		this.unit = unit;
	}
	/**
	 * Simple constructor.
	 * @param x Value.
	 * @param dx Error.
	 * @param unit Unit. For channel number use null.
	 */
	public MeasureElement(double x, double dx, String unit)
	{
		this.value = ""+x;
		this.error = dx;
		this.unit = unit;
	}

	/**
	 * Clones this instance.
	 */
	@Override
	public MeasureElement clone()
	{
		MeasureElement p = new MeasureElement(this.value, this.error, this.unit);
		return p;
	}

	/**
	 * Returns if this instance is equals to another.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof MeasureElement)) return false;

		MeasureElement that = (MeasureElement) o;

		if (Double.compare(that.error, error) != 0) return false;
		if (value != null ? !value.equals(that.value) : that.value != null) return false;

		return !(unit != null ? !unit.equals(that.unit) : that.unit != null);
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		result = value != null ? value.hashCode() : 0;
		temp = Double.doubleToLongBits(error);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (unit != null ? unit.hashCode() : 0);
		return result;
	}

	/**
	 * ID constant for microns in the x axis.
	 */
	public static final String UNIT_X_MICRON = "um";
	/**
	 * ID constant for cm in the x axis.
	 */
	public static final String UNIT_X_CM = "cm";
	/**
	 * ID constant for km/s in the x axis.
	 */
	public static final String UNIT_X_KMS = "km/s";
	/**
	 * ID constant for AU in the x axis.
	 */
	public static final String UNIT_X_AU = "AU";
	/**
	 * ID constant for Hz in the x axis.
	 */
	public static final String UNIT_X_HZ = "Hz";
	/**
	 * ID constant for MHz in the x axis.
	 */
	public static final String UNIT_X_MHZ = "MHz";
	/**
	 * ID constant for GHz in the x axis.
	 */
	public static final String UNIT_X_GHZ = "GHz";
	/**
	 * ID constant for Angstrom in the x axis.
	 */
	public static final String UNIT_X_ANGSTROM = "Angstrom";
	/**
	 * ID constant for radians in the x axis.
	 */
	public static final String UNIT_X_RADIANS = "rad";
	/**
	 * ID constant for solar radius in the x axis.
	 */
	public static final String UNIT_X_SOLAR_RADIUS = "SolRad";
	/**
	 * ID constant for solar mass in the x axis.
	 */
	public static final String UNIT_X_SOLAR_MASS = "SolMass";
	/**
	 * ID constant for arcsec in the x axis.
	 */
	public static final String UNIT_X_ARCSEC = "arcsec";
	/**
	 * ID constant for parsec in the x axis.
	 */
	public static final String UNIT_X_PARSEC = "pc";
	/**
	 * ID constant for light year in the x axis.
	 */
	public static final String UNIT_X_LIGHT_YEAR = "lyr";
	/**
	 * ID constant for second in the x axis.
	 */
	public static final String UNIT_X_SECOND = "s";
	/**
	 * ID constant for julian day in the x axis.
	 */
	public static final String UNIT_X_JULIAN_DAY = "JD";

	/**
	 * ID constant for K in the y axis.
	 */
	public static final String UNIT_Y_K = "K";
	/**
	 * ID constant for Jy in the y axis.
	 */
	public static final String UNIT_Y_JY = "Jy";
	/**
	 * ID constant for mJy in the y axis.
	 */
	public static final String UNIT_Y_MJY = "mJy";
	/**
	 * ID constant for magnitudes in the y axis. This unit should only be used with a
	 * {@linkplain jparsec.astrophysics.FluxElement} instance, that contains a given photometric band.
	 */
	public static final String UNIT_Y_MAG = "MAG";
	/**
	 * ID constant for W m<sup>-2</sup> Hz<sup>-1</sup> in the y axis.
	 */
	public static final String UNIT_Y_WM2HZ = "W/m2/Hz";
	/**
	 * ID constant for erg s<sup>-1</sup> cm<sup>-2</sup> Hz<sup>-1</sup> in the y axis.
	 */
	public static final String UNIT_Y_ERGSCM2HZ = "erg/s/cm2/Hz";
	/**
	 * ID constant for solar luminosity in the y axis.
	 */
	public static final String UNIT_Y_SOLAR_LUMINOSITY = "SolLum";
	/**
	 * ID constant for photons as unit.
	 */
	public static final String UNIT_Y_PHOTON = "ph";
	/**
	 * ID constant for magnitudes in the y axis, Johnson system, U band.
	 */
	public static final String UNIT_Y_MAG_JOHNSON_U = PhotometricBandElement.BAND_U_JOHNSON_MORGAN.name;
	/**
	 * ID constant for magnitudes in the y axis, Johnson system, B band.
	 */
	public static final String UNIT_Y_MAG_JOHNSON_B = PhotometricBandElement.BAND_B_JOHNSON_MORGAN.name;
	/**
	 * ID constant for magnitudes in the y axis, Johnson system, V band.
	 */
	public static final String UNIT_Y_MAG_JOHNSON_V = PhotometricBandElement.BAND_V_JOHNSON_MORGAN.name;
	/**
	 * ID constant for magnitudes in the y axis, Johnson system, R band.
	 */
	public static final String UNIT_Y_MAG_JOHNSON_R = PhotometricBandElement.BAND_R_JOHNSON_MORGAN.name;
	/**
	 * ID constant for magnitudes in the y axis, Johnson system, I band.
	 */
	public static final String UNIT_Y_MAG_JOHNSON_I = PhotometricBandElement.BAND_I_JOHNSON_MORGAN.name;
	/**
	 * ID constant for magnitudes in the y axis, Johnson system, J band.
	 */
	public static final String UNIT_Y_MAG_JOHNSON_J = PhotometricBandElement.BAND_J_JOHNSON_MORGAN.name;
	/**
	 * ID constant for magnitudes in the y axis, Johnson system, H band.
	 */
	public static final String UNIT_Y_MAG_JOHNSON_H = PhotometricBandElement.BAND_H_JOHNSON_MORGAN.name;
	/**
	 * ID constant for magnitudes in the y axis, Johnson system, K band.
	 */
	public static final String UNIT_Y_MAG_JOHNSON_K = PhotometricBandElement.BAND_K_JOHNSON_MORGAN.name;
	/**
	 * ID constant for magnitudes in the y axis, Johnson system, L band.
	 */
	public static final String UNIT_Y_MAG_JOHNSON_L = PhotometricBandElement.BAND_L_JOHNSON_MORGAN.name;
	/**
	 * ID constant for magnitudes in the y axis, Johnson system, M band.
	 */
	public static final String UNIT_Y_MAG_JOHNSON_M = PhotometricBandElement.BAND_M_JOHNSON_MORGAN.name;
	/**
	 * ID constant for magnitudes in the y axis, Johnson system, N band.
	 */
	public static final String UNIT_Y_MAG_JOHNSON_N = PhotometricBandElement.BAND_N_JOHNSON_MORGAN.name;
	/**
	 * ID constant for magnitudes in the y axis, 2MASS system, J band.
	 */
	public static final String UNIT_Y_MAG_2MASS_J = PhotometricBandElement.BAND_J_2MASS.name;
	/**
	 * ID constant for magnitudes in the y axis, 2MASS system, H band.
	 */
	public static final String UNIT_Y_MAG_2MASS_H = PhotometricBandElement.BAND_H_2MASS.name;
	/**
	 * ID constant for magnitudes in the y axis, 2MASS system, Ks band.
	 */
	public static final String UNIT_Y_MAG_2MASS_Ks = PhotometricBandElement.BAND_Ks_2MASS.name;
	
	private String convert(String value, String newUnit)
	throws JPARSECException {
		if (this.unit.equals(newUnit) || newUnit == null) return value;

		if (this.unit.equals(MeasureElement.UNIT_Y_MAG) || newUnit.equals(MeasureElement.UNIT_Y_MAG))
			throw new JPARSECException("general magnitude conversion is not supported here.");

		// Reduce units
		String tunit = this.unit;
/*		if (this.unit != null && !this.unit.equals("")) {
			for (int i=unit.length()-1; i>=0; i--) {
				if (DataSet.isDoubleFastCheck(unit.substring(i))) {
					tunit = unit.substring(0, i);
					break;
				}
			}
		}
*/
		String tNewUnit = newUnit;
		if (newUnit != null && !newUnit.equals("")) {
			for (int i=newUnit.length()-1; i>=0; i--) {
				if (DataSet.isDoubleFastCheck(newUnit.substring(i))) {
					tNewUnit = newUnit.substring(0, i);
					break;
				}
			}
		}

		Converter c = new Converter(tunit, tNewUnit);
		double val = c.convert(DataSet.getDoubleValueWithoutLimit(value));
		String out = ""+val;
		if (value.startsWith("<") || value.startsWith(">")) out = value.substring(0, 1) + out;
		return out;
	}

	/**
	 * Converts a given value to a new Unit.
	 * @param newUnit New unit in standard conventions.
	 * @throws JPARSECException If an error occurs.
	 */
	public void convert(String newUnit)
	throws JPARSECException {
		if (this.unit.equals(newUnit)) return;

		PhotometricBandElement pb1 = PhotometricBandElement.getPhotometricBand(this.unit);
		PhotometricBandElement pb2 = PhotometricBandElement.getPhotometricBand(newUnit);
		if (pb1 != null && pb2 != null)
			throw new JPARSECException("cannot convert magnitudes in different photometric bands or systems.");

		if (pb1 != null)
		{
			MeasureElement out = Photometry.getFluxFromMagnitude(DataSet.getDoubleValueWithoutLimit(this.value), this.error, pb1);
			String limit = DataSet.getLimit(this.value);
			this.value = limit + out.getValue();
			this.error = out.error;
			this.unit = MeasureElement.UNIT_Y_JY;
		} else {
			if (pb2 != null)
			{
				MeasureElement out = Photometry.getMagnitudeFromFlux(DataSet.getDoubleValueWithoutLimit(this.value), this.error, pb2);
				String limit = DataSet.getLimit(this.value);
				this.value = limit + out.getValue();
				this.error = out.error;
				this.unit = pb2.name;
			}
		}

		if (pb2 != null) {
			this.value = this.convert(this.value, newUnit);
			this.error = DataSet.parseDouble(this.convert(""+this.error, newUnit));
			this.unit = newUnit;
		} else {
			try {
				this.value = this.convert(this.value, newUnit);
			} catch (Exception exc) {
				return;
			}
			this.error = DataSet.parseDouble(this.convert(""+this.error, newUnit));
			String tunit = newUnit;
			if (newUnit != null && !newUnit.equals("")) {
				for (int i=newUnit.length()-1; i>=0; i--) {
					if (DataSet.isDoubleFastCheck(newUnit.substring(i))) {
						tunit = newUnit.substring(0, i);
						break;
					}
				}
			}
			this.unit = tunit;

		}
	}
	
	/**
	 * Obtains the current value in a new unit.
	 * @param newUnit The new unit.
	 * @return The converted value.
	 * @throws JPARSECException If an error occurs.
	 */
	public MeasureElement get(String newUnit)
	throws JPARSECException {
		if ((unit == null && newUnit == null) || this.unit.equals(newUnit)) return this.clone();

		PhotometricBandElement pb1 = PhotometricBandElement.getPhotometricBand(this.unit);
		PhotometricBandElement pb2 = PhotometricBandElement.getPhotometricBand(newUnit);
		if (pb1 != null && pb2 != null)
			throw new JPARSECException("cannot convert magnitudes in different photometric bands or systems.");

		MeasureElement me = this.clone();
		if (pb1 != null)
		{
			MeasureElement out = Photometry.getFluxFromMagnitude(DataSet.getDoubleValueWithoutLimit(me.value), me.error, pb1);
			String limit = DataSet.getLimit(me.value);
			me.value = limit + out.getValue();
			me.error = out.error;
			me.unit = MeasureElement.UNIT_Y_JY;
		} else {
			if (pb2 != null)
			{
				MeasureElement out = Photometry.getMagnitudeFromFlux(DataSet.getDoubleValueWithoutLimit(me.value), me.error, pb2);
				String limit = DataSet.getLimit(me.value);
				me.value = limit + out.getValue();
				me.error = out.error;
				me.unit = pb2.name;
			}
		}

		String value = me.convert(me.value, newUnit);
		double error = DataSet.parseDouble(me.convert(""+me.error, newUnit));
		MeasureElement p = new MeasureElement(value, error, newUnit);
		return p;
	}

	/**
	 * Returns the limit '&lt;' or '&gt;'.
	 * @return The limit, or empty String if it does not exist.
	 */
	public String getLimit()
	{
		return DataSet.getLimit(this.value);
	}

	/**
	 * Returns the numerical value without the limit.
	 * @return Value of the measure.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getValue()
	throws JPARSECException {
		return DataSet.getDoubleValueWithoutLimit(this.value);
	}

	/**
	 * Multiply the current measure by a double.
	 * @param val The double.
	 * @throws JPARSECException If an error occurs.
	 */
	public void multiply(double val)
	throws JPARSECException {
		double newVal = val * DataSet.getDoubleValueWithoutLimit(this.value);
		this.value = this.getLimit() + newVal;
		this.error *= val;
	}

	/**
	 * Multiply a measure by another.
	 * @param me The other measure. The unit will be transformed to the
	 * units of the current instance that calls this method, in case
	 * this is required and it is possible.
	 * @throws JPARSECException If an error occurs.
	 */
	public void multiply(MeasureElement me)
	throws JPARSECException {
		MeasureElement m = me.clone();
		if (unit != null || m.unit != null) {
			if (!this.unit.equals(m.unit)) m.convert(this.unit);
		}
		double v1 = DataSet.getDoubleValueWithoutLimit(this.value);
		String l1 = DataSet.getLimit(this.value);
		double v2 = DataSet.getDoubleValueWithoutLimit(m.value);
		this.value = l1+(v1*v2);
		this.error = Math.sqrt(Math.pow(v2*this.error, 2.0)+Math.pow(v1*m.error, 2.0));
		if (this.unit != null && !this.unit.equals("")) {
			int prev1 = 1;
			String unit1 = unit;
			for (int i=unit1.length()-1; i>=0; i--) {
				if (DataSet.isDoubleFastCheck(unit1.substring(i))) {
					prev1 = Integer.parseInt(unit1.substring(i));
					unit1 = unit1.substring(0, i);
					break;
				}
			}
			int complex1 = Math.max(unit1.indexOf("/"), unit1.indexOf("*"));
			String unit2 = m.unit;
			int prev2 = 1;
			for (int i=unit2.length()-1; i>=0; i--) {
				if (DataSet.isDoubleFastCheck(unit2.substring(i))) {
					prev2 = Integer.parseInt(unit2.substring(i));
					unit2 = unit2.substring(0, i);
					break;
				}
			}
			if (unit1.equals(unit2) && complex1 < 0) {
				int val = prev1 + prev2;
				if (val != 0) {
					unit = unit1;
					this.unit += ""+val;
				} else {
					this.unit = "";
				}
			} else {
				this.unit = "("+this.unit+")*("+m.unit+")";
			}
		}
	}
	/**
	 * Divides a measure by another.
	 * @param me The other measure. The unit will be transformed to the
	 * units of the current instance that calls this method, in case
	 * this is required and it is possible.
	 * @throws JPARSECException If an error occurs.
	 */
	public void divide(MeasureElement me)
	throws JPARSECException {
		MeasureElement m = me.clone();
		if (unit != null || m.unit != null) {
			if (!this.unit.equals(m.unit)) m.convert(this.unit);
		}
		double v1 = DataSet.getDoubleValueWithoutLimit(this.value);
		String l1 = DataSet.getLimit(this.value);
		double v2 = DataSet.getDoubleValueWithoutLimit(m.value);
		this.value = l1+(v1/v2);
		this.error = Math.sqrt(Math.pow(this.error/v2, 2.0)+Math.pow(-v1*m.error/(v2*v2), 2.0));
		if (this.unit != null && !this.unit.equals("")) {
			int prev1 = 1;
			String unit1 = unit;
			for (int i=unit1.length()-1; i>=0; i--) {
				if (DataSet.isDoubleFastCheck(unit1.substring(i))) {
					prev1 = Integer.parseInt(unit1.substring(i));
					unit1 = unit1.substring(0, i);
					break;
				}
			}
			int complex1 = Math.max(unit1.indexOf("/"), unit1.indexOf("*"));
			String unit2 = m.unit;
			int prev2 = 1;
			for (int i=unit2.length()-1; i>=0; i--) {
				if (DataSet.isDoubleFastCheck(unit2.substring(i))) {
					prev2 = Integer.parseInt(unit2.substring(i));
					unit2 = unit2.substring(0, i);
					break;
				}
			}
			if (unit1.equals(unit2) && complex1 < 0) {
				int val = prev1-prev2;
				if (val != 0) {
					unit = unit1;
					this.unit += ""+val;
				} else {
					this.unit = "";
				}
			} else {
				this.unit = "("+this.unit+")/("+m.unit+")";
			}
		}
	}
	/**
	 * Adds a measure to another.
	 * @param me The other measure.
	 * @throws JPARSECException If an error occurs.
	 */
	public void add(MeasureElement me)
	throws JPARSECException {
		MeasureElement m = me.clone();
		if (unit != null || m.unit != null) {
			if (!this.unit.equals(m.unit)) m.convert(this.unit);
			if (!m.unit.equals(this.unit)) throw new JPARSECException("Cannot add values in different units.");
		}
		double v1 = DataSet.getDoubleValueWithoutLimit(this.value);
		String l1 = DataSet.getLimit(this.value);
		double v2 = DataSet.getDoubleValueWithoutLimit(m.value);
		this.value = l1+(v1+v2);
		this.error = Math.sqrt(Math.pow(this.error, 2.0)+Math.pow(m.error, 2.0));
	}
	/**
	 * Adds a measure to another.
	 * @param me The other measure.
	 * @throws JPARSECException If an error occurs.
	 */
	public void subtract(MeasureElement me)
	throws JPARSECException {
		MeasureElement m = me.clone();
		if (unit != null || m.unit != null) {
			if (!this.unit.equals(m.unit)) m.convert(this.unit);
			if (!m.unit.equals(this.unit)) throw new JPARSECException("Cannot substract values in different units.");
		}
		double v1 = DataSet.getDoubleValueWithoutLimit(this.value);
		String l1 = DataSet.getLimit(this.value);
		double v2 = DataSet.getDoubleValueWithoutLimit(m.value);
		this.value = l1+(v1-v2);
		this.error = Math.sqrt(Math.pow(this.error, 2.0)+Math.pow(-m.error, 2.0));
	}
	/**
	 * Measure to the power of another.
	 * @param me The other measure.
	 * @throws JPARSECException If an error occurs.
	 */
	public void pow(MeasureElement me)
	throws JPARSECException {
		MeasureElement m = me.clone();
		if (unit != null || m.unit != null) {
			if (!this.unit.equals(m.unit)) m.convert(this.unit);
			if (me.unit != null && !me.unit.equals("")) throw new JPARSECException("Cannot apply pow operation using a measure with units as exponent.");
		}
		double v1 = DataSet.getDoubleValueWithoutLimit(this.value);
		String l1 = DataSet.getLimit(this.value);
		double v2 = DataSet.getDoubleValueWithoutLimit(m.value);
		this.value = l1+Math.pow(v1, v2);
		this.error = Math.sqrt(Math.pow(this.error * v2 * Math.pow(v1, v2 - 1.0), 2.0)+
				Math.pow(m.error * Math.pow(v1, v2) * Math.log(v1), 2.0));
	}
	/**
	 * Measure to the power of a double.
	 * @param d The double value.
	 * @throws JPARSECException If an error occurs.
	 */
	public void pow(double d)
	throws JPARSECException {
		double v1 = DataSet.getDoubleValueWithoutLimit(this.value);
		String l1 = DataSet.getLimit(this.value);
		this.value = l1+Math.pow(v1, d);
		this.error = this.error * d * Math.pow(v1, d - 1.0);
		this.unit = "("+this.unit+")^("+d+")";
	}
	/**
	 * Inverts a measure, a to 1/a.
	 * @throws JPARSECException If an error occurs.
	 */
	public void invert()
	throws JPARSECException {
		double v1 = DataSet.getDoubleValueWithoutLimit(this.value);
		String l1 = DataSet.getLimit(this.value);
		this.value = l1+(1.0/v1);
		this.error = Math.abs(this.error/(v1*v1));
		this.unit = "1.0/("+this.unit+")";
/*		if (this.unit != null && !this.unit.equals("")) { // Can be multiple units ...
			int prev = -1;
			for (int i=unit.length()-1; i>=0; i--) {
				if (DataSet.isDoubleFastCheck(unit.substring(i))) {
					prev = -Integer.parseInt(unit.substring(i));
					unit = unit.substring(0, i);
					break;
				}
			}
			this.unit += ""+prev;
		}
*/
	}

	/**
	 * Returns a String representation of this object following international
	 * conventions about error representation. For instance, the value
	 * 0.000125 +/- 0.00009 is returned as 0.00013 +/- 0.00010, taking into
	 * account the error to round the value properly. Empty String is returned
	 * in case of error. The unit is also written.
	 */
	@Override
	public String toString()
	{
		return formatToString(0, true);
	}
	/**
	 * Returns a String representation of this object following international
	 * conventions about error representation. For instance, the value
	 * 0.000125 +/- 0.00009 is returned as 0.00013 +/- 0.00010, taking into
	 * account the error to round the value properly, or as 0.00013 (0.00010)
	 * in case parenthesis are used. Empty String is returned in case of error.
	 * The unit is not written.
	 * @param useParentheses True to use parenthesis instead of +/-.
	 * @return The string.
	 */
	public String toString(boolean useParentheses)
	{
		String out = formatToString(0, false);
		if (useParentheses) out = formatWithParentheses(out);
		return out;
	}
	/**
	 * Returns a String representation of this object following international
	 * conventions about error representation. For instance, the value
	 * 0.000125 +/- 0.00009 is returned as 0.00013 +/- 0.00010, taking into
	 * account the error to round the value properly, or as 0.00013 (0.00010)
	 * in case parenthesis are used. Empty String is returned in case of error.
	 * The unit is written optionally.
	 * @param useParentheses True to use parenthesis instead of +/-.
	 * @param includeUnit True to include the unit in the output.
	 * @return The string.
	 */
	public String toString(boolean useParentheses, boolean includeUnit)
	{
		if (!includeUnit) return toString(useParentheses);

		if (!useParentheses) {
			if (!includeUnit) return formatToString(0, false);
			return this.toString();
		}

		String v = formatWithParentheses(formatToString(0, false));
		if (!includeUnit || unit == null) return v;
		return v + " " + this.unit;
	}
	/**
	 * Returns a String representation of this object using parenthesis
	 * instead of +/- optionally. For instance, the value 0.00013 +/-
	 * 0.00010 is returned as that string or as 0.00013 (0.00010). Empty
	 * String is returned in case of error. The unit is also written.
	 * @param useParentheses True to use parenthesis instead of +/-.
	 * @param decimalPlaces The number of significant digits to use for the error.
	 * Usually 2 when it can be reduced to 25 or less, and 1 otherwise, but
	 * you can use here the (non-standard) value you prefer.
	 * @return The string.
	 */
	public String toString(boolean useParentheses, int decimalPlaces)
	{
		if (!useParentheses) return this.formatToString(decimalPlaces, true);

		String v = this.formatToString(decimalPlaces, true);
		return formatWithParentheses(v);
	}

	// 0 decimalPlaces = obtain it automatically (1 for reduced error > 25 and 2 otherwise)
	private String formatToString(int decimalPlaces, boolean unit)
	{
		String out = "";

		try {
			double n = this.getValue();
			double dn = this.error;

			if (dn == 0.0 || Double.isNaN(dn) || Double.isInfinite(dn) || dn < 0) {
				if (unit && this.unit != null) return ""+this.getValue()+" +/- "+ dn + " " + this.unit.trim();
				return ""+this.getValue()+" +/- "+dn;
			}

			double m = 10.0;
			int nm = 0;
			do {
				if (dn > 90.0) {
					dn = dn / m;
					nm --;
				}
				if (dn < 9.0) {
					dn = dn * m;
					nm ++;
				}
			} while (dn < 9.0 || dn > 90.0);

			n = FastMath.multiplyBy10ToTheX(n, nm);

			int place = 1;
			if (dn <= 25) place = 0;
			if (decimalPlaces > 0) place = 2-decimalPlaces;
			if (place < 0) {
				dn = DataSet.parseDouble(Functions.formatValue((float)dn+0.5*FastMath.multiplyBy10ToTheX(1.0, place), -place, 2-place, true));
			} else {
				dn = DataSet.parseDouble(Functions.formatValue((float)dn, 0, 2-place, true));
			}

			n = Functions.roundToPlace(n, place);
			int afterDecimalPoint = 0;
			if (decimalPlaces == 0 && dn > 25) afterDecimalPoint = -1;

			n = FastMath.multiplyBy10ToTheX(n, -nm);
			dn = FastMath.multiplyBy10ToTheX(dn, -nm);

			if (nm > 0) afterDecimalPoint += nm;
			if (decimalPlaces > 0) afterDecimalPoint += decimalPlaces-2;
			out = this.getLimit()+Functions.formatValue(n, afterDecimalPoint)+" +/- "+Functions.formatValue(dn, afterDecimalPoint);

			if (Math.abs(nm) > 8) {
				n = FastMath.multiplyBy10ToTheX(n, nm);
				dn = FastMath.multiplyBy10ToTheX(dn, nm);
				afterDecimalPoint = 0;
				if (decimalPlaces > 0) afterDecimalPoint += decimalPlaces-2;
				String plus = "";
				if (nm < 0) plus = "+";
				out = this.getLimit()+Functions.formatValue(n, afterDecimalPoint)+"E"+plus+(-nm)+" +/- "+Functions.formatValue(dn, afterDecimalPoint)+"E"+plus+(-nm);
			}

			if (unit && this.unit != null) out += " "+this.unit.trim();
		} catch (JPARSECException exc) {}
		return out;
	}
	private String formatWithParentheses(String v) {
		int index = v.indexOf("+/-");
		if (index >= 0) {
			String f1 = FileIO.getField(1, v, "+/-", true).trim();
			String f2 = FileIO.getField(2, v, "+/-", true).trim();
			String f2a = FileIO.getField(1, f2, " ", true);
			String f2b = FileIO.getRestAfterField(1, f2, " ", true);

			v = f1 + " ("+f2a+") "+f2b;
		}
		return v.trim();
	}

	/** Planck constant in J*s. Value from CODATA 2014. */
	public static final MeasureElement PLANCK_CONSTANT = new MeasureElement(6.626070040E-34, 0.000000081E-34, "J*s");
	/** Boltzmann constant in J/K. Value from CODATA 2014. */
	public static final MeasureElement BOLTZMANN_CONSTANT = new MeasureElement(1.38064852E-23, 0.00000079E-23, "J/K");
	/** Stefan-Boltzmann en W/(m^2 K^4). Value from CODATA 2014. */
	public static final MeasureElement STEFAN_BOLTZMANN_CONSTANT = new MeasureElement(0.00000005670367, 0.00000000000013, "W/m2/K4)");
	/** Electron charge in C (J/eV). Value from CODATA 2014. */
	public static final MeasureElement ELECTRON_CHARGE = new MeasureElement(1.6021766208E-19, 0.0000000098E-19, "C");
	/** Gravitational constant in m^3/(kg s^2). Value from CODATA 2014. */
	public static final MeasureElement GRAVITATIONAL_CONSTANT = new MeasureElement(6.67408E-11, 0.00031E-11, "m3/kg/s2)");
	/** Avogadro number in mol^-1. CODATA 2014. */
	public static final MeasureElement AVOGADRO_CONSTANT = new MeasureElement(6.022140857E+23, 0.000000074E+23, "/mol");
	/** Gas constant in J / (mol K). Value from CODATA 2014. */
	public static final MeasureElement GAS_CONSTANT = new MeasureElement(8.3144598, 0.0000048, "J/mol/K");
	/** Electron mass in Kg. Value from CODATA 2014. */
	public static final MeasureElement ELECTRON_MASS = new MeasureElement(9.10938356E-31, 0.00000011E-31, "Kg");
	/** Proton mass in Kg. Value from CODATA 2014. */
	public static final MeasureElement PROTON_MASS = new MeasureElement(1.672621898E-27, 0.000000021E-27, "Kg");
	/** Fine structure constant. Value from CODATA 2014. */
	public static final MeasureElement FINE_STRUCTURE_CONSTANT = new MeasureElement(0.0072973525664, 0.0000000000017, "");
	/** Atomic unit mass, or 1/12 of the mass of the C-12 isotope, in Kg. Value from CODATA 2014. */
	public static final MeasureElement AMU = new MeasureElement(1.660539040E-27, 0.000000020E-27, "Kg");
	/** Rydberg constant in m^-1. Value from CODATA 2014. */
	public static final MeasureElement RYDBERG_CONSTANT = new MeasureElement(10973731.568508, 0.000065, "/m");
	/** Wien constant in m K. Value from CODATA 2014. */
	public static final MeasureElement WIEN_CONSTANT = new MeasureElement(0.0028977729, 0.0000000017, "m*K");
	/** molar volume of an ideal gas in m^3/mol (100 kPa). Value from CODATA 2014 */
	public static final MeasureElement VOLUME_OF_1MOL_IDEAL_GAS = new MeasureElement(0.022710947, 0.000000013, "m3/mol");
	/** Bohr's magneton in MeV/T. Value from CODATA 2014 */
	public static final MeasureElement BOHR_MAGNETON = new MeasureElement(5.7883818012E-11, 0.0000000026E-11, "MeV/T");
	/** Bohr radius in m. Value from CODATA 2014. */
	public static final MeasureElement BOHR_RADIUS = new MeasureElement(5.2917721067E-11, 0.0000000012E-11, "m");
	/** Classical radius of the electron in m. Value from CODATA 2014. */
	public static final MeasureElement ELECTRON_RADIUS = new MeasureElement(2.8179403227E-15, 0.0000000019E-15, "m");

	/**
	 * Returns an array with the values of a set of measures.
	 * @param v The set of measures.
	 * @return The set of values.
	 * @throws JPARSECException If an error occurs retrieving the
	 * value of any of the values in the input array.
	 */
	public static double[] getValues(MeasureElement v[]) throws JPARSECException {
		double out[] = new double[v.length];
		for (int i=0; i<v.length; i++) {
			out[i] = v[i].getValue();
		}
		return out;
	}
	/**
	 * Returns an array with the errors of a set of measures.
	 * @param v The set of measures.
	 * @return The set of errors.
	 */
	public static double[] getErrorValues(MeasureElement v[]) {
		double out[] = new double[v.length];
		for (int i=0; i<v.length; i++) {
			out[i] = v[i].error;
		}
		return out;
	}

	/**
	 * Converts a set of measures to a string array.
	 * @param v The set of measures
	 * @param useParentheses True to use parenthesis instead of +/-.
	 * @param includeUnit True to include the unit in the output.
	 * @return The string array.
	 */
	public static String[] toString(MeasureElement v[], boolean useParentheses, boolean includeUnit) {
		String out[] = new String[v.length];
		for (int i=0; i<v.length; i++) {
			out[i] = v[i].toString(useParentheses, includeUnit);
		}
		return out;
	}
}
