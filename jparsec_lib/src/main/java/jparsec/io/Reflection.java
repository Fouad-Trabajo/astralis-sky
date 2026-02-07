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
package jparsec.io;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import jparsec.util.JPARSECException;


/**
 * This class is designed to allow using reflection to call functions in other methods recusively
 * and automatically. It can also copy the values of the fields from one object to another.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Reflection {

	private Reflection() { }

	/**
	 * Calls an static method with one parameter (a double) and that returns also a double,
	 * for all values in a given array.
	 * @param classAndMethod Class and method to call, for instance jparsec.astronomy.Star.getDistance.
	 * It must be a static method requiring one double parameter.
	 * @param v The array of values.
	 * @return The array of output values, out[i] = class.method(v[i]).
	 * @throws JPARSECException If an error occurs.
	 */
	public static double[] applySimpleStaticMethod(String classAndMethod, double v[]) throws JPARSECException {
		try
		{
			if (classAndMethod == null) throw new JPARSECException("Input class is null!");
			String src = classAndMethod.substring(0, classAndMethod.lastIndexOf("."));
			String method = classAndMethod.substring(classAndMethod.lastIndexOf(".") + 1);
			Class srcC = Class.forName(src);
			Method m = srcC.getDeclaredMethod(method, new Class[] {Double.TYPE});
			double out[] = new double[v.length];
			for (int i=0; i<out.length; i++) {
				out[i] = (Double) m.invoke(null, v[i]);
			}
			return out;
		} catch (Exception e)
		{
			throw new JPARSECException("The process failed.", e);
		}
	}

	/**
	 * Calls an static method with one parameter (a double) and that returns also a double,
	 * for all values in a given array.
	 * @param c The Class.
	 * @param method Method name. It must be a static method of the Class c requiring one
	 * double parameter.
	 * @param v The array of values.
	 * @return The array of output values, out[i] = class.method(v[i]).
	 * @throws JPARSECException If an error occurs.
	 */
	public static double[] applySimpleStaticMethod(Class c, String method, double v[]) throws JPARSECException {
		try
		{
			if (c == null || method == null) throw new JPARSECException("Input class/method is null!");
			Method m = c.getDeclaredMethod(method, new Class[] {Double.TYPE});
			double out[] = new double[v.length];
			for (int i=0; i<out.length; i++) {
				out[i] = (Double) m.invoke(null, v[i]);
			}
			return out;
		} catch (Exception e)
		{
			throw new JPARSECException("The process failed.", e);
		}
	}

	/**
	 * Calls a method with one variable parameter, returning one value for each of the values
	 * allowed for that parameter. The rest of the parameters of the method are kept as constants.
	 * @param instance The object instance.
	 * @param method The name of the method to call.
	 * @param methodValues The input values for the method. One of them will be the one to modify,
	 * the others will be kept to the input values set here.
	 * @param v The set of values to use for certain input parameters.
	 * @param index The index of the parameter that will be modified using the previous array.
	 * @return The set of output values for each of the values set using the previous input
	 * parameters.
	 * @throws JPARSECException If an error occurs.
	 */
	public static Object[] applyGenericMethod(Object instance, String method, Object methodValues[], Object v[], int index) throws JPARSECException {
		try
		{
			if (instance == null || method == null) throw new JPARSECException("Null instance or method!");
			Class srcC = null;
			if (instance instanceof Class) {
				srcC = (Class) instance;
			} else {
				srcC = instance.getClass();
			}
			Class objClass[] = new Class[methodValues.length];
			for (int i=0; i<methodValues.length; i++) {
				objClass[i] = methodValues[i].getClass();
				if (objClass[i] == Double.class) objClass[i] = Double.TYPE;
				if (objClass[i] == Boolean.class) objClass[i] = Boolean.TYPE;
				if (objClass[i] == Byte.class) objClass[i] = Byte.TYPE;
				if (objClass[i] == Short.class) objClass[i] = Short.TYPE;
				if (objClass[i] == Integer.class) objClass[i] = Integer.TYPE;
				if (objClass[i] == Long.class) objClass[i] = Long.TYPE;
				if (objClass[i] == Float.class) objClass[i] = Float.TYPE;
				if (objClass[i] == Character.class) objClass[i] = Character.TYPE;
			}
			Method m = srcC.getDeclaredMethod(method, objClass);
			Object out[] = new Object[v.length];
			for (int i=0; i<out.length; i++) {
				methodValues[index] = v[i];
				out[i] = m.invoke(instance, methodValues);
			}
			return out;
		} catch (Exception e) {
			throw new JPARSECException("The process failed.", e);
		}
	}

	/**
	 * Clones the values of all the fields, setting the same
	 * values in the destination object as those in the source
	 * object. This process will include assigning the values
	 * of private fields, but not static, final, abstract,
	 * interface, or native.
	 * @param src Source object.
	 * @param dest Destination object.
	 * @throws JPARSECException If an error occurs in the process.
	 */
	public static void copyFields(Object src, Object dest) throws JPARSECException {
		try
		{
			if (src == null) throw new JPARSECException("Source object is null!");
			Class srcC = src.getClass();
			if (dest == null) {
				Constructor c[] = srcC.getConstructors();
				dest = c[0].newInstance();
			}
			Class destC = dest.getClass();
			if (!srcC.getName().equals(destC.getName())) throw new JPARSECException("Source and destination objects must be instances of the same class.");
			if (!srcC.getCanonicalName().equals(destC.getCanonicalName())) throw new JPARSECException("Source and destination objects must be instances of the same class.");
			if (!srcC.getPackage().equals(destC.getPackage())) throw new JPARSECException("Source and destination objects must be instances of the same class.");

			Field srcF[] = srcC.getDeclaredFields();
			Field destF[] = destC.getDeclaredFields();
			if (srcF.length != destF.length) throw new JPARSECException("Source and destination objects must be instances of the same class.");

			for (int i=0; i<srcF.length; i++) {
				int index = i;
				String srcFi = srcF[i].getName();
				if (!srcFi.equals(destF[index].getName())) {
					index = -1;
					for (int j=0; j<destF.length; j++) {
						if (destF[j].getName().equals(srcFi)) {
							index = j;
							break;
						}
					}
					if (index < 0) throw new JPARSECException("Source and destination objects must be instances of the same class.");
				}

				int mod = destF[index].getModifiers();
				if (Modifier.isPrivate(mod)) {
					destF[index].setAccessible(true);
					srcF[i].setAccessible(true);
				}
				if (!Modifier.isStatic(mod) && !Modifier.isFinal(mod) && !Modifier.isAbstract(mod) && !Modifier.isInterface(mod) &&
						!Modifier.isNative(mod)) {
					destF[index].set(dest, srcF[i].get(src));
				}
			}
		} catch (Exception e)
		{
			throw new JPARSECException("The field copy process failed.", e);
		}
	}
}
