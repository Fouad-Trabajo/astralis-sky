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

import java.awt.geom.Point2D;

import jparsec.graph.DataSet;
import jparsec.io.image.WCS;
import jparsec.math.Constant;
import jparsec.math.FastMath;
import jparsec.observer.LocationElement;
import jparsec.util.JPARSECException;

/**
 * A class to process plate photographs/CCD images and perform astrometry.
 * Based on the program appeared in Sky &amp; Telescope, July 1990, by Jordan D. Marche.<BR>
 * It is recommended to use the position (1, 1) for the center of the first pixel
 * when giving the location of the stars in the constructor. This (1, 1) is the first
 * pixel for SExtractor, so the positions given by SExtractor can be used directly with
 * no modification. With only the WCS instance you cannot know which is the first
 * pixel in the image.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Astrometry {

	private double plateConstants[], residuals[], S1, S2, L;
	private LocationElement loc0;

	/**
	 * Constructor for a previously computed plate solution. Information about residuals
	 * will not be available.
	 * @param loc0 The approximate apparent equatorial coordinates of the plate center,
	 * (X/2, Y/2) position, being X and Y the width and height in pixels.
	 * @param cte The plate constants.
	 * @throws JPARSECException If the invalid set of plate constants are invalid.
	 */
	public Astrometry(LocationElement loc0, double cte[]) throws JPARSECException {
		L = 1;
		if (cte.length != 6) throw new JPARSECException("Invalid set of plate constants");
		plateConstants = cte.clone();
		this.loc0 = loc0.clone();
	}

	/**
	 * Constructor for a previously computed plate solution. Information about residuals
	 * are included in the constructor.
	 * @param loc0 The approximate apparent equatorial coordinates of the plate center,
	 * (X/2, Y/2) position, being X and Y the width and height in pixels.
	 * @param cte The plate constants.
	 * @param res The plate residuals in RA and DEC (2 values), in radians.
	 * @throws JPARSECException If the invalid set of plate constants are invalid.
	 */
	public Astrometry(LocationElement loc0, double cte[], double res[]) throws JPARSECException {
		L = 1;
		if (cte.length != 6) throw new JPARSECException("Invalid set of plate constants");
		plateConstants = cte.clone();
		this.loc0 = loc0.clone();
		this.S1 = res[0];
		this.S2 = res[1];
	}

	/**
	 * Constructor for a given plate.
	 * @param loc0 The approximate apparent equatorial coordinates of the plate center,
	 * (X/2, Y/2) position, being X and Y the width and height in pixels.
	 * @param loc The apparent equatorial coordinates of each reference star.
	 * @param p The rectangular positions (x, y) in the plate for each reference star.
	 * The x position goes from 0 to X-1, and y from 0 to Y-1.
	 * @throws JPARSECException If the number of reference stars is below 4.
	 */
	public Astrometry(LocationElement loc0, LocationElement loc[], Point2D p[]) throws JPARSECException {
		int N = loc.length;
		if (N < 4) throw new JPARSECException("The number of reference stars should be 4 at least.");
		this.loc0 = loc0.clone();
		double L = 1;
		this.L = L;

		double A[] = new double[loc.length], D[] = new double[loc.length];
		double X1[] = new double[loc.length], Y1[] = new double[loc.length];
		double X[] = new double[loc.length], Y[] = new double[loc.length];
		double sd = Math.sin(loc0.getLatitude()), cd = Math.cos(loc0.getLatitude());
		for (int i=0; i<loc.length; i++) {
			A[i] = loc[i].getLongitude();
			D[i] = loc[i].getLatitude();

			double sj = Math.sin(D[i]), cj = Math.cos(D[i]);
			double h = sj * sd + cj * cd * Math.cos(A[i] - loc0.getLongitude());
			X1[i] = cj * Math.sin(A[i] - loc0.getLongitude()) / h;
			Y1[i] = (sj * cd - cj * sd * Math.cos(A[i] - loc0.getLongitude())) / h;
			X[i] = p[i].getX();
			Y[i] = p[i].getY();
		}

		double R1 = 0.0, R2 = 0.0, R3 = 0.0, R7 = 0.0, R8 = 0.0, R9 = 0.0, XS = 0.0, YS = 0.0;
		double R[][] = new double[loc.length][9];
		for (int i=0; i < loc.length; i++) {
			XS += X[i];
			YS += Y[i];
			R[i][0] = X[i] * X[i];
			R1 += R[i][0];
			R[i][1] = Y[i] * Y[i];
			R2 += R[i][1];
			R[i][2] = X[i] * Y[i];
			R3 += R[i][2];
			R[i][6] = Y1[i]-Y[i] / L;
			R7 += R[i][6];
			R[i][7] = R[i][6] * X[i];
			R8 += R[i][7];
			R[i][8] = R[i][6] * Y[i];
			R9 += R[i][8];
		}

		// Solve by Cramer's Rule
		double DD = R1 * (R2 * N - YS * YS) - R3 * (R3 * N - XS * YS) + XS * (R3 * YS - XS * R2);
		double D_ = R8 * (R2 * N - YS * YS ) -R3 * (R9 * N - R7 * YS) + XS * (R9 * YS - R7 * R2);
		double E = R1 * (R9 * N- R7 * YS) - R8 * (R3 * N - XS * YS) + XS * (R3 * R7 - XS * R9);
		double F = R1 * (R2 * R7 - YS * R9) - R3 * (R3 * R7 - XS * R9) + R8 * (R3 * YS - XS * R2);
		D_ /= DD;
		E /= DD;
		F /= DD;
		double R4 = 0, R5 = 0, R6 = 0;
		for (int i=0; i<loc.length; i++) {
			R[i][3] = X1[i] - X[i] / L;
			R4 += R[i][3];
			R[i][4] = R[i][3] * X[i];
			R5 += R[i][4];
			R[i][5] = R[i][3] * Y[i];
			R6 += R[i][5];
		}
		double A_ = R5 * (R2 * N - YS * YS) - R3 * (R6 * N - R4 * YS) + XS * (R6 * YS - R4 * R2);
		double B = R1 * (R6 * N - R4 * YS) - R5 * (R3 * N - XS * YS) + XS * (R3 * R4 - XS * R6);
		double C = R1 * (R2 * R4 - YS * R6) - R3 * (R3 * R4 - XS * R6) + R5 * (R3 * YS - XS * R2);
		A_ /= DD;
		B /= DD;
		C /= DD;

		plateConstants = new double[] {A_, B, C, D_, E, F};

		double AS = 0, DS = 0;
		double RA[] = new double[loc.length], RD[] = new double[loc.length];
		for (int i=0; i<loc.length; i++) {
			RA[i] = X[i] - L * (X1[i] - (A_ * X[i] + B * Y[i] + C));
			RD[i] = Y[i] - L * (Y1[i] - (D_ * X[i] + E * Y[i] + F));
			AS += FastMath.pow((RA[i] / L) / Math.cos(loc0.getLatitude()), 2.0);
			DS += FastMath.pow(RD[i] / L, 2.0);
		}
		S1 = Math.sqrt(AS / (N - 3.0));
		S2 = Math.sqrt(DS / (N - 3.0));

		residuals = DataSet.addDoubleArray(RA, RD);
	}

	/**
	 * Returns the equatorial position on the plate for a given
	 * rectangular position. The x position goes from 0 to the
	 * size in pixels of the plate, and the same for y.
	 * @param X The x position.
	 * @param Y The y position.
	 * @return The equatorial coordinates.
	 */
	public LocationElement getPlatePosition(double X, double Y) {
		// Find standard coordinates of target
		double A = plateConstants[0], B = plateConstants[1], C = plateConstants[2], D = plateConstants[3],
			E = plateConstants[4], F = plateConstants[5];
		double XX = A * X + B * Y + C + X / L, YY = D * X + E * Y + F + Y / L;
		B = Math.cos(loc0.getLatitude()) - YY * Math.sin(loc0.getLatitude());
		double G = Math.sqrt(XX * XX + B * B);
		// Find right ascension of target
		double A5 = Math.atan(XX / B);
		if (B<0) A5 += Math.PI;
		double A6 = A5 + loc0.getLongitude();
		// Find declination of target
		double D6 = Math.atan((Math.sin(loc0.getLatitude()) + YY * Math.cos(loc0.getLatitude())) / G);

		return new LocationElement(A6, D6, 1.0);
	}

	/**
	 * Returns the rectangular position on the plate for a given
	 * equatorial position.
	 * @param loc The equatorial position.
	 * @return The rectangular coordinates.
	 */
	public Point2D getPlatePosition(LocationElement loc) {
		// Brute force method
		double X = 500, Y = 500; // First guess
		double dx = 5, dy = 5;
		while (true) {
			LocationElement l1 = getPlatePosition(X, Y);
			LocationElement l2 = getPlatePosition(X + dx, Y);
			if (LocationElement.getAngularDistance(l2, loc) <
				LocationElement.getAngularDistance(l1, loc)) {
				while (true) {
					l2 = getPlatePosition(X + dx, Y);
					if (LocationElement.getAngularDistance(l2, loc) >
							LocationElement.getAngularDistance(l1, loc)) break;
					l1 = l2;
					X += dx;
				};
			} else {
				while (true) {
					l2 = getPlatePosition(X - dx, Y);
					if (LocationElement.getAngularDistance(l2, loc) >
							LocationElement.getAngularDistance(l1, loc)) break;
					l1 = l2;
					X -= dx;
				};
			}

			l2 = getPlatePosition(X, Y + dy);
			if (LocationElement.getAngularDistance(l2, loc) <
				LocationElement.getAngularDistance(l1, loc)) {
				while (true) {
					l2 = getPlatePosition(X, Y + dy);
					if (LocationElement.getAngularDistance(l2, loc) >
							LocationElement.getAngularDistance(l1, loc)) break;
					l1 = l2;
					Y += dy;
				};
			} else {
				while (true) {
					l2 = getPlatePosition(X, Y - dy);
					if (LocationElement.getAngularDistance(l2, loc) >
							LocationElement.getAngularDistance(l1, loc)) break;
					l1 = l2;
					Y -= dy;
				};
			}

			dx = dx / 4.0;
			dy = dy / 4.0;
			if (dx < 1E-4 && dy < 1E-4) break; // better than 0.001 pixel
		}

		return new Point2D.Double(X, Y);


		// The following algorithm does not converge always, specially for loc0
/*		double A50 = loc.getLongitude() - loc0.getLongitude();
		double X = 0, Y = 0; // First guess
		double A = plateConstants[0], B = plateConstants[1], C = plateConstants[2], D = plateConstants[3],
			E = plateConstants[4], F = plateConstants[5];

		// Set an arbitrary number of 20 iterations to converge
		double sinLat0 = Math.sin(loc0.getLatitude()), cosLat0 = Math.cos(loc0.getLatitude());
		double tanDec = Math.tan(loc.getLatitude());
		for (int i=0; i<20; i++) {
			double XX = A * X + B * Y + C + X / L, YY = D * X + E * Y + F + Y / L;
			double BB = cosLat0 - YY * sinLat0;
			double A5 = A50;
			if (BB < 0.0) A5 -= Math.PI;
			XX = Math.tan(A5) * BB;
			double G = Math.sqrt(XX * XX + BB * BB);
			YY = (tanDec * G - sinLat0) / cosLat0;

			Y = (YY - D * X - F) / (E + 1.0 / L);
			X = (XX - B * Y - C) / (A + 1.0 / L);
		}

		return new Point2D.Double(X, Y);
*/
	}

	/**
	 * Returns the residuals for a given position in the plate. Note these
	 * values are not dependent on the position on the plate, they are
	 * constants.
	 * @return The residuals for right ascension and declination, in radians.
	 */
	public double[] getPlatePositionResidual() {
		return new double[] {S1, S2};
	}

	/**
	 * Returns the plate constants named A, B, C, D, E, and F.
	 * @return The plate constants.
	 */
	public double[] getPlateConstants() {
		return plateConstants.clone();
	}

	/**
	 * Returns the residuals for the input equatorial positions
	 * of the reference stars. You can use this data to see if a
	 * given reference star is producing erros in the determination
	 * of the plate constants, and delete that reference star if
	 * necessary.
	 * @return The residuals for right ascension and declination
	 * for all reference stars (radians). If you have 5 reference stars, this
	 * array will have 10 values: first 5 for the residuals in right
	 * ascension for each star, and another 5 for the corresponding
	 * residuals in declination.
	 */
	public double[] getResiduals() {
		return residuals.clone();
	}

	/**
	 * Returns the solution of this plate as an WCS instance. Projection
	 * returned is TAN, and coordinates equatorial, for equinox and epoch 2000.
	 * @param distortion True to fit also the distortion CD polynomial.
	 * @return The WCS instance that fits this plate solution.
	 * @throws JPARSECException If an error occurs.
	 */
	public WCS getAsWCS(boolean distortion) throws JPARSECException {
		WCS wcs = new WCS();
		Point2D p0 = this.getPlatePosition(loc0);
		wcs.setCrpix1(p0.getX());
		wcs.setCrpix2(p0.getY());
		wcs.setCrval1(loc0.getLongitude() * Constant.RAD_TO_DEG);
		wcs.setCrval2(loc0.getLatitude() * Constant.RAD_TO_DEG);

		LocationElement loc = loc0.clone();
		loc.move(Constant.DEG_TO_RAD / Math.cos(loc0.getLatitude()), 0, 0);
		Point2D p1 = this.getPlatePosition(loc);
		double d = FastMath.hypot(p0.getX() - p1.getX(), p0.getY() - p1.getY());
		if (p1.getX() < p0.getX()) d = -d;
		wcs.setCdelt1(1.0 / d);

		loc = loc0.clone();
		if (loc.getLatitude() > 0.0) {
			loc.move(0, -Constant.DEG_TO_RAD, 0);
		} else {
			loc.move(0, Constant.DEG_TO_RAD, 0);
		}
		p1 = this.getPlatePosition(loc);
		d = FastMath.hypot(p0.getX() - p1.getX(), p0.getY() - p1.getY());
		if (loc.getLatitude() > 0.0) {
			if (p1.getY() > p0.getY()) d = -d;
		} else {
			if (p1.getY() < p0.getY()) d = -d;
		}
		wcs.setCdelt2(1.0 / d);
		double ang = -(Math.atan2(p0.getY() - p1.getY(), p0.getX() - p1.getX()) - Constant.PI_OVER_TWO);
		if (loc.getLatitude() > 0.0) ang += Math.PI;
		wcs.setCrota2(ang * Constant.RAD_TO_DEG);

		wcs.setWidth(1000);
		wcs.setHeight(1000);
		//wcs.setProjection(PROJECTION.TAN);
		//wcs.setCoordinateSystem(COORDINATE_SYSTEM.EQUATORIAL);
		//wcs.setEpoch(2000);
		//wcs.setEquinox(2000);

		// Fit the WCS more accurately using 10 random positions. Only crota will
		// usually change
		final int n = 200;
		final double x[] = new double[n], y[] = new double[n];
		final LocationElement locs[] = new LocationElement[n];
		final Point2D p[] = new Point2D[n];
		double minDisp = 0;
		for (int i=0; i<n; i++) {
			x[i] = Math.random() * 1000;
			y[i] = Math.random() * 1000;
			locs[i] = this.getPlatePosition(x[i], y[i]);
			p[i] = new Point2D.Double(x[i], y[i]);
			minDisp += LocationElement.getAngularDistance(locs[i], wcs.getSkyCoordinates(p[i]));
		}
		double dcd1 = 1E-2, dcd2 = 1E-2, dang = 0.5;
		//double dcd1end = 1E-6, dcd2end = 1E-6, dangend = 1E-4, lastIterDisp = -1;
		double dcd11 = 1E-4, dcd12 = 1E-4, dcd21 = 1E-4, dcd22 = 1E-4;
		int i = 0, s = 1;
		if (distortion) wcs.setCD(new double[] {-1E-4, -1E-6, -1E-6, 1E-4}); // initial guess values
		int iter = 0;
		while (true) {
			//if (i == 0 && s == 1) lastIterDisp = minDisp;
			if (i == 0) wcs.setCrota2(wcs.getCrota2() + dang * s);
			if (i == 1) wcs.setCdelt1(wcs.getCdelt1() + dcd1 * s);
			if (i == 2) wcs.setCdelt2(wcs.getCdelt2() + dcd2 * s);
			double cd[] = wcs.getCD();
			if (i == 3) wcs.setCD(new double[] {cd[0] + dcd11 * s, cd[1], cd[2], cd[3]});
			if (i == 4) wcs.setCD(new double[] {cd[0], cd[1] + dcd12 * s, cd[2], cd[3]});
			if (i == 5) wcs.setCD(new double[] {cd[0], cd[1], cd[2] + dcd21 * s, cd[3]});
			if (i == 6) wcs.setCD(new double[] {cd[0], cd[1], cd[2], cd[3] + dcd22 * s});
			// CROTA2 must be 0.0 so that SkyView will use the CD polynomia
			double cr2 = wcs.getCrota2();
			wcs.setCrota2(0.0);
			double newDisp = 0.0;
			for (int j=0; j<n; j++) {
				newDisp += LocationElement.getAngularDistance(locs[j], wcs.getSkyCoordinates(p[j]));
			}
			wcs.setCrota2(cr2);
			if (newDisp < minDisp && newDisp > 0) {
				minDisp = newDisp;
				continue;
			}
			if (i == 0) wcs.setCrota2(wcs.getCrota2() - dang * s);
			if (i == 1) wcs.setCdelt1(wcs.getCdelt1() - dcd1 * s);
			if (i == 2) wcs.setCdelt2(wcs.getCdelt2() - dcd2 * s);
			cd = wcs.getCD();
			if (i == 3) wcs.setCD(new double[] {cd[0] - dcd11 * s, cd[1], cd[2], cd[3]});
			if (i == 4) wcs.setCD(new double[] {cd[0], cd[1] - dcd12 * s, cd[2], cd[3]});
			if (i == 5) wcs.setCD(new double[] {cd[0], cd[1], cd[2] - dcd21 * s, cd[3]});
			if (i == 6) wcs.setCD(new double[] {cd[0], cd[1], cd[2], cd[3] - dcd22 * s});
			if (s == 1) {
				s = -1;
				continue;
			}
			s = 1;
			i ++;
			if (i == 3 && !distortion) {
				i = 0;
				iter ++;
			}
			if (i == 7) {
				i = 3;
				iter ++;
			}
			if (i == 0) dang /= 4.0;
			if (i == 1) dcd1 /= 4.0;
			if (i == 2) dcd2 /= 4.0;
			if (i == 3) dcd11 /= 4.0;
			if (i == 4) dcd12 /= 4.0;
			if (i == 5) dcd21 /= 4.0;
			if (i == 6) dcd22 /= 4.0;
			if (iter > 10) break;
//			if ((lastIterDisp == minDisp) || (dang < dangend && dcd1 < dcd1end && dcd2 < dcd2end)) break;
		}
		return wcs;
	}
}
