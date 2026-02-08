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

import java.io.*;
import java.util.*;

import jparsec.util.*;
import jparsec.util.Logger.LEVEL;

/**
 * A class to read and write serializable objects using static methods.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Serialization
{
	// private constructor so that this class cannot be instantiated.
	private Serialization() {}

	/**
	 * ID for an invalid serial version.
	 */
	public static final long INVALID_SUID = -1;

	/**
	 * Obtain the serial version of a class.
	 * @param classPath Path for the class.
	 * @return Its serial version.
	 * @throws JPARSECException If an error occurs.
	 */
	public static long getSerialVersionUID(String classPath)
	throws JPARSECException {
		long theSUID = INVALID_SUID;
		try {
			ObjectStreamClass myObject = ObjectStreamClass.lookup(
				Class.forName( classPath ) );
			theSUID = myObject.getSerialVersionUID();
		} catch (Exception e)
		{
			throw new JPARSECException ("error obtaining serial version.", e);
		}

		return theSUID;
	}

	/**
	 * Obtain the serial version of an already serialized object.
	 * @param is Input stream to a given serialized object. The
	 * next object in the stream is read and the UID returned. In
	 * case mark is supported the read position in the stream is
	 * reset so that the same object can be read later.
	 * @return Its serial version.
	 * @throws JPARSECException If an error occurs.
	 */
	public static long getSerialVersionUID(InputStream is)
	throws JPARSECException {
		long theSUID = INVALID_SUID;
		try {
			int readlimit = 1024 * 1024 * 1024;
			if (is.markSupported()) is.mark(readlimit);
			ObjectInputStream ois = new getUID(is);
			ois.readObject();
			theSUID = ((getUID)ois).serialVersion;
			if (is.markSupported()) is.reset();
			ois.close();
		} catch (Exception e) {
			throw new JPARSECException ("error obtaining serial version.", e);
		}

		return theSUID;
	}

	/**
	 * Writes an object using serialization.
	 * @param serialObject A serializable object.
	 * @param filePath Path for the file.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void writeObject(Object serialObject, String filePath)
	throws JPARSECException {
		if (serialObject instanceof String) {
			WriteFile.writeAnyExternalFile(filePath, (String) serialObject);
			return;
		}

		try {
			//	  Create a stream for writing.
		    FileOutputStream fos = new FileOutputStream( filePath );

		    ObjectOutputStream outStream =
		        new ObjectOutputStream( fos );

		    //  Save each object.
		    outStream.writeObject( serialObject );

		    //  Finally, we call the flush() method for our object, which forces the data to
		    //  get written to the stream:
		    outStream.flush();
		    outStream.close();
		    fos.close();
		} catch (Exception e)
		{
			throw new JPARSECException ("error writting serial object.", e);
		}
	}

	/**
	 * Reads an object using serialization.
	 * @param filePath Path for the file.
	 * @return The object.
	 * @throws JPARSECException If an error occurs.
	 */
	public static Object readObject(String filePath)
	throws JPARSECException {
		try {
			 //  Create a stream for reading.
		      FileInputStream fis = new FileInputStream( filePath );

		      //  Next, create an object that can read from that file.
		      ObjectInputStream inStream = new ObjectInputStream( fis );

		      // Retrieve the Serializable object.
		      Object serialObject = inStream.readObject();

		      inStream.close();
		      fis.close();
		      return serialObject;
		} catch (Exception e)
		{
			throw new JPARSECException ("error reading serial object.", e);
		}
	}

	/**
	 * Writes several objects using serialization.
	 * @param serialObject A serializable object array.
	 * @param filePath Path for the file.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void writeObjects(Object[] serialObject, String filePath)
	throws JPARSECException {
		int i = -1;
		try {
			//	  Create a stream for writing.
		    FileOutputStream fos = new FileOutputStream( filePath );

		    ObjectOutputStream outStream =
		        new ObjectOutputStream( fos );

		    //  Save each object.
		    for (i=0; i<serialObject.length; i++)
		    {
		    	outStream.writeObject( serialObject[i] );
		    }
		    outStream.writeObject("NO_MORE_OBJECTS");

		    //  Finally, we call the flush() method for our object, which forces the data to
		    //  get written to the stream:
		    outStream.flush();
		    outStream.close();
		    fos.close();
		} catch (Exception e)
		{
			throw new JPARSECException ("error writting serial object #"+(i+1)+" ("+serialObject[i].getClass()+").", e);
		}
	}

	/**
	 * Writes several objects using serialization.
	 * @param serialObject A serializable object array.
	 * @param filePath Path for the file.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void writeObjects(ArrayList<Object[]> serialObject, String filePath)
	throws JPARSECException {
		int i = -1;
		try {
			//	  Create a stream for writing.
		    FileOutputStream fos = new FileOutputStream( filePath );

		    ObjectOutputStream outStream =
		        new ObjectOutputStream( fos );

		    //  Save each object.
		    for (i=0; i<serialObject.size(); i++)
		    {
		    	outStream.writeObject( serialObject.get(i) );
		    }
		    outStream.writeObject("NO_MORE_OBJECTS");

		    //  Finally, we call the flush() method for our object, which forces the data to
		    //  get written to the stream:
		    outStream.flush();
		    outStream.close();
		    fos.close();
		} catch (Exception e)
		{
			throw new JPARSECException ("error writting serial object "+(i+1)+" ("+serialObject.get(i).getClass()+").", e);
		}
	}

	/**
	 * Reads several objects using serialization.
	 * @param filePath Path for the file.
	 * @return The object array.
	 * @throws JPARSECException If an error occurs.
	 */
	public static Object[] readObjects(String filePath)
	throws JPARSECException {
		ArrayList<Object> v = new ArrayList<Object>();
		try {
			 //  Create a stream for reading.
		      FileInputStream fis = new FileInputStream( filePath );

		      //  Next, create an object that can read from that file.
		      ObjectInputStream inStream = new ObjectInputStream( fis );

		      boolean moreObjects = true;
		      do {
		    	  // Retrieve the Serializable object.
		    	  Object serialObject = inStream.readObject();
		    	  if (serialObject instanceof String && "NO_MORE_OBJECTS".equals(serialObject)) {
		    		  moreObjects = false;
		    	  } else {
		    		  v.add(serialObject);
		    	  }
		      } while (moreObjects);

		      inStream.close();
		      fis.close();
		} catch (Exception e)
		{
			throw new JPARSECException ("error reading serial object "+(v.size()+1)+".", e);
		}

		return v.toArray();
	}

	/**
	 * Reads several objects using serialization.
	 * @param s Stream to the input file.
	 * @return The object array.
	 * @throws JPARSECException If an error occurs.
	 */
	public static Object[] readObjects(InputStream s)
	throws JPARSECException {
		ArrayList<Object> v = new ArrayList<Object>();
		try {
		      //  Next, create an object that can read from that file.
		      ObjectInputStream inStream = new ObjectInputStream( s );

		      boolean moreObjects = true;
		      do {
		    	  // Retrieve the Serializable object.
		    	  Object serialObject = inStream.readObject();
		    	  if (serialObject instanceof String && "NO_MORE_OBJECTS".equals(serialObject)) {
		    		  moreObjects = false;
		    	  } else {
		    		  v.add(serialObject);
		    	  }
		      } while (moreObjects);

		      inStream.close();
		} catch (Exception e)
		{
			throw new JPARSECException ("error reading serial object "+(v.size()+1)+".", e);
		}

		return v.toArray();
	}

	/**
	 * Reads several objects using serialization.
	 * @param filePath Path for the file.
	 * @return The object array.
	 * @throws JPARSECException If an error occurs.
	 */
	public static ArrayList readObjectsAsList(String filePath)
	throws JPARSECException {
		ArrayList v = new ArrayList();
		try {
			 //  Create a stream for reading.
		      FileInputStream fis = new FileInputStream( filePath );

		      //  Next, create an object that can read from that file.
		      ObjectInputStream inStream = new ObjectInputStream( fis );

		      boolean moreObjects = true;
		      do {
		    	  // Retrieve the Serializable object.
		    	  Object serialObject = inStream.readObject();
		    	  if (serialObject instanceof String && "NO_MORE_OBJECTS".equals(serialObject)) {
		    		  moreObjects = false;
		    	  } else {
		    		  v.add(serialObject);
		    	  }
		      } while (moreObjects);

		      inStream.close();
		      fis.close();
		} catch (Exception e)
		{
			throw new JPARSECException ("error reading serial object "+(v.size()+1)+".", e);
		}

		return v;
	}
	
	/**
	 * Reads several objects using serialization, skipping possible errors.
	 * Should not be used, since it could hang the process.
	 * @param filePath Path for the file.
	 * @param skipIndexes Indexes (starting from 0) of the objects that will be skipped, the other will
	 * be read.
	 * @return The object array. Indexes of the skipped objects will contain null, so the length
	 * will be the same as the number of objects in the input file.
	 * @throws JPARSECException If an error occurs.
	 */
	public static Object[] readObjectsSkipErrors(String filePath, int[] skipIndexes)
	throws Exception {
		ArrayList<Object> v = new ArrayList<Object>();
		int n = 0;
		try {
			 //  Create a stream for reading.
		      FileInputStream fis = new FileInputStream( filePath );

		      //  Next, create an object that can read from that file.
		      ObjectInputStream inStream = new ObjectInputStream( fis );

		      boolean moreObjects = true;
		      do {
		    	  // Retrieve the Serializable object.
		    	  Object serialObject = null;
		    	  boolean skip = false;
		    	  for (int i=0;i<skipIndexes.length;i++) {
		    		  if (skipIndexes[i] == n) {
		    			  skip = true;
		    			  break;
		    		  }
		    	  }
		    	  if (skip) {
		    		  try { serialObject = inStream.readObject(); } catch (Exception exc) { serialObject = null; }
		    	  } else {
			    	  serialObject = inStream.readObject();
		    	  }
		    	  if (serialObject != null && serialObject instanceof String && "NO_MORE_OBJECTS".equals(serialObject)) {
		    		  moreObjects = false;
		    	  } else {
		    		  v.add(serialObject);
		    	  }
		    	  n++;
		      } while (moreObjects);

		      inStream.close();
		      fis.close();
		} catch (Exception e)
		{
			throw new JPARSECException ("error reading serial object "+(n+1)+".", e);
		}

		return v.toArray();
	}

	/**
	 * Reads several objects using serialization.
	 * @param filePath Path for the file.
	 * @param nObj Number of objects to read.
	 * @return The object array.
	 * @throws JPARSECException If an error occurs.
	 */
	public static Object[] readObjects(String filePath, int nObj)
	throws JPARSECException {
		ArrayList<Object> v = new ArrayList<Object>();
		int n = 0;
		try {
			 //  Create a stream for reading.
		      FileInputStream fis = new FileInputStream( filePath );

		      //  Next, create an object that can read from that file.
		      ObjectInputStream inStream = new ObjectInputStream( fis );

		      boolean moreObjects = true;
		      do {
		    	  // Retrieve the Serializable object.
		    	  Object serialObject = inStream.readObject();
		    	  n++;
		    	  if (serialObject instanceof String && "NO_MORE_OBJECTS".equals(serialObject)) {
		    		  moreObjects = false;
		    	  } else {
		    		  v.add(serialObject);
		    	  }
		      } while (moreObjects && n<nObj);

		      inStream.close();
		      fis.close();
		} catch (Exception e)
		{
			throw new JPARSECException ("error reading serial object "+(n+1)+".", e);
		}

		return v.toArray();
	}

	/**
     * Returns a copy of the object, or null if the object cannot
     * be serialized.
     * @param orig Original object.
     * @return The copy, or null if it is not serializable.
     */
    public static Object copy(Object orig) {
        Object obj = null;
        try {
            // Write the object out to a byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(orig);
            out.flush();
            out.close();
            bos.close();

            // Make an input stream from the byte array and read
            // a copy of the object back in.
            ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(bos.toByteArray()));
            obj = in.readObject();
            in.close();
        }
        catch(IOException e) {
        	Logger.log(LEVEL.ERROR, "Could not copy this object through serialization. Details: "+e.getLocalizedMessage());
        }
        catch(ClassNotFoundException cnfe) {
        	Logger.log(LEVEL.ERROR, "Could not copy this object through serialization. Details: "+cnfe.getLocalizedMessage());
        }
        return obj;
    }
}

class getUID extends ObjectInputStream {

	/** Class name. */
	public String name;
	/** Serial version. */
	public long serialVersion;

	/**
	 * Constructor for an input stream.
	 * @param in The stream.
	 * @throws IOException
	 */
	public getUID(InputStream in) throws IOException {
		super(in);
	}

	@Override
	protected ObjectStreamClass readClassDescriptor() throws IOException,
		ClassNotFoundException {
		ObjectStreamClass descriptor = super.readClassDescriptor();
		name = descriptor.getName();
		serialVersion = descriptor.getSerialVersionUID();
	    return descriptor;
	}
}
