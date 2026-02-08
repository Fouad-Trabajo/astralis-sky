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
import java.awt.image.BufferedImage;

import jparsec.graph.DataSet;
import jparsec.math.FastMath;
import jparsec.util.JPARSECException;

/**
 * Advanced image/2d array resize method based on the image resize plugin at
 * http://bigwww.epfl.ch/algorithms/ijplugins/resize/, implementing the method
 * by A. Mu&ntilde;oz Barrutia, T. Blu, M. Unser, "Least-Squares Image Resizing Using
 * Finite Differences," IEEE Transactions on Image Processing, vol. 10, no. 9,
 * pp. 1365-1378, September 2001.<P>
 * This method should only be used (or it is only safe to be used) to reduce the
 * size of an image.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Resize
{
	// private constructor so that this class cannot be instantiated.
	private Resize() {}

	private int analyDegree;
	private int syntheDegree;
	private int analyEven;
	private int corrDegree;
	private double halfSupport;
	private double[] splineArrayHeight;
	private double[] splineArrayWidth;
	private int[] indexMinHeight;
	private int[] indexMaxHeight;
	private int[] indexMinWidth;
	private int[] indexMaxWidth;

	private static final double sqrt8minus3 = (Math.sqrt(8.0) - 3.0);
	private static final double sqrt3minus2 = (Math.sqrt(3.0) - 2.0);
	private static final double cte1 = (Math.sqrt(664.0 - Math.sqrt(438976.0)) + Math.sqrt(304.0) - 19.0);
	private static final double cte2 = (Math.sqrt(664.0 + Math.sqrt(438976.0)) - Math.sqrt(304.0) - 19.0);
	private static final double cte3 = (Math.sqrt(67.5 - Math.sqrt(4436.25)) + Math.sqrt(26.25) - 6.5);
	private static final double cte4 = (Math.sqrt(67.5 + Math.sqrt(4436.25)) - Math.sqrt(26.25) - 6.5);

	/**
	 * This flag is by default set to false, which means that resampling
	 * an image to greater sizes will throw an error. You can set this
	 * to true to allow it, generally with great results, but in some
	 * cases the oversampling using this method can produce artifacts in the
	 * output image. The oversampling seems outside the aims of this method,
	 * since in the original code the default behavior was to throw the error.
	 */
	public static boolean ALLOW_RESIZE_TO_GREATER_SIZES = false;

  /**
   * Resizes an image.
   * @param image The input image.
   * @param width The new width. Set to 0 to calculate automatically.
   * @param height The new height. Set to 0 to calculate automatically.
   * Width and height cannot be both 0.
   * @param sameRatio True to maintain original image ratio.
   * @return The output image.
   * @throws JPARSECException If an error occurs.
   */
  public static BufferedImage resize(BufferedImage image, int width, int height, boolean sameRatio) throws JPARSECException {
		if (width < 1 && height < 1) return image;

		int origWidth = image.getWidth();
		int origHeight = image.getHeight();

		if (width < 1 && height > 1)
		{
			double scale = (double) height / (double) origHeight;
			width = (int) (scale * origWidth + 0.5);
		} else {
			if (width > 1 && height < 1)
			{
				double scale = (double) width / (double) origWidth;
				height = (int) (scale * origHeight + 0.5);
			} else {
				if (width > 1 && height > 1 && sameRatio) {
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
				origWidth == width && origHeight == height) return image;

		int analyDegree = 3, syntheDegree = 3, interpDegree = 3;
		double shiftY = 0.0, shiftX = 0.0;
		double zoomX = (double) width / (double) image.getWidth();
		double zoomY = (double) height / (double) image.getHeight();
		Resize r = new Resize();
		return r.computeZoom(image, false, analyDegree, syntheDegree, interpDegree, zoomY, zoomX, shiftY, shiftX);
  }

  /**
   * Resizes a 2d array data [x][y].
   * @param image The input image.
   * @param width The new width. Set to 0 to calculate automatically.
   * @param height The new height. Set to 0 to calculate automatically.
   * Width and height cannot be both 0.
   * @param sameRatio True to maintain original image ratio.
   * @return The output data.
   * @throws JPARSECException If an error occurs.
   */
  public static double[][] resize(double[][] image, int width, int height, boolean sameRatio) throws JPARSECException {
	  if (width < 1 && height < 1) return image;

		int origWidth = image.length;
		int origHeight = image[0].length;

		if (width < 1 && height > 1)
		{
			double scale = (double) height / (double) origHeight;
			width = (int) (scale * origWidth + 0.5);
		} else {
			if (width > 1 && height < 1)
			{
				double scale = (double) width / (double) origWidth;
				height = (int) (scale * origHeight + 0.5);
			} else {
				if (width > 1 && height > 1 && sameRatio) {
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
				origWidth == width && origHeight == height) return image;

		int analyDegree = 3, syntheDegree = 3, interpDegree = 3;
		double shiftY = 0.0, shiftX = 0.0;
		double zoomX = (double) width / (double) image.length;
		double zoomY = (double) height / (double) image[0].length;
		Resize r = new Resize();
		return r.computeZoom(image, false, analyDegree, syntheDegree, interpDegree, zoomY, zoomX, shiftY, shiftX);
  }

  /**
   * Resizes a 2d array data [x][y], using internally a value of -1 for analyDegree to eliminate banding in noisy
   * images, like astronomical ones.
   * @param image The input image.
   * @param width The new width. Set to 0 to calculate automatically.
   * @param height The new height. Set to 0 to calculate automatically.
   * Width and height cannot be both 0.
   * @param sameRatio True to maintain original image ratio.
   * @return The output data.
   * @throws JPARSECException If an error occurs.
   */
  public static double[][] resizeNoBanding(double[][] image, int width, int height, boolean sameRatio) throws JPARSECException {
	  if (width < 1 && height < 1) return image;

		int origWidth = image.length;
		int origHeight = image[0].length;

		if (width < 1 && height > 1)
		{
			double scale = (double) height / (double) origHeight;
			width = (int) (scale * origWidth + 0.5);
		} else {
			if (width > 1 && height < 1)
			{
				double scale = (double) width / (double) origWidth;
				height = (int) (scale * origHeight + 0.5);
			} else {
				if (width > 1 && height > 1 && sameRatio) {
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
				origWidth == width && origHeight == height) return image;

		int analyDegree = -1, syntheDegree = 3, interpDegree = 3;
		double shiftY = 0.0, shiftX = 0.0;
		double zoomX = (double) width / (double) image.length;
		double zoomY = (double) height / (double) image[0].length;
		Resize r = new Resize();
		return r.computeZoom(image, false, analyDegree, syntheDegree, interpDegree, zoomY, zoomX, shiftY, shiftX);
  }
  
  private BufferedImage computeZoom(BufferedImage input, boolean inversable, int analyDegree, int syntheDegree, int interpDegree, double zoomY, double zoomX, double shiftY,
		  double shiftX) throws JPARSECException
  {
	this.analyEven = 0;
    this.analyDegree = analyDegree;
    this.syntheDegree = syntheDegree;

    int nx = input.getWidth();
    int ny = input.getHeight();

    int[] size = new int[4];

    int totalDegree = interpDegree + analyDegree + 1;

    size = calculatefinalsize(inversable, ny, nx, zoomY, zoomX);

    int workingSizeX = size[1];
    int workingSizeY = size[0];
    int finalSizeX = size[3];
    int finalSizeY = size[2];

    if ((analyDegree + 1) / 2 * 2 == analyDegree + 1) {
      this.analyEven = 1;
    }
    this.corrDegree = (analyDegree + syntheDegree + 1);
    this.halfSupport = ((totalDegree + 1.0) / 2.0);

    int addBorderHeight = border(finalSizeY, this.corrDegree);
    if (addBorderHeight < totalDegree) {
      addBorderHeight += totalDegree;
    }

    int finalTotalHeight = finalSizeY + addBorderHeight;
    int lengthTotalHeight = workingSizeY + (int)Math.ceil(addBorderHeight / zoomY);

    this.indexMinHeight = new int[finalTotalHeight];
    this.indexMaxHeight = new int[finalTotalHeight];

    int lengthArraySplnHeight = finalTotalHeight * (2 + totalDegree);
    int i = 0;

    double factHeight = FastMath.pow(zoomY, analyDegree + 1);

    shiftY += ((analyDegree + 1.0) / 2.0 - Math.floor((analyDegree + 1.0) / 2.0)) * (1.0 / zoomY - 1.0);
    this.splineArrayHeight = new double[lengthArraySplnHeight];

    for (int l = 0; l < finalTotalHeight; ++l) {
      double affineIndex = l / zoomY + shiftY;
      this.indexMinHeight[l] = (int)Math.ceil(affineIndex - this.halfSupport);
      this.indexMaxHeight[l] = (int)Math.floor(affineIndex + this.halfSupport);
      for (int k = this.indexMinHeight[l]; k <= this.indexMaxHeight[l]; ++k) {
        this.splineArrayHeight[i] = (factHeight * beta(affineIndex - k, totalDegree));
        ++i;
      }
    }

    int addBorderWidth = border(finalSizeX, this.corrDegree);
    if (addBorderWidth < totalDegree) {
      addBorderWidth += totalDegree;
    }

    int finalTotalWidth = finalSizeX + addBorderWidth;
    int lengthTotalWidth = workingSizeX + (int)Math.ceil(addBorderWidth / zoomX);

    this.indexMinWidth = new int[finalTotalWidth];
    this.indexMaxWidth = new int[finalTotalWidth];

    int lengthArraySplnWidth = finalTotalWidth * (2 + totalDegree);
    i = 0;
    double factWidth = FastMath.pow(zoomX, analyDegree + 1);

    shiftX += ((analyDegree + 1.0) / 2.0 - Math.floor((analyDegree + 1.0) / 2.0)) * (1.0 / zoomX - 1.0);
    this.splineArrayWidth = new double[lengthArraySplnWidth];

    for (int l = 0; l < finalTotalWidth; ++l) {
      double affineIndex = l / zoomX + shiftX;
      this.indexMinWidth[l] = (int)Math.ceil(affineIndex - this.halfSupport);
      this.indexMaxWidth[l] = (int)Math.floor(affineIndex + this.halfSupport);
      for (int k = this.indexMinWidth[l]; k <= this.indexMaxWidth[l]; ++k) {
        this.splineArrayWidth[i] = (factWidth * beta(affineIndex - k, totalDegree));
        ++i;
      }
    }
    double[] outputColumn = new double[finalSizeY];
    double[] outputRow = new double[finalSizeX];
    double[] workingRow = new double[workingSizeX];
    double[] workingColumn = new double[workingSizeY];

    double[] addVectorHeight = new double[lengthTotalHeight];
    double[] addOutputVectorHeight = new double[finalTotalHeight];
    double[] addVectorWidth = new double[lengthTotalWidth];
    double[] addOutputVectorWidth = new double[finalTotalWidth];

    int periodColumnSym = 2 * workingSizeY - 2;
    int periodRowSym = 2 * workingSizeX - 2;
    int periodColumnAsym = 2 * workingSizeY - 3;
    int periodRowAsym = 2 * workingSizeX - 3;

    BufferedImage output = null, image = null;
    if (ALLOW_RESIZE_TO_GREATER_SIZES) {
	    output = new BufferedImage(finalSizeX, Math.max(finalSizeY, workingSizeY), BufferedImage.TYPE_INT_ARGB);
	    image = new BufferedImage(finalSizeX, Math.max(finalSizeY, workingSizeY), BufferedImage.TYPE_INT_ARGB);
    } else {
	    output = new BufferedImage(finalSizeX, workingSizeY, BufferedImage.TYPE_INT_ARGB);
	    image = new BufferedImage(finalSizeX, workingSizeY, BufferedImage.TYPE_INT_ARGB);
    }

    for (int c=0; c< 4; c++) {
	    if (inversable)
	    {
	    	BufferedImage inverImage = new BufferedImage(workingSizeX, workingSizeY, BufferedImage.TYPE_INT_ARGB);

	      for (int x = 0; x < nx; ++x) {
	        for (int y = 0; y < ny; ++y) {
	          inverImage.setRGB(x, y, input.getRGB(x, y));
	        }
	      }

	      if (workingSizeX > nx) {
	        getColumn(inverImage, nx - 1, workingColumn, c);
	        for (int y = nx; y < workingSizeX; ++y) {
	          putColumn(inverImage, y, workingColumn, c);
	        }
	      }

	      if (workingSizeY > ny) {
	        getRow(inverImage, ny - 1, workingRow, c);
	        for (int y = ny; y < workingSizeY; ++y) {
	          putRow(inverImage, y, workingRow, c);
	        }

	      }

	      for (int y = 0; y < workingSizeY; ++y) {
	        getRow(inverImage, y, workingRow, c);
	        getInterpolationCoefficients(workingRow, interpDegree);
	        resamplingRow(workingRow, outputRow, addVectorWidth, addOutputVectorWidth, periodRowSym, periodRowAsym);

	        putRow(image, y, outputRow, c);
	      }

	      for (int y = 0; y < finalSizeX; ++y) {
	        getColumn(image, y, workingColumn, c);
	        getInterpolationCoefficients(workingColumn, interpDegree);
	        resamplingColumn(workingColumn, outputColumn, addVectorHeight, addOutputVectorHeight, periodColumnSym, periodColumnAsym);

	        putColumn2(output, y, outputColumn, c);
	      }

	    } else {

		    for (int y = 0; y < workingSizeY; ++y) {
		    	getRow(input, y, workingRow, c);
		        getInterpolationCoefficients(workingRow, interpDegree);
		        resamplingRow(workingRow, outputRow, addVectorWidth, addOutputVectorWidth, periodRowSym, periodRowAsym);

		        putRow(image, y, outputRow, c);
		    }

		    for (int y = 0; y < finalSizeX; ++y) {
		    	getColumn(image, y, workingColumn, c);
		        getInterpolationCoefficients(workingColumn, interpDegree);
		        resamplingColumn(workingColumn, outputColumn, addVectorHeight, addOutputVectorHeight, periodColumnSym, periodColumnAsym);

		        putColumn2(output, y, outputColumn, c);
		    }
	    }
    }
    return output.getSubimage(0, 0, finalSizeX, finalSizeY);
  }

  private double[][] computeZoom(double[][] input, boolean inversable, int analyDegree, int syntheDegree, int interpDegree, double zoomY, double zoomX, double shiftY,
		  double shiftX) throws JPARSECException
  {
	this.analyEven = 0;
    this.analyDegree = analyDegree;
    this.syntheDegree = syntheDegree;

    int nx = input.length;
    int ny = input[0].length;

    int[] size = new int[4];

    int totalDegree = interpDegree + analyDegree + 1;

    size = calculatefinalsize(inversable, ny, nx, zoomY, zoomX);

    int workingSizeX = size[1];
    int workingSizeY = size[0];
    int finalSizeX = size[3];
    int finalSizeY = size[2];

    if ((analyDegree + 1) / 2 * 2 == analyDegree + 1) {
      this.analyEven = 1;
    }
    this.corrDegree = (analyDegree + syntheDegree + 1);
    this.halfSupport = ((totalDegree + 1.0) / 2.0);

    int addBorderHeight = border(finalSizeY, this.corrDegree);
    if (addBorderHeight < totalDegree) {
      addBorderHeight += totalDegree;
    }

    int finalTotalHeight = finalSizeY + addBorderHeight;
    int lengthTotalHeight = workingSizeY + (int)Math.ceil(addBorderHeight / zoomY);

    this.indexMinHeight = new int[finalTotalHeight];
    this.indexMaxHeight = new int[finalTotalHeight];

    int lengthArraySplnHeight = finalTotalHeight * (2 + totalDegree);
    int i = 0;

    double factHeight = FastMath.pow(zoomY, analyDegree + 1);

    shiftY += ((analyDegree + 1.0) / 2.0 - Math.floor((analyDegree + 1.0) / 2.0)) * (1.0 / zoomY - 1.0);
    this.splineArrayHeight = new double[lengthArraySplnHeight];

    for (int l = 0; l < finalTotalHeight; ++l) {
      double affineIndex = l / zoomY + shiftY;
      this.indexMinHeight[l] = (int)Math.ceil(affineIndex - this.halfSupport);
      this.indexMaxHeight[l] = (int)Math.floor(affineIndex + this.halfSupport);
      for (int k = this.indexMinHeight[l]; k <= this.indexMaxHeight[l]; ++k) {
        this.splineArrayHeight[i] = (factHeight * beta(affineIndex - k, totalDegree));
        ++i;
      }
    }

    int addBorderWidth = border(finalSizeX, this.corrDegree);
    if (addBorderWidth < totalDegree) {
      addBorderWidth += totalDegree;
    }

    int finalTotalWidth = finalSizeX + addBorderWidth;
    int lengthTotalWidth = workingSizeX + (int)Math.ceil(addBorderWidth / zoomX);

    this.indexMinWidth = new int[finalTotalWidth];
    this.indexMaxWidth = new int[finalTotalWidth];

    int lengthArraySplnWidth = finalTotalWidth * (2 + totalDegree);
    i = 0;
    double factWidth = FastMath.pow(zoomX, analyDegree + 1);

    shiftX += ((analyDegree + 1.0) / 2.0 - Math.floor((analyDegree + 1.0) / 2.0)) * (1.0 / zoomX - 1.0);
    this.splineArrayWidth = new double[lengthArraySplnWidth];

    for (int l = 0; l < finalTotalWidth; ++l) {
      double affineIndex = l / zoomX + shiftX;
      this.indexMinWidth[l] = (int)Math.ceil(affineIndex - this.halfSupport);
      this.indexMaxWidth[l] = (int)Math.floor(affineIndex + this.halfSupport);
      for (int k = this.indexMinWidth[l]; k <= this.indexMaxWidth[l]; ++k) {
        this.splineArrayWidth[i] = (factWidth * beta(affineIndex - k, totalDegree));
        ++i;
      }
    }
    double[] outputColumn = new double[finalSizeY];
    double[] outputRow = new double[finalSizeX];
    double[] workingRow = new double[workingSizeX];
    double[] workingColumn = new double[workingSizeY];

    double[] addVectorHeight = new double[lengthTotalHeight];
    double[] addOutputVectorHeight = new double[finalTotalHeight];
    double[] addVectorWidth = new double[lengthTotalWidth];
    double[] addOutputVectorWidth = new double[finalTotalWidth];

    int periodColumnSym = 2 * workingSizeY - 2;
    int periodRowSym = 2 * workingSizeX - 2;
    int periodColumnAsym = 2 * workingSizeY - 3;
    int periodRowAsym = 2 * workingSizeX - 3;

    double[][] output = new double[finalSizeX][finalSizeY];
    double[][] image = new double[finalSizeX][workingSizeY];

    if (inversable)
    {
      double[][] inverImage = new double[workingSizeX][workingSizeY];

      for (int x = 0; x < nx; ++x) {
        for (int y = 0; y < ny; ++y) {
          inverImage[x][y] = input[x][y];
        }
      }

      if (workingSizeX > nx) {
        getColumn(inverImage, nx - 1, workingColumn);
        for (int y = nx; y < workingSizeX; ++y) {
          putColumn(inverImage, y, workingColumn);
        }
      }

      if (workingSizeY > ny) {
        getRow(inverImage, ny - 1, workingRow);
        for (int y = ny; y < workingSizeY; ++y) {
          putRow(inverImage, y, workingRow);
        }

      }

      for (int y = 0; y < workingSizeY; ++y) {
        getRow(inverImage, y, workingRow);
        getInterpolationCoefficients(workingRow, interpDegree);
        resamplingRow(workingRow, outputRow, addVectorWidth, addOutputVectorWidth, periodRowSym, periodRowAsym);

        putRow(image, y, outputRow);
      }

      for (int y = 0; y < finalSizeX; ++y) {
        getColumn(image, y, workingColumn);
        getInterpolationCoefficients(workingColumn, interpDegree);
        resamplingColumn(workingColumn, outputColumn, addVectorHeight, addOutputVectorHeight, periodColumnSym, periodColumnAsym);

        putColumn(output, y, outputColumn);
      }

    } else {

	    for (int y = 0; y < workingSizeY; ++y) {
	        getRow(input, y, workingRow);
	        getInterpolationCoefficients(workingRow, interpDegree);
	        resamplingRow(workingRow, outputRow, addVectorWidth, addOutputVectorWidth, periodRowSym, periodRowAsym);

	        putRow(image, y, outputRow);
	    }

	    for (int y = 0; y < finalSizeX; ++y) {
	        getColumn(image, y, workingColumn);
	        getInterpolationCoefficients(workingColumn, interpDegree);
	        resamplingColumn(workingColumn, outputColumn, addVectorHeight, addOutputVectorHeight, periodColumnSym, periodColumnAsym);

	        putColumn(output, y, outputColumn);
	    }
    }

    double out[][] = new double[finalSizeX][finalSizeY];
    for (i=0; i<finalSizeX; i++) {
    	out[i] = DataSet.getSubArray(output[i], 0, finalSizeY-1);
    }
    return out;
  }

  private void getRow(BufferedImage img, int row, double data[], int c) {
	for (int i=0; i< data.length; i++) {
		data[i] = 0;

		int color = img.getRGB(i, row), alpha = ((color>>24)&255);
		if (alpha > 0) {
			if (c == 0) data[i] = (color>>16)&255; // red
			if (c == 1) data[i] = (color>>8)&255; // green
			if (c == 2) data[i] = (color)&255; // blue
			if (c == 3) data[i] = alpha;
		}
	}
  }
  private void getColumn(BufferedImage img, int col, double data[], int c) {
	  for (int i=0; i< data.length; i++) {
			data[i] = 0;
			int color = img.getRGB(col, i), alpha = ((color>>24)&255);
			if (alpha > 0) {
				if (c == 0) data[i] = (color>>16)&255; // red
				if (c == 1) data[i] = (color>>8)&255; // green
				if (c == 2) data[i] = (color)&255; // blue
				if (c == 3) data[i] = alpha;
			}
	  }
  }
  private void putRow(BufferedImage img, int row, double data[], int c) {
	  for (int i=0; i< data.length; i++) {
		  if (data[i] > 255) data[i] = 255;
		  if (data[i] < 0) data[i] = 0;
		  if (c == 0) img.setRGB(i, row, new Color((int) data[i], 0, 0).getRGB());
		  if (c == 1) img.setRGB(i, row, new Color(0, (int) data[i], 0).getRGB());
		  if (c == 2) img.setRGB(i, row, new Color(0, 0, (int) data[i]).getRGB());
		  if (c == 3) img.setRGB(i, row, new Color(0, 0, 0, (int) data[i]).getRGB());
	  }
  }
  private void putColumn(BufferedImage img, int col, double data[], int c) {
	  for (int i=0; i< data.length; i++) {
		  if (data[i] > 255) data[i] = 255;
		  if (data[i] < 0) data[i] = 0;
		  if (c == 0) img.setRGB(col, i, new Color((int) data[i], 0, 0).getRGB());
		  if (c == 1) img.setRGB(col, i, new Color(0, (int) data[i], 0).getRGB());
		  if (c == 2) img.setRGB(col, i, new Color(0, 0, (int) data[i]).getRGB());
		  if (c == 3) img.setRGB(col, i, new Color(0, 0, 0, (int) data[i]).getRGB());
	  }
  }

  private void putColumn2(BufferedImage img, int col, double data[], int c) {
	  for (int i=0; i< data.length; i++) {
		  if (data[i] > 255) data[i] = 255;
		  if (data[i] < 0) data[i] = 0;
		  if (c == 0) img.setRGB(col, i, new Color((int) data[i], 0, 0).getRGB());
		  if (c == 1) {
			  Color co = new Color(img.getRGB(col, i));
			  img.setRGB(col, i, new Color(co.getRed(), (int) data[i], 0).getRGB());
		  }
		  if (c == 2) {
			  Color co = new Color(img.getRGB(col, i));
			  img.setRGB(col, i, new Color(co.getRed(), co.getGreen(), (int) data[i]).getRGB());
		  }
		  if (c == 3) {
			  Color co = new Color(img.getRGB(col, i));
			  img.setRGB(col, i, new Color(co.getRed(), co.getGreen(), co.getBlue(), (int) data[i]).getRGB());
		  }
	  }
  }

  private void getRow(double[][] img, int row, double data[]) {
		for (int i=0; i< data.length; i++) {
			data[i] = img[i][row];
		}
	  }
  private void getColumn(double[][] img, int col, double data[]) {
		for (int i=0; i< data.length; i++) {
			data[i] = img[col][i];
		}
	  }
  private void putRow(double[][] img, int row, double data[]) {
		for (int i=0; i< img.length; i++) {
			img[i][row] = data[i];
		}
	  }
  private void putColumn(double[][] img, int col, double data[]) {
		for (int i=0; i< img[col].length; i++) {
			img[col][i] = data[i];
		}
	  }

  private void resamplingRow(double[] inputVector, double[] outputVector, double[] addVector, double[] addOutputVector, int maxSymBoundary, int maxAsymBoundary) throws JPARSECException
  {
    int lengthInput = inputVector.length;
    int lengthOutput = outputVector.length;
    int lengthtotal = addVector.length;
    int lengthOutputtotal = addOutputVector.length;

    double average = 0.0;

    if (this.analyDegree != -1) {
      average = doInteg(inputVector, this.analyDegree + 1);
    }

    System.arraycopy(inputVector, 0, addVector, 0, lengthInput);

    for (int l = lengthInput; l < lengthtotal; ++l) {
      if (this.analyEven == 1) {
        int l2 = l;
        if (l >= maxSymBoundary)
          l2 = (int)Math.abs(Math.IEEEremainder(l, maxSymBoundary));
        if (l2 >= lengthInput)
          l2 = maxSymBoundary - l2;
        addVector[l] = inputVector[l2];
      }
      else {
        int l2 = l;
        if (l >= maxAsymBoundary)
          l2 = (int)Math.abs(Math.IEEEremainder(l, maxAsymBoundary));
        if (l2 >= lengthInput)
          l2 = maxAsymBoundary - l2;
        addVector[l] = (-inputVector[l2]);
      }
    }

    int i = 0;

    for (int l = 0; l < lengthOutputtotal; ++l) {
      addOutputVector[l] = 0.0;
      for (int k = this.indexMinWidth[l]; k <= this.indexMaxWidth[l]; ++k) {
        int index = k;
        double sign = 1.0;
        if (k < 0) {
          index = -k;
          if (this.analyEven == 0) {
            --index;
            sign = -1.0;
          }
        }
        if (k >= lengthtotal) {
          index = lengthtotal - 1;
        }
        addOutputVector[l] += sign * addVector[index] * this.splineArrayWidth[i];
        ++i;
      }

    }

    if (this.analyDegree != -1)
    {
      doDiff(addOutputVector, this.analyDegree + 1);
      for (i = 0; i < lengthOutputtotal; ++i) {
        addOutputVector[i] += average;
      }
      getInterpolationCoefficients(addOutputVector, this.corrDegree);

      getSamples(addOutputVector, this.syntheDegree);
    }

    System.arraycopy(addOutputVector, 0, outputVector, 0, lengthOutput);
  }

  private void resamplingColumn(double[] inputVector, double[] outputVector, double[] addVector, double[] addOutputVector, int maxSymBoundary, int maxAsymBoundary) throws JPARSECException
  {
    int lengthInput = inputVector.length;
    int lengthOutput = outputVector.length;
    int lengthtotal = addVector.length;
    int lengthOutputtotal = addOutputVector.length;

    double average = 0.0;

    if (this.analyDegree != -1) {
      average = doInteg(inputVector, this.analyDegree + 1);
    }

    System.arraycopy(inputVector, 0, addVector, 0, lengthInput);

    for (int l = lengthInput; l < lengthtotal; ++l) {
      if (this.analyEven == 1) {
        int l2 = l;
        if (l >= maxSymBoundary)
          l2 = (int)Math.abs(Math.IEEEremainder(l, maxSymBoundary));
        if (l2 >= lengthInput)
          l2 = maxSymBoundary - l2;
        addVector[l] = inputVector[l2];
      }
      else {
        int l2 = l;
        if (l >= maxAsymBoundary)
          l2 = (int)Math.abs(Math.IEEEremainder(l, maxAsymBoundary));
        if (l2 >= lengthInput)
          l2 = maxAsymBoundary - l2;
        addVector[l] = (-inputVector[l2]);
      }
    }

    int i = 0;

    for (int l = 0; l < lengthOutputtotal; ++l) {
      addOutputVector[l] = 0.0;
      for (int k = this.indexMinHeight[l]; k <= this.indexMaxHeight[l]; ++k) {
        int index = k;
        double sign = 1.0;
        if (k < 0) {
          index = -k;
          if (this.analyEven == 0) {
            --index;
            sign = -1.0;
          }
        }
        if (k >= lengthtotal) {
          index = lengthtotal - 1;
        }
        addOutputVector[l] += sign * addVector[index] * this.splineArrayHeight[i];
        ++i;
      }
    }

    if (this.analyDegree != -1)
    {
      doDiff(addOutputVector, this.analyDegree + 1);
      for (i = 0; i < lengthOutputtotal; ++i) {
        addOutputVector[i] += average;
      }
      getInterpolationCoefficients(addOutputVector, this.corrDegree);

      getSamples(addOutputVector, this.syntheDegree);
    }

    System.arraycopy(addOutputVector, 0, outputVector, 0, lengthOutput);
  }

  private double beta(double x, int degree)
  {
    double betan = 0.0;
    double a;
    switch (degree)
    {
    case 0:
      if (Math.abs(x) < 0.5) {
        betan = 1.0;
        break;
      }

      if (x != -0.5) break;
      betan = 1.0;
      break;
    case 1:
      x = Math.abs(x);
      if (x >= 1.0) break;
      betan = 1.0 - x;
      break;
    case 2:
      x = Math.abs(x);
      if (x < 0.5) {
        betan = 0.75 - (x * x);
        break;
      }

      if (x >= 1.5) break;
      x -= 1.5;
      betan = x * x * 0.5;
      break;
    case 3:
      x = Math.abs(x);
      if (x < 1.0) {
        betan = x * x * (x - 2.0) * 0.5 + 2.0 / 3.0;
        break;
      }
      if (x >= 2.0) break;
      x -= 2.0;
      betan = -x * x * x / 6.0;
      break;
    case 4:
      x = Math.abs(x);
      if (x < 0.5) {
        x *= x;
        betan = x * (x * 0.25 - 0.625) + 0.5989583333333334;
        break;
      }
      if (x < 1.5) {
        betan = x * (x * (x * (0.8333333333333334 - (x / 6.0)) - 1.25) + 0.2083333333333333) + 0.5729166666666666;
        break;
      }

      if (x >= 2.5) break;
      x -= 2.5;
      x *= x;
      betan = x * x / 24.0;
      break;
    case 5:
      x = Math.abs(x);
      if (x < 1.0) {
        a = x * x;
        betan = a * (a * (0.25 - (x / 12.0)) - 0.5) + 0.55;
        break;
      }

      if (x < 2.0) {
        betan = x * (x * (x * (x * (x / 24.0 - 0.375) + 1.25) - 1.75) + 0.625) + 0.425;
        break;
      }

      if (x >= 3.0) break;
      a = 3.0 - x;
      x = a * a;
      betan = a * x * x / 120.0; break;
    case 6:
      x = Math.abs(x);
      if (x < 0.5) {
        x *= x;
        betan = x * (x * (0.1458333333333333 - (x * 0.02777777777777778)) - 0.4010416666666667) + 0.5110243055555556;
        break;
      }

       if (x < 1.5) {
         betan = x * (x * (x * (x * (x * (x * 0.02083333333333333 - 0.1458333333333333) + 0.328125) - 0.1215277777777778) - 0.35546875) - 0.009114583333333334) + 0.5117838541666667;
         break;
       }

       if (x < 2.5) {
         betan = x * (x * (x * (x * (x * (0.1166666666666667 - (x * 0.008333333333333333)) - 0.65625) + 1.847222222222222) - 2.5703125) + 1.319791666666667) + 0.1795572916666667;
         break;
       }

      if (x >= 3.5) break;
      x -= 3.5;
      x *= x * x;
      betan = x * x * 0.001388888888888889; break;
    case 7:
      x = Math.abs(x);
      if (x < 1.0) {
        a = x * x;
        betan = a * (a * (a * (x * 0.006944444444444444 - 0.02777777777777778) + 0.111111111111111) - 0.3333333333333333) + 0.4793650793650794;
        break;
      }

      if (x < 2.0) {
    	  betan = x * (x * (x * (x * (x * (x * (0.05 - (x / 240.0)) - 0.2333333333333333) + 0.5) - 0.388888888888889) - 0.1) - 0.07777777777777778) + 0.4904761904761905;
    	  break;
      }

      if (x < 3.0) {
    	  betan = x * (x * (x * (x * (x * (x * (x * 0.001388888888888889 - 0.02777777777777778) + 0.2333333333333333) - 1.055555555555556) + 2.722222222222222) - 3.833333333333334) + 2.411111111111111) - 0.2206349206349206;
    	  break;
      }

      if (x >= 4.0) break;
      a = 4.0 - x;
      x = a * a * a;
      betan = x * x * a * 0.0001984126984126984;
    }

    return betan;
  }

  private double doInteg(double[] c, int nb)
  {
    int size = c.length;
    double m = 0.0; double average = 0.0;

    switch (nb)
    {
    case 1:
      for (int f = 0; f < size; ++f)
        average += c[f];
      average = (2.0 * average - c[(size - 1)] - c[0]) / (2 * size - 2);
      integSA(c, average);
      break;
    case 2:
      for (int f = 0; f < size; ++f)
        average += c[f];
      average = (2.0 * average - c[(size - 1)] - c[0]) / (2 * size - 2);
      integSA(c, average);
      integAS(c, c);
      break;
    case 3:
      for (int f = 0; f < size; ++f)
        average += c[f];
      average = (2.0 * average - c[(size - 1)] - c[0]) / (2 * size - 2);
      integSA(c, average);
      integAS(c, c);
      for (int f = 0; f < size; ++f)
        m += c[f];
      m = (2.0 * m - c[(size - 1)] - c[0]) / (2 * size - 2);
      integSA(c, m);
      break;
    case 4:
      for (int f = 0; f < size; ++f)
        average += c[f];
      average = (2.0 * average - c[(size - 1)] - c[0]) / (2 * size - 2);
      integSA(c, average);
      integAS(c, c);
      for (int f = 0; f < size; ++f)
        m += c[f];
      m = (2.0 * m - c[(size - 1)] - c[0]) / (2 * size - 2);
      integSA(c, m);
      integAS(c, c);
    }

    return average;
  }

  private void integSA(double[] c, double m)
  {
    int size = c.length;
    c[0] = ((c[0] - m) * 0.5);
    for (int i = 1; i < size; ++i)
      c[i] = (c[i] - m + c[(i - 1)]);
  }

  private void integAS(double[] c, double[] y)
  {
    int size = c.length;
    double[] z = new double[size];
    System.arraycopy(c, 0, z, 0, size);
    y[0] = z[0];
    y[1] = 0.0;
    for (int i = 2; i < size; ++i)
      y[i] = (y[(i - 1)] - z[(i - 1)]);
  }

  private void doDiff(double[] c, int nb)
  {
    switch (nb)
    {
    case 1:
      diffAS(c);
      break;
    case 2:
      diffSA(c);
      diffAS(c);
      break;
    case 3:
      diffAS(c);
      diffSA(c);
      diffAS(c);
      break;
    case 4:
      diffSA(c);
      diffAS(c);
      diffSA(c);
      diffAS(c);
    }
  }

  private void diffSA(double[] c)
  {
    int size = c.length;
    double old = c[(size - 2)];
    for (int i = 0; i <= size - 2; ++i)
      c[i] -= c[(i + 1)];
    c[(size - 1)] -= old;
  }

  private void diffAS(double[] c)
  {
    int size = c.length;
    for (int i = size - 1; i > 0; --i)
      c[i] -= c[(i - 1)];
    c[0] = (2.0 * c[0]);
  }

  private int border(int size, int degree) throws JPARSECException
  {
    int horizon = size;
    double z;
    switch (degree)
    {
    case 0:
    case 1:
      return 0;
    case 2:
      z = sqrt8minus3;
      break;
    case 3:
      z = sqrt3minus2;
      break;
    case 4:
      z = cte1;
      break;
    case 5:
      z = cte3;
      break;
    case 6:
      z = -0.4882945893030448;
      break;
    case 7:
      z = -0.5352804307964382;
      break;
    default:
      throw new JPARSECException("Invalid interpDegree degree (should be [0..7])");
    }

    horizon = 2 + (int)(Math.log(1.E-09) / Math.log(Math.abs(z)));
    horizon = (horizon < size) ? horizon : size;
    return horizon;
  }

  private static int[] calculatefinalsize(boolean inversable, int height, int width, double zoomY, double zoomX)
  {
    int[] size = new int[4];

    size[0] = height;
    size[1] = width;

    if (inversable == true) {
      int w2 = (int)Math.round(Math.round((size[0] - 1) * zoomY) / zoomY);
      while (size[0] - 1 - w2 != 0) {
        size[0] += 1;
        w2 = (int)Math.round(Math.round((size[0] - 1) * zoomY) / zoomY);
      }

      int h2 = (int)Math.round(Math.round((size[1] - 1) * zoomX) / zoomX);
      while (size[1] - 1 - h2 != 0) {
        size[1] += 1;
        h2 = (int)Math.round(Math.round((size[1] - 1) * zoomX) / zoomX);
      }
      size[2] = ((int)Math.round((size[0] - 1) * zoomY) + 1);
      size[3] = ((int)Math.round((size[1] - 1) * zoomX) + 1);
    }
    else {
      size[2] = (int)Math.round(size[0] * zoomY);
      size[3] = (int)Math.round(size[1] * zoomX);
    }
    return size;
  }

  private void getInterpolationCoefficients(double[] c, int degree) throws JPARSECException
  {
    double[] z = new double[0];
    double lambda = 1.0;

    switch (degree)
    {
    case 0:
    case 1:
      return;
    case 2:
      z = new double[1];
      z[0] = sqrt8minus3;
      break;
    case 3:
      z = new double[1];
      z[0] = sqrt3minus2;
      break;
    case 4:
      z = new double[2];
      z[0] = cte1;
      z[1] = cte2;
      break;
    case 5:
      z = new double[2];
      z[0] = cte3;
      z[1] = cte4;
      break;
    case 6:
      z = new double[3];
      z[0] = -0.4882945893030448;
      z[1] = -0.08167927107623751;
      z[2] = -0.001414151808325818;
      break;
    case 7:
      z = new double[3];
      z[0] = -0.5352804307964382;
      z[1] = -0.1225546151923267;
      z[2] = -0.009148694809608277;
      break;
    default:
      throw new JPARSECException("Invalid spline degree (should be [0..7])");
    }

    if (c.length == 1) {
      return;
    }

    for (int k = 0; k < z.length; ++k) {
      lambda = lambda * (1.0 - z[k]) * (1.0 - (1.0 / z[k]));
    }

    for (int n = 0; n < c.length; ++n) {
      c[n] *= lambda;
    }

    for (int k = 0; k < z.length; ++k) {
      c[0] = getInitialCausalCoefficient(c, z[k], 1.E-09);
      for (int n = 1; n < c.length; ++n) {
        c[n] += z[k] * c[(n - 1)];
      }
      c[(c.length - 1)] = getInitialAntiCausalCoefficient(c, z[k], 1.E-09);
      for (int n = c.length - 2; 0 <= n; --n)
        c[n] = (z[k] * (c[(n + 1)] - c[n]));
    }
  }

  private void getSamples(double[] c, int degree) throws JPARSECException
  {
    double[] h = new double[0];
    double[] s = new double[c.length];

    switch (degree)
    {
    case 0:
    case 1:
      return;
    case 2:
      h = new double[2];
      h[0] = 0.75;
      h[1] = 0.125;
      break;
    case 3:
      h = new double[2];
      h[0] = 2.0 / 3.0;
      h[1] = 1.0 / 6.0;
      break;
    case 4:
      h = new double[3];
      h[0] = 0.5989583333333334;
      h[1] = 0.1979166666666667;
      h[2] = 0.002604166666666667;
      break;
    case 5:
      h = new double[3];
      h[0] = 0.55;
      h[1] = 0.2166666666666667;
      h[2] = 0.008333333333333333;
      break;
    case 6:
      h = new double[4];
      h[0] = 0.5110243055555556;
      h[1] = 0.2287977430555556;
      h[2] = 0.01566840277777778;
      h[3] = 2.170138888888889E-05;
      break;
    case 7:
      h = new double[4];
      h[0] = 0.4793650793650794;
      h[1] = 0.236309523809524;
      h[2] = 0.02380952380952381;
      h[3] = 0.0001984126984126984;
      break;
    default:
      throw new JPARSECException("Invalid spline degree (should be [0..7])");
    }

    symmetricFir(h, c, s);
    System.arraycopy(s, 0, c, 0, s.length);
  }

  private double getInitialAntiCausalCoefficient(double[] c, double z, double tolerance)
  {
    return ((z * c[(c.length - 2)] + c[(c.length - 1)]) * z / (z * z - 1.0));
  }

  private double getInitialCausalCoefficient(double[] c, double z, double tolerance)
  {
    double z1 = z; double zn = FastMath.pow(z, c.length - 1);
    double sum = c[0] + zn * c[(c.length - 1)];
    int horizon = c.length;

    if (tolerance > 0.0) {
      horizon = 2 + (int)(Math.log(tolerance) / Math.log(Math.abs(z)));
      horizon = (horizon < c.length) ? horizon : c.length;
    }
    zn *= zn;
    for (int n = 1; n < horizon - 1; ++n) {
      zn /= z;
      sum += (z1 + zn) * c[n];
      z1 *= z;
    }
    return (sum / (1.0 - FastMath.pow(z, 2 * c.length - 2)));
  }

  private void symmetricFir(double[] h, double[] c, double[] s) throws JPARSECException
  {
    if (c.length != s.length) {
      throw new JPARSECException("Incompatible size");
    }
    switch (h.length)
    {
    case 2:
      if (2 <= c.length) {
        s[0] = (h[0] * c[0] + 2.0 * h[1] * c[1]);
        for (int i = 1; i < c.length - 1; ++i) {
          s[i] = (h[0] * c[i] + h[1] * (c[(i - 1)] + c[(i + 1)]));
        }
        s[(s.length - 1)] = (h[0] * c[(c.length - 1)] + 2.0 * h[1] * c[(c.length - 2)]);
      } else {
        switch (c.length)
        {
        case 1:
          s[0] = ((h[0] + 2.0 * h[1]) * c[0]);
          break;
        default:
          throw new JPARSECException("The size of the c array must be 1 in this case");
        }
      }
      return;
    case 3:
      if (4 <= c.length) {
        s[0] = (h[0] * c[0] + 2.0 * h[1] * c[1] + 2.0 * h[2] * c[2]);
        s[1] = (h[0] * c[1] + h[1] * (c[0] + c[2]) + h[2] * (c[1] + c[3]));
        for (int i = 2; i < c.length - 2; ++i) {
          s[i] = (h[0] * c[i] + h[1] * (c[(i - 1)] + c[(i + 1)]) + h[2] * (c[(i - 2)] + c[(i + 2)]));
        }

        s[(s.length - 2)] = (h[0] * c[(c.length - 2)] + h[1] * (c[(c.length - 3)] + c[(c.length - 1)]) + h[2] * (c[(c.length - 4)] + c[(c.length - 2)]));
        s[(s.length - 1)] = (h[0] * c[(c.length - 1)] + 2.0 * h[1] * c[(c.length - 2)] + 2.0 * h[2] * c[(c.length - 3)]);
      }
      else
      {
        switch (c.length)
        {
        case 3:
          s[0] = (h[0] * c[0] + 2.0 * h[1] * c[1] + 2.0 * h[2] * c[2]);
          s[1] = (h[0] * c[1] + h[1] * (c[0] + c[2]) + 2.0 * h[2] * c[1]);
          s[2] = (h[0] * c[2] + 2.0 * h[1] * c[1] + 2.0 * h[2] * c[0]);
          break;
        case 2:
          s[0] = ((h[0] + 2.0 * h[2]) * c[0] + 2.0 * h[1] * c[1]);
          s[1] = ((h[0] + 2.0 * h[2]) * c[1] + 2.0 * h[1] * c[0]);
          break;
        case 1:
          s[0] = ((h[0] + 2.0 * (h[1] + h[2])) * c[0]);
          break;
        default:
          throw new JPARSECException("The size of the c array must be between 1 and 3");
        }
      }
      return;
    case 4:
      if (6 <= c.length) {
        s[0] = (h[0] * c[0] + 2.0 * h[1] * c[1] + 2.0 * h[2] * c[2] + 2.0 * h[3] * c[3]);
        s[1] = (h[0] * c[1] + h[1] * (c[0] + c[2]) + h[2] * (c[1] + c[3]) + h[3] * (c[2] + c[4]));
        s[2] = (h[0] * c[2] + h[1] * (c[1] + c[3]) + h[2] * (c[0] + c[4]) + h[3] * (c[1] + c[5]));

        for (int i = 3; i < c.length - 3; ++i) {
          s[i] = (h[0] * c[i] + h[1] * (c[(i - 1)] + c[(i + 1)]) + h[2] * (c[(i - 2)] + c[(i + 2)]) + h[3] * (c[(i - 3)] + c[(i + 3)]));
        }

        s[(s.length - 3)] = (h[0] * c[(c.length - 3)] + h[1] * (c[(c.length - 4)] + c[(c.length - 2)]) + h[2] * (c[(c.length - 5)] + c[(c.length - 1)]) + h[3] * (c[(c.length - 6)] + c[(c.length - 2)]));
        s[(s.length - 2)] = (h[0] * c[(c.length - 2)] + h[1] * (c[(c.length - 3)] + c[(c.length - 1)]) + h[2] * (c[(c.length - 4)] + c[(c.length - 2)]) + h[3] * (c[(c.length - 5)] + c[(c.length - 3)]));
        s[(s.length - 1)] = (h[0] * c[(c.length - 1)] + 2.0 * h[1] * c[(c.length - 2)] + 2.0 * h[2] * c[(c.length - 3)] + 2.0 * h[3] * c[(c.length - 4)]);
      }
      else
      {
        switch (c.length)
        {
        case 5:
          s[0] = (h[0] * c[0] + 2.0 * h[1] * c[1] + 2.0 * h[2] * c[2] + 2.0 * h[3] * c[3]);
          s[1] = (h[0] * c[1] + h[1] * (c[0] + c[2]) + h[2] * (c[1] + c[3]) + h[3] * (c[2] + c[4]));
          s[2] = (h[0] * c[2] + (h[1] + h[3]) * (c[1] + c[3]) + h[2] * (c[0] + c[4]));
          s[3] = (h[0] * c[3] + h[1] * (c[2] + c[4]) + h[2] * (c[1] + c[3]) + h[3] * (c[0] + c[2]));
          s[4] = (h[0] * c[4] + 2.0 * h[1] * c[3] + 2.0 * h[2] * c[2] + 2.0 * h[3] * c[1]);
          break;
        case 4:
          s[0] = (h[0] * c[0] + 2.0 * h[1] * c[1] + 2.0 * h[2] * c[2] + 2.0 * h[3] * c[3]);
          s[1] = (h[0] * c[1] + h[1] * (c[0] + c[2]) + h[2] * (c[1] + c[3]) + 2.0 * h[3] * c[2]);
          s[2] = (h[0] * c[2] + h[1] * (c[1] + c[3]) + h[2] * (c[0] + c[2]) + 2.0 * h[3] * c[1]);
          s[3] = (h[0] * c[3] + 2.0 * h[1] * c[2] + 2.0 * h[2] * c[1] + 2.0 * h[3] * c[0]);
          break;
        case 3:
          s[0] = (h[0] * c[0] + 2.0 * (h[1] + h[3]) * c[1] + 2.0 * h[2] * c[2]);
          s[1] = (h[0] * c[1] + (h[1] + h[3]) * (c[0] + c[2]) + 2.0 * h[2] * c[1]);
          s[2] = (h[0] * c[2] + 2.0 * (h[1] + h[3]) * c[1] + 2.0 * h[2] * c[0]);
          break;
        case 2:
          s[0] = ((h[0] + 2.0 * h[2]) * c[0] + 2.0 * (h[1] + h[3]) * c[1]);
          s[1] = ((h[0] + 2.0 * h[2]) * c[1] + 2.0 * (h[1] + h[3]) * c[0]);
          break;
        case 1:
          s[0] = ((h[0] + 2.0 * (h[1] + h[2] + h[3])) * c[0]);
          break;
        default:
          throw new JPARSECException("The size of the c array must be between 1 and 5");
        }
      }
      return;
    }

    throw new JPARSECException("Invalid filter half-length (should be [2..4])");
  }
}
