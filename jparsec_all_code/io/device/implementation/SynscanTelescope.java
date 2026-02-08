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

import javax.swing.JOptionPane;

import jparsec.astronomy.CoordinateSystem;
import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.Functions;
import jparsec.ephem.Target.TARGET;
import jparsec.io.device.GenericCamera;
import jparsec.io.device.GenericTelescope;
import jparsec.io.device.SerialConnection;
import jparsec.io.image.HeaderElement;
import jparsec.math.Constant;
import jparsec.math.Evaluation;
import jparsec.math.FastMath;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.AstroDate;
import jparsec.time.TimeElement;
import jparsec.time.TimeScale;
import jparsec.time.TimeElement.SCALE;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;
import jparsec.util.Version;

/**
 * An implementation of a Synscan telescope, identical to Celestron.
 * @author T. Alonso Albi - OAN (Spain)
 */
public class SynscanTelescope implements GenericTelescope {

    /** Constant for 2 raised to 16th power */
    private static final double TWO_EXP_16 = 65536.0; // 2 ^ 16

    /** Constant for 2 raised to 24th power */
    private static final double TWO_EXP_24 = 16777216.0;  // 2 ^ 24;

    /** 16 bit constant to convert degrees to step count required for goto */
    private static final double DEGREES_TO_COUNT_16 = TWO_EXP_16 / 360.0;

    /** 24 bit constant to convert degrees to step count required for goto */
    private static final double DEGREES_TO_COUNT_24 = TWO_EXP_24 / 360.0;

    /** 16 bit constant to convert count to degrees */
    private static final double COUNT_TO_DEGREES_16 = 360.0 / TWO_EXP_16;

    /** 24 bit constant to convert count to degrees */
    private static final double COUNT_TO_DEGREES_24 = 360.0 / TWO_EXP_24;

    /** String containing zeros, used for padding */
    private static final String STR0 = "00000000";

    /** Define null char */
    private static final char NULL_CHAR = Character.MIN_VALUE;

	private SerialConnection sc;

	private TELESCOPE_MODEL telescopeModel = null;
	private boolean isMoving = false, isMovingS = false, isMovingN = false, isMovingE = false, isMovingW = false;
	private FOCUS_SPEED fs = FOCUS_SPEED.SLOW;
	private FOCUS_DIRECTION fd = FOCUS_DIRECTION.IN;
	private MOVE_SPEED ms = MOVE_SPEED.VERY_FAST;
	private MOVE_DIRECTION md = MOVE_DIRECTION.NORTH_UP;
	private LocationElement objLoc = null, parkPos;
	private boolean highPrecision = true;
	private int trackingMode = 0;
	private double[] field = new double[] {-1, -1, -1, -1, -1};
	private TimeElement time0;
	private ObserverElement obs;
	private double timeOffset = 0;
	private LocationElement lastEq = new LocationElement(), lastHz = new LocationElement();
	//private boolean noGW = false;
	private String object, telName;
	private int nullResp = 0;
	private static final double MOVE_TOLERANCE_1s = 30 * Constant.ARCSEC_TO_RAD;

