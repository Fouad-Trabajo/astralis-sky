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

import jparsec.graph.*;
import jparsec.graph.GridChartElement.TYPE;
import jparsec.io.image.FitsIO;
import jparsec.io.image.HeaderElement;
import jparsec.io.image.ImageSplineTransform;
import jparsec.io.image.WCS;
import jparsec.astronomy.CoordinateSystem;
import jparsec.astronomy.CoordinateSystem.COORDINATE_SYSTEM;
import jparsec.astrophysics.FluxElement;
import jparsec.astrophysics.Spectrum;
import jparsec.astrophysics.gildas.Spectrum30m.XUNIT;
import jparsec.observer.*;
import jparsec.time.*;
import jparsec.ephem.*;
import jparsec.ephem.Target.TARGET;
import jparsec.math.Constant;
import jparsec.math.Evaluation;
import jparsec.math.FastMath;
import jparsec.math.Interpolation;
import jparsec.math.MeasureElement;
import jparsec.math.Table;
import jparsec.io.FileIO;
import jparsec.io.binaryFormat.Convertible;
import jparsec.util.*;
import nom.tam.fits.ImageHDU;

/**
 * A class to read/write lmv (GILDAS) datacubes. A datacube can be created in
 * two ways: from a GILDAS .lmv (or .gdf) file or directly by inserting the
 * cube and the rest of the data in the constructor. In the first case the
 * property {@link #cube} will continue to be equal to null to prevent
 * memory errors when reading large files. Whenever you retrieve the
 * cube of data the input file will be read again. You should prevent this
 * behavior using {@link #setCubeData(float[][][])}, or
 * using the method that reduces the resolution of the cube.
 * In the second case the cube must be explicitly inserted in the constructor
 * or the {@link #cube} property. It is supposed that enough memory
 * is available to hold the instance in this case.  In large files (&gt;20 MB)
 * to previously smooth the datacube to a lower resolution level could be
 * required.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class LMVCube implements Serializable
{
	private static final long serialVersionUID = 1L;

	private static final long INIT = 0;
	private boolean readingFile;
	private String path;

	/**
	 * Full constructor for a cube with 3 axes ordered as [velocity][dec][ra], and projection
	 * type AZP.
	 * @param source Source name.
	 * @param cube The cube.
	 * @param unit The map units.
	 * @param line The line name.
	 * @param freq The frequency in MHz.
	 * @param label1 Label for x axis.
	 * @param label2 Label for y axis.
	 * @param label3 Label for z axis.
	 * @param raPos The right ascension of the reference pixel in radians.
	 * @param decPos Declination of the reference pixel in radians.
	 * @param ra The right ascension of the source in radians.
	 * @param dec Declination of the source in radians.
	 * @param v0 Velocity LSR in km/s.
	 * @param coordSys Coordinate system.
	 * @param epoch Epoch of coordinates.
	 * @param beamMajor Beam major axis in radians.
	 * @param beamMinor Beam minor axis in radians.
	 * @param beamPA Beam position angle in radians.
	 * @param skyPA Position angle in the sky.
	 * @param blanking Blanking value.
	 * @param blankTol Tolerance of blanking.
	 * @param formula Conversion formula.
	 * @param rms The rms.
	 * @param dra Proper motion in RA.
	 * @param ddec Proper motion in DEC.
	 * @param parallax Parallax.
	 * @throws JPARSECException If an error occurs.
	 */
	public LMVCube(String source, float[][][] cube, String unit, String line, double freq,
			String label1, String label2, String label3, double raPos, double decPos, double ra,
			double dec, float v0, CoordinateSystem.COORDINATE_SYSTEM coordSys, float epoch, float beamMajor, float beamMinor, float beamPA, double skyPA,
			float blanking, float blankTol, double[] formula, float rms, float dra, float ddec, float parallax)
	throws JPARSECException {
		this.axis12PA = skyPA;
		this.axis1Dim = cube[0][0].length;
		this.axis2Dim = cube[0].length;
		this.axis3Dim = cube.length;
		this.axis1Pos = raPos;
		this.axis2Pos = decPos;
		this.axis1Label = label1;
		this.axis2Label = label2;
		this.axis3Label = label3;
		this.axis4Dim = 1;
		this.axis4Label = "";
		this.beamMajor = beamMajor;
		this.beamMinor = beamMinor;
		this.beamPA = beamPA;
		this.blanking = blanking;
		this.blankingTolerance = blankTol;
		this.conversionFormula = formula;
		this.coordinateSystem = CoordinateSystem.COORDINATE_SYSTEMS[coordSys.ordinal()].toUpperCase();
		this.epoch = epoch;
		this.fluxUnit = unit;
		this.freqResolution = -freq * formula[8] * 1000.0 / Constant.SPEED_OF_LIGHT;
		this.imageFrequency = freq;
		this.line = line;
		this.maximumFlux = this.minimumFlux = 0;
		this.numberOfAxes = 3;
		this.projectionType = LMVCube.PROJECTION.AZP;
		this.restFreq = freq;
		this.rms = rms;
		this.noise = rms;
		this.sourceDEC = dec;
		this.sourceRA = ra;
		this.sourceParallax = parallax;
		this.sourceProperMotionRA = dra;
		this.sourceProperMotionDEC = ddec;
		this.sourceName = source;
		LocationElement loc = new LocationElement(ra, dec, 1.0);
		TimeElement time = new TimeElement(Constant.J2000, TimeElement.SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		ObserverElement obs = new ObserverElement();
		EphemerisElement eph = new EphemerisElement(TARGET.SUN, EphemerisElement.COORDINATES_TYPE.APPARENT,
				EphemerisElement.EQUINOX_J2000, EphemerisElement.GEOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
				EphemerisElement.FRAME.ICRF);
		loc = CoordinateSystem.equatorialToGalactic(loc, time, obs, eph);
		this.sourceGLon = loc.getLongitude();
		this.sourceGLat = loc.getLatitude();
		this.velOffset = v0;
		this.velResolution = (float) formula[8];
		this.xAxisID = 1;
		this.yAxisID = 2;
		this.zAxisID = 3;
		readingFile = false;
		this.cube = cube;
		this.setExtremaData(cube);
		this.setWCS();
	}

	/**
	 * Almost full constructor for a cube with 3 axes ordered as [velocity][dec][ra], and projection
	 * type AZP. The position for the reference pixel is taken from the source position.
	 * @param source Source name.
	 * @param cube The cube.
	 * @param unit The map units.
	 * @param line The line name.
	 * @param freq The frequency in MHz.
	 * @param label1 Label for x axis.
	 * @param label2 Label for y axis.
	 * @param label3 Label for z axis.
	 * @param ra The right ascension in radians.
	 * @param dec Declination in radians.
	 * @param v0 Velocity LSR in km/s.
	 * @param coordSys Coordinate system.
	 * @param epoch Epoch of coordinates.
	 * @param beamMajor Beam major axis in radians.
	 * @param beamMinor Beam minor axis in radians.
	 * @param beamPA Beam position angle in radians.
	 * @param skyPA Position angle in the sky.
	 * @param blanking Blanking value.
	 * @param blankTol Tolerance of blanking.
	 * @param formula Conversion formula.
	 * @param rms The rms.
	 * @param dra Proper motion in RA.
	 * @param ddec Proper motion in DEC.
	 * @param parallax Parallax.
	 * @throws JPARSECException If an error occurs.
	 */
	public LMVCube(String source, float[][][] cube, String unit, String line, double freq,
			String label1, String label2, String label3, double ra,
			double dec, float v0, CoordinateSystem.COORDINATE_SYSTEM coordSys, float epoch, float beamMajor, float beamMinor, float beamPA, double skyPA,
			float blanking, float blankTol, double[] formula, float rms, float dra, float ddec, float parallax)
	throws JPARSECException {
		this.axis12PA = skyPA;
		this.axis1Dim = cube[0][0].length;
		this.axis2Dim = cube[0].length;
		this.axis3Dim = cube.length;
		this.axis1Label = label1;
		this.axis2Label = label2;
		this.axis3Label = label3;
		this.axis1Pos = ra;
		this.axis2Pos = dec;
		this.axis4Dim = 1;
		this.axis4Label = "";
		this.beamMajor = beamMajor;
		this.beamMinor = beamMinor;
		this.beamPA = beamPA;
		this.blanking = blanking;
		this.blankingTolerance = blankTol;
		this.conversionFormula = formula;
		this.coordinateSystem = CoordinateSystem.COORDINATE_SYSTEMS[coordSys.ordinal()].toUpperCase();
		this.epoch = epoch;
		this.fluxUnit = unit;
		this.freqResolution = -freq * formula[8] * 1000.0 / Constant.SPEED_OF_LIGHT;
		this.imageFrequency = freq;
		this.line = line;
		this.maximumFlux = this.minimumFlux = 0;
		this.numberOfAxes = 3;
		this.projectionType = LMVCube.PROJECTION.AZP;
		this.restFreq = freq;
		this.rms = rms;
		this.noise = rms;
		this.sourceDEC = dec;
		this.sourceRA = ra;
		this.sourceParallax = parallax;
		this.sourceProperMotionRA = dra;
		this.sourceProperMotionDEC = ddec;
		this.sourceName = source;
		LocationElement loc = new LocationElement(ra, dec, 1.0);
		TimeElement time = new TimeElement(Constant.J2000, TimeElement.SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		ObserverElement obs = new ObserverElement();
		EphemerisElement eph = new EphemerisElement(TARGET.SUN, EphemerisElement.COORDINATES_TYPE.APPARENT,
				EphemerisElement.EQUINOX_J2000, EphemerisElement.GEOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
				EphemerisElement.FRAME.ICRF);
		loc = CoordinateSystem.equatorialToGalactic(loc, time, obs, eph);
		this.sourceGLon = loc.getLongitude();
		this.sourceGLat = loc.getLatitude();
		this.velOffset = v0;
		this.velResolution = (float) formula[8];
		this.xAxisID = 1;
		this.yAxisID = 2;
		this.zAxisID = 3;
		readingFile = false;
		this.cube = cube;
		this.setExtremaData(cube);
		this.setWCS();
	}

	/**
	 * Simplified constructor, similar to the full one, but for J2000 equatorial
	 * coordinates with blanking and sky position angle equal to 0.
	 * @param source Source name.
	 * @param cube The cube.
	 * @param unit The map units.
	 * @param line The line name.
	 * @param freq The frequency in MHz.
	 * @param ra The right ascension in radians.
	 * @param dec Declination in radians.
	 * @param v0 Velocity LSR in km/s.
	 * @param beamMajor Beam major axis in radians.
	 * @param beamMinor Beam minor axis in radians.
	 * @param beamPA Beam position angle in radians.
	 * @param formula Conversion formula.
	 * @throws JPARSECException If an error occurs.
	 */
	public LMVCube(String source, float[][][] cube, String unit, String line, double freq,
			double ra, double dec, float v0, float beamMajor, float beamMinor, float beamPA, double[] formula)
	throws JPARSECException {
		init(source, cube, unit, line, freq, ra, dec, v0, beamMajor, beamMinor, beamPA, formula);
	}

	private void init(String source, float[][][] cube, String unit, String line, double freq,
			double ra, double dec, float v0, float beamMajor, float beamMinor, float beamPA, double[] formula)
	throws JPARSECException {
		this.axis12PA = 0;
		this.axis1Dim = cube[0][0].length;
		this.axis2Dim = cube[0].length;
		this.axis3Dim = cube.length;
		this.axis1Label = COORDINATES_X[CoordinateSystem.COORDINATE_SYSTEM.EQUATORIAL.ordinal()];
		this.axis2Label = COORDINATES_Y[CoordinateSystem.COORDINATE_SYSTEM.EQUATORIAL.ordinal()];
		this.axis3Label = COORDINATES_Z[CoordinateSystem.COORDINATE_SYSTEM.EQUATORIAL.ordinal()];
		this.axis1Pos = ra;
		this.axis2Pos = dec;
		this.axis4Dim = 1;
		this.axis4Label = "";
		this.beamMajor = beamMajor;
		this.beamMinor = beamMinor;
		this.beamPA = beamPA;
		this.blanking = 0;
		this.blankingTolerance = 0;
		this.conversionFormula = formula;
		this.coordinateSystem = CoordinateSystem.COORDINATE_SYSTEMS[CoordinateSystem.COORDINATE_SYSTEM.EQUATORIAL.ordinal()].toUpperCase();
		this.epoch = 2000;
		this.fluxUnit = unit;
		this.freqResolution = -freq * formula[8] * 1000.0 / Constant.SPEED_OF_LIGHT;
		this.imageFrequency = freq;
		this.line = line;
		this.maximumFlux = this.minimumFlux = 0;
		this.numberOfAxes = 3;
		this.projectionType = LMVCube.PROJECTION.AZP;
		this.restFreq = freq;
		this.sourceDEC = dec;
		this.sourceRA = ra;
		this.sourceName = source;
		LocationElement loc = new LocationElement(ra, dec, 1.0);
		TimeElement time = new TimeElement(Constant.J2000, TimeElement.SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		ObserverElement obs = new ObserverElement();
		EphemerisElement eph = new EphemerisElement(TARGET.SUN, EphemerisElement.COORDINATES_TYPE.APPARENT,
				EphemerisElement.EQUINOX_J2000, EphemerisElement.GEOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
				EphemerisElement.FRAME.ICRF);
		loc = CoordinateSystem.equatorialToGalactic(loc, time, obs, eph);
		this.sourceGLon = loc.getLongitude();
		this.sourceGLat = loc.getLatitude();
		this.velOffset = v0;
		this.velResolution = (float) formula[8];
		this.xAxisID = 1;
		this.yAxisID = 2;
		this.zAxisID = 3;
		readingFile = false;
		this.cube = cube;
		this.setExtremaData(cube);
		this.setWCS();
	}
	
	/**
	 * Constructor for a given lmv file.
	 * @param path The path to the file.
	 * @throws JPARSECException If an error occurs,
	 * for example if the file is not an lmv one.
	 */
    public LMVCube(String path)
    throws JPARSECException {
    	readFile(path);
    }
    
    /**
     * Reads a cube from a .fits file (or tries), based on Gildas .fits files. 
     * This method is called 
     * automatically when the input path for the file constructor points 
     * to a .fits file. Source name needs to be set manually later, it is 
     * left empty here. In case flux is not K you may need to set flux unit 
     * and scale the intensity to transform Jy to K, also manually.
     * @param path Path to the .fits file.
     * @throws JPARSECException If an error occurs.
     */
    public void readFromFits(String path) throws JPARSECException {
		FitsIO f = new FitsIO(path);
		
		// Read HDU 0, of image type, with its header and data
		ImageHDU hdu = (ImageHDU) f.getHDU(0);
		HeaderElement header[] = f.getHeader(0);
		Object o = null;
		try {
			o = (hdu.getData()).getData();
		} catch (Exception exc) {
			throw new JPARSECException(exc);
		}
		
		
		// Read main source data. Velocities are in m/s, angles in degrees, and frecuencies 
		// in Hz. Only exception is VELREF which is in km/s in Leroy's file (Gildas will not
		// load it correctly) ...
		float[][][] cube;
		try {
			int data[][][][] = (int[][][][]) o;
			
			cube = new float[data[0].length][data[0][0].length][data[0][0][0].length]; 
			double max = -1, min = -1;
			double bzero = hdu.getBZero(), bscale = hdu.getBScale(); //, maxVal = Math.pow(2, Math.abs(hdu.getBitPix())-1);
			for (int v=0;v<cube.length;v++) {
				cube[v] = DataSet.toFloatArray(DataSet.toDoubleArray(data[0][v]));
				for  (int y=0; y<cube[0].length; y++) {
					for  (int x=0; x<cube[0][0].length; x++) {
						if (Double.isNaN(cube[v][y][x])) cube[v][y][x] = 0;
						cube[v][y][x] = (float) ((bzero + bscale * (cube[v][y][x]))); // - maxVal ?
						if (cube[v][y][x] > max || max == -1) max = cube[v][y][x];
						if (cube[v][y][x] < min || min == -1) min = cube[v][y][x];
					}					
				}
			}
		} catch (Exception exc) {
			int data[][][] = (int[][][]) o;
			
			cube = new float[data.length][data[0].length][data[0][0].length]; 
			double max = -1, min = -1;
			double bzero = hdu.getBZero(), bscale = hdu.getBScale(); //, maxVal = Math.pow(2, Math.abs(hdu.getBitPix())-1);
			for (int v=0;v<cube.length;v++) {
				cube[v] = DataSet.toFloatArray(DataSet.toDoubleArray(data[v]));
				for  (int y=0; y<cube[0].length; y++) {
					for  (int x=0; x<cube[0][0].length; x++) {
						if (Double.isNaN(cube[v][y][x])) cube[v][y][x] = 0;
						cube[v][y][x] = (float) ((bzero + bscale * (cube[v][y][x]))); // - maxVal ?
						if (cube[v][y][x] > max || max == -1) max = cube[v][y][x];
						if (cube[v][y][x] < min || min == -1) min = cube[v][y][x];
					}					
				}
			}
		}
		
		String unit = hdu.getBUnit();
		String line = HeaderElement.getByKey(header, "LINE").getAsString().trim();
		double freq = HeaderElement.getByKeyAsDouble(header, "RESTFREQ") * 1.0E-6;
		double ra = HeaderElement.getByKeyAsDouble(header, "CRVAL1") * Constant.DEG_TO_RAD;
		double dec = HeaderElement.getByKeyAsDouble(header, "CRVAL2") * Constant.DEG_TO_RAD;
		double v0 = HeaderElement.getByKeyAsDouble(header, "VELREF"); // LSR, in km/s in the fits file
		double beamMajor = 0, beamMinor = 0, beamPA = 0;
		try {
			beamMajor = HeaderElement.getByKeyAsDouble(header, "BMAJ") * Constant.DEG_TO_RAD;
			beamMinor = HeaderElement.getByKeyAsDouble(header, "BMIN") * Constant.DEG_TO_RAD;
			beamPA = HeaderElement.getByKeyAsDouble(header, "BPA") * Constant.DEG_TO_RAD;
		} catch (Exception exc) {
			exc.printStackTrace();
		}
		
		double formula[] = new double[] {
				// - 3 values: reference pixel, value, increment in axis 1-4
				// - In Gildas convention first pixel is 1, 1. In Fits ... depends on the 
				// software (I think). It should be also 1, 1, but many software use 0, 0 (IDL)
				// as first pixel, making this a mess.
				HeaderElement.getByKeyAsDouble(header, "CRPIX1"), 0, HeaderElement.getByKeyAsDouble(header, "CDELT1") * Constant.DEG_TO_RAD,
				HeaderElement.getByKeyAsDouble(header, "CRPIX2"), 0, HeaderElement.getByKeyAsDouble(header, "CDELT2") * Constant.DEG_TO_RAD,
				HeaderElement.getByKeyAsDouble(header, "CRPIX3"), HeaderElement.getByKeyAsDouble(header, "CRVAL3") * 0.001, HeaderElement.getByKeyAsDouble(header, "CDELT3") * 0.001,
				0, 0, 0
		};
		
		// Create the Gildas cube from previous data
		String source = "";
		init(source, cube, unit, line, freq, ra, dec, (float) v0, (float) beamMajor, (float) beamMinor, (float) beamPA, formula);
		projectionType = PROJECTION.TAN; // TAN projection mentioned in CTYPE
    }

    private void readFile(String path)
    throws JPARSECException {
        readingFile = true;
        this.path = path;
        
        if (path.endsWith(".fits")) {
        	readFromFits(path);
        	readingFile = false;
        	return;
        }
        
        try
        {
            bis = new RandomAccessFile(path, "r");
            //bis = new BufferedInputStream(fis, 8192);
            //bis.mark(0x7fffffff);
            byte abyte0[] = new byte[12];
            bis.read(abyte0);
            String s1 = new String(abyte0);
            if(!s1.substring(0, 6).equals("GILDAS") && !s1.substring(7, 12).equals("IMAGE"))
                throw new IOException("'"+s1+"' is not a GILDAS image");
            convert = GildasDataFormat.getConvertibleImage(s1.charAt(6));
            readHeader();
    		this.setExtremaData();
    		this.setWCS();
        } catch(Exception exception) {
            throw new JPARSECException(exception);
        }
    }
    private void readHeader()
    throws JPARSECException {
        try
        {
            char c = '\0';
            bis.seek(INIT);
            byte abyte0[] = new byte[512];
            bis.read(abyte0);
            c = '\f';
            //int imageFormat =
            	convert.readInt(abyte0, c);
            //int numberOfBlocksOfImage =
            		convert.readInt(abyte0, c + 4);
            //boolean isBig = (numberOfBlocksOfImage + 1) % 16 == 0;
            c = '(';
            //int lengthOfGeneralSection =
            	convert.readInt(abyte0, c);
            numberOfAxes = convert.readInt(abyte0, c + 4);
            axis1Dim = convert.readInt(abyte0, c + 4 + 4);
            axis2Dim = convert.readInt(abyte0, c + 4 + 4 + 4);
            axis3Dim = convert.readInt(abyte0, c + 4 + 4 + 4 + 4);
            axis4Dim = convert.readInt(abyte0, c + 4 + 4 + 4 + 4 + 4);
            c = '@';
            conversionFormula = new double[12];
            for(int i = 0; i < 12; i++)
            {
                conversionFormula[i] = convert.readDouble(abyte0, c + i * 8);
            }

            c = '\240';
            //int lengthOfBlankingSection =
            		convert.readInt(abyte0, c);
            blanking = convert.readFloat(abyte0, c + 4);
            blankingTolerance = convert.readFloat(abyte0, c + 4 + 4);
            //int lengthOfExtremaSection =
            	convert.readInt(abyte0, c + 4 + 4 + 4);
            minimumFlux = convert.readFloat(abyte0, c + 4 + 4 + 4 + 4);
            maximumFlux = convert.readFloat(abyte0, c + 4 + 4 + 4 + 4 + 4);
            c = '\270';
            minimumAndMaximumFluxPositions = new int[8];
            for(int j = 0; j < 8; j++)
            {
                minimumAndMaximumFluxPositions[j] = convert.readInt(abyte0, c + j * 4);
            }

            c = '\330';
            //int lengthOfDescriptionSection =
            	convert.readInt(abyte0, c);
            fluxUnit = new String(abyte0, c + 4, 12);
            axis1Label = new String(abyte0, c + 4 + 12, 12);
            axis2Label = new String(abyte0, c + 4 + 12 + 12, 12);
            axis3Label = new String(abyte0, c + 4 + 12 + 12 + 12, 12);
            axis4Label = new String(abyte0, c + 4 + 12 + 12 + 12 + 12, 12);
            coordinateSystem = new String(abyte0, c + 4 + 48 + 12, 12);
            c = '\u0124';
            //int lengthOfPositionSection =
            	convert.readInt(abyte0, c);
            sourceName = new String(abyte0, c + 4, 12);
            c = '\u0134';
            sourceRA = convert.readDouble(abyte0, c);
            sourceDEC = convert.readDouble(abyte0, c + 8);
            sourceGLon = convert.readDouble(abyte0, c + 8 + 8);
            sourceGLat = convert.readDouble(abyte0, c + 8 + 8 + 8);
            c = '\u0154';
            epoch = convert.readFloat(abyte0, c);
            //int lengthOfProjectionSection =
            	convert.readInt(abyte0, c + 4);
            projectionType = PROJECTION.TAN;
            int pi = convert.readInt(abyte0, c + 4 + 4);
            if (pi < PROJECTION.values().length) projectionType = PROJECTION.values()[pi];
            axis1Pos = convert.readDouble(abyte0, c + 4 + 4 + 4);
            axis2Pos = convert.readDouble(abyte0, c + 4 + 4 + 4 + 8);
            axis12PA = convert.readDouble(abyte0, c + 4 + 4 + 4 + 8 + 8);
            c = '\u0178';
            xAxisID = convert.readInt(abyte0, c);
            yAxisID = convert.readInt(abyte0, c + 4);
            //int lengthOfSpectroscopySection =
            	convert.readInt(abyte0, c + 4 + 4);
            line = new String(abyte0, c + 4 + 4 + 4, 12);
            c = '\u0190';
            freqResolution = convert.readDouble(abyte0, c);
            imageFrequency = convert.readDouble(abyte0, c + 8);
            restFreq = convert.readDouble(abyte0, c + 8 + 8);
            velResolution = convert.readFloat(abyte0, c + 8 + 8 + 8);
            velOffset = convert.readFloat(abyte0, c + 8 + 8 + 8 + 4);
            zAxisID = convert.readInt(abyte0, c + 8 + 8 + 8 + 4 + 4);
            //int lengthOfResolutionSection =
            	convert.readInt(abyte0, c + 8 + 8 + 8 + 4 + 4 + 4);
            beamMajor = convert.readFloat(abyte0, c + 8 + 8 + 8 + 4 + 4 + 4 + 4);
            beamMinor = convert.readFloat(abyte0, c + 8 + 8 + 8 + 4 + 4 + 4 + 4 + 4);
            beamPA = convert.readFloat(abyte0, c + 8 + 8 + 8 + 4 + 4 + 4 + 4 + 4 + 4);
            //int lengthOfNoiseSection =
            	convert.readInt(abyte0, c + 48 + 4);
            noise = convert.readFloat(abyte0, c + 48 + 8);
            rms = convert.readFloat(abyte0, c + 48 + 12);
            //int lengthOfProperMotionSection =
            	convert.readInt(abyte0, c + 48 + 16);
            sourceProperMotionRA = convert.readFloat(abyte0, c + 48 + 20);
            sourceProperMotionDEC = convert.readFloat(abyte0, c + 48 + 24);
            sourceParallax = convert.readFloat(abyte0, c + 48 + 28);
        }
        catch(Exception exception)
        {
            throw new JPARSECException(JPARSECException.getTrace(exception.getStackTrace()));
        }
    }

    private void setExtremaData() throws JPARSECException {
    	if (!readingFile || cube != null) {
    		setExtremaData(setBlankingToZero(cube.clone()));
    		return;
    	}

    	if (readingFile && cube == null) readHeader();

        float ad[][][] = new float[1][axis2Dim][axis1Dim];
        double maxValue = 4.9406564584124654E-324D;
        double minValue = 1.7976931348623157E+308D;
    	this.minimumAndMaximumFluxPositions = new int[8];
        try
        {
            bis.seek(INIT);
            bis.skipBytes(512);

            byte abyte0[] = new byte[axis1Dim * axis2Dim * 4];
            int j = 0;
            for(int l = 0; l < axis3Dim; l++)
            {
            	bis.read(abyte0);
                for(int j1 = 0; j1 < axis2Dim; j1++)
                {
                    for(int k1 = 0; k1 < axis1Dim; k1++)
                    {
                        float d = convert.readFloat(abyte0, j % (axis1Dim * axis2Dim * 4));
                        ad[0][j1][k1] = d;
                        j += 4;
                    }
                }

                ad = setBlankingToZero(ad);
        		for (int x=0; x<ad[0][0].length; x++)
        		{
            		for (int y=0; y<ad[0].length; y++)
            		{
                        if(ad[0][y][x] < minValue) {
                            minValue = ad[0][y][x];
                            minimumAndMaximumFluxPositions[0] = x + 1;
                            minimumAndMaximumFluxPositions[2] = y + 1;
                            minimumAndMaximumFluxPositions[4] = 0 + 1;
                            minimumAndMaximumFluxPositions[6] = 0 + 1;
                        }
                        if(ad[0][y][x] > maxValue) {
                            maxValue = ad[0][y][x];
                            minimumAndMaximumFluxPositions[1] = x + 1;
                            minimumAndMaximumFluxPositions[3] = y + 1;
                            minimumAndMaximumFluxPositions[5] = 0 + 1;
                            minimumAndMaximumFluxPositions[7] = 0 + 1;
                        }
            		}
        		}
            }

    		this.minimumFlux = (float) minValue;
    		this.maximumFlux = (float) maxValue;
        } catch(Exception exception) {
			throw new JPARSECException(exception);
        }
    }

    private void setExtremaData(float[][][] cube)
    throws JPARSECException {
    	if (cube == null) return;
        double maxValue = 4.9406564584124654E-324D;
        double minValue = 1.7976931348623157E+308D;
    	this.minimumAndMaximumFluxPositions = new int[8];

		for (int v=0; v<cube.length; v++)
		{
    		for (int x=0; x<cube[0][0].length; x++)
    		{
        		for (int y=0; y<cube[0].length; y++)
        		{
                    if(cube[v][y][x] < minValue) {
                        minValue = cube[v][y][x];
                        minimumAndMaximumFluxPositions[0] = x + 1;
                        minimumAndMaximumFluxPositions[2] = y + 1;
                        minimumAndMaximumFluxPositions[4] = v + 1;
                        minimumAndMaximumFluxPositions[6] = 0 + 1;
                    }
                    if(cube[v][y][x] > maxValue) {
                        maxValue = cube[v][y][x];
                        minimumAndMaximumFluxPositions[1] = x + 1;
                        minimumAndMaximumFluxPositions[3] = y + 1;
                        minimumAndMaximumFluxPositions[5] = v + 1;
                        minimumAndMaximumFluxPositions[7] = 0 + 1;
                    }
        		}
    		}
		}

		this.minimumFlux = (float) minValue;
		this.maximumFlux = (float) maxValue;
    }

    /**
     * Returns the data in the cube changing any blanking values to zero. This
     * change is required to use the returned data as input for a given 2D/3D
     * chart. This method reads again the header if
     * the cube data is empty to set the internal variables to their original
     * values, without any smooth.
     * @return The cube of data as an array in 3d, ordered by
     * number of levels, number of columns (DEC), and number of rows (RA).
	 * @throws JPARSECException If an error occurs,
	 * for example if the file is not an lmv one.
     */
    public float[][][] getCubeData()
    throws JPARSECException {
    	return getCubeData(true);
    }

    /**
     * Returns the data in the cube changing optionally any blanking values to zero. This
     * change is required to use the returned data as input for a given 2D/3D
     * chart. This method reads again the header if
     * the cube data is empty to set the internal variables to their original
     * values, without any smooth.
     * @param applyBlanking True to apply blanking (setting to 0 values equal to it).
     * This is slow when requesting frequently the cube data.
     * @return The cube of data as an array in 3d, ordered by
     * number of levels, number of columns (DEC), and number of rows (RA).
	 * @throws JPARSECException If an error occurs,
	 * for example if the file is not an lmv one.
     */
    public float[][][] getCubeData(boolean applyBlanking)
    throws JPARSECException {
    	if (!readingFile || cube != null) {
    		if (applyBlanking) return setBlankingToZero(cube.clone());
    		return cube.clone();
    	}

    	if (readingFile && cube == null) readHeader();

        float ad[][][] = new float[axis3Dim][axis2Dim][axis1Dim];
        try
        {
            bis.seek(INIT);
            bis.skipBytes(512);

            byte abyte0[] = new byte[axis1Dim * axis2Dim * 4];
            int j = 0;
            for(int l = 0; l < axis3Dim; l++)
            {
            	bis.read(abyte0);
                for(int j1 = 0; j1 < axis2Dim; j1++)
                {
                    for(int k1 = 0; k1 < axis1Dim; k1++)
                    {
                        float d = convert.readFloat(abyte0, j % (axis1Dim * axis2Dim * 4));
                        ad[l][j1][k1] = d;
                        j += 4;
                    }
                }
            }
        }
        catch(Exception exception)
        {
			throw new JPARSECException(exception);
        }

    	if (readingFile && cube == null) {
			this.setExtremaData(ad);
			this.setWCS();
    	}

        if (applyBlanking) return setBlankingToZero(ad);
        return ad;
    }

    /**
     * Returns the cube as a Table object.
     * @param plane The plane to return, or -1 for integrated intensity.
     * @param rawData True to return raw data with the blanking value set to
     * a given value, or false to return 0 as blanking.
     * @return The cube.
     * @throws JPARSECException If an error occurs.
     */
    public Table getAsTable(int plane, boolean rawData) throws JPARSECException {
    	if (plane == -1) return new Table(DataSet.toDoubleArray(this.integratedIntensity()), fluxUnit + " (km/s)");
    	if (plane < 0 || plane >= this.axis3Dim) throw new JPARSECException("Plane "+plane+" does not exist.");
    	float data[][] = null;
    	if (rawData) {
    		data = this.getRawCubeData(plane);
    	} else {
    		data = this.getCubeData(plane);
    	}
    	return new Table(DataSet.toDoubleArray(data), fluxUnit);
    }

    private float[][][] setBlankingToZero(float[][][] cube) {
		for (int v=0; v<cube.length; v++)
		{
    		for (int y=0; y<cube[0].length; y++)
    		{
        		for (int x=0; x<cube[0][0].length; x++)
        		{
        			if (cube[v][y][x] == blanking) cube[v][y][x] = 0f;
        		}
    		}
		}
		return cube;
    }

    private float[][] setBlankingToZero(float[][] cube) {
		for (int y=0; y<cube[0].length; y++)
		{
    		for (int x=0; x<cube.length; x++)
    		{
    			if (cube[x][y] == blanking) cube[x][y] = 0f;
    		}
		}
		return cube;
    }

    /**
     * Returns the raw data in the cube without any modification. This method
     * reads again the header if
     * the cube data is empty to set the internal variables to their original
     * values, without any smooth.
     * @return The raw data of the cube as an array in 3d, ordered by
     * number of levels, number of columns (DEC), and number of rows (RA).
	 * @throws JPARSECException If an error occurs,
	 * for example if the file is not an lmv one.
     */
    public float[][][] getRawCubeData()
    throws JPARSECException {
    	if (!readingFile || cube != null) return cube;

    	if (readingFile && cube == null) readHeader();

        float ad[][][] = new float[axis3Dim][axis2Dim][axis1Dim];
        try
        {
            bis.seek(INIT);
            bis.skipBytes(512);

            byte abyte0[] = new byte[axis1Dim * axis2Dim * 4];
            int j = 0;
            for(int l = 0; l < axis3Dim; l++)
            {
            	bis.read(abyte0);
                for(int j1 = 0; j1 < axis2Dim; j1++)
                {
                    for(int k1 = 0; k1 < axis1Dim; k1++)
                    {
                        float d = convert.readFloat(abyte0, j % (axis1Dim * axis2Dim * 4));
                        ad[l][j1][k1] = d;
                        j += 4;
                    }
                }
            }
        }
        catch(Exception exception)
        {
			throw new JPARSECException(exception);
        }

    	if (readingFile && cube == null) {
			this.setExtremaData(ad);
			this.setWCS();
    	}

        return ad;
    }

    /**
     * Writes the instance to a .lmv file.
     * @param path Path to the file.
     * @throws JPARSECException If an error occurs.
     */
    public void write(String path) throws JPARSECException
    {
		FileOutputStream fos;
		DataOutputStream file;
		try {
			fos = new FileOutputStream(path);
			file = new DataOutputStream( fos );
			int off = '\0';

			// Write GILDAS image ID header
			String format = "-";
			String gildas = "GILDAS"+format+"IMAGE"; // GILDAS_UVFIL, ...
			Convertible convert = GildasDataFormat.getConvertibleImage(format.charAt(0));
			LMVCube.writeBytes(file, convert, 0, gildas, 12);
			int imageFormat = -11;
			LMVCube.writeBytes(file, convert, 0, imageFormat);
			double k =  2.0 * ((this.axis1Dim * this.axis2Dim * this.axis3Dim * 4.0) / 1024.0);
			int nblocks = 15 + (int) k;
			LMVCube.writeBytes(file, convert, 0, nblocks);
			off += 20;

			// Write header sections
			int delta = '('-off;
			off = '(';
			for (int i=0; i<delta/4; i++)
			{
				LMVCube.writeBytes(file, convert, 0, 0);
			}

			int lengthOfGeneralSection = 116;
			LMVCube.writeBytes(file, convert, 0, lengthOfGeneralSection);
			LMVCube.writeBytes(file, convert, 0, this.numberOfAxes);
			LMVCube.writeBytes(file, convert, 0, this.axis1Dim);
			LMVCube.writeBytes(file, convert, 0, this.axis2Dim);
			LMVCube.writeBytes(file, convert, 0, this.axis3Dim);
			LMVCube.writeBytes(file, convert, 0, this.axis4Dim);
			off += 24;
            for(int i = 0; i < 12; i++)
            {
    			LMVCube.writeBytes(file, convert, 0, this.conversionFormula[i]);
    			off += 8;
            }
			int lengthOfBlankingSection = 8;
			LMVCube.writeBytes(file, convert, 0, lengthOfBlankingSection);
			LMVCube.writeBytes(file, convert, 0, this.blanking);
			LMVCube.writeBytes(file, convert, 0, this.blankingTolerance);
			int lengthOfExtremaSection = 40;
			LMVCube.writeBytes(file, convert, 0, lengthOfExtremaSection);
			LMVCube.writeBytes(file, convert, 0, this.minimumFlux);
			LMVCube.writeBytes(file, convert, 0, this.maximumFlux);
			off += 24;
			for(int j = 0; j < 8; j++)
            {
    			LMVCube.writeBytes(file, convert, 0, this.minimumAndMaximumFluxPositions[j]);
    			off += 4;
            }
			int lengthOfDescriptionSection = 72;
			LMVCube.writeBytes(file, convert, 0, lengthOfDescriptionSection);
			LMVCube.writeBytes(file, convert, 0, this.fluxUnit, 12);
			LMVCube.writeBytes(file, convert, 0, this.axis1Label, 12);
			LMVCube.writeBytes(file, convert, 0, this.axis2Label, 12);
			LMVCube.writeBytes(file, convert, 0, this.axis3Label, 12);
			LMVCube.writeBytes(file, convert, 0, this.axis4Label, 12);
			LMVCube.writeBytes(file, convert, 0, this.coordinateSystem, 12);
			off += 76;
			int lengthOfPositionSection = 48;
			LMVCube.writeBytes(file, convert, 0, lengthOfPositionSection);
			LMVCube.writeBytes(file, convert, 0, this.sourceName, 12);
			off += 16;
			LMVCube.writeBytes(file, convert, 0, this.sourceRA);
			LMVCube.writeBytes(file, convert, 0, this.sourceDEC);
			LMVCube.writeBytes(file, convert, 0, this.sourceGLon);
			LMVCube.writeBytes(file, convert, 0, this.sourceGLat);
			off += 32;
			LMVCube.writeBytes(file, convert, 0, this.epoch);
            int lengthOfProjectionSection = 36;
			LMVCube.writeBytes(file, convert, 0, lengthOfProjectionSection);
			LMVCube.writeBytes(file, convert, 0, this.projectionType.ordinal());
			LMVCube.writeBytes(file, convert, 0, this.axis1Pos);
			LMVCube.writeBytes(file, convert, 0, this.axis2Pos);
			LMVCube.writeBytes(file, convert, 0, this.axis12PA);
			off += 12+24;
			LMVCube.writeBytes(file, convert, 0, this.xAxisID);
			LMVCube.writeBytes(file, convert, 0, this.yAxisID);
            int lengthOfSpectroscopySection = 48;
			LMVCube.writeBytes(file, convert, 0, lengthOfSpectroscopySection);
			LMVCube.writeBytes(file, convert, 0, this.line, 12);
			off += 12+12;
			LMVCube.writeBytes(file, convert, 0, this.freqResolution);
			LMVCube.writeBytes(file, convert, 0, this.imageFrequency);
			LMVCube.writeBytes(file, convert, 0, this.restFreq);
			LMVCube.writeBytes(file, convert, 0, this.velResolution);
			LMVCube.writeBytes(file, convert, 0, this.velOffset);
			LMVCube.writeBytes(file, convert, 0, this.zAxisID);
            int lengthOfResolutionSection = 12;
			LMVCube.writeBytes(file, convert, 0, lengthOfResolutionSection);
			LMVCube.writeBytes(file, convert, 0, this.beamMajor);
			LMVCube.writeBytes(file, convert, 0, this.beamMinor);
			LMVCube.writeBytes(file, convert, 0, this.beamPA);
			off += 12+24+16;
			int lengthOfNoiseSection = 8;
			LMVCube.writeBytes(file, convert, 0, lengthOfNoiseSection);
			LMVCube.writeBytes(file, convert, 0, this.noise);
			LMVCube.writeBytes(file, convert, 0, this.rms);
			int lengthOfProperMotionSection = 12;
			LMVCube.writeBytes(file, convert, 0, lengthOfProperMotionSection);
			LMVCube.writeBytes(file, convert, 0, this.sourceProperMotionRA);
			LMVCube.writeBytes(file, convert, 0, this.sourceProperMotionDEC);
			LMVCube.writeBytes(file, convert, 0, this.sourceParallax);
			off += 12+16;

			// Write cube data
			delta = 512 - off;
			off = 512;
			for (int i=0; i<delta/4; i++)
			{
				LMVCube.writeBytes(file, convert, 0, 0);
			}
            for(int l = 0; l < axis3Dim; l++)
            {
    			float cube[][] = this.getRawCubeData(l);
                for(int j1 = 0; j1 < axis2Dim; j1++)
                {
                	byte b[] = new byte[4 * axis1Dim];
                	byte b0[] = new byte[4];
                    for(int k1 = 0; k1 < axis1Dim; k1++)
                    {
            			//LMVCube.writeBytes(file, convert, 0, cube[k1][j1]);
                    	convert.writeFloat(b0, 0, cube[k1][j1]);
                    	int n = k1*4;
                    	b[n] = b0[0];
                    	b[n+1] = b0[1];
                    	b[n+2] = b0[2];
                    	b[n+3] = b0[3];
                    }
                    file.write(b);
                }
            }
            for (int i=0;i<1920;i++)
            {
            	LMVCube.writeBytes(file, convert, 0, 0);
            }

		} catch (Exception ioe) {
			throw new JPARSECException(ioe);
		}
    }

	private static void writeBytes(DataOutputStream file, Convertible convert, int off, int value)
	throws Exception {
		byte abyte0[] = new byte[4];
		convert.writeInt(abyte0, off, value);
		file.write(abyte0);
	}
	private static void writeBytes(DataOutputStream file, Convertible convert, int off, double value)
	throws Exception {
		byte abyte0[] = new byte[8];
		convert.writeDouble(abyte0, off, value);
		file.write(abyte0);
	}
	private static void writeBytes(DataOutputStream file, Convertible convert, int off, float value)
	throws Exception {
		byte abyte0[] = new byte[4];
		convert.writeFloat(abyte0, off, value);
		file.write(abyte0);
	}
	private static void writeBytes(DataOutputStream file, Convertible convert, int off, String value, int nchar)
	throws Exception {
		if (value.length() > nchar) {
			value = value.substring(0, nchar);
		} else {
			value = FileIO.addSpacesAfterAString(value, nchar);
		}
		file.write(value.getBytes());
	}

    /**
     * Returns the data in the cube for a given velocity plane. This method reads
     * again the header if the cube data is empty to set the internal variables to
     * their original values, without any smooth.
     * @param plane The index of the velocity plane in the file, starting from 0.
     * @return The plane, ordered as axis1, axis2 (oposite as {@linkplain #getCubeData()}).
     * @throws JPARSECException If the plane is invalid.
     */
    public float[][] getRawCubeData(int plane)
    throws JPARSECException {
    	if (plane < 0 || plane >= this.axis3Dim) throw new JPARSECException("Plane "+plane+" does not exist.");
        float ad[][] = new float[axis1Dim][axis2Dim];
    	if (!readingFile || cube != null) {
            for(int j1 = 0; j1 < axis1Dim; j1++)
            {
                for(int k1 = 0; k1 < axis2Dim; k1++)
                {
                	ad[j1][k1] = cube[plane][k1][j1];
                }
            }
    		return ad;
    	}

    	if (readingFile && cube == null) readHeader();

        try
        {
            bis.seek(INIT);
            bis.skipBytes(512);
            byte abyte0[] = new byte[axis1Dim * axis2Dim * 4];
            int j = 0;
            for (int s=0;s<plane+1;s++)
            {
            	bis.read(abyte0);
            }
            for(int j1 = 0; j1 < axis2Dim; j1++)
            {
                for(int k1 = 0; k1 < axis1Dim; k1++)
                {
                    float d = convert.readFloat(abyte0, j % (axis1Dim * axis2Dim * 4));
                    ad[k1][j1] = d;
                    j += 4;
                }
            }
        }
        catch(Exception exception)
        {
			throw new JPARSECException(exception);
        }

    	if (readingFile && cube == null) {
			this.setWCS();
    	}

        return ad;
    }

    /**
     * Returns the data in the cube for a given velocity plane. Possible blanking values
     * are changed to zero. This method reads
     * again the header if the cube data is empty to set the internal variables to
     * their original values, without any smooth.
     * @param plane The index of the velocity plane in the file, starting from 0.
     * @return The plane, ordered as axis1, axis2 (oposite as {@linkplain #getCubeData()}).
     * @throws JPARSECException If the plane is invalid.
     */
    public float[][] getCubeData(int plane)
    throws JPARSECException {
    	if (plane < 0 || plane >= this.axis3Dim) throw new JPARSECException("Plane "+plane+" does not exist.");
        float ad[][] = new float[axis1Dim][axis2Dim];
    	if (!readingFile || cube != null) {
            for(int j1 = 0; j1 < axis1Dim; j1++)
            {
                for(int k1 = 0; k1 < axis2Dim; k1++)
                {
                	ad[j1][k1] = cube[plane][k1][j1];
                }
            }
    		return setBlankingToZero(ad);
    	}

    	if (readingFile && cube == null) readHeader();

        try
        {
            bis.seek(INIT);
            bis.skipBytes(512);
            byte abyte0[] = new byte[axis1Dim * axis2Dim * 4];
            int j = 0;
            for (int s=0;s<plane+1;s++)
            {
                //int i1 =
                	bis.read(abyte0);
            }
            for(int j1 = 0; j1 < axis2Dim; j1++)
            {
                for(int k1 = 0; k1 < axis1Dim; k1++)
                {
                    float d = convert.readFloat(abyte0, j % (axis1Dim * axis2Dim * 4));
                    ad[k1][j1] = d;
                    j += 4;
                }
            }
        }
        catch(Exception exception)
        {
			throw new JPARSECException(exception);
        }

    	if (readingFile && cube == null) {
			this.setWCS();
    	}

        return setBlankingToZero(ad);
    }

    /**
     * Returns the data in a given plane of the cube smoothing it if necessary. The
     * internal variables are modified to properly account for the new size of the
     * cube data.
     * @param plane The index of the velocity plane in the file, starting from 0.
     * @param max1 Maximum number of elements in x axis.
     * @param max2 Maximum number of elements in y axis.
     * @param max3 Maximum number of elements in z axis.
     * @return The plane, ordered as axis1, axis2 (oposite as {@linkplain #getCubeData()}).
     * @throws JPARSECException If the plane is invalid.
     */
    public float[][] getRawCubeData(int plane, int max1, int max2, int max3)
    throws JPARSECException {
    	if (plane < 0 || plane >= this.axis3Dim || plane >= max3) throw new JPARSECException("plane "+plane+" does not exist.");
    	float cube[][][] = getRawCubeData(max1, max2, max3);
    	float p[][] = new float[cube[0][0].length][cube[0].length];
        for(int i = 0; i < p.length; i++)
        {
            for(int j = 0; j < p[0].length; j++)
            {
            	p[i][j] = cube[plane][j][i];
            }
        }
    	return p;
    }

    /**
     * Returns the data in a given plane of the cube smoothing it if necessary. The
     * internal variables are modified to properly account for the new size of the
     * cube data.
     * @param plane The index of the velocity plane in the file, starting from 0.
     * @param max1 Maximum number of elements in x axis.
     * @param max2 Maximum number of elements in y axis.
     * @param max3 Maximum number of elements in z axis.
     * @return The plane, ordered as axis1, axis2 (oposite as {@linkplain #getCubeData()}).
     * @throws JPARSECException If the plane is invalid.
     */
    public float[][] getCubeData(int plane, int max1, int max2, int max3)
    throws JPARSECException {
    	if (plane < 0 || plane >= this.axis3Dim || plane >= max3) throw new JPARSECException("plane "+plane+" does not exist.");
    	float cube[][][] = getCubeData(max1, max2, max3);
    	float p[][] = new float[cube[0][0].length][cube[0].length];
        for(int i = 0; i < p.length; i++)
        {
            for(int j = 0; j < p[0].length; j++)
            {
            	p[i][j] = cube[plane][j][i];
            }
        }
    	return p;
    }

    /**
     * Returns the data in the cube smoothing if necessary. The
     * internal variables are modified to properly account for the
     * new size of the cube data.
     * @param max1 Maximum number of elements in x axis.
     * @param max2 Maximum number of elements in y axis.
     * @param max3 Maximum number of elements in z axis.
     * @return The cube of data as an array in 3d, ordered by
     * number of levels (z), number of columns (DEC, y), and
     * number of rows (RA, x).
     * @throws JPARSECException If an error occurs.
     */
    public float[][][] getRawCubeData(int max1, int max2, int max3)
    throws JPARSECException {
    	if (readingFile && cube == null) readHeader();
    	int reduce1 = 1, reduce2 = 1, reduce3 = 1;
    	int n1 = this.axis1Dim, n2 = this.axis2Dim, n3 = this.axis3Dim;
    	if (max1 < this.axis1Dim) {
    		do {
    			reduce1 ++;
    			n1 = axis1Dim / reduce1;
    		} while (((double) axis1Dim/ (double) reduce1) > max1);
    	}
    	if (max2 < this.axis2Dim) {
    		do {
    			reduce2 ++;
    			n2 = axis2Dim / reduce2;
    		} while (((double) axis2Dim/ (double) reduce2) > max2);
    	}
    	if (max3 < this.axis3Dim) {
    		do {
    			reduce3 ++;
    			n3 = axis3Dim / reduce3;
    		} while (((double) axis3Dim/ (double) reduce3) > max3);
    	}

        float ad_out[][][] = new float[n3][n2][n1];
		for (int v=0; v<n3; v++)
		{
    		for (int y=0; y<n2; y++)
    		{
        		for (int x=0; x<n1; x++)
        		{
        			ad_out[v][y][x] = 0f;
        		}
    		}
		}
    	if (!readingFile || cube != null) {
    		for (int v=0; v<cube.length; v++)
    		{
    			int vi = v / reduce3;
        		for (int y=0; y<cube[0].length; y++)
        		{
        			int yi = y / reduce2;
            		for (int x=0; x<cube[0][0].length; x++)
            		{
            			int xi = x / reduce1;
            			if (cube[v][y][x] != blanking)  {
            				ad_out[vi][yi][xi] += cube[v][y][x] / (float) (reduce2 * reduce1 * reduce3);
            			}
            		}
        		}
    		}
    	} else {
	        float ad[][][] = new float[reduce3][axis2Dim][axis1Dim];
	        try {
	            bis.seek(INIT);
	            bis.skipBytes(512);
	            byte abyte0[] = new byte[axis1Dim * axis2Dim * 4];
	            int j = 0;
	            n3 = -1;
	            for(int l = 0; l < axis3Dim; l++)
	            {
	            	int index3 = l % reduce3;
	            	if (index3 == 0) ad = new float[reduce3][axis2Dim][axis1Dim];

	            	bis.read(abyte0);
	                for(int j1 = 0; j1 < axis2Dim; j1++)
	                {
	                    for(int k1 = 0; k1 < axis1Dim; k1++)
	                    {
	                        float d = convert.readFloat(abyte0, j % (axis1Dim * axis2Dim * 4));
	                        ad[index3][j1][k1] = d;
	                        j += 4;
	                    }
	                }

	                if (index3 == (reduce3-1)) {
	                	n3 ++;
	                    for(int l1 = 0; l1 < reduce3; l1++)
	                    {
	                    	n1 = -1;
		                    for(int j1 = 0; j1 < axis2Dim; j1 = j1 + reduce2)
		                    {
		                    	n1 ++;
		                    	n2 = -1;
		                        for(int k1 = 0; k1 < axis1Dim; k1 = k1 + reduce1)
		                        {
		                        	n2 ++;
		    	                    for(int j2 = 0; j2 < reduce2; j2 ++)
		    	                    {
		    	                        for(int k2 = 0; k2 <reduce1; k2 ++)
		    	                        {
		    	                			if (ad[l1][j1+j2][k1+k2] != blanking)  {
			    	                        	ad_out[n3][n1][n2] += ad[l1][j1+j2][k1+k2] / (float) (reduce2 * reduce1 * reduce3);
		    	                			}
		    	                        }
		    	                    }
		                        }
		                    }
	                    }
	                }
	            }

		    	if (readingFile && cube == null) {
		    		this.setCubeData(ad_out);
		    		this.cube = null;
		    	}

	        } catch(IOException exception) {
				throw new JPARSECException(exception);
	        }

    	}

		for (int v=0; v<n3; v++)
		{
    		for (int y=0; y<n2; y++)
    		{
        		for (int x=0; x<n1; x++)
        		{
        			if (ad_out[v][y][x] == 0f) ad_out[v][y][x] = blanking;
        		}
    		}
		}

        return ad_out;
    }

    /**
     * Returns the data in the cube smoothing if necessary. The output
     * is calculated considering that the blanking value is equal to zero,
     * a requirement if the cube is going to be charted. The
     * internal variables are modified to properly account for the
     * new size of the cube data.
     * @param max1 Maximum number of elements in x axis.
     * @param max2 Maximum number of elements in y axis.
     * @param max3 Maximum number of elements in z axis.
     * @return The cube of data as an array in 3d, ordered by
     * number of levels (z), number of columns (DEC, y), and
     * number of rows (RA, x).
     * @throws JPARSECException If an error occurs.
     */
    public float[][][] getCubeData(int max1, int max2, int max3)
    throws JPARSECException {
    	if (readingFile && cube == null) readHeader();
    	int reduce1 = 1, reduce2 = 1, reduce3 = 1;
    	int n1 = this.axis1Dim, n2 = this.axis2Dim, n3 = this.axis3Dim;
    	if (max1 < this.axis1Dim) {
    		do {
    			reduce1 ++;
    			n1 = axis1Dim / reduce1;
    		} while (((double) axis1Dim/ (double) reduce1) > max1);
    	}
    	if (max2 < this.axis2Dim) {
    		do {
    			reduce2 ++;
    			n2 = axis2Dim / reduce2;
    		} while (((double) axis2Dim/ (double) reduce2) > max2);
    	}
    	if (max3 < this.axis3Dim) {
    		do {
    			reduce3 ++;
    			n3 = axis3Dim / reduce3;
    		} while (((double) axis3Dim/ (double) reduce3) > max3);
    	}

        float ad_out[][][] = new float[n3][n2][n1];
        int ad_out_sum[][][] = new int[n3][n2][n1];
		for (int v=0; v<n3; v++)
		{
    		for (int y=0; y<n2; y++)
    		{
        		for (int x=0; x<n1; x++)
        		{
        			ad_out[v][y][x] = 0f;
        			ad_out_sum[v][y][x] = 0;
        		}
    		}
		}
    	if (!readingFile || cube != null) {
    		for (int v=0; v<cube.length; v++)
    		{
    			int vi = v / reduce3;
        		for (int y=0; y<cube[0].length; y++)
        		{
        			int yi = y / reduce2;
            		for (int x=0; x<cube[0][0].length; x++)
            		{
            			int xi = x / reduce1;
            			if (vi < ad_out.length && yi < ad_out[0].length && xi < ad_out[0][0].length &&
                    			v < cube.length && y < cube[0].length && x < cube[0][0].length) {
	            			if (cube[v][y][x] != blanking)  {
	            				ad_out[vi][yi][xi] += cube[v][y][x];
	            				ad_out_sum[vi][yi][xi] ++;
	            			}
            			}
            		}
        		}
    		}
    		for (int vi=0; vi<ad_out.length; vi++)
    		{
        		for (int yi=0; yi<ad_out[0].length; yi++)
        		{
            		for (int xi=0; xi<ad_out[0][0].length; xi++)
            		{
            			if (ad_out_sum[vi][yi][xi] > 0) ad_out[vi][yi][xi] /= ad_out_sum[vi][yi][xi];
            		}
        		}
    		}
    	} else {
	        float ad[][][] = new float[reduce3][axis2Dim][axis1Dim];
	        try {
	            bis.seek(INIT);
	            bis.skipBytes(512);
	            byte abyte0[] = new byte[axis1Dim * axis2Dim * 4];
	            int j = 0;
	            n3 = -1;
	            for(int l = 0; l < axis3Dim; l++)
	            {
	            	int index3 = l % reduce3;
	            	if (index3 == 0) ad = new float[reduce3][axis2Dim][axis1Dim];

	            	bis.read(abyte0);
	                for(int j1 = 0; j1 < axis2Dim; j1++)
	                {
	                    for(int k1 = 0; k1 < axis1Dim; k1++)
	                    {
	                        float d = convert.readFloat(abyte0, j % (axis1Dim * axis2Dim * 4));
	                        ad[index3][j1][k1] = d;
	                        j += 4;
	                    }
	                }

	                if (index3 == (reduce3-1)) {
	                	n3 ++;
	                    for(int l1 = 0; l1 < reduce3; l1++)
	                    {
	                    	n1 = -1;
		                    for(int j1 = 0; j1 < axis2Dim; j1 = j1 + reduce2)
		                    {
		                    	n1 ++;
		                    	n2 = -1;
		                        for(int k1 = 0; k1 < axis1Dim; k1 = k1 + reduce1)
		                        {
		                        	n2 ++;
		    	                    for(int j2 = 0; j2 < reduce2; j2 ++)
		    	                    {
		    	                        for(int k2 = 0; k2 <reduce1; k2 ++)
		    	                        {
		    	                        	if (n3 < ad_out.length && n1 < ad_out[0].length && n2 < ad_out[0][0].length &&
		    	                        			l1 < ad.length && j1+j2 < ad[0].length && k1+k2 < ad[0][0].length) {
			    	                			if (ad[l1][j1+j2][k1+k2] != blanking)  {
				    	                        	ad_out[n3][n1][n2] += ad[l1][j1+j2][k1+k2] / (float) (reduce2 * reduce1 * reduce3);
			    	                			}
		    	                        	}
		    	                        }
		    	                    }
		                        }
		                    }
	                    }
	                }
	            }

		    	if (readingFile && cube == null) {
		    		this.setCubeData(ad_out);
		    		this.cube = null;
		    	}

	        } catch(IOException exception) {
				throw new JPARSECException(exception);
	        }
    	}

        return ad_out;
    }

    /**
     * Standard labels for x axis for a given coordinate system.
     */
    private static final String COORDINATES_X[] = new String[] {"RA--", "GLON", "ELON"};
    /**
     * Standard labels for y axis for a given coordinate system.
     */
    private static final String COORDINATES_Y[] = new String[] {"DEC-", "GLAT", "ELAT"};
    /**
     * Standard labels for z axis.
     */
    private static final String COORDINATES_Z[] = new String[] {"VELOCITY", "VELOCITY", "VELOCITY"};

    /**
     * Returns the coordinates in the current image.
     * @return ID constant for coordinates type.
     */
    public CoordinateSystem.COORDINATE_SYSTEM getCoordinatesType()
    {
        if(coordinateSystem.substring(0, 8).toLowerCase().equals(CoordinateSystem.COORDINATE_SYSTEMS[COORDINATE_SYSTEM.ECLIPTIC.ordinal()].substring(0, 8).toLowerCase()))
        	return COORDINATE_SYSTEM.ECLIPTIC;

        if(coordinateSystem.substring(0, 8).toLowerCase().equals(CoordinateSystem.COORDINATE_SYSTEMS[COORDINATE_SYSTEM.GALACTIC.ordinal()].substring(0, 8).toLowerCase()))
        	return COORDINATE_SYSTEM.GALACTIC;

        return COORDINATE_SYSTEM.EQUATORIAL;
    }

    /**
     * The set of cube projections.
     */
    public enum PROJECTION {
	    /** ID constant for unknown projection. */
	    UNKNOWN,
	    /** ID constant for tangential projection. */
	    TAN,
	    /** ID constant for sinusoidal projection. */
	    SIN,
	    /** ID constant for azimuthal projection. */
	    AZP,
	    /** ID constant for stereographic (zenithal orthomorphic) projection. */
	    STG,
	    /** Another unknown value from Gildas ... */
	    UNKNOWN2,
	    /** ID constant for Aitoff projection. */
	    AIT,
	    /** ID constant for global sinusoidal projection. */
	    GLS
    };

    /**
     * Names of the projections.
     */
    public static final String PROJECTIONS[] = new String[] {"", "-TAN", "-SIN", "-AZP", "-STG", "", "-AIT", "-GLS"};

    /**
     * Returns the projection name of the current image.
     * @return Projection name.
     */
    public String getProjectionName()
    {
    	return PROJECTIONS[getProjection().ordinal()];
    }

    /**
     * Returns the projection in the current image.
     * @return Projection ID constant.
     */
    public PROJECTION getProjection()
    {
    	return projectionType;
    }

    /**
     * Returns the initial velocity of the cube, third axis.
     * @return Initial velocity.
     */
    public float getv0()
    {
		float v0 = (float) (this.conversionFormula[7] - this.conversionFormula[6] * this.conversionFormula[8]);
		return (float) (v0 + this.conversionFormula[8]);
    }

    /**
     * Returns the final velocity of the cube, third axis.
     * @return Final velocity.
     */
    public float getvf()
    {
		float v0 = (float) (this.conversionFormula[7] - this.conversionFormula[6] * this.conversionFormula[8]);
		float vf = (float) (v0 + this.conversionFormula[8] * this.axis3Dim);
		return vf;
    }

    /**
     * Sets the reference velocity for this cube.
     * @param v The new reference velocity in km/s.
     */
    public void setReferenceVelocity(double v) {
    	conversionFormula[7] = v;
    }

    /**
     * Gets the reference velocity for this cube.
     * @return The reference velocity in km/s.
     */
    public double getReferenceVelocity() {
    	return conversionFormula[7];
    }

    /**
     * Returns the initial x position of the cube, first axis.
     * @return Initial position.
     */
    public float getx0()
    {
		float x0 = (float) (this.conversionFormula[1] - this.conversionFormula[0] * this.conversionFormula[2]);
		return (float) (x0 + this.conversionFormula[2]);
    }

    /**
     * Returns the final x position of the cube, first axis.
     * @return Final position.
     */
    public float getxf()
    {
		float x0 = (float) (this.conversionFormula[1] - this.conversionFormula[0] * this.conversionFormula[2]);
		float xf = (float) (x0 + this.conversionFormula[2] * this.axis1Dim);
		return xf;
    }

    /**
     * Returns the initial y position of the cube, second axis.
     * @return Initial position.
     */
    public float gety0()
    {
		float y0 = (float) (this.conversionFormula[4] - this.conversionFormula[3] * this.conversionFormula[5]);
		return (float) (y0 + this.conversionFormula[5]);
    }

    /**
     * Returns the final y position of the cube, second axis.
     * @return Final position.
     */
    public float getyf()
    {
		float y0 = (float) (this.conversionFormula[4] - this.conversionFormula[3] * this.conversionFormula[5]);
		float yf = (float) (y0 + this.conversionFormula[5] * this.axis2Dim);
		return yf;
    }

    /**
     * Updates the WCS in case the array {@linkplain #conversionFormula} has been
     * modified by hand.
     * @throws JPARSECException If an error occurs.
     */
    public void updateWCS() throws JPARSECException {
    	setWCS();
    }

    private void setWCS() throws JPARSECException
    {
    	wcs = new WCS();
        double crpix1 = conversionFormula[0] - 1.0 - conversionFormula[1] / conversionFormula[2];
        double crpix2 = conversionFormula[3] - 1.0 - conversionFormula[4] / conversionFormula[5];
        wcs.setCrpix1(crpix1);
        wcs.setCrpix2(crpix2);
        double crval1 = axis1Pos * Constant.RAD_TO_DEG;
        double crval2 = axis2Pos * Constant.RAD_TO_DEG;
        double crdelt1 = this.conversionFormula[2] * Constant.RAD_TO_DEG;
        double crdelt2 = this.conversionFormula[5] * Constant.RAD_TO_DEG;
        wcs.setCrval1(crval1);
        wcs.setCdelt1(crdelt1);
        wcs.setCrval2(crval2);
        wcs.setCdelt2(crdelt2);
        wcs.setCoordinateSystem(getCoordinatesType());
        wcs.setEpoch(this.epoch);
        wcs.setEquinox(wcs.getEquinox());
        String pr = getProjection().name();
        if (pr.startsWith("UNKNOWN")) {
        	pr = "TAN";
        	JPARSECException.addWarning("Unknown projection type. Value forced to TAN projection.");
        }
        wcs.setProjection(WCS.PROJECTION.valueOf(pr));
        wcs.setWidth(this.axis1Dim);
        wcs.setHeight(this.axis2Dim);
    }

    /**
     * Returns the WCS instance smoothed to a given
     * resolution. This method uses the current WCS
     * instance and cube data to do the transformation
     * to use coordinates in a different datacube.
     * @param dim1 Dimension in axis 1 (RA).
     * @param dim2 Dimension in axis 2 (DEC).
     * @return The WCS instance.
     * @throws JPARSECException  If an error occurs.
     */
    public WCS getWCS(int dim1, int dim2) throws JPARSECException
    {
    	double ratio1 = (double) this.axis1Dim / (double) dim1;
    	double ratio2 = (double) this.axis2Dim / (double) dim2;
    	//ratio1 = (int) (ratio1+0.5);
    	//ratio2 = (int) (ratio2+0.5);
    	double newDelt1 = this.wcs.getCdelt1() * ratio1;
    	double newDelt2 = this.wcs.getCdelt2() * ratio2;
    	double newPix1 = (wcs.getCrpix1() + 1) / ratio1 + (ratio1 - 1.0) / (2.0 * ratio1) - 1;
    	double newPix2 = (wcs.getCrpix2() + 1) / ratio2 + (ratio2 - 1.0) / (2.0 * ratio2) - 1;

    	WCS wcs = this.wcs.clone();
        wcs.setCrpix1(newPix1);
        wcs.setCrpix2(newPix2);
        wcs.setCdelt1(newDelt1);
        wcs.setCdelt2(newDelt2);
        return wcs;
    }

    /**
     * Sets the datacube to a given cube data, changing the WCS
     * data, the extrema (minimum/maximum fluxes and their
     * positions), the conversion formula, and the resolution in
     * velocity and frequency accordingly. This method implies
     * that the input file (if any) will not be read again. To read it
     * again set the input cube to null.
     * @param cube The cube data.
	 * @throws JPARSECException If an error occurs,
	 * for example if the file is not an lmv one.
     */
    public void setCubeData(float[][][] cube)
    throws JPARSECException {
    	if (cube != null) {
        	this.cube = cube.clone();
	    	int dim1 = cube[0][0].length;
	    	int dim2 = cube[0].length;
	    	int dim3 = cube.length;
	    	this.wcs = this.getWCS(dim1, dim2);
	    	double ratio1 = (double) this.axis1Dim / (double) dim1;
	    	double ratio2 = (double) this.axis2Dim / (double) dim2;
	    	double ratio3 = (double) this.axis3Dim / (double) dim3;
	    	//ratio1 = (int) (ratio1+0.5);
	    	//ratio2 = (int) (ratio2+0.5);
	    	//ratio3 = (int) (ratio3+0.5);
	    	this.velResolution = this.velResolution * (float) ratio3;
	    	this.freqResolution = this.freqResolution * (float) ratio3;
	    	this.noise = (float) Math.sqrt(this.noise * noise / (ratio1*ratio2*ratio3));
	    	this.rms = (float) Math.sqrt(this.rms * rms / (ratio1*ratio2*ratio3));
	    	this.axis1Dim = dim1;
	    	this.axis2Dim = dim2;
	    	this.axis3Dim = dim3;
	    	this.setExtremaData(cube);
	    	this.conversionFormula[0] = this.conversionFormula[0] / ratio1 + (ratio1 - 1.0) / (2.0 * ratio1);
	    	this.conversionFormula[2] = this.conversionFormula[2] * ratio1;
	    	this.conversionFormula[3] = this.conversionFormula[3] / ratio2 + (ratio2 - 1.0) / (2.0 * ratio2);
	    	this.conversionFormula[5] = this.conversionFormula[5] * ratio2;
	    	this.conversionFormula[6] = this.conversionFormula[6] / ratio3 + (ratio3 - 1.0) / (2.0 * ratio3);
	    	this.conversionFormula[8] = this.conversionFormula[8] * ratio3;
    	} else {
    		this.cube = null;
    		readFile(this.path);
    	}
    }

    /**
     * Flips the cube to get positive x, y, and z increments.
     * When this method is called from a cube read from file,
     * the {@linkplain #cube} property is set to the cube data
     * to avoid reading the file again later, so memory is
     * consumed. If this is a problem it is better to avoid calling
     * this method, since the VISAD and SGT graphic libraries
     * re-scale automatically the data if a negative increment
     * is found.
     * @throws JPARSECException If an error occurs.
     */
    public void FlipCubeToGetPositiveIncrements()
    throws JPARSECException {
    	if (cube == null) cube = this.getRawCubeData();
    	float limits[] = new float[] {this.getx0(), this.getxf(), this.gety0(), this.getyf(),
    			this.getv0(), this.getvf()};
		  float initX = limits[0];
		  float finalX = limits[1];
		  float initY = limits[2];
		  float finalY = limits[3];
		  float initZ = limits[4];
		  float finalZ = limits[5];
		  this.freqResolution = Math.abs(this.freqResolution);
		  this.velResolution = Math.abs(this.velResolution);

		  boolean changed = false;
		  if (initZ > finalZ) {
			  cube = this.invertVelocity(cube);
			  float aux = initZ;
			  initZ = finalZ;
			  finalZ = aux;
			  this.conversionFormula[6] = axis3Dim + 1.0 - this.conversionFormula[6];
			  //this.conversionFormula[7] = initZ;
			  this.conversionFormula[8] = Math.abs(this.conversionFormula[8]);
			  changed = true;
		  }
		  if (initX > finalX) {
			  cube = this.invertXAxis(cube);
			  float aux = initX;
			  initX = finalX;
			  finalX = aux;
			  this.conversionFormula[0] = 1;
			  this.conversionFormula[1] = initX;
			  this.conversionFormula[2] = Math.abs(this.conversionFormula[2]);
			  changed = true;
		  }
		  if (initY > finalY) {
			  cube = this.invertYAxis(cube);
			  float aux = initY;
			  initY = finalY;
			  finalY = aux;
			  this.conversionFormula[3] = 1;
			  this.conversionFormula[4] = initY;
			  this.conversionFormula[5] = Math.abs(this.conversionFormula[5]);
			  changed = true;
		  }

		 if (changed) setWCS();
    }

    private float[][][] invertVelocity(float cube[][][])
    {
    	float out[][][] = new float[cube.length][cube[0].length][cube[0][0].length];
    	for (int v=0; v<out.length; v++)
    	{
    		out[v] = cube[out.length-1-v].clone();
    	}
    	return out;
    }

    private float[][][] invertXAxis(float cube[][][])
    {
    	float out[][][] = new float[cube.length][cube[0].length][cube[0][0].length];
    	for (int v=0; v<out.length; v++)
    	{
        	for (int y=0; y<out[0].length; y++)
        	{
            	for (int x=0; x<out[0][0].length; x++)
            	{
            		out[v][y][x] = cube[v][y][out[0][0].length-1-x];
            	}
        	}
    	}
    	return out;
    }

    private float[][][] invertYAxis(float cube[][][])
    {
    	float out[][][] = new float[cube.length][cube[0].length][cube[0][0].length];
    	for (int v=0; v<out.length; v++)
    	{
        	for (int y=0; y<out[0].length; y++)
        	{
        		out[v][y] = cube[v][out[0].length-1-y].clone();
        	}
    	}
    	return out;
    }

    /**
     * Transforms a given offset respect to the central position in the map to
     * its (x, y) position in the cube matrix.
     * @param offsetRA Offset in RA relative to center, in radians (sky plain, not offset in coordinates).
     * @param offsetDEC Offset in DEC relative to center, in radians.
     * @return Position (x, y) in the cube matrix. This position can be used
     * directly with a WCS transform.
     */
    public double[] getPositionFromOffset(double offsetRA, double offsetDEC)
    {
		double dx = (offsetRA - this.getx0()) / (this.getxf() - this.getx0());
		double dy = (offsetDEC - this.gety0()) / (this.getyf() - this.gety0());
		double xp = ((this.axis1Dim-1.0) * dx);
		double yp = ((this.axis2Dim-1.0) * (1.0 - dy));
		double px = xp;
		double py = this.axis2Dim - 1 - yp;
		return new double[] {px, py};
    }

    /**
     * Transforms a given position in the cube matrix to coordinate offsets.
     * @param px Position in the matrix in x axis (RA), from 0 to axis1Dim-1.
     * @param py Position in the matrix in y axis (DEC), from 0 to axis2Dim-1.
     * @return Offsets in RA and DEC, radians.
     */
    public double[] getOffsetFromPosition(double px, double py)
    {
		double dx = px / (axis1Dim - 1.0);
		double dy = 1.0 + (py + 1.0 - axis2Dim) / (axis2Dim - 1.0);
		double offsetRA = this.getx0() + dx * (this.getxf() - this.getx0());
		double offsetDEC = this.gety0() + dy * (this.getyf() - this.gety0());
		return new double[] {offsetRA, offsetDEC};
    }

    /**
     * Resamples the cube to a different resolution using a high-quality 2d spline
     * interpolation method (function {@linkplain ImageSplineTransform#resize(int, int)}). Note that
     * 3d interpolation is not supported, so
     * the velocity axis is not resampled. Each velocity plane is
     * treated separately. Note also that errors could arise if no
     * enough memory is available.
     * After this is done, the cube can no longer be read again from a file, it will
     * be stored in memory.
     * @param w New width (axis 1 or RA).
     * @param h New height (axis 2 or DEC).
     * @param rawData True to process raw data, false to consider
     * blanking = 0.
     * @throws JPARSECException If an error occurs.
     */
    public void resample(int w, int h, boolean rawData) throws JPARSECException
    {
    	float out[][][] = new float[this.axis3Dim][h][w];
    	for (int k=0; k<this.axis3Dim; k++) {
    		float[][] cube = this.getCubeData(k);
    		if (rawData) cube = this.getRawCubeData(k);
        	double[][] plane = new double[cube.length][cube[0].length];
        	for (int i=0; i<cube.length; i++) {
            	for (int j=0; j<cube[0].length; j++) {
            		plane[i][j] = cube[i][j];
            	}
        	}
        	ImageSplineTransform t = new ImageSplineTransform(plane);
        	t.resize(w, h);
        	plane = t.getImage();
        	out[k] = new float[plane[0].length][plane.length];
        	for (int j=0; j<plane[0].length; j++) {
            	for (int i=0; i<plane.length; i++) {
            		out[k][j][i] = (float) plane[i][j];
            	}
        	}
    	}
    	this.setCubeData(out);
    	this.readingFile = false;
    }
    /**
     * Resamples the cube to a different resolution using a high-quality 2d spline
     * interpolation method (function {@linkplain ImageSplineTransform#resize(int, int)}).
     * Velocity axis is resampled using linear/spline interpolation. Note also that errors could arise if no
     * enough memory is available.
     * After this is done, the cube can no longer be read again from a file, it will
     * be stored in memory.
     * @param w New width (axis 1 or RA).
     * @param h New height (axis 2 or DEC).
     * @param l New resolution (axis 3 or VEL).
     * @param spline True to use spline interpolation, false for linear.
     * @param rawData True to process raw data, false to consider
     * blanking = 0.
     * @throws JPARSECException If an error occurs.
     */
    public void resample(int w, int h, int l, boolean spline, boolean rawData) throws JPARSECException
    {
    	float out[][][] = new float[this.axis3Dim][h][w];
    	for (int k=0; k<this.axis3Dim; k++) {
    		float[][] cube = this.getCubeData(k);
    		if (rawData) cube = this.getRawCubeData(k);
        	double[][] plane = new double[cube.length][cube[0].length];
        	for (int i=0; i<cube.length; i++) {
            	for (int j=0; j<cube[0].length; j++) {
            		plane[i][j] = cube[i][j];
            	}
        	}
        	ImageSplineTransform t = new ImageSplineTransform(plane);
        	t.resize(w, h);
        	plane = t.getImage();
        	out[k] = new float[plane[0].length][plane.length];
        	for (int j=0; j<plane[0].length; j++) {
            	for (int i=0; i<plane.length; i++) {
            		out[k][j][i] = (float) plane[i][j];
            	}
        	}
    	}
    	if (l != this.axis3Dim) {
    		float out2[][][] = new float[l][h][w];
    		double iny[] = new double[out.length];
    		double inx[] = new double[out.length];
        	for (int i=0; i<out[0].length; i++) {
            	for (int j=0; j<out[0][0].length; j++) {
                	for (int k=0; k<iny.length; k++) {
                		iny[k] = out[k][i][j];
                		inx[k] = k;
                	}
                	Interpolation interp = new Interpolation(inx, iny, true);
                	for (int k=0; k<l; k++) {
                		double newx = (double) k * (iny.length - 1.0) / (l - 1.0);
                		if (spline) {
                			out2[k][i][j] = (float) interp.splineInterpolation(newx);
                		} else {
                			out2[k][i][j] = (float) interp.linearInterpolation(newx);
                		}
                	}
            	}
        	}
        	out = out2;
    	}

    	this.setCubeData(out);
    	this.readingFile = false;
    }

    /**
     * Returns the integrated intensity of the cube.
     * @return The integrated intensity as a float[][] object,
     * ordered as [i][j] (different from the ordering of the cube data!).
     * @throws JPARSECException If an error occurs.
     */
    public float[][] integratedIntensity() throws JPARSECException
    {
    	float[][][] cube = this.getCubeData();
    	float out[][] = new float[cube[0][0].length][cube[0].length];
    	for (int j=0; j<cube[0].length; j++) {
        	for (int i=0; i<cube[0][0].length; i++) {
        		out[i][j] = 0f;
        	}
    	}
    	float vfac = Math.abs(velResolution);
    	if (cube.length == 1) vfac = 1;
    	int nwar = 0, nwm = 10;
    	for (int k=0; k<cube.length; k++) {
        	float[][] cubeP = this.getCubeData(k);
        	for (int i=0; i<cubeP.length; i++) {
            	for (int j=0; j<cubeP[0].length; j++) {
        			if (!Double.isInfinite(cubeP[i][j]) && !Double.isNaN(cubeP[i][j])) {
        				out[i][j] += cubeP[i][j] * vfac;
        			} else {
        				if (nwar < nwm) {
	        				JPARSECException.addWarning("infinite or NaN found in position "+i+" "+j+" "+k);
	        				nwar ++;
	        				if (nwar == nwm) JPARSECException.addWarning("No more warnings like this will be thrown");
	        				//JPARSECException.addWarning("infinite or NaN found in position "+i+" "+j+" "+k+". Integrated intensity set to zero.");
	        				//out[i][j] = 0; // Float.NaN;
        				}
        			}
            	}
        	}
    	}
    	return out;
    }

    /**
     * Returns the integrated intensity of the cube.
     * @param minIntensity The minimum intensity to consider a given channel and
     * add its intensity to the output integrated intensity map. This value
     * is like a 3 sigma threshold.
     * @return The integrated intensity as a float[][] object,
     * ordered as [i][j] (different from the ordering of the cube data!).
     * @throws JPARSECException If an error occurs.
     */
    public float[][] integratedIntensity(double minIntensity) throws JPARSECException
    {
    	float[][][] cube = this.getCubeData();
    	float out[][] = new float[cube[0][0].length][cube[0].length];
    	for (int j=0; j<cube[0].length; j++) {
        	for (int i=0; i<cube[0][0].length; i++) {
        		out[i][j] = 0f;
        	}
    	}
    	float vfac = Math.abs(velResolution);
    	if (cube.length == 1) vfac = 1;
    	int nwar = 0, nwm = 10;
    	for (int k=0; k<cube.length; k++) {
        	float[][] cubeP = this.getCubeData(k);
        	for (int i=0; i<cubeP.length; i++) {
            	for (int j=0; j<cubeP[0].length; j++) {
        			if (!Double.isInfinite(cubeP[i][j]) && !Double.isNaN(cubeP[i][j])) {
        				if (cubeP[i][j] > minIntensity) out[i][j] += cubeP[i][j] * vfac;
        			} else {
        				if (nwar < nwm) {
	        				JPARSECException.addWarning("infinite or NaN found in position "+i+" "+j+" "+k);
	        				nwar ++;
	        				if (nwar == nwm) JPARSECException.addWarning("No more warnings like this will be thrown");
	        				//JPARSECException.addWarning("infinite or NaN found in position "+i+" "+j+" "+k+". Integrated intensity set to zero.");
	        				//out[i][j] = 0; // Float.NaN;
        				}
        			}
            	}
        	}
    	}
    	return out;
    }

    /**
     * Returns the integrated intensity of the cube between a given range of channels.
     * @param chan0 First channel for the integrated intensity, from 0 to {@linkplain #axis3Dim}-1.
     * @param chanf Last channel for the integrated intensity, from 0 to {@linkplain #axis3Dim}-1.
     * @return The integrated intensity as a float[][] object,
     * ordered as [i][j] (different from the ordering of the cube data!).
     * @throws JPARSECException If an error occurs, for instance for an invalid range of channels.
     */
    public float[][] integratedIntensity(int chan0, int chanf) throws JPARSECException
    {
    	return integratedIntensity(chan0, chanf, -1E300);
    }

    /**
     * Returns the integrated intensity of the cube between a given range of channels.
     * @param chan0 First channel for the integrated intensity, from 0 to {@linkplain #axis3Dim}-1.
     * @param chanf Last channel for the integrated intensity, from 0 to {@linkplain #axis3Dim}-1.
     * @param threeSigma Value for 3 sigma threshold. All channels with intensity greater or equal to this
     * value will be considered with adding flux to the integrated intensity.
     * @return The integrated intensity as a float[][] object,
     * ordered as [i][j] (different from the ordering of the cube data!).
     * @throws JPARSECException If an error occurs, for instance for an invalid range of channels.
     */
    public float[][] integratedIntensity(int chan0, int chanf, double threeSigma) throws JPARSECException
    {
    	float[][][] cube = this.getCubeData();
    	float out[][] = new float[cube[0][0].length][cube[0].length];
    	for (int j=0; j<cube[0].length; j++) {
        	for (int i=0; i<cube[0][0].length; i++) {
        		out[i][j] = 0f;
        	}
    	}
    	float vfac = Math.abs(velResolution);
    	if (cube.length == 1) vfac = 1;
    	int nwar = 0, nwm = 10;
    	if (chan0 < 0 || chanf < 0 || chan0 >= axis3Dim || chanf >= axis3Dim)
    		throw new JPARSECException("Invalid range of channels "+chan0+"-"+chanf+". Should be between 0-"+(axis3Dim-1)+".");
    	for (int k=chan0; k<=chanf; k++) {
        	float[][] cubeP = this.getCubeData(k);
        	for (int i=0; i<cubeP.length; i++) {
            	for (int j=0; j<cubeP[0].length; j++) {
        			if (!Double.isInfinite(cubeP[i][j]) && !Double.isNaN(cubeP[i][j])) {
        				if (cubeP[i][j] >= threeSigma)
        					out[i][j] += cubeP[i][j] * vfac;
        			} else {
        				if (nwar < nwm) {
	        				JPARSECException.addWarning("infinite or NaN found in position "+i+" "+j+" "+k);
	        				nwar ++;
	        				if (nwar == nwm) JPARSECException.addWarning("No more warnings like this will be thrown");
	        				//JPARSECException.addWarning("infinite or NaN found in position "+i+" "+j+" "+k+". Integrated intensity set to zero.");
	        				//out[i][j] = 0; // Float.NaN;
        				}
        			}
            	}
        	}
    	}
    	return out;
    }

    /**
     * Returns the integrated intensity of the cube between a given range of channels.
     * @param maxX Maximum dimension of axis 1.
     * @param maxY Maximum dimension of axis 2.
     * @param maxZ Maximum dimension of axis 3.
     * @param chan0 First channel for the integrated intensity, from 0 to {@linkplain #axis3Dim}-1.
     * @param chanf Last channel for the integrated intensity, from 0 to {@linkplain #axis3Dim}-1.
     * @return The integrated intensity as a float[][] object,
     * ordered as [i][j] (different from the ordering of the cube data!).
     * @throws JPARSECException If an error occurs, for instance for an invalid range of channels.
     */
    public float[][] integratedIntensity(int maxX, int maxY, int maxZ, int chan0, int chanf) throws JPARSECException
    {
    	return integratedIntensity(maxX, maxY, maxZ, chan0, chanf, -1E300);
    }

    /**
     * Returns the integrated intensity of the cube between a given range of channels.
     * @param maxX Maximum dimension of axis 1.
     * @param maxY Maximum dimension of axis 2.
     * @param maxZ Maximum dimension of axis 3.
     * @param chan0 First channel for the integrated intensity, from 0 to {@linkplain #axis3Dim}-1.
     * @param chanf Last channel for the integrated intensity, from 0 to {@linkplain #axis3Dim}-1.
     * @param threeSigma Value for 3 sigma threshold. All channels with intensity greater or equal to this
     * value will be considered with adding flux to the integrated intensity.
     * @return The integrated intensity as a float[][] object,
     * ordered as [i][j] (different from the ordering of the cube data!).
     * @throws JPARSECException If an error occurs, for instance for an invalid range of channels.
     */
    public float[][] integratedIntensity(int maxX, int maxY, int maxZ, int chan0, int chanf, double threeSigma) throws JPARSECException
    {
    	float[][][] cube = this.getCubeData(maxX, maxY, maxZ);
    	float out[][] = new float[cube[0][0].length][cube[0].length];
    	for (int j=0; j<cube[0].length; j++) {
        	for (int i=0; i<cube[0][0].length; i++) {
        		out[i][j] = 0f;
        	}
    	}
    	float vfac = Math.abs(velResolution);
    	if (cube.length == 1) vfac = 1;
    	int nwar = 0, nwm = 10;
    	if (chan0 < 0 || chanf < 0 || chan0 >= axis3Dim || chanf >= axis3Dim)
    		throw new JPARSECException("Invalid range of channels "+chan0+"-"+chanf+". Should be between 0-"+(axis3Dim-1)+".");
    	for (int k=chan0; k<=chanf; k++) {
        	float[][] cubeP = this.getCubeData(k);
        	for (int i=0; i<cubeP.length; i++) {
            	for (int j=0; j<cubeP[0].length; j++) {
        			if (!Double.isInfinite(cubeP[i][j]) && !Double.isNaN(cubeP[i][j])) {
        				if (cubeP[i][j] >= threeSigma)
        					out[i][j] += cubeP[i][j] * vfac;
        			} else {
        				if (nwar < nwm) {
	        				JPARSECException.addWarning("infinite or NaN found in position "+i+" "+j+" "+k);
	        				nwar ++;
	        				if (nwar == nwm) JPARSECException.addWarning("No more warnings like this will be thrown");
	        				//JPARSECException.addWarning("infinite or NaN found in position "+i+" "+j+" "+k+". Integrated intensity set to zero.");
	        				//out[i][j] = 0; // Float.NaN;
        				}
        			}
            	}
        	}
    	}
    	return out;
    }

    /**
     * Returns the integrated intensity of the cube.
     * @param chan0 First channel index for the integrated intensity, from 0 to {@linkplain #axis3Dim}-1.
     * @param chanf Last channel index for the integrated intensity, from 0 to {@linkplain #axis3Dim}-1.
     * @return The integrated intensity as a float[][] object,
     * ordered as [j][i] (same from the ordering of the cube data!).
     * @throws JPARSECException If an error occurs, for instance for an invalid range of channels.
     */
    public float[][] integratedIntensityKeepOrdering(int chan0, int chanf) throws JPARSECException
    {
    	return integratedIntensityKeepOrdering(chan0, chanf, -1E300);
    }

    /**
     * Returns the integrated intensity of the cube.
     * @param chan0 First channel index for the integrated intensity, from 0 to {@linkplain #axis3Dim}-1.
     * @param chanf Last channel index for the integrated intensity, from 0 to {@linkplain #axis3Dim}-1.
     * @param threeSigma Value for 3 sigma threshold. All channels with intensity greater or equal to this
     * value will be considered with adding flux to the integrated intensity.
     * @return The integrated intensity as a float[][] object,
     * ordered as [j][i] (same from the ordering of the cube data!).
     * @throws JPARSECException If an error occurs, for instance for an invalid range of channels.
     */
    public float[][] integratedIntensityKeepOrdering(int chan0, int chanf, double threeSigma) throws JPARSECException
    {
    	if (chan0 < 0 || chanf < 0 || chan0 >= axis3Dim || chanf >= axis3Dim)
    		throw new JPARSECException("Invalid range of channels "+chan0+"-"+chanf+". Should be between 0-"+(axis3Dim-1)+".");
    	float[][][] cube = this.getCubeData();
    	float out[][] = new float[cube[0].length][cube[0][0].length];
    	for (int j=0; j<cube[0].length; j++) {
        	for (int i=0; i<cube[0][0].length; i++) {
        		out[j][i] = 0f;
        	}
    	}
    	float vfac = Math.abs(velResolution);
    	if (cube.length == 1) vfac = 1;
    	int nwar = 0, nwm = 10;
    	for (int k=chan0; k<=chanf; k++) {
        	float[][] cubeP = this.getCubeData(k);
        	for (int i=0; i<cubeP.length; i++) {
            	for (int j=0; j<cubeP[0].length; j++) {
        			if (!Double.isInfinite(cubeP[i][j]) && !Double.isNaN(cubeP[i][j])) {
        				if (cubeP[i][j] >= threeSigma)
        					out[j][i] += cubeP[i][j] * vfac;
        			} else {
        				if (nwar < nwm) {
	        				JPARSECException.addWarning("infinite or NaN found in position "+j+" "+i+" "+k);
	        				nwar ++;
	        				if (nwar == nwm) JPARSECException.addWarning("No more warnings like this will be thrown");
	        				//JPARSECException.addWarning("infinite or NaN found in position "+j+" "+i+" "+k+". Integrated intensity set to zero.");
	        				//out[i][j] = 0;
        				}
        			}
            	}
        	}
    	}
    	return out;
    }

    /**
     * Returns the integrated intensity of the cube smoothing if necessary.
     * @param maxX Maximum dimension of axis 1.
     * @param maxY Maximum dimension of axis 2.
     * @param maxZ Maximum dimension of axis 3.
     * @return The integrated intensity as a float[][] object,
     * ordered as [i][j].
     * @throws JPARSECException If an error occurs.
     */
    public float[][] integratedIntensity(int maxX, int maxY, int maxZ) throws JPARSECException
    {
    	float[][][] cube = this.getCubeData(maxX, maxY, maxZ);
    	float out[][] = new float[cube[0][0].length][cube[0].length];
    	for (int j=0; j<cube[0].length; j++) {
        	for (int i=0; i<cube[0][0].length; i++) {
        		out[i][j] = 0f;
        	}
    	}
    	float vfac = Math.abs(velResolution);
    	if (cube.length == 1) vfac = 1;
    	for (int k=0; k<cube.length; k++) {
        	for (int i=0; i<cube[0][0].length; i++) {
            	for (int j=0; j<cube[0].length; j++) {
            		out[i][j] += cube[k][j][i] * vfac;
            	}
        	}
    	}
    	return out;
    }

    /**
     * Returns the integrated intensity of the cube smoothing if necessary.
     * @param maxX Maximum dimension of axis 1.
     * @param maxY Maximum dimension of axis 2.
     * @param maxZ Maximum dimension of axis 3.
     * @param threeSigma Value for 3 sigma threshold. All channels with intensity greater or equal to this
     * value will be considered with adding flux to the integrated intensity. This is
     * applied after the smoothing in case maxX, maxY, or maxZ are lower than the current
     * size of the cube.
     * @return The integrated intensity as a float[][] object,
     * ordered as [i][j].
     * @throws JPARSECException If an error occurs.
     */
    public float[][] integratedIntensity(int maxX, int maxY, int maxZ, double threeSigma) throws JPARSECException
    {
    	float[][][] cube = this.getCubeData(maxX, maxY, maxZ);
    	float out[][] = new float[cube[0][0].length][cube[0].length];
    	for (int j=0; j<cube[0].length; j++) {
        	for (int i=0; i<cube[0][0].length; i++) {
        		out[i][j] = 0f;
        	}
    	}
    	float vfac = Math.abs(velResolution);
    	if (cube.length == 1) vfac = 1;
    	for (int k=0; k<cube.length; k++) {
        	for (int i=0; i<cube[0][0].length; i++) {
            	for (int j=0; j<cube[0].length; j++) {
            		if (cube[k][j][i] >= threeSigma)
            			out[i][j] += cube[k][j][i] * vfac;
            	}
        	}
    	}
    	return out;
    }

    /**
     * Scales the intensities in a given cube multiplying them by a given factor.
     * @param cube The cube to be modified.
     * @param blanking Blanking value to be conserved. If the cube was obtained from
     * the raw cube data, the value would be the blanking value of the lmv cube instance,
     * if not, this value should be zero.
     * @param scale Value to multiply intensities greater than the blanking value.
     * @return The new cube.
     */
    public static float[][][] scaleIntensity(float[][][] cube, float blanking, float scale) {
		for (int v=0; v<cube.length; v++)
		{
    		for (int y=0; y<cube[0].length; y++)
    		{
        		for (int x=0; x<cube[0][0].length; x++)
        		{
        			if (cube[v][y][x] != blanking) cube[v][y][x] *= scale;
        		}
    		}
		}
		return cube;
    }

    /**
     * Adds the data in a cube to another, cell by cell. Cubes should have the same
     * size, but no check is done.
     * @param cube1 The first cube.
     * @param cube2 The second cube.
     * @param fixNaN True to consider a NaN value as 0 in the input cubes, false to keep
     * all NaNs. Even selecting true output cube could contain NaNs in case both input
     * cubes contain NaN for a given channel/DEC/RA position.
     * @return The new cube.
     */
    public static float[][][] addCube(float[][][] cube1, float[][][] cube2, boolean fixNaN) {
    	float[][][] cube = cube1.clone();
		for (int v=0; v<cube.length; v++)
		{
    		for (int y=0; y<cube[0].length; y++)
    		{
        		for (int x=0; x<cube[0][0].length; x++)
        		{
        			cube[v][y][x] = Float.NaN;
        			float out1 = cube1[v][y][x];
        			float out2 = cube2[v][y][x];
        			if ((Float.isInfinite(out1) || Float.isNaN(out1)) &&
        					(Float.isInfinite(out2) || Float.isNaN(out2))) continue;

        			if (fixNaN && (Float.isInfinite(out1) || Float.isNaN(out1))) out1 = 0.0f;
        			if (fixNaN && (Float.isInfinite(out2) || Float.isNaN(out2))) out2 = 0.0f;
        			cube[v][y][x] = out1 + out2;
        		}
    		}
		}
		return cube;
    }

    /**
     * Convolves this cube of data supposing that there's no emission
     * in the offset 0 0, or it is already considered.
     * A Gaussian convolution kernel is used, and points outside the map
     * are considered. All posible NaNs are removed.
     * @param maxX Maximum dimension of axis 1.
     * @param maxY Maximum dimension of axis 2.
     * @param maxZ Maximum dimension of axis 3.
	 * @param beam_x Beam major axis size in arcseconds.
	 * @param beam_y Beam minor axis size in arcseconds.
	 * @param beam_pa Beam position angle in degrees, same Gildas criteria.
     * @return The new convolved cube. The data returned is not applied
     * to the current cube, which is left unmodified.
     * @throws JPARSECException If an error occurs.
     */
    public float[][][] convolve(int maxX, int maxY, int maxZ, double beam_x, double beam_y, double beam_pa) throws JPARSECException {
    	float[][][] inCube = this.getCubeData(maxX, maxY, maxZ).clone();
    	float[][][] outCube = new float[inCube.length][inCube[0].length][inCube[0][0].length];
    	double spatialResolution = Math.abs(conversionFormula[2]) * Constant.RAD_TO_ARCSEC;
    	double kernel[][] = Table.convolveGetGaussianKernel(beam_x, beam_y, beam_pa, spatialResolution, 4);
    	for (int y=0; y<outCube[0].length; y++)
		{
    		for (int x=0; x<outCube[0][0].length; x++)
    		{
    			float data[] = Table.convolveMap(inCube, kernel, x, y, null, 0, true);
    			for (int i=0; i<data.length; i++) {
    				outCube[i][y][x] = data[i];
    			}
    		}
		}
		return outCube;
    }

    /**
     * Convolves this cube of data supposing that there's no emission
     * in the offset 0 0, or it is already considered.
     * A Gaussian convolution kernel is used, and points outside the map
     * are considered. All posible NaNs are removed.
	 * @param beam_x Beam major axis size in arcseconds.
	 * @param beam_y Beam minor axis size in arcseconds.
	 * @param beam_pa Beam position angle in degrees, same Gildas criteria.
     * @return The new convolved cube. The data returned is not applied
     * to the current cube, which is left unmodified.
     * @throws JPARSECException If an error occurs.
     */
    public float[][][] convolve(double beam_x, double beam_y, double beam_pa) throws JPARSECException {
    	float[][][] inCube = this.getCubeData().clone();
    	float[][][] outCube = new float[inCube.length][inCube[0].length][inCube[0][0].length];
    	double kernel[][] = Table.convolveGetGaussianKernel(beam_x, beam_y, beam_pa, this.getSpatialResolution(), 4);
    	for (int y=0; y<outCube[0].length; y++)
		{
    		for (int x=0; x<outCube[0][0].length; x++)
    		{
    			float data[] = Table.convolveMap(inCube, kernel, x, y, null, 0, true);
    			for (int i=0; i<data.length; i++) {
    				outCube[i][y][x] = data[i];
    			}
    		}
		}
		return outCube;
    }

    /**
     * Returns the spatial resolution of the cube in arcseconds.
     * @return Spatial resolution in arcseconds.
     */
    public double getSpatialResolution() {
    	return Math.abs(conversionFormula[2]) * Constant.RAD_TO_ARCSEC;
    }

    /**
     * Convolves this cube using the supplied kernel. Points outside the map
     * are considered. All posible NaNs are removed.
	 * @param kernel The kernel array.
     * @return The new convolved cube. The data returned is not applied
     * to the current cube, which is left unmodified.
     * @throws JPARSECException If an error occurs.
     */
    public float[][][] convolve(double kernel[][]) throws JPARSECException {
    	float[][][] inCube = this.getCubeData().clone();
    	float[][][] outCube = new float[inCube.length][inCube[0].length][inCube[0][0].length];
    	for (int y=0; y<outCube[0].length; y++)
		{
    		for (int x=0; x<outCube[0][0].length; x++)
    		{
    			float data[] = Table.convolveMap(inCube, kernel, x, y, null, 0, true);
    			for (int i=0; i<data.length; i++) {
    				outCube[i][y][x] = data[i];
    			}
    		}
		}
		return outCube;
    }

    /**
     * Scales the intensities in a given cube multiplying them by a given factor.
     * After this is done, the cube can no longer be read again from a file, it will
     * be stored in memory.
     * @param scale Value to multiply intensities greater than the blanking value.
     * @throws JPARSECException If an error occurs.
     */
    public void scaleIntensity(float scale) throws JPARSECException {
    	this.setCubeData(this.getRawCubeData());
    	this.readingFile = false;
		for (int v=0; v<cube.length; v++)
		{
    		for (int y=0; y<cube[0].length; y++)
    		{
        		for (int x=0; x<cube[0][0].length; x++)
        		{
        			if (cube[v][y][x] != blanking) cube[v][y][x] *= scale;
        		}
    		}
		}
		setExtremaData();
    }

    /**
     * Scales the intensities in a given cube by applying a given formula to them.
     * After this is done, the cube can no longer be read again from a file, it will
     * be stored in memory.
     * @param formula An expression in Java language as a function of variable 'x', which
     * is the intensity at any position in the cube.
     * @throws JPARSECException If an error occurs.
     */
    public void scaleIntensity(String formula) throws JPARSECException {
    	this.setCubeData(this.getRawCubeData());
    	this.readingFile = false;
    	Evaluation eval = new Evaluation(formula, new String[] {"x 0"});
		for (int v=0; v<cube.length; v++)
		{
    		for (int y=0; y<cube[0].length; y++)
    		{
        		for (int x=0; x<cube[0][0].length; x++)
        		{
        			if (cube[v][y][x] != blanking) {
        				eval.resetVariable("x", cube[v][y][x]);
        				cube[v][y][x] = (float) eval.evaluate();
        			}
        		}
    		}
		}
		setExtremaData();
    }

    /**
     * Clips a given cube to get a region inside it. After clipped, the cube
     * can no longer be read again from a file, it will be stored in memory.
     * @param inix Initial x index. First is 0.
     * @param endx Ending x index.
     * @param iniy Initial y index.
     * @param endy Ending y index.
     * @param iniz Initial z index.
     * @param endz Ending z index.
     * @throws JPARSECException If an error occurs.
     */
    public void clip(int inix, int endx, int iniy, int endy, int iniz, int endz) throws JPARSECException {
		setCubeData(getRawCubeData());
		readingFile = false;

		float originalCube[][][] = cube.clone();
		float cubeData[][][] = new float[endz-iniz+1][endy-iniy+1][endx-inix+1];
		for (int v=0; v<cubeData.length; v++)
		{
    		for (int y=0; y<cubeData[0].length; y++)
    		{
        		for (int x=0; x<cubeData[0][0].length; x++)
        		{
        			cubeData[v][y][x] = originalCube[v+iniz][y+iniy][x+inix];
        		}
    		}
		}
		cube = cubeData;
    	this.axis1Dim = cube[0][0].length;
    	this.axis2Dim = cube[0].length;
    	this.axis3Dim = cube.length;
    	this.setExtremaData(cube);
    	this.conversionFormula[0] = this.conversionFormula[0]-inix;
    	this.conversionFormula[3] = this.conversionFormula[3]-iniy;
    	this.conversionFormula[6] = this.conversionFormula[6]-iniz;
    	setWCS();
    }

    /**
     * Returns a chart with certain spectrum.
     * @param xindex Index in RA, from 0 to axis1Dim-1.
     * @param yindex Index in DEC, from 0 to axis2Dim-1.
     * @param width Chart width in pixels.
     * @param height Chart height in pixels.
     * @param xUnit Unit for x axis. Note here the corrected
     * velocity mode is not supported.
     * @return The chart.
     * @throws JPARSECException If an error occurs.
     */
    public CreateChart getChart(int xindex, int yindex, int width, int height, XUNIT xUnit)
    throws JPARSECException {
    	float[][][] cube = this.getCubeData();
    	double y[] = new double[cube.length];
    	for (int i=0; i<y.length; i++) {
    		y[i] = cube[i][yindex][xindex];
    	}
    	cube = null;
    	double x[] = new double[y.length];

    	double refchan = 0.0, vref = 0.0, vres = 0.0, rfreq = 0.0;
    	try {
 	    	refchan = this.conversionFormula[6];
	    	vref = this.conversionFormula[7];
	    	vres = this.conversionFormula[8];
	       	rfreq = this.restFreq;
    	} catch (Exception exc) {}

    	for (int i=0; i<y.length; i++)
    	{
    		double v = vref + (i - refchan) * vres;
    		switch (xUnit)
    		{
    		case CHANNEL_NUMBER:
    			x[i] = i;
    			break;
    		case VELOCITY_KMS:
        		x[i] = v;
        		break;
    		case FREQUENCY_MHZ:
    	    	double delta = (v - vref) / vres;
    	    	double fres = - vres * rfreq / (Constant.SPEED_OF_LIGHT * 0.001);
    	    	x[i] = rfreq + delta * fres;
    			break;
//    		case FREQUENCY_MHZ:
//        		x[i] = rfreq * (1.0 / (1.0 + v * 1000.0 / Constant.SPEED_OF_LIGHT));
//    			break;
    		default:
    			throw new JPARSECException("invalid value for x axis units.");
    		}
    	}

    	String title = this.sourceName;
    	String legend = this.line;
		SimpleChartElement chart1 = new SimpleChartElement(ChartElement.TYPE.XY_CHART,
				ChartElement.SUBTYPE.XY_SCATTER, x, y, title, Translate.translate(292)+" (km s^{-1})",
				"T_{mb} ("+this.fluxUnit.trim()+")", legend, true, false,
				width, height);
		switch (xUnit)
		{
		case CHANNEL_NUMBER:
			chart1.xLabel = Translate.translate(293);
			break;
		case FREQUENCY_MHZ:
//		case GILDAS_FREQUENCY_MHZ:
			chart1.xLabel = Translate.translate(294)+" (MHz)";
			break;
		default:
			break;
		}

		ChartElement chart = ChartElement.parseSimpleChartElement(chart1);
		chart.series[0].showLines = true;
		chart.series[0].showShapes = false;
		chart.series[0].showErrorBars = false;
		chart.series[0].stroke = JPARSECStroke.STROKE_DEFAULT_LINE_THIN;
		chart.showErrorBars = false;
		CreateChart ch = new CreateChart(chart);
		return ch;
    }

    /**
     * Returns a spectrum for a given position.
     * @param xindex Index in RA, from 0 to axis1Dim-1.
     * @param yindex Index in DEC, from 0 to axis2Dim-1.
     * @return The spectrum.
     * @throws JPARSECException If an error occurs, for instance if
     * the cube doesn't include valid information about reference
     * frequency, velocity, or channel.
     */
    public Spectrum getSpectrum(int xindex, int yindex)
    throws JPARSECException {
    	return getSpectrum(xindex, yindex, true);
    }

    /**
     * Returns a spectrum for a given position. Observing time is set to J2000 epoch.
     *
     * Note the backend is named to 30M, but should be named correctly for Doppler corrections (default is correct for 30M).
     * One of these: 'BURE','PDBI','NOEMA','PICOVELETA','VELETA', 'ACA','ALMA','APEX','ATF','CARMA',
     * 'CSO','EFFELSBERG','FCRAO','GBT','GI2T','HHT','IOTA','JCMT','KITTPEAK','KOSMA','LASILLA','MAUNAKEA',
     * 'MEDICINA','NRO','NOBEYAMA','PARANAL','PTI','SEST','SMA','SMT','VLA','VLT','YEBES'
     *
     * @param xindex Index in RA, from 0 to axis1Dim-1.
     * @param yindex Index in DEC, from 0 to axis2Dim-1.
     * @param applyBlanking True to apply blanking when requesting cube data (slow).
     * @return The spectrum.
     * @throws JPARSECException If an error occurs, for instance if
     * the cube doesn't include valid information about reference
     * frequency, velocity, or channel.
     */
    public Spectrum getSpectrum(int xindex, int yindex, boolean applyBlanking)
    throws JPARSECException {
    	float[][][] cube = this.cube;
    	if (applyBlanking || cube == null) cube = this.getCubeData(applyBlanking);

    	double refchan = 0.0, vref = 0.0, vres = 0.0, rfreq = 0.0;
    	try {
 	    	refchan = this.conversionFormula[6];
	    	vref = this.conversionFormula[7];
	    	vres = this.conversionFormula[8];
	       	rfreq = this.restFreq;
    	} catch (Exception exc) {
    		throw new JPARSECException("Could not read values for reference channel, velocity, and frequency.", exc);
    	}

    	FluxElement flux[] = new FluxElement[cube.length];
    	for (int i=0; i<flux.length; i++)
    	{
    		flux[i] = new FluxElement(new MeasureElement(i+1, 0, null), new MeasureElement(cube[i][yindex][xindex], 0, "K"));
    	}

    	Spectrum s = new Spectrum(flux);
		s.line = this.line;
		s.source = this.sourceName;
		s.backend = "30M";
		s.ra = sourceRA;
		s.dec = sourceDEC;
		s.epochJD = Constant.J2000 + (epoch - 2000.0) * Constant.JULIAN_DAYS_PER_CENTURY * 0.01;
		s.sigmaRMS = rms;
		s.imgFrequency = imageFrequency;
    	s.referenceChannel = refchan;
    	s.referenceFrequency = rfreq;
    	s.referenceVelocity = vref;
    	s.velocityResolution = vres;
    	s.observingTimeJD = Constant.J2000;
    	double off[] = this.getOffsetFromPosition(xindex, yindex);
    	s.offsetX = off[0] * Constant.RAD_TO_ARCSEC;
    	s.offsetY = off[1] * Constant.RAD_TO_ARCSEC;
		return s;
    }

	/**
	 * Convolves the current map with a given beam profile and at a given position in the grid.
     * A Gaussian convolution kernel is used, and points outside the map
     * are considered. All posible NaNs are removed.<BR>
     *
     * Note the backend is named to CONVOLVED, but should be named correctly for Doppler corrections.
     * One of these: 'BURE','PDBI','NOEMA','PICOVELETA','VELETA', 'ACA','ALMA','APEX','ATF','CARMA',
     * 'CSO','EFFELSBERG','FCRAO','GBT','GI2T','HHT','IOTA','JCMT','KITTPEAK','KOSMA','LASILLA','MAUNAKEA',
     * 'MEDICINA','NRO','NOBEYAMA','PARANAL','PTI','SEST','SMA','SMT','VLA','VLT','YEBES'
	 *
	 * @param beam_x Beam major axis size in arcseconds.
	 * @param beam_y Beam minor axis size in arcseconds.
	 * @param beam_pa Beam position angle in degrees, same Gildas criteria.
	 * @param index_x_obs The x offset where the convolution will be done. Between 0 and the size of the grid in x axis minus 1.
	 * @param index_y_obs The y offset where the convolution will be done. Between 0 and the size of the grid in y axis minus 1.
	 * @return Convolved spectrum.
	 * @throws JPARSECException If an error occurs.
	 */
 	public Spectrum convolve(double beam_x, double beam_y, double beam_pa, int index_x_obs, int index_y_obs) throws JPARSECException
 	{
 		double spatialResolution = Math.abs(conversionFormula[2]) * Constant.RAD_TO_ARCSEC;
 		float out[] = Table.convolveMap(this.getCubeData(), Table.convolveGetGaussianKernel(beam_x, beam_y, beam_pa, spatialResolution, 4),
 				index_x_obs, index_y_obs, null, 0, true);
		int resolution = this.axis3Dim;

		FluxElement fluxes[] = new FluxElement[resolution];
		double v0 = this.getv0();
		for (int i=0; i<resolution; i++)
		{
			double v = v0+i*this.velResolution;
			MeasureElement mx = new MeasureElement(v, 0, MeasureElement.UNIT_X_KMS);
			MeasureElement my = new MeasureElement(out[i], 0, MeasureElement.UNIT_Y_K);
			fluxes[i] = new FluxElement(mx, my);
		}
		Spectrum outSpectrum = new Spectrum(fluxes);

		// Set other properties in my spectrum
		outSpectrum.observationNumber = 1;
		outSpectrum.offsetX = outSpectrum.offsetY = 0;
		outSpectrum.line = this.line;
		outSpectrum.backend = "CONVOLVED";
		outSpectrum.referenceVelocity = conversionFormula[7];
		outSpectrum.referenceChannel = conversionFormula[6];
		outSpectrum.referenceFrequency = this.restFreq;
		outSpectrum.source = this.sourceName;
		outSpectrum.velocityResolution = this.velResolution;
		outSpectrum.ra = sourceRA;
		outSpectrum.dec = sourceDEC;
		outSpectrum.epochJD = Constant.J2000 + (epoch - 2000.0) * Constant.JULIAN_DAYS_PER_CENTURY * 0.01;
		outSpectrum.sigmaRMS = rms;
		outSpectrum.imgFrequency = imageFrequency;
    	double off[] = this.getOffsetFromPosition(index_x_obs, index_y_obs);
    	outSpectrum.offsetX = off[0] * Constant.RAD_TO_ARCSEC;
    	outSpectrum.offsetY = off[1] * Constant.RAD_TO_ARCSEC;
		return outSpectrum;
	}



	/**
	 * Reprojects the cube to the resolution and beam of another one.
	 * Reprojection in spatial coordinates only, velocity axis is not changed.
	 * @param ref The reference cube.
	 * @throws JPARSECException If an error occurs.
	 */
	public void reproject(LMVCube ref) throws JPARSECException {
		// Convolution. Note this should be called BEFORE reproject for better results in case
		// input map is bigger than the one to reproject to
		double beamRef = ref.beamMajor * Constant.RAD_TO_ARCSEC;
		double beam_x = Math.sqrt(beamRef * beamRef - FastMath.pow(beamMajor * Constant.RAD_TO_ARCSEC, 2));
		double beam_y = beam_x, beam_pa = 0;

		if (!Double.isNaN(beam_x) && beam_x >= 0.5) {
			setCubeData(convolve(beam_x, beam_y, beam_pa));
		} else {
			JPARSECException.addWarning("This cube ("+sourceName+", "+line+", "+(beamMajor * Constant.RAD_TO_ARCSEC)+"\") will not be convolved since its resolution is equal/lower than the reference one.");
		}

		// Reprojection
		float refX0 = ref.getx0(), refXF = ref.getxf();
		double refDX = ref.conversionFormula[2];
		float refY0 = ref.gety0(), refYF = ref.getyf();
		double refDY = ref.conversionFormula[5];

		double offx = 0, offy = 0;
		if (axis1Pos != ref.axis1Pos) {
			offx = -(axis1Pos - ref.axis1Pos) / refDX;
			if (offx > axis1Dim) offx = 0;
		}
		if (axis2Pos != ref.axis2Pos) {
			offy = -(axis2Pos - ref.axis2Pos) / refDY;
			if (offy > axis2Dim) offy = 0;
		}
		offx *= refDX;
		offy *= refDY;

		float[][][] cubeData = new float[axis3Dim][ref.axis2Dim][ref.axis1Dim];
		for (int z=0; z<axis3Dim; z++) {
			CreateGridChart GC = getChart(z);
			int xi = -1;
			double x = refX0;
			while (true) {
				xi ++;
				int yi = -1;
				double y = refY0;
				while (true) {
					yi ++;
					double f = GC.getIntensityAt((x + offx) * Constant.RAD_TO_ARCSEC, (y + offy) * Constant.RAD_TO_ARCSEC);
					cubeData[z][yi][xi] = (float) f;

					y += refDY;
					if ((refDY > 0 && y > refYF+refDY*0.5) || (refDY < 0 && y < refYF+refDY*0.5)) break;
				}

				x += refDX;
				if ((refDX > 0 && x > refXF+refDX*0.5) || (refDX < 0 && x < refXF+refDX*0.5)) break;
			}
		}

		setCubeData(cubeData);
		for (int i=0; i<6; i++) {
			conversionFormula[i] = ref.conversionFormula[i];
		}
		updateWCS();
	}

	/**
	 * Reprojects the cube to the range and resolution of another one, only in
	 * the velocity axis.
	 * @param ref The reference cube.
	 * @throws JPARSECException If an error occurs.
	 */
	public void reprojectInVelocity(LMVCube ref) throws JPARSECException {
		double rv0 = ref.getv0(), rvf = ref.getvf(), rvres = (rvf - rv0) / (ref.axis3Dim - 1.0);
		double v0 = getv0(), vf = getvf(); //, vres = (vf - v0) / (axis3Dim - 1.0);
		int new3Dim = ref.axis3Dim;
		float data2[][][] = new float[new3Dim][this.axis2Dim][this.axis1Dim];

		for (int i=0; i<data2[0][0].length; i++) {
			for (int j=0; j<data2[0].length; j++) {
				Table t = this.getSpectrum(i, j, false).getAsTable();
				for (int k=0; k<data2.length; k++) {
					double v = rv0 + k * rvres;
					double pix = (axis3Dim - 1.0) * (v - v0) / (vf - v0);
					data2[k][j][i] = 0;
					if (pix >= 0 && pix <= (axis3Dim - 1.0)) {
						int n = 0;
						double value = 0;
						for (double off=-0.5;off<=0.55;off=off+0.25) {
							double pix0 = pix + off;
							if (pix0 >= 0 && pix0 <= (axis3Dim - 1.0)) {
								n ++;
								value += t.interpolate(pix0, 0, 0).getValue();
							}
						}
						data2[k][j][i] = (float) (value / n);
					}
				}
			}
		}

		this.setCubeData(data2);
		this.conversionFormula[6] = ref.conversionFormula[6];
		this.conversionFormula[7] = ref.conversionFormula[7];
		this.conversionFormula[8] = ref.conversionFormula[8];
		this.velResolution = (float) ref.conversionFormula[8];
	}

	/**
	 * Reprojects the current cube to the spatial resolucion, velocity range,
	 * and frecuency/LSR velocity values of another cube. After this
	 * process, the resulting cube will match channel/index by channel/index
	 * the provided one in terms of spatial coordinates and velocity for
	 * a given channel.
	 * @param lmv The reference cube.
	 * @throws JPARSECException If an error occurs.
	 */
	public void reprojectAll(LMVCube lmv) throws JPARSECException {
		reproject(lmv);
		float pdata[][][] = getCubeData();
		Spectrum30m ps30m = null;
		for (int x=0; x<axis1Dim; x++) {
			for (int y=0; y<axis2Dim; y++) {
				Spectrum30m s30m = new Spectrum30m(getSpectrum(x, y, false));
				if (ps30m == null) ps30m = s30m;
				s30m.modifyRestFrequency(lmv.restFreq);
				s30m.modifyVelocityLSR(lmv.conversionFormula[7]);
				float[] npdata = s30m.getSpectrumData();
				for (int z=0; z<npdata.length; z++) {
					pdata[z][y][x] = npdata[z];
				}
			}
		}
		restFreq = ps30m.getReferenceFrequency();
		conversionFormula[6] = ps30m.getReferenceChannel();
		conversionFormula[7] = ps30m.getReferenceVelocity();
		conversionFormula[8] = ps30m.getVelocityResolution();
		velOffset = (float) conversionFormula[7];
		setCubeData(pdata);
		reprojectInVelocity(lmv);
	}

	/**
	 * Corrects the cube for primary beam.
	 * @param pbeam The primary beam to use in arcsec, or 0 or lower to calculate it
	 * automatically for PdBI.
	 * @param pradius The radius of the region to modify in arcsec, or &lt;= 0 to
	 * apply the primary beam correction to the entire spatial region covered by
	 * the cube.
	 * @throws JPARSECException If an error occurs.
	 */
	public void correctForPrimaryBeam(double pbeam, double pradius) throws JPARSECException {
		if (pbeam <= 0) pbeam = 115.0 * 43 / (this.restFreq * 0.001); // 43" at 115 GHz
		double sampling = 0.5 * Math.abs(getx0() - getxf()) / (pbeam * Constant.ARCSEC_TO_RAD);
		double primaryBeam[][] = Table.convolveGetGaussianKernel(pbeam, pbeam,
				0, getSpatialResolution(), sampling);
		float data[][][] = getCubeData();
		ImageSplineTransform ist = new ImageSplineTransform(2, primaryBeam);
		for (int j=0; j<data[0].length; j++) {
			for (int i=0; i<data[0][0].length; i++) {
				if (pradius > 0) {
					double r = Constant.RAD_TO_ARCSEC * Math.abs(conversionFormula[2]) * Math.sqrt(FastMath.pow(j-data[0].length/2.0-0.5, 2)+FastMath.pow(i-data[0][0].length/2.0-0.5, 2));
					if (r > pradius) continue;
				}
				double val = ist.interpolate(i + 0.5,  j + 0.5);
				if (val != 0.0) {
					for (int k=0; k<data.length; k++) {
							data[k][j][i] /= val;
					}
				} else {
					for (int k=0; k<data.length; k++) {
						data[k][j][i] = Float.NaN;
					}
				}
			}
		}
		setCubeData(data);
	}

 	/**
 	 * Returns a chart of a given plane or integrated intensity.
 	 * @param plane Plane number (first is 0), or -1 for integrated intensity.
 	 * @return The chart.
 	 * @throws JPARSECException If an error occurs.
 	 */
 	public CreateGridChart getChart(int plane) throws JPARSECException {
		String unit = fluxUnit.trim();
		if (unit.equals("Jy/beam")) unit = "Jy beam^{-1}";
		String l = "#"+plane;
		if (plane == -1) l = Translate.translate(925);

		float x0 = (float) (getx0() * Constant.RAD_TO_ARCSEC);
		float xf = (float) (getxf() * Constant.RAD_TO_ARCSEC);
		float y0 = (float) (gety0() * Constant.RAD_TO_ARCSEC);
		float yf = (float) (getyf() * Constant.RAD_TO_ARCSEC);
		float data[][] = null;
		if (plane == -1) {
			data = this.integratedIntensity();
			unit += " (km/s)";
		} else {
			data = this.getCubeData(plane);
		}
		GridChartElement chart = new GridChartElement(l,
				this.axis1Label.trim()+" offset (\")", this.axis2Label.trim()+" offset (\")", unit,
				GridChartElement.COLOR_MODEL.BLUE_TO_RED,
				new double[] {x0, xf, y0, yf}, DataSet.toDoubleArray(data),
				null, 600);
		chart.type = TYPE.RASTER_CONTOUR;
		chart.levelsOrientation = GridChartElement.WEDGE_ORIENTATION.HORIZONTAL_BOTTOM;
		chart.levelsBorderStyle = GridChartElement.WEDGE_BORDER.NO_BORDER;

		CreateGridChart gridChart = new CreateGridChart(chart);

		return gridChart;
 	}
 	
 	/**
 	 * Returns a chart of the integrated intensity around a given range of planes.
 	 * @param plane0 First plane number (first is 0) to start integrating the emission.
 	 * @param plane1 Last plane index for integrating the emission.
 	 * @return The chart.
 	 * @throws JPARSECException If an error occurs.
 	 */
 	public CreateGridChart getChart(int plane0, int plane1) throws JPARSECException {
		String unit = fluxUnit.trim();
		if (unit.equals("Jy/beam")) unit = "Jy beam^{-1}";
		String l = "#"+plane0+"-"+plane1;

		float x0 = (float) (getx0() * Constant.RAD_TO_ARCSEC);
		float xf = (float) (getxf() * Constant.RAD_TO_ARCSEC);
		float y0 = (float) (gety0() * Constant.RAD_TO_ARCSEC);
		float yf = (float) (getyf() * Constant.RAD_TO_ARCSEC);
		float data[][] = null;
		data = this.integratedIntensity(plane0, plane1);
		unit += " (km/s)";
		GridChartElement chart = new GridChartElement(l,
				this.axis1Label.trim()+" offset (\")", this.axis2Label.trim()+" offset (\")", unit,
				GridChartElement.COLOR_MODEL.BLUE_TO_RED,
				new double[] {x0, xf, y0, yf}, DataSet.toDoubleArray(data),
				null, 600);
		chart.type = TYPE.RASTER_CONTOUR;
		chart.levelsOrientation = GridChartElement.WEDGE_ORIENTATION.HORIZONTAL_BOTTOM;
		chart.levelsBorderStyle = GridChartElement.WEDGE_BORDER.NO_BORDER;

		CreateGridChart gridChart = new CreateGridChart(chart);

		return gridChart;
 	}

 	/**
 	 * Returns the spectra along a given strip of this cube between two equatorial positions. The strip is
 	 * created by considering all grid points that lies in a straight line between the center of
 	 * the pixel position corresponding to the beginning of the strip, and the center of the pixel
 	 * for the ending of the strip. No interpolation or data transformation is done.
 	 * @param loc0 Position where the strip begins.
 	 * @param loc1 Position where the strip ends.
 	 * @return The set of spectra, or null if there are no pixels between initial and ending positions.
 	 * @throws JPARSECException If any of the input positions is outside the image.
 	 */
 	public Spectrum30m[] getStrip(LocationElement loc0, LocationElement loc1)
 	throws JPARSECException {
 		double pos0[] = new double[] {loc0.getLongitude() - this.axis1Pos, loc0.getLatitude() - this.axis2Pos};
 		double pos1[] = new double[] {loc1.getLongitude() - this.axis1Pos, loc1.getLatitude() - this.axis2Pos};
 		if (pos0[0] > Math.PI) pos0[0] -= Constant.TWO_PI;
 		if (pos1[0] > Math.PI) pos1[0] -= Constant.TWO_PI;

 		double cosDec = Math.cos(axis2Pos);
 		pos0[0] *= cosDec;
 		pos1[0] *= cosDec;

		double matrixPos0[] = getPositionFromOffset(pos0[0], pos0[1]);
		double matrixPos1[] = getPositionFromOffset(pos1[0], pos1[1]);

		int stripn[] = new int[] {(int) (matrixPos0[0] + 0.5), (int) (matrixPos0[1] + 0.5),
				(int) (matrixPos1[0] + 0.5), (int) (matrixPos1[1] + 0.5)};

		int np = (int) (10.0 * FastMath.hypot(stripn[2]-stripn[0], stripn[3]-stripn[1]));
		double stepX = (double) (stripn[2] - stripn[0]) / (double) (np-1);
		double stepY = (double) (stripn[3] - stripn[1]) / (double) (np-1);

		float cube[][][] = getCubeData();
		int px = -1, py = -1;
		ArrayList<Object> v = new ArrayList<Object>();
		double offr0 = -1, offrf = -1, posr0 = 0;
		posr0 = cube[0].length/2.0 - 0.5;
		for (int p=0; p<np; p++)
		{
			double xp = stripn[0] + 0.5 + stepX * p;
			double yp = stripn[1] + 0.5 + stepY * p;
			if ((int) xp != px || (int) yp != py) {
				px = (int) xp;
				py = (int) yp;
				offrf = FastMath.hypot(px-posr0, py-posr0);
				if (offr0 == -1) offr0 = offrf;

				v.add(new Spectrum30m(this.getSpectrum(px, py)));
			}
		}

		if (v.size() == 0) return null;

		return (Spectrum30m[]) DataSet.toObjectArray(v);
 	}

 	/**
 	 * Increases or reduces the extension of the input locations to match the size or limits
 	 * of the cube data, so that a strip can be obtained. Input location objects are modified
 	 * so that the strip conserves its direction.
 	 * @param loc0 Starting position for the strip, to be corrected.
 	 * @param loc1 Final position for the strip, to be corrected.
 	 * @param onlyReduce True to modify the strip limits only in case any of the input limits
 	 * lies outside the cube data, so a correction is strickly required to avoid an error when
 	 * calling {@linkplain #getStrip(LocationElement, LocationElement, double[], boolean, boolean, boolean)}.
 	 */
 	public void getStripLimits(LocationElement loc0, LocationElement loc1, boolean onlyReduce)
 	{
 		double pos0[] = new double[] {loc0.getLongitude() - this.axis1Pos, loc0.getLatitude() - this.axis2Pos};
 		double pos1[] = new double[] {loc1.getLongitude() - this.axis1Pos, loc1.getLatitude() - this.axis2Pos};
 		if (pos0[0] > Math.PI) pos0[0] -= Constant.TWO_PI;
 		if (pos1[0] > Math.PI) pos1[0] -= Constant.TWO_PI;

 		double cosDec = Math.cos(axis2Pos);
 		pos0[0] *= cosDec;
 		pos1[0] *= cosDec;

		double matrixPos0[] = getPositionFromOffset(pos0[0], pos0[1]);
		double matrixPos1[] = getPositionFromOffset(pos1[0], pos1[1]);

		if (onlyReduce && matrixPos0[0] >= 0 && matrixPos0[1] >= 0 && matrixPos1[0] >= 0 &&
				matrixPos1[1] >= 0 && matrixPos0[0] <= (this.axis1Dim-1) && matrixPos1[0] <= (this.axis1Dim-1)
				&& matrixPos0[1] <= (this.axis2Dim-1) && matrixPos1[1] <= (this.axis2Dim-1)) return;

		double m = (matrixPos1[1] - matrixPos0[1]) / (matrixPos1[0] - matrixPos0[0]);
		double n = matrixPos1[1] - m * matrixPos1[0];

		if (Math.abs(m) >= 1) { // High inclination => limit in y
			double x0 = -n / m; // for y = 0
			double xf = (this.axis2Dim - 1.0 - n) / m; // for y = ymax
			matrixPos0[0] = x0;
			matrixPos1[0] = xf;
			matrixPos0[1] = m * x0 + n;
			matrixPos1[1] = m * xf + n;
		} else { // limit in x
			double y0 = n; // for x = 0
			double yf = m * (this.axis1Dim - 1.0) + n; // for x = xmax
			matrixPos0[0] = 0;
			matrixPos1[0] = this.axis1Dim - 1.0;
			matrixPos0[1] = y0;
			matrixPos1[1] = yf;
		}

		double off0[] = this.getOffsetFromPosition(matrixPos0[0], matrixPos0[1]);
		double off1[] = this.getOffsetFromPosition(matrixPos1[0], matrixPos1[1]);
		off0[0] /= cosDec;
		off1[0] /= cosDec;
		loc0.setLongitude(this.axis1Pos + off0[0]);
		loc1.setLongitude(this.axis1Pos + off1[0]);
		loc0.setLatitude(this.axis2Pos + off0[1]);
		loc1.setLatitude(this.axis2Pos + off1[1]);
 	}

 	/**
 	 * Creates a chart with a strip of this cube between two equatorial positions. The strip is
 	 * created by considering all grid points that lies in a straight line between the center of
 	 * the pixel position corresponding to the beginning of the strip, and the center of the pixel
 	 * for the ending of the strip. No interpolation or data transformation is done unless interpolation
 	 * is allowed.
 	 * @param loc0 Position where the strip begins.
 	 * @param loc1 Position where the strip ends.
 	 * @param contours Optional contour levels for the output chart. Set to null for no contours.
 	 * @param usePhysicalOffsets True to show the offsets in the vertical axis in arcseconds instead of
 	 * as grid offset, in both cases respect the center defined in the next input parameter.
 	 * @param offsetsRespectCenter True to return offsets range respect the center of the defined strip, false
 	 * to return them respect the center of the cube.
 	 * @param interpolate True to interpolate in-between pixels, false to return the observed true data
 	 * for each pixel, despite the strip will no pass exactly through the center of the pixels.
 	 * @return The output chart, or null if there are no pixels between initial and ending positions.
 	 * @throws JPARSECException If any of the input positions is outside the image.
 	 */
 	public CreateGridChart getStrip(LocationElement loc0, LocationElement loc1, double[] contours, boolean usePhysicalOffsets,
 			boolean offsetsRespectCenter, boolean interpolate)
 	throws JPARSECException {
 		double dist = LocationElement.getAngularDistance(loc0, loc1) * Constant.RAD_TO_ARCSEC * 0.5;
 		if (!usePhysicalOffsets) dist /= (Math.abs(conversionFormula[2]) * Constant.RAD_TO_ARCSEC);

 		double pos0[] = new double[] {loc0.getLongitude() - this.axis1Pos, loc0.getLatitude() - this.axis2Pos};
 		double pos1[] = new double[] {loc1.getLongitude() - this.axis1Pos, loc1.getLatitude() - this.axis2Pos};
 		if (pos0[0] > Math.PI) pos0[0] -= Constant.TWO_PI;
 		if (pos1[0] > Math.PI) pos1[0] -= Constant.TWO_PI;

 		double cosDec = Math.cos(axis2Pos);
 		pos0[0] *= cosDec;
 		pos1[0] *= cosDec;

		double matrixPos0[] = getPositionFromOffset(pos0[0], pos0[1]);
		double matrixPos1[] = getPositionFromOffset(pos1[0], pos1[1]);

		int stripn[] = new int[] {(int) (matrixPos0[0] + 0.5), (int) (matrixPos0[1] + 0.5),
				(int) (matrixPos1[0] + 0.5), (int) (matrixPos1[1] + 0.5)};

		int np = (int) (4.0 * FastMath.hypot(stripn[2]-stripn[0], stripn[3]-stripn[1]));
		double stepX = (double) (stripn[2] - stripn[0]) / (double) (np-1);
		double stepY = (double) (stripn[3] - stripn[1]) / (double) (np-1);

		float cube[][][] = getCubeData();
		int n = cube.length;
		double dataX[] = new double[n];
		double dataY[] = new double[n];
		double dx = (getvf() - getv0()) / (n - 1.0);
		int px = -1, py = -1;
		ArrayList<double[]> v = new ArrayList<double[]>();
		ChartSeriesElement.setShapeSize(2);
		double offr0 = -1, offrf = -1, posr0;
		posr0 = cube[0].length/2.0 - 0.5;
		boolean useOffsetR = true;
		boolean error = false;
		double scale = Math.abs(this.getx0() - this.getxf()) * Constant.RAD_TO_ARCSEC / this.axis1Dim;
		if (interpolate) {
			double dataY2[][] = new double[np][n];
			for (int i=0; i<n; i++) {
				dataX[i] = getv0() + dx * (double) i;
				ImageSplineTransform t = new ImageSplineTransform(DataSet.toDoubleArray(cube[i]));
				for (int p=0; p<np; p++)
				{
					double xp = stripn[0] + 0.5 + stepX * p;
					double yp = stripn[1] + 0.5 + stepY * p;
					offrf = FastMath.hypot(xp-posr0, yp-posr0);
					if (usePhysicalOffsets) offrf *= scale;
					if (offr0 == -1) offr0 = offrf;
					dataY2[p][i] = 0.0;
					if (yp < cube[0].length && xp < cube[0][0].length) {
						dataY2[p][i] = t.interpolate(yp, xp);
					} else {
						error = true;
					}
					if (error) break;
				}
				if (error) break;
			}
			for (int p=0; p<np; p++)
			{
				v.add(dataY2[p]);
			}
		} else {
			for (int p=0; p<np; p++)
			{
				double xp = stripn[0] + 0.5 + stepX * p;
				double yp = stripn[1] + 0.5 + stepY * p;
				if ((int) xp != px || (int) yp != py) {
					px = (int) xp;
					py = (int) yp;
					offrf = FastMath.hypot(px-posr0, py-posr0);
					if (usePhysicalOffsets) offrf *= scale;
					if (offr0 == -1) offr0 = offrf;
					for (int i=0; i<n; i++) {
						dataX[i] = getv0() + dx * (double) i;
						dataY[i] = 0.0;
						if (py < cube[0].length && px < cube[0][0].length) {
							dataY[i] = cube[i][py][px];
						} else {
							error = true;
						}
					}
					if (error) break;
					v.add(dataY);
				}
			}
		}

		if (error) throw new JPARSECException("The strip has initial and ending points outside range!");

		if (v.size() > 0) {
			CreateChart charts[] = new CreateChart[v.size()];
			double data[][] = new double[(v.get(0)).length][charts.length];
			for (int i=0; i<charts.length; i++)
			{
				double[] c = v.get(i);
				for (int k=0; k<c.length; k++) {
					data[k][i] = c[k];
				}
			}
    		float x0 = getv0();
    		float xf = getvf();
    		float y0 = 1f;
    		float yf = v.size();
    		if (useOffsetR) {
	    		y0 = (float) -offr0;
	    		yf = (float) offrf;
	    		if (stepY < 0.0) {
		    		y0 = (float) offr0;
		    		yf = (float) -offrf;
	    		}
    		}
    		if (offsetsRespectCenter) {
    			y0 = (float) (FastMath.sign(y0) * dist);
    			yf = (float) (FastMath.sign(yf) * dist);
    		}
			GridChartElement chart = new GridChartElement("",
					"Velocity (km s^{-1})", "Strip position offset", "",
					GridChartElement.COLOR_MODEL.BLUE_TO_RED,
					new double[] {x0, xf, y0, yf},  data,
					contours, 400);
			chart.levelsOrientation = GridChartElement.WEDGE_ORIENTATION.HORIZONTAL_BOTTOM;
			chart.levelsBorderStyle = GridChartElement.WEDGE_BORDER.NO_BORDER;
			fixOrientation(chart);

			CreateGridChart gridChart = new CreateGridChart(chart);

			return gridChart;
		}

		return null;
 	}

	private void fixOrientation(GridChartElement chart) {
		  chart.invertXaxis = false;
		  chart.invertYaxis = false;
		  if (chart.limits[0] > chart.limits[1]) {
			  double aux = chart.limits[0];
			  chart.limits[0] = chart.limits[1];
			  chart.limits[1] = aux;
			  Double data2[][] = new Double[chart.data.length][chart.data[0].length];
			  for (int i=0; i<chart.data.length; i++)
			  {
				  data2[i] = chart.data[chart.data.length-1-i].clone();
			  }
			  chart.data = data2;
		  }
		  if (chart.limits[2] > chart.limits[3]) {
			  double aux = chart.limits[2];
			  chart.limits[2] = chart.limits[3];
			  chart.limits[3] = aux;
			  Double data2[][] = new Double[chart.data.length][chart.data[0].length];
			  for (int i=0; i<chart.data.length; i++)
			  {
				  for (int j=0; j<chart.data[0].length; j++)
				  {
					  data2[i][j] = chart.data[i][chart.data[0].length - 1 - j];
				  }
			  }
			  chart.data = data2;
		  }
	}
    // Java objects to read the lmv file
    private Convertible convert;
    private RandomAccessFile bis;

    /**
     * Closes the LMV file, only necessary when you read a large number 
     * of LMV cubes from files. Does nothing when the cube don't come 
     * from a file.
     * @throws IOException If the internal instance to the file cannot be closed.
     */
    public void closeFile() throws IOException {
    	if (bis != null) bis.close();
    }
    
    /**
     * Minimum flux.
     */
    public float minimumFlux;
    /**
     * Maximum flux.
     */
    public float maximumFlux;
    /**
     * Minimum and maximum position for axis 1, 2,
     * 3, and 4. Units are pixels on the image, starting
     * from 1 (Gildas or Fortran convention).
     */
    public int minimumAndMaximumFluxPositions[];

    /**
     * The cube of data.
     */
    private float[][][] cube;
    /**
     * Number of axis in current image.
     */
    public int numberOfAxes;
    /**
     * Dimension in axis 1.
     */
    public int axis1Dim;
    /**
     * Dimension in axis 2.
     */
    public int axis2Dim;
    /**
     * Dimension in axis 3.
     */
    public int axis3Dim;
    /**
     * Dimension in axis 4.
     */
    public int axis4Dim;
    /**
     * Conversion formula. It is formed by 4 sets of 3 values containing
     * respectively a reference pixel, value, and increment for axis 1, 2,
     * 3, and 4. Note that these values follows GILDAS conventions, where
     * y axis goes up following declination, so reference pixel and increment
     * is inverted in y axis respect to common Java conventions where the
     * pixel (0, 0) is the top-left corner of the image. In addition, first
     * pixel in the cube data is pixel reference (1, 1, 1).
     */
    public double conversionFormula[];
    /**
     * Blanking value in map units.
     */
    public float blanking;
    /**
     * Tolerance of blanking value.
     */
    public float blankingTolerance;
    /**
     * Map flux units
     */
    public String fluxUnit;
    /**
     * Label for axis 1, typicaly RA.
     */
    public String axis1Label;
    /**
     * Label for axis 2, typicaly DEC.
     */
    public String axis2Label;
    /**
     * Label for axis 3, typicaly VELOCITY.
     */
    public String axis3Label;
    /**
     * Label for axis 4, typicaly an empty string.
     */
    public String axis4Label;
    /**
     * Coordinate system. See {@linkplain #getCoordinatesType()}.
     */
    public String coordinateSystem;
    /**
     * Source name.
     */
    public String sourceName;
    /**
     * Source right ascension in radians.
     */
    public double sourceRA;
    /**
     * Source declination in radians.
     */
    public double sourceDEC;
    /**
     * Source galactic longitude in radians.
     */
    public double sourceGLon;
    /**
     * Source galatic latitude in radians.
     */
    public double sourceGLat;
    /**
     * Epoch of coordinates as a year.
     */
    public float epoch;
    /**
     * Projection type.
     */
    public PROJECTION projectionType;
    /**
     * Position in axis 1, typicaly longitude or RA.
     */
    public double axis1Pos;
    /**
     * Position in axis 2, typicaly latitude or DEC.
     */
    public double axis2Pos;
    /**
     * Position angle of the map in radians.
     */
    public double axis12PA;
    /**
     * ID of x (longitude, RA) axis, typically 1.
     */
    public int xAxisID;
    /**
     * ID of y (latitude, DEC) axis, typically 2.
     */
    public int yAxisID;
    /**
     * ID of z (velocity) axis, typically 3.
     */
    public int zAxisID;
    /**
     * Line name.
     */
    public String line;
    /**
     * Frequency resolution in MHz.
     */
    public double freqResolution;
    /**
     * Image frequency in MHz.
     */
    public double imageFrequency;
    /**
     * Rest frequency in MHz.
     */
    public double restFreq;
    /**
     * Velocity resolution in km/s.
     */
    public float velResolution;
    /**
     * Velocity LSR in km/s.
     */
    public float velOffset;
    /**
     * Beam major axis in radians.
     */
    public float beamMajor;
    /**
     * Beam minor axis in radians.
     */
    public float beamMinor;
    /**
     * Beam position angle in radians.
     */
    public float beamPA;
    /**
     * Theoretical noise.
     */
    public float noise;
    /**
     * Actual noise.
     */
    public float rms;
    /**
     * Proper motion along RA, in mas/yr.
     */
    public float sourceProperMotionRA;
    /**
     * Proper motion along DEC, in mas/yr.
     */
    public float sourceProperMotionDEC;
    /**
     * Parallax in mas.
     */
    public float sourceParallax;
    /**
     * The WCS.
     */
    public WCS wcs;

	private void writeObject(ObjectOutputStream out)
	throws IOException {
		out.writeObject(this.path);
		out.writeBoolean(this.readingFile);
		out.writeObject(this.cube);
		if (cube != null || !readingFile) {
			out.writeFloat(this.beamMajor);
			out.writeFloat(this.beamMinor);
			out.writeFloat(this.beamPA);
			out.writeFloat(this.blanking);
			out.writeFloat(this.blankingTolerance);
			out.writeFloat(this.epoch);
			out.writeFloat(this.maximumFlux);
			out.writeFloat(this.minimumFlux);
			out.writeFloat(this.noise);
			out.writeFloat(this.rms);
			out.writeFloat(this.sourceParallax);
			out.writeFloat(this.sourceProperMotionDEC);
			out.writeFloat(this.sourceProperMotionRA);
			out.writeFloat(this.velOffset);
			out.writeFloat(this.velResolution);
			out.writeInt(this.axis1Dim);
			out.writeInt(this.axis2Dim);
			out.writeInt(this.axis3Dim);
			out.writeInt(this.axis4Dim);
			out.writeInt(this.numberOfAxes);
			out.writeInt(this.projectionType.ordinal());
			out.writeInt(this.xAxisID);
			out.writeInt(this.yAxisID);
			out.writeInt(this.zAxisID);
			out.writeDouble(this.axis12PA);
			out.writeDouble(this.axis1Pos);
			out.writeDouble(this.axis2Pos);
			out.writeDouble(this.freqResolution);
			out.writeDouble(this.imageFrequency);
			out.writeDouble(this.restFreq);
			out.writeDouble(this.sourceDEC);
			out.writeDouble(this.sourceGLat);
			out.writeDouble(this.sourceGLon);
			out.writeDouble(this.sourceRA);
			out.writeObject(this.conversionFormula);
			out.writeObject(this.coordinateSystem);
			out.writeObject(this.fluxUnit);
			out.writeObject(this.line);
			out.writeObject(this.minimumAndMaximumFluxPositions);
			out.writeObject(this.sourceName);
			out.writeObject(this.axis1Label);
			out.writeObject(this.axis2Label);
			out.writeObject(this.axis3Label);
			out.writeObject(this.axis4Label);
			out.writeObject(this.wcs);
		}
	}
	private void readObject(ObjectInputStream in)
	throws IOException, ClassNotFoundException {
		path = (String) in.readObject();
		readingFile = in.readBoolean();
		cube = (float[][][]) in.readObject();
		if (cube != null || !readingFile) {
			this.beamMajor = in.readFloat();
			this.beamMinor = in.readFloat();
			this.beamPA = in.readFloat();
			this.blanking = in.readFloat();
			this.blankingTolerance = in.readFloat();
			this.epoch = in.readFloat();
			this.maximumFlux = in.readFloat();
			this.minimumFlux = in.readFloat();
			this.noise = in.readFloat();
			this.rms = in.readFloat();
			this.sourceParallax = in.readFloat();
			this.sourceProperMotionDEC = in.readFloat();
			this.sourceProperMotionRA = in.readFloat();
			this.velOffset = in.readFloat();
			this.velResolution = in.readFloat();
			this.axis1Dim = in.readInt();
			this.axis2Dim = in.readInt();
			this.axis3Dim = in.readInt();
			this.axis4Dim = in.readInt();
			this.numberOfAxes = in.readInt();
			this.projectionType = PROJECTION.values()[in.readInt()];
			this.xAxisID = in.readInt();
			this.yAxisID = in.readInt();
			this.zAxisID = in.readInt();
			this.axis12PA = in.readDouble();
			this.axis1Pos = in.readDouble();
			this.axis2Pos = in.readDouble();
			this.freqResolution = in.readDouble();
			this.imageFrequency = in.readDouble();
			this.restFreq = in.readDouble();
			this.sourceDEC = in.readDouble();
			this.sourceGLat = in.readDouble();
			this.sourceGLon = in.readDouble();
			this.sourceRA = in.readDouble();
			this.conversionFormula = (double[]) in.readObject();
			this.coordinateSystem = (String) in.readObject();
			this.fluxUnit = (String) in.readObject();
			this.line = (String) in.readObject();
			this.minimumAndMaximumFluxPositions = (int[]) in.readObject();
			this.sourceName = (String) in.readObject();
			this.axis1Label = (String) in.readObject();
			this.axis2Label = (String) in.readObject();
			this.axis3Label = (String) in.readObject();
			this.axis4Label = (String) in.readObject();
			try {
				this.wcs = (WCS) in.readObject();
			} catch (Exception exc) {
				try {
					setWCS();
				} catch (JPARSECException e) {
					e.printStackTrace();
				}
			}
		} else {
			if (path != null) {
		        bis = new RandomAccessFile(path, "r");
		        //bis.mark(0x7fffffff);
				if (cube == null && readingFile) {
					try {
						this.readFile(path);
					} catch (Exception exc) {
						throw new IOException("file "+path+" cannot be read");
					}
				}
			}
		}
 	}
}
