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
package jparsec.graph;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Vector;

import jparsec.io.image.Picture;
import jparsec.math.Constant;
import jparsec.math.LATEXFormula;
import jparsec.util.JPARSECException;

/**
 * This class is designed to bundle together all the information required
 * to draw short Strings with subscripts and superscripts.<P>
 *
 * The string should be encoded following some rules:<P>
 * - Superscripts and subscript using ^ and _ characters, followed by the text to be drawn
 * in superscript or subscript.<P>
 * - You can use ^{superscript} to delimited the portion to be superscripted or subscripted.<P>
 * - You can use '@COMMAND' to use a special command. Available commands are Greek letters
 * ('@alpha' for small and '@ALPHA', '@BETA' for capital, see a list of available Greek letter in Constellation class), colors ('@RED',
 * '@BLUE', and so on, using any color directly provided as constant in Java's Color class),
 * size change ('@SIZExx', where xx is the size of the text), and @ FOLLOWED BY BOLD, ITALIC, or PLAIN.<P>
 * - You can draw latex formulas with the command '@LATEX{}', with the latex formula between the {}.<P>
 * - You can draw digital clock symbols with the command '@CLOCK{}', with the numbers between the {}.
 * Besides numbers, supported characters are hms&deg;'": _-dDbBtTCUL. The plus + is mapped to a blank space of half width.
 * In case of bold effect applied, the digits will be drawn with a blur effect.<P>
 * - You can rotate text with the '@ROTATExxx' command, being xxx the angle in degrees.<P>
 * - You can increase/decrease text size with '@SIZE+x' and '@SIZE-x'.<P>
 * - You can use transparent color to hide something with '@TRANSPARENT'.<P>
 * - You can justify text to the right, center, or left of the input position using '@RIGHT', '@CENTER', and '@LEFT'.<P>
 * - You can insert a blank space inside a subscript/superscript with '@SPACE', in those cases where
 * programatically the text is split or cut whenever a blank space like ' ' is found.<P>
 * - It is recommended to clear the list of text states whenever a new text if written with {@link TextLabel#clearTextStateList()}.<P>
 *
 *  Example:<P>
 *  ln(@REDy{^@ORANGEx_i}@SIZE20@GREENHI@BLUE@ALPHA@BETA@GAMMA@SIZE10@BLACK) = ln (y^xi HI alfa beta gamma ),
 *  where the Greek characters will be drawn in Greek, HI in a bigger size, and some portions in different colors.
 *  <P>
 *  Other special symbols include the sun, the planets, and the constellations. However, all these
 *  symbols will be drawn only if they are available in the system.
 *
 * @version $Revision: 2.00 $, $Date: 2007/10/28$
 * @version $Revision: 1.10 $, $Date: 1996/09/05 04:53:27 $
 * @author  T. Alonso Albi - OAN (Spain)
 * @author  Leigh Brookshaw
 */
public class TextLabel extends Object {
	private static Color digitalClockOutColor = null;
	/**
	 * Sets the color to be used to draw segments of a digital clock
	 * where there's no 'light', including transparency. Default is
	 * null to use a gray color.
	 * @param col The color.
	 */
	public static void setDigitalClockOutColor(Color col) {
		digitalClockOutColor = col;
	}

	/**
	 * The set of align values.
	 */
	public enum ALIGN {
		/** Center the Text over the point. */
		CENTER,
		/** Position the Text to the Left of the  point. */
		LEFT,
		/** Position the Text to the Right of the  point. */
		RIGHT,
		/** Position the Text to the left of the  point and displaced 1/2 of its width. */
		LEFT_PLUS
	};

	/**
	 * The possible formatting for doubles.
	 */
	public enum DOUBLE_FORMAT {
		/** Format to use when parsing a double. */
		SCIENTIFIC,
		/** Format to use when parsing a double. */
		ALGEBRAIC
	};

  /**
   ** Minimum Point size allowed for script characters
   */
  final static int MINIMUM_SIZE  =  2;

  /**
   * Decrease in size of successive script levels.
   */
     private double script_fraction = 0.55;
  /**
   * Superscript offset.
   */
     private double sup_offset      = 0.6;
  /**
   * Subscript offset.
   */
     private double sub_offset      = 1.1;
  /**
   * Font to use for text.
   */
     private Font font     = null;
  /**
   * Text color.
   */
     private Color color   = null;
  /**
   * Background Color.
   */
     private Color background   = null;
  /**
   * Background Color for images.
   */
     private Color background2   = null;
  /**
   * The text to display.
   */
     private String text   = null;
  /**
   * The logical name of the font to use.
   */
     private String fontname  = "Dialog";
  /**
   * The font size.
   */
     private int    fontsize  = 0;
  /**
   * The font style.
   */
     private int    fontstyle = Font.PLAIN;
  /**
   * Text justification. Either CENTER, LEFT or RIGHT.
   */
     private ALIGN justification = TextLabel.ALIGN.LEFT;
  /**
   * The width of the text using the current Font.
   */
     private int width   = 0;
  /**
   * The ascent using the current font.
   */
     private int ascent  = 0;
  /**
   * The maximum ascent using the current font.
   */
     private int maxAscent  = 0;
  /**
   * The descent using the current font.
   */
     private int descent = 0;
  /**
   * The maximum descent using the current font.
   */
     private int maxDescent = 0;
  /**
   * The height using the current font ie ascent+descent+leading.
   */
     private int height = 0;
  /**
   * The leading using the current font.
   */
     private int leading = 0;
  /**
   * Has the string been parsed! This only needs to be done once
   * unless the font is altered.
   */
     private boolean parse = true;

  /**
   * Local graphics context.
   */
     private Graphics lg = null;
  /**
   * The parsed string. Each element in the vector represents
   * a change of context in the string ie font change and offset.
   */
     private Vector<TextState> list = new Vector<TextState>(8,4);
     private static Vector<TextState> listCopy = new Vector<TextState>();

     private ArrayList<LATEXFormula> formulas = new ArrayList<LATEXFormula>();
     private ArrayList<DigitalClock> dclock = new ArrayList<DigitalClock>();
     private static boolean renderUnicodeAsImages = false;
     /**
      * This flag determines if an attempt will be made to export Greek fonts
      * to a Graphics context belonging to a PDFGraphics2D instance or similar.
      * What this does is to render Greek fonts using the font {@linkplain Font#SERIF}
      * from Java and 'a' for the Greek alpha, 'b' for beta, and so on. In an external
      * app calling this you will need to map the {@linkplain Font#SERIF} font
      * to font 'Symbol', in case this font is available in your system, as usually
      * happens. This is a hack that works fine for the iText library, but requires
      * an adequate FontMapper object. Default value is false, that allows to export Greek
      * characters to images and render them in the PDF (but not in vector graphics).
      */
     public  static boolean TRY_TO_EXPORT_GREEK_CHARACTERS_TO_PDF = false;

  /**
   * Instantiate the class.
   */
     public TextLabel() { }
  /**
   * Instantiate the class.
   * @param s String to parse.
   */
     public TextLabel(String s) {
            this.text = s;
	  }
  /**
   * Instantiate the class.
   * @param s String to parse.
   * @param f Font to use.
   */
     public TextLabel(String s, Font f) {
            this(s);
            font      = new Font(f.getName(), f.getStyle(), f.getSize());
            fontname  = f.getName();
            fontstyle = f.getStyle();
            fontsize  = f.getSize();
	  }
  /**
   * Instantiate the class.
   * @param s String to parse.
   * @param f Font to use.
   * @param c Color to use
   * @param j Justification.
   */
     public TextLabel(String s, Font f, Color c, ALIGN j) {
            this(s,f);
            color  = c;
            justification = j;
 	  }
     /**
      * Instantiate the class.
      * @param s String to parse.
      * @param c Color to use
      * @param j Justification.
      */
        public TextLabel(String s, Color c, ALIGN j) {
               this(s);
               color  = c;
               justification = j;
    	  }
  /**
   * Instantiate the class.
   * @param s String to parse.
   * @param c Color to use.
   */
     public TextLabel(String s, Color c) {
            this(s);
            color = c;
	  }
  /**
   * Instantiate the class.
   * @param f Font to use.
   * @param c Color to use.
   * @param j Justification.
   */
     public TextLabel(Font f, Color c, ALIGN j) {
    	 this("", f);
            color  = c;
            justification = j;
 	  }

