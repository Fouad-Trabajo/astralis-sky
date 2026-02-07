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

import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.Functions;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.event.MainEvents;
import jparsec.ephem.event.MainEvents.EVENT_TIME;
import jparsec.ephem.event.SimpleEventElement.EVENT;
import jparsec.ephem.planets.EphemElement;
import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.math.Constant;
import jparsec.math.Interpolation;
import jparsec.observer.City;
import jparsec.observer.CityElement;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.util.JPARSECException;

/**
 * Class that performs some simple calculations about star properties, including
 * simple photometry and extinction.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Star
{
	// private constructor so that this class cannot be instantiated.
	private Star() {}

	/**
	 * Calculate luminosity ratio L1/L2.
	 *
	 * @param m1 Absolute magnitude of body 1.
	 * @param m2 Absolute magnitude of body 2.
	 * @return Luminosity ratio.
	 */
	public static double luminosityRatio(double m1, double m2)
	{
		double lum = Math.pow(10.0, (m2 - m1) / 2.5);
		return lum;
	}

	/**
	 * Computes the combined apparent magnitude of two objects.
	 * @param m1 Magnitude of body 1.
	 * @param m2 Magnitude of body 2.
	 * @return The combined magnitude.
	 */
	public static double combinedMagnitude(double m1, double m2)
	{
		if (m1 > m2) {
			double t = m1;
			m1 = m2;
			m2 = t;
		}
		double lum = Star.luminosityRatio(m1, m2);
		double m = m2 - 2.5 * Math.log10(1.0 + lum);
		return m;
	}

	/**
	 * Returns the magnitude difference between two fluxes measured in
	 * different bands for the same object.
	 * @param flux1 Flux for band 1.
	 * @param flux2 Flux for band 2.
	 * @return M1 - M2.
	 */
	public static double getMagnitudeDifference(double flux1, double flux2) {
		return -2.5 * Math.log10(flux1 / flux2);
	}

	/**
	 * Obtain absolute magnitude.
	 *
	 * @param ap_mag Apparent magnitude.
	 * @param dist Distance in parsec.
	 * @return Absolute magnitude.
	 */
	public static double absoluteMagnitude(double ap_mag, double dist)
	{
		double abs_mag = ap_mag + 5 - 5 * Math.log(dist) / Math.log(10.0);
		return abs_mag;
	}

	/**
	 * Gets the distance of a star.
	 *
	 * @param ap_mag Apparent magnitude.
	 * @param abs_mag Absolute magnitude.
	 * @return The distance in parsec.
	 */
	public static double distance(double ap_mag, double abs_mag)
	{
		double dist = Math.pow(10.0, (ap_mag + 5 - abs_mag) / 5.0);
		return dist;
	}

	/**
	 * Obtain distance from distance modulus.
	 * @param distanceModulus Distance modulus.
	 * @return Distance in pc.
	 */
	public static double getDistance(double distanceModulus)
	{
		double dist = Math.pow(10.0, (distanceModulus + 5) / 5.0);
		return dist;
	}

	/**
	 * Obtains the dynamical parallax in a double star system
	 * using Kepler's third law.
	 *
	 * @param a Distance (semi-major axis) in arcseconds.
	 * @param P Period in years.
	 * @param M Mass of the system in solar masses.
	 * @return Parallax in arcseconds.
	 */
	public static double dynamicalParallax(double a, double P, double M)
	{
		double par = a / (Math.pow(P * P * M, 1.0 / 3.0));
		return par;
	}

	/**
	 * Obtains gravitational energy of a body in J.
	 *
	 * @param M Mass in solar masses.
	 * @param r Radius in solar radii.
	 * @return Energy in J.
	 */
	public static double gravitationalEnergy(double M, double r)
	{
		double Eg = 3.0 * Constant.GRAVITATIONAL_CONSTANT * M * M * Constant.SUN_MASS * Constant.SUN_MASS / (5.0 * r * Constant.SUN_RADIUS * 1000.0);
		return Eg;
	}

	/**
	 * Obtains gravitational time scale, or the maximum time a given body could
	 * radiate energy during a contraction phase.
	 *
	 * @param M Mass in solar masses.
	 * @param r Radius in solar radii.
	 * @param L Luminosity in J/s.
	 * @return Time in seconds.
	 */
	public static double gravitationalTimeScale(double M, double r, double L)
	{
		double t = gravitationalEnergy(M, r) / L;
		return t;
	}

	/**
	 * Obtains Schwartzchild radius for an object of certain mass. If the radius
	 * is lower than this value, then the object is a black hole.
	 *
	 * @param M Mass in solar masses.
	 * @return Radius in solar radii.
	 */
	public static double schwartzchildRadius(double M)
	{
		double r = 2.0 * Constant.GRAVITATIONAL_CONSTANT * M * Constant.SUN_MASS / (Constant.SPEED_OF_LIGHT * Constant.SPEED_OF_LIGHT);

		return r / (1000.0 * Constant.SUN_RADIUS);
	}

	/**
	 * Obtain wind mass lost ratio in solar masses by year. The effect in
	 * stellar evolution is negligible except in high mass stars.
	 *
	 * @param n Proton density in cm^-3 at certain distance from the star.
	 *        Typical value of 7 for the Sun.
	 * @param v Proton velocity in km/s at the same distance. Typical value of
	 *        500 for the Sun.
	 * @param r The distance where n and v are measured in AU. Typical value of
	 *        1 for the Sun.
	 * @return Mass lose ratio in solar masses by year. Typical value of 5E-14
	 *         Msun/year for the Sun.
	 */
	public static double stellarWindMass(double n, double v, double r)
	{
		double dm_dt = 4.0 * Math.PI * r * r * Constant.AU * Constant.AU * n * 1.0E15 * Constant.H2_MASS * v / Constant.SUN_MASS;

		return dm_dt * Constant.SECONDS_PER_DAY * Constant.TROPICAL_YEAR;
	}

	/**
	 * Returns the mass of an object using Kepler's third law.
	 *
	 * @param a Distance of a measure in AU.
	 * @param P Orbital period at that position in years.
	 * @return Mass in solar masses.
	 */
	public static double kepler3rdLawOfMasses(double a, double P)
	{
		double M = P * P / (a * a * a);

		return M;
	}

	/**
	 * Obtain total flux of a black body at certain temperature.
	 *
	 * @param T Effective temperature.
	 * @return Flux in W/m^2.
	 */
	public static double blackBodyFlux(double T)
	{
		return Constant.STEFAN_BOLTZMANN_CONSTANT * Math.pow(T, 4.0);
	}

	/**
	 * Applies Tully-Fisher relation for obtaining the total luminosity of a spiral
	 * galaxy. This method calculates the luminosity ratio compared to the Milky Way
	 * and returned this value multiplied by 4.5E10 (the total luminosity of the
	 * Milky Way in Lsun as measured by Portinari 2005). The luminosity ratio is
	 * computed by means of the absolute magnitude of Milky Way and the other galaxy,
	 * computed with M = -9.5 log10 (v) + 2, using v = 250 km/s (Turner 2013) for
	 * the Milky Way.<BR>
	 * Al alternative approach for this result is given with the method to compute
	 * the luminosity using the Fabber-Jackson relation, that can also be applied to
	 * spirals.
	 *
	 * @param v Rotation velocity in km/s in the outer galaxy.
	 * @return Luminosity in solar units.
	 */
	public static double tullyFisherRelation(double v)
	{
		double Mmw = -9.5 * Math.log10(250) + 2;
		double Mga = -9.5 * Math.log10(v) + 2;
		double Lmw = 4.5 * 1E10; // Portinari 2005
		return Lmw * Star.luminosityRatio(Mga, Mmw);
		// See also http://www.astro.caltech.edu/~george/ay21/eaa/eaa-tfr.pdf
	}

	/**
	 * Applies Fabber-Jackson relation for obtaining the luminosity of an
	 * elliptical galaxy. This relation is a power law with index 4.55,
	 * calibrated using the Milky Way with a total luminosity of 4.5E10 Lsun
	 * (Portinari 2005), and circular velocity (or dispersion in case of
	 * ellipticals) of 250 km/s (Turner 2013). This method can also be applied
	 * as the Tully Fisher relation by providing the circular velocity instead
	 * of velocity dispersion.
	 *
	 * @param sigma Velocity dispersion in km/s in the inner region.
	 * @return Luminosity in solar units.
	 */
	public static double fabberJacksonRelation(double sigma)
	{
		double beta = 4.55;
		double L0 = 4.5E10;

		double k = L0 / Math.pow(250.0, beta);
		double L = k * Math.pow(sigma, beta);

		return L;
	}

	/**
	 * Calculates Sechter luminosity function.
	 *
	 * @param L Luminosity in solar units.
	 * @param alfa Slope parameter.
	 * @return Number of objects per unit of luminosity, dN/dL.
	 */
	public static double sechterRelation(double L, double alfa)
	{
		double L0 = 1.0E10;

		double phi0 = 1.0 / gammaFunction(1.0 + alfa);
		double phi = Math.pow(L / L0, alfa) * Math.exp(-L / L0) * phi0 / L0;

		return phi;
	}

	/**
	 * Obtains gamma function for evaluating Sechter luminosity function.
	 *
	 * @param alfa Slope parameter. Must be integer or half-integer.
	 * @return Value of the function.
	 */
	private static double gammaFunction(double alfa)
	{
		if (alfa <= 1)
			return 0.0;

		double g = 1.0;
		do
		{
			g = g * (alfa - 1.0);
			alfa = alfa - 1.0;
		} while (alfa > 1.0);

		if (alfa == 0.5)
			g = g * Math.sqrt(Math.PI);

		return g;
	}

	/**
	 * Obtains gravitational redshift.
	 *
	 * @param M Mass in solar masses.
	 * @param R Radio in solar radii.
	 * @return Redshift z.
	 */
	public static double gravitationalRedshift(double M, double R)
	{
		double z = -1.0 + Math
				.sqrt(1.0 / (1.0 - 2.0 * M * Constant.SUN_MASS * Constant.GRAVITATIONAL_CONSTANT / (R * Constant.SUN_RADIUS * 1000.0 * Constant.SPEED_OF_LIGHT * Constant.SPEED_OF_LIGHT)));

		return z;
	}

	/**
	 * Obtains cosmological redshift.
	 *
	 * @param v Velocity of the source in m/s.
	 * @return Redshift z.
	 */
	public static double cosmologicalRedshift(double v)
	{
		double z = -1.0 + Math.sqrt((Constant.SPEED_OF_LIGHT + v) / (Constant.SPEED_OF_LIGHT - v));

		return z;
	}

	/**
	 * Obtains the contribution of the motion of the Sun in the galaxy to the redshift.
	 * It is supposed that the Sun moves at 250 km/s (see Turner 2013) towards a direction
	 * l = 90&deg;, b = 0&deg;, in galactic coordinates.
	 *
	 * @param loc Galactic coordination of the source.
	 * @return Contribution of the Sun movement in the galaxy to the redshift
	 * towards the given direction.
	 */
	public static double galacticRedshift(LocationElement loc)
	{
		double dz = (250.0 / Constant.SPEED_OF_LIGHT) * Math.sin(loc.getLongitude()) * Math.cos(loc.getLatitude());

		return dz;
	}

	/**
	 * Obtain wavelength of the peak emission of a black body applying Wien's
	 * approximation.
	 *
	 * @param T Temperature in K.
	 * @return Wavelength in m.
	 */
	public static double wienApproximation(double T)
	{
		return Constant.WIEN_CONSTANT / T;
	}

	/**
	 * Evaluates Planck's function.
	 *
	 * @param T Temperature in K.
	 * @param lambda Wavelength in m.
	 * @return B in Jy/sr.
	 */
	public static double blackBody(double T, double lambda)
	{
		double nu = Constant.SPEED_OF_LIGHT / lambda;
		double B = 2.0 * (Constant.PLANCK_CONSTANT / Constant.ERGIO_TO_JULE) * Math.pow(nu, 3.0) / (1.0E4 * Constant.SPEED_OF_LIGHT * Constant.SPEED_OF_LIGHT);
		B = B / (Math.exp(Constant.PLANCK_CONSTANT * nu / (Constant.BOLTZMANN_CONSTANT * T)) - 1.0);

		B = B * Constant.ERG_S_CM2_HZ_TO_JY;
		return B;
	}

	/**
	 * Evaluates Planck's function.
	 *
	 * @param T Temperature in K.
	 * @param nu Frequency in Hz.
	 * @return B in Jy/sr.
	 */
	public static double blackBodyNu(double T, double nu)
	{
		double B = 2.0 * (Constant.PLANCK_CONSTANT / Constant.ERGIO_TO_JULE) * Math.pow(nu, 3.0) / (1.0E4 * Constant.SPEED_OF_LIGHT * Constant.SPEED_OF_LIGHT);
		B = B / (Math.exp(Constant.PLANCK_CONSTANT * nu / (Constant.BOLTZMANN_CONSTANT * T)) - 1.0);

		B = B * Constant.ERG_S_CM2_HZ_TO_JY;
		return B;
	}

	/**
	 * Evaluates error of Planck's function, taking into account certain error
	 * in the temperature.
	 *
	 * @param T Temperature in K.
	 * @param dT Temperature error in K.
	 * @param lambda Wavelength in m.
	 * @return B error in Jy/sr.
	 */
	public static double blackBodyFluxError(double T, double dT, double lambda)
	{
		double nu = Constant.SPEED_OF_LIGHT / lambda;
		double B = 2.0 * (Constant.PLANCK_CONSTANT / Constant.ERGIO_TO_JULE) * Math.pow(nu, 3.0) / (1.0E4 * Constant.SPEED_OF_LIGHT * Constant.SPEED_OF_LIGHT);
		B = B / Math.pow(Math.exp(Constant.PLANCK_CONSTANT * nu / (Constant.BOLTZMANN_CONSTANT * T)) - 1.0, 2.0);
		B = B * Math.exp(Constant.PLANCK_CONSTANT * nu / (Constant.BOLTZMANN_CONSTANT * T));
		B *= -dT * Constant.PLANCK_CONSTANT * nu / (Constant.BOLTZMANN_CONSTANT * T * T);

		B = B * Constant.ERG_S_CM2_HZ_TO_JY;
		return B;
	}

	/**
	 * Obtains surface brightness of an object.
	 *
	 * @param m Magnitude.
	 * @param r Radius in arcseconds. If the object is not circular, an
	 *        equivalent value can be given so that the area (PI * r^2) in equal
	 *        to the area of the object in the sky.
	 * @return Brightness in mag/arcsecond^2, or 0 if radius is 0.
	 */
	public static double getSurfaceBrightness(double m, double r)
	{
		double bright = 0.0;
		if (r <= 0.0) return bright;

		double area = Math.PI * (r / 60.0) * (r / 60.0);
		double s1 = m + Math.log(area) / Math.log(Math.pow(100.0, 0.2));
		bright = s1 + 8.890756;
		return bright;
	}

	/**
	 * Obtain the luminosity using the mass-luminosity relation for main sequence
	 * stars. Formula used depends on if the mass is lower than 0.43, lower than 2,
	 * and lower than 20 or not. See http://en.wikipedia.org/wiki/Mass%E2%80%93luminosity_relation
	 * for the different formulae using Cassisi (2005) and Duric (2004).
	 * @param mass Mass in solar units.
	 * @return Luminosity in solar units.
	 */
	public static double getLuminosityFromMassLuminosityRelation(double mass)
	{
		if (mass < 0.43) return 0.23 * Math.pow(mass, 2.3);
		if (mass < 2) return Math.pow(mass, 4);
		if (mass > 20) return (1.5 * Math.pow(20, 3.5)) * Math.pow(mass / 20, 2); // Wikipedia says 1 as exponent here, but seems excesive
		return 1.5 * Math.pow(mass, 3.5);
	}
	/**
	 * Obtain the luminosity using the mass-luminosity relation for main sequence
	 * stars.
	 * @param luminosity Luminosity in solar units.
	 * @return Mass in solar units.
	 */
	public static double getMassFromMassLuminosityRelation(double luminosity)
	{
		if (luminosity < 0.033) return Math.pow(luminosity / 0.23, 1.0 / 2.3);
		if (luminosity < 16) return Math.pow(luminosity, 0.25);
		double ul = 1.5 * Math.pow(20, 3.5);
		if (luminosity > ul) return Math.pow((luminosity / ul), 1.0 / 2) * 20; //(luminosity * 20) / ul;
		return Math.pow(luminosity / 1.5, 1.0 / 3.5);
	}

	/**
	 * Obtains approximate star life time for a given mass.
	 * @param mass Star mass in solar units.
	 * @return Lifetime in years.
	 */
	public static double getStarLifeTime(double mass)
	{
		double time = 1.0E10 * mass / getLuminosityFromMassLuminosityRelation(mass);
		return time;
	}

	/**
	 * Obtains star radius.
	 *
	 * @param luminosity Luminosity in solar units.
	 * @param temperature Temperature in K.
	 * @return Radius in solar radii.
	 */
	public static double getStarRadius(double luminosity, double temperature)
	{
		return Math.sqrt(luminosity * Constant.SUN_LUMINOSITY / (4.0 * Math.PI * Constant.STEFAN_BOLTZMANN_CONSTANT *
				Math.pow(temperature, 4.0))) / (Constant.SUN_RADIUS * 1000.0);
	}
	/**
	 * Obtains star luminosity.
	 *
	 * @param radius Radius in solar units.
	 * @param temperature Temperature in K.
	 * @return Luminosity in solar units.
	 */
	public static double getStarLuminosity(double radius, double temperature)
	{
		double luminosity =  Math.pow(radius * Constant.SUN_RADIUS * 1000.0, 2.0) *
			(4.0 * Math.PI * Constant.STEFAN_BOLTZMANN_CONSTANT * Math.pow(temperature, 4.0));

		return luminosity / Constant.SUN_LUMINOSITY;
	}

	/**
	 * Obtain distance modulus
	 * @param distance Distance in pc.
	 * @return Distance modulus.
	 */
	public static double getDistanceModulus(double distance)
	{
		double module = -5.0 + 5.0 * Math.log10(distance);
		return module;
	}

	/**
	 * Obtains the surface gravity.
	 * @param mass Mass in solar masses.
	 * @param radius Radius in solar radii.
	 * @return Gravity in m/s^2.
	 */
	public static double getSurfaceGravity(double mass, double radius)
	{
		double g = Constant.GRAVITATIONAL_CONSTANT * mass * Constant.SUN_MASS / (Math.pow(radius * Constant.SUN_RADIUS * 1000.0, 2.0));
		return g;
	}

	/**
	 * Obtains the magnitude of a star given the flux on it, the background, and the
	 * same properties (including magnitude) of a comparison star close to it.
	 * @param flux Flux of the star.
	 * @param skyBackground Flux of the background.
	 * @param fluxComparison Flux of a comparison star.
	 * @param skyBackgroundComparison Sky background around comparison star.
	 * @param magComparison Magnitude of a comparison star.
	 * @return Magnitude of the star.
	 */
	public static double getStarMagnitude(double flux, double skyBackground, double fluxComparison, double skyBackgroundComparison,
			double magComparison)
	{
		flux = flux - skyBackground;
		fluxComparison = fluxComparison - skyBackgroundComparison;
		double mag = 2.5 * Math.log(fluxComparison / flux) / Math.log(10.0) + magComparison;
		return mag;
	}

	/**
	 * Calculates the size of an impact crater.
	 * @param impactorDiameter Diameter of the impactor in m.
	 * @param impactorDensity Density of the impactor in kg/m^3. Usual value is 2000.
	 * @param impactorVelocity Velocity of the impactor in km/s. Usual value is 50.
	 * @param zenithAngle Zenith angle of the impactor trajectory in degrees.
	 * @return Diameter and depth of the impact crater in m.
	 */
	public static double[] impactCrater(double impactorDiameter, double impactorDensity,
			double impactorVelocity, double zenithAngle)
	{
		double IV=impactorVelocity * 1000.0;
		double VI=Math.PI * impactorDiameter * impactorDiameter * impactorDiameter / 6.0;
		double GF=Math.pow((Math.sin((90.0 - zenithAngle) * Constant.DEG_TO_RAD)), .33);
		double MI=impactorDensity * VI;
		double KE=.5 * MI * IV * IV;
		double KT=KE / 4.2E+12; // impactor kinetic energy in kT TNT
		double diameter=2.0 * 18.0 * Math.pow(KT, 0.3) * GF;
		double depth=9.0 * Math.pow(KT, 0.3) * GF;
		return new double[] {diameter, depth};
	}

	/**
	 * Calculates the extinction in magnitudes due to Earth's
	 * atmosphere.
	 * @param z Zenith angle, in radians. Extinction is set to 0 if this
	 * value is above PI/2.0.
	 * @param h Height above sea level, in km.
	 * @param quality The 'quality' of the night, from 0 (summer-like
	 * conditions) to 10 (winter-like conditions).
	 * @return Extinction in magnitudes.
	 */
	public static double getExtinction(double z, double h, int quality) {
		if (z > Constant.PI_OVER_TWO) return 0.0;

		// I'm following http://www.icq.eps.harvard.edu/ICQExtinct.html
		// Air mass, Rozenberg (1966)
		double X = 1.0 / (Math.cos(z) + 0.025 * Math.exp(-11.0 * Math.cos(z)));
		// Extinction in magnitudes per unit mass due to Rayleigh scattering, Hayes and Latham 1975
		double Aray = 0.1451 * Math.exp(-h / 7.996);
		// Same due to aerosols, corrected after Angstrom (1961) and Schaefer (1992)
		double H = 1.5;
		double A0 = 0.065 - (quality * 0.03 / 10.0);
		double Aaer = A0 * Math.pow(0.51, -1.3) * Math.exp(-h / H);
		// Extinction due to ozone is less important and almost constant, Schaefer 1992
		double Aoz = 0.016;

		return X * (Aray + Aaer + Aoz);
	}

	/**
	 * Performs a simple photometry measurement.
	 * @param fluxIn Flux of the input star, background subtracted.
	 * @param fluxReference Flux of a reference star, background subtracted.
	 * @param magReference Magnitude of reference star.
	 * @return Magnitude of input star.
	 */
	public static double getStarMagnitude(double fluxIn, double fluxReference, double magReference) {
		double dm = Math.log10(fluxIn / fluxReference);
		double mag = magReference - dm * 2.5;
		return mag;
	}

	/**
	 * Transform from integer to string representation of a variable star
	 * designation. Method from Guide software.
	 * <BR>
	 * Variable star designations follow a rather ugly scheme, for historical
	 * reasons. The first 334 are labeled in the following order:
	 * <BR>
	 *
	 * <pre>
	 *   R  S  T  U  V  W  X  Y  Z RR RS RT RU RV RW RX RY RZ SS ST SU SV SW SX
	 *  SY SZ TT TU TV TW TX TY TZ UU UV UW UX UY UZ VV VW VX VY VZ WW WX WY WZ
	 *  XX XY XZ YY YZ ZZ AA AB AC AD AE AF AG AH AI AK AL AM AN AO AP AQ AR AS
	 *  AT AU AV AW AX AY AZ BB BC BD BE BF BG BH BI BK BL BM BN BO BP BQ BR BS
	 *  BT BU BV BW BX BY BZ CC CD CE CF CG CH CI CK CL CM CN CO CP CQ CR CS CT
	 *  CU CV CW CX CY CZ DD DE DF DG DH DI DK DL DM DN DO DP DQ DR DS DT DU DV
	 *  DW DX DY DZ EE EF EG EH EI EK EL EM EN EO EP EQ ER ES ET EU EV EW EX EY
	 *  EZ FF FG FH FI FK FL FM FN FO FP FQ FR FS FT FU FV FW FX FY FZ GG GH GI
	 *  GK GL GM GN GO GP GQ GR GS GT GU GV GW GX GY GZ HH HI HK HL HM HN HO HP
	 *  HQ HR HS HT HU HV HW HX HY HZ II IK IL IM IN IO IP IQ IR IS IT IU IV IW
	 *  IX IY IZ KK KL KM KN KO KP KQ KR KS KT KU KV KW KX KY KZ LL LM LN LO LP
	 *  LQ LR LS LT LU LV LW LX LY LZ MM MN MO MP MQ MR MS MT MU MV MW MX MY MZ
	 *  NN NO NP NQ NR NS NT NU NV NW NX NY NZ OO OP OQ OR OS OT OU OV OW OX OY
	 *  OZ PP PQ PR PS PT PU PV PW PX PY PZ QQ QR QS QT QU QV QW QX QY QZ
	 * </pre>
	 *
	 * <BR>
	 * The first one found in a constellation is 'R (constellation name)'; followed
	 * by 'S', 'T', ... 'Z'. That allows up to nine variables per constellation; the
	 * tenth gets labeled 'RR', followed by 'RS', 'RT', 'RU',... 'RZ'; then 'SS',
	 * 'ST', 'SU'... 'SZ', 'TT'... 'TZ', and so on, up to 'ZZ'. This allows a
	 * further 9+8+7+6+5+4+3+2+1=45 stars to be labeled. The letters are always 'R'
	 * through 'Z', and the second letter is never alphabetically before the first.
	 * <BR>
	 * Following this, we cycle the first letter back to 'A'. This gives 'AA', 'AB',
	 * 'AC', ... 'AZ'; 'BB', 'BC', 'BD', ... 'BZ'; and, eventually, 'QQ', 'QR', ...
	 * 'QZ'. For some reason, 'J' is always skipped.
	 * <BR>
	 * Following this cycle for 334 variable stars, they are simply labeled
	 * "V335", "V336", etc.
	 *
	 * @param var_no Variable number.
	 * @return String representation.
	 * @throws JPARSECException For input below 1.
	 */
	public static String getVariableStarDesignation(int var_no)
	throws JPARSECException {
		if (var_no > 334) return "V"+var_no;
		if (var_no < 1) throw new JPARSECException("invalid variable number, must be greater than 0.");

		char buff1 = ' ', buff2 = ' ';
		String var = "";
		int i, curr_no = 10;

		if (var_no < 10)
		{
			buff1 = (char) ('R' + var_no - 1);
			var = String.valueOf(buff1);
		} else if (var_no > 334)
		{
			buff1 = 'V';
			var = String.valueOf(buff1);
			var += Integer.toString(var_no);
		} else
		/* two-letter abbr */
		{
			for (i = 'R'; i <= 'Z' && curr_no + ('Z' - i) < var_no; i++)
			{
				curr_no += 'Z' - i + 1;
			}
			/* gotta get weird to allow for fact that J isn't used */
			if (i > 'Z') /* in variable star designators */
			{
				for (i = 'A'; i < 'Q' && curr_no + ('Y' - i) < var_no; i++)
				{
					curr_no += 'Z' - i;
				}
			}
			buff1 = (char) i;
			buff2 = (char) (i + var_no - curr_no);
			/* more weirdness due to missing J: bump up letters J-Q */
			if (buff1 < 'R' && buff1 >= 'J')
				buff1++;
			if (buff1 < 'R' && buff2 >= 'J')
				buff2++;

			var = String.valueOf(buff1);
			var += String.valueOf(buff2);
		}

		return var;
	}

	/**
	 * Obtain the variable number from it's string representation.
	 * Method from Guide software.
	 * <BR>
	 * Variable star designations follow a rather ugly scheme, for historical
	 * reasons. The first 334 are labeled in the following order:
	 * <BR>
	 *
	 * <pre>
	 *   R  S  T  U  V  W  X  Y  Z RR RS RT RU RV RW RX RY RZ SS ST SU SV SW SX
	 *  SY SZ TT TU TV TW TX TY TZ UU UV UW UX UY UZ VV VW VX VY VZ WW WX WY WZ
	 *  XX XY XZ YY YZ ZZ AA AB AC AD AE AF AG AH AI AK AL AM AN AO AP AQ AR AS
	 *  AT AU AV AW AX AY AZ BB BC BD BE BF BG BH BI BK BL BM BN BO BP BQ BR BS
	 *  BT BU BV BW BX BY BZ CC CD CE CF CG CH CI CK CL CM CN CO CP CQ CR CS CT
	 *  CU CV CW CX CY CZ DD DE DF DG DH DI DK DL DM DN DO DP DQ DR DS DT DU DV
	 *  DW DX DY DZ EE EF EG EH EI EK EL EM EN EO EP EQ ER ES ET EU EV EW EX EY
	 *  EZ FF FG FH FI FK FL FM FN FO FP FQ FR FS FT FU FV FW FX FY FZ GG GH GI
	 *  GK GL GM GN GO GP GQ GR GS GT GU GV GW GX GY GZ HH HI HK HL HM HN HO HP
	 *  HQ HR HS HT HU HV HW HX HY HZ II IK IL IM IN IO IP IQ IR IS IT IU IV IW
	 *  IX IY IZ KK KL KM KN KO KP KQ KR KS KT KU KV KW KX KY KZ LL LM LN LO LP
	 *  LQ LR LS LT LU LV LW LX LY LZ MM MN MO MP MQ MR MS MT MU MV MW MX MY MZ
	 *  NN NO NP NQ NR NS NT NU NV NW NX NY NZ OO OP OQ OR OS OT OU OV OW OX OY
	 *  OZ PP PQ PR PS PT PU PV PW PX PY PZ QQ QR QS QT QU QV QW QX QY QZ
	 * </pre>
	 *
	 * <BR>
	 * The first one found in a constellation is 'R (constellation name)'; followed
	 * by 'S', 'T', ... 'Z'. That allows up to nine variables per constellation; the
	 * tenth gets labelled 'RR', followed by 'RS', 'RT', 'RU',... 'RZ'; then 'SS',
	 * 'ST', 'SU'... 'SZ', 'TT'... 'TZ', and so on, up to 'ZZ'. This allows a
	 * further 9+8+7+6+5+4+3+2+1=45 stars to be labelled. The letters are always 'R'
	 * through 'Z', and the second letter is never alphabetically before the first.
	 * <BR>
	 * Following this, we cycle the first letter back to 'A'. This gives 'AA', 'AB',
	 * 'AC', ... 'AZ'; 'BB', 'BC', 'BD', ... 'BZ'; and, eventually, 'QQ', 'QR', ...
	 * 'QZ'. For some reason, 'J' is always skipped.
	 * <BR>
	 * Following this lunacy for 334 variable stars, they are simply labelled
	 * "V335", "V336", etc.
	 *
	 * @param name String representation of the variable.
	 * @return Integer value with the variable number.
	 */
	public static int getVariableStarNumber(String name)
	{
		String designation = name.trim();
		designation = FileIO.getField(1, designation, " ", true);
		if (designation.startsWith("V")) {
			try {
				int n = Integer.parseInt(designation.substring(1));
				if (n < 1) throw new JPARSECException("invalid variable number, must be greater than 0.");
				return n;
			} catch (Exception exc) {}
		}

		int first, second, rval = -2;

		designation = designation.toUpperCase().trim();
		char desig[] = designation.toCharArray();

		first = desig[0];
		second = first;
		if (designation.length() > 1)
			second = desig[1];
		int len = designation.length();

		switch (len)
		{
		case 1:
			if (first >= 'R' && first <= 'Z')
				rval = first - 'R';
			if (desig[0] >= 'A' && desig[0] <= 'Q')
				rval = 9200 + desig[0] - 'A';
			if (desig[0] >= 'a' && desig[0] <= 'q' || desig[0] == 'u')
				rval = 9100 + desig[0] - 'a';
			break;
		case 2:
			if (second >= first && second <= 'Z')
				if (first >= 'R') /* RR...ZZ */
				{
					first -= 'R';
					second -= 'R';
					rval = first * 8 - first * (first - 1) / 2 + 9 + second;
				} else if (first != 'J' && second != 'J' && first >= 'A')
				{ /* AA...QQ */
					first -= 'A';
					if (first > 8)
						first--;
					second -= 'A';
					if (second > 8)
						second--;
					rval = first * 24 - first * (first - 1) / 2 + 9 + 45 + second;
				}
			break;
		default:
			if (first == 'V')
			{
				rval = 0;

				for (int i = 1; i <= desig[i]; i++)
					if (desig[i] >= '0' && desig[i] <= '9')
						rval = rval * 10 + desig[i] - '0';
				rval--;
			}
			break;
		}

		rval++;
		return (rval);
	}

	/**
	 * Based on the cosmology calculator by Edward L. Wright,
	 * javascript version at http://www.astro.ucla.edu/~wright/CosmoCalc.html.
	 * For an open universe set WV to 0 and WM &lt; 1. For a flat universe to WV = 1.0 - WM.
	 * @param H0 Hubble constant, recommended value is 71 km/(s Mpc).
	 * @param WM Omega-0 (omega of matter), recommended value is 0.27.
	 * @param WV Omega-vacuum or lambda (due to cosmological constant), recommended value is 0.73.
	 * @param z Redshift of the observed object, from 0 (local universe).
	 * @return A set of 11 values containing:<BR>
	 * Time elapsed from Big Bang (age of universe).<BR>
	 * The age of the universe at input redshift z.<BR>
	 * The light travel time from the object at z.<BR>
	 * The comoving radial distance, which goes into Hubble's law, in Mpc and Gly (2 values).<BR>
	 * The comoving volume within redshift z in Gpc^3.<BR>
	 * The angular size distance in Mpc and Gly (2 values).<BR>
	 * Scale factor when imaging the object, in kpc/".<BR>
	 * The luminosity distance in Mpc and Gly (2 values).<BR>
	 */
	public static double[] cosmology(double H0, double WM, double WV, double z) {
		int i = 0;	// index
		int n = 1000;	// number of points in integrals
		int nda = 1;	// number of digits in angular size distance
		double h = H0 / 100.0;
		double WR = 4.165E-5 / (h * h); // Omega(radiation), includes 3 massless neutrino species, T0 = 2.72528
		double WK = 1.0 - WM - WR - WV; // Omega curvaturve = 1-Omega(total)
		double c = Constant.SPEED_OF_LIGHT * 0.001; // velocity of light in km/sec
		double Tyr = 977.8; // coefficent for converting 1/H into Gyr
		double DTT = 0.5;	// time from z to now in units of 1/H0
		double DTT_Gyr = 0.0;	// value of DTT in Gyr
		double age = 0.5;	// age of Universe in units of 1/H0
		double age_Gyr = 0.0;	// value of age in Gyr
		double zage = 0.1;	// age of Universe at redshift z in units of 1/H0
		double zage_Gyr = 0.0;	// value of zage in Gyr
		double DCMR = 0.0;	// comoving radial distance in units of c/H0
		double DCMR_Mpc = 0.0;
		double DCMR_Gyr = 0.0;
		double DA = 0.0;	// angular size distance
		double DA_Mpc = 0.0;
		double DA_Gyr = 0.0;
		double kpc_DA = 0.0;
		double DL = 0.0;	// luminosity distance
		double DL_Mpc = 0.0;
		double DL_Gyr = 0.0;	// DL in units of billions of light years
		double V_Gpc = 0.0;
		double a = 1.0;	// 1/(1+z), the scale factor of the Universe
		double az = 0.5;	// 1/(1+z(object))

		az = 1.0 / (1 + 1.0 * z);
		age = 0;
		for (i = 0; i < n; i++) {
			a = az * (i + 0.5) / n;
		    age = age + 1.0 / Math.sqrt(WK + (WM / a) + (WR / (a * a)) + (WV * a * a));
		};
		zage = az * age / n;

		// Correction for annihilation of particles not present now like e+/e-
		// added 13-Aug-03 based on T_vs_t.f
		double lpz = Math.log((1.0 + 1.0 * z)) / Math.log(10.0);
		double dzage = 0;
		if (lpz >  7.500) dzage = 0.002 * (lpz -  7.500);
		if (lpz >  8.000) dzage = 0.014 * (lpz -  8.000) +  0.001;
		if (lpz >  8.500) dzage = 0.040 * (lpz -  8.500) +  0.008;
		if (lpz >  9.000) dzage = 0.020 * (lpz -  9.000) +  0.028;
		if (lpz >  9.500) dzage = 0.019 * (lpz -  9.500) +  0.039;
		if (lpz > 10.000) dzage = 0.048;
		if (lpz > 10.775) dzage = 0.035 * (lpz - 10.775) +  0.048;
		if (lpz > 11.851) dzage = 0.069 * (lpz - 11.851) +  0.086;
		if (lpz > 12.258) dzage = 0.461 * (lpz - 12.258) +  0.114;
		if (lpz > 12.382) dzage = 0.024 * (lpz - 12.382) +  0.171;
		if (lpz > 13.055) dzage = 0.013 * (lpz - 13.055) +  0.188;
		if (lpz > 14.081) dzage = 0.013 * (lpz - 14.081) +  0.201;
		if (lpz > 15.107) dzage = 0.214;
		zage = zage * Math.pow(10.0, dzage);
		zage_Gyr = (Tyr / H0) * zage;
		DTT = 0.0;
		DCMR = 0.0;

		// Do integral over a=1/(1+z) from az to 1 in n steps, midpoint rule
		for (i = 0; i != n; i++) {
			a = az + (1.0 - az) * (i + 0.5) / n;
			double adot = Math.sqrt(WK + (WM / a) + (WR / (a * a)) + (WV * a * a));
		    DTT = DTT + 1.0 / adot;
		    DCMR = DCMR + 1.0 / (a * adot);
		};
		DTT = (1.0 - az) * DTT / n;
		DCMR = (1.0 - az) * DCMR / n;
		age = DTT + zage;
		age_Gyr = age * (Tyr / H0);
		DTT_Gyr = (Tyr / H0) * DTT;
		DCMR_Gyr = (Tyr / H0) * DCMR;
		DCMR_Mpc = (c / H0) * DCMR;
		DA = az * DCMT(WK, DCMR);
		DA_Mpc = (c / H0) * DA;
		kpc_DA = DA_Mpc / 206.264806;
		DA_Gyr = (Tyr / H0) * DA;
		DL = DA / (az * az);
		DL_Mpc = (c / H0) * DL;
		DL_Gyr = (Tyr / H0) * DL;
		V_Gpc = Constant.FOUR_PI * Math.pow(0.001 * c / H0, 3.0) * VCM(WK, DCMR);

		if (DA_Mpc < 100) nda = 3;
		double roundFactor = Math.pow(10, nda);
		return new double[] {age_Gyr, zage_Gyr, DTT_Gyr, DCMR_Mpc, DCMR_Gyr, V_Gpc,
				((int) (DA_Mpc * roundFactor)) / roundFactor, ((int) (DA_Gyr * roundFactor * 1E3)) / (1E3 * roundFactor),
				kpc_DA, DL_Mpc, DL_Gyr};
	}

	// Tangential comoving distance
	private static double DCMT(double WK, double DCMR) {
	  double ratio = 1.0;
	  double x = Math.sqrt(Math.abs(WK)) * DCMR;
	  if (x > 0.1) {
	    ratio =  (WK > 0) ? 0.5 * (Math.exp(x) - Math.exp(-x)) / x : Math.sin(x) / x;
	    return ratio * DCMR;
	  };
	  double y = x * x;

	  // Statement below fixed 13-Aug-03 to correct sign error in expansion
	  if (WK < 0) y = -y;
	  ratio = 1.0 + y / 6.0 + y * y/ 120.0;
	  return ratio * DCMR;
	}

	// Comoving volume computation
	private static double VCM(double WK, double DCMR) {
	  double ratio = 1.0;
	  double x = Math.sqrt(Math.abs(WK)) * DCMR;
	  if (x > 0.1) {
	    ratio =  (WK > 0) ? (0.125 * (Math.exp(2.0 * x) - Math.exp(-2.0 * x)) - x / 2.0) / (x * x * x / 3.0) :
	    	(x / 2.0 - Math.sin(2.0 * x) / 4.0) / (x * x * x / 3.0);
	    return ratio * DCMR * DCMR * DCMR / 3.0;
	  };
	  double y = x * x;

	  // Statement below fixed 13-Aug-03 to correct sign error in expansion
	  if (WK < 0) y = -y;
	  ratio = 1.0 + y / 5.0 + (2.0 / 105.0) * y * y;
	  return ratio * DCMR * DCMR * DCMR / 3.0;
	}

	/**
	 * Based on the cosmology calculator by Edward L. Wright,
	 * javascript version at http://www.astro.ucla.edu/~wright/CosmoCalc.html.
	 * For an open universe set WV to 0 and WM &lt; 1. For a flat universe to WV = 1.0 - WM.
	 * @param H0 Hubble constant, recommended value is 71 km/(s Mpc).
	 * @param WM Omega-0 (omega of matter), recommended value is 0.27.
	 * @param WV Omega-vacuum or lambda (due to consmological constant), recommended value is 0.73.
	 * @param tltt Light travel time from the object.
	 * @return Redshift of the object to be used in {@linkplain Star#cosmology(double, double, double, double)}.
	 */
	public static double getRedshiftFromLightTravelTime(double H0, double WM, double WV, double tltt) {
		double h = H0 / 100.0;
		double WR = 4.165E-5 / (h * h); // Omega(radiation), includes 3 massless neutrino species, T0 = 2.72528
		double WK = 1.0 - WM - WR - WV; // Omega curvaturve = 1-Omega(total)
		double Tyr = 977.8; // coefficent for converting 1/H into Gyr

		double ltt = H0 * tltt / Tyr;
		double age = 0;
		double ad = 1, adot = 0.0;
		for (double a = 0.9995; age < ltt; a = a-0.001) {
			adot = Math.sqrt(WK + (WM / a) + (WR / (a * a)) + (WV * a * a));
		    age = age + 0.001 / adot;
		    ad = ad - 0.001;
		    if (a < 0.001) {
		    	age = ltt + 100;
		    	adot = 0;
		    	ad = 1.0E-9;
		    };
		};
		double z = 1.0 / (ad + (age - ltt) * adot) - 1.0;
		return z;
	}

	/**
	 * Returns the B-V color index of a perfect black body.
	 * @param T Effective temperature in K.
	 * @return B-V.
	 */
	public static double getBlackBodyBminusV(double T) {
		return -0.715 + 7090.0 / T;
	}

	/**
	 * Returns the effective temperature of a perfect black body.
	 * @param BminusV The B-V color index.
	 * @return The effective temperature in K.
	 */
	public static double getBlackBodyT(double BminusV) {
		return 7090.0 / (0.715 + BminusV);
	}

	/**
	 * The set of luminosity classes.
	 */
	public enum LUMINOSITY_CLASS {
		/** Class V or main sequence. */
		MAIN_SEQUENCE_V,
		/** Class III or giants. */
		GIANTS_III,
		/** Class I or supergiants */
		SUPERGIANTS_I;

		/**
		 * Returns the set of B-V values.
		 * @return B-V values.
		 * @throws JPARSECException If an error occurs.
		 */
		public double[] getBV() throws JPARSECException {
			if (this == MAIN_SEQUENCE_V) return DataSet.toDoubleValues(DataSet.extractColumnFromTable(MAIN_SEQUENCE, " ", 0));
			// SpT Mv B-V   U-B   V-R   R-I  Teff  BC
			if (this == GIANTS_III) return DataSet.toDoubleValues(DataSet.extractColumnFromTable(CLASS_III, " ", 2));
			return DataSet.toDoubleValues(DataSet.extractColumnFromTable(CLASS_I, " ", 2));
		}
		/**
		 * Returns the set of Teff in log scale.
		 * @return Teff values.
		 * @throws JPARSECException If an error occurs.
		 */
		public double[] getLogT() throws JPARSECException {
			if (this == MAIN_SEQUENCE_V) return DataSet.toDoubleValues(DataSet.extractColumnFromTable(MAIN_SEQUENCE, " ", 1));
			// SpT Mv B-V   U-B   V-R   R-I  Teff  BC
			double log10 = Math.log(10.0);
			if (this == GIANTS_III) {
				double out[] = DataSet.toDoubleValues(DataSet.extractColumnFromTable(CLASS_III, " ", 6));
				for (int i=0; i<out.length; i++) {
					out[i] = Math.log(out[i]) / log10;
				}
				return out;
			}
			double out[] = DataSet.toDoubleValues(DataSet.extractColumnFromTable(CLASS_I, " ", 6));
			for (int i=0; i<out.length; i++) {
				out[i] = Math.log(out[i]) / log10;
			}
			return out;
		}
		/**
		 * Returns the set of bolometric corrections.
		 * @return BC values.
		 * @throws JPARSECException If an error occurs.
		 */
		public double[] getBC() throws JPARSECException {
			if (this == MAIN_SEQUENCE_V) return DataSet.toDoubleValues(DataSet.extractColumnFromTable(MAIN_SEQUENCE, " ", 2));
			// SpT Mv B-V   U-B   V-R   R-I  Teff  BC
			if (this == GIANTS_III) return DataSet.toDoubleValues(DataSet.extractColumnFromTable(CLASS_III, " ", 7));
			return DataSet.toDoubleValues(DataSet.extractColumnFromTable(CLASS_I, " ", 7));
		}

		/**
		 * Returns the set of absolute magnitudes.
		 * @return Mv values.
		 * @throws JPARSECException If an error occurs.
		 */
		public double[] getMv() throws JPARSECException {
			if (this == MAIN_SEQUENCE_V) {
				String dataset[] = new String[0];
				for (int i=0; i<MAIN_SEQUENCE.length; i++) {
					int n = FileIO.getNumberOfFields(MAIN_SEQUENCE[i], " ", true);
					if (n > 4) dataset = DataSet.addStringArray(dataset, new String[] {MAIN_SEQUENCE[i]});
				}
				return DataSet.toDoubleValues(DataSet.extractColumnFromTable(dataset, " ", 4));
			}
			if (this == GIANTS_III) return DataSet.toDoubleValues(DataSet.extractColumnFromTable(CLASS_III, " ", 1));
			return DataSet.toDoubleValues(DataSet.extractColumnFromTable(CLASS_I, " ", 1));
		}
		/**
		 * Returns the set of B-V values to be used with the Mv values.
		 * @return B-V values.
		 * @throws JPARSECException If an error occurs.
		 */
		public double[] getBVForMv() throws JPARSECException {
			if (this == MAIN_SEQUENCE_V) {
				String dataset[] = new String[0];
				for (int i=0; i<MAIN_SEQUENCE.length; i++) {
					int n = FileIO.getNumberOfFields(MAIN_SEQUENCE[i], " ", true);
					if (n > 4) dataset = DataSet.addStringArray(dataset, new String[] {MAIN_SEQUENCE[i]});
				}
				return DataSet.toDoubleValues(DataSet.extractColumnFromTable(dataset, " ", 0));
			}
			return getBV();
		}
	};

	/**
	 * Returns the B-V color index of a star.
	 * Based on Flower P.J. Astrophys. J. 469, 355 (1996) and A. Cox (1982).
	 * @param T Effective temperature in K. NaN is returned in case no
	 * solution is possible (T &gt; 56000 or T &lt; 3000) and luminosity class is main sequence,
	 * otherwise an error can be thrown.
	 * @param lclass The luminosity class for the star.
	 * @return B-V.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double getStarBminusV(double T, LUMINOSITY_CLASS lclass) throws JPARSECException {
		if (lclass == LUMINOSITY_CLASS.MAIN_SEQUENCE_V && (T > 56000 || T < 3000)) return Double.NaN;

		Interpolation interp = new Interpolation(lclass.getLogT(), lclass.getBV(), false);
		return interp.splineInterpolation(Math.log10(T));
	}

	/**
	 * Returns the effective temperature of a star. Based on Flower P.J.,
	 * Astrophys. J. 469, 355 (1996), and Cox (1982).
	 * @param BminusV The B-V color index.
	 * @param lclass The luminosity class for the star.
	 * @return The effective temperature in K. NaN is returned in case no solution
	 * is found (B-V &lt; -0.35 or B-V &gt; 1.80) and luminosity class is main sequence,
	 * otherwise an error can be thrown.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double getStarTeff(double BminusV, LUMINOSITY_CLASS lclass) throws JPARSECException {
		if (lclass == LUMINOSITY_CLASS.MAIN_SEQUENCE_V && (BminusV < -0.35 || BminusV > 1.80)) return Double.NaN;
		Interpolation interp = new Interpolation(lclass.getBV(), lclass.getLogT(), false);
		return Math.pow(10.0, interp.splineInterpolation(BminusV));
	}

	/**
	 * Returns the bolometric correction for a star, so
	 * that Mbol = Mv + BC. Based on Flower P.J. Astrophys. J. 469, 355 (1996)
	 * and Cox (1982).
	 * @param T Effective temperature in K.
	 * @param lclass The luminosity class for the star.
	 * @return BC. NaN is returned in case no solution is possible (T &gt; 56000 or T &lt; 3000)
	 * and luminosity class is main sequence, otherwise an error can be thrown.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double getStarBolometricCorrection(double T, LUMINOSITY_CLASS lclass) throws JPARSECException {
		if (lclass == LUMINOSITY_CLASS.MAIN_SEQUENCE_V && (T > 56000 || T < 3000)) return Double.NaN;
		Interpolation interp = new Interpolation(lclass.getLogT(), lclass.getBC(), false);
		return interp.splineInterpolation(Math.log10(T));
	}

	/**
	 * Obtains stellar bolometric luminosity for a star in the main sequence, using
	 * the bolometric correction and the bolometric magnitude of the Sun = 4.75.
	 *
	 * @param mv Star absolute magnitude.
	 * @param T Effective temperature in K.
	 * @param lclass The luminosity class for the star.
	 * @return Luminosity in solar units.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double getStarLuminosityUsingBolometricCorrection(double mv, double T, LUMINOSITY_CLASS lclass) throws JPARSECException
	{
		double Mbol = mv + Star.getStarBolometricCorrection(T, lclass);
		double MbolSun = 4.75;
		double L = Math.pow(10.0, (MbolSun - Mbol) / 2.5);
		return L;
	}

	/**
	 * Obtains the bolometric luminosity of a star in the main sequence. This method
	 * only needs the effective temperature, which is used to compute the
	 * star B-V color and then its absolute magnitude. Then the bolometric
	 * correction is used to compute the luminosity relative to the Sun.
	 *
	 * @param T Effective temperature in K for the star in the main sequence.
	 * @param lclass The luminosity class for the star.
	 * @return Luminosity in solar units.
	 * @throws JPARSECException If an error occurs, but should never happen.
	 */
	public static double getStarLuminosityUsingBolometricCorrection(double T, LUMINOSITY_CLASS lclass) throws JPARSECException
	{
		return Star.getStarLuminosityUsingBolometricCorrection(getStarAbsoluteMagnitude(getStarBminusV(T, lclass), lclass), T, lclass);
	}

	/**
	 * Returns the absolute magnitude of a star given its
	 * B-V color index. Based on Flower (1996), Pickles (1998), and Cox (1982).
	 * @param BminusV B-V.
	 * @param lclass The luminosity class for the star.
	 * @return Absolute magnitude. NaN is returned for a B-V out of range
	 * -0.35 to 1.80 and luminosity class main sequence, otherwise an error can
	 * be thrown.
	 * @throws JPARSECException Should never happen.
	 */
	public static double getStarAbsoluteMagnitude(double BminusV, LUMINOSITY_CLASS lclass) throws JPARSECException {
		if (lclass == LUMINOSITY_CLASS.MAIN_SEQUENCE_V && (BminusV < -0.35 || BminusV > 1.8)) return Double.NaN;
		Interpolation interp = new Interpolation(lclass.getBVForMv(), lclass.getMv(), false);
		return interp.splineInterpolation(BminusV);
		// Note data from Schmidt-Kaler, http://xoomer.virgilio.it/hrtrace/Sk.htm, is outdated (1982)
	}

	/**
	 * Returns the B-V given the absolute magnitude of the star. A table is used to compute
	 * the best B-V by interpolating.
	 * @param Mv Absolute magnitude.
	 * @param lclass The luminosity class for the star.
	 * @return B-V. NaN is returned for an absolute magnitude out of range from
	 * -6.2 to 16.5 and luminosity class main sequence, otherwise an error can be thrown.
	 * @throws JPARSECException Should never happen.
	 */
	public static double getStarBminusVFromMv(double Mv, LUMINOSITY_CLASS lclass) throws JPARSECException {
		if (lclass == LUMINOSITY_CLASS.MAIN_SEQUENCE_V && (Mv < -6.2 || Mv > 16.5)) return Double.NaN;
		Interpolation interp = new Interpolation(lclass.getMv(), lclass.getBVForMv(), false);
		return interp.splineInterpolation(Mv);
		// Note data from Schmidt-Kaler, http://xoomer.virgilio.it/hrtrace/Sk.htm, is outdated (1982)
	}

	/**
	 * Returns the spectral type for a star in the main sequence given its effective temperature.
	 * @param T Effective temperature in K.
	 * @param ndec Number of decimal positions.
	 * @return Spectral type as character + number, for instance O5. Null in case T is out of range
	 * 3000 to 56000.
	 * In case ndec is greater than 0, a possible output for ndec = 1 would be O5.1.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String getSpectralType(double T, int ndec) throws JPARSECException {
		if (T > 56000 || T < 3000) return null;

		String dataset[] = new String[0];
		for (int i=0; i<MAIN_SEQUENCE.length; i++) {
			int n = FileIO.getNumberOfFields(MAIN_SEQUENCE[i], " ", true);
			if (n > 4) dataset = DataSet.addStringArray(dataset, new String[] {MAIN_SEQUENCE[i]});
		}
		double Tef[] = DataSet.toDoubleValues(DataSet.extractColumnFromTable(dataset, " ", 1));
		double Sp[] = DataSet.toDoubleValues(DataSet.extractColumnFromTable(dataset, " ", 5));
		Interpolation interp = new Interpolation(Tef, Sp, false);
		double s = interp.splineInterpolation(Math.log10(T));
		String SpT = "OBAFGKM";
		int index = (int) (s / 10.0);
		String out = SpT.substring(index, index + 1);
		float subT = (float) (s - index * 10);
		if (ndec <= 0) subT = (int) (subT + 0.5);
		if (subT >= 9.5) {
			if (out.equals("M")) {
				subT = 9;
			} else {
				subT = 0;
				index ++;
				out = SpT.substring(index, index + 1);
			}
		}
		if (ndec > 0) {
			return out + Functions.formatValue(subT, ndec);
		} else {
			return out + (int) subT;
		}
	}

	/**
	 * Returns the spectral type for a star in the main sequence given its effective temperature.
	 * @param sp Spectral type, one of OBAFGKM plus a number.
	 * @return Effective temperature in K.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double getEffectiveTemperature(String sp) throws JPARSECException {
		String SpT = "OBAFGKM";
		int index = SpT.indexOf(sp.substring(0, 1));
		double s = Double.parseDouble(sp.substring(1)) + index * 10;

		String dataset[] = new String[0];
		for (int i=0; i<MAIN_SEQUENCE.length; i++) {
			int n = FileIO.getNumberOfFields(MAIN_SEQUENCE[i], " ", true);
			if (n > 4) dataset = DataSet.addStringArray(dataset, new String[] {MAIN_SEQUENCE[i]});
		}
		double Tef[] = DataSet.toDoubleValues(DataSet.extractColumnFromTable(dataset, " ", 1));
		double Sp[] = DataSet.toDoubleValues(DataSet.extractColumnFromTable(dataset, " ", 5));
		Interpolation interp = new Interpolation(Sp, Tef, false);
		return Math.pow(10.0, interp.splineInterpolation(s));
	}

	/**
	 * Applies the period-luminosity relation for Cepheids:
	 * L (Lsun) = 1.15 log10 P + 2.47.
	 * @param period The period in days.
	 * @return Luminosity in solaru nits.
	 */
	public double CefeidPeriodLuminosity(double period) {
		double L = 1.15 * Math.log10(period) + 2.47;
		return L;
	}

	/**
	 * Applies the Virial Theorem that relates the velocity dispersion
	 * in a set of sources (cluster of galaxies, globular cluster, ...)
	 * within a given radius with the total mass.
	 * @param sigma Velocity dispersion in km/s.
	 * @param r Radius in pc.
	 * @return Mass in solar masses.
	 */
	public static double virialTheorem(double sigma, double r) {
		double radius = r * Constant.PARSEC;
		double M = 3 * 0.5 * sigma * sigma * 1.0E6 * radius / Constant.GRAVITATIONAL_CONSTANT;
		return M / Constant.SUN_MASS;
	}

	/**
	 * Returns the Carrington rotation number for the Sun at the specified Julian day.<BR>
	 *
	 * Richard C. Carrington determined the solar rotation rate by watching low-latitude sunspots in the
	 * 1850s. He defined a fixed solar coordinate system that rotates in a sidereal frame exactly once
	 * every 25.38 days (Carrington, Observations of the Spots on the Sun, 1863, p 221, 244). The synodic
	 * rotation rate varies a little during the year because of the eccentricity of the Earth's orbit;
	 * the mean synodic value is about 27.2753 days.<BR>
	 *
	 * Carrington Rotation 1 began at a seemingly arbitrary instant late on Nov 9, 1853, when Carrington
	 * began his Greenwich photo-heliographic series. Rotations are counted from that time with the
	 * central meridian longitude decreasing from 360 to 0 during each rotation as the central meridian
	 * point rotates under the Earth. Actually the canonical zero meridian used today is the one that
	 * passed through the ascending node of the solar equator on the ecliptic at Greenwich mean noon on
	 * January 1, 1854 (Julian Day 2398220.0).<BR>
	 *
	 * @param jd Julian day in TT.
	 * @return Carrington rotation number.
	 */
	public static int getCarringtonRotationNumber(double jd) {
		double rotation = ((jd - 2451545.11) / 27.2753) + 1958;

		double frac = rotation - Math.floor(rotation);
		if (frac < 0) frac ++;
		double timeMarginDays = 1;
		double fracMargin =  timeMarginDays / 27.2753;
		if (frac < fracMargin || frac > 1.0-fracMargin) {
			try {
				TimeElement time = new TimeElement(jd, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
				CityElement city = City.findCity("Madrid");
				ObserverElement observer = ObserverElement.parseCity(city);
				EphemerisElement eph = new EphemerisElement(TARGET.SUN, EphemerisElement.COORDINATES_TYPE.APPARENT,
						EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.GEOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
						EphemerisElement.FRAME.DYNAMICAL_EQUINOX_J2000, EphemerisElement.ALGORITHM.MOSHIER);

				EphemElement ephem = Ephem.getEphemeris(time, observer, eph, false);
				double lon = Functions.normalizeRadians(ephem.longitudeOfCentralMeridian) * Constant.RAD_TO_DEG;
				if (lon > 0 && lon < (timeMarginDays*360.0/27.2753))
					rotation ++;
			} catch (Exception exc) {}
		}

		return (int) Math.floor(rotation);
	}

	/**
	 * Returns the Julian day when the solar longitude was 0.0 degres prior to the specified
	 * Julian day. Accuracy is better than 1s. Calculation is geocentric.
	 * @param jd The Julian day in TDB.
	 * @return The latest Julian day when heliographic longitude was 0 as visible from Earth's
	 * center, in TDB.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double getCarringtonRotationNumberLastStart(double jd) throws JPARSECException {
		TimeElement time = new TimeElement(jd, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		CityElement city = City.findCity("Madrid");
		ObserverElement observer = ObserverElement.parseCity(city);
		EphemerisElement eph = new EphemerisElement(TARGET.SUN, EphemerisElement.COORDINATES_TYPE.APPARENT,
				EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.GEOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
				EphemerisElement.FRAME.DYNAMICAL_EQUINOX_J2000, EphemerisElement.ALGORITHM.MOSHIER);
		eph.optimizeForSpeed();
		int iter = 0;
		double maxErr = 1E-6;
		while(true) {
			EphemElement ephem = Ephem.getEphemeris(time, observer, eph, false);
			double lon = Functions.normalizeRadians(ephem.longitudeOfCentralMeridian) * Constant.RAD_TO_DEG;
			double off = 0;
			if (iter > 0 && lon < 180) {
				off = lon * 25.38 / 360.0;
			} else {
				off = (360.0 - lon) * 25.38 / 360.0;
			}
			time.add(-off);
			iter ++;
			if (Math.abs(off) < maxErr) break;
		}

		return time.astroDate.jd();
	}

	/**
	 * Returns the number in Ernest W. Brown's numbered series of lunar
	 * cycles for the specified JD. The base Julian day for Ernest W.
	 * Brown's numbered series of lunations is 1923 Jan 17 02:41 UT.
	 * This date has been widely quoted as "Jan 16 1923" and indeed it
	 * was (in EST) at Yale University where Prof. Brown worked.<BR>
	 *
	 * A lunation starts with a new Moon.<BR>
	 *
	 * The output of this method is the correct lunation number for the
	 * specified Julian day in TT, in case a new lunation number started
	 * just before the input date. Note Meeus also introduced another
	 * lunation number starting from the first new moon in year 2000
	 * (Jan 6, 18:14 UTC). Meeus lunation = Brown lunation - 953.
	 *
	 * @param jd Julian day in TDB.
	 * @return Lunation number.
	 */
	public static int getBrownLunationNumber(double jd) {
		double lun = 1 + (jd - 2423436.40347) / 29.530588861;

		double frac = lun - Math.floor(lun);
		if (frac < 0) frac ++;
		double timeMarginDays = 1;
		double fracMargin =  timeMarginDays / 29.530588861;
		if (frac < fracMargin || frac > 1.0-fracMargin) {
			try {
				double TT = MainEvents.MoonPhaseOrEclipse(jd, EVENT.MOON_NEW, EVENT_TIME.CLOSEST).time;
				double elapsed = jd - TT;
				if (elapsed > 0 && elapsed < timeMarginDays) lun ++;
			} catch (Exception exc) {}
		}

		return (int) Math.floor(lun);
	}
	/**
	 * Following data is for main sequence stars, compiled with data coming from
	 * different sources: Flower P.J. Astrophys. J. 469, 355 (1996), Pickles (1998),
	 * Kenneth R. Lang.  Astrophysical Data.  New York: Springer-Verlag, 1991.
	 */
	private static final String MAIN_SEQUENCE[] = new String[] {
		//B-V   logTeff  BC   Teff   Mabs   Sp
		"-0.35 4.7538 -4.720 56728   -6.2   0",
		"-0.34 4.7031 -4.506 50477   -6.0   3",
		"-0.33 4.6551 -4.197 45196   -5.8   5",
		"-0.32 4.6098 -3.861 40719   -5.4   6",
		"-0.31 4.5670 -3.534 36897   -5.0   8",
		"-0.30 4.5266 -3.234 33620   -4.5   10",
		"-0.29 4.4884 -2.966 30789   -4.0   10",
		"-0.28 4.4523 -2.730 28333",
		"-0.27 4.4183 -2.523 26199",
		"-0.26 4.3863 -2.341 24338   -3.2   11",
		"-0.25 4.3561 -2.177 22703",
		"-0.24 4.3276 -2.028 21261   -2.45  12",
		"-0.23 4.3008 -1.891 19989",
		"-0.22 4.2755 -1.762 18858   -1.7   13",
		"-0.21 4.2518 -1.641 17852",
		"-0.20 4.2294 -1.525 16958   -1.5   13",
		"-0.19 4.2083 -1.414 16154",
		"-0.18 4.1885 -1.307 15434",
		"-0.17 4.1699 -1.205 14787   -1.2   15",
		"-0.16 4.1524 -1.107 14203",
		"-0.15 4.1360 -1.013 13677   -0.9   16",
		"-0.14 4.1205 -0.923 13197",
		"-0.13 4.1060 -0.839 12764   -0.6   17",
		"-0.12 4.0923 -0.759 12368",
		"-0.11 4.0795 -0.684 12008   -0.25  18",
		"-0.10 4.0674 -0.614 11678",
		"-0.09 4.0560 -0.549 11376",
		"-0.08 4.0453 -0.488 11099",
		"-0.07 4.0353 -0.432 10846",
		"-0.06 4.0258 -0.381 10612    0.2   19",
		"-0.05 4.0169 -0.334 10396",
		"-0.04 4.0084 -0.290 10195",
		"-0.03 4.0005 -0.252 10011",
		"-0.02 3.9930 -0.216  9840    0.5   20",
		"-0.01 3.9859 -0.184  9680",
		"0.00 3.9791 -0.155  9530    0.6   20",
		"0.01 3.9728 -0.129  9392",
		"0.02 3.9667 -0.106  9261    1.0   21",
		"0.03 3.9609 -0.084  9139",
		"0.04 3.9555 -0.067  9026",
		"0.05 3.9502 -0.050  8916    1.3   22",
		"0.06 3.9452 -0.036  8814",
		"0.07 3.9404 -0.024  8717",
		"0.08 3.9358 -0.013  8625    1.5   23",
		"0.09 3.9314 -0.004  8538",
		"0.10 3.9271  0.004  8454    1.7   25",
		"0.11 3.9229  0.010  8373",
		"0.12 3.9189  0.015  8296",
		"0.13 3.9150  0.019  8222",
		"0.14 3.9113  0.022  8152",
		"0.15 3.9076  0.024  8083    2.0   26",
		"0.16 3.9040  0.026  8016",
		"0.17 3.9004  0.028  7950",
		"0.18 3.8969  0.029  7886",
		"0.19 3.8935  0.031  7825",
		"0.20 3.8902  0.032  7766    2.2   27",
		"0.21 3.8869  0.033  7707",
		"0.22 3.8836  0.033  7648",
		"0.23 3.8804  0.034  7592",
		"0.24 3.8771  0.034  7535",
		"0.25 3.8740  0.035  7481    2.5   28",
		"0.26 3.8708  0.035  7426",
		"0.27 3.8676  0.035  7372",
		"0.28 3.8645  0.035  7319",
		"0.29 3.8614  0.035  7267",
		"0.30 3.8583  0.034  7216    2.75  30",
		"0.31 3.8552  0.034  7164",
		"0.32 3.8521  0.033  7113",
		"0.33 3.8490  0.032  7063",
		"0.34 3.8460  0.031  7014",
		"0.35 3.8429  0.030  6964    3.2   32",
		"0.36 3.8399  0.028  6916",
		"0.37 3.8368  0.026  6867",
		"0.38 3.8338  0.025  6820",
		"0.39 3.8307  0.022  6771",
		"0.40 3.8277  0.020  6725",
		"0.41 3.8247  0.018  6678",
		"0.42 3.8216  0.015  6632",
		"0.43 3.8187  0.012  6587",
		"0.44 3.8156  0.009  6541    3.5   35",
		"0.45 3.8127  0.006  6496",
		"0.46 3.8098  0.003  6453    3.5   35",
		"0.47 3.8068 -0.001  6409",
		"0.48 3.8039 -0.004  6366",
		"0.49 3.8010 -0.008  6324",
		"0.50 3.7981 -0.012  6282",
		"0.51 3.7952 -0.016  6240",
		"0.52 3.7923 -0.021  6198    4.0   38",
		"0.53 3.7894 -0.025  6158",
		"0.54 3.7865 -0.030  6117",
		"0.55 3.7838 -0.035  6078",
		"0.56 3.7810 -0.039  6040",
		"0.57 3.7783 -0.045  6002",
		"0.58 3.7755 -0.050  5964    4.4   40",
		"0.59 3.7728 -0.055  5927",
		"0.60 3.7702 -0.061  5891",
		"0.61 3.7675 -0.067  5855",
		"0.62 3.7649 -0.073  5819",
		"0.63 3.7622 -0.079  5784    5.0   42",
		"0.64 3.7597 -0.085  5751",
		"0.65 3.7572 -0.091  5717",
		"0.66 3.7547 -0.098  5684",
		"0.67 3.7523 -0.104  5653",
		"0.68 3.7497 -0.111  5620    5.1   45",
		"0.69 3.7473 -0.117  5589",
		"0.70 3.7450 -0.124  5559",
		"0.71 3.7426 -0.132  5528",
		"0.72 3.7403 -0.139  5499",
		"0.73 3.7380 -0.146  5470",
		"0.74 3.7358 -0.153  5442    5.6   48",
		"0.75 3.7334 -0.161  5413",
		"0.76 3.7313 -0.168  5386",
		"0.77 3.7291 -0.176  5359",
		"0.78 3.7270 -0.184  5333",
		"0.79 3.7249 -0.192  5307",
		"0.80 3.7228 -0.200  5282",
		"0.81 3.7207 -0.208  5256    5.9   50",
		"0.82 3.7186 -0.216  5231",
		"0.83 3.7166 -0.225  5207",
		"0.84 3.7146 -0.233  5183",
		"0.85 3.7126 -0.242  5159",
		"0.86 3.7106 -0.250  5136    6.0   50",
		"0.87 3.7088 -0.259  5114",
		"0.88 3.7067 -0.268  5090",
		"0.89 3.7048 -0.277  5068",
		"0.90 3.7030 -0.285  5047",
		"0.91 3.7011 -0.295  5025    6.1   51",
		"0.92 3.6993 -0.304  5004",
		"0.93 3.6976 -0.313  4984",
		"0.94 3.6957 -0.322  4963",
		"0.95 3.6940 -0.332  4943",
		"0.96 3.6921 -0.342  4922    6.4   52",
		"0.97 3.6904 -0.352  4902",
		"0.98 3.6887 -0.361  4883",
		"0.99 3.6868 -0.372  4862",
		"1.00 3.6851 -0.382  4843",
		"1.01 3.6835 -0.392  4825",
		"1.02 3.6818 -0.403  4806",
		"1.03 3.6800 -0.414  4786",
		"1.04 3.6783 -0.426  4767",
		"1.05 3.6765 -0.437  4748",
		"1.06 3.6750 -0.448  4731    6.65  53",
		"1.07 3.6733 -0.459  4713",
		"1.08 3.6715 -0.471  4694",
		"1.09 3.6699 -0.482  4676",
		"1.10 3.6682 -0.494  4658",
		"1.11 3.6665 -0.505  4640",
		"1.12 3.6648 -0.517  4622",
		"1.13 3.6632 -0.528  4605",
		"1.14 3.6615 -0.540  4587    7.1   54",
		"1.15 3.6598 -0.552  4569",
		"1.16 3.6583 -0.564  4553",
		"1.17 3.6566 -0.576  4535",
		"1.18 3.6550 -0.588  4518",
		"1.19 3.6532 -0.601  4500",
		"1.20 3.6516 -0.614  4483",
		"1.21 3.6499 -0.626  4466",
		"1.22 3.6483 -0.640  4449",
		"1.23 3.6467 -0.652  4433",
		"1.24 3.6449 -0.666  4415",
		"1.25 3.6434 -0.679  4399",
		"1.26 3.6417 -0.694  4382",
		"1.27 3.6401 -0.707  4366",
		"1.28 3.6384 -0.722  4349    7.5   55",
		"1.29 3.6368 -0.736  4333",
		"1.30 3.6351 -0.752  4316",
		"1.31 3.6335 -0.766  4300",
		"1.32 3.6318 -0.782  4283",
		"1.33 3.6300 -0.798  4266",
		"1.34 3.6285 -0.814  4251",
		"1.35 3.6268 -0.831  4234",
		"1.36 3.6250 -0.848  4217",
		"1.37 3.6235 -0.865  4202",
		"1.38 3.6218 -0.883  4186",
		"1.39 3.6200 -0.901  4169",
		"1.40 3.6184 -0.920  4153",
		"1.41 3.6167 -0.939  4137",
		"1.42 3.6149 -0.960  4120",
		"1.43 3.6131 -0.980  4103",
		"1.44 3.6113 -1.002  4086",
		"1.45 3.6096 -1.024  4070    8.3   57",
		"1.46 3.6078 -1.047  4053",
		"1.47 3.6060 -1.071  4036",
		"1.48 3.6040 -1.096  4018",
		"1.49 3.6022 -1.122  4001",
		"1.50 3.6001 -1.150  3982",
		"1.51 3.5981 -1.178  3964",
		"1.52 3.5961 -1.209  3945",
		"1.53 3.5940 -1.241  3926",
		"1.54 3.5917 -1.276  3906",
		"1.55 3.5894 -1.312  3885",
		"1.56 3.5872 -1.350  3865    8.9   60",
		"1.57 3.5847 -1.393  3843",
		"1.58 3.5822 -1.437  3821",
		"1.59 3.5794 -1.486  3797",
		"1.60 3.5767 -1.539  3773",
		"1.61 3.5738 -1.595  3748",
		"1.62 3.5707 -1.658  3721    9.5   61",
		"1.63 3.5674 -1.728  3693",
		"1.64 3.5640 -1.802  3664",
		"1.65 3.5604 -1.885  3634",
		"1.66 3.5564 -1.978  3601",
		"1.67 3.5524 -2.078  3568   10.2   62",
		"1.68 3.5481 -2.191  3533",
		"1.69 3.5436 -2.318  3496",
		"1.70 3.5387 -2.460  3457   10.8   63",
		"1.71 3.5334 -2.620  3415",
		"1.72 3.5279 -2.803  3372   11.85  64",
		"1.73 3.5219 -3.007  3326",
		"1.74 3.5156 -3.239  3278",
		"1.75 3.5089 -3.502  3228   13.15  65",
		"1.76 3.5017 -3.805  3175",
		"1.77 3.4940 -4.152  3119",
		"1.78 3.4859 -4.544  3061   15.0   66",
		"1.79 3.4771 -5.004  3000",
		"1.80 3.4678 -5.535  2936   16.5   67"
	};

	// Data from Allen's Astrophysical Quantities, by Arthur N. Cox.
	// See http://xoomer.virgilio.it/hrtrace/Sk.htm
	private static final String[] CLASS_III = new String[] {
		// SpT Mv B-V   U-B   V-R   R-I  Teff  BC
		"G5  0.9  0.86  0.56  0.69  0.48 5050 -0.34",
		"G8  0.8  0.94  0.70  0.70  0.48 4800 -0.42",
		"K0  0.7  1.00  0.84  0.77  0.53 4660 -0.50",
		"K2  0.5  1.16  1.16  0.84  0.58 4390 -0.61",
		"K5 -0.2  1.50  1.81  1.20  0.90 4050 -1.02",
		"M0 -0.4  1.56  1.87  1.23  0.94 3690 -1.25",
		"M2 -0.6  1.60  1.89  1.34  1.10 3540 -1.62",
		"M5 -0.3  1.63  1.58  2.18  1.96 3380 -2.48"
	};

	private static final String[] CLASS_I = new String[] {
		"O9 -6.5 -0.27 -1.13 -0.15 -0.32 32000 -3.18",
		"B2 -6.4 -0.17 -0.93 -0.05 -0.15 17600 -1.58",
		"B5 -6.2 -0.10 -0.72  0.02 -0.07 13600 -0.95",
		"B8 -6.2 -0.03 -0.55  0.02 0.00 11100 -0.66",
		"A0 -6.3 -0.01 -0.38  0.03  0.05 9980 -0.41",
		"A2 -6.5  0.03 -0.25  0.07  0.07 9380 -0.28",
		"A5 -6.6  0.09 -0.08  0.12  0.13 8610 -0.13",
		"F0 -6.6 -0.17  0.15  0.21  0.20 7460 -0.01",
		"F2 -6.6  0.23  0.18  0.26  0.21 7030 0.00",
		"F5 -6.6  0.32  0.27  0.35  0.23 6370 -0.03",
		"F8 -6.5  0.56  0.41  0.45  0.27 5750 -0.09",
		"G0 -6.4  0.76  0.52  0.51  0.33 5370 -0.15",
		"G2 -6.3  0.87  0.63  0.58  0.40 5190 -0.21",
		"G5 -6.2  1.02  0.83  0.67  0.44 4930 -0.33",
		"G8 -6-1  1.14  1.07  0.69  0.46 4700 -0.42",
		"K0 -6.0  1.25  1.17  0.76  0.48 4550 -0.50",
		"K2 -5.9  1.36  1.32  0.85  0.55 4310 -0.61",
		"K5 -5.8  1.60  1.80  1.20  0.90 3990 -1.01",
		"M0 -5.6  1.67  1.90  1.23  0.94 3620 -1.29",
		"M2 -5.6  1.71  1.95  1.34  1.10 3370 -1.62",
		"M5 -5.6  1.80  1.60  2.18  1.96 2880 -3.47"
	};
}
