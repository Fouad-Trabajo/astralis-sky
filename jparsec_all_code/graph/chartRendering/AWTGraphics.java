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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.PixelGrabber;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

import jparsec.graph.DataSet;
import jparsec.graph.JPARSECStroke;
import jparsec.graph.TextLabel;
import jparsec.io.image.Picture;
import jparsec.math.FastMath;
import jparsec.util.DataBase;
import jparsec.util.JPARSECException;
import jparsec.util.Logger;
import jparsec.util.Logger.LEVEL;
import jparsec.vo.GeneralQuery;

/**
 * Implements the {@linkplain Graphics} interface for Java desktop platform,
 * with anaglyph support and optional sub-pixel precision. This class
 * contains fast implementations of drawLine and other methods from the
 * PerfGraphics library at http://www.randelshofer.ch/oop/graphics/.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class AWTGraphics implements Graphics {

	/**
	 * The color mode.
	 */
	private ANAGLYPH_COLOR_MODE colorMode = ANAGLYPH_COLOR_MODE.GREEN_RED;

	private BufferedImage image, image2 = null;
	private Graphics2D g;
	private Graphics2D g2 = null;
	private int criteria = GeneralPath.WIND_EVEN_ODD, w = 0, h = 0;
	private boolean invertH = false, invertV = false;
	private boolean invertEnabled = true;
	private boolean transparencyEnabled = true;
	private boolean externalGraphics = false;
	private boolean isContinuous = true;
	private float lineWidth = 1;
	private int rasterData[], rasterData2[];
	private java.awt.Rectangle clip = null;
	private boolean useRaster = false;
	private TextLabel t = null;

	private static int IMAGE_TYPE = BufferedImage.TYPE_INT_RGB;

	/**
	 * Sets the type of BufferedImage to be created. Default is RGB without
	 * transparency.
	 * @param type One of the BufferedImage#TYPE constants.
	 */
	public static void setBufferedImageType(int type) {
		IMAGE_TYPE = type;
	}

	/**
	 * Returns the type of BufferedImage to be created. Default is RGB without
	 * transparency.
	 * @return Image type used for rendering, one of the BufferedImage#TYPE constants.
	 */
	public static int getBufferedImageType() {
		return IMAGE_TYPE;
	}

	/**
	 * Creates a new Graphics provider for Java desktop platform,
	 * with no anaglyph mode. Antialiasing is enabled by default.
	 * @param w The width of the image to render.
	 * @param h The height.
	 * @param invertH True to invert image horizontally.
	 * @param invertV True to invert image vertically.
	 */
	public AWTGraphics(int w, int h, boolean invertH, boolean invertV) {
		this.w = w;
		this.h = h;
		image = new BufferedImage(w, h, IMAGE_TYPE);
		g = image.createGraphics();
		this.colorMode = Graphics.ANAGLYPH_COLOR_MODE.NO_ANAGLYPH;
		this.enableAntialiasing();
		this.invertH = invertH;
		this.invertV = invertV;
		if (!useRaster || rasterData == null) setUseRaster(true);
	}

	/**
	 * Creates a new Graphics provider for Java desktop platform,
	 * with no anaglyph mode, suitable for an external Graphics
	 * object. Antialiasing is enabled by default.
	 * @param w The width of the image to render.
	 * @param h The height.
	 * @param invertH True to invert image horizontally.
	 * @param invertV True to invert image vertically.
	 * @param g The Graphics instance.
	 * @param generateImageAlso True to generate also an image of the
	 * rendering. False is recommended to save memory and performance,
	 * when possible.
	 */
	public AWTGraphics(int w, int h, boolean invertH, boolean invertV, Graphics2D g,
			boolean generateImageAlso) {
		this.w = w;
		this.h = h;
		if (generateImageAlso) {
			image = new BufferedImage(w, h, IMAGE_TYPE);
			this.g = image.createGraphics();
			this.g2 = g;
		} else {
			this.g = g;
		}
		image2 = null;
		this.colorMode = Graphics.ANAGLYPH_COLOR_MODE.NO_ANAGLYPH;
		this.enableAntialiasing();
		this.invertH = invertH;
		this.invertV = invertV;
		externalGraphics = true;
		if (!useRaster || rasterData == null) setUseRaster(true);
	}

	/**
	 * Creates a new Graphics provider for Java desktop platform.
	 * Antialiasing is enabled by default.
	 * @param w The width of the image to render.
	 * @param h The height.
	 * @param mode The color mode, anaglyph or normal.
	 * @param invertH True to invert image horizontally.
	 * @param invertV True to invert image vertically.
	 */
	public AWTGraphics(int w, int h, ANAGLYPH_COLOR_MODE mode, boolean invertH, boolean invertV) {
		this.w = w;
		this.h = h;
		image = new BufferedImage(w, h, IMAGE_TYPE);
		g = image.createGraphics();
		this.colorMode = mode;
		if (colorMode.isReal3D()) {
			image2 = new BufferedImage(w, h, IMAGE_TYPE);
			g2 = image2.createGraphics();
		}
		this.enableAntialiasing();
		this.invertH = invertH;
		this.invertV = invertV;
		if (!useRaster || rasterData == null) setUseRaster(true);
	}

	/**
	 * Creates a new Graphics provider for Java desktop platform.
	 * Antialiasing is enabled by default.
	 * @param w The width of the image to render.
	 * @param h The height.
	 * @param invertH True to invert image horizontally.
	 * @param invertV True to invert image vertically.
	 * @param externalImage The external image to render to. The difference respect
	 * other constructors not using this parameter is a better performance when
	 * rendering to external graphics and anaglyph mode are not required.
	 */
	public AWTGraphics(int w, int h, boolean invertH, boolean invertV,
			BufferedImage externalImage) {
		this.w = w;
		this.h = h;
		image = externalImage; //new BufferedImage(w, h, IMAGE_TYPE);
		g = image.createGraphics();
		this.colorMode = Graphics.ANAGLYPH_COLOR_MODE.NO_ANAGLYPH;
		if (colorMode.isReal3D()) {
			image2 = new BufferedImage(w, h, IMAGE_TYPE);
			g2 = image2.createGraphics();
		}
		this.enableAntialiasing();
		this.invertH = invertH;
		this.invertV = invertV;
		if (!useRaster || rasterData == null) setUseRaster(true);
	}

	/**
	 * Regenerates the Graphics2D context in a fast way, without creating a new BufferedImage unless
	 * something in the input values changed from initial values.
	 * Antialiasing is enabled by default.
	 * @param w The width of the image to render.
	 * @param h The height.
	 * @param mode The color mode, anaglyph or normal.
	 * @param invertH True to invert image horizontally.
	 * @param invertV True to invert image vertically.
	 */
	public void regenerate(int w, int h, ANAGLYPH_COLOR_MODE mode, boolean invertH, boolean invertV) {
		if (w != this.w || h != this.h || colorMode != mode || invertH != this.invertH || invertV != this.invertV) {
			this.w = w;
			this.h = h;
			image = new BufferedImage(w, h, IMAGE_TYPE);
			rasterData = null;
			image2 = null;
			rasterData2 = null;
			this.colorMode = mode;
			if (colorMode.isReal3D()) {
				image2 = new BufferedImage(w, h, IMAGE_TYPE);
				rasterData2 = null;
			}
			useRaster = false;
			this.invertH = invertH;
			this.invertV = invertV;
		}
		this.colorMode = mode;

		g = image.createGraphics();
		g2 = null;
		if (image2 != null && colorMode.isReal3D())
			g2 = image2.createGraphics();
		this.enableAntialiasing();
		//if (!useRaster || rasterData == null)
			setUseRaster(true);
	}

	/**
	 * Regenerates the Graphics2D context in a fast way, without creating a new BufferedImage unless
	 * something in the input values changed from initial values.
	 * Antialiasing is enabled by default.
	 * @param w The width of the image to render.
	 * @param h The height.
	 * @param invertH True to invert image horizontally.
	 * @param invertV True to invert image vertically.
	 * @param externalImage The external image to render to. The difference respect
	 * other constructors not using this parameter is a better performance when
	 * rendering to external graphics and anaglyph mode are not required.
	 */
	public void regenerate(int w, int h, boolean invertH, boolean invertV, BufferedImage externalImage) {
		ANAGLYPH_COLOR_MODE mode = Graphics.ANAGLYPH_COLOR_MODE.NO_ANAGLYPH;
		if (w != this.w || h != this.h || colorMode != mode || invertH != this.invertH || invertV != this.invertV) {
			this.w = w;
			this.h = h;
			rasterData = null;
			rasterData2 = null;
			image2 = null;
			this.colorMode = mode;
			if (colorMode.isReal3D()) {
				image2 = new BufferedImage(w, h, IMAGE_TYPE);
				rasterData2 = null;
			}
			useRaster = false;
			this.invertH = invertH;
			this.invertV = invertV;
		}
		this.colorMode = mode;

		image = externalImage;
		g = image.createGraphics();
		g2 = null;
		if (image2 != null && colorMode.isReal3D())
			g2 = image2.createGraphics();
		this.enableAntialiasing();

		setUseRaster(true);
	}

	@Override
	public ANAGLYPH_COLOR_MODE getAnaglyphMode() {
		return this.colorMode;
	}

	@Override
	public void disableAnaglyph() {
		colorMode = ANAGLYPH_COLOR_MODE.NO_ANAGLYPH;
		image2 = null;
		g2 = null;
	}

	@Override
	public void disableInversion() {
		this.invertEnabled = false;
	}

	@Override
	public void enableInversion() {
		this.invertEnabled = true;
	}

	@Override
	public void disableTransparency() {
		this.transparencyEnabled = false;
	}

	@Override
	public void enableTransparency() {
		this.transparencyEnabled = true;
	}

	@Override
	public boolean renderingToExternalGraphics() {
		return externalGraphics;
	}

	@Override
	public void enableInversion(boolean h, boolean v) {
		this.invertEnabled = true;
		this.invertH = h;
		this.invertV = v;
	}

	@Override
	public void setAnaglyph(Object image1, Object image2) {
		g.drawImage(toImage(image1), 0, 0, null);

		if (g2 != null && image2 != null) {
			g2.drawImage(toImage(image2), 0, 0, null);
		}
	}

	@Override
	public void setAnaglyph(Object image1, Object image2, float x, float y) {
		g.drawImage(toImage(image1), (int)x, (int)y, null);

		if (g2 != null && image2 != null) {
			g2.drawImage(toImage(image2), (int)x, (int)y, null);
		}
	}

	private float invertX(float x) {
		return this.getWidth()-1-x;
	}
	private float invertY(float y) {
		return this.getHeight()-1-y;
	}

	@Override
	public void drawLine(float i, float j, float k, float l, boolean fast) {
		drawLine(i, j, k, l, g, g2, fast); //, this.getClip());
	}

	@Override
	public void drawStraightLine(float i, float j, float k, float l) {
		if (!externalGraphics && (i == k || j == l)) {
			/*
			Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
			this.disableAntialiasing();
			drawLine(i, j, k, l, g, g2);
			if (aa == RenderingHints.VALUE_ANTIALIAS_ON) this.enableAntialiasing();
			*/
			int ii = (int)(i), jj = (int)(j);
	    	int color = getColor();
	    	int alpha = this.getAlpha(color);
	    	if (alpha != 255) {
	    		int red = this.getRed(color);
	    		int green = this.getGreen(color);
	    		int blue = this.getBlue(color);
	    		int col1 = getImageRGB(3, 3);
	    		int red1 = this.getRed(col1);
	    		int green1 = this.getGreen(col1);
	    		int blue1 = this.getBlue(col1);
	    		float masking_factor = 1.1f-(alpha)/255.0f;
	    		//if (green1 > 150) masking_factor += 0.15f;
	    		red = (int) (red * (1 - masking_factor) + red1 * masking_factor);
	    		green = (int) (green * (1 - masking_factor) + green1 * masking_factor);
	    		blue = (int) (blue * (1 - masking_factor) + blue1 * masking_factor);
	    		color = red<<16 | green<<8 | blue;
	    	}

			if (i== k && j == l) {
				int[] rec = this.getClip();
				if (this.invertEnabled) {
					if (this.invertH) {
						ii = (int) invertX(ii);
					}
					if (this.invertV) {
						jj = (int) invertY(jj);
					}
				}
				if (ii >= rec[0] && jj >= rec[1] && ii < rec[0]+rec[2] && jj < rec[1]+rec[3]) {
					if (g != null) setImageRGB(ii, jj, color);
					if (g2 != null) setImageRGB2(ii, jj, color);
				}
			} else {
/*				int kk = (int)(k), ll = (int)(l);
				int[] rec = this.getClip();
				if (ii > kk) {
					int tmp = ii;
					ii = kk;
					kk = tmp;
				}
				if (jj > ll) {
					int tmp = jj;
					jj = ll;
					ll = tmp;
				}
				if (kk == rec[0]+rec[2]) kk --;
				if (ii == rec[0]-1) ii ++;
				if (ii >= rec[0] && jj >= rec[1] && kk < rec[0]+rec[2] && ll < rec[1]+rec[3]) {
					int w = kk-ii+1, h = ll-jj+1;
					if (this.invertEnabled) {
						if (this.invertH) {
							ii = (int) invertX(ii)+1-w;
						}
						if (this.invertV) {
							jj = (int) invertY(jj)+1-h;
						}
					}

					if (ii < 0 || ii+w > getWidth()) return;
					if (jj < 0 || jj+h > getHeight()) return;
					int data[] = new int[w*h];
					Arrays.fill(data, color);
					if (g != null) image.setRGB(ii, jj, w, h, data, 0, 0);
					if (g2 != null) image2.setRGB(ii, jj, w, h, data, 0, 0);
				} else {
*/
		    	drawFastLine((int)i, (int)j, (int)k, (int)l, color, this.getClip(), false);
//				}
			}
			return;
		} else {
			if (!externalGraphics) {
		    	int color = getColor();
		    	int alpha = this.getAlpha(color);
		    	if (alpha != 255) {
		    		int red = this.getRed(color);
		    		int green = this.getGreen(color);
		    		int blue = this.getBlue(color);
		    		int col1 = getImageRGB(3, 3);
		    		int red1 = this.getRed(col1);
		    		int green1 = this.getGreen(col1);
		    		int blue1 = this.getBlue(col1);
		    		float masking_factor = 1.2f-(alpha)/255.0f;
		    		if (green1 > 150) masking_factor += 0.15f;
		    		red = (int) (red * (1 - masking_factor) + red1 * masking_factor);
		    		green = (int) (green * (1 - masking_factor) + green1 * masking_factor);
		    		blue = (int) (blue * (1 - masking_factor) + blue1 * masking_factor);
		    		color = red<<16 | green<<8 | blue;
		    	}
				drawFastLine((int)i, (int)j, (int)k, (int)l, color, this.getClip(), false);
			} else {
				drawLine(i, j, k, l, false);
			}
		}
	}

	private void drawLine(float i, float j, float k, float l, Graphics2D g, Graphics2D g2, boolean fast) { //, int clip[]) {
/*		if (i < 0 || k < 0 || i > this.w || k > w || j < 0 || l < 0 || j > h || l > h) {
			int x1 = (int) i, y1 = (int) j, x2 = (int) k, y2 = (int) l;
	    	int cx1 = clip[0], cy1 = clip[1], cx2 = cx1 + clip[2]-1, cy2 = cy1 + clip[3]-1;

			if (this.invertEnabled) {
				if (this.invertH) {
					x1 = (int) invertX(x1);
					x2 = (int) invertX(x2);
				}
				if (this.invertV) {
					y1 = (int) invertY(y1);
					y2 = (int) invertY(y2);
				}
			}

	        // Clip line using Liang-Barsky line clipping
	        u1 = 0;
	        u2 = 1;
	        int dx = x2 - x1;

	        if (clipTest(-dx, x1 - cx1)) {
	            if (clipTest(dx, cx2 - x1)) {
	                int dy = y2 - y1;
	                if (clipTest(-dy, y1 - cy1)) {
	                    if (clipTest(dy, cy2 - y1)) {
	                        if (u2 < 1f) {
	                            x2 = Math.min(Math.max(cx1, x1 + (int) (u2 * dx)), cx2);
	                            y2 = Math.min(Math.max(cy1, y1 + (int) (u2 * dy)), cy2);
	                        }
	                        if (u1 > 0f) {
	                            x1 = Math.min(Math.max(cx1, x1 + (int) (u1 * dx)), cx2);
	                            y1 = Math.min(Math.max(cy1, y1 + (int) (u1 * dy)), cy2);
	                        }
	                    } else return;
	                } else return;
	            } else return;
	        } else return;
	        i = x1;
	        j = y1;
	        k = x2;
	        l = y2;
		} else {

		}
*/

		if (g != null && !externalGraphics && this.isContinuous && lineWidth < 2 && fast) {
	    	int color = getColor();
	    	int alpha = this.getAlpha(color);
	    	if (alpha != 255) {
	    		int red = this.getRed(color);
	    		int green = this.getGreen(color);
	    		int blue = this.getBlue(color);
	    		int col1 = getImageRGB(3, 3);
	    		int red1 = this.getRed(col1);
	    		int green1 = this.getGreen(col1);
	    		int blue1 = this.getBlue(col1);
	    		float masking_factor = 1.2f-(alpha)/255.0f;
	    		if (green1 > 150) masking_factor += 0.15f;
	    		red = (int) (red * (1 - masking_factor) + red1 * masking_factor);
	    		green = (int) (green * (1 - masking_factor) + green1 * masking_factor);
	    		blue = (int) (blue * (1 - masking_factor) + blue1 * masking_factor);
	    		color = red<<16 | green<<8 | blue;
	    	}
	    	this.drawFastLine((int)i, (int)j, (int)k, (int)l, color, this.getClip(), true);
	    	return;
		}
		if (this.invertEnabled) {
			if (this.invertH) {
				i = invertX(i);
				k = invertX(k);
			}
			if (this.invertV) {
				j = invertY(j);
				l = invertY(l);
			}
		}

		if (!externalGraphics && (i == k && j == l)) {
			int ii = (int)(i), jj = (int)(j);
			int[] rec = this.getClip();
			if (ii >= rec[0] && jj >= rec[1] && ii < rec[0]+rec[2] && jj < rec[1]+rec[3]) {
				if (g != null) setImageRGB(ii, jj, getColor());
				if (g2 != null) setImageRGB2(ii, jj, getColor());
			}
			return;
		}

		if (g != null) {
			if (externalGraphics || !this.isContinuous || lineWidth >= 2 || !fast)
				g.drawLine((int)i, (int)j, (int)k, (int)l);
		}
		if (g2 != null) {
			if (externalGraphics) { // Improve quality (sub-pixel rendering) for external graphics
				if (i == k && j == l) {
					g2.translate(i, j);
					g2.fillOval(0, 0, 1, 1);
					g2.translate(-i, -j);					
				} else {
					GeneralPath path = new GeneralPath();
					path.moveTo(i,  j);
					path.lineTo(k, l);
					g2.draw(path);
				}
			} else {
				g2.drawLine((int)i, (int)j, (int)k, (int)l);
			}
		}
	}

	@Override
	public void drawLines(int i[], int j[], int np, boolean fastMode) {
		drawLines(i, j, np, g, g2, fastMode);
	}

	private void drawLines(int i[], int j[], int np, Graphics2D g, Graphics2D g2, boolean fastMode) {
		if (fastMode && !externalGraphics) {
			if (g != null) drawPolyline(i, j, np);
			//if (g2 != null) drawPolyline(i, j, np);
		} else {
			if (this.invertEnabled && (this.invertH || this.invertV)) {
				for (int k=0; k<np; k++) {
					if (this.invertH) {
						i[k] = (int) invertX(i[k]);
					}
					if (this.invertV) {
						j[k] = (int) invertY(j[k]);
					}
				}
			}

			if (g != null) g.drawPolyline(i, j, np);
			//if (g2 != null) g2.drawPolyline(i, j, np);
		}
	}

    /**
     * Draws a line, using the current color, between the points
     * <code>(x1,&nbsp;y1)</code> and <code>(x2,&nbsp;y2)</code>
     * in this graphics context's coordinate system.
     * @param   x1  the first point's <i>x</i> coordinate.
     * @param   y1  the first point's <i>y</i> coordinate.
     * @param   x2  the second point's <i>x</i> coordinate.
     * @param   y2  the second point's <i>y</i> coordinate.
     * @param color The color.
     */
    private void drawFastLine(int x1, int y1, int x2, int y2, int color, int clip[],
    		boolean antialias) {
		if (this.invertEnabled) {
			if (this.invertH) {
				x1 = (int) invertX(x1);
				x2 = (int) invertX(x2);
			}
			if (this.invertV) {
				y1 = (int) invertY(y1);
				y2 = (int) invertY(y2);
			}
		}

        // Clip line using Liang-Barsky line clipping
        u1 = 0;
        u2 = 1;
        int dx = x2 - x1;

        if (dx == 0 && (x1 < 0 || x1 >= w)) return;
        if (clipTest(-dx, x1 - clip[0])) {
        	int cx2 = clip[0] + clip[2]-1;
            if (clipTest(dx, cx2 - x1)) {
                int dy = y2 - y1;
                if (dy == 0 && (y1 < 0 || y1 >= h)) return;
                if (clipTest(-dy, y1 - clip[1])) {
                	int cy2 = clip[1] + clip[3]-1;
                    if (clipTest(dy, cy2 - y1)) {
                        if (u2 < 1f) {
                            x2 = Math.min(Math.max(clip[0], x1 + (int) (u2 * dx)), cx2);
                            y2 = Math.min(Math.max(clip[1], y1 + (int) (u2 * dy)), cy2);
                        }
                        if (u1 > 0f) {
                            x1 = Math.min(Math.max(clip[0], x1 + (int) (u1 * dx)), cx2);
                            y1 = Math.min(Math.max(clip[1], y1 + (int) (u1 * dy)), cy2);
                        }
                    } else return;
                } else return;
            } else return;
        } else return;


        if (antialias) {
        	plotLineWu(x1, y1, x2, y2, color);
        } else {
        	plotLine(x1, y1, x2, y2, color);
        }
    }

    /**
     * LiangBarsky clip test variables.
     */
    float u1, u2;
    /** LiangBarsky clip test.
     * u1 and u2 are var parameters of this function
     */
    private boolean clipTest(int p, float q) {
        float r;
        boolean clipTest = true;
        if (p < 0f) {
            r = q / p;
            if (r > u2) {
                clipTest = false;
            } else if (r > u1) {
                u1 = r;
            }
        } else if (p > 0f) {
            r = q / p;
            if (r < u1) {
                clipTest = false;
            } else if (r < u2) {
                u2 = r;
            }
        } else if (q < 0f) {
            clipTest = false;
        }

        return clipTest;
    }

    /**
     * Draws digital line from (x1,y1) to (x2,y2).
     * Does no clipping.  Uses Bresenham's algorithm.
     */
    private void plotLine(int x1, int y1, int x2, int y2, int color) {
            // Bresenham's algorithm for all other cases
            // This algorithm taken from "Digital Line Drawing" by Paul Heckbert
            // from "Graphics Gems", Academic Press, 1990.
            int dx = x2-x1, ax = Math.abs(dx)<<1, sx = (dx > 0) ? 1 : -1;
            int dy = y2-y1, ay = Math.abs(dy)<<1, sy = (dy > 0) ? 1 : -1;

            int x = x1, y = y1, d;

            // Basic support only for point-like strokes
            int h = 0, hmax = 0;
            boolean continuos = isContinuous;
            if (!isContinuous) {
                JPARSECStroke s = getStroke((BasicStroke)g.getStroke());
            	float da[] = s.getDashArray();
            	if (da[0] < 2) {
            		hmax = (int)(0.5 + da[1] / da[0]);
            	} else {
            		//hmax = -(int)da[0];
            		continuos = true;
            	}
            }

            if (ax>ay) {		/* x dominant */
                d = ay-(ax>>1);
                if (ay == 0 && d < 0 && continuos) {
                	if (sx > 0) {
                		for (int px = x; px <= x2; px ++) {
                			setImageRGB(px, y, color);
                			if (image2 != null && px < x2) setImageRGB2(px, y, color);
                		}
/*                    	int colors[] = new int[x2-x+1];
                    	for (int i=0; i<colors.length; i++) {
                    		colors[i] = color;
                    	}
	                	image.setRGB(x, y, colors.length, 1, colors, 0, w);
	                	if (image2 != null) image2.setRGB(x, y, x2-x, 1, colors, 0, w);
*/
	                	return;
                	} else {
                		for (int px = x2; px <= x; px ++) {
                			setImageRGB(px, y, color);
                			if (image2 != null && px < x) setImageRGB2(px, y, color);
                		}
/*                    	int colors[] = new int[x-x2+1];
                    	for (int i=0; i<colors.length; i++) {
                    		colors[i] = color;
                    	}
	                	image.setRGB(x2, y, colors.length, 1, colors, 0, w);
	                	if (image2 != null) image2.setRGB(x2, y, x-x2, 1, colors, 0, w);
*/
	                	return;
                	}
                }

                for (;;) {
                	if (!continuos) {
	                	h ++;
	                	if (h == hmax) {
	                		setImageRGB(x, y, color);
		                	if (image2 != null) setImageRGB2(x, y, color);
		                	h = 0;
/*	                	} else {
	                		if (hmax < 0) {
	                			if (h < 6) {
	    		                	setImageRGB(x, y, color);
	    		                	if (image2 != null) setImageRGB2(x, y, color);
	                			} else {
	                				h = 0;
	                				for (int i=0; i<6;i++) {
		                                if (x==x2) return;
		                                if (d>=0) {
		                                    y += sy;
		                                    d -= ax;
		                                }
		                                x += sx;
		                                d += ay;
	                				}
	                			}
	                		}
*/	                	}
                	} else {
                		setImageRGB(x, y, color);
	                	if (image2 != null) setImageRGB2(x, y, color);
                	}
                    //pixels[x + y] = color;
                    if (x==x2) return;
                    if (d>=0) {
                        y += sy;
                        d -= ax;
                    }
                    x += sx;
                    d += ay;
                }
            } else {			/* y dominant */
                d = ax-(ay>>1);
                if (ax == 0 && d < 0 && continuos) {
                	if (sy > 0) {
                		for (int py = y; py <= y2; py ++) {
                			setImageRGB(x, py, color);
                			if (image2 != null && py < y2) setImageRGB2(x, py, color);
                		}
/*                    	int colors[] = new int[y2-y+1];
                    	for (int i=0; i<colors.length; i++) {
                    		colors[i] = color;
                    	}
	                	image.setRGB(x, y, 1, colors.length, colors, 0, h);
	                	if (image2 != null) image2.setRGB(x, y, 1, y2-y, colors, 0, h);
*/
	                	return;
                	} else {
                		for (int py = y2; py <= y; py ++) {
                			setImageRGB(x, py, color);
                			if (image2 != null && py < y) setImageRGB2(x, py, color);
                		}
/*                    	int colors[] = new int[y-y2+1];
                    	for (int i=0; i<colors.length; i++) {
                    		colors[i] = color;
                    	}
	                	image.setRGB(x, y2, 1, colors.length, colors, 0, h);
	                	if (image2 != null) image2.setRGB(x, y2, 1, y-y2, colors, 0, h);
*/
	                	return;
                	}
                }

                for (;;) {
                	if (!continuos) {
	                	h ++;
	                	if (h == hmax) {
	                		setImageRGB(x, y, color);
		                	if (image2 != null) setImageRGB2(x, y, color);
	                		h = 0;
	                	}
                	} else {
                		setImageRGB(x, y, color);
	                	if (image2 != null) setImageRGB2(x, y, color);
                	}
                    //pixels[x + y] = color;
                    if (y==y2) return;
                    if (d>=0) {
                        x += sx;
                        d -= ay;
                    }
                    y += sy;
                    d += ax;
                }
            }
    }


    /**
     * Number of intensity bits.
     */
    private final static int INTENSITY_BITS = 8;
    /**
     * Number of alpha levels used for antialiasing.
     */
    //private final static int NUM_LEVELS = 512;
    /** Mask used to flip all bits in an intensity weighting, producing the
     * result (1 - intensity weighting) */
    private static int WEIGHTING_COMPLEMENT_MASK = 511; //NUM_LEVELS - 1;
    protected int premultipliedR;
    protected int premultipliedG;
    protected int premultipliedB;
    protected int previousIn, previousOut, oneMinusAlpha,currentIn;
    /***
     * Wu antialised line algorithm taken from:
     * http://www.codeproject.com/gdi/antialias.asp#dwuln
     *
     * @param x1 start point of line
     * @param y1 start point of line
     * @param x2 end point of line
     * @param y2 end point of line
     */
    protected void plotLineWu(int x1, int y1, int x2, int y2, int color) {
/*    	if (x2 == x1 || y2 == y1) {
    		this.plotLine(x1, y1, x2, y2, color);
    		return;
    	}
*/
    	int alpha = 255; //this.getAlpha(color);
    	//if (alpha == 0) return;
    	WEIGHTING_COMPLEMENT_MASK = 255;
    	if (lineWidth > 1) WEIGHTING_COMPLEMENT_MASK = 511;

        oneMinusAlpha = alpha ^ WEIGHTING_COMPLEMENT_MASK;
        int redBits = (color & 0xff0000);
        int greenBits = (color & 0xff00);
        int blueBits = (color & 0xff);
        premultipliedR = redBits * alpha;
        premultipliedG = greenBits * alpha;
        premultipliedB = blueBits * alpha;
        previousIn = previousOut = color;

        // Make sure the line runs top to bottom
        if (y1 > y2) {
            int temp;
            temp = y1; y1 = y2; y2 = temp;
            temp = x1; x1 = x2; x2 = temp;
        }
        // Draw the initial pixel, which is always exactly intersected by
        // the line and so needs no weighting
        plot(x1, y1);

        int dx = x2 - x1;
        int dy = y2 - y1;

        int sx;
        if (dx >= 0) {
            sx = 1;
        } else {
            sx = -1;
            dx = -dx; // make dx positive
        }

        // Special-case horizontal, vertical, and diagonal lines, which
        // require no weighting because they go right through the center of
        // every pixel
        if (dy == 0) {
            // Horizontal line
            while (dx-- != 0) {
                x1 += sx;
                //blend(xy);
                // BEGIN INLINED blend(xy)
                int p = getImageRGB(x1, y1);
                if (p != previousIn) {
                    previousIn = p;
                    previousOut =
                            ((((p & 0xff0000) * oneMinusAlpha + premultipliedR) / 255) & 0xff0000) |
                            ((((p & 0xff00) * oneMinusAlpha + premultipliedG) / 255) & 0xff00) |
                            ((((p & 0xff) * oneMinusAlpha + premultipliedB) / 255) );
                }
                setImageRGB(x1, y1, previousOut);
                // END INLINED blend(xy)
            }
        } else if (dx == 0) {
            // Vertical line
            do {
                y1 ++;
                //blend(xy);
                // BEGIN INLINED blend(xy)
                int p = getImageRGB(x1, y1);
                if (p != previousIn) {
                    previousIn = p;
                    previousOut =
                            ((((p & 0xff0000) * oneMinusAlpha + premultipliedR) / 255) & 0xff0000) |
                            ((((p & 0xff00) * oneMinusAlpha + premultipliedG) / 255) & 0xff00) |
                            ((((p & 0xff) * oneMinusAlpha + premultipliedB) / 255) );
                }
                setImageRGB(x1, y1, previousOut);
                // END INLINED blend(xy)
            } while (--dy != 0);
        } else if (dx == dy) {
            // Diagonal line
            do {
            	x1 += sx;
            	y1 ++;
                //blend(xy);
                // BEGIN INLINED blend(xy)
                int p = getImageRGB(x1, y1);
                if (p != previousIn) {
                    previousIn = p;
                    previousOut =
                            ((((p & 0xff0000) * oneMinusAlpha + premultipliedR) / 255) & 0xff0000) |
                            ((((p & 0xff00) * oneMinusAlpha + premultipliedG) / 255) & 0xff00) |
                            ((((p & 0xff) * oneMinusAlpha + premultipliedB) / 255) );
                }
                setImageRGB(x1, y1, previousOut);
            	if (image2 != null) setImageRGB2(x1, y1, previousOut);
                // END INLINED blend(xy)
            } while (--dy != 0);
        } else {

            // Line is not horizontal, diagonal, or vertical
            // ---------------------------------------------


            int errorAcc = 0;  // initialize the line error accumulator to 0

            // # of bits by which to shift errorAcc to get intensity level
            int intensityShift = 0xffff & (16 - INTENSITY_BITS);


            /* Is this an X-major or Y-major line? */
            if (dy > dx) {
                // Y-major line; calculate 16-bit fixed-point fractional part of a
                // pixel that X advances each time Y advances 1 pixel, truncating the
                // result so that we won't overrun the endpoint along the X axis
                int errorAdj = 0xffff & ((dx << 16) / dy);

                int y = y1; // *= w;

                // Draw all pixels other than the first and last
                while (--dy != 0) {
                    int errorAccTemp = errorAcc;   // remember currrent accumulated error
                    errorAcc = 0xffff & (errorAcc + errorAdj);      // calculate error for next pixel
                    if (errorAcc <= errorAccTemp) {
                        // The error accumulator turned over, so advance the X coord
                        x1 += sx;
                    }
                    y ++; // Y-major, so always advance Y


                    // The INTENSITY_BITS most significant bits of errorAcc give us the
                    // intensity weighting for this pixel, and the complement of the
                    // weighting for the paired pixel */
                    //int intensity = errorAcc >> intensityShift;
                    //blend(x1 + sx + y, intensity);
                    //blend(x1+y, intensity ^ WEIGHTING_COMPLEMENT_MASK);

                    int intensity = errorAcc >> intensityShift;

                    // BEGIN INLINED blend(xy, intensity)
                    if (intensity != 0) {
                        int p = getImageRGB(x1 + sx, y);
                        int a = (intensity * alpha) / 255;
                        int oneMinusA = a ^ WEIGHTING_COMPLEMENT_MASK;

                        setImageRGB(x1 + sx, y,
                                ((((p & 0xff0000) * oneMinusA + redBits * a) / 255) & 0xff0000) |
                                ((((p & 0xff00) * oneMinusA + greenBits * a) / 255) & 0xff00) |
                                ((((p & 0xff) * oneMinusA + blueBits * a) / 255) ) );
                    	if (image2 != null) setImageRGB2(x1 + sx, y, getImageRGB(x1 + sx, y));
                    }
                    // END INLINED blend(xy)

                    intensity ^= WEIGHTING_COMPLEMENT_MASK;

                    // BEGIN INLINED blend(xy, intensity)
                    if (intensity != 0) {
                        int p = getImageRGB(x1, y);
                        int a = (intensity * alpha) / 255;
                        int oneMinusA = a ^ WEIGHTING_COMPLEMENT_MASK;

                        setImageRGB(x1, y,
                                ((((p & 0xff0000) * oneMinusA + redBits * a) / 255) & 0xff0000) |
                                ((((p & 0xff00) * oneMinusA + greenBits * a) / 255) & 0xff00) |
                                ((((p & 0xff) * oneMinusA + blueBits * a) / 255) ) );
                    	if (image2 != null) setImageRGB2(x1, y, getImageRGB(x1, y));
                    }
                    // END INLINED blend(xy)
                }
            } else {
                // It's an X-major line; calculate 16-bit fixed-point fractional part of a
                // pixel that Y advances each time X advances 1 pixel, truncating the
                // result to avoid overrunning the endpoint along the X axis
                int errorAdj = 0xffff & ((dy << 16) / dx);

                int y = y1; // *= w;

                // Draw all pixels other than the first and last
                while (--dx != 0) {
                    int errorAccTemp = errorAcc;   // remember currrent accumulated error
                    errorAcc = 0xffff & (errorAcc + errorAdj);      // calculate error for next pixel
                    if (errorAcc <= errorAccTemp) {
                        // The error accumulator turned over, so advance the Y coord
                        y ++ ;
                    }
                    x1 += sx; // X-major, so always advance X

                    // The INTENSITY_BITS most significant bits of errorAcc give us the
                    // intensity weighting for this pixel, and the complement of the
                    // weighting for the paired pixel
                    //int intensity = errorAcc >> intensityShift;
                    //blend(x1+y, intensity ^ WEIGHTING_COMPLEMENT_MASK);
                    //blend(x1+y+gw, intensity);

                    int intensity = errorAcc >> intensityShift;

                    // BEGIN INLINED blend(xy, intensity)
                    if (intensity != 0) {
                        int p = getImageRGB(x1, y + 1);
                        int a = (intensity * alpha) / 255;
                        int oneMinusA = a ^ WEIGHTING_COMPLEMENT_MASK;

                        setImageRGB(x1, y + 1,
                                ((((p & 0xff0000) * oneMinusA + redBits * a) / 255) & 0xff0000) |
                                ((((p & 0xff00) * oneMinusA + greenBits * a) / 255) & 0xff00) |
                                ((((p & 0xff) * oneMinusA + blueBits * a) / 255) ) );
                    	if (image2 != null) setImageRGB2(x1, y + 1, getImageRGB(x1, y + 1));
                    }
                    // END INLINED blend(xy)

                    intensity ^= WEIGHTING_COMPLEMENT_MASK;

                    // BEGIN INLINED blend(xy, intensity)
                    if (intensity != 0) {
                        int p = getImageRGB(x1, y);
                        int a = (intensity * alpha) / 255;
                        int oneMinusA = a ^ WEIGHTING_COMPLEMENT_MASK;

                        setImageRGB(x1, y,
                                ((((p & 0xff0000) * oneMinusA + redBits * a) / 255) & 0xff0000) |
                                ((((p & 0xff00) * oneMinusA + greenBits * a) / 255) & 0xff00) |
                                ((((p & 0xff) * oneMinusA + blueBits * a) / 255) ));
                    	if (image2 != null) setImageRGB2(x1, y, getImageRGB(x1, y));
                    }
                    // END INLINED blend(xy)
                }
            }

            // Draw the final pixel, which is always exactly intersected by the line
            // and so needs no weighting
            plot(x2, y2);
        }
    }

    private int getImageRGB(int x, int y) {
    	if (x+tx < 0 || x+tx >= image.getWidth() || y+ty < 0 || y+ty >= image.getHeight()) return 0;
    	if (tx == 0 && ty == 0) {
        	if (rasterData == null) return image.getRGB(x, y);
        	return (255<<24) | rasterData[x + y * image.getWidth()];
    	}
    	if (rasterData == null) return image.getRGB(x + (int) tx, y + (int) ty);
    	return (255<<24) | rasterData[x + (int) tx + (y + (int) ty) * image.getWidth()];
    }

    private void setImageRGB(int x, int y, int color) {
    	if (x+tx < 0 || x+tx >= image.getWidth() || y+ty < 0 || y+ty >= image.getHeight()) return;
    	if (tx == 0 && ty == 0) {
        	if (rasterData == null) {
        		image.setRGB(x, y, color);
        	} else {
            	rasterData[x + y * image.getWidth()] = color;
        	}
        	return;
    	}
    	if (rasterData == null) {
    		image.setRGB(x + (int) tx, y + (int) ty, color);
    	} else {
        	rasterData[x + (int) tx + (y + (int) ty) * image.getWidth()] = color;
    	}
    }

    private void setImageRGB2(int x, int y, int color) {
    	if (image2 == null) return;
    	if (x+tx < 0 || x+tx >= image2.getWidth() || y+ty < 0 || y+ty >= image2.getHeight()) return;
    	if (tx == 0 && ty == 0) {
        	if (rasterData == null) {
        		image2.setRGB(x, y, color);
        	} else {
            	rasterData2[x + y * image2.getWidth()] = color;
        	}
        	return;
    	}
    	if (rasterData2 == null) {
    		image2.setRGB(x + (int) tx, y + (int) ty, color);
    	} else {
        	rasterData2[x + (int) tx + (y + (int) ty) * image2.getWidth()] = color;
    	}
    }

    /**
     * Plots a single pixel.
     */
    protected void plot(int x, int y) {
    	if (x+tx < 0 || x+tx >= image.getWidth() || y+ty < 0 || y+ty >= image.getHeight()) return;
        int p = getImageRGB(x,  y);
        if (p != previousIn) {
            previousIn = p;
            previousOut =
                    ((((p & 0xff0000) * oneMinusAlpha + premultipliedR) / 255) & 0xff0000) |
                    ((((p & 0xff00) * oneMinusAlpha + premultipliedG) / 255) & 0xff00) |
                    ((((p & 0xff) * oneMinusAlpha + premultipliedB) / 255) );
        }
        setImageRGB(x, y, previousOut);
    	if (image2 != null) setImageRGB2(x, y, previousOut);
    }