     /**
      * Clones this instance.
      */
     @Override
     public TextLabel clone()
     {
         TextLabel tl = new TextLabel(this.text, font,color,justification);
         tl.ascent = this.ascent;
         tl.background = this.background;
         tl.background2 = this.background2;
         tl.descent = this.descent;
         tl.fontname = this.fontname;
         tl.fontsize = this.fontsize;
         tl.fontstyle = this.fontstyle;
         tl.height = this.height;
         tl.width = this.width;
         tl.leading = this.leading;
         tl.lg = this.lg.create();
         tl.list = null;
         if (this.list != null) {
             tl.list = new Vector<TextState>();
        	 for (int i=0; i<list.size(); i++) {
        		 tl.list.add(list.get(i).copyAll());
        	 }
         }
         tl.maxAscent = this.maxAscent;
         tl.maxDescent = this.maxDescent;
         tl.parse = this.parse;
         tl.script_fraction = this.script_fraction;
         tl.sub_offset = this.sub_offset;
         tl.sup_offset = this.sup_offset;
         return tl;
     }
     /**
      * Returns if this instance is equals to a given object.
      */
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TextLabel)) return false;

        TextLabel textLabel = (TextLabel) o;

        if (Double.compare(textLabel.script_fraction, script_fraction) != 0) return false;
        if (Double.compare(textLabel.sup_offset, sup_offset) != 0) return false;
        if (Double.compare(textLabel.sub_offset, sub_offset) != 0) return false;
        if (fontsize != textLabel.fontsize) return false;
        if (fontstyle != textLabel.fontstyle) return false;
        if (width != textLabel.width) return false;
        if (ascent != textLabel.ascent) return false;
        if (maxAscent != textLabel.maxAscent) return false;
        if (descent != textLabel.descent) return false;
        if (maxDescent != textLabel.maxDescent) return false;
        if (height != textLabel.height) return false;
        if (leading != textLabel.leading) return false;
        if (parse != textLabel.parse) return false;
        if (font != null ? !font.equals(textLabel.font) : textLabel.font != null) return false;
        if (color != null ? !color.equals(textLabel.color) : textLabel.color != null) return false;
        if (background != null ? !background.equals(textLabel.background) : textLabel.background != null) return false;
        if (background2 != null ? !background2.equals(textLabel.background2) : textLabel.background2 != null)
            return false;
        if (text != null ? !text.equals(textLabel.text) : textLabel.text != null) return false;
        if (fontname != null ? !fontname.equals(textLabel.fontname) : textLabel.fontname != null) return false;
        if (justification != textLabel.justification) return false;
        if (lg != null ? !lg.equals(textLabel.lg) : textLabel.lg != null) return false;
        if (list != null ? !list.equals(textLabel.list) : textLabel.list != null) return false;
        if (formulas != null ? !formulas.equals(textLabel.formulas) : textLabel.formulas != null) return false;

        return !(dclock != null ? !dclock.equals(textLabel.dclock) : textLabel.dclock != null);
    }

	@Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(script_fraction);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(sup_offset);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(sub_offset);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (font != null ? font.hashCode() : 0);
        result = 31 * result + (color != null ? color.hashCode() : 0);
        result = 31 * result + (background != null ? background.hashCode() : 0);
        result = 31 * result + (background2 != null ? background2.hashCode() : 0);
        result = 31 * result + (text != null ? text.hashCode() : 0);
        result = 31 * result + (fontname != null ? fontname.hashCode() : 0);
        result = 31 * result + fontsize;
        result = 31 * result + fontstyle;
        result = 31 * result + (justification != null ? justification.hashCode() : 0);
        result = 31 * result + width;
        result = 31 * result + ascent;
        result = 31 * result + maxAscent;
        result = 31 * result + descent;
        result = 31 * result + maxDescent;
        result = 31 * result + height;
        result = 31 * result + leading;
        result = 31 * result + (parse ? 1 : 0);
        result = 31 * result + (lg != null ? lg.hashCode() : 0);
        result = 31 * result + (list != null ? list.hashCode() : 0);
        result = 31 * result + (formulas != null ? formulas.hashCode() : 0);
        result = 31 * result + (dclock != null ? dclock.hashCode() : 0);
        return result;
    }

  /**
   * Create a New TextLabel object copying the state of the existing
   * object into the new one. The state of the class is the color,
   * font, and justification ie everything but the string.
   * @return The copy.
   */
     public TextLabel copyState() {
            return new TextLabel(new Font(font.getName(), font.getStyle(), font.getSize()),color,justification);
     }

  /**
   * Copy the state of the parsed TextLabel into the existing
   * object.
   * @param t The TextLabel to get the state information from.
   */
     public void copyState(TextLabel t) {
            if(t==null) return;

            font  = t.getFont();
            color = t.getColor();
            justification = t.getJustification();

            if(font == null) return;
            fontname  = font.getName();
            fontstyle = font.getStyle();
            fontsize  = font.getSize();

            parse  = true;
     }
  /**
   * Set the Font to use with the class.
   * @param f Font.
   */
     public void setFont(  Font f   ) {
            font      = new Font(f.getName(), f.getStyle(), f.getSize());
            fontname  = f.getName();
            fontstyle = f.getStyle();
            fontsize  = f.getSize();
            parse = true;

     }
  /**
   * Set the String to use with the class.
   * @param s String.
   */
     public void setText(  String s ) {
            text   = s;
            parse = true;
     }

  /**
   * Set the Color to use with the class.
   * @param c Color.
   */
     public void setColor( Color c  ) {
            color = c;
     }
  /**
   * Set the Background Color to use with the class.
   * @param c Color.
   */
     public void setBackground( Color c  ) {
            background = c;
     }
  /**
   * Set the Background Color to use as background for Latex formulae.
   * @param c Color.
   */
     public void setBackgroundColorForImages( Color c  ) {
            background2 = c;
     }


  /**
   * Set the Justification to use with the class.
   * @param i Justification.
   */
     public void setJustification( ALIGN i ) {
    	 justification = i;
	}


     /**
      * Returns the font in use.
      * @return The font the class is using.
      */
        public Font   getFont()  {
        	return font;
        }
     /**
      * Returns the text to draw.
      * @return The String the class is using.
      */
        public String getText()  {
        	return text;
        }

     /**
      * Returns the current color.
      * @return The Color the class is using.
      */
        public Color  getColor() {
        	return color;
        }
     /**
      * Returns the background color.
      * @return The Background Color the class is using.
      */
        public Color  getBackground() {
        	return background;
        }
     /**
      * Returns the justification.
      * @return The Justification the class is using.
      */
        public ALIGN getJustification() {
        	return justification;
        }

     /**
      * Returns the font metrics.
      * @param g Graphics context.
      * @return The Font metrics the class is using.
      */
        public FontMetrics getFM(Graphics g) {
            if(g==null) return null;

            if(font==null) return g.getFontMetrics();
            else           return g.getFontMetrics(font);
        }

        /**
         * Returns the width of the character.
         * @param g Graphics context.
         * @param ch The character.
         * @return The width of the character.
         */
           public int charWidth(Graphics g, char ch) {
               FontMetrics fm;
               if(g==null) return 0;

               if(font==null) fm =  g.getFontMetrics();
               else           fm =  g.getFontMetrics(font);

               return fm.charWidth(ch);
           }

        /**
         * Returns the width of the text.
         * @param g Graphics context.
         * @return The width of the parsed text.
         */
           public int getWidth(Graphics g) {

	    		if (!text.contains("@") && !text.contains("^") && !text.contains("_")) {
	    			width = g.getFontMetrics().stringWidth(text);
	    		} else {
	                parseText(g.create(), true);
	    		}

               return width;
           }

        /**
         * Returns the height of the text.
         * @param g Graphics context.
         * @return The height of the parsed text.
         */
           public int getHeight(Graphics g) {
               parseText(g.create(), true);

               return height;
           }

        /**
         * Returns the ascent.
         * @param g Graphics context.
         * @return the ascent of the parsed text.
         */
           public int getAscent(Graphics g) {
               if(g == null) return 0;

               parseText(g.create(), true);

               return ascent;
           }

        /**
         * Returns the maximum ascent.
         * @param g Graphics context.
         * @return The maximum ascent of the parsed text.
         */
           public int getMaxAscent(Graphics g) {
               if(g == null) return 0;

               parseText(g.create(), true);

               return maxAscent;
           }

        /**
         * Returns the descent.
         * @param g Graphics context.
         * @return The descent of the parsed text.
         */
           public int getDescent(Graphics g) {
               if(g == null) return 0;

               parseText(g.create(), true);

               return descent;
           }

        /**
         * Returns the maximum descent.
         * @param g Graphics context.
         * @return The maximum descent of the parsed text.
         */
           public int getMaxDescent(Graphics g) {
               if(g == null) return 0;

               parseText(g.create(), true);

               return maxDescent;
           }

      /**
       * Returns the leading of the text.
       * @param g Graphics context.
       * @return The leading of the parsed text.
       */
       public int getLeading(Graphics g) {
           if(g == null) return 0;

           parseText(g.create(), true);

           return leading;
       }

       /**
        * Set to true to render Unicode characters (Greek characters for example)
        * as images. Default is false, but true maybe be useful to export to a
        * Graphics context that doesn't support Unicode easily, like PDF output.
        * @param renderUnicode True or false.
        */
       public static void setRenderUnicodeAsImages(boolean renderUnicode) {
    	   renderUnicodeAsImages = renderUnicode;
       }
       /**
        * Returns true if the text contains special characters, like
        * Greek characters and other Unicodes.
        * @return True or false.
        */
       public boolean containSpecialCharacter() {
           if (text == null) return false;

           String lower = text.toLowerCase();
    	   for (String cmd : commands) {
    		   if (lower.contains("@" + cmd)) {
    			   return true;
    		   }
    	   }

    	   return false;
       }
       private static final String commands[] = new String[] {
    	     "RED", "GREEN", "BLUE", "SIZE", "BOLD", "ITALIC", "PLAIN", "BLACK", "WHITE",
      		 "CYAN", "GRAY", "MAGENTA", "ORANGE", "PINK", "YELLOW", "alpha", "beta", "gamma",
      		 "delta", "epsilon", "zeta", "eta", "theta", "iota", "kappa", "lambda", "mu", "nu",
   			 "xi", "omicron", "pi", "rho", "sigma", "tau", "upsilon", "phi", "chi", "psi", "omega",
   			 "sun", "mercury", "venus", "earth", "mars", "jupiter",
   			 "saturn", "uranus", "neptune", "pluto", "aries", "taurus",
   			 "gemini", "cancer", "leo", "virgo", "libra", "scorpius",
   			 "sagittarius", "capricornus", "aquarius", "pisces"};

       private static final String[] greek = new String[] {
    	    "\u03B1", "\u03B2", "\u03B3",
    	    "\u03B4", "\u03B5", "\u03B6", "\u03B7", "\u03B8", "\u03B9", "\u03BA", "\u03BB", "\u03BC", "\u03BD",
    	    "\u03BE", "\u03BF",	"\u03C0", "\u03C1", //"\u03C2",
     		"\u03C3", "\u03C4", "\u03C5", "\u03C6", "\u03C7", "\u03C8", "\u03C9",

     		"\u2299", "\u2640", "\u2641", "\u2295", "\u2642", "\u2643", "\u2644",
     		"\u2645", "\u2646", "\u2647", "\u9800", "\u9801", "\u9802", "\u9803",
     		"\u9804", "\u9805", "\u9806", "\u9807", "\u9808", "\u9809", "\u9810",
     		"\u9811"};

       private static final String[] greekCapital = new String[] {
    	   "\u0391", "\u0392", "\u0393",
    	   "\u0394", "\u0395", "\u0396", "\u0397", "\u0398", "\u0399", "\u039A", "\u039B", "\u039C", "\u039D",
    	   "\u039E", "\u039F",	"\u03A0", "\u03A1",
    		"\u03A3", "\u03A4", "\u03A5", "\u03A6", "\u03A7", "\u03A8", "\u03A9"
       };

       private static final String[] greekLATEX = new String[] {
    	   "\\alpha", "\\beta", "\\gamma",
    	   "\\delta", "\\epsilon", "\\zeta", "\\eta", "\\theta", "\\iota", "\\kappa", "\\lambda", "\\mu", "\\nu",
    	   "\\xi", "\\omicron", "\\pi", "\\rho",
    		"\\sigma", "\\tau", "\\upsilon", "\\phi", "\\chi", "\\psi", "\\omega"
    		//, "\\cdot", "\\rightarrow", "\\leftarrow"
       };
       private static final String[] greekCapitalLATEX = new String[] {
    	   "A", "B", "\\Gamma",
    	   "\\Delta", "E", "Z", "H", "\\Theta", "I", "K", "\\Lambda", "M", "N",
    	   "\\Xi", "O", "\\Pi", "P",
    		"\\Sigma", "T", "\\Upsilon", "\\Phi", "X", "\\Psi", "\\Omega"
    		//, "\\cdot", "\\rightarrow", "\\leftarrow"
       };

       // To be used with font Symbol when exporting to PDF
       private static final char[] greekPDF = new char[] {'a', 'b', 'g', 'd', 'e',
    		'z', 'h', 'q', 'i', 'k', 'l', 'm', 'n', 'x', 'o',
    		'p', 'r', 's', 't', 'u', 'f', 'c', 'y', 'w'}; //, '\u2022', '\t', '\u00ac'}; // '\u2022','',''
       private static final char[] greekCapitalPDF = new char[] {'A', 'B', 'G', 'D', 'E',
    		'Z', 'H', 'Q', 'I', 'K', 'L', 'M', 'N', 'X', 'O',
    		'P', 'R', 'S', 'T', 'U', 'F', 'C', 'Y', 'W'}; //, '\u2022', '\t', '\u00ac'};

       /**
        * Clears the list of text states.
        */
       public static void clearTextStateList() {
           listCopy = new Vector<TextState>();
       }

	  /**
	   * Parse the text. When the text is parsed the width, height, leading
	   * are all calculated. The text will only be truly parsed if
	   * the graphics context has changed or the text has changed or
	   * the font has changed. Otherwise nothing is done when this
	   * method is called.
	   * @param g Graphics context.
	   * @param justWidth True if only the width of the string is requested (faster),
	   * false to draw everything.
	   */
       public void parseText(Graphics g, boolean justWidth) {
    	 Stack<TextState> state = new Stack<TextState>();
         TextState current = new TextState();
         char ch;
         int w;

         if(lg != g) parse = true;
         lg = g;

         if(!parse) return;

         parse = false;
         width   = 0;
         leading = 0;
         ascent  = 0;
         descent = 0;
         height  = 0;
         maxAscent = 0;
         maxDescent = 0;

         if( text == null || g == null) return;

         list.removeAllElements();

         if(font == null) current.f = g.getFont();
         else             current.f = font;

         state.push(current);
         list.addElement(current);
         if (listCopy.size() > 0) clearTextStateList();
         if (dclock.size() > 0) dclock = new ArrayList<DigitalClock>();
         if (formulas.size() > 0) formulas = new ArrayList<LATEXFormula>();
         listCopy.add(current);

         //String num = "0123456789";
         for(int i=0; i<text.length(); i++) {
             ch = text.charAt(i);

             switch (ch) {
             case '@':
                      i++;
                      int k = -1;
                      boolean capital = false;
                      for (int j=0;j<commands.length; j++)
                      {
                    	  int end = i+commands[j].length();
                    	  int kk = -1;
                    	  if (end >= text.length()) {
                    		  kk = text.substring(i).toUpperCase().indexOf(commands[j].toUpperCase());
                    	  } else {
                    		  kk = text.substring(i, i+commands[j].length()).toUpperCase().indexOf(commands[j].toUpperCase());
                    	  }
                    	  if (kk == 0) {
                    		  String t = text.substring(i);
                        	  if (end < text.length()) t = text.substring(i, i+commands[j].length());
                    		  k = j;
                    		  i = i + commands[j].length();
                    		  if (t.toUpperCase().equals(t)) capital = true;
                    		  break;
                    	  }
                      }
                      if (k == -1)
                      {
                    	  String t = text.substring(i);
                    	  if (t.startsWith("latex{") || t.startsWith("LATEX{") || t.startsWith("clock{") ||
                    			  t.startsWith("CLOCK{")) {
                    		  boolean clock = false;
                          	  if (t.startsWith("clock{") || t.startsWith("CLOCK{")) clock = true;
                    		  t = t.substring(6);
                    		  int end = -1;
                    		  for (int j=0;j<t.length(); j++) {
                    			  if (t.substring(j, j+1).equals("{")) end ++;
                    			  if (t.substring(j, j+1).equals("}")) end --;
                    			  if (end < -1) {
                    				  end = j;
                    				  break;
                    			  }
                    		  }
                    		  if (end == -1) end = t.length();
                    		  String f = t.substring(0, end);
                     		  i = i + 6 + end;

                     		  int initS = current.f.getSize();
                     		  int maxH = (int) (lg.getFontMetrics().getHeight() * 1.33);

                         	  if (clock) {
                          		  int cw = (initS*4)/10-1;
                          		  int chh = cw/4, cb = chh/5;
                          		  if (cb == 0) cb = 1;
                          		  Color out = digitalClockOutColor;
                          		  if (out == null) out = new Color(128, 128, 128, 48);
                          		DigitalClock dc = new DigitalClock(cw, chh, cb, this.getColor(), out, current.f.isBold(), current.f.isItalic());
                          		dc.setString(f);
                          		int ww = 0;
                        		for (int in=0; in<f.length(); in++) {
                        			try {
                        				BufferedImage image = dc.getDigitImage(f.substring(in, in + 1));
                        				ww += image.getWidth();
                        			} catch (Exception exc) {}
                        		}
                        		width += ww;
                        		dclock.add(dc);
                          	  } else {
                          		  LATEXFormula lf = null;
	                     		  do {
	                     			  lf = new LATEXFormula(f, current.f.getStyle(), initS);
	                     			  initS = initS - 1;
	                     		  } while(lf.getAsImage().getHeight() > maxH);
	                    		  formulas.add(lf);
	                    		  BufferedImage img = lf.getAsImage();
	    	            		  width += img.getWidth();
                          	  }

                              w = current.getWidth(g);
                              if(!current.isEmpty()) {
                                   current = current.copyState();
                                   list.addElement(current);
                              }

                          	  if (clock) {
                          		  current.s = new StringBuffer("@CLOCK");
                          	  } else {
                          		  current.s = new StringBuffer("@FORMULA");
                          	  }
                    		  current.x += w;
                    	  } else {
                    		  if (t.toLowerCase().startsWith("rotate")) {
                    			  t = text.substring(i+6);
                    			  String number = "0123456789.-";
                    			  int n = -1;
                    			  do {
                    				  n ++;
                    			  } while (n+1 < t.length() && number.contains(t.substring(n,n+1)) && n < 3);
                    			  if (n > 0) {
                        			  i = i + 6 + n - 1;
                        			  current.angle = DataSet.parseDouble(t.substring(0, n)) * Constant.DEG_TO_RAD;
                    			  } else {
    		                    	  if(i<text.length()) {
    		                    		  current.s.append(text.charAt(i));
    		                    	  }
                    			  }
                    		  } else {
                        		  if (t.toLowerCase().startsWith("transparent")) {
                        			  current.col = new Color(0, 0, 0, 0);
                        			  i = i + 10;
                        		  } else {
                            		  if (t.toLowerCase().startsWith("space")) {
                            			  current.s.append(" ");
                            			  i = i + 4;
                            		  } else {
                                		  if (t.toLowerCase().startsWith("leftplus")) {
                                			  this.setJustification(TextLabel.ALIGN.LEFT_PLUS);
                                			  i = i + 7;
                                		  } else {
	                                		  if (t.toLowerCase().startsWith("left")) {
	                                			  this.setJustification(TextLabel.ALIGN.LEFT);
	                                			  i = i + 3;
	                                		  } else {
	                                    		  if (t.toLowerCase().startsWith("right")) {
	                                    			  this.setJustification(TextLabel.ALIGN.RIGHT);
	                                    			  i = i + 4;
	                                    		  } else {
	                                        		  if (t.toLowerCase().startsWith("center")) {
	                                        			  this.setJustification(TextLabel.ALIGN.CENTER);
	                                        			  i = i + 5;
	                                        		  } else {
							                    		  current.s.append(text.charAt(i-1));
								                    	  if(i<text.length()) current.s.append(text.charAt(i));
	                                        		  }
	                                    		  }
	                                		  }
                                		  }
                            		  }
                        		  }
                    		  }
                    	  }
                      } else {
                          w = current.getWidth(g);
                          if(!current.isEmpty()) {
                               current = current.copyState();
                               current.f = new Font(this.fontname, current.f.getStyle(), current.f.getSize());
                               list.addElement(current);
                          }

                          current.x += w;

                          if (k == 0) current.col = Color.RED;
                    	  if (k == 1) current.col = Color.GREEN;
                    	  if (k == 2) current.col = Color.BLUE;
                    	  if (k == 7) current.col = Color.BLACK;
                    	  if (k == 8) current.col = Color.WHITE;
                    	  if (k == 9) current.col = Color.CYAN;
                    	  if (k == 10) current.col = Color.GRAY;
                    	  if (k == 11) current.col = Color.MAGENTA;
                    	  if (k == 12) current.col = Color.ORANGE;
                    	  if (k == 13) current.col = Color.PINK;
                    	  if (k == 14) current.col = Color.YELLOW;
                    	  if (k > 14) {
                    		  if (renderUnicodeAsImages && !justWidth && (k<40 || k == 42)) {
                    			  int initS = current.f.getSize();
                    			  LATEXFormula lf = null;
                    			  int maxH = lg.getFontMetrics().getHeight() * 11 / 8;
 	    	            		  String text0 = "\\"+commands[k];
 	    	            		  if (capital && k-15 < greekCapitalLATEX.length) text0 = greekCapitalLATEX[k-15];
 	    	            		  if (k==39) text0 = "\\odot";
 	    	            		  if (k==42) text0 = "\\oplus";
 	                     		  do {
 	                     			  lf = new LATEXFormula(text0, initS);
 	                     			  initS = initS - 1;
 	                     		  } while(initS > 2 && lf.getAsImage().getHeight() > maxH);
 	                    		  formulas.add(lf);
 	                    		  BufferedImage img = lf.getAsImage();
 	    	            		  width += img.getWidth();
 	    	            		  String text1 = text.substring(0, i)+"@LATEX{"+text0+"}";
 	    	            		  if (text.length() > i) text1 += text.substring(i);
 	    	            		  text = text1;
                         		  i = i + 8 + text0.length();

                                  w = current.getWidth(g);
                                  if(!current.isEmpty()) {
                                       current = current.copyState();
                                       list.addElement(current);
                                  }

                        		  current.s = new StringBuffer("@FORMULA");
                        		  current.x += w;
                    		  } else {
                                  w = current.getWidth(g);
                                  if(!current.isEmpty()) {
                                       current = current.copyState();
                                       list.addElement(current);
                                  }

                        		  current.s = new StringBuffer("");
                        		  current.x += w;
                    			  if (capital && (k-15) < greekCapital.length) {
                    				  current.s.append(greekCapital[k-15]);
                    			  } else {
                    				  current.s.append(greek[k-15]);
                    			  }

                                  w = current.getWidth(g);
                                  current = current.copyState();
                                  list.addElement(current);
                        		  current.s = new StringBuffer("");
                        		  current.x += w;
                    		  }
                    	  }
                    	  if (k == 3) {
                    		  //boolean isN = true;
                    		  boolean plus = false, minus = false;
                    		  if (text.substring(i, i+1).equals("+")) plus = true;
                    		  if (text.substring(i, i+1).equals("-")
                    				  || text.substring(i, i+1).equals("_")) minus = true;
                    		  int size = 0;
                			  if (plus || minus) {
                				  i++;
                        		  size = Integer.parseInt(text.substring(i, i+1).trim());
                        		  i++;
                			  } else {
                        		  size = Integer.parseInt(text.substring(i, i+2).trim());
                        		  i += 2;
                			  }
                			  /*
                    		  int iold = i;
                    		  i--;
                    		  do {
                    			  i++;
                    			  if (i == text.length()) { // >= and i-- before
                    				  isN = false;
                    				  //i--;
                    			  } else {
	                    			  int jj = num.indexOf(text.substring(i, i+1));
	                    			  if (jj < 0) isN = false;
                    			  }
                    		  } while (isN);
                    		  int size = Integer.parseInt(text.substring(iold, i).trim());
                    		  */
                    		  if (plus) size = current.f.getSize() + size;
                    		  if (minus) size = current.f.getSize() - size;
                    		  current.f = new Font(current.f.getName(), current.f.getStyle(), size);
                    	  }
                    	  if (k == 4)
                    		  current.f = new Font(current.f.getName(), Font.BOLD, current.f.getSize());
                    	  if (k == 5)
                    		  current.f = new Font(current.f.getName(), Font.ITALIC, current.f.getSize());
                    	  if (k == 6)
                    		  current.f = new Font(current.f.getName(), Font.PLAIN, current.f.getSize());
                    	  if (k > 2 && k < 7) state.push(current);
                    	  i--;
                      }
                      break;
/*
**                    Push the current state onto the state stack
**                    and start a new storage string
*/
	     case '{':
                      w = current.getWidth(g);
                      if(!current.isEmpty()) {
                           current = current.copyState();
                           current.f = new Font(this.fontname, current.f.getStyle(), current.f.getSize());
                           list.addElement(current);
                      }

                      state.push(current);
                      current.x += w;
                      break;
/*
**                    Pop the state off the state stack and set the current
**                    state to the top of the state stack
*/
	     case '}':
                      w = current.x + current.getWidth(g);
                      if (!state.isEmpty()) {
                          state.pop();
                          if (!state.isEmpty()) {
                        	  current = state.peek().copyState();
                          }
                      }
                      if (listCopy.size() > 0) {
                    	  current = listCopy.get(listCopy.size()-1);
                    	  listCopy.remove(current);
                      }
                      current.f = new Font(this.fontname, current.f.getStyle(), current.f.getSize());
                      if (listCopy.size() == 0) listCopy.add(current.copyState());
                      list.addElement(current);
                      current.x = w;
                      break;
	     case '^':
                      w = current.getWidth(g);
                      state.push(current.copyState());
                      if(!current.isEmpty()) {
                           current = current.copyState();
                           current.f = new Font(this.fontname, current.f.getStyle(), current.f.getSize());
                           list.addElement(current);
                      }
                      listCopy.add(current.copyState());
                      current.f = getScriptFont(current.f);
                      current.x += w;
                      current.y -= (int)((double)(current.getAscent(g))*sup_offset+0.5);
                      break;
 	     case '_':
                      w = current.getWidth(g);
                      state.push(current.copyState());
                      if(!current.isEmpty()) {
                           current = current.copyState();
                           current.f = new Font(this.fontname, current.f.getStyle(), current.f.getSize());
                           list.addElement(current);
                      }
                      listCopy.add(current.copyState());
                      current.f = getScriptFont(current.f);
                      current.x += w;
                      current.y += (int)((double)(current.getDescent(g))*sub_offset+0.5);
                      break;

             default:
                      current.s.append(ch);
                      break;
	     }
	   }

         for(int i=0; i<list.size(); i++) {
            current = list.elementAt(i);
            width  += current.getWidth(g);

            if( !current.isEmpty() ) {
               ascent  = Math.max(ascent, Math.abs(current.y) + current.getAscent(g));
               descent = Math.max(descent, Math.abs(current.y) + current.getDescent(g));
               leading  = Math.max(leading, current.getLeading(g));

               maxDescent = Math.max(maxDescent, Math.abs(current.y) + current.getMaxDescent(g));
               maxAscent  = Math.max(maxAscent, Math.abs(current.y) + current.getMaxAscent(g));
            }
         }

         height = ascent+descent+leading;
     }

     /**
      * Returns if the text is null.
      * @return true if the text has never been set or is null.
      */
     public boolean isNull() {
    	 return text == null;
     }
	  /**
	   * Parse the text then draws it.
	   * @param g Graphics context.
	   * @param x pixel position of the text.
	   * @param y pixel position of the text.
	   * @param j justification of the text.
	   */
     public void draw(Graphics g, int x, int y, ALIGN j) {
         justification = j;

         if (g == null) return;

         drawString(g, x, y);
       }

     /**
      * Forces the parse process to be ignored for a faster rendering.
      */
     public void avoidParsingTextAgain() {
    	 parse = false;
     }

     /**
      * Returns a simplified version of the string.
      * @return The string.
      */
     public String getSimplifiedString()
     {
    	 String originalText = text;
    	 text = DataSet.replaceAll(text, "@LATEX{\\rightarrow}", ".>", true);
         parseText((new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)).createGraphics(), true);

         String t = "";
         for(int i=0; i<list.size(); i++) {
        	 TextState ts = list.elementAt(i);
        	 if (ts.s != null) {
        		 for (int j=0; j<ts.s.length(); j++)
        		 {
        			 t += ts.s.charAt(j);
        		 }
        	 }
         }
         text = originalText;
         return t;
     }

     /**
      * Returns a simplified version of the string.
      * @param lab The label.
      * @return The simplified label.
      */
     public static String getSimplifiedString(String lab)
     {
    	 String label = DataSet.replaceAll(lab, "@LATEX{\\rightarrow}", "-", true);
    	 TextLabel tl = new TextLabel(label);
         tl.parseText((new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)).createGraphics(), true);

         String t = "";
         for(int i=0; i<tl.list.size(); i++) {
        	 TextState ts = tl.list.elementAt(i);
        	 if (ts.s != null) {
        		 for (int j=0; j<ts.s.length(); j++)
        		 {
        			 t += ts.s.charAt(j);
        		 }
        	 }
         }
    	 t = DataSet.replaceAll(t, "@FORMULA", "*", true);
    	 t = DataSet.replaceAll(t, "@CLOCK", "*", true);
         return t;
     }

     /**
      * Parse the text then draws it without any rotation.
      * @param lg Graphics context.
      * @param xoffset Pixel position of the text.
      * @param yoffset Pixel position of the text.
      */
     public void draw(Graphics lg, int xoffset, int yoffset) {
    	 drawString(lg, xoffset, yoffset);
     }

     /**
      * Parse the text then draw it without any rotation.
      * @param lg Graphics context.
      * @param xoffset Pixel position of the text.
      * @param yoffset Pixel position of the text.
      * @return The last font used for rendering.
      */
     public Font drawAndReturnLastFont(Graphics lg, int xoffset, int yoffset) {
    	 return drawString(lg, xoffset, yoffset);
     }

     private Font drawString(Graphics lg, int xoffset, int yoffset) {
         TextState ts;

         if(lg == null || text == null) return null;

         if (parse) parseText(lg.create(), false);

         if(justification == ALIGN.CENTER ) {
               xoffset -= width/2;
         } else {
        	 if(justification == ALIGN.RIGHT ) xoffset -= width;
        	 if(justification == ALIGN.LEFT_PLUS) xoffset += width/2;
         }

        if(background != null) {
        	 Color col = lg.getColor();
        	 lg.setColor(background);
        	 lg.fillRect(xoffset,yoffset-ascent,width,height);
        	 lg.setColor(col);
         }

         if(font  != null) lg.setFont(new Font(font.getFontName(), font.getStyle(), font.getSize()));
         if(color != null) lg.setColor(color);

         int index = 0, cindex = 0;
         boolean replaceX = false;
         int rx = 0;
         Graphics g = lg.create();
         Font out = lg.getFont();

         // In case the rendering is done against a PDF graphics we will change font to Symbol
         // for Greek characters instead of using images, so that they will be in vector graphics.
         String graphics = lg.getClass().getName();
         boolean toPdf = false;
         if (!graphics.equals("sun.java2d.SunGraphics2D") && graphics.toLowerCase().contains("pdfgraphics2d")) {
        	 toPdf = true;
         }

         for(int i=0; i<list.size(); i++) {
              ts = list.elementAt(i);
              int x = ts.x + xoffset;
              if (replaceX) x = rx;

              if (ts.f != null) {
            	  g.setFont(ts.f);
            	  out = ts.f;
              }
              if (ts.col != null) g.setColor(ts.col);
              if (ts.s != null)  {
            	  String t = ts.toString();
                  if (g.getFont().getSize() < 3) {
                	  Font f = new Font(g.getFont().getName(), g.getFont().getStyle(), 3);
                	  g.setFont(f);
                  }

            	  int tf = t.indexOf("@FORMULA");
            	  if (tf >= 0 && formulas.size() > index) {
            		  do {
	            		  LATEXFormula lf = formulas.get(index);
	            		  lf.getFormula().setColor(g.getColor());
	            		  if (tf > 0) g.drawString(t.substring(0, tf),ts.x+xoffset,ts.y+yoffset);
	            		  xoffset += g.getFontMetrics().stringWidth(t.substring(0, tf));

	            		  if (toPdf) {
	            			  int a1 = DataSet.getIndex(greekLATEX, lf.getCode());
	            			  int a2 = DataSet.getIndex(greekCapitalLATEX, lf.getCode());
	            			  if (a1 >= 0 || a2 >= 0) {
	                 			  String tt = t;
	            				  if (a1 >= 0) tt = ""+greekPDF[a1];
	            				  if (a2 >= 0) tt = ""+greekCapitalPDF[a2];
		                 		  try {
		                              if (ts.angle != 0) {
		                            	  ((Graphics2D) g).rotate(ts.angle, x+width/2, ts.y+yoffset);
		                            	  xoffset -= width * 0.5 * Math.sin(ts.angle);
		                            	  x = ts.x + xoffset;
		                              }
		                    		  Font ff = g.getFont();
		                    		  g.setFont(new Font(Font.SERIF, ff.getStyle(), Math.max(1, ff.getSize())));
		    	            		  g.drawString(tt,x,ts.y+yoffset);
		    	            		  xoffset += g.getFontMetrics().stringWidth(tt);
		    	            		  g.setFont(ff);
		                		  } catch (Exception exc) {}

			            		  index ++;
			            		  t = t.substring(tf+8);
			                	  tf = t.indexOf("@FORMULA");
			                	  continue;
	            			  }
	            		  }
	            		  BufferedImage img = lf.getAsImage();

	            		  if (img.getWidth() < 20) {
	            			  int w = img.getWidth();
	            			  lf = new LATEXFormula(lf.getCode(), 40);
		            		  lf.getFormula().setColor(g.getColor());
	            			  Picture pic = new Picture(lf.getAsImage());
	            			  int newH = (int) (w * (double) pic.getHeight() / (double) pic.getWidth());
	            			  if (pic.getImage().getWidth() > w && newH > 1) {
	            				  pic.getScaledInstance(w, 0, true);
		            			  img = pic.getImage();
	            			  }
	            		  }

            			  int py = ts.y+yoffset-img.getHeight()/2-g.getFontMetrics().getHeight()/4;
            			  if (py < 0) py = 0;
	            		  if (background2 != null) {
	            			  g.drawImage(Picture.removeAlpha(img, background2), ts.x+xoffset,py, null);
	            		  } else {
	            			  g.drawImage(img, ts.x+xoffset,py, null);
	            		  }
	            		  xoffset += img.getWidth();
	            		  index ++;
	            		  t = t.substring(tf+8);
	                	  tf = t.indexOf("@FORMULA");
            		  } while (tf >= 0);
            		  if (t.length() > 0) g.drawString(t,ts.x+xoffset,ts.y+yoffset);
            		  rx = ts.x + xoffset + g.getFontMetrics().stringWidth(t);
            		  replaceX = true;
            	  } else {
                	  tf = t.indexOf("@CLOCK");
                	  if (tf >= 0 && dclock.size() > cindex) {
                		  DigitalClock dc = dclock.get(cindex);
                		  String ra = dc.getString();
                		  int th = dc.getImageHeight() - dc.getBorder();
                			for (int ii=0; ii<ra.length(); ii++) {
                				try {
	                				BufferedImage image = dc.getDigitImage(ra.substring(ii, ii + 1));
	                				g.drawImage(image, x, ts.y+yoffset-th, null);
	                				rx += image.getWidth();
	                				x += image.getWidth();
                				} catch (Exception exc) {}
                			}
                			replaceX = true;
                			cindex ++;
                	  } else {
	            		  try {
	                		  Font ff = g.getFont();
		            		  if (toPdf && t.length() == 1) {
		            			  int a1 = DataSet.getIndex(greek, t);
		            			  int a2 = DataSet.getIndex(greekCapital, t);
		            			  if ((a1 >= 0 && a1 < greekCapital.length) || (a2 >= 0 && a2 < greekCapital.length)) {
		            				  if (a1 >= 0) t = ""+greekPDF[a1];
		            				  if (a2 >= 0) t = ""+greekCapitalPDF[a2];
		            				  g.setFont(new Font(Font.SERIF, ff.getStyle(), Math.max(1, ff.getSize())));
		            			  }
		            		  } else {
		                		  if (ff.getSize() < 1) g.setFont(new Font(ff.getName(), ff.getStyle(), 1));
		            		  }

	                          if (ts.angle != 0) {
	                        	  ((Graphics2D) g).rotate(ts.angle, x+width/2, ts.y+yoffset);
	                        	  xoffset -= width * 0.5 * Math.sin(ts.angle);
	                        	  x = ts.x + xoffset;
	                          }
		            		  g.drawString(t,x,ts.y+yoffset);
		            		  rx += g.getFontMetrics().stringWidth(t);
	            		  } catch (Exception exc) {}
                	  }
            	  }
              }
         }

         lg.setColor(g.getColor());
         return out;
       }

     /**
      * Returns the font name.
      * @return Logical font name of the set font.
      */
      public String getFontName()  { return fontname; }
     /**
      * Returns the font style.
      * @return Style of the set font.
      */
      public int getFontStyle() { return fontstyle; }
     /**
      * Returns the font size.
      * @return Size of the set font.
      */
      public int getFontSize()  { return fontsize; }
     /**
      * Set the Logical font name of the current font
      * @param s Logical font name.
      */
      public void setFontName(String s) {  fontname  = s; rebuildFont(); }
     /**
      * Set the Font style of the current font.
      * @param i Font style.
      */
      public void setFontStyle(int i)   {  fontstyle = i; rebuildFont(); }
     /**
      * Set the Font size of the current font.
      * @param i Font size.
      */
      public void setFontSize(int i)    {  fontsize  = i; rebuildFont(); }

     /*
     ** Rebuild the font using the current fontname, fontstyle, and fontsize.
     */
     private void rebuildFont()
     {
        parse = true;

        if( fontsize <= 0 || fontname == null) {
        	font = null;
   	    } else {
   	    	font = new Font(fontname, fontstyle, fontsize);
   	    }
     }

     /**
      * Returns the script font version.
      * @param f Font.
      * @return The script font version of the parsed font using the
      *        {@linkplain TextLabel#script_fraction} variable.
      */
       public Font getScriptFont(Font f) {
            if(f == null) return f;

           int size = f.getSize();

            if(size <= MINIMUM_SIZE) return f;

            size = (int)((double)(f.getSize())*script_fraction + 0.5);

            if(size <= MINIMUM_SIZE) return f;

            return new Font(f.getName(), f.getStyle(), size);
   	   }
	}

