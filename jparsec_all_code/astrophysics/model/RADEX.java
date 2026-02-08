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
package jparsec.astrophysics.model;

import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.math.CGSConstant;
import jparsec.math.matrix.Matrix;
import jparsec.util.JPARSECException;
import jparsec.util.Logger;
import jparsec.util.Logger.LEVEL;

/**
 * RADEX program, adequate to calculate the strengths of atomic and
 * molecular lines from interstellar clouds, which are assumed to be
 * homogeneous. The use of this class is subject to reference to the
 * original paper: Van der Tak, F.F.S., Black, J.H., Sch&ouml;ier, F.L.,
 * Jansen, D.J., van Dishoeck, E.F., 2007, A&amp;A 468, 627-635.<BR>
 *
 * This Java version corresponds to the translation of the original
 * Fortran program version from August 29, 2007, updated to 30nov2011
 * version on April 4, 2012.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class RADEX {

	/**
	 * Column density of molecule (cm^-2).
	 */
	public double cdmol = 1.0E13;
	/**
	 * Line width (cm/s).
	 */
	public double deltav = 1.0;
	/**
	 * Background temperature (K).
	 */
	public double tbg = 2.73;
	/**
	 * Number of collision partners.
	 */
	public int npart;
	/**
	 * Type of collision partner.
	 */
	public PARTNER[] id;
	/**
	 * Density of collision partner (cm^-3).
	 */
	public double[] density;
	/**
	 * Kinetic temperature (K).
	 */
	public double tkin = 30.0;
	/**
	 * ID for the molecule.
	 */
	public int molfile;
	/**
	 * Minimum frequency (GHz).
	 */
	public double fmin;
	/**
	 * Maximum frequency (GHz).
	 */
	public double fmax;
	/**
	 * ID constant for the method (geometry) to apply.
	 */
	public METHOD method;
	/**
	 * True to use JPL catalog to solve transitions, false for COLOGNE. JPL
	 * usually includes the hyperfine transitions (OH, HCL, N2H+, and HCN), and COLOGNE
	 * contains them at once (added). OH and HCL are not supported even in this way in COLOGNE.
	 */
	public boolean jpl;

	/**
	 * Full constructor. This constructor also checks the input data, and, if everything is OK,
	 * the calculation of line intensities are performed. Minimum and maximum frequencies can define
	 * a range to return the intensities for a given set of transitions, or can be the same value
	 * to specify exactly one and only that transition.
	 * @param molID Molecule ID.
	 * @param columnDensity Column density.
	 * @param lineWidth Line width in km/s.
	 * @param Tkin Kinetic temperature.
	 * @param Tbg Background temperature.
	 * @param minFreq Minimum frequency (GHz).
	 * @param maxFreq Maximum frequency (GHz).
	 * @param partnerID IDs for the partners.
	 * @param partnerDensity Densities for the partners.
	 * @param method Geometry to apply.
	 * @param jpl True to use JPL catalog to solve transitions, false for COLOGNE. JPL
	 * usually includes the hyperfine transitions (OH, HCL, N2H+, and HCN), and COLOGNE
	 * contains them at once (added). OH and HCL are not supported even in this way in COLOGNE.
	 * @throws JPARSECException If an error occurs.
	 */
	public RADEX(int molID, double columnDensity, double lineWidth, double Tkin, double Tbg,
			double minFreq, double maxFreq, PARTNER partnerID[], double partnerDensity[],
			METHOD method, boolean jpl)
	throws JPARSECException {
		int nPartner = partnerID.length;
		this.cdmol = columnDensity;
		this.deltav = lineWidth * 1.0E5;
		this.density = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		this.id = new PARTNER[nPartner];
		for (int i=0; i<nPartner;i++)
		{
			int j = partnerID[i].ordinal();
			this.density[j] = partnerDensity[i];
			this.id[i] = partnerID[i];
		}
		this.fmax = maxFreq;
		this.fmin = minFreq;
		this.molfile = molID;
		this.npart = nPartner;
		this.tbg = Tbg;
		this.tkin = Tkin;
		this.method = method;
		this.jpl = jpl;

		this.check();
		this.execute(true);
	}

	/**
	 * Constructor for one collisional partner. This constructor also checks the
	 * input data, and, if everything is OK, the calculation of line intensities
	 * are performed. Minimum and maximum frequencies can define
	 * a range to return the intensities for a given set of transitions, or can be the same value
	 * to specify exactly one and only that transition.
	 * @param molID Molecule ID.
	 * @param columnDensity Column density.
	 * @param lineWidth Line width in km/s.
	 * @param Tkin Kinetic temperature.
	 * @param Tbg Background temperature.
	 * @param minFreq Minimum frequency (GHz).
	 * @param maxFreq Maximum frequency (GHz).
	 * @param partnerID ID for the partner.
	 * @param partnerDensity Densities for the partners.
	 * @param method Geometry to apply.
	 * @param jpl True to use JPL catalog to solve transitions, false for COLOGNE. JPL
	 * usually includes the hyperfine transitions (OH, HCL, N2H+, and HCN), and COLOGNE
	 * contains them at once (added). OH and HCL are not supported even in this way in COLOGNE.
	 * @throws JPARSECException If an error occurs.
	 */
	public RADEX(int molID, double columnDensity, double lineWidth, double Tkin, double Tbg,
			double minFreq, double maxFreq, PARTNER partnerID, double partnerDensity,
			METHOD method, boolean jpl)
	throws JPARSECException {
		this(molID, columnDensity, lineWidth, Tkin, Tbg, minFreq, maxFreq, new PARTNER[] {partnerID},
				new double[] {partnerDensity}, method, jpl);
	}

	/**
	 * The set of possible collisional partners.
	 */
	public enum PARTNER {
		/** ID constant for H2 collision partner. */
		H2,
		/** ID constant for p-H2 collision partner. */
		pH2,
		/** ID constant for o-H2 collision partner. */
		oH2,
		/** ID constant for e collision partner. */
		e,
		/** ID constant for H collision partner. */
		H,
		/** ID constant for He collision partner. */
		He,
		/** ID constant for H+ collision partner. */
		Hplus
	};

	/**
	 * The set of calculation methods.
	 */
	public enum METHOD {
		/** ID constant for an uniform sphere method. */
		UNIFORM_SPHERE,
		/** ID constant for an expanding sphere method (LVG). */
		EXPANDING_SPHERE,
		/** ID constant for a plane parallel slab (shock). */
		PLANE_PARALLEL_SLAB
	};

	/**
	 * Holds the names of the available molecules/atoms.
	 */
	public static final String[] MOLECULE_ATOM_NAMES = new String[] {"13CO", "13CS", "29SiO", "C", "C+", "C17O",
		"C18O", "C34S", "CO", "CS", "DCO+", "e-CH3OH", "H13CN", "H13CO+", "HC15N", "HC17O+", "HC18O+", "HC3N",
		"HCl_hfs", "HCl", "HCN_hfs", "HCN", "HCO+", "HCS+", "HDO", "HNC", "HNCO", "N2H+_hfs", "N2H+", "o-C3H2", "o-H2CO",
		"o-H2O_lowT", "o-H2O", "o-H3O+", "o-NH3", "o-SiC2", "O", "OCS", "OH_hfs", "OH", "p-C3H2", "p-H2CO",
		"p-H2O_lowT", "p-H20", "p-H3O+", "p-NH3", "SiO", "SiS", "SO", "SO2", "N2D+", "NO", "CN", "CH3CN", "HF", "HOC+"};

	/**
	 * Holds the JPL catalog names of the available molecules/atoms.
	 */
	public static final String[] MOLECULE_ATOM_NAMES_CORRESPONDENCE_JPL_CATALOG = new String[] {
		"29001 C-13-O", "45001 C-13-S", "45002 Si-29-O", "12001 C-atom", "NULL", "29006 CO-17",
		"30001 CO-18", "46001 CS-34", "28001 CO", "44001 CS", "30003 DCO+", "32003 CH3OH",
		"28002 HC-13-N", "30002 HC-13-O+", "28003 HCN-15", "NULL", "31001 HCO-18+", "51001 HCCCN",
		"36001 HCl", "NULL", "NULL", "27001 HCN", "29002 HCO+", "45005 HCS+", // HCl is always hfs
		"19002 HDO", "27002 HNC", "43002 HNCO", "NULL", "29005 NNH+", "38002 c-C3H2", "30004 H2CO",
		"NULL", "18003 H2O", "19004 H3O+", "17002 NH3", "52007 SiCC", "16001 O-atom", "60001 OCS",
		"17001 OH", "NULL", "38002 c-C3H2", "30004 H2CO", "NULL", "18003 H2O", "19004 H3O+", // OH is always hfs
		"17002 NH3", "44002 SiO", "60002 SiS", "48001 SO", "64002 SO2", "30009 NND+",
		"30008 NO", "26001 CN", "41001 CH3CN", "20002 HF", "29007 HOC+"};

	/**
	 * Holds the COLOGNE catalog names of the available molecules/atoms.
	 */
	public static final String[] MOLECULE_ATOM_NAMES_CORRESPONDENCE_COLOGNE_CATALOG = new String[] {
		"29501 C-13-O", "45501 C-13-S", "45504 Si-29-O", "12501 C-atom", "NULL", "29503 CO-17",
		"30502 CO-18", "46501 CS-34", "28503 CO", "44501 CS", "30510 DCO+", "32504 *CH3OH",  // 44501 would be hfs ...
		"28501 HC-13-N", "30504 HC-13-O+", "28506 HCN-15", "NULL", "31506 HCO-18+", "51501 HC3N",
		"NULL", "NULL", "NULL", "27501 HCN", "29507 HCO+", "45506 HCS+",
		"NULL", "27502 HNC", "NULL", "NULL", "29506 N2H+", "38501 l-C3H2", "30501 H2CO",
		"NULL", "NULL", "NULL", "NULL", "52527 SiC2", "NULL", "60503 OCS",
		"NULL", "NULL", "38501 l-C3H2", "30501 H2CO", "NULL", "NULL", "NULL", // 17501 OH+, but not OH
		"NULL", "44505 SiO", "60506 SiS", "48501 SO", "64502 SO2", "30509 N2D+",
		"NULL", "26504 CN", "41505 CH3CN", "NULL", "29504 HOC+"};

	/**
	 * Maximum number of collisional partners.
	 */
	public static final int MAX_PART = 7;
	/**
	 * Maximum number of collisional temperatures.
	 */
	public static final int MAX_TEMP = 19;
	/**
	 * Maximum number of energy levels.
	 */
	public static final int MAX_LEV = 199;
	/**
	 * Maximum number of radiative transitions.
	 */
	public static final int MAX_LINE = 1999;
	/**
	 * Maximum number of collisional transitions.
	 */
	public static final int MAX_COLL = 19999;

	/**
	 * Minimum number of iterations.
	 */
	public static final int MIN_ITER = 10;
	/**
	 * Maximum number of iterations.
	 */
	public static final int MAX_ITER = 9999;
	/**
	 * Relative tolerance on solution.
	 */
	public static final double CCRIT = 1.0E-6;
	/**
	 * Round-off error.
	 */
	public static final double EPS = 1.0E-30;
	/**
	 * Minimum level population.
	 */
	public static final double MIN_POP = 1.0E-20;


	/**
	 * Checks the instance for consistency, in the same way as it is
	 * done in the original RADEX program.
	 * @throws JPARSECException If any input data is not consistent.
	 */
	private void check()
	throws JPARSECException {
		if (fmin > fmax) throw new JPARSECException("invalid frequencies "+fmin+" -> "+fmax+".");
		if (fmin < 0.0 || fmax > 3.0E7) throw new JPARSECException("invalid frequencies "+fmin+" -> "+fmax+".");

		if (tkin < 0.1 || tkin > 1E4) throw new JPARSECException("invalid kinetic temperature "+tkin+".");
		if (npart < 1 || npart > 7) throw new JPARSECException("invalid number of collision partners "+npart+".");

		if (tbg < 0.0 || tbg > 1E4) throw new JPARSECException("invalid background temperature "+tbg+".");

		for (int i=0; i<npart; i++)
		{
			if (density[i] < 1E-3 || density[i] > 1E13)
				throw new JPARSECException("invalid density for partner number "+(i+1)+".");
		}

		int minMol = 0; //(int) DataSet.getMinimumValue(MOLECULES);
		int maxMol = RADEX.MOLECULE_ATOM_NAMES.length-1; //(int) DataSet.getMaximumValue(MOLECULES);
		if (molfile < minMol || molfile > maxMol)
			throw new JPARSECException("invalid molecule "+molfile+".");

		if (cdmol < 9E4 || cdmol > 1E25)
			throw new JPARSECException("invalid column density "+cdmol+" for molecule.");
		if (deltav < 10.0 || deltav > 1.0E8)
			throw new JPARSECException("invalid line width "+(deltav/1.0E5)+" km/s.");

		String mol = MOLECULE_ATOM_NAMES_CORRESPONDENCE_JPL_CATALOG[molfile];
		if (!jpl) mol = MOLECULE_ATOM_NAMES_CORRESPONDENCE_COLOGNE_CATALOG[molfile];
		if (mol.equals("NULL")) throw new JPARSECException("The selected combination of catalog and molecule is incompatible.");

		if (molfile == 27 && jpl && fmax < 100) {
			molfile --;
			Logger.log(LEVEL.WARNING, "Molecule file was modified to N2H+_hfs to account for hyperfine splitting in the 1-0 transition.");
		}
		if (molfile == 20 && jpl && fmax < 180) {
			molfile --;
			Logger.log(LEVEL.WARNING, "Molecule file was modified to HCN_hfs to account for hyperfine splitting in the 1-0 and 2-1 transitions.");
		}
	}

	// Common variables declaration
	private int nlev, nline, ncoll, ntemp;
	private int iupp[];
	private int ilow[];

