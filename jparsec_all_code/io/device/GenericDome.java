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
package jparsec.io.device;

import jparsec.io.image.HeaderElement;
import jparsec.observer.LocationElement;

/**
 * An interface for the commands that should be available to all domes.
 * @author T. Alonso Albi - OAN (Spain)
 */
public interface GenericDome {

	/**
	 * The set of dome models supported.
	 */
	public enum DOME_MODEL {
		/** A virtual dome for testing. */
		VIRTUAL_ROLL_OFF_ROOF(false),
		/** Another virtual dome for testing. */
		VIRTUAL_DOME(true);

		private boolean hasControl = false;

		private DOME_MODEL(boolean hasLeftRightAzControl) {
			this.hasControl = hasLeftRightAzControl;
		}

		/**
		 * Returns if this dome has control to rotate left/right
		 * and get its azimuth.
		 * @return True or false.
		 */
		public boolean hasLeftRightControl() {
			return hasControl;
		}

		/**
		 * Returns if this dome is virtual or not.
		 * @return True or false.
		 */
		public boolean isVirtual() {
			if (this.name().startsWith("VIRTUAL_")) return true;
			return false;
		}
	}

	/**
	 * Opens the dome.
	 * @return True if the command success.
	 */
	public boolean open();
	/**
	 * Closes the dome.
	 * @return True if the command success.
	 */
	public boolean close();
	/**
	 * Rotates the dome towards left.
	 * @param angle The angle to rotate in radians.
	 * @return True if the command success.
	 */
	public boolean rotateLeft(double angle);
	/**
	 * Rotates the dome towards right.
	 * @param angle The angle to rotate in radians.
	 * @return True if the command success.
	 */
	public boolean rotateRight(double angle);
	/**
	 * Gets the current azimuth of the dome.
	 * @return The azimuth in radians.
	 */
	public double getAzimuth();
	/**
	 * Gets the dome model.
	 * @return Dome model.
	 */
	public DOME_MODEL getDomeModel();
	/**
	 * Returns if the dome is open or not.
	 * @return True or false.
	 */
	public boolean isOpen();
	/**
	 * Synchronize the azimuth position of the dome to
	 * a given horizontal position.
	 * @param locHz The horizontal position of the telescope.
	 * @return True if the command success.
	 */
	public boolean sync(LocationElement locHz);
	/**
	 * Returns if the dome is currently synchronized with a given
	 * azimuth direction.
	 * @param locHz The horizontal position of the telescope.
	 * @return True if the azimuth direction is visible through the
	 * dome's aperture, otherwise false.
	 */
	public boolean isSync(LocationElement locHz);
	/**
	 * Returns the time interval to use to keeps the position
	 * of the dome synchronized with the position of the
	 * telescope.
	 * @return Time in seconds.
	 */
	public double getSyncTime();
	/**
	 * Returns if the dome is rotating or opening, or not moving at all.
	 * @return True or false.
	 */
	public boolean isMoving();
	/**
	 * Returns the fits header describing the status of this dome.
	 * The set of entries are:
	 * <pre>
	 * Entry        Value (typical)   Description
	 * ------------------------------------------
	 *
	 * DOM_AZ      10.1              Dome azimuth (deg)
	 * DOM_OPEN    true              Dome open ?
	 * DOM_MOVI    false             Dome moving ?
	 * DOM_MODE    VIRTUAL           Dome model (driver)
	 * </pre>
	 * @return The set of entries for the fits header.
	 */
	public HeaderElement[] getFitsHeader();
}