/**
 * A structure class used exclusively with the TextLabel class.
 * When the Text changes state (new font, new color, new offset)
 * then this class holds the information plus the substring
 * that the state pertains to.
 * @author  Leigh Brookshaw
 */
class TextState extends Object {
	  // The font.
      Font f         = null;
      // The string buffer.
      StringBuffer s = null;
      // X position of the text.
      int x          = 0;
      // Y position of the text.
      int y          = 0;
      // Text color.
      Color col      = null;
      // Rotation angle
      double angle = 0;

      /**
       * Constructor.
       */
      public TextState() {
              s = new StringBuffer();
	    }
      /**
       * Returns a copy of this instance.
       * @return A copy (font, x, y, text).
       */
      public TextState copyAll() {
             TextState tmp = copyState();
             if(s.length()==0) return tmp;
             for(int i=0; i<s.length(); i++) { tmp.s.append(s.charAt(i)); }
             tmp.col = new Color(col.getRed(), col.getGreen(), col.getBlue(), col.getAlpha());
             return tmp;
	   }

      /**
       * Returns a copy of the state of this instance.
       * @return A copy of the state (font, x, y).
       */
      public TextState copyState() {
             TextState tmp = new TextState();
             tmp.f = new Font(f.getName(), f.getStyle(), f.getSize());
             tmp.x = x;
             tmp.y = y;
             tmp.angle = 0;
             return tmp;
	   }

