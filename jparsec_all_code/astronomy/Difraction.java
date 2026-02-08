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
package jparsec.astronomy;

import jparsec.io.image.ImageSplineTransform;
import jparsec.math.Constant;
import jparsec.util.JPARSECException;

/**
 * Implements difraction correction to sky images, in order to make them look as
 * through a telescope.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Difraction
{
	// private constructor so that this class cannot be instantiated.
	private Difraction() {}

	/**
	 * Obtain the difraction pattern of a point star supposing a perfect aligned
	 * telescope and lenses.
	 *
	 * @param diam_obj Diameter of the objective (supposed circular) in cm.
	 * @param lambda Wave length in cm. Use 0.000056 (5600 angstrom) for visible light.
	 * @param field Field of view of the pattern in arcseconds. In case is &lt;1 it will
	 * be set to 1.
	 * @param obstruc Diameter of the central obstruction of the telescope in cm. For
	 * 	      Schmidth-Cassegrain or Newton instruments, otherwise set to 0.0.
	 * @param spider Width of the spiders in cm, for Newton telescopes. Otherwise 0.0.
	 * @return An array with size (10*field)+1, (10*field)+1, representing the
	 *         difraction pattern.
	 */
	public static double[][] pattern(double diam_obj, double lambda, int field, double obstruc, double spider)
	{
		if (field < 1) field = 1;

		double R = diam_obj * 0.5;
		double L = lambda;
		double RET = 16.0; // Parts in which the reticulus will be divided by. If
		// increased, better results but much more computing time
		obstruc = obstruc / diam_obj;
		spider = spider / diam_obj;

		double ct = 2.0 * Math.PI / (L * Constant.RAD_TO_ARCSEC);
		int resolution_x = 10 * field;
		int resolution_y = 10 * field;
		double A[] = new double[resolution_x + 1];
		double B[] = new double[resolution_y + 1];
		double sec_x = (double) resolution_x / (double) field;
		double sec_y = (double) resolution_y / (double) field;
		for (int i = 0; i <= resolution_x; i++)
		{
			A[i] = ct * (double) (i - (int) (resolution_x * 0.5)) / sec_x;
		}
		for (int j = 0; j <= resolution_y; j++)
		{
			B[j] = -ct * (double) (j - (int) (resolution_y * 0.5)) / sec_y;
		}

		double r_ini = R;
		double dr = 2.0 * R / RET;

		// Initialize pattern and set the iterations to obtain the pattern
		// of the reticulus
		double pattern[][] = new double[resolution_x + 1][resolution_y + 1];
		for (double y = r_ini; y >= -r_ini; y = y - dr)
		{
			for (double x = -r_ini; x <= r_ini; x = x + dr)
			{
				double r1 = Math.sqrt(x * x + y * y);

				// Determine if the ray is blocked by the central obstruction or
				// the spiders
				if (r1 <= R && r1 >= (obstruc * R) && Math.abs(x) >= (spider * R) && Math.abs(y) >= (spider * R))
				{

					// Obtain the phase of the input ray and it's cosine or
					// intensity contribution of difraction
					for (int i = 0; i <= resolution_x; i++)
					{
						for (int j = 0; j <= resolution_y; j++)
						{
							double p = x * A[i] + y * B[j];
							pattern[i][j] += Math.cos(p);
						}
					}
				}
			}
		}

		// Now obtain the pattern of the objective. Note we assume circular
		// shape using polar coordinates r0 and theta
		dr = dr * 0.5;
		for (double r0 = dr; r0 <= R; r0 = r0 + dr)
		{
			double dtheta = 2.0 * Math.PI * dr / (r0 * 3.0);
			double etheta = 2.0 * Math.PI - dtheta;
			double dtheta0 = dtheta * 0.5 * Math.random();

			for (double theta = dtheta0; theta <= etheta; theta = theta + dtheta)
			{
				double x = r0 * Math.cos(theta);
				double y = r0 * Math.sin(theta);
				double r1 = Math.sqrt(x * x + y * y);

				// Determine if the ray is blocked by the central obstruction or
				// the spiders
				if (r1 <= R && r1 >= (obstruc * R) && Math.abs(x) >= (spider * R) && Math.abs(y) >= (spider * R))
				{

					// Obtain the phase of the input ray and it's cosine or
					// intensity contribution of difraction
					for (int i = 0; i <= resolution_x; i++)
					{
						for (int j = 0; j <= resolution_y; j++)
						{
							double p = x * A[i] + y * B[j];
							pattern[i][j] += Math.cos(p);
						}
					}
				}

			}
		}

		// Normalize by assigning unity to the central point. Amplitudes
		// transformed to intensities.
		double c = pattern[(int) (resolution_x * 0.5)][(int) (resolution_y * 0.5)];
		for (int i = 0; i <= resolution_x; i++)
		{
			for (int j = 0; j <= resolution_y; j++)
			{
				pattern[i][j] = pattern[i][j] * pattern[i][j] / (c * c);
			}
		}

		return pattern;
	}

	/**
	 * Obtain the difraction pattern in the visible of a point star supposing a
	 * perfect aligned telescope and lenses.
	 *
	 * @param telescope Telescope object.
	 * @param field Field of view in arcseconds.
	 * @return An array with size (10*field)+1, (10*field+1), representing the
	 *         difraction pattern over the given field.
	 */
	public static double[][] pattern(TelescopeElement telescope, int field)
	{
		return pattern(telescope.diameter / 10.0, 0.000056, field, telescope.centralObstruction / 10.0,
				telescope.spidersSize / 10.0);
	}

	/**
	 * Resamples a given difraction pattern to a given resolution, using P. Th&eacute;venaz
	 * 2d interpolation.
	 * This method should only be used with patterns generated with
	 * this class.
	 * @param pattern The input difraction pattern.
	 * @param spatialResolution The spatial resolution desired for the
	 * new pattern in arcseconds.
	 * @return The new pattern.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double[][] resample(double[][] pattern, double spatialResolution) throws JPARSECException {
		double res = ((pattern.length-1)/10.0)/pattern.length;
		double ratio = pattern.length * (res / spatialResolution);
		ImageSplineTransform ist = new ImageSplineTransform(pattern);
		int w = (int) (ratio + 0.5);
		ist.resize(w, w, 3);
		return ist.getImage();
	}

	/**
	 * Convolves a given sky image with a given telescope pattern. This method can be extremely
	 * slow.
	 * @param pattern The telescope difraction pattern.
	 * @param difraction_pattern_field Field of the pattern in arcseconds.
	 * @param screen_in The set of RGB colors for the sky image as a [x][y] matrix.
	 * @param field The field of view of the sky image in radians.
	 * @param background The background color. Should be close to black.
	 * @return The convolved sky image as a set of colors. The matrix is [RGB][x][y],
	 * where the first dimension identifies the R, G, and B colors of the image planes separately.
	 */
	public static int[][][] convolve(double pattern[][], int difraction_pattern_field,
			int screen_in[][], double field, int background)
	{
		int w = screen_in.length, h = screen_in[0].length;
		double difraction_scale_factor = (double) difraction_pattern_field * 0.5 * w / field;
		if (difraction_pattern_field < 1)
			difraction_pattern_field = 1;
		int screen_out[][][] = new int[3][w][h];
		double center = (pattern.length * 0.5);
		int size = (int) (0.5 + 0.5 * difraction_scale_factor / (double) (difraction_pattern_field * 10));
		double intensity[][] = new double[pattern.length][pattern.length];
		int deltax[][] = new int[pattern.length][pattern.length];
		int deltay[][] = new int[pattern.length][pattern.length];
		double normalize_factor = 0.0;
		for (int i = 0; i < pattern.length; i++)
		{
			for (int j = 0; j < pattern.length; j++)
			{
				intensity[i][j] = pattern[i][j] * pattern[i][j];
				normalize_factor += intensity[i][j];
				deltax[i][j] = (int) (difraction_scale_factor * (double) (i - center) / (double) center);
				deltay[i][j] = (int) (difraction_scale_factor * (double) (j - center) / (double) center);
			}
		}

		for (int dx = 0; dx < w; dx++)
		{
			for (int dy = 0; dy < h; dy++)
			{
				int color = screen_in[dx][dy];
				if (color != background)
				{
					int red_in = 0xff & (color >> 16);
					int green_in = 0xff & (color >> 8);
					int blue_in = 0xff & color;
					for (int i = 0; i < pattern.length; i++)
					{
						for (int j = 0; j < pattern.length; j++)
						{
							int red = (int) (red_in * intensity[i][j]);
							int green = (int) (green_in * intensity[i][j]);
							int blue = (int) (blue_in * intensity[i][j]);

							if ((red + green + blue) > 5 && isInTheScreen(dx + deltax[i][j], dy + deltay[i][j], size, w, h))
							{
								if (size > 0)
								{
									int ddx = deltax[i][j] - size;
									int ddy = deltay[i][j] - size;
									for (int k = 0; k <= (2.0 * size); k++)
									{
										for (int l = 0; l <= (2.0 * size); l++)
										{
											screen_out[0][(dx + ddx + k)][(dy + ddy + l)] += red;
											screen_out[1][(dx + ddx + k)][(dy + ddy + l)] += green;
											screen_out[2][(dx + ddx + k)][(dy + ddy + l)] += blue;
										}
									}
								} else
								{
									screen_out[0][(dx + deltax[i][j])][(dy + deltay[i][j])] += red;
									screen_out[1][(dx + deltax[i][j])][(dy + deltay[i][j])] += green;
									screen_out[2][(dx + deltax[i][j])][(dy + deltay[i][j])] += blue;
								}
							}
						}
					}
				}
			}
		}

		// Controls contrast (background considered RGB emision). 10 is an
		// adequate value in principle.
		normalize_factor *= (1.0 + 2*size)*(1.0 + 2*size);
		int backgroundCol = 10;
		for (int i = 0; i < w; i++)
		{
			for (int j = 0; j < h; j++)
			{
				int red = screen_out[0][i][j];
				int green = screen_out[1][i][j];
				int blue = screen_out[2][i][j];

				red = (int) (red / normalize_factor);
				green = (int) (green / normalize_factor);
				blue = (int) (blue / normalize_factor);

				if (red < backgroundCol && green < backgroundCol && blue < backgroundCol)
				{
					red = (background>>16)&255;
					green = (background>>8)&255;
					blue = (background)&255;
				}

				screen_out[0][i][j] = red;
				screen_out[1][i][j] = green;
				screen_out[2][i][j] = blue;
			}
		}

		return screen_out;
	}

	private static boolean isInTheScreen(int x, int y, int size, int w, int h)
	{
		boolean isVisible = false;

		if (x >= size && x < (w - size) && y >= size && y < (h - size))
			isVisible = true;

		return isVisible;
	}
}
