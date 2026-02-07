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
package jparsec.ephem.probes;

import java.util.ArrayList;

import jparsec.astronomy.CoordinateSystem;
import jparsec.astronomy.Star;
import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.Functions;
import jparsec.ephem.Nutation;
import jparsec.ephem.Obliquity;
import jparsec.ephem.Precession;
import jparsec.ephem.EphemerisElement.ALGORITHM;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.event.Saros;
import jparsec.ephem.event.SimpleEventElement;
import jparsec.ephem.event.SimpleEventElement.EVENT;
import jparsec.ephem.planets.EphemElement;
import jparsec.graph.DataSet;
import jparsec.math.Constant;
import jparsec.math.FastMath;
import jparsec.observer.City;
import jparsec.observer.CityElement;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.observer.ReferenceEllipsoid.ELLIPSOID;
import jparsec.time.AstroDate;
import jparsec.time.SiderealTime;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.time.TimeScale;
import jparsec.util.DataBase;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;

/**
<p>The <code>SDP4_SGP4</code> class is a base class to calculate ephemeris for
an artificial Earth satellite, using the SGP4 and SDP4 models.</p>

<p>The SGP4 and SDP4 models of satellite prediction
(Felix R. Hoots, Roland L. Roehrich, T.S. Kelso, 1980, 1988, <em>Spacetrack report no. 3, Models for propagation of NORAD element sets</em>, <a href="http://www.celestrak.com/NORAD/documentation/">http://www.celestrak.com/NORAD/documentation/</a>)
give us algorithms to predict a few days' worth of motion of any
artificial satellite for which we have a NORAD Two Line Element set (TLE).
Hoots' report contains Fortran source code for three generations of
models: SGP, SGP4/SDP4 and SGP8/SDP8.  The TLE's are compiled assuming the
SGP4/SDP4 model, and that is the model that Kelso has ported to Pascal on
DOS.
(T.S. Kelso, 2000, <em>TrakStar 2.64</em>, <a href="http://www.celestrak.com">http://www.celestrak.com</a>,
T.S. Kelso, 1999, <em>NORAD SGP4/SDP4 units 2.60</em>, <a href="http://www.celestrak.com">http://www.celestrak.com</a>)
SGPn is used for near-Earth satellites (orbits shorter than 225&nbsp;min)
while SDPn is used for satellites further afield.</p>

@author Horst Meyerdierks, http://www.chiandh.me.uk
@author T. Alonso Albi - OAN (Spain)
 */
public class SDP4_SGP4
{
	private SatelliteOrbitalElement sat;
  /** Position vector [Gm] */
  private double[] itsR;
  /** Velocity vector [km/s] */
  private double[] itsV;
  /** The TLE epoch expressed in JD. */
  private double itsEpochJD;


  /** Whether period is >= 225 min */
  protected boolean isDeep;

  protected double E1_XMO,E1_XNODEO,E1_OMEGAO,E1_EO,E1_XINCL,
    E1_XNO,E1_XNDT2O,E1_XNDD6O,E1_BSTAR,E1_X,E1_Y,E1_Z,
    E1_XDOT,E1_YDOT,E1_ZDOT,E1_EPOCH,E1_DS50;

  protected double C1_CK2,C1_CK4,C1_E6A,C1_QOMS2T,C1_S,C1_TOTHRD,
    C1_XJ3,C1_XKE,C1_XKMPER,C1_XMNPDA,C1_AE;

  protected double C2_DE2RA,C2_PI,C2_PIO2,C2_TWOPI,C2_X3PIO2;

  protected double SGP4_A1,SGP4_A3OVK2,SGP4_AO,SGP4_AODP,SGP4_AYCOF,
    SGP4_BETAO,SGP4_BETAO2,SGP4_C1,SGP4_C1SQ,
    SGP4_C2,SGP4_C3,SGP4_C4,SGP4_C5,SGP4_COEF,SGP4_COEF1,
    SGP4_COSIO,SGP4_D2,SGP4_D3,SGP4_D4,SGP4_DEL1,SGP4_DELMO,
    SGP4_DELO,SGP4_EETA,
    SGP4_EOSQ,SGP4_ETA,SGP4_ETASQ,SGP4_OMGCOF,SGP4_OMGDOT,
    SGP4_PERIGE,SGP4_PINVSQ,SGP4_PSISQ,
    SGP4_QOMS24,SGP4_S4,SGP4_SINIO,SGP4_SINMO,SGP4_T2COF,
    SGP4_T3COF,SGP4_T4COF,SGP4_T5COF,SGP4_TEMP,SGP4_TEMP1,
    SGP4_TEMP2,SGP4_TEMP3,SGP4_THETA2,SGP4_THETA4,SGP4_TSI,
    SGP4_X1M5TH,SGP4_X1MTH2,SGP4_X3THM1,SGP4_X7THM1,
    SGP4_XHDOT1,SGP4_XLCOF,SGP4_XMCOF,SGP4_XMDOT,SGP4_XNODCF,
    SGP4_XNODOT,SGP4_XNODP;
  protected int SGP4_ISIMP;

  protected double SDP4_A1,SDP4_A3OVK2,SDP4_AO,SDP4_AODP,
    SDP4_AYCOF,SDP4_BETAO,SDP4_BETAO2,SDP4_C1,SDP4_C2,
    SDP4_C4,SDP4_COEF,SDP4_COEF1,SDP4_COSG,SDP4_COSIO,
    SDP4_DEL1,SDP4_DELO,SDP4_EETA,SDP4_EOSQ,
    SDP4_ETA,SDP4_ETASQ,SDP4_OMGDOT,SDP4_PERIGE,SDP4_PINVSQ,
    SDP4_PSISQ,SDP4_QOMS24,SDP4_S4,SDP4_SING,
    SDP4_SINIO,SDP4_T2COF,SDP4_TEMP1,SDP4_TEMP2,SDP4_TEMP3,
    SDP4_THETA2,SDP4_THETA4,SDP4_TSI,SDP4_X1M5TH,
    SDP4_X1MTH2,SDP4_X3THM1,SDP4_X7THM1,SDP4_XHDOT1,SDP4_XLCOF,
    SDP4_XMDOT,SDP4_XNODCF,SDP4_XNODOT,SDP4_XNODP;

  protected double DEEP_A1,DEEP_A2,DEEP_A3,DEEP_A4,DEEP_A5,DEEP_A6,
    DEEP_A7,DEEP_A8,DEEP_A9,DEEP_A10,DEEP_AINV2,DEEP_ALFDP,
    DEEP_AQNV,DEEP_ATIME,DEEP_BETDP,DEEP_BFACT,DEEP_C,DEEP_CC,
    DEEP_COSIS,DEEP_COSOK,DEEP_COSQ,DEEP_CTEM,DEEP_D2201,
    DEEP_D2211,DEEP_D3210,DEEP_D3222,DEEP_D4410,DEEP_D4422,
    DEEP_D5220,DEEP_D5232,DEEP_D5421,DEEP_D5433,DEEP_DALF,
    DEEP_DAY,DEEP_DBET,DEEP_DEL1,DEEP_DEL2,DEEP_DEL3,DEEP_DELT,
    DEEP_DLS,DEEP_E3,DEEP_EE2,DEEP_EOC,DEEP_EQ,DEEP_F2,
    DEEP_F220,DEEP_F221,DEEP_F3,DEEP_F311,DEEP_F321,DEEP_F322,
    DEEP_F330,DEEP_F441,DEEP_F442,DEEP_F522,DEEP_F523,
    DEEP_F542,DEEP_F543,DEEP_FASX2,DEEP_FASX4,DEEP_FASX6,
    DEEP_FT,DEEP_G200,DEEP_G201,DEEP_G211,DEEP_G300,DEEP_G310,
    DEEP_G322,DEEP_G410,DEEP_G422,DEEP_G520,DEEP_G521,DEEP_G532,
    DEEP_G533,DEEP_GAM,DEEP_OMEGAQ,DEEP_PE,DEEP_PGH,DEEP_PH,
    DEEP_PINC,DEEP_PL,DEEP_PREEP,DEEP_S1,DEEP_S2,
    DEEP_S3,DEEP_S4,DEEP_S5,DEEP_S6,DEEP_S7,DEEP_SAVTSN,DEEP_SE,
    DEEP_SE2,DEEP_SE3,DEEP_SEL,DEEP_SES,DEEP_SGH,DEEP_SGH2,
    DEEP_SGH3,DEEP_SGH4,DEEP_SGHL,DEEP_SGHS,DEEP_SH,DEEP_SH2,
    DEEP_SH3,DEEP_SH1,DEEP_SHS,DEEP_SI,DEEP_SI2,DEEP_SI3,
    DEEP_SIL,DEEP_SINI2,DEEP_SINIS,DEEP_SINOK,DEEP_SINQ,
    DEEP_SINZF,DEEP_SIS,DEEP_SL,DEEP_SL2,DEEP_SL3,DEEP_SL4,
    DEEP_SLL,DEEP_SLS,DEEP_SSE,DEEP_SSG,DEEP_SSH,DEEP_SSI,
    DEEP_SSL,DEEP_STEM,DEEP_STEP2,DEEP_STEPN,DEEP_STEPP,
    DEEP_TEMP,DEEP_TEMP1,DEEP_THGR,DEEP_X1,DEEP_X2,DEEP_X2LI,
    DEEP_X2OMI,DEEP_X3,DEEP_X4,DEEP_X5,DEEP_X6,DEEP_X7,DEEP_X8,
    DEEP_XFACT,DEEP_XGH2,DEEP_XGH3,DEEP_XGH4,DEEP_XH2,DEEP_XH3,
    DEEP_XI2,DEEP_XI3,DEEP_XL,DEEP_XL2,DEEP_XL3,DEEP_XL4,
    DEEP_XLAMO,DEEP_XLDOT,DEEP_XLI,DEEP_XLS,
    DEEP_XMAO,DEEP_XNDDT,DEEP_XNDOT,DEEP_XNI,DEEP_XNO2,
    DEEP_XNODCE,DEEP_XNOI,DEEP_XNQ,DEEP_XOMI,DEEP_XPIDOT,
    DEEP_XQNCL,DEEP_Z1,DEEP_Z11,DEEP_Z12,DEEP_Z13,DEEP_Z2,
    DEEP_Z21,DEEP_Z22,DEEP_Z23,DEEP_Z3,DEEP_Z31,DEEP_Z32,
    DEEP_Z33,DEEP_ZCOSG,DEEP_ZCOSGL,DEEP_ZCOSH,DEEP_ZCOSHL,
    DEEP_ZCOSI,DEEP_ZCOSIL,DEEP_ZE,DEEP_ZF,DEEP_ZM,DEEP_ZMO,
    DEEP_ZMOL,DEEP_ZMOS,DEEP_ZN,DEEP_ZSING,DEEP_ZSINGL,
    DEEP_ZSINH,DEEP_ZSINHL,DEEP_ZSINI,DEEP_ZSINIL,DEEP_ZX,DEEP_ZY;
  protected int DEEP_IRESFL,DEEP_ISYNFL,DEEP_IRET,DEEP_IRETN,DEEP_LS;

  protected double DEEP_ZNS,DEEP_C1SS,DEEP_ZES,DEEP_ZNL,DEEP_C1L,
    DEEP_ZEL,DEEP_ZCOSIS,DEEP_ZSINIS,DEEP_ZSINGS,
    DEEP_ZCOSGS,DEEP_Q22,DEEP_Q31,DEEP_Q33,DEEP_G22,DEEP_G32,
    DEEP_G44,DEEP_G52,DEEP_G54,
    DEEP_ROOT22,DEEP_ROOT32,DEEP_ROOT44,DEEP_ROOT52,DEEP_ROOT54,
    DEEP_THDT;

  protected double DPINI_EQSQ,DPINI_SINIQ,DPINI_COSIQ,
    DPINI_RTEQSQ,DPINI_AO,DPINI_COSQ2,DPINI_SINOMO,DPINI_COSOMO,
    DPINI_BSQ,DPINI_XLLDOT,DPINI_OMGDT,DPINI_XNODOT,DPINI_XNODP;

  protected double DPSEC_XLL,DPSEC_OMGASM,DPSEC_XNODES,DPSEC_EM,
    DPSEC_XINC,DPSEC_XN,DPSEC_T;

  /**
   * Returns the orbital elements for the artificial satellite
   * computed by this instance.
   * @return The {@linkplain SatelliteOrbitalElement} object.
   */
  public SatelliteOrbitalElement getSat() {
	  return this.sat;
  }
  /**
   * The constructor to apply SDP4/SGP4 model.
   * @param sat The orbital elements.
   * @throws JPARSECException If the orbital elements cannot be
   * parsed.
   */
  public SDP4_SGP4(SatelliteOrbitalElement sat) throws JPARSECException {
	    Init();

	    this.sat = sat;
	    ReadNorad12(sat);
  }

  /**
   * Initialize the SDP4.
   *
   * <p>This initializes the SDP4 object.  Most state variables are set to
   * zero, some to constants necessary for the calculations. */

  private void Init()
  {
    double QO, SO, XJ2, XJ4;

    itsR = new double[3];
    itsV = new double[3];
    itsR[0] = 0.01; itsR[1] = 0.; itsR[2] = 0.;
    itsV[0] = 0.;   itsV[1] = 0.; itsV[2] = 0.;
    itsEpochJD    = 0.;

    /* Initialize /E1/. */

    E1_XMO = 0.;
    E1_XNODEO = 0.;
    E1_OMEGAO = 0.;
    E1_EO = 0.;
    E1_XINCL = 0.;
    E1_XNO = 0.;
    E1_XNDT2O = 0.;
    E1_XNDD6O = 0.;
    E1_BSTAR = 0.;
    E1_X = 0.;
    E1_Y = 0.;
    E1_Z = 0.;
    E1_XDOT = 0.;
    E1_YDOT = 0.;
    E1_ZDOT = 0.;
    E1_EPOCH = 0.;
    E1_DS50 = 0.;

    /* Initialize /C1/. */

    C1_E6A = 1.E-6;
    C1_TOTHRD = .66666667;
    C1_XJ3 = -.253881E-5;
    C1_XKE = .743669161E-1;
    C1_XKMPER = 6378.135;
    C1_XMNPDA = 1440.;
    C1_AE = 1.;

    QO = 120.0;
    SO = 78.0;
    XJ2 = 1.082616E-3;
    XJ4 = -1.65597E-6;
    C1_CK2 = .5 * XJ2 * C1_AE * C1_AE;
    C1_CK4 = -.375 * XJ4 * C1_AE * C1_AE * C1_AE * C1_AE;
    C1_QOMS2T  = ((QO - SO) * C1_AE / C1_XKMPER);
    C1_QOMS2T *= C1_QOMS2T;
    C1_QOMS2T *= C1_QOMS2T;
    C1_S = C1_AE * (1. + SO / C1_XKMPER);

    /* Initialize /C2/. */

    C2_DE2RA = .174532925E-1;
    C2_PI = 3.14159265;
    C2_PIO2 = 1.57079633;
    C2_TWOPI = 6.2831853;
    C2_X3PIO2 = 4.71238898;

    /* Initialization of /COMSGP4/. */

    SGP4_A1 = 0.;
    SGP4_A3OVK2 = 0.;
    SGP4_AO = 0.;
    SGP4_AODP = 0.;
    SGP4_AYCOF = 0.;
    SGP4_BETAO = 0.;
    SGP4_BETAO2 = 0.;
    SGP4_C1 = 0.;
    SGP4_C1SQ = 0.;
    SGP4_C2 = 0.;
    SGP4_C3 = 0.;
    SGP4_C4 = 0.;
    SGP4_C5 = 0.;
    SGP4_COEF = 0.;
    SGP4_COEF1 = 0.;
    SGP4_COSIO = 0.;
    SGP4_D2 = 0.;
    SGP4_D3 = 0.;
    SGP4_D4 = 0.;
    SGP4_DEL1 = 0.;
    SGP4_DELMO = 0.;
    SGP4_DELO = 0.;
    SGP4_EETA = 0.;
    SGP4_EOSQ = 0.;
    SGP4_ETA = 0.;
    SGP4_ETASQ = 0.;
    SGP4_OMGCOF = 0.;
    SGP4_OMGDOT = 0.;
    SGP4_PERIGE = 0.;
    SGP4_PINVSQ = 0.;
    SGP4_PSISQ = 0.;
    SGP4_QOMS24 = 0.;
    SGP4_S4 = 0.;
    SGP4_SINIO = 0.;
    SGP4_SINMO = 0.;
    SGP4_T2COF = 0.;
    SGP4_T3COF = 0.;
    SGP4_T4COF = 0.;
    SGP4_T5COF = 0.;
    SGP4_TEMP = 0.;
    SGP4_TEMP1 = 0.;
    SGP4_TEMP2 = 0.;
    SGP4_TEMP3 = 0.;
    SGP4_THETA2 = 0.;
    SGP4_THETA4 = 0.;
    SGP4_TSI = 0.;
    SGP4_X1M5TH = 0.;
    SGP4_X1MTH2 = 0.;
    SGP4_X3THM1 = 0.;
    SGP4_X7THM1 = 0.;
    SGP4_XHDOT1 = 0.;
    SGP4_XLCOF = 0.;
    SGP4_XMCOF = 0.;
    SGP4_XMDOT = 0.;
    SGP4_XNODCF = 0.;
    SGP4_XNODOT = 0.;
    SGP4_XNODP = 0.;
    SGP4_ISIMP = 0;

    /* Initialization of /COMSDP4/. */

    SDP4_A1 = 0.;
    SDP4_A3OVK2 = 0.;
    SDP4_AO = 0.;
    SDP4_AODP = 0.;
    SDP4_AYCOF = 0.;
    SDP4_BETAO = 0.;
    SDP4_BETAO2 = 0.;
    SDP4_C1 = 0.;
    SDP4_C2 = 0.;
    SDP4_C4 = 0.;
    SDP4_COEF = 0.;
    SDP4_COEF1 = 0.;
    SDP4_COSG = 0.;
    SDP4_COSIO = 0.;
    SDP4_DEL1 = 0.;
    SDP4_DELO = 0.;
    SDP4_EETA = 0.;
    SDP4_EOSQ = 0.;
    SDP4_ETA = 0.;
    SDP4_ETASQ = 0.;
    SDP4_OMGDOT = 0.;
    SDP4_PERIGE = 0.;
    SDP4_PINVSQ = 0.;
    SDP4_PSISQ = 0.;
    SDP4_QOMS24 = 0.;
    SDP4_S4 = 0.;
    SDP4_SING = 0.;
    SDP4_SINIO = 0.;
    SDP4_T2COF = 0.;
    SDP4_TEMP1 = 0.;
    SDP4_TEMP2 = 0.;
    SDP4_TEMP3 = 0.;
    SDP4_THETA2 = 0.;
    SDP4_THETA4 = 0.;
    SDP4_TSI = 0.;
    SDP4_X1M5TH = 0.;
    SDP4_X1MTH2 = 0.;
    SDP4_X3THM1 = 0.;
    SDP4_X7THM1 = 0.;
    SDP4_XHDOT1 = 0.;
    SDP4_XLCOF = 0.;
    SDP4_XMDOT = 0.;
    SDP4_XNODCF = 0.;
    SDP4_XNODOT = 0.;
    SDP4_XNODP = 0.;

    /* Initialization of /COMDEEP1/. */

    DEEP_A1 = 0.;
    DEEP_A2 = 0.;
    DEEP_A3 = 0.;
    DEEP_A4 = 0.;
    DEEP_A5 = 0.;
    DEEP_A6 = 0.;
    DEEP_A7 = 0.;
    DEEP_A8 = 0.;
    DEEP_A9 = 0.;
    DEEP_A10 = 0.;
    DEEP_AINV2 = 0.;
    DEEP_ALFDP = 0.;
    DEEP_AQNV = 0.;
    DEEP_ATIME = 0.;
    DEEP_BETDP = 0.;
    DEEP_BFACT = 0.;
    DEEP_C = 0.;
    DEEP_CC = 0.;
    DEEP_COSIS = 0.;
    DEEP_COSOK = 0.;
    DEEP_COSQ = 0.;
    DEEP_CTEM = 0.;
    DEEP_D2201 = 0.;
    DEEP_D2211 = 0.;
    DEEP_D3210 = 0.;
    DEEP_D3222 = 0.;
    DEEP_D4410 = 0.;
    DEEP_D4422 = 0.;
    DEEP_D5220 = 0.;
    DEEP_D5232 = 0.;
    DEEP_D5421 = 0.;
    DEEP_D5433 = 0.;
    DEEP_DALF = 0.;
    DEEP_DAY = 0.;
    DEEP_DBET = 0.;
    DEEP_DEL1 = 0.;
    DEEP_DEL2 = 0.;
    DEEP_DEL3 = 0.;
    DEEP_DELT = 0.;
    DEEP_DLS = 0.;
    DEEP_E3 = 0.;
    DEEP_EE2 = 0.;
    DEEP_EOC = 0.;
    DEEP_EQ = 0.;
    DEEP_F2 = 0.;
    DEEP_F220 = 0.;
    DEEP_F221 = 0.;
    DEEP_F3 = 0.;
    DEEP_F311 = 0.;
    DEEP_F321 = 0.;
    DEEP_F322 = 0.;
    DEEP_F330 = 0.;
    DEEP_F441 = 0.;
    DEEP_F442 = 0.;
    DEEP_F522 = 0.;
    DEEP_F523 = 0.;
    DEEP_F542 = 0.;
    DEEP_F543 = 0.;
    DEEP_FASX2 = 0.;
    DEEP_FASX4 = 0.;
    DEEP_FASX6 = 0.;
    DEEP_FT = 0.;
    DEEP_G200 = 0.;
    DEEP_G201 = 0.;
    DEEP_G211 = 0.;
    DEEP_G300 = 0.;
    DEEP_G310 = 0.;
    DEEP_G322 = 0.;
    DEEP_G410 = 0.;
    DEEP_G422 = 0.;
    DEEP_G520 = 0.;
    DEEP_G521 = 0.;
    DEEP_G532 = 0.;
    DEEP_G533 = 0.;
    DEEP_GAM = 0.;
    DEEP_OMEGAQ = 0.;
    DEEP_PE = 0.;
    DEEP_PGH = 0.;
    DEEP_PH = 0.;

    /* Initialization of /COMDEEP2/. */

    DEEP_PINC = 0.;
    DEEP_PL = 0.;
    DEEP_PREEP = 0.;
    DEEP_S1 = 0.;
    DEEP_S2 = 0.;
    DEEP_S3 = 0.;
    DEEP_S4 = 0.;
    DEEP_S5 = 0.;
    DEEP_S6 = 0.;
    DEEP_S7 = 0.;
    DEEP_SAVTSN = 0.;
    DEEP_SE = 0.;
    DEEP_SE2 = 0.;
    DEEP_SE3 = 0.;
    DEEP_SEL = 0.;
    DEEP_SES = 0.;
    DEEP_SGH = 0.;
    DEEP_SGH2 = 0.;
    DEEP_SGH3 = 0.;
    DEEP_SGH4 = 0.;
    DEEP_SGHL = 0.;
    DEEP_SGHS = 0.;
    DEEP_SH = 0.;
    DEEP_SH2 = 0.;
    DEEP_SH3 = 0.;
    DEEP_SH1 = 0.;
    DEEP_SHS = 0.;
    DEEP_SI = 0.;
    DEEP_SI2 = 0.;
    DEEP_SI3 = 0.;
    DEEP_SIL = 0.;
    DEEP_SINI2 = 0.;
    DEEP_SINIS = 0.;
    DEEP_SINOK = 0.;
    DEEP_SINQ = 0.;
    DEEP_SINZF = 0.;
    DEEP_SIS = 0.;
    DEEP_SL = 0.;
    DEEP_SL2 = 0.;
    DEEP_SL3 = 0.;
    DEEP_SL4 = 0.;
    DEEP_SLL = 0.;
    DEEP_SLS = 0.;
    DEEP_SSE = 0.;
    DEEP_SSG = 0.;
    DEEP_SSH = 0.;
    DEEP_SSI = 0.;
    DEEP_SSL = 0.;
    DEEP_STEM = 0.;
    DEEP_STEP2 = 0.;
    DEEP_STEPN = 0.;
    DEEP_STEPP = 0.;
    DEEP_TEMP = 0.;
    DEEP_TEMP1 = 0.;
    DEEP_THGR = 0.;
    DEEP_X1 = 0.;
    DEEP_X2 = 0.;
    DEEP_X2LI = 0.;
    DEEP_X2OMI = 0.;
    DEEP_X3 = 0.;
    DEEP_X4 = 0.;
    DEEP_X5 = 0.;
    DEEP_X6 = 0.;
    DEEP_X7 = 0.;
    DEEP_X8 = 0.;
    DEEP_XFACT = 0.;
    DEEP_XGH2 = 0.;
    DEEP_XGH3 = 0.;
    DEEP_XGH4 = 0.;
    DEEP_XH2 = 0.;
    DEEP_XH3 = 0.;
    DEEP_XI2 = 0.;
    DEEP_XI3 = 0.;
    DEEP_XL = 0.;
    DEEP_XL2 = 0.;
    DEEP_XL3 = 0.;
    DEEP_XL4 = 0.;

    /* Initialization of /COMDEEP3/. */

    DEEP_XLAMO = 0.;
    DEEP_XLDOT = 0.;
    DEEP_XLI = 0.;
    DEEP_XLS = 0.;
    DEEP_XMAO = 0.;
    DEEP_XNDDT = 0.;
    DEEP_XNDOT = 0.;
    DEEP_XNI = 0.;
    DEEP_XNO2 = 0.;
    DEEP_XNODCE = 0.;
    DEEP_XNOI = 0.;
    DEEP_XNQ = 0.;
    DEEP_XOMI = 0.;
    DEEP_XPIDOT = 0.;
    DEEP_XQNCL = 0.;
    DEEP_Z1 = 0.;
    DEEP_Z11 = 0.;
    DEEP_Z12 = 0.;
    DEEP_Z13 = 0.;
    DEEP_Z2 = 0.;
    DEEP_Z21 = 0.;
    DEEP_Z22 = 0.;
    DEEP_Z23 = 0.;
    DEEP_Z3 = 0.;
    DEEP_Z31 = 0.;
    DEEP_Z32 = 0.;
    DEEP_Z33 = 0.;
    DEEP_ZCOSG = 0.;
    DEEP_ZCOSGL = 0.;
    DEEP_ZCOSH = 0.;
    DEEP_ZCOSHL = 0.;
    DEEP_ZCOSI = 0.;
    DEEP_ZCOSIL = 0.;
    DEEP_ZE = 0.;
    DEEP_ZF = 0.;
    DEEP_ZM = 0.;
    DEEP_ZMO = 0.;
    DEEP_ZMOL = 0.;
    DEEP_ZMOS = 0.;
    DEEP_ZN = 0.;
    DEEP_ZSING = 0.;
    DEEP_ZSINGL = 0.;
    DEEP_ZSINH = 0.;
    DEEP_ZSINHL = 0.;
    DEEP_ZSINI = 0.;
    DEEP_ZSINIL = 0.;
    DEEP_ZX = 0.;
    DEEP_ZY = 0.;
    DEEP_IRESFL = 0;
    DEEP_ISYNFL = 0;
    DEEP_IRET = 0;
    DEEP_IRETN = 0;
    DEEP_LS = 0;

    /* Initialization of /COMDEEP4/. */

    DEEP_ZNS = 1.19459E-5;
    DEEP_C1SS = 2.9864797E-6;
    DEEP_ZES = 0.01675;
    DEEP_ZNL = 1.5835218E-4;
    DEEP_C1L = 4.7968065E-7;
    DEEP_ZEL = 0.05490;
    DEEP_ZCOSIS = 0.91744867;
    DEEP_ZSINIS = 0.39785416;
    DEEP_ZSINGS = -0.98088458;
    DEEP_ZCOSGS = 0.1945905;
    DEEP_Q22 = 1.7891679E-6;
    DEEP_Q31 = 2.1460748E-6;
    DEEP_Q33 = 2.2123015E-7;
    DEEP_G22 = 5.7686396;
    DEEP_G32 = 0.95240898;
    DEEP_G44 = 1.8014998;
    DEEP_G52 = 1.0508330;
    DEEP_G54 = 4.4108898;
    DEEP_ROOT22 = 1.7891679E-6;
    DEEP_ROOT32 = 3.7393792E-7;
    DEEP_ROOT44 = 7.3636953E-9;
    DEEP_ROOT52 = 1.1428639E-7;
    DEEP_ROOT54 = 2.1765803E-9;
    DEEP_THDT = 4.3752691E-3;

    return;
  }

