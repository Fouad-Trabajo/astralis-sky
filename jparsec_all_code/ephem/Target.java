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
package jparsec.ephem;

import java.util.ArrayList;

import jparsec.ephem.moons.MoonEphem;
import jparsec.ephem.planets.JPLEphemeris;
import jparsec.graph.DataSet;
import jparsec.observer.ObserverElement;
import jparsec.time.TimeElement;
import jparsec.util.JPARSECException;
import jparsec.util.Logger;
import jparsec.util.Logger.LEVEL;
import jparsec.util.Translate;
import jparsec.util.Translate.LANGUAGE;

/**
 * A common place to store target body IDs. This class is updated following new
 * IAU resolutions about Pluto.
 * <P>
 * Size parameters comes from the IAU2009 recommended values. Note that not all
 * known natural objects are listed here, but only those whose size or
 * rotational parameters are known. Otherwise, ephemeris can also be obtained
 * for objects not listed here (see
 * {@linkplain MoonEphem#calcAllJPLSatellites(TimeElement, ObserverElement, EphemerisElement)}).
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public final class Target
{
	// private constructor so that this class cannot be instantiated.
	private Target() {}

	/**
	 * The set of targets for JPARSEC ephemerides.
	 */
	public enum TARGET {
		// Object name, equatorial radius (km), polar radius (km), relative mass to the Sun (1, >= 1)
		
		// Main Solar System bodies, Sun must be the first
		/** The Sun */
		SUN (696000.0, 696000.0, 1.0, null, 0),
		/** Mercury */
		MERCURY (2440.53, 2438.26, 6023600.0, SUN, 1),
		/** Venus */
		VENUS (6051.8, 6051.8, 408523.71, SUN, 2),
		/** Earth */
		EARTH (6378.1366, 6356.7519, 332946.050895, SUN, 3),
		/** Mars */
		MARS (3396.19, 3376.2, 3098708.0, SUN, 5),
		/** Jupiter */
		JUPITER (71492.0, 66854.0, 1047.3486, SUN, 6),
		/** Saturn */
		SATURN (60268.0, 54364.0, 3497.898, SUN, 7),
		/** Uranus */
		URANUS (25559.0, 24973.0, 22902.98, SUN, 8),
		/** Neptune */
		NEPTUNE (24764.0, 24341.0, 19412.24, SUN, 9),
		/** Dwarf planet Pluto */
		Pluto (1188.3, 1188.3, 1.352E8, SUN, 10),
		/** The Moon */
		Moon (1737.4, 1737.4, 2.7068700387534E7, EARTH, 4),
		/** Special target for Earth-Moon barycenter */
		Earth_Moon_Barycenter (0.0, 0.0, 328900.5614, SUN, 80),
		
		// Generic comet/asteroid
		/** Generic comet */
		Comet (0.0, 0.0, 0.0, SUN, 74),
		/** Generic asteroid */
		Asteroid (0.0, 0.0, 0.0, SUN, 73),
		/** Generic NEO */
		NEO (0.0, 0.0, 0.0, SUN, 1275),
		
		// Special values available in some theories (JPL, Moshier)
		/** Special target for nutation, only for JPL ephemerides. */
		Nutation (0, 0, 0, EARTH, 1315),
		/** Special target for lunar librations, only for Moshier and JPL ephemerides. */
		Libration (0, 0, 0, SUN, 1316),
		/** Special target for Solar System barycenter, only for Moshier and JPL ephemerides. */
		Solar_System_Barycenter (0, 0, 0, SUN, 1317),
		
		// Natural satellites, Phobos must be the first and Proteus the last
		/** A satellite of Mars */
		Phobos (13.0, 9.1, 0.0, MARS, 82),
		/** A satellite of Mars */
		Deimos (7.8, 5.1, 0.0, MARS, 83),

		/** A satellite of Jupiter */
		Io (1829.4, 1815.7, 0.0, JUPITER, 84),
		/** A satellite of Jupiter */
		Europa (1562.6, 1559.5, 0.0, JUPITER, 85),
		/** A satellite of Jupiter */
		Ganymede (2631.2, 2631.2, 0.0, JUPITER, 86),
		/** A satellite of Jupiter */
		Callisto (2410.3, 2410.3, 0.0, JUPITER, 87),

		/** A satellite of Saturn */
		Mimas (207.8, 190.6, 0.0, SATURN, 88),
		/** A satellite of Saturn */
		Enceladus (256.6, 248.3, 0.0, SATURN, 89),
		/** A satellite of Saturn */
		Tethys (538.4, 526.3, 0.0, SATURN, 90),
		/** A satellite of Saturn */
		Dione (563.4, 559.6, 0.0, SATURN, 91),
		/** A satellite of Saturn */
		Rhea (765.0, 762.4, 0.0, SATURN, 92),
		/** A satellite of Saturn */
		Titan (2575.15, 2574.47, 0.0, SATURN, 93),
		/** A satellite of Saturn */
		Hyperion (180.1, 102.7, 0.0, SATURN, 94),
		/** A satellite of Saturn */
		Iapetus (745.7, 712.1, 0.0, SATURN, 95),

		/** A satellite of Uranus */
		Miranda (240.4, 232.9, 0.0, URANUS, 96),
		/** A satellite of Uranus */
		Ariel (581.1, 577.7, 0.0, URANUS, 97),
		/** A satellite of Uranus */
		Umbriel (584.7, 584.7, 0.0, URANUS, 98),
		/** A satellite of Uranus */
		Titania (788.9, 788.9, 0.0, URANUS, 99),
		/** A satellite of Uranus */
		Oberon (761.4, 761.4, 0.0, URANUS, 100),
		
		/** A satellite of Neptune */
		Triton (1352.6, 1352.6, 0.0, NEPTUNE, 101),
		/** A satellite of Neptune */
		Nereid (170.0, 170.0, 0.0, NEPTUNE, 102),
		
		/** A satellite of Pluto */
		Charon (606.0, 606.0, 1.352E8/0.1218, Pluto, 103),

		/** A satellite of Jupiter */
		Amalthea (125.0, 64.0, 0.0, JUPITER, 104),
		/** A satellite of Jupiter */
		Thebe (58.0, 42.0, 0.0, JUPITER, 105),
		/** A satellite of Jupiter */
		Adrastea (10.0, 7.0, 0.0, JUPITER, 106),
		/** A satellite of Jupiter */
		Metis (30.0, 17.0, 0.0, JUPITER, 107),
		/** A satellite of Jupiter */
		Himalia (85.0, 85.0, 0.0, JUPITER, 127),
		/** A satellite of Jupiter */
		Elara (40.0, 40.0, 0.0, JUPITER, 128),
		/** A satellite of Jupiter */
		Pasiphae (18.0, 18.0, 0.0, JUPITER, 129),
		/** A satellite of Jupiter */
		Sinope (14.0, 14.0, 0.0, JUPITER, 130),
		/** A satellite of Jupiter */
		Lysithea (12.0, 12.0, 0.0, JUPITER, 131),
		/** A satellite of Jupiter */
		Carme (15.0, 15.0, 0.0, JUPITER, 132),
		/** A satellite of Jupiter */
		Ananke (10.0, 10.0, 0.0, JUPITER, 133),
		/** A satellite of Jupiter */
		Leda (5.0, 5.0, 0.0, JUPITER, 134),

		/** A satellite of Saturn */
		Atlas (20.5, 9.4, 0.0, SATURN, 108),
		/** A satellite of Saturn */
		Prometheus (68.2, 28.2, 0.0, SATURN, 109),
		/** A satellite of Saturn */
		Pandora (52.2, 31.5, 0.0, SATURN, 110),
		/** A satellite of Saturn */
		Pan (17.2, 10.4, 0.0, SATURN, 111),
		/** A satellite of Saturn */
		Epimetheus (64.9, 53.0, 0.0, SATURN, 135),
		/** A satellite of Saturn */
		Janus (101.7, 76.3, 0.0, SATURN, 136),
		/** A satellite of Saturn */
		Telesto (16.3, 9.8, 0.0, SATURN, 137),
		/** A satellite of Saturn */
		Calypso (15.3, 6.3, 0.0, SATURN, 138),
		/** A satellite of Saturn */
		Helene (22.5, 19.6, 0.0, SATURN, 139),
		/** A satellite of Saturn */
		Phoebe (109.4, 101.8, 0.0, SATURN, 140),

		/** A satellite of Uranus */
		Cordelia (13.0, 13.0, 0.0, URANUS, 112),
		/** A satellite of Uranus */
		Ophelia (15.0, 15.0, 0.0, URANUS, 113),
		/** A satellite of Uranus */
		Cressida (31.0, 31.0, 0.0, URANUS, 114),
		/** A satellite of Uranus */
		Bianca (21.0, 21.0, 0.0, URANUS, 115),
		/** A satellite of Uranus */
		Desdemona (27.0, 27.0, 0.0, URANUS, 116),
		/** A satellite of Uranus */
		Juliet (42.0, 42.0, 0.0, URANUS, 117),
		/** A satellite of Uranus */
		Portia (54.0, 54.0, 0.0, URANUS, 118),
		/** A satellite of Uranus */
		Rosalind (27.0, 27.0, 0.0, URANUS, 119),
		/** A satellite of Uranus */
		Puck (77.0, 77.0, 0.0, URANUS, 120),
		/** A satellite of Uranus */
		Belinda (33.0, 33.0, 0.0, URANUS, 821),
		
		/** A satellite of Neptune */
		Naiad (29.0, 29.0, 0.0, NEPTUNE, 126),
		/** A satellite of Neptune */
		Thalassa (40.0, 40.0, 0.0, NEPTUNE, 125),
		/** A satellite of Neptune */
		Despina (74.0, 74.0, 0.0, NEPTUNE, 123),
		/** A satellite of Neptune */
		Galatea (79.0, 79.0, 0.0, NEPTUNE, 124),
		/** A satellite of Neptune */
		Larissa (104.0, 89.0, 0.0, NEPTUNE, 122),
		/** A satellite of Neptune */
		Proteus (218.0, 201.0, 0.0, NEPTUNE, 121),

		// Asteroids. Ceres must be the first
		/** Dwarf planet */
		Ceres (487.3, 446, 1.0/4.7E-10, SUN, 1307),
		/** An asteroid */
		Pallas (275.0, 238.0, 1.0E10, SUN, 1308),
		/** An asteroid */
		Vesta (289.0, 229.0, 1.0/1.3E-10, SUN, 143),
		/** An asteroid */
		Lutetia (62.0, 46.5, 0.0, SUN, 1309),
		/** An asteroid */
		Ida (26.8, 7.6, 0.0, SUN, 141),
		/** An asteroid */
		Eros (17.0, 5.5, 0.0, SUN, 144),
		/** An asteroid */
		Davida (180.0, 127.0, 0.0, SUN, 1310),
		/** An asteroid */
		Gaspra (9.1, 4.4, 0.0, SUN, 142),
		/** An asteroid */
		Steins (3.24, 2.04, 0.0, SUN, 1311),
		/** An asteroid */
		Itokawa (0.268, 0.104, 0.0, SUN, 1312),
		
		// Comets
		/** A comet */
		P9_Tempel_1 (3.7, 2.5, 0.0, SUN, 1313),
		/** A comet */
		P19_Borrelly (3.5, 3.5, 0.0, SUN, 1314),
		
		// Generic unknown body, must be the last item
		/** ID value for a generic 'not a planet' body, for instance a space probe. */
		NOT_A_PLANET (0, 0, 0, SUN, 81);

		/**
		 * The equatorial radius in km.
		 */
		public final double equatorialRadius;

		/**
		 * The polar radius in km.
		 */
		public final double polarRadius;

		/**
		 * The relative mass to the Sun (1), greater or equal to 1. Default values
		 * come from JPL DE405 ephemerides, and are used for correcting for
		 * light deflection. 0 used when it is unknown. Note this field is not final 
		 * since the mass of the objects are expected to change to more accurate 
		 * values with time, and can potentially be different in more recent 
		 * JPL integrations. For consistency it is possible to change these values 
		 * to those used in different JPL integrations, see 
		 * {@linkplain #setPlanetaryMassFromJPLEphemeris(JPLEphemeris)}.
		 */
		public double relativeMass;

		/**
		 * The central body
		 */
		public final TARGET centralBody;

		/**
		 * The index of the TARGET name
		 * It is resolved at run-time to either a Spanish or English name.
		 */
		private final int nameIndex;

		/**
		 * The id number (index) for a comet or asteroid.
		 */
		private int id = -1;

		TARGET(double eqR, double poR, double rM, TARGET centralBody, int nameIndex) {
			this.equatorialRadius = eqR;
			this.polarRadius = poR;
			this.relativeMass = rM;
			this.centralBody = centralBody;
			this.nameIndex = nameIndex;
		}

		/**
		 * Sets the index of a body, used to select a minor body
		 * when reading asteroids/comets. Default initial value
		 * for index is -1.
		 * @param i The index of the body. Should be &gt;= 0.
		 */
		public void setIndex(int i) {
			id = i;
		}

		/**
		 * Returns the index, or -1 if it is not set.
		 * @return The index.
		 */
		public int getIndex() {
			return id;
		}

		/**
		 * Check if the index is indeed a valid index of an object. The check is
		 * made for main objects suitable for ephemeris calculations, such as the
		 * Sun, the Moon, any planet except Earth, and Pluto. If the index
		 * is a valid one but is not an index of a given asteroid or a natural satellite
		 * (neither the Earth or its barycenter), true will be returned. If the index
		 * is equal to a generic Comet/Asteroid true will be returned since this is
		 * valid for that specific kind of calculations.
		 *
		 * @return True if it is a valid index, false otherwise.
		 */
		public boolean isValidObjectButNotASatelliteNeitherAnAsteroid()
		{
			return
				this.ordinal() >= TARGET.SUN.ordinal() &&
				this.ordinal() <= TARGET.Asteroid.ordinal() &&
				this != TARGET.Earth_Moon_Barycenter && this != TARGET.EARTH;
		}

		/**
		 * Check if the index is indeed a valid index of a natural satellite.
		 *
		 * @return True if it is a valid satellite, false otherwise. Note this method
		 * returns false for the Moon.
		 */
		public boolean isNaturalSatellite()
		{
			return
				this.ordinal() >= TARGET.Phobos.ordinal() &&
				this.ordinal() <= TARGET.Proteus.ordinal();
		}

		/**
		 * Check if the index is indeed a planet.
		 *
		 * @return True if it is a planet or the Sun, false otherwise.
		 */
		public boolean isPlanet()
		{
			return
				this.ordinal() >= TARGET.SUN.ordinal() &&
				this.ordinal() <= TARGET.NEPTUNE.ordinal();
		}

		/**
		 * Check if the index is indeed an asteroid (or comet).
		 *
		 * @return True if it is an asteroid, false otherwise.
		 */
		public boolean isAsteroid()
		{
			// Asteroid or comet !
			return
				this.ordinal() >= TARGET.Ceres.ordinal() &&
				this.ordinal() <= TARGET.NOT_A_PLANET.ordinal();
		}

		/**
		 * Obtain the name of an object in the default language.
		 * The language is either Spanish or English, and is set with {@link Translate#setDefaultLanguage(LANGUAGE) }
		 *
		 * @return A string with the name of the object.
		 */
		public String getName()
		{
			return getNames()[this.ordinal()];
		}

		/**
		 * Obtain the name of an object.
		 *
		 * @return A string with the name of the object.
		 */
		public String getEnglishName()
		{
			return getNames(LANGUAGE.ENGLISH)[this.ordinal()];
		}

		private String getNameForLanguage(final LANGUAGE lang)
		{
			try {
				return Translate.getEntry(nameIndex, lang);
			} catch (Exception exc) {
				exc.printStackTrace();
				String message = "Cannot translate body name " + this + " to language " + Translate.getDefaultLanguage() + ". This error should never happen!";
				Logger.log(LEVEL.ERROR, message);
				throw new RuntimeException(message);
			}
		}

		/**
		 * Returns flattening factor = (equatorial radius - polar radius ) /
		 * equatorial radius.
		 *
		 * @return Flattening factor. Set to 0 if the object size is unknown.
		 */
		public double getFlatteningFactor()
		{
			if (this.equatorialRadius == 0.0) return 0.0;

			return (this.equatorialRadius - this.polarRadius) / this.equatorialRadius;
		}

		/**
		 * Obtains the central body ID of certain object (natural satellite).
		 *
		 * @return The central body or SUN if it is not found.
		 */
		public TARGET getCentralBody()
		{
			return this.centralBody == null ? SUN : this.centralBody;
		}
		
		/**
		 * Use a given instance of {@linkplain JPLEphemeris} to obtain the values 
		 * used there for the relative mass of the planets (Mercury to Neptune plus Pluto)
		 * respect to the Sun. The values will be set to the field {@linkplain #relativeMass}.
		 * @param jpl The JPL ephemeris instance.
		 * @throws JPARSECException If an error occurs.
		 */
		public static void setPlanetaryMassFromJPLEphemeris(JPLEphemeris jpl) throws JPARSECException {
			try {
				TARGET jplTargets[] = new TARGET[] {TARGET.SUN, TARGET.MERCURY, TARGET.VENUS,
						TARGET.Earth_Moon_Barycenter, TARGET.MARS, TARGET.JUPITER, TARGET.SATURN, TARGET.URANUS,
						TARGET.NEPTUNE}; //, TARGET.Pluto};
				double gms[] = new double[jplTargets.length];
				for (int i=0;i<jplTargets.length;i++) {
					if (jplTargets[i] == TARGET.SUN) {
				        gms[i] = Double.parseDouble(jpl.getConstant("GMS"));
					} else {
						if (jplTargets[i] == TARGET.Earth_Moon_Barycenter) {
					        gms[i] = Double.parseDouble(jpl.getConstant("GMB"));
						} else {
					        gms[i] = Double.parseDouble(jpl.getConstant("GM"+jplTargets[i].ordinal()));
						}
					}
				}
				for (int i=1;i<jplTargets.length;i++) {
					jplTargets[i].relativeMass = gms[0] / gms[i];
				}
			} catch (Exception exc) {
				if (exc instanceof JPARSECException) throw (JPARSECException) exc;
				throw new JPARSECException(exc);
			}
		}
	}

	private static String[] populateStringArrayList(LANGUAGE language)
	{
		ArrayList<String> vS = new ArrayList<String>();
		for (TARGET target : TARGET.values()) {
			vS.add(target.getNameForLanguage(language));
		}

		String data[] = DataSet.arrayListToStringArray(vS);
		return data;
	}

	/**
	 * Returns the names of all available targets.
	 * @return All names.
	 */
	public static String[] getNames()
	{
		return populateStringArrayList(null);
	}

	/**
	 * Returns the names of all available targets for a given
	 * language.
	 * @param language The language.
	 * @return All names.
	 */
	public static String[] getNames(LANGUAGE language)
	{
		return populateStringArrayList(language);
	}

	/**
	 * Obtain the ID constant of an object.
	 *
	 * @param name The name of the object.
	 * @return Object ID, or the 'not a planet' constant if it is not found.
	 * @throws JPARSECException If an error occurs, for instance when the object
	 *  is null.
	 */
	public static TARGET getID(String name) throws JPARSECException
	{
		if (name == null || name.equals("")) throw new JPARSECException("null/empty String as input planet!");

		name = DataSet.capitalize(name, false);
		if (Translate.getDefaultLanguage() == LANGUAGE.ENGLISH) {
			try {
				TARGET out = TARGET.valueOf(name);
				return out;
			} catch (Exception exc) {}
		}

		for (LANGUAGE lang : LANGUAGE.values()) {
			String[] names = Target.getNames(lang);
			int index = DataSet.getIndexContaining(names, name);
			//if (index < 0) index = DataSet.getIndexStartingWith(names, name);
			if (index >= 0 && name.length() == names[index].length()) return TARGET.values()[index];
		}
		return TARGET.NOT_A_PLANET;
	}

	/**
	 * Obtain the ID constant of an object.
	 *
	 * @param name The name of the object in English.
	 * @return Object ID, or 'not a planet' constant if it is not found.
	 * @throws JPARSECException If an error occurs, for instance when the object
	 *  is null.
	 */
	public static TARGET getIDFromEnglishName(String name) throws JPARSECException
	{
		if (name == null || name.equals("")) throw new JPARSECException("null/empty String as input planet!");

		name = DataSet.capitalize(name, false);
		try {
			TARGET out = TARGET.valueOf(name);
			return out;
		} catch (Exception exc) {}

		String names[] = Target.getNames(LANGUAGE.ENGLISH);
		int index = DataSet.getIndex(names, name);
		//if (index < 0) index = DataSet.getIndexStartingWith(names, name);
		if (index >= 0) return TARGET.values()[index];

		return TARGET.NOT_A_PLANET;
	}

	/**
	 * Transforms a given latitude from planetocentric to planetogeodetic.
	 * @param lat Latitude in radians.
	 * @param target Target body.
	 * @return Planetogeodetic latitude.
	 */
	public static double planetocentricToPlanetogeodeticLatitude(double lat, TARGET target) {
		double shape = target.getFlatteningFactor(); // (equatorial - polar radius) / (equatorial radius)
		return Math.atan(Math.tan(lat) / Math.pow(1.0 - shape, 2.0));
	}

	/**
	 * Transforms a given latitude from planetogeodetic to planetocentric.
	 * @param lat Latitude in radians.
	 * @param target Target body.
	 * @return Planetocentric latitude.
	 */
	public static double planetogeodeticToPlanetocentricLatitude(double lat, TARGET target) {
		double shape = target.getFlatteningFactor(); // (equatorial - polar radius) / (equatorial radius)
		return Math.atan(Math.tan(lat) * Math.pow(1.0 - shape, 2.0));
	}
}
