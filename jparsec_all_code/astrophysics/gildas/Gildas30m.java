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
package jparsec.astrophysics.gildas;

import java.io.*;
import java.util.ArrayList;

import jparsec.graph.DataSet;
import jparsec.io.binaryFormat.ByteArrayConverter;
import jparsec.io.binaryFormat.Convertible;
import jparsec.io.image.HeaderElement;
import jparsec.util.JPARSECException;

/**
 * A class to read spectra (.30m files) in the GILDAS format. A class spectra file can have
 * any number of spectra and channels, but the observation number must be unique if
 * several spectra are read from a file. The possible values of some parameters (projection,
 * coordinates, velocity, and spectrum kind) are listed here as constants, and not with
 * enumeration, since some values for those constants have values like -1.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Gildas30m {

	/**
	 * Gildas value used as an identifier for projection none.
	 */
    public static final int PROJECTION_NONE = 0;
	/**
	 * Gildas value used as an identifier for projection gnomonic.
	 */
    public static final int PROJECTION_GNOMONIC = 1;
	/**
	 * Gildas value used as an identifier for projection ortho.
	 */
    public static final int PROJECTION_ORTHO = 2;
	/**
	 * Gildas value used as an identifier for projection azimutal.
	 */
    public static final int PROJECTION_AZIMUTHAL = 3;
	/**
	 * Gildas value used as an identifier for projection stereo.
	 */
    public static final int PROJECTION_STEREO = 4;
	/**
	 * Gildas value used as an identifier for projection Lambert.
	 */
    public static final int PROJECTION_LAMBERT = 5;
	/**
	 * Gildas value used as an identifier for projection Aitoff.
	 */
    public static final int PROJECTION_AITOFF = 6;
	/**
	 * Gildas value used as an identifier for projection radio.
	 */
    public static final int PROJECTION_RADIO = 7;

	/**
	 * Gildas value used as an identifier for unknown coordinates.
	 */
    public static final int COORDINATES_UNKNOWN = 1;
	/**
	 * Gildas value used as an identifier for equatorial coordinates.
	 */
    public static final int COORDINATES_EQUATORIAL = 2;
	/**
	 * Gildas value used as an identifier for galactic coordinates.
	 */
    public static final int COORDINATES_GALACTIC = 3;
	/**
	 * Gildas value used as an identifier for horizontal coordinates.
	 */
    public static final int COORDINATES_HORIZONTAL = 4;

	/**
	 * Gildas value used as an identifier for unknown velocity.
	 */
    public static final int VELOCITY_UNKNOWN = 0;
	/**
	 * Gildas value used as an identifier for LSR velocity.
	 */
    public static final int VELOCITY_LSR = 1;
	/**
	 * Gildas value used as an identifier for heliocentric velocity.
	 */
    public static final int VELOCITY_HELIOCENTRIC = 2;
	/**
	 * Gildas value used as an identifier for geocentric velocity.
	 */
    public static final int VELOCITY_EAR = 3;
	/**
	 * Gildas value used as an identifier for auto velocity.
	 */
    public static final int VELOCITY_AUTO = -1;
    /**
     * Gildas value used as an identifier for an spectrum.
     */
    public static final int KIND_SPECTRAL = 0;
    /**
     * Gildas value used as an identifier for a continuum observation.
     */
    public static final int KIND_CONTINUUM = 1;

	/**
	 * ID constant for this header parameter.
	 */
    public static final String NUM = "num";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String NUM_DESC = "Observation number";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String BLOCK = "block";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String BLOCK_DESC = "Number of block";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String VERSION = "version";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String VERSION_DESC = "Version of the spectrum";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String SOURCE = "source";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String SOURCE_DESC = "Source of the spectrum";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String LINE = "line";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String LINE_DESC = "Name of the line";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String TELES = "teles";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String TELES_DESC = "Backend of the telescope";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String LDOBS = "ldobs";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String LDOBS_DESC = "Date of observation";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String LDRED = "ldred";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String LDRED_DESC = "Date of reduction";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String OFF1 = "off1";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String OFF1_DESC = "Offset on X";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String OFF2 = "off2";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String OFF2_DESC = "Offset on Y";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String TYPEC = "typec";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String TYPEC_DESC = "Type of coordinates";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String KIND = "kind";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String KIND_DESC = "Type of data";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String QUALITY = "qual";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String QUALITY_DESC = "Quality of data";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String SCAN = "scan";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String SCAN_DESC = "Scan number";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String POSA = "posa";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String POSA_DESC = "Position angle";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String UT_TIME = "uttime";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String UT_TIME_DESC = "UT of observation";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String LST_TIME = "lsttim";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String LST_TIME_DESC = "LST of observation";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String AZIMUTH = "azimu";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String AZIMUTH_DESC = "Azimuth";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String ELEVATION = "elevat";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String ELEVATION_DESC = "Elevation";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String TAU = "tau";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String TAU_DESC = "Opacity";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String TSYS = "tsys";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String TSYS_DESC = "System temperature";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String INTEG = "integ";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String INTEG_DESC = "Integration time";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String EPOCH = "epoch";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String EPOCH_DESC = "Epoch of coordinates";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String LAMBDA = "lambda";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String LAMBDA_DESC = "Lambda";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String BETA = "beta";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String BETA_DESC = "Beta";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String LAMBDA_OFF = "lamboff";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String LAMBDA_OFF_DESC = "Offset in lambda";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String BETA_OFF = "betaoff";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String BETA_OFF_DESC = "Offset in beta";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String PROJECTION = "proj";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String PROJECTION_DESC = "Projection system";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String REF_FREQ = "rfreq";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String REF_FREQ_DESC = "Rest Frequency";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String NCHAN = "nchan";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String NCHAN_DESC = "Number of channels";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String REF_CHAN = "refchan";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String REF_CHAN_DESC = "Reference channel";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String FREQ_RESOL = "fresol";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String FREQ_RESOL_DESC = "Frequency resolution";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String FREQ_OFF = "freqoff";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String FREQ_OFF_DESC = "Frequency offset";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String VEL_RESOL = "vres";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String VEL_RESOL_DESC = "Velocity resolution";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String REF_VEL = "voff";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String REF_VEL_DESC = "Velocity at reference channel";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String BAD = "bad";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String BAD_DESC = "Blanking value";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String IMAGE = "image";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String IMAGE_DESC = "Image frequency";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String VEL_TYPE = "veltype";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String VEL_TYPE_DESC = "Type of velocity";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String DOPPLER= "doppler";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String DOPPLER_DESC = "Doppler correction";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String BEAM_EFF = "beameff";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String BEAM_EFF_DESC = "Beam efficiency";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String FORW_EFF = "forweff";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String FORW_EFF_DESC = "Forward efficiency";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String GAIN_IM = "gain_im";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String GAIN_IM_DESC = "Image/Signal gain ratio";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String H2OMM = "h2omm";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String H2OMM_DESC = "mm of water vapor";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String PAMB = "pamb";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String PAMB_DESC = "Ambient pressure";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String TAMB = "tamb";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String TAMB_DESC = "Ambient temperature";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String TATMSIG = "tatmsig";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String TATMSIG_DESC = "Atmosphere temperature in signal band";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String TCHOP = "tchop";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String TCHOP_DESC = "Chopper temperature";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String TCOLD = "tcold";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String TCOLD_DESC = "Cold temperature";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String TAUSIG = "tausig";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String TAUSIG_DESC = "Opacity in signal band";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String TAUIMA = "tauima";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String TAUIMA_DESC = "Opacity in image band";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String TATMIMG = "tauima";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String TATMIMG_DESC = "Atmosphere temp in image band";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String TREC = "trec";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String TREC_DESC = "Receiver temperature";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String MODE = "mode";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String MODE_DESC = "Calibration mode";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String FACTOR = "factor";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String FACTOR_DESC = "Applied calibration factor";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String ALTITUDE = "altitud";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String ALTITUDE_DESC = "Site elevation";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String COUNT1 = "count1";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String COUNT1_DESC = "Power of atmosphere";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String COUNT2 = "count2";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String COUNT2_DESC = "Power of chopper";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String COUNT3 = "count3";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String COUNT3_DESC = "Power of cold";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String LONOFF = "lonoff";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String LONOFF_DESC = "Longitude offset for sky measurement";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String LATOFF = "latoff";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String LATOFF_DESC = "Latitude offset for sky measurement";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String LON = "lon";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String LON_DESC = "Longitude of the observatory";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String LAT = "lat";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String LAT_DESC = "Latitude of the observatory";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String OTF_NDUMPS = "otfndum";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String OTF_NDUMPS_DESC = "Number of records";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String OTF_LEN_HEADER = "otflhea";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String OTF_LEN_HEADER_DESC = "Length of data header";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String OTF_LEN_DATA = "otfldat";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String OTF_LEN_DATA_DESC = "Length of line data";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String OTF_LEN_DUMP = "otfldum";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String OTF_LEN_DUMP_DESC = "Length of record";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String NPHASE = "nphase";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String NPHASE_DESC = "Number of phases";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String SWMODE = "swmode";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String SWMODE_DESC = "Switching mode";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String SWDECALAGE = "swdecal";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String SWDECALAGE_DESC = "Frequency offsets";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String SWDURATION = "swdurat";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String SWDURATION_DESC = "Time per phase";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String SWPOIDS = "swpoids";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String SWPOIDS_DESC = "Weight of each phase";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String SWLDECAL = "swldecal";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String SWLDECAL_DESC = "Lambda offsets of each phase";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String SWBDECAL = "swbdecal";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String SWBDECAL_DESC = "Beta offsets of each phase";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String MYVERSION = "myver";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String MYVERSION_DESC = "Internal version";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String SIGMA = "sigma";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String SIGMA_DESC = "RMS of the spectrum";
	/**
	 * ID constant for this header parameter.
	 */
    public static final String GAUSS = "gauss";
	/**
	 * Holds a description for this header parameter.
	 */
    public static final String GAUSS_DESC = "Gauss fitting parameters";

	/**
	 * Returns the corresponding key of a given parameter description. The math
	 * is performed using the current set of parameters defined in {@linkplain Gildas30m}.
	 * @param description The description of the key.
	 * @return The parameter key, or null if no match is found.
	 */
	public String getKey(String description)
	{
		String key = null;
		if (description.equals(Gildas30m.ALTITUDE_DESC)) key = Gildas30m.ALTITUDE;
		if (description.equals(Gildas30m.AZIMUTH_DESC)) key = Gildas30m.AZIMUTH;
		if (description.equals(Gildas30m.BAD_DESC)) key = Gildas30m.BAD;
		if (description.equals(Gildas30m.BEAM_EFF_DESC)) key = Gildas30m.BEAM_EFF;
		if (description.equals(Gildas30m.BETA_DESC)) key = Gildas30m.BETA;
		if (description.equals(Gildas30m.BETA_OFF_DESC)) key = Gildas30m.BETA_OFF;
		if (description.equals(Gildas30m.LATOFF_DESC)) key = Gildas30m.LATOFF;
		if (description.equals(Gildas30m.BLOCK_DESC)) key = Gildas30m.BLOCK;
		if (description.equals(Gildas30m.COUNT1_DESC)) key = Gildas30m.COUNT1;
		if (description.equals(Gildas30m.COUNT2_DESC)) key = Gildas30m.COUNT2;
		if (description.equals(Gildas30m.COUNT3_DESC)) key = Gildas30m.COUNT3;
		if (description.equals(Gildas30m.ELEVATION_DESC)) key = Gildas30m.ELEVATION;
		if (description.equals(Gildas30m.EPOCH_DESC)) key = Gildas30m.EPOCH;
		if (description.equals(Gildas30m.FACTOR_DESC)) key = Gildas30m.FACTOR;
		if (description.equals(Gildas30m.FORW_EFF_DESC)) key = Gildas30m.FORW_EFF;
		if (description.equals(Gildas30m.FREQ_OFF_DESC)) key = Gildas30m.FREQ_OFF;
		if (description.equals(Gildas30m.FREQ_RESOL_DESC)) key = Gildas30m.FREQ_RESOL;
		if (description.equals(Gildas30m.GAIN_IM_DESC)) key = Gildas30m.GAIN_IM;
		if (description.equals(Gildas30m.GAUSS_DESC)) key = Gildas30m.GAUSS;
		if (description.equals(Gildas30m.H2OMM_DESC)) key = Gildas30m.H2OMM;
		if (description.equals(Gildas30m.IMAGE_DESC)) key = Gildas30m.IMAGE;
		if (description.equals(Gildas30m.INTEG_DESC)) key = Gildas30m.INTEG;
		if (description.equals(Gildas30m.KIND_DESC)) key = Gildas30m.KIND;
		if (description.equals(Gildas30m.LAMBDA_DESC)) key = Gildas30m.LAMBDA;
		if (description.equals(Gildas30m.LAMBDA_OFF_DESC)) key = Gildas30m.LAMBDA_OFF;
		if (description.equals(Gildas30m.LONOFF_DESC)) key = Gildas30m.LONOFF;
		if (description.equals(Gildas30m.LON_DESC)) key = Gildas30m.LON;
		if (description.equals(Gildas30m.LAT_DESC)) key = Gildas30m.LAT;
		if (description.equals(Gildas30m.LDOBS_DESC)) key = Gildas30m.LDOBS;
		if (description.equals(Gildas30m.LDRED_DESC)) key = Gildas30m.LDRED;
		if (description.equals(Gildas30m.LINE_DESC)) key = Gildas30m.LINE;
		if (description.equals(Gildas30m.LST_TIME_DESC)) key = Gildas30m.LST_TIME;
		if (description.equals(Gildas30m.MODE_DESC)) key = Gildas30m.MODE;
		if (description.equals(Gildas30m.MYVERSION_DESC)) key = Gildas30m.MYVERSION;
		if (description.equals(Gildas30m.NCHAN_DESC)) key = Gildas30m.NCHAN;
		if (description.equals(Gildas30m.NPHASE_DESC)) key = Gildas30m.NPHASE;
		if (description.equals(Gildas30m.NUM_DESC)) key = Gildas30m.NUM;
		if (description.equals(Gildas30m.OFF1_DESC)) key = Gildas30m.OFF1;
		if (description.equals(Gildas30m.OFF2_DESC)) key = Gildas30m.OFF2;
		if (description.equals(Gildas30m.OTF_LEN_DATA_DESC)) key = Gildas30m.OTF_LEN_DATA;
		if (description.equals(Gildas30m.OTF_LEN_DUMP_DESC)) key = Gildas30m.OTF_LEN_DUMP;
		if (description.equals(Gildas30m.OTF_LEN_HEADER_DESC)) key = Gildas30m.OTF_LEN_HEADER;
		if (description.equals(Gildas30m.OTF_NDUMPS_DESC)) key = Gildas30m.OTF_NDUMPS;
		if (description.equals(Gildas30m.PAMB_DESC)) key = Gildas30m.PAMB;
		if (description.equals(Gildas30m.POSA_DESC)) key = Gildas30m.POSA;
		if (description.equals(Gildas30m.PROJECTION_DESC)) key = Gildas30m.PROJECTION;
		if (description.equals(Gildas30m.QUALITY_DESC)) key = Gildas30m.QUALITY;
		if (description.equals(Gildas30m.REF_CHAN_DESC)) key = Gildas30m.REF_CHAN;
		if (description.equals(Gildas30m.REF_FREQ_DESC)) key = Gildas30m.REF_FREQ;
		if (description.equals(Gildas30m.SCAN_DESC)) key = Gildas30m.SCAN;
		if (description.equals(Gildas30m.SIGMA_DESC)) key = Gildas30m.SIGMA;
		if (description.equals(Gildas30m.SOURCE_DESC)) key = Gildas30m.SOURCE;
		if (description.equals(Gildas30m.TAMB_DESC)) key = Gildas30m.TAMB;
		if (description.equals(Gildas30m.TATMSIG_DESC)) key = Gildas30m.TATMSIG;
		if (description.equals(Gildas30m.TAU_DESC)) key = Gildas30m.TAU;
		if (description.equals(Gildas30m.TAUIMA_DESC)) key = Gildas30m.TAUIMA;
		if (description.equals(Gildas30m.TAUSIG_DESC)) key = Gildas30m.TAUSIG;
		if (description.equals(Gildas30m.TCHOP_DESC)) key = Gildas30m.TCHOP;
		if (description.equals(Gildas30m.TCOLD_DESC)) key = Gildas30m.TCOLD;
		if (description.equals(Gildas30m.TELES_DESC)) key = Gildas30m.TELES;
		if (description.equals(Gildas30m.TREC_DESC)) key = Gildas30m.TREC;
		if (description.equals(Gildas30m.TSYS_DESC)) key = Gildas30m.TSYS;
		if (description.equals(Gildas30m.TYPEC_DESC)) key = Gildas30m.TYPEC;
		if (description.equals(Gildas30m.UT_TIME_DESC)) key = Gildas30m.UT_TIME;
		if (description.equals(Gildas30m.VEL_RESOL_DESC)) key = Gildas30m.VEL_RESOL;
		if (description.equals(Gildas30m.VEL_TYPE_DESC)) key = Gildas30m.VEL_TYPE;
		if (description.equals(Gildas30m.VERSION_DESC)) key = Gildas30m.VERSION;
		if (description.equals(Gildas30m.REF_VEL_DESC)) key = Gildas30m.REF_VEL;
		return key;
	}
    private int next_free_block;
    private String code;
    private int ilex;
    private int imex;
    private int next_free_entry;
    private byte index_arr[];
    private RandomAccessFile file;
    private Convertible convert;
    private ArrayList<String> ordered_index, ordered_kind, ordered_keys;
    private String index[];

    private void createIndex(int max)
    throws JPARSECException {
    	index = new String[next_free_entry - 1];
        ordered_index = new ArrayList<String>(next_free_entry);
        ordered_keys = new ArrayList<String>(next_free_entry);
        ordered_kind = new ArrayList<String>();
        int vals[] = new int[next_free_entry - 1];
        int maxVal = -1;
        if (max <= 0) max = next_free_entry;
        for(int i = 0; i < max - 1; i++)
        {
            HeaderElement aobj1[] = readHeader(i);
            index[i] = aobj1[1].getAsString();
            int val = aobj1[0].getAsInt();
            vals[i] = val;
            if (val > maxVal || maxVal == -1) maxVal = val;
            ordered_keys.add(aobj1[0].getAsString());
            ordered_kind.add(aobj1[11].getAsString());
        }
        for(int i = 0; i <= maxVal; i++)
        {
        	ordered_index.add("");
        }
        for(int i = 0; i < vals.length; i++)
        {
            ordered_index.set(vals[i], ""+i);
        }
    }


    /**
     * Returns a given spectrum.
     * @param i The index of the spectrum.
     * @return The spectrum data.
     * @throws JPARSECException If an error occurs.
     */
    public Spectrum30m getSpectrum(int i)
    throws JPARSECException {
        Spectrum30m spectrum = new Spectrum30m();
        if(index == null) createIndex(-1);
        int ii = new Integer(ordered_index.get(i));
        if (ii < 0) {
        	throw new JPARSECException("Cannot find spectrum #"+i+"!");
        }
        Integer integer = new Integer(index[ii]);
        readSections(integer, spectrum);
        spectrum = new Spectrum30m(spectrum.getTreeMap(), this.getHeader(i),
        		this.getData(i));
        return spectrum;
    }

    /**
     * Returns the list of spectrums in the current instance.
     * @param onlySpectral True to return only spectral observations.
     * @return The list of spectrum indexes.
     * @throws JPARSECException If an error occurs.
     */
    public int[] getListOfSpectrums(boolean onlySpectral)
    throws JPARSECException {
        if(index == null) createIndex(-1);
        Object keys[] = DataSet.arrayListToStringArray(ordered_keys);
        if (!onlySpectral) {
	        int key[] = new int[keys.length];
	        for (int i=0; i<keys.length; i++)
	        {
	        	key[i] = Integer.parseInt((String) keys[i]);
	        }
	        return key;
        } else {
            Object kinds[] = DataSet.arrayListToStringArray(ordered_kind);
            int nsp = 0;
	        for (int i=0; i<kinds.length; i++)
	        {
	        	int k = Integer.parseInt((String) kinds[i]);
	        	if (k == Gildas30m.KIND_SPECTRAL) nsp ++;
	        }
	        int key[] = new int[nsp];
	        nsp = 0;
	        for (int i=0; i<kinds.length; i++)
	        {
	        	int k = Integer.parseInt((String) kinds[i]);
	        	if (k == Gildas30m.KIND_SPECTRAL) {
	        		key[nsp] = Integer.parseInt((String) keys[i]);
	        		nsp ++;
	        	}
	        }
	        return key;
        }
    }

    /**
     * Recovers a given number of spectra from a damaged file.
     * This option can be used when you detect the file you are
     * trying to read is corrupted after a given entry. With this
     * option only the first n spectra are read. It should be used
     * right after creating the instance.
     * @param n Number of spectra to try to read.
     * @throws JPARSECException If an error occurs.
     */
    public void recoverDamagedFile(int n) throws JPARSECException {
    	createIndex(n);
    }

    /**
     * Returns the header of a given spectrum.
     * @param i The index of the spectrum.
     * @return The header, or null if the spectrum does not exist.
     * @throws JPARSECException If an error occurs.
     */
    public SpectrumHeader30m getHeader(int i)
    throws JPARSECException {
        if(index == null) createIndex(-1);
        Integer integer1 = new Integer(ordered_index.get(i));
        int ix = integer1.intValue();
        return new SpectrumHeader30m(this.readHeader(ix));
    }

    private HeaderElement[] readHeader(int i)
    throws JPARSECException {
        int j = i / ilex;
        int k = (i - j * ilex) / 4;
        long l = (ByteArrayConverter.readInt(index_arr, j * 4) + k) - 1;
        long i1 = (i - k * 4 - j * ilex) * 32 * 4;
        byte abyte0[] = new byte[128];
        try
        {
            long skip = (l * 512L + i1);
            if (skip > file.length()) throw new JPARSECException("Cannot read header for entry "+i+". Maybe corrupted file ?");
            file.seek(skip);
            file.read(abyte0);
        }
        catch(Exception exception)
        {
			throw new JPARSECException(exception);
        }
        int j1 = convert.readInt(abyte0, 0);
        int k1 = convert.readInt(abyte0, 4);
        int i2 = convert.readInt(abyte0, 8);
        String s = new String(abyte0, 12, 12);
        String s1 = new String(abyte0, 24, 12);
        String s2 = new String(abyte0, 36, 12);
        int j2 = convert.readInt(abyte0, 48);
        int k2 = convert.readInt(abyte0, 52);
        float f = convert.readFloat(abyte0, 56);
        float f1 = convert.readFloat(abyte0, 60);
        int i3 = convert.readInt(abyte0, 64);
        int j3 = convert.readInt(abyte0, 68);
        int k3 = convert.readInt(abyte0, 72);
        int l3 = convert.readInt(abyte0, 76);
        float f2 = convert.readFloat(abyte0, 80);
        HeaderElement aobj[] = new HeaderElement[15];
        aobj[0] = new HeaderElement(Gildas30m.NUM, k1, Gildas30m.NUM_DESC);
        aobj[1] = new HeaderElement(Gildas30m.BLOCK, j1, Gildas30m.BLOCK_DESC);
        aobj[2] = new HeaderElement(Gildas30m.VERSION, ""+i2, Gildas30m.VERSION_DESC);
        aobj[3] = new HeaderElement(Gildas30m.SOURCE, s, Gildas30m.SOURCE_DESC);
        aobj[4] = new HeaderElement(Gildas30m.LINE, s1, Gildas30m.LINE_DESC);
        aobj[5] = new HeaderElement(Gildas30m.TELES, s2, Gildas30m.TELES_DESC);
        aobj[6] = new HeaderElement(Gildas30m.LDOBS, j2, Gildas30m.LDOBS_DESC);
        aobj[7] = new HeaderElement(Gildas30m.LDRED, k2, Gildas30m.LDRED_DESC);
        aobj[8] = new HeaderElement(Gildas30m.OFF1, f, Gildas30m.OFF1_DESC);
        aobj[9] = new HeaderElement(Gildas30m.OFF2, f1, Gildas30m.OFF2_DESC);
        aobj[10] = new HeaderElement(Gildas30m.TYPEC, i3, Gildas30m.TYPEC_DESC);
        aobj[11] = new HeaderElement(Gildas30m.KIND, j3, Gildas30m.KIND_DESC);
        aobj[12] = new HeaderElement(Gildas30m.QUALITY, k3, Gildas30m.QUALITY_DESC);
        aobj[13] = new HeaderElement(Gildas30m.SCAN, l3, Gildas30m.SCAN_DESC);
        aobj[14] = new HeaderElement(Gildas30m.POSA, f2, Gildas30m.POSA_DESC);
        return aobj;
    }

    private float[] readData(int i)
    throws JPARSECException {
        try
        {
            byte abyte0[] = readSomeBlocks(i);
            //convert.readInt(abyte0, 8);
            int k = convert.readInt(abyte0, 16);
            int l = convert.readInt(abyte0, 20);
            //convert.readInt(abyte0, 28);
            //convert.readInt(abyte0, 32);
            float ad[] = new float[l];
            for(int k1 = 0; k1 < ad.length; k1++)
                ad[k1] = convert.readFloat(abyte0, ((k + k1) - 1) * 4);

            return ad;
        }
        catch(Exception exception)
        {
			throw new JPARSECException(exception);
        }
    }

    private double[] readSections(int i, Spectrum30m spectrum)
    throws JPARSECException {
        try
        {
            byte abyte0[] = readSomeBlocks(i);
            //convert.readInt(abyte0, 8);
            //convert.readInt(abyte0, 16);
            //convert.readInt(abyte0, 20);
            int i1 = convert.readInt(abyte0, 28); // Number of sections
            //convert.readInt(abyte0, 32);
            int ai[] = new int[i1];
            int ai1[] = new int[i1];
            int ai2[] = new int[i1];
            for(int k1 = 0; k1 < i1; k1++)
            {
                ai[k1] = convert.readInt(abyte0, 36 + k1 * 4); // Section id
                ai1[k1] = convert.readInt(abyte0, 36 + i1 * 4 + k1 * 4); // Section length
                ai2[k1] = convert.readInt(abyte0, 36 + i1 * 4 + i1 * 4 + k1 * 4); // Section address
                switch(ai[k1])
                {
                case -2:
                    readGeneralSection(abyte0, ai2[k1], ai1[k1], spectrum);
                    break;

                case -3:
                    readPositionSection(abyte0, ai2[k1], ai1[k1], spectrum);
                    break;

                case -4:
                    readSpectroscopySection(abyte0, ai2[k1], ai1[k1], spectrum);
                    break;

                case -10:
                    readContinuumSection(abyte0, ai2[k1], ai1[k1], spectrum);
                    break;

                case -14:
                    readCalibrationSection(abyte0, ai2[k1], ai1[k1], spectrum);
                    break;

                case -30:
                    readDataDescriptorSection(abyte0, ai2[k1], ai1[k1], spectrum);
                    break;

                case -8:
                    readFrequencySwitchingSection(abyte0, ai2[k1], ai1[k1], spectrum);
                    break;

                case -5:
                    readBaselineSection(abyte0, ai2[k1], ai1[k1], spectrum);
                    break;
                }
            }

        }
        catch(Exception exception)
        {
			throw new JPARSECException(exception);
        }
        return null;
    }

    private void readGeneralSection(byte abyte0[], int i, int j, Spectrum30m spectrum)
    {
        i--;
        double d = convert.readDouble(abyte0, i * 4 + 0);
        spectrum.put(new String(UT_TIME), new HeaderElement(d, Gildas30m.UT_TIME_DESC));
        double d1 = convert.readDouble(abyte0, i * 4 + 8);
        spectrum.put(new String(LST_TIME), new HeaderElement(d1, Gildas30m.LST_TIME_DESC));
        float f = convert.readFloat(abyte0, i * 4 + 16);
        spectrum.put(new String(Gildas30m.AZIMUTH), new HeaderElement(f, Gildas30m.AZIMUTH_DESC));
        float f1 = convert.readFloat(abyte0, i * 4 + 20);
        spectrum.put(new String(Gildas30m.ELEVATION), new HeaderElement(f1, Gildas30m.ELEVATION_DESC));
        float f2 = convert.readFloat(abyte0, i * 4 + 24);
        spectrum.put(new String(Gildas30m.TAU), new HeaderElement(f2, Gildas30m.TAU_DESC));
        float f3 = convert.readFloat(abyte0, i * 4 + 28);
        spectrum.put(new String(Gildas30m.TSYS), new HeaderElement(f3, Gildas30m.TSYS_DESC));
        float f4 = convert.readFloat(abyte0, i * 4 + 32);
        spectrum.put(new String(Gildas30m.INTEG), new HeaderElement(f4, Gildas30m.INTEG_DESC));
    }

    private void readContinuumSection(byte abyte0[], int i, int j, Spectrum30m spectrum)
    {
        i--;
        double restf = convert.readDouble(abyte0, i * 4 + 0);
        float width = convert.readFloat(abyte0, i * 4 + 8);
        int npo = convert.readInt(abyte0, i * 4 + 12);
        float rpo = convert.readFloat(abyte0, i * 4 + 16);
        float tref = convert.readFloat(abyte0, i * 4 + 20);
        float aref = convert.readFloat(abyte0, i * 4 + 24);
        float apos = convert.readFloat(abyte0, i * 4 + 28);
        float tres = convert.readFloat(abyte0, i * 4 + 32);
        float ares = convert.readFloat(abyte0, i * 4 + 36);
        float bad = convert.readFloat(abyte0, i * 4 + 40);
        int ctype = convert.readInt(abyte0, i * 4 + 44);
        double cimag = convert.readDouble(abyte0, i * 4 + 48);
        float colla = convert.readFloat(abyte0, i * 4 + 56);
        float colle = convert.readFloat(abyte0, i * 4 + 60);

        spectrum.put(new String("COL_AZ"), new HeaderElement(colla, ""));
        spectrum.put(new String("COL_EL"), new HeaderElement(colle, ""));
    }

    private void readPositionSection(byte abyte0[], int i, int j, Spectrum30m spectrum)
    {
        i--;
        String s = new String(abyte0, i * 4, 12);
        spectrum.put(new String(SOURCE), new HeaderElement(s, SOURCE_DESC));
        float f = convert.readFloat(abyte0, i * 4 + 12);
        spectrum.put(new String(Gildas30m.EPOCH), new HeaderElement(f, Gildas30m.EPOCH_DESC));
        double d = convert.readDouble(abyte0, i * 4 + 16);
        spectrum.put(new String(Gildas30m.LAMBDA), new HeaderElement(d, Gildas30m.LAMBDA_DESC));
        double d1 = convert.readDouble(abyte0, i * 4 + 24);
        spectrum.put(new String(Gildas30m.BETA), new HeaderElement(d1, Gildas30m.BETA_DESC));
        float f1 = convert.readFloat(abyte0, i * 4 + 32);
        spectrum.put(new String(Gildas30m.LAMBDA_OFF), new HeaderElement(f1, Gildas30m.LAMBDA_OFF_DESC));
        float f2 = convert.readFloat(abyte0, i * 4 + 36);
        spectrum.put(new String(Gildas30m.BETA_OFF), new HeaderElement(f2, Gildas30m.BETA_OFF_DESC));
        int k = convert.readInt(abyte0, i * 4 + 40);
        spectrum.put(new String(Gildas30m.PROJECTION), new HeaderElement(k, Gildas30m.PROJECTION_DESC));
    }

    private void readSpectroscopySection(byte abyte0[], int i, int j, Spectrum30m spectrum)
    {
        i--;
        String s = new String(abyte0, i * 4, 12);
        spectrum.put(new String(LINE), new HeaderElement(s, LINE_DESC));
        double d = convert.readDouble(abyte0, i * 4 + 12);
        spectrum.put(new String(Gildas30m.REF_FREQ), new HeaderElement(d, Gildas30m.REF_FREQ_DESC));
        int k = convert.readInt(abyte0, i * 4 + 20);
        spectrum.put(new String(Gildas30m.NCHAN), new HeaderElement(k, Gildas30m.NCHAN_DESC));
        float f = convert.readFloat(abyte0, i * 4 + 24);
        spectrum.put(new String(Gildas30m.REF_CHAN), new HeaderElement((double)f, Gildas30m.REF_CHAN_DESC));
        float f1 = convert.readFloat(abyte0, i * 4 + 28);
        spectrum.put(new String(Gildas30m.FREQ_RESOL), new HeaderElement(f1, Gildas30m.FREQ_RESOL_DESC));
        float f2 = convert.readFloat(abyte0, i * 4 + 32);
        spectrum.put(new String(Gildas30m.FREQ_OFF), new HeaderElement(f2, Gildas30m.FREQ_OFF_DESC));
        float f3 = convert.readFloat(abyte0, i * 4 + 36);
        spectrum.put(new String(Gildas30m.VEL_RESOL), new HeaderElement(f3, Gildas30m.VEL_RESOL_DESC));
        float f4 = convert.readFloat(abyte0, i * 4 + 40);
        spectrum.put(new String(Gildas30m.REF_VEL), new HeaderElement(f4, Gildas30m.REF_VEL_DESC));
        float f5 = convert.readFloat(abyte0, i * 4 + 44);
        spectrum.put(new String(Gildas30m.BAD), new HeaderElement(f5, Gildas30m.BAD_DESC));
        double d1 = convert.readDouble(abyte0, i * 4 + 48);
        spectrum.put(new String(Gildas30m.IMAGE), new HeaderElement(d1, Gildas30m.IMAGE_DESC));
        int l = convert.readInt(abyte0, i * 4 + 56);
        spectrum.put(new String(Gildas30m.VEL_TYPE), new HeaderElement(l, Gildas30m.VEL_TYPE_DESC));
        if (j >= 17) {
	        double f6 = convert.readDouble(abyte0, i * 4 + 60);
	        spectrum.put(new String(Gildas30m.DOPPLER), new HeaderElement(f6, Gildas30m.DOPPLER_DESC));
        } else {
	        spectrum.put(new String(Gildas30m.DOPPLER), new HeaderElement(0.0, Gildas30m.DOPPLER_DESC));
        }
    }

    private void readCalibrationSection(byte abyte0[], int i, int j, Spectrum30m spectrum)
    {
        i--;
        float f = convert.readFloat(abyte0, i * 4);
        spectrum.put(new String(Gildas30m.BEAM_EFF), new HeaderElement(f, Gildas30m.BEAM_EFF_DESC));
        float f1 = convert.readFloat(abyte0, i * 4 + 4);
        spectrum.put(new String(Gildas30m.FORW_EFF), new HeaderElement(f1, Gildas30m.FORW_EFF_DESC));
        float f2 = convert.readFloat(abyte0, i * 4 + 8);
        spectrum.put(new String(Gildas30m.GAIN_IM), new HeaderElement(f2, Gildas30m.GAIN_IM_DESC));
        float f3 = convert.readFloat(abyte0, i * 4 + 12);
        spectrum.put(new String(Gildas30m.H2OMM), new HeaderElement(f3, Gildas30m.H2OMM_DESC));
        float f4 = convert.readFloat(abyte0, i * 4 + 16);
        spectrum.put(new String(Gildas30m.PAMB), new HeaderElement(f4, Gildas30m.PAMB_DESC));
        float f5 = convert.readFloat(abyte0, i * 4 + 20);
        spectrum.put(new String(Gildas30m.TAMB), new HeaderElement(f5, Gildas30m.TAMB_DESC));
        float f6 = convert.readFloat(abyte0, i * 4 + 24);
        spectrum.put(new String(Gildas30m.TATMSIG), new HeaderElement(f6, Gildas30m.TATMSIG_DESC));
        float f7 = convert.readFloat(abyte0, i * 4 + 28);
        spectrum.put(new String(Gildas30m.TCHOP), new HeaderElement(f7, Gildas30m.TCHOP_DESC));
        float f8 = convert.readFloat(abyte0, i * 4 + 32);
        spectrum.put(new String(Gildas30m.TCOLD), new HeaderElement(f8, Gildas30m.TCOLD_DESC));
        float f9 = convert.readFloat(abyte0, i * 4 + 36);
        spectrum.put(new String(Gildas30m.TAUSIG), new HeaderElement(f9, Gildas30m.TAUSIG_DESC));
        float f10 = convert.readFloat(abyte0, i * 4 + 40);
        spectrum.put(new String(Gildas30m.TAUIMA), new HeaderElement(f10, Gildas30m.TAUIMA_DESC));
        float f11a = convert.readFloat(abyte0, i * 4 + 44);
        spectrum.put(new String(Gildas30m.TATMIMG), new HeaderElement(f11a, Gildas30m.TATMIMG_DESC));
        float f11 = convert.readFloat(abyte0, i * 4 + 48);
        spectrum.put(new String(Gildas30m.TREC), new HeaderElement(f11, Gildas30m.TREC_DESC));
        int k = convert.readInt(abyte0, i * 4 + 48 + 4);
        spectrum.put(new String(Gildas30m.MODE), new HeaderElement(k, Gildas30m.MODE_DESC));
        float f12 = convert.readFloat(abyte0, i * 4 + 52 + 4);
        spectrum.put(new String(Gildas30m.FACTOR), new HeaderElement(f12, Gildas30m.FACTOR_DESC));
        float f13 = convert.readFloat(abyte0, i * 4 + 56 + 4);
        spectrum.put(new String(Gildas30m.ALTITUDE), new HeaderElement(f13, Gildas30m.ALTITUDE_DESC));
        float f14 = convert.readFloat(abyte0, i * 4 + 60 + 4);
        spectrum.put(new String(Gildas30m.COUNT1), new HeaderElement(f14, Gildas30m.COUNT1_DESC));
        float f15 = convert.readFloat(abyte0, i * 4 + 64 + 4);
        spectrum.put(new String(Gildas30m.COUNT2), new HeaderElement(f15, Gildas30m.COUNT2_DESC));
        float f16 = convert.readFloat(abyte0, i * 4 + 68 + 4);
        spectrum.put(new String(Gildas30m.COUNT3), new HeaderElement(f16, Gildas30m.COUNT3_DESC));
        float f17 = convert.readFloat(abyte0, i * 4 + 72 + 4);
        spectrum.put(new String(Gildas30m.LONOFF), new HeaderElement(f17, Gildas30m.LONOFF_DESC));
        float f18 = convert.readFloat(abyte0, i * 4 + 76 + 4);
        spectrum.put(new String(Gildas30m.LATOFF), new HeaderElement(f18, Gildas30m.LATOFF_DESC));
        double f19 = convert.readDouble(abyte0, i * 4 + 80 + 4);
        spectrum.put(new String(Gildas30m.LON), new HeaderElement(f19, Gildas30m.LON_DESC));
        double f20 = convert.readDouble(abyte0, i * 4 + 88 + 4);
        spectrum.put(new String(Gildas30m.LAT), new HeaderElement(f20, Gildas30m.LAT_DESC));
    }

    private void readDataDescriptorSection(byte abyte0[], int i, int j, Spectrum30m spectrum)
    {
        i--;
        int k = convert.readInt(abyte0, i * 4 + 0);
        spectrum.put(Gildas30m.OTF_NDUMPS, new HeaderElement(k, Gildas30m.OTF_NDUMPS_DESC));
        int l = convert.readInt(abyte0, i * 4 + 4);
        spectrum.put(new String(Gildas30m.OTF_LEN_HEADER), new HeaderElement(l, Gildas30m.OTF_LEN_HEADER_DESC));
        int i1 = convert.readInt(abyte0, i * 4 + 8);
        spectrum.put(new String(Gildas30m.OTF_LEN_DATA), new HeaderElement(i1, Gildas30m.OTF_LEN_DATA_DESC));
        int j1 = convert.readInt(abyte0, i * 4 + 12);
        spectrum.put(new String(Gildas30m.OTF_LEN_DUMP), new HeaderElement(j1, Gildas30m.OTF_LEN_DUMP_DESC));
    }

    private void readFrequencySwitchingSection(byte abyte0[], int i, int j, Spectrum30m spectrum)
    {
        i--;
        int k = convert.readInt(abyte0, i * 4 + 0);
        spectrum.put(Gildas30m.NPHASE, new HeaderElement(k, Gildas30m.NPHASE_DESC));
        int l = convert.readInt(abyte0, i * 4 + 4 + k * 8 + k * 4 + k * 4);
        spectrum.put(Gildas30m.SWMODE, new HeaderElement(l, Gildas30m.SWMODE_DESC));
        for(int i1 = 0; i1 < k; i1++)
        {
            double d = convert.readDouble(abyte0, i * 4 + 4 + i1 * 8);
            spectrum.put(new String((new StringBuilder()).append(Gildas30m.SWDECALAGE).append(i1).toString()), new HeaderElement(d, Gildas30m.SWDECALAGE_DESC));
            double d1 = convert.readFloat(abyte0, i * 4 + 4 + k * 8 + i1 * 4);
            spectrum.put(new String((new StringBuilder()).append(Gildas30m.SWDURATION).append(i1).toString()), new HeaderElement(d1, Gildas30m.SWDURATION_DESC));
            double d2 = convert.readFloat(abyte0, i * 4 + 4 + k * 8 + k * 4 + i1 * 4);
            spectrum.put(new String((new StringBuilder()).append(Gildas30m.SWPOIDS).append(i1).toString()), new HeaderElement(d2, Gildas30m.SWPOIDS_DESC));
            double d3 = convert.readFloat(abyte0, i * 4 + 4 + k * 8 + k * 4 + k * 4 + 4 + i1 * 4);
            spectrum.put(new String((new StringBuilder()).append(Gildas30m.SWLDECAL).append(i1).toString()), new HeaderElement(d3, Gildas30m.SWLDECAL_DESC));
            double d4 = convert.readFloat(abyte0, i * 4 + 4 + k * 8 + k * 4 + k * 4 + 4 + k * 4 + i1 * 4);
            spectrum.put(new String((new StringBuilder()).append(Gildas30m.SWBDECAL).append(i1).toString()), new HeaderElement(d4, Gildas30m.SWBDECAL_DESC));
        }

    }

    private void readBaselineSection(byte abyte0[], int i, int j, Spectrum30m spectrum)
    {
        i--;
        //convert.readInt(abyte0, i * 4 + 0);
        double d = convert.readFloat(abyte0, i * 4 + 4);
        spectrum.put(SIGMA, new HeaderElement(d, SIGMA_DESC));
        //convert.readFloat(abyte0, i * 4 + 4 + 4);
        int l = convert.readInt(abyte0, i * 4 + 4 + 4 + 4);
        if(l != 0)
        {
            double ad[] = new double[l];
            double ad1[] = new double[l];
            for(int i1 = 0; i1 < l; i1++)
            {
                ad[i1] = convert.readFloat(abyte0, i * 4 + 4 + 4 + 4 + i1 * 4);
                ad1[i1] = convert.readFloat(abyte0, i * 4 + 4 + 4 + 4 + i1 * 4 + l * 4);
            }

        }
    }

    private byte[] readSomeBlocks(int i)
    throws JPARSECException {
        try
        {
            long skip = (i - 1L) * 512L;
            if (skip > file.length()) throw new JPARSECException("Cannot read entry "+i+". Maybe corrupted file ?");
            file.seek(skip);
            byte abyte0[] = new byte[512];
            file.read(abyte0);
            int j = convert.readInt(abyte0, 4);
            byte abyte1[] = new byte[512 * j];
            System.arraycopy(abyte0, 0, abyte1, 0, abyte0.length);
            file.read(abyte1, abyte0.length, 512 * (j - 1));
            return abyte1;
        }
        catch(Exception exception)
        {
			throw new JPARSECException(exception);
        }
    }

    private float[] getData(int is)
    throws JPARSECException {
        Integer integer1 = new Integer(ordered_index.get(is));
        Integer integer = new Integer(index[integer1.intValue()].toString());
        float y[] = this.readData(integer.intValue());
    	return y;
    }

    /**
     * Closes the file and liberates resources. Once closed, the
     * file cannot be read again.
     * @throws JPARSECException If an error occurs.
     */
    public void closeFile() throws JPARSECException {
    	try {
    		if (file != null) file.close();
    	} catch (Exception exc) {
	    	 throw new JPARSECException("cannot close the file.", exc);
    	}
    }

    /**
     * Constructor given the path of a .30m file. Don't forget to
     * close the file after using it.
     * @param path The path of a .30m file. A .fits file from Gildas
     * containing spectra is also acceptable,
     * but will be converted into old .30m format,
     * saved in the same input directory (as xxx.old30m), and NOT removed.
     * @throws JPARSECException If an error occurs.
     */
	public Gildas30m(String path)
	throws JPARSECException {
		if (path.toLowerCase().endsWith(".fits") || path.toLowerCase().endsWith(".fit")) {
			String newPath = path.substring(0, path.lastIndexOf(".")) + ".old30m";
			Spectrum30m.writeAs30m(Spectrum30m.readSpectraFromFITS(path), newPath);
			path = newPath;
		}

        index_arr = new byte[1004];
        index = null;

		try {
		   if (file != null) file.close();
           file = new RandomAccessFile(new File(path), "r");
           byte abyte0[] = new byte[4];
           file.read(abyte0);
           code = new String(abyte0);
           convert = GildasDataFormat.getConvertible(code);
           if (convert == null) throw new JPARSECException("Unsupported Gildas format/version");
           file.read(abyte0);
           next_free_block = convert.readInt(abyte0, 0);
           file.read(abyte0);
           ilex = convert.readInt(abyte0, 0);
           file.read(abyte0);
           imex = convert.readInt(abyte0, 0);
           file.read(abyte0);
           next_free_entry = convert.readInt(abyte0, 0);
           file.read(index_arr);
           for(int i = 0; i < 251; i++)
           {
               int j = convert.readInt(index_arr, i * 4);
               Gildas30m.writeInt(index_arr, i * 4, j);
           }
	     } catch (EOFException eof) {
	    	 throw new JPARSECException("end of file encountered, probably corrupt file.", eof);
	     } catch (FileNotFoundException fnf) {
	    	 throw new JPARSECException("file "+path+" not found.", fnf);
	     } catch (IOException ioe) {
	    	 throw new JPARSECException("error reading, probably corrupt file.", ioe);
	     }
	}

	private static void writeInt(byte[] array, int offset, int value) {
		array[offset] =     (byte)(value >> 24);
		array[offset + 1] = (byte)(value >> 16);
		array[offset + 2] = (byte)(value >> 8);
		array[offset + 3] = (byte)value;
	}
}

/*
Following keys only exists in a continuum file, not spectroscopy

List of keys (key name, value, description)
-bad = 0.0 (Blanking value)
-freqoff = 0.0 (Frequency offset)
-fresol = 0.0 (Frequency resolution)
-image = 0.0 (Image frequency)
-line =              (Name of the line)
-nchan = 0 (Number of channels)
-refchan = 0.0 (Reference channel)
-rfreq = 0.0 (Rest Frequency)
-veltype = 0 (Type of velocity)
-voff = 0.0 (Velocity at reference channel)
-vres = 0.0 (Velocity resolution)

*/