	@Override
	public boolean hasGPS() {
		if (telescopeModel == TELESCOPE_MODEL.CELESTRON_NEXSTAR_GPS) return true;
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
		fs = rate;
		return true;
    }
	@Override
	public FOCUS_SPEED getFocusSpeed() { return fs; }
	@Override
    public synchronized boolean startFocus(FOCUS_DIRECTION direction) {
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
		String cmd = "";
		//boolean out = false;
		int sp[] = new int[] {9, 6, 3, 1};
		int speed = sp[ms.ordinal()];
		switch(direction) {
		case NORTH_UP:
			cmd = "P" + (char) 2 + (char) 17 + (char) 36 + (char) speed + (char) 0 + (char) 0 + (char) 0;
			break;
		case WEST_RIGHT:
			cmd = "P" + (char) 2 + (char) 16 + (char) 36 + (char) speed + (char) 0 + (char) 0 + (char) 0;
			break;
		case SOUTH_DOWN:
			cmd = "P" + (char) 2 + (char) 17 + (char) 37 + (char) speed + (char) 0 + (char) 0 + (char) 0;
			break;
		case EAST_LEFT:
			cmd = "P" + (char) 2 + (char) 16 + (char) 37 + (char) speed + (char) 0 + (char) 0 + (char) 0;
			break;
		}
		sendCmdAndReceiveResponse(cmd);
		md = direction;
		isMoving = true;
		if (direction == MOVE_DIRECTION.NORTH_UP) isMovingN = true;
		if (direction == MOVE_DIRECTION.EAST_LEFT) isMovingE = true;
		if (direction == MOVE_DIRECTION.WEST_RIGHT) isMovingW = true;
		if (direction == MOVE_DIRECTION.SOUTH_DOWN) isMovingS = true;
		return true;
		/*
		out = sendCmd(cmd);
		if (out) {
			readString();
			md = direction;
			isMoving = true;
			if (direction == MOVE_DIRECTION.NORTH_UP) isMovingN = true;
			if (direction == MOVE_DIRECTION.EAST_LEFT) isMovingE = true;
			if (direction == MOVE_DIRECTION.WEST_RIGHT) isMovingW = true;
			if (direction == MOVE_DIRECTION.SOUTH_DOWN) isMovingS = true;
		}
		return out;
		*/
    }
	@Override
    public synchronized boolean move(MOVE_DIRECTION direction, float seconds) {
		return false; // unsupported

    }
	@Override
    public synchronized boolean stopMove(MOVE_DIRECTION direction) {
		String cmd = "";
		//boolean out = false;
		int speed = 0;
		switch(direction) {
		case NORTH_UP:
			cmd = "P" + (char) 2 + (char) 17 + (char) 36 + (char) speed + (char) 0 + (char) 0 + (char) 0;
			break;
		case WEST_RIGHT:
			cmd = "P" + (char) 2 + (char) 16 + (char) 36 + (char) speed + (char) 0 + (char) 0 + (char) 0;
			break;
		case SOUTH_DOWN:
			cmd = "P" + (char) 2 + (char) 17 + (char) 37 + (char) speed + (char) 0 + (char) 0 + (char) 0;
			break;
		case EAST_LEFT:
			cmd = "P" + (char) 2 + (char) 16 + (char) 37 + (char) speed + (char) 0 + (char) 0 + (char) 0;
			break;
		}
		sendCmdAndReceiveResponse(cmd);
		if (direction == MOVE_DIRECTION.NORTH_UP) isMovingN = false;
		if (direction == MOVE_DIRECTION.EAST_LEFT) isMovingE = false;
		if (direction == MOVE_DIRECTION.WEST_RIGHT) isMovingW = false;
		if (direction == MOVE_DIRECTION.SOUTH_DOWN) isMovingS = false;
		isMoving = checkMove();
		return true;
		/*
		out = sendCmd(cmd);
		if (out) {
			readString();
			if (direction == MOVE_DIRECTION.NORTH_UP) isMovingN = false;
			if (direction == MOVE_DIRECTION.EAST_LEFT) isMovingE = false;
			if (direction == MOVE_DIRECTION.WEST_RIGHT) isMovingW = false;
			if (direction == MOVE_DIRECTION.SOUTH_DOWN) isMovingS = false;
			isMoving = checkMove();
		}
		return out;
		*/
    }
	@Override
	public MOVE_DIRECTION getLastMoveDirection() {return md;}
	@Override
	public synchronized LocationElement getEquatorialPosition() {
		if (sc.getTimeSinceLastCommand() > SerialConnection.MAX_TIME_WAIT_FOR_RESPONSE_MS) {
    		String cmd = "e";
    		if (!highPrecision) cmd = "E";

    		String radec = sendCmdAndReceiveResponse(cmd);
			if (radec == null || radec.equals("")) return lastEq;
    		LocationElement lastEq = decodeHexadecimal(radec);
            if (lastEq != null && lastEq.getLatitude() != 0.0 && lastEq.getLongitude() != 0) {
	            if (this.lastEq != null) isMoving = LocationElement.getAngularDistance(lastEq, this.lastEq) > MOVE_TOLERANCE_1s;
	    		this.lastEq = lastEq;
	            return lastEq;
            }
		}
		if (lastEq == null) return null;
		return lastEq.clone();
	}
	@Override
	public synchronized LocationElement getApparentEquatorialPosition() {
		LocationElement loc = this.getEquatorialPosition();
		if (loc == null) return loc;
		EphemerisElement eph = new EphemerisElement(TARGET.NOT_A_PLANET, EphemerisElement.COORDINATES_TYPE.APPARENT,
				EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.TOPOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
				EphemerisElement.FRAME.DYNAMICAL_EQUINOX_J2000,
				EphemerisElement.ALGORITHM.MOSHIER);
		eph.preferPrecisionInEphemerides = false;
		eph.correctForEOP = false;
		eph.correctForPolarMotion = false;
		TimeElement time = getTime();
		try {
			if (!this.telescopeModel.isJ2000()) return loc;
			if (obs == null) getObserver();
			loc = Ephem.fromJ2000ToApparentGeocentricEquatorial(loc, time, obs, eph);
		} catch (Exception exc) {
			exc.printStackTrace();
		}
		return loc;
	}

	@Override
	public synchronized LocationElement getJ2000EquatorialPosition() {
		LocationElement loc = this.getEquatorialPosition();
		if (loc == null || this.telescopeModel.isJ2000()) return loc;
		EphemerisElement eph = new EphemerisElement(TARGET.NOT_A_PLANET, EphemerisElement.COORDINATES_TYPE.APPARENT,
				EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.TOPOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
				EphemerisElement.FRAME.DYNAMICAL_EQUINOX_J2000,
				EphemerisElement.ALGORITHM.MOSHIER);
		eph.preferPrecisionInEphemerides = false;
		eph.correctForEOP = false;
		eph.correctForPolarMotion = false;
		TimeElement time = getTime();
		try {
			loc.setRadius(2062650);
			if (obs == null) getObserver();
			loc = Ephem.toMeanEquatorialJ2000(loc, time, obs, eph);
		} catch (Exception exc) {
			exc.printStackTrace();
		}
		return loc;
	}

