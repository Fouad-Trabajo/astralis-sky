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
import jparsec.math.Constant;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.TimeElement;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;

/**
 * An interface for the commands that should be available to all telescopes.
 * @author T. Alonso Albi - OAN (Spain)
 */
public interface GenericTelescope {

	/**
	 * Focus speed values.
	 */
	public enum FOCUS_SPEED {
		FAST(1),
		SLOW(2);

		private int val;
		private FOCUS_SPEED(int x) {
			val = x;
		}

		/** Returns the value for this constant.
		 * @return ID constant for the value. */
		public int getValue() { return val; }
	};

	/**
	 * Focus direction values.
	 */
	public enum FOCUS_DIRECTION {
		IN(1),
		OUT(2);

		private int val;
		private FOCUS_DIRECTION(int x) {
			val = x;
		}

		/** Returns the value for this constant.
		 * @return ID constant for the enum value. */
		public int getValue() { return val; }
	};

	/**
	 * Slew rates values.
	 */
	public enum MOVE_SPEED {
		VERY_FAST,
		FAST,
		QUICK,
		SLOW
	};

	/**
	 * Slew direction values, in equatorial or azimuthal
	 * depending on how the scope is mounted.
	 */
	public enum MOVE_DIRECTION {
		NORTH_UP(1),
		EAST_LEFT(2),
		SOUTH_DOWN(3),
		WEST_RIGHT(4);

		private int val;
		private MOVE_DIRECTION(int x) {
			val = x;
		}

		/** Returns the value for this constant.
		 * @return ID constant for the value. */
		public int getValue() { return val; }
	};

	/**
	 * Mount types.
	 */
	public enum MOUNT {
		AZIMUTHAL,
		EQUATORIAL
	};

	/**
	 * The set of telescope types.
	 */
	public enum TELESCOPE_TYPE {
		SCHMIDT_CASSEGRAIN(true, false),
		NEWTON(true, true),
		REFRACTOR(true, true),
		REFRACTOR_WITH_ERECTING_PRISM(false, false),
		/** Hypothetical telescope that only inverts vertically. Like an
		 * Schmidt-Cassegrain rotated 90&deg;. */
		SC_VERTICALLY_INVERTED(false, true);

		private boolean ih, iv;
		private TELESCOPE_TYPE(boolean ih, boolean iv) {
			this.ih = ih;
			this.iv = iv;
		}
		/**
		 * Returns if this telescope inverts the image horizontally.
		 * @return True or false.
		 */
		public boolean invertH() {
			return ih;
		}
		/**
		 * Returns if this telescope inverts the image vertically.
		 * @return True or false.
		 */
		public boolean invertV() {
			return iv;
		}
	};

	/**
	 * The list of telescope types available.
	 */
	public static final String[] TELESCOPE_TYPES = new String[] {
		"Schmidt-Cassegrain", "Newton", Translate.translate(67), Translate.translate(69)
	};

	/**
	 * The set of telescope models supported.
	 */
	public enum TELESCOPE_MODEL {
		/** AutoStar I model series, for ETX, DS, LX90, LXD, and LT models. */
		MEADE_AUTOSTAR(false),
		/** AutoStar II model series, LX200, LX400, and others, with GPS. */
		MEADE_AUTOSTAR_II(true),
		/** Specific model for LX200 telescopes with aperture of 14 inches or lower,
		 * and without GPS. */
		MEADE_LX200(false),
		/** Specific model for 16 inch LX200 telescope without GPS. */
		MEADE_LX200_16inch(false),
		/** Celestron Nextar 5/8. */
		CELESTRON_NEXSTAR_5_8 (false),
		/** Celestron Nextar 5I/8I. */
		CELESTRON_NEXSTAR_5I_8I (false),
		/** Celestron Nextar GT original. */
		CELESTRON_NEXSTAR_GT_ORIGINAL(false),
		/** Celestron Nextar GT. */
		CELESTRON_NEXSTAR_GT(false),
		/** Celestron Nextar GPS. */
		CELESTRON_NEXSTAR_GPS(true),
		/** Celestron Advanced Series. */
		CELESTRON_ASC(false),
		/** Celestron CGE. */
		CELESTRON_CGE(false),
		/** Genreic Synscan protocol. */
		SYNSCAN(false),
		/** A virtual telescope for testing, with an equatorial mount. */
		VIRTUAL_TELESCOPE_EQUATORIAL_MOUNT(false),
		/** A virtual telescope for testing, with an horizontal mount. */
		VIRTUAL_TELESCOPE_AZIMUTHAL_MOUNT(false),
		/** An external telescope to just show a mark in sky rendering. */
		EXTERNAL_TELESCOPE_JUST_MARK(false);

