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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import javax.swing.RepaintManager;

import jparsec.util.JPARSECException;

/**
 * Basic support for printing to a file or printer.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Printer implements Printable
{
	/**
	 * Sets the component to print.
	 */
	private Component componentToBePrinted;

	/**
	 * Constructor for a component.
	 * @param componentToPrint Component to be printed.
	 */
	public Printer(Component componentToPrint)
	{
		componentToBePrinted = componentToPrint;
	}

	/**
	 * Print to the printer, portrait by default.
	 *
	 * @throws JPARSECException Thrown if the process fails.
	 */
	public void print() throws JPARSECException
	{
		PrinterJob printJob = PrinterJob.getPrinterJob();
		printJob.setPrintable(this);

		/*
		 * PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
		 * aset.add(OrientationRequested.PORTRAIT); aset.add(new Copies(1));
		 * aset.add(new JobName("JPARSEC PRINTJOB", null));
		 */

		if (printJob.printDialog())
			try
			{
				// printJob.print(aset);
				printJob.print();
			} catch (PrinterException pe)
			{
				throw new JPARSECException(pe);
			}
	}

	/**
	 * Implements printable method.
	 */
	public int print(Graphics g, PageFormat pageFormat, int pageIndex)
	{
		if (pageIndex > 0)
		{
			return (NO_SUCH_PAGE);
		} else
		{

			Graphics2D g2d = (Graphics2D) g;
			g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

			// Begin of optional scaling
			// Portrait
			double scalex = pageFormat.getImageableWidth() / (double) componentToBePrinted.getWidth();
			double scaley = pageFormat.getImageableHeight() / (double) componentToBePrinted.getHeight();

			/*
			 * // Landscape double scalex = pageFormat.getImageableHeight() /
			 * (double) componentToBePrinted.getWidth(); double scaley =
			 * pageFormat.getImageableWidth() / (double)
			 * componentToBePrinted.getHeight(); System.out.println("scalex
			 * "+scalex); System.out.println("scaley "+scaley);
			 * System.out.println("max x "+pageFormat.getWidth());
			 * System.out.println("max y "+pageFormat.getHeight());
			 */

			double scale = scalex;
			if (scaley < scale)
				scale = scaley;
			g2d.scale(scale, scale);
			// End of scaling

			boolean visible = componentToBePrinted.isVisible();
			componentToBePrinted.setVisible(true);
			disableDoubleBuffering(componentToBePrinted);
			componentToBePrinted.paint(g2d);
			enableDoubleBuffering(componentToBePrinted);
			componentToBePrinted.setVisible(visible);
			return (PAGE_EXISTS);
		}
	}

	void disableDoubleBuffering(Component c)
	{
		RepaintManager currentManager = RepaintManager.currentManager(c);
		currentManager.setDoubleBufferingEnabled(false);
	}

	void enableDoubleBuffering(Component c)
	{
		RepaintManager currentManager = RepaintManager.currentManager(c);
		currentManager.setDoubleBufferingEnabled(true);
	}
}
