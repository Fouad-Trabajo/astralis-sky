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
package jparsec.vo;

import jparsec.graph.DataSet;

/**
 * A class to hold a feed message. Based on the tutorial at
 * http://www.vogella.de/articles/RSSFeed/article.html.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class FeedMessageElement {
	/** Title of the message. */
	public String title;
	/** Description of the message. */
	public String description;
	/** The link associated to the message. */
	public String link;
	/** The author. */
	public String author;
	/** The guid identifier. */
	public String guid;
	/** The publication date of this message. */
	public String pubDate;
	/** A set of links to images inside the message. Each must be an http url. */
	public String imageURL[];

	/**
	 * Constructor for a new message.
	 * @param t Title.
	 * @param d Description.
	 * @param l Link.
	 * @param a Author.
	 * @param g Guid.
	 */
	public FeedMessageElement(String t, String d, String l, String a,
			String g) {
		title = t;
		description = d;
		link = l;
		author = a;
		guid = g;
	}

	/**
	 * Full constructor for a new message.
	 * @param t Title.
	 * @param d Description.
	 * @param l Link.
	 * @param a Author.
	 * @param g Guid.
	 * @param img The set of http urls for images inside the message.
	 * @param pDate Publication date.
	 */
	public FeedMessageElement(String t, String d, String l, String a,
			String g, String img[], String pDate) {
		title = t;
		description = d;
		link = l;
		author = a;
		guid = g;
		if (img != null && img.length > 0) imageURL = img.clone();
		pubDate = pDate;
	}

	@Override
	public String toString() {
		String out = "FeedMessage [title=" + title + ", description=" + description
				+ ", link=" + link + ", author=" + author + ", guid=" + guid;
		if (imageURL != null && imageURL.length > 0) out += ", images=" + DataSet.toString(imageURL, ", ");
		if (pubDate != null) out += ", pubDate="+pubDate;
		out +=  "]";
		return out;
	}
}
