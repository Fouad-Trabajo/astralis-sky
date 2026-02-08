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
package jparsec.observer;

import jparsec.ephem.Target.TARGET;
import jparsec.math.FastMath;

/**
 * A class to apply geodetic/geocentric coordinates transformations using
 * different reference ellipsoids.
 *
 * @author T. Alonso Albi - OAN (Spain), M. Huss
 * @version 1.0
 */
public class ReferenceEllipsoid
{
	/** Empty constructor so that this class cannot be instantiated. */
	private ReferenceEllipsoid() { }

	/**
	 * The set of ellipsoids.
	 */
	public enum ELLIPSOID {
		/** ID constant for WSG72 reference ellipsoid. */
		WGS72 (6378.135, 298.26),
		/** ID constant for WSG84 reference ellipsoid. */
		WGS84 (6378.137, 298.257223563),
		/** ID constant for IERS1989 reference ellipsoid. */
		IERS1989 (6378.136, 298.257),
		/** ID constant for Merit (1983) reference ellipsoid. */
		MERIT1983 (6378.137, 298.257),
		/** ID constant for GRS80 reference ellipsoid. */
		GRS80 (6378.137, 298.257222101),
		/** ID constant for GRS67 reference ellipsoid. */
		GRS67 (6378.160, 298.247167),
		/** ID constant for IAU1976 reference ellipsoid. */
		IAU1976 (6378.140, 298.257),
		/** ID constant for IAU1964 reference ellipsoid. */
		IAU1964 (6378.160, 298.25),
		/** ID constant for IERS2003 reference ellipsoid. */
		IERS2003 (6378.1366, 298.25642),
		/** ID constant for the latest reference ellipsoid. */
		LATEST (6378.1366, 298.25642),
		/** ID constant for the ellipsoid of Mercury. */
		MERCURY (TARGET.MERCURY),
		/** ID constant for the ellipsoid of Venus. */
		VENUS (TARGET.VENUS),
		/** ID constant for the ellipsoid of the Moon. */
		Moon (TARGET.Moon),
		/** ID constant for the ellipsoid of Mars. */
		MARS (TARGET.MARS),
		/** ID constant for the ellipsoid of Jupiter. */
		JUPITER (TARGET.JUPITER),
		/** ID constant for the ellipsoid of Saturn. */
		SATURN (TARGET.SATURN),
		/** ID constant for the ellipsoid of Uranus. */
		URANUS (TARGET.URANUS),
		/** ID constant for the ellipsoid of Neptune. */
		NEPTUNE (TARGET.NEPTUNE),
		/** ID constant for the ellipsoid of Pluto. */
		Pluto (TARGET.Pluto),
		/** A custom ellipsoid. Default initial value
		 * is that of the Earth target. */
		CUSTOM (TARGET.EARTH)
		;

		/**
		 * Earth radius in km;
		 */
		private double earthRadius;
		/**
		 * Inverse of the flattening factor =
		 * (equatorial radius / (equatorial radius - polar radius)).
		 */
		private double inverseFlatteningFactor;

		private ELLIPSOID (double a, double f) {
			this.earthRadius = a;
			this.inverseFlatteningFactor = f;
		}

		/**
		 * Sets the current ellipsoid to that of an non-Earth body.
		 * @param target The target.
		 */
		private ELLIPSOID(TARGET target) {
			this.earthRadius = target.equatorialRadius;
			this.inverseFlatteningFactor = target.equatorialRadius / (target.equatorialRadius - target.polarRadius);
		}

		/**
		 * Sets the current ellipsoid to that of an non-Earth body. This method
		 * can only be called using the CUSTOM ellipsoid, otherwise it will have no effect.
		 * @param target The target.
		 */
		public void setEllipsoid(TARGET target) {
			if (this != CUSTOM) return;
			this.earthRadius = target.equatorialRadius;
			this.inverseFlatteningFactor = target.equatorialRadius / (target.equatorialRadius - target.polarRadius);
		}

		/**
		 * Sets the current ellipsoid to that of an non-Earth body. This method
		 * can only be called using the CUSTOM ellipsoid, otherwise it will have no effect.
		 * @param equatorialRadius Equatorial radius in km.
		 * @param polarRadius Polar radius.
		 */
		public void setEllipsoid(double equatorialRadius, double polarRadius) {
			if (this != CUSTOM) return;
			this.earthRadius = equatorialRadius;
			this.inverseFlatteningFactor = equatorialRadius / (equatorialRadius - polarRadius);
		}

		/**
		 * Returns the Earth's equatorial radius for this ellipsoid.
		 * @return Earth radius.
		 */
		public double getEquatorialRadius() {
			return this.earthRadius;
		}

		/**
		 * Returns the polar radius of the Earth.
		 * @return Polar radius.
		 */
		public double getPolarRadius() {
			double f = 1.0 - 1.0 / this.inverseFlatteningFactor;
			return f * this.earthRadius;
		}

		/**
		 * Returns the Earth's radius at a given latitude.
		 * @param latitude Latitude in radians.
		 * @return Earth radius in km.
		 */
		public double getRadiusAtLatitude(double latitude) {
			return this.earthRadius * (1.0 - FastMath.pow(Math.sin(latitude), 2.0) / inverseFlatteningFactor);
		}

		/**
		 * Returns the inverse of the flatenning factor for this ellipsoid.
		 * @return Inverse of flatenning factor, a value around 298.26.
		 */
		public double getInverseOfFlatteningFactor() {
			return this.inverseFlatteningFactor;
		}
	};
}
