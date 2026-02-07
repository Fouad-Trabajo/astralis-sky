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
 * Implements the Armenian calendar. See Calendrical Calculations for reference.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Armenian extends Egyptian
{
	private static final long serialVersionUID = 2470952263348488987L;
	
	/**
	 * Calendar epoch.
	 * Armenian calendar year 1 started on Julian date 552/07/11
	 */
	public final static long EPOCH = new Julian(552, 7, 11).fixed;

	/**
	 * Name of days of the week.
	 */
	public static final String DAY_OF_WEEK_NAMES[] = {
		"Miashabathi", "Erkoushabathi", "Erekhshabathi", "Chorekhshabathi", "Hingshabathi", "Urbath", "Shabath"
	};

	/**
	 * Name of months.
	 */
	public static final String MONTH_NAMES[] = {
		"Nawasardi", "Hori", "Sahmi", "Tre", "Kaloch", "Arach", "Mehekani",
		"Areg", "Ahekani", "Mareri", "Margach", "Hrotich", "Aweleach"
	};

	/**
	 * Constructor using a fixed date.
	 *
	 * @param fromFixed fixed date.
	 */
	public Armenian(final long fromFixed) {
		super(EPOCH, fromFixed);
	}

	/**
	 * Constructor using a Julian day.
	 *
	 * @param fromJulianDate Julian day.
	 */
	public Armenian(final double fromJulianDate) {
		super(EPOCH, fromJulianDate);
	}

	/**
	 * Constructor using year, month, day.
	 *
	 * @param year Year.
	 * @param month Month.
	 * @param day Day.
	 */
	public Armenian(final long year, final int month, final int day) {
		super(EPOCH, year, month, day);
	}
	
    @Override
    long toFixed(final long year, final int month, final int day) {
        return this.epoch + 365 * (year - 1) + 30 * (month - 1) + day - 1;
    }

    @Override
    int dayFromFixed(final long year, final int month) {
        return 1 + (int) (this.fixed - this.epoch - 365 * (year - 1) - 30 * (month - 1));
    }
}
