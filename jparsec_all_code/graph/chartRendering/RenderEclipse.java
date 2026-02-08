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
package jparsec.graph.chartRendering;

import java.util.ArrayList;

import jparsec.astronomy.TelescopeElement;
import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.EphemerisElement.COORDINATES_TYPE;
import jparsec.ephem.Functions;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.event.LunarEclipse;
import jparsec.ephem.event.MainEvents;
import jparsec.ephem.event.MoonEventElement;
import jparsec.ephem.event.SimpleEventElement;
import jparsec.ephem.event.SolarEclipse;
import jparsec.ephem.event.MainEvents.EVENT_TIME;
import jparsec.ephem.event.SimpleEventElement.EVENT;
import jparsec.ephem.planets.EphemElement;
import jparsec.ephem.planets.OrbitEphem;
import jparsec.graph.DataSet;
import jparsec.graph.JPARSECStroke;
import jparsec.graph.chartRendering.Graphics.FONT;
import jparsec.graph.chartRendering.SatelliteRenderElement.PLANET_MAP;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.math.Constant;
import jparsec.math.FastMath;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.observer.ObserverElement.DST_RULE;
import jparsec.time.AstroDate;
import jparsec.time.SiderealTime;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.time.TimeScale;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;

/**
 * Calculates eclipse maps and local circumstances. Based on
 * Elements of solar eclipses 1951-2200, by Jean Meeus. Check also the
 * interesting work by J. Garc&iacute;a Ferrer (in Spanish) at
 * http://personal.telefonica.terra.es/web/xgarciaf/eclipse/eclipse.htm,
 * used for the implementation done here.<P>
 * Bessel elements used in this class are those computed by Shinobu Takesako
 * using JPL DE422 ephemerides. See EmapWin Ver. 2.12 (2012.11.17),
 * downloadable from http://www.kotenmon.com/cal/emapwin_eng.htm. All solar
 * eclipses between year 3000 B.C. and 3000 A.D. are available.<P>
 * Note implementation is not completely robust. Charts of solar eclipses
 * are not completely clean, and {@linkplain SolarEclipse} and {@linkplain LunarEclipse}
 * classes are used to correct the approximate start/end times of the different
 * phases of the eclipses.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 * @see SolarEclipse
 * @see LunarEclipse
 */
public class RenderEclipse {

	/** Coefficients for the two degree polynomial to obtain X for any given time. X axis
	 * is the intersection between the fundamental plain and the equator. The fundamental
	 * plane is perpendicular to the shadow cone axis. X is positive towards East. */
	private double X0, X1, X2, X3;
	/** Coefficients for the two degree polynomial to obtain Y for any given time. X axis
	 * is the intersection between the fundamental plain and the equator, while Y axis is
	 * perpendicular to X axis and positive towards North. */
	private double Y0, Y1, Y2, Y3;
	/** Reference time for the Besselian elements, measured in hours. */
	private double T0;
	/** Minimum distance shadow cone axis-Earth's center, in Earth equatorial radii units.
	 * Positive if shadow cone axis is towards north respect Earth's center.
	 * Limiting value due to Earth flattening is 0.997. */
	private double GAMMA;
	/** Coefficients for the two degree polynomial to obtain D for any given time. D is
	 * the declination of the shadow cone axis. */
	private double D0, D1, D2;
	/** Coefficients for the one degree polynomial to obtain M for any given time. M is
	 * the hour angle of the shadow cone axis. */
	private double M0, M1;
	/** Coefficients for the two degree polynomial to obtain L1 for any given time. L1 is
	 * the radius of the penumbral cone in the fundamental plain. */
	private double L10, L11, L12;
	/** Coefficients for the two degree polynomial to obtain L2 for any given time. L2 is
	 * the radius of the umbral cone in the fundamental plain. */
	private double L20, L21, L22;
	/** Tangent of the Earth's penumbral cone wrt Moon's shadow cone axis. Always positive
	 * and considered constant during the eclipse. */
	private double F1;
	/** Tangent of the Earth's umbra cone wrt Moon's shadow cone axis. Always positive
	 * and considered constant during the eclipse. */
	private double F2;
	/** TT-TU (s) for the eclipse */
	private double DT;
	/** Input date. */
	private AstroDate astro;

	private static final double ELEVATION_LIMIT = Math.sin(-0.567*Constant.DEG_TO_RAD);
	private static final double FLATENNING_FACTOR = .99664719; // 1.0 - 1.0 / 298.257
	private static final double EARTH_RADIUS = 6378140.0;
	private static final double DT_TO_DEG = Constant.SIDEREAL_DAY_LENGTH * 15.0 / 3600.0;
	/**
	 * True to show information in a simplified way for divulgation, without LT (local time).
	 * Default is false.
	 */
	public static boolean ShowWithoutLT = false;
	/**
	 * Selects if the Moon texture should be shown or not. Default is false.
	 */
	public static boolean ShowMoonTexture = false;

	/**
	 * Basic constructor for a given date. Calculations uses Besselian elements
	 * from the data included in JPARSEC.
	 * @param astro The approximate date (within 1 day) of a given solar eclipse.
	 * @throws JPARSECException If the day is wrong or outside range -3000 to +3000.
	 */
	public RenderEclipse(AstroDate astro) throws JPARSECException {
		String sep = " ";
		boolean skip = true;
		String val = FileIO.addSpacesBeforeAString(""+astro.getAstronomicalYear(), 5)+sep+FileIO.addSpacesBeforeAString(""+astro.getMonth(), 2)+sep;

		String data[] = DataSet.arrayListToStringArray(ReadFile.readResourceContaining(FileIO.DATA_ORBITAL_ELEMENTS_DIRECTORY + "solarEclipses.txt", ReadFile.ENCODING_ISO_8859, val));
		String val1 = val + FileIO.addSpacesBeforeAString(""+astro.getDay(), 2)+sep;

		int index = DataSet.getIndexStartingWith(data, val1);
		double jd = astro.jd();
		if (index < 0) {
			val1 = val + FileIO.addSpacesBeforeAString(""+(astro.getDay()+1), 2)+sep;
			index = DataSet.getIndexStartingWith(data, val1);
			if (index >= 0) jd ++;
		}
		if (index < 0) {
			val1 = val + FileIO.addSpacesBeforeAString(""+(astro.getDay()-1), 2)+sep;
			index = DataSet.getIndexStartingWith(data, val1);
			if (index >= 0) jd --;
		}

		if (index < 0) throw new JPARSECException("Solar eclipse not found on date "+astro.toMinString());

		int field = 10;
		GAMMA = DataSet.parseDouble(FileIO.getField(6, data[index], sep, skip));
		T0 = DataSet.parseDouble(FileIO.getField(field++, data[index], sep, skip));
		X0 = DataSet.parseDouble(FileIO.getField(field++, data[index], sep, skip));
		X1 = DataSet.parseDouble(FileIO.getField(field++, data[index], sep, skip));
		X2 = DataSet.parseDouble(FileIO.getField(field++, data[index], sep, skip));
		X3 = DataSet.parseDouble(FileIO.getField(field++, data[index], sep, skip));
		Y0 = DataSet.parseDouble(FileIO.getField(field++, data[index], sep, skip));
		Y1 = DataSet.parseDouble(FileIO.getField(field++, data[index], sep, skip));
		Y2 = DataSet.parseDouble(FileIO.getField(field++, data[index], sep, skip));
		Y3 = DataSet.parseDouble(FileIO.getField(field++, data[index], sep, skip));
		D0 = DataSet.parseDouble(FileIO.getField(field++, data[index], sep, skip));
		D1 = DataSet.parseDouble(FileIO.getField(field++, data[index], sep, skip));
		D2 = DataSet.parseDouble(FileIO.getField(field++, data[index], sep, skip));
		M0 = DataSet.parseDouble(FileIO.getField(field++, data[index], sep, skip));
		M1 = DataSet.parseDouble(FileIO.getField(field++, data[index], sep, skip));
		L10 = DataSet.parseDouble(FileIO.getField(field++, data[index], sep, skip));
		L11 = DataSet.parseDouble(FileIO.getField(field++, data[index], sep, skip));
		L12 = DataSet.parseDouble(FileIO.getField(field++, data[index], sep, skip));
		L20 = DataSet.parseDouble(FileIO.getField(field++, data[index], sep, skip));
		L21 = DataSet.parseDouble(FileIO.getField(field++, data[index], sep, skip));
		L22 = DataSet.parseDouble(FileIO.getField(field++, data[index], sep, skip));
		F1 = DataSet.parseDouble(FileIO.getField(field++, data[index], sep, skip));
		F2 = DataSet.parseDouble(FileIO.getField(field++, data[index], sep, skip));
		this.astro = new AstroDate(jd);
		DT = TimeScale.getTTminusUT1(this.astro);
	}

	/**
	 * Returns the date found for the solar eclipse calculated in this instance.
	 * @return The eclipse date.
	 */
	public AstroDate getEclipseDate() {
		return this.astro;
	}

	private double[] centralEclipseLatitudeLimits(double LG) {
		// North limit
		int I = 1;
		double T = 0, FI = 0, FIN = 0;
		int niter = 0;
		boolean fromNorth = false, fromSouth = false;
		do {
			double T2 = T * T, T3 = T2 * T;

			double X = X0 + X1 * T + X2 * T2 + X3 * T3;
			double Y = Y0 + Y1 * T + Y2 * T2 + Y3 * T3;
			double D = D0 + D1 * T + D2 * T2;
			double M = M0 + M1 * T;
			// Daily variations
			double VX = X1 + 2 * X2 * T + 3 * X3 * T2;
			double VY = Y1 + 2 * Y2 * T + 3 * Y3 * T2;
			double H = M + LG - DT_TO_DEG * DT;
			// Geocentric coordinates
			double FU = Math.atan(FLATENNING_FACTOR * Math.tan(FI * Constant.DEG_TO_RAD));
			double RSIN = FLATENNING_FACTOR * Math.sin(FU);
			double RCOS = Math.cos(FU);
			D *= Constant.DEG_TO_RAD;
			H *= Constant.DEG_TO_RAD;
			double cosH = Math.cos(H), sinH = Math.sin(H);
			double cosD = Math.cos(D), sinD = Math.sin(D);
			double P = RCOS * sinH;
			double Q = RSIN * cosD - RCOS * cosH * sinD;
			double R = RSIN * sinD + RCOS * cosH * cosD;
			double VP = Constant.DEG_TO_RAD * M1 * RCOS * cosH;
			double VQ = Constant.DEG_TO_RAD * (M1 * P * sinD - R * D1);
			double U = X - P, V = Y - Q;
			double A = VX - VP, B = VY - VQ;
			double N = FastMath.hypot(A, B);
			double TAU = -(U * A + V * B) / (N * N);
			T += TAU;
			double L2 = L20 + L21 * T + L22 * T2;
			double L2P = L2 - R * F2;
			// Latitude correction
			double CW = (V * A - U * B) / N;
			double CQ = (B * Math.sin(H) * RSIN + A * (Math.cos(H) * Math.sin(D) * RSIN + Math.cos(D) * RCOS)) / (Constant.RAD_TO_DEG * N);
			double CFI = (CW + I * Math.abs(L2P)) / CQ;
			FI = FI + CFI;

			niter ++;
			if (niter < 50) {
				if (Math.abs(TAU) > .000001) continue;
				if (Math.abs(CFI) >= .00001) continue;
				double sinALT = (sinD * FastMath.sin(FI * Constant.DEG_TO_RAD) + cosD * FastMath.cos(FI * Constant.DEG_TO_RAD) * cosH); // Approx Sun elevation
				if (sinALT < ELEVATION_LIMIT) return null; // below -34' to consider refraction
			} else {
				if (!fromNorth) {
					FI = 80;
					niter = 0;
					T = 0;
					fromNorth = true;
					continue;
				} else {
					if (!fromSouth) {
						FI = -80;
						niter = 0;
						T = 0;
						fromSouth = true;
						continue;
					} else {
						if (I == 1) FI = 90;
						if (I == -1) FI = -90;
//						return null;
					}
				}
			}

			if (Math.abs(FI) > 90) FI = 90 * FastMath.sign(FI);

			if (I == -1) {
				if (FIN == 90 && FI == -90) return null;
				if ((FIN == 90 || FI == -90) && Math.abs(FIN-FI) > 25) return null;
				return new double[] {FIN, FI};
			}
			FIN = FI;
			I = -1;
			T = 0;
			FI = 0;
			niter = 0;
		} while (true);
	}

	private double[] partialEclipseLatitudeLimits(double LG) {
		// North limit
		int I = 1;
		double T = 0, FI = 0, FIN = 0;
		int niter = 0;
		boolean fromNorth = false, fromSouth = false;
		do {
			double T2 = T * T, T3 = T2 * T;

			double X = X0 + X1 * T + X2 * T2 + X3 * T3;
			double Y = Y0 + Y1 * T + Y2 * T2 + Y3 * T3;
			double D = D0 + D1 * T + D2 * T2;
			double M = M0 + M1 * T;
			// Daily variations
			double VX = X1 + 2 * X2 * T + 3 * X3 * T2;
			double VY = Y1 + 2 * Y2 * T + 3 * Y3 * T2;
			double H = M + LG - DT_TO_DEG * DT;
			// Geocentric coordinates
			double FU = Math.atan(FLATENNING_FACTOR * Math.tan(FI * Constant.DEG_TO_RAD));
			double RSIN = FLATENNING_FACTOR * Math.sin(FU);
			double RCOS = Math.cos(FU);
			D *= Constant.DEG_TO_RAD;
			H *= Constant.DEG_TO_RAD;
			double cosH = Math.cos(H), sinH = Math.sin(H);
			double cosD = Math.cos(D), sinD = Math.sin(D);
			double P = RCOS * sinH;
			double Q = RSIN * cosD - RCOS * cosH * sinD;
			double R = RSIN * sinD + RCOS * cosH * cosD;
			double VP = Constant.DEG_TO_RAD * M1 * RCOS * cosH;
			double VQ = Constant.DEG_TO_RAD * (M1 * P * sinD - R * D1);
			double U = X - P, V = Y - Q;
			double A = VX - VP, B = VY - VQ;
			double N = FastMath.hypot(A, B);
			double TAU = -(U * A + V * B) / (N * N);
			T += TAU;
			double L1 = L10 + L11 * T + L12 * T2;
			double L1P = L1 - R * F1;
			// Latitude correction
			double CW = (V * A - U * B) / N;
			double CQ = (B * Math.sin(H) * RSIN + A * (Math.cos(H) * Math.sin(D) * RSIN + Math.cos(D) * RCOS)) / (Constant.RAD_TO_DEG * N);
			double CFI = (CW + I * Math.abs(L1P)) / CQ;
			FI = FI + CFI;

			niter ++;
			if (niter < 50) {
				if (Math.abs(TAU) > .000001) continue;
				if (Math.abs(CFI) >= .00001) continue;
				double sinALT = (sinD * FastMath.sin(FI * Constant.DEG_TO_RAD) + cosD * FastMath.cos(FI * Constant.DEG_TO_RAD) * cosH); // Approx Sun elevation
				if (sinALT < ELEVATION_LIMIT) return null; // below -34' to consider refraction
			} else {
				if (!fromNorth) {
					FI = 80;
					niter = 0;
					T = 0;
					fromNorth = true;
					continue;
				} else {
					if (!fromSouth) {
						FI = -80;
						niter = 0;
						T = 0;
						fromSouth = true;
						continue;
					} else {
						if (I == 1) FI = 90;
						if (I == -1) FI = -90;
//						return null;
					}
				}
			}
			if (Math.abs(FI) > 90) FI = 90 * FastMath.sign(FI);

			if (I == -1) {
				if (FIN == 90 && FI == -90) return null;
//				if ((FIN == 90 || FI == -90) && Math.abs(FIN-FI) > 25) return null;
				return new double[] {FIN, FI};
			}
			FIN = FI;
			I = -1;
			T = 0;
			FI = 0;
			niter = 0;
		} while (true);
	}

