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

import java.io.Serializable;

import jparsec.graph.DataSet;
import jparsec.math.FastMath;
import jparsec.util.JPARSECException;
import jparsec.util.Logger;
import jparsec.util.Logger.LEVEL;

/**
 * Spline interpolation in 2D applied to rotate or resample images,
 * based on two different routines by Philippe Th&eacute;venaz and A. Mu&ntilde;oz Barrutia et al.
 * Basic implementation of Th&eacute;venaz method is taken from SkyView.
 * The implementation by Th&eacute;venaz is not as good as the other, and it
 * is used to interpolate within the array representing an image. The
 * second method by Mu&ntilde;oz Barrutia et al. is better, and it is used to
 * change the size of the image.<P>
 *
 * P. Th&eacute;venaz, T. Blu, M. Unser, "Interpolation Revisited," IEEE Transactions on Medical Imaging, vol. 19, no. 7, pp. 739-758, July 2000.<BR>
 * A. Mu&ntilde;oz Barrutia, T. Blu, M. Unser, "Least-Squares Image Resizing Using Finite Differences," IEEE Transactions on Image Processing,
 * vol. 10, no. 9, pp. 1365-1378, September 2001.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class ImageSplineTransform implements Serializable {

	private static final long serialVersionUID = 1L;

	/** The order of the spline */
	private int splineDegree;

	/** A copy of the image that is transformed into
	 *  the spline coefficients
	 */
	private double[][] image;
	private double[][] imgCoef;
	private int[][] imgInt;
	private short[][] imgShort;
	private byte[][] imgByte;

	/** Copy not transformed. Disabled to save memory. */
//	private double[][] originalImage;
	private int imgHeight, imgWidth;
	private boolean sTOc = false;
	private static final double VAL1_6 = 0.166666666667;
	private static final double sqrt8minus3 = (Math.sqrt(8.0) - 3.0);
	private static final double sqrt3minus2 = (Math.sqrt(3.0) - 2.0);
	private static final double cte1 = (Math.sqrt(664.0 - Math.sqrt(438976.0)) + Math.sqrt(304.0) - 19.0);
	private static final double cte2 = (Math.sqrt(664.0 + Math.sqrt(438976.0)) - Math.sqrt(304.0) - 19.0);
	private static final double cte3 = Math.sqrt(135.0 / 2.0 - Math.sqrt(17745.0 / 4.0)) + Math.sqrt(105.0 / 4.0) - 13.0 / 2.0;
	private static final double cte4 = Math.sqrt(135.0 / 2.0 + Math.sqrt(17745.0 / 4.0)) - Math.sqrt(105.0 / 4.0) - 13.0 / 2.0;
	private static final double v1Over24 = (1.0 / 24.0);
	private static final double v11Over24 = (11.0 / 24.0);

	/**
	 * Constructor for a given image.
	 * @param degree Interpolation degree, between 2 and 9.
	 * Usual value is 3.
	 * @param image The image as a 2d array [x][y].
	 * @throws JPARSECException If the degree is invalid.
	 */
	public ImageSplineTransform(int degree, double[][] image)
	throws JPARSECException {
		if (degree < 2 || degree > 9)
			throw new JPARSECException("invalid degree "+degree+". It should be between 2 and 9");
		splineDegree = degree;
		imgWidth = image.length;
		imgHeight = image[0].length;
		//originalImage = clone(image);
        this.image  = image; //clone(image);
	}

	/**
	 * Constructor for a given image.
	 * @param degree Interpolation degree, between 2 and 9.
	 * Usual value is 3.
	 * @param image The image as a 2d array [x][y].
	 * @throws JPARSECException If the degree is invalid.
	 */
	public ImageSplineTransform(int degree, int[][] image)
	throws JPARSECException {
		if (degree < 2 || degree > 9)
			throw new JPARSECException("invalid degree "+degree+". It should be between 2 and 9");
		splineDegree = degree;
		imgWidth = image.length;
		imgHeight = image[0].length;
		//originalImage = DataSet.toDoubleArray(image);
        imgInt = image;
	}

	/**
	 * Constructor for a given image.
	 * @param degree Interpolation degree, between 2 and 9.
	 * Usual value is 3.
	 * @param image The image as a 2d array [x][y].
	 * @throws JPARSECException If the degree is invalid.
	 */
	public ImageSplineTransform(int degree, short[][] image)
	throws JPARSECException {
		if (degree < 2 || degree > 9)
			throw new JPARSECException("invalid degree "+degree+". It should be between 2 and 9");
		splineDegree = degree;
		imgWidth = image.length;
		imgHeight = image[0].length;
		//originalImage = DataSet.toDoubleArray(image);
        imgShort = image;
	}

	/**
	 * Constructor for a given image.
	 * @param degree Interpolation degree, between 2 and 9.
	 * Usual value is 3.
	 * @param image The image as a 2d array [x][y].
	 * @throws JPARSECException If the degree is invalid.
	 */
	public ImageSplineTransform(int degree, byte[][] image)
	throws JPARSECException {
		if (degree < 2 || degree > 9)
			throw new JPARSECException("invalid degree "+degree+". It should be between 2 and 9");
		splineDegree = degree;
		imgWidth = image.length;
		imgHeight = image[0].length;
		//originalImage = DataSet.toDoubleArray(image);
        imgByte = image;
	}

	/**
	 * Constructor for a given image and degree = 3.
	 * @param image The image as a 2d array [x][y].
	 */
	public ImageSplineTransform(double[][] image)
	{
		splineDegree = 3;
		imgWidth = image.length;
		imgHeight = image[0].length;
		//originalImage = clone(image);
        this.image  = image; //clone(image);
	}

