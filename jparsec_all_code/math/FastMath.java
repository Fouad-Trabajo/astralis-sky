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
package jparsec.math;

import jparsec.ephem.Functions;

/**
 * Provides methods for fast (approximate) calculation of trigonometric functions.
 * Memory consumption is low, about 7 kB per trigonometric table, and speed is
 * several times better. Some methods comes from the FastMath library by Bill Rossi,
 * integrated in Apache Commons Math.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class FastMath
{
	// private constructor so that this class cannot be instantiated.
	private FastMath() {}

	// The value of N cannot be too high in old computers due to memory limitations
	private static int N = 3600; // 0.1 degree precision
	private static float[] sin = null;
	private static float[] tan = null;
	private static float[] asin = null;
	private static double STEP = Constant.TWO_PI / N, STEP_INVERSE = 1.0 / STEP;
	private static int A_N = N * 10; // 0.01 degree precision
	protected static double A_STEP = 1.0 / A_N;
	private static double PI3_OVER_2 = 3.0 * Constant.PI_OVER_TWO;
	private static double pow10[];
	private static int N_4 = N + N / 4;

	/**
	 * Sets the accurate mode flag for sin/cos/atan2 functions. Default is true and will
	 * produce errors of 4E-7 for sin/cos and 8E-5 for the accurate version of the atan2
	 * function. In case you set it to false you should increase the number of angles by
	 * a factor 10 at least for an acceptable (and just 10% faster) sin/cos.
	 */
	public static boolean ACCURATE_MODE = true;
	/**
	 * Set to true to use exact mode, with no speed optimization. Default is false.
	 */
	public static boolean EXACT_MODE = false;

	/** Exponential evaluated at integer values,
     * exp(x) =  expIntTableA[x + 750] + expIntTableB[x+750].
     */
    private static final double EXP_INT_TABLE_A[] = new double[1500];

    /** Exponential evaluated at integer values,
     * exp(x) =  expIntTableA[x + 750] + expIntTableB[x+750]
     */
    private static final double EXP_INT_TABLE_B[] = new double[1500];

    /** Exponential over the range of 0 - 1 in increments of 2^-10
     * exp(x/1024) =  expFracTableA[x] + expFracTableB[x].
     */
    private static final double EXP_FRAC_TABLE_A[] = new double[1025];

    /** Exponential over the range of 0 - 1 in increments of 2^-10
     * exp(x/1024) =  expFracTableA[x] + expFracTableB[x].
     */
    private static final double EXP_FRAC_TABLE_B[] = new double[1025];

    /**
     * 0x40000000 - used to split a double into two parts, both with the low order bits cleared.
     * Equivalent to 2^30.
     */
    private static final long HEX_40000000 = 0x40000000L; // 1073741824L

    /** Factorial table, for Taylor series expansions. */
    private static final double FACT[] = new double[20];

	/**
	 * Initializes the cache for sin and cos and the rest.
	 */
	public static void initialize()
	{
		sin = new float[N+1];
		tan = new float[N+1];
		for (int i=0; i<=N;i++)
		{
			double val = (double) i * STEP;
			sin[i] = (float) Math.sin(val);
			tan[i] = (float) Math.tan(val);
		}

		asin = new float[A_N+1];
		for (int i=0; i<=A_N;i++)
		{
			double aval = (double) i * A_STEP;
			asin[i] = (float) Math.asin(aval);
		}

		pow10 = new double[634];
		for (int i=0; i<pow10.length;i++)
		{
			pow10[i] = Double.parseDouble("1.0e"+(i-325)); //Math.pow(10.0, i-308);
		}
        int i;

        // Generate an array of factorials
        FACT[0] = 1.0;
        for (i = 1; i < FACT.length; i++) {
            FACT[i] = FACT[i-1] * i;
        }

        double tmp[] = new double[2];
        double recip[] = new double[2];

        // Populate expIntTable
        for (i = 0; i < 750; i++) {
            expint(i, tmp);
            EXP_INT_TABLE_A[i+750] = tmp[0];
            EXP_INT_TABLE_B[i+750] = tmp[1];

            if (i != 0) {
                // Negative integer powers
                splitReciprocal(tmp, recip);
                EXP_INT_TABLE_A[750-i] = recip[0];
                EXP_INT_TABLE_B[750-i] = recip[1];
            }
        }

        // Populate expFracTable
        for (i = 0; i < EXP_FRAC_TABLE_A.length; i++) {
            slowexp(i/1024.0, tmp);
            EXP_FRAC_TABLE_A[i] = tmp[0];
            EXP_FRAC_TABLE_B[i] = tmp[1];
        }
	}

	/**
	 * Sets the maximum number of angles to calculate.
	 * Default value is 3600, producing errors in the sin/cos
	 * values of around 4E-7 if {@linkplain #ACCURATE_MODE} is true.
	 * The initialization of the
	 * variables sin and cos is also done when calling this
	 * method.
	 * @param n The number of angles.
	 */
	public static void setMaximumNumberOfAngles(int n) {
		N = n;
		A_N = N * 10;
		STEP = Constant.TWO_PI / N;
		A_STEP = 1.0 / A_N;
		STEP_INVERSE = 1.0 / STEP;
		initialize();
	}

	/**
	 * Returns the maximum number of angles.
	 * @return Maximum number of angles.
	 */
	public static int getMaximumNumberOfAngles() {
		return N;
	}

	/**
	 * Returns the resolution of the calculations,
	 * defined as the number of calculation angles
	 * divided by 2 PI.
	 * @return The resolution.
	 */
	public static double getResolution() {
		return STEP;
	}

	/**
	 * Calculates the sine of the argument.
	 * @param x Argument.
	 * @return Its sine.
	 */
	public static double sin(double x)
	{
		if (EXACT_MODE) return Math.sin(x);

		if (sin == null) FastMath.initialize();
		if (x < 0 || x > Constant.TWO_PI)
			x = Functions.normalizeRadians(x);

		if (!ACCURATE_MODE) return sin[(int) (0.5 + x * STEP_INVERSE)];
		double di = x * STEP_INVERSE;
		int i = (int) di;
		if (i == di) return sin[i];
		return sin[i] + (sin[1 + i] - sin[i]) * (di - i);
	}

	/**
	 * Calculates the cosine of the argument.
	 * @param x Argument.
	 * @return Its cosine.
	 */
	public static double cos(double x)
	{
		if (EXACT_MODE) return Math.cos(x);

		if (sin == null) FastMath.initialize();
		if (x < 0 || x > Constant.TWO_PI)
			x = Functions.normalizeRadians(x);

		double di = N_4 - x * STEP_INVERSE;
		if (di > N) di -= N;
		if (!ACCURATE_MODE) return sin[(int) (0.5 + di)];
		int i = (int) di;
		if (i == di) return sin[i];
		return sin[i] + (sin[1 + i] - sin[i]) * (di - i);
	}

	/**
	 * Computes the sine and cosine of the argument in one call.
	 * @param x The argument.
	 * @param fast True to force disabling the accurate mode for
	 * better performance. In other functions this is set to false.
	 * @param sincos A previously allocated array of at least 2
	 * elements to hold the sine and cosine.
	 */
	public static void sincos(double x, boolean fast, double sincos[]) {
		if (EXACT_MODE) {
			sincos[0] = Math.sin(x);
			sincos[1] = Math.cos(x);
			return;
		}

		if (sin == null) FastMath.initialize();
		if (x < 0 || x > Constant.TWO_PI)
			x = Functions.normalizeRadians(x);

		double di = x * STEP_INVERSE;
		if (fast || !ACCURATE_MODE) {
			sincos[0] = sin[(int) (0.5 + di)];
			di = N_4 - di;
			if (di > N) di -= N;
			sincos[1] = sin[(int) (0.5 + di)];
			return;
		}

		int i = (int) di;
		if (i == di) {
			sincos[0] = sin[i];
		} else {
			sincos[0] = sin[i] + (sin[1 + i] - sin[i]) * (di - i);
		}

		di = N_4 - di;
		if (di > N) di -= N;
		i = (int) di;
		if (i == di) {
			sincos[1] = sin[i];
		} else {
			sincos[1] = sin[i] + (sin[1 + i] - sin[i]) * (di - i);
		}
	}

	/**
	 * Calculates the tangent of the argument. For critical arguments
	 * around PI/2 or 3PI/2 the maximum relative error is below 1%. This
	 * function is around 8 times faster than intrinsic Java function.
	 * @param x Argument.
	 * @return Its tangent.
	 */
	public static double tan(double x)
	{
		if (EXACT_MODE) return Math.tan(x);

		if (tan == null) FastMath.initialize();
		if (x < 0 || x > Constant.TWO_PI)
			x = Functions.normalizeRadians(x);
		if (Math.abs(Constant.PI_OVER_TWO-x) < 0.01 || Math.abs(PI3_OVER_2-x) < 0.01) return Math.tan(x);

		if (!ACCURATE_MODE) return tan[(int) (0.5 + x * STEP_INVERSE)];

		double di = x * STEP_INVERSE;
		int i = (int) di;
		if (i == N) return tan[i];
		return tan[i] + (tan[1 + i] - tan[i]) * (di - i);
	}

	/**
	 * Calculates the sine of the argument.
	 * @param x Argument.
	 * @return Its sine.
	 */
	public static float sinf(double x)
	{
		return (float) sin(x);
	}
	/**
	 * Calculates the cosine of the argument.
	 * @param x Argument.
	 * @return Its cosine.
	 */
	public static float cosf(double x)
	{
		return (float) cos(x);
	}

	/**
	 * Calculates the tangent of the argument.
	 * @param x Argument.
	 * @return Its tangent.
	 */
	public static float tanf(double x)
	{
		return (float) tan(x);
	}

	/**
	 * Calculates the arc-sine of the argument.
	 * Accuracy similar or better compared to sin and cos
	 * functions except for arguments very close to 1 or -1, where
	 * errors up to 0.1&deg; can be reached. 33x faster than intrinsic.
	 * @param x Argument.
	 * @return Its arc-sine.
	 */
	public static double asin(double x)
	{
		if (EXACT_MODE) return Math.asin(x);
		if (x == 0) return x;
		if (x > 1 || x < -1 || x == Double.NaN) return Double.NaN;

		double s = 1;
		if (x < 0) {
			x = Math.abs(x);
			s = -1;
		}

		if (asin == null) FastMath.initialize();
		if (!ACCURATE_MODE) return s*asin[(int) (0.5 + x * A_N)];

		double di = x * A_N;
		int i = (int) di;
		if (i == di || i == A_N) return asin[i];
		return s*(asin[i] + (asin[1 + i] - asin[i]) * (di - i));
	}

	/**
	 * Calculates the arc-cosine of the argument.
	 * Accuracy similar or better compared to sin and cos
	 * functions except for arguments very close to 1 or -1, where
	 * errors up to 0.1&deg; can be reached. 33x faster than intrinsic.
	 * @param x Argument.
	 * @return Its arc-cosine.
	 */
	public static double acos(double x)
	{
		return Constant.PI_OVER_TWO - asin(x);
	}

	/**
	 * Multiplies an integer value by a power of 2.
	 * @param val The value.
	 * @param x The power of 2. From 31 the integer data 
	 * range cannot account for the value.
	 * @return The result.
	 */
	public static int multiplyBy2ToTheX(int val, int x) {
		if (x < 20) {
			return val << x;
		} else {
			return (int) (val * Math.pow(2.0, x));
		}
	}
	/**
	 * Divides an integer value by a power of 2.
	 * @param val The value.
	 * @param x The power of 2.
	 * @return The result.
	 */
	public static int divideBy2ToTheX(int val, int x) {
		if (x < 20) {
			return val >> x;
		} else {
			return (int) (val / Math.pow(2.0, x));
		}
	}

	/**
	 * Multiplies a double value by a power of 10.
	 * @param val The value.
	 * @param x The power of 10.
	 * @return The result.
	 */
	public static double multiplyBy10ToTheX(double val, int x) {
		if (x > 308 || EXACT_MODE) return val * Math.pow(10.0, x);
		if (pow10 == null) FastMath.initialize();
		return val * pow10[x + 325];
	}

	/**
	 * Performs fast approximate atan2 calculation, with a
	 * maximum error of 0.005 radians or 0.3 degrees. If used
	 * for rotating coordinates in a 1000 px screen resolution,
	 * maximum error will be 2 px. It is
	 * 10 times faster than Math.atan2. Based on Hastings
	 * approximations. Note certain critical angles cannot
	 * be obtained with this method, so it is not suitable to
	 * obtain spherical coordinates.<P>
	 * For angles between +/- 45 deg this function uses
	 * {@linkplain #atan(double)}, with an accuracy 3 times
	 * better and only slightly worse performance.
	 * @param y Y value.
	 * @param x X value.
	 * @return The atan2.
	 */
	public static double atan2(double y, double x) {
		if (EXACT_MODE) return Math.atan2(y, x);

		if (Math.abs(y) <= Math.abs(x)) {
			if (y >= 0.0 && x <= 0.0) {
				if (x == 0.0 && y == 0.0) return 0;
				if (x == 0) return Constant.PI_OVER_TWO;
				return Math.PI + FastMath.atan(y/x);
			}
			if (y < 0.0 && x <= 0.0) {
				if (x == 0) return -Constant.PI_OVER_TWO;
				return -Math.PI + FastMath.atan(y/x);
			}
			return FastMath.atan(y/x);
		}

        if ( x == 0.0 )
        {
    		if (x == 0.0 && y == 0.0) return 0;
            if ( y > 0.0 ) return Constant.PI_OVER_TWO;
            return -Constant.PI_OVER_TWO;
        }
        double atan;
        double z = y/x;
        if ( Math.abs( z ) < 1 )
        {
            atan = z/(1.0 + 0.28*z*z);
            if ( x < 0.0 )
            {
                    if ( y < 0.0 ) return atan - Math.PI;
                    return atan + Math.PI;
            }
        } else {
            atan = Constant.PI_OVER_TWO - z/(z*z + 0.28);
            if ( y < 0 ) return atan - Math.PI;
        }
        return atan;
	}

	/**
	 * Performs fast atan2 approximation using cordic, more accurate than the other
	 * function, but not so fast. Its error is 2.5E-4 radians (0.015 deg), about
	 * 20 times more accurate than Hastings approximation, but 60% slower. It has
	 * an excellent ratio performance/accuracy. Suitable to be used in spherical coordinates.
	 * Function created by Simon Hosie, see http://www.dsprelated.com/showmessage/10922/1.php.
	 * <P>
	 * In case {@linkplain #ACCURATE_MODE} is true more terms will be used, with a precision
	 * up to 8E-5 radians (0.005 deg), but calculations will be slightly slower. This mode is
	 * still 4.5 times faster than intrinsic Math.atan2.
	 * @param y Y value.
	 * @param x X value.
	 * @return The arctan.
	 */
	public static double atan2_accurate(double y, double x) {
		if (EXACT_MODE) return Math.atan2(y, x);

        if ( x == 0.0 )
        {
                if ( y > 0.0 ) return Constant.PI_OVER_TWO;
                if ( y == 0.0 ) return 0.0;
                return -Constant.PI_OVER_TWO;
        }
        if ( y == 0.0 )
        {
                if ( x >= 0.0 ) return 0;
                if ( x < 0.0 ) return Math.PI;
        }

		double result = 0;
		double y2;

		if (y < 0)	/* if we point downward */
		{
			result = -Math.PI;
			y = -y;
			x = -x;
		}
		if (x < 0)	/* if we point left */
		{
			result += Constant.PI_OVER_TWO;
			y2 = y;
			y = -x;
			x = y2;
		}
		if (y > x)	/* 45 degrees or beyond */
		{
			result += Constant.PI_OVER_FOUR;
			y2 = y;
			y -= x;
			x += y2;
		}
		if (2*y > x)	/* 26.565 degrees */
		{
			result += 0.4636476090008061;
			y2 = y;
			y = 2 * y - x;
			x = 2 * x + y2;
		}
		if (4*y > x)	// 14.036 degrees
		{
			result += 0.24497866312686414;
			y2 = y;
			y = 4 * y - x;
			x = 4 * x + y2;
		}
		if (8*y > x)	// 7.125 degrees
		{
			result += 0.12435499454676144;
			y2 = y;
			y = 8 * y - x;
			x = 8 * x + y2;
		}
		if (ACCURATE_MODE) {
			if (16*y > x) // atan(1/16)
			{
				result += 0.06241880999595735;
				y2 = y;
				y = 16 * y - x;
				x = 16 * x + y2;
			}
			if (32*y > x) // atan(1/32)
			{
				result += 0.031239833430268277;
				y2 = y;
				y = 32 * y - x;
				x = 32 * x + y2;
			}
			if (64*y > x) // atan(1/64)
			{
				result += 0.015623728620476831;
				y2 = y;
				y = 64 * y - x;
				x = 64 * x + y2;
			}
		}

		/* linear interpolation of the remaining 64-ant */
		return result + 0.99483995637409152 * y / x;
	}

	/**
	 * Returns the sign of the argument.
	 * @param n A value.
	 * @return 0 if value = 0, 1 if value &gt; 0, -1 if value &lt; 0.
	 */
	public static int sign(double n) {
		if (n == 0) return 0;
		if (n > 0) return 1;
		return -1;
	}


	// http://martin.ankerl.com/2007/10/04/optimized-pow-approximation-for-java-and-c-c/

	/**
	 * Returns the approximate arctan of the argument. Maximum error is 0.09 deg.
	 * 20 times faster than intrinsic function. For x outside -1 to 1 the
	 * intrinsic function is called.
	 * @param x The tangent.
	 * @return The arctan.
	 */
	public static double atan(double x)
	{
		if (EXACT_MODE || x < -1.0 || x > 1.0) return Math.atan(x);

		if (x < 0.0) return Constant.PI_OVER_FOUR * x + x*(x + 1.0)*(0.2447 - 0.0663*x);
	    return Constant.PI_OVER_FOUR * x - x*(x - 1.0)*(0.2447 + 0.0663*x);
	}

	/**
	 * Performs fast approximate exponential function, with a maximum error
	 * of 4%, and usually around 2%. 10 times faster than intrinsic function,
	 * and 3 times faster than {@linkplain #exp(double)}.
	 * @param val The argument.
	 * @return The exponential.
	 */
	public static double exp_approx(double val) {
		if (EXACT_MODE) return Math.exp(val);

	    return Double.longBitsToDouble(((long) (1512775 * val + 1072632447)) << 32);
	}

	// ****** METHODS FROM APACHE COMMONS MATH ******

    /**
     * Natural logarithm. Math.log is called.
     *
     * @param x   a double
     * @return log(x)
     */
    public static double log(final double x) {
    	return Math.log(x);
    }

   /** Compute split[0], split[1] such that their sum is equal to d,
     * and split[0] has its 30 least significant bits as zero.
     * @param d number to split
     * @param split placeholder where to place the result
     */
    private static void split(final double d, final double split[]) {
        if (d < 8e298 && d > -8e298) {
            final double a = d * HEX_40000000;
            split[0] = (d + a) - a;
            split[1] = d - split[0];
        } else {
            final double a = d * 9.31322574615478515625E-10;
            split[0] = (d + a - d) * HEX_40000000;
            split[1] = d - split[0];
        }
    }

    /** Recompute a split.
     * @param a input/out array containing the split, changed
     * on output
     */
    private static void resplit(final double a[]) {
        final double c = a[0] + a[1];
        final double d = -(c - a[0] - a[1]);

        if (c < 8e298 && c > -8e298) {
            double z = c * HEX_40000000;
            a[0] = (c + z) - z;
            a[1] = c - a[0] + d;
        } else {
            double z = c * 9.31322574615478515625E-10;
            a[0] = (c + z - c) * HEX_40000000;
            a[1] = c - a[0] + d;
        }
    }

    /** Multiply two numbers in split form.
     * @param a first term of multiplication
     * @param b second term of multiplication
     * @param ans placeholder where to put the result
     */
    private static void splitMult(double a[], double b[], double ans[]) {
        ans[0] = a[0] * b[0];
        ans[1] = a[0] * b[1] + a[1] * b[0] + a[1] * b[1];

        /* Resplit */
        resplit(ans);
    }

    /** Compute the reciprocal of in.  Use the following algorithm.
     *  in = c + d.
     *  want to find x + y such that x+y = 1/(c+d) and x is much
     *  larger than y and x has several zero bits on the right.
     *
     *  Set b = 1/(2^22),  a = 1 - b.  Thus (a+b) = 1.
     *  Use following identity to compute (a+b)/(c+d)
     *
     *  (a+b)/(c+d)  =   a/c   +    (bc - ad) / (c^2 + cd)
     *  set x = a/c  and y = (bc - ad) / (c^2 + cd)
     *  This will be close to the right answer, but there will be
     *  some rounding in the calculation of X.  So by carefully
     *  computing 1 - (c+d)(x+y) we can compute an error and
     *  add that back in.   This is done carefully so that terms
     *  of similar size are subtracted first.
     *  @param in initial number, in split form
     *  @param result placeholder where to put the result
     */
    private static void splitReciprocal(final double in[], final double result[]) {
        final double b = 1.0/4194304.0;
        final double a = 1.0 - b;

        if (in[0] == 0.0) {
            in[0] = in[1];
            in[1] = 0.0;
        }

        result[0] = a / in[0];
        result[1] = (b*in[0]-a*in[1]) / (in[0]*in[0] + in[0]*in[1]);

        if (result[1] != result[1]) { // can happen if result[1] is NAN
            result[1] = 0.0;
        }

        /* Resplit */
        resplit(result);

        for (int i = 0; i < 2; i++) {
            /* this may be overkill, probably once is enough */
            double err = 1.0 - result[0] * in[0] - result[0] * in[1] -
            result[1] * in[0] - result[1] * in[1];
            /*err = 1.0 - err; */
            err = err * (result[0] + result[1]);
            /*printf("err = %16e\n", err); */
            result[1] += err;
        }
    }

    /** Compute (a[0] + a[1]) * (b[0] + b[1]) in extended precision.
     * @param a first term of the multiplication
     * @param b second term of the multiplication
     * @param result placeholder where to put the result
     */
    private static void quadMult(final double a[], final double b[], final double result[]) {
        final double xs[] = new double[2];
        final double ys[] = new double[2];
        final double zs[] = new double[2];

        /* a[0] * b[0] */
        split(a[0], xs);
        split(b[0], ys);
        splitMult(xs, ys, zs);

        result[0] = zs[0];
        result[1] = zs[1];

        /* a[0] * b[1] */
        split(b[1], ys);
        splitMult(xs, ys, zs);

        double tmp = result[0] + zs[0];
        result[1] = result[1] - (tmp - result[0] - zs[0]);
        result[0] = tmp;
        tmp = result[0] + zs[1];
        result[1] = result[1] - (tmp - result[0] - zs[1]);
        result[0] = tmp;

        /* a[1] * b[0] */
        split(a[1], xs);
        split(b[0], ys);
        splitMult(xs, ys, zs);

        tmp = result[0] + zs[0];
        result[1] = result[1] - (tmp - result[0] - zs[0]);
        result[0] = tmp;
        tmp = result[0] + zs[1];
        result[1] = result[1] - (tmp - result[0] - zs[1]);
        result[0] = tmp;

        /* a[1] * b[0] */
        split(a[1], xs);
        split(b[1], ys);
        splitMult(xs, ys, zs);

        tmp = result[0] + zs[0];
        result[1] = result[1] - (tmp - result[0] - zs[0]);
        result[0] = tmp;
        tmp = result[0] + zs[1];
        result[1] = result[1] - (tmp - result[0] - zs[1]);
        result[0] = tmp;
    }

    /** Compute exp(x) - 1
     * @param x number to compute shifted exponential
     * @return exp(x) - 1
     */
    public static double expm1(double x) {
      return expm1(x, null);
    }

    /** Internal helper method for expm1
     * @param x number to compute shifted exponential
     * @param hiPrecOut receive high precision result for -1.0 < x < 1.0
     * @return exp(x) - 1
     */
    private static double expm1(double x, double hiPrecOut[]) {
        if (x != x || x == 0.0) { // NaN or zero
            return x;
        }

        if (sin == null) initialize();

        if (x <= -1.0 || x >= 1.0) {
            // If not between +/- 1.0
            //return exp(x) - 1.0;
            double hiPrec[] = new double[2];
            exp(x, 0.0, hiPrec);
            if (x > 0.0) {
                return -1.0 + hiPrec[0] + hiPrec[1];
            } else {
                final double ra = -1.0 + hiPrec[0];
                double rb = -(ra + 1.0 - hiPrec[0]);
                rb += hiPrec[1];
                return ra + rb;
            }
        }

        double baseA;
        double baseB;
        double epsilon;
        boolean negative = false;

        if (x < 0.0) {
            x = -x;
            negative = true;
        }

        {
            int intFrac = (int) (x * 1024.0);
            double tempA = EXP_FRAC_TABLE_A[intFrac] - 1.0;
            double tempB = EXP_FRAC_TABLE_B[intFrac];

            double temp = tempA + tempB;
            tempB = -(temp - tempA - tempB);
            tempA = temp;

            temp = tempA * HEX_40000000;
            baseA = tempA + temp - temp;
            baseB = tempB + (tempA - baseA);

            epsilon = x - intFrac/1024.0;
        }


        /* Compute expm1(epsilon) */
        double zb = 0.008336750013465571;
        zb = zb * epsilon + 0.041666663879186654;
        zb = zb * epsilon + 0.16666666666745392;
        zb = zb * epsilon + 0.49999999999999994;
        zb = zb * epsilon;
        zb = zb * epsilon;

        double za = epsilon;
        double temp = za + zb;
        zb = -(temp - za - zb);
        za = temp;

        temp = za * HEX_40000000;
        temp = za + temp - temp;
        zb += za - temp;
        za = temp;

        /* Combine the parts.   expm1(a+b) = expm1(a) + expm1(b) + expm1(a)*expm1(b) */
        double ya = za * baseA;
        //double yb = za*baseB + zb*baseA + zb*baseB;
        temp = ya + za * baseB;
        double yb = -(temp - ya - za * baseB);
        ya = temp;

        temp = ya + zb * baseA;
        yb += -(temp - ya - zb * baseA);
        ya = temp;

        temp = ya + zb * baseB;
        yb += -(temp - ya - zb*baseB);
        ya = temp;

        //ya = ya + za + baseA;
        //yb = yb + zb + baseB;
        temp = ya + baseA;
        yb += -(temp - baseA - ya);
        ya = temp;

        temp = ya + za;
        //yb += (ya > za) ? -(temp - ya - za) : -(temp - za - ya);
        yb += -(temp - ya - za);
        ya = temp;

        temp = ya + baseB;
        //yb += (ya > baseB) ? -(temp - ya - baseB) : -(temp - baseB - ya);
        yb += -(temp - ya - baseB);
        ya = temp;

        temp = ya + zb;
        //yb += (ya > zb) ? -(temp - ya - zb) : -(temp - zb - ya);
        yb += -(temp - ya - zb);
        ya = temp;

        if (negative) {
            /* Compute expm1(-x) = -expm1(x) / (expm1(x) + 1) */
            double denom = 1.0 + ya;
            double denomr = 1.0 / denom;
            double denomb = -(denom - 1.0 - ya) + yb;
            double ratio = ya * denomr;
            temp = ratio * HEX_40000000;
            final double ra = ratio + temp - temp;
            double rb = ratio - ra;

            temp = denom * HEX_40000000;
            za = denom + temp - temp;
            zb = denom - za;

            rb += (ya - za * ra - za * rb - zb * ra - zb * rb) * denomr;

            // f(x) = x/1+x
            // Compute f'(x)
            // Product rule:  d(uv) = du*v + u*dv
            // Chain rule:  d(f(g(x)) = f'(g(x))*f(g'(x))
            // d(1/x) = -1/(x*x)
            // d(1/1+x) = -1/( (1+x)^2) *  1 =  -1/((1+x)*(1+x))
            // d(x/1+x) = -x/((1+x)(1+x)) + 1/1+x = 1 / ((1+x)(1+x))

            // Adjust for yb
            rb += yb * denomr;                      // numerator
            rb += -ya * denomb * denomr * denomr;   // denominator

            // negate
            ya = -ra;
            yb = -rb;
        }

        if (hiPrecOut != null) {
            hiPrecOut[0] = ya;
            hiPrecOut[1] = yb;
        }

        return ya + yb;
    }

    /** Compute exp(p) for a integer p in extended precision.
     * @param p integer whose exponential is requested
     * @param result placeholder where to put the result in extended precision
     * @return exp(p) in standard precision (equal to result[0] + result[1])
     */
    private static double expint(int p, final double result[]) {
        //double x = M_E;
        final double xs[] = new double[2];
        final double as[] = new double[2];
        final double ys[] = new double[2];
        //split(x, xs);
        //xs[1] = (double)(2.7182818284590452353602874713526625L - xs[0]);
        //xs[0] = 2.71827697753906250000;
        //xs[1] = 4.85091998273542816811e-06;
        //xs[0] = Double.longBitsToDouble(0x4005bf0800000000L);
        //xs[1] = Double.longBitsToDouble(0x3ed458a2bb4a9b00L);

        /* E */
        xs[0] = 2.718281828459045;
        xs[1] = 1.4456468917292502E-16;

        split(1.0, ys);

        while (p > 0) {
            if ((p & 1) != 0) {
                quadMult(ys, xs, as);
                ys[0] = as[0]; ys[1] = as[1];
            }

            quadMult(xs, xs, as);
            xs[0] = as[0]; xs[1] = as[1];

            p >>= 1;
        }

        if (result != null) {
            result[0] = ys[0];
            result[1] = ys[1];

            resplit(result);
        }

        return ys[0] + ys[1];
    }

	/**
     * Exponential function. Just slightly faster than intrinsic exp.
     *
     * Computes exp(x), function result is nearly rounded.   It will be correctly
     * rounded to the theoretical value for 99.9% of input values, otherwise it will
     * have a 1 UPL error.
     *
     * Method:
     *    Lookup intVal = exp(int(x))
     *    Lookup fracVal = exp(int(x-int(x) / 1024.0) * 1024.0 );
     *    Compute z as the exponential of the remaining bits by a polynomial minus one
     *    exp(x) = intVal * fracVal * (1 + z)
     *
     * Accuracy:
     *    Calculation is done with 63 bits of precision, so result should be correctly
     *    rounded for 99.9% of input values, with less than 1 ULP error otherwise.
     *
     * @param val   a double
     * @return double e<sup>x</sup>
     */
	public static double exp(double val) {
		//if (EXACT_MODE) return Math.exp(val);
        return exp(val, 0.0, null);
	}

    /**
     * Internal helper method for exponential function.
     * @param x original argument of the exponential function
     * @param extra extra bits of precision on input (To Be Confirmed)
     * @param hiPrec extra bits of precision on output (To Be Confirmed)
     * @return exp(x)
     */
    private static double exp(double x, double extra, double[] hiPrec) {
    	if (sin == null) initialize();

        double intPartA;
        double intPartB;
        int intVal;

        /* Lookup exp(floor(x)).
         * intPartA will have the upper 22 bits, intPartB will have the lower
         * 52 bits.
         */
        if (x < 0.0) {
            intVal = (int) -x;

            if (intVal > 746) {
                if (hiPrec != null) {
                    hiPrec[0] = 0.0;
                    hiPrec[1] = 0.0;
                }
                return 0.0;
            }

            if (intVal > 709) {
                /* This will produce a subnormal output */
                final double result = exp(x+40.19140625, extra, hiPrec) / 285040095144011776.0;
                if (hiPrec != null) {
                    hiPrec[0] /= 285040095144011776.0;
                    hiPrec[1] /= 285040095144011776.0;
                }
                return result;
            }

            if (intVal == 709) {
                /* exp(1.494140625) is nearly a machine number... */
                final double result = exp(x+1.494140625, extra, hiPrec) / 4.455505956692756620;
                if (hiPrec != null) {
                    hiPrec[0] /= 4.455505956692756620;
                    hiPrec[1] /= 4.455505956692756620;
                }
                return result;
            }

            intVal++;

            intPartA = EXP_INT_TABLE_A[750-intVal];
            intPartB = EXP_INT_TABLE_B[750-intVal];

            intVal = -intVal;
        } else {
            intVal = (int) x;

            if (intVal > 709) {
                if (hiPrec != null) {
                    hiPrec[0] = Double.POSITIVE_INFINITY;
                    hiPrec[1] = 0.0;
                }
                return Double.POSITIVE_INFINITY;
            }

            intPartA = EXP_INT_TABLE_A[750+intVal];
            intPartB = EXP_INT_TABLE_B[750+intVal];
        }

        /* Get the fractional part of x, find the greatest multiple of 2^-10 less than
         * x and look up the exp function of it.
         * fracPartA will have the upper 22 bits, fracPartB the lower 52 bits.
         */
        final int intFrac = (int) ((x - intVal) * 1024.0);
        final double fracPartA = EXP_FRAC_TABLE_A[intFrac];
        final double fracPartB = EXP_FRAC_TABLE_B[intFrac];

        /* epsilon is the difference in x from the nearest multiple of 2^-10.  It
         * has a value in the range 0 <= epsilon < 2^-10.
         * Do the subtraction from x as the last step to avoid possible loss of percison.
         */
        final double epsilon = x - (intVal + intFrac / 1024.0);

        /* Compute z = exp(epsilon) - 1.0 via a minimax polynomial.  z has
       full double precision (52 bits).  Since z < 2^-10, we will have
       62 bits of precision when combined with the contant 1.  This will be
       used in the last addition below to get proper rounding. */

        /* Remez generated polynomial.  Converges on the interval [0, 2^-10], error
       is less than 0.5 ULP */
        double z = 0.04168701738764507;
        z = z * epsilon + 0.1666666505023083;
        z = z * epsilon + 0.5000000000042687;
        z = z * epsilon + 1.0;
        z = z * epsilon + -3.940510424527919E-20;

        /* Compute (intPartA+intPartB) * (fracPartA+fracPartB) by binomial
       expansion.
       tempA is exact since intPartA and intPartB only have 22 bits each.
       tempB will have 52 bits of precision.
         */
        double tempA = intPartA * fracPartA;
        double tempB = intPartA * fracPartB + intPartB * fracPartA + intPartB * fracPartB;

        /* Compute the result.  (1+z)(tempA+tempB).  Order of operations is
       important.  For accuracy add by increasing size.  tempA is exact and
       much larger than the others.  If there are extra bits specified from the
       pow() function, use them. */
        final double tempC = tempB + tempA;
        final double result;
        if (extra != 0.0) {
            result = tempC*extra*z + tempC*extra + tempC*z + tempB + tempA;
        } else {
            result = tempC*z + tempB + tempA;
        }

        if (hiPrec != null) {
            // If requesting high precision
            hiPrec[0] = tempA;
            hiPrec[1] = tempC*extra*z + tempC*extra + tempC*z + tempB;
        }

        return result;
    }

    /**
     *  For x between 0 and 1, returns exp(x), uses extended precision
     *  @param x argument of exponential
     *  @param result placeholder where to place exp(x) split in two terms
     *  for extra precision (i.e. exp(x) = result[0] + result[1]
     *  @return exp(x)
     */
    private static double slowexp(final double x, final double result[]) {
        final double xs[] = new double[2];
        final double ys[] = new double[2];
        final double facts[] = new double[2];
        final double as[] = new double[2];
        split(x, xs);
        ys[0] = ys[1] = 0.0;

        for (int i = 19; i >= 0; i--) {
            splitMult(xs, ys, as);
            ys[0] = as[0];
            ys[1] = as[1];

            split(FACT[i], as);
            splitReciprocal(as, facts);

            splitAdd(ys, facts, as);
            ys[0] = as[0];
            ys[1] = as[1];
        }

        if (result != null) {
            result[0] = ys[0];
            result[1] = ys[1];
        }

        return ys[0] + ys[1];
    }

    /** Add two numbers in split form.
     * @param a first term of addition
     * @param b second term of addition
     * @param ans placeholder where to put the result
     */
    private static void splitAdd(final double a[], final double b[], final double ans[]) {
        ans[0] = a[0] + b[0];
        ans[1] = a[1] + b[1];

        resplit(ans);
    }

	/**
	 * Performs fast pow function. When y is integer and below +/- 6 output is exact
	 * and 6 times faster than Math.pow. When y is integer and x = 10 the function
	 * {@linkplain #multiplyBy10ToTheX(double, int)} is called, otherwise Math.pow is called.
	 * @param x Base.
	 * @param y Exponent.
	 * @return x^y.
	 */
	public static double pow(double x, double y) {
		if (y == (int) y && (x == 10.0 || (y >= -4 && y <= 4))) {
			if (x == 10) return FastMath.multiplyBy10ToTheX(1.0, (int)y);
			if (y == 0) return 1.0;
			if (y == 1) return x;
			if (y == -1) return 1.0 / x;
			double z = x * x;
			if (y == 2) return z;
			if (y == 3) return z*x;
			if (y == 4) return z*z;
			if (y == 5) return z*z*x;
			if (y == -2) return 1.0/z;
			if (y == -3) return 1.0/(z*x);
			if (y == -4) return 1.0/(z*z);
			if (y == -5) return 1.0/(z*z*x);
		}

		//if (EXACT_MODE)
			return Math.pow(x, y);

	}

    /** Compute the hyperbolic cosine of a number. 50% faster than intrinsic function.
     * @param x number on which evaluation is done
     * @return hyperbolic cosine of x
     */
    public static double cosh(double x) {
      if (x != x) {
          return x;
      }

      if (x > 20.0) {
          return exp(x)/2.0;
      }

      if (x < -20) {
          return exp(-x)/2.0;
      }

      double hiPrec[] = new double[2];
      if (x < 0.0) {
          x = -x;
      }
      exp(x, 0.0, hiPrec);

      double ya = hiPrec[0] + hiPrec[1];
      double yb = -(ya - hiPrec[0] - hiPrec[1]);

      double temp = ya * HEX_40000000;
      double yaa = ya + temp - temp;
      double yab = ya - yaa;

      // recip = 1/y
      double recip = 1.0/ya;
      temp = recip * HEX_40000000;
      double recipa = recip + temp - temp;
      double recipb = recip - recipa;

      // Correct for rounding in division
      recipb += (1.0 - yaa*recipa - yaa*recipb - yab*recipa - yab*recipb) * recip;
      // Account for yb
      recipb += -yb * recip * recip;

      // y = y + 1/y
      temp = ya + recipa;
      yb += -(temp - ya - recipa);
      ya = temp;
      temp = ya + recipb;
      yb += -(temp - ya - recipb);
      ya = temp;

      double result = ya + yb;
      result *= 0.5;
      return result;
    }

    /** Compute the hyperbolic sine of a number. 50% faster than intrinsic function.
     * @param x number on which evaluation is done
     * @return hyperbolic sine of x
     */
    public static double sinh(double x) {
      boolean negate = false;
      if (x != x) {
          return x;
      }

      if (x > 20.0) {
          return exp(x)/2.0;
      }

      if (x < -20) {
          return -exp(-x)/2.0;
      }

      if (x == 0) {
          return x;
      }

      if (x < 0.0) {
          x = -x;
          negate = true;
      }

      double result;

      if (x > 0.25) {
          double hiPrec[] = new double[2];
          exp(x, 0.0, hiPrec);

          double ya = hiPrec[0] + hiPrec[1];
          double yb = -(ya - hiPrec[0] - hiPrec[1]);

          double temp = ya * HEX_40000000;
          double yaa = ya + temp - temp;
          double yab = ya - yaa;

          // recip = 1/y
          double recip = 1.0/ya;
          temp = recip * HEX_40000000;
          double recipa = recip + temp - temp;
          double recipb = recip - recipa;

          // Correct for rounding in division
          recipb += (1.0 - yaa*recipa - yaa*recipb - yab*recipa - yab*recipb) * recip;
          // Account for yb
          recipb += -yb * recip * recip;

          recipa = -recipa;
          recipb = -recipb;

          // y = y + 1/y
          temp = ya + recipa;
          yb += -(temp - ya - recipa);
          ya = temp;
          temp = ya + recipb;
          yb += -(temp - ya - recipb);
          ya = temp;

          result = ya + yb;
          result *= 0.5;
      }
      else {
          double hiPrec[] = new double[2];
          expm1(x, hiPrec);

          double ya = hiPrec[0] + hiPrec[1];
          double yb = -(ya - hiPrec[0] - hiPrec[1]);

          /* Compute expm1(-x) = -expm1(x) / (expm1(x) + 1) */
          double denom = 1.0 + ya;
          double denomr = 1.0 / denom;
          double denomb = -(denom - 1.0 - ya) + yb;
          double ratio = ya * denomr;
          double temp = ratio * HEX_40000000;
          double ra = ratio + temp - temp;
          double rb = ratio - ra;

          temp = denom * HEX_40000000;
          double za = denom + temp - temp;
          double zb = denom - za;

          rb += (ya - za*ra - za*rb - zb*ra - zb*rb) * denomr;

          // Adjust for yb
          rb += yb*denomr;                        // numerator
          rb += -ya * denomb * denomr * denomr;   // denominator

          // y = y - 1/y
          temp = ya + ra;
          yb += -(temp - ya - ra);
          ya = temp;
          temp = ya + rb;
          yb += -(temp - ya - rb);
          ya = temp;

          result = ya + yb;
          result *= 0.5;
      }

      if (negate) {
          result = -result;
      }

      return result;
    }

    /** Compute the hyperbolic tangent of a number. 50% faster than intrinsic function.
     * @param x number on which evaluation is done
     * @return hyperbolic tangent of x
     */
    public static double tanh(double x) {
      boolean negate = false;

      if (x != x) {
          return x;
      }

      if (x > 20.0) {
          return 1.0;
      }

      if (x < -20) {
          return -1.0;
      }

      if (x == 0) {
          return x;
      }

      if (x < 0.0) {
          x = -x;
          negate = true;
      }

      double result;
      if (x >= 0.5) {
          double hiPrec[] = new double[2];
          // tanh(x) = (exp(2x) - 1) / (exp(2x) + 1)
          exp(x*2.0, 0.0, hiPrec);

          double ya = hiPrec[0] + hiPrec[1];
          double yb = -(ya - hiPrec[0] - hiPrec[1]);

          /* Numerator */
          double na = -1.0 + ya;
          double nb = -(na + 1.0 - ya);
          double temp = na + yb;
          nb += -(temp - na - yb);
          na = temp;

          /* Denominator */
          double da = 1.0 + ya;
          double db = -(da - 1.0 - ya);
          temp = da + yb;
          db += -(temp - da - yb);
          da = temp;

          temp = da * HEX_40000000;
          double daa = da + temp - temp;
          double dab = da - daa;

          // ratio = na/da
          double ratio = na/da;
          temp = ratio * HEX_40000000;
          double ratioa = ratio + temp - temp;
          double ratiob = ratio - ratioa;

          // Correct for rounding in division
          ratiob += (na - daa*ratioa - daa*ratiob - dab*ratioa - dab*ratiob) / da;

          // Account for nb
          ratiob += nb / da;
          // Account for db
          ratiob += -db * na / da / da;

          result = ratioa + ratiob;
      }
      else {
          double hiPrec[] = new double[2];
          // tanh(x) = expm1(2x) / (expm1(2x) + 2)
          expm1(x*2.0, hiPrec);

          double ya = hiPrec[0] + hiPrec[1];
          double yb = -(ya - hiPrec[0] - hiPrec[1]);

          /* Numerator */
          double na = ya;
          double nb = yb;

          /* Denominator */
          double da = 2.0 + ya;
          double db = -(da - 2.0 - ya);
          double temp = da + yb;
          db += -(temp - da - yb);
          da = temp;

          temp = da * HEX_40000000;
          double daa = da + temp - temp;
          double dab = da - daa;

          // ratio = na/da
          double ratio = na/da;
          temp = ratio * HEX_40000000;
          double ratioa = ratio + temp - temp;
          double ratiob = ratio - ratioa;

          // Correct for rounding in division
          ratiob += (na - daa*ratioa - daa*ratiob - dab*ratioa - dab*ratiob) / da;

          // Account for nb
          ratiob += nb / da;
          // Account for db
          ratiob += -db * na / da / da;

          result = ratioa + ratiob;
      }

      if (negate) {
          result = -result;
      }

      return result;
    }

    /** Compute the inverse hyperbolic cosine of a number.
     * @param a number on which evaluation is done
     * @return inverse hyperbolic cosine of a
     */
    public static double acosh(final double a) {
        return FastMath.log(a + FastMath.sqrt(a * a - 1));
    }

    /** Compute the inverse hyperbolic sine of a number.
     * @param a number on which evaluation is done
     * @return inverse hyperbolic sine of a
     */
    public static double asinh(double a) {

        boolean negative = false;
        if (a < 0) {
            negative = true;
            a = -a;
        }

        double absAsinh;
        if (a > 0.167) {
            absAsinh = FastMath.log(FastMath.sqrt(a * a + 1) + a);
        } else {
            final double a2 = a * a;
            if (a > 0.097) {
                absAsinh = a * (1 - a2 * (1 / 3.0 - a2 * (1 / 5.0 - a2 * (1 / 7.0 - a2 * (1 / 9.0 - a2 * (1.0 / 11.0 - a2 * (1.0 / 13.0 - a2 * (1.0 / 15.0 - a2 * (1.0 / 17.0) * 15.0 / 16.0) * 13.0 / 14.0) * 11.0 / 12.0) * 9.0 / 10.0) * 7.0 / 8.0) * 5.0 / 6.0) * 3.0 / 4.0) / 2.0);
            } else if (a > 0.036) {
                absAsinh = a * (1 - a2 * (1 / 3.0 - a2 * (1 / 5.0 - a2 * (1 / 7.0 - a2 * (1 / 9.0 - a2 * (1.0 / 11.0 - a2 * (1.0 / 13.0) * 11.0 / 12.0) * 9.0 / 10.0) * 7.0 / 8.0) * 5.0 / 6.0) * 3.0 / 4.0) / 2.0);
            } else if (a > 0.0036) {
                absAsinh = a * (1 - a2 * (1 / 3.0 - a2 * (1 / 5.0 - a2 * (1 / 7.0 - a2 * (1 / 9.0) * 7.0 / 8.0) * 5.0 / 6.0) * 3.0 / 4.0) / 2.0);
            } else {
                absAsinh = a * (1 - a2 * (1 / 3.0 - a2 * (1 / 5.0) * 3.0 / 4.0) / 2.0);
            }
        }

        return negative ? -absAsinh : absAsinh;

    }

    /** Compute the inverse hyperbolic tangent of a number.
     * @param a number on which evaluation is done
     * @return inverse hyperbolic tangent of a
     */
    public static double atanh(double a) {

        boolean negative = false;
        if (a < 0) {
            negative = true;
            a = -a;
        }

        double absAtanh;
        if (a > 0.15) {
            absAtanh = 0.5 * FastMath.log((1 + a) / (1 - a));
        } else {
            final double a2 = a * a;
            if (a > 0.087) {
                absAtanh = a * (1 + a2 * (1.0 / 3.0 + a2 * (1.0 / 5.0 + a2 * (1.0 / 7.0 + a2 * (1.0 / 9.0 + a2 * (1.0 / 11.0 + a2 * (1.0 / 13.0 + a2 * (1.0 / 15.0 + a2 * (1.0 / 17.0)))))))));
            } else if (a > 0.031) {
                absAtanh = a * (1 + a2 * (1.0 / 3.0 + a2 * (1.0 / 5.0 + a2 * (1.0 / 7.0 + a2 * (1.0 / 9.0 + a2 * (1.0 / 11.0 + a2 * (1.0 / 13.0)))))));
            } else if (a > 0.003) {
                absAtanh = a * (1 + a2 * (1.0 / 3.0 + a2 * (1.0 / 5.0 + a2 * (1.0 / 7.0 + a2 * (1.0 / 9.0)))));
            } else {
                absAtanh = a * (1 + a2 * (1.0 / 3.0 + a2 * (1.0 / 5.0)));
            }
        }

        return negative ? -absAtanh : absAtanh;

    }

	/**
	 * Performs fast approximate sqrt function. It is 50% faster than intrinsic function,
	 * and the error is 4% or below for arguments below 10000.
	 * @param a The argument.
	 * @return sqrt(a).
	 */
	public static double sqrt(double a) {
		if (ACCURATE_MODE || EXACT_MODE) return Math.sqrt(a);

	    long x = Double.doubleToLongBits(a) >> 32;
	    double y = Double.longBitsToDouble((x + 1072632448) << 31);

	    // repeat the following line for more precision, but uncommenting
	    // will be as fast as intrinsic ...
	    // y = (y + a / y) * 0.5;
	    return y;
	}

	   /**
     * Return the exponent of a double number, removing the bias.
     * <p>
     * For double numbers of the form 2<sup>x</sup>, the unbiased
     * exponent is exactly x.
     * </p>
     * @param d number from which exponent is requested
     * @return exponent for d in IEEE754 representation, without bias
     */
    public static int getExponent(final double d) {
        return (int) ((Double.doubleToLongBits(d) >>> 52) & 0x7ff) - 1023;
    }

    /**
     * Return the exponent of a float number, removing the bias.
     * <p>
     * For float numbers of the form 2<sup>x</sup>, the unbiased
     * exponent is exactly x.
     * </p>
     * @param f number from which exponent is requested
     * @return exponent for d in IEEE754 representation, without bias
     */
    public static int getExponent(final float f) {
        return ((Float.floatToIntBits(f) >>> 23) & 0xff) - 127;
    }

    /**
     * Multiply a double number by a power of 2.
     * @param d number to multiply
     * @param n power of 2
     * @return d &times; 2<sup>n</sup>
     */
    public static double scalb(final double d, final int n) {

        // first simple and fast handling when 2^n can be represented using normal numbers
        if ((n > -1023) && (n < 1024)) {
            return d * Double.longBitsToDouble(((long) (n + 1023)) << 52);
        }

        // handle special cases
        if (Double.isNaN(d) || Double.isInfinite(d) || (d == 0)) {
            return d;
        }
        if (n < -2098) {
            return (d > 0) ? 0.0 : -0.0;
        }
        if (n > 2097) {
            return (d > 0) ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        }

        // decompose d
        final long bits = Double.doubleToLongBits(d);
        final long sign = bits & 0x8000000000000000L;
        int  exponent   = ((int) (bits >>> 52)) & 0x7ff;
        long mantissa   = bits & 0x000fffffffffffffL;

        // compute scaled exponent
        int scaledExponent = exponent + n;

        if (n < 0) {
            // we are really in the case n <= -1023
            if (scaledExponent > 0) {
                // both the input and the result are normal numbers, we only adjust the exponent
                return Double.longBitsToDouble(sign | (((long) scaledExponent) << 52) | mantissa);
            } else if (scaledExponent > -53) {
                // the input is a normal number and the result is a subnormal number

                // recover the hidden mantissa bit
                mantissa = mantissa | (1L << 52);

                // scales down complete mantissa, hence losing least significant bits
                final long mostSignificantLostBit = mantissa & (1L << (-scaledExponent));
                mantissa = mantissa >>> (1 - scaledExponent);
                if (mostSignificantLostBit != 0) {
                    // we need to add 1 bit to round up the result
                    mantissa++;
                }
                return Double.longBitsToDouble(sign | mantissa);

            } else {
                // no need to compute the mantissa, the number scales down to 0
                return (sign == 0L) ? 0.0 : -0.0;
            }
        } else {
            // we are really in the case n >= 1024
            if (exponent == 0) {

                // the input number is subnormal, normalize it
                while ((mantissa >>> 52) != 1) {
                    mantissa = mantissa << 1;
                    --scaledExponent;
                }
                ++scaledExponent;
                mantissa = mantissa & 0x000fffffffffffffL;

                if (scaledExponent < 2047) {
                    return Double.longBitsToDouble(sign | (((long) scaledExponent) << 52) | mantissa);
                } else {
                    return (sign == 0L) ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                }

            } else if (scaledExponent < 2047) {
                return Double.longBitsToDouble(sign | (((long) scaledExponent) << 52) | mantissa);
            } else {
                return (sign == 0L) ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
            }
        }

    }

    /**
     * Multiply a float number by a power of 2.
     * @param f number to multiply
     * @param n power of 2
     * @return f &times; 2<sup>n</sup>
     */
    public static float scalb(final float f, final int n) {

        // first simple and fast handling when 2^n can be represented using normal numbers
        if ((n > -127) && (n < 128)) {
            return f * Float.intBitsToFloat((n + 127) << 23);
        }

        // handle special cases
        if (Float.isNaN(f) || Float.isInfinite(f) || (f == 0f)) {
            return f;
        }
        if (n < -277) {
            return (f > 0) ? 0.0f : -0.0f;
        }
        if (n > 276) {
            return (f > 0) ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
        }

        // decompose f
        final int bits = Float.floatToIntBits(f);
        final int sign = bits & 0x80000000;
        int  exponent  = (bits >>> 23) & 0xff;
        int mantissa   = bits & 0x007fffff;

        // compute scaled exponent
        int scaledExponent = exponent + n;

        if (n < 0) {
            // we are really in the case n <= -127
            if (scaledExponent > 0) {
                // both the input and the result are normal numbers, we only adjust the exponent
                return Float.intBitsToFloat(sign | (scaledExponent << 23) | mantissa);
            } else if (scaledExponent > -24) {
                // the input is a normal number and the result is a subnormal number

                // recover the hidden mantissa bit
                mantissa = mantissa | (1 << 23);

                // scales down complete mantissa, hence losing least significant bits
                final int mostSignificantLostBit = mantissa & (1 << (-scaledExponent));
                mantissa = mantissa >>> (1 - scaledExponent);
                if (mostSignificantLostBit != 0) {
                    // we need to add 1 bit to round up the result
                    mantissa++;
                }
                return Float.intBitsToFloat(sign | mantissa);

            } else {
                // no need to compute the mantissa, the number scales down to 0
                return (sign == 0) ? 0.0f : -0.0f;
            }
        } else {
            // we are really in the case n >= 128
            if (exponent == 0) {

                // the input number is subnormal, normalize it
                while ((mantissa >>> 23) != 1) {
                    mantissa = mantissa << 1;
                    --scaledExponent;
                }
                ++scaledExponent;
                mantissa = mantissa & 0x007fffff;

                if (scaledExponent < 255) {
                    return Float.intBitsToFloat(sign | (scaledExponent << 23) | mantissa);
                } else {
                    return (sign == 0) ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
                }

            } else if (scaledExponent < 255) {
                return Float.intBitsToFloat(sign | (scaledExponent << 23) | mantissa);
            } else {
                return (sign == 0) ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
            }
        }

    }

    /**
     * Absolute value.
     * @param x number from which absolute value is requested
     * @return abs(x)
     */
    public static int abs(final int x) {
        return (x < 0) ? -x : x;
    }

    /**
     * Absolute value.
     * @param x number from which absolute value is requested
     * @return abs(x)
     */
    public static long abs(final long x) {
        return (x < 0l) ? -x : x;
    }

    /**
     * Absolute value.
     * @param x number from which absolute value is requested
     * @return abs(x)
     */
    public static float abs(final float x) {
        return (x < 0.0f) ? -x : (x == 0.0f) ? 0.0f : x; // -0.0 => +0.0
    }

    /**
     * Absolute value.
     * @param x number from which absolute value is requested
     * @return abs(x)
     */
    public static double abs(double x) {
        return (x < 0.0) ? -x : (x == 0.0) ? 0.0 : x; // -0.0 => +0.0
    }

	/**
	 * Returns sqrt(x*x+y*y). 80 times faster than intrinsic.
	 * @param x First argument.
	 * @param y Second argument.
	 * @return sqrt(x*x+y*y).
	 */
	public static double hypot(double x, double y) {
		return Math.sqrt(x*x+y*y);
		// Below is implementation without overflow using Apache. 20 times faster than intrinsic function.
/*      if (Double.isInfinite(x) || Double.isInfinite(y)) {
	            return Double.POSITIVE_INFINITY;
	        } else if (Double.isNaN(x) || Double.isNaN(y)) {
	            return Double.NaN;
	        } else {

	            final int expX = getExponent(x);
	            final int expY = getExponent(y);
	            if (expX > expY + 27) {
	                // y is neglectible with respect to x
	                return abs(x);
	            } else if (expY > expX + 27) {
	                // x is neglectible with respect to y
	                return abs(y);
	            } else {

	                // find an intermediate scale to avoid both overflow and underflow
	                final int middleExp = (expX + expY) / 2;

	                // scale parameters without losing precision
	                final double scaledX = scalb(x, -middleExp);
	                final double scaledY = scalb(y, -middleExp);

	                // compute scaled hypotenuse
	                final double scaledH = sqrt(scaledX * scaledX + scaledY * scaledY);

	                // remove scaling
	                return scalb(scaledH, middleExp);
	            }
	        }
*/	}

	public static double parseDouble(String s) {
		if (pow10 == null) initialize();
		if (s.equals("NaN")) return Double.NaN;
		if (s.startsWith("+")) s = s.substring(1);
		int exp = 0;
		int e = s.indexOf("E");
		if (e < 0) e = s.indexOf("e");
		if (e >= 0) {
			String ss = s.substring(e+1);
			if (ss.startsWith("+")) ss = ss.substring(1);
			exp = Short.parseShort(ss);
			s = s.substring(0, e);
		} else {
			if (s.lastIndexOf("+") > 0 || s.lastIndexOf("-") > 0)
			throw new RuntimeException("Not a number");
		}
		int p = s.indexOf(".");
		int n = s.length();
		if (p >= 0) {
			s = s.substring(0, p) + s.substring(p+1);
			exp += p-1;
			n --;
		} else {
			exp += n-1;
		}
		if (n > 17) return (Long.parseLong(s.substring(0, 17)) * pow10[309]) * pow10[exp+325];
		if (n < 9) return (Integer.parseInt(s) * pow10[326-n]) * pow10[exp+325];

		return (Long.parseLong(s) * pow10[326-n]) * pow10[exp+325];
	}
}
