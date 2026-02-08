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

import jparsec.observer.CityElement;

/**
 * Implements the Observational Islamic calendar. See Calendrical Calculations
 * for reference.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class IslamicObservational extends Islamic
{
	private static final long serialVersionUID = 182875218135355239L;
	
	/**
	 * Cairo location.
	 */
	public static final CityElement CAIRO = new CityElement("Cairo, Egypt", 31.3, 30.1, 2, 200);

	/**
	 * Islamic location, currently set to Cairo.
	 */
	public static final CityElement ISLAMIC_LOCALE = CAIRO;

	private transient long prevLunarPhase;

	private transient long months;

	/**
	 * Fixed day constructor.
	 *
	 * @param fixed fixed day.
	 */
	public IslamicObservational(final long fixed)
	{
		super(fixed);
	}

	/**
	 * Julian day constructor.
	 *
	 * @param julianDay Julian day.
	 */
	public IslamicObservational(final double julianDay)
	{
		super(julianDay);
	}

	/**
	 * Explicit constructor.
	 *
	 * @param year Year.
	 * @param month Month.
	 * @param day Day.
	 */
	public IslamicObservational(final long year, final int month, final int day)
	{
		super(year, month, day);
	}

	/**
	 * To fixed day.
	 *
	 * @param year Year.
	 * @param month Month.
	 * @param day Day.
	 * @return Fixed day.
	 */
	@Override
	long toFixed(final long year, final int month, final int day) {
		try {
			long l2 = Islamic.EPOCH + (long) Math.floor(((year - 1) * 12 + month - 0.5) * Calendar.LUNAR_CYCLE);
			return Calendar.phasisOnOrBefore(l2, ISLAMIC_LOCALE) + day - 1;
		} catch (Exception ex) {
			return 0;
		}
	}

	@Override
	long yearFromFixed() {
		try {
			prevLunarPhase = Calendar.phasisOnOrBefore(this.fixed, ISLAMIC_LOCALE);
		}
		catch (Exception ignored) {
			return 0;
		}

		this.months = Math.round((double) (prevLunarPhase - Islamic.EPOCH) / Calendar.LUNAR_CYCLE);

		return 1 + (this.months / 12);
	}

	@Override
	int monthFromFixed(long year) {
		return 1 + (int) (this.months % 12);
	}

	@Override
	int dayFromFixed(long year, int month) {
		return 1 + (int) (this.fixed - prevLunarPhase);
	}
}
