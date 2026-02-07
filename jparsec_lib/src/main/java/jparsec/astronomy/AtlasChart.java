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
package jparsec.astronomy;

import jparsec.math.Constant;
import jparsec.observer.LocationElement;

/**
 * A class to obtain the chart that best shows a given position in different
 * atlases.
 * <P>
 * It also supports the Rukl lunar atlas given a lunar latitude and longitude.
 *
 * @author Mark Huss, Project Pluto
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class AtlasChart
{
	// private constructor so that this class cannot be instantiated.
	private AtlasChart() {}

	/**
	 * This function returns the volume and page number in the Millennium Star Atlas that
	 * best shows the location specified.
	 *
	 * @param ra Right ascension in radians.
	 * @param dec Declination in radians.
	 * @return The Millenium Atlas volume v and page p in format v-p.
	 */
	public static String millenniumAtlas(double ra, double dec)
	{
		int page;
		ra = ra * Constant.RAD_TO_HOUR;
		dec = dec * Constant.RAD_TO_DEG;

		if (dec >= 87.) /* polar cap pages */
			page = (ra < 4. || ra > 16. ? 2 : 1);
		else if (dec <= -87.) /* polar cap pages */
			page = (ra < 4. || ra > 16. ? 516 : 515);
		else
		{
			int gore = (int) (ra / 8.), zone = (int) ((93. - dec) / 6.);
			double remains = Math.ceil(ra / 8.) * 8. - ra;
			final int per_zone[] =
			{ 2, 4, 8, 10, 12, 14, 16, 20, 20, 22, 22, 24, 24, 24, 24, 24, 24, 24, 24, 24, 22, 22, 20, 20, 16, 14, 12,
					10, 8, 4, 2 };

			page = (int) (remains * (double) per_zone[zone] / 8.) + 1 + gore * 516;
			while (0 != zone--)
				page += per_zone[zone];
		}

        // get the Volume number
        String vol = "VI";
        if (ra > 8.0 && ra <= 16.0) {
            vol = "VII";
        }
        if (ra > 16.0 && ra < 24.0) {
            vol = "VIII";
        }

        return vol + "-" + page;
	}

	/**
	 * This function returns the page number in Sky Atlas 2000 page that best
	 * shows the location specified.
	 *
	 * @param ra Right ascension in radians.
	 * @param dec Declination in radians.
	 * @return The appropriate Sky Atlas 2000 page.
	 */
	public static int skyAtlas2000(double ra, double dec)
	{
		int page;
		ra = ra * Constant.RAD_TO_HOUR;
		dec = dec * Constant.RAD_TO_DEG;

		if (Math.abs(dec) < 18.5) /* between -18.5 and 18.5 */
		{
			page = 9 + (int) (ra / 3. + 5. / 6.);
			if (page == 9)
				page = 17;
		} else if (Math.abs(dec) < 52.) /* between 18.5 and 52, N and S */
		{
			page = 4 + (int) (ra / 4.);
			if (dec < 0.)
				page += 14;
		} else
		/* above 52, N and S */
		{
			page = 1 + (int) (ra / 8.);
			if (dec < 0.)
				page += 23;
		}
		return page;
	}

	/**
	 * This function returns the volume and page number in Uranometria that best shows the
	 * location specified.
	 *
	 * @param ra Right ascension in radians.
	 * @param dec Declination in radians.
	 * @param fix472 True to swap charts 472 and 473 (needed in original
	 *        edition).
	 * @return The Uranometria volume v and page p, in format v-p.
	 */
	public static String uranometria(double ra, double dec, boolean fix472)
	{
		final int decLimits[] =
		{ -900, -845, -725, -610, -500, -390, -280, -170, -55, 55, 170, 280, 390, 500, 610, 725, 845, 900 };
		final int nDivides[] =
		{ 2, 12, 20, 24, 30, 36, 45, 45, 45, 45, 45, 36, 30, 24, 20, 12, 2 };

		ra = ra * Constant.RAD_TO_HOUR;
		dec = dec * Constant.RAD_TO_DEG;

		int divide, startValue = 472;

		for (divide = 0; (double) decLimits[divide + 1] < dec * 10.; divide++)
			startValue -= (int) nDivides[divide + 1];

		double angle = ra * (int) nDivides[divide] / 24.;
		if (nDivides[divide] >= 20)
			angle += .5;
		else if (nDivides[divide] == 12)
			angle += 5. / 12.;

		int page = (int) angle % nDivides[divide] + startValue;

		if (page >= 472 && fix472) /* charts 472 and 473 are "flipped" */
			page = (472 + 473) - page;

        String vol = "VI";
        if (page > 120) vol = "VII";

        return vol + "-" + page;
	}

	/**
	 * This function returns the page number in the original edition of
	 * Uranometria that best shows the location specified.
	 *
	 * @param ra Right ascension in radians.
	 * @param dec Declination in radians.
	 * @return The appropriate Uranometria page.
	 */
	public static String uranometria(double ra, double dec)
	{
		return uranometria(ra, dec, true);
	}

    /**
     * 17 November 2003:  first cut at code for the "new Uranometria"
     * page layout,  which is quite a bit different from the "old" layout.
     * <pre>
     * zone page dec   RA
     *  0     1 +89.9   0 00  One polar zone
     *  1     2 +79.0   0 00  Six four-hour zones
     *  2     8 +68.0   0 00  Ten 2.4 hour zones
     *  3    18 +57.0   0 00  Twelve two-hour zones
     *  4    30 +46.0   0 00  Fifteen 1.6-hour zones
     *  5    45 +35.0   0 00  Eighteen 1 1/3 hour zones
     *  6    63 +23.5   0 00  Eighteen 1 1/3 hour zones
     *  7    81 +11.5   0 00  Twenty 1.2 hour zones
     *  8   101   0.0   0 00  Twenty 1.2 hour zones
     *  9   121 -11.5   0 00  Twenty 1.2 hour zones
     * 10   141 -23.5   0 00  Eighteen 1 1/3 hour zones
     * 11   159 -35.0   0 00  Eighteen 1 1/3 hour zones
     * 12   177 -46.0   0 00  Fifteen 1.6-hour zones
     * 13   192 -57.0   0 00  Twelve two-hour zones
     * 14   204 -68.0   0 00  Ten 2.4 hour zones
     * 15   214 -79.0   0 00  Six four-hour zones
     * 16   220 -89.9   0 00  One polar zone
     * </pre>
     * Given an RA in decimal hours and a dec in decimal degrees,  the
     * following routine returns the number of the new Uranometria 2000
     * page that best shows that location. Basically,  the sky is divided into sixteen
     * zones in declination,  with each zone divided into one to twenty
     * pages (fewer pages closer to the poles,  with the zones at the
     * celestial equator and just above and below rendered as twenty pages.)
     * Within a zone,  pages start at RA=0,  then work their way _west_
     * around the sky (hence the "24-ra" in this code.).
     * @param ra Right ascension in radians.
     * @param dec Declination in radians.
     * @return The volume as 'V1-' or 'V2-', followed by the page number.
     */
    public static String uranometria2ed(double ra, double dec) {
        ra = ra * Constant.RAD_TO_HOUR;
        dec = dec * Constant.RAD_TO_DEG;

        int zone_no = (int) ((90.0 - dec) * 16.0 / 180.0 + 0.5);
        int[] zone_splits = {1, 6, 10, 12, 15, 18, 18, 20, 20, 20, 18, 18, 15, 12, 10, 6, 1};

        int rval = (int) ((24.0 - ra) * (double) zone_splits[zone_no] / 24.0 + 0.5);

        rval %= zone_splits[zone_no];
        zone_no--;
        while (zone_no >= 0) {
            rval += zone_splits[zone_no];
            zone_no--;
        }
        int page = rval + 1;
        String vol = "V1";
        if (page > 120) {
            vol = "V2";
        }

        return vol + "-" + page;
    }

	/**
	 * This function returns the page number in Rukl that best shows the lunar
	 * location specified. Returns a String to accomodate Rukl's roman numeral
	 * libration pages.
	 *
	 * @param lon lunar longitude in radians.
	 * @param lat lunar latitude in radians.
	 * @return The appropriate Rukl page.
	 */
	public static String rukl(double lon, double lat)
	{
		double x = Math.cos(lat) * Math.cos(lon);
		double y = Math.cos(lat) * Math.sin(lon);
		double z = Math.sin(lat);
		int page = -1, ix = (int) (y * 5.5 + 5.5), iy = (int) (4. - 4. * z);
		final int page_starts[] =
		{ -1, 7, 17, 28, 39, 50, 60, 68 };

		StringBuffer buff = new StringBuffer("Rukl ");
		if (x > 0.)
		{
			if (iy <= 1 || iy >= 6)
			{
				if (0 == ix)
					ix = 1;
				else if (10 == ix)
					ix = 9;
			}

			if (0 == iy || 7 == iy)
				if (1 == ix)
					ix = 2;
				else if (9 == ix)
					ix = 8;

			page = ix + page_starts[iy];
			buff.append(page);
		}

		/* Allow a basically eight-degree libration zone. This */
		/* isn't _perfect_, but it's "not bad" either. */
		if (x < Math.PI * 8. / 180. && x > -Math.PI * 8. / 180.)
		{
			int zone_no = (int) (Math.atan2(z, y) * 4. / Math.PI + 4.);
			String librationZonePages[] =
			{ "VII", "VI", "V", "IV", "III", "II", "I", "VIII" };

			if (page > -1)
				buff.append('/');

			buff.append(librationZonePages[zone_no]);
		}
		return (-1 == page) ? "" : buff.toString();
	}

	/**
	 * The set of atlas types.
	 */
	public enum ATLAS {
		/** Uranometria. */
		URANOMETRIA,
		/** Uranometria 2nd edition. */
		URANOMETRIA_2nd_EDITION,
		/** Sky Atlas 2000. */
		SKY_ATLAS_2000,
		/** Rukl lunar atlas. */
		RUKL,
		/** Millenium star atlas. */
		MILLENIUM_STAR};

	/**
	 * Returns the chart number in certain atlas.
	 *
	 * @param loc Object position. Equatorial coordinates except in Rukl atlas.
	 * @param atlas Atlas ID constant.
	 * @return Chart number as a string.
	 */
	public static String atlasChart(LocationElement loc, ATLAS atlas)
	{
		switch (atlas)
		{
		case MILLENIUM_STAR:
			return AtlasChart.millenniumAtlas(loc.getLongitude(), loc.getLatitude());
		case RUKL:
			return AtlasChart.rukl(loc.getLongitude(), loc.getLatitude());
		case URANOMETRIA:
			return AtlasChart.uranometria(loc.getLongitude(), loc.getLatitude());
		case SKY_ATLAS_2000:
			return "" + AtlasChart.skyAtlas2000(loc.getLongitude(), loc.getLatitude());
        case URANOMETRIA_2nd_EDITION:
            return "" + AtlasChart.uranometria2ed(loc.getLongitude(), loc.getLatitude());
		}
		return null; // Should never happen
	}
}
