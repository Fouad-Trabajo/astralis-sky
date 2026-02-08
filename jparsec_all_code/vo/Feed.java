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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import jparsec.graph.DataSet;
import jparsec.io.ReadFile;
import jparsec.io.WriteFile;
import jparsec.util.JPARSECException;
import jparsec.util.Version;

/**
 * A class to read and write RSS feeds. Based on the tutorial at
 * http://www.vogella.de/articles/RSSFeed/article.html
 * Images are supported in messages using enclosures.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 * @since 1.85
 */
public class Feed {

	private static final String RSS = "rss";
	private static final String VERSION = "version";
	private static final String URL = "url";

	private static final String TITLE = "title";
	private static final String DESCRIPTION = "description";
	private static final String CHANNEL = "channel";
	private static final String LANGUAGE = "language";
	private static final String COPYRIGHT = "copyright";
	private static final String LINK = "link";
	private static final String AUTHOR = "author";
	private static final String ITEM = "item";
	private static final String PUB_DATE = "pubDate";
	private static final String GUID = "guid";
	private static final String ENCLOSURE = "enclosure";
	private static final String LAST_BUILD = "lastBuildDate";
	private static final String GENERATOR = "generator";
	private static final String EDITOR = "managingEditor";
	private static final String WEB_MASTER = "webMaster";
	private static final String TTL = "ttl";
	private static final String IMAGE = "image";

	private final String title;
	private final String link;
	private final String description;
	private final String language;
	private final String copyright;
	private final String pubDate;
	private final String lastBuildDate;
	private final String managingEditor;
	private final String webMaster;
	private String generator;
	private final String ttl;
	private String imgURL = null;

	private ArrayList<FeedMessageElement> entries = new ArrayList<FeedMessageElement>();

	/**
	 * Full constructor for a new feed (without an image).
	 * @param title The title.
	 * @param link The main link to the site.
	 * @param description The common description of the posts.
	 * @param language The language abbreviation, en, es, and so on.
	 * @param copyright Copyright message.
	 * @param pubDate Publication date (beginning of the feed) string message. For instance,
	 * Fri, 29 Jul 2011 23:30:44 +0200.
	 * @param lastBuild Date of the last build of the feed. Can be null.
	 * @param editor Email of the editor. Can be null.
	 * @param webMaster Email of the web master. Can be null.
	 * @param ttl Number of minutes the feed can be cached before updating. Set to 0 to ignore this parameter.
	 */
	public Feed(String title, String link, String description, String language,
			String copyright, String pubDate, String lastBuild, String editor, String webMaster, int ttl) {
		this.title = title;
		this.link = link;
		this.description = description;
		this.language = language;
		this.copyright = copyright;
		this.pubDate = pubDate;
		this.lastBuildDate = lastBuild;
		this.managingEditor = editor;
		this.webMaster = webMaster;
		this.generator = Version.PACKAGE_NAME+" v"+Version.VERSION_ID;
		if (ttl > 0) {
			this.ttl = ""+ttl;
		} else {
			this.ttl = null;
		}
	}

	/**
	 * Constructor for a new feed.
	 * @param title The title.
	 * @param link The main link to the site.
	 * @param description The common description of the posts.
	 * @param language The language abbreviation, en, es, and so on.
	 * @param copyright Copyright message.
	 * @param pubDate Publication date (beginning of the feed) string message. For instance,
	 * Fri, 29 Jul 2011 23:30:44 +0200.
	 */
	public Feed(String title, String link, String description, String language,
			String copyright, String pubDate) {
		this.title = title;
		this.link = link;
		this.description = description;
		this.language = language;
		this.copyright = copyright;
		this.pubDate = pubDate;
		this.generator = Version.PACKAGE_NAME+" v"+Version.VERSION_ID;
		this.ttl = null;
		this.lastBuildDate = null;
		this.managingEditor = null;
		this.webMaster = null;
	}

	/**
	 * Returns the list of messages in the feed.
	 * @return The list of messages.
	 */
	public ArrayList<FeedMessageElement> getMessages() {
		return entries;
	}

