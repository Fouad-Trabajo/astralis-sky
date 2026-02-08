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
package jparsec.astrophysics.gildas;


import java.awt.Color;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Set;
import java.util.TreeMap;

import jparsec.astronomy.CoordinateSystem;
import jparsec.astrophysics.FluxElement;
import jparsec.astrophysics.Spectrum;
import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.EphemerisElement.FRAME;
import jparsec.ephem.Functions;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.planets.PlanetEphem;
import jparsec.ephem.stars.StarElement;
import jparsec.ephem.stars.StarEphem;
import jparsec.graph.ChartElement;
import jparsec.graph.ChartSeriesElement;
import jparsec.graph.CreateChart;
import jparsec.graph.DataSet;
import jparsec.graph.JPARSECStroke;
import jparsec.graph.SimpleChartElement;
import jparsec.io.FileIO;
import jparsec.io.binaryFormat.Convertible;
import jparsec.io.image.FitsBinaryTable;
import jparsec.io.image.FitsIO;
import jparsec.io.image.HeaderElement;
import jparsec.math.Constant;
import jparsec.math.FastMath;
import jparsec.math.Interpolation;
import jparsec.math.MeasureElement;
import jparsec.math.Table;
import jparsec.observer.LocationElement;
import jparsec.observer.Observatory;
import jparsec.observer.ObserverElement;
import jparsec.observer.Country.COUNTRY;
import jparsec.time.AstroDate;
import jparsec.time.DateTimeOps;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.time.TimeScale;
import jparsec.util.JPARSECException;
import jparsec.util.Logger;
import jparsec.util.Logger.LEVEL;
import jparsec.util.Translate;
import nom.tam.fits.BinaryTableHDU;

