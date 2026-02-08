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
package jparsec.io.image;

import java.net.*;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.Queue;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.awt.image.*;

import javax.swing.*;

import jparsec.ephem.Functions;
import jparsec.graph.TextLabel;
import jparsec.io.SystemClipboard;
import jparsec.math.Constant;
import jparsec.util.JPARSECException;
import jparsec.util.Logger;
import jparsec.util.Logger.LEVEL;

/**
*  Draw provides a basic capability for
*  creating drawings. It uses a simple graphics model that
*  allows the creation of drawings consisting of points, lines, and curves
*  in a window. A JPanel can be obtained (see {@linkplain Draw#getComponent(BufferedImage)})
*  that allows to draw by hand using the mouse.
*  <P>
*  This class is based on <i>Introduction to Programming in Java: An
*  Interdisciplinary Approach, Spring 2007 preliminary version</i>,
*  section 1.5, and <a href="http://www.cs.princeton.edu/introcs/15inout">
*  http://www.cs.princeton.edu/introcs/15inout</a>.
*
*  @author T. Alonso Albi - OAN (Spain)
*  @version 1.0
*/
public class Draw {

   // default colors
   private static final Color DEFAULT_PEN_COLOR = Color.BLACK;
   private static final Color DEFAULT_CLEAR_COLOR = new Color(0, 0, 0, 0);

   // current pen color
   private Color penColor;

   // canvas size
   private int width, height;

   // default pen radius
   private static final double DEFAULT_PEN_RADIUS = 0.005;

   // current pen radius
   private double penRadius;

   // boundary of drawing canvas, 0% border
   private static final double BORDER = 0.0;
   private static final double DEFAULT_XMIN = 0.0;
   private static final double DEFAULT_XMAX = 1.0;
   private static final double DEFAULT_YMIN = 0.0;
   private static final double DEFAULT_YMAX = 1.0;
   private static double xmin, ymin, xmax, ymax;

   // default font
   private static final Font DEFAULT_FONT = new Font("SansSerif", Font.PLAIN, 16);

   // current font
   private Font font;

   // double buffered graphics
   private BufferedImage screenImage;
   private static BufferedImage screenImageBackUp;
   private Graphics2D screenGraphics;

   private String title;
   /**
    * Constructor for a draw.
    */
   public Draw() {
	   init(null);
   }

   /**
    * Constructor for a draw with an
    * external graphics supplied. In this case
    * the image is null and you cannot retrieve it.
    * @param g The external graphics.
    * @param width Width.
    * @param height Height.
    */
   public Draw(Graphics2D g, int width, int height) {
	   this.width = width;
	   this.height = height;
	   init(g);
   }

   /**
    * Constructor for a draw.
    * @param width Width.
    * @param height Height.
    */
   public Draw(int width, int height) {
	   this.width = width;
	   this.height = height;
	   init(null);
   }
   /**
    * Constructor for a draw.
    * @param title A title for the frame.
    * @param width Width.
    * @param height Height.
    */
   public Draw(String title, int width, int height) {
	   this.width = width;
	   this.height = height;
	   this.title = title;
	   init(null);
   }

   /**
    * Set the window size to w-by-h pixels. This method cannot
    * be called in case the draw is done to an external graphics.
    *
    * @param w the width as a number of pixels.
    * @param h the height as a number of pixels.
    * @param clear True to clear the draw, false to retrieve
    * all previous things drawn.
    * @throws JPARSECException If the size is invalid.
    */
   public void setCanvasSize(int w, int h, boolean clear) throws JPARSECException {
       if (w < 1 || h < 1) throw new JPARSECException("width and height must be positive");
       BufferedImage copy = Picture.copy(screenImage);
       width = w;
       height = h;
       init(null);
       if (!clear) screenGraphics.drawImage(copy, 0, 0, null);
   }