	/**
	 * Sets the list of messages in the feed.
	 * @param messages The new list of messages.
	 */
	public void setMessages(ArrayList<FeedMessageElement> messages) {
		for (int i=0; i<entries.size(); i++) {
			entries.remove(entries.size()-1);
		}

		for (int i=0; i<messages.size(); i++) {
			entries.add(messages.get(i));
		}
	}

	/**
	 * Returns the title of the feed.
	 * @return The title.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Returns the main link to the posts.
	 * @return Main link.
	 */
	public String getLink() {
		return link;
	}

	/**
	 * Returns the common description for the messages in the feed.
	 * @return The feed description.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * The language abbreviation for the feed.
	 * @return es, en, en-us, and so on.
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * The copyright message for the feed content.
	 * @return Copyright message.
	 */
	public String getCopyright() {
		return copyright;
	}

	/**
	 * The publication date in a given format, for
	 * instance Fri, 29 Jul 2011 23:30:44 +0200.
	 * @return Publication date string.
	 */
	public String getPubDate() {
		return pubDate;
	}

	/**
	 * The last build date in a given format, for
	 * instance Fri, 29 Jul 2011 23:30:44 +0200.
	 * @return Last build date string.
	 */
	public String getLastBuildDate() {
		return lastBuildDate;
	}

	/**
	 * The email of the responsible of the content.
	 * @return The managing editor.
	 */
	public String getManagingEditor() {
		return managingEditor;
	}

	/**
	 * The email of the responsible of the technical issues.
	 * @return The web master.
	 */
	public String getWebMaster() {
		return webMaster;
	}

	/**
	 * The generator of the feed.
	 * @return The generator.
	 */
	public String getGenerator() {
		return generator;
	}

	/**
	 * The cached time.
	 * @return The maximum cached time before reading the feed again in minutes.
	 */
	public String getCachedTime() {
		return ttl;
	}

	/**
	 * 	Sets an image to the feed. Maximum width is 144, default value is 88.
	 * Maximum height is 400, default value is 31.
	 * @param url The url of the image, or null.
	 */
	public void setFeedImage(String url) {
		imgURL = url;
	}

	/**
	 * Returns the url of the image of this feed.
	 * @return Image url.
	 */
	public String getFeedImage() {
		return imgURL;
	}

	@Override
	public String toString() {
		String out = "Feed [copyright=" + copyright + ", description=" + description
				+ ", language=" + language + ", link=" + link + ", pubDate="
				+ pubDate + ", title=" + title;
		if (lastBuildDate != null) out += ", lastBuild="+lastBuildDate;
		if (managingEditor != null) out += ", managingEditor="+managingEditor;
		if (webMaster != null) out += ", webMaster="+webMaster;
		if (generator != null) out += ", generator="+generator;
		if (ttl != null) out += ", ttl="+ttl;
		out += "]";
		return out;
	}

	private static String fix(String label) {
		if (label == null) return label;
		label = DataSet.replaceAll(label, "JPARSEC_L", "<", true);
		label = DataSet.replaceAll(label, "JPARSEC_U", ">", true);
		label = DataSet.replaceAll(label, "JPARSEC_A", "&", true);
		return label;
	}

	/**
	 * Reads a feed from a given URL.
	 * @param feedUrl The feed URL.
	 * @return The feed.
	 * @throws JPARSECException If an error occurs.
	 */
	public static Feed readFeed(URL feedUrl) throws JPARSECException {
		String txt = DataSet.arrayListToString(ReadFile.readAnyExternalFile(feedUrl, ReadFile.ENCODING_UTF_8));
		return readFeed(txt);
	}
	