	private double[] centralEclipseCurve(double HD, double MD) {
		double TD = HD + MD / 60.0;
		double T = TD - T0;
		double T2 = T * T, T3 = T2 * T;
		double X = X0 + X1 * T + X2 * T2 + X3 * T3;
		double Y = Y0 + Y1 * T + Y2 * T2 + Y3 * T3;
		double D = D0 + D1 * T + D2 * T2;
		double M = M0 + M1 * T;
		double L2 = L20 + L21 * T + L22 * T2;

		// Diurnal variations
		double VX = X1 + 2 * X2 * T + 3 * X3 * T2;
		double VY = Y1 + 2 * Y2 * T + 3 * Y3 * T2;

		D *= Constant.DEG_TO_RAD;
		double sinD = Math.sin(D), cosD = Math.cos(D);
		double W = 1.0 / Math.sqrt(1.0 - 6.694385E-03 * FastMath.pow(cosD, 2.0));
		double P = M1 * Constant.DEG_TO_RAD;
		double B = VY - P * X * sinD;
		double C = VX + P * Y * sinD;
		double YP = W * Y;
		double B1 = W * sinD;
		double B2 = FLATENNING_FACTOR * W * cosD;
		if ((1.0 - X * X - YP * YP) < 0) return null;

		double BT = Math.sqrt(1.0 - X * X - YP * YP);
		double FI1 = Math.asin(BT * B1 + YP * B2);
		double H = Math.atan(X / (BT * B2 - YP * B1));
		if ((BT * B2 - YP * B1) < 0) H += Math.PI;
		H *= Constant.RAD_TO_DEG;
		double FI = Constant.RAD_TO_DEG	* Math.atan(1.00336409 * Math.tan(FI1)); // Lat
		double LG = M - H - DT_TO_DEG * DT; // Lon
		LG = Functions.normalizeDegrees(-LG);
		double L2P = L2 - BT * F2;
		boolean total = true;
		if (L2P > 0) total = false; // ANULAR
		double A = C - P * BT * cosD;
		double N = FastMath.hypot(A, B);
		double DUR = 7200 * Math.abs(L2P) / N; // s
		double K = FastMath.hypot(BT, (X * A + Y * B)) / N;
		double ANCH = EARTH_RADIUS * 0.001 * 2.0 * Math.abs(L2P) / K; // km

		double L1P = L10 + L11 * T + L12 * T2 - B * F1;
		double S = (L1P - L2P) / (L1P + L2P); // Moon/Sun apparent diameter ratio
		return new double[] { LG, FI, ANCH, DUR, S, (total == true) ? 1 : 0 };
	}

	private double[] centralEclipseCurve(double LG) {
		double T = 0, FI = 0, D = 0, X = 0, Y = 0, VX = 0, VY = 0, L2 = 0, T2 = 0, T3 = 0;

		int niter = 0;
		while (true) {
			niter ++;

			T2 = T * T;
			T3 = T2 * T;
			X = X0 + X1 * T + X2 * T2 + X3 * T3;
			Y = Y0 + Y1 * T + Y2 * T2 + Y3 * T3;
			D = D0 + D1 * T + D2 * T2;
			double M = M0 + M1 * T;
			L2 = L20 + L21 * T + L22 * T2;

			// Diurnal variations
			VX = X1 + 2 * X2 * T + 3 * X3 * T2;
			VY = Y1 + 2 * Y2 * T + 3 * Y3 * T2;

			double H = M + LG - DT_TO_DEG * DT;

			// Geocentric coordinates
			double FU = Math.atan(FLATENNING_FACTOR * Math.tan(FI * Constant.DEG_TO_RAD));
			double RSIN = FLATENNING_FACTOR * Math.sin(FU);
			double RCOS = Math.cos(FU);
			D *= Constant.DEG_TO_RAD;
			H *= Constant.DEG_TO_RAD;
			double cosH = Math.cos(H), sinH = Math.sin(H);
			double cosD = Math.cos(D), sinD = Math.sin(D);
			double P = RCOS * sinH;
			double Q = RSIN * cosD - RCOS * cosH * sinD;
			double R = RSIN * sinD + RCOS * cosH * cosD;
			double VP = Constant.DEG_TO_RAD * M1 * RCOS * cosH;
			double VQ = Constant.DEG_TO_RAD * (M1 * P * sinD - R * D1);
			double U = X - P, V = Y - Q;
			double A = VX - VP, B = VY - VQ;
			double N = FastMath.hypot(A, B);
			double TAU = -(U * A + V * B) / (N * N);
			T += TAU;
			double Wc = (V * A - U * B) / N;
			double Qc = (B * sinH * RSIN + A * (cosH * sinD * RSIN + cosD + RCOS)) / (Constant.RAD_TO_DEG * N);
			double dFI = Wc / Qc;
			FI += dFI;
			if (TAU < 0.001 && dFI < 0.0001) break;
			if (niter > 50) return null;
		};

		D *= Constant.DEG_TO_RAD;
		double sinD = Math.sin(D), cosD = Math.cos(D);
		double W = 1.0 / Math.sqrt(1.0 - 6.694385E-03 * FastMath.pow(cosD, 2.0));
		double P = M1 * Constant.DEG_TO_RAD;
		double B = VY - P * X * sinD;
		double C = VX + P * Y * sinD;
		double YP = W * Y;
		if ((1.0 - X * X - YP * YP) < 0) return null;

		double BT = Math.sqrt(1.0 - X * X - YP * YP);
		double L2P = L2 - BT * F2;
		boolean total = true;
		if (L2P > 0) total = false; // ANULAR
		double A = C - P * BT * cosD;
		double N = FastMath.hypot(A, B);
		double DUR = 7200 * Math.abs(L2P) / N; // s
		double K = FastMath.hypot(BT, (X * A + Y * B)) / N;
		double ANCH = EARTH_RADIUS * 0.001 * 2.0 * Math.abs(L2P) / K; // km

		double L1P = L10 + L11 * T + L12 * T2 - B * F1;
		double S = (L1P - L2P) / (L1P + L2P); // Moon/Sun apparent diameter ratio
		return new double[] { LG, FI, ANCH, DUR, S, (total == true) ? 1 : 0 };
	}

	private double[] extremeTimes() {
		double W = 1.0 / Math.sqrt(1.0 - 6.694385E-03 * FastMath.pow(Math.cos(D0 * Constant.DEG_TO_RAD), 2.0));
		double U = X0, A = X1, V = W * Y0, B = W * Y1;
		double N = FastMath.hypot(A, B);
		double S = (A * V - U * B) / N;
		if (S < -1 || S > 1) return null;
		double sqS = Math.sqrt(1.0 - S * S);
		double TAU1 = -(U * A + V * B) / (N * N) - sqS / N;
		double TAU2 = -(U * A + V * B) / (N * N) + sqS / N;
		double ext[] = extremes(TAU1);
		TAU1 += ext[0];
		double TD1 = T0 + TAU1;
		ext = extremes(TAU2);
		TAU2 += ext[1];
		double TD2 = T0 + TAU2;

		return new double[] {TD1, TD2};
	}

	private double[] extremes(double T) {
		double T2 = T * T, T3 = T2 * T;
		double X = X0 + X1 * T + X2 * T2 + X3 * T3;
		double Y = Y0 + Y1 * T + Y2 * T2 + Y3 * T3;
		double D = (D0 + D1 * T + D2 * T2) * Constant.DEG_TO_RAD;
		double W = 1.0 / Math.sqrt(1.0 - 6.694385E-03 * FastMath.pow(Math.cos(D), 2.0));
		double V = W * Y, U = X;
		double A = X1 + 2 * X2 * T + 3 * X3 * T2;
		double B = W * (Y1 + 2 * Y2 * T + 3 * Y3 * T2);
		double N = FastMath.hypot(A, B);
		double S = (A * V - U * B) / N;
		if (S < -1 || S > 1) return null;
		double sqS = Math.sqrt(1.0 - S * S);
		double CTAU1 = -(U * A + V * B) / (N * N) - sqS / N;
		double CTAU2 = -(U * A + V * B) / (N * N) + sqS / N;
		return new double[] {CTAU1, CTAU2};
	}

	private double[] sameMagnitudeCurve(double G, double LG) {
		double limit1[] = null, limit2[] = null;
		int sgn = 1;
		for (int i=0; i<2; i++) {
			// North-limit iteration
			int J = 0;
			double T = 0, FI = 0;
			double limit[] = null;
			do {
				double T2 = T * T, T3 = T2 * T;
				J++;
				double X = X0 + X1 * T + X2 * T2 + X3 * T3;
				double Y = Y0 + Y1 * T + Y2 * T2 + Y3 * T3;
				double D = D0 + D1 * T + D2 * T2;
				double M = M0 + M1 * T;
				// Daily variations
				double VX = X1 + 2 * X2 * T + 3 * X3 * T2;
				double VY = Y1 + 2 * Y2 * T + 3 * Y3 * T2;
				double H = M + LG - DT_TO_DEG * DT;

				// Geocentric coordinates
				double FU = Math.atan(FLATENNING_FACTOR * Math.tan(FI * Constant.DEG_TO_RAD));
				double RSIN = FLATENNING_FACTOR * FastMath.sin(FU);
				double RCOS = FastMath.cos(FU);
				D *= Constant.DEG_TO_RAD;
				H *= Constant.DEG_TO_RAD;
				double cosH = FastMath.cos(H), sinH = FastMath.sin(H);
				double cosD = FastMath.cos(D), sinD = FastMath.sin(D);
				double P = RCOS * sinH;
				double Q = RSIN * cosD - RCOS * cosH * sinD;
				double R = RSIN * sinD + RCOS * cosH * cosD;
				double VP = Constant.DEG_TO_RAD * M1 * RCOS * cosH;
				double VQ = Constant.DEG_TO_RAD * (M1 * P * sinD - R * D1);
				double U = X - P, V = Y - Q;
				double A = VX - VP, B = VY - VQ;
				double N = FastMath.hypot(A, B);
				double TAU = -(U * A + V * B) / (N * N);
				T += TAU;
				double L1 = L10 + L11 * T + L12 * T2;
				double L2 = L20 + L21 * T + L22 * T2;
				double L1P = L1 - R * F1;
				double L2P = L2 - R * F2;
				// Latitude correction
				double E = L1P - G * (L1P + L2P);
				double CW = (V * A - U * B) / N;
				double CQ = (B * sinH * RSIN + A * (cosH * sinD * RSIN + cosD * RCOS)) / (Constant.RAD_TO_DEG * N);
				double CFI = (CW + sgn * Math.abs(E)) / CQ;
				FI += CFI;
				if (J > 50) break;
				if (Math.abs(TAU) > .000001) continue;
				if (Math.abs(CFI) >= .00001) continue;

				if (Math.abs(FI) > 90) FI = 90 * FastMath.sign(FI);
				// FI
				double TD = T0 + T;
				limit = new double[] { FI, TD - DT / 3600 };
				double sinALT = (sinD * FastMath.sin(FI * Constant.DEG_TO_RAD) + cosD * FastMath.cos(FI * Constant.DEG_TO_RAD) * cosH); // Approx Sun elevation
				if (sinALT < ELEVATION_LIMIT) limit = null; // below -34' to consider refraction
				break;
			} while (true);

			if (limit == null) limit = new double[] {-1, -1};
			if (i == 0) {
				limit1 = limit;
			} else {
				limit2 = limit;
			}
			sgn = -1;
		}

		return new double[] {limit1[0], limit1[1], limit2[0], limit2[1]};
	}

	private double[] sameTimeCurve(double TU, double LG, double dPHI) throws JPARSECException {
		double min = -90, max = 90;
		double limit[] = sameMagnitudeCurve(0, LG);
		if (limit[0] != -1) max = limit[0];
		if (limit[2] != -1) min = limit[2];

		double T = TU + DT / 3600 - T0;

		double G_MAX = -1, FI_MAX = -1, minDif = 1E10;
		for (double FI=min;FI<=max;FI=FI+dPHI) {
			double FID = FI * Constant.DEG_TO_RAD;
			double FU = FastMath.atan2_accurate(FLATENNING_FACTOR * Math.tan(FID), 1.0); //Math.atan(FLATENNING_FACTOR * Math.tan(FID));
			double RSIN = FLATENNING_FACTOR * FastMath.sin(FU);
			double RCOS = FastMath.cos(FU);

			// Calculate eclipse maximum
			double data[] = this.fastEclipseMaximum(0, 0, 0, LG, 0.0, 0, RCOS, RSIN);
			if (data != null) {
				double T2 = data[0];
				double dt = Math.abs(T2-T);
				if (dt >= minDif || dt > 0.01) continue;
				double U = data[1], V = data[2], L1P = data[3], L2P = data[4], D = data[8], H = data[9];
				double M = FastMath.hypot(U, V);
				double G = (L1P - M) / (L1P + L2P);
				if (G < 0.0) continue;
				double sinFI = FastMath.sin(FID), cosFI = FastMath.cos(FID);
				double sinALT = (FastMath.sin(D) * sinFI + FastMath.cos(D) * cosFI * FastMath.cos(H));
				if (sinALT >= ELEVATION_LIMIT) {
					FI_MAX = FI;
					G_MAX = G;
					minDif = dt;
				} else {
					if (sinALT < -FastMath.sin(20*Constant.DEG_TO_RAD)) {
						FI += 19;
					} else {
						if (sinALT < -FastMath.sin(10*Constant.DEG_TO_RAD)) {
							FI += 9;
						} else {
							if (sinALT < -FastMath.sin(5*Constant.DEG_TO_RAD)) FI += 4;
						}
					}
				}
			}
		}

		if (G_MAX < 0 || minDif > 0.01) return null;
		return new double[] {FI_MAX, G_MAX};
	}

	/**
	 * Calculates if any part of the eclipse is visible from a given observer.
	 * @param obs The observer.
	 * @return False if even the partial phase cannot be visible, true if
	 * at least the partial phase is visible.
	 * @throws JPARSECException If an error occurs.
	 */
	public boolean isVisible(ObserverElement obs) throws JPARSECException {
		double lon = obs.getLongitudeRad(), FI = obs.getLatitudeRad(), ALTURA = obs.getHeight();
		double DTU = 0;
		double sinFI = Math.sin(FI), cosFI = Math.cos(FI);
		double FU = Math.atan(FLATENNING_FACTOR * Math.tan(FI));
		double RSIN = FLATENNING_FACTOR * Math.sin(FU) + ALTURA * sinFI / EARTH_RADIUS;
		double RCOS = Math.cos(FU) + ALTURA * cosFI / EARTH_RADIUS;
		// Calculate eclipse maximum
		double data[] = this.eclipseMaximum(0, 0, 0, lon * Constant.RAD_TO_DEG, DTU, 0, RCOS, RSIN);
		double G = -1;
		if (data != null) {
			double U = data[1], V = data[2], L1P = data[3], L2P = data[4], A = data[5], B = data[6], N = data[7], D = data[8], H = data[9];
			double M = FastMath.hypot(U, V);
			G = (L1P - M) / (L1P + L2P);
			double sinALT = (Math.sin(D) * sinFI + Math.cos(D) * cosFI * Math.cos(H));
			if (sinALT < ELEVATION_LIMIT) {
				// Start/end of partial phase (external contacts)
				double TM = data[0];
				double S = (A * V - U * B) / (N * L1P);
				if (S < -1 || S > 1) return false;
				double TAU = L1P / N * Math.sqrt(1.0 - S * S);
				double PC = TM - TAU, UC = TM + TAU;
				// Correction to first contact
				data = this.eclipseMaximum(-1, 0, PC, lon * Constant.RAD_TO_DEG, DTU, S, RCOS, RSIN);
				D = data[8];
				H = data[9];
				sinALT = Math.sin(D) * sinFI + Math.cos(D) * cosFI * Math.cos(H);
				if (sinALT < ELEVATION_LIMIT) {
					// Correction to last contact
					data = this.eclipseMaximum(1, 0, UC, lon * Constant.RAD_TO_DEG, DTU, S, RCOS, RSIN);
					D = data[8];
					H = data[9];
					sinALT = Math.sin(D) * sinFI + Math.cos(D) * cosFI * Math.cos(H);
					G = -10;
					if (sinALT < ELEVATION_LIMIT) G = -1;
				} else {
					// Correction to last contact
					data = this.eclipseMaximum(1, 0, UC, lon * Constant.RAD_TO_DEG, DTU, S, RCOS, RSIN);
					D = data[8];
					H = data[9];
					sinALT = Math.sin(D) * sinFI + Math.cos(D) * cosFI * Math.cos(H);
					if (sinALT < ELEVATION_LIMIT) G = -10;
				}
			}
		}
		if (G < 0.0) return false;
		return true;
	}

	/**
	 * Calculates the maximum magnitude of a solar eclipse from a given
	 * location.
	 * @param obs The observer.
	 * @return The greatest magnitude. -1 is returned in case the
	 * iteration required for this calculation does not converge. In
	 * that case the eclipse is anyway not visible.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getGreatestMagnitude(ObserverElement obs) throws JPARSECException {
		double lon = obs.getLongitudeRad(), FI = obs.getLatitudeRad(), ALTURA = obs.getHeight();
		double DTU = 0;
		double sinFI = Math.sin(FI), cosFI = Math.cos(FI);
		double FU = Math.atan(FLATENNING_FACTOR * Math.tan(FI));
		double RSIN = FLATENNING_FACTOR * Math.sin(FU) + ALTURA * sinFI / EARTH_RADIUS;
		double RCOS = Math.cos(FU) + ALTURA * cosFI / EARTH_RADIUS;
		// Calculate eclipse maximum
		double data[] = this.eclipseMaximum(0, 0, 0, lon * Constant.RAD_TO_DEG, DTU, 0, RCOS, RSIN);
		double G = -1;
		if (data != null) {
			double U = data[1], V = data[2], L1P = data[3], L2P = data[4];
			double M = FastMath.hypot(U, V);
			G = (L1P - M) / (L1P + L2P);
		}
		return G;
	}

	/**
	 * Returns the position and anchor of the Earths shadow for a given instant.
	 * @param astroUTC The date in UTC.
	 * @return The geographical coordinates of the Earth's shadow, and its anchor
	 * in km in the radius field. Null is returned when there's no shadow.
	 * @throws JPARSECException If an error occurs.
	 */
	public LocationElement getCentralEclipse(AstroDate astroUTC) throws JPARSECException {
		double t[] = this.extremeTimes();
		if (t != null) {
			double jd = astroUTC.jd() + DT / 3600.0;
			AstroDate astroTT = new AstroDate(jd);
			double time = astroTT.getHour() + astroTT.getMinute() / 60.0 + astroTT.getSeconds() / 3600.0;
			double time0 = astro.jd();
			double time1 = ((int) (Math.abs(time0) - 0.5) + 0.5) * FastMath.sign(time0) + T0 / 24.0;
			if (jd > time1 && time < T0) time += 24.0;
			if (jd < time1 && time > T0) time -= 24.0;
			double[] data = centralEclipseCurve(time, 0);
			if (data != null) {
				LocationElement centralPosition = new LocationElement(data[0] * Constant.DEG_TO_RAD, data[1] * Constant.DEG_TO_RAD, data[2]);
				return centralPosition;
			}
		}
		return null;
	}

