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
package jparsec.util;

import java.io.Serializable;
import java.awt.image.*;
import java.awt.Dimension;

import jparsec.io.Serialization;
import jparsec.io.image.*;

/**
 * A class to hold data for the release of a JPARSEC application or JPARSEC itself.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Module implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Application name.
	 */
	public String name;
	/**
	 * Libraries.
	 */
	public String lib[];
	/**
	 * Version of the libraries.
	 */
	public float libVersion[];
	/**
	 * Web link to download.
	 */
	public String webLink;
	/**
	 * Launch class path.
	 */
	public String launchClass;
	/**
	 * Release notes.
	 */
	public String releaseNotes[];
	/**
	 * Required JRE version.
	 */
	public String jre;
	/**
	 * The icon.
	 */
	public int[][] icon;
	/**
	 * Program author.
	 */
	public String author;
	/**
	 * Group name for the application. Constants defined in this class.
	 */
	public String group;

	/**
	 * Full constructor;
	 * @param nom Name.
	 * @param l Libraries.
	 * @param v Versions.
	 * @param link Link.
	 * @param launch Launch class.
	 * @param rel Release.
	 * @param jre JRE version.
	 * @param img The icon. Can be null.
	 * @param author Author.
	 * @throws JPARSECException If an error occurs.
	 */
	public Module(String nom, String l[], float v[], String link, String launch, String rel[],
			String jre, BufferedImage img, String author)
	throws JPARSECException {
		this.name = nom;
		this.jre = jre;
		this.lib = l;
		this.libVersion = v;
		this.launchClass = launch;
		this.releaseNotes = rel;
		this.author = author;
		this.webLink = link;

		if (img == null) {
			this.icon = null;
		} else {
			Picture pic = new Picture(img);
			Dimension d = pic.getSize();
			this.icon = new int[d.width][d.height];
			for (int i=0; i<d.width; i++)
			{
				for (int j=0; j<d.height; j++)
				{
					this.icon[i][j] = pic.getColorAt(i, j).getRGB();
				}
			}
		}
	}

	/**
	 * Full constructor;
	 * @param nom Name.
	 * @param l Libraries.
	 * @param v Versions.
	 * @param link Link.
	 * @param launch Launch class.
	 * @param rel Release.
	 * @param jre JRE version.
	 * @param img The icon. Can be null.
	 * @param author Author.
	 * @param group Group name.
	 * @throws JPARSECException If an error occurs.
	 */
	public Module(String nom, String l[], float v[], String link, String launch, String rel[],
			String jre, BufferedImage img, String author, String group)
	throws JPARSECException {
		this.name = nom;
		this.jre = jre;
		this.lib = l;
		this.libVersion = v;
		this.launchClass = launch;
		this.releaseNotes = rel;
		this.author = author;
		this.webLink = link;
		this.group = group;

		if (img == null) {
			this.icon = null;
		} else {
			Picture pic = new Picture(img);
			Dimension d = pic.getSize();
			this.icon = new int[d.width][d.height];
			for (int i=0; i<d.width; i++)
			{
				for (int j=0; j<d.height; j++)
				{
					this.icon[i][j] = pic.getColorAt(i, j).getRGB();
				}
			}
		}
	}

	/**
	 * Returns the icon image.
	 * @return The icon image, or null if it does not exist.
	 */
	public BufferedImage getImage()
	{
		if (this.icon == null) return null;
		Dimension d = new Dimension(this.icon.length, this.icon[0].length);
		Picture p = new Picture(d.width, d.height);
		for (int i=0; i<d.width; i++)
		{
			for (int j=0; j<d.height; j++)
			{
				p.setColor(i, j, this.icon[i][j]);
			}
		}
		return p.getImage();
	}

	/**
	 * Creates a binary file with the current instance.
	 * @param path The path.
	 * @throws JPARSECException If an error occurs.
	 */
	public void writeBinaryFile(String path)
	throws JPARSECException {
		Serialization.writeObject(this, path);
	}

	/**
	 * Shared name for a group of applications for the same field.
	 */
	public static final String GROUP_STAR_FORMATION = "Star Formation";
	/**
	 * Shared name for a group of applications for the same field.
	 */
	public static final String GROUP_GENERAL_ASTRONOMY = "Popular astronomy";
	/**
	 * Shared name for a group of applications for the same field.
	 */
	public static final String GROUP_RADIOASTRONOMY = "Radio astronomy";
	/**
	 * Shared name for a group of applications for the same field.
	 */
	public static final String GROUP_INSTRUMENTS = "Instrumentation";
	/**
	 * Shared name for a group of applications for the same field.
	 */
	public static final String GROUP_RADIATIVE_TRANSFER = "Radiative transfer";
}
