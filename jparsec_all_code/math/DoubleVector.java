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

/*
 *    DoubleVector.java
 *    Copyright (C) 2002 Yong Wang
 *
 */

package jparsec.math;

import java.lang.reflect.Method;
import java.util.Arrays;

import jparsec.graph.DataSet;
import jparsec.util.JPARSECException;

/**
 * A vector specialized on doubles.
 *
 * @author Yong Wang
 * @version $Revision: 1.2 $
 */
public class  DoubleVector implements Cloneable {

  double[] V; // array for internal storage of elements.

  private int  sizeOfVector;      // size of the vector

  /** Constructs a null vector.
   */
  public DoubleVector() {
    this( 0 );
  }

  /** Constructs an n-vector of zeros.
      @param n    length.
  */
  public DoubleVector( int n ){
    V = new double[ n ];
    sizeOfVector = V.length;
  }

  /** Constructs a constant n-vector.
      @param n    length.
      @param s    the scalar value used to fill the vector
  */
  public DoubleVector( int n, double s ){
    this( n );
    set( s );
  }

  /** Constructs a vector directly from a double array
   *  @param v   the array
   */
  public DoubleVector( double v[] ){
    if( v == null ) {
      V = new double[0];
      sizeOfVector = V.length;
    }
    else {
      V = v.clone();
      sizeOfVector = V.length;
    }
  }

  /** Constructs a 3d vector directly from its components
   *  @param x X value
   *  @param y Y value
   *  @param z Z value
   */
  public DoubleVector(double x, double y, double z) {
       V = new double[] {x, y, z};
       sizeOfVector = V.length;
  }


  /** Set a single element.
   *  @param i    Index.
   *  @param s    a[i].
   */
  public void  set( int i, double s ) {

    V[i] = s;
  }

  /** Set all elements to a value
   *  @param s    the value
   */
  public void  set( double s ) {
    set(0, size()-1, s);
  }

  /** Set some elements to a value
   *  @param i0 the index of the first element
   *  @param i1 the index of the second element
   *  @param s the value
   */
  public void set( int i0, int i1, double s ) {

    for(int i = i0; i <= i1; i++ )
      V[i] = s;
  }

  /** Set some elements using a 2-D array
   *  @param i0 the index of the first element
   *  @param i1 the index of the second element
   *  @param j0 the index of the starting element in the 2-D array
   *  @param v the values
   */
  public void  set( int i0, int i1, double [] v, int j0){
    for(int i = i0; i<= i1; i++)
      V[i] = v[j0 + i - i0];
  }

  /** Set the elements using a DoubleVector
   *  @param v the DoubleVector
   */
  public void  set( DoubleVector v ){
    set( 0, v.size() - 1, v, 0);
  }

  /** Set some elements using a DoubleVector.
   *  @param i0 the index of the first element
   *  @param i1 the index of the second element
   *  @param v the DoubleVector
   *  @param j0 the index of the starting element in the DoubleVector
   */
  public void  set( int i0, int i1, DoubleVector v, int j0){
    for(int i = i0; i<= i1; i++)
      V[i] = v.V[j0 + i - i0];
  }

  /** Access the internal one-dimensional array.
      @return     Pointer to the one-dimensional array of vector elements.
  */
  public double []  getArray() {
    return V;
  }

  void  setArray( double [] a ) {
    V = a;
  }

  /** Returns a copy of the DoubleVector usng a double array.
      @return the one-dimensional array.  */
  public double[] getArrayCopy() {
    double v[] = new double[size()];

    for(int i= 0; i < size(); i++ )
      v[i] = V[i];

    return v;
  }

  /** Sorts the array in place */
  public void  sort() {
    Arrays.sort( V, 0, size() );
  }

  /** Gets the size of the vector.
      @return     the size
  */
  public int  size(){
    return sizeOfVector;
  }

  /**
   *  Sets the size of the vector
   *  @param m the size
   * @throws JPARSECException If the size is greater than the capacity.
   */
  public void  setSize( int m ) throws JPARSECException{
    if( m > capacity() )
      throw new JPARSECException("insufficient capacity");
    sizeOfVector = m;
  }

  /** Gets the capacity of the vector.
   *  @return     the capacity.
   */
  public int  capacity() {
    if( V == null ) return 0;
    return V.length;
  }

  /** Sets the capacity of the vector
   *  @param n the capacity.
   * @throws JPARSECException If an error occurs.
   */
  public void  setCapacity ( int n ) throws JPARSECException {
    if( n == capacity() ) return;
    double [] oldV = V;
    int m = Math.min( n, size() );
    V = new double[ n ];
    setSize( m );
    set(0, m-1, oldV, 0);
  }

  /** Gets a single element.
   *  @param i    Index.
   *  @return     the value of the i-th element
   */
  public double  get( int i ) {
    return V[i];
  }

