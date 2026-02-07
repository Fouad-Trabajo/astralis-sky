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

import jparsec.time.AstroDate;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;
import jparsec.util.Translate.LANGUAGE;

/**
 * A class to transform the date between different calendars based on year, month, and day.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class CalendarGenericConversion
{
	// private constructor so that this class cannot be instantiated.
	private CalendarGenericConversion() {}

	// List ordered by calendars in use or old calendars

	/**
	 * The set of available calendars that have in common that
	 * a given date is represented with a year, a month, and a day.
	 * First 6 are old calendars, the next 9 ones are still used today.
	 */
	public enum CALENDAR {
		// Old calendars

		/** ID constant for Julian calendar. */
		JULIAN,
		/** ID constant for Hindu (old solar) calendar. */
		HINDU_OLD_SOLAR,
		/** ID constant for Egyptian calendar. */
		EGYPTIAN,
		/** ID constant for French calendar. */
		FRENCH,
		/** ID constant for French (modified) calendar. */
		FRENCH_MODIFIED,
		/** ID constant for Persian arithmetic calendar. */
		PERSIAN_ARITHMETIC,

		// In use

		/** ID constant for Gregorian calendar. */
		GREGORIAN,
		/** ID constant for Islamic calendar. */
		ISLAMIC,
		/** ID constant for Islamic observational calendar. */
		ISLAMIC_OBSERVATIONAL,
		/** ID constant for Hebrew calendar. */
		HEBREW,
		/** ID constant for Hindu (solar) calendar. */
		HINDU_SOLAR,
		/** ID constant for Persian calendar. */
		PERSIAN,
		/** ID constant for Coptic calendar. */
		COPTIC,
		/** ID constant for Ethiopic calendar. */
		ETHIOPIC,
		/** ID constant for Armenian calendar. */
		ARMENIAN
	}

	/**
	 * Calendar names array.
	 */
	public static final String CALENDAR_NAMES[] = {
		"Julian", "Hindu solar (old)", "Egyptian", "French", "French (modified)", "Persian (old arithmetic)",
		"Gregorian", "Islamic (arithmetic)", "Islamic (observational)", "Hebrew", "Hindu solar",
		"Persian (astronomical)", "Coptic", "Ethiopic", "Armenian"
	};

	/**
	 * Converts a given date from one calendar to another. Any of the 15
	 * different calendars with ID constants defined in this class are available
	 * for input or output.
	 *
	 * @param input_calendar Input calendar.
	 * @param output_calendar Output calendar.
	 * @param adate Date in the input calendar.
	 * @return Date in the output calendar.
	 * @throws JPARSECException If the input/output calendars are invalid.
	 */
	public static AstroDate GenericConversion(CALENDAR input_calendar, CALENDAR output_calendar, AstroDate adate)
			throws JPARSECException
	{
		int out[] = GenericConversion(input_calendar, output_calendar, adate.getAstronomicalYear(), 
				adate.getMonth(), adate.getDay());
		return new AstroDate(out[0], out[1], out[2]);
	}
	
	/**
	 * Converts a given date from one calendar to another. Any of the 15
	 * different calendars with ID constants defined in this class are available
	 * for input or output.
	 *
	 * @param input_calendar Input calendar.
	 * @param output_calendar Output calendar.
	 * @param year Year in input calendar. Year should be set in the astronomical convention, year 0 exists.
	 * @param month Month in input calendar.
	 * @param day Day in input calendar.
	 * @return Array with year, month, and day in output calendar.
	 * @throws JPARSECException If the input/output calendars are invalid.
	 */
	public static int[] GenericConversion(CALENDAR input_calendar, CALENDAR output_calendar, int year, int month, int day)
			throws JPARSECException
	{
		BaseCalendar inputCal;

		switch (input_calendar) {
		case ARMENIAN:
			inputCal = new Armenian(year, month, day);
			break;
		case COPTIC:
			inputCal = new Coptic(year, month, day);
			break;
		case EGYPTIAN:
			inputCal = new Egyptian(year, month, day);
			break;
		case ETHIOPIC:
			inputCal = new Ethiopic(year, month, day);
			break;
		case FRENCH:
			inputCal = new French(year, month, day);
			break;
		case FRENCH_MODIFIED:
			inputCal = new FrenchModified(year, month, day);
			break;
		case GREGORIAN:
			inputCal = new Gregorian(year, month, day);
			break;
		case HEBREW:
			inputCal = new Hebrew(year, month, day);
			break;
		case HINDU_OLD_SOLAR:
			inputCal = new HinduOldSolar(year, month, day);
			break;
		case HINDU_SOLAR:
			inputCal = new HinduSolar(year, month, day);
			break;
		case ISLAMIC:
			inputCal = new Islamic(year, month, day);
			break;
		case ISLAMIC_OBSERVATIONAL:
			inputCal = new IslamicObservational(year, month, day);
			break;
		case JULIAN:
			inputCal = new Julian(year, month, day);
			break;
		case PERSIAN:
			inputCal = new Persian(year, month, day);
			break;
		case PERSIAN_ARITHMETIC:
			inputCal = new PersianArithmetic(year, month, day);
			break;
		default:
			throw new JPARSECException("Invalid calendar: " + input_calendar);
		}

		double julianDay = inputCal.julianDate;

		BaseCalendar outputCal;

		switch (output_calendar) {
		case ARMENIAN:
			outputCal = new Armenian(julianDay);
			break;
		case COPTIC:
			outputCal = new Coptic(julianDay);
			break;
		case EGYPTIAN:
			outputCal = new Egyptian(julianDay);
			break;
		case ETHIOPIC:
			outputCal = new Ethiopic(julianDay);
			break;
		case FRENCH:
			outputCal = new French(julianDay);
			break;
		case FRENCH_MODIFIED:
			outputCal = new FrenchModified(julianDay);
			break;
		case GREGORIAN:
			outputCal = new Gregorian(julianDay);
			break;
		case HEBREW:
			outputCal = new Hebrew(julianDay);
			break;
		case HINDU_OLD_SOLAR:
			outputCal = new HinduOldSolar(julianDay);
			break;
		case HINDU_SOLAR:
			outputCal = new HinduSolar(julianDay);
			break;
		case ISLAMIC:
			outputCal = new Islamic(julianDay);
			break;
		case ISLAMIC_OBSERVATIONAL:
			outputCal = new IslamicObservational(julianDay);
			break;
		case JULIAN:
			outputCal = new Julian(julianDay);
			break;
		case PERSIAN:
			outputCal = new Persian(julianDay);
			break;
		case PERSIAN_ARITHMETIC:
			outputCal = new PersianArithmetic(julianDay);
			break;
		default:
			throw new JPARSECException("Invalid calendar: " + output_calendar);
		}

		return new int[] {
			(int) outputCal.getYear(),
			outputCal.getMonth(),
			outputCal.getDay()
		};
	}

	/**
	 * Returns the name of the month for a given calendar.
	 * @param month The month number, January is 1.
	 * @param calendar The calendar id value.
	 * @return The name of the month.
	 * @throws JPARSECException If the calendar or month is invalid.
	 */
	public static String getMonthName(int month, CALENDAR calendar) throws JPARSECException {
		try {
			String out;

			switch (calendar) {
			case ARMENIAN:
				out = Calendar.nameFromMonth(month, Armenian.MONTH_NAMES);
				break;
			case COPTIC:
				out = Calendar.nameFromMonth(month, Coptic.MONTH_NAMES);
				break;
			case EGYPTIAN:
				out = Calendar.nameFromMonth(month, Egyptian.MONTH_NAMES);
				break;
			case ETHIOPIC:
				out = Calendar.nameFromMonth(month, Ethiopic.MONTH_NAMES);
				break;
			case FRENCH:
				out = Calendar.nameFromMonth(month, French.MONTH_NAMES);
				break;
			case FRENCH_MODIFIED:
				out = Calendar.nameFromMonth(month, French.MONTH_NAMES);
				break;
			case GREGORIAN:
				return AstroDate.getMonthName(month);
				//out = Calendar.nameFromMonth(month, Gregorian.MONTH_NAMES);
			case HEBREW:
				out = Calendar.nameFromMonth(month, Hebrew.MONTH_NAMES);
				break;
			case HINDU_OLD_SOLAR:
				out = Calendar.nameFromMonth(month, HinduOldSolar.MONTH_NAMES);
				break;
			case HINDU_SOLAR:
				out = Calendar.nameFromMonth(Calendar.adjustedMod(month + 1, 12), HinduOldLunar.MONTH_NAMES);
				break;
			case ISLAMIC:
				out = Calendar.nameFromMonth(month, Islamic.MONTH_NAMES);
				break;
			case ISLAMIC_OBSERVATIONAL:
				out = Calendar.nameFromMonth(month, Islamic.MONTH_NAMES);
				break;
			case JULIAN:
				return AstroDate.getMonthName(month);
				//out = Calendar.nameFromMonth(month, Gregorian.MONTH_NAMES);
			case PERSIAN:
				out = Calendar.nameFromMonth(month, Persian.MONTH_NAMES);
				break;
			case PERSIAN_ARITHMETIC:
				out = Calendar.nameFromMonth(month, Persian.MONTH_NAMES);
				break;
			default:
				throw new JPARSECException("Invalid calendar: " + calendar);
			}
			out = Translate.translate(out);
			if (out.toLowerCase().equals("may") && Translate.getDefaultLanguage() == LANGUAGE.SPANISH) out += "o";
			return out;
		} catch (JPARSECException e) {
			throw e;
		} catch (Exception exc) {
			throw new JPARSECException("In valid month " + month + " in calendar " + calendar);
		}
	}

	/**
	 * Returns the index of the month for a given calendar.
	 * @param month The month name.
	 * @param calendar The calendar id value.
	 * @return The index of the month, or -1 if no match is found.
	 * @throws JPARSECException If the calendar or month is invalid.
	 */
	public static int getMonthNumber(String month, CALENDAR calendar) throws JPARSECException {
		try {
			int out;

			switch (calendar) {
			case ARMENIAN:
				out = Calendar.indexFromName(month, Armenian.MONTH_NAMES);
				break;
			case COPTIC:
				out = Calendar.indexFromName(month, Coptic.MONTH_NAMES);
				break;
			case EGYPTIAN:
				out = Calendar.indexFromName(month, Egyptian.MONTH_NAMES);
				break;
			case ETHIOPIC:
				out = Calendar.indexFromName(month, Ethiopic.MONTH_NAMES);
				break;
			case FRENCH:
				out = Calendar.indexFromName(month, French.MONTH_NAMES);
				break;
			case FRENCH_MODIFIED:
				out = Calendar.indexFromName(month, French.MONTH_NAMES);
				break;
			case GREGORIAN:
				out = Calendar.indexFromName(month, Gregorian.MONTH_NAMES);
				break;
			case HEBREW:
				out = Calendar.indexFromName(month, Hebrew.MONTH_NAMES);
				break;
			case HINDU_OLD_SOLAR:
				out = Calendar.indexFromName(month, HinduOldSolar.MONTH_NAMES);
				break;
			case HINDU_SOLAR:
				out = Calendar.indexFromName(month, HinduOldLunar.MONTH_NAMES);
				break;
			case ISLAMIC:
				out = Calendar.indexFromName(month, Islamic.MONTH_NAMES);
				break;
			case ISLAMIC_OBSERVATIONAL:
				out = Calendar.indexFromName(month, Islamic.MONTH_NAMES);
				break;
			case JULIAN:
				out = Calendar.indexFromName(month, Gregorian.MONTH_NAMES);
				break;
			case PERSIAN:
				out = Calendar.indexFromName(month, Persian.MONTH_NAMES);
				break;
			case PERSIAN_ARITHMETIC:
				out = Calendar.indexFromName(month, Persian.MONTH_NAMES);
				break;
			default:
				throw new JPARSECException("Invalid calendar: " + calendar);
			}
			return out;
		} catch (JPARSECException e) {
			throw e;
		} catch (Exception exc) {
			throw new JPARSECException("Invalid month " + month + " in calendar " + calendar);
		}
	}

	/**
	 * Returns the name of the day of the week for a given calendar.
	 * @param jd0 The Julian day.
	 * @param calendar The calendar id value.
	 * @return The name of the day.
	 * @throws JPARSECException If the calendar or day is invalid.
	 */
	public static String getDayOfWeekName(double jd0, CALENDAR calendar) throws JPARSECException {
		String out;
		double jd = Math.floor(jd0 - 0.5) + 0.5;
		long day = (long) jd - Gregorian.EPOCH;
		try {
			switch (calendar) {
			case ARMENIAN:
				out = Calendar.nameFromDayOfWeek(Calendar.dayOfWeekFromFixed(day), Armenian.DAY_OF_WEEK_NAMES);
				break;
			case COPTIC:
				out = Calendar.nameFromDayOfWeek(Calendar.dayOfWeekFromFixed(day), Coptic.DAY_OF_WEEK_NAMES);
				break;
			case ETHIOPIC:
				out = Calendar.nameFromDayOfWeek(Calendar.dayOfWeekFromFixed(day), Ethiopic.DAY_OF_WEEK_NAMES);
				break;
			case FRENCH:
				out = Calendar.nameFromNumber(new French(jd).getDayOfWeek(), French.DAY_OF_WEEK_NAMES);
				break;
			case FRENCH_MODIFIED:
				out = Calendar.nameFromNumber(new FrenchModified(jd).getDayOfWeek(), French.DAY_OF_WEEK_NAMES);
				break;
			case GREGORIAN:
				return AstroDate.getDayOfWeekName(jd);
				//out = Calendar.nameFromDayOfWeek(Calendar.dayOfWeekFromFixed(day), Gregorian.DAY_OF_WEEK_NAMES);
			case HEBREW:
				out = Calendar.nameFromDayOfWeek(Calendar.dayOfWeekFromFixed(day), Hebrew.DAY_OF_WEEK_NAMES);
				break;
			case HINDU_OLD_SOLAR:
				out = Calendar.nameFromDayOfWeek(Calendar.dayOfWeekFromFixed(day), HinduOldSolar.DAY_OF_WEEK_NAMES);
				break;
			case HINDU_SOLAR:
				out = Calendar.nameFromDayOfWeek(Calendar.dayOfWeekFromFixed(day), HinduOldSolar.DAY_OF_WEEK_NAMES);
				break;
			case ISLAMIC:
				out = Calendar.nameFromDayOfWeek(Calendar.dayOfWeekFromFixed(day), Islamic.DAY_OF_WEEK_NAMES);
				break;
			case ISLAMIC_OBSERVATIONAL:
				out = Calendar.nameFromDayOfWeek(Calendar.dayOfWeekFromFixed(day), Islamic.DAY_OF_WEEK_NAMES);
				break;
			case JULIAN:
				return AstroDate.getDayOfWeekName(jd);
				//out = Calendar.nameFromDayOfWeek(Calendar.dayOfWeekFromFixed(day), Gregorian.DAY_OF_WEEK_NAMES);
			case PERSIAN:
				out = Calendar.nameFromDayOfWeek(Calendar.dayOfWeekFromFixed(day), Persian.DAY_OF_WEEK_NAMES);
				break;
			case PERSIAN_ARITHMETIC:
				out =  Calendar.nameFromDayOfWeek(Calendar.dayOfWeekFromFixed(day), Persian.DAY_OF_WEEK_NAMES);
				break;
			default:
				throw new JPARSECException("Invalid calendar: " + calendar);
			}

			out = Translate.translate(out);
			return out;
		} catch (JPARSECException e) {
			throw e;
		} catch (Exception exc) {
			throw new JPARSECException("Invalid month for julian day " + jd);
		}
	}

	/**
	 * Returns the index of the name of the day of the week for a given calendar.
	 * @param day Day of week name.
	 * @param calendar The calendar id value.
	 * @return The index of the name of the day in the week, or -1 if no match is
	 * found.
	 * @throws JPARSECException If the calendar or day is invalid.
	 */
	public static int getDayOfWeekNumber(String day, CALENDAR calendar) throws JPARSECException {
		int out;
		try {
			switch (calendar) {
			case ARMENIAN:
				out = Calendar.indexFromName(day, Armenian.DAY_OF_WEEK_NAMES);
				break;
			case COPTIC:
				out = Calendar.indexFromName(day, Coptic.DAY_OF_WEEK_NAMES);
				break;
			case ETHIOPIC:
				out = Calendar.indexFromName(day, Ethiopic.DAY_OF_WEEK_NAMES);
				break;
			case FRENCH:
				out = Calendar.indexFromName(day, French.DAY_OF_WEEK_NAMES);
				break;
			case FRENCH_MODIFIED:
				out = Calendar.indexFromName(day, French.DAY_OF_WEEK_NAMES);
				break;
			case GREGORIAN:
				out = Calendar.indexFromName(day, Gregorian.DAY_OF_WEEK_NAMES);
				break;
			case HEBREW:
				out = Calendar.indexFromName(day, Hebrew.DAY_OF_WEEK_NAMES);
				break;
			case HINDU_OLD_SOLAR:
				out = Calendar.indexFromName(day, HinduOldSolar.DAY_OF_WEEK_NAMES);
				break;
			case HINDU_SOLAR:
				out = Calendar.indexFromName(day, HinduOldSolar.DAY_OF_WEEK_NAMES);
				break;
			case ISLAMIC:
				out = Calendar.indexFromName(day, Islamic.DAY_OF_WEEK_NAMES);
				break;
			case ISLAMIC_OBSERVATIONAL:
				out = Calendar.indexFromName(day, Islamic.DAY_OF_WEEK_NAMES);
				break;
			case JULIAN:
				out = Calendar.indexFromName(day, Gregorian.DAY_OF_WEEK_NAMES);
				break;
			case PERSIAN:
				out = Calendar.indexFromName(day, Persian.DAY_OF_WEEK_NAMES);
				break;
			case PERSIAN_ARITHMETIC:
				out =  Calendar.indexFromName(day, Persian.DAY_OF_WEEK_NAMES);
				break;
			default:
				throw new JPARSECException("Invalid calendar: " + calendar);
			}

			return out;
		} catch (JPARSECException e) {
			throw e;
		} catch (Exception exc) {
			throw new JPARSECException("Invalid day" + day + " in calendar " + calendar);
		}
	}
}