      /**
       * Returns the text.
       */
      @Override
      public String toString() {
             return s.toString();
	   }

      /**
       * True if there is no text.
       * @return True or false.
       */
      public boolean isEmpty() {
           return (s.length() == 0);
	 }

      /**
       * Text color.
       * @return The color.
       */
      public Color getColor()
      {
    	  return this.col;
      }

      /**
       * Text rotation.
       * @return The angle in radians.
       */
      public double getAngle()
      {
    	  return this.angle;
      }

      /**
       * Text width.
       * @param g Graphics context.
       * @return The width.
       */
      public int getWidth(Graphics g) {

          if (this.f != null) g.setFont(this.f);
          if (this.col != null) g.setColor(this.col);

          if (s.length()==0) return 0;

          String ss = s.toString();
          if (ss.startsWith("@FORMULA")) ss = s.substring(8);
          if (ss.startsWith("@CLOCK")) ss = s.substring(6);
          return g.getFontMetrics().stringWidth(ss);
      }

      /**
       * Text height.
       * @param g Graphics context.
       * @return The height.
       */
      public int getHeight(Graphics g) {
           if (g == null || f == null ) return 0;

           return g.getFontMetrics().getHeight();
	 }
      /**
       * Text ascent.
       * @param g Graphics context.
       * @return The ascent.
       */
      public int getAscent(Graphics g) {
           if(g == null || f == null ) return 0;

            return g.getFontMetrics().getAscent();
	 }
      /**
       * Text descent.
       * @param g Graphics context.
       * @return The descent.
       */
      public int getDescent(Graphics g) {
           if(g == null || f == null ) return 0;

            return g.getFontMetrics().getDescent();
      }
      /**
       * Text maximum ascent.
       * @param g Graphics context.
       * @return The maximum ascent.
       */
      public int getMaxAscent(Graphics g) {
           if(g == null || f == null ) return 0;

            return g.getFontMetrics().getMaxAscent();
      }
      /**
       * Text maximum descent.
       * @param g Graphics context.
       * @return The maximum descent.
       */
      public int getMaxDescent(Graphics g) {
           if(g == null || f == null ) return 0;

            return g.getFontMetrics().getMaxDescent();
      }
      /**
       * Text leading.
       * @param g Graphics context.
       * @return The leading.
       */
      public int getLeading(Graphics g) {
           if(g == null || f == null ) return 0;

            return g.getFontMetrics().getLeading();
      }
}

