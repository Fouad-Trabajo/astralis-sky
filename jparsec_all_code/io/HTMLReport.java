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
package jparsec.io;

import java.io.Serializable;
import java.text.DateFormat;

import jparsec.graph.DataSet;
import jparsec.math.MeasureElement;
import jparsec.math.Table;
import jparsec.time.TimeFormat;
import jparsec.util.*;

/**
 * A class to create simple HTML files.<P>
 *
 * The basic process is to write the header, to start the body, to end the body, and
 * to end the document. Any text to write should be located inside the body. Anyway,
 * the header/body and document end are handled or called automatically in case the
 * user don't call them, so it is possible to concentrate just in the content.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class HTMLReport implements Serializable {
	private static final long serialVersionUID = 1L;

	private StringBuffer htmlCode = new StringBuffer(1000);

	private STYLE textStyle = STYLE.PLAIN;
	private SIZE textSize = SIZE.NORMAL;
	private String textColor = COLOR_BLACK;

	private String sep = FileIO.getLineSeparator();
	private String msg = Translate.translate(257)+" "+Translate.translate(161)+" "+Version.PACKAGE_NAME+" v"+Version.VERSION_ID+
	" "+Translate.translate(259) + " ";

	private boolean header = false, body = false, document = true;

	/**
	 * The set of text styles.
	 */
	public enum STYLE {
		/** ID constant for a plain text style. */
		PLAIN,
		/** ID constant for an underline plain text style. */
		UNDERLINE,
		/** ID constant for a bold text style. */
		BOLD,
		/** ID constant for an italic text style. */
		ITALIC,
		/** ID constant for an underline bold text style. */
		UNDERLINE_BOLD,
		/** ID constant for an underline italic text style. */
		UNDERLINE_ITALIC
	};

	/**
	 * The set of text sizes.
	 */
	public enum SIZE {
		/** ID constant for a very small text size. */
		VERY_SMALL,
		/** ID constant for a small text size. */
		SMALL,
		/** ID constant for a medium text size (default). */
		NORMAL,
		/** ID constant for a large text size. */
		LARGE,
		/** ID constant for a very large text size. */
		VERY_LARGE,
		/** ID constant for an undefined text size, will not be used. */
		UNDEFINED
	};

	/**
	 * ID constant for a black text color.
	 */
	public static final String COLOR_BLACK = "000000";
	/**
	 * ID constant for a white text color.
	 */
	public static final String COLOR_WHITE = "ffffff";
	/**
	 * ID constant for a red text color.
	 */
	public static final String COLOR_RED = "ff0000";
	/**
	 * ID constant for a green text color.
	 */
	public static final String COLOR_GREEN = "00ff00";
	/**
	 * ID constant for a blue text color.
	 */
	public static final String COLOR_BLUE = "0000ff";
	/**
	 * ID constant for a yellow text color.
	 */
	public static final String COLOR_YELLOW = "ffff00";

	/**
	 * Writes the header of the HTML file.
	 * @param windowTitle Title to be seen in the web browser.
	 */
	public void writeHeader(String windowTitle)
	{
		htmlCode.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//ES\">");
		htmlCode.append("<!-- "+msg+TimeFormat.dateNow(DateFormat.FULL)+" -->" + sep);
		htmlCode.append("<html>" + sep);
		htmlCode.append("<head>" + sep);
		htmlCode.append("<title>" + sep);
		htmlCode.append(HTMLReport.format(windowTitle) + sep);
		htmlCode.append("</title>" + sep);
		htmlCode.append("</head>" + sep);
		header = true;
	}

	/**
	 * Writes the header of the HTML file.
	 * @param windowTitle Title to be seen in the web browser.
	 * @param windowIcon Name/path of the icon to use. For instance
	 * 'favicon.ico'. Set to null to use no icon.
	 */
	public void writeHeader(String windowTitle, String windowIcon)
	{
		htmlCode.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//ES\">");
		htmlCode.append("<!-- "+msg+TimeFormat.dateNow(DateFormat.FULL)+" -->" + sep);
		htmlCode.append("<html>" + sep);
		htmlCode.append("<head>" + sep);
		htmlCode.append("<title>" + sep);
		htmlCode.append(HTMLReport.format(windowTitle) + sep);
		htmlCode.append("</title>" + sep);
		if (windowIcon != null)
			htmlCode.append("<link rel=\"shortcut icon\" href=\""+windowIcon+"\" />" + sep);
		htmlCode.append("</head>" + sep);
		header = true;
	}

	/**
	 * Begins the body tag.
	 */
	public void beginBody()
	{
		body = true;
		if (htmlCode.indexOf("<body") >= 0) return;
		htmlCode.append("<body>" + sep);
	}
	/**
	 * Begins the body tag.
	 * @param backgroundColor The background color in hexadecimal, for instance ffffff.
	 * notation, or null.
	 */
	public void beginBody(String backgroundColor)
	{
		body = true;
		if (htmlCode.indexOf("<body") >= 0) return;
		String c = "";
		if (backgroundColor != null) c = " bgcolor=\"#"+backgroundColor+"\"";
		htmlCode.append("<body"+c+">" + sep);
	}
	/**
	 * Ends the body tag.
	 */
	public void endBody()
	{
		htmlCode.append("</body>" + sep);
		body = false;
	}
	/**
	 * Ends the html tag.
	 */
	public void endDocument()
	{
		htmlCode.append("</html>" + sep);
		document = false;
	}
	/**
	 * Begins a center tag.
	 */
	public void beginCenter()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		if (!body) try { beginBody(); } catch (Exception exc) {}
		htmlCode.append("<center>" + sep);
	}
	/**
	 * Ends a center tag.
	 */
	public void endCenter()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		if (!body) try { beginBody(); } catch (Exception exc) {}
		htmlCode.append("</center>" + sep);
	}
	/**
	 * To begin a superindex.
	 */
	public void beginSuperIndex()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		if (!body) try { beginBody(); } catch (Exception exc) {}
		htmlCode.append("<sup>");
	}
	/**
	 * To end a superindex.
	 *
	 */
	public void endSuperIndex()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		if (!body) try { beginBody(); } catch (Exception exc) {}
		htmlCode.append("</sup>");
	}
	/**
	 * To begin a subindex.
	 */
	public void beginSubIndex()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		if (!body) try { beginBody(); } catch (Exception exc) {}
		htmlCode.append("<sub>");
	}
	/**
	 * To end a subindex.
	 *
	 */
	public void endSubIndex()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		if (!body) try { beginBody(); } catch (Exception exc) {}
		htmlCode.append("</sub>");
	}

	/**
	 * Writes an (indeed) very big title.
	 * @param title Text of the title.
	 */
	public void writeAVeryBigMainTitle(String title)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		if (!body) try { beginBody(); } catch (Exception exc) {}
		htmlCode.append("<h1>");
		this.writeTextWithStyleAndColor(title, false);
		htmlCode.append("</h1>" + sep);
	}

	/**
	 * Calls {@linkplain #writeMainTitle(String)}.
	 * @param title The title.
	 */
	public void beginSection(String title) {
		writeMainTitle(title);
	}
	
	/**
	 * Writes a big title.
	 * @param title Text of the title.
	 */
	public void writeMainTitle(String title)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		if (!body) try { beginBody(); } catch (Exception exc) {}
		htmlCode.append("<h2>");
		this.writeTextWithStyleAndColor(title, false);
		htmlCode.append("</h2>" + sep);
	}
	/**
	 * Writes a normal title.
	 * @param title Text of the title.
	 */
	public void writeTitle(String title)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		if (!body) try { beginBody(); } catch (Exception exc) {}
		htmlCode.append("<h3>");
		this.writeTextWithStyleAndColor(title, false);
		htmlCode.append("</h3>" + sep);
	}
	/**
	 * Writes a list of items.
	 * @param lines Lines to be formatted as items.
	 */
	public void writeList(String[] lines)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		if (!body) try { beginBody(); } catch (Exception exc) {}
		htmlCode.append("<ul>" + sep);
		for (int i=0; i<lines.length; i++)
		{
			htmlCode.append("<li>");
			this.writeTextWithStyleAndColor(lines[i], true);
			htmlCode.append("</li>" + sep);
		}
		htmlCode.append("</ul>" + sep);
	}
	/**
	 * Writes a paragraph.
	 * @param text Text to write.
	 */
	public void writeParagraph(String text)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		if (!body) try { beginBody(); } catch (Exception exc) {}
		htmlCode.append("<p>");
		this.writeTextWithStyleAndColor(text, true);
		htmlCode.append("</p>" + sep);
	}
	/**
	 * Writes a paragraph.
	 * @param text Text to write.
	 * @param options The options for the p tag in html format.
	 */
	public void writeParagraphWithOptions(String text, String options)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		if (!body) try { beginBody(); } catch (Exception exc) {}
		htmlCode.append("<p "+options+">");
		this.writeTextWithStyleAndColor(text, false);
		htmlCode.append("</p>" + sep);
	}
	/**
	 * Writes raw text without any command.
	 * @param text Text to write.
	 */
	public void writeRawText(String text)
	{
		//if (!header) try { writeHeader(""); } catch (Exception exc) {}
		//if (!body) try { beginBody(); } catch (Exception exc) {}
		htmlCode.append(text);
	}
	/**
	 * Creates a link.
	 * @param href Link destination.
	 * @param highlighted Text to be highlighted.
	 * @return Code for the link.
	 */
	public String writeLink(String href, String highlighted)
	{
		String code = "<a href = \"" + href + "\">"+HTMLReport.format(highlighted)+"</a>";
		return code;
	}
	/**
	 * Creates a link.
	 * @param href Link destination.
	 * @param highlighted Text to be highlighted.
	 * @param target The target: _blank, _self, and so on.
	 * @return Code for the link.
	 */
	public String writeLink(String href, String highlighted, String target)
	{
		if (target != null) {
			target = " target=\""+target+"\"";
		} else {
			target = "";
		}
		String code = "<a href = \"" + href + "\" "+target+">"+HTMLReport.format(highlighted)+"</a>";
		return code;
	}
	/**
	 * Writes the header of a table.
	 * @param border Border style. Set to 0 to get no borders.
	 * @param cellspacing Cell spacing.
	 * @param cellpadding Cell padding.
	 * @param width Width. Set to a number in pixels or as a percentaje.
	 */
	public void writeTableHeader(int border, int cellspacing, int cellpadding, String width)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		if (!body) try { beginBody(); } catch (Exception exc) {}
		htmlCode.append("<table border="+border+" cellspacing="+cellspacing+" cellpadding="+cellpadding+" width="+width+">"+ sep);
	}

	/**
	 * Ends a table tag.
	 */
	public void endTable()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		if (!body) try { beginBody(); } catch (Exception exc) {}
		htmlCode.append("</table>"+sep);
	}

	/**
	 * Writes an entire table object, row by row. Header should be written previously. The table
	 * is written using parenthesis for the error values, and without units. Parameters like background
	 * color and align are ignored.
	 * @param table The table object.
	 * @throws JPARSECException In case the number of dimensions in the Table is more than 2.
	 */
	public void writeRowsInTable(Table table) throws JPARSECException {
		if (table.getDimensions() > 2) throw new JPARSECException("The table must be 1 or 2 dimensions.");
		boolean useParentheses = true, includeUnit = false;
		for (int i=0; i<table.getNrows(); i++) {
			writeRowInTable(MeasureElement.toString(table.getRowValues(0, i), useParentheses, includeUnit), null, null, null);
		}
	}

	/**
	 * Write a row in a table.<P>
	 *
	 * The background color is composed by three groups of two characters in hexadecimal format,
	 * for each component red, green, and blue. 0000ff is intense blue, ff0000 is red, and so on.<P>
	 *
	 * Align commands are right, left, center.<P>
	 *
	 * Column span is the number of columns to be agruped by a given text. If a table has 6 columns
	 * (length of columns array), then a column span of 6 alows to write text ocuping the whole row,
	 * as a title for example. In this case the columns array should have length 1 to write only that
	 * title.
	 *
	 * @param columns Columns to be written to complete the row.
	 * @param bgcolor Background color. Set to null to skip the command.
	 * @param align Align command. Set to null to skip the command.
	 * @param colspan Column span command. Set to null to skip the command.
	 */
	public void writeRowInTable(String columns[], String bgcolor, String align, String colspan)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		if (!body) try { beginBody(); } catch (Exception exc) {}
		htmlCode.append("<tr>"+sep);
		if (bgcolor == null) {
			bgcolor = "";
		} else {
			bgcolor = "bgcolor=\""+bgcolor+"\"";
		}
		if (align == null) {
			align = "";
		} else {
			align = "align=\""+align+"\"";
		}
		if (colspan == null) {
			colspan = "";
		} else {
			colspan = "colspan=\""+colspan+"\"";
		}
		String prop = bgcolor+" "+align+" "+colspan;
		prop = prop.trim();
		if (!prop.equals("")) prop = " " + prop;

		for (int i=0; i<columns.length; i++)
		{
			htmlCode.append("<td"+prop+">"+sep);
			this.writeTextWithStyleAndColor(columns[i], true);
			htmlCode.append(sep);
			htmlCode.append("</td>"+sep);
		}
		htmlCode.append("</tr>"+sep);
	}
	/**
	 * Write a row in a table including a possible title (tooltip text) that appears when
	 * the mouse is on the text item.<P>
	 *
	 * The background color is composed by three groups of two characters in hexadecimal format,
	 * for each component red, green, and blue. 0000ff is intense blue, ff0000 is red, and so on.<P>
	 *
	 * Align commands are right, left, center.<P>
	 *
	 * Column span is the number of columns to be agruped by a given text. If a table has 6 columns
	 * (length of columns array), then a column span of 6 alows to write text ocuping the whole row,
	 * as a title for example. In this case the columns array should have length 1 to write only that
	 * title.
	 *
	 * @param columns Columns to be written to complete the row.
	 * @param title Title for the tooltip in each column item.
	 * @param bgcolor Background color. Set to null to skip the command.
	 * @param align Align command. Set to null to skip the command.
	 * @param colspan Column span command. Set to null to skip the command.
	 */
	public void writeRowInTable(String columns[], String title[], String bgcolor, String align, String colspan)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		if (!body) try { beginBody(); } catch (Exception exc) {}
		htmlCode.append("<tr>"+sep);
		if (bgcolor == null) {
			bgcolor = "";
		} else {
			bgcolor = "bgcolor=\"#"+bgcolor+"\"";
		}
		if (align == null) {
			align = "";
		} else {
			align = "align=\""+align+"\"";
		}
		if (colspan == null) {
			colspan = "";
		} else {
			colspan = "colspan=\""+colspan+"\"";
		}
		String prop = bgcolor+" "+align+" "+colspan;
		prop = prop.trim();
		if (!prop.equals("")) prop = " " + prop;

		for (int i=0; i<columns.length; i++)
		{
			htmlCode.append("<td"+prop+" title=\""+title[i]+"\">"+sep);
			this.writeTextWithStyleAndColor(columns[i], true);
			htmlCode.append(sep);
			htmlCode.append("</td>"+sep);
		}
		htmlCode.append("</tr>"+sep);
	}
	/**
	 * Writes a comment, not visible in the resulting document.
	 * @param text Text to write.
	 * @throws JPARSECException If the text style is invalid.
	 */
	public void writeComment(String text)
	throws JPARSECException {
		htmlCode.append("<!-- " + text + " -->");
	}
	/**
	 * Writes a blank paragraph, like a big skip.
	 * @throws JPARSECException If the text style is invalid.
	 */
	public void writeBigSkip()
	throws JPARSECException {
		this.writeParagraph("&nbsp;");
	}
	/**
	 * Writes a blank line, like a small skip.
	 * @throws JPARSECException If the text style is invalid.
	 */
	public void writeSmallSkip()
	throws JPARSECException {
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		if (!body) try { beginBody(); } catch (Exception exc) {}
		htmlCode.append("&nbsp;<BR>" + sep);
	}
	/**
	 * Writes an space in the code, to separate code sections. Not visible in the browser.
	 */
	public void writeSpace()
	{
		htmlCode.append(sep);
	}
	/**
	 * Writes an horizontal line.
	 */
	public void writeHorizontalLine()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		if (!body) try { beginBody(); } catch (Exception exc) {}
		htmlCode.append("<HR>" + sep);
	}
	/**
	 * Returns the code for an image tag. The image text is not written to the html
	 * code since here is supposed that further processing is required. None of the
	 * parameters can be null.
	 * @param width Width.
	 * @param height Height.
	 * @param align Align.
	 * @param border Border.
	 * @param alt Tooltip text.
	 * @param src Link to the image.
	 * @return HTML code.
	 */
	public String writeImage(String width, String height, String align, String border,
			String alt, String src)
	{
		String code = "<img width=\""+width+"\" height=\""+height+"\" align=\""+align+"\" ";
		code += "border=\""+border+"\" alt=\""+HTMLReport.format(alt)+"\" src=\""+src+"\">";
		return code;
	}
	/**
	 * Writes the code for an image tag. None of the parameters can be null.
	 * @param width Width.
	 * @param height Height.
	 * @param align Align.
	 * @param border Border.
	 * @param alt Tooltip text.
	 * @param src Link to the image.
	 */
	public void writeImageToHTML(String width, String height, String align, String border,
			String alt, String src)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		if (!body) try { beginBody(); } catch (Exception exc) {}
		String code = "<img width=\""+width+"\" height=\""+height+"\" align=\""+align+"\" ";
		code += "border=\""+border+"\" alt=\""+HTMLReport.format(alt)+"\" src=\""+src+"\">";
		htmlCode.append(code);
	}

	/**
	 * Writes the code for an image tag. This methods is here to support the change from a
	 * Latex to an HTML report without issues. Caption is ignored.
	 * @param width Width.
	 * @param height Height.
	 * @param align Align.
	 * @param src Link to the image.
	 * @param caption Ignored.
	 * @param alt Tooltip text.
	 */
	public void writeImageWithCaption(String width, String height, String align, String src,
			String caption, String alt)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		if (!body) try { beginBody(); } catch (Exception exc) {}
		String code = "<img width=\""+width+"\" height=\""+height+"\" align=\""+align+"\" ";
		code += "alt=\""+HTMLReport.format(alt)+"\" src=\""+src+"\">";
		htmlCode.append(code);
	}

	/**
	 * Returns the HTML code.
	 * @return HTML code.
	 */
	public String getCode()
	{
		if (header && body && document) {
			HTMLReport hr = new HTMLReport();
			hr.header = hr.body = true;
			hr.setCode(this.htmlCode.toString());
			hr.endBody();
			hr.endDocument();
			return hr.htmlCode.toString();
		}
		return this.htmlCode.toString();
	}
	/**
	 * Sets the HTML code.
	 * @param code Code to be set.
	 */
	public void setCode(String code)
	{
		this.htmlCode = new StringBuffer(code);
	}

	private String getBeginOfCurrentStyle()
	{
		String style = "";
		switch (this.textStyle)
		{
		case PLAIN:
			style = "";
			break;
		case BOLD:
			style = "<B>";
			break;
		case ITALIC:
			style = "<I>";
			break;
		case UNDERLINE:
			style = "<U>";
			break;
		case UNDERLINE_BOLD:
			style = "<U><B>";
			break;
		case UNDERLINE_ITALIC:
			style = "<U><I>";
			break;
		}
		return style;
	}
	private String getEndOfCurrentStyle()
	{
		String style = "";
		switch (this.textStyle)
		{
		case PLAIN:
			style = "";
			break;
		case BOLD:
			style = "</B>";
			break;
		case ITALIC:
			style = "</I>";
			break;
		case UNDERLINE:
			style = "</U>";
			break;
		case UNDERLINE_BOLD:
			style = "</B></U>";
			break;
		case UNDERLINE_ITALIC:
			style = "</I></U>";
			break;
		}
		return style;
	}
	/**
	 * Sets the style of the text to write.
	 * @param style Text style.
	 */
	public void setTextStyle(STYLE style)
	{
		this.textStyle = style;
	}
	/**
	 * Sets the size of the text to write.
	 * @param size Text size.
	 */
	public void setTextSize(SIZE size)
	{
		this.textSize = size;
	}
	/**
	 * Sets the color of the text to write.
	 * @param color Text color in hex format.
	 * Some constants defined in this class.
	 */
	public void setTextColor(String color)
	{
		this.textColor = color;
	}
	private void writeTextWithStyleAndColor(String text, boolean useSize)
	{
		htmlCode.append(this.getBeginOfCurrentStyle());
		String size = "";
		if (useSize && textSize != SIZE.UNDEFINED) size = "size=\""+(2+this.textSize.ordinal())+"\"";
		htmlCode.append("<font "+size+" color = \"#"+this.textColor+"\">");
		htmlCode.append(HTMLReport.format(text));
		htmlCode.append("</font>");
		htmlCode.append(this.getEndOfCurrentStyle());
	}

	/**
	 * Formats a given string changing characters with tildes to their html representation, 
	 * including the uppercase characters and the special characters &deg;, &ntilde;, &micro;, &plusmn;.
	 * @param input Input string.
	 * @return The output.
	 */
	public static String format(String input) {
		if (input == null) return "";
		input = DataSet.replaceAll(input, "\u00e1", "&aacute;", true);
		input = DataSet.replaceAll(input, "\u00e9", "&eacute;", true);
		input = DataSet.replaceAll(input, "\u00ed", "&iacute;", true);
		input = DataSet.replaceAll(input, "\u00f3", "&oacute;", true);
		input = DataSet.replaceAll(input, "\u00fa", "&uacute;", true);
		input = DataSet.replaceAll(input, "\u00c1", "&Aacute;", true);
		input = DataSet.replaceAll(input, "\u00c9", "&Eacute;", true);
		input = DataSet.replaceAll(input, "\u00cd", "&Iacute;", true);
		input = DataSet.replaceAll(input, "\u00d3", "&Oacute;", true);
		input = DataSet.replaceAll(input, "\u00da", "&Uacute;", true);
		input = DataSet.replaceAll(input, "\u00f1", "&ntilde;", true);
		input = DataSet.replaceAll(input, "\u00d1", "&Ntilde;", true);
		input = DataSet.replaceAll(input, "\u00b0", "&deg;", true);
		input = DataSet.replaceAll(input, "\u00ba", "&deg;", true);
		input = DataSet.replaceAll(input, "\\mu", "&micro;", true);
		input = DataSet.replaceAll(input, "+/-", "&plusmn;", true);
		return input;
	}
	
	/**
	 * Unformats a string modified with the {@linkplain #format(String)} method 
	 * to get back the original special characters.
	 * @param input Input string.
	 * @return The output.
	 */
	public static String unformat(String input) {
		if (input == null) return "";
		input = DataSet.replaceAll(input, "&aacute;", "\u00e1", true);
		input = DataSet.replaceAll(input, "&eacute;", "\u00e9", true);
		input = DataSet.replaceAll(input, "&iacute;", "\u00ed", true);
		input = DataSet.replaceAll(input, "&oacute;", "\u00f3", true);
		input = DataSet.replaceAll(input, "&uacute;", "\u00fa", true);
		input = DataSet.replaceAll(input, "&Aacute;", "\u00c1", true);
		input = DataSet.replaceAll(input, "&Eacute;", "\u00c9", true);
		input = DataSet.replaceAll(input, "&Iacute;", "\u00cd", true);
		input = DataSet.replaceAll(input, "&Oacute;", "\u00d3", true);
		input = DataSet.replaceAll(input, "&Uacute;", "\u00da", true);
		input = DataSet.replaceAll(input, "&ntilde;", "\u00f1", true);
		input = DataSet.replaceAll(input, "&Ntilde;", "\u00d1", true);
		input = DataSet.replaceAll(input, "&deg;",    "\u00b0", true);
		input = DataSet.replaceAll(input, "&deg;",    "\u00ba", true);
		input = DataSet.replaceAll(input, "&micro;",  "\\mu",   true);
		input = DataSet.replaceAll(input, "&plusmn;", "+/-",    true);
		return input;
	}
}