	@Override
	public synchronized LocationElement getHorizontalPosition() {
		if (sc.getTimeSinceLastCommand() > SerialConnection.MAX_TIME_WAIT_FOR_RESPONSE_MS) {
    		String cmd = "z";
    		if (!highPrecision) cmd = "Z";

    		String azel = sendCmdAndReceiveResponse(cmd);
			if (azel == null || azel.equals("")) return lastHz;
    		LocationElement lastHz = decodeHexadecimal(azel);
            if (lastHz != null && lastHz.getLatitude() != 0.0 && lastHz.getLongitude() != 0) {
            	this.lastHz = lastHz;
            	return lastHz;
            }
		}
		return lastHz;
	}
	@Override
    public synchronized boolean setObjectCoordinates(LocationElement loc, String name) {
		objLoc = new LocationElement(Functions.normalizeRadians(loc.getLongitude()), loc.getLatitude(), 1);
		object = name;
		return true;
     }
	@Override
	public String getObjectName() {
		return object;
	}
	@Override
    public synchronized LocationElement getObjectCoordinates() {
		if (objLoc == null) return null;
		return objLoc.clone();
    }

	@Override
    public synchronized boolean gotoObject() {
		if (objLoc == null) return false;

		LocationElement loc = objLoc.clone();
		if (this.telescopeModel.isJ2000()) {
			try {
				if (loc.getLongitude() != 0 || loc.getLatitude() != Constant.PI_OVER_TWO) {
					EphemerisElement eph = new EphemerisElement(TARGET.NOT_A_PLANET, EphemerisElement.COORDINATES_TYPE.APPARENT,
							EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.TOPOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
							EphemerisElement.FRAME.DYNAMICAL_EQUINOX_J2000,
							EphemerisElement.ALGORITHM.MOSHIER);
					eph.preferPrecisionInEphemerides = false;
					eph.correctForEOP = false;
					eph.correctForPolarMotion = false;
					TimeElement time = getTime();
					if (obs == null) getObserver();
					loc = Ephem.toMeanEquatorialJ2000(loc, time, obs, eph);
				}
			} catch (Exception exc) {
				exc.printStackTrace();
			}
		}

        String raStr = encodeHexadecimal(loc.getLongitude());
        String decStr = encodeHexadecimal(loc.getLatitude());

        String cmdStr = "";
        if (telescopeModel == TELESCOPE_MODEL.CELESTRON_NEXSTAR_5_8) {
                cmdStr = "R" + raStr + decStr;
        } else if (telescopeModel == TELESCOPE_MODEL.CELESTRON_NEXSTAR_GT_ORIGINAL) {
                cmdStr = "R" + raStr + "X" + decStr + "X";
                cmdStr = cmdStr.replace('X', NULL_CHAR);
        } else if (telescopeModel == TELESCOPE_MODEL.CELESTRON_NEXSTAR_GPS || telescopeModel == TELESCOPE_MODEL.CELESTRON_NEXSTAR_5I_8I ||
        		telescopeModel == TELESCOPE_MODEL.CELESTRON_NEXSTAR_GT || telescopeModel == TELESCOPE_MODEL.CELESTRON_ASC ||
        		telescopeModel == TELESCOPE_MODEL.CELESTRON_CGE || telescopeModel == TELESCOPE_MODEL.SYNSCAN) {
            if (highPrecision) {
                cmdStr = "r" + raStr + "," + decStr;
            } else {
                cmdStr = "R" + raStr + "," + decStr;
            }
        }

/*        final int raBytes = raToInt(Functions.getHMS(loc.getLongitude()));
        final int decBytes = decToInt(Functions.getDMSs(loc.getLatitude()));
		String c = "R";
		c += (byte)(raBytes>>8);
		c += (byte)(raBytes);
		c += ",";
		c += (byte)(decBytes>>8);
		c += (byte)(decBytes);
*/
        sendCmdAndReceiveResponse(cmdStr);
        return true;
        //boolean out = sendCmd(cmdStr);
		//if (out) readString();
		//return out;
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
		String o = sendCmdAndReceiveResponse("L");
    	if (!o.equals("")) {
    		return readString().equals("1");
    	}

    	LocationElement pos = this.getEquatorialPosition();
    	int wait = Math.max(SerialConnection.MAX_TIME_WAIT_FOR_RESPONSE_MS, (int) seconds*1000) + 100;
        try {
        	Thread.sleep(wait);
        } catch(InterruptedException e) {}
        return this.distanceToPosition(pos, true) > tolerance;
    }
	@Override
    public synchronized double getLocalTime() {
		String o = this.sendCmdAndReceiveResponse("h");
    	if (!o.equals("")) {
    		String time = readString();
    		int hr = time.charAt(0), mi = time.charAt(1), se = time.charAt(2);
    		return hr + mi / 60.0 + se / 3600.0;
    	}
    	return -1;
    }
	@Override
    public synchronized boolean setLocalTime(TimeElement time0, ObserverElement obs) {
		TimeElement time = time0;
		if (time.timeScale != SCALE.LOCAL_TIME && time.timeScale != SCALE.UNIVERSAL_TIME_UTC) {
			try {
				EphemerisElement eph = new EphemerisElement();
				eph.optimizeForSpeed();
				double local = TimeScale.getJD(time0, obs, eph, SCALE.LOCAL_TIME);
				time = new TimeElement(local, SCALE.LOCAL_TIME);
			} catch (Exception exc) {}
		}
		
        char hr = (char) ((int) time.astroDate.getHour());
        char mi = (char) ((int) time.astroDate.getMinute());
        char se = (char) ((int) time.astroDate.getRoundedSecond());

        char m = (char) (time.astroDate.getMonth());
        char d = (char) (time.astroDate.getDay());
        char y = (char) (time.astroDate.getYear() - 2000);

        int tz = (int)obs.getTimeZone(), dst = 0;
        if (time.timeScale == SCALE.UNIVERSAL_TIME_UTC) tz = 0;
		if (tz < 0) tz = 256 - tz;

		if (time.timeScale == SCALE.LOCAL_TIME) {
	        try {
				EphemerisElement eph = new EphemerisElement(TARGET.NOT_A_PLANET, EphemerisElement.COORDINATES_TYPE.APPARENT,
						EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.TOPOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
						EphemerisElement.FRAME.DYNAMICAL_EQUINOX_J2000,
						EphemerisElement.ALGORITHM.MOSHIER);
				eph.preferPrecisionInEphemerides = false;
				eph.correctForEOP = false;
				eph.correctForPolarMotion = false;
		        double JD_UT = TimeScale.getJD(time, obs, eph, SCALE.UNIVERSAL_TIME_UTC);
		        dst = (int) TimeScale.getDST(JD_UT, obs);
	        } catch (Exception exc) {
	        	exc.printStackTrace();
	        }
		}

        String cmd = "H"+hr+mi+se+m+d+y+(char)tz+(char)dst;
        sendCmdAndReceiveResponse(cmd);
        return true;
        
        //boolean out = sendCmd(cmd);
    	//if (out) readString();
    	//return out;
    }
	
