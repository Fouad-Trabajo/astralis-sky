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
 * Implements a base Calendar.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public abstract class BaseCalendar implements Serializable {
	private static final long serialVersionUID = 1L;
	
	protected final long year;
    protected final int month;
    protected final int day;
    protected final long epoch;
    protected final long fixed;
    protected final double julianDate;

    BaseCalendar(final long epoch, final long year, final int month, final int day) {
        this.epoch = epoch;
        this.year = toYear(year);
        this.month = month;
        this.day = day;
        this.fixed = toFixed(year, month, day);
        this.julianDate = toJulian(this.fixed);
    }

    BaseCalendar(final long epoch, final long fromFixed) {
        this.epoch = epoch;
        this.fixed = fromFixed;
        this.julianDate = toJulian(fromFixed);
        this.year = yearFromFixed();
        this.month = monthFromFixed(this.year);
        this.day = dayFromFixed(this.year, this.month);
    }

    BaseCalendar(final long epoch, final double fromJulianDate) {
        this(epoch, (long) (Math.floor(fromJulianDate - 0.5) + 0.5) - Gregorian.EPOCH);
    }

    double toJulian (final long fixed) {
        return fixed + Gregorian.EPOCH + 0.5;
    }

    long toYear(final long year) {
        return year;
    }

    abstract long toFixed(final long year, final int month, final int day);

    abstract long yearFromFixed();

    abstract int monthFromFixed(final long year);

    abstract int dayFromFixed(final long year, final int month);

    /**
     * Returns the day of the week for ISO, Julian, and Gregorian
     * calendars. For other calendars -1 is returned.
     * @return Day of week (monday = 1), or -1.
     */
    public int getDayOfWeek() {
        return -1;
    }

    /**
     * Returns the day of the month, or the day of the week for ISO
     * calendar.
     * @return Day of the month, or day of the week for ISO calendar.
     */
    public int getDay() {
        return day;
    }

    /**
     * Returns the fixed date, i.e. the number of days elapsed since
     * the epoch of the Gregorian calendar (referred to previous midnight).
     * @return Fixed date.
     */
    public long getFixed() {
        return fixed;
    }

    /**
     * Returns the Julian date corresponding to the previous midnight
     * respect the instant represented by this calendar.
     * @return The Julian date of the previous midnight.
     */
    public double getJulianDate() {
        return julianDate;
    }

    /**
     * Returns the epoch of this calendar. The epoch is the fixed date
     * for the date of the beggining of this calendar.
     * @return The epoch.
     */
    public double getEpoch() {
        return epoch;
    }

    /**
     * Returns the month number, 1 = January.
     * @return The month, or 0 for ISO calendar.
     */
    public int getMonth() {
        return month;
    }

    /**
     * Returns the current year.
     * @return The year.
     */
    public long getYear() {
        return year;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName())
               .append(' ')
               .append(year)
               .append('/')
               .append(month)
               .append('/')
               .append(day);

        return builder.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof BaseCalendar)) {
            return false;
        }

        BaseCalendar that = (BaseCalendar) o;

        return fixed == that.fixed;
    }

    @Override
    public int hashCode() {
        return (int) (fixed ^ (fixed >>> 32));
    }
}