	/**
	 * Reads a feed from a given text.
	 * @param txt The feed raw text.
	 * @return The feed.
	 * @throws JPARSECException If an error occurs.
	 */
	public static Feed readFeed(String txt) throws JPARSECException {
		Feed feed = null;
		try {

			boolean isFeedHeader = true;
			// Set header values intial to the empty string
			String description = "";
			String title = "";
			String link = "";
			String language = "";
			String copyright = "";
			String author = "";
			String pubdate = "";
			String guid = "";
			String lastBuild = null, generator = null, editor = null, master = null, ttl = "0", img = null;
			String imgs[] = new String[0];

			// First create a new XMLInputFactory
			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			// Setup a new eventReader
			txt = DataSet.replaceAll(txt, "&lt;", "JPARSEC_L", true);
			txt = DataSet.replaceAll(txt, "&gt;", "JPARSEC_U", true);
			txt = DataSet.replaceAll(txt, "&amp;", "JPARSEC_A", true);

			InputStream in = new ByteArrayInputStream(txt.getBytes(ReadFile.ENCODING_UTF_8));
			//InputStream in = feedUrl.openStream(); // Problems with &lt; and &gt;

			XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
			// Read the XML document
			while (eventReader.hasNext()) {

				XMLEvent event = eventReader.nextEvent();

				if (event.isStartElement()) {
					if (event.asStartElement().getName().getLocalPart() == (ITEM)) {
						if (isFeedHeader) {
							isFeedHeader = false;
							title = fix(title);
							link = fix(link);
							description = fix(description);
							language = fix(language);
							copyright = fix(copyright);
							pubdate = fix(pubdate);
							lastBuild = fix(lastBuild);
							editor = fix(editor);
							master = fix(master);
							generator = fix(generator);
							feed = new Feed(title, link, description, language,
									copyright, pubdate, lastBuild, editor, master, Integer.parseInt(ttl));
							feed.generator = generator;
							if (img != null) feed.setFeedImage(img);
							pubdate = null;
						}
						event = eventReader.nextEvent();

						description = "";
						title = "";
						link = "";
						language = "";
						copyright = "";
						author = "";
						pubdate = "";
						guid = "";
						imgs = new String[0];
						img = null;
						lastBuild = null;
						generator = null;
						editor = null;
						master = null;
						ttl = "0";
						img = null;
						continue;
					}

					if (event.asStartElement().getName().getLocalPart() == (ENCLOSURE)) {
						Attribute a = event.asStartElement().getAttributeByName(QName.valueOf(URL));
						if (a != null) imgs = DataSet.addStringArray(imgs, new String[] {a.getValue()});
						event = eventReader.nextEvent();
						continue;
					}

					if (event.asStartElement().getName().getLocalPart() == (IMAGE)) {
						Attribute a = event.asStartElement().getAttributeByName(QName.valueOf(URL));
						if (a != null) img = a.getValue();
						event = eventReader.nextEvent();
						continue;
					}

					if (event.asStartElement().getName().getLocalPart() == (TITLE)) {
						event = eventReader.nextEvent();
						title = event.asCharacters().getData();
						continue;
					}
					if (event.asStartElement().getName().getLocalPart() == (DESCRIPTION)) {
						event = eventReader.nextEvent();
						if (event instanceof javax.xml.stream.events.Characters)
							description = event.asCharacters().getData();
						continue;
					}

					if (event.asStartElement().getName().getLocalPart() == (LINK)) {
						event = eventReader.nextEvent();
						link = event.asCharacters().getData();
						continue;
					}

					if (event.asStartElement().getName().getLocalPart() == (GUID)) {
						event = eventReader.nextEvent();
						guid = event.asCharacters().getData();
						continue;
					}
					if (event.asStartElement().getName().getLocalPart() == (LANGUAGE)) {
						event = eventReader.nextEvent();
						language = event.asCharacters().getData();
						continue;
					}
					if (event.asStartElement().getName().getLocalPart() == (AUTHOR)) {
						event = eventReader.nextEvent();
						author = event.asCharacters().getData();
						continue;
					}
					if (event.asStartElement().getName().getLocalPart() == (PUB_DATE)) {
						event = eventReader.nextEvent();
						pubdate = event.asCharacters().getData();
						continue;
					}
					if (event.asStartElement().getName().getLocalPart() == (COPYRIGHT)) {
						event = eventReader.nextEvent();
						copyright = event.asCharacters().getData();
						continue;
					}

					if (event.asStartElement().getName().getLocalPart() == (LAST_BUILD)) {
						event = eventReader.nextEvent();
						lastBuild = event.asCharacters().getData();
						continue;
					}
					if (event.asStartElement().getName().getLocalPart() == (GENERATOR)) {
						event = eventReader.nextEvent();
						generator = event.asCharacters().getData();
						continue;
					}
					if (event.asStartElement().getName().getLocalPart() == (TTL)) {
						event = eventReader.nextEvent();
						ttl = event.asCharacters().getData();
						continue;
					}
					if (event.asStartElement().getName().getLocalPart() == (EDITOR)) {
						event = eventReader.nextEvent();
						editor = event.asCharacters().getData();
						continue;
					}
					if (event.asStartElement().getName().getLocalPart() == (WEB_MASTER)) {
						event = eventReader.nextEvent();
						master = event.asCharacters().getData();
						continue;
					}

				} else if (event.isEndElement()) {
					if (event.asEndElement().getName().getLocalPart() == (ITEM)) {
						title = fix(title);
						description = fix(description);
						link = fix(link);
						author = fix(author);
						guid = fix(guid);
						pubdate = fix(pubdate);
						FeedMessageElement message = new FeedMessageElement(
								title, description, link,
								author, guid, imgs, pubdate);
						feed.getMessages().add(message);
						event = eventReader.nextEvent();
						continue;
					}
				}
			}
		} catch (Exception e) {
			throw new JPARSECException("Error reading the feed.", e);
		}
		return feed;
	}