	/**
	 * Creates an image only with the map of a solar eclipse.
	 * @param outputTimeScale The output time scale to use for labels in the chart.
	 * @param observer The observer. The name will be written at the observer's location
	 * unless that name is set to an empty string.
	 * @param eph0 The ephemeris properties.
	 * @param g2 The Graphics object. Anaglyph mode and color properties are used
	 * to render the map.
	 * @param map Options for the map.
	 * @throws JPARSECException If an error occurs.
	 */
	public void solarEclipseMap(SCALE outputTimeScale, ObserverElement observer, EphemerisElement eph0, Graphics g2,
			PLANET_MAP map) throws JPARSECException {
		map.zoomFactor = 1;
		map.showGrid = false;
		float quality = 1;
		if (!g2.renderingToAndroid() && (RenderPlanet.FORCE_HIGHT_QUALITY || map == PLANET_MAP.MAP_SPHERICAL || g2.renderingToExternalGraphics())) quality = RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR;
		int w = g2.getWidth();

		TimeElement time = new TimeElement(astro, outputTimeScale);
		EphemerisElement eph = eph0.clone();
		eph.correctForEOP = false;
		eph.correctForPolarMotion = false;
		eph.correctEOPForDiurnalSubdiurnalTides = false;
		eph.preferPrecisionInEphemerides = false;

		double jd_TU = TimeScale.getJD(time, observer, eph, SCALE.UNIVERSAL_TIME_UT1);
		double jd = TimeScale.getJD(time, observer, eph, time.timeScale);
		double DTU = (jd - jd_TU) * 24.0;
		double t[] = this.extremeTimes();

		LocationElement cp = map.centralPosition;
		if (map.centralPosition == null && map == PLANET_MAP.MAP_SPHERICAL) {
			double data[] = null;
			if (t != null) data = centralEclipseCurve((t[0] + t[1]) * 0.5, 0);
			if (data != null) {
				map.centralPosition = new LocationElement(data[0] * Constant.DEG_TO_RAD, data[1] * Constant.DEG_TO_RAD, 1.0);
			} else {
				double mmax = -10;
				LocationElement loc0 = null;
				for (int lon = 0; lon < 350; lon = lon + 30) {
					for (int lat = 60; lat >= -70; lat = lat - 30) {
						LocationElement loc = new LocationElement(lon * Constant.DEG_TO_RAD, lat * Constant.DEG_TO_RAD, 1.0);
						double m = this.getGreatestMagnitude(new ObserverElement("NO", loc.getLongitude(), loc.getLatitude(), 0, 0, DST_RULE.NONE));
						if (m > mmax || mmax == -10) {
							loc0 = loc;
							mmax = m;
						}
					}
				}
				if (loc0 != null) map.centralPosition = loc0;
			}
		}
		SatelliteRenderElement render = new SatelliteRenderElement((int) (w*quality), (int) (g2.getHeight()*quality));
		render.showMoon = false;
		render.showSun = false;
		render.showObserver = false;
		render.showDayAndNight = false;
		render.planetMap = map;
		render.anaglyphMode = g2.getAnaglyphMode();
		int c = g2.getColor();
		int c2 = render.planetMap.showGridColor;
		RenderSatellite satRender = new RenderSatellite(time, observer, eph, render);
		if (quality == 1) {
			Graphics g3 = g2.getGraphics(render.width, render.height);
			g3.setFont(g3.getFont());
			satRender.renderize(g3);
			render.planetMap.showGridColor = c;
			this.renderMap(g3, map, satRender, w, DTU, observer, quality, t);
			if (render.anaglyphMode.isReal3D()) {
				g2.setAnaglyph(g3.getImage(0, 0, render.width, render.height), g3.getImage2(0, 0, render.width, render.height));
			} else {
				g2.drawImage(g3.getRendering(), 0, 0);
				g2.drawImage(g3.getRendering(), 0, 0);
			}
/*
			// Vector graphics disabled for planetary rendering here, since image quality is poor. Previous lines can be replaced by following ones
			satRender.renderize(g2);
			render.planetMap.showGridColor = c;
			this.renderMap(g2, map, satRender, w, DTU, observer, quality, t);
*/
		} else {
			Graphics g3 = g2.getGraphics(render.width, render.height);
			g3.setFont(FONT.getDerivedFont(g3.getFont(), g2.getFont().getSize()*2));
			satRender.renderize(g3);
			render.planetMap.showGridColor = c;
			this.renderMap(g3, map, satRender, (int) (w*quality), DTU, observer, w/400f, t);
			if (render.anaglyphMode.isReal3D()) {
				g2.setAnaglyph(g3.getScaledImage(g3.getImage(0, 0, render.width, render.height), w, g2.getHeight(), true, RenderSatellite.ALLOW_SPLINE_RESIZING),
						g3.getScaledImage(g3.getImage2(0, 0, render.width, render.height), w, g2.getHeight(), true, false));
			} else {
				//g2.drawImage(g3.getScaledImage(g3.getRendering(), w, g2.getHeight(), true, RenderSatellite.ALLOW_SPLINE_RESIZING), 0, 0);
				g2.drawImage(g3.getRendering(), 0, 0, 1.0/quality, 1.0/quality);
			}
		}
		render.planetMap.showGridColor = c2;
		map.centralPosition = cp;

		// Draw leyend at the bottom
		int limH = (int)(g2.getWidth()*1.2);
		if (map == PLANET_MAP.MAP_FLAT) limH /= 2;
		if (g2.getHeight() > limH) {
			float transp2 = 96 / 255f, transp3 = 128 / 255f, transp4 = 164 / 255f;
			if (map.EarthMapSource == null) {
				transp2 = (96+32) / 255f;
				transp3 = (128+32) / 255f;
				transp4 = (164+32) / 255f;
			}
			int red0 = 255, green0 = 255, blue0 = 255;
			float tr = transp4;
			int red = (int) (red0 * (1.0 - tr));
			int green = (int) (green0 * (1.0 - tr));
			int blue = (int) (blue0 * (1.0 - tr));
			g2.setColor(red, green, blue, 255);

			int s = 5, s2 = 2*s+1, px = 20, dy = 20, dyt = g2.getFont().getSize()/2;
			g2.fillOval(px-s, limH-s, s2, s2, false);
			g2.drawString(Translate.translate(1121), px + 30, limH + dyt);

			tr = transp3;
			limH += dy;
			red = (int) (red0 * (1.0 - tr));
			green = (int) (green0 * (1.0 - tr));
			blue = (int) (blue0 * (1.0 - tr));
			g2.setColor(red, green, blue, 255);
			g2.fillOval(px-s, limH-s, s2, s2, false);
			g2.drawString(Translate.translate(1122), px + 30, limH + dyt);

			tr = transp2;
			limH += dy;
			red = (int) (red0 * (1.0 - tr));
			green = (int) (green0 * (1.0 - tr));
			blue = (int) (blue0 * (1.0 - tr));
			g2.setColor(red, green, blue, 255);
			g2.fillOval(px-s, limH-s, s2, s2, false);
			g2.drawString(Translate.translate(1123), px + 30, limH + dyt);
		}
	}
	private void renderMap(Graphics g2, PLANET_MAP map, RenderSatellite satRender, int w, double DTU,
			ObserverElement observer, float quality, double times[]) throws JPARSECException {
		Object img = g2.cloneImage(g2.getImage(0, 0, g2.getWidth(), g2.getHeight()));

		float zoom = map.zoomFactor;
		double lon0 = 0, lon1 = Constant.TWO_PI, lonStep = Constant.TWO_PI / (w * zoom);
		double lat1 = -Constant.PI_OVER_TWO, lat0 = -lat1, ALTURA = 0;
		float dist = g2.getAnaglyphMode().getReferenceZ() / 1.15f;

		if (map == PLANET_MAP.MAP_FLAT) {
			LocationElement loc0 = satRender.getGeographicalPosition(0, 0);
			lon0 = loc0.getLongitude();
			lon1 = lon0 + lonStep * w;
			lat0 = loc0.getLatitude();
			lat1 = lat0 - lonStep * w / 2;
		} else {
			lonStep /= 4;
		}
		int ps = (int) (map.zoomFactor/2), ps2 = 2*ps+1; // ps = 0 for max quality
		if (ps2 > 2) lonStep *= (ps2-1.5);
		float transp2 = 96 / 255f, transp3 = 128 / 255f, transp4 = 164 / 255f;
		if (map.EarthMapSource == null) {
			transp2 = (96+32) / 255f;
			transp3 = (128+32) / 255f;
			transp4 = (164+32) / 255f;
		}

		// Darken region where eclipse is visible
		for (double FI = lat1; FI <= lat0; FI = FI + lonStep) {
			double sinFI = Math.sin(FI), cosFI = Math.cos(FI);
			double FU = Math.atan(FLATENNING_FACTOR * Math.tan(FI));
			double RSIN = FLATENNING_FACTOR * Math.sin(FU) + ALTURA * sinFI / EARTH_RADIUS;
			double RCOS = Math.cos(FU) + ALTURA * cosFI / EARTH_RADIUS;
			boolean visible = true;
			for (double lon = lon0; lon <= lon1; lon = lon + lonStep) {
				int pos[] = satRender.getTexturePosition(new LocationElement(lon, FI, 1.0));
				if (pos == null) continue;
				if (pos[0] < 0 || pos[0] >= g2.getWidth()) continue;
				if (pos[1] < 0 || pos[1] >= g2.getHeight()) continue;
				int oldc = g2.getRGB(img, pos[0], pos[1]);
				if (oldc != g2.getRGB(pos[0], pos[1])) continue;

				// Calculate eclipse maximum
				double data[] = this.fastEclipseMaximum(0, 0, 0, lon * Constant.RAD_TO_DEG, DTU, 0, RCOS, RSIN);
				if (data == null) continue;

				double G = -1;
				visible = false;
				double U = data[1], V = data[2], L1P = data[3], L2P = data[4], A = data[5], B = data[6], N = data[7], D = data[8], H = data[9];
				double M = FastMath.hypot(U, V);
				G = (L1P - M) / (L1P + L2P);
				double sinALT = (FastMath.sin(D) * sinFI + FastMath.cos(D) * cosFI * FastMath.cos(H));
				visible = sinALT > ELEVATION_LIMIT;
//					if (sinALT < elevLimit) {
					// Start/end of partial phase (external contacts)
					double TM = data[0];
					double S = (A * V - U * B) / (N * L1P);
					if (S > 1 || S < -1) continue;
					double TAU = L1P / N * Math.sqrt(1.0 - S * S);
					double PC = TM - TAU, UC = TM + TAU;
					// Correction to first contact
					data = this.fastEclipseMaximum(-1, 0, PC, lon * Constant.RAD_TO_DEG, DTU, S, RCOS, RSIN);
					D = data[8];
					H = data[9];
					sinALT = FastMath.sin(D) * sinFI + FastMath.cos(D) * cosFI * FastMath.cos(H);
					// Correction to last contact
					data = this.fastEclipseMaximum(1, 0, UC, lon * Constant.RAD_TO_DEG, DTU, S, RCOS, RSIN);
					D = data[8];
					H = data[9];
					double sinALT2 = FastMath.sin(D) * sinFI + FastMath.cos(D) * cosFI * FastMath.cos(H);
					if (sinALT < ELEVATION_LIMIT) {
						G = -10;
						if (sinALT2 < ELEVATION_LIMIT) G = -1;
					} else {
						if (sinALT2 < ELEVATION_LIMIT) {
							G = -10;
							visible = !visible;
						}
					}
//					}


				if (G == -10) {
					float t = transp3;
					if (visible) t = transp4;

					int red = (int) (g2.getRed(oldc) * (1.0 - t)); // + 0 * transp2);
					int green = (int) (g2.getGreen(oldc) * (1.0 - t)); // + 0 * transp2);
					int blue = (int) (g2.getBlue(oldc) * (1.0 - t)); // + 0 * transp2);
					g2.setColor(red, green, blue, 255);

					if (ps2 <= 1) {
						g2.fillOval(pos[0]-ps, pos[1]-ps, ps2, ps2, false);
					} else {
						g2.fillRect(pos[0]-ps, pos[1]-ps, ps2, ps2);
					}
				} else {
					if (G < 0) continue;
					float t = transp2; //(float) G;
					int red = (int) (g2.getRed(oldc) * (1.0 - t)); // + 0 * transp2);
					int green = (int) (g2.getGreen(oldc) * (1.0 - t)); // + 0 * transp2);
					int blue = (int) (g2.getBlue(oldc) * (1.0 - t)); // + 0 * transp2);
					g2.setColor(red, green, blue, 255);

					if (ps2 <= 1) {
						g2.fillOval(pos[0]-ps, pos[1]-ps, ps2, ps2, false);
					} else {
						g2.fillRect(pos[0]-ps, pos[1]-ps, ps2, ps2);
					}
				}
			}
		}

		img = g2.getImage(0, 0, g2.getWidth(), g2.getHeight());

		// Draw central eclipse region
		double lonStep2 = lonStep;
		if (map == PLANET_MAP.MAP_SPHERICAL) lonStep2 *= 0.5;
		double minLon = -1, maxLon = -1;
		for (double lon = lon0; lon <= lon1; lon = lon + lonStep2) {
			double lats[] = centralEclipseLatitudeLimits(lon * Constant.RAD_TO_DEG);
			if (lats == null) continue;

			boolean total = false; //(val[5] == 1);
			transp2 = (total? 255:156) / 255f;

			int posN[] = satRender.getTexturePosition(new LocationElement(lon, lats[0] * Constant.DEG_TO_RAD, 1.0));
			if (posN == null) continue;
			if (posN[0] < 0 || posN[0] >= g2.getWidth()) continue;
			int posS[] = satRender.getTexturePosition(new LocationElement(lon, lats[1] * Constant.DEG_TO_RAD, 1.0));
			if (posS != null) {
				if (lon < minLon || minLon == -1) minLon = lon;
				if (lon > maxLon || maxLon == -1) maxLon = lon;
				for (int j=posN[1]+(int)ps; j<=posS[1]-(int)ps; j++) {
					int px = posN[0];
					if (map == PLANET_MAP.MAP_SPHERICAL) px = (int) (posN[0]+(posS[0]-posN[0])*(double)(posN[1]-j)/(double)(posN[1]-posS[1]));
					if (j < 0 || j >= g2.getHeight()) continue;
					int oldc = g2.getRGB(img, px, j);
					if (oldc != g2.getRGB(px, j)) continue;

					int red = (int) (g2.getRed(oldc) * (1.0 - transp2)); // + 0 * transp2);
					int green = (int) (g2.getGreen(oldc) * (1.0 - transp2)); // + 0 * transp2);
					int blue = (int) (g2.getBlue(oldc) * (1.0 - transp2)); // + 0 * transp2);
					g2.setColor(red, green, blue, 255);

					if (ps2 <= 1) {
						g2.fillOval(px-ps, j-ps, ps2, ps2, dist);
					} else {
						g2.fillRect(px-ps, j-ps, ps2, ps2, dist);
					}
				}
			}
		}

		// Draw some magnitude lines
		double stepLon = lonStep; //Math.abs(satRender.getGeographicalPosition(0, 0).getLongitude()-satRender.getGeographicalPosition(1, 0).getLongitude())/zoom;
		g2.setFont(Graphics.FONT.getDerivedFont(g2.getFont(), g2.getFont().getSize()+2, Graphics.BOLD));
		g2.setColor(satRender.render.planetMap.showGridColor, false);
		float s2 = 1+ps+(quality-1)*2, s2p = 2*s2+1;
		g2.setStroke(new JPARSECStroke(JPARSECStroke.STROKE_DEFAULT_LINE_THIN, 2.0f));
		if (s2p >= 5) g2.setStroke(JPARSECStroke.STROKE_DEFAULT_LINE_THICK);
		if (s2p <= 3) g2.setStroke(new JPARSECStroke(JPARSECStroke.STROKE_DEFAULT_LINE_THIN, 1.0f));
		ps = (int)(quality-1);
		if (ps < 0) ps = 0;
		ps2 = 2*ps+1;
		ArrayList<String> labels = new ArrayList<String>();
		int dyt = 20;

		// Select correct initial longitude
		double offset = 0;
		double data[] = sameMagnitudeCurve(0.1, -180);
		if (data != null) {
			double phi1 = data[0], phi2 = data[2];
			if (phi1 != -1 || phi2 != -1) offset = Math.PI;
		}
		double stepMag = 0.2;
		if (map.zoomFactor > 3) stepMag = 0.05;

		for (double mag = 0.8; mag >= -0.1; mag = mag - stepMag) {
			boolean magShown1 = false, magShown2 = false;
			for (double lon=-Math.PI+offset; lon<Math.PI+offset; lon=lon+stepLon) {
				if (zoom > 1) {
					int pos[] = satRender.getTexturePosition(new LocationElement(lon, 0, 1.0));
					if (pos == null) continue;
					if (pos[0] < 0 || pos[0] > w) continue;
				}
				data = sameMagnitudeCurve(mag, lon*Constant.RAD_TO_DEG);
				if (data == null) continue;
				double phi1 = data[0], phi2 = data[2];
				if (phi1 == -1 && phi2 == -1) continue;

				if (phi1 != -1) {
					int pos[] = satRender.getTexturePosition(new LocationElement(lon, phi1 * Constant.DEG_TO_RAD, 1.0));
					if (pos != null && (pos[1] > dyt && pos[1] < g2.getHeight())) {
						if (!magShown1 && pos[0] > 40 && pos[0] < w) {
							String l = (int)(0.5+mag*100)+"%";
							g2.drawString(l, pos[0]-20-g2.getStringWidth(l), pos[1]);
							magShown1 = true;
						}
						g2.fillOval(pos[0]-ps, pos[1]-ps, ps2, ps2, dist);
					}
				}
				if (phi2 != -1) {
					int pos[] = satRender.getTexturePosition(new LocationElement(lon, phi2 * Constant.DEG_TO_RAD, 1.0));
					if (pos != null && (pos[1] > 0 && pos[1] < g2.getHeight())) {
						if (!magShown2 && pos[0] > 40 && pos[0] < w && pos[1] > dyt) {
							String l = (int)(0.5+mag*100)+"%";
							g2.drawString(l, pos[0]-g2.getStringWidth(l)-20, pos[1]);
							magShown2 = true;
						}
						g2.fillOval(pos[0]-ps, pos[1]-ps, ps2, ps2, dist);
					}
				}
			}
		}

		int lastx1[] = new int[24];
		int lasty1[] = new int[24];
//		int lastx2[] = new int[24];
//		int lasty2[] = new int[24];
		for (int i=0; i<23; i++) {
			lastx1[i] = -1;
			lasty1[i] = -1;
//			lastx2[i] = -1;
//			lasty2[i] = -1;
		}
/*		double magStep = 0.025; // 0.001
		for (double mag = 1; mag > -magStep; mag = mag - magStep) {
			double oldt1 = -1, oldt2 = -1;
			for (double lon=-Math.PI; lon<Math.PI; lon=lon+stepLon) {
				if (zoom > 1) {
					int pos[] = satRender.getTexturePosition(new LocationElement(lon, 0, 1.0));
					if (pos == null) continue;
					if (pos[0] < 0 || pos[0] > w) continue;
				}
				data = sameMagnitudeCurve(mag, lon*Constant.RAD_TO_DEG);
				if (data == null) continue;
				double phi1 = data[0], phi2 = data[2];
				if (phi1 == -1 && phi2 == -1) continue;
				double newt1 = data[1] + DTU, newt2 = data[3] + DTU;

				if (phi1 != -1) {
					if (oldt1 != -1 && (int) oldt1 != (int) newt1) {
						int pos[] = satRender.getTexturePosition(new LocationElement(lon, phi1 * Constant.DEG_TO_RAD, 1.0));
						if (pos != null && (pos[1] > dyt && pos[1] < g2.getHeight())) {
							int val = (int)newt1;
							if (val < 0) val += 24;
							if (val > 24) val -= 24;
							if (lastx1[val] != -1 && FastMath.hypot(lastx1[val]-pos[0], lasty1[val]-pos[1]) < w/2) g2.drawLine(lastx1[val], lasty1[val], pos[0], pos[1], true);
							lastx1[val] = pos[0];
							lasty1[val] = pos[1];
							String l = val+"^{h}";
							if (pos[0] > 10 && pos[0] < w && pos[1] > 40 && !labels.contains(l) && (mag < magStep || zoom >= 4)) {
								float dx = dyt*((pos[0]-g2.getWidth()/2)*4f)/g2.getWidth();
								float dy = Math.max(dyt, dyt*((pos[1]-g2.getHeight()/2)*4f)/g2.getHeight());
								g2.drawString(l, pos[0]-g2.getStringWidth(l)/2+dx, pos[1]+dy-dyt);
								labels.add(l);
							}
						}
					}
					oldt1 = newt1;
				} else {
					oldt1 = -1;
				}

				if (phi2 != -1) {
					if (oldt2 != -1 && (int) oldt2 != (int) newt2) {
						int pos[] = satRender.getTexturePosition(new LocationElement(lon, phi2 * Constant.DEG_TO_RAD, 1.0));
						if (pos != null && (pos[1] > 0 && pos[1] < g2.getHeight())) {
							int val = (int)newt2;
							if (val < 0) val += 24;
							if (val > 24) val -= 24;
							if (lastx2[val] != -1 && FastMath.hypot(lastx2[val]-pos[0], lasty2[val]-pos[1]) < w/2) g2.drawLine(lastx2[val], lasty2[val], pos[0], pos[1], true);
							lastx2[val] = pos[0];
							lasty2[val] = pos[1];
							String l = val+"^{h}";
							if (pos[0] > 10 && pos[0] < w && !labels.contains(l) && (mag < magStep || zoom >= 4)) {
								float dx = dyt*((pos[0]-g2.getWidth()/2)*4f)/g2.getWidth();
								float dy = Math.max(dyt, dyt*((pos[1]-g2.getHeight()/2)*4f)/g2.getHeight());
								g2.drawString(l, pos[0]-g2.getStringWidth(l)/2+dx, pos[1]+dy+dyt);
								labels.add(l);
							}
						}
					}
					oldt2 = newt2;
				} else {
					oldt2 = -1;
				}
			}
		}
*/

		if (map.zoomFactor <= 3) {
			//g2.setColor(255, 0, 0, 255);
			labels = new ArrayList<String>();
			if (times == null) times = new double[] {this.T0};
			double t0 = ((int) times[0] - DT / 3600.0) + 1;
			boolean getMin = false, getMax = false;
			stepLon *= 0.5;
			if (map == PLANET_MAP.MAP_FLAT) stepLon *= 0.5; //0.75;
			for (double time=t0-3;time<=t0+3;time=time+1.0) {
				double newt1 = time-DTU;
				int val = (int)(time+0.5);
				if (val < 0) val += 24;
				if (val >= 24) val -= 24;
				for (int i=0; i<23; i++) {
					lastx1[i] = -1;
					lasty1[i] = -1;
				}
				int pos[] = null, pymin = -1;
				for (double lon=-Math.PI; lon<Math.PI; lon=lon+stepLon) {
					if (zoom > 1) {
						int pos2[] = satRender.getTexturePosition(new LocationElement(lon, 0, 1.0));
						if (pos2 == null) continue;
						if (pos2[0] < 0 || pos2[0] > w) continue;
					}
					data = sameTimeCurve(newt1, lon*Constant.RAD_TO_DEG, 0.3);
					if (data == null) continue;
					double phi1 = data[0];
					if (!getMin && !getMax) {
						if (phi1 > 0) {
							getMin = true;
						} else {
							getMax = true;
						}
					}
					int pos2[] = satRender.getTexturePosition(new LocationElement(lon, phi1 * Constant.DEG_TO_RAD, 1.0));
					if (pos2 != null && (pos2[1] > 20 && pos2[1] < g2.getHeight())) {
						if (map == PLANET_MAP.MAP_SPHERICAL && lastx1[val] != -1 && FastMath.hypot(lastx1[val]-pos2[0], lasty1[val]-pos2[1])*360.0/(zoom*w) < 40) {
							g2.drawLine(lastx1[val], lasty1[val], pos2[0], pos2[1], false);
						} else {
							g2.fillOval(pos2[0]-ps-1, pos2[1]-ps-1, ps2+2, ps2+2, false);
							g2.fillOval(lastx1[val]-ps-1, lasty1[val]-ps-1, ps2+2, ps2+2, false);
						}
						lastx1[val] = pos2[0];
						lasty1[val] = pos2[1];
						if ((pos2[1] > pymin && getMin || pos2[1] < pymin && getMax) || pymin == -1) {
							pos = pos2;
							pymin = pos2[1];
						}
					}
				}
				if (pos != null && (pos[1] > 20 && pos[1] < g2.getHeight())) {
					String l = val+"^{h}";
					if (pos[0] > 10 && pos[0] < w && pos[1] > 40 && !labels.contains(l)) {
						float dx = dyt*((pos[0]-g2.getWidth()/2)*4f)/g2.getWidth();
						float dy = Math.max(dyt, dyt*((pos[1]-g2.getHeight()/2)*4f)/g2.getHeight());
						dx *= RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR * 0.5;
						dy *= RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR * 0.5;
						g2.drawString(l, pos[0]-g2.getStringWidth(l)/2+dx, pos[1]+dy+dyt);
						labels.add(l);
					}
				}
			}
		}

		// Show observer
		LocationElement loc = new LocationElement(observer.getLongitudeRad(), observer.getLatitudeRad(), 1.0);
		int pos[] = satRender.getTexturePosition(loc);
		if (pos != null) {
			g2.setColor(satRender.render.showObserverColor, true);
			double alt_sun = Constant.PI_OVER_TWO - LocationElement.getApproximateAngularDistance(loc, satRender.locSun);
			if (satRender.render.observerInRedAtNight && alt_sun < -0.75*Constant.DEG_TO_RAD) g2.setColor(255, 0, 0, 255);
			String label = observer.getName();
			if (label != null && !label.equals("")) {
				float ww = g2.getStringWidth(label);
				g2.drawLine(pos[0], pos[1]-5, pos[0], pos[1]-15, true);
				g2.drawString(label, pos[0]-ww/2f, pos[1]-20);
			}
		}
	}

