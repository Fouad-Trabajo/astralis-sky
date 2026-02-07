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
import jparsec.util.JPARSECException;

/**
 * Implements the Chinese calendar.
 * <p>
 * A lunisolar calendar based on astronomical events. Days begin at civil midnight.
 * Months begin and end with the new Moon. There are about 15 versions of the Chinese calendar,
 * the one implemented here is the latest, being used since Gregorian year 1645 (Qing dinasty).
 * <p>
 * See Calendrical Calculations for reference.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Chinese extends BaseCalendar
{
	private static final long serialVersionUID = -7348372384774729919L;
	
	/**
	 * Calendar epoch.
	 */
	public static final long EPOCH = new Gregorian(-2636L, 2, 15).fixed;

	/**
	 * Calendar day number at epoch.
	 */
	public static final int DAY_NAME_EPOCH = 15;

	/**
	 * Calendar month number at epoch.
	 */
	public static final int MONTH_NAME_EPOCH = 3;

	/**
	 * Stem year names.
	 */
	public static final String YEAR_STEM_NAMES[] = { "Jia", "Yi", "Bing", "Ding", "Wu", "Ji", "Geng", "Xin", "Ren", "Gui" };

	/**
	 * Branch year names.
	 */
	public static final String YEAR_BRANCH_NAMES[] = { "Zi", "Chou", "Yin", "Mao", "Chen", "Si", "Wu", "Wei", "Shen", "You", "Xu", "Hai" };

	/**
	 * Cycle.
	 */
	public long cycle;

	/**
	 * Leap month flag.
	 */
	public boolean leapMonth;

	private transient int monthV;
	private transient int dayV;

	/**
	 * Constructor from a fixed epoch.
	 *
	 * @param fixed Fixed day.
	 */
	public Chinese(final long fixed)
	{
		super(EPOCH, fixed);
	}

	/**
	 * Constructor from a Julian day.
	 *
	 * @param julianDay Julian day.
	 */
	public Chinese(final double julianDay)
	{
		super(EPOCH, julianDay);
	}

	/**
	 * Constructor.
	 *
	 * @param cycle Cycle.
	 * @param year Year.
	 * @param month Month.
	 * @param leapMonth Leap month flag.
	 * @param day Day.
	 */
	public Chinese(final long cycle, final long year, final int month, final boolean leapMonth, final int day)
	{
		super(EPOCH, cycle * 60 + year, month, day);

		this.cycle = cycle;
		this.leapMonth = leapMonth;
	}

	@Override
	long yearFromFixed() {
		long l1 = winterSolsticeOnOrBefore(this.fixed);
		long l2 = winterSolsticeOnOrBefore(l1 + 370);
		long l3 = newMoonOnOrAfter(l1 + 1);
		long l4 = newMoonBefore(l2 + 1);
		long l5 = newMoonBefore(this.fixed + 1);

		boolean flag = Math.round((double) (l4 - l3) / Calendar.LUNAR_CYCLE) == 12L;

		this.monthV = (int) Calendar.adjustedMod(Math.round((double) (l5 - l3) / Calendar.LUNAR_CYCLE) - (long) (!flag || !hasPriorLeapMonth(l3, l5) ? 0 : 1), 12L);

		this.leapMonth = flag && hasNoMajorSolarTerm(l5) && !hasPriorLeapMonth(l3, newMoonBefore(l5));
		long y = (long) Math.floor((1.5 - (this.monthV / 12.0)) + (double) (fixed - EPOCH) / Calendar.TROPICAL_YEAR);
		this.cycle = 1 + ((y - 1) / 60);
		this.dayV = (int) ((this.fixed - l5) + 1);

		return Calendar.adjustedMod(y, 60);
	}

	@Override
	int monthFromFixed(long year) {
		return this.monthV;
	}

	@Override
	int dayFromFixed(long year, int month) {
		return this.dayV;
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
		long l1 = (long) Math.floor((double) EPOCH + ((double) ((cycle - 1L) * 60L + (long) (year - 1)) + 0.5D) * Calendar.TROPICAL_YEAR);
		long l2 = newYearOnOrBefore(l1);
		long l3 = newMoonOnOrAfter(l2 + (long) (29 * (this.month - 1)));
		Chinese chinese = new Chinese(l3);
		long l4 = this.month != chinese.month || this.leapMonth != chinese.leapMonth ? newMoonOnOrAfter(l3 + 1) : l3;

		return l4 + this.day - 1;
	}

	@Override
	long toYear(final long year) {
		return 1 + ((year - 1) % 60);
	}

	@Override
	public String toString() {
		return "Chinese {" +
				" cycle=" + this.cycle +
				", year=" + this.year +
				", month=" + this.month +
				", leapMonth=" + this.leapMonth +
				", day=" + this.day +
				" }";
	}

	/**
	 * Gets the time when the solar longitude is equal to some value.
	 *
	 * @param fixed Fixed day.
	 * @param d Desired longitude of the Sun to search for.
	 * @return Time when the Sun has this longitude in UT.
	 */
	private static double solarLongitudeOnOrAfter(final long fixed, final double d)
	{
		CityElement location = beijing(fixed);
		double d1 = Calendar.solarLongitudeAfter(Calendar.universalFromStandard(fixed, location), d);

		return Calendar.standardFromUniversal(d1, location);
	}

	private static double midnightInChina(final long fixed)
	{
		return Calendar.universalFromStandard(fixed, beijing(fixed));
	}

	private static long winterSolsticeOnOrBefore(final long fixed)
	{
		double longitude = Calendar.estimatePriorSolarLongitude(midnightInChina(fixed + 1L), Calendar.WINTER);
		long l1 = (long) (Math.floor(longitude) - 1);

		while (Calendar.WINTER > Calendar.solarLongitude(midnightInChina(l1 + 1))) {
			l1++;
		}

		return l1;
	}

	private static long newYearInSui(final long fixed)
	{
		long l1 = winterSolsticeOnOrBefore(fixed);
		long l2 = winterSolsticeOnOrBefore(l1 + 370L);
		long l3 = newMoonOnOrAfter(l1 + 1L);
		long l4 = newMoonOnOrAfter(l3 + 1L);
		long l5 = newMoonBefore(l2 + 1L);

		if (Math.round((double) (l5 - l3) / Calendar.LUNAR_CYCLE) == 12L && (hasNoMajorSolarTerm(l3) || hasNoMajorSolarTerm(l4)))
			return newMoonOnOrAfter(l4 + 1L);
		else
			return l4;
	}

	private static long newYearOnOrBefore(final long fixed)
	{
		long l1 = newYearInSui(fixed);

		if (fixed >= l1)
			return l1;
		else
			return newYearInSui(fixed - 180L);
	}

	private static int currentMajorSolarTerm(final long fixed)
	{
		double d = Calendar.solarLongitude(Calendar.universalFromStandard(fixed, beijing(fixed)));
		return (int) Calendar.adjustedMod(2L + Calendar.quotient(d, 30.0), 12L);
	}

	private static double minorSolarTermOnOrAfter(final long fixed)
	{
		double d = Calendar.mod(30.0 * Math.ceil((Calendar.solarLongitude(midnightInChina(fixed)) - 15.0) / 30.0) + 15.0, 360.0);
		return solarLongitudeOnOrAfter(fixed, d);
	}

	/**
	 * Returns previous new moon.
	 *
	 * @param fixed Fixed day.
	 * @return New Moon.
	 */
	public static long newMoonBefore(final long fixed)
	{
		double d = Calendar.newMoonBefore(midnightInChina(fixed));
		return (long) Math.floor(Calendar.standardFromUniversal(d, beijing(d)));
	}

	/**
	 * Returns next new moon.
	 *
	 * @param fixed Fixed day.
	 * @return New Moon.
	 */
	public static long newMoonOnOrAfter(final long fixed)
	{
		double d = Calendar.newMoonAfter(midnightInChina(fixed));
		return (long) Math.floor(Calendar.standardFromUniversal(d, beijing(d)));
	}

	private static boolean hasNoMajorSolarTerm(final long fixed)
	{
		return currentMajorSolarTerm(fixed) == currentMajorSolarTerm(newMoonOnOrAfter(fixed + 1L));
	}

	private static boolean hasPriorLeapMonth(final long fixed, final long l1)
	{
		return l1 >= fixed && (hasNoMajorSolarTerm(l1) || hasPriorLeapMonth(fixed, newMoonBefore(l1)));
	}

	private static ChineseName sexagesimalName(final long fixed) throws JPARSECException
	{
		return new ChineseName((int) Calendar.adjustedMod(fixed, 10L), (int) Calendar.adjustedMod(fixed, 12L));
	}

	/**
	 * Returns name of year.
	 *
	 * @param i Fixed day.
	 * @return Year name.
	 * @throws JPARSECException If an error occurs.
	 */
	public static ChineseName nameOfYear(final long i) throws JPARSECException {
		return sexagesimalName(i);
	}

	/**
	 * Returns name of month.
	 *
	 * @param i year in chinese calendar.
	 * @param j month in chinese calendar.
	 * @return Month name.
	 * @throws JPARSECException If an error occurs.
	 */
	public static ChineseName nameOfMonth(final long i, final int j) throws JPARSECException
	{
		long fixed = 12L * (long) (i - 1) + (long) (j - 1);
		return sexagesimalName(fixed + 15L);
	}

	/**
	 * Returns name of day.
	 *
	 * @param fixed Fixed day.
	 * @return Day name.
	 * @throws JPARSECException If an error occurs.
	 */
	public static ChineseName nameOfDay(final long fixed) throws JPARSECException
	{
		return sexagesimalName(fixed + 15L);
	}

	/**
	 * Returns Beijing's city object.
	 * Note that in 1929 the official time zone changed.
	 *
	 * @param d Fixed day.
	 * @return The adequate object.
	 */
	public static final CityElement beijing(final double d)
	{
		long fixed = Gregorian.yearFromFixed((long) Math.floor(d));

		return new CityElement("Beijing, China", Calendar.angle(116.0, 25.0, 0.0), 39.55, 
				fixed >= 1929L ? 8.0: 7.7611111111111111, 44);
	}

	/**
	 * Returns Tokyo's city object.
	 * Note that in 1888 the official location for Tokyo changed.
	 *
	 * @param d Fixed day.
	 * @return The adequate object.
	 */
	public static final CityElement tokyo(final double d)
	{
		long fixed = Gregorian.yearFromFixed((long) Math.floor(d));

		return (fixed < 1888L) ? 
			new CityElement("Tokyo, Japan", Calendar.angle(139D, 46D, 0.0D), 35.7, 9.3177777777777777, 24) : 
			new CityElement("Tokyo, Japan", 135.0, 35.0, 9.0, 0);
	}

	/**
	 * Returns new year.
	 *
	 * @param fixed Gregorian year.
	 * @return Fixed date of the new year for that Gregorian year.
	 */
	public static long newYear(final long fixed)
	{
		return newYearOnOrBefore(new Gregorian(fixed, 7, 1).fixed);
	}

	/**
	 * Returns dragon festival date.
	 *
	 * @param fixed Fixed day.
	 * @return Dragon festival fixed date.
	 */
	public static long dragonFestival(final long fixed)
	{
		long y = (fixed - Gregorian.yearFromFixed(EPOCH)) + 1;
		long cycle = 1 + (y - 1) / 60;
		int year = (int) Calendar.adjustedMod(y, 60);

		return new Chinese(cycle, year, 5, false, 5).fixed;
	}

	/**
	 * Returns quingMing.
	 *
	 * @param fixed Fixed day.
	 * @return QuingMing.
	 */
	public static long qingMing(final long fixed)
	{
		return (long) Math.floor(minorSolarTermOnOrAfter(new Gregorian(fixed, 3, 30).fixed));
	}

	/**
	 * Returns chinese age.
	 *
	 * @param c1 Chinese instance.
	 * @param fixed Fixed day.
	 * @return Age.
	 * @throws JPARSECException If chinese is not larger than fixed.
	 */
	public static long age(final Chinese c1, final long fixed) throws JPARSECException
	{
		Chinese c2 = new Chinese(fixed);

		if (fixed >= c1.fixed)
			return 60L * c2.cycle - c1.cycle + c2.year - c1.year + 1;
		else
			throw new JPARSECException("chinese chinese date, must be lower than second parameter "+fixed+".");
	}

	/**
	 * Returns the year number.
	 * @return The year.
	 */
	public int getYearNumber() {
		return 1 + (int) (this.cycle * 60 + this.year);
	}
}