/*	private double[][] clone(double[][] img)
	{
		double[][] out = new double[img.length][img[0].length];
		for (int i=0; i<out.length; i++)
		{
			for (int j=0; j<out[0].length; j++)
			{
				out[i][j] = img[i][j];
			}
		}
		return out;
	}
*/
	/**
	 * Returns the width of the image in this instance.
	 * @return The width.
	 */
	public int getWidth() {
		return imgWidth;
	}

	/**
	 * Returns the height of the image in this instance.
	 * @return The height.
	 */
	public int getHeight() {
		return imgHeight;
	}

    /** Transform the image to spline coefficients on the image copy.
     * Note that except for
     * variables visible outside a method, these routines use
     * Thevenaz' original variable names which sometimes
     * violated Java conventions.
     */
    private  int samplesToCoefficients () {
    	sTOc = true;
    	double[]  Line;
	   	double[]  Pole = new double[4];
	   	int	  NbPoles;

	   	/* recover the poles from a lookup table */
	   	switch (splineDegree) {
		 case 2:
		    			NbPoles = 1;
		    			Pole[0] = sqrt8minus3;
		    			break;
		 case 3:
		    			NbPoles = 1;
		    			Pole[0] = sqrt3minus2;
		    			break;
		 case 4:
		    			NbPoles = 2;
		    			Pole[0] = cte1;
		    			Pole[1] = cte2;
		    			break;
		 case 5:
		    			NbPoles = 2;
		    			Pole[0] = cte3;
		    			Pole[1] = cte4;
		    			break;
		case 6:
			NbPoles = 3;
			Pole[0] = -0.48829458930304475513011803888378906211227916123938;
			Pole[1] = -0.081679271076237512597937765737059080653379610398148;
			Pole[2] = -0.0014141518083258177510872439765585925278641690553467;
			break;
		case 7:
			NbPoles = 3;
			Pole[0] = -0.53528043079643816554240378168164607183392315234269;
			Pole[1] = -0.12255461519232669051527226435935734360548654942730;
			Pole[2] = -0.0091486948096082769285930216516478534156925639545994;
			break;
		case 8:
			NbPoles = 4;
			Pole[0] = -0.57468690924876543053013930412874542429066157804125;
			Pole[1] = -0.16303526929728093524055189686073705223476814550830;
			Pole[2] = -0.023632294694844850023403919296361320612665920854629;
			Pole[3] = -0.00015382131064169091173935253018402160762964054070043;
			break;
		case 9:
			NbPoles = 4;
			Pole[0] = -0.60799738916862577900772082395428976943963471853991;
			Pole[1] = -0.20175052019315323879606468505597043468089886575747;
			Pole[2] = -0.043222608540481752133321142979429688265852380231497;
			Pole[3] = -0.0021213069031808184203048965578486234220548560988624;
			break;
		 default:
			 System.err.println("Invalid spline degree\n");
			 return(1);
		}

	   	/* convert the image samples into interpolation coefficients */
	   	/* in-place separable process, along x */
	   	Line = new double[imgHeight];

	   	double logPole[] = new double[Pole.length];
	   	for (int i=0; i<logPole.length; i++) {
	   		logPole[i] = Math.log(Math.abs(Pole[i]));
	   	}

	   	imgCoef = new double[imgWidth][imgHeight];
	   	for (int y = 0; y < imgWidth; y++) {
		     getRow(imgCoef, y, Line, imgHeight);
		     convertToInterpolationCoefficients(Line, imgHeight, Pole, NbPoles, 1.e-14, logPole);
		     putRow(imgCoef, y, Line, imgHeight);
		}

	   	/* in-place separable process, along y */
	   	Line = new double[imgWidth];

	   	for (int x = 0; x < imgHeight; x++) {
		     getColumn(imgCoef, imgHeight, x, Line, imgWidth);
		     convertToInterpolationCoefficients(Line, imgWidth, Pole, NbPoles, 1.e-14, logPole);
		     putColumn(imgCoef, imgHeight, x, Line, imgWidth);
		}

	   	return(0);
   } /* end SamplesToCoefficients */


   private void convertToInterpolationCoefficients
     (
    double	c[],		/* input samples --> output coefficients */
	int	DataLength,	/* number of samples or coefficients */
	double	z[],		/* poles */
	int	NbPoles,	/* number of poles */
	double	Tolerance,	/* admissible relative error */
	double logPole[]
     )
   { /* begin ConvertToInterpolationCoefficients */

   	double	Lambda = 1.0;

   	/* special case required by mirror boundaries */
   	if (DataLength == 1) {
	    return;
	}

   	/* compute the overall gain */
   	for (int k = 0; k < NbPoles; k++) {
	     Lambda = Lambda * (1.0 - z[k]) * (1.0 - 1.0 / z[k]);
	}

   	/* apply the gain */
   	for (int n = 0; n < DataLength; n++) {
	     c[n] *= Lambda;
	}

   	/* loop over all poles */
   	double logTol = 0.0;
   	if (Tolerance > 0.0) logTol = Math.log(Tolerance);
   	for (int k = 0; k < NbPoles; k++) {
	     /* causal initialization */
	     c[0] = initialCausalCoefficient(c, DataLength, z[k], Tolerance, logTol, logPole[k]);
	     /* causal recursion */
	     for (int n = 1; n < DataLength; n++) {
		 c[n] += z[k] * c[n - 1];
	     }
	     /* anticausal initialization */
	     c[DataLength - 1] = initialAntiCausalCoefficient(c, DataLength, z[k]);
	     /* anticausal recursion */
	     for (int n = DataLength - 2; 0 <= n; n--) {
		 c[n] = (z[k] * (c[n + 1] - c[n]));
	     }
	}
   } /* end ConvertToInterpolationCoefficients */

   /*--------------------------------------------------------------------------*/
   private  double initialCausalCoefficient
 				(
 						double	c[],		/* coefficients */
				 					int	DataLength,	/* number of coefficients */
				 					double	z,			/* actual pole */
				 					double Tolerance,
				 					double	logTol,	/* admissible relative error */
				 					double logPole
				 				)

   { /* begin InitialCausalCoefficient */

   	double	Sum, zn, z2n, iz;

   	/* this initialization corresponds to mirror boundaries */
   	int Horizon = DataLength;

   	if (Tolerance > 0.0) {
   		/*
   		double x = logTol / logPole;
   		// This is faster than Math.ceil ?
		if (x > 0) {
			Horizon = (int) (x+1);
		} else {
			Horizon = (int) x;
		}
		*/
	    Horizon = (int)Math.ceil(logTol / logPole);
	}
   	if (Horizon < DataLength) {
	    		/* accelerated loop */
	    zn = z;
	    Sum = c[0];
	    for (int n = 1; n < Horizon; n++) {
		Sum += zn * c[n];
		zn *= z;
	    }
	    return(Sum);
	}
   	else {
	    /* full loop */
	    zn = z;
	    iz = 1.0 / z;
	    z2n = FastMath.pow(z, (double)(DataLength - 1));
	    Sum = c[0] + z2n * c[DataLength - 1];
	    z2n *= z2n * iz;
	    for (int n = 1; n <= DataLength - 2; n++) {
		 Sum += (zn + z2n) * c[n];
		 zn *= z;
		 z2n *= iz;
	    }
	    return(Sum / (1.0 - zn * zn));
	}
   } /* end InitialCausalCoefficient */



   /*--------------------------------------------------------------------------*/
   private void getColumn
 				(
 						double[][] Image,		/* input image array */
				 					int	 Width,		/* width of the image */
				 					int	 x,		/* x coordinate of the selected line */
				 					double[] Line,		/* output linear array */
				 					int	 Height		/* length of the line */
				 				)

   { /* begin GetColumn */

   	for (int y = 0; y < Height; y++) {
	     Line[y] = Image[y][x];
	}
   } /* end GetColumn */

   /*--------------------------------------------------------------------------*/
   private void getRow
 				(
 						double[][]  Image,		/* input image array */
				 				        int	  y,			/* y coordinate of the selected line */
				 				       double[]  Line,	      	 	/* output linear array */
				 					int	  Width		        /* length of the line */
				 				)

   { /* begin GetRow */

	   	for (int x = 0; x < Width; x++) {
		     Line[x] = Image[y][x];
		}
   } /* end GetRow */

   /*--------------------------------------------------------------------------*/
   private double initialAntiCausalCoefficient
 				(
 						double	c[],		/* coefficients */
				 					int	DataLength,	/* number of samples or coefficients */
				 					double	z			/* actual pole */
				 				)

   { /* begin InitialAntiCausalCoefficient */

   	/* this initialization corresponds to mirror boundaries */
   	return((z / (z * z - 1.0)) * (z * c[DataLength - 2] + c[DataLength - 1]));
   } /* end InitialAntiCausalCoefficient */


   /*--------------------------------------------------------------------------*/
   private void putColumn
 				(
 						double[][] Image,		/* output image array */
				 					int	 Width,		/* width of the image */
				 					int	 x,			/* x coordinate of the selected line */
				 					double	 Line[],		/* input linear array */
				 					int	 Height		/* length of the line and height of the image */
				 				)

   { /* begin PutColumn */


   	for (int y = 0; y < Height; y++) {
 	   Image[y][x] = Line[y];
	}
   } /* end PutColumn */

    /*--------------------------------------------------------------------------*/
    private void putRow
 				(
 						double[][] Image,		/* output image array */
				 					int      y,			/* y coordinate of the selected line */
				 					double	 Line[],		/* input linear array */
				 					int	 Width		/* length of the line and width of the image */
				 				)

   { /* begin PutRow */

       	for (int x = 0; x < Width; x++) {
      	   Image[y][x] = Line[x];
    	}
   } /* end PutRow */



   private double[]	 xWeight= new double[10];
   private double[]     yWeight=new double[10];
   private int[]	 xIndex= new int[10];
   private int[]        yIndex=new int[10];

   /**
    * Returns if a given interpolation point is out of the image.
    * @param px The x position.
    * @param py The y position.
    * @return True or false.
    */
   public boolean isOutOfImage(double px, double py) {
	   if (image == null) {
		   if (imgInt != null) {
			   if (px < 0 || px > imgInt.length-1 || py < 0 || py > imgInt[0].length-1) return true;
		   } else {
			   if (imgShort != null) {
				   if (px < 0 || px > imgShort.length-1 || py < 0 || py > imgShort[0].length-1) return true;
			   } else {
				   if (px < 0 || px > imgByte.length-1 || py < 0 || py > imgByte[0].length-1) return true;
			   }
		   }
		   return false;
	   }
	   if (px < 0 || px > image.length-1 || py < 0 || py > image[0].length-1) return true;
	   return false;
   }

   /**
    * Interpolate at a given position using a fast bilinear algorithm.
    * Image must be a 2x2 image at least.
    * @param x X index position in the image, from 0 to width-1.
    * @param y Y index position in the image, from 0 to height-1.
    * @return Interpolated value.
    * @throws JPARSECException If the point is outside the image.
    */
   public double fastBilinearInterpolation(
 				 					double	x,			/* x coordinate where to interpolate */
				 					double	y			/* y coordinate where to interpolate */
				 				) throws JPARSECException {
		if (isOutOfImage(x, y)) throw new JPARSECException("point ("+x+", "+y+") out of image (0, 0)-("+imgWidth+", "+imgHeight+").");
   		int x0 = (int)x;
   		int y0 = (int)y;
		if (x == x0 && y == y0) return getImage(x0, y0);

   		int x1 = x0 + 1;
   		int y1 = y0 + 1;
   		if (y1 >= imgHeight-1) {
   			y1 = y0;
   			y0 --;
   		}
   		double dx1 = x1 - x;
	  	double p = dx1*getImage(x0, y0);
	  	double q = dx1*getImage(x0, y1);
	  	if (x != x0) {
	   		double dx0 = 1 - dx1;
		  	p += dx0*getImage(x1, y0);
	  		q += dx0*getImage(x1, y1);
	  	}

	  	return ((y1-y)*p + (y-y0)*q);

/*	  	// Improve estimate using points around. Slow
		int dxy = 1;
		double norm = 1, out = ((y1-y)*p + (y-y0)*q), dxy2 = 1 + dxy;
		for (int ix=x0-dxy;ix<=x1+dxy;ix++) {
			for (int iy=y0-dxy;iy<=y1+dxy;iy++) {
				if (ix < 0 || ix >= imgWidth-1) continue;
				if (iy < 0 || iy >= imgHeight-1) continue;
				double w = FastMath.pow(1.0 - FastMath.hypot((x-ix)/dxy2, (y-iy)/dxy2), dxy2*2);
				norm += w;
				out += w * getImage(ix, iy);
			}
		}
		return out / norm;
*/
	}

   /**
    * Interpolate at a given position. If you entered the image
    * as a column array of size [1][y] set px = 0 to interpolate.
    * @param px X index position in the image, from 0 to width-1.
    * @param py Y index position in the image, from 0 to height-1.
    * @return Interpolated value.
    * @throws JPARSECException If the point is outside the image.
    */
   public double interpolate(
 				 					double	px,			/* x coordinate where to interpolate */
				 					double	py			/* y coordinate where to interpolate */
				 				) throws JPARSECException

   { /* begin InterpolatedValue */

	   if (isOutOfImage(px, py)) throw new JPARSECException("point ("+px+", "+py+") out of image (0, 0)-("+imgWidth+", "+imgHeight+").");
	   if (!sTOc) samplesToCoefficients();

	   	int	 Width2 = 2 * imgHeight - 2, Height2 = 2 * imgWidth - 2;
	   	double	 interpolated;
	   	double	 w, w2, w4, t, t0, t1;
		int i,j;

	   	/* compute the interpolation indexes */
	   	if (splineDegree % 2  != 0) {
		    i = (int)Math.floor(py) - splineDegree / 2;
		    j = (int)Math.floor(px) - splineDegree / 2;
		} else {
		    i = (int)Math.floor(py + 0.5) - splineDegree / 2;
		    j = (int)Math.floor(px + 0.5) - splineDegree / 2;
		}
	    for (int k = 0; k <= splineDegree; k++) {
	    	xIndex[k] = i++;
	    	yIndex[k] = j++;
	    }

	   	/* compute the interpolation weights */
	   	switch (splineDegree) {
		 case 2:
		      /* x */
		      w = py - (double)xIndex[1];
		      xWeight[1] = 0.75 - w * w;
		      xWeight[2] = 0.5 * (w - xWeight[1] + 1.0);
		      xWeight[0] = 1.0 - xWeight[1] - xWeight[2];
		      /* y */
		      w = px - (double)yIndex[1];
		      yWeight[1] = 0.75 - w * w;
		      yWeight[2] = 0.5 * (w - yWeight[1] + 1.0);
		      yWeight[0] = 1.0 - yWeight[1] - yWeight[2];
		      break;
		 case 3:
		      /* x */
		      w = py - (double)xIndex[1];
		      xWeight[3] = VAL1_6 * w * w * w;
		      xWeight[0] = VAL1_6 + 0.5 * w * (w - 1.0) - xWeight[3];
		      xWeight[2] = w + xWeight[0] - 2.0 * xWeight[3];
		      xWeight[1] = 1.0 - xWeight[0] - xWeight[2] - xWeight[3];
		      /* y */
		      w = px - (double)yIndex[1];
		      yWeight[3] = VAL1_6 * w * w * w;
		      yWeight[0] = VAL1_6 + 0.5 * w * (w - 1.0) - yWeight[3];
		      yWeight[2] = w + yWeight[0] - 2.0 * yWeight[3];
		      yWeight[1] = 1.0 - yWeight[0] - yWeight[2] - yWeight[3];
		      break;
		 case 4:
		      /* x */
		      w = py - (double)xIndex[2];
		      w2 = w * w;
		      t = VAL1_6 * w2;
		      xWeight[0] = 0.5 - w;
		      xWeight[0] *= xWeight[0];
		      xWeight[0] *= v1Over24 * xWeight[0];
		      t0 = w * (t - v11Over24);
		      t1 = 19.0 / 96.0 + w2 * (0.25 - t);
		      xWeight[1] = t1 + t0;
		      xWeight[3] = t1 - t0;
		      xWeight[4] = xWeight[0] + t0 + 0.5 * w;
		      xWeight[2] = 1.0 - xWeight[0] - xWeight[1] - xWeight[3] - xWeight[4];
		      /* y */
		      w = px - (double)yIndex[2];
		      w2 = w * w;
		      t = VAL1_6 * w2;
		      yWeight[0] = 0.5 - w;
		      yWeight[0] *= yWeight[0];
		      yWeight[0] *= v1Over24 * yWeight[0];
		      t0 = w * (t - v11Over24);
		      t1 = 19.0 / 96.0 + w2 * (0.25 - t);
		      yWeight[1] = t1 + t0;
		      yWeight[3] = t1 - t0;
		      yWeight[4] = yWeight[0] + t0 + 0.5 * w;
		      yWeight[2] = 1.0 - yWeight[0] - yWeight[1] - yWeight[3] - yWeight[4];
		      break;
		 case 5:
		      /* x */
		      w = py - (double)xIndex[2];
		      w2 = w * w;
		      xWeight[5] = (1.0 / 120.0) * w * w2 * w2;
		      w2 -= w;
		      w4 = w2 * w2;
		      w -= 0.5;
		      t = w2 * (w2 - 3.0);
		      xWeight[0] = v1Over24 * (0.2 + w2 + w4) - xWeight[5];
		      t0 = v1Over24 * (w2 * (w2 - 5.0) + 46.0 / 5.0);
		      t1 = (-1.0 / 12.0) * w * (t + 4.0);
		      xWeight[2] = t0 + t1;
		      xWeight[3] = t0 - t1;
		      t0 = (1.0 / 16.0) * (9.0 / 5.0 - t);
		      t1 = v1Over24 * w * (w4 - w2 - 5.0);
		      xWeight[1] = t0 + t1;
		      xWeight[4] = t0 - t1;
		      /* y */
		      w = px - (double)yIndex[2];
		      w2 = w * w;
		      yWeight[5] = (1.0 / 120.0) * w * w2 * w2;
		      w2 -= w;
		      w4 = w2 * w2;
		      w -= 0.5;
		      t = w2 * (w2 - 3.0);
		      yWeight[0] = v1Over24 * (1.0 / 5.0 + w2 + w4) - yWeight[5];
		      t0 = v1Over24 * (w2 * (w2 - 5.0) + 46.0 / 5.0);
		      t1 = (-1.0 / 12.0) * w * (t + 4.0);
		      yWeight[2] = t0 + t1;
		      yWeight[3] = t0 - t1;
		      t0 = (1.0 / 16.0) * (9.0 / 5.0 - t);
		      t1 = v1Over24 * w * (w4 - w2 - 5.0);
		      yWeight[1] = t0 + t1;
		      yWeight[4] = t0 - t1;
		      break;
		case 6:
			/* x */
			w = py - (double)xIndex[3];
			xWeight[0] = 0.5 - w;
			xWeight[0] *= xWeight[0] * xWeight[0];
			xWeight[0] *= xWeight[0] / 720.0;
			xWeight[1] = (361.0 / 192.0 - w * (59.0 / 8.0 + w
				* (-185.0 / 16.0 + w * (25.0 / 3.0 + w * (-5.0 / 2.0 + w)
				* (0.5 + w))))) / 120.0;
			xWeight[2] = (10543.0 / 960.0 + w * (-289.0 / 16.0 + w
				* (79.0 / 16.0 + w * (43.0 / 6.0 + w * (-17.0 / 4.0 + w
				* (-1.0 + w)))))) / 48.0;
			w2 = w * w;
			xWeight[3] = (5887.0 / 320.0 - w2 * (231.0 / 16.0 - w2
				* (21.0 / 4.0 - w2))) / 36.0;
			xWeight[4] = (10543.0 / 960.0 + w * (289.0 / 16.0 + w
				* (79.0 / 16.0 + w * (-43.0 / 6.0 + w * (-17.0 / 4.0 + w
				* (1.0 + w)))))) / 48.0;
			xWeight[6] = 0.5 + w;
			xWeight[6] *= xWeight[6] * xWeight[6];
			xWeight[6] *= xWeight[6] / 720.0;
			xWeight[5] = 1.0 - xWeight[0] - xWeight[1] - xWeight[2] - xWeight[3]
				- xWeight[4] - xWeight[6];
			/* y */
			w = px - (double)yIndex[3];
			yWeight[0] = 0.5 - w;
			yWeight[0] *= yWeight[0] * yWeight[0];
			yWeight[0] *= yWeight[0] / 720.0;
			yWeight[1] = (361.0 / 192.0 - w * (59.0 / 8.0 + w
				* (-185.0 / 16.0 + w * (25.0 / 3.0 + w * (-5.0 / 2.0 + w)
				* (0.5 + w))))) / 120.0;
			yWeight[2] = (10543.0 / 960.0 + w * (-289.0 / 16.0 + w
				* (79.0 / 16.0 + w * (43.0 / 6.0 + w * (-17.0 / 4.0 + w
				* (-1.0 + w)))))) / 48.0;
			w2 = w * w;
			yWeight[3] = (5887.0 / 320.0 - w2 * (231.0 / 16.0 - w2
				* (21.0 / 4.0 - w2))) / 36.0;
			yWeight[4] = (10543.0 / 960.0 + w * (289.0 / 16.0 + w
				* (79.0 / 16.0 + w * (-43.0 / 6.0 + w * (-17.0 / 4.0 + w
				* (1.0 + w)))))) / 48.0;
			yWeight[6] = 0.5 + w;
			yWeight[6] *= yWeight[6] * yWeight[6];
			yWeight[6] *= yWeight[6] / 720.0;
			yWeight[5] = 1.0 - yWeight[0] - yWeight[1] - yWeight[2] - yWeight[3]
				- yWeight[4] - yWeight[6];
			break;
		case 7:
			/* x */
			w = py - (double)xIndex[3];
			xWeight[0] = 1.0 - w;
			xWeight[0] *= xWeight[0];
			xWeight[0] *= xWeight[0] * xWeight[0];
			xWeight[0] *= (1.0 - w) / 5040.0;
			w2 = w * w;
			xWeight[1] = (120.0 / 7.0 + w * (-56.0 + w * (72.0 + w
				* (-40.0 + w2 * (12.0 + w * (-6.0 + w)))))) / 720.0;
			xWeight[2] = (397.0 / 7.0 - w * (245.0 / 3.0 + w * (-15.0 + w
				* (-95.0 / 3.0 + w * (15.0 + w * (5.0 + w
				* (-5.0 + w))))))) / 240.0;
			xWeight[3] = (2416.0 / 35.0 + w2 * (-48.0 + w2 * (16.0 + w2
				* (-4.0 + w)))) / 144.0;
			xWeight[4] = (1191.0 / 35.0 - w * (-49.0 + w * (-9.0 + w
				* (19.0 + w * (-3.0 + w) * (-3.0 + w2))))) / 144.0;
			xWeight[5] = (40.0 / 7.0 + w * (56.0 / 3.0 + w * (24.0 + w
				* (40.0 / 3.0 + w2 * (-4.0 + w * (-2.0 + w)))))) / 240.0;
			xWeight[7] = w2;
			xWeight[7] *= xWeight[7] * xWeight[7];
			xWeight[7] *= w / 5040.0;
			xWeight[6] = 1.0 - xWeight[0] - xWeight[1] - xWeight[2] - xWeight[3]
				- xWeight[4] - xWeight[5] - xWeight[7];
			/* y */
			w = px - (double)yIndex[3];
			yWeight[0] = 1.0 - w;
			yWeight[0] *= yWeight[0];
			yWeight[0] *= yWeight[0] * yWeight[0];
			yWeight[0] *= (1.0 - w) / 5040.0;
			w2 = w * w;
			yWeight[1] = (120.0 / 7.0 + w * (-56.0 + w * (72.0 + w
				* (-40.0 + w2 * (12.0 + w * (-6.0 + w)))))) / 720.0;
			yWeight[2] = (397.0 / 7.0 - w * (245.0 / 3.0 + w * (-15.0 + w
				* (-95.0 / 3.0 + w * (15.0 + w * (5.0 + w
				* (-5.0 + w))))))) / 240.0;
			yWeight[3] = (2416.0 / 35.0 + w2 * (-48.0 + w2 * (16.0 + w2
				* (-4.0 + w)))) / 144.0;
			yWeight[4] = (1191.0 / 35.0 - w * (-49.0 + w * (-9.0 + w
				* (19.0 + w * (-3.0 + w) * (-3.0 + w2))))) / 144.0;
			yWeight[5] = (40.0 / 7.0 + w * (56.0 / 3.0 + w * (24.0 + w
				* (40.0 / 3.0 + w2 * (-4.0 + w * (-2.0 + w)))))) / 240.0;
			yWeight[7] = w2;
			yWeight[7] *= yWeight[7] * yWeight[7];
			yWeight[7] *= w / 5040.0;
			yWeight[6] = 1.0 - yWeight[0] - yWeight[1] - yWeight[2] - yWeight[3]
				- yWeight[4] - yWeight[5] - yWeight[7];
			break;
		case 8:
			/* x */
			w = py - (double)xIndex[4];
			xWeight[0] = 0.5 - w;
			xWeight[0] *= xWeight[0];
			xWeight[0] *= xWeight[0];
			xWeight[0] *= xWeight[0] / 40320.0;
			w2 = w * w;
			xWeight[1] = (39.0 / 16.0 - w * (6.0 + w * (-9.0 / 2.0 + w2)))
				* (21.0 / 16.0 + w * (-15.0 / 4.0 + w * (9.0 / 2.0 + w
				* (-3.0 + w)))) / 5040.0;
			xWeight[2] = (82903.0 / 1792.0 + w * (-4177.0 / 32.0 + w
				* (2275.0 / 16.0 + w * (-487.0 / 8.0 + w * (-85.0 / 8.0 + w
				* (41.0 / 2.0 + w * (-5.0 + w * (-2.0 + w)))))))) / 1440.0;
			xWeight[3] = (310661.0 / 1792.0 - w * (14219.0 / 64.0 + w
				* (-199.0 / 8.0 + w * (-1327.0 / 16.0 + w * (245.0 / 8.0 + w
				* (53.0 / 4.0 + w * (-8.0 + w * (-1.0 + w)))))))) / 720.0;
			xWeight[4] = (2337507.0 / 8960.0 + w2 * (-2601.0 / 16.0 + w2
				* (387.0 / 8.0 + w2 * (-9.0 + w2)))) / 576.0;
			xWeight[5] = (310661.0 / 1792.0 - w * (-14219.0 / 64.0 + w
				* (-199.0 / 8.0 + w * (1327.0 / 16.0 + w * (245.0 / 8.0 + w
				* (-53.0 / 4.0 + w * (-8.0 + w * (1.0 + w)))))))) / 720.0;
			xWeight[7] = (39.0 / 16.0 - w * (-6.0 + w * (-9.0 / 2.0 + w2)))
				* (21.0 / 16.0 + w * (15.0 / 4.0 + w * (9.0 / 2.0 + w
				* (3.0 + w)))) / 5040.0;
			xWeight[8] = 0.5 + w;
			xWeight[8] *= xWeight[8];
			xWeight[8] *= xWeight[8];
			xWeight[8] *= xWeight[8] / 40320.0;
			xWeight[6] = 1.0 - xWeight[0] - xWeight[1] - xWeight[2] - xWeight[3]
				- xWeight[4] - xWeight[5] - xWeight[7] - xWeight[8];
			/* y */
			w = px - (double)yIndex[4];
			yWeight[0] = 0.5 - w;
			yWeight[0] *= yWeight[0];
			yWeight[0] *= yWeight[0];
			yWeight[0] *= yWeight[0] / 40320.0;
			w2 = w * w;
			yWeight[1] = (39.0 / 16.0 - w * (6.0 + w * (-9.0 / 2.0 + w2)))
				* (21.0 / 16.0 + w * (-15.0 / 4.0 + w * (9.0 / 2.0 + w
				* (-3.0 + w)))) / 5040.0;
			yWeight[2] = (82903.0 / 1792.0 + w * (-4177.0 / 32.0 + w
				* (2275.0 / 16.0 + w * (-487.0 / 8.0 + w * (-85.0 / 8.0 + w
				* (41.0 / 2.0 + w * (-5.0 + w * (-2.0 + w)))))))) / 1440.0;
			yWeight[3] = (310661.0 / 1792.0 - w * (14219.0 / 64.0 + w
				* (-199.0 / 8.0 + w * (-1327.0 / 16.0 + w * (245.0 / 8.0 + w
				* (53.0 / 4.0 + w * (-8.0 + w * (-1.0 + w)))))))) / 720.0;
			yWeight[4] = (2337507.0 / 8960.0 + w2 * (-2601.0 / 16.0 + w2
				* (387.0 / 8.0 + w2 * (-9.0 + w2)))) / 576.0;
			yWeight[5] = (310661.0 / 1792.0 - w * (-14219.0 / 64.0 + w
				* (-199.0 / 8.0 + w * (1327.0 / 16.0 + w * (245.0 / 8.0 + w
				* (-53.0 / 4.0 + w * (-8.0 + w * (1.0 + w)))))))) / 720.0;
			yWeight[7] = (39.0 / 16.0 - w * (-6.0 + w * (-9.0 / 2.0 + w2)))
				* (21.0 / 16.0 + w * (15.0 / 4.0 + w * (9.0 / 2.0 + w
				* (3.0 + w)))) / 5040.0;
			yWeight[8] = 0.5 + w;
			yWeight[8] *= yWeight[8];
			yWeight[8] *= yWeight[8];
			yWeight[8] *= yWeight[8] / 40320.0;
			yWeight[6] = 1.0 - yWeight[0] - yWeight[1] - yWeight[2] - yWeight[3]
				- yWeight[4] - yWeight[5] - yWeight[7] - yWeight[8];
			break;
		case 9:
			/* x */
			w = py - (double)xIndex[4];
			xWeight[0] = 1.0 - w;
			xWeight[0] *= xWeight[0];
			xWeight[0] *= xWeight[0];
			xWeight[0] *= xWeight[0] * (1.0 - w) / 362880.0;
			xWeight[1] = (502.0 / 9.0 + w * (-246.0 + w * (472.0 + w
				* (-504.0 + w * (308.0 + w * (-84.0 + w * (-56.0 / 3.0 + w
				* (24.0 + w * (-8.0 + w))))))))) / 40320.0;
			xWeight[2] = (3652.0 / 9.0 - w * (2023.0 / 2.0 + w * (-952.0 + w
				* (938.0 / 3.0 + w * (112.0 + w * (-119.0 + w * (56.0 / 3.0 + w
				* (14.0 + w * (-7.0 + w))))))))) / 10080.0;
			xWeight[3] = (44117.0 / 42.0 + w * (-2427.0 / 2.0 + w * (66.0 + w
				* (434.0 + w * (-129.0 + w * (-69.0 + w * (34.0 + w * (6.0 + w
				* (-6.0 + w))))))))) / 4320.0;
			w2 = w * w;
			xWeight[4] = (78095.0 / 63.0 - w2 * (700.0 + w2 * (-190.0 + w2
				* (100.0 / 3.0 + w2 * (-5.0 + w))))) / 2880.0;
			xWeight[5] = (44117.0 / 63.0 + w * (809.0 + w * (44.0 + w
				* (-868.0 / 3.0 + w * (-86.0 + w * (46.0 + w * (68.0 / 3.0 + w
				* (-4.0 + w * (-4.0 + w))))))))) / 2880.0;
			xWeight[6] = (3652.0 / 21.0 - w * (-867.0 / 2.0 + w * (-408.0 + w
				* (-134.0 + w * (48.0 + w * (51.0 + w * (-4.0 + w) * (-1.0 + w)
				* (2.0 + w))))))) / 4320.0;
			xWeight[7] = (251.0 / 18.0 + w * (123.0 / 2.0 + w * (118.0 + w
				* (126.0 + w * (77.0 + w * (21.0 + w * (-14.0 / 3.0 + w
				* (-6.0 + w * (-2.0 + w))))))))) / 10080.0;
			xWeight[9] = w2 * w2;
			xWeight[9] *= xWeight[9] * w / 362880.0;
			xWeight[8] = 1.0 - xWeight[0] - xWeight[1] - xWeight[2] - xWeight[3]
				- xWeight[4] - xWeight[5] - xWeight[6] - xWeight[7] - xWeight[9];
			/* y */
			w = px - (double)yIndex[4];
			yWeight[0] = 1.0 - w;
			yWeight[0] *= yWeight[0];
			yWeight[0] *= yWeight[0];
			yWeight[0] *= yWeight[0] * (1.0 - w) / 362880.0;
			yWeight[1] = (502.0 / 9.0 + w * (-246.0 + w * (472.0 + w
				* (-504.0 + w * (308.0 + w * (-84.0 + w * (-56.0 / 3.0 + w
				* (24.0 + w * (-8.0 + w))))))))) / 40320.0;
			yWeight[2] = (3652.0 / 9.0 - w * (2023.0 / 2.0 + w * (-952.0 + w
				* (938.0 / 3.0 + w * (112.0 + w * (-119.0 + w * (56.0 / 3.0 + w
				* (14.0 + w * (-7.0 + w))))))))) / 10080.0;
			yWeight[3] = (44117.0 / 42.0 + w * (-2427.0 / 2.0 + w * (66.0 + w
				* (434.0 + w * (-129.0 + w * (-69.0 + w * (34.0 + w * (6.0 + w
				* (-6.0 + w))))))))) / 4320.0;
			w2 = w * w;
			yWeight[4] = (78095.0 / 63.0 - w2 * (700.0 + w2 * (-190.0 + w2
				* (100.0 / 3.0 + w2 * (-5.0 + w))))) / 2880.0;
			yWeight[5] = (44117.0 / 63.0 + w * (809.0 + w * (44.0 + w
				* (-868.0 / 3.0 + w * (-86.0 + w * (46.0 + w * (68.0 / 3.0 + w
				* (-4.0 + w * (-4.0 + w))))))))) / 2880.0;
			yWeight[6] = (3652.0 / 21.0 - w * (-867.0 / 2.0 + w * (-408.0 + w
				* (-134.0 + w * (48.0 + w * (51.0 + w * (-4.0 + w) * (-1.0 + w)
				* (2.0 + w))))))) / 4320.0;
			yWeight[7] = (251.0 / 18.0 + w * (123.0 / 2.0 + w * (118.0 + w
				* (126.0 + w * (77.0 + w * (21.0 + w * (-14.0 / 3.0 + w
				* (-6.0 + w * (-2.0 + w))))))))) / 10080.0;
			yWeight[9] = w2 * w2;
			yWeight[9] *= yWeight[9] * w / 362880.0;
			yWeight[8] = 1.0 - yWeight[0] - yWeight[1] - yWeight[2] - yWeight[3]
				- yWeight[4] - yWeight[5] - yWeight[6] - yWeight[7] - yWeight[9];
			break;
		}

	   	/* apply the mirror boundary conditions */
	   	for (int k = 0; k <= splineDegree; k++)
	   	{
		    xIndex[k] = (imgHeight == 1) ? (0) : ((xIndex[k] < 0) ?
							  (-xIndex[k] - Width2 * ((-xIndex[k]) / Width2))
							: (xIndex[k] - Width2 * (xIndex[k] / Width2)));
		    if (imgHeight <= xIndex[k]) xIndex[k] = Width2 - xIndex[k];

		    yIndex[k] = (imgWidth == 1) ? (0) : ((yIndex[k] < 0) ?
							   (-yIndex[k] - Height2 * ((-yIndex[k]) / Height2))
							 : (yIndex[k] - Height2 * (yIndex[k] / Height2)));
		    if (imgWidth <= yIndex[k]) yIndex[k] = Height2 - yIndex[k];
		}

	   	/* perform interpolation */
	   	interpolated = 0.0;
	   	for (j = 0; j <= splineDegree; j++) {

		     w = 0.0;
		     for (i = 0; i <= splineDegree; i++) {
		    	 w += xWeight[i] * getImage(yIndex[j], xIndex[i]);
		     }
		     interpolated += yWeight[j] * w;
		}

	   	return interpolated;
   }

   /**
    * Returns the image array.
    * @return Image array.
    */
   public double[][] getImage()
   {
	   if (image == null) {
		   try {
			   if (imgInt != null) return DataSet.toDoubleArray(imgInt);
			   if (imgShort != null) return DataSet.toDoubleArray(DataSet.toIntArray(imgShort, 0));
			   return DataSet.toDoubleArray(DataSet.toIntArray(imgByte, 0));
		   } catch (Exception exc) {
			   exc.printStackTrace();
			   return null;
		   }
	   }
	   return this.image;
   }

   /**
    * Returns the image value at a given point.
    * @param x X position.
    * @param y Y position.
    * @return Image value.
    */
   public double getImage(int x, int y)
   {
	   if (image == null) {
		   try {
			   if (imgInt != null) return imgInt[x][y];
			   if (imgShort != null) return imgShort[x][y];
			   return imgByte[x][y];
		   } catch (Exception exc) {
			   exc.printStackTrace();
			   return 0;
		   }
	   }
	   return image[x][y];
   }

   /**
    * Sets the image value at a given point.
    * @param x X position.
    * @param y Y position.
    * @param z The value.
    */
   public void setImage(int x, int y, double z)
   {
	   if (image == null) {
		   try {
			   if (imgInt != null) {
				   imgInt[x][y] = (int) z;
				   return;
			   }
			   if (imgShort != null) {
				   imgShort[x][y] = (short) z;
				   return;
			   }
			   if (z < -128) z = -128;
			   if (z > 127) z = 127;
			   imgByte[x][y] = (byte) ((int) z);
			   return;
		   } catch (Exception exc) {
			   exc.printStackTrace();
			   return;
		   }
	   }
	   image[x][y] = (Double) z;
   }

   /**
    * Sets the degree of the interpolation.
    * @param degree New degree.
    */
   public void setDegree(int degree)
   {
		splineDegree = degree;
		//this.image  = clone(originalImage);
        samplesToCoefficients();
   }

   /**
    * Rotates around the center. Image dimensions will not change.
    * @param ang Angle in radians.
    */
   public void rotate(double ang)
   {
	   double[][] img = new double[image.length][image[0].length];
	   double x0 = (imgWidth-1.0)/2.0;
	   double y0 = (imgHeight-1.0)/2.0;
	   for (int i=0; i<image.length; i++)
	   {
		   for (int j=0; j<image[0].length; j++)
		   {
			   double dx = i - x0;
			   double dy = j - y0;
			   double r = Math.sqrt(dx*dx+dy*dy);
			   double a = Math.atan2(dy, dx);
			   double newX = x0 + r * FastMath.cos(a-ang);
			   double newY = y0 + r * FastMath.sin(a-ang);
			   try {
				   double newZ = this.interpolate(newX, newY);
				   img[i][j] = newZ;
			   } catch (Exception exc) {
				   double aproxX = Math.round(newX);
				   double aproxY = Math.round(newY);
				   if (Math.abs(aproxX-newX)<1E-10 && Math.abs(aproxY-newY)<1E-10) { // Try to fix possible critical problems when rotating 45, 90, ... degrees
					   try {
						   double newZ = this.interpolate(aproxX, aproxY);
						   img[i][j] = newZ;
					   } catch (Exception exc2) {
						   Logger.log(LEVEL.ERROR, "Found unexpected error when rotating image.");
					   }
				   }
			   }
		   }
	   }
	   //this.originalImage = img;
       this.image  = img; //clone(originalImage);
       samplesToCoefficients();
   }

   /**
    * Recenters the image at another position.
    * @param x0 X position of new center.
    * @param y0 Y position of new center.
    */
   public void recenter(double x0, double y0)
   {
	   double[][] img = new double[image.length][image[0].length];
	   x0 -= (imgWidth-1.0)/2.0;
	   y0 -= (imgHeight-1.0)/2.0;
	   for (int i=0; i<image.length; i++)
	   {
		   for (int j=0; j<image[0].length; j++)
		   {
			   double newX = x0 + i;
			   double newY = y0 + j;
			   try {
				   double newZ = 0.0;
				   if (!this.isOutOfImage(newX, newY)) newZ = this.interpolate(newX, newY);
				   img[i][j] = newZ;
			   } catch (Exception exc) {
				   Logger.log(LEVEL.ERROR, "Found unexpected error when recentering image.");
			   }
		   }
	   }
	   //this.originalImage = img;
       this.image  = img; //clone(originalImage);
       samplesToCoefficients();
   }

   /**
    * Resizes the image to another size using the method
    * Least-Squares Image Resizing Using Finite Differences. This method is
    * faster and more accurate than the method using spline interpolation.<P>
    * A. Mu&ntilde;oz Barrutia, T. Blu, M. Unser, "Least-Squares Image Resizing Using Finite Differences,"
    * IEEE Transactions on Image Processing, vol. 10, no. 9, pp. 1365-1378, September 2001.
    * @param w New width.
    * @param h New height.
    * @throws JPARSECException If an error occurs.
    */
   public void resize(int w, int h) throws JPARSECException {
	   double img[][] = getResizedData(w, h, 3);
	   //this.originalImage = img;
       this.image  = img; //clone(originalImage);
       imgWidth = image.length;
       imgHeight = image[0].length;
       samplesToCoefficients();
   }

   /**
    * Resizes the image data to another size using the method
    * Least-Squares Image Resizing Using Finite Differences. This method is
    * faster and more accurate than the method using spline interpolation.<P>
    * A. Mu&ntilde;oz Barrutia, T. Blu, M. Unser, "Least-Squares Image Resizing Using Finite Differences,"
    * IEEE Transactions on Image Processing, vol. 10, no. 9, pp. 1365-1378, September 2001.
    * @param w New width.
    * @param h New height.
    * @return The image data.
    * @throws JPARSECException If an error occurs.
    */
   public double[][] getResizedData(int w, int h) throws JPARSECException {
	   return Resize.resize(this.getImage(), w, h, false);
   }

   /**
    * Resizes the image data to another size using the method
    * Least-Squares Image Resizing Using Finite Differences. This method is
    * faster and more accurate than the method using spline interpolation.<P>
    * This call use an specific method to eliminate possible banding when 
    * resizing noisy astronomical images.
    * <P>
    * A. Mu&ntilde;oz Barrutia, T. Blu, M. Unser, "Least-Squares Image Resizing Using Finite Differences,"
    * IEEE Transactions on Image Processing, vol. 10, no. 9, pp. 1365-1378, September 2001.
    * @param w New width.
    * @param h New height.
    * @return The image data.
    * @throws JPARSECException If an error occurs.
    */
   public double[][] getResizedDataNoBanding(int w, int h) throws JPARSECException {
	   return Resize.resizeNoBanding(this.getImage(), w, h, false);
   }
   
   /**
    * Resizes the image to another size using P. Th&eacute;venaz interpolation.
    * This method is generally slower and less accurate, but sometimes
    * (when resizing to very little sizes) can be better than the one using
    * finite differences.
    * @param w New width.
    * @param h New height.
    * @param quality A quality value to set the quality of the resize.
    * Should be and odd number equal or greater than 1. The greater the
    * value the slower the resizing process will be.
    */
   public void resize(int w, int h, int quality)
   {
	   double img[][] = getResizedData(w, h, quality);
	   //this.originalImage = img;
       this.image  = img; //clone(originalImage);
       imgWidth = image.length;
       imgHeight = image[0].length;
       samplesToCoefficients();
   }


   /**
    * Resizes the image data to another size using P. Th&eacute;venaz interpolation.
    * This method is generally slower and less accurate, but sometimes
    * (when resizing to very little sizes) can be better than the one using
    * finite differences.
    * @param w New width.
    * @param h New height.
    * @param quality A quality value to set the quality of the resize.
    * Should be and odd number equal or greater than 1. The greater the
    * value the slower the resizing process will be.
    * @return The image data.
    */
   public double[][] getResizedData(int w, int h, int quality) {
	   double[][] img = new double[w][h];
	   double w0 = imgWidth-1.0;
	   double h0 = imgHeight-1.0;
	   for (int i=0; i<w; i++)
	   {
		   for (int j=0; j<h; j++)
		   {
			   double newX = w0 * (double) i / (w - 1.0);
			   double newY = h0 * (double) j / (h - 1.0);
			   try {
				   if (quality <= 1) {
					   double newZ = this.interpolate(newX, newY);
					   img[i][j] = newZ;
				   } else {
					   int rn = quality;
					   double newZ = 0.0;
					   int nc = 0;
					   double ws0 = w0 / ((w - 1.0) * (rn - 1.0));
					   double hs0 = h0 / ((h - 1.0) * (rn - 1.0));
					   double ws1 = newX - w0 * 0.5 / (w - 1.0);
					   double hs1 = newY - h0 * 0.5 / (h - 1.0);
					   for (int ri=0; ri<rn; ri++)
					   {
						   for (int rj=0; rj<rn; rj++)
						   {
							   double nX = ws1 + ws0 * ri;
							   double nY = hs1 + hs0 * rj;
							   if (nX >= 0 && nY >= 0 && nX <= w0 && nY <= h0) {
								   double v = this.interpolate(nX, nY);

								   newZ += v;
								   nc ++;
							   }
						   }
					   }
					   img[i][j] = newZ / (double) nc;
				   }
			   } catch (Exception exc) {}
		   }
	   }
	   return img;
   }
   
   /**
    * Interpolation at a given position calculated more accurately
    * respect the method with a single point. Recommended quality
    * value is 3, which means that 3 points will be averaged inside
    * the provided cell position to obtain the final interpolated value.
    * This value will represent better the flux at that position
    * averaged within one cell, and hence it is much better to maintain
    * the total flux.
    * @param newX Interpolation x position.
    * @param newY Interpolation y position.
    * @param quality Quality value, odd number greater than one. 1 or 
    * below will return the normal interpolated value.
    * @return The interpolated value.
    * @throws JPARSECException If an error occurs.
    */
   public double interpolate(double newX, double newY, int quality) throws JPARSECException {
	   double w0 = imgWidth-1.0;
	   double h0 = imgHeight-1.0;
	   if (quality <= 1) {
		   return this.interpolate(newX, newY);
	   } else {
		   int rn = quality;
		   double newZ = 0.0;
		   int nc = 0;
		   double ws0 = 1.0 / (rn - 1.0);
		   double ws1 = newX - 0.5;
		   double hs1 = newY - 0.5;
		   for (int ri=0; ri<rn; ri++)
		   {
			   for (int rj=0; rj<rn; rj++)
			   {
				   double nX = ws1 + ws0 * ri;
				   double nY = hs1 + ws0 * rj;
				   if (nX >= 0 && nY >= 0 && nX <= w0 && nY <= h0) {
					   double v = this.interpolate(nX, nY);

					   newZ += v;
					   nc ++;
				   }
			   }
		   }
		   return newZ / (double) nc;
	   }
   }
}