/*
    private int cx1, cx2, cy1, cy2, color;
    private int redBits = (color & 0xff0000);
    private int greenBits = (color & 0xff00);
    private int blueBits = (color & 0xff);
    private void fastFill(Shape s, int c, int clip[]) {
    	if (colorBar == null) colorBar = new int[w];
        java.awt.Rectangle b = s.getBounds();

        int x1 = b.x;
        int y1 = b.y;
        int x2 = x1 + b.width - 1;
        int y2 = y1 + b.height - 1;
        cx1 = clip[0];
        cx2 = clip[0] + clip[2];
        cy1 = clip[1];
        cy2 = clip[1] + clip[3];
        color = c;
        redBits = (color & 0xff0000);
        greenBits = (color & 0xff00);
        blueBits = (color & 0xff);

        if (x1 > cx2 || y1 > cy2 || x2 < cx1 || y2 < cy1) {
            return;
        }

        scanFillHorizontal(s, b, c);
    }
    // ColorBar is used to speed up scan fill operations.
    protected int[] colorBar;
    //Is filled with a list of edges for each scan line.
    //The list of edges on an indivdiual scan line is sorted from left to right.
    protected Edge[] edges;
    // Flatness is used for flattening general paths.
    protected final static double flatness = 0.33333d;
    // Translation on X- and Y-axis.
    protected int tx = 0, ty = 0;
    // A faster array fill implementation than the one found in java.util.Arrays.
    protected final static void arrayfill(int[] array, int from, int to, int value) {
        int len = to - from + 1;
        if (len > 0)
            array[from] = value;
        for (int i = 1; i < len; i += i) {
            System.arraycopy(array, from, array, from + i,
                    ((len - i) < i) ? (len - i) : i);
        }
    }
    protected void buildEdgeListHorizontal(int[] x, int[] y, int cnt) {
        Edge edge;
        int v1, v2;
        int yPrev;
        int i;

        yPrev = y[cnt - 2];
        for (v1 = cnt - 1; v1 >= 1; v1--) {
            yPrev = y[v1 - 1];
            if (yPrev != y[v1]) break;
        }

        for (i = 0; i < cnt; i++) {
            v2 = i;
            if (y[v1] != y[v2]) {
                // we only add nonhorizontal lines to the edge list
                edge = new Edge();
                if (y[v1] < y[v2]) {
                    makeEdgeRecHorizontal(v1, v2, yNext(i, x, y, cnt), edge, x, y);
                } else {
                    makeEdgeRecHorizontal(v2, v1, yPrev, edge, x, y);
                }
                yPrev = y[v1];
            }
            v1 = v2;
        }
    }
    protected void buildEdgeListVertical(int[] x, int[] y, int cnt) {
        Edge edge;
        int v1, v2;
        int xPrev;
        int i;

        xPrev = x[cnt - 2];
        for (v1 = cnt - 1; v1 >= 1; v1--) {
            xPrev = x[v1 - 1];
            if (xPrev != x[v1]) break;
        }

        for (i = 0; i < cnt; i++) {
            v2 = i;
            if (x[v1] != x[v2]) {
                // we only add nonvertical lines to the edge list
                edge = new Edge();
                if (x[v1] < x[v2]) {
                    makeEdgeRecVertical(v1, v2, xNext(i, x, y, cnt), edge, x, y);
                } else {
                    makeEdgeRecVertical(v2, v1, xPrev, edge, x, y);
                }
                xPrev = x[v1];
            }
            v1 = v2;
        }
    }
    protected void makeEdgeRecHorizontal(int lower, int upper, int yComp, Edge edge, int[] x, int[] y) {
        edge.dPerScan = (x[upper] - x[lower]) / (float) (y[upper] - y[lower]);
        edge.intersect = x[lower];

        if (y[lower] < cy1) {
            edge.intersect += edge.dPerScan * (cy1 - y[lower]);
        }
        if (y[upper] < yComp) {
            // On a monotonically increasing or decreasing edge boundary,
            // shorten the edge by one scan line
            edge.upper = y[upper] - 1;
        } else {
            edge.upper = y[upper];
        }
        if (y[lower] <= cy2 && edge.upper >= cy1) {
            //insertEdge(edges[y[lower]], edge);
            insertEdge(edges[Math.max(cy1, y[lower])], edge);
        }
    }
    // given an index, returns y coordinate of next nonhorizontal line
    protected int yNext(int k, int[] x, int[] y, int cnt) {
        int j;
        if (k + 1 >= cnt) {
            j = 0;
        } else {
            j = k + 1;
        }
        while (y[k] == y[j]) {
            if (j + 1 >= cnt) {
                j = 0;
            } else {
                j++;
            }
        }
        return y[j];
    }
    // given an index, returns x coordinate of next nonvertical line
    protected int xNext(int k, int[] x, int[] y, int cnt) {
        int j;
        if (k + 1 >= cnt) {
            j = 0;
        } else {
            j = k + 1;
        }
        while (x[k] == x[j]) {
            if (j + 1 >= cnt) {
                j = 0;
            } else {
                j++;
            }
        }
        return x[j];
    }
    // Inserts edge into list in order of increasing edge.intersect
    protected void insertEdge(Edge list, Edge edge) {
        Edge p, q;
        q = list;
        p = q.next;
        while (p != null) {
            if (edge.intersect < p.intersect) {
                p = null;
            } else {
                q = p;
                p = p.next;
            }
        }
        edge.next = q.next;
        q.next = edge;
    }
    protected void makeEdgeRecVertical(int lower, int upper, int xComp, Edge edge, int[] x, int[] y) {
        edge.dPerScan = (y[upper] - y[lower]) / (float) (x[upper] - x[lower]);
        edge.intersect = y[lower];

        if (x[lower] < cx1) {
            edge.intersect += edge.dPerScan * (cx1 - x[lower]);
        }
        if (x[upper] < xComp) {
            // On a monotonically increasing or decreasing edge boundary,
            // shorten the edge by one scan line
            edge.upper = x[upper] - 1;
        } else {
            edge.upper = x[upper];
        }
        if (x[lower] <= cx2 && edge.upper >= cx1) {
            //insertEdge(edges[y[lower]], edge);
            insertEdge(edges[Math.max(cx1, x[lower])], edge);
        }
    }
    protected void buildActiveList(int scan, Edge active) {
        Edge p, q;
        p = edges[scan].next;
        while (p != null) {
            q = p.next;
            insertEdge(active, p);
            p = q;
        }
    }
    protected void updateActiveList(int scan, Edge active) {
        Edge p, q;

        q = active;
        p = active.next;
        while (p != null) {
            if (scan >= p.upper) {
                p = p.next;
                deleteAfter(q);
            } else {
                p.intersect += p.dPerScan;
                q = p;
                p = p.next;
            }
        }
    }
    protected void prepareEdgeList(int scan) {
        Edge p, q;
        q = edges[scan];
        p = q.next;
        while (p != null) {
            if (scan >= p.upper) {
                p = p.next;
                deleteAfter(q);
            } else {
                q = p;
                p = p.next;
            }
        }
    }
    protected void deleteAfter(Edge q) {
        Edge p;
        p = q.next;
        q.next = p.next;
    }
    protected void resortActiveList(Edge active) {
        Edge p, q;
        p = active.next;
        active.next = null;
        while (p != null) {
            q = p.next;
            insertEdge(active, p);
            p = q;
        }
    }
    // Scan fills a shape.
    private void scanFillHorizontal(Shape s, java.awt.Rectangle translatedShapeBounds, int color) {
        if (colorBar[0] != color) {
            // arrayfill is faster than a for loop
            arrayfill(colorBar, 0, w - 1, color);
        }
        // Initialize edge list with dummy nodes for each scan line.
        // We need the dummy nodes as anchors for the edge lists.
        if (edges == null) {
            int n = Math.max(h, w);
            edges = new Edge[n];
            for (int i=0; i < n; i++) {
                edges[i] = new Edge();
            }
        }

        // Build the edge list
        int[] x = new int[100];
        int[] y = new int[100];
        int closeX = 0;
        int closeY = 0;
        int count = 0;

        float[] coords = new float[6];
        boolean didClose = true;
        for (PathIterator i = s.getPathIterator(null, flatness); ! i.isDone(); i.next()) {
            switch (i.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO :
                    if (! didClose) {
                        x[count] = closeX + tx;
                        y[count] = closeY + ty;
                        count++;
                       buildEdgeListHorizontal(x, y, count);
                    }
                    count = 0;
                    x[count] = closeX = (int) coords[0] + tx;
                    y[count] = closeY = (int) coords[1] + ty;
                    didClose = false;
                    count++;
                    break;
                case PathIterator.SEG_LINETO :
                    x[count] = (int) coords[0] + tx;
                    y[count] = (int) coords[1] + ty;
                    count++;
                    break;
                case PathIterator.SEG_CLOSE :
                    x[count] = closeX + tx;
                    y[count] = closeY + ty;
                    count++;
                    buildEdgeListHorizontal(x, y, count);
                    count = 0;
                    didClose = true;
                    break;
            }
            // Grow point array if necessary
            if (count >= x.length) {
                int[] tmp = x;
                x = new int[x.length * 2];
                System.arraycopy(tmp, 0, x, 0, tmp.length);
                tmp = y;
                y = new int[y.length * 2];
                System.arraycopy(tmp, 0, y, 0, tmp.length);
            }
        }
        if (! didClose) {
            x[count] = closeX;
            y[count] = closeY;
            count++;
            buildEdgeListHorizontal(x, y, count);
        }


        // Initialize active list with dummy node.
        // We need the dummy node as anchor for the active list.
        Edge active = new Edge();
        int miny = Math.max(cy1, translatedShapeBounds.y);
        int maxy = Math.min(cy2, translatedShapeBounds.y + translatedShapeBounds.height - 1);

        if (translatedShapeBounds.x >= cx1 && translatedShapeBounds.x + translatedShapeBounds.width - 1 <= cx2) {
            for (int scan = miny; scan <= maxy; scan++) {
                buildActiveList(scan, active);
                plotH(scan, active);
                updateActiveList(scan, active);
                resortActiveList(active);
                edges[scan].next = null;
            }
        } else {
            for (int scan = miny; scan <= maxy; scan++) {
                buildActiveList(scan, active);
                plotHClipped(scan, active);
                updateActiveList(scan, active);
                resortActiveList(active);
                edges[scan].next = null;
            }
        }
    }
    //Blends an individual pixel at the specified coordinate with
    //the specified alpha transparency.
    //This operation does no clipping.
    protected void blend(int x, int y, int a) {
        if (a != 0) {
            int p = image.getRGB(x, y);
            //int a = (intensity * alpha) / 255;
            int oneMinusA = a ^ WEIGHTING_COMPLEMENT_MASK;

            setImageRGB(x, y,
                    ((((p & 0xff0000) * oneMinusA + redBits * a) / 255) & 0xff0000) |
                    ((((p & 0xff00) * oneMinusA + greenBits * a) / 255) & 0xff00) |
                    ((((p & 0xff) * oneMinusA + blueBits * a) / 255) ) );
        }
    }
    //ScanFill algorithm for shapes which are wider than tall.
    protected void scanFillHorizontal(int[] x, int[] y, int cnt, int miny, int maxy) {
        if (colorBar[0] != color) {
            // arrayfill is faster than a for loop
            arrayfill(colorBar, 0, w - 1, color);
        }
        // Initialize edge list with dummy nodes for each scan line.
        // We need the dummy nodes as anchors for the edge lists.
        if (edges == null) {
            int n = Math.max(h, w);
            edges = new Edge[n];
            for (int i=0; i < n; i++) {
                edges[i] = new Edge();
            }
        }

        // Initialize active list with dummy node.
        // We need the dummy node as anchor for the active list.
        Edge active = new Edge();

        // Build the edge list
        buildEdgeListHorizontal(x, y, cnt);

        for (int scan = miny; scan <= maxy; scan++) {
            buildActiveList(scan, active);
            plotHClipped(scan, active);
            updateActiveList(scan, active);
            resortActiveList(active);
            edges[scan].next = null;
        }
    }
    //ScanFill algorithm for shapes which are taller than wide.
    protected void scanFillVertical(int[] x, int[] y, int cnt, int minx, int maxx) {
        // Initialize edge list with dummy nodes for each scan line.
        // We need the dummy nodes as anchors for the edge lists.
        if (edges == null) {
            int n = Math.max(h, w);
            edges = new Edge[n];
            for (int i=0; i < n; i++) {
                edges[i] = new Edge();
            }
        }

        // Initialize active list with dummy node.
        // We need the dummy node as anchor for the active list.
        Edge active = new Edge();

        // Build the edge list
        buildEdgeListVertical(x, y, cnt);

        for (int scan = minx; scan <= maxx; scan++) {
            buildActiveList(scan, active);
            plotVClipped(scan, active);
            updateActiveList(scan, active);
            resortActiveList(active);
            edges[scan].next = null;
        }
    }
    // Fills a scan line between pairs of edges in the active edge list.
    // Uses a brute force algorithm to perform antialiasing.
    protected void plotHClipped(int scan, Edge active) {
        Edge p1, p2;
        p1 = active.next;
        while (p1 != null) {
            p2 = p1.next;
            int x1 = (int) (p1.intersect);
            int x2 = (int) p2.intersect;

            if (x1 <= cx2 && x2 >= cx1) {
                if (p1.intersect + p1.dPerScan > p2.intersect + p2.dPerScan &&
                        p2.intersect - p1.intersect >= 1f) {
                    // The two edges cross in the middle of the current scan line
                    int left = (int) Math.min(x1, x2 + p2.dPerScan);
                    int right = (int) Math.max(x1 + p1.dPerScan, x2) + 1;
                    float p1y = (left + 0.5f - p1.intersect) / p1.dPerScan;
                    float p2y = (left + 0.5f - p2.intersect) / p2.dPerScan;
                    float p1dPerPixel = 1f / p1.dPerScan;
                    float p2dPerPixel = 1f / p2.dPerScan;
                    for (int x=left, n = Math.min(right, cx2); x <= n; x++) {
                        if (x >= cx1) {
                            if (p1y < p2y) {
                                blend(x, scan,
                                        Math.max(0,
                                        Math.max(0, (int) (p1y * 255f)) +
                                        255 - Math.min(255, (int) (p2y * 255f))
                                        )
                                        );
                            } else {
                                blend(x, scan,
                                        Math.max(0,
                                        255 - Math.min(255, (int) (p1y * 255f)) +
                                        Math.max(0, (int) (p2y * 255f))
                                        )
                                        );
                            }
                        }
                        p1y += p1dPerPixel;
                        p2y += p2dPerPixel;
                    }
                } else if (p1.intersect + p1.dPerScan >= p2.intersect ||
                        p2.intersect + p2.dPerScan <= p1.intersect) {
                    // The two edges do not cross in the current scan line, but
                    // the polygon is less than a one pixel wide in the
                    // current scan line.
                    int left = (int) Math.min(x1, x2 + p2.dPerScan);
                    int right = (int) Math.max(x1 + p1.dPerScan, x2) + 1;
                    float p1y = (left + 0.5f - p1.intersect) / p1.dPerScan;
                    float p2y = (left + 0.5f - p2.intersect) / p2.dPerScan;
                    float p1dPerPixel = 1f / p1.dPerScan;
                    float p2dPerPixel = 1f / p2.dPerScan;
                    for (int x=left, n = Math.min(right, cx2); x <= n; x++) {
                        if (x >= cx1) {
                            int p1i = Math.max(0, Math.min(255, (int) (p1y * 255f)));
                            int p2i = Math.max(0, Math.min(255, (int) (p2y * 255f)));

                            blend(x, scan,
                                    Math.abs(p1i - p2i)
                                    );

                        }
                        p1y += p1dPerPixel;
                        p2y += p2dPerPixel;
                    }
                } else {
                    // The two edges do not cross in the current scan line, and
                    // the polygon is at least one pixel wide in the current
                    // scan line

                    if (p1.dPerScan == 0f || p1.dPerScan == 1f || p1.dPerScan == -1f) {
                        // no antialiasing needed
                    } else if (p1.dPerScan < 0f) {
                        // Fractional part of the intersection at pixel x1
                        float frac = p1.intersect - (float) Math.floor(p1.intersect);

                        if (p1.dPerScan + frac >= 0f) {
                            // The edge leaves the scan line at the bottom of the
                            // pixel at x1. We only need to do alpha blending for
                            // the point at x1.

                            //  +----+---*+   * = edge at x1
                            //  |    |  /x|
                            //  |    | /xx|
                            //  +----+*---+   * = edge at x1 + p1.dPerScan
                            if (x1 >= cx1) {
                                blend(x1, scan,
                                        255 - (int) ((frac + frac + p1.dPerScan) /2f * 255f)
                                        );
                            }
                        } else {
                            // The edge leaves the scan line at the left of the
                            // pixel at x1. We need to do alpha blending for
                            // the point at x1 and to points on its left.

                            //  +----+--*-+   * = edge at x1
                            //  |    |/xxx|
                            //  |   /|xxxx|
                            //  +--*-+----+   * = edge at x1 + p1.dPerScan
                            if (x1 >= cx1) {
                                blend(x1, scan,
                                        255 - (int) (frac * frac / -p1.dPerScan / 2f * 255f)
                                        );
                            }


                            // Plot points to the left of x1
                            float dPerPixel = 1f / p1.dPerScan;

                            // y-value of the edge at the center of the pixel.
                            // The y-value is expressed as a fraction of the height
                            // of the pixel, measured from the bottom of the pixel.
                            float yInPixel = 1f + (0.5f + frac) * dPerPixel;
                            int x = x1-1;
                            for (int i=0, n = (int) -p1.dPerScan + 1; i < n; i++) {
                                if (x < cx1) break;
                                if (yInPixel < 0) {
                                    //  +----+  The edge enters at the right side of
                                    //  |    |  the pixel and leaves at the bottom.
                                    //  |   x*
                                    //  |  xx|
                                    //  +-*--+
                                    break;
                                } else {
                                    //  +----+  The edge enters at the right side of
                                    //  |   x*  the pixel and leaves at the left side.
                                    //  | xxx|  or at the bottom.
                                    //  *xxxx|  For this case, we can use the yInPixel as
                                    //  +----+  an approximation of the intensity value.
                                    //          The approximation is accurate, if
                                    //          the edge leaves at the left side.
                                    blend(x, scan, (int) (255f * yInPixel));
                                }
                                yInPixel += dPerPixel;
                                x--;
                            }
                        }
                        x1++;

                    } else {
                        // Fractional part of the intersection at pixel x1
                        float frac = p1.intersect - (float) Math.floor(p1.intersect);

                        if (p1.dPerScan + frac <= 1f) {
                            // The edge leaves the scan line at the bottom of the
                            // pixel at x1. We only need to do alpha blending for
                            // the point at x1.
                            //  +*---+   * = edge at x1
                            //  |x\  |
                            //  |xx\ |
                            //  +--*-+   * = edge + p1.dPerScan
                            if (x1 >= cx1) {
                                blend(x1, scan,
                                        (int) ((1f - frac + 1f - frac - p1.dPerScan) / 2f * 255f)
                                        );
                            }
                            x1++;
                        } else {
                            //  +--*-+----+   * = edge at x1
                            //  |xxx\|    |
                            //  |xxxx|\   |
                            //  +----+-*--+   * = edge + p1.dPerScan
                            if (x1 >= cx1) {
                                blend(x1, scan,
                                        (int) ((1f - frac) * (1f - frac) / p1.dPerScan / 2f * 255f)
                                        );
                            }
                            x1++;
                            // Plot points to the right of x1
                            //  +----+  We only consider the case, where the edge
                            //  |xxxx|  enters at the left side of the pixel and leaves
                            //  *xxxx|  at the right side. For this case,
                            //  | xxx|  we can compute the relative y position at the
                            //  |   x*  center of the pixel, and use that as the intensity
                            //  |    |  value.
                            //  +----+  This gives accurate results except for the right
                            //          most pixel, on which the edge leaves at the bottom
                            //          of the pixel. For this pixel we should compute
                            //          the size of the triangle entering at the right
                            //          of the pixel and leaving at its bottom.
                            float dPerPixel = 1f / p1.dPerScan;
                            float yInPixel = (1.5f - frac) * dPerPixel;
                            for (int i=0, n = (int) p1.dPerScan + 1; i < n; i++) {
                                if (yInPixel >= 1f || x1 > cx2) break;
                                if (x1 >= cx1) {
                                    blend(x1, scan, (int) (255f * yInPixel));
                                }
                                x1++;
                                yInPixel += dPerPixel;
                            }
                        }
                    }
                    //------
                    if (p2.dPerScan == 0f || p2.dPerScan == 1f || p2.dPerScan == -1f) {
                        // no antialiasing needed
                    } else if (p2.dPerScan < 0f) {
                        // Fractional part of the intersection at pixel x2
                        float frac = (float) (p2.intersect - Math.floor(p2.intersect));

                        if (p2.dPerScan + frac >= 0f) {
                            // The edge leaves the scan line at the bottom of the
                            // pixel at x2. We only need to do alpha blending for
                            // the point at x2.

                            //  +---*+   * = edge at x2
                            //  |xx/ |
                            //  |x/  |
                            //  +*---+   * = edge at x2 + p2.dPerScan
                            if (x2 <= cx2) {
                                blend(x2, scan,
                                        (int) ((frac + frac + p2.dPerScan) /2f * 255f)
                                        );
                            }
                            x2--;
                        } else {
                            //  +----+--*-+   * = edge at x2
                            //  |xxxx|/   |
                            //  |xxx/|    |
                            //  +--*-+----+   * = edge at x2 + p2.dPerScan

                            if (x2 <= cx2) {
                                blend(x2, scan,
                                        (int) (frac * frac / -p2.dPerScan / 2f * 255f)
                                        );
                            }
                            x2--;
                            //}

                            // Plot points to the left of x2
                            float dPerPixel = 1f / p2.dPerScan;
                            float yInPixel = (0.5f + frac) * -dPerPixel;

                            for (int i=0, n = (int) -p2.dPerScan + 1; i < n; i++) {
                                if (x2 < cx1) break;
                                if (yInPixel >= 1f) {
                                    //  +----+  The edge enters at the right side of
                                    //  |xxxx|  the pixel and leaves at the bottom.
                                    //  |xxxx*
                                    //  |xxx |
                                    //  +-*--+
                                    break;
                                } else {
                                    //  +----+  The edge enters at the right side of
                                    //  |xxxx*  the pixel and leaves at the left side.
                                    //  |xx  |  or at the bottom.
                                    //  *    |  For this case, we can use the yInPixel as
                                    //  +----+  an approximation of the intensity value.
                                    //          The approximation is accurate, if
                                    //          the edge leaves at the left side.
                                    if (x2 <= cx2) {
                                        blend(x2, scan, (int) (255f * yInPixel));
                                        //pixels[x2+scan*gw] = 0xff0000;
                                    }
                                }
                                yInPixel -= dPerPixel;
                                x2--;
                            }
                        }
                    } else {
                        float frac = p2.intersect - (float) Math.floor(p2.intersect);

                        if (p2.dPerScan + frac <= 1f) {
                            // The edge leaves the scan line at the bottom of the
                            // pixel at x2. We only need to do alpha blending for
                            // the point at x2.

                            //  +*---+   * = edge at x2
                            //  |x\  |
                            //  |xx\ |
                            //  +---*+   * = edge at x2 + p2.dPerScan
                            if (x2 <= cx2) {
                                blend(x2, scan,
                                        (int) ((frac + frac + p2.dPerScan) /2f * 255f)
                                        );
                            }
                            x2--;
                        } else {
                            //  +--*-+----+   * = edge at x2
                            //  |xxx\|    |
                            //  |xxxx|\   |
                            //  +----+-x--+   * = edge at x2 + p2.dPerScan
                            if (x2 <= cx2) {
                                blend(x2, scan,
                                        255 - (int) (1f - frac + (1f - frac) / p2.dPerScan / 2f * 255f)
                                        );
                            }

                            // Plot points to the right of x2
                            //  +----+  We only consider the case, where the edge
                            //  |    |  enters at the left side of the pixel and leaves
                            //  *x   |  at the right side. For this case,
                            //  |xxx |  we can compute the relative y position at the
                            //  |xxxx*  center of the pixel, and use that as the intensity
                            //  |xxxx|  value.
                            //  +----+  This gives accurate results except for the right
                            //          most pixel, on which the edge leaves at the bottom
                            //          of the pixel. For this pixel we should compute
                            //          the size of the triangle entering at the right
                            //          of the pixel and leaving at its bottom.
                            float dPerPixel = 1f / p2.dPerScan;
                            float yInPixel = (1.5f - frac) * dPerPixel;
                            int x = x2 + 1;
                            for (int i=0, n = (int) p2.dPerScan + 1; i < n; i++) {
                                if (yInPixel >= 1f || x > cx2) break;
                                if (x >= cx1) {
                                    blend(x, scan, 255 - (int) (255f * yInPixel));
                                }
                                x++;
                                yInPixel += dPerPixel;
                            }

                            x2--;
                        }
                    }

                    // Arraycopy from color bar
                    if (x2 >= x1) {
                        try {
                        	this.plotLine(Math.max(cx1, x1), scan, Math.min(cx2, x2)+1, scan, colorBar[0]);
                        	//System.arraycopy(colorBar, 0, pixels, start, Math.min(cx2, x2)+scan*w - start + 1);
                        } catch (IndexOutOfBoundsException e) {
                            //System.out.println(pixels.length+" "+start+".."+(start + Math.min(cx2, x2)+scan*w - start + 1));
                        }
                    }
                }
            }
            // advance to next pair of intersections
            p1 = p2.next;
        }
    }
    // Fills a scan line between pairs of edges in the active edge list.
    // Uses a brute force algorithm to perform antialiasing.
    protected void plotH(int scan, Edge active) {
        // XXX - Remove clipping from this method
        Edge p1, p2;
        p1 = active.next;
        while (p1 != null) {
            p2 = p1.next;
            int x1 = (int) (p1.intersect);
            int x2 = (int) p2.intersect;

            if (x1 <= cx2 && x2 >= cx1) {
                if (p1.intersect + p1.dPerScan > p2.intersect + p2.dPerScan &&
                        p2.intersect - p1.intersect >= 1f) {
                    // The two edges cross in the middle of the current scan line
                    int left = (int) Math.min(x1, x2 + p2.dPerScan);
                    int right = (int) Math.max(x1 + p1.dPerScan, x2) + 1;
                    float p1y = (left + 0.5f - p1.intersect) / p1.dPerScan;
                    float p2y = (left + 0.5f - p2.intersect) / p2.dPerScan;
                    float p1dPerPixel = 1f / p1.dPerScan;
                    float p2dPerPixel = 1f / p2.dPerScan;
                    for (int x=left, n = Math.min(right, cx2); x <= n; x++) {
                        if (x >= cx1) {
                            if (p1y < p2y) {
                                blend(x, scan,
                                        Math.max(0,
                                        Math.max(0, (int) (p1y * 255f)) +
                                        255 - Math.min(255, (int) (p2y * 255f))
                                        )
                                        );
                            } else {
                                blend(x, scan,
                                        Math.max(0,
                                        255 - Math.min(255, (int) (p1y * 255f)) +
                                        Math.max(0, (int) (p2y * 255f))
                                        )
                                        );
                            }
                        }
                        p1y += p1dPerPixel;
                        p2y += p2dPerPixel;
                    }
                } else if (p1.intersect + p1.dPerScan >= p2.intersect ||
                        p2.intersect + p2.dPerScan <= p1.intersect) {
                    // The two edges do not cross in the current scan line, but
                    // the polygon is less than a one pixel wide in the
                    // current scan line.
                    int left = (int) Math.min(x1, x2 + p2.dPerScan);
                    int right = (int) Math.max(x1 + p1.dPerScan, x2) + 1;
                    float p1y = (left + 0.5f - p1.intersect) / p1.dPerScan;
                    float p2y = (left + 0.5f - p2.intersect) / p2.dPerScan;
                    float p1dPerPixel = 1f / p1.dPerScan;
                    float p2dPerPixel = 1f / p2.dPerScan;
                    for (int x=left, n = Math.min(right, cx2); x <= n; x++) {
                        if (x >= cx1) {
                            int p1i = Math.max(0, Math.min(255, (int) (p1y * 255f)));
                            int p2i = Math.max(0, Math.min(255, (int) (p2y * 255f)));

                            blend(x, scan,
                                    Math.abs(p1i - p2i)
                                    );

                        }
                        p1y += p1dPerPixel;
                        p2y += p2dPerPixel;
                    }
                } else {
                    // The two edges do not cross in the current scan line, and
                    // the polygon is at least one pixel wide in the current
                    // scan line

                    if (p1.dPerScan == 0f || p1.dPerScan == 1f || p1.dPerScan == -1f) {
                        // no antialiasing needed
                    } else if (p1.dPerScan < 0f) {
                        // Fractional part of the intersection at pixel x1
                        float frac = p1.intersect - (float) Math.floor(p1.intersect);

                        if (p1.dPerScan + frac >= 0f) {
                            // The edge leaves the scan line at the bottom of the
                            // pixel at x1. We only need to do alpha blending for
                            // the point at x1.

                            //  +----+---*+   * = edge at x1
                            //  |    |  /x|
                            //  |    | /xx|
                            //  +----+*---+   * = edge at x1 + p1.dPerScan
                            if (x1 >= cx1) {
                                blend(x1, scan,
                                        255 - (int) ((frac + frac + p1.dPerScan) /2f * 255f)
                                        );
                            }
                        } else {
                            // The edge leaves the scan line at the left of the
                            // pixel at x1. We need to do alpha blending for
                            // the point at x1 and to points on its left.

                            //  +----+--*-+   * = edge at x1
                            //  |    |/xxx|
                            //  |   /|xxxx|
                            //  +--*-+----+   * = edge at x1 + p1.dPerScan
                            if (x1 >= cx1) {
                                blend(x1, scan,
                                        255 - (int) (frac * frac / -p1.dPerScan / 2f * 255f)
                                        );
                            }


                            // Plot points to the left of x1
                            float dPerPixel = 1f / p1.dPerScan;

                            // y-value of the edge at the center of the pixel.
                            // The y-value is expressed as a fraction of the height
                            // of the pixel, measured from the bottom of the pixel.
                            float yInPixel = 1f + (0.5f + frac) * dPerPixel;
                            int x = x1-1;
                            for (int i=0, n = (int) -p1.dPerScan + 1; i < n; i++) {
                                if (x < cx1) break;
                                if (yInPixel < 0) {
                                    //  +----+  The edge enters at the right side of
                                    //  |    |  the pixel and leaves at the bottom.
                                    //  |   x*
                                    //  |  xx|
                                    //  +-*--+
                                    break;
                                } else {
                                    //  +----+  The edge enters at the right side of
                                    //  |   x*  the pixel and leaves at the left side.
                                    //  | xxx|  or at the bottom.
                                    //  *xxxx|  For this case, we can use the yInPixel as
                                    //  +----+  an approximation of the intensity value.
                                    //          The approximation is accurate, if
                                    //          the edge leaves at the left side.
                                    blend(x, scan, (int) (255f * yInPixel));
                                }
                                yInPixel += dPerPixel;
                                x--;
                            }
                        }
                        x1++;

                    } else {
                        // Fractional part of the intersection at pixel x1
                        float frac = p1.intersect - (float) Math.floor(p1.intersect);

                        if (p1.dPerScan + frac <= 1f) {
                            // The edge leaves the scan line at the bottom of the
                            // pixel at x1. We only need to do alpha blending for
                            // the point at x1.
                            //  +*---+   * = edge at x1
                            //  |x\  |
                            //  |xx\ |
                            //  +--*-+   * = edge + p1.dPerScan
                            if (x1 >= cx1) {
                                blend(x1, scan,
                                        (int) ((1f - frac + 1f - frac - p1.dPerScan) / 2f * 255f)
                                        );
                            }
                            x1++;
                        } else {
                            //  +--*-+----+   * = edge at x1
                            //  |xxx\|    |
                            //  |xxxx|\   |
                            //  +----+-*--+   * = edge + p1.dPerScan
                            if (x1 >= cx1) {
                                blend(x1, scan,
                                        (int) ((1f - frac) * (1f - frac) / p1.dPerScan / 2f * 255f)
                                        );
                            }
                            x1++;
                            // Plot points to the right of x1
                            //  +----+  We only consider the case, where the edge
                            //  |xxxx|  enters at the left side of the pixel and leaves
                            //  *xxxx|  at the right side. For this case,
                            //  | xxx|  we can compute the relative y position at the
                            //  |   x*  center of the pixel, and use that as the intensity
                            //  |    |  value.
                            //  +----+  This gives accurate results except for the right
                            //          most pixel, on which the edge leaves at the bottom
                            //          of the pixel. For this pixel we should compute
                            //          the size of the triangle entering at the right
                            //          of the pixel and leaving at its bottom.
                            float dPerPixel = 1f / p1.dPerScan;
                            float yInPixel = (1.5f - frac) * dPerPixel;
                            for (int i=0, n = (int) p1.dPerScan + 1; i < n; i++) {
                                if (yInPixel >= 1f || x1 > cx2) break;
                                if (x1 >= cx1) {
                                    blend(x1, scan, (int) (255f * yInPixel));
                                }
                                x1++;
                                yInPixel += dPerPixel;
                            }
                        }
                    }
                    //------
                    if (p2.dPerScan == 0f || p2.dPerScan == 1f || p2.dPerScan == -1f) {
                        // no antialiasing needed
                    } else if (p2.dPerScan < 0f) {
                        // Fractional part of the intersection at pixel x2
                        float frac = (float) (p2.intersect - Math.floor(p2.intersect));

                        if (p2.dPerScan + frac >= 0f) {
                            // The edge leaves the scan line at the bottom of the
                            // pixel at x2. We only need to do alpha blending for
                            // the point at x2.

                            //  +---*+   * = edge at x2
                            //  |xx/ |
                            //  |x/  |
                            //  +*---+   * = edge at x2 + p2.dPerScan
                            if (x2 <= cx2) {
                                blend(x2, scan,
                                        (int) ((frac + frac + p2.dPerScan) /2f * 255f)
                                        );
                            }
                            x2--;
                        } else {
                            //  +----+--*-+   * = edge at x2
                            //  |xxxx|/   |
                            //  |xxx/|    |
                            //  +--*-+----+   * = edge at x2 + p2.dPerScan

                            if (x2 <= cx2) {
                                blend(x2, scan,
                                        (int) (frac * frac / -p2.dPerScan / 2f * 255f)
                                        );
                            }
                            x2--;
                            //}

                            // Plot points to the left of x2
                            float dPerPixel = 1f / p2.dPerScan;
                            float yInPixel = (0.5f + frac) * -dPerPixel;

                            for (int i=0, n = (int) -p2.dPerScan + 1; i < n; i++) {
                                if (x2 < cx1) break;
                                if (yInPixel >= 1f) {
                                    //  +----+  The edge enters at the right side of
                                    //  |xxxx|  the pixel and leaves at the bottom.
                                    //  |xxxx*
                                    //  |xxx |
                                    //  +-*--+
                                    break;
                                } else {
                                    //  +----+  The edge enters at the right side of
                                    //  |xxxx*  the pixel and leaves at the left side.
                                    //  |xx  |  or at the bottom.
                                    //  *    |  For this case, we can use the yInPixel as
                                    //  +----+  an approximation of the intensity value.
                                    //          The approximation is accurate, if
                                    //          the edge leaves at the left side.
                                    if (x2 <= cx2) {
                                        blend(x2, scan, (int) (255f * yInPixel));
                                        //pixels[x2+scan*gw] = 0xff0000;
                                    }
                                }
                                yInPixel -= dPerPixel;
                                x2--;
                            }
                        }
                    } else {
                        float frac = p2.intersect - (float) Math.floor(p2.intersect);

                        if (p2.dPerScan + frac <= 1f) {
                            // The edge leaves the scan line at the bottom of the
                            // pixel at x2. We only need to do alpha blending for
                            // the point at x2.

                            //  +*---+   * = edge at x2
                            //  |x\  |
                            //  |xx\ |
                            //  +---*+   * = edge at x2 + p2.dPerScan
                            if (x2 <= cx2) {
                                blend(x2, scan,
                                        (int) ((frac + frac + p2.dPerScan) /2f * 255f)
                                        );
                            }
                            x2--;
                        } else {
                            //  +--*-+----+   * = edge at x2
                            //  |xxx\|    |
                            //  |xxxx|\   |
                            //  +----+-x--+   * = edge at x2 + p2.dPerScan
                            if (x2 <= cx2) {
                                blend(x2, scan,
                                        255 - (int) (1f - frac + (1f - frac) / p2.dPerScan / 2f * 255f)
                                        );
                            }

                            // Plot points to the right of x2
                            //  +----+  We only consider the case, where the edge
                            //  |    |  enters at the left side of the pixel and leaves
                            //  *x   |  at the right side. For this case,
                            //  |xxx |  we can compute the relative y position at the
                            //  |xxxx*  center of the pixel, and use that as the intensity
                            //  |xxxx|  value.
                            //  +----+  This gives accurate results except for the right
                            //          most pixel, on which the edge leaves at the bottom
                            //          of the pixel. For this pixel we should compute
                            //          the size of the triangle entering at the right
                            //          of the pixel and leaving at its bottom.
                            float dPerPixel = 1f / p2.dPerScan;
                            float yInPixel = (1.5f - frac) * dPerPixel;
                            int x = x2 + 1;
                            for (int i=0, n = (int) p2.dPerScan + 1; i < n; i++) {
                                if (yInPixel >= 1f || x > cx2) break;
                                if (x >= cx1) {
                                    blend(x, scan, 255 - (int) (255f * yInPixel));
                                }
                                x++;
                                yInPixel += dPerPixel;
                            }

                            x2--;
                        }
                    }

                    // Arraycopy from color bar
                    if (x2 >= x1) {
                        try {
                        	this.plotLine(Math.max(cx1, x1), scan, Math.min(cx2, x2)+1, scan, colorBar[0]);
                        	//System.arraycopy(colorBar, 0, pixels, start, Math.min(cx2, x2)+scan*w - start + 1);
                        } catch (IndexOutOfBoundsException e) {
                            //System.out.println(pixels.length+" "+start+".."+(start + Math.min(cx2, x2)+scan*gw - start + 1));
                        }
                    }
                }
            }
            // advance to next pair of intersections
            p1 = p2.next;
        }
    }
    protected void plotVClipped(int scan, Edge active) {
        Edge p1, p2;
        p1 = active.next;
        while (p1 != null) {
            p2 = p1.next;
            // Avoid Math.round. It is too expensive!!
            //int y1 = Math.round(p1.intersect);
            //int y2 = Math.round(p2.intersect);
            int y1 = (int) (p1.intersect);
            int y2 = (int) p2.intersect;
            if (y1 <= cy2 && y2 >= cy1) {
                for (int xy = Math.max(cy1, y1), n = Math.min(cy2, y2); xy <= n; xy += w) {
                	setImageRGB(scan, xy, color);
                }

            }
            // advance to next pair of intersections
            p1 = p2.next;
        }
    }
    // -------------------------
    // END ScanFill algorithm
    // -------------------------
*/

    /**
     * Draws a sequence of connected lines defined by
     * arrays of <i>x</i> and <i>y</i> coordinates.
     * Each pair of (<i>x</i>,&nbsp;<i>y</i>) coordinates defines a point.
     * The figure is not closed if the first point
     * differs from the last point.
     * @param       xPoints an array of <i>x</i> points
     * @param       yPoints an array of <i>y</i> points
     * @param       nPoints the total number of points
     * @see         java.awt.Graphics#drawPolygon(int[], int[], int)
     * @since       JDK1.1
     */
    private void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
    	int color = getColor();
    	int alpha = this.getAlpha(color);
    	if (alpha != 255) {
    		int red = this.getRed(color);
    		int green = this.getGreen(color);
    		int blue = this.getBlue(color);
    		int col1 = getImageRGB(3, 3);
    		int red1 = this.getRed(col1);
    		int green1 = this.getGreen(col1);
    		int blue1 = this.getBlue(col1);
    		float masking_factor = 1.2f-(alpha)/255.0f;
    		if (green1 > 150) masking_factor += 0.15f;
    		red = (int) (red * (1 - masking_factor) + red1 * masking_factor);
    		green = (int) (green * (1 - masking_factor) + green1 * masking_factor);
    		blue = (int) (blue * (1 - masking_factor) + blue1 * masking_factor);
    		color = red<<16 | green<<8 | blue;
    	}

    	int clip[] = this.getClip();
        for (int i=1; i < nPoints; i++) {
        	if (xPoints[i-1] == yPoints[i-1] && xPoints[i-1] == -1) continue;
        	if (xPoints[i] == yPoints[i] && xPoints[i] == -1) continue;
            drawFastLine(xPoints[i-1], yPoints[i-1], xPoints[i], yPoints[i], color, clip, false);
        }
    }



    private int lastR = -1, lastG = -1, lastB = -1, lastA = -1, lastRGB = -1;
	@Override
	public void setColor(int r, int g, int b, int a) {
		if (!transparencyEnabled) a = 255;

		if (a == lastA && r == lastR && g == lastG && b == lastB) return;

		Color col = new Color(r, g, b, a);
		this.g.setColor(col);
		if (g2 != null) g2.setColor(col);
		lastR = r;
		lastG = g;
		lastB = b;
		lastA = a;
		lastRGB = a << 24 | r<<16 | g<<8 | b;
	}

	@Override
	public void drawImage(Object img, float x, float y) {
		drawImage(img, x, y, 1.0, 1.0, g, g2);
	}

	@Override
	public void drawImage(Object img, float x, float y, double scalex, double scaley) {
		drawImage(img, x, y, scalex, scaley, g, g2);
	}

	private void drawImage(Object image, float x, float y, double scalex, double scaley, Graphics2D g, Graphics2D g2) {
/*		if (image.getClass().isArray()) {
			if (g != null) {
				Object o[] = (Object[]) image;
				int data[] = (int[]) o[1];
				int size[] = (int[]) o[0];
				if (this.invertEnabled) {
					if (this.invertH) x = invertX(x) - (int)(size[0]*scalex)+1;
					if (this.invertV) y = invertY(y) - (int)(size[1]*scaley)+1;
				}
				int ii = (int) x;
				int jj = (int) y;
				int dy0 = jj * size[0];
				//int rec[] = this.getClip();
				for (int j=jj; j<jj+size[1]; j++) {
					// Method 1: also works with alpha, but slower than Java2D
					//if (j < rec[1] || j >= rec[1]+rec[3]) continue;
					//int py = (j-jj)*size[0];
					//for (int i=ii; i<ii+size[0]; i++) {
					//	if (i < rec[0] || i >= rec[0]+rec[2]) continue;

					//	int index = i-ii+py;
				    //    int alpha = getAlpha(data[index]);
				    //    oneMinusAlpha = 255 - alpha;
				    //    premultipliedR = (data[index] & 0xff0000) * alpha;
				    //    premultipliedG = (data[index] & 0xff00) * alpha;
				    //    premultipliedB = (data[index] & 0xff) * alpha;
				    //    previousIn = previousOut = data[index];

				    //    this.plot(i, j);
					//}


					// Method 2, for stars without textures (images with no alpha)
					int dj = j * size[0];
					int py = ii + dj;
					int dy = dj - dy0;
					if (rasterData != null) System.arraycopy(data, dy, rasterData, py, size[0]);
					if (rasterData2 != null) System.arraycopy(data, dy, rasterData2, py, size[0]);

				}
			}
			return;
		}
*/
		BufferedImage img = toImage(image);

		if (this.invertEnabled) {
			if (this.invertH) x = invertX(x) - (int)(img.getWidth()*scalex)+1;
			if (this.invertV) y = invertY(y) - (int)(img.getHeight()*scaley)+1;
		}

		if (scalex == 1.0 && scaley == 1.0) {
			if (!this.externalGraphics || (x == (int)(x) && y == (int) y)) {
				if (g != null) g.drawImage(img, (int)x, (int)y, null);
				if (g2 != null) g2.drawImage(img, (int)x, (int)y, null);
				return;
			}

			if (g != null) {
				g.translate(x, y);
				g.drawImage(img, 0, 0, null);
				g.translate(-x, -y);
			}
			if (g2 != null) {
				g2.translate(x, y);
				g2.drawImage(img, 0, 0, null);
				g2.translate(-x, -y);
			}
		} else {
			AffineTransform trans = new AffineTransform();
			trans.translate(x, y);
			trans.scale(scalex, scaley);
			float dx = 0, dy = 0;
			if (scalex < 0.0) dx = -img.getWidth();
			if (scaley < 0.0) dy = -img.getHeight();
			trans.translate(dx, dy);

			if (g != null) g.drawImage(img, trans, null);
			if (g2 != null) g2.drawImage(img, trans, null);
		}
	}

	@Override
	public Object getImage(String url) {
		Object obj = DataBase.getData(url, "AWTGraphics", true);
		if (obj != null) return obj;

		if (url.startsWith("file:") || url.startsWith("http:")  || url.startsWith("https:")) {
			try {
				BufferedImage img = GeneralQuery.queryImage(url);
				return img;
			} catch (Exception exc) { return null;}
		}

		try {
			InputStream res = getClass().getClassLoader().getResourceAsStream(url);
			if (res == null) return null;
			BufferedImage img = ImageIO.read(res);
			return img;
		} catch (Exception exc) {
			// Toolkit should be avoided when reading images, even in case MediaTracker is used after
			Image image = Toolkit.getDefaultToolkit().getImage(getClass().getClassLoader().getResource(url));
			try {
				return Picture.toBufferedImage(image); // Required in Linux when /tmp has almost no memory left
			} catch (JPARSECException e) {
				e.printStackTrace();
				return image; // XXX: Will produce slowdown when using getRGB, but this should never happen ?
			}
		}
	}

	@Override
	public void addToDataBase(Object img, String id, int life) {
		if (life <= 0) {
			DataBase.addData(id, "AWTGraphics", img, true);
			return;
		}

		DataBase.addData(id, "AWTGraphics", img, true, life);
	}

	@Override
	public void clearDataBase() {
		DataBase.deleteThreadData("AWTGraphics");
	}

	@Override
	public Object getFromDataBase(String id) {
		return DataBase.getData(id, "AWTGraphics", true);
	}

	@Override
	public Object getImage(int w, int h, int[] pixels) {
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		image.getRaster().setDataElements(0, 0, w, h, pixels);
		return image;
	}

	@Override
	public void setRGB(Object image, int x, int y, int c) {
		BufferedImage img = toImage(image);
		img.setRGB(x, y, c);
	}

	@Override
	public Object getScaledImage(Object image, int w, int h, boolean sameRatio, boolean useSpline) {
		return getScaledImage(image, w, h, sameRatio, useSpline, 0);
	}

	@Override
	public Object getScaledImage(Object image, int w, int h, boolean sameRatio, boolean useSpline, int dy) {
		BufferedImage img = toImage(image);
		Picture p = new Picture(img);
		if (useSpline && w < img.getWidth() && h < img.getHeight()) {
			try {
				p.getScaledInstanceUsingSplines(w, h, sameRatio);
			} catch (Exception e) {
				p.getScaledInstance(w, h, sameRatio, true);
			}
		} else {
			p.getScaledInstance(w, h, sameRatio, false);
		}

		if (dy == 0) return p.getImage();

		BufferedImage out = new BufferedImage(p.getWidth(), p.getHeight()+dy*2, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.drawImage(p.getImage(), 0, dy, null);
		g.dispose();
		return out;
	}

	@Override
	public Object getRotatedAndScaledImage(Object image, float radius_x, float radius_y, float ang, float scalex,
			float scaley) {
		BufferedImage img = toImage(image);

		AffineTransform trans = new AffineTransform();

		if (ang != 0.0) trans.rotate(ang, radius_x, radius_y );
		if (scalex != 1.0 || scaley != 1.0) {
			trans.scale(scalex, scaley);
			float dx = 0, dy = 0;
			if (scalex < 0.0) dx = -img.getWidth();
			if (scaley < 0.0) dy = -img.getHeight();
			trans.translate(dx, dy);
		}
		BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.drawImage(img, trans, null);
		g.dispose();
		return out;
	}


	@Override
	public Object cloneImage(Object image) {
		BufferedImage img = toImage(image);
		BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
		java.awt.Graphics gg = out.getGraphics();
		gg.drawImage(img, 0, 0, null);
		gg.dispose();
		return out;
	}

	@Override
	public int[] getImageAsPixels(Object image) {
		BufferedImage img = toImage(image);

		int width = img.getWidth(), height = img.getHeight();
		int a[] = new int[width*height];

		PixelGrabber pg = new PixelGrabber(img, 0, 0, width, height, a, 0, width);
		try
		{
			pg.grabPixels();
		} catch (InterruptedException exception)
		{
			int index = -1;
			for (int j=0; j<height; j++)
			{
				for (int i=0; i<width; i++)
				{
					index ++;
					a[index] = img.getRGB(i, height-1-j);
				}
			}
		}

		return a;

	}

	@Override
	public int[] getSize(Object sun) {
		BufferedImage img = toImage(sun);
		return new int[] {img.getWidth(), img.getHeight()};
	}

	@Override
	public Object getRendering() {
		if (colorMode.isReal3D()) return blendImagesToAnaglyphMode(image, image2);

		return image;
	}

	@Override
	public Object getRendering(int i, int j, int width, int k) {
		if (i < 0) i = 0;
		if (j < 0) j = 0;
		if (i+width > w) width = w-i;
		if (j+k > h) k = h-j;
		if (colorMode.isReal3D()) return blendImagesToAnaglyphMode(image.getSubimage(i, j, width, k), image2.getSubimage(i, j, width, k));

		return image.getSubimage(i, j, width, k);
	}

	@Override
	public void setColor(int col, boolean hasalpha) {
		int alpha = 255;
		if (hasalpha && transparencyEnabled) alpha = getAlpha(col);
		if (col == lastRGB && alpha == lastA) return;
		setColor(getRed(col), getGreen(col), getBlue(col), alpha);
	}

	@Override
	public void fillOval(float i, float j, float k, float l, boolean fastMode) {
		int c = this.getColor();
		if (fastMode && !externalGraphics && k > 1 && l > 1 && this.getAlpha(c) == 255) {
			EllipseBresenhamInterpolator ebi = new EllipseBresenhamInterpolator((int)(k/2), (int) (l/2));
	        int rx = (int) (k / 2); // radius x
	        int ry = (int) (l / 2); // radius y
	        int cxw = (int) (i + rx); // center x for the west half of the ellipse
	        int cyn = (int) (j + ry); // center y for the north half of the ellipse
	        int cxe = cxw + ((int) k) % 2; // center x for the east half of the ellipse
	        int cys = cyn + ((int) l % 2); // center y for the south half of the ellipse

	        int dy = -1;
	        int clip[] = this.getClip();
	        do {
				this.drawFastLine(cxe + ebi.dx, cyn + ebi.dy, cxw - ebi.dx, cyn + ebi.dy, c, clip, false);
				this.drawFastLine(cxe + ebi.dx, cys - ebi.dy, cxw - ebi.dx, cys - ebi.dy, c, clip, false);
	            dy = ebi.dy;
	            ebi.next();
	        } while (dy < 0);
			return;
		}

		fillOval(i, j, k, l, g, g2);
	}

	private void fillOval(float i, float j, float k, float l, Graphics2D g, Graphics2D g2) {
		if (this.invertEnabled) {
			if (this.invertH) {
				i = invertX(i)-Math.abs(k)+1;
			}
			if (this.invertV) {
				j = invertY(j)-Math.abs(l)+1;
			}
		}

		if (!externalGraphics && k >= 0 && l >= 0 && k <= 1 && l <= 1) {
			int ii = (int)(i), jj = (int)(j);
			int[] rec = this.getClip();
			if (ii >= rec[0] && jj >= rec[1] && ii < rec[0]+rec[2] && jj < rec[1]+rec[3]) {
			   	int color = getColor();
		    	int alpha = this.getAlpha(color);
		    	if (alpha != 255) { // Fake transparency. k, l < 0 => real transparency using Java2D's fillOval
		    		int red = this.getRed(color);
		    		int green = this.getGreen(color);
		    		int blue = this.getBlue(color);
		    		int col1 = getImageRGB(3, 3);
		    		int red1 = this.getRed(col1);
		    		int green1 = this.getGreen(col1);
		    		int blue1 = this.getBlue(col1);
		    		float masking_factor = 1.2f-(alpha)/255.0f;
		    		if (green1 > 150) masking_factor += 0.15f;
		    		red = (int) (red * (1 - masking_factor) + red1 * masking_factor);
		    		green = (int) (green * (1 - masking_factor) + green1 * masking_factor);
		    		blue = (int) (blue * (1 - masking_factor) + blue1 * masking_factor);
		    		color = red<<16 | green<<8 | blue;
		    	}

				if (g != null) setImageRGB(ii, jj, color);
				if (g2 != null) setImageRGB2(ii, jj, color);
			}
			return;
		}

		if (!externalGraphics && k == l && k >= 0 && l <= 2) {
			int ii = (int)(i), jj = (int)(j);
			int[] rec = this.getClip();
			if (ii >= rec[0] && jj >= rec[1] && ii < rec[0]+rec[2]-1 && jj < rec[1]+rec[3]-1) {
				int color = getColor();
		    	int alpha = this.getAlpha(color);
		    	if (alpha != 255) { // Fake transparency. k, l < 0 => real transparency using Java2D's fillOval
		    		int red = this.getRed(color);
		    		int green = this.getGreen(color);
		    		int blue = this.getBlue(color);
		    		int col1 = getImageRGB(3, 3);
		    		int red1 = this.getRed(col1);
		    		int green1 = this.getGreen(col1);
		    		int blue1 = this.getBlue(col1);
		    		float masking_factor = 1.2f-(alpha)/255.0f;
		    		if (green1 > 150) masking_factor += 0.15f;
		    		red = (int) (red * (1 - masking_factor) + red1 * masking_factor);
		    		green = (int) (green * (1 - masking_factor) + green1 * masking_factor);
		    		blue = (int) (blue * (1 - masking_factor) + blue1 * masking_factor);
		    		color = red<<16 | green<<8 | blue;
		    	}
				if (g != null) {
					setImageRGB(ii, jj, color);
					setImageRGB(ii+1, jj, color);
					setImageRGB(ii+1, jj+1, color);
					setImageRGB(ii, jj+1, color);
				}
				if (g2 != null) {
					setImageRGB2(ii, jj, color);
					setImageRGB2(ii+1, jj, color);
					setImageRGB2(ii+1, jj+1, color);
					setImageRGB2(ii, jj+1, color);
				}
			}
			return;
		}

		k = Math.abs(k);
		l = Math.abs(l);
		if (!externalGraphics && i == (int)(i) && j == (int) j) {
			if (g != null) g.fillOval((int)i, (int)j, (int) (k+0.5), (int) (l+0.5));
			if (g2 != null) g2.fillOval((int)i, (int)j, (int) (k+0.5), (int) (l+0.5));
			return;
		}

		if (externalGraphics) {
			k--;
			l--;
		}

		if (g != null) {
			g.translate(i, j);
			g.fillOval(0, 0, (int) (k+0.5), (int) (l+0.5));
			g.translate(-i, -j);
		}
		if (g2 != null) {
			g2.translate(i, j);
			g2.fillOval(0, 0, (int) (k+0.5), (int) (l+0.5));
			g2.translate(-i, -j);
		}
	}

	@Override
	public void fillRect(float i, float j, float width, float height) {
		fillRect(i, j, width, height, g, g2);
	}

	private void fillRect(float i, float j, float width, float height, Graphics2D g, Graphics2D g2) {
		if (this.invertEnabled) {
			if (this.invertH) {
				i = invertX(i)-width+1;
			}
			if (this.invertV) {
				j = invertY(j)-height+1;
			}
		}

		if ((i == (int)(i) && j == (int) j)) {
			this.disableAntialiasing();
			if (g != null) g.fillRect((int)i, (int)j, (int) (width+0.5), (int) (height+0.5));
			if (g2 != null) g2.fillRect((int)i, (int)j, (int) (width+0.5), (int) (height+0.5));
			this.enableAntialiasing();
			return;
		}

		if ((width <= 1 && height <= 1)) {
			if (g != null) g.fillRect((int)(i+0.5), (int)(j+0.5), (int) (width+0.5), (int) (height+0.5));
			if (g2 != null) g2.fillRect((int)(i+0.5), (int)(j+0.5), (int) (width+0.5), (int) (height+0.5));
			return;
		}

		if (g != null) {
			g.translate(i, j);
			g.fillRect(0, 0, (int) (width+0.5), (int)(height+0.5));
			g.translate(-i, -j);
		}
		if (g2 != null) {
			g2.translate(i, j);
			g2.fillRect(0, 0, (int) (width+0.5), (int)(height+0.5));
			g2.translate(-i, -j);
		}
	}

	@Override
	public void drawString(String string, float i, float j) {
		drawString(string, i, j, g, g2);
	}

	private void drawString(String string, float i, float j, Graphics2D g, Graphics2D g2) {
		if (this.invertEnabled) {
			if (this.invertH) i = invertX(i);
			if (this.invertV) j = invertY(j);
		}

		if (string.indexOf("^") >= 0 || string.indexOf("_") >= 0 || string.indexOf("@") >= 0) {
			if (t == null) {
				t = new TextLabel(string, g.getFont(),  g.getColor(), TextLabel.ALIGN.LEFT);
			} else {
				t.setText(string);
				t.setFont(g.getFont());
				t.setColor(g.getColor());
				t.setJustification(TextLabel.ALIGN.LEFT);
			}
			if (this.invertH && this.invertEnabled) i -= t.getWidth(g)-1;
			t.draw(g, (int)(i+0.5), (int)(j+0.5), TextLabel.ALIGN.LEFT);
			if (g2 != null) t.draw(g2, (int)(i+0.5), (int)(j+0.5), TextLabel.ALIGN.LEFT);
		} else {
			float ii = i;
			if (this.invertH && this.invertEnabled) {
				i -= g.getFontMetrics().stringWidth(string)-1;
				if (g2 != null) {
					ii -= g2.getFontMetrics().stringWidth(string)-1;
				} else {
					ii = i;
				}
			}
//			GlyphVector gv = g.getFont().createGlyphVector(new FontRenderContext(null,  true, false), string);
//			g.drawGlyphVector(gv, i, j);
//			if (g2 != null) g2.drawGlyphVector(gv, ii, j);
			g.drawString(string, i, j);
			if (g2 != null) g2.drawString(string, ii, j);
		}
	}

	@Override
	public int getColor() {
		if (lastRGB != -1) return lastRGB;
		return g.getColor().getRGB();
	}

	@Override
	public void drawOval(float i, float j, float k, float l, boolean fast) {
		drawOval(i, j, k, l, g, g2, fast);
	}

	private void drawOval(float i, float j, float k, float l, Graphics2D g, Graphics2D g2,
			boolean fast) {
		if (this.invertEnabled) {
			if (this.invertH) i = invertX(i)-k+1;
			if (this.invertV) j = invertY(j)-l+1;
		}

		if (!externalGraphics && k <= 1 && l <= 1) {
			int ii = (int)(i), jj = (int)(j);
			int[] rec = this.getClip();
			if (ii >= rec[0] && jj >= rec[1] && ii < rec[0]+rec[2] && jj < rec[1]+rec[3]) {
				if (g != null) setImageRGB(ii, jj, getColor());
				if (g2 != null) setImageRGB2(ii, jj, getColor());
			}
			return;
		}

		if (!externalGraphics && ((i == (int)(i) && j == (int) j))) {
			if (fast) {
				drawFastOval((int)i, (int)j, (int)k-1, (int)l-1, this.getClip());
			} else {
				if (g != null) g.drawOval((int)i, (int)j, (int) (k-0.5), (int) (l-0.5));
				if (g2 != null) g2.drawOval((int)i, (int)j, (int) (k-0.5), (int) (l-0.5));
			}
			return;
		}
		
		if (externalGraphics) {
			k--;
			l--;
		}
		if (g != null) {
			g.translate(i, j);
			g.drawOval(0, 0, (int)(k+0.5), (int)(l+0.5));
			g.translate(-i, -j);
		}
		if (g2 != null) {
			g2.translate(i, j);
			g2.drawOval(0, 0, (int)(k+0.5), (int)(l+0.5));
			g2.translate(-i, -j);
		}
	}

    private void drawFastOval(int ox, int oy, int owidth, int oheight, int clip[]) {
    	int color = getColor();
    	int alpha = this.getAlpha(color);
    	if (alpha != 255) {
    		int red = this.getRed(color);
    		int green = this.getGreen(color);
    		int blue = this.getBlue(color);
    		int col1 = getImageRGB(3, 3);
    		int red1 = this.getRed(col1);
    		int green1 = this.getGreen(col1);
    		int blue1 = this.getBlue(col1);
    		float masking_factor = 1.2f-(alpha)/255.0f;
    		if (green1 > 150) masking_factor += 0.15f;
    		red = (int) (red * (1 - masking_factor) + red1 * masking_factor);
    		green = (int) (green * (1 - masking_factor) + green1 * masking_factor);
    		blue = (int) (blue * (1 - masking_factor) + blue1 * masking_factor);
    		color = red<<16 | green<<8 | blue;
    	}
        int hmax = 0;
        if (!isContinuous) {
            JPARSECStroke s = getStroke((BasicStroke)g.getStroke());
        	float da[] = s.getDashArray();
        	if (da[0] <= 2) {
        		hmax = (int)(0.5 + da[1] / da[0]);
        	} else {
        		hmax = 0;
        	}
        }

        /*
         * This method has been derived from:
         * agg_ellipse_bresenham.h
         *
         * Anti-Grain Geometry - Version 2.3
         * Copyright (C) 2002-2005 Maxim Shemanarev (http://www.antigrain.com)
         *
         * Permission to copy, use, modify, sell and distribute this software
         * is granted provided this copyright notice appears in all copies.
         * This software is provided "as is" without express or implied
         * warranty, and with no claim as to its suitability for any purpose.
         */

        // Reject oval, if it is outside of clip bounds
    	int cx2 = clip[0] + clip[2]-1;
    	int cy2 = clip[1] + clip[3]-1;
        if (ox + owidth < clip[0] || ox > cx2 ||
                oy + oheight < clip[1] || oy > cy2) {
            return;
        }

        int rx = owidth / 2; // radius x
        int ry = oheight / 2; // radius y
        int cxw = ox + rx; // center x for the west half of the ellipse
        int cyn = (oy + ry); // center y for the north half of the ellipse
        int cxe = cxw + owidth % 2; // center x for the east half of the ellipse
        int cys = cyn + (oheight % 2); // center y for the south half of the ellipse

        EllipseBresenhamInterpolator ei = new EllipseBresenhamInterpolator(rx, ry);

        //int dx, dy;
        if (ox >= clip[0] && oy >= clip[1] && ox + owidth <= cx2 && oy + oheight <= cy2) {
            // Do fast rendering without clipping
            if (cxe != cxw && cyn != cys) {
            	int dy = -1;
                do {
                    setImageRGB(cxe + ei.dx, cyn + ei.dy, color);
                    setImageRGB(cxe + ei.dx, cys - ei.dy, color);
                    setImageRGB(cxw - ei.dx, cys - ei.dy, color);
                    setImageRGB(cxw - ei.dx, cyn + ei.dy, color);
                    if (image2 != null) {
                    	setImageRGB2(cxe + ei.dx, cyn + ei.dy, color);
                    	setImageRGB2(cxe + ei.dx, cys - ei.dy, color);
                    	setImageRGB2(cxw - ei.dx, cys - ei.dy, color);
                    	setImageRGB2(cxw - ei.dx, cyn + ei.dy, color);
                    }
                    dy = ei.dy;
                    ei.next();
                    if (hmax > 0) {
                    	for (int i=0; i<hmax; i++) {
                    		ei.next();
                    	}
                    }
                }
                while(dy < 0);
            } else if (cxe != cxw) {
                setImageRGB(cxe + ei.dx, cyn + ei.dy, color);
                setImageRGB(cxe + ei.dx, cys - ei.dy, color);
                setImageRGB(cxw - ei.dx, cys - ei.dy, color);
                setImageRGB(cxw - ei.dx, cyn + ei.dy, color);
                if (image2 != null) {
                	setImageRGB2(cxe + ei.dx, cyn + ei.dy, color);
                	setImageRGB2(cxe + ei.dx, cys - ei.dy, color);
                	setImageRGB2(cxw - ei.dx, cys - ei.dy, color);
                	setImageRGB2(cxw - ei.dx, cyn + ei.dy, color);
                }
                ei.next();
                if (hmax > 0) {
                	for (int i=0; i<hmax; i++) {
                		ei.next();
                	}
                }
                do {
                    setImageRGB(cxe + ei.dx, cyn + ei.dy, color);
                    setImageRGB(cxe + ei.dx, cys - ei.dy, color);
                    setImageRGB(cxw - ei.dx, cys - ei.dy, color);
                    setImageRGB(cxw - ei.dx, cyn + ei.dy, color);
                    if (image2 != null) {
                    	setImageRGB2(cxe + ei.dx, cyn + ei.dy, color);
                    	setImageRGB2(cxe + ei.dx, cys - ei.dy, color);
                    	setImageRGB2(cxw - ei.dx, cys - ei.dy, color);
                    	setImageRGB2(cxw - ei.dx, cyn + ei.dy, color);
                    }
                    ei.next();
                    if (hmax > 0) {
                    	for (int i=0; i<hmax; i++) {
                    		ei.next();
                    	}
                    }
                } while(ei.dy < 0);
                setImageRGB(cxe + ei.dx, cys - ei.dy, color);
                setImageRGB(cxw - ei.dx, cyn + ei.dy, color);
                if (image2 != null) {
                	setImageRGB2(cxe + ei.dx, cys - ei.dy, color);
                	setImageRGB2(cxw - ei.dx, cyn + ei.dy, color);
                }
            } else if (cyn != cys) {
                setImageRGB(cxe + ei.dx, cyn + ei.dy, color);
                setImageRGB(cxe + ei.dx, cys - ei.dy, color);
                if (image2 != null) {
                	setImageRGB2(cxe + ei.dx, cyn + ei.dy, color);
                	setImageRGB2(cxe + ei.dx, cys - ei.dy, color);
                }
                ei.next();
                if (hmax > 0) {
                	for (int i=0; i<hmax; i++) {
                		ei.next();
                	}
                }
                do {
                    setImageRGB(cxe + ei.dx, cyn + ei.dy, color);
                    setImageRGB(cxe + ei.dx, cys - ei.dy, color);
                    setImageRGB(cxw - ei.dx, cys - ei.dy, color);
                    setImageRGB(cxw - ei.dx, cyn + ei.dy, color);
                    if (image2 != null) {
                        setImageRGB2(cxe + ei.dx, cyn + ei.dy, color);
                        setImageRGB2(cxe + ei.dx, cys - ei.dy, color);
                        setImageRGB2(cxw - ei.dx, cys - ei.dy, color);
                        setImageRGB2(cxw - ei.dx, cyn + ei.dy, color);
                    }
                    ei.next();
                    if (hmax > 0) {
                    	for (int i=0; i<hmax; i++) {
                    		ei.next();
                    	}
                    }
                } while(ei.dy < 0);
                setImageRGB(cxe + ei.dx, cyn + ei.dy, color);
                setImageRGB(cxe + ei.dx, cys - ei.dy, color);
                setImageRGB(cxw - ei.dx, cys - ei.dy, color);
                setImageRGB(cxw - ei.dx, cyn + ei.dy, color);
                if (image2 != null) {
                    setImageRGB2(cxe + ei.dx, cyn + ei.dy, color);
                    setImageRGB2(cxe + ei.dx, cys - ei.dy, color);
                    setImageRGB2(cxw - ei.dx, cys - ei.dy, color);
                    setImageRGB2(cxw - ei.dx, cyn + ei.dy, color);
                }
            } else {
                setImageRGB(cxe + ei.dx, cyn + ei.dy, color);
                setImageRGB(cxe + ei.dx, cys - ei.dy, color);
                if (image2 != null) {
                    setImageRGB2(cxe + ei.dx, cyn + ei.dy, color);
                    setImageRGB2(cxe + ei.dx, cys - ei.dy, color);
                }
                ei.next();
                if (hmax > 0) {
                	for (int i=0; i<hmax; i++) {
                		ei.next();
                	}
                }
                do {
                    setImageRGB(cxe + ei.dx, cyn + ei.dy, color);
                    setImageRGB(cxe + ei.dx, cys - ei.dy, color);
                    setImageRGB(cxw - ei.dx, cys - ei.dy, color);
                    setImageRGB(cxw - ei.dx, cyn + ei.dy, color);
                    if (image2 != null) {
                        setImageRGB2(cxe + ei.dx, cyn + ei.dy, color);
                        setImageRGB2(cxe + ei.dx, cys - ei.dy, color);
                        setImageRGB2(cxw - ei.dx, cys - ei.dy, color);
                        setImageRGB2(cxw - ei.dx, cyn + ei.dy, color);
                    }
                    ei.next();
                    if (hmax > 0) {
                    	for (int i=0; i<hmax; i++) {
                    		ei.next();
                    	}
                    }
                } while(ei.dy < 0);
                setImageRGB(cxe + ei.dx, cys - ei.dy, color);
                setImageRGB(cxw - ei.dx, cyn + ei.dy, color);
                if (image2 != null) {
                    setImageRGB2(cxe + ei.dx, cys - ei.dy, color);
                    setImageRGB2(cxw - ei.dx, cyn + ei.dy, color);
                }
            }
        } else {

            // Do rendering with pixel level clipping
            cyn = (oy + ry); // center y for the north half of the ellipse
            cys = (cyn + oheight % 2); // center y for the south half of the ellipse
        	int dy = -1;
            do {
                int ex = cxe + ei.dx;
                if (ex >= clip[0] && ex <= cx2) {
                    if (cyn + ei.dy >= clip[1] && cyn + ei.dy <= cy2) {
                    	setImageRGB(ex, cyn + ei.dy, color);
                    }
                    if (cys - ei.dy >= clip[1] && cys - ei.dy <= cy2) {
                    	setImageRGB(ex, cys - ei.dy, color);
                    }
                }
                ex = cxw - ei.dx;
                if (ex >= clip[0] && ex <= cx2) {
                    if (cyn - ei.dy >= clip[1] && cyn - ei.dy <= cy2) {
                    	setImageRGB(ex, cyn - ei.dy, color);
                    }
                    if (cys + ei.dy >= clip[1] && cys + ei.dy <= cy2) {
                    	setImageRGB(ex, cys + ei.dy, color);
                    }
                }
                dy = ei.dy;
                ei.next();
                if (hmax > 0) {
                	for (int i=0; i<hmax; i++) {
                		ei.next();
                	}
                }
            }
            while(dy < 0);
        }
    }



	@Override
	public void disableAntialiasing() {
		//Object antialiasing = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        if (g2 != null) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        }
	}

	@Override
	public void enableAntialiasing() {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        if (g2 != null) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        }
	}

	@Override
	public Graphics.FONT getFont() {
		Font font = g.getFont();
		FONT f = FONT.getDerivedFont(FONT.DIALOG_BOLD_12, font.getName());
		f = FONT.getDerivedFont(f, font.getSize(), font.getStyle());
		return f;
	}

	@Override
	public void setClip(int i, int j, int k, int l) {
		if (this.invertEnabled) {
			if (this.invertH) {
				i = (int) (invertX(i)-k+1);
			}
			if (this.invertV) {
				j = (int) (invertY(j)-l+1);
			}
		}

		if (clip != null && i == clip.x && j == clip.y && k == clip.width && l == clip.height) return;
		g.setClip(i, j, k, l);
		clip = g.getClipBounds();
		if (g2 != null) {
			// XXX - Little fix for iText pdf output
			if (this.renderingToExternalGraphics()) l --;
			g2.setClip(i, j, k, l);
		}
	}

	@Override
	public int[] getClip() {
		//Rectangle2D rec = g.getClipBounds();
		//if (rec == null) return new int[] {0, 0, w, h};
		//return new int[] {(int) rec.getX(), (int) rec.getY(), (int) rec.getWidth(), (int) rec.getHeight()};
		if (clip == null) return new int[] {0, 0, w, h};
		return new int[] {clip.x, clip.y, clip.width, clip.height};
	}

	@Override
	public void setStroke(JPARSECStroke stroke) {
		isContinuous = stroke.isContinuousLine();
		lineWidth = stroke.getLineWidth();
		g.setStroke(AWTGraphics.getStroke(stroke));
		if (g2 != null) g2.setStroke(AWTGraphics.getStroke(stroke));
	}

	@Override
	public void draw(Object pathAxes) {
		draw(pathAxes, g, g2);
	}

	private void draw(Object pathAxes, Graphics2D g, Graphics2D g2) {
		if (pathAxes instanceof GeneralPath) {
			if (g != null) g.draw((GeneralPath) pathAxes);
			if (g2 != null) g2.draw((GeneralPath) pathAxes);
		} else {
			if (pathAxes instanceof Polygon) {
				if (g != null) 	g.draw((Polygon) pathAxes);
				if (g2 != null) g2.draw((Polygon) pathAxes);
			}
		}
	}

	@Override
	public Graphics getGraphics() {
		AWTGraphics awt = new AWTGraphics(this.getWidth(), this.getHeight(), this.colorMode, this.invertH, this.invertV);
        if (g.getRenderingHint(RenderingHints.KEY_ANTIALIASING) != RenderingHints.VALUE_ANTIALIAS_ON)
        	awt.disableAntialiasing();
        awt.invertEnabled = this.invertEnabled;
        awt.transparencyEnabled = this.transparencyEnabled;
		return awt;
	}

	@Override
	public Graphics getGraphics(int w, int h) {
		AWTGraphics awt = new AWTGraphics(w, h, this.colorMode, this.invertH, this.invertV);
        if (g.getRenderingHint(RenderingHints.KEY_ANTIALIASING) != RenderingHints.VALUE_ANTIALIAS_ON)
        	awt.disableAntialiasing();
        awt.invertEnabled = this.invertEnabled;
        awt.transparencyEnabled = this.transparencyEnabled;
		return awt;
	}

	@Override
	public void setColorFromObject(Object c) {
		if (c != null) {
			Color col = (Color) c;
			if (!transparencyEnabled) col = new Color(col.getRGB(), false);
			g.setColor(col);
			if (g2 != null) g2.setColor(col);
			lastR = col.getRed();
			lastG = col.getGreen();
			lastB = col.getBlue();
			lastA = col.getAlpha();
			lastRGB = lastA << 24 | lastR<<16 | lastG<<8 | lastB;
		}
	}

	@Override
	public void fill(Object path) {
		fill(path, g, g2);
	}

	private void fill(Object path, Graphics2D g, Graphics2D g2) {
		if (path instanceof GeneralPath) {
			if (clip != null && !((GeneralPath) path).intersects(clip)) return;
			if (g != null) 	g.fill((GeneralPath) path);
			if (g2 != null) g2.fill((GeneralPath) path);
		} else {
			if (path instanceof Polygon) {
				if (clip != null && !((Polygon) path).intersects(clip)) return;
				if (g != null) 	g.fill((Polygon) path);
				if (g2 != null) g2.fill((Polygon) path);
			}
		}
	}

	private FontMetrics fontMetric = null;
	private Font fontMetricFont = null;
	@Override
	public Rectangle getStringBounds(String labelg) {
		if (labelg.contains("^") || labelg.contains("_") || labelg.contains("@")) {
			if (labelg.contains("^{")) {
				labelg = DataSet.replaceOne(labelg, "^{", "", 1);
				if (labelg.endsWith("}")) labelg = labelg.substring(0, labelg.length()-1);
			}
			if (labelg.contains("^") || labelg.contains("_") || labelg.contains("@")) {
				labelg = TextLabel.getSimplifiedString(labelg);
			}
		}
		if (this.externalGraphics && g2 != null) {
			Rectangle2D rec = g2.getFontMetrics().getStringBounds(labelg, g2);
			return new Rectangle((float)rec.getX(), (float)rec.getY(), (float)rec.getWidth(), (float)rec.getHeight());
		} else {
			if (fontMetric == null || g.getFont() != fontMetricFont) {
				fontMetric = g.getFontMetrics();
				fontMetricFont = g.getFont();
			}
			Rectangle2D rec = fontMetric.getStringBounds(labelg, g);
			return new Rectangle((float)rec.getX(), (float)rec.getY(), (float)rec.getWidth(), (float)rec.getHeight());
		}
	}

	@Override
	public float getStringWidth(String labelg) {
		return getStringBounds(labelg).getWidth();
	}

	// Use raster to speed-up getRGB calls
	private void setUseRaster(boolean raster) {
		useRaster = false;
		if (image == null) return;

		useRaster = raster;
		if (useRaster) {
			rasterData = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
			if (image2 != null) rasterData2 = ((DataBufferInt) image2.getRaster().getDataBuffer()).getData();
		}
	}

	@Override
	public int getRGB(int i, int j) {
		if (this.invertEnabled) {
			if (this.invertH) i = (int) invertX(i);
			if (this.invertV) j = (int) invertY(j);
		}

		if (rasterData != null) return (255<<24) | rasterData[i + j * image.getWidth()];
		return image.getRGB(i, j);
	}

	@Override
	public int[] getRGBs(Object s, int i, int j, int w, int h) {
		BufferedImage image = toImage(s);

		if (i+w > image.getWidth()) w = image.getWidth()-i;
		if (j+h > image.getHeight()) h = image.getHeight()-j;

		if (w <= 0 || h <= 0) return null;
		
		return image.getRGB(i, j, w, h, null, 0, w);
	}
	
	@Override
	public int[] getRGBs(int i, int j, int w, int h) {
		if (this.invertEnabled) {
			if (this.invertH) {
				i = (int)invertX(i)-w+1;
			}
			if (this.invertV) {
				j = (int)invertY(j)-h+1;
			}
		}

		if (i+w > image.getWidth()) w = image.getWidth()-i;
		if (j+h > image.getHeight()) h = image.getHeight()-j;

		if (w <= 0 || h <= 0) return null;
		
		if (rasterData != null) {
			int rgb[] = new int[w*h];
			for (int y=j; y<j+h; y++) {
				int srcPos = y * image.getWidth() + i;
				int destPos = (y - j) * w;
				System.arraycopy(rasterData, srcPos, rgb, destPos, w);
			}
			return rgb;
		}
		return image.getRGB(i, j, w, h, null, 0, w);
	}

	@Override
	public int getRGB2(int i, int j) {
		if (this.invertEnabled) {
			if (this.invertH) i = (int) invertX(i);
			if (this.invertV) j = (int) invertY(j);
		}

		if (rasterData2 != null) return (255<<24) | rasterData2[i + j * image2.getWidth()];
		return image2.getRGB(i, j);
	}

	@Override
	public float[] getInvertedPosition(float i, float j) {
		if (this.invertEnabled) {
			if (this.invertH) i = (int) invertX(i);
			if (this.invertV) j = (int) invertY(j);
		}
		return new float[] {i, j};
	}

	@Override
	public int[] getInvertedRectangle(int[] rec) {
		if (this.invertEnabled) {
			if (this.invertH) {
				rec[0] = (int) (invertX(rec[0])-rec[2]+1);
			}
			if (this.invertV) {
				rec[1] = (int) (invertY(rec[1])-rec[3]+1);
			}
		}
		return rec;
	}

	@Override
	public int getRGB(Object s, int i, int j) {
		BufferedImage img = toImage(s);

		if (this.invertEnabled) {
			if (this.invertH) i = img.getWidth() - 1 - i;
			if (this.invertV) j = img.getHeight() - 1 - j;
		}

		return img.getRGB(i, j);
	}

	@Override
	public int getRGBLeft(Object s, int i, int j, float z) {
		BufferedImage img = toImage(s);

		float y = this.getY(j, z);
		float x = this.getXLeft(i, z);

		if (this.invertEnabled) {
			if (this.invertH) x = (img.getWidth()-1 - x);
			if (this.invertV) y = (img.getHeight()-1 - y);
		}

		return img.getRGB((int)(x+0.5), (int)(y+0.5));
	}
	@Override
	public int getRGBRight(Object s, int i, int j, float z) {
		BufferedImage img = toImage(s);

		float y = this.getY(j, z);
		float x = this.getXRight(i, z);

		if (this.invertEnabled) {
			if (this.invertH) x = (img.getWidth()-1 - x);
			if (this.invertV) y = (img.getHeight()-1 - y);
		}

		return img.getRGB((int)(x+0.5), (int)(y+0.5));
	}

	@Override
	public int getRGBLeft(int i, int j, float z) {
		if (this.invertEnabled) {
			if (this.invertH) i = (int) invertX(i);
			if (this.invertV) j = (int) invertY(j);
		}

		float y = this.getY(j, z);
		float x = this.getXLeft(i, z);

		if (rasterData != null) return (255<<24) | rasterData[(int)(x+0.5) + (int)(y+0.5) * image.getWidth()];

		return image.getRGB((int)(x+0.5), (int)(y+0.5));
	}
	@Override
	public int getRGBRight(int i, int j, float z) {
		if (this.invertEnabled) {
			if (this.invertH) i = (int) invertX(i);
			if (this.invertV) j = (int) invertY(j);
		}

		float y = this.getY(j, z);
		float x = this.getXRight(i, z);

		if (rasterData2 != null) return (255<<24) | rasterData2[(int)(x+0.5) + (int)(y+0.5) * image2.getWidth()];
		return image2.getRGB((int)(x+0.5), (int)(y+0.5));
	}

	@Override
	public int getWidth() {
		return w;
	}

	@Override
	public int getHeight() {
		return h;
	}

	@Override
	public void setFont(Graphics.FONT f) {
		String n = f.getFontName();
		// Hack to use a font supported by Java in PDF export (later transformed to Symbol again using a font mapping in iText).
		// Symbol is for Greek alphabet
		if (n.equals("Symbol")) n = Font.SERIF;
		int size = f.getSize();
		/*
		if (!renderingToExternalGraphics() && w >= 3000) {
        	int hugeFactor = Math.max((w/1000) - 2, 4);
        	size += hugeFactor * 2; 
        	if (size > 35) size = 35;
		}
		*/
		Font font = new Font(n, f.getType(), size);
		g.setFont(font);
		if (g2 != null) g2.setFont(font);
	}

	@Override
	public Object makeColorTransparent(Object img, int color, boolean fromBlack, boolean toWhite, int t) {
		BufferedImage image = toImage(img);
		if (!fromBlack && !toWhite) {
			Picture pic = new Picture(image);
			pic.makeTransparent(t, new Color(color));
			return pic.getImage();
			//waitUntilImagesAreRead(new Object[] {out});
			//return ((sun.awt.image.ToolkitImage) out).getBufferedImage();
		} else {
			if (fromBlack) {
				int c = ((this.getAlpha(color) >> 24) & 0xff) << 24 | 0<<16 | 0<<8 | 0;
				return Picture.makeTransparent(image, new Color(c), new Color(color), t);
				//waitUntilImagesAreRead(new Object[] {out});
				//return ((sun.awt.image.ToolkitImage) out).getBufferedImage();
			} else {
				int c = ((this.getAlpha(color) >> 24) & 0xff) << 24 | 255<<16 | 255<<8 | 255;
				return Picture.makeTransparent(image, new Color(color), new Color(c), t);
				//waitUntilImagesAreRead(new Object[] {out});
				//return ((sun.awt.image.ToolkitImage) out).getBufferedImage();
			}
		}
	}


	@Override
	public void drawRect(float i, float j, float k, float l) {
		drawRect(i, j, k, l, g, g2);
	}

	private void drawRect(float i, float j, float k, float l, Graphics2D g, Graphics2D g2) {
		if (this.invertEnabled) {
			if (this.invertH) i = invertX(i)-k+1;
			if (this.invertV) j = invertY(j)-l+1;
		}

		if (i == (int)(i) && j == (int) j) {
			if (g != null) g.drawRect((int)i, (int)j, (int) (k-0.5), (int) (l-0.5));
			if (g2 != null) g2.drawRect((int)i, (int)j, (int) (k-0.5), (int) (l-0.5));
			return;
		}
		if (g != null) {
			g.translate(i, j);
			g.drawRect(0, 0, (int)(k-0.5), (int)(l-0.5));
			g.translate(-i, -j);
		}
		if (g2 != null) {
			g2.translate(i, j);
			g2.drawRect(0, 0, (int)(k-0.5), (int)(l-0.5));
			g2.translate(-i, -j);
		}
	}

	@Override
	public void drawRotatedString(String labelDEC, float i, float j, float k) {
		drawRotatedString(labelDEC, i, j, k, g, g2);
	}
	private void drawRotatedString(String labelDEC, float i, float j, float k, Graphics2D g, Graphics2D g2) {
		if (this.invertEnabled) {
			if (this.invertH) i = invertX(i);
			if (this.invertV) j = invertY(j);
		}

		if (g != null) {
			g.translate(i, j);
			g.rotate(-k);
			g.drawString(labelDEC, 0, 0);
			g.rotate(k);
			g.translate(-i, -j);
		}
		if (g2 != null) {
			g2.translate(i, j);
			g2.rotate(-k);
			g2.drawString(labelDEC, 0, 0);
			g2.rotate(k);
			g2.translate(-i, -j);
		}
	}

	@Override
	public Object getImage(Object img, int i, int j, int width, int k) {
		BufferedImage image = toImage(img);

		return image.getSubimage(i, j, width, k);
	}

	@Override
	public Object getImage(int i, int j, int width, int k) {
		if (image == null) return null;
		if (this.invertEnabled) {
			if (this.invertH) i = (int) (invertX(i)-width+1);
			if (this.invertV) j = (int) (invertY(j)-k+1);
		}

		if (i < 0) i = 0;
		if (j < 0) j = 0;
		if (i+width > w) width = w-i;
		if (j+k > h) k = h-j;
		return image.getSubimage(i, j, width, k);
	}

	@Override
	public Object getImage2(int i, int j, int width, int k) {
		if (image2 == null) return null;
		if (this.invertEnabled) {
			if (this.invertH) i = (int) (invertX(i)-width+1);
			if (this.invertV) j = (int) (invertY(j)-k+1);
		}

		if (i < 0) i = 0;
		if (j < 0) j = 0;
		if (i+width > image2.getWidth()) width = image2.getWidth()-i;
		if (j+k > image2.getHeight()) k = image2.getHeight()-j;
		return image2.getSubimage(i, j, width, k);
	}

	@Override
	public void waitUntilImagesAreRead(Object[] objects) {
		MediaTracker tracker = new MediaTracker(new JPanel());
		for (int i=0; i<objects.length; i++) {
			if (objects[i] instanceof Image || objects[i] instanceof BufferedImage)
				tracker.addImage((Image) objects[i], i);
		}
		try
		{
			tracker.waitForAll();
		} catch (Exception e) {
			Logger.log(LEVEL.WARNING, "Could not read image/s completely.");
		}
	}

	@Override
	public Object getColorObject() {
		return g.getColor();
	}

	@Override
	public int getRed(int color) {
		return (color>>16)&255;
	}

	@Override
	public int getGreen(int color) {
		return (color>>8)&255;
	}

	@Override
	public int getBlue(int color) {
		return (color)&255;
	}

	@Override
	public int getAlpha(int color) {
		return (color>>24)&255;
	}

	@Override
	public int invertColor(int color) {
		Color col = new Color(color);
		int r = 255-col.getRed();
		int g = 255-col.getGreen();
		int b = 255-col.getBlue();
		int a = 255-col.getAlpha();
		col = new Color(r, g, b, a);
		return col.getRGB();
	}

	@Override
	public void setColor(int col, int alpha) {
		if (!transparencyEnabled) alpha = 255;

		int r = this.getRed(col);
		int g = this.getGreen(col);
		int b = this.getBlue(col);
		if (alpha == lastA && r == lastR && g == lastG && b == lastB) return;

		Color color = new Color(r, g, b, alpha);
		this.g.setColor(color);
		if (g2 != null) g2.setColor(color);

		lastR = r;
		lastG = g;
		lastB = b;
		lastA = alpha;
		lastRGB = lastA << 24 | lastR<<16 | lastG<<8 | lastB;
	}

	@Override
	public Object getColorInvertedImage(Object image) {
		Picture p = new Picture(toImage(image));
		p.invertColors();
		return p.getImage();
	}

	@Override
	public Object getColorInvertedImage(Object image, boolean r, boolean g, boolean b) {
		Picture p = new Picture(toImage(image));
		p.invertColors(r, g, b);
		return p.getImage();
	}

	// GeneralPath methods

	@Override
	public Object generalPathInitialize() {
		return new GeneralPath(criteria);
	}

	@Override
	public void generalPathMoveTo(Object obj, float x, float y) {
		if (this.invertEnabled) {
			if (this.invertH) x = invertX(x);
			if (this.invertV) y = invertY(y);
		}

		((GeneralPath) obj).moveTo(x, y);
	}

	@Override
	public void generalPathLineTo(Object obj, float x, float y) {
		if (this.invertEnabled) {
			if (this.invertH) x = invertX(x);
			if (this.invertV) y = invertY(y);
		}

		((GeneralPath) obj).lineTo(x, y);
	}

	@Override
	public void generalPathQuadTo(Object obj, float x1, float y1, float x2,
			float y2) {
		if (this.invertEnabled) {
			if (this.invertH) {
				x1 = invertX(x1);
				x2 = invertX(x2);
			}
			if (this.invertV) {
				y1 = invertY(y1);
				y2 = invertY(y2);
			}
		}

		((GeneralPath) obj).quadTo(x1, y1, x2, y2);
	}

	@Override
	public void generalPathCurveTo(Object obj, float x1, float y1, float x2,
			float y2, float x3, float y3) {
		if (this.invertEnabled) {
			if (this.invertH) {
				x1 = invertX(x1);
				x2 = invertX(x2);
				x3 = invertX(x3);
			}
			if (this.invertV) {
				y1 = invertY(y1);
				y2 = invertY(y2);
				y3 = invertY(y3);
			}
		}

		((GeneralPath) obj).curveTo(x1, y1, x2, y2, x3, y3);
	}

	@Override
	public void generalPathClosePath(Object obj) {
		((GeneralPath) obj).closePath();
	}

	// Anaglyph methods

	@Override
	public void fillOvalAnaglyphLeft(float i, float j, float k, float l, float dist) {
		float ya = this.getY(j, dist);
		fillOval(this.getXLeft(i, dist), ya, k, l, g, null);
	}

	@Override
	public void fillOvalAnaglyphRight(float i, float j, float k, float l, float dist) {
		float ya = this.getY(j, dist);
		fillOval(this.getXRight(i, dist), ya, k, l, null, g2);
	}

	@Override
	public void fillOval(float i, float j, float k, float l, float dist) {
		if (this.colorMode == Graphics.ANAGLYPH_COLOR_MODE.NO_ANAGLYPH || dist == this.colorMode.getReferenceZ()) {
			fillOval(i, j, k, l, false);
			return;
		}

		float ya = this.getY(j, dist);
		if (g2 == null) {
			Color c = g.getColor();
			g.setColor(this.getColorLeft());
			fillOval(this.getXLeft(i, dist), ya, k, l, false);
			g.setColor(this.getColorRight());
			fillOval(this.getXRight(i, dist), ya, k, l, false);
			g.setColor(c);
		} else {
			fillOval(this.getXLeft(i, dist), ya, k, l, g, null);
			fillOval(this.getXRight(i, dist), ya, k, l, null, g2);
		}
	}

	@Override
	public void drawLine(float i, float j, float k, float l, float dist1, float dist2) {
		if (this.colorMode == Graphics.ANAGLYPH_COLOR_MODE.NO_ANAGLYPH ||
				(dist1 == this.colorMode.getReferenceZ() && dist2 == this.colorMode.getReferenceZ())) {
			drawLine(i, j, k, l, false);
			return;
		}

		float ya1 = this.getY(j, dist1);
		float ya2 = this.getY(l, dist2);
		if (g2 == null) {
			Color c = g.getColor();
			g.setColor(this.getColorLeft());
			drawLine(this.getXLeft(i, dist1), ya1, this.getXLeft(k, dist2), ya2, false);
			g.setColor(this.getColorRight());
			drawLine(this.getXRight(i, dist1), ya1, this.getXRight(k, dist2), ya2, false);
			g.setColor(c);
		} else {
//			int clip[] = this.getClip();
			drawLine(this.getXLeft(i, dist1), ya1, this.getXLeft(k, dist2), ya2, g, null, false); //, clip);
			drawLine(this.getXRight(i, dist1), ya1, this.getXRight(k, dist2), ya2, null, g2, false); //, clip);
		}
	}

	@Override
	public void drawStraightLine(float i, float j, float k, float l, float dist1, float dist2) {
		if (this.colorMode == Graphics.ANAGLYPH_COLOR_MODE.NO_ANAGLYPH ||
				(dist1 == this.colorMode.getReferenceZ() && dist2 == this.colorMode.getReferenceZ())) {
			drawStraightLine(i, j, k, l);
			return;
		}

		float ya1 = this.getY(j, dist1);
		float ya2 = this.getY(l, dist2);
		if (g2 == null) {
			Color c = g.getColor();
			g.setColor(this.getColorLeft());
			drawStraightLine(this.getXLeft(i, dist1), ya1, this.getXLeft(k, dist2), ya2);
			g.setColor(this.getColorRight());
			drawStraightLine(this.getXRight(i, dist1), ya1, this.getXRight(k, dist2), ya2);
			g.setColor(c);
		} else {
			drawStraightLine(this.getXLeft(i, dist1), ya1, this.getXLeft(k, dist2), ya2);
		}
	}

	@Override
	public void drawLines(int i[], int j[], int np, float dist[], boolean fastMode) {
		if (this.colorMode == Graphics.ANAGLYPH_COLOR_MODE.NO_ANAGLYPH ||
				(dist == null || (dist[0] == this.colorMode.getReferenceZ() && dist[1] == this.colorMode.getReferenceZ()))) {
			drawLines(i, j, np, fastMode);
			return;
		}

		Color c = g.getColor();
//		int clip[] = this.getClip();
		for (int k=0; k<np-1; k++) {
			float ya1 = this.getY(j[k], dist[k]);
			float ya2 = this.getY(j[k+1], dist[k+1]);
			if (g2 == null) {
				g.setColor(this.getColorLeft());
				drawLine(this.getXLeft(i[k], dist[k]), ya1, this.getXLeft(i[k+1], dist[k+1]), ya2, false);
				g.setColor(this.getColorRight());
				drawLine(this.getXRight(i[k], dist[k]), ya1, this.getXRight(i[k+1], dist[k+1]), ya2, false);
			} else {
				drawLine(this.getXLeft(i[k], dist[k]), ya1, this.getXLeft(i[k+1], dist[k+1]), ya2, g, null, false); //, clip);
				drawLine(this.getXRight(i[k], dist[k]), ya1, this.getXRight(i[k+1], dist[k+1]), ya2, null, g2, false); //, clip);
			}
		}
		g.setColor(c);
	}

	@Override
	public void drawOval(float i, float j, float k, float l, float dist) {
		if (this.colorMode == Graphics.ANAGLYPH_COLOR_MODE.NO_ANAGLYPH || dist == this.colorMode.getReferenceZ()) {
			drawOval(i, j, k, l, false);
			return;
		}

		float ya = this.getY(j, dist);
		if (g2 == null) {
			Color c = g.getColor();
			g.setColor(this.getColorLeft());
			drawOval(this.getXLeft(i, dist), ya, k, l, false);
			g.setColor(this.getColorRight());
			drawOval(this.getXRight(i, dist), ya, k, l, false);
			g.setColor(c);
		} else {
			drawOval(this.getXLeft(i, dist), ya, k, l, g, null, false);
			drawOval(this.getXRight(i, dist), ya, k, l, null, g2, false);
		}
	}

	@Override
	public void fill(Object s, float z) {
		if (this.colorMode == Graphics.ANAGLYPH_COLOR_MODE.NO_ANAGLYPH || z == this.colorMode.getReferenceZ()) {
			this.fill(s);
			return;
		}

		if (z > 2*this.colorMode.getReferenceZ()) z = 2 * this.colorMode.getReferenceZ();
		if (z < 0) z = 0;
		z = (z - this.colorMode.getReferenceZ()) * this.colorMode.getEyeSeparation() * 0.5f;
		if (g2 == null) {
			Color c = g.getColor();
			g.setColor(this.getColorLeft());
			g.translate(-z, 0);
			g.fill((GeneralPath) s);
			g.setColor(this.getColorRight());
			g.translate(2 * z, 0);
			g.fill((GeneralPath) s);
			g.translate(-z, 0);
			g.setColor(c);
		} else {
			g.translate(-z, 0);
			g.fill((GeneralPath) s);
			g.translate(z, 0);
			g2.translate(z, 0);
			g2.fill((GeneralPath) s);
			g2.translate(-z, 0);
		}
	}

	@Override
	public void drawString(String str, float x, float y, float z) {
		if (this.colorMode == Graphics.ANAGLYPH_COLOR_MODE.NO_ANAGLYPH || z == this.colorMode.getReferenceZ()) {
			this.drawString(str, x, y);
			return;
		}

		float ya = this.getY(y, z);
		if (g2 == null) {
			Color c = g.getColor();
			g.setColor(this.getColorLeft());
			drawString(str, this.getXLeft(x, z), ya);
			g.setColor(this.getColorRight());
			drawString(str, this.getXRight(x, z), ya);
			g.setColor(c);
		} else {
			drawString(str, this.getXLeft(x, z), ya, g, null);
			drawString(str, this.getXRight(x, z), ya, g2, null);
		}
	}

	@Override
	public void drawImage(Object img, float x, float y, float z) {
		drawImage(img, x, y, z, 1.0, 1.0);
	}

	@Override
	public void drawImage(Object img, float x, float y, float z, double scalex, double scaley) {
		if (g2 == null || this.colorMode == Graphics.ANAGLYPH_COLOR_MODE.NO_ANAGLYPH || z == this.colorMode.getReferenceZ()) {
			drawImage(img, x, y, scalex, scaley);
			return;
		}

		if (this.invertEnabled) {
			if (this.invertH) x = invertX(x);
			if (this.invertV) y = invertY(y);
		}

		if (z > 2*this.colorMode.getReferenceZ()) z = 2 * this.colorMode.getReferenceZ();
		if (z < 0) z = 0;
		z = (z - this.colorMode.getReferenceZ()) * this.colorMode.getEyeSeparation() * 0.5f;
/*		if (g2 == null) {
			Color c = g.getColor();
			g.setColor(this.getColorLeft());
			g.translate(z, 0);
			drawImage(img, x, y, scalex, scaley, g, g2);
			g.setColor(this.getColorRight());
			g.translate(-2*z, 0);
			drawImage(img, x, y, scalex, scaley, g, g2);
			g.translate(z, 0);
			g.setColor(c);
		} else {
*/			g.translate(-z, 0);
			g2.translate(z, 0);
			drawImage(img, x, y, scalex, scaley, g, g2);
			g.translate(z, 0);
			g2.translate(-z, 0);
//		}

	}

	@Override
	public void fillRect(float i, float j, float k, float l, float dist) {
		if (this.colorMode == Graphics.ANAGLYPH_COLOR_MODE.NO_ANAGLYPH || dist == this.colorMode.getReferenceZ()) {
			fillRect(i, j, k, l);
			return;
		}

		float ya = this.getY(j, dist);
		if (g2 == null) {
			Color c = g.getColor();
			g.setColor(this.getColorLeft());
			fillRect(this.getXLeft(i, dist), ya, k, l);
			g.setColor(this.getColorRight());
			fillRect(this.getXRight(i, dist), ya, k, l);
			g.setColor(c);
		} else {
			fillRect(this.getXLeft(i, dist), ya, k, l, g, null);
			fillRect(this.getXRight(i, dist), ya, k, l, null, g2);
		}
	}

	@Override
	public void draw(Object s, float z) {
		if (this.colorMode == Graphics.ANAGLYPH_COLOR_MODE.NO_ANAGLYPH || z == this.colorMode.getReferenceZ()) {
			this.draw(s);
			return;
		}

		if (z > 2*this.colorMode.getReferenceZ()) z = 2 * this.colorMode.getReferenceZ();
		if (z < 0) z = 0;
		z = (z - this.colorMode.getReferenceZ()) * this.colorMode.getEyeSeparation() * 0.5f;
		if (g2 == null) {
			Color c = g.getColor();
			g.setColor(this.getColorLeft());
			g.translate(-z, 0);
			g.draw((GeneralPath) s);
			g.setColor(this.getColorRight());
			g.translate(2 * z, 0);
			g.draw((GeneralPath) s);
			g.translate(-z, 0);
			g.setColor(c);
		} else {
			g.translate(-z, 0);
			g.draw((GeneralPath) s);
			g.translate(z, 0);
			g2.translate(z, 0);
			g2.draw((GeneralPath) s);
			g2.translate(-z, 0);
		}
	}

	@Override
	public void drawRect(float i, float j, float k, float l, float dist) {
		if (this.colorMode == Graphics.ANAGLYPH_COLOR_MODE.NO_ANAGLYPH || dist == this.colorMode.getReferenceZ()) {
			drawRect(i, j, k, l);
			return;
		}

		if (g2 == null) {
			Color c = g.getColor();
			g.setColor(this.getColorLeft());
			drawRect(this.getXLeft(i, dist), this.getY(j, dist), k, l);
			g.setColor(this.getColorRight());
			drawRect(this.getXRight(i, dist), this.getY(j, dist), k, l);
			g.setColor(c);
		} else {
			drawRect(this.getXLeft(i, dist), this.getY(j, dist), k, l, g, null);
			drawRect(this.getXRight(i, dist), this.getY(j, dist), k, l, null, g2);
		}
	}

	@Override
	public void drawRotatedString(String label, float i, float j, float k,
			float z) {
		if (g2 == null || this.colorMode == Graphics.ANAGLYPH_COLOR_MODE.NO_ANAGLYPH || z == this.colorMode.getReferenceZ()) {
			drawRotatedString(label, i, j, k, g, g2);
			return;
		}

		if (this.invertEnabled) {
			if (this.invertH) i = invertX(i);
			if (this.invertV) j = invertY(j);
		}

		if (z > 2*this.colorMode.getReferenceZ()) z = 2 * this.colorMode.getReferenceZ();
		if (z < 0) z = 0;
		float r = (z - this.colorMode.getReferenceZ()) * this.colorMode.getEyeSeparation() * 0.5f;
		g.translate(i+r, j);
		g.rotate(-k);
		g.drawString(label, 0, 0);
		g.rotate(k);
		g.translate(-i-r, -j);
		g2.translate(i-r, j);
		g2.rotate(-k);
		g2.drawString(label, 0, 0);
		g2.rotate(k);
		g2.translate(-i+r, -j);
	}



	private Color getColorRight() {
		if (colorMode == ANAGLYPH_COLOR_MODE.GREEN_RED) return new Color(0, 239, 0, 128);
		return new Color(255, 0, 0, 128);
	}
	private Color getColorLeft() {
		if (colorMode == Graphics.ANAGLYPH_COLOR_MODE.GREEN_RED) return new Color(255, 0, 0, 128);
		return new Color(0, 255, 255, 128);
	}
	private float getXRight(float x, float z) {
		if (z > 2*this.colorMode.getReferenceZ()) z = 2 * this.colorMode.getReferenceZ();
		if (z < 0) z = 0;
		float dx = (0.5f + z - this.colorMode.getReferenceZ()) * this.colorMode.getEyeSeparation() * 0.5f;
		if (this.invertEnabled && this.invertH) return x - dx;
		return x + dx;
//		if (x == (int) x) out = (int) (out+0.5);
	}
	private float getXLeft(float x, float z) {
		if (z > 2*this.colorMode.getReferenceZ()) z = 2 * this.colorMode.getReferenceZ();
		if (z < 0) z = 0;
		float dx = (0.5f + z - this.colorMode.getReferenceZ()) * this.colorMode.getEyeSeparation() * 0.5f;
		if (this.invertEnabled && this.invertH) return x + dx;
		return x - dx;
//		if (x == (int) x) out = (int) (out+0.5);
	}
	private float getY(float y, float z) {
		return y;
	}

	@Override
	  public Object blendImagesToAnaglyphMode(Object l, Object r)
	  {
		BufferedImage leftImg = toImage(l);
		BufferedImage rightImg = toImage(r);

		if (colorMode == ANAGLYPH_COLOR_MODE.TRUE_3D_MODE_LEFT_RIGHT) {
			 BufferedImage buf = new BufferedImage(w*2, h, BufferedImage.TYPE_INT_ARGB);
			 Graphics2D g = buf.createGraphics();
			 g.drawImage(leftImg, 0, 0, null);
			 g.drawImage(rightImg, w, 0, null);
			 g.dispose();
			 return buf;
		}

		if (colorMode == ANAGLYPH_COLOR_MODE.TRUE_3D_MODE_LEFT_RIGHT_HALF_WIDTH) {
			 BufferedImage buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			 Graphics2D g = buf.createGraphics();
			 g.drawImage((BufferedImage)this.getScaledImage(leftImg, w/2, h, false, false), 0, 0, null);
			 g.drawImage((BufferedImage)this.getScaledImage(rightImg, w/2, h, false, false), w/2, 0, null);
			 g.dispose();
			 return buf;
		}

		 float[] matrixLeft = Graphics.duboisRC_left, matrixRight = Graphics.duboisRC_right;
		 if (colorMode == Graphics.ANAGLYPH_COLOR_MODE.DUBOIS_AMBER_BLUE) {
			 matrixLeft = Graphics.duboisAB_left;
			 matrixLeft = Graphics.duboisAB_right;
		 } else {
			 if (colorMode == Graphics.ANAGLYPH_COLOR_MODE.DUBOIS_GREEN_MAGENTA) {
				 matrixLeft = Graphics.duboisGM_left;
				 matrixLeft = Graphics.duboisGM_right;
			 }
		 }

		 int[] rgb1 = ((DataBufferInt) leftImg.getRaster().getDataBuffer()).getData();
		 int[] rgb2 = ((DataBufferInt) rightImg.getRaster().getDataBuffer()).getData();

		 for (int x=0; x<rgb1.length; x++) {
			 rgb1[x] = combine(rgb1[x], rgb2[x], matrixLeft, matrixRight);
		 }
		 return leftImg;
	  }

	  private int combine(int rgb1, int rgb2, float matrixLeft[], float matrixRight[]) {
			 int r1 = (rgb1 >> 16) & 0xff;
			 int g1 = (rgb1 >> 8) & 0xff;
			 int b1 = rgb1 & 0xff;

			 int r2 = (rgb2 >> 16) & 0xff;
			 int g2 = (rgb2 >> 8) & 0xff;
			 int b2 = rgb2 & 0xff;

			 // For CRT/Plasma displays and no gamma correction, Dubois 2009
			 float ra1 = matrixLeft[0] * r1 + matrixLeft[3] * g1 + matrixLeft[6] * b1;
			 float ga1 = matrixLeft[1] * r1 + matrixLeft[4] * g1 + matrixLeft[7] * b1;
			 float ba1 = matrixLeft[2] * r1 + matrixLeft[5] * g1 + matrixLeft[8] * b1;

			 float ra2 = matrixRight[0] * r2 + matrixRight[3] * g2 + matrixRight[6] * b2;
			 float ga2 = matrixRight[1] * r2 + matrixRight[4] * g2 + matrixRight[7] * b2;
			 float ba2 = matrixRight[2] * r2 + matrixRight[5] * g2 + matrixRight[8] * b2;

			 if (ra1 > 255) ra1 = 255;
			 if (ra1 < 0) ra1 = 0;
			 if (ga1 > 255) ga1 = 255;
			 if (ga1 < 0) ga1 = 0;
			 if (ba1 > 255) ba1 = 255;
			 if (ba1 < 0) ba1 = 0;

			 if (ra2 > 255) ra2 = 255;
			 if (ra2 < 0) ra2 = 0;
			 if (ga2 > 255) ga2 = 255;
			 if (ga2 < 0) ga2 = 0;
			 if (ba2 > 255) ba2 = 255;
			 if (ba2 < 0) ba2 = 0;

			 int rb = (int) (0.5 + ra1 + ra2);
			 int gb = (int) (0.5 + ga1 + ga2);
			 int bb = (int) (0.5 + ba1 + ba2);

			 if (rb > 255) rb = 255;
			 if (rb < 0) rb = 0;
			 if (gb > 255) gb = 255;
			 if (gb < 0) gb = 0;
			 if (bb > 255) bb = 255;
			 if (bb < 0) bb = 0;

			 return 255 << 24 | rb<<16 | gb<<8 | bb;
	  }

	  /**
	   * Converts a {@linkplain JPARSECStroke} into its Java implementation.
	   * @param s The JPARSEC stroke.
	   * @return The {@linkplain BasicStroke} Java implementation.
	   */
	  public static BasicStroke getStroke(JPARSECStroke s) {
		  BasicStroke bs = new BasicStroke(s.getLineWidth(), s.getEndCap(), s.getLineJoin(), s.getMiterLimit(), s.getDashArray(), s.getDashPhase());
		  return bs;
	  }

	  /**
	   * Converts a {@linkplain BasicStroke} into its JPARSEC implementation.
	   * @param bs The BasicStroke stroke.
	   * @return The {@linkplain JPARSECStroke} Java implementation.
	   */
	  public static JPARSECStroke getStroke(BasicStroke bs) {
		  JPARSECStroke s = new JPARSECStroke(bs.getLineWidth(), bs.getEndCap(), bs.getLineJoin(), bs.getMiterLimit(), bs.getDashArray(), bs.getDashPhase());
		  return s;
	  }

	  /**
	   * Rotates a Shape around its center using Shape's coordinates.
	   * @param s The shape.
	   * @param angle The angle to rotate in radians.
	   * @return The new shape.
	   */
	  public static Shape rotateShape(Shape s, double angle) {
		  return AffineTransform.getRotateInstance(angle, s.getBounds().getCenterX(), s.getBounds().getCenterY()).createTransformedShape(s);
	  }

	  /**
	   * Rotates a Shape around its center using the original Graphics2D coordinates.
	   * The transform eventually used in the Graphics2D is reverted, then the rotation
	   * is done, and then the transform is applied again. The Graphics2D is not modified,
	   * only the output shape.
	   * @param g The Graphics2D object.
	   * @param s The shape.
	   * @param angle The angle to rotate in radians.
	   * @return The new shape.
	   * @throws JPARSECException If the rotation cannot be applied to this shape.
	   */
	  public static Shape rotateShape(Graphics2D g, Shape s, double angle) throws JPARSECException {
		  try {
			  AffineTransform at = g.getTransform();
			  AffineTransform atf = g.getTransform().createInverse();
			  PathIterator pi = s.getPathIterator(at);
			  double c[] = new double[6];
			  GeneralPath path = new GeneralPath();
			  double x0 = s.getBounds().getCenterX(), y0 = s.getBounds().getCenterY();
			  java.awt.geom.Point2D p0 = at.transform(new java.awt.geom.Point2D.Double(x0, y0), new java.awt.geom.Point2D.Double());
			  x0 = p0.getX();
			  y0 = p0.getY();
			  while (!pi.isDone()) {
				  int type = pi.currentSegment(c);
				  if (type == PathIterator.SEG_CLOSE) {
					  path.closePath();
					  pi.next();
					  continue;
				  }
				  double dx = c[0] - x0, dy = c[1] - y0;
				  double r = FastMath.hypot(dx, dy);
				  double ang = FastMath.atan2_accurate(dy, dx);
				  c[0] = x0 + r * FastMath.cos(ang + angle);
				  c[1] = y0 + r * FastMath.sin(ang + angle);
				  java.awt.geom.Point2D p = atf.transform(new java.awt.geom.Point2D.Double(c[0], c[1]), new java.awt.geom.Point2D.Double());
				  c[0] = p.getX();
				  c[1] = p.getY();

				  if (type == PathIterator.SEG_MOVETO) path.moveTo(c[0], c[1]);
				  if (type == PathIterator.SEG_LINETO) path.lineTo(c[0], c[1]);
				  if (type == PathIterator.SEG_QUADTO) {
					  dx = c[2] - x0;
					  dy = c[3] - y0;
					  r = FastMath.hypot(dx, dy);
					  ang = FastMath.atan2_accurate(dy, dx);
					  c[2] = x0 + r * FastMath.cos(ang + angle);
					  c[3] = y0 + r * FastMath.sin(ang + angle);
					  p = atf.transform(new java.awt.geom.Point2D.Double(c[2], c[3]), new java.awt.geom.Point2D.Double());
					  c[2] = p.getX();
					  c[3] = p.getY();
					  path.quadTo(c[0], c[1], c[2], c[3]);
				  }
				  if (type == PathIterator.SEG_CUBICTO) {
					  dx = c[2] - x0;
					  dy = c[3] - y0;
					  r = FastMath.hypot(dx, dy);
					  ang = FastMath.atan2_accurate(dy, dx);
					  c[2] = x0 + r * FastMath.cos(ang + angle);
					  c[3] = y0 + r * FastMath.sin(ang + angle);
					  p = atf.transform(new java.awt.geom.Point2D.Double(c[2], c[3]), new java.awt.geom.Point2D.Double());
					  c[2] = p.getX();
					  c[3] = p.getY();
					  dx = c[4] - x0;
					  dy = c[5] - y0;
					  r = FastMath.hypot(dx, dy);
					  ang = FastMath.atan2_accurate(dy, dx);
					  c[4] = x0 + r * FastMath.cos(ang + angle);
					  c[5] = y0 + r * FastMath.sin(ang + angle);
					  p = atf.transform(new java.awt.geom.Point2D.Double(c[4], c[5]), new java.awt.geom.Point2D.Double());
					  c[4] = p.getX();
					  c[5] = p.getY();
					  path.curveTo(c[0], c[1], c[2], c[3], c[4], c[5]);
				  }
				  pi.next();
			  };
			  return path;
		  } catch (Exception exc) {
			  throw new JPARSECException("Cannot apply the rotation.", exc);
		  }
	  }

		/**
		 * Enables antialiasing for a given Graphics instance.
		 * @param g The Graphics instance.
		 */
		public static void enableAntialiasing(Graphics2D g) {
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		}

		/**
		 * Disables antialiasing for a given Graphics instance.
		 * @param g The Graphics instance.
		 */
		public static void disableAntialiasing(Graphics2D g) {
	        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
	        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		}

	  @Override
	  public Object getDirectGraphics() {
		  return g;
	  }
	  @Override
	  public Object getDirectGraphics2() {
		  return g2;
	  }

	  private BufferedImage toImage(Object img) {
			if (img instanceof BufferedImage) {
				return (BufferedImage) img;
			} else {
				ImageIcon imgIcon = new ImageIcon((Image) img);
				BufferedImage image = new BufferedImage(imgIcon.getIconWidth(), imgIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
				image.getGraphics().drawImage(imgIcon.getImage(), 0, 0, null);
				return image;
			}
	  }

	@Override
	public boolean renderingToAndroid() {
		return false;
	}

	@Override
	public void drawPoint(int i, int j, int color) {
		if (this.invertEnabled) {
			if (this.invertH) {
				i = (int) invertX(i);
			}
			if (this.invertV) {
				j = (int) invertY(j);
			}
		}

		int[] rec = this.getClip();
		if (i >= rec[0] && j >= rec[1] && i < rec[0]+rec[2]-1 && j < rec[1]+rec[3]-1) {
	    	int alpha = this.getAlpha(color);
	    	if (alpha != 255) { // Fake transparency. k, l < 0 => real transparency using Java2D's fillOval
	    		int red = this.getRed(color);
	    		int green = this.getGreen(color);
	    		int blue = this.getBlue(color);
	    		int col1 = getImageRGB(3, 3);
	    		int red1 = this.getRed(col1);
	    		int green1 = this.getGreen(col1);
	    		int blue1 = this.getBlue(col1);
	    		float masking_factor = 1.2f-(alpha)/255.0f;
	    		if (green1 > 150) masking_factor += 0.15f;
	    		red = (int) (red * (1 - masking_factor) + red1 * masking_factor);
	    		green = (int) (green * (1 - masking_factor) + green1 * masking_factor);
	    		blue = (int) (blue * (1 - masking_factor) + blue1 * masking_factor);
	    		color = 255<<24 | red<<16 | green<<8 | blue;
	    	}

			setImageRGB(i, j, color);
			if (g2 != null) setImageRGB2(i, j, color);
		}
	}

	private double ang = 0, tx = 0, ty = 0;
	@Override
	public void rotate(double radians) {
		if (g != null || g2 != null) ang = radians;
		if (g != null) g.rotate(radians);
		if (g2 != null) g2.rotate(radians);
	}

	@Override
	public void traslate(double x, double y) {
		if (g != null || g2 != null) {
			tx = x;
			ty = y;
		}
		if (g != null) g.translate(x, y);
		if (g2 != null) g2.translate(x, y);
	}

	@Override
	public double getRotation() {
		return ang;
	}

	@Override
	public double[] getTranslation() {
		return new double[] {tx, ty};
	}
}