  /**
   *  Adds a value to an element
   *  @param i  the index of the element
   *  @param s the value
   */
  public void  setPlus( int i, double s ) {
    V[i] += s;
  }

  /**
   *  Multiplies a value to an element
   *  @param i  the index of the element
   *  @param s the value
   */
  public void  setTimes( int i, double s ) {
    V[i] *= s;
  }

  /**
   *  Adds an element into the vector.
   *  @param x  the value of the new element.
   * @throws JPARSECException If the size (incremented by 1 with this call)
   * is greater than the capacity.
   */
  public void addElement( double x ) throws JPARSECException {
    if( capacity() == 0 ) setCapacity( 10 );
    if( size() == capacity() ) setCapacity( 2 * capacity() );
    V[size()] = x;
    setSize( size() + 1 );
  }

  /**
   *  Returns the squared vector.
   *  @return The result as a new vector, without modifying the current instance.
   */
  public DoubleVector square() {
    DoubleVector v = new DoubleVector( size() );
    for(int i = 0; i < size(); i++ ) v.V[i] = V[i] * V[i];
    return v;
  }

  /**
   *  Returns the inverted vector
   *  @return The result as a new vector, without modifying the current instance.
   */
  public DoubleVector invert() {
    DoubleVector v = new DoubleVector( size() );
    for(int i = 0; i < size(); i++ ) v.V[i] = 1.0 / V[i];
    return v;
  }

  /**
   *  Returns the square-root of all the elements in the vector .
   *  @return The result as a new vector, without modifying the current instance.
   */
  public DoubleVector sqrt() {
    DoubleVector v = new DoubleVector( size() );
    for(int i = 0; i < size(); i++ ) v.V[i] = Math.sqrt(V[i]);
    return v;
  }

  /** Makes a deep copy of the vector.
   *  @return The result as a new vector, without modifying the current instance.
   */
  public DoubleVector  copy() {
    return clone();
  }

  /** Clones the DoubleVector object.
   */
  public DoubleVector  clone() {
    int n = size();
    DoubleVector u = new DoubleVector( n );
    for( int i = 0; i < n; i++)
      u.V[i] = V[i];
    return u;
  }

  /**
   * Returns the inner product of two DoubleVectors
   * @param v the second DoubleVector
   * @return the product
   * @throws JPARSECException If the size of the input vector
   * is different from this one.
   */
  public double  innerProduct(DoubleVector v) throws JPARSECException {
    if(size() != v.size())
      throw new JPARSECException("sizes unmatch");
    double p = 0;
    for (int i = 0; i < size(); i++) {
      p += V[i] * v.V[i];
    }
    return p;
  }

  /**
   * Returns the signs of all elements in terms of -1, 0 and +1.
   * @return The sign.
   */
  public DoubleVector sign()
  {
    DoubleVector s = new DoubleVector( size() );
    for( int i = 0; i < size(); i++ ) {
      if( V[i] > 0 ) s.V[i] = 1;
      else if( V[i] < 0 ) s.V[i] = -1;
      else s.V[i] = 0;
    }
    return s;
  }

  /** Returns the sum of all elements in the vector.
   *  @return The result.
   */
  public double  sum()
  {
    double s = 0;
    for( int i=0; i< size(); i++) s += V[i];
    return s;
  }

  /** Returns the squared sum of all elements in the vector.
   *  @return The result.
   */
  public double  sum2()
  {
    double s2 = 0;
    for( int i=0; i< size(); i++) s2 += V[i] * V[i];
    return s2;
  }

  /** Returns the L1-norm of the vector
   *  @return The L1 norm.
   */
  public double norm1()
  {
    double s = 0;
    for( int i=0; i< size(); i++) s += Math.abs(V[i]);
    return s;
  }

  /** Returns the L2-norm of the vector.
   * @return The norm, sqrt(x^2+...).
   */
  public double norm2()
  {
    return Math.sqrt( sum2() );
  }

  /** Returns ||u-v||^2.
   *  @param v the second vector.
   *  @return The result as a new vector, without modifying the current instance.
   */
  public double sum2( DoubleVector v )
  {
    return minus( v ).sum2();
  }

  /** Returns a subvector.
   *  @param i0   the index of the first element.
   *  @param i1   the index of the last element.
   *  @return     v[i0:i1].
   */
  public DoubleVector  subvector( int i0, int i1 )
  {
    DoubleVector v = new DoubleVector( i1-i0+1 );
    v.set(0, i1 - i0, this, i0);
    return v;
  }

  /** Adds a value to all the elements
   *  @param x the value
   *  @return The result as a new vector, without modifying the current instance.
   */
  public DoubleVector  plus ( double x ) {
    return copy().plusEquals( x );
  }

