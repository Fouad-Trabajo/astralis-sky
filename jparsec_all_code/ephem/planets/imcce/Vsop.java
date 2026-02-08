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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.EphemerisElement.ALGORITHM;
import jparsec.ephem.EphemerisElement.COORDINATES_TYPE;
import jparsec.ephem.EphemerisElement.FRAME;
import jparsec.ephem.EphemerisElement.REDUCTION_METHOD;
import jparsec.ephem.Functions;
import jparsec.ephem.IAU2006;
import jparsec.ephem.Nutation;
import jparsec.ephem.PhysicalParameters;
import jparsec.ephem.Precession;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.planets.EphemElement;
import jparsec.io.FileFormatElement;
import jparsec.io.FileIO;
import jparsec.io.ReadFormat;
import jparsec.math.Constant;
import jparsec.math.matrix.Matrix;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.SiderealTime;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.time.TimeScale;
import jparsec.util.DataBase;
import jparsec.util.JPARSECException;

/**
 * This class provides VSOP planetary positions for Mercury through Neptune.
 * The version of VSOP implemeted is VSOP87A (J2000 heliocentric rectangular
 * coordinates).
*
 * Bretagnon P., Francou G., : 1988, Astron. Astrophys., 202, 309.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Vsop
{
	// private constructor so that this class cannot be instantiated.
	private Vsop() {}

	/**
	 * Obtain rectangular position of a planet using full VSOP87 theory. Mean
	 * equinox and ecliptic J2000.
	 *
	 * @param JD Julian day in TDB.
	 * @param planet Planet ID.
	 * @return Array (x, y, z, vx, vy, vz).
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public static double[] getHeliocentricEclipticPositionJ2000(double JD, TARGET planet) throws JPARSECException
	{
		String extension;
		switch (planet)
		{
		case MERCURY:
			extension = "mer";
			break;
		case VENUS:
			extension = "ven";
			break;
		case EARTH:
			extension = "ear";
			break;
		case Earth_Moon_Barycenter:
			extension = "emb";
			break;
		case MARS:
			extension = "mar";
			break;
		case JUPITER:
			extension = "jup";
			break;
		case SATURN:
			extension = "sat";
			break;
		case URANUS:
			extension = "ura";
			break;
		case NEPTUNE:
			extension = "nep";
			break;
		case Moon:
			extension = "emb";
			break;
		case SUN:
			extension = "";
			break;
		default:
			throw new JPARSECException("Invalid body " + planet + '.');
		}

		if ("".equals(extension))
			return new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };

		double jcen = Functions.toCenturies(JD);
		return evaluateVsop(extension, jcen);
	}

	private static final String PATH_TO_FILE = FileIO.VSOP87_DIRECTORY + "VSOP87a";

	/**
	 * Evaluates full VSOP87A theory.
	 *
	 * @param extension Planet extension of file.
	 * @param jcen Julian centuries from J2000.
	 * @return Array (x, y, z, vx, vy, vz), mean equinox and ecliptic J2000.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	private static double[] evaluateVsop(String extension, double jcen) throws JPARSECException
	{
		double out[] = new double[]
		{ 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };

		double jmil = jcen * 0.1;
		double t[] = new double[]
		{ 0.0, 1.0, jmil, Math.pow(jmil, 2.0), Math.pow(jmil, 3.0), Math.pow(jmil, 4.0), Math.pow(jmil, 5.0) };

		int k = 0;
		int ideb = 0;
		int iv = 0;

		/* File reading and substitution of time */
		String line = "";
		FileFormatElement VSOP_begin_record[] =
			{ new FileFormatElement(18, 18, "THEORY_ID"), new FileFormatElement(23, 29, "PLANET_NAME"),
				new FileFormatElement(42, 42, "VARIABLE_NUMBER"), new FileFormatElement(60, 60, "TIME_EXPONENT"),
				new FileFormatElement(61, 67, "NUMER_OF_TERMS") };
		FileFormatElement VSOP_record[] =
			{ new FileFormatElement(80, 97, "A"), new FileFormatElement(98, 111, "B"), new FileFormatElement(112, 131, "C") };

		ReadFormat rf_begin = new ReadFormat();
		rf_begin.setFormatToRead(VSOP_begin_record);
		ReadFormat rf = new ReadFormat();
		rf.setFormatToRead(VSOP_record);

		try
		{
			InputStream is = Vsop.class.getClassLoader().getResourceAsStream(PATH_TO_FILE+"."+extension);
			BufferedReader dis = new BufferedReader(new InputStreamReader(is));

			while ((line = dis.readLine()) != null)	{
				iv = rf_begin.readInteger(line, "THEORY_ID");
				int ic = rf_begin.readInteger(line, "VARIABLE_NUMBER") - 1;
				int it = rf_begin.readInteger(line, "TIME_EXPONENT") + 1;
				int in = rf_begin.readInteger(line, "NUMER_OF_TERMS");

				if (ideb == 0) {
					ideb = 1;
					if (iv == 0)
						k = 2;
					if (iv == 2 || iv == 4)
						k = 1;
				}

				if (in > 0)	{
					for (int n = 0; n < in; n++) {
						line = dis.readLine();
						double a = rf.readDouble(line, "A");
						double b = rf.readDouble(line, "B");
						double c = rf.readDouble(line, "C");

						double u = b + c * t[2];
						double cu = Math.cos(u);
						out[ic] += a * cu * t[it];
						if (iv == 0)
							break;
						double su = Math.sin(u);
						out[ic + 3] += t[it - 1] * (it - 1) * a * cu;
						out[ic + 3] -= t[it] * a * c * su;
					}
				}
			}

			// Close file
			dis.close();

			if (iv != 0) {
				for (int i = 3; i < 6; i++)	{
					out[i] = out[i] / Constant.JULIAN_DAYS_PER_MILLENIA;
				}
			}

			if (k != 0)
				out[k] = Functions.normalizeRadians(out[k]);
		} catch (FileNotFoundException e1) {
			throw new JPARSECException("file not found in path " + PATH_TO_FILE+".", e1);
		} catch (IOException e2) {
			throw new JPARSECException("error while reading file " + PATH_TO_FILE + ".", e2);
		}

		return out;
	}

	/**
	 * Get rectangular ecliptic geocentric position of a planet in equinox
	 * J2000.
	 *
	 * @param JD Julian day in TDB.
	 * @param planet Planet ID.
	 * @param light_time Light time in days.
	 * @param addSat True to add the planetocentric position of the satellite to the position
	 * of the planet.
	 * @param obs The observer object. Can be null for the Earth's center.
	 * @return Array with x, y, z, vx, vy, vz coordinates. Note velocity components are those
	 * for the Earth (used for aberration correction) not those for the planet relative to the
	 * geocenter.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public static double[] getGeocentricPosition(double JD, TARGET planet, double light_time,
			boolean addSat, ObserverElement obs) throws JPARSECException
	{
		// Heliocentric position corrected for light time
		double helio_object[] = getHeliocentricEclipticPositionJ2000(JD - light_time, planet);

		if (addSat) {
			Object o = DataBase.getData("offsetPosition", true);
			if (o != null) {
				double[] planetocentricPositionOfTargetSatellite = (double[]) o;
				helio_object = Functions.sumVectors(helio_object, planetocentricPositionOfTargetSatellite);
			}
		}

		double helio_earth[] = null;
		if (obs != null && obs.getMotherBody() != TARGET.EARTH) {
			helio_object[3] = helio_object[4] = helio_object[5] = 0.0;
			EphemerisElement eph = new EphemerisElement();
			eph.ephemMethod = REDUCTION_METHOD.IAU_2006;
			eph.algorithm = ALGORITHM.VSOP87_ELP2000ForMoon;
			helio_earth = Ephem.equatorialToEcliptic(obs.heliocentricPositionOfObserver(JD, eph), Constant.J2000, eph);
		} else {
			// Compute position of Earth
			helio_earth = getHeliocentricEclipticPositionJ2000(JD, TARGET.EARTH);
		}

		// Compute geocentric position of the object, and also velocity vector of the geocenter
		double geo_pos[] = new double[]
		{ -helio_earth[0] + helio_object[0], -helio_earth[1] + helio_object[1], -helio_earth[2] + helio_object[2],
		helio_earth[3], helio_earth[4], helio_earth[5], };

		return geo_pos;
	}

	/**
	 * Transform J2000 mean ecliptic coordinates into equatorial.
	 * Specific to this theory (class) to compare positions with DE200.
	 *
	 * @param position Ecliptic coordinates (x, y, z) or (x, y, z, vx, vy, vz)
	 *        refered to mean ecliptic and dynamical equinox of J2000.
	 * @return Equatorial FK5 coordinates.
	 */
	private static double[] meanEclipticJ2000ToEquatorialFK5(double position[])
	{
		double RotM[][] = new double[4][4];
		double out_pos[] = new double[3];
		double out_vel[] = new double[3];

		RotM[1][1] = 1.000000000000;
		RotM[1][2] = 0.000000440360;
		RotM[1][3] = -0.000000190919;
		RotM[2][1] = -0.000000479966;
		RotM[2][2] = 0.917482137087;
		RotM[2][3] = -0.397776982902;
		RotM[3][1] = 0.000000000000;
		RotM[3][2] = 0.397776982902;
		RotM[3][3] = 0.917482137087;

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
	 * Calculate planetary positions, providing full data. This method uses full
	 * VSOP87A theory from the IMCCE.
	 * <P>
	 * Typical difference respect DE200 is below 0.01 arcseconds during the 21st century, and below 1"
	 * (compared to JPL DE403 Ephemeris) for inner planets outside this century.
	 * It is not recommended to use VSOP87 for giant planets before 1900 or
	 * after 2100, despite that the results match JPL DE200 Ephemeris to within
	 * the arcsecond for several millenia. Results closer to JPL DE403 can be obtained with
	 * JPL ephemeris or Moshier algorithms.
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
	public static EphemElement vsopEphemeris(TimeElement time, // Time Element
			ObserverElement obs, // Observer Element
			EphemerisElement eph) // Ephemeris Element
			throws JPARSECException
	{
		if ((!eph.targetBody.isPlanet() && eph.targetBody != TARGET.SUN) || eph.targetBody == TARGET.EARTH || eph.targetBody == TARGET.Earth_Moon_Barycenter)
			throw new JPARSECException("target object is invalid.");

		// Check Ephemeris object
		if (!EphemerisElement.checkEphemeris(eph))
			throw new JPARSECException("invalid ephemeris object.");

		/* Obtain topocentric position of object */
		EphemElement ephem_elem = vsopCalc(time, obs, eph, true, true);

		// Obtain julian day in Barycentric Dynamical Time
		double JD_TDB = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);

		/* Physical ephemeris */
		EphemerisElement new_eph = eph.clone();
		new_eph.ephemType = COORDINATES_TYPE.APPARENT;
		new_eph.equinox = EphemerisElement.EQUINOX_OF_DATE;
		EphemElement ephem_elem2 = ephem_elem;
		if (eph.ephemType != EphemerisElement.COORDINATES_TYPE.APPARENT || eph.equinox != EphemerisElement.EQUINOX_OF_DATE)
			ephem_elem2 = vsopCalc(time, obs, new_eph, true, true);
		new_eph.targetBody = TARGET.SUN;
		ephem_elem2 = PhysicalParameters.physicalParameters(JD_TDB, vsopCalc(time, obs, new_eph, false, false), ephem_elem2, obs, eph);
		PhysicalParameters.setPhysicalParameters(ephem_elem, ephem_elem2, time, obs, eph);

		/* Horizontal coordinates */
		if (eph.isTopocentric)
			ephem_elem = Ephem.horizontalCoordinates(time, obs, eph, ephem_elem);

		/* Set coordinates to the output equinox */
		if (EphemerisElement.EQUINOX_OF_DATE != eph.equinox)
			ephem_elem = Ephem.toOutputEquinox(ephem_elem, eph, JD_TDB);

		ephem_elem.name = eph.targetBody.getName();
		return ephem_elem;
	}

	private static EphemElement vsopCalc(TimeElement time, // Time Element
			ObserverElement obs, // Observer Element
			EphemerisElement eph, boolean addGCRS, boolean addOffset) // Ephemeris Element
			throws JPARSECException
	{
		// Obtain julian day in Barycentric Dynamical Time
		double JD_TDB = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);

		// Add warning for possible incorrect result.
		if ((JD_TDB < 2415020.5 || JD_TDB > 2488092.5) && eph.targetBody.compareTo(TARGET.MARS) > 0) JPARSECException.addWarning("VSOP is not recommended for giant planets outside years 1900-2100");

		// Obtain geocentric position
		double geo_eq[] = Vsop.meanEclipticJ2000ToEquatorialFK5(Vsop.getGeocentricPosition(JD_TDB, eph.targetBody, 0.0, addOffset, obs));

		// Obtain topocentric light_time
		LocationElement loc = LocationElement.parseRectangularCoordinates(geo_eq);
		double light_time = loc.getRadius() * Constant.LIGHT_TIME_DAYS_PER_AU;
		if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.GEOMETRIC)
			light_time = 0.0;

		if (eph.ephemType != EphemerisElement.COORDINATES_TYPE.GEOMETRIC) // && eph.targetBody != TARGET.SUN)
		{
			double topo[] = obs.topocentricObserverICRF(time, eph);
			geo_eq = Vsop.meanEclipticJ2000ToEquatorialFK5(Vsop.getGeocentricPosition(JD_TDB, eph.targetBody, light_time, addOffset, obs));
			double light_time_corrected = Ephem.getTopocentricLightTime(geo_eq, topo, eph);
			// Iterate to obtain correct light time and geocentric position.
			// Typical differente in light time is 0.1 seconds. Iterate to
			// a precission up to 1E-6 seconds.
			do
			{
				light_time = light_time_corrected;
				geo_eq = Vsop.meanEclipticJ2000ToEquatorialFK5(Vsop.getGeocentricPosition(JD_TDB, eph.targetBody,
						light_time, addOffset, obs));
				light_time_corrected = Ephem.getTopocentricLightTime(geo_eq, topo, eph);
			} while (Math.abs(light_time - light_time_corrected) > (1.0E-6 / Constant.SECONDS_PER_DAY));
			light_time = light_time_corrected;
		}

		// Obtain heliocentric equatorial coordinates
		double helio_object[] = Vsop.getHeliocentricEclipticPositionJ2000(JD_TDB - light_time, eph.targetBody);
		if (addOffset) {
			Object o = DataBase.getData("offsetPosition", true);
			if (o != null) {
				double[] planetocentricPositionOfTargetSatellite = (double[]) o;
				helio_object = Functions.sumVectors(helio_object, planetocentricPositionOfTargetSatellite);
			}
		}
		helio_object = Ephem.eclipticToEquatorial(helio_object, Constant.J2000, eph);

		// Correct for solar deflection and aberration
		if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT)
		{
			double geo_sun_0[] = Vsop.meanEclipticJ2000ToEquatorialFK5(Vsop
					.getGeocentricPosition(JD_TDB, TARGET.SUN, 0.0, false, obs));
			if (eph.preferPrecisionInEphemerides)
				// geo_eq = Ephem.solarDeflection(geo_eq, geo_sun_0, helio_object);
				geo_eq = Ephem.solarAndPlanetaryDeflection(geo_eq, geo_sun_0, helio_object,
					new TARGET[] {TARGET.JUPITER}, JD_TDB, false, obs);

			geo_eq = Ephem.aberration(geo_eq, geo_sun_0, light_time);
			if (addGCRS) DataBase.addData("GCRS", geo_eq, true);
		} else {
			if (addGCRS) DataBase.addData("GCRS", null, true);
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
			if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT) {
				/* Correct nutation */
				true_eq = Nutation.nutateInEquatorialCoordinates(JD_TDB, eph, geo_date, true);
			}

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

		if (eph.targetBody == TARGET.SUN) ephem_elem.heliocentricEclipticLatitude = ephem_elem.heliocentricEclipticLongitude =
			ephem_elem.distanceFromSun = 0;

		/* Topocentric correction */
		if (eph.isTopocentric)
			ephem_elem = Ephem.topocentricCorrection(time, obs, eph, ephem_elem);

		return ephem_elem;
	}
}
