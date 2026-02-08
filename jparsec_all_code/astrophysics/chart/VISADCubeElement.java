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
package jparsec.astrophysics.chart;

import visad.*;
import java.rmi.RemoteException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import jparsec.astrophysics.gildas.LMVCube;
import jparsec.math.Constant;
import jparsec.util.*;

/**
 * A class to create a cube of data to be shown with VISAD.<P>
 * A cube contains four dimensions: the x axis, the y axis, the z
 * axis, and the t axis. The x and y axes typically define a surface
 * to be shown as coordinates. The z axis is the z coordinate, for
 * example the time if the x, y plane changes with it. The t axis
 * is the value of a physical measure (flux for instance) as function
 * of the other three.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class VISADCubeElement implements Serializable{
	private static final long serialVersionUID = 1L;

 /**
  * Holds the fields to be represented in the cube.
  */
  public RealType rightAscension, declination, velocity, flux;

  // Tuple to pack easting and northing together
  private RealTupleType domain_tuple;

  // The function (domain_tuple -> temperature )
  private FunctionType func_domain_temp;

   // Our Data values for the domain are represented by the Set
  private Set domain_set;

  // The Data class FlatField
  private FlatField vals_ff;

  private int ncolumns = 32;
  private int nrows = 32;
  private int nlevels = 16;

  /**
   * Holds the limits of the physical units in each axis.
   */
  public float initX, finalX, initY, finalY, initZ, finalZ, initT, finalT;
  /**
   * Holds overscan factor for the plane that moves along the z
   * axis of the cube, by default 1.1.
   */
  public float overScanZ = 1.1f;

  /**
   * Constructor for a default cube with right ascension, declination,
   * and velocity as axis.
   * @param cube The cube of data as an array in 3d, ordered by
   * number of levels, number of columns (DEC), and number of rows (RA).
   * @param limits Array with physical values of the Initial and ending points in
   * X, Y, and Z axes.
   * @throws JPARSECException If an exception occurs.
   */
  public VISADCubeElement(float[][][] cube, float[] limits)
    throws JPARSECException{

	  this.init(cube, limits, Translate.translate(912), UNIT.RADIAN, Translate.translate(913), UNIT.RADIAN, Translate.translate(292), UNIT.METER, Translate.translate(414), UNIT.KELVIN);
  }

  /**
   * The set of units for the axis of the cube.
   */
  public enum UNIT {
	  /** ID constant for Kelvin unit. */
	  KELVIN,
	  /** ID constant for meter unit. */
	  METER,
	  /** ID constant for radian unit. */
	  RADIAN,
	  /** ID constant for second unit. */
	  SECOND,
	  /** ID constant for steradian unit. */
	  STERADIAN,
	  /** ID constant for Ampere unit. */
	  AMPERE,
	  /** ID constant for candela unit. */
	  CANDELA,
	  /** ID constant for kilogram unit. */
	  KILOGRAM,
	  /** ID constant for mole unit. */
	  MOLE,
	  /** ID constant for meter per second unit. */
	  KILOMETER_PER_SECOND,
	  /** ID constant for arcseconds unit. */
	  ARCSEC
  };

  private static final Unit[] UNITS = new Unit[] {SI.kelvin, SI.meter, SI.radian, SI.second,
	  SI.steradian, SI.ampere, SI.candela, SI.kilogram, SI.mole, null, null};

  /**
   * Constructor for a cube with custom elements in the axes.
   * @param cube The cube of data as an array in 3d, ordered by
   * number of levels, number of columns (DEC), and number of rows (RA).
   * @param limits Array with physical values of the Initial and ending points in
   * X, Y, and Z axes.
   * @param xLabel Physical meaning of x axis. Blank spaces and other symbols not allowed.
   * @param xUnit ID constant of the unit for x axis. Constants defined in this class.
   * @param yLabel Physical meaning of y axis. Blank spaces and other symbols not allowed.
   * @param yUnit ID constant of the unit for y axis. Constants defined in this class.
   * @param zLabel Physical meaning of z axis. Blank spaces and other symbols not allowed.
   * @param zUnit ID constant of the unit for z axis. Constants defined in this class.
   * @param tLabel Physical meaning of t axis. Blank spaces and other symbols not allowed.
   * @param tUnit ID constant of the unit for t axis. Constants defined in this class.
   * @throws JPARSECException If an exception occurs.
   */
  public VISADCubeElement(float[][][] cube, float[] limits, String xLabel, UNIT xUnit,
		  String yLabel, UNIT yUnit, String zLabel, UNIT zUnit, String tLabel, UNIT tUnit)
    throws JPARSECException{
	  this.init(cube, limits, xLabel, xUnit, yLabel, yUnit, zLabel, zUnit, tLabel, tUnit);
  }

  private float cube[][][], limits[];
  private String xl, yl, zl, tl, xu, yu, zu, tu;
  private void init(float[][][] cube, float[] limits, String xLabel, UNIT xUnit,
		  String yLabel, UNIT yUnit, String zLabel, UNIT zUnit, String tLabel, UNIT tUnit) throws JPARSECException {
	  try {
		  this.cube = cube;
		  this.limits = limits;
		  xl = xLabel;
		  xu = xUnit.name();
		  yl = yLabel;
		  yu = yUnit.name();
		  zl = zLabel;
		  zu = zUnit.name();
		  tl = tLabel;
		  tu = tUnit.name();

		  this.nlevels = cube.length;
		  this.ncolumns = cube[0].length;
		  this.nrows = cube[0][0].length;
		  this.initX = limits[0];
		  this.finalX = limits[1];
		  this.initY = limits[2];
		  this.finalY = limits[3];
		  this.initZ = limits[4];
		  this.finalZ = limits[5];

		  float initT =cube[0][0][0], endT = cube[0][0][0];
		    for(int l = 0; l < nlevels; l++)
		        for(int c = 0; c < ncolumns; c++)
		          for(int r = 0; r < nrows; r++){
		        	  if (cube[l][c][r] < initT) initT = cube[l][c][r];
		        	  if (cube[l][c][r] > endT) endT = cube[l][c][r];
		        }
  		  this.initT = initT;
		  this.finalT = endT;

	    // Create the quantities
	    // Use RealType(String name, Unit unit, Set set);
		  try {
			  Unit as = visad.data.units.Parser.parse("arcsec");
			  Unit vu = visad.data.units.Parser.parse("km/s");
			  UNITS[VISADCubeElement.UNIT.ARCSEC.ordinal()] = as;
			  UNITS[VISADCubeElement.UNIT.KILOMETER_PER_SECOND.ordinal()] = vu;
		  } catch (Exception exc) { throw new JPARSECException(exc); }
		  rightAscension = RealType.getRealType(xLabel, UNITS[xUnit.ordinal()], null);
		  declination = RealType.getRealType(yLabel, UNITS[yUnit.ordinal()], null);
		  velocity = RealType.getRealType(zLabel, UNITS[zUnit.ordinal()], null);
		  flux = RealType.getRealType(tLabel, UNITS[tUnit.ordinal()], null);
		  domain_tuple = new RealTupleType(rightAscension, declination, velocity);

	    // Create a FunctionType (domain_tuple -> range_tuple )
	    // Use FunctionType(MathType domain, MathType range)
	    func_domain_temp = new FunctionType( domain_tuple, flux);

	    // Create the domain Set
	    // Integer3DSet(MathType type, int lengthX, int lengthY, int lengthZ)
	    domain_set = new Linear3DSet(domain_tuple,
	    		limits[0], limits[1], nrows,
	    		limits[2], limits[3], ncolumns,
	    		limits[4], limits[5], nlevels );

	    // Fill our 'flat' array with the temperature values
	    // by looping over NCOLS and NROWS
	    // but first get the samples to help with the calculations
	    // Create a FlatField
	    vals_ff = new FlatField( func_domain_temp, domain_set);

	    // ...and put the temperature values above into it

	    // Note the argument false, meaning that the array won't be copied
	    float[][] samples = getSamples(cube);

	    vals_ff.setSamples( samples , false );
	  } catch (RemoteException exc)
	  {
		  throw new JPARSECException("remote exception.",exc);
	  }
	  catch (VisADException ex)
	  {
		  throw new JPARSECException("VisAD exception.",ex);
	  }
  }

  /**
   * Returns the 'samples' object to be used as a FlatField in the
   * VISAD library.
   * @param cube The 3d cube.
   * @return The 2d samples.
   */
  public float[][] getSamples(float cube[][][]){
    float[][] flat_samples = new float[1][ncolumns * nrows * nlevels];

    // Note the use of an index variable, indicating the order of the samples
    int index = 0;
    for(int l = 0; l < nlevels; l++)
      for(int c = 0; c < ncolumns; c++)
        for(int r = 0; r < nrows; r++){

	      // set value for RealType temperature
	      flat_samples[0][ index ] =   cube[l][c][r];

	      // increment index
	      index++;
      }
    return flat_samples;
  }

  /**
   * Returns the FlatField representing the temperature data in the cube
   * @return The flat field object.
   */
    public FlatField getData(){
    	return vals_ff;
    }

    /**
     * Returns the input cube.
     * @return The input cube.
     * @throws Exception If an error occurs.
     */
    public float[][][] getCube()
    throws Exception {
    	float[][] samples = vals_ff.getFloats();
        float[][][] cube = new float[nlevels][ncolumns][nrows];
        int i = 0;
        for(int l = 0; l < nlevels; l++) {
            for(int c = 0; c < ncolumns; c++) {
              for(int r = 0; r < nrows; r++){
            	  cube[l][c][r] = samples[0][i];
            	  i++;
              }
            }
        }
        return cube;
    }

    /**
     * Return number of columns, x axis.
     * @return Number of columns.
     */
    public int getNColumns()
    {
    	return this.ncolumns;
    }
    /**
     * Return number of rows, y axis.
     * @return Number of rows.
     */
    public int getNRows()
    {
    	return this.nrows;
    }
    /**
     * Return number of levels, z axis.
     * @return Number of levels.
     */
    public int getNLevels()
    {
    	return this.nlevels;
    }

    /**
     * Constructor for a given Gildas lmv file.
     * @param lmvFile The path to the file.
     * @throws JPARSECException If an error occurs.
     */
    public VISADCubeElement(String lmvFile) throws JPARSECException {
    	LMVCube lmv = new LMVCube(lmvFile);

  		float v0 = lmv.getv0();
		float vf = lmv.getvf();
		float x0 = (float) (lmv.getx0() * Constant.RAD_TO_ARCSEC);
		float xf = (float) (lmv.getxf() * Constant.RAD_TO_ARCSEC);
		float y0 = (float) (lmv.gety0() * Constant.RAD_TO_ARCSEC);
		float yf = (float) (lmv.getyf() * Constant.RAD_TO_ARCSEC);

		this.init(lmv.getCubeData(),
				  new float[] {x0, xf, y0, yf, v0, vf},
				  "OFFSET_RA", VISADCubeElement.UNIT.ARCSEC,
				  "OFFSET_DEC", VISADCubeElement.UNIT.ARCSEC,
				  "Velocity", VISADCubeElement.UNIT.KILOMETER_PER_SECOND,
				  "FLUX", VISADCubeElement.UNIT.KELVIN);
    }

	private void writeObject(ObjectOutputStream out)
	throws IOException {
		out.writeObject(this.cube);
		out.writeObject(this.limits);
		out.writeObject(this.xl);
		out.writeObject(this.yl);
		out.writeObject(this.zl);
		out.writeObject(this.tl);
		out.writeObject(this.xu);
		out.writeObject(this.yu);
		out.writeObject(this.zu);
		out.writeObject(this.tu);
	}

	/**
	 * Reads the object.
	 * @param in Input stream.
	 * @throws IOException If an error occurs.
	 * @throws ClassNotFoundException If an error occurs.
	 */
	private void readObject(ObjectInputStream in)
	throws IOException, ClassNotFoundException {
		cube = (float[][][]) in.readObject();
		limits = (float[]) in.readObject();
		xl = (String) in.readObject();
		yl = (String) in.readObject();
		zl = (String) in.readObject();
		tl = (String) in.readObject();
		xu = (String) in.readObject();
		yu = (String) in.readObject();
		zu = (String) in.readObject();
		tu = (String) in.readObject();

		try {
			this.init(cube,
					  limits,
					  xl, UNIT.valueOf(xu),
					  yl, UNIT.valueOf(yu),
					  zl, UNIT.valueOf(zu),
					  tl, UNIT.valueOf(tu));
		} catch (JPARSECException e) {	}
 	}

}
