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
package jparsec.io.device.implementation;

import jparsec.io.device.GenericCamera;
import jparsec.io.image.HeaderElement;

/**
 * An implementation of the GenericCamera interface for a virtual camera connected
 * to an external telescope. This method just holds the camera name and field shape.
 * @author T. Alonso Albi - OAN (Spain)
 */
public class ExternalCamera implements GenericCamera {

	@Override
	public boolean setISO(String iso) {
		return false;
	}

	@Override
	public String getISO() {
		return null;
	}

	@Override
	public boolean setExpositionTime(String time) {
		return false;
	}

	@Override
	public String getExpositionTime() {
		return null;
	}

	@Override
	public boolean setResolutionMode(String mode) {
		return false;
	}

	@Override
	public String getResolutionMode() {
		return null;
	}

	@Override
	public boolean setAperture(String aperture) {
		return false;
	}

	@Override
	public String getAperture() {
		return null;
	}

	@Override
	public boolean shotAndDownload(boolean keepInCamera) {
		return false;
	}

	@Override
	public boolean isShooting() {
		return false;
	}

	@Override
	public boolean setImageID(IMAGE_ID img) {
		return false;
	}

	@Override
	public IMAGE_ID getImageID() {
		return null;
	}

	@Override
	public String getPathOfLastDownloadedImage() {
		return null;
	}

	@Override
	public boolean setDownloadDirectory(String path) {
		return false;
	}

	@Override
	public boolean setFilter(FILTER filter) {
		return false;
	}

	@Override
	public FILTER getFilter() {
		return null;
	}

	@Override
	public String[] getPossibleISOs() {
		return null;
	}

	@Override
	public String[] getPossibleResolutionModes() {
		return null;
	}

	@Override
	public String[] getPossibleExpositionTimes() {
		return null;
	}

	@Override
	public String[] getPossibleApertures() {
		return null;
	}

	@Override
	public void setCCDorBulbModeTime(int seconds) {
	}

	@Override
	public int getCCDorBulbModeTime() {
		return 0;
	}

	@Override
	public double getInverseElectronicGain() {
		return 0;
	}

	@Override
	public double getSaturationLevelADUs() {
		return 0;
	}

	@Override
	public int getDepth() {
		return 0;
	}

	@Override
	public HeaderElement[] getFitsHeaderOfLastImage() {
		return null;
	}

	@Override
	public boolean isBulb() {
		return false;
	}

	@Override
	public void setMinimumIntervalBetweenShots(int seconds) {
	}

	@Override
	public int getMinimumIntervalBetweenShots() {
		return 0;
	}


	private String name;
	private double ang = 0;
	private CAMERA_MODEL model;

	@Override
	public CAMERA_MODEL getCameraModel() {
		return model;
	}

	@Override
	public String getCameraName() {
		return name;
	}

	@Override
	public void setCameraName(String n) {
		name = n;
	}
	
	@Override
	public double getWidthHeightRatio() {
		return 1;
	}

	@Override
	public boolean setCameraOrientation(double ang) {
		this.ang = ang;
		return true;
	}

	@Override
	public double getCameraOrientation() {
		return ang;
	}

	public ExternalCamera(String name, CAMERA_MODEL model) {
		this.name = name;
		this.model = model;
	}

	@Override
	public long getLastShotStartTime() {
		return 0;
	}

	@Override
	public long getLastShotEndTime() {
		return 0;
	}
}
