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

import jparsec.astronomy.CoordinateSystem;
import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.Functions;
import jparsec.ephem.Target.TARGET;
import jparsec.io.image.HeaderElement;
import jparsec.math.Constant;
import jparsec.math.Evaluation;
import jparsec.math.FastMath;
import jparsec.observer.City;
import jparsec.observer.CityElement;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.AstroDate;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.time.TimeScale;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;
import jparsec.util.Version;

/**
 * An implementation of a virtual telescope.
 * @author T. Alonso Albi - OAN (Spain)
 */
public class VirtualTelescope implements GenericTelescope {

	private TimeElement time;
	private ObserverElement obs;
	private EphemerisElement eph;
	private LocationElement loc, objLoc, parkPos;
	private String object;
	private TELESCOPE_MODEL model;
	private double[] field = new double[] {-1, -1, -1, -1, -1};
	private MoveThread move;

	private boolean isMoving = false, isMovingS = false, isMovingN = false, isMovingE = false,
			isMovingW = false, connected = true, isTracking = true;
	private FOCUS_SPEED fs = FOCUS_SPEED.SLOW;
	private FOCUS_DIRECTION fd = FOCUS_DIRECTION.IN;
	private MOVE_SPEED ms = MOVE_SPEED.VERY_FAST;
	private MOVE_DIRECTION md = MOVE_DIRECTION.NORTH_UP;
	private double timeOffset = 0.0;
	private static final double MOVE_TOLERANCE_1s = 30 * Constant.ARCSEC_TO_RAD;

