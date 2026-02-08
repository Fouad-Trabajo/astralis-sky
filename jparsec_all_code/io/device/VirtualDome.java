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

import javax.swing.JOptionPane;

import jparsec.ephem.Functions;
import jparsec.io.image.HeaderElement;
import jparsec.math.Constant;
import jparsec.observer.LocationElement;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;

/**
 * An implementation of the dome interface for virtual domes.
 * @author T. Alonso Albi - OAN (Spain)
 */
public class VirtualDome implements GenericDome {

	private DOME_MODEL dome;
	private boolean isOpen = false;
	private double azimuth = Math.PI;

	@Override
	public boolean open() {
		if (isOpen) return false;
		isOpen = true;
		return true;
	}

	@Override
	public boolean close() {
		if (!isOpen) return false;
		isOpen = false;
		return true;
	}

	@Override
	public boolean rotateLeft(double angle) {
		if (!dome.hasLeftRightControl()) return false;
		azimuth -= angle;
		return true;
	}

	@Override
	public boolean rotateRight(double angle) {
		if (!dome.hasLeftRightControl()) return false;
		azimuth += angle;
		return true;
	}

	@Override
	public double getAzimuth() {
		if (!dome.hasLeftRightControl()) return 0;
		azimuth = Functions.normalizeRadians(azimuth);
		return azimuth;
	}

	@Override
	public DOME_MODEL getDomeModel() {
		return dome;
	}

	@Override
	public boolean isOpen() {
		return isOpen;
	}

	@Override
	public boolean sync(LocationElement locHz) {
		if (getDomeModel().hasLeftRightControl() && !isMoving()) {
			try {
				double angle = Functions.normalizeRadians(locHz.getLongitude() - getAzimuth());
				if (angle > Math.PI) angle -= Constant.TWO_PI;
				if (angle != 0) {
					if (angle > 0) {
						rotateRight(angle);
					} else {
						rotateLeft(-angle);
					}
				}
				return true;
			} catch (Exception exc) {
				JOptionPane.showMessageDialog(null, Translate.translate(1159), Translate.translate(240), JOptionPane.WARNING_MESSAGE);
			}
		}
		return false;
	}

	@Override
	public boolean isSync(LocationElement locHz) {
		double dif = Math.abs(this.getAzimuth() - locHz.getLongitude());
		if (dif > Math.PI) dif = Constant.TWO_PI - dif;
		double aperture = 16; // Total aperture of the dome in degrees
		return (aperture * 0.5 - dif * Constant.RAD_TO_DEG) > 1; // 1 deg of margin
	}

	@Override
	public double getSyncTime() {
		return 60.0;
	}

	@Override
	public boolean isMoving() {
		return false;
	}

	@Override
	public HeaderElement[] getFitsHeader() {
		HeaderElement header[] = new HeaderElement[] {
				new HeaderElement("DOM_AZ", Functions.formatAngle(this.getAzimuth(), 3), "Dome azimuth (deg)"),
				new HeaderElement("DOM_OPEN", ""+this.isOpen, "Dome open ?"),
				new HeaderElement("DOM_MOVI", ""+this.isMoving(), "Dome moving ?"),
				new HeaderElement("DOM_MODE", ""+this.dome.name(), "Dome model (driver)"),
		};
		return header;
	}

	/**
	 * Constructor for a virtual dome.
	 * @param dome The dome mode.
	 * @throws JPARSECException If the dome is not virtual.
	 */
	public VirtualDome(DOME_MODEL dome) throws JPARSECException {
		if (!dome.isVirtual()) throw new JPARSECException("This is not a virtual dome!");
		this.dome = dome;
	}
}