	private boolean FAST_MODE = false;

  /**
   * Calculate ephemeris for the satellite.
   * The ephemerisElement object is used when transforming to apparent
   * coordinates. In any other case output position is the same
   * (geometric = astrometric).
   *
   * @param time Time object.
   * @param obs Observer object.
   * @param eph Ephemeris object.
   * @return The ephemeris.
 * @throws JPARSECException  If an error occurs.*/
  public SatelliteEphemElement calcSatellite(TimeElement time, ObserverElement obs, EphemerisElement eph) throws JPARSECException
  {
	  return calcSatellite(time, obs, eph, true);
  }

  /**
   * Calculate ephemeris for the satellite.
   * The ephemerisElement object is used when transforming to apparent
   * coordinates. In any other case output position is the same
   * (geometric = astrometric).
   *
   * @param time Time object.
   * @param obs Observer object.
   * @param eph Ephemeris object.
   * @param magAndSize True to calculate magnitude and size, which is a
   * relatively slow operation.
   * @return The ephemeris.
 * @throws JPARSECException  If an error occurs.*/
  public SatelliteEphemElement calcSatellite(TimeElement time, ObserverElement obs, EphemerisElement eph,
		  boolean magAndSize) throws JPARSECException
  {
    double[] TS = new double[1];
    int[] IFLAG = new int[1];

	double JD = TimeScale.getJD(time, obs, eph, SCALE.UNIVERSAL_TIME_UTC);
	boolean exactMode = FastMath.EXACT_MODE;
	FastMath.EXACT_MODE = false;
	if (!FAST_MODE) FastMath.EXACT_MODE = true;

    TS[0] = C1_XMNPDA * (JD - itsEpochJD);

    IFLAG[0] = 1;
    if (!isDeep) {RunSGP4(IFLAG, TS);}
    else                {RunSDP4(IFLAG, TS);}


    // Rest of calculations for topocentric results
	double LAT = obs.getLatitudeRad();
	double LON = obs.getLongitudeRad();
	double HT_km = obs.getHeight() / 1000.0;

	double cosLAT = FastMath.cos(LAT);
	double sinLAT = FastMath.sin(LAT);
	double cosLON = FastMath.cos(LON);
	double sinLON = FastMath.sin(LON);

	ELLIPSOID ellipsoid = obs.getEllipsoid();
	double equatorialRadius = ellipsoid.getEquatorialRadius();
	double flatenning = 1.0 / ellipsoid.getInverseOfFlatteningFactor();
	double polarRadius = equatorialRadius * (1.0 - flatenning);

	double D = FastMath.hypot(equatorialRadius * cosLAT, polarRadius * sinLAT); //Math.sqrt(equatorialRadius * equatorialRadius * cosLAT * cosLAT + polarRadius * polarRadius * sinLAT * sinLAT);
	double Rx = equatorialRadius * equatorialRadius / D + HT_km;
	double Rz = polarRadius * polarRadius / D + HT_km;

	// Observer's unit vectors UP EAST and NORTH in GEOCENTRIC coords.
	double Ux = cosLAT * cosLON;
	double Ex = -sinLON;
	double Nx = -sinLAT * cosLON;

	double Uy = cosLAT * sinLON;
	double Ey = cosLON;
	double Ny = -sinLAT * sinLON;

	double Uz = sinLAT;
	//Ez = 0.0;
	double Nz = cosLAT;

	// Observer's XYZ coords at Earth's surface
	double Ox = Rx * Ux;
	double Oy = Rx * Uy;
	double Oz = Rz * Uz;

	double YT = Constant.TROPICAL_YEAR; // Tropical year, days
	double earthTraslationRate = Constant.TWO_PI / YT; // Earth's traslation rate, rads/whole day
	double earthTraslationRate2 = Constant.TWO_PI + earthTraslationRate; // ditto radians/day
	double W0 = earthTraslationRate2 / Constant.SECONDS_PER_DAY; // ditto radians/sec

	double VOx = -Oy * W0;
	double VOy = Ox * W0; // Observer's velocity, GEOCENTRIC coords. (VOz=0)

    // Parse output from SDP
    double SATx = itsR[0] * 1.0E6;
    double SATy = itsR[1] * 1.0E6;
    double SATz = itsR[2] * 1.0E6;
    double VELx = itsV[0];
    double VELy = itsV[1];
    double VELz = itsV[2];

	double GHAA = jparsec.time.SiderealTime.greenwichMeanSiderealTime(time, obs, eph);
	if (!FAST_MODE) GHAA += jparsec.time.SiderealTime.equationOfEquinoxes(time, obs, eph);

	double C = FastMath.cos(GHAA);
	double S = -FastMath.sin(GHAA);
	double Sx = (SATx * C - SATy * S);
	double Vx = VELx * C - VELy * S;
	double Sy = (SATx * S + SATy * C);
	double Vy = VELx * S + VELy * C;
	double Sz = SATz;
	double Vz = VELz;

	double JD_TDB = 0;
	if (!FAST_MODE) {
		JD_TDB = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		DataBase.addData("GCRS", Precession.precessToJ2000(JD_TDB, new double[] {Sx / Constant.AU, Sy / Constant.AU, Sz / Constant.AU}, eph), true);
	}

	// Compute and manipulate range/velocity/antenna vectors
	Rx = Sx - Ox;
	double Ry = Sy - Oy;
	Rz = Sz - Oz; // Rangevec = Satvec - Obsvec
	double R = Math.sqrt(Rx * Rx + Ry * Ry + Rz * Rz); // Range magnitude
	Rx = Rx / R;
	Ry = Ry / R;
	Rz = Rz / R; // Normalise Range vector
	double U = Rx * Ux + Ry * Uy + Rz * Uz; // UP Component of unit range
	double E = Rx * Ex + Ry * Ey; // EAST do (Ez=0)
	double N = Rx * Nx + Ry * Ny + Rz * Nz; // NORTH do
	double AZI = FastMath.atan2_accurate(E, N); // Azimuth
	double EL = FAST_MODE ? FastMath.asin(U) : Math.asin(U); // Elevation

	// Calculate sub-satellite Lat/Lon
	double RS = Math.sqrt(Sx * Sx + Sy * Sy + Sz * Sz);
	double SLON = FastMath.atan2_accurate(Sy, Sx); // Lon, + East
	double SLAT = FAST_MODE ? FastMath.asin(Sz / RS) : Math.asin(Sz / RS); // Lat, + North
	double HGT = RS - equatorialRadius;

	// Resolve Sat-Obs velocity vector along unit range vector. (VOz=0)
	double RR = (Vx-VOx)*Rx + (Vy-VOy)*Ry + Vz*Rz; // Range rate, km/s

	// Sidereal and Solar data. NEVER needs changing. Valid to year 2000+
	double G0 = 98.9821; // GHAA, Year YG, Jan 0.0
	double MAS0 = 356.0507; // MA Sun and rate, deg, deg/day
	double MASD = 0.98560028; // MA Sun and rate, deg, deg/day
	double EQC1 = 0.03342;
	double EQC2 = 0.00035; // Sun's Equation of centre terms
	double EQC3 = 5.0E-6;
	double year = sat.year;
	double days = sat.day;
	// Convert satellite Epoch to Day No. and Fraction of day
	AstroDate astro = new AstroDate((int) year, 1, (int) Math.floor(days));
	double DE = astro.jd();
	days = days - Math.floor(days);
	double TN = 0.0;
	double DN = JD;
	double T = (DN - DE) + (TN - days); // Elapsed T since epoch, days

	// Revolution number
	double dragCoeff = -2.0 * sat.firstDerivative / (sat.meanMotion * 3.0); // Drag coeff. (Angular momentum rate)/(Ang mom) s^-1
	double DT = dragCoeff * T / 2.0;
	double KDP = 1.0 - 7.0 * DT; // Linear drag terms
	double M = sat.meanAnomaly + sat.meanMotion * T * (1.0 - 3.0 * DT); // Mean anomaly at YR,TN
	double DR = Math.floor(M / Constant.TWO_PI); // Strip out whole no of revs
	double RN = sat.revolutionNumber + DR; // Current Orbit number

	// Bring Sun data to Satellite Epoch
	double TEG = (DE - 2451543.5) + days; // Elapsed Time: Epoch - YG

	double sunMeanRA = Constant.DEG_TO_RAD * G0 + TEG * earthTraslationRate + Math.PI; // Mean RA Sun at Sat epoch
	double sunMeanAnomaly = Constant.DEG_TO_RAD * (MAS0 + MASD * TEG); // Mean MA Sun ..

	// Note other programs (XEphem among them) uses the following lines, which seems to be wrong
	// by 0.004 deg around year 2011. Algorithm at Saros class from Calendrical Calculations agree
	// with previous code up to 0.00001 deg.
	//double Tp = (itsEpochJD - 2415020.0) / 36525.0;
    //double sunMeanAnomaly2 = (358.475845 + 35999.04975 * Tp - 0.00015 * Tp * Tp - 0.00000333333 * Tp * Tp * Tp) * Constant.DEG_TO_RAD;

	double MAS = sunMeanAnomaly + Constant.DEG_TO_RAD * MASD * T; // MA of Sun round its orbit
	MAS = Functions.normalizeRadians(MAS);
	double TAS = sunMeanRA + earthTraslationRate * T + EQC1 * FastMath.sin(MAS) + EQC2 * FastMath.sin(2 * MAS) + EQC3 * FastMath.sin(3 * MAS);
	TAS = Functions.normalizeRadians(TAS);

	double INS = Constant.DEG_TO_RAD * 23.4393;
	double CNS = FastMath.cos(INS);
	double SNS = FastMath.sin(INS); // Sun's inclination
	C = FastMath.cos(TAS);
	S = FastMath.sin(TAS); // Sin/Cos Sun's true anomaly
	double SUNx = C;
	double SUNy = S * CNS;
	double SUNz = S * SNS; // Sun unit vector - CELESTIAL coords

	// Antenna location (for ILL)
	// Fixed (not real) values for the orientation of the satellite antenna
	double ALON = 180.0;
	double ALAT = 0;
	// Average Precession rates
	double GM = 3.98600433e14 * 1.0e-9; // Earth's Gravitational constant g' * R' * R' km^3/s^2, DE405
	double J2 = 0.00108263; // 2nd Zonal coeff, Earth's Gravity Field
	double n = sat.meanMotion / Constant.SECONDS_PER_DAY; // Mean motion rad/s
	double a = Math.pow((GM / (n * n)), 1.0 / 3.0); // Semi major axis km
	double b = a * Math.sqrt(1.0 - sat.eccentricity * sat.eccentricity); // Semi minor axis km. Note 'in astronomy' (comets/asteroids) b=a*(1-e)
	double sinIncl = FastMath.sin(sat.inclination);
	double cosIncl = FastMath.cos(sat.inclination);
	double PC = equatorialRadius * a / (b * b);
	PC = 1.5 * J2 * PC * PC * sat.meanMotion; // Precession const, rad/Day
	double nodePrecessionRate = -PC * cosIncl; // Node precession rate, rad/day
	double perigeePrecessionRate = PC * (5.0 * cosIncl * cosIncl - 1.0) / 2.0; // Perigee precession rate, rad/day
	// Antenna unit vector in orbit plane coordinates.
	cosLON = FastMath.cos(Constant.DEG_TO_RAD * ALON);
	sinLON = FastMath.sin(Constant.DEG_TO_RAD * ALON);
	cosLAT = FastMath.cos(Constant.DEG_TO_RAD * ALAT);
	sinLAT = FastMath.sin(Constant.DEG_TO_RAD * ALAT);
	double Ax = -cosLAT * cosLON;
	double Ay = -cosLAT * sinLON;
	double Az = -sinLAT;
	double correctedArgPerigee = sat.argumentOfPerigee + perigeePrecessionRate * T * KDP;
	double cosCorrectedArgPerigee = FastMath.cos(correctedArgPerigee);
	double sinCorrectedArgPerigee = FastMath.sin(correctedArgPerigee);
	double correctedNodeRA = sat.ascendingNodeRA + nodePrecessionRate * T * KDP;
	double cosCorrectedNodeRA = FastMath.cos(correctedNodeRA);
	double sinCorrectedNodeRA = FastMath.sin(correctedNodeRA);
	// Plane -> celestial coordinate transformation, [C] = [RAAN]*[IN]*[AP]
	double CXx = cosCorrectedArgPerigee * cosCorrectedNodeRA - sinCorrectedArgPerigee * cosIncl * sinCorrectedNodeRA;
	double CXy = -sinCorrectedArgPerigee * cosCorrectedNodeRA - cosCorrectedArgPerigee * cosIncl * sinCorrectedNodeRA;
	double CXz = sinIncl * sinCorrectedNodeRA;
	double CYx = cosCorrectedArgPerigee * sinCorrectedNodeRA + sinCorrectedArgPerigee * cosIncl * cosCorrectedNodeRA;
	double CYy = -sinCorrectedArgPerigee * sinCorrectedNodeRA + cosCorrectedArgPerigee * cosIncl * cosCorrectedNodeRA;
	double CYz = -sinIncl * cosCorrectedNodeRA;
	double CZx = sinCorrectedArgPerigee * sinIncl;
	double CZy = cosCorrectedArgPerigee * sinIncl;
	double CZz = cosIncl;
	double ANTx = Ax * CXx + Ay * CXy + Az * CXz;
	double ANTy = Ax * CYx + Ay * CYy + Az * CYz;
	double ANTz = Ax * CZx + Ay * CZy + Az * CZz;

	// Find Solar angle, elongation, illumination, and eclipse status.
	double SSA = -(ANTx * SUNx + ANTy * SUNy + ANTz * SUNz); // Sin of Sun angle -a.h
	double ILL = Math.sqrt(1.0 - SSA * SSA); // Illumination
//	double SATx = C * (Sx + S * Sy) / (S * S + C * C);
//	double SATy = S * (-Sx + C * Sy) / (S * S + C * C);
//	double SATz = Sz;
//	double ELO = Math.asin(Rx * SUNx + Ry * SUNy + Rz * SUNz); // Calculated later from az/el
	double CUA = -(SATx * SUNx + SATy * SUNy + SATz * SUNz) / RS; // Cos of umbral angle -h.s
	double UMD = RS * Math.sqrt(1.0 - CUA * CUA) / equatorialRadius; // Umbral dist, Earth radii

	String ECL = "Visible at sunset/sunrise";
	if (CUA <= 0.0)
		ECL = "Visible with the sun"; // + for shadow side
	if (UMD <= 1.0 && CUA >= 0.0)
		ECL = "Eclipsed"; // - for sunny side

	// Obtain SUN unit vector in GEOCENTRIC coordinates
	C = FastMath.cos(-GHAA);
	S = FastMath.sin(-GHAA);
	double Hx = SUNx * C - SUNy * S;
	double Hy = SUNx * S + SUNy * C; // If Sun more than 10 deg below horizon
	double Hz = SUNz; // satellite possibly visible

	U = Hx * Ux + Hy * Uy + Hz * Uz;
	E = Hx * Ex + Hy * Ey;
	N = Hx * Nx + Hy * Ny + Hz * Nz;
	double SAZ = FastMath.atan2_accurate(E, N); // Azimuth
	double SEL = FAST_MODE ? FastMath.asin(U) : Math.asin(U); // Elevation

	if ((SEL * Constant.RAD_TO_DEG < -10.0) && !(ECL.equals("Eclipsed")))
		ECL = "Possibly visible";

	double iridiumAngle = SatelliteEphem.IRIDIUM_ANGLE_NOT_APPLICABLE, iridiumAngleMoon = iridiumAngle;
	if (sat.isIridium()) {
		iridiumAngle = SatelliteEphem.iridiumAngle(new double[] {Sx, Sy, Sz}, new double[] {Vx, Vy, Vz},
			new double[] {Sx - Ox, Sy - Oy, Sz - Oz}, new double[] {Hx, Hy, Hz});

		// Obtain Moon iridium angle
		double jdTT = TimeScale.getJD(time, obs, eph, SCALE.TERRESTRIAL_TIME);
		double pos[] = Saros.getMoonPosition(jdTT);
		if (pos[3] < 29.5306-3.0 && pos[3] > 3.0) {
			double toIlluminatedDiskCenter = 0.25 * Constant.DEG_TO_RAD * (pos[3] - 29.53 * 0.5) / 15.0;
			LocationElement locMoon = CoordinateSystem.eclipticToEquatorial(
					new LocationElement(pos[0] + toIlluminatedDiskCenter, pos[1], 1.0), INS, true);
			double locM[] = locMoon.getRectangularCoordinates();
			double MOONx = locM[0], MOONy = locM[1], MOONz = locM[2];

			double Mx = MOONx * C - MOONy * S;
			double My = MOONx * S + MOONy * C;
			double Mz = MOONz;
			iridiumAngleMoon = SatelliteEphem.iridiumAngle(new double[] {Sx, Sy, Sz}, new double[] {Vx, Vy, Vz},
					new double[] {Sx - Ox, Sy - Oy, Sz - Oz}, new double[] {Mx, My, Mz});
		}
	}

	// Obtain Sun unit vector in EQ coordinates
//	Hx =  SUNx*CXx + SUNy*CYx + SUNz*CZx;
//	Hy =  SUNx*CXy + SUNy*CYy + SUNz*CZy;
//	Hz =  SUNx*CXz + SUNy*CYz + SUNz*CZz;

	boolean isEclipsed = false;
	if (ECL.equals("Eclipsed")) isEclipsed = true;
	if (isEclipsed) ILL = 0;

	FastMath.EXACT_MODE = exactMode;

	double ELO = 0;
	if (FAST_MODE) {
		ELO = LocationElement.getApproximateAngularDistance(new LocationElement(SAZ, SEL, 1.0), new LocationElement(AZI, EL, 1.0));
	} else {
		ELO = LocationElement.getAngularDistance(new LocationElement(SAZ, SEL, 1.0), new LocationElement(AZI, EL, 1.0));
	}

	LocationElement loc_horiz = new LocationElement(AZI, EL, R);
	double ast = FAST_MODE ? GHAA + obs.getLongitudeRad() : SiderealTime.apparentSiderealTime(time, obs, eph);
	LocationElement loc_eq = CoordinateSystem.horizontalToEquatorial(loc_horiz, ast, obs.getLatitudeRad(), true);

	if (FAST_MODE) {
		SatelliteEphemElement ephem = new SatelliteEphemElement(sat.getName(), loc_eq.getLongitude(), loc_eq.getLatitude(), R, AZI, EL,
				(float) SLON, (float) SLAT, (float) HGT, (float) RR, (float) ELO, (float) ILL,
				isEclipsed, (int) RN);

		ephem.iridiumAngle = (float) iridiumAngle;
		ephem.iridiumAngleForMoon = (float) iridiumAngleMoon;
		ephem.sunElevation = (float) SEL;
		return ephem;
	}

	// Mean equatorial to true equatorial
	if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT)
	{
		/* Correct nutation */
		double true_eq[] = loc_eq.getRectangularCoordinates();
		true_eq = Nutation.nutateInEquatorialCoordinates(JD_TDB, eph, true_eq, true);
		loc_eq = LocationElement.parseRectangularCoordinates(true_eq);

		/* To apparent elevation */
		loc_horiz = CoordinateSystem.equatorialToHorizontal(loc_eq, ast, obs, eph, true, true);
		//loc_horiz.setLatitude(Ephem.getApparentElevation(eph, obs, loc_horiz.getLatitude(), 5));
	}

