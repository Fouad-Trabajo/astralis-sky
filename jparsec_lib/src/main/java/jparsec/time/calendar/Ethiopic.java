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
package jparsec.time.calendar;

/**
 * Implements the Ethiopic calendar. See Calendrical Calculations for reference.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Ethiopic extends Coptic
{
	private static final long serialVersionUID = 4298274021293834199L;
	
	/**
	 * Calendar epoch.
	 */
	public static final long EPOCH = new Julian(8, 8, 29).fixed;

	/**
	 * Day of week names.
	 */
	public static final String DAY_OF_WEEK_NAMES[] = { "Ihud", "Sanyo", "Maksanyo", "Rob", "Hamus", "Arb", "Kidamme" };

	/**
	 * Month names.
	 */
	public static final String MONTH_NAMES[] = {
		"Maskaram", "Teqemt", "Kehdar", "Takhsas", "Ter", "Yakatit", "Magabit",
		"Miyazya", "Genbot", "Sane", "Hamle", "Nahase", "Paguemen"
	};

	/**
	 * Fixed day constructor.
	 *
	 * @param fixed Julian day.
	 */
	public Ethiopic(final long fixed) {
		super(EPOCH, fixed);
	}

	/**
	 * Julian day constructor.
	 *
	 * @param julianDay Julian day.
	 */
	public Ethiopic(final double julianDay) {
		super(EPOCH, julianDay);
	}

	/**
	 * Explicit constructor.
	 *
	 * @param year Year.
	 * @param month Month.
	 * @param day Day.
	 */
	public Ethiopic(final long year, final int month, final int day) {
		super(EPOCH, year, month, day);
	}
}
