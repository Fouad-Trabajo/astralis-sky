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
package jparsec.io;

import java.io.Serializable;

import jparsec.graph.chartRendering.SkyRenderElement;

/**
 * An adequate class for establishing the format of input files. Formats of
 * archives of asteroids and comets are supported by default in the official
 * format of the Minor Planet Center and SkyMap software.
 * <P>
 * NOTE: The availability of the SkyMap format is only for our practical
 * purposes, since it is a popular commercial program used by astronomers. This
 * does not mean any particular interest at all.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 * @see ReadFile
 */
public class FileFormatElement implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Define a {@linkplain FileFormatElement} object by giving the initial index of the field, the
	 * ending index, and a name for the field. Positions should be stored in the
	 * common way as they are given in the description of the format in external
	 * sources, starting at the position 1 for the first character in the
	 * record. In Java, the starting position is usually 0.
	 *
	 * @param a Start of field in the file record.
	 * @param b End position of the field.
	 * @param field Name or description.
	 */
	public FileFormatElement(int a, int b, String field)
	{
		startingPosition = a;
		endingPosition = b;
		fieldName = field;
	}

	/**
	 * Holds the stating position of the field.
	 */
	public int startingPosition;

	/**
	 * Holds the ending position of the field.
	 */
	public int endingPosition;

	/**
	 * Holds the name or descripcion of the field.
	 */
	public String fieldName;

	/**
	 * Returns a clone of this instance;
	 */
	@Override
	public FileFormatElement clone()
	{
		return new FileFormatElement(this.startingPosition, this.endingPosition, this.fieldName);
	}
	/**
	 * Returns if the input object is equals to this instance.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof FileFormatElement)) return false;

		FileFormatElement that = (FileFormatElement) o;

		if (startingPosition != that.startingPosition) return false;
		if (endingPosition != that.endingPosition) return false;

		return !(fieldName != null ? !fieldName.equals(that.fieldName) : that.fieldName != null);
	}

	@Override
	public int hashCode() {
		int result = startingPosition;
		result = 31 * result + endingPosition;
		result = 31 * result + (fieldName != null ? fieldName.hashCode() : 0);
		return result;
	}
	/**
	 * A {@linkplain FileFormatElement} object suitable for reading asteroids in the MPC format.
	 * During the reading process angles are adequate transfomed to radians.
	 * <BR>
	 *
	 * <pre>
	 * <B>
	 * Columns    Field Name                  Meaning</B>
	 *
	 * 1 -    7   PROVISIONAL_DESIGNATION     Number or provisional designation in packed form
	 * 9 -   13   ABSOLUTE_MAGNITUDE          Absolute magnitude, H
	 * 15 -  19   MAGNITUDE_SLOPE             Slope parameter, G
	 * 21 -  25   EPOCH                       Epoch in packed form, .0 TT
	 * 27 -  35   MEAN_ANOMALY                Mean anomaly at the epoch, in degrees
	 * 38 -  46   ARGUMENT_OF_PERIHELION      Argument of perihelion, J2000.0 (degrees)
	 * 49 -  57   ASCENDING_NODE_LONGITUDE    Longitude of the ascending node, J2000.0 (degrees)
	 * 60 -  68   INCLINATION                 Inclination to the ecliptic, J2000.0 (degrees)
	 * 71 -  79   ECCENTRICITY                Orbital eccentricity
	 * 81 -  91   MEAN_MOTION                 Mean daily motion (degrees per day)
	 * 93 - 103   SEMIMAJOR_AXIS              Semimajor axis (AU)
	 * 106        UNCERTAINTY_PARAMETER       Uncertainty parameter, U
	 *                                        If this column contains `E' it indicates
	 *                                        that the orbital eccentricity was assumed.
	 *                                        For one-opposition orbits this column can
	 *                                        also contain `D' if a double (or multiple)
	 *                                        designation is involved or `F' if an e-assumed
	 *                                        double (or multiple) designation is involved.
	 *
	 * 108 - 116  REFERENCE                   Reference
	 * 118 - 122  NUMBER_OF_OBSERVATIONS      Number of observations
	 * 124 - 126  NUMBER_OF_OPOSITIONS        Number of oppositions
	 *
	 * For multiple-opposition orbits:
	 * 128 - 131  FIRST_OBS_OR_ARC_LENGTH     Year of first observation
	 * 132                                    '-'
	 * 133 - 136  LAST_OBS_OR_DAYS            Year of last observation
	 *
	 * For single-opposition orbits:
	 * 128 - 131  FIRST_OBS_OR_ARC_LENGTH     Arc length (days)
	 * 133 - 136  LAST_OBS_OR_DAYS            'days'
	 *
	 * 138 - 141  RMS                         r.m.s residual (&quot;)
	 * 143 - 145  COARSE_PERTURBERS           Coarse indicator of perturbers
	 *                                        (blank if unperturbed one-opposition object)
	 * 147 - 149  PRECISE_PERTURBERS          Precise indicator of perturbers
	 *                                        (blank if unperturbed one-opposition object)
	 * 151 - 160  COMPUTER NAME               Computer name
	 *
	 * There may sometimes be additional information beyond column 160
	 * as follows:
	 *
	 * 162 - 165                              4-hexdigit flags
	 *                                        The bottom 6 bits are used to encode a
	 *                                        value representing the orbit type (other
	 *                                        values are undefined):
	 *
	 *                                        Value
	 *                                        2  Aten
	 *                                        3  Apollo
	 *                                        4  Amor
	 *                                        5  Object with q &lt; 1.381 AU
	 *                                        6  Object with q &lt; 1.523 AU
	 *                                        7  Object with q &lt; 1.665 AU
	 *                                        8  Hilda
	 *                                        9  Jupiter Trojan
	 *                                       10  Centaur
	 *                                       14  Plutino
	 *                                       15  Other resonant TNO
	 *                                       16  Cubewano
	 *                                       17  Scattered disk
	 *
	 *                                        Additional information is conveyed by
	 *                                        adding in the following bit values:
	 *
	 *                                       64  Unused
	 *                                      128  Unused
	 *                                      256  Unused
	 *                                      512  Unused
	 *                                     1024  Unused
	 *                                     2048  Unused
	 *                                     4096  Unused
	 *                                     8192  1-opposition object seen at
	 *                                           earlier opposition
	 *                                    16384  Critical list numbered object
	 *                                    32768  Object is PHA
	 *
	 *
	 * 167 - 194  NAME                        Readable designation
	 *
	 * 195 - 202                              Date of last observation included in
	 *                                        orbit solution (YYYYMMDD format)
	 * </pre>
	 */
	public static final FileFormatElement MPC_ASTEROIDS_FORMAT[] =
	{ new FileFormatElement(1, 7, "PROVISIONAL_DESIGNATION"), new FileFormatElement(9, 13, "ABSOLUTE_MAGNITUDE"),
			new FileFormatElement(15, 19, "MAGNITUDE_SLOPE"), new FileFormatElement(21, 25, "EPOCH"),
			new FileFormatElement(27, 35, "MEAN_ANOMALY"), new FileFormatElement(38, 46, "ARGUMENT_OF_PERIHELION"),
			new FileFormatElement(49, 57, "ASCENDING_NODE_LONGITUDE"), new FileFormatElement(60, 68, "INCLINATION"),
			new FileFormatElement(71, 79, "ECCENTRICITY"), new FileFormatElement(81, 91, "MEAN_MOTION"),
			new FileFormatElement(93, 103, "SEMIMAJOR_AXIS"), new FileFormatElement(106, 106, "UNCERTAINTY_PARAMETER"),
			new FileFormatElement(108, 116, "REFERENCE"), new FileFormatElement(118, 122, "NUMBER_OF_OBSERVATIONS"),
			new FileFormatElement(124, 126, "NUMBER_OF_OPOSITIONS"), new FileFormatElement(128, 131, "FIRST_OBS_OR_ARC_LENGTH"),
			new FileFormatElement(133, 136, "LAST_OBS_OR_DAYS"), new FileFormatElement(138, 141, "RMS"),
			new FileFormatElement(143, 145, "COARSE_PERTURBERS"), new FileFormatElement(147, 149, "PRECISE_PERTURBERS"),
			new FileFormatElement(151, 160, "COMPUTER_NAME"), new FileFormatElement(167, 194, "NAME") };

	/**
	 * A {@linkplain FileFormatElement} object suitable for reading comets in the MPC format.
	 * <BR>
	 *
	 * <pre>
	 * <B>
	 * Columns    Field Name                  Meaning</B>
	 *
	 * 1 -   4    COMET_NUMBER                Periodic comet number
	 * 5          ORBIT_TYPE                  Orbit type (generally `C', `P' or `D')
	 * 6 -  12    PROVISIONAL_DESIGNATION     Provisional designation (in packed form)
	 * 15 -  18   PERIHELION_YEAR             Year of perihelion passage
	 * 20 -  21   PERIHELION_MONTH            Month of perihelion passage
	 * 23 -  29   PERIHELION_DAY              Day of perihelion passage (TT)
	 * 31 -  39   PERIHELION_DISTANCE         Perihelion distance (AU)
	 * 42 -  49   ECCENTRICITY                Orbital eccentricity
	 * 52 -  59   ARGUMENT_OF_PERIHELION      Argument of perihelion, J2000.0 (degrees)
	 * 62 -  69   ASCENDING_NODE_LONGITUDE    Longitude of the ascending node, J2000.0 (degrees)
	 * 72 -  79   INCLINATION                 Inclination in degrees, J2000.0 (degrees)
	 * 82 -  85   PERTURBED_YEAR              Year of epoch for perturbed solutions
	 * 86 -  87   PERTURBED_MONTH             Month of epoch for perturbed solutions
	 * 88 -  89   PERTURBED_DAY               Day of epoch for perturbed solutions
	 * 92 -  95   ABSOLUTE_MAGNITUDE          Absolute magnitude
	 * 97 - 100   MAGNITUDE_SLOPE             Slope parameter
	 * 103 -      NAME                        Name
	 * </pre>
	 */
	public static final FileFormatElement MPC_COMETS_FORMAT[] =
	{ new FileFormatElement(1, 4, "COMET_NUMBER"), new FileFormatElement(5, 5, "ORBIT_TYPE"),
			new FileFormatElement(6, 12, "PROVISIONAL_DESIGNATION"), new FileFormatElement(15, 18, "PERIHELION_YEAR"),
			new FileFormatElement(20, 21, "PERIHELION_MONTH"), new FileFormatElement(23, 29, "PERIHELION_DAY"),
			new FileFormatElement(31, 39, "PERIHELION_DISTANCE"), new FileFormatElement(42, 49, "ECCENTRICITY"),
			new FileFormatElement(52, 59, "ARGUMENT_OF_PERIHELION"), new FileFormatElement(62, 69, "ASCENDING_NODE_LONGITUDE"),
			new FileFormatElement(72, 79, "INCLINATION"), new FileFormatElement(82, 85, "PERTURBED_YEAR"),
			new FileFormatElement(86, 87, "PERTURBED_MONTH"), new FileFormatElement(88, 89, "PERTURBED_DAY"),
			new FileFormatElement(92, 95, "ABSOLUTE_MAGNITUDE"), new FileFormatElement(97, 100, "MAGNITUDE_SLOPE"),
			new FileFormatElement(103, 200, "NAME") };

	/**
	 * A {@linkplain FileFormatElement} object suitable for reading natural satellites in the MPC
	 * format.
	 * <BR>
	 *
	 * <PRE><B>
	 * Columns     Field Name                 Meaning</B>
	 *
	 *   1 -  12   PROVISIONAL_DESIGNATION    Number or provisional designation in packed form
	 *
	 *  14 -  18   EPOCH                      Epoch in packed form
	 *
	 *  20 -  32   REFERENCE_TIME             Time of perihelion (JDT)
	 *
	 *  34 -  42   ARGUMENT_OF_PERIHELION     Argument of perihelion, J2000.0 (degrees)
	 *  44 -  52   ASCENDING_NODE_LONGITUDE   Longitude of the ascending node, J2000.0 (degrees)
	 *  54 -  62   INCLINATION                Inclination to the ecliptic, J2000.0 (degrees)
	 *
	 *  64 -  72   ECCENTRICITY               Orbital eccentricity
	 *  74 -  82   PERIHELION_DISTANCE        Periapsis distance (AU)
	 *
	 *  84 -  85   CENTRAL_BODY               Central body (05 = Jupiter, 06 = Saturn, 07 = Uranus, 08 = Neptune)
	 *  87 -  91   ABSOLUTE_MAGNITUDE         Absolute magnitude, H
	 *
	 *  93 -  98   OBSERVED_ARC               Observed arc (days)
	 * 100 - 104   NUMBER_OF_OBSERVATION      Number of observations
	 *
	 * 106 - 108   ORBIT_COMPUTER             Orbit computer
	 * 110 - 118   REFERENCE                  Publication reference
	 *
	 * 120 - 124   FIRST_OBS                  Date (UT) of first included observation in packed form
	 * 126 - 130   LAST_OBS                   Date (UT) of last included observation in packed form
	 *
	 * 132 - 136   RMS                        r.m.s residual (")
	 * 138 - 140   COARSE_PERTURBERS          Coarse indicator of perturbers
	 * 142 - 144   PRECISE_PERTURBERS         Precise indicator of perturbers
	 *                                        (blank if unperturbed one-opposition object)
	 * 146 -       NAME                       Name of satellite
	 * </PRE>
	 */
	public static final FileFormatElement MPC_SATELLITES_FORMAT[] =
	{ new FileFormatElement(1, 12, "PROVISIONAL_DESIGNATION"), new FileFormatElement(14, 18, "EPOCH"),
			new FileFormatElement(20, 32, "REFERENCE_TIME"), new FileFormatElement(34, 42, "ARGUMENT_OF_PERIHELION"),
			new FileFormatElement(44, 52, "ASCENDING_NODE_LONGITUDE"), new FileFormatElement(54, 62, "INCLINATION"),
			new FileFormatElement(64, 72, "ECCENTRICITY"), new FileFormatElement(74, 82, "PERIHELION_DISTANCE"),
			new FileFormatElement(84, 85, "CENTRAL_BODY"), new FileFormatElement(87, 91, "ABSOLUTE_MAGNITUDE"),
			new FileFormatElement(93, 98, "OBSERVED_ARC"), new FileFormatElement(100, 104, "NUMBER_OF_OBSERVATION"),
			new FileFormatElement(106, 108, "ORBIT_COMPUTER"), new FileFormatElement(110, 118, "REFERENCE"),
			new FileFormatElement(120, 124, "FIRST_OBS"), new FileFormatElement(126, 130, "LAST_OBS"),
			new FileFormatElement(132, 136, "RMS"), new FileFormatElement(138, 140, "COARSE_PERTURBERS"),
			new FileFormatElement(142, 144, "PRECISE_PERTURBERS"), new FileFormatElement(146, 200, "NAME") };

	/**
	 * A {@linkplain FileFormatElement} object suitable for reading comets in the SkyMap format.
	 */
	public static final FileFormatElement SKYMAP_COMETS_FORMAT[] =
	{ new FileFormatElement(1, 47, "NAME"), new FileFormatElement(48, 51, "PERIHELION_YEAR"),
			new FileFormatElement(53, 54, "PERIHELION_MONTH"), new FileFormatElement(56, 62, "PERIHELION_DAY"),
			new FileFormatElement(64, 72, "PERIHELION_DISTANCE"), new FileFormatElement(80, 87, "ECCENTRICITY"),
			new FileFormatElement(89, 96, "ARGUMENT_OF_PERIHELION"), new FileFormatElement(98, 105, "ASCENDING_NODE_LONGITUDE"),
			new FileFormatElement(107, 114, "INCLINATION"), new FileFormatElement(116, 120, "ABSOLUTE_MAGNITUDE"),
			new FileFormatElement(123, 200, "MAGNITUDE_SLOPE") };

	/**
	 * A {@linkplain FileFormatElement} object suitable for reading asteroids in the SkyMap format.
	 */
	public static final FileFormatElement SKYMAP_ASTEROIDS_FORMAT[] =
	{ new FileFormatElement(1, 47, "NAME"), new FileFormatElement(48, 51, "PERIHELION_YEAR"),
			new FileFormatElement(53, 54, "PERIHELION_MONTH"), new FileFormatElement(56, 59, "PERIHELION_DAY"),
			new FileFormatElement(61, 68, "MEAN_ANOMALY"), new FileFormatElement(70, 78, "SEMIMAJOR_AXIS"),
			new FileFormatElement(80, 87, "ECCENTRICITY"), new FileFormatElement(89, 96, "ARGUMENT_OF_PERIHELION"),
			new FileFormatElement(98, 105, "ASCENDING_NODE_LONGITUDE"), new FileFormatElement(107, 114, "INCLINATION"),
			new FileFormatElement(116, 120, "ABSOLUTE_MAGNITUDE"), new FileFormatElement(123, 200, "MAGNITUDE_SLOPE") };

	/**
	 * A {@linkplain FileFormatElement} object suitable for reading stars in the BSC5 format
	 * (Hoffleit et al. 1991). This is and old catalogue, still useful, but the
	 * accuracy is improved in newer catalogues.
	 */
	public static final FileFormatElement BSC5_STARS_FORMAT[] =
	{ new FileFormatElement(1, 4, "HR"), new FileFormatElement(5, 14, "NAME"), new FileFormatElement(26, 31, "HD"),
			new FileFormatElement(32, 37, "SAO"), new FileFormatElement(38, 41, "FK5"), new FileFormatElement(45, 49, "ADS"),
			new FileFormatElement(52, 60, "VAR"), new FileFormatElement(76, 77, "RA_HOUR_J2000"),
			new FileFormatElement(78, 79, "RA_MIN_J2000"), new FileFormatElement(80, 83, "RA_SEC_J2000"),
			new FileFormatElement(84, 86, "DEC_DEG_J2000"), new FileFormatElement(87, 88, "DEC_MIN_J2000"),
			new FileFormatElement(89, 90, "DEC_SEG_J2000"), new FileFormatElement(103, 107, "MAG"),
			new FileFormatElement(149, 154, "RA_PM"), new FileFormatElement(155, 160, "DEC_PM"),
			new FileFormatElement(162, 166, "PARALLAX"), new FileFormatElement(167, 170, "RADIAL_VELOCITY"),
			new FileFormatElement(130, 147, "SPECTRUM") };

	/**
	 * A {@linkplain FileFormatElement} object suitable for reading stars in the JPARSEC format, a
	 * derived format based on Sky2000 Master Catalog, version 5 (Myers et al.
	 * 2006). This catalogue contains accurate data for 260 000 stars, up to
	 * magnitude 10, without wasting a lot of memory. Recommended for general
	 * use. The catalogue is complete up to magnitude 9.5. For magnitude 10 the estimate
	 * is 80% complete.
	 */
	public static final FileFormatElement JPARSEC_SKY2000_FORMAT[] =
	{ new FileFormatElement(1, 8, "NAME"), new FileFormatElement(10, 20, "RA"), new FileFormatElement(22, 32, "DEC"),
			new FileFormatElement(34, 41, "RA_PM"), new FileFormatElement(43, 50, "DEC_PM"), new FileFormatElement(52, 57, "PARALLAX"),
			new FileFormatElement(59, 63, "MAG"), new FileFormatElement(65, 65, "TYPE"), new FileFormatElement(67, 68, "SPECTRUM"),
			new FileFormatElement(70, 75, "RADIAL_VELOCITY"), new FileFormatElement(77, 80, "ID") ,
			new FileFormatElement(82, 250, "DATA")};

	/**
	 * A {@linkplain FileFormatElement} object suitable for reading stars in the original Sky2000
	 * Master Catalog format, versions 2-5 (Myers et al. 2002, 2006). Highly
	 * recomendable, but ocupes a lot of memory.
	 */
	public static final FileFormatElement SKY2000_STARS_FORMAT[] =
	{
			new FileFormatElement(1, 7, "SKY2000"),
			new FileFormatElement(28, 35, "SKYMAP"),
			new FileFormatElement(36, 43, "HD"),
			new FileFormatElement(44, 50, "SAO"),
			new FileFormatElement(51, 63, "BD"),
			new FileFormatElement(64, 67, "HR"),
			new FileFormatElement(68, 83, "WDS"),
//			new FileFormatElement(78, 82, "WDS_NUMBER_OF_COMPONENTS"),
			new FileFormatElement(84, 90, "PPM"),
			new FileFormatElement(99, 108, "NAME"),
			new FileFormatElement(109, 118, "VARIABLE_NAME"),
			new FileFormatElement(119, 120, "RA_HOUR_J2000"), // ICRS
			new FileFormatElement(121, 122, "RA_MIN_J2000"),
			new FileFormatElement(123, 129, "RA_SEC_J2000"),
			new FileFormatElement(130, 132, "DEC_DEG_J2000"),
			new FileFormatElement(133, 134, "DEC_MIN_J2000"),
			new FileFormatElement(135, 140, "DEC_SEG_J2000"),
			new FileFormatElement(150, 157, "RA_PM"), // s/y
			new FileFormatElement(158, 165, "DEC_PM"), // "/y
			new FileFormatElement(168, 173, "RADIAL_VELOCITY"), // km/s
			new FileFormatElement(176, 183, "PARALLAX"), // ", x 1000
			new FileFormatElement(233, 238, "MAG"),
			new FileFormatElement(239, 243, "MAG_DERIVED"), // when MAG inexistent
			new FileFormatElement(259, 264, "B-V"),
			new FileFormatElement(279, 284, "U-B"),
			new FileFormatElement(305, 334, "SPECTRUM"),
			new FileFormatElement(337, 338, "SPECTRUM_SIMPLE"),
			new FileFormatElement(342, 348, "SEP_OF_MAIN_COMPONENTS"), // "
			new FileFormatElement(349, 353, "MAG_DIF_A-B"),
			new FileFormatElement(354, 360, "ORBIT_PERIOD"), // yr
			new FileFormatElement(361, 363, "POSITION_ANGLE"), // deg
			new FileFormatElement(364, 370, "OBSERVATION_YEAR"), // yr
			new FileFormatElement(412, 416, "MAX_MAG"),
			new FileFormatElement(417, 421, "MIN_MAG"),
			new FileFormatElement(427, 427, "DMAG_BAND"),
			new FileFormatElement(428, 435, "VAR_PERIOD"), // d
			new FileFormatElement(436, 443, "VAR_EPOCH"), // JD - 2400000
			new FileFormatElement(444, 446, "VAR_TYPE"),
	};

	/**
	 * A {@linkplain FileFormatElement} object suitable for reading stars in the FK6 format (Wielen
	 * et al. 2000). This catalogue is not recommended for general use, since
	 * misses a lot of stars.
	 */
	public static final FileFormatElement FK6_STARS_FORMAT[] =
	{ new FileFormatElement(2, 6, "FK6"), new FileFormatElement(10, 15, "H"), new FileFormatElement(24, 42, "NAME"),
			new FileFormatElement(45, 46, "RA_HOUR_J2000"), new FileFormatElement(48, 49, "RA_MIN_J2000"),
			new FileFormatElement(51, 59, "RA_SEC_J2000"), new FileFormatElement(64, 66, "DEC_DEG_J2000"),
			new FileFormatElement(68, 69, "DEC_MIN_J2000"), new FileFormatElement(71, 78, "DEC_SEG_J2000"),
			new FileFormatElement(196, 199, "MAG"), new FileFormatElement(82, 90, "RA_PM"), new FileFormatElement(94, 102, "DEC_PM"),
			new FileFormatElement(161, 165, "PARALLAX"), new FileFormatElement(184, 191, "RADIAL_VELOCITY") };

	/**
	 * A {@linkplain FileFormatElement} object suitable for reading basic star information in the
	 * Hipparcos format (ESA, 1997). Note there is no information about radial
	 * velocity, so it is not recommended.
	 */
	public static final FileFormatElement HIPPARCOS_STARS_FORMAT[] =
	{ new FileFormatElement(9, 14, "H"), new FileFormatElement(436, 447, "SPECTRAL_TYPE"), new FileFormatElement(246, 251, "B-V"),
			new FileFormatElement(76, 77, "RA_ICRS_J1991.25"), new FileFormatElement(78, 79, "DEC_ICRS_J1991.25"),
			new FileFormatElement(42, 46, "MAG"), new FileFormatElement(88, 95, "RA_PM"), new FileFormatElement(97, 104, "DEC_PM"),
			new FileFormatElement(80, 86, "PARALLAX") };

	/**
	 * A {@linkplain FileFormatElement} object suitable for reading basic star information in the
	 * IRS format (Corbin et al. 1991). Note there is no information about
	 * radial velocity nor parallax, so it is not recommended.
	 */
	public static final FileFormatElement IRS_STARS_FORMAT[] =
	{ new FileFormatElement(2, 7, "IRS"), new FileFormatElement(87, 89, "SPECTRAL_TYPE"), new FileFormatElement(151, 152, "RA_HOUR_J2000"),
			new FileFormatElement(153, 154, "RA_MIN_J2000"), new FileFormatElement(155, 159, "RA_SEC_J2000"),
			new FileFormatElement(172, 174, "DEC_DEG_J2000"), new FileFormatElement(175, 176, "DEC_MIN_J2000"),
			new FileFormatElement(177, 180, "DEC_SEG_J2000"), new FileFormatElement(82, 86, "MAG"),
			new FileFormatElement(160, 165, "RA_PM"), new FileFormatElement(181, 186, "DEC_PM") };

	/**
	 * An adequate {@linkplain FileFormatElement} object to read line 1 of TLE (two line element)
	 * data.
	 * <BR>
	 *
     * <PRE>
     * Line 1
     * Column Name                    Description
     * 01                             Line Number of Element Data
     * 03-07  SAT_NUMBER              Satellite Number
     * 08     CLASSIFICATION          Classification (U=Unclassified)
     * 10-11  INTERNATIONAL_ID_YEAR   International Designator, last two digits of launch year,  2000+ if below 57.
     * 12-14  INTERNATIONAL_ID_NUMBER International Designator, launch number of the year
     * 15-17  INTERNATIONAL_ID_PIECE  International Designator, piece of the launch
     * 19-20  EPOCH_YEAR_LAST2DIGITS  Epoch Year, last two digits of year,  2000+ if below 57
     * 21-32  EPOCH_DAY               Epoch Day of the year and fractional portion of the day
     * 34-43  FIRST_DERIVATIVE        First Time Derivative of the Mean Motion (rev/day^2)
     * 45-52  SECOND_DERIVATIVE       Second Time Derivative of Mean Motion (decimal point assumed)
     * 54-61  BSTAR                   drag term (decimal point assumed)
     * 63     Ephemeris type
     * 65-68  Element number
     * 69     Checksum (Module 10)
     *        (Letters, blanks, periods, plus signs = 0; minus signs = 1)
     * </PRE>
	 */
	public static final FileFormatElement TLE_LINE1_FORMAT[] =
	{ new FileFormatElement(3, 7, "SAT_NUMBER"), new FileFormatElement(8, 8, "CLASSIFICATION"),
			new FileFormatElement(10, 11, "INTERNATIONAL_ID_YEAR"), new FileFormatElement(12, 14, "INTERNATIONAL_ID_NUMBER"),
			new FileFormatElement(15, 17, "INTERNATIONAL_ID_PIECE"), new FileFormatElement(19, 20, "EPOCH_YEAR_LAST2DIGITS"),
			new FileFormatElement(21, 32, "EPOCH_DAY"), new FileFormatElement(34, 43, "FIRST_DERIVATIVE"),
			new FileFormatElement(45, 52, "SECOND_DERIVATIVE"), new FileFormatElement(54, 61, "DRAG"),
			new FileFormatElement(63, 63, "EPHEMERIS_TYPE"), new FileFormatElement(65, 68, "ELEMENT_NUMBER") };

	/**
	 * An adequate {@linkplain FileFormatElement} object to read line 2 of TLE (two line element)
	 * data.
	 * <BR>
	 *
 	 * <PRE>
     * Line 2
     * Column Name                    Description
     * 01                             Line Number of Element Data
     * 03-07                          Satellite Number
     * 09-16                          Inclination [Degrees]
     * 18-25                          Right Ascension of the Ascending Node [Degrees]
     * 27-33                          Eccentricity (decimal point assumed)
     * 35-42                          Argument of Perigee [Degrees]
     * 44-51                          Mean Anomaly [Degrees]
     * 53-63                          Mean Motion [Revs per day]
     * 64-68                          Revolution number at epoch [Revs]
     * 69                             Checksum (Modulo 10)
     * </PRE>
	 */
	public static final FileFormatElement TLE_LINE2_FORMAT[] =
	{ new FileFormatElement(3, 7, "SAT_NUMBER"), new FileFormatElement(9, 16, "INCLINATION"),
			new FileFormatElement(18, 25, "ASCENDING_NODE_RA"), new FileFormatElement(27, 33, "ECCENTRICITY"),
			new FileFormatElement(35, 42, "ARGUMENT_OF_PERIGEE"), new FileFormatElement(44, 51, "MEAN_ANOMALY"),
			new FileFormatElement(53, 63, "MEAN_MOTION"), new FileFormatElement(64, 68, "REVOLUTION_NUMBER") };

	/**
	 * A {@linkplain FileFormatElement} suitable for reading telescopes from the provided external
	 * file scopes.txt.
	 */
	public static final FileFormatElement TELESCOPES[] =
	{ new FileFormatElement(1, 33, "NAME"), new FileFormatElement(34, 37, "DIAMETER"), new FileFormatElement(38, 50, "F") };

	/**
	 * A {@linkplain FileFormatElement} suitable for reading oculars from the provided external file
	 * eyepiece.txt.
	 */
	public static final FileFormatElement OCULARS[] =
	{ new FileFormatElement(1, 33, "NAME"), new FileFormatElement(34, 37, "FOCAL"), new FileFormatElement(39, 50, "FIELD") };

	/**
	 * A {@linkplain FileFormatElement} suitable for reading deep sky objects from the provided
	 * external file objects.txt.
	 */
	public static final FileFormatElement DEEP_SKY_OBJECTS[] =
	{ new FileFormatElement(1, 12, "NAME"), new FileFormatElement(14, 27, "TYPE"), new FileFormatElement(29, 39, "RA"),
		new FileFormatElement(41, 52, "DEC"), new FileFormatElement(54, 58, "MAG"), new FileFormatElement(60, 69, "SIZE_MAX"),
		new FileFormatElement(71, 80, "SIZE_MIN"), new FileFormatElement(82, 84, "PA"), new FileFormatElement(86, 300, "COMMENTS") };

	/**
	 * The format to read .sou files of radiosources from IRAM, using the constants for
	 * external catalogs defined in {@linkplain SkyRenderElement}.
	 */
	public static final FileFormatElement[] IRAM_SOU_FORMAT = new FileFormatElement[] {
			new FileFormatElement(1, 9, SkyRenderElement.EXTERNAL_CATALOG_FIELD_NAME1),
			new FileFormatElement(11, 12, SkyRenderElement.EXTERNAL_CATALOG_FIELD_COORDINATES_TYPE),
			new FileFormatElement(15, 18, SkyRenderElement.EXTERNAL_CATALOG_FIELD_EQUINOX_YEAR),
			new FileFormatElement(20, 21, SkyRenderElement.EXTERNAL_CATALOG_FIELD_RA_H),
			new FileFormatElement(23, 24, SkyRenderElement.EXTERNAL_CATALOG_FIELD_RA_M),
			new FileFormatElement(26, 32, SkyRenderElement.EXTERNAL_CATALOG_FIELD_RA_S),
			new FileFormatElement(36, 38, SkyRenderElement.EXTERNAL_CATALOG_FIELD_DEC_D),
			new FileFormatElement(40, 41, SkyRenderElement.EXTERNAL_CATALOG_FIELD_DEC_M),
			new FileFormatElement(43, 48, SkyRenderElement.EXTERNAL_CATALOG_FIELD_DEC_S),
			new FileFormatElement(53, 99, SkyRenderElement.EXTERNAL_CATALOG_FIELD_DETAILS)
	};

	/**
	 * The format of the orbital elements or double stars by Hartkopf 2010.
	 */
	public static final FileFormatElement DOUBLE_STARS_HARTKPF_2010[] = new FileFormatElement[] {
			new FileFormatElement(1, 9, "RA"), new FileFormatElement(10, 18, "DEC"), new FileFormatElement(20, 29, "WDS"),
			new FileFormatElement(31, 45, "NAME"), new FileFormatElement(46, 50, "ADS"), new FileFormatElement(52, 57, "HD"),
			new FileFormatElement(59, 64, "HIP"), new FileFormatElement(67, 71, "MAGP"), new FileFormatElement(74, 78, "MAGS"),
			new FileFormatElement(82, 92, "P"), new FileFormatElement(93, 93, "P_UNIT"), new FileFormatElement(106, 114, "A"),
			new FileFormatElement(115, 115, "A_UNIT"), new FileFormatElement(126, 133, "INCL"), new FileFormatElement(144, 151, "NODE"),
			new FileFormatElement(163, 174, "TP"), new FileFormatElement(175, 175, "TP_UNIT"), new FileFormatElement(188, 195, "EXC"),
			new FileFormatElement(206, 213, "LP"), new FileFormatElement(224, 227, "EQ"), new FileFormatElement(229, 232, "LAST"),
			new FileFormatElement(234, 234, "GRADE"), new FileFormatElement(236, 236, "NOTES"), new FileFormatElement(238, 245, "REF"),
			new FileFormatElement(247, 264, "PNG")
	};

	/**
	 * The format of the orbital elements or double stars by Worley 1983.
	 */
	public static final FileFormatElement DOUBLE_STARS_WORLEY_1983[] = new FileFormatElement[] {
			new FileFormatElement(2, 3, "RAH"), new FileFormatElement(5, 8, "RAM"), new FileFormatElement(11, 11, "DECS"),
			new FileFormatElement(12, 13, "DECD"), new FileFormatElement(15, 16, "DECM"), new FileFormatElement(17, 17, "NOTES"),
			new FileFormatElement(20, 25, "ADS"), new FileFormatElement(27, 31, "MAGP"), new FileFormatElement(32, 32, "P_VARIABLE"),
			new FileFormatElement(34, 38, "MAGS"), new FileFormatElement(39, 39, "S_VARIABLE"), new FileFormatElement(42, 50, "PERIOD"),
			new FileFormatElement(51, 57, "A"), new FileFormatElement(60, 65, "INCL"), new FileFormatElement(68, 73, "NODE"),
			new FileFormatElement(79, 79, "GRADE"), new FileFormatElement(133, 159, "NAME"), new FileFormatElement(175, 182, "TP"),
			new FileFormatElement(184, 189, "EXC"), new FileFormatElement(200, 203, "EQ"), new FileFormatElement(82, 85, "LAST"),
			new FileFormatElement(221, 262, "REF"), new FileFormatElement(192, 197, "LP")
	};

	/**
	 * An object defining the format for space probes in the JPARSEC database.
	 * <BR>
	 * The meaning of the columns are as follows:
	 * <BR>
	 *
	 * <pre>
	 * - Name of probe. A &quot;-&quot; and a number means the phase of the probe, which changes
	 * when the motor was active and the trajectory changed.
	 * - Type of orbit: e = elliptic, h == hyperbolic. Useless.
	 * - Inclination in degrees.
	 * - Longitude of ascending node in degrees.
	 * - Argument of perihelion in degrees.
	 * - Semimajor axis in AU.
	 * - Mean motion in degrees/day.
	 * - Eccentricity.
	 * - Mean anomaly in degrees.
	 * - Month/day/year of reference time. Format mm/dd/yyyy (TT).
	 * - Reference equinox of elements (3 fields, year, month, day, month=day=0 always).
	 * - Year, month, day (3 fields) of the initial active instant (launch from Earth, or new phase).
	 * - Year, month, day (3 fields) of the final active instant (TT).
	 * </pre>
	 */
	public static final FileFormatElement JPARSEC_PROBES_FORMAT[] =
	{ new FileFormatElement(1, 16, "NAME"), new FileFormatElement(17, 17, "ORBIT_TYPE"), new FileFormatElement(19, 27, "INCLINATION"),
			new FileFormatElement(29, 37, "ASCENDING_NODE_LONGITUDE"), new FileFormatElement(39, 49, "ARGUMENT_OF_PERIHELION"),
			new FileFormatElement(51, 59, "SEMIMAJOR_AXIS"), new FileFormatElement(61, 69, "MEAN_MOTION"),
			new FileFormatElement(71, 80, "ECCENTRICITY"), new FileFormatElement(82, 92, "MEAN_ANOMALY"),
			new FileFormatElement(94, 95, "REF_MONTH"), new FileFormatElement(97, 100, "REF_DAY"),
			new FileFormatElement(102, 105, "REF_YEAR"), new FileFormatElement(109, 112, "REFERENCE_EQUINOX"),
			new FileFormatElement(121, 124, "BEGIN_YEAR"), new FileFormatElement(126, 127, "BEGIN_MONTH"),
			new FileFormatElement(129, 133, "BEGIN_DAY"), new FileFormatElement(135, 138, "END_YEAR"),
			new FileFormatElement(140, 141, "END_MONTH"), new FileFormatElement(143, 200, "END_DAY"), };

	/**
	 * A {@linkplain FileFormatElement} object adequate to read Sun Spots database.
	 */
	public static FileFormatElement NOAA_GREENWICH_SOLAR_SPOTS[] =
	{
			new FileFormatElement(1, 4, "YEAR"),
			new FileFormatElement(5, 6, "MONTH"),
			new FileFormatElement(7, 8, "DAY"),
			new FileFormatElement(9, 12, "TIME"),
			new FileFormatElement(13, 20, "GREENWICH_GROUP_NUMBER"), // UNTIL 1975
			new FileFormatElement(13, 20, "NOAA_GROUP_NUMBER"), // FROM 1976
			new FileFormatElement(13, 20, "GROUP_NUMBER"), // (GENERIC)
			new FileFormatElement(23, 24, "GREENWICH_GROUP_TYPE"), // UNTIL 1981
			new FileFormatElement(21, 21, "GREENWICH_GROUP_NUMBER_SUFFIX"), // FROM
																		// 1982
			new FileFormatElement(22, 24, "NOAA_GROUP_TYPE"), // FROM 1982
			new FileFormatElement(36, 39, "UMBRAL_AREA"), // UNTIL 1981
			new FileFormatElement(41, 44, "SPOT_AREA"), new FileFormatElement(46, 50, "DISTANCE_TO_CENTRE"),
			new FileFormatElement(52, 56, "POSITION_ANGLE"), new FileFormatElement(58, 62, "LONGITUDE"),
			new FileFormatElement(64, 68, "LATITUDE"), new FileFormatElement(70, 74, "CENTRAL_MERIDIAN_DISTANCE"),
	};
}

