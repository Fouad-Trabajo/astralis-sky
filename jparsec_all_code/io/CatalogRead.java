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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import jparsec.graph.DataSet;
import jparsec.math.Constant;
import jparsec.util.DataBase;
import jparsec.util.JPARSECException;
import jparsec.util.Logger;
import jparsec.util.Logger.LEVEL;
import jparsec.util.Translate;

/**
 * Performs file and parameter reading operations. This class supports JPL and
 * COLOGNE database of molecular spectroscopy.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class CatalogRead
{
	// private constructor so that this class cannot be instantiated.
	private CatalogRead() {}

	/**
	 * Maximum number of transitions to read. 30000 is the default value.
	 */
	public static int maxTransitions = 30000;

	/**
	 * Read JPL Catalog main file.
	 *
	 * @return Strings with the list of molecules.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public static ArrayList<String> readJPLcatalog() throws JPARSECException
	{
		ArrayList<String> v = new ArrayList<String>();

		String fich = "catdir.cat";

		// Initial variables and URLs definition
		String line;

		int read_err = 0;

		// Lets read the catalog entries
		try
		{
			InputStream is = CatalogRead.class.getClassLoader().getResourceAsStream("JPL/"+fich);
			BufferedReader dis = new BufferedReader(new InputStreamReader(is));

			while ((line = dis.readLine()) != null)
			{

				String mol = line;
				mol = getMoleculeFileName(mol);

				// Determine whether the molecule transitions file exist or not
				try
				{
					is = CatalogRead.class.getClassLoader().getResourceAsStream("JPL/"+mol);
					BufferedReader dis_mol = new BufferedReader(new InputStreamReader(is));

					dis_mol.close();
				} catch (FileNotFoundException e0)
				{
					Logger.log(LEVEL.ERROR, "Cannot find resource JPL/"+mol);
					// throw new JPARSECException( "file not found.", e0);
					read_err = 1;
				} catch (IOException e1)
				{
					Logger.log(LEVEL.ERROR, "Cannot read resource JPL/"+mol);
					// throw new JPARSECException(e1);
					read_err = 2;
				}

				/* If there is no errors then the molecule transitions file is
				 * in the program directory and we can access it. So, we can
				 * add this molecule to the menu later
				 */
				if (read_err == 0)
				{
					v.add(line);
				}

			}
			dis.close();

		} catch (FileNotFoundException e2)
		{
			throw new JPARSECException("file not found " + fich+".", e2);
		} catch (IOException e3)
		{
			throw new JPARSECException("error while reading file " + fich + ".", e3);
		}

		return v;
	}

	/**
	 * Read JPL molecule transitions file.
	 *
	 * @param mol Molecule file name.
	 * @param limit_temp Temperature limit to search for transitions. Only
	 *        transitions with upper level energy below this value will be
	 *        returned. 0 will return all transitions.
	 * @return Strings with the list of transitions.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public static ArrayList<String> readJPLtransitions(String mol, double limit_temp) throws JPARSECException
	{
		return readJPLtransitions(mol, limit_temp, 0);
	}

	/**
	 * Read JPL molecule transitions file.
	 *
	 * @param mol Molecule file name.
	 * @param limit_temp Temperature limit to search for transitions. Only
	 *        transitions with upper level energy below this value will be
	 *        returned. 0 will return all transitions.
	 * @param limit_rint Minimum value of the <i>rint</i> parameter of the transition, which
	 * is associated with the intensity of the line. Set to 0 to avoid this condition.
	 * @return Strings with the list of transitions.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public static ArrayList<String> readJPLtransitions(String mol, double limit_temp, double limit_rint) throws JPARSECException
	{
		ArrayList<String> v = new ArrayList<String>();

		// Initialize variables
		String fich = mol;
		double trans_temp = 0.0;
		int ncat = -1;
		String energy, line;

		// Now lets read the molecule transitions file
		try
		{
			InputStream is = CatalogRead.class.getClassLoader().getResourceAsStream("JPL/"+fich);
			BufferedReader dis = new BufferedReader(new InputStreamReader(is));

			while ((line = dis.readLine()) != null)
			{
				double frequency = DataSet.parseDouble(line.substring(0, 13).trim());

				energy = line.substring(31, 41).trim();
				if (energy.equals(""))
				{
					energy = "0";
				}
				trans_temp = frequency * Constant.HZ_TO_K * 1.0E+6 + Constant.CM_TO_K * DataSet.parseDouble(energy);
				double rint = DataSet.parseDouble(line.substring(21, 29).trim());

				/* We add the transition to the menu if the energy is less than
				 * the user input temperature. We also add the frequency to the
				 * transition quantum numbers. We consider a memory limit of max
				 * transitions
				 */
				if ((rint > limit_rint || limit_rint == 0.0) && (trans_temp < limit_temp || limit_temp == 0.0) && ncat < maxTransitions)
				{
					ncat++;
					v.add(line);
				}
			}

			dis.close();

			if (ncat == maxTransitions)
				JPARSECException
						.addWarning(Translate.translate(256));
		} catch (FileNotFoundException e2)
		{
			throw new JPARSECException("file not found " + fich+".", e2);
		} catch (IOException e3)
		{
			throw new JPARSECException("error while reading file " + fich + ".", e3);
		}

		return v;
	}

	/**
	 * Read COLOGNE Catalog main catalog.
	 *
	 * @return Strings with the list of molecules.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public static ArrayList<String> readCOLOGNEcatalog() throws JPARSECException
	{
		ArrayList<String> v = new ArrayList<String>();

		String fich = "catdir.cat";

		// Initial variables and URLs definition
		String line;

		// Lets read the catalog entries
		try
		{
			InputStream is = CatalogRead.class.getClassLoader().getResourceAsStream("COLOGNE/"+fich);
			BufferedReader dis = new BufferedReader(new InputStreamReader(is));

			// We read the first two information lines of this file
			line = dis.readLine();
			line = dis.readLine();

			while ((line = dis.readLine()) != null)
			{

				String mol = line;
				mol = getMoleculeFileName(mol);

				// Determine whether the molecule transitions file exist or not
				int read_err = 0;
				try
				{
					is = CatalogRead.class.getClassLoader().getResourceAsStream("COLOGNE/"+mol);
					BufferedReader dis_mol = new BufferedReader(new InputStreamReader(is));

					dis_mol.close();
				} catch (FileNotFoundException e0)
				{
					Logger.log(LEVEL.ERROR, "Cannot find resource COLOGNE/"+mol);
					// throw new JPARSECException("file not found "+fich, e0);
					read_err = 1;
				} catch (IOException e1)
				{
					Logger.log(LEVEL.ERROR, "Cannot read resource COLOGNE/"+mol);
					// throw new JPARSECException(e1);
					read_err = 2;
				}

				/* If there is no errors then the molecule transitions file is
				 * in the program directory and we can access it. So, we can add this
				 * molecule to the menu later
				 */
				if (read_err == 0)
				{
					v.add(line);
				}

			}
			dis.close();

		} catch (FileNotFoundException e2)
		{
			throw new JPARSECException("file not found " + fich+".", e2);
		} catch (IOException e3)
		{
			throw new JPARSECException(e3);
		}

		return v;
	}

	/**
	 * Read COLOGNE molecule transitions file.
	 *
	 * @param mol Molecule file name.
	 * @param limit_temp Temperature limit to search for transitions. Only
	 *        transitions with upper level energy below this value will be
	 *        returned. 0 will return all transitions.
	 * @return Array of strings with the list of transitions.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public static ArrayList<String> readCOLOGNEtransitions(String mol, double limit_temp) throws JPARSECException
	{
		return readCOLOGNEtransitions(mol, limit_temp, 0);
	}

	/**
	 * Read COLOGNE molecule transitions file.
	 *
	 * @param mol Molecule file name.
	 * @param limit_temp Temperature limit to search for transitions. Only
	 *        transitions with upper level energy below this value will be
	 *        returned. 0 will return all transitions.
	 * @param limit_rint Minimum value of the <i>rint</i> parameter of the transition, which
	 * is associated with the intensity of the line. Set to 0 to avoid this condition.
	 * @return Array of strings with the list of transitions.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public static ArrayList<String> readCOLOGNEtransitions(String mol, double limit_temp, double limit_rint) throws JPARSECException
	{
		ArrayList<String> v = new ArrayList<String>();

		// Initialize variables
		String fich = mol, line;
		double trans_temp = 0.0;
		int ncat = -1;
		String energy;

		// Now lets read the molecule transitions file
		try
		{
			InputStream is = CatalogRead.class.getClassLoader().getResourceAsStream("COLOGNE/"+fich);
			BufferedReader dis = new BufferedReader(new InputStreamReader(is));

			while ((line = dis.readLine()) != null)
			{

				energy = line.substring(31, 41).trim();
				if (energy.equals(""))
				{
					energy = "0";
				}
				trans_temp = Constant.CM_TO_K * DataSet.parseDouble(energy);
				double rint = DataSet.parseDouble(line.substring(21, 29).trim());

				/* We add the transition to the menu if the energy is less than
				 * the user input temperature. We also add the frequency to the
				 * transition quantum numbers. We consider a memory limit of max
				 * transitions
				 */
				if ((rint > limit_rint || limit_rint == 0.0) && (trans_temp < limit_temp || limit_temp == 0.0) && ncat < maxTransitions)
				{
					ncat++;
					v.add(line);
				}

			}

			dis.close();

			if (ncat == maxTransitions)
				JPARSECException
						.addWarning(Translate.translate(256));

		} catch (FileNotFoundException e2)
		{
			throw new JPARSECException("file not found " + fich+".", e2);
		} catch (IOException e3)
		{
			throw new JPARSECException("error while reading file " + fich + ".", e3);
		}

		return v;
	}

	private static ArrayList<String> v_JPL = null, v_COLOGNE = null;

	/**
	 * Gets a string with the data of certain molecule.
	 *
	 * @param name Name of the molecule as given in the catalog.
	 * @param jpl_cat True for JPL catalog, false for COLOGNE.
	 * @return Line of the catalog with the molecule information.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String getMolecule(String name, boolean jpl_cat) throws JPARSECException
	{
		ArrayList<String> v;

		if (jpl_cat) {
			if (v_JPL == null) v_JPL = CatalogRead.readJPLcatalog();
			v = v_JPL;
		} else {
			if (v_COLOGNE == null) v_COLOGNE = CatalogRead.readCOLOGNEcatalog();
			v = v_COLOGNE;
		}

		String molecule = "";
		for (int i = 0; i < v.size(); i++)
		{
			String mol = v.get(i);
			int a = mol.indexOf(name);
			if (a >= 0)
			{
				molecule = mol;
				break;
			}
		}

		if (molecule.equals(""))
			throw new JPARSECException("molecule '"+name+"' not found.");

		return molecule;
	}

	/**
	 * Gets a string with the data of certain transition. If the molecule has
	 * more than {@linkplain #maxTransitions} transitions, and the required one is after that number,
	 * returning string will be an empty string.
	 *
	 * @param transition Transition name or frequency as given in the catalog.
	 * @param name Name of the molecule as given in the catalog.
	 * @param jpl_cat True for JPL catalog, false for COLOGNE.
	 * @return Line of the catalog with the transition information.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String getTransition(String transition, String name, boolean jpl_cat) throws JPARSECException
	{
		return CatalogRead.getTransition(transition, name, jpl_cat, 0, 0);
	}

	/**
	 * Gets a string with the data of certain transition. If the molecule has
	 * more than {@linkplain #maxTransitions} transitions, and the required one is after that number,
	 * returning string will be an empty string.
	 *
	 * @param transition Transition name or frequency as given in the catalog.
	 * @param name Name of the molecule as given in the catalog.
	 * @param jpl_cat True for JPL catalog, false for COLOGNE.
	 * @param limit_temp Temperature limit to search for transitions. Only
	 *        transitions with upper level energy below this value will be
	 *        returned. 0 will return all transitions.
	 * @param limit_rint Minimum value of the <i>rint</i> parameter of the transition, which
	 * is associated with the intensity of the line. Set to 0 to avoid this condition.
	 * @return Line of the catalog with the transition information.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String getTransition(String transition, String name, boolean jpl_cat,
			double limit_temp, double limit_rint) throws JPARSECException
	{
		ArrayList<String> v = new ArrayList<String>();

		String molecule = CatalogRead.getMoleculeFileName(CatalogRead.getMolecule(name, jpl_cat));
		Object o = DataBase.getData("transitionsJPLorCDSM", false);
		boolean read = false;
		if (o != null) {
			Object oo[] = (Object[])o;
			boolean jpl = (Boolean)oo[0];
			String mol = (String)oo[1];
			double lt = (Double)oo[2];
			double lr = (Double)oo[3];
			if (jpl == jpl_cat && mol.equals(molecule) && lt == limit_temp && lr == limit_rint) {
				v = new ArrayList<String>(Arrays.asList((String[])oo[4]));
				read = true;
			}
		}
		if (!read) {
			if (jpl_cat)
			{
				v = CatalogRead.readJPLtransitions(molecule, limit_temp, limit_rint);
			} else
			{
				v = CatalogRead.readCOLOGNEtransitions(molecule, limit_temp, limit_rint);
			}
			DataBase.addData("transitionsJPLorCDSM", new Object[] {jpl_cat, molecule, limit_temp, limit_rint, DataSet.arrayListToStringArray(v)}, false);
		}

		String out = "";
		for (int i = 0; i < v.size(); i++)
		{
			String tran = v.get(i);
			int a = tran.indexOf(transition);
			if (a >= 0)
			{
				out = tran;
				break;
			}
		}

		if (out.equals(""))
			throw new JPARSECException("transition '"+transition+"' not found.");

		return out;
	}

	/**
	 * Gets a string with the data of certain transitions. If the molecule has
	 * more than {@linkplain #maxTransitions} transitions, and the required one is after that number,
	 * returning string will be an empty string.
	 *
	 * @param transition Transition name or frequency as given in the catalog.
	 * @param name Name of the molecule as given in the catalog.
	 * @param jpl_cat True for JPL catalog, false for COLOGNE.
	 * @param width Width in MHz to return all transitions at frequency around (+/-) (width/2).
	 * @return The transitions found. The first one is the reference one.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String[] getTransitions(String transition, String name, boolean jpl_cat, double width) throws JPARSECException
	{
		return CatalogRead.getTransitions(transition, name, jpl_cat, width, 0, 0);
	}

	/**
	 * Gets a string with the data of certain transitions. If the molecule has
	 * more than {@linkplain #maxTransitions} transitions, and the required one is after that number,
	 * returning string will be an empty string.
	 *
	 * @param transition Transition name or frequency as given in the catalog.
	 * @param name Name of the molecule as given in the catalog.
	 * @param jpl_cat True for JPL catalog, false for COLOGNE.
	 * @param width Width in MHz to return all transitions at frequency around (+/-) (width/2).
	 * @param limit_temp Temperature limit to search for transitions. Only
	 *        transitions with upper level energy below this value will be
	 *        returned. 0 will return all transitions.
	 * @param limit_rint Minimum value of the <i>rint</i> parameter of the transition, which
	 * is associated with the intensity of the line. Set to 0 to avoid this condition.
	 * @return The transitions found. The first one is the reference one.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String[] getTransitions(String transition, String name, boolean jpl_cat, double width,
			double limit_temp, double limit_rint) throws JPARSECException
	{
		String tran0 = CatalogRead.getTransition(transition, name, jpl_cat, limit_temp, limit_rint);
		double frec0 = 1.0E+6 * DataSet.parseDouble(tran0.substring(0, 13).trim());

		ArrayList<String> v = new ArrayList<String>();

		String molecule = CatalogRead.getMoleculeFileName(CatalogRead.getMolecule(name, jpl_cat));
		Object o = DataBase.getData("transitionsJPLorCDSM", false);
		boolean read = false;
		if (o != null) {
			Object oo[] = (Object[])o;
			boolean jpl = (Boolean)oo[0];
			String mol = (String)oo[1];
			double lt = (Double)oo[2];
			double lr = (Double)oo[3];
			if (jpl == jpl_cat && mol.equals(molecule) && lt == limit_temp && lr == limit_rint) {
				v = new ArrayList<String>(Arrays.asList((String[])oo[4]));
				read = true;
			}
		}
		if (!read) {
			if (jpl_cat)
			{
				v = CatalogRead.readJPLtransitions(molecule, limit_temp, limit_rint);
			} else
			{
				v = CatalogRead.readCOLOGNEtransitions(molecule, limit_temp, limit_rint);
			}
			DataBase.addData("transitionsJPLorCDSM", new Object[] {jpl_cat, molecule, limit_temp, limit_rint, DataSet.arrayListToStringArray(v)}, false);
		}

		String out[] = new String[] {tran0};
		for (int i = 0; i < v.size(); i++)
		{
			String tran = v.get(i);

			double frec = 1.0E+6 * DataSet.parseDouble(tran.substring(0, 13).trim());

			if ( Math.abs(frec-frec0) <= (width*0.5*1.0E6) && !tran.equals(tran0))
			{
				out = DataSet.addStringArray(out, new String[] {tran});
			}
		}

		return out;
	}

	/**
	 * Gets a string with the data of certain transitions. If the molecule has
	 * more than {@linkplain #maxTransitions} transitions, and the required one is after that number,
	 * returning string will be an empty string.
	 *
	 * @param frec0 Transition frequency in MHz.
	 * @param name Name of the molecule as given in the catalog.
	 * @param jpl_cat True for JPL catalog, false for COLOGNE.
	 * @param width Width in MHz to return all transitions at frequency around (+/-) (width/2).
	 * @param limit_temp Temperature limit to search for transitions. Only
	 *        transitions with upper level energy below this value will be
	 *        returned. 0 will return all transitions.
	 * @param limit_rint Minimum value of the <i>rint</i> parameter of the transition, which
	 * is associated with the intensity of the line. Set to 0 to avoid this condition.
	 * @return The transitions found. The first one is the reference one.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String[] getTransitions(double frec0, String name, boolean jpl_cat, double width,
			double limit_temp, double limit_rint) throws JPARSECException
	{
		ArrayList<String> v = new ArrayList<String>();

		String molecule = CatalogRead.getMoleculeFileName(CatalogRead.getMolecule(name, jpl_cat));
		Object o = DataBase.getData("transitionsJPLorCDSM", false);
		boolean read = false;
		if (o != null) {
			Object oo[] = (Object[])o;
			boolean jpl = (Boolean)oo[0];
			String mol = (String)oo[1];
			double lt = (Double)oo[2];
			double lr = (Double)oo[3];
			if (jpl == jpl_cat && mol.equals(molecule) && lt == limit_temp && lr == limit_rint) {
				v = new ArrayList<String>(Arrays.asList((String[])oo[4]));
				read = true;
			}
		}
		if (!read) {
			if (jpl_cat)
			{
				v = CatalogRead.readJPLtransitions(molecule, limit_temp, limit_rint);
			} else
			{
				v = CatalogRead.readCOLOGNEtransitions(molecule, limit_temp, limit_rint);
			}
			DataBase.addData("transitionsJPLorCDSM", new Object[] {jpl_cat, molecule, limit_temp, limit_rint, DataSet.arrayListToStringArray(v)}, false);
		}

		if (frec0 == 0.0)
			return DataSet.arrayListToStringArray(v);

		String out[] = new String[] {};
		for (int i = 0; i < v.size(); i++)
		{
			String tran = v.get(i);

			double frec = DataSet.parseDouble(tran.substring(0, 13).trim());

			if ( Math.abs(frec-frec0) < width*0.5)
			{
				out = DataSet.addStringArray(out, new String[] {tran});
			}
		}

		return out;
	}

	/**
	 * Obtains the name of the molecule file in the catalog list of files,
	 * for JPL and COLOGNE catalogs.
	 *
	 * @param mol Name of the molecule.
	 * @return Name of the file containing information about this molecule.
	 */
	public static String getMoleculeFileName(String mol)
	{
		int lon;

		if (mol.length() > 6) mol = mol.substring(0, 6).trim();
		mol = FileIO.getField(1, mol, " ", true);
		mol = mol.trim();
		lon = 6 - mol.length();
		for (int i = 0; i < lon; i++)
		{
			mol = "0" + mol;
		}
		mol = "c" + mol + ".cat";

		return mol;
	}

	/** The indexes for the different fields of a given transition record in JPL/COLOGNE catalogs. */
	public static final int JPL_COLOGNE_FORMAT_FREQUENCY_INDEX = 0, JPL_COLOGNE_FORMAT_FREQUENCY_ERROR_INDEX = 1,
			JPL_COLOGNE_FORMAT_INTENSITY_INDEX = 2, JPL_COLOGNE_FORMAT_DEGREE_FREEDOM_INDEX = 3,
					JPL_COLOGNE_FORMAT_LOWER_STATE_ENERGY_INDEX = 4, JPL_COLOGNE_FORMAT_GU_INDEX = 5,
							JPL_COLOGNE_FORMAT_TAG_INDEX = 6, JPL_COLOGNE_FORMAT_QN_CODING_INDEX = 7,
									JPL_COLOGNE_FORMAT_QN_INDEX = 8;

	/** The identifiers for the different fields of a given transition record
	 * in both JPL and COLOGNE catalogs. */
	public static final String[] JPL_COLOGNE_FORMAT_FIELDS = new String[] {
		"FREQUENCY", "FREQUENCY_ERROR", "INTENSITY", "DEGREE_FREEDOM",
		"LOWER_STATE_ENERGY", "GU", "TAG", "QN_CODING", "QN"
	};

	/** The object the describes the different fields of a given transition record.
	 * The identifiers for each field are those from {@linkplain #JPL_COLOGNE_FORMAT_FIELDS}. */
	public static final FileFormatElement JPL_COLOGNE_FORMAT[] = new FileFormatElement[] {
			new FileFormatElement(1, 13, JPL_COLOGNE_FORMAT_FIELDS[0]),
			new FileFormatElement(16, 21, JPL_COLOGNE_FORMAT_FIELDS[1]),
			new FileFormatElement(23, 29, JPL_COLOGNE_FORMAT_FIELDS[2]),
			new FileFormatElement(31, 32, JPL_COLOGNE_FORMAT_FIELDS[3]),
			new FileFormatElement(33, 41, JPL_COLOGNE_FORMAT_FIELDS[4]),
			new FileFormatElement(42, 44, JPL_COLOGNE_FORMAT_FIELDS[5]),
			new FileFormatElement(46, 51, JPL_COLOGNE_FORMAT_FIELDS[6]),
			new FileFormatElement(53, 55, JPL_COLOGNE_FORMAT_FIELDS[7]),
			new FileFormatElement(56, 150, JPL_COLOGNE_FORMAT_FIELDS[8])
	};
}
