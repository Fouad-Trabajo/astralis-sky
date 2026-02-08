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

import java.io.Serializable;

import jparsec.ephem.EphemerisElement;
import jparsec.ephem.Target.TARGET;
import jparsec.graph.chartRendering.RenderPlanet;
import jparsec.util.JPARSECException;

/**
 * A class to instantiate an observer outside the Earth, as a previous step
 * to calculate ephemerides. In the ephemerides properties the algorithm to
 * use must support this observer (Moshier, Series96, or any of the JPL
 * integration methods, but not ELP2000 for the Moon). When using other
 * bodies take into account that local time = UTC on Earth, since no DST
 * rule can be applied. In practice, NEVER use local time when obtaining
 * ephemerides for an observer out from the Earth.
 *
 * @see EphemerisElement
 * @see ObserverElement
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class ExtraterrestrialObserverElement implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Basic constructor.
	 * @param n A name for the place.
	 * @param pos The mean barycentric equatorial vector (position and velocity) of the observer, J2000 equinox.
	 * @throws JPARSECException If the length or the position vector is not 6.
	 */
	public ExtraterrestrialObserverElement(String n, double pos[]) throws JPARSECException {
		if (pos.length != 6) throw new JPARSECException("Position vector must be of length 6 (position+velocity).");
		this.name = n;
		this.barycentricVector = pos;
	}

	/**
	 * Basic constructor.
	 * @param feature The name of a planetary feature.
	 * @param target The body where the feature is located.
	 * @throws JPARSECException If the feature is not found or the body is unsupported.
	 */
	public ExtraterrestrialObserverElement(String feature, TARGET target) throws JPARSECException {
		obsLoc = RenderPlanet.identifyFeature(feature, target);
		if (obsLoc != null) obsLoc.setRadius(0.0);
		name = feature;
		motherPlanet = target;
		if (target.isNaturalSatellite() && (target.ordinal() < TARGET.Phobos.ordinal() || target.ordinal() > TARGET.Oberon.ordinal()))
			throw new JPARSECException("unsupported body. Use one of the main satellites of Mars, Jupiter, Saturn, or Uranus.");
	}

	/**
	 * Full constructor for an observer located on the surface of another non-Earth planet/satellite.
	 * @param feature The name of a planetary feature. Just informative, not used internally.
	 * @param target The body where the feature is located.
	 * @param loc The location of the observer. Here you must be careful to put the correct
	 * longitude on a given planet, since the longitude to use here is the one calculated using
	 * the IAU recomendations for the prime meridian of the planets, with has opposite sign in
	 * some cases respect planetary nomenclature. The cases are Mars and the rest of outer
	 * planets/satellites from Earth. For instance, Curiosity landed at 137.4&deg; E of longitude
	 * (and 4.5&deg; S of latitude), here you should put -137.4 degrees (in radians). The radius
	 * of the location object should be the desired elevation above sea level in m. You can use 0.
	 * @throws JPARSECException If the body is not supported.
	 */
	public ExtraterrestrialObserverElement(String feature, TARGET target, LocationElement loc) throws JPARSECException {
		obsLoc = loc;
		name = feature;
		motherPlanet = target;
		if (target.isNaturalSatellite() && (target.ordinal() < TARGET.Phobos.ordinal() || target.ordinal() > TARGET.Oberon.ordinal()))
				throw new JPARSECException("unsupported body. Use one of the main satellites of Mars, Jupiter, Saturn, or Uranus.");
	}

	/** The mean equatorial J2000 barycentric vector of the observer (AU, AU/d). This vector is only
	 * used when the observer is located in the space, not in the surface of another body. */
	public double barycentricVector[];
	/** A name for the place. */
	public String name;
	/** The mother planet where the observer is located. */
	public TARGET motherPlanet = null;
	/** The observer location in the mother planet. */
	public LocationElement obsLoc = null;
}