	/**
	 * Returns the maximum of the eclipse.
	 * @param obs Observer.
	 * @param eph0 Ephemeris properties.
	 * @return The maximum.
	 * @throws JPARSECException If an error occurs.
	 */
	public TimeElement solarEclipseMaximum(ObserverElement obs, EphemerisElement eph0) throws JPARSECException {
		double LG = obs.getLongitudeDeg(), FI = obs.getLatitudeDeg(), ALTURA = obs.getHeight();

		TimeElement time0 = new TimeElement(astro, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		EphemerisElement eph = eph0.clone();
		eph.targetBody = TARGET.Moon;
		eph.correctForEOP = false;
		eph.correctForPolarMotion = false;
		eph.correctEOPForDiurnalSubdiurnalTides = false;
		eph.preferPrecisionInEphemerides = false;
		double jd_TDB = TimeScale.getJD(time0, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		double jdRef = (int) (jd_TDB - 0.5) + 0.5;
		double DTU = this.DT;

		// Geocentric coordinates
		double sinFI = Math.sin(FI * Constant.DEG_TO_RAD), cosFI = Math.cos(FI * Constant.DEG_TO_RAD);
		double FU = Math.atan(FLATENNING_FACTOR * Math.tan(FI * Constant.DEG_TO_RAD));
		double RSIN = FLATENNING_FACTOR * Math.sin(FU) + ALTURA * sinFI / EARTH_RADIUS;
		double RCOS = Math.cos(FU) + ALTURA * cosFI / EARTH_RADIUS;

		// Calculate eclipse maximum
		int I = 0, J = 0;
		double T = 0, S = 0;
		double data[] = this.eclipseMaximum(I, J, T, LG, DTU, S, RCOS, RSIN);
		if (data == null) return null;
		double TD = T0 + data[0];
		return new TimeElement(jdRef + TD / 24.0, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
	}

	/**
	 * Creates a chart of a solar eclipse with an scheme of the eclipse and
	 * optionally also the map.
	 * @param outputTimeScale The output time scale for the time labels in the chart.
	 * @param obs The observer.
	 * @param eph0 The ephemeris properties.
	 * @param g The Graphics object.
	 * @param horizontalCoord True to show the chart in horizontal coordinates, false
	 * for equatorial ones.
	 * @param map Options for the eclipse map. null will render no map at all.
	 * @throws JPARSECException If an error occurs.
	 */
	public void renderSolarEclipse(SCALE outputTimeScale, ObserverElement obs, EphemerisElement eph0, Graphics g,
			boolean horizontalCoord, PLANET_MAP map) throws JPARSECException {
		double LG = obs.getLongitudeDeg(), FI = obs.getLatitudeDeg(), ALTURA = obs.getHeight();

		TimeElement time0 = new TimeElement(astro, outputTimeScale);
		EphemerisElement eph = eph0.clone();
		eph.targetBody = TARGET.Moon;
		eph.correctForEOP = false;
		eph.correctForPolarMotion = false;
		eph.correctEOPForDiurnalSubdiurnalTides = false;
		eph.preferPrecisionInEphemerides = false;
		double jd_TDB = TimeScale.getJD(time0, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		double jd = TimeScale.getJD(time0, obs, eph, time0.timeScale);
		double jdRef = (int) (jd_TDB - 0.5) + 0.5;
		double DT = -(jd - jd_TDB) * Constant.SECONDS_PER_DAY;
		double DTU = this.DT;

		// Geocentric coordinates
		double sinFI = Math.sin(FI * Constant.DEG_TO_RAD), cosFI = Math.cos(FI * Constant.DEG_TO_RAD);
		double FU = Math.atan(FLATENNING_FACTOR * Math.tan(FI * Constant.DEG_TO_RAD));
		double RSIN = FLATENNING_FACTOR * Math.sin(FU) + ALTURA * sinFI / EARTH_RADIUS;
		double RCOS = Math.cos(FU) + ALTURA * cosFI / EARTH_RADIUS;

		// Calculate eclipse maximum
		int I = 0, J = 0;
		double T = 0, S = 0;
		double data[] = this.eclipseMaximum(I, J, T, LG, DTU, S, RCOS, RSIN);
		if (data == null) return;
		T = data[0];
		double U = data[1], V = data[2], L1P = data[3], L2P = data[4], A = data[5], B = data[6], N = data[7], D = data[8], H = data[9];
		double TM = T, TD = T0 + T; // , TU=TD-DT/3600; // Eclipse maximum
		double M = FastMath.hypot(U, V);
		double G = (L1P - M) / (L1P + L2P);
		String type = "";
		//if (G < 0) return null; // No eclipse in this place, G = magnitude
		if (G < 0.0) type = "No eclipse";
		if (Math.abs(L2P) > M && L2P < 0) type = "Total";
		if (Math.abs(L2P) > M && L2P > 0) type = "Annular";
		if (Math.abs(L2P) < M) type = "Partial";
		double RATIO = (L1P - L2P) / (L1P + L2P); // diam moon/diam sun
		double PA = Constant.RAD_TO_DEG * Math.atan(U / V);
		if (V < 0) PA += 180; // PA Moon center, from N towards E
		double ALT = Math.asin(Math.sin(D) * sinFI + Math.cos(D) * cosFI * Math.cos(H)); // Sun elevation

		if (horizontalCoord) {
			double x = (sinFI / cosFI) * Math.cos(D) - Math.sin(D) * Math.cos(H);
			double y = Math.sin(H);
			double p = 0.0;
			if (x != 0.0)
			{
				p = Math.atan2(y, x);
			} else {
				p = (y / Math.abs(y)) * Constant.PI_OVER_TWO;
			}
			PA -= p * Constant.RAD_TO_DEG;
		}

		double mPA = PA * Constant.DEG_TO_RAD, mALT = ALT, ME = TD, mR = M;
		double DUR = L2P / N;

		// Start/end of partial phase (external contacts)
		S = (A * V - U * B) / (N * L1P);
		double TAU = L1P / N * Math.sqrt(1.0 - S * S);
		double PC = TM - TAU, UC = TM + TAU;
		// Correction to first contact
		T = PC;
		J = 0;
		I = -1;
		data = this.eclipseMaximum(I, J, T, LG, DTU, S, RCOS, RSIN);
		boolean first = true;
		double fPA = 0, fALT = 0, fTD = 0;
		if (data == null) {
			first = false;
		} else {
			T = data[0];
			U = data[1];
			V = data[2];
			L1P = data[3];
			L2P = data[4];
			A = data[5];
			B = data[6];
			N = data[7];
			D = data[8];
			H = data[9];
			TD = T0 + T;
			// TU=TD-DT/3600;
			ALT = Math.asin(Math.sin(D) * sinFI + Math.cos(D) * cosFI * Math.cos(H));
			PA = Constant.RAD_TO_DEG * Math.atan(U / V);
			if (V < 0) PA += 180; // PA Moon center, from N towards E

			if (horizontalCoord) {
				double x = (sinFI / cosFI) * Math.cos(D) - Math.sin(D) * Math.cos(H);
				double y = Math.sin(H);
				double p = 0.0;
				if (x != 0.0)
				{
					p = Math.atan2(y, x);
				} else {
					p = (y / Math.abs(y)) * Constant.PI_OVER_TWO;
				}
				PA -= p * Constant.RAD_TO_DEG;
			}

			fPA = PA * Constant.DEG_TO_RAD;
			fALT = ALT;
			fTD = TD;
		}

		// Correction to last contact
		T = UC;
		J = 0;
		I = 1;
		data = this.eclipseMaximum(I, J, T, LG, DTU, S, RCOS, RSIN);
		boolean last = true;
		double lPA = 0, lALT = 0, lTD = 0;
		if (data == null) {
			last = false;
		} else {
			T = data[0];
			U = data[1];
			V = data[2];
			L1P = data[3];
			L2P = data[4];
			A = data[5];
			B = data[6];
			N = data[7];
			D = data[8];
			H = data[9];
			TD = T0 + T;
			// TU=TD-DT/3600;
			ALT = Math.asin(Math.sin(D) * sinFI + Math.cos(D) * cosFI * Math.cos(H));
			PA = Constant.RAD_TO_DEG * Math.atan(U / V);
			if (V < 0) PA += 180; // PA Moon center, from N towards E

			if (horizontalCoord) {
				double x = (sinFI / cosFI) * Math.cos(D) - Math.sin(D) * Math.cos(H);
				double y = Math.sin(H);
				double p = 0.0;
				if (x != 0.0)
				{
					p = Math.atan2(y, x);
				} else {
					p = (y / Math.abs(y)) * Constant.PI_OVER_TWO;
				}
				PA -= p * Constant.RAD_TO_DEG;
			}

			lPA = PA * Constant.DEG_TO_RAD;
			lALT = ALT;
			lTD = TD;
		}

		// Charting
		g.setColor(0, 0, 0, 255);
		g.fillRect(0, 0, g.getWidth(), g.getHeight());
		int w = g.getWidth(), h = g.getHeight();
		g.setFont(FONT.DIALOG_BOLD_11);
		int bestSize = (w * 11) / 600;
		if (bestSize > 11) g.setFont(FONT.getDerivedFont(g.getFont(), bestSize));
		boolean horMap = false;
		if (map != null && map != PLANET_MAP.NO_MAP) {
			if (w < 3 * h) {
				if (map == PLANET_MAP.MAP_SPHERICAL && h > w) {
					h = h - w;
				} else {
					h = h - w / 2;
				}
			} else {
				if (map == PLANET_MAP.MAP_SPHERICAL) {
					w = w - h;
				} else {
					w = w - h * 2;
				}
				horMap = true;
			}
		}
		int cx = w / 2, cy = h / 2, cr = ((Math.min(cx, cy) * 4) / 14);
		g.setColor(128, 128, 128, 128);
		g.drawLine(cx, 30, cx, h - 70, true);
		g.drawLine(30, cy, w - 30, cy, true);
		g.setColor(255, 255, 255, 128);
		int fs = g.getFont().getSize();
		if (horizontalCoord) {
			g.drawString("Z", cx - fs / 3, 20);
		} else {
			g.drawString("N", cx - fs / 3, 20);
			g.drawString("E", 15, cy + fs / 3);
		}
		g.setColor(255, 255, 0, 255);

		double P = 1.0;
		S = RATIO;
		int sp = (int) (0.5 + (P * cr) / P), sp2 = 2 * sp + 1;
		g.fillOval(cx - sp, cy - sp, sp2, sp2, false);
		int ss = (int) (0.5 + (S * cr) / P), ss2 = 2 * ss + 1;

		int radius = sp + ss;
		float pxf = 0, pyf = 0, pxl = 0, pyl = 0, pxc, pyc;
		float MoonDist = g.getAnaglyphMode().getReferenceZ() / 1.15f;
		if (G > 0) {
			if (first) {
				pxf = cx - (float) (radius * Math.sin(fPA));
				pyf = cy - (float) (radius * Math.cos(fPA));
				g.setColor(0, 0, 0, 192);
				renderMoon(pxf - ss, pyf - ss, ss2, MoonDist, g, jdRef + fTD / 24.0, obs, eph, !horizontalCoord);
				g.setColor(255, 255, 255, 192);
				g.drawOval(pxf - ss, pyf - ss, ss2, ss2, MoonDist);
			}
			if (last) {
				pxl = cx - (float) (radius * Math.sin(lPA));
				pyl = cy - (float) (radius * Math.cos(lPA));
				g.setColor(0, 0, 0, 192);
				renderMoon(pxl - ss, pyl - ss, ss2, MoonDist, g, jdRef + lTD / 24.0, obs, eph, !horizontalCoord);
				g.setColor(255, 255, 255, 192);
				g.drawOval(pxl - ss, pyl - ss, ss2, ss2, MoonDist);
			}
			if (first && last) {
				double jd_TDB2 = (int) (jd_TDB - 0.5) + 0.5 + TM / 24.0;
				double sun[] = OrbitEphem.sun(jd_TDB2);
				double sunDiam = Constant.RAD_TO_DEG * Math.atan2(TARGET.SUN.equatorialRadius * 2, Constant.AU *
						Math.sqrt(sun[0] * sun[0] + sun[1] * sun[1] + sun[2] * sun[2]));

				double rpix = mR * sp2 / sunDiam;
				pxc = (int) (0.5 + cx - rpix * Math.sin(mPA));
				pyc = (int) (0.5 + cy - rpix * Math.cos(mPA));

				double m1 = (pyc - pyl) / (pxc - pxl), n1 = pyl - m1 * pxl;

				double m2 = (pyc - pyf) / (pxc - pxf), n2 = pyf - m2 * pxf;
				double offx = (ss + 20) / Math.max(1.0, Math.abs(m2));
				double size = (15.0 * w / Math.max(1.0, Math.abs(m1))) / 800.0;
				double endx = pxf + offx, endy = m2 * endx + n2;
				g.drawLine((float) (endx), (float) (endy), (float) (pxc),
						(float) (pyc), false);

				double startx = pxc, starty = pyc;
				offx = (ss + 20) / Math.max(1.0, Math.abs(m1));
				endx = pxl - offx;
				endy = m1 * endx + n1;
				double endx1 = endx - size, endy1 = m1 * endx1 + n1;
				double dx = (endx1 - endx) / 2, dy = (endy1 - endy) / 2;
				g.drawLine((float) (endx), (float) (endy), (float) (startx),
						(float) (starty), false);
				g.drawLine((float) (endx - dy), (float) (endy + dx),
						(float) (endx + dy), (float) (endy - dx), false);
				g.drawLine((float) (endx - dy), (float) (endy + dx),
						(float) (endx1), (float) (endy1), false);
				g.drawLine((float) (endx1), (float) (endy1), (float) (endx + dy),
						(float) (endy - dx), false);

				g.setColor(0, 0, 0, 240);
				renderMoon(pxc - ss, pyc - ss, ss2, MoonDist, g, jdRef + ME / 24.0, obs, eph, !horizontalCoord);
				g.setColor(255, 255, 255, 192);
				g.drawOval(pxc - ss, pyc - ss, ss2, ss2, MoonDist);
			}
		}

		int px = 10, py = 10, px2 = cx + 20, step = g.getFont().getSize() + 2;
		g.setColor(255, 255, 255, 255);
		if (type.equals("No eclipse") || G < 0) {
			String label = Translate.translate(1089);
			if (G < 0) label += " "+Translate.translate(1090)+" "+obs.getName();
			g.drawString(label, 20, py+=step);
			if (map == null || map == PLANET_MAP.NO_MAP) return;
		}

		if (w >= 400 && !type.equals("No eclipse") && G > 0) {
			String ts = TimeElement.TIME_SCALES_ABBREVIATED[time0.timeScale.ordinal()];
			py = h - step * 5;
			g.drawString(Translate.translate(type+" solar eclipse on")+" "+time0.astroDate.toStringDate(false)+" "+Translate.translate(1090)+" "+obs.getName(), px, py+=step);
			g.setFont(FONT.DIALOG_PLAIN_11);
			if (bestSize > 11) g.setFont(FONT.getDerivedFont(g.getFont(), bestSize));
			py+=step/2;

			String ts2 = ts;
			if (ShowWithoutLT && (ts.equals("TL") || ts.equals("LT"))) ts2 = "";

			// Improve accuracy and consistency of output times (max and start/end of partiality), even in Android
			MoonEventElement[] events = null;
//			if (!g.renderingToAndroid()) {
				SolarEclipse se = new SolarEclipse(new TimeElement(jd_TDB-1, SCALE.BARYCENTRIC_DYNAMICAL_TIME), obs, eph);
				events = se.getEvents();
				if (ShowWithoutLT) {
					g.drawString(Translate.translate(1094)+": "+Functions.formatRA((ME-DT/3600.)/Constant.RAD_TO_HOUR, 0)+" "+ts2+" ("+Translate.translate(157)+": "+Functions.formatValue(G, 2)+")", px, py+=step);
				} else {
					g.drawString(Translate.translate(1094)+": "+Functions.formatRA((ME-DT/3600.)/Constant.RAD_TO_HOUR, 0)+" "+ts2, px, py+=step);
				}
//			} else {
//				g.drawString(Translate.translate(1094)+": "+Functions.formatRA((ME-DT/3600.)/Constant.RAD_TO_HOUR, 0)+" "+ts, px, py+=step);
//			}

			g.drawString(Translate.translate(1095)+": "+Functions.formatValue(Ephem.getApparentElevation(eph0, obs, mALT, 10)*Constant.RAD_TO_DEG, 1)+"\u00b0", px, py+=step);
			if (G >= 0) {
				py = 10;
				if (first && last) {
					if (events != null) {
						double jdStart = TimeScale.getJD(new TimeElement(events[0].startTime, SCALE.BARYCENTRIC_DYNAMICAL_TIME), obs, eph, outputTimeScale);
						double jdEnd = TimeScale.getJD(new TimeElement(events[0].endTime, SCALE.BARYCENTRIC_DYNAMICAL_TIME), obs, eph, outputTimeScale);
						jdStart = (jdStart + 0.5 - (int)(jdStart + 0.5)) * 24.0;
						jdEnd = (jdEnd + 0.5 - (int)(jdEnd + 0.5)) * 24.0;
						g.drawString(Translate.translate(1096)+": "+Functions.formatRA(jdStart/Constant.RAD_TO_HOUR, 0)+" "+ts2, px2, py+=step);
						g.drawString(Translate.translate(1097)+": "+Functions.formatRA(jdEnd/Constant.RAD_TO_HOUR, 0)+" "+ts2, px, py);
					} else {
						g.drawString(Translate.translate(1096)+": "+Functions.formatRA((fTD-DT/3600.)/Constant.RAD_TO_HOUR, 0)+" "+ts, px2, py+=step);
						g.drawString(Translate.translate(1097)+": "+Functions.formatRA((lTD-DT/3600.)/Constant.RAD_TO_HOUR, 0)+" "+ts, px, py);
					}
					//py+=step/2;
					g.drawString(Translate.translate(1095)+": "+Functions.formatValue(Ephem.getApparentElevation(eph0, obs, fALT, 10)*Constant.RAD_TO_DEG, 1)+"\u00b0", px2, py+=step);
					g.drawString(Translate.translate(1095)+": "+Functions.formatValue(Ephem.getApparentElevation(eph0, obs, lALT, 10)*Constant.RAD_TO_DEG, 1)+"\u00b0", px, py);
					py+=step/2;
				}
				if (!ShowWithoutLT) {
					g.drawString(Translate.translate(1098)+": "+Functions.formatValue(G, 3), px, py+=step);
				}
				if (first && last && (type.equals("Total") || type.equals("Annular")))
					g.drawString(Translate.translate(type+" phase duration")+": "+Functions.formatValue(7200*DUR, 1)+"s", px, py+=step);
			}
		}

		if (map != null && map != PLANET_MAP.NO_MAP) {
			if ((type.equals("No eclipse") || !this.isVisible(obs) || G < 0) && map != PLANET_MAP.MAP_SPHERICAL) map.zoomFactor = 1;
			int mapx = 0, mapy = h;
			if (horMap) {
				mapx = w;
				mapy = 0;
			}
			g.traslate(mapx, mapy);
			g.setColor(255, 0, 0, 255);

			this.solarEclipseMap(time0.timeScale, obs, eph0, g, map);
			g.traslate(-mapx, -mapy);
		}
		return;
	}

	private double[] eclipseMaximum(int I, int J, double T, double LG, double DT, double S,
			double RCOS, double RSIN)
	{
		if (S < -1 || S > 1) return null;
		double sqS = Math.sqrt(1.0 - S * S);
		do {
			J ++;
			double T2 = T * T, T3 = T2 * T;
			double X = X0 + X1 * T + X2 * T2 + X3 * T3;
			double Y = Y0 + Y1 * T + Y2 * T2 + Y3 * T3;
			double D = D0 + D1 * T + D2 * T2;
			double M = M0 + M1 * T;
			// Daily variations
			double VX = X1 + 2 * X2 * T + 3 * X3 * T2;
			double VY = Y1 + 2 * Y2 * T + 3 * Y3 * T2;
			double H = M + LG - DT_TO_DEG * DT;
			D *= Constant.DEG_TO_RAD;
			H *= Constant.DEG_TO_RAD;
			double sinD = Math.sin(D), cosD = Math.cos(D);
			double sinH = Math.sin(H), cosH = Math.cos(H);
			double P = RCOS * sinH;
			double Q = RSIN * cosD - RCOS * cosH * sinD;
			double R = RSIN * sinD + RCOS * cosH * cosD;
			double VP = Constant.DEG_TO_RAD * M1 * RCOS * cosH;
			double VQ = Constant.DEG_TO_RAD * (M1 * P * sinD - R * D1);
			double U = X - P, V = Y - Q;
			double A = VX - VP, B = VY - VQ;
			double N = FastMath.hypot(A, B);
			double L1 = L10 + L11 * T + L12 * T2;
			double L2 = L20 + L21 * T + L22 * T2;
			double L1P = L1 - R * F1;
			double L2P = L2 - R * F2;
			double TAU = -(U * A + V * B) / (N * N) + I * L1P * sqS / N;
			T += TAU;
			if (J > 20) return null; // new double[] {T, U, V, L1P, L2P}; // No convergency
			if (Math.abs(TAU) > 1E-6) continue;
			return new double[] {T, U, V, L1P, L2P, A, B, N, D, H};
		} while (true);
	}

	private double[] fastEclipseMaximum(int I, int J, double T, double LG, double DT, double S,
			double RCOS, double RSIN)
	{
		if (S < -1 || S > 1) return null;
		double sqS = Math.sqrt(1.0 - S * S);
		do {
			J ++;
			double T2 = T * T, T3 = T2 * T;
			double X = X0 + X1 * T + X2 * T2 + X3 * T3;
			double Y = Y0 + Y1 * T + Y2 * T2 + Y3 * T3;
			double D = D0 + D1 * T + D2 * T2;
			double M = M0 + M1 * T;
			// Daily variations
			double VX = X1 + 2 * X2 * T + 3 * X3 * T2;
			double VY = Y1 + 2 * Y2 * T + 3 * Y3 * T2;
			double H = M + LG - DT_TO_DEG * DT;
			D *= Constant.DEG_TO_RAD;
			H *= Constant.DEG_TO_RAD;
			double sinD = FastMath.sin(D), cosD = FastMath.cos(D);
			double sinH = FastMath.sin(H), cosH = FastMath.cos(H);
			double P = RCOS * sinH;
			double Q = RSIN * cosD - RCOS * cosH * sinD;
			double R = RSIN * sinD + RCOS * cosH * cosD;
			double VP = Constant.DEG_TO_RAD * M1 * RCOS * cosH;
			double VQ = Constant.DEG_TO_RAD * (M1 * P * sinD - R * D1);
			double U = X - P, V = Y - Q;
			double A = VX - VP, B = VY - VQ;
			double N = FastMath.hypot(A, B);
			double L1 = L10 + L11 * T + L12 * T2;
			double L2 = L20 + L21 * T + L22 * T2;
			double L1P = L1 - R * F1;
			double L2P = L2 - R * F2;
			double TAU = -(U * A + V * B) / (N * N) + I * L1P * sqS / N;
			T += TAU;
			if (J > 20) return null; // new double[] {T, U, V, L1P, L2P}; // No convergency
			if (Math.abs(TAU) > 1E-6) continue;
			return new double[] {T, U, V, L1P, L2P, A, B, N, D, H};
		} while (true);
	}

	/**
	 * Creates a chart with a scheme of a lunar eclipse and optionally also
	 * the eclipse map.
	 * @param time0 The time object with the correct date (just date) of the eclipse.
	 * The time scale of this object will be also used for the time labels in the chart.
	 * @param obs The observer.
	 * @param eph0 The ephemeris properties.
	 * @param g The Graphics object.
	 * @param horizontalCoord True to show the chart in horizontal coordinates, false
	 * for equatorial ones.
	 * @param map Options for the eclipse map. null will render no map at all.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void renderLunarEclipse(TimeElement time0, ObserverElement obs, EphemerisElement eph0, Graphics g,
			boolean horizontalCoord, PLANET_MAP map) throws JPARSECException {
		EphemerisElement eph = eph0.clone();
		eph.correctForEOP = false;
		eph.correctForPolarMotion = false;
		eph.correctEOPForDiurnalSubdiurnalTides = false;
		eph.preferPrecisionInEphemerides = false;
		eph.isTopocentric = false;
		eph.ephemType = COORDINATES_TYPE.APPARENT;
		eph.equinox = EphemerisElement.EQUINOX_OF_DATE;
		eph.targetBody = TARGET.Moon;

		double eclipseJD = MainEvents.MoonPhaseOrEclipse(time0.astroDate.jd(), EVENT.MOON_LUNAR_ECLIPSE, EVENT_TIME.CLOSEST).time;
		TimeElement time = new TimeElement(eclipseJD, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		time.add(-8.0 / 24.0);
		LunarEclipse se = new LunarEclipse(time, obs, eph);
		MoonEventElement me[] = se.getEvents();
		if (me == null || me.length == 0) throw new JPARSECException("No eclipse found.");
		AstroDate start = new AstroDate(me[me.length-1].startTime);
		double jd_TDB = start.jd();
		double jd = TimeScale.getJD(new TimeElement(start, SCALE.BARYCENTRIC_DYNAMICAL_TIME), obs, eph, time0.timeScale);
		double DT = -(jd - jd_TDB) * Constant.SECONDS_PER_DAY;
		start.setDayFraction(0);
		double jd0 = start.jd();
		time = new TimeElement(start, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		eph.targetBody = TARGET.SUN;
		EphemElement sun = Ephem.getEphemeris(time, obs, eph, false);
		eph.targetBody = TARGET.Moon;
		EphemElement moon = Ephem.getEphemeris(time, obs, eph, false);
		double AA = moon.rightAscension * Constant.RAD_TO_HOUR, CC = sun.rightAscension * Constant.RAD_TO_HOUR,
				KK = sun.declination * Constant.RAD_TO_DEG;
		time.add(1.0);
		eph.targetBody = TARGET.SUN;
		EphemElement sunp = Ephem.getEphemeris(time, obs, eph, false);
		eph.targetBody = TARGET.Moon;
		EphemElement moonp = Ephem.getEphemeris(time, obs, eph, false);
		double BB = moonp.rightAscension * Constant.RAD_TO_HOUR, DD = sunp.rightAscension * Constant.RAD_TO_HOUR,
				LL = sunp.declination * Constant.RAD_TO_DEG;

		CC = 24.0 * ((CC + 12.0) / 24.0 - Math.floor((CC + 12.0) / 24.0));
		DD = 24.0 * ((DD + 12.0) / 24.0 - Math.floor((DD + 12.0) / 24.0));
		double H = Math.floor(24.0 * (CC - AA) / (BB - AA - DD + CC));
		time.add(-1.0 + H / 24.0);
		eph.targetBody = TARGET.Moon;
		EphemElement moonh = Ephem.getEphemeris(time, obs, eph, false);
		AA = moonh.rightAscension * Constant.RAD_TO_HOUR;
		double PP = moonh.declination * Constant.RAD_TO_DEG;

		double jdTDB = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME), stepH = Math.max(1, 0.125+(se.getEclipseMaximum() - jdTDB) * 24);
		if (se.getEclipseType().toLowerCase().equals("total") && se.getEclipseMaximum() < jdTDB) stepH = 0.125;
		time.add(stepH / 24.0);
		eph.targetBody = TARGET.SUN;
		EphemElement sunhp = Ephem.getEphemeris(time, obs, eph, false);
		eph.targetBody = TARGET.Moon;
		EphemElement moonhp = Ephem.getEphemeris(time, obs, eph, false);
		BB = moonhp.rightAscension * Constant.RAD_TO_HOUR;
		double NN = moonhp.declination * Constant.RAD_TO_DEG;
		double T = (AA - CC - (BB - AA) * (H)) / ((DD - CC) / 24.0 - BB + AA); // Time of oposition in RA, TDB

		double S1 = moonhp.angularRadius * Constant.RAD_TO_DEG * 60.0,
				P1 = Math.asin(Constant.EARTH_RADIUS / (Constant.AU * moonhp.distance))	* Constant.RAD_TO_DEG * 60.0;
		double R2 = sunhp.distance, S2 = sunhp.angularRadius * Constant.RAD_TO_DEG * 60.0, P2 = 8.793999 / R2;

		// Using Chauvenet enlargement method slightly corrected (1.8% instead
		// of 2%). See http://eclipse.gsfc.nasa.gov/LEcat5/shadow.html
		double S = (P1 + P2 / 60.0 - S2) * 50.9 / 50.0; // Umbra size in '
		double P = (P1 + P2 / 60.0 + S2) * 50.9 / 50.0; // Penumbra size in '

		double Q = ((NN - PP) * (T - Math.floor(T)) + PP + KK + (LL - KK) * T / 24) * 60;
		double O = ((DD - CC) / 24 - BB + AA) * 900;
		double U = (NN - PP + (LL - KK) / 24) * 60;
		double V = O * O + U * U, W = Q * U, W2 = W * W;
		double D = Math.abs(O * Q) / Math.sqrt(V);

		time.add((-H-1.0+T) / 24.0);
		double sinFI = Math.sin(obs.getLatitudeRad()), cosFI = Math.cos(obs.getLatitudeRad());
		double tsl = SiderealTime.apparentSiderealTime(time, obs, eph);
		EphemElement moont = Ephem.getEphemeris(time, obs, eph, false);
		double moonRA = moont.rightAscension;
		double moonDEC = moont.declination;

		int I = -1;
		double X = 0, P0[] = new double[3], F0[] = new double[3], PARF[] = new double[3], PARP[] = new double[3], F = 0; // , Z = 0, PC = 0, UC = 0;
		String type = "";
		if (D <= S - S1) {
			type = "Total";
			X = Q * Q - (S - S1) * (S - S1);

			// Start/end times of lunar eclipse phases
			I++; // Counter for the different phases
			P0[I] = T + (-W - Math.sqrt(W2 - V * X)) / V; // Start
			F0[I] = T + (-W + Math.sqrt(W2 - V * X)) / V; // End

			if (horizontalCoord) {
				double A = O * (F0[I] - T);
				double B = (Q + U * (F0[I] - T));
				double moonHA = tsl + (F0[I] - T) * Constant.TWO_PI * Constant.SIDEREAL_DAY_LENGTH / 24.0 - (moonRA - A * Constant.DEG_TO_RAD / 60.0);
				double moonD = moonDEC - B * Constant.DEG_TO_RAD / 60.0;
				double x = (sinFI / cosFI) * Math.cos(moonD) - Math.sin(moonD) * Math.cos(moonHA);
				double y = Math.sin(H);
				double p = 0.0;
				if (x != 0.0)
				{
					p = Math.atan2(y, x);
				} else {
					p = (y / Math.abs(y)) * Constant.PI_OVER_TWO;
				}
				PARF[I] = -p;

				A = O * (P0[I] - T);
				B = (Q + U * (P0[I] - T));
				moonHA = tsl + (P0[I] - T) * Constant.TWO_PI * Constant.SIDEREAL_DAY_LENGTH / 24.0 - (moonRA - A * Constant.DEG_TO_RAD / 60.0);
				moonD = moonDEC - B * Constant.DEG_TO_RAD / 60.0;
				x = (sinFI / cosFI) * Math.cos(moonD) - Math.sin(moonD) * Math.cos(moonHA);
				y = Math.sin(moonHA);
				p = 0.0;
				if (x != 0.0)
				{
					p = Math.atan2(y, x);
				} else {
					p = (y / Math.abs(y)) * Constant.PI_OVER_TWO;
				}
				PARP[I] = -p;
			}
		}
		if (D < S + S1) {
			if (type.equals("")) type = "Partial";
			X = Q * Q - (S + S1) * (S + S1);

			I++;
			P0[I] = T + (-W - Math.sqrt(W2 - V * X)) / V;
			F0[I] = T + (-W + Math.sqrt(W2 - V * X)) / V;

			if (horizontalCoord) {
				double A = O * (F0[I] - T);
				double B = (Q + U * (F0[I] - T));
				double moonHA = tsl + (F0[I] - T) * Constant.TWO_PI * Constant.SIDEREAL_DAY_LENGTH / 24.0 - (moonRA - A * Constant.DEG_TO_RAD / 60.0);
				double moonD = moonDEC - B * Constant.DEG_TO_RAD / 60.0;
				double x = (sinFI / cosFI) * Math.cos(moonD) - Math.sin(moonD) * Math.cos(moonHA);
				double y = Math.sin(moonHA);
				double p = 0.0;
				if (x != 0.0)
				{
					p = Math.atan2(y, x);
				} else {
					p = (y / Math.abs(y)) * Constant.PI_OVER_TWO;
				}
				PARF[I] = -p;

				A = O * (P0[I] - T);
				B = (Q + U * (P0[I] - T));
				moonHA = tsl + (P0[I] - T) * Constant.TWO_PI * Constant.SIDEREAL_DAY_LENGTH / 24.0 - (moonRA - A * Constant.DEG_TO_RAD / 60.0);
				moonD = moonDEC - B * Constant.DEG_TO_RAD / 60.0;
				x = (sinFI / cosFI) * Math.cos(moonD) - Math.sin(moonD) * Math.cos(moonHA);
				y = Math.sin(moonHA);
				p = 0.0;
				if (x != 0.0)
				{
					p = Math.atan2(y, x);
				} else {
					p = (y / Math.abs(y)) * Constant.PI_OVER_TWO;
				}
				PARP[I] = -p;
			}
		}
		if (D < P + S1) {
			if (type.equals("")) type = "Penumbral";

			// Contact angle and phase
			double Z = S;
			if (type.equals("Penumbral")) Z = P;
			F = (Z + S1 - D) / (2 * S1);
			/*
			 * double HH = Math.asin(D / (Z + S1)), JJ = Math.acos(U / Math.sqrt(V));
			 * if (O * Q > 0) HH = -HH;
			 * PC = (JJ + HH); // First contact, E
			 * UC = Math.PI - JJ + HH; // Last contact, W
			 */

			// Penumbral phase
			X = Q * Q - (P + S1) * (P + S1);

			I++;
			P0[I] = T + (-W - Math.sqrt(W2 - V * X)) / V;
			F0[I] = T + (-W + Math.sqrt(W2 - V * X)) / V;

			if (horizontalCoord) {
				double A = O * (F0[I] - T);
				double B = (Q + U * (F0[I] - T));
				double moonHA = tsl + (F0[I] - T) * Constant.TWO_PI * Constant.SIDEREAL_DAY_LENGTH / 24.0 - (moonRA - A * Constant.DEG_TO_RAD / 60.0);
				double moonD = moonDEC - B * Constant.DEG_TO_RAD / 60.0;
				double x = (sinFI / cosFI) * Math.cos(moonD) - Math.sin(moonD) * Math.cos(moonHA);
				double y = Math.sin(moonHA);
				double p = 0.0;
				if (x != 0.0)
				{
					p = Math.atan2(y, x);
				} else {
					p = (y / Math.abs(y)) * Constant.PI_OVER_TWO;
				}
				PARF[I] = -p;

				A = O * (P0[I] - T);
				B = (Q + U * (P0[I] - T));
				moonHA = tsl + (P0[I] - T) * Constant.TWO_PI * Constant.SIDEREAL_DAY_LENGTH / 24.0 - (moonRA - A * Constant.DEG_TO_RAD / 60.0);
				moonD = moonDEC - B * Constant.DEG_TO_RAD / 60.0;
				x = (sinFI / cosFI) * Math.cos(moonD) - Math.sin(moonD) * Math.cos(moonHA);
				y = Math.sin(moonHA);
				p = 0.0;
				if (x != 0.0)
				{
					p = Math.atan2(y, x);
				} else {
					p = (y / Math.abs(y)) * Constant.PI_OVER_TWO;
				}
				PARP[I] = -p;
			}
		}
		if (type.equals("")) type = "No eclipse";

		// Charting
		float dist = g.getAnaglyphMode().getReferenceZ() / 1.15f;
		g.setColor(0, 0, 0, 255);
		g.fillRect(0, 0, g.getWidth(), g.getHeight());
		eph.isTopocentric = true;
		TimeElement timeMax = time.clone();
		time.add((-T) / 24.0);
		int w = g.getWidth(), h = g.getHeight();
		g.setFont(FONT.DIALOG_BOLD_11);
		int bestSize = (int) ((w * 11.0) / 700.0);
		if (bestSize > 11) g.setFont(FONT.getDerivedFont(g.getFont(), bestSize));
		boolean horMap = false;
		if (map != null && map != PLANET_MAP.NO_MAP) {
			if (w < 3 * h) {
				if (map == PLANET_MAP.MAP_SPHERICAL && h > w) {
					h = h - w;
				} else {
					h = h - w / 2;
				}
			} else {
				if (map == PLANET_MAP.MAP_SPHERICAL) {
					w = w - h;
				} else {
					w = w - h * 2;
				}
				horMap = true;
			}
		}
		int cx = w / 2, cy = h / 2, cr = ((Math.min(cx, cy) * 2) / 3);
		g.setColor(128, 128, 128, 128);
		g.drawLine(cx, 30, cx, cy + cr + 10, true);
		g.drawLine(30, cy, w - 30, cy, true);
		g.setColor(255, 255, 255, 128);
		int fs = g.getFont().getSize();
		if (horizontalCoord) {
			g.drawString("Z", cx - fs / 3, 20);
		} else {
			g.drawString("N", cx - fs / 3, 20);
			g.drawString("E", 15, cy + fs / 3);
		}
		g.setColor(0, 0, 255, 128);
		int sp = (int) ((P * cr) / P), sp2 = 2 * sp + 1;
		g.fillOval(cx - sp, cy - sp, sp2, sp2, false);
		if (g.getAnaglyphMode().isReal3D()) {
			g.setColor(255, 0, 0, 255);
		} else {
			g.setColor(255, 0, 0, 128);
		}
		int ss = (int) ((S * cr) / P), ss2 = 2* ss + 1;
		g.fillOval(cx - ss, cy - ss, ss2, ss2, false);
		g.setColor(255, 255, 255, 128);
		double extend = cr / P, ext2 = (1.2 * extend);
		if (!horizontalCoord) {
			double endx = (cx + O * extend * ext2), endy = (cy - ((Q + U * ext2) * extend));
			g.drawLine((float) (cx - O * extend * ext2), (float) (cy - ((Q - U * ext2) * extend)), (float) endx, (float) endy, false);
			ext2 += 0.2;
			double endx1 = (cx + O * extend * ext2), endy1 = (cy - ((Q + U * ext2) * extend));
			double dx = (endx1 - endx) / 2, dy = (endy1 - endy) / 2;
			g.drawLine((float) (endx - dy), (float) (endy + dx), (float) (endx + dy), (float) (endy - dx), false);
			g.drawLine((float) (endx - dy), (float) (endy + dx), (float) (endx1), (float) (endy1), false);
			g.drawLine((float) (endx1), (float) (endy1), (float) (endx + dy), (float) (endy - dx), false);

			/*
			for (int i = -3; i <= 3; i++) {
				g.drawLine((float) (cx + O * i * extend), (float) (cy - ((Q + U * i) * extend) + extend), (float) (cx + O * i * extend), (float) (cy - ((Q + U * i) * extend) - extend), false);
			}
			*/
		} else {
			if (ShowWithoutLT && I > 0) {
				double lastx = -1, lasty = -1, dx = 0, dy = 0, dr = 0, dang = 0;
				double finalx = -1, finaly = -1;
				
				double ME = (P0[0] + F0[0]) / 2.0;
				double A = O * (ME - T) * extend, B = (Q + U * (ME - T)) * extend;
				double PARME = (PARP[0] + PARF[0]) / 2.0;
				dx = A;
				dy = -B;
				dr = FastMath.hypot(dx, dy);
				dang = FastMath.atan2_accurate(-dy, dx) + PARME;
				dx = cx + (float) (dr * Math.cos(dang));
				dy = cy - (float) (dr * Math.sin(dang));
				double maxX = dx, maxY = dy;

				for (int i = 0; i <= I; i++) {
					A = O * (P0[i] - T) * extend;
					B = (Q + U * (P0[i] - T)) * extend;

					dx = A;
					dy = -B;
					dr = FastMath.hypot(dx, dy);
					dang = FastMath.atan2_accurate(-dy, dx) + PARP[i];
					dx = cx + (float) (dr * Math.cos(dang));
					dy = cy - (float) (dr * Math.sin(dang));
					if (lastx != -1 && lasty != -1)
						g.drawLine((float) (lastx), (float) (lasty), (float) (dx), (float) (dy), false);
					if (i == I)
						g.drawLine((float) (dx + (dx - lastx)/1.5f), (float) (dy + (dy - lasty)/1.5f), (float) (dx), (float) (dy), false);
					if (i == 0) {
						finalx = dx;
						finaly = dy;
					}
					if (i < I) {
						lastx = dx;
						lasty = dy;
					}
				}
				if (I == 1) g.drawLine((float) (lastx), (float) (lasty), (float) (maxX), (float) (maxY), false);
				lastx = -1;
				lasty = -1;
				for (int i = 0; i <= I; i++) {
					A = O * (F0[i] - T) * extend;
					B = (Q + U * (F0[i] - T)) * extend;

					dx = A;
					dy = -B;
					dr = FastMath.hypot(dx, dy);
					dang = FastMath.atan2_accurate(-dy, dx) + PARF[i];
					dx = cx + (float) (dr * Math.cos(dang));
					dy = cy - (float) (dr * Math.sin(dang));
					if (lastx != -1 && lasty != -1)
						g.drawLine((float) (lastx), (float) (lasty), (float) (dx), (float) (dy), false);
					if (i < I) {
						lastx = dx;
						lasty = dy;
					}
				}
				if (I == 1) g.drawLine((float) (lastx), (float) (lasty), (float) (maxX), (float) (maxY), false);
				
				double startx = dx + (dx - lastx)*0.5, starty = dy + (dy - lasty)*0.5;
				g.drawLine((float) (startx), (float) (starty), (float) (dx), (float) (dy), false);
				double size = 20, ang = FastMath.atan2((starty - dy), (startx - dx));
				double endx = startx + size * FastMath.cos(ang);
				double endy = starty + size * FastMath.sin(ang);
				double endx1 = endx + size * FastMath.cos(ang), endy1 = endy + size * FastMath.sin(ang);
				dx = (endx1 - endx) / 2;
				dy = (endy1 - endy) / 2;
				g.drawLine((float) (endx), (float) (endy), (float) (startx),
						(float) (starty), false);
				g.drawLine((float) (endx - dy), (float) (endy + dx),
						(float) (endx + dy), (float) (endy - dx), false);
				g.drawLine((float) (endx - dy), (float) (endy + dx),
						(float) (endx1), (float) (endy1), false);
				g.drawLine((float) (endx1), (float) (endy1), (float) (endx + dy),
						(float) (endy - dx), false);
			}
		}
		int alpha = 90;
		g.setColor(255, 255, 255, alpha);
		float C = (float) (S1 * extend), C2 = 2 * C + 1;
		double jdRef = time.astroDate.jd();
		for (int i = 0; i <= I; i++) {
			g.setColor(255, 255, 255, alpha += 40);
			double A = O * (P0[i] - T) * extend;
			double B = (Q + U * (P0[i] - T)) * extend;

			if (horizontalCoord) {
				double dx = A, dy = -B, dr = FastMath.hypot(dx, dy), dang = FastMath.atan2_accurate(-dy, dx) + PARP[i];
				dx = cx + (float) (dr * Math.cos(dang));
				dy = cy - (float) (dr * Math.sin(dang));
				renderMoon((float) (dx - C), (float) (dy - C), C2, dist, g, jdRef + P0[i] / 24.0, obs, eph, !horizontalCoord);
			} else {
				renderMoon((float) (cx + A - C), (float) (cy - B - C), C2, dist, g, jdRef + P0[i] / 24.0, obs, eph, !horizontalCoord);
			}

			A = O * (F0[i] - T) * extend;
			B = (Q + U * (F0[i] - T)) * extend;

			if (horizontalCoord) {
				double dx = A, dy = -B, dr = FastMath.hypot(dx, dy), dang = FastMath.atan2_accurate(-dy, dx) + PARF[i];
				dx = cx + (float) (dr * Math.cos(dang));
				dy = cy - (float) (dr * Math.sin(dang));
				renderMoon((float) (dx - C), (float) (dy - C), C2, dist, g, jdRef + F0[i] / 24.0, obs, eph, !horizontalCoord);
			} else {
				renderMoon((float) (cx + A - C), (float) (cy - B - C), C2, dist, g, jdRef + F0[i] / 24.0, obs, eph, !horizontalCoord);
			}
		}
		double ME = (P0[0] + F0[0]) / 2.0;
		double A = O * (ME - T) * extend, B = (Q + U * (ME - T)) * extend;
		g.setColor(255, 255, 255, 90);
		if (horizontalCoord) {
			double PARME = (PARP[0] + PARF[0]) / 2.0;
			double dx = A, dy = -B, dr = FastMath.hypot(dx, dy), dang = FastMath.atan2_accurate(-dy, dx) + PARME;
			dx = cx + (float) (dr * Math.cos(dang));
			dy = cy - (float) (dr * Math.sin(dang));
			renderMoon((float) (dx - C), (float) (dy - C), C2, dist, g, jdRef + ME / 24.0, obs, eph, !horizontalCoord);
		} else {
			renderMoon((float) (cx + A - C), (float) (cy - B - C), C2, dist, g, jdRef + ME / 24.0, obs, eph, !horizontalCoord);
		}

		g.setColor(255, 255, 255, 255);
		int px = 10, py = 10, px2 = cx + 20, step = g.getFont().getSize() + 2;
		if (type.equals("No eclipse")) {
			g.drawString(Translate.translate(1089), 20, py+=step);
			return;
		}

		if (w > 400) {
			// Improve time estimates using LunarEclipse class. Note we 'draw' some values
			// assuming an uniform movement for the Moon, but we print 'others' (corrected ones)
			if (!type.equals("No eclipse")) { // && eph0.preferPrecisionInEphemerides) {
				LunarEclipse le = new LunarEclipse(new TimeElement(jd_TDB-1, SCALE.BARYCENTRIC_DYNAMICAL_TIME), obs, eph);
				SCALE output = SCALE.BARYCENTRIC_DYNAMICAL_TIME;
				MoonEventElement[] events = le.getEvents();
				MoonEventElement ev[] = new MoonEventElement[3];
				int ind = -1;
				String fp = Translate.translate(1272);
				for (int i = 0; i < events.length; i++)
				{
					if (!events[events.length-1-i].details.equals(fp)) {
						ind ++;
						ev[ind] = events[events.length-1-i];
					}
				}

				if (ind == I && Math.abs(le.getEclipseMaximum()-jd_TDB) < 1) {
					for (int ii=0; ii<=I; ii++) {
						double oldP0 = P0[ii], oldF0 = F0[ii];
						P0[ii] = (ev[ii].getEventTime(obs, eph, true, output)+0.5);
						F0[ii] = (ev[ii].getEventTime(obs, eph, false, output)+0.5);

						P0[ii] = (P0[ii] - (int) P0[ii]) * 24.0;
						F0[ii] = (F0[ii] - (int) F0[ii]) * 24.0;

						if (oldP0 > 23.0 && P0[ii] < 12) P0[ii] += 24.0;
						if (oldF0 > 23.0 && F0[ii] < 12) F0[ii] += 24.0;
					}
				} else {
					JPARSECException.addWarning("Lunar eclipse found in LunarEclipse class around "+le.getEclipseMaximum()+", while here is around "+jd_TDB);
				}
			}

			String ts = TimeElement.TIME_SCALES_ABBREVIATED[time0.timeScale.ordinal()];
			if (ShowWithoutLT && (ts.equals("TL") || ts.equals("LT"))) ts = "";
			py = h - step * 5;
			g.drawString(Translate.translate(type+" lunar eclipse on")+" "+timeMax.astroDate.toStringDate(false)+" "+Translate.translate(1090)+" "+obs.getName(), px, py+=step);
			g.setFont(FONT.DIALOG_PLAIN_11);
			if (bestSize > 11) g.setFont(FONT.getDerivedFont(g.getFont(), bestSize));
			g.drawString(Translate.translate(1104)+" "+Functions.formatRA((T-DT/3600.)/Constant.RAD_TO_HOUR, 0)+" "+ts, px, py+=step);
			py+=step/2;
			time.add(ME / 24.0);
			double elev = Ephem.getEphemeris(time, obs, eph, false).elevation;
			time.add(-ME / 24.0);
			g.drawString(Translate.translate(1094)+": "+Functions.formatRA((ME-DT/3600.)/Constant.RAD_TO_HOUR, 0)+" "+ts+" ("+Translate.translate(29)+" "+Functions.formatAngleAsDegrees(elev, 1)+"\u00b0)", px, py+=step);
			py = 10;
			String alt = "";
			if (ShowWithoutLT) alt = Translate.translate(29)+" ";
			if (I == 2) {
				/*
				// Note these lines should give same result, but P0/F0 were corrected using LunarEclipse
				// and anyway the results obtained only using this method are approximate, so moon elevation
				// values are not accurate.
				A = O * (P0[0] - T);
				B = (Q + U * (P0[0] - T));
				double moonHA = tsl + (P0[0] - T) * Constant.TWO_PI * Constant.SIDEREAL_DAY_LENGTH / 24.0 - (moonRA - A * Constant.DEG_TO_RAD / 60.0);
				double moonD = moonDEC - B * Constant.DEG_TO_RAD / 60.0;
				elev = Ephem.getApparentElevation(eph, obs, Math.asin(sinFI * Math.sin(moonD) + cosFI * Math.cos(moonD) * Math.cos(moonHA)), 10);
				 */
				time.add(P0[0] / 24.0);
				elev = Ephem.getEphemeris(time, obs, eph, false).elevation;
				g.drawString(Translate.translate(1109)+": "+Functions.formatRA((P0[0]-DT/3600.)/Constant.RAD_TO_HOUR, 0)+" "+ts+" ("+alt+Functions.formatAngleAsDegrees(elev, 1)+"\u00b0)", px2, py+=step);
				time.add((-P0[0]+F0[0]) / 24.0);
				elev = Ephem.getEphemeris(time, obs, eph, false).elevation;
				g.drawString(Translate.translate(1110)+": "+Functions.formatRA((F0[0]-DT/3600.)/Constant.RAD_TO_HOUR, 0)+" "+ts+" ("+alt+Functions.formatAngleAsDegrees(elev, 1)+"\u00b0)", px, py);
				//py+=step/2;

				time.add((-F0[0]+P0[1]) / 24.0);
				elev = Ephem.getEphemeris(time, obs, eph, false).elevation;
				g.drawString(Translate.translate(1111)+": "+Functions.formatRA((P0[1]-DT/3600.)/Constant.RAD_TO_HOUR, 0)+" "+ts+" ("+alt+Functions.formatAngleAsDegrees(elev, 1)+"\u00b0)", px2, py+=step);
				time.add((-P0[1]+F0[1]) / 24.0);
				elev = Ephem.getEphemeris(time, obs, eph, false).elevation;
				g.drawString(Translate.translate(1112)+": "+Functions.formatRA((F0[1]-DT/3600.)/Constant.RAD_TO_HOUR, 0)+" "+ts+" ("+alt+Functions.formatAngleAsDegrees(elev, 1)+"\u00b0)", px, py);
				//py+=step/2;
				g.drawString(Translate.translate(1113)+": "+Functions.formatRA((P0[2]-DT/3600.)/Constant.RAD_TO_HOUR, 0)+" "+ts, px2, py+=step);
				g.drawString(Translate.translate(1114)+": "+Functions.formatRA((F0[2]-DT/3600.)/Constant.RAD_TO_HOUR, 0)+" "+ts, px, py);
				py+=step/2;
			} else {
				if (I > 0) {
					time.add(P0[0] / 24.0);
					elev = Ephem.getEphemeris(time, obs, eph, false).elevation;
					g.drawString(Translate.translate(1111)+": "+Functions.formatRA((P0[0]-DT/3600.)/Constant.RAD_TO_HOUR, 0)+" "+ts+" ("+alt+Functions.formatAngleAsDegrees(elev, 1)+"\u00b0)", px2, py+=step);
					time.add((-P0[0]+F0[0]) / 24.0);
					elev = Ephem.getEphemeris(time, obs, eph, false).elevation;
					g.drawString(Translate.translate(1112)+": "+Functions.formatRA((F0[0]-DT/3600.)/Constant.RAD_TO_HOUR, 0)+" "+ts+" ("+alt+Functions.formatAngleAsDegrees(elev, 1)+"\u00b0)", px, py);
					//py+=step/2;
					g.drawString(Translate.translate(1113)+": "+Functions.formatRA((P0[1]-DT/3600.)/Constant.RAD_TO_HOUR, 0)+" "+ts, px2, py+=step);
					g.drawString(Translate.translate(1114)+": "+Functions.formatRA((F0[1]-DT/3600.)/Constant.RAD_TO_HOUR, 0)+" "+ts, px, py);
					py+=step/2;
				} else {
					if (I == 0) {
						g.drawString(Translate.translate(1113)+": "+Functions.formatRA((P0[0]-DT/3600.)/Constant.RAD_TO_HOUR, 0)+" "+ts, px2, py+=step);
						g.drawString(Translate.translate(1114)+": "+Functions.formatRA((F0[0]-DT/3600.)/Constant.RAD_TO_HOUR, 0)+" "+ts, px, py);
						py+=step/2;
					}
				}
			}
			if (type.equals("Total") || type.equals("Partial")) type = "Umbral";
			g.drawString(Translate.translate(type + " magnitude")+": "+Functions.formatValue(F, 3), px, py+=step);
		}

		if (map != null && map != PLANET_MAP.NO_MAP) {
			int mapx = 0, mapy = h;
			if (horMap) {
				mapx = w;
				mapy = 0;
			}
			g.traslate(mapx, mapy);
			lunarEclipseMap(new TimeElement(jd0+ME/24.0, SCALE.BARYCENTRIC_DYNAMICAL_TIME), obs, eph0, g, map);
//			if (map == PLANET_MAP.MAP_FLAT) {
				boolean hq = RenderPlanet.FORCE_HIGHT_QUALITY;
				float quality = 1, maxq = RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR;
				if (!g.renderingToAndroid() && (RenderPlanet.FORCE_HIGHT_QUALITY || map == PLANET_MAP.MAP_SPHERICAL || g.renderingToExternalGraphics())) {
					quality = maxq;
					RenderPlanet.FORCE_HIGHT_QUALITY = true;
				}
				RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR = 1.0f;
				Graphics g3 = g.getGraphics((int) (g.getWidth()*quality), (int) (g.getHeight()*quality));
				ObserverElement obs2 = obs.clone();
				obs2.setName("");
				for (int i = I; i >= 0; i--) {
					lunarEclipseMap(new TimeElement(jd0+P0[i]/24.0, SCALE.BARYCENTRIC_DYNAMICAL_TIME), obs2, eph0, g3, map);
					g.drawImage(g.makeColorTransparent(g3.getRendering(), 0, false, true, 30), 0, 0, 1.0/quality, 1.0/quality);
					lunarEclipseMap(new TimeElement(jd0+F0[i]/24.0, SCALE.BARYCENTRIC_DYNAMICAL_TIME), obs2, eph0, g3, map);
					g.drawImage(g.makeColorTransparent(g3.getRendering(), 0, false, true, 30), 0, 0, 1.0/quality, 1.0/quality);
				}
				RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR = maxq;
				RenderPlanet.FORCE_HIGHT_QUALITY = hq;
//			}
			g.traslate(-mapx, -mapy);
		}
	}

	/**
	 * Creates a chart with only the map of a lunar eclipse.
	 * @param time The time object with the correct date of the lunar eclipse.
	 * @param observer The observer. It will not be shown in case its name is
	 * set to an empty string. In that case the strings showing the edge where
	 * the eclipse maximum occurs at moonrise/moonset will not be shown.
	 * @param eph0 The ephemeris properties.
	 * @param g The Graphics object. Anaglyph mode property is used to render the map in 3d.
	 * @param map Options for the eclipse map.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void lunarEclipseMap(TimeElement time, ObserverElement observer, EphemerisElement eph0, Graphics g,
			PLANET_MAP map) throws JPARSECException {
		map.zoomFactor = 1;
		map.showGrid = false;
		float zoom = map.zoomFactor;
		int w = g.getWidth();
		g.setColor(0, 0, 0, 255);
		g.fillRect(0, 0, w, g.getHeight());

		float quality = 1;
		if (!g.renderingToAndroid() && (RenderPlanet.FORCE_HIGHT_QUALITY || map == PLANET_MAP.MAP_SPHERICAL || g.renderingToExternalGraphics())) quality = RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR;
		SatelliteRenderElement render = new SatelliteRenderElement((int) (w*quality), (int) (g.getHeight()*quality));
		//render.showMoon = false;
		//render.showSun = false;
		//render.showObserver = false;
		//render.showDayAndNight = false;
		render.planetMap = map;
		render.anaglyphMode = g.getAnaglyphMode();
		if (observer.getName() == null || observer.getName().equals("")) render.showObserver = false;

		EphemerisElement eph = eph0.clone();
		eph.correctForEOP = false;
		eph.correctForPolarMotion = false;
		eph.correctEOPForDiurnalSubdiurnalTides = false;
		eph.preferPrecisionInEphemerides = false;

		render.highlightMoon = true;
		RenderSatellite satRender = new RenderSatellite(time, observer, eph, render);
		if (quality == 1) {
			satRender.renderize(g);
		} else {
			Graphics g2 = g.getGraphics(render.width, render.height);
			g2.setFont(FONT.getDerivedFont(g2.getFont(), (int)(g2.getFont().getSize()*quality)));
			satRender.renderize(g2);
			if (render.anaglyphMode.isReal3D()) {
				g.setAnaglyph(g2.getScaledImage(g2.getImage(0, 0, render.width, render.height), w, g.getHeight(), true, RenderSatellite.ALLOW_SPLINE_RESIZING), g2.getScaledImage(g2.getImage2(0, 0, render.width, render.height), w, g.getHeight(), true, false));
			} else {
				//g.drawImage(g2.getScaledImage(g2.getRendering(), w, g.getHeight(), true, RenderSatellite.ALLOW_SPLINE_RESIZING), 0, 0);
				g.drawImage(g2.getRendering(), 0, 0, 1.0/quality, 1.0/quality);
			}
		}

		if (map == PLANET_MAP.MAP_SPHERICAL || observer.getName().equals("")) return;
		int pos[] = satRender.getTexturePosition(satRender.locMoon[0]);
		if (pos == null) return;
		pos[1] -= 30*quality;
		w *= quality;
		g.setColor(255, 255, 255, 255);
		if (map.EarthMapSource == PLANET_MAP.EARTH_MAP_POLITICAL)
			g.setColor(128, 128, 128, 255);
		String label = Translate.translate(1094); // Eclipse maximum
		float sw = g.getStringWidth(label)/2;
		float px = (pos[0]-w*zoom/4)/quality-sw;
		float dx1 = 0, dx2 = 0;
		if (px < 0 && px > -sw) {
			dx1 = -px;
			if (dx1 > sw && zoom == 1) dx1 = w/quality;
		}
		g.drawString(label, px+dx1, pos[1]/quality);
		if (px+dx1+sw*2 > w*zoom/quality) g.drawString(label, px+dx1-w*zoom/quality, pos[1]/quality);
		px = (pos[0]+w*zoom/4)/quality-sw;
		if (w-px < sw && w-px > -sw) {
			dx2 = -w/quality + sw;
			if (zoom > 1) dx2 = -dx2-px;
		}
		g.drawString(label, px+dx2, pos[1]/quality);
		if (px+dx2+sw*2 > w*zoom/quality) g.drawString(label, px+dx2-w*zoom/quality, pos[1]/quality);

		label = Translate.translate(1107); //"at moonrise";
		sw = g.getStringWidth(label)/2;
		px = (pos[0]-w*zoom/4)/quality-sw;
		g.drawString(label, px+dx1, pos[1]/quality+15);
		if (px+dx1+sw*2 > w*zoom/quality) g.drawString(label, px+dx1-w*zoom/quality, pos[1]/quality+15);
		label = Translate.translate(1108); //"at moonset";
		sw = g.getStringWidth(label)/2;
		px = (pos[0]+w*zoom/4)/quality-sw;
		g.drawString(label, px+dx2, pos[1]/quality+15);
		if (px+dx2+sw*2 > w*zoom/quality) g.drawString(label, px+dx2-w*zoom/quality, pos[1]/quality+15);

		pos[1] += 65*quality;
		label = Translate.translate(1119); // eclipse visible;
		sw = g.getStringWidth(label);
		px = pos[0]/quality-sw/2;
		g.drawString(label, px+dx2, pos[1]/quality);
		if (px+dx2+sw*2 > w*zoom/quality) g.drawString(label, px+dx2-w*zoom/quality, pos[1]/quality);
		pos[1] += 20*quality;
		g.drawLine(px, pos[1]/quality, pos[0]/quality+sw/2, pos[1]/quality, true);
		g.drawLine(px, pos[1]/quality-5, px, pos[1]/quality+5, true);
		g.drawLine(px, pos[1]/quality-5, px - 10, pos[1]/quality, true);
		g.drawLine(px, pos[1]/quality+5, px - 10, pos[1]/quality, true);

		pos = satRender.getTexturePosition(satRender.locSun);
		if (pos == null) return;
		pos[1] += 35*quality;
		label = Translate.translate(1120); // eclipse not visible;
		sw = g.getStringWidth(label);
		px = pos[0]/quality-sw/2;
		g.drawString(label, px+dx2, pos[1]/quality);
		if (px+dx2+sw*2 > w*zoom/quality) g.drawString(label, px+dx2-w*zoom/quality, pos[1]/quality);
	}

	/**
	 * Returns if a given lunar eclipse is visible from a given observer.
	 * @param time0 The time object with the date of a lunar eclipse.
	 * @param obs The observer.
	 * @param eph0 The ephemerides properties.
	 * @param considerPenumbralPhase True to consider penumbral phase, false
	 * to return if the partial eclipse is visible or not.
	 * @return True if the eclipse is visible (the entire eclipse, the beginning,
	 * or the ending).
	 * @throws JPARSECException If an error occurs.
	 */
	public static boolean lunarEclipseVisible(TimeElement time0, ObserverElement obs, EphemerisElement eph0,
			boolean considerPenumbralPhase) throws JPARSECException {
		EphemerisElement eph = eph0.clone();
		eph.correctForEOP = false;
		eph.correctForPolarMotion = false;
		eph.correctEOPForDiurnalSubdiurnalTides = false;
		eph.preferPrecisionInEphemerides = false;
		eph.isTopocentric = false;
		eph.ephemType = COORDINATES_TYPE.APPARENT;
		eph.equinox = EphemerisElement.EQUINOX_OF_DATE;
		eph.targetBody = TARGET.Moon;

		double eclipseJD = MainEvents.MoonPhaseOrEclipse(time0.astroDate.jd(), EVENT.MOON_LUNAR_ECLIPSE, EVENT_TIME.CLOSEST).time;
		TimeElement time = new TimeElement(eclipseJD, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		time.add(-8.0 / 24.0);
		LunarEclipse se = new LunarEclipse(time, obs, eph);
		MoonEventElement me[] = se.getEvents();
		if (me == null || me.length == 0) throw new JPARSECException("No eclipse found.");
		AstroDate start = new AstroDate(me[me.length-1].startTime);
		start.setDayFraction(0);
		time = new TimeElement(start, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		eph.targetBody = TARGET.SUN;
		EphemElement sun = Ephem.getEphemeris(time, obs, eph, false);
		eph.targetBody = TARGET.Moon;
		EphemElement moon = Ephem.getEphemeris(time, obs, eph, false);
		double AA = moon.rightAscension * Constant.RAD_TO_HOUR, CC = sun.rightAscension * Constant.RAD_TO_HOUR,
				KK = sun.declination * Constant.RAD_TO_DEG;
		time.add(1.0);
		eph.targetBody = TARGET.SUN;
		EphemElement sunp = Ephem.getEphemeris(time, obs, eph, false);
		eph.targetBody = TARGET.Moon;
		EphemElement moonp = Ephem.getEphemeris(time, obs, eph, false);
		double BB = moonp.rightAscension * Constant.RAD_TO_HOUR, DD = sunp.rightAscension * Constant.RAD_TO_HOUR,
				LL = sunp.declination * Constant.RAD_TO_DEG;

		CC = 24.0 * ((CC + 12.0) / 24.0 - Math.floor((CC + 12.0) / 24.0));
		DD = 24.0 * ((DD + 12.0) / 24.0 - Math.floor((DD + 12.0) / 24.0));
		double H = Math.floor(24.0 * (CC - AA) / (BB - AA - DD + CC));
		time.add(-1.0 + H / 24.0);
		eph.targetBody = TARGET.Moon;
		EphemElement moonh = Ephem.getEphemeris(time, obs, eph, false);
		AA = moonh.rightAscension * Constant.RAD_TO_HOUR;
		double PP = moonh.declination * Constant.RAD_TO_DEG;

		double jdTDB = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME), stepH = Math.max(1, 0.125+(se.getEclipseMaximum() - jdTDB) * 24);
		if (se.getEclipseType().toLowerCase().equals("total") && se.getEclipseMaximum() < jdTDB) stepH = 0.125;
		time.add(stepH / 24.0);
		eph.targetBody = TARGET.SUN;
		EphemElement sunhp = Ephem.getEphemeris(time, obs, eph, false);
		eph.targetBody = TARGET.Moon;
		EphemElement moonhp = Ephem.getEphemeris(time, obs, eph, false);
		BB = moonhp.rightAscension * Constant.RAD_TO_HOUR;
		double NN = moonhp.declination * Constant.RAD_TO_DEG;
		double T = (AA - CC - (BB - AA) * (H)) / ((DD - CC) / 24.0 - BB + AA); // Time of oposition in RA, TDB

		double S1 = moonhp.angularRadius * Constant.RAD_TO_DEG * 60.0,
				P1 = Math.asin(Constant.EARTH_RADIUS / (Constant.AU * moonhp.distance))	* Constant.RAD_TO_DEG * 60.0;
		double R2 = sunhp.distance, S2 = sunhp.angularRadius * Constant.RAD_TO_DEG * 60.0, P2 = 8.793999 / R2;

		// Using Chauvenet enlargement method slightly corrected (1.8% instead
		// of 2%). See http://eclipse.gsfc.nasa.gov/LEcat5/shadow.html
		double S = (P1 + P2 / 60.0 - S2) * 50.9 / 50.0; // Umbra size in '
		double P = (P1 + P2 / 60.0 + S2) * 50.9 / 50.0; // Penumbra size in '

		double Q = ((NN - PP) * (T - Math.floor(T)) + PP + KK + (LL - KK) * T / 24) * 60;
		double O = ((DD - CC) / 24 - BB + AA) * 900;
		double U = (NN - PP + (LL - KK) / 24) * 60;
		double V = O * O + U * U, W = Q * U, W2 = W * W;
		double D = Math.abs(O * Q) / Math.sqrt(V);

		time.add((-H-1.0+T) / 24.0);

		int I = -1;
		double X = 0, P0[] = new double[3], F0[] = new double[3]; // , Z = 0, PC = 0, UC = 0;
		String type = "";
		if (D <= S - S1) {
			type = "Total";
			X = Q * Q - (S - S1) * (S - S1);

			// Start/end times of lunar eclipse phases
			I++; // Counter for the different phases
			P0[I] = T + (-W - Math.sqrt(W2 - V * X)) / V; // Start
			F0[I] = T + (-W + Math.sqrt(W2 - V * X)) / V; // End
		}
		if (D < S + S1) {
			if (type.equals("")) type = "Partial";
			X = Q * Q - (S + S1) * (S + S1);

			I++;
			P0[I] = T + (-W - Math.sqrt(W2 - V * X)) / V;
			F0[I] = T + (-W + Math.sqrt(W2 - V * X)) / V;
		}
		if (D < P + S1) {
			if (type.equals("")) type = "Penumbral";

			// Contact angle and phase
			/*
			 * double Z = S;
			 * (type.equals("Penumbral")) Z = P;
			 * F = (Z + S1 - D) / (2 * S1);
			 * double HH = Math.asin(D / (Z + S1)), JJ = Math.acos(U / Math.sqrt(V));
			 * if (O * Q > 0) HH = -HH;
			 * PC = (JJ + HH); // First contact, E
			 * UC = Math.PI - JJ + HH; // Last contact, W
			 */

			// Penumbral phase
			X = Q * Q - (P + S1) * (P + S1);

			I++;
			P0[I] = T + (-W - Math.sqrt(W2 - V * X)) / V;
			F0[I] = T + (-W + Math.sqrt(W2 - V * X)) / V;
		}
		if (type.equals("")) type = "No eclipse";

		// Improve time estimates using LunarEclipse class (not required just to see if it is visible)
/*		if (!type.equals("No eclipse")) { // && eph0.preferPrecisionInEphemerides) {
			LunarEclipse le = new LunarEclipse(new TimeElement(jd_TDB-1, SCALE.BARYCENTRIC_DYNAMICAL_TIME), obs, eph);
			SCALE output = SCALE.BARYCENTRIC_DYNAMICAL_TIME;
			MoonEventElement[] events = le.getEvents();
			MoonEventElement ev[] = new MoonEventElement[3];
			int ind = -1;
			for (int i = 0; i < events.length; i++)
			{
				if (!events[events.length-1-i].details.equals("full penumbral")) {
					ind ++;
					ev[ind] = events[events.length-1-i];
				}
			}

			if (ind == I && Math.abs(le.getEclipseMaximum()-jd_TDB) < 1) {
				for (int ii=0; ii<=I; ii++) {
					double oldP0 = P0[ii], oldF0 = F0[ii];
					P0[ii] = (ev[ii].getEventTime(obs, eph, true, output)+0.5);
					F0[ii] = (ev[ii].getEventTime(obs, eph, false, output)+0.5);

					P0[ii] = (P0[ii] - (int) P0[ii]) * 24.0;
					F0[ii] = (F0[ii] - (int) F0[ii]) * 24.0;

					if (oldP0 > 23.0 && P0[ii] < 12) P0[ii] += 24.0;
					if (oldF0 > 23.0 && F0[ii] < 12) F0[ii] += 24.0;
				}
			} else {
				JPARSECException.addWarning("Lunar eclipse found in LunarEclipse class around "+le.getEclipseMaximum()+", while here is around "+jd_TDB);
			}
		}
*/
		eph.isTopocentric = true;
		time.add((-T) / 24.0);
		if (type.equals("No eclipse")) return false;
		double elev1 = 0, elev2 = 0;
		int dI = 0;
		if (!considerPenumbralPhase) dI = -1;
		if (I == 2) {
			time.add(P0[2+dI] / 24.0);
			elev1 = Ephem.getEphemeris(time, obs, eph, false).elevation;

			time.add((-P0[2+dI]+F0[2+dI]) / 24.0);
			elev2 = Ephem.getEphemeris(time, obs, eph, false).elevation;
		} else {
			if (I > 0) {
				time.add(P0[1+dI] / 24.0);
				elev1 = Ephem.getEphemeris(time, obs, eph, false).elevation;

				time.add((-P0[1+dI]+F0[1+dI]) / 24.0);
				elev2 = Ephem.getEphemeris(time, obs, eph, false).elevation;

			} else {
				if (I == 0) {
					if (dI < 0) return false;

					time.add(P0[0] / 24.0);
					elev1 = Ephem.getEphemeris(time, obs, eph, false).elevation;

					time.add((-P0[0]+F0[0]) / 24.0);
					elev2 = Ephem.getEphemeris(time, obs, eph, false).elevation;
				}
			}
		}
		if (elev1 > 0 || elev2 > 0) return true;
		return false;
	}

	/**
	 * Returns the magnitude of a lunar eclipse
	 * @param time0 The time object with the correct date (just date) of the eclipse.
	 * The time scale of this object will be also used for the time labels in the chart.
	 * @param obs The observer.
	 * @param eph0 The ephemeris properties.
	 * @return The magnitude of the eclipse.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double lunarEclipseMagnitude(TimeElement time0, ObserverElement obs, EphemerisElement eph0)
			throws JPARSECException {
		SimpleEventElement see = MainEvents.MoonPhaseOrEclipse(time0.astroDate.jd(), EVENT.MOON_LUNAR_ECLIPSE, EVENT_TIME.CLOSEST);
		String det = see.details;
		
		int mag = det.indexOf("mag");
		if (mag < 0) throw new JPARSECException("Cannot compute eclipse magnitude");
		
		String s = det.substring(mag + 3).trim();
		s = s.substring(0, s.indexOf(","));
		return Double.parseDouble(s);
	}

	/**
	 * Returns a link to show a Google Map view of this solar eclipse using
	 * the maps by Xavier Jubier at http://xjubier.free.fr.
	 * @return The URL to the Google Map.
	 */
    public String getGoogleMapLink() {
    	String date = ""+astro.getYear()+Functions.fmt(astro.getMonth(), 2)+Functions.fmt(astro.getDay(), 2);
    	String url = "http://xjubier.free.fr/en/site_pages/solar_eclipses/xSE_GoogleMapFull.php?";
    	url += "Ecl=+" + date + "&Acc=2&Umb=1&Lmt=1&Mag=0&Max=0";
    	return url;
    }

	private static void renderMoon(float px, float py, float r, float dist, Graphics g,
			double jd, ObserverElement obs, EphemerisElement eph, boolean northUp) {
		if (ShowMoonTexture) {
			try {
				TimeElement time = new TimeElement(jd, SCALE.TERRESTRIAL_TIME);
				PlanetRenderElement render = new PlanetRenderElement((int) r, (int) r, false, true, false, false, northUp, false);
				render.highQuality = !g.renderingToAndroid();
				TelescopeElement telescope = TelescopeElement.SCHMIDT_CASSEGRAIN_20cm;
				telescope.invertHorizontal = telescope.invertVertical = false;
				EphemElement ephem = Ephem.getEphemeris(time, obs, eph, false);
				telescope.ocular.focalLength = TelescopeElement.getOcularFocalLengthForCertainField(ephem.angularRadius*2, telescope);
				render.telescope = telescope;
				RenderPlanet renderPlanet = new RenderPlanet(time, obs, eph, render);
				Graphics g2 = g.getGraphics((int) r, (int) r);
				renderPlanet.renderize(g2);
				Object image = g2.getRendering();
				g.setColor(1, 1, 1, 255);
				image = g.makeColorTransparent(image, g.getColor(), false, true, 250);
				g.setColor(0, 0, 0, 255);
				image = g.makeColorTransparent(image, g.getColor(), true, false, 0);
				g.drawImage(image, px, py);
			} catch (Exception exc) {
				exc.printStackTrace();
			}
		} else {
			if (dist == 0) {
				g.fillOval(px, py, r, r, false);
			} else {
				g.fillOval(px, py, r, r, dist);
			}
		}
	}
}
