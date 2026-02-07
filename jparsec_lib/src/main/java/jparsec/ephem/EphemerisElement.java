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

import java.io.Serializable;

import jparsec.ephem.Target.TARGET;
import jparsec.ephem.planets.OrbitalElement;
import jparsec.math.Constant;
import jparsec.util.JPARSECException;

/**
 * Support class to perform ephemerides calculations. This class provides
 * methods and fields that can be passed to any ephemeris calculation method to
 * obtain certain kind of ephemeris results. Output ephemeris type ({@linkplain
 * EphemerisElement#ephemType}), output equinox ({@linkplain EphemerisElement#equinox}), ephem methods to apply
 * ({@linkplain EphemerisElement#ephemMethod}), topocentric or geocentric ephemeris ({@linkplain
 * EphemerisElement#isTopocentric}), frame of the results ({@linkplain EphemerisElement#frame}), and of
 * course the target body of the ephemeris ({@linkplain EphemerisElement#targetBody}) are some of the
 * selectable parameters. These fields are lowercase, while uppercase parameters
 * in the corresponding enums are possible values of each other.
 *
 * The types of ephemerides are geometric, astrometric, and apparent.
 *
 * Geometric positions refer to the true positions of the body in the Solar
 * System, without light-time or any other corrections. Sometimes used to obtain
 * mean coordinates referred to certain equinox.
 *
 * Astrometric positions are those corrected for light-time and stellar aberration, but not for
 * planetary aberration, nutation, or deflection. They are mean positions referred to
 * certain equinox (J2000, equinox of the date, or any other) that can be
 * compared to catalog positions (or published sky maps such as Sky Atlas or Uranometria) referred to the same epoch.
 *
 * Apparent positions are the positions of the object in the sky as seen by the
 * observer. Typically topocentric, but geocentric apparent position can be
 * obtained as well.
 *
 * Reference frames available are dynamical J2000, ICRF, FK5, and FK4. ICRF is the new
 * reference frame officially adopted by IAU, recommended in the
 * IERS Conventions 2003. The use of the ICRF frame should include the IAU2000/2006/2009
 * formulae for precession, nutation, and Greenwich mean sidereal time. Note
 * that the official ephemeris adopted by the IAU are JPL DE406, and the 'apparent place'
 * of a planet is defined by the IAU to correspond to a position referred to the
 * true equator and equinox of date, which means that the positions should be apparent
 * respect the equinox of date, and based on the dynamical equinox of J2000 (see USNO
 * circular 179). So some sources such as the Astronomical Almanac gives position using the
 * dynamical equinox of J2000 as frame, although other sources uses ICRF. Difference is 0.01".
 * If you are in doubt, dynamical equinox of J2000 is recommended.
 *
 * Note also that all these selections depends on each other, and incorrect
 * decisions could give results different from other sources. On the other
 * hand, some other sources sometimes makes a mixture of precise algorimths with
 * classical ones (IAU 1980 theory of nutation, or even the old Lieske precession
 * algorithm, which is still available here to allow matching results with other sources). Note
 * that the new precession theory by Capitaine et al. 2003 is the one
 * recommended in the report of the IAU Division I Working Group on Precession
 * and the Ecliptic (Hilton et al. 2006, Celest. Mech., 94, 351-367). The
 * recommendation is to adopt this theory replacing the inconsistent precession
 * part of the IAU2000A precession-nutation model (2006). Here, we have
 * directly applied Capitaine et al. theory for IAU2006 and beyond, although
 * support to the original IAU2000 model is still given.
 *
 * This library provides results with and accuracy up to the milliarcsecond
 * level when comparing to JPL ephemerides, using JPL Ephemerides algorithms
 * and selecting the adequate ephemeris parameters. The accuracy of other methods decreases when
 * comparing to the new long time span ephemeris JPL DE406, but it is generally
 * in the arcsecond level. Accuracy is similar in the other calculations
 * methods, respect to the corresponding fit to JPL ephemeris version (DE200 in
 * VSOP and ELP theories). Results from Moshier are well below the arcsecond
 * level, even when comparing to JPL DE406, but the discrepancy is greater (not
 * too much) for Pluto in ancient dates. The limited accuracy of the rectangular coordinates
 * obtained with each theory is the cause of the subsequent discrepancies.
 *
 * Please read the documentation for the different algorithms to apply and methods
 * for reducing coordinates in the corresponding parameters to choose an adequate
 * and consistent ephemeris object.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class EphemerisElement implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor. It defines an Ephemeris object by selecting Sun as
	 * target body, equinox of date for the results, topocentric coordinates,
	 * IAU2006 methods, apparent coordinates, and dynamical equinox J2000 as
	 * frame. Ephemeris algorithm is set to Moshier.
	 */
	public EphemerisElement()
	{
		targetBody = TARGET.SUN;
		equinox = EphemerisElement.EQUINOX_OF_DATE;
		isTopocentric = true;
		frame = FRAME.DYNAMICAL_EQUINOX_J2000;
		ephemMethod = EphemerisElement.REDUCTION_METHOD.IAU_2006;
		ephemType = EphemerisElement.COORDINATES_TYPE.APPARENT;
		algorithm = ALGORITHM.MOSHIER;
	}

	/**
	 * Explicit constructor for planets. It defines an Ephemeris object by
	 * giving the values of the different fields. Ephemeris algorithm is set to
	 * Moshier.
	 *
	 * @param planet Planet ID constant.
	 * @param ephem_type Can be {@linkplain COORDINATES_TYPE#APPARENT}, {@linkplain
	 *        COORDINATES_TYPE#ASTROMETRIC}, or {@linkplain COORDINATES_TYPE#GEOMETRIC}.
	 * @param equinox Can be {@linkplain EphemerisElement#EQUINOX_J2000}, {@linkplain
	 *        EphemerisElement#EQUINOX_OF_DATE}, or any positive Julian day.
	 * @param topocentric True for topocentric results, false for geocentric.
	 * @param apply_method {@linkplain REDUCTION_METHOD#JPL_DE4xx}, {@linkplain
	 *        REDUCTION_METHOD#WILLIAMS_1994}, {@linkplain REDUCTION_METHOD#SIMON_1994},
	 *        {@linkplain REDUCTION_METHOD#IAU_1976}, {@linkplain REDUCTION_METHOD#LASKAR_1986}, {@linkplain REDUCTION_METHOD#IAU_2000},
	 *        or any other later IAU methods.
	 * @param frame Reference frame.
	 */
	public EphemerisElement(TARGET planet, COORDINATES_TYPE ephem_type, double equinox, boolean topocentric, REDUCTION_METHOD apply_method,
			FRAME frame)
	{
		targetBody = planet;
		this.equinox = equinox;
		isTopocentric = topocentric;
		ephemMethod = apply_method;
		ephemType = ephem_type;
		this.frame = frame;
		algorithm = ALGORITHM.MOSHIER;
	}

	/**
	 * Explicit constructor for any object. It defines an Ephemeris object by
	 * giving the values of most of the fields.
	 *
	 * @param planet Planet ID constant.
	 * @param ephem_type Can be {@linkplain COORDINATES_TYPE#APPARENT}, {@linkplain
	 *        COORDINATES_TYPE#ASTROMETRIC}, or {@linkplain COORDINATES_TYPE#GEOMETRIC}.
	 * @param equinox Can be {@linkplain EphemerisElement#EQUINOX_J2000}, {@linkplain
	 *        EphemerisElement#EQUINOX_OF_DATE}, or any positive Julian day.
	 * @param topocentric True for topocentric results, false for geocentric.
	 * @param apply_method {@linkplain REDUCTION_METHOD#JPL_DE4xx}, {@linkplain
	 *        REDUCTION_METHOD#WILLIAMS_1994}, {@linkplain REDUCTION_METHOD#SIMON_1994},
	 *        {@linkplain REDUCTION_METHOD#IAU_1976}, {@linkplain REDUCTION_METHOD#LASKAR_1986}, {@linkplain REDUCTION_METHOD#IAU_2000},
	 *        or any other later IAU method.
	 * @param frame Reference frame.
	 * @param algorithm Algorithm to apply.
	 */
	public EphemerisElement(TARGET planet, COORDINATES_TYPE ephem_type, double equinox, boolean topocentric, REDUCTION_METHOD apply_method,
			FRAME frame, ALGORITHM algorithm)
	{
		targetBody = planet;
		this.equinox = equinox;
		isTopocentric = topocentric;
		ephemMethod = apply_method;
		ephemType = ephem_type;
		this.frame = frame;
		this.algorithm = algorithm;
	}

	/**
	 * Explicit constructor for any object. It defines an Ephemeris object by
	 * giving the values of all the fields.
	 *
	 * @param planet Planet ID constant.
	 * @param ephem_type Can be {@linkplain COORDINATES_TYPE#APPARENT}, {@linkplain
	 *        COORDINATES_TYPE#ASTROMETRIC}, or {@linkplain COORDINATES_TYPE#GEOMETRIC}.
	 * @param equinox Can be {@linkplain EphemerisElement#EQUINOX_J2000}, {@linkplain
	 *        EphemerisElement#EQUINOX_OF_DATE}, or any positive Julian day.
	 * @param topocentric True for topocentric results, false for geocentric.
	 * @param apply_method {@linkplain REDUCTION_METHOD#JPL_DE4xx}, {@linkplain
	 *        REDUCTION_METHOD#WILLIAMS_1994}, {@linkplain REDUCTION_METHOD#SIMON_1994},
	 *        {@linkplain REDUCTION_METHOD#IAU_1976}, {@linkplain REDUCTION_METHOD#LASKAR_1986}, {@linkplain REDUCTION_METHOD#IAU_2000},
	 *        or any other later IAU method.
	 * @param frame Reference frame.
	 * @param algorithm Algorithm to apply.
	 * @param orbit Orbital Element set.
	 */
	public EphemerisElement(TARGET planet, COORDINATES_TYPE ephem_type, double equinox, boolean topocentric, REDUCTION_METHOD apply_method,
			FRAME frame, ALGORITHM algorithm, OrbitalElement orbit)
	{
		targetBody = planet;
		this.equinox = equinox;
		isTopocentric = topocentric;
		ephemMethod = apply_method;
		ephemType = ephem_type;
		this.frame = frame;
		this.algorithm = algorithm;
		this.orbit = orbit;
	}

	/**
	 * Planet ID for ephemeris, probe index value when calculation orbits or
	 * probes, or satellite index for orbits of artificial satellites.
	 */
	public TARGET targetBody;

	/**
	 * Set equinox of the results as a Julian day in Terrestrial Time.
	 */
	public double equinox;

	/**
	 * True for topocentric ephemeris, false for geocentric.
	 */
	public boolean isTopocentric;

	/**
	 * Methods for Greenwich Mean Sidereal Time, Precession, and Obliquity.
	 * Please read the documentation in each value to choose the adequate one.
	 */
	public REDUCTION_METHOD ephemMethod;

	/**
	 * Defines the frame reference of the results.
	 */
	public FRAME frame;

	/**
	 * Type of output coordinates. You should choose apparent coordinates almost always.
	 */
	public COORDINATES_TYPE ephemType;

	/**
	 * Algorithm to apply for the calculation. This field is necessary when
	 * calling in a general way to {@linkplain Ephem#getEphemeris(jparsec.time.TimeElement, jparsec.observer.ObserverElement, EphemerisElement, boolean)},
	 * which is the recommendend procedure. Please read the documentation in each parameter to choose the adequate one.
	 */
	public ALGORITHM algorithm;

	/**
	 * Holds the orbital elements of a given body.
	 */
	public OrbitalElement orbit;

	/**
	 * Constant for selecting equinox of date results. Select this unless you
	 * want to do something special.
	 */
	public static final double EQUINOX_OF_DATE = -1.0E9;

	/**
	 * Constant for selecting equinox of J2000 as the reference equinox for
	 * results.
	 */
	public static final double EQUINOX_J2000 = Constant.J2000;

	/**
	 * The different set of reduction algorithms that can be applied in JPARSEC.
	 */
	public enum REDUCTION_METHOD {
		/**
		 * Constant ID for selecting IAU 2000 formulae of precession, obliquity,
		 * nutation, and Greenwich mean sidereal time. Pole movement is considered
		 * when possible. Planetary rotation models are set to IAU 2000 resolutions.
		 * @deprecated It is not recommended since the precession is inconsistent.
		 */
		@Deprecated
		IAU_2000,
		/**
		 * Constant ID for selecting IAU 2006 formulae of obliquity, nutation, and
		 * Greenwich mean sidereal time. Note that the precession algorithm is from
		 * Capitaine et al. 2003 (Astronomy &amp; Astrophysics 412, 567-586, 2003), officially
		 * adopted by the IAU as a replacement of the precession part of IAU2000A
		 * precession-nutation model. See also Hilton et al. 2006. Pole movement is
		 * considered when possible. Should be used by default for better precission
		 * in modern theories like JPL DE405 and above. Planetary rotation models are
		 * set to IAU 2006 resolutions.
		 */
		IAU_2006,
		/**
		 * Same as IAU2006, but planetary rotation models are those recommended by
		 * the IAU working group on cartographic coordinates, in 2009.
		 */
		IAU_2009,
		/**
		 * Same as IAU2006, but planetary rotation models are those recommended by
		 * the IAU working group on cartographic coordinates, in 2015.
		 */
		IAU_2015,
		/**
		 * Constant ID for selecting JPL DE403/404/405/406 formulae for precession,
		 * obliquity, nutation (IAU 1980), and Greenwich mean sidereal time. Quite
		 * similar to Williams formulae. Adequate for planets using Moshier
		 * method, Series96, or JPL DE40x ephemerides.
		 */
		JPL_DE4xx,
		/**
		 * Constant ID for selecting Williams formulae of precession (DE403 JPL
		 * Ephemeris), nutation (IAU 1980), obliquity, and Greenwich mean sidereal
		 * time. See James G. Williams, "Contributions to the Earth's obliquity rate,
		 * precession, and nutation," Astron. J. 108, 711-724 (1994). It is convenient
		 * to use this when obtaining ephemeris of the Moon using Moshier method.
		 */
		WILLIAMS_1994,
		/**
		 * Constant ID for selecting SIMON formulae of precession, obliquity,
		 * nutation (IAU 1980), and Greenwich mean sidereal time. See
		 * J. L. Simon, P. Bretagnon, J. Chapront, M. Chapront-Touze', G. Francou,
		 * and J. Laskar, "Numerical Expressions for precession formulae and mean
		 * elements for the Moon and the planets," Astronomy and Astrophysics 282,
		 * 663-683 (1994).
		 */
		SIMON_1994,
		/**
		 * Constant ID for selecting Laskar formulae of precession, nutation (IAU
		 * 1980), and Greenwich mean sidereal time. See J. Laskar,
		 * "Secular terms of classical planetary theories using the results of
		 * general theory," Astronomy and Astrophysics 157, 59070 (1986).
		 */
		LASKAR_1986,
		/**
		 * Constant ID for selecting IAU 1976 formulae of precession, nutation (IAU
		 * 1980), and Greenwich mean sidereal time. This will use
		 * old formulae that will match results from IMCCE ephemeris server. You
		 * may consider using this formulae for VSOP theory. See
		 * J. H. Lieske, T. Lederle, W. Fricke, and B. Morando, "Expressions for the
		 * Precession Quantities Based upon the IAU (1976) System of Astronomical
		 * Constants," Astronomy and Astrophysics 58, 1-16 (1977).
		 */
		IAU_1976;
		
		/** 
		 * Returns if this reduction method is a IAU method from 2000 or later.
		 * @return True or false.
		 */
		public boolean isIAU20xx() {
			return this.name().startsWith("IAU_20");
		}
		
		/**
		 * Returns the latest method available.
		 * @return Latest available.
		 */
		public static REDUCTION_METHOD getLatest() {
			return REDUCTION_METHOD.IAU_2015;
		}
	};


	/**
	 * Array with the method names, ordered as the same way as the individual
	 * variables are set.
	 */
	public static final String[] REDUCTION_METHODS = new String[] {"IAU 2000", "IAU 2006", "IAU 2009",
		"IAU 2015", "JPL DE4xx", "Williams", "Simon", "Laskar", "IAU 1976"};

	/**
	 * The set of coordinate types.
	 */
	public enum COORDINATES_TYPE {
		/**
		 * Constant ID for apparent ephemeris calculation, as seen from Earth surface.
		 * If no special ephemeris calculation is required, select this.
		 */
		APPARENT,
		/**
		 * Constant ID for astrometric ephemeris calculation, corrected for light-time
		 * and stellar aberration (Earth speed), but not planetary aberration (object
		 * speed in its movement around the Sun), neither nutation. They are mean positions
		 * that can be compared to maps or charts refered to the same equinox and epoch.
		 */
		ASTROMETRIC,
		/**
		 * Constant ID for geometric ephemeris calculation, the true positions of the
		 * objects not corrected for aberration, nutation, or light-time.
		 */
		GEOMETRIC
		};
	/**
	 * Array with the ephem type names, ordered as the same way as the individual
	 * variables are set.
	 */
	public static final String[] COORDINATE_TYPES = new String[] {"Apparent", "Astrometric",
		"Geometric"};

	/**
	 * Value for topocentric results.
	 */
	public static final boolean TOPOCENTRIC = true;

	/**
	 * Value for geocentric results.
	 */
	public static final boolean GEOCENTRIC = false;

	/**
	 * The set of output frames.
	 */
	public enum FRAME {
		/**
		 * Value ID for ICRF reference frame. An offset is conveniently applied to
		 * transform from mean dynamical frame. This correction is
		 * convenient for apparent true positions in the sky refered to the ICRS
		 * system. If you are in doubt, DO NOT SELECT ICRF.
		 */
		ICRF,
		/**
		 * Value ID for dynamical equinox of J2000 frame, adequate (in combination with
		 * other options like apparent positions and equinox of date) to obtain ephemerides
		 * for the true equator and equinox of date. The difference with ICRF is about 0.02".
		 * If you are in doubt, DO SELECT THIS.
		 */
		DYNAMICAL_EQUINOX_J2000,
		/**
		 * ID for selecting old FK5 frame in output positions. Select this in
		 * combination with astrometric positions (and a given epoch, like J2000)
		 * to obtain planetary positions that can be compared to stellar catalogs in
		 * FK5 frame.
		 */
		FK5,
		/**
		 * ID for selecting very old FK4 frame in output positions.
		 */
		FK4
		};

	/**
	 * Array with the frame names, ordered as the same way as the individual
	 * variables are set.
	 */
	public static final String[] FRAMES = new String[] {"ICRF",
		"Dynamical equinox", "FK5", "FK4"};

	/**
	 * The set of available algorithms. JPL ephemerides requires the ASCII ephemerides
	 * files, many of which are available for the 20th and 21st centuries (jpl_ephem.jar).
	 * You can also use external files for the entire time range covered by each integration.
	 */
	public enum ALGORITHM {
		/** JPL DE438 ephemeris algorithm only available from external files. */
		JPL_DE438,
		/**
		 * ID value for JPL DE431 ephemeris algorithm.
		 * Available from 13000 bc to 17000 ad only from external files.
		 */
		JPL_DE431,
		/**
		 * ID value for JPL DE430 ephemeris algorithm.
		 * Available from 1900 to 2100.
		 */
		JPL_DE430,
		/**
		 * ID value for JPL DE424 ephemeris algorithm.
		 * Available from 1900 to 2100.
		 */
		JPL_DE424,
		/**
		 * ID value for JPL DE422 ephemeris algorithm.
		 * Available from 1900 to 2100.
		 */
		JPL_DE422,
		/**
		 * ID value for JPL DE414 ephemeris algorithm.
		 * Available from 1950 to 2050.
		 */
		JPL_DE414,
		/**
		 * ID value for JPL DE413 ephemeris algorithm.
		 * Available from 1950 to 2050.
		 */
		JPL_DE413,
		/**
		 * ID value for JPL DE406 ephemeris algorithm.
		 * Available from 1900 to 2100. If you want maximum precission you
		 * should consider using this formulae, since it has good
		 * time span, acceptable performance, and is the officially adopted by IAU.
		 * Available from 3000 B.C. to 3100 from external files.
		 */
		JPL_DE406,
		/**
		 * ID value for JPL DE405 ephemeris algorithm.
		 * Available from 1940 to 2060 in JPARSEC, or from
		 * 1600 to 2200 from external files.
		 */
		JPL_DE405,
		/**
		 * ID value for JPL DE403 ephemeris algorithm.
		 * Available from 1950 to 2050.
		 */
		JPL_DE403,
		/**
		 * ID value for JPL DE200 ephemeris algorithm.
		 * Available from 1940 to 2060.
		 */
		JPL_DE200,
		/**
		 * ID value for Moshier ephemeris algorithm. Good precission in ancient times
		 * for the planets (but not the Moon), but other algorithms are better for 20th and 21st centuries.
		 * You may consider using ELP2000 for the Moon instead of this,
		 * especially in ancient times, despite that ELP2000 has poor performance.
		 */
		MOSHIER,
		/**
		 * ID value for VSOP87 or ELP2000 ephemeris algorithm. Good set of algorithms for
		 * planets and the Moon, but Moshier offers better performance and precission in planets
		 * in ancient dates.
		 */
		VSOP87_ELP2000ForMoon,
		/**
		 * ID value for Series96 (20th and 21st centuries) or Moshier Moon ephemeris algorithm.
		 * You may consider using JPL DE406 instead.
		 */
		SERIES96_MOSHIERForMoon,
		/**
		 * ID value for Newcomb ephemeris algorithm. Target object should be the Sun.
		 */
		NEWCOMB,
		/**
		 * ID value for ephemeris of general objects given the orbital elements.
		 */
		ORBIT,
		/**
		 * ID value for probes ephemeris algorithm.
		 */
		PROBE,
		/**
		 * ID value for natural satellites algorithm.
		 */
		NATURAL_SATELLITE,
		/**
		 * ID value for artificial satellites ephemeris algorithm.
		 */
		ARTIFICIAL_SATELLITE,
		/**
		 * ID value for star ephemeris algorithm.
		 */
		STAR;
		
		/**
		 * Return if this is a JPL algorithm.
		 * @return True or false.
		 */
		public boolean isJPL() {
			if (this.name().startsWith("JPL")) return true;
			return false;
		}
	};

	/**
	 * The different set of observing wavelengths. This will affect objects positions
	 * corrected by refraction.
	 */
	public enum OBSERVING_WAVELENGTH {
		/** Optical wavelengths. Default value, Bennet (1982) formulae
		 * for refraction will be used. Classical formulae commonly used,
		 * but less accurate. See Journal of Navigation (Royal Institute) 35,
		 * 255-259, and also Explanatory Supplement to the Astronomical
		 * Almanac, p. 144. This formula is used for observed elevations
		 * between -4 and 90 deg. */
		OPTICAL_BENNET,
		/** Similar formulae as the Bennet for optical, but with parameters
		 * adapted to Yebes observatory at radio wavelengths. */
		RADIO_BENNET,
		/** Optical wavelengths. Yan (1996) formulae for refraction will be
		 * used, with an accuracy of 0.3". See also Magnum (2001),
		 * ALMA Memorandum 366, and the corrections/comments by NRAO at
		 * https://safe.nrao.edu/wiki/pub/Main/RefBendDelayCalc/RefBendDelayCalc.pdf.
		 * The observing wavelength in microns
		 * can be set in the corresponding variable of the enum constant,
		 * in case a value different from 0.555 microns is desired.
		 * This formula is used for observed elevations between -1 and 90 deg.  */
		OPTICAL_YAN,
		/** Radio wavelengths. Yan (1996) formulae for refraction will be
		 * used, with an accuracy of 0.3" below 100 GHz. See also Magnum (2001),
		 * ALMA Memorandum 366. Frequency range is from the GHz regime
		 * to the THz. Maximum error comparing with the atmospheric model by
		 * Liebe (1996) for ALMA site is 2% at 500 GHz, and it is
		 * around 1% for nu &gt; 300 GHz. Between 100 and 300 GHz the
		 * error is only 0.5%. This formula is used for observed elevations between
		 * -1 and 90 deg.  */
		RADIO_YAN,
		/**
		 * Calculates refraction for a given zenith distance and wavelength using
		 * the Hohenkerk and Sinclair method (NAO Technical Notes 59 and 63), slightly
		 * modified to account for refraction at radio wavelengths. The method is
		 * a Java implementation of the one present in the SLALIB package. This method
		 * should be more accurate than any of the others, but also requires more
		 * calculations.
		 */
		NUMERICAL_INTEGRATION;

		/**
		 * Defines the observing wavelength for the source. This affects the Yan formulae
		 * in the optical refraction case, which is designed to drop some terms for 0.532 microns,
		 * although the eye has its maximum sensitivity in 0.555 microns. Default value is
		 * 0.555 microns. In radio wavelengths refraction is independent from frequency.
		 * This value also affects the refraction for the numerical integration option. In case
		 * wavelength is greater than 100 microns the algorithm is adapted to account for
		 * refraction in radio wavelengths.
		 */
		public double observingWavelength = 0.555;

		/**
		 * Resets the observing wavelength to its default value
		 * of 0.555 microns.
		 */
		public void clear() {
			observingWavelength = 0.555;
		}
	}

	/**
	 * The observing wavelength. Default value is optical with Bennet (1982) refraction model.
	 */
	public OBSERVING_WAVELENGTH wavelength = OBSERVING_WAVELENGTH.OPTICAL_BENNET;

	/**
	 * True to correct output right ascension and declination for local refraction effects,
	 * and equinox based calculations, in case coordinates are topocentric and apparent.
	 * Default value is false.
	 */
	public boolean correctEquatorialCoordinatesForRefraction = false;

	/**
	 * Extinction correction flag for equinox based calculations. If set to true, in every ephemerides
	 * calculation with topocentric results and apparent coordinates the apparent
	 * magnitude will be corrected for extinction (only in that case, topocentric plus
	 * apparent). Default value is false to keep always the same value for the output
	 * magnitude.
	 */
	public boolean correctForExtinction = false;

	/**
	 * Sets the precession model to that of Vondr&aacute;k et al. 2011 (see A&amp;A 534, A22)
	 * instead of the original IAU2006 model by Capitaine et al. 2003. This affects the
	 * calculation of precession for reduction algorithms IAU2006 and IAU2009. The effect
	 * is a correct output for dates very far from J2000 epoch. IAU model should not be used
	 * 10 000 years far from J2000. Vondr&aacute;k parameterization allows to reach up to
	 * 200 000 years far from J2000, with an error in the degree level. Default value is
	 * equals to the value of {@linkplain #ALLOW_VONDRAK_PRECESSION}, which is true at startup.
	 * Set this value to false to use the original IAU model, or the mentioned static variable
	 * to use always the original IAU model. Only for equinox based calculations.
	 */
	public boolean useVondrak2011PrecessionFormulaInsteadOfIAU2006 = ALLOW_VONDRAK_PRECESSION;

	/**
	 * True (default value) to correct for pole motion using the Earth orientation parameters
	 * in equinox based calculations. Not supported in Android.
	 */
	public boolean correctForPolarMotion = true;

	/**
	 * Sets if Earth Orientation Parameters should be used to correct ephemerides
	 * for UT1 minus UTC time offset and the nutation components of the polar motion,
	 * in equinox based calculations. Default value is true. In case you change this value after
	 * computing ephemerides don't forget to call {@linkplain jparsec.observer.EarthOrientationParameters#clearEOP()}
	 * and {@linkplain Nutation#clearPreviousCalculation()}.
	 */
	public boolean correctForEOP = true;

	/**
	 * Sets if Earth Orientation Parameters should be corrected for diurnal and subdiurnal Earth
	 * tides, using the model by Ray 1994.
	 */
	public boolean correctEOPForDiurnalSubdiurnalTides = true;

	/**
	 * Sets the prefer precision flag in ephemeris calculations. Default is
	 * true to allow maximum precision. Set to false to avoid this, resulting in
	 * (maybe) faster ephemeris, but generally with enough precision. Note that
	 * if you calculate ephemerides with JPL algorithms in a given date the
	 * performance could be better compared to setting this flag to false (which
	 * forces the Series96/Moshier methods). When calling ephemeris through
	 * {@linkplain Ephem#getEphemeris(jparsec.time.TimeElement, jparsec.observer.ObserverElement, jparsec.ephem.EphemerisElement, boolean, boolean)}
	 * method the best possible algorithm will be used if this
	 * is set to true, although Moshier method is forced when JPL
	 * ephemeris files cannot be found and Series96 cannot be applied, so no
	 * dependency problems will appear, although the theory used could be
	 * different from the one selected in the ephemeris object.<BR>
	 * When this flag is set to false Moshier ephemerides can be used between
	 * years -3000 to +3000, with true the lower limit for inner planets is -1350.
	 */
	public boolean preferPrecisionInEphemerides = true;

	/**
	 * This flag controls the default value of the field {@linkplain #useVondrak2011PrecessionFormulaInsteadOfIAU2006}.
	 * Default value is true. Set to false to force original IAU2006 model when a new ephemeris
	 * object is instantiated. Inside JPARSEC this happens in some methods, so in case the original IAU 2006 model
	 * is required this flag should be set to false.
	 */
	public static boolean ALLOW_VONDRAK_PRECESSION = true;

	/**
	 * Returns a String representation of this object.
	 */
	@Override
	public String toString() {
		String out = "target: "+this.targetBody+", algorithm: "+this.algorithm+", eph method: "+this.ephemMethod+", eph type: "+this.ephemType+", frame: "+this.frame+", equinox: "+this.equinox+", topocentric: "+this.isTopocentric;
		return out;
	}

	/**
	 * Check if the Ephemeris object is valid or not. A very basic check is made for
	 * target body.
	 *
	 * @param eph Ephemeris object.
	 * @return True if it is adequate for calculations, false otherwise.
	 * @throws JPARSECException Thrown if the object is invalid.
	 */
	public static boolean checkEphemeris(EphemerisElement eph) throws JPARSECException
	{
		boolean is_correct = false;
		int check = 0;

		try
		{
			if (eph.equinox > 0 || eph.equinox == EphemerisElement.EQUINOX_OF_DATE)
				check++;
			if (eph.targetBody == TARGET.Nutation || eph.targetBody == TARGET.Libration ||
					eph.targetBody == TARGET.Solar_System_Barycenter)
				throw new JPARSECException("target body cannot be '"+eph.targetBody.getName()+"'.");
			if (eph.algorithm == ALGORITHM.NEWCOMB && eph.targetBody != TARGET.SUN)
				throw new JPARSECException("target body must be the Sun.");
			if (eph.algorithm == ALGORITHM.NATURAL_SATELLITE && !eph.targetBody.isNaturalSatellite())
				throw new JPARSECException("target body must be a natural satellite.");

			if (check == 1) is_correct = true;
		} catch (JPARSECException ve)
		{
			throw ve;
		}

		return is_correct;
	}

	/**
	 * Returns the epoch of the ephemeris object. The epoch
	 * is the equinox that ephemeris are calculate for. If this
	 * object has an equinox field different from equinox of date,
	 * output will be that value, otherwise it will be the input
	 * Julian day.
	 * @param jd Julian day of calculations.
	 * @return Julian day of the reference equinox of the results.
	 */
	public double getEpoch(double jd) {
		double out = jd;
		if (equinox != EQUINOX_OF_DATE) out = equinox;
		return out;
	}

	/**
	 * To clone the object.
	 */
	@Override
	public EphemerisElement clone()
	{
		// Create new EphemerisElement identical to the input
		EphemerisElement new_eph = new EphemerisElement(this.targetBody, this.ephemType, this.equinox,
				this.isTopocentric, this.ephemMethod, this.frame, this.algorithm, this.orbit);

		new_eph.targetBody.setIndex(this.targetBody.getIndex());
		new_eph.wavelength = this.wavelength;
		new_eph.wavelength.observingWavelength = this.wavelength.observingWavelength;
		new_eph.correctEOPForDiurnalSubdiurnalTides = this.correctEOPForDiurnalSubdiurnalTides;
		new_eph.correctEquatorialCoordinatesForRefraction = this.correctEquatorialCoordinatesForRefraction;
		new_eph.correctForEOP = this.correctForEOP;
		new_eph.correctForExtinction = this.correctForExtinction;
		new_eph.correctForPolarMotion = this.correctForPolarMotion;
		new_eph.useVondrak2011PrecessionFormulaInsteadOfIAU2006 = this.useVondrak2011PrecessionFormulaInsteadOfIAU2006;
		new_eph.preferPrecisionInEphemerides = this.preferPrecisionInEphemerides;
		return new_eph;
	}
	/**
	 * Returns if the input object is equals to this ephemeris object.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof EphemerisElement)) return false;

		EphemerisElement that = (EphemerisElement) o;

		if (Double.compare(that.equinox, equinox) != 0) return false;
		if (isTopocentric != that.isTopocentric) return false;
		if (correctEquatorialCoordinatesForRefraction != that.correctEquatorialCoordinatesForRefraction) return false;
		if (correctForExtinction != that.correctForExtinction) return false;
		if (useVondrak2011PrecessionFormulaInsteadOfIAU2006 != that.useVondrak2011PrecessionFormulaInsteadOfIAU2006)
			return false;
		if (correctForPolarMotion != that.correctForPolarMotion) return false;
		if (correctForEOP != that.correctForEOP) return false;
		if (correctEOPForDiurnalSubdiurnalTides != that.correctEOPForDiurnalSubdiurnalTides) return false;
		if (preferPrecisionInEphemerides != that.preferPrecisionInEphemerides) return false;
		if (targetBody != that.targetBody) return false;
		if (ephemMethod != that.ephemMethod) return false;
		if (frame != that.frame) return false;
		if (ephemType != that.ephemType) return false;
		if (algorithm != that.algorithm) return false;
		if (orbit != null ? !orbit.equals(that.orbit) : that.orbit != null) return false;

		return wavelength == that.wavelength;
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		result = targetBody != null ? targetBody.hashCode() : 0;
		temp = Double.doubleToLongBits(equinox);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (isTopocentric ? 1 : 0);
		result = 31 * result + (ephemMethod != null ? ephemMethod.hashCode() : 0);
		result = 31 * result + (frame != null ? frame.hashCode() : 0);
		result = 31 * result + (ephemType != null ? ephemType.hashCode() : 0);
		result = 31 * result + (algorithm != null ? algorithm.hashCode() : 0);
		result = 31 * result + (orbit != null ? orbit.hashCode() : 0);
		result = 31 * result + (wavelength != null ? wavelength.hashCode() : 0);
		result = 31 * result + (correctEquatorialCoordinatesForRefraction ? 1 : 0);
		result = 31 * result + (correctForExtinction ? 1 : 0);
		result = 31 * result + (useVondrak2011PrecessionFormulaInsteadOfIAU2006 ? 1 : 0);
		result = 31 * result + (correctForPolarMotion ? 1 : 0);
		result = 31 * result + (correctForEOP ? 1 : 0);
		result = 31 * result + (correctEOPForDiurnalSubdiurnalTides ? 1 : 0);
		result = 31 * result + (preferPrecisionInEphemerides ? 1 : 0);
		return result;
	}

	/**
	 * Optimizes the options in this instance to obtain the fastest possible ephemerides. Flags
	 * adjusted to false are {@linkplain #preferPrecisionInEphemerides}, {@linkplain #correctForEOP},
	 * and {@linkplain #correctForPolarMotion}.
	 */
	public void optimizeForSpeed() {
		this.preferPrecisionInEphemerides = this.correctForEOP = this.correctForPolarMotion = false;
	}
	/**
	 * Optimizes the options in this instance to obtain the most accurate possible ephemerides. Flags
	 * adjusted to true are {@linkplain #preferPrecisionInEphemerides}, {@linkplain #correctForEOP},
	 * and {@linkplain #correctForPolarMotion}.
	 */
	public void optimizeForAccuracy() {
		this.preferPrecisionInEphemerides = this.correctForEOP = this.correctForPolarMotion = true;
	}
}