   private void init(Graphics2D g)
   {
	   if (g == null) {
		   screenImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		   screenGraphics = screenImage.createGraphics();
	   } else {
		   screenImage = null;
		   screenGraphics = g;
	   }
       setXscale();
       setYscale();
       clear();
       setPenColor(DEFAULT_PEN_COLOR);
       try {
    	   setPenRadius(DEFAULT_PEN_RADIUS);
       } catch (Exception exc) {
    	   Logger.log(LEVEL.ERROR, "This error should never happen.");
       }
       setFont(DEFAULT_FONT);

       // add antialiasing
       RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
                                                 RenderingHints.VALUE_ANTIALIAS_ON);
       hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
       screenGraphics.addRenderingHints(hints);
   }

   /**
    * Returns the screen graphics.
    * @return The Graphics object.
    */
   public Graphics2D getOffScreenGraphics() {
	   return screenGraphics;
   }

   /**
    * Returns the image. Null is returned in case the draw is done to
    * an external graphics.
    * @return The image.
    */
   public BufferedImage getOffScreenImage() {
	   return screenImage;
   }

  /*************************************************************************
   *  User and screen coordinate systems
   *************************************************************************/

   /**
    * Set the X scale to be the default.
    */
   public void setXscale() { setXscale(DEFAULT_XMIN, DEFAULT_XMAX); }
   /**
    * Set the Y scale to be the default.
    */
   public void setYscale() { setYscale(DEFAULT_YMIN, DEFAULT_YMAX); }
   /**
    * Set the X scale.
    * @param min the minimum value of the X scale.
    * @param max the maximum value of the X scale.
    */
   public void setXscale(double min, double max) {
       double size = max - min;
       xmin = min - BORDER * size;
       xmax = max + BORDER * size;
   }
   /**
    * Set the Y scale.
    * @param min the minimum value of the Y scale.
    * @param max the maximum value of the Y scale.
    */
   public void setYscale(double min, double max) {
       double size = max - min;
       ymin = min - BORDER * size;
       ymax = max + BORDER * size;
   }

   // helper functions that scale from user coordinates to screen coordinates and back
   private double  scaleX(double x) { return (width - 1)  * (x - xmin) / (xmax - xmin); }
   private double  scaleY(double y) { return (height - 1) * (ymax - y) / (ymax - ymin); }
   private double factorX(double w) { return w * width  / Math.abs(xmax - xmin);  }
   private double factorY(double h) { return h * height / Math.abs(ymax - ymin);  }


   /**
    * Clear the screen image and make it transparent.
    */
   public void clear() {
	   clear(DEFAULT_CLEAR_COLOR);
   }
   /**
    * Clear the screen with the given color.
    * @param color the Color to make the background.
    */
   public void clear(Color color) {
	   Color c = screenGraphics.getColor();
       screenGraphics.setBackground(color);
       screenGraphics.clearRect(0, 0, width, height);
       screenGraphics.setColor(c);
   }

   /**
    * Set the pen size to the given size.
    * @param r the radius of the pen.
    * @throws JPARSECException if r is negative.
    */
   public void setPenRadius(double r) throws JPARSECException {
       if (r < 0) throw new JPARSECException("pen radius must be positive");
       penRadius = r * width;
       BasicStroke stroke = new BasicStroke((float) penRadius);
       screenGraphics.setStroke(stroke);
   }

   /**
    * Set the pen color to the given color. The available pen colors are
      BLACK, BLUE, CYAN, DARK_GRAY, GRAY, GREEN, LIGHT_GRAY, MAGENTA,
      ORANGE, PINK, RED, WHITE, and YELLOW.
    * @param color the Color to make the pen.
    */
   public void setPenColor(Color color) {
       penColor = color;
       screenGraphics.setColor(penColor);
   }

   /**
    * Set the font as given for all string writing.
    * @param f the font to make text.
    */
   public void setFont(Font f) { font = f; }

   /**
    * Returns the font.
    * @return The font.
    */
   public Font getFont() { return font; }

  /*************************************************************************
   *  Drawing geometric shapes.
   *************************************************************************/

   /**
    * Draw a line from (x0, y0) to (x1, y1).
    * @param x0 the x-coordinate of the starting point.
    * @param y0 the y-coordinate of the starting point.
    * @param x1 the x-coordinate of the destination point.
    * @param y1 the y-coordinate of the destination point.
    */
   public void line(double x0, double y0, double x1, double y1) {
       screenGraphics.draw(new Line2D.Double(scaleX(x0), scaleY(y0), scaleX(x1), scaleY(y1)));
   }

   /**
    * Draw one pixel at (x, y).
    * @param x the x-coordinate of the pixel.
    * @param y the y-coordinate of the pixel.
    */
   public void pixel(double x, double y) {
       screenGraphics.fillRect((int) Math.round(scaleX(x)), (int) Math.round(scaleY(y)), 1, 1);
   }

   /**
    * Draw a point at (x, y).
    * @param x the x-coordinate of the point.
    * @param y the y-coordinate of the point.
    */
   public void point(double x, double y) {
       double xs = scaleX(x);
       double ys = scaleY(y);
       double r = penRadius;
       if (r <= 1) pixel(x, y);
       else screenGraphics.fill(new Ellipse2D.Double(xs - r/2, ys - r/2, r, r));
   }

   /**
    * Draw circle of radius r, centered on (x, y); degenerate to pixel if small.
    * @param x the x-coordinate of the center of the circle.
    * @param y the y-coordinate of the center of the circle.
    * @param r the radius of the circle.
    * @throws JPARSECException if the radius of the circle is negative.
    */
   public void circle(double x, double y, double r) throws JPARSECException {
       if (r < 0) throw new JPARSECException("circle radius can't be negative");
       double xs = scaleX(x);
       double ys = scaleY(y);
       double ws = factorX(2*r);
       double hs = factorY(2*r);
       if (ws <= 1 && hs <= 1) pixel(x, y);
       else screenGraphics.draw(new Ellipse2D.Double(xs - ws/2, ys - hs/2, ws, hs));
   }

   /**
    * Draw filled circle of radius r, centered on (x, y); degenerate to pixel if small.
    * @param x the x-coordinate of the center of the circle.
    * @param y the y-coordinate of the center of the circle.
    * @param r the radius of the circle.
    * @throws JPARSECException if the radius of the circle is negative.
    */
   public void filledCircle(double x, double y, double r) throws JPARSECException {
       if (r < 0) throw new JPARSECException("circle radius can't be negative");
       double xs = scaleX(x);
       double ys = scaleY(y);
       double ws = factorX(2*r);
       double hs = factorY(2*r);
       if (ws <= 1 && hs <= 1) pixel(x, y);
       else screenGraphics.fill(new Ellipse2D.Double(xs - ws/2, ys - hs/2, ws, hs));
   }

   /**
    * Draw an arc of radius r, centered on (x, y), from angle1 to angle2 (in degrees).
    * @param x the x-coordinate of the center of the circle.
    * @param y the y-coordinate of the center of the circle.
    * @param r the radius of the circle.
    * @param angle1 the starting angle. 0 would mean an arc beginning at 3 o'clock..
    * @param angle2 the angle at the end of the arc. For example, if
    *        you want a 90 degree arc, then angle2 should be angle1 + 90.
    * @throws JPARSECException if the radius of the circle is negative.
    */
   public void arc(double x, double y, double r, double angle1, double angle2) throws JPARSECException {
       if (r < 0) throw new JPARSECException("arc radius can't be negative");
       while (angle2 < angle1) angle2 += 360;
       double xs = scaleX(x);
       double ys = scaleY(y);
       double ws = factorX(2*r);
       double hs = factorY(2*r);
       if (ws <= 1 && hs <= 1) pixel(x, y);
       else screenGraphics.draw(new Arc2D.Double(xs - ws/2, ys - hs/2, ws, hs, angle1, angle2 - angle1, Arc2D.OPEN));
   }

   /**
    * Draw squared of side length 2r, centered on (x, y); degenerate to pixel if small.
    * @param x the x-coordinate of the center of the square.
    * @param y the y-coordinate of the center of the square.
    * @param r radius is half the length of any side of the square.
    * @throws JPARSECException if r is negative.
    */
   public void square(double x, double y, double r) throws JPARSECException {
       if (r < 0) throw new JPARSECException("square side length can't be negative");
       double xs = scaleX(x);
       double ys = scaleY(y);
       double ws = factorX(2*r);
       double hs = factorY(2*r);
       if (ws <= 1 && hs <= 1) pixel(x, y);
       else screenGraphics.draw(new Rectangle2D.Double(xs - ws/2, ys - hs/2, ws, hs));
   }

   /**
    * Draw a filled square of side length 2r, centered on (x, y); degenerate to pixel if small.
    * @param x the x-coordinate of the center of the square.
    * @param y the y-coordinate of the center of the square.
    * @param r radius is half the length of any side of the square.
    * @throws JPARSECException if r is negative.
    */
   public void filledSquare(double x, double y, double r) throws JPARSECException {
       if (r < 0) throw new JPARSECException("square side length can't be negative");
       double xs = scaleX(x);
       double ys = scaleY(y);
       double ws = factorX(2*r);
       double hs = factorY(2*r);
       if (ws <= 1 && hs <= 1) pixel(x, y);
       else screenGraphics.fill(new Rectangle2D.Double(xs - ws/2, ys - hs/2, ws, hs));
   }

   /**
    * Draw a polygon with the given (x[i], y[i]) coordinates.
    * @param x an array of all the x-coordindates of the polygon.
    * @param y an array of all the y-coordindates of the polygon.
    */
   public void polygon(double[] x, double[] y) {
       int N = x.length;
       GeneralPath path = new GeneralPath();
       path.moveTo((float) scaleX(x[0]), (float) scaleY(y[0]));
       for (int i = 0; i < N; i++)
           path.lineTo((float) scaleX(x[i]), (float) scaleY(y[i]));
       path.closePath();
       screenGraphics.draw(path);
   }

   /**
    * Draw a filled polygon with the given (x[i], y[i]) coordinates.
    * @param x an array of all the x-coordindates of the polygon.
    * @param y an array of all the y-coordindates of the polygon.
    */
   public void filledPolygon(double[] x, double[] y) {
       int N = x.length;
       GeneralPath path = new GeneralPath();
       path.moveTo((float) scaleX(x[0]), (float) scaleY(y[0]));
       for (int i = 0; i < N; i++)
           path.lineTo((float) scaleX(x[i]), (float) scaleY(y[i]));
       path.closePath();
       screenGraphics.fill(path);
   }

   /**
    * Draw a polygon.
    * @param path The polygon.
    */
   public void polygon(Polygon path) {
       screenGraphics.draw(path);
   }

   /**
    * Draw a filled polygon.
    * @param path The polygon.
    */
   public void filledPolygon(Polygon path) {
       screenGraphics.fill(path);
   }

   /**
    * Applies a flood fill operation with a given color starting at a specific
    * pixel. The image object used internally must be non null, so the constructor
    * using an external Graphics2D object is not supported here.
    * @param x X position.
    * @param y Y position.
    * @param fillColour The color to fill the area.
    * @return True if successful, false in case starting point already had the
    * filling color.
    */
	public boolean floodFill(int x, int y, Color fillColour)
	{
		int minX = 0, maxX = width-1;
		int minY = 0, maxY = height-1;

		//***Get starting color.
		int startPixel = screenImage.getRGB(x, y);

		if (startPixel == fillColour.getRGB()) return false;
		screenGraphics.setColor(fillColour);

		BitSet pixelsChecked = new BitSet(width*height);
		Queue<FloodFillRange> ranges = new LinkedList<FloodFillRange>();

		//***Do first call to floodfill.
		LinearFill(x,  y, width, minX, maxX, startPixel, pixelsChecked, ranges); //, col0, y);

		//***Call floodfill routine while floodfill ranges still exist on the queue
		FloodFillRange range;
		while (ranges.size() > 0)
		{
			//**Get Next Range Off the Queue
			range = ranges.remove();

			//**Check Above and Below Each Pixel in the Floodfill Range
			int upY = range.Y - 1;//so we can pass the y coord by ref
			int downY = range.Y + 1;
			int offU = width * upY;
			int offD = width * downY;
			for (int i = range.startX; i <= range.endX; i++)
			{
				//*Start Fill Upwards
				if (i > minX && i < maxX) {
					//if we're not above the top of the bitmap and the pixel above this one is within the color tolerance
					if (range.Y > minY+2 && !pixelsChecked.get(offU+i) && CheckPixel(i, upY, startPixel))
						LinearFill( i,  upY, width, minX, maxX, startPixel, pixelsChecked, ranges);

					//*Start Fill Downwards
					//if we're not below the bottom of the bitmap and the pixel below this one is within the color tolerance
					if (range.Y < maxY-2 && !pixelsChecked.get(offD+i) && CheckPixel(i, downY, startPixel))
						LinearFill( i,  downY, width, minX, maxX, startPixel,  pixelsChecked, ranges);
				}
			}
		}
		return true;
	}
	// Finds the furthermost left and right boundaries of the fill area
	// on a given y coordinate, starting from a given x coordinate, filling as it goes.
	// Adds the resulting horizontal range to the queue of floodfill ranges,
	// to be processed in the main loop.
	//
	// int x, int y: The starting coords
	protected void LinearFill(int x, int y, int width,
			int minX, int maxX,
			int startPixel, BitSet pixelsChecked, Queue<FloodFillRange> ranges)
	{
		//***Find Left Edge of Color Area
		int lFillLoc = x; //the location to check/fill on the left
		int off = width*y;
		while (true)
		{
			//**fill with the color
			//if (lFillLoc != x) g.fillOval(lFillLoc, y, 1, 1);
			//**indicate that this pixel has already been checked and filled
			pixelsChecked.set(off+lFillLoc, true);
			//**de-increment
			lFillLoc--;     //de-increment counter
			//**exit loop if we're at edge of bitmap or color area
			if (lFillLoc <= minX || pixelsChecked.get(off+lFillLoc) || !CheckPixel(lFillLoc, y, startPixel))
				break;
		}
		// fillOval of size 1 uses setRGB, and also drawStraightLine. But note that the drawLine
		// method of Graphics is very slow even compared to setRGB pixel by pixel ... Using setRGB
		// means also we cannot use real transparency.
		lFillLoc++;

		//***Find Right Edge of Color Area
		int rFillLoc = x; //the location to check/fill on the left
		while (true)
		{
			//**fill with the color
			//g.fillOval(rFillLoc, y, 1, 1);
			//**indicate that this pixel has already been checked and filled
			pixelsChecked.set(off+rFillLoc, true);
			//**increment
			rFillLoc++;     //increment counter
			//**exit loop if we're at edge of bitmap or color area
			if (rFillLoc >= maxX || pixelsChecked.get(off+rFillLoc) || !CheckPixel(rFillLoc, y, startPixel))
				break;
		}
		rFillLoc--;
		if (lFillLoc < rFillLoc) {
			screenGraphics.drawLine(lFillLoc, y, rFillLoc, y);

			//add range to queue
			FloodFillRange r = new FloodFillRange(lFillLoc, rFillLoc, y);
			ranges.offer(r);
		}
	}
	//Sees if a pixel is within the color tolerance range.
	protected boolean CheckPixel(int x, int y, int startPixel)
	{
		return screenImage.getRGB(x, y) == startPixel;
	}
	// Represents a linear range to be filled and branched from.
	protected class FloodFillRange
	{
		public int startX;
		public int endX;
		public int Y;

		public FloodFillRange(int startX, int endX, int y)
		{
		    this.startX = startX;
		    this.endX = endX;
		    this.Y = y;
		}
	}

  /*************************************************************************
   *  Drawing images.
   * @throws JPARSECException
   *************************************************************************/

   // get an image from the given filename
   private Image getImage(String filename) throws JPARSECException {

       // to read from file
       ImageIcon icon = new ImageIcon(filename);

       // try to read from URL
       if ((icon == null) || (icon.getImageLoadStatus() != MediaTracker.COMPLETE)) {
           try {
               URL url = new URL(filename);
               icon = new ImageIcon(url);
           } catch (Exception e) {
        	   Logger.log(LEVEL.ERROR, "Cannot read image from url "+filename);
           }
       }

       // in case file is inside a .jar
       if ((icon == null) || (icon.getImageLoadStatus() != MediaTracker.COMPLETE)) {
           URL url = Draw.class.getResource(filename);
           if (url == null) throw new JPARSECException("image " + filename + " not found");
           icon = new ImageIcon(url);
       }

       return icon.getImage();
   }

   /**
    * Draw picture (gif, jpg, or png) centered on (x, y).
    * @param x the center x-coordinate of the image.
    * @param y the center y-coordinate of the image.
    * @param s the name of the image/picture, e.g., "ball.gif".
    * @throws JPARSECException If the image cannot be read or its size is 0.
    */
   public void picture(double x, double y, String s) throws JPARSECException {
       Image image = getImage(s);
       double xs = scaleX(x);
       double ys = scaleY(y);
       int ws = image.getWidth(null);
       int hs = image.getHeight(null);
       if (ws < 0 || hs < 0) throw new JPARSECException("image " + s + " is corrupt");

       screenGraphics.drawImage(image, (int) Math.round(xs - ws/2.0), (int) Math.round(ys - hs/2.0), null);
   }

   /**
    * Draw picture (gif, jpg, or png) centered on (x, y).
    * @param x the center x-coordinate of the image.
    * @param y the center y-coordinate of the image.
    * @param image The image.
    * @throws JPARSECException If the image cannot be read or its size is 0.
    */
   public void picture(double x, double y, BufferedImage image) throws JPARSECException {
       double xs = scaleX(x);
       double ys = scaleY(y);
       int ws = image.getWidth(null);
       int hs = image.getHeight(null);
       screenGraphics.drawImage(image, (int) Math.round(xs - ws/2.0), (int) Math.round(ys - hs/2.0), null);
   }

   /**
    * Draw picture (gif, jpg, or png) centered on (x, y),
    * rotated given number of degrees.
    * @param x the center x-coordinate of the image.
    * @param y the center y-coordinate of the image.
    * @param s the name of the image/picture, e.g., "ball.gif".
    * @param degrees is the number of degrees to rotate counterclockwise.
    * @throws JPARSECException If the image cannot be read or its size is 0.
    */
   public void picture(double x, double y, String s, double degrees) throws JPARSECException {
       Image image = getImage(s);
       double xs = scaleX(x);
       double ys = scaleY(y);
       int ws = image.getWidth(null);
       int hs = image.getHeight(null);
       if (ws < 0 || hs < 0) throw new JPARSECException("image " + s + " is corrupt");

       screenGraphics.rotate(Math.toRadians(-degrees), xs, ys);
       screenGraphics.drawImage(image, (int) Math.round(xs - ws/2.0), (int) Math.round(ys - hs/2.0), null);
       screenGraphics.rotate(Math.toRadians(+degrees), xs, ys);
   }

   /**
    * Draw picture (gif, jpg, or png) centered on (x, y).
    * Rescaled to w-by-h.
    * @param x the center x coordinate of the image.
    * @param y the center y coordinate of the image.
    * @param s the name of the image/picture, e.g., "ball.gif".
    * @param w the width of the image.
    * @param h the height of the image.
    * @throws JPARSECException If the image cannot be read or its size is 0.
    */
   public void picture(double x, double y, String s, double w, double h) throws JPARSECException {
       Image image = getImage(s);
       double xs = scaleX(x);
       double ys = scaleY(y);
       double ws = factorX(w);
       double hs = factorY(h);
       if (ws < 0 || hs < 0) throw new JPARSECException("image " + s + " is corrupt");
       if (ws <= 1 && hs <= 1) pixel(x, y);
       else {
           screenGraphics.drawImage(image, (int) Math.round(xs - ws/2.0),
                                      (int) Math.round(ys - hs/2.0),
                                      (int) Math.round(ws),
                                      (int) Math.round(hs), null);
       }
   }

   /**
    * Draw picture (gif, jpg, or png) centered on (x, y),
    * rotated given number of degrees, rescaled to w-by-h.
    * @param x the center x-coordinate of the image.
    * @param y the center y-coordinate of the image.
    * @param s the name of the image/picture, e.g., "ball.gif".
    * @param w the width of the image.
    * @param h the height of the image.
    * @param degrees is the number of degrees to rotate counterclockwise.
    * @throws JPARSECException If the image cannot be read or its size is 0.
    */
   public void picture(double x, double y, String s, double w, double h, double degrees) throws JPARSECException {
       Image image = getImage(s);
       double xs = scaleX(x);
       double ys = scaleY(y);
       double ws = factorX(w);
       double hs = factorY(h);
       if (ws < 0 || hs < 0) throw new JPARSECException("image " + s + " is corrupt");
       if (ws <= 1 && hs <= 1) pixel(x, y);

       screenGraphics.rotate(Math.toRadians(-degrees), xs, ys);
       screenGraphics.drawImage(image, (int) Math.round(xs - ws/2.0),
                                  (int) Math.round(ys - hs/2.0),
                                  (int) Math.round(ws),
                                  (int) Math.round(hs), null);
       screenGraphics.rotate(Math.toRadians(+degrees), xs, ys);
   }

   /**
    * Returns the width in pixels.
    * @return width.
    */
   public int getWidth() {
	   return width;
   }
   /**
    * Returns the height in pixels.
    * @return height.
    */
   public int getHeight() {
	   return height;
   }

  /*************************************************************************
   *  Drawing text.
   *************************************************************************/

   /**
    * Write the given text string in the current font, center on (x, y).
    * @param x the center x coordinate of the text.
    * @param y the center y coordinate of the text.
    * @param s the text.
    * @param center True to center the text at that position.
    */
   public void text(double x, double y, String s, boolean center) {
       screenGraphics.setFont(font);
       FontMetrics metrics = screenGraphics.getFontMetrics();
       double xs = scaleX(x);
       double ys = scaleY(y);
       TextLabel tl = new TextLabel(s);
       int ws = tl.getWidth(screenGraphics);
       int hs = metrics.getDescent();
       if (!center) ws = hs = 0;
       tl.draw(screenGraphics, (int) (0.5 + xs - ws/2.0), (int) (ys + hs));
   }

   /**
    * Write the given text string in the current font, starting on (x, y).
    * @param x the center x coordinate of the text.
    * @param y the center y coordinate of the text.
    * @param s the text.
    * @param angle Angle in radians.
    */
   public void textRotated(double x, double y, String s, double angle) {
       screenGraphics.setFont(font);
       double xs = scaleX(x);
       double ys = scaleY(y);
       FontMetrics metrics = screenGraphics.getFontMetrics();
       String ang = ""+(int)(Functions.normalizeRadians(angle)*Constant.RAD_TO_DEG);
       TextLabel tl = new TextLabel("@ROTATE"+ang+s);
       int ws = tl.getWidth(screenGraphics);
       int hs = metrics.getDescent();
       tl.draw(screenGraphics, (int) (0.5 + xs + (-ws + font.getSize() - hs) / 2.0), (int) (ys + hs));
   }

   private Picture pio;
   /**
    * Display on-screen image. This method cannot
    * be called in case the draw is done to an external graphics.
    */
   public void show() {
	   if (pio == null) pio = new Picture(screenImage);
       try {
    	   if (title == null) title = "";
    	   pio.show(title);
       } catch (Exception e) {
    	   Logger.log(LEVEL.ERROR, "Cannot show the image. Details: "+e.getLocalizedMessage());
       }
   }

   /**
    * Return the associated Picture object. This method cannot
    * be called in case the draw is done to an external graphics.
    * @return The Picture object.
    */
   public Picture getPicture()
   {
	   if (pio == null) pio = new Picture(screenImage);
	   return pio;
   }

   private void saveBackUp() {
	   screenImageBackUp = new BufferedImage(screenImage.getWidth(), screenImage.getHeight(), screenImage.getType());
	   Graphics2D g = screenImageBackUp.createGraphics();
	   g.drawImage(screenImage, 0, 0, null);
	   g.dispose();
   }
   private void loadBackUp() {
	   if (screenImageBackUp != null) screenGraphics.drawImage(screenImageBackUp, 0, 0, null);
   }
   private int lastX = -1, lastY = -1;
   private int mX = -1, mY = -1;
   /**
    * Returns a panel where the user can draw using the mouse and keyboard.
    * The color can be changed with key c followed by a number from 0 to 9.
    * The pen radius can be changed with r followed by a number.
    * The font size with f + number. Note that if pen radius is below 3
    * plain text will be used, italic up to 6 and bold up to 9.
    * Text can be drawn with key t followed by the text and the enter key.
    * Mouse dragging will draw points/lines.<P>
    * Other options are to save the draw into memory (S), to load from memory (L),
    * and to delete the draw (X), and to hide the panel (W or Q). Font size
    * can also be changed with M.<P>
    * This method cannot
    * be called in case the draw is done to an external graphics.
    * @param background Optional background image, can be null.
    * @return The panel.
    */
   public JPanel getComponent(BufferedImage background) {
	   final JPanel panel = new JPanel();
	   panel.setLayout(null);
	   panel.setBounds(0, 0, width, height);
	   panel.setOpaque(false);
	   if (background != null) {
		   JLabel label0 = new JLabel(new ImageIcon(background));
		   label0.setBounds(0, 0, width, height);
		   panel.add(label0);
	   }
	   final JLabel label = new JLabel(new ImageIcon(screenImage));
	   label.setBounds(0, 0, width, height);
	   label.setOpaque(false);
	   panel.add(label, 0);

	   panel.addMouseListener(new MouseAdapter() {
		   @Override
		   public void mousePressed(MouseEvent e) {
				point((double)e.getX()/(double)width, 1.0-(double)e.getY()/(double)height);
				label.repaint();
				lastX = e.getX();
				lastY = e.getY();
		   }
	   });
	   panel.addMouseMotionListener(new MouseAdapter() {
		   @Override
			public void mouseDragged(MouseEvent e) {
				if (lastX >= 0 && lastY >= 0) {
					line((double)lastX/(double)width, 1.0-(double)lastY/(double)height, (double)e.getX()/(double)width, 1.0-(double)e.getY()/(double)height);
				} else {
					point((double)e.getX()/(double)width, 1.0-(double)e.getY()/(double)height);
				}
				label.repaint();
				lastX = e.getX();
				lastY = e.getY();
				panel.requestFocusInWindow();
			}
		   @Override
			public void mouseMoved(MouseEvent e) {
				mX = e.getX();
				mY = e.getY();
		   }
	   });
	   panel.setFocusable(true);
	   panel.addKeyListener(new KeyAdapter() {
		   private boolean setColor = false, setRadius = false, setFontSize = false, setText = false;
		   private String text = "";
		   private int textX = -1, textY = -1;
		   private Color[] c = new Color[] {Color.BLACK, Color.RED, Color.GREEN, Color.BLUE,
				   Color.MAGENTA, Color.PINK, Color.ORANGE, Color.YELLOW, Color.CYAN, Color.WHITE};
		   public void processKey(String key) {
			   if (key.equals("c")) setColor = true;
			   if (key.equals("r")) setRadius = true;
			   if (key.equals("f") || key.equals("m")) setFontSize = true;
			   if (key.equals("t")) {
				   setText = true;
				   textX = mX;
				   textY = mY;
			   }
			   try {
				   int n = Integer.parseInt(""+key); // Error if key is not a number
				   if (setColor) setPenColor(c[n]);
				   if (setRadius) {
					   double r = n / 1000.0;
					   setPenRadius(r);
				   }
				   if (setFontSize) {
					   int s = Font.PLAIN;
					   if (penRadius > 0.003*width) s = Font.ITALIC;
					   if (penRadius > 0.006*width) s = Font.BOLD;
					   Font f = new Font(font.getName(), s, n*3+8);
					   setFont(f);
				   }
				   setColor = false;
				   setRadius = false;
				   setFontSize = false;
			   } catch (Exception exc) {}
		   }

		   @Override
		   public void keyPressed(KeyEvent e) {
			   if (setText) {
				   if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					   setText = false;
					   text((double)textX/(double)width, 1.0-(double)textY/(double)height, text, true);
					   text = "";
					   label.repaint();
				   } else {
					   if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
						   if (text.length() > 0) text = text.substring(0, text.length()-1);
					   } else {
						   if (e.getKeyCode() == KeyEvent.VK_DELETE) {
							   text = "";
						   } else {
							   if (e.getKeyCode() == KeyEvent.VK_V && e.getModifiers() == KeyEvent.CTRL_MASK) {
								   String s = SystemClipboard.getClipboard();
								   if (s != null) text += s;
							   } else {
								   if (e.getKeyCode() == KeyEvent.VK_CIRCUMFLEX) {
									   text += "^";
								   } else {
									   if (e.getKeyCode() == KeyEvent.VK_UP) {
										   text += "^{";
									   } else {
										   if (e.getKeyCode() == KeyEvent.VK_DOWN) {
											   text += "_{";
										   } else {
											   if (e.getKeyCode() == KeyEvent.VK_AT) {
												   text += "@";
											   } else {
												   if (e.getKeyCode() == KeyEvent.VK_BRACELEFT) {
													   text += "{";
												   } else {
													   if (e.getKeyCode() == KeyEvent.VK_BRACERIGHT) {
														   text += "}";
													   } else {
														   if (e.getKeyChar() != KeyEvent.CHAR_UNDEFINED)
															   text += e.getKeyChar();
													   }
												   }
											   }
										   }
									   }
								   }
							   }
						   }
					   }
					   Logger.log(LEVEL.TRACE_LEVEL3, "Text: "+text);
				   }
				   return;
			   }
			   if (e.getKeyCode() == KeyEvent.VK_L) {
				   clear();
				   loadBackUp();
				   panel.repaint();
			   }
			   if (e.getKeyCode() == KeyEvent.VK_X) {
				   clear();
				   panel.repaint();
			   }
			   if (e.getKeyCode() == KeyEvent.VK_Q || e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_B) {
				   e.consume();
				   panel.getParent().requestFocusInWindow();
			   }
			   if (e.getKeyCode() == KeyEvent.VK_S) saveBackUp();
			   if (e.getKeyCode() == KeyEvent.VK_C) processKey("c");
			   if (e.getKeyCode() == KeyEvent.VK_R) processKey("r");
			   if (e.getKeyCode() == KeyEvent.VK_F || e.getKeyCode() == KeyEvent.VK_M) processKey("m");
			   if (e.getKeyCode() == KeyEvent.VK_T) processKey("t");
			   processKey(""+e.getKeyChar());
		   }
		   @Override
		   public void keyReleased(KeyEvent e) {
			   if (e.getKeyCode() == 0) {
				   panel.getParent().requestFocusInWindow();
				   e.setSource(panel.getParent());
			   }
		   }
	   });
	   return panel;
   }
}
