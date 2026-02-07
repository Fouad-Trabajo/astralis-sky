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
package jparsec.ephem.planets;

import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.EphemerisElement.FRAME;
import jparsec.ephem.Functions;
import jparsec.ephem.IAU2006;
import jparsec.ephem.Nutation;
import jparsec.ephem.PhysicalParameters;
import jparsec.ephem.Precession;
import jparsec.ephem.Target.TARGET;
import jparsec.math.Constant;
import jparsec.math.matrix.Matrix;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.time.TimeScale;
import jparsec.util.DataBase;
import jparsec.util.JPARSECException;

/**
 * Applies Newcomb's solar theory, published in 1898.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Newcomb
{
	// private constructor so that this class cannot be instantiated.
	private Newcomb() {}

	/**
	 * Calculates geocentric ecliptic coordinates (mean equinox and ecliptic of
	 * the date) of the sun applying Newcomb's solar theory, published in 1898.
	 * Terms smaller than 0.1" or 0.0000001 AU are neglected here. The greatest
	 * possible error amounts to 2" in longitude, 0.5" in latitude, and 0.000005
	 * AU in distance.<P>
	 *
	 * Reference:
	 *
	 * <I>Practical Ephemeris Calculations</I>, Oliver Montenbruck. 1989.
	 *
	 * @param JD Julian day in dynamical time.
	 * @return Array with (x, y, z) coordinates.
	 */
	public static double[] sunPosition(double JD)
	{
		double T = 1.0 + Functions.toCenturies(JD);

		// Long period perturbation of the mean longitude and the mean anomaly
		// of the sun
		double DLP = 0.0;
		double DLP_data[][] =
		{ new double[]
		{ 1.882, -0.016, 57.24, 150.27 }, new double[]
		{ 6.40, 0.0, 231.19, 20.20 }, new double[]
		{ 0.266, 0.0, 31.80, 119.00 } };
		for (int i = 0; i < 3; i++)
		{
			DLP += (DLP_data[i][0] + DLP_data[i][1] * T) * Math.sin(Functions
					.normalizeDegrees(DLP_data[i][2] + DLP_data[i][3] * T) * Constant.DEG_TO_RAD);
		}
		DLP = DLP * Constant.ARCSEC_TO_RAD;

		// Mean longitude of the sun
		double LO = 0.0;
		double LO_data[] = new double[]
		{ 279.6966778, 36000, 2768.13 * Constant.ARCSEC_TO_DEG, 1.089 * Constant.ARCSEC_TO_DEG,
				0.202 * Constant.ARCSEC_TO_DEG, 315.6, 893.3 };
		LO += LO_data[0] + LO_data[1] * T + LO_data[2] * T + LO_data[3] * T * T + LO_data[4] * Math.sin(Functions
				.normalizeDegrees(LO_data[5] + LO_data[6] * T) * Constant.DEG_TO_RAD);
		LO = Functions.normalizeDegrees(LO) * Constant.DEG_TO_RAD + DLP;

		// Mean anomaly of the sun, Venus, Mars, Jupiter, and Saturn
		double G[] = new double[]
		{
				Functions
						.normalizeRadians((358.4758333 + 35999 * T + 179.10 * Constant.ARCSEC_TO_DEG * T - 0.54 * Constant.ARCSEC_TO_DEG * T * T) * Constant.DEG_TO_RAD + DLP),
				0.0,
				Functions.normalizeRadians((212.45 + 58517.493 * T) * Constant.DEG_TO_RAD),
				0.0,
				Functions.normalizeRadians((319.58 + 19139.977 * T) * Constant.DEG_TO_RAD),
				Functions.normalizeRadians((225.28 + 3034.583 * T + 1300 * Constant.ARCSEC_TO_DEG * Math.sin(Functions
						.normalizeDegrees(133.775 + 39.804 * T) * Constant.DEG_TO_RAD)) * Constant.DEG_TO_RAD),
				Functions.normalizeRadians((175.60 + 1221.794 * T) * Constant.DEG_TO_RAD) };

		// Mean angular distance of the moon from the sun (longitude of moon -
		// longitude of sun)
		double D = 350.737486 + 445267.114217 * T;
		D = Functions.normalizeRadians(D * Constant.DEG_TO_RAD);

		// Mean anomaly of the moon
		double A = 296.104608 + 477198.849108 * T;
		A = Functions.normalizeRadians(A * Constant.DEG_TO_RAD);

		// Mean argument of the latitude of the moon (distance from lunar node)
		double U = 11.250889 + 483202.025150 * T;
		U = Functions.normalizeRadians(U * Constant.DEG_TO_RAD);

		// Difference between true and mean longitudes of the sun according to
		// the two body problem (equation of centre)
		double DL = (6910.057 - 17.240 * T) * Math.sin(G[0]);
		DL += (72.338 - 0.361 * T) * Math.sin(2.0 * G[0]);
		DL += 1.054 * Math.sin(3.0 * G[0]);
		DL = DL * Constant.ARCSEC_TO_RAD;

		// Logarithm to base ten of radius in AU, according to the two body
		// problem.
		double RO = 0.00003057 - 0.00000015 * T;
		RO += (-0.00727412 + 0.00001814 * T) * Math.cos(G[0]);
		RO += (-0.00009138 + 0.00000046 * T) * Math.cos(2.0 * G[0]);
		RO += -0.00000145 * Math.cos(3.0 * G[0]);

		// Perturbations in longitude due to Venus (2), Mars (4), Jupiter (5),
		// and Saturn (6)
		double DL2_data[][] =
		{ new double[]
		{ 4.838, 299.102, 1, -1 }, new double[]
		{ 0.116, 148.900, 2, -1 }, new double[]
		{ 5.526, 148.313, 2, -2 }, new double[]
		{ 2.497, 315.943, 2, -3 }, new double[]
		{ 0.666, 177.710, 3, -3 }, new double[]
		{ 1.559, 345.253, 3, -4 }, new double[]
		{ 1.024, 318.150, 3, -5 }, new double[]
		{ 0.210, 206.200, 4, -4 }, new double[]
		{ 0.144, 195.400, 4, -5 }, new double[]
		{ 0.152, 343.800, 4, -6 }, new double[]
		{ 0.123, 195.300, 5, -7 }, new double[]
		{ 0.154, 359.600, 5, -8 } };
		double DL4_data[][] =
		{ new double[]
		{ 0.274, 217.700, -1, 1 }, new double[]
		{ 2.043, 343.888, -2, 2 }, new double[]
		{ 1.770, 200.402, -2, 1 }, new double[]
		{ 0.129, 294.200, -3, 3 }, new double[]
		{ 0.425, 338.880, -3, 2 }, new double[]
		{ 0.500, 105.180, -4, 3 }, new double[]
		{ 0.585, 334.060, -4, 2 }, new double[]
		{ 0.204, 100.800, -5, 3 }, new double[]
		{ 0.154, 227.400, -6, 4 }, new double[]
		{ 0.101, 096.300, -6, 3 }, new double[]
		{ 0.106, 222.700, -7, 4 } };
		double DL5_data[][] =
		{ new double[]
		{ 0.163, 198.600, -1, 2 }, new double[]
		{ 7.208, 179.532, -1, 1 }, new double[]
		{ 2.600, 263.217, -1, 0 }, new double[]
		{ 2.731, 087.145, -2, 2 }, new double[]
		{ 1.610, 109.493, -2, 1 }, new double[]
		{ 0.164, 170.500, -3, 3 }, new double[]
		{ 0.556, 082.650, -3, 2 }, new double[]
		{ 0.210, 098.500, -3, 1 } };
		double DL6_data[][] =
		{ new double[]
		{ 0.419, 100.580, -1, 1 }, new double[]
		{ 0.320, 269.460, -1, 0 }, new double[]
		{ 0.108, 290.600, -2, 2 }, new double[]
		{ 0.112, 293.600, -2, 1 } };

		// Obtain contributions to longitude
		double LON = 0.0;
		for (int i = 0; i < 12; i++)
		{
			LON += DL2_data[i][0] * Math
					.cos(DL2_data[i][1] * Constant.DEG_TO_RAD + DL2_data[i][2] * G[2] + DL2_data[i][3] * G[0]);
			if (i < 11)
				LON += DL4_data[i][0] * Math
						.cos(DL4_data[i][1] * Constant.DEG_TO_RAD + DL4_data[i][2] * G[4] + DL4_data[i][3] * G[0]);
			if (i < 8)
				LON += DL5_data[i][0] * Math
						.cos(DL5_data[i][1] * Constant.DEG_TO_RAD + DL5_data[i][2] * G[5] + DL5_data[i][3] * G[0]);
			if (i < 4)
				LON += DL6_data[i][0] * Math
						.cos(DL6_data[i][1] * Constant.DEG_TO_RAD + DL6_data[i][2] * G[6] + DL6_data[i][3] * G[0]);
		}
		LON = LON * Constant.ARCSEC_TO_RAD;

		// Longitude perturbation by the moon
		double DLM = 6.454 * Math.sin(D) + 0.177 * Math.sin(D + A) - 0.424 * Math.sin(D - A) + 0.172 * Math
				.sin(D - G[0]);
		DLM = DLM * Constant.ARCSEC_TO_RAD;

		// Perturbations in LOG(R) by Venus, Mars, Jupiter, and Saturn
		double DR2_data[][] =
		{ new double[]
		{ 2359, 209.080, 1, -1 }, new double[]
		{ 160, 58.400, 2, -1 }, new double[]
		{ 6842, 58.318, 2, -2 }, new double[]
		{ 869, 226.700, 2, -3 }, new double[]
		{ 1045, 87.570, 3, -3 }, new double[]
		{ 1497, 255.250, 3, -4 }, new double[]
		{ 194, 49.500, 3, -5 }, new double[]
		{ 376, 116.280, 4, -4 }, new double[]
		{ 196, 105.200, 4, -5 }, new double[]
		{ 163, 145.400, 5, -5 }, new double[]
		{ 141, 105.400, 5, -7 } };
		double DR4_data[][] =
		{ new double[]
		{ 150, 127.700, -1, 1 }, new double[]
		{ 2057, 253.828, -2, 2 }, new double[]
		{ 151, 295.000, -2, 1 }, new double[]
		{ 168, 203.500, -3, 3 }, new double[]
		{ 215, 249.000, -3, 2 }, new double[]
		{ 478, 15.170, -4, 3 }, new double[]
		{ 105, 65.900, -4, 2 }, new double[]
		{ 107, 324.600, -5, 4 }, new double[]
		{ 139, 137.300, -6, 4 } };
		double DR5_data[][] =
		{ new double[]
		{ 208, 112.000, -1, 2 }, new double[]
		{ 7067, 89.545, -1, 1 }, new double[]
		{ 244, 338.600, -1, 0 }, new double[]
		{ 103, 350.500, -2, 3 }, new double[]
		{ 4026, 357.108, -2, 2 }, new double[]
		{ 1459, 19.467, -2, 1 }, new double[]
		{ 281, 81.200, -3, 3 }, new double[]
		{ 803, 352.560, -3, 2 }, new double[]
		{ 174, 8.600, -3, 1 }, new double[]
		{ 113, 347.700, -4, 2 } };
		double DR6_data[][] =
		{ new double[]
		{ 429, 10.600, -1, 1 }, new double[]
		{ 162, 200.600, -2, 2 }, new double[]
		{ 112, 203.100, -2, 1 } };

		// Obtain contributions to LOG(R)
		double RAD = 0.0;
		for (int i = 0; i < 11; i++)
		{
			RAD += DR2_data[i][0] * Math
					.cos(DR2_data[i][1] * Constant.DEG_TO_RAD + DR2_data[i][2] * G[2] + DR2_data[i][3] * G[0]);
			if (i < 9)
				RAD += DR4_data[i][0] * Math
						.cos(DR4_data[i][1] * Constant.DEG_TO_RAD + DR4_data[i][2] * G[4] + DR4_data[i][3] * G[0]);
			if (i < 10)
				RAD += DR5_data[i][0] * Math
						.cos(DR5_data[i][1] * Constant.DEG_TO_RAD + DR5_data[i][2] * G[5] + DR5_data[i][3] * G[0]);
			if (i < 3)
				RAD += DR6_data[i][0] * Math
						.cos(DR6_data[i][1] * Constant.DEG_TO_RAD + DR6_data[i][2] * G[6] + DR6_data[i][3] * G[0]);
		}
		RAD = RAD / 1.0e9;

		// Perturbation in LOG(R) by the moon
		double DRM = 13360 * Math.cos(D) + 370 * Math.cos(D + A) - 1330 * Math.cos(D - A) - 140 * Math.cos(D + G[0]) + 360 * Math
				.cos(D - G[0]);
		DRM = DRM / 1.0e9;

		double LA = -0.210 * Math.cos(151.800 * Constant.DEG_TO_RAD + 3 * G[2] - 4 * G[0]);
		LA += -0.166 * Math.cos(265.500 * Constant.DEG_TO_RAD - 2 * G[5] + G[0]);
		LA += 0.576 * Math.sin(U);

		// Get longitude, latitude, and distance
		LocationElement loc = new LocationElement();
		loc.setLongitude(Functions.normalizeRadians(LO + DL + DLM + LON));
		loc.setLatitude(LA * Constant.ARCSEC_TO_RAD);
		loc.setRadius(Math.pow(10.0, RO + DRM + RAD));

		// To rectangular coordinates
		double pos[] = LocationElement.parseLocationElement(loc);

		return pos;
	}

	/**
	 * Calculate sun position, providing full data. This method uses Newcomb's
	 * solar theory. Typical error is 0.3 arcseconds, but it could be as high as
	 * 2 arcseconds for certain dates.
	 * <P>
	 *
	 * @param time Time object containing the date.
	 * @param obs Observer object containing the observer position.
	 * @param eph Ephemeris object with the target and ephemeris
	 *        properties.
	 * @return Ephem object containing full ephemeris data.
	 * @deprecated This is a very old but accurate method. Better results will
	 *             be obtained with Series96 or VSOP theories.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public static EphemElement newcombSunEphemeris(TimeElement time, // Time
																		// Element
			ObserverElement obs, // Observer Element
			EphemerisElement eph) // Ephemeris Element
			throws JPARSECException
	{
		if (eph.targetBody != TARGET.SUN)
			throw new JPARSECException("target object is not the Sun.");

		if (obs.getMotherBody() != TARGET.EARTH) throw new JPARSECException("observer must be on Earth in Newcomb's method.");

		// Check Ephemeris object
		if (!EphemerisElement.checkEphemeris(eph))
			throw new JPARSECException("invalid ephemeris object.");

		// Obtain julian day in Barycentric Dynamical Time
		double JD_TDB = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);

		// Obtain geocentric position
		double geo_eq[] = Ephem.eclipticToEquatorial(sunPosition(JD_TDB), JD_TDB, eph);

		// Obtain topocentric light_time
		LocationElement loc = LocationElement.parseRectangularCoordinates(geo_eq);
		double light_time = loc.getRadius() * Constant.LIGHT_TIME_DAYS_PER_AU;
		if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.GEOMETRIC)
			light_time = 0.0;
		if (eph.ephemType != EphemerisElement.COORDINATES_TYPE.GEOMETRIC && eph.targetBody != TARGET.SUN)
		{
			double topo[] = obs.topocentricObserverICRF(time, eph);
			geo_eq = Ephem.eclipticToEquatorial(sunPosition(JD_TDB - light_time), JD_TDB, eph);
			double light_time_corrected = Ephem.getTopocentricLightTime(geo_eq, topo, eph);
			// Iterate to obtain correct light time and geocentric position.
			// Typical differente in light time is 0.1 seconds. Iterate to
			// a precission up to 0.001 seconds.
			do
			{
				light_time = light_time_corrected;
				geo_eq = Ephem.eclipticToEquatorial(sunPosition(JD_TDB - light_time), JD_TDB, eph);
				light_time_corrected = Ephem.getTopocentricLightTime(geo_eq, topo, eph);
			} while (Math.abs(light_time - light_time_corrected) > (0.001 / Constant.SECONDS_PER_DAY));
			light_time = light_time_corrected;
		}

		// Obtain heliocentric ecliptic coordinates
		double helio_object[] = Functions.substract(new double[]	{ 0.0, 0.0, 0.0 }, sunPosition(JD_TDB - light_time));
		LocationElement loc_elem = LocationElement.parseRectangularCoordinates(helio_object);

		// Correct for solar deflection and aberration
		if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT)
		{
			// Obtain velocity of Earth
			double time_step = 1.0;
			double earth[] = Ephem.eclipticToEquatorial(sunPosition(JD_TDB - light_time + time_step), JD_TDB,
					eph);
			double earth_vel[] = Functions.scalarProduct(Functions.substract(earth, geo_eq),
					1.0 / time_step);
			double earth_pos[] = new double[]
			{ earth[0], earth[1], earth[2], -earth_vel[0], -earth_vel[1], -earth_vel[2] };
			// Earth's vector can be calculated also applying VSOP. The result
			// is slightly better.
			// Here we prefer to use only Newcomb's theory independently
			geo_eq = Ephem.aberration(geo_eq, earth_pos, light_time);

			DataBase.addData("GCRS", geo_eq, true);
		} else {
			DataBase.addData("GCRS", null, true);
		}

		/* Correct frame bias in J2000 epoch */
		double geo_date[] = geo_eq;
 		if (eph.frame != EphemerisElement.FRAME.FK5)
		{
			// Transform from mean equinox of date to J2000
			geo_date = Precession.precessToJ2000(JD_TDB, geo_date, eph);

			/* Correction to output frame. */
			geo_date = Ephem.toOutputFrame(geo_date, FRAME.FK5, eph.frame);

			if (eph.frame == FRAME.FK4) {
				// Transform from B1950 to mean equinox of date
				 geo_date = Precession.precess(Constant.B1950, JD_TDB, geo_date, eph);
			} else {
				// Transform from J2000 to mean equinox of date
				geo_date = Precession.precessFromJ2000(JD_TDB, geo_date, eph);
			}
		}

		// Mean equatorial to true equatorial
		double true_eq[] = geo_date;
		if (obs.getMotherBody() == TARGET.EARTH) {
			if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT)
			{
				/* Correct nutation */
				true_eq = Nutation.nutateInEquatorialCoordinates(JD_TDB, eph, geo_date, true);
			}

			// Correct for polar motion
			if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT &&
					eph.correctForPolarMotion)
			{
				Matrix mat = IAU2006.getPolarMotionCorrectionMatrix(time, obs, eph);
				true_eq = mat.times(new Matrix(true_eq)).getColumn(0);
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
		try
		{
			ephem_elem = PhysicalParameters.physicalParameters(JD_TDB, ephem_elem, ephem_elem, obs, eph);
			if (obs.getMotherBody() != TARGET.NOT_A_PLANET && obs.getMotherBody() != TARGET.EARTH) {
				LocationElement locE = ephem_elem.getEquatorialLocation();
				locE = Ephem.getPositionFromEarth(locE, time, obs, eph);
				ephem_elem.constellation = jparsec.astronomy.Constellation.getConstellationName(locE.getLongitude(),
						locE.getLatitude(), JD_TDB, eph);
			}
		} catch (JPARSECException ve)
		{
			throw ve;
		}

		/* Horizontal coordinates */
		if (eph.isTopocentric)
			ephem_elem = Ephem.horizontalCoordinates(time, obs, eph, ephem_elem);

		/* Set coordinates to the output equinox */
		if (EphemerisElement.EQUINOX_OF_DATE != eph.equinox)
		{
			ephem_elem = Ephem.toOutputEquinox(ephem_elem, eph, JD_TDB);
		}

		ephem_elem.name = TARGET.SUN.getName();
		return ephem_elem;
	}
}
