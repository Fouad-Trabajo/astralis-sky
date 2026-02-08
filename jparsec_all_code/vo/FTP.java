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
package jparsec.vo;

import com.jcraft.jsch.*;

import java.io.*;
import java.util.*;
import javax.swing.JOptionPane;

import jparsec.graph.*;
import jparsec.util.*;

/**
 * A class to provide FTP connectivity.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class FTP {

	/**
	 * The user name.
	 */
	public String userName;
	/**
	 * The password.
	 */
	public String password;
	/**
	 * The host name.
	 */
	public String host;

	private Session session = null;
    private ChannelSftp ftp = null;

	/**
	 * Constructor. The connection is automatically started in
	 * passive mode.
	 * @param host Host.
	 * @param user User name.
	 * @param pass Password.
	 * @throws JPARSECException If an error occurs.
	 */
	public FTP(String host, String user, String pass)
	throws JPARSECException {
		this.host = host;
		this.userName = user;
		this.password = pass;

		try {
		      JSch jsch=new JSch();

		      session=jsch.getSession(user, host, 22);

		      UserInfo ui = new MyUserInfo(password);
		      session.setUserInfo(ui);
		      session.connect();

		      Channel channel = session.openChannel("sftp");
		      channel.connect();
		      ftp=(ChannelSftp)channel;
		} catch (Exception io)
		{
			throw new JPARSECException(io);
		}
	}

	/**
	 * Constructor. The connection is automatically started in
	 * passive mode.
	 * @param host Host.
	 * @param user User name.
	 * @param pass Password.
	 * @param timeout Connection timeout in milliseconds.
	 * @throws JPARSECException If an error occurs.
	 */
	public FTP(String host, String user, String pass, int timeout)
	throws JPARSECException {
		this.host = host;
		this.userName = user;
		this.password = pass;

		try {
		      JSch jsch=new JSch();

		      session=jsch.getSession(user, host, 22);

		      UserInfo ui = new MyUserInfo(password);
		      session.setUserInfo(ui);
		      session.setServerAliveInterval(timeout);
		      session.connect();

		      Channel channel = session.openChannel("sftp");
		      channel.connect();
		      ftp=(ChannelSftp)channel;
		} catch (Exception io)
		{
			throw new JPARSECException(io);
		}
	}

	/**
	 * Executes a command on the server.
	 * @param command The command.
	 * @return Output from the server.
	 * @throws JPARSECException If an error occurs.
	 */
	public String executeCommand(String command) throws JPARSECException {
		try {
			StringBuffer out = new StringBuffer("");
			ChannelExec exec=(ChannelExec) session.openChannel("exec");
			exec.setCommand(command);
			exec.setErrStream(System.err);
			InputStream in = exec.getInputStream();
			exec.connect();
			byte[] tmp = new byte[1024];
			while (true) {
				while (in.available() > 0) {
					int i = in.read(tmp, 0, 1024);
					if (i > 0) {
						out.append(new String(tmp, 0, i));
						if (i < 1024) break;
					}
				}
				if (exec.isClosed()) break;
				try { Thread.sleep(1000); } catch (Exception ee) {}
			}
			exec.disconnect();
			return out.toString();
		} catch (Exception exc) {
			throw new JPARSECException("Error executing command "+command, exc);
		}
	}

	/**
	 * Returns the list of files/directories in the current path.
	 * @param raw True to retrieve raw data.
	 * @return The list.
	 * @throws JPARSECException If an error occurs.
	 */
	public String[] getDirectoryList(boolean raw)
	throws JPARSECException {
		try {
	        Vector<?> vv = ftp.ls(".");
	        ArrayList<String> out = new ArrayList<String>();
		    if(vv!=null){
			      for(int ii=0; ii<vv.size(); ii++){

		                Object obj=vv.elementAt(ii);
		                if(obj instanceof ChannelSftp.LsEntry){
		                  if (raw) {
		                	  out.add(((ChannelSftp.LsEntry)obj).getLongname());
		                  } else {
		                	  out.add(((ChannelSftp.LsEntry)obj).getFilename());
		                  }
		                }
			      }
			    }

	        return DataSet.arrayListToStringArray(out);
		} catch (Exception io)
		{
			throw new JPARSECException(io);
		}
	}

	/**
	 * Returns the last modified time of the list of files/directories in the current path. Values
	 * are in seconds from Jan 1, 1970.
	 * @return The list.
	 * @throws JPARSECException If an error occurs.
	 */
	public long[] getLastModifiedTimeOfDirectoryList()
	throws JPARSECException {
		try {
	        Vector<?> vv = ftp.ls(".");
	        ArrayList<Long> out = new ArrayList<Long>();
		    if(vv!=null){
			      for(int ii=0; ii<vv.size(); ii++){

		                Object obj=vv.elementAt(ii);
		                if(obj instanceof ChannelSftp.LsEntry){
		                	out.add(new Long(((ChannelSftp.LsEntry)obj).getAttrs().getMTime()));
		                }
			      }
			    }

	        return DataSet.arrayListToLongArray(out);
		} catch (Exception io)
		{
			throw new JPARSECException(io);
		}
	}

	/**
	 * Changes the directory.
	 * @param dir New directory.
	 * @throws JPARSECException If an error occurs.
	 */
	public void changeDirectory(String dir)
	throws JPARSECException {
		try {
			ftp.cd(dir);
		} catch (Exception io)
		{
			throw new JPARSECException(io);
		}
	}

	/**
	 * Changes the local directory.
	 * @param dir New local directory.
	 * @throws JPARSECException If an error occurs.
	 */
	public void changeLocalDirectory(String dir)
	throws JPARSECException {
		try {
			ftp.lcd(dir);
		} catch (Exception io)
		{
			throw new JPARSECException(io);
		}
	}

	/**
	 * Goes back to the home directory.
	 * @throws JPARSECException If an error occurs.
	 */
	public void changeToHomeDirectory()
	throws JPARSECException {
		try {
			ftp.cd(ftp.getHome());
		} catch (Exception io)
		{
			throw new JPARSECException(io);
		}
	}

	/**
	 * Uploads a file to the server.
	 * @param localPath The local path.
	 * @param remotePath The remote path.
	 * @throws JPARSECException If an error occurs.
	 */
	public void uppload(String localPath, String remotePath)
	throws JPARSECException {
		try {
	        ftp.put(localPath, remotePath);
		} catch (Exception io)
		{
			throw new JPARSECException(io);
		}
	}

	/**
	 * Uploads a string as a file to the server.
	 * @param string The string.
	 * @param remotePath The remote path.
	 * @throws JPARSECException If an error occurs.
	 */
	public void upploadString(String string, String remotePath)
	throws JPARSECException {
		try {
	        OutputStream out = ftp.put(remotePath);
	        try {
	            out.write(string.getBytes());
	        }
	        finally {
	            out.close(); // MUST be closed to complete the transfer
	        }
		} catch (Exception io)
		{
			throw new JPARSECException(io);
		}
	}
	/**
	 * Downloads a file from the server.
	 * @param localPath The local path.
	 * @param remotePath The remote path.
	 * @throws JPARSECException If an error occurs.
	 */
	public void download(String localPath, String remotePath)
	throws JPARSECException {
		try {
	        ftp.get(remotePath, localPath);
		} catch (Exception io)
		{
			throw new JPARSECException(io);
		}
	}
	/**
	 * Downloads a file as a string from the server.
	 * @param remotePath The remote path.
	 * @return The string.
	 * @throws JPARSECException If an error occurs.
	 */
	public String downloadString(String remotePath)
	throws JPARSECException {
		try {
            StringBuffer s2 = new StringBuffer();
            InputStream in = ftp.get(remotePath);
            try {
                int ch = 0;
                while ((ch = in.read()) >= 0) {
                    s2.append((char)ch);
                }
            }
            finally {
                in.close(); // MUST be closed to complete the transfer
            }
            return s2.toString();
		} catch (Exception io)
		{
			throw new JPARSECException(io);
		}
	}

	/**
	 * Disconnects.
	 * @throws JPARSECException If an error occurs.
	 */
	public void disconnect()
	throws JPARSECException {
		try {
	        ftp.disconnect();
		} catch (Exception io)
		{
			throw new JPARSECException(io);
		}
	}

	/**
	 * Returns current directory.
	 * @return Working directory.
	 * @throws JPARSECException if an error occurs.
	 */
	public String getCurrentDir()
	throws JPARSECException {
		try {
			return ftp.pwd();
		} catch (Exception io)
		{
			throw new JPARSECException(io);
		}
	}
	/**
	 * Creates a directory.
	 * @param name Directory name.
	 * @throws JPARSECException if an error occurs.
	 */
	public void makeDir(String name)
	throws JPARSECException {
		try {
			ftp.mkdir(name);
		} catch (Exception io)
		{
			throw new JPARSECException(io);
		}
	}
	/**
	 * Removes a directory.
	 * @param name Directory name.
	 * @throws JPARSECException if an error occurs.
	 */
	public void rmDir(String name)
	throws JPARSECException {
		try {
			ftp.rmdir(name);
		} catch (Exception io)
		{
			throw new JPARSECException(io);
		}
	}
	/**
	 * Removes a file.
	 * @param name File name.
	 * @throws JPARSECException if an error occurs.
	 */
	public void rmFile(String name)
	throws JPARSECException {
		try {
			ftp.rm(name);
		} catch (Exception io)
		{
			throw new JPARSECException(io);
		}
	}
	/**
	 * Renames a file.
	 * @param oldName File name.
	 * @param newName New file name.
	 * @throws JPARSECException if an error occurs.
	 */
	public void rename(String oldName, String newName)
	throws JPARSECException {
		try {
			ftp.rename(oldName, newName);
		} catch (Exception io)
		{
			throw new JPARSECException(io);
		}
	}
	
	/**
	 * Returns the current Session object from jsch.
	 * @return The Session object.
	 */
	public Session getSession () {
		return session;
	}


	  private static class MyUserInfo implements UserInfo{
		  public MyUserInfo (String pass) {
			  passwd = pass;
		  }
		  public String getPassword(){ return passwd; }
		  public boolean promptYesNo(String str){
			  return true;
		  }

		  String passwd;

		  public String getPassphrase(){ return null; }
		  public boolean promptPassphrase(String message){ return true; }
		  public boolean promptPassword(String message){
			  return true;
		  }
		  public void showMessage(String message){
			  JOptionPane.showMessageDialog(null, message);
		  }
	  }
}
