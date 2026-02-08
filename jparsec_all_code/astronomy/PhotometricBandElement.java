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
package jparsec.astronomy;

import java.io.Serializable;

import jparsec.math.Constant;

/**
 * A class to hold photometric data from telescopes.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class PhotometricBandElement implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Effective wavelength in microns.
	 */
	public float effectiveWavelength;
	/**
	 * Effective wavelength error in microns.
	 */
	public float effectiveWavelengthError;
	/**
	 * Bandwidth in microns.
	 */
	public float bandWidth;
	/**
	 * Bandwidth error in microns.
	 */
	public float bandWidthError;
	/**
	 * Flux for magnitude mag0 in Jy.
	 */
	public float fluxAt0Magnitude;
	/**
	 * Flux error for magnitude mag0 in Jy.
	 */
	public float fluxAt0MagnitudeError;
	/**
	 * Sets the magnitude (absolute) where flux are related to. Set to 0 by default.
	 */
	public float magnitude0ForFlux;
	/**
	 * True (default value) to consider the flux as given in magnitudes. Otherwise, it
	 * will be consider as given in Jy.
	 */
	public boolean fluxGivenAsMagnitude = true;

	/**
	 * Beam of the instrument in arcseconds.
	 */
	public float beam;

	/**
	 * Name of the field (for an external catalogue) that contains the data for this band.
	 */
	public String fieldName = "";
	/**
	 * Name of the photometric band.
	 */
	public String name = "";

	/**
	 * Empty constructor.
	 */
	public PhotometricBandElement()	{}

	/**
	 * Constructor with main parameters.
	 * @param wave Effective wavelength.
	 * @param width Bandwidth.
	 * @param flux0 Flux for magnitude 0.
	 */
	public PhotometricBandElement(float wave, float width, float flux0)
	{
		this.effectiveWavelength = wave;
		this.bandWidth = width;
		this.fluxAt0Magnitude = flux0;

		this.magnitude0ForFlux = 0.0f;
	}

	/**
	 * Full constructor.
	 * @param name Name.
	 * @param wave Effective wavelength.
	 * @param dwave Effective wavelength error.
	 * @param width Bandwidth.
	 * @param dwidth Bandwidth error.
	 * @param flux0 Flux for magnitude 0.
	 * @param dflux0 Flux for magnitude 0 error.
	 * @param beam The beam.
	 */
	public PhotometricBandElement(String name, float wave, float dwave, float width,
			float dwidth, float flux0, float dflux0, float beam)
	{
		this.name = name;
		this.effectiveWavelength = wave;
		this.bandWidth = width;
		this.fluxAt0Magnitude = flux0;

		this.effectiveWavelengthError = dwave;
		this.bandWidthError = dwidth;
		this.fluxAt0MagnitudeError = dflux0;

		this.magnitude0ForFlux = 0.0f;
		this.beam = beam;
	}

	/**
	 * Full constructor setting also the magnitude/Jy flux flag.
	 * @param name Name.
	 * @param wave Effective wavelength.
	 * @param dwave Effective wavelength error.
	 * @param width Bandwidth.
	 * @param dwidth Bandwidth error.
	 * @param flux0 Flux for magnitude 0.
	 * @param dflux0 Flux for magnitude 0 error.
	 * @param beam The beam.
	 * @param magnitude False to consider the flux in Jy.
	 */
	public PhotometricBandElement(String name, float wave, float dwave, float width,
			float dwidth, float flux0, float dflux0, float beam, boolean magnitude)
	{
		this.name = name;
		this.effectiveWavelength = wave;
		this.bandWidth = width;
		this.fluxAt0Magnitude = flux0;

		this.effectiveWavelengthError = dwave;
		this.bandWidthError = dwidth;
		this.fluxAt0MagnitudeError = dflux0;

		this.magnitude0ForFlux = 0.0f;
		this.fluxGivenAsMagnitude = magnitude;
		this.beam = beam;
	}

	/**
	 * Sets the name of the photometric band and field name.
	 * @param field Field name.
	 * @param name Name.
	 * @return The band object.
	 */
	public PhotometricBandElement setBandAndFieldName(String field, String name)
	{
		this.fieldName = field;
		this.name = name;
		return this;
	}

	private static final float BEAM_2MASS = 2.0f;
	private static final float BEAM_JOHNSON = 1.0f;
	private static final float BEAM_MSX6C = 18.3f;
	private static final float BEAM_DENIS = 3.0f;
	private static final float BEAM_IRAS12 = 30.0f;
	private static final float BEAM_IRAS25 = 30.0f;
	private static final float BEAM_IRAS60 = 60.0f;
	private static final float BEAM_IRAS100 = 120.0f;

	/**
	 * Photometric data for 1.4 GHz VLA.
	 */
	public static final PhotometricBandElement BAND_1p4GHz_VLA = new PhotometricBandElement(
			"VLA 1.4 GHz",
			214137.5f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 15, false);
	/**
	 * Photometric data for 2MASS J band, Cohen et al. 2003.
	 */
	public static final PhotometricBandElement BAND_J_2MASS = new PhotometricBandElement(
			"2MASS J",
			1.235f, 0.006f, 0.162f, 0.001f, 1594, 27.8f, BEAM_2MASS);
	/**
	 * Photometric data for 2MASS H band, Cohen et al. 2003.
	 */
	public static final PhotometricBandElement BAND_H_2MASS = new PhotometricBandElement(
			"2MASS H",
			1.662f, 0.009f, 0.251f, 0.002f, 1024, 20.0f, BEAM_2MASS);
	/**
	 * Photometric data for 2MASS Ks band, Cohen et al. 2003.
	 */
	public static final PhotometricBandElement BAND_Ks_2MASS = new PhotometricBandElement(
			"2MASS Ks",
			2.159f, 0.011f, 0.262f, 0.002f, 666.7f, 12.6f, BEAM_2MASS);
    /**
     * Photometric data for 12 micron IRAS band. Just effective wavelength.
     */
    public static final PhotometricBandElement BAND_12_IRAS = new PhotometricBandElement(
			"IRAS 12um",
			12.0f, 0.0f, (float) (0.0*2.0), 0.0f, 0.0f, 0.0f, BEAM_IRAS12, false);
    /**
     * Photometric data for 25 micron IRAS band. Just effective wavelength.
     */
    public static final PhotometricBandElement BAND_25_IRAS = new PhotometricBandElement(
			"IRAS 25um",
			25.0f, 0.0f, (float) (0.0*2.0), 0.0f, 0.0f, 0.0f, BEAM_IRAS25, false);
    /**
     * Photometric data for 60 micron IRAS band. Just effective wavelength.
     */
    public static final PhotometricBandElement BAND_60_IRAS = new PhotometricBandElement(
			"IRAS 60um",
			60.0f, 0.0f, (float) (0.0*2.0), 0.0f, 0.0f, 0.0f, BEAM_IRAS60, false);
    /**
     * Photometric data for 100 micron IRAS band. Just effective wavelength.
     */
    public static final PhotometricBandElement BAND_100_IRAS = new PhotometricBandElement(
			"IRAS 100um",
			100.0f, 0.0f, (float) (0.0*2.0), 0.0f, 0.0f, 0.0f, BEAM_IRAS100, false);

    /**
     * Photometric data for 250 GHz (30m) observations.
     */
    public static final PhotometricBandElement BAND_250GHz_30m = new PhotometricBandElement(
			"30m at 250GHz",
			1199.17f, 0.0f, (float) (0.0*2.0), 0.0f, 0.0f, 0.0f, 10, false);

    /**
     * Photometric data for Catalogue of stellar UV fluxes.
     */
    public static final PhotometricBandElement BAND_F2740_UV = new PhotometricBandElement(
			"Catalogue of stellar UV fluxes",
			0.274f, 0.0f, (float) (0.0*2.0), 0.0f, 0.0f, 0.0f, 2, false);
    /**
     * Photometric data for Catalogue of stellar UV fluxes.
     */
    public static final PhotometricBandElement BAND_F2365_UV = new PhotometricBandElement(
			"Catalogue of stellar UV fluxes",
			0.2365f, 0.0f, (float) (0.0*2.0), 0.0f, 0.0f, 0.0f, 2, false);
    /**
     * Photometric data for Catalogue of stellar UV fluxes.
     */
    public static final PhotometricBandElement BAND_F1965_UV = new PhotometricBandElement(
			"Catalogue of stellar UV fluxes",
			0.1965f, 0.0f, (float) (0.0*2.0), 0.0f, 0.0f, 0.0f, 2, false);
    /**
     * Photometric data for Catalogue of stellar UV fluxes.
     */
    public static final PhotometricBandElement BAND_F1565_UV = new PhotometricBandElement(
			"Catalogue of stellar UV fluxes",
			0.1565f, 0.0f, (float) (0.0*2.0), 0.0f, 0.0f, 0.0f, 2, false);

    /**
     * Photometric data for MSX6C B1 band. Just effective wavelength and bandwidth. Egan et al. 2003.
     */
    public static final PhotometricBandElement BAND_B1_MSX6C = new PhotometricBandElement(
			"MSX6C B1",
			4.29f, 0.0f, (float) (0.07*2.0), 0.0f, 0.0f, 0.0f, BEAM_MSX6C, false);
    /**
     * Photometric data for MSX6C B2 band. Just effective wavelength and bandwidth. Egan et al. 2003.
     */
    public static final PhotometricBandElement BAND_B2_MSX6C = new PhotometricBandElement(
			"MSX6C B2",
			4.35f, 0.0f, (float) (0.105*2.0), 0.0f, 0.0f, 0.0f, BEAM_MSX6C, false);
    /**
     * Photometric data for MSX6C A band. Just effective wavelength and bandwidth. Egan et al. 2003.
     */
    public static final PhotometricBandElement BAND_A_MSX6C = new PhotometricBandElement(
			"MSX6C A",
			8.28f, 0.0f, (float) (1.5*2.0), 0.0f, 0.0f, 0.0f, BEAM_MSX6C, false);
    /**
     * Photometric data for MSX6C C band. Just effective wavelength and bandwidth. Egan et al. 2003.
     */
    public static final PhotometricBandElement BAND_C_MSX6C = new PhotometricBandElement(
			"MSX6C C",
			12.13f, 0.0f, (float) (1.05*2.0), 0.0f, 0.0f, 0.0f, BEAM_MSX6C, false);
    /**
     * Photometric data for MSX6C D band. Just effective wavelength and bandwidth. Egan et al. 2003.
     */
    public static final PhotometricBandElement BAND_D_MSX6C = new PhotometricBandElement(
			"MSX6C D",
			14.65f, 0.0f, (float) (1.2*2.0), 0.0f, 0.0f, 0.0f, BEAM_MSX6C, false);
    /**
     * Photometric data for MSX6C E band. Just effective wavelength and bandwidth. Egan et al. 2003.
     */
    public static final PhotometricBandElement BAND_E_MSX6C = new PhotometricBandElement(
			"MSX6C E",
			21.34f, 0.0f, (float) (3.5*2.0), 0.0f, 0.0f, 0.0f, BEAM_MSX6C, false);

    /**
     * Photometric data for Johnson and Morgan U band. Johnson and Morgan, 1953.
     */
    public static final PhotometricBandElement BAND_U_JOHNSON_MORGAN = new PhotometricBandElement(
			"JOHNSON U",
			0.36f, 0.0f, (float) (0.04*2.0), 0.0f, (float) (1.88E-23/Constant.JY_TO_W_HZ_M2), 0.0f, BEAM_JOHNSON);
    /**
     * Photometric data for Johnson and Morgan B band. Johnson and Morgan, 1953.
     */
    public static final PhotometricBandElement BAND_B_JOHNSON_MORGAN = new PhotometricBandElement(
			"JOHNSON B",
			0.44f, 0.0f, (float) (0.10*2.0), 0.0f, (float) (4.44E-23/Constant.JY_TO_W_HZ_M2), 0.0f, BEAM_JOHNSON);
    /**
     * Photometric data for Johnson and Morgan V band. Johnson and Morgan, 1953.
     */
    public static final PhotometricBandElement BAND_V_JOHNSON_MORGAN = new PhotometricBandElement(
			"JOHNSON V",
			0.55f, 0.0f, (float) (0.08*2.0), 0.0f, (float) (3.81E-23/Constant.JY_TO_W_HZ_M2), 0.0f, BEAM_JOHNSON);
    /**
     * Photometric data for Johnson and Morgan R band. Johnson and Morgan, 1953.
     */
    public static final PhotometricBandElement BAND_R_JOHNSON_MORGAN = new PhotometricBandElement(
			"JOHNSON R",
			0.70f, 0.0f, (float) (0.21*2.0), 0.0f, (float) (3.01E-23/Constant.JY_TO_W_HZ_M2), 0.0f, BEAM_JOHNSON);
    /**
     * Photometric data for Johnson and Morgan I band. Johnson and Morgan, 1953.
     */
    public static final PhotometricBandElement BAND_I_JOHNSON_MORGAN = new PhotometricBandElement(
			"JOHNSON I",
			0.90f, 0.0f, (float) (0.22*2.0), 0.0f, (float) (2.43E-23/Constant.JY_TO_W_HZ_M2), 0.0f, BEAM_JOHNSON);
    /**
     * Photometric data for Johnson and Morgan J band. Johnson and Morgan, 1953.
     */
    public static final PhotometricBandElement BAND_J_JOHNSON_MORGAN = new PhotometricBandElement(
			"JOHNSON J",
			1.25f, 0.0f, (float) (0.3*2.0), 0.0f, (float) (1.77E-23/Constant.JY_TO_W_HZ_M2), 0.0f, BEAM_JOHNSON);
    /**
     * Photometric data for Johnson and Morgan H band. Johnson and Morgan, 1953.
     */
    public static final PhotometricBandElement BAND_H_JOHNSON_MORGAN = new PhotometricBandElement(
			"JOHNSON H",
			1.62f, 0.0f, (float) (0.2*2.0), 0.0f, (float) (1.26E-13/(Constant.SPEED_OF_LIGHT*100.0*Constant.JY_TO_W_HZ_M2)), 0.0f, BEAM_JOHNSON);
    /**
     * Photometric data for Johnson and Morgan K band. Johnson and Morgan, 1953.
     */
    public static final PhotometricBandElement BAND_K_JOHNSON_MORGAN = new PhotometricBandElement(
			"JOHNSON K",
			2.2f, 0.0f, (float) (0.6*2.0), 0.0f, (float) (6.3E-24/Constant.JY_TO_W_HZ_M2), 0.0f, BEAM_JOHNSON);
    /**
     * Photometric data for Johnson and Morgan L band. Johnson and Morgan, 1953.
     */
    public static final PhotometricBandElement BAND_L_JOHNSON_MORGAN = new PhotometricBandElement(
			"JOHNSON L",
			3.4f, 0.0f, (float) (0.9*2.0), 0.0f, (float) (3.1E-24/Constant.JY_TO_W_HZ_M2), 0.0f, BEAM_JOHNSON);
    /**
     * Photometric data for Johnson and Morgan M band. Johnson and Morgan, 1953.
     */
    public static final PhotometricBandElement BAND_M_JOHNSON_MORGAN = new PhotometricBandElement(
			"JOHNSON M",
			5.0f, 0.0f, (float) (1.1*2.0), 0.0f, (float) (1.8E-24/Constant.JY_TO_W_HZ_M2), 0.0f, BEAM_JOHNSON);
    /**
     * Photometric data for Johnson and Morgan N band. Johnson and Morgan, 1953.
     */
    public static final PhotometricBandElement BAND_N_JOHNSON_MORGAN = new PhotometricBandElement(
			"JOHNSON N",
			10.2f, 0.0f, (float) (6.0*2.0), 0.0f, (float) (4.3E-25/Constant.JY_TO_W_HZ_M2), 0.0f, BEAM_JOHNSON);

    /**
     * Photometric data for DENIS I band. Fouqu&eacute; et al., 2000.
     */
    public static final PhotometricBandElement BAND_I_DENIS = new PhotometricBandElement(
			"DENIS I",
			0.791f, 0.010f, (float) (0.0*2.0), 0.0f, 2499f, 0.0f, BEAM_DENIS);
    /**
     * Photometric data for DENIS J band. Fouqu&eacute; et al., 2000.
     */
    public static final PhotometricBandElement BAND_J_DENIS = new PhotometricBandElement(
			"DENIS J",
			1.228f, 0.020f, (float) (0.0*2.0), 0.0f, 1595f, 0.0f, BEAM_DENIS);
    /**
     * Photometric data for DENIS Ks band. Fouqu&eacute; et al., 2000.
     */
    public static final PhotometricBandElement BAND_Ks_DENIS = new PhotometricBandElement(
			"DENIS Ks",
			2.145f, 0.015f, (float) (0.0*2.0), 0.0f, 665f, 0.0f, BEAM_DENIS);

    /**
     * Photometric data for WISE W1 band. Cutri et al, 2013.
     */
    public static final PhotometricBandElement BAND_W1_WISE = new PhotometricBandElement(
			"WISE 3.4um", 3.353f, 0.013f, 0.066f, 0.001f, 309.54f, 4.58f, 3);
    // http://wise2.ipac.caltech.edu/docs/release/prelim/expsup/sec4_3g.html

    /**
     * Photometric data for WISE W2 band. Cutri et al, 2013.
     */
    public static final PhotometricBandElement BAND_W2_WISE = new PhotometricBandElement(
			"WISE 4.6um", 4.603f, 0.017f, 1.042f, 0.001f, 171.787f, 2.52f, 4);
    /**
     * Photometric data for WISE W3 band. Cutri et al, 2013.
     */
    public static final PhotometricBandElement BAND_W3_WISE = new PhotometricBandElement(
			"WISE 12um", 11.561f, 0.045f, 5.5f, 0.02f, 31.674f, 0.45f, 8);
    /**
     * Photometric data for WISE W4 band. Cutri et al, 2013.
     */
    public static final PhotometricBandElement BAND_W4_WISE = new PhotometricBandElement(
			"WISE 22um", 22.088f, 0.118f, 4.1f, 0.05f, 8.363f, 0.12f, 15);

    /**
     * Photometric data for S65 AKARI band.
     */
    public static final PhotometricBandElement BAND_S65_AKARI = new PhotometricBandElement(
			"AKARI S65", 65f, 0.0f, 30, 0.0f, 0.0f, 0.0f, 30, false);
    /**
     * Photometric data for S90 AKARI band.
     */
    public static final PhotometricBandElement BAND_S90_AKARI = new PhotometricBandElement(
			"AKARI S90", 90f, 0.0f, 50, 0.0f, 0.0f, 0.0f, 45, false);
    /**
     * Photometric data for S140 AKARI band.
     */
    public static final PhotometricBandElement BAND_S140_AKARI = new PhotometricBandElement(
			"AKARI S140", 140f, 0.0f, 70, 0.0f, 0.0f, 0.0f, 60, false);
    /**
     * Photometric data for S160 AKARI band.
     */
    public static final PhotometricBandElement BAND_S160_AKARI = new PhotometricBandElement(
			"AKARI S160", 160f, 0.0f, 40, 0.0f, 0.0f, 0.0f, 70, false);

    private static final PhotometricBandElement[] ALL = new PhotometricBandElement[] {
    	PhotometricBandElement.BAND_100_IRAS,
    	PhotometricBandElement.BAND_12_IRAS,
    	PhotometricBandElement.BAND_1p4GHz_VLA,
    	PhotometricBandElement.BAND_25_IRAS,
    	PhotometricBandElement.BAND_60_IRAS,
    	PhotometricBandElement.BAND_A_MSX6C,
    	PhotometricBandElement.BAND_B1_MSX6C,
    	PhotometricBandElement.BAND_B2_MSX6C,
    	PhotometricBandElement.BAND_B_JOHNSON_MORGAN,
    	PhotometricBandElement.BAND_C_MSX6C,
    	PhotometricBandElement.BAND_D_MSX6C,
    	PhotometricBandElement.BAND_E_MSX6C,
    	PhotometricBandElement.BAND_H_2MASS,
    	PhotometricBandElement.BAND_H_JOHNSON_MORGAN,
    	PhotometricBandElement.BAND_I_DENIS,
    	PhotometricBandElement.BAND_I_JOHNSON_MORGAN,
    	PhotometricBandElement.BAND_J_2MASS,
    	PhotometricBandElement.BAND_J_DENIS,
    	PhotometricBandElement.BAND_J_JOHNSON_MORGAN,
    	PhotometricBandElement.BAND_K_JOHNSON_MORGAN,
    	PhotometricBandElement.BAND_Ks_2MASS,
    	PhotometricBandElement.BAND_Ks_DENIS,
    	PhotometricBandElement.BAND_L_JOHNSON_MORGAN,
    	PhotometricBandElement.BAND_M_JOHNSON_MORGAN,
    	PhotometricBandElement.BAND_N_JOHNSON_MORGAN,
    	PhotometricBandElement.BAND_R_JOHNSON_MORGAN,
    	PhotometricBandElement.BAND_U_JOHNSON_MORGAN,
    	PhotometricBandElement.BAND_V_JOHNSON_MORGAN,
    	PhotometricBandElement.BAND_W1_WISE,
    	PhotometricBandElement.BAND_W2_WISE,
    	PhotometricBandElement.BAND_W3_WISE,
    	PhotometricBandElement.BAND_W4_WISE,
    	PhotometricBandElement.BAND_S65_AKARI,
    	PhotometricBandElement.BAND_S90_AKARI,
    	PhotometricBandElement.BAND_S140_AKARI,
    	PhotometricBandElement.BAND_S160_AKARI
    };

    /**
     * Returns a given photometric band by its name.
     * @param name The name to search.
     * @return The photometric band, or null if it is not found.
     */
    public static PhotometricBandElement getPhotometricBand(String name)
    {
    	PhotometricBandElement p = null;
    	for (int i=0; i<ALL.length; i++)
    	{
    		if (name.equals(ALL[i].name)) p = ALL[i].clone();
    	}
    	return p;
    }

    /**
     * To clone the object.
     */
	@Override
    public PhotometricBandElement clone()
    {
    	PhotometricBandElement p = new PhotometricBandElement();
    	p.bandWidth = this.bandWidth;
    	p.bandWidthError = this.bandWidthError;
    	p.effectiveWavelength = this.effectiveWavelength;
    	p.effectiveWavelengthError = this.effectiveWavelengthError;
    	p.fluxAt0Magnitude = this.fluxAt0Magnitude;
    	p.fluxAt0MagnitudeError = this.fluxAt0MagnitudeError;
    	p.magnitude0ForFlux = this.magnitude0ForFlux;
    	p.fieldName = this.fieldName;
    	p.fluxGivenAsMagnitude = this.fluxGivenAsMagnitude;
    	p.name = this.name;
    	p.beam = this.beam;
    	return p;
    }

    /**
     * Checks if this object is equal to another one.
     * @param band Object to compare with.
     * @return True is it is equal.
     */
	@Override
	public boolean equals(Object band) {
		if (this == band) return true;
		if (!(band instanceof PhotometricBandElement)) return false;

		PhotometricBandElement that = (PhotometricBandElement) band;

		if (Float.compare(that.effectiveWavelength, effectiveWavelength) != 0) return false;
		if (Float.compare(that.effectiveWavelengthError, effectiveWavelengthError) != 0) return false;
		if (Float.compare(that.bandWidth, bandWidth) != 0) return false;
		if (Float.compare(that.bandWidthError, bandWidthError) != 0) return false;
		if (Float.compare(that.fluxAt0Magnitude, fluxAt0Magnitude) != 0) return false;
		if (Float.compare(that.fluxAt0MagnitudeError, fluxAt0MagnitudeError) != 0) return false;
		if (Float.compare(that.magnitude0ForFlux, magnitude0ForFlux) != 0) return false;
		if (fluxGivenAsMagnitude != that.fluxGivenAsMagnitude) return false;
		if (Float.compare(that.beam, beam) != 0) return false;
		if (fieldName != null ? !fieldName.equals(that.fieldName) : that.fieldName != null) return false;

		return !(name != null ? !name.equals(that.name) : that.name != null);
	}

	@Override
	public int hashCode() {
		int result = (effectiveWavelength != +0.0f ? Float.floatToIntBits(effectiveWavelength) : 0);
		result = 31 * result + (effectiveWavelengthError != +0.0f ? Float.floatToIntBits(effectiveWavelengthError) : 0);
		result = 31 * result + (bandWidth != +0.0f ? Float.floatToIntBits(bandWidth) : 0);
		result = 31 * result + (bandWidthError != +0.0f ? Float.floatToIntBits(bandWidthError) : 0);
		result = 31 * result + (fluxAt0Magnitude != +0.0f ? Float.floatToIntBits(fluxAt0Magnitude) : 0);
		result = 31 * result + (fluxAt0MagnitudeError != +0.0f ? Float.floatToIntBits(fluxAt0MagnitudeError) : 0);
		result = 31 * result + (magnitude0ForFlux != +0.0f ? Float.floatToIntBits(magnitude0ForFlux) : 0);
		result = 31 * result + (fluxGivenAsMagnitude ? 1 : 0);
		result = 31 * result + (beam != +0.0f ? Float.floatToIntBits(beam) : 0);
		result = 31 * result + (fieldName != null ? fieldName.hashCode() : 0);
		result = 31 * result + (name != null ? name.hashCode() : 0);
		return result;
	}
}