class EllipseBresenhamInterpolator {
    /**
     * Current dx value.
     */
    public int dx;
    /**
     * Current m_dy value.
     */
    public int dy;

    private int m_rx2;
    private int m_ry2;
    private int m_two_rx2;
    private int m_two_ry2;
    private int m_inc_x;
    private int m_inc_y;
    private int m_cur_f;
    public int m_dx;
    public int m_dy;


    /**
     * Creates a new instance.
     */
    public EllipseBresenhamInterpolator(int rx, int ry) {
        m_rx2 = rx * rx;
        m_ry2 = ry * ry;
        m_two_rx2 = m_rx2 << 1;
        m_two_ry2 = m_ry2 << 1;
        m_dx = 0;
        m_dy = 0;
        m_inc_x = 0;
        m_inc_y = -ry * m_two_rx2;
        m_cur_f = 0;

        dx = 0;
        dy = -ry;
    }

    public boolean hasNext() {
        return dy < 0;
    }

    public void next() {
        int mx, my, mxy, min_m;
        int fx, fy, fxy;

        mx = fx = m_cur_f + m_inc_x + m_ry2;
        if (mx < 0) mx = -mx;

        my = fy = m_cur_f + m_inc_y + m_rx2;
        if (my < 0) my = -my;

        mxy = fxy = m_cur_f + m_inc_x + m_ry2 + m_inc_y + m_rx2;
        if (mxy < 0) mxy = -mxy;

        min_m = mx;
        boolean flag = true;

        if (min_m > my) {
            min_m = my;
            flag = false;
        }

        m_dx = m_dy = 0;

        if(min_m > mxy) {
            m_inc_x += m_two_ry2;
            m_inc_y += m_two_rx2;
            m_cur_f = fxy;
            m_dx = 1;
            m_dy = 1;

            dx += m_dx;
            dy += m_dy;

            return;
        }

        if(flag) {
            m_inc_x += m_two_ry2;
            m_cur_f = fx;
            m_dx = 1;

            dx += m_dx;
            dy += m_dy;
            return;
        }

        m_inc_y += m_two_rx2;
        m_cur_f = fy;
        m_dy = 1;

            dx += m_dx;
            dy += m_dy;
    }
}