	/**
	 * Writes the current feed to a given file.
	 * @param outputFile The path of the output file, or null to create no file.
	 * @return The contents of the file.
	 * @throws JPARSECException If an error occurs.
	 */
	public String writeFeed(String outputFile) throws JPARSECException {
		try {
			// Create a XMLOutputFactory
			XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();

			// Create XMLEventWriter
			StringWriter sw = new StringWriter();
			XMLEventWriter eventWriter = outputFactory
					.createXMLEventWriter(sw); //FileOutputStream(outputFile));

			// Create a EventFactory

			XMLEventFactory eventFactory = XMLEventFactory.newInstance();
			XMLEvent end = eventFactory.createDTD("\n");

			// Create and write Start Tag

			StartDocument startDocument = eventFactory.createStartDocument();

			eventWriter.add(startDocument);

			// Create open tag
			eventWriter.add(end);

			StartElement rssStart = eventFactory.createStartElement("", "", RSS);
			eventWriter.add(rssStart);
			eventWriter.add(eventFactory.createAttribute(VERSION, "2.0"));
			eventWriter.add(end);

			eventWriter.add(eventFactory.createStartElement("", "", CHANNEL));
			eventWriter.add(end);

			// Write the different nodes

			createNode(eventWriter, TITLE, getTitle());

			if (!getLink().equals("")) createNode(eventWriter, LINK, getLink());

			if (!getDescription().equals("")) createNode(eventWriter, DESCRIPTION, getDescription());

			createNode(eventWriter, LANGUAGE, getLanguage());

			if (!getCopyright().equals("")) createNode(eventWriter, COPYRIGHT, getCopyright());

			if (!getPubDate().equals("")) createNode(eventWriter, PUB_DATE, getPubDate());

			if (getLastBuildDate() != null) createNode(eventWriter, LAST_BUILD, getLastBuildDate());
			if (getManagingEditor() != null) createNode(eventWriter, EDITOR, getManagingEditor());
			if (getWebMaster() != null) createNode(eventWriter, WEB_MASTER, getWebMaster());
			if (getCachedTime() != null) createNode(eventWriter, TTL, getCachedTime());
			if (getGenerator() != null) createNode(eventWriter, GENERATOR, getGenerator());
			if (getFeedImage() != null && !getFeedImage().equals("")) createImageNode(eventWriter, IMAGE, getFeedImage(), getTitle(), getLink());

			for (FeedMessageElement entry : getMessages()) {
				eventWriter.add(eventFactory.createStartElement("", "", ITEM));
				eventWriter.add(end);
				createNode(eventWriter, TITLE, entry.title);
				createNode(eventWriter, DESCRIPTION, entry.description);
				if (entry.link != null && !entry.link.equals("")) createNode(eventWriter, LINK, entry.link);
				createNode(eventWriter, AUTHOR, entry.author);
				if (entry.pubDate != null && !entry.pubDate.equals("")) createNode(eventWriter, PUB_DATE, entry.pubDate);
				if (!entry.guid.equals("")) createNode(eventWriter, GUID, entry.guid);
				if (entry.imageURL != null && entry.imageURL.length > 0) {
					for (int i=0; i< entry.imageURL.length; i++) {
						createImageNode(eventWriter, ENCLOSURE, entry.imageURL[i], null, null);
					}
				}
				eventWriter.add(end);
				eventWriter.add(eventFactory.createEndElement("", "", ITEM));
				eventWriter.add(end);
			}

			eventWriter.add(end);
			eventWriter.add(eventFactory.createEndElement("", "", CHANNEL));
			eventWriter.add(end);
			eventWriter.add(eventFactory.createEndElement("", "", RSS));

			eventWriter.add(end);

			eventWriter.add(eventFactory.createEndDocument());

			eventWriter.close();

			String out = sw.toString();
			if (outputFile != null) WriteFile.writeAnyExternalFile(outputFile, out, ReadFile.ENCODING_UTF_8);

			return out;
		} catch (Exception exc) {
			throw new JPARSECException(exc);
		}
	}

