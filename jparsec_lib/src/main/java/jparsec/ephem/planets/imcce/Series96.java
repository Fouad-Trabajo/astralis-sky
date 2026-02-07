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
import jparsec.ephem.EphemerisElement.FRAME;
import jparsec.ephem.EphemerisElement.REDUCTION_METHOD;
import jparsec.ephem.Functions;
import jparsec.ephem.IAU2006;
import jparsec.ephem.Nutation;
import jparsec.ephem.PhysicalParameters;
import jparsec.ephem.Precession;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.moons.MoonEphem;
import jparsec.ephem.planets.EphemElement;
import jparsec.ephem.planets.PlanetEphem;
import jparsec.io.FileIO;
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
import jparsec.util.Translate;

/**
 * A class to calculate planetary orbits based on the IMCCE's Series96. A fit to
 * JPL DE403 ephemeris over the interval 1900-2100 (1850-2100 for Neptune and
 * 1700-2100 for Pluto). The interval 1900-2100 is the one when this theory is
 * applicable, because we need the position and velocity of the Earth to obtain
 * geocentric coordinates, and the Series96 theory is limited to years 1900-2100
 * in this respect.
 * <P>
 * Representation of planetary ephemerides by frequency analysis. Application to
 * the five outer planets. Astron. and Astrophys. Suppl. Ser., 109, 191 (1995). J.
 * Chapront, G. Francou - IMCCE (France).
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Series96
{
	// private constructor so that this class cannot be instantiated.
	private Series96() {}

	/**
	 * Path to series96 folder where the files are located.
	 */
	private static final String PATH = FileIO.SERIES96_DIRECTORY;

	/**
	 * Get rectangular equatorial geocentric position of a planet in epoch
	 * J2000.
	 *
	 * @param JD Julian day in TDB.
	 * @param planet Planet ID.
	 * @param light_time Light time in days.
	 * @param addSat True to add the planetocentric position of the satellite to the position
	 * of the planet.
	 * @param obs The observer object. Can be null for the Earth's center.
	 * @return Array with x, y, z, (object position) vx, vy, vz (Earth barycentric
	 * velocity) coordinates. Note velocity components are those
	 * for the Earth (used for aberration correction) not those for the planet relative to the
	 * geocenter.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public static double[] getGeocentricPosition(double JD, TARGET planet, double light_time, boolean addSat, ObserverElement obs) throws JPARSECException
	{
		// Heliocentric position corrected for light time
		double helio_object[] = getHeliocentricEquatorialPositionJ2000(JD - light_time, planet);

		if (addSat) {
			Object o = DataBase.getData("offsetPosition", true);
			if (o != null) {
				double[] planetocentricPositionOfTargetSatellite = (double[]) o;
				helio_object = Functions.sumVectors(helio_object, planetocentricPositionOfTargetSatellite);
			}
		}

		if (obs != null && obs.getMotherBody() != TARGET.EARTH) {
			helio_object[3] = helio_object[4] = helio_object[5] = 0.0;
			EphemerisElement eph = new EphemerisElement();
			eph.ephemMethod = REDUCTION_METHOD.IAU_2006;
			eph.algorithm = ALGORITHM.SERIES96_MOSHIERForMoon;
			double helio_earth[] = obs.heliocentricPositionOfObserver(JD, eph);
			double geo_pos[] = new double[]
					{ -helio_earth[0] + helio_object[0], -helio_earth[1] + helio_object[1], -helio_earth[2] + helio_object[2],
					helio_earth[3], helio_earth[4], helio_earth[5], };
			return geo_pos;
		}

		// Geocentric position of Earth-Moon barycenter
		double geo_barycenter[] = getBarycenter(JD);
		double time_step = 0.1;
		double geo_barycenter_plus[] = getBarycenter(JD + time_step);
		double geo_barycenter_vel[] =
		{ (geo_barycenter_plus[0] - geo_barycenter[0]) / time_step,
				(geo_barycenter_plus[1] - geo_barycenter[1]) / time_step,
				(geo_barycenter_plus[2] - geo_barycenter[2]) / time_step };
		// Compute apparent position of barycenter from Earth
		double helio_barycenter[] = getHeliocentricEquatorialPositionJ2000(JD, TARGET.Earth_Moon_Barycenter);

		// Compute geocentric position of the object, and
		// also velocity vector of the geocenter
		double geo_pos[] = new double[]
		{ geo_barycenter[0] - helio_barycenter[0] + helio_object[0],
				geo_barycenter[1] - helio_barycenter[1] + helio_object[1],
				geo_barycenter[2] - helio_barycenter[2] + helio_object[2],

				-geo_barycenter_vel[0] + helio_barycenter[3], -geo_barycenter_vel[1] + helio_barycenter[4],
				-geo_barycenter_vel[2] + helio_barycenter[5], };

		return geo_pos;

	}

	/**
	 * Calculate heliocentric position for a given time (dynamical equinox and
	 * ecliptic J2000). Methods from Capitaine et al. are considered.
	 *
	 * @param JD Julian day in TDB. Must be in the range 2341972.5 - 2488092.5
	 *        (years 1700 - 2100) for Pluto. For Neptune JD starts at 2396758.5,
	 *        and for the other planets at 2415020.5. Scale: TDB.
	 * @param planet Object to calculate.
	 * @return An array of 6 elements containing the heliocentric rectangular
	 *         position and velocity of object (mean equinox and ecliptic of
	 *         J2000).
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public static double[] getHeliocentricEclipticPositionJ2000(double JD, TARGET planet) throws JPARSECException
	{
		double v[] = getHeliocentricEquatorialPositionJ2000(JD, planet);
		EphemerisElement eph = new EphemerisElement();
		eph.ephemMethod = REDUCTION_METHOD.IAU_2006;
		v = Ephem.equatorialToEcliptic(v, Constant.J2000, eph);
		return v;
	}

	/**
	 * Calculate heliocentric position for a given time (dynamical equinox and
	 * equator J2000).
	 *
	 * @param JD Julian day in TDB. Must be in the range 2341972.5 - 2488092.5
	 *        (years 1700 - 2100) for Pluto. For Neptune JD starts at 2396758.5,
	 *        and for the other planets at 2415020.5. Scale: TDB.
	 * @param planet Object to calculate.
	 * @return An array of 6 elements containing the heliocentric rectangular
	 *         position and velocity of object (mean equinox and equator J2000).
	 *         For Pluto the body center is returned instead of the barycenter, 
	 *         using the latest reduction method available.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public static double[] getHeliocentricEquatorialPositionJ2000(double JD, TARGET planet) throws JPARSECException
	{
		if ((JD < 2341972.5 || JD > 2488092.5) && planet == TARGET.Pluto)
			throw new JPARSECException(
					"invalid date " + JD + " for Pluto, outside interval (2341972.5 - 2488092.5).");
		if ((JD < 2396758.5 || JD > 2488092.5) && planet == TARGET.NEPTUNE)
			throw new JPARSECException(
					"invalid date " + JD + " for Neptune, outside interval (2396758.5.5 - 2488092.5).");
		if ((JD < 2415020.5 || JD > 2488092.5) && planet != TARGET.NEPTUNE && planet != TARGET.Pluto)
			throw new JPARSECException(
					"invalid date " + JD + ", outside interval (2415020.5 - 2488092.5).");

		double object[] = new double[] { 0, 0, 0, 0, 0, 0};

		if (planet == TARGET.SUN)
			return object;

		TARGET pl = planet;
		if (pl == TARGET.EARTH)
			pl = TARGET.Earth_Moon_Barycenter;
		Series96 s96 = new Series96();
		Series96_set set = s96.readSeries96(pl, JD);

		if (JD >= (set.TDEB - 0.5) && JD <= (set.TFIN + 0.5))
		{

			int nb = (int) ((JD - set.TDEB) / set.DT) + 1;
			if (JD <= set.TDEB)
				nb = 1;
			if (JD >= set.TFIN)
				nb = set.IBLOCK;

			double tinit = set.TDEB + (nb - 1) * set.DT;
			double x = 2.0 * (JD - tinit) / set.DT - 1.0;
			double fx = x * set.DT / 2.0;

			// Compute the positions (secular terms)
			double wx = 1.0;
			int max = set.IMAX * 2 - 1;
			for (int iv = 0; iv <= 2; iv++)
			{
				wx = 1.0;
				for (int i = 0; i <= max; i++)
				{
					object[iv] += set.SEC[iv][i] * wx;
					wx = wx * x;
				}
			}

			// Compute the positions (Poisson terms)
			wx = 1.0;
			for (int m = 0; m <= set.MX; m++)
			{
				double ws[] =
				{ 0.0, 0.0, 0.0 };
				for (int i = 0; i < set.NF[m]; i++)
				{
					double f = set.FQ[m][i] * fx;
					double cf = Math.cos(f);
					double sf = Math.sin(f);

					for (int iv = 0; iv < 3; iv++)
					{
						ws[iv] += set.CT[iv][m][i] * cf + set.ST[iv][m][i] * sf;
					}

				}

				object[0] += ws[0] * wx;
				object[1] += ws[1] * wx;
				object[2] += ws[2] * wx;

				wx = wx * x;
			}

			// Compute velocities (secular terms)
			double wt = 2.0 / set.DT;
			for (int iv = 0; iv < 3; iv++)
			{
				wx = 1.0;
				for (int i = 1; i <= max; i++)
				{
					object[3 + iv] = object[3 + iv] + i * set.SEC[iv][i] * wx;
					wx = wx * x;
				}
				object[3 + iv] *= wt;
			}

			// Compute velocities (Poisson terms)
			wx = 1.0;
			double wy = 0.0;
			for (int m = 0; m <= set.MX; m++)
			{
				int nw = set.NF[m];
				for (int i = 0; i < nw; i++)
				{
					double fw = set.FQ[m][i];
					double f = fw * fx;
					double cf = Math.cos(f);
					double sf = Math.sin(f);
					for (int iv = 0; iv < 3; iv++)
					{
						double stw = set.ST[iv][m][i];
						double ctw = set.CT[iv][m][i];
						object[3 + iv] += fw * (stw * cf - ctw * sf) * wx;
						if (m > 0)
							object[3 + iv] += m * wt * (ctw * cf + stw * sf) * wy;
					}
				}
				wy = wx;
				wx = wx * x;
			}

			object[0] = object[0] / 1.0E10;
			object[1] = object[1] / 1.0E10;
			object[2] = object[2] / 1.0E10;
			object[3] = object[3] / 1.0E10;
			object[4] = object[4] / 1.0E10;
			object[5] = object[5] / 1.0E10;

			if (planet == TARGET.EARTH)
			{
				// From Earth-Moon barycenter to Earth's center
				double geo_barycenter[] = getBarycenter(JD);
				double time_step = 0.001;
				double geo_barycenter_plus[] = getBarycenter(JD + time_step);
				double geo_barycenter_vel[] =
				{ (geo_barycenter_plus[0] - geo_barycenter[0]) / time_step,
						(geo_barycenter_plus[1] - geo_barycenter[1]) / time_step,
						(geo_barycenter_plus[2] - geo_barycenter[2]) / time_step };
				object[0] -= geo_barycenter[0];
				object[1] -= geo_barycenter[1];
				object[2] -= geo_barycenter[2];
				object[3] -= geo_barycenter_vel[0];
				object[4] -= geo_barycenter_vel[1];
				object[5] -= geo_barycenter_vel[2];
			}

			// Return position of Pluto's body center
			if (planet == TARGET.Pluto)
				object = MoonEphem.fromPlutoBarycenterToPlutoCenter(object.clone(), JD, EphemerisElement.REDUCTION_METHOD.getLatest(), true);
		}

		return object;
	}

	/**
	 * Returns true if Series96 method is available in the classpath.
	 * @return True or false.
	 */
	public static boolean isSeries96Available() {
		try {
			Series96 s = new Series96();
			s.readSeries96(TARGET.JUPITER, 2451545.0);
			return true;
		} catch (Exception exc) {
			return false;
		}
	}

	/**
	 * Read Series96 files. Files are stored in the path defined by
	 * Series96.path. Files are original from IMCCE, except Pluto, which has
	 * been modified in certain line to be able to read it using
	 * Functions.getField method.
	 *
	 * @param planet Planet ID value.
	 * @param JD Julian day of calculations.
	 * @return A Series96_set object.
	 */
	private Series96_set readSeries96(TARGET planet, double JD) throws JPARSECException
	{
		String fich = Translate.translate(planet.getName(), Translate.getDefaultLanguage(), Translate.LANGUAGE.ENGLISH);

		String line = "";
		Series96_set set = new Series96_set();

		// Lets read the catalogue entries
		try
		{
			InputStream is = getClass().getClassLoader().getResourceAsStream(Series96.PATH + fich);
			BufferedReader dis = new BufferedReader(new InputStreamReader(is));

			line = dis.readLine();
			set.TDEB = Double.parseDouble(FileIO.getField(2, line.trim(), " ", true));
			set.DT = Double.parseDouble(FileIO.getField(3, line.trim(), " ", true));
			set.MX = Integer.parseInt(FileIO.getField(4, line.trim(), " ", true));
			set.IMAX = Integer.parseInt(FileIO.getField(5, line.trim(), " ", true));
			set.IBLOCK = Integer.parseInt(FileIO.getField(6, line.trim(), " ", true));
			set.TFIN = set.TDEB + set.DT * set.IBLOCK;

			int nb = (int) ((JD - set.TDEB) / set.DT) + 1;

			for (int m = 0; m <= set.MX; m++)
			{
				line = dis.readLine();
				set.NF[m] = Integer.parseInt(line.trim());

				for (int i = 0; i < set.NF[m]; i++)
				{
					line = dis.readLine();
					set.FQ[m][i] = Double.parseDouble(line.trim());
				}
			}
			line = dis.readLine();

			for (int k = 1; k <= nb; k++)
			{
				for (int iv = 0; iv < 3; iv++)
				{
					line = dis.readLine();

					for (int i = 0; i <= set.IMAX; i = i + 2)
					{
						line = dis.readLine();
						set.SEC[iv][i] = Double.parseDouble(FileIO.getField(1, line.trim(), " ", true));
						set.SEC[iv][i + 1] = Double.parseDouble(FileIO.getField(2, line.trim(), " ", true));
					}

					for (int m = 0; m <= set.MX; m++)
					{
						int ip = (int) Functions.module((double) m, 2.0);
						for (int i = 0; i < set.NF[m]; i++)
						{
							line = dis.readLine();
							if (ip == 0)
							{
								set.CT[iv][m][i] = Double.parseDouble(FileIO.getField(1, line.trim(), " ", true));
								set.ST[iv][m][i] = Double.parseDouble(FileIO.getField(2, line.trim(), " ", true));
							} else
							{
								set.CT[iv][m][i] = Double.parseDouble(FileIO.getField(2, line.trim(), " ", true));
								set.ST[iv][m][i] = Double.parseDouble(FileIO.getField(1, line.trim(), " ", true));
							}
						}
					}

				}
			}

			dis.close();

		} catch (FileNotFoundException e1)
		{
			throw new JPARSECException("file not found in path " + fich+".", e1);
		} catch (IOException e2)
		{
			throw new JPARSECException("error while reading file " + fich + ".", e2);
		}

		return set;
	}

	/**
	 * Obtain rectangular coordinates of the geocentric position of the
	 * Earth-Moon Barycenter. Equinox and equator J2000. No
	 * limitation exists in the input date. It has been tested that this method
	 * has a discrepancy with DE406 close to 200 km (2 degrees) in the position of the
	 * barycenter at 3000 B.C., producing errors of several 0.1" only
	 * for objects close to Earth.
	 *
	 * @param JD Julian day (TDB).
	 * @return Array with x, y, z coordinates.
	 */
	public static double[] getBarycenter(double JD)
	{
//		if (JD < 2415020.5 || JD > 2488092.5)
//			throw new JPARSECException("invalid date " + JD + ", outside interval (2415020.5 - 2488092.5).");

		double r[] = new double[3];
		double v[] = new double[3];
		int n1[] = new int[]
		{ 1, 44, 93 };
		int n2[] = new int[]
		{ 43, 92, 138 };
		double c[] = new double[]
		{ -244075., -2965., 8528., 2345., -2486., 1426., 527., -43., -393., 394., -218., 73., 91., -173., 25., -20.,
				75., 72., 6., 72., -40., -58., 56., -53., 46., -44., -5., 0., -1., -12., 21., -4., -4., -2., 9., 8.,
				10., 2., -10., -12., -11., 10., -10., -176962., -23344., -11109., -922., -4118., 714., -1135., -601.,
				299., 564., -311., 261., -251., 254., 229., 213., -179., 57., -19., -125., -113., -87., -52., 75., 16.,
				-5., -42., -4., -10., 8., -19., 9., 40., 29., -25., 11., 19., -12., -5., 18., -15., -16., 13., 13., 9.,
				-1., -3., -5., -1., -76714., 25611., -10120., -400., 1387., -1785., 310., 580., -492., -527., 44.,
				130., 244., -135., 113., -38., 110., 92., -78., 25., -54., -26., -49., -38., 27., -23., 32., 2., -2.,
				1., -18., -2., -23., -4., 3., -8., 4., -11., 17., 3., -11., 13., 8., -11., -7., 4. };

		double s[] = new double[]
		{ 192874., 25444., 1005., 4489., -778., 1238., -326., -614., 339., -285., -276., -232., 195., -63., 136., 124.,
				95., 57., -82., 5., 45., 4., 11., -9., 20., -10., -44., -32., 27., -20., 6., -20., 17., 17., -14.,
				-14., 4., -11., -10., 3., 6., -7., 4., -223938., -2720., 635., 7824., 2151., -2281., 1309., -675.,
				483., -40., -360., 362., 327., -200., 204., 67., 84., -159., -145., 23., -18., 69., 66., 5., 65., 66.,
				-36., -53., 51., -48., 42., -40., -5., 0., -1., -19., -11., 17., 20., -4., -4., -2., 9., 7., -9., -13.,
				-11., -10., 11., -97079., -1464., -1179., 3392., 1557., 933., -989., -754., 567., -470., 334., 210.,
				-17., -156., 157., -151., -87., 29., 36., -69., 10., 43., -8., 30., -39., 29., 2., 29., 29., -25.,
				-16., -23., 3., 22., -21., 18., -17., 13., -2., 14., -8., 0., -9., 0., -8., 9. };
		double f[] = new double[]
		{ 0.2299708345453799, 0.0019436907548255, 0.4579979783362081, 0.0324605575663244, -0.1955665862245038,
				0.4274811115247091, -0.2318206046403833, 0.6555082553155372, 0.2127688645204655, 0.2471728045705681,
				0.6860251221267625, 0.2604877013568789, 0.0496625275912389, -0.1783646161993155, 0.0172021241604381,
				0.0191456607800137, -0.2260834530360027, 0.4102791414995209, -0.0152582792703628, 0.4407960083110198,
				0.8835353991060917, 0.1994539677341547, 0.2662248529612594, 0.4751999483613963, 0.4427395449305955,
				-0.0037934608495551, 0.6383062852903491, 0.4637351299405887, 0.1937168161297741, 0.0152585875411362,
				-0.2127685562496920, 0.0000001541352498, -0.4598477484309377, 0.9140522659175908, 0.2452292679512662,
				0.6249913885040383, 0.2300338378809035, -0.2299078312098563, 0.4446830815498973, 0.8530185322948665,
				0.4885148451477070, 0.0381977091707050, 0.2147124011397673, 0.2299708345453799, 0.0019436907548255,
				0.2308957195928816, 0.4579979783362081, 0.0324605575663244, -0.1955665862245038, 0.4274811115247091,
				-0.0028685758023272, -0.2318206046403833, 0.6555082553155372, 0.2127688645204655, 0.2471728045705681,
				0.1946417011770021, 0.6860251221267625, 0.4589228633837098, 0.2604877013568789, 0.0496625275912389,
				-0.1783646161993155, -0.0333854426135524, 0.0172021241604381, 0.0191456607800137, -0.2260834530360027,
				0.4102791414995209, -0.0152582792703628, 0.4284059965722108, 0.4407960083110198, 0.8835353991060917,
				0.1994539677341547, 0.2662248529612594, 0.4751999483613963, 0.4427395449305955, -0.0037934608495551,
				0.6383062852903491, 0.4637351299405887, 0.1937168161297741, 0.6564331403627652, 0.0152585875411362,
				0.1774397311520876, -0.2127685562496920, 0.0000001541352498, -0.4598477484309377, 0.9140522659175908,
				0.2452292679512662, 0.6249913885040383, 0.4446830815498973, 0.6869500071742641, 0.8530185322948665,
				0.4885148451477070, 0.2251585679885010, 0.2299708345453799, 0.2308957195928816, 0.0019436907548255,
				0.4579979783362081, -0.0028685758023272, 0.0324605575663244, -0.1955665862245038, 0.1946417011770021,
				0.4274811115247091, 0.4589228633837098, -0.0333854426135524, -0.2318206046403833, 0.6555082553155372,
				0.2127688645204655, 0.2471728045705681, 0.4284059965722108, 0.6860251221267625, 0.2604877013568789,
				0.0496625275912389, -0.1783646161993155, 0.0172021241604381, 0.6564331403627652, 0.0191456607800137,
				-0.2260834530360027, 0.1774397311520876, 0.4102791414995209, -0.0152582792703628, 0.6869500071742641,
				0.4407960083110198, 0.2251585679885010, 0.8835353991060917, 0.1994539677341547, 0.4226688449678302,
				0.2662248529612594, 0.4751999483613963, 0.4427395449305955, -0.0037934608495551, 0.2118436712021903,
				0.6383062852903491, -0.0505874126387406, -0.2614125864043805, 0.4637351299405887, 0.0200705458272416,
				0.1937168161297741, 0.0143333942228611, -0.0181270092079398 };

		double t = JD - Constant.J2000;

		for (int iv = 0; iv < 3; iv++)
		{
			v[iv] = 0.0;
			for (int n = n1[iv] - 1; n < n2[iv]; n++)
			{
				double x = f[n] * t;
				double cx = Math.cos(x);
				double sx = Math.sin(x);
				v[iv] = v[iv] + c[n] * cx + s[n] * sx;
			}
			v[iv] = v[iv] / 1.0e10;
			r[iv] = v[iv];
		}

		return r;

	}

	/**
	 * Calculate ephemeris, providing full data. This method uses Series96
	 * theory from the IMCCE, valid between 1900 and 2100. Typical error is
	 * about 0.001 arcseconds or below, when comparing to JPL Ephemeris DE403.
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
	public static EphemElement series96Ephemeris(TimeElement time, // Time
																	// Element
			ObserverElement obs, // Observer Element
			EphemerisElement eph) // Ephemeris Element
			throws JPARSECException
	{
		if ((!eph.targetBody.isPlanet() && eph.targetBody != TARGET.SUN && eph.targetBody != TARGET.Pluto) || (obs.getMotherBody() == TARGET.EARTH && (eph.targetBody == TARGET.EARTH || eph.targetBody == TARGET.Earth_Moon_Barycenter)))
			throw new JPARSECException("target object is invalid.");

		// Check Ephemeris object
		if (!EphemerisElement.checkEphemeris(eph))
			throw new JPARSECException("invalid ephemeris object.");

		// Obtain julian day in Barycentric Dynamical Time
		double JD_TDB = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);

		// Obtain geocentric position
		double geo_eq[] = Series96.getGeocentricPosition(JD_TDB, eph.targetBody, 0.0, true, obs);

		// Obtain topocentric light_time
		LocationElement loc = LocationElement.parseRectangularCoordinates(geo_eq);
		double light_time = loc.getRadius() * Constant.LIGHT_TIME_DAYS_PER_AU;
		if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.GEOMETRIC)
			light_time = 0.0;
		if (eph.ephemType != EphemerisElement.COORDINATES_TYPE.GEOMETRIC && eph.targetBody != TARGET.SUN)
		{
			double topo[] = obs.topocentricObserverICRF(time, eph);
			geo_eq = Series96.getGeocentricPosition(JD_TDB, eph.targetBody, light_time, true, obs);
			double light_time_corrected = Ephem.getTopocentricLightTime(geo_eq, topo, eph);
			// Iterate to obtain correct light time and geocentric position.
			// Typical differente in light time is 0.1 seconds. Iterate to
			// a precission up to 1E-6 seconds.
			do
			{
				light_time = light_time_corrected;
				geo_eq = Series96.getGeocentricPosition(JD_TDB, eph.targetBody, light_time_corrected, true, obs);
				light_time_corrected = Ephem.getTopocentricLightTime(geo_eq, topo, eph);
			} while (Math.abs(light_time - light_time_corrected) > (1.0E-6 / Constant.SECONDS_PER_DAY));
			light_time = light_time_corrected;
		}

		// Obtain heliocentric ecliptic coordinates, mean equinox of date
		double helio_object[] = Series96.getHeliocentricEquatorialPositionJ2000(JD_TDB - light_time, eph.targetBody);
		Object o = DataBase.getData("offsetPosition", true);
		if (o != null) {
			double[] planetocentricPositionOfTargetSatellite = (double[]) o;
			helio_object = Functions.sumVectors(helio_object, planetocentricPositionOfTargetSatellite);
		}

		// Correct for solar deflection and aberration
		if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT)
		{
			double earth[] = Series96.getGeocentricPosition(JD_TDB, TARGET.SUN, 0.0, false, obs);
			if (eph.preferPrecisionInEphemerides)
				//geo_eq = Ephem.solarDeflection(geo_eq, earth, helio_object);
				geo_eq = Ephem.solarAndPlanetaryDeflection(geo_eq, earth, helio_object,
					new TARGET[] {TARGET.JUPITER}, JD_TDB, false, obs);

			geo_eq = Ephem.aberration(geo_eq, earth, light_time);
			DataBase.addData("GCRS", geo_eq, true);
		} else {
			DataBase.addData("GCRS", null, true);
		}

		/* Correction to output frame. */
		geo_eq = Ephem.toOutputFrame(geo_eq, FRAME.ICRF, eph.frame);
		helio_object = Ephem.toOutputFrame(helio_object, FRAME.ICRF, eph.frame);

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
			ephem_elem = Ephem.toOutputEquinox(ephem_elem, eph, JD_TDB);

		ephem_elem.name = eph.targetBody.getName();

		return ephem_elem;
	}
};

/* A set of values (one block) in the Series96 files */
class Series96_set
{
	Series96_set()
	{
		NF = new int[4];
		MX = 0;
		IMAX = 0;
		IBLOCK = 0;
		TDEB = 0.0;
		DT = .0;
		TFIN = 0.0;
		FQ = new double[4][300];
		SEC = new double[4][4];
		CT = new double[4][4][300];
		ST = new double[4][4][300];
	};

	int NF[];
	int MX;
	int IMAX;
	int IBLOCK;
	double TDEB;
	double DT;
	double TFIN;
	double FQ[][];
	double SEC[][];
	double CT[][][];
	double ST[][][];
}
