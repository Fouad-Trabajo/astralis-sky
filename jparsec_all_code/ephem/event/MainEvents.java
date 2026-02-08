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

import java.text.DecimalFormat;
import java.util.ArrayList;

import jparsec.astronomy.CoordinateSystem;
import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.Functions;
import jparsec.ephem.Precession;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.event.SimpleEventElement.EVENT;
import jparsec.ephem.planets.EphemElement;
import jparsec.ephem.planets.JPLEphemeris;
import jparsec.ephem.planets.imcce.Elp2000;
import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.math.Constant;
import jparsec.math.FastMath;
import jparsec.math.Interpolation;
import jparsec.observer.City;
import jparsec.observer.CityElement;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.AstroDate;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.time.TimeScale;
import jparsec.time.calendar.Calendar;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;

/**
 * Main events for the year.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class MainEvents
{
	// private constructor so that this class cannot be instantiated.
	private MainEvents() {}

	/**
	 * The set of event 'times', to select the previous, next, or
	 * closest event to a given input time.
	 */
	public enum EVENT_TIME {
		/** ID constant for the next event. */
		NEXT,
		/** ID constant for the previous event. */
		PREVIOUS,
		/** ID constant for the closest event in time. */
		CLOSEST
	};

	/**
	 * Calculates the instant of a given Moon phase or eclipse, following Meeus's Astronomical
	 * Algorithms. Error is always below 2 minutes, and usually below 1 minute.
	 * @param jd The starting Julian day of calculations in dynamical time.
	 * @param event The event, constants defined in {@linkplain SimpleEventElement} (MOON_...).
	 * @param eventType The event type (next, last, or closest to input date).  The use of the
	 * closest option is recommended when possible, since next/previous events could give incorrect
	 * events for a given date far from J2000.
	 * @return The event.
	 * @throws JPARSECException If an error occurs.
	 */
	public static SimpleEventElement MoonPhaseOrEclipse(double jd, SimpleEventElement.EVENT event, EVENT_TIME eventType)
	throws JPARSECException {
		String annular0 = "annular", hybrid0 = "hybrid", partial0 = "partial", umbra0 = "umbra", central0 = "central";
		String total0 = "total", penumbra0 = "penumbra", penumbral0 = "penumbral";
		String psd0 = "partial phase semi-duration in";
		String tsd0 = "total phase semi-duration in";
		String notcentral0 = "not central";

		SimpleEventElement.EVENT inputEvent = event;
		boolean eclipse = false;
		if (event.compareTo(SimpleEventElement.EVENT.MOON_LUNAR_ECLIPSE) >= 0) {
			if (event.compareTo(SimpleEventElement.EVENT.MOON_SOLAR_ECLIPSE) > 0) throw new JPARSECException("invalid event.");
			eclipse = true;
			event = SimpleEventElement.EVENT.values()[event.ordinal()-SimpleEventElement.EVENT.MOON_LUNAR_ECLIPSE.ordinal()];
		}
		AstroDate astro = new AstroDate(jd);
		double year = astro.getAstronomicalYear() + (astro.getMonth() - 1.0 + astro.getDayPlusFraction() / (1.0 + astro.getDaysInMonth())) / 12.0;

		double kapprox = (year - 2000.0) * 12.3685;
		double k = kapprox;
		switch (event) {
		case MOON_FULL:
			k = MainEvents.round(k, 0.5, eventType);
			break;
		case MOON_NEW:
			k = MainEvents.round(k, 0.0, eventType);
			break;
		case MOON_FIRST_QUARTER:
			k = MainEvents.round(k, 0.25, eventType);
			break;
		case MOON_LAST_QUARTER:
			k = MainEvents.round(k, 0.75, eventType);
			break;
		default:
			throw new JPARSECException("invalid event.");
		}

		double t = k / 1236.85;
		double jde = 2451550.09765 + 29.530588853 * k + 0.0001337 * t * t - 0.000000150 * t * t * t + 0.00000000073 * t * t * t * t;
		// Fix k if required
		if (jde > jd && eventType == EVENT_TIME.PREVIOUS) k --;
		if (jde < jd && eventType == EVENT_TIME.NEXT) k ++;
		if (jde > jd && eventType != EVENT_TIME.PREVIOUS) {
			double km = k - 1;
			t = km / 1236.85;
			double newjde = 2451550.09765 + 29.530588853 * km + 0.0001337 * t * t - 0.000000150 * t * t * t + 0.00000000073 * t * t * t * t;
			if ((newjde > jd && eventType == EVENT_TIME.NEXT) ||
					(Math.abs(jd-newjde) < Math.abs(jd-jde) && eventType == EVENT_TIME.CLOSEST)) k = km;
		}
		if (jde < jd && eventType != EVENT_TIME.NEXT) {
			double kp = k + 1;
			t = kp / 1236.85;
			double newjde = 2451550.09765 + 29.530588853 * kp + 0.0001337 * t * t - 0.000000150 * t * t * t + 0.00000000073 * t * t * t * t;
			if ((newjde < jd && eventType == EVENT_TIME.PREVIOUS) ||
					(Math.abs(jd-newjde) < Math.abs(jd-jde) && eventType == EVENT_TIME.CLOSEST)) k = kp;
		}
		t = k / 1236.85;

		boolean eclipseFound = false;
		double p = 0, q = 0, ww = 0, g = 0, u = 0, gg = 0;
		double delta = 1;
		if (eventType == MainEvents.EVENT_TIME.PREVIOUS) delta = -delta;
		String type = "";
		do {
			t = k / 1236.85;
			jde = 2451550.09765 + 29.530588853 * k + 0.0001337 * t * t - 0.000000150 * t * t * t + 0.00000000073 * t * t * t * t;

			double M = 2.5534 + 29.10535669 * k - 0.0000218 * t * t - 0.00000011*t*t*t;
			double Mp = 201.5643 + 385.81693528 * k + 0.0107438 * t * t + 0.00001239 * t * t * t - 0.000000058 * t*t*t*t;
			double F = 160.7108 + 390.67050274 * k - 0.0016341*t*t - 0.00000227*t*t*t + 0.000000011*t*t*t*t;
			double O = 124.7746 - 1.5637558 * k + 0.002069*t*t+0.00000215*t*t*t;
			double E = 1.0 - 0.002516 * t - 0.0000074*t*t;

			M *= Constant.DEG_TO_RAD;
			Mp *= Constant.DEG_TO_RAD;
			F *= Constant.DEG_TO_RAD;
			O *= Constant.DEG_TO_RAD;

			// We are following Meeus's Astronomical Algorithms, chapter 47.
			// Taking all terms => max. error < 20s, and approx 1 min for eclipses (see chapter 49).
			double W = 0.00306 - 0.00038 * E * Math.cos(M) + 0.00026 * Math.cos(Mp) - 0.00002 * Math.cos(Mp-M) + 0.00002 * Math.cos(Mp+M) + 0.00002 * Math.cos(2.0 * F);
			double F1 = F - 0.02665 * Math.sin(O) * Constant.DEG_TO_RAD;
			double A1 = (299.77 + 0.107408 * k - 0.009173 * t * t) * Constant.DEG_TO_RAD;
			double A2 = (251.88 + 0.016321 * k) * Constant.DEG_TO_RAD;
			double A3 = (251.83 + 26.651886 * k) * Constant.DEG_TO_RAD;
			double A4 = (349.42 + 36.412478 * k) * Constant.DEG_TO_RAD;
			double A5 = (84.66 + 18.206239 * k) * Constant.DEG_TO_RAD;
			double A6 = (141.74 + 53.303771 * k) * Constant.DEG_TO_RAD;
			double A7 = (207.14 + 2.453732 * k) * Constant.DEG_TO_RAD;
			double A8 = (154.84 + 7.306860 * k) * Constant.DEG_TO_RAD;
			double A9 = (34.52 + 27.261239 * k) * Constant.DEG_TO_RAD;
			double A10 = (207.19 + 0.121824 * k) * Constant.DEG_TO_RAD;
			double A11 = (291.34 + 1.844379 * k) * Constant.DEG_TO_RAD;
			double A12 = (161.72 + 24.198154 * k) * Constant.DEG_TO_RAD;
			double A13 = (239.56 + 25.513099 * k) * Constant.DEG_TO_RAD;
			double A14 = (331.55 + 3.592518 * k) * Constant.DEG_TO_RAD;
			double djde = 0.0;
			if (eclipse) {
				p = -0.0392 * Math.sin(Mp) + 0.2070*E*Math.sin(M)+
					0.0024*E*Math.sin(2.0*M)+0.0116*Math.sin(2.0*Mp)-
					0.0073*E*Math.sin(Mp+M)+0.0067*E*Math.sin(Mp-M)+
					0.0118*Math.sin(2.0*F1);
				q = 5.2207 - 0.3299 * Math.cos(Mp) - 0.0048*E*Math.cos(M)+
					0.002*E*Math.cos(2.0*M)-0.006*E*Math.cos(Mp+M)+
					0.0041*E*Math.cos(Mp-M);
				ww = Math.abs(Math.cos(F1));
				g = (p * Math.cos(F1) + q * Math.sin(F1)) * (1.0 - 0.0048 * ww);
				u = 0.0059 + 0.0046 * E * Math.cos(M) - 0.0182 * Math.cos(Mp) +
					0.0004 * Math.cos(2.0 * Mp) - 0.0005 * Math.cos(M+Mp);
				gg = Math.abs(g);
			}
			DecimalFormat formatter = new DecimalFormat("0.00");
			switch (event) {
			case MOON_FULL:
				if (eclipse) {
					djde = -0.4065 * Math.sin(Mp) + 0.1727*E*Math.sin(M)+
						0.0161*Math.sin(2.0*Mp)-0.0097*Math.sin(2.0*F1)+
						0.0073*E*Math.sin(Mp-M)-0.0050*E*Math.sin(Mp+M)+
						0.0021*E*Math.sin(2.0*M)-0.0023*Math.sin(Mp-2.0*F1)+
						0.0012*Math.sin(Mp+2.0*F1)+0.0006*E*Math.sin(2.0*Mp+M)-
						0.0004*Math.sin(3.0*Mp)-0.0003*E*Math.sin(M+2.0*F1)+
						0.0003*Math.sin(A1)-0.0002*E*Math.sin(M-2.0*F1)-
						0.0002*E*Math.sin(2.0*Mp-M)-0.0002*Math.sin(O);

					double magP = (1.5573 + u - gg) / 0.545;
					double magU = (1.0128 - u - gg) / 0.545;

					if (magP > 0.0 || magU > 0.0) {
						eclipseFound = true;
						if (magU < 0.0) {
							type = penumbral0+", mag "+DataSet.replaceAll(formatter.format(magP), ",", ".", false);
						} else {
							type = total0;
							if (magU < 1.0) type = partial0;
							type += ", mag "+DataSet.replaceAll(formatter.format(magU), ",", ".", false);

							double pp = 1.0128 - u, tt = 0.4678 - u, n = 0.5458 + 0.04 * Math.cos(Mp);

							if (pp > g) {
								int partialSemiDurInUmbra = (int) (60.0 * Math.sqrt(pp*pp-g*g) / n + 0.5);
								if (partialSemiDurInUmbra > 0) type +=", "+psd0+" "+umbra0+" "+partialSemiDurInUmbra+" min";
							}
							if (tt > g) {
								int totalSemiDurInUmbra = (int) (0.5 + 60.0 * Math.sqrt(tt*tt-g*g) / n);
								if (totalSemiDurInUmbra > 0) type +=", "+tsd0+" "+umbra0+" "+totalSemiDurInUmbra+" min";
							}
							double h = 1.5573 + u;
							if (h > g) {
								int partialSemiDurInPenumbra = (int) (0.5 + 60.0 * Math.sqrt(h*h-g*g) / n);
								if (partialSemiDurInPenumbra > 0) type +=", "+psd0+" "+penumbra0+" "+partialSemiDurInPenumbra+" min";
							}
						}

						try {
							if (JPARSECException.getLastMethodName().indexOf(Saros.class.getCanonicalName()) < 0) {
								Saros saros = new Saros(jde+djde);
								if (saros.sarosEclipseNumber != Saros.INVALID_SAROS_RESULT) {
									String sen = ""+saros.sarosEclipseMaxNumber;
									if (sen.equals("-1")) sen = "-";
									type +=", saros " + saros.sarosSeries+" (eclipse "+saros.sarosEclipseNumber+"/"+sen+"), inex "+saros.inexCycle;
								}
							}
						} catch (Exception exc) {}
					}
				} else {
					djde = -0.40614 * Math.sin(Mp) + 0.17302*E*Math.sin(M)+
						0.01614*Math.sin(2.0*Mp)+0.01043*Math.sin(2.0*F)+
						0.00734*E*Math.sin(Mp-M)-0.00515*E*Math.sin(Mp+M)+
						0.00209*E*E*Math.sin(2.0*M)-0.00111*Math.sin(Mp-2.0*F)-
						0.00057*Math.sin(Mp+2.0*F)+0.00056*E*Math.sin(2.0*Mp+M)-
						0.00042*Math.sin(3.0*Mp)+0.00042*E*Math.sin(M+2.0*F)+
						0.00038*E*Math.sin(M-2.0*F)-0.00024*E*Math.sin(2.0*Mp-M)-
						0.00007*E*Math.sin(Mp+2.0*M)-0.00017*Math.sin(O);
					djde = djde + 0.000325*Math.sin(A1) + 0.000165*Math.sin(A2) + 0.000164*Math.sin(A3) +
						0.000126*Math.sin(A4) + 0.000110*Math.sin(A5) + 0.000062*Math.sin(A6) + 0.000060*Math.sin(A7) +
						0.000056*Math.sin(A8) + 0.000047*Math.sin(A9) + 0.000042*Math.sin(A10) + 0.000040*Math.sin(A11) +
						0.000037*Math.sin(A12) + 0.000035*Math.sin(A13) + 0.000023*Math.sin(A14);
				}
				break;
			case MOON_NEW:
				if (eclipse) {
					djde = -0.4075 * Math.sin(Mp) + 0.1721*E*Math.sin(M)+
						0.0161*Math.sin(2.0*Mp)-0.0097*Math.sin(2.0*F1)+
						0.0073*E*Math.sin(Mp-M)-0.0050*E*Math.sin(Mp+M)+
						0.0021*E*Math.sin(2.0*M)-0.0023*Math.sin(Mp-2.0*F1)+
						0.0012*Math.sin(Mp+2.0*F1)+0.0006*E*Math.sin(2.0*Mp+M)-
						0.0004*Math.sin(3.0*Mp)-0.0003*E*Math.sin(M+2.0*F1)+
						0.0003*Math.sin(A1)-0.0002*E*Math.sin(M-2.0*F1)-
						0.0002*E*Math.sin(2.0*Mp-M)-0.0002*Math.sin(O);

					if (gg < (1.5433 + u)) {
						eclipseFound = true;

						if (gg < 0.9972 || (gg > 0.9972 && gg < 0.9972+Math.abs(u))) {
							if (u < 0) type = total0;
							if (u > 0.0047) type = annular0;
							if (type.equals("")) {
								double www = 0.00464 * Math.sqrt(1.0 - g * g);
								if (u < www) {
									type = annular0+"-"+total0+" ("+hybrid0+")";
								} else {
									type = annular0;
								}
							}

							if (gg < 0.9972) {
								type = central0+", "+type;
							} else {
								type = notcentral0+", "+type;
							}
						} else {
							double mag = (1.5433 + u - gg) / (0.5461 + 2.0 * u);
							type = partial0+", mag "+DataSet.replaceAll(formatter.format(mag), ",", ".", false);
						}

						try {
							if (JPARSECException.getLastMethodName().indexOf(Saros.class.getCanonicalName()) < 0) {
								Saros saros = new Saros(jde+djde);
								String sen = ""+saros.sarosEclipseMaxNumber;
								if (sen.equals("-1")) sen = "-";
								type +=", saros " + saros.sarosSeries+", eclipse "+saros.sarosEclipseNumber+"/"+sen+", inex "+saros.inexCycle;
							}
						} catch (Exception exc) {}
					}
				} else {
					djde = -0.4072 * Math.sin(Mp) + 0.17241*E*Math.sin(M)+
						0.01608*Math.sin(2.0*Mp)+0.01039*Math.sin(2.0*F)+
						0.00739*E*Math.sin(Mp-M)-0.00514*E*Math.sin(Mp+M)+
						0.00208*E*E*Math.sin(2.0*M)-0.00111*Math.sin(Mp-2.0*F)-
						0.00057*Math.sin(Mp+2.0*F)+0.00056*E*Math.sin(2.0*Mp+M)-
						0.00042*Math.sin(3.0*Mp)+0.00042*E*Math.sin(M+2.0*F)+
						0.00038*E*Math.sin(M-2.0*F)-0.00024*E*Math.sin(2.0*Mp-M)-
						0.00007*Math.sin(Mp+2.0*M)-0.00017*Math.sin(O);
					djde = djde + 0.00004 * (Math.sin(2.0*Mp-2.0*F) + Math.sin(3.0 * M)) +
						0.00003 * (Math.sin(Mp+M-2.0*F) + Math.sin(2.0*Mp+2.0*F) - Math.sin(Mp+M+2.0*F) + Math.sin(Mp-M+2.0*F)) +
						0.00002 * (-Math.sin(Mp-M-2.0*F) - Math.sin(3.0*Mp+M) + Math.sin(4.0*Mp));

					djde = djde + 0.000325*Math.sin(A1) + 0.000165*Math.sin(A2) + 0.000164*Math.sin(A3) +
					0.000126*Math.sin(A4) + 0.000110*Math.sin(A5) + 0.000062*Math.sin(A6) + 0.000060*Math.sin(A7) +
					0.000056*Math.sin(A8) + 0.000047*Math.sin(A9) + 0.000042*Math.sin(A10) + 0.000040*Math.sin(A11) +
					0.000037*Math.sin(A12) + 0.000035*Math.sin(A13) + 0.000023*Math.sin(A14);
				}
				break;
			case MOON_FIRST_QUARTER:
				djde = W-0.62801 * Math.sin(Mp) + 0.17172*E*Math.sin(M)+
					0.00862*Math.sin(2.0*Mp)+0.00804*Math.sin(2.0*F)+
					0.00454*E*Math.sin(Mp-M)-0.01183*E*Math.sin(Mp+M)+
					0.00204*E*E*Math.sin(2.0*M)-0.00180*Math.sin(Mp-2.0*F)-
					0.0007*Math.sin(Mp+2.0*F)-0.00040*Math.sin(3.0*Mp)-0.00034*E*Math.sin(2.0*Mp-M)+0.00032*E*Math.sin(M+2.0*F)+0.00032*E*Math.sin(M-2.0*F)-
					0.00028*E*E*Math.sin(Mp+2.0*M)+0.00027*E*Math.sin(2.0*Mp+M)-0.00017*Math.sin(O)-0.00005*Math.sin(Mp-M-2.0*F)+
					0.00004*Math.sin(2.0*Mp+2.0*F)-0.00004*Math.sin(Mp+M+2.0*F)+0.00004*Math.sin(Mp-2*M)+0.00003*Math.sin(Mp+M-2.0*F)+
					0.00003*Math.sin(3.0*M)+0.00002*Math.sin(2.0*Mp-2.0*F)+0.00002*Math.sin(Mp-M+2.0*F)-0.00002*Math.sin(3.0*Mp+M);

				djde = djde + 0.000325*Math.sin(A1) + 0.000165*Math.sin(A2) + 0.000164*Math.sin(A3) +
				0.000126*Math.sin(A4) + 0.000110*Math.sin(A5) + 0.000062*Math.sin(A6) + 0.000060*Math.sin(A7) +
				0.000056*Math.sin(A8) + 0.000047*Math.sin(A9) + 0.000042*Math.sin(A10) + 0.000040*Math.sin(A11) +
				0.000037*Math.sin(A12) + 0.000035*Math.sin(A13) + 0.000023*Math.sin(A14);
				break;
			case MOON_LAST_QUARTER:
				djde = -W-0.62801 * Math.sin(Mp) + 0.17172*E*Math.sin(M)+
					0.00862*Math.sin(2.0*Mp)+0.00804*Math.sin(2.0*F)+
					0.00454*E*Math.sin(Mp-M)-0.01183*E*Math.sin(Mp+M)+
					0.00204*E*E*Math.sin(2.0*M)-0.00180*Math.sin(Mp-2.0*F)-
					0.0007*Math.sin(Mp+2.0*F)-0.00040*Math.sin(3.0*Mp)-0.00034*E*Math.sin(2.0*Mp-M)+0.00032*E*Math.sin(M+2.0*F)+0.00032*E*Math.sin(M-2.0*F)-
					0.00028*E*E*Math.sin(Mp+2.0*M)+0.00027*E*Math.sin(2.0*Mp+M)-0.00017*Math.sin(O)-0.00005*Math.sin(Mp-M-2.0*F)+
					0.00004*Math.sin(2.0*Mp+2.0*F)-0.00004*Math.sin(Mp+M+2.0*F)+0.00004*Math.sin(Mp-2*M)+0.00003*Math.sin(Mp+M-2.0*F)+
					0.00003*Math.sin(3.0*M)+0.00002*Math.sin(2.0*Mp-2.0*F)+0.00002*Math.sin(Mp-M+2.0*F)-0.00002*Math.sin(3.0*Mp+M);

				djde = djde + 0.000325*Math.sin(A1) + 0.000165*Math.sin(A2) + 0.000164*Math.sin(A3) +
				0.000126*Math.sin(A4) + 0.000110*Math.sin(A5) + 0.000062*Math.sin(A6) + 0.000060*Math.sin(A7) +
				0.000056*Math.sin(A8) + 0.000047*Math.sin(A9) + 0.000042*Math.sin(A10) + 0.000040*Math.sin(A11) +
				0.000037*Math.sin(A12) + 0.000035*Math.sin(A13) + 0.000023*Math.sin(A14);
				break;
			default:
				throw new JPARSECException("invalid event.");
			}

			jde += djde;
			k = k + delta;
		} while (eclipse && !eclipseFound);

		if (Translate.getDefaultLanguage() != Translate.LANGUAGE.ENGLISH) {
			String annular = Translate.translate(167); // annular
			String hybrid = Translate.translate(962); // hybrid
			String partial = Translate.translate(169); // partial
			String umbra = Translate.getEntry(965, null); // umbra
			String psd = Translate.getEntry(963, null); // partial phase semi-duration
			String tsd = Translate.getEntry(964, null); // total phase semi-duration
			String notcentral = Translate.getEntry(966, null); // not central

			type = DataSet.replaceAll(type, annular0, annular, false);
			type = DataSet.replaceAll(type, hybrid0, hybrid, false);
			type = DataSet.replaceAll(type, psd0, psd, false);
			type = DataSet.replaceAll(type, tsd0, tsd, false);
			type = DataSet.replaceAll(type, partial0, partial, false);
			type = DataSet.replaceAll(type, " "+umbra0, " "+umbra, false);
			type = DataSet.replaceAll(type, notcentral0, notcentral, false);
		}

		// Correct Meeus results for secular acceleration
		double deltaT = Elp2000.timeCorrectionForSecularAcceleration(jde) - jde;
		jde -= deltaT;

		// Supermoon ?
		if (event == EVENT.MOON_FULL) {
			TimeElement mtime = new TimeElement(jde, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
			EphemerisElement eph = new EphemerisElement(TARGET.Moon, EphemerisElement.COORDINATES_TYPE.APPARENT,
					EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.GEOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
					EphemerisElement.FRAME.DYNAMICAL_EQUINOX_J2000, EphemerisElement.ALGORITHM.MOSHIER); 
			eph.optimizeForSpeed();
			CityElement city = City.findCity("Madrid");
			ObserverElement observer = ObserverElement.parseCity(city);
			EphemElement mephem = Ephem.getEphemeris(mtime, observer, eph, false);
			boolean superMoon = (mephem.distance * Constant.AU) < 360000;
			if (superMoon) {
				if (type.equals("")) {
					type = Translate.translate(1323);
				} else {
					type += " ("+Translate.translate(1323)+")";
				}
			}
		}

		SimpleEventElement see = new SimpleEventElement(jde, inputEvent, type);
		see.body = TARGET.Moon.getName();
		return see;
	}

	/**
	 * Calculates the instant of an equinox or solstice, with a precission of 1s.
	 * @param year The year.
	 * @param event The event, constants defined in {@linkplain SimpleEventElement} (SUN_...).
	 * @return The event.
	 * @throws JPARSECException If an error occurs.
	 */
	public static SimpleEventElement EquinoxesAndSolstices(int year, SimpleEventElement.EVENT event) throws JPARSECException {
		int m = 3;
		if (event == SimpleEventElement.EVENT.SUN_SUMMER_SOLSTICE) m = 6;
		if (event == SimpleEventElement.EVENT.SUN_AUTUMN_EQUINOX) m = 9;
		if (event == SimpleEventElement.EVENT.SUN_WINTER_SOLSTICE) m = 12;
		AstroDate astro = new AstroDate(year, m, 1);
		double jd = astro.jd();
		// Since official instants of equinoxes and solstices are the result of geocentric
		// ephemerides (the same instants for the entire Earth), an inpu observer is not
		// needed in this method. Here I consider Madrid as city for the ephemerides because
		// I need an observer somewhere, but Madrid is not used. The same applies to meteor
		// showers and Saturn rings in the next methods.
		CityElement city = City.findCity("Madrid");
		ObserverElement observer = ObserverElement.parseCity(city);
		EphemerisElement eph = new EphemerisElement(TARGET.SUN, EphemerisElement.COORDINATES_TYPE.APPARENT,
				EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.GEOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
				EphemerisElement.FRAME.ICRF, EphemerisElement.ALGORITHM.MOSHIER);
		eph.optimizeForSpeed();

		double delta = 1.0, precision = 0.1 / (24.0 * 3600.0);
		do {
			TimeElement time = new TimeElement(jd, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
			EphemElement ephem = Ephem.getEphemeris(time, observer, eph, false);
			LocationElement loc = CoordinateSystem.equatorialToEcliptic(ephem.getEquatorialLocation(), time, observer, eph);
			switch (event) {
			case SUN_SPRING_EQUINOX:
				delta = 58.13 * Math.sin(-loc.getLongitude());
				break;
			case SUN_SUMMER_SOLSTICE:
				delta = 58.13 * Math.sin(Constant.PI_OVER_TWO-loc.getLongitude());
				break;
			case SUN_AUTUMN_EQUINOX:
				delta = 58.13 * Math.sin(Math.PI-loc.getLongitude());
				break;
			case SUN_WINTER_SOLSTICE:
				delta = 58.13 * Math.sin(-Constant.PI_OVER_TWO-loc.getLongitude());
				break;
			default:
				throw new JPARSECException("invalid event.");
			}
			jd = jd + delta;
		} while (Math.abs(delta) > precision);

		SimpleEventElement see = new SimpleEventElement(jd, event, "");
		see.body = TARGET.SUN.getName();
		return see;
	}

	/**
	 * Calculates the instants of the maximum of meteor showers.
	 * @param year The year.
	 * @return The events. The details of the event will be set as
	 * Spanish name|English name|radiant position|THZ|Julian day of maximum in TDB|JD in TDB of first date when
	 * the shower is visible|JD in TDB of the last date when it is visible.
	 * @throws JPARSECException If an error occurs.
	 */
	public static SimpleEventElement[] meteorShowers(int year) throws JPARSECException {
		ArrayList<String> meteor = ReadFile.readResource(FileIO.DATA_SKY_DIRECTORY + "meteorShowers.txt", ReadFile.ENCODING_UTF_8);
		ArrayList<SimpleEventElement> events = new ArrayList<SimpleEventElement>();
		AstroDate astro = new AstroDate(year, 1, 1);
		double jd = astro.jd();
		CityElement city = City.findCity("Madrid");
		ObserverElement observer = ObserverElement.parseCity(city);
		EphemerisElement eph = new EphemerisElement(TARGET.SUN, EphemerisElement.COORDINATES_TYPE.APPARENT,
				EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.GEOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
				EphemerisElement.FRAME.ICRF, EphemerisElement.ALGORITHM.MOSHIER);
		eph.optimizeForSpeed();
		String months[] = new String[] {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
		for (int i=1;i<meteor.size(); i++) {
			String li = meteor.get(i);
			String name = FileIO.getField(1, li, ";", false) + "|"+FileIO.getField(10, li, ";", false);
			double sunLon = Double.parseDouble(FileIO.getField(4, li, ";", false));
			double radRA = Double.parseDouble(FileIO.getField(5, li, ";", false)) * Constant.DEG_TO_RAD;
			double radDEC = Double.parseDouble(FileIO.getField(6, li, ";", false)) * Constant.DEG_TO_RAD;
			String thz = FileIO.getField(9, li, ";", false);

			String lim = FileIO.getField(2, li, ";", false);
			String lmin = FileIO.getField(1, lim, "-", false);
			String lmax = FileIO.getField(2, lim, "-", false);
			String lminm = FileIO.getField(1, lmin, " ", false).trim();
			String lmind = FileIO.getField(2, lmin, " ", false).trim();
			String lmaxm = FileIO.getField(1, lmax, " ", false).trim();
			String lmaxd = FileIO.getField(2, lmax, " ", false).trim();
			String max = FileIO.getField(3, li, ";", false);
			String maxm = FileIO.getField(1, max, " ", false).trim();
			String maxd = FileIO.getField(2, max, " ", false).trim();
			AstroDate lmina = new AstroDate(year, 1 + DataSet.getIndex(months, lminm), Integer.parseInt(lmind));
			AstroDate lmaxa = new AstroDate(year, 1 + DataSet.getIndex(months, lmaxm), Integer.parseInt(lmaxd));
			AstroDate maxa = new AstroDate(year, 1 + DataSet.getIndex(months, maxm), Integer.parseInt(maxd));

			LocationElement loc = LocationElement.parseRectangularCoordinates(Precession.precessFromJ2000(jd, LocationElement.parseLocationElement(new LocationElement(radRA, radDEC, 1.0)), eph));
			String radiant = Functions.formatRAOnlyMinutes(loc.getLongitude(), 1) + ", "+Functions.formatDECOnlyMinutes(loc.getLatitude(), 1);

			LocationElement lon = LocationElement.parseRectangularCoordinates(Precession.precessPosAndVelInEcliptic(Constant.J2000, jd, LocationElement.parseLocationElement(new LocationElement(sunLon * Constant.DEG_TO_RAD, 0, 1)), eph));
			double time = Calendar.solarLongitudeAfter(Calendar.fixedFromJD(jd), lon.getLongitude() * Constant.RAD_TO_DEG);
			time = 1721424.5 + time;
			TimeElement t = new TimeElement(time, SCALE.UNIVERSAL_TIME_UT1);
			double tdb = TimeScale.getJD(t, observer, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);

			double mint = tdb - Math.abs(maxa.jd() - lmina.jd());
			double maxt = tdb + Math.abs(maxa.jd() - lmaxa.jd());
			SimpleEventElement see = new SimpleEventElement(tdb, SimpleEventElement.EVENT.METEOR_SHOWER, name+"|"+radiant+"|"+thz+"|"+tdb+"|"+mint+"|"+maxt);
			events.add(see);
		}
		SimpleEventElement ev[] = new SimpleEventElement[events.size()];
		for (int i=0; i<ev.length; i++) {
			ev[i] = events.get(i);
			ev[i].body = TARGET.EARTH.getName();
		}
		return ev;
	}

	private static double round(double kapprox, double delta, EVENT_TIME eventType) {
		double k = delta + Math.floor(kapprox);
		if (eventType == EVENT_TIME.NEXT && k < kapprox) k ++;
		if (eventType == EVENT_TIME.PREVIOUS && k > kapprox) k --;
		if (eventType == EVENT_TIME.CLOSEST && k < kapprox - 0.5 && delta == 0.0) k ++;
		return k;
	}

	/**
	 * Calculates the time when the rings of Saturn will be visible edge-on
	 * (more exactly, when the position angle of pole will be 0 from Earth's
	 * geocenter). Calculations are done using Moshier method with an accuracy
	 * of 1 minute for the output time.
	 * @param jd Starting calculation time.
	 * @param eventType Event type.
	 * @return The event.
	 * @throws JPARSECException If an error occurs.
	 */
	public static SimpleEventElement SaturnRingsEdgeOn(double jd, EVENT_TIME eventType) throws JPARSECException {
		TimeElement time = new TimeElement(jd, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		CityElement city = City.findCity("Madrid");
		ObserverElement observer = ObserverElement.parseCity(city);
		EphemerisElement eph = new EphemerisElement(TARGET.SATURN, EphemerisElement.COORDINATES_TYPE.APPARENT,
				EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.GEOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2009,
				EphemerisElement.FRAME.DYNAMICAL_EQUINOX_J2000, EphemerisElement.ALGORITHM.MOSHIER);
		eph.optimizeForSpeed();
		double step = 365, precision = 1.0 / (60.0 * 24.0);
		if (eventType == EVENT_TIME.CLOSEST) {
			time.add(-step);
			EphemElement ephem1 = Ephem.getEphemeris(time, observer, eph, false);
			time.add(2*step);
			EphemElement ephem2 = Ephem.getEphemeris(time, observer, eph, false);
			time.add(-step);
			if (Math.abs(ephem1.subsolarLatitude) < Math.abs(ephem2.subsolarLatitude)) step = -step;
		} else {
			if (eventType == EVENT_TIME.PREVIOUS) step = -step;
		}
		
		// First the minimum inclination as seen from Sun
		do {
			EphemElement ephem0 = Ephem.getEphemeris(time, observer, eph, false);
			double pa0 = ephem0.subsolarLatitude, pa1 = pa0;
			if (pa0 == 0.0) break;
			do {
				time.add(step);
				EphemElement ephem1 = Ephem.getEphemeris(time, observer, eph, false);
				pa1 = ephem1.subsolarLatitude;

				if (pa1 == 0.0) break;
			} while (FastMath.sign(pa0) == FastMath.sign(pa1));
			if (pa1 == 0.0) break;
			time.add(-step);
			step = step / 2.0;
		} while (Math.abs(step) > precision);
		
		step = 10;
		// Now as seen from Earth
		do {
			EphemElement ephem0 = Ephem.getEphemeris(time, observer, eph, false);
			double pa0 = ephem0.positionAngleOfPole, pa1 = pa0;
			if (pa0 == 0.0) break;
			do {
				time.add(step);
				EphemElement ephem1 = Ephem.getEphemeris(time, observer, eph, false);
				pa1 = ephem1.positionAngleOfPole;
				if (FastMath.sign(pa0) == FastMath.sign(pa1) && Math.abs(pa1) > Math.abs(pa0))
					step = -step;

				if (pa1 == 0.0) break;
			} while (FastMath.sign(pa0) == FastMath.sign(pa1));
			if (pa1 == 0.0) break;
			time.add(-step);
			step = step / 2.0;
		} while (Math.abs(step) > precision);


		String details = "";
		SimpleEventElement see = new SimpleEventElement(time.astroDate.jd(), EVENT.SATURN_RINGS_EDGE_ON, details);
		see.body = TARGET.SATURN.getName();
		return see;
	}

	/**
	 * Calculates the time when the rings of Saturn will be visible with
	 * maximum aperture (more exactly, when the position angle of pole will be
	 * maximum or minimum from Earth's geocenter). Calculations are done using
	 * Moshier method with an accuracy of 1 minute for the output time.
	 * @param jd Starting calculation time.
	 * @param eventType Event type.
	 * @return The event. The details field will contain the position angle of
	 * pole in degrees.
	 * @throws JPARSECException If an error occurs.
	 */
	public static SimpleEventElement SaturnRingsMaximumAperture(double jd, EVENT_TIME eventType) throws JPARSECException {
		TimeElement time = new TimeElement(jd, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		CityElement city = City.findCity("Madrid");
		ObserverElement observer = ObserverElement.parseCity(city);
		EphemerisElement eph = new EphemerisElement(TARGET.SATURN, EphemerisElement.COORDINATES_TYPE.APPARENT,
				EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.GEOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2009,
				EphemerisElement.FRAME.DYNAMICAL_EQUINOX_J2000, EphemerisElement.ALGORITHM.MOSHIER);
		eph.optimizeForSpeed();
		double step = 365, precision = 1.0 / (60.0 * 24.0);
		if (eventType == EVENT_TIME.CLOSEST) {
			time.add(-step);
			EphemElement ephem1 = Ephem.getEphemeris(time, observer, eph, false);
			time.add(2*step);
			EphemElement ephem2 = Ephem.getEphemeris(time, observer, eph, false);
			time.add(-step);
			if (Math.abs(ephem1.subsolarLatitude) > Math.abs(ephem2.subsolarLatitude)) step = -step;
		} else {
			if (eventType == EVENT_TIME.PREVIOUS) step = -step;
		}

		EphemElement ephem0 = null;
		ephem0 = Ephem.getEphemeris(time, observer, eph, false);
		time.add(step);
		EphemElement ephem1 = Ephem.getEphemeris(time, observer, eph, false);
		double dif = Math.abs(ephem1.positionAngleOfPole) - Math.abs(ephem0.positionAngleOfPole);
		if (dif < 0) {
			// First go to pa = 0
			SimpleEventElement s = SaturnRingsEdgeOn(jd, eventType);
			time = new TimeElement(s.time + step, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		}

		// First the maximum inclination as seen from Sun
		do {
			ephem0 = Ephem.getEphemeris(time, observer, eph, false);
			dif = 1.0;
			double oldpa = ephem0.subsolarLatitude;
			do {
				time.add(step);
				ephem1 = Ephem.getEphemeris(time, observer, eph, false);
				dif = Math.abs(ephem1.subsolarLatitude) - Math.abs(oldpa);
				oldpa = ephem1.subsolarLatitude;
			} while (dif > 0);
			step = -step / 2.0;
		} while (Math.abs(step) > precision);

		step = 10;
		// Now as seen from Earth
		do {
			ephem0 = Ephem.getEphemeris(time, observer, eph, false);
			dif = 1.0;
			double oldpa = ephem0.positionAngleOfPole;
			do {
				time.add(step);
				ephem1 = Ephem.getEphemeris(time, observer, eph, false);
				dif = Math.abs(ephem1.positionAngleOfPole) - Math.abs(oldpa);
				oldpa = ephem1.positionAngleOfPole;
			} while (dif > 0);
			step = -step / 2.0;
		} while (Math.abs(step) > precision);


		String details = ""+(float)(ephem0.positionAngleOfPole*Constant.RAD_TO_DEG);
		SimpleEventElement see = new SimpleEventElement(time.astroDate.jd(), EVENT.SATURN_RINGS_MAXIMUM_APERTURE, details);
		see.body = TARGET.SATURN.getName();
		return see;
	}

	/**
	 * Calculates the approximate instant of the closest perihelion or aphelion in time following Meeus.
	 * In current dates the difference compared to {@linkplain MainEvents#getPlanetaryEvent(Target.TARGET, double, 
	 * SimpleEventElement.EVENT, MainEvents.EVENT_TIME, boolean)}
	 * is of a few hours for inner planets and more than one day for giant planets, and the other method
	 * is more accurate than this one.
	 * @param target Target body. Earth is allowed.
	 * @param jd The starting Julian day of calculations.
	 * @param event The event, constants defined in {@linkplain SimpleEventElement} (PLANET_...).
	 * @param eventType The event type (next, last, or closest to input date).  The use of the
	 * closest option is recommended when possible, since next/previous events could give incorrect
	 * events for a given date far from J2000.
	 * @return The event. The target body ID value is given in the details field.
	 * @throws JPARSECException If an error occurs.
	 * @deprecated {@linkplain MainEvents#getPlanetaryEvent(Target.TARGET, double, 
	 * SimpleEventElement.EVENT, MainEvents.EVENT_TIME, boolean)} is
	 * recommended instead.
	 */
	public static SimpleEventElement PerihelionAndAphelion(TARGET target, double jd, EVENT event, EVENT_TIME eventType) throws JPARSECException {
		AstroDate astro = new AstroDate(jd);
		double year = astro.getAstronomicalYear() + (astro.getMonth() - 1.0 + astro.getDayPlusFraction() / (1.0 + astro.getDaysInMonth())) / 12.0;

		double k = 0, delta = 0, jde = 0;
		if (event == SimpleEventElement.EVENT.PLANET_MAXIMUM_DISTANCE_FROM_SUN) delta = 0.5;
		switch (target) {
		case MERCURY:
			k = 4.15201 * (year - 2000.12);
			k = MainEvents.round(k, delta, eventType);
			jde = 2451590.257 + 87.96934963 * k;
			// Fix k if required
			if (jde > jd && eventType == EVENT_TIME.PREVIOUS) k --;
			if (jde < jd && eventType == EVENT_TIME.NEXT) k ++;
			if (jde > jd && eventType != EVENT_TIME.PREVIOUS) {
				double km = k - 1;
				double newjde = 2451590.257 + 87.96934963 * km;
				if ((newjde > jd && eventType == EVENT_TIME.NEXT) ||
						(Math.abs(jd-newjde) < Math.abs(jd-jde) && eventType == EVENT_TIME.CLOSEST)) k = km;
			}
			if (jde < jd && eventType != EVENT_TIME.NEXT) {
				double kp = k + 1;
				double newjde = 2451590.257 + 87.96934963 * kp;
				if ((newjde < jd && eventType == EVENT_TIME.PREVIOUS) ||
						(Math.abs(jd-newjde) < Math.abs(jd-jde) && eventType == EVENT_TIME.CLOSEST)) k = kp;
			}
			jde = 2451590.257 + 87.96934963 * k;

			jd = jde;
			break;
		case VENUS:
			k = 1.62549 * (year - 2000.53);
			k = MainEvents.round(k, delta, eventType);
			jde = 2451738.233 + 224.7008188 * k - 0.0000000327 * k * k;
			// Fix k if required
			if (jde > jd && eventType == EVENT_TIME.PREVIOUS) k --;
			if (jde < jd && eventType == EVENT_TIME.NEXT) k ++;
			if (jde > jd && eventType != EVENT_TIME.PREVIOUS) {
				double km = k - 1;
				double newjde = 2451738.233 + 224.7008188 * km - 0.0000000327 * km * km;
				if ((newjde > jd && eventType == EVENT_TIME.NEXT) ||
						(Math.abs(jd-newjde) < Math.abs(jd-jde) && eventType == EVENT_TIME.CLOSEST)) k = km;
			}
			if (jde < jd && eventType != EVENT_TIME.NEXT) {
				double kp = k + 1;
				double newjde = 2451738.233 + 224.7008188 * kp - 0.0000000327 * kp * kp;
				if ((newjde < jd && eventType == EVENT_TIME.PREVIOUS) ||
						(Math.abs(jd-newjde) < Math.abs(jd-jde) && eventType == EVENT_TIME.CLOSEST)) k = kp;
			}
			jde = 2451738.233 + 224.7008188 * k - 0.0000000327 * k * k;

			jd = jde;
			break;
		case EARTH:
		case Earth_Moon_Barycenter:
			k = 0.99997 * (year - 2000.01);
			k = MainEvents.round(k, delta, eventType);
			jde = 2451547.507 + 365.2596358 * k + 0.0000000156 * k * k;
			// Fix k if required
			if (jde > jd && eventType == EVENT_TIME.PREVIOUS) k --;
			if (jde < jd && eventType == EVENT_TIME.NEXT) k ++;
			if (jde > jd && eventType != EVENT_TIME.PREVIOUS) {
				double km = k - 1;
				double newjde = 2451547.507 + 365.2596358 * km + 0.0000000156 * km * km;
				if ((newjde > jd && eventType == EVENT_TIME.NEXT) ||
						(Math.abs(jd-newjde) < Math.abs(jd-jde) && eventType == EVENT_TIME.CLOSEST)) k = km;
			}
			if (jde < jd && eventType != EVENT_TIME.NEXT) {
				double kp = k + 1;
				double newjde = 2451547.507 + 365.2596358 * kp + 0.0000000156 * kp * kp;
				if ((newjde < jd && eventType == EVENT_TIME.PREVIOUS) ||
						(Math.abs(jd-newjde) < Math.abs(jd-jde) && eventType == EVENT_TIME.CLOSEST)) k = kp;
			}
			jde = 2451547.507 + 365.2596358 * k + 0.0000000156 * k * k;

			jd = jde;

            // Apply the corrections from Barycenter (Meeus, Astronomical Algorithms - 2nd Edition, page 273)
			if (target == TARGET.EARTH) {
	            double A1 = Functions.normalizeDegrees(328.41 + 132.788585 * k) * Constant.DEG_TO_RAD;
	            double A2 = Functions.normalizeDegrees(316.13 + 584.903153 * k) * Constant.DEG_TO_RAD;
	            double A3 = Functions.normalizeDegrees(346.20 + 450.380738 * k) * Constant.DEG_TO_RAD;
	            double A4 = Functions.normalizeDegrees(136.95 + 659.306737 * k) * Constant.DEG_TO_RAD;
	            double A5 = Functions.normalizeDegrees(249.52 + 329.653368 * k) * Constant.DEG_TO_RAD;

	            if (event == SimpleEventElement.EVENT.PLANET_MINIMUM_DISTANCE_FROM_SUN) {
	                jd += 1.278 * Math.sin(A1);
	                jd -= 0.055 * Math.sin(A2);
	                jd -= 0.091 * Math.sin(A3);
	                jd -= 0.056 * Math.sin(A4);
	                jd -= 0.045 * Math.sin(A5);
	            } else {
	                jd -= 1.352 * Math.sin(A1);
	                jd += 0.061 * Math.sin(A2);
	                jd += 0.062 * Math.sin(A3);
	                jd += 0.029 * Math.sin(A4);
	                jd += 0.031 * Math.sin(A5);
	            }
			}
			break;
		case MARS:
			k = 0.53166 * (year - 2001.78);
			k = MainEvents.round(k, delta, eventType);
			jde = 2452195.026 + 686.9957857 * k - 0.0000001187 * k * k;
			// Fix k if required
			if (jde > jd && eventType == EVENT_TIME.PREVIOUS) k --;
			if (jde < jd && eventType == EVENT_TIME.NEXT) k ++;
			if (jde > jd && eventType != EVENT_TIME.PREVIOUS) {
				double km = k - 1;
				double newjde = 2452195.026 + 686.9957857 * km - 0.0000001187 * km * km;
				if ((newjde > jd && eventType == EVENT_TIME.NEXT) ||
						(Math.abs(jd-newjde) < Math.abs(jd-jde) && eventType == EVENT_TIME.CLOSEST)) k = km;
			}
			if (jde < jd && eventType != EVENT_TIME.NEXT) {
				double kp = k + 1;
				double newjde = 2452195.026 + 686.9957857 * kp - 0.0000001187 * kp * kp;
				if ((newjde < jd && eventType == EVENT_TIME.PREVIOUS) ||
						(Math.abs(jd-newjde) < Math.abs(jd-jde) && eventType == EVENT_TIME.CLOSEST)) k = kp;
			}
			jde = 2452195.026 + 686.9957857 * k - 0.0000001187 * k * k;

			jd = jde;
			break;
		case JUPITER:
			k = 0.08430 * (year - 2011.20);
			k = MainEvents.round(k, delta, eventType);
			jde = 2455636.936 + 4332.897065 * k + 0.0001367 * k * k;
			// Fix k if required
			if (jde > jd && eventType == EVENT_TIME.PREVIOUS) k --;
			if (jde < jd && eventType == EVENT_TIME.NEXT) k ++;
			if (jde > jd && eventType != EVENT_TIME.PREVIOUS) {
				double km = k - 1;
				double newjde = 2455636.936 + 4332.897065 * km + 0.0001367 * km * km;
				if ((newjde > jd && eventType == EVENT_TIME.NEXT) ||
						(Math.abs(jd-newjde) < Math.abs(jd-jde) && eventType == EVENT_TIME.CLOSEST)) k = km;
			}
			if (jde < jd && eventType != EVENT_TIME.NEXT) {
				double kp = k + 1;
				double newjde = 2455636.936 + 4332.897065 * kp + 0.0001367 * kp * kp;
				if ((newjde < jd && eventType == EVENT_TIME.PREVIOUS) ||
						(Math.abs(jd-newjde) < Math.abs(jd-jde) && eventType == EVENT_TIME.CLOSEST)) k = kp;
			}
			jde = 2455636.936 + 4332.897065 * k + 0.0001367 * k * k;

			jd = jde;
			break;
		case SATURN:
			k = 0.03393 * (year - 2003.52);
			k = MainEvents.round(k, delta, eventType);
			jde = 2452830.12 + 10764.21676 * k + 0.000827 * k * k;
			// Fix k if required
			if (jde > jd && eventType == EVENT_TIME.PREVIOUS) k --;
			if (jde < jd && eventType == EVENT_TIME.NEXT) k ++;
			if (jde > jd && eventType != EVENT_TIME.PREVIOUS) {
				double km = k - 1;
				double newjde = 2452830.12 + 10764.21676 * km + 0.000827 * km * km;
				if ((newjde > jd && eventType == EVENT_TIME.NEXT) ||
						(Math.abs(jd-newjde) < Math.abs(jd-jde) && eventType == EVENT_TIME.CLOSEST)) k = km;
			}
			if (jde < jd && eventType != EVENT_TIME.NEXT) {
				double kp = k + 1;
				double newjde = 2452830.12 + 10764.21676 * kp + 0.000827 * kp * kp;
				if ((newjde < jd && eventType == EVENT_TIME.PREVIOUS) ||
						(Math.abs(jd-newjde) < Math.abs(jd-jde) && eventType == EVENT_TIME.CLOSEST)) k = kp;
			}
			jde = 2452830.12 + 10764.21676 * k + 0.000827 * k * k;

			jd = jde;
			break;
		case URANUS:
			k = 0.01190 * (year - 2051.1);
			k = MainEvents.round(k, delta, eventType);
			jde = 2470213.5 + 30694.8767 * k - 0.00541 * k * k;
			// Fix k if required
			if (jde > jd && eventType == EVENT_TIME.PREVIOUS) k --;
			if (jde < jd && eventType == EVENT_TIME.NEXT) k ++;
			if (jde > jd && eventType != EVENT_TIME.PREVIOUS) {
				double km = k - 1;
				double newjde = 2470213.5 + 30694.8767 * km - 0.00541 * km * km;
				if ((newjde > jd && eventType == EVENT_TIME.NEXT) ||
						(Math.abs(jd-newjde) < Math.abs(jd-jde) && eventType == EVENT_TIME.CLOSEST)) k = km;
			}
			if (jde < jd && eventType != EVENT_TIME.NEXT) {
				double kp = k + 1;
				double newjde = 2470213.5 + 30694.8767 * kp - 0.00541 * kp * kp;
				if ((newjde < jd && eventType == EVENT_TIME.PREVIOUS) ||
						(Math.abs(jd-newjde) < Math.abs(jd-jde) && eventType == EVENT_TIME.CLOSEST)) k = kp;
			}
			jde = 2470213.5 + 30694.8767 * k - 0.00541 * k * k;

			jd = jde;
			break;
		case NEPTUNE:
			k = 0.00607 * (year - 2047.5);
			k = MainEvents.round(k, delta, eventType);
			jde = 2468895.1 + 60190.33 * k + 0.03429 * k * k;
			// Fix k if required
			if (jde > jd && eventType == EVENT_TIME.PREVIOUS) k --;
			if (jde < jd && eventType == EVENT_TIME.NEXT) k ++;
			if (jde > jd && eventType != EVENT_TIME.PREVIOUS) {
				double km = k - 1;
				double newjde = 2468895.1 + 60190.33 * km + 0.03429 * km * km;
				if ((newjde > jd && eventType == EVENT_TIME.NEXT) ||
						(Math.abs(jd-newjde) < Math.abs(jd-jde) && eventType == EVENT_TIME.CLOSEST)) k = km;
			}
			if (jde < jd && eventType != EVENT_TIME.NEXT) {
				double kp = k + 1;
				double newjde = 2468895.1 + 60190.33 * kp + 0.03429 * kp * kp;
				if ((newjde < jd && eventType == EVENT_TIME.PREVIOUS) ||
						(Math.abs(jd-newjde) < Math.abs(jd-jde) && eventType == EVENT_TIME.CLOSEST)) k = kp;
			}
			jde = 2468895.1 + 60190.33 * k + 0.03429 * k * k;

			jd = jde;
			break;
		default:
			throw new JPARSECException("invalid body.");
		}

		// Obtain distance from Sun
		TimeElement time = new TimeElement(jd, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		CityElement city = City.findCity("Madrid");
		ObserverElement observer = ObserverElement.parseCity(city);
		EphemerisElement eph = new EphemerisElement(target, EphemerisElement.COORDINATES_TYPE.APPARENT,
				EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.GEOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2009,
				EphemerisElement.FRAME.DYNAMICAL_EQUINOX_J2000, EphemerisElement.ALGORITHM.MOSHIER);
		eph.optimizeForSpeed();
		if (eph.targetBody == TARGET.EARTH) eph.targetBody = TARGET.SUN;
		EphemElement ephem = Ephem.getEphemeris(time, observer, eph, false);
		String details = "";
		if (eph.targetBody == TARGET.SUN) {
			details = Functions.formatValue(ephem.distance, 3);
		} else {
			details = Functions.formatValue(ephem.distanceFromSun, 3);
		}

		SimpleEventElement see = new SimpleEventElement(jd, event, details);
		see.body = target.getName();
		return see;
	}

	/**
	 * Calculates the instant of a given planetary conjunction, following Meeus's Astronomical
	 * Algorithms. Error is around a few hours.
	 * @param target The target ID constant. Valid objects are any planet (Mercury to Neptune,
	 * except Earth).
	 * @param jd The starting Julian day of calculations.
	 * @param eventType The event type (next, last, or closest to input date).  The use of the
	 * closest option is recommended when possible, since next/previous events could give incorrect
	 * events for a given date far from J2000.
	 * @return The event. In case the target is Mercury or Venus the details field
	 * will be 'Superior' for superior conjunction of 'Inferior' for an inferior one.
	 * @throws JPARSECException If an error occurs.
	 * @deprecated {@linkplain MainEvents#getPlanetaryEvent(Target.TARGET, double, 
	 * SimpleEventElement.EVENT, MainEvents.EVENT_TIME, boolean)}
	 * will give better results, although this method is indeed used in the other to obtain a fast
	 * first estimate.
	 */
	public static SimpleEventElement Conjunction(TARGET target, double jd, EVENT_TIME eventType)
	throws JPARSECException {
		double A = 0.0, B = 0.0, M0 = 0.0, M1 = 0.0;
		double AA = 0.0, MM0 = 0.0;
		switch (target) {
		case MERCURY:
			A = 2451612.023;
			B = 115.8774771;
			M0 = 63.5867;
			M1 = 114.2088742;
			AA = 2451554.084;
			MM0 = 6.4822;
			break;
		case VENUS:
			A = 2451996.706;
			B = 583.921361;
			M0 = 82.7311;
			M1 = 215.513058;
			AA = 2451704.746;
			MM0 = 154.9745;
			break;
		case MARS:
			A = 2451707.414;
			B = 779.936104;
			M0 = 157.6047;
			M1 = 48.705244;
			break;
		case JUPITER:
			A = 2451671.186;
			B = 398.884046;
			M0 = 121.8980;
			M1 = 33.140229;
			break;
		case SATURN:
			A = 2451681.124;
			B = 378.091904;
			M0 = 131.6934;
			M1 = 12.647487;
			break;
		case URANUS:
			A = 2451579.489;
			B = 369.656035;
			M0 = 31.5219;
			M1 = 4.333093;
			break;
		case NEPTUNE:
			A = 2451569.379;
			B = 367.486703;
			M0 = 21.5569;
			M1 = 2.194998;
			break;
		default:
			throw new JPARSECException("invalid body.");
		}
		AstroDate astro = new AstroDate(jd);
		double year = astro.getAstronomicalYear() + (astro.getMonth() - 1.0 + astro.getDayPlusFraction() / (1.0 + astro.getDaysInMonth())) / 12.0;

		double kapprox = (year * 365.2425 + 1721060.0 - A) / B;
		double k = Math.round(kapprox);
		if (eventType == EVENT_TIME.PREVIOUS && k > kapprox) k --;
		if (eventType == EVENT_TIME.NEXT && k < kapprox) k ++;
		double jde = A + k * B;
		// Fix k if required
		if (jde > jd && eventType == EVENT_TIME.PREVIOUS) k --;
		if (jde < jd && eventType == EVENT_TIME.NEXT) k ++;
		if (jde > jd && eventType != EVENT_TIME.PREVIOUS) {
			double km = k - 1;
			double newjde = A + km * B;
			if ((newjde > jd && eventType == EVENT_TIME.NEXT) ||
					(Math.abs(jd-newjde) < Math.abs(jd-jde) && eventType == EVENT_TIME.CLOSEST)) k = km;
		}
		if (jde < jd && eventType != EVENT_TIME.NEXT) {
			double kp = k + 1;
			double newjde = A + kp * B;
			if ((newjde < jd && eventType == EVENT_TIME.PREVIOUS) ||
					(Math.abs(jd-newjde) < Math.abs(jd-jde) && eventType == EVENT_TIME.CLOSEST)) k = kp;
		}
		jde = A + k * B;

		boolean inferior = true;

		if (target == TARGET.MERCURY || target == TARGET.VENUS) {
			double kapprox2 = (year * 365.2425 + 1721060.0 - AA) / B;
			double k2 = Math.round(kapprox2);
			if (eventType == EVENT_TIME.PREVIOUS && k2 > kapprox2) k2 --;
			if (eventType == EVENT_TIME.NEXT && k2 < kapprox2) k2 ++;
			double jde2 = AA + k2 * B;
			if (Math.abs(jd - jde2) < Math.abs(jd - jde)) {
				jde = jde2;
				k = k2;
				A = AA;
				M0 = MM0;
				inferior = false;
			}
		}

		double M = (M0 + k * M1) * Constant.DEG_TO_RAD;
		double T = Functions.toCenturies(jde);

		double a = 82.74 + 40.76 * T, b = 29.86 + 1181.36 * T, c = 14.13 + 590.68 * T,
				d = 220.02 + 1262.87 * T, e = 207.83 + 8.51 * T, f = 108.84 + 419.96 * T,
				g = 276.74 + 209.98 * T;
		a *= Constant.DEG_TO_RAD;
		b *= Constant.DEG_TO_RAD;
		c *= Constant.DEG_TO_RAD;
		d *= Constant.DEG_TO_RAD;
		e *= Constant.DEG_TO_RAD;
		f *= Constant.DEG_TO_RAD;
		g *= Constant.DEG_TO_RAD;

		switch (target) {
		case MERCURY:
			if (inferior) {
				jde += (0.0545 + 0.0002 * T) +
						(-6.2008 + 0.0074 * T+ 0.00003 * T * T) * Math.sin(M) +
						(-3.2750 - 0.0197 * T + 0.00001 * T * T) * Math.cos(M) +
						(0.4737 - 0.0052 * T - 0.00001 * T * T) * Math.sin(2.0 * M) +
						(0.81111 + 0.0033 * T - 0.00002 * T * T) * Math.cos(2.0 * M) +
						(0.0037 + 0.0018 * T) * Math.sin(3.0 * M) +
						(-0.1768 + 0.00001 * T * T) * Math.cos(3.0 * M) +
						(-0.0211 - 0.0004 * T) * Math.sin(4.0 * M) +
						(0.0326 - 0.0003 * T) * Math.cos(4.0 * M) +
						(0.0083 + 0.0001 * T) * Math.sin(5.0 * M) +
						(-0.0040 + 0.0001 * T) * Math.cos(5.0 * M);
			} else {
				jde += (-0.0545 - 0.0002 * T) +
						(7.3894 - 0.0100 * T - 0.00003 * T * T) * Math.sin(M) +
						(3.2200 + 0.0197 * T - 0.00001 * T * T) * Math.cos(M) +
						(0.8383 - 0.0064 * T - 0.00001 * T * T) * Math.sin(2.0 * M) +
						(0.9666 + 0.0039 * T - 0.00003 * T * T) * Math.cos(2.0 * M) +
						(0.0770 - 0.0026 * T) * Math.sin(3.0 * M) +
						(0.2758 + 0.0002 * T - 0.00002 * T * T) * Math.cos(3.0 * M) +
						(-0.0128 - 0.0008 * T) * Math.sin(4.0 * M) +
						(0.0734 - 0.0004 * T - 0.00001 * T * T) * Math.cos(4.0 * M) +
						(-0.0122 - 0.0002 * T) * Math.sin(5.0 * M) +
						(0.0173 - 0.0002 * T) * Math.cos(5.0 * M);
			}
			break;
		case VENUS:
			if (inferior) {
				jde += (-0.0096 + 0.0002 * T - 0.00001 * T * T) +
						(2.0009 - 0.0033 * T - 0.00001 * T * T) * Math.sin(M) +
						(0.5980 - 0.0104 * T + 0.00001 * T * T) * Math.cos(M) +
						(0.0967 - 0.0018 * T - 0.00003 * T * T) * Math.sin(2.0 * M) +
						(0.0913 + 0.0009 * T - 0.00002 * T * T) * Math.cos(2.0 * M) +
						(0.0046 - 0.0002 * T) * Math.sin(3.0 * M) +
						(0.0079 + 0.0001 * T) * Math.cos(3.0 * M);
			} else {
				jde += (0.0099 - 0.0002 * T - 0.00001 * T * T) +
						(4.1991 - 0.0121 * T - 0.00003 * T * T) * Math.sin(M) +
						(-0.6095 + 0.0102 * T - 0.00002 * T * T) * Math.cos(M) +
						(0.2500 - 0.0028 * T - 0.00003 * T * T) * Math.sin(2.0 * M) +
						(0.0063 + 0.0025 * T - 0.00002 * T * T) * Math.cos(2.0 * M) +
						(0.0232 - 0.0005 * T - 0.00001 * T * T) * Math.sin(3.0 * M) +
						(0.0031 + 0.0004 * T) * Math.cos(3.0 * M);
			}
			break;
		case MARS:
			jde += (0.3102 - 0.0001 * T + 0.00001 * T * T) +
			(9.7273 - 0.0156 * T + 0.00001 * T * T) * Math.sin(M) +
			(-18.3195 - 0.0467 * T + 0.00009 * T * T) * Math.cos(M) +
			(-1.6488 - 0.0133 * T + 0.00001 * T * T) * Math.sin(2.0 * M) +
			(-2.6117 - 0.0020 * T + 0.00004 * T * T) * Math.cos(2.0 * M) +
			(-0.6827 - 0.0026 * T + 0.00001 * T * T) * Math.sin(3.0 * M) +
			(0.0281 + 0.0035 * T + 0.00001 * T * T) * Math.cos(3.0 * M) +
			(-0.0823 + 0.0006 * T + 0.00001 * T * T) * Math.sin(4.0 * M) +
			(0.1584 + 0.0013 * T) * Math.cos(4.0 * M) +
			(0.0270 + 0.0005 * T) * Math.sin(5.0 * M) +
			(0.0433) * Math.cos(5.0 * M);
			break;
		case JUPITER:
			jde += (0.1027 + 0.0002 * T - 0.00009 * T * T) +
			(-2.2637 + 0.0163 * T - 0.00003 * T * T) * Math.sin(M) +
			(-6.1540 - 0.0210 * T + 0.00008 * T * T) * Math.cos(M) +
			(-0.2021 - 0.0017 * T + 0.00001 * T * T) * Math.sin(2.0 * M) +
			(0.1310 - 0.0008 * T) * Math.cos(2.0 * M) +
			(0.0086) * Math.sin(3.0 * M) +
			(0.0087 + 0.0002 * T) * Math.cos(3.0 * M) +
			(0 + 0.0144 * T - 0.00008 * T * T) * Math.sin(a) +
			(0.3642 - 0.0019 * T - 0.00029 * T * T) * Math.cos(a);
			break;
		case SATURN:
			jde += (0.0172 - 0.0006 * T + 0.00023 * T * T) +
			(-8.5885 + 0.0411 * T + 0.00020 * T * T) * Math.sin(M) +
			(-1.1470 + 0.0352 * T - 0.00011 * T * T) * Math.cos(M) +
			(0.3331 - 0.0034 * T - 0.00001 * T * T) * Math.sin(2.0 * M) +
			(0.1145 - 0.0045 * T + 0.00002 * T * T) * Math.cos(2.0 * M) +
			(-0.0169 + 0.0002 * T) * Math.sin(3.0 * M) +
			(-0.0109 + 0.0004 * T) * Math.cos(3.0 * M) +
			(0 - 0.0337 * T + 0.00018 * T * T) * Math.sin(a) +
			(-0.8510 + 0.0044 * T + 0.00068 * T * T) * Math.cos(a) +
			(0 - 0.0064 * T + 0.00004 * T * T) * Math.sin(b) +
			(0.2397 - 0.0012 * T - 0.00008 * T * T) * Math.cos(b) +
			(0 - 0.0010 * T) * Math.sin(c) +
			(0.1245 + 0.0006 * T) * Math.cos(c) +
			(0 + 0.0024 * T - 0.00003 * T * T) * Math.sin(d) +
			(0.0477 - 0.0005 * T - 0.00006 * T * T) * Math.cos(d);
			break;
		case URANUS:
			jde += (-0.0859 + 0.0003 * T) +
			(-3.8179 - 0.0148 * T + 0.00003 * T * T) * Math.sin(M) +
			(5.1228 - 0.0105 * T - 0.00002 * T * T) * Math.cos(M) +
			(-0.0803 + 0.0011 * T) * Math.sin(2.0 * M) +
			(-0.1905 - 0.0006 * T) * Math.cos(2.0 * M) +
			(0.0088 + 0.0001 * T) * Math.sin(3.0 * M) +
			(0) * Math.cos(3.0 * M) +
			(0.8850) * Math.cos(e) +
			(0.2153) * Math.cos(f);
			break;
		case NEPTUNE:
			jde += (0.0168) +
			(-2.5606 + 0.0088 * T + 0.00002 * T * T) * Math.sin(M) +
			(-0.8611 - 0.0037 * T + 0.00002 * T * T) * Math.cos(M) +
			(0.0118 - 0.0004 * T + 0.00001 * T * T) * Math.sin(2.0 * M) +
			(0.0307 - 0.0003 * T) * Math.cos(2.0 * M) +
			(-0.5964) * Math.cos(e) +
			(0.0728) * Math.cos(g);
			break;
		default:
			throw new JPARSECException("invalid body.");
		}
		SimpleEventElement see = new SimpleEventElement(jde, SimpleEventElement.EVENT.PLANET_MINIMUM_ELONGATION, "");
		if (target == TARGET.MERCURY || target == TARGET.VENUS) {
			String type = "Inferior";
			if (!inferior) type = "Superior";
			see.details = type;
		}
		see.body = target.getName();
		return see;
	}

	/**
	 * Calculates the instant of a given planetary opposition or maximum elongation (Mercury and Venus),
	 * following Meeus's Astronomical Algorithms. Error is around a few hours.
	 * @param target The target ID constant. Valid objects are any planet (Mercury to Neptune,
	 * except Earth).
	 * @param jd The starting Julian day of calculations.
	 * @param eventType The event type (next, last, or closest to input date).  The use of the
	 * closest option is recommended when possible, since next/previous events could give incorrect
	 * events for a given date far from J2000.
	 * @return The event. In case the target is Mercury or Venus the details field
	 * will contains the elongation in degrees and the E for maximum elongation towards East, or 'W'
	 * for maximum elongation towards west from the Sun.
	 * @throws JPARSECException If an error occurs.
	 * @deprecated {@linkplain MainEvents#getPlanetaryEvent(Target.TARGET, double, 
	 * SimpleEventElement.EVENT, MainEvents.EVENT_TIME, boolean)}
	 * will give better results, although this method is indeed used in the other to obtain a fast
	 * first estimate.
	 */
	public static SimpleEventElement OppositionOrMaxElongation(TARGET target, double jd, EVENT_TIME eventType)
	throws JPARSECException {
		double A = 0.0, B = 0.0, M0 = 0.0, M1 = 0.0;
		switch (target) {
		case MERCURY:
			A = 2451612.023;
			B = 115.8774771;
			M0 = 63.5867;
			M1 = 114.2088742;
			break;
		case VENUS:
			A = 2451996.706;
			B = 583.921361;
			M0 = 82.7311;
			M1 = 215.513058;
			break;
		case MARS:
			A = 2452097.382;
			B = 779.936104;
			M0 = 181.9573;
			M1 = 48.705244;
			break;
		case JUPITER:
			A = 2451870.628;
			B = 398.884046;
			M0 = 318.4681;
			M1 = 33.140229;
			break;
		case SATURN:
			A = 2451870.170;
			B = 378.091904;
			M0 = 318.0172;
			M1 = 12.647487;
			break;
		case URANUS:
			A = 2451764.317;
			B = 369.656035;
			M0 = 213.6884;
			M1 = 4.333093;
			break;
		case NEPTUNE:
			A = 2451753.122;
			B = 367.486703;
			M0 = 202.6544;
			M1 = 2.194998;
			break;
		default:
			throw new JPARSECException("invalid body.");
		}
		AstroDate astro = new AstroDate(jd);
		double year = astro.getAstronomicalYear() + (astro.getMonth() - 1.0 + astro.getDayPlusFraction() / (1.0 + astro.getDaysInMonth())) / 12.0;

		double kapprox = (year * 365.2425 + 1721060.0 - A) / B;
		double k = Math.round(kapprox);
		if (eventType == EVENT_TIME.PREVIOUS && k > kapprox) k --;
		if (eventType == EVENT_TIME.NEXT && k < kapprox) k ++;
		double jde = A + k * B;
		// Fix k if required
		if (jde > jd && eventType == EVENT_TIME.PREVIOUS) k --;
		if (jde < jd && eventType == EVENT_TIME.NEXT) k ++;
		if (jde > jd && eventType != EVENT_TIME.PREVIOUS) {
			double km = k - 1;
			double newjde = A + km * B;
			if ((newjde > jd && eventType == EVENT_TIME.NEXT) ||
					(Math.abs(jd-newjde) < Math.abs(jd-jde) && eventType == EVENT_TIME.CLOSEST)) k = km;
		}
		if (jde < jd && eventType != EVENT_TIME.NEXT) {
			double kp = k + 1;
			double newjde = A + kp * B;
			if ((newjde < jd && eventType == EVENT_TIME.PREVIOUS) ||
					(Math.abs(jd-newjde) < Math.abs(jd-jde) && eventType == EVENT_TIME.CLOSEST)) k = kp;
		}
		jde = A + k * B;


		double M = (M0 + k * M1) * Constant.DEG_TO_RAD;
		double T = Functions.toCenturies(jde);

		double a = 82.74 + 40.76 * T, b = 29.86 + 1181.36 * T, c = 14.13 + 590.68 * T,
				d = 220.02 + 1262.87 * T, e = 207.83 + 8.51 * T, f = 108.84 + 419.96 * T,
				g = 276.74 + 209.98 * T;
		a *= Constant.DEG_TO_RAD;
		b *= Constant.DEG_TO_RAD;
		c *= Constant.DEG_TO_RAD;
		d *= Constant.DEG_TO_RAD;
		e *= Constant.DEG_TO_RAD;
		f *= Constant.DEG_TO_RAD;
		g *= Constant.DEG_TO_RAD;

		String elong = "";
		switch (target) {
		case MERCURY:
			double jdeEast = (-21.6101 + 0.0002 * T) +
					(-1.9803 - 0.0060 * T + 0.00001 * T * T) * Math.sin(M) +
					(1.4151 - 0.0072 * T - 0.00001 * T * T) * Math.cos(M) +
					(0.5528 - 0.0005 * T - 0.00001 * T * T) * Math.sin(2.0 * M) +
					(0.2905 + 0.0034 * T + 0.00001 * T * T) * Math.cos(2.0 * M) +
					(-0.1121 - 0.0001 * T + 0.00001 *T * T) * Math.sin(3.0 * M) +
					(-0.0098 - 0.0015 * T) * Math.cos(3.0 * M) +
					(0.0192) * Math.sin(4.0 * M) +
					(0.0111 + 0.0004 * T) * Math.cos(4.0 * M) +
					(-0.0061) * Math.sin(5.0 * M) +
					(-0.0032 - 0.0001 * T) * Math.cos(5.0 * M);
			double elongEast = (22.4697) +
					(-4.2666 + 0.0054 * T + 0.00002 * T * T) * Math.sin(M) +
					(-1.8537 - 0.0137 * T) * Math.cos(M) +
					(0.3598 + 0.0008 * T - 0.00001 * T * T) * Math.sin(2.0 * M) +
					(-0.0680 + 0.0026 * T) * Math.cos(2.0 * M) +
					(-0.0524 - 0.0003 * T) * Math.sin(3.0 * M) +
					(0.0052 - 0.0006 * T) * Math.cos(3.0 * M) +
					(0.0107 + 0.0001 * T * T) * Math.sin(4.0 * M) +
					(-0.0013 + 0.0001 * T) * Math.cos(4.0 * M) +
					(-0.0021) * Math.sin(5.0 * M) +
					(-0.0003) * Math.cos(5.0 * M);
			elong = ""+(float) elongEast+" E";

			double jdeWest = (21.6249 - 0.0002 * T) +
					(0.1306 + 0.0065 * T) * Math.sin(M) +
					(-2.7661 - 0.0011 * T + 0.00001 * T * T) * Math.cos(M) +
					(0.2438 - 0.0024 * T - 0.00001 * T * T) * Math.sin(2.0 * M) +
					(0.5767 + 0.0023 * T) * Math.cos(2.0 * M) +
					(0.1041) * Math.sin(3.0 * M) +
					(-0.0184 + 0.0007 * T) * Math.cos(3.0 * M) +
					(-0.0051 - 0.0001 * T) * Math.sin(4.0 * M) +
					(0.0048 + 0.0001 * T) * Math.cos(4.0 * M) +
					(0.0026) * Math.sin(5.0 * M) +
					(0.0037) * Math.cos(5.0 * M);
			double elongWest = (22.4143 - 0.0001 * T) +
					(4.3651 - 0.0048 * T - 0.00002 * T * T) * Math.sin(M) +
					(2.3787 + 0.0121 * T - 0.00001 * T * T) * Math.cos(M) +
					(0.2674 + 0.0022 * T) * Math.sin(2.0 * M) +
					(-0.3873 + 0.0008 * T + 0.00001 * T * T) * Math.cos(2.0 * M) +
					(-0.0369 - 0.0001 * T) * Math.sin(3.0 * M) +
					(0.0017 - 0.0001 * T) * Math.cos(3.0 * M) +
					(0.0059) * Math.sin(4.0 * M) +
					(0.0061 + 0.0001 * T * T) * Math.cos(4.0 * M) +
					(0.0007) * Math.sin(5.0 * M) +
					(-0.0011) * Math.cos(5.0 * M);
			if (Math.abs(jde + jdeEast - jd) < Math.abs(jde + jdeWest - jd)) {
				jde += jdeEast;
			} else {
				jde += jdeWest;
				elong = ""+(float) elongWest+" W";
			}
			break;
		case VENUS:
			jdeEast = (-70.7600 + 0.0002 * T - 0.00001 * T * T) +
					(1.0282 - 0.0010 * T - 0.00001 * T * T) * Math.sin(M) +
					(0.2761 - 0.0060 * T) * Math.cos(M) +
					(-0.0438 - 0.0023 * T + 0.00002 * T * T) * Math.sin(2.0 * M) +
					(0.1660 - 0.0037 * T - 0.00004 * T * T) * Math.cos(2.0 * M) +
					(0.0036 + 0.0001 * T) * Math.sin(3.0 * M) +
					(-0.0011 + 0.00001 * T * T) * Math.cos(3.0 * M);
			elongEast = (46.3173 + 0.0001 * T) +
					(0.6916 - 0.0024 * T) * Math.sin(M) +
					(0.6676 - 0.0045 * T) * Math.cos(M) +
					(0.0309 - 0.0002 * T) * Math.sin(2.0 * M) +
					(0.0036 - 0.0001 * T) * Math.cos(2.0 * M) +
					(0) * Math.sin(3.0 * M) +
					(0) * Math.cos(3.0 * M);

			jdeWest = (70.7462 - 0.00001 * T * T) +
					(1.1218 - 0.0025 * T - 0.00001 * T * T) * Math.sin(M) +
					(0.4538 - 0.0056 * T) * Math.cos(M) +
					(0.1320 + 0.0020 * T - 0.00003 * T * T) * Math.sin(2.0 * M) +
					(-0.0702 + 0.0022 * T + 0.00004 * T * T) * Math.cos(2.0 * M) +
					(0.0062 - 0.0001 * T) * Math.sin(3.0 * M) +
					(0.0015 - 0.00001 * T * T) * Math.cos(3.0 * M);
			elongWest = (46.3245) +
					(-0.5366 - 0.0003 * T + 0.00001 * T * T) * Math.sin(M) +
					(0.3097 + 0.0016 * T - 0.00001 * T * T) * Math.cos(M) +
					(-0.0163) * Math.sin(2.0 * M) +
					(-0.0075 + 0.0001 * T) * Math.cos(2.0 * M) +
					(0) * Math.sin(3.0 * M) +
					(0) * Math.cos(3.0 * M);
			if (Math.abs(jde + jdeEast - jd) < Math.abs(jde + jdeWest - jd)) {
				jde += jdeEast;
			} else {
				jde += jdeWest;
				elong = ""+(float) elongWest+" W";
			}
			break;
		case MARS:
			jde += (-0.3088 + 0.00002 * T * T) +
			(-17.6965 + 0.0363 * T + 0.00005 * T * T) * Math.sin(M) +
			(18.3131 + 0.0467 * T - 0.00006 * T * T) * Math.cos(M) +
			(-0.2162 - 0.0198 * T + 0.00001 * T * T) * Math.sin(2.0 * M) +
			(-4.5028 - 0.0019 * T + 0.00007 * T * T) * Math.cos(2.0 * M) +
			(0.8987 + 0.0058 * T - 0.00002 * T * T) * Math.sin(3.0 * M) +
			(0.7666 - 0.0050 * T - 0.00003 * T * T) * Math.cos(3.0 * M) +
			(-0.3636 - 0.0001 * T + 0.00002 * T * T) * Math.sin(4.0 * M) +
			(0.0402 + 0.0032 * T) * Math.cos(4.0 * M) +
			(0.0737 - 0.0008 * T) * Math.sin(5.0 * M) +
			(-0.0980 - 0.0011 * T) * Math.cos(5.0 * M);
			break;
		case JUPITER:
			jde += (-0.1029 - 0.00009 * T * T) +
			(-1.9658 - 0.0056 * T + 0.00007 * T * T) * Math.sin(M) +
			(6.1537 + 0.0210 * T - 0.00006 * T * T) * Math.cos(M) +
			(-0.2081 - 0.0013 * T) * Math.sin(2.0 * M) +
			(-0.1116 - 0.0010 * T) * Math.cos(2.0 * M) +
			(0.0074 + 0.0001 * T) * Math.sin(3.0 * M) +
			(-0.0097 - 0.0001 * T) * Math.cos(3.0 * M) +
			(0 + 0.0144 * T - 0.00008 * T * T) * Math.sin(a) +
			(0.3642 - 0.0019 * T - 0.00029 * T * T) * Math.cos(a);
			break;
		case SATURN:
			jde += (-0.0209 + 0.0006 * T + 0.00023 * T * T) +
			(4.5795 - 0.0312 * T - 0.00017 * T * T) * Math.sin(M) +
			(1.1462 - 0.0351 * T + 0.00011 * T * T) * Math.cos(M) +
			(0.0985 - 0.0015 * T) * Math.sin(2.0 * M) +
			(0.0733 - 0.0031 * T + 0.00001 * T * T) * Math.cos(2.0 * M) +
			(0.0025 - 0.0001 * T) * Math.sin(3.0 * M) +
			(0.0050 - 0.0002 * T) * Math.cos(3.0 * M) +
			(0 - 0.0337 * T + 0.00018 * T * T) * Math.sin(a) +
			(-0.8510 + 0.0044 * T + 0.00068 * T * T) * Math.cos(a) +
			(0 - 0.0064 * T + 0.00004 * T * T) * Math.sin(b) +
			(0.2397 - 0.0012 * T - 0.00008 * T * T) * Math.cos(b) +
			(0 - 0.0010 * T) * Math.sin(c) +
			(0.1245 + 0.0006 * T) * Math.cos(c) +
			(0 + 0.0024 * T - 0.00003 * T * T) * Math.sin(d) +
			(0.0477 - 0.0005 * T - 0.00006 * T * T) * Math.cos(d);
			break;
		case URANUS:
			jde += (0.0844 - 0.0006 * T) +
			(-0.1048 + 0.0246 * T) * Math.sin(M) +
			(-5.1221 + 0.0104 * T + 0.00003 * T * T) * Math.cos(M) +
			(-0.1428 + 0.0005 * T) * Math.sin(2.0 * M) +
			(-0.0148 - 0.0013 * T) * Math.cos(2.0 * M) +
			(0) * Math.sin(3.0 * M) +
			(0.0055) * Math.cos(3.0 * M) +
			(0.8850) * Math.cos(e) +
			(0.2153) * Math.cos(f);
			break;
		case NEPTUNE:
			jde += (-0.0140 + 0.00001 * T * T) +
			(-1.3486 + 0.0010 * T + 0.00001 * T * T) * Math.sin(M) +
			(0.8597 + 0.0037 * T) * Math.cos(M) +
			(-0.0082 - 0.0002 * T + 0.00001 * T * T) * Math.sin(2.0 * M) +
			(0.0037 - 0.0003 * T) * Math.cos(2.0 * M) +
			(-0.5964) * Math.cos(e) +
			(0.0728) * Math.cos(g);
			break;
		default:
			throw new JPARSECException("invalid body.");
		}
		SimpleEventElement see = new SimpleEventElement(jde, SimpleEventElement.EVENT.PLANET_MINIMUM_ELONGATION, elong);
		see.body = target.getName();
		return see;
	}

	/**
	 * Calculates the instant of a given planetary event. Precision is of a few minutes for
	 * inner planets.
	 * @param target The target ID constant. Valid objects are the planets, Pluto, and the Moon.
	 * @param jd The starting Julian day of calculations.
	 * @param event The event, constants defined in {@linkplain SimpleEventElement} (PLANET_...).
	 * @param eventType The event type (next, last, or closest to input date). The use of the
	 * closest option is recommended when possible, since next/previous events could give incorrect
	 * events for a given date far from J2000.
	 * @param maximumAccuracy True for maximum accuracy (few minutes of error at most), false for
	 * approximate but faster result.
	 * @return The event data. The maximum/minimum distance or elongation (in AU and degrees) will be given
	 * as a number in the details of the event.
	 * @throws JPARSECException If an error occurs.
	 */
	public static SimpleEventElement getPlanetaryEvent(TARGET target, double jd, EVENT event, EVENT_TIME eventType,
			boolean maximumAccuracy) throws JPARSECException {
		return getPlanetaryEvent(target, jd, event, eventType, maximumAccuracy, false);
	}
	
	/**
	 * Calculates the instant of a given planetary event. Precision is of a few minutes for
	 * inner planets.
	 * @param target The target ID constant. Valid objects are the planets, Pluto, and the Moon.
	 * @param jd The starting Julian day of calculations.
	 * @param event The event, constants defined in {@linkplain SimpleEventElement} (PLANET_...).
	 * @param eventType The event type (next, last, or closest to input date). The use of the
	 * closest option is recommended when possible, since next/previous events could give incorrect
	 * events for a given date far from J2000.
	 * @param maximumAccuracy True for maximum accuracy (few minutes of error at most), false for
	 * approximate but faster result.
	 * @param allowJPL True to allow trying the latest JPL integration available to calculate the event.
	 * @return The event data. The maximum/minimum distance or elongation (in AU and degrees) will be given
	 * as a number in the details of the event.
	 * @throws JPARSECException If an error occurs.
	 */
	public static SimpleEventElement getPlanetaryEvent(TARGET target, double jd, EVENT event, EVENT_TIME eventType,
			boolean maximumAccuracy, boolean allowJPL) throws JPARSECException {
		if (!maximumAccuracy && (target == TARGET.EARTH || target == TARGET.Earth_Moon_Barycenter))
			return MainEvents.PerihelionAndAphelion(target, jd, event, eventType);
		if (target == TARGET.Moon) {
			if (event == EVENT.PLANET_MAXIMUM_DISTANCE) return LunarEvent.getApogee(jd, eventType);
			if (event == EVENT.PLANET_MINIMUM_DISTANCE) return LunarEvent.getPerigee(jd, eventType);
			if (event == EVENT.PLANET_MAXIMUM_ELONGATION) return MainEvents.MoonPhaseOrEclipse(jd, EVENT.MOON_FULL, eventType);
			if (event == EVENT.PLANET_MINIMUM_ELONGATION) return MainEvents.MoonPhaseOrEclipse(jd, EVENT.MOON_NEW, eventType);
		}

		TimeElement time = new TimeElement(jd, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		CityElement city = City.findCity("Madrid");
		ObserverElement obs = ObserverElement.parseCity(city);
		EphemerisElement eph = new EphemerisElement(target, EphemerisElement.COORDINATES_TYPE.APPARENT,
				EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.GEOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
				EphemerisElement.FRAME.ICRF, EphemerisElement.ALGORITHM.MOSHIER);
		eph.optimizeForSpeed();
		
		if (maximumAccuracy && allowJPL) { // Try to use JPL integration for maximum accuracy
			try {
				JPLEphemeris jpl = new JPLEphemeris(JPLEphemeris.LATEST); // Used to check if it is available
				if (jpl != null) {
					eph.algorithm = JPLEphemeris.LATEST;
					eph.preferPrecisionInEphemerides = true;
				}
			} catch (Exception exc) {}			
		}

		EphemElement ephem = null;
		boolean inferior = true;
		boolean east = true;

		// This block of code is to accelerate calculations, using approximate algorithms by Meeus
		if (event == SimpleEventElement.EVENT.PLANET_MAXIMUM_DISTANCE_FROM_SUN || event == SimpleEventElement.EVENT.PLANET_MINIMUM_DISTANCE_FROM_SUN) {
			SimpleEventElement newEvent = MainEvents.PerihelionAndAphelion(target, jd, event, eventType);
			if (target == TARGET.EARTH) {
				target = TARGET.SUN;
				eph.targetBody = target;
				if (event == EVENT.PLANET_MAXIMUM_DISTANCE_FROM_SUN) event = EVENT.PLANET_MAXIMUM_DISTANCE;
				if (event == EVENT.PLANET_MINIMUM_DISTANCE_FROM_SUN) event = EVENT.PLANET_MINIMUM_DISTANCE;
			}
			jd = newEvent.time;
			time = new TimeElement(jd, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
			ephem = Ephem.getEphemeris(time, obs, eph, false);
		}
		if (event == SimpleEventElement.EVENT.PLANET_MINIMUM_ELONGATION) {
			SimpleEventElement newEvent = MainEvents.Conjunction(target, jd, eventType);
			if (newEvent.details.toLowerCase().indexOf("superior") >= 0)
				inferior = false;
			jd = newEvent.time;
			time = new TimeElement(jd, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
			ephem = Ephem.getEphemeris(time, obs, eph, false);
		}
		if (event == SimpleEventElement.EVENT.PLANET_MAXIMUM_ELONGATION) {
			SimpleEventElement newEvent = MainEvents.OppositionOrMaxElongation(target, jd, eventType);
			jd = newEvent.time;
			time = new TimeElement(jd, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
			ephem = Ephem.getEphemeris(time, obs, eph, false);
			east = true;
			if (newEvent.details.indexOf("W") >= 0) east = false;
		}
		if (event == SimpleEventElement.EVENT.PLANET_MINIMUM_DISTANCE && target != TARGET.SUN) {
			if (target.compareTo(TARGET.VENUS) > 0) {
				SimpleEventElement newEvent = MainEvents.OppositionOrMaxElongation(target, jd, eventType);
				jd = newEvent.time;
				time = new TimeElement(jd, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
				ephem = Ephem.getEphemeris(time, obs, eph, false);
			} else {
				SimpleEventElement newEvent = MainEvents.Conjunction(target, jd, eventType);
				if (newEvent.details.toLowerCase().indexOf("superior") >= 0) {
					EVENT_TIME newTime = EVENT_TIME.NEXT;
					if (eventType == EVENT_TIME.PREVIOUS) newTime = EVENT_TIME.PREVIOUS;
					if (eventType == EVENT_TIME.CLOSEST) {
						newTime = EVENT_TIME.NEXT;
						if (newEvent.time < jd) newTime = EVENT_TIME.PREVIOUS;
					}
					newEvent.time += 10 * (newTime == EVENT_TIME.NEXT ? 1.0 : -1.0);
					newEvent = MainEvents.Conjunction(target, newEvent.time, newTime);
				}
				jd = newEvent.time;
				time = new TimeElement(jd, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
				ephem = Ephem.getEphemeris(time, obs, eph, false);
			}
		}
		if (event == SimpleEventElement.EVENT.PLANET_MAXIMUM_DISTANCE && target != TARGET.SUN) {
			if (target.compareTo(TARGET.VENUS) > 0) {
				SimpleEventElement newEvent = MainEvents.Conjunction(target, jd, eventType);
				jd = newEvent.time;
				time = new TimeElement(jd, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
				ephem = Ephem.getEphemeris(time, obs, eph, false);
			} else {
				SimpleEventElement newEvent = MainEvents.Conjunction(target, jd, eventType);
				if (newEvent.details.toLowerCase().indexOf("inferior") >= 0) {
					EVENT_TIME newTime = EVENT_TIME.NEXT;
					if (eventType == EVENT_TIME.PREVIOUS) newTime = EVENT_TIME.PREVIOUS;
					if (eventType == EVENT_TIME.CLOSEST) {
						newTime = EVENT_TIME.NEXT;
						if (newEvent.time < jd) newTime = EVENT_TIME.PREVIOUS;
					}
					newEvent.time += 10 * (newTime == EVENT_TIME.NEXT ? 1.0 : -1.0);
					newEvent = MainEvents.Conjunction(target, newEvent.time, newTime);
				}
				jd = newEvent.time;
				time = new TimeElement(jd, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
				ephem = Ephem.getEphemeris(time, obs, eph, false);
			}
		}

		if (ephem == null) ephem = Ephem.getEphemeris(time, obs, eph, false);

		if (maximumAccuracy || event == EVENT.PLANET_DICHOTOMY_OR_QUADRATURE) {
			TimeElement time1 = new TimeElement(jd+1.0/(24.0*60.0), SCALE.BARYCENTRIC_DYNAMICAL_TIME);
			EphemElement ephem1 = Ephem.getEphemeris(time1, obs, eph, false);
			double d = 0.0;
			switch (event) {
			case PLANET_MAXIMUM_DISTANCE:
				d = ephem1.distance - ephem.distance;
				eventType = MainEvents.EVENT_TIME.PREVIOUS;
				if (d > 0.0) eventType = MainEvents.EVENT_TIME.NEXT;
				break;
			case PLANET_MAXIMUM_DISTANCE_FROM_SUN:
				d = ephem1.distanceFromSun - ephem.distanceFromSun;
				eventType = MainEvents.EVENT_TIME.PREVIOUS;
				if (d > 0.0) eventType = MainEvents.EVENT_TIME.NEXT;
				break;
			case PLANET_MAXIMUM_ELONGATION:
				d = ephem1.elongation - ephem.elongation;
				eventType = MainEvents.EVENT_TIME.PREVIOUS;
				if (d > 0.0) eventType = MainEvents.EVENT_TIME.NEXT;
				break;
			case PLANET_MINIMUM_DISTANCE:
				d = ephem1.distance - ephem.distance;
				eventType = MainEvents.EVENT_TIME.PREVIOUS;
				if (d < 0.0) eventType = MainEvents.EVENT_TIME.NEXT;
				break;
			case PLANET_MINIMUM_DISTANCE_FROM_SUN:
				d = ephem1.distanceFromSun - ephem.distanceFromSun;
				eventType = MainEvents.EVENT_TIME.PREVIOUS;
				if (d < 0.0) eventType = MainEvents.EVENT_TIME.NEXT;
				break;
			case PLANET_MINIMUM_ELONGATION:
				d = ephem1.elongation - ephem.elongation;
				eventType = MainEvents.EVENT_TIME.PREVIOUS;
				if (d < 0.0) eventType = MainEvents.EVENT_TIME.NEXT;
				break;
			case PLANET_DICHOTOMY_OR_QUADRATURE:
				if (target == TARGET.MERCURY || target == TARGET.VENUS) {
					d = ephem1.phase - ephem.phase;
					eventType = MainEvents.EVENT_TIME.PREVIOUS;
					if ((d > 0.0 && ephem1.phase < 0.5) || (d < 0.0 && ephem1.phase > 0.5)) 
						eventType = MainEvents.EVENT_TIME.NEXT;
				} else {
					if (target.ordinal() >= TARGET.MARS.ordinal() && 
							target.ordinal() <= TARGET.Pluto.ordinal()) {
						d = ephem1.elongation - ephem.elongation;
						eventType = MainEvents.EVENT_TIME.PREVIOUS;
						if ((d > 0.0 && ephem1.elongation < Constant.PI_OVER_TWO) || (d < 0.0 && ephem1.elongation > Constant.PI_OVER_TWO)) 
							eventType = MainEvents.EVENT_TIME.NEXT;
					}
				}
				break;
			default:
				throw new JPARSECException("invalid event.");
			}

			double step = (target.ordinal() - TARGET.SUN.ordinal()) * 5.0;
			if (target.compareTo(TARGET.SATURN) <= 0) step = 0.5 / 24.0;
			if (target == TARGET.Moon) step = 0.5;
			if (eventType == MainEvents.EVENT_TIME.PREVIOUS) step = -step;
			double precission = 1.0 / (24.0 * 60.0); // 1 minute
			d = 0.0;
			int dd = 1;

			do {
				do {
					jd = jd + step;
					time1 = new TimeElement(jd, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
					ephem1 = Ephem.getEphemeris(time1, obs, eph, false);
					switch (event) {
					case PLANET_MAXIMUM_DISTANCE:
						d = ephem1.distance - ephem.distance;
						break;
					case PLANET_MAXIMUM_DISTANCE_FROM_SUN:
						d = ephem1.distanceFromSun - ephem.distanceFromSun;
						break;
					case PLANET_MAXIMUM_ELONGATION:
						d = ephem1.elongation - ephem.elongation;
						break;
					case PLANET_MINIMUM_DISTANCE:
						d = ephem1.distance - ephem.distance;
						dd = -1;
						break;
					case PLANET_MINIMUM_DISTANCE_FROM_SUN:
						d = ephem1.distanceFromSun - ephem.distanceFromSun;
						dd = -1;
						break;
					case PLANET_MINIMUM_ELONGATION:
						d = ephem1.elongation - ephem.elongation;
						dd = -1;
						break;
					case PLANET_DICHOTOMY_OR_QUADRATURE:
						double d2 = 0;
						if (target == TARGET.MERCURY || target == TARGET.VENUS) {
							d2 = ephem1.phase - 0.5;
						} else {
							if (target.ordinal() >= TARGET.MARS.ordinal() && 
									target.ordinal() <= TARGET.Pluto.ordinal()) {
								d2 = ephem1.elongation - Constant.PI_OVER_TWO;
							}
						}
						if (d == 0) dd = FastMath.sign(d2);
						d = d2;
						break;
					default:
						throw new JPARSECException("invalid event.");
					}
					ephem = ephem1.clone();
				} while (FastMath.sign(d) == FastMath.sign(dd));
				if (eventType == MainEvents.EVENT_TIME.PREVIOUS) {
					eventType = MainEvents.EVENT_TIME.NEXT;
				} else {
					eventType = MainEvents.EVENT_TIME.PREVIOUS;
				}
				step = -step / 2.0;
			} while (Math.abs(step) > precission);
		}

		String details = "";
		switch (event) {
		case PLANET_MAXIMUM_DISTANCE:
		case PLANET_MINIMUM_DISTANCE:
			details = Functions.formatValue(ephem.distance, 3);
			break;
		case PLANET_MAXIMUM_DISTANCE_FROM_SUN:
		case PLANET_MINIMUM_DISTANCE_FROM_SUN:
			details = Functions.formatValue(ephem.distanceFromSun, 3);
			break;
		case PLANET_MAXIMUM_ELONGATION:
		case PLANET_MINIMUM_ELONGATION:
			details = Functions.formatValue(ephem.elongation * Constant.RAD_TO_DEG, 3);
			if (event == SimpleEventElement.EVENT.PLANET_MINIMUM_ELONGATION &&
					(target == TARGET.MERCURY || target == TARGET.VENUS)) {
				String type = "Inferior";
				if (!inferior) type = "Superior";
				details += ", "+type;
			}
			if (event == SimpleEventElement.EVENT.PLANET_MAXIMUM_ELONGATION &&
					(target == TARGET.MERCURY || target == TARGET.VENUS)) {
				String type = "E";
				if (!east) type = "W";
				details += ", "+type;
			}
			break;
		case PLANET_DICHOTOMY_OR_QUADRATURE:
			TimeElement tend = new TimeElement(jd, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
			ephem = Ephem.getEphemeris(tend, obs, eph, false);
			String type = "E";
			if (ephem.phaseAngle < 0) type = "W";				
			if (target == TARGET.MERCURY || target == TARGET.VENUS) {
				type = "W";
				if (ephem.phaseAngle < 0) type = "E";
			}
			details = type;
			break;
		default:
			throw new JPARSECException("invalid event.");
		}
		SimpleEventElement see = new SimpleEventElement(jd, event, details);
		see.body = target.getName();
		return see;
	}

	/**
	 * Calculates the instant of a given Mercury or Venus transit. Precision in the time
	 * of the maximum is of a few minutes.
	 * @param target The target ID constant. Valid objects are Mercury and Venus.
	 * @param jd The starting Julian day of calculations.
	 * @param jd_limit Limiting Julian day. If it is crossed a null object will be returned.
	 * @param maximumAccuracy True for maximum accuracy (few minutes of error at most), false for
	 * approximate but faster result.
	 * @return The event, or null if it is not found. The minimum elongation (degrees) will be given
	 * as a number in the details of the event.
	 * @throws JPARSECException If an error occurs.
	 */
	public static SimpleEventElement getMercuryOrVenusTransit(TARGET target, double jd, double jd_limit,
			boolean maximumAccuracy) throws JPARSECException
	{
		if (target != TARGET.MERCURY && target != TARGET.VENUS) throw new JPARSECException("invalid object.");
		if (jd_limit < jd) return null;

		CityElement city = City.findCity("Madrid");
		ObserverElement obs = ObserverElement.parseCity(city);
		EphemerisElement eph = new EphemerisElement(TARGET.SUN, EphemerisElement.COORDINATES_TYPE.APPARENT,
				EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.GEOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
				EphemerisElement.FRAME.ICRF, EphemerisElement.ALGORITHM.MOSHIER);
		EphemerisElement eph2 = new EphemerisElement(target, EphemerisElement.COORDINATES_TYPE.APPARENT,
				EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.GEOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
				EphemerisElement.FRAME.ICRF, EphemerisElement.ALGORITHM.MOSHIER);
		eph.optimizeForSpeed();
		eph2.optimizeForSpeed();

		SimpleEventElement.EVENT event = SimpleEventElement.EVENT.PLANET_MINIMUM_ELONGATION;
		double minElong = 0, sunRadius = 0.25, dist = 1.5, sunDist = 1;
		SimpleEventElement see;
		int step = 30;
		if (target == TARGET.VENUS) step = 200;
		EVENT_TIME eventType = EVENT_TIME.NEXT;
		double delta = 1;
		if (jd_limit < jd) {
			eventType = EVENT_TIME.PREVIOUS;
			delta = -1;
		}
		do {
			see = MainEvents.getPlanetaryEvent(target, jd, event, eventType, maximumAccuracy);
			jd = see.time + step * FastMath.sign(delta);

			minElong = Double.parseDouble(FileIO.getField(1, see.details, ",", true));
			if (minElong < 0.3) {
				TimeElement time = new TimeElement(see.time, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
				EphemElement ephem = Ephem.getEphemeris(time, obs, eph, false);
				EphemElement ephem2 = Ephem.getEphemeris(time, obs, eph2, false);
				sunRadius = ephem.angularRadius * Constant.RAD_TO_DEG;
				dist = ephem2.distance;
				sunDist = ephem.distance;
			}

			if (see.details.toLowerCase().indexOf("inferior") >= 0)
				jd += step * FastMath.sign(delta);
		} while ((minElong > sunRadius || dist > sunDist) && ((jd < jd_limit && eventType == MainEvents.EVENT_TIME.NEXT) || (jd > jd_limit && eventType == MainEvents.EVENT_TIME.PREVIOUS)));
		if (minElong > sunRadius || dist > sunDist) see = null;
		if (see != null && see.time > jd_limit) see = null;
		if (see != null) {
			see.body = target.getName();
			see.eventType = SimpleEventElement.EVENT.MERCURY_TRANSIT;
			if (target == TARGET.VENUS) see.eventType = SimpleEventElement.EVENT.VENUS_TRANSIT;
		}
		return see;
	}

	/**
	 * Returns the longitude of the Great Red Spot (GRS), using historical observations.
	 * Data available only since 1969. For previous dates and dates after last update a
	 * linear extrapolation is used. Data is regularly updated from Sky &amp; Telescope and ALPO.
	 * @param JD_TDB Julian day.
	 * @return GRS longitude (system II), in radians.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double getJupiterGRSLongitude(double JD_TDB)
	throws JPARSECException {
		String jarpath = FileIO.DATA_ORBITAL_ELEMENTS_DIRECTORY + "JupiterGRS.txt";
		ArrayList<String> file = ReadFile.readResource(jarpath);

		double jd[] = new double[file.size()];
		double lon[] = new double[file.size()];
		for (int i=0; i<file.size(); i++)
		{
			String li = file.get(i);
			String date = FileIO.getField(1, li, ",", true);
			int year = Integer.parseInt(FileIO.getField(1, date, "-", true));
			int month = Integer.parseInt(FileIO.getField(2, date, "-", true));
			int day = Integer.parseInt(FileIO.getField(3, date, "-", true));
			jd[i] = new AstroDate(year, month, day).jd();
			lon[i] = Double.parseDouble(FileIO.getField(2, li, ",", true));
		}

		Interpolation i = new Interpolation(jd, lon, true);
		double lonGRS = i.linearInterpolation(JD_TDB);
		return lonGRS * Constant.DEG_TO_RAD;
	}

	/**
	 * Returns the instant of the next transit of the Great Red Spot.
	 * Calculations are performed by default for the geocenter, using
	 * Moshier algorithms and IAU 2006 resolutions. IAU 2009 is not
	 * used here since it introduces differences of several degrees
	 * in the longitude of the central meridian.
	 * @param JD_TDB Input dynamical time as a Julian day.
	 * @return Output dynamical time of the next transit, with a theoretical
	 * precision better than one second (obviously unrealistic).
	 * @throws JPARSECException If an error occurs.
	 */
	public static SimpleEventElement getJupiterGRSNextTransitTime(double JD_TDB)
	throws JPARSECException {
		CityElement city = City.findCity("Madrid");
		ObserverElement observer = ObserverElement.parseCity(city);
		EphemerisElement eph = new EphemerisElement(TARGET.JUPITER, EphemerisElement.COORDINATES_TYPE.APPARENT,
				EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.GEOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
				EphemerisElement.FRAME.ICRF);
		eph.algorithm = EphemerisElement.ALGORITHM.MOSHIER;
		eph.optimizeForSpeed();

		double prec = 1.0 / Constant.SECONDS_PER_DAY;
		double jd;
		for (jd = JD_TDB; jd < JD_TDB + 1.0; jd = jd + prec)
		{
			AstroDate astro = new AstroDate(jd);
			TimeElement time = new TimeElement(astro, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
			EphemElement ephem = Ephem.getEphemeris(time, observer, eph, false);
			double GRS_lon = getJupiterGRSLongitude(JD_TDB);

			double dif = GRS_lon - ephem.longitudeOfCentralMeridianSystemII;
			dif = Functions.normalizeRadians(dif);
			if (dif > Math.PI) dif = dif - Constant.TWO_PI;
			double dt = dif * 9.9 / (24.0 * Constant.TWO_PI);
			jd = jd + dt * 0.25; // * 0.5;
			if (dt < prec) {
				jd = jd + dt * 0.75; // * 0.5;
				break;
			}
		}
		SimpleEventElement see = new SimpleEventElement(jd, SimpleEventElement.EVENT.JUPITER_GRS_TRANSIT, "");
		see.body = TARGET.JUPITER.getName();
		return see;
	}
}