	private void createNode(XMLEventWriter eventWriter, String name, String value) throws XMLStreamException {
		XMLEventFactory eventFactory = XMLEventFactory.newInstance();
		XMLEvent end = eventFactory.createDTD("\n");
		XMLEvent tab = eventFactory.createDTD("\t");
		// Create Start node
		StartElement sElement = eventFactory.createStartElement("", "", name);
		eventWriter.add(tab);
		eventWriter.add(sElement);
		// Create Content
		Characters characters = eventFactory.createCharacters(value);
		eventWriter.add(characters);
		// Create End node
		EndElement eElement = eventFactory.createEndElement("", "", name);
		eventWriter.add(eElement);
		eventWriter.add(end);
	}

	private void createImageNode(XMLEventWriter eventWriter, String name, String value,
			String title, String link) throws XMLStreamException {
		XMLEventFactory eventFactory = XMLEventFactory.newInstance();
		XMLEvent end = eventFactory.createDTD("\n");
		XMLEvent tab = eventFactory.createDTD("\t");
		// Create Start node
		ArrayList<Attribute> attr = new ArrayList<Attribute>();
		attr.add(eventFactory.createAttribute("url", value));
		String type = "jpg";
		if (value.toLowerCase().endsWith(".gif")) type = "gif";
		if (value.toLowerCase().endsWith(".png")) type = "png";
		if (value.toLowerCase().endsWith(".bmp")) type = "bmp";
		if (title == null && link == null) {
			attr.add(eventFactory.createAttribute("type", "image/"+type));
		} else {
			if (title != null) attr.add(eventFactory.createAttribute(TITLE, title));
			if (link != null) attr.add(eventFactory.createAttribute(LINK, link));
		}
		StartElement sElement = eventFactory.createStartElement("", "", name, attr.iterator(), null);
		eventWriter.add(tab);
		eventWriter.add(sElement);
		// Create End node
		EndElement eElement = eventFactory.createEndElement("", "", name);
		eventWriter.add(eElement);
		eventWriter.add(end);
	}

	/**
	 * Creates a simple feed message with no images.
	 * @param title The title.
	 * @param description The description. It can contains links
	 * using the html a tag in a standard way, if it is rendered in html.
	 * @param link The link associated to the message.
	 * @param author The author.
	 * @return The feed message object.
	 */
	public static FeedMessageElement createMessage(String title, String description,
			String link, String author) {
		FeedMessageElement message = new FeedMessageElement(
				title, description, link,
				author, link);
		return message;
	}

	/**
	 * Creates a simple feed message with images.
	 * @param title The title.
	 * @param description The description.
	 * @param link The link associated to the message.
	 * @param author The author.
	 * @param guid The Guid, to ensure the uniqueness of the
	 * message. It is identical (usually) to the link.
	 * @param imgs The set of http urls to images inside the message.
	 * @param pDate Publication date in a standard format, for instance
	 * Tue, 10 Jun 2003 04:00:00 GMT or Fri, 29 Jul 2011 23:30:44 +0200.
	 * @return The feed message object.
	 */
	public static FeedMessageElement createMessage(String title, String description,
			String link, String author, String guid, String[] imgs, String pDate) {
		FeedMessageElement message = new FeedMessageElement(
				title, description, link,
				author, guid, imgs, pDate);
		return message;
	}

	/**
	 * Adds an entry to the feed.
	 * @param f The entry.
	 */
	public void addFeedMessage(FeedMessageElement f) {
		entries.add(f);
	}
}
