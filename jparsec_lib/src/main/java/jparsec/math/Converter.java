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

import java.util.Enumeration;

import cds.astro.*;
import jparsec.util.*;

/**
 * A class to perform unit conversions.<P>
 * A simple example is:<BR>
 * <pre>
 * try {
 * 		Converter c = new Converter("pc", "AU");
 * 		double val = c.convert(10);
 * 		System.out.println(val);
 *		System.out.println("Correct value: " + (10 * Constant.PARSEC / (1000.0 * Constant.AU)));
 * } catch (JPARSECException e)
 * {
 * 		e.showException();
 * }
 * </pre>
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Converter {

	/**
	 * Origin unit, for example AU or km/s.
	 */
	public String originUnit;
	/**
	 * Target unit, for example km or c (speed of light).
	 */
	public String targetUnit;

	/**
	 * Constructor.
	 * @param origin Origin unit.
	 * @param target Target unit.
	 * @throws JPARSECException If the conversion is unsupported.
	 */
	public Converter(String origin, String target)
	throws JPARSECException {
		this.originUnit = origin;
		this.targetUnit = target;

		if (!this.isConversionSupported())
			throw new JPARSECException("unsupported conversion.");
	}

	/**
	 * Returns all supported symbols.
	 * @return Symbols.
	 */
	public static String[] getAllSymbols()
	{
		String symbols = "";
		Enumeration<String> list = Unit.symbols();
		while (list.hasMoreElements()) {
			String symbol = list.nextElement();
			symbols += symbol;
			symbols += jparsec.io.FileIO.getLineSeparator();
		}
		return jparsec.graph.DataSet.toStringArray(symbols, jparsec.io.FileIO.getLineSeparator());
	}

	/**
	 * Explains the meaning of a unit.
	 * @param unit Any unit.
	 * @return Explanation.
	 */
	public static String explainUnit(String unit)
	{
		return Unit.explainUnit(unit);
	}

	/**
	 * Returns all symbols and their explanations.
	 * @return Symbols plus explanations.
	 */
	public static String[] getAllSymbolsWithExplanations()
	{
		String symbols = "";
		Enumeration<String> list = Unit.symbols();
		while (list.hasMoreElements()) {
			String symbol = list.nextElement();
			symbols += symbol;
			symbols += "   ";
			symbols += explainUnit(symbol);
			symbols += jparsec.io.FileIO.getLineSeparator();
		}
		return jparsec.graph.DataSet.toStringArray(symbols, jparsec.io.FileIO.getLineSeparator());
	}

	/**
	 * Check if a given symbol is supported.
	 * @param symbol A primitive unit.
	 * @return True if it is supported.
	 */
	public static boolean isSymbolSupported(String symbol)
	{
		String array[] = Converter.getAllSymbols();
		boolean isSupported = false;
		for (int i=0; i<array.length; i++)
		{
			if (array[i].equals(symbol)) {
				isSupported = true;
				break;
			}
		}
		return isSupported;
	}

	/**
	 * Checks if a given conversion is supported.
	 * @return True if it is supported.
	 */
	public boolean isConversionSupported()
	{
		try {
			Unit u1 = new Unit(this.originUnit);
			Unit u2 = new Unit(this.targetUnit);

			// If no error occurs instantiating, then it is supported
			return true;
		} catch (Exception e)
		{
			return false;
		}
	}

	/**
	 * Returns origin unit as a unit instance.
	 * @return Instance.
	 * @throws JPARSECException If unit cannot bet parsed.
	 */
	public Unit getOriginUnit()
	throws JPARSECException {
		try {
			Unit u = new Unit(this.originUnit);
			return u;
		} catch (Exception e)
		{
			throw new JPARSECException("cannot understand origin unit.", e);
		}
	}

	/**
	 * Returns target unit as a unit instance.
	 * @return Instance.
	 * @throws JPARSECException If unit cannot bet parsed.
	 */
	public Unit getTargetUnit()
	throws JPARSECException {
		try {
			Unit u = new Unit(this.targetUnit);
			return u;
		} catch (Exception e)
		{
			throw new JPARSECException("cannot understand target unit.", e);
		}
	}

	/**
	 * Returns unit as a unit instance.
	 * @param unit Unit.
	 * @return Instance.
	 * @throws JPARSECException If unit cannot bet parsed.
	 */
	public static Unit getUnit(String unit)
	throws JPARSECException {
		try {
			Unit u = new Unit(unit);
			return u;
		} catch (Exception e)
		{
			throw new JPARSECException("cannot understand target unit.", e);
		}
	}

	/**
	 * Converts a value in a origin unit into the units of the target unit.
	 * @param value Value.
	 * @return Conversion.
	 * @throws JPARSECException If an error occurs.
	 */
	public double convert(double value)
	throws JPARSECException {
		try {
			String val = ""+value+this.originUnit;
			Unit u = new Unit(val);
			Unit target = new Unit(this.targetUnit);
			u.convertTo(target);
			return u.getValue();
		} catch (Exception e)
		{
			throw new JPARSECException("error during conversion, probably units are not compatible.", e);
		}
	}
}
