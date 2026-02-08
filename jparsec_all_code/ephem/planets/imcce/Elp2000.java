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
package jparsec.ephem.planets.imcce;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.zip.GZIPInputStream;
import jparsec.astronomy.CoordinateSystem;
import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.EphemerisElement.FRAME;
import jparsec.ephem.Functions;
import jparsec.ephem.IAU2006;
import jparsec.ephem.Nutation;
import jparsec.ephem.PhysicalParameters;
import jparsec.ephem.Precession;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.planets.EphemElement;
import jparsec.ephem.planets.JPLEphemeris;
import jparsec.ephem.planets.PlanetEphem;
import jparsec.math.Constant;
import jparsec.math.matrix.Matrix;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.SiderealTime;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.time.TimeScale;
import jparsec.util.Configuration;
import jparsec.util.DataBase;
import jparsec.util.JPARSECException;

/**
 * This class implements the Lunar Solution ELP2000 from the IMCCE. The entire
 * theory is applied. <BR>
 * Reference frame is mean dynamical ecliptic and inertial equinox of J2000
 * epoch.<BR>
 * References: <BR>
 * 1. <I>ELP 2000-85: a semi-analytical lunar ephemeris adequate for historical
 * times</I>, Chapront-Touze M., Chapront J., Astron. &amp; Astrophys. 190, 342
 * (1988).
 * <BR>
 * 2. <I>The Lunar Ephemeris ELP 2000</I>, Chapront-Touze M., Chapront J.,
 * Astron. &amp; Astrophys. 124, 50 (1983).
 *
 * @see Ephem
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Elp2000
{
    private final static Elp2000_data data = (Elp2000_data) deserialise("elp2000_data.ser.gz");

    private static Object deserialise (final String fileName) {
        try {
            InputStream is = Elp2000.class.getClassLoader().getResourceAsStream("jparsec/ephem/planets/imcce/" + fileName);
            BufferedInputStream bis = new BufferedInputStream (is);
            GZIPInputStream gis = new GZIPInputStream(bis);
            ObjectInputStream ois = new ObjectInputStream(gis);
            Object result = ois.readObject();

            ois.close();
            gis.close();
            bis.close();
            is.close();

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Could not deserialise " + fileName);
        }
    }

    // private constructor so that this class cannot be instantiated.
    private Elp2000() {}

    /**
     * Computation of geocentric lunar coordinates from ELP 2000-82 and
     * ELP2000-85 theories (M. Chapront-Touze and J. Chapront). Constants fitted
     * to JPL's ephemerides DE200/LE200.
     * <BR>
     * Reference frame in mean dynamical ecliptic and inertial equinox of J2000
     * epoch.
     * <BR>
     * Files, series, constants and coordinate systems are described in the
     * notice LUNAR SOLUTION ELP 2000-82B, available from the IMCCE.
     *
     * @param JD Julian Day in TDB.
     * @return An array with the geocentric rectangular positions. Units are AU.
     */
    public static double[] calc(final double JD)
    {
        double cpi, cpi2, pis2, rad, deg, c1, c2, ath, a0, am, alfa, dtasm;
        double Zw[][] = new double[6][6];
        double eart[] = new double[6];
        double peri[] = new double[6];
        double ZZp[][] = new double[9][3];
        double delnu, dele, delg, delnp, delep;
        double del[][] = new double[5][6];
        double zeta[] = new double[3];
        double p1, p2, p3, p4, p5, q1, q2, q3, q4, q5, preces;
        double ZZt[] = new double[6];
        double pre[] = new double[4];
        double ZZr[] = new double[4];
        int ific;

        // Main constants
        cpi = Math.PI;
        cpi2 = 2.0 * cpi;
        pis2 = cpi / 2.0;
        rad = 648000.0 / cpi;
        deg = cpi / 180.0;
        c1 = 60.0;
        c2 = 3600.0;
        ath = 384747.9806743165;
        a0 = 384747.9806448954;
        am = 0.074801329518;
        alfa = 0.002571881335;
        dtasm = 2.0 * alfa / (3.0 * am);

        // Lunar arguments
        Zw[1][1] = (218 + 18 / c1 + 59.95571 / c2) * deg;
        Zw[2][1] = (83 + 21 / c1 + 11.67475 / c2) * deg;
        Zw[3][1] = (125 + 2 / c1 + 40.39816 / c2) * deg;
        eart[1] = (100 + 27 / c1 + 59.22059 / c2) * deg;
        peri[1] = (102 + 56 / c1 + 14.42753 / c2) * deg;
        Zw[1][2] = 1732559343.73604 / rad;
        Zw[2][2] = 14643420.2632 / rad;
        Zw[3][2] = -6967919.3622 / rad;
        eart[2] = 129597742.2758 / rad;
        peri[2] = 1161.2283 / rad;
        Zw[1][3] = -5.8883 / rad;
        Zw[2][3] = -38.2776 / rad;
        Zw[3][3] = 6.3622 / rad;
        eart[3] = -0.0202 / rad;
        peri[3] = 0.5327 / rad;
        Zw[1][4] = 0.6604e-2 / rad;
        Zw[2][4] = -0.45047e-1 / rad;
        Zw[3][4] = 0.7625e-2 / rad;
        eart[4] = 0.9e-5 / rad;
        peri[4] = -0.138e-3 / rad;
        Zw[1][5] = -0.3169e-4 / rad;
        Zw[2][5] = 0.21301e-3 / rad;
        Zw[3][5] = -0.3586e-4 / rad;
        eart[5] = 0.15e-6 / rad;
        peri[5] = 0.0;

        // Precession constant
        preces = 5029.0966 / rad;

        // Planetary arguments
        ZZp[1][1] = (252 + 15 / c1 + 3.25986 / c2) * deg;
        ZZp[2][1] = (181 + 58 / c1 + 47.28305 / c2) * deg;
        ZZp[3][1] = eart[1];
        ZZp[4][1] = (355 + 25 / c1 + 59.78866 / c2) * deg;
        ZZp[5][1] = (34 + 21 / c1 + 5.34212 / c2) * deg;
        ZZp[6][1] = (50 + 4 / c1 + 38.89694 / c2) * deg;
        ZZp[7][1] = (314 + 3 / c1 + 18.01841 / c2) * deg;
        ZZp[8][1] = (304 + 20 / c1 + 55.19575 / c2) * deg;
        ZZp[1][2] = 538101628.68898 / rad;
        ZZp[2][2] = 210664136.43355 / rad;
        ZZp[3][2] = eart[2];
        ZZp[4][2] = 68905077.59284 / rad;
        ZZp[5][2] = 10925660.42861 / rad;
        ZZp[6][2] = 4399609.65932 / rad;
        ZZp[7][2] = 1542481.19393 / rad;
        ZZp[8][2] = 786550.32074 / rad;

        // Corrections of the constants (fit to DE200/LE200)
        delnu = +0.55604 / rad / Zw[1][2];
        dele = +0.01789 / rad;
        delg = -0.08066 / rad;
        delnp = -0.06424 / rad / Zw[1][2];
        delep = -0.12879 / rad;

        // Delaunay's arguments
        for (int i = 1; i <= 5; i++)
        {
            del[1][i] = Zw[1][i] - eart[i];
            del[4][i] = Zw[1][i] - Zw[3][i];
            del[3][i] = Zw[1][i] - Zw[2][i];
            del[2][i] = eart[i] - peri[i];
        }
        del[1][1] = del[1][1] + cpi;
        zeta[1] = Zw[1][1];
        zeta[2] = Zw[1][2] + preces;

        // Precession matrix
        p1 = 0.10180391e-4;
        p2 = 0.47020439e-6;
        p3 = -0.5417367e-9;
        p4 = -0.2507948e-11;
        p5 = 0.463486e-14;
        q1 = -0.113469002e-3;
        q2 = 0.12372674e-6;
        q3 = 0.1265417e-8;
        q4 = -0.1371808e-11;
        q5 = -0.320334e-14;

        ZZt[1] = 1.0;
        ZZt[2] = Functions.toCenturies(JD);
        ZZt[3] = ZZt[2] * ZZt[2];
        ZZt[4] = ZZt[3] * ZZt[2];
        ZZt[5] = ZZt[4] * ZZt[2];

        pre[1] = ELP_TRUNCATION - 1.0e-12;
        pre[2] = ELP_TRUNCATION - 1.0e-12;
        pre[3] = ELP_TRUNCATION * ath / rad;

        // Calculate element
        for (ific = 1; ific <= 36; ific++)
        {
            Elp2000Set1 elp0[];
            Elp2000Set1 elp1[];
            Elp2000Set1 elp2[];
            Elp2000Set2 elp3[];
            Elp2000Set3 elp4[];

            int iv = (int) Functions.module(ific - 1.0, 3.0) + 1;
            switch (ific)
            {
            case 1:
                elp0 = data.elp_lon_sine_0_LonSine0;
                ZZr[iv] += calcELEM1(elp0, ific, iv, am, pis2, cpi2, dtasm, delnp, delnu, dele, delep, delg, ZZt, del, pre);
                elp1 = data.elp_lon_sine_1_LonSine1;
                ZZr[iv] += calcELEM1(elp1, ific, iv, am, pis2, cpi2, dtasm, delnp, delnu, dele, delep, delg, ZZt, del, pre);
                elp2 = data.elp_lon_sine_2_LonSine2;
                ZZr[iv] += calcELEM1(elp2, ific, iv, am, pis2, cpi2, dtasm, delnp, delnu, dele, delep, delg, ZZt, del, pre);
                break;
            case 2:
                elp0 = data.elp_lat_sine_0_LatSine0;
                ZZr[iv] += calcELEM1(elp0, ific, iv, am, pis2, cpi2, dtasm, delnp, delnu, dele, delep, delg, ZZt, del, pre);
                elp1 = data.elp_lat_sine_1_LatSine1;
                ZZr[iv] += calcELEM1(elp1, ific, iv, am, pis2, cpi2, dtasm, delnp, delnu, dele, delep, delg, ZZt, del, pre);
                elp2 = data.elp_lat_sine_2_LatSine2;
                ZZr[iv] += calcELEM1(elp2, ific, iv, am, pis2, cpi2, dtasm, delnp, delnu, dele, delep, delg, ZZt, del, pre);
                break;
            case 3:
                elp0 = data.elp_rad_cose_0_RadCose0;
                ZZr[iv] += calcELEM1(elp0, ific, iv, am, pis2, cpi2, dtasm, delnp, delnu, dele, delep, delg, ZZt, del, pre);
                elp1 = data.elp_rad_cose_1_RadCose1;
                ZZr[iv] += calcELEM1(elp1, ific, iv, am, pis2, cpi2, dtasm, delnp, delnu, dele, delep, delg, ZZt, del, pre);
                break;
            case 4:
                elp3 = data.elp_lon_earth_perturb_Lon;
                ZZr[iv] += calcELEM2(elp3, ific, iv, cpi2, deg, zeta, ZZt, del, pre);
                break;
            case 5:
                elp3 = data.elp_lat_earth_perturb_Lat;
                ZZr[iv] += calcELEM2(elp3, ific, iv, cpi2, deg, zeta, ZZt, del, pre);
                break;
            case 6:
                elp3 = data.elp_rad_earth_perturb_Rad;
                ZZr[iv] += calcELEM2(elp3, ific, iv, cpi2, deg, zeta, ZZt, del, pre);
                break;
            case 7:
                elp3 = data.elp_earth_perturb_t_Lon;
                ZZr[iv] += calcELEM2(elp3, ific, iv, cpi2, deg, zeta, ZZt, del, pre);
                break;
            case 8:
                elp3 = data.elp_earth_perturb_t_Lat;
                ZZr[iv] += calcELEM2(elp3, ific, iv, cpi2, deg, zeta, ZZt, del, pre);
                break;
            case 9:
                elp3 = data.elp_earth_perturb_t_Rad;
                ZZr[iv] += calcELEM2(elp3, ific, iv, cpi2, deg, zeta, ZZt, del, pre);
                break;
            case 10:
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_0_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_1_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_2_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_3_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_4_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_5_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_6_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_7_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_8_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_9_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_10_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_11_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_12_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_13_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_14_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_15_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_16_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_17_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_18_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_19_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_20_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_21_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_22_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_23_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_24_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_25_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_26_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_27_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_28_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_29_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_30_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_31_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_32_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_33_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_34_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb10_35_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                break;
            case 11:
                ZZr[iv] += calcELEM3(data.elp_plan_perturb11_0_Lat, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb11_1_Lat, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb11_2_Lat, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb11_3_Lat, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb11_4_Lat, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb11_5_Lat, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb11_6_Lat, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb11_7_Lat, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb11_8_Lat, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb11_9_Lat, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb11_10_Lat, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb11_11_Lat, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb11_12_Lat, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb11_13_Lat, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                break;
            case 12:
                ZZr[iv] += calcELEM3(data.elp_plan_perturb12_0_Rad, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb12_1_Rad, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb12_2_Rad, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb12_3_Rad, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb12_4_Rad, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb12_5_Rad, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb12_6_Rad, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb12_7_Rad, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb12_8_Rad, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb12_9_Rad, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb12_10_Rad, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb12_11_Rad, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb12_12_Rad, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb12_13_Rad, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb12_14_Rad, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb12_15_Rad, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb12_16_Rad, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                break;
            case 13:
                ZZr[iv] += calcELEM3(data.elp_plan_perturb13_0_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb13_1_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb13_2_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb13_3_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb13_4_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb13_5_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb13_6_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb13_7_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb13_8_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb13_9_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb13_10_Lon, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                break;
            case 14:
                ZZr[iv] += calcELEM3(data.elp_plan_perturb14_0_Lat, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb14_1_Lat, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb14_2_Lat, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                break;
            case 15:
                ZZr[iv] += calcELEM3(data.elp_plan_perturb15_0_Rad, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb15_1_Rad, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb15_2_Rad, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb15_3_Rad, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                ZZr[iv] += calcELEM3(data.elp_plan_perturb15_4_Rad, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                break;
            case 16:
                elp4 = data.elp_plan_perturb2_Lon;
                ZZr[iv] += calcELEM3(elp4, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                break;
            case 17:
                elp4 = data.elp_plan_perturb2_Lat;
                ZZr[iv] += calcELEM3(elp4, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                break;
            case 18:
                elp4 = data.elp_plan_perturb2_Rad;
                ZZr[iv] += calcELEM3(elp4, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                break;
            case 19:
                elp4 = data.elp_plan_perturb2_Lon_t;
                ZZr[iv] += calcELEM3(elp4, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                break;
            case 20:
                elp4 = data.elp_plan_perturb2_Lat_t;
                ZZr[iv] += calcELEM3(elp4, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                break;
            case 21:
                elp4 = data.elp_plan_perturb2_Rad_t;
                ZZr[iv] += calcELEM3(elp4, ific, iv, cpi2, deg, ZZp, ZZt, del, pre);
                break;
            case 22:
                elp3 = data.elp_tidal_Lon;
                ZZr[iv] += calcELEM2(elp3, ific, iv, cpi2, deg, zeta, ZZt, del, pre);
                break;
            case 23:
                elp3 = data.elp_tidal_Lat;
                ZZr[iv] += calcELEM2(elp3, ific, iv, cpi2, deg, zeta, ZZt, del, pre);
                break;
            case 24:
                elp3 = data.elp_tidal_Rad;
                ZZr[iv] += calcELEM2(elp3, ific, iv, cpi2, deg, zeta, ZZt, del, pre);
                break;
            case 25:
                elp3 = data.elp_tidal_Lon_t;
                ZZr[iv] += calcELEM2(elp3, ific, iv, cpi2, deg, zeta, ZZt, del, pre);
                break;
            case 26:
                elp3 = data.elp_tidal_Lat_t;
                ZZr[iv] += calcELEM2(elp3, ific, iv, cpi2, deg, zeta, ZZt, del, pre);
                break;
            case 27:
                elp3 = data.elp_tidal_Rad_t;
                ZZr[iv] += calcELEM2(elp3, ific, iv, cpi2, deg, zeta, ZZt, del, pre);
                break;
            case 28:
                elp3 = data.elp_moon_Lon;
                ZZr[iv] += calcELEM2(elp3, ific, iv, cpi2, deg, zeta, ZZt, del, pre);
                break;
            case 29:
                elp3 = data.elp_moon_Lat;
                ZZr[iv] += calcELEM2(elp3, ific, iv, cpi2, deg, zeta, ZZt, del, pre);
                break;
            case 30:
                elp3 = data.elp_moon_Rad;
                ZZr[iv] += calcELEM2(elp3, ific, iv, cpi2, deg, zeta, ZZt, del, pre);
                break;
            case 31:
                elp3 = data.elp_rel_Lon;
                ZZr[iv] += calcELEM2(elp3, ific, iv, cpi2, deg, zeta, ZZt, del, pre);
                break;
            case 32:
                elp3 = data.elp_rel_Lat;
                ZZr[iv] += calcELEM2(elp3, ific, iv, cpi2, deg, zeta, ZZt, del, pre);
                break;
            case 33:
                elp3 = data.elp_rel_Rad;
                ZZr[iv] += calcELEM2(elp3, ific, iv, cpi2, deg, zeta, ZZt, del, pre);
                break;
            case 34:
                elp3 = data.elp_plan_Lon;
                ZZr[iv] += calcELEM2(elp3, ific, iv, cpi2, deg, zeta, ZZt, del, pre);
                break;
            case 35:
                elp3 = data.elp_plan_Lat;
                ZZr[iv] += calcELEM2(elp3, ific, iv, cpi2, deg, zeta, ZZt, del, pre);
                break;
            case 36:
                elp3 = data.elp_plan_Rad;
                ZZr[iv] += calcELEM2(elp3, ific, iv, cpi2, deg, zeta, ZZt, del, pre);
                break;
            }
        }

        ZZr[1] = ZZr[1] / rad + Zw[1][1] + Zw[1][2] * ZZt[2] + Zw[1][3] * ZZt[3] + Zw[1][4] * ZZt[4] + Zw[1][5] * ZZt[5];
        ZZr[2] = ZZr[2] / rad;
        ZZr[3] = ZZr[3] * a0 / ath;

        double x1 = ZZr[3] * Math.cos(ZZr[2]);
        double x2 = x1 * Math.sin(ZZr[1]);
        x1 = x1 * Math.cos(ZZr[1]);
        double x3 = ZZr[3] * Math.sin(ZZr[2]);

        double pw = (p1 + p2 * ZZt[2] + p3 * ZZt[3] + p4 * ZZt[4] + p5 * ZZt[5]) * ZZt[2];
        double qw = (q1 + q2 * ZZt[2] + q3 * ZZt[3] + q4 * ZZt[4] + q5 * ZZt[5]) * ZZt[2];
        double rra = 2.0 * Math.sqrt(1.0 - pw * pw - qw * qw);
        double pwqw = 2.0 * pw * qw;
        double pw2 = 1.0 - 2.0 * pw * pw;
        double qw2 = 1.0 - 2.0 * qw * qw;
        pw = pw * rra;
        qw = qw * rra;

        ZZr[1] = pw2 * x1 + pwqw * x2 + pw * x3;
        ZZr[2] = pwqw * x1 + qw2 * x2 - qw * x3;
        ZZr[3] = -pw * x1 + qw * x2 + (pw2 + qw2 - 1.0) * x3;

        return new double[] { ZZr[1] / Constant.AU, ZZr[2] / Constant.AU, ZZr[3] / Constant.AU };
    }

    // Main problem, files 1-3
    private static double calcELEM1(Elp2000Set1[] elp, int ific, int iv, double am, double pis2, double cpi2,
            double dtasm, double delnp, double delnu, double dele, double delep, double delg, double[] ZZt,
            double[][] del, double[] pre)
    {
        double tgv, Zx, Zy, Zr = 0.0;

        for (int i = 0; i < elp.length; i++)
        {
            if (Math.abs(elp[i].COEF[0]) > pre[iv])
            {
                tgv = elp[i].COEF[1] + dtasm * elp[i].COEF[5];
                Zx = elp[i].COEF[0] + tgv * (delnp - am * delnu) + elp[i].COEF[2] * delg + elp[i].COEF[3] * dele + elp[i].COEF[4] * delep;
                Zy = 0.0;

                if (ific == 3)
                    Zx -= 2.0 * elp[i].COEF[0] * delnu / 3.0;

                for (int k = 1; k <= 5; k++)
                {
                    for (int j = 1; j <= 4; j++)
                    {
                        Zy = Zy + elp[i].ILU[j - 1] * del[j][k] * ZZt[k];
                    }
                }

                if (iv == 3)
                    Zy += pis2;

                Zy = Functions.module(Zy, cpi2);

                Zr += Zx * Math.sin(Zy);
            }
        }

        return Zr;
    }

    // Figures - Tides - Relativity - Solar eccentricity, files 4-9, 22-36
    private static double calcELEM2(Elp2000Set2[] elp, int ific, int iv, double cpi2, double deg, double[] zeta,
            double[] ZZt, double[][] del, double[] pre)
    {
        double Zx = 0.0, Zy = 0.0, Zr = 0.0;

        for (int i = 0; i < elp.length; i++)
        {
            if (elp[i].COEF[1] > pre[iv])
            {
                Zx = elp[i].COEF[1];
                if (ific >= 7 && ific <= 9)
                    Zx = Zx * ZZt[2];
                if (ific >= 25 && ific <= 27)
                    Zx = Zx * ZZt[2];
                if (ific >= 34 && ific <= 36)
                    Zx = Zx * ZZt[3];

                Zy = elp[i].COEF[0] * deg;
                for (int k = 1; k <= 2; k++)
                {
                    Zy = Zy + elp[i].ILU[0] * zeta[k] * ZZt[k];
                    for (int j = 1; j <= 4; j++)
                    {
                        Zy = Zy + elp[i].ILU[j] * del[j][k] * ZZt[k];
                    }
                }
                Zy = Functions.module(Zy, cpi2);
                Zr += Zx * Math.sin(Zy);
            }
        }

        return Zr;
    }

    // Planetary perturbations, files 10-21
    private static double calcELEM3(Elp2000Set3[] elp, int ific, int iv, double cpi2, double deg, double[][] p,
            double[] ZZt, double[][] del, double[] pre)
    {
        double Zx = 0.0, Zy = 0.0, Zr = 0.0;

        for (int i = 0; i < elp.length; i++)
        {
            if (elp[i].COEF[1] > pre[iv])
            {
                Zx = elp[i].COEF[1];
                if ((ific >= 13 && ific <= 15) || ific >= 19 && ific <= 21)
                    Zx = Zx * ZZt[2];
                Zy = elp[i].COEF[0] * deg;

                for (int k = 1; k <= 2; k++)
                {
                    if (ific < 16)
                    {
                        double z = elp[i].ILU[8] * del[1][k] + elp[i].ILU[9] * del[3][k] + elp[i].ILU[10] * del[4][k];
                        Zy = Zy + z * ZZt[k];
                        for (int j = 1; j <= 8; j++)
                        {
                            Zy = Zy + elp[i].ILU[j - 1] * p[j][k] * ZZt[k];
                        }
                    } else
                    {
                        for (int j = 1; j <= 4; j++)
                        {
                            Zy = Zy + elp[i].ILU[j + 6] * del[j][k] * ZZt[k];
                        }
                        for (int j = 1; j <= 7; j++)
                        {
                            Zy = Zy + elp[i].ILU[j - 1] * p[j][k] * ZZt[k];
                        }
                    }
                }
                Zy = Functions.module(Zy, cpi2);
                Zr += Zx * Math.sin(Zy);
            }
        }

        return Zr;
    }

    /**
     * Transform J2000 mean inertial coordinates into mean equatorial J2000.
     * Specific to this theory (class) to compare positions with DE200.
     *
     * @param position Ecliptic coordinates (x, y, z) or (x, y, z, vx, vy, vz)
     *        refered to mean ecliptic and inertial equinox of J2000.
     * @return Equatorial FK5 coordinates.
     */
    public static double[] meanJ2000InertialToEquatorialFK5(double position[])
    {
        double RotM[][] = new double[4][4];
        double out_pos[] = new double[3];
        double out_vel[] = new double[3];

        RotM[1][1] = 1.000000000000;
        RotM[1][2] = 0.000000437913;
        RotM[1][3] = -0.000000189859;
        RotM[2][1] = -0.000000477299;
        RotM[2][2] = 0.917482137607;
        RotM[2][3] = -0.397776981701;
        RotM[3][1] = 0.000000000000;
        RotM[3][2] = 0.397776981701;
        RotM[3][3] = 0.917482137607;

        // Apply rotation
        out_pos[0] = RotM[1][1] * position[0] + RotM[1][2] * position[1] + RotM[1][3] * position[2]; // x
        out_pos[1] = RotM[2][1] * position[0] + RotM[2][2] * position[1] + RotM[2][3] * position[2]; // y
        out_pos[2] = RotM[3][1] * position[0] + RotM[3][2] * position[1] + RotM[3][3] * position[2]; // z
        if (position.length > 3)
        {
            out_vel[0] = RotM[1][1] * position[3] + RotM[1][2] * position[4] + RotM[1][3] * position[5]; // vx
            out_vel[1] = RotM[2][1] * position[3] + RotM[2][2] * position[4] + RotM[2][3] * position[5]; // vy
            out_vel[2] = RotM[3][1] * position[3] + RotM[3][2] * position[4] + RotM[3][3] * position[5]; // vz

            return new double[]
            { out_pos[0], out_pos[1], out_pos[2], out_vel[0], out_vel[1], out_vel[2] };
        }

        return out_pos;
    }

    /**
     * Corrects Julian day of calculations of ELP2000 theory for secular
     * acceleration of the Moon. This method uses the current value of static
     * variable {@linkplain Constant#MOON_SECULAR_ACCELERATION}.
     * <BR>
     * Correction should be performed to standard dynamical time of calculations
     * (Barycentric Dynamical Time), as obtained by using the corresponding methods.
     * {@linkplain Elp2000#elp2000Ephemeris(TimeElement, ObserverElement, EphemerisElement)}
     * accepts any time scale, so it is possible to use the
     * output Julian day of this method with any time scale, unless a very
     * little error (well below the uncertainty in TT-UT correction) could exist
     * if this correction is applied to LT or UT, before the correction to TDB
     * which is performed in {@linkplain Elp2000#elp2000Ephemeris(TimeElement, ObserverElement, EphemerisElement)}.
     * <BR>
     * Correction for different years (using the default value) are as follows:
     *
     * <pre>
     * Year       Correction (seconds)
     * -2000      -2796
     * -1000      -1561
     *     0      -683
     *  1000      -163
     *  1955       0.000
     *  2000      -0.362
     *  3000      -195
     * </pre>
     *
     * @param jd Julian day in TDB.
     * @return Output (corrected) Julian day in TDB.
     */
    public static double timeCorrectionForSecularAcceleration(double jd)
    {
		if (!Configuration.ENABLE_TIME_CORRECTION_FOR_MOON_SECULAR_ACCELERATION) return jd;
        double cent = (jd - 2435109.0) / Constant.JULIAN_DAYS_PER_CENTURY;
        double deltaT = 0.91072 * (Constant.MOON_SECULAR_ACCELERATION - JPLEphemeris.MOON_SECULAR_ACCELERATION_DE200) * cent * cent;

        return jd + deltaT / Constant.SECONDS_PER_DAY;
    }

    private static double ELP_TRUNCATION = 0;
    
    /**
     * Sets the truncation level for ELP2000 when computing ephemerides with 
     * {@linkplain #elp2000Ephemeris(TimeElement, ObserverElement, EphemerisElement)}. 
     * Default value is 0 for the entire theory.
     * A value of 0.001 produces similar results with much faster computing time.
     * @param l Truncation level in arcseconds. 
     */
    public static void setELPTruncationLevel(double l) {
    	ELP_TRUNCATION = l;
    }
    
    /**
     * Returns the current ELP truncation level.
     * @return Truncation level in arcseconds.
     */
    public static double getELPTruncationLevel() {
    	return ELP_TRUNCATION;
    }
    
    /**
     * Calculate Moon position (center of mass), providing full data. This
     * method uses ELP2000 theory from the IMCCE. Typical error is below 0.01
     * arcseconds when comparing to JPL DE200 Ephemeris. The position error in
     * the original theory reached some arcseconds outside 20th century due to an
     * uncertainty with Moon secular acceleration in JPL DE200 integration. So
     * this theory was not accurate outside period 1900-2100, although it can be
     * improved by correcting for Moon secular acceleration.
     * <BR>
     * This time correction for Moon secular acceleration is automatically done, 
     * which means that this implementation will better match JPL DE405 in ancient 
     * times. So the results cannot be compared directly with DE200 due to this 
     * time correction (well, only around year 1955). Another possible correction 
     * you may want to apply is from center of mass to geometric center by means of
     * {@linkplain Elp2000#fromMoonBarycenterToGeometricCenter(TimeElement, ObserverElement, EphemerisElement, EphemElement)}.
     * <BR>
     * This method also uses Series96 theory for the position of the Earth
     * between 1900 and 2100, and VSOP87A theory outside this interval. This could
     * affect the heliocentric position and physical ephemeris.
     *
     * @param time Time object containing the date.
     * @param obs Observer object containing the observer position.
     * @param eph Ephemeris object with the target and ephemeris
     *        properties.
     * @return Ephem object containing ephemeris data. Rise, set, transit
     * times and maximum elevation fields are not computed in this method, use
     * {@linkplain Ephem} class for that.
     * @throws JPARSECException Thrown if the calculation fails.
     */
    public static EphemElement elp2000Ephemeris(TimeElement time, // Time
                                                                    // Element
            ObserverElement obs, // Observer Element
            EphemerisElement eph) // Ephemeris Element
            throws JPARSECException
    {
        if (eph.targetBody != TARGET.Moon)
            throw new JPARSECException("target object is not the Moon.");

        if (obs.getMotherBody() != TARGET.EARTH) throw new JPARSECException("observer must be on Earth in ELP2000.");

        // Check Ephemeris object
        if (!EphemerisElement.checkEphemeris(eph))
        {
            throw new JPARSECException("invalid ephemeris object.");
        }

        // Obtain julian day in Barycentric Dynamical Time
        double JD_TDB = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
        double JD_TDB_corrected = Elp2000.timeCorrectionForSecularAcceleration(JD_TDB);

        // Obtain geocentric position
        double geo_eq[] = meanJ2000InertialToEquatorialFK5(Elp2000.calc(JD_TDB_corrected));

        // Obtain topocentric light_time
        LocationElement loc = LocationElement.parseRectangularCoordinates(geo_eq);
        double light_time = loc.getRadius() * Constant.LIGHT_TIME_DAYS_PER_AU;
        if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.GEOMETRIC)
            light_time = 0.0;
        if (eph.ephemType != EphemerisElement.COORDINATES_TYPE.GEOMETRIC)
        {
            double topo[] = obs.topocentricObserverICRF(time, eph);
            geo_eq = meanJ2000InertialToEquatorialFK5(Elp2000.calc(JD_TDB_corrected - light_time));
            double light_time_corrected = Ephem.getTopocentricLightTime(geo_eq, topo, eph);
            // Iterate to obtain correct light time and geocentric position.
            // Typical differente in light time is 0.1 seconds for planets.
            // Iterate to a precission up to 0.001 seconds.
            do
            {
                light_time = light_time_corrected;
                geo_eq = meanJ2000InertialToEquatorialFK5(Elp2000.calc(JD_TDB_corrected - light_time));
                light_time_corrected = Ephem.getTopocentricLightTime(geo_eq, topo, eph);
            } while (Math.abs(light_time - light_time_corrected) > (1.0E-6 / Constant.SECONDS_PER_DAY));
            light_time = light_time_corrected;
        }

        // Obtain heliocentric ecliptic coordinates, mean equinox of date
        double earth_0[] = { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
        double earth_ltS[] = { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
        if (JD_TDB > 2341972.5 && JD_TDB < 2488092.5)
        {
            try {
                earth_0 = Series96.getGeocentricPosition(JD_TDB, TARGET.SUN, 0.0, false, obs);
                earth_ltS = Series96.getGeocentricPosition(JD_TDB, TARGET.SUN, Functions.getNorm(earth_0)*Constant.LIGHT_TIME_DAYS_PER_AU, false, obs);
            } catch (Exception exc) {
                earth_0 = PlanetEphem.getGeocentricPosition(JD_TDB, TARGET.SUN, 0.0, false, obs);
                earth_0 = Ephem.eclipticToEquatorial(earth_0, Constant.J2000, eph);
                earth_ltS = PlanetEphem.getGeocentricPosition(JD_TDB, TARGET.SUN, Functions.getNorm(earth_0)*Constant.LIGHT_TIME_DAYS_PER_AU, false, obs);
                earth_ltS = Ephem.eclipticToEquatorial(earth_ltS, Constant.J2000, eph);
            }
        } else
        {
            try {
                earth_0 = Vsop.getGeocentricPosition(JD_TDB, TARGET.SUN, 0.0, false, obs);
                earth_ltS = Vsop.getGeocentricPosition(JD_TDB, TARGET.SUN, Functions.getNorm(earth_0)*Constant.LIGHT_TIME_DAYS_PER_AU, false, obs);
            } catch (Exception exc) {
                earth_0 = PlanetEphem.getGeocentricPosition(JD_TDB, TARGET.SUN, 0.0, false, obs);
                earth_ltS = PlanetEphem.getGeocentricPosition(JD_TDB, TARGET.SUN, Functions.getNorm(earth_0)*Constant.LIGHT_TIME_DAYS_PER_AU, false, obs);
            }
            earth_0 = Ephem.eclipticToEquatorial(earth_0, Constant.J2000, eph);
            earth_ltS = Ephem.eclipticToEquatorial(earth_ltS, Constant.J2000, eph);
        }

        // Here the Sun is supposed to be at barycenter
        double helio_object[] = Functions.substract(meanJ2000InertialToEquatorialFK5(Elp2000.calc(JD_TDB_corrected - light_time)),
                earth_ltS);
        if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.ASTROMETRIC) {
            geo_eq = Ephem.aberration(new double[] {-geo_eq[0], -geo_eq[1], -geo_eq[2], 0, 0, 0},
                    earth_0, light_time);
            geo_eq = Functions.scalarProduct(geo_eq, -1.0);
        }

        // Correct for solar deflection
        if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT)  {
        	if (eph.preferPrecisionInEphemerides) {
		        //geo_eq = Ephem.solarDeflection(geo_eq, earth_0, helio_object);
		        geo_eq = Ephem.solarAndPlanetaryDeflection(geo_eq, earth_0, helio_object,
		            new TARGET[] {TARGET.JUPITER, TARGET.EARTH}, JD_TDB, false, obs);
        	}
            DataBase.addData("GCRS", geo_eq, true);
        } else {
            DataBase.addData("GCRS", null, true);
        }

        /* Correction to output frame. */
        geo_eq = Ephem.toOutputFrame(geo_eq, FRAME.FK5, eph.frame);
        helio_object = Ephem.toOutputFrame(helio_object, FRAME.FK5, eph.frame);

        double geo_date[];
        if (eph.frame == FRAME.FK4) {
            // Transform from B1950 to mean equinox of date
             geo_date = Precession.precess(Constant.B1950, JD_TDB, geo_eq, eph);
             helio_object = Precession.precess(Constant.B1950, JD_TDB, helio_object, eph);
        } else {
            // Transform from J2000 to mean equinox of date
            geo_date = Precession.precessFromJ2000(JD_TDB, geo_eq, eph);
            helio_object = Precession.precessFromJ2000(JD_TDB, helio_object, eph);
        }

        // Get heliocentric ecliptic position
        LocationElement loc_elem = LocationElement.parseRectangularCoordinates(Ephem.equatorialToEcliptic(helio_object, JD_TDB, eph));

        // Mean equatorial to true equatorial
        double true_eq[] = geo_date;
        if (obs.getMotherBody() == TARGET.EARTH) {
            if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT)
                /* Correct nutation */
                true_eq = Nutation.nutateInEquatorialCoordinates(JD_TDB, eph, geo_date, true);

            // Correct for polar motion
            if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT &&
                    eph.correctForPolarMotion)
            {
                double gast = SiderealTime.greenwichApparentSiderealTime(time, obs, eph);
                true_eq = Functions.rotateZ(true_eq, -gast);
                Matrix mat = IAU2006.getPolarMotionCorrectionMatrix(time, obs, eph);
                true_eq = mat.times(new Matrix(true_eq)).getColumn(0);
                true_eq = Functions.rotateZ(true_eq, gast);
            }
        }

        // Pass to coordinates as seen from another body, if necessary
        if (obs.getMotherBody() != TARGET.NOT_A_PLANET && obs.getMotherBody() != TARGET.EARTH)
            true_eq = Ephem.getPositionFromBody(LocationElement.parseRectangularCoordinates(true_eq), time, obs, eph).getRectangularCoordinates();

        // Get equatorial coordinates
        LocationElement ephem_loc = LocationElement.parseRectangularCoordinates(true_eq);

        // Set preliminary results
        EphemElement ephem_elem = new EphemElement();
        ephem_elem.rightAscension = ephem_loc.getLongitude();
        ephem_elem.declination = ephem_loc.getLatitude();
        ephem_elem.distance = ephem_loc.getRadius();
        ephem_elem.heliocentricEclipticLongitude = loc_elem.getLongitude();
        ephem_elem.heliocentricEclipticLatitude = loc_elem.getLatitude();
        ephem_elem.lightTime = (float) light_time;
        // Note distances are apparent, not true
        ephem_elem.distanceFromSun = loc_elem.getRadius();

        /* Topocentric correction */
        if (eph.isTopocentric)
            ephem_elem = Ephem.topocentricCorrection(time, obs, eph, ephem_elem);

        /* Physical ephemeris */
        Object gcrs = DataBase.getData("GCRS", true);
        EphemerisElement new_eph = new EphemerisElement(eph.targetBody, EphemerisElement.COORDINATES_TYPE.APPARENT,
                EphemerisElement.EQUINOX_OF_DATE, eph.isTopocentric, eph.ephemMethod, eph.frame);
        EphemElement ephem_elem2 = ephem_elem;
        if (eph.ephemType != EphemerisElement.COORDINATES_TYPE.APPARENT || eph.equinox != EphemerisElement.EQUINOX_OF_DATE)
            ephem_elem2 = PlanetEphem.MoshierEphemeris(time, obs, new_eph);
        new_eph.targetBody = TARGET.SUN;
        // Priority to Moshier since performance is far better
        try {
            ephem_elem2 = PhysicalParameters.physicalParameters(JD_TDB, PlanetEphem.MoshierEphemeris(time, obs, new_eph), ephem_elem2, obs, eph);
        } catch (Exception exc) {
            ephem_elem2 = PhysicalParameters.physicalParameters(JD_TDB, Vsop.vsopEphemeris(time, obs, new_eph), ephem_elem2, obs, eph);
        }
        PhysicalParameters.setPhysicalParameters(ephem_elem, ephem_elem2, time, obs, eph);
        DataBase.addData("GCRS", gcrs, true);

        /* Horizontal coordinates */
        if (eph.isTopocentric)
            ephem_elem = Ephem.horizontalCoordinates(time, obs, eph, ephem_elem);

        /* Set coordinates to the output equinox */
        if (EphemerisElement.EQUINOX_OF_DATE != eph.equinox)
        {
            ephem_elem = Ephem.toOutputEquinox(ephem_elem, eph, JD_TDB);
        }

        ephem_elem.name = eph.targetBody.getName();
        return ephem_elem;
    }

    /**
     * Corrects position of the Moon from its barycenter (center of mass) to its geometric center.
     * This correction is -0.5 arcsec in ecliptic longitude, +0.25 arcsec in latitude, and +2 km
     * in distance. See http://eclipse.gsfc.nasa.gov/LEcat5/ephemeris.html.
     * @param time Time object.
     * @param observer Observer object.
     * @param eph Ephemeris object.
     * @param ephem Ephem object.
     * @return The new ephem object with corrected position.
     * @throws JPARSECException If an error occurs.
     */
    public static EphemElement fromMoonBarycenterToGeometricCenter(TimeElement time, ObserverElement observer, EphemerisElement eph, EphemElement ephem) throws JPARSECException {
        LocationElement loc = CoordinateSystem.equatorialToEcliptic(new LocationElement(ephem.rightAscension, ephem.declination, ephem.distance), time, observer, eph);
        loc.setLongitude(loc.getLongitude() - 0.5 * Constant.ARCSEC_TO_RAD);
        loc.setLatitude(loc.getLatitude() + 0.25 * Constant.ARCSEC_TO_RAD);
        loc = CoordinateSystem.eclipticToEquatorial(loc, time, observer, eph);
        ephem.rightAscension = loc.getLongitude();
        ephem.declination = loc.getLatitude();
        ephem.distance += 2.0 / Constant.AU; // Geometric center is 2 km away from center of mass
        return ephem;
    }
}

class Elp2000Set1 implements Serializable {
    private static final long serialVersionUID = -4164711893557783607L;
    final int[] ILU;
    final double[] COEF;

    Elp2000Set1(final int[] ilu, final double[] coef) {
        this.ILU = ilu;
        this.COEF = coef;
    }
}

class Elp2000Set2 implements Serializable {
    private static final long serialVersionUID = -6678573540491229204L;
    final int[] ILU;
    final double[] COEF;

    Elp2000Set2(final int[] ilu, final double[] coef) {
        this.ILU = ilu;
        this.COEF = coef;
    }
}

class Elp2000Set3 implements Serializable {
    private static final long serialVersionUID = -743088436610082707L;
    final int[] ILU;
    final double[] COEF;

    Elp2000Set3(final int[] ilu, final double[] coef) {
        this.ILU = ilu;
        this.COEF = coef;
    }
}

class Elp2000_data implements Serializable {
    private static final long serialVersionUID = 5274349098491029263L;
    // part 1
    public Elp2000Set1[] elp_lon_sine_0_LonSine0;
    public Elp2000Set1[] elp_lon_sine_1_LonSine1;
    public Elp2000Set1[] elp_lon_sine_2_LonSine2;
    public Elp2000Set1[] elp_lat_sine_0_LatSine0;
    public Elp2000Set1[] elp_lat_sine_1_LatSine1;
    public Elp2000Set1[] elp_lat_sine_2_LatSine2;
    public Elp2000Set1[] elp_rad_cose_0_RadCose0;
    public Elp2000Set1[] elp_rad_cose_1_RadCose1;
    public Elp2000Set2[] elp_lon_earth_perturb_Lon;
    public Elp2000Set2[] elp_lat_earth_perturb_Lat;
    public Elp2000Set2[] elp_rad_earth_perturb_Rad;
    public Elp2000Set2[] elp_earth_perturb_t_Lon;
    public Elp2000Set2[] elp_earth_perturb_t_Lat;
    public Elp2000Set2[] elp_earth_perturb_t_Rad;
    public Elp2000Set2[] elp_tidal_Lon;
    public Elp2000Set2[] elp_tidal_Lat;
    public Elp2000Set2[] elp_tidal_Rad;
    public Elp2000Set2[] elp_tidal_Lon_t;
    public Elp2000Set2[] elp_tidal_Lat_t;
    public Elp2000Set2[] elp_tidal_Rad_t;
    public Elp2000Set2[] elp_moon_Lon;
    public Elp2000Set2[] elp_moon_Lat;
    public Elp2000Set2[] elp_moon_Rad;
    public Elp2000Set2[] elp_rel_Lon;
    public Elp2000Set2[] elp_rel_Lat;
    public Elp2000Set2[] elp_rel_Rad;
    public Elp2000Set2[] elp_plan_Lon;
    public Elp2000Set2[] elp_plan_Lat;
    public Elp2000Set2[] elp_plan_Rad;
    public Elp2000Set3[] elp_plan_perturb2_Lon;
    public Elp2000Set3[] elp_plan_perturb2_Lat;
    public Elp2000Set3[] elp_plan_perturb2_Rad;
    public Elp2000Set3[] elp_plan_perturb2_Lon_t;
    public Elp2000Set3[] elp_plan_perturb2_Lat_t;
    public Elp2000Set3[] elp_plan_perturb2_Rad_t;
    public Elp2000Set3[] elp_plan_perturb10_0_Lon;
    public Elp2000Set3[] elp_plan_perturb10_1_Lon;
    public Elp2000Set3[] elp_plan_perturb10_2_Lon;
    public Elp2000Set3[] elp_plan_perturb10_3_Lon;
    public Elp2000Set3[] elp_plan_perturb10_4_Lon;
    public Elp2000Set3[] elp_plan_perturb10_5_Lon;
    public Elp2000Set3[] elp_plan_perturb10_6_Lon;
    public Elp2000Set3[] elp_plan_perturb10_7_Lon;
    public Elp2000Set3[] elp_plan_perturb10_8_Lon;
    public Elp2000Set3[] elp_plan_perturb10_9_Lon;
    public Elp2000Set3[] elp_plan_perturb10_10_Lon;
    public Elp2000Set3[] elp_plan_perturb10_11_Lon;
    public Elp2000Set3[] elp_plan_perturb10_12_Lon;
    public Elp2000Set3[] elp_plan_perturb10_13_Lon;
    public Elp2000Set3[] elp_plan_perturb10_14_Lon;
    public Elp2000Set3[] elp_plan_perturb10_15_Lon;
    public Elp2000Set3[] elp_plan_perturb10_16_Lon;
    public Elp2000Set3[] elp_plan_perturb10_17_Lon;
    public Elp2000Set3[] elp_plan_perturb10_18_Lon;
    public Elp2000Set3[] elp_plan_perturb10_19_Lon;
    public Elp2000Set3[] elp_plan_perturb10_20_Lon;
    public Elp2000Set3[] elp_plan_perturb10_21_Lon;
    public Elp2000Set3[] elp_plan_perturb10_22_Lon;
    public Elp2000Set3[] elp_plan_perturb10_23_Lon;
    public Elp2000Set3[] elp_plan_perturb10_24_Lon;
    public Elp2000Set3[] elp_plan_perturb10_25_Lon;
    public Elp2000Set3[] elp_plan_perturb10_26_Lon;
    public Elp2000Set3[] elp_plan_perturb10_27_Lon;
    public Elp2000Set3[] elp_plan_perturb10_28_Lon;
    public Elp2000Set3[] elp_plan_perturb10_29_Lon;
    public Elp2000Set3[] elp_plan_perturb10_30_Lon;
    public Elp2000Set3[] elp_plan_perturb10_31_Lon;
    public Elp2000Set3[] elp_plan_perturb10_32_Lon;
    public Elp2000Set3[] elp_plan_perturb10_33_Lon;
    public Elp2000Set3[] elp_plan_perturb10_34_Lon;
    public Elp2000Set3[] elp_plan_perturb10_35_Lon;

    // part 2
    public Elp2000Set3[] elp_plan_perturb11_0_Lat;
    public Elp2000Set3[] elp_plan_perturb11_1_Lat;
    public Elp2000Set3[] elp_plan_perturb11_2_Lat;
    public Elp2000Set3[] elp_plan_perturb11_3_Lat;
    public Elp2000Set3[] elp_plan_perturb11_4_Lat;
    public Elp2000Set3[] elp_plan_perturb11_5_Lat;
    public Elp2000Set3[] elp_plan_perturb11_6_Lat;
    public Elp2000Set3[] elp_plan_perturb11_7_Lat;
    public Elp2000Set3[] elp_plan_perturb11_8_Lat;
    public Elp2000Set3[] elp_plan_perturb11_9_Lat;
    public Elp2000Set3[] elp_plan_perturb11_10_Lat;
    public Elp2000Set3[] elp_plan_perturb11_11_Lat;
    public Elp2000Set3[] elp_plan_perturb11_12_Lat;
    public Elp2000Set3[] elp_plan_perturb11_13_Lat;
    public Elp2000Set3[] elp_plan_perturb12_0_Rad;
    public Elp2000Set3[] elp_plan_perturb12_1_Rad;
    public Elp2000Set3[] elp_plan_perturb12_2_Rad;
    public Elp2000Set3[] elp_plan_perturb12_3_Rad;
    public Elp2000Set3[] elp_plan_perturb12_4_Rad;
    public Elp2000Set3[] elp_plan_perturb12_5_Rad;
    public Elp2000Set3[] elp_plan_perturb12_6_Rad;
    public Elp2000Set3[] elp_plan_perturb12_7_Rad;
    public Elp2000Set3[] elp_plan_perturb12_8_Rad;
    public Elp2000Set3[] elp_plan_perturb12_9_Rad;
    public Elp2000Set3[] elp_plan_perturb12_10_Rad;
    public Elp2000Set3[] elp_plan_perturb12_11_Rad;
    public Elp2000Set3[] elp_plan_perturb12_12_Rad;
    public Elp2000Set3[] elp_plan_perturb12_13_Rad;
    public Elp2000Set3[] elp_plan_perturb12_14_Rad;
    public Elp2000Set3[] elp_plan_perturb12_15_Rad;
    public Elp2000Set3[] elp_plan_perturb12_16_Rad;
    public Elp2000Set3[] elp_plan_perturb13_0_Lon;
    public Elp2000Set3[] elp_plan_perturb13_1_Lon;
    public Elp2000Set3[] elp_plan_perturb13_2_Lon;
    public Elp2000Set3[] elp_plan_perturb13_3_Lon;
    public Elp2000Set3[] elp_plan_perturb13_4_Lon;
    public Elp2000Set3[] elp_plan_perturb13_5_Lon;
    public Elp2000Set3[] elp_plan_perturb13_6_Lon;
    public Elp2000Set3[] elp_plan_perturb13_7_Lon;
    public Elp2000Set3[] elp_plan_perturb13_8_Lon;
    public Elp2000Set3[] elp_plan_perturb13_9_Lon;
    public Elp2000Set3[] elp_plan_perturb13_10_Lon;
    public Elp2000Set3[] elp_plan_perturb14_0_Lat;
    public Elp2000Set3[] elp_plan_perturb14_1_Lat;
    public Elp2000Set3[] elp_plan_perturb14_2_Lat;
    public Elp2000Set3[] elp_plan_perturb15_0_Rad;
    public Elp2000Set3[] elp_plan_perturb15_1_Rad;
    public Elp2000Set3[] elp_plan_perturb15_2_Rad;
    public Elp2000Set3[] elp_plan_perturb15_3_Rad;
    public Elp2000Set3[] elp_plan_perturb15_4_Rad;
}
