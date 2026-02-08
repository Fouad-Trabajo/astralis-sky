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

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ConvolveOp;
import java.awt.image.DataBuffer;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.Kernel;
import java.awt.image.PixelGrabber;
import java.awt.image.RGBImageFilter;
import java.awt.image.RescaleOp;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.jfree.chart.encoders.EncoderUtil;
import org.jfree.chart.encoders.ImageFormat;

import jparsec.ephem.Functions;
import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.io.Printer;
import jparsec.io.device.ObservationManager;
import jparsec.util.JPARSECException;
import jparsec.util.Logger;
import jparsec.util.Logger.LEVEL;
import jparsec.util.Translate;
import jparsec.vo.GeneralQuery;

/**
 * A class to read, write, and process images.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Picture
{
	private JFrame frame;
    private JMenuItem saveAs;
    private JMenuItem print;
    private JMenuItem close;
	private BufferedImage image;
	private String path;

	/** Set this flag to true to allow spline resizing in method
	 * {@linkplain #getScaledInstanceUsingSplines(int, int, boolean)},
	 * that calls to the resize method using finite differences
	 * implemented in class {@linkplain Resize}.
	 * This is the default value. Set this to false to force the multi
	 * step technique always, which is usually slightly worse, but much faster.
	 * In less words, this flag allows to force fast resizing mode in
	 * JPARSEC even when calling to the 'spline' resizing method. */
	public static boolean ALLOW_SPLINE_RESIZING = true;

	/**
	 * Constructor that reads an image from the disk, using ImageIO library.
	 * Note imageIO does not support certain transparent PNG formats.
	 * @param path Path of the image.
	 * @throws JPARSECException If an error occurs.
	 */
	public Picture (String path)
	throws JPARSECException {
		if (path.startsWith("http://")) {
			image = GeneralQuery.queryImage(path);
		} else {
			image = Picture.readImage(path);
			this.path = path;
		}
		check();
	}

	/**
	 * Constructor that reads an image from the disk, using Toolkit library.
	 * @param url Url to the image.
	 * @throws JPARSECException If an error occurs.
	 */
	public Picture (URL url)
	throws JPARSECException {
		image = Picture.readImage(url);
		this.path = url.getFile();
		check();
	}

	/**
	 * Constructor for an image of certain size.
	 * @param width The width.
	 * @param height The height.
	 * @param pixels Array of RGB colors of size width*height, as
	 * obtained for example by grabbing pixels.
	 */
	public Picture(int width, int height, int[] pixels) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		image.getRaster().setDataElements(0, 0, width, height, pixels);
		this.image = image;
	}

	/**
	 * Constructor for a blank image of certain size.
	 * @param width The width.
	 * @param height The height.
	 */
	public Picture (int width, int height)
	{
		image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	}

	/**
	 * Constructor for a blank image of certain size.
	 * @param width The width.
	 * @param height The height.
	 * @param alpha True to use an alpha channel.
	 */
	public Picture (int width, int height, boolean alpha)
	{
		if (alpha) {
			image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		} else {
			image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		}
	}

	/**
	 * Constructor for a given Image.
	 * @param img The image.
	 */
	public Picture (BufferedImage img)
	{
		image = img;
/*
    	BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB); //img.getType());

	    // Copy image to buffered image
	    Graphics g = bimage.createGraphics();

	    // Paint the image onto the buffered image
	    g.drawImage(img, 0, 0, null);
	    g.dispose();
	    image = bimage;
*/
		check();
	}

	private void check() {
		if (image == null) return;
		int dt = image.getRaster().getDataBuffer().getDataType();
		if (image.getType() == BufferedImage.TYPE_CUSTOM || dt != DataBuffer.TYPE_INT && dt != DataBuffer.TYPE_BYTE)
			image = Picture.copyWithTransparency(image);
	}

	/**
	 * Constructor for an image given as an array. The image should be
	 * given with the red, green, blue, and alpha components (array size
	 * of 4, x, and y).
	 * @param array The array.
	 * @param useAlpha True to use the alpha (transparency) channel. Note
	 * that selecting true could affect the JPEG export since JPEG format
	 * does not support it.
	 */
	public Picture(int[][][] array, boolean useAlpha)
	{
		if (useAlpha) {
			image = new BufferedImage(array[0].length, array[0][0].length, BufferedImage.TYPE_INT_ARGB);
			setColor(array[0], array[1], array[2], array[3]);
		} else {
			image = new BufferedImage(array[0].length, array[0][0].length, BufferedImage.TYPE_INT_RGB);
			setColor(array[0], array[1], array[2], null);
		}
	}

	/**
	 * Constructor for an image given as 3 arrays of r, g, and b color maps.
	 * Array sizes should be the same for each of them.
	 * @param r The red color map.
	 * @param g The green color map.
	 * @param b The blue color map.
	 * @param a The alpha color map. Set to null for a non transparent image.
	 */
	public Picture(int[][] r, int[][] g, int[][] b, int[][] a)
	{
		int w = 0, h = 0;
		if (r != null) {
			w = r.length;
			h = r[0].length;
		} else {
			if (g != null) {
				w = g.length;
				h = g[0].length;
			} else {
				if (b != null) {
					w = b.length;
					h = b[0].length;
				} else {
					if (a != null) {
						w = a.length;
						h = a[0].length;
					}
				}
			}
		}
		int t = (a == null) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
		image = new BufferedImage(w, h, t);
		setColor(r, g, b, a);
	}

	/**
	 * Constructor for an image given as 3 byte arrays of r, g, and b color maps.
	 * Array sizes should be the same for each of them.
	 * @param r The red color map.
	 * @param g The green color map.
	 * @param b The blue color map.
	 * @param a The alpha color map. Set to null for a non transparent image.
	 */
	public Picture(byte[][] r, byte[][] g, byte[][] b, byte[][] a)
	{
		int w = 0, h = 0;
		if (r != null) {
			w = r.length;
			h = r[0].length;
		} else {
			if (g != null) {
				w = g.length;
				h = g[0].length;
			} else {
				if (b != null) {
					w = b.length;
					h = b[0].length;
				} else {
					if (a != null) {
						w = a.length;
						h = a[0].length;
					}
				}
			}
		}
		int t = (a == null) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
		image = new BufferedImage(w, h, t);
		setColor(r, g, b, a);
	}

	/**
	 * Sets the color for each of the pixels using 2d arrays.
	 * @param r The red image.
	 * @param g The green image.
	 * @param b The blue image.
	 * @param a The alpha channel. Can be null.
	 */
	public void setColor(int r[][], int g[][], int b[][], int a[][]) {
		path = null;
		int d1 = 0, d2 = 0;
		if (r != null) {
			d1 = r.length;
			d2 = r[0].length;
		}
		if (g != null) {
			d1 = g.length;
			d2 = g[0].length;
		}
		if (b != null) {
			d1 = b.length;
			d2 = b[0].length;
		}
		for (int j=0; j<d2; j++)
		{
			int rgb[] = new int[d1];
			for (int i=0; i<d1; i++)
			{
				int rc = 0, gc = 0, bc = 0, ac = 0;
				if (r != null) rc = r[i][j];
				if (g != null) gc = g[i][j];
				if (b != null) bc = b[i][j];
				if (rc < 0) rc = 0;
				if (rc > 255) rc = 255;
				if (gc < 0) gc = 0;
				if (gc > 255) gc = 255;
				if (bc < 0) bc = 0;
				if (bc > 255) bc = 255;

				if (a != null) {
					ac = a[i][j];
					if (ac < 0) ac = 0;
					if (ac > 255) ac = 255;
					rgb[i] = Functions.getColor(rc, gc, bc, ac);
				} else {
					rgb[i] = Functions.getColor(rc, gc, bc, 255);
				}
			}
	        image.setRGB(0, j, d1, 1, rgb, 0, d1);
		}
	}

	/**
	 * Sets the color for each of the pixels using 2d byte arrays.
	 * @param r The red image.
	 * @param g The green image.
	 * @param b The blue image.
	 * @param a The alpha channel. Can be null.
	 */
	public void setColor(byte r[][], byte g[][], byte b[][], byte a[][]) {
		path = null;
		int d1 = 0, d2 = 0;
		if (r != null) {
			d1 = r.length;
			d2 = r[0].length;
		}
		if (g != null) {
			d1 = g.length;
			d2 = g[0].length;
		}
		if (b != null) {
			d1 = b.length;
			d2 = b[0].length;
		}
		int add = 128;
		for (int j=0; j<d2; j++)
		{
			int rgb[] = new int[d1];
			for (int i=0; i<d1; i++)
			{
				int rc = add, gc = add, bc = add, ac = 0;
				if (r != null) rc += r[i][j];
				if (g != null) gc += g[i][j];
				if (b != null) bc += b[i][j];
				if (rc > 255) rc = 255;
				if (gc > 255) gc = 255;
				if (bc > 255) bc = 255;
				if (a != null) {
					ac = a[i][j] + 0;
					rgb[i] = Functions.getColor(rc, gc, bc, ac);
				} else {
					rgb[i] = Functions.getColor(rc, gc, bc, 255);
				}
			}
	        image.setRGB(0, j, d1, 1, rgb, 0, d1);
		}
	}

	/**
	 * Constructor for an image given as a 2d array. The image should be
	 * given with ARGB colors.
	 * @param array The array, ordered as [x][y].
	 */
	public Picture(int[][] array)
	{
		image = new BufferedImage(array.length, array[0].length, BufferedImage.TYPE_INT_ARGB);
		for (int j=0; j<array[0].length; j++)
		{
			int rgb[] = new int[array.length];
			for (int i=0; i<array.length; i++)
			{
				rgb[i] = array[i][j];
			}
	        image.setRGB(0, j, array.length, 1, rgb, 0, array.length);
		}
	}

	/**
	 * Returns the current image.
	 * @return The image.
	 */
	public BufferedImage getImage()
	{
		return this.image;
	}

	/**
	 * Sets the current image.
	 * @param img The image.
	 */
	public void setImage(BufferedImage img)
	{
		path = null;
		this.image = img;
	}
	
	/**
	 * Updates the current image without modifying the original image object.
	 * @param img The image.
	 */
	public void updateImage(BufferedImage img)
	{
		path = null;
		int w = image.getWidth(), h = image.getHeight(), rgbArray[];
		rgbArray = img.getRGB(0, 0, w, h, null, 0, w);
		this.image.setRGB(0, 0, w, h, rgbArray, 0, w);
	}
	
	/**
	 * Sets the current image without changing its size.
	 * @param pixels The pixels of the image.
	 * @throws JPARSECException If an error occurs.
	 */
	public void setImage(int[] pixels) throws JPARSECException
	{
		path = null;
		image.setRGB(0, 0, this.getWidth(), this.getHeight(), pixels, 0, this.getWidth());
	}
	/**
	 * Returns the width of the image.
	 * @return Width.
	 */
	public int getWidth() {
		return image.getWidth();
	}
	/**
	 * Returns the height of the image.
	 * @return Height.
	 */
	public int getHeight() {
		return image.getHeight();
	}
	/**
	 * Return image size by waiting for the image to be fully loaded.
	 * @return Width and height.
	 */
	public Dimension getSize() {
	     int width = image.getWidth();
	     int height = image.getHeight();
	     if (width == -1 || height == -1) {
	    	 do {
	    		 if (width == -1) width = image.getWidth(null);
	    		 if (height == -1) height = image.getHeight(null);
	    	 } while (width == -1 || height == -1);
	     }
	     return new Dimension (width, height);
	   }

	/**
	 * Reads an image from hard disk, using ImageIO library.
	 *
	 * @param path Full path to the place where the image is located.
	 * @throws JPARSECException Thrown if the method fails.
	 * @return The image.
	 */
	public static BufferedImage readImage(String path) throws JPARSECException
	{
		String path2 = path;
		if (path2.startsWith("file:")) {
			try {
				path2 = new URL(path).getFile();
				path2 = DataSet.replaceAll(path2, "%20", " ", true);
			} catch (Exception exc) {
				Logger.log(LEVEL.ERROR, "Could not clean the url/path "+path);
			}
		}

		if (path.toLowerCase().endsWith(".pgm")) return ObservationManager.readPGM(path, true);

		BufferedImage buf = null;
		File file = new File(path2);
		if (!file.exists()) throw new JPARSECException("file "+path2+" does not exist.");
		try
		{
			buf = ImageIO.read(file);
		} catch (Exception e)
		{
			throw new JPARSECException("error reading image "+path2+".", e);
		}
		return buf;
	}

	/**
	 * Reads an image from hard disk, using Toolkit.
	 *
	 * @param url Url to the file.
	 * @throws JPARSECException Thrown if the method fails.
	 * @return The image.
	 */
	public static BufferedImage readImage(URL url) throws JPARSECException
	{
		String path = url.getFile();
		path = DataSet.replaceAll(path, "%20", " ", true);
		if (path.startsWith("file:")) {
			if (path.toLowerCase().endsWith(".pgm")) return ObservationManager.readPGM(path, true);
		}

		return Picture.toBufferedImage(Toolkit.getDefaultToolkit().getImage(url));
	}

	/**
	 *  Convenience method to create a BufferedImage of the desktop.
	 *
	 *  @return	image The image for the given region.
	 *  @throws JPARSECException If an error occurs.
	 */
	public static BufferedImage createDesktopImage()
		throws JPARSECException
	{
		try {
			Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
			java.awt.Rectangle region = new java.awt.Rectangle(0, 0, d.width, d.height);
			return Picture.createImage(region);
		} catch (Exception e)
		{
			throw new JPARSECException("error creating desktop image.", e);
		}
	}
	/**
	 *  Create a BufferedImage from a rectangular region on the screen.
	 *
	 *  @param	 region Region on the screen to create image from.
	 *  @return	image The image for the given region.
	 *  @throws JPARSECException If an error occurs.
	 */
	public static BufferedImage createImage(java.awt.Rectangle region)
	throws JPARSECException
	{
		try {
			BufferedImage image = new Robot().createScreenCapture( region );
			return image;
		} catch (Exception e)
		{
			throw new JPARSECException("error creating image.", e);
		}
	}

	/**
	 * Writes an image to hard disk.
	 *
	 * @param path Full path to the directory where the image will be created.
	 * @param file_name Name of the image, including extension.
	 * @param format Format ID constant.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public void write(String path, String file_name, FORMAT format)
			throws JPARSECException
	{
		try
		{
			String fullPath = path + file_name;

			if (format == Picture.FORMAT.EPS) {
				Picture.exportAsEPSFile(fullPath, image);
			} else {
				if (format == Picture.FORMAT.PDF) {
					Picture.exportAsPDFFile(fullPath, image);
				} else {
					if (format == Picture.FORMAT.SVG) {
						Picture.exportAsSVGFile(fullPath, image);
					} else {
						String formatS = FORMAT_JPEG;
						if (format == Picture.FORMAT.PNG) formatS = FORMAT_PNG;
						if (format == Picture.FORMAT.GIF) formatS = FORMAT_GIF;
						if (format == Picture.FORMAT.BMP) formatS = FORMAT_BMP;

						File file = new File(fullPath);
						ImageIO.write(image, formatS, file);
					}
				}
			}
		} catch (Exception e)
		{
			throw new JPARSECException("error writing image "+path+file_name+".", e);
		}
	}

	/**
	 * Returns if a given output file name includes a valid extension for
	 * a supported output format in method {@linkplain #write(String)}.
	 * @param path_and_file_name Path to the output image file.
	 * @return True or false.
	 */
	public static boolean formatSupported(String path_and_file_name) {
		boolean isJPG = path_and_file_name.toLowerCase().endsWith(".jpg");
		if (!isJPG) isJPG = path_and_file_name.toLowerCase().endsWith(".jpeg");
		boolean isPNG = path_and_file_name.toLowerCase().endsWith(".png");
		boolean isBMP = path_and_file_name.toLowerCase().endsWith(".bmp");
		boolean isGIF = path_and_file_name.toLowerCase().endsWith(".gif");
		boolean isEPS = path_and_file_name.toLowerCase().endsWith(".eps");
		boolean isPDF = path_and_file_name.toLowerCase().endsWith(".pdf");
		boolean isSVG = path_and_file_name.toLowerCase().endsWith(".svg");
		return isJPG || isPNG || isBMP || isGIF || isEPS || isPDF || isSVG;
	}

	/**
	 * Writes an image to hard disk. Format is automatically taken from name
	 * extension (jpg, png, bmp, gif, and also eps, pdf, svg if dependencies are
	 * satisfied to export to these formats).
	 *
	 * @param path_and_file_name Path plus name of the image, including
	 *        extension.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public void write(String path_and_file_name) throws JPARSECException
	{
		String format = "";
		boolean isJPG = path_and_file_name.toLowerCase().endsWith(".jpg");
		if (!isJPG) isJPG = path_and_file_name.toLowerCase().endsWith(".jpeg");
		boolean isPNG = path_and_file_name.toLowerCase().endsWith(".png");
		boolean isBMP = path_and_file_name.toLowerCase().endsWith(".bmp");
		boolean isGIF = path_and_file_name.toLowerCase().endsWith(".gif");
		boolean isEPS = path_and_file_name.toLowerCase().endsWith(".eps");
		boolean isPDF = path_and_file_name.toLowerCase().endsWith(".pdf");
		boolean isSVG = path_and_file_name.toLowerCase().endsWith(".svg");
		if (isJPG)
			format = FORMAT_JPEG;
		if (isPNG)
			format = FORMAT_PNG;
		if (isBMP)
			format = FORMAT_BMP;
		if (isGIF)
			format = FORMAT_GIF;
		if (isEPS)
			format = FORMAT_EPS;
		if (isPDF)
			format = FORMAT_PDF;
		if (isSVG)
			format = FORMAT_SVG;

		if (format.equals(""))
			throw new JPARSECException("invalid file extension.");

		try
		{
			String fullPath = path_and_file_name;

			if (format.equals(Picture.FORMAT_EPS)) {
				Picture.exportAsEPSFile(fullPath, image);
			} else {
				if (format.equals(Picture.FORMAT_PDF)) {
					Picture.exportAsPDFFile(fullPath, image);
				} else {
					if (format.equals(Picture.FORMAT_SVG)) {
						Picture.exportAsSVGFile(fullPath, image);
					} else {
						File file = new File(fullPath);
						ImageIO.write(image, format, file);
					}
				}
			}
		} catch (Exception e)
		{
			throw new JPARSECException(e.getMessage(), e);
		}
	}

	/**
	 * The set of image formats.
	 */
	public enum FORMAT {
		/** bmp image format */
		BMP,
		/** jpeg image format */
		JPEG,
		/** png image format */
		PNG,
		/** gif image format */
		GIF,
		/** eps image format */
		EPS,
		/** svg image format */
		SVG,
		/** pdf image format */
		PDF
	};

	/**
	 * ID constant for .bmp image format.
	 */
	private static final String FORMAT_BMP = "BMP";

	/**
	 * ID constant for .jpg image format.
	 */
	private static final String FORMAT_JPEG = "JPEG";

	/**
	 * ID constant for .png image format.
	 */
	private static final String FORMAT_PNG = "PNG";

	/**
	 * ID constant for .gif image format.
	 */
	private static final String FORMAT_GIF = "GIF";

	/**
	 * ID constant for .eps image format.
	 */
	private static final String FORMAT_EPS = "EPS";

	/**
	 * ID constant for .svg image format.
	 */
	private static final String FORMAT_SVG = "SVG";

	/**
	 * ID constant for .pdf image format.
	 */
	private static final String FORMAT_PDF = "PDF";

	/**
	 * Writes a JPEG image to hard disk, considering certain level of
	 * compression quality.
	 *
	 * @param path_and_file_name Path plus name of the image, including
	 *        extension.
	 * @param quality The output quality (0.0 to 1.0).
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public void writeAsJPEG(String path_and_file_name, double quality)
			throws JPARSECException
	{
		try
		{
			String fullPath = path_and_file_name;
			File file = new File(fullPath);
			OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
			EncoderUtil.writeBufferedImage(image, ImageFormat.JPEG, out, (float) quality);
		} catch (Exception e)
		{
			throw new JPARSECException(e);
		}
	}

	/**
	 * Writes a PNG image to hard disk, considering certain level of
	 * compression quality. PNG is lossless.
	 *
	 * @param path_and_file_name Path plus name of the image, including
	 *        extension.
	 * @param quality The output quality (0.0 to 1.0).
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public void writeAsPNG(String path_and_file_name, double quality)
			throws JPARSECException
	{
		try
		{
			String fullPath = path_and_file_name;
			File file = new File(fullPath);
			OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
			EncoderUtil.writeBufferedImage(image, ImageFormat.PNG, out, (float) quality);
		} catch (Exception e)
		{
			throw new JPARSECException(e);
		}
	}

	/**
	 * Flips the image in horizontal and/or vertical.
	 * @param h True to flip horizontally.
	 * @param v True to flip vertically.
	 */
	public void flip(boolean h, boolean v) {
		if (h && v) {
			path = null;
			// Flip the image vertically and horizontally;
			// equivalent to rotating the image 180 degrees
			AffineTransform tx = AffineTransform.getScaleInstance(-1, -1);
			tx.translate(-image.getWidth(), -image.getHeight());
			AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
			image = op.filter(image, null);
		} else {
			if (h) {
				path = null;
				// Flip the image horizontally
				AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
				tx.translate(-image.getWidth(), 0);
				AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
				image = op.filter(image, null);
			}
			if (v) {
				path = null;
				// Flip the image vertically
				AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
				tx.translate(0, -image.getHeight());
				AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
				image = op.filter(image, null);
			}
		}
	}

	/**
	 * Re-scales the image to another size. If it is desirable to re-scale just
	 * in one axis, adapting the other to maintain the original width/height
	 * ratio, then set the other length to zero. This method uses
	 * a multi-step technique.
	 *
	 * @param width New width.
	 * @param height New height.
	 * @see Resize
	 */
	public void scaleImage(int width, int height)
	{
		path = null;
		// Flip in case of negative size
		if (width < 0 || height < 0) {
			boolean h = false, v = false;
			if (width < 0) h = true;
			if (height < 0) v = true;
			flip(h, v);
			width = Math.abs(width);
			height = Math.abs(height);
		}

		if (width < 1 && height < 1) return;

		int origWidth = image.getWidth();
		int origHeight = image.getHeight();

		if (width < 1 && height > 0)
		{
			double scale = (double) height / (double) origHeight;
			width = (int) (scale * origWidth + 0.5);
		} else {
			if (width > 0 && height < 1)
			{
				double scale = (double) width / (double) origWidth;
				height = (int) (scale * origHeight + 0.5);
			}
		}

		if (origWidth == width && origHeight == height) return;

		getScaledInstance(width, height, false);
	}
	/**
	 * Re-scales the image to another size, maintaining the original image width/height
	 * ratio. The result will be the smallest image compatible with the input size. Width
	 * or height can be set to 0 to calculate automatic value, but not both.
	 * This method uses a multi-step technique.
	 *
	 * @param width New width. Set to 0 or negative to calculate value automatically.
	 * @param height New height. Set to 0 or negative to calculate value automatically.
	 * @see Resize
	 */
	public void scaleMaintainingImageRatio(int width, int height)
	{
		path = null;
		// Flip in case of negative size
		if (width < 0 || height < 0) {
			boolean h = false, v = false;
			if (width < 0) h = true;
			if (height < 0) v = true;
			flip(h, v);
			width = Math.abs(width);
			height = Math.abs(height);
		}

		if (width < 1 && height < 1) return;

		int origWidth = image.getWidth();
		int origHeight = image.getHeight();

		if (width < 1 && height > 0)
		{
			double scale = (double) height / (double) origHeight;
			width = (int) (scale * origWidth + 0.5);
		} else {
			if (width > 0 && height < 1)
			{
				double scale = (double) width / (double) origWidth;
				height = (int) (scale * origHeight + 0.5);
			} else {
				if (width > 0 && height > 0) {
					double scaleW = (double) width / (double) origWidth;
					double scaleH = (double) height / (double) origHeight;
					double scale = scaleW;
					if (scaleH < scaleW) {
						scale = scaleH;
						width = (int) (scale * origWidth + 0.5);
					} else {
						height = (int) (scale * origHeight + 0.5);
					}
				}
			}
		}

		if (origWidth == width && origHeight == height) return;

		getScaledInstance(width, height, false);
	}

	/**
	 * Scales the image to another size using the multi-step technique described in
	 * http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html.
	 * Width or height can be set to 0 to calculate automatic value assuming
	 * that the same width/height ratio must be preserved.
	 * @param width Width. Set to 0 to calculate value automatically.
	 * @param height Height. Set to 0 to calculate value automatically.
	 * @param sameRatio True to maintain image ratio (maximum size that fits the rectangle
	 * with the provided width and height).
	 */
	public void getScaledInstance(int width, int height, boolean sameRatio) {
		getScaledInstance(width, height, sameRatio, true);
	}

	/**
	 * Scales the image to another size using the multi-step technique described in
	 * http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html.
	 * Width or height can be set to 0 to calculate automatic value assuming
	 * that the same width/height ratio must be preserved.
	 * @param width Width. Set to 0 to calculate value automatically, negative value to 
	 * invert image in horizontal.
	 * @param height Height. Set to 0 to calculate value automatically, negative value to 
	 * invert image in vertical.
	 * @param sameRatio True to maintain image ratio (maximum size that fits the rectangle
	 * with the provided width and height).
	 * @param bicubic True for bicubic interpolation quality, slower. It will not be used 
	 * to upscale an image.
	 */
	public void getScaledInstance(int width, int height, boolean sameRatio, boolean bicubic) {
		path = null;
		// Flip in case of negative size
		if (width < 0 || height < 0) {
			boolean h = false, v = false;
			if (width < 0) h = true;
			if (height < 0) v = true;
			flip(h, v);
			width = Math.abs(width);
			height = Math.abs(height);
		}

		if (width < 1 && height < 1) return;

		int origWidth = image.getWidth();
		int origHeight = image.getHeight();

		if (width < 1 && height > 0)
		{
			double scale = (double) height / (double) origHeight;
			width = (int) (scale * origWidth + 0.5);
		} else {
			if (width > 0 && height < 1)
			{
				double scale = (double) width / (double) origWidth;
				height = (int) (scale * origHeight + 0.5);
			} else {
				if (width > 0 && height > 0 && sameRatio) {
					double scaleW = (double) width / (double) origWidth;
					double scaleH = (double) height / (double) origHeight;
					double scale = scaleW;
					if (scaleH < scaleW) {
						scale = scaleH;
						width = (int) (scale * origWidth + 0.5);
					} else {
						height = (int) (scale * origHeight + 0.5);
					}
				}
			}
		}

		if (origWidth == width && sameRatio || origHeight == height && sameRatio ||
				origWidth == width && origHeight == height) return;

        // Original implementation from http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html
        int type = (image.getTransparency() == Transparency.OPAQUE) ?
                BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        if (image.getType() != BufferedImage.TYPE_INT_RGB && image.getType() != BufferedImage.TYPE_INT_ARGB
        		&& image.getType() != BufferedImage.TYPE_CUSTOM)
        	type = image.getType();
        BufferedImage ret =  image;
        // Use multi-step technique: start with original size, then
        // scale down in multiple passes with drawImage()
        // until the target size is reached
        int w = image.getWidth();
        int h = image.getHeight();

	       do {
	            if (w > width) {
	                w /= 2;
	                if (w < width) {
	                    w = width;
	                }
	            } else {
	            	bicubic = false;
	                w = width;
	            }

	            if (h > height) {
	                h /= 2;
	                if (h < height) {
	                    h = height;
	                }
	            } else {
	                h = height;
	            }

	            BufferedImage tmp = new BufferedImage(w,h,type);
				Graphics2D g2 = tmp.createGraphics();
	            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, bicubic ? RenderingHints.VALUE_INTERPOLATION_BICUBIC : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
	            g2.drawImage(ret, 0, 0, w, h, null);
	            g2.dispose();

	            ret = tmp;
	        } while (w != width || h != height);

	        image = ret;
	}

	/**
	 * Scales the image to another size using splines algorithm. Width or height can be set
	 * to 0 or negative to calculate automatic value assuming that the same width/height
	 * ratio must be preserved. Splines is the best possible down-scaling algorithm, giving
	 * very good results also when up-scaling, but it should be used with small images.
	 * @param width Width. Set to 0 to calculate value automatically.
	 * @param height Height. Set to 0 to calculate value automatically.
	 * @param sameRatio True to maintain image ratio (maximum size that fits the rectangle
	 * with the provided width and height).
	 * @throws JPARSECException If an error occurs.
	 * @see Resize
	 */
	public void getScaledInstanceUsingSplines(int width, int height, boolean sameRatio) throws JPARSECException {
		path = null;
		if (!ALLOW_SPLINE_RESIZING) {
			this.getScaledInstance(width, height, sameRatio);
			return;
		}

		// Flip in case of negative size
		if (width < 0 || height < 0) {
			boolean h = false, v = false;
			if (width < 0) h = true;
			if (height < 0) v = true;
			flip(h, v);
			width = Math.abs(width);
			height = Math.abs(height);
		}

		image = Resize.resize(image, width, height, sameRatio);
	}

	/**
	 * Rotates an image.
	 * @param ang Rotation angle in radians.
	 * @param radius_x The translation/rotation radius in x, it will be usually
	 * the width of the image * 0.5.
	 * @param radius_y The translation/rotation radius in y, it will be usually
	 * the height of the image * 0.5.
	 */
	public void rotate(double ang, int radius_x, int radius_y) {
		path = null;
		AffineTransform trans = new AffineTransform();

		if (ang != 0.0) trans.rotate(ang, radius_x, radius_y );
		double nw = Math.abs(image.getWidth() * Math.cos(ang)) + Math.abs(image.getHeight() * Math.sin(ang)) + 2;
		double nh = Math.abs(image.getWidth() * Math.sin(ang)) + Math.abs(image.getHeight() * Math.cos(ang)) + 2;
		BufferedImage out = new BufferedImage((int) nw, (int) nh, image.getType());
		Graphics2D g = out.createGraphics();
		g.drawImage(image, trans, null);
		g.dispose();
		image = out;
	}

	/**
	 * Resizes an image.
	 * @param w New width.
	 * @param h New height.
	 */
	public void resize(int w, int h) {
		path = null;
		BufferedImage out = new BufferedImage(w, h, image.getType());
		Graphics2D g = out.createGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		image = out;
	}

	/**
	 * Moves an image.
	 * @param x X displacement.
	 * @param y Y displacement.
	 */
	public void move(int x, int y) {
		path = null;
		BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
		Graphics2D g = out.createGraphics();
		g.drawImage(image, x, y, null);
		g.dispose();
		image = out;
	}

	/**
	 * Shows an image in an external frame without file save nor
	 * mouse drag support.
	 * @param title Frame title.
	 * @throws JPARSECException If an error occurs.
	 */
	public void show(String title)
	throws JPARSECException {
		Dimension d = new Dimension(500, 500);
		if (image != null) d = this.getSize();
		this.show(d.width, d.height, title, false, false, true);
	}

	/**
	 * re-paints the frame.
	 * @throws JPARSECException If an error occurs.
	 */
	public void update ()
	throws JPARSECException {
		canvas.p.setImage(image);
		if (frame != null) this.frame.repaint();
	}
	/**
	 * Returns the current JFrame object.
	 * @return The JFrame object.
	 */
	public JFrame getJFrame()
	{
		return this.frame;
	}

	/**
	 * Returns the current display canvas object.
	 * @return The DisplayCanvas object.
	 */
	public JPanel getCanvas()
	{
		return this.canvas;
	}

	private DisplayCanvas canvas;
	/**
	 * Shows an image in an external frame with file save and mouse drag support.
	 * @param width Frame width.
	 * @param height Frame height.
	 * @param title Frame title.
	 * @param drag True to include drag support.
	 * @param addMenu True to include a menu.
	 * @param decoration Add decoration to window.
	 * @throws JPARSECException If an error occurs.
	 */
	public void show(int width, int height, String title, boolean drag, boolean addMenu,
			boolean decoration)
	throws JPARSECException {
		frame = new javax.swing.JFrame(title);
		JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu(Translate.translate(260));
        menuBar.add(menu);
	    MyAction myAction = new MyAction(this);
        saveAs = new JMenuItem(" "+Translate.translate(261)+"...   ");
	    saveAs.addActionListener(myAction);
	    saveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                                 Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        print = new JMenuItem(" "+Translate.translate(262)+"...   ");
        print.addActionListener(myAction);
        print.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P,
                                 Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        close = new JMenuItem(" "+Translate.translate(263)+"...   ");
        close.addActionListener(myAction);
        close.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
                                 Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menu.add(saveAs);
        menu.add(print);
        menu.add(close);
        if (addMenu) frame.setJMenuBar(menuBar);
        if (!decoration) frame.setUndecorated(true);

		Container container = frame.getContentPane();
		container.setSize(width, height);
		container.setPreferredSize(new Dimension(width, height));

		canvas = new DisplayCanvas(this, width, height, drag);

	    container.add(canvas);
	    frame.setIconImage(image);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setTitle(title);
        frame.setResizable(true);

        frame.pack();
        frame.setVisible(true);
        canvas.paintImmediately(0, 0, width, height);
	}

	/**
	 * Closes the frame.
	 */
	public void dispose()
	{
		frame.dispose();
	}

	/**
	 * Returns the image as an array. The array has 3
	 * dimensions. The last two are the (x, y) position,
	 * while the first one has size 4 for each of the
	 * components: red, green, blue, and alpha colors.
	 * @return The array.
	 */
	public int[][][] getImageAsArray()
	{
		Dimension d = this.getSize();
		int a[][][] = new int[4][d.width][d.height];
		for (int i=0; i<d.width; i++)
		{
			for (int j=0; j<d.height; j++)
			{
				int c[] = Picture.getColorComponents(image.getRGB(i, j));
				a[0][i][j] = c[0];
				a[1][i][j] = c[1];
				a[2][i][j] = c[2];
				a[3][i][j] = c[3];
			}
		}
		return a;
	}

	/**
	 * Returns the image as an array. The array has 2
	 * dimensions (x, y).
	 * @param component The component to return, from 0 (R) to 4
	 * (A): RGBA.
	 * @return The array, or null if component is out of range.
	 */
	public int[][] getImageAsArray(int component)
	{
		if (component < 0 || component > 4) return null;
		Dimension d = this.getSize();
		int a[][] = new int[d.width][d.height];
		int bitoff[] = new int[] {16, 8, 0, 24};
		int bo = bitoff[component];
		for (int i=0; i<d.width; i++)
		{
			for (int j=0; j<d.height; j++)
			{
				if (bo == 0) {
					a[i][j] = image.getRGB(i, j) & 255;
				} else {
					a[i][j] = (image.getRGB(i, j) >> bo) & 255;
				}
			}
		}
		return a;
	}

	/**
	 * Returns the image as an array. The array has 2
	 * dimensions (x, y).
	 * @param component The component to return, from 0 (R) to 4
	 * (A): RGBA.
	 * @return The array, or null if component is out of range.
	 */
	public byte[][] getImageAsByteArray(int component)
	{
		if (component < 0 || component > 4) return null;
		Dimension d = this.getSize();
		byte a[][] = new byte[d.width][d.height];
		int bitoff[] = new int[] {16, 8, 0, 24};
		int bo = bitoff[component];
		for (int i=0; i<d.width; i++)
		{
			for (int j=0; j<d.height; j++)
			{
				if (bo == 0) {
					a[i][j] = (byte) ((image.getRGB(i, j) & 255) - 128);
				} else {
					a[i][j] = (byte) (((image.getRGB(i, j) >> bo) & 255) - 128);
				}
			}
		}
		return a;
	}

	/**
	 * Returns the image as an array (x, y) of RGB colors.
	 * @return The array.
	 */
	public int[][] getImageAsArray2d()
	{
		Dimension d = this.getSize();
		int a[][] = new int[d.width][d.height];
		for (int i=0; i<d.width; i++)
		{
			for (int j=0; j<d.height; j++)
			{
				int c[] = Picture.getColorComponents(image.getRGB(i, j));
				a[i][j] = Functions.getColor(c[0], c[1], c[2], c[3]);
			}
		}
		return a;
	}

	/**
	 * Returns the image as an array 1d of RGB colors, like that
	 * obtained grabbing pixels.
	 * @return The array.
	 */
	public int[] getImageAsArray1d()
	{
		int a[] = new int[getWidth()*getHeight()];
		image.getRGB(0, 0, this.getWidth(), this.getHeight(), a, 0, this.getWidth());
		return a;
	}

	/**
	 * Returns the color for a given RGB value.
	 * @param argb ARGB color.
	 * @return The color.
	 */
	public static Color getColor(int argb) {
		int c[] = Picture.getColorComponents(argb);
        return new Color(c[0], c[1], c[2], c[3]);
    }

	/**
	 * Returns the color components for a given RGB value.
	 * @param argb ARGB color.
	 * @return The color components r, g, b, a.
	 */
	public static int[] getColorComponents(int argb) {
		return Functions.getColorComponents(argb);
    }

	/**
	 * Returns the color at a given position.
	 * @param i X position.
	 * @param j Y position.
	 * @return The color.
	 */
	public Color getColorAt(int i, int j) {
        return Picture.getColor(image.getRGB(i, j));
    }
	/**
	 * Sets the color of a given point.
	 * @param i X position.
	 * @param j Y Position.
	 * @param c The color.
	 */
    public void setColor(int i, int j, Color c) {
		path = null;
        if (c == null)
        	throw new RuntimeException("can't set color to null.");
        image.setRGB(i, j, c.getRGB());
    }

	/**
	 * Sets the color of a given point.
	 * @param i X position.
	 * @param j Y Position.
	 * @param rgb The color.
	 */
    public void setColor(int i, int j, int rgb) {
		path = null;
        image.setRGB(i, j, rgb);
    }

    /**
     * Sets the alpha channel for this image. The image should be transparent, if
     * not you can use {@linkplain #copyWithTransparency(BufferedImage)}.
     * @param alpha The value of the alpha channel, 255 for opaque image, 0 for transparent.
     * @param useMinValue True to set the alpha value to the minimum value between
     * the provided alpha value and the current alpha value of that pixel, false to set
     * the absolute value of alpha to the provided value for all pixels, without
     * considering the alpha of each pixel.
     */
    public void setAlphaChannel(final int alpha, boolean useMinValue) {
		path = null;
    	/*
        ImageFilter filter = new RGBImageFilter()
        {
          public final int filterRGB(int x, int y, int rgb)
          {
            int r = (rgb & 0xFF0000) >> 16;
            int g = (rgb & 0xFF00) >> 8;
            int b = rgb & 0xFF;
			return alpha<<24 | r<<16 | g<<8 | b;
          }
        };

        ImageProducer ip = new FilteredImageSource(image.getSource(), filter);
        Image img = Toolkit.getDefaultToolkit().createImage(ip);
        BufferedImage dest = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg2 = dest.createGraphics();
        gg2.drawImage(img, 0, 0, null);
        gg2.dispose();
        image = dest;
    	 */

    	for (int i=0; i<image.getWidth(); i++)
    	{
        	for (int j=0; j<image.getHeight(); j++)
        	{
        		int c[] = Picture.getColorComponents(image.getRGB(i, j));
        		if (useMinValue) {
        			c[3] = Math.min(alpha, c[3]);
        		} else {
        			c[3] = alpha;
        		}
        		this.setColor(i, j, Functions.getColor(c[0], c[1], c[2], c[3]));
        	}
    	}

    }

    /**
     * Converts current image to flat version. Completely transparent colors will be changed to the
     * input color.
     * @param flatColor The color to set for transparent points.
     * @throws JPARSECException If an error occurs.
     */
    public void flatten(Color flatColor) throws JPARSECException {
		path = null;
		changeColor(new Color(0, 0, 0, 0), flatColor, false);
    }

    /**
     * Changes one color with another.
     * @param col0 Initial color.
     * @param col1 New color.
     * @param both True to change also pixels with
     * new color with the initial one.
     * @throws JPARSECException If an error occurs.
     */
    public void changeColor(Color col0, Color col1, boolean both) throws JPARSECException
    {
		path = null;
    	int col0rgb = col0.getRGB();
    	int col1rgb = col1.getRGB();

    	int a[] = this.getImageAsArray1d();
    	for (int i=0; i<a.length; i++)
    	{
    		if (a[i] == col0rgb) {
    			a[i] = col1rgb;
    		} else {
    			if (both && a[i] == col1rgb)
    				a[i] = col0rgb;
    		}
    	}
    	this.setImage(a);
    }

	/**
	 * Returns a label with the current image to include in a GUI widget.
	 * @return The label.
	 */
	public JLabel getAsJLabel() {
        if (image == null) { return null; }         // no image available
        if (path != null && FileIO.exists(path) && path.toLowerCase().endsWith(".gif")) {
            ImageIcon icon = new ImageIcon(path);
            return new JLabel(icon);        	
        }
        ImageIcon icon = new ImageIcon(image);
        return new JLabel(icon);
    }

	/**
	 * Prints an image to a printer.
	 * @throws JPARSECException If an error occurs.
	 */
	private void print()
	throws JPARSECException {
		Printer print = new Printer(this.frame);
		print.print();
	}

	/**
	 * Prints an image to a printer. The image is previously
	 * shown in an external frame.
	 * @param title Frame title.
	 * @throws JPARSECException If an error occurs.
	 */
	public void print(String title)
	throws JPARSECException {
		javax.swing.JFrame frame = new javax.swing.JFrame(title);
		Container container = frame.getContentPane();

		Dimension d = this.getSize();
		DisplayCanvas canvas = new DisplayCanvas(this, d.width, d.height, false);
	    container.add(canvas);
		frame.setSize(d.width, d.height);

		Printer print = new Printer(frame);
		print.print();
	}

	/**
	 * Transforms an Image to a BufferedImage. Pixel grabber
     * is used to retrieve the image's color model and see
     * if it has alpha channel.
	 * @param image The image.
	 * @return The BufferedImage.
	 * @throws JPARSECException In case of error using pixel grabber.
	 */
    public static BufferedImage toBufferedImage(Image image) throws JPARSECException {
        if (image instanceof BufferedImage) {
            return (BufferedImage)image;
        }

        // This code ensures that all the pixels in the image are loaded
        image = new ImageIcon(image).getImage();

        // Determine if the image has transparent pixels; for this method's
        // implementation, see e661 Determining If an Image Has Transparent Pixels
        boolean hasAlpha = hasAlpha(image);

        // Create a buffered image with a format that's compatible with the screen
        BufferedImage bimage = null;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try {
            // Determine the type of transparency of the new buffered image
            int transparency = Transparency.OPAQUE;
            if (hasAlpha) {
                transparency = Transparency.BITMASK;
            }

            // Create the buffered image
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            bimage = gc.createCompatibleImage(
                image.getWidth(null), image.getHeight(null), transparency);
        } catch (HeadlessException e) {
            // The system does not have a screen
        }

        if (bimage == null) {
            // Create a buffered image using the default color model
            int type = BufferedImage.TYPE_INT_RGB;
            if (hasAlpha) {
                type = BufferedImage.TYPE_INT_ARGB;
            }
            bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
        }

        // Copy image to buffered image
        Graphics g = bimage.createGraphics();

        // Paint the image onto the buffered image
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return bimage;
    }
    /**
     * Returns true if the specified image has transparent pixels. Pixel grabber
     * is used to retrieve the image's color model.
     * @throws JPARSECException In case of error using pixel grabber.
     */
    private static boolean hasAlpha(Image image) throws JPARSECException {
        // If buffered image, the color model is readily available
        if (image instanceof BufferedImage) {
            BufferedImage bimage = (BufferedImage)image;
            return bimage.getColorModel().hasAlpha();
        }

        // Use a pixel grabber to retrieve the image's color model;
        // grabbing a single pixel is usually sufficient
        PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
        try {
            pg.grabPixels();
        } catch (InterruptedException e) {
        	throw new JPARSECException("Error using the pixel grabber.");
        }

        // Get the image's color model
        ColorModel cm = pg.getColorModel();
        if (cm == null) return false;
        return cm.hasAlpha();
    }

    /**
     * Returns true if the specified image has transparent pixels. Pixel grabber
     * is used to retrieve the image's color model.
     * @return True of false.
     * @throws JPARSECException  In case of error using pixel grabber.
     */
    public boolean hasAlpha() throws JPARSECException
    {
    	return Picture.hasAlpha(this.image);
    }

    /**
     * Transforms the current image into transparent.
     * @param transparencyLevel Transparency level, from 0 (invisible pixel)
     * to 255 (full opaque pixel).
     * @param transparentCol The color to be modified to some level. Can be null
     * to affect all colors.
     * of transparency, or null to change the whole image.
     */
    public void makeTransparent(int transparencyLevel, Color transparentCol)
    {
		path = null;
    	Dimension d = this.getSize();
        BufferedImage buf = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB);

        Color backG = new Color(0,0,0);
        boolean back = false;
        int tr = -1, tg = -1, tb = -1;
        if (transparentCol != null) {
	        tr = transparentCol.getRed();
	        tg = transparentCol.getGreen();
	        tb = transparentCol.getBlue();
        }
    	for (int i=0; i<d.width; i++)
    	{
    		for (int j=0; j<d.height; j++)
    		{
    			Color col = this.getColorAt(i, j);
    			Color newCol = col;
    			int cr = col.getRed(), cg = col.getGreen(), cb = col.getBlue(), ca = col.getAlpha();
    			boolean change = false;
    			if (transparentCol == null) {
    				change = true;
    			} else {
    				if (cr == tr && cg == tg &&	cb == tb) change = true;
    			}
    			int level = transparencyLevel;
    			int t0 = (ca<<24); //Functions.getColor(0, 0, 0, ca);
    			int t255 = (ca<<24 | 16777215); //Functions.getColor(255, 255, 255, ca);
    			if (col.getRGB() == t0 ||
    					col.getRGB() == t255) {
    				if (!back) {
    					back = true;
    					backG = new Color(cr, cg, cb, col.getAlpha());
    				}
    				if (col.getRGB() == backG.getRGB()) level = 0;
    			}
    			if (change) newCol = new Color(cr, cg, cb, level);
    			buf.setRGB(i, j, newCol.getRGB());
    		}
    	}
    	this.image = buf;
    }

    /**
     * Transforms the current image into transparent, for a given range of colors.
     * @param c1 Color with the lowest values of the rgb components.
     * @param c2 Color with the highest values of the rgb components.
     * @param t Transparency level, 0 for fully transparent, 255 for opaque.
     */
    public void makeTransparent(Color c1, Color c2, final int t)
    {
		path = null;
    	Dimension d = this.getSize();
        BufferedImage buf = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = buf.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        // Primitive test, just an example
        final int r1 = c1.getRed();
        final int g1 = c1.getGreen();
        final int b1 = c1.getBlue();
        final int r2 = c2.getRed();
        final int g2 = c2.getGreen();
        final int b2 = c2.getBlue();
        ImageFilter filter = new RGBImageFilter()
        {
          public final int filterRGB(int x, int y, int rgb)
          {
            int r = (rgb & 0xFF0000) >> 16;
            int g = (rgb & 0xFF00) >> 8;
            int b = rgb & 0xFF;
            if (r >= r1 && r <= r2 &&
                g >= g1 && g <= g2 &&
                b >= b1 && b <= b2)
            {
                return t<<24 | r<<16 | g<<8 | b;
            }
            return rgb;
          }
        };

        ImageProducer ip = new FilteredImageSource(buf.getSource(), filter);
        Image img = Toolkit.getDefaultToolkit().createImage(ip);
        BufferedImage dest = new BufferedImage(
                d.width, d.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg2 = dest.createGraphics();
        gg2.drawImage(img, 0, 0, null);
        gg2.dispose();
        image = dest;
    }

    /**
     * Transforms the current image into transparent, for a given range of colors.
     * @param buf The image.
     * @param c1 Color with the lowest values of the rgb components.
     * @param c2 Color with the highest values of the rgb components.
     * @param t Transparency level, 0 for fully transparent, 255 for opaque.
     * @return The new image.
     */
    public static BufferedImage makeTransparent(BufferedImage buf, Color c1, Color c2, final int t)
    {
        // Primitive test, just an example
        final int r1 = c1.getRed();
        final int g1 = c1.getGreen();
        final int b1 = c1.getBlue();
        final int r2 = c2.getRed();
        final int g2 = c2.getGreen();
        final int b2 = c2.getBlue();
        ImageFilter filter = new RGBImageFilter()
        {
          public final int filterRGB(int x, int y, int rgb)
          {
            int r = (rgb & 0xFF0000) >> 16;
            int g = (rgb & 0xFF00) >> 8;
            int b = rgb & 0xFF;
            if (r >= r1 && r <= r2 &&
                g >= g1 && g <= g2 &&
                b >= b1 && b <= b2)
            {
              return t<<24 | r<<16 | g<<8 | b;
            }
            return rgb;
          }
        };

        ImageProducer ip = new FilteredImageSource(buf.getSource(), filter);
        Image img = Toolkit.getDefaultToolkit().createImage(ip);

        BufferedImage dest = new BufferedImage(buf.getWidth(), buf.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg2 = dest.createGraphics();
        gg2.drawImage(img, 0, 0, null);
        gg2.dispose();
        return dest;
    }
    /**
     * A suitable pattern to produce a sharp effect.
     */
    public static final double[] PATTERN_SHARP = {
    		0.0, -1.0, 0.0,
    		-1.0, 5.0, -1.0,
    		0.0, -1.0, 0.0};
    /**
     * A suitable pattern to produce an identity transformation.
     */
    public static final double[] PATTERN_IDENTITY = {
		0.0, 0.0, 0.0,
		0.0, 1.0, 0.0,
		0.0, 0.0, 0.0};
    /**
     * A suitable pattern to produce a blur effect.
     */
    public static final double[] PATTERN_BLUR = {
		1.0/9.0, 1.0/9.0, 1.0/9.0,
		1.0/9.0, 1.0/9.0, 1.0/9.0,
		1.0/9.0, 1.0/9.0, 1.0/9.0};
    /**
     * A suitable pattern to produce a edge detection effect.
     */
    public static final double[] PATTERN_EDGE = {
		0.0, -1.0, 0.0,
		-1.0, 4.0, -1.0,
		0.0, -1.0, 0.0};
    /**
     * A suitable pattern to produce a sharpening effect.
     */
    public static final double[] PATTERN_SHARPENING = {
		0.0, -1.0/2.0, 0.0,
		-1.0/2.0, 3.0, -1.0/2.0,
		0.0, -1.0/2.0, 0.0};

    /**
     * Convolves an image with a given pattern.
     * @param pattern Pattern to apply. Some constants defined in this class.
     */
	public void convolve(double pattern[])
	{
		path = null;
		float myPattern[] = new float[pattern.length];
		for (int i=0; i<pattern.length; i++)
		{
			myPattern[i] = (float) pattern[i];
		}
		Dimension d = this.getSize();
		BufferedImage bufferedImage = new BufferedImage(d.width, d.height, image.getType());
	    Graphics2D big = bufferedImage.createGraphics();
	    int w = (int) Math.sqrt(pattern.length);
	    Kernel kernel = new Kernel(w, w, myPattern);
	    ConvolveOp convolveOp = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
	    big.drawImage(image, convolveOp, 0, 0);
	    image = bufferedImage;
	}

	/**
	 * Inverts the colors of an image, giving a negative effect.
	 */
	public void invertColors()
	{
		path = null;
		Dimension d = this.getSize();

		for (int i=0; i<d.width; i++)
		{
			for (int j=0; j<d.height; j++)
			{
				int rgb = image.getRGB(i, j);
				int alpha = (rgb >> 24) & 255;
				Color col = new Color(rgb);
				int red = 255 - col.getRed();
				int green = 255 - col.getGreen();
				int blue = 255 - col.getBlue();
				Color inverted = new Color(red, green, blue, alpha);
				rgb = inverted.getRGB();
				image.setRGB(i, j, rgb);
			}
		}
	}

	/**
	 * Inverts the colors of an image, giving a negative effect.
	 * @param r True to invert red channel.
	 * @param g True to invert green channel.
	 * @param b True to invert blue channel.
	 */
	public void invertColors(boolean r, boolean g, boolean b)
	{
		path = null;
		Dimension d = this.getSize();

		for (int i=0; i<d.width; i++)
		{
			for (int j=0; j<d.height; j++)
			{
				int rgb = image.getRGB(i, j);
				int alpha = (rgb >> 24) & 255;
				Color col = new Color(rgb);
				int red = col.getRed(), green = col.getGreen(), blue = col.getBlue();
				if (r) red = 255 - red;
				if (g) green = 255 - green;
				if (b) blue = 255 - blue;
				Color inverted = new Color(red, green, blue, alpha);
				rgb = inverted.getRGB();
				image.setRGB(i, j, rgb);
			}
		}
	}

	/**
	 * Encodes an image into a string in png format.
	 * @param getHtmlSource True to get directly the contents of the srcc property
	 * of the html img tag to show the image.
	 * @return The encoded image, in base64 and png format.
	 * @throws JPARSECException If an error occurs.
	 */
	public String imageToString(boolean getHtmlSource)
	throws JPARSECException {
        String imageString = null;

        //image to bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
            baos.flush();
            byte[] imageAsRawBytes = baos.toByteArray();
            baos.close();

            //bytes to string
            imageString = new String(Base64Coder.encode(imageAsRawBytes));
        } catch (IOException ex) {
            throw new JPARSECException(ex);
        }

        if (getHtmlSource) imageString = "data:image/png;base64,"+imageString;
        return imageString;
    }

	/**
	 * Encodes an image into a string in a given format, using base64.
	 * @param format The informal format of the image, for example png, gif, jpg, ...
	 * @param getHtmlSource True to get directly the contents of the html img tag
	 * to show the image.
	 * @return The encoded image, encoded in base64.
	 * @throws JPARSECException If an error occurs.
	 */
	public String imageToString(String format, boolean getHtmlSource)
	throws JPARSECException {
        String imageString = null;

        //image to bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, format, baos);
            baos.flush();
            byte[] imageAsRawBytes = baos.toByteArray();
            baos.close();

            //bytes to string
            imageString = new String(Base64Coder.encode(imageAsRawBytes));
        } catch (IOException ex) {
            throw new JPARSECException(ex);
        }

        if (getHtmlSource) imageString = "data:image/"+format+";base64,"+imageString;
        return imageString;
    }

	/**
	 * Encodes an image into a string in jpg format, using base64.
	 * @param quality The quality of the jpg from 0 to 1 (max).
	 * @param getHtmlSource True to get directly the contents of the html img tag
	 * to show the image.
	 * @return The encoded image, encoded in base64.
	 * @throws JPARSECException If an error occurs.
	 */
	public String imageToStringInJPG(double quality, boolean getHtmlSource)
	throws JPARSECException {
        String imageString = null, format = "jpg";

        //image to bytes
        try {
            byte[] imageAsRawBytes = EncoderUtil.encode(image, "jpeg", (float) quality);

            //bytes to string
            imageString = new String(Base64Coder.encode(imageAsRawBytes));
        } catch (IOException ex) {
            throw new JPARSECException(ex);
        }

        if (getHtmlSource) imageString = "data:image/"+format+";base64,"+imageString;
        return imageString;
    }

	/**
	 * Decodes a string and returns the image.
	 * @param imageString The image encoded as a string.
	 * @return The image.
	 * @throws JPARSECException If an error occurs.
	 */
    public static BufferedImage stringToImage(String imageString)
    throws JPARSECException {
        //string to ByteArrayInputStream
        BufferedImage bImage = null;
        try {
            byte[] output = Base64Coder.decode(imageString);
            ByteArrayInputStream bais = new ByteArrayInputStream(output);
            bImage = ImageIO.read(bais);
        } catch (IOException ex) {
            throw new JPARSECException(ex);
        }

        return bImage;
    }

    /**
     * Returns a clone copy of the input image.
     * @param img The image.
     * @return The copy. In case the input image is of an undefined or
     * custom type, a copy with transparency is returned.
     */
    public static BufferedImage copy(BufferedImage img) {
    	if (img.getType() == BufferedImage.TYPE_CUSTOM) return copyWithTransparency(img);
    	BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), img.getType());

	    // Copy image to buffered image
	    Graphics g = bimage.createGraphics();

	    // Paint the image onto the buffered image
	    g.drawImage(img, 0, 0, null);
	    g.dispose();

	    return bimage;
    }

    /**
     * Returns a clone copy of the input image including
     * an alpha channel.
     * @param img The image.
     * @return The copy.
     */
    public static BufferedImage copyWithTransparency(BufferedImage img) {
    	BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

	    // Copy image to buffered image
	    Graphics g = bimage.createGraphics();

	    // Paint the image onto the buffered image
	    g.drawImage(img, 0, 0, null);
	    g.dispose();

	    return bimage;
    }

	/**
	 * Inverts a color.
	 * @param col The color.
	 * @return The inverted color.
	 */
	public static Color invertColor (Color col)
	{
		int red = 255 - col.getRed();
		int blue = 255 - col.getBlue();
		int green = 255 - col.getGreen();
		return new Color(red, green, blue);
	}
	/**
	 * Gives certain level of transparency to a color.
	 * @param c The color.
	 * @param i Transparency, from 0 (transparent) to 255 (opaque).
	 * @return The color with transparency applied.
	 */
	public static Color setTransparent (Color c, int i)
	{
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), i);
	}
	
	/**
	 * Gives certain level of transparency to a set of colors.
	 * @param c The colors.
	 * @param j Transparency, from 0 (transparent) to 255 (opaque).
	 * @return The colors with transparency applied.
	 */
	public static Color[] setTransparent(Color[] c, int j) {
		for (int i=0; i<c.length; i++)
		{
			c[i] = setTransparent(c[i], j);
		}
		return c;
	}

	/**
	 * Transforms a given color in an image to transparent.
	 * @param im Input image.
	 * @param color Input color.
	 * @return Transformed image.
	 */
    public static Image makeColorTransparent(BufferedImage im, final Color color) {
        ImageFilter filter = new RGBImageFilter() {

                // the color we are looking for... Alpha bits are set to opaque
                public int markerRGB = color.getRGB() | 0xFF000000;

                public final int filterRGB(int x, int y, int rgb) {
                        if ((rgb | 0xFF000000) == markerRGB) {
                                // Mark the alpha bits as zero - transparent
                                return 0x00FFFFFF & rgb;
                        } else {
                                // nothing to do
                                return rgb;
                        }
                }
        };

        ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
        return Toolkit.getDefaultToolkit().createImage(ip);
    }

	/**
	 * The RGB components.
	 */
	public enum RGB {
		/** ID constant for red component. */
		COLOR_RED,
		/** ID constant for green component. */
		COLOR_GREEN,
		/** ID constant for blue component. */
		COLOR_BLUE
	};

	/**
	 * Inverts the colors of an image, giving a negative effect.
	 * @param componentToInvert ID constant of the component to invert.
	 */
	public void invertColor(RGB componentToInvert)
	{
		path = null;
		Dimension d = this.getSize();

		for (int i=0; i<d.width; i++)
		{
			for (int j=0; j<d.height; j++)
			{
				int rgb = image.getRGB(i, j);
				Color color = new Color(rgb);

				switch (componentToInvert)
			    {
			    case COLOR_RED:
					color = new Color(255-color.getRed(), color.getGreen(), color.getBlue());
			    	break;
			    case COLOR_GREEN:
					color = new Color(color.getRed(), 255-color.getGreen(), color.getBlue());
			    	break;
			    case COLOR_BLUE:
					color = new Color(color.getRed(), color.getGreen(), 255-color.getBlue());
			    	break;
			    }
				image.setRGB(i, j, color.getRGB());
			}
		}
	}

	/**
	 * Creates an SVG file by calling a given generic drawing method.
	 * @param file_name The file name to create.
	 * @param instance The instance will render the image.
	 * @param method The name of the method that renders the image. This method
	 * must have only one input parameter: the Graphics2D drawing instance.
	 * @param w The width of the output image.
	 * @param h The height of the output image.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void createSVGPicture(String file_name, Object instance, String method, int w, int h) throws JPARSECException {
		File plotFile = new File(file_name);
		final Dimension size = new Dimension(w, h);
		try
		{
			// Using reflection so that everything will work without freehep in classpath (not this)
			Class c = Class.forName("org.freehep.graphicsio.svg.SVGGraphics2D");
			Constructor cc = c.getConstructor(new Class[] {plotFile.getClass(), size.getClass()});
			Object svgGraphics = cc.newInstance(new Object[] {plotFile, size});
			Method m = c.getMethod("startExport");
			m.invoke(svgGraphics);

			Method call = instance.getClass().getMethod(method, Graphics2D.class);
			call.invoke(instance, svgGraphics);

			Method mm = c.getMethod("endExport");
			mm.invoke(svgGraphics);
		} catch (Exception e)
		{
			throw new JPARSECException("cannot write to file.", e);
		}
	}

	/**
	 * Creates an EPS file by calling a given generic drawing method.
	 * @param file_name The file name to create.
	 * @param instance The instance will render the image.
	 * @param method The name of the method that renders the image. This method
	 * must have only one input parameter: the Graphics2D drawing instance.
	 * @param w The width of the output image.
	 * @param h The height of the output image.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void createEPSPicture(String file_name, Object instance, String method, int w, int h) throws JPARSECException {
		File plotFile = new File(file_name);
		final Dimension size = new Dimension(w, h);
		try
		{
			// Using reflection so that everything will work without freehep in classpath (not this)
			Class c = Class.forName("org.freehep.graphicsio.ps.PSGraphics2D");
			Constructor cc = c.getConstructor(new Class[] {plotFile.getClass(), size.getClass()});
			Object svgGraphics = cc.newInstance(new Object[] {plotFile, size});
			Method m = c.getMethod("startExport");
			m.invoke(svgGraphics);

			Method call = instance.getClass().getMethod(method, Graphics2D.class);
			call.invoke(instance, svgGraphics);

			Method mm = c.getMethod("endExport");
			mm.invoke(svgGraphics);
		} catch (Exception e)
		{
			throw new JPARSECException("cannot write to file.", e);
		}
	}

	/**
	 * Creates an PDF file by calling a given generic drawing method.
	 * @param file_name The file name to create.
	 * @param instance The instance will render the image.
	 * @param method The name of the method that renders the image. This method
	 * must have only one input parameter: the Graphics2D drawing instance.
	 * @param w The width of the output image.
	 * @param h The height of the output image.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void createPDFPicture(String file_name, Object instance, String method, int w, int h) throws JPARSECException {
		File plotFile = new File(file_name);
		final Dimension size = new Dimension(w, h);
		try
		{
			// Using reflection so that everything will work without freehep in classpath (not this)
			Class c = Class.forName("org.freehep.graphicsio.pdf.PDFGraphics2D");
			Constructor cc = c.getConstructor(new Class[] {plotFile.getClass(), size.getClass()});
			Object svgGraphics = cc.newInstance(new Object[] {plotFile, size});
			Method m = c.getMethod("startExport");
			m.invoke(svgGraphics);

			Method call = instance.getClass().getMethod(method, Graphics2D.class);
			call.invoke(instance, svgGraphics);

			Method mm = c.getMethod("endExport");
			mm.invoke(svgGraphics);
		} catch (Exception e)
		{
			throw new JPARSECException("cannot write to file.", e);
		}
	}

	private static void exportAsSVGFile(String file_name, BufferedImage image) throws JPARSECException
	{
		File plotFile = new File(file_name);
		Picture pio = new Picture(image);
		final Dimension size = pio.getSize();
		try
		{
			// Using reflection so that everything will work without freehep in classpath (not this)
			Class c = Class.forName("org.freehep.graphicsio.svg.SVGGraphics2D");
			Constructor cc = c.getConstructor(new Class[] {plotFile.getClass(), size.getClass()});
			Object svgGraphics = cc.newInstance(new Object[] {plotFile, size});
			Method m = c.getMethod("startExport");
			m.invoke(svgGraphics);
			((Graphics2D) svgGraphics).drawImage(image, new AffineTransform(), null);
			Method mm = c.getMethod("endExport");
			mm.invoke(svgGraphics);
		} catch (Exception e)
		{
			throw new JPARSECException("cannot write to file.", e);
		}
	}

	private static void exportAsEPSFile(String file_name, BufferedImage image) throws JPARSECException
	{
		File plotFile = new File(file_name);
		Picture pio = new Picture(image);
		final Dimension size = pio.getSize();
		try
		{
			// Using reflection so that everything will work without freehep in classpath (not this)
			Class c = Class.forName("org.freehep.graphicsio.ps.PSGraphics2D");
			Constructor cc = c.getConstructor(new Class[] {plotFile.getClass(), size.getClass()});
			Object psGraphics = cc.newInstance(new Object[] {plotFile, size});
			Method m = c.getMethod("startExport");
			m.invoke(psGraphics);
			((Graphics2D) psGraphics).drawImage(image, new AffineTransform(), null);
			Method mm = c.getMethod("endExport");
			mm.invoke(psGraphics);
		} catch (Exception e)
		{
			throw new JPARSECException("cannot write to file.", e);
		}
	}

	private static void exportAsPDFFile(String file_name, BufferedImage image) throws JPARSECException
	{
		File plotFile = new File(file_name);
		Picture pio = new Picture(image);
		final Dimension size = pio.getSize();
		try
		{
			// Using reflection so that everything will work without freehep in classpath (not this)
			Class c = Class.forName("org.freehep.graphicsio.pdf.PDFGraphics2D");
			Constructor cc = c.getConstructor(new Class[] {plotFile.getClass(), size.getClass()});
			Object pdfGraphics = cc.newInstance(new Object[] {plotFile, size});
			Method m = c.getMethod("startExport");
			m.invoke(pdfGraphics);
			((Graphics2D) pdfGraphics).drawImage(image, new AffineTransform(), null);
			Method mm = c.getMethod("endExport");
			mm.invoke(pdfGraphics);
		} catch (Exception e)
		{
			throw new JPARSECException("cannot write to file.", e);
		}
	}

	/**
	 *  Create a BufferedImage for Swing components.
	 *  The entire component will be captured to an image.
	 *
	 *  @param	 component Swing component to create image from.
	 *  @return	image The image for the given region.
	 *  @exception IOException If an error occurs during writing.
	*/
	public static BufferedImage createImage(JComponent component)
		throws IOException
	{
		Dimension d = component.getSize();

		if (d.width == 0)
		{
			d = component.getPreferredSize();
			component.setSize( d );
		}

		java.awt.Rectangle region = new java.awt.Rectangle(0, 0, d.width, d.height);
		return Picture.createImage(component, region);
	}

	/**
	 *  Create a BufferedImage for Swing components.
	 *  All or part of the component can be captured to an image.
	 *
	 *  @param	 component Swing component to create image from.
	 *  @param	 region The region of the component to be captured to an image.
	 *  @return	image The image for the given region.
	 *  @exception IOException If an error occurs during writing.
	*/
	public static BufferedImage createImage(JComponent component, java.awt.Rectangle region)
		throws IOException
	{
		boolean opaqueValue = component.isOpaque();
		component.setOpaque( true );
		BufferedImage image = new BufferedImage(region.width, region.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = image.createGraphics();

		// Enable antialiasing for shapes
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

        // Enable antialiasing for text
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		g2d.setClip( region );
		component.paint( g2d );
		g2d.dispose();
		component.setOpaque( opaqueValue );
		return image;
	}

	/**
	 * Transforms the image to grayscale.
	 */
	public void toGrayScale() {
		path = null;
		BufferedImage gray = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		Graphics g = gray.getGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		image = gray;
	}

    /**
     * Returns the monochrome luminance of a given color.
     * @param color The color.
     * @return The luminance.
     */
    public static double lum(Color color) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        return .299*r + .587*g + .114*b;
    }

    /**
     * Returns a gray version of this Color.
     * @param color The color.
     * @return The gray version.
     */
    public static Color toGray(Color color) {
        int y = (int) (Math.round(lum(color)));   // round to nearest int
        Color gray = new Color(y, y, y, color.getAlpha());
        return gray;
    }

	/**
	 * Modifies the contrast and/or brightness of the image. To increase the
	 * contrast the usual way is to use a scale of 1.3 and to calculate the
	 * offset with the expression offset = 255 - 255 * scale.
	 * @param scale The scale factor, 1.0 to do nothing. It is a
	 * multiplication factor, use around 1.2 to increase contrast and
	 * obtain a visible effect. Can be lower than unity.
	 * @param offset The brightness offset, use 0 to do nothing. It is
	 * an additive factor, use around 10 to increase brightness and obtain
	 * a visible effect. Can be negative.
	 */
	public void adjustContrastAndBrightness(double scale, double offset) {
		path = null;
		RescaleOp rescaleOp = new RescaleOp((float) scale, (float)offset, null);
		rescaleOp.filter(Picture.copy(image), image);
	}

	/**
	 * Removes the alpha channel of the image, setting transparent colors
	 * to a given color.
	 * @param image The input transparent image.
	 * @param fillColor The color to fill transparent pixels.
	 * @return output image.
	 */
	public static BufferedImage removeAlpha(BufferedImage image, Color fillColor) {
		int w = image.getWidth();
		int h = image.getHeight();
		BufferedImage image2 = new BufferedImage(w, h,  BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image2.createGraphics();
		if (fillColor != null) {
			g.setColor(fillColor);
			g.fillRect(0,0,w,h);
		}
		g.drawRenderedImage(image, null);
		g.dispose();
		return image2;
	}

	/**
	 * Applies a median NEWS filter to remove noise. Based on code by John Burkardt.
	 * This method can be called few times to remove noise more effectively.
	 * @param aggressive Level of denoise intensity, from 0 to 2. The median is applied
	 * to a greater size around each pixel for level 2 (up to 21 pixels).
	 */
	public void denoise(int aggressive) {
		path = null;
	  int i, j;
	  double p[] = new double[21];

	  int rgb[][][] = this.getImageAsArray();
	  int m = rgb[0].length, n = rgb[0][0].length;

	  for (int c=0; c<4; c++) {
		  int rgb2[][] = new int[m][n];

		  //  Process the main part of the image:
		  for ( i = 1; i < m - 1; i++ )
		  {
		    for ( j = 1; j < n - 1; j++ )
		    {
		      p[0] = rgb[c][i-1][j];
		      p[1] = rgb[c][i+1][j];
		      p[2] = rgb[c][i][j+1];
		      p[3] = rgb[c][i][j-1];
		      p[4] = rgb[c][i][j];

		      if (aggressive > 0 && i > 1 && i < m - 2 && j > 1 && j < n - 2) {
			      p[5] = rgb[c][i-1][j-1];
			      p[6] = rgb[c][i+1][j+1];
			      p[7] = rgb[c][i-1][j+1];
			      p[8] = rgb[c][i+1][j-1];
			      p[9] = rgb[c][i-2][j];
			      p[10] = rgb[c][i+2][j];
			      p[11] = rgb[c][i][j+2];
			      p[12] = rgb[c][i][j-2];
			      if (aggressive > 1) {
				      p[13] = rgb[c][i-2][j+1];
				      p[14] = rgb[c][i-2][j-1];
				      p[15] = rgb[c][i+2][j+1];
				      p[16] = rgb[c][i+2][j-1];
				      p[17] = rgb[c][i-1][j+2];
				      p[18] = rgb[c][i-1][j-2];
				      p[19] = rgb[c][i+1][j+2];
				      p[20] = rgb[c][i+1][j-2];

			    	  rgb2[i][j] = (int) DataSet.getKthSmallestValue (p, p.length, p.length/2); //i4vec_median ( 5, p );
			      } else {
			    	  rgb2[i][j] = (int) DataSet.getKthSmallestValue (p, 13, 13/2); //i4vec_median ( 5, p );
			      }
		      } else {
			      rgb2[i][j] = (int) DataSet.getKthSmallestValue (p, 5, 5/2); //i4vec_median ( 5, p );
		      }
		    }
		  }
		  //  Process the four borders.
		  //  Get an odd number of data points,
		  for ( i = 1; i < m - 1; i++ )
		  {
			  j = 0;
		      p[0] = rgb[c][i-1][j];
		      p[1] = rgb[c][i+1][j];
		      p[2] = rgb[c][i][j];
		      p[3] = rgb[c][i][j+1];
		      p[4] = rgb[c][i][j+2];

		      if (aggressive > 0) {
			      p[5] = rgb[c][i+1][j+1];
			      p[6] = rgb[c][i-1][j+1];
			      if (aggressive > 1 && i > 1 && i < m - 2 && j > 1 && j < n - 2) {
				      p[7] = rgb[c][i+2][j];
				      p[8] = rgb[c][i-2][j];
				      p[9] = rgb[c][i+1][j+2];
				      p[10] = rgb[c][i-1][j+2];
			    	  rgb2[i][j] = (int) DataSet.getKthSmallestValue (p, 11, 11/2);
			      } else {
			    	  rgb2[i][j] = (int) DataSet.getKthSmallestValue (p, 7, 7/2);
			      }
		      } else {
		    	  rgb2[i][j] = (int) DataSet.getKthSmallestValue (p, 5, 5/2);
		      }

		      j = n - 1;
		      p[0] = rgb[c][i-1][j];
		      p[1] = rgb[c][i+1][j];
		      p[2] = rgb[c][i][j-2];
		      p[3] = rgb[c][i][j-1];
		      p[4] = rgb[c][i][j];

		      if (aggressive > 0) {
			      p[5] = rgb[c][i+1][j-1];
			      p[6] = rgb[c][i-1][j-1];
			      if (aggressive > 1 && i > 1 && i < m - 2 && j > 1 && j < n - 2) {
				      p[7] = rgb[c][i+2][j];
				      p[8] = rgb[c][i-2][j];
				      p[9] = rgb[c][i+1][j-2];
				      p[10] = rgb[c][i-1][j-2];
			    	  rgb2[i][j] = (int) DataSet.getKthSmallestValue (p, 11, 11/2);
			      } else {
			    	  rgb2[i][j] = (int) DataSet.getKthSmallestValue (p, 7, 7/2);
			      }
		      } else {
		    	  rgb2[i][j] = (int) DataSet.getKthSmallestValue (p, 5, 5/2);
		      }
		  }

		  for ( j = 1; j < n - 1; j++ )
		  {
			  i = 0;
		      p[0] = rgb[c][i][j];
		      p[1] = rgb[c][i+1][j];
		      p[2] = rgb[c][i+2][j];
		      p[3] = rgb[c][i][j-1];
		      p[4] = rgb[c][i][j+1];

		      if (aggressive > 0) {
			      p[5] = rgb[c][i+1][j+1];
			      p[6] = rgb[c][i+1][j-1];
			      if (aggressive > 1 && i > 1 && i < m - 2 && j > 1 && j < n - 2) {
				      p[7] = rgb[c][i][j+2];
				      p[8] = rgb[c][i][j-1];
				      p[9] = rgb[c][i+2][j+1];
				      p[10] = rgb[c][i+2][j-1];
			    	  rgb2[i][j] = (int) DataSet.getKthSmallestValue (p, 11, 11/2);
			      } else {
			    	  rgb2[i][j] = (int) DataSet.getKthSmallestValue (p, 7, 7/2);
			      }
		      } else {
		    	  rgb2[i][j] = (int) DataSet.getKthSmallestValue (p, 5, 5/2);
		      }

		      i = m - 1;
		      p[0] = rgb[c][i-2][j];
		      p[1] = rgb[c][i-1][j];
		      p[2] = rgb[c][i][j];
		      p[3] = rgb[c][i][j-1];
		      p[4] = rgb[c][i][j+1];

		      if (aggressive > 0) {
			      p[5] = rgb[c][i-1][j-1];
			      p[6] = rgb[c][i-1][j+1];
			      if (aggressive > 1 && i > 1 && i < m - 2 && j > 1 && j < n - 2) {
				      p[7] = rgb[c][i][j+2];
				      p[8] = rgb[c][i][j-1];
				      p[9] = rgb[c][i-2][j+1];
				      p[10] = rgb[c][i-2][j-1];
			    	  rgb2[i][j] = (int) DataSet.getKthSmallestValue (p, 11, 11/2);
			      } else {
			    	  rgb2[i][j] = (int) DataSet.getKthSmallestValue (p, 7, 7/2);
			      }
		      } else {
		    	  rgb2[i][j] = (int) DataSet.getKthSmallestValue (p, 5, 5/2);
		      }
		  }

		  //  Process the four corners.
		  i = 0;
		  j = 0;
	      p[0] = rgb[c][i+1][j];
	      p[1] = rgb[c][i][j];
	      p[2] = rgb[c][i][j+1];
	      if (aggressive > 0) {
		      p[3] = rgb[c][i+1][j+1];
		      p[4] = rgb[c][i+2][j];
	    	  rgb2[i][j] = (int) DataSet.getKthSmallestValue (p, 5, 5/2);
	      } else {
	    	  rgb2[i][j] = (int) DataSet.getKthSmallestValue (p, 3, 3/2);
	      }

		  i = 0;
		  j = n - 1;
	      p[0] = rgb[c][i+1][j];
	      p[1] = rgb[c][i][j];
	      p[2] = rgb[c][i][j-1];
	      if (aggressive > 0) {
		      p[3] = rgb[c][i+1][j-1];
		      p[4] = rgb[c][i+2][j];
	    	  rgb2[i][j] = (int) DataSet.getKthSmallestValue (p, 5, 5/2);
	      } else {
	    	  rgb2[i][j] = (int) DataSet.getKthSmallestValue (p, 3, 3/2);
	      }
		  i = m - 1;
		  j = 0;
	      p[0] = rgb[c][i-1][j];
	      p[1] = rgb[c][i][j];
	      p[2] = rgb[c][i][j+1];
	      if (aggressive > 0) {
		      p[3] = rgb[c][i-1][j+1];
		      p[4] = rgb[c][i-2][j];
	    	  rgb2[i][j] = (int) DataSet.getKthSmallestValue (p, 5, 5/2);
	      } else {
	    	  rgb2[i][j] = (int) DataSet.getKthSmallestValue (p, 3, 3/2);
	      }

		  i = m - 1;
		  j = n - 1;
	      p[0] = rgb[c][i-1][j];
	      p[1] = rgb[c][i][j];
	      p[2] = rgb[c][i][j-1];
	      if (aggressive > 0) {
		      p[3] = rgb[c][i-1][j-1];
		      p[4] = rgb[c][i-2][j];
	    	  rgb2[i][j] = (int) DataSet.getKthSmallestValue (p, 5, 5/2);
	      } else {
	    	  rgb2[i][j] = (int) DataSet.getKthSmallestValue (p, 3, 3/2);
	      }

	      rgb[c] = rgb2;
	  }

	  this.setColor(rgb[0], rgb[1], rgb[2], rgb[3]);
	}
	private class MyAction implements java.awt.event.ActionListener {
		private Picture image;
		/**
		 * Constructor for an image.
		 * @param pio The image.
		 */
		public MyAction (Picture pio)
		{
			image = pio;
		}
		public void actionPerformed(java.awt.event.ActionEvent event) {
			Object obj = event.getSource();
			if (obj == saveAs)
			{
	        	try {
		        	String name = FileIO.fileChooser(false);
		        	if (name != null) image.write(name);
	        	} catch (JPARSECException e)
	        	{
					JOptionPane.showMessageDialog(null,
							Translate.translate(
									"Could not save the chart. Please enter the full\n" +
									"file name including one of these extensions:\n" +
									"jpg, png, bmp, gif."), Translate.translate(230), JOptionPane.ERROR_MESSAGE);
	        	}
			}
			if (obj == print)
			{
	        	try {
		        	image.print();
	        	} catch (JPARSECException e)
	        	{
					JOptionPane.showMessageDialog(null,
							Translate.translate(243), Translate.translate(230), JOptionPane.ERROR_MESSAGE);
	        	}
			}
			if (obj == close) frame.dispose();
		}
	}

}


