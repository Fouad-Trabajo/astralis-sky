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

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;

import jparsec.util.*;

/**
 * A class to read from and write to the system clipboard using static methods.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class SystemClipboard
{
	// private constructor so that this class cannot be instantiated.
	private SystemClipboard() {}

	/**
	 * Copies an image (JPG, GIF) to the system clipboard.
	 *
	 * @param image Image to copy to clipboard.
	 */
	public static void setClipboard(Image image)
	{
		ImageSelection imageSelection = new ImageSelection(image);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(imageSelection, null);
	}

	/**
	 * Copies a string to the system clipboard.
	 *
	 * @param str String to copy to clipboard.
	 */
	public static void setClipboard(String str)
	{
		StringSelection ss = new StringSelection(str);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
	}

	/**
	 * Copies the current selected text to the system clipboard.
	 */
	public static void setClipboard()
	{
		StringSelection ss = new StringSelection(Toolkit.getDefaultToolkit().getSystemSelection().toString());
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
	}

	/**
	 * Gets the current text from the system clipboard.
	 *
	 * @return The contents of the system clipboard.
	 */
	public static String getClipboard()
	{
		String text = Toolkit.getDefaultToolkit().getSystemClipboard().toString();
		return text;
	}

	/**
	 * Gets an image from the system clipboard.
	 * @return Image object.
	 * @throws JPARSECException If an error occur.
	 */
    public static Image getImageFromClipboard()
    throws JPARSECException {
    	  // get the system clipboard
    	  Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

    	  // get the contents on the clipboard in a
    	  // Transferable object
    	  Transferable clipboardContents = systemClipboard.getContents(null);

    	  // check if contents are empty, if so, return null
    	  if (clipboardContents == null)
    	    return null;
    	  else
    	    // make sure content on clipboard is
    	    // falls under a format supported by the
    	    // imageFlavor Flavor
    	    if (clipboardContents.isDataFlavorSupported(DataFlavor.imageFlavor))
    	       {
    	         // convert the Transferable object
    	         // to an Image object
    	    	try {
    	         Image image = (Image) clipboardContents.getTransferData(DataFlavor.imageFlavor);
    	         return image;
    	    	} catch (Exception e)
    	    	{
    	    		throw new JPARSECException(e);
    	    	}
    	       }
    	      return null;
    	   }
}

//Inner class is used to hold an image while on the clipboard.
class ImageSelection implements Transferable
{
  // the Image object which will be housed by the ImageSelection
  private Image image;

  public ImageSelection(Image image) {
    this.image = image;
  }

  // Returns the supported flavors of our implementation
  public DataFlavor[] getTransferDataFlavors()
  {
    return new DataFlavor[] {DataFlavor.imageFlavor};
  }

  // Returns true if flavor is supported
  public boolean isDataFlavorSupported(DataFlavor flavor)
  {
    return DataFlavor.imageFlavor.equals(flavor);
  }

  // Returns Image object housed by Transferable object
  public Object getTransferData(DataFlavor flavor)
    throws UnsupportedFlavorException,IOException
  {
    if (!DataFlavor.imageFlavor.equals(flavor))
    {
      throw new UnsupportedFlavorException(flavor);
    }
    // else return the payload
    return image;
  }
}
