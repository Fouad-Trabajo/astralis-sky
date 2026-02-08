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

import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;

import jparsec.ephem.Functions;
import jparsec.graph.DataSet;
import jparsec.io.HTMLReport.SIZE;
import jparsec.math.MeasureElement;
import jparsec.math.Table;
import jparsec.time.TimeFormat;
import jparsec.util.*;
import jparsec.util.Logger.LEVEL;
import jparsec.util.Translate.LANGUAGE;
import jparsec.vo.ADSElement;

/**
 * A class to generate reports for Latex.<P>
 *
 * The basic process is to write the header, to start the body, to end the body, and
 * to end the document. Any text to write should be located inside the body. Anyway,
 * the header/body and document end are handled or called automatically in case the
 * user don't call them, so it is possible to concentrate just in the content. <P>
 *
 * Latex report has been designed to be compatible with html reports. If you create
 * an html report, you can also report to latex by just changing the object instance.
 * Some methods are not completely compatible, though.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class LATEXReport implements Serializable
{
	private static final long serialVersionUID = 1L;

	private StringBuffer latexCode = new StringBuffer(1000);
	private StringBuffer bibTexCode = new StringBuffer(1000);

	private HTMLReport.STYLE textStyle = HTMLReport.STYLE.PLAIN;
	private SIZE textSize = SIZE.NORMAL;
	private String textColor = COLOR_BLACK;

	private String sep = FileIO.getLineSeparator();
	private String msg = Translate.translate(257)+" "+Translate.translate(161)+" "+Version.PACKAGE_NAME+" v"+Version.VERSION_ID+
	" "+Translate.translate(259) + " ";

	private String avoidFormatting = "\\{}~";
	private boolean fixFigures = false;

	private boolean insideTable = false, tableFirstLine = false;
	private boolean header = false, document = true, beamer = false, justify = false, firstJustify = true;

	/**
	 * ID constant for a black text color.
	 */
	public static final String COLOR_BLACK = "Black";
	/**
	 * ID constant for a white text color.
	 */
	public static final String COLOR_WHITE = "White";
	/**
	 * ID constant for a red text color.
	 */
	public static final String COLOR_RED = "Red";
	/**
	 * ID constant for a green text color.
	 */
	public static final String COLOR_GREEN = "Green";
	/**
	 * ID constant for a blue text color.
	 */
	public static final String COLOR_BLUE = "Blue";
	/**
	 * ID constant for a yellow text color.
	 */
	public static final String COLOR_YELLOW = "Yellow";

	/**
	 * Empty constructor.
	 */
	public LATEXReport() {}
	/**
	 * Not so empty constructor...
	 * @param fixedFigures True to add adequate code to the figures to fix
	 * their positions in the document.
	 */
	public LATEXReport(boolean fixedFigures) { fixFigures = true; }
	/**
	 * Selects to add or not relevant code to fix the positions of the figures in the output pdf.
	 * @param b True or false.
	 */
	public void setFixFiguresFlag(boolean b) {
		fixFigures = b;
	}

	/**
	 * Writes the header of the Latex file.
	 * @param title Title to be seen in the document.
	 */
	public void writeHeader(String title)
	{
		latexCode.append("% "+msg + TimeFormat.dateNow(DateFormat.FULL) + sep);
		latexCode.append("\\documentclass{article}" + sep);
		latexCode.append("\\usepackage{graphicx}" + sep);
		latexCode.append("\\usepackage{ulem}" + sep);
		//latexCode.append("\\usepackage{txfonts}" + sep);
		latexCode.append("\\usepackage{verbatim}" + sep);
		latexCode.append("\\usepackage{hyperref}" + sep);
		if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH)
			latexCode.append("\\usepackage[spanish]{babel}" + sep);
		latexCode.append("\\usepackage{longtable}" + sep);
		latexCode.append("\\usepackage{ragged2e}" + sep);
		latexCode.append("\\usepackage{setspace}" + sep);
		latexCode.append("\\usepackage{pdfpages}" + sep);
		latexCode.append("\\usepackage[space]{grffile}" + sep);
		//latexCode.append("\\usepackage{blindtext}" + sep);
		latexCode.append("\\usepackage[usenames]{color}" + sep);
		latexCode.append("\\usepackage{natbib}" + sep);
		latexCode.append(" \\bibpunct{(}{)}{;}{a}{}{,} % to follow the A&A style" + sep);
		latexCode.append(sep);
		latexCode.append("\\pdfpagewidth 8.5in" + sep);
		latexCode.append("\\pdfpageheight 11in " + sep);
		latexCode.append("\\setlength\\topmargin{0in}" + sep);
		latexCode.append("\\setlength\\headheight{0in}" + sep);
		latexCode.append("\\setlength\\headsep{0in}" + sep);
		latexCode.append("\\setlength\\textheight{8.7in}" + sep);
		latexCode.append("\\setlength\\textwidth{6.5in}" + sep);
		latexCode.append("\\setlength\\oddsidemargin{0in}" + sep);
		latexCode.append("\\setlength\\evensidemargin{0in}" + sep);
		latexCode.append("\\setlength\\parindent{0.25in}" + sep);
		latexCode.append("\\setlength\\parskip{0.25in}" + sep);
		latexCode.append(sep);
		latexCode.append("\\begin{document}" + sep);
		header = true;
		if (title != null) {
			latexCode.append("\\title{");
			this.writeTextWithStyleAndColor(title, false);
			latexCode.append("}" + sep);
			latexCode.append("\\maketitle" + sep);
		}
	}

	/**
	 * Writes the header of the Latex file.
	 * @param title Title to be seen in the document.
	 * @param textWidthPercentage Percentage of the page width ocupped by text, from 0 to 100.
	 * @param textHeightPercentage Percentage of the page height ocupped by text, from 0 to 100.
	 */
	public void writeHeader(String title, int textWidthPercentage, int textHeightPercentage)
	{
		double w = (textWidthPercentage / 100.0) * 8.5;
		double h = (textHeightPercentage / 100.0) * 11;

		latexCode.append("% "+msg + TimeFormat.dateNow(DateFormat.FULL) + sep);
		latexCode.append("\\documentclass{article}" + sep);
		latexCode.append("\\usepackage{graphicx}" + sep);
		latexCode.append("\\usepackage{ulem}" + sep);
		//latexCode.append("\\usepackage{txfonts}" + sep);
		latexCode.append("\\usepackage{verbatim}" + sep);
		latexCode.append("\\usepackage{hyperref}" + sep);
		if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH)
			latexCode.append("\\usepackage[spanish]{babel}" + sep);
		latexCode.append("\\usepackage{longtable}" + sep);
		latexCode.append("\\usepackage{ragged2e}" + sep);
		latexCode.append("\\usepackage{setspace}" + sep);
		latexCode.append("\\usepackage{pdfpages}" + sep);
		latexCode.append("\\usepackage[space]{grffile}" + sep);
		//latexCode.append("\\usepackage{blindtext}" + sep);
		latexCode.append("\\usepackage[usenames]{color}" + sep);
		latexCode.append("\\usepackage{natbib}" + sep);
		latexCode.append(" \\bibpunct{(}{)}{;}{a}{}{,} % to follow the A&A style" + sep);
		latexCode.append(sep);
		latexCode.append("\\pdfpagewidth 8.5in" + sep);
		latexCode.append("\\pdfpageheight 11in " + sep);
		latexCode.append("\\setlength\\topmargin{0in}" + sep);
		latexCode.append("\\setlength\\headheight{0in}" + sep);
		latexCode.append("\\setlength\\headsep{0in}" + sep);
		latexCode.append("\\setlength\\textheight{"+Functions.formatValue(h, 1)+"in}" + sep);
		latexCode.append("\\setlength\\textwidth{"+Functions.formatValue(w, 1)+"in}" + sep);
		latexCode.append("\\setlength\\oddsidemargin{0in}" + sep);
		latexCode.append("\\setlength\\evensidemargin{0in}" + sep);
		latexCode.append("\\setlength\\parindent{0.25in}" + sep);
		latexCode.append("\\setlength\\parskip{0.25in}" + sep);
		latexCode.append(sep);
		latexCode.append("\\begin{document}" + sep);
		header = true;
		if (title != null) {
			latexCode.append("\\title{");
			this.writeTextWithStyleAndColor(title, false);
			latexCode.append("}" + sep);
			latexCode.append("\\maketitle" + sep);
		}
	}

	/**
	 * Writes the header of the Latex file with an author.
	 * Not compatible with HTML.
	 * @param documentType Document type. Set to null to use default article.
	 * @param title Title to be seen in the document.
	 * @param author Document author.
	 * @param date Date.
	 * @param packages A set of package names to be imported using usepackage Latex command.
	 * Can be null to import no additional one. Default imported packages are graphicx, txfonts,
	 * verbatim, hyperref, spanish {babel}, longtable, usenames {color}, natbib.
	 */
	public void writeHeader(String documentType, String title, String author, String date, String packages[])
	{
		if (documentType == null) documentType = "article";
		latexCode.append("% "+msg + TimeFormat.dateNow(DateFormat.FULL) + sep);
		latexCode.append("\\documentclass{"+documentType+"}" + sep);
		latexCode.append("\\usepackage{graphicx}" + sep);
		latexCode.append("\\usepackage{ulem}" + sep);
		latexCode.append("\\usepackage{txfonts}" + sep);
		latexCode.append("\\usepackage{verbatim}" + sep);
		latexCode.append("\\usepackage{hyperref}" + sep);
		if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH)
			latexCode.append("\\usepackage[spanish]{babel}" + sep);
		latexCode.append("\\usepackage{longtable}" + sep);
		latexCode.append("\\usepackage{ragged2e}" + sep);
		latexCode.append("\\usepackage{setspace}" + sep);
		latexCode.append("\\usepackage{pdfpages}" + sep);
		latexCode.append("\\usepackage[space]{grffile}" + sep);
		latexCode.append("\\usepackage{natbib}" + sep);
		if (packages != null && packages.length > 0) {
			for (int i=0; i<packages.length; i++) {
				latexCode.append("\\usepackage{"+packages[i]+"}" + sep);
			}
		}
		//latexCode.append("\\usepackage{blindtext}" + sep);
		latexCode.append("\\usepackage[usenames]{color}" + sep);
		latexCode.append(" \\bibpunct{(}{)}{;}{a}{}{,} % to follow the A&A style" + sep);
		latexCode.append(sep);
		latexCode.append("\\pdfpagewidth 8.5in" + sep);
		latexCode.append("\\pdfpageheight 11in " + sep);
		latexCode.append("\\setlength\\topmargin{0in}" + sep);
		latexCode.append("\\setlength\\headheight{0in}" + sep);
		latexCode.append("\\setlength\\headsep{0.5in}" + sep);
		latexCode.append("\\setlength\\textheight{7.7in}" + sep);
		latexCode.append("\\setlength\\textwidth{6.5in}" + sep);
		latexCode.append("\\setlength\\oddsidemargin{0in}" + sep);
		latexCode.append("\\setlength\\evensidemargin{0in}" + sep);
		latexCode.append("\\setlength\\parindent{0.25in}" + sep);
		latexCode.append("\\setlength\\parskip{0.25in}" + sep);
		latexCode.append(sep);
		latexCode.append("\\begin{document}" + sep);
		header = true;
		if (author != null) {
			latexCode.append("\\author{");
			this.writeTextWithStyleAndColor(author, false);
			latexCode.append("}" + sep);
		}
		if (date != null) {
			latexCode.append("\\date{");
			this.writeTextWithStyleAndColor(date, false);
			latexCode.append("}" + sep);
		}
		if (title != null) {
			latexCode.append("\\title{");
			this.writeTextWithStyleAndColor(title, false);
			latexCode.append("}" + sep);
			latexCode.append("\\maketitle" + sep);
		}
	}

	/**
	 * Writes the header of the Latex file for a presentation.
	 * Not compatible with HTML.
	 * @param theme The theme name, or null to use Warsaw.
	 * @param title Title to be seen in the document.
	 * @param author Document author.
	 * @param institute The institute.
	 * @param date Date.
	 * @param packages A set of package names to be imported using usepackage Latex command.
	 * Can be null to import no additional one. Default imported packages are graphicx, txfonts,
	 * verbatim, hyperref, spanish {babel}, longtable, usenames {color}, natbib.
	 * @param hideNavigationControls True to hide navigation controls.
	 * @param showTitlePage True to include the first frame showing a default presentation title.
	 * @param forceSmallNavigationBar True to force the navigation bar to show just one section and
	 * subsection (the current one).
	 * @param leftMargin Left margin as a value in cm for the text location. Null for default value.
	 * @param rightMargin Right margin as a value in cm for the text location. Null for default value.
	 */
	public void writeHeaderForPresentationUsingBeamer(String theme, String title, String author, String institute, String date,
			String packages[], boolean hideNavigationControls, boolean showTitlePage, boolean forceSmallNavigationBar,
			String leftMargin, String rightMargin)
	{
		if (theme == null) theme = "Warsaw";
		header = true;
		latexCode.append("% "+msg + TimeFormat.dateNow(DateFormat.FULL) + sep);
		latexCode.append("\\documentclass{beamer}" + sep);
		latexCode.append("\\usepackage[latin1]{inputenc}" + sep);
		latexCode.append("\\usepackage{graphicx}" + sep);
		latexCode.append("\\usepackage{ulem}" + sep);
		latexCode.append("\\usepackage{txfonts}" + sep);
		latexCode.append("\\usepackage{verbatim}" + sep);
		latexCode.append("\\usepackage{hyperref}" + sep);
		if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH)
			latexCode.append("\\usepackage[spanish]{babel}" + sep);
		latexCode.append("\\usepackage{longtable}" + sep);
		latexCode.append("\\usepackage{ragged2e}" + sep);
		latexCode.append("\\usepackage{setspace}" + sep);
		latexCode.append("\\usepackage{pdfpages}" + sep);
		latexCode.append("\\usepackage[space]{grffile}" + sep);
		if (packages != null && packages.length > 0) {
			for (int i=0; i<packages.length; i++) {
				latexCode.append("\\usepackage{"+packages[i]+"}" + sep);
			}
		}
		//latexCode.append("\\usepackage{blindtext}" + sep);
		latexCode.append("\\usepackage[usenames]{color}" + sep);
		latexCode.append("\\usetheme{Warsaw}" + sep);
		if (author != null) {
			latexCode.append("\\author{");
			this.writeTextWithStyleAndColor(author, false);
			latexCode.append("}" + sep);
		}
		if (institute != null) {
			latexCode.append("\\institute{");
			this.writeTextWithStyleAndColor(institute, false);
			latexCode.append("}" + sep);
		}
		if (date != null) {
			latexCode.append("\\date{");
			this.writeTextWithStyleAndColor(date, false);
			latexCode.append("}" + sep);
		}
		if (title != null) {
			latexCode.append("\\title{");
			this.writeTextWithStyleAndColor(title, false);
			latexCode.append("}" + sep);
		}
		if (hideNavigationControls) latexCode.append("\\usenavigationsymbolstemplate{}" + sep);
		if (leftMargin != null) latexCode.append("\\setbeamersize{text margin left="+leftMargin+"cm}" + sep);
		if (rightMargin != null) latexCode.append("\\setbeamersize{text margin right="+rightMargin+"cm}" + sep);

		if (forceSmallNavigationBar) {
			latexCode.append("\\setbeamertemplate{headline}" + sep);
			latexCode.append("{%" + sep);
			latexCode.append("\\leavevmode%" + sep);
			latexCode.append("\\begin{beamercolorbox}[wd=.5\\paperwidth,ht=2.5ex,dp=1.125ex]{section in head/foot}%" + sep);
			latexCode.append("\\hbox to .5\\paperwidth{\\hfil\\insertsectionhead\\hfil}" + sep);
			latexCode.append("\\end{beamercolorbox}%" + sep);
			latexCode.append("\\begin{beamercolorbox}[wd=.5\\paperwidth,ht=2.5ex,dp=1.125ex]{subsection in head/foot}%" + sep);
			latexCode.append("\\hbox to .5\\paperwidth{\\hfil\\insertsubsectionhead\\hfil}" + sep);
			latexCode.append("\\end{beamercolorbox}%" + sep);
			latexCode.append("}" + sep);
		}

		latexCode.append(sep + "\\begin{document}" + sep);

		if (showTitlePage) {
			latexCode.append("\\begin{frame}[plain]" + sep);
			latexCode.append("\\titlepage" + sep);
			latexCode.append("\\end{frame}" + sep);
		}
		beamer = true;
	}

	/**
	 * Begins the body. Does nothing, only for compatibility with HTML reports.
	 */
	public void beginBody() {}
	/**
	 * Ends the body tag. Does nothing, only for compatibility with HTML reports.
	 */
	public void endBody() {}

	/**
	 * Begins a frame for a presentation.
	 * @param plain True to begin a plain frame without decorations.
	 * @param addTitlePage True to add \\titlepage to the frame.
	 */
	public void beginFrame(boolean plain, boolean addTitlePage) {
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		if (!plain) {
			latexCode.append(sep + "\\begin{frame}" + sep);
		} else {
			latexCode.append(sep + "\\begin{frame}[plain]" + sep);
		}
		if (addTitlePage) latexCode.append("\\titlepage" + sep);
	}

	/**
	 * Begins a frame for a presentation.
	 * @param name Name.
	 */
	public void beginFrame(String name)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		if (name == null) {
			latexCode.append(sep + "\\begin{frame}" + sep);
		} else {
			latexCode.append(sep + "\\begin{frame}{"+name+"}" + sep);
		}
	}

	/**
	 * Ends a frame of a presentation.
	 */
	public void endFrame()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append(sep + "\\end{frame}" + sep);
	}
	/**
	 * Begins a chapter.
	 * @param name Name.
	 */
	public void beginChapter(String name)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append(sep + "\\chapter{");
		this.writeTextWithStyleAndColor(name, false);
		latexCode.append("}" + sep + sep);
	}
	/**
	 * Begins a section.
	 * @param name Name.
	 */
	public void beginSection(String name)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append(sep + "\\section{");
		this.writeTextWithStyleAndColor(name, false);
		latexCode.append("}" + sep + sep);
	}
	/**
	 * Begins a sub-section.
	 * @param name Name.
	 */
	public void beginSubSection(String name)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append(sep + "\\subsection{");
		this.writeTextWithStyleAndColor(name, false);
		latexCode.append("}" + sep + sep);
	}

	/**
	 * Begins a columns block.
	 */
	public void beginColumns() {
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append(sep + "\\begin{columns}");
	}
	/**
	 * Ends a columns block.
	 */
	public void endColumns() {
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append(sep + "\\end{columns}");
	}

	/**
	 * Begins a column block.
	 * @param widthFraction Fraction of the width as a value, from 0 to 1.
	 */
	public void beginColumn(String widthFraction) {
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append(sep + "\\begin{column}{"+widthFraction+" \\textwidth}");
	}
	/**
	 * Ends a column block.
	 */
	public void endColumn() {
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append(sep + "\\end{column}");
	}

	/**
	 * Ends the Latex document.
	 */
	public void endDocument()
	{
		if (!document) return;
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append(sep + "\\end{document}" + sep);
		document = false;
	}
	/**
	 * Ends the Latex document adding the bibliography commands.
	 * @param bibCommands Commands for the bibliography, as given by
	 * {@linkplain LATEXReport#getBibItem(String, String, String, String)}.
	 */
	public void endDocumentWithBibliography(String[] bibCommands)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append(sep);
		latexCode.append("\\begin{thebibliography}{"+bibCommands.length+"}" + sep);
		for (int i=0; i<bibCommands.length; i++)
		{
			latexCode.append(bibCommands[i] + sep);
		}
		latexCode.append("\\end{thebibliography}" + sep);
		latexCode.append(sep);
		this.endDocument();
	}
	/**
	 * Returns a bibliography item.
	 * @param key The key.
	 * @param author The author.
	 * @param title The title. Can be null.
	 * @param publication The publication.
	 * @return The text of the bib item.
	 */
	public static String getBibItem(String key, String author, String title, String publication)
	{
		String bibItem = "\\bibitem{"+key+"} "+author+", ";
		if (title != null && !title.equals("")) bibItem += "\\textit{"+title+"}, ";
		bibItem += publication+".";
		return bibItem;
	}
	/**
	 * Ends the Latex document adding the bibliography commands.
	 * @param style Style name. Can be null to use default A&amp;A.
	 * @param bibFileName Name of the bibliographic file.
	 */
	public void endDocumentWithBibliography(String style, String bibFileName)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append(sep);
		latexCode.append("\\bibliographystyle{"+style+"}" + sep);
		latexCode.append("\\input{aas_macros.tex}" + sep);
		latexCode.append("\\bibliography{"+bibFileName+"}" + sep);
		latexCode.append(sep);
		this.endDocument();
	}
	/**
	 * Adds a bibtex entry to the document. Web access required to solve
	 * the bibtex entry.
	 * @param bibtex Bibtex object.
	 * @throws JPARSECException If an error occurs.
	 */
	public void addCite(ADSElement bibtex)
	throws JPARSECException {
		this.bibTexCode.append(bibtex.getBibTexEntry() + sep + sep);
	}
	/**
	 * Adds a bib entry to the document.
	 * @param key The key.
	 * @param citep True for a citep command.
	 */
	public void addCite(String key, boolean citep)
	{
		String addP = "";
		if (citep) addP = "p";
		this.bibTexCode.append("~\\cite"+addP+"{"+key+"}" + sep);
	}
	/**
	 * Returns the bibtex code with all added bibtex entries.
	 * A file should be created using the same name stablished
	 * in the bibliography command.
	 * @return The set of bibtex references.
	 */
	public String getBibTexCode()
	{
		return this.bibTexCode.toString();
	}
	/**
	 * Begins a center tag.
	 */
	public void beginCenter()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append("\\begin{center}" + sep);
	}
	/**
	 * Ends a center tag.
	 */
	public void endCenter()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append("\\end{center}" + sep);
	}
	/**
	 * Begins a right justify tag (for text).
	 */
	public void beginRightJustify()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append("\\begin{flushright}" + sep);
	}
	/**
	 * Ends a right justify tag (for text).
	 */
	public void endRightJustify()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append("\\end{flushright}" + sep);
	}
	/**
	 * Begins a left justify tag (for text).
	 */
	public void beginLeftJustify()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append("\\begin{flushleft}" + sep);
	}
	/**
	 * Ends a left justify tag (for text).
	 */
	public void endLeftJustify()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append("\\end{flushleft}" + sep);
	}
	/**
	 * Begins a quotation (for text).
	 */
	public void beginQuotation()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append("\\begin{quotation}" + sep);
	}
	/**
	 * Ends a quotation (for text).
	 */
	public void endQuotation()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append("\\end{quotation}" + sep);
	}
	/**
	 * Begins a quote (for text).
	 */
	public void beginQuote()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append("\\begin{quote}" + sep);
	}
	/**
	 * Ends a quote (for text).
	 */
	public void endQuote()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append("\\end{quote}" + sep);
	}
	/**
	 * Begins a justify (for text).
	 */
	public void beginJustify()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append("\\begin{justify}" + sep);
		justify = true;
	}
	/**
	 * Ends a justify (for text).
	 */
	public void endJustify()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append("\\end{justify}" + sep);
		justify = false;
	}
	/**
	 * To begin a superindex.
	 */
	public void beginSuperIndex()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append("$^{");
	}
	/**
	 * To end a superindex.
	 *
	 */
	public void endSuperIndex()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append("}$");
	}
	/**
	 * To begin a subindex.
	 */
	public void beginSubIndex()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append("$_{");
	}
	/**
	 * To end a subindex.
	 *
	 */
	public void endSubIndex()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append("}$");
	}
	/**
	 * Writes an (indeed) very big title.
	 * @param title Text of the title.
	 */
	public void writeAVeryBigMainTitle(String title)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append(sep + "\\begin{Huge}");
		this.writeTextWithStyleAndColor(title, false);
		latexCode.append("\\end{Huge}" + sep + sep);
	}
	/**
	 * Writes a big title.
	 * @param title Text of the title.
	 */
	public void writeMainTitle(String title)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append(sep + "\\begin{LARGE}");
		this.writeTextWithStyleAndColor(title, false);
		latexCode.append("\\end{LARGE}" + sep + sep);
	}
	/**
	 * Writes a normal title.
	 * @param title Text of the title.
	 */
	public void writeTitle(String title)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append(sep + "\\begin{large}");
		this.writeTextWithStyleAndColor(title, false);
		latexCode.append("\\end{large}" + sep + sep);
	}
	/**
	 * Writes a list of items.
	 * @param lines Lines to be formatted as items.
	 * @throws JPARSECException If the text style is invalid.
	 */
	public void writeList(String[] lines)
	throws JPARSECException{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		if (beamer) {
			if (justify) {
				if (firstJustify) {
					latexCode.append("\\let\\olditem\\item" + sep);
					firstJustify = false;
				}
				latexCode.append("\\renewcommand\\item{\\olditem\\justifying}" + sep);
			} else {
				if (firstJustify) {
					latexCode.append("\\let\\olditem\\item" + sep);
					firstJustify = false;
				}
				latexCode.append("\\renewcommand\\item{\\olditem\\raggedright}" + sep);
			}
		}
		latexCode.append(sep + "\\begin{itemize}" + sep);
		for (int i=0; i<lines.length; i++)
		{
			latexCode.append("\\item ");
			this.writeParagraph(lines[i]);
		}
		latexCode.append("\\end{itemize}" + sep + sep);
	}
	/**
	 * Writes a list of items with numbers. Not compatible with HTML.
	 * @param lines Lines to be formatted as items.
	 */
	public void writeListUsingNumbers(String[] lines)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append(sep + "\\begin{enumerate}" + sep);
		for (int i=0; i<lines.length; i++)
		{
			latexCode.append("\\item ");
			this.writeTextWithStyleAndColor(lines[i], true);
			latexCode.append(sep);
		}
		latexCode.append("\\end{enumerate}" + sep + sep);
	}
	/**
	 * Writes a paragraph.
	 * @param text Text to write.
	 * @param indent The indentation in unit of cm of the first line.
	 */
	public void writeParagraph(String text, double indent)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		if (!latexCode.toString().endsWith(sep)) latexCode.append(sep);
		//\hspace{0.5cm}
		this.writeTextWithStyleAndColor(text, true);
		String beg = this.getBeginOfCurrentStyle(true);
		String s = latexCode.toString();
		String rest = s.substring(s.lastIndexOf(beg) + beg.length());
		setCode(s.substring(0, s.lastIndexOf(beg)) + beg + "\\hspace{"+Functions.formatValue(indent, 3)+"cm} " + rest);
		if (beamer) {
			String ends = this.getEndOfCurrentStyle(true);
			setCode(latexCode.toString().substring(0, latexCode.toString().lastIndexOf(ends)) + "\\\\" + ends);
		}
		latexCode.append(sep + sep);
	}

	/**
	 * Writes a paragraph.
	 * @param text Text to write.
	 */
	public void writeParagraph(String text)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append(sep);
		this.writeTextWithStyleAndColor(text, true);
		if (beamer) {
			String ends = this.getEndOfCurrentStyle(true);
			setCode(latexCode.toString().substring(0, latexCode.toString().lastIndexOf(ends)) + "\\\\" + ends);
		}
		latexCode.append(sep + sep);
	}

	/**
	 * Sets the line spacing in units of the default line break (1).
	 * @param sp A value, 2 for double space, and so on.
	 */
	public void setLineSpacing(double sp) {
		latexCode.append(sep + "\\linespread{"+Functions.formatValue(sp, 3)+"}" + sep + sep);
	}

	/**
	 * Writes a footnote. Not compatible with HTML.
	 * @param text Text to write.
	 */
	public void writeFootNote(String text)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append("\\footnote{");
		this.writeTextWithStyleAndColor(text, false);
		latexCode.append("}" + sep);
	}
	/**
	 * Foces a new page. Not compatible with HTML.
	 */
	public void forceNewPage()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append(sep);
		latexCode.append("\\newpage" + sep);
		latexCode.append(sep);
	}
	/**
	 * Flush content in memory (clearpage). Not compatible with HTML.
	 */
	public void flush()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append(sep);
		latexCode.append("\\clearpage" + sep);
		latexCode.append(sep);
	}
	/**
	 * Writes a comment, not visible in the resulting document.
	 * @param text Text to write.
	 */
	public void writeComment(String text)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append("% " + text + sep);
	}
	/**
	 * Writes text in a small font.
	 * @param text Text to write.
	 */
	public void writeSmallTextLine(String text)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append(sep + "\\begin{small}");
		this.writeTextWithStyleAndColor(text, false);
		latexCode.append("\\end{small}" + sep + sep);
	}
	/**
	 * Writes raw text without any command.
	 * @param text Text to write.
	 */
	public void writeRawText(String text)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append(text);
	}
	/**
	 * Creates a link in HTML format.
	 * @param href Link destination.
	 * @param highlighted Text to be highlighted.
	 * @return Code for the link.
	 */
	public String writeHTMLLink(String href, String highlighted)
	{
		String code = "\\htmladdnormallink{"+highlighted+"}{"+href+"}";
		return code;
	}
	/**
	 * Creates a link.
	 * @param href Link destination.
	 * @param highlighted Text to be highlighted.
	 * @return Code for the link.
	 */
	public String writeLink(String href, String highlighted)
	{
		String code = "\\href{"+href+"}{"+highlighted+"}";
		return code;
	}
	/**
	 * Writes the header of a table.
	 * @param border Border style. Ignored, only for compatibility with HTML report.
	 * @param cellspacing Cell spacing. Ignored, only for compatibility with HTML report.
	 * @param cellpadding Cell padding. Ignored, only for compatibility with HTML report.
	 * @param width Width. Ignored, only for compatibility with HTML report.
	 */
	public void writeTableHeader(int border, int cellspacing, int cellpadding, String width)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append(sep + "\\begin{table}[h]" + sep);
		latexCode.append("\\begin{tabular*}{1.0\\textwidth}");
		insideTable = true;
		tableFirstLine = false;
	}
	/**
	 * Writes the header of a table.
	 * @param caption Caption for the table.
	 */
	public void writeTableHeader(String caption)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append(sep + "\\begin{table}[h]" + sep);
		if (caption != null) {
			latexCode.append("\\caption{");
			this.writeTextWithStyleAndColor(caption, false);
			latexCode.append("}" + sep);
		}
		latexCode.append("\\begin{tabular*}{1.0\\textwidth}" + sep);
		insideTable = true;
		tableFirstLine = false;
	}
	/**
	 * Writes the header of a long table.
	 * @param caption Caption for the table.
	 * @param columnAligment Aligment command (l, r, c) for each column in the table;
	 */
	public void writeLongTableHeader(String caption, String columnAligment)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append(sep + "% *** For decimal point alignment in numbers use the siunitx package and the S column alignment type ***");
		latexCode.append(sep + "\\begin{longtable}{"+columnAligment+"}" + sep);
		if (caption != null) {
			latexCode.append("\\caption{");
			this.writeTextWithStyleAndColor(caption, false);
			latexCode.append("} \\\\" + sep);
		}
		latexCode.append("\\hline\\noalign{\\smallskip}" + sep);
		insideTable = true;
		tableFirstLine = true;
	}
	/**
	 * Ends a table tag.
	 */
	public void endTable()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append("\\noalign{\\smallskip}\\hline" + sep);
		latexCode.append("\\end{tabular*}" + sep);
		latexCode.append("\\end{table}" + sep + sep);
		insideTable = false;
		tableFirstLine = false;
	}
	/**
	 * Ends a table tag adding a label.
	 * Not compatible with HTML.
	 * @param label The label.
	 */
	public void endTable(String label)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append("\\noalign{\\smallskip}\\hline" + sep);
		latexCode.append("\\end{tabular*}" + sep);
		if (label != null) latexCode.append("\\label{"+label+"}" + sep);
		latexCode.append("\\end{table}" + sep + sep);
		insideTable = false;
		tableFirstLine = false;
	}

	/**
	 * Writes a label. Not compatible with HTML.
	 * @param label The label.
	 */
	public void writeLabel(String label) {
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		if (label != null) latexCode.append("\\label{"+label+"}" + sep);
	}

	/**
	 * Ends a long table tag adding a label.
	 * @param label The label. Set to null to avoid it.
	 */
	public void endLongTable(String label)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append("\\hline\\noalign{\\smallskip}" + sep);
		if (label != null) latexCode.append("\\label{"+label+"}" + sep);
		latexCode.append("\\end{longtable}" + sep + sep);
		insideTable = false;
		tableFirstLine = false;
	}
	/**
	 * Write a row in a table.<P>
	 *
	 * Align commands are right, left, center.<P>
	 *
	 * Column span is the number of columns to be agruped by a given text. If a table has 6 columns
	 * (length of columns array), then a column span of 6 alows to write text ocuping the whole row,
	 * as a title for example. In this case the columns array should have length 1 to write only that
	 * title.
	 *
	 * @param columns Columns to be written to complete the row.
	 * @param bgcolor Background color. Ignored, only for compatibility with HTML report.
	 * @param align Align command. Set to null to use default left align.
	 * @param colspan Column span command. Set to null to skip the command.
	 */
	public void writeRowInTable(String columns[], String bgcolor, String align, String colspan)
	{
		this.writeRowInTable(columns, bgcolor, align, colspan, true);
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
			writeRowInTable(MeasureElement.toString(table.getRowValues(0, i), useParentheses, includeUnit), null, null, null, false);
		}
	}

	/**
	 * Write a row in a table.<P>
	 *
	 * Align commands are r (right), l (left), and c (center).<P>
	 *
	 * Column span is the number of columns to be agruped by a given text. If a table has 6 columns
	 * (length of columns array), then a column span of 6 alows to write text ocuping the whole row,
	 * as a title for example. In this case the columns array should have length 1 to write only that
	 * title.
	 *
	 * @param columns Columns to be written to complete the row.
	 * @param bgcolor Background color. Ignored, only for compatibility with HTML report.
	 * @param align Align command for the columns. Set to null to use default left align.
	 * @param colspan Column span command. Set to null to skip the command.
	 * @param format True to format text with the current style and color.
	 */
	public void writeRowInTable(String columns[], String bgcolor, String align, String colspan, boolean format)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		if (align == null) {
			align = "l";
		} else {
			if (!align.startsWith("c") && !align.startsWith("l") && !align.startsWith("r")) align = "l";
		}
		String cols = "";
		int howMany = columns.length;
		if (colspan != null) howMany *= Integer.parseInt(colspan);
		if (howMany == align.length()) {
			cols = align;
		} else {
			for (int i=0; i<howMany; i++) {
				cols += align.substring(0, 1);
			}
		}
		if (!tableFirstLine) {
			latexCode.append("{@{\\extracolsep{\\fill}}"+cols+"}" + sep);
			latexCode.append("\\hline\\noalign{\\smallskip}" + sep);
			tableFirstLine = true;
		}