	// Speed of movement for the different MOVE_SPEED enum values in degrees per second
	private static final double SPEED[] = new double[] {5, 1, 1.0/6.0, (Constant.SIDEREAL_DAY_LENGTH / Constant.SECONDS_PER_DAY) * 360.0};
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
		return isMoving;
	}

	@Override
    public synchronized boolean setFocusSpeed(FOCUS_SPEED rate) {
		switch(rate) {
		case FAST:
			break;
		case SLOW:
			break;
		}
		fs = rate;
		return false;
    }
	@Override
	public FOCUS_SPEED getFocusSpeed() { return fs; }
	@Override
    public synchronized boolean startFocus(FOCUS_DIRECTION direction) {
		switch(direction) {
		case IN:
			break;
		case OUT:
			break;
		}
		fd = direction;
		return false;
    }
	@Override
	public FOCUS_DIRECTION getFocusDirection() { return fd; }
	@Override
    public synchronized boolean stopFocus() {
		fd = null;
		return false;
    }
	@Override
    public synchronized boolean setMoveSpeed(MOVE_SPEED rate) {
		ms = rate;
		return true;
    }
	@Override
	public MOVE_SPEED getMoveSpeed() { return ms; }
	@Override
    public synchronized boolean startMove(MOVE_DIRECTION direction) {
		switch(direction) {
		case NORTH_UP:
			isMovingN = true;
			break;
		case EAST_LEFT:
			isMovingE = true;
			break;
		case SOUTH_DOWN:
			isMovingS = true;
			break;
		case WEST_RIGHT:
			isMovingW = true;
			break;
		}
		md = direction;
		isMoving = true;

		if (move != null && move.isWorking()) {
			move.shouldStopN = !isMovingN;
			move.shouldStopS = !isMovingS;
			move.shouldStopE = !isMovingE;
			move.shouldStopW = !isMovingW;
			return false;
		} else {
			move = new MoveThread(loc, null, SPEED[ms.ordinal()], getMount() == MOUNT.EQUATORIAL);
			move.shouldStopN = !isMovingN;
			move.shouldStopS = !isMovingS;
			move.shouldStopE = !isMovingE;
			move.shouldStopW = !isMovingW;
			move.start();
		}

		return true;
    }
	@Override
    public synchronized boolean move(MOVE_DIRECTION direction, float seconds) {
		return false;
    }
	@Override
    public synchronized boolean stopMove(MOVE_DIRECTION direction) {
		switch(direction) {
		case NORTH_UP:
			isMovingN = false;
			if (move != null && move.isWorking()) move.shouldStopN = true;
			break;
		case EAST_LEFT:
			isMovingE = false;
			if (move != null && move.isWorking()) move.shouldStopE = true;
			break;
		case SOUTH_DOWN:
			isMovingS = false;
			if (move != null && move.isWorking()) move.shouldStopS = true;
			break;
		case WEST_RIGHT:
			isMovingW = false;
			if (move != null && move.isWorking()) move.shouldStopW = true;
			break;
		}
		isMoving = checkMove();
		return true;
    }
	@Override
	public MOVE_DIRECTION getLastMoveDirection() {return md;}
	@Override
	public synchronized LocationElement getEquatorialPosition() {
        if (isTracking) return loc.clone();
		try {
			if (parkPos != null)
				return CoordinateSystem.horizontalToEquatorial(parkPos, time, obs, eph);
			return CoordinateSystem.horizontalToEquatorial(new LocationElement(0, Constant.PI_OVER_TWO, 1), time, obs, eph);
		} catch (Exception exc) { return null; }
	}
	@Override
	public synchronized LocationElement getApparentEquatorialPosition() {
		LocationElement loc = this.getEquatorialPosition();
		try {
			if (!this.model.isJ2000()) return loc;
			loc = Ephem.fromJ2000ToApparentGeocentricEquatorial(loc, time, obs, eph);
		} catch (Exception exc) {
			exc.printStackTrace();
		}
		return loc;
	}

	@Override
	public synchronized LocationElement getJ2000EquatorialPosition() {
		LocationElement loc = this.getEquatorialPosition();
		if (this.model.isJ2000()) return loc;
		try {
			loc.setRadius(2062650);
			loc = Ephem.toMeanEquatorialJ2000(loc, time, obs, eph);
		} catch (Exception exc) {
			exc.printStackTrace();
		}
		return loc;
	}
	@Override
	public synchronized LocationElement getHorizontalPosition() {
		if (!isTracking()) {
			if (parkPos != null) return parkPos;
			return new LocationElement(0, Constant.PI_OVER_TWO, 1);
		}

		updateTime();
		try {
			return CoordinateSystem.equatorialToHorizontal(getApparentEquatorialPosition(), time, obs, eph);
		} catch (Exception e) {
			return null;
		}
	}
	@Override
    public synchronized boolean setObjectCoordinates(LocationElement loc, String name) {
		objLoc = new LocationElement(Functions.normalizeRadians(loc.getLongitude()), loc.getLatitude(), 1);
		object = name;
		return true;
    }
	@Override
    public synchronized LocationElement getObjectCoordinates() {
		return objLoc;
    }

	@Override
    public synchronized boolean gotoObject() {
		LocationElement loc = objLoc.clone();
		try {
			updateTime();
			if (model.isJ2000()) loc = Ephem.toMeanEquatorialJ2000(loc, time, obs, eph);
			LocationElement locHZ = CoordinateSystem.equatorialToHorizontal(loc, time, obs, eph);
			if (locHZ.getLatitude() < 0.0) return false;
		} catch (Exception e) {
			return false;
		}

		if (move != null && move.isWorking()) return false;
		if (!isConnected()) return false;
		
		move = new MoveThread(this.loc, loc, SPEED[0], getMount() == MOUNT.EQUATORIAL);
		move.start();
		return true;
    }
	@Override
    public synchronized double distanceToPosition(LocationElement loc, boolean isEquatorial) {
    	if (isEquatorial) {
    		return LocationElement.getAngularDistance(loc, getEquatorialPosition());
    	} else {
    		return LocationElement.getAngularDistance(loc, getHorizontalPosition());
    	}
    }
	@Override
    public synchronized boolean isMoving(float seconds, double tolerance) {
    	LocationElement pos = this.getEquatorialPosition();
        try {
        	Thread.sleep((int) (seconds*1000));
        } catch(InterruptedException e) {}
    	return this.distanceToPosition(pos, true) > tolerance;
    }
	@Override
    public synchronized double getLocalTime() {
		try {
			updateTime();
			return AstroDate.getDayFraction(time.astroDate.jd()) * 24.0;
		} catch (Exception e) {
			return -1;
		}
    }
	@Override
    public synchronized boolean setLocalTime(TimeElement time, ObserverElement obs) {
		try {
			double hours = AstroDate.getDayFraction(time.astroDate.jd()) * 24.0;
			timeOffset = hours - getLocalTime();
	    	return true;
		} catch (Exception exc) {}
		return false;
    }
	@Override
    public synchronized boolean sync() {
		if (this.distanceToPosition(objLoc, true) < 5 * Constant.DEG_TO_RAD) {
			loc = objLoc.clone();
			return true;
		}
		return false;
    }
	@Override
    public synchronized boolean stopMoving() {
   		isMoving = false;
   		isMovingE = isMovingN = isMovingW = isMovingS = false;
		if (move != null && move.isWorking()) move.shouldStop = true;
   		return true;
    }
	@Override
    public synchronized boolean park() {
		try {
			LocationElement loc0 = null;
			if (objLoc != null) loc0 = objLoc.clone();
			updateTime();
			LocationElement loc = null;
			if (parkPos != null) {
				loc = CoordinateSystem.horizontalToEquatorial(parkPos, time, obs, eph);
			} else {
				loc = new LocationElement(0, Constant.PI_OVER_TWO, 1);
				if (obs.getLatitudeDeg() < 0) loc.setLatitude(-loc.getLatitude());
			}
			this.setObjectCoordinates(loc, "Park position");
			if (!this.gotoObject()) {
				objLoc = loc0;
				return false;
			}
			objLoc = loc0;
			disconnect();
			isTracking = false;
			return true;
		} catch (JPARSECException e) {
			return false;
		}
    }
	@Override
    public synchronized boolean setParkPosition(LocationElement loc) {
		parkPos = loc;
		return true;
	}
	@Override
	public LocationElement getParkPosition() {
		if (parkPos != null) return parkPos;
		try {
			LocationElement loc = new LocationElement(0, Constant.PI_OVER_TWO, 1);
			if (obs.getLatitudeDeg() < 0) loc.setLatitude(-loc.getLatitude());
			
			TimeElement time = getTime();
			EphemerisElement eph = new EphemerisElement(TARGET.NOT_A_PLANET, EphemerisElement.COORDINATES_TYPE.APPARENT,
					EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.TOPOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
					EphemerisElement.FRAME.DYNAMICAL_EQUINOX_J2000,
					EphemerisElement.ALGORITHM.MOSHIER);
			eph.preferPrecisionInEphemerides = false;
			eph.correctForEOP = false;
			eph.correctForPolarMotion = false;

			loc = CoordinateSystem.equatorialToHorizontal(loc, time, obs, eph);
			return loc;
		} catch (Exception exc) { exc.printStackTrace(); }
		return null;
	}
	@Override
    public synchronized boolean unpark() {
		try {
			connect();
			isTracking = true;
			return true;
		} catch (JPARSECException e) {
			return false;
		}
    }
	@Override
    public synchronized String getTelescopeName() {
		return Translate.translate(1185);
    }
	@Override
    public synchronized String getTelescopePort() {
    	return null;
    }
	@Override
    public synchronized ObserverElement getObserver() {
		return obs;
    }
	@Override
    public synchronized boolean setObserver(ObserverElement o) {
		obs = o;
		return true;
    }
	@Override
    public synchronized TimeElement getTime() {
		updateTime();
		return time;
    }
	@Override
    public synchronized boolean isTracking() {
		return connected && isTracking;
    }
	@Override
    public synchronized boolean isAligned() {
		return connected && true;
    }
	@Override
    public synchronized MOUNT getMount() {
		if (this.model == TELESCOPE_MODEL.VIRTUAL_TELESCOPE_AZIMUTHAL_MOUNT) return MOUNT.AZIMUTHAL;
		return MOUNT.EQUATORIAL;
    }
	@Override
    public synchronized boolean disconnect() {
		connected = false;
    	return true;
    }
	@Override
    public synchronized boolean connect() throws JPARSECException {
		connected = true;
    	return true;
    }
	@Override
    public synchronized boolean isConnected() {
		return connected;
    }

    private boolean checkMove() {
    	if (isMovingN || isMovingE || isMovingW || isMovingS) return true;
		return this.isMoving(1, MOVE_TOLERANCE_1s);
    }

    private void updateTime() {
    	time = new TimeElement();
    	try {
			time.add(timeOffset / 24.0);
		} catch (JPARSECException e) {
			time = null;
		}
    }

    @Override
    public TELESCOPE_MODEL getTelescopeModel() {
    	return TELESCOPE_MODEL.VIRTUAL_TELESCOPE_EQUATORIAL_MOUNT;
    }

    @Override
    public double getFieldOfView(int camera) {
    	return field[camera];
    }

    @Override
    public void setFieldOfView(double field, int camera) {
    	this.field[camera] = field;
    }

	private GenericCamera[] cameras;
	@Override
	public GenericCamera[] getCameras() {
		return cameras;
	}

	@Override
	public void setCameras(GenericCamera[] cameras) throws JPARSECException {
		this.cameras = cameras;
	}

	@Override
	public boolean invertHorizontally() {
		return type.invertH();
	}

	@Override
	public boolean invertVertically() {
		return type.invertV();
	}

	private TELESCOPE_TYPE type = TELESCOPE_TYPE.SCHMIDT_CASSEGRAIN;
	@Override
	public void setTelescopeType(TELESCOPE_TYPE type) {
		this.type = type;
	}

	@Override
	public String getObjectName() {
		return object;
	}

	@Override
	public HeaderElement[] getFitsHeader(int cameraIndex) {
		try {
			TimeElement time = getTime();
			ObserverElement obs = this.getObserver();
			EphemerisElement eph = this.eph;
			double jd = TimeScale.getJD(time, obs, eph, SCALE.UNIVERSAL_TIME_UTC);

			LocationElement eq = this.getEquatorialPosition(), hz = this.getHorizontalPosition();
			LocationElement eqapp = this.getApparentEquatorialPosition(), eq2000 = this.getJ2000EquatorialPosition();

			HeaderElement header0[] = new HeaderElement[] {
					new HeaderElement("BITPIX", "32", "Bits per data value"),
					new HeaderElement("NAXIS", "2", "Dimensionality"),
					new HeaderElement("NAXIS1", "0", "Width"),
					new HeaderElement("NAXIS2", "0", "Height"),
					new HeaderElement("EXTEND", "T", "Extension permitted"),
					new HeaderElement("AUTHOR", Version.PACKAGE_NAME+" "+Version.VERSION_ID+", "+Version.AUTHOR, "Data author"),
					new HeaderElement("BUNIT", "counts", "Physical unit"),
					new HeaderElement("BSCALE", "1.0", "Data scaling factor"),
					new HeaderElement("BZERO", "0", "Zero value"),
					new HeaderElement("DATE-OBS", ""+getTime().toString(), "Date and time, usually in LT"),
					new HeaderElement("TIME_JD", ""+jd, "Date and time as JD, in UTC"),
					new HeaderElement("OBS_LON", ""+obs.getLongitudeDeg(), "Longitude in deg, west negative"),
					new HeaderElement("OBS_LAT", ""+obs.getLatitudeDeg(), "Latitude in deg, south negative"),
					new HeaderElement("OBS_NAME", obs.getName(), "Observer location name"),
					new HeaderElement("OBS_TZ", ""+obs.getTimeZone(), "Time zone"),
					new HeaderElement("OBS_DST", obs.getDSTCode().name(), "DST code"),
					new HeaderElement("TEL_MODE", this.model.name(), "Telescope model (driver)"),
					new HeaderElement("TEL_TYPE", this.type.name(), "Telescope type (S/C, refractor, ...)"),
					new HeaderElement("TELESCOP", this.getTelescopeName(), "Telescope name"),
					new HeaderElement("MOUNT", this.getMount().name(), "Telescope mount"),
					new HeaderElement("CONNECT", ""+this.isConnected(), "Telescope connected ?"),
					new HeaderElement("TRACKING", ""+this.isTracking(), "Telescope tracking ?"),
					new HeaderElement("ALIGNED", ""+this.isAligned(), "Telescope aligned ?"),
					new HeaderElement("MOVING", ""+this.isMoving(), "Telescope moving ?"),
					new HeaderElement("OBJECT", object, "Object name"),
					new HeaderElement("RA", ""+eqapp.getLongitude(), "Telescope apparent, unrefracted RA"),
					new HeaderElement("DEC", ""+eqapp.getLatitude(), "Telescope apparent, unrefracted DEC"),
					new HeaderElement("RAJ2000", ""+eq2000.getLongitude(), "Telescope J2000 RA"),
					new HeaderElement("DECJ2000", ""+eq2000.getLatitude(), "Telescope J2000 DEC"),
					new HeaderElement("AZ", ""+hz.getLongitude(), "Telescope AZ"),
					new HeaderElement("EL", ""+hz.getLatitude(), "Telescope EL")
			};

			if (cameraIndex < 0) return header0;

			GenericCamera camera = this.getCameras()[cameraIndex];
			if (isTracking()) {
				try {
					AstroDate astro = new AstroDate(camera.getLastShotStartTime());
					double timeExp = camera.getCCDorBulbModeTime();
					if (!camera.isBulb()) timeExp = Evaluation.evaluate(camera.getExpositionTime(), null);
					time = new TimeElement(astro, TimeElement.SCALE.UNIVERSAL_TIME_UTC);
					int jdi = HeaderElement.getIndex(header0, "TIME_JD");
					header0[jdi].value = astro.jd();
					
					LocationElement hz0 = CoordinateSystem.equatorialToHorizontal(eq, time, obs, eph);
					header0 = HeaderElement.addHeaderEntry(header0, new HeaderElement("DATE0", time.toString(), "Date and time for the beginning of the observation"));
					header0 = HeaderElement.addHeaderEntry(header0, new HeaderElement("AZ0", ""+hz0.getLongitude(), "Telescope AZ for the beginning of the observation"));
					header0 = HeaderElement.addHeaderEntry(header0, new HeaderElement("EL0", ""+hz0.getLatitude(), "Telescope EL for the beginning of the observation"));
					time.add(0.5 * timeExp / Constant.SECONDS_PER_DAY);
					hz0 = CoordinateSystem.equatorialToHorizontal(eq, time, obs, eph);
					header0 = HeaderElement.addHeaderEntry(header0, new HeaderElement("DATE-EFF", time.toString(), "Date and time for the middle of the observation"));
					header0 = HeaderElement.addHeaderEntry(header0, new HeaderElement("AZ-EFF", ""+hz0.getLongitude(), "Telescope AZ for the middle of the observation"));
					header0 = HeaderElement.addHeaderEntry(header0, new HeaderElement("EL-EFF", ""+hz0.getLatitude(), "Telescope EL for the middle of the observation"));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			HeaderElement cameraHeader[] = camera.getFitsHeaderOfLastImage();
			cameraHeader = HeaderElement.addHeaderEntry(cameraHeader, new HeaderElement("FIELD", Functions.formatAngleAsDegrees(this.getFieldOfView(cameraIndex), 3), "Camera field of view (deg)"));
			cameraHeader = HeaderElement.addHeaderEntry(cameraHeader, new HeaderElement("CAM_INDE", ""+cameraIndex, "Camera index id value"));

			return HeaderElement.addHeaderEntry(header0, cameraHeader);
		} catch (Exception exc) {
			exc.printStackTrace();
			return null;
		}
	}

    /**
     * Constructor for a virtual telescope.
     * @param telescope The telescope model.
     * @throws JPARSECException If no serial ports are available.
     */
	public VirtualTelescope(TELESCOPE_MODEL telescope) throws JPARSECException {
       	setFocusSpeed(fs);
       	setMoveSpeed(ms);
       	this.model = telescope;

       	time = new TimeElement();
       	try {
       		obs = new ObserverElement("");
       	} catch (Exception exc) {
       		CityElement city = City.findCity("Madrid");
       		obs = ObserverElement.parseCity(city);
       	}
		eph = new EphemerisElement(TARGET.NOT_A_PLANET, EphemerisElement.COORDINATES_TYPE.APPARENT,
				EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.TOPOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
				EphemerisElement.FRAME.DYNAMICAL_EQUINOX_J2000,
				EphemerisElement.ALGORITHM.MOSHIER);
		eph.preferPrecisionInEphemerides = false;
		eph.correctForEOP = false;
		eph.correctForPolarMotion = false;

		loc = CoordinateSystem.horizontalToEquatorial(new LocationElement(0, Constant.PI_OVER_TWO, 1), time, obs, eph);
	}

	class MoveThread extends Thread {
		private LocationElement loc0, loc1;
		private double speed;
		private boolean eq;

		public boolean shouldStop = false, shouldStopN = true, shouldStopS = true, shouldStopE = true, shouldStopW = true;
		public MoveThread(LocationElement loc0, LocationElement loc1, double speed, boolean eq) {
			this.loc0 = loc0.clone();
			this.loc1 = null;
			if (loc1 != null) this.loc1 = loc1.clone();
			if (!eq) {
				try {
					this.loc0 = CoordinateSystem.equatorialToHorizontal(loc0, time, obs, eph);
					if (loc1 != null) this.loc1 = CoordinateSystem.equatorialToHorizontal(loc1, time, obs, eph);
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("ERROR MOVING THE VIRTUAL TELESCOPE !");
				}
			}
			this.speed = speed * Constant.DEG_TO_RAD;
			this.eq = eq;
		}
		@Override
		public void run() {
			try {
				long t0 = System.currentTimeMillis();
				sleep(100);

				double dlat = 0, dlon = 0;
				if (loc1 != null) {
					dlat = loc1.getLatitude() - loc0.getLatitude();
					dlon = Functions.normalizeRadians(loc1.getLongitude() - loc0.getLongitude());
					if (dlon > Math.PI) dlon -= Constant.TWO_PI;
					dlat = FastMath.sign(dlat);
					dlon = FastMath.sign(dlon);
					if (dlat == 0.0 && dlon == 0.0) {
						shouldStop = true;
						return;
					}
				}
				isMoving = true;
				do {
					// Update elapsed time and amount of movement
					long t1 = System.currentTimeMillis();
					double ang = speed * (t1 - t0) * 0.001;
					t0 = t1;

					// Move
					if (loc1 == null) {
						if (!shouldStopN) loc0.setLatitude(loc0.getLatitude() + ang);
						if (!shouldStopS) loc0.setLatitude(loc0.getLatitude() - ang);
						if (!shouldStopE) loc0.setLongitude(loc0.getLongitude() + ang);
						if (!shouldStopW) loc0.setLongitude(loc0.getLongitude() - ang);
						if (shouldStopN && shouldStopS && shouldStopE && shouldStopW) shouldStop = true;
					} else {
						double newLon = loc0.getLongitude() + ang * dlon;
						double newLat = loc0.getLatitude() + ang * dlat;
						if (dlon != 0.0) {
							if (dlon > 0 && newLon > loc1.getLongitude()) {
								newLon = loc1.getLongitude();
								dlon = 0.0;
							}
							if (dlon < 0 && newLon < loc1.getLongitude()) {
								newLon = loc1.getLongitude();
								dlon = 0.0;
							}
						}
						if (dlat != 0.0) {
							if (dlat > 0 && newLat > loc1.getLatitude()) {
								newLat = loc1.getLatitude();
								dlat = 0.0;
							}
							if (dlat < 0 && newLat < loc1.getLatitude()) {
								newLat = loc1.getLatitude();
								dlat = 0.0;
							}
						}
						loc0.setLongitude(newLon);
						loc0.setLatitude(newLat);
					}

					// Set new location
					if (eq) {
						loc = loc0;
					} else {
						try {
							loc  = CoordinateSystem.horizontalToEquatorial(loc0, time, obs, eph);
						} catch (Exception e) {
							e.printStackTrace();
							System.out.println("ERROR MOVING THE VIRTUAL TELESCOPE !");
						}
					}

					sleep(100);
				} while(!shouldStop && (loc1 == null || (dlon != 0.0 || dlat != 0.0)));
				shouldStop = true;
				isMoving = false;

				if (loc1 != null && loc1.equals(objLoc)) loc = objLoc.clone();
			} catch (Exception exc) {
				exc.printStackTrace();
				System.out.println("ERROR MOVING THE VIRTUAL TELESCOPE !");
			}
		}

		/** Returns if the thread is working or not. */
		public boolean isWorking() {
			return !shouldStop;
		}
	}

	@Override
	public boolean hasGOTO() {
		return true;
	}

	@Override
	public boolean reset(LocationElement loc) {
		return false;
	}

	@Override
	public boolean setTrackingActive(boolean b) {
		isTracking = b;
		return true;
	}
}
