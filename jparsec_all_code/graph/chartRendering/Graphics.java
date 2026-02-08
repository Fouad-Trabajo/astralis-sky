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

import jparsec.graph.JPARSECStroke;
import jparsec.graph.chartRendering.SkyRenderElement.COLOR_MODE;

/**
 * The main Graphics interface to provide rendering support in different platforms.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public interface Graphics {

	/**
	 * The set of color modes for the anaglyph method.
	 * There are two simple anaglyph modes (green-red and red-cyan glasses) to obtain
	 * an adequate anaglyph effect without losing too much performance in sky rendering.
	 * In case you select one of these you should also select the
	 * {@linkplain COLOR_MODE#WHITE_BACKGROUND_SIMPLE_GREEN_RED_OR_RED_CYAN_ANAGLYPH}
	 * color mode. For other anaglyph methods the set of colors can be freely adjusted,
	 * and can be applied to sky, planetary, and satellite rendering.
	 */
	enum ANAGLYPH_COLOR_MODE {
		/** ID value for no anaglyph mode. */
		NO_ANAGLYPH,
		/** Simple green-red anaglyph: green for left eye, red for right. Only for
		 * sky rendering. */
		GREEN_RED,
		/** Simple red-cyan anaglyph: red for left eye, cyan for right. Only for sky
		 * rendering. */
		RED_CYAN,
		/** Dubois anaglyph mode, red-cyan anaglyph. */
		DUBOIS_RED_CYAN,
		/** Dubois anaglyph mode, green-magenta anaglyph. */
		DUBOIS_GREEN_MAGENTA,
		/** Dubois anaglyph mode, amber-blue anaglyph. */
		DUBOIS_AMBER_BLUE,
		/** Returns a true 3d image with left/view mode. */
		TRUE_3D_MODE_LEFT_RIGHT,
		/** Returns a true 3d image with left/view mode, and half width for each view. */
		TRUE_3D_MODE_LEFT_RIGHT_HALF_WIDTH;

		private float z0 = 100f;
		private float d = 0.5f;

		/**
		 * Returns true if the mode is set to a real 3d mode (Dubois or left/right view).
		 * @return True for any Dubois method and for the true left/right 3d mode, false
		 * otherwise.
		 */
		public boolean isReal3D() {
			if (this == DUBOIS_AMBER_BLUE || this == DUBOIS_GREEN_MAGENTA || this == DUBOIS_RED_CYAN ||
					this == TRUE_3D_MODE_LEFT_RIGHT || this == TRUE_3D_MODE_LEFT_RIGHT_HALF_WIDTH) return true;
			return false;
		}
		/**
		 * Returns true if the mode is a 3d one.
		 * @return True in case the mode is not set to {@linkplain #NO_ANAGLYPH}.
		 */
		public boolean is3D() {
			if (this == NO_ANAGLYPH) return false;
			return true;
		}
		/**
		 * Returns true if the mode is set to any of the Dubois methods.
		 * @return True for any Dubois method, false otherwise.
		 */
		public boolean isDubois() {
			if (this == DUBOIS_AMBER_BLUE || this == DUBOIS_GREEN_MAGENTA || this == DUBOIS_RED_CYAN) return true;
			return false;
		}

		/**
		 * Returns the reference z position, 100 by default.
		 * Levels visible in depth will be from 0 to 2*z. This means that
		 * the z position of the points/lines to draw should be within this
		 * interval.
		 * @return Reference z position.
		 */
		public float getReferenceZ() {
			return z0;
		}

		/**
		 * Returns the eye separation, (8 / reference z) for red-cyan and
		 * green-red simple anaglyph, and 0.5 by default
		 * for Dubois method. Dubois method is more powerful and allows
		 * a lot of range in distances.
		 * This means that since the points to draw are restricted
		 * to distances from 0 to 2*reference z, then
		 * the left/right images will be separated by +/- 8 (or +/- 50 pixels
		 * at most for Dubois method).
		 * @return Eye separation.
		 */
		public float getEyeSeparation() {
			if (this == GREEN_RED || this == RED_CYAN) return 8/z0;
			return d;
		}

		/**
		 * Sets the reference z position.
		 * Levels visible in depth will be from 0 to 2*z. This means that
		 * the z position of the points/lines to draw should be within this
		 * interval.
		 * @param z Reference distance.
		 */
		public void setReferenceZ(float z) {
			z0 = z;
		}

		/**
		 * Sets the eye separation, only for Dubois method. Should be between
		 * 0 and 5. Default value is 0.5.
		 * @param d Eye separation. In case input value is
		 * lower than 0 it will be set to zero, in case is greater than 5.0 it will
		 * be set to 5.
		 */
		public void setEyeSeparation(float d) {
			if (d < 0.0f) d = 0.0f;
			if (d > 5.0f) d = 5.0f;
			this.d = d;
		}

		/**
		 * Resets reference z and eye separation values to their
		 * default values.
		 */
		public void clear() {
			z0 = 100f;
			d = 0.5f;
		}
	}

	  /**
	   *  Dubois anaglyph matrix, green-magenta for left eye, see http://www.flickr.com/photos/e_dubois/5132528166/
	   */
	  public static final float[] duboisGM_left = new float[] {
	              -0.062f,  0.284f, -0.015f,
	              -0.158f,  0.668f, -0.027f,
	              -0.039f,  0.143f,  0.021f};
	  /**
	   *  Dubois anaglyph matrix, green-magenta for right eye, see http://www.flickr.com/photos/e_dubois/5132528166/
	   */
	  public static final float[] duboisGM_right = new float[] {
	               0.529f, -0.016f,  0.009f,
	               0.705f, -0.015f,  0.075f,
	               0.024f, -0.065f,  0.937f};
	  /**
	   *  Dubois anaglyph matrix, amber-blue for left eye, see http://www.flickr.com/photos/e_dubois/5230654930/
	   */
	  public static final float[] duboisAB_left = new float[] {
	               1.062f, -0.026f, -0.038f,
	              -0.205f,  0.908f, -0.173f,
	               0.299f,  0.068f,  0.022f};
	  /**
	   *  Dubois anaglyph matrix, amber-blue for right eye, see http://www.flickr.com/photos/e_dubois/5230654930/
	   */
	  public static final float[] duboisAB_right = new float[] {
	              -0.016f,  0.006f,  0.094f,
	              -0.123f,  0.062f,  0.185f,
	              -0.017f, -0.017f,  0.911f};
	  /**
	   * Dubois anaglyph matrix, red-cyan for left eye, see http://www.site.uottawa.ca/~edubois/anaglyph/LeastSquaresHowToPhotoshop.pdf
	   */
	  public static final float[] duboisRC_left = new float[] {
	               0.437f, -0.062f, -0.048f,
	               0.449f, -0.062f, -0.050f,
	               0.164f, -0.024f, -0.017f};
	  /**
	   * Dubois anaglyph matrix, red-cyan for right eye, see http://www.site.uottawa.ca/~edubois/anaglyph/LeastSquaresHowToPhotoshop.pdf
	   */
	  public static final float[] duboisRC_right = new float[] {
	              -0.011f,  0.377f, -0.026f,
	              -0.032f,  0.761f, -0.093f,
	              -0.007f,  0.009f,  1.234f};

	  static final String SANS_SERIF = "SansSerif", DIALOG = "Dialog", SYMBOL = "Symbol";
	  /** Font styles. */
	public static final int PLAIN = 0, BOLD = 1, ITALIC = 2;
	/**
	 * The set of supported fonts for rendering. There are 3:
	 * Dialog, SansSerif, and Symbol. Symbol is not directly
	 * supported by Java
	 */
	enum FONT {
		SANS_SERIF_PLAIN_5(SANS_SERIF, PLAIN, 5),
		SANS_SERIF_PLAIN_6(SANS_SERIF, PLAIN, 6),
		SANS_SERIF_PLAIN_7(SANS_SERIF, PLAIN, 7),
		SANS_SERIF_PLAIN_8(SANS_SERIF, PLAIN, 8),
		SANS_SERIF_PLAIN_9(SANS_SERIF, PLAIN, 9),
		SANS_SERIF_PLAIN_10(SANS_SERIF, PLAIN, 10),
		SANS_SERIF_PLAIN_11(SANS_SERIF, PLAIN, 11),
		SANS_SERIF_PLAIN_12(SANS_SERIF, PLAIN, 12),
		SANS_SERIF_PLAIN_13(SANS_SERIF, PLAIN, 13),
		SANS_SERIF_PLAIN_14(SANS_SERIF, PLAIN, 14),
		SANS_SERIF_PLAIN_15(SANS_SERIF, PLAIN, 15),
		SANS_SERIF_PLAIN_16(SANS_SERIF, PLAIN, 16),
		SANS_SERIF_PLAIN_17(SANS_SERIF, PLAIN, 17),
		SANS_SERIF_PLAIN_18(SANS_SERIF, PLAIN, 18),
		SANS_SERIF_PLAIN_19(SANS_SERIF, PLAIN, 19),
		SANS_SERIF_PLAIN_20(SANS_SERIF, PLAIN, 20),
		SANS_SERIF_PLAIN_21(SANS_SERIF, PLAIN, 21),
		SANS_SERIF_PLAIN_22(SANS_SERIF, PLAIN, 22),
		SANS_SERIF_PLAIN_23(SANS_SERIF, PLAIN, 23),
		SANS_SERIF_PLAIN_24(SANS_SERIF, PLAIN, 24),
		SANS_SERIF_PLAIN_25(SANS_SERIF, PLAIN, 25),
		SANS_SERIF_PLAIN_26(SANS_SERIF, PLAIN, 26),
		SANS_SERIF_PLAIN_27(SANS_SERIF, PLAIN, 27),
		SANS_SERIF_PLAIN_28(SANS_SERIF, PLAIN, 28),
		SANS_SERIF_PLAIN_29(SANS_SERIF, PLAIN, 29),
		SANS_SERIF_PLAIN_30(SANS_SERIF, PLAIN, 30),
		SANS_SERIF_PLAIN_31(SANS_SERIF, PLAIN, 31),
		SANS_SERIF_PLAIN_32(SANS_SERIF, PLAIN, 32),
		SANS_SERIF_PLAIN_33(SANS_SERIF, PLAIN, 33),
		SANS_SERIF_PLAIN_34(SANS_SERIF, PLAIN, 34),
		SANS_SERIF_PLAIN_35(SANS_SERIF, PLAIN, 35),
		SANS_SERIF_PLAIN_36(SANS_SERIF, PLAIN, 36),
		SANS_SERIF_PLAIN_37(SANS_SERIF, PLAIN, 37),
		SANS_SERIF_PLAIN_38(SANS_SERIF, PLAIN, 38),
		SANS_SERIF_PLAIN_39(SANS_SERIF, PLAIN, 39),
		SANS_SERIF_PLAIN_40(SANS_SERIF, PLAIN, 40),
		SANS_SERIF_PLAIN_41(SANS_SERIF, PLAIN, 41),
		SANS_SERIF_PLAIN_42(SANS_SERIF, PLAIN, 42),
		SANS_SERIF_PLAIN_43(SANS_SERIF, PLAIN, 43),
		SANS_SERIF_PLAIN_44(SANS_SERIF, PLAIN, 44),
		SANS_SERIF_PLAIN_45(SANS_SERIF, PLAIN, 45),
		SANS_SERIF_PLAIN_46(SANS_SERIF, PLAIN, 46),
		SANS_SERIF_PLAIN_47(SANS_SERIF, PLAIN, 47),
		SANS_SERIF_PLAIN_48(SANS_SERIF, PLAIN, 48),
		SANS_SERIF_PLAIN_49(SANS_SERIF, PLAIN, 49),
		SANS_SERIF_PLAIN_50(SANS_SERIF, PLAIN, 50),
		SANS_SERIF_PLAIN_51(SANS_SERIF, PLAIN, 51),
		SANS_SERIF_PLAIN_52(SANS_SERIF, PLAIN, 52),
		SANS_SERIF_PLAIN_53(SANS_SERIF, PLAIN, 53),
		SANS_SERIF_PLAIN_54(SANS_SERIF, PLAIN, 54),
		SANS_SERIF_PLAIN_55(SANS_SERIF, PLAIN, 55),

		SANS_SERIF_BOLD_5(SANS_SERIF, BOLD, 5),
		SANS_SERIF_BOLD_6(SANS_SERIF, BOLD, 6),
		SANS_SERIF_BOLD_7(SANS_SERIF, BOLD, 7),
		SANS_SERIF_BOLD_8(SANS_SERIF, BOLD, 8),
		SANS_SERIF_BOLD_9(SANS_SERIF, BOLD, 9),
		SANS_SERIF_BOLD_10(SANS_SERIF, BOLD, 10),
		SANS_SERIF_BOLD_11(SANS_SERIF, BOLD, 11),
		SANS_SERIF_BOLD_12(SANS_SERIF, BOLD, 12),
		SANS_SERIF_BOLD_13(SANS_SERIF, BOLD, 13),
		SANS_SERIF_BOLD_14(SANS_SERIF, BOLD, 14),
		SANS_SERIF_BOLD_15(SANS_SERIF, BOLD, 15),
		SANS_SERIF_BOLD_16(SANS_SERIF, BOLD, 16),
		SANS_SERIF_BOLD_17(SANS_SERIF, BOLD, 17),
		SANS_SERIF_BOLD_18(SANS_SERIF, BOLD, 18),
		SANS_SERIF_BOLD_19(SANS_SERIF, BOLD, 19),
		SANS_SERIF_BOLD_20(SANS_SERIF, BOLD, 20),
		SANS_SERIF_BOLD_21(SANS_SERIF, BOLD, 21),
		SANS_SERIF_BOLD_22(SANS_SERIF, BOLD, 22),
		SANS_SERIF_BOLD_23(SANS_SERIF, BOLD, 23),
		SANS_SERIF_BOLD_24(SANS_SERIF, BOLD, 24),
		SANS_SERIF_BOLD_25(SANS_SERIF, BOLD, 25),
		SANS_SERIF_BOLD_26(SANS_SERIF, BOLD, 26),
		SANS_SERIF_BOLD_27(SANS_SERIF, BOLD, 27),
		SANS_SERIF_BOLD_28(SANS_SERIF, BOLD, 28),
		SANS_SERIF_BOLD_29(SANS_SERIF, BOLD, 29),
		SANS_SERIF_BOLD_30(SANS_SERIF, BOLD, 30),
		SANS_SERIF_BOLD_31(SANS_SERIF, BOLD, 31),
		SANS_SERIF_BOLD_32(SANS_SERIF, BOLD, 32),
		SANS_SERIF_BOLD_33(SANS_SERIF, BOLD, 33),
		SANS_SERIF_BOLD_34(SANS_SERIF, BOLD, 34),
		SANS_SERIF_BOLD_35(SANS_SERIF, BOLD, 35),
		SANS_SERIF_BOLD_36(SANS_SERIF, BOLD, 36),
		SANS_SERIF_BOLD_37(SANS_SERIF, BOLD, 37),
		SANS_SERIF_BOLD_38(SANS_SERIF, BOLD, 38),
		SANS_SERIF_BOLD_39(SANS_SERIF, BOLD, 39),
		SANS_SERIF_BOLD_40(SANS_SERIF, BOLD, 40),
		SANS_SERIF_BOLD_41(SANS_SERIF, BOLD, 41),
		SANS_SERIF_BOLD_42(SANS_SERIF, BOLD, 42),
		SANS_SERIF_BOLD_43(SANS_SERIF, BOLD, 43),
		SANS_SERIF_BOLD_44(SANS_SERIF, BOLD, 44),
		SANS_SERIF_BOLD_45(SANS_SERIF, BOLD, 45),
		SANS_SERIF_BOLD_46(SANS_SERIF, BOLD, 46),
		SANS_SERIF_BOLD_47(SANS_SERIF, BOLD, 47),
		SANS_SERIF_BOLD_48(SANS_SERIF, BOLD, 48),
		SANS_SERIF_BOLD_49(SANS_SERIF, BOLD, 49),
		SANS_SERIF_BOLD_50(SANS_SERIF, BOLD, 50),
		SANS_SERIF_BOLD_51(SANS_SERIF, BOLD, 51),
		SANS_SERIF_BOLD_52(SANS_SERIF, BOLD, 52),
		SANS_SERIF_BOLD_53(SANS_SERIF, BOLD, 53),
		SANS_SERIF_BOLD_54(SANS_SERIF, BOLD, 54),
		SANS_SERIF_BOLD_55(SANS_SERIF, BOLD, 55),

		SANS_SERIF_ITALIC_5(SANS_SERIF, ITALIC, 5),
		SANS_SERIF_ITALIC_6(SANS_SERIF, ITALIC, 6),
		SANS_SERIF_ITALIC_7(SANS_SERIF, ITALIC, 7),
		SANS_SERIF_ITALIC_8(SANS_SERIF, ITALIC, 8),
		SANS_SERIF_ITALIC_9(SANS_SERIF, ITALIC, 9),
		SANS_SERIF_ITALIC_10(SANS_SERIF, ITALIC, 10),
		SANS_SERIF_ITALIC_11(SANS_SERIF, ITALIC, 11),
		SANS_SERIF_ITALIC_12(SANS_SERIF, ITALIC, 12),
		SANS_SERIF_ITALIC_13(SANS_SERIF, ITALIC, 13),
		SANS_SERIF_ITALIC_14(SANS_SERIF, ITALIC, 14),
		SANS_SERIF_ITALIC_15(SANS_SERIF, ITALIC, 15),
		SANS_SERIF_ITALIC_16(SANS_SERIF, ITALIC, 16),
		SANS_SERIF_ITALIC_17(SANS_SERIF, ITALIC, 17),
		SANS_SERIF_ITALIC_18(SANS_SERIF, ITALIC, 18),
		SANS_SERIF_ITALIC_19(SANS_SERIF, ITALIC, 19),
		SANS_SERIF_ITALIC_20(SANS_SERIF, ITALIC, 20),
		SANS_SERIF_ITALIC_21(SANS_SERIF, ITALIC, 21),
		SANS_SERIF_ITALIC_22(SANS_SERIF, ITALIC, 22),
		SANS_SERIF_ITALIC_23(SANS_SERIF, ITALIC, 23),
		SANS_SERIF_ITALIC_24(SANS_SERIF, ITALIC, 24),
		SANS_SERIF_ITALIC_25(SANS_SERIF, ITALIC, 25),
		SANS_SERIF_ITALIC_26(SANS_SERIF, ITALIC, 26),
		SANS_SERIF_ITALIC_27(SANS_SERIF, ITALIC, 27),
		SANS_SERIF_ITALIC_28(SANS_SERIF, ITALIC, 28),
		SANS_SERIF_ITALIC_29(SANS_SERIF, ITALIC, 29),
		SANS_SERIF_ITALIC_30(SANS_SERIF, ITALIC, 30),
		SANS_SERIF_ITALIC_31(SANS_SERIF, ITALIC, 31),
		SANS_SERIF_ITALIC_32(SANS_SERIF, ITALIC, 32),
		SANS_SERIF_ITALIC_33(SANS_SERIF, ITALIC, 33),
		SANS_SERIF_ITALIC_34(SANS_SERIF, ITALIC, 34),
		SANS_SERIF_ITALIC_35(SANS_SERIF, ITALIC, 35),
		SANS_SERIF_ITALIC_36(SANS_SERIF, ITALIC, 36),
		SANS_SERIF_ITALIC_37(SANS_SERIF, ITALIC, 37),
		SANS_SERIF_ITALIC_38(SANS_SERIF, ITALIC, 38),
		SANS_SERIF_ITALIC_39(SANS_SERIF, ITALIC, 39),
		SANS_SERIF_ITALIC_40(SANS_SERIF, ITALIC, 40),
		SANS_SERIF_ITALIC_41(SANS_SERIF, ITALIC, 41),
		SANS_SERIF_ITALIC_42(SANS_SERIF, ITALIC, 42),
		SANS_SERIF_ITALIC_43(SANS_SERIF, ITALIC, 43),
		SANS_SERIF_ITALIC_44(SANS_SERIF, ITALIC, 44),
		SANS_SERIF_ITALIC_45(SANS_SERIF, ITALIC, 45),
		SANS_SERIF_ITALIC_46(SANS_SERIF, ITALIC, 46),
		SANS_SERIF_ITALIC_47(SANS_SERIF, ITALIC, 47),
		SANS_SERIF_ITALIC_48(SANS_SERIF, ITALIC, 48),
		SANS_SERIF_ITALIC_49(SANS_SERIF, ITALIC, 49),
		SANS_SERIF_ITALIC_50(SANS_SERIF, ITALIC, 50),
		SANS_SERIF_ITALIC_51(SANS_SERIF, ITALIC, 51),
		SANS_SERIF_ITALIC_52(SANS_SERIF, ITALIC, 52),
		SANS_SERIF_ITALIC_53(SANS_SERIF, ITALIC, 53),
		SANS_SERIF_ITALIC_54(SANS_SERIF, ITALIC, 54),
		SANS_SERIF_ITALIC_55(SANS_SERIF, ITALIC, 55),

		DIALOG_PLAIN_5(DIALOG, PLAIN, 5),
		DIALOG_PLAIN_6(DIALOG, PLAIN, 6),
		DIALOG_PLAIN_7(DIALOG, PLAIN, 7),
		DIALOG_PLAIN_8(DIALOG, PLAIN, 8),
		DIALOG_PLAIN_9(DIALOG, PLAIN, 9),
		DIALOG_PLAIN_10(DIALOG, PLAIN, 10),
		DIALOG_PLAIN_11(DIALOG, PLAIN, 11),
		DIALOG_PLAIN_12(DIALOG, PLAIN, 12),
		DIALOG_PLAIN_13(DIALOG, PLAIN, 13),
		DIALOG_PLAIN_14(DIALOG, PLAIN, 14),
		DIALOG_PLAIN_15(DIALOG, PLAIN, 15),
		DIALOG_PLAIN_16(DIALOG, PLAIN, 16),
		DIALOG_PLAIN_17(DIALOG, PLAIN, 17),
		DIALOG_PLAIN_18(DIALOG, PLAIN, 18),
		DIALOG_PLAIN_19(DIALOG, PLAIN, 19),
		DIALOG_PLAIN_20(DIALOG, PLAIN, 20),
		DIALOG_PLAIN_21(DIALOG, PLAIN, 21),
		DIALOG_PLAIN_22(DIALOG, PLAIN, 22),
		DIALOG_PLAIN_23(DIALOG, PLAIN, 23),
		DIALOG_PLAIN_24(DIALOG, PLAIN, 24),
		DIALOG_PLAIN_25(DIALOG, PLAIN, 25),
		DIALOG_PLAIN_26(DIALOG, PLAIN, 26),
		DIALOG_PLAIN_27(DIALOG, PLAIN, 27),
		DIALOG_PLAIN_28(DIALOG, PLAIN, 28),
		DIALOG_PLAIN_29(DIALOG, PLAIN, 29),
		DIALOG_PLAIN_30(DIALOG, PLAIN, 30),
		DIALOG_PLAIN_31(DIALOG, PLAIN, 31),
		DIALOG_PLAIN_32(DIALOG, PLAIN, 32),
		DIALOG_PLAIN_33(DIALOG, PLAIN, 33),
		DIALOG_PLAIN_34(DIALOG, PLAIN, 34),
		DIALOG_PLAIN_35(DIALOG, PLAIN, 35),
		DIALOG_PLAIN_36(DIALOG, PLAIN, 36),
		DIALOG_PLAIN_37(DIALOG, PLAIN, 37),
		DIALOG_PLAIN_38(DIALOG, PLAIN, 38),
		DIALOG_PLAIN_39(DIALOG, PLAIN, 39),
		DIALOG_PLAIN_40(DIALOG, PLAIN, 40),
		DIALOG_PLAIN_41(DIALOG, PLAIN, 41),
		DIALOG_PLAIN_42(DIALOG, PLAIN, 42),
		DIALOG_PLAIN_43(DIALOG, PLAIN, 43),
		DIALOG_PLAIN_44(DIALOG, PLAIN, 44),
		DIALOG_PLAIN_45(DIALOG, PLAIN, 45),
		DIALOG_PLAIN_46(DIALOG, PLAIN, 46),
		DIALOG_PLAIN_47(DIALOG, PLAIN, 47),
		DIALOG_PLAIN_48(DIALOG, PLAIN, 48),
		DIALOG_PLAIN_49(DIALOG, PLAIN, 49),
		DIALOG_PLAIN_50(DIALOG, PLAIN, 50),
		DIALOG_PLAIN_51(DIALOG, PLAIN, 51),
		DIALOG_PLAIN_52(DIALOG, PLAIN, 52),
		DIALOG_PLAIN_53(DIALOG, PLAIN, 53),
		DIALOG_PLAIN_54(DIALOG, PLAIN, 54),
		DIALOG_PLAIN_55(DIALOG, PLAIN, 55),

		DIALOG_BOLD_5(DIALOG, BOLD, 5),
		DIALOG_BOLD_6(DIALOG, BOLD, 6),
		DIALOG_BOLD_7(DIALOG, BOLD, 7),
		DIALOG_BOLD_8(DIALOG, BOLD, 8),
		DIALOG_BOLD_9(DIALOG, BOLD, 9),
		DIALOG_BOLD_10(DIALOG, BOLD, 10),
		DIALOG_BOLD_11(DIALOG, BOLD, 11),
		DIALOG_BOLD_12(DIALOG, BOLD, 12),
		DIALOG_BOLD_13(DIALOG, BOLD, 13),
		DIALOG_BOLD_14(DIALOG, BOLD, 14),
		DIALOG_BOLD_15(DIALOG, BOLD, 15),
		DIALOG_BOLD_16(DIALOG, BOLD, 16),
		DIALOG_BOLD_17(DIALOG, BOLD, 17),
		DIALOG_BOLD_18(DIALOG, BOLD, 18),
		DIALOG_BOLD_19(DIALOG, BOLD, 19),
		DIALOG_BOLD_20(DIALOG, BOLD, 20),
		DIALOG_BOLD_21(DIALOG, BOLD, 21),
		DIALOG_BOLD_22(DIALOG, BOLD, 22),
		DIALOG_BOLD_23(DIALOG, BOLD, 23),
		DIALOG_BOLD_24(DIALOG, BOLD, 24),
		DIALOG_BOLD_25(DIALOG, BOLD, 25),
		DIALOG_BOLD_26(DIALOG, BOLD, 26),
		DIALOG_BOLD_27(DIALOG, BOLD, 27),
		DIALOG_BOLD_28(DIALOG, BOLD, 28),
		DIALOG_BOLD_29(DIALOG, BOLD, 29),
		DIALOG_BOLD_30(DIALOG, BOLD, 30),
		DIALOG_BOLD_31(DIALOG, BOLD, 31),
		DIALOG_BOLD_32(DIALOG, BOLD, 32),
		DIALOG_BOLD_33(DIALOG, BOLD, 33),
		DIALOG_BOLD_34(DIALOG, BOLD, 34),
		DIALOG_BOLD_35(DIALOG, BOLD, 35),
		DIALOG_BOLD_36(DIALOG, BOLD, 36),
		DIALOG_BOLD_37(DIALOG, BOLD, 37),
		DIALOG_BOLD_38(DIALOG, BOLD, 38),
		DIALOG_BOLD_39(DIALOG, BOLD, 39),
		DIALOG_BOLD_40(DIALOG, BOLD, 40),
		DIALOG_BOLD_41(DIALOG, BOLD, 41),
		DIALOG_BOLD_42(DIALOG, BOLD, 42),
		DIALOG_BOLD_43(DIALOG, BOLD, 43),
		DIALOG_BOLD_44(DIALOG, BOLD, 44),
		DIALOG_BOLD_45(DIALOG, BOLD, 45),
		DIALOG_BOLD_46(DIALOG, BOLD, 46),
		DIALOG_BOLD_47(DIALOG, BOLD, 47),
		DIALOG_BOLD_48(DIALOG, BOLD, 48),
		DIALOG_BOLD_49(DIALOG, BOLD, 49),
		DIALOG_BOLD_50(DIALOG, BOLD, 50),
		DIALOG_BOLD_51(DIALOG, BOLD, 51),
		DIALOG_BOLD_52(DIALOG, BOLD, 52),
		DIALOG_BOLD_53(DIALOG, BOLD, 53),
		DIALOG_BOLD_54(DIALOG, BOLD, 54),
		DIALOG_BOLD_55(DIALOG, BOLD, 55),

		DIALOG_ITALIC_5(DIALOG, ITALIC, 5),
		DIALOG_ITALIC_6(DIALOG, ITALIC, 6),
		DIALOG_ITALIC_7(DIALOG, ITALIC, 7),
		DIALOG_ITALIC_8(DIALOG, ITALIC, 8),
		DIALOG_ITALIC_9(DIALOG, ITALIC, 9),
		DIALOG_ITALIC_10(DIALOG, ITALIC, 10),
		DIALOG_ITALIC_11(DIALOG, ITALIC, 11),
		DIALOG_ITALIC_12(DIALOG, ITALIC, 12),
		DIALOG_ITALIC_13(DIALOG, ITALIC, 13),
		DIALOG_ITALIC_14(DIALOG, ITALIC, 14),
		DIALOG_ITALIC_15(DIALOG, ITALIC, 15),
		DIALOG_ITALIC_16(DIALOG, ITALIC, 16),
		DIALOG_ITALIC_17(DIALOG, ITALIC, 17),
		DIALOG_ITALIC_18(DIALOG, ITALIC, 18),
		DIALOG_ITALIC_19(DIALOG, ITALIC, 19),
		DIALOG_ITALIC_20(DIALOG, ITALIC, 20),
		DIALOG_ITALIC_21(DIALOG, ITALIC, 21),
		DIALOG_ITALIC_22(DIALOG, ITALIC, 22),
		DIALOG_ITALIC_23(DIALOG, ITALIC, 23),
		DIALOG_ITALIC_24(DIALOG, ITALIC, 24),
		DIALOG_ITALIC_25(DIALOG, ITALIC, 25),
		DIALOG_ITALIC_26(DIALOG, ITALIC, 26),
		DIALOG_ITALIC_27(DIALOG, ITALIC, 27),
		DIALOG_ITALIC_28(DIALOG, ITALIC, 28),
		DIALOG_ITALIC_29(DIALOG, ITALIC, 29),
		DIALOG_ITALIC_30(DIALOG, ITALIC, 30),
		DIALOG_ITALIC_31(DIALOG, ITALIC, 31),
		DIALOG_ITALIC_32(DIALOG, ITALIC, 32),
		DIALOG_ITALIC_33(DIALOG, ITALIC, 33),
		DIALOG_ITALIC_34(DIALOG, ITALIC, 34),
		DIALOG_ITALIC_35(DIALOG, ITALIC, 35),
		DIALOG_ITALIC_36(DIALOG, ITALIC, 36),
		DIALOG_ITALIC_37(DIALOG, ITALIC, 37),
		DIALOG_ITALIC_38(DIALOG, ITALIC, 38),
		DIALOG_ITALIC_39(DIALOG, ITALIC, 39),
		DIALOG_ITALIC_40(DIALOG, ITALIC, 40),
		DIALOG_ITALIC_41(DIALOG, ITALIC, 41),
		DIALOG_ITALIC_42(DIALOG, ITALIC, 42),
		DIALOG_ITALIC_43(DIALOG, ITALIC, 43),
		DIALOG_ITALIC_44(DIALOG, ITALIC, 44),
		DIALOG_ITALIC_45(DIALOG, ITALIC, 45),
		DIALOG_ITALIC_46(DIALOG, ITALIC, 46),
		DIALOG_ITALIC_47(DIALOG, ITALIC, 47),
		DIALOG_ITALIC_48(DIALOG, ITALIC, 48),
		DIALOG_ITALIC_49(DIALOG, ITALIC, 49),
		DIALOG_ITALIC_50(DIALOG, ITALIC, 50),
		DIALOG_ITALIC_51(DIALOG, ITALIC, 51),
		DIALOG_ITALIC_52(DIALOG, ITALIC, 52),
		DIALOG_ITALIC_53(DIALOG, ITALIC, 53),
		DIALOG_ITALIC_54(DIALOG, ITALIC, 54),
		DIALOG_ITALIC_55(DIALOG, ITALIC, 55),

		SYMBOL_PLAIN_5(SYMBOL, PLAIN, 5),
		SYMBOL_PLAIN_6(SYMBOL, PLAIN, 6),
		SYMBOL_PLAIN_7(SYMBOL, PLAIN, 7),
		SYMBOL_PLAIN_8(SYMBOL, PLAIN, 8),
		SYMBOL_PLAIN_9(SYMBOL, PLAIN, 9),
		SYMBOL_PLAIN_10(SYMBOL, PLAIN, 10),
		SYMBOL_PLAIN_11(SYMBOL, PLAIN, 11),
		SYMBOL_PLAIN_12(SYMBOL, PLAIN, 12),
		SYMBOL_PLAIN_13(SYMBOL, PLAIN, 13),
		SYMBOL_PLAIN_14(SYMBOL, PLAIN, 14),
		SYMBOL_PLAIN_15(SYMBOL, PLAIN, 15),
		SYMBOL_PLAIN_16(SYMBOL, PLAIN, 16),
		SYMBOL_PLAIN_17(SYMBOL, PLAIN, 17),
		SYMBOL_PLAIN_18(SYMBOL, PLAIN, 18),
		SYMBOL_PLAIN_19(SYMBOL, PLAIN, 19),
		SYMBOL_PLAIN_20(SYMBOL, PLAIN, 20),
		SYMBOL_PLAIN_21(SYMBOL, PLAIN, 21),
		SYMBOL_PLAIN_22(SYMBOL, PLAIN, 22),
		SYMBOL_PLAIN_23(SYMBOL, PLAIN, 23),
		SYMBOL_PLAIN_24(SYMBOL, PLAIN, 24),
		SYMBOL_PLAIN_25(SYMBOL, PLAIN, 25),
		SYMBOL_PLAIN_26(SYMBOL, PLAIN, 26),
		SYMBOL_PLAIN_27(SYMBOL, PLAIN, 27),
		SYMBOL_PLAIN_28(SYMBOL, PLAIN, 28),
		SYMBOL_PLAIN_29(SYMBOL, PLAIN, 29),
		SYMBOL_PLAIN_30(SYMBOL, PLAIN, 30),
		SYMBOL_PLAIN_31(SYMBOL, PLAIN, 31),
		SYMBOL_PLAIN_32(SYMBOL, PLAIN, 32),
		SYMBOL_PLAIN_33(SYMBOL, PLAIN, 33),
		SYMBOL_PLAIN_34(SYMBOL, PLAIN, 34),
		SYMBOL_PLAIN_35(SYMBOL, PLAIN, 35),
		SYMBOL_PLAIN_36(SYMBOL, PLAIN, 36),
		SYMBOL_PLAIN_37(SYMBOL, PLAIN, 37),
		SYMBOL_PLAIN_38(SYMBOL, PLAIN, 38),
		SYMBOL_PLAIN_39(SYMBOL, PLAIN, 39),
		SYMBOL_PLAIN_40(SYMBOL, PLAIN, 40),
		SYMBOL_PLAIN_41(SYMBOL, PLAIN, 41),
		SYMBOL_PLAIN_42(SYMBOL, PLAIN, 42),
		SYMBOL_PLAIN_43(SYMBOL, PLAIN, 43),
		SYMBOL_PLAIN_44(SYMBOL, PLAIN, 44),
		SYMBOL_PLAIN_45(SYMBOL, PLAIN, 45),
		SYMBOL_PLAIN_46(SYMBOL, PLAIN, 46),
		SYMBOL_PLAIN_47(SYMBOL, PLAIN, 47),
		SYMBOL_PLAIN_48(SYMBOL, PLAIN, 48),
		SYMBOL_PLAIN_49(SYMBOL, PLAIN, 49),
		SYMBOL_PLAIN_50(SYMBOL, PLAIN, 50),
		SYMBOL_PLAIN_51(SYMBOL, PLAIN, 51),
		SYMBOL_PLAIN_52(SYMBOL, PLAIN, 52),
		SYMBOL_PLAIN_53(SYMBOL, PLAIN, 53),
		SYMBOL_PLAIN_54(SYMBOL, PLAIN, 54),
		SYMBOL_PLAIN_55(SYMBOL, PLAIN, 55);

		private static final int MIN_SIZE = 5, MAX_SIZE = 55;
		private static final String TYPES[] = new String[] {"_PLAIN_", "_BOLD_", "_ITALIC_"};

		private String name;
		private int type;
		private int size;

		FONT(String name, int type, int size) {
			this.name = name;
			this.type = type;
			this.size = size;
		}

		/**
		 * Returns the font name.
		 * @return Font name.
		 */
		public String getFontName() { return this.name; }
		/**
		 * Returns the font size.
		 * @return Font size.
		 */
		public int getSize() { return this.size; }
		/**
		 * Returns the font style.
		 * @return Font style: 0 (plain), 1 (bold), 2 (italic).
		 */
		public int getType() { return this.type; }
		/**
		 * Returns true for a SansSerif type font.
		 * @return True or false.
		 */
		public boolean isSansSerif() { return (name.equals(SANS_SERIF)); }
		/**
		 * Returns true for a Dialog type font.
		 * @return True or false.
		 */
		public boolean isDialog() { return (name.equals(DIALOG)); }
		/**
		 * Returns true for a Symbol type font.
		 * @return True or false.
		 */
		public boolean isSymbol() { return (name.equals(SYMBOL)); }
		/**
		 * Returns a derived font.
		 * @param font The font.
		 * @param size The new size. Should be between 5 and 55.
		 * @return The new font.
		 */
		public static FONT getDerivedFont(FONT font, int size) {
			String f = font.toString();
			f = f.substring(0, f.lastIndexOf("_"));
			if (size < MIN_SIZE) size = MIN_SIZE;
			if (size > MAX_SIZE) size = MAX_SIZE;
			f += "_"+size;
			FONT ft = FONT.valueOf(f);
			return ft;
		}

		/**
		 * Returns a derived font.
		 * @param font The font.
		 * @param size The new size. Should be between 5 and 55.
		 * @param type Font style: 0 (plain), 1 (bold), 2 (italic).
		 * @return The new font.
		 */
		public static FONT getDerivedFont(FONT font, int size, int type) {
			if (font.getSize() == size && font.getType() == type) return font;
			String f = font.toString();
			f = f.substring(0, f.lastIndexOf("_")+1);
			if (size < MIN_SIZE) size = MIN_SIZE;
			if (size > MAX_SIZE) size = MAX_SIZE;
			String fn = "SANS_SERIF";
			if (font.name.equals(DIALOG)) fn = "DIALOG";
			if (font.name.equals(SYMBOL)) fn = "SYMBOL";
			if (f.indexOf(TYPES[0]) > 0 && type != PLAIN) {
				f = fn + TYPES[type];
			} else {
				if (f.indexOf(TYPES[1]) > 0 && type != BOLD) {
					f = fn + TYPES[type];
				} else {
					if (f.indexOf(TYPES[2]) > 0 && type != ITALIC) f = fn + TYPES[type];
				}
			}
			f += ""+size;
			FONT ft = FONT.valueOf(f);
			return ft;
		}

		/**
		 * Returns a derived font.
		 * @param font The font.
		 * @param name The font name.
		 * @return The new font.
		 */
		public static FONT getDerivedFont(FONT font, String name) {
			String f = "SANS_SERIF";
			if (name.equals(DIALOG)) f = "DIALOG";
			if (name.equals(SYMBOL)) f = "SYMBOL";
			f += TYPES[font.getType()]+font.getSize();
			FONT ft = FONT.valueOf(f);
			return ft;
		}
	}

	/**
	 * Draws a line from (x0, y0) to (xf, yf), general method for antialiasing.
	 * @param i x0.
	 * @param j y0.
	 * @param k xf.
	 * @param l yf.
	 * @param fastMode True to use Wu method instead of java2d one.
	 */
	void drawLine(float i, float j, float k, float l, boolean fastMode);

	/**
	 * Draws an straight line from (x0, y0) to (xf, yf).
	 * x0 and xf or y0 and yf should be equal, otherwise
	 * a fast line method is called.
	 * @param i x0.
	 * @param j y0.
	 * @param k xf.
	 * @param l yf.
	 */
	void drawStraightLine(float i, float j, float k, float l);

	/**
	 * Draws an straight line from (x0, y0) to (xf, yf),
	 * for distances dist1 and dist2.
	 * x0 and xf or y0 and yf should be equal, otherwise
	 * a fast line method is called.
	 * @param i x0.
	 * @param j y0.
	 * @param k xf.
	 * @param l yf.
	 * @param dist1 Initial distance point x0, y0.
	 * @param dist2 Final distance point x1, y1.
	 */
	void drawStraightLine(float i, float j, float k, float l, float dist1,
			float dist2);

	/**
	 * Sets a color from RGB components.
	 * @param r Red, 0-255.
	 * @param g Green.
	 * @param b Blue.
	 * @param a Alpha value, 255 for opaque.
	 */
	void setColor(int r, int g, int b, int a);

	/**
	 * Draws an image at (x, y).
	 * @param img The image object, previously read
	 * using {@linkplain #getImage(String)}. For
	 * Java desktop it will an Image or ToolkitImage
	 * object.
	 * @param x X position.
	 * @param y Y position.
	 */
	void drawImage(Object img, float x, float y);

	/**
	 * Returns the anaglyph mode.
	 * @return Anaglyph color mode.
	 */
	ANAGLYPH_COLOR_MODE getAnaglyphMode();

	/**
	 * Draws an image at (x, y), with a given
	 * scale in x/y. This method is designed to support
	 * drawing images in (almost) vector graphics format
	 * for scaling factors lower than unity.
	 * @param img The image object, previously read
	 * using {@linkplain #getImage(String)}. For
	 * Java desktop it will an Image or ToolkitImage
	 * object.
	 * @param x X position.
	 * @param y Y position.
	 * @param scalex Scaling factor in x axis.
	 * @param scaley Scaling factor in y axis.
	 */
	void drawImage(Object img, float x, float y, double scalex, double scaley);

	/**
	 * Returns an image.
	 * @param url The path of an image resource in the
	 * classpath.
	 * @return The image object. For Java desktop it
	 * will a ToolkitImage object.
	 */
	Object getImage(String url);

	/**
	 * Returns an image.
	 * @param w The width.
	 * @param h The height.
	 * @param pixels the RGB colors of the pixels.
	 * @return The image object. For Java desktop it
	 * will a BufferedImage object.
	 */
	Object getImage(int w, int h, int[] pixels);

	/**
	 * Returns an scaled image.
	 * @param image The image.
	 * @param w The width.
	 * @param h The height.
	 * @param sameRatio True to maintain the image w/h ratio.
	 * @param useSpline True to allow using spline technique for
	 * resizing, in case the implementation supports it.
	 * @return The image object. For Java desktop it
	 * will a BufferedImage object.
	 */
	Object getScaledImage(Object image, int w, int h, boolean sameRatio, boolean useSpline);

	/**
	 * Returns an scaled image.
	 * @param image The image.
	 * @param w The width.
	 * @param h The height.
	 * @param sameRatio True to maintain the image w/h ratio.
	 * @param useSpline True to allow using spline technique for
	 * resizing, in case the implementation supports it.
	 * @param dy Number of pixels to move the image down. In case of not 0
	 * the output image will be of greater size, recentered in vertical.
	 * @return The image object. For Java desktop it
	 * will a BufferedImage object.
	 */
	Object getScaledImage(Object image, int w, int h, boolean sameRatio, boolean useSpline, int dy);

	/**
	 * Returns an scaled and/or rotated image.
	 * @param image The image.
	 * @param radius_x The translation/rotation radius in x, it will be usually
	 * the width of the image * scale * 0.5.
	 * @param radius_y The translation/rotation radius in y, it will be usually
	 * the height of the image * scale * 0.5.
	 * @param ang Rotation angle in radians.
	 * @param scalex Scale factor in x axis, after rotation.
	 * @param scaley Scale factor in y axis, after rotation.
	 * @return The image object. For Java desktop it
	 * will a BufferedImage object.
	 */
	Object getRotatedAndScaledImage(Object image, float radius_x,
			float radius_y, float ang, float scalex, float scaley);

	/**
	 * Copies an image.
	 * @param image The image.
	 * @return The copy.
	 */
	Object cloneImage(Object image);

	/**
	 * Returns an image as a set of pixels.
	 * @param image The image object.
	 * @return The set of pixels for the image.
	 */
	int[] getImageAsPixels(Object image);

	/**
	 * Returns an image with inverted colors.
	 * @param image Input image.
	 * @return The output image object. For Java desktop it
	 * will a BufferedImage object.
	 */
	Object getColorInvertedImage(Object image);

	/**
	 * Returns an image with inverted colors.
	 * @param image Input image.
	 * @param r True to invert red channel.
	 * @param g True to invert green channel.
	 * @param b True to invert blue channel.
	 * @return The output image object. For Java desktop it
	 * will a BufferedImage object.
	 */
	Object getColorInvertedImage(Object image, boolean r, boolean g, boolean b);

	/**
	 * Returns the width and height of an image.
	 * @param img The image.
	 * @return It's width and height in pixels.
	 */
	int[] getSize(Object img);

	/**
	 * Returns the rendering as an image.
	 * @return The image. For Java desktop
	 * it will be a BufferedImage.
	 */
	Object getRendering();

	/**
	 * Sets the color.
	 * @param col The color as a RGB integer.
	 * @param hasalpha True if the RGB is ARGB.
	 */
	void setColor(int col, boolean hasalpha);

	/**
	 * Sets the color.
	 * @param col The color as a RGB integer.
	 * @param alpha The new alpha component, 0-255
	 */
	void setColor(int col, int alpha);

	/**
	 * Fills an oval within (x0, y0)-(x0+width, y0+height).
	 * @param i x0.
	 * @param j y0.
	 * @param k width.
	 * @param l height.
	 * @param fastMode True to draw the oval in a fast (slightly
	 * less quality) mode.
	 */
	void fillOval(float i, float j, float k, float l, boolean fastMode);

	/**
	 * Draws a point.
	 * @param i x.
	 * @param j y.
	 * @param col color.
	 */
	void drawPoint(int i, int j, int col);

	/**
	 * Fills a rectangle within (x0, y0)-(x0+width, y0+height).
	 * @param i x0.
	 * @param j y0.
	 * @param width width.
	 * @param height height.
	 */
	void fillRect(float i, float j, float width, float height);

	/**
	 * Returns the ARGB color.
	 * @return ARGB color.
	 */
	int getColor();

	/**
	 * Draws an oval within (x0, y0)-(x0+width, y0+height).
	 * @param i x0.
	 * @param j y0.
	 * @param k width.
	 * @param l height.
	 * @param fastMode True to draw the oval in a fast (slightly
	 * less quality) mode.
	 */
	void drawOval(float i, float j, float k, float l, boolean fastMode);

	/**
	 * To disable antialiasing in shapes and text.
	 */
	void disableAntialiasing();

	/**
	 * To enable antialiasing in shapes and text.
	 */
	void enableAntialiasing();

	/**
	 * Returns the font.
	 * @return The current font.
	 */
	Graphics.FONT getFont();

	/**
	 * Sets the clip region.
	 * @param i x0.
	 * @param j y0.
	 * @param k width.
	 * @param l height.
	 */
	void setClip(int i, int j, int k, int l);

	/**
	 * Gets the clipping region.
	 * @return Four integers with x0, y0, width, height.
	 */
	int[] getClip();

	/**
	 * Sets the stroke.
	 * @param stroke The stroke object
	 */
	void setStroke(JPARSECStroke stroke);

	/**
	 * Draws a shape.
	 * @param path The shape.
	 */
	void draw(Object path);

	/**
	 * Returns a copy of the current Graphics provider.
	 * @return A copy of this Graphics.
	 */
	Graphics getGraphics();

	/**
	 * Returns a copy of the current Graphics provider, but with a different size.
	 * @param w The new width.
	 * @param h The new height.
	 * @return A copy of this Graphics.
	 */
	Graphics getGraphics(int w, int h);

	/**
	 * Sets the color.
	 * @param c The color object, AWT Color class for desktop.
	 */
	void setColorFromObject(Object c);

	/**
	 * Draws a string.
	 * @param labelgc The string.
	 * @param f x position.
	 * @param g y position.
	 */
	void drawString(String labelgc, float f, float g);

	/**
	 * Fills a shape.
	 * @param path The shape.
	 */
	void fill(Object path);

	/**
	 * Returns the bounds of a string.
	 * @param labelg The label.
	 * @return Its bounds.
	 */
	Rectangle getStringBounds(String labelg);

	/**
	 * Returns the width of a string.
	 * @param labels The label.
	 * @return Its width.
	 */
	float getStringWidth(String labels);

	/**
	 * Returns the RGB color at a given position. In case Dubois
	 * anaglyph is selected this method returns the RGB color
	 * of the left view.
	 * @param i x.
	 * @param j y.
	 * @return The color at (x, y).
	 */
	int getRGB(int i, int j);

	/**
	 * Returns the RGB colors at a given rectangle. In case Dubois
	 * anaglyph is selected this method returns the RGB color
	 * of the left view.
	 * @param i x.
	 * @param j y.
	 * @param w width.
	 * @param h height.
	 * @return The colors at (x, y) -&gt; (w, h).
	 */
	int[] getRGBs(int i, int j, int w, int h);

	/**
	 * Returns the RGB colors at a given rectangle in an image.
	 * @param s The image object.
	 * @param i x.
	 * @param j y.
	 * @param w width.
	 * @param h height.
	 * @return The colors at (x, y) -&gt; (w, h).
	 */
	int[] getRGBs(Object s, int i, int j, int w, int h);
	
	/**
	 * Returns the RGB color at a given position for the right view
	 * of a Dubois anaglyph.
	 * @param i x.
	 * @param j y.
	 * @return The color at (x, y).
	 */
	int getRGB2(int i, int j);

	/**
	 * Returns the RGB color at a given position for an input image.
	 * @param s The image object.
	 * @param i x.
	 * @param j y.
	 * @return The color at (x, y).
	 */
	int getRGB(Object s, int i, int j);

	/**
	 * Returns the RGB color at a given position for an input image,
	 * supposing an object is at (i, j) and at a distance z, and the
	 * color to return corresponds to the left view of the anaglyph.
	 * @param s The image object.
	 * @param i x.
	 * @param j y.
	 * @param z Distance.
	 * @return The color at (x, y).
	 */
	int getRGBLeft(Object s, int i, int j, float z);

	/**
	 * Returns the RGB color at a given position for an input image,
	 * supposing an object is at (i, j) and at a distance z, and the
	 * color to return corresponds to the right view of the anaglyph.
	 * @param s The image object.
	 * @param i x.
	 * @param j y.
	 * @param z Distance.
	 * @return The color at (x, y).
	 */
	int getRGBRight(Object s, int i, int j, float z);

	/**
	 * Returns the RGB color at a given position for the current rendering,
	 * supposing an object is at (i, j) and at a distance z, and the
	 * color to return corresponds to the left view of the anaglyph.
	 * @param i x.
	 * @param j y.
	 * @param z Distance.
	 * @return The color at (x, y).
	 */
	int getRGBLeft(int i, int j, float z);

	/**
	 * Returns the RGB color at a given position for the current rendering,
	 * supposing an object is at (i, j) and at a distance z, and the
	 * color to return corresponds to the right view of the anaglyph.
	 * @param i x.
	 * @param j y.
	 * @param z Distance.
	 * @return The color at (x, y).
	 */
	int getRGBRight(int i, int j, float z);

	/**
	 * Returns the width of the image in this Graphics.
	 * @return The width in pixels.
	 */
	int getWidth();

	/**
	 * Returns the height of the image in this Graphics.
	 * @return The height in pixels.
	 */
	int getHeight();

	/**
	 * Sets the font.
	 * @param font The font.
	 */
	void setFont(Graphics.FONT font);

	/**
	 * Draws a rectangle.
	 * @param x X.
	 * @param y Y.
	 * @param width Width.
	 * @param height Height.
	 */
	void drawRect(float x, float y, float width, float height);

	/**
	 * Draws a string rotated.
	 * @param label The label.
	 * @param i x.
	 * @param j y.
	 * @param k rotation angle in radians.
	 */
	void drawRotatedString(String label, float i, float j, float k);

	/**
	 * Draws a string rotated at a certain distance.
	 * @param label The label.
	 * @param i x.
	 * @param j y.
	 * @param k rotation angle in radians.
	 * @param z Distance.
	 */
	void drawRotatedString(String label, float i, float j, float k, float z);

	/**
	 * Returns a subimage of the current rendering. In case Dubois anaglyph
	 * is selected this method returns a sub-image of the left-view.
	 * @param i x0.
	 * @param j y0.
	 * @param width width.
	 * @param k height.
	 * @return The image.
	 */
	Object getImage(int i, int j, int width, int k);

	/**
	 * Returns the rendering as an sub-image.
	 * @param i x0.
	 * @param j y0.
	 * @param width width.
	 * @param k height.
	 * @return The image. For Java desktop
	 * it will be a BufferedImage.
	 */
	Object getRendering(int i, int j, int width, int k);

	/**
	 * Returns a subimage of the current rendering for the right view of the
	 * Dubois anaglyph.
	 * @param i x0.
	 * @param j y0.
	 * @param width width.
	 * @param k height.
	 * @return The image.
	 */
	Object getImage2(int i, int j, int width, int k);

	/**
	 * Waits until the input images are loaded.
	 * @param objects The images.
	 */
	void waitUntilImagesAreRead(Object[] objects);

	/**
	 * Returns the color object.
	 * @return AWT Color for Java desktop.
	 */
	Object getColorObject();

	/**
	 * Returns the red component of a color.
	 * @param color ARGB color.
	 * @return 0-255.
	 */
	int getRed(int color);
	/**
	 * Returns the green component of a color.
	 * @param color ARGB color.
	 * @return 0-255.
	 */
	int getGreen(int color);
	/**
	 * Returns the blue component of a color.
	 * @param color ARGB color.
	 * @return 0-255.
	 */
	int getBlue(int color);
	/**
	 * Returns the alpha component of a color.
	 * @param color ARGB color.
	 * @return 0-255.
	 */
	int getAlpha(int color);

	/**
	 * Inverts a color.
	 * @param color The color.
	 * @return The inverted color.
	 */
	int invertColor(int color);

	// Now methods for GeneralPath

	/**
	 * Initializes a GeneralPath object.
	 * @return The GeneralPath.
	 */
	Object generalPathInitialize();

	/**
	 * MoveTo method for a GeneralPath object.
	 * @param obj The GeneralPatho object.
	 * @param x X position.
	 * @param y Y position.
	 */
	void generalPathMoveTo(Object obj, float x, float y);

	/**
	 * LineTo method for a GeneralPath object.
	 * @param obj The GeneralPatho object.
	 * @param x X position.
	 * @param y Y position.
	 */
	void generalPathLineTo(Object obj, float x, float y);

	/**
	 * QuadTo method for a GeneralPath object.
	 * @param obj The GeneralPatho object.
	 * @param x1 X1 position.
	 * @param y1 Y1 position.
	 * @param x2 X2 position.
	 * @param y2 Y2 position.
	 */
	void generalPathQuadTo(Object obj, float x1, float y1, float x2, float y2);

	/**
	 * CurveTo method for a GeneralPath object.
	 * @param obj The GeneralPatho object.
	 * @param x1 X1 position.
	 * @param y1 Y1 position.
	 * @param x2 X2 position.
	 * @param y2 Y2 position.
	 * @param x3 X3 position.
	 * @param y3 Y3 position.
	 */
	void generalPathCurveTo(Object obj, float x1, float y1, float x2, float y2, float x3, float y3);

	/**
	 * ClosePath method for a GeneralPath object.
	 * @param obj The GeneralPatho object.
	 */
	void generalPathClosePath(Object obj);

	// Now methods for rendering in anaglyph mode

	/**
	 * Draws an oval within (x0, y0)-(x0+width, y0+height)
	 * at a given distance.
	 * @param i x0.
	 * @param j y0.
	 * @param k width.
	 * @param l height.
	 * @param dist The distance.
	 */
	void drawOval(float i, float j, float k, float l, float dist);

	/**
	 * Fills an oval within (x0, y0)-(x0+width, y0+height)
	 * at a given distance.
	 * @param i x0.
	 * @param j y0.
	 * @param k width.
	 * @param l height.
	 * @param dist The distance.
	 */
	void fillOval(float i, float j, float k, float l, float dist);

	/**
	 * Fills an oval within (x0, y0)-(x0+width, y0+height)
	 * at a given distance for the left view of the anaglyph.
	 * @param i x0.
	 * @param j y0.
	 * @param k width.
	 * @param l height.
	 * @param dist The distance.
	 */
	void fillOvalAnaglyphLeft(float i, float j, float k, float l, float dist);

	/**
	 * Fills an oval within (x0, y0)-(x0+width, y0+height)
	 * at a given distance for the right view of the anaglyph.
	 * @param i x0.
	 * @param j y0.
	 * @param k width.
	 * @param l height.
	 * @param dist The distance.
	 */
	void fillOvalAnaglyphRight(float i, float j, float k, float l, float dist);

	/**
	 * Draws a line from (x0, y0) to (xf, yf).
	 * @param i x0.
	 * @param j y0.
	 * @param k xf.
	 * @param l yf.
	 * @param dist1 The distance of the (x0, y0) point.
	 * @param dist2 The distance of the (xf, yf) point.
	 */
	void drawLine(float i, float j, float k, float l, float dist1, float dist2);

	/**
	 * Fills a shape at a given distance.
	 * @param s The shape.
	 * @param z The distance.
	 */
	void fill(Object s, float z);

	/**
	 * Draws a String at a given distance.
	 * @param s The string.
	 * @param x X position.
	 * @param y Y position.
	 * @param z Distance.
	 */
	void drawString(String s, float x, float y, float z);

	/**
	 * Draws an image at (x, y), distance z.
	 * @param img The image object, previously read
	 * using {@linkplain #getImage(String)}. For
	 * Java desktop it will an Image or ToolkitImage
	 * object.
	 * @param x X position.
	 * @param y Y position.
	 * @param z Distance.
	 */
	void drawImage(Object img, float x, float y, float z);

	/**
	 * Draws an image at (x, y), distance z, and with a given
	 * scale in x/y. This method is designed to support
	 * drawing images in (almost) vector graphics format
	 * for scaling factors lower than unity.
	 * @param img The image object, previously read
	 * using {@linkplain #getImage(String)}. For
	 * Java desktop it will an Image or ToolkitImage
	 * object.
	 * @param x X position.
	 * @param y Y position.
	 * @param z Distance.
	 * @param scalex Scaling factor in x axis.
	 * @param scaley Scaling factor in y axis.
	 */
	void drawImage(Object img, float x, float y, float z, double scalex,
			double scaley);

	/**
	 * Fills a rectangle within (x0, y0)-(x0+width, y0+height), at
	 * distance z.
	 * @param i x0.
	 * @param j y0.
	 * @param width width.
	 * @param height height.
	 * @param z Distance.
	 */
	void fillRect(float i, float j, float width, float height, float z);

	/**
	 * Draws a shape at distance z.
	 * @param path The shape.
	 * @param z Distance.
	 */
	void draw(Object path, float z);

	/**
	 * Draws a rectangle at distance z.
	 * @param x X.
	 * @param y Y.
	 * @param width Width.
	 * @param height Height.
	 * @param z Distance.
	 */
	void drawRect(float x, float y, float width, float height, float z);

	/** Disables the analyph mode. */
	void disableAnaglyph();

	/** Sets the left and right images of the anaglyph in the current Graphics.
	 * @param image1 Left image.
	 * @param image2 Right image.
	 */
	void setAnaglyph(Object image1, Object image2);

	/** Sets the left and right images of the anaglyph in the current Graphics,
	 * drawing them into a given (x, y) position.
	 * @param image1 Left image.
	 * @param image2 Right image.
	 * @param x X position to draw the images on the current views.
	 * @param y Y position to draw the images on the current views.
	 */
	void setAnaglyph(Object image1, Object image2, float x, float y);

	/** Enables the inversion of the image in horizontal and/or vertical. */
	void enableInversion();

	/** Disables the inversion of the image in horizontal and/or vertical. */
	void disableInversion();

	/**
	 * Enables inversion.
	 * @param h True to allow (force) horizontal inversion.
	 * @param v True to allow (force) vertical inversion.
	 */
	void enableInversion(boolean h, boolean v);

	/**
	 * Return the inverted position for a given x, y coordinates.
	 * @param i X position.
	 * @param j Y position.
	 * @return The inverted position if inversion is enabled and
	 * some of the x and/or inversion flags are set.
	 */
	float[] getInvertedPosition(float i, float j);

	  /**
	   * Creates an anaglyph image from left and right eye views. This method uses
	   * the method described in <i>Conversion of a Stereo Pair to Anaglyph with the
	   * Lest-Squares Projection Method</i> (2009), by Eric Dubois (for CRT/Plasma
	   * displays). The result is an anaglyph that can be viewed with a pair of 3d
	   * glasses with some color for the left eye, and another for the right one.
	   * @param leftImg Left image. Should be in RGB color space.
	   * @param rightImg Right image. Should be in RGB color space.
	   * @return Anaglyph view in RGB color space, adequate for CRT/Plasma screens.
	   */
	Object blendImagesToAnaglyphMode(Object leftImg,
			Object rightImg);

	/**
	 * Sets the color of an image at a given pixel.
	 * @param image The image.
	 * @param x X position.
	 * @param y Y position.
	 * @param c The color.
	 */
	void setRGB(Object image, int x, int y, int c);

	/**
	 * Returns if this instance is rendering to an external
	 * Graphics device for vector graphics, or to an image
	 * hold internally.
	 * @return True or false.
	 */
	boolean renderingToExternalGraphics();

	/**
	 * Transforms to transparent the color in a given image.
	 * @param img The image.
	 * @param color The color.
	 * @param fromBlack True to set as transparent all colors from
	 * black to the input one.
	 * @param toWhite True to set as transparent all colors from input
	 * to white.
	 * @param t Transparency level. 0 for fully transparent.
	 * @return Output image.
	 */
	Object makeColorTransparent(Object img, int color, boolean fromBlack, boolean toWhite, int t);

	/**
	 * Returns a sub-image.
	 * @param img Input image.
	 * @param i X position.
	 * @param j Y position.
	 * @param width Width.
	 * @param k Height.
	 * @return Sub-image.
	 */
	Object getImage(Object img, int i, int j, int width, int k);

	/**
	 * Returns the (possibly) inverted rectangle.
	 * @param rec The edges as x, y, width, height.
	 * @return The inverted rectangle if any inversion is enabled.
	 */
	int[] getInvertedRectangle(int[] rec);

	/**
	 * Draws a set of lines connected.
	 * @param i The set of x points.
	 * @param j The set of y points.
	 * @param np The number of lines to draw.
	 * @param dist The set of distances of each point.
	 * @param fastMode true to use fast mode, but with less quality.
	 * In particular, stroke, transparency, and antialiasing are not supported.
	 */
	void drawLines(int[] i, int[] j, int np, float dist[], boolean fastMode);

	/**
	 * Draws a set of lines connected.
	 * @param i The set of x points.
	 * @param j The set of y points.
	 * @param np The number of lines to draw.
	 * @param fastMode true to use fast mode, but with less quality.
	 * In particular, stroke, transparency, and antialiasing are not supported.
	 */
	void drawLines(int[] i, int[] j, int np, boolean fastMode);

	/**
	 * Returns the Graphics instance directly,
	 * Graphics2D for desktop or Canvas for Android.
	 * @return The Graphics object for the main
	 * image or the left image of the anaglyph.
	 */
	Object getDirectGraphics();

	/**
	 * Returns the Graphics instance directly,
	 * Graphics2D for desktop or Canvas for Android.
	 * @return The Graphics object for the secondary
	 * image or the right image of the anaglyph.
	 */
	Object getDirectGraphics2();

	/**
	 * Disables the transparency in the colors.
	 */
	void disableTransparency();

	/**
	 * Enables transparent colors.
	 */
	void enableTransparency();

	/**
	 * Returns if this Graphics context is for Android or not.
	 * @return True or false.
	 */
	boolean renderingToAndroid();

	/**
	 * Rotates the Graphics context. Support is minimal, use with care.
	 * @param radians Angle in radians.
	 */
	void rotate(double radians);

	/**
	 * Traslates the Graphics context. Support is minimal, use with care.
	 * @param x X traslation.
	 * @param y Y position of traslation point.
	 */
	void traslate(double x, double y);

	/**
	 * Returns the rotation angle of this instance.
	 * @return Angle in radians.
	 */
	double getRotation();

	/**
	 * Returns the traslation values for this instance.
	 * @return Traslation in x and in y.
	 */
	double[] getTranslation();

	/**
	 * Adds a given object to the database. This is currently used
	 * only in getImage method, so that the image returned is the
	 * one hold in the database, without wasting time in reading
	 * the file or doing some image processing. Not recommended in Android.
	 * @param img The image/object to save.
	 * @param id The identifier. Use the path set in getImage.
	 * @param life Life time in seconds in the database. &lt;= 0 to keep it
	 * forever.
	 */
	void addToDataBase(Object img, String id, int life);

	/**
	 * Clears the elements in the database.
	 */
	void clearDataBase();

	/**
	 * Returns an object from the database.
	 * @param id The identifier, for instance the path to an image in
	 * getImage method.
	 * @return The object, or null if none is found.
	 */
	Object getFromDataBase(String id);

	/**
	 * Changes a color in an image.
	 * @param img The image.
	 * @param col0 First color.
	 * @param col1 New color.
	 * @param both True to change also the new color by
	 * the first one in the inputi mage.
	 * @return The new image.
	 */
	//Object changeColor(Object img, int col0, int col1, boolean both);

	  /** RGB color for IndianRed. */
	  public static final int COLOR_RED_IndianRed = -3318692;
	  /** RGB color for LightCoral. */
	  public static final int COLOR_RED_LightCoral = -1015680;
	  /** RGB color for Salmon. */
	  public static final int COLOR_RED_Salmon = -360334;
	  /** RGB color for DarkSalmon. */
	  public static final int COLOR_RED_DarkSalmon = -1468806;
	  /** RGB color for LightSalmon. */
	  public static final int COLOR_RED_LightSalmon = -24454;
	  /** RGB color for Crimson. */
	  public static final int COLOR_RED_Crimson = -2354116;
	  /** RGB color for Red. */
	  public static final int COLOR_RED_Red = -65536;
	  /** RGB color for FireBrick. */
	  public static final int COLOR_RED_FireBrick = -5103070;
	  /** RGB color for DarkRed. */
	  public static final int COLOR_RED_DarkRed = -7667712;
	  /** RGB color for Pink. */
	  public static final int COLOR_PINK_Pink = -16181;
	  /** RGB color for LightPink. */
	  public static final int COLOR_PINK_LightPink = -18751;
	  /** RGB color for HotPink. */
	  public static final int COLOR_PINK_HotPink = -38476;
	  /** RGB color for DeepPink. */
	  public static final int COLOR_PINK_DeepPink = -60269;
	  /** RGB color for MediumVioletRed. */
	  public static final int COLOR_PINK_MediumVioletRed = -3730043;
	  /** RGB color for PaleVioletRed. */
	  public static final int COLOR_PINK_PaleVioletRed = -2396013;
	  /** RGB color for LightSalmon. */
	  public static final int COLOR_ORANGE_LightSalmon = -24454;
	  /** RGB color for Coral. */
	  public static final int COLOR_ORANGE_Coral = -32944;
	  /** RGB color for Tomato. */
	  public static final int COLOR_ORANGE_Tomato = -40121;
	  /** RGB color for OrangeRed. */
	  public static final int COLOR_ORANGE_OrangeRed = -47872;
	  /** RGB color for DarkOrange. */
	  public static final int COLOR_ORANGE_DarkOrange = -29696;
	  /** RGB color for Orange. */
	  public static final int COLOR_ORANGE_Orange = -23296;
	  /** RGB color for Gold. */
	  public static final int COLOR_YELLOW_Gold = -10496;
	  /** RGB color for Yellow. */
	  public static final int COLOR_YELLOW_Yellow = -256;
	  /** RGB color for LightYellow. */
	  public static final int COLOR_YELLOW_LightYellow = -32;
	  /** RGB color for LemonChiffon. */
	  public static final int COLOR_YELLOW_LemonChiffon = -1331;
	  /** RGB color for LightGoldenrodYellow. */
	  public static final int COLOR_YELLOW_LightGoldenrodYellow = -329006;
	  /** RGB color for PapayaWhip. */
	  public static final int COLOR_YELLOW_PapayaWhip = -4139;
	  /** RGB color for Moccasin. */
	  public static final int COLOR_YELLOW_Moccasin = -6987;
	  /** RGB color for PeachPuff. */
	  public static final int COLOR_YELLOW_PeachPuff = -9543;
	  /** RGB color for PaleGoldenrod. */
	  public static final int COLOR_YELLOW_PaleGoldenrod = -1120086;
	  /** RGB color for Khaki. */
	  public static final int COLOR_YELLOW_Khaki = -989556;
	  /** RGB color for DarkKhaki. */
	  public static final int COLOR_YELLOW_DarkKhaki = -4343957;
	  /** RGB color for Lavender. */
	  public static final int COLOR_PURPLE_Lavender = -1644806;
	  /** RGB color for Thistle. */
	  public static final int COLOR_PURPLE_Thistle = -2572328;
	  /** RGB color for Plum. */
	  public static final int COLOR_PURPLE_Plum = -2252579;
	  /** RGB color for Violet. */
	  public static final int COLOR_PURPLE_Violet = -1146130;
	  /** RGB color for Orchid. */
	  public static final int COLOR_PURPLE_Orchid = -2461482;
	  /** RGB color for Fuchsia. */
	  public static final int COLOR_PURPLE_Fuchsia = -65281;
	  /** RGB color for Magenta. */
	  public static final int COLOR_PURPLE_Magenta = -65281;
	  /** RGB color for MediumOrchid. */
	  public static final int COLOR_PURPLE_MediumOrchid = -4565549;
	  /** RGB color for MediumPurple. */
	  public static final int COLOR_PURPLE_MediumPurple = -7114533;
	  /** RGB color for Amethyst. */
	  public static final int COLOR_PURPLE_Amethyst = -6723892;
	  /** RGB color for BlueViolet. */
	  public static final int COLOR_PURPLE_BlueViolet = -7722014;
	  /** RGB color for DarkViolet. */
	  public static final int COLOR_PURPLE_DarkViolet = -7077677;
	  /** RGB color for DarkOrchid. */
	  public static final int COLOR_PURPLE_DarkOrchid = -6737204;
	  /** RGB color for DarkMagenta. */
	  public static final int COLOR_PURPLE_DarkMagenta = -7667573;
	  /** RGB color for Purple. */
	  public static final int COLOR_PURPLE_Purple = -8388480;
	  /** RGB color for Indigo. */
	  public static final int COLOR_PURPLE_Indigo = -11861886;
	  /** RGB color for SlateBlue. */
	  public static final int COLOR_PURPLE_SlateBlue = -9807155;
	  /** RGB color for DarkSlateBlue. */
	  public static final int COLOR_PURPLE_DarkSlateBlue = -12042869;
	  /** RGB color for MediumSlateBlue. */
	  public static final int COLOR_PURPLE_MediumSlateBlue = -8689426;
	  /** RGB color for GreenYellow. */
	  public static final int COLOR_GREEN_GreenYellow = -5374161;
	  /** RGB color for Chartreuse. */
	  public static final int COLOR_GREEN_Chartreuse = -8388864;
	  /** RGB color for LawnGreen. */
	  public static final int COLOR_GREEN_LawnGreen = -8586240;
	  /** RGB color for Lime. */
	  public static final int COLOR_GREEN_Lime = -16711936;
	  /** RGB color for LimeGreen. */
	  public static final int COLOR_GREEN_LimeGreen = -13447886;
	  /** RGB color for PaleGreen. */
	  public static final int COLOR_GREEN_PaleGreen = -6751336;
	  /** RGB color for LightGreen. */
	  public static final int COLOR_GREEN_LightGreen = -7278960;
	  /** RGB color for MediumSpringGreen. */
	  public static final int COLOR_GREEN_MediumSpringGreen = -16713062;
	  /** RGB color for SpringGreen. */
	  public static final int COLOR_GREEN_SpringGreen = -16711809;
	  /** RGB color for MediumSeaGreen. */
	  public static final int COLOR_GREEN_MediumSeaGreen = -12799119;
	  /** RGB color for SeaGreen. */
	  public static final int COLOR_GREEN_SeaGreen = -13726889;
	  /** RGB color for ForestGreen. */
	  public static final int COLOR_GREEN_ForestGreen = -14513374;
	  /** RGB color for Green. */
	  public static final int COLOR_GREEN_Green = -16744448;
	  /** RGB color for DarkGreen. */
	  public static final int COLOR_GREEN_DarkGreen = -16751616;
	  /** RGB color for YellowGreen. */
	  public static final int COLOR_GREEN_YellowGreen = -6632142;
	  /** RGB color for OliveDrab. */
	  public static final int COLOR_GREEN_OliveDrab = -9728477;
	  /** RGB color for Olive. */
	  public static final int COLOR_GREEN_Olive = -8355840;
	  /** RGB color for DarkOliveGreen. */
	  public static final int COLOR_GREEN_DarkOliveGreen = -11179217;
	  /** RGB color for MediumAquamarine. */
	  public static final int COLOR_GREEN_MediumAquamarine = -10039894;
	  /** RGB color for DarkSeaGreen. */
	  public static final int COLOR_GREEN_DarkSeaGreen = -7357297;
	  /** RGB color for LightSeaGreen. */
	  public static final int COLOR_GREEN_LightSeaGreen = -14634326;
	  /** RGB color for DarkCyan. */
	  public static final int COLOR_GREEN_DarkCyan = -16741493;
	  /** RGB color for Teal. */
	  public static final int COLOR_GREEN_Teal = -16744320;
	  /** RGB color for Aqua. */
	  public static final int COLOR_BLUE_Aqua = -16711681;
	  /** RGB color for Cyan. */
	  public static final int COLOR_BLUE_Cyan = -16711681;
	  /** RGB color for LightCyan. */
	  public static final int COLOR_BLUE_LightCyan = -2031617;
	  /** RGB color for PaleTurquoise. */
	  public static final int COLOR_BLUE_PaleTurquoise = -5247250;
	  /** RGB color for Aquamarine. */
	  public static final int COLOR_BLUE_Aquamarine = -8388652;
	  /** RGB color for Turquoise. */
	  public static final int COLOR_BLUE_Turquoise = -12525360;
	  /** RGB color for MediumTurquoise. */
	  public static final int COLOR_BLUE_MediumTurquoise = -12004916;
	  /** RGB color for DarkTurquoise. */
	  public static final int COLOR_BLUE_DarkTurquoise = -16724271;
	  /** RGB color for CadetBlue. */
	  public static final int COLOR_BLUE_CadetBlue = -10510688;
	  /** RGB color for SteelBlue. */
	  public static final int COLOR_BLUE_SteelBlue = -12156236;
	  /** RGB color for LightSteelBlue. */
	  public static final int COLOR_BLUE_LightSteelBlue = -5192482;
	  /** RGB color for PowderBlue. */
	  public static final int COLOR_BLUE_PowderBlue = -5185306;
	  /** RGB color for LightBlue. */
	  public static final int COLOR_BLUE_LightBlue = -5383962;
	  /** RGB color for SkyBlue. */
	  public static final int COLOR_BLUE_SkyBlue = -7876885;
	  /** RGB color for LightSkyBlue. */
	  public static final int COLOR_BLUE_LightSkyBlue = -7876870;
	  /** RGB color for DeepSkyBlue. */
	  public static final int COLOR_BLUE_DeepSkyBlue = -16728065;
	  /** RGB color for DodgerBlue. */
	  public static final int COLOR_BLUE_DodgerBlue = -14774017;
	  /** RGB color for CornflowerBlue. */
	  public static final int COLOR_BLUE_CornflowerBlue = -10185235;
	  /** RGB color for MediumSlateBlue. */
	  public static final int COLOR_BLUE_MediumSlateBlue = -8689426;
	  /** RGB color for RoyalBlue. */
	  public static final int COLOR_BLUE_RoyalBlue = -12490271;
	  /** RGB color for Blue. */
	  public static final int COLOR_BLUE_Blue = -16776961;
	  /** RGB color for MediumBlue. */
	  public static final int COLOR_BLUE_MediumBlue = -16777011;
	  /** RGB color for DarkBlue. */
	  public static final int COLOR_BLUE_DarkBlue = -16777077;
	  /** RGB color for Navy. */
	  public static final int COLOR_BLUE_Navy = -16777088;
	  /** RGB color for MidnightBlue. */
	  public static final int COLOR_BLUE_MidnightBlue = -15132304;
	  /** RGB color for Cornsilk. */
	  public static final int COLOR_BROWN_Cornsilk = -1828;
	  /** RGB color for BlanchedAlmond. */
	  public static final int COLOR_BROWN_BlanchedAlmond = -5171;
	  /** RGB color for Bisque. */
	  public static final int COLOR_BROWN_Bisque = -6972;
	  /** RGB color for NavajoWhite. */
	  public static final int COLOR_BROWN_NavajoWhite = -8531;
	  /** RGB color for Wheat. */
	  public static final int COLOR_BROWN_Wheat = -663885;
	  /** RGB color for BurlyWood. */
	  public static final int COLOR_BROWN_BurlyWood = -2180985;
	  /** RGB color for Tan. */
	  public static final int COLOR_BROWN_Tan = -2968436;
	  /** RGB color for RosyBrown. */
	  public static final int COLOR_BROWN_RosyBrown = -4419697;
	  /** RGB color for SandyBrown. */
	  public static final int COLOR_BROWN_SandyBrown = -744352;
	  /** RGB color for Goldenrod. */
	  public static final int COLOR_BROWN_Goldenrod = -2448096;
	  /** RGB color for DarkGoldenrod. */
	  public static final int COLOR_BROWN_DarkGoldenrod = -4684277;
	  /** RGB color for Peru. */
	  public static final int COLOR_BROWN_Peru = -3308225;
	  /** RGB color for Chocolate. */
	  public static final int COLOR_BROWN_Chocolate = -2987746;
	  /** RGB color for SaddleBrown. */
	  public static final int COLOR_BROWN_SaddleBrown = -7650029;
	  /** RGB color for Sienna. */
	  public static final int COLOR_BROWN_Sienna = -6270419;
	  /** RGB color for Brown. */
	  public static final int COLOR_BROWN_Brown = -5952982;
	  /** RGB color for Maroon. */
	  public static final int COLOR_BROWN_Maroon = -8388608;
	  /** RGB color for White. */
	  public static final int COLOR_WHITE_White = -1;
	  /** RGB color for Snow. */
	  public static final int COLOR_WHITE_Snow = -1286;
	  /** RGB color for Honeydew. */
	  public static final int COLOR_WHITE_Honeydew = -983056;
	  /** RGB color for MintCream. */
	  public static final int COLOR_WHITE_MintCream = -655366;
	  /** RGB color for Azure. */
	  public static final int COLOR_WHITE_Azure = -983041;
	  /** RGB color for AliceBlue. */
	  public static final int COLOR_WHITE_AliceBlue = -984833;
	  /** RGB color for GhostWhite. */
	  public static final int COLOR_WHITE_GhostWhite = -460545;
	  /** RGB color for WhiteSmoke. */
	  public static final int COLOR_WHITE_WhiteSmoke = -657931;
	  /** RGB color for Seashell. */
	  public static final int COLOR_WHITE_Seashell = -2578;
	  /** RGB color for Beige. */
	  public static final int COLOR_WHITE_Beige = -657956;
	  /** RGB color for OldLace. */
	  public static final int COLOR_WHITE_OldLace = -133658;
	  /** RGB color for FloralWhite. */
	  public static final int COLOR_WHITE_FloralWhite = -1296;
	  /** RGB color for Ivory. */
	  public static final int COLOR_WHITE_Ivory = -16;
	  /** RGB color for AntiqueWhite. */
	  public static final int COLOR_WHITE_AntiqueWhite = -332841;
	  /** RGB color for Linen. */
	  public static final int COLOR_WHITE_Linen = -331546;
	  /** RGB color for LavenderBlush. */
	  public static final int COLOR_WHITE_LavenderBlush = -3851;
	  /** RGB color for MistyRose. */
	  public static final int COLOR_WHITE_MistyRose = -6943;
	  /** RGB color for Gainsboro. */
	  public static final int COLOR_GRAY_Gainsboro = -2302756;
	  /** RGB color for LightGrey. */
	  public static final int COLOR_GRAY_LightGrey = -2894893;
	  /** RGB color for Silver. */
	  public static final int COLOR_GRAY_Silver = -4144960;
	  /** RGB color for DarkGray. */
	  public static final int COLOR_GRAY_DarkGray = -5658199;
	  /** RGB color for Gray. */
	  public static final int COLOR_GRAY_Gray = -8355712;
	  /** RGB color for DimGray. */
	  public static final int COLOR_GRAY_DimGray = -9868951;
	  /** RGB color for LightSlateGray. */
	  public static final int COLOR_GRAY_LightSlateGray = -8943463;
	  /** RGB color for SlateGray. */
	  public static final int COLOR_GRAY_SlateGray = -9404272;
	  /** RGB color for DarkSlateGray. */
	  public static final int COLOR_GRAY_DarkSlateGray = -13676721;
	  /** RGB color for Black. */
	  public static final int COLOR_GRAY_Black = -16777216;
}