class DigitalClock {

	/** The different sections of a digit. */
	public enum DIGIT_SECTION {
		/** The different sections of a digit. */
		H_TOP, H_MEDIUM, H_BOTTOM, V_TOP_LEFT, V_TOP_RIGHT, V_BOTTOM_LEFT, V_BOTTOM_RIGHT
	}

	/** The set of supported digits. */
	public enum DIGIT {
		NUMBER_0 ("1011111"),
		NUMBER_1 ("0000101"),
		NUMBER_2 ("1110110"),
		NUMBER_3 ("1110101"),
		NUMBER_4 ("0101101"),
		NUMBER_5 ("1111001"),
		NUMBER_6 ("1111011"),
		NUMBER_7 ("1000101"),
		NUMBER_8 ("1111111"),
		NUMBER_9 ("1111101"),
		CHARACTER_d ("0110111"),
		CHARACTER_h ("0101011"),
		CHARACTER_m ("1111010"), // should be rotated later
		CHARACTER_s ("1111001"),
		CHARACTER_o ("1101100"),
		CHARACTER_I ("0000100"),
		CHARACTER_II ("0001100"),
		CHARACTER_L ("0011010"),
		CHARACTER_U ("0011111"),
		CHARACTER_T ("0101010"), // should be rotated later
		CHARACTER_t ("0101010"), // should be rotated later
		CHARACTER_D ("1011111"),
		CHARACTER_B ("1111111"),
		CHARACTER_b ("0111011"),
		CHARACTER_C ("1011010"),
		CHARACTER_BAR ("0100000"),
		CHARACTER_BAR_BOTTOM ("0010000"),
		CHARACTER_BLANK("0000000"),
		CHARACTER_BLANK_SPACE (null), // unsupported here
		CHARACTER_BLANK_SPACE_HALF (null), // unsupported here
		CHARACTER_TWO_POINTS (null), // unsupported here
		CHARACTER_ONE_POINT (null); // unsupported here