	@Override
	public boolean setObserver(ObserverElement obs) {
		try {
			double lon = obs.getLongitudeDeg(), lat = obs.getLatitudeDeg();
			int D = lat >= 0 ? 0 : 1;
			int H = lon >= 0 ? 0 : 1;
			lon = Math.abs(lon);
			lat = Math.abs(lat);
	
			int dec1 = (int) lat;
			lat = (lat - dec1) * 60.0;
			int dec2 = (int) lat;
			lat = (lat - dec2) * 60.0;
			int dec3 = (int) lat;
			
			int A = dec1;
			int B = dec2;
			int C = dec3;
	
			dec1 = (int) lon;
			lon = (lon - dec1) * 60.0;
			dec2 = (int) lon;
			lon = (lon - dec2) * 60.0;
			dec3 = (int) lon;
	
			int E = dec1;
			int F = dec2;
			int G = dec3;
			
			char cA = (char) A;
			char cB = (char) B;
			char cC = (char) C;
			char cD = (char) D;
			char cE = (char) E;
			char cF = (char) F;
			char cG = (char) G;
			char cH = (char) H;
			String com = "W" + cA + cB + cC + cD + cE + cF + cG + cH;
			sendCmdAndReceiveResponse(com);
			return true;
		} catch (Exception exc) {
			exc.printStackTrace();
		}
		return false;
	}
	
	@Override
    public synchronized boolean sync() {
		if (objLoc == null) return false;

		LocationElement loc = objLoc.clone();
		if (this.telescopeModel.isJ2000()) {
			try {
				EphemerisElement eph = new EphemerisElement(TARGET.NOT_A_PLANET, EphemerisElement.COORDINATES_TYPE.APPARENT,
						EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.TOPOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
						EphemerisElement.FRAME.DYNAMICAL_EQUINOX_J2000,
						EphemerisElement.ALGORITHM.MOSHIER);
				eph.preferPrecisionInEphemerides = false;
				eph.correctForEOP = false;
				eph.correctForPolarMotion = false;
				TimeElement time = getTime();
				if (obs == null) getObserver();
				loc = Ephem.toMeanEquatorialJ2000(loc, time, obs, eph);
			} catch (Exception exc) {
				exc.printStackTrace();
			}
		}

        String raStr = encodeHexadecimal(loc.getLongitude());
        String decStr = encodeHexadecimal(loc.getLatitude());
		String cmd = "s";
		if (!highPrecision) cmd = "S";

        String cmdStr = cmd;
        if (telescopeModel == TELESCOPE_MODEL.CELESTRON_NEXSTAR_5_8) {
        	cmdStr += raStr + decStr;
        } else if (telescopeModel == TELESCOPE_MODEL.CELESTRON_NEXSTAR_GT_ORIGINAL) {
        	cmdStr += raStr + "X" + decStr + "X";
        	cmdStr = cmdStr.replace('X', NULL_CHAR);
        } else if (telescopeModel == TELESCOPE_MODEL.CELESTRON_NEXSTAR_GPS || telescopeModel == TELESCOPE_MODEL.CELESTRON_NEXSTAR_5I_8I ||
        		telescopeModel == TELESCOPE_MODEL.CELESTRON_NEXSTAR_GT || telescopeModel == TELESCOPE_MODEL.CELESTRON_ASC ||
        		telescopeModel == TELESCOPE_MODEL.CELESTRON_CGE || telescopeModel == TELESCOPE_MODEL.SYNSCAN) {
        	cmdStr += raStr + "," + decStr;
        }

		this.sendCmdAndReceiveResponse(cmdStr);
		return true;
    }
	@Override
    public synchronized boolean reset(LocationElement loc) {
		double lon24 = (Functions.normalizeRadians(loc.getLongitude()) / Constant.TWO_PI) * FastMath.pow(2, 24);
		double lat24 = (loc.getLatitude() / Constant.TWO_PI) * FastMath.pow(2, 24);
		
		int lon1 = (int) (lon24 / (256 * 256));
		lon24 -= lon1 * 256 * 256;
		int lon2 = (int) (lon24 / 256);
		lon24 -= lon2 * 256;
		int lon3 = (int) lon24;

		int lat1 = (int) (lat24 / (256 * 256));
		lat24 -= lat1 * 256 * 256;
		int lat2 = (int) (lat24 / 256);
		lat24 -= lat2 * 256;
		int lat3 = (int) lat24;

		char c4 = 4, c16 = 16, c17 = 17, c0 = 0;
		char clon1 = (char) lon1, clon2 = (char) lon2, clon3 = (char) lon3;
		char clat1 = (char) lat1, clat2 = (char) lat2, clat3 = (char) lat3;
		sendCmdAndReceiveResponse("P" + c4 + c16 + c4 + clon1 + clon2 + clon3 + c0);
		sendCmdAndReceiveResponse("P" + c4 + c17 + c4 + clat1 + clat2 + clat3 + c0);
		return true;
	}
	
