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
 * Implements the Balinese calendar. See Calendrical Calculations for reference.
 * <p>
 * Note that it is not possible to pass from a given date to a Julian day,
 * since no year exists in this calendar.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Balinese implements Serializable
{
	/**
	 * Epoch = -1721279 fixed.
	 */
	public static final long EPOCH = Calendar.fixedFromJD(146D);

	private static final int PANCAWARA_I[] = { 5, 9, 7, 4, 8 };

	private static final int SAPTAWARA_J[] = { 5, 4, 3, 7, 8, 6, 9 };

	/**
	 * Dwiwara names.
	 */
	public static final String DWIWARA_NAMES[] = { "Menga", "Pepet" };

	/**
	 * Triwara names.
	 */
	public static final String TRIWARA_NAMES[] = { "Pasah", "Beteng", "Kajeng" };

	/**
	 * Caturwara names.
	 */
	public static final String CATURWARA_NAMES[] = { "Sri", "Laba", "Jaya", "Menala" };

	/**
	 * Pancawara names.
	 */
	public static final String PANCAWARA_NAMES[] = { "Umanis", "Paing", "Pon", "Wage", "Keliwon" };

	/**
	 * Sadwara names.
	 */
	public static final String SADWARA_NAMES[] = { "Tungleh", "Aryang", "Urukung", "Paniron", "Was", "Maulu" };

	/**
	 * Saptawara names.
	 */
	public static final String SAPTAWARA_NAMES[] = { "Redite", "Coma", "Anggara", "Buda", "Wraspati", "Sukra", "Saniscara" };

	/**
	 * Asatawara names.
	 */
	public static final String ASATAWARA_NAMES[] = { "Sri", "Indra", "Guru", "Yama", "Ludra", "Brahma", "Kala", "Uma" };

	/**
	 * Sangawara names.
	 */
	public static final String SANGAWARA_NAMES[] = { "Dangu", "Jangur", "Gigis", "Nohan", "Ogan", "Erangan", "Urungan", "Tulus", "Dadi" };

	/**
	 * Dasawara names.
	 */
	public static final String DASAWARA_NAMES[] = { "Pandita", "Pati", "Suka", "Duka", "Sri", "Manuh", "Manusa", "Raja", "Dewa", "Raksasa" };

	/**
	 * Week names.
	 */
	public static final String WEEK_NAMES[] = {
		"Sinta", "Landep", "Ukir", "Kulantir", "Taulu", "Gumbreg", "Wariga", "Warigadian", "Jukungwangi", "Sungsang",
		"Dunggulan", "Kuningan", "Langkir", "Medangsia", "Pujut", "Pahang", "Krulut", "Merakih", "Tambir",
		"Medangkungan", "Matal", "Uye", "Menail", "Parangbakat", "Bala", "Ugu", "Wayang", "Kelawu", "Dukut",
		"Watugunung"
	};

	private static final long serialVersionUID = -4264579975033439901L;

	/**
	 * Luang flag.
	 */
	public final boolean luang;

	/**
	 * Dwiwara.
	 */
	public final int dwiwara;

	/**
	 * Triwara.
	 */
	public final int triwara;

	/**
	 * Caturwara.
	 */
	public final int caturwara;

	/**
	 * Pancawara.
	 */
	public final int pancawara;

	/**
	 * Sadwara.
	 */
	public final int sadwara;

	/**
	 * Saptawara.
	 */
	public final int saptawara;

	/**
	 * Asatawara.
	 */
	public final int asatawara;

	/**
	 * Sangawara.
	 */
	public final int sangawara;

	/**
	 * Dasawara.
	 */
	public final int dasawara;

	/**
	 * Gets the Balinese calendar fields from the fixed day.
	 *
	 * @param fixed Fixed day.
	 */
	public Balinese (final long fixed)
	{
		this.luang = luangFromFixed(fixed);
		this.dwiwara = dwiwaraFromFixed(fixed);
		this.triwara = triwaraFromFixed(fixed);
		this.caturwara = caturwaraFromFixed(fixed);
		this.pancawara = pancawaraFromFixed(fixed);
		this.sadwara = sadwaraFromFixed(fixed);
		this.saptawara = saptawaraFromFixed(fixed);
		this.asatawara = asatawaraFromFixed(fixed);
		this.sangawara = sangawaraFromFixed(fixed);
		this.dasawara = dasawaraFromFixed(fixed);
	}

	/**
	 * Constructor from a Julian day.
	 *
	 * @param julianDay Julian day.
	 */
	public Balinese(final double julianDay)
	{
		this((long) Math.floor(julianDay - 0.5) - Gregorian.EPOCH);
	}

	/**
	 * Constructor using all fields.
	 *
	 * @param luang Luang.
	 * @param dwiwara Dwiwara.
	 * @param triwara Triwara.
	 * @param caturwara Caturwara.
	 * @param pancawara Pancawara.
	 * @param sadwara Sadwara.
	 * @param saptawara Saptawara.
	 * @param asatawara Asatawara.
	 * @param sangawara Sangawara.
	 * @param dasawara Dasawara.
	 */
	public Balinese(final boolean luang, final int dwiwara, final int triwara, final int caturwara, final int pancawara,
		final int sadwara, final int saptawara, final int asatawara, final int sangawara, final int dasawara)
	{
		this.luang = luang;
		this.dwiwara = dwiwara;
		this.triwara = triwara;
		this.caturwara = caturwara;
		this.pancawara = pancawara;
		this.sadwara = sadwara;
		this.saptawara = saptawara;
		this.asatawara = asatawara;
		this.sangawara = sangawara;
		this.dasawara = dasawara;
	}

	/**
	 * Gets the day from the fixed date.
	 *
	 * @param fixed Fixed date.
	 * @return Day.
	 */
	public static int dayFromFixed(final long fixed)
	{
		return (int) Calendar.mod(fixed - EPOCH, 210);
	}

	/**
	 * Gets the luang from the fixed date.
	 *
	 * @param fixed Fixed date.
	 * @return Luang.
	 */
	public static boolean luangFromFixed(final long fixed)
	{
		return Calendar.mod(dasawaraFromFixed(fixed), 2) == 0;
	}

	/**
	 * Gets the dwiwara from the fixed date.
	 *
	 * @param fixed Fixed date.
	 * @return Dwiwara.
	 */
	public static int dwiwaraFromFixed(final long fixed)
	{
		return Calendar.mod(dasawaraFromFixed(fixed) + 1, 2) + 1;
	}

	/**
	 * Gets the triwara from the fixed date.
	 *
	 * @param fixed Fixed date.
	 * @return Triwara.
	 */
	public static int triwaraFromFixed(final long fixed)
	{
		return Calendar.mod(dayFromFixed(fixed), 3) + 1;
	}

	/**
	 * Gets the caturwara from the fixed date.
	 *
	 * @param fixed Fixed date.
	 * @return Caturwara.
	 */
	public static int caturwaraFromFixed(final long fixed)
	{
		return Calendar.adjustedMod(asatawaraFromFixed(fixed), 4);
	}

	/**
	 * Gets the pancawara from the fixed date.
	 *
	 * @param fixed Fixed date.
	 * @return Pancawara.
	 */
	public static int pancawaraFromFixed(final long fixed)
	{
		return Calendar.mod(dayFromFixed(fixed) + 1, 5) + 1;
	}

	/**
	 * Gets the sadwara from the fixed date.
	 *
	 * @param fixed Fixed date.
	 * @return Sadwara.
	 */
	public static int sadwaraFromFixed(final long fixed)
	{
		return Calendar.mod(dayFromFixed(fixed), 6) + 1;
	}

	/**
	 * Gets the saptawara from the fixed date.
	 *
	 * @param fixed Fixed date.
	 * @return Saptawara.
	 */
	public static int saptawaraFromFixed(final long fixed)
	{
		return Calendar.mod(dayFromFixed(fixed), 7) + 1;
	}

	/**
	 * Gets the asatawara from the fixed date.
	 *
	 * @param fixed Fixed date.
	 * @return Asatawara.
	 */
	public static int asatawaraFromFixed(final long fixed)
	{
		int i = dayFromFixed(fixed);
		return Calendar.mod(Math.max(6, 4 + Calendar.mod(i - 70, 210)), 8) + 1;
	}

	/**
	 * Gets the sangawara from the fixed date.
	 *
	 * @param fixed Fixed date.
	 * @return Sangawara.
	 */
	public static int sangawaraFromFixed(final long fixed)
	{
		return Calendar.mod(Math.max(0, dayFromFixed(fixed) - 3), 9) + 1;
	}

	/**
	 * Gets the dasawara from the fixed date.
	 *
	 * @param fixed Fixed date.
	 * @return Dasawara.
	 */
	public static int dasawaraFromFixed(final long fixed)
	{
		int i = pancawaraFromFixed(fixed);
		int j = saptawaraFromFixed(fixed);
		return (PANCAWARA_I[i - 1] + SAPTAWARA_J[j - 1] + 1) % 10;
	}

	/**
	 * Gets the week from the fixed date.
	 *
	 * @param fixed Fixed date.
	 * @return Week.
	 */
	public static int weekFromFixed(final long fixed)
	{
		return 1 + dayFromFixed(fixed) / 7;
	}

	/**
	 * Gets the interval between two dates.
	 *
	 * @param balinese Balinese instance.
	 * @param fixed Fixed date.
	 * @return Interval in days.
	 */
	public static long onOrBefore(final Balinese balinese, final long fixed)
	{
		int i = balinese.pancawara - 1;
		int j = balinese.sadwara - 1;
		int k = balinese.saptawara - 1;
		int i1 = (i + 14 + 15 * (k - i)) % 35;
		int j1 = j + 36 * (i1 - j);
		int k1 = dayFromFixed(0);

		return fixed - ((fixed + k1 - j1) % 210);
	}

	/**
	 * Gets the day from a Balinese date.
	 *
	 * @return Day.
	 */
	public int day()
	{
		return (int) ((onOrBefore(this, EPOCH + 209) - EPOCH) + 1);
	}

	/**
	 * Gets the week from a Balinese date.
	 *
	 * @return Week.
	 */
	public int week()
	{
		return 1 + (day() - 1) / 7;
	}

	@Override
	public String toString() {
		return "Balinese {" +
			" luang=" + luang +
			", asatawara=" + asatawara +
			", dwiwara=" + dwiwara +
			", triwara=" + triwara +
			", caturwara=" + caturwara +
			", pancawara=" + pancawara +
			", sadwara=" + sadwara +
			", saptawara=" + saptawara +
			", sangawara=" + sangawara +
			", dasawara=" + dasawara +
		" }";
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;

		if (!(o instanceof Balinese)) return false;

		Balinese balinese = (Balinese) o;

		if (luang != balinese.luang) return false;
		if (dwiwara != balinese.dwiwara) return false;
		if (triwara != balinese.triwara) return false;
		if (caturwara != balinese.caturwara) return false;
		if (pancawara != balinese.pancawara) return false;
		if (sadwara != balinese.sadwara) return false;
		if (saptawara != balinese.saptawara) return false;
		if (asatawara != balinese.asatawara) return false;
		if (sangawara != balinese.sangawara) return false;

		return dasawara == balinese.dasawara;
	}

	@Override
	public int hashCode() {
		int result = (luang ? 1 : 0);
		result = 31 * result + dwiwara;
		result = 31 * result + triwara;
		result = 31 * result + caturwara;
		result = 31 * result + pancawara;
		result = 31 * result + sadwara;
		result = 31 * result + saptawara;
		result = 31 * result + asatawara;
		result = 31 * result + sangawara;
		result = 31 * result + dasawara;

		return result;
	}
}
