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
package jparsec.graph.chartRendering;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.Target;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.moons.MoonEphem;
import jparsec.ephem.planets.EphemElement;
import jparsec.ephem.planets.OrbitEphem;
import jparsec.ephem.planets.OrbitalElement;
import jparsec.ephem.probes.SDP4_SGP4;
import jparsec.ephem.probes.SatelliteEphem;
import jparsec.ephem.probes.SatelliteEphemElement;
import jparsec.ephem.probes.Spacecraft;
import jparsec.ephem.stars.StarElement;
import jparsec.ephem.stars.StarEphem;
import jparsec.ephem.stars.StarEphemElement;
import jparsec.graph.JPARSECStroke;
import jparsec.graph.chartRendering.RenderSky.OBJECT;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.io.ReadFile.FORMAT;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.AstroDate;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.util.Configuration;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;
import jparsec.util.Translate.LANGUAGE;

/**
 * Support class for sky rendering operations, allowing to represent trajectory
 * paths of Solar System objects or stars.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class TrajectoryElement implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Default empty constructor.
	 */
	public TrajectoryElement()
	{
		objectType = null;
		objectName = "";
		apparentObjectName = "";
		startTimeJD = endTimeJD = stepTimeJD = 0.0f;
		drawLabels = false;
		labelsSteps = 0;
		drawLabelsFormat = LABELS.YEAR_MONTH_DAY;
		autoCentering = autoScale = false;
	}

	/**
	 * Explicit constructor.
	 *
	 * @param obj_type Object type ID constant. Can be null to search it automatically (any object
	 * except probes).
	 * @param obj_name Object name.
	 * @param JD_init Initial Julian day TDB.
	 * @param JD_end Ending Julian day.
	 * @param JD_step Time step between calculations.
	 * @param labels Show labels.
	 * @param labels_type Type of labels.
	 * @param labels_step Number of steps between two consecutive labels.
	 * @param auto_center To automatically center the path.
	 * @param auto_scale To automatically scale the render.
	 * @throws JPARSECException If the object type is unsupported.
	 */
	public TrajectoryElement(OBJECT obj_type, String obj_name, double JD_init, double JD_end, float JD_step,
			boolean labels, LABELS labels_type, int labels_step, boolean auto_center, boolean auto_scale) throws JPARSECException
	{
		if (obj_type == null) {
			TARGET t = jparsec.ephem.Target.getID(obj_name);
			if (t == TARGET.NOT_A_PLANET && Translate.getDefaultLanguage() != LANGUAGE.ENGLISH)
				t = jparsec.ephem.Target.getIDFromEnglishName(obj_name);
			if (t != TARGET.NOT_A_PLANET) obj_type = OBJECT.PLANET;
		}
		if (obj_type == null) {
			LocationElement se = null;
			int index = StarEphem.getStarTargetIndex(obj_name);
			if (index == -1) {
				se = RenderSky.searchDeepSkyObjectJ2000(obj_name);
				if (se == null) {
					int ai = OrbitEphem.getIndexOfAsteroid(obj_name);
					if (ai >= 0) {
						obj_type = OBJECT.ASTEROID;
					} else {
						int ci = OrbitEphem.getIndexOfComet(obj_name);
						if (ci >= 0) {
							obj_type = OBJECT.COMET;
						} else {
							int ti = OrbitEphem.getIndexOfTransNeptunian(obj_name);
							if (ti >= 0) {
								obj_type = OBJECT.TRANSNEPTUNIAN;
							} else {
								try {
									ReadFile re = new ReadFile();
									re.setFormat(ReadFile.FORMAT.MPC);
									re.setPath(OrbitEphem.PATH_TO_MPC_NEOs_FILE);
									try { re.readFileOfNEOs((JD_init + JD_end) * 0.5, 365); } catch (Exception exc) {}
									int n = re.searchByName(obj_name);
									if (n >= 0) {
										obj_type = OBJECT.NEO;
									} else {
										int br = obj_name.indexOf("[");
										if (br > 0) obj_name = obj_name.substring(0, br).trim();
										n = SatelliteEphem.getArtificialSatelliteTargetIndex(obj_name);
										if (n < 0) {
											SatelliteEphem.USE_IRIDIUM_SATELLITES = !SatelliteEphem.USE_IRIDIUM_SATELLITES;
											SatelliteEphem.setSatellitesFromExternalFile(null);
											n = SatelliteEphem.getArtificialSatelliteTargetIndex(obj_name);
											if (n < 0) {
												SatelliteEphem.USE_IRIDIUM_SATELLITES = !SatelliteEphem.USE_IRIDIUM_SATELLITES;
												SatelliteEphem.setSatellitesFromExternalFile(null);
											}
										}
										if (n >= 0) obj_type = OBJECT.ARTIFICIAL_SATELLITE;
									}
								} catch (JPARSECException exc) {}
							}
						}
					}
				} else {
					obj_type = OBJECT.DEEPSKY;
				}
			} else {
				obj_type = OBJECT.STAR;
			}
		}

		if (obj_type == OBJECT.NOVA //|| obj_type == OBJECT.ARTIFICIAL_SATELLITE
				|| obj_type == OBJECT.DEEPSKY || obj_type == OBJECT.SUPERNOVA)
			throw new JPARSECException("Unsupported object type.");
		objectType = obj_type;
		objectName = obj_name;
		apparentObjectName = obj_name;
		startTimeJD = JD_init;
		endTimeJD = JD_end;
		stepTimeJD = JD_step;
		drawLabels = labels;
		labelsSteps = labels_step;
		autoCentering = auto_center;
		autoScale = auto_scale;
		drawLabelsFormat = labels_type;
	}

	/**
	 * Selects type of object through the provided symbolic constants.
	 */
	public OBJECT objectType;

	/**
	 * Sets objects name, should be correct to find it when calculating ephemeris.
	 */
	public String objectName;
	/**
	 * Set apparent object name to be shown when rendering the sky.
	 */
	public String apparentObjectName;
	
	/**
	 * Text to add to each position as a label.
	 */
	public String addLabel[];

	/**
	 * Initial Julian day of calculations in Barycentric dynamical time.
	 */
	public double startTimeJD;

	/**
	 * Ending Julian day of calculations in Barycentric dynamical time.
	 */
	public double endTimeJD;

	/**
	 * Sets Julian day step for calculations.
	 */
	public float stepTimeJD;

	/**
	 * Set the stroke to draw the path. Default is a thin line.
	 */
	public JPARSECStroke stroke = JPARSECStroke.STROKE_DEFAULT_LINE_THIN;

	/**
	 * True if labels should be drawn.
	 */
	public boolean drawLabels;

	/**
	 * The time scale for labels, default is local time.
	 */
	public SCALE timeScaleForLabels = SCALE.LOCAL_TIME;

	/**
	 * Sets the format of the labels according to the provided symbolic
	 * constants.
	 */
	public LABELS drawLabelsFormat;

	/**
	 * Sets number of steps between two consecutive drawn labels. Time step will
	 * be this value multiplied by the step time.
	 */
	public int labelsSteps;

	/**
	 * True if it is desirable to auto center the trajectory path. This
	 * overrides any other configuration in the sky render object.
	 */
	public boolean autoCentering;

	/**
	 * True if it is desirable to auto scale the trajectory path. Auto scale
	 * operation is only performed when auto centering is enabled. This
	 * overrides any other configuration in the sky render object.
	 */
	public boolean autoScale;

	/**
	 * Sets the font to be used for paths.
	 */
	public Graphics.FONT drawPathFont = Graphics.FONT.DIALOG_ITALIC_12;

	/**
	 * Sets the font color to be used for main paths.
	 */
	public int drawPathColor1 = 128<<24 | 0<<16 | 0<<8 | 0;

	/**
	 * Sets the font color to be used to highlight points in the paths
	 * with text labels.
	 */
	public int drawPathColor2 = 128<<24 | 255<<16 | 0<<8 | 0;

	/**
	 * The set of label types.
	 */
	public enum LABELS {
		/** Symbolic constant for numeric labels of type YYYY-MM-DD. */
		YEAR_MONTH_DAY,
		/** Symbolic constant for numeric labels of type MM-DD. */
		MONTH_DAY,
		/** Symbolic constant for numeric labels of type MM-DD-YYYY. */
		MONTH_DAY_YEAR,
		/** Symbolic constant for labels of type DD-MMM, with month expressed with 3 characters. */
		DAY_MONTH_ABBREVIATION;
	};

	/** True (default) to show the time scale. LT (local time) is not shown. */
	public boolean showTimeScale = true;

	/** True (default) to show the time when it is not 0h or the interval between calculations is very little.
	 * Setting to false will also hide the time scale. */
	public boolean showTime = true;

	/**
	 * Clones this instance.
	 */
	@Override
	public TrajectoryElement clone()
	{
		TrajectoryElement t = new TrajectoryElement();
		t.autoCentering = this.autoCentering;
		t.autoScale = this.autoScale;
		t.drawLabels = this.drawLabels;
		t.timeScaleForLabels = this.timeScaleForLabels;
		t.drawLabelsFormat = this.drawLabelsFormat;
		t.drawPathColor1 = this.drawPathColor1;
		t.drawPathFont = this.drawPathFont;
		t.endTimeJD = this.endTimeJD;
		t.labelsSteps = this.labelsSteps;
		t.objectName = this.objectName;
		t.apparentObjectName = this.apparentObjectName;
		t.addLabel = this.addLabel;
		t.objectType = this.objectType;
		t.startTimeJD = this.startTimeJD;
		t.stepTimeJD = this.stepTimeJD;
		t.drawPathColor2 = this.drawPathColor2;
		t.showTime = this.showTime;
		t.showTimeScale = this.showTimeScale;
		t.stroke = this.stroke.clone();
		t.central_loc = null;
		if (this.central_loc != null) t.central_loc = this.central_loc.clone();
		t.loc_path = null;
		if (this.loc_path != null) {
			t.loc_path = new LocationElement[this.loc_path.length];
			for (int i=0; i<t.loc_path.length; i++) {
				t.loc_path[i] = null;
				if (loc_path[i] != null) t.loc_path[i] = loc_path[i].clone();
			}
		}
		t.loc_path2 = null;
		if (this.loc_path2 != null) {
			t.loc_path2 = new LocationElement[this.loc_path2.length];
			for (int i=0; i<t.loc_path2.length; i++) {
				t.loc_path2[i] = null;
				if (loc_path2[i] != null) t.loc_path2[i] = loc_path2[i].clone();
			}
		}
		return t;
	}
	/**
	 * Returns true if the input object is equals to this instance.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof TrajectoryElement)) return false;

		TrajectoryElement that = (TrajectoryElement) o;

		if (Double.compare(that.startTimeJD, startTimeJD) != 0) return false;
		if (Double.compare(that.endTimeJD, endTimeJD) != 0) return false;
		if (Float.compare(that.stepTimeJD, stepTimeJD) != 0) return false;
		if (drawLabels != that.drawLabels) return false;
		if (labelsSteps != that.labelsSteps) return false;
		if (autoCentering != that.autoCentering) return false;
		if (autoScale != that.autoScale) return false;
		if (drawPathColor1 != that.drawPathColor1) return false;
		if (drawPathColor2 != that.drawPathColor2) return false;
		if (showTimeScale != that.showTimeScale) return false;
		if (showTime != that.showTime) return false;
		if (objectType != that.objectType) return false;
		if (objectName != null ? !objectName.equals(that.objectName) : that.objectName != null) return false;
		if (apparentObjectName != null ? !apparentObjectName.equals(that.apparentObjectName) : that.apparentObjectName != null)
			return false;
		if (stroke != null ? !stroke.equals(that.stroke) : that.stroke != null) return false;
		if (timeScaleForLabels != that.timeScaleForLabels) return false;
		if (drawLabelsFormat != that.drawLabelsFormat) return false;
		if (drawPathFont != that.drawPathFont) return false;
		// Probably incorrect - comparing Object[] arrays with Arrays.equals
		if (!Arrays.equals(loc_path, that.loc_path)) return false;
		if (!Arrays.equals(loc_path2, that.loc_path2)) return false;
		if (!Arrays.equals(addLabel, that.addLabel)) return false;
		if (central_loc != null ? !central_loc.equals(that.central_loc) : that.central_loc != null) return false;

		return !(star != null ? !star.equals(that.star) : that.star != null);
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		result = objectType != null ? objectType.hashCode() : 0;
		result = 31 * result + (objectName != null ? objectName.hashCode() : 0);
		result = 31 * result + (apparentObjectName != null ? apparentObjectName.hashCode() : 0);
		temp = Double.doubleToLongBits(startTimeJD);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(endTimeJD);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (stepTimeJD != +0.0f ? Float.floatToIntBits(stepTimeJD) : 0);
		result = 31 * result + (stroke != null ? stroke.hashCode() : 0);
		result = 31 * result + (drawLabels ? 1 : 0);
		result = 31 * result + (timeScaleForLabels != null ? timeScaleForLabels.hashCode() : 0);
		result = 31 * result + (drawLabelsFormat != null ? drawLabelsFormat.hashCode() : 0);
		result = 31 * result + labelsSteps;
		result = 31 * result + (autoCentering ? 1 : 0);
		result = 31 * result + (autoScale ? 1 : 0);
		result = 31 * result + (drawPathFont != null ? drawPathFont.hashCode() : 0);
		result = 31 * result + drawPathColor1;
		result = 31 * result + drawPathColor2;
		result = 31 * result + (showTimeScale ? 1 : 0);
		result = 31 * result + (showTime ? 1 : 0);
		result = 31 * result + (loc_path != null ? Arrays.hashCode(loc_path) : 0);
		result = 31 * result + (loc_path2 != null ? Arrays.hashCode(loc_path2) : 0);
		result = 31 * result + (addLabel != null ? Arrays.hashCode(addLabel) : 0);
		result = 31 * result + (central_loc != null ? central_loc.hashCode() : 0);
		result = 31 * result + (star != null ? star.hashCode() : 0);
		return result;
	}

	/**
	 * An array to store the equatorial positions of the object along the path.
	 */
	public LocationElement[] loc_path = null;
	private LocationElement[] loc_path2 = null;
	
	/**
	 * Position at the center of the trajectory.
	 */
	public LocationElement central_loc;

	/**
	 * Populates {@linkplain TrajectoryElement#loc_path} with the equatorial positions
	 * along the trajectory. In case the {@linkplain #loc_path} array is already populated
	 * this method does nothing.
	 * @param obs Observer object containing the observer position.
	 * @param eph Ephemeris object defining the ephemeris properties.
	 * @throws JPARSECException If an error occurs.
	 */
	public void populateTrajectoryPath(ObserverElement obs, EphemerisElement eph)
	throws JPARSECException {
		if (loc_path2 != null) {
			loc_path = loc_path2.clone();
			return;
		}
		if (loc_path != null) return;
		if (this.objectType != null)
		{
			int step = -1;
			int max_steps = 1 + (int) ((this.endTimeJD - this.startTimeJD) / this.stepTimeJD);
			this.loc_path = new LocationElement[max_steps];
			EphemerisElement eph_path = eph.clone();
			eph_path.targetBody = Target.getID(this.objectName);
			double ra_min = 0.0, ra_max = 0.0, dec_min = 0.0, dec_max = 0.0;
			EphemElement ephem = new EphemElement();
			boolean all_ok = true;
			int my_star = -1, probe = -1;
			ReadFile reStar = null;
			if (this.objectType == OBJECT.STAR && star == null)
			{
				reStar = new ReadFile();
				reStar.setPath(FileIO.DATA_STARS_SKY2000_DIRECTORY + "JPARSEC_Sky2000.txt");
				reStar.setFormat(ReadFile.FORMAT.JPARSEC_SKY2000);
				reStar.readFileOfStars();

				my_star = RenderSky.getStar(this.objectName, reStar);
				if (my_star < 0)
					all_ok = false;
			}
			if (this.objectType == OBJECT.PROBE)
			{
				probe = Spacecraft.searchProbe(this.objectName);
				eph_path.targetBody = TARGET.NOT_A_PLANET;
				eph_path.targetBody.setIndex(probe);
				eph_path.algorithm = EphemerisElement.ALGORITHM.PROBE;
				if (probe < 0)
					all_ok = false;
			}
			if (this.objectType == OBJECT.ASTEROID || this.objectType == OBJECT.TRANSNEPTUNIAN)
			{
				String file = "MPC_asteroids_bright.txt";
				if (this.objectType == OBJECT.TRANSNEPTUNIAN)
					file = "MPC_distant_bodies.txt";
				ReadFile re = new ReadFile();
				re.setFormat(ReadFile.FORMAT.MPC);
				re.setPath(FileIO.DATA_ORBITAL_ELEMENTS_DIRECTORY + file);

				eph_path.targetBody = TARGET.Asteroid;
				eph_path.algorithm = EphemerisElement.ALGORITHM.ORBIT;

				re.readFileOfAsteroids();
				int aster = -1;
				OrbitalElement orbit[] = (OrbitalElement[]) re.getReadElements();
				for (int indexO = 0; indexO < orbit.length; indexO++)
				{
					if (orbit[indexO].name.toLowerCase().indexOf(this.objectName.toLowerCase()) >= 0)
					{
						eph_path.orbit = orbit[indexO];
						aster = indexO;
					}
				}
				// Elements must be always the latest available ones to match
				// trajectory with sky rendering
//				if (aster < 0) {
					try {
						AstroDate astro = new AstroDate(startTimeJD);
						String p = null;
						if (this.objectType == OBJECT.ASTEROID) {
							p = Configuration.updateAsteroidsInTempDir(astro);
							if (p != null) {
								orbit = OrbitEphem.getOrbitalElementsOfAsteroids();
								for (int indexO = 0; indexO < orbit.length; indexO++)
								{
									if (orbit[indexO].name.toLowerCase().indexOf(this.objectName.toLowerCase()) >= 0)
									{
										eph_path.orbit = orbit[indexO];
										aster = indexO;
									}
								}
							}
						} else {
							p = Configuration.updateTransNeptuniansInTempDir(astro);
							if (p != null) {
								orbit = OrbitEphem.getOrbitalElementsOfTransNeptunians();
								for (int indexO = 0; indexO < orbit.length; indexO++)
								{
									if (orbit[indexO].name.toLowerCase().indexOf(this.objectName.toLowerCase()) >= 0)
									{
										eph_path.orbit = orbit[indexO];
										aster = indexO;
									}
								}
							}
						}
					} catch (Exception exc) {}
//				}

				if (aster < 0)
					all_ok = false;
			}
			if (this.objectType == OBJECT.COMET)
			{
				ReadFile re = new ReadFile();
				re.setFormat(ReadFile.FORMAT.MPC);
				re.setPath(OrbitEphem.PATH_TO_MPC_COMETS_FILE);

				eph_path.targetBody = TARGET.Comet;
				eph_path.algorithm = EphemerisElement.ALGORITHM.ORBIT;

				re.readFileOfComets();
				int comet = -1;
				OrbitalElement orbit[] = (OrbitalElement[]) re.getReadElements();
				for (int indexO = 0; indexO < orbit.length; indexO++)
				{
					if (orbit[indexO].name.toLowerCase().indexOf(this.objectName.toLowerCase()) >= 0)
					{
						eph_path.orbit = orbit[indexO];
						comet = indexO;
					}
				}
				if (comet < 0) {
					ArrayList<OrbitalElement> oldComets = ReadFile.readFileOfOldComets(new AstroDate(startTimeJD), 10);
					OrbitEphem.setCometsFromElements(oldComets);
					orbit = OrbitEphem.getOrbitalElementsOfComets();
					for (int indexO = 0; indexO < orbit.length; indexO++)
					{
						if (orbit[indexO].name.toLowerCase().indexOf(this.objectName.toLowerCase()) >= 0)
						{
							eph_path.orbit = orbit[indexO];
							comet = indexO;
						}
					}
					if (comet < 0) OrbitEphem.setCometsFromElements(null);
				}

//				if (comet < 0) {
					try {
						AstroDate astro = new AstroDate(startTimeJD);
						String p = Configuration.updateCometsInTempDir(astro);
						if (p == null) {
							ArrayList<OrbitalElement> oldComets = ReadFile.readFileOfOldComets(astro, 10);
							if (oldComets != null) OrbitEphem.setCometsFromElements(oldComets);
						}
						orbit = OrbitEphem.getOrbitalElementsOfComets();
						for (int indexO = 0; indexO < orbit.length; indexO++)
						{
							if (orbit[indexO].name.toLowerCase().indexOf(this.objectName.toLowerCase()) >= 0)
							{
								eph_path.orbit = orbit[indexO];
								comet = indexO;
							}
						}
					} catch (Exception exc) {}
//				}
				if (comet < 0)
					all_ok = false;
			}
			if (this.objectType == OBJECT.NEO)
			{
				eph_path.targetBody = TARGET.NEO;
				eph_path.algorithm = EphemerisElement.ALGORITHM.ORBIT;
				int neo = -1;

				ReadFile re2 = new ReadFile(FORMAT.MPC, OrbitEphem.PATH_TO_MPC_NEOs_FILE);
				try { re2.readFileOfNEOs(startTimeJD, 365); } catch (Exception exc) {}
				OrbitalElement[] orbit = (OrbitalElement[]) re2.getReadElements();
				for (int indexO = 0; indexO < orbit.length; indexO++)
				{
					if (orbit[indexO].name.toLowerCase().indexOf(this.objectName.toLowerCase()) >= 0)
					{
						eph_path.orbit = orbit[indexO];
						neo = indexO;
					}
				}

				if (neo < 0) all_ok = false;
			}
			if (this.objectType == OBJECT.ARTIFICIAL_SATELLITE)
			{
				eph_path.targetBody = TARGET.NOT_A_PLANET;
				eph_path.algorithm = EphemerisElement.ALGORITHM.ARTIFICIAL_SATELLITE;
				int br = objectName.indexOf("[");
				if (br > 0) objectName = objectName.substring(0, br).trim();
				int sat = SatelliteEphem.getArtificialSatelliteTargetIndex(objectName);
				if (sat < 0) {
					SatelliteEphem.USE_IRIDIUM_SATELLITES = !SatelliteEphem.USE_IRIDIUM_SATELLITES;
					SatelliteEphem.setSatellitesFromExternalFile(null);
					sat = SatelliteEphem.getArtificialSatelliteTargetIndex(objectName);
					if (sat < 0) {
						SatelliteEphem.USE_IRIDIUM_SATELLITES = !SatelliteEphem.USE_IRIDIUM_SATELLITES;
						SatelliteEphem.setSatellitesFromExternalFile(null);
					}
				}
				if (sat >= 0) eph_path.targetBody.setIndex(sat);
				if (sat < 0) all_ok = false;
			}
			if (all_ok && this.startTimeJD > 1228000.5 && this.startTimeJD < 2817057.5 && this.endTimeJD > 1228000.5 && this.endTimeJD < 2817057.5)
			{
				for (double Jdate = this.startTimeJD; Jdate <= this.endTimeJD; Jdate += this.stepTimeJD)
				{
					step++;
					if (loc_path.length == step) break;

					TimeElement time_path = new TimeElement(Jdate, TimeElement.SCALE.BARYCENTRIC_DYNAMICAL_TIME);
					switch (this.objectType)
					{
					case PLANET:
						if (eph_path.targetBody.isNaturalSatellite()) {
							switch (eph.targetBody)
							{
							case MARS:
								ephem = EphemElement.parseMoonEphemElement(
										MoonEphem.martianSatellitesEphemerides_2007(time_path, obs, eph_path)
										[eph_path.targetBody.ordinal()-TARGET.Phobos.ordinal()], Jdate);
								break;
							case JUPITER:
								ephem = EphemElement.parseMoonEphemElement(
										MoonEphem.galileanSatellitesEphemerides_L1(time_path, obs, eph_path)
										[eph_path.targetBody.ordinal()-TARGET.Io.ordinal()], Jdate);
								break;
							case SATURN:
								ephem = EphemElement.parseMoonEphemElement(
										MoonEphem.saturnianSatellitesEphemerides_TASS17(time_path, obs, eph_path, false)
										[eph_path.targetBody.ordinal()-TARGET.Mimas.ordinal()], Jdate);
								break;
							case URANUS:
								ephem = EphemElement.parseMoonEphemElement(
										MoonEphem.uranianSatellitesEphemerides_GUST86(time_path, obs, eph_path)
										[eph_path.targetBody.ordinal()-TARGET.Miranda.ordinal()], Jdate);
								break;
							default:
								throw new JPARSECException("unsupported body for trajectory. Use one of the main satellites of Mars, Jupiter, Saturn, or Uranus.");
							}
						} else {
							ephem = Ephem.getEphemeris(time_path, obs, eph_path, false, true); //PlanetEphem.MoshierEphemeris(time_path, obs, eph_path);
						}
						this.loc_path[step] = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
						break;
					case STAR:
						this.loc_path[step] = calcStar(my_star, time_path, obs, eph_path, reStar);
						break;
					case PROBE:
						probe = Spacecraft.searchProbe(this.objectName, Jdate);
						eph_path.targetBody = TARGET.NOT_A_PLANET;
						eph_path.targetBody.setIndex(probe);
						ephem = Spacecraft.orbitEphemeris(time_path, obs, eph_path);
						this.loc_path[step] = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
						break;
					case ASTEROID:
					case TRANSNEPTUNIAN:
					case COMET:
					case NEO:
						ephem = OrbitEphem.orbitEphemeris(time_path, obs, eph_path);
						this.loc_path[step] = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
						break;
					case ARTIFICIAL_SATELLITE:
						SatelliteEphemElement see = SDP4_SGP4.satEphemeris(time_path, obs, eph_path, false);
						ephem = EphemElement.parseSatelliteEphemElement(see, time_path.astroDate.jd());
						this.loc_path[step] = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
						if (see.isEclipsed) loc_path[step].setRadius(EphemElement.INVALID_MAGNITUDE);
						break;
					default:
						throw new JPARSECException("invalid trajectory object type.");
					}

					if (step == (max_steps / 2))
						central_loc = this.loc_path[step];
					if (step == 0 || ephem.rightAscension < ra_min)
						ra_min = ephem.rightAscension;
					if (step == 0 || ephem.rightAscension > ra_max)
						ra_max = ephem.rightAscension;
					if (step == 0 || ephem.declination < dec_min)
						dec_min = ephem.declination;
					if (step == 0 || ephem.declination > dec_max)
						dec_max = ephem.declination;
				}
			} else
			{
				throw new JPARSECException(
						"cannot obtain trajectory (bad date, inexistent object, ...).");
			}
		} else {
			throw new JPARSECException(
					"cannot obtain trajectory (undefined object).");
		}
	}

	/**
	 * Populates {@linkplain TrajectoryElement#loc_path} with the equatorial positions
	 * provided as input, forcing the position of the object. This method is useful when
	 * you have ephemerides obtained with other tools.
	 * @param positions An array of positions. Its size should be equal at least
	 * to {@linkplain #getNumberofPointsInTrajectory()}.
	 * @param objName The name for the object when showing the label, or null to maintain
	 * the default one.
	 * @throws JPARSECException If an error occurs.
	 */
	public void populateTrajectoryPath(LocationElement positions[], String objName)
	throws JPARSECException {
		int max_steps = 1 + (int) ((this.endTimeJD - this.startTimeJD) / this.stepTimeJD);
		this.loc_path = new LocationElement[max_steps];
		for (int i=0; i<max_steps; i++) {
			loc_path[i] = positions[i];
		}
		this.central_loc = loc_path[max_steps/2];
		if (objName != null) this.apparentObjectName = objName;
		loc_path2 = loc_path.clone();
		return;
	}

	/**
	 * Populates {@linkplain TrajectoryElement#loc_path} with the equatorial positions
	 * provided as input, forcing the position of the object. This method is useful when
	 * you have ephemerides obtained with other tools.
	 * @param positions An array of positions. Its size should be equal at least
	 * to {@linkplain #getNumberofPointsInTrajectory()}.
	 * @param addLabel Text to add to the label in each position.
	 * @param objName The name for the object when showing the label, or null to maintain
	 * the default one.
	 * @throws JPARSECException If an error occurs.
	 */
	public void populateTrajectoryPath(LocationElement positions[], String addLabel[], String objName)
	throws JPARSECException {
		int max_steps = 1 + (int) ((this.endTimeJD - this.startTimeJD) / this.stepTimeJD);
		this.loc_path = new LocationElement[max_steps];
		for (int i=0; i<max_steps; i++) {
			loc_path[i] = positions[i];
		}
		this.central_loc = loc_path[max_steps/2];
		if (addLabel != null) this.addLabel = addLabel;
		if (objName != null) this.apparentObjectName = objName;
		loc_path2 = loc_path.clone();
		return;
	}
	
	/**
	 * Returns the number of points to be calculated for the trajectory.
	 * @return Number of points.
	 */
	public int getNumberofPointsInTrajectory() {
		return 1 + (int) ((this.endTimeJD - this.startTimeJD) / this.stepTimeJD);
	}

	private LocationElement calcStar(int my_star, TimeElement time, ObserverElement observer, EphemerisElement eph,
			ReadFile re)
			throws JPARSECException
	{
		if (star != null) {
			StarEphemElement star_ephem = StarEphem.starEphemeris(time, observer, eph, star, false);
			return new LocationElement(star_ephem.rightAscension, star_ephem.declination, 1.0);
		} else {
			StarElement star = (StarElement) re.getReadElements()[my_star];
			StarEphemElement star_ephem = StarEphem.starEphemeris(time, observer, eph, star, false);
			return new LocationElement(star_ephem.rightAscension, star_ephem.declination, 1.0);
		}
	}

	private StarElement star = null;
	/**
	 * Sets the star object to be used to calculate its trajectory instead
	 * of having to read the entire star catalog and search for it. Default
	 * value is null to read the catalog, but for Android it is good to set
	 * it here so that performance is much better.
	 * @param s The star object, or null.
	 */
	public void setStarObject(StarElement s) {
		star = s;
	}
}