/**
 * A class to hold the properties of an spectrum, including all the information from
 * the header. The spectrum can be written as a .30m or .fits file. In the latter case
 * it can be read by CLASS using the command FITS READ.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Spectrum30m implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor, note it is useless because the treemap is empty
	 * and there is no header or data.
	 */
    public Spectrum30m()
    {
        map = new TreeMap<String,HeaderElement>();
    }

    /**
     * Reads the first spectrum in a given .30m file.
     * @param path The path to the .30m file.
     * @throws JPARSECException If an error occurs.
     */
    public Spectrum30m(String path) throws JPARSECException {
		Gildas30m g = new Gildas30m(path);
		int list[] = g.getListOfSpectrums(true);
		Spectrum30m s30m = g.getSpectrum(list[0]);
		map = s30m.map;
		header = s30m.header;
		data = s30m.data;
    }

	/**
	 * Default constructor, the treemap is empty so put method should be used to insert
	 * data.
	 * @param header The header.
	 * @param spectrumData The spectrum data.
	 */
    public Spectrum30m(SpectrumHeader30m header, float[] spectrumData)
    {
		this(new TreeMap<String,HeaderElement>(), header, spectrumData);
    }

	/**
	 * Full constructor.
	 * @param tmap The map with the detailed information.
	 * @param header The header.
	 * @param spectrumData The spectrum data.
	 */
    public Spectrum30m(TreeMap<String,HeaderElement> tmap, SpectrumHeader30m header, float[] spectrumData)
    {
        map = (TreeMap) tmap.clone();
        this.header = header.clone();
        this.data = spectrumData.clone();
    }

    /**
     * Adds/replaces a given key with a value.
     * @param key The key name.
     * @param value Tha value.
     */
    public void put(String key, HeaderElement value)
    {
        map.put(key, value);
    }

    /**
     * Returns a given value of some key.
     * @param key The key.
     * @return The value.
     */
    public HeaderElement get(String key)
    {
        HeaderElement p =  map.get(key);
        if (p == null) p = new HeaderElement("", "");
        return p;
    }

    /**
     * Returns if a given key exists in the header.
     * @param key The key name.
     * @return True or false.
     */
    public boolean keyExists(String key) {
    	return map.get(key) != null;
    }

    /**
     * Returns the tree map.
     * @return The tree map.
     */
    private TreeMap<String,HeaderElement> getMap()
    {
        return map;
    }

    private TreeMap<String,HeaderElement> map;
    private SpectrumHeader30m header;
    private float data[];

	private void writeObject(ObjectOutputStream out)
	throws IOException {
		out.writeObject(header);
		out.writeObject(data);
		out.writeInt(map.size());
		if (map.size() > 0) {
			Set<String> set = map.keySet();
			String keys[] = new String[map.size()];
			set.toArray(keys);
			out.writeObject(keys);
			for (int i=0; i<map.size(); i++) {
				HeaderElement p = map.get(keys[i]);
				out.writeObject(p);
			}
		}
	}
	private void readObject(ObjectInputStream in)
	throws IOException, ClassNotFoundException {
		header = (SpectrumHeader30m) in.readObject();
		Object o = in.readObject();
		if (o.getClass().getComponentType() == float.class) {
			data = (float[]) o;
		} else {
			try {
				data = DataSet.toFloatArray((double[]) o);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
        map = new TreeMap<String,HeaderElement>();
        int n = in.readInt();
        if (n > 0) {
        	String keys[] = (String[]) in.readObject();
	        for (int i=0; i<n; i++) {
	        	HeaderElement p = (HeaderElement) in.readObject();
	        	map.put(keys[i], p);
	        }
        }
 	}

    /**
     * Returns the tree map.
     * @return The tree map.
     */
    public TreeMap<String,HeaderElement> getTreeMap()
    {
    	return (TreeMap<String,HeaderElement>) map.clone();
    }
    /**
     * Returns the header.
     * @return The header.
     */
    public SpectrumHeader30m getHeader()
    {
    	return header.clone();
    }
    /**
     * Returns the visible header as 8 parameters, i.e., those fields that
     * are visible in GILDAS when a spectrum is drawn.
     * Values from index 0 are obs num (0), obs version (1),
     * source name (2), line name (3), telescope (4), offset RA
     * (5, in arcsec), offset DEC (6, in arcsec), scan number (7).
     * @return The 8 main header values.
     */
    public HeaderElement[] getVisibleHeader()
    {
    	return header.getVisibleHeader();
    }

    /**
     * Returns the spectrum data as an array, with the
     * main beam temperature as function of channel
     * number. Be careful: first channel number
     * is 1, while first data index is always 0.
     * @return The spectrum data.
     */
    public float[] getSpectrumData()
    {
    	return data.clone();
    }

	/**
	 * Returns this spectrum as a Table object in 1d.
	 * @return The table object.
	 */
	public Table getAsTable() {
		MeasureElement m[] = new MeasureElement[data.length];
		for (int i=0; i<m.length; i++) {
			m[i] = new MeasureElement(data[i], 0, MeasureElement.UNIT_Y_K);
		}
		return new Table(m);
	}

    /**
     * Returns the number of channels in this spectrum.
     * @return The number of channels.
     */
    public int getNumberOfChannels()
    {
    	return data.length;
    }

    /**
     * Sets the detailed map of parameters.
     * @param map The new map.
     */
    public void setTreeMap(TreeMap<String,HeaderElement> map)
    {
    	this.map = (TreeMap) map.clone();
    }
    /**
     * Sets the header.
     * @param header The new header.
     */
    public void setHeader(SpectrumHeader30m header)
    {
    	this.header = header.clone();
    }
    /**
     * Sets the spectrum data to a given array. Be careful: first channel number
     * is 1, while first data index is always 0.
     * @param data New array of data.
     */
    public void setSpectrumData(float data[])
    {
    	this.data = data;
    }

    /**
     * Returns the list of keys in this spectrum.
     * @return List of keys.
     */
    public String[] getKeys()
    {
    	TreeMap<String,HeaderElement> tm = this.getMap();
    	Object keys[] = tm.keySet().toArray();
    	String k[] = new String[keys.length];
    	for (int i=0; i<k.length; i++)
    	{
    		k[i] = (String) keys[i];
    	}
    	return k;
    }

    /**
     * Return the list of parameters in this spectrum.
     * @return List of parameters.
     */
    public HeaderElement[] getValues()
    {
    	TreeMap <String,HeaderElement>tm = this.getMap();
    	Object keys[] = tm.keySet().toArray();
    	HeaderElement p[] = new HeaderElement[keys.length];
    	for (int i=0; i<keys.length; i++)
    	{
    		p[i] = tm.get(keys[i]);
    	}
    	return p;
    }

    /**
     * The set of units for x axis.
     */
    public enum XUNIT {
	    /** ID constant to show spectrum chart in units of km/s.
	     * This velocity is the same given by Gildas, which is
	     * 'classical', without relativistic correction,
	     * and could be inaccurate in very wide spectra. */
	    VELOCITY_KMS,
	    /** ID constant to show spectrum chart in units channel number. */
	    CHANNEL_NUMBER,
	    /** ID constant to show spectrum chart in units of MHz, as given
	     * by Gildas. */
	    FREQUENCY_MHZ,
	    /** ID constant to show spectrum chart in units of km/s.
	     * This velocity is corrected considering the relativistic
	     * effects, important in very wide spectra. */
	    VELOCITY_KMS_CORRECTED,
    }

    /**
     * Returns the non corrected (Gildas) velocity for a given channel.
     * @param channel The channel in Gildas conventions, starting from 1 (first).
     * @return Velocity in km/s.
     */
    public double getVelocity(double channel) {
    	double refchan = 0.0, vref = 0.0, vres = 0.0;
    	try {
 	    	refchan = (this.get(Gildas30m.REF_CHAN)).getAsDouble();
	    	vref = (this.get(Gildas30m.REF_VEL)).getAsDouble();
	    	vres = (this.get(Gildas30m.VEL_RESOL)).getAsDouble();
    	} catch (Exception exc) {
    		if (Logger.reportJPARSECLogs) Logger.log(LEVEL.ERROR, "could not recover reference channel, velocity, and/or velocity resolution!");
    	}
		double v = vref + (channel - refchan) * vres;
		return v;
    }

    /**
     * Returns the corrected (non Gildas) velocity for a given channel.
     * @param channel The channel in Gildas conventions, starting from 1 (first).
     * @return Corrected velocity in km/s.
     * @throws JPARSECException If the fields required to compute the return are not numbers.
     */
    public double getCorrectedVelocity(double channel) throws JPARSECException {
    	return this.getCorrectedVelocityForAGivenFrequency(this.getFrequency(channel));
    }

    /**
     * Returns the channel number for a given velocity. Be careful: first channel number
     * is 1, while first data index is always 0.
     * @param v Velocity in km/s (non corrected, as given by Gildas).
     * @return Channel number.
     */
    public double getChannel(double v) {
    	double refchan = 0.0, vref = 0.0, vres = 0.0;
    	try {
 	    	refchan = (this.get(Gildas30m.REF_CHAN)).getAsDouble();
	    	vref = (this.get(Gildas30m.REF_VEL)).getAsDouble();
	    	vres = (this.get(Gildas30m.VEL_RESOL)).getAsDouble();
    	} catch (Exception exc) {
    		if (Logger.reportJPARSECLogs) Logger.log(LEVEL.ERROR, "could not recover reference channel, velocity, and/or velocity resolution!");
    	}
		double channel = (v - vref) / vres + refchan;
		return channel;
    }

    /**
     * Returns the resolution in velocity.
     * @return Resolution in km/s.
     * @throws JPARSECException If the value is not a number.
     */
    public double getVelocityResolution() throws JPARSECException {
    	return (this.get(Gildas30m.VEL_RESOL)).getAsDouble();
    }

    /**
     * Returns the resolution in frequency. Be careful since
     * this parameter is not set in all 30m spectra I've seen ...
     * @throws JPARSECException If the value is not a number.
     * @return Resolution in MHz.
     */
    public double getFrequencyResolution() throws JPARSECException {
    	return (this.get(Gildas30m.FREQ_RESOL)).getAsDouble();
    }

    /**
     * Returns the reference velocity.
     * @return Reference velocity in km/s.
     * @throws JPARSECException If the value is not a number.
     */
    public double getReferenceVelocity() throws JPARSECException {
    	return (this.get(Gildas30m.REF_VEL)).getAsDouble();
    }

    /**
     * Returns the reference channel. It follows Gildas conventions, where
     * value 1 is the first channel.
     * @return Reference channel number.
     * @throws JPARSECException If the value is not a number.
     */
    public double getReferenceChannel() throws JPARSECException {
    	return (this.get(Gildas30m.REF_CHAN)).getAsDouble();
    }

    /**
     * Returns the frequency for a given channel.
     * @param channel The channel, first is 1.
     * @return Frequency in MHz.
     * @throws JPARSECException If the fields required to compute the return are not numbers.
     */
    public double getFrequency(double channel) throws JPARSECException {
    	double v = this.getVelocity(channel);
		return this.getFrequencyForAGivenVelocity(v);
    }

    /**
     * Returns the image frequency for a given channel.
     * @param channel The channel, first is 1.
     * @return Frequency in MHz.
     * @throws JPARSECException If the fields required to compute the return are not numbers.
     */
    public double getImageFrequency(double channel) throws JPARSECException {
    	double v = this.getVelocity(channel);
		return this.getImageFrequencyForAGivenVelocity(v);
    }

    /**
     * Returns the frequency for a given velocity.
     * @param velocity Velocity in km/s.
     * @return Frequency in MHz.
     * @throws JPARSECException If the fields required to compute the return are not numbers.
     */
    public double getFrequencyForAGivenVelocity(double velocity) throws JPARSECException {
    	double vref = (this.get(Gildas30m.REF_VEL)).getAsDouble();
	    double vres = (this.get(Gildas30m.VEL_RESOL)).getAsDouble();
    	double delta = (velocity - vref) / vres;
    	double fref = (this.get(Gildas30m.REF_FREQ)).getAsDouble();
    	double fres = - vres * fref / (Constant.SPEED_OF_LIGHT * 0.001);
    	double freq = fref + delta * fres;

		return freq;
    }

    /**
     * Returns the frequency for a given corrected velocity, not the velocity
     * given by Gildas .
     * @param velocity Correct velocity in km/s.
     * @return Frequency in MHz.
     * @throws JPARSECException If the fields required to compute the return are not numbers.
     */
    public double getFrequencyForAGivenCorrectedVelocity(double velocity) throws JPARSECException {
    	double vref = (this.get(Gildas30m.REF_VEL)).getAsDouble();
    	double rfreq = (this.get(Gildas30m.REF_FREQ)).getAsDouble();
		double freq = rfreq * (1.0 / (1.0 - (vref - velocity) * 1000.0 / Constant.SPEED_OF_LIGHT));
		return freq;
    }

    /**
     * Returns the image frequency for a given velocity.
     * @param velocity Non corrected (Gildas) velocity in km/s.
     * @return Frequency in MHz.
     * @throws JPARSECException If the fields required to compute the return are not numbers.
     */
    public double getImageFrequencyForAGivenVelocity(double velocity) throws JPARSECException {
    	double vref = (this.get(Gildas30m.REF_VEL)).getAsDouble();
	    double vres = (this.get(Gildas30m.VEL_RESOL)).getAsDouble();
       	double imgf = (this.get(Gildas30m.IMAGE)).getAsDouble();
       	if (imgf == 0) return 0;

    	double delta = (velocity - vref) / vres;
    	double fref = (this.get(Gildas30m.REF_FREQ)).getAsDouble();
    	double fres = - vres * fref / (Constant.SPEED_OF_LIGHT * 0.001);
    	double freq = imgf - delta * fres;

		return freq;
    }

    /**
     * Returns the image frequency for a given corrected velocity.
     * @param velocity Corrected (non Gildas) velocity in km/s.
     * @return Frequency in MHz.
     * @throws JPARSECException If the fields required to compute the return are not numbers.
     */
    public double getImageFrequencyForAGivenCorrectedVelocity(double velocity) throws JPARSECException {
    	return this.getImageFrequencyForAGivenVelocity(this.getVelocityForAGivenFrequency(this.getFrequencyForAGivenCorrectedVelocity(velocity)));
    }

    /**
     * Returns the non corrected velocity for a given frequency.
     * @param frequency Frequency in MHz.
     * @return Non corrected velocity in km/s.
     * @throws JPARSECException If the fields required to compute the return are not numbers.
     */
    public double getVelocityForAGivenFrequency(double frequency) throws JPARSECException {
    	double vref = (this.get(Gildas30m.REF_VEL)).getAsDouble();
	    double vres = (this.get(Gildas30m.VEL_RESOL)).getAsDouble();
    	double fref = (this.get(Gildas30m.REF_FREQ)).getAsDouble();
    	double fres = - vres * fref / (Constant.SPEED_OF_LIGHT * 0.001);
    	double delta = (frequency - fref) / fres;
    	double vel = vref + delta * vres;
		return vel;
    }

    /**
     * Returns the width of the channel for a given frequency, corrected for
     * the change in the width with respect to the reference frequency in
     * very wide spectra.
     * @param frequency The frequency in MHz.
     * @return Channel width in km/s. Actually it is the corrected velocity
     * resolution, which could be a negative value.
     * @throws JPARSECException If the fields required to compute the return are not numbers.
     */
    public double getChannelWidth(double frequency) throws JPARSECException {
	    double vres = (this.get(Gildas30m.VEL_RESOL)).getAsDouble();
    	double fref = (this.get(Gildas30m.REF_FREQ)).getAsDouble();
    	double fres = - vres * fref / (Constant.SPEED_OF_LIGHT * 0.001);

    	double v0 = getCorrectedVelocityForAGivenFrequency(frequency);
    	double v1 = getCorrectedVelocityForAGivenFrequency(frequency + fres);
    	return Math.abs(v1 - v0) * FastMath.sign(vres);
    }

    /**
     * Returns the correct velocity for a given frequency.
     * @param frequency Frequency in MHz.
     * @return Correct velocity in km/s, could differ substantially from that of Gildas.
     * @throws JPARSECException If the fields required to compute the return are not numbers.
     */
    public double getCorrectedVelocityForAGivenFrequency(double frequency) throws JPARSECException {
    	double vref = (this.get(Gildas30m.REF_VEL)).getAsDouble();
    	double rfreq = (this.get(Gildas30m.REF_FREQ)).getAsDouble();
		double vel = vref - (1.0 - rfreq /  frequency) * Constant.SPEED_OF_LIGHT / 1000.0;
		return vel;
    }

    /**
     * Returns the correct velocity for a given Gildas velocity.
     * @param velocity Gildas velocity in km/s.
     * @return Correct velocity in km/s, could differ substantially from that of Gildas.
     * @throws JPARSECException If the fields required to compute the return are not numbers.
     */
    public double getCorrectedVelocityForAGivenGildasVelocity(double velocity) throws JPARSECException {
    	return this.getCorrectedVelocityForAGivenFrequency(this.getFrequencyForAGivenVelocity(velocity));
    }

    /**
     * Returns the Gildas velocity for a given corrected velocity.
     * @param correctedVelocity Corrected velocity in km/s.
     * @return Gildas velocity in km/s.
     * @throws JPARSECException If the fields required to compute the return are not numbers.
     */
    public double getGildasVelocityForAGivenCorrectedVelocity(double correctedVelocity) throws JPARSECException {
    	return this.getVelocityForAGivenFrequency(this.getFrequencyForAGivenCorrectedVelocity(correctedVelocity));
    }

	/**
	 * Transforms the parameters of this line from Gildas velocity to corrected velocity
	 * (corrected for relativistic effects so that the width of the channel changes with
	 * frequency). Usually the line parameters are referred initially to Gildas velocity,
	 * since Gildas is the source of the spectrum. Note an improper call could result in
	 * an instance inadequate to represent the fit to a given line, so use this call with
	 * care. Only velocity, velocity width, and area are changed.
	 * @param line The input line.
	 * @return The output line.
     * @throws JPARSECException If the fields required to compute the return are not numbers.
	 */
	public SpectrumLine toCorrectedVelocity(SpectrumLine line) throws JPARSECException {
    	double vres = (this.get(Gildas30m.VEL_RESOL)).getAsDouble();

		SpectrumLine out = line.clone();
		out.vel = this.getCorrectedVelocityForAGivenGildasVelocity(line.vel);
		double freq = this.getFrequencyForAGivenVelocity(line.vel);
		double factor = getChannelWidth(freq) / vres;
		out.width *= factor;
		out.widthError *= factor;
		out.velError *= factor;

		MeasureElement width = new MeasureElement(out.width, out.widthError, "");
		MeasureElement peak = new MeasureElement(out.peakT, out.peakTError, "");
		width.multiply(peak);
		out.area = width.getValue() * 1.064467;
		out.areaError = width.error * 1.064467;

		return out;
	}

	/**
	 * Transforms the parameters of this line from corrected velocity to Gildas velocity
	 * (not corrected for relativistic effects). Usually the line parameters are referred
	 * initially to Gildas velocity, since Gildas is the source of the spectrum. Note an
	 * improper call could result in an instance inadequate to represent the fit to a given
	 * line, so use this call with care. Only velocity, velocity width, and area are changed.
	 * @param line The input line.
	 * @return The output line.
     * @throws JPARSECException If the fields required to compute the return are not numbers.
	 */
	public SpectrumLine toGildasVelocity(SpectrumLine line) throws JPARSECException {
    	double vres = (this.get(Gildas30m.VEL_RESOL)).getAsDouble();

		SpectrumLine out = line.clone();
		out.vel = this.getGildasVelocityForAGivenCorrectedVelocity(line.vel);
		double freq = this.getFrequencyForAGivenCorrectedVelocity(line.vel);
		double factor = getChannelWidth(freq) / vres;
		out.width /= factor;
		out.widthError /= factor;
		out.velError /= factor;

		MeasureElement width = new MeasureElement(out.width, out.widthError, "");
		MeasureElement peak = new MeasureElement(out.peakT, out.peakTError, "");
		width.multiply(peak);
		out.area = width.getValue() * 1.064467;
		out.areaError = width.error * 1.064467;

		return out;
	}

    /**
     * Returns the Gildas velocity for a given image frequency.
     * @param frequency Image frequency in MHz.
     * @return Velocity in km/s.
     * @throws JPARSECException If the fields required to compute the return are not numbers.
     */
    public double getVelocityForAGivenImageFrequency(double frequency) throws JPARSECException {
    	double vref = (this.get(Gildas30m.REF_VEL)).getAsDouble();
	    double vres = (this.get(Gildas30m.VEL_RESOL)).getAsDouble();
       	double imgf = (this.get(Gildas30m.IMAGE)).getAsDouble();
       	double fref = (this.get(Gildas30m.REF_FREQ)).getAsDouble();
    	double fres = - vres * fref / (Constant.SPEED_OF_LIGHT * 0.001);
    	double delta = (frequency - (imgf - fref) - fref) / fres;
    	double vel = vref - delta * vres;
		return vel;
    }

    /**
     * Returns the corrected (non Gildas) velocity for a given image frequency.
     * @param frequency Image frequency in MHz.
     * @return Corrected velocity in km/s.
     * @throws JPARSECException If the fields required to compute the return are not numbers.
     */
    public double getCorrectedVelocityForAGivenImageFrequency(double frequency) throws JPARSECException {
    	return this.getCorrectedVelocityForAGivenFrequency(this.getFrequencyForAGivenVelocity(this.getVelocityForAGivenImageFrequency(frequency)));
    }

    /**
     * Returns the velocity for a given frequency as Gildas does, without considering
     * relativistic corrections, and using a new rest frequency.
     * @param frequency Frequency in MHz.
     * @param fref The new rest frequency to compute a new velocity scale, in MHz.
     * @return Vlsr in km/s.
     * @throws JPARSECException If the fields required to compute the return are not numbers.
     */
    public double getVlsrForAGivenFrequency(double frequency, double fref) throws JPARSECException {
    	Spectrum30m copy = this.clone();
    	copy.modifyRestFrequency(fref);
    	return copy.getVelocityForAGivenFrequency(frequency);
    }

    /**
     * Returns the reference image frequency of this spectrum.
     * @return Image frequency in MHz, or 0.0 if that value cannot be retrieved
     * from the current spectrum.
     */
    public double getReferenceImageFrequency() {
       	double imgf = 0.0;
       	try {
       		imgf = this.get(Gildas30m.IMAGE).getAsDouble();
       	} catch (Exception exc) {}
       	return imgf;
    }

    /**
     * Returns the reference frequency of this spectrum.
     * @return Reference frequency in MHz, or 0.0 if that value cannot be retrieved
     * from the current spectrum.
     */
    public double getReferenceFrequency() {
       	double fref = 0.0;
       	try {
       		fref = (this.get(Gildas30m.REF_FREQ)).getAsDouble();
       	} catch (Exception exc) {}
       	return fref;
    }

    /**
     * Return the spectrum as a table, containing channel number, Gildas velocity (km/s),
     * frequency (MHz), and intensity. Each line is separated by a line separator.
     * @return The text of the table.
     * @throws JPARSECException If the fields required to compute the return are not numbers.
     */
    public String getSpectrumAsTable() throws JPARSECException {
    	float y[] = this.getSpectrumData();

    	double vref = (this.get(Gildas30m.REF_VEL)).getAsDouble();
	    double vres = (this.get(Gildas30m.VEL_RESOL)).getAsDouble();
       	double rfreq = (this.get(Gildas30m.REF_FREQ)).getAsDouble();
       	double refchan = (this.get(Gildas30m.REF_CHAN)).getAsDouble();

    	double channel[] = new double[y.length];
    	double vel[] = new double[y.length];
    	double freq[] = new double[y.length];
    	String out = "";
    	for (int i=0; i<y.length; i++)
    	{
    		channel[i] = i+1;
        	vel[i] = vref + (i - refchan) * vres;
        	freq[i] = rfreq * (1.0 / (1.0 - (vref - vel[i]) * 1000.0 / Constant.SPEED_OF_LIGHT));
        	out += channel[i]+"   "+vel[i]+"   "+freq[i]+"   "+y[i]+FileIO.getLineSeparator();
    	}
    	return out;
    }
    /**
     * Returns a chart with certain spectrum.
     * @param width Chart width in pixels.
     * @param height Chart height in pixels.
     * @param xUnit The unit for the x axis.
     * @return The chart.
     * @throws JPARSECException If an error occurs.
     */
    public CreateChart getChart(int width, int height, XUNIT xUnit)
    throws JPARSECException {
    	double y[] = DataSet.toDoubleArray(this.getSpectrumData());
    	double x[] = new double[y.length];

    	for (int i=0; i<y.length; i++)
    	{
    		switch (xUnit)
    		{
    		case CHANNEL_NUMBER:
    			x[i] = i + 1;
    			break;
    		case VELOCITY_KMS:
        		x[i] = this.getVelocity(i+1);
        		break;
    		case FREQUENCY_MHZ:
    			x[i] = this.getFrequency(i+1);
    			break;
    		case VELOCITY_KMS_CORRECTED:
    			x[i] = this.getCorrectedVelocity(i+1);
    			break;
    		default:
    			throw new JPARSECException("invalid value for x axis units.");
    		}
    	}

    	SpectrumHeader30m sh = this.getHeader();
    	HeaderElement header[] = sh.getHeaderParameters();
    	String title = (this.get(Gildas30m.SOURCE)).getAsString().trim();
    	title += " ("+header[0].getAsString()+')';
    	String legend = header[4].getAsString();
		SimpleChartElement chart1 = new SimpleChartElement(ChartElement.TYPE.XY_CHART,
				ChartElement.SUBTYPE.XY_SCATTER, x, y, title, Translate.translate(292)+" (km s^{-1})", "T_{mb} (K)", legend, true, false,
				width, height);
		switch (xUnit)
		{
		case CHANNEL_NUMBER:
			chart1.xLabel = Translate.translate(293);
			break;
		case FREQUENCY_MHZ:
			chart1.xLabel = Translate.translate(294)+" (MHz)";
			break;
		default:
			break;
		}

		ChartElement chart = ChartElement.parseSimpleChartElement(chart1);
		chart.series[0].showLines = true;
		chart.series[0].showShapes = false;
		chart.series[0].showErrorBars = false;
		chart.series[0].stroke = JPARSECStroke.STROKE_DEFAULT_LINE_THIN;
		chart.showErrorBars = false;
		CreateChart ch = new CreateChart(chart);
		return ch;
    }

    /**
     * Returns a chart with this spectrum including a set of Gaussians.
     * Only valid lines are drawn into the chart (lines enabled and not deleted in their
     * corresponding internal flags).
     * @param lines The set of Gaussians.
     * @param xunit The unit to use in the chart for the x axis.
     * @return The chart.
     * @throws JPARSECException If an error occurs.
     */
	public CreateChart getChartWithLines(SpectrumLine lines[], XUNIT xunit) throws JPARSECException {
		CreateChart ch = getChart(800, 600, xunit);
		if (lines != null && lines.length > 0) {
			int index = 0;
			for (int l=0;l<lines.length; l++) {
				if (!lines[l].deleted && lines[l].enabled) {
					index ++;
					Spectrum30m s30m2 = new Spectrum30m(Spectrum.getGaussianSpectrum(200, lines[l]));
					if (xunit == XUNIT.CHANNEL_NUMBER)  s30m2.resample(this);
					ChartSeriesElement series = s30m2.getChart(800, 600, xunit).getChartElement().series[0];
					series.legend = lines[l].label;
					if (series.legend == null || series.legend.trim().equals(""))
						series.legend = "Line "+index;
					series.color = Color.RED;
					ch.addSeries(series);
				}
			}
			ch.updateChart();
		}
		return ch;
	}
	
    /**
     * Returns a chart with this spectrum including a set of Gaussians.
     * Invalid lines are also drawn into the chart (with stroke).
     * @param lines The set of Gaussians.
     * @param xunit The unit to use in the chart for the x axis.
     * @return The chart.
     * @throws JPARSECException If an error occurs.
     */
	public CreateChart getChartWithAllLines(SpectrumLine lines[], XUNIT xunit) throws JPARSECException {
		CreateChart ch = getChart(800, 600, xunit);
		if (lines != null && lines.length > 0) {
			int index = 0;
			for (int l=0;l<lines.length; l++) {
				index ++;
				Spectrum30m s30m2 = new Spectrum30m(Spectrum.getGaussianSpectrum(200, lines[l]));
				if (xunit == XUNIT.CHANNEL_NUMBER)  s30m2.resample(this);
				ChartSeriesElement series = s30m2.getChart(800, 600, xunit).getChartElement().series[0];
				series.legend = lines[l].label;
				if (series.legend == null || series.legend.trim().equals(""))
					series.legend = "Line "+index;
				series.color = Color.RED;
				if (lines[l].deleted || !lines[l].enabled) series.stroke = JPARSECStroke.STROKE_LINES_MEDIUM;
				ch.addSeries(series);
			}
			ch.updateChart();
		}
		return ch;
	}

    /**
     * Clones this instance.
     */
	@Override
    public Spectrum30m clone()
    {
    	Spectrum30m s = new Spectrum30m((TreeMap<String, HeaderElement>) this.getMap().clone(), this.getHeader().clone(), this.getSpectrumData().clone());
    	return s;
    }
    /**
     * Checks if this instance is equals to another.
     */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Spectrum30m)) return false;

		Spectrum30m that = (Spectrum30m) o;

		if (map != null ? !map.equals(that.map) : that.map != null) return false;
		if (header != null ? !header.equals(that.header) : that.header != null) return false;

		return Arrays.equals(data, that.data);
	}

	@Override
	public int hashCode() {
		int result = map != null ? map.hashCode() : 0;
		result = 31 * result + (header != null ? header.hashCode() : 0);
		result = 31 * result + (data != null ? Arrays.hashCode(data) : 0);
		return result;
	}

    /**
     * Returns the current 30m spectrum as a general spectrum object.
     * @param xUnit The unit for the x axis. Y axis will be main beam temperature.
     * @return The spectrum.
     * @throws JPARSECException If an error occurs.
     */
    public Spectrum getAsSpectrum(XUNIT xUnit)
    throws JPARSECException {
    	float y[] = this.getSpectrumData();
    	double x[] = new double[y.length];

       	String xu = "";
    	for (int i=0; i<y.length; i++)
    	{
    		switch (xUnit)
    		{
    		case CHANNEL_NUMBER:
    			x[i] = i + 1;
    			xu = null;
    			break;
    		case VELOCITY_KMS:
        		x[i] = this.getVelocity(i + 1);
        		xu = MeasureElement.UNIT_X_KMS;
        		break;
    		case FREQUENCY_MHZ:
    			x[i] = this.getFrequency(i + 1);
        		xu = MeasureElement.UNIT_X_MHZ;
    			break;
    		case VELOCITY_KMS_CORRECTED:
    			x[i] = this.getCorrectedVelocity(i+1);
        		xu = MeasureElement.UNIT_X_KMS;
    			break;
    		default:
    			throw new JPARSECException("invalid value for x axis units.");
    		}
    	}

    	FluxElement f[] = new FluxElement[data.length];
		for (int i=0; i<data.length; i++)
		{
			String xval = ""+x[i];
			double dxval = 0.0;
			MeasureElement mx = new MeasureElement(xval, dxval, xu);

			String yval = ""+y[i];
			double dyval = 0.0;
			MeasureElement my = new MeasureElement(yval, dyval, MeasureElement.UNIT_Y_K);

			f[i] = new FluxElement(mx, my);
		}

		HeaderElement header[] = this.getHeader().getHeaderParameters();
		Spectrum s = new Spectrum(f);

		s.backend = header[SpectrumHeader30m.HEADER.TELES.ordinal()].getAsString();
		s.observationNumber = header[SpectrumHeader30m.HEADER.NUM.ordinal()].getAsInt();
		s.source = header[SpectrumHeader30m.HEADER.SOURCE.ordinal()].getAsString();
		s.line = header[SpectrumHeader30m.HEADER.LINE.ordinal()].getAsString();
		s.offsetX = header[SpectrumHeader30m.HEADER.OFFSET1.ordinal()].getAsDouble();
		s.offsetY = header[SpectrumHeader30m.HEADER.OFFSET2.ordinal()].getAsDouble();
		s.scanNumber = header[SpectrumHeader30m.HEADER.SCAN.ordinal()].getAsInt();
		GregorianCalendar date = GildasDataFormat.getDate(header[SpectrumHeader30m.HEADER.DATE_OBS.ordinal()].getAsInt());
		int coordType = header[SpectrumHeader30m.HEADER.TYPEC.ordinal()].getAsInt();

		s.beamEfficiency = this.get(Gildas30m.BEAM_EFF).getAsDouble();
		s.dec = this.get(Gildas30m.BETA).getAsDouble();
		s.epochJD = this.get(Gildas30m.EPOCH).getAsDouble();
		s.integrationTime = this.get(Gildas30m.INTEG).getAsDouble();
		s.observingTimeJD = this.get(Gildas30m.UT_TIME).getAsDouble();
		s.ra = this.get(Gildas30m.LAMBDA).getAsDouble();
		s.referenceChannel = this.get(Gildas30m.REF_CHAN).getAsDouble();
		s.referenceFrequency = this.get(Gildas30m.REF_FREQ).getAsDouble();
		s.referenceVelocity = this.get(Gildas30m.REF_VEL).getAsDouble();
		s.velocityResolution = this.get(Gildas30m.VEL_RESOL).getAsDouble();
		s.sigmaRMS = this.get(Gildas30m.BEAM_EFF).getAsDouble();
		s.imgFrequency = this.get(Gildas30m.IMAGE).getAsDouble();

		if (s.epochJD == 1950.0) {
			s.epochJD = Constant.B1950;
		} else {
			s.epochJD = (s.epochJD - 2000.0) * 365.25 + Constant.J2000;
		}

		AstroDate astro = new AstroDate(date);
		s.observingTimeJD = s.observingTimeJD * 24.0 / Constant.TWO_PI + astro.jd();

		if (coordType != Gildas30m.COORDINATES_EQUATORIAL) {
			if (coordType == Gildas30m.COORDINATES_GALACTIC) {
				LocationElement loc = CoordinateSystem.galacticToEquatorial(new LocationElement(s.ra, s.dec, 1.0), Constant.J2000, false);
				s.ra = loc.getLongitude();
				s.dec = loc.getLatitude();
			} else {
				throw new JPARSECException("source coordinates in the spectrum cannot be understood.");
			}
		}
    	return s;
    }

    /**
     * Constructor for a general spectrum. Some values are set for the 30m telescope site.
     * @param sp The spectrum instance.
     * @throws JPARSECException If an error occurs.
     */
    public Spectrum30m(Spectrum sp)
    throws JPARSECException {
    	String xs[] = sp.getXs(null), ys[] = sp.getYs(MeasureElement.UNIT_Y_K);

        double refch = sp.referenceChannel;
        double v0 = sp.referenceVelocity;
        double velres = sp.velocityResolution;
        double rfreq = sp.referenceFrequency;
        double imgFreq = sp.imgFrequency;

    	// Transform x values to channel number, if necessary
    	String xunit = sp.spectrum[0].x.unit;
    	if (xunit != null) {
	    	String newXs[] = xs.clone();
	    	double val[] = DataSet.toDoubleValues(xs);
	    	double fscale = 1;
	    	if (xunit.equals(MeasureElement.UNIT_X_HZ)) fscale = 1E-6;
	    	if (xunit.equals(MeasureElement.UNIT_X_GHZ)) fscale = 1E3;
	    	for (int i=0; i<xs.length; i++) {
	    		if (xunit.equals(MeasureElement.UNIT_X_KMS)) {
	   				double channel = (val[i] - v0) / velres + refch;
	   				newXs[i] = ""+channel;
	    		}
	    		if (xunit.equals(MeasureElement.UNIT_X_MHZ) ||
	    				xunit.equals(MeasureElement.UNIT_X_GHZ) ||
	    				xunit.equals(MeasureElement.UNIT_X_HZ)) {
    		    	double fres = - velres * rfreq / (Constant.SPEED_OF_LIGHT * 0.001);
    		    	double delta = (val[i] * fscale - rfreq) / fres;
    		    	double vel = v0 + delta * velres;
	   				double channel = (vel - v0) / velres + refch;
	   				newXs[i] = ""+channel;
	    		}
	    	}
	    	xs = newXs;
    	}

    	if (!DataSet.isSorted(true, DataSet.toDoubleValues(xs))) {
    		DataSet.reverse2(xs);
    		DataSet.reverse2(ys);
    	}
        ArrayList<Object> v = DataSet.sortInCrescent(xs, ys, null, null);
        this.data = DataSet.toFloatValues((String[]) v.get(1));

        // Check x values also and allow auto-resampling if resolution is not vres / x values are not integers
        double xval[] = DataSet.toDoubleValues((String[]) v.get(0));
        double xres = -1, xresOld = -1;
        boolean resample = false;
        String why = "first channel is not 1";
        if (xval[0] != 1) {
        	resample = true;
        } else {
	        why = "non-integer channels";
	    	if (xval[0] == (int) xval[0]) {
		        for (int i=1; i<xval.length; i++) {
		        	double dif = Math.abs(Math.abs(xval[i]) - (int) (Math.abs(xval[i]) + 0.5));
		        	if (dif > 1.0E-3 && (float)xval[i] != (int) xval[i]) {
		        		resample = true;
		        		break;
		        	}
		        	xres = xval[i] - xval[i-1];
		        	if (xresOld != -1 && (float)xresOld != (float)xres) {
		        		resample = true;
		        		why = "non-constant velocity resolution";
		        		break;
		        	}
		        	xresOld = xres;
		        }
	    	} else {
	    		resample = true;
	    	}
        }

    	if (resample) {
    		JPARSECException.addWarning("Input spectrum data will be resampled ("+why+") to convert it to Gildas");
    		int firstChannel = (int) Math.abs(xval[0]);
    		if (xval[0] >= 0) {
    			if (firstChannel != xval[0]) firstChannel ++;
    		} else {
    			firstChannel = -firstChannel;
    		}
    		int lastChannel = (int) Math.abs(xval[xval.length-1]);
    		if (xval[xval.length-1] < 0) lastChannel = -lastChannel - 1;
    		double newX[] = DataSet.getSetOfValues(firstChannel, lastChannel, lastChannel - firstChannel + 1, false);
    		float newY[] = new float[newX.length];
    		Interpolation interp = new Interpolation(xval, DataSet.toDoubleArray(data), false);
	        for (int i=0; i<newX.length; i++) {
	        	newY[i] = (float) interp.splineInterpolation(newX[i]);
	        }
			double vfirst = v0 + (firstChannel - refch) * velres;
			double vlast = v0 + (lastChannel - refch) * velres;
	        // update also ref. freq
	    	double delta = (vfirst - v0) / velres;
	    	double fres = - velres * rfreq / (Constant.SPEED_OF_LIGHT * 0.001);
	    	double freqFirst = rfreq + delta * fres;

	    	if (imgFreq != 0) imgFreq = imgFreq - delta * fres;
	        data = newY;
	        refch = 1;
	        v0 = vfirst;
	        velres = (vlast - vfirst) / (newX.length - 1);
	        rfreq = freqFirst;
    	}

        double ut, lst = 0.0, az= 0.0, el = 0.0, tau= 0.0, tsys = 0.0, integt, epoch;
        double lamb, beta, lamboff = 0.0, betaoff = 0.0, freqres;
        double freqoff = 0.0, bad = 0.0, beameff, foreff = 0.0;
        double gainim = 0.0, h2omm= 0.0, pamb = 0.0, tamb = 0.0, tatmsig = 0.0, tchop = 0.0;
        double tcold = 0.0, tausig = 0.0, tauima = 0.0, trec = 0.0, factor = 0.0, altitude = 2851.5;
        double lon = -0.05931949000000015, lat = 0.6469658600000003;
        double c1 = 0.0, c2 = 0.0, c3 = 0.0, sigma, swde = 0.0, swdu = 0.0, swp = 0.0;
        double swl = 0.0, swb = 0.0, todo = 0.0, doppler = 0;
        int projection = Gildas30m.PROJECTION_RADIO, nch = data.length, veltype = 1, mode = 1, otfn = 0;
        int otfh = 0, otfd = 0, otfdu = 0, nph = 0, swm = 0, version = 0, block = 0, kind = 0;
        String source, line, teles;

        integt = sp.integrationTime;
        epoch = 2000.0 + (sp.epochJD - Constant.J2000) / 365.25;
        lamb = sp.ra;
        beta = sp.dec;
        beameff = sp.beamEfficiency;
        sigma = sp.sigmaRMS;
        source = sp.source;
        line = sp.line;
        teles = sp.backend;
        if (source == null) source = "";
        if (line == null) line = "";
        if (teles == null) teles = "";
        ut = AstroDate.getDayFraction(sp.observingTimeJD) * Constant.TWO_PI;
        int quality = 0;
        freqres = - velres * rfreq / (Constant.SPEED_OF_LIGHT * 0.001);
        HeaderElement ldobs = new HeaderElement(Gildas30m.LDOBS, GildasDataFormat.getGILDASdate(sp.observingTimeJD), Gildas30m.LDOBS_DESC);

        map = new TreeMap<String,HeaderElement>();
        map.put(new String(Gildas30m.LDOBS), ldobs);
        map.put(new String(Gildas30m.LDRED), new HeaderElement(ldobs.getAsInt(), Gildas30m.LDRED_DESC));
        map.put(new String(Gildas30m.UT_TIME), new HeaderElement(ut, Gildas30m.UT_TIME_DESC));
        map.put(new String(Gildas30m.LST_TIME), new HeaderElement(lst, Gildas30m.LST_TIME_DESC));
        map.put(new String(Gildas30m.AZIMUTH), new HeaderElement(az, Gildas30m.AZIMUTH_DESC));
        map.put(new String(Gildas30m.ELEVATION), new HeaderElement(el, Gildas30m.ELEVATION_DESC));
        map.put(new String(Gildas30m.TAU), new HeaderElement(tau, Gildas30m.TAU_DESC));
        map.put(new String(Gildas30m.TSYS), new HeaderElement(tsys, Gildas30m.TSYS_DESC));
        map.put(new String(Gildas30m.INTEG), new HeaderElement(integt, Gildas30m.INTEG_DESC));
        map.put(new String(Gildas30m.SOURCE), new HeaderElement(source, Gildas30m.SOURCE_DESC));
        map.put(new String(Gildas30m.EPOCH), new HeaderElement(epoch, Gildas30m.EPOCH_DESC));
        map.put(new String(Gildas30m.LAMBDA), new HeaderElement(lamb, Gildas30m.LAMBDA_DESC));
        map.put(new String(Gildas30m.BETA), new HeaderElement(beta, Gildas30m.BETA_DESC));
        map.put(new String(Gildas30m.LAMBDA_OFF), new HeaderElement(lamboff, Gildas30m.LAMBDA_OFF_DESC));
        map.put(new String(Gildas30m.BETA_OFF), new HeaderElement(betaoff, Gildas30m.BETA_OFF_DESC));
        map.put(new String(Gildas30m.PROJECTION), new HeaderElement(projection, Gildas30m.PROJECTION_DESC));
        map.put(new String(Gildas30m.LINE), new HeaderElement(line, Gildas30m.LINE_DESC));
        map.put(new String(Gildas30m.TELES), new HeaderElement(teles, Gildas30m.LINE_DESC));
        map.put(new String(Gildas30m.REF_FREQ), new HeaderElement(rfreq, Gildas30m.REF_FREQ_DESC));
        map.put(new String(Gildas30m.NCHAN), new HeaderElement(nch, Gildas30m.NCHAN_DESC));
        map.put(new String(Gildas30m.REF_CHAN), new HeaderElement(refch, Gildas30m.REF_CHAN_DESC));
        map.put(new String(Gildas30m.FREQ_RESOL), new HeaderElement(freqres, Gildas30m.FREQ_RESOL_DESC));
        map.put(new String(Gildas30m.FREQ_OFF), new HeaderElement(freqoff, Gildas30m.FREQ_OFF_DESC));
        map.put(new String(Gildas30m.VEL_RESOL), new HeaderElement(velres, Gildas30m.VEL_RESOL_DESC));
        map.put(new String(Gildas30m.REF_VEL), new HeaderElement(v0, Gildas30m.REF_VEL_DESC));
        map.put(new String(Gildas30m.BAD), new HeaderElement(bad, Gildas30m.BAD_DESC));
        map.put(new String(Gildas30m.IMAGE), new HeaderElement(imgFreq, Gildas30m.IMAGE_DESC));
        map.put(new String(Gildas30m.VEL_TYPE), new HeaderElement(veltype, Gildas30m.VEL_TYPE_DESC));
        map.put(new String(Gildas30m.DOPPLER), new HeaderElement(doppler, Gildas30m.DOPPLER_DESC));
        map.put(new String(Gildas30m.BEAM_EFF), new HeaderElement(beameff, Gildas30m.BEAM_EFF_DESC));
        map.put(new String(Gildas30m.FORW_EFF), new HeaderElement(foreff, Gildas30m.FORW_EFF_DESC));
        map.put(new String(Gildas30m.GAIN_IM), new HeaderElement(gainim, Gildas30m.GAIN_IM_DESC));
        map.put(new String(Gildas30m.H2OMM), new HeaderElement(h2omm, Gildas30m.H2OMM_DESC));
        map.put(new String(Gildas30m.PAMB), new HeaderElement(pamb, Gildas30m.PAMB_DESC));
        map.put(new String(Gildas30m.TAMB), new HeaderElement(tamb, Gildas30m.TAMB_DESC));
        map.put(new String(Gildas30m.TATMSIG), new HeaderElement(tatmsig, Gildas30m.TATMSIG_DESC));
        map.put(new String(Gildas30m.TCHOP), new HeaderElement(tchop, Gildas30m.TCHOP_DESC));
        map.put(new String(Gildas30m.TCOLD), new HeaderElement(tcold, Gildas30m.TCOLD_DESC));
        map.put(new String(Gildas30m.TAUSIG), new HeaderElement(tausig, Gildas30m.TAUSIG_DESC));
        map.put(new String(Gildas30m.TAUIMA), new HeaderElement(tauima, Gildas30m.TAUIMA_DESC));
        map.put(new String(Gildas30m.TREC), new HeaderElement(trec, Gildas30m.TREC_DESC));
        map.put(new String(Gildas30m.MODE), new HeaderElement(mode, Gildas30m.MODE_DESC));
        map.put(new String(Gildas30m.FACTOR), new HeaderElement(factor, Gildas30m.FACTOR_DESC));
        map.put(new String(Gildas30m.ALTITUDE), new HeaderElement(altitude, Gildas30m.ALTITUDE_DESC));
        map.put(new String(Gildas30m.LON), new HeaderElement(lon, Gildas30m.LON_DESC));
        map.put(new String(Gildas30m.LAT), new HeaderElement(lat, Gildas30m.LAT_DESC));
        map.put(new String(Gildas30m.COUNT1), new HeaderElement(c1, Gildas30m.COUNT1_DESC));
        map.put(new String(Gildas30m.COUNT2), new HeaderElement(c2, Gildas30m.COUNT2_DESC));
        map.put(new String(Gildas30m.COUNT3), new HeaderElement(c3, Gildas30m.COUNT3_DESC));
        map.put(Gildas30m.OTF_NDUMPS, new HeaderElement(otfn, Gildas30m.OTF_NDUMPS_DESC));
        map.put(new String(Gildas30m.OTF_LEN_HEADER), new HeaderElement(otfh, Gildas30m.OTF_LEN_HEADER_DESC));
        map.put(new String(Gildas30m.OTF_LEN_DATA), new HeaderElement(otfd, Gildas30m.OTF_LEN_DATA_DESC));
        map.put(new String(Gildas30m.OTF_LEN_DUMP), new HeaderElement(otfdu, Gildas30m.OTF_LEN_DUMP_DESC));
        map.put(new String(Gildas30m.LAMBDA_OFF), new HeaderElement(sp.offsetX * Constant.ARCSEC_TO_RAD, Gildas30m.LAMBDA_OFF_DESC));
        map.put(new String(Gildas30m.BETA_OFF), new HeaderElement(sp.offsetY * Constant.ARCSEC_TO_RAD, Gildas30m.BETA_OFF_DESC));
        map.put(Gildas30m.OFF1, new HeaderElement(sp.offsetX * Constant.ARCSEC_TO_RAD, Gildas30m.OFF1_DESC));
        map.put(Gildas30m.OFF2, new HeaderElement(sp.offsetY * Constant.ARCSEC_TO_RAD, Gildas30m.OFF2_DESC));
        map.put(Gildas30m.LAMBDA_OFF, new HeaderElement(sp.offsetX * Constant.ARCSEC_TO_RAD, Gildas30m.LAMBDA_OFF_DESC));
        map.put(Gildas30m.BETA_OFF, new HeaderElement(sp.offsetY * Constant.ARCSEC_TO_RAD, Gildas30m.BETA_OFF_DESC));
        map.put(Gildas30m.NPHASE, new HeaderElement(nph, Gildas30m.NPHASE_DESC));
        map.put(Gildas30m.SWMODE, new HeaderElement(swm, Gildas30m.SWMODE_DESC));
        map.put(Gildas30m.TYPEC, new HeaderElement(Gildas30m.COORDINATES_EQUATORIAL, Gildas30m.TYPEC_DESC));
        for(int i1 = 0; i1 < nph; i1++)
        {
            map.put(new String((new StringBuilder()).append(Gildas30m.SWDECALAGE).append(i1).toString()), new HeaderElement(swde, Gildas30m.SWDECALAGE_DESC));
            map.put(new String((new StringBuilder()).append(Gildas30m.SWDURATION).append(i1).toString()), new HeaderElement(swdu, Gildas30m.SWDURATION_DESC));
            map.put(new String((new StringBuilder()).append(Gildas30m.SWPOIDS).append(i1).toString()), new HeaderElement(swp, Gildas30m.SWPOIDS_DESC));
            map.put(new String((new StringBuilder()).append(Gildas30m.SWLDECAL).append(i1).toString()), new HeaderElement(swl, Gildas30m.SWLDECAL_DESC));
            map.put(new String((new StringBuilder()).append(Gildas30m.SWBDECAL).append(i1).toString()), new HeaderElement(swb, Gildas30m.SWBDECAL_DESC));
        }
        map.put(Gildas30m.SIGMA, new HeaderElement(sigma, Gildas30m.SIGMA_DESC));

        header = new SpectrumHeader30m(new HeaderElement[] {
        		new HeaderElement(Gildas30m.NUM, sp.observationNumber, Gildas30m.NUM_DESC),
           		new HeaderElement(Gildas30m.BLOCK, block, Gildas30m.BLOCK_DESC),
           		new HeaderElement(Gildas30m.VERSION, version, Gildas30m.VERSION_DESC),
           		new HeaderElement(Gildas30m.SOURCE, sp.source, Gildas30m.SOURCE_DESC),
           		new HeaderElement(Gildas30m.LINE, sp.line, Gildas30m.LINE_DESC),
           		new HeaderElement(Gildas30m.TELES, sp.backend, Gildas30m.TELES_DESC),
           		ldobs,
           		new HeaderElement(Gildas30m.LDRED, GildasDataFormat.getGILDASdate(sp.observingTimeJD), Gildas30m.LDRED_DESC),
           		new HeaderElement(Gildas30m.OFF2, sp.offsetX * Constant.ARCSEC_TO_RAD, Gildas30m.OFF1_DESC),
           		new HeaderElement(Gildas30m.OFF1, sp.offsetY * Constant.ARCSEC_TO_RAD, Gildas30m.OFF2_DESC),
           		new HeaderElement(Gildas30m.TYPEC, Gildas30m.COORDINATES_EQUATORIAL, Gildas30m.TYPEC_DESC),
           		new HeaderElement(Gildas30m.KIND, kind, Gildas30m.KIND_DESC),
           		new HeaderElement(Gildas30m.QUALITY, quality, Gildas30m.QUALITY_DESC),
           		new HeaderElement(Gildas30m.SCAN, sp.scanNumber, Gildas30m.SCAN_DESC),
           		new HeaderElement(Gildas30m.POSA, todo, Gildas30m.POSA_DESC),
        });
    }

    /**
     * Writes the data in the .30m file.
     * @param path Path to the output file.
     * @throws JPARSECException If an error occurs.
     */
    public void writeAs30m(String path)
    throws JPARSECException {
    	Spectrum30m.writeAs30m(new Spectrum30m[] {this}, path);
    }

    private String fixBlank(String in) {
    	if (in == null) return in;
    	if (in.equals("")) return "0";
    	return in;
    }

	/**
	 * Writes the current spectra to a .fits file.
	 * The spectrum can be written in standard 64 bits/pixel (double), or in
	 * a compatible format with GILDAS, limited to 32 bits/pixel (float).
	 * The reduction from 64 to 32 bits/pixel will cause a minimum lose in precision.
	 * @param path The path to the file.
	 * @param forGILDAS True to write the spectrum in a compatible way with GILDAS.
	 * @throws JPARSECException If an error occurs.
	 */
	public void writeAsFITS(String path, boolean forGILDAS)
	throws JPARSECException {
		String ut =  this.get(Gildas30m.UT_TIME).getAsString();
		HeaderElement headerParams[] = this.getHeader().getHeaderParameters();
		AstroDate astro = new AstroDate(GildasDataFormat.getDate(HeaderElement.getByKey(headerParams, Gildas30m.LDOBS).getAsInt()));
		astro.setDayFraction(Double.parseDouble(ut) / Constant.TWO_PI);
		String dateObs = astro.getYear()+"-"+DateTimeOps.twoDigits(astro.getMonth())+"-"+DateTimeOps.twoDigits(astro.getDay())+"T"+DateTimeOps.twoDigits(astro.getHour())+":"+DateTimeOps.twoDigits(astro.getMinute())+":"+DateTimeOps.twoDigits((float)astro.getSeconds());
		ut = DateTimeOps.twoDigits(astro.getHour())+":"+DateTimeOps.twoDigits(astro.getMinute())+":"+DateTimeOps.twoDigits((float)astro.getSeconds());
		astro = new AstroDate(GildasDataFormat.getDate(HeaderElement.getByKey(headerParams, Gildas30m.LDRED).getAsInt()));
		String dateRed = astro.getYear()+"-"+DateTimeOps.twoDigits(astro.getMonth())+"-"+DateTimeOps.twoDigits(astro.getDay())+"T"+DateTimeOps.twoDigits(astro.getHour())+":"+DateTimeOps.twoDigits(astro.getMinute())+":"+DateTimeOps.twoDigits((float)astro.getSeconds());
		astro = new AstroDate();
		String date = astro.getYear()+"-"+DateTimeOps.twoDigits(astro.getMonth())+"-"+DateTimeOps.twoDigits(astro.getDay())+"T"+DateTimeOps.twoDigits(astro.getHour())+":"+DateTimeOps.twoDigits(astro.getMinute())+":"+DateTimeOps.twoDigits((float)astro.getSeconds());
		String lst =  this.get(Gildas30m.LST_TIME).getAsString();
		astro.setDayFraction(Double.parseDouble(lst) / Constant.TWO_PI);
		lst = DateTimeOps.twoDigits(astro.getHour())+":"+DateTimeOps.twoDigits(astro.getMinute())+":"+DateTimeOps.twoDigits((float)astro.getSeconds());

		double ddata[] = DataSet.toDoubleArray(data);
		float max = data[DataSet.getIndexOfMaximum(ddata)];
		float min = data[DataSet.getIndexOfMinimum(ddata)];
		if (max > 0.0) {
			max *= 1.05;
		} else {
			max /= 1.05;
		}
		if (min > 0.0) {
			min /= 1.05;
		} else {
			min *= 1.05;
		}
		double zero = 0.0;

		int bitpix = -64;
		if (forGILDAS) bitpix = -32;
        double offx = map.get(Gildas30m.OFF1).getAsDouble() * Constant.RAD_TO_DEG;
        double offy = map.get(Gildas30m.OFF2).getAsDouble() * Constant.RAD_TO_DEG;
		HeaderElement header[] = new HeaderElement[] {
				new HeaderElement("SIMPLE", "T", ""),
				new HeaderElement("BITPIX", ""+bitpix, ""),
				new HeaderElement("NAXIS", "4", ""),
				new HeaderElement("NAXIS1", ""+this.data.length, ""),
				new HeaderElement("NAXIS2", "1", ""),
				new HeaderElement("NAXIS3", "1", ""),
				new HeaderElement("NAXIS4", "1", ""),
				new HeaderElement("BLOCKED", "T", ""),
				new HeaderElement("BLANK", this.get(Gildas30m.BAD).getAsDouble(), "Blanking value"),
				new HeaderElement("BSCALE", "1.0", ""),
				new HeaderElement("BZERO", ""+zero, ""),
				new HeaderElement("DATAMIN", ""+min, ""),
				new HeaderElement("DATAMAX", ""+max, ""),
				new HeaderElement("BUNIT", "K", ""),
				new HeaderElement("CTYPE1", "FREQ", ""),
				new HeaderElement("CRVAL1", this.get(Gildas30m.FREQ_OFF).getAsDouble(), "Offset frequency"),
				new HeaderElement("CDELT1",this.get(Gildas30m.FREQ_RESOL).getAsDouble(), "Frequency resolution"),
				new HeaderElement("CRPIX1", this.get(Gildas30m.REF_CHAN).getAsDouble(), ""),
				new HeaderElement("CTYPE2", "RA---GLS", ""),
				new HeaderElement("EQUINOX", this.get(Gildas30m.EPOCH).getAsDouble(), ""),
				new HeaderElement("CRVAL2", ""+(Double.parseDouble(fixBlank(this.get(Gildas30m.LAMBDA).getAsString()))*
						Constant.RAD_TO_DEG), ""),
				new HeaderElement("CDELT2", ""+offx, ""),
				new HeaderElement("CRPIX2", "0.0000000000000E+00", ""),
				new HeaderElement("CTYPE3", "DEC--GLS", ""),
				new HeaderElement("CRVAL3", ""+(Double.parseDouble(fixBlank(this.get(Gildas30m.BETA).getAsString()))*
						Constant.RAD_TO_DEG), ""),
				new HeaderElement("CDELT3", ""+offy, ""),
				new HeaderElement("CRPIX3", "0.0000000000000E+00", ""),
				new HeaderElement("CTYPE4", "STOKES", ""),
				new HeaderElement("CRVAL4", "1.0000000000000", ""),
				new HeaderElement("CDELT4", "0.0000000000000", ""),
				new HeaderElement("CRPIX4", "0.0000000000000", ""),
				new HeaderElement("TELESCOP", HeaderElement.getByKey(headerParams, Gildas30m.TELES).getAsString(), ""),
				new HeaderElement("OBJECT", this.get(Gildas30m.SOURCE).getAsString(), ""),
				new HeaderElement("LINE", this.header.getVisibleHeader()[SpectrumHeader30m.VISIBLE_HEADER.VISIBLE_LINE.ordinal()].getAsString(), "Line name"),
				new HeaderElement("RESTFREQ",  ""+(Double.parseDouble(fixBlank(this.get(Gildas30m.REF_FREQ).getAsString()))*
						1.0E6), "Rest frequency"),
				new HeaderElement("VELO-LSR", this.get(Gildas30m.REF_VEL).getAsDouble(), "Velocity of reference channel"),
				new HeaderElement("DELTAV",  ""+(Double.parseDouble(fixBlank(this.get(Gildas30m.VEL_RESOL).getAsString()))*
						1.0E3), "Velocity spacing of channels"),
				new HeaderElement("IMAGFREQ",  ""+(Double.parseDouble(fixBlank(this.get(Gildas30m.IMAGE).getAsString()))*
						1.0E6), "Image frequency"),
				new HeaderElement("TSYS", this.get(Gildas30m.TSYS).getAsDouble(), "System temperature"),
				new HeaderElement("OBSTIME", this.get(Gildas30m.INTEG).getAsDouble(), "Integration time"),
				new HeaderElement("SCAN-NUM", HeaderElement.getByKey(headerParams, Gildas30m.SCAN).getAsInt(), "Scan number"),
				new HeaderElement("OBS-NUM", HeaderElement.getByKey(headerParams, Gildas30m.NUM).getAsInt(), "Observation number"),
				new HeaderElement("TAU-ATM", this.get(Gildas30m.TAU).getAsDouble(), "Atmospheric opacity"),
				new HeaderElement("GAINIMAG", this.get(Gildas30m.GAIN_IM).getAsDouble(), "Image sideband gain ratio"),
				new HeaderElement("BEAMEFF", this.get(Gildas30m.BEAM_EFF).getAsDouble(), "Beam efficiency"),
				new HeaderElement("FORWEFF", this.get(Gildas30m.FORW_EFF).getAsDouble(), "Image sideband gain ratio"),
				new HeaderElement("ORIGIN", "JPARSEC", ""),
				new HeaderElement("DATE", date, "Date written"),
				new HeaderElement("TIMESYS", "UTC", ""),
				new HeaderElement("DATE-OBS", dateObs, "Date observed"),
				new HeaderElement("DATE-RED", dateRed, "Date reduced"),
				new HeaderElement("ELEVATIO", ""+(Double.parseDouble(fixBlank(this.get(Gildas30m.ELEVATION).getAsString()))*
						Constant.RAD_TO_DEG), "Telescope elevation"),
				new HeaderElement("AZIMUTH", ""+(Double.parseDouble(fixBlank(this.get(Gildas30m.AZIMUTH).getAsString()))*
						Constant.RAD_TO_DEG), "Telescope azimuth"),
				new HeaderElement("ALTITUDE", ""+(Double.parseDouble(fixBlank(this.get(Gildas30m.ALTITUDE).getAsString()))), "Site elevation"),
				new HeaderElement("UT", ut, "Universal time at start"),
				new HeaderElement("LST", lst, "Sideral time at start"),
				new HeaderElement("MODE", ""+(Integer.parseInt(fixBlank(this.get(Gildas30m.MODE).getAsString()))), "Calibration Mode"),
				new HeaderElement("PAMB", ""+(Double.parseDouble(fixBlank(this.get(Gildas30m.PAMB).getAsString()))), Gildas30m.PAMB_DESC),
				new HeaderElement("SIGMA", ""+(Double.parseDouble(fixBlank(this.get(Gildas30m.SIGMA).getAsString()))), Gildas30m.SIGMA_DESC),
//				new ImageHeaderElement("SWMODE", ""+(Integer.parseInt(fixBlank(this.get(Gildas30m.SWMODE).getAsString())), Gildas30m.SWMODE_DESC),
				new HeaderElement("TAMB", ""+(Double.parseDouble(fixBlank(this.get(Gildas30m.TAMB).getAsString()))), Gildas30m.TAMB_DESC),
				new HeaderElement("VELTYPE", ""+(Integer.parseInt(fixBlank(this.get(Gildas30m.VEL_TYPE).getAsString()))), Gildas30m.VEL_TYPE_DESC),
				new HeaderElement("DOPPLER", ""+(Double.parseDouble(fixBlank(this.get(Gildas30m.DOPPLER).getAsString()))), Gildas30m.DOPPLER_DESC),
				new HeaderElement("COUNT1", ""+(Double.parseDouble(fixBlank(this.get(Gildas30m.COUNT1).getAsString()))), Gildas30m.COUNT1_DESC),
				new HeaderElement("COUNT2", ""+(Double.parseDouble(fixBlank(this.get(Gildas30m.COUNT2).getAsString()))), Gildas30m.COUNT2_DESC),
				new HeaderElement("COUNT3", ""+(Double.parseDouble(fixBlank(this.get(Gildas30m.COUNT3).getAsString()))), Gildas30m.COUNT3_DESC),
				new HeaderElement("FACTOR", ""+(Double.parseDouble(fixBlank(this.get(Gildas30m.FACTOR).getAsString()))), Gildas30m.FACTOR_DESC),
				new HeaderElement("H2OMM", ""+(Double.parseDouble(fixBlank(this.get(Gildas30m.H2OMM).getAsString()))), Gildas30m.H2OMM_DESC),
				new HeaderElement("TATMSIG", ""+(Double.parseDouble(fixBlank(this.get(Gildas30m.TATMSIG).getAsString()))), Gildas30m.TATMSIG_DESC),
				new HeaderElement("TAUIMA", ""+(Double.parseDouble(fixBlank(this.get(Gildas30m.TAUIMA).getAsString()))), Gildas30m.TAUIMA_DESC),
				new HeaderElement("TAUSIG", ""+(Double.parseDouble(fixBlank(this.get(Gildas30m.TAUSIG).getAsString()))), Gildas30m.TAUSIG_DESC),
				new HeaderElement("TCHOP", ""+(Double.parseDouble(fixBlank(this.get(Gildas30m.TCHOP).getAsString()))), Gildas30m.TCHOP_DESC),
				new HeaderElement("TCOLD", ""+(Double.parseDouble(fixBlank(this.get(Gildas30m.TCOLD).getAsString()))), Gildas30m.TCOLD_DESC),
				new HeaderElement("TREC", ""+(Double.parseDouble(fixBlank(this.get(Gildas30m.TREC).getAsString()))), Gildas30m.TREC_DESC),
				new HeaderElement("BLOCK", ""+this.header.getHeaderParameters()[SpectrumHeader30m.HEADER.BLOCK.ordinal()].getAsInt(), Gildas30m.BLOCK_DESC),
				new HeaderElement("OFFSETX", ""+(this.header.getHeaderParameters()[SpectrumHeader30m.HEADER.OFFSET1.ordinal()].getAsDouble()), Gildas30m.OFF1_DESC),
				new HeaderElement("OFFSETY", ""+(this.header.getHeaderParameters()[SpectrumHeader30m.HEADER.OFFSET2.ordinal()].getAsDouble()), Gildas30m.OFF2_DESC),
				new HeaderElement("KIND", ""+(this.header.getHeaderParameters()[SpectrumHeader30m.HEADER.KIND.ordinal()].getAsInt()), Gildas30m.KIND_DESC),
				new HeaderElement("POSA", ""+(this.header.getHeaderParameters()[SpectrumHeader30m.HEADER.POSA.ordinal()].getAsDouble()), Gildas30m.POSA_DESC),
				// Added to complete data for the observatory
				new HeaderElement("LON", ""+(Double.parseDouble(fixBlank(this.get(Gildas30m.LON).getAsString()))), Gildas30m.LON_DESC),
				new HeaderElement("LAT", ""+(Double.parseDouble(fixBlank(this.get(Gildas30m.LAT).getAsString()))), Gildas30m.LAT_DESC)
		};

		if (forGILDAS) {
			float[][][][] data = new float[1][1][1][this.data.length];
			for (int i=0; i<this.data.length; i++)
			{
				data[0][0][0][i] = this.data[i];
			}
			FitsIO fits = new FitsIO(data);
			fits.setHeader(0, header);
			fits.write(0, path);
		} else {
			double[][][][] data = new double[1][1][1][this.data.length];
			for (int i=0; i<this.data.length; i++)
			{
				data[0][0][0][i] = this.data[i];
			}
			FitsIO fits = new FitsIO(data);
			fits.setHeader(0, header);
			fits.write(0, path);
		}
	}

	/**
	 * Reads a set of 30m spectra from a fits file exported by Gildas, in SPECTRUM or INDEX modes.
	 * @param path The path to the .fits file.
	 * @return The set of spectra from the file.
	 * @throws JPARSECException If the input file is invalid.
	 */
	public static Spectrum30m[] readSpectraFromFITS(String path) throws JPARSECException {
		try {
			FitsIO fits = new FitsIO(path);
			if (fits.getNumberOfPlains() != 2 || !FitsIO.isBinaryTable(fits.getHDU(1))) {
				try {
					Spectrum30m s30m = new Spectrum30m();
					s30m.readFromFITS(path);
					return new Spectrum30m[] {s30m};
				} catch (Exception exc) {
					throw new JPARSECException("The file is not a valid fits file.");
				}
			}
			HeaderElement head[] = fits.getHeader(1);
		   	BinaryTableHDU bintable = (BinaryTableHDU) fits.getHDU(1);
		   	Spectrum30m out[] = new Spectrum30m[bintable.getNRows()];

		   	String source = HeaderElement.getByKey(head, "OBJECT").getAsString(); // EMPTY !
		   	String line = HeaderElement.getByKey(head, "LINE").getAsString();
		   	double refch = HeaderElement.getByKey(head, "CRPIX1").getAsDouble();
		   	HeaderElement vel = HeaderElement.getByKey(head, "VELOCITY");
		   	double f = 1;
		   	if (vel == null) {
			   	vel = HeaderElement.getByKey(head, "VELO-LSR");
			   	f = 0.001;
		   	}
		   	double doppler = -1;
		   	//if (ImageHeaderElement.getIndex(head, "DOPPLER") >= 0)
		   	//	doppler = ImageHeaderElement.getByKey(head, "DOPPLER").getAsDouble();
		   	double v0 = vel.getAsDouble() * f;
		   	double freqoff = HeaderElement.getByKey(head, "FOFFSET").getAsDouble();
		   	double epoch = HeaderElement.getByKey(head, "EPOCH").getAsDouble();
		   	double az = Constant.DEG_TO_RAD * HeaderElement.getByKey(head, "AZIMUTH").getAsDouble();
		   	double el = Constant.DEG_TO_RAD * HeaderElement.getByKey(head, "ELEVATIO").getAsDouble();
		   	double tsys = HeaderElement.getByKey(head, "TSYS").getAsDouble();
		   	double tau = HeaderElement.getByKey(head, "TAU-ATM").getAsDouble();
		   	double ut = HeaderElement.getByKey(head, "UT").getAsDouble() / 3600.0;
		   	double lst = HeaderElement.getByKey(head, "LST").getAsDouble() / 3600.0;
		   	double time = HeaderElement.getByKey(head, "OBSTIME").getAsDouble();
		   	double beameff = HeaderElement.getByKey(head, "BEAMEFF").getAsDouble();
		   	double foreff = HeaderElement.getByKey(head, "FORWEFF").getAsDouble();
		   	double gainimg = HeaderElement.getByKey(head, "GAINIMAG").getAsDouble();
		   	double tcold = HeaderElement.getByKey(head, "TCOLD").getAsDouble();
		   	double tchop = HeaderElement.getByKey(head, "TCHOP").getAsDouble();
		   	double patm = HeaderElement.getByKey(head, "PRESSURE").getAsDouble();
		   	double tatm = HeaderElement.getByKey(head, "TOUTSIDE").getAsDouble();
		   	double h2o = HeaderElement.getByKey(head, "MH2O").getAsDouble();
		   	String telescope = HeaderElement.getByKey(head, "TELESCOP").getAsString();
		   	int scan = HeaderElement.getByKey(head, "SCAN").getAsInt();
		   	double rfreq = 1.0E-6 * HeaderElement.getByKey(head, "RESTFREQ").getAsDouble();
		   	double imgfreq = 1.0E-6 * HeaderElement.getByKey(head, "IMAGFREQ").getAsDouble();
		   	double velres = 1.0E-3 * HeaderElement.getByKey(head, "DELTAV").getAsDouble();
	 		String dobs = DataSet.replaceAll(HeaderElement.getByKey(head, "DATE-OBS").getAsString(), "T", " ", false);
			double jdObs = (new AstroDate(dobs)).jd();
	 		String dred = DataSet.replaceAll(HeaderElement.getByKey(head, "DATE-RED").getAsString(), "T", " ", false);
			double jdRed = (new AstroDate(dred)).jd();
			double offsetX = 0, offsetY = 0;
			offsetX = HeaderElement.getByKey(head, "CDELT2").getAsDouble() * Constant.DEG_TO_RAD;
			offsetY = HeaderElement.getByKey(head, "CDELT3").getAsDouble() * Constant.DEG_TO_RAD;

		   	int observationNumber = -1, block = 0, version = 0, kind = 0, quality = 0, todo = 0;
	        int projection = Gildas30m.PROJECTION_RADIO, veltype = 1, mode = 1;
		   	for (int i=0; i<out.length; i++) {
		   		observationNumber ++;
				float sp[] = ((float[]) FitsBinaryTable.getBinaryTableElement(fits.getHDU(1), i, 0));
				//float wave[] = ((float[]) FitsBinaryTable.getBinaryTableElement(fits.getHDU(1), i, 1));
				//String telescope = FitsBinaryTable.getBinaryTableElement(fits.getHDU(1), i, 2).toString();
				//float rfreq = ((float[]) FitsBinaryTable.getBinaryTableElement(fits.getHDU(1), i, 3))[0];
				//float imgfreq = ((float[]) FitsBinaryTable.getBinaryTableElement(fits.getHDU(1), i, 4))[0];
				//rfreq *= 1.0E-6;
				//imgfreq *= 1.0E-6;
				//float velres = ((float[]) FitsBinaryTable.getBinaryTableElement(fits.getHDU(1), i, 5))[0];
				//float sp[] = ((float[]) FitsBinaryTable.getBinaryTableElement(fits.getHDU(1), i, 6));
				//sp = DataSet.getSubArray(sp, 0, (int)maxis-1);
				SpectrumHeader30m header = new SpectrumHeader30m(new HeaderElement[] {
		        		new HeaderElement(Gildas30m.NUM, observationNumber, Gildas30m.NUM_DESC),
		           		new HeaderElement(Gildas30m.BLOCK, block, Gildas30m.BLOCK_DESC),
		           		new HeaderElement(Gildas30m.VERSION, version, Gildas30m.VERSION_DESC),
		           		new HeaderElement(Gildas30m.SOURCE, source, Gildas30m.SOURCE_DESC),
		           		new HeaderElement(Gildas30m.LINE, line, Gildas30m.LINE_DESC),
		           		new HeaderElement(Gildas30m.TELES, telescope, Gildas30m.TELES_DESC),
		           		new HeaderElement(Gildas30m.LDOBS, GildasDataFormat.getGILDASdate(jdObs), Gildas30m.LDOBS_DESC),
		           		new HeaderElement(Gildas30m.LDRED, GildasDataFormat.getGILDASdate(jdRed), Gildas30m.LDRED_DESC),
		           		new HeaderElement(Gildas30m.OFF1, offsetX, Gildas30m.OFF1_DESC),
		           		new HeaderElement(Gildas30m.OFF2, offsetY, Gildas30m.OFF2_DESC),
		           		new HeaderElement(Gildas30m.TYPEC, Gildas30m.COORDINATES_EQUATORIAL, Gildas30m.TYPEC_DESC),
		           		new HeaderElement(Gildas30m.KIND, kind, Gildas30m.KIND_DESC),
		           		new HeaderElement(Gildas30m.QUALITY, quality, Gildas30m.QUALITY_DESC),
		           		new HeaderElement(Gildas30m.SCAN, scan, Gildas30m.SCAN_DESC),
		           		new HeaderElement(Gildas30m.POSA, todo, Gildas30m.POSA_DESC),
		        });
				out[i] = new Spectrum30m(header, (new float[][] {sp})[0]);

		        out[i].map.put(Gildas30m.LDOBS, new HeaderElement(GildasDataFormat.getGILDASdate(jdObs), Gildas30m.LDOBS_DESC));
		        out[i].map.put(Gildas30m.LDRED, new HeaderElement(GildasDataFormat.getGILDASdate(jdRed), Gildas30m.LDRED_DESC));
		        out[i].map.put(Gildas30m.SOURCE, new HeaderElement(source, Gildas30m.SOURCE_DESC));
		        out[i].map.put(Gildas30m.LINE, new HeaderElement(line, Gildas30m.LINE_DESC));
		        out[i].map.put(Gildas30m.REF_FREQ, new HeaderElement(rfreq, Gildas30m.REF_FREQ_DESC));
		        out[i].map.put(Gildas30m.VEL_RESOL, new HeaderElement(velres, Gildas30m.VEL_RESOL_DESC));
		        out[i].map.put(Gildas30m.IMAGE, new HeaderElement(imgfreq, Gildas30m.IMAGE_DESC));
		        out[i].map.put(Gildas30m.REF_CHAN, new HeaderElement(refch, Gildas30m.REF_CHAN_DESC));
		        out[i].map.put(Gildas30m.NCHAN, new HeaderElement(out[i].getNumberOfChannels(), Gildas30m.NCHAN_DESC));
		        out[i].map.put(Gildas30m.EPOCH, new HeaderElement(epoch, Gildas30m.EPOCH_DESC));
		        out[i].map.put(Gildas30m.PROJECTION, new HeaderElement(projection, Gildas30m.PROJECTION_DESC));
		        out[i].map.put(Gildas30m.VEL_TYPE, new HeaderElement(veltype, Gildas30m.VEL_TYPE_DESC));
		        out[i].map.put(Gildas30m.DOPPLER, new HeaderElement(doppler, Gildas30m.DOPPLER_DESC));
		        out[i].map.put(Gildas30m.MODE, new HeaderElement(mode, Gildas30m.MODE_DESC));
		        out[i].map.put(Gildas30m.REF_VEL, new HeaderElement(v0, Gildas30m.REF_VEL_DESC));
		        out[i].map.put(Gildas30m.FREQ_OFF, new HeaderElement(freqoff, Gildas30m.FREQ_OFF_DESC));
		        out[i].map.put(Gildas30m.AZIMUTH, new HeaderElement(az, Gildas30m.AZIMUTH_DESC));
		        out[i].map.put(Gildas30m.ELEVATION, new HeaderElement(el, Gildas30m.ELEVATION_DESC));
		        out[i].map.put(Gildas30m.TAU, new HeaderElement(tau, Gildas30m.TAU_DESC));
		        out[i].map.put(Gildas30m.TSYS, new HeaderElement(tsys, Gildas30m.TSYS_DESC));
				out[i].map.put(Gildas30m.UT_TIME, new HeaderElement(ut, Gildas30m.UT_TIME_DESC));
		        out[i].map.put(Gildas30m.LST_TIME, new HeaderElement(lst, Gildas30m.LST_TIME_DESC));
		        out[i].map.put(Gildas30m.INTEG, new HeaderElement(time, Gildas30m.INTEG_DESC));
		        out[i].map.put(Gildas30m.BEAM_EFF, new HeaderElement(beameff, Gildas30m.BEAM_EFF_DESC));
		        out[i].map.put(Gildas30m.FORW_EFF, new HeaderElement(foreff, Gildas30m.FORW_EFF_DESC));
		        out[i].map.put(Gildas30m.GAIN_IM, new HeaderElement(gainimg, Gildas30m.GAIN_IM_DESC));
		        out[i].map.put(Gildas30m.H2OMM, new HeaderElement(h2o, Gildas30m.H2OMM_DESC));
		        out[i].map.put(Gildas30m.PAMB, new HeaderElement(patm, Gildas30m.PAMB_DESC));
		        out[i].map.put(Gildas30m.TAMB, new HeaderElement(tatm, Gildas30m.TAMB_DESC));
		        out[i].map.put(Gildas30m.TCHOP, new HeaderElement(tchop, Gildas30m.TCHOP_DESC));
		        out[i].map.put(Gildas30m.TCOLD, new HeaderElement(tcold, Gildas30m.TCOLD_DESC));
		        out[i].map.put(Gildas30m.SCAN, new HeaderElement(scan, Gildas30m.SCAN_DESC));
		        out[i].map.put(Gildas30m.NUM, new HeaderElement(observationNumber, Gildas30m.NUM_DESC));
		        out[i].map.put(Gildas30m.OFF1, new HeaderElement(offsetX, Gildas30m.OFF1_DESC));
		        out[i].map.put(Gildas30m.OFF2, new HeaderElement(offsetY, Gildas30m.OFF2_DESC));
		        out[i].map.put(Gildas30m.LAMBDA_OFF, new HeaderElement(offsetX, Gildas30m.LAMBDA_OFF_DESC));
		        out[i].map.put(Gildas30m.BETA_OFF, new HeaderElement(offsetY, Gildas30m.BETA_OFF_DESC));
		        out[i].map.put(Gildas30m.TYPEC, new HeaderElement(Gildas30m.COORDINATES_EQUATORIAL, Gildas30m.TYPEC_DESC));

			   	double freqres = - velres * rfreq / (Constant.SPEED_OF_LIGHT * 0.001);
		        out[i].map.put(Gildas30m.FREQ_RESOL, new HeaderElement(freqres, Gildas30m.FREQ_RESOL_DESC));
		        //out[i].map.put(Gildas30m.BAD, new Parameter(bad, Gildas30m.BAD_DESC));
		        //out[i].map.put(Gildas30m.SIGMA, new Parameter(sigma, Gildas30m.SIGMA_DESC));
		   	}
		   	return out;
		} catch (Exception exc) {
			exc.printStackTrace();
		}
		return null;
	}

	/**
	 * Reads the spectrum in a .fits file.<P>
	 *
	 * An example of a full FITS header is:
	 * <pre>
	 * SIMPLE = T /
	 * BITPIX = -32 /
	 * NAXIS = 4 /
	 * NAXIS1 = 256 /
	 * NAXIS2 = 1 /
	 * NAXIS3 = 1 /
	 * NAXIS4 = 1 /
	 * BLOCKED = T /
	 * BLANK = -1000.0 / Blanking value
	 * BSCALE = 1.0 /
	 * BZERO = 0.0 /
	 * DATAMIN = 0.8968728496914817 /
	 * DATAMAX = 0.9888023167848587 /
	 * BUNIT = K /
	 * CTYPE1 = FREQ /
	 * CRVAL1 = 0.0 / Offset frequency
	 * CDELT1 = -1.0 / Frequency resolution
	 * CRPIX1 = 127.5 /
	 * CTYPE2 = RA---GLS /
	 * EQUINOX = 2000.0 /
	 * CRVAL2 = 325.7570478593505 /
	 * CDELT2 = 0.0000000000000E+00 /
	 * CRPIX2 = 0.0000000000000E+00 /
	 * CTYPE3 = DEC--GLS /
	 * CRVAL3 = 66.05655580294503 /
	 * CDELT3 = 0.0000000000000E+00 /
	 * CRPIX3 = 0.0000000000000E+00 /
	 * CTYPE4 = STOKES /
	 * CRVAL4 = 1.0000000000000 /
	 * CDELT4 = 0.0000000000000 /
	 * CRPIX4 = 0.0000000000000 /
	 * TELESCOP = 30M-1M4-B230 /
	 * OBJECT = N7129S       /
	 * LINE = SO2(11210)   / Line name
	 * RESTFREQ = 2.0530057E11 / Rest frequency
	 * VELO-LSR = -10.0 / Velocity of reference channel
	 * DELTAV = 1460.2612 / Velocity spacing of channels
	 * IMAGFREQ = 2.1381566535578754E11 / Image frequency
	 * TSYS = 4509.677 / System temperature
	 * OBSTIME = 2400.0 / Integration time
	 * SCAN-NUM = 5721 / Scan number
	 * OBS-NUM = 6740 / Observation number
	 * TAU-ATM = 0.5581591 / Atmospheric opacity
	 * GAINIMAG = 0.0316 / Image sideband gain ratio
	 * BEAMEFF = 0.576 / Beam efficiency
	 * FORWEFF = 0.91 / Image sideband gain ratio
	 * ORIGIN = JPARSEC /
	 * DATE = 2008-03-04T18:26:32.465 / Date written
	 * TIMESYS = UTC /
	 * DATE-OBS = 2004-06-07T05:06:01.8317326 / Date observed
	 * DATE-RED = 2004-06-07T00:00:00.0 / Date reduced
	 * ELEVATIO = 17.708361883400332 / Telescope elevation
	 * AZIMUTH = 375.7734875815459 / Telescope azimuth
	 * UT = 05:06:01.8317326 / Universal time at start
	 * LST = 03:14:14.860806 / Sideral time at start
	 * LON = 0.0 (Longitude of the observatory)
	 * LAT = 0.0 (Latitude of the observatory)
	 * </pre>
	 * @param path The path to the .fits file.
	 * @throws JPARSECException If an error occurs.
	 * @return Number of errors found when reading the header,
	 * due to the absence of certain header parameter or to
	 * an incorrect data type. Fits format using in Gildas has
	 * evolved and changed since the first versions, and many
	 * parameters are not present, so more than 20 errors is normal.
	 * Currently 24 is the usual value.
	 */
	public int readFromFITS(String path)
	throws JPARSECException {
		int nErr = 0;
		FitsIO fits = new FitsIO(path);
		if (fits.getNumberOfPlains() == 2 && FitsIO.isBinaryTable(fits.getHDU(1))) {
			Spectrum30m s30m = Spectrum30m.readSpectraFromFITS(path)[0];
			this.map = s30m.map;
			this.header = s30m.header;
			this.data = s30m.data;
			return 0;
		}

		int n = 0;
		HeaderElement head[] = fits.getHeader(n);
		Object data = fits.getDataAsDoubleArray(n, 0);
		double d[][] = (double[][]) data;
		this.data = new float[d.length];
		for (int i=0; i<d.length; i++)
		{
			this.data[i] = (float) d[i][0];
		}

		int nch = 0;
		try { nch = (HeaderElement.getByKey(head, "NAXIS1")).getAsInt(); } catch (Exception exc1) { nErr ++; }
        double ut, lst, az = 0.0, el = 0.0, tau = 0.0, tsys = 0.0, integt = 0.0, epoch = 2000.0;
        double lamb = 0.0, beta = 0.0, lamboff = 0.0, betaoff = 0.0, rfreq = 0.0, refch = 0.0, freqres = 0.0;
        double freqoff = 0.0, velres = 0.0, v0 = 0.0, bad = 0.0, imgfreq = 0.0, beameff = 0.0, foreff = 0.0;
        double gainim = 0.0, h2omm = 0.0, pamb = 0.0, tamb = 0.0, tatmsig = 0.0, tchop = 0.0;
        double tcold = 0.0, tausig = 0.0, tauima = 0.0, trec = 0.0, factor = 0.0, altitude = 0.0;
        double c1 = 0.0, c2 = 0.0, c3 = 0.0, sigma = 0.0, swde = 0.0, swdu = 0.0, swp = 0.0;
        double swl = 0.0, swb = 0.0, todo = 0.0, lon = 0.0, lat = 0.0, doppler = -1;
        int projection = Gildas30m.PROJECTION_RADIO, veltype = 1, mode = 1;
        int nph = 0, version = 2, block = 0, kind = 0, quality = 0;
        String source = "", line = "";
        int observationNumber = 0;
        double offsetX = 0, offsetY = 0;
        String dobs = "";

//        sigma = sp.sigmaRMS;
        try { source = (HeaderElement.getByKey(head, "OBJECT")).getAsString(); } catch (Exception exc1) { nErr ++; }
        try { line = (HeaderElement.getByKey(head, "LINE")).getAsString(); } catch (Exception exc1) { nErr ++; }

        try { observationNumber = (HeaderElement.getByKey(head, "OBS-NUM")).getAsInt(); } catch (Exception exc1) { nErr ++; }
        try { bad = (HeaderElement.getByKey(head, "BLANK")).getAsDouble();
			double scale = (HeaderElement.getByKey(head, "BSCALE")).getAsDouble();
			double bzero = (HeaderElement.getByKey(head, "BZERO")).getAsDouble();
			bad = bad * scale + bzero;
			for (int i=0; i<d.length; i++)
			{
				if (this.data[i] == bad) this.data[i] = Float.NaN;
			}
        } catch (Exception exc1) { nErr ++; }
        try { refch = Double.parseDouble((HeaderElement.getByKey(head, "CRPIX1")).getAsString()); } catch (Exception exc1) { nErr ++; }
        try { freqoff = Double.parseDouble((HeaderElement.getByKey(head, "CRVAL1")).getAsString()); } catch (Exception exc1) { nErr ++; }
        try { freqres = 1.0E-6 * Double.parseDouble((HeaderElement.getByKey(head, "CDELT1")).getAsString()); } catch (Exception exc1) { nErr ++; }
        try { epoch = Double.parseDouble((HeaderElement.getByKey(head, "EQUINOX")).getAsString()); } catch (Exception exc1) { nErr ++; }
        try { lamb = Double.parseDouble((HeaderElement.getByKey(head, "CRVAL2")).getAsString()) * Constant.DEG_TO_RAD; } catch (Exception exc1) { nErr ++; }
        try { beta = Double.parseDouble((HeaderElement.getByKey(head, "CRVAL3")).getAsString()) * Constant.DEG_TO_RAD; } catch (Exception exc1) { nErr ++; }
        try { v0 = 0.001 * Double.parseDouble((HeaderElement.getByKey(head, "VELO-LSR")).getAsString()); } catch (Exception exc1) {
            try { v0 = Double.parseDouble((HeaderElement.getByKey(head, "VELO")).getAsString()); } catch (Exception exc2) { nErr ++; }
        }
        try { rfreq = Double.parseDouble((HeaderElement.getByKey(head, "RESTFREQ")).getAsString()) / 1.0E6; } catch (Exception exc1) { nErr ++; }
        try { velres = Double.parseDouble((HeaderElement.getByKey(head, "DELTAV")).getAsString()) / 1.0E3; } catch (Exception exc1) { nErr ++; }
        try { imgfreq = Double.parseDouble((HeaderElement.getByKey(head, "IMAGFREQ")).getAsString()) / 1.0E6; } catch (Exception exc1) { nErr ++; }
        try { tsys = Double.parseDouble((HeaderElement.getByKey(head, "TSYS")).getAsString()); } catch (Exception exc1) { nErr ++; }
        try { integt = Double.parseDouble((HeaderElement.getByKey(head, "OBSTIME")).getAsString()); } catch (Exception exc1) { nErr ++; }
        try { tau = Double.parseDouble((HeaderElement.getByKey(head, "TAU-ATM")).getAsString()); } catch (Exception exc1) { nErr ++; }
        try { gainim = Double.parseDouble((HeaderElement.getByKey(head, "GAINIMAG")).getAsString()); } catch (Exception exc1) { nErr ++; }
        try { beameff = Double.parseDouble((HeaderElement.getByKey(head, "BEAMEFF")).getAsString()); } catch (Exception exc1) { nErr ++; }
        try { foreff = Double.parseDouble((HeaderElement.getByKey(head, "FORWEFF")).getAsString()); } catch (Exception exc1) { nErr ++; }
        try { dobs = (HeaderElement.getByKey(head, "DATE-OBS")).getAsString(); } catch (Exception exc1) { nErr ++; }
        try { az = Constant.DEG_TO_RAD * Double.parseDouble(HeaderElement.getByKey(head, "AZIMUTH").getAsString()); } catch (Exception exc1) { nErr ++; }
        try { el = Constant.DEG_TO_RAD * Double.parseDouble(HeaderElement.getByKey(head, "ELEVATIO").getAsString()); } catch (Exception exc1) { nErr ++; }
        try { altitude = Double.parseDouble(HeaderElement.getByKey(head, "ALTITUDE").getAsString()); } catch (Exception exc1) { nErr ++; }
        try { lon = Double.parseDouble(HeaderElement.getByKey(head, "LON").getAsString()); } catch (Exception exc1) { nErr ++; }
        try { lat = Double.parseDouble(HeaderElement.getByKey(head, "LAT").getAsString()); } catch (Exception exc1) { nErr ++; }
        try { mode = Integer.parseInt(HeaderElement.getByKey(head, "MODE").getAsString()); } catch (Exception exc1) { nErr ++; }
        try { pamb = Double.parseDouble(HeaderElement.getByKey(head, "PAMB").getAsString()); } catch (Exception exc1) { nErr ++; }
        try { sigma = Double.parseDouble(HeaderElement.getByKey(head, "SIGMA").getAsString()); } catch (Exception exc1) { nErr ++; }
//        try { swm = Integer.parseInt(ImageHeaderElement.getByKey(head, "SWMODE").getAsString()); } catch (Exception exc1) { nErr ++; }
        try { tamb = Double.parseDouble(HeaderElement.getByKey(head, "TAMB").getAsString()); } catch (Exception exc1) { nErr ++; }
        try { veltype = Integer.parseInt(HeaderElement.getByKey(head, "VELTYPE").getAsString()); } catch (Exception exc1) { nErr ++; }
        //try { doppler = Double.parseDouble(ImageHeaderElement.getByKey(head, "DOPPPLER").getAsString()); } catch (Exception exc1) { nErr ++; }
        try { c1 = Double.parseDouble(HeaderElement.getByKey(head, "COUNT1").getAsString()); } catch (Exception exc1) { nErr ++; }
        try { c2 = Double.parseDouble(HeaderElement.getByKey(head, "COUNT2").getAsString()); } catch (Exception exc1) { nErr ++; }
        try { c3 = Double.parseDouble(HeaderElement.getByKey(head, "COUNT3").getAsString()); } catch (Exception exc1) { nErr ++; }
        try { factor = Double.parseDouble(HeaderElement.getByKey(head, "FACTOR").getAsString()); } catch (Exception exc1) { nErr ++; }
        try { h2omm = Double.parseDouble(HeaderElement.getByKey(head, "MH2O").getAsString()); } catch (Exception exc1) { nErr ++; }
        try { h2omm = Double.parseDouble(HeaderElement.getByKey(head, "H2OMM").getAsString()); } catch (Exception exc1) { nErr ++; }
        try { tatmsig = Double.parseDouble(HeaderElement.getByKey(head, "TATMSIG").getAsString()); } catch (Exception exc1) { nErr ++; }
        try { tauima = Double.parseDouble(HeaderElement.getByKey(head, "TAUIMA").getAsString()); } catch (Exception exc1) { nErr ++; }
        try { tausig = Double.parseDouble(HeaderElement.getByKey(head, "TAUSIG").getAsString()); } catch (Exception exc1) { nErr ++; }
        try {
        	double tauatm = Double.parseDouble(HeaderElement.getByKey(head, "TAU-ATM").getAsString());
        	nErr = nErr - 2;
        } catch (Exception exc1) { }
        try { tchop = Double.parseDouble(HeaderElement.getByKey(head, "TCHOP").getAsString()); } catch (Exception exc1) { nErr ++; }
        try { tcold = Double.parseDouble(HeaderElement.getByKey(head, "TCOLD").getAsString()); } catch (Exception exc1) { nErr ++; }
        try { trec = Double.parseDouble(HeaderElement.getByKey(head, "TREC").getAsString()); } catch (Exception exc1) { nErr ++; }
        try { block = Integer.parseInt(HeaderElement.getByKey(head, "BLOCK").getAsString()); } catch (Exception exc1) { nErr ++; }
        try {
        	offsetX = Double.parseDouble(HeaderElement.getByKey(head, "OFFSETX").getAsString()); } catch (Exception exc1) {
        	nErr ++;
        	try {
            	offsetX = Double.parseDouble(HeaderElement.getByKey(head, "CDELT2").getAsString()) * Constant.DEG_TO_RAD;
            	nErr --;
        	} catch (Exception exc2) {}
        }
        try {
        	offsetY = Double.parseDouble(HeaderElement.getByKey(head, "OFFSETY").getAsString()); } catch (Exception exc1) {
       		nErr ++;
        	try {
            	offsetY = Double.parseDouble(HeaderElement.getByKey(head, "CDELT3").getAsString()) * Constant.DEG_TO_RAD;
            	nErr --;
        	} catch (Exception exc2) {}
       	}
        try { kind = Integer.parseInt(HeaderElement.getByKey(head, "KIND").getAsString()); } catch (Exception exc1) { nErr ++; }
        try { todo = Double.parseDouble(HeaderElement.getByKey(head, "POSA").getAsString()); } catch (Exception exc1) { nErr ++; }

 		dobs = DataSet.replaceAll(dobs, "T", " ", false);
		AstroDate aobs = new AstroDate(dobs);
		double jdObs = aobs.jd();
		String dred = (HeaderElement.getByKey(head, "DATE-RED")).getAsString();
		dred = DataSet.replaceAll(dred, "T", " ", false);
		AstroDate ared = new AstroDate(dred);
		double jdRed = ared.jd();
		ut = AstroDate.getDayFraction(jdObs) * Constant.TWO_PI;
		String dlst = (HeaderElement.getByKey(head, "LST")).getAsString();

		double val[] = DataSet.toDoubleValues(DataSet.toStringArray(dlst, ":", false));
		lst = (val[0] + val[1] / 60.0 + val[2] / 3600.0) * Constant.TWO_PI / 24.0;
		//dlst = "2000-01-01 "+dlst;
		//AstroDate alst = new AstroDate(dlst);
		//lst = AstroDate.getDayFraction(alst.jd()) * Constant.TWO_PI;

        map = new TreeMap<String,HeaderElement>();
        map.put(Gildas30m.LDOBS, new HeaderElement(GildasDataFormat.getGILDASdate(jdObs), Gildas30m.LDOBS_DESC));
        map.put(Gildas30m.LDRED, new HeaderElement(GildasDataFormat.getGILDASdate(jdRed), Gildas30m.LDRED_DESC));
        map.put(Gildas30m.UT_TIME, new HeaderElement(ut, Gildas30m.UT_TIME_DESC));
        map.put(Gildas30m.LST_TIME, new HeaderElement(lst, Gildas30m.LST_TIME_DESC));
        map.put(Gildas30m.AZIMUTH, new HeaderElement(az, Gildas30m.AZIMUTH_DESC));
        map.put(Gildas30m.ELEVATION, new HeaderElement(el, Gildas30m.ELEVATION_DESC));
        map.put(Gildas30m.TAU, new HeaderElement(tau, Gildas30m.TAU_DESC));
        map.put(Gildas30m.TSYS, new HeaderElement(tsys, Gildas30m.TSYS_DESC));
        map.put(Gildas30m.INTEG, new HeaderElement(integt, Gildas30m.INTEG_DESC));
        map.put(Gildas30m.SOURCE, new HeaderElement(source, Gildas30m.SOURCE_DESC));
        map.put(Gildas30m.EPOCH, new HeaderElement(epoch, Gildas30m.EPOCH_DESC));
        map.put(Gildas30m.LAMBDA, new HeaderElement(lamb, Gildas30m.LAMBDA_DESC));
        map.put(Gildas30m.BETA, new HeaderElement(beta, Gildas30m.BETA_DESC));
        map.put(Gildas30m.LAMBDA_OFF, new HeaderElement(lamboff, Gildas30m.LAMBDA_OFF_DESC));
        map.put(Gildas30m.BETA_OFF, new HeaderElement(betaoff, Gildas30m.BETA_OFF_DESC));
        map.put(Gildas30m.PROJECTION, new HeaderElement(projection, Gildas30m.PROJECTION_DESC));
        map.put(Gildas30m.LINE, new HeaderElement(line, Gildas30m.LINE_DESC));
        map.put(Gildas30m.REF_FREQ, new HeaderElement(rfreq, Gildas30m.REF_FREQ_DESC));
        map.put(Gildas30m.NCHAN, new HeaderElement(nch, Gildas30m.NCHAN_DESC));
        map.put(Gildas30m.REF_CHAN, new HeaderElement(refch, Gildas30m.REF_CHAN_DESC));
        map.put(Gildas30m.FREQ_RESOL, new HeaderElement(freqres, Gildas30m.FREQ_RESOL_DESC));
        map.put(Gildas30m.FREQ_OFF, new HeaderElement(freqoff, Gildas30m.FREQ_OFF_DESC));
        map.put(Gildas30m.VEL_RESOL, new HeaderElement(velres, Gildas30m.VEL_RESOL_DESC));
        map.put(Gildas30m.REF_VEL, new HeaderElement(v0, Gildas30m.REF_VEL_DESC));
        map.put(Gildas30m.BAD, new HeaderElement(bad, Gildas30m.BAD_DESC));
        map.put(Gildas30m.IMAGE, new HeaderElement(imgfreq, Gildas30m.IMAGE_DESC));
        map.put(Gildas30m.VEL_TYPE, new HeaderElement(veltype, Gildas30m.VEL_TYPE_DESC));
        map.put(Gildas30m.DOPPLER, new HeaderElement(doppler, Gildas30m.DOPPLER_DESC));
        map.put(Gildas30m.BEAM_EFF, new HeaderElement(beameff, Gildas30m.BEAM_EFF_DESC));
        map.put(Gildas30m.FORW_EFF, new HeaderElement(foreff, Gildas30m.FORW_EFF_DESC));
        map.put(Gildas30m.GAIN_IM, new HeaderElement(gainim, Gildas30m.GAIN_IM_DESC));
        map.put(Gildas30m.H2OMM, new HeaderElement(h2omm, Gildas30m.H2OMM_DESC));
        map.put(Gildas30m.PAMB, new HeaderElement(pamb, Gildas30m.PAMB_DESC));
        map.put(Gildas30m.TAMB, new HeaderElement(tamb, Gildas30m.TAMB_DESC));
        map.put(Gildas30m.TATMSIG, new HeaderElement(tatmsig, Gildas30m.TATMSIG_DESC));
        map.put(Gildas30m.TCHOP, new HeaderElement(tchop, Gildas30m.TCHOP_DESC));
        map.put(Gildas30m.TCOLD, new HeaderElement(tcold, Gildas30m.TCOLD_DESC));
        map.put(Gildas30m.TAUSIG, new HeaderElement(tausig, Gildas30m.TAUSIG_DESC));
        map.put(Gildas30m.TAUIMA, new HeaderElement(tauima, Gildas30m.TAUIMA_DESC));
        map.put(Gildas30m.TREC, new HeaderElement(trec, Gildas30m.TREC_DESC));
        map.put(Gildas30m.MODE, new HeaderElement(mode, Gildas30m.MODE_DESC));
        map.put(Gildas30m.FACTOR, new HeaderElement(factor, Gildas30m.FACTOR_DESC));
        map.put(Gildas30m.ALTITUDE, new HeaderElement(altitude, Gildas30m.ALTITUDE_DESC));
        map.put(Gildas30m.COUNT1, new HeaderElement(c1, Gildas30m.COUNT1_DESC));
        map.put(Gildas30m.COUNT2, new HeaderElement(c2, Gildas30m.COUNT2_DESC));
        map.put(Gildas30m.COUNT3, new HeaderElement(c3, Gildas30m.COUNT3_DESC));
        map.put(Gildas30m.LON, new HeaderElement(lon, Gildas30m.LON_DESC));
        map.put(Gildas30m.LAT, new HeaderElement(lat, Gildas30m.LAT_DESC));
        map.put(Gildas30m.OFF1, new HeaderElement(offsetX, Gildas30m.OFF1_DESC));
        map.put(Gildas30m.OFF2, new HeaderElement(offsetY, Gildas30m.OFF2_DESC));
        map.put(Gildas30m.LAMBDA_OFF, new HeaderElement(offsetX, Gildas30m.LAMBDA_OFF_DESC));
        map.put(Gildas30m.BETA_OFF, new HeaderElement(offsetY, Gildas30m.BETA_OFF_DESC));
        map.put(Gildas30m.TYPEC, new HeaderElement(Gildas30m.COORDINATES_EQUATORIAL, Gildas30m.TYPEC_DESC));
//        map.put(Gildas30m.OTF_NDUMPS, new Parameter(otfn, Gildas30m.OTF_NDUMPS_DESC));
//        map.put(Gildas30m.OTF_LEN_HEADER, new Parameter(otfh, Gildas30m.OTF_LEN_HEADER_DESC));
//        map.put(Gildas30m.OTF_LEN_DATA, new Parameter(otfd, Gildas30m.OTF_LEN_DATA_DESC));
//        map.put(Gildas30m.OTF_LEN_DUMP, new Parameter(otfdu, Gildas30m.OTF_LEN_DUMP_DESC));
//        map.put(Gildas30m.NPHASE, new Parameter(nph, Gildas30m.NPHASE_DESC));
//        map.put(Gildas30m.SWMODE, new Parameter(swm, Gildas30m.SWMODE_DESC));
        for(int i1 = 0; i1 < nph; i1++)
        {
            map.put((new StringBuilder().append(Gildas30m.SWDECALAGE).append(i1).toString()), new HeaderElement(swde, Gildas30m.SWDECALAGE_DESC));
            map.put((new StringBuilder().append(Gildas30m.SWDURATION).append(i1).toString()), new HeaderElement(swdu, Gildas30m.SWDURATION_DESC));
            map.put((new StringBuilder().append(Gildas30m.SWPOIDS).append(i1).toString()), new HeaderElement(swp, Gildas30m.SWPOIDS_DESC));
            map.put((new StringBuilder().append(Gildas30m.SWLDECAL).append(i1).toString()), new HeaderElement(swl, Gildas30m.SWLDECAL_DESC));
            map.put((new StringBuilder().append(Gildas30m.SWBDECAL).append(i1).toString()), new HeaderElement(swb, Gildas30m.SWBDECAL_DESC));
        }
        map.put(Gildas30m.SIGMA, new HeaderElement(sigma, Gildas30m.SIGMA_DESC));

        String telescope = "", scanN = "";
   		try { telescope = HeaderElement.getByKey(head, "TELESCOP").getAsString(); } catch (Exception exc1) { nErr ++; }
   		try { scanN = HeaderElement.getByKey(head, "SCAN-NUM").getAsString(); } catch (Exception exc1) { nErr ++; }

   		int scanNumber = (int) Math.round(Double.parseDouble(scanN));
        map.put(Gildas30m.NUM, new HeaderElement(observationNumber, Gildas30m.NUM_DESC));
        map.put(Gildas30m.SCAN, new HeaderElement(scanNumber, Gildas30m.SCAN_DESC));
        header = new SpectrumHeader30m(new HeaderElement[] {
        		new HeaderElement(Gildas30m.NUM, observationNumber, Gildas30m.NUM_DESC),
           		new HeaderElement(Gildas30m.BLOCK, block, Gildas30m.BLOCK_DESC),
           		new HeaderElement(Gildas30m.VERSION, version, Gildas30m.VERSION_DESC),
           		new HeaderElement(Gildas30m.SOURCE, source, Gildas30m.SOURCE_DESC),
           		new HeaderElement(Gildas30m.LINE, line, Gildas30m.LINE_DESC),
           		new HeaderElement(Gildas30m.TELES, telescope, Gildas30m.TELES_DESC),
           		new HeaderElement(Gildas30m.LDOBS, GildasDataFormat.getGILDASdate(jdObs), Gildas30m.LDOBS_DESC),
           		new HeaderElement(Gildas30m.LDRED, GildasDataFormat.getGILDASdate(jdRed), Gildas30m.LDRED_DESC),
           		new HeaderElement(Gildas30m.OFF1, offsetX, Gildas30m.OFF1_DESC),
           		new HeaderElement(Gildas30m.OFF2, offsetY, Gildas30m.OFF2_DESC),
           		new HeaderElement(Gildas30m.TYPEC, Gildas30m.COORDINATES_EQUATORIAL, Gildas30m.TYPEC_DESC),
           		new HeaderElement(Gildas30m.KIND, kind, Gildas30m.KIND_DESC),
           		new HeaderElement(Gildas30m.QUALITY, quality, Gildas30m.QUALITY_DESC),
           		new HeaderElement(Gildas30m.SCAN, scanNumber, Gildas30m.SCAN_DESC),
           		new HeaderElement(Gildas30m.POSA, todo, Gildas30m.POSA_DESC),
        });
        return nErr;
	}

    /**
     * Writes the data in the .30m file. Currently the general, position,
     * spectroscopy, baseline, and calibration sections are written.
     * @param sp The spectrums to be written.
     * @param path Path to the output file.
     * @throws JPARSECException If an error occurs.
     */
	public static void writeAs30m(Spectrum30m sp[], String path)
	throws JPARSECException {
		FileOutputStream fos;
		DataOutputStream file;
		try {
			fos = new FileOutputStream(path);
			file = new DataOutputStream( fos );

			byte abyte0[] = new byte[4];
			int off = 0;
			String code = GildasDataFormat.EEEI_CODE; // Only format currently supported
			Convertible convert = GildasDataFormat.getConvertible(code);
			file.write(code.getBytes());

			int ns = sp.length;

			int additionalBlocks = 0;
			if (ns > 384) {
				double n = (ns - 384) / 4.0;
				if (n != (int) n) n ++;
				additionalBlocks = (int) n;
			}

			// Since next free block is by default 99 and the undocumented 'first index' (see below)
			// is limited to two blocks, the number of blocks prior to spectra data is 96. Since each
			// entry in the index has 128 bytes = 1/4 block, the number of index entries is 96 * 4 =
			// 384 at most. To solve this and put more than 384 spectra we use the additionalBlocks
			// variable here... (Thanks for that great documentation!!!)
			int next_free_block = 99 + additionalBlocks;
			for (int i=0; i<sp.length; i++) {
				double l = sp[i].data.length * 4.0 / 512.0;
				if (l != (int) l) l ++;
				// l + 1 = number of blocks of 512 bytes in this spectrum = l blocks of data + 1 of header
				// I know it can be reduced by one in most cases, but it doesn't matter
				next_free_block += (int) l + 2;
			}
			convert.writeInt(abyte0, off, next_free_block);
			file.write(abyte0);
			int ilex = ns; // Constant 384 before, but better put it to the number of spectra to avoid a mess
			convert.writeInt(abyte0, off, ilex);
			file.write(abyte0);
			int imex = 1; // Constant if the number of index extensions is 1 => ilex or less spectra. I'd better avoid index extensions...
			convert.writeInt(abyte0, off, imex);
			file.write(abyte0);
			int next_free_entry = ns + 1;
			convert.writeInt(abyte0, off, next_free_entry);
			file.write(abyte0);

			// One of the unlimited funny things of the 30m format is that here the first index
			// is limited to 251 spectra (and this index is not documented, you have to put 3 in
			// all entries to get it work), but later you have to put all spectra indexes ...
	        byte index_arr[] = new byte[1004]; // 251 * 4 = 2 blocks minus 20 previous bytes
			for(int i = 0; i < 251; i++)
			{
				int j = 3;
				Spectrum30m.writeInt(index_arr, i * 4, j);
			}
			file.write(index_arr);

			// Write index entries
			next_free_block = 99 + additionalBlocks;
			for(int i = 0; i < ns; i++)
			{
				convert.writeInt(abyte0, off, next_free_block);
				file.write(abyte0);

				double l = sp[i].data.length * 4.0 / 512.0;
				if (l != (int) l) l ++;
				next_free_block += (int) l + 2;

				HeaderElement header[] = sp[i].getHeader().getHeaderParameters();
				convert.writeInt(abyte0, off, (HeaderElement.getByKey(header, Gildas30m.NUM)).getAsInt());
				file.write(abyte0);
				convert.writeInt(abyte0, off, (HeaderElement.getByKey(header, Gildas30m.VERSION)).getAsInt());
				file.write(abyte0);
				Spectrum30m.writeBytes(file, convert, off, (HeaderElement.getByKey(header, Gildas30m.SOURCE)).getAsString(), 12);
				Spectrum30m.writeBytes(file, convert, off, (HeaderElement.getByKey(header, Gildas30m.LINE)).getAsString(), 12);
				Spectrum30m.writeBytes(file, convert, off, (HeaderElement.getByKey(header, Gildas30m.TELES)).getAsString(), 12);
				convert.writeInt(abyte0, off, (HeaderElement.getByKey(header, Gildas30m.LDOBS)).getAsInt());
				file.write(abyte0);
				convert.writeInt(abyte0, off, (HeaderElement.getByKey(header, Gildas30m.LDRED)).getAsInt());
				file.write(abyte0);
				convert.writeFloat(abyte0, off, (HeaderElement.getByKey(header, Gildas30m.OFF1)).getAsFloat());
				file.write(abyte0);
				convert.writeFloat(abyte0, off, (HeaderElement.getByKey(header, Gildas30m.OFF2)).getAsFloat());
				file.write(abyte0);
				convert.writeInt(abyte0, off, (HeaderElement.getByKey(header, Gildas30m.TYPEC)).getAsInt());
				file.write(abyte0);
				convert.writeInt(abyte0, off, (HeaderElement.getByKey(header, Gildas30m.KIND)).getAsInt());
				file.write(abyte0);
				convert.writeInt(abyte0, off, (HeaderElement.getByKey(header, Gildas30m.QUALITY)).getAsInt());
				file.write(abyte0);
				convert.writeInt(abyte0, off, (HeaderElement.getByKey(header, Gildas30m.SCAN)).getAsInt());
				file.write(abyte0);
				convert.writeFloat(abyte0, off, (HeaderElement.getByKey(header, Gildas30m.POSA)).getAsFloat());
				file.write(abyte0);
				// Set subscan to 0
				for (int ii=0; ii<8; ii++)
				{
					convert.writeInt(abyte0, off, 0);
					file.write(abyte0);
				}
				Spectrum30m.writeBytes(file, convert, off, "", 12);
			}

			// Complete current block
			int r = ns % 4;
			if (r > 0) {
				r = 4 - r;
				for (int i=0; i < r; i++)
				{
					for (int j=0; j < 32; j++)
					{
						convert.writeInt(abyte0, off, 0);
						file.write(abyte0);
					}
				}
			}

			// Complete up to the beginning of real data
			int bl = 3 + ns / 4;
			if (ns % 4 > 0) bl ++;
			for (int i=bl; i < 99 + additionalBlocks; i++)
			{
				for (int j=0; j < 128; j++)
				{
					convert.writeInt(abyte0, off, 0);
					file.write(abyte0);
				}
			}

			// Write observations
			for(int i = 0; i < ns; i++)
			{
				double l = sp[i].data.length * 4.0 / 512.0;
				if (l != (int) l) l ++;
				int length = ((int) l + 2) * 128;

				int nchan = sp[i].data.length; //sp[i].get(Gildas30m.NCHAN).toInt();

				// Observation header
				Spectrum30m.writeBytes(file, convert, off, "2", 4);
				convert.writeInt(abyte0, off, (int) l + 2);
				file.write(abyte0);
				convert.writeInt(abyte0, off, 143 + nchan - 1); // ?
				file.write(abyte0);
				convert.writeInt(abyte0, off, 0);
				file.write(abyte0);
				convert.writeInt(abyte0, off, 143);
				file.write(abyte0);
				convert.writeInt(abyte0, off, nchan);
				file.write(abyte0);
				convert.writeInt(abyte0, off, 0);
				file.write(abyte0);
				convert.writeInt(abyte0, off, 5); // Number of sections
				file.write(abyte0);
				convert.writeInt(abyte0, off, i+1);
				file.write(abyte0);

				// ID values of the sections to write
				convert.writeInt(abyte0, off, -2); // General
				file.write(abyte0);
				convert.writeInt(abyte0, off, -3); // Position
				file.write(abyte0);
				convert.writeInt(abyte0, off, -4); // Spectroscopy
				file.write(abyte0);
				convert.writeInt(abyte0, off, -14); // Calibration
				file.write(abyte0);
				convert.writeInt(abyte0, off, -5); // Baseline
				file.write(abyte0);

				// Lengths, 1 means 4 bytes => integer, float for example
				int lengths[] = new int[] {20-11, 11, 17, 25, 4};
				convert.writeInt(abyte0, off, lengths[0]);
				file.write(abyte0);
				convert.writeInt(abyte0, off, lengths[1]);
				file.write(abyte0);
				convert.writeInt(abyte0, off, lengths[2]);
				file.write(abyte0);
				convert.writeInt(abyte0, off, lengths[3]);
				file.write(abyte0);
				convert.writeInt(abyte0, off, lengths[4]);
				file.write(abyte0);

				// Addresses, add 3 if a new section is added
				int add = 25;
				convert.writeInt(abyte0, off, add);
				file.write(abyte0);
				convert.writeInt(abyte0, off, add+lengths[0]);
				file.write(abyte0);
				convert.writeInt(abyte0, off, add+lengths[0]+lengths[1]);
				file.write(abyte0);
				convert.writeInt(abyte0, off, add+lengths[0]+lengths[1]+lengths[2]);
				file.write(abyte0);
				convert.writeInt(abyte0, off, add+lengths[0]+lengths[1]+lengths[2]+lengths[3]);
				file.write(abyte0);

				// General section
				//Parameter header[] = sp[i].getHeader().getHeaderParameters();
/*				Spectrum30m.writeBytes(file, convert, off, (Parameter.getByKey(header, Gildas30m.NUM)).getAsInt());
				Spectrum30m.writeBytes(file, convert, off, (Parameter.getByKey(header, Gildas30m.VERSION)).getAsInt());
				Spectrum30m.writeBytes(file, convert, off, (Parameter.getByKey(header, Gildas30m.TELES)).value, 12);
				Spectrum30m.writeBytes(file, convert, off, (Parameter.getByKey(header, Gildas30m.LDOBS)).getAsInt());
				Spectrum30m.writeBytes(file, convert, off, (Parameter.getByKey(header, Gildas30m.LDRED)).getAsInt());
				Spectrum30m.writeBytes(file, convert, off, (Parameter.getByKey(header, Gildas30m.TYPEC)).getAsInt());
				Spectrum30m.writeBytes(file, convert, off, (Parameter.getByKey(header, Gildas30m.KIND)).getAsInt());
				Spectrum30m.writeBytes(file, convert, off, (Parameter.getByKey(header, Gildas30m.QUALITY)).getAsInt());
				Spectrum30m.writeBytes(file, convert, off, (Parameter.getByKey(header, Gildas30m.SCAN)).getAsInt());
//				Spectrum30m.writeBytes(file, convert, off, (Parameter.getByKey(header, Gildas30m.SCAN)).getAsInt());
*/
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.UT_TIME).getAsDouble());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.LST_TIME).getAsDouble());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.AZIMUTH).getAsFloat());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.ELEVATION).getAsFloat());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.TAU).getAsFloat());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.TSYS).getAsFloat());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.INTEG).getAsFloat());
//				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.XUNIT).toInt());

				// Position section
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.SOURCE).getAsString(), 12);
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.EPOCH).getAsFloat());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.LAMBDA).getAsDouble());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.BETA).getAsDouble());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.LAMBDA_OFF).getAsFloat());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.BETA_OFF).getAsFloat());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.PROJECTION).getAsInt());
//				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.algo).toDouble());
//				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.algo).toDouble());
//				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.algo).toDouble());

				// Spectroscopic section
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.LINE).getAsString(), 12);
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.REF_FREQ).getAsDouble());
				Spectrum30m.writeBytes(file, convert, off, nchan);
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.REF_CHAN).getAsFloat());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.FREQ_RESOL).getAsFloat());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.FREQ_OFF).getAsFloat());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.VEL_RESOL).getAsFloat());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.REF_VEL).getAsFloat());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.BAD).getAsFloat());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.IMAGE).getAsDouble());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.VEL_TYPE).getAsInt());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.DOPPLER).getAsDouble());

				// Calibration section
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.BEAM_EFF).getAsFloat());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.FORW_EFF).getAsFloat());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.GAIN_IM).getAsFloat());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.H2OMM).getAsFloat());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.PAMB).getAsFloat());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.TAMB).getAsFloat());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.TATMSIG).getAsFloat());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.TCHOP).getAsFloat());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.TCOLD).getAsFloat());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.TAUSIG).getAsFloat());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.TAUIMA).getAsFloat());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.TATMIMG).getAsFloat());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.TREC).getAsFloat());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.MODE).getAsInt());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.FACTOR).getAsFloat());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.ALTITUDE).getAsFloat());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.COUNT1).getAsFloat());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.COUNT2).getAsFloat());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.COUNT3).getAsFloat());
				float lonoff = 0, latoff = 0;
				try {
					lonoff = sp[i].get(Gildas30m.LONOFF).getAsFloat();
					latoff = sp[i].get(Gildas30m.LATOFF).getAsFloat();
				} catch (Exception exc) {}
				Spectrum30m.writeBytes(file, convert, off, lonoff);
				Spectrum30m.writeBytes(file, convert, off, latoff);
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.LON).getAsDouble());
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.LAT).getAsDouble());

				// Baseline section
				Spectrum30m.writeBytes(file, convert, off, 0);
				Spectrum30m.writeBytes(file, convert, off, sp[i].get(Gildas30m.SIGMA).getAsFloat());
				Spectrum30m.writeBytes(file, convert, off, 0f);
				Spectrum30m.writeBytes(file, convert, off, 0);

				int nzero = 143 - (add+lengths[0]+lengths[1]+lengths[2]+lengths[3]+lengths[4]);
				for (int j=0; j<nzero; j++)
				{
					Spectrum30m.writeBytes(file, convert, off, 0);
				}

				// Data section
				byte b[] = new byte[4*sp[i].data.length];
				byte b0[] = new byte[4];
				for (int j=0; j<sp[i].data.length; j++)
				{
					convert.writeFloat(b0, off, sp[i].data[j]);
					int n = j*4;
					b[n] = b0[0];
					b[n+1] = b0[1];
					b[n+2] = b0[2];
					b[n+3] = b0[3];
					//Spectrum30m.writeBytes(file, convert, off, sp[i].data[j]);
				}
				file.write(b);

				nzero = length - (143 + sp[i].data.length) + 1;
				if (nzero > 0) {
					b = new byte[4*nzero];
					for (int j=0; j<nzero; j++)
					{
						convert.writeInt(b0, off, 0);
	                	int n = j*4;
	                	b[n] = b0[0];
	                	b[n+1] = b0[1];
	                	b[n+2] = b0[2];
	                	b[n+3] = b0[3];
//						Spectrum30m.writeBytes(file, convert, off, 0);
					}
					file.write(b);
				}
			}
		} catch (JPARSECException e) {
			throw e;
		} catch (Exception ioe) {
			ioe.printStackTrace();
			throw new JPARSECException(ioe);
		}
	}

	private static void writeBytes(DataOutputStream file, Convertible convert, int off, int value)
	throws Exception {
		byte abyte0[] = new byte[4];
		convert.writeInt(abyte0, off, value);
		file.write(abyte0);
	}
	private static void writeBytes(DataOutputStream file, Convertible convert, int off, double value)
	throws Exception {
		byte abyte0[] = new byte[8];
		convert.writeDouble(abyte0, off, value);
		file.write(abyte0);
	}
	private static void writeBytes(DataOutputStream file, Convertible convert, int off, float value)
	throws Exception {
		byte abyte0[] = new byte[4];
		convert.writeFloat(abyte0, off, value);
		file.write(abyte0);
	}
	private static void writeBytes(DataOutputStream file, Convertible convert, int off, String value, int nchar)
	throws Exception {
		if (value.length() > nchar) {
			value = value.substring(0, nchar);
		} else {
			value = FileIO.addSpacesAfterAString(value, nchar);
		}
		file.write(value.getBytes());
	}

	private static void writeInt(byte[] array, int offset, int value) {
		array[offset] =     (byte)(value >> 24);
		array[offset + 1] = (byte)(value >> 16);
		array[offset + 2] = (byte)(value >> 8);
		array[offset + 3] = (byte)value;
	}

	/**
	 * Resamples this spectrum to the resolution and velocity range of another one.
	 * All new values outside the velocity window currently covered are set to 0.
	 * @param s30m The reference spectrum to resample to.
	 * @throws JPARSECException If an error occurs.
	 */
	public void resample(Spectrum30m s30m) throws JPARSECException {
		double refchan = (s30m.get(Gildas30m.REF_CHAN)).getAsDouble();
		double vref = (s30m.get(Gildas30m.REF_VEL)).getAsDouble();
		double vres = (s30m.get(Gildas30m.VEL_RESOL)).getAsDouble();
		int nchan = s30m.getNumberOfChannels();

    	double refchan0 = (this.get(Gildas30m.REF_CHAN)).getAsDouble();
    	double vref0 = (this.get(Gildas30m.REF_VEL)).getAsDouble();
    	double vres0 = (this.get(Gildas30m.VEL_RESOL)).getAsDouble();

		float newData[] = new float[nchan];
		int nchanOld = this.getNumberOfChannels();
		Interpolation interp = new Interpolation(
				DataSet.getSetOfValues(1, nchanOld, nchanOld, false),
				DataSet.toDoubleArray(this.getSpectrumData()), false);
		for (int i=0; i<nchan; i++) {
			newData[i] = 0;
			double v = vref + ((i+1) - refchan) * vres; // s30m.getVelocity(i+1)
			double n = (v - vref0) / vres0 + refchan0; // this.getChannel(v);
			if (n >= 1 && n <= nchanOld) newData[i] = (float) interp.splineInterpolation(n);
		}

		double freq = this.getFrequencyForAGivenVelocity(vref);
		double imgFreq = this.getImageFrequencyForAGivenVelocity(vref);

		this.setSpectrumData(newData);
		this.put(Gildas30m.REF_CHAN, new HeaderElement(refchan, Gildas30m.REF_CHAN_DESC));
		this.put(Gildas30m.REF_VEL, new HeaderElement(vref, Gildas30m.REF_VEL_DESC));
		this.put(Gildas30m.VEL_RESOL, new HeaderElement(vres, Gildas30m.VEL_RESOL_DESC));
		this.put(Gildas30m.REF_FREQ, new HeaderElement(freq, Gildas30m.REF_FREQ_DESC));
		this.put(Gildas30m.IMAGE, new HeaderElement(imgFreq, Gildas30m.IMAGE_DESC));
	}

	/**
	 * Resamples this spectrum to the resolution and velocity range of another one.
	 * All new values outside the velocity window currently covered are set to 0.
	 * This methods allows to set a more restricted velocity interval to speedup the 
	 * resampling when the spectra has many channels and only some of them will be 
	 * later useful. The velocity interval corresponds to the input spectrum used as 
	 * reference.
	 * @param s30m The reference spectrum to resample to.
	 * @param v0 Lower limit for the velocity interval.
	 * @param vf Upper limit for the velocity interval.
	 * @param spline True to use spline interpolation in the resampling, false for linear 
	 * interpolation.
	 * @throws JPARSECException If an error occurs.
	 */
	public void resampleInVelocityInterval(Spectrum30m s30m, double v0, double vf, 
			boolean spline) throws JPARSECException {
		double refchan = (s30m.get(Gildas30m.REF_CHAN)).getAsDouble();
		double vref = (s30m.get(Gildas30m.REF_VEL)).getAsDouble();
    	double vres = (s30m.get(Gildas30m.VEL_RESOL)).getAsDouble();
		int nchan = s30m.getNumberOfChannels();

    	double refchan0 = (this.get(Gildas30m.REF_CHAN)).getAsDouble();
    	double vref0 = (this.get(Gildas30m.REF_VEL)).getAsDouble();
    	double vres0 = (this.get(Gildas30m.VEL_RESOL)).getAsDouble();

		float newData[] = new float[nchan];
		int nchanOld = this.getNumberOfChannels();
		Interpolation interp = new Interpolation(
				DataSet.getSetOfValues(1, nchanOld, nchanOld, false),
				DataSet.toDoubleArray(this.getSpectrumData()), false);
		for (int i=0; i<nchan; i++) {
			newData[i] = 0;
			double v = vref + ((i+1) - refchan) * vres; // s30m.getVelocity(i+1)
			if (v < v0 || v > vf) continue;
			double n = (v - vref0) / vres0 + refchan0; // this.getChannel(v);
			if (n >= 1 && n <= nchanOld) {
				if (!spline) {
					newData[i] = (float) interp.linearInterpolation(n);
				} else {
					newData[i] = (float) interp.splineInterpolation(n);
				}
			}
		}

		double freq = this.getFrequencyForAGivenVelocity(vref);
		double imgFreq = this.getImageFrequencyForAGivenVelocity(vref);

		this.setSpectrumData(newData);
		this.put(Gildas30m.REF_CHAN, new HeaderElement(refchan, Gildas30m.REF_CHAN_DESC));
		this.put(Gildas30m.REF_VEL, new HeaderElement(vref, Gildas30m.REF_VEL_DESC));
		this.put(Gildas30m.VEL_RESOL, new HeaderElement(vres, Gildas30m.VEL_RESOL_DESC));
		this.put(Gildas30m.REF_FREQ, new HeaderElement(freq, Gildas30m.REF_FREQ_DESC));
		this.put(Gildas30m.IMAGE, new HeaderElement(imgFreq, Gildas30m.IMAGE_DESC));
	}
	
	/**
	 * Crops the spectrum and leaves only the data in a given channel range.
	 * @param chan0 Initial channel, first is 1.
	 * @param chanf Last channel.
     * @throws JPARSECException If the fields required to compute the return are not numbers.
	 */
	public void crop(int chan0, int chanf) throws JPARSECException {
		if (chan0 > chanf) {
			int tmp = chan0;
			chan0 = chanf;
			chanf = tmp;
		}
		if (chan0 > 1) {
			double refchan = (get(Gildas30m.REF_CHAN)).getAsDouble();
			refchan -= (chan0 - 1.0);
			this.put(Gildas30m.REF_CHAN, new HeaderElement(refchan, Gildas30m.REF_CHAN_DESC));
		}
		if (chanf >= data.length) chanf = data.length;
		if (chan0 > 1 || chanf < data.length)
			this.setSpectrumData(DataSet.getSubArray(data, chan0-1, chanf-1));
	}

	/**
	 * Returns the observation date in UT.
	 * @return Observation date in UT.
	 * @throws JPARSECException In case the date value is not available.
	 */
	public AstroDate getObservationDate() throws JPARSECException {
		String d = (get(Gildas30m.LDOBS)).getAsString();
		if (d == null || d.equals("") || !DataSet.isDoubleStrictCheck(d)) {
			d = getHeader().getHeaderParameters()[6].getAsString();
			if (!DataSet.isDoubleStrictCheck(d))
				throw new JPARSECException("Value for observation date is not available");
		}
		int date = Integer.parseInt(d);
		String ut = (get(Gildas30m.UT_TIME)).getAsString();
		if (ut == null || ut.equals("") || !DataSet.isDoubleStrictCheck(ut))
			throw new JPARSECException("Value for observation time is not available");
		
		double time = Double.parseDouble(ut);
		AstroDate astro = GildasDataFormat.getAstroDate(date);
		astro.add(time / Constant.TWO_PI);
		return astro;
	}

	private double getDoppler() {
    	// Compute doppler since Gildas always set doppler to 0 in the header
		try {
			TimeElement time = new TimeElement(getObservationDate(), SCALE.UNIVERSAL_TIME_UTC);
			ObserverElement obs = ObserverElement.parseObservatory(Observatory.findObservatorybyName("Veleta", COUNTRY.Spain));
			EphemerisElement eph = new EphemerisElement(TARGET.NOT_A_PLANET, EphemerisElement.COORDINATES_TYPE.APPARENT,
					EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.TOPOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
					EphemerisElement.FRAME.DYNAMICAL_EQUINOX_J2000, EphemerisElement.ALGORITHM.MOSHIER);
			double ra = (get(Gildas30m.LAMBDA)).getAsDouble();
			double dec = (get(Gildas30m.BETA)).getAsDouble();
			LocationElement locEq = new LocationElement(ra, dec, 1);

			double JD_TDB = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
			double geo_sun[] = Ephem.eclipticToEquatorial(PlanetEphem.getGeocentricPosition(JD_TDB, TARGET.Solar_System_Barycenter, 0.0, false, obs), Constant.J2000, eph);
			
			/*
			double eqRotMod = Constant.EARTH_MEAN_ROTATION_RATE * obs.getEllipsoid().getEquatorialRadius() * 
					Math.cos(obs.getLatitudeRad()) / (Constant.AU / Constant.SECONDS_PER_DAY);
			double eqRot[] = CoordinateSystem.horizontalToEquatorial(new LocationElement(Constant.PI_OVER_TWO, 0, eqRotMod), time, obs, eph).getRectangularCoordinates();
			geo_sun[3] += eqRot[0];
			geo_sun[4] += eqRot[1];
			geo_sun[5] += eqRot[2];
			*/
			
			double doppler = Functions.scalarProduct(new double[] {geo_sun[3], geo_sun[4], geo_sun[5]}, locEq.getRectangularCoordinates());
			double voff = -(get(Gildas30m.REF_VEL)).getAsDouble();
			StarElement star = new StarElement("", ra, dec, 0, 0, 0, 0, (float) voff, Constant.J2000, FRAME.DYNAMICAL_EQUINOX_J2000);
			double lsr = StarEphem.getLSRradialVelocity(time, obs, eph, star);
			doppler = doppler * (Constant.AU / Constant.SECONDS_PER_DAY) + lsr;
			doppler /= (Constant.SPEED_OF_LIGHT * 0.001);
			
			return doppler;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return 0;
	}
	
	/**
	 * Modifies the rest frequency for this spectrum.
	 * @param freq The new rest frequency in MHz.
     * @throws JPARSECException If the fields required to compute the return are not numbers.
	 */
	public void modifyRestFrequency(double freq) throws JPARSECException {
		double restFreq = (get(Gildas30m.REF_FREQ)).getAsDouble();
		if (freq != restFreq) {
	    	double fres = - this.getVelocityResolution() * restFreq / (Constant.SPEED_OF_LIGHT * 0.001);
			double vres = -fres * (Constant.SPEED_OF_LIGHT * 0.001) / freq;
	    	double doppler = getDoppler();

			/*
	    	double doppler = 0.0;
	    	if (!keyExists(Gildas30m.DOPPLER)) {
	    		JPARSECException.addWarning("Spectrum with an unknown doppler factor.");
	    	} else {
	    		doppler = (get(Gildas30m.DOPPLER)).getAsDouble();
	    	}
	    	*/
			double refchan = getReferenceChannel() + ((freq - restFreq) / fres) * (1.0 + doppler);

			this.put(Gildas30m.VEL_RESOL, new HeaderElement(vres, Gildas30m.VEL_RESOL_DESC));
			this.put(Gildas30m.REF_FREQ, new HeaderElement(freq, Gildas30m.REF_FREQ_DESC));
			this.put(Gildas30m.REF_CHAN, new HeaderElement(refchan, Gildas30m.REF_CHAN_DESC));
			double img = (get(Gildas30m.IMAGE)).getAsDouble();
			this.put(Gildas30m.IMAGE, new HeaderElement(img+(restFreq-freq), Gildas30m.IMAGE_DESC));
		}
	}

	/**
	 * Modifies the LSR velocity of the spectrum.
	 * @param vel The new LSR velocity.
	 * @throws JPARSECException If an error occurs.
	 */
	public void modifyVelocityLSR(double vel) throws JPARSECException {
		double vref = (get(Gildas30m.REF_VEL)).getAsDouble();
		if (vel != vref) {
			double restFreq = (get(Gildas30m.REF_FREQ)).getAsDouble();
	    	double fres = - this.getVelocityResolution() * restFreq / (Constant.SPEED_OF_LIGHT * 0.001);

	    	double doppler0 = 0.0;
	    	if (!keyExists(Gildas30m.DOPPLER)) {
	    		JPARSECException.addWarning("Spectrum with an unknown doppler factor.");
	    	} else {
	    		doppler0 = (get(Gildas30m.DOPPLER)).getAsDouble();
	    		if (doppler0 == -1) doppler0 = getDoppler();
	    	}
			double dopplerAdd = (vref - vel) / (0.001 * Constant.SPEED_OF_LIGHT);
			double doppler = doppler0 + dopplerAdd;
			double refchan = getReferenceChannel() + (restFreq / fres) * dopplerAdd;

			this.put(Gildas30m.DOPPLER, new HeaderElement(doppler, Gildas30m.DOPPLER_DESC));
			this.put(Gildas30m.REF_VEL, new HeaderElement(vel, Gildas30m.REF_VEL_DESC));
			this.put(Gildas30m.REF_CHAN, new HeaderElement(refchan, Gildas30m.REF_CHAN_DESC));
			double img = (get(Gildas30m.IMAGE)).getAsDouble();
		    img = img * (1.0 + doppler0) / (1.0 + doppler) - restFreq * dopplerAdd / (1.0 + doppler);
			this.put(Gildas30m.IMAGE, new HeaderElement(img, Gildas30m.IMAGE_DESC));
		}
	}

	@Override
	public String toString() {
		Set<String> ks = map.keySet();
		Object[] keys = ks.toArray();
    	String sep = FileIO.getLineSeparator();
    	StringBuffer out = new StringBuffer("");
		for (int i=0; i<keys.length; i++) {
			HeaderElement p = map.get(keys[i]);

    		String d = "";
    		if (p.comment != null && p.comment.length() > 0)
    			d = " ("+p.comment+")";
    		out.append(keys[i].toString()+" = "+p.getAsString() + d + sep);
		}
		return out.toString();
	}
}
