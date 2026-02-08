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

import jparsec.astronomy.CoordinateSystem;
import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.Functions;
import jparsec.graph.DataSet;
import jparsec.io.ReadFile;
import jparsec.io.device.GenericCamera;
import jparsec.io.device.GenericCamera.CAMERA_MODEL;
import jparsec.io.device.GenericTelescope;
import jparsec.io.image.HeaderElement;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.TimeElement;
import jparsec.util.JPARSECException;

/**
 * An implementation of an external telescope, to just show a mark in sky rendering
 * with its current location. The mark is a circle. The default internal time object for this
 * telescope is the current CPU date and local time, although there is also a set time method.
 * @author T. Alonso Albi - OAN (Spain)
 */
public class ExternalTelescope implements GenericTelescope {

	@Override
	public boolean hasGOTO() {
		return false;
	}

	@Override
	public boolean hasGPS() {
		return false;
	}

	@Override
	public boolean hasFocuser() {
		return false;
	}

	@Override
	public boolean isMoving() {
		return false;
	}

	@Override
	public boolean isTracking() {
		return true;
	}

	@Override
	public boolean isAligned() {
		return true;
	}

	@Override
	public boolean setTrackingActive(boolean b) {
		return false;
	}

	@Override
	public MOUNT getMount() {
		return null;
	}

	@Override
	public boolean setFocusSpeed(FOCUS_SPEED fs) {
		return false;
	}

	@Override
	public FOCUS_SPEED getFocusSpeed() {
		return null;
	}

	@Override
	public boolean startFocus(FOCUS_DIRECTION fd) {
		return false;
	}

	@Override
	public boolean stopFocus() {
		return false;
	}

	@Override
	public FOCUS_DIRECTION getFocusDirection() {
		return null;
	}

	@Override
	public boolean setMoveSpeed(MOVE_SPEED ms) {
		return false;
	}

	@Override
	public MOVE_SPEED getMoveSpeed() {
		return null;
	}

	@Override
	public boolean startMove(MOVE_DIRECTION md) {
		return false;
	}

	@Override
	public boolean move(MOVE_DIRECTION md, float seconds) {
		return false;
	}

	@Override
	public boolean stopMove(MOVE_DIRECTION md) {
		return false;
	}

	@Override
	public MOVE_DIRECTION getLastMoveDirection() {
		return null;
	}

	@Override
	public boolean setObjectCoordinates(LocationElement loc, String name) {
		return false;
	}

	@Override
	public LocationElement getObjectCoordinates() {
		return null;
	}

	@Override
	public String getObjectName() {
		return null;
	}

	@Override
	public boolean gotoObject() {
		return false;
	}

	@Override
	public double distanceToPosition(LocationElement loc, boolean isEquatorial) {
		return 0;
	}

	@Override
	public boolean isMoving(float seconds, double tolerance) {
		return false;
	}

	@Override
	public double getLocalTime() {
		return 0;
	}

	@Override
	public boolean setLocalTime(TimeElement time, ObserverElement obs) {
		return false;
	}

	@Override
	public boolean sync() {
		return false;
	}

	@Override
	public boolean reset(LocationElement loc) {
		return false;
	}

	@Override
	public boolean stopMoving() {
		return false;
	}

	@Override
	public boolean park() {
		return false;
	}

	@Override
	public boolean setParkPosition(LocationElement loc) {
		return false;
	}

	@Override
	public boolean unpark() {
		return false;
	}
	
	@Override
	public LocationElement getParkPosition() {
		return null;
	}

	@Override
	public boolean invertHorizontally() {
		return false;
	}

	@Override
	public boolean invertVertically() {
		return false;
	}

	@Override
	public void setTelescopeType(TELESCOPE_TYPE type) {
	}

	@Override
	public HeaderElement[] getFitsHeader(int cameraIndex) {
		return null;
	}

	@Override
	public TELESCOPE_MODEL getTelescopeModel() {
		return TELESCOPE_MODEL.EXTERNAL_TELESCOPE_JUST_MARK;
	}

	private boolean connected = true;
	private String name = null, path = null;
	private double fov[] = new double[] {0};
	private ExternalCamera cameras[];
	private POSITION_TYPE type;
	private ObserverElement observer;
	private TimeElement time = null;

	@Override
	public void setCameras(GenericCamera[] cameras) throws JPARSECException {
		fov = new double[cameras.length];
		this.cameras = new ExternalCamera[cameras.length];
		for (int i=0; i<cameras.length; i++) {
			if (!(cameras[i] instanceof ExternalCamera))
				throw new JPARSECException("Cameras for this telescope must be of type ExternalCamera");
			this.cameras[i] = (ExternalCamera) cameras[i];
		}
	}

	@Override
	public GenericCamera[] getCameras() {
		return cameras;
	}


	@Override
	public boolean disconnect() {
		connected = false;
		return true;
	}

	@Override
	public boolean connect() throws JPARSECException {
		connected = true;
		return true;
	}

	@Override
	public boolean isConnected() {
		return connected;
	}

	@Override
	public String getTelescopeName() {
		return name;
	}