class DisplayCanvas extends JPanel {
	private static final long serialVersionUID = 1L;

	   int x, y, oldx, oldy;
	   int pressedX, pressedY;
	   boolean drag;
	   Picture p;

	   DisplayCanvas(Picture image, int w, int h, boolean d) throws JPARSECException {
		   drag = d;
	     setBackground(Color.white);
	     setSize(w, h);

	     MediaTracker mt = new MediaTracker(this);
	     mt.addImage(image.getImage(), 1);
	     try {
	       mt.waitForAll();
	     } catch (Exception e) {
	    	 throw new JPARSECException("cannot red the image.", e);
	     }

	     pressedX = pressedY = oldx = oldy = x = y = 0;
	     p = image;

	     if (d) {
	    	 addMouseMotionListener(new MouseMotionHandler());
	     } else {
	    	add(p.getAsJLabel()); 
	     }
	   }

	   public void paintComponent(Graphics g) {
	     super.paintComponent(g);
	     if (!drag) return;
	     Graphics2D g2D = (Graphics2D) g;
	     g2D.drawImage(p.getImage(), x, y, this);
	   }

	   class MouseMotionHandler extends MouseMotionAdapter {
	     public void mouseDragged(MouseEvent e) {
	    	 if (drag) {
		       x = oldx + e.getX() - pressedX;
		       y = oldy + e.getY() - pressedY;
	    	 }
	    	 repaint();
	     }
	     public void mouseMoved(MouseEvent e) {
	    	 if (drag) {
		       pressedX = e.getX();
		       pressedY = e.getY();
		       oldx = x;
		       oldy = y;
	    	 }
		 }
	   }
}
