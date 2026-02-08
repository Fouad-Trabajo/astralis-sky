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

/**
 * A class to hold physical constants in C.G.S. units. Values are identical to the
 * corresponding SI constants defined in {@linkplain Constant} class, except for
 * the corresponding unit change.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class CGSConstant
{
	// private constructor so that this class cannot be instantiated.
	private CGSConstant() {}

	/** Light speed in cm/s. */
	public static final double SPEED_OF_LIGHT = Constant.SPEED_OF_LIGHT * 100.0;

	/** Mass of the hydrogen molecule in g. */
	public static final double H2_MASS = Constant.H2_MASS * 1000.0;

	/** AU to cm transform coefficient. */
	public static final double AU = Constant.AU * 1.0E5;

	/** pc to cm transform coefficient. */
	public static final double PARSEC = Constant.PARSEC * 100.0;

	/** Mass of the sun in g. */
	public static final double SUN_MASS = Constant.SUN_MASS * 1000.0;

	/** Boltzmann constant in erg/K.	  */
	public static final double BOLTZMANN_CONSTANT = Constant.BOLTZMANN_CONSTANT / Constant.ERGIO_TO_JULE;

	/** Planck constant in erg*s. */
	public static final double PLANCK_CONSTANT = Constant.PLANCK_CONSTANT / Constant.ERGIO_TO_JULE;

	/** Atomic unit mass, or 1/12 of the mass of the C-12 isotope, in g. */
	public static final double AMU = Constant.AMU * 1000.0;
}
