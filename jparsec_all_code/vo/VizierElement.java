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
package jparsec.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import jparsec.astronomy.PhotometricBandElement;
import jparsec.astronomy.Photometry;
import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.math.Constant;
import jparsec.math.Converter;
import jparsec.math.MeasureElement;
import jparsec.util.JPARSECException;

/**
 * A class to hold properties and data for Vizier catalogs.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class VizierElement implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Name of catalog.
	 */
	public String catalogName;
	/**
	 * Author of catalog.
	 */
	public String catalogAuthor;
	/**
	 * Catalog description.
	 */
	public String catalogDescription;
	/**
	 * Beam of instrument in arcseconds, that can be used to search around an object
	 * inside the beam.
	 */
	public float beam;
	/**
	 * Fields of the catalog formatted as identifier (for development purposes), a
	 * blank space separator, and the name of the field as Vizier gives it for a
	 * given catalog. First index is table index, since a given catalog could have
	 * several tables. Second is field index.
	 */
	public String[][] fields;
	/**
	 * Photometric bands used by the catalog. The name of the band should be set to
	 * the name of the field that holds the flux for this band. The corresponding flux
	 * error field name should be the same name as the flux plus the string "_ERROR".
	 */
	public PhotometricBandElement[] bands;
	/**
	 * Holds data retrieved from Vizier as a set of String arrays.
	 */
	public ArrayList<String[]> data;
	/**
	 * Fields of the catalog as Vizier gives them.
	 */
	public String[] dataFields;
	/**
	 * Hold the units for the fluxes.
	 */
	public String unit[];
	/**
	 * Holds links to be added to certain fields when representing a table.
	 * Format is the field name, a space, an the link structure. Any other field
	 * can be inserted in the structure with the field name between html tags.
	 */
	public String[] links;

	private int tableIndex = 0;

	/**
	 * Constructor.
	 * @param name Name of catalog.
	 * @param author Author of the catalog.
	 * @param description Description of catalog.
	 * @param beam Beam size.
	 * @param fields Fields.
	 * @param bands Bands.
	 */
	public VizierElement(String name, String author, String description, float beam, String[][] fields,
			PhotometricBandElement[] bands)
	{
		this.catalogName = name;
		this.catalogAuthor = author;
		this.catalogDescription = description;
		this.beam = beam;
		this.fields = fields;
		this.bands = bands;
		this.links = new String[] {};
	}

	/**
	 * Constructor with links.
	 * @param name Name of catalog.
	 * @param author Author of the catalog.
	 * @param description Description of catalog.
	 * @param beam Beam size.
	 * @param fields Fields.
	 * @param bands Bands.
	 * @param links Links in the adequate format.
	 */
	public VizierElement(String name, String author, String description, float beam, String[][] fields,
			PhotometricBandElement[] bands, String[] links)
	{
		this.catalogName = name;
		this.catalogAuthor = author;
		this.catalogDescription = description;
		this.beam = beam;
		this.fields = fields;
		this.bands = bands;
		this.links = links;
	}

	/**
	 * Default object for DENIS database.
	 */
	public static final VizierElement DENIS = new VizierElement(
			"B/denis", "Denis Consortium 2005",
			"The DENIS database (3rd Release)",
			2.0f,
			new String[][] {
					new String[] {
					"STRIP  Strip", "RAJ2000  RAJ2000", "DECJ2000  DEJ2000", "MAG_I  Imag", "MAG_I_ERROR  e_Imag",
					"MAG_J  Jmag", "MAG_J_ERROR  e_Jmag", "MAG_K  Kmag", "MAG_K_ERROR  e_Kmag",
					"MAG_R_A2  Rmag", "MAG_B_A2  Bmag"
					}
			},
			new PhotometricBandElement[] {
					PhotometricBandElement.BAND_I_DENIS.setBandAndFieldName("MAG_I", "DENIS I BAND").clone(),
					PhotometricBandElement.BAND_J_DENIS.setBandAndFieldName("MAG_J", "DENIS J BAND").clone(),
					PhotometricBandElement.BAND_Ks_DENIS.setBandAndFieldName("MAG_K", "DENIS Ks BAND").clone()
			}
	);

	/**
	 * Default object for 2MASS database.
	 */
	public static final VizierElement _2MASS = new VizierElement(
			"II/246", "Cutri et al. 2003",
			"The 2MASS Point Source Catalogue",
			2.0f,
			new String[][] {
					new String[] {
					"RAJ2000  RAJ2000", "DECJ2000  DEJ2000", "ID  2MASS", "MAG_J  Jmag",
					"MAG_J_ERROR  e_Jmag",
					"MAG_H  Hmag", "MAG_H_ERROR  e_Hmag", "MAG_K  Kmag", "MAG_K_ERROR  e_Kmag",
					"QUALITY  Qflg"
					}
			},
			new PhotometricBandElement[] {
					PhotometricBandElement.BAND_J_2MASS.setBandAndFieldName("MAG_J", "2MASS J BAND").clone(),
					PhotometricBandElement.BAND_H_2MASS.setBandAndFieldName("MAG_H", "2MASS H BAND").clone(),
					PhotometricBandElement.BAND_Ks_2MASS.setBandAndFieldName("MAG_K", "2MASS Ks BAND").clone()
			}
	);

	/**
	 * Default object for USNO-B1 database.
	 */
	public static final VizierElement USNO_B1 = new VizierElement(
			"I/284", "Monet+ 2003",
			"The USNO-B1.0 Catalog" //+ FileIO.getLineSeparator() + "The Whole-Sky USNO-B1.0 Catalog of 1,045,913,669 sources"
			,
			2.0f,
			new String[][] {
					new String[] {
					"NAME  USNO-B1.0", "RAJ2000  RAJ2000", "DECJ2000  DEJ2000",
					"RA_ERROR  e_RAdeg", "DEC_ERROR  e_DEdeg",
					"EPOCH  Epoch", "PM_RA  pmRA", "PM_DEC  pmDE", "N_DET  Ndet",
					"MAG_B1  B1mag", "MAG_R1  R1mag",
					"MAG_B2  B2mag", "MAG_R2  R2mag", "MAG_I  Imag"
					}
			},
			new PhotometricBandElement[] {
					PhotometricBandElement.BAND_B_JOHNSON_MORGAN.setBandAndFieldName("MAG_B", "USNO B BAND").clone(),
					PhotometricBandElement.BAND_R_JOHNSON_MORGAN.setBandAndFieldName("MAG_R", "USNO R BAND").clone(),
					PhotometricBandElement.BAND_I_JOHNSON_MORGAN.setBandAndFieldName("MAG_I", "USNO I BAND").clone()
			}
	);

	/**
	 * Default object for MSX6C database.
	 */
	public static final VizierElement MSX6C = new VizierElement(
			"V/114", "Egan et al. 2003",
			"MSX6C Infrared Point Source Catalog" //+ FileIO.getLineSeparator() + "The complete MSX6C catalogue in the Galactic Plane (|b|<=6\u00b0)"
			,
			36.0f,
			new String[][] {
					new String[] {
					"NAME  MSX6C", "RAJ2000  RAJ2000", "DECJ2000  DEJ2000",
					"POS_ERROR  ePos",
					"MAG_B1  B1", "QUALITY_B1  q_B1", "MAG_B2  B2", "QUALITY_B2  q_B2",
					"MAG_A  A", "QUALITY_A  q_A", "MAG_C  C", "QUALITY_C  q_C",
					"MAG_D  D", "QUALITY_D  q_D", "MAG_E  E", "QUALITY_E  q_E"
					}
			},
			new PhotometricBandElement[] {
					PhotometricBandElement.BAND_B1_MSX6C.setBandAndFieldName("MAG_B1", "MSX6C B1 BAND").clone(),
					PhotometricBandElement.BAND_B2_MSX6C.setBandAndFieldName("MAG_B2", "MSX6C B2 BAND").clone(),
					PhotometricBandElement.BAND_A_MSX6C.setBandAndFieldName("MAG_A", "MSX6C A BAND").clone(),
					PhotometricBandElement.BAND_C_MSX6C.setBandAndFieldName("MAG_C", "MSX6C C BAND").clone(),
					PhotometricBandElement.BAND_D_MSX6C.setBandAndFieldName("MAG_D", "MSX6C D BAND").clone(),
					PhotometricBandElement.BAND_E_MSX6C.setBandAndFieldName("MAG_E", "MSX6C E BAND").clone()
			}
	);

	/**
	 * Default object for IRAS database.
	 */
	public static final VizierElement IRAS = new VizierElement(
			"II/125", "IPAC 1986",
			"IRAS catalogue of Point Sources (Version 2.0)",
			70.0f,
			new String[][] {
					new String[] {
					"NAME  IRAS", "RA1950  RA1950", "DEC1950  DE1950", "FLUX_12  Fnu_12",
					"FLUX_25  Fnu_25", "FLUX_60  Fnu_60", "FLUX_100  Fnu_100",
					"QUALITY_12  q_Fnu_12",
					"QUALITY_25  q_Fnu_25", "QUALITY_60  q_Fnu_60",
					"QUALITY_100  q_Fnu_100",
					}
			},
			new PhotometricBandElement[] {
					PhotometricBandElement.BAND_12_IRAS.setBandAndFieldName("FLUX_12", "IRAS 12 MICRONS").clone(),
					PhotometricBandElement.BAND_25_IRAS.setBandAndFieldName("FLUX_25", "IRAS 25 MICRONS").clone(),
					PhotometricBandElement.BAND_60_IRAS.setBandAndFieldName("FLUX_60", "IRAS 60 MICRONS").clone(),
					PhotometricBandElement.BAND_100_IRAS.setBandAndFieldName("FLUX_100", "IRAS 100 MICRONS").clone()
			}
	);

	/**
	 * Default object for RADIO database from Altenhoff et al. 1994.
	 */
	public static final VizierElement RADIO = new VizierElement(
			"J/A+A/281/161", "Altenhoff et al. 1994",
			"Radio emission from stars at 250GHz",
			10.0f,
			new String[][] {
					new String[] {
					"TYPE  St", "NAME  Name", "SPECTRAL_TYPE  SpType", "LIMIT_FLAT_S  l_S(250)",
					"S250  S(250)", "S250_ERROR  e_S(250)", "LIMIT_FLAG_SPECTRAL_INDEX  l_alf",
					"SPECTRAL_INDEX  alf",
					"RMS_SPECTRAL_INDEX  e_alf", "UNCERTAINTY_ALF  u_alf",
					"REMARKS  Rem", "SIMBAD  SimbadName", "RAJ2000  _RA", " DECJ2000  _DE"
					}
			},
			new PhotometricBandElement[] {
					PhotometricBandElement.BAND_250GHz_30m.setBandAndFieldName("S250", "Radio emision from stars at 250GHz (30m)").clone()
			}
	);

	/**
	 * Default object for UV database from Thompson et al. 1978.
	 */
	public static final VizierElement UV = new VizierElement(
			"II/59B", "Thompson et al. 1978",
			"Catalogue of stellar UV fluxes (TD1)",
			2.0f,
			new String[][] {
					new String[] {
					"ID  TD1", "NAME_HD  HD", "MAG_V  Vmag", "SpType  SpType",
					"RA1950  RA1950", "DEC1950  DE1950", "F2740  F2740",
					"F2365  F2365",
					"F1965  F1965", "F1565  F1565",
					"REMARKS  Rem", "SIMBAD  SimbadName", "RAJ2000  _RA", " DECJ2000  _DE"
					}
			},
			new PhotometricBandElement[] {
					PhotometricBandElement.BAND_V_JOHNSON_MORGAN.setBandAndFieldName("MAG_V", "Catalogue of stellar UV fluxes").clone(),
					PhotometricBandElement.BAND_F2740_UV.setBandAndFieldName("F2740", "Catalogue of stellar UV fluxes").clone(),
					PhotometricBandElement.BAND_F2365_UV.setBandAndFieldName("F2365", "Catalogue of stellar UV fluxes").clone(),
					PhotometricBandElement.BAND_F1965_UV.setBandAndFieldName("F1965", "Catalogue of stellar UV fluxes").clone(),
					PhotometricBandElement.BAND_F1565_UV.setBandAndFieldName("F1565", "Catalogue of stellar UV fluxes").clone()
			}
	);

	/**
	 * Default object for UBVRIJKLMNH Photoelectric Catalog.
	 */
	public static final VizierElement UBVRIJKLMNH = new VizierElement(
			"II/7A", "Morel et al. 1978",
			"UBVRIJKLMNH Photoelectric Catalogue",
			2.0f,
			new String[][] {
					new String[] {
					"NAME  LID", "SUFFIX  n_LID", "MAG_U  U", "MAG_B  B",
					"MAG_V  V", "MAG_R  R",
					"MAG_I  I", "MAG_J  J", "MAG_K  K", "MAG_L  L", "MAG_M  M",
					"MAG_N  N",	"MAG_H  H", "REF  ref", "RAJ2000  _RA", "DECJ2000 _DE"
					}
			},
			new PhotometricBandElement[] {
					PhotometricBandElement.BAND_U_JOHNSON_MORGAN.setBandAndFieldName("MAG_U", "UBVRIJKLMNH U BAND").clone(),
					PhotometricBandElement.BAND_B_JOHNSON_MORGAN.setBandAndFieldName("MAG_B", "UBVRIJKLMNH B BAND").clone(),
					PhotometricBandElement.BAND_V_JOHNSON_MORGAN.setBandAndFieldName("MAG_V", "UBVRIJKLMNH V BAND").clone(),
					PhotometricBandElement.BAND_R_JOHNSON_MORGAN.setBandAndFieldName("MAG_R", "UBVRIJKLMNH R BAND").clone(),
					PhotometricBandElement.BAND_I_JOHNSON_MORGAN.setBandAndFieldName("MAG_I", "UBVRIJKLMNH I BAND").clone(),
					PhotometricBandElement.BAND_J_JOHNSON_MORGAN.setBandAndFieldName("MAG_J", "UBVRIJKLMNH J BAND").clone(),
					PhotometricBandElement.BAND_K_JOHNSON_MORGAN.setBandAndFieldName("MAG_K", "UBVRIJKLMNH K BAND").clone(),
					PhotometricBandElement.BAND_L_JOHNSON_MORGAN.setBandAndFieldName("MAG_L", "UBVRIJKLMNH L BAND").clone(),
					PhotometricBandElement.BAND_M_JOHNSON_MORGAN.setBandAndFieldName("MAG_M", "UBVRIJKLMNH M BAND").clone(),
					PhotometricBandElement.BAND_N_JOHNSON_MORGAN.setBandAndFieldName("MAG_N", "UBVRIJKLMNH N BAND").clone(),
					PhotometricBandElement.BAND_H_JOHNSON_MORGAN.setBandAndFieldName("MAG_H", "UBVRIJKLMNH H BAND").clone()
			}
	);

	/**
	 * Default object for TYCHO-2 database.
	 */
	public static final VizierElement TYCHO2 = new VizierElement(
			"I/259", "Hog et al. 2000",
			"The Tycho-2 main catalogue",
			2.0f,
			new String[][] {
					new String[] {
					"NAME1  TYC1", "NAME2  TYC2", "NAME3  TYC3", "PM_RA  pmRA", "PM_DEC  pmDE", "MAG_BT  BTmag",
					"MAG_VT  VTmag", "HIPPARCOS  HIP", "RA_ICRS  RA(ICRS)", "DEC_ICRS  DE(ICRS)"
					}
			},
			new PhotometricBandElement[] {
					PhotometricBandElement.BAND_B_JOHNSON_MORGAN.setBandAndFieldName("MAG_BT", "TYCHO-2 B BAND").clone(),
					PhotometricBandElement.BAND_V_JOHNSON_MORGAN.setBandAndFieldName("MAG_VT", "TYCHO-2 V BAND").clone()
			}
	);

	/**
	 * Default object for Kharchenko database.
	 */
	public static final VizierElement KHARCHENKO = new VizierElement(
			"I/280A", "Kharchenko 2001",
			"The all-sky catalogue of 2.5 million stars",
			2.0f,
			new String[][] {
					new String[] {
					"RAJ2000  RAJ2000", "DECJ2000  DEJ2000", "PARALLAX  Plx", "P_ERROR  e_Plx", "PM_RA  pmRA", "PM_DEC  pmDE", "MAG_B  Bmag",
					"MAG_V  Vmag", "SP_TYPE  SpType"
					}
			},
			new PhotometricBandElement[] {
					PhotometricBandElement.BAND_B_JOHNSON_MORGAN.setBandAndFieldName("MAG_B", "I/280A B BAND").clone(),
					PhotometricBandElement.BAND_V_JOHNSON_MORGAN.setBandAndFieldName("MAG_V", "I/280A V BAND").clone()
			}
	);

	/**
	 * Default object for PdBI database.
	 */
	public static final VizierElement PdBI = new VizierElement(
			"B/iram", "Dan, Neri 2006",
			"Plateau de Bure Interferometer Observation Log  (IRAM 1991-2006)",
			2.0f,
			new String[][] {
					new String[] {
					"PROGRAM_CODE  Prog", "SOURCE  Source", "DATE_INI  Obs",
					"INT_TIME  tos", "PROGRAM_TYPE  Type", "VEL  Vel",
					"LSR_VEL  n_Vel", "FREQ_3MM  F3mm", "N_FREQ_3MM  n_F3mm",
					"FREQ_1MM  F1mm", "N_FREQ_1MM  n_F1mm", "CONF  Conf",
					"RAJ2000  RAJ2000", "DECJ2000  DEJ2000"
					},
					new String[] {
						"SOURCE  Name", "DATE_INI  Obs", "OBS_MODE  obsMode", "SCAN  scan", "TAU  tau",
						"RECEIVER  receiv", "LINE  line", "FREQ  Freq", "RAJ2000  RAJ2000", "DECJ2000  DEJ2000"
					}
			},
			new PhotometricBandElement[] {
			}
	);

	/**
	 * Default object for ISO database.
	 */
	public static final VizierElement ISO = new VizierElement(
			"VI/111", "ISO data center 2004",
			"The ISO Observation Log",
			40.0f,
			new String[][] {
					new String[] {
					"SOURCE  Target", "AOT  AOT", "TDT  TDT", "RAJ2000  RAJ2000", "DECJ2000  DEJ2000", "ABSTRACT  abs",
					"INT_TIME  oLen"
					}
			},
			new PhotometricBandElement[] {
			}
	);

	/**
	 * Default object for IUE atlas of pre-main sequence stars database.
	 */
	public static final VizierElement IUE_PRE_MAIN = new VizierElement(
			"J/ApJS/147/305", "Valenti et al. 2003",
			"Pre-main-sequence stars observed by IUE with LW cameras",
			15.0f, // Doubts/problems
			new String[][] {
					new String[] {
					"ID_NUMBER  Seq", "CODE_ERROR  u_Seq", "ID_ALTERNATIVE  HBC",
					"SOURCE  Name", "OTHER NAME  OName", "SPECTRAL_TYPE  SpType",
					"TYPE  Cat", "NOTE  n_Cat", "NUMBER_OF_OBSERVATIONS  N",
					"CODE_MISC  sp", "PLOT_DATA_LINK  Plot", "SIMBAD_DATA_LINK  Simbad",
					"RAJ2000  _RA", "DECJ2000  _DE"
					}
			},
			new PhotometricBandElement[] {
			},
			new String[] {
					"PLOT_DATA_LINK  http://cdsarc.u-strasbg.fr/viz-bin/vizExec/Vgraph?J/ApJS/147/305/<SOURCE>",
					"SIMBAD_DATA_LINK  http://simbad.u-strasbg.fr/simbad/sim-id?Ident=<SOURCE>%20"
			}
	);

	/**
	 * Default object for Wendker catalog of radio continuum emission from stars.
	 */
	public static final VizierElement Wendker = new VizierElement(
			"II/199A", "Wendker 1995",
			"Radio continuum emission from stars",
			1.0f,
			new String[][] {
					new String[] {
					"ID  Seq", "NAME  Name", "ALIAS  Alias",
					"DETECTED  Det", "RA1950  RA1950", "DEC1950  DE1950",
					"DATA_LINK  Data"
					}
			},
			new PhotometricBandElement[] {
			},
			new String[] {
					"DATA_LINK  http://cdsarc.u-strasbg.fr/viz-bin/vizExec/getstar?II/199A&<ID>"
			}
	);

	/**
	 * Default object for IUE database.
	 */
	public static final VizierElement IUE = new VizierElement(
			"VI/110", "NASA-ESA 2000",
			"The INES (IUE Newly Extracted Spectra)",
			15.0f,
			new String[][] {
					new String[] {
					"DATE_INI  Obs", "INT_TIME  ExpTime", "SOURCE  Object",
					"RAJ2000  RA2000", "DECJ2000  DE2000",
					"CLASS  IUEClass", "SPECTRUM_LINK  Spectrum"
					}
			},
			new PhotometricBandElement[] {
			},
			new String[] {
					"SPECTRUM_LINK  http://sdc.laeff.inta.es/cgi-ines/IUEdbsMY?rd_mm=0&rd_ss=<SEARCH_RADIUS>&object=<SOURCE>"
			}
	);

	/**
	 * Default object for VLA 1.4 GHz sky survey database.
	 */
	public static final VizierElement VLA = new VizierElement(
			"VIII/65", "Condon et al. 1998",
			"1.4GHz NRAO VLA Sky Survey (NVSS)",
			15.0f,
			new String[][] {
					new String[] {
					"SOURCE  NVSS", "RAJ2000  RAJ2000", "DECJ2000  DEJ2000", "RA_ERR  e_RAJ2000", "DEC_ERR  e_DEJ2000",
					"S1_4  S1.4", "S1_4_ERROR  e_S1.4", "MAJOR_AXIS_LIMIT  I_MajAxis", "MAJOR_AXIS  MajAxis", "MINOR_AXIS_LIMIT  I_MinAxis",
					"MINOR_AXIS  MinAxis", "RESIDUAL_CODE  f_resFlux"
					}
			},
			new PhotometricBandElement[] {
					PhotometricBandElement.BAND_1p4GHz_VLA.setBandAndFieldName("S1_4", "NVSS (VLA) 1.4GHz").clone()
			},
			new String[] {
					"IMAGE_LINK   http://www.cv.nrao.edu/cgi-bin/postage.pl?RA=<RAJ2000>&Dec=<DECJ2000>&Cells=5.0%205.0&Type=image/x-fits"
			}
	);

	/**
	 * Default object for WISE sky survey database.
	 */
	public static final VizierElement WISE = new VizierElement(
			"II/328", "Cutri et al. 2013",
			"AllWISE Data Release",
			12.0f,
			new String[][] {
					new String[] {
					"SOURCE  AllWISE", "RAJ2000  RAJ2000", "DECJ2000  DEJ2000",
					"W1  W1mag", "W1_ERROR  e_W1mag",
					"W2  W2mag", "W2_ERROR  e_W2mag",
					"W3  W3mag", "W3_ERROR  e_W3mag",
					"W4  W4mag", "W4_ERROR  e_W4mag",
					}
			},
			new PhotometricBandElement[] {
					PhotometricBandElement.BAND_W1_WISE.setBandAndFieldName("W1", "WISE 3.4um").clone(),
					PhotometricBandElement.BAND_W2_WISE.setBandAndFieldName("W2", "WISE 4.6um").clone(),
					PhotometricBandElement.BAND_W3_WISE.setBandAndFieldName("W3", "WISE 12um").clone(),
					PhotometricBandElement.BAND_W4_WISE.setBandAndFieldName("W4", "WISE 22um").clone()
			}
	);

	/**
	 * Default object for AKARI sky survey database.
	 */
	public static final VizierElement AKARI = new VizierElement(
			"II/298", "ISAS/JAXA, 2010",
			"AKARI/FIS All-Sky Survey Point Source Catalogues",
			8.0f,
			new String[][] {
					new String[] {
					"SOURCE  objName", "RAJ2000  RAJ2000", "DECJ2000  DEJ2000",
					"S65  S65", "S65_ERROR  e_S65",
					"S90  S90", "S90_ERROR  e_S90",
					"S140  S140", "S140_ERROR  e_S140",
					"S160  S160", "S160_ERROR  e_S160",
					}
			},
			new PhotometricBandElement[] {
					PhotometricBandElement.BAND_S65_AKARI.setBandAndFieldName("S65", "AKARI S65").clone(),
					PhotometricBandElement.BAND_S90_AKARI.setBandAndFieldName("S90", "AKARI S90").clone(),
					PhotometricBandElement.BAND_S140_AKARI.setBandAndFieldName("S140", "AKARI S140").clone(),
					PhotometricBandElement.BAND_S160_AKARI.setBandAndFieldName("S160", "AKARI S160").clone()
			}
	);

	/**
	 * Default object for UCAC4 catalog.
	 */
	public static final VizierElement UCAC4 = new VizierElement(
			"I/322A", "Zacharias N. et al. 2012",
			"The fourth U.S. Naval Observatory CCD Astrograph Catalog (UCAC4)",
			1.0f,
			new String[][] {
					new String[] {
					"NAME1  UCAC4", "PM_RA  pmRA", "PM_DEC  pmDE", "MAG_BT  Bmag",
					"MAG_VT  Vmag", "RA_ICRS  RAdeg", "DEC_ICRS  DEdeg"
					}
			},
			new PhotometricBandElement[] {
					PhotometricBandElement.BAND_B_JOHNSON_MORGAN.setBandAndFieldName("MAG_BT", "UCAC4 B BAND").clone(),
					PhotometricBandElement.BAND_V_JOHNSON_MORGAN.setBandAndFieldName("MAG_VT", "UCAC4 V BAND").clone()
			}
	);
	
	private static final VizierElement[] ALL = new VizierElement[] {
		DENIS,
		_2MASS,
		USNO_B1,
		MSX6C,
		UBVRIJKLMNH,
		IRAS,
		TYCHO2,
		RADIO,
		//UV,
		PdBI, ISO, IUE, IUE_PRE_MAIN, VLA, Wendker, KHARCHENKO,
		WISE, AKARI, UCAC4
	};

	/**
	 * Gets one of the predefined Vizier objects from its name.
	 * @param name Name. If the name cannot be found, the search will be extended
	 * to the author and description.
	 * @return Vizier element.
	 */
	public static VizierElement getVizierElement (String name)
	{
		VizierElement elem = null;
		if (name == null || name.equals("")) return elem;

		for (int i=0; i<ALL.length; i++)
		{
			if (ALL[i].catalogName.toLowerCase().equals(name.toLowerCase())) {
				elem = ALL[i];
				break;
			}
		}
		return elem;
	}

	/**
	 * Checks if certain {@linkplain PhotometricBandElement} is present in this instance.
	 * @param band A band element.
	 * @return True or false.
	 */
	public boolean photometricBandBelongsToThisVizierElement(PhotometricBandElement band)
	{
		boolean belongs = false;
		for (int i=0; i<this.bands.length; i++)
		{
			if (this.bands[i].equals(band)) belongs = true;
		}
		return belongs;
	}

	/**
	 * Obtains the fluxes for a given Vizier element. The class Spectrum in the 
	 * astrophysics package accepts a table like this to construct a Spectrum object, using 
	 * cm for x axis (column 1) and mJy for y axis (column 2 and 3 for the errors).
	 * @param bandName Band name to get the fluxes from. Can be null to
	 * retrieve all fluxes available.
	 * @param allFluxes Defines what fluxes to get, all (true) or the first found (false).
	 * @param use_ESA_method Method to apply for Tycho catalog (ESA or Kidger).
	 * @return Array of Strings with wavelength in cm, flux in mJy, flux error,
	 * the band name and the band beam, separated by blank spaces.
	 * @throws JPARSECException If an error occurs.
	 */
	public String[] getFluxes(String bandName, boolean allFluxes, boolean use_ESA_method)
	throws JPARSECException {
		if (bandName == null) bandName = "";
		bandName = bandName.trim();

		ArrayList<String> out = new ArrayList<String>();
		for (int i=0; i<this.bands.length; i++)
		{
			if (bandName.equals("") || (!bandName.equals("") && bandName.indexOf(this.bands[i].fieldName) >= 0))
			{
				int nData = 1;
				if (allFluxes) nData = this.data.size();

				ArrayList<String> pond = new ArrayList<String>();
				for (int j=0; j<nData; j++)
				{
					String f[] = this.getFluxAndError(i, j);
					if (f != null) {
						String flux = f[0], fluxError = f[1];
						flux = flux.trim();
						if (!flux.equals("")) {
							MeasureElement fluxAndError = Photometry.getFluxFromMagnitude(
									DataSet.parseDouble(flux),
									DataSet.parseDouble(fluxError),
									this.bands[i]);

							String adding = "   "+this.bands[i].name+"   "+this.bands[i].beam;
							if (fluxAndError.getValue() > 0.0) {
								pond.add(""+this.bands[i].effectiveWavelength*Constant.MICRON_TO_CM+"   "+(fluxAndError.getValue()*1.0E3)+"   "+(fluxAndError.error*1.0E3)+adding);

								out.add(""+this.bands[i].effectiveWavelength*Constant.MICRON_TO_CM+"   "+(fluxAndError.getValue()*1.0E3)+"   "+(fluxAndError.error*1.0E3)+adding);
							}
						}
					}
				}
			}
		}

		if (VizierElement.TYCHO2.equalsExceptForTheData(this) &&
				this.data.size() == 1 && out.size() == 2) {
			String fB[] = this.getFluxAndError(0, 0);
			String fV[] = this.getFluxAndError(1, 0);

			if (fB != null && fV != null) {
				double mB = DataSet.parseDouble(fB[0]);
				double mV = DataSet.parseDouble(fV[0]);
				double newM[] = Photometry.getApproximateJohnsonBVFromTycho(mB, mV, use_ESA_method);
				MeasureElement fluxAndErrorB = Photometry.getFluxFromMagnitude(
						newM[0],
						DataSet.parseDouble(fB[1]),
						this.bands[0]);
				MeasureElement fluxAndErrorV = Photometry.getFluxFromMagnitude(
						newM[1],
						DataSet.parseDouble(fV[1]),
						this.bands[1]);

				String lB = out.get(0);
				String lV = out.get(1);
				String newlB = FileIO.getField(1, lB, " ", true) + "   "+(fluxAndErrorB.getValue()*1.0E3) + "   "+(fluxAndErrorB.error*1.0E3);
				String newlV = FileIO.getField(1, lV, " ", true) + "   "+(fluxAndErrorV.getValue()*1.0E3) + "   "+(fluxAndErrorV.error*1.0E3);
				newlB = newlB + "   " + FileIO.getRestAfterField(3, lB, " ", true);
				newlV = newlV + "   " + FileIO.getRestAfterField(3, lV, " ", true);
				out = new ArrayList<String>();
				out.add(newlB);
				out.add(newlV);
			} else {
				JPARSECException.addWarning("cannot correct Tycho-2 magnitudes to Johnson system.");
			}
		} else {
			if (VizierElement.TYCHO2.equalsExceptForTheData(this))
				JPARSECException.addWarning("cannot correct Tycho-2 magnitudes to Johnson system.");
		}

		if (this.bands.length == 0 && VizierElement.IUE_PRE_MAIN.equalsExceptForTheData(this) &&
				this.data.size() == 1) {
			try {
				String sn = this.data.get(0)[3];
				sn = DataSet.replaceAll(sn, " ", "", false);
				ArrayList<String> f = ReadFile.readResource(FileIO.DATA_IUE_DIRECTORY+"h_"+sn+"IUE_spc.txt");
				String file[] = DataSet.arrayListToStringArray(f);
				for (int i=0; i<file.length; i++)
				{
					String line = file[i].trim();
					if (!line.startsWith("#") && !line.equals("") && i % 4 == 0) {
						double w = DataSet.parseDouble(FileIO.getField(1, line, " ", true));
						double fl = DataSet.parseDouble(FileIO.getField(2, line, " ", true));
						fl = 1.0E23 * fl * w / (jparsec.math.CGSConstant.SPEED_OF_LIGHT / (w * 1.0E-8)); // to Jy
						w = w * 1.0E-8; // to cm
						String adding = "   IUE   "+this.beam;
						if (fl > 0.01) out.add(""+w+"   "+(fl*1.0E3)+"   "+(0*1.0E3)+adding); // flux must be > 10 mJy
					}
				}
			} catch (Exception exc) {
				exc.printStackTrace();
			}
		}

		if (this.bands.length == 0 && VizierElement.Wendker.equalsExceptForTheData(this) &&
				this.data.size() == 1) {
			try {
				String sn = this.data.get(0)[0].trim();
				sn = FileIO.addSpacesBeforeAString(sn, 8);
				String newS = "-------------------------------------------------------------------------------------";
				sn = newS+sn;
				ArrayList<String> f = ReadFile.readResource(FileIO.DATA_Wendker_DIRECTORY+"catalog");
				ArrayList<String> fr = ReadFile.readResource(FileIO.DATA_Wendker_DIRECTORY+"refs");
				String refer[] = DataSet.arrayListToStringArray(fr);
				boolean found = false, end = false, start = false;
				for (int i=0; i<f.size(); i++)
				{
					String line = (f.get(i)).trim();
					if (line.startsWith(sn)) found = true;
					if (line.startsWith(newS) && found && start) end = true;
					if (start && !end) {
						double w = jparsec.math.CGSConstant.SPEED_OF_LIGHT / (DataSet.parseDouble(FileIO.getField(1, line, "  ", true)) * 1.0E6);
						String fl1 = FileIO.getField(2, line, "  ", true);
						String fl2 = FileIO.getField(3, line, "  ", true);
						int po = line.indexOf("(");
						int pc = line.indexOf(")");
						String ref = line.substring(po+1, pc);
						ref = DataSet.replaceAll(ref, " ", "", false);
						int index = DataSet.getIndexStartingWith(refer, FileIO.addSpacesBeforeAString(ref, 5));
						String fullRef = "";
						if (index > 0) {
							fullRef = "="+FileIO.getRestAfterField(1, refer[index].trim(), " ", true);
							fullRef = DataSet.replaceAll(fullRef, "  ", " ", false);
						}
						String adding = "   Radiocontinuum Wendker 1995 (#"+ref+fullRef+")   -";
						double f1 = DataSet.getDoubleValueWithoutLimit(fl1);
						double f2 = DataSet.getDoubleValueWithoutLimit(fl2);
						if (f1 > 0) out.add(""+w+"   "+(fl1)+"   "+(0*1.0E3)+adding); // flux must be > 10 mJy
						if (f2 > 0) out.add(""+w+"   "+(fl2)+"   "+(0*1.0E3)+adding); // flux must be > 10 mJy
					}
					if (line.startsWith("***") && found) start = true;
				}
			} catch (Exception exc) {
				exc.printStackTrace();
			}
		}

		return DataSet.arrayListToStringArray(out);
	}

	/**
	 * Obtains the index of a given field in the Vizier catalogue.
	 * @param fieldName Field name as stablished in the JPARSEC convention.
	 * @return Field index in the Vizier catalogue.
	 */
	public int getVizierFieldIndex(String fieldName)
	{
		int nField = this.getFieldPosition(fieldName);
		if (nField < 0) return nField;
		String vizierFieldName = FileIO.getField(2, this.fields[tableIndex][nField], " ", true);
		int vizierFieldIndex = DataSet.getIndex(this.dataFields, vizierFieldName);
		return vizierFieldIndex;
	}

	private String[] getFluxAndError(int i, int j)
	throws JPARSECException {
		int vizierFieldIndex = this.getVizierFieldIndex(this.bands[i].fieldName);
		int vizierFieldErrorIndex = this.getVizierFieldIndex(this.bands[i].fieldName+"_ERROR");

		if (vizierFieldIndex == -1) {
			return null;
		}

		String[] record = this.data.get(j);
		String flux = record[vizierFieldIndex];
		String fluxError = "";
		if (vizierFieldErrorIndex >= 0) fluxError = record[vizierFieldErrorIndex];

		if (flux.indexOf("   ") >= 0) {
			fluxError = FileIO.getField(2, flux, " ", true);
			flux = FileIO.getField(1, flux, " ", true);
		}

		flux = flux.trim();
		fluxError = fluxError.trim();
		if (flux.equals("")) return null;
		if (fluxError.equals("")) fluxError = "0";

		double fluxValue = DataSet.parseDouble(flux);
		if (fluxValue > 30.0 && this.bands[i].fluxGivenAsMagnitude) {
			return null;
		}
		if (fluxValue <= 0.0 && !this.bands[i].fluxGivenAsMagnitude) {
			return null;
		}

		// Check flux units
		if (!this.bands[i].fluxGivenAsMagnitude) {
			if (!this.unit[i].toLowerCase().equals("jy")) {
				double fluxValueError = DataSet.parseDouble(fluxError);
				 try {
				 		Converter c = new Converter(this.unit[vizierFieldIndex], "Jy");
				 		fluxValue = c.convert(fluxValue);
				 		fluxValueError = c.convert(fluxValueError);
				 		flux = ""+fluxValue;
				 		fluxError = ""+fluxValueError;
				 } catch (JPARSECException e)
				 {
				 		e.showException();
				 }
			}
		}

		return new String[] {flux, fluxError};
	}

	/**
	 * Returns field position from a name.
	 * @param fieldName Field name.
	 * @return Position in {@linkplain VizierElement#fields}
	 * array, or -1 if it is not found.
	 */
	public int getFieldPosition(String fieldName)
	{
		int out = -1;
		for (int i=0; i<this.fields[tableIndex].length; i++)
		{
			if (FileIO.getField(1, this.fields[tableIndex][i], " ", true).indexOf(fieldName) >= 0) {
				out = i;
				break;
			}
		}
		return out;
	}


	/**
	 * Checks if this element is equal to another one.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof VizierElement)) return false;

		VizierElement that = (VizierElement) o;

		if (Float.compare(that.beam, beam) != 0) return false;
		if (tableIndex != that.tableIndex) return false;
		if (catalogName != null ? !catalogName.equals(that.catalogName) : that.catalogName != null) return false;
		if (catalogAuthor != null ? !catalogAuthor.equals(that.catalogAuthor) : that.catalogAuthor != null)
			return false;
		if (catalogDescription != null ? !catalogDescription.equals(that.catalogDescription) : that.catalogDescription != null)
			return false;
		if (!Arrays.deepEquals(fields, that.fields)) return false;
		if (!Arrays.equals(bands, that.bands)) return false;
		if (data != null ? !data.equals(that.data) : that.data != null) return false;
		if (!Arrays.equals(dataFields, that.dataFields)) return false;
		if (!Arrays.equals(unit, that.unit)) return false;

		return Arrays.equals(links, that.links);
	}

	@Override
	public int hashCode() {
		int result = catalogName != null ? catalogName.hashCode() : 0;
		result = 31 * result + (catalogAuthor != null ? catalogAuthor.hashCode() : 0);
		result = 31 * result + (catalogDescription != null ? catalogDescription.hashCode() : 0);
		result = 31 * result + (beam != +0.0f ? Float.floatToIntBits(beam) : 0);
		result = 31 * result + (fields != null ? Arrays.deepHashCode(fields) : 0);
		result = 31 * result + (bands != null ? Arrays.hashCode(bands) : 0);
		result = 31 * result + (data != null ? data.hashCode() : 0);
		result = 31 * result + (dataFields != null ? Arrays.hashCode(dataFields) : 0);
		result = 31 * result + (unit != null ? Arrays.hashCode(unit) : 0);
		result = 31 * result + (links != null ? Arrays.hashCode(links) : 0);
		result = 31 * result + tableIndex;
		return result;
	}

	/**
	 * Checks if this element is equal to another one except for the possible
	 * data vector.
	 * @param v Object to compare with.
	 * @return True or false.
	 */
	public boolean equalsExceptForTheData(VizierElement v)
	{
		if (v == null) return false;

		if (v.beam != this.beam) return false;
		if (!v.catalogAuthor.equals(this.catalogAuthor)) return false;
		if (!v.catalogDescription.equals(this.catalogDescription)) return false;
		if (!v.catalogName.equals(this.catalogName)) return false;

		if (fields.length != v.fields.length) return false;
		for (int in=0; in<fields.length; in++) {
			for (int i=0; i<this.fields[in].length; i++)
			{
				String f1 = this.fields[in][i];
				String f2 = null;
				if (this.fields[in].length == v.fields[in].length) {
					f2 = v.fields[in][i];
					if (!f1.equals(f2)) return false;
				} else {
					return false;
				}
			}
		}

		for (int i=0; i<this.bands.length; i++)
		{
			if (v.bands.length != this.bands.length) {
				return false;
			} else {
				if (!this.bands[i].equals(v.bands[i])) return false;
			}
		}

		return true;
	}

	/**
	 * To clone the object.
	 */
	@Override
	public VizierElement clone()
	{
		PhotometricBandElement[] bands = new PhotometricBandElement[this.bands.length];
		for (int i=0; i<this.bands.length; i++)
		{
			bands[i] = this.bands[i].clone();
		}

		VizierElement v = new VizierElement(this.catalogName, this.catalogAuthor,
				this.catalogDescription, this.beam, this.fields.clone(), bands);
		v.data = null;
		if (this.data != null) {
			v.data = new ArrayList<String[]>();
			for (int i=0; i<this.data.size(); i++) {
				v.data.add(this.data.get(i).clone());
			}
		}
		v.dataFields = null;
		if (dataFields != null) v.dataFields = dataFields.clone();
		v.links = null;
		if (links != null) v.links = this.links;
		v.unit = null;
		if (unit != null) v.unit = this.unit;
		return v;
	}

	/**
	 * Sets the table index for this Vizier object. This value
	 * is automatically set in JPARSEC by means of the
	 * {@linkplain VizierQuery#readVOTable(cds.savot.model.SavotVOTable, jparsec.observer.LocationElement)}.
	 *
	 * @param index The table index, 0 for the first (default
	 * value), 1 for the second, and so on.
	 */
	public void setTableIndex(int index) {
		this.tableIndex = index;
	}
	/**
	 * Returns the table index number for this Vizier object.
	 * 0 means the first table in the catalog, 1 the second, and so on.
	 * Most catalogs only contains 1 table so this value will be 0
	 * most of the times.
	 * @return Table index number.
	 */
	public int getTableIndex() {
		return this.tableIndex;
	}
}
