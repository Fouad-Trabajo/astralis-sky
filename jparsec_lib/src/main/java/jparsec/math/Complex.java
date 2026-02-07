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

import java.io.Serializable;

import jparsec.ephem.Functions;

/**
 * A support class for complex numbers.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Complex implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Real part.
	 */
	public double real;
	/**
	 * Imaginary part.
	 */
	public double imaginary;

	/**
	 * Default constructor.
	 */
	public Complex() {
		this.real = this.imaginary = 0.0;
	}

	/**
	 * Explicit constructor.
	 *
	 * @param r Real.
	 * @param i Imaginary.
	 */
	public Complex(double r, double i) {
		this.real = r;
		this.imaginary = i;
	}

	/**
	 * Constructor for a set of real values.
	 *
	 * @param reals Set of reals.
	 * @return Set of complex values.
	 */
	public static Complex[] createSetOfComplex(double[] reals) {
		Complex c[] = new Complex[reals.length];
		for (int i = 0; i < reals.length; i++) {
			c[i] = new Complex(reals[i], 0.0);
		}
		return c;
	}

	/**
	 * Returns a set of reals from the complex set.
	 *
	 * @param c Set of complex values.
	 * @return The reals.
	 */
	public static double[] getSetOfReals(Complex[] c) {
		double r[] = new double[c.length];
		for (int i = 0; i < c.length; i++) {
			r[i] = c[i].real;
		}
		return r;
	}

	/**
	 * Returns a set of imaginary values from the complex set.
	 *
	 * @param c Set of complex values.
	 * @return The imaginary values.
	 */
	public static double[] getSetOfImaginary(Complex[] c) {
		double r[] = new double[c.length];
		for (int i = 0; i < c.length; i++) {
			r[i] = c[i].imaginary;
		}
		return r;
	}

	/**
	 * Add operation.
	 *
	 * @param a A complex number.
	 * @return The sum of both.
	 */
	public Complex add(Complex a) {
		return new Complex(
			a.real + this.real,
			a.imaginary + this.imaginary);
	}

	/**
	 * Substract a complex to this instance.
	 *
	 * @param a A complex number.
	 * @return this-b.
	 */
	public Complex substract(Complex a) {
		return new Complex(
			this.real - a.real,
			this.imaginary - a.imaginary);
	}

	/**
	 * Product operation.
	 *
	 * @param a A complex number.
	 * @return this*b.
	 */
	public Complex multiply(Complex a) {
		return new Complex(
			this.real * a.real - this.imaginary * a.imaginary,
			this.imaginary * a.real + this.real * a.imaginary);
	}

	/**
	 * Obtains the conjugate.
	 *
	 * @return The conjugate.
	 */
	public Complex conjugate() {
		return new Complex(real, -imaginary);
	}

	/**
	 * Division operation.
	 *
	 * @param b A complex number.
	 * @return this/b.
	 */
	public Complex div(Complex b) {
		Complex c = new Complex();
		double r, den;
		if (Math.abs(b.real) >= Math.abs(b.imaginary)) {
			r = b.imaginary / b.real;
			den = b.real + r * b.imaginary;
			c.real = (this.real + r * this.imaginary) / den;
			c.imaginary = (this.imaginary - r * this.real) / den;
		} else {
			r = b.real / b.imaginary;
			den = b.imaginary + r * b.real;
			c.real = (this.real * r + this.imaginary) / den;
			c.imaginary = (this.imaginary * r - this.real) / den;
		}
		return c;
	}

	/**
	 * Absolute value.
	 *
	 * @return The absolute value.
	 */
	public double abs() {
		double x, y, ans, temp;
		x = Math.abs(this.real);
		y = Math.abs(this.imaginary);
		if (x == 0.0)
			ans = y;
		else if (y == 0.0)
			ans = x;
		else if (x > y) {
			temp = y / x;
			ans = x * Math.sqrt(1.0f + temp * temp);
		} else {
			temp = x / y;
			ans = y * Math.sqrt(1.0f + temp * temp);
		}
		return ans;
	}

	/**
	 * Obtains the root square.
	 *
	 * @return The root square.
	 */
	public Complex sqrt() {
		Complex c = new Complex();
		double x, y, w, r;
		if ((this.real == 0.0) && (this.imaginary == 0.0)) {
			c.real = 0.0;
			c.imaginary = 0.0;
			return c;
		} else {
			x = Math.abs(this.real);
			y = Math.abs(this.imaginary);
			if (x >= y) {
				r = y / x;
				w = Math.sqrt(x)
						* Math.sqrt(0.5 * (1.0 + Math.sqrt(1.0 + r * r)));
			} else {
				r = x / y;
				w = Math.sqrt(y)
						* Math.sqrt(0.5 * (r + Math.sqrt(1.0 + r * r)));
			}
			if (this.real >= 0.0) {
				c.real = w;
				c.imaginary = this.imaginary / (2.0 * w);
			} else {
				c.imaginary = (this.imaginary >= 0) ? w : -w;
				c.real = this.imaginary / (2.0 * c.imaginary);
			}
			return c;
		}
	}

	/**
	 * Multiplies a complex by a scalar.
	 *
	 * @param x A number.
	 * @return this*x.
	 */
	public Complex multiply(double x) {
		return new Complex(
			x * this.real,
			x * this.imaginary);
	}

	/**
	 * Returns the exponential function of certain Complex.
	 *
	 * @return Exp(this) as a new Complex.
	 */
	public Complex exponential() {
		double ang = Functions.normalizeRadians(this.imaginary);
		return new Complex(Math.exp(this.real) * Math.cos(ang),
				Math.exp(this.real) * Math.sin(ang));
	}

	/**
	 * Returns the <i>principal</i> angle of a Complex number, in
	 * radians, measured counter-clockwise from the real axis. (Think of the
	 * reals as the x-axis, and the imaginaries as the y-axis.)
	 *
	 * <BR>
	 * There are infinitely many solutions, besides the principal solution. If
	 * <b>A</b> is the principal solution of <i>arg(z)</i>, the others are of
	 * the form:
	 * <BR>
	 *
	 * <pre>
	 *     <b>A</b> + 2*k*<b>PI</b>
	 * </pre>
	 * <BR>
	 * where k is any integer.
	 * <BR>
	 * arg() always returns a double between -
	 * <b>PI</b> and +<b>PI</b>.
	 * <BR>
	 * <b>Note:</b>
	 * <BR>
	 * 2*PI radians is the same as 360 degrees.
	 * <BR>
	 * <b>Domain Restrictions:</b>
	 * <BR>
	 * There are no restrictions: the class defines arg(0) to be 0
	 *
	 * @return Principal angle (in radians)
	 **/
	public double arg() {
		return Math.atan2(imaginary, real);
	}

	/**
	 * Returns the sine of a Complex number.
	 *
	 * <BR>
	 * <pre>
	 *     sin(z)  =  ( exp(<i><b>i</b></i>*z) - exp(-<i><b>i</b></i>*z) ) / (2*<i><b>i</b></i>)
	 * </pre>
	 *
	 * @return The Complex sine
	 * @see Complex#asin()
	 * @see Complex#sinh()
	 * @see Complex#cosec()
	 * @see Complex#cos()
	 * @see Complex#tan()
	 **/
	public Complex sin() {
		Complex result;
		// sin(z) = ( exp(i*z) - exp(-i*z) ) / (2*i)

		double scalar;
		double iz_re, iz_im;
		double _re1, _im1;
		double _re2, _im2;

		// iz: i.mul(z) ...
		iz_re = -imaginary;
		iz_im = real;

		// _1: iz.exp() ...
		scalar = Math.exp(iz_re);
		_re1 = scalar * Math.cos(iz_im);
		_im1 = scalar * Math.sin(iz_im);

		// _2: iz.neg().exp() ...
		scalar = Math.exp(-iz_re);
		_re2 = scalar * Math.cos(-iz_im);
		_im2 = scalar * Math.sin(-iz_im);

		// _1: _1.sub(_2) ...
		_re1 = _re1 - _re2;
		_im1 = _im1 - _im2;

		// result: _1.div(2*i) ...
		result = new Complex(0.5 * _im1, -0.5 * _re1);

		return result;
	}

	/**
	 * Returns the cosine of a Complex number.
	 *
	 * <BR>
	 * <pre>
	 *     cos(z)  =  ( exp(<i><b>i</b></i>*z) + exp(-<i><b>i</b></i>*z) ) / 2
	 * </pre>
	 *
	 * @return The Complex cosine
	 * @see Complex#acos()
	 * @see Complex#cosh()
	 * @see Complex#sec()
	 * @see Complex#sin()
	 * @see Complex#tan()
	 **/
	public Complex cos() {
		Complex result;
		// cos(z) = ( exp(i*z) + exp(-i*z) ) / 2

		double scalar;
		double iz_re, iz_im;
		double _re1, _im1;
		double _re2, _im2;

		// iz: i.mul(z) ...
		iz_re = -imaginary;
		iz_im = real;

		// _1: iz.exp() ...
		scalar = Math.exp(iz_re);
		_re1 = scalar * Math.cos(iz_im);
		_im1 = scalar * Math.sin(iz_im);

		// _2: iz.neg().exp() ...
		scalar = Math.exp(-iz_re);
		_re2 = scalar * Math.cos(-iz_im);
		_im2 = scalar * Math.sin(-iz_im);

		// _1: _1.add(_2) ...
		_re1 = _re1 + _re2;
		_im1 = _im1 + _im2;

		// result: _1.scale(0.5) ...
		result = new Complex(0.5 * _re1, 0.5 * _im1);
		return result;
	}

	/**
	 * Returns the tangent of a Complex number.
	 *
	 * <BR>
	 * <pre>
	 * tan(z) = sin(z) / cos(z)
	 * </pre>
	 * <BR>
	 * <b>Domain Restrictions:</b>
	 * <BR>
	 * tan(z) is undefined whenever z = (k + 1/2) * <b>PI</b><br>
	 * where k is any integer
	 *
	 * @return The Complex tangent
	 * @see Complex#atan()
	 * @see Complex#tanh()
	 * @see Complex#cot()
	 * @see Complex#sin()
	 * @see Complex#cos()
	 **/
	public Complex tan() {
		Complex result;
		// tan(z) = sin(z) / cos(z)

		double scalar;
		double iz_re, iz_im;
		double _re1, _im1;
		double _re2, _im2;
		double _re3, _im3;

		double cs_re, cs_im;

		// sin() ...

		// iz: i.mul(z) ...
		iz_re = -imaginary;
		iz_im = real;

		// _1: iz.exp() ...
		scalar = Math.exp(iz_re);
		_re1 = scalar * Math.cos(iz_im);
		_im1 = scalar * Math.sin(iz_im);

		// _2: iz.neg().exp() ...
		scalar = Math.exp(-iz_re);
		_re2 = scalar * Math.cos(-iz_im);
		_im2 = scalar * Math.sin(-iz_im);

		// _3: _1.sub(_2) ...
		_re3 = _re1 - _re2;
		_im3 = _im1 - _im2;

		// result: _3.div(2*i) ...
		result = new Complex(0.5 * _im3, -0.5 * _re3);

		// cos() ...

		// _3: _1.add(_2) ...
		_re3 = _re1 + _re2;
		_im3 = _im1 + _im2;

		// cs: _3.scale(0.5) ...
		cs_re = 0.5 * _re3;
		cs_im = 0.5 * _im3;

		// result: result.div(cs) ...
		result = result.div(new Complex(cs_re, cs_im));
		return result;
	}

	/**
	 * Returns the cosecant of a Complex number.
	 *
	 * <BR>
	 * <pre>
	 * cosec(z) = 1 / sin(z)
	 * </pre>
	 * <BR>
	 * <b>Domain Restrictions:</b>
	 * <BR>
	 * cosec(z) is undefined whenever z = k * <b>PI</b><br>
	 * where k is any integer
	 *
	 * @return The Complex cosecant
	 * @see Complex#sin()
	 * @see Complex#sec()
	 * @see Complex#cot()
	 **/
	public Complex cosec() {
		Complex result;
		// cosec(z) = 1 / sin(z)

		double scalar;
		double iz_re, iz_im;
		double _re1, _im1;
		double _re2, _im2;

		// iz: i.mul(z) ...
		iz_re = -imaginary;
		iz_im = real;

		// _1: iz.exp() ...
		scalar = Math.exp(iz_re);
		_re1 = scalar * Math.cos(iz_im);
		_im1 = scalar * Math.sin(iz_im);

		// _2: iz.neg().exp() ...
		scalar = Math.exp(-iz_re);
		_re2 = scalar * Math.cos(-iz_im);
		_im2 = scalar * Math.sin(-iz_im);

		// _1: _1.sub(_2) ...
		_re1 = _re1 - _re2;
		_im1 = _im1 - _im2;

		// _result: _1.div(2*i) ...
		result = new Complex(0.5 * _im1, -0.5 * _re1);

		// result: one.div(_result) ...
		inv(result);
		return result;
	}

	/**
	 * Returns the secant of a Complex number.
	 *
	 * <BR>
	 * <pre>
	 * sec(z) = 1 / cos(z)
	 * </pre>
	 * <BR>
	 * <b>Domain Restrictions:</b>
	 * <BR>
	 * sec(z) is undefined whenever z = (k + 1/2) * <b>PI</b>
	 * where k is any integer
	 *
	 * @return The Complex secant
	 * @see Complex#cos()
	 * @see Complex#cosec()
	 * @see Complex#cot()
	 **/
	public Complex sec() {
		Complex result;
		// sec(z) = 1 / cos(z)

		double scalar;
		double iz_re, iz_im;
		double _re1, _im1;
		double _re2, _im2;

		// iz: i.mul(z) ...
		iz_re = -imaginary;
		iz_im = real;

		// _1: iz.exp() ...
		scalar = Math.exp(iz_re);
		_re1 = scalar * Math.cos(iz_im);
		_im1 = scalar * Math.sin(iz_im);

		// _2: iz.neg().exp() ...
		scalar = Math.exp(-iz_re);
		_re2 = scalar * Math.cos(-iz_im);
		_im2 = scalar * Math.sin(-iz_im);

		// _1: _1.add(_2) ...
		_re1 = _re1 + _re2;
		_im1 = _im1 + _im2;

		// result: _1.scale(0.5) ...
		result = new Complex(0.5 * _re1, 0.5 * _im1);

		// result: one.div(result) ...
		inv(result);
		return result;
	}

	/**
	 * Returns the cotangent of a Complex number.
	 *
	 * <BR>
	 * <pre>
	 * cot(z) = 1 / tan(z)
	 * </pre>
	 * <BR>
	 * <b>Domain Restrictions:</b>
	 * <BR>
	 * cot(z) is undefined whenever z = k * <b>PI</b><br>
	 * where k is any integer
	 *
	 * @return The Complex cotangent
	 * @see Complex#tan()
	 * @see Complex#cosec()
	 * @see Complex#sec()
	 **/
	public Complex cot() {
		Complex result;
		// cot(z) = 1 / tan(z) = cos(z) / sin(z)

		double scalar;
		double iz_re, iz_im;
		double _re1, _im1;
		double _re2, _im2;
		double _re3, _im3;

		double sn_re, sn_im;

		// cos() ...

		// iz: i.mul(z) ...
		iz_re = -imaginary;
		iz_im = real;

		// _1: iz.exp() ...
		scalar = Math.exp(iz_re);
		_re1 = scalar * Math.cos(iz_im);
		_im1 = scalar * Math.sin(iz_im);

		// _2: iz.neg().exp() ...
		scalar = Math.exp(-iz_re);
		_re2 = scalar * Math.cos(-iz_im);
		_im2 = scalar * Math.sin(-iz_im);

		// _3: _1.add(_2) ...
		_re3 = _re1 + _re2;
		_im3 = _im1 + _im2;

		// result: _3.scale(0.5) ...
		result = new Complex(0.5 * _re3, 0.5 * _im3);

		// sin() ...

		// _3: _1.sub(_2) ...
		_re3 = _re1 - _re2;
		_im3 = _im1 - _im2;

		// sn: _3.div(2*i) ...
		sn_re = 0.5 * _im3;
		sn_im = -0.5 * _re3;

		// result: result.div(sn) ...
		result = result.div(new Complex(sn_re, sn_im));
		return result;
	}

	/**
	 * Returns the hyperbolic sine of a Complex number.
	 *
	 * <BR>
	 * <pre>
	 * sinh(z) = (exp(z) - exp(-z)) / 2
	 * </pre>
	 *
	 * @return The Complex hyperbolic sine
	 * @see Complex#sin()
	 * @see Complex#asinh()
	 **/
	public Complex sinh() {
		Complex result;
		// sinh(z) = ( exp(z) - exp(-z) ) / 2

		double scalar;
		double _re1, _im1;
		double _re2, _im2;

		// _1: z.exp() ...
		scalar = Math.exp(real);
		_re1 = scalar * Math.cos(imaginary);
		_im1 = scalar * Math.sin(imaginary);

		// _2: z.neg().exp() ...
		scalar = Math.exp(-real);
		_re2 = scalar * Math.cos(-imaginary);
		_im2 = scalar * Math.sin(-imaginary);

		// _1: _1.sub(_2) ...
		_re1 = _re1 - _re2;
		_im1 = _im1 - _im2;

		// result: _1.scale(0.5) ...
		result = new Complex(0.5 * _re1, 0.5 * _im1);
		return result;
	}

	/**
	 * Returns the hyperbolic cosine of a Complex number.
	 *
	 * <BR>
	 * <pre>
	 * cosh(z) = (exp(z) + exp(-z)) / 2
	 * </pre>
	 *
	 * @return The Complex hyperbolic cosine
	 * @see Complex#cos()
	 * @see Complex#acosh()
	 **/
	public Complex cosh() {
		Complex result;
		// cosh(z) = ( exp(z) + exp(-z) ) / 2

		double scalar;
		double _re1, _im1;
		double _re2, _im2;

		// _1: z.exp() ...
		scalar = Math.exp(real);
		_re1 = scalar * Math.cos(imaginary);
		_im1 = scalar * Math.sin(imaginary);

		// _2: z.neg().exp() ...
		scalar = Math.exp(-real);
		_re2 = scalar * Math.cos(-imaginary);
		_im2 = scalar * Math.sin(-imaginary);

		// _1: _1.add(_2) ...
		_re1 = _re1 + _re2;
		_im1 = _im1 + _im2;

		// result: _1.scale(0.5) ...
		result = new Complex(0.5 * _re1, 0.5 * _im1);
		return result;
	}

	/**
	 * Returns the hyperbolic tangent of a Complex number.
	 *
	 * <BR>
	 * <pre>
	 * tanh(z) = sinh(z) / cosh(z)
	 * </pre>
	 *
	 * @return The Complex hyperbolic tangent
	 * @see Complex#tan()
	 * @see Complex#atanh()
	 **/
	public Complex tanh() {
		Complex result;
		// tanh(z) = sinh(z) / cosh(z)

		double scalar;
		double _re1, _im1;
		double _re2, _im2;
		double _re3, _im3;

		double ch_re, ch_im;

		// sinh() ...

		// _1: z.exp() ...
		scalar = Math.exp(real);
		_re1 = scalar * Math.cos(imaginary);
		_im1 = scalar * Math.sin(imaginary);

		// _2: z.neg().exp() ...
		scalar = Math.exp(-real);
		_re2 = scalar * Math.cos(-imaginary);
		_im2 = scalar * Math.sin(-imaginary);

		// _3: _1.sub(_2) ...
		_re3 = _re1 - _re2;
		_im3 = _im1 - _im2;

		// result: _3.scale(0.5) ...
		result = new Complex(0.5 * _re3, 0.5 * _im3);

		// cosh() ...

		// _3: _1.add(_2) ...
		_re3 = _re1 + _re2;
		_im3 = _im1 + _im2;

		// ch: _3.scale(0.5) ...
		ch_re = 0.5 * _re3;
		ch_im = 0.5 * _im3;

		// result: result.div(ch) ...
		result = result.div(new Complex(ch_re, ch_im));
		return result;
	}

	/**
	 * Returns the <i>principal</i> arc sine of a Complex number.
	 *
	 * <BR>
	 * <pre>
	 *     asin(z)  =  -<i><b>i</b></i> * log(<i><b>i</b></i>*z + sqrt(1 - z*z))
	 * </pre>
	 * <BR>
	 * There are infinitely many solutions, besides the principal solution. If
	 * <b>A</b> is the principal solution of <i>asin(z)</i>, the others are of
	 * the form:
	 * <BR>
	 *
	 *     k*<b>PI</b> + (-1)<sup>k</sup>  * <b>A</b>
	 * <BR>
	 * where k is any integer.
	 *
	 * @return Principal Complex arc sine
	 * @see Complex#sin()
	 * @see Complex#sinh()
	 **/
	public Complex asin() {
		Complex result;
		// asin(z) = -i * log(i*z + sqrt(1 - z*z))

		double _re1, _im1;

		// _1: one.sub(z.mul(z)) ...
		_re1 = 1.0 - ((real * real) - (imaginary * imaginary));
		_im1 = 0.0 - ((real * imaginary) + (imaginary * real));

		// result: _1.sqrt() ...
		result = new Complex(_re1, _im1);
		result = result.sqrt();

		// _1: z.mul(i) ...
		_re1 = -imaginary;
		_im1 = +real;

		// result: _1.add(result) ...
		result.real = _re1 + result.real;
		result.imaginary = _im1 + result.imaginary;

		// _1: result.log() ...
		_re1 = Math.log(result.abs());
		_im1 = result.arg();

		// result: i.neg().mul(_1) ...
		result.real = _im1;
		result.imaginary = -_re1;
		return result;
	}

	/**
	 * Returns the <i>principal</i> arc cosine of a Complex number.
	 *
	 * <BR>
	 * <pre>
	 *     acos(z)  =  -<i><b>i</b></i> * log( z + <i><b>i</b></i> * sqrt(1 - z*z) )
	 * </pre>
	 * <BR>
	 * There are infinitely many solutions, besides the principal solution. If
	 * <b>A</b> is the principal solution of <i>acos(z)</i>, the others are of
	 * the form:
	 * <BR>
	 *
	 * <pre>
	 *     2*k*<b>PI</b> +/- <b>A</b>
	 * </pre>
	 * <BR>
	 * where k is any integer.
	 *
	 * @return Principal Complex arc cosine
	 * @see Complex#cos()
	 * @see Complex#cosh()
	 **/
	public Complex acos() {
		Complex result;
		// acos(z) = -i * log( z + i * sqrt(1 - z*z) )

		double _re1, _im1;

		// _1: one.sub(z.mul(z)) ...
		_re1 = 1.0 - ((real * real) - (imaginary * imaginary));
		_im1 = 0.0 - ((real * imaginary) + (imaginary * real));

		// result: _1.sqrt() ...
		result = new Complex(_re1, _im1);
		result = result.sqrt();

		// _1: i.mul(result) ...
		_re1 = -result.imaginary;
		_im1 = +result.real;

		// result: z.add(_1) ...
		result.real = real + _re1;
		result.imaginary = imaginary + _im1;

		// _1: result.log()
		_re1 = Math.log(result.abs());
		_im1 = result.arg();

		// result: i.neg().mul(_1) ...
		result.real = _im1;
		result.imaginary = -_re1;
		return result;
	}

	/**
	 * Returns the <i>principal</i> arc tangent of a Complex number.
	 *
	 * <BR>
	 * <pre>
	 *     atan(z)  =  -<i><b>i</b></i>/2 * log( (<i><b>i</b></i>-z)/(<i><b>i</b></i>+z) )
	 * </pre>
	 * <BR>
	 * There are infinitely many solutions, besides the principal solution. If
	 * <b>A</b> is the principal solution of <i>atan(z)</i>, the others are of
	 * the form:
	 * <BR>
	 * <pre>
	 *     <b>A</b> + k*<b>PI</b>
	 * </pre>
	 * <BR>
	 * where k is any integer.
	 * <BR>
	 * <b>Domain Restrictions:</b>
	 * <BR>
	 * atan(z) is undefined for z = + <b>i</b> or z = - <b>i</b>
	 *
	 * @return Principal Complex arc tangent
	 * @see Complex#tan()
	 * @see Complex#tanh()
	 **/
	public Complex atan() {
		Complex result;
		// atan(z) = -i/2 * log( (i-z)/(i+z) )

		double _re1, _im1;

		// result: i.sub(z) ...
		result = new Complex(-real, 1.0 - imaginary);

		// _1: i.add(z) ...
		_re1 = +real;
		_im1 = 1.0 + imaginary;

		// result: result.div(_1) ...
		result = result.div(new Complex(_re1, _im1));

		// _1: result.log() ...
		_re1 = Math.log(result.abs());
		_im1 = result.arg();

		// result: half_i.neg().mul(_2) ...
		result.real = 0.5 * _im1;
		result.imaginary = -0.5 * _re1;
		return result;
	}

	/**
	 * Returns the <i>principal</i> inverse hyperbolic sine of a
	 * Complex number.
	 *
	 * <BR>
	 * <pre>
	 * asinh(z) = log(z + sqrt(z * z + 1))
	 * </pre>
	 * <BR>
	 * There are infinitely many solutions, besides the principal solution. If
	 * <b>A</b> is the principal solution of <i>asinh(z)</i>, the others are of
	 * the form:
	 * <BR>
	 *     k*<b>PI</b>*<b><i>i</i></b> + (-1)<sup>k</sup>  * <b>A</b>
	 * <BR>
	 * where k is any integer.
	 *
	 * @return Principal Complex inverse hyperbolic sine
	 * @see Complex#sinh()
	 **/
	public Complex asinh() {
		Complex result;
		// asinh(z) = log(z + sqrt(z*z + 1))

		double _re1, _im1;

		// _1: z.mul(z).add(one) ...
		_re1 = ((real * real) - (imaginary * imaginary)) + 1.0;
		_im1 = ((real * imaginary) + (imaginary * real)) + 0.0;

		// result: _1.sqrt() ...
		result = new Complex(_re1, _im1);
		result = result.sqrt();

		// result: z.add(result) ...
		result.real = real + result.real;
		result.imaginary = imaginary + result.imaginary;

		// _1: result.log() ...
		_re1 = Math.log(result.abs());
		_im1 = result.arg();

		// result: _1 ...
		result.real = _re1;
		result.imaginary = _im1;
		return result;
	}

	/**
	 * Returns the <i>principal</i> inverse hyperbolic cosine of a
	 * Complex number.
	 *
	 * <BR>
	 * <pre>
	 * acosh(z) = log(z + sqrt(z * z - 1))
	 * </pre>
	 * <BR>
	 * There are infinitely many solutions, besides the principal solution. If
	 * <b>A</b> is the principal solution of <i>acosh(z)</i>, the others are of
	 * the form:
	 * <BR>
	 * <pre>
	 *     2*k*<b>PI</b>*<b><i>i</i></b> +/- <b>A</b>
	 * </pre>
	 * <BR>
	 * where k is any integer.
	 *
	 * @return Principal Complex inverse hyperbolic cosine
	 * @see Complex#cosh()
	 **/
	public Complex acosh() {
		Complex result;
		// acosh(z) = log(z + sqrt(z*z - 1))

		double _re1, _im1;

		// _1: z.mul(z).sub(one) ...
		_re1 = ((real * real) - (imaginary * imaginary)) - 1.0;
		_im1 = ((real * imaginary) + (imaginary * real)) - 0.0;

		// result: _1.sqrt() ...
		result = new Complex(_re1, _im1);
		result = result.sqrt();

		// result: z.add(result) ...
		result.real = real + result.real;
		result.imaginary = imaginary + result.imaginary;

		// _1: result.log() ...
		_re1 = Math.log(result.abs());
		_im1 = result.arg();

		// result: _1 ...
		result.real = _re1;
		result.imaginary = _im1;
		return result;
	}

	/**
	 * Returns the <i>principal</i> inverse hyperbolic tangent of a
	 * Complex number.
	 *
	 * <BR>
	 * <pre>
	 * atanh(z) = 1 / 2 * log((1 + z) / (1 - z))
	 * </pre>
	 * <BR>
	 * There are infinitely many solutions, besides the principal solution. If
	 * <b>A</b> is the principal solution of <i>atanh(z)</i>, the others are of
	 * the form:
	 * <BR>
	 * <pre>
	 *     <b>A</b> + k*<b>PI</b>*<b><i>i</i></b>
	 * </pre>
	 * <BR>
	 * where k is any integer.
	 * <BR>
	 * <b>Domain Restrictions:</b>
	 * <BR>
	 * atanh(z) is undefined for z = + 1 or z = - 1
	 *
	 * @return Principal Complex inverse hyperbolic tangent
	 * @see Complex#tanh()
	 **/
	public Complex atanh() {
		Complex result;
		// atanh(z) = 1/2 * log( (1+z)/(1-z) )

		double _re1, _im1;

		// result: one.add(z) ...
		result = new Complex(1.0 + real, +imaginary);

		// _1: one.sub(z) ...
		_re1 = 1.0 - real;
		_im1 = -imaginary;

		// result: result.div(_1) ...
		result = result.div(new Complex(_re1, _im1));

		// _1: result.log() ...
		_re1 = Math.log(result.abs());
		_im1 = result.arg();

		// result: _1.scale(0.5) ...
		result.real = 0.5 * _re1;
		result.imaginary = 0.5 * _im1;
		return result;
	}

	/**
	 * Converts a Complex into a String of the form (
	 * <i>a</i> + <i>b</i>i).
	 *
	 * <BR>
	 * This enables the Complex to be easily printed. For example, if
	 * z was <i>2 - 5<b>i</b></i>, then
	 *
	 * <pre>
	 * System.out.println(&quot;z = &quot; + z);
	 * </pre>
	 *
	 * would print
	 *
	 * <pre>
	 *     z = (2 - 5i)
	 * </pre>
	 *
	 * <!-- <i><b>Note:</b>
	 * <ul>
	 * Concatenating Strings, using a system overloaded meaning of the
	 * "+" operator, in fact causes the toString() method to
	 * be invoked on the object z at runtime.
	 * </ul>
	 * -->
	 *
	 * @return String containing the cartesian coordinate
	 *         representation
	 **/
	@Override
	public String toString() {
		if (imaginary < 0.0) { // ...remembering NaN & Infinity
			return ("(" + real + " - " + (-imaginary) + "i)");
		} else if (1.0 / imaginary == Double.NEGATIVE_INFINITY) {
			return ("(" + real + " - " + 0.0 + "i)");
		} else {
			return ("(" + real + " + " + (+imaginary) + "i)");
		}
	}

	static private void inv(Complex z) {
		double zRe, zIm;
		double scalar;

		if (Math.abs(z.real) >= Math.abs(z.imaginary)) {
			scalar = 1.0 / (z.real + z.imaginary * (z.imaginary / z.real));

			zRe = scalar;
			zIm = scalar * (-z.imaginary / z.real);
		} else {
			scalar = 1.0 / (z.real * (z.real / z.imaginary) + z.imaginary);

			zRe = scalar * (z.real / z.imaginary);
			zIm = -scalar;
		}

		z.real = zRe;
		z.imaginary = zIm;
	}
}