		private String matrix;
		private DIGIT(String matrix) {
			this.matrix = matrix;
		}

		/**
		 * Returns if this digit contains a given section.
		 * @param s The section.
		 * @return True or false.
		 */
		public boolean hasSection(DIGIT_SECTION s) {
			int i = s.ordinal();
			return matrix.substring(i, i+1).equals("1");
		}
	}

	private int w, h, b;
	private Color in, out;
	private boolean blur, rotate;
	private String s;

	/**
	 * Constructor for a digital clock. Adequate values
	 * for w, h, and b are, respectively, 50, 11, and 2.
	 * @param w The width of each section.
	 * @param h The height of each section.
	 * @param b A border value for each section.
	 * @param in The 'in' color, yellow for instance.
	 * @param out The 'out' color, gray for instance.
	 * @param blur True to slightly blur the output image.
	 * @param rotate True to slightly rotate the digits for an italic-like effect.
	 */
	public DigitalClock(int w, int h, int b,
			Color in, Color out, boolean blur, boolean rotate) {
		this.w = w;
		this.h = h;
		this.b = b;

		this.in = in;
		this.out = out;
		this.blur = blur;
		this.rotate = rotate;
	}

	/**
	 * Returns the width of a clock's section.
	 * @return The width.
	 */
	public int getWidth () { return w; }
	/**
	 * Returns the height of a clock's section.
	 * @return The height.
	 */
	public int getHeight () { return h; }
	/**
	 * Returns the border of a clock's section.
	 * @return The border.
	 */
	public int getBorder () { return b; }