  /** Adds a value to all the elements in place
   *  @param x the value.
   *  @return The same vector of the instance.
   */
  public DoubleVector plusEquals ( double x ) {
    for( int i = 0; i < size(); i++ )
      V[i] += x;
    return this;
  }

  /**
   *  Adds another vector element by element
   *  @param v the second vector
   *  @return The result as a new vector, without modifying the current instance.
   */
  public DoubleVector  plus( DoubleVector v ) {
    return copy().plusEquals( v );
  }

  /**
   *  Adds another vector in place element by element
   *  @param v the second vector
   *  @return The same vector of the instance.
   */
  public DoubleVector  plusEquals( DoubleVector v ) {
    for(int i = 0; i < size(); i++ )
      V[i] += v.V[i];
    return this;
  }

  /**
   *  Subtracts a value
   *  @param x the value
   *  @return The result as a new vector, without modifying the current instance.
   */
  public DoubleVector  minus( double x ) {
    return plus( -x );
  }

  /**
   *  Subtracts a value in place
   *  @param x the value
   *  @return The same vector of the instance.
   */
  public DoubleVector  minusEquals( double x ) {
    plusEquals( -x );
    return this;
  }

  /**
   *  Subtracts another DoubleVector element by element .
   *  @param v the second DoubleVector.
   *  @return The result as a new vector, without modifying the current instance.
   */
  public DoubleVector  minus( DoubleVector v ) {
    return copy().minusEquals( v );
  }

  /**
   *  Subtracts another DoubleVector element by element in place.
   *  @param v the second DoubleVector.
   *  @return The same vector of the instance.
   */
  public DoubleVector  minusEquals( DoubleVector v ) {
    for(int i = 0; i < size(); i++ )
      V[i] -=  v.V[i];
    return this;
  }

  /** Multiplies by a scalar
      @param s    scalar
      @return     s * v
  */
  public DoubleVector  times( double s ) {
    return copy().timesEquals( s );
  }

  /** Multiply a vector by a scalar in place, u = s * u
   *  @param s    scalar
   *  @return     replace u by s * u
  */
  public DoubleVector  timesEquals( double s ) {
    for (int i = 0; i < size(); i++) {
      V[i] *= s;
    }
    return this;
  }

  /**
   *  Multiplies another DoubleVector element by element
   *  @param v the second DoubleVector
   *  @return The result as a new vector, without modifying the current instance.
   */
  public DoubleVector  times( DoubleVector v ) {
    return copy().timesEquals( v );

  }

  /**
   *  Multiplies another DoubleVector element by element in place
   *  @param v the second DoubleVector
   *  @return The same vector of the instance.
   */
  public DoubleVector  timesEquals( DoubleVector v ) {
    for(int i = 0; i < size(); i++ )
      V[i] *= v.V[i];
    return this;
  }

  /**
   *  Divided by another DoubleVector element by element
   *  @param v the second DoubleVector
   *  @return The result as a new vector, without modifying the current instance.
   */
  public DoubleVector  dividedBy ( DoubleVector v ) {
    return copy().dividedByEquals( v );
  }

  /**
   *  Divided by another DoubleVector element by element in place
   *  @param v the second DoubleVector
   *  @return The same vector of the instance.
   */
  public DoubleVector  dividedByEquals ( DoubleVector v ) {
    for( int i = 0; i < size(); i++ ) {
      V[i] /= v.V[i];
    }
    return this;
  }

  /**
   *  Checks if it is an empty vector.
   *  @return True if size() == 0, or false.
   */
  public boolean  isEmpty() {
    if( size() == 0 ) return true;
    return false;
  }

  /**
   * Returns a vector that stores the cumulated values of the original
   * vector.
   *  @return The result as a new vector, without modifying the current instance.
   */
  public DoubleVector cumulate()
  {
    return copy().cumulateInPlace();
  }

  /**
   * Cumulates the original vector in place
   *  @return The result as a new vector, without modifying the current instance.
   */
  public DoubleVector cumulateInPlace()
  {
    for (int i = 1; i < size(); i++) {
      V[i] += V[i-1];
    }
    return this;
  }

  /**
   * Returns the index of the maximum. <p>
   * If multiple maximums exist, the index of the first is returned.
   * @return Index of maximum.
   */
  public int  indexOfMax()
  {
    int index = 0;
    double ma = V[0];

    for( int i = 1; i < size(); i++ ){
      if( ma < V[i] ) {
	ma = V[i];
	index = i;
      }
    }
    return index;
  }


  /**
   * Returns true if vector not sorted.
   * @return True or false.
   */
  public boolean unsorted () {
    if( size() < 2 ) return false;
    for( int i = 1; i < size(); i++ ) {
      if( V[i-1] > V[i] )
	return true;
    }
    return false;
  }