		private boolean isMeade = false;
		private boolean isCelestron = false;
		private boolean isSynscan = false;
		private boolean hasGPS = true;
		private boolean J2000 = false;

		private TELESCOPE_MODEL(boolean hasGPS) {
			this.hasGPS = hasGPS;
			isMeade = false;
			isCelestron = false;
			isSynscan = false;
			if (this.name().startsWith("MEADE")) isMeade = true;
			if (this.name().startsWith("CELESTRON")) isCelestron = true;
			if (this.name().startsWith("SYNSCAN")) isSynscan = true;

			J2000 = false;

			// TODO: Check this for Celestron, Meade ok !!!
			if (isMeade || isCelestron || isSynscan) J2000 = true;
		}

		/**
		 * Returns if this telescope uses mean J2000 coordinates as input/output
		 * to command position. False means it uses apparent coordinates.
		 * @return True or false.
		 */
		public boolean isJ2000() {
			return J2000;
		}

		/**
		 * Returns if this telescope has GPS or not.
		 * @return True or false.
		 */
		public boolean hasGPS() {
			return hasGPS;
		}

		/**
		 * Sets if this telescope has GPS or not.
		 * @param hasGPS True or false.
		 */
		public void setHasGPS(boolean hasGPS) {
			this.hasGPS = hasGPS;
		}

		/**
		 * Returns if this telescope is a Meade model or not.
		 * @return True or false.
		 */
		public boolean isMeade() {
			return isMeade;
		}

		/**
		 * Returns if this telescope is a Celestron model or not.
		 * @return True or false.
		 */
		public boolean isCelestron() {
			return isCelestron;
		}
		
		/**
		 * Returns if this telescope is a synscan model or not.
		 * @return True or false.
		 */
		public boolean isSynscan() {
			return isSynscan;
		}

		/**
		 * Returns if this telescope if a virtual telescope or not.
		 * @return True or false.
		 */
		public boolean isVirtual() {
			if (this.name().startsWith("VIRTUAL_")) return true;
			return false;
		}

		/**
		 * Clears this enum in case the has GPS method was called.
		 */
		public void clear() {
			hasGPS = false;
			if (this == MEADE_AUTOSTAR_II) hasGPS = true;
		}

		/**
		 * Returns the approximate error in radians expected in this telescope model
		 * when commanding a given position.
		 * @return The error angle in radians.
		 */
		public double getExpectedErrorWhenCenteringObjects() {
			double error = 0;

			// exact => error = 0
			if (!isVirtual()) {
				// 1 deg
				if (this == CELESTRON_NEXSTAR_5_8 || this == CELESTRON_NEXSTAR_5I_8I ||
						this == MEADE_AUTOSTAR) {
					error = Constant.DEG_TO_RAD;
				} else {
				// 5'
					error = Constant.DEG_TO_RAD * 5.0 / 60.0;
				}
			}

			return error;
		}
	}

	/**
	 * Returns if this telescope has goto or not.
	 * @return True or false.
	 */
	public boolean hasGOTO();

