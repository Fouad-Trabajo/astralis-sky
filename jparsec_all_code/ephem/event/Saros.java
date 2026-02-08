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
package jparsec.ephem.event;

import java.util.ArrayList;

import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.Functions;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.event.MainEvents.EVENT_TIME;
import jparsec.ephem.event.SimpleEventElement.EVENT;
import jparsec.ephem.planets.EphemElement;
import jparsec.graph.DataSet;
import jparsec.graph.chartRendering.RenderEclipse;
import jparsec.math.Constant;
import jparsec.observer.City;
import jparsec.observer.CityElement;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.AstroDate;
import jparsec.time.SiderealTime;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;
import jparsec.util.Translate.LANGUAGE;

/**
 * Obtain Saros data for certain eclipse.
 * <BR>
 * This class provides methods for obtaining a list of eclipses and their Saros
 * data. Here is a possible output data from results taken exclusively from this
 * class.
 * <BR>
 *
 * <pre>
 * SAROS CYCLE EXAMPLE, from date  01-jan-2007
 * Eclipse      type    Civil date  Julian day   Saros   Inex   Eclipse number/Total
 *                                              series
 * Moon        total   03-mar-2007   2454163.5    123      50   52/72
 * Sun       partial   19-mar-2007   2454178.6    149      32   20/71
 * Moon        total   28-ago-2007   2454340.9    128      42   40/71
 * Sun       partial   11-sep-2007   2454355.0    154      24   6/71
 * Sun       partial   07-feb-2008   2454503.7    121      77   60/71
 * Moon        total   21-feb-2008   2454517.6    133      34   26/71
 * Sun       partial   01-ago-2008   2454679.9    126      69   47/72
 * Moon      partial   16-ago-2008   2454695.4    138      26   28/82
 * Sun       annular   26-ene-2009   2454857.8    131      61   50/70
 * Moon    penumbral   09-feb-2009   2454872.1    143      18   17/72
 * Moon    penumbral   07-jul-2009   2455019.9    110      71   71/72
 * Sun         total   22-jul-2009   2455034.6    136      53   37/71
 * Moon    penumbral   06-ago-2009   2455049.5    148      10   3/70
 * Moon    penumbral   31-dic-2009   2455197.3    115      63   57/72
 * Sun       annular   15-ene-2010   2455211.8    141      45   23/70
 * Moon      partial   26-jun-2010   2455374.0    120      55   57/83
 * Sun       partial   11-jul-2010   2455389.3    146      37   27/76
 * Moon        total   21-dic-2010   2455551.8    125      47   48/72
 * Sun       partial   04-ene-2011   2455565.9    151      29   14/72
 * Sun       partial   01-jun-2011   2455714.4    118      82   68/72
 * Moon        total   15-jun-2011   2455728.3    130      39   34/71
 * Sun       partial   01-jul-2011   2455743.9    156      21   1/69
 * Sun       partial   25-nov-2011   2455890.8    123      74   53/70
 * Moon        total   10-dic-2011   2455906.1    135      31   23/71
 * Sun       annular   20-may-2012   2456068.5    128      66   58/73
 * Moon      partial   04-jun-2012   2456083.0    140      23   24/77
 * Sun         total   13-nov-2012   2456245.4    133      58   45/72
 * Moon    penumbral   28-nov-2012   2456260.1    145      15   11/71
 * Moon    penumbral   25-abr-2013   2456408.3    112      68   65/72
 * Sun       annular   10-may-2013   2456422.5    138      50   31/70
 * Moon    penumbral   25-may-2013   2456437.7    150      7    1/71
 * Moon    penumbral   18-oct-2013   2456584.5    117      60   52/71
 * Sun         total   03-nov-2013   2456600.0    143      42   23/72
 * Moon        total   15-abr-2014   2456762.8    122      52   56/74
 * Sun       partial   29-abr-2014   2456776.8    148      34   21/75
 * Moon        total   08-oct-2014   2456939.0    127      44   42/72
 * Sun       partial   23-oct-2014   2456954.4    153      26   9/70
 * Sun       partial   20-mar-2015   2457101.9    120      79   61/71
 * Moon        total   04-abr-2015   2457117.0    132      36   30/71
 * Sun       partial   13-sep-2015   2457278.8    125      71   54/73
 * Moon        total   28-sep-2015   2457293.6    137      28   26/78
 * Sun         total   09-mar-2016   2457456.6    130      63   52/73
 * Moon    penumbral   23-mar-2016   2457471.0    142      20   18/73
 * Moon    penumbral   18-ago-2016   2457618.9    109      73   72/71
 * Sun       annular   01-sep-2016   2457632.9    135      55   39/71
 * Moon    penumbral   16-sep-2016   2457648.3    147      12   8/70
 * Moon    penumbral   11-feb-2017   2457795.5    114      65   59/71
 * Sun       annular   26-feb-2017   2457811.1    140      47   29/71
 * Moon      partial   07-ago-2017   2457973.3    119      57   61/82
 * Sun         total   21-ago-2017   2457987.3    145      39   22/77
 * Moon        total   31-ene-2018   2458150.1    124      49   49/73
 * Sun       partial   15-feb-2018   2458165.4    150      31   17/71
 * Sun       partial   13-jul-2018   2458312.6    117      84   69/71
 * Moon        total   27-jul-2018   2458327.3    129      41   38/71
 * Sun       partial   11-ago-2018   2458341.9    155      23   6/71
 * Sun       partial   06-ene-2019   2458489.6    122      76   58/70
 * Moon        total   21-ene-2019   2458504.7    134      33   27/72
 * Sun       partial   02-jul-2019   2458667.3    127      68   58/82
 * Moon      partial   16-jul-2019   2458681.4    139      25   21/79
 * Sun       annular   26-dic-2019   2458843.7    132      60   46/71
 * Moon    penumbral   10-ene-2020   2458859.3    144      17   16/71
 * Moon    penumbral   05-jun-2020   2459006.3    111      70   67/71
 * Sun       annular   21-jun-2020   2459021.8    137      52   36/70
 * Moon    penumbral   05-jul-2020   2459035.7    149      9    3/71
 * Moon    penumbral   30-nov-2020   2459183.9    116      62   58/73
 * Sun         total   14-dic-2020   2459198.2    142      44   23/72
 * Moon      partial   26-may-2021   2459361.0    121      54   55/82
 * Sun       partial   10-jun-2021   2459376.0    147      36   23/80
 * Moon      partial   19-nov-2021   2459537.9    126      46   45/70
 * Sun       partial   04-dic-2021   2459552.8    152      28   13/70
 * Sun       partial   30-abr-2022   2459700.4    119      81   66/71
 * Moon        total   16-may-2022   2459715.7    131      38   34/72
 * Sun       partial   30-may-2022   2459730.0    157      20   0/70
 * Sun       partial   25-oct-2022   2459878.0    124      73   55/73
 * Moon        total   08-nov-2022   2459892.0    136      30   20/72
 * Sun       annular   20-abr-2023   2460054.7    129      65   52/80
 * Moon    penumbral   05-may-2023   2460070.2    141      22   24/72
 * Sun       annular   14-oct-2023   2460232.2    134      57   44/71
 * Moon    penumbral   28-oct-2023   2460246.4    146      14   11/72
 * Moon    penumbral   25-mar-2024   2460394.8    113      67   64/71
 * Sun         total   08-abr-2024   2460409.3    139      49   30/71
 * Moon    penumbral   18-sep-2024   2460571.6    118      59   52/73
 * Sun       annular   02-oct-2024   2460586.3    144      41   17/70
 * </pre>
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Saros
{
	/**
	 * Returns a fast an approximate location of the Sun.
	 * Error below 0.003 deg during several centuries around year 2000.
	 * @param jd Julian day in TT.
	 * @return Apparent ecliptic longitude, latitude (always 0), distance (in AU).
	 * Angles in radians.
	 */
	public static double[] getSunPosition(double jd) {
		double t = Functions.toCenturies(jd);

		// Mean longitude of Sun corrected for precession, taken from Moshier. 
		// Final accuracy up to +/- 27" or better over many millenia compared to VSOP87
		double xe = (129597742.283429 * t + 361679.198) + (-5.23e-6 * t - 2.04411e-2) * t * t;
		xe += (((((((((-8.66e-20 * t - 4.759e-17) * t + 2.424e-15) * t + 1.3095e-12) * t + 1.7451e-10) * t - 1.8055e-8) * t - 0.0000235316) * t + 0.000076) * t + 1.105414) * t + 5028.791959) * t;
		double lon = Functions.normalizeRadians(Math.PI + Constant.ARCSEC_TO_RAD * xe) * Constant.RAD_TO_DEG;
		
		/* Mean anomaly of sun = l' (J. Laskar) */
		double x = (1.2959658102304320e+08 * t + 1.2871027407441526e+06);
		x += ((((((((1.62e-20 * t - 1.0390e-17) * t - 3.83508e-15) * t + 4.237343e-13) * t + 8.8555011e-11) * t - 4.77258489e-8) * t - 1.1297037031e-5) * t + 8.7473717367324703e-05) * t - 5.5281306421783094e-01) * t * t;
		double sanomaly = Functions.normalizeRadians(Constant.ARCSEC_TO_RAD * x);

		double c = (1.9146 - .004817 * t - .000014 * t * t) * Math.sin(sanomaly);
		c = c + (.019993 - .000101 * t) * Math.sin(2 * sanomaly);
		c = c + .00029 * Math.sin(3.0 * sanomaly); // Correction to the mean ecliptic longitude

		// Now, let calculate nutation and aberration, and substract also approximate aberration
		double M1 = Functions.normalizeDegrees(124.90 - 1934.134 * t + 0.002063 * t * t) * Constant.RAD_TO_DEG;
		double M2 = Functions.normalizeDegrees(201.11 + 72001.5377 * t + 0.00057 * t * t) * Constant.RAD_TO_DEG;
		double nutLon = (-(17.2026 + 0.01737 * t) * Math.sin(M1) + (-1.32012 - 0.00013 * t) * Math.sin(M2)) * Constant.ARCSEC_TO_DEG;
		double d = nutLon -.00569;

		double slongitude = lon + c + d; // apparent longitude (error<0.003 deg)
		double slatitude = 0; // Sun's ecliptic latitude is always negligible
		double ecc = .016708617 - 4.2037E-05 * t - 1.236E-07 * t * t; // Eccentricity
		double v = sanomaly + c * Constant.DEG_TO_RAD; // True anomaly
		double sdistance = 1.000001018 * (1.0 - ecc * ecc) / (1.0 + ecc * Math.cos(v)); // In UA

		return new double[] {slongitude * Constant.DEG_TO_RAD, slatitude * Constant.DEG_TO_RAD, sdistance};
	}

	/**
	 * Returns a fast an approximate location of the Moon.
	 * Error below 0.01 deg during several centuries around year 2000.
	 * @param jd Julian day in TT.
	 * @return Apparent ecliptic longitude, latitude, distance (in Earth radii), and age (days).
	 * Angles in radians.
	 */
	public static double[] getMoonPosition(double jd) {
		double t = Functions.toCenturies(jd);
		
		// These expansions up to t^7 for the mean elements are taken from S. L. Moshier, see program cmoon
		/* Mean elongation of moon = D */
		double x = (1.6029616009939659e+09 * t + 1.0722612202445078e+06);
		x += (((((-3.207663637426e-013 * t + 2.555243317839e-011) * t + 2.560078201452e-009) * t - 3.702060118571e-005) * t + 6.9492746836058421e-03) * t /* D, t^3 */
				- 6.7352202374457519e+00) * t * t; /* D, t^2 */
		double phase = Functions.normalizeRadians(Constant.ARCSEC_TO_RAD * x);

		/* Mean distance of moon from its ascending node = F */
		x = (1.7395272628437717e+09 * t + 3.3577951412884740e+05);
		x += (((((4.474984866301e-013 * t + 4.189032191814e-011) * t - 2.790392351314e-009) * t - 2.165750777942e-006) * t - 7.5311878482337989e-04) * t /* F, t^3 */
				- 1.3117809789650071e+01) * t * t; /* F, t^2 */
		double node = Functions.normalizeRadians(Constant.ARCSEC_TO_RAD * x);

		/* Mean anomaly of sun = l' (J. Laskar) */
		x = (1.2959658102304320e+08 * t + 1.2871027407441526e+06);
		x += ((((((((1.62e-20 * t - 1.0390e-17) * t - 3.83508e-15) * t + 4.237343e-13) * t + 8.8555011e-11) * t - 4.77258489e-8) * t - 1.1297037031e-5) * t + 8.7473717367324703e-05) * t - 5.5281306421783094e-01) * t * t;
		double sanomaly = Functions.normalizeRadians(Constant.ARCSEC_TO_RAD * x);

		/* Mean anomaly of moon = l */
		x = (1.7179159228846793e+09 * t + 4.8586817465825332e+05);
		x += (((((-1.755312760154e-012 * t + 3.452144225877e-011) * t - 2.506365935364e-008) * t - 2.536291235258e-004) * t + 5.2099641302735818e-02) * t /* l, t^3 */
				+ 3.1501359071894147e+01) * t * t; /* l, t^2 */
		double anomaly = Functions.normalizeRadians(Constant.ARCSEC_TO_RAD * x);

		/* Mean longitude of moon, re mean ecliptic and equinox of date = L */
		x = (1.7325643720442266e+09 * t + 7.8593980921052420e+05);
		x += (((((7.200592540556e-014 * t + 2.235210987108e-010) * t - 1.024222633731e-008) * t - 6.073960534117e-005) * t + 6.9017248528380490e-03) * t /* L, t^3 */
				- 5.6550460027471399e+00) * t * t; /* L, t^2 */
		double l = Functions.normalizeRadians(Constant.ARCSEC_TO_RAD * x) * Constant.RAD_TO_DEG;
		
		// Now, let calculate nutation and aberration
		double M1 = Functions.normalizeDegrees(124.90 - 1934.134 * t + 0.002063 * t * t) * Constant.DEG_TO_RAD;
		double M2 = Functions.normalizeDegrees(201.11 + 72001.5377 * t + 0.00057 * t * t) * Constant.DEG_TO_RAD;

		double Psin = 29.530588853;
		double age = Psin * phase / Constant.TWO_PI;

		double E = 1.0 - (.002495 + 7.52E-06 * (t + 1.0)) * (t + 1.0);

		// Now longitude, with the three main correcting terms of evection,
		// variation, and equation of year, plus other terms (error<0.01 deg)
		l += 6.28875 * Math.sin(anomaly) + 1.274018 * Math.sin(2 * phase - anomaly) + .658309 * Math.sin(2 * phase);
		l +=  0.213616 * Math.sin(2 * anomaly) - E * .185596 * Math.sin(sanomaly) - 0.114336 * Math.sin(2 * node);
		l += .058793 * Math.sin(2 * phase - 2 * anomaly) + .057212 * E * Math.sin(2 * phase - anomaly - sanomaly) + .05332 * Math.sin(2 * phase + anomaly);
		l += .045874 * E * Math.sin(2 * phase - sanomaly) + .041024 * E * Math.sin(anomaly - sanomaly) - .034718 * Math.sin(phase) - E * .030465 * Math.sin(sanomaly + anomaly);
		l += .015326 * Math.sin(2 * (phase - node)) - .012528 * Math.sin(2 * node + anomaly) - .01098 * Math.sin(2 * node - anomaly) + .010674 * Math.sin(4 * phase - anomaly);
		l += .010034 * Math.sin(3 * anomaly) + .008548 * Math.sin(4 * phase - 2 * anomaly);
		l += -E * .00791 * Math.sin(sanomaly - anomaly + 2 * phase) - E * .006783 * Math.sin(2 * phase + sanomaly) + .005162 * Math.sin(anomaly - phase) + E * .005 * Math.sin(sanomaly + phase);
		l += .003862 * Math.sin(4 * phase) + E * .004049 * Math.sin(anomaly - sanomaly + 2 * phase) + .003996 * Math.sin(2 * (anomaly + phase)) + .003665 * Math.sin(2 * phase - 3 * anomaly);
		
		// Fit DE404
		l += 14.1983 * Math.cos(2.3232 * t + 0.4964) / 3600.0;
		double ph = 0.5582 + 0.11 * t;
		l += 7.2167 * Math.cos(33.8624 * t - ph) / 3600.0;
		// Additional terms to fit DE431
		//l += -0.0759777 * Math.pow(t - 1.541336, 2.0) / 3600.0;
		// Previous term can be replaced by this one for a better fit between years -2000 to 6000
		l += (-0.0001617 * t * t * t - 0.075925 * t * t + 0.3932 * t - 0.4375) / 3600.0;
		l += -0.01 * t * t * Math.cos(anomaly) / 3600.0;

		double longitude = l;

		// Let's add nutation here also
		double nutLon = (-(17.2026 + 0.01737 * t) * Math.sin(M1) + (-1.32012 - 0.00013 * t) * Math.sin(M2)) * Constant.ARCSEC_TO_DEG;
		longitude += nutLon;

		// Now Moon parallax
		double parallax = .950724 + .051818 * Math.cos(anomaly) + .009531 * Math.cos(2 * phase - anomaly);
		parallax += .007843 * Math.cos(2 * phase) + .002824 * Math.cos(2 * anomaly);
		parallax += 0.000857 * Math.cos(2 * phase + anomaly) + E * .000533 * Math.cos(2 * phase - sanomaly);
		parallax += E * .000401 * Math.cos(2 * phase - anomaly - sanomaly) + E * .00032 * Math.cos(anomaly - sanomaly) - .000271 * Math.cos(phase);
		parallax += -E * .000264 * Math.cos(sanomaly + anomaly) - .000198 * Math.cos(2 * node - anomaly);

		ph = (20 - 1799 * t) * Constant.DEG_TO_RAD;
		parallax += 0.0000925 * Math.cos(ph);
		
		// Ecliptic latitude with nodal phase (error<0.01 deg)
		l = 5.128189 * Math.sin(node) + 0.280606 * Math.sin(node + anomaly) + 0.277693 * Math.sin(anomaly - node);
		l += .173238 * Math.sin(2 * phase - node) + .055413 * Math.sin(2 * phase + node - anomaly);
		l += .046272 * Math.sin(2 * phase - node - anomaly) + .032573 * Math.sin(2 * phase + node);
		l += .017198 * Math.sin(2 * anomaly + node) + .009267 * Math.sin(2 * phase + anomaly - node);
		l += .008823 * Math.sin(2 * anomaly - node) + E * .008247 * Math.sin(2 * phase - sanomaly - node) + .004323 * Math.sin(2 * (phase - anomaly) - node);
		l += .0042 * Math.sin(2 * phase + node + anomaly) + E * .003372 * Math.sin(node - sanomaly - 2 * phase);
		
		l +=  -8 * Math.sin(M1) * Math.cos(node) / 3600.0;
		l +=  -0.007 * t * t * Math.cos(node) / 3600.0;
		
		double latitude = l;

		// Moon distance in Earth radii is, more or less,
		double distance = 1.0 / Math.sin(parallax * Constant.DEG_TO_RAD);

		return new double[] {longitude * Constant.DEG_TO_RAD, latitude * Constant.DEG_TO_RAD, distance, age};
	}

	/**
	 * Find all eclipses between two dates, including all saros information.
	 * Algorithm is approximate, and only valid in current epoch.
	 *
	 * @param jd_initial Initial jd to search in TT.
	 * @param jd_final Final jd to search in TT.
	 * @return A set of double precission arrays, with the following
	 *         information ordered:
	 *         <BR> - Julian day of the eclipse in TT (maximum, with an accuracy up to
	 *         one or two minutes).
	 *         <BR> - Eclipse type, solar or lunar. The value is the ordinal of the
	 *         eclipse type enumeration defined in SimpleEventElement class.
	 *         <BR> - Eclipse subtype, ID constants defined in
	 *         SolarEclipse class (even for lunar eclipses). For penumbral lunar
	 *         eclipses, the constant type_no_eclipse is used.
	 *         <BR> - Saros series.
	 *         <BR> - Inex number.
	 *         <BR> - Eclipse number in the current saros series.
	 *         <BR> - Number of the last eclipse in the current saros series.
	 * @throws JPARSECException If an error occurs.
	 * @deprecated The methods in {@linkplain MainEvents} should be used instead.
	 */
	public static ArrayList<double[]> getAllEclipses(double jd_initial, double jd_final) throws JPARSECException
	{
		double Psin = 29.530588853;

		ArrayList<double[]> out = new ArrayList<double[]>();

		// Cycle of calculations
		double step = 1.0 / 1440.0; // 1 minute precision (a lot less than a eclipse length, required for correct eclipse type in many cases)
		double jd = jd_initial - step;
		do {
			jd += step;

			double t = Functions.toCenturies(jd);
			
			// Mean longitude of Sun corrected for precession, taken from Moshier. 
			// Final accuracy up to +/- 27" or better over many millenia compared to VSOP87
			double xe = (129597742.283429 * t + 361679.198) + (-5.23e-6 * t - 2.04411e-2) * t * t;
			xe += (((((((((-8.66e-20 * t - 4.759e-17) * t + 2.424e-15) * t + 1.3095e-12) * t + 1.7451e-10) * t - 1.8055e-8) * t - 0.0000235316) * t + 0.000076) * t + 1.105414) * t + 5028.791959) * t;
			double lon = Functions.normalizeRadians(Math.PI + Constant.ARCSEC_TO_RAD * xe) * Constant.RAD_TO_DEG;
			
			/* Mean anomaly of sun = l' (J. Laskar) */
			double x = (1.2959658102304320e+08 * t + 1.2871027407441526e+06);
			x += ((((((((1.62e-20 * t - 1.0390e-17) * t - 3.83508e-15) * t + 4.237343e-13) * t + 8.8555011e-11) * t - 4.77258489e-8) * t - 1.1297037031e-5) * t + 8.7473717367324703e-05) * t - 5.5281306421783094e-01) * t * t;
			double sanomaly = Functions.normalizeRadians(Constant.ARCSEC_TO_RAD * x);

			double c = (1.9146 - .004817 * t - .000014 * t * t) * Math.sin(sanomaly);
			c = c + (.019993 - .000101 * t) * Math.sin(2 * sanomaly);
			c = c + .00029 * Math.sin(3.0 * sanomaly); // Correction to the mean ecliptic longitude

			// Now, let calculate nutation and aberration, and substract also approximate aberration
			double M1 = Functions.normalizeDegrees(124.90 - 1934.134 * t + 0.002063 * t * t) * Constant.DEG_TO_RAD;
			double M2 = Functions.normalizeDegrees(201.11 + 72001.5377 * t + 0.00057 * t * t) * Constant.DEG_TO_RAD;
			double nutLon = (-(17.2026 + 0.01737 * t) * Math.sin(M1) + (-1.32012 - 0.00013 * t) * Math.sin(M2)) * Constant.ARCSEC_TO_DEG;
			double d = nutLon -.00569;

			double slongitude = lon + c + d; // apparent longitude (error<0.003 deg)
			double slatitude = 0; // Sun's ecliptic latitude is always negligible
			double ecc = .016708617 - 4.2037E-05 * t - 1.236E-07 * t * t; // Eccentricity
			double v = sanomaly + c * Constant.DEG_TO_RAD; // True anomaly
			double sdistance = 1.000001018 * (1.0 - ecc * ecc) / (1.0 + ecc * Math.cos(v)); // In UA

			// MOON PARAMETERS
			// These expansions up to t^7 for the mean elements are taken from S. L. Moshier, see program cmoon
			/* Mean elongation of moon = D */
			x = (1.6029616009939659e+09 * t + 1.0722612202445078e+06);
			x += (((((-3.207663637426e-013 * t + 2.555243317839e-011) * t + 2.560078201452e-009) * t - 3.702060118571e-005) * t + 6.9492746836058421e-03) * t /* D, t^3 */
					- 6.7352202374457519e+00) * t * t; /* D, t^2 */
			double phase = Functions.normalizeRadians(Constant.ARCSEC_TO_RAD * x);

			/* Mean distance of moon from its ascending node = F */
			x = (1.7395272628437717e+09 * t + 3.3577951412884740e+05);
			x += (((((4.474984866301e-013 * t + 4.189032191814e-011) * t - 2.790392351314e-009) * t - 2.165750777942e-006) * t - 7.5311878482337989e-04) * t /* F, t^3 */
					- 1.3117809789650071e+01) * t * t; /* F, t^2 */
			double node = Functions.normalizeRadians(Constant.ARCSEC_TO_RAD * x);

			/* Mean anomaly of moon = l */
			x = (1.7179159228846793e+09 * t + 4.8586817465825332e+05);
			x += (((((-1.755312760154e-012 * t + 3.452144225877e-011) * t - 2.506365935364e-008) * t - 2.536291235258e-004) * t + 5.2099641302735818e-02) * t /* l, t^3 */
					+ 3.1501359071894147e+01) * t * t; /* l, t^2 */
			double anomaly = Functions.normalizeRadians(Constant.ARCSEC_TO_RAD * x);

			/* Mean longitude of moon, re mean ecliptic and equinox of date = L */
			x = (1.7325643720442266e+09 * t + 7.8593980921052420e+05);
			x += (((((7.200592540556e-014 * t + 2.235210987108e-010) * t - 1.024222633731e-008) * t - 6.073960534117e-005) * t + 6.9017248528380490e-03) * t /* L, t^3 */
					- 5.6550460027471399e+00) * t * t; /* L, t^2 */
			double l = Functions.normalizeRadians(Constant.ARCSEC_TO_RAD * x) * Constant.RAD_TO_DEG;
			
			double E = 1.0 - (.002495 + 7.52E-06 * (t + 1.0)) * (t + 1.0);

			// Now longitude, with the three main correcting terms of evection,
			// variation, and equation of year, plus other terms (error<0.01 deg)
			l += 6.28875 * Math.sin(anomaly) + 1.274018 * Math.sin(2 * phase - anomaly) + .658309 * Math.sin(2 * phase);
			l +=  0.213616 * Math.sin(2 * anomaly) - E * .185596 * Math.sin(sanomaly) - 0.114336 * Math.sin(2 * node);
			l += .058793 * Math.sin(2 * phase - 2 * anomaly) + .057212 * E * Math.sin(2 * phase - anomaly - sanomaly) + .05332 * Math.sin(2 * phase + anomaly);
			l += .045874 * E * Math.sin(2 * phase - sanomaly) + .041024 * E * Math.sin(anomaly - sanomaly) - .034718 * Math.sin(phase) - E * .030465 * Math.sin(sanomaly + anomaly);
			l += .015326 * Math.sin(2 * (phase - node)) - .012528 * Math.sin(2 * node + anomaly) - .01098 * Math.sin(2 * node - anomaly) + .010674 * Math.sin(4 * phase - anomaly);
			l += .010034 * Math.sin(3 * anomaly) + .008548 * Math.sin(4 * phase - 2 * anomaly);
			l += -E * .00791 * Math.sin(sanomaly - anomaly + 2 * phase) - E * .006783 * Math.sin(2 * phase + sanomaly) + .005162 * Math.sin(anomaly - phase) + E * .005 * Math.sin(sanomaly + phase);
			l += .003862 * Math.sin(4 * phase) + E * .004049 * Math.sin(anomaly - sanomaly + 2 * phase) + .003996 * Math.sin(2 * (anomaly + phase)) + .003665 * Math.sin(2 * phase - 3 * anomaly);
			
			// Fit DE404
			l += 14.1983 * Math.cos(2.3232 * t + 0.4964) / 3600.0;
			double ph = 0.5582 + 0.11 * t;
			l += 7.2167 * Math.cos(33.8624 * t - ph) / 3600.0;
			// Additional terms to fit DE431
			//l += -0.0759777 * Math.pow(t - 1.541336, 2.0) / 3600.0;
			// Previous term can be replaced by this one for a better fit between years -2000 to 6000
			l += (-0.0001617 * t * t * t - 0.075925 * t * t + 0.3932 * t - 0.4375) / 3600.0;
			l += -0.01 * t * t * Math.cos(anomaly) / 3600.0;

			double longitude = l + nutLon;
			double age = Functions.normalizeDegrees(longitude - slongitude) * Psin / 360.0;

			// Now Moon parallax
			double parallax = .950724 + .051818 * Math.cos(anomaly) + .009531 * Math.cos(2 * phase - anomaly);
			parallax += .007843 * Math.cos(2 * phase) + .002824 * Math.cos(2 * anomaly);
			parallax += 0.000857 * Math.cos(2 * phase + anomaly) + E * .000533 * Math.cos(2 * phase - sanomaly);
			parallax += E * .000401 * Math.cos(2 * phase - anomaly - sanomaly) + E * .00032 * Math.cos(anomaly - sanomaly) - .000271 * Math.cos(phase);
			parallax += -E * .000264 * Math.cos(sanomaly + anomaly) - .000198 * Math.cos(2 * node - anomaly);

			ph = (20 - 1799 * t) * Constant.DEG_TO_RAD;
			parallax += 0.0000925 * Math.cos(ph);
			
			// Ecliptic latitude with nodal phase (error<0.01 deg)
			l = 5.128189 * Math.sin(node) + 0.280606 * Math.sin(node + anomaly) + 0.277693 * Math.sin(anomaly - node);
			l += .173238 * Math.sin(2 * phase - node) + .055413 * Math.sin(2 * phase + node - anomaly);
			l += .046272 * Math.sin(2 * phase - node - anomaly) + .032573 * Math.sin(2 * phase + node);
			l += .017198 * Math.sin(2 * anomaly + node) + .009267 * Math.sin(2 * phase + anomaly - node);
			l += .008823 * Math.sin(2 * anomaly - node) + E * .008247 * Math.sin(2 * phase - sanomaly - node) + .004323 * Math.sin(2 * (phase - anomaly) - node);
			l += .0042 * Math.sin(2 * phase + node + anomaly) + E * .003372 * Math.sin(node - sanomaly - 2 * phase);
			
			l +=  -8 * Math.sin(M1) * Math.cos(node) / 3600.0;
			l +=  -0.007 * t * t * Math.cos(node) / 3600.0;
			
			double latitude = l;

			// Moon distance in Earth radii is, more or less,
			double distance = 1.0 / Math.sin(parallax * Constant.DEG_TO_RAD);

			// LET SEE NOW IF THE ECLIPSE CONDITIONS ARE SATISFIED
			double ldiam = .2725 * parallax * 2;
			double sdiam = 2.0 * Math.atan2(TARGET.SUN.equatorialRadius, sdistance * Constant.AU) * Constant.RAD_TO_DEG;
			double latLimitEclipse = parallax + (sdiam + ldiam) * 0.5; // 1.53 approx
			double lonDif = Functions.normalizeDegrees(Math.abs(longitude - slongitude));
			double latDif = Math.abs(latitude - slatitude);
			
			// Since time step is 0.05 hrs, Moon will move in steps of 0.025 deg, so I put here a sensitivity of 0.033 deg
			if (latDif < latLimitEclipse && (lonDif < 0.012 || Math.abs(180 - lonDif) < 0.012)) {		
				SimpleEventElement.EVENT ecl_type = SimpleEventElement.EVENT.MOON_LUNAR_ECLIPSE;
				SolarEclipse.ECLIPSE_TYPE type = SolarEclipse.ECLIPSE_TYPE.TOTAL;
				if (age < 5 || age > 20) {
					ecl_type = SimpleEventElement.EVENT.MOON_SOLAR_ECLIPSE; // Solar eclipse
					if (sdiam > ldiam) type = SolarEclipse.ECLIPSE_TYPE.ANNULAR;
					// Shadow distance projected towards the poles in Earth radii
					double projection = Math.abs(latitude) + (ldiam - sdiam) * 0.5 - parallax;
					if (projection > 0) type = SolarEclipse.ECLIPSE_TYPE.PARTIAL; // Shadow not visible
				} else {
					latLimitEclipse = parallax - (sdiam + ldiam) * 0.5; // 0.46 approx
					if (Math.abs(latitude - slatitude) > latLimitEclipse)
						type = SolarEclipse.ECLIPSE_TYPE.PARTIAL;
					latLimitEclipse = parallax - (sdiam - ldiam) * 0.5; // 0.8 approx
					if (Math.abs(latitude - slatitude) > latLimitEclipse)
						type = SolarEclipse.ECLIPSE_TYPE.NO_ECLIPSE; // Penumbral
				}

				Saros saros = new Saros(jd, ecl_type);
				double eclipse_element[] = new double[]
						{ saros.eclipseDate, ecl_type.ordinal(), type.ordinal(), saros.sarosSeries, saros.inexCycle,
						saros.sarosEclipseNumber, saros.sarosEclipseMaxNumber };
					out.add(eclipse_element);
				jd = jd + 3;
				continue;
			}

			// If no eclipse is found change jd to the adequate value to accelerate calculations.
			double ld = Math.min(Math.min(lonDif, 360.0 - lonDif), Math.abs(180 - lonDif));
			jd += ld / 24.0;
		} while (jd < jd_final);

		return out;
	}

	/**
	 * Gets the integer ID of the eclipsed body.
	 *
	 * @param type Eclipse type, constants defined in SimpleEventElement class.
	 * @return {@linkplain TARGET#Moon} or {@linkplain TARGET#SUN} constants.
	 * @throws JPARSECException If input type is invalid.
	 */
	public static TARGET getEclipsedTarget(SimpleEventElement.EVENT type) throws JPARSECException
	{
		if (type == SimpleEventElement.EVENT.MOON_LUNAR_ECLIPSE)
			return TARGET.Moon;
		if (type == SimpleEventElement.EVENT.MOON_SOLAR_ECLIPSE)
			return TARGET.SUN;
		throw new JPARSECException("invalid input type " + type.name() + ".");
	}

	/**
	 * Transform an eclipse subtype into a string representation.
	 *
	 * @param type Eclipse type.
	 * @return total, annular, partial, or penumbral.
	 */
	public static String getEclipseTypeAsString(SolarEclipse.ECLIPSE_TYPE type)
	{
		if (type == SolarEclipse.ECLIPSE_TYPE.ANNULAR) return Translate.translate(167);
		if (type == SolarEclipse.ECLIPSE_TYPE.PARTIAL) return Translate.translate(169);
		if (type == SolarEclipse.ECLIPSE_TYPE.NO_ECLIPSE) return Translate.translate(170);

		return Translate.translate(168); // total
	}

	/**
	 * Obtain saros information for an eclipse that occurs around certain Julian
	 * day. Eclipse date and type are
	 * obtained from approximate assumptions, and it is not recommended for far
	 * away epochs.
	 *
	 * @param jd Julian day of eclipse.
	 * @throws JPARSECException If an error occurs.
	 */
	public Saros(double jd) throws JPARSECException
	{
		this(jd, null);
	}

	/**
	 * Obtain saros information for an eclipse that occurs around certain Julian
	 * day. Results are set to static variables. Eclipse date and type are
	 * supposed to be input date and type.
	 *
	 * @param jd Julian day of eclipse.
	 * @param type The eclipse type.
	 * @throws JPARSECException If an error occurs.
	 */
	public Saros(double jd, SimpleEventElement.EVENT type) throws JPARSECException
	{
		if (type == null) {
			AstroDate astro = new AstroDate(jd);
			double year = astro.getAstronomicalYear() + (astro.getMonth() - 1.0 + astro.getDayPlusFraction() / (1.0 + astro.getDaysInMonth())) / 12.0;
			double kapprox = (year - 2000) * 12.3685;
			double t = kapprox / 1236.85;
			double kexact = (jd - (0.0001337 * t * t - 0.000000150 * t * t * t + 0.00000000073 * t * t * t * t) - 2451550.09765) / 29.530588853;
			double dec = kexact - Math.floor(kexact);
			if (dec < 0) dec ++;
			type = SimpleEventElement.EVENT.MOON_SOLAR_ECLIPSE;
			if (Math.abs(dec - 0.5) < 0.25) type = SimpleEventElement.EVENT.MOON_LUNAR_ECLIPSE;
		}

		if (type != SimpleEventElement.EVENT.MOON_LUNAR_ECLIPSE && type != SimpleEventElement.EVENT.MOON_SOLAR_ECLIPSE)
			throw new JPARSECException("invalid eclipse type or eclipse not found.");

		// Read saros data to a more confortable way
		double jdsaros0[][] = new double[200][2];
		double jdsaros1[][] = new double[200][2];
		double status[][] = new double[200][2];
		for (int i = 0; i <= 199; i++)
		{
			for (int j = 0; j <= 1; j++)
			{
				jdsaros0[i][j] = 0.0;
				jdsaros1[i][j] = 0.0;
				status[i][j] = 0.0;
			}
		}
		int n = 0;
		for (int i = 0; i <= 1; i++)
		{
			int s = 0, z = 0;
			double jd1 = 1.0, jd2 = 1.0;
			do
			{
				s = (int) Saros.sarosData[n + 0];
				jd1 = Saros.sarosData[n + 1];
				jd2 = Saros.sarosData[n + 2];
				z = (int) Saros.sarosData[n + 3];
				n = n + 4;

				jdsaros0[s][i] = jd1;
				jdsaros1[s][i] = jd2;
				status[s][i] = z;
			} while ((s + jd1 + jd2 + z) != 0);
		}

		// Start calculations
		double Psin = 29.530588853;
		int lunation = (int) Math.floor(jd / Psin - 82064);
		int D = lunation - 848;
		int C = (int) Math.floor((-61 * D + 358 / 2 - D / (12.0 * 358.0)) / 358.0);
		int saros = 136 + 38 * D + 223 * C;

		// If it is a lunar eclipse
		if (type == SimpleEventElement.EVENT.MOON_LUNAR_ECLIPSE)
			saros = saros + 12;

		// Now calculate the eclipse relative number in the inex cycle,
		// following G. Van den Bergh
		int inex = (int) (((jd - 2347927.0) / Psin - (saros - 124.0) * 358.0) / 223.0 + 56.5);
		if (type == SimpleEventElement.EVENT.MOON_LUNAR_ECLIPSE)
			inex = (int) (((jd - 2457471.0) / Psin - (saros - 142.0) * 358.0) / 223.0 + 20.5);

		// Set provisional results
		sarosSeries = saros;
		inexCycle = inex;
		sarosEclipseNumber = INVALID_SAROS_RESULT;
		sarosEclipseMaxNumber = INVALID_SAROS_RESULT;

		// This is to improve maximum estimate
		SimpleEventElement see = MainEvents.MoonPhaseOrEclipse(jd, type, MainEvents.EVENT_TIME.CLOSEST);
		eclipseDate = see.time;

		// Now calculate the eclipse relative number in the series and the
		// maximum number of eclipses in that series
		if (saros > 0 && saros < 180)
		{
			int ni = type.ordinal() - Math.min(SimpleEventElement.EVENT.MOON_LUNAR_ECLIPSE.ordinal(), SimpleEventElement.EVENT.MOON_SOLAR_ECLIPSE.ordinal());
 			int numeclipse = (int) (((jd - jdsaros0[saros][ni]) / Psin) / 223.0 + 1.5);
			int nummax = (int) (((jdsaros1[saros][ni] - jdsaros0[saros][ni]) / Psin) / 223.0 + 1.5);

			// Dont save values if they are incorrect
			if (status[saros][ni] == 0)
			{
				numeclipse = INVALID_SAROS_RESULT;
				nummax = INVALID_SAROS_RESULT;
			}
			if (status[saros][ni] == 2 || nummax < 69)
				nummax = INVALID_SAROS_RESULT;

			sarosSeries = saros;
			inexCycle = inex;
			sarosEclipseNumber = numeclipse;
			sarosEclipseMaxNumber = nummax;
		}

	}

	/**
	 * Returns the list of countries where a given eclipse will be visible. This method is 
	 * not efficient.
	 * @param jdMax The Julian day of the maximum of the eclipse in TT. You can set
	 * the value obtained with other methods in this class, or a value within 6 hours
	 * around it for a given phase of the eclipse. Otherwise, the maximum will be
	 * computed and used instead for the closest eclipse to the given date (within 1 day).
	 * @param stepDeg step in degrees to search for the visibility around the Earth.
	 * 5 degrees is often good enough.
	 * @return Two strings at most containing a list of cities where the solar eclipse
	 * is partial (first string) and total (second string). The list of countries with a
	 * partial eclipse will contain, for each country, a values with the maximum magnitude
	 * of the eclipse found for any of the cities in the database that belongs to each 
	 * country. For lunar eclipses only one string is returned.
	 * @throws JPARSECException If an error occurs, for instance if in the provided date
	 * there is no eclipse.
	 */
	public static String[] getEclipseVisibility(double jdMax, double stepDeg) throws JPARSECException {
		
		double JDMoon = MainEvents.MoonPhaseOrEclipse(jdMax, EVENT.MOON_LUNAR_ECLIPSE, EVENT_TIME.CLOSEST).time;
		double JDSun = MainEvents.MoonPhaseOrEclipse(jdMax, EVENT.MOON_SOLAR_ECLIPSE, EVENT_TIME.CLOSEST).time;
		double difMoon = Math.abs(jdMax-JDMoon);
		double difSun = Math.abs(jdMax-JDSun);
		
		if (difMoon > 1 && difSun > 1) throw new JPARSECException("No eclipse on "+jdMax);
		EVENT type = EVENT.MOON_LUNAR_ECLIPSE;
		double max = JDMoon;
		if (difSun <= 1) {
			type = EVENT.MOON_SOLAR_ECLIPSE;
			max = JDSun;
		}
		if (Math.abs(jdMax - max) < 0.25) max = jdMax;

		TimeElement time = new TimeElement(max, SCALE.TERRESTRIAL_TIME);
		EphemerisElement eph = new EphemerisElement(TARGET.Moon, EphemerisElement.COORDINATES_TYPE.APPARENT,
				EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.GEOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
				EphemerisElement.FRAME.DYNAMICAL_EQUINOX_J2000, EphemerisElement.ALGORITHM.MOSHIER);
		CityElement city = City.findCity("Madrid");
		ObserverElement observer = ObserverElement.parseCity(city);

		EphemElement ephemMoon = Ephem.getEphemeris(time, observer, eph, false);
		double gmt = SiderealTime.greenwichApparentSiderealTime(time, observer, eph);
		LocationElement locMoon = new LocationElement(ephemMoon.rightAscension - gmt,
				ephemMoon.declination, 1.0);

		LocationElement locEcl = locMoon.clone(), locSun = null;
		if (type == EVENT.MOON_SOLAR_ECLIPSE) {
			eph.targetBody = TARGET.SUN;
			EphemElement ephemSun = Ephem.getEphemeris(time, observer, eph, false);
			locSun = new LocationElement(ephemSun.rightAscension - gmt, ephemSun.declination, 1.0);
			double d = (ephemMoon.declination - ephemSun.declination) * ephemMoon.distance * Constant.AU;
			double lat = locMoon.getLatitude() + Math.asin(d / Constant.EARTH_RADIUS);
			if (lat > Constant.PI_OVER_TWO) lat = Constant.PI_OVER_TWO - (lat - Constant.PI_OVER_TWO);
			if (lat < -Constant.PI_OVER_TWO) lat = -Constant.PI_OVER_TWO + (-Constant.PI_OVER_TWO - lat);
			locEcl.setLatitude(lat);
		}

		double step = stepDeg*Constant.DEG_TO_RAD;
		String out = "", outTotal = null;
		RenderEclipse re = null;
		if (type == EVENT.MOON_SOLAR_ECLIPSE) {
			try {
				re = new RenderEclipse(new AstroDate(max - 0.25));
			} catch (Exception exc) {
				re = new RenderEclipse(new AstroDate(max + 0.25));
			}
			outTotal = "";
		}
		CityElement cities[] = City.getAllCities();
		double magLunar = -1;
		for (double lon=Math.PI; lon>-Math.PI;lon=lon-step) {
			for (double lat=-Constant.PI_OVER_TWO+step; lat<=Constant.PI_OVER_TWO-step;lat=lat+step) {
				LocationElement loc = new LocationElement(lon, lat, 1.0);
				if (type == EVENT.MOON_SOLAR_ECLIPSE) {
					double d = LocationElement.getApproximateAngularDistance(locSun, loc);
					if (d > Constant.PI_OVER_TWO) continue;
				} else {
					double d = LocationElement.getApproximateAngularDistance(locEcl, loc);
					if (d > Constant.PI_OVER_TWO) continue;
				}

				CityElement c = null;
				double minDist = -1;
				for (int i=0; i<cities.length; i++) {
					LocationElement cp = new LocationElement(cities[i].longitude*Constant.DEG_TO_RAD,
							cities[i].latitude*Constant.DEG_TO_RAD, 1.0);
					double dist = LocationElement.getApproximateAngularDistance(cp, loc);
					if (dist < minDist || minDist == -1) {
						c = cities[i];
						minDist = dist;
					}
				}
				if (c == null || minDist > step*0.5) continue;

				String add = "";
				observer.setLongitudeRad(loc.getLongitude());
				observer.setLatitudeRad(loc.getLatitude());
				observer.setName(c.name);
				if (c.country.equals("Spain") && observer.getLatitudeDeg() < 32) {
					add = "Canary Islands, ";
					if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH) add = "Islas Canarias, ";
				}
				if (type == EVENT.MOON_SOLAR_ECLIPSE) {
					if (!re.isVisible(observer)) continue;
					double maxMag = re.getGreatestMagnitude(observer);
					if (maxMag < 0) continue;
					boolean total = false;
					if (maxMag > 0.99) total = true;
					if (total) {
						if (outTotal.indexOf(c.country) < 0 || (!add.equals("") && outTotal.indexOf(add.substring(0, add.indexOf(","))) < 0)) {
							if (add.equals("")) {
								outTotal += c.country+", ";
							} else {
								outTotal += c.country+" ("+add.substring(0, add.indexOf(","))+"), ";
							}
						}
					} else {
						if (out.indexOf(c.country) < 0 || (!add.equals("") && out.indexOf(add) < 0)) {
							out += c.country+" ("+add+Functions.formatValue(maxMag, 2)+"), ";
						} else {
							int i1 = out.indexOf(c.country);
							if (i1 < 0 && !add.equals("")) i1 = out.indexOf(add);
							if (i1 >= 0) {
								String entry = out.substring(i1);
								if (entry.indexOf("), ") > 0) entry = entry.substring(0, entry.indexOf("), ")+3);
								if (entry.indexOf("(") > 0) {
									String mag = entry.substring(entry.indexOf("(") + 1);
									mag = mag.substring(0, mag.indexOf(")"));
									if (mag.indexOf(",") > 0) mag = mag.substring(mag.indexOf(",")+1).trim();
									double dmag = Double.parseDouble(mag);
									if (maxMag > dmag) {
										String newEntry = c.country+" ("+add+Functions.formatValue(maxMag, 2)+"), ";
										out = DataSet.replaceOne(out, entry, newEntry, 1);
									}
								}
							}							
						}
					}
				} else {
					if (magLunar < 0) magLunar = RenderEclipse.lunarEclipseMagnitude(new TimeElement(jdMax, SCALE.TERRESTRIAL_TIME), observer, eph);
					if (magLunar < 0) continue;
					if (out.indexOf(c.country) < 0 || (!add.equals("") && out.indexOf(add) < 0))
						out += c.country+" ("+add+Functions.formatValue(magLunar, 2)+"), ";
				}
			}
		}
		out = out.substring(0, out.length()-2);
		if (outTotal != null && outTotal.length() > 0) {
			outTotal = outTotal.substring(0, outTotal.length()-2);
			return new String[] {out, outTotal};
		}
		return new String[] {out};
	}

	/**
	 * Holds saros series.
	 */
	public int sarosSeries;

	/**
	 * Holds inex.
	 */
	public int inexCycle;

	/**
	 * Holds saros eclipse number in series.
	 */
	public int sarosEclipseNumber;

	/**
	 * Holds saros maximum number of eclipses in this series.
	 */
	public int sarosEclipseMaxNumber;

	/**
	 * Holds the instant of the maximum of the eclipse in TDB (approximate).
	 */
	public double eclipseDate;

	/**
	 * ID constant for an invalid result (not available).
	 */
	public static final int INVALID_SAROS_RESULT = -1;

	/**
	 * Saros series static data, provisional status.
	 */
	private static double sarosData[] = new double[]
	{

		// Julian day of the begining and ending of saros series
		// MOON: saros series, initial jd, final jd, status (0 => bad begining, 1 => OK, 2 => bad ending)
		// Data from F. Spenak, see http://eclipse.gsfc.nasa.gov/LEsaros/LEsaroscat.html, which has always flag OK :)
		1,782437.5,1256579.5,1,
		2,799593.5,1273737.5,1,
		3,783824.5,1277723.5,1,
		4,754884.5,1308051.5,1,
		5,824724.5,1325209.5,1,
		6,762857.5,1322610.5,1,
		7,773430.5,1352937.5,1,
		8,810343.5,1370095.5,1,
		9,807743.5,1295058.5,1,
		10,824901.5,1305629.5,1,
		11,855229.5,1335957.5,1,
		12,859215.5,1333359.5,1,
		13,876373.5,1350515.5,1,
		14,906701.5,1380844.5,1,
		15,910687.5,1384831.5,1,
		16,927845.5,1401987.5,1,
		17,958173.5,1425731.5,1,
		18,962159.5,1436303.5,1,
		19,979317.5,1453459.5,1,
		20,1009645.5,1477202.5,1,
		21,1007046.5,1487775.5,1,
		22,1017618.5,1498346.5,1,
		23,1054531.5,1528674.5,1,
		24,979493.5,1532661.5,1,
		25,976895.5,1543232.5,1,
		26,1020394.5,1573561.5,1,
		27,1017794.5,1570962.5,1,
		28,1028367.5,1509095.5,1,
		29,1058695.5,1598691.5,1,
		30,1062681.5,1543410.5,1,
		31,1073253.5,1547396.5,1,
		32,1110167.5,1584309.5,1,
		33,1114153.5,1588297.5,1,
		34,1131311.5,1598868.5,1,
		35,1161639.5,1629196.5,1,
		36,1165625.5,1639768.5,1,
		37,1176197.5,1643754.5,1,
		38,1213111.5,1680668.5,1,
		39,1217097.5,1691240.5,1,
		40,1221084.5,1695226.5,1,
		41,1257997.5,1732140.5,1,
		42,1255398.5,1736127.5,1,
		43,1186946.5,1740113.5,1,
		44,1283128.5,1777026.5,1,
		45,1227845.5,1781013.5,1,
		46,1225247.5,1719146.5,1,
		47,1255575.5,1815327.5,1,
		48,1272732.5,1760046.5,1,
		49,1276719.5,1750862.5,1,
		50,1307047.5,1781190.5,1,
		51,1317619.5,1791762.5,1,
		52,1328191.5,1795748.5,1,
		53,1358519.5,1826076.5,1,
		54,1375676.5,1843234.5,1,
		55,1379663.5,1847220.5,1,
		56,1409991.5,1877548.5,1,
		57,1420562.5,1894706.5,1,
		58,1424549.5,1898692.5,1,
		59,1461463.5,1922435.5,1,
		60,1465449.5,1939593.5,1,
		61,1436509.5,1943579.5,1,
		62,1493179.5,1973906.5,1,
		63,1457653.5,1991064.5,1,
		64,1435298.5,1981880.5,1,
		65,1452456.5,2012208.5,1,
		66,1476198.5,2022780.5,1,
		67,1480184.5,1954328.5,1,
		68,1503928.5,1971485.5,1,
		69,1527670.5,2001813.5,1,
		70,1531656.5,1999214.5,1,
		71,1548814.5,2016371.5,1,
		72,1579142.5,2046700.5,1,
		73,1583128.5,2050686.5,1,
		74,1600286.5,2067843.5,1,
		75,1624028.5,2091586.5,1,
		76,1628015.5,2102158.5,1,
		77,1651758.5,2119315.5,1,
		78,1675500.5,2143058.5,1,
		79,1672901.5,2147045.5,1,
		80,1683474.5,2164201.5,1,
		81,1713801.5,2194530.5,1,
		82,1645349.5,2191931.5,1,
		83,1649336.5,2195917.5,1,
		84,1686249.5,2232831.5,1,
		85,1683650.5,2177550.5,1,
		86,1694222.5,2168365.5,1,
		87,1731136.5,2205279.5,1,
		88,1735122.5,2202680.5,1,
		89,1745694.5,2213252.5,1,
		90,1776022.5,2243580.5,1,
		91,1786594.5,2254152.5,1,
		92,1797166.5,2258138.5,1,
		93,1827494.5,2288466.5,1,
		94,1838066.5,2299039.5,1,
		95,1848638.5,2309610.5,1,
		96,1878966.5,2339938.5,1,
		97,1882952.5,2350511.5,1,
		98,1880354.5,2361082.5,1,
		99,1923853.5,2391410.5,1,
		100,1881741.5,2395397.5,1,
		101,1852801.5,2392798.5,1,
		102,1889715.5,2436297.5,1,
		103,1893701.5,2427113.5,1,
		104,1897688.5,2365246.5,1,
		105,1928016.5,2402159.5,1,
		106,1938588.5,2412731.5,1,
		107,1942575.5,2410132.5,1,
		108,1972903.5,2440460.5,1,
		109,1990059.5,2451033.5,1,
		110,1994046.5,2461604.5,1,
		111,2024375.5,2485347.5,1,
		112,2034946.5,2502505.5,1,
		113,2045518.5,2506491.5,1,
		114,2075847.5,2536818.5,1,
		115,2086418.5,2553976.5,1,
		116,2083820.5,2557963.5,1,
		117,2120733.5,2581705.5,1,
		118,2124719.5,2598863.5,1,
		119,2062852.5,2596264.5,1,
		120,2086596.5,2626592.5,1,
		121,2103752.5,2637164.5,1,
		122,2094568.5,2575297.5,1,
		123,2118311.5,2585868.5,1,
		124,2142054.5,2616197.5,1,
		125,2146040.5,2613598.5,1,
		126,2169783.5,2624170.5,1,
		127,2186940.5,2654498.5,1,
		128,2197512.5,2658485.5,1,
		129,2214670.5,2675642.5,1,
		130,2238412.5,2699385.5,1,
		131,2242398.5,2709957.5,1,
		132,2266142.5,2727113.5,1,
		133,2289884.5,2750857.5,1,
		134,2287285.5,2754843.5,1,
		135,2311028.5,2772000.5,1,
		136,2334770.5,2802329.5,1,
		137,2292659.5,2799730.5,1,
		138,2276890.5,2810301.5,1,
		139,2326974.5,2840630.5,1,
		140,2304619.5,2805104.5,1,
		141,2308606.5,2776164.5,1,
		142,2345520.5,2819663.5,1,
		143,2349506.5,2817064.5,1,
		144,2360078.5,2821050.5,1,
		145,2390406.5,2851378.5,1,
		146,2394392.5,2861951.5,1,
		147,2411550.5,2865937.5,1,
		148,2441878.5,2896265.5,1,
		149,2445864.5,2906837.5,1,
		150,2456437.5,2917409.5,1,
		151,2486765.5,2947737.5,1,
		152,2490751.5,2958309.5,1,
		153,2501323.5,2962295.5,1,
		154,2538236.5,2999209.5,1,
		155,2529052.5,3003196.5,1,
		156,2473771.5,3000597.5,1,
		157,2563367.5,3037510.5,1,
		158,2508085.5,3034912.5,1,
		159,2505486.5,2979630.5,1,
		160,2542400.5,3009957.5,1,
		161,2546386.5,3020530.5,1,
		162,2556958.5,3017931.5,1,
		163,2587287.5,3041673.5,1,
		164,2597858.5,3058831.5,1,
		165,2601845.5,3062818.5,1,
		166,2632173.5,3086560.5,1,
		167,2649330.5,3110303.5,1,
		168,2653317.5,3114289.5,1,
		169,2683645.5,3138032.5,1,
		170,2694217.5,3155190.5,1,
		171,2698203.5,3159176.5,1,
		172,2728532.5,3182918.5,1,
		173,2739103.5,3206662.5,1,
		174,2683822.5,3197477.5,1,
		175,2740492.5,3221219.5,1,
		176,2724722.5,3238377.5,1,
		177,2708952.5,3183096.5,1,
		178,2732695.5,3187082.5,1,
		179,2749852.5,3223996.5,1,
		180,2753839.5,3214812.5,1,
		0,0,0,0,

		// SUN: saros series, initial jd, final jd, status (0 => bad begining, 1 => OK, 2 => bad ending)
		// Data from F. Spenak, see http://eclipse.gsfc.nasa.gov/SEsaros/SEsaros0-180.html
		0,641886.5,1109443.5,1,
		1,672214.5,1139771.5,1,
		2,676200.5,1150343.5,1,
		3,693357.5,1160915.5,1,
		4,723685.5,1191243.5,1,
		5,727671.5,1201815.5,1,
		6,744829.5,1212386.5,1,
		7,775157.5,1242715.5,1,
		8,779143.5,1253287.5,1,
		9,783131.5,1263858.5,1,
		10,820044.5,1294187.5,1,
		11,810859.5,1304759.5,1,
		12,748993.5,1308745.5,1,
		13,792492.5,1345658.5,1,
		14,789892.5,1343060.5,1,
		15,787294.5,1274607.5,1,
		16,824207.5,1377374.5,1,
		17,834779.5,1315508.5,1,
		18,838766.5,1312909.5,1,
		19,869094.5,1343236.5,1,
		20,886251.5,1353809.5,1,
		21,890238.5,1357795.5,1,
		22,927151.5,1388123.5,1,
		23,937722.5,1405281.5,1,
		24,941709.5,1409267.5,1,
		25,978623.5,1439595.5,1,
		26,989194.5,1456753.5,1,
		27,993181.5,1460739.5,1,
		28,1023510.5,1491067.5,1,
		29,1034081.5,1508225.5,1,
		30,972214.5,1512211.5,1,
		31,1061811.5,1542539.5,1,
		32,1006529.5,1553111.5,1,
		33,997345.5,1543927.5,1,
		34,1021088.5,1580840.5,1,
		35,1038245.5,1584827.5,1,
		36,1042231.5,1516374.5,1,
		37,1065974.5,1540117.5,1,
		38,1089716.5,1563860.5,1,
		39,1093703.5,1561261.5,1,
		40,1117446.5,1585003.5,1,
		41,1141188.5,1608746.5,1,
		42,1145175.5,1612733.5,1,
		43,1168918.5,1636475.5,1,
		44,1192660.5,1660218.5,1,
		45,1196647.5,1664205.5,1,
		46,1220390.5,1687947.5,1,
		47,1244132.5,1711690.5,1,
		48,1234948.5,1715677.5,1,
		49,1265277.5,1732833.5,1,
		50,1282433.5,1756577.5,1,
		51,1207395.5,1760563.5,1,
		52,1217968.5,1777720.5,1,
		53,1254881.5,1801463.5,1,
		54,1252282.5,1733011.5,1,
		55,1262855.5,1736997.5,1,
		56,1293182.5,1773911.5,1,
		57,1297169.5,1771312.5,1,
		58,1314326.5,1781883.5,1,
		59,1344654.5,1812212.5,1,
		60,1348640.5,1816199.5,1,
		61,1365798.5,1826770.5,1,
		62,1396126.5,1857098.5,1,
		63,1400112.5,1867671.5,1,
		64,1417270.5,1878242.5,1,
		65,1447598.5,1908570.5,1,
		66,1444999.5,1919143.5,1,
		67,1462157.5,1929714.5,1,
		68,1492485.5,1960042.5,1,
		69,1456959.5,1964029.5,1,
		70,1421434.5,1968015.5,1,
		71,1471518.5,2004929.5,1,
		72,1455748.5,1995745.5,1,
		73,1466320.5,1933878.5,1,
		74,1496648.5,1983962.5,1,
		75,1500634.5,1974778.5,1,
		76,1511207.5,1978764.5,1,
		77,1548120.5,2009092.5,1,
		78,1552106.5,2019665.5,1,
		79,1562679.5,2023651.5,1,
		80,1599592.5,2060564.5,1,
		81,1603578.5,2071136.5,1,
		82,1614150.5,2075123.5,1,
		83,1644479.5,2105450.5,1,
		84,1655050.5,2122608.5,1,
		85,1659037.5,2126594.5,1,
		86,1695950.5,2156922.5,1,
		87,1693351.5,2167495.5,1,
		88,1631484.5,2171481.5,1,
		89,1727666.5,2201809.5,1,
		90,1672384.5,2212381.5,1,
		91,1663200.5,2150514.5,1,
		92,1693529.5,2174256.5,1,
		93,1710685.5,2191414.5,1,
		94,1714672.5,2182230.5,1,
		95,1738415.5,2199387.5,1,
		96,1755572.5,2223130.5,1,
		97,1766144.5,2227117.5,1,
		98,1789887.5,2250859.5,1,
		99,1807044.5,2274602.5,1,
		100,1817616.5,2278589.5,1,
		101,1841359.5,2302331.5,1,
		102,1858516.5,2319489.5,1,
		103,1862502.5,2330060.5,1,
		104,1892831.5,2347217.5,1,
		105,1903402.5,2370961.5,1,
		106,1887633.5,2374947.5,1,
		107,1924547.5,2392104.5,1,
		108,1921948.5,2415847.5,1,
		109,1873251.5,2400078.5,1,
		110,1890409.5,2357966.5,1,
		111,1914151.5,2427807.5,1,
		112,1918138.5,2385696.5,1,
		113,1935296.5,2396267.5,1,
		114,1959038.5,2426596.5,1,
		115,1963024.5,2430583.5,1,
		116,1986767.5,2441154.5,1,
		117,2010510.5,2471482.5,1,
		118,2014496.5,2482055.5,1,
		119,2031654.5,2492626.5,1,
		120,2061982.5,2522954.5,1,
		121,2065968.5,2526941.5,1,
		122,2083126.5,2537512.5,1,
		123,2113454.5,2567841.5,1,
		124,2104269.5,2578413.5,1,
		125,2108256.5,2582399.5,1,
		126,2151755.5,2619313.5,1,
		127,2083302.5,2616714.5,1,
		128,2080704.5,2554847.5,1,
		129,2124203.5,2644443.5,1,
		130,2121603.5,2595747.5,1,
		131,2132176.5,2586563.5,1,
		132,2162504.5,2623476.5,1,
		133,2166490.5,2634048.5,1,
		134,2177062.5,2638035.5,1,
		135,2207390.5,2668362.5,1,
		136,2217962.5,2678935.5,1,
		137,2228534.5,2682921.5,1,
		138,2258862.5,2713249.5,1,
		139,2269434.5,2730407.5,1,
		140,2273421.5,2734393.5,1,
		141,2310334.5,2764721.5,1,
		142,2314320.5,2781879.5,1,
		143,2311722.5,2779280.5,1,
		144,2355221.5,2809607.5,1,
		145,2319695.5,2820180.5,1,
		146,2284169.5,2778069.5,1,
		147,2314498.5,2834738.5,1,
		148,2325069.5,2812384.5,1,
		149,2329056.5,2790029.5,1,
		150,2352799.5,2813771.5,1,
		151,2369956.5,2837514.5,1,
		152,2380528.5,2834915.5,1,
		153,2404271.5,2858657.5,1,
		154,2421428.5,2882401.5,1,
		155,2425414.5,2886387.5,1,
		156,2455743.5,2903544.5,1,
		157,2472900.5,2927287.5,1,
		158,2476886.5,2931274.5,1,
		159,2500629.5,2955016.5,1,
		160,2517786.5,2978759.5,1,
		161,2515187.5,2982746.5,1,
		162,2545516.5,2999902.5,1,
		163,2556087.5,3023646.5,1,
		164,2487635.5,3007876.5,1,
		165,2504793.5,2972350.5,1,
		166,2535121.5,3035605.5,1,
		167,2525936.5,2993495.5,1,
		168,2543094.5,2997481.5,1,
		169,2573422.5,3034394.5,1,
		170,2577408.5,3038381.5,1,
		171,2594566.5,3042367.5,1,
		172,2624894.5,3079281.5,1,
		173,2628880.5,3083268.5,1,
		174,2646038.5,3093839.5,1,
		175,2669780.5,3124167.5,1,
		176,2673766.5,3134740.5,1,
		177,2690924.5,3138726.5,1,
		178,2721252.5,3175639.5,1,
		179,2718653.5,3179626.5,1,
		180,2729226.5,3183612.5,1,
		0,0,0,0
	};
}
