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

import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.math.Integration;
import jparsec.math.MeasureElement;
import jparsec.util.JPARSECException;

/**
 * A class to perform photometric calculations.<P>
 *
 * 2MASS information based on M. Cohen et al, "Spectral
 * Irradiance Calibration in the Infrared. XIV. The
 * Absolute Calibration of 2MASS", AJ 126, 1090 (2003).<P>
 *
 * Johnson and Morgan based on Johnson, H. L., ApJ 141,
 * 923 (1953).
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Photometry
{
	// private constructor so that this class cannot be instantiated.
	private Photometry() {}

	/**
	 * Obtains the flux from a photometric measurement. If the flux is given in
	 * Jy, not in magnitudes, then the flux in Jy will be returned, but only if
	 * the corresponding field {@linkplain PhotometricBandElement#fluxGivenAsMagnitude}
	 * is set to false.
	 * @param mag Absolute magnitude.
	 * @param dmag Magnitude error.
	 * @param band Data for the photometric band.
	 * @return Flux in Jy with its error.
	 */
	public static MeasureElement getFluxFromMagnitude(double mag, double dmag, PhotometricBandElement band)
	{
		if (!band.fluxGivenAsMagnitude) return new MeasureElement(mag, dmag, MeasureElement.UNIT_Y_JY);

		double flux = 0.0;

		double luminosityRatio = Math.pow(10.0, (band.magnitude0ForFlux - mag) / 2.5);
		flux = luminosityRatio * band.fluxAt0Magnitude;

		double dLumRatio = luminosityRatio * Math.log(10.0) * (-dmag) / 2.5;
		// Here I consider the error of the flux at magnitude 0 to be 0 to conserve the error in the input magnitude
		// when reversing back the conversion, otherwise it increases continuously.
		double dFlux = Math.sqrt(Math.pow(band.fluxAt0Magnitude * dLumRatio, 2.0) +
			Math.pow(luminosityRatio * band.fluxAt0MagnitudeError*0, 2.0));

		return new MeasureElement(flux, dFlux, MeasureElement.UNIT_Y_JY);
	}

	/**
	 * Obtains the flux from a photometric measurement. If the flux is given in
	 * Jy, not in magnitudes, then the flux in Jy will be returned, but only if
	 * the corresponding field {@linkplain PhotometricBandElement#fluxGivenAsMagnitude}
	 * is set to false.
	 * @param flux Flux in Jy.
	 * @param dflux Flux error.
	 * @param band Data for the photometric band.
	 * @return Magnitude and magnitude error.
	 */
	public static MeasureElement getMagnitudeFromFlux(double flux, double dflux, PhotometricBandElement band)
	{
		String yUnit = MeasureElement.UNIT_Y_MAG;
		if (band.equals(PhotometricBandElement.BAND_U_JOHNSON_MORGAN)) yUnit = MeasureElement.UNIT_Y_MAG_JOHNSON_U;
		if (band.equals(PhotometricBandElement.BAND_B_JOHNSON_MORGAN)) yUnit = MeasureElement.UNIT_Y_MAG_JOHNSON_B;
		if (band.equals(PhotometricBandElement.BAND_V_JOHNSON_MORGAN)) yUnit = MeasureElement.UNIT_Y_MAG_JOHNSON_V;
		if (band.equals(PhotometricBandElement.BAND_R_JOHNSON_MORGAN)) yUnit = MeasureElement.UNIT_Y_MAG_JOHNSON_R;
		if (band.equals(PhotometricBandElement.BAND_I_JOHNSON_MORGAN)) yUnit = MeasureElement.UNIT_Y_MAG_JOHNSON_I;
		if (band.equals(PhotometricBandElement.BAND_J_JOHNSON_MORGAN)) yUnit = MeasureElement.UNIT_Y_MAG_JOHNSON_J;
		if (band.equals(PhotometricBandElement.BAND_H_JOHNSON_MORGAN)) yUnit = MeasureElement.UNIT_Y_MAG_JOHNSON_H;
		if (band.equals(PhotometricBandElement.BAND_K_JOHNSON_MORGAN)) yUnit = MeasureElement.UNIT_Y_MAG_JOHNSON_K;
		if (band.equals(PhotometricBandElement.BAND_J_2MASS)) yUnit = MeasureElement.UNIT_Y_MAG_2MASS_J;
		if (band.equals(PhotometricBandElement.BAND_H_2MASS)) yUnit = MeasureElement.UNIT_Y_MAG_2MASS_H;
		if (band.equals(PhotometricBandElement.BAND_Ks_2MASS)) yUnit = MeasureElement.UNIT_Y_MAG_2MASS_Ks;

		if (!band.fluxGivenAsMagnitude) return new MeasureElement(0.0, 0.0, yUnit);

		double lumR = flux / band.fluxAt0Magnitude;
		double mag = band.magnitude0ForFlux - 2.5 * Math.log10(lumR);

		// Here I consider the error of the flux at magnitude 0 to be 0 to conserve the error in the input flux
		// when reversing back the conversion, otherwise it increases continuously.
		double dlumR = Math.sqrt(Math.pow(lumR * dflux / flux, 2.0) + Math.pow(lumR * band.fluxAt0MagnitudeError*0 / band.fluxAt0Magnitude, 2.0));
		double dmag = dlumR * 2.5 / (lumR * Math.log(10.0));

		return new MeasureElement(mag, dmag, yUnit);
	}

	/**
	 * Obtains approximate B and V magnitudes from Tycho BT and VT
	 * magnitudes.<P>
	 *
	 * @param Bt Tycho-2 BT magnitude.
	 * @param Vt Tycho-2 VT magnitude.
	 * @param ESA_method Method to apply, ESA official one (true) or the one developed by M. Kidger
	 * and F. Martin-Luis (false). The one by Kidger seems to be better. For explanations and request
	 * see http://homepage.ntlworld.com/roger.dymock/Tycho Photometry.htm.
	 * @return B and V magnitudes.
	 * @throws JPARSECException If an error occurs.
	 * @see Colors#bvTychoTobvJohnson(double) bvTychoTobvJohnson(double) method at
	 * Colors class for a possibly better algorithm.
	 */
	public static double[] getApproximateJohnsonBVFromTycho(double Bt, double Vt,
			boolean ESA_method)
	throws JPARSECException {
		double v = 0.0, b = 0.0;
		if (ESA_method) {
			v = Vt - 0.090 * (Bt - Vt);
			b = v + 0.850 * (Bt - Vt);
		} else {
			v = Vt - 0.016 - 0.0741 * (Bt - Vt);
			b = Bt + 0.064 - 0.2983 * (Bt - Vt);
		}
		return new double[] {b, v};
	}

	/**
	 * Obtains V-R color in Johnson system from Cousins system.
	 * Reference: Fernie, J. D., 1983PASP...95..782F.
	 * @param vr V-R color in Cousins.
	 * @return V-R in Johnson.
	 */
	public static double getJohnsonVRFromCousins(double vr)
	{
		double vrJ = 0.034 + 1.364 * vr;
		if (vr > 0.8) vrJ = -0.311 + 1.803 * vr;
		return vrJ;
	}
	/**
	 * Obtains V-I color in Johnson system from Cousins system.
	 * Reference: Fernie, J. D., 1983PASP...95..782F.
	 * @param vi V-I color in Cousins.
	 * @return V-I in Johnson.
	 */
	public static double getJohnsonVIFromCousins(double vi)
	{
		double viJ = -0.005 + 1.273 * vi;
		if (vi > 1.5) viJ = 0.723 + 0.486 * vi + 0.215 * vi * vi;
		return viJ;
	}
	/**
	 * Obtains R-I color in Johnson system from Cousins system.
	 * Reference: Fernie, J. D., 1983PASP...95..782F.
	 * @param ri R-I color in Cousins.
	 * @return R-I in Johnson.
	 */
	public static double getJohnsonRIFromCousins(double ri)
	{
		double riJ = -0.040 + 1.176 * ri;
		if (ri > 0.7) riJ = 0.205 + 0.733 * ri + 0.171 * ri * ri;
		return riJ;
	}

	private static final String[] JOHNSON_U = new String[] {
		   "3000    0.000",
		   "3050    0.040",
		   "3100    0.100",
		   "3150    0.250",
		   "3200    0.610",
		   "3250    0.750",
		   "3300    0.840",
		   "3350    0.880",
		   "3400    0.930",
		   "3450    0.950",
		   "3500    0.970",
		   "3550    0.990",
		   "3600    1.000",
		   "3650    0.990",
		   "3700    0.970",
		   "3750    0.920",
		   "3800    0.730",
		   "3850    0.560",
		   "3900    0.360",
		   "3950    0.230",
		   "4000    0.050",
		   "4050    0.030",
		   "4100    0.010",
		   "4150    0.000"
	};
	private static final String[] JOHNSON_B = new String[] {
		  "3600    0.000",
		   "3650    0.000",
		   "3700    0.020",
		   "3750    0.050",
		   "3800    0.110",
		   "3850    0.180",
		   "3900    0.350",
		   "3950    0.550",
		   "4000    0.920",
		   "4050    0.950",
		   "4100    0.980",
		   "4150    0.990",
		   "4200    1.000",
		   "4250    0.990",
		   "4300    0.980",
		   "4350    0.960",
		   "4400    0.940",
		   "4450    0.910",
		   "4500    0.870",
		   "4550    0.830",
		   "4600    0.790",
		   "4650    0.740",
		   "4700    0.690",
		   "4750    0.630",
		   "4800    0.580",
		   "4850    0.520",
		   "4900    0.460",
		   "4950    0.410",
		   "5000    0.360",
		   "5050    0.300",
		   "5100    0.250",
		   "5150    0.200",
		   "5200    0.150",
		   "5250    0.120",
		   "5300    0.090",
		   "5350    0.060",
		   "5400    0.040",
		   "5450    0.020",
		   "5500    0.010",
		   "5550    0.000"
	};
	private static final String[] JOHNSON_V = new String[] {
		   "4600    0.000",
		   "4650    0.000",
		   "4700    0.010",
		   "4750    0.010",
		   "4800    0.020",
		   "4850    0.050",
		   "4900    0.110",
		   "4950    0.200",
		   "5000    0.380",
		   "5050    0.670",
		   "5100    0.780",
		   "5150    0.850",
		   "5200    0.910",
		   "5250    0.940",
		   "5300    0.960",
		   "5350    0.980",
		   "5400    0.980",
		   "5450    0.950",
		   "5500    0.870",
		   "5550    0.790",
		   "5600    0.720",
		   "5650    0.710",
		   "5700    0.690",
		   "5750    0.650",
		   "5800    0.620",
		   "5850    0.580",
		   "5900    0.520",
		   "5950    0.460",
		   "6000    0.400",
		   "6050    0.340",
		   "6100    0.290",
		   "6150    0.240",
		   "6200    0.200",
		   "6250    0.170",
		   "6300    0.140",
		   "6350    0.110",
		   "6400    0.080",
		   "6450    0.060",
		   "6500    0.050",
		   "6550    0.030",
		   "6600    0.020",
		   "6650    0.020",
		   "6700    0.010",
		   "6750    0.010",
		   "6800    0.010",
		   "6850    0.010",
		   "6900    0.010",
		   "6950    0.010",
		   "7000    0.010",
		   "7050    0.010",
		   "7100    0.010",
		   "7150    0.010",
		   "7200    0.010",
		   "7250    0.010",
		   "7300    0.010",
		   "7350    0.000"
	};
	private static final String[] JOHNSON_R = new String[] {
		   "5200    0.000",
		   "5250    0.010",
		   "5300    0.020",
		   "5350    0.040",
		   "5400    0.060",
		   "5450    0.110",
		   "5500    0.180",
		   "5550    0.230",
		   "5600    0.280",
		   "5650    0.340",
		   "5700    0.400",
		   "5750    0.460",
		   "5800    0.500",
		   "5850    0.550",
		   "5900    0.600",
		   "5950    0.640",
		   "6000    0.690",
		   "6050    0.710",
		   "6100    0.740",
		   "6150    0.770",
		   "6200    0.790",
		   "6250    0.810",
		   "6300    0.840",
		   "6350    0.860",
		   "6400    0.880",
		   "6450    0.900",
		   "6500    0.910",
		   "6550    0.920",
		   "6600    0.940",
		   "6650    0.950",
		   "6700    0.960",
		   "6750    0.970",
		   "6800    0.980",
		   "6850    0.990",
		   "6900    0.990",
		   "6950    1.000",
		   "7000    1.000",
		   "7050    0.990",
		   "7100    0.980",
		   "7150    0.960",
		   "7200    0.940",
		   "7250    0.920",
		   "7300    0.900",
		   "7350    0.880",
		   "7400    0.850",
		   "7450    0.830",
		   "7500    0.800",
		   "7550    0.770",
		   "7600    0.730",
		   "7650    0.700",
		   "7700    0.660",
		   "7750    0.620",
		   "7800    0.570",
		   "7850    0.530",
		   "7900    0.490",
		   "7950    0.450",
		   "8000    0.420",
		   "8050    0.390",
		   "8100    0.360",
		   "8150    0.340",
		   "8200    0.310",
		   "8250    0.270",
		   "8300    0.220",
		   "8350    0.190",
		   "8400    0.170",
		   "8450    0.150",
		   "8500    0.130",
		   "8550    0.120",
		   "8600    0.110",
		   "8650    0.100",
		   "8700    0.080",
		   "8750    0.070",
		   "8800    0.060",
		   "8850    0.060",
		   "8900    0.050",
		   "8950    0.040",
		   "9000    0.040",
		   "9050    0.030",
		   "9100    0.030",
		   "9150    0.020",
		   "9200    0.020",
		   "9250    0.020",
		   "9300    0.010",
		   "9350    0.010",
		   "9400    0.010",
		   "9450    0.010",
		   "9500    0.000",
	};
	private static final String[] JOHNSON_I = new String[] {
		   "6800    0.000",
		   "6850    0.000",
		   "6900    0.010",
		   "6950    0.010",
		   "7000    0.010",
		   "7050    0.040",
		   "7100    0.080",
		   "7150    0.130",
		   "7200    0.170",
		   "7250    0.210",
		   "7300    0.260",
		   "7350    0.300",
		   "7400    0.360",
		   "7450    0.400",
		   "7500    0.440",
		   "7550    0.490",
		   "7600    0.560",
		   "7650    0.600",
		   "7700    0.650",
		   "7750    0.720",
		   "7800    0.760",
		   "7850    0.840",
		   "7900    0.900",
		   "7950    0.930",
		   "8000    0.960",
		   "8050    0.970",
		   "8100    0.970",
		   "8150    0.980",
		   "8200    0.980",
		   "8250    0.990",
		   "8300    0.990",
		   "8350    0.990",
		   "8400    0.990",
		   "8450    1.000",
		   "8500    1.000",
		   "8550    1.000",
		   "8600    1.000",
		   "8650    1.000",
		   "8700    0.990",
		   "8750    0.980",
		   "8800    0.980",
		   "8850    0.970",
		   "8900    0.960",
		   "8950    0.940",
		   "9000    0.930",
		   "9050    0.900",
		   "9100    0.880",
		   "9150    0.860",
		   "9200    0.840",
		   "9250    0.800",
		   "9300    0.760",
		   "9350    0.740",
		   "9400    0.710",
		   "9450    0.680",
		   "9500    0.650",
		   "9550    0.610",
		   "9600    0.580",
		   "9650    0.560",
		   "9700    0.520",
		   "9750    0.500",
		   "9800    0.470",
		   "9850    0.440",
		   "9900    0.420",
		   "9950    0.390",
		  "10000    0.360",
		  "10050    0.340",
		  "10100    0.320",
		  "10150    0.300",
		  "10200    0.280",
		  "10250    0.260",
		  "10300    0.240",
		  "10350    0.220",
		  "10400    0.200",
		  "10450    0.190",
		  "10500    0.170",
		  "10550    0.160",
		  "10600    0.150",
		  "10650    0.130",
		  "10700    0.120",
		  "10750    0.110",
		  "10800    0.100",
		  "10850    0.090",
		  "10900    0.090",
		  "10950    0.080",
		  "11000    0.080",
		  "11050    0.070",
		  "11100    0.060",
		  "11150    0.050",
		  "11200    0.050",
		  "11250    0.040",
		  "11300    0.040",
		  "11350    0.030",
		  "11400    0.030",
		  "11450    0.020",
		  "11500    0.020",
		  "11550    0.020",
		  "11600    0.020",
		  "11650    0.020",
		  "11700    0.010",
		  "11750    0.010",
		  "11800    0.010",
		  "11850    0.000"
	};

	private static final String[] MASS2_H = new String[] {
		"1.289   0.",
		"1.315   5.91635E-08",
		"1.341   1.27100E-07",
		"1.368   0.",
		"1.397   0.",
		"1.418   1.71065E-05",
		"1.440   5.11074E-04",
		"1.462   2.76582E-03",
		"1.478   8.08827E-03",
		"1.486   2.87356E-02",
		"1.493   8.71147E-02",
		"1.504   0.201449",
		"1.515   0.438159",
		"1.528   0.686357",
		"1.539   0.818076",
		"1.546   0.882073",
		"1.551   0.911825",
		"1.556   0.926872",
		"1.565   0.929288",
		"1.572   0.872747",
		"1.577   0.856619",
		"1.583   0.882556",
		"1.592   0.918084",
		"1.597   0.926654",
		"1.602   0.907594",
		"1.613   0.925974",
		"1.619   0.920496",
		"1.628   0.924198",
		"1.633   0.923533",
		"1.642   0.941788",
		"1.648   0.949134",
		"1.657   0.980658",
		"1.659   0.993744",
		"1.671   1.00000",
		"1.684   0.956052",
		"1.701   0.924116",
		"1.715   0.982120",
		"1.727   0.991589",
		"1.739   0.988683",
		"1.746   0.979168",
		"1.751   0.968184",
		"1.753   0.937040",
		"1.756   0.918998",
		"1.764   0.842264",
		"1.775   0.667111",
		"1.785   0.269402",
		"1.790   0.451630",
		"1.796   0.173062",
		"1.803   0.107726",
		"1.810   7.07003E-02",
		"1.813   5.10945E-03",
		"1.818   1.99705E-02",
		"1.828   3.91934E-04",
		"1.835   1.53053E-06",
		"1.850   5.94581E-05",
		"1.871   0.",
		"1.893   3.05088E-05",
		"1.914   0."
	};
	private static final String[] MASS2_J = new String[] {
		"1.062   0.",
		"1.066   4.07068E-04",
		"1.070   1.54293E-03",
		"1.075   2.67013E-03",
		"1.078   5.50643E-03",
		"1.082   1.22532E-02",
		"1.084   2.02928E-02",
		"1.087   3.06470E-02",
		"1.089   4.05135E-02",
		"1.093   5.15324E-02",
		"1.096   5.63529E-02",
		"1.102   7.18073E-02",
		"1.105   0.273603",
		"1.107   0.340997",
		"1.109   0.358446",
		"1.112   0.380134",
		"1.116   0.330668",
		"1.117   0.239548",
		"1.120   0.250062",
		"1.123   0.283301",
		"1.128   0.258233",
		"1.129   0.251474",
		"1.132   0.538119",
		"1.134   0.223168",
		"1.138   0.536893",
		"1.140   0.110203",
		"1.143   0.529207",
		"1.147   0.261940",
		"1.154   0.320155",
		"1.159   0.174300",
		"1.164   0.607031",
		"1.167   0.617933",
		"1.170   0.676289",
		"1.173   0.727940",
		"1.175   0.746531",
		"1.179   0.830404",
		"1.182   0.790307",
		"1.186   0.809605",
		"1.188   0.836888",
		"1.192   0.835984",
		"1.195   0.749936",
		"1.199   0.708013",
		"1.202   0.698759",
		"1.209   0.704854",
		"1.216   0.700382",
		"1.221   0.732765",
		"1.227   0.705725",
		"1.231   0.842431",
		"1.236   0.921873",
		"1.240   0.952505",
		"1.244   0.967585",
		"1.247   0.959508",
		"1.253   0.922697",
		"1.255   0.892978",
		"1.258   0.852943",
		"1.260   0.802308",
		"1.265   0.750078",
		"1.270   0.678072",
		"1.275   0.652417",
		"1.279   0.638754",
		"1.286   0.642413",
		"1.292   0.648560",
		"1.297   0.682380",
		"1.302   0.752903",
		"1.305   0.775942",
		"1.307   0.811828",
		"1.310   0.777008",
		"1.313   0.721030",
		"1.316   0.952459",
		"1.319   0.855137",
		"1.323   0.841401",
		"1.326   1.00000",
		"1.330   0.894736",
		"1.333   0.854912",
		"1.334   0.537894",
		"1.336   0.279866",
		"1.339   0.906532",
		"1.343   0.689345",
		"1.346   0.553327",
		"1.349   0.243177",
		"1.353   1.43760E-02",
		"1.355   1.89290E-04",
		"1.360   4.00791E-02",
		"1.363   4.53595E-03",
		"1.370   3.19968E-04",
		"1.373   3.72168E-02",
		"1.377   5.38436E-04",
		"1.383   0.",
		"1.388   1.44443E-04",
		"1.392   3.29774E-03",
		"1.395   3.14438E-04",
		"1.396   8.47738E-03",
		"1.397   2.53731E-02",
		"1.398   0.118446",
		"1.400   1.35728E-04",
		"1.401   6.10438E-05",
		"1.402   5.21326E-02",
		"1.404   1.03768E-02",
		"1.406   4.78050E-02",
		"1.407   4.19727E-04",
		"1.410   2.36641E-03",
		"1.412   5.26108E-03",
		"1.416   8.64765E-03",
		"1.421   7.28868E-04",
		"1.426   3.48399E-04",
		"1.442   3.78145E-04",
		"1.450   0."
	};
	private static final String[] MASS2_Ks = new String[] {
		"1.900   0.",
		"1.915   8.16050E-06",
		"1.927   1.61002E-05",
		"1.934   1.59036E-04",
		"1.939   4.94992E-04",
		"1.948   5.37610E-03",
		"1.957   1.18628E-02",
		"1.962   1.97031E-02",
		"1.969   4.21742E-02",
		"1.976   8.73064E-02",
		"1.981   0.152759",
		"1.989   0.248173",
		"1.990   0.190245",
		"1.998   0.233884",
		"2.008   0.294551",
		"2.014   0.398217",
		"2.019   0.336603",
		"2.028   0.620746",
		"2.037   0.764986",
		"2.045   0.746412",
		"2.061   0.625063",
		"2.072   0.725492",
		"2.075   0.689468",
		"2.082   0.787906",
		"2.089   0.818135",
		"2.099   0.822833",
		"2.106   0.863294",
		"2.113   0.877829",
		"2.120   0.854895",
		"2.124   0.895329",
		"2.138   0.918862",
		"2.145   0.926769",
		"2.155   0.926657",
		"2.169   0.900943",
		"2.176   0.922819",
		"2.185   0.842755",
		"2.197   0.945854",
		"2.208   0.980363",
		"2.213   0.987926",
		"2.218   0.984788",
		"2.232   0.964659",
		"2.237   0.981633",
		"2.248   0.983449",
		"2.256   0.961316",
		"2.260   0.979226",
		"2.263   1.00000",
		"2.265   0.963168",
		"2.270   0.981193",
		"2.272   0.968068",
		"2.276   0.910892",
		"2.277   0.982136",
		"2.281   0.889606",
		"2.284   0.891766",
		"2.286   0.942380",
		"2.291   0.840424",
		"2.293   0.804239",
		"2.295   0.707670",
		"2.297   0.657619",
		"2.299   0.560736",
		"2.306   0.443653",
		"2.311   0.348239",
		"2.316   0.230228",
		"2.320   0.162597",
		"2.325   0.135967",
		"2.328   9.21021E-02",
		"2.335   6.23901E-02",
		"2.339   4.30926E-02",
		"2.344   3.39814E-02",
		"2.346   3.09546E-02",
		"2.352   1.18112E-02",
		"2.361   6.83260E-03",
		"2.363   7.48518E-04",
		"2.370   2.99553E-03",
		"2.375   2.09686E-03",
		"2.384   4.06306E-04",
		"2.399   0."
	};

	/**
	 * The set of filters.
	 */
	public enum FILTER {
		/** ID constant for Johnson's U filter. */
		U_JOHNSON,
		/** ID constant for Johnson's V filter. */
		B_JOHNSON,
		/** ID constant for Johnson's B filter. */
		V_JOHNSON,
		/** ID constant for Johnson's R filter. */
		R_JOHNSON,
		/** ID constant for Johnson's I filter. */
		I_JOHNSON,
		/** ID constant for 2MASS J filter. */
		J_2MASS,
		/** ID constant for 2MASS H filter. */
		H_2MASS,
		/** ID constant for 2MASS Ks filter. */
		Ks_2MASS
	};

	/** Reference magnitudes for Vega in each of the filters UBVRIJHKs. 
	 * From Simbad, Jan 2008 */
	public static final double VEGA_MAGNITUDES[] = new double[] {
		0.02, 0.03, 0.03, 0.1, 0.2, -0.18, -0.03, 0.13
	};

	/**
	 * Returns the filter set of wavelengths in microns.
	 * @param filterID The filter ID constant.
	 * @return The set of wavelengths.
	 * @throws JPARSECException If the filter does not exist.
	 */
	public static double[] getFilterWavelengths(FILTER filterID)
	throws JPARSECException {
		String data[];
		switch(filterID)
		{
		case U_JOHNSON:
			data =Photometry.JOHNSON_U;
			break;
		case B_JOHNSON:
			data =Photometry.JOHNSON_B;
			break;
		case V_JOHNSON:
			data =Photometry.JOHNSON_V;
			break;
		case R_JOHNSON:
			data =Photometry.JOHNSON_R;
			break;
		case I_JOHNSON:
			data =Photometry.JOHNSON_I;
			break;
		case J_2MASS:
			data = Photometry.MASS2_J;
			break;
		case H_2MASS:
			data = Photometry.MASS2_H;
			break;
		case Ks_2MASS:
			data = Photometry.MASS2_Ks;
			break;
		default:
			throw new JPARSECException("invalid filter.");
		}

		double out[] = new double[data.length];
		for (int i=0; i<data.length; i++)
		{
			out[i] = DataSet.parseDouble(FileIO.getField(1, data[i], " ", true));
			if (filterID == FILTER.H_2MASS || filterID == FILTER.J_2MASS
					|| filterID == FILTER.Ks_2MASS) {
				out[i] = out[i];
			} else {
				out[i] = out[i] / 1.0E4;
			}
		}
		return out;
	}

	/**
	 * Returns the filter set of transmitancies, each of them for
	 * the corresponding filter wavelength. The values are
	 * normalized to unity.<P>
	 *
	 * The transmitancies are based on Johnson, ApJ 141, 923 (1965), and
	 * Cohen et al, AJ 126, 1090 (2003).
	 * @param filterID The filter ID constant.
	 * @return The set of transmitancies.
	 * @throws JPARSECException If the filter does not exist.
	 */
	public static double[] getFilterTransmitancy(FILTER filterID)
	throws JPARSECException {
		String data[];
		switch(filterID)
		{
		case U_JOHNSON:
			data =Photometry.JOHNSON_U;
			break;
		case B_JOHNSON:
			data =Photometry.JOHNSON_B;
			break;
		case V_JOHNSON:
			data =Photometry.JOHNSON_V;
			break;
		case R_JOHNSON:
			data =Photometry.JOHNSON_R;
			break;
		case I_JOHNSON:
			data =Photometry.JOHNSON_I;
			break;
		case J_2MASS:
			data = Photometry.MASS2_J;
			break;
		case H_2MASS:
			data = Photometry.MASS2_H;
			break;
		case Ks_2MASS:
			data = Photometry.MASS2_Ks;
			break;
		default:
			throw new JPARSECException("invalid filter.");
		}

		double out[] = new double[data.length];
		for (int i=0; i<data.length; i++)
		{
			out[i] = DataSet.parseDouble(FileIO.getField(2, data[i], " ", true));
		}
		return out;
	}

	/**
	 * Returns the flux of a star for a given effective temperature and integrated over a given filter.
	 * @param Tef The effective temperature of the star in K.
	 * @param filterID The filter ID constant.
	 * @return The flux, in units of Jy micron / sr.
	 * @throws JPARSECException If the filter does not exist.
	 */
	public static double getStarFlux(double Tef, FILTER filterID)
	throws JPARSECException {
		double waves[] = Photometry.getFilterWavelengths(filterID);
		double trans[] = Photometry.getFilterTransmitancy(filterID);

		for (int i=0; i<waves.length; i++)
		{
			trans[i] *= Star.blackBody(Tef, waves[i] * 1.0E-6);
		}

		double minWave = DataSet.getMinimumValue(waves);
		double maxWave = DataSet.getMaximumValue(waves);
		int n = 1000;

		Integration in = new Integration(waves, trans, minWave, maxWave);
		double out = in.simpleIntegration((maxWave-minWave) / (double) n);
		return out;
	}

	/**
	 * Returns the color index of a star (considered as a black body)
	 * between two filters.
	 *
	 * @param Tef Effective temperature.
	 * @param filterID1 The first filter.
	 * @param filterID2 The second filter.
	 * @param vega True to use Vega as reference for 0 magnitude, false for a synthetic star.
	 * @return The approximate color index M (filter1) - M (filter2).
	 * @throws JPARSECException If the filters are invalid.
	 * @deprecated This method is not accurate, use
	 * {@linkplain jparsec.astrophysics.model.Kurucz#getColorIndexUsingKuruczModels(double, double, double, 
	 * jparsec.astronomy.Photometry.FILTER, jparsec.astronomy.Photometry.FILTER, boolean)}
	 * instead.
	 */
	public static double getBlackBodyColorIndex(double Tef, FILTER filterID1, FILTER filterID2, boolean vega)
	throws JPARSECException {
		double f1 = Photometry.getStarFlux(Tef, filterID1);
		double f2 = Photometry.getStarFlux(Tef, filterID2);

		// 9700 K is the effective temperature of Vega, used as reference for a color index of 0
		double fr1 = Photometry.getStarFlux(9700, filterID1);
		double fr2 = Photometry.getStarFlux(9700, filterID2);

		double color = -2.5 * Math.log10(f1 / f2) + 2.5 * Math.log10(fr1 / fr2);
		// Now correct for the 'true' color index of Vega, which now is not exactly zero due to evolution of instruments
		// and filters.
		if (vega) color = color + Photometry.VEGA_MAGNITUDES[filterID1.ordinal()] - Photometry.VEGA_MAGNITUDES[filterID2.ordinal()];
		return color;
	}
}