	/**
	 * Returns the typical width of the image representing a digit.
	 * @return The width.
	 */
	public int getImageWidth() {
		int sep = w / 5;
		return this.w+2*this.b+2+sep;
	}

	/**
	 * Returns the typical height of the image representing a digit.
	 * @return The height.
	 */
	public int getImageHeight() {
		return this.w*2-this.b+this.h+1;
	}

	/**
	 * Sets the String to be rendered (just to save it).
	 * @param s The string.
	 */
	public void setString(String s) {
		this.s = s;
	}

	/**
	 * Returns the string to be rendered.
	 * @return The string.
	 */
	public String getString() {
		return s;
	}
	/**
	 * Returns a polygon with a given section of a digit in a clock.
	 * @param s The section to return.
	 * @return The section to be drawn.
	 */
	private Polygon getDigitSection(DIGIT_SECTION s) {
		switch (s) {
		case H_TOP:
			return new Polygon(
					new int[] {b, w + b, w + b - h, h + b, b},
					new int[] {0, 0, h, h, 0},
					5
					);
		case H_MEDIUM:
			if (h > 4 * b) {
				return new Polygon(
					new int[] {h + b, w + b - h, w + 2*b, w + 2*b, w + b - h, h + b, 0, 0, h + b},
					new int[] {w-1, w-1, w+2*b+1, w+h-2*b-2, w+h-1, w+h-1, w+h-2*b-2, w+2*b+1, w-1},
					9
					);
			} else {
				return new Polygon(
						new int[] {h + b, w + b - h, w, w + b - h, h + b, 2*b, h + b},
						new int[] {w-1, w-1, w-1+h/2, w+h-1, w+h-1, w-1+h/2, w-1},
						7
						);
			}
		case H_BOTTOM:
			return new Polygon(
					new int[] {h + b, w + b - h, w + b, b, h + b},
					new int[] {w*2-b-1, w*2-b-1, w*2-1-b+h, w*2-1-b+h, w*2-1-b},
					5
					);
		case V_TOP_LEFT:
			return new Polygon(
					new int[] {0, h, h, 0, 0},
					new int[] {b, h + b, w-1-b, w + b, b},
					5
					);
		case V_TOP_RIGHT:
			return new Polygon(
					new int[] {w - h + 2*b, w + 2*b, w + 2*b, w-h+2*b, w - h + 2*b},
					new int[] {h+b, b, w+b, w-1-b, h+b},
					5
					);
		case V_BOTTOM_LEFT:
			return new Polygon(
					new int[] {0, h, h, 0, 0},
					new int[] {w+h-2*b, w+h+1, (w-b)*2-1, (w-b)*2+h-1, w+h-2*b},
					5
					);
		case V_BOTTOM_RIGHT:
			return new Polygon(
					new int[] {w-h+2*b, w+2*b, w+2*b, w-h+2*b, w-h+2*b},
					new int[] {w+h+1, w+h-2*b, (w-b)*2+h-1, (w-b)*2-1, 2+h+1},
					5
					);
		}
		return null;
	}

