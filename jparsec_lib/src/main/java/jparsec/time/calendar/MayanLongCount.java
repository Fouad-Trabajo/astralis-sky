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

import java.io.Serializable;

/**
 * Implements the Mayan Long Count calendar.
 * <P>
 * A strictly counting days calendar, based on a cycle of 2 880 000 days.
 *
 * See Calendrical Calculations for reference.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class MayanLongCount implements Serializable
{
	private static final long serialVersionUID = 2050528164648687620L;
	
	/**
	 * Calendar epoch.
	 */
	public static final long EPOCH = -1137142; // Calendar.fixedFromJD(584283D);

	/**
	 * 144 000 days.
	 */
	public long baktun;

	/**
	 * 7200 days.
	 */
	public int katun;

	/**
	 * 360 days.
	 */
	public int tun;

	/**
	 * 20 days.
	 */
	public int uinal;

	/**
	 * 1 day.
	 */
	public int kin;

	/**
	 * Default constructor.
	 */
	public MayanLongCount() { }

	/**
	 * Julian day constructor.
	 *
	 * @param julianDay Julian day.
	 */
	public MayanLongCount(final double julianDay)
	{
		fromJulianDay(julianDay);
	}

	/**
	 * Explicit constructor.
	 *
	 * @param baktun Baktun.
	 * @param katun Katun.
	 * @param tun Tun.
	 * @param uinal Uinal.
	 * @param kin Kin.
	 */
	public MayanLongCount(final long baktun, final int katun, final int tun, final int uinal, final int kin)
	{
		this.baktun = baktun;
		this.katun = katun;
		this.tun = tun;
		this.uinal = uinal;
		this.kin = kin;
	}

	/**
	 * To fixed day.
	 *
	 * @param baktun Baktun.
	 * @param katun Katun.
	 * @param tun Tun.
	 * @param uinal Uinal.
	 * @param kin Kin.
	 * @return Fixed day.
	 */
	public static long toFixed(final long baktun, final int katun, final int tun, final int uinal, final int kin) {
		return EPOCH + baktun * 0x23280 + katun * 7200 + tun * 360 + uinal * 20 + kin;
	}

	/**
	 * To fixed day.
	 *
	 * @return Fixed day.
	 */
	public long toFixed() {
		return toFixed(baktun, katun, tun, uinal, kin);
	}

	/**
	 * Sets the date from fixed epoch.
	 *
	 * @param fixed Fixed day.
	 */
	public void fromFixed(final long fixed) {
		long l1 = fixed - EPOCH;
		baktun = Calendar.quotient(l1, 144000D);
		int i = (int) Calendar.mod(l1, 0x23280L);
		katun = (int) Calendar.quotient(i, 7200D);
		int j = Calendar.mod(i, 7200);
		tun = (int) Calendar.quotient(j, 360D);
		int k = Calendar.mod(j, 360);
		uinal = (int) Calendar.quotient(k, 20D);
		kin = Calendar.mod(k, 20);
	}

	/**
	 * Transforms a Mayan date into a Julian day
	 *
	 * @param baktun Baktun.
	 * @param katun Katun.
	 * @param tun Tun.
	 * @param uinal Uinal.
	 * @param kin Kin.
	 * @return Julian day.
	 */
	public static double toJulianDay(final long baktun, final int katun, final int tun, final int uinal, final int kin) {
		return toFixed(baktun, katun, tun, uinal, kin) + Gregorian.EPOCH;
	}

	/**
	 * Transforms a Mayan date into a Julian day
	 *
	 * @return Julian day.
	 */
	public double toJulianDay() {
		return toFixed() + Gregorian.EPOCH + 0.5;
	}

	/**
	 * Sets a Mayan date with a given Julian day
	 *
	 * @param julianDay Julian day.
	 */
	public void fromJulianDay(final double julianDay) {
		fromFixed((long) Math.floor(julianDay - 0.5) - Gregorian.EPOCH);
	}
}