	@Override
    public synchronized String getTelescopePort() {
    	return null;
    }

	@Override
	public void setFieldOfView(double field, int camera) {
		fov[camera] = field;
	}

	@Override
	public double getFieldOfView(int camera) {
		return fov[camera];
	}

	@Override
	public ObserverElement getObserver() {
		return observer;
	}
	
	@Override
	public boolean setObserver(ObserverElement o) {
		observer = o;
		return true;
	}

	@Override
	public TimeElement getTime() {
		if (time != null) return time;
		return new TimeElement();
	}


	@Override
	public LocationElement getEquatorialPosition() {
		if (type == POSITION_TYPE.EQUATORIAL_J2000) return getJ2000EquatorialPosition();
		return getApparentEquatorialPosition();
	}

	@Override
	public LocationElement getApparentEquatorialPosition() {
		LocationElement loc = getLocation();
		if (type == POSITION_TYPE.EQUATORIAL_DATE) return loc;

		EphemerisElement eph = new EphemerisElement();
		try {
			TimeElement time = getTime();
			ObserverElement observer = getObserver();
			if (type == POSITION_TYPE.EQUATORIAL_J2000) {
				return Ephem.fromJ2000ToApparentGeocentricEquatorial(loc, time, observer, eph);
			} else {
				return CoordinateSystem.horizontalToEquatorial(loc, time, observer, eph);
			}
		} catch (Exception exc) {
			return null;
		}
	}

	@Override
	public LocationElement getJ2000EquatorialPosition() {
		LocationElement loc = getLocation();
		if (type == POSITION_TYPE.EQUATORIAL_J2000) return loc;

		EphemerisElement eph = new EphemerisElement();
		try {
			TimeElement time = getTime();
			ObserverElement observer = getObserver();
			if (type == POSITION_TYPE.EQUATORIAL_DATE) {
				return Ephem.toMeanEquatorialJ2000(loc, time, observer, eph);
			} else {
				return CoordinateSystem.equatorialToHorizontal(loc, time, observer, eph);
			}
		} catch (Exception exc) {
			return null;
		}
	}

	@Override
	public LocationElement getHorizontalPosition() {
		LocationElement loc = getLocation();
		if (type == POSITION_TYPE.HORIZONTAL) return loc;

		EphemerisElement eph = new EphemerisElement();
		try {
			TimeElement time = getTime();
			ObserverElement observer = getObserver();
			if (type == POSITION_TYPE.EQUATORIAL_J2000)
				loc = Ephem.fromJ2000ToApparentGeocentricEquatorial(loc, time, observer, eph);
			return CoordinateSystem.equatorialToHorizontal(loc, time, observer, eph);
		} catch (Exception exc) {
			return null;
		}
	}

	private LocationElement getLocation() {
		try {
			String file[] = DataSet.arrayListToStringArray(ReadFile.readAnyExternalFile(path));
			LocationElement loc = new LocationElement(
					Functions.parseDeclination(file[0]),
					Functions.parseDeclination(file[1]),
					1.0
			);
			return loc;
		} catch (Exception exc) {
			return new LocationElement();
		}
	}

	/**
	 * The different position types allowed for the external file with
	 * the current telescope coordinates.
	 */
	public enum POSITION_TYPE {
		/** Identifier for equatorial coordinates for equinox of date. */
		EQUATORIAL_DATE,
		/** Identifier for equatorial coordinates for J2000 equinox. */
		EQUATORIAL_J2000,
		/** Identifier for horizontal coordinates. */
		HORIZONTAL
	};

	/**
	 * Sets the time object.
	 * @param time Time object.
	 */
	public void setTime(TimeElement time) {
		this.time = time;
	}

	/**
	 * Constructor for an external telescope.
	 * @param name The telescope name.
	 * @param beam The possible beam marks to show on the sky. Each
	 * value is the field of view in radians for a given frequency.
	 * @param beamNames Names for the different beam sizes or marks.
	 * @param posPath Path to a file containing the current position of the telescope.
	 * The position must be a two line file with longitude and latitude.
	 * @param type Position type for longitude and latitude. Can be RA/DEC of date,
	 * RA/DEC for J2000 equinox, or acimut/elevation. All values must be in degrees.
	 * @param observer The observer object locating the telescope. Time used for this
	 * telescope is the current computer date and local time, although a specific
	 * set time method exits in this class.
	 * @throws JPARSECException If an error occurs.
	 */
	public ExternalTelescope(String name, double beam[], String beamNames[], String posPath,
			POSITION_TYPE type, ObserverElement observer) throws JPARSECException {
		this.name = name;
		fov = beam.clone();
		if (beam.length < 1) throw new JPARSECException("At least one beam required.");
		cameras = new ExternalCamera[beam.length];
		if (beamNames != null) {
			if (beam.length != beamNames.length) throw new JPARSECException("Different number of beams and beam names.");
			for (int i=0; i<beam.length; i++) {
				cameras[i] = new ExternalCamera(beamNames[i], CAMERA_MODEL.VIRTUAL_ROUND_SENSOR);
			}
		}
		path = posPath;
		this.type = type;
		this.observer = observer.clone();
	}
}