  /**
   *  Combine two vectors together.
   *  @param v the second vector.
   *  @return The result as a new vector, without modifying the current instance.
   */
  public DoubleVector  cat( DoubleVector v ) {
    DoubleVector w = new DoubleVector( size() + v.size() );
    w.set(0, size() - 1, this, 0);
    w.set(size(), size() + v.size()-1, v, 0);
    return w;
  }

  /**
   *  Swaps the values stored at i and j.
   *  @param i the index i.
   *  @param j the index j.
   */
  public void  swap( int i, int j ){
    if( i == j ) return;
    double t = V[i];
    V[i] = V[j];
    V[j] = t;
  }

  /**
   * Returns the maximum value of all elements
   * @return Maximum value.
   * @throws JPARSECException If the size of the vector is 0.
   */
  public double max () throws JPARSECException {
    if( size() < 1 ) throw new JPARSECException("zero size");
    double ma = V[0];
    if( size() < 2 ) return ma;
    for( int i = 1; i < size(); i++ ) {
      if( V[i] > ma ) ma = V[i];
    }
    return ma;
  }


  /**
   *  Applies a method to the vector
   *  @param className the class name
   *  @param method the method
   *  @return The new vector.
   * @throws JPARSECException If an error occurs.
   */
  public DoubleVector map( String className, String method ) throws JPARSECException {
    try {
      Class c = Class.forName( className );
      Class [] cs = new Class[1];
      cs[ 0 ] = Double.TYPE;
      Method m = c.getMethod( method, cs );

      DoubleVector w = new DoubleVector( size() );
      Object [] obj = new Object[1];
      for( int i = 0; i < size(); i++ ) {
		obj[0] = new Double( V[i] );
		w.set( i, DataSet.parseDouble(m.invoke( null, obj ).toString()) );
      }
      return w;
    }
    catch ( Exception e ) {
    	throw new JPARSECException(e);
    }
  }

  /**
   * Returns the reverse vector.
   * @return Reverse vector.
   */
  public DoubleVector  rev() {
    int n = size();
    DoubleVector w = new DoubleVector( n );
    for(int i = 0; i < n; i++ )
      w.V[i] = V[n-i-1];
    return w;
  }

  /**
   * Returns a random vector of uniform distribution
   * @param n the size of the vector
   * @return A random vector.
   */
  public static DoubleVector  random( int n ) {
    DoubleVector v = new DoubleVector( n );
    for (int i = 0; i < n; i++) {
      v.V[i] = Math.random();
    }
    return v;
  }

  /**
   * Checks if this vector is equals to another.
   * @param v The other vector.
   * @return True is they have the same components,
   * false otherwise.
   */
	@Override
  public boolean equals(Object v) {
    if (this == v) return true;
    if (!(v instanceof DoubleVector)) return false;

    DoubleVector that = (DoubleVector) v;

    if (sizeOfVector != that.sizeOfVector) return false;
    return Arrays.equals(V, that.V);
  }

	@Override
  public int hashCode() {
    int result = V != null ? Arrays.hashCode(V) : 0;
    result = 31 * result + sizeOfVector;
    return result;
  }

  /**
   * Returns a new vector which is the cross product of current
   * vector with another one. They must be 3d vectors.
   * @param v The second vector.
   * @return The cross product.
   * @throws JPARSECException If the size of the vectors is not 3.
   */
  public DoubleVector crossProduct(DoubleVector v) throws JPARSECException {
	  if (v.size() != 3 || this.size() != 3) throw new JPARSECException("The size of the vectors must be 3.");
	  double crossProduct[] = new double[3];
	  double v0[] = this.V;
	  double v1[] = v.V;
	  crossProduct[0] = v0[1] * v1[2] - v0[2] * v1[1];
	  crossProduct[1] = v0[2] * v1[0] - v0[0] * v1[2];
	  crossProduct[2] = v0[0] * v1[1] - v0[1] * v1[0];
	  return new DoubleVector(crossProduct);
  }

  /**
   * Returns a string representation of the vector.
   * @return The string.
   */
	@Override
  public String toString() {
	  return DataSet.toString(DataSet.toStringValues(V), ", ");
  }

  /**
   * Returns an array with a given component of a set of double vectors.
   * @param v The set of vectors.
   * @param c A given component, starting with 0 (first).
   * @return The array of double values with that component in each
   * of the vectors.
   */
  public static double[] getAGivenComponent(DoubleVector v[], int c) {
	  double out[] = new double[v.length];
	  for (int i=0; i<v.length; i++) {
		  out[i] = v[i].get(c);
	  }
	  return out;
  }

  /**
   * Returns the unitary vector (same direction, norm2 = 1.0).
   * @return The unitary vector. In case norm2 == 0 null is returned.
   */
  public DoubleVector getUnitary() {
	  double n2 = this.norm2();
	  if (n2 == 0.0) return null;
	  DoubleVector dv = this.clone().times(1.0 / n2);
	  return dv;
  }
}