	/**
	 * Checks if this telescope has GPS active or not. GPS is
	 * only available is AutoStar II models and Celestron GPS.
	 * @return True or false.
	 */
	public boolean hasGPS(); // AutoStar II
	/**
	 * Checks if this telescope has a focuser.
	 * Only for Max/RCX models in Meade.
	 * @return True or false.
	 */
	public boolean hasFocuser(); // Max/RCX
	/**
	 * Checks if this telescope is moving or not in a fast way,
	 * without sending commands to the telescope. Tracking and goto are not
	 * considered as movement here, only if the move command is
	 * called.
	 * @return True or false.
	 */
	public boolean isMoving();
	/**
	 * Checks if the telescope is tracking an object or not.
	 * @return True or false. In case the command fails, false is returned.
	 */
	public boolean isTracking();
	/**
	 * Checks if the telescope is aligned or not.
	 * @return True or false. In case the command fails, false is returned.
	 */
	public boolean isAligned();
	/**
	 * Returns the mount type, azimuthal or equatorial. Partially supported in
	 * Celestron, returning null unless tracking is active in az/el or RA/DEC
	 * when the telescope is connected.
	 * @return Mount type. In case the command fails, null is returned.
	 */
	public MOUNT getMount();
	/**
	 * Sets the focus speed. Focus is unsupported in Celestron.
	 * @param fs Focus speed.
	 * @return True if the command success, false otherwise.
	 */
	public boolean setFocusSpeed(FOCUS_SPEED fs);
	/**
	 * Returns the current focus speed. Focus is unsupported in Celestron.
	 * @return Focus speed.
	 */
	public FOCUS_SPEED getFocusSpeed();
	/**
	 * Starts focusing in or out. Focus is unsupported in Celestron.
	 * @param fd Focus direction in/out.
	 * @return True if the command success, false otherwise.
	 */
	public boolean startFocus(FOCUS_DIRECTION fd);
	/**
	 * Stops the focusing. Focus is unsupported in Celestron.
	 * @return True if the command success, false otherwise.
	 */
	public boolean stopFocus();
	/**
	 * Returns the current focus direction. Focus is unsupported in Celestron.
	 * @return Focus direction.
	 */
	public FOCUS_DIRECTION getFocusDirection();
	/**
	 * Sets the speed of movement.
	 * @param ms Speed.
	 * @return True if the command success, false otherwise.
	 */
	public boolean setMoveSpeed(MOVE_SPEED ms);
	/**
	 * Returns the speed of movement.
	 * @return Speed.
	 */
	public MOVE_SPEED getMoveSpeed();
	/**
	 * Starts to move the telescope in a given direction.
	 * @param md The direction.
	 * @return True if the command success, false otherwise.
	 */
	public boolean startMove(MOVE_DIRECTION md);
	/**
	 * Moves the telescope in a given direction for some time.
	 * This command is unsupported in Meade LX200 and Celestron.
	 * @param md The direction.
	 * @param seconds The time in seconds. Must be below 10s,
	 * otherwise it will be ignored.
	 * @return True if the command success, false otherwise.
	 */
	public boolean move(MOVE_DIRECTION md, float seconds); // unsupported in LX200, <10s
	/**
	 * Stops moving the telescope in some direction.
	 * @param md The direction.
	 * @return True if the command success, false otherwise.
	 */
	public boolean stopMove(MOVE_DIRECTION md);
	/**
	 * Returns the last direction where the telecope was moved.
	 * @return The last moving direction.
	 */
	public MOVE_DIRECTION getLastMoveDirection();
	/**
	 * Returns the equatorial position of the telescope, as given by
	 * the hardware.
	 * @return The position.
	 */
	public LocationElement getEquatorialPosition();
	/**
	 * Returns the apparent equatorial position of the telescope.
	 * @return The apparent position.
	 */
	public LocationElement getApparentEquatorialPosition();
	/**
	 * Returns the J2000 equatorial position of the telescope.
	 * @return The J2000 position.
	 */
	public LocationElement getJ2000EquatorialPosition();
	/**
	 * Returns the horizontal position of the telescope.
	 * @return The position.
	 */
	public LocationElement getHorizontalPosition();
	/**
	 * Sets the coordinates for an object to later command a goto.
	 * @param loc The position in apparent equatorial coordinates.
	 * @param name Object name.
	 * @return True if the command success, false otherwise.
	 */
	public boolean setObjectCoordinates(LocationElement loc, String name);
	/**
	 * Returns the apparent coordinates of the commanded object.
	 * @return The position.
	 */
	public LocationElement getObjectCoordinates();
	/**
	 * Returns the current object centered.
	 * @return Object centered.
	 */
	public String getObjectName();
	/**
	 * Go to the position commanded.
	 * @return True if the command success, false otherwise.
	 */
	public boolean gotoObject();
	/**
	 * Returns the distance between the current equatorial location
	 * and the input location.
	 * @param loc The input location.
	 * @param isEquatorial True if input location is equatorial (RA, DEC),
	 * false if it is horizontal (azimuth, elevation).
	 * @return The distance in radians.
	 */
	public double distanceToPosition(LocationElement loc, boolean isEquatorial);
	/**
	 * Checks if this telescope is moving by comparing the position between two
	 * times. The implementation for Celestron return true/false ignoring (not
	 * requiring) any of the input parameters.
	 * @param seconds Time to wait to get the position of the telescope for the
	 * 2nd time.
	 * @param tolerance Minimum angular distance to consider that the telescope
	 * moves.
	 * @return True or false.
	 */
	public boolean isMoving(float seconds, double tolerance);
	/**
	 * Returns the local time of the telescope in hours.
	 * @return The local time. -1 is returned if the command fails.
	 */
	public double getLocalTime();
	/**
	 * Sets the local time of the telescope.
	 * @param time Time object.
	 * @param obs The observer, required to correctly set the DST value. Not used 
	 * for Meade telescopes, so it that case it is recommended to use a time object 
	 * with UT scale.
	 * @return True if the command success, false otherwise.
	 */
	public boolean setLocalTime(TimeElement time, ObserverElement obs);
	/**
	 * Synchronizes the telescope position with the coordinates of the
	 * current commanded target object. The position returned by the
	 * scope will be later referred to the target object, improving
	 * precision when locating offsets close to a given one. It is
	 * recommended to call first {@linkplain #distanceToPosition(LocationElement, boolean)}
	 * with the target position to be sure that the distance is below a given
	 * reasonably limit (few degrees). 
	 * 
	 * Note Meade seems to sync to the 
	 * coordinates of the selected object in the internal database of the 
	 * telescope, while Celestron/Synscan uses the coordinates of the current 
	 * object you wanted to center. The second case is more effective since this 
	 * command should be used after fixing the object position in the telescope, so 
	 * the object coordinates will be slightly different from the telescope coordinates.
	 * This command will correct this difference, but it seems Meade does not allow you 
	 * to sync to a given custom sky location.
	 * @return True if the command success, false otherwise.
	 */
	public boolean sync();
	/**
	 * Resets the position of the mount to a given sky location in J2000 coordinates.
	 * Could be useful to unpark the scope to a given position.
	 * @param loc Equatorial J2000 coordinates of the mount. For azimuthal mounts you 
	 * should probably set here the horizontal coordinates.
	 * @return True if the command worked. Ignored for Meade, that will return always false.
	 */
	public boolean reset(LocationElement loc);
	/**
	 * Stops moving the scope.
	 * @return True if the command success, false otherwise.
	 */
	public boolean stopMoving();
	/**
	 * Parks the telescope. Unsupported in LX200 and Celestron.
	 * @return True if the command success, false otherwise.
	 */
	public boolean park(); // unsupported in LX200
	/**
	 * Sets the park position in horizontal coordinates.
	 * @param loc The park position in azimuth and elevation, or null for
	 * automatic park position given by the telescope.
	 * @return True.
	 */
	public boolean setParkPosition(LocationElement loc);
	/**
	 * Returns the park position in horizontal coordinates.
	 * @return Park position.
	 */
	public LocationElement getParkPosition();
	/**
	 * Unparks the telescope. Unsupported in LX200 and Celestron.
	 * @return True if the command success, false otherwise.
	 */
	public boolean unpark(); // unsupported in LX200
	/**
	 * Returns the telescope model name.
	 * @return Model name. In case the command fails, null is returned.
	 */
	public String getTelescopeName();
	/**
	 * Returns the telescope port name.
	 * @return Port name. In case the command fails, null is returned.
	 */
	public String getTelescopePort();
	/**
	 * Returns the observer's location as it is set by the
	 * telescope.
	 * @return The observer object. In case the command fails, null is returned.
	 */
	public ObserverElement getObserver();
	/**
	 * Sets the observer's location in the telescope.
	 * @param obs The new observer object.
	 * @return True if the command worked, false otherwise.
	 */
	public boolean setObserver(ObserverElement obs);
	/**
	 * Return the time and date of the telescope.
	 * @return The time object. In case the command fails, null is returned.
	 */
	public TimeElement getTime();
	/**
	 * Disconnects the telescope.
	 * @return True if the command success, false otherwise.
	 */
	public boolean disconnect();
	/**
	 * Connects to the telescope. During initialization the connection
	 * is opened automatically, you only need to call this in case you
	 * first disconnects.
	 * @return True if the command success, false otherwise.
	 * @throws JPARSECException If an error occurs during the connection.
	 */
	public boolean connect() throws JPARSECException;
	/**
	 * Returns if the telescope is connected or not.
	 * @return True or false.
	 */
	public boolean isConnected();
	/**
	 * Sets if the tracking (in EQ or HOR depending on the mount) should be 
	 * active or not.
	 * @param b True or false to enable/disable tracking.
	 * @return True if command worked.
	 */
	public boolean setTrackingActive(boolean b);
	/**
	 * Returns the telescope model.
	 * @return Telescope model.
	 */
	public TELESCOPE_MODEL getTelescopeModel();
	/**
	 * Sets the field of view of a camera attached to this telescope. This sets
	 * the size of the mark of this telescope when showing
	 * the sky. Set the field to -1 for a default (constant)
	 * size independent of the field.
	 * @param field Field of view in radians.
	 * @param camera The camera index, starting with 0 for the first.
	 */
	public void setFieldOfView(double field, int camera);
	/**
	 * Returns the field of view of a camera attached to this telescope. This
	 * method returns -1 in case the field has not been set.
	 * @param camera The camera index, starting with 0 for the first.
	 * @return Field of view in radians.
	 */
	public double getFieldOfView(int camera);
	/**
	 * Returns the set of cameras attached to the telescope.
	 * @return The cameras.
	 */
	public GenericCamera[] getCameras();
	/**
	 * Sets the cameras attached to the telescope.
	 * @param cameras The set of cameras.
	 * @throws JPARSECException If any of the cameras is incompatible with this telescope.
	 */
	public void setCameras(GenericCamera[] cameras)  throws JPARSECException;
	/**
	 * Returns if this telescope inverts the image horizontally.
	 * @return True or false.
	 */
	public boolean invertHorizontally();
	/**
	 * Returns if this telescope inverts the image vertically.
	 * @return True or false.
	 */
	public boolean invertVertically();
	/**
	 * Sets the telescope type. Default value is Schmidt-Cassegrain.
	 * Changing this will affect the horizontal/vertical inversion
	 * flags returned in other methods.
	 * @param type The goto telescope type.
	 */
	public void setTelescopeType(TELESCOPE_TYPE type);
	/**
	 * Returns the fits header for this telescope and the last image
	 * taken with a given camera attached to it. The entries retrieved are:
	 * <pre>
	 * Entry    Value (typical)   Description
	 * --------------------------------------
	 *
	 * BITPIX      16             Bits per data value
	 * NAXIS        2             Dimensionality
	 * NAXIS1       0             Width
	 * NAXIS2       0             Height
	 * EXTEND       T             Extension permitted
	 * AUTHOR       JPARSEC       Data author
	 * BUNIT        counts        Physical unit
	 * BSCALE       1.0           Data scaling factor
	 * BZERO       32768          (minus) data zero value
	 * DATE-OBS    2013-01-01 ... Date and time, usually in LT
	 * TIME_JD     2451545.123    Date and time as JD, in UT1
	 * OBS_LON     -3.4           Longitude in deg, west negative
	 * OBS_LAT     40.25          Latitude in deg, south negative
	 * OBS_NAME     Madrid        Observer name
	 * OBS_TZ       1             Time zone
	 * OBS_DST      N1            DST code
	 * TEL_MODE     VIRTUAL       Telescope model (driver)
	 * TEL_TYPE     NEWTON        Telescope type (S/C, refractor, ...)
	 * TELESCOP     VIRTUAL       Telescope name
	 * MOUNT        EQUATORIAL    Telescope mount
	 * CONNECT      true          Telescope connected ?
	 * TRACKING     true          Telescope tracking ?
	 * ALIGNED      true          Telescope aligned ?
	 * MOVING       false         Telescope moving ?
	 * RA           12h 00m 00s   Telescope apparent RA
	 * DEC          10&deg; 00' 00"   Telescope apparent DEC
	 * RAJ2000      12h 00m 00s   Telescope J2000 RA
	 * DECJ2000     10&deg; 00' 00"   Telescope J2000 DEC
	 * AZ           10&deg; 00' 00"   Telescope AZ
	 * EL           10&deg; 00' 00"   Telescope EL
	 * DATE0        2013-01-01 .. Date and time for the beginning of the observation
	 * AZ0          10&deg; 00' 00"   Telescope AZ for the beginning of the observation
	 * EL0          10&deg; 00' 00"   Telescope EL for the beginning of the observation
	 * DATE-EFF     2013-01-01 .. Date and time for the middle of the observation
	 * AZ-EFF       10&deg; 00' 00"   Telescope AZ for the middle of the observation
	 * EL-EFF       10&deg; 00' 00"   Telescope EL for the middle of the observation
	 * ... + camera entries ...
	 * FIELD        0.5           Camera field of view (deg)
	 * CAM_INDE     0             Camera index id value
	 * </pre>
	 * The values for BITPIX, NAXIS1, NAXIS2, and BZERO are later modified depending on the
	 * camera used. The values for DATE-OBS, TIME_JD, AZ, EL are for the end of the photo.
	 * @param cameraIndex Index for the camera, or -1 to take no camera.
	 * @return The set of entries for the header.
	 */
	public HeaderElement[] getFitsHeader(int cameraIndex);
}
