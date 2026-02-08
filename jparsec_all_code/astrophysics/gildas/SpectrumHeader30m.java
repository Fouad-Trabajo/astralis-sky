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

import java.io.Serializable;
import java.util.Arrays;

import jparsec.io.FileIO;
import jparsec.io.image.HeaderElement;

/**
 * A class to hold the header of a spectrum (.30m files).
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class SpectrumHeader30m
    implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor for a given set of objects.
	 * @param aobj Input objects.
	 */
    public SpectrumHeader30m(HeaderElement aobj[])
    {
        header = aobj.clone();
        visibleHeader = getVisibleHeader();
    }

    /**
     * Returns the header as a set of 15 objects.
     * Values from index 0 are obs num (0), block (1), obs version (2),
     * source name (3), line name (4), telescope (5), obs date (6),
     * reduction date (7), offset RA (8, rad), offset DEC (9, rad), coord type (10),
     * kind (11), quality (12), scan number (13), pos angle (14, in deg).
     * @return  The header.
     */
    public HeaderElement[] getHeaderParameters()
    {
        return header.clone();
    }

    /**
     * Returns the visible header as 8 parameters, i.e., those fields that
     * are visible in GILDAS when a spectrum is drawn.
     * Values from index 0 are obs num (0), obs version (1),
     * source name (2), line name (3), telescope (4), offset RA
     * (5, in rad), offset DEC (6, in rad), scan number (7).
     * @return The 8 main header values.
     */
    public HeaderElement[] getVisibleHeader()
    {
        if(visibleHeader == null)
        {
            HeaderElement aobj[] = new HeaderElement[8];
            aobj[0] = header[0];
            aobj[1] = header[2];
            aobj[2] = header[3];
            aobj[3] = header[4];
            aobj[4] = header[5];
            aobj[5] = header[8];
            aobj[6] = header[9];
            aobj[7] = header[13];
            visibleHeader = aobj;
        }
        return visibleHeader;
    }

    private HeaderElement header[];
    private HeaderElement visibleHeader[];
    /**
     * The set of header parameters.
     */
    public enum HEADER {
	    /** ID constant for the index of the observation number. */
	    NUM,
	    /** ID constant for the index of the block. */
	    BLOCK,
	    /** ID constant for the index of the version number. */
	    VERSION,
	    /** ID constant for the index of the source name. */
	    SOURCE,
	    /** ID constant for the index of the line name. */
	    LINE,
	    /** ID constant for the index of the telescope name. */
	    TELES,
	    /** ID constant for the index of the observation date. */
	    DATE_OBS,
	    /** ID constant for the index of the reduction date. */
	    DATE_RED,
	    /** ID constant for the index of the x offset. */
	    OFFSET1,
	    /** ID constant for the index of the y offset. */
	    OFFSET2,
	    /** ID constant for the index of the coordinate type. */
	    TYPEC,
	    /** ID constant for the index of the kind. */
	    KIND,
	    /** ID constant for the index of the quality. */
	    QUALITY,
	    /** ID constant for the index of the scan number. */
	    SCAN,
	    /** ID constant for the index of the position angle. */
	    POSA
    };

    /**
     * The set of 'visible' header parameters, those that appear
     * when showing a spectrum in Gildas in long format.
     */
    public enum VISIBLE_HEADER {
	    /** ID constant for the index of the observation number in the visible header. */
	    VISIBLE_NUM,
	    /** ID constant for the index of the version number in the visible header. */
	    VISIBLE_VERSION,
	    /** ID constant for the index of the source name in the visible header. */
	    VISIBLE_SOURCE,
	    /** ID constant for the index of the line name in the visible header. */
	    VISIBLE_LINE,
	    /** ID constant for the index of the telescope name in the visible header. */
	    VISIBLE_TELES,
	    /** ID constant for the index of the x offset in the visible header. */
	    VISIBLE_OFFSET1,
	    /** ID constant for the index of the y offset in the visible header. */
	    VISIBLE_OFFSET2,
	    /** ID constant for the index of the scan number in the visible header. */
	    VISIBLE_SCAN
    };

    /**
     * Check if two instances are the same.
     */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof SpectrumHeader30m)) return false;

		SpectrumHeader30m that = (SpectrumHeader30m) o;

		if (!Arrays.equals(header, that.header)) return false;

		return Arrays.equals(visibleHeader, that.visibleHeader);
	}

	@Override
	public int hashCode() {
		int result = header != null ? Arrays.hashCode(header) : 0;
		result = 31 * result + (visibleHeader != null ? Arrays.hashCode(visibleHeader) : 0);
		return result;
	}

    /**
     * Clones this instance.
     */
	@Override
    public SpectrumHeader30m clone()
    {
    	HeaderElement p[] = new HeaderElement[header.length];
    	for (int i=0; i<p.length; i++) {
    		p[i] = header[i].clone();
    	}
    	SpectrumHeader30m s = new SpectrumHeader30m(p);
    	if (this.visibleHeader != null) {
        	HeaderElement pp[] = new HeaderElement[visibleHeader.length];
        	for (int i=0; i<pp.length; i++) {
        		pp[i] = visibleHeader[i].clone();
        	}
    		s.visibleHeader = pp;
    	}
    	return s;
    }

    @Override
    public String toString() {
    	String sep = FileIO.getLineSeparator();
    	StringBuffer out = new StringBuffer("");
    	HeaderElement[] header = this.getHeaderParameters();
    	HEADER h[] = HEADER.values();
    	for (int i=0; i<header.length; i++) {
    		String d = "";
    		if (header[i].comment != null && header[i].comment.length() > 0)
    			d = " ("+header[i].comment+")";
    		out.append(h[i].toString()+" = "+header[i].getAsString() + d + sep);
    	}
    	return out.toString();
    }
}