	@Override
    public synchronized boolean stopMoving() {
		sendCmdAndReceiveResponse("M");
		isMoving = false;
		isMovingE = isMovingN = isMovingW = isMovingS = false;
		return true;
		/*
    	if (sendCmd("M")) {
    		readString();
    		isMoving = false;
    		isMovingE = isMovingN = isMovingW = isMovingS = false;
    		return true;
    	}
    	return false;
    	*/
    }
	@Override
    public synchronized boolean park() {
		//if (parkPos == null) return false;
		try {
			TimeElement time = getTime();
			EphemerisElement eph = new EphemerisElement(TARGET.NOT_A_PLANET, EphemerisElement.COORDINATES_TYPE.APPARENT,
					EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.TOPOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
					EphemerisElement.FRAME.DYNAMICAL_EQUINOX_J2000,
					EphemerisElement.ALGORITHM.MOSHIER);
			eph.preferPrecisionInEphemerides = false;
			eph.correctForEOP = false;
			eph.correctForPolarMotion = false;
			LocationElement loc;
			LocationElement loc0 = null;
			if (objLoc != null) loc0 = objLoc.clone();
			if (obs == null) getObserver();
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
			// TODO: disable tracking
			return true;
		} catch (JPARSECException e) {
			return false;
		}
    }
	@Override
	public LocationElement getParkPosition() {
		if (parkPos != null) return parkPos;
		try {
			LocationElement loc = new LocationElement(0, Constant.PI_OVER_TWO, 1);
			if (obs == null) getObserver();
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
		//if (parkPos == null) return false;
		try {
			connect();
			// TODO: enable tracking
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
    public synchronized String getTelescopeName() {
		if (telName == null) telName = this.sendCmdAndReceiveResponse("m");
		int model = telName.charAt(0);
		
		String models[] = new String[] {
				"EQ6", "HEQ5", "EQ5", "EQ3", "EQ8", "AZ_EQ6", "AZ_EQ5", // 0-6
				"AZ_GOTO", "DOB_GOTO", "ALLVIEW_GOTO", "UNKNOWN" // 7=128-143, 8=144-159, 9=160, 10=any other value
		};
		
		if (model >= 0 && model <= 6) return models[model];

		if (model >= 128 && model <= 143) return models[7];
		if (model >= 144 && model <= 159) return models[8];
		if (model == 160) return models[9];
		return "SkyWatcher " + models[10];
    }
	@Override
    public synchronized String getTelescopePort() {
    	return sc.getPortName();
    }
	@Override
    public synchronized ObserverElement getObserver() {
		if (sc.getTimeSinceLastCommand() < SerialConnection.MAX_TIME_WAIT_FOR_RESPONSE_MS && obs != null) 
			return obs;

		String o = this.sendCmdAndReceiveResponse("w");
		if (o == null || o.equals("")) return new ObserverElement();
		int latd = o.charAt(0), latm = o.charAt(1), lats = o.charAt(2), lath = o.charAt(3);
		int lond = o.charAt(4), lonm = o.charAt(5), lons = o.charAt(6), lonh = o.charAt(7);
		double lon = lond + lonm / 60.0 +lons / 3600.0;
		if (lonh == 1) lon = -lon;
		double lat = latd + latm / 60.0 +lats / 3600.0;
		if (lath == 1) lat = -lat;

		String time = this.sendCmdAndReceiveResponse("h");
		if (time.equals("")) return new ObserverElement();
		int localToUTC = time.charAt(6);
		if (localToUTC > 128) localToUTC = 256 - localToUTC;
		// FIXME: localToUTC only integer, not sure about the sign when setting observer
		ObserverElement observer = new ObserverElement(getTelescopeName(), lon * Constant.DEG_TO_RAD, lat * Constant.DEG_TO_RAD, 0, localToUTC);
		obs = observer;
		return observer;
    }
	@Override
    public synchronized TimeElement getTime() {
		if (sc.getTimeSinceLastCommand() < SerialConnection.MAX_TIME_WAIT_FOR_RESPONSE_MS && time0 != null) {
    		TimeElement time = new TimeElement();
    		if (time0.timeScale == SCALE.UNIVERSAL_TIME_UTC) {
    			try {
					time = new TimeElement(new AstroDate(System.currentTimeMillis()), SCALE.UNIVERSAL_TIME_UTC);
				} catch (Exception e) {	}
    		}
    		try { time.add(timeOffset); } catch (Exception exc) { exc.printStackTrace(); }
    		return time;
		}

		try {
    		String time = this.sendCmdAndReceiveResponse("h");
    		int hr = time.charAt(0), mi = time.charAt(1), se = time.charAt(2);
    		int m = time.charAt(3), d = time.charAt(4), y = 2000 + time.charAt(5);
            int tz = (int) time.charAt(6);
            int dst = (int) time.charAt(7);
    		double hours = hr + mi / 60.0 + se / 3600.0;
    		AstroDate astro = new AstroDate(y, m, d + hours / 24.0);
    		TimeElement t = new TimeElement(astro, SCALE.LOCAL_TIME);
    		
    		time0 = new TimeElement();
    		if (tz == 0 && dst == 0) {
    			t.timeScale = SCALE.UNIVERSAL_TIME_UTC;
    			time0 = new TimeElement(new AstroDate(System.currentTimeMillis()), SCALE.UNIVERSAL_TIME_UTC);
    		}
    		
    		timeOffset = astro.jd() - time0.astroDate.jd();
    		return t;
		} catch (Exception exc) { }
		return new TimeElement();
    }
	@Override
    public synchronized boolean isTracking() {
		//if (noGW || this.telescopeModel == TELESCOPE_MODEL.CELESTRON_NEXSTAR_5_8) return true;
		String o = sendCmdAndReceiveResponse("t");
		if (o == null) return false;
		if (o.length() > 0 && (int) o.charAt(0) == 0) return false;
		return true;
		/*
    	if (sendCmd("t")) {
    		String o = this.readString();
    		//if (o.equals("")) noGW = true;
    		if (o.length() > 0 && o.charAt(0) == '0') return false;
    		return true;
    	}
    	return false;
    	*/
    }
	@Override
    public synchronized boolean isAligned() {
		//if (noGW || this.telescopeModel == TELESCOPE_MODEL.CELESTRON_NEXSTAR_5_8) return true;

		String o = sendCmdAndReceiveResponse("J");
		if (o == null) return false;
		if (o.length() > 0 && (int) o.charAt(0) == 1) return true;
		/*
    	if (sendCmd("J")) {
    		String o = this.readString();
    		//if (o.equals("")) noGW = true;
    		if (o.length() > 0 && o.charAt(0) == '1') return true;
    	}
    	*/
    	return false;
    }
	@Override
    public synchronized MOUNT getMount() {
		if (trackingMode > 0) {
			if (trackingMode == 1) return MOUNT.AZIMUTHAL;
			return MOUNT.EQUATORIAL;
		}
		return null;
    }
	@Override
    public synchronized boolean disconnect() {
		sc.closeConnection();
    	return true;
    }
	@Override
    public synchronized boolean connect() throws JPARSECException {
		sc.openConnection();
    	return true;
    }
	@Override
    public synchronized boolean isConnected() {
		return sc.isOpen();
    }

	/**    // Convert RA from a string to a number.
	private static int raToInt(double hms[]) {
		return (int)hms[0]*600+(int)hms[1]*10+(int)hms[2];
    }
    // Convert dec from a string to a number.
    private static int decToInt(double dms[]) {
    	return (int) (dms[3]*((int)dms[0]*600+(int)dms[1]*10+(int)dms[2]));
    }
*/
    /**
     * Encode angle as hexadecimal value
     * @param val Angle to be encoded
     * @return Hexadecimal string for encode precision
     */
    private String encodeHexadecimal(double val) {
        String str = "";

        if (highPrecision) {
            str = encodeHexadecimal24(val);
        } else {
            str = encodeHexadecimal16(val);
        }

        return str;
    }


    /** Encode as 16 bit hexadecimal value
     * @param val Value to be converted
     * @return String containing coordinate encoded as a hexadecimal
     */
    private String encodeHexadecimal16(double val) {
        String str = "";

        if (!Double.isNaN(val)) {

            long val1 = Math.round(Functions.normalizeDegrees(val * Constant.RAD_TO_DEG) * DEGREES_TO_COUNT_16);

            // Convert val1 to hex
            str = Long.toHexString(val1);

            // Pad str with zeros
            if (str.length() < 4) {
                str = STR0.substring(0, 4 - str.length()) + str;
            }
        }

        return str.toUpperCase();
    }
    /** Encode as 24 bit hexadecimal value
     * @param val Value to be converted
     * @return String containing coordinate encoded as a hexadecimal
     */
    private String encodeHexadecimal24(double val) {
        String str = "";

        if (!Double.isNaN(val)) {
            long val1 = Math.round(Functions.normalizeDegrees(val * Constant.RAD_TO_DEG) * DEGREES_TO_COUNT_24);

            // Convert val1 to hex
            str = Long.toHexString(val1);

            // Pad str1 with zeros
            if (str.length() < 6) {
                str = STR0.substring(0, 6 - str.length()) + str;
            }
            str = str + "00";
        }

        return str.toUpperCase();
    }

    /**
     * Decode hexadecimal format which does not contain comma or null separator
     * @param str String containing the coordinate
     */
    private LocationElement decodeHexadecimalFormat1(String str) {
        String str1 = str.substring(0, 4);
        String str2 = str.substring(4, 8);
        long val1 = Long.parseLong(str1, 16);
        long val2 = Long.parseLong(str2, 16);
        return new LocationElement((double) val1 * COUNT_TO_DEGREES_16 * Constant.DEG_TO_RAD,
        		(double) val2 * COUNT_TO_DEGREES_16 * Constant.DEG_TO_RAD, 1.0);
    }

    /**
     * Decode hexadecimal format which contains comma or null separator
     * @param str String containing the coordinate
     */
    private LocationElement decodeHexadecimalFormat2(String str) {
        String str1 = "";
        String str2 = "";
        long val1 = 0L;
        long val2 = 0L;

        if (highPrecision) {
        	if (str.length() < 15) return null;
            str1 = str.substring(0, 6);
            str2 = str.substring(9, 15);
            val1 = Long.parseLong(str1, 16);
            val2 = Long.parseLong(str2, 16);
            return new LocationElement((double) val1 * COUNT_TO_DEGREES_24 * Constant.DEG_TO_RAD,
            		(double) val2 * COUNT_TO_DEGREES_24 * Constant.DEG_TO_RAD, 1.0);
        } else {
        	if (str.length() < 9) return null;
            str1 = str.substring(0, 4);
            str2 = str.substring(5, 9);
            val1 = Long.parseLong(str1, 16);
            val2 = Long.parseLong(str2, 16);
            return new LocationElement((double) val1 * COUNT_TO_DEGREES_16 * Constant.DEG_TO_RAD,
            		(double) val2 * COUNT_TO_DEGREES_16 * Constant.DEG_TO_RAD, 1.0);
        }
    }

    /**
     * Decode hexadecimal string
     * @param str String containing coordinate from telescope
     * @param coord Point2D coordinate to be set
     */
    private LocationElement decodeHexadecimal(String str) {
        if ((str != null) && (str.length() >= 8)) {
            if (telescopeModel == TELESCOPE_MODEL.CELESTRON_CGE || telescopeModel == TELESCOPE_MODEL.CELESTRON_NEXSTAR_GPS ||
            		telescopeModel == TELESCOPE_MODEL.CELESTRON_NEXSTAR_GT || telescopeModel == TELESCOPE_MODEL.CELESTRON_NEXSTAR_GT_ORIGINAL ||
            		telescopeModel == TELESCOPE_MODEL.CELESTRON_ASC || telescopeModel == TELESCOPE_MODEL.CELESTRON_NEXSTAR_5I_8I || 
            		telescopeModel == TELESCOPE_MODEL.SYNSCAN) {
                return decodeHexadecimalFormat2(str);
            } else if (telescopeModel == TELESCOPE_MODEL.CELESTRON_NEXSTAR_5_8) {
                return decodeHexadecimalFormat1(str);
            }
        }
        return null;
    }

    /**
    * Sets high precision.
    */
	private synchronized void setHighPrecision() {
        if (telescopeModel == TELESCOPE_MODEL.CELESTRON_NEXSTAR_5_8 ||
        		telescopeModel == TELESCOPE_MODEL.CELESTRON_NEXSTAR_GT_ORIGINAL ||
        		telescopeModel == TELESCOPE_MODEL.CELESTRON_NEXSTAR_GT) {
        	highPrecision = false;
        } else {
        	highPrecision = true;
        }
    }

	@Override
	public synchronized boolean setTrackingActive(boolean b) {
		int mode = 0;
		if (b) {
			mode = 1;
			if (getMount() == MOUNT.EQUATORIAL) mode = 2;
		}
		sendCmdAndReceiveResponse("T" + (char) mode);
		trackingMode = mode;
		return true;
	}

	private synchronized void getTrackingMode() {
		String o = sendCmdAndReceiveResponse("t");
		int t = o.charAt(0);
		trackingMode = t;
		/*
		if (sendCmd("t")) {
			int t = readString().charAt(0);
			trackingMode = t;
		}
		*/
	}

    private boolean checkMove() {
    	if (isMovingN || isMovingE || isMovingW || isMovingS) return true;
		return this.isMoving(1, MOVE_TOLERANCE_1s);
    }

    /**
    * Sends a command to the scope.
    */
    /*
    private synchronized boolean sendCmd(String cmd) {
    	try {
    		if (!sc.isOpen()) return false;
            sc.sendString(cmd);
            return true;
    	} catch (Exception exc) {
    		exc.printStackTrace();
    		return false;
    	}
    }
    */
    /**
    * Sends a command to the scope and returns a response.
    */
    private synchronized String sendCmdAndReceiveResponse(String cmd) {
    	try {
    		if (!sc.isOpen()) return null;

    		sc.sendString(cmd);

            return this.readString();
    	} catch (Exception exc) {
    		exc.printStackTrace();
    		return null;
    	}
    }

    /**
    * Reads a string from the scope, dropping the terminating #.
    */
    private synchronized String readString() {
		if (!sc.isOpen()) return "";

		String s = sc.receiveString();
    	if (s == null) {
    		nullResp ++;
    		if (nullResp > 2) disconnect();
    		return "";
    	}
    	nullResp = 0;
    	if (s.endsWith("#")) s = s.substring(0, s.length()-1);
    	return s;
    }

    @Override
    public TELESCOPE_MODEL getTelescopeModel() {
    	return telescopeModel;
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
	public HeaderElement[] getFitsHeader(int cameraIndex) {
		try {
			TimeElement time = getTime();
			ObserverElement obs = this.getObserver();
			EphemerisElement eph = new EphemerisElement(TARGET.NOT_A_PLANET, EphemerisElement.COORDINATES_TYPE.APPARENT,
					EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.TOPOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
					EphemerisElement.FRAME.DYNAMICAL_EQUINOX_J2000,
					EphemerisElement.ALGORITHM.MOSHIER);
			eph.preferPrecisionInEphemerides = false;
			eph.correctForEOP = false;
			eph.correctForPolarMotion = false;

			double jd = TimeScale.getJD(time, obs, eph, SCALE.UNIVERSAL_TIME_UT1);
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
					new HeaderElement("BZERO", ""+FastMath.multiplyBy2ToTheX(1, 31), "(minus) data zero value"),
					new HeaderElement("DATE-OBS", ""+getTime().toString(), "Date and time, usually in LT"),
					new HeaderElement("TIME_JD", ""+jd, "Date and time as JD, in UT1"),
					new HeaderElement("OBS_LON", ""+obs.getLongitudeDeg(), "Longitude in deg, west negative"),
					new HeaderElement("OBS_LAT", ""+obs.getLatitudeDeg(), "Latitude in deg, south negative"),
					new HeaderElement("OBS_NAME", obs.getName(), "Observer name"),
					new HeaderElement("OBS_TZ", ""+obs.getTimeZone(), "Time zone"),
					new HeaderElement("OBS_DST", obs.getDSTCode().name(), "DST code"),
					new HeaderElement("TEL_MODEL", this.telescopeModel.name(), "Telescope model (driver)"),
					new HeaderElement("TEL_TYPE", this.type.name(), "Telescope type (S/C, refractor, ...)"),
					new HeaderElement("TELESCOP", this.getTelescopeName(), "Telescope name"),
					new HeaderElement("MOUNT", this.getMount().name(), "Telescope mount"),
					new HeaderElement("CONNECTED", ""+this.isConnected(), "Telescope connected ?"),
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
					double timeExp = camera.getCCDorBulbModeTime();
					if (!camera.isBulb()) timeExp = Evaluation.evaluate(camera.getExpositionTime(), null);
					time = new TimeElement(new AstroDate(camera.getLastShotStartTime()), SCALE.UNIVERSAL_TIME_UTC);
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
			cameraHeader = HeaderElement.addHeaderEntry(cameraHeader, new HeaderElement("CAM_INDEX", ""+cameraIndex, "Camera index id value"));

			return HeaderElement.addHeaderEntry(header0, cameraHeader);
		} catch (Exception exc) {
			exc.printStackTrace();
			return null;
		}
	}

    /**
     * Constructor for a Celestron telescope.
     * @param model The telescope model, must be a Celestron one.
     * @param port The port to use, COMx in Windows and /dev/tty... in Linux.
     * Set to null to scan for all possible ports and launch a window to select
     * in case there's more than 1.
     * @throws JPARSECException If no serial ports are available.
     */
	public SynscanTelescope(TELESCOPE_MODEL model, String port) throws JPARSECException {
		if (!model.isSynscan()) throw new JPARSECException("Telescope must be a Synscan one!");
		telescopeModel = model;
		String ports[] = SerialConnection.getAvailablePorts();
		if (ports.length == 0) throw new JPARSECException("No serial ports available!");
		if (port == null) {
			if (ports.length == 1) {
				port = ports[0];
			} else {
				for (int i=0; i<ports.length; i++) {
			        sc = new SerialConnection();
			        sc.setPortName(ports[i]);
			        sc.openConnection();

			        String name = this.getTelescopeName();
			        if (name != null && !name.equals("")) {
			        	port = ports[i];
			        	sc.closeConnection();
			        	break;
			        }
			        sc.closeConnection();
				}
				if (port == null) {
					int s = JOptionPane.showOptionDialog(null,
							Translate.translate(1126),
							Translate.translate(1125), JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null, ports, ports[0]);
					if (s >= 0) port = ports[s];
				}
			}
		}
		if (port == null) throw new JPARSECException("No serial ports selected/available!");

        sc = new SerialConnection();
        sc.setPortName(port);
        sc.openConnection();
       	setHighPrecision();
       	setFocusSpeed(fs);
       	setMoveSpeed(ms);
       	getTrackingMode();
	}

	@Override
	public boolean hasGOTO() {
		return true;
	}
}