	SatelliteEphemElement ephem = new SatelliteEphemElement(sat.getName(), loc_eq.getLongitude(), loc_eq.getLatitude(),
			loc_eq.getRadius(), loc_horiz.getLongitude(), loc_horiz.getLatitude(), (float) SLON, (float) SLAT, (float) HGT, (float) RR, (float) ELO, (float) ILL,
			isEclipsed, (int) RN);

	if (magAndSize) ephem = SatelliteEphem.getMagnitudeAndAngularSize(ephem, sat);
	ephem.iridiumAngle = (float) iridiumAngle;
	ephem.iridiumAngleForMoon = (float) iridiumAngleMoon;
	ephem.sunElevation = (float) SEL;

	// Correct apparent magnitude for extinction
	if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT && eph.correctForExtinction
			&& obs.getMotherBody() == TARGET.EARTH && ephem.magnitude != SatelliteEphemElement.UNKNOWN_MAGNITUDE)
		ephem.magnitude += Star.getExtinction(Constant.PI_OVER_TWO-ephem.elevation, obs.getHeight() / 1000.0, 5);

	return ephem;
  }

	/**
	 * Calculate the ephemeris of a satellite.
	 * <P>
	 * The ephemerisElement object is used when transforming to apparent
	 * coordinates. In any other case output position is the same
	 * (geometric = astrometric). Results are referred to mean equinox
	 * of date.
	 * <P>
	 * A pass is defined as the instant when the satellite is more then 15
	 * degrees above the horizon of the observer. A search for the next pass up
	 * to 7 days after calculation time will be done.
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object. The index of the satellite must be added to the index property.
	 * @param fullEphemeris True for full ephemeris, including next pass time and rise, set, transit.
	 * @return Satellite ephem.
	 * @throws JPARSECException If the method fails, for example because of an
	 *         invalid date.
	 */
	public static SatelliteEphemElement satEphemeris(TimeElement time, ObserverElement obs, EphemerisElement eph,
			boolean fullEphemeris) throws JPARSECException
	{
		// Check Ephemeris object
		if (!EphemerisElement.checkEphemeris(eph))
			throw new JPARSECException("invalid ephemeris object.");

		if (eph.targetBody.getIndex() < 0)
			throw new JPARSECException("invalid target body in ephemeris object.");

		// Obtain object
		SatelliteOrbitalElement sat = SatelliteEphem.getArtificialSatelliteOrbitalElement(eph.targetBody.getIndex());

		// Obtain ephemeris
		SDP4_SGP4 s = new SDP4_SGP4(sat);
		SatelliteEphemElement ephem = s.calcSatellite(time, obs, eph);

		// Obtain next pass time, when the satellite is at least 15 degrees
		// above horizon
		double min_elevation = 15.0 * Constant.DEG_TO_RAD;
		if (fullEphemeris) {
			ephem.nextPass = getNextPass(time, obs, eph, sat, min_elevation, 7, true);
			if (ephem.nextPass != 0.0)
				ephem = SDP4_SGP4.getCurrentOrNextRiseSetTransit(time, obs, eph, ephem, 34.0 * Constant.DEG_TO_RAD / 60.0);
		}

		return ephem;
	}

	/**
	 * Calculate the ephemeris of a satellite.
	 * <P>
	 * The ephemerisElement object is used when transforming to apparent
	 * coordinates. In any other case output position is the same
	 * (geometric = astrometric). Results are referred to mean equinox
	 * of date.
	 * <P>
	 * A pass is defined as the instant when the satellite is more then 15
	 * degrees above the horizon of the observer. A search for the next pass up
	 * to 7 days after calculation time will be done.
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object. The index of the satellite must be added to the index property.
	 * @param fullEphemeris True for full ephemeris, including next pass time and rise, set, transit.
	 * @param magAndSize True to calculate magnitude and size, which is a
	 * relatively slow operation.
	 * @return Satellite ephem.
	 * @throws JPARSECException If the method fails, for example because of an
	 *         invalid date.
	 */
	public static SatelliteEphemElement satEphemeris(TimeElement time, ObserverElement obs, EphemerisElement eph,
			boolean fullEphemeris, boolean magAndSize) throws JPARSECException
	{
		// Check Ephemeris object
		if (!EphemerisElement.checkEphemeris(eph))
			throw new JPARSECException("invalid ephemeris object.");

		if (eph.targetBody.getIndex() < 0)
			throw new JPARSECException("invalid target body in ephemeris object.");

		// Obtain object
		SatelliteOrbitalElement sat = SatelliteEphem.getArtificialSatelliteOrbitalElement(eph.targetBody.getIndex());

		// Obtain ephemeris
		SDP4_SGP4 s = new SDP4_SGP4(sat);
		SatelliteEphemElement ephem = s.calcSatellite(time, obs, eph, magAndSize);

		// Obtain next pass time, when the satellite is at least 15 degrees
		// above horizon
		double min_elevation = 15.0 * Constant.DEG_TO_RAD;
		if (fullEphemeris) {
			ephem.nextPass = getNextPass(time, obs, eph, sat, min_elevation, 7, true);
			if (ephem.nextPass != 0.0)
				ephem = SDP4_SGP4.getCurrentOrNextRiseSetTransit(time, obs, eph, ephem, 34.0 * Constant.DEG_TO_RAD / 60.0);
		}

		return ephem;
	}

	// Returns the approximate time in days required for a given satellite to move from
	// one side to the other side of the sky from a given observer.
	private static double getBestQuickSearch(SatelliteOrbitalElement sat, double minElev) {
		double GM = 3.98600433e14 * 1.0e-9; // Earth's Gravitational constant g' * R' * R'
		double n = sat.meanMotion / Constant.SECONDS_PER_DAY; // Mean motion rad/s
		double a = Math.pow((GM / (n * n)), 1.0 / 3.0); // Semi major axis km
		double ecc = sat.eccentricity;
		double b = a * Math.sqrt(1.0 - ecc * ecc); // Semi minor axis km

		double r = (a + b) / 2.0 - Constant.EARTH_RADIUS;
		double ang = Constant.PI_OVER_TWO - 2.0 * minElev;
		double dr = ang * r;
		double drDay = Constant.TWO_PI * (r + Constant.EARTH_RADIUS);
		double dt = dr * Constant.SECONDS_PER_DAY / drDay;
		return dt / Constant.SECONDS_PER_DAY; // days
	}

	/**
	 * Obtain the time of the next pass of the satellite above observer. It can be used
	 * as an starting point prior to obtain rise, set, transit times.
	 * <P>
	 * A pass is defined as an instant when the elevation of the satellite is
	 * greater than certain minimum value. If the observer has a perfect sight
	 * of the horizon, it is possible to set a value equal to zero, but the
	 * satellite will be probably too faint.
	 * <P>
	 * The pass is a search iteration with a precision of 1 minute of time. If
	 * the satellite appears too quickly or just above minimum elevation only
	 * for a few seconds, then the search could fail. Another possible cause
	 * of fail is for geostationary satellites.
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param sat Satellite orbital elements.
	 * @param min_elevation Minimum elevation in radians.
	 * @param maxDays Maximum number of days to search for a next pass.
	 * @param current True to return the input time if the satellite is above the minimum
	 * elevation, false to return next pass without considering the actual position of the satellite.
	 * @return Julian day of the next pass in local time, or 0.0 if the satellite
	 * has no next transit. If the day is negative that means that the satellite is
	 * eclipsed during the next pass.
	 * @throws JPARSECException If the method fails, for example because of an
	 *         invalid date.
	 */
	public static double getNextPass(TimeElement time, ObserverElement obs, EphemerisElement eph,
			SatelliteOrbitalElement sat, double min_elevation, double maxDays, boolean current) throws JPARSECException
	{
		// Check Ephemeris object
		if (!EphemerisElement.checkEphemeris(eph))
			throw new JPARSECException("invalid ephemeris object.");

		if (min_elevation < 0.0 || min_elevation >= Math.PI*0.5) throw new JPARSECException("invalid minimum elevation.");

		// Obtain ephemeris
		SDP4_SGP4 s = new SDP4_SGP4(sat);
		s.FAST_MODE = true;
		SatelliteEphemElement ephem = s.calcSatellite(time, obs, eph, false);

		// Obtain Julian day in reference scale
		SCALE refScale = SCALE.UNIVERSAL_TIME_UTC;
		double JD = TimeScale.getJD(time, obs, eph, refScale);
		double JD_LT = TimeScale.getJD(time, obs, eph, SCALE.LOCAL_TIME);

		// Set time step to 0.5 minutes
		double time_step = 0.5 / (Constant.MINUTES_PER_HOUR * Constant.HOURS_PER_DAY);
		int nstep = 0;
		int max_step = (int) Math.floor(maxDays / time_step);
		double qs = getBestQuickSearch(sat, min_elevation) / time_step;
		int quickSearch = (int) (0.5 + qs / 2.0);
		if (quickSearch < 1) quickSearch = 1;
		if (quickSearch > 8) quickSearch = 8;

		// Obtain next pass. First we obtain the time when the satellite is
		// below the minimum elevation (necessary if it is currently above). Then, we
		// obtain the next pass
		while (ephem.elevation > min_elevation && nstep < max_step && !current)
		{
			nstep++;
			double new_JD = JD + (double) nstep * time_step;

			TimeElement new_time = new TimeElement(new_JD, refScale);

			ephem = s.calcSatellite(new_time, obs, eph, false);
		}

		if (nstep >= max_step) {
//			JPARSECException.addWarning("this satellite is permanently above the horizon and the minimum elevation.");
			return 0.0;
		}

		while (ephem.elevation < min_elevation && nstep < max_step)
		{
			if (ephem.elevation < -25.0 * Constant.DEG_TO_RAD) {
				nstep = nstep + quickSearch;
			} else {
				if (ephem.elevation < -15.0 * Constant.DEG_TO_RAD) {
					int bqs = quickSearch / 2;
					if (bqs < 1) bqs = 1;
					nstep = nstep + bqs;
				} else {
					int bqs = quickSearch / 4;
					if (bqs < 1 || ephem.elevation > 0) bqs = 1;
					nstep = nstep + bqs;
				}
			}
			double new_JD = JD + (double) nstep * time_step;

			TimeElement new_time = new TimeElement(new_JD, refScale);

			ephem = s.calcSatellite(new_time, obs, eph, false);
		}

		while (ephem.elevation > min_elevation && nstep < max_step)
		{
			nstep--;
			double new_JD = JD + (double) nstep * time_step;

			TimeElement new_time = new TimeElement(new_JD, refScale);

			ephem = s.calcSatellite(new_time, obs, eph, false);
		}

		double next_pass = JD_LT + nstep * time_step;

		if (next_pass >= JD_LT + maxDays) {
//			JPARSECException.addWarning("could not find next pass time during next "+maxDays+" days.");
			next_pass = 0.0;
		}

		if (ephem.isEclipsed) next_pass = -next_pass;

		return next_pass;
	}

	/**
	 * Obtain all transits of a given satellite on top of the Sun or the Moon.
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param sat Satellite orbital elements.
	 * @param maxDays Maximum number of days to search for a next pass.
	 * @param minDist Minimum distance in degrees to consider that there is a transit
	 * on top of the Sun or Moon. Typical value is 0.25 degrees, but you can set it to a
	 * higher value to return more transits (although you will have to move from the 
	 * observing site set to have a change to see it).
	 * @return An array of event objects with the type of transit (on the Sun or the
	 * Moon) using the secondary object field, and with the initial and ending transit times.
	 * The details field will contain the elevation above the horizon, always &gt;= 0.
	 * Precision is 0.5s.
	 * @throws JPARSECException If the method fails, for example because of an
	 *         invalid date.
	 */
	public static ArrayList<SimpleEventElement> getNextSunOrMoonTransits(TimeElement time, ObserverElement obs, EphemerisElement eph,
			SatelliteOrbitalElement sat, double maxDays, double minDist) throws JPARSECException
	{
		// Check Ephemeris object
		if (!EphemerisElement.checkEphemeris(eph))
			throw new JPARSECException("invalid ephemeris object.");

		double min_elevation = 0;
		boolean current = true;
		ArrayList<SimpleEventElement> out = new ArrayList<SimpleEventElement>();
		double JD = TimeScale.getJD(time, obs, eph, SCALE.UNIVERSAL_TIME_UTC);
		TimeElement new_time = new TimeElement(JD, SCALE.UNIVERSAL_TIME_UTC);
		double jdF = maxDays + JD;
		double time_step = 0.5 / Constant.SECONDS_PER_DAY;

		String eclipsed = Translate.translate(163).toLowerCase();
		while (true) {
			maxDays = jdF - new_time.astroDate.jd();
			double next_pass = getNextPass(new_time, obs, eph, sat, min_elevation, maxDays, current);
			if (next_pass == 0) break;
			current = false;

			// Obtain ephemeris
			SDP4_SGP4 s = new SDP4_SGP4(sat);
			s.FAST_MODE = true;
			SatelliteEphemElement ephem = s.calcSatellite(time, obs, eph, false);

			// Obtain Julian day in reference scale
			new_time = new TimeElement(Math.abs(next_pass), SCALE.LOCAL_TIME);
			JD = TimeScale.getJD(new_time, obs, eph, SCALE.UNIVERSAL_TIME_UTC);
			double JD_TT = TimeScale.getJD(new_time, obs, eph, SCALE.TERRESTRIAL_TIME);
			new_time = new TimeElement(JD, SCALE.UNIVERSAL_TIME_UTC);

			int nstep = 0;
			boolean insideSun = false, insideMoon = false;
			double obl = Obliquity.meanObliquity(Functions.toCenturies(JD_TT), eph) +
					Nutation.getFastNutation(JD_TT)[1];

			while (ephem.elevation > min_elevation || nstep == 0)
			{
				nstep ++;
				double new_JD = JD + (double) nstep * time_step;

				new_time = new TimeElement(new_JD, SCALE.UNIVERSAL_TIME_UTC);

				ephem = s.calcSatellite(new_time, obs, eph, false);

				LocationElement loc = CoordinateSystem.equatorialToEcliptic(ephem.getEquatorialLocation(), obl, true);
				double sun[] = Saros.getSunPosition(JD_TT + (double) nstep * time_step);
				double moon[] = Saros.getMoonPosition(JD_TT + (double) nstep * time_step);

				double dSun = LocationElement.getAngularDistance(loc, new LocationElement(sun[0], sun[1], 1.0)) * Constant.RAD_TO_DEG;
				double dMoon = LocationElement.getAngularDistance(loc, new LocationElement(moon[0], moon[1], 1.0)) * Constant.RAD_TO_DEG;

				if (dSun < minDist && !insideSun) {
					String det = Functions.formatAngleAsDegrees(ephem.elevation, 1)+"\u00b0";
					if (ephem.isEclipsed) det += ", "+eclipsed;
					SimpleEventElement see = new SimpleEventElement(JD_TT + (double) nstep * time_step,
							EVENT.ARTIFICIAL_SATELLITES_TRANSITS_SUN_MOON, det);
					see.body = sat.name;
					see.secondaryBody = TARGET.SUN.getName();
					see.eventLocation = ephem.getEquatorialLocation();
					out.add(see);
					insideSun = true;
				} else {
					if (insideSun && dSun >= minDist) {
						insideSun = false;
						SimpleEventElement see = out.get(out.size()-1);
						if (see.secondaryBody.equals(TARGET.SUN.getName()))
							see.endTime = JD_TT + (nstep - 1.0) * time_step;
					}
				}
				if (dMoon < minDist && !insideMoon) {
					String det = Functions.formatAngleAsDegrees(ephem.elevation, 1)+"\u00b0";
					if (ephem.isEclipsed) det += ", "+eclipsed;
					SimpleEventElement see = new SimpleEventElement(JD_TT + (double) nstep * time_step,
							EVENT.ARTIFICIAL_SATELLITES_TRANSITS_SUN_MOON, det);
					see.body = sat.name;
					see.secondaryBody = TARGET.Moon.getName();
					see.eventLocation = ephem.getEquatorialLocation();
					out.add(see);
					insideMoon = true;
				} else {
					if (insideMoon && dMoon >= minDist) {
						insideMoon = false;
						SimpleEventElement see = out.get(out.size()-1);
						if (see.secondaryBody.equals(TARGET.Moon.getName()))
							see.endTime = JD_TT + (nstep - 1.0) * time_step;
					}
				}
				double min = Math.min(dMoon, dSun);
				if (min > 5 && !insideMoon && !insideSun) nstep += (int) (min / (time_step * Constant.SECONDS_PER_DAY));
				if ((insideSun || insideMoon) && ephem.elevation <= min_elevation && out.size() > 0) out.remove(out.size()-1);
			}
		}

		return out;
	}

	/**
	 * Obtain all transits of a given satellite on top of any planet (Sun or Moon excluded).
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param sat Satellite orbital elements.
	 * @param maxDays Maximum number of days to search for a next pass.
	 * @param minDist Minimum distance in degrees, in addition to the radius of the planet, 
	 * to consider that there is a transit on top of the planet. Strict value should be 0.
	 * @return An array of event objects with the type of transit (target body)
	 * set as the secondary object field, and with the initial and ending transit times.
	 * The details field will contain the elevation above the horizon, always &gt;= 0.
	 * Precision is 0.5s.
	 * @throws JPARSECException If the method fails, for example because of an
	 *         invalid date.
	 */
	public static ArrayList<SimpleEventElement> getNextPlanetTransits(TimeElement time, ObserverElement obs, EphemerisElement eph,
			SatelliteOrbitalElement sat, double maxDays, double minDist) throws JPARSECException
	{
		// Check Ephemeris object
		if (!EphemerisElement.checkEphemeris(eph))
			throw new JPARSECException("invalid ephemeris object.");

		double min_elevation = 0;
		boolean current = true;
		ArrayList<SimpleEventElement> out = new ArrayList<SimpleEventElement>();
		double JD = TimeScale.getJD(time, obs, eph, SCALE.UNIVERSAL_TIME_UTC);
		TimeElement new_time = new TimeElement(JD, SCALE.UNIVERSAL_TIME_UTC);
		double jdF = maxDays + JD;
		double time_step = 0.5 / Constant.SECONDS_PER_DAY;

		String eclipsed = Translate.translate(163).toLowerCase();
		EphemElement pephem[] = new EphemElement[7];
		double pephemDate[] = new double[7];
		TARGET ptar[] = new TARGET[] {TARGET.MERCURY, TARGET.VENUS, TARGET.MARS, 
				TARGET.JUPITER, TARGET.SATURN, TARGET.URANUS, TARGET.NEPTUNE};
		double ptol[] = new double[] {20/Constant.SECONDS_PER_DAY, 60/Constant.SECONDS_PER_DAY, 
				160/Constant.SECONDS_PER_DAY, 900/Constant.SECONDS_PER_DAY, 2500/Constant.SECONDS_PER_DAY, 
				4000/Constant.SECONDS_PER_DAY, 7000/Constant.SECONDS_PER_DAY};
		double dplan[] = new double[7];
		boolean pinside[] = new boolean[7];
		EphemerisElement peph = eph.clone();
		if (peph.algorithm == ALGORITHM.ARTIFICIAL_SATELLITE) peph.algorithm = ALGORITHM.MOSHIER;
		peph.optimizeForSpeed();
		
		while (true) {
			maxDays = jdF - new_time.astroDate.jd();
			double next_pass = getNextPass(new_time, obs, eph, sat, min_elevation, maxDays, current);
			if (next_pass == 0) break;
			current = false;

			// Obtain ephemeris
			SDP4_SGP4 s = new SDP4_SGP4(sat);
			s.FAST_MODE = true;
			SatelliteEphemElement ephem = s.calcSatellite(time, obs, eph, false);

			// Obtain Julian day in reference scale
			new_time = new TimeElement(Math.abs(next_pass), SCALE.LOCAL_TIME);
			JD = TimeScale.getJD(new_time, obs, eph, SCALE.UNIVERSAL_TIME_UTC);
			double JD_TT = TimeScale.getJD(new_time, obs, eph, SCALE.TERRESTRIAL_TIME);
			new_time = new TimeElement(JD, SCALE.UNIVERSAL_TIME_UTC);

			int nstep = 0;
			boolean insidePlan = false;
			for (int t=0; t<ptar.length; t++) {	pinside[t] = false; }

			while (ephem.elevation > min_elevation || nstep == 0)
			{
				nstep ++;
				double new_JD = JD + (double) nstep * time_step;

				new_time = new TimeElement(new_JD, SCALE.UNIVERSAL_TIME_UTC);

				ephem = s.calcSatellite(new_time, obs, eph, false);

				LocationElement loc = ephem.getEquatorialLocation();
				for (int t=0; t<ptar.length; t++) {
					if (pephem[t] == null || Math.abs(pephemDate[t]-new_JD) > ptol[t]) {
						peph.targetBody = ptar[t];
						pephem[t] = Ephem.getEphemeris(new_time, obs, peph, false);
						pephemDate[t] = new_JD;
					}
					dplan[t] = LocationElement.getAngularDistance(loc, pephem[t].getEquatorialLocation()) * Constant.RAD_TO_DEG;
					
					double md = pephem[t].angularRadius * Constant.RAD_TO_DEG + minDist;
					if (dplan[t] < md && !pinside[t]) {
						String det = Functions.formatAngleAsDegrees(ephem.elevation, 1)+"\u00b0";
						if (ephem.isEclipsed) det += ", "+eclipsed;
						SimpleEventElement see = new SimpleEventElement(JD_TT + (double) nstep * time_step, 
								EVENT.ARTIFICIAL_SATELLITES_TRANSITS, det);
						see.body = sat.name;
						see.secondaryBody = ptar[t].getName();
						see.eventLocation = ephem.getEquatorialLocation();
						out.add(see);
						pinside[t] = true;
					} else {
						if (pinside[t] && dplan[t] >= md) {
							pinside[t] = false;
							SimpleEventElement see = out.get(out.size()-1);
							if (see.secondaryBody.equals(ptar[t].getName()))
								see.endTime = JD_TT + (nstep - 1.0) * time_step;
						}
					}
				}
				insidePlan = false;
				for (int t=0; t<ptar.length; t++) {	if (pinside[t]) insidePlan = true; }
				double min = DataSet.getMinimumValue(dplan);
				if (min > 5 && !insidePlan) nstep += (int) (min / (time_step * Constant.SECONDS_PER_DAY));
				if (insidePlan && ephem.elevation <= min_elevation && out.size() > 0) out.remove(out.size()-1);
			}
		}

		return out;
	}

	/**
	 * Obtain the time of the next flares of the satellite above observer. This
	 * method calls {@linkplain SDP4_SGP4#getNextPass(TimeElement, ObserverElement, EphemerisElement, SatelliteOrbitalElement, double, double, boolean)}
	 * and then checks for the flaring conditions. The returned array contains as the
	 * three latest objects the ephemerides for the satellite when the flare starts, ends, and
	 * reaches its maximum. In these objects the apparent magnitude expected for the flare is
	 * set to the magnitude field, and it is corrected for extinction.
	 *
	 * The field {@linkplain SatelliteEphem#MAXIMUM_IRIDIUM_ANGLE_FOR_FLARES} sets the sensitivty when
	 * searching for more or less bright flares.
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param ephIn Ephemeris object.
	 * @param sat Satellite orbital elements. Should be obviosly an IRIDIUM satellite.
	 * @param min_elevation Minimum elevation of the satellite in radians.
	 * @param maxDays Maximum number of days to search for a next flare.
	 * @param current True to return the input time if the satellite is above the minimum
	 * elevation and flaring, false to return next flare without considering the actual
	 * position of the satellite.
	 * @param precision Precision in the search for events in seconds. The more the value you enter here,
	 * the faster the calculations will be, but some of the events could be skipped. A good value is
	 * 5, which means that flares that last for less than 5s could be missed. This value must be between
	 * 1 and 10. The output precision of the found flares will be always 1s.
	 * @return An array list with all the events for this satellite. The list will be null
	 * if the satellite has no next flare during the number of days given. Otherwise, it will
	 * contains arrays of double values with the Julian day of the beggining of the next flare
	 * in local time, the Julian day of the ending time of the flare, the Julian day of the
	 * maximum of the flare, and the minimum iridium angle as fourth value. The fifth, sixth,
	 * and seventh values will be respectivelly the {@linkplain SatelliteEphemElement} object
	 * for the start, end, and maximum times. No check is done
	 * for flares during day or night, although it is easy to provide a time object for sunset
	 * and a maximum number of days of 0.5 or the required value for sunrise. Precision in
	 * returned times is 1 second, and they consider the minimum elevation so that the start/end
	 * times could reflect the instants when the satellite reaches the minimum elevation (or
	 * is eclipsed) and not the start/end times of the flare.
	 * @throws JPARSECException If the method fails, for example because of an
	 *         invalid date.
	 */
	public static ArrayList<Object[]> getNextIridiumFlares(TimeElement time, ObserverElement obs, EphemerisElement ephIn,
			SatelliteOrbitalElement sat, double min_elevation, double maxDays, boolean current, int precision) throws JPARSECException
	{
		if (sat == null || sat.name.toLowerCase().indexOf("dummy")>=0) return null;
		if (precision < 1 || precision > 10) throw new JPARSECException("Precision parameters is "+precision+", which is outside range 1-10.");

		ArrayList<Object[]> events = null;
		SCALE refScale = SCALE.UNIVERSAL_TIME_UTC;
		EphemerisElement eph = ephIn.clone();
		eph.optimizeForSpeed();
		double inputJD = TimeScale.getJD(time, obs, eph, refScale);
		double inputJD_LT = TimeScale.getJD(time, obs, eph, SCALE.LOCAL_TIME);
		double limitJD = inputJD + maxDays;
		double limitJD_LT = inputJD_LT + maxDays;
		double jd = inputJD, jdOut = 0.0;
		while (jd < limitJD && jd != 0.0) {
			TimeElement newTime = new TimeElement(jd, refScale);
			maxDays = limitJD - jd;
			jd = SDP4_SGP4.getNextPass(newTime, obs, eph, sat, min_elevation, maxDays, current);
			jd = Math.abs(jd); // <0 => eclipsed, but this limitation should be set at the end only if the sat is eclipsed
			if (jd > 0.0 && jd < limitJD_LT) {
	 			jd = TimeScale.getJD(new TimeElement(Math.abs(jd), SCALE.LOCAL_TIME), obs, eph, refScale);
				current = false;

				jdOut = jd;
				SDP4_SGP4 s = new SDP4_SGP4(sat);
				s.FAST_MODE = true;
				newTime = new TimeElement(jdOut, refScale);
				SatelliteEphemElement ephem = s.calcSatellite(newTime, obs, eph);
				//if (!ephem.isEclipsed) { // this limitation should be set at the end only if the sat is eclipsed
					// Check iridium angle second by second until flare ends, minimum elevation, or eclipse
					double startTime = 0.0, endTime = 0.0, maxTime = 0.0, minimumIA = 0.0;
					SatelliteEphemElement start = null, end = null, max = null;
					boolean above = false;
					if (ephem.iridiumAngle <= SatelliteEphem.MAXIMUM_IRIDIUM_ANGLE_FOR_FLARES)	{
						ephem.magnitude = (float) SatelliteEphem.getIridiumFlareMagnitude(ephem, obs);
						startTime = jdOut;
						minimumIA = ephem.iridiumAngle;
						maxTime = jdOut;
						start = ephem;
						max = ephem;
						end = ephem;
						endTime = jdOut;
					}

					boolean found = false;
					do {
						jdOut += precision / Constant.SECONDS_PER_DAY;
						newTime = new TimeElement(jdOut, refScale);
						ephem = s.calcSatellite(newTime, obs, eph);
						if (ephem.elevation > min_elevation) above = true;
						if (above && ephem.iridiumAngle <= SatelliteEphem.MAXIMUM_IRIDIUM_ANGLE_FOR_FLARES) {
							found = true;
							break;
						}
						if (ephem.elevation < min_elevation && above) break;
					} while (true);

					if (found) {
						jdOut -= precision / Constant.SECONDS_PER_DAY;
						above = false;
						do {
							jdOut += 1.0 / Constant.SECONDS_PER_DAY;
							newTime = new TimeElement(jdOut, refScale);
							ephem = s.calcSatellite(newTime, obs, eph);
							ephem.magnitude = (float) SatelliteEphem.getIridiumFlareMagnitude(ephem, obs);
							if (ephem.elevation > min_elevation) above = true;

							if (ephem.iridiumAngle <= SatelliteEphem.MAXIMUM_IRIDIUM_ANGLE_FOR_FLARES && startTime == 0.0) {
					 			if (ephem.elevation < min_elevation || ephem.isEclipsed) {
									startTime = 0.0;
									break;
								}
					 			startTime = TimeScale.getJD(newTime, obs, eph, SCALE.LOCAL_TIME);
								start = ephem;
								max = ephem;
								end = ephem;
								minimumIA = ephem.iridiumAngle;
								maxTime = startTime;
								endTime = startTime;
							}
							if ((ephem.iridiumAngle > SatelliteEphem.MAXIMUM_IRIDIUM_ANGLE_FOR_FLARES || ephem.elevation < min_elevation || ephem.isEclipsed) && startTime != 0.0) {
								endTime = TimeScale.getJD(newTime, obs, eph, SCALE.LOCAL_TIME);
								end = ephem;
								break;
							}
							if (startTime != 0.0 && ephem.iridiumAngle < minimumIA) {
								minimumIA = ephem.iridiumAngle;
								maxTime = TimeScale.getJD(newTime, obs, eph, SCALE.LOCAL_TIME);
								max = ephem;
							}
							if (ephem.elevation < min_elevation && above) {
								startTime = 0.0;
								break;
							}
						} while (true);
					}

					jd = jdOut;
					if (startTime != 0.0) {
						if (!max.isEclipsed) {
							if (events == null) events = new ArrayList<Object[]>();
							events.add(new Object[] {startTime, endTime, maxTime, minimumIA, start, end, max});
						}
					}
				//}
			}
			jd = Math.abs(jd);
			if (jd != 0.0) jd += 10.0 / 1440.0;
		}
		return events;
	}

	/**
	 * Obtain the time of the next lunar flares of the satellite above observer. This
	 * method calls {@linkplain SDP4_SGP4#getNextPass(TimeElement, ObserverElement, EphemerisElement, SatelliteOrbitalElement, double, double, boolean)}
	 * and then checks for the flaring conditions. The returned array contains as the
	 * three latest objects the ephemerides for the satellite when the flare starts, ends, and
	 * reaches its maximum. In these objects the apparent magnitude expected for the flare is
	 * set to the magnitude field, and it is corrected for extinction.
	 *
	 * The field {@linkplain SatelliteEphem#MAXIMUM_IRIDIUM_ANGLE_FOR_LUNAR_FLARES} sets the sensitivty when
	 * searching for more or less bright lunar flares.
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param ephIn Ephemeris object.
	 * @param sat Satellite orbital elements. Should be obviosly an IRIDIUM satellite.
	 * @param min_elevation Minimum elevation of the satellite in radians.
	 * @param maxDays Maximum number of days to search for a next flare.
	 * @param current True to return the input time if the satellite is above the minimum
	 * elevation and flaring, false to return next flare without considering the actual
	 * position of the satellite.
	 * @param precision Precision in the search for events in seconds. The more the value you enter here,
	 * the faster the calculations will be, but some of the events could be skipped. A good value is
	 * 5, which means that flares that last for less than 5s could be missed. This value must be between
	 * 1 and 10. The output precision of the found flares will be always 1s.
	 * @return An array list with all the events for this satellite. The list will be null
	 * if the satellite has no next flare during the number of days given. Otherwise, it will
	 * contains arrays of double values with the Julian day of the beggining of the next flare
	 * in local time, the Julian day of the ending time of the flare, the Julian day of the
	 * maximum of the flare, and the minimum iridium angle as fourth value. The fifth, sixth,
	 * and seventh values will be respectivelly the {@linkplain SatelliteEphemElement} object
	 * for the start, end, and maximum times. No check is done
	 * for flares during day or night, although it is easy to provide a time object for sunset
	 * and a maximum number of days of 0.5 or the required value for sunrise. Precision in
	 * returned times is 1 second, and they consider the minimum elevation so that the start/end
	 * times could reflect the instants when the satellite reaches the minimum elevation (or
	 * is eclipsed) and not the start/end times of the flare.
	 * @throws JPARSECException If the method fails, for example because of an
	 *         invalid date.
	 */
	public static ArrayList<Object[]> getNextIridiumLunarFlares(TimeElement time, ObserverElement obs, EphemerisElement ephIn,
			SatelliteOrbitalElement sat, double min_elevation, double maxDays, boolean current, int precision) throws JPARSECException
	{
		if (sat == null || sat.name.toLowerCase().indexOf("dummy")>=0) return null;
		if (precision < 1 || precision > 10) throw new JPARSECException("Precision parameters is "+precision+", which is outside range 1-10.");

		ArrayList<Object[]> events = null;
		SCALE refScale = SCALE.UNIVERSAL_TIME_UTC;
		EphemerisElement eph = ephIn.clone();
		eph.optimizeForSpeed();
		double inputJD = TimeScale.getJD(time, obs, eph, refScale);
		double inputJD_LT = TimeScale.getJD(time, obs, eph, SCALE.LOCAL_TIME);
		double limitJD = inputJD + maxDays;
		double limitJD_LT = inputJD_LT + maxDays;
		double jd = inputJD, jdOut = 0.0;
		while (jd < limitJD && jd != 0.0) {
			TimeElement newTime = new TimeElement(jd, refScale);
			maxDays = limitJD - jd;
			jd = SDP4_SGP4.getNextPass(newTime, obs, eph, sat, min_elevation, maxDays, current);
			jd = Math.abs(jd); // <0 => eclipsed, but this limitation should be set at the end only if the sat is eclipsed
			if (jd > 0.0 && jd < limitJD_LT) {
	 			jd = TimeScale.getJD(new TimeElement(Math.abs(jd), SCALE.LOCAL_TIME), obs, eph, refScale);
				current = false;

				jdOut = jd;
				SDP4_SGP4 s = new SDP4_SGP4(sat);
				s.FAST_MODE = true;
				newTime = new TimeElement(jdOut, refScale);
				SatelliteEphemElement ephem = s.calcSatellite(newTime, obs, eph);
				//if (!ephem.isEclipsed) { // this limitation should be set at the end only if the sat is eclipsed
					// Check iridium angle second by second until flare ends, minimum elevation, or eclipse
					double startTime = 0.0, endTime = 0.0, maxTime = 0.0, minimumIA = 0.0;
					SatelliteEphemElement start = null, end = null, max = null;
					boolean above = false;
					if (ephem.iridiumAngleForMoon <= SatelliteEphem.MAXIMUM_IRIDIUM_ANGLE_FOR_LUNAR_FLARES)	{
						ephem.magnitude = (float) SatelliteEphem.getIridiumLunarFlareMagnitude(newTime, obs, eph, ephem);
						startTime = jdOut;
						minimumIA = ephem.iridiumAngleForMoon;
						maxTime = jdOut;
						start = ephem;
						max = ephem;
						end = ephem;
						endTime = jdOut;
					}

					boolean found = false;
					do {
						jdOut += precision / Constant.SECONDS_PER_DAY;
						newTime = new TimeElement(jdOut, refScale);
						ephem = s.calcSatellite(newTime, obs, eph);
						if (ephem.elevation > min_elevation) above = true;
						if (above && ephem.iridiumAngleForMoon <= SatelliteEphem.MAXIMUM_IRIDIUM_ANGLE_FOR_LUNAR_FLARES) {
							found = true;
							break;
						}
						if (ephem.elevation < min_elevation && above) break;
					} while (true);

					if (found) {
						jdOut -= precision / Constant.SECONDS_PER_DAY;
						above = false;
						do {
							jdOut += 1.0 / Constant.SECONDS_PER_DAY;
							newTime = new TimeElement(jdOut, refScale);
							ephem = s.calcSatellite(newTime, obs, eph);
							ephem.magnitude = (float) SatelliteEphem.getIridiumLunarFlareMagnitude(newTime, obs, eph, ephem);
							if (ephem.elevation > min_elevation) above = true;

							if (ephem.iridiumAngleForMoon <= SatelliteEphem.MAXIMUM_IRIDIUM_ANGLE_FOR_LUNAR_FLARES && startTime == 0.0) {
					 			if (ephem.elevation < min_elevation || ephem.isEclipsed) {
									startTime = 0.0;
									break;
								}
					 			startTime = TimeScale.getJD(newTime, obs, eph, SCALE.LOCAL_TIME);
								start = ephem;
								max = ephem;
								end = ephem;
								minimumIA = ephem.iridiumAngleForMoon;
								maxTime = startTime;
								endTime = startTime;
							}
							if ((ephem.iridiumAngleForMoon > SatelliteEphem.MAXIMUM_IRIDIUM_ANGLE_FOR_LUNAR_FLARES || ephem.elevation < min_elevation || ephem.isEclipsed) && startTime != 0.0) {
								endTime = TimeScale.getJD(newTime, obs, eph, SCALE.LOCAL_TIME);
								end = ephem;
								break;
							}
							if (startTime != 0.0 && ephem.iridiumAngleForMoon < minimumIA) {
								minimumIA = ephem.iridiumAngleForMoon;
								maxTime = TimeScale.getJD(newTime, obs, eph, SCALE.LOCAL_TIME);
								max = ephem;
							}
							if (ephem.elevation < min_elevation && above) {
								startTime = 0.0;
								break;
							}
						} while (true);
					}

					jd = jdOut;
					if (startTime != 0.0) {
						if (!max.isEclipsed) {
							if (events == null) events = new ArrayList<Object[]>();
							events.add(new Object[] {startTime, endTime, maxTime, minimumIA, start, end, max});
						}
					}
				//}
			}
			jd = Math.abs(jd);
			if (jd != 0.0) jd += 10.0 / 1440.0;
		}
		return events;
	}

	/**
	 * Calculates current or next rise, set, transit for a satellite. It is recommended that the
	 * input ephem objects corresponds to a time when the satellite is above the horizon,
	 * obtained for the next pass, but it is not required.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object. The index of the satellite must be added to the index property.
	 * @param satEphem Satellite ephem object where the data will be inserted.
	 * @param horizon Refraction at horizon. Standard value is 34 arcminutes.
	 * @return The ephem object with the data included. If the rise, set, transit cannot be
	 * obtained the output will be set to zero, thats means that the results are unknown.
	 * @throws JPARSECException If an error occurs.
	 */
	public static SatelliteEphemElement getCurrentOrNextRiseSetTransit(TimeElement time, ObserverElement obs, EphemerisElement eph,
			SatelliteEphemElement satEphem, double horizon)
	throws JPARSECException {
		SatelliteEphemElement sat = satEphem.clone();
		TimeElement timeEphem = time.clone();
		int index = 0;
		if (sat.rise == null) {
			sat.rise = new double[] {0.0};
			sat.set = new double[] {0.0};
			sat.transit = new double[] {0.0};
			sat.transitElevation = new float[] {0.0f};
		} else {
			index = sat.rise.length;
			sat.rise = DataSet.addDoubleArray(sat.rise, new double[] {0.0});
			sat.set = DataSet.addDoubleArray(sat.set, new double[] {0.0});
			sat.transit = DataSet.addDoubleArray(sat.transit, new double[] {0.0});
			sat.transitElevation = DataSet.addFloatArray(sat.transitElevation, new float[] {0.0f});
		}
		SatelliteEphemElement satOut = sat.clone();

		// Obtain next pass time, when the satellite is at least 15 degrees
		// above horizon
		if (sat.nextPass == 0.0) {
			// Check Ephemeris object
			if (!EphemerisElement.checkEphemeris(eph))
				throw new JPARSECException("invalid ephemeris object.");

			if (eph.targetBody.getIndex() < 0)
				throw new JPARSECException("invalid target body in ephemeris object.");

			// Obtain object
			SatelliteOrbitalElement satOrb = SatelliteEphem.getArtificialSatelliteOrbitalElement(eph.targetBody.getIndex());
			double min_elevation = 15.0 * Constant.DEG_TO_RAD;
			sat.nextPass = getNextPass(timeEphem, obs, eph, satOrb, min_elevation, 7, true);
		}

		if (sat.elevation < 0.0) {
			if (sat.nextPass == 0.0) {
				//throw new JPARSECException("satellite below horizon and no next pass could be obtained.");
				return sat;
			}
			timeEphem = new TimeElement(Math.abs(sat.nextPass), SCALE.LOCAL_TIME);
			sat = SDP4_SGP4.satEphemeris(timeEphem, obs, eph, false, false);
		}
		double jdref = TimeScale.getJD(timeEphem, obs, eph, SCALE.LOCAL_TIME);
		double jdMax = jdref, maxElev = sat.elevation;
		double precission = 1.0;
		double jd = jdref;
		int maxIter = 5000;
		int iter = 0;
		do {
			iter++;
			jd = jd - precission / Constant.SECONDS_PER_DAY;
			timeEphem = new TimeElement(jd, SCALE.LOCAL_TIME);
			sat = SDP4_SGP4.satEphemeris(timeEphem, obs, eph, false, false);
			if (sat.elevation > maxElev) {
				maxElev = sat.elevation;
				jdMax = jd;
			}
		} while (sat.elevation > -horizon && iter < maxIter);
		double rise = jd;
		if (iter == maxIter) rise = 0.0;
		iter = 0;

		jd = jdref;
		do {
			iter ++;
			jd = jd + precission / Constant.SECONDS_PER_DAY;
			timeEphem = new TimeElement(jd, SCALE.LOCAL_TIME);
			sat = SDP4_SGP4.satEphemeris(timeEphem, obs, eph, false, false);
			if (sat.elevation > maxElev) {
				maxElev = sat.elevation;
				jdMax = jd;
			}
		} while (sat.elevation > -horizon && iter < maxIter);
		double set = jd;
		double tra = jdMax;
		float traE = (float) maxElev;
		if (iter == maxIter) {
			set = 0.0;
			tra = 0.0;
			traE = 0.0f;
		}
		satOut.rise[index] = rise;
		satOut.set[index] = set;
		satOut.transit[index] = tra;
		satOut.transitElevation[index] = traE;
		return satOut;
	}

  /**
   * A helper routine to calculate the two-dimensional inverse tangens. */
  private final double ACTAN(double SINX, double COSX)
  {
    double value, TEMP;

    if (COSX == 0.) {
      if (SINX == 0.) {
        value = 0.;
      }
      else if (SINX > 0.) {
	value = C2_PIO2;
      }
      else {
	value = C2_X3PIO2;
      }
    }
    else if (COSX > 0.) {
      if (SINX == 0.) {
	value = 0.;
      }
      else if (SINX > 0.) {
	TEMP = SINX / COSX;
        value = Math.atan(TEMP);
      }
      else {
	value = C2_TWOPI;
	TEMP = SINX / COSX;
	value = value + Math.atan(TEMP);
      }
    }
    else {
      value = C2_PI;
      TEMP = SINX / COSX;
      value = value + Math.atan(TEMP);
    }

    return value;
  }


  /**
   * Deep space initialization.
 * @throws JPARSECException */

  private final void DEEP1() throws JPARSECException
  {
    DEEP_THGR = THETAG(E1_EPOCH);
    DEEP_EQ = E1_EO;
    DEEP_XNQ = DPINI_XNODP;
    DEEP_AQNV = 1./DPINI_AO;
    DEEP_XQNCL = E1_XINCL;
    DEEP_XMAO = E1_XMO;
    DEEP_XPIDOT = DPINI_OMGDT + DPINI_XNODOT;
    DEEP_SINQ = Math.sin(E1_XNODEO);
    DEEP_COSQ = Math.cos(E1_XNODEO);
    DEEP_OMEGAQ = E1_OMEGAO;

    /* Initialize lunar solar terms. */

    DEEP_DAY = E1_DS50 + 18261.5;
    if (DEEP_DAY != DEEP_PREEP) {
      DEEP_PREEP = DEEP_DAY;
      DEEP_XNODCE = 4.5236020 - 9.2422029E-4 * DEEP_DAY;
      DEEP_STEM = Math.sin(DEEP_XNODCE);
      DEEP_CTEM = Math.cos(DEEP_XNODCE);
      DEEP_ZCOSIL = .91375164 - .03568096 * DEEP_CTEM;
      DEEP_ZSINIL = Math.sqrt(1. - DEEP_ZCOSIL * DEEP_ZCOSIL);
      DEEP_ZSINHL = .089683511 * DEEP_STEM / DEEP_ZSINIL;
      DEEP_ZCOSHL = Math.sqrt(1. - DEEP_ZSINHL * DEEP_ZSINHL);
      DEEP_C = 4.7199672 + .22997150 * DEEP_DAY;
      DEEP_GAM = 5.8351514 + .0019443680 * DEEP_DAY;
      DEEP_ZMOL = Functions.normalizeRadians(DEEP_C - DEEP_GAM);
      DEEP_ZX = .39785416 * DEEP_STEM / DEEP_ZSINIL;
      DEEP_ZY = DEEP_ZCOSHL * DEEP_CTEM + 0.91744867 * DEEP_ZSINHL * DEEP_STEM;
      DEEP_ZX = ACTAN(DEEP_ZX, DEEP_ZY);
      DEEP_ZX = DEEP_GAM + DEEP_ZX - DEEP_XNODCE;
      DEEP_ZCOSGL = Math.cos(DEEP_ZX);
      DEEP_ZSINGL = Math.sin(DEEP_ZX);
      DEEP_ZMOS = 6.2565837 + .017201977 * DEEP_DAY;
      DEEP_ZMOS = Functions.normalizeRadians(DEEP_ZMOS);
    }

    /* Do solar terms. */

    DEEP_SAVTSN = 1.E20;
    DEEP_ZCOSG = DEEP_ZCOSGS;
    DEEP_ZSING = DEEP_ZSINGS;
    DEEP_ZCOSI = DEEP_ZCOSIS;
    DEEP_ZSINI = DEEP_ZSINIS;
    DEEP_ZCOSH = DEEP_COSQ;
    DEEP_ZSINH = DEEP_SINQ;
    DEEP_CC = DEEP_C1SS;
    DEEP_ZN = DEEP_ZNS;
    DEEP_ZE = DEEP_ZES;
    DEEP_ZMO = DEEP_ZMOS;
    DEEP_XNOI = 1./DEEP_XNQ;

    /* First pass through label 20. */

    DEEP_A1  =  DEEP_ZCOSG * DEEP_ZCOSH + DEEP_ZSING * DEEP_ZCOSI * DEEP_ZSINH;
    DEEP_A3  = -DEEP_ZSING * DEEP_ZCOSH + DEEP_ZCOSG * DEEP_ZCOSI * DEEP_ZSINH;
    DEEP_A7  = -DEEP_ZCOSG * DEEP_ZSINH + DEEP_ZSING * DEEP_ZCOSI * DEEP_ZCOSH;
    DEEP_A8  =  DEEP_ZSING * DEEP_ZSINI;
    DEEP_A9  =  DEEP_ZSING * DEEP_ZSINH + DEEP_ZCOSG * DEEP_ZCOSI * DEEP_ZCOSH;
    DEEP_A10 =  DEEP_ZCOSG * DEEP_ZSINI;
    DEEP_A2  =  DPINI_COSIQ * DEEP_A7 + DPINI_SINIQ * DEEP_A8;
    DEEP_A4  =  DPINI_COSIQ * DEEP_A9 + DPINI_SINIQ * DEEP_A10;
    DEEP_A5  = -DPINI_SINIQ * DEEP_A7 + DPINI_COSIQ * DEEP_A8;
    DEEP_A6  = -DPINI_SINIQ * DEEP_A9 + DPINI_COSIQ * DEEP_A10;

    DEEP_X1 =  DEEP_A1 * DPINI_COSOMO + DEEP_A2 * DPINI_SINOMO;
    DEEP_X2 =  DEEP_A3 * DPINI_COSOMO + DEEP_A4 * DPINI_SINOMO;
    DEEP_X3 = -DEEP_A1 * DPINI_SINOMO + DEEP_A2 * DPINI_COSOMO;
    DEEP_X4 = -DEEP_A3 * DPINI_SINOMO + DEEP_A4 * DPINI_COSOMO;
    DEEP_X5 =  DEEP_A5 * DPINI_SINOMO;
    DEEP_X6 =  DEEP_A6 * DPINI_SINOMO;
    DEEP_X7 =  DEEP_A5 * DPINI_COSOMO;
    DEEP_X8 =  DEEP_A6 * DPINI_COSOMO;

    DEEP_Z31 = 12. * DEEP_X1 * DEEP_X1 - 3. * DEEP_X3 * DEEP_X3;
    DEEP_Z32 = 24. * DEEP_X1 * DEEP_X2 - 6. * DEEP_X3 * DEEP_X4;
    DEEP_Z33 = 12. * DEEP_X2 * DEEP_X2 - 3. * DEEP_X4 * DEEP_X4;
    DEEP_Z1  =  3. * (DEEP_A1 * DEEP_A1 + DEEP_A2 * DEEP_A2)
      + DEEP_Z31 * DPINI_EQSQ;
    DEEP_Z2  =  6. * (DEEP_A1 * DEEP_A3 + DEEP_A2 * DEEP_A4)
      + DEEP_Z32 * DPINI_EQSQ;
    DEEP_Z3  =  3. * (DEEP_A3 * DEEP_A3 + DEEP_A4 * DEEP_A4)
      + DEEP_Z33 * DPINI_EQSQ;
    DEEP_Z11 = -6. * DEEP_A1 * DEEP_A5
      + DPINI_EQSQ * (-24. * DEEP_X1 * DEEP_X7 - 6. * DEEP_X3 * DEEP_X5);
    DEEP_Z12 = -6. * (DEEP_A1 *DEEP_A6 + DEEP_A3 * DEEP_A5)
      + DPINI_EQSQ * (-24. * (DEEP_X2 * DEEP_X7 + DEEP_X1 * DEEP_X8)
		      - 6. * (DEEP_X3 * DEEP_X6 + DEEP_X4 * DEEP_X5));
    DEEP_Z13 = -6. * DEEP_A3 * DEEP_A6
      + DPINI_EQSQ * (-24. * DEEP_X2 * DEEP_X8 - 6. * DEEP_X4 * DEEP_X6);
    DEEP_Z21 =  6. * DEEP_A2 * DEEP_A5
      + DPINI_EQSQ * ( 24. * DEEP_X1 * DEEP_X5 - 6. * DEEP_X3 * DEEP_X7);
    DEEP_Z22 =  6. * (DEEP_A4 * DEEP_A5 + DEEP_A2 * DEEP_A6)
      + DPINI_EQSQ * ( 24. * (DEEP_X2 * DEEP_X5 + DEEP_X1 * DEEP_X6)
		      - 6. * (DEEP_X4 * DEEP_X7 + DEEP_X3 * DEEP_X8));
    DEEP_Z23 =  6. * DEEP_A4 * DEEP_A6
      + DPINI_EQSQ * ( 24. * DEEP_X2 * DEEP_X6 - 6. * DEEP_X4 * DEEP_X8);
    DEEP_Z1 =  DEEP_Z1 + DEEP_Z1 + DPINI_BSQ * DEEP_Z31;
    DEEP_Z2 =  DEEP_Z2 + DEEP_Z2 + DPINI_BSQ * DEEP_Z32;
    DEEP_Z3 =  DEEP_Z3 + DEEP_Z3 + DPINI_BSQ * DEEP_Z33;
    DEEP_S3 =  DEEP_CC * DEEP_XNOI;
    DEEP_S2 = -.5 * DEEP_S3 / DPINI_RTEQSQ;
    DEEP_S4 =  DEEP_S3 * DPINI_RTEQSQ;
    DEEP_S1 = -15. * DEEP_EQ * DEEP_S4;
    DEEP_S5 =  DEEP_X1 * DEEP_X3 + DEEP_X2 * DEEP_X4;
    DEEP_S6 =  DEEP_X2 * DEEP_X3 + DEEP_X1 * DEEP_X4;
    DEEP_S7 =  DEEP_X2 * DEEP_X4 - DEEP_X1 * DEEP_X3;
    DEEP_SE =  DEEP_S1 * DEEP_ZN * DEEP_S5;
    DEEP_SI =  DEEP_S2 * DEEP_ZN * (DEEP_Z11 + DEEP_Z13);
    DEEP_SL = -DEEP_ZN * DEEP_S3 * (DEEP_Z1  + DEEP_Z3
				    - 14. - 6. * DPINI_EQSQ);
    DEEP_SGH =  DEEP_S4 * DEEP_ZN * (DEEP_Z31 + DEEP_Z33 - 6.);
    DEEP_SH  = -DEEP_ZN * DEEP_S2 * (DEEP_Z21 + DEEP_Z23);
    if (DEEP_XQNCL < 5.2359877E-2) DEEP_SH = 0.0;
    DEEP_EE2 =  2. * DEEP_S1 * DEEP_S6;
    DEEP_E3  =  2. * DEEP_S1 * DEEP_S7;
    DEEP_XI2 =  2. * DEEP_S2 * DEEP_Z12;
    DEEP_XI3 =  2. * DEEP_S2 * (DEEP_Z13 - DEEP_Z11);
    DEEP_XL2 = -2. * DEEP_S3 * DEEP_Z2;
    DEEP_XL3 = -2. * DEEP_S3 * (DEEP_Z3 - DEEP_Z1);
    DEEP_XL4 = -2. * DEEP_S3 * (-21. - 9. * DPINI_EQSQ) * DEEP_ZE;
    DEEP_XGH2 =   2. * DEEP_S4 * DEEP_Z32;
    DEEP_XGH3 =   2. * DEEP_S4 * (DEEP_Z33 - DEEP_Z31);
    DEEP_XGH4 = -18. * DEEP_S4 * DEEP_ZE;
    DEEP_XH2 = -2. * DEEP_S2 * DEEP_Z22;
    DEEP_XH3 = -2. * DEEP_S2 * (DEEP_Z23 - DEEP_Z21);

    /* Do lunar terms (label 30). */

    DEEP_SSE = DEEP_SE;
    DEEP_SSI = DEEP_SI;
    DEEP_SSL = DEEP_SL;
    DEEP_SSH = DEEP_SH / DPINI_SINIQ;
    DEEP_SSG = DEEP_SGH - DPINI_COSIQ * DEEP_SSH;
    DEEP_SE2 = DEEP_EE2;
    DEEP_SI2 = DEEP_XI2;
    DEEP_SL2 = DEEP_XL2;
    DEEP_SGH2 = DEEP_XGH2;
    DEEP_SH2 = DEEP_XH2;
    DEEP_SE3 = DEEP_E3;
    DEEP_SI3 = DEEP_XI3;
    DEEP_SL3 = DEEP_XL3;
    DEEP_SGH3 = DEEP_XGH3;
    DEEP_SH3 = DEEP_XH3;
    DEEP_SL4 = DEEP_XL4;
    DEEP_SGH4 = DEEP_XGH4;
    DEEP_ZCOSG = DEEP_ZCOSGL;
    DEEP_ZSING = DEEP_ZSINGL;
    DEEP_ZCOSI = DEEP_ZCOSIL;
    DEEP_ZSINI = DEEP_ZSINIL;
    DEEP_ZCOSH = DEEP_ZCOSHL * DEEP_COSQ + DEEP_ZSINHL * DEEP_SINQ;
    DEEP_ZSINH = DEEP_SINQ * DEEP_ZCOSHL - DEEP_COSQ * DEEP_ZSINHL;
    DEEP_ZN = DEEP_ZNL;
    DEEP_CC = DEEP_C1L;
    DEEP_ZE = DEEP_ZEL;
    DEEP_ZMO = DEEP_ZMOL;

    /* Second pass through label 20. */

    DEEP_A1  =  DEEP_ZCOSG * DEEP_ZCOSH + DEEP_ZSING * DEEP_ZCOSI * DEEP_ZSINH;
    DEEP_A3  = -DEEP_ZSING * DEEP_ZCOSH + DEEP_ZCOSG * DEEP_ZCOSI * DEEP_ZSINH;
    DEEP_A7  = -DEEP_ZCOSG * DEEP_ZSINH + DEEP_ZSING * DEEP_ZCOSI * DEEP_ZCOSH;
    DEEP_A8  =  DEEP_ZSING * DEEP_ZSINI;
    DEEP_A9  =  DEEP_ZSING * DEEP_ZSINH + DEEP_ZCOSG * DEEP_ZCOSI * DEEP_ZCOSH;
    DEEP_A10 =  DEEP_ZCOSG * DEEP_ZSINI;
    DEEP_A2  =  DPINI_COSIQ * DEEP_A7 + DPINI_SINIQ * DEEP_A8;
    DEEP_A4  =  DPINI_COSIQ * DEEP_A9 + DPINI_SINIQ * DEEP_A10;
    DEEP_A5  = -DPINI_SINIQ * DEEP_A7 + DPINI_COSIQ * DEEP_A8;
    DEEP_A6  = -DPINI_SINIQ * DEEP_A9 + DPINI_COSIQ * DEEP_A10;

    DEEP_X1 =  DEEP_A1 * DPINI_COSOMO + DEEP_A2 * DPINI_SINOMO;
    DEEP_X2 =  DEEP_A3 * DPINI_COSOMO + DEEP_A4 * DPINI_SINOMO;
    DEEP_X3 = -DEEP_A1 * DPINI_SINOMO + DEEP_A2 * DPINI_COSOMO;
    DEEP_X4 = -DEEP_A3 * DPINI_SINOMO + DEEP_A4 * DPINI_COSOMO;
    DEEP_X5 =  DEEP_A5 * DPINI_SINOMO;
    DEEP_X6 =  DEEP_A6 * DPINI_SINOMO;
    DEEP_X7 =  DEEP_A5 * DPINI_COSOMO;
    DEEP_X8 =  DEEP_A6 * DPINI_COSOMO;

    DEEP_Z31 = 12. * DEEP_X1 * DEEP_X1 - 3. * DEEP_X3 * DEEP_X3;
    DEEP_Z32 = 24. * DEEP_X1 * DEEP_X2 - 6. * DEEP_X3 * DEEP_X4;
    DEEP_Z33 = 12. * DEEP_X2 * DEEP_X2 - 3. * DEEP_X4 * DEEP_X4;
    DEEP_Z1  =  3. * (DEEP_A1 * DEEP_A1 + DEEP_A2 * DEEP_A2)
      + DEEP_Z31 * DPINI_EQSQ;
    DEEP_Z2  =  6. * (DEEP_A1 * DEEP_A3 + DEEP_A2 * DEEP_A4)
      + DEEP_Z32 * DPINI_EQSQ;
    DEEP_Z3  =  3. * (DEEP_A3 * DEEP_A3 + DEEP_A4 * DEEP_A4)
      + DEEP_Z33 * DPINI_EQSQ;
    DEEP_Z11 = -6. * DEEP_A1 * DEEP_A5
      + DPINI_EQSQ * (-24. * DEEP_X1 * DEEP_X7 - 6. * DEEP_X3 * DEEP_X5);
    DEEP_Z12 = -6. * (DEEP_A1 *DEEP_A6 + DEEP_A3 * DEEP_A5)
      + DPINI_EQSQ * (-24. * (DEEP_X2 * DEEP_X7 + DEEP_X1 * DEEP_X8)
		      - 6. * (DEEP_X3 * DEEP_X6 + DEEP_X4 * DEEP_X5));
    DEEP_Z13 = -6. * DEEP_A3 * DEEP_A6
      + DPINI_EQSQ * (-24. * DEEP_X2 * DEEP_X8 - 6. * DEEP_X4 * DEEP_X6);
    DEEP_Z21 =  6. * DEEP_A2 * DEEP_A5
      + DPINI_EQSQ * ( 24. * DEEP_X1 * DEEP_X5 - 6. * DEEP_X3 * DEEP_X7);
    DEEP_Z22 =  6. * (DEEP_A4 * DEEP_A5 + DEEP_A2 * DEEP_A6)
      + DPINI_EQSQ * ( 24. * (DEEP_X2 * DEEP_X5 + DEEP_X1 * DEEP_X6)
		      - 6. * (DEEP_X4 * DEEP_X7 + DEEP_X3 * DEEP_X8));
    DEEP_Z23 =  6. * DEEP_A4 * DEEP_A6
      + DPINI_EQSQ * ( 24. * DEEP_X2 * DEEP_X6 - 6. * DEEP_X4 * DEEP_X8);
    DEEP_Z1 =  DEEP_Z1 + DEEP_Z1 + DPINI_BSQ * DEEP_Z31;
    DEEP_Z2 =  DEEP_Z2 + DEEP_Z2 + DPINI_BSQ * DEEP_Z32;
    DEEP_Z3 =  DEEP_Z3 + DEEP_Z3 + DPINI_BSQ * DEEP_Z33;
    DEEP_S3 =  DEEP_CC * DEEP_XNOI;
    DEEP_S2 = -.5 * DEEP_S3 / DPINI_RTEQSQ;
    DEEP_S4 =  DEEP_S3 * DPINI_RTEQSQ;
    DEEP_S1 = -15. * DEEP_EQ * DEEP_S4;
    DEEP_S5 =  DEEP_X1 * DEEP_X3 + DEEP_X2 * DEEP_X4;
    DEEP_S6 =  DEEP_X2 * DEEP_X3 + DEEP_X1 * DEEP_X4;
    DEEP_S7 =  DEEP_X2 * DEEP_X4 - DEEP_X1 * DEEP_X3;
    DEEP_SE =  DEEP_S1 * DEEP_ZN * DEEP_S5;
    DEEP_SI =  DEEP_S2 * DEEP_ZN * (DEEP_Z11 + DEEP_Z13);
    DEEP_SL = -DEEP_ZN * DEEP_S3 * (DEEP_Z1  + DEEP_Z3
				    - 14. - 6. * DPINI_EQSQ);
    DEEP_SGH =  DEEP_S4 * DEEP_ZN * (DEEP_Z31 + DEEP_Z33 - 6.);
    DEEP_SH  = -DEEP_ZN * DEEP_S2 * (DEEP_Z21 + DEEP_Z23);
    if (DEEP_XQNCL < 5.2359877E-2) DEEP_SH = 0.0;
    DEEP_EE2 =  2. * DEEP_S1 * DEEP_S6;
    DEEP_E3  =  2. * DEEP_S1 * DEEP_S7;
    DEEP_XI2 =  2. * DEEP_S2 * DEEP_Z12;
    DEEP_XI3 =  2. * DEEP_S2 * (DEEP_Z13 - DEEP_Z11);
    DEEP_XL2 = -2. * DEEP_S3 * DEEP_Z2;
    DEEP_XL3 = -2. * DEEP_S3 * (DEEP_Z3 - DEEP_Z1);
    DEEP_XL4 = -2. * DEEP_S3 * (-21. - 9. * DPINI_EQSQ) * DEEP_ZE;
    DEEP_XGH2 =   2. * DEEP_S4 * DEEP_Z32;
    DEEP_XGH3 =   2. * DEEP_S4 * (DEEP_Z33 - DEEP_Z31);
    DEEP_XGH4 = -18. * DEEP_S4 * DEEP_ZE;
    DEEP_XH2 = -2. * DEEP_S2 * DEEP_Z22;
    DEEP_XH3 = -2. * DEEP_S2 * (DEEP_Z23 - DEEP_Z21);

    /* Label 40. */

    DEEP_SSE = DEEP_SSE + DEEP_SE;
    DEEP_SSI = DEEP_SSI + DEEP_SI;
    DEEP_SSL = DEEP_SSL + DEEP_SL;
    DEEP_SSG = DEEP_SSG + DEEP_SGH - DPINI_COSIQ / DPINI_SINIQ * DEEP_SH;
    DEEP_SSH = DEEP_SSH + DEEP_SH / DPINI_SINIQ;

    /* Geopotential resonance initialization for 12 hour orbits. */

    DEEP_IRESFL = 0;
    DEEP_ISYNFL = 0;
    if (DEEP_XNQ >= .0052359877 || DEEP_XNQ <= .0034906585) {
      if (DEEP_XNQ < 8.26E-3  || DEEP_XNQ > 9.24E-3) return;
      if (DEEP_EQ  < 0.5) return;
      DEEP_IRESFL = 1;
      DEEP_EOC = DEEP_EQ * DPINI_EQSQ;
      DEEP_G201 = -.306 - (DEEP_EQ - .64) * .440;

      if (DEEP_EQ <= .65) {
	DEEP_G211 =     3.616  -    13.247  * DEEP_EQ
	          +    16.290  * DPINI_EQSQ;
	DEEP_G310 =   -19.302  +   117.390  * DEEP_EQ
	          -   228.419  * DPINI_EQSQ +   156.591  * DEEP_EOC;
	DEEP_G322 =   -18.9068 +   109.7927 * DEEP_EQ
	          -   214.6334 * DPINI_EQSQ +   146.5816 * DEEP_EOC;
	DEEP_G410 =   -41.122  +   242.694  * DEEP_EQ
	          -   471.094  * DPINI_EQSQ +   313.953  * DEEP_EOC;
	DEEP_G422 =  -146.407  +   841.880  * DEEP_EQ
	          -  1629.014  * DPINI_EQSQ +  1083.435  * DEEP_EOC;
	DEEP_G520 =  -532.114  +  3017.977  * DEEP_EQ
	          -  5740.     * DPINI_EQSQ +  3708.276  * DEEP_EOC;
      }
      else {
	DEEP_G211 =   -72.099  +   331.819  * DEEP_EQ
                  -   508.738  * DPINI_EQSQ +   266.724  * DEEP_EOC;
	DEEP_G310 =  -346.844  +  1582.851  * DEEP_EQ
                  -  2415.925  * DPINI_EQSQ +  1246.113  * DEEP_EOC;
	DEEP_G322 =  -342.585  +  1554.908  * DEEP_EQ
                  -  2366.899  * DPINI_EQSQ +  1215.972  * DEEP_EOC;
	DEEP_G410 = -1052.797  +  4758.686  * DEEP_EQ
                  -  7193.992  * DPINI_EQSQ +  3651.957  * DEEP_EOC;
	DEEP_G422 = -3581.69   + 16178.11   * DEEP_EQ
                  - 24462.77   * DPINI_EQSQ + 12422.52   * DEEP_EOC;
	if (DEEP_EQ <= .715) {
	  DEEP_G520 =  1464.74 -  4664.75 * DEEP_EQ +  3763.64 * DPINI_EQSQ;
	}
	else {
	  DEEP_G520 = -5149.66 + 29936.92 * DEEP_EQ - 54087.36 * DPINI_EQSQ
                    + 31324.56 * DEEP_EOC;
	}
      }

      if (DEEP_EQ < .7) {
	DEEP_G533 = -919.2277  + 4988.61   * DEEP_EQ
                  - 9064.77   * DPINI_EQSQ + 5542.21  * DEEP_EOC;
	DEEP_G521 = -822.71072 + 4568.6173 * DEEP_EQ
                  - 8491.4146 * DPINI_EQSQ + 5337.524 * DEEP_EOC;
	DEEP_G532 = -853.666   + 4690.25   * DEEP_EQ
                  - 8624.77   * DPINI_EQSQ + 5341.4   * DEEP_EOC;
      }
      else {
	DEEP_G533 = -37995.78  + 161616.52 * DEEP_EQ
                  - 229838.2  * DPINI_EQSQ + 109377.94 * DEEP_EOC;
	DEEP_G521 = -51752.104 + 218913.95 * DEEP_EQ
                  - 309468.16 * DPINI_EQSQ + 146349.42 * DEEP_EOC;
	DEEP_G532 = -40023.88  + 170470.89 * DEEP_EQ
                  - 242699.48 * DPINI_EQSQ + 115605.82 * DEEP_EOC;
      }

      DEEP_SINI2 = DPINI_SINIQ * DPINI_SINIQ;
      DEEP_F220 =   .75 * (1. + 2. * DPINI_COSIQ + DPINI_COSQ2);
      DEEP_F221 =  1.5     * DEEP_SINI2;
      DEEP_F321 =  1.875   * DPINI_SINIQ * (1. - 2. * DPINI_COSIQ
					  - 3. * DPINI_COSQ2);
      DEEP_F322 = -1.875   * DPINI_SINIQ * (1. + 2. * DPINI_COSIQ
					  - 3. * DPINI_COSQ2);
      DEEP_F441 = 35.      * DEEP_SINI2 * DEEP_F220;
      DEEP_F442 = 39.3750  * DEEP_SINI2 * DEEP_SINI2;
      DEEP_F522 =  9.84375   * DPINI_SINIQ * (DEEP_SINI2 * ( 1.
	- 2. * DPINI_COSIQ -  5. * DPINI_COSQ2)
	+  .33333333 * (-2. + 4. * DPINI_COSIQ + 6. * DPINI_COSQ2));
      DEEP_F523 = DPINI_SINIQ * (4.92187512 * DEEP_SINI2
	* (-2. - 4. * DPINI_COSIQ
	+ 10. * DPINI_COSQ2) + 6.56250012 * ( 1. + 2. * DPINI_COSIQ
					      - 3. * DPINI_COSQ2));
      DEEP_F542 = 29.53125 * DPINI_SINIQ * ( 2. - 8. * DPINI_COSIQ
	+ DPINI_COSQ2 * (-12. + 8. * DPINI_COSIQ + 10. * DPINI_COSQ2));
      DEEP_F543 = 29.53125 * DPINI_SINIQ * (-2. - 8. * DPINI_COSIQ
	+ DPINI_COSQ2 * ( 12. + 8. * DPINI_COSIQ - 10. * DPINI_COSQ2));
      DEEP_XNO2 = DEEP_XNQ * DEEP_XNQ;
      DEEP_AINV2 = DEEP_AQNV * DEEP_AQNV;
      DEEP_TEMP1 = 3. * DEEP_XNO2 * DEEP_AINV2;
      DEEP_TEMP = DEEP_TEMP1 * DEEP_ROOT22;
      DEEP_D2201 = DEEP_TEMP * DEEP_F220*DEEP_G201;
      DEEP_D2211 = DEEP_TEMP * DEEP_F221*DEEP_G211;
      DEEP_TEMP1 = DEEP_TEMP1 * DEEP_AQNV;
      DEEP_TEMP = DEEP_TEMP1 * DEEP_ROOT32;
      DEEP_D3210 = DEEP_TEMP * DEEP_F321 * DEEP_G310;
      DEEP_D3222 = DEEP_TEMP * DEEP_F322 * DEEP_G322;
      DEEP_TEMP1 = DEEP_TEMP1 * DEEP_AQNV;
      DEEP_TEMP = 2. * DEEP_TEMP1 * DEEP_ROOT44;
      DEEP_D4410 = DEEP_TEMP * DEEP_F441 * DEEP_G410;
      DEEP_D4422 = DEEP_TEMP * DEEP_F442 * DEEP_G422;
      DEEP_TEMP1 = DEEP_TEMP1 * DEEP_AQNV;
      DEEP_TEMP = DEEP_TEMP1 * DEEP_ROOT52;
      DEEP_D5220 = DEEP_TEMP * DEEP_F522 * DEEP_G520;
      DEEP_D5232 = DEEP_TEMP * DEEP_F523 * DEEP_G532;
      DEEP_TEMP = 2. * DEEP_TEMP1 * DEEP_ROOT54;
      DEEP_D5421 = DEEP_TEMP * DEEP_F542 * DEEP_G521;
      DEEP_D5433 = DEEP_TEMP * DEEP_F543 * DEEP_G533;
      DEEP_XLAMO = DEEP_XMAO + E1_XNODEO + E1_XNODEO - DEEP_THGR - DEEP_THGR;
      DEEP_BFACT = DPINI_XLLDOT + DPINI_XNODOT + DPINI_XNODOT
	- DEEP_THDT - DEEP_THDT;
      DEEP_BFACT = DEEP_BFACT + DEEP_SSL + DEEP_SSH + DEEP_SSH;
    }

    /* Synchronous resonance terms initialization. */

    else {
      DEEP_IRESFL = 1;
      DEEP_ISYNFL = 1;
      DEEP_G200 = 1.0 + DPINI_EQSQ * (-2.5 + .8125 * DPINI_EQSQ);
      DEEP_G310 = 1.0 + 2.0 * DPINI_EQSQ;
      DEEP_G300 = 1.0 + DPINI_EQSQ * (-6.0 + 6.60937 * DPINI_EQSQ);
      DEEP_F220 = .75 * (1. + DPINI_COSIQ) * (1. + DPINI_COSIQ);
      DEEP_F311 = .9375 * DPINI_SINIQ * DPINI_SINIQ * (1. + 3. * DPINI_COSIQ)
	- .75 * (1. + DPINI_COSIQ);
      DEEP_F330 = 1. + DPINI_COSIQ;
      DEEP_F330 = 1.875 * DEEP_F330 * DEEP_F330 * DEEP_F330;
      DEEP_DEL1 = 3. * DEEP_XNQ  * DEEP_XNQ  * DEEP_AQNV * DEEP_AQNV;
      DEEP_DEL2 = 2. * DEEP_DEL1 * DEEP_F220 * DEEP_G200 * DEEP_Q22;
      DEEP_DEL3 = 3. * DEEP_DEL1 * DEEP_F330 * DEEP_G300 * DEEP_Q33 * DEEP_AQNV;
      DEEP_DEL1 = DEEP_DEL1 * DEEP_F311 * DEEP_G310 * DEEP_Q31 * DEEP_AQNV;
      DEEP_FASX2 = .13130908;
      DEEP_FASX4 = 2.8843198;
      DEEP_FASX6 = .37448087;
      DEEP_XLAMO = DEEP_XMAO + E1_XNODEO + E1_OMEGAO - DEEP_THGR;
      DEEP_BFACT = DPINI_XLLDOT + DEEP_XPIDOT - DEEP_THDT;
      DEEP_BFACT = DEEP_BFACT + DEEP_SSL + DEEP_SSG + DEEP_SSH;
    }

    DEEP_XFACT = DEEP_BFACT - DEEP_XNQ;

    /* Initialize integrator. */

    DEEP_XLI = DEEP_XLAMO;
    DEEP_XNI = DEEP_XNQ;
    DEEP_ATIME =    0.;
    DEEP_STEPP =  720.;
    DEEP_STEPN = -720.;
    DEEP_STEP2 = 259200.;
    return;
  }


  /**
   * Deep space secular effects. */

  private final void DEEP2()
  {
    DPSEC_XLL    = DPSEC_XLL    + DEEP_SSL * DPSEC_T;
    DPSEC_OMGASM = DPSEC_OMGASM + DEEP_SSG * DPSEC_T;
    DPSEC_XNODES = DPSEC_XNODES + DEEP_SSH * DPSEC_T;
    DPSEC_EM   = E1_EO    + DEEP_SSE * DPSEC_T;
    DPSEC_XINC = E1_XINCL + DEEP_SSI * DPSEC_T;
    if (DPSEC_XINC < 0.) {
      DPSEC_XINC   = -DPSEC_XINC;
      DPSEC_XNODES =  DPSEC_XNODES + C2_PI;
      DPSEC_OMGASM =  DPSEC_OMGASM - C2_PI;
    }
    if (DEEP_IRESFL == 0) return;

    /* Label 100. */

    for (;;) {

      if (DEEP_ATIME == 0. ||
	  (DPSEC_T >= 0. && DEEP_ATIME <  0.) ||
	  (DPSEC_T <  0. && DEEP_ATIME >= 0.)) {
	if (DPSEC_T < 0.) {
          DEEP_DELT = DEEP_STEPN;
	}
	else {
	  DEEP_DELT = DEEP_STEPP;
	}
	DEEP_ATIME = 0.;
	DEEP_XNI = DEEP_XNQ;
	DEEP_XLI = DEEP_XLAMO;
	if (Math.abs(DPSEC_T - DEEP_ATIME) >= DEEP_STEPP) {
          DEEP_IRET  = 125;
	  DEEP_IRETN = 165;
	}
	else {
          DEEP_FT = DPSEC_T - DEEP_ATIME;
	  DEEP_IRETN = 140;
	}
      }
      else if (Math.abs(DPSEC_T) >= Math.abs(DEEP_ATIME)) {
	DEEP_DELT = DEEP_STEPN;
	if (DPSEC_T > 0.) DEEP_DELT = DEEP_STEPP;
	if (Math.abs(DPSEC_T - DEEP_ATIME) >= DEEP_STEPP) {
          DEEP_IRET  = 125;
	  DEEP_IRETN = 165;
	}
	else {
          DEEP_FT = DPSEC_T-DEEP_ATIME;
	  DEEP_IRETN = 140;
	}
      }
      else {
	DEEP_DELT = DEEP_STEPP;
        if (DPSEC_T >= 0.) DEEP_DELT = DEEP_STEPN;
	DEEP_IRET  = 100;
	DEEP_IRETN = 165;
      }

      /* Dot terms calculated (label 150).
       * Label 125 return point moved here by duplicating some code above. */

      for (;;) {

	if (DEEP_ISYNFL != 0) {
	  DEEP_XNDOT = DEEP_DEL1 * Math.sin(DEEP_XLI - DEEP_FASX2)
	    + DEEP_DEL2 * Math.sin(2. * (DEEP_XLI - DEEP_FASX4))
	    + DEEP_DEL3 * Math.sin(3. * (DEEP_XLI - DEEP_FASX6));
	  DEEP_XNDDT = DEEP_DEL1 * Math.cos(DEEP_XLI - DEEP_FASX2)
	    + 2. * DEEP_DEL2 * Math.cos(2. * (DEEP_XLI - DEEP_FASX4))
	    + 3. * DEEP_DEL3 * Math.cos(3. * (DEEP_XLI - DEEP_FASX6));
	}
	else {
	  DEEP_XOMI  = DEEP_OMEGAQ + DPINI_OMGDT * DEEP_ATIME;
	  DEEP_X2OMI = DEEP_XOMI + DEEP_XOMI;
	  DEEP_X2LI  = DEEP_XLI + DEEP_XLI;
	  DEEP_XNDOT = DEEP_D2201 * Math.sin(DEEP_X2OMI + DEEP_XLI - DEEP_G22)
            + DEEP_D2211 * Math.sin( DEEP_XLI   - DEEP_G22)
	    + DEEP_D3210 * Math.sin( DEEP_XOMI  + DEEP_XLI  - DEEP_G32)
            + DEEP_D3222 * Math.sin(-DEEP_XOMI  + DEEP_XLI  - DEEP_G32)
            + DEEP_D4410 * Math.sin( DEEP_X2OMI + DEEP_X2LI - DEEP_G44)
            + DEEP_D4422 * Math.sin( DEEP_X2LI  - DEEP_G44)
            + DEEP_D5220 * Math.sin( DEEP_XOMI  + DEEP_XLI  - DEEP_G52)
            + DEEP_D5232 * Math.sin(-DEEP_XOMI  + DEEP_XLI  - DEEP_G52)
            + DEEP_D5421 * Math.sin( DEEP_XOMI  + DEEP_X2LI - DEEP_G54)
	    + DEEP_D5433 * Math.sin(-DEEP_XOMI  + DEEP_X2LI - DEEP_G54);
         DEEP_XNDDT = DEEP_D2201 * Math.cos(DEEP_X2OMI + DEEP_XLI - DEEP_G22)
            + DEEP_D2211 * Math.cos( DEEP_XLI   - DEEP_G22)
            + DEEP_D3210 * Math.cos( DEEP_XOMI  + DEEP_XLI  - DEEP_G32)
            + DEEP_D3222 * Math.cos(-DEEP_XOMI  + DEEP_XLI  - DEEP_G32)
            + DEEP_D5220 * Math.cos( DEEP_XOMI  + DEEP_XLI  - DEEP_G52)
            + DEEP_D5232 * Math.cos(-DEEP_XOMI  + DEEP_XLI  - DEEP_G52)
            + 2. * (DEEP_D4410 * Math.cos(DEEP_X2OMI + DEEP_X2LI - DEEP_G44)
            + DEEP_D4422 * Math.cos( DEEP_X2LI  - DEEP_G44)
            + DEEP_D5421 * Math.cos( DEEP_XOMI  + DEEP_X2LI - DEEP_G54)
	    + DEEP_D5433 * Math.cos(-DEEP_XOMI  + DEEP_X2LI - DEEP_G54));
	}
	DEEP_XLDOT = DEEP_XNI + DEEP_XFACT;
	DEEP_XNDDT = DEEP_XNDDT * DEEP_XLDOT;
	if (DEEP_IRETN == 140) {
	  DPSEC_XN = DEEP_XNI + DEEP_XNDOT * DEEP_FT
	    + DEEP_XNDDT * DEEP_FT * DEEP_FT * 0.5;
	  DEEP_XL = DEEP_XLI + DEEP_XLDOT * DEEP_FT
            + DEEP_XNDOT * DEEP_FT * DEEP_FT * 0.5;
	  DEEP_TEMP = -DPSEC_XNODES + DEEP_THGR + DPSEC_T * DEEP_THDT;
	  DPSEC_XLL = DEEP_XL - DPSEC_OMGASM + DEEP_TEMP;
	  if (DEEP_ISYNFL == 0) DPSEC_XLL = DEEP_XL + DEEP_TEMP + DEEP_TEMP;
	  return;
	}
	if (DEEP_IRETN == 165) {
	  DEEP_XLI = DEEP_XLI + DEEP_XLDOT * DEEP_DELT
	    + DEEP_XNDOT * DEEP_STEP2;
	  DEEP_XNI = DEEP_XNI + DEEP_XNDOT * DEEP_DELT
	    + DEEP_XNDDT * DEEP_STEP2;
	  DEEP_ATIME = DEEP_ATIME + DEEP_DELT;
	}
	if (DEEP_IRET == 125) {
	  if (Math.abs(DPSEC_T - DEEP_ATIME) >= DEEP_STEPP) {
	    DEEP_IRET  = 125;
            DEEP_IRETN = 165;
	  }
	  else {
	    DEEP_FT = DPSEC_T - DEEP_ATIME;
            DEEP_IRETN = 140;
	  }
	}
	if (DEEP_IRET != 125) break;
      }
    }
  }


  /**
   * Deep space lunar-solar periodics. */

  private final void DEEP3()
  {
    DEEP_SINIS = Math.sin(DPSEC_XINC);
    DEEP_COSIS = Math.cos(DPSEC_XINC);
    if (Math.abs(DEEP_SAVTSN - DPSEC_T) >= 30.) {
      DEEP_SAVTSN = DPSEC_T;
      DEEP_ZM = DEEP_ZMOS +    DEEP_ZNS * DPSEC_T;
      DEEP_ZF = DEEP_ZM + 2. * DEEP_ZES * Math.sin(DEEP_ZM);
      DEEP_SINZF = Math.sin(DEEP_ZF);
      DEEP_F2 =  .5 * DEEP_SINZF * DEEP_SINZF - .25;
      DEEP_F3 = -.5 * DEEP_SINZF * Math.cos(DEEP_ZF);
      DEEP_SES  = DEEP_SE2  * DEEP_F2 + DEEP_SE3  * DEEP_F3;
      DEEP_SIS  = DEEP_SI2  * DEEP_F2 + DEEP_SI3  * DEEP_F3;
      DEEP_SLS  = DEEP_SL2  * DEEP_F2 + DEEP_SL3  * DEEP_F3
	+ DEEP_SL4  * DEEP_SINZF;
      DEEP_SGHS = DEEP_SGH2 * DEEP_F2 + DEEP_SGH3 * DEEP_F3
	+ DEEP_SGH4 * DEEP_SINZF;
      DEEP_SHS  = DEEP_SH2  * DEEP_F2 + DEEP_SH3  * DEEP_F3;
      DEEP_ZM = DEEP_ZMOL + DEEP_ZNL * DPSEC_T;
      DEEP_ZF = DEEP_ZM + 2. * DEEP_ZEL * Math.sin(DEEP_ZM);
      DEEP_SINZF = Math.sin(DEEP_ZF);
      DEEP_F2 =  .5 * DEEP_SINZF * DEEP_SINZF - .25;
      DEEP_F3 = -.5 * DEEP_SINZF * Math.cos(DEEP_ZF);
      DEEP_SEL  = DEEP_EE2  * DEEP_F2 + DEEP_E3   * DEEP_F3;
      DEEP_SIL  = DEEP_XI2  * DEEP_F2 + DEEP_XI3  * DEEP_F3;
      DEEP_SLL  = DEEP_XL2  * DEEP_F2 + DEEP_XL3  * DEEP_F3
	+ DEEP_XL4  * DEEP_SINZF;
      DEEP_SGHL = DEEP_XGH2 * DEEP_F2 + DEEP_XGH3 * DEEP_F3
	+ DEEP_XGH4 * DEEP_SINZF;
      DEEP_SH1 = DEEP_XH2 * DEEP_F2 + DEEP_XH3 * DEEP_F3;
      DEEP_PE   = DEEP_SES + DEEP_SEL;
      DEEP_PINC = DEEP_SIS + DEEP_SIL;
      DEEP_PL   = DEEP_SLS + DEEP_SLL;
    }
    DEEP_PGH = DEEP_SGHS + DEEP_SGHL;
    DEEP_PH  = DEEP_SHS  + DEEP_SH1;
    DPSEC_XINC = DPSEC_XINC + DEEP_PINC;
    DPSEC_EM = DPSEC_EM + DEEP_PE;

    /* Apply periodics directly. */

    if (DEEP_XQNCL >= .2) {
      DEEP_PH = DEEP_PH / DPINI_SINIQ;
      DEEP_PGH = DEEP_PGH - DPINI_COSIQ * DEEP_PH;
      DPSEC_OMGASM = DPSEC_OMGASM + DEEP_PGH;
      DPSEC_XNODES = DPSEC_XNODES + DEEP_PH;
      DPSEC_XLL = DPSEC_XLL + DEEP_PL;
    }

    /* Apply periodics with Lyddane modification. */

    else {
      DEEP_SINOK = Math.sin(DPSEC_XNODES);
      DEEP_COSOK = Math.cos(DPSEC_XNODES);
      DEEP_ALFDP = DEEP_SINIS*DEEP_SINOK;
      DEEP_BETDP = DEEP_SINIS*DEEP_COSOK;
      DEEP_DALF  =  DEEP_PH * DEEP_COSOK + DEEP_PINC * DEEP_COSIS * DEEP_SINOK;
      DEEP_DBET  = -DEEP_PH * DEEP_SINOK + DEEP_PINC * DEEP_COSIS * DEEP_COSOK;
      DEEP_ALFDP = DEEP_ALFDP + DEEP_DALF;
      DEEP_BETDP = DEEP_BETDP + DEEP_DBET;
      DEEP_XLS   = DPSEC_XLL + DPSEC_OMGASM + DEEP_COSIS * DPSEC_XNODES;
      DEEP_DLS   = DEEP_PL + DEEP_PGH - DEEP_PINC * DPSEC_XNODES * DEEP_SINIS;
      DEEP_XLS   = DEEP_XLS + DEEP_DLS;
      DPSEC_XNODES =ACTAN(DEEP_ALFDP, DEEP_BETDP);
      DPSEC_XLL    = DPSEC_XLL + DEEP_PL;
      DPSEC_OMGASM = DEEP_XLS - DPSEC_XLL
	- Math.cos(DPSEC_XINC) * DPSEC_XNODES;
    }

    return;
  }


  /**
   * Wrapper for deep space initialization.
 * @throws JPARSECException */

  private final void DPINIT(double EOSQ, double SINIO, double COSIO,
    double BETAO, double AODP, double THETA2, double SING, double COSG,
    double BETAO2, double XMDOT, double OMGDOT, double XNODOTT, double XNODPP) throws JPARSECException
  {
    /* Although this is a ported Fortran subroutine, it is in fact called
     * only with arguments that are instance variables.  So the problem
     * of returning values does not arise even if the call is by value only.
     * It also is probably the case that this routine does not have returned
     * arguments anyway. */

    DPINI_EQSQ = EOSQ;
    DPINI_SINIQ = SINIO;
    DPINI_COSIQ = COSIO;
    DPINI_RTEQSQ = BETAO;
    DPINI_AO = AODP;
    DPINI_COSQ2 = THETA2;
    DPINI_SINOMO = SING;
    DPINI_COSOMO = COSG;
    DPINI_BSQ = BETAO2;
    DPINI_XLLDOT = XMDOT;
    DPINI_OMGDT = OMGDOT;
    DPINI_XNODOT = XNODOTT;
    DPINI_XNODP = XNODPP;
    DEEP1();
    EOSQ = DPINI_EQSQ;
    SINIO = DPINI_SINIQ;
    COSIO = DPINI_COSIQ;
    BETAO = DPINI_RTEQSQ;
    AODP = DPINI_AO;
    THETA2 = DPINI_COSQ2;
    SING = DPINI_SINOMO;
    COSG = DPINI_COSOMO;
    BETAO2 = DPINI_BSQ;
    XMDOT = DPINI_XLLDOT;
    OMGDOT = DPINI_OMGDT;
    XNODOTT = DPINI_XNODOT;
    XNODPP = DPINI_XNODP;
    return;
  }


  /**
   * Wrapper for deep space lunar-solar periodics. */

  private final void DPPER(double[] dpper_args)
  {
    DPSEC_EM     = dpper_args[0];
    DPSEC_XINC   = dpper_args[1];
    DPSEC_OMGASM = dpper_args[2];
    DPSEC_XNODES = dpper_args[3];
    DPSEC_XLL    = dpper_args[4];
    DEEP3();
    dpper_args[0] = DPSEC_EM;
    dpper_args[1] = DPSEC_XINC;
    dpper_args[2] = DPSEC_OMGASM;
    dpper_args[3] = DPSEC_XNODES;
    dpper_args[4] = DPSEC_XLL;
    return;
  }


  /**
   * Wrapper for deep space secular effects. */

  private final void DPSEC(double[] dpsec_args, double[] TSINCE)
  {
    DPSEC_XLL    = dpsec_args[0];
    DPSEC_OMGASM = dpsec_args[1];
    DPSEC_XNODES = dpsec_args[2];
    /* DPSEC_EM = EMM
     * DPSEC_XINC = XINCC */
    DPSEC_XN = dpsec_args[5];
    DPSEC_T = TSINCE[0];
    DEEP2();
    dpsec_args[0] = DPSEC_XLL;
    dpsec_args[1] = DPSEC_OMGASM;
    dpsec_args[2] = DPSEC_XNODES;
    dpsec_args[3] = DPSEC_EM;
    dpsec_args[4] = DPSEC_XINC;
    dpsec_args[5] = DPSEC_XN;
    TSINCE[0] = DPSEC_T;
    return;
  }

  /**
   * Run the SDP4 model.
   *
   * <p>This should be run for long-period satellites.  The criterion is
   * evaluated on reading the orbital data from the TLE and stored in the
   * state variable itsIsDeep (should be 1 for calling this routine).
   *
   * @param IFLAG
   *   IFLAG[0] must be given as 1 for the first call for any given satellite.
   *   It is then returned as 0 and can be given as 0 for further calls.
   * @param TSINCE
   *   TSINCE[0] is the time difference between the time of interest and the
   *   epoch of the TLE.  It must be given in minutes.
 * @throws JPARSECException */

  private final void RunSDP4(int[] IFLAG, double[] TSINCE) throws JPARSECException
  {
    double A, AXN, AYN, AYNL, BETA, BETAL, CAPU, COS2U, COSEPW,
      COSIK, COSNOK, COSU, COSUK, E, ECOSE, ELSQ, EM, EPW, ESINE, OMGADF,
      PL, R, RDOT, RDOTK, RFDOT, RFDOTK, RK, SIN2U, SINEPW, SINIK,
      SINNOK, SINU, SINUK, TEMP, TEMP4, TEMP5, TEMP6, TEMPA,
      TEMPE, TEMPL, TSQ, U, UK, UX, UY, UZ, VX, VY, VZ, XINC, XINCK,
      XL, XLL, XLT, XMAM, XMDF, XMX, XMY, XN, XNODDF, XNODE, XNODEK;
    double[] dpsec_args = new double[6];
    double[] dpper_args = new double[5];
    int I;

    /* The Java compiler requires these initializations. */

    TEMP4 = 0.;
    TEMP5 = 0.;
    TEMP6 = 0.;
    COSEPW = 0.;
    SINEPW = 0.;
    EM = 0.;
    XINC = 0.;

    if (IFLAG[0] != 0) {

      /* RECOVER ORIGINAL MEAN MOTION (SDP4_XNODP) AND SEMIMAJOR AXIS
       * (SDP4_AODP) FROM INPUT ELEMENTS */

      SDP4_A1 = Math.pow(C1_XKE / E1_XNO, C1_TOTHRD);
      SDP4_COSIO = Math.cos(E1_XINCL);
      SDP4_THETA2 = SDP4_COSIO * SDP4_COSIO;
      SDP4_X3THM1 = 3. * SDP4_THETA2 - 1.;
      SDP4_EOSQ = E1_EO * E1_EO;
      SDP4_BETAO2 = 1. - SDP4_EOSQ;
      SDP4_BETAO = Math.sqrt(SDP4_BETAO2);
      SDP4_DEL1 = 1.5 * C1_CK2 * SDP4_X3THM1
	/ (SDP4_A1 * SDP4_A1 * SDP4_BETAO * SDP4_BETAO2);
      SDP4_AO = SDP4_A1 * (1. - SDP4_DEL1 * (.5 * C1_TOTHRD + SDP4_DEL1
	* (1. + 134./81. * SDP4_DEL1)));
      SDP4_DELO = 1.5 * C1_CK2 * SDP4_X3THM1
	/ (SDP4_AO * SDP4_AO * SDP4_BETAO * SDP4_BETAO2);
      SDP4_XNODP = E1_XNO / (1. + SDP4_DELO);
      SDP4_AODP = SDP4_AO / (1. - SDP4_DELO);

      /* INITIALIZATION
       *
       * FOR PERIGEE BELOW 156 KM, THE VALUES OF
       * S AND QOMS2T ARE ALTERED */

      SDP4_S4 = C1_S;
      SDP4_QOMS24 = C1_QOMS2T;
      SDP4_PERIGE = (SDP4_AODP * (1. - E1_EO) - C1_AE) * C1_XKMPER;
      if (SDP4_PERIGE < 156.) {
	SDP4_S4 = SDP4_PERIGE - 78.;
	if (SDP4_PERIGE <= 98.) {
	  SDP4_S4 = 20.;
        }
	SDP4_QOMS24 = ((120. - SDP4_S4) * C1_AE / C1_XKMPER);
	SDP4_QOMS24 *= SDP4_QOMS24;
	SDP4_QOMS24 *= SDP4_QOMS24;
	SDP4_S4 = SDP4_S4 / C1_XKMPER + C1_AE;
      }
      SDP4_PINVSQ = 1. / (SDP4_AODP * SDP4_AODP * SDP4_BETAO2 * SDP4_BETAO2);
      SDP4_SING = Math.sin(E1_OMEGAO);
      SDP4_COSG = Math.cos(E1_OMEGAO);
      SDP4_TSI = 1. / (SDP4_AODP - SDP4_S4);
      SDP4_ETA = SDP4_AODP * E1_EO * SDP4_TSI;
      SDP4_ETASQ = SDP4_ETA * SDP4_ETA;
      SDP4_EETA = E1_EO * SDP4_ETA;
      SDP4_PSISQ = Math.abs(1. - SDP4_ETASQ);
      SDP4_COEF = SDP4_QOMS24 * SDP4_TSI * SDP4_TSI * SDP4_TSI * SDP4_TSI;
      SDP4_COEF1 = SDP4_COEF / Math.pow(SDP4_PSISQ, 3.5);
      SDP4_C2 = SDP4_COEF1 * SDP4_XNODP * (SDP4_AODP * (1. + 1.5 * SDP4_ETASQ
	+ SDP4_EETA * (4. + SDP4_ETASQ))
	+ .75 * C1_CK2 * SDP4_TSI / SDP4_PSISQ * SDP4_X3THM1
	* (8. + 3. * SDP4_ETASQ * (8. + SDP4_ETASQ)));
      SDP4_C1 = E1_BSTAR * SDP4_C2;
      SDP4_SINIO = Math.sin(E1_XINCL);
      SDP4_A3OVK2 = -C1_XJ3 / C1_CK2 * C1_AE * C1_AE * C1_AE;
      SDP4_X1MTH2 = 1. - SDP4_THETA2;
      SDP4_C4 = 2. * SDP4_XNODP * SDP4_COEF1 * SDP4_AODP * SDP4_BETAO2
	* (SDP4_ETA * (2. + .5 * SDP4_ETASQ) + E1_EO * (.5 + 2. * SDP4_ETASQ)
	- 2. * C1_CK2 * SDP4_TSI / (SDP4_AODP * SDP4_PSISQ)
	* (-3. * SDP4_X3THM1 * (1. - 2. * SDP4_EETA + SDP4_ETASQ
	* (1.5 - .5 * SDP4_EETA)) + .75 * SDP4_X1MTH2
	* (2. * SDP4_ETASQ - SDP4_EETA * (1. + SDP4_ETASQ))
	* Math.cos(2. * E1_OMEGAO)));
      SDP4_THETA4 = SDP4_THETA2 * SDP4_THETA2;
      SDP4_TEMP1 = 3. * C1_CK2 * SDP4_PINVSQ * SDP4_XNODP;
      SDP4_TEMP2 = SDP4_TEMP1 * C1_CK2 * SDP4_PINVSQ;
      SDP4_TEMP3 = 1.25 * C1_CK4 * SDP4_PINVSQ * SDP4_PINVSQ * SDP4_XNODP;
      SDP4_XMDOT = SDP4_XNODP + .5 * SDP4_TEMP1 * SDP4_BETAO * SDP4_X3THM1
	+ .0625 * SDP4_TEMP2 * SDP4_BETAO
	* (13. - 78. * SDP4_THETA2 + 137. * SDP4_THETA4);
      SDP4_X1M5TH = 1. - 5. * SDP4_THETA2;
      SDP4_OMGDOT = -.5 * SDP4_TEMP1 * SDP4_X1M5TH
	+ .0625 * SDP4_TEMP2 * (7. - 114. * SDP4_THETA2 + 395. * SDP4_THETA4)
	+ SDP4_TEMP3 * (3. - 36. * SDP4_THETA2 + 49. * SDP4_THETA4);
      SDP4_XHDOT1 = -SDP4_TEMP1 * SDP4_COSIO;
      SDP4_XNODOT = SDP4_XHDOT1 + (.5 * SDP4_TEMP2 * (4. - 19. * SDP4_THETA2)
	+ 2. * SDP4_TEMP3 * (3. - 7. * SDP4_THETA2)) * SDP4_COSIO;
      SDP4_XNODCF = 3.5 * SDP4_BETAO2 * SDP4_XHDOT1 * SDP4_C1;
      SDP4_T2COF = 1.5 * SDP4_C1;
      SDP4_XLCOF = .125 * SDP4_A3OVK2 * SDP4_SINIO
	* (3. + 5. * SDP4_COSIO) / (1. + SDP4_COSIO);
      SDP4_AYCOF = .25 * SDP4_A3OVK2 * SDP4_SINIO;
      SDP4_X7THM1 = 7. * SDP4_THETA2 - 1.;
      IFLAG[0] = 0;
      DPINIT(SDP4_EOSQ, SDP4_SINIO, SDP4_COSIO, SDP4_BETAO, SDP4_AODP,
	SDP4_THETA2, SDP4_SING, SDP4_COSG, SDP4_BETAO2, SDP4_XMDOT,
	SDP4_OMGDOT, SDP4_XNODOT, SDP4_XNODP);
    }

    /* UPDATE FOR SECULAR GRAVITY AND ATMOSPHERIC DRAG */

    XMDF   = E1_XMO    + SDP4_XMDOT  * TSINCE[0];
    OMGADF = E1_OMEGAO + SDP4_OMGDOT * TSINCE[0];
    XNODDF = E1_XNODEO + SDP4_XNODOT * TSINCE[0];
    TSQ = TSINCE[0] * TSINCE[0];
    XNODE = XNODDF + SDP4_XNODCF * TSQ;
    TEMPA = 1. - SDP4_C1 * TSINCE[0];
    TEMPE = E1_BSTAR * SDP4_C4 * TSINCE[0];
    TEMPL = SDP4_T2COF * TSQ;
    XN = SDP4_XNODP;

    dpsec_args[0] = XMDF;
    dpsec_args[1] = OMGADF;
    dpsec_args[2] = XNODE;
    dpsec_args[3] = EM;
    dpsec_args[4] = XINC;
    dpsec_args[5] = XN;
    DPSEC(dpsec_args, TSINCE);
    XMDF   = dpsec_args[0];
    OMGADF = dpsec_args[1];
    XNODE  = dpsec_args[2];
    EM     = dpsec_args[3];
    XINC   = dpsec_args[4];
    XN     = dpsec_args[5];

    A = Math.pow(C1_XKE / XN, C1_TOTHRD) * TEMPA * TEMPA;
    E = EM - TEMPE;
    XMAM = XMDF + SDP4_XNODP * TEMPL;

    dpper_args[0] = E;
    dpper_args[1] = XINC;
    dpper_args[2] = OMGADF;
    dpper_args[3] = XNODE;
    dpper_args[4] = XMAM;
    DPPER(dpper_args);
    E      = dpper_args[0];
    XINC   = dpper_args[1];
    OMGADF = dpper_args[2];
    XNODE  = dpper_args[3];
    XMAM   = dpper_args[4];

    XL = XMAM + OMGADF + XNODE;
    BETA = Math.sqrt(1. - E * E);
    XN = C1_XKE / Math.pow(A, 1.5);

    /* LONG PERIOD PERIODICS */

    AXN = E * Math.cos(OMGADF);
    TEMP = 1. / (A * BETA * BETA);
    XLL = TEMP * SDP4_XLCOF * AXN;
    AYNL = TEMP * SDP4_AYCOF;
    XLT = XL + XLL;
    AYN = E * Math.sin(OMGADF) + AYNL;

    /* SOLVE KEPLERS EQUATION */

    CAPU = Functions.normalizeRadians(XLT - XNODE);
    SDP4_TEMP2 = CAPU;
    for (I = 1; I < 11; I++) {
      SINEPW = Math.sin(SDP4_TEMP2);
      COSEPW = Math.cos(SDP4_TEMP2);
      SDP4_TEMP3 = AXN * SINEPW;
      TEMP4 = AYN * COSEPW;
      TEMP5 = AXN * COSEPW;
      TEMP6 = AYN * SINEPW;
      EPW = (CAPU - TEMP4 + SDP4_TEMP3 - SDP4_TEMP2)
	/ (1. - TEMP5 - TEMP6) + SDP4_TEMP2;
      if (Math.abs(EPW-SDP4_TEMP2) <= C1_E6A) break;
      SDP4_TEMP2 = EPW;
    }

    /* SHORT PERIOD PRELIMINARY QUANTITIES */

    ECOSE = TEMP5 + TEMP6;
    ESINE = SDP4_TEMP3 - TEMP4;
    ELSQ = AXN * AXN + AYN * AYN;
    TEMP = 1. - ELSQ;
    PL = A * TEMP;
    R = A * (1. - ECOSE);
    SDP4_TEMP1 = 1. / R;
    RDOT = C1_XKE * Math.sqrt(A) * ESINE * SDP4_TEMP1;
    RFDOT = C1_XKE * Math.sqrt(PL) * SDP4_TEMP1;
    SDP4_TEMP2 = A * SDP4_TEMP1;
    BETAL = Math.sqrt(TEMP);
    SDP4_TEMP3 = 1. / (1. + BETAL);
    COSU = SDP4_TEMP2 * (COSEPW - AXN + AYN * ESINE * SDP4_TEMP3);
    SINU = SDP4_TEMP2 * (SINEPW - AYN - AXN * ESINE * SDP4_TEMP3);
    U = ACTAN(SINU, COSU);
    SIN2U =2. * SINU * COSU;
    COS2U =2. * COSU * COSU - 1.;
    TEMP = 1. / PL;
    SDP4_TEMP1 = C1_CK2 * TEMP;
    SDP4_TEMP2 = SDP4_TEMP1 * TEMP;

    /* UPDATE FOR SHORT PERIODICS */

    RK = R * (1. - 1.5 * SDP4_TEMP2 * BETAL * SDP4_X3THM1)
      + .5 * SDP4_TEMP1 * SDP4_X1MTH2 * COS2U;
    UK = U - .25 * SDP4_TEMP2 * SDP4_X7THM1 * SIN2U;
    XNODEK = XNODE + 1.5 * SDP4_TEMP2 * SDP4_COSIO * SIN2U;
    XINCK = XINC + 1.5 * SDP4_TEMP2 * SDP4_COSIO * SDP4_SINIO * COS2U;
    RDOTK = RDOT - XN * SDP4_TEMP1 * SDP4_X1MTH2 * SIN2U;
    RFDOTK = RFDOT + XN * SDP4_TEMP1
      * (SDP4_X1MTH2 * COS2U + 1.5 * SDP4_X3THM1);

    /* ORIENTATION VECTORS */

    SINUK = Math.sin(UK);
    COSUK = Math.cos(UK);
    SINIK = Math.sin(XINCK);
    COSIK = Math.cos(XINCK);
    SINNOK = Math.sin(XNODEK);
    COSNOK = Math.cos(XNODEK);
    XMX = -SINNOK * COSIK;
    XMY =  COSNOK * COSIK;
    UX = XMX * SINUK + COSNOK * COSUK;
    UY = XMY * SINUK + SINNOK * COSUK;
    UZ = SINIK * SINUK;
    VX = XMX * COSUK - COSNOK * SINUK;
    VY = XMY * COSUK - SINNOK * SINUK;
    VZ = SINIK * COSUK;

    /* POSITION AND VELOCITY */

    E1_X = RK * UX;
    E1_Y = RK * UY;
    E1_Z = RK * UZ;
    E1_XDOT = RDOTK * UX + RFDOTK * VX;
    E1_YDOT = RDOTK * UY + RFDOTK * VY;
    E1_ZDOT = RDOTK * UZ + RFDOTK * VZ;

    itsR[0] = E1_X    * C1_XKMPER / C1_AE / 1E6;
    itsR[1] = E1_Y    * C1_XKMPER / C1_AE / 1E6;
    itsR[2] = E1_Z    * C1_XKMPER / C1_AE / 1E6;
    itsV[0] = E1_XDOT * C1_XKMPER / C1_AE * C1_XMNPDA / 86400.;
    itsV[1] = E1_YDOT * C1_XKMPER / C1_AE * C1_XMNPDA / 86400.;
    itsV[2] = E1_ZDOT * C1_XKMPER / C1_AE * C1_XMNPDA / 86400.;

    return;
  }


  /**
   * Run the SGP4 model.
   *
   * <p>This should be run for short-period satellites.  The criterion is
   * evaluated on reading the orbital data from the TLE and stored in the
   * state variable itsIsDeep (should be 0 for calling this routine).
   *
   * @param IFLAG
   *   IFLAG[0] must be given as 1 for the first call for any given satellite.
   *   It is then returned as 0 and can be given as 0 for further calls.
   * @param TSINCE
   *   TSINCE[0] is the time difference between the time of interest and the
   *   epoch of the TLE.  It must be given in minutes. */

  private final void RunSGP4(int[] IFLAG, double[] TSINCE)
  {
    double COSUK, SINUK, RFDOTK, VX, VY, VZ, UX, UY, UZ, XMY, XMX,
      COSNOK, SINNOK, COSIK, SINIK, RDOTK, XINCK, XNODEK, UK, RK,
      COS2U, SIN2U, U, SINU, COSU, BETAL, RFDOT, RDOT, R, PL, ELSQ,
      ESINE, ECOSE, EPW, TEMP6, TEMP5, TEMP4, COSEPW, SINEPW,
      CAPU, AYN, XLT, AYNL, XLL, AXN, XN, BETA, XL, E, A, TFOUR,
      TCUBE, DELM, DELOMG, TEMPL, TEMPE, TEMPA, XNODE, TSQ, XMP,
      OMEGA, XNODDF, OMGADF, XMDF;
    int I;

    /* The Java compiler requires these initializations. */

    TEMP4 = 0.;
    TEMP5 = 0.;
    TEMP6 = 0.;
    COSEPW = 0.;
    SINEPW = 0.;

    if (IFLAG[0] != 0) {

      /* RECOVER ORIGINAL MEAN MOTION (SGP4_XNODP) AND SEMIMAJOR AXIS
       * (SGP4_AODP) FROM INPUT ELEMENTS */

      SGP4_A1 = Math.pow(C1_XKE / E1_XNO, C1_TOTHRD);
      SGP4_COSIO = Math.cos(E1_XINCL);
      SGP4_THETA2 = SGP4_COSIO * SGP4_COSIO;
      SGP4_X3THM1 = 3. * SGP4_THETA2 - 1.;
      SGP4_EOSQ = E1_EO * E1_EO;
      SGP4_BETAO2 = 1. - SGP4_EOSQ;
      SGP4_BETAO = Math.sqrt(SGP4_BETAO2);

      SGP4_DEL1 = 1.5 * C1_CK2 * SGP4_X3THM1
	/ (SGP4_A1 * SGP4_A1 * SGP4_BETAO * SGP4_BETAO2);
      SGP4_AO = SGP4_A1 * (1. - SGP4_DEL1
	* (.5 * C1_TOTHRD + SGP4_DEL1 * (1. + 134./81. * SGP4_DEL1)));
      SGP4_DELO = 1.5 * C1_CK2 * SGP4_X3THM1
	/ (SGP4_AO * SGP4_AO * SGP4_BETAO * SGP4_BETAO2);
      SGP4_XNODP = E1_XNO / (1. + SGP4_DELO);
      SGP4_AODP = SGP4_AO / (1. - SGP4_DELO);

      /* INITIALIZATION
       *
       * FOR PERIGEE LESS THAN 220 KILOMETERS, THE SGP4_ISIMP FLAG IS SET AND
       * THE EQUATIONS ARE TRUNCATED TO LINEAR VARIATION IN SQRT A AND
       * QUADRATIC VARIATION IN MEAN ANOMALY.  ALSO, THE SGP4_C3 TERM, THE
       * DELTA OMEGA TERM, AND THE DELTA M TERM ARE DROPPED. */

      SGP4_ISIMP = 0;
      if ((SGP4_AODP * (1. - E1_EO) / C1_AE) < (220. / C1_XKMPER + C1_AE)) {
	SGP4_ISIMP = 1;
      }

      /* FOR PERIGEE BELOW 156 KM, THE VALUES OF
       * S AND QOMS2T ARE ALTERED */

      SGP4_S4 = C1_S;
      SGP4_QOMS24 = C1_QOMS2T;
      SGP4_PERIGE = (SGP4_AODP * (1. - E1_EO) - C1_AE) * C1_XKMPER;
      if (SGP4_PERIGE < 156.) {
        SGP4_S4 = SGP4_PERIGE - 78.;
	if (SGP4_PERIGE <= 98.) {SGP4_S4 = 20.;}
	SGP4_QOMS24 = (120. - SGP4_S4) * C1_AE / C1_XKMPER;
	SGP4_QOMS24 *= SGP4_QOMS24;
	SGP4_QOMS24 *= SGP4_QOMS24;
	SGP4_S4 = SGP4_S4 / C1_XKMPER + C1_AE;
      }
      SGP4_PINVSQ = 1. / (SGP4_AODP * SGP4_AODP * SGP4_BETAO2 * SGP4_BETAO2);
      SGP4_TSI = 1. / (SGP4_AODP - SGP4_S4);
      SGP4_ETA = SGP4_AODP * E1_EO * SGP4_TSI;
      SGP4_ETASQ = SGP4_ETA * SGP4_ETA;
      SGP4_EETA = E1_EO * SGP4_ETA;
      SGP4_PSISQ = Math.abs(1. - SGP4_ETASQ);
      SGP4_COEF = SGP4_QOMS24 * SGP4_TSI * SGP4_TSI * SGP4_TSI * SGP4_TSI;
      SGP4_COEF1 = SGP4_COEF / Math.pow(SGP4_PSISQ, 3.5);
      SGP4_C2 = SGP4_COEF1 * SGP4_XNODP * (SGP4_AODP * (1. + 1.5 * SGP4_ETASQ
	+ SGP4_EETA * (4. + SGP4_ETASQ)) + .75 * C1_CK2 * SGP4_TSI / SGP4_PSISQ
	* SGP4_X3THM1 * (8. + 3. * SGP4_ETASQ * (8. + SGP4_ETASQ)));
      SGP4_C1 = E1_BSTAR * SGP4_C2;
      SGP4_SINIO = Math.sin(E1_XINCL);
      SGP4_A3OVK2 = -C1_XJ3 / C1_CK2 * C1_AE * C1_AE * C1_AE;
      SGP4_C3 = SGP4_COEF * SGP4_TSI * SGP4_A3OVK2 * SGP4_XNODP * C1_AE
	* SGP4_SINIO / E1_EO;
      SGP4_X1MTH2 = 1. - SGP4_THETA2;
      SGP4_C4 = 2. * SGP4_XNODP * SGP4_COEF1 * SGP4_AODP * SGP4_BETAO2
	* (SGP4_ETA * (2. + .5 * SGP4_ETASQ) + E1_EO * (.5 + 2. * SGP4_ETASQ)
	- 2. * C1_CK2 * SGP4_TSI / (SGP4_AODP * SGP4_PSISQ)
	* (-3. * SGP4_X3THM1 * (1. - 2. * SGP4_EETA + SGP4_ETASQ
	* (1.5 - .5 * SGP4_EETA)) + .75 * SGP4_X1MTH2
	* (2. * SGP4_ETASQ - SGP4_EETA * (1. + SGP4_ETASQ))
	* Math.cos(2. * E1_OMEGAO)));
      SGP4_C5 = 2. * SGP4_COEF1 * SGP4_AODP * SGP4_BETAO2
	* (1. + 2.75 * (SGP4_ETASQ + SGP4_EETA) + SGP4_EETA * SGP4_ETASQ);
      SGP4_THETA4 = SGP4_THETA2 * SGP4_THETA2;
      SGP4_TEMP1 = 3. * C1_CK2 * SGP4_PINVSQ * SGP4_XNODP;
      SGP4_TEMP2 = SGP4_TEMP1 * C1_CK2 * SGP4_PINVSQ;
      SGP4_TEMP3 = 1.25 * C1_CK4 * SGP4_PINVSQ * SGP4_PINVSQ * SGP4_XNODP;
      SGP4_XMDOT = SGP4_XNODP + .5 * SGP4_TEMP1 * SGP4_BETAO * SGP4_X3THM1
	+ .0625 * SGP4_TEMP2 * SGP4_BETAO * (13. - 78. * SGP4_THETA2
	+ 137. * SGP4_THETA4);
      SGP4_X1M5TH = 1. - 5. * SGP4_THETA2;
      SGP4_OMGDOT = -.5 * SGP4_TEMP1 * SGP4_X1M5TH
	+ .0625 * SGP4_TEMP2 * (7. - 114. * SGP4_THETA2 + 395. * SGP4_THETA4)
	+ SGP4_TEMP3 * (3. - 36. * SGP4_THETA2 + 49. * SGP4_THETA4);
      SGP4_XHDOT1 = -SGP4_TEMP1 * SGP4_COSIO;
      SGP4_XNODOT = SGP4_XHDOT1 + (.5 * SGP4_TEMP2 * (4. - 19. * SGP4_THETA2)
	+ 2. * SGP4_TEMP3 * (3. - 7. * SGP4_THETA2)) * SGP4_COSIO;
      SGP4_OMGCOF = E1_BSTAR * SGP4_C3 * Math.cos(E1_OMEGAO);
      SGP4_XMCOF = -C1_TOTHRD * SGP4_COEF * E1_BSTAR * C1_AE / SGP4_EETA;
      SGP4_XNODCF = 3.5 * SGP4_BETAO2 * SGP4_XHDOT1 * SGP4_C1;
      SGP4_T2COF = 1.5 * SGP4_C1;
      SGP4_XLCOF = .125 * SGP4_A3OVK2 * SGP4_SINIO
	  * (3. + 5. * SGP4_COSIO) / (1. + SGP4_COSIO);
      SGP4_AYCOF = .25 * SGP4_A3OVK2 * SGP4_SINIO;
      SGP4_DELMO = (1. + SGP4_ETA * Math.cos(E1_XMO));
      SGP4_DELMO *= (SGP4_DELMO * SGP4_DELMO);
      SGP4_SINMO = Math.sin(E1_XMO);
      SGP4_X7THM1 = 7. * SGP4_THETA2 - 1.;
      if (SGP4_ISIMP != 1) {
	SGP4_C1SQ = SGP4_C1 * SGP4_C1;
	SGP4_D2 = 4. * SGP4_AODP * SGP4_TSI * SGP4_C1SQ;
	SGP4_TEMP = SGP4_D2 * SGP4_TSI * SGP4_C1 / 3.;
	SGP4_D3 = (17. * SGP4_AODP + SGP4_S4) * SGP4_TEMP;
	SGP4_D4 = .5 * SGP4_TEMP * SGP4_AODP * SGP4_TSI
	  * (221. * SGP4_AODP + 31. * SGP4_S4) * SGP4_C1;
	SGP4_T3COF = SGP4_D2 + 2. * SGP4_C1SQ;
	SGP4_T4COF = .25 * (3. * SGP4_D3 + SGP4_C1
	  * (12. * SGP4_D2 + 10. * SGP4_C1SQ));
	SGP4_T5COF = .2 * (3. * SGP4_D4 + 12. * SGP4_C1 * SGP4_D3
	  + 6. * SGP4_D2 * SGP4_D2
	  + 15. * SGP4_C1SQ * (2. * SGP4_D2 + SGP4_C1SQ));
      }
      IFLAG[0] = 0;
    }

    /* UPDATE FOR SECULAR GRAVITY AND ATMOSPHERIC DRAG */

    XMDF   = E1_XMO    + SGP4_XMDOT  * TSINCE[0];
    OMGADF = E1_OMEGAO + SGP4_OMGDOT * TSINCE[0];
    XNODDF = E1_XNODEO + SGP4_XNODOT * TSINCE[0];
    OMEGA = OMGADF;
    XMP = XMDF;
    TSQ = TSINCE[0] * TSINCE[0];
    XNODE = XNODDF + SGP4_XNODCF * TSQ;
    TEMPA = 1. - SGP4_C1 * TSINCE[0];
    TEMPE = E1_BSTAR * SGP4_C4 * TSINCE[0];
    TEMPL = SGP4_T2COF * TSQ;
    if (SGP4_ISIMP != 1) {
      DELOMG = SGP4_OMGCOF * TSINCE[0];
      DELM = SGP4_XMCOF * (Math.pow(1. + SGP4_ETA * Math.cos(XMDF), 3.)
	- SGP4_DELMO);
      SGP4_TEMP = DELOMG + DELM;
      XMP = XMDF + SGP4_TEMP;
      OMEGA = OMGADF - SGP4_TEMP;
      TCUBE = TSQ * TSINCE[0];
      TFOUR = TSINCE[0] * TCUBE;
      TEMPA = TEMPA - SGP4_D2 * TSQ - SGP4_D3 * TCUBE - SGP4_D4 * TFOUR;
      TEMPE = TEMPE + E1_BSTAR * SGP4_C5 * (Math.sin(XMP) - SGP4_SINMO);
      TEMPL = TEMPL + SGP4_T3COF * TCUBE
	+ TFOUR * (SGP4_T4COF + TSINCE[0] * SGP4_T5COF);
    }
    A = SGP4_AODP * TEMPA * TEMPA;
    E = E1_EO - TEMPE;
    XL = XMP + OMEGA + XNODE + SGP4_XNODP * TEMPL;
    BETA = Math.sqrt(1. - E * E);
    XN = C1_XKE / Math.pow(A, 1.5);

    /* LONG PERIOD PERIODICS */

    AXN = E * Math.cos(OMEGA);
    SGP4_TEMP = 1. / (A * BETA * BETA);
    XLL = SGP4_TEMP * SGP4_XLCOF * AXN;
    AYNL = SGP4_TEMP * SGP4_AYCOF;
    XLT = XL + XLL;
    AYN = E * Math.sin(OMEGA) + AYNL;

    /* SOLVE KEPLERS EQUATION */

    CAPU = Functions.normalizeRadians(XLT - XNODE);
    SGP4_TEMP2 = CAPU;
    for (I = 1; I < 11; I++) {
      SINEPW = Math.sin(SGP4_TEMP2);
      COSEPW = Math.cos(SGP4_TEMP2);
      SGP4_TEMP3 = AXN * SINEPW;
      TEMP4 = AYN * COSEPW;
      TEMP5 = AXN * COSEPW;
      TEMP6 = AYN * SINEPW;
      EPW = (CAPU - TEMP4 + SGP4_TEMP3 - SGP4_TEMP2)
	/ (1. - TEMP5 - TEMP6) + SGP4_TEMP2;
      if (Math.abs(EPW - SGP4_TEMP2) <= C1_E6A) break;
      SGP4_TEMP2 = EPW;
    }

    /* SHORT PERIOD PRELIMINARY QUANTITIES */

    ECOSE = TEMP5 + TEMP6;
    ESINE = SGP4_TEMP3 - TEMP4;
    ELSQ = AXN * AXN + AYN * AYN;
    SGP4_TEMP = 1. - ELSQ;
    PL = A * SGP4_TEMP;
    R = A * (1. - ECOSE);
    SGP4_TEMP1 = 1. / R;
    RDOT = C1_XKE * Math.sqrt(A) * ESINE * SGP4_TEMP1;
    RFDOT = C1_XKE * Math.sqrt(PL) * SGP4_TEMP1;
    SGP4_TEMP2 = A * SGP4_TEMP1;
    BETAL = Math.sqrt(SGP4_TEMP);
    SGP4_TEMP3 = 1. / (1. + BETAL);
    COSU = SGP4_TEMP2 * (COSEPW - AXN + AYN * ESINE * SGP4_TEMP3);
    SINU = SGP4_TEMP2 * (SINEPW - AYN - AXN * ESINE * SGP4_TEMP3);
    U = ACTAN(SINU, COSU);
    SIN2U = 2. * SINU * COSU;
    COS2U = 2. * COSU * COSU - 1.;
    SGP4_TEMP = 1. / PL;
    SGP4_TEMP1 = C1_CK2 * SGP4_TEMP;
    SGP4_TEMP2 = SGP4_TEMP1 * SGP4_TEMP;

    /* UPDATE FOR SHORT PERIODICS */

    RK = R * (1. - 1.5 * SGP4_TEMP2 * BETAL * SGP4_X3THM1)
      + .5 * SGP4_TEMP1 * SGP4_X1MTH2 * COS2U;
    UK = U - .25 * SGP4_TEMP2 * SGP4_X7THM1 * SIN2U;
    XNODEK = XNODE + 1.5 * SGP4_TEMP2 * SGP4_COSIO * SIN2U;
    XINCK = E1_XINCL + 1.5 * SGP4_TEMP2 * SGP4_COSIO * SGP4_SINIO*COS2U;
    RDOTK = RDOT - XN * SGP4_TEMP1 * SGP4_X1MTH2 * SIN2U;
    RFDOTK = RFDOT + XN * SGP4_TEMP1
      * (SGP4_X1MTH2 * COS2U + 1.5 * SGP4_X3THM1);

    /* ORIENTATION VECTORS */

    SINUK = Math.sin(UK);
    COSUK = Math.cos(UK);
    SINIK = Math.sin(XINCK);
    COSIK = Math.cos(XINCK);
    SINNOK = Math.sin(XNODEK);
    COSNOK = Math.cos(XNODEK);
    XMX = -SINNOK * COSIK;
    XMY =  COSNOK * COSIK;
    UX = XMX * SINUK + COSNOK * COSUK;
    UY = XMY * SINUK + SINNOK * COSUK;
    UZ = SINIK * SINUK;
    VX = XMX * COSUK - COSNOK * SINUK;
    VY = XMY * COSUK - SINNOK * SINUK;
    VZ = SINIK * COSUK;

    /* POSITION AND VELOCITY */

    E1_X = RK * UX;
    E1_Y = RK * UY;
    E1_Z = RK * UZ;
    E1_XDOT = RDOTK * UX + RFDOTK * VX;
    E1_YDOT = RDOTK * UY + RFDOTK * VY;
    E1_ZDOT = RDOTK * UZ + RFDOTK * VZ;

    itsR[0] = E1_X    * C1_XKMPER / C1_AE / 1E6;
    itsR[1] = E1_Y    * C1_XKMPER / C1_AE / 1E6;
    itsR[2] = E1_Z    * C1_XKMPER / C1_AE / 1E6;
    itsV[0] = E1_XDOT * C1_XKMPER / C1_AE * C1_XMNPDA / 86400.;
    itsV[1] = E1_YDOT * C1_XKMPER / C1_AE * C1_XMNPDA / 86400.;
    itsV[2] = E1_ZDOT * C1_XKMPER / C1_AE * C1_XMNPDA / 86400.;

    return;
  }


  /**
   * A helper routine to calculate the Greenwich sidereal time. */
  private final double THETAG(double EP) throws JPARSECException
  {
/*
   	// Original algorithm. Difference with JPARSEC is currently negligible, but this is
   	// only valid in a limited range of dates. Note here EP is not a JD
    double YR = (EP + 2.E-7) * 1.E-3;
    int JY = (int)YR;
    double D = EP - JY * 1.E3;
    //if (JY < 57)
    	JY = JY + 100; // INCORRECT: Only valid if reference year is 2100>y>=2000 (orbital elements prior to 2000 will be wrong)

		double N = (JY - 69) / 4;
	    if (JY < 70) N = (JY - 72) / 4;
	    E1_DS50 = 7305. + 365. * (JY - 70) + N + D;
	    double gst = Functions.normalizeRadians(1.72944494 + 6.3003880987 * E1_DS50);
	    return gst;
*/
	  try {
			TimeElement time = new TimeElement(EP, SCALE.UNIVERSAL_TIME_UTC);
			CityElement city = City.findCity("Madrid");
			ObserverElement observer = ObserverElement.parseCity(city);
			EphemerisElement eph = new EphemerisElement(TARGET.NOT_A_PLANET, EphemerisElement.COORDINATES_TYPE.APPARENT,
					EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.GEOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
					EphemerisElement.FRAME.ICRF, EphemerisElement.ALGORITHM.MOSHIER);
			eph.correctForEOP = false;
			E1_DS50 = EP - 2433281.5; // new AstroDate(1950, 1, 0).jd();
			return SiderealTime.greenwichMeanSiderealTime(time, observer, eph);
	  } catch (Exception exc) {
		  throw new JPARSECException("Invalid epoch");
	  }
  }

  /**
   * Reads the orbital elements into internal variables.
   * @param sat The orbital elements for the satellite.
   * @throws JPARSECException If an error occurs.
   */

  private final void ReadNorad12(SatelliteOrbitalElement sat) throws JPARSECException
  {
    double t, A1, DEL1, AO, DELO, XNODP;

    AstroDate astro = new AstroDate(sat.year, AstroDate.JANUARY, sat.day);
    E1_EPOCH = astro.jd();
    E1_XNDT2O = sat.firstDerivative / Constant.TWO_PI;
    E1_XNDD6O = sat.secondDerivative;
    E1_BSTAR = sat.drag / C1_AE;

    E1_XINCL = sat.inclination;
    E1_XNODEO = sat.ascendingNodeRA;
    E1_EO = sat.eccentricity;
    E1_OMEGAO = sat.argumentOfPerigee;
    E1_XMO = sat.meanAnomaly;
    E1_XNO = sat.meanMotion;

    E1_XNO    = E1_XNO  / C1_XMNPDA;
    E1_XNDT2O = E1_XNDT2O * C2_TWOPI / C1_XMNPDA / C1_XMNPDA;
    E1_XNDD6O = E1_XNDD6O * C2_TWOPI / C1_XMNPDA / C1_XMNPDA / C1_XMNPDA;

    /* Figure out which side of 225 minutes the period is. */

    A1 = Math.pow(C1_XKE / E1_XNO, C1_TOTHRD);
    t = 1.5 * C1_CK2 * (3. * Math.cos(E1_XINCL) * Math.cos(E1_XINCL) - 1.)
      / Math.pow(1.-E1_EO*E1_EO, 1.5);
    DEL1 = t / (A1 * A1);
    AO = A1 * (1. - DEL1 * (.5 * C1_TOTHRD + DEL1 * (1. + 134. / 81. * DEL1)));
    DELO = t / (AO * AO);
    XNODP = E1_XNO / (1. + DELO);
    if (C2_TWOPI / XNODP >= 225.) {isDeep = true;} else {isDeep = false;}

	itsEpochJD = astro.jd();

    return;
  }
}