	/**
	 * Returns an image with the digit.
	 * @param s The digit, string of length 1.
	 * @return The image.
	 * @throws JPARSECException If an error occurs.
	 */
	public BufferedImage getDigitImage(String s) throws JPARSECException {
		if (s.length() != 1) throw new JPARSECException("String must be of length 1");

		if (s.equals("0") || s.equals("1") || s.equals("2") || s.equals("3") ||
				s.equals("4") || s.equals("5") || s.equals("6") || s.equals("7") ||
				s.equals("8") || s.equals("9")) {
			int n = Integer.parseInt(s);
			return getDigitImage(DIGIT.values()[n]);
		} else {
			if (s.equals("L")) return getDigitImage(DIGIT.CHARACTER_L);
			if (s.equals("U")) return getDigitImage(DIGIT.CHARACTER_U);
			if (s.equals("T")) return getDigitImage(DIGIT.CHARACTER_T);
			if (s.equals("t")) return getDigitImage(DIGIT.CHARACTER_t);
			if (s.equals("C")) return getDigitImage(DIGIT.CHARACTER_C);
			if (s.equals("B")) return getDigitImage(DIGIT.CHARACTER_B);
			if (s.equals("b")) return getDigitImage(DIGIT.CHARACTER_b);
			if (s.equals("D")) return getDigitImage(DIGIT.CHARACTER_D);
			if (s.equals("d")) return getDigitImage(DIGIT.CHARACTER_d);
			if (s.equals("h")) return getDigitImage(DIGIT.CHARACTER_h);
			if (s.equals("m")) return getDigitImage(DIGIT.CHARACTER_m);
			if (s.equals("s")) return getDigitImage(DIGIT.CHARACTER_s);
			if (s.equals("\u00b0")) return getDigitImage(DIGIT.CHARACTER_o);
			if (s.equals("'")) return getDigitImage(DIGIT.CHARACTER_I);
			if (s.equals("-")) return getDigitImage(DIGIT.CHARACTER_BAR);
			if (s.equals("_")) return getDigitImage(DIGIT.CHARACTER_BAR_BOTTOM);
			if (s.equals("\"")) return getDigitImage(DIGIT.CHARACTER_II);
			if (s.equals(":")) return getDigitImage(DIGIT.CHARACTER_TWO_POINTS);
			if (s.equals("+")) return getDigitImage(DIGIT.CHARACTER_BLANK_SPACE_HALF);
			if (s.equals(".")) return getDigitImage(DIGIT.CHARACTER_ONE_POINT);
			if (s.equals(" ")) return getDigitImage(DIGIT.CHARACTER_BLANK_SPACE);
		}
		throw new JPARSECException("Unsupported digit "+s+"!");
	}

	/**
	 * Returns an image with the digit.
	 * @param d The digit.
	 * @return The image.
	 * @throws JPARSECException If an error occurs.
	 */
	public BufferedImage getDigitImage(DIGIT d) throws JPARSECException {
		int w = this.getImageWidth(), h = this.getImageHeight();
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		//g.setColor(new Color(0, 0, 0, 0));
		//g.fillRect(0, 0, w, h);
		g.setColor(new Color(in.getRGB(), true));

		if (d != DIGIT.CHARACTER_BLANK_SPACE && d != DIGIT.CHARACTER_BLANK_SPACE_HALF) {
			if (d == DIGIT.CHARACTER_TWO_POINTS || d == DIGIT.CHARACTER_ONE_POINT) {
				int r = 2*b, r2 = 2*r+1;
				if (d == DIGIT.CHARACTER_TWO_POINTS) {
					int sepY = Math.min(h/4, 8*b) - b;
					g.fillOval(w/2-b-r, h/2-sepY-r, r2, r2);
					g.fillOval(w/2-b-r, h/2+sepY-r, r2, r2);
				} else {
					g.fillOval(w/2-b-r, h-r2, r2, r2);
				}
			} else {
				DIGIT_SECTION s[] = DIGIT_SECTION.values();
				for (int i = 0; i < s.length; i++) {
					Polygon p = this.getDigitSection(s[i]);
					p.translate(1, 1);
					if (d.hasSection(s[i])) {
						g.setColor(new Color(in.getRGB(), true));
					} else {
						g.setColor(new Color(out.getRGB(), true));
					}
					g.fill(p);
				}
			}
		}
		Picture pic = null;
		if (rotate) {
			pic = new Picture(image);
			pic.rotate(6 * Constant.DEG_TO_RAD, pic.getWidth()/2, pic.getHeight()/2);
	 		image = pic.getImage();
		}

		if (d.ordinal() > 9 && d != DIGIT.CHARACTER_TWO_POINTS && d != DIGIT.CHARACTER_BAR &&
				d != DIGIT.CHARACTER_BLANK_SPACE && d != DIGIT.CHARACTER_ONE_POINT) {
			if (pic == null) pic = new Picture(image);
			if (d == DIGIT.CHARACTER_s || d == DIGIT.CHARACTER_d || d == DIGIT.CHARACTER_L
					|| d == DIGIT.CHARACTER_U || d == DIGIT.CHARACTER_B || d == DIGIT.CHARACTER_b
					|| d == DIGIT.CHARACTER_D || d == DIGIT.CHARACTER_C || d == DIGIT.CHARACTER_BLANK_SPACE_HALF) {
				pic.getScaledInstance(w/4, h/4, true);
			} else {
				pic.getScaledInstance(w/2, h/2, true);
				if (d == DIGIT.CHARACTER_m || d == DIGIT.CHARACTER_T || d == DIGIT.CHARACTER_t) {
					pic.rotate(Constant.PI_OVER_TWO, w/4, h/4+this.h);
					if (d != DIGIT.CHARACTER_t) {
						pic.getScaledInstance(w/4+this.h, h/2+this.b, false);
					} else {
						pic.getScaledInstance(w/4, h/2+this.b, false);
					}
					if (blur) pic.convolve(Picture.PATTERN_SHARPENING);
					pic.move(0, -h/4-b);
				}
			}
			pic.resize(pic.getWidth(), pic.getHeight() + b);
			pic.move(0, this.b);
	 		image = pic.getImage();
		}

		if (blur) {
			if (pic == null) pic = new Picture(image);
	 		if (d.ordinal() <= 9) {
				double blur = 1.0/9.0;
				pic.convolve(DataSet.getSetOfValues(blur, blur, 9, false));
			} else {
				double blur = 1.0/6.0;
				double pattern[] = new double[] {
						0, blur, 0,
						blur, 1.0-blur*4, blur,
						0, blur, 0
				};
				pic.convolve(pattern);
			}
	 		image = pic.getImage();
		}

		return image;
	}
}
