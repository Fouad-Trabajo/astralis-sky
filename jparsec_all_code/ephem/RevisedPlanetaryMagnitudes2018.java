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
package jparsec.ephem;

import jparsec.ephem.EphemerisElement;
import jparsec.ephem.Functions;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.planets.EphemElement;
import jparsec.math.Constant;
import jparsec.observer.ObserverElement;
import jparsec.time.AstroDate;
import jparsec.time.TimeElement;
import jparsec.util.JPARSECException;

/**
 * Implementation and test for the Mallama and Hilton revision for the computation 
 * of more realistic planetary magnitudes.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class RevisedPlanetaryMagnitudes2018 {
	
	/**
	 * Compute the apparent magnitude of a planet using the equations by 
	 * Mallama, A., Hilton, J., 'Computing Apparent Planetary Magnitude for the 
	 * Astronomical Almanac', Astronomy and Computing (2018).
	 * @param time Time object for the computation. Used only for Neptune since between 
	 * 1980 and 2000 its absolute magnitude rose.
	 * @param obs The observer object.
	 * @param eph Ephemeris object with the target body
	 * @param ephem The computed ephemerides for the target body and date.
	 * @param rings Include the brightness of the Saturn rings.
	 * @return The revised magnitude, could be the same present in the ephemeris 
	 * object for unsupported bodies or calculation conditions.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double getRevisedPlanetMagnitude(TimeElement time, ObserverElement obs, 
			EphemerisElement eph, EphemElement ephem, boolean rings) throws JPARSECException {
		TARGET target = eph.targetBody;
		if (!target.isPlanet() || target == TARGET.SUN) return ephem.magnitude;
		
		// Phase angle limit for using equations
		double geocentric_phase_angle_limit;
		// Upper limit of the observed range of phase angles
		double phase_angle_upper_limit = 180;
		// Lower limit of the observed range of phase angles
		double phase_angle_lower_limit = 0;
				
		double r = ephem.distanceFromSun;
		double delta = ephem.distance;
		double ph_ang = Functions.normalizeRadians(Math.abs(ephem.phaseAngle)) * Constant.RAD_TO_DEG;
		double pre_ap_mag = ephem.magnitude;
		double ph_ang_factor, ap_mag = pre_ap_mag;

		// Compute the 'r' distance factor
		double r_mag_factor = 2.5 * Math.log10(r * r);
		
		// Compute the 'delta' distance factor
		double delta_mag_factor = 2.5 * Math.log10(delta * delta);
		
		// Compute the distance factor
		double distance_mag_factor = r_mag_factor + delta_mag_factor;
		
		// Sub-Sun and sub-Earth geographic latitudes (degrees)
		double sun_sub_lat_geod = 0, earth_sub_lat_geod = 0;
		if (target == TARGET.SATURN || target == TARGET.URANUS) {
			sun_sub_lat_geod = ephem.subsolarLatitude * Constant.RAD_TO_DEG;
			earth_sub_lat_geod = ephem.positionAngleOfPole * Constant.RAD_TO_DEG;
		}
		
		switch (target) {
		case MERCURY:
			phase_angle_lower_limit = 2.0;
			phase_angle_upper_limit = 170.0;

			// Compute the phase angle factor
			// ph_ang_factor =  + 6.617E-02 * ph_ang - 1.867E-03 * Math.pow(ph_ang, 2)  
			// + 4.103E-05 * Math.pow(ph_ang, 3) - 4.583E-07 * Math.pow(ph_ang, 4) 
			// + 2.643E-09 * Math.pow(ph_ang, 5) - 7.012E-12 * Math.pow(ph_ang, 6) + 6.592E-15 * Math.pow(ph_ang, 7);

			// 6th order
			ph_ang_factor = 6.3280e-02 * ph_ang    - 1.6336e-03 * Math.pow(ph_ang, 2) + 3.3644e-05 * Math.pow(ph_ang, 3) 
				- 3.4265e-07 * Math.pow(ph_ang, 4) + 1.6893e-09 * Math.pow(ph_ang, 5) - 3.0334e-12 * Math.pow(ph_ang, 6);

			// Add factors to determine the apparent magnitude
			ap_mag = -0.613 + distance_mag_factor + ph_ang_factor;
			break;
		case VENUS:
			phase_angle_lower_limit = 2.0;
			phase_angle_upper_limit = 179.0;

			// Compute the phase angle factor
			if (ph_ang < 163.7) {
				// phase angle less 163.7 degrees (equation #3)
				ph_ang_factor = -1.044E-03 * ph_ang + 3.687E-04 * ph_ang * ph_ang - 2.814E-06 *  
						Math.pow(ph_ang, 3) + 8.938E-09 * Math.pow(ph_ang, 4);
			} else {
				// phase angle greater than or equal to 163.7 (equation #4)
				ph_ang_factor = 236.05828 + 4.384 - 2.81914E+00 * ph_ang + 8.39034E-03 * ph_ang * ph_ang;
			}
			
			// Add factors to determine the apparent magnitude
			ap_mag = -4.384 + distance_mag_factor + ph_ang_factor;
			break;
		case EARTH:
			phase_angle_upper_limit = 170.0;
			 
			// Compute the phase angle factor
			ph_ang_factor = -1.060e-03 * ph_ang + 2.054e-04 * ph_ang * ph_ang;

			// Add factors to determine the apparent magnitude
			ap_mag = -3.99 + distance_mag_factor + ph_ang_factor;
			break;
		case MARS:
			geocentric_phase_angle_limit = 50.0;
			phase_angle_upper_limit = 120.0;
			double Ls_offset = -85.0;	// Add to convert from heliocentric ecliptic longitude to vernal equinox
										// longitude (Ls)
			// Compute the phase angle factor 
			if (ph_ang <= geocentric_phase_angle_limit) {
				// Use equation #6 for phase angles below the geocentric limit
			    ph_ang_factor = 2.267E-02 * ph_ang - 1.302E-04 * ph_ang * ph_ang;
			} else {
				// Use equation #7 for phase angles above the geocentric limit
			    ph_ang_factor = - 0.02573 * ph_ang + 0.0003445 * ph_ang * ph_ang;
			}

			// Sub-Sun and sub-Earth longitude (degrees)
			double sub_sun_long = ephem.subsolarLongitude * Constant.RAD_TO_DEG;
			double sub_earth_long = ephem.longitudeOfCentralMeridian * Constant.RAD_TO_DEG;

			// Compute the effective central meridian longitude
			double eff_CM = Functions.normalizeDegrees((sub_earth_long + sub_sun_long) / 2.);

			// Use Stirling interpolation to determine the magnitude correction
			double mag_corr_rot = MarsStirlingRotation(eff_CM);
			  
			// Convert the ecliptic longitude to Ls
			double h_ecl_long = ephem.heliocentricEclipticLongitude * Constant.RAD_TO_DEG;
			double Ls = h_ecl_long + Ls_offset;
			if (Ls > 360.) Ls = Ls - 360.;
			if (Ls <   0.) Ls = Ls + 360.;
			  
			// Use Stirling interpolation to determine the magnitude correction
			double mag_corr_orb = MarsStirlingOrbital(Ls);
			  
			// Add factors to determine the apparent magnitude
			if (ph_ang <= geocentric_phase_angle_limit) {
				// Use equation #6 for phase angles below the geocentric limit
			    ap_mag = -1.601 + distance_mag_factor + ph_ang_factor + mag_corr_rot + mag_corr_orb;
			} else {
				// Use equation #7 for phase angles above the geocentric limit
			    ap_mag = -0.367 + distance_mag_factor + ph_ang_factor + mag_corr_rot + mag_corr_orb;
			}
			break;
		case JUPITER:
			geocentric_phase_angle_limit = 12.0;
			phase_angle_upper_limit = 130.0;

			// Compute the phase angle factor
			if (ph_ang <= geocentric_phase_angle_limit) {
				// Use equation #8 for phase angles below the geocentric limit
				ph_ang_factor = -3.7E-04 * ph_ang + 6.16E-04 * ph_ang * ph_ang;
			} else {
				// Use equation #9 for phase angles above the geocentric limit
				double ph_ang_f = ph_ang / 180.0;
				ph_ang_factor = -2.5 * Math.log10(1.0 - 1.507 * ph_ang_f - 0.363 * 
					Math.pow(ph_ang_f, 2.0) - 0.062 * Math.pow(ph_ang_f, 3.0) +
					2.809 * Math.pow(ph_ang_f, 4.0) - 1.876 * Math.pow(ph_ang_f, 5.0));
			}

			// Add factors to determine the apparent magnitude
			if (ph_ang <= geocentric_phase_angle_limit) {
				// Use equation #6 for phase angle <= 50 degrees
			    ap_mag = -9.395 + distance_mag_factor + ph_ang_factor;
			} else {
				// Use equation #7 for phase angle > 50 degrees
			    ap_mag = -9.428 + distance_mag_factor + ph_ang_factor;
			}
			break;
		case SATURN:
			double e2 = 0.8137; // eccentricity squared of Saturn ellipse
			geocentric_phase_angle_limit = 6.5;
			double geocentric_inclination_limit = 27.0;
			phase_angle_upper_limit = 150.0;

			// Compute the effective sub-latitude
			// First convert geodetic to geocentric
			double sun_sub_lat_geoc = SaturnGeodeticToGeocentric(sun_sub_lat_geod, e2);
			double earth_sub_lat_geoc = SaturnGeodeticToGeocentric(earth_sub_lat_geod, e2);
			// Then take the square root of the product of the saturnicentric latitude of
			// the Sun and that of Earth but set to zero when the signs are opposite
			double sub_lat_geoc_product = sun_sub_lat_geoc * earth_sub_lat_geoc;
			// Finally compute the effective value
			double sub_lat_geoc = (sub_lat_geoc_product > 0.) ? Math.sqrt(sub_lat_geoc_product) : 0;
			  
			// Compute the effect of phase angle and inclination
			if (rings && ph_ang <= geocentric_phase_angle_limit && 
				sub_lat_geoc <= geocentric_inclination_limit) {
				sub_lat_geoc *= Constant.DEG_TO_RAD;
			    // Use equation #10 for globe+rings and geocentric circumstances
			    ap_mag = -8.914 - 1.825 * Math.sin (sub_lat_geoc) + 
			    	0.026 * ph_ang - 0.378 * Math.sin (sub_lat_geoc) * Math.exp (-2.25 * ph_ang);
			} else if (!rings && ph_ang <= geocentric_phase_angle_limit &&
			     sub_lat_geoc <= geocentric_inclination_limit) {
			    // Use equation #11 for globe-alone and geocentric circumstances
			    ap_mag = -8.95 - 3.7e-4 * ph_ang + 6.16e-4 * Math.pow(ph_ang, 2);
			} else if (!rings && ph_ang > geocentric_phase_angle_limit) {
			    // Use equation #12 for globe-alone beyond geocentric phase angle limit
			    ap_mag = -8.94 + 2.446e-4 * ph_ang + 2.672e-4 * Math.pow(ph_ang, 2) - 1.506e-6 * 
			    		Math.pow(ph_ang, 3) + 4.767e-9 * Math.pow(ph_ang, 4);
			} else {
				// Unable to compute a magnitude for these conditions
				return ap_mag;
			}
			  
			// Add the distance factors to determine the apparent magnitude
			ap_mag = ap_mag + distance_mag_factor;
			break;
		case URANUS:
			geocentric_phase_angle_limit = 3.1;
			phase_angle_upper_limit = 154.0;

			// Compute the effective sub-latitude
			// Take the average of the absolute values of the planetographic latitude of
			// the Sun and that of Earth
			double sub_lat_planetog = (Math.abs(sun_sub_lat_geod) + Math.abs(earth_sub_lat_geod)) / 2.;
			
			// Compute the sub-latitude factor
			double sub_lat_factor = -0.00084 * sub_lat_planetog;
			
			// Compute the magnitude depending on the phase angle
			if (ph_ang <= geocentric_phase_angle_limit) {
				// Use equation #13 for phase angles below the geocentric limit
				ap_mag = -7.110 + distance_mag_factor + sub_lat_factor;
			} else {
				// Use equation #14 for phase angles above the geocentric limit
				ap_mag = -7.110 + distance_mag_factor + sub_lat_factor + 6.587e-3 * ph_ang + 
						1.045e-4 * ph_ang * ph_ang;
			}
			break;
		case NEPTUNE:
			geocentric_phase_angle_limit = 1.9;
			phase_angle_upper_limit = 133.14;
			AstroDate date = time.astroDate;
			double year = date.getYear() + (date.getMonth() - 1.0 + date.getDay() / 30.0) / 12.0;

			// Equation 16 - compute the magnitude at unit distance as a function of time
			if (year > 2000.0) ap_mag = -7.00;
			if (year < 1980.0) ap_mag = -6.89;
			if (year >= 1980 && year <= 2000.) ap_mag = -6.89 - 0.0054 * (year - 1980.0);

			// Add factors to determine the apparent magnitude
			ap_mag = ap_mag + distance_mag_factor;
			  
			// Add phase angle factor from equation 17 if needed
			if (ph_ang > geocentric_phase_angle_limit) {
				// Check the year because equation 17 only pertains to t > 2000.0
			    if (year > 2000.) {
			    	ap_mag = ap_mag + 7.944e-3 * ph_ang + 9.617e-5 * ph_ang * ph_ang;
			    } else {
			    	// Unable to compute a magnitude for these conditions
					return pre_ap_mag;
				}
			}
			break;
		default:
			return pre_ap_mag;
		}
		
		// Warning if the phase angle is outside the limits of observation
		if (ph_ang < phase_angle_lower_limit || ph_ang > phase_angle_upper_limit) 
			JPARSECException.addWarning(target.getEnglishName() + " phase angle exceeds limits of observed magnitudes. " + 
					"Revised magnitude is not reliable");
		
		return ap_mag;
	}

	/* Compute the magnitude correction for rotation angle or orbital position based
	 on data from Mallama, 2016, Icarus, 192, 404-416
	 Stirling interpolation algorithm from Duncombe in Computational Techniques in
	 Explanatory Supplement to the Astronomical Almanac
	 This code is based on Hilton's python code
	 Example is from 'https://mat.iitm.ac.in/home/sryedida/public_html/caimna/interpolation/cdf.html'
	*/
	private static double MarsStirlingOrbital(double angle) {
		double L2[] = new double[] {
			-0.030, -0.017, -0.029, -0.017, -0.014, -0.006, -0.018, -0.020, 
			-0.014, -0.030, -0.008, -0.040, -0.024, -0.037, -0.036, -0.032, 
			0.010,  0.010, -0.001,  0.044,  0.025, -0.004, -0.016, -0.008,
			0.029, -0.054, -0.033,  0.055,  0.017,  0.052,  0.006,  0.087, 
			0.006,  0.064,  0.030,  0.019, -0.030, -0.017, -0.029, -0.017 };
        
		// Determine the starting point for the differences and p, an array of the 
		// proportional distance of the longitude between the two tabulated values
		// raised to the nth power.
		int array_offset = 0;
		int zero_point = (int) (angle / 10.0) + array_offset;
		double p[] = new double[] {1.0, angle / 10.0 - zero_point, 0, 0, 0};
		for (int i = 2; i <= 4; i++) {
			p[i] = Math.pow(p[1], i);
		}

		// Calculate arrays of the first through fourth differences
		double delta[] = new double[4];
		double delta_2[] = new double[3];
		double delta_3[] = new double[2];
		for (int i = 0; i <= 2; i++) { // FIXME: possible bug in original code (2 instead of 3)
			delta[i] = L2[i + 1 + zero_point] - L2[i + zero_point];
		}
		for (int i = 0; i<= 2; i++) {
			delta_2[i] = delta[i + 1] - delta[i];
		}
		for (int i = 0; i <= 1; i++) {
			delta_3[i] = delta_2[i + 1] - delta_2[i];
		}
		double delta_4 = delta_3[1] - delta_3[0];
		
		// Convert differences into polynomial coefficients
		double a0 = L2[2 + zero_point];
		double a4 = delta_4 / 24.0;
		double a3 = (delta_3[0] + delta_3[1]) / 12.0;
		double a2 = delta_2[1] / 2.0 - a4;
		double a1 = (delta[1] + delta[2]) / 2.0 - a3;
		double a[] = new double[] {a0, a1, a2, a3, a4};
		
		// Evaluate the polynomial to compute the magnitude correction
		double mag_corr = 0.;
		for (int i = 0; i <= 4; i++) {
		  mag_corr += a[i] * p[i];
		}
		return mag_corr;
	}

	/* Compute the magnitude correction for rotation angle or orbital position based
	 on data from Mallama, 2016, Icarus, 192, 404-416
	 Stirling interpolation algorithm from Duncombe in Computational Techniques in
	 Explanatory Supplement to the Astronomical Almanac
	 This code is based on Hilton's python code
	 Example is from 'https://mat.iitm.ac.in/home/sryedida/public_html/caimna/interpolation/cdf.html'
	*/
	private static double MarsStirlingRotation(double angle) {
		double L1[] = new double[] {
			0.024,  0.034,  0.036,  0.045,  0.038,  0.023,  0.015,  0.011, 
			0.000, -0.012, -0.018, -0.036, -0.044, -0.059, -0.060, -0.055, 
			-0.043, -0.041, -0.041, -0.036, -0.036, -0.018, -0.038, -0.011, 
			0.002,  0.004,  0.018,  0.019,  0.035,  0.050,  0.035,  0.027, 
			0.037,  0.048,  0.025,  0.022,  0.024,  0.034,  0.036,  0.045 };

		// Determine the starting point for the differences and p, an array of the 
		// proportional distance of the longitude between the two tabulated values
		// raised to the nth power.
		int array_offset = 0;
		int zero_point = (int) (angle / 10.0) + array_offset;
		double p[] = new double[] {1.0, angle / 10.0 - zero_point, 0, 0, 0};
		for (int i = 2; i<= 4; i++) {
			p[i] = Math.pow(p[1], i);
		}

		// Calculate arrays of the first through fourth differences
		double delta[] = new double[4];
		double delta_2[] = new double[3];
		double delta_3[] = new double[2];
		for (int i = 0; i <= 2; i++) { // FIXME: possible bug in original code (2 instead of 3)
			delta[i] = L1[i + 1 + zero_point] - L1[i + zero_point];
		}
		for (int i = 0; i <= 2; i++) {
			delta_2[i] = delta[i + 1] - delta[i];
		}
		for (int i = 0; i <= 1; i++) {
			delta_3[i] = delta_2[i + 1] - delta_2[i];
		}
		double delta_4 = delta_3[1] - delta_3[0];
		
		// Convert differences into polynomial coefficients
		double a0 = L1[2 + zero_point];
		double a4 = delta_4 / 24.0;
		double a3 = (delta_3[0] + delta_3[1]) / 12.0;
		double a2 = delta_2[1] / 2.0 - a4;
		double a1 = (delta[1] + delta[2]) / 2.0 - a3;
		double a[] = new double[] {a0, a1, a2, a3, a4};
		
		// Evaluate the polynomial to compute the magnitude correction
		double mag_corr = 0.;
		for (int i = 0; i <= 4; i ++) {
		  mag_corr += a[i] * p[i];
		}
		return mag_corr;
	}

	// Convert from geodetic to geocentric latitude
	// Equation #18
	private static double SaturnGeodeticToGeocentric(double geodetic, double e2) {
		// Wiki (Saturn article): The axial tilt of Saturn to its orbit is 26.73 degrees.
		// Wiki (Latitude article):
		// f = (a - b) / a
		// For Saturn, a = 60,268, b = 54,364. So, f = 0.0980.
		// e**2 = 2 f - f**2. So,  e2 = 0.186.
		// Then geocentric latitude and geodetic latitude are related by
		// Geocentric = atand ( ( 1 - e**2 ) * tand (geodetic ) )
		//            = atand ( 0.8137 * tan (geodetic) )

		// Tested as follows:
		// HORIZONS: 31.75 is the largest sub-solar planetodetic latitude from 1980-01-01
		// until 1987-12-01 and after that it decreases.
		// The following code gives the planetocentric sub-sun latitude as 26.73 which is
		// exactly equal to the inclination quoted above.

		return Math.atan(e2 * Math.tan(geodetic * Constant.DEG_TO_RAD)) * Constant.RAD_TO_DEG;
	}
}