//	private double amass;
	private double eterm[];
	private double gstat[];
	private double aeinst[];
	private double eup[];
	private double totdens;

//	private String specref;
	private double taul[];
	private double tex[];
	private double backi[];
	private double xnu[];
	private double trj[];
	private double totalb[];
	private double spfreq[];
	private String[] qnum;
	private double fk = CGSConstant.PLANCK_CONSTANT * CGSConstant.SPEED_OF_LIGHT / CGSConstant.BOLTZMANN_CONSTANT;
	private double thc= 2.0 * CGSConstant.PLANCK_CONSTANT * CGSConstant.SPEED_OF_LIGHT;
	private double fgaus = 1.0645 * 8.0 * Math.PI;

	private double crate[][];
    private double colld[][][];
	private double xpop[];
	private double ctot[];

	/**
	 * Reads the molecule data file.
	 */
	private void readdata()
	throws JPARSECException {
		  // Reads molecular data files (2003 format)
	      //int ilev,jlev;   // to loop over energy levels

	//     upper/lower levels of collisional transition
	      int lcu[];
	      int lcl[];
	      double coll[][][];
	      double temp[];  // collision temperatures

//	      String collref; // text about source of collisional data

	//     to interpolate rate coeffs
	      int iup,ilo,nint = 0;
	      double tupp,tlow,fint;

	//     to verify matching of densities and rates
	      //boolean found;

	//     to calculate thermal o/p ratio for H2
	      //double opr;

	//     Executable part begins here.
	      String jarpath = FileIO.DATA_RADEX_DIRECTORY + RADEX.MOLECULE_ATOM_NAMES[this.molfile].toLowerCase() + ".dat";
	      String file[] = DataSet.arrayListToStringArray(ReadFile.readResource(jarpath));

	      // Remove tabs
	      file = DataSet.replaceAll(file, "\t", "  ", true);

//	      specref = file[1];
//	      amass = DataSet.parseDouble(file[3].trim());
	      nlev = Integer.parseInt(file[5].trim());

	  	//     Term energies and statistical weights
	      eterm = new double[nlev];
	      gstat = new double[nlev];
	      qnum = new String[nlev];
	      for (int i=0; i<nlev; i++)
	      {
	    	  eterm[i] = DataSet.parseDouble(FileIO.getField(2, file[7+i], " ", true).trim());
	    	  gstat[i] = DataSet.parseDouble(FileIO.getField(3, file[7+i], " ", true).trim());
	    	  qnum[i] = FileIO.getField(4, file[7+i], " ", true);
	      }

	  	//     Radiative upper & lower levels and Einstein coefficients
	      nline = Integer.parseInt(file[8+nlev].trim());
	      iupp = new int[nline];
	      ilow = new int[nline];
	      aeinst = new double[nline];
	      spfreq = new double[nline];
	      eup = new double[nline];
	      xnu = new double[nline];
	      for (int i=0; i<nline; i++)
	      {
	    	  iupp[i] = Integer.parseInt(FileIO.getField(2, file[10+nlev+i], " ", true)) - 1;
	    	  ilow[i] = Integer.parseInt(FileIO.getField(3, file[10+nlev+i], " ", true)) - 1;
	    	  aeinst[i] = DataSet.parseDouble(FileIO.getField(4, file[10+nlev+i], " ", true));
	    	  spfreq[i] = DataSet.parseDouble(FileIO.getField(5, file[10+nlev+i], " ", true));
	    	  eup[i] = DataSet.parseDouble(FileIO.getField(6, file[10+nlev+i], " ", true));
	    	  xnu[i] = (eterm[iupp[i]] - eterm[ilow[i]]);
	      }

	  	//     Number of collision partners
	      npart = Integer.parseInt(file[11+nlev+nline].trim());
//	      if (npartMax < npart) throw new JPARSECException("the number of collision partners ("+npart+") is greater than the maximum value ("+npartMax+").");
	      int index = 0;
	      colld = new double[npart][MAX_LEV][MAX_LEV];
	      coll = new double[npart][MAX_COLL][MAX_TEMP];
	      id = new PARTNER[npart];
	      for (int i=0; i<npart; i++)
	      {
	    	  index += 2;
	    	  id[i] = PARTNER.values()[Integer.parseInt(FileIO.getField(1, file[11+nlev+nline+index], " ", true)) - 1];
//	    	  collref = FileIO.getField(2, file[13+nlev+nline+i], " ");
	    	  index += 2;
	    	  ncoll = Integer.parseInt(file[11+nlev+nline+index].trim());
	    	  index += 2;
	    	  ntemp = Integer.parseInt(file[11+nlev+nline+index].trim());

	    	  temp = new double[ntemp];
	    	  index += 2;
	    	  for (int j=0; j<ntemp; j++)
	    	  {
	    		  temp[j] = DataSet.parseDouble(FileIO.getField(j+1, file[11+nlev+nline+index], " ", true));
	    	  }

	    	  lcu = new int[ncoll];
	    	  lcl = new int[ncoll];
	    	  if (i == 0) coll = new double[npart][ncoll][ntemp];
	    	  index += 2;
	    	  for (int j = 0; j<ncoll; j++)
	    	  {
	    		  lcu[j] = Integer.parseInt(FileIO.getField(2, file[11+nlev+nline+index+j], " ", true)) - 1;
	    		  lcl[j] = Integer.parseInt(FileIO.getField(3, file[11+nlev+nline+index+j], " ", true)) - 1;
		    	  for (int k = 0; k<ntemp; k++)
		    	  {
		    		  coll[i][j][k] = DataSet.parseDouble(FileIO.getField(4+k, file[11+nlev+nline+index+j], " ", true));
		    	  }
	    	  }
	    	  index = index + ncoll - 1;

	    		//     interpolate array coll(ncol,ntemp) to desired temperature

	    		//     Must do this now because generally, rates with different partners
	    		//     are calculated for a different set of temperatures

	    	  if (ntemp <= 1) {
	    		  for (int j=0; j<ncoll; j++)
	    		  {
		               iup=lcu[j];
		               ilo=lcl[j];
		               colld[i][iup][ilo] = coll[i][j][0];
	    		  }
	    	  } else {
	    		  if (tkin > temp[0]) {
	    			  if (tkin < temp[ntemp-1])
	    			  {
	    		    		//===  interpolation :
	    				  for (int j=0; j<(ntemp-1); j++)
	    				  {
	    					  if (tkin > temp[j] && tkin <= temp[j+1]) nint = j;
	    				  }
		                  tupp = temp[nint+1];
		                  tlow = temp[nint];
		                  fint = (tkin-tlow)/(tupp-tlow);
		                for (int j=0; j<ncoll; j++)
		                {
		                     iup=lcu[j];
		                     ilo=lcl[j];
		                     colld[i][iup][ilo] = coll[i][j][nint]+fint*(coll[i][j][nint+1]-coll[i][j][nint]);
		                     if (colld[i][iup][ilo] < 0.0) colld[i][iup][ilo] = coll[i][j][nint];
		                }
	    			  } else {
	    				  // Tkin too high :
		                  if (tkin != temp[ntemp-1]) JPARSECException.addWarning("kinetic temperature higher than temperatures for which collisional rates are present.");
		                  for (int j =0; j<ncoll; j++)
		                  {
		                     iup=lcu[j];
		                     ilo=lcl[j];
		                     colld[i][iup][ilo] = coll[i][j][ntemp-1];
		                  }
	    			  }
	    		  } else {
	    			  // Tkin too low :
	                  if (tkin != temp[1]) JPARSECException.addWarning("kinetic temperature lower than temperatures for which collisional rates are present.");
	                  for (int j =0; j<ncoll; j++)
	                  {
	                     iup=lcu[j];
	                     ilo=lcl[j];
	                     colld[i][iup][ilo] = coll[i][j][0];
	                  }
	    		  }
	    	  }
	      }
	}

	private void reset() throws JPARSECException {
		//     Combine rate coeffs of several partners, multiplying by partner density.
	      crate = new double[nlev][nlev];
	      for (int iup = 0; iup < nlev; iup ++)
	      {
		      for (int ilo = 0; ilo < nlev; ilo ++)
		      {
		    	  crate[iup][ilo] = 0.0;
		      }
	      }

	      totdens = 0.0;
	      boolean found = false;

	//     Special case (CO, atoms): user gives total H2 but data file has o/p-H2.
	//     Quite a big IF:
	      if (npart > 1 && density[0] > EPS && density[1] < EPS && density[2] < EPS)
	      {
	         double opr = DataSet.getMinimumValue(new double[] {3.0, 9.0*Math.exp(-170.6/tkin)});
	         double d = density[0];
	         density[0] = d;
	         density[1] = density[0]/(opr+1.0);
	         density[2] = density[0]/(1.0+1.0/opr); // Only if npart is 3 (o-H2), does it happen ?
	         JPARSECException.addWarning("co/atoms is a special case: npart modified to 2 (o-H2, p-H2) and thermal o/p ratio for H2 adjusted");
	      }

	      for (int i=0; i<PARTNER.values().length; i++)
	      {
	    	  totdens = totdens + density[i];
		      for (int j=0; j<id.length; j++)
		      {
		    	  if (id[j] == PARTNER.values()[i] && density[i] > 0.0) {
		    		  found = true;
			    	  for (int iup=0; iup<nlev; iup++)
			    	  {
				    	  for (int ilo=0; ilo<nlev; ilo++)
				    	  {
			                     crate[iup][ilo] += density[i] * colld[j][iup][ilo];
				    	  }
			    	  }
		    	  }
		      }
	      }

	      if (!found) throw new JPARSECException("no rates found for any collisional partner.");

	//     Calculate upward rates from detailed balance
	      ctot = new double[nlev];
  	  for (int iup=0; iup<nlev; iup++)
  	  {
	    	  for (int ilo=0; ilo<nlev; ilo++)
	    	  {
	    		  double ediff = eterm[iup]-eterm[ilo];
	    		  if (ediff > 0.0) {
	    		    if ((fk*ediff/tkin) >= 160.0)
	    		    {
	    		      crate[ilo][iup] = 0.0;
	    		    } else {
	    		      crate[ilo][iup] = gstat[iup]/gstat[ilo]*Math.exp(-fk*ediff/tkin)*crate[iup][ilo];
	    		    }
	    		  }
	    	  }
		      ctot[iup] = 0.0;
  	  }

	//     Calculate total collision rates (inverse collisional lifetime)
  	  for (int ilev = 0; ilev < nlev; ilev ++)
  	  {
      	  for (int jlev = 0; jlev < nlev; jlev ++)
      	  {
      		  ctot[ilev] += crate[ilev][jlev];
      	  }
  	  }

	}

	private void execute(boolean readData)
	throws JPARSECException {
		//     Read data file
		if (readData) readdata();
		reset();

		//     Calculate background radiation field
		backrad();

		int niter = 0;
		boolean conv = false;

		//     Set up rate matrix, splitting it in radiative and collisional parts
		//     Invert rate matrix to get `thin' starting condition
	    xpop = new double[nlev];
	    taul = new double[nline];
	    tex = new double[nline];
	    conv = matrix(niter);

		//     Start iterating
		for (niter = 1; niter <= MAX_ITER; niter ++)
		{
		//     Invert rate matrix using escape probability for line photons
		         conv = matrix(niter);
		         if (conv) break;
		}

		if (!conv) throw new JPARSECException("calculation did not converge in "+MAX_ITER+" iterations.");

		//     Write output
        	 output();
	}

	private void output() throws JPARSECException
	{
	      int iline;    // to loop over lines
	      int m,n;      // upper & lower level of the line

	      double xt;        // frequency cubed
	      double hnu;       // photon energy
	      double bnutex;    // line source function
	      double ftau;      // exp(-tau)
	      double toti;      // background intensity
	      double tbl;       // black body temperature
	      double wh;        // Planck correction
	      double tback;     // background temperature
	      double ta;        // line antenna temperature
	      double tr;        // line radiation temperature
	      double beta;      // escape probability
	      double bnu;       // Planck function
	      double kkms;      // line integrated intensity (K km/s)
	      // double wavel;     // line wavelength (micron)

	      //     Begin executable statements
	      int ntran = 0;
	      for (iline=0; iline <nline; iline++)
	      {
	    	  if (isInRange(spfreq[iline]))
	    	  {
	    		  ntran ++;
	    	  }
	      }
	      flux = new double[ntran];
	      tantenna = new double[ntran];
	      transition = new String[ntran];
	      exc = new double[ntran];
	      opac = new double[ntran];
	      freq = new double[ntran];
	      upper = new double[ntran];
	      ergs = new double[ntran];
	      trad = new double[ntran];

	     ntran = -1;
	     for (iline=0; iline <nline; iline++)
	     {
			m  = iupp[iline];
			n  = ilow[iline];
			xt = Math.pow(xnu[iline],3.0);

			//     Calculate source function
			hnu = fk*xnu[iline]/tex[iline];
			if (hnu >= 160.0) {
			  bnutex = 0.0;
			} else {
			  bnutex = thc*xt/(Math.exp(fk*xnu[iline]/tex[iline])-1.0);
			}

			//     Calculate line brightness in excess of background
			ftau = 0.0;
			if (Math.abs(taul[iline]) <= 300.0) ftau = Math.exp(-taul[iline]);
			toti = backi[iline]*ftau+bnutex*(1.0-ftau);
			if (toti == 0.0) {
			  tbl = 0.0;
			} else {
			  wh = thc*xt/toti+1.0;
			  if (wh <= 0.0) {
			    tbl = toti/(thc*xnu[iline]*xnu[iline]/fk);
			  } else {
			    tbl = fk*xnu[iline]/Math.log(wh);
			  }
			}
			if (backi[iline] == 0.0) {
			  tback = 0.0;
			} else {
			  tback = fk*xnu[iline]/Math.log(thc*xt/backi[iline]+1.0);
			}

			//     Calculate antenna temperature
			tbl = tbl-tback;
		    hnu = fk*xnu[iline];
		    if(Math.abs(tback/hnu) <= 0.02) {
			  ta = toti;
		    } else {
		      ta = toti-backi[iline];
		    }
		    ta = ta/(thc*xnu[iline]*xnu[iline]/fk);

		    //     Calculate radiation temperature
			beta = escprob(taul[iline]);
			bnu  = totalb[iline]*beta+(1.0-beta)*bnutex;
			if(bnu == 0.0) {
			  tr = totalb[iline];
			} else {
			  wh = thc*xt/bnu+1.0;
			  if(wh <= 0.0) {
			    tr = bnu/(thc*xnu[iline]*xnu[iline]/fk);
			  } else {
			    tr = fk*xnu[iline]/Math.log(wh);
			  }
			}

			//    Check if line within output freq range
			if (isInRange(spfreq[iline]))
			{
				ntran ++;

		          // wavel = CGSConstant.SPEED_OF_LIGHT / spfreq[iline] / 1.0E5; // unit =  micron
		          kkms  = 1.0645*deltav*ta;
		          ergs[ntran]  = fgaus*CGSConstant.BOLTZMANN_CONSTANT*deltav*ta*Math.pow(xnu[iline],3.0);
		          trad[ntran] = tr;
		          transition[ntran] = ""+qnum[m]+"-"+qnum[n];
		          tantenna[ntran] = ta;
		          flux[ntran] = kkms/1.0E5;
		          exc[ntran] = tex[iline];
		          opac[ntran] = taul[iline];
		          freq[ntran] = spfreq[iline];
		          upper[ntran] = eup[iline];
			}
	   }
	}
	private double exc[];
	private double opac[];
	private double freq[];
	private double upper[];
	private String transition[];
	private double tantenna[];
	private double flux[];
	private double ergs[];
	private double trad[];

	private boolean isInRange(double freq) throws JPARSECException { // input in GHz
		double range = 10; // 10 MHz standard margin, large due probably to errors in the molecular data of radex
		if (molfile == 18 || molfile == 20 || molfile == 27 || molfile == 38) range = 0.001; // 0.001 MHz for HFS

		if (Math.abs(freq - fmin) * 1000.0 < range) return true;
		if (Math.abs(freq - fmax) * 1000.0 < range) return true;

		if (freq <= fmax && freq >= fmin) return true;
		return false;
	}

	/**
	 * Returns the energy of the upper level.
	 * @param line Index for the transition.
	 * @return Upper level energy, in K.
	 */
	public double getUpperLevelEnergy(int line)
	{
		return upper[line];
	}
	/**
	 * Returns the frequency of the transition.
	 * @param line Index for the transition.
	 * @return The frequency in GHz.
	 */
	public double getFrequency(int line)
	{
		return freq[line];
	}
	/**
	 * Returns the number of transitions calculated.
	 * @return Number of transitions.
	 */
	public int getNumberOfTransitions()
	{
		return transition.length;
	}
	/**
	 * Returns the excitation temperature.
	 * @param line Index for the transition.
	 * @return Excitation temperature, in K.
	 */
	public double getExcitationTemperature(int line)
	{
		return exc[line];
	}
	/**
	 * Returns the opacity of certain transition.
	 * @param line Index for the transition.
	 * @return Opacity.
	 */
	public double getOpacity(int line)
	{
		return opac[line];
	}
	/**
	 * Returns the transition name.
	 * @param line Index for the transition.
	 * @return Transition name, defined as
	 * upper level-lower level.
	 */
	public String getName(int line)
	{
		return transition[line];
	}
	/**
	 * Returns the radiation temperature.
	 * @param line Index for the transition.
	 * @return Radiation temperature, in K.
	 */
	public double getRadiationTemperature(int line)
	{
		return trad[line];
	}
	/**
	 * Returns the antenna temperature.
	 * @param line Index for the transition.
	 * @return Antenna temperature, in K.
	 */
	public double getAntennaTemperature(int line)
	{
		return tantenna[line];
	}
	/**
	 * Returns the flux.
	 * @param line Index for the transition.
	 * @return The flux, in K km/s.
	 */
	public double getFlux(int line)
	{
		return flux[line];
	}
	/**
	 * Returns the flux in CGS units.
	 * @param line Index for the transition.
	 * @return The flux, in erg s.
	 */
	public double getFluxInCGS(int line)
	{
		return ergs[line];
	}

	private boolean matrix(int niter)
	throws JPARSECException {
		//     Set up rate matrix

	      int ilev,jlev,klev;   // to loop over energy levels
	      int nplus;            // to solve statistical equilibrium
	      int iline;            // to loop over lines
	      int m,n;              // line upper/lower levels
	      int nthick = 0;           // counts optically thick lines
	      int nfat = 0;             // counts highly optically thick lines
	      int nreduce;          // size of reduced rate matrix
//	      int indx,dsign;       // needed for NumRep equation solver

	      double rhs[] = new double[nlev+1]; //(maxlev)           // RHS of rate equation
	      double yrate[][] = new double[nlev+1][nlev+1]; //(maxlev,maxlev)  // rate matrix

	      double etr,exr;               // to calculate radiative rates
	      double xt;                    // frequency cubed
	      // double hnu;                   // photon energy
	      // double bnutex;                // line source function
	      double cddv = 0.0;                  // N(mol) / delta V
	      double beta;          // escape probability
	      double bnu;                   // Planck function
	      double uarray[][] = new double[nlev][nlev]; //(maxlev,maxlev) // reduced rate matrix
	      double redcrit;               // reduction criterion
	      double sumx;                  // summed radiative rate
	      double total;                 // to normalize populations
	      double tsum,thistex;          // to check convergence

	      boolean conv = false;                // are we converged?

	      double xpopold[] = new double[nlev];
	      boolean reduce = false;

	//     Executable statements begin here
	//     Clear array of level populations.
	      for (ilev = 0; ilev < nlev; ilev ++)
	      {
	    	  rhs[ilev] = 0.0;
		      for (jlev = 0; jlev < nlev; jlev ++)
		      {
		          yrate[ilev][jlev] = 0.0;
		      }
	      }

	//     Initialize rate matrix
	      nplus = nlev;
	      for (ilev = 0; ilev < nlev; ilev ++)
	      {
		      for (jlev = 0; jlev < nlev; jlev ++)
		      {
		          yrate[ilev][jlev] =  -EPS*totdens;
		      }
		  	//     Add conservation equation
		      yrate[nplus][ilev] = 1.0;
		      rhs[ilev] = EPS*totdens;
		      yrate[ilev][nplus] = EPS*totdens;
	      }
	      rhs[nplus] = EPS*totdens;

	      //     Contribution of radiative processes to the rate matrix.

	      //     First iteration: use background intensity
	      if (niter == 0) {
	    	  for (iline=0; iline < nline; iline ++)
	    	  {
	            m   = iupp[iline];
	            n   = ilow[iline];
	            etr = fk*xnu[iline]/trj[iline];
	            if (etr >= 160.0) {
	               exr = 0.0;
	            } else {
	               exr = 1.0/(Math.exp(etr)-1.0);
	            }
				yrate[m][m] += aeinst[iline]*(1.0+exr);
				yrate[n][n] += aeinst[iline]*gstat[m]*exr/gstat[n];
				yrate[m][n] += - aeinst[iline]*(gstat[m]/gstat[n])*exr;
				yrate[n][m] += - aeinst[iline]*(1.0+exr);
	    	  }
	      } else {
	//     Subsequent iterations: use escape probability.
	         cddv = cdmol / deltav;
	//     Count optically thick lines
	         nthick = 0;
	         nfat   = 0;

	    	  for (iline=0; iline < nline; iline ++)
	    	  {
	            xt  = Math.pow(xnu[iline],3.0);
	            m   = iupp[iline];
	            n   = ilow[iline];

	//     Calculate source function
/*	            hnu = fk * xnu[iline] / tex[iline];
	            if (hnu >= 160.0) {
	               bnutex = 0.0;
	            } else {
	            	bnutex = thc*xt/(Math.exp(fk*xnu[iline]/tex[iline])-1.0);
	            }
*/
	//     Calculate line optical depth.
	            taul[iline] = cddv*(xpop[n]*gstat[m]/gstat[n]-xpop[m])/(fgaus*xt/aeinst[iline]);
	            if (taul[iline] > 1.0E-2) nthick = nthick+1;
	            if (taul[iline] > 1.0E5) nfat   = nfat+1;

	//     Use escape probability approximation for internal intensity.
	            beta = escprob(taul[iline]);
	            bnu  = totalb[iline]*beta; //+(1.0-beta)*bnutex; // 30nov2011 version eliminates this
	            exr  = bnu/(thc*xt);

	//     Radiative contribution to the rate matrix
				yrate[m][m] += aeinst[iline]*(beta+exr); // 30nov2011
				yrate[n][n] += aeinst[iline]*gstat[m]*exr/gstat[n];
				yrate[m][n] += - aeinst[iline]*(gstat[m]/gstat[n])*exr;
				yrate[n][m] += - aeinst[iline]*(beta+exr); // 30nov2011
	    	  }
	      }

	//     Warn user if convergence problems expected
	      if (niter == 1 && nfat > 0) JPARSECException.addWarning("some lines have very high optical depth");

	//     Contribution of collisional processes to the rate matrix.
	      for (ilev = 0; ilev < nlev; ilev ++)
	      {
	    	  yrate[ilev][ilev] += ctot[ilev];
		      for (jlev = 0; jlev < nlev; jlev ++)
		      {
		           if(ilev != jlev) yrate[ilev][jlev] += - crate[jlev][ilev];
		      }
	      }

	      if (reduce)
	      {
	//     An auxiliary array is passed to the linear equation solver after
	//     renormalization. The array Y retains the original matrix elements.
	    	  //System.out.println("reducing matrix");
		      for (ilev = 0; ilev < nlev; ilev ++)
		      {
			      for (jlev = 0; jlev < nlev; jlev ++)
			      {
			            uarray[ilev][jlev] = yrate[ilev][jlev];
			      }
		      }

	//     Now test whether the matrix should be reduced
	//     to exclude the radiatively coupled levels.
		      redcrit = 10.0*tkin/fk;
		      nreduce = 0;
		      for (ilev = 0; ilev < nlev; ilev ++)
		      {
		         if (eterm[ilev] <= redcrit) nreduce = nreduce+1;
		      }

	//     We now separate the collisionally coupled levels from those that
	//     are coupled mainly by radiative processes, compute an effective
	//     cascade matrix for rates of transfer from one low-lying level
	//     to another and then solve this reduced system of equations
	//     explicitly for the low-lying levels only.
		      for (jlev = 0; jlev < nreduce; jlev ++)
		      {
			      for (ilev = 0; ilev < nreduce; ilev ++)
			      {
				      for (klev = nreduce; klev < nlev; klev ++)
				      {
				    	  uarray[ilev][jlev] += Math.abs(yrate[klev][jlev]*yrate[ilev][klev]/yrate[klev][klev]);
				      }
			      }
		      }

		      //     Invert this reduced matrix
		      Matrix matrix = new Matrix(uarray);
		      matrix.deleteRow(matrix.getRowDimension()-1);
		      matrix.deleteColumn(matrix.getColumnDimension()-1);
		      double barray[] = new double[nreduce];
		      for (int bin = 0; bin < matrix.getColumnDimension(); bin ++)
		      {
		    	  barray[bin] = 0.0;
		    	  matrix.set(matrix.getRowDimension(), bin, 1.0);
		      }
		      barray[barray.length-1] = 1.0;

		      double rhsOut[][] = matrix.solve(new Matrix(barray)).getArray();
		      for (int bin = 0; bin < rhsOut.length; bin ++)
		      {
		    	  rhs[bin] = rhsOut[bin][0];
		      }

		      //     Compute the populations of the highly excited states
		      if (nlev > nreduce) {
			      for (klev = nreduce; klev < nlev; klev ++)
			      {
			    	  sumx = 0.0;
				      for (jlev = 0; jlev < nreduce; jlev ++)
				      {
				    	  sumx = rhs[jlev]*yrate[klev][jlev] + sumx;
				      }
			    	  rhs[klev] = Math.abs(sumx/yrate[klev][klev]);
			      }
		      }
	      } else {  //if we don't want to reduce
		      Matrix matrix = new Matrix(yrate);
		      matrix.deleteRow(matrix.getRowDimension()-1);
		      matrix.deleteColumn(matrix.getColumnDimension()-1);
		      double barray[] = new double[nplus];
		      for (int bin = 0; bin < matrix.getColumnDimension(); bin ++)
		      {
		    	  barray[bin] = 0.0;
		    	  matrix.set(matrix.getRowDimension()-1, bin, 1.0);
		      }
		      barray[barray.length-1] = 1.0;

		      double rhsOut[][] = matrix.solve(new Matrix(barray)).getArray();
		      for (int bin = 0; bin < rhsOut.length; bin ++)
		      {
		    	  rhs[bin] = rhsOut[bin][0];
		      }
	      }

	      //     Level populations are the normalized RHS components
	      total = 0.0;
	      for (ilev = 0; ilev < nlev; ilev ++)
	      {
	         total += rhs[ilev];
	      }

	      //     Limit population to MIN_POP
	      for (ilev = 0; ilev < nlev; ilev ++)
	      {
	    	  if (niter > 0) xpopold[ilev] = xpop[ilev];
	    	  xpop[ilev] = DataSet.getMaximumValue(new double[] {MIN_POP, rhs[ilev]/total});

	    	  if (niter == 0) xpopold[ilev] = xpop[ilev];
	      }

	      //     Compute excitation temperatures of the lines
	      tsum = 0.0;
    	  for (iline=0; iline < nline; iline ++)
    	  {
			m  = iupp[iline];
			n  = ilow[iline];
	        xt = Math.pow(xnu[iline], 3.0);
	        if (niter == 0) {
	           if (xpop[n] <= MIN_POP || xpop[m] <= MIN_POP) {
	              tex[iline] = totalb[iline];
	           } else {
	              tex[iline] = fk*xnu[iline]/(Math.log(xpop[n]*gstat[m]/(xpop[m]*gstat[n])));
	           }
	        } else {
	           if (xpop[n] <= MIN_POP || xpop[m] <= MIN_POP) {
	              thistex = tex[iline];
	           } else {
	              thistex = fk*xnu[iline]/(Math.log(xpop[n]*gstat[m]/(xpop[m]*gstat[n])));
	           }
	           //     Only thick lines count for convergence
	           if(taul[iline] > 0.01) tsum += Math.abs((thistex-tex[iline])/thistex);
	           //     Update excitation temperature & optical depth
	           tex[iline]  = 0.5*(thistex+tex[iline]);
	           taul[iline] = cddv*(xpop[n]*gstat[m]/gstat[n]-xpop[m])/(fgaus*xt/aeinst[iline]);
	        }
    	  }

    	  //     Introduce a minimum number of iterations
	      if(niter >= MIN_ITER) {
	         if (nthick == 0) conv = true;
	         if( tsum/nthick < CCRIT) conv = true;
	      }

	      // now do the underrelaxation! (30nov2011 version)
	      for (ilev = 0; ilev < nlev; ilev ++) {
	    	  xpop[ilev]=0.3*xpop[ilev]+0.7*xpopold[ilev];
	      }
	      return conv;
	}

	private double escprob(double tau)
	{
	      double beta = 0.0;
	      double taur;  //optical radius

	      taur = tau/2.0;

	      switch (this.method)
	      {
	      case UNIFORM_SPHERE:
	    	  //     Uniform sphere formula from Osterbrock (Astrophysics of
	    	  //     Gaseous Nebulae and Active Galactic Nuclei) Appendix 2
	    	  //     with power law approximations for large and small tau
	         if(Math.abs(taur) < 0.1) {
	            beta = 1.0-0.75*taur+(taur*taur)/2.5-(Math.pow(taur,3.0))/6.0+(Math.pow(taur,4.0))/17.5;
	         } else {
	        	 if(Math.abs(taur) > 50) {
	        		 beta = 0.75/taur;
	        	 } else {
	        		 beta = 0.75/taur*(1.0-1.0/(2.0*(taur*taur))+(1.0/taur+1.0/(2.0*(taur*taur)))*Math.exp(-2.*taur));
	        	 }
	         }
	      break;
	      case EXPANDING_SPHERE:
	    	  //     Expanding sphere = Large Velocity Gradient (LVG) or Sobolev case.
	    	  //     Formula from De Jong, Boland and Dalgarno (1980, A&A 91, 68)
	    	  //     corrected by factor 2 in order to match ESCPROB(TAU=0)=1
	        if (Math.abs(taur) < 0.01) {
	          beta = 1.0;
	        } else {
	        	if(Math.abs(taur) < 7.0) {
	        		beta = 2.0*(1.0 - Math.exp(-2.34*taur))/(4.68*taur);
	        	} else {
	        		beta = 2.0/(taur*4.0*(Math.sqrt(Math.log(taur/Math.sqrt(Math.PI)))));
	        	}
	        }
	        break;
	      case PLANE_PARALLEL_SLAB:
	    	  //     Slab geometry (e.g., shocks): de Jong, Dalgarno & Chu 1975,
	    	  //     ApJ 199, 69 (again with power law approximations)
	        if (Math.abs(3.0*tau) < 0.1) {
	          beta = 1.0 - 1.5*(tau + tau*tau);
	        } else {
	        	if (Math.abs(3.0*tau) > 50.0) {
	        		beta = 1.0/(3.0*tau);
	        	} else {
	        		beta = (1.0- Math.exp(-3.0*tau))/(3.0*tau);
	        	}
	        }
	        break;
	      }
	      return beta;
	}

	private void backrad()
	throws JPARSECException {
		// This routine returns the intensity of continuum radiation that is
		// felt by the radiating molecules.  Intensity is computed at the
		// line frequencies only.

		// OPTIONS:
		//  1 - Single blackbody; default is the cosmic microwave background
		//      at T_CMB=2.725 K.  For values different from the default, the
		//      corresponding redshift is determined according to
		//      T=T_CMB(1+z)
		//  2 - The mean Galactic background radiation field plus CMB. This
		//      is a slightly revised version of Black, J. H. 1994, in
		//      The First Symposium on the Infrared Cirrus and Diffuse
		//      Interstellar Clouds. ASP Conference Series, Vol. 58,
		//      R.M. Cutri and W.B. Latter, Eds., p.355. Details are posted
		//      at http://www.oso.chalmers.se/~jblack/RESEARCH/isrf.html
		//      This spectrum is NOT adjustable by a scale factor because
		//      it consists of several components that are not expected to
		//      scale linearly with respect to each other.
		//  3 - A user-defined radiation field that is specified by NRAD
		//      values of frequency [cm-1], intensity [Jy nsr-1], and dilution
		//      factor [dimensionless]. Spline interpolation is applied to
		//      this table. The intensity need not be specified at all
		//      frequencies of the line list, but a warning message will
		//      appear if extrapolation (rather than interpolation) is required.

		      int iline;
		      double hnu;

		//     bgfile:  file with user's background
		//     title:   one-liner describing user's background
		//     nrad:    number of wavelength points in user's background
		//     irad:    to loop over background wavelength
		//     iline:   to loop over lines
		//     xnubr:   frequencies of user's background [cm^-1]
		//     spinbr:  intensities of user's background [Jy/nsr]
		//     dilbr:   dilution factors of user's background
		//     hnu:     helps to calculate intensity
		//     tbb3:    cmb addition to user's background
		//     cbi:     cmb intensity

		//     logfreq: base 10 log of frequency
		//     logflux: base 10 log of intensity
		//     fpp:     helps to calculate splines
		//     aa,bb:   help to calculate Planck function
		//     fout:    interpolated intensity
		//     xnumin,xnumax: min/max line frequencies
		//     xln:     log of line frequency

		//     Executable statements begin here
		      backi = new double[nline];
		      trj = new double[nline];
		      totalb = new double[nline];
		      if (tbg > 0.0) {
		    	  //     option 1: Single black body
		    	  for (iline=0; iline < nline; iline++)
		    	  {
		            hnu = fk*xnu[iline]/tbg;
		            if (hnu >= 160.0) {
		               backi[iline]=EPS; // Do not set backi to zero: may propagate into T(ex) and if line is thick, Tsum = NaN and code never converges ...
		            } else {
		               backi[iline] = thc*(Math.pow(xnu[iline], 3.0))/(Math.exp(fk*xnu[iline]/tbg)-1.0);
		            }
		            trj[iline]    = tbg;
		            totalb[iline] = backi[iline];
		    	  }
		      } else {
		    	  if(tbg == 0.0) {
		    		  //     option 2:  mean Galactic background radiation
		    		  galbr();
		    	  } else {
		    		  throw new JPARSECException("background temperature cannot be negative.");
		    	  }
		      }
	}

	private void galbr()
	throws JPARSECException {
	      int iline;
	      double aa,hnuk,cbi,cmi,cmib,yy,xla,ylg;
	      final double tcmb=2.725;

	      //     aa,hnuk: help to calculate Planck function
	      //     tcmb:    CMB temperature
	      //     cbi:     CMB intensity
	      //     cmi:     synchrotron radiation intensity
	      //     cmib:    dust radiation intensity
	      //     yy,xla,ylg: to calculate stellar radiation field

    	  for (iline=0; iline < nline; iline++)
    	  {
	       aa   = thc*Math.pow(xnu[iline], 3.0);
	       hnuk = fk*xnu[iline]/tcmb;

	       if (xnu[iline] <= 10.0) {
	        cbi = aa/(Math.exp(hnuk) - 1.0);
	        cmi = 0.3*1.767E-19/Math.pow(xnu[iline],0.75);     // synchrotron component
	       } else {
	    	   if (xnu[iline] <= 104.98) {
		        cbi  = aa/(Math.exp(hnuk) - 1.0);
		        cmib = aa/(Math.exp(fk*xnu[iline]/23.3) - 1.0);
		        cmi  = 0.3*5.846E-7*Math.pow(xnu[iline], 1.65)*cmib;  // COBE single-T dust
	    	   } else {
	    		   if (xnu[iline] <= 1113.126) {
			        cmi = 1.3853E-12*Math.pow(xnu[iline], -1.8381);
			        cbi = 0.0;
	    		   } else {
	    			   if (xnu[iline] <= 4461.40) {
				        cbi = 0.0;
				        cmi = 1.0E-18*(18.213601 - 0.023017717*xnu[iline] + 1.1029705E-5*Math.pow(xnu[iline], 2.0)
				        		- 2.1887383E-9*Math.pow(xnu[iline], 3.0) + 1.5728533E-13*Math.pow(xnu[iline], 4.0));
	    			   } else {
	    				   if (xnu[iline] <= 8333.33) {
					        cbi = 0.0;
					        cmi = 1.0E-18*(-2.4304726 + 0.0020261152*xnu[iline]- 2.0830715E-7*Math.pow(xnu[iline], 2.0)
					        		+ 6.1703393E-12*Math.pow(xnu[iline], 3.0));
	    				   } else {
	    					   if (xnu[iline] <= 14286.0) {
						        yy  = -17.092474 - 4.2153656E-5*xnu[iline];
						        cbi = 0.0;
						        cmi = Math.pow(10.0, yy);
	    					   } else {
	    						   if (xnu[iline] <= 40000.0) {
							        xla = 1.0E8/xnu[iline];
							        ylg = -1.7506877E-14*Math.pow(xla,4.0)+ 3.9030189E-10*Math.pow(xla, 3.0) + 3.1282174E-7*(xla*xla)
							        	- 3.0189024E-3*xla + 2.0845155;
							        cmi = 1.581E-24*xnu[iline]*ylg;
							        cbi = 0.0;
	    						   } else {
	    							   if (xnu[iline] <= 55556.0) {
								        xla = 1.0E8/xnu[iline];
								        ylg = -0.56020085 + 9.806303E-4*xla;
								        cmi = 1.581E-24*xnu[iline]*ylg;
								        cbi = 0.0;
	    							   } else {
	    								   if (xnu[iline] <= 90909.0) {
									        xla = 1.0E8/xnu[iline];
									        ylg = -21.822255 + 3.2072800E-2*xla - 7.3408518E-6*xla*xla;
									        cmi = 1.581E-25*xnu[iline]*ylg;
									        cbi = 0.0;
	    								   } else {
	    									   if (xnu[iline] <= 109678.76) {
										        xla = 1.0E8/xnu[iline];
										        ylg = 30.955076 - 7.3393509E-2*xla + 4.4906433E-5*xla*xla;
										        cmi = 1.581E-25*xnu[iline]*ylg;
										        cbi = 0.0;
	    									   } else {
												   // radiation field extends to lyman limit of h
	    										   JPARSECException.addWarning("xnu = "+xnu[iline]+" is outside the range of the fitting function and beyond the Lyman limit.");
											        cbi=0.0;
											        cmi=0.0;
	    									   }
	    								   }
	    							   }
	    						   }
	    					   }
	    				   }
	    			   }
	    		   }
	    	   }
	       }

		      backi[iline] = cbi+cmi;
		      trj[iline]   = fk*xnu[iline]/Math.log(1.0+aa/backi[iline]);    // brightness temperature
		      totalb[iline] = backi[iline]; // 24aug2011 version
    	  }
	}

	/**
	 * Return the integer identifier for a given molecule in RADEX, considering possible
	 * orto and para cases.
	 * @param molecule The molecule name, as given in the JPL or COLOGNE databases, and contained
	 * in arrays in this class ({@linkplain #MOLECULE_ATOM_NAMES_CORRESPONDENCE_JPL_CATALOG} and {@linkplain #MOLECULE_ATOM_NAMES_CORRESPONDENCE_COLOGNE_CATALOG}).
	 * @param jpl True for JPL, false for COLOGNE.
	 * @param freq The frequency in GHz of the line you are interested.
	 * @return The integer identifier.
	 * @throws JPARSECException If an error occurs.
	 */
	public static int getMoleculeRadexID(String molecule, boolean jpl, double freq) throws JPARSECException {
		int mol = -1, n[];
		if (jpl) {
			mol = DataSet.getIndexContaining(RADEX.MOLECULE_ATOM_NAMES_CORRESPONDENCE_JPL_CATALOG, molecule);
			n = DataSet.getRepeatedElements(MOLECULE_ATOM_NAMES_CORRESPONDENCE_JPL_CATALOG, RADEX.MOLECULE_ATOM_NAMES_CORRESPONDENCE_JPL_CATALOG[mol]);
		} else {
			mol = DataSet.getIndexContaining(RADEX.MOLECULE_ATOM_NAMES_CORRESPONDENCE_COLOGNE_CATALOG, molecule);
			n = DataSet.getRepeatedElements(MOLECULE_ATOM_NAMES_CORRESPONDENCE_COLOGNE_CATALOG, RADEX.MOLECULE_ATOM_NAMES_CORRESPONDENCE_COLOGNE_CATALOG[mol]);
		}

		if (n.length > 1) {
			double minDif = -1;
			int index = -1;
			for (int i=0; i<n.length; i++) {
				double freqs[] = getFreqs(n[i]);
				for (int j=0; j<freqs.length; j++) {
					double d = Math.abs(freqs[j] - freq);
					if (d < minDif || minDif == -1) {
						minDif = d;
						index = i;
					}
				}
			}
			if (index >= 0) return n[index];
		}
		return mol;
	}

	private static double[] getFreqs(int molfile) throws JPARSECException {
	      String jarpath = FileIO.DATA_RADEX_DIRECTORY + RADEX.MOLECULE_ATOM_NAMES[molfile].toLowerCase() + ".dat";
	      String file[] = DataSet.arrayListToStringArray(ReadFile.readResource(jarpath));

	      // Remove tabs
	      file = DataSet.replaceAll(file, "\t", "  ", true);

	      int nlev = Integer.parseInt(file[5].trim());

	  	//     Radiative upper & lower levels and Einstein coefficients
	      int nline = Integer.parseInt(file[8+nlev].trim());
	      double[] spfreq = new double[nline];
	      for (int i=0; i<nline; i++)
	      {
	    	  spfreq[i] = DataSet.parseDouble(FileIO.getField(5, file[10+nlev+i], " ", true));
	      }

	      return spfreq;
	}

	/**
	 * Updates calculations in case some values of the instance were modified by hand.
	 * @throws JPARSECException If an error occurs.
	 */
	public void update() throws JPARSECException {
		this.check();
		this.execute(false);
	}
}
