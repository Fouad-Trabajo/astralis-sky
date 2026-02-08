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
import java.util.zip.*;

import jparsec.graph.DataSet;
import jparsec.util.*;

/**
 * A class to (un)compress files using java zip support and static methods.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Zip
{
	// private constructor so that this class cannot be instantiated.
	private Zip() {}

	/**
	 * The file separator for zip files.
	 */
	public static final String ZIP_SEPARATOR = "/";

	private static final int BUFFER = 2048;
	private static final String SEPARATOR = FileIO.getFileSeparator();

	/**
	 * Unzips a given zip file and returns the list of files inside it.
	 * @param fileName File to unzip.
	 * @param destDir Destination directory of the unzipped files. Null will create no files, useful to just
	 * get the contents inside it.
	 * @param createNecessarySubdirs True to create any necessary subdirectories, false to avoid
	 * doing that, which will prevent for unzipping any file outside the already existent subdirectories.
	 * @return The set of content files in the zip file.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String[] unZipFile(String fileName, String destDir, boolean createNecessarySubdirs)
	throws JPARSECException {
		ArrayList<String> v = new ArrayList<String>();
		if (destDir != null && !destDir.endsWith(SEPARATOR)) destDir += SEPARATOR;
		 try {
	         BufferedOutputStream dest = null;
	         FileInputStream fis = new FileInputStream(new File(fileName));
	         CheckedInputStream checksum = new CheckedInputStream(fis, new Adler32());
	         ZipInputStream zis = new ZipInputStream(new BufferedInputStream(checksum));
	         ZipEntry entry;
	         while((entry = zis.getNextEntry()) != null) {
	            //System.out.println("Extracting: " +entry);
	        	v.add(entry.getName());
	            int count;
	            byte data[] = new byte[BUFFER];
	            // write the files to the disk
	            if (destDir != null) {
	        		String path = DataSet.replaceAll(entry.getName(), Zip.ZIP_SEPARATOR, SEPARATOR, true);
	            	File file = new File(destDir+path);
	            	String dir = destDir + path;
	            	dir = dir.substring(0, dir.lastIndexOf(SEPARATOR));
	            	File dirf = new File(dir);
	            	if (!dirf.exists() && createNecessarySubdirs) FileIO.createDirectories(dirf.getAbsolutePath());
	            	if (dirf.exists())
	            	{
		            	if (!file.isDirectory()) {
				            FileOutputStream fos = new FileOutputStream(file);
				            dest = new BufferedOutputStream(fos, BUFFER);
				            while ((count = zis.read(data, 0, BUFFER)) != -1) {
				               dest.write(data, 0, count);
				            }
				            dest.flush();
				            dest.close();
		            	}
	            	}
	            }
	         }
	         zis.close();
	         //System.out.println("Checksum: "+checksum.getChecksum().getValue());
	      } catch(Exception e) {
		      throw new JPARSECException("error during uncompression.", e);
	      }

	      return DataSet.arrayListToStringArray(v);
	}
	/**
	 * Unzips a given zip file and returns the contents of a file inside it.
	 * @param fileName File to unzip.
	 * @param filePath Path inside the zip file to the file to be read.
	 * @return The contents of the selected file.
	 * @throws JPARSECException If an error occurs.
	 */
	public static byte[] unZipFileGetFileInside(String fileName, String filePath)
	throws JPARSECException {
		 try {
	         BufferedOutputStream dest = null;
	         FileInputStream fis = new FileInputStream(new File(fileName));
	         CheckedInputStream checksum = new CheckedInputStream(fis, new Adler32());
	         ZipInputStream zis = new ZipInputStream(new BufferedInputStream(checksum));
	         ZipEntry entry;
	         ByteArrayOutputStream bos = null;
	         while((entry = zis.getNextEntry()) != null) {
	            //System.out.println("Extracting: " +entry);
	        	if (!entry.getName().equals(filePath)) continue;
	            int count;
	            byte data[] = new byte[BUFFER];

	            bos = new ByteArrayOutputStream();
	            dest = new BufferedOutputStream(bos, BUFFER);
	            while ((count = zis.read(data, 0, BUFFER)) != -1) {
	               dest.write(data, 0, count);
	            }
	            dest.flush();
	            dest.close();
	            bos.flush();
	         }
	         zis.close();
	         if (bos != null) return bos.toByteArray();
	         //System.out.println("Checksum: "+checksum.getChecksum().getValue());
	      } catch(Exception e) {
		      throw new JPARSECException("error during uncompression.", e);
	      }

	      return null;
	}
	/**
	 * Compress a set of files. If the file name to create has the same name and path as
	 * any of the input files to zip, that file will be skipped (not added) and overwritten.
	 * @param fileName Zip file to create.
	 * @param filesToAdd A set of files (full paths) to write.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void zipFile(String fileName, String filesToAdd[])
	throws JPARSECException {
	    try {
	    		BufferedInputStream origin = null;
	            FileOutputStream dest = new FileOutputStream(new File(fileName));

	            CheckedOutputStream checksum = new CheckedOutputStream(dest, new Adler32());
	            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(checksum));

	            //out.setMethod(ZipOutputStream.DEFLATED);
	            byte data[] = new byte[BUFFER];

	            for (int i=0; i<filesToAdd.length; i++) {
	            	if (!sameFile(filesToAdd[i], fileName)) {
	            	   File file = new File(filesToAdd[i]);
	            	   String zipPath = DataSet.replaceAll(filesToAdd[i], SEPARATOR, Zip.ZIP_SEPARATOR, true);
		               ZipEntry entry = new ZipEntry(zipPath);
		               out.putNextEntry(entry);
		               int count;
		               FileInputStream fi = new FileInputStream(file);
		               origin = new BufferedInputStream(fi, BUFFER);
		               while((count = origin.read(data, 0, BUFFER)) != -1) {
		                  out.write(data, 0, count);
		               }
		               origin.close();
	            	}
	            }
	            out.close();

		    } catch(Exception e) {
		      throw new JPARSECException("error during compression.", e);
		    }
	}

	private static void deleteFile(String path)
	{
        try {
        	FileIO.deleteFile(path);
        } catch (Exception e) {}
	}
	private static void addContents(ZipOutputStream out, String path, String folder)
	throws Exception {
        byte data[] = new byte[BUFFER];
        String files[] = FileIO.getFiles(path);
        String dirs[] = FileIO.getSubdirectories(path);
        if (files != null)
        {
	        for (int i=0; i<files.length; i++)
	        {
	        	ZipEntry entry = new ZipEntry(folder + FileIO.getLastField(files[i], SEPARATOR, true));
	            out.putNextEntry(entry);

	            int count;
	        	FileInputStream fi = new FileInputStream(files[i]);
	        	BufferedInputStream origin = new BufferedInputStream(fi, BUFFER);
	        	while((count = origin.read(data, 0, BUFFER)) != -1) {
	        		out.write(data, 0, count);
	        	}
	            origin.close();
	            fi.close();

            	deleteFile(files[i]);
	        }
        }
        if (dirs != null)
        {
	        for (int i=0; i<dirs.length; i++)
	        {
	        	ZipEntry entry = new ZipEntry(folder + FileIO.getLastField(dirs[i], SEPARATOR, true) + ZIP_SEPARATOR);
	            out.putNextEntry(entry);
	        }

	        if (dirs.length == 0) deleteFile(path);
        }

        if (dirs == null) deleteFile(path);
	}
	private static void addDirectory(String path, ZipOutputStream out)
	throws Exception {
        if (path.endsWith(SEPARATOR)) path = path.substring(0, path.length() - 1);
        String folder = FileIO.getLastField(path, SEPARATOR, true) + ZIP_SEPARATOR;
        path += SEPARATOR;
        ZipEntry entry = new ZipEntry(folder);
        out.putNextEntry(entry);

        Zip.addContents(out, path, folder);
        String dirs[] = FileIO.getSubdirectories(path);
        if (dirs != null)
        {
	        for (int i=0; i<dirs.length; i++)
	        {
	        	String newPath = path + FileIO.getLastField(dirs[i], SEPARATOR, true) + SEPARATOR;
	        	String newFolder = folder + FileIO.getLastField(dirs[i], SEPARATOR, true) + ZIP_SEPARATOR;
	        	Zip.addContents(out, newPath, newFolder);

	            String dirs2[] = FileIO.getSubdirectories(newPath);
	            if (dirs2 != null)
	            {
		            for (int j=0; j<dirs2.length; j++)
		            {
		            	String newPath2 = newPath + FileIO.getLastField(dirs2[j], SEPARATOR, true) + SEPARATOR;
		            	String newFolder2 = newFolder + FileIO.getLastField(dirs2[j], SEPARATOR, true) + ZIP_SEPARATOR;
		            	Zip.addContents(out, newPath2, newFolder2);

		                String dirs3[] = FileIO.getSubdirectories(newPath2);
			            if (dirs3 != null)
			            {
			                for (int k=0; k<dirs3.length; k++)
			                {
			                	String newPath3 = newPath2 + FileIO.getLastField(dirs3[k], SEPARATOR, true) + SEPARATOR;
			                	String newFolder3 = newFolder2 + FileIO.getLastField(dirs3[k], SEPARATOR, true) + ZIP_SEPARATOR;
			                	Zip.addContents(out, newPath3, newFolder3);

			    	            String dirs4[] = FileIO.getSubdirectories(newPath3);
			    	            if (dirs4 != null)
			    	            {
			    		            for (int l=0; l<dirs4.length; l++)
			    		            {
			    		            	String newPath4 = newPath3 + FileIO.getLastField(dirs4[l], SEPARATOR, true) + SEPARATOR;
			    		            	String newFolder4 = newFolder3 + FileIO.getLastField(dirs4[l], SEPARATOR, true) + ZIP_SEPARATOR;
			    		            	Zip.addContents(out, newPath4, newFolder4);

			    		                String dirs5[] = FileIO.getSubdirectories(newPath4);
			    			            if (dirs5 != null)
			    			            {
			    			                for (int m=0; m<dirs5.length; m++)
			    			                {
			    			                	String newPath5 = newPath4 + FileIO.getLastField(dirs5[m], SEPARATOR, true) + SEPARATOR;
			    			                	String newFolder5 = newFolder4 + FileIO.getLastField(dirs5[m], SEPARATOR, true) + ZIP_SEPARATOR;
			    			                	Zip.addContents(out, newPath5, newFolder5);
			    			                }
			    			    	        if (dirs5.length == 0) {
			    			    	        	deleteFile(newPath4);
			    			    	        	dirs4 = FileIO.getSubdirectories(newPath3);
			    			    	        }
			    			            }
			    		    	        if (dirs5 == null) {
			    		    	        	deleteFile(newPath4);
		    			    	        	dirs4 = FileIO.getSubdirectories(newPath3);
			    		    	        }
			    		            }
			    	    	        if (dirs4.length == 0) {
			    	    	        	deleteFile(newPath3);
	    			    	        	dirs3 = FileIO.getSubdirectories(newPath2);
			    	    	        }
			    	            }
	    		    	        if (dirs4 == null) {
	    		    	        	deleteFile(newPath3);
    			    	        	dirs3 = FileIO.getSubdirectories(newPath2);
	    		    	        }
			                }
			    	        if (dirs3.length == 0) {
			    	        	deleteFile(newPath2);
			    	        	dirs2 = FileIO.getSubdirectories(newPath);
			    	        }
			            }
		    	        if (dirs3 == null) {
		    	        	deleteFile(newPath2);
		    	        	dirs2 = FileIO.getSubdirectories(newPath);
		    	        }
		            }
	    	        if (dirs2.length == 0) {
	    	        	deleteFile(newPath);
	    	        	dirs = FileIO.getSubdirectories(path);
	    	        }
	            }
    	        if (dirs2 == null) {
    	        	deleteFile(newPath);
    	        	dirs = FileIO.getSubdirectories(path);
    	        }
	        }
	        if (dirs.length == 0) deleteFile(path);
        }
        if (dirs == null) deleteFile(path);
	}
	/**
	 * Compress a given directory and its contents, up to the fifth subdirectory level.
	 * @param fileName Zip file to create.
	 * @param path Path of the directory to compress.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void zipDirectory(String fileName, String path)
	throws JPARSECException {
	    try {
	    	File file = new File(path);
	    	if (!file.isDirectory()) throw new JPARSECException(path+" is not a directory.");

	    	FileOutputStream dest = new FileOutputStream(new File(fileName));

	    	CheckedOutputStream checksum = new CheckedOutputStream(dest, new Adler32());
	    	ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(checksum));

	    	Zip.addDirectory(path, out);
	    	out.close();

	    } catch(Exception e) {
	    	throw new JPARSECException("error during compression.", e);
	    }
	}

	private static boolean sameFile(String file1, String file2)
	throws JPARSECException {
		boolean same = false;

		File f1 = new File(file1);
		File f2 = new File(file2);

		try {
			if (f1.getCanonicalPath().equals(f2.getCanonicalPath())) same = true;
		} catch (IOException e)
		{
			throw new JPARSECException(e);
		}

		return same;
	}
}
