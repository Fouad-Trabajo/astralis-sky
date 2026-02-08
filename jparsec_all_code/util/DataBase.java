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
package jparsec.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import jparsec.graph.DataSet;
import jparsec.io.ApplicationLauncher;
import jparsec.io.FileIO;
import jparsec.io.Serialization;

/**
 * A basic abstraction of a database to hold objects in memory or in disk.
 * <P>
 * This class holds objects in memory or serialized in files. Data is identified by a String that
 * holds the data identifier and the process id. This String is used as the name for the serialized
 * file for slow performance but unlimited memory mode, very useful for Android. The use of a process
 * id allows also to access data in a thread-safe way. As the identifier of the thread the thread's name
 * is used, as returned by {@linkplain ApplicationLauncher#getProcessID()}.
 * <BR><BR>
 * In JPARSEC all information is kept in memory for better performance.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class DataBase
{
	// private constructor so that this class cannot be instantiated.
	private DataBase() {}

	private static ArrayList<String> threads = new ArrayList<String>();
	private static ArrayList<String> dataID = new ArrayList<String>(); // Memory
	private static ArrayList<String> dataID_disk = new ArrayList<String>(); // Disk
	private static ArrayList<Object> data = new ArrayList<Object>(); // Memory
	private static HashMap<String, Object> data_lifeTime = new HashMap<String, Object>(); // Memory
	private static String cacheDir = null;

	/**
	 * Adds data to the database. Process id is determined using
	 * {@linkplain ApplicationLauncher#getProcessID()}.
	 * @param id The identifier of the data.
	 * @param o The set of objects.
	 * @param forceMemory True to force the use of the memory instead of disk.
	 * Useful to store little objects used frequently, that would slow down
	 * the execution if the are continuously accessed through disk.
	 * @return The identifier of the thread writing this data.
	 */
	public static String addData(String id, Object o, boolean forceMemory) {
		return addData(id, ApplicationLauncher.getProcessID(), o, forceMemory);
	}
	/**
	 * Adds data to database. In case the data already exists, it is
	 * replaced with the new data.
	 * @param id The identifier of the data.
	 * @param pid The process id, should be identical to
	 * {@linkplain ApplicationLauncher#getProcessID()}. You can set it here
	 * since the mentioned method uses reflection and it is slow. pid can
	 * also be null for static objects (for all process).
	 * @param o The set of objects.
	 * @param forceMemory True to force the use of the memory instead of disk.
	 * Useful to store little objects used frequently, that would slow down
	 * the execution if the are continuously accessed through disk.
	 * @return The identifier of the thread writing this data.
	 */
	public static String addData(String id, String pid, Object o, boolean forceMemory) {
		return DataBase.addData(id, pid, o, forceMemory, 0);
	}

	/**
	 * Adds data to database. In case the data already exists, it is
	 * replaced with the new data.
	 * @param id The identifier of the data.
	 * @param pid The process id, should be identical to
	 * {@linkplain ApplicationLauncher#getProcessID()}. You can set it here
	 * since the mentioned method uses reflection and it is slow. pid can
	 * also be null for static objects (for all process).
	 * @param o The set of objects.
	 * @param forceMemory True to force the use of the memory instead of disk.
	 * Useful to store little objects used frequently, that would slow down
	 * the execution if the are continuously accessed through disk.
	 * @param lifeTimeSeconds Life time of the data in seconds. In case data
	 * is requested after this period, it will be deleted and null would be
	 * returned. Set to 0 or negative for infinity life time.
	 * @return The identifier of the thread writing this data.
	 */
	public static synchronized String addData(String id, String pid, Object o, boolean forceMemory, int lifeTimeSeconds) {
		if (pid == null) pid = "";

		int index = threads.indexOf(pid);
		if (index == -1) threads.add(pid);

		String dataid = id+"_"+pid;

		if (lifeTimeSeconds > 0) {
			data_lifeTime.put(dataid, new long[] {lifeTimeSeconds, System.currentTimeMillis()});
			if (gc == null || !gc.isAlive()) {
				gc = new Thread(new gcThread());
				gc.start();
			}
		}

		if (forceMemory) {
			index = dataID.indexOf(dataid);
			boolean newData = false;
			if (index == -1) {
				index = dataID.size();
				dataID.add(dataid);
				newData = true;
			}

//			if (o != null) System.out.println("added "+o.getClass()+" ("+dataid+")");
			if (newData) {
				data.add(o);
			} else {
				data.set(index, o);
			}
			return pid;
		}

		try {
			if (cacheDir == null) cacheDir = FileIO.getTemporalDirectory();
			String of = cacheDir + dataid;

			index = dataID.indexOf(of);
			if (index == -1) dataID_disk.add(of);

			Serialization.writeObject(o, of);
			File file = new File(of);
			file.deleteOnExit();
		} catch (Exception exc) {
			exc.printStackTrace();
		}
		return pid;
	}

	/**
	 * Returns data from the database. Process id is determined using
	 * {@linkplain ApplicationLauncher#getProcessID()}.
	 * @param id The identifier of the data.
	 * @param forceMemory True to force the use of the memory instead of disk.
	 * Useful to store little objects used frequently, that would slow down
	 * the execution if the are continuously accessed through disk.
	 * @return The set of objects, or null if none can be found.
	 */
	public static Object getData(String id, boolean forceMemory) {
		return getData(id, ApplicationLauncher.getProcessID(), forceMemory);
	}
	/**
	 * Returns data from the database. A search is performed over all existent
	 * process ids until data is found.
	 * @param id The identifier of the data.
	 * @param forceMemory True to force the use of the memory instead of disk.
	 * Useful to store little objects used frequently, that would slow down
	 * the execution if the are continuously accessed through disk.
	 * @return The set of objects, or null if none can be found.
	 */
	public static Object getDataForAnyThread(String id, boolean forceMemory) {
		for (int i=0; i<threads.size(); i++) {
			Object o = getData(id, threads.get(i), forceMemory);
			if (o != null) return o;
		}
		return null;
	}

	/**
	 * Returns if a given identifier holds data in the database.
	 * @param id The identifier for the data.
	 * @param anyThread True to scan all thread, false for the one
	 * calling this method.
	 * @return True or false.
	 */
	public static boolean dataExists(String id, boolean anyThread) {
		if (anyThread) {
			for (int i=0; i<threads.size(); i++) {
				Object o = getData(id, threads.get(i), true);
				if (o == null) o = getData(id, threads.get(i), false);
				if (o != null) return true;
			}
		} else {
			Object o = getData(id, true);
			if (o == null) o = getData(id, false);
			if (o != null) return true;
		}
		return false;
	}

	/**
	 * Returns if a given identifier holds data in the database.
	 * @param id The identifier for the data.
	 * @param threadName Thread name.
	 * @param inMemory True to check in memory, false to check on disk cache.
	 * @return True or false.
	 */
	public static boolean dataExists(String id, String threadName, boolean inMemory) {
		Object o = getData(id, threadName, inMemory);
		if (o != null) return true;
		return false;
	}
	/**
	 * Returns data from the database.
	 * @param id The identifier of the data.
	 * @param pid The process id, should be identical to
	 * {@linkplain ApplicationLauncher#getProcessID()}. You can set it here
	 * since the mentioned method uses reflection and it is slow. pid can
	 * also be null for static objects (for all process).
	 * @param forceMemory True to force the use of the memory instead of disk.
	 * Useful to store little objects used frequently, that would slow down
	 * the execution if the are continuously accessed through disk. THIS FLAG
	 * IS DISABLED, CURRENTLY EVERYTHING IS FORCED TO BE ON MEMORY.
	 * @return The set of objects, or null if none can be found.
	 */
	public static Object getData(String id, String pid, boolean forceMemory) {
		if (pid == null) pid = "";

		String dataid = id+"_"+pid;

		boolean deleteAndReturnNull = false;
		if (data_lifeTime.size() > 0) {
			Object o = data_lifeTime.get(dataid);
			if (o != null) {
				long data[] = (long[]) o;
				long t0 = System.currentTimeMillis();
				long t1 = data[1] + data[0] * 1000;
				if (t0 > t1) {
					deleteAndReturnNull = true;
					data_lifeTime.remove(dataid);
				}
			}
		}

		if (forceMemory) {
			int index = dataID.indexOf(dataid);
			if (index == -1) {
				return null;
			}

			if (deleteAndReturnNull) {
				data.set(index, null);
				return null;
			}

/*			if (data.get(index) != null) {
				try {
					int n = ((Object[]) data.get(index)).length;
					System.out.println("retrieved "+dataid+" / "+n+" / "+data.get(index).getClass()); //+" / "+JPARSECException.getCurrentTrace());
				} catch (Exception exc) {}
			}
*/
			return data.get(index);
		}

		String of = cacheDir + dataid;
		if (deleteAndReturnNull) {
			File file = new File(of);
			file.delete();
			return null;
		}

		try {
			return Serialization.readObject(of);
		} catch (Exception exc) {
			return null;
		}
	}

	/**
	 * Returns the data for a given index in the database, without
	 * any kind of check.
	 * @param index The index.
	 * @return The data.
	 */
	public static Object getData(int index) {
		return data.get(index);
	}

	/**
	 * Returns the index of a specific item in the database.
	 * @param id The identifier of the data.
	 * @param pid The process id, should be identical to
	 * {@linkplain ApplicationLauncher#getProcessID()}. You can set it here
	 * since the mentioned method uses reflection and it is slow. pid can
	 * also be null for static objects (for all process).
	 * @return The index for the item in the database, or -1 if it is not found.
	 */
	public static int getIndex(String id, String pid) {
		if (pid == null) pid = "";
		String dataid = id+"_"+pid;
		int index = dataID.indexOf(dataid);
		return index;
	}


	/**
	 * Sets the cache directory.
	 * @param path The path, including the latest file separator.
	 */
	public static void setCacheDirectory(String path) {
		cacheDir = path;
	}

	/**
	 * Returns the path of the cache directory.
	 * @return The path.
	 */
	public static String getCacheDirectory() {
		return cacheDir;
	}

	/**
	 * Returns the identifiers of the threads currently
	 * using data.
	 * @return The identifiers.
	 */
	public static String[] getThreads() {
		return DataSet.arrayListToStringArray(threads);
	}

	/**
	 * Removes all the data corresponding to certain thread.
	 * @param pid The thread id.
	 */
	public static synchronized void deleteThreadData(String pid) {
		int index = threads.indexOf(pid);
		if (index < 0) return;

		for (int i=0; i<dataID.size(); i++) {
			String d = dataID.get(i);
			if (d.endsWith("_"+pid)) {
				dataID.remove(i);
				data.remove(i);
			}
		}

		for (int i=0; i<dataID_disk.size(); i++) {
			String d = dataID_disk.get(i);
			if (d.endsWith("_"+pid)) {
				dataID_disk.remove(i);
				File file = new File(d);
				if (file.exists()) file.delete();
			}
		}

		threads.remove(index);
	}

	/**
	 * Returns all identifiers in the DataBase.
	 * @param inMemory True to return those in memory, false for disk-based data.
	 * @return The identifiers.
	 */
	public static String[] getAllDatabaseIdentifiers(boolean inMemory) {
		String out[] = DataSet.arrayListToStringArray(inMemory ? dataID : dataID_disk);
		for (int i=0; i<out.length; i++) {
			for (int j=0; j<threads.size(); j++) {
				if (out[i].endsWith("_"+threads.get(j))) {
					out[i] = out[i].substring(0, out[i].indexOf("_"+threads.get(j)));
					break;
				}
			}
		}
		return out;
	}
	
	/**
	 * Clears the Database entirely.
	 */
	public static void clearEntireDatabase() {
		/*
		int nnull = 0;
		String notNull = "";
		for (int i=0; i<data.size(); i++) {
			if (data.get(i) == null) {
				nnull ++;
			} else {
				notNull += dataID.get(i)+",";
			}
		}
		System.out.println("Total objects: "+data.size()+"/"+dataID.size()+", null objects: "+nnull+", non null elements "+(data.size()-nnull)+": "+notNull);
		*/

		threads = new ArrayList<String>();
		dataID = new ArrayList<String>(); // Memory
		dataID_disk = new ArrayList<String>(); // Disk
		data = new ArrayList<Object>(); // Memory
		data_lifeTime = new HashMap<String, Object>(); // Memory
		cacheDir = null;

	}

	private static Thread gc = null;
	private static class gcThread implements Runnable {
		public gcThread() { }
		public void run() {
			try {
				while(true) {
					if (data_lifeTime.size() > 0) {
						Object keys[] = data_lifeTime.keySet().toArray();
						int alive = 0;
						long sleep = 1000, minTime = -1;
						for (int i=0; i<keys.length; i++) {
							Object o = data_lifeTime.get(keys[i]);
							long data[] = (long[]) o;
							long t0 = System.currentTimeMillis();
							long lifetime = data[0] * 1000;
							long t1 = data[1] + lifetime;
							if (t0 > t1) {
								data_lifeTime.remove(keys[i]);

								int index = dataID.indexOf(keys[i]);
								if (index >= 0) {
									DataBase.data.set(index, null);
								} else {
									String of = cacheDir + keys[i];
									File file = new File(of);
									if (file.exists()) file.delete();
								}
							} else {
								if (lifetime < minTime || minTime == -1) minTime = lifetime;
								alive ++;
							}
						}
						if (alive == 0) break;
						if (minTime > sleep) sleep = minTime;
						Thread.sleep(sleep);
					} else {
						break;
					}
				}
			} catch (Exception exc) {}
		}
	}
}