//		int begin = latexCode.lastIndexOf("begin{tabular*}");
//		int end = latexCode.lastIndexOf("\\\\");
//		if (end < begin) {
//			latexCode.append("{@{\\extracolsep{\\fill}}"+cols+"}" + sep);
//			latexCode.append("\\hline\\noalign{\\smallskip}" + sep);
//		}

		for (int i=0; i<columns.length; i++)
		{
			if (!columns[i].trim().equals("")) {
				String line = "";
				if (colspan == null) {
					line = "";
				} else {
					line = "\\multicolumn{"+colspan+"}{c}{";
				}

				if (!line.equals(""))
				{
					latexCode.append(line);
					if (format) {
						this.writeTextWithStyleAndColor(columns[i], false);
					} else {
						latexCode.append(columns[i]);
					}
					latexCode.append("}");
					i = i + Integer.parseInt(colspan);
				} else {
					if (format) {
						this.writeTextWithStyleAndColor(columns[i], false);
					} else {
						latexCode.append(columns[i]);
					}
				}
			}
			if (i < (columns.length - 1)) latexCode.append(" & ");
		}
		latexCode.append(" \\\\"+sep);
	}

	/**
	 * Write a row in a table including a possible title (tooltip text) that appears when
	 * the mouse is on the text item.<P>
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
	 * @param bgcolor Background color. Ignored, only for compatibility with HTML report.
	 * @param align Align command. Set to null to use default left align.
	 * @param colspan Column span command. Set to null to skip the command.
	 * @param format True to format text with the current style and color.
	 */
	public void writeRowInTable(String columns[], String title[], String bgcolor, String align, String colspan, boolean format)
	{
		for (int i=0; i<columns.length; i++)
		{
			columns[i] = "\\href{"+title[i]+"}{\\nolinkurl{"+columns[i]+"}}";
		}

		this.writeRowInTable(columns, bgcolor, align, colspan, format);
	}
	/**
	 * Write a row in a table including a possible title (tooltip text) that appears when
	 * the mouse is on the text item.<P>
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
	 * @param bgcolor Background color. Ignored, only for compatibility with HTML report.
	 * @param align Align command. Set to null to use default left align.
	 * @param colspan Column span command. Set to null to skip the command.
	 */
	public void writeRowInTable(String columns[], String title[], String bgcolor, String align, String colspan)
	{
		for (int i=0; i<columns.length; i++)
		{
			columns[i] = "\\href{"+title[i]+"}{\\nolinkurl{"+columns[i]+"}}";
		}

		this.writeRowInTable(columns, bgcolor, align, colspan, true);
	}
	/**
	 * Writes a blank paragraph, like a big skip (2 cm).
	 */
	public void writeBigSkip()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append("\\vspace{2cm}" + sep);
	}
	/**
	 * Writes a blank line, like a small skip (0.5 cm).
	 */
	public void writeSmallSkip()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append(sep + "\\vspace{0.5cm}" + sep + sep);
	}
	/**
	 * Writes a blank line, like a small skip.
	 * @param cm The amount of space in cm. 2cm is a big skip, 0.5 a small skip.
	 */
	public void writeVerticalSkip(double cm)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append(sep + "\\vspace{"+Functions.formatValue(cm, 3)+"cm}" + sep + sep);
	}

	/**
	 * Writes an space in the code, to separate code sections. Not visible in the browser.
	 */
	public void writeSpace()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append(sep);
	}
	/**
	 * Writes an horizontal line.
	 */
	public void writeHorizontalLine()
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		if (!insideTable) {
			latexCode.append(sep + "\\begin{table}[h]" + sep);
			latexCode.append("\\begin{tabular}[c]{ccc}");
		}
		latexCode.append("\\hline\\noalign{\\smallskip} ");
		if (!insideTable) {
			latexCode.append("\\\\" + sep);
			latexCode.append("\\end{tabular}");
			latexCode.append("\\end{table}" + sep + sep);
		} else {
			latexCode.append(sep);
		}
	}
	/**
	 * Returns the code for an image tag. None of the parameters can be null.
	 * @param width Width. As a number for pixels or as percentage.
	 * @param height Height. As a number for pixels or as percentage.
	 * @param align Align.
	 * @param border Border. Ignored
	 * @param alt Alt (tooltip text). Ignored.
	 * @param src Link to the image.
	 * @return Latex code.
	 */
	public String writeImage(String width, String height, String align, String border,
			String alt, String src)
	{
		String code = this.beginAlignment(align);
		if (width != null) {
			if (width.endsWith("%")) {
				double scale = Double.parseDouble(width.substring(0, width.length() - 1)) / 100.0;
				width = "" + scale + "\\textwidth";
			} else {
				width += "pt";
			}
		}
		if (height != null) {
			if (height.endsWith("%")) {
				double scale = Double.parseDouble(height.substring(0, height.length() - 1)) / 100.0;
				height = "" + scale + "\\textheight";
			} else {
				height += "pt";
			}
		}

		String fix = "";
		if (fixFigures) fix = "[!ht]";
		code += sep + "\\begin{figure}" + fix + sep;
		code += "\\includegraphics[";
		if (width != null) {
			code +="width="+width;
			if (height != null) code+=", ";
		}
		if (height != null) code += "height="+height;
		code +="]{"+src+"}"+sep;
		if (beamer && align != null) {
			if (align.toLowerCase().equals("left")) code += "\\hspace*{15cm}"+sep;
			if (align.toLowerCase().equals("right")) code += "\\hspace*{-15cm}"+sep;
		}

		code += "\\end{figure}"+sep + sep;
		code += this.endAlignment(align);
		return code;
	}

	/**
	 * Returns the code for an image tag. None of the parameters can be null.
	 * @param width Width. As a number for pixels or as percentage.
	 * @param height Height. As a number for pixels or as percentage.
	 * @param align Align.
	 * @param border Border. Ignored
	 * @param alt Alt (tooltip text). Ignored.
	 * @param src Link to the image.
	 * @return Latex code.
	 */
	public String writeImages(String width, String height, String align, String border,
			String alt, String src[])
	{
		String code = this.beginAlignment(align);
		if (width != null) {
			if (width.endsWith("%")) {
				double scale = Double.parseDouble(width.substring(0, width.length() - 1)) / 100.0;
				width = "" + scale + "\\textwidth";
			} else {
				width += "pt";
			}
		}
		if (height != null) {
			if (height.endsWith("%")) {
				double scale = Double.parseDouble(height.substring(0, height.length() - 1)) / 100.0;
				height = "" + scale + "\\textheight";
			} else {
				height += "pt";
			}
		}

		String fix = "";
		if (fixFigures) fix = "[!ht]";
		code += sep + "\\begin{figure}" + fix + sep;
		for (int i=0; i<src.length; i++) {
			code += "\\includegraphics[";
			if (width != null) {
				code +="width="+width;
				if (height != null) code+=", ";
			}
			if (height != null) code += "height="+height;
			code +="]{"+src[i]+"}"+sep;
		}
		if (beamer && align != null) {
			if (align.toLowerCase().equals("left")) code += "\\hspace*{15cm}"+sep;
			if (align.toLowerCase().equals("right")) code += "\\hspace*{-15cm}"+sep;
		}

		code += "\\end{figure}"+sep + sep;
		code += this.endAlignment(align);
		return code;
	}

	/**
	 * Writes the code for an image tag.
	 * @param width Width.
	 * @param height Height.
	 * @param align Align.
	 * @param src Link to the image.
	 * @param caption Caption for the image. Can be null.
	 * @param label Label as an id for the image. Can be null.
	 */
	public void writeImageWithCaption(String width, String height, String align,
			String src, String caption, String label) {
		this.writeImageWithCaption(width, height, null, align, src, caption, label);
	}

	/**
	 * Writes the code for an image tag. Not compatible with HTML.
	 * @param width Width.
	 * @param height Height.
	 * @param angle Rotation angle of the image in degrees, or null.
	 * @param align Align.
	 * @param src Link to the image.
	 * @param caption Caption for the image. Can be null.
	 * @param label Label as an id for the image. Can be null.
	 */
	public void writeImageWithCaption(String width, String height, String angle, String align,
			String src, String caption, String label)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		String code = this.beginAlignment(align);
		if (width != null) {
			if (width.endsWith("%")) {
				double scale = Double.parseDouble(width.substring(0, width.length() - 1)) / 100.0;
				width = "" + Functions.formatValue(scale, 5) + "\\textwidth";
			} else {
				width += "pt";
			}
		}
		if (height != null) {
			if (height.endsWith("%")) {
				double scale = Double.parseDouble(height.substring(0, height.length() - 1)) / 100.0;
				height = "" + Functions.formatValue(scale, 5) + "\\textheight";
			} else {
				height += "pt";
			}
		}

		String fix = "";
		if (fixFigures) fix = "[!ht]";
		code += sep + "\\begin{figure}" + fix + sep;
		code += "\\includegraphics[";
		if (angle != null) {
			code += "angle="+angle;
			if (width != null || height != null) code += ", ";
		}
		if (width != null) {
			code +="width="+width;
			if (height != null) code+=", ";
		}
		if (height != null) code += "height="+height;
		code +="]{"+src+"}"+sep;
		if (beamer && align != null) {
			if (align.toLowerCase().equals("left")) code += "\\hspace*{15cm}"+sep;
			if (align.toLowerCase().equals("right")) code += "\\hspace*{-15cm}"+sep;
		}
		if (caption != null) code += "\\smallskip"+sep;
		if (caption != null) {
			code += "\\caption{";
			code += this.getTextWithStyleAndColor(caption, false);
			code += "}"+sep;
		}
		if (label != null && !label.equals("")) code += "\\label{"+label+"}"+sep;
		code += "\\end{figure}"+sep + sep;
		code += this.endAlignment(align);

		this.latexCode.append(code);
	}
	/**
	 * Writes the code for a set of images. Not compatible with HTML.
	 * @param width Width. Can be null.
	 * @param height Height. Can be null.
	 * @param angle Rotation angle of the image in degrees, or null.
	 * @param align Align.
	 * @param src Links to the images.
	 * @param caption Caption for the image. Can be null.
	 * @param label Label as an id for the image. Can be null.
	 */
	public void writeImagesWithCaption(String width, String height, String angle, String align,
			String[] src, String caption, String label)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		String code = this.beginAlignment(align);
		if (width != null) {
			if (width.endsWith("%")) {
				double scale = Double.parseDouble(width.substring(0, width.length() - 1)) / 100.0;
				width = "" + Functions.formatValue(scale, 5) + "\\textwidth";
			} else {
				width += "pt";
			}
		}
		if (height != null) {
			if (height.endsWith("%")) {
				double scale = Double.parseDouble(height.substring(0, height.length() - 1)) / 100.0;
				height = "" + Functions.formatValue(scale, 5) + "\\textheight";
			} else {
				height += "pt";
			}
		}

		String fix = "";
		if (fixFigures) fix = "[!ht]";
		code += sep + "\\begin{figure}" + fix + sep;
		for (int i=0;i<src.length;i++) {
			code += "\\includegraphics[";
			if (angle != null) {
				code += "angle="+angle;
				if (width != null || height != null) code += ", ";
			}
			if (width != null) {
				code +="width="+width;
				if (height != null) code+=", ";
			}
			if (height != null) code += "height="+height;
			code +="]{"+src[i]+"}"+sep;
		}

		if (caption != null) code += "\\smallskip"+sep;
		if (caption != null) {
			code += "\\caption{";
			code += this.getTextWithStyleAndColor(caption, false);
			code += "}"+sep;
		}
		if (label != null && !label.equals("")) code += "\\label{"+label+"}"+sep;
		code += "\\end{figure}"+sep + sep;
		code += this.endAlignment(align);

		this.latexCode.append(code);
	}
	private String beginAlignment(String align)
	{
		if (align == null) align = "left";
		if (align.toLowerCase().equals("left")) align = "flushleft";
		if (align.toLowerCase().equals("right")) align = "flushright";

		String code = "\\begin{"+align.toLowerCase()+"}" + sep;
		return code;
	}
	private String endAlignment(String align)
	{
		if (align == null) align = "left";
		if (align.toLowerCase().equals("left")) align = "flushleft";
		if (align.toLowerCase().equals("right")) align = "flushright";

		String code = "\\end{"+align.toLowerCase()+"}" + sep;
		return code;
	}
	/**
	 * Returns the Latex code. The document IS closed
	 * automatically in case it is still opened.
	 * @return Latex code.
	 */
	public String getCode()
	{
		if (header && document) {
			LATEXReport lr = new LATEXReport();
			lr.header = true;
			lr.setCode(this.latexCode.toString());
			lr.endDocument();
			return lr.latexCode.toString();
		}
		return this.latexCode.toString();
	}
	/**
	 * Returns the Latex code. The document is NOT closed
	 * automatically in case it is still opened.
	 * @return Latex code.
	 */
	public String getCurrentCode()
	{
		return this.latexCode.toString();
	}

	/**
	 * Sets the Latex code.
	 * @param code Code to be set.
	 */
	public void setCode(String code)
	{
		this.latexCode = new StringBuffer(code);
	}

	private String getBeginOfCurrentStyle(boolean alsoSize)
	{
		String style = "";
		if (alsoSize) {
			switch (textSize) {
			case NORMAL:
				style = "{\\normal {";
				break;
			case LARGE:
				style = "{\\large {";
				break;
			case SMALL:
				style = "{\\small {";
				break;
			case VERY_LARGE:
				style = "{\\huge {";
				break;
			case VERY_SMALL:
				style = "{\\tiny {";
				break;
			case UNDEFINED:
				break;
			}
		}

		switch (this.textStyle)
		{
		case PLAIN:
			break;
		case BOLD:
			style += "{\\textbf {";
			break;
		case ITALIC:
			style += "{\\textit {";
			break;
		case UNDERLINE:
			style += "{\\underline {";
			break;
		case UNDERLINE_BOLD:
			style += "{\\textbf{\\underline {";
			break;
		case UNDERLINE_ITALIC:
			style += "{\\textit{\\underline {";
			break;
		}
		return style;
	}
	private String getEndOfCurrentStyle(boolean alsoSize)
	{
		String style = "";
		switch (this.textStyle)
		{
		case PLAIN:
			style = "";
			break;
		case BOLD:
			style = "}}";
			break;
		case ITALIC:
			style = "}}";
			break;
		case UNDERLINE:
			style = "}}";
			break;
		case UNDERLINE_BOLD:
			style = "}}}";
			break;
		case UNDERLINE_ITALIC:
			style = "}}}";
			break;
		}
		if (alsoSize) return style + "}}";
		return style;
	}
	/**
	 * Sets the style of the text to write.
	 * @param style Text style.
	 */
	public void setTextStyle(HTMLReport.STYLE style)
	{
		this.textStyle = style;
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
	private void writeTextWithStyleAndColor(String text, boolean alsoSize)
	{
		if (!header) try { writeHeader(""); } catch (Exception exc) {}
		latexCode.append(getTextWithStyleAndColor(text, alsoSize));
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
	 * Returns the LATEX code for a given text using the current style and color.
	 * @param text The text.
	 * @param alsoSize True to get also the code with the current text size.
	 * @return LATEX code.
	 */
	public String getTextWithStyleAndColor(String text, boolean alsoSize)
	{
		String code = this.getBeginOfCurrentStyle(alsoSize);
		if (this.textColor != LATEXReport.COLOR_BLACK) {
			code += "\\textcolor{"+this.textColor+"}{";
		}
		code += formatSymbols(text);
		if (this.textColor != LATEXReport.COLOR_BLACK) {
			code += "}";
		}
		code += this.getEndOfCurrentStyle(alsoSize);
		return code;
	}

	/**
	 * Compile a latex file to a dvi file. 3 consecutive compilations
	 * are performed to avoid problems, and previous temporal files .aux,
	 * .log, .dvi, .out, .toc, .idx, .bbl, and .blg are deleted.
	 * @param path Path to a .tex file.
	 * @return Output log from command.
	 */
	public static String compileLatexToDVI(String path)
	{
		if (!path.endsWith(".tex")) path += ".tex";
		LATEXReport.deleteTemporalFiles(path);

		String outputDir = "";
		if (path.indexOf(FileIO.getFileSeparator()) >= 0) outputDir = path.substring(0, path.lastIndexOf(FileIO.getFileSeparator()));
		String od = outputDir;
		if (!outputDir.equals("")) outputDir = "-output-directory="+outputDir;

		String out = "";
		try {
			//String com = "latex "+outputDir+" -interaction=batchmode " + path;
			String com[] = new String[] {"latex", outputDir, "-interaction=batchmode", path};
			Process p = ApplicationLauncher.executeCommand(com, null, new File(od));
			p.waitFor();
			p = ApplicationLauncher.executeCommand(com, null, new File(od));
			p.waitFor();
			p = ApplicationLauncher.executeCommand(com, null, new File(od));
			p.waitFor();
			out = ApplicationLauncher.getConsoleOutputFromProcess(p);
		} catch (Exception exc) {}
		return out;
	}
	/**
	 * Delete temporal files (.aux, .log, .dvi, .out, .toc,
	 * .idx, .blg, .bbl).
	 * @param path Path to the .tex file.
	 */
	public static void deleteTemporalFiles(String path)
	{
		if (!path.endsWith(".tex")) path += ".tex";
		String filePath = path.substring(0, path.lastIndexOf(".tex"));
		try {
			(new File(filePath+".aux")).delete();
			(new File(filePath+".log")).delete();
			(new File(filePath+".dvi")).delete();
			(new File(filePath+".out")).delete();
			(new File(filePath+".toc")).delete();
			(new File(filePath+".idx")).delete();
			(new File(filePath+".blg")).delete();
			(new File(filePath+".bbl")).delete();
			(new File(filePath+".snm")).delete();
			(new File(filePath+".nav")).delete();
		} catch (Exception exc) {
		}
	}

	/**
	 * Compile a latex file to create a Postscript file.
	 * @param path Path to a .tex file.
	 * @return Path to the resulting ps file.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String compileLatexToPS(String path)
	throws JPARSECException {
		String dviName = path.substring(0, path.lastIndexOf(".")) + ".dvi";
		LATEXReport.compileLatexToDVI(path);
		String psName = dviName.substring(0, dviName.lastIndexOf(".")) + ".ps";
		//String com = "dvips -o "+psName+" " + dviName;
		String com[] = new String[] {"dvips", "-o", psName, dviName};
		Process p = ApplicationLauncher.executeCommand(com, null, new File(FileIO.getDirectoryFromPath(dviName)));
		try {
			p.waitFor();
			LATEXReport.deleteTemporalFiles(path);
		} catch (Exception exc) {
			Logger.log(LEVEL.ERROR, "Exception when compiling with latex. Output message was: "+exc.getLocalizedMessage());
		}
		return psName;
	}
	/**
	 * Compile a latex file to create a PDF file directly using pdflatex.
	 * @param path Path to a .tex file.
	 * @return Path to the resulting pdf file.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String compileLatexToPDF(String path)
	throws JPARSECException {
		if (!path.endsWith(".tex")) path += ".tex";

		LATEXReport.deleteTemporalFiles(path);

		String outputDir = "";
		if (path.indexOf(FileIO.getFileSeparator()) >= 0) outputDir = path.substring(0, path.lastIndexOf(FileIO.getFileSeparator()));
		String od = outputDir;
		if (!outputDir.equals("")) outputDir = "-output-directory="+outputDir;

		//String out = "";
		try {
			//String com = "pdflatex "+outputDir+" -interaction=batchmode " + path;
			String com[] = new String[] {"pdflatex", outputDir, "-interaction=batchmode", path};
			Process p = ApplicationLauncher.executeCommand(com, null, new File(od));
			p.waitFor();
			p = ApplicationLauncher.executeCommand(com, null, new File(od));
			p.waitFor();
			p = ApplicationLauncher.executeCommand(com, null, new File(od));
			p.waitFor();
			//out = ApplicationLauncher.getConsoleOutputFromProcess(p);
			LATEXReport.deleteTemporalFiles(path);
		} catch (Exception exc) {
			Logger.log(LEVEL.ERROR, "Exception when compiling with pdflatex. Output message was: "+exc.getLocalizedMessage());
		}
		String pdfName = path.substring(0, path.lastIndexOf(".")) + ".pdf";
		return pdfName;
	}
	/**
	 * Compile a latex file to create a PDF file, first converting it to dvi.
	 * @param path Path to a .tex file.
	 * @return Path to the resulting pdf file.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String compileLatexToDVIAndThenPDF(String path)
	throws JPARSECException {
		String dviName = path.substring(0, path.lastIndexOf(".")) + ".dvi";
		LATEXReport.compileLatexToDVI(path);

		String pdfName = dviName.substring(0, dviName.lastIndexOf(".")) + ".pdf";
		Process p;
		try {
			//String com1 = "rm "+pdfName;
			String com1[] = new String[] {"rm", pdfName};
			p = ApplicationLauncher.executeCommand(com1);
			p.waitFor();
			//String com2 = "dvipdf -sOutputFile="+pdfName+" " + dviName;
			String com2[] = new String[] {"dvipdf", "-sOutputFile="+pdfName, dviName};
			p = ApplicationLauncher.executeCommand(com2, null, new File(FileIO.getDirectoryFromPath(dviName)));
		} catch (Exception exc) {
			//String com3 = "dvipdfm -o "+pdfName+" " + dviName;
			String com3[] = new String[] {"dvipdfm", "-o", pdfName, dviName};
			p = ApplicationLauncher.executeCommand(com3, null, new File(FileIO.getDirectoryFromPath(dviName)));
		}

		try {
			p.waitFor();
			LATEXReport.deleteTemporalFiles(path);
		} catch (Exception exc) {
			Logger.log(LEVEL.ERROR, "Exception when compiling with dvipdf. Output message was: "+exc.getLocalizedMessage());
		}
		return pdfName;
	}
	/**
	 * Sets the set of characters to preserve as the user types then without
	 * reformating. See {@linkplain LATEXReport#formatSymbols(String)} for list
	 * of symbols. Default symbols to avoid formatting are \{}~.

	 * @param toAvoid Set of characters to preserve without reformatting.
	 */
	public void setAvoidFormatting(String toAvoid)
	{
		this.avoidFormatting = toAvoid;
	}

	/**
	 * Returns the characters that should not be formatted.
	 * @return Characters to preserve.
	 */
	public String getAvoidFormatting()
	{
		return this.avoidFormatting;
	}

	/**
	 * Replaces normal symbols by it's Latex representation. Currently only for &lt;, &gt;,
	 * &aacute;, &eacute;, &iacute;, &oacute;, &uacute;, +/-, {, }, _, ^, ~, \, $, #, %, &amp;, &deg;, &ntilde;. Any of them can be preserved in the
	 * input by adding the character to {@linkplain LATEXReport#setAvoidFormatting(String)}.
	 * Default symbols to avoid formatting are \{}~.
	 *
	 * @param text Input text.
	 * @return Output table.
	 */
	public String formatSymbols(String text)
	{
		if (text == null) return text;
		if (text.equals("")) return text;

		if (this.avoidFormatting.indexOf("\\")<0) text = DataSet.replaceAll(text, "\\", "*JPARSEC_BAR*", true);
		if (this.avoidFormatting.indexOf("$")<0) {
			text = DataSet.replaceAll(text, "$", "*JPARSEC*", true);
			text = DataSet.replaceAll(text, "*JPARSEC*", "\\$", true);
		}
		if (this.avoidFormatting.indexOf("\\")<0) text = DataSet.replaceAll(text, "*JPARSEC_BAR*", "$\\backslash$", true);
		if (this.avoidFormatting.indexOf("_")<0) {
			text = DataSet.replaceAll(text, "_", "*JPARSEC*", true);
			text = DataSet.replaceAll(text, "*JPARSEC*", "\\_", true);
		}
		if (this.avoidFormatting.indexOf("^")<0) {
			text = DataSet.replaceAll(text, "^", "*JPARSEC*", true);
			text = DataSet.replaceAll(text, "*JPARSEC*", "\\^", true);
		}
		if (this.avoidFormatting.indexOf("{")<0) {
			text = DataSet.replaceAll(text, "{", "*JPARSEC*", true);
			text = DataSet.replaceAll(text, "*JPARSEC*", "\\{", true);
		}
		if (this.avoidFormatting.indexOf("}")<0) {
			text = DataSet.replaceAll(text, "}", "*JPARSEC*", true);
			text = DataSet.replaceAll(text, "*JPARSEC*", "\\}", true);
		}
		if (this.avoidFormatting.indexOf("~")<0) {
			text = DataSet.replaceAll(text, "~", "*JPARSEC*", true);
			text = DataSet.replaceAll(text, "*JPARSEC*", "\\~", true);
		}
		if (this.avoidFormatting.indexOf("#")<0) {
			text = DataSet.replaceAll(text, "#", "*JPARSEC*", true);
			text = DataSet.replaceAll(text, "*JPARSEC*", "\\#", true);
		}
		if (this.avoidFormatting.indexOf("%")<0) {
			text = DataSet.replaceAll(text, "%", "*JPARSEC*", true);
			text = DataSet.replaceAll(text, "*JPARSEC*", "\\%", true);
		}
		if (this.avoidFormatting.indexOf("&")<0) {
			text = DataSet.replaceAll(text, "&", "*JPARSEC*", true);
			text = DataSet.replaceAll(text, "*JPARSEC*", "\\&", true);
		}
		if (this.avoidFormatting.indexOf(">")<0) {
			text = DataSet.replaceAll(text, ">", "*JPARSEC*", true);
			text = DataSet.replaceAll(text, "*JPARSEC*", "$>$", true);
		}
		if (this.avoidFormatting.indexOf("<")<0) {
			text = DataSet.replaceAll(text, "<", "*JPARSEC*", true);
			text = DataSet.replaceAll(text, "*JPARSEC*", "$<$", true);
		}
		if (this.avoidFormatting.indexOf("\u00e1")<0) text = DataSet.replaceAll(text, "\u00e1", "\\'{a}", true);
		if (this.avoidFormatting.indexOf("\u00e9")<0) text = DataSet.replaceAll(text, "\u00e9", "\\'{e}", true);
		if (this.avoidFormatting.indexOf("\u00ed")<0) text = DataSet.replaceAll(text, "\u00ed", "\\'{i}", true);
		if (this.avoidFormatting.indexOf("\u00f3")<0) text = DataSet.replaceAll(text, "\u00f3", "\\'{o}", true);
		if (this.avoidFormatting.indexOf("\u00fa")<0) text = DataSet.replaceAll(text, "\u00fa", "\\'{u}", true);
		if (this.avoidFormatting.indexOf("\u00f1")<0) text = DataSet.replaceAll(text, "\u00f1", "\\~n", true);
		if (this.avoidFormatting.indexOf("+/-")<0) text = DataSet.replaceAll(text, "+/-", "$\\pm$", true);
		if (this.avoidFormatting.indexOf("\u00b0")<0) text = DataSet.replaceAll(text, "\u00b0", "$^\\circ$", true);
		if (this.avoidFormatting.indexOf("\u00c1")<0) text = DataSet.replaceAll(text, "\u00c1", "\\'{A}", true);
		if (this.avoidFormatting.indexOf("\u00c9")<0) text = DataSet.replaceAll(text, "\u00c9", "\\'{E}", true);
		if (this.avoidFormatting.indexOf("\u00cd")<0) text = DataSet.replaceAll(text, "\u00cd", "\\'{I}", true);
		if (this.avoidFormatting.indexOf("\u00d3")<0) text = DataSet.replaceAll(text, "\u00d3", "\\'{O}", true);
		if (this.avoidFormatting.indexOf("\u00da")<0) text = DataSet.replaceAll(text, "\u00da", "\\'{U}", true);
		if (this.avoidFormatting.indexOf("\u00d1")<0) text = DataSet.replaceAll(text, "\u00d1", "\\~N", true);

		return text;
	}

	/**
	 * Replaces normal symbols by it's Latex representation. Currently only for &lt;, &gt;,
	 * &aacute;, &eacute;, &iacute;, &oacute;, &uacute;, +/-, {, }, _, ^, ~, \, $, #, %, &amp;, &deg;, &ntilde;. Default symbols to avoid
	 * formatting are \{}~.
	 *
	 * @param text Input text.
	 * @return Output table.
	 */
	public static String format(String text)
	{
		if (text == null) return text;
		if (text.equals("")) return text;

		String avoidFormatting = "\\{}~";

		if (avoidFormatting.indexOf("\\")<0) text = DataSet.replaceAll(text, "\\", "*JPARSEC_BAR*", true);
		if (avoidFormatting.indexOf("$")<0) {
			text = DataSet.replaceAll(text, "$", "*JPARSEC*", true);
			text = DataSet.replaceAll(text, "*JPARSEC*", "\\$", true);
		}
		if (avoidFormatting.indexOf("\\")<0) text = DataSet.replaceAll(text, "*JPARSEC_BAR*", "$\\backslash$", true);
		if (avoidFormatting.indexOf("_")<0) {
			text = DataSet.replaceAll(text, "_", "*JPARSEC*", true);
			text = DataSet.replaceAll(text, "*JPARSEC*", "\\_", true);
		}
		if (avoidFormatting.indexOf("^")<0) {
			text = DataSet.replaceAll(text, "^", "*JPARSEC*", true);
			text = DataSet.replaceAll(text, "*JPARSEC*", "\\^", true);
		}
		if (avoidFormatting.indexOf("{")<0) {
			text = DataSet.replaceAll(text, "{", "*JPARSEC*", true);
			text = DataSet.replaceAll(text, "*JPARSEC*", "\\{", true);
		}
		if (avoidFormatting.indexOf("}")<0) {
			text = DataSet.replaceAll(text, "}", "*JPARSEC*", true);
			text = DataSet.replaceAll(text, "*JPARSEC*", "\\}", true);
		}
		if (avoidFormatting.indexOf("~")<0) {
			text = DataSet.replaceAll(text, "~", "*JPARSEC*", true);
			text = DataSet.replaceAll(text, "*JPARSEC*", "\\~", true);
		}
		if (avoidFormatting.indexOf("#")<0) {
			text = DataSet.replaceAll(text, "#", "*JPARSEC*", true);
			text = DataSet.replaceAll(text, "*JPARSEC*", "\\#", true);
		}
		if (avoidFormatting.indexOf("%")<0) {
			text = DataSet.replaceAll(text, "%", "*JPARSEC*", true);
			text = DataSet.replaceAll(text, "*JPARSEC*", "\\%", true);
		}
		if (avoidFormatting.indexOf("&")<0) {
			text = DataSet.replaceAll(text, "&", "*JPARSEC*", true);
			text = DataSet.replaceAll(text, "*JPARSEC*", "\\&", true);
		}
		if (avoidFormatting.indexOf(">")<0) {
			text = DataSet.replaceAll(text, ">", "*JPARSEC*", true);
			text = DataSet.replaceAll(text, "*JPARSEC*", "$>$", true);
		}
		if (avoidFormatting.indexOf("<")<0) {
			text = DataSet.replaceAll(text, "<", "*JPARSEC*", true);
			text = DataSet.replaceAll(text, "*JPARSEC*", "$<$", true);
		}
		if (avoidFormatting.indexOf("\u00e1")<0) text = DataSet.replaceAll(text, "\u00e1", "\\'{a}", true);
		if (avoidFormatting.indexOf("\u00e9")<0) text = DataSet.replaceAll(text, "\u00e9", "\\'{e}", true);
		if (avoidFormatting.indexOf("\u00ed")<0) text = DataSet.replaceAll(text, "\u00ed", "\\'{i}", true);
		if (avoidFormatting.indexOf("\u00f3")<0) text = DataSet.replaceAll(text, "\u00f3", "\\'{o}", true);
		if (avoidFormatting.indexOf("\u00fa")<0) text = DataSet.replaceAll(text, "\u00fa", "\\'{u}", true);
		if (avoidFormatting.indexOf("\u00f1")<0) text = DataSet.replaceAll(text, "\u00f1", "\\~n", true);
		if (avoidFormatting.indexOf("+/-")<0) text = DataSet.replaceAll(text, "+/-", "$\\pm$", true);
		if (avoidFormatting.indexOf("\u00b0")<0) text = DataSet.replaceAll(text, "\u00b0", "$^\\circ$", true);
		if (avoidFormatting.indexOf("\u00c1")<0) text = DataSet.replaceAll(text, "\u00c1", "\\'{A}", true);
		if (avoidFormatting.indexOf("\u00c9")<0) text = DataSet.replaceAll(text, "\u00c9", "\\'{E}", true);
		if (avoidFormatting.indexOf("\u00cd")<0) text = DataSet.replaceAll(text, "\u00cd", "\\'{I}", true);
		if (avoidFormatting.indexOf("\u00d3")<0) text = DataSet.replaceAll(text, "\u00d3", "\\'{O}", true);
		if (avoidFormatting.indexOf("\u00da")<0) text = DataSet.replaceAll(text, "\u00da", "\\'{U}", true);
		if (avoidFormatting.indexOf("\u00d1")<0) text = DataSet.replaceAll(text, "\u00d1", "\\~N", true);

		return text;
	}
}
