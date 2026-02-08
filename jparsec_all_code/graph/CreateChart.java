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
package jparsec.graph;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.CategoryPointerAnnotation;
import org.jfree.chart.annotations.XYPointerAnnotation;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ColorBar;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.entity.StandardEntityCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.ContourPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.RingPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.AreaRenderer;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.LineRenderer3D;
import org.jfree.chart.renderer.category.StackedAreaRenderer;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYStepAreaRenderer;
import org.jfree.chart.renderer.xy.XYStepRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.contour.ContourDataset;
import org.jfree.data.contour.DefaultContourDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYIntervalSeries;
import org.jfree.data.xy.XYIntervalSeriesCollection;
import org.jfree.ui.TextAnchor;

import jparsec.ephem.Functions;
import jparsec.graph.ChartElement.SUBTYPE;
import jparsec.graph.chartRendering.AWTGraphics;
import jparsec.io.FileIO;
import jparsec.io.WriteFile;
import jparsec.io.image.Picture;
import jparsec.math.Evaluation;
import jparsec.math.GenericFit;
import jparsec.math.Interpolation;
import jparsec.math.LinearFit;
import jparsec.math.Polynomial;
import jparsec.math.Regression;
import jparsec.time.AstroDate;
import jparsec.util.JPARSECException;
import jparsec.util.Logger;
import jparsec.util.Logger.LEVEL;

/**
 * Creates a chart using JFreeChart or GILDAS.<P>
 *
 * Charts can be created from a {@linkplain ChartElement} or a
 * {@linkplain SimpleChartElement} objects. Once created they can
 * be visualize using either the JFreeChart original panel or a
 * default panel defined in {@linkplain Picture}.<P>
 *
 * When using a JFreeChart panel, you will get some advantages like
 * automatic resize of the chart, point labels when setting the mouse
 * on a given point, and an emerging window (right mouse click) with
 * some options. However, sub-charts will not be visible.<P>
 *
 * When using a default panel, you will be able to see a sub-chart
 * (a little chart inside the main one) if the sub-chart was defined
 * in the {@linkplain ChartElement} object. Another advantage is the
 * automatic update of the chart, without calling any method. In this
 * default panel the width of the chart should be equal to the height.<P>
 *
 * It is also possible to export the chart to the following formats:
 * JPG, PNG, EPS, SVG, and PDF.<P>
 *
 * The JFreechart version used here is a modified version of the original.
 * The JCommon library was modified to support subscripts and superscripts
 * usually required to properly draw scientific charts.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class CreateChart implements Serializable
{
	private static final long serialVersionUID = 1L;

	private JFreeChart chart;
	private ChartElement chart_elem;
	private void writeObject(ObjectOutputStream out)
	throws IOException {
		out.writeObject(this.chart_elem);
	}
	private void readObject(ObjectInputStream in)
	throws IOException, ClassNotFoundException {
		this.chart_elem = (ChartElement) in.readObject();
		try {
			JFreeChart chart = createChart(this.chart_elem);
			this.setChart(chart);
		} catch (Exception exc) { }
 	}

	/**
	 * Creates an HTML file with the chart.
	 *
	 * @param chart Chart element.
	 * @param file_name File name without extension.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void createChartAsHTMLFile(SimpleChartElement chart, String file_name) throws JPARSECException
	{
		int ext = file_name.toLowerCase().lastIndexOf(".htm");
		if (ext > 0) file_name = file_name.substring(0, ext);

		CreateChart jfchart = new CreateChart(chart);
		jfchart.chartAsHTMLFile(file_name);
	}

	/**
	 * Creates an HTML file with the chart.
	 *
	 * @param chart Chart element.
	 * @param file_name File name without extension.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void createChartAsHTMLFile(ChartElement chart, String file_name) throws JPARSECException
	{
		int ext = file_name.toLowerCase().lastIndexOf(".htm");
		if (ext > 0) file_name = file_name.substring(0, ext);

		CreateChart jfchart = new CreateChart(chart);
		jfchart.chartAsHTMLFile(file_name);
	}

	/**
	 * Exports the chart as an HTML file.
	 *
	 * @param file_name File name without extension.
	 * @throws JPARSECException If an error occurs.
	 */
	public void chartAsHTMLFile(String file_name)
	throws JPARSECException {
		int ext = file_name.toLowerCase().lastIndexOf(".htm");
		if (ext > 0) file_name = file_name.substring(0, ext);

		int size_x = this.chart_elem.imageWidth;
		int size_y = this.chart_elem.imageHeight;
		this.chartAsHTMLFile(file_name, size_x, size_y);
	}

	/**
	 * Exports the chart as an HTML file.
	 *
	 * @param file_name File name without extension.
	 * @param size_x Image width.
	 * @param size_y Image height.
	 * @throws JPARSECException If an error occurs.
	 */
	public void chartAsHTMLFile(String file_name, int size_x, int size_y)
	throws JPARSECException {
		int ext = file_name.toLowerCase().lastIndexOf(".htm");
		if (ext > 0) file_name = file_name.substring(0, ext);

		// save it to an image
		try
		{
			ChartRenderingInfo info = new ChartRenderingInfo(new StandardEntityCollection());
			File file1 = new File(file_name + ".png");
			ChartUtilities.saveChartAsPNG(file1, getChart(), size_x, size_y, info);

			// write an HTML page incorporating the image with an image map
			File file2 = new File(file_name + ".html");
			OutputStream out = new BufferedOutputStream(new FileOutputStream(file2));
			PrintWriter writer = new PrintWriter(out);
			writer.println("<HTML>");
			writer.println("<HEAD><TITLE>JFreeChart Image Map Demo</TITLE></HEAD>");
			writer.println("<BODY>");
			ChartUtilities.writeImageMap(writer, "chart", info, true);
			writer
					.println("<IMG SRC=\""+file_name+".png\" " + "WIDTH=\" " + size_x + " \" HEIGHT=\" " + size_y + " \" BORDER=\"0\" USEMAP=\"#chart\">");
			writer.println("</BODY>");
			writer.println("</HTML>");
			writer.close();

		} catch (Exception e)
		{
			throw new JPARSECException(e);
		}
	}

	/**
	 * Creates a EPS file with the chart.
	 *
	 * @param chart_elem Chart element.
	 * @param file_name File name without extension.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void createChartAsEPSFile(SimpleChartElement chart_elem, String file_name) throws JPARSECException
	{
		int ext = file_name.toLowerCase().lastIndexOf(".eps");
		if (ext > 0) file_name = file_name.substring(0, ext);

		CreateChart chart = new CreateChart();
		chart.chartAsEPSFile(chart_elem, file_name);
	}

	/**
	 * Exports the chart as an EPS file.
	 *
	 * @param file_name File name without extension.
	 * @throws JPARSECException If an error occurs.
	 */
	public void chartAsEPSFile(String file_name)
	throws JPARSECException {
		int ext = file_name.toLowerCase().lastIndexOf(".eps");
		if (ext > 0) file_name = file_name.substring(0, ext);

		int size_x = this.chart_elem.imageWidth;
		int size_y = this.chart_elem.imageHeight;
		this.chartAsEPSFile(file_name, size_x, size_y);
	}

	/**
	 * Exports the chart as an EPS file.
	 *
	 * @param file_name File name.
	 * @param size_x Image width.
	 * @param size_y Image height.
	 * @throws JPARSECException If an error occurs.
	 */
	public void chartAsEPSFile(String file_name, int size_x, int size_y) throws JPARSECException
	{
		int ext = file_name.toLowerCase().lastIndexOf(".eps");
		if (ext > 0) file_name = file_name.substring(0, ext);

		File plotFile = new File(file_name + ".eps");

//		final Dimension size = new Dimension(chart_elem.imageWidth, chart_elem.imageHeight);
		final Dimension size = new Dimension(size_x, size_y);
		try
		{
			// Using reflection so that everything will work without freehep in classpath
			Class<?> c = Class.forName("org.freehep.graphicsio.ps.PSGraphics2D");
			Constructor<?> cc = c.getConstructor(new Class[] { File.class, Dimension.class });
			Object psGraphics = cc.newInstance(new Object[] { plotFile, size });
			Method m = c.getMethod("startExport");
			m.invoke(psGraphics);
			this.paintChart((Graphics2D) psGraphics, size_x, size_y);
			Method mm = c.getMethod("endExport");
			mm.invoke(psGraphics);
		} catch (Exception e)
		{
			throw new JPARSECException("cannot write to file.", e);
		}
	}

	private void chartAsEPSFile(SimpleChartElement chart_elem, String file_name) throws JPARSECException
	{
		int ext = file_name.toLowerCase().lastIndexOf(".eps");
		if (ext > 0) file_name = file_name.substring(0, ext);

		CreateChart ch = new CreateChart(chart_elem);
		ch.chartAsEPSFile(file_name);
	}

	/**
	 * Creates a PDF file with the chart.
	 *
	 * @param chart_elem Chart element.
	 * @param file_name File name without extension.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void createChartAsPDFFile(SimpleChartElement chart_elem, String file_name) throws JPARSECException
	{
		int ext = file_name.toLowerCase().lastIndexOf(".pdf");
		if (ext > 0) file_name = file_name.substring(0, ext);

		CreateChart chart = new CreateChart();
		chart.chartAsPDFFile(chart_elem, file_name);
	}

	/**
	 * Exports the chart as an PDF file.
	 *
	 * @param file_name File name without extension.
	 * @throws JPARSECException If an error occurs.
	 */
	public void chartAsPDFFile(String file_name)
	throws JPARSECException {
		int ext = file_name.toLowerCase().lastIndexOf(".pdf");
		if (ext > 0) file_name = file_name.substring(0, ext);

		this.chartAsPDFFile(this.chart_elem, file_name);
	}


	private void chartAsPDFFile(SimpleChartElement chart_elem, String file_name) throws JPARSECException
	{
		int ext = file_name.toLowerCase().lastIndexOf(".pdf");
		if (ext > 0) file_name = file_name.substring(0, ext);

		CreateChart ch = new CreateChart(chart_elem);
		ch.chartAsPDFFile(file_name);
	}

	/**
	 * Creates a PNG file with the chart.
	 *
	 * @param chart_elem Chart element.
	 * @param file_name File name without extension.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void createChartAsPNGFile(SimpleChartElement chart_elem, String file_name) throws JPARSECException
	{
		int ext = file_name.toLowerCase().lastIndexOf(".png");
		if (ext > 0) file_name = file_name.substring(0, ext);

		CreateChart chart = new CreateChart();
		chart.chartAsPNGFile(chart_elem, file_name);
	}

	/**
	 * Exports the chart as an PNG file.
	 *
	 * @param file_name File name without extension.
	 * @throws JPARSECException If an error occurs.
	 */
	public void chartAsPNGFile(String file_name)
	throws JPARSECException {
		int ext = file_name.toLowerCase().lastIndexOf(".png");
		if (ext > 0) file_name = file_name.substring(0, ext);

		int size_x = this.chart_elem.imageWidth;
		int size_y = this.chart_elem.imageHeight;
		this.chartAsPNGFile(file_name, size_x, size_y);
	}

	/**
	 * Exports the chart as a PNG file.
	 *
	 * @param file_name File name without extension.
	 * @param size_x Image width.
	 * @param size_y Image height.
	 * @throws JPARSECException If an error occurs.
	 */
	public void chartAsPNGFile(String file_name, int size_x, int size_y) throws JPARSECException
	{
		int ext = file_name.toLowerCase().lastIndexOf(".png");
		if (ext > 0) file_name = file_name.substring(0, ext);

		try
		{
			// write chart as PNG file
			ChartUtilities.saveChartAsPNG(new File(file_name+".png"), getChart(), size_x, size_y);
		} catch (Exception e)
		{
			throw new JPARSECException(e);
		}
	}

	private void chartAsPNGFile(SimpleChartElement chart_elem, String file_name) throws JPARSECException
	{
		int ext = file_name.toLowerCase().lastIndexOf(".png");
		if (ext > 0) file_name = file_name.substring(0, ext);

		CreateChart ch = new CreateChart(chart_elem);
		ch.chartAsPNGFile(file_name);
	}

	/**
	 * Creates a SVG file with the chart.
	 *
	 * @param chart_elem Chart element.
	 * @param file_name File name without extension.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void createChartAsSVGFile(SimpleChartElement chart_elem, String file_name) throws JPARSECException
	{
		int ext = file_name.toLowerCase().lastIndexOf(".svg");
		if (ext > 0) file_name = file_name.substring(0, ext);

		CreateChart chart = new CreateChart();
		chart.chartAsSVGFile(chart_elem, file_name);
	}

	/**
	 * Exports the chart as an SVG file.
	 *
	 * @param file_name File name without extension.
	 * @throws JPARSECException If an error occurs.
	 */
	public void chartAsSVGFile(String file_name)
	throws JPARSECException {
		int ext = file_name.toLowerCase().lastIndexOf(".svg");
		if (ext > 0) file_name = file_name.substring(0, ext);

		this.chartAsSVGFile(this.chart_elem, file_name);
	}

	private void chartAsSVGFile(SimpleChartElement chart_elem, String file_name) throws JPARSECException
	{
		int ext = file_name.toLowerCase().lastIndexOf(".svg");
		if (ext > 0) file_name = file_name.substring(0, ext);

		CreateChart ch = new CreateChart(chart_elem);
		ch.chartAsSVGFile(file_name);
	}

	/**
	 * Creates a EPS file with the chart.
	 *
	 * @param chart_elem Chart element.
	 * @param file_name File name without extension.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void createChartAsEPSFile(ChartElement chart_elem, String file_name) throws JPARSECException
	{
		int ext = file_name.toLowerCase().lastIndexOf(".eps");
		if (ext > 0) file_name = file_name.substring(0, ext);

		CreateChart chart = new CreateChart();
		chart.chartAsEPSFile(chart_elem, file_name);
	}

	private void chartAsEPSFile(ChartElement chart_elem, String file_name) throws JPARSECException
	{
		int ext = file_name.toLowerCase().lastIndexOf(".eps");
		if (ext > 0) file_name = file_name.substring(0, ext);

		CreateChart rPlot = new CreateChart(chart_elem);
		File plotFile = new File(file_name+".eps");

		final Dimension size = new Dimension(chart_elem.imageWidth, chart_elem.imageHeight);
		try
		{
			// Using reflection so that everything will work without freehep in classpath
			Class<?> c = Class.forName("org.freehep.graphicsio.ps.PSGraphics2D");
			Constructor<?> cc = c.getConstructor(new Class[] {plotFile.getClass(), size.getClass()});
			Object psGraphics = cc.newInstance(new Object[] {plotFile, size});
			Method m = c.getMethod("startExport");
			m.invoke(psGraphics);
			rPlot.getChart().draw((Graphics2D) psGraphics, new Rectangle2D.Double(0, 0, chart_elem.imageWidth, chart_elem.imageHeight));
			Method mm = c.getMethod("endExport");
			mm.invoke(psGraphics);
		} catch (Exception e)
		{
			throw new JPARSECException("cannot write to file.", e);
		}
	}

	/**
	 * Creates a PDF file with the chart.
	 *
	 * @param chart_elem Chart element.
	 * @param file_name File name without extension.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void createChartAsPDFFile(ChartElement chart_elem, String file_name) throws JPARSECException
	{
		int ext = file_name.toLowerCase().lastIndexOf(".pdf");
		if (ext > 0) file_name = file_name.substring(0, ext);

		CreateChart chart = new CreateChart();
		chart.chartAsPDFFile(chart_elem, file_name);
	}

	private void chartAsPDFFile(ChartElement chart_elem, String file_name) throws JPARSECException
	{
		int ext = file_name.toLowerCase().lastIndexOf(".pdf");
		if (ext > 0) file_name = file_name.substring(0, ext);

		CreateChart rPlot = new CreateChart(chart_elem);
		File plotFile = new File(file_name+".pdf");

		final Dimension size = new Dimension(chart_elem.imageWidth, chart_elem.imageHeight);
		try
		{
			// Using reflection so that everything will work without freehep in classpath (not this)
			Class<?> c = Class.forName("org.freehep.graphicsio.pdf.PDFGraphics2D");
			Constructor<?> cc = c.getConstructor(new Class[] {plotFile.getClass(), size.getClass()});
			Object pdfGraphics = cc.newInstance(new Object[] {plotFile, size});
			Method m = c.getMethod("startExport");
			m.invoke(pdfGraphics);
			rPlot.getChart().draw((Graphics2D) pdfGraphics, new Rectangle2D.Double(0, 0, chart_elem.imageWidth, chart_elem.imageHeight));
			Method mm = c.getMethod("endExport");
			mm.invoke(pdfGraphics);
		} catch (Exception e)
		{
			throw new JPARSECException("cannot write to file.", e);
		}
	}

	/**
	 * Creates a PNG file with the chart.
	 *
	 * @param chart_elem Chart element.
	 * @param file_name File name without extension.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void createChartAsPNGFile(ChartElement chart_elem, String file_name) throws JPARSECException
	{
		int ext = file_name.toLowerCase().lastIndexOf(".png");
		if (ext > 0) file_name = file_name.substring(0, ext);

		CreateChart chart = new CreateChart();
		chart.chartAsPNGFile(chart_elem, file_name);
	}

	private void chartAsPNGFile(ChartElement chart_elem, String file_name) throws JPARSECException
	{
		int ext = file_name.toLowerCase().lastIndexOf(".png");
		if (ext > 0) file_name = file_name.substring(0, ext);

		CreateChart chart = new CreateChart(chart_elem);

		try
		{
			// write chart as PNG file
			ChartUtilities.saveChartAsPNG(new File(file_name + ".png"), chart.getChart(), chart_elem.imageWidth, chart_elem.imageHeight);
		} catch (Exception e)
		{
			throw new JPARSECException(e);
		}
	}

	/**
	 * Creates a SVG file with the chart.
	 *
	 * @param chart_elem Chart element.
	 * @param file_name File name without extension.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void createChartAsSVGFile(ChartElement chart_elem, String file_name) throws JPARSECException
	{
		int ext = file_name.toLowerCase().lastIndexOf(".svg");
		if (ext > 0) file_name = file_name.substring(0, ext);

		CreateChart chart = new CreateChart();
		chart.chartAsSVGFile(chart_elem, file_name);
	}

	private void chartAsSVGFile(ChartElement chart_elem, String file_name) throws JPARSECException
	{
		int ext = file_name.toLowerCase().lastIndexOf(".svg");
		if (ext > 0) file_name = file_name.substring(0, ext);

		CreateChart rPlot = new CreateChart(chart_elem);
		File plotFile = new File(file_name+".svg");

		final Dimension size = new Dimension(chart_elem.imageWidth, chart_elem.imageHeight);
		try
		{
			// Using reflection so that everything will work without freehep in classpath
			Class<?> c = Class.forName("org.freehep.graphicsio.svg.SVGGraphics2D");
			Constructor<?> cc = c.getConstructor(new Class[] { File.class, Dimension.class });
			Object svgGraphics = cc.newInstance(new Object[] { plotFile, size });
			Method m = c.getMethod("startExport");
			m.invoke(svgGraphics);
			rPlot.getChart().draw((Graphics2D) svgGraphics, new Rectangle2D.Double(0, 0, chart_elem.imageWidth, chart_elem.imageHeight));
			Method mm = c.getMethod("endExport");
			mm.invoke(svgGraphics);
		} catch (Exception e)
		{
			throw new JPARSECException("cannot write to file.", e);
		}
	}

	/**
	 * Exports the chart as an SVG file.
	 *
	 * @param file_name File name.
	 * @param size_x Image width.
	 * @param size_y Image height.
	 * @throws JPARSECException If an error occurs.
	 */
	public void chartAsSVGFile(String file_name, int size_x, int size_y) throws JPARSECException
	{
		int ext = file_name.toLowerCase().lastIndexOf(".svg");
		if (ext > 0) file_name = file_name.substring(0, ext);

		File plotFile = new File(file_name + ".svg");
		final Dimension size = new Dimension(size_x, size_y);

		try
		{
			// Using reflection so that everything will work without freehep in classpath
			Class<?> c = Class.forName("org.freehep.graphicsio.svg.SVGGraphics2D");
			Constructor<?> cc = c.getConstructor(new Class[] {File.class, Dimension.class });
			Object svgGraphics = cc.newInstance(new Object[] {plotFile, size});
			Method m = c.getMethod("startExport");
			m.invoke(svgGraphics);
			this.paintChart((Graphics2D) svgGraphics, size_x, size_y);
			Method mm = c.getMethod("endExport");
			mm.invoke(svgGraphics);
		} catch (Exception e)
		{
			throw new JPARSECException("cannot write to file.", e);
		}
	}

	/**
	 * Returns the chart as an image.
	 * @return The image.
	 */
	public BufferedImage chartAsBufferedImage()
	{
		return this.getChart().createBufferedImage(chart_elem.imageWidth, chart_elem.imageHeight);
	}
	/**
	 * Returns the chart as an image.
	 * @param width Image width.
	 * @param height Image height.
	 * @return The image.
	 */
	public BufferedImage chartAsBufferedImage(int width, int height)
	{
		return this.getChart().createBufferedImage(width, height);
	}
	/**
	 * Empty constructor.
	 */
	public CreateChart()
	{
	}

	/**
	 * Draws the current chart to a Graphics device.
	 * @param g Graphics object.
	 * @throws JPARSECException If an error occurs.
	 */
	public void paintChart(Graphics g)
	throws JPARSECException {
		getChart().draw((Graphics2D) g, new Rectangle2D.Double(0, 0, chart_elem.imageWidth,
				chart_elem.imageHeight));

		if (chart_elem == null) return;
		if (chart_elem.subCharts != null) {
			for (int i=0; i<chart_elem.subCharts.length; i++)
			{
				CreateChart subC = new CreateChart(chart_elem.subCharts[i]);
				subC.getChart().draw((Graphics2D) g, new Rectangle2D.Double(
						chart_elem.subChartPosition[i].getX(), chart_elem.subChartPosition[i].getY(),
						subC.chart_elem.imageWidth,
						subC.chart_elem.imageHeight));
			}
		}
	}

	/**
	 * Draws the current chart to a Graphics device.
	 * @param g Graphics object.
	 * @param w The width.
	 * @param h The height.
	 * @throws JPARSECException If an error occurs.
	 */
	public void paintChart(Graphics g, int w, int h)
	throws JPARSECException {
		getChart().draw((Graphics2D) g, new Rectangle2D.Double(0, 0, w, h));
		double scaleX = (double) w / (double) chart_elem.imageWidth;
		double scaleY = (double) h / (double) chart_elem.imageHeight;
		if (chart_elem == null) return;
		if (chart_elem.subCharts != null) {
			for (int i=0; i<chart_elem.subCharts.length; i++)
			{
				CreateChart subC = new CreateChart(chart_elem.subCharts[i]);
				subC.getChart().draw((Graphics2D) g, new Rectangle2D.Double(
						chart_elem.subChartPosition[i].getX() * scaleX,
						chart_elem.subChartPosition[i].getY() * scaleY,
						subC.chart_elem.imageWidth * scaleX,
						subC.chart_elem.imageHeight * scaleY));
			}
		}
	}

	/**
	 * Updates the chart.
	 * @throws JPARSECException If an error occurs.
	 */
	public void updateChart()
	throws JPARSECException {
		for (int i=0; i<this.getChartElement().series.length; i++) {
			if (this.getChartElement().series[i].regressionType != null)
				this.getChartElement().series[i].regressionType.clearRegression();
		}

		CreateChart newc = new CreateChart(this.getChartElement());
		this.setChart(newc.getChart());
		if (p != null) {
			p.setImage(newc.chartAsBufferedImage(
					newc.getChartElement().imageWidth,
					newc.getChartElement().imageHeight));
			p.update();
		}
		if (chartPanel != null) chartPanel.setChart(this.getChart());
	}

	/**
	 * Returns a BufferedImage instance with the current chart, adequate to
	 * write an image to disk.
	 * @return The image.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public BufferedImage createBufferedImage() throws JPARSECException
	{
		BufferedImage buf = new BufferedImage(this.chart_elem.imageWidth,
				this.chart_elem.imageHeight, BufferedImage.TYPE_INT_RGB);
		Graphics g = buf.createGraphics();
		this.paintChart(g);
		return buf;
	}

	/**
	 * Shows a series of charts in an JFreeChart panel.
	 * @param charts The charts.
	 * @param title Frame title.
	 * @param horizontal True to show them in horizontal orientation, false to arrange them in vertical.
	 * @return The frame.
	 * @throws JPARSECException If an error occurs.
	 */
	public static JFrame showChartsInJFreeChartPanel(CreateChart charts[], String title, boolean horizontal)
	throws JPARSECException {
		JFrame frame = new JFrame(title);
        JPanel p = new JPanel();
        if (horizontal) {
        	p.setLayout(new BoxLayout(p,BoxLayout.X_AXIS));
        } else {
            p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        }
		ChartPanel chartPanel[] = new ChartPanel[charts.length];
		for (int i=0; i<charts.length; i++)
		{
			int w = charts[i].chart_elem.imageWidth;
			int h = charts[i].chart_elem.imageHeight;
			chartPanel[i] = new ChartPanel(charts[i].getChart(), w, h, w/2, h/2, w*2, h*2, true, true, true, true, true, true, true);
			chartPanel[i].setPreferredSize(new java.awt.Dimension(w, h));
			chartPanel[i].setDisplayToolTips(true);
			chartPanel[i].setMouseWheelEnabled(true);

			if (charts[i].chart_elem.subCharts != null) {
				for (int ii=0; ii<charts[i].chart_elem.subCharts.length; ii++)
				{
					CreateChart subC = new CreateChart(charts[i].chart_elem.subCharts[ii]);
					subC.getChart().draw((Graphics2D) chartPanel[i].getGraphics(), new Rectangle2D.Double(
							charts[i].chart_elem.subChartPosition[ii].getX(), charts[i].chart_elem.subChartPosition[ii].getY(),
							subC.chart_elem.imageWidth,
							subC.chart_elem.imageHeight));
				}
			}

			p.add(chartPanel[i]);
		}
		JScrollPane sp = new JScrollPane(p);
        if (horizontal) {
        	sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        } else {
        	sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        }
		frame.add(sp);
		frame.setIconImage(charts[0].chartAsBufferedImage(
				charts[0].chart_elem.imageWidth,
				charts[0].chart_elem.imageHeight));
		frame.pack();
		frame.setVisible(true);

		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		return frame;
	}

	/**
	 * Returns the JPanel for the chart.
	 * @return The component.
	 * @throws JPARSECException If an error occurs.
	 */
	public ChartPanel getComponent()
	throws JPARSECException {
		int w = chart_elem.imageWidth;
		int h = chart_elem.imageHeight;
		ChartPanel chartPanel = new ChartPanel(getChart(), w, h, w/2, h/2, w*2, h*2, true, true, true, true, true, true, true);
		chartPanel.setPreferredSize(new java.awt.Dimension(w, h));
		chartPanel.setDisplayToolTips(true);
		chartPanel.setMouseWheelEnabled(true);
		return chartPanel;
	}
	
	/**
	 * Returns the JPanel for the chart.
	 * @return The component.
	 * @param prop Show properties option in popup menu.
	 * @param copy Show copy option in popup menu.
	 * @param save Show save option in popup menu.
	 * @param print Show print option in popup menu.
	 * @param zoom Show zoom option in popup menu.
	 * @param scale Show scale option in popup menu.
	 * @throws JPARSECException If an error occurs.
	 */
	public ChartPanel getComponent(boolean prop, boolean copy, boolean save, boolean print, boolean zoom, boolean scale)
	throws JPARSECException {
		int w = chart_elem.imageWidth;
		int h = chart_elem.imageHeight;
		ChartPanel chartPanel = new ChartPanel(getChart(), w, h, w/2, h/2, w*2, h*2, true, prop, copy, save, print, zoom, scale);
		chartPanel.setPreferredSize(new java.awt.Dimension(w, h));
		chartPanel.setDisplayToolTips(true);
		chartPanel.setMouseWheelEnabled(true);
		return chartPanel;
	}

	/**
	 * Shows the main chart in an JFreeChart panel.
	 * @return The panel object.
	 * @throws JPARSECException If an error occurs.
	 */
	public ChartPanel showChartInJFreeChartPanel()
	throws JPARSECException {
		int w = chart_elem.imageWidth;
		int h = chart_elem.imageHeight;
		chartPanel = new ChartPanel(getChart(), w, h, w/2, h/2, w*2, h*2, true, true, true, true, true, true, true);
		chartPanel.setPreferredSize(new java.awt.Dimension(w, h));
		chartPanel.setDisplayToolTips(true);
		chartPanel.setMouseWheelEnabled(true);
		String title = (new TextLabel(chart_elem.title,
				new Font("Dialog", Font.PLAIN, 10),
				Color.BLACK, TextLabel.ALIGN.LEFT)).getSimplifiedString();
		JFrame aframe = new JFrame(title);
		aframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		aframe.getContentPane().add(chartPanel);
		aframe.setIconImage(this.chartAsBufferedImage(w, h));
		aframe.pack();
		aframe.setVisible(true);

		if (chart_elem.subCharts != null) {
			for (int i=0; i<chart_elem.subCharts.length; i++)
			{
				CreateChart subC = new CreateChart(chart_elem.subCharts[i]);
				subC.getChart().draw((Graphics2D) chartPanel.getGraphics(), new Rectangle2D.Double(
						chart_elem.subChartPosition[i].getX(), chart_elem.subChartPosition[i].getY(),
						subC.chart_elem.imageWidth,
						subC.chart_elem.imageHeight));
			}
		}
		return chartPanel;
	}

	/**
	 * Shows the main chart in an JFreeChart panel including key controls to modify the chart.
	 * Left and right cursor keys to show more or less series, and up/down to show labels with
	 * a greater/lower font size. Q key to lose focus.
	 * @throws JPARSECException If an error occurs.
	 */
	public void showChartInJFreeChartPanelWithAdvancedControls()
	throws JPARSECException {
		int w = chart_elem.imageWidth;
		int h = chart_elem.imageHeight;
		final ChartPanel chartPanel = new ChartPanel(getChart(), w, h, w/2, h/2, w*2, h*2, true, true, true, true, true, true, true);
		chartPanel.setPreferredSize(new java.awt.Dimension(w, h));
		chartPanel.setDisplayToolTips(true);
		chartPanel.setMouseWheelEnabled(true);
		String title = (new TextLabel(chart_elem.title,
				new Font("Dialog", Font.PLAIN, 10),
				Color.BLACK, TextLabel.ALIGN.LEFT)).getSimplifiedString();
		JFrame aframe = new JFrame(title);
		aframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		aframe.getContentPane().add(chartPanel);
		aframe.setIconImage(this.chartAsBufferedImage(w, h));
		aframe.pack();
		aframe.setVisible(true);

		if (chart_elem.subCharts != null) {
			for (int i=0; i<chart_elem.subCharts.length; i++)
			{
				CreateChart subC = new CreateChart(chart_elem.subCharts[i]);
				subC.getChart().draw((Graphics2D) chartPanel.getGraphics(), new Rectangle2D.Double(
						chart_elem.subChartPosition[i].getX(), chart_elem.subChartPosition[i].getY(),
						subC.chart_elem.imageWidth,
						subC.chart_elem.imageHeight));
			}
		}

		aframe.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
         	   if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
          		   try {
          			   ChartElement ce = getChartElement();
          			   for (int i=0; i<ce.series.length; i++) {
          				   if (!ce.series[i].enable) {
          					   ce.series[i].enable = true;
          					   break;
          				   }
          			   }
          			   JFreeChart ch2 = (new CreateChart(ce)).getChart();
          			   chartPanel.setChart(ch2);
          		   } catch (Exception exc) {
          				Logger.log(LEVEL.ERROR, "Error processing key right event. Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
          		   }
         	   }
         	   if (e.getKeyCode() == KeyEvent.VK_LEFT) {
          		   try {
          			   ChartElement ce = getChartElement();
          			   for (int i=ce.series.length-1; i>=0; i--) {
          				   if (ce.series[i].enable) {
          					   ce.series[i].enable = false;
          					   break;
          				   }
          			   }
          			   JFreeChart ch2 = (new CreateChart(ce)).getChart();
          			   chartPanel.setChart(ch2);
          		   } catch (Exception exc) {
         				Logger.log(LEVEL.ERROR, "Error processing key left event. Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
          		   }
         	   }
         	   if (e.getKeyCode() == KeyEvent.VK_DOWN) {
          		   try {
          			   decreaseFontSize();
          			   JFreeChart ch2 = (new CreateChart(getChartElement())).getChart();
          			   chartPanel.setChart(ch2);
          		   } catch (Exception exc) {
         				Logger.log(LEVEL.ERROR, "Error processing key down event. Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
          		   }
      		   }
         	   if (e.getKeyCode() == KeyEvent.VK_UP) {
      			   try {
      				   increaseFontSize();
          			   JFreeChart ch2 = (new CreateChart(getChartElement())).getChart();
          			   chartPanel.setChart(ch2);
          		   } catch (Exception exc) {
         				Logger.log(LEVEL.ERROR, "Error processing key up event. Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
          		   }
      		   }
         	   if (e.getKeyCode() == KeyEvent.VK_Q) chartPanel.getParent().requestFocusInWindow();
            }
      });
	}

	private Picture p = null;
	private transient ChartPanel chartPanel = null;

	/**
	 * Shows the chart in an external panel.
	 * @param show True to create and show the window. If false,
	 * a later call to {@link Picture#show(String)} or
	 * {@link Picture#show(int, int, String, boolean, boolean, boolean)}
	 * is needed.
	 * @throws JPARSECException If an error occurs.
	 * @return The picture object.
	 */
	public Picture showChart(boolean show)
	throws JPARSECException {
		p = new Picture(this.chartAsBufferedImage(
				this.chart_elem.imageWidth,
				this.chart_elem.imageHeight));
		Graphics g = p.getImage().getGraphics();
		this.paintChart(g);
		if (show) p.show(this.chart_elem.title);
		return p;
	}

	/**
	 * Shows the chart in an external panel with a Menu to save the image.
	 * @throws JPARSECException If an error occurs.
	 */
	public void showChartWithMenu()
	throws JPARSECException {
		p = new Picture(this.chartAsBufferedImage(
				this.chart_elem.imageWidth,
				this.chart_elem.imageHeight));
		Graphics g = p.getImage().getGraphics();
		this.paintChart(g);
		p.show(this.chart_elem.imageWidth, this.chart_elem.imageHeight, this.chart_elem.title,
				false, true, true);
	}

	/**
	 * Creates a frame with the chart.
	 *
	 * @param chart_elem Chart element.
	 * @throws JPARSECException If an error occurs.
	 */
	public CreateChart(SimpleChartElement chart_elem) throws JPARSECException
	{
		this.chart_elem = ChartElement.parseSimpleChartElement(chart_elem);
		JFreeChart chart = createChart(this.chart_elem);
		this.setChart(chart);
	}

	/**
	 * Creates a frame with the chart.
	 *
	 * @param chart_elem Chart element.
	 * @throws JPARSECException If an error occurs.
	 */
	public CreateChart(ChartElement chart_elem) throws JPARSECException
	{
		ChartElement c = chart_elem.clone();
		this.chart_elem = c;
		JFreeChart chart = createChart(c);
		this.setChart(chart);
	}

	private static int increaseFontSize = 0;
	private JFreeChart createChart(ChartElement chart_elem) throws JPARSECException
	{
		try {
		switch (chart_elem.chartType)
		{
		case PIE_CHART:
			// Only the first series is considered
			boolean seeLegend = false;
			if (chart_elem.series[0].showLegend) seeLegend = true;
			DefaultPieDataset pieDataset = new DefaultPieDataset();
			for (int i = 0; i < chart_elem.series[0].xValues.length; i++)
			{
				if (chart_elem.xForCategoryCharts != null) {
					pieDataset.setValue(chart_elem.xForCategoryCharts[i], new Double(chart_elem.series[0].yValues[i]));
				} else {
					pieDataset.setValue(chart_elem.series[0].xValues[i], new Double(chart_elem.series[0].yValues[i]));
				}
			}

			JFreeChart jfp = null;
			switch (chart_elem.subType)
			{
			case PIE_3D:
				jfp = ChartFactory.createPieChart3D(chart_elem.title, pieDataset, seeLegend,
						true, true);
				break;
			case PIE_DEFAULT:
				jfp = ChartFactory.createPieChart(chart_elem.title, pieDataset, seeLegend,
						true, true);
				break;
			case PIE_RING:
				jfp = ChartFactory.createRingChart(chart_elem.title, pieDataset, seeLegend,
						true, true);
				break;
			default:
				throw new JPARSECException("invalid chart subtype.");
			}

			if (increaseFontSize != 0) {
				Font f = jfp.getLegend().getItemFont();
				f = f.deriveFont(f.getStyle(), f.getSize()+increaseFontSize);
				jfp.getLegend().setItemFont(f);
				f = jfp.getTitle().getFont();
				f = f.deriveFont(f.getStyle(), f.getSize()+increaseFontSize);
				jfp.getTitle().setFont(f);
			}

			if (chart_elem.subType == ChartElement.SUBTYPE.PIE_3D)
			{
				PiePlot3D plot = (PiePlot3D) jfp.getPlot();
				plot.setBackgroundPaint(chart_elem.backgroundGradient);
				if (increaseFontSize != 0) {
					Font f = plot.getLabelFont();
					f = f.deriveFont(f.getStyle(), f.getSize()+increaseFontSize);
					plot.setLabelFont(f);
					f = plot.getNoDataMessageFont();
					f = f.deriveFont(f.getStyle(), f.getSize()+increaseFontSize);
					plot.setNoDataMessageFont(f);
				}
				for (int i = 0; i < chart_elem.series[0].xValues.length; i++)
				{
					plot.setLegendItemShape(chart_elem.series[0].shape);
					plot.setSectionOutlinesVisible(chart_elem.series[0].showLegend);
					if (chart_elem.series[0].useCustomColorsInPieCharts)
						plot.setSectionPaint(chart_elem.series[0].xValues[i],
								chart_elem.series[0].colorsForPieCharts[i]);
				}
				if (chart_elem.showBackgroundImage)
				{
					chart_elem.backgroundGradient = new Color(255, 255, 255, 0);
					jfp.getLegend().setBackgroundPaint(chart_elem.backgroundGradient);
					if (chart_elem.showBackgroundImageOnlyInDataArea)
					{
						plot.setBackgroundImage(chart_elem.backgroundImage);
						plot.setBackgroundImageAlpha(1f);
					} else
					{
						jfp.setBackgroundImage(chart_elem.backgroundImage);
						jfp.setBackgroundImageAlpha(1f);
					}
				}
			} else
			{
				if (chart_elem.subType == ChartElement.SUBTYPE.PIE_DEFAULT)
				{
					PiePlot plot = (PiePlot) jfp.getPlot();
					plot.setBackgroundPaint(chart_elem.backgroundGradient);
					if (increaseFontSize != 0) {
						Font f = plot.getLabelFont();
						f = f.deriveFont(f.getStyle(), f.getSize()+increaseFontSize);
						plot.setLabelFont(f);
						f = plot.getNoDataMessageFont();
						f = f.deriveFont(f.getStyle(), f.getSize()+increaseFontSize);
						plot.setNoDataMessageFont(f);
					}
					for (int i = 0; i < chart_elem.series[0].xValues.length; i++)
					{
						plot.setLegendItemShape(chart_elem.series[0].shape);
						plot.setSectionOutlinesVisible(chart_elem.series[0].showLegend);
						if (chart_elem.series[0].useCustomColorsInPieCharts)
							plot.setSectionPaint(chart_elem.series[0].xValues[i],
									chart_elem.series[0].colorsForPieCharts[i]);
					}
					if (chart_elem.showBackgroundImage)
					{
						chart_elem.backgroundGradient = new Color(255, 255, 255, 0);
						jfp.getLegend().setBackgroundPaint(chart_elem.backgroundGradient);
						if (chart_elem.showBackgroundImageOnlyInDataArea)
						{
							plot.setBackgroundImage(chart_elem.backgroundImage);
							plot.setBackgroundImageAlpha(1f);
						} else
						{
							jfp.setBackgroundImage(chart_elem.backgroundImage);
							jfp.setBackgroundImageAlpha(1f);
						}
					}
				} else
				{
					RingPlot plot = (RingPlot) jfp.getPlot();
					plot.setBackgroundPaint(chart_elem.backgroundGradient);
					if (increaseFontSize != 0) {
						Font f = plot.getLabelFont();
						f = f.deriveFont(f.getStyle(), f.getSize()+increaseFontSize);
						plot.setLabelFont(f);
						f = plot.getNoDataMessageFont();
						f = f.deriveFont(f.getStyle(), f.getSize()+increaseFontSize);
						plot.setNoDataMessageFont(f);
					}
					for (int i = 0; i < chart_elem.series[0].xValues.length; i++)
					{
						plot.setLegendItemShape(chart_elem.series[0].shape);
						plot.setSeparatorsVisible(chart_elem.series[0].showLines);
						plot.setSectionOutlinesVisible(chart_elem.series[0].showLegend);
						if (chart_elem.series[0].useCustomColorsInPieCharts)
							plot.setSectionPaint(chart_elem.series[0].xValues[i],
									chart_elem.series[0].colorsForPieCharts[i]);
					}
					if (chart_elem.showBackgroundImage)
					{
						chart_elem.backgroundGradient = new Color(255, 255, 255, 0);
						jfp.getLegend().setBackgroundPaint(chart_elem.backgroundGradient);
						if (chart_elem.showBackgroundImageOnlyInDataArea)
						{
							plot.setBackgroundImage(chart_elem.backgroundImage);
							plot.setBackgroundImageAlpha(1f);
						} else
						{
							jfp.setBackgroundImage(chart_elem.backgroundImage);
							jfp.setBackgroundImageAlpha(1f);
						}
					}
				}
			}

			jfp.setBackgroundPaint(chart_elem.backgroundGradient);
			return jfp;

		case XY_CHART:
			boolean flag_problem_logxaxis = false,
			flag_problem_logyaxis = false;
			boolean showLegend = false;
			XYIntervalSeriesCollection xyDataset = null;
			if (chart_elem.subType != SUBTYPE.XY_TIME) xyDataset = new XYIntervalSeriesCollection();
			TimeSeriesCollection timeData = null;
			if (chart_elem.subType == SUBTYPE.XY_TIME) timeData = new TimeSeriesCollection();
			double y_min = 0.0;
			//double epoch = new AstroDate(1970, AstroDate.JANUARY, 1).jd();
			for (int nser = 0; nser < chart_elem.series.length; nser++)
			{
		        TimeSeries timeSeries = null;
		        if (chart_elem.subType == SUBTYPE.XY_TIME) timeSeries = new TimeSeries(chart_elem.series[nser].legend, Second.class);

		        String legend = chart_elem.series[nser].legend;
				if (legend == null)
					legend = "";
				XYIntervalSeries series = null;
				if (chart_elem.subType != SUBTYPE.XY_TIME) series = new XYIntervalSeries(legend.trim());
				double x_val[] = (double[]) DataSet.getDoubleValuesIncludingLimits(chart_elem.series[nser].xValues)
						.get(0);
				double y_val[] = (double[]) DataSet.getDoubleValuesIncludingLimits(chart_elem.series[nser].yValues)
						.get(0);

				try {
					double thisymin = DataSet.getMinimumValue(y_val);
	
					if (nser == 0 || thisymin < y_min)
						y_min = thisymin;
				} catch (Exception exc) {}

				double dx_val[] = chart_elem.series[nser].dxValues;
				double dy_val[] = chart_elem.series[nser].dyValues;
				for (int i = 0; i < x_val.length; i++)
				{
					boolean flag1 = false, flag2 = false;
					double dx = 0.0, dy = 0.0;
					double minx = x_val[i];
					double miny = y_val[i];
					if (chart_elem.series[nser].showErrorBars 
							&& (dx_val != null || dy_val != null))
					{
						if (dx_val != null)
							dx = dx_val[i];
						if (dy_val != null)
							dy = dy_val[i];
						minx = x_val[i] - dx;
						if (minx <= 0.0 && chart_elem.xAxisInLogScale)
							flag_problem_logxaxis = true;
						if (minx < chart_elem.series[nser].xMinimumValue && chart_elem.xAxisInLogScale)
						{
							if (dx > 0.0)
								minx = chart_elem.series[nser].xMinimumValue;
							flag1 = true;
						}
						miny = y_val[i] - dy;
						if (miny <= 0.0 && chart_elem.yAxisInLogScale)
							flag_problem_logyaxis = true;
						if (miny < chart_elem.series[nser].yMinimumValue && chart_elem.yAxisInLogScale)
						{
							if (dy > 0.0)
								miny = chart_elem.series[nser].yMinimumValue;
							flag2 = true;
						}
					}
					if (series != null && !flag1 && !flag2)
					{
						series.add(x_val[i], minx, x_val[i] + dx, y_val[i], miny, y_val[i] + dy);
					}

					if (chart_elem.subType == ChartElement.SUBTYPE.XY_TIME)
					{
						double jd = DataSet.getDoubleValueWithoutLimit(chart_elem.series[nser].xValues[i]);
						AstroDate astro = new AstroDate(jd);
						Date date = new Date(astro.getYear()-1900, astro.getMonth()-1, astro.getDay(), astro.getHour(), astro.getMinute(), astro.getRoundedSecond()); //(long) ((astro.jd()-epoch)*Constant.MILLISECONDS_PER_HOUR*24.0));
						timeSeries.addOrUpdate(
								new Second(date),
								y_val[i]);
					}
				}

				if (xyDataset != null) xyDataset.addSeries(series);
				if (timeData != null) timeData.addSeries(timeSeries);
				if (chart_elem.series[nser].showLegend)
					showLegend = true;
			}

			PlotOrientation pl = PlotOrientation.VERTICAL;
			if (chart_elem.changeOrientationToHorizontal)
				pl = PlotOrientation.HORIZONTAL;

			JFreeChart jf = null;

			switch (chart_elem.subType)
			{
			case XY_TIME:
				jf = ChartFactory.createTimeSeriesChart(chart_elem.title, chart_elem.xLabel,
		        		chart_elem.yLabel, timeData, showLegend, true, true);
				break;
			case XY_AREA:
				jf = ChartFactory.createXYAreaChart(chart_elem.title, chart_elem.xLabel, chart_elem.yLabel,
						xyDataset, pl, showLegend,
						true, true);
				break;
			case XY_LINE:
				jf = ChartFactory.createXYLineChart(chart_elem.title, chart_elem.xLabel, chart_elem.yLabel,
						xyDataset, pl, showLegend,
						true, true);
				break;
			case XY_STEP:
				jf = ChartFactory.createXYStepChart(chart_elem.title, chart_elem.xLabel, chart_elem.yLabel,
						xyDataset, pl, showLegend,
						true, true);
				break;
			case XY_STEP_AREA:
				jf = ChartFactory.createXYStepAreaChart(chart_elem.title, chart_elem.xLabel, chart_elem.yLabel,
						xyDataset, pl, showLegend,
						true, true);
				break;
			case XY_POLAR:
				jf = ChartFactory.createPolarChart(chart_elem.title,
						xyDataset,
						showLegend,
						true, true);
				return jf;
			case XY_SCATTER:
				jf = ChartFactory.createScatterPlot(chart_elem.title, chart_elem.xLabel, chart_elem.yLabel,
						xyDataset, pl, showLegend,
						true, true);
				break;
			}
			XYPlot plot = (XYPlot) jf.getPlot();
			ValueAxis y_axis = plot.getRangeAxis();
			ValueAxis x_axis = plot.getDomainAxis();
			if (chart_elem.xAxisInverted) x_axis.setInverted(true);
			if (chart_elem.yAxisInverted) y_axis.setInverted(true);
			double y_down = y_axis.getLowerBound();
			double y_up = y_axis.getUpperBound();

			if (increaseFontSize != 0) {
				Font f = x_axis.getLabelFont();
				f = f.deriveFont(f.getStyle(), f.getSize()+increaseFontSize);
				x_axis.setLabelFont(f);
				f = x_axis.getTickLabelFont();
				f = f.deriveFont(f.getStyle(), f.getSize()+increaseFontSize);
				x_axis.setTickLabelFont(f);
				if (jf.getLegend() != null) {
					f = jf.getLegend().getItemFont();
					f = f.deriveFont(f.getStyle(), f.getSize()+increaseFontSize);
					jf.getLegend().setItemFont(f);
				}
				if (jf.getTitle() != null) {
					f = jf.getTitle().getFont();
					f = f.deriveFont(f.getStyle(), f.getSize()+increaseFontSize);
					jf.getTitle().setFont(f);
				}
			}
			if (increaseFontSize != 0) {
				Font f = y_axis.getLabelFont();
				f = f.deriveFont(f.getStyle(), f.getSize()+increaseFontSize);
				y_axis.setLabelFont(f);
				f = y_axis.getTickLabelFont();
				f = f.deriveFont(f.getStyle(), f.getSize()+increaseFontSize);
				y_axis.setTickLabelFont(f);
			}

			if (chart_elem.subType != ChartElement.SUBTYPE.XY_POLAR)
			{
				LogarithmicAxis xAxis = new LogarithmicAxis(chart_elem.xLabel);
				LogarithmicAxis yAxis = new LogarithmicAxis(chart_elem.yLabel);
				if (chart_elem.xAxisInverted) xAxis.setInverted(true);
				if (chart_elem.yAxisInverted) yAxis.setInverted(true);

				if (increaseFontSize != 0) {
					Font f = xAxis.getLabelFont();
					f = f.deriveFont(f.getStyle(), f.getSize()+increaseFontSize);
					xAxis.setLabelFont(f);
					f = xAxis.getTickLabelFont();
					f = f.deriveFont(f.getStyle(), f.getSize()+increaseFontSize);
					xAxis.setTickLabelFont(f);
				}
				if (increaseFontSize != 0) {
					Font f = yAxis.getLabelFont();
					f = f.deriveFont(f.getStyle(), f.getSize()+increaseFontSize);
					yAxis.setLabelFont(f);
					f = yAxis.getTickLabelFont();
					f = f.deriveFont(f.getStyle(), f.getSize()+increaseFontSize);
					yAxis.setTickLabelFont(f);
				}

				if (flag_problem_logxaxis)
					xAxis.setAllowNegativesFlag(true);
				if (flag_problem_logyaxis)
					yAxis.setAllowNegativesFlag(true);
				if (chart_elem.xTickLabels == ChartElement.TICK_LABELS.EXPONENTIAL_VALUES)
					xAxis.setExpTickLabelsFlag(true);
				if (chart_elem.xTickLabels == ChartElement.TICK_LABELS.LOGARITHM_VALUES)
					xAxis.setLog10TickLabelsFlag(true);
				if (chart_elem.yTickLabels == ChartElement.TICK_LABELS.EXPONENTIAL_VALUES)
					yAxis.setExpTickLabelsFlag(true);
				if (chart_elem.yTickLabels == ChartElement.TICK_LABELS.LOGARITHM_VALUES)
					yAxis.setLog10TickLabelsFlag(true);

				if (y_down <= 0 && chart_elem.yAxisInLogScale)
				{
					y_down = chart_elem.series[0].yMinimumValue;
					yAxis.setRange(y_down, y_up);
				}

				if (chart_elem.xAxisInLogScale)
					plot.setDomainAxis(xAxis);
				if (chart_elem.yAxisInLogScale)
					plot.setRangeAxis(yAxis);

			}

			XYErrorRenderer renderer = new XYErrorRenderer();
			XYLineAndShapeRenderer renderer2 = new XYLineAndShapeRenderer();
			XYAreaRenderer renderer3 = null;
			XYStepAreaRenderer renderer4 = null;
			XYStepRenderer renderer5 = null;
			renderer.setCapLength(2);

			if (chart_elem.subType == ChartElement.SUBTYPE.XY_STEP ||
					chart_elem.subType == ChartElement.SUBTYPE.XY_STEP_AREA ||
					chart_elem.subType == ChartElement.SUBTYPE.XY_AREA) {
				renderer3 = new XYAreaRenderer();
				renderer4 = new XYStepAreaRenderer();
				renderer5 = new XYStepRenderer();
			}
			int series_number = chart_elem.series.length;
			for (int i = chart_elem.series.length - 1; i >= 0; i--)
			{
				if (chart_elem.series[i].enable) {
					renderer.setSeriesShapesVisible(i, chart_elem.series[i].showShapes);
					renderer.setSeriesLinesVisible(i, chart_elem.series[i].showLines);
					if (chart_elem.series[i].stroke != null) renderer.setSeriesStroke(i, AWTGraphics.getStroke(chart_elem.series[i].stroke));
					renderer.setSeriesVisibleInLegend(i, new Boolean(chart_elem.series[i].showLegend));
					renderer.setSeriesPaint(i, chart_elem.series[i].color);
					renderer.setSeriesShape(i, chart_elem.series[i].shape);
					renderer.setDrawXError(chart_elem.showErrorBars);
					renderer.setDrawYError(chart_elem.showErrorBars);
					renderer.setSeriesFillPaint(i, chart_elem.series[i].color);

					renderer2.setSeriesFillPaint(i, chart_elem.series[i].color);
					renderer2.setSeriesShapesVisible(i, chart_elem.series[i].showShapes);
					renderer2.setSeriesLinesVisible(i, chart_elem.series[i].showLines);
					if (chart_elem.series[i].stroke != null) renderer2.setSeriesStroke(i, AWTGraphics.getStroke(chart_elem.series[i].stroke));
					renderer2.setSeriesVisibleInLegend(i, new Boolean(chart_elem.series[i].showLegend));
					renderer2.setSeriesPaint(i, chart_elem.series[i].color);
					renderer2.setSeriesShape(i, chart_elem.series[i].shape);

					if (renderer3 != null) {
						renderer3.setSeriesFillPaint(i, chart_elem.series[i].color);
						if (chart_elem.series[i].stroke != null) renderer3.setSeriesStroke(i, AWTGraphics.getStroke(chart_elem.series[i].stroke));
						renderer3.setSeriesVisibleInLegend(i, new Boolean(chart_elem.series[i].showLegend));
						renderer3.setSeriesPaint(i, chart_elem.series[i].color);
						renderer3.setSeriesShape(i, chart_elem.series[i].shape);

						renderer4.setSeriesFillPaint(i, chart_elem.series[i].color);
						if (chart_elem.series[i].stroke != null) renderer4.setSeriesStroke(i, AWTGraphics.getStroke(chart_elem.series[i].stroke));
						renderer4.setSeriesVisibleInLegend(i, new Boolean(chart_elem.series[i].showLegend));
						renderer4.setSeriesPaint(i, chart_elem.series[i].color);
						renderer4.setSeriesShape(i, chart_elem.series[i].shape);

						renderer5.setSeriesFillPaint(i, chart_elem.series[i].color);
						renderer5.setSeriesShapesVisible(i, chart_elem.series[i].showShapes);
						renderer5.setSeriesLinesVisible(i, chart_elem.series[i].showLines);
						if (chart_elem.series[i].stroke != null) renderer5.setSeriesStroke(i, AWTGraphics.getStroke(chart_elem.series[i].stroke));
						renderer5.setSeriesVisibleInLegend(i, new Boolean(chart_elem.series[i].showLegend));
						renderer5.setSeriesPaint(i, chart_elem.series[i].color);
						renderer5.setSeriesShape(i, chart_elem.series[i].shape);
					}

					ArrayList<Object> vx = DataSet.getDoubleValuesIncludingLimits(chart_elem.series[i].xValues);
					ArrayList<Object> vy = DataSet.getDoubleValuesIncludingLimits(chart_elem.series[i].yValues);
					double x_val[] = (double[]) vx.get(0);
					double y_val[] = (double[]) vy.get(0);
					int x_limit[] = (int[]) vx.get(vx.size()-1);
					int y_limit[] = (int[]) vy.get(vy.size()-1);
					for (int ii = 0; ii < x_val.length; ii++)
					{
						double angle = 0.0;
						if (y_limit[ii] == 1)
							angle = -Math.PI * 0.5;
						if (y_limit[ii] == -1)
							angle = Math.PI * 0.5;
						if (y_limit[ii] != 0)
						{
							XYPointerAnnotation pointer = new XYPointerAnnotation("", x_val[ii], y_val[ii], angle);
							pointer.setBaseRadius(0.0);
							pointer.setTipRadius(-chart_elem.series[i].sizeOfArrowInLimits);
							pointer.setArrowPaint(chart_elem.series[i].color);
							pointer.setPaint(chart_elem.series[i].color);
							plot.addAnnotation(pointer);
						}
						if (x_limit[ii] == 1)
							angle = 0;
						if (x_limit[ii] == -1)
							angle = Math.PI;
						if (x_limit[ii] != 0)
						{
							XYPointerAnnotation pointer = new XYPointerAnnotation("", x_val[ii], y_val[ii], angle);
							pointer.setBaseRadius(0.0);
							pointer.setTipRadius(-chart_elem.series[i].sizeOfArrowInLimits);
							pointer.setArrowPaint(chart_elem.series[i].color);
							pointer.setPaint(chart_elem.series[i].color);
							plot.addAnnotation(pointer);
						}

						int npo = 0;
						try { npo = chart_elem.series[i].pointers.length; } catch (Exception exc) {}
						if (npo > 0 && ii == 0)
						{
							for (int iii = 0; iii < chart_elem.series[i].pointers.length; iii++)
							{
								if (chart_elem.series[i].pointers[iii] != null)
								{
									String pointerMsg = chart_elem.series[i].pointers[iii];
									double pointerX = 0.0, pointerY = 0.0;
									if (pointerMsg.startsWith("(")) {
										int pp = pointerMsg.indexOf(")");
										String ppp = pointerMsg.substring(1, pp);
										int coma = ppp.indexOf(",");
										String p1 = ppp.substring(0, coma);
										String p2 = ppp.substring(coma+1);
										String msg = pointerMsg.substring(pp+1).trim();
										pointerMsg = "";
										for (int ijk=0; ijk<chart_elem.series[i].xValues.length; ijk++)
										{
											if (p1.trim().equals(chart_elem.series[i].xValues[ijk]) &&
													p2.trim().equals(chart_elem.series[i].yValues[ijk])) {
												pointerMsg = msg;
												pointerX = DataSet.getDoubleValueWithoutLimit(p1);
												pointerY = DataSet.getDoubleValueWithoutLimit(p2);
												break;
											}
										}
										if (pointerMsg.equals("")) {
											pointerX = DataSet.getDoubleValueWithoutLimit(p1);
											pointerY = DataSet.getDoubleValueWithoutLimit(p2);
											pointerMsg = msg;
										}
									} else {
										int n = Integer.parseInt(FileIO.getField(1, pointerMsg, " ", true));
										pointerX = DataSet.getDoubleValueWithoutLimit(chart_elem.series[i].xValues[n-1]);
										pointerY = DataSet.getDoubleValueWithoutLimit(chart_elem.series[i].yValues[n-1]);
										pointerMsg = FileIO.getRestAfterField(1, pointerMsg, " ", true);
									}

										Rectangle2D rect = new Rectangle2D.Double(0, 0, chart_elem.imageWidth, chart_elem.imageHeight); //this.getComponent().getScreenDataArea();
										double posx = x_axis
												.valueToJava2D(pointerX, rect, plot
														.getDomainAxisEdge());
										double posy = y_axis
												.valueToJava2D(pointerY, rect, plot
														.getRangeAxisEdge());
										angle = jparsec.ephem.Functions.normalizeRadians(Math.atan2(
												(rect.getCenterX() - posy),
												(rect.getCenterY() - posx)));

										ChartSeriesElement.POINTER_ANGLE ang = chart_elem.series[i].pointersAngle;

										if (ang == ChartSeriesElement.POINTER_ANGLE.AVOID_SUPERIMPOSED_STRINGS) {
											ang = this.getMostEmptyDirection(chart_elem, posx, posy, plot);
										}
										if (ang == ChartSeriesElement.POINTER_ANGLE.DOWNWARDS) angle = Math.PI  * 0.5;
										if (ang == ChartSeriesElement.POINTER_ANGLE.UPWARDS) angle = -Math.PI * 0.5;
										if (ang == ChartSeriesElement.POINTER_ANGLE.LEFTWARDS) angle = Math.PI;
										if (ang == ChartSeriesElement.POINTER_ANGLE.RIGHTWARDS) angle = 0.0;
										if (ang == ChartSeriesElement.POINTER_ANGLE.TO_OUTSIDE) angle = -angle;
										if (ang != ChartSeriesElement.POINTER_ANGLE.DOWNWARDS &&
												ang != ChartSeriesElement.POINTER_ANGLE.UPWARDS &&
												ang != ChartSeriesElement.POINTER_ANGLE.LEFTWARDS &&
												ang != ChartSeriesElement.POINTER_ANGLE.RIGHTWARDS &&
												ang != ChartSeriesElement.POINTER_ANGLE.TO_OUTSIDE &&
												ang != ChartSeriesElement.POINTER_ANGLE.TO_CENTER) angle = 0; //-ang;

										if (pointerMsg.startsWith("@UP")) {
											angle = -Math.PI * 0.5;
											pointerMsg = pointerMsg.substring(3);
										}
										if (pointerMsg.startsWith("@DOWN")) {
											angle = Math.PI * 0.5;
											pointerMsg = pointerMsg.substring(5);
										}
										if (pointerMsg.startsWith("@LEFT")) {
											angle = Math.PI;
											pointerMsg = pointerMsg.substring(5);
										}
										if (pointerMsg.startsWith("@RIGHT")) {
											angle = 0;
											pointerMsg = pointerMsg.substring(6);
										}
										if (pointerMsg.startsWith("@CENTER")) {
											pointerMsg = pointerMsg.substring(8);
										}

										String text = pointerMsg;
										if (angle == Math.PI) text = "@CENTER"+text;
										if (angle == 0) text = "@LEFTPLUS" + text;

										double label_pos = 15.0 * chart_elem.series[i].pointersLabelOffsetFactor;
										XYPointerAnnotation pointer = new XYPointerAnnotation(text, pointerX, pointerY,
												angle);
										pointer.setBaseRadius(20.0);
										pointer.setTipRadius(10.0);
										pointer.setPaint(chart_elem.series[i].color);
										pointer.setArrowPaint(chart_elem.series[i].color);
										pointer.setFont(plot.getDomainAxis().getLabelFont().deriveFont(10f));
										pointer.setTextAnchor(TextAnchor.CENTER);
										pointer.setLabelOffset(label_pos);
										if (!chart_elem.series[i].showArrowInPointers)
										{
											pointer.setArrowLength(0);
											pointer.setTipRadius(0);
											pointer.setBaseRadius(0);
										}
										plot.addAnnotation(pointer);
								}
							}
						}
					}

					if (xyDataset != null && (chart_elem.series[i].regressionType == ChartSeriesElement.REGRESSION.LINEAR ||
							chart_elem.series[i].regressionType == ChartSeriesElement.REGRESSION.POLYNOMIAL ||
							chart_elem.series[i].regressionType == ChartSeriesElement.REGRESSION.GENERIC_FIT ||
							chart_elem.series[i].regressionType == ChartSeriesElement.REGRESSION.REGRESSION_CUSTOM) &&
							chart_elem.series[i].regressionType.getShowRegression() && chart_elem.series[i].enable != false)
					{
						double dx_val[] = chart_elem.series[i].dxValues;
						double dy_val[] = chart_elem.series[i].dyValues;

						if (dx_val == null) {
							dx_val = new double[chart_elem.series[i].xValues.length];
							for (int k=0; k<dx_val.length;k++)
							{
								dx_val[k] = 0.0;
							}
						}
						if (dy_val == null) {
							dy_val = new double[chart_elem.series[i].xValues.length];
							for (int k=0; k<dy_val.length;k++)
							{
								dy_val[k] = 0.0;
							}
						}

						// Fit in LOG scale if necessary
						double x2[] = new double[x_val.length];
						double y2[] = new double[y_val.length];
						double dx2[] = new double[dx_val.length];
						double dy2[] = new double[dy_val.length];
						for (int j = 0; j < x_val.length; j++)
						{
							x2[j] = x_val[j];
							y2[j] = y_val[j];
							dx2[j] = dx_val[j];
							dy2[j] = dy_val[j];
							if (chart_elem.xAxisInLogScale && dx_val != null)
								dx2[j] = dx_val[j] * Math.log10(Math.E) / x_val[j];
							if (chart_elem.yAxisInLogScale && dy_val != null)
								dy2[j] = Math.abs(dy_val[j] * Math.log10(Math.E) / y_val[j]);
							if (chart_elem.xAxisInLogScale)
								x2[j] = Math.log10(x_val[j]);
							if (chart_elem.yAxisInLogScale)
								y2[j] = Math.log10(y_val[j]);
						}

						LinearFit myfit = null; //chart_elem.series[i].regressionType.getLinearFit();
						Polynomial pol = null; //chart_elem.series[i].regressionType.getPolynomialFit();
						String[] gff = null; //chart_elem.series[i].regressionType.getGenericFitFunctions();
						GenericFit gf = null;
						String function = null;
						if (gff != null && gff.length >= 3) gf = new GenericFit(x2, y2, gff[0], gff[1], gff[2]);
						if (chart_elem.series[i].regressionType == ChartSeriesElement.REGRESSION.LINEAR) {
							if (myfit == null)
							{
								myfit = new LinearFit(x2, y2, dx2, dy2);
								myfit.linearFit();
								chart_elem.series[i].regressionType.setEquationFromLinearFit(myfit);
							}
						} else {
							if (chart_elem.series[i].regressionType == ChartSeriesElement.REGRESSION.POLYNOMIAL) {
								if (pol == null) {
									Regression reg = new Regression(x2, y2);
									reg.polynomial(chart_elem.series[i].regressionType.getPolynomialDegree());
									chart_elem.series[i].regressionType.setEquationValues(reg.getBestEstimates(), reg.getBestEstimatesErrors());
									pol = new Polynomial(reg.getBestEstimates());
								}
							} else {
								if (chart_elem.series[i].regressionType == ChartSeriesElement.REGRESSION.REGRESSION_CUSTOM) {
									function = chart_elem.series[i].regressionType.getCustomRegressionFitFunction();
									if (!chart_elem.series[i].regressionType.regressionDone()) {
										Regression reg = new Regression(x2, y2);
										reg.customFunction(function, chart_elem.series[i].regressionType.getCustomRegressionFitInitialEstimates());
										chart_elem.series[i].regressionType.setEquationValues(reg.getBestEstimates(), reg.getBestEstimatesErrors());
										if (chart_elem.series[i].regressionType.getEquation() == null || chart_elem.series[i].regressionType.getEquation().equals(""))
											chart_elem.series[i].regressionType.setEquation(function);
									}
								} else {
									gf.fit();
									if (chart_elem.series[i].regressionType.getEquation() != null) {
										if (chart_elem.series[i].regressionType.getEquation().equals("")) chart_elem.series[i].regressionType.setEquation(gf.getFunction());
									} else {
										chart_elem.series[i].regressionType.setEquation(gf.getFunction());
									}
								}
							}
						}

						double xmax = chart_elem.getxMax();
						double xmin = chart_elem.getxMin();
						double ymax = chart_elem.getyMax();
						double ymin = chart_elem.getyMin();
						if (chart_elem.xAxisInLogScale || chart_elem.yAxisInLogScale) {
							if (ymin < chart_elem.series[i].yMinimumValue)
								ymin = chart_elem.series[i].yMinimumValue;
							if (xmin < chart_elem.series[i].xMinimumValue)
								xmin = chart_elem.series[i].xMinimumValue;
						}

						XYIntervalSeries series = new XYIntervalSeries(chart_elem.series[i].regressionType.getEquation());
						int np = 2*chart_elem.series[i].xValues.length;
						double xx[] = DataSet.getSetOfValues(xmin, xmax, np, chart_elem.xAxisInLogScale);
						for (int jj=0; jj<np; jj++)
						{
							double p[] = null;
							if (chart_elem.series[i].regressionType == ChartSeriesElement.REGRESSION.LINEAR) {
								p = CreateChart.getPoint(chart_elem, myfit, i, xx[jj], ymax, ymin);
							} else {
								if (chart_elem.series[i].regressionType == ChartSeriesElement.REGRESSION.POLYNOMIAL) {
									p = CreateChart.getPoint(chart_elem, pol, i, xx[jj], ymax, ymin);
								} else {
									if (chart_elem.series[i].regressionType == ChartSeriesElement.REGRESSION.REGRESSION_CUSTOM) {
										p = CreateChart.getPoint(chart_elem, function, chart_elem.series[i].regressionType.getEquationValues(), i, xx[jj], ymax, ymin);
									} else {
										p = CreateChart.getPoint(chart_elem, gf, i, xx[jj], ymax, ymin);
									}
								}
							}
							series.add(p[0], p[0], p[0], p[1], p[1], p[1]);
						}

						xyDataset.addSeries(series);
						series_number++;
						renderer.setSeriesShapesVisible(series_number - 1, false);
						renderer.setSeriesLinesVisible(series_number - 1, true);
						renderer.setSeriesVisibleInLegend(series_number - 1, new Boolean(chart_elem.series[i].regressionType.getShowEquation()));
						renderer.setSeriesPaint(series_number - 1, chart_elem.series[i].color);
						if (chart_elem.series[i].regressionType.getColor() != null) renderer.setSeriesPaint(series_number - 1, chart_elem.series[i].regressionType.getColor());
						if (chart_elem.series[i].stroke != null) renderer.setSeriesStroke(series_number - 1, AWTGraphics.getStroke(chart_elem.series[i].stroke));

						renderer2.setSeriesShapesVisible(series_number - 1, false);
						renderer2.setSeriesLinesVisible(series_number - 1, true);
						renderer2.setSeriesVisibleInLegend(series_number - 1, new Boolean(chart_elem.series[i].regressionType.getShowEquation()));
						renderer2.setSeriesPaint(series_number - 1, chart_elem.series[i].color);
						if (chart_elem.series[i].regressionType.getColor() != null) renderer2.setSeriesPaint(series_number - 1, chart_elem.series[i].regressionType.getColor());
						if (chart_elem.series[i].stroke != null) renderer2.setSeriesStroke(series_number - 1, AWTGraphics.getStroke(chart_elem.series[i].stroke));

						if (renderer3 != null) {
							renderer3.setSeriesVisibleInLegend(series_number - 1, new Boolean(chart_elem.series[i].regressionType.getShowEquation()));
							renderer3.setSeriesPaint(series_number - 1, chart_elem.series[i].color);
							if (chart_elem.series[i].regressionType.getColor() != null) renderer3.setSeriesPaint(series_number - 1, chart_elem.series[i].regressionType.getColor());
							if (chart_elem.series[i].stroke != null) renderer3.setSeriesStroke(series_number - 1, AWTGraphics.getStroke(chart_elem.series[i].stroke));
							renderer4.setSeriesVisibleInLegend(series_number - 1, new Boolean(chart_elem.series[i].regressionType.getShowEquation()));
							renderer4.setSeriesPaint(series_number - 1, chart_elem.series[i].color);
							if (chart_elem.series[i].regressionType.getColor() != null) renderer4.setSeriesPaint(series_number - 1, chart_elem.series[i].regressionType.getColor());
							if (chart_elem.series[i].stroke != null) renderer4.setSeriesStroke(series_number - 1, AWTGraphics.getStroke(chart_elem.series[i].stroke));

							renderer5.setSeriesShapesVisible(series_number - 1, false);
							renderer5.setSeriesLinesVisible(series_number - 1, true);
							renderer5.setSeriesVisibleInLegend(series_number - 1, new Boolean(chart_elem.series[i].regressionType.getShowEquation()));
							renderer5.setSeriesPaint(series_number - 1, chart_elem.series[i].color);
							if (chart_elem.series[i].regressionType.getColor() != null) renderer5.setSeriesPaint(series_number - 1, chart_elem.series[i].regressionType.getColor());
							if (chart_elem.series[i].stroke != null) renderer5.setSeriesStroke(series_number - 1, AWTGraphics.getStroke(chart_elem.series[i].stroke));
						}
					}

					if (xyDataset != null && (chart_elem.series[i].regressionType == ChartSeriesElement.REGRESSION.SPLINE_INTERPOLATION || chart_elem.series[i].regressionType == ChartSeriesElement.REGRESSION.LINEAR_INTERPOLATION) &&
							chart_elem.series[i].xValues.length > 2 && chart_elem.series[i].enable != false)
					{
						// Fit in LOG scale if necessary
						for (int j = 0; j < x_val.length; j++)
						{
							if (chart_elem.xAxisInLogScale)
								x_val[j] = Math.log10(x_val[j]);
							if (chart_elem.yAxisInLogScale)
								y_val[j] = Math.log10(y_val[j]);
						}

						double xmin = DataSet.getMinimumValue(x_val);
						double xmax = DataSet.getMaximumValue(x_val);

						double ymax = y_up;
						double ymin = y_down;
						if (chart_elem.xAxisInLogScale || chart_elem.yAxisInLogScale) {
							if (ymin < chart_elem.series[i].yMinimumValue)
								ymin = chart_elem.series[i].yMinimumValue;
							if (xmin < chart_elem.series[i].xMinimumValue)
								xmin = chart_elem.series[i].xMinimumValue;
						}

						chart_elem.series[i].regressionType.setEquationValues(null, null);
						XYIntervalSeries series = new XYIntervalSeries(chart_elem.series[i].regressionType.getEquation());
						int n = chart_elem.series[i].xValues.length;
						if (chart_elem.series[i].regressionType == ChartSeriesElement.REGRESSION.SPLINE_INTERPOLATION) n *= 5;
						if (n < 100) n = 100;
						double step = Math.abs(xmax - xmin) / (n-1);
						ArrayList<double[]> v = DataSet.sortInCrescent(x_val, y_val, true);
						x_val = v.get(0);
						y_val = v.get(1);
						for (double x = xmin; x <= xmax; x = x + step)
						{
							double px = x, y;

							Interpolation interp = new Interpolation(x_val, y_val, false);
							if (chart_elem.series[i].regressionType == ChartSeriesElement.REGRESSION.SPLINE_INTERPOLATION)
							{
								y = interp.splineInterpolation(px);
							} else
							{
								y = interp.linearInterpolation(px);
							}
							if (chart_elem.yAxisInLogScale)
								y = Math.pow(10.0, y);
							if (chart_elem.xAxisInLogScale)
								px = Math.pow(10.0, px);
							if (y > ymin && y < ymax)
								series.add(px, px, px, y, y, y);
						}

						xyDataset.addSeries(series);
						series_number++;
						renderer.setSeriesShape(series_number - 1, ChartSeriesElement.SHAPE_EMPTY);
						renderer.setSeriesShapesVisible(series_number - 1, false);
						renderer.setSeriesLinesVisible(series_number - 1, true);
						renderer.setSeriesVisibleInLegend(series_number - 1, new Boolean(chart_elem.series[i].regressionType.getShowEquation()));
						renderer.setSeriesPaint(series_number - 1, chart_elem.series[i].color);
						if (chart_elem.series[i].regressionType.getColor() != null) renderer.setSeriesPaint(series_number - 1, chart_elem.series[i].regressionType.getColor());
						if (chart_elem.series[i].stroke != null) renderer.setSeriesStroke(series_number - 1, AWTGraphics.getStroke(chart_elem.series[i].stroke));

						renderer2.setSeriesShape(series_number - 1, ChartSeriesElement.SHAPE_EMPTY);
						renderer2.setSeriesShapesVisible(series_number - 1, false);
						renderer2.setSeriesLinesVisible(series_number - 1, true);
						renderer2.setSeriesVisibleInLegend(series_number - 1, new Boolean(chart_elem.series[i].regressionType.getShowEquation()));
						renderer2.setSeriesPaint(series_number - 1, chart_elem.series[i].color);
						if (chart_elem.series[i].regressionType.getColor() != null) renderer2.setSeriesPaint(series_number - 1, chart_elem.series[i].regressionType.getColor());
						if (chart_elem.series[i].stroke != null) renderer2.setSeriesStroke(series_number - 1, AWTGraphics.getStroke(chart_elem.series[i].stroke));

						if (renderer3 != null) {
							renderer3.setSeriesShape(series_number - 1, ChartSeriesElement.SHAPE_EMPTY);
							renderer3.setSeriesVisibleInLegend(series_number - 1, new Boolean(chart_elem.series[i].regressionType.getShowEquation()));
							renderer3.setSeriesPaint(series_number - 1, chart_elem.series[i].color);
							if (chart_elem.series[i].regressionType.getColor() != null) renderer3.setSeriesPaint(series_number - 1, chart_elem.series[i].regressionType.getColor());
							if (chart_elem.series[i].stroke != null) renderer3.setSeriesStroke(series_number - 1, AWTGraphics.getStroke(chart_elem.series[i].stroke));

							renderer4.setSeriesShape(series_number - 1, ChartSeriesElement.SHAPE_EMPTY);
							renderer4.setSeriesVisibleInLegend(series_number - 1, new Boolean(chart_elem.series[i].regressionType.getShowEquation()));
							renderer4.setSeriesPaint(series_number - 1, chart_elem.series[i].color);
							if (chart_elem.series[i].regressionType.getColor() != null) renderer4.setSeriesPaint(series_number - 1, chart_elem.series[i].regressionType.getColor());
							if (chart_elem.series[i].stroke != null) renderer4.setSeriesStroke(series_number - 1, AWTGraphics.getStroke(chart_elem.series[i].stroke));

							renderer5.setSeriesShape(series_number - 1, ChartSeriesElement.SHAPE_EMPTY);
							renderer5.setSeriesShapesVisible(series_number - 1, false);
							renderer5.setSeriesLinesVisible(series_number - 1, true);
							renderer5.setSeriesVisibleInLegend(series_number - 1, new Boolean(chart_elem.series[i].regressionType.getShowEquation()));
							renderer5.setSeriesPaint(series_number - 1, chart_elem.series[i].color);
							if (chart_elem.series[i].regressionType.getColor() != null) renderer5.setSeriesPaint(series_number - 1, chart_elem.series[i].regressionType.getColor());
							if (chart_elem.series[i].stroke != null) renderer5.setSeriesStroke(series_number - 1, AWTGraphics.getStroke(chart_elem.series[i].stroke));
						}
					}
				}
			}

			renderer.setDrawSeriesLineAsPath(true);
			renderer2.setDrawSeriesLineAsPath(true);
			if (renderer5 != null) renderer5.setDrawSeriesLineAsPath(true);
			if (chart_elem.subType == ChartElement.SUBTYPE.XY_STEP ||
					chart_elem.subType == ChartElement.SUBTYPE.XY_STEP_AREA ||
					chart_elem.subType == ChartElement.SUBTYPE.XY_AREA) {
				if (chart_elem.subType == ChartElement.SUBTYPE.XY_STEP) plot.setRenderer(renderer5);
				if (chart_elem.subType == ChartElement.SUBTYPE.XY_STEP_AREA) plot.setRenderer(renderer4);
				if (chart_elem.subType == ChartElement.SUBTYPE.XY_AREA) plot.setRenderer(renderer3);
			} else {
				if (chart_elem.showErrorBars) {
					plot.setRenderer(renderer);
				} else {
					plot.setRenderer(renderer2);
				}
			}

			if (chart_elem.showBackgroundImage)
			{
				chart_elem.backgroundGradient = new Color(255, 255, 255, 0);
				jf.getLegend().setBackgroundPaint(chart_elem.backgroundGradient);
				if (chart_elem.showBackgroundImageOnlyInDataArea)
				{
					plot.setBackgroundImage(chart_elem.backgroundImage);
					plot.setBackgroundImageAlpha(1f);
				} else
				{
					jf.setBackgroundImage(chart_elem.backgroundImage);
					jf.setBackgroundImageAlpha(1f);
				}
			}
			jf.setBackgroundPaint(chart_elem.backgroundGradient);
			plot.setBackgroundPaint(chart_elem.backgroundGradient);

			for (int nser = chart_elem.series.length-1; nser >= 0; nser--)
			{
				if (chart_elem.series[nser].enable == false) {
					if (xyDataset != null) xyDataset.removeSeries(nser);
					if (timeData != null) timeData.removeSeries(nser);
				}
			}
			return jf;

		case CATEGORY_CHART:
			boolean flag_problem_logyaxis2 = false;
			DefaultCategoryDataset categoryDataset = new DefaultCategoryDataset();
			int ndatasets = 1;
			int serieID[] = new int[ndatasets];
			boolean show_legend = true;
/*			boolean isBar = false;
			if (chart_elem.subType == ChartElement.SUBTYPE.CATEGORY_BAR ||
					chart_elem.subType == ChartElement.SUBTYPE.CATEGORY_BAR_3D ||
					chart_elem.subType == ChartElement.SUBTYPE.CATEGORY_STACKED_BAR ||
					chart_elem.subType == ChartElement.SUBTYPE.CATEGORY_STACKED_BAR_3D)
				isBar = true;
//			if (!isBar) {
*/				for (int i = 0; i < chart_elem.xForCategoryCharts.length; i++)
				{
					categoryDataset.addValue(null, "X_VALUES_FOR_CATEGORY_CHARTS", chart_elem.xForCategoryCharts[i]);
					//System.out.println("category: "+chart_elem.xForCategoryCharts[i]);
				}
//			}
			for (int i=0; i<chart_elem.series.length; i++)
			{
				if (chart_elem.series[i].pointers != null && chart_elem.series[i].enable) {
					for (int j = 0; j < chart_elem.series[i].pointers.length; j++)
					{
						String p = FileIO.getField(1, chart_elem.series[i].pointers[j], " ", true);
						if (!p.startsWith("(")) {
							String pp = FileIO.getRestAfterField(1, chart_elem.series[i].pointers[j], " ", true);
							String px = chart_elem.series[i].xValues[Integer.parseInt(p)-1];
							String py = chart_elem.series[i].yValues[Integer.parseInt(p)-1];
							chart_elem.series[i].pointers[j] = "("+px+","+py+") " + pp;
						}
					}
				}
				ArrayList<String[]> v = DataSet.sort(chart_elem.series[i].xValues, chart_elem.series[i].yValues, null, null, chart_elem.xForCategoryCharts);
				chart_elem.series[i].xValues = v.get(0);
				chart_elem.series[i].yValues = v.get(1);
			}
			ArrayList<int[]> id = new ArrayList<int[]>();
			id.add(new int[] {0, -1});
			int ns = 0;
			for (int i=0; i<chart_elem.series.length; i++)
			{
				String xs[] = DataSet.getStringValuesExcludingLimits(chart_elem.series[i].xValues);
				String legend0 = chart_elem.series[i].legend;
				int nr = 0, nrMax = 0;
				for (int k=0; k<xs.length; k++)
				{
					String legend = legend0;
					if (k > 0) {
						if (xs[k].equals(xs[k-1])) {
							nr ++;
							for (int j=0; j<nr; j++)
							{
								legend += "*";
							}
						} else {
							nr = 0;
						}
						if (nr > nrMax) nrMax = nr;
					}
					categoryDataset.addValue(DataSet.getDoubleValueWithoutLimit(chart_elem.series[i].yValues[k]), legend, xs[k]);
				}
				for (int j=0; j<nrMax+1; j++)
				{
					ns ++;
					id.add(new int[] {ns, i});
				}
			}
			serieID = new int[id.size()];
			for (int i=0; i<serieID.length; i++)
			{
				int s[] = id.get(i);
				serieID[i] = s[1];
			}

			PlotOrientation plo = PlotOrientation.VERTICAL;
			if (chart_elem.changeOrientationToHorizontal)
				plo = PlotOrientation.HORIZONTAL;

			JFreeChart jfc = getCategoryChart(chart_elem, categoryDataset, plo, show_legend);

			if (increaseFontSize != 0) {
				Font f = jfc.getLegend().getItemFont();
				f = f.deriveFont(f.getStyle(), f.getSize()+increaseFontSize);
				jfc.getLegend().setItemFont(f);
				f = jfc.getTitle().getFont();
				f = f.deriveFont(f.getStyle(), f.getSize()+increaseFontSize);
				jfc.getTitle().setFont(f);
			}

			CategoryPlot bar_plot = jfc.getCategoryPlot();

			if (chart_elem.subType == ChartElement.SUBTYPE.CATEGORY_AREA)
			{
				AreaRenderer bar_render = (AreaRenderer) bar_plot.getRenderer();
				bar_render.setSeriesVisibleInLegend(0, new Boolean(false));
				bar_render.setSeriesVisible(0, new Boolean(false));
				for (int i = 1; i < serieID.length; i++)
				{
					bar_render.setSeriesPaint(i, chart_elem.series[serieID[i]].color);
					bar_render.setSeriesShape(i, chart_elem.series[serieID[i]].shape);
					bar_render.setSeriesStroke(i, AWTGraphics.getStroke(chart_elem.series[serieID[i]].stroke));
					bar_render.setSeriesVisible(i, new Boolean(chart_elem.series[serieID[i]].enable));
					bar_render.setSeriesVisibleInLegend(i, new Boolean(false));
					boolean legend = false;
					if (i > 1) {
						if (serieID[i] != serieID[i-1]) legend = true;
					} else {
						legend = true;
					}
					if (legend) bar_render.setSeriesVisibleInLegend(i, new Boolean(chart_elem.series[serieID[i]].showLegend));
				}
			} else
			{
				if (chart_elem.subType == ChartElement.SUBTYPE.CATEGORY_STACKED_AREA)
				{
					StackedAreaRenderer bar_render = (StackedAreaRenderer) bar_plot.getRenderer();
					bar_render.setSeriesVisibleInLegend(0, new Boolean(false));
					bar_render.setSeriesVisible(0, new Boolean(false));
					for (int i = 1; i < serieID.length; i++)
					{
						bar_render.setSeriesPaint(i, chart_elem.series[serieID[i]].color);
						bar_render.setSeriesShape(i, chart_elem.series[serieID[i]].shape);
						bar_render.setSeriesStroke(i, AWTGraphics.getStroke(chart_elem.series[serieID[i]].stroke));
						bar_render.setSeriesVisible(i, new Boolean(chart_elem.series[serieID[i]].enable));
						bar_render.setSeriesVisibleInLegend(i, new Boolean(false));
						boolean legend = false;
						if (i > 1) {
							if (serieID[i] != serieID[i-1]) legend = true;
						} else {
							legend = true;
						}
						if (legend) bar_render.setSeriesVisibleInLegend(i, new Boolean(chart_elem.series[serieID[i]].showLegend));
					}
				} else
				{
					if (chart_elem.subType == ChartElement.SUBTYPE.CATEGORY_LINE)
					{
						LineAndShapeRenderer bar_render = (LineAndShapeRenderer) bar_plot.getRenderer();
						bar_render.setSeriesVisibleInLegend(0, new Boolean(false));
						bar_render.setSeriesLinesVisible(0, false);
						bar_render.setSeriesShapesVisible(0, false);
						bar_render.setSeriesVisible(0, new Boolean(false));
						for (int i = 1; i<serieID.length; i++)
						{
							bar_render.setSeriesPaint(i, chart_elem.series[serieID[i]].color);
							bar_render.setSeriesShape(i, chart_elem.series[serieID[i]].shape);
							bar_render.setSeriesStroke(i, AWTGraphics.getStroke(chart_elem.series[serieID[i]].stroke));
							bar_render.setSeriesVisibleInLegend(i, new Boolean(false));
							bar_render.setSeriesShapesVisible(i, chart_elem.series[serieID[i]].showShapes);
							bar_render.setSeriesLinesVisible(i, chart_elem.series[serieID[i]].showLines);
							bar_render.setSeriesVisible(i, new Boolean(chart_elem.series[serieID[i]].enable));
							boolean legend = false;
							if (i > 1) {
								if (serieID[i] != serieID[i-1]) legend = true;
							} else {
								legend = true;
							}
							if (legend) bar_render.setSeriesVisibleInLegend(i, new Boolean(chart_elem.series[serieID[i]].showLegend));
						}
					} else
					{
						if (chart_elem.subType == ChartElement.SUBTYPE.CATEGORY_LINE_3D)
						{
							LineRenderer3D bar_render = (LineRenderer3D) bar_plot.getRenderer();
							bar_render.setSeriesVisibleInLegend(0, new Boolean(false));
							bar_render.setSeriesLinesVisible(0, false);
							bar_render.setSeriesShapesVisible(0, false);
							bar_render.setSeriesVisible(0, new Boolean(false));
							for (int i = 1; i < serieID.length; i++)
							{
								bar_render.setSeriesPaint(i, chart_elem.series[serieID[i]].color);
								bar_render.setSeriesShape(i, chart_elem.series[serieID[i]].shape);
								bar_render.setSeriesStroke(i, AWTGraphics.getStroke(chart_elem.series[serieID[i]].stroke));
								bar_render.setSeriesVisibleInLegend(i, new Boolean(false));
								bar_render.setSeriesShapesVisible(i, chart_elem.series[serieID[i]].showShapes);
								bar_render.setSeriesLinesVisible(i, chart_elem.series[serieID[i]].showLines);
								bar_render.setSeriesVisible(i, new Boolean(chart_elem.series[serieID[i]].enable));
								boolean legend = false;
								if (i > 1) {
									if (serieID[i] != serieID[i-1]) legend = true;
								} else {
									legend = true;
								}
								if (legend) bar_render.setSeriesVisibleInLegend(i, new Boolean(chart_elem.series[serieID[i]].showLegend));
							}
						} else
						{
							BarRenderer bar_render = (BarRenderer) bar_plot.getRenderer();
							bar_render.setSeriesVisibleInLegend(0, new Boolean(false));
							bar_render.setSeriesVisible(0, new Boolean(false));
							for (int i = 1; i < serieID.length; i++)
							{
								bar_render.setSeriesPaint(i, chart_elem.series[serieID[i]].color);
								bar_render.setSeriesShape(i, chart_elem.series[serieID[i]].shape);
								bar_render.setSeriesStroke(i, AWTGraphics.getStroke(chart_elem.series[serieID[i]].stroke));
								bar_render.setSeriesVisibleInLegend(i, new Boolean(false));
								bar_render.setSeriesVisible(i, new Boolean(chart_elem.series[serieID[i]].enable));
								boolean legend = false;
								if (i > 1) {
									if (serieID[i] != serieID[i-1]) legend = true;
								} else {
									legend = true;
								}
								if (legend) bar_render.setSeriesVisibleInLegend(i, new Boolean(chart_elem.series[serieID[i]].showLegend));
							}
						}
					}
				}
			}

			// Other chart properties not established by JPARSEC
			// CategoryAxis bar_axis = bar_plot.getDomainAxis();
			// bar_render.setDrawBarOutline(false);
			// bar_render.setItemMargin(0.5);
			// bar_axis.setLowerMargin(0.1);
			// bar_axis.setUpperMargin(0.1);
			// bar_axis.setCategoryMargin(0.2);

			LogarithmicAxis yAxis = new LogarithmicAxis(chart_elem.yLabel);
			y_axis = bar_plot.getRangeAxis();
			CategoryAxis x_caxis = bar_plot.getDomainAxis();
			if (chart_elem.yAxisInverted) y_axis.setInverted(true);
			if (increaseFontSize != 0) {
				Font f = x_caxis.getLabelFont();
				f = f.deriveFont(f.getStyle(), f.getSize()+increaseFontSize);
				x_caxis.setLabelFont(f);
				f = x_caxis.getTickLabelFont();
				f = f.deriveFont(f.getStyle(), f.getSize()+increaseFontSize);
				x_caxis.setTickLabelFont(f);
			}
			if (increaseFontSize != 0) {
				Font f = y_axis.getLabelFont();
				f = f.deriveFont(f.getStyle(), f.getSize()+increaseFontSize);
				y_axis.setLabelFont(f);
				f = y_axis.getTickLabelFont();
				f = f.deriveFont(f.getStyle(), f.getSize()+increaseFontSize);
				y_axis.setTickLabelFont(f);
			}

			y_down = y_axis.getLowerBound();
			y_up = y_axis.getUpperBound();
			if (flag_problem_logyaxis2)
				yAxis.setAllowNegativesFlag(true);
			if (chart_elem.yTickLabels == ChartElement.TICK_LABELS.EXPONENTIAL_VALUES)
				yAxis.setExpTickLabelsFlag(true);
			if (chart_elem.yTickLabels == ChartElement.TICK_LABELS.LOGARITHM_VALUES)
				yAxis.setLog10TickLabelsFlag(true);
			if (chart_elem.yTickLabels == ChartElement.TICK_LABELS.REGULAR_VALUES)
			{
				yAxis.setExpTickLabelsFlag(false);
				yAxis.setLog10TickLabelsFlag(false);
			}

			if (y_down <= 0 && chart_elem.yAxisInLogScale)
			{
				y_down = chart_elem.series[0].yMinimumValue;
				yAxis.setRange(y_down, y_up);
			}

			if (chart_elem.yAxisInLogScale)
			{
				if (increaseFontSize != 0) {
					Font f = yAxis.getLabelFont();
					f = f.deriveFont(f.getStyle(), f.getSize()+increaseFontSize);
					yAxis.setLabelFont(f);
					f = yAxis.getTickLabelFont();
					f = f.deriveFont(f.getStyle(), f.getSize()+increaseFontSize);
					yAxis.setTickLabelFont(f);
				}
				double orders =  Math.log10(y_up/y_down);
				double fac = orders / 5.0;
				yAxis.setRange(y_down - y_down*fac, y_up + y_up*fac);
				bar_plot.setRangeAxis(yAxis);
			}

			for (int i = chart_elem.series.length - 1; i >= 0; i--)
			{
				if (chart_elem.series[i].enable) {
					String x_val[] = (String[]) DataSet.getStringValuesAndLimits(chart_elem.series[i].xValues)
							.get(0);
					double y_val[] = (double[]) DataSet.getDoubleValuesIncludingLimits(chart_elem.series[i].yValues)
							.get(0);
					int x_limit[] = (int[]) DataSet.getStringValuesAndLimits(chart_elem.series[i].xValues)
							.get(1);
					int y_limit[] = (int[]) DataSet.getDoubleValuesIncludingLimits(chart_elem.series[i].yValues)
							.get(1);
					for (int ii = 0; ii < y_val.length; ii++)
					{
						double angle = 0.0;
						if (y_limit[ii] == 1)
							angle = -Math.PI * 0.5;
						if (y_limit[ii] == -1)
							angle = Math.PI * 0.5;
						if (y_limit[ii] != 0)
						{
							CategoryPointerAnnotation pointer = new CategoryPointerAnnotation("", x_val[ii], y_val[ii],
									angle);
							pointer.setBaseRadius(0.0);
							pointer.setTipRadius(-chart_elem.series[i].sizeOfArrowInLimits);
							pointer.setArrowPaint(chart_elem.series[i].color);
							pointer.setPaint(chart_elem.series[i].color);
							bar_plot.addAnnotation(pointer);
						}

						if (x_limit[ii] == 1)
							angle = 0;
						if (x_limit[ii] == -1)
							angle = Math.PI;
						if (x_limit[ii] != 0)
						{
							CategoryPointerAnnotation pointer = new CategoryPointerAnnotation("", x_val[ii], y_val[ii],
									angle);
							pointer.setBaseRadius(0.0);
							pointer.setTipRadius(-chart_elem.series[i].sizeOfArrowInLimits);
							pointer.setArrowPaint(chart_elem.series[i].color);
							pointer.setPaint(chart_elem.series[i].color);
							bar_plot.addAnnotation(pointer);
						}

						int npo = 0;
						try { npo = chart_elem.series[i].pointers.length; } catch (Exception exc) {}
						if (npo > 0 && ii == 0)
						{
							for (int iii = 0; iii < chart_elem.series[i].pointers.length; iii++)
							{
								if (chart_elem.series[i].pointers[iii] != null)
								{
									String pointerMsg = chart_elem.series[i].pointers[iii];
									String pointerX = "";
									double pointerY = 0.0;
									if (pointerMsg.startsWith("(")) {
										int pp = pointerMsg.indexOf(")");
										String ppp = pointerMsg.substring(1, pp);
										int coma = ppp.indexOf(",");
										String p1 = ppp.substring(0, coma);
										String p2 = ppp.substring(coma+1);
										String msg = pointerMsg.substring(pp+1).trim();
										pointerMsg = "";
										for (int ijk=0; ijk<chart_elem.xForCategoryCharts.length; ijk++)
										{
											if (ijk < chart_elem.series[i].yValues.length) {
												if (p1.trim().equals(chart_elem.xForCategoryCharts[ijk]) &&
														p2.trim().equals(chart_elem.series[i].yValues[ijk])) {
													pointerMsg = msg;
													pointerX = p1;
													pointerY = DataSet.getDoubleValueWithoutLimit(p2);
													break;
												}
											}
										}
										if (pointerMsg.equals("")) {
											pointerX = p1;
											pointerY = DataSet.getDoubleValueWithoutLimit(p2);
											pointerMsg = msg;
										}
									} else {
										int n = Integer.parseInt(FileIO.getField(1, pointerMsg, " ", true));
										pointerX = chart_elem.series[i].xValues[n-1];
										pointerY = DataSet.getDoubleValueWithoutLimit(chart_elem.series[i].yValues[n-1]);
										pointerMsg = FileIO.getRestAfterField(1, pointerMsg, " ", true);
									}

										int ppp = chart_elem.getIndexOfCategoryPoint(pointerX);
										double posx = chart_elem.imageWidth * (double) ppp / (double) chart_elem.xForCategoryCharts.length;
										Rectangle2D rect = new Rectangle2D.Double(0, 0, chart_elem.imageWidth, chart_elem.imageHeight); //this.getComponent().getScreenDataArea();
										double posy = y_axis
											.valueToJava2D(pointerY, rect, bar_plot
											.getRangeAxisEdge());
										angle = jparsec.ephem.Functions.normalizeRadians(Math.atan2(
												(rect.getCenterX() - posy),
												(rect.getCenterY() - posx)));

										ChartSeriesElement.POINTER_ANGLE ang = chart_elem.series[i].pointersAngle;
										if (ang == ChartSeriesElement.POINTER_ANGLE.DOWNWARDS) angle = Math.PI * 0.5;
										if (ang == ChartSeriesElement.POINTER_ANGLE.UPWARDS) angle = -Math.PI * 0.5;
										if (ang == ChartSeriesElement.POINTER_ANGLE.LEFTWARDS) angle = Math.PI;
										if (ang == ChartSeriesElement.POINTER_ANGLE.RIGHTWARDS) angle = 0.0;
										if (ang == ChartSeriesElement.POINTER_ANGLE.TO_OUTSIDE) angle = -angle;
										if (ang != ChartSeriesElement.POINTER_ANGLE.DOWNWARDS &&
												ang != ChartSeriesElement.POINTER_ANGLE.UPWARDS &&
												ang != ChartSeriesElement.POINTER_ANGLE.LEFTWARDS &&
												ang != ChartSeriesElement.POINTER_ANGLE.RIGHTWARDS &&
												ang != ChartSeriesElement.POINTER_ANGLE.TO_OUTSIDE &&
												ang != ChartSeriesElement.POINTER_ANGLE.TO_CENTER) angle = 0; //-ang;

										if (pointerMsg.startsWith("@UP")) {
											angle = -Math.PI * 0.5;
											pointerMsg = pointerMsg.substring(3);
										}
										if (pointerMsg.startsWith("@DOWN")) {
											angle = Math.PI * 0.5;
											pointerMsg = pointerMsg.substring(5);
										}
										if (pointerMsg.startsWith("@LEFT")) {
											angle = Math.PI;
											pointerMsg = pointerMsg.substring(5);
										}
										if (pointerMsg.startsWith("@RIGHT")) {
											angle = 0;
											pointerMsg = pointerMsg.substring(6);
										}
										if (pointerMsg.startsWith("@CENTER")) {
											pointerMsg = pointerMsg.substring(8);
										}

										String text = pointerMsg;
										if (angle == Math.PI) text = "@CENTER"+text;
										if (angle == 0) text = "@LEFTPLUS" + text;
										CategoryPointerAnnotation pointer = new CategoryPointerAnnotation(text, pointerX,
												pointerY, angle);
										double label_pos = 15.0;
										pointer
												.setLabelOffset(label_pos * chart_elem.series[i].pointersLabelOffsetFactor);
										pointer.setBaseRadius(20.0);
										pointer.setTipRadius(10.0);
										pointer.setArrowPaint(chart_elem.series[i].color);
										pointer.setPaint(chart_elem.series[i].color);
										pointer.setFont(bar_plot.getDomainAxis().getLabelFont());
										pointer.setTextAnchor(TextAnchor.CENTER);
										if (!chart_elem.series[i].showArrowInPointers)
										{
											pointer.setArrowLength(0);
											pointer.setTipRadius(0);
											pointer.setBaseRadius(0);
										}
										bar_plot.addAnnotation(pointer);
								}
							}
						}
					}

/*					if (chart_elem.series[i].regressionType == ChartSeriesElement.REGRESSION.LINEAR &&
							chart_elem.series[i].regressionType.getShowRegression())
					{
						double dx_val[] = chart_elem.series[i].dxValues;
						double dy_val[] = chart_elem.series[i].dyValues;

						// Fit in LOG scale if necessary
						for (int j = 0; j < y_val.length; j++)
						{
							if (chart_elem.yAxisInLogScale)
								dy_val[j] = Math.abs(dy_val[j] * Math.log10(Math.E) / y_val[j]);
							if (chart_elem.yAxisInLogScale)
								y_val[j] = Math.log10(y_val[j]);
						}
						// try {
						double fit_xval[] = new double[x_val.length];
						for (int fit = 0; fit < x_val.length; fit++)
						{
							fit_xval[fit] = fit;
						}
						LinearFit myfit = new LinearFit(fit_xval, y_val, dx_val, dy_val); // chart_elem.SERIES[i].LINEAR_FIT;
						myfit.linearFit();

						double x = 0;
						double y = Math.pow(10.0, myfit.evaluateFittingFunction(x));
						if (!chart_elem.yAxisInLogScale)
							y = myfit.evaluateFittingFunction(x);
						categoryDataset.addValue(y, "*" + chart_elem.series[i].legend, x_val[(int) x]);

						x = x_val.length - 1;
						y = Math.pow(10.0, myfit.evaluateFittingFunction(x));
						if (!chart_elem.yAxisInLogScale)
							y = myfit.evaluateFittingFunction(x);
						categoryDataset.addValue(y, "*" + chart_elem.series[i].legend, x_val[(int) x]);

						int s_number = chart_elem.series.length + i;

						jfc = getCategoryChart(chart_elem, categoryDataset, plo, show_legend);
						bar_plot = jfc.getCategoryPlot();
						CategoryItemRenderer erenderer = bar_plot.getRenderer();

						// erenderer.setSeriesShapesVisible(series_number-1,
						// false);
						// erenderer.setSeriesLinesVisible(series_number-1,
						// true);
						// erenderer.setSeriesShape(s_number-1,
						// chart_elem.SERIES[i].SHAPE);
						// erenderer.setSeriesVisibleInLegend(s_number-1,
						// false);
						// erenderer.setSeriesPaint(s_number-1,
						// chart_elem.SERIES[i].COLOR);
						// erenderer.setDrawXError(false);
						// erenderer.setDrawYError(false);
						// erenderer.setSeriesStroke(s_number-1,
						// chart_elem.SERIES[i].STROKE);

						if (chart_elem.subType == ChartElement.SUBTYPE.CATEGORY_AREA)
						{
							AreaRenderer bar_render = (AreaRenderer) bar_plot.getRenderer();
							bar_render.setSeriesPaint(s_number - 1, chart_elem.series[i].color);
							bar_render.setSeriesShape(s_number - 1, chart_elem.series[i].shape);
							bar_render.setSeriesVisibleInLegend(s_number - 1, false);
							bar_render.setSeriesVisible(s_number - 1, true);
						} else
						{
							if (chart_elem.subType == ChartElement.SUBTYPE.CATEGORY_STACKED_AREA)
							{
								StackedAreaRenderer bar_render = (StackedAreaRenderer) bar_plot.getRenderer();
								bar_render.setSeriesPaint(s_number - 1, chart_elem.series[i].color);
								bar_render.setSeriesShape(s_number - 1, chart_elem.series[i].shape);
								bar_render.setSeriesVisibleInLegend(s_number - 1, false);
								bar_render.setSeriesVisible(s_number - 1, true);
							} else
							{
								if (chart_elem.subType == ChartElement.SUBTYPE.CATEGORY_LINE)
								{
									LineAndShapeRenderer bar_render = (LineAndShapeRenderer) bar_plot.getRenderer();
									bar_render.setSeriesPaint(s_number - 1, chart_elem.series[i].color);
									bar_render.setSeriesShape(s_number - 1, chart_elem.series[i].shape);
									bar_render.setSeriesLinesVisible(s_number - 1, true);
									bar_render.setSeriesStroke(s_number - 1, chart_elem.series[i].stroke.stroke);
									bar_render.setSeriesShapesVisible(s_number - 1, chart_elem.series[i].showShapes);
									bar_render.setSeriesVisibleInLegend(s_number - 1, false);
									bar_render.setSeriesVisible(s_number - 1, true);
								} else
								{
									if (chart_elem.subType == ChartElement.SUBTYPE.CATEGORY_LINE_3D)
									{
										LineRenderer3D bar_render = (LineRenderer3D) bar_plot.getRenderer();
										bar_render.setSeriesPaint(s_number - 1, chart_elem.series[i].color);
										bar_render.setSeriesShape(s_number - 1, chart_elem.series[i].shape);
										bar_render.setSeriesLinesVisible(s_number - 1, true);
										bar_render.setSeriesStroke(s_number - 1, chart_elem.series[i].stroke.stroke);
										bar_render.setSeriesShapesVisible(s_number - 1,
												chart_elem.series[i].showShapes);
										bar_render.setSeriesVisibleInLegend(s_number - 1, false);
										bar_render.setSeriesVisible(s_number - 1, true);
									} else
									{
										BarRenderer bar_render = (BarRenderer) bar_plot.getRenderer();
										bar_render.setSeriesPaint(s_number - 1, chart_elem.series[i].color);
										bar_render.setSeriesShape(s_number - 1, chart_elem.series[i].shape);
										bar_render.setSeriesVisibleInLegend(s_number - 1, false);
										bar_render.setSeriesVisible(s_number - 1, true);
									}
								}
							}
						}
					}
*/

				}
			}

			// bar_plot.setRenderer(erenderer);

			if (chart_elem.showBackgroundImage)
			{
				chart_elem.backgroundGradient = new Color(255, 255, 255, 0);
				jfc.getLegend().setBackgroundPaint(chart_elem.backgroundGradient);
				if (chart_elem.showBackgroundImageOnlyInDataArea)
				{
					bar_plot.setBackgroundImage(chart_elem.backgroundImage);
					bar_plot.setBackgroundImageAlpha(1f);
				} else
				{
					jfc.setBackgroundImage(chart_elem.backgroundImage);
					jfc.setBackgroundImageAlpha(1f);
				}
			}

			jfc.setBackgroundPaint(chart_elem.backgroundGradient);
			bar_plot.setBackgroundPaint(chart_elem.backgroundGradient);
			return jfc;
		default:
			throw new JPARSECException("invalid chart type.");
		}
		} catch (JPARSECException exc)
		{
			throw exc;
		}
	}

	private static JFreeChart getCategoryChart(ChartElement chart_elem, DefaultCategoryDataset categoryDataset,
			PlotOrientation plo, boolean show_legend)
	{
		// MISSING CHARTS:
		// createGanttChart, createMultiplePieChart, createMultiplePieChart3D
		JFreeChart jfc = null;
		switch (chart_elem.subType)
		{
		case CATEGORY_STACKED_BAR_3D:
			jfc = ChartFactory.createStackedBarChart3D(chart_elem.title, chart_elem.xLabel, chart_elem.yLabel,
					categoryDataset, plo, show_legend,
					true, true);
			break;
		case CATEGORY_BAR_3D:
			jfc = ChartFactory.createBarChart3D(chart_elem.title, chart_elem.xLabel, chart_elem.yLabel,
					categoryDataset, plo, show_legend,
					true, true);
			break;
		case CATEGORY_STACKED_BAR:
			jfc = ChartFactory.createStackedBarChart(chart_elem.title, chart_elem.xLabel, chart_elem.yLabel,
					categoryDataset, plo, show_legend,
					true, true);
			break;
		case CATEGORY_BAR:
			jfc = ChartFactory.createBarChart(chart_elem.title, chart_elem.xLabel, chart_elem.yLabel,
					categoryDataset, plo, show_legend,
					true, true);
			break;
		case CATEGORY_WATER_FALL:
			jfc = ChartFactory.createWaterfallChart(chart_elem.title, chart_elem.xLabel, chart_elem.yLabel,
					categoryDataset, plo, show_legend,
					true, true);
			break;
		case CATEGORY_LINE_3D:
			jfc = ChartFactory.createLineChart3D(chart_elem.title, chart_elem.xLabel, chart_elem.yLabel,
					categoryDataset, plo, show_legend,
					true, true);
			break;
		case CATEGORY_LINE:
			jfc = ChartFactory.createLineChart(chart_elem.title, chart_elem.xLabel, chart_elem.yLabel,
					categoryDataset, plo, show_legend,
					true, true);
			break;
		case CATEGORY_STACKED_AREA:
			jfc = ChartFactory.createStackedAreaChart(chart_elem.title, chart_elem.xLabel, chart_elem.yLabel,
					categoryDataset, plo, show_legend,
					true, true);
			break;
		case CATEGORY_AREA:
			jfc = ChartFactory.createAreaChart(chart_elem.title, chart_elem.xLabel, chart_elem.yLabel,
					categoryDataset, plo, show_legend,
					true, true);
			break;
		default:
			break;
		}

		return jfc;
	}


	private static double[] getPoint(ChartElement chart_elem, LinearFit myfit, int i, double x, double ymax, double ymin)
	{
		double y = 0.0;
		if (chart_elem.yAxisInLogScale && chart_elem.xAxisInLogScale)
		{
			y = Math.pow(10.0, myfit.evaluateFittingFunction(Math.log10(x)));
			if (y > ymax)
			{
				y = ymax;
				x = Math.pow(10.0, myfit.evaluateAbcissa(Math.log10(y)));
			}
			if (y < ymin)
			{
				y = ymin;
				x = Math.pow(10.0, myfit.evaluateAbcissa(Math.log10(y)));
			}
			if (y < chart_elem.series[i].yMinimumValue)
			{
				y = chart_elem.series[i].yMinimumValue;
				x = Math.pow(10.0, myfit.evaluateAbcissa(Math.log10(y)));
			}
		}
		if (chart_elem.yAxisInLogScale && !chart_elem.xAxisInLogScale)
		{
			y = Math.pow(10.0, myfit.evaluateFittingFunction(x));
			if (y > ymax)
			{
				y = ymax;
				x = myfit.evaluateAbcissa(Math.log10(y));
			}
			if (y < ymin)
			{
				y = ymin;
				x = myfit.evaluateAbcissa(Math.log10(y));
			}
			if (y < chart_elem.series[i].yMinimumValue)
			{
				y = chart_elem.series[i].yMinimumValue;
				x = myfit.evaluateAbcissa(Math.log10(y));
			}
		}
		if (chart_elem.xAxisInLogScale && !chart_elem.yAxisInLogScale)
		{
			y = myfit.evaluateFittingFunction(Math.log10(x));
			if (y > ymax)
			{
				y = ymax;
				x = Math.pow(10.0, myfit.evaluateAbcissa(y));
			}
			if (y < ymin)
			{
				y = ymin;
				x = Math.pow(10.0, myfit.evaluateAbcissa(y));
			}
			if (y < chart_elem.series[i].yMinimumValue)
			{
				y = chart_elem.series[i].yMinimumValue;
				x = Math.pow(10.0, myfit.evaluateAbcissa(y));
			}
		}
		if (!chart_elem.xAxisInLogScale && !chart_elem.yAxisInLogScale)
		{
			y = myfit.evaluateFittingFunction(x);
			if (y > ymax)
			{
				y = ymax;
				x = myfit.evaluateAbcissa(y);
			}
			if (y < ymin)
			{
				y = ymin;
				x = myfit.evaluateAbcissa(y);
			}
			if (y < chart_elem.series[i].yMinimumValue)
			{
				y = chart_elem.series[i].yMinimumValue;
				x = myfit.evaluateAbcissa(y);
			}
		}

		return new double[] { x, y };
	}

	private static double[] getPoint(ChartElement chart_elem, Polynomial pol, int i, double x, double ymax, double ymin) throws JPARSECException
	{
		double y = 0.0;
/*		if (chart_elem.yAxisInLogScale && chart_elem.xAxisInLogScale)
		{
			y = Math.pow(10.0, pol.evaluate(Math.log10(x)).real);
			if (y > ymax)
			{
				y = ymax;
				x = Math.pow(10.0, solve(pol, Math.log10(y)));
			}
			if (y < ymin)
			{
				y = ymin;
				x = Math.pow(10.0, solve(pol, Math.log10(y)));
			}
			if (y < chart_elem.series[i].yMinimumValue)
			{
				y = chart_elem.series[i].yMinimumValue;
				x = Math.pow(10.0, solve(pol, Math.log10(y)));
			}
		}
*/
		if (chart_elem.yAxisInLogScale && !chart_elem.xAxisInLogScale)
		{
			y = Math.pow(10.0, pol.evaluate(x).real);
/*			if (y > ymax)
			{
				y = ymax;
				x = solve(pol, Math.log10(y));
			}
			if (y < ymin)
			{
				y = ymin;
				x = solve(pol, Math.log10(y));
			}
			if (y < chart_elem.series[i].yMinimumValue)
			{
				y = chart_elem.series[i].yMinimumValue;
				x = solve(pol, Math.log10(y));
			}
*/		}
		if (chart_elem.xAxisInLogScale && !chart_elem.yAxisInLogScale)
		{
			y = pol.evaluate(Math.log10(x)).real;
/*			if (y > ymax)
			{
				y = ymax;
				x = Math.pow(10.0, solve(pol, y));
			}
			if (y < ymin)
			{
				y = ymin;
				x = Math.pow(10.0, solve(pol, y));
			}
			if (y < chart_elem.series[i].yMinimumValue)
			{
				y = chart_elem.series[i].yMinimumValue;
				x = Math.pow(10.0, solve(pol, y));
			}
*/		}
		if (!chart_elem.xAxisInLogScale && !chart_elem.yAxisInLogScale)
		{
			y = pol.evaluate(x).real;
/*			if (y > ymax)
			{
				y = ymax;
				x = solve(pol, y);
			}
			if (y < ymin)
			{
				y = ymin;
				x = solve(pol, y);
			}
			if (y < chart_elem.series[i].yMinimumValue)
			{
				y = chart_elem.series[i].yMinimumValue;
				x = solve(pol, y);
			}
*/		}

		return new double[] { x, y };
	}

	private static double[] getPoint(ChartElement chart_elem, String function, double estimates[], int i, double x, double ymax, double ymin) throws JPARSECException
	{
		double y = 0.0;
		String param[] = new String[estimates.length];
		for (int ii=0; ii<param.length; ii++) {
			param[ii] = "p"+(ii+1)+" "+estimates[ii];
		}
		Evaluation eval = new Evaluation(function, DataSet.addStringArray(param, new String[] {"x "+x}));
		Evaluation evalLog = new Evaluation(function, DataSet.addStringArray(param, new String[] {"x "+Math.log10(x)}));
/*		if (chart_elem.yAxisInLogScale && chart_elem.xAxisInLogScale)
		{
			y = Math.pow(10.0, evalLog.evaluate().real);
			if (y > ymax)
			{
				y = ymax;
				x = Math.pow(10.0, solve(pol, Math.log10(y)));
			}
			if (y < ymin)
			{
				y = ymin;
				x = Math.pow(10.0, solve(pol, Math.log10(y)));
			}
			if (y < chart_elem.series[i].yMinimumValue)
			{
				y = chart_elem.series[i].yMinimumValue;
				x = Math.pow(10.0, solve(pol, Math.log10(y)));
			}
		}
*/
		if (chart_elem.yAxisInLogScale && !chart_elem.xAxisInLogScale)
		{
			y = Math.pow(10.0, eval.evaluate());
/*			if (y > ymax)
			{
				y = ymax;
				x = solve(pol, Math.log10(y));
			}
			if (y < ymin)
			{
				y = ymin;
				x = solve(pol, Math.log10(y));
			}
			if (y < chart_elem.series[i].yMinimumValue)
			{
				y = chart_elem.series[i].yMinimumValue;
				x = solve(pol, Math.log10(y));
			}
*/		}
		if (chart_elem.xAxisInLogScale && !chart_elem.yAxisInLogScale)
		{
			y = evalLog.evaluate();
/*			if (y > ymax)
			{
				y = ymax;
				x = Math.pow(10.0, solve(pol, y));
			}
			if (y < ymin)
			{
				y = ymin;
				x = Math.pow(10.0, solve(pol, y));
			}
			if (y < chart_elem.series[i].yMinimumValue)
			{
				y = chart_elem.series[i].yMinimumValue;
				x = Math.pow(10.0, solve(pol, y));
			}
*/		}
		if (!chart_elem.xAxisInLogScale && !chart_elem.yAxisInLogScale)
		{
			y = eval.evaluate();
/*			if (y > ymax)
			{
				y = ymax;
				x = solve(pol, y);
			}
			if (y < ymin)
			{
				y = ymin;
				x = solve(pol, y);
			}
			if (y < chart_elem.series[i].yMinimumValue)
			{
				y = chart_elem.series[i].yMinimumValue;
				x = solve(pol, y);
			}
*/		}

		return new double[] { x, y };
	}

/*	private static double solve(Polynomial pol, double y) throws JPARSECException {
		Polynomial p = pol.clone();
		Complex c[] = p.getCoefficients();
		c[0].real -= y;
		p.setCoefficients(c);
		Complex z[] = p.zeros();
		return z[0].real; // Which to retrieve!?
	}
*/

	private static double[] getPoint(ChartElement chart_elem, GenericFit gf, int i, double x, double ymax, double ymin) throws JPARSECException
	{
		double y = 0.0;
/*		if (chart_elem.yAxisInLogScale && chart_elem.xAxisInLogScale)
		{
			y = Math.pow(10.0, gf.evaluateFittingFunction(Math.log10(x)));
			if (y > ymax)
			{
				y = ymax;
				x = Math.pow(10.0, solve(pol, Math.log10(y)));
			}
			if (y < ymin)
			{
				y = ymin;
				x = Math.pow(10.0, solve(pol, Math.log10(y)));
			}
			if (y < chart_elem.series[i].yMinimumValue)
			{
				y = chart_elem.series[i].yMinimumValue;
				x = Math.pow(10.0, solve(pol, Math.log10(y)));
			}
		}
*/
		if (chart_elem.yAxisInLogScale && !chart_elem.xAxisInLogScale)
		{
			y = Math.pow(10.0, gf.evaluateFittingFunction(x));
/*			if (y > ymax)
			{
				y = ymax;
				x = solve(pol, Math.log10(y));
			}
			if (y < ymin)
			{
				y = ymin;
				x = solve(pol, Math.log10(y));
			}
			if (y < chart_elem.series[i].yMinimumValue)
			{
				y = chart_elem.series[i].yMinimumValue;
				x = solve(pol, Math.log10(y));
			}
*/		}
		if (chart_elem.xAxisInLogScale && !chart_elem.yAxisInLogScale)
		{
			y = gf.evaluateFittingFunction(Math.log10(x));
/*			if (y > ymax)
			{
				y = ymax;
				x = Math.pow(10.0, solve(pol, y));
			}
			if (y < ymin)
			{
				y = ymin;
				x = Math.pow(10.0, solve(pol, y));
			}
			if (y < chart_elem.series[i].yMinimumValue)
			{
				y = chart_elem.series[i].yMinimumValue;
				x = Math.pow(10.0, solve(pol, y));
			}
*/		}
		if (!chart_elem.xAxisInLogScale && !chart_elem.yAxisInLogScale)
		{
			y = gf.evaluateFittingFunction(x);
/*			if (y > ymax)
			{
				y = ymax;
				x = solve(pol, y);
			}
			if (y < ymin)
			{
				y = ymin;
				x = solve(pol, y);
			}
			if (y < chart_elem.series[i].yMinimumValue)
			{
				y = chart_elem.series[i].yMinimumValue;
				x = solve(pol, y);
			}
*/		}

		return new double[] { x, y };
	}

	/**
	 * Creates a simple chart from a set of values.
	 *
	 * @param x X values.
	 * @param y Y values.
	 * @param legend Legend text. Empty string implies no legend
	 * @param title Chart title.
	 * @param xlabel X axis label.
	 * @param ylabel Y axis label.
	 * @param logscale True to create a log scale chart.
	 * @return The chart.
	 * @throws JPARSECException If an error occurs.
	 */
	public static CreateChart createSimpleChart(double x[], double y[], String title, String xlabel, String ylabel,
			String legend, boolean logscale) throws JPARSECException
	{
		SimpleChartElement elem = new SimpleChartElement(ChartElement.TYPE.XY_CHART,
				ChartElement.SUBTYPE.XY_SCATTER, x, y, title, xlabel, ylabel, legend, true, false, 800, 600);
		elem.xAxisInLogScale = logscale;
		elem.yAxisInLogScale = logscale;
		if (legend.equals(""))
			elem.showLegend = false;

		return new CreateChart(elem);
	}

	/**
	 * Deletes a series from the chart.
	 * @param index Index of the series.
	 * @throws JPARSECException If an error occurs.
	 */
	public void deleteSeries(int index)
	throws JPARSECException {
		this.chart_elem.deleteSeries(index);
		CreateChart newC = new CreateChart(this.chart_elem);
		this.setChart(newC.getChart());
		this.chart_elem = newC.chart_elem;
	}
	/**
	 * Deletes a series from the chart.
	 * @param legend Legend of the series to delete.
	 * @throws JPARSECException If an error occurs.
	 */
	public void deleteSeries(String legend)
	throws JPARSECException {
		this.chart_elem.deleteSeries(legend);
		CreateChart newC = new CreateChart(this.chart_elem);
		this.setChart(newC.getChart());
		this.chart_elem = newC.chart_elem;
	}
	/**
	 * Deletes all series from the chart except one.
	 * @param legend Legend of the series to maintain.
	 * @throws JPARSECException If an error occurs.
	 */
	public void deleteAllSeriesExcept(String legend)
	throws JPARSECException {
		this.chart_elem.deleteAllSeriesExcept(legend);
		CreateChart newC = new CreateChart(this.chart_elem);
		this.setChart(newC.getChart());
		this.chart_elem = newC.chart_elem;
	}
	/**
	 * Adds a series from the chart.
	 * @param series New series.
	 * @throws JPARSECException If an error occurs.
	 */
	public void addSeries(ChartSeriesElement series)
	throws JPARSECException {
		this.chart_elem.addSeries(series);
		CreateChart newC = new CreateChart(this.chart_elem);
		this.setChart(newC.getChart());
		this.chart_elem = newC.chart_elem;
	}
	/**
	 * Adds a series from the chart.
	 * @param series New series.
	 * @param index Index for the position of this series.
	 * @throws JPARSECException If an error occurs.
	 */
	public void addSeries(ChartSeriesElement series, int index)
	throws JPARSECException {
		this.chart_elem.addSeries(series, index);
		CreateChart newC = new CreateChart(this.chart_elem);
		this.setChart(newC.getChart());
		this.chart_elem = newC.chart_elem;
	}
	/**
	 * Returns a chart series with the given legend.
	 * @param legend Legend of the series to retrieve.
	 * @return The series.
	 * @throws JPARSECException If the series cannot be found.
	 */
	public ChartSeriesElement getSeries(String legend)
	throws JPARSECException {
		ChartSeriesElement ch = null;
		for (int i=0; i<this.chart_elem.series.length;i++)
		{
			if (this.chart_elem.series[i].legend.equals(legend)) {
				ch = this.chart_elem.series[i];
				break;
			}
		}
		if (ch == null) throw new JPARSECException("series "+legend+" cannot be found.");
		return ch;
	}
	/**
	 * Returns a chart series-
	 * @param index Index for the series.
	 * @return The series.
	 */
	public ChartSeriesElement getSeries(int index)
	{
		ChartSeriesElement ch = this.chart_elem.series[index];
		return ch;
	}

	/**
	 * Sets the chart type to a new type.
	 * @param type New type.
	 */
	public void setChartType(ChartElement.TYPE type)
	{
		this.chart_elem.chartType = type;
	}
	/**
	 * Sets the chart subtype to a new subtype.
	 * @param subtype New subtype.
	 */
	public void setChartSubType(ChartElement.SUBTYPE subtype)
	{
		this.chart_elem.subType = subtype;
	}
	/**
	 * Returns the chart object of this instance.
	 * @return Chart.
	 */
	public ChartElement getChartElement()
	{
		return this.chart_elem;
	}

	/**
	 * Creates a script to draw the current chart using GILDAS. Only the most
	 * simple charts (x-y scattering and category) are fully supported, with limitations on
	 * colors and line strokes, among others.
	 * @param fileName Name of the postscript file to be created, with path.
	 * @param leyendPosition Position of the leyend, constants defined in
	 * class {@linkplain GILDAS_LEYEND}.
	 * @param autoDestroy True to add commands to autodestroy the script.
	 * @throws JPARSECException If an error occurs.
	 * @return The contents of the script.
	 */
	public String exportAsScriptForGILDAS(String fileName, GILDAS_LEYEND leyendPosition, boolean autoDestroy)
	throws JPARSECException {

		int pp = fileName.lastIndexOf(FileIO.getFileSeparator());
		String path = "";
		if (pp > 0) {
			path = fileName.substring(0, pp+1);
			fileName = fileName.substring(pp+1);
		}
		int ext = fileName.toLowerCase().lastIndexOf(".ps");
		if (ext < 0) ext = fileName.toLowerCase().lastIndexOf(".greg");
		if (ext > 0) fileName = fileName.substring(0, ext);

		// Define absolute limits of box in GILDAS
		double xmin = 4.0; //this.chart_elem.getxMin();
		double xmax = 28.0; //this.chart_elem.getxMax();
		double ymin = 2.5; //this.chart_elem.getyMin();
		double ymax = 19.5; //this.chart_elem.getyMax();

		String sep = FileIO.getLineSeparator();
		StringBuffer script = this.exportChartForGILDAS(true, 1.0, -1, leyendPosition, path, fileName);

		if (this.chart_elem.subCharts != null) {
			if (this.chart_elem.subCharts.length > 0) {
				for (int i=0; i<this.chart_elem.subCharts.length; i++)
				{
					CreateChart newChart = new CreateChart(this.chart_elem.subCharts[i]);
					script.append("! CODE TO SHOW A SUBCHART INSIDE THE MAIN CHART"+sep);
					Point p = this.chart_elem.subChartPosition[i];
					double subWidth = this.chart_elem.subCharts[i].imageWidth;
					double subHeight = this.chart_elem.subCharts[i].imageHeight;
					double width = this.chart_elem.imageWidth;
					double height = this.chart_elem.imageHeight;
					double scaleFactorX = 0.7; //0.7;
					double scaleFactorY = 1.0; //0.87;
					double physX0 = (xmax-xmin) * p.getX() / width * scaleFactorX; // + xmin;
					double physY0 = ymax - (ymax-ymin) * p.getY() / height * scaleFactorY - ymin;
					double physXf = (xmax-xmin) * (p.getX()+subWidth) / width * scaleFactorX; // + xmin;
					double physYf = ymax - (ymax-ymin) * (p.getY()+subHeight) / height * scaleFactorY - ymin;

					String pxi = "box_xmin+"+(physX0/24.0)+"*(box_xmax-box_xmin)";
					String pxf = "box_xmin+"+(physXf/24.0)+"*(box_xmax-box_xmin)";
					String pyi = "box_ymin+"+(physY0/17.0)+"*(box_ymax-box_ymin)";
					String pyf = "box_ymin+"+(physYf/17.0)+"*(box_ymax-box_ymin)";

					script.append("set expand 0.7*(box_xmax-box_xmin)/24"+sep);
					script.append("set box "+pxi+" "+pxf+" "+pyf+" "+pyi+sep);
					script.append(newChart.exportChartForGILDAS(false, 0.7, i, leyendPosition, path, fileName));
				}
			}
		}

		script.append("! EXPORT AS PS"+ sep);
		script.append("sys \"rm "+fileName+".ps\""+ sep);
		script.append("hard "+fileName+".ps /dev ps color"+ sep);
		if (autoDestroy)
		{
			script.append(""+ sep);
			script.append(""+ sep);
			script.append("! REMOVE ALL UNNECESARY FILES"+ sep);
			script.append("sys \"rm "+fileName+".greg\""+ sep);
			ChartSeriesElement series[] = this.chart_elem.series;
			for (int i=0; i<series.length; i++)
			{
				script.append("sys \"rm serie"+i+fileName+".jparsec\""+ sep);
			}
			if (this.chart_elem.subCharts != null) {
				if (this.chart_elem.subCharts.length > 0) {
					for (int i=0; i<this.chart_elem.subCharts.length; i++)
					{
						for (int ii=0; ii<this.chart_elem.subCharts[i].series.length; ii++)
						{
							script.append("sys \"rm subchart"+i+"serie"+ii+fileName+".jparsec\""+ sep);
						}
					}
				}
			}
		}

		WriteFile.writeAnyExternalFile(path+fileName+".greg", script.toString());

		return script.toString();
	}

	/**
	 * The set of positions for the leyend in Gildas charts.
	 */
	public enum GILDAS_LEYEND {
		/** Symbolic constant to set the position of the leyend in a GILDAS figure. */
		BOTTOM,
		/** Symbolic constant to set the position of the leyend in a GILDAS figure. */
		TOP_LEFT_CORNER,
		/** Symbolic constant to set the position of the leyend in a GILDAS figure. */
		TOP_RIGHT_CORNER,
		/** Symbolic constant to set the position of the leyend in a GILDAS figure. */
		BOTTOM_LEFT_CORNER,
		/** Symbolic constant to set the position of the leyend in a GILDAS figure. */
		BOTTOM_RIGHT_CORNER,
		/** Symbolic constant to set the position of the leyend in a GILDAS figure. */
		NO_LEYEND
	}

	/**
	 * Creates a script to draw the current chart using GILDAS. Only the most
	 * simple charts (x-y scattering and category) are fully supported, with limitations on
	 * colors and line strokes, among others.
	 * @param mainChart True if it is the main chart.
	 * @param lastExpand Last expand value in GILDAS.
	 * @param subChartID ID for the sub-chart if it is not the main chart.
	 * @param leyendPosition Position of the leyend.
	 * @param path Path of the files.
	 * @param fileName Name of .ps file to generate.
	 * @throws JPARSECException If an error occurs.
	 * @return String with the GILDAS script.
	 */
	private StringBuffer exportChartForGILDAS(boolean mainChart, double lastExpand, int subChartID,
			GILDAS_LEYEND leyendPosition, String path, String fileName)
	throws JPARSECException {
		boolean warningColor = false, warningMark = false, warningPointer = false;

		ChartSeriesElement series[] = this.chart_elem.series;
		StringBuffer script = new StringBuffer(1000);
		String sep = FileIO.getLineSeparator();

		JFreeChart jf = this.getChart();
		double xmin, xmax, ymin, ymax;
		try {
			XYPlot plot = (XYPlot) jf.getPlot();
			ValueAxis y_axis = plot.getRangeAxis();
			ValueAxis x_axis = plot.getDomainAxis();
			xmin = x_axis.getLowerBound();
			xmax = x_axis.getUpperBound();
			if (this.chart_elem.subType == ChartElement.SUBTYPE.XY_TIME) {
				for (int iii=0; iii<series.length;iii++)
				{
					double min = DataSet.getMinimumValue(DataSet.getDoubleValuesExcludingLimits(series[iii].xValues));
					double max = DataSet.getMaximumValue(DataSet.getDoubleValuesExcludingLimits(series[iii].xValues));
					if (min < xmin || iii==0) xmin = min;
					if (max > xmax || iii==0) xmax = max;
				}
			}
			ymin = y_axis.getLowerBound();
			ymax = y_axis.getUpperBound();

			if (chart_elem.xAxisInverted) {
				double tmp = xmin;
				xmin = xmax;
				xmax = tmp;
			}
		} catch (Exception e1)
		{
			try {
				CategoryPlot plot = jf.getCategoryPlot();
				ValueAxis y_axis = plot.getRangeAxis();
				xmin = -1;
				xmax = chart_elem.xForCategoryCharts.length;
				ymin = y_axis.getLowerBound();
				ymax = y_axis.getUpperBound();
			} catch (Exception e2)
			{
				throw new JPARSECException("unsupported chart type.", e2);
			}
		}
		if (chart_elem.yAxisInverted) {
			double tmp = ymin;
			ymin = ymax;
			ymax = tmp;
		}

		double offsetx1 = xmax - this.chart_elem.getxMax();
		double offsetx2 = -xmin + this.chart_elem.getxMin();
		double offsetx = offsetx1;
		if (offsetx2 > offsetx1) offsetx = offsetx2;

		double offsety1 = ymax - this.chart_elem.getyMax();
		double offsety2 = -ymin + this.chart_elem.getyMin();
		double offsety = offsety1;
		if (offsety2 > offsety1) offsety = offsety2;

		if (this.chart_elem.yAxisInLogScale)
		{
			if (ymin < series[0].yMinimumValue && this.chart_elem.chartType != ChartElement.TYPE.CATEGORY_CHART)
				ymin = series[0].yMinimumValue;
			if (ymin < 0.0 && this.chart_elem.chartType == ChartElement.TYPE.CATEGORY_CHART)
				ymin = series[0].yMinimumValue;
		}

		String limit = "limits "+xmin+" "+xmax+" "+ymin+" "+ymax;
		if (this.chart_elem.xAxisInLogScale) limit += " /XLOG";
		if (this.chart_elem.yAxisInLogScale) limit += " /YLOG";
		script.append("! SCRIPT TO DRAW A CHART WITH GILDAS"+ sep);
		script.append("! AUTOMATICALLY GENERATED BY JPARSEC PACKAGE"+ sep);
		script.append("! ON " + (new Date().toString())+ sep);
		script.append(""+ sep);
		script.append("! CHART TITLE: "+this.chart_elem.title.trim()+ sep);
		script.append(""+ sep);
		script.append("! Draw box"+ sep);
		if (mainChart) script.append("set expand "+lastExpand+"*(box_xmax-box_xmin)/24"+sep);
		script.append(limit + sep);
		if (this.chart_elem.chartType == ChartElement.TYPE.CATEGORY_CHART) {
			script.append("box N O ! LABELS FOR X AXIS WILL BE SET AT THE END"+sep);
		} else {
			script.append("box"+sep);
		}
		script.append(""+ sep);

		for (int i=0; i<series.length; i++)
		{
			if (series[i].enable) {
				script.append("! BEGIN OF COMMANDS FOR DRAWING SERIES NUMBER "+i+" - "+series[i].legend + sep);
				script.append(""+ sep);
				String color = "";

				if (series[i].stroke != null)
				{
					if (series[i].stroke.equals(JPARSECStroke.STROKE_DEFAULT_LINE)) color += " /dashed 1";
					if (series[i].stroke.equals(JPARSECStroke.STROKE_LINES_LARGE)) color += " /dashed 7";
					if (series[i].stroke.equals(JPARSECStroke.STROKE_LINES_MEDIUM)) color += " /dashed 6";
					if (series[i].stroke.equals(JPARSECStroke.STROKE_LINES_SHORT)) color += " /dashed 2";
					if (series[i].stroke.equals(JPARSECStroke.STROKE_POINTS_HIGH_SPACE)) color += " /dashed 3";
					if (series[i].stroke.equals(JPARSECStroke.STROKE_POINTS_LOW_SPACE)) color += " /dashed 5";
					if (series[i].stroke.equals(JPARSECStroke.STROKE_POINTS_MEDIUM_SPACE)) color += " /dashed 4";
				}
				if (color.indexOf("/dashed") < 0) color += " /dashed 1";

				if (series[i].color.equals(Color.BLACK) ||
						series[i].color.equals(Color.black)) color += " /col 0 ! BLACK";
				if (series[i].color.equals(Color.GRAY) ||
						series[i].color.equals(Color.gray)) color += " /col 0 ! BLACK (FROM GRAY)";
				if (series[i].color.equals(Color.RED) ||
						series[i].color.equals(Color.red)) color += " /col 1 ! RED";
				if (series[i].color.equals(Color.GREEN) ||
						series[i].color.equals(Color.green)) color += " /col 2 ! GREEN";
				if (series[i].color.equals(Color.BLUE) ||
						series[i].color.equals(Color.blue)) color += " /col 3 ! BLUE";
				if (series[i].color.equals(Color.CYAN) ||
						series[i].color.equals(Color.cyan)) color += " /col 4 ! CYAN";
				if (series[i].color.equals(Color.YELLOW) ||
						series[i].color.equals(Color.yellow)) color += " /col 5 ! YELLOW";
				if (series[i].color.equals(Color.ORANGE) ||
						series[i].color.equals(Color.orange)) color += " /col 5 ! YELLOW (FROM ORANGE)";
				if (series[i].color.equals(Color.MAGENTA) ||
						series[i].color.equals(Color.magenta)) color += " /col 6 ! MAGENTA";
				if (series[i].color.equals(Color.PINK) ||
						series[i].color.equals(Color.pink)) color += " /col 6 ! MAGENTA (FROM PINK)";
				if (series[i].color.equals(Color.WHITE) ||
						series[i].color.equals(Color.white)) color += " /col 7 ! WHITE";

				if (color.indexOf("/col") == -1) {
					color += " /col 0 ! BLACK (FROM A COLOR UNSUPPORTED BY GILDAS)";
				}
				if (color.indexOf("(FROM") >= 0) {
					warningColor = true;
				}

				// Use relativelly thin line for black, and thicker for other colors that
				// will be passed to gray scale in the printer. This effect has been
				// finally removed.
				double pointSize0 = 0.2 * (double) series[i].shapeSize / 3.0;
				if (pointSize0 >= 0.2) {
					if (color.indexOf("/col 0") >= 0) {
						color = "pen /wei 1" + color;
					} else {
						color = "pen /wei 2" + color;
					}
				} else {
					color = "pen /wei 1" + color;
				}

				script.append("! Color selection" + sep);
				script.append(color + sep);

				ChartSeriesElement.setShapeSize(series[i].shapeSize);
				String pointSize = ""+(float)pointSize0;
				if (!series[i].showShapes) pointSize = "0.0";

				boolean equalDiamond = ChartSeriesElement.equalShapes(series[i].shape, ChartSeriesElement.SHAPE_DIAMOND);
				boolean equalEllipse = ChartSeriesElement.equalShapes(series[i].shape, ChartSeriesElement.SHAPE_ELLIPSE);
				boolean equalPoint = ChartSeriesElement.equalShapes(series[i].shape, ChartSeriesElement.SHAPE_POINT);
				boolean equalEmpty = ChartSeriesElement.equalShapes(series[i].shape, ChartSeriesElement.SHAPE_EMPTY);
				boolean equalSquare = ChartSeriesElement.equalShapes(series[i].shape, ChartSeriesElement.SHAPE_SQUARE);
				boolean equalRectangleX = ChartSeriesElement.equalShapes(series[i].shape, ChartSeriesElement.SHAPE_HORIZONTAL_RECTANGLE);
				boolean equalRectangleY = ChartSeriesElement.equalShapes(series[i].shape, ChartSeriesElement.SHAPE_VERTICAL_RECTANGLE);
				boolean equalTriangleD = ChartSeriesElement.equalShapes(series[i].shape, ChartSeriesElement.SHAPE_TRIANGLE_DOWN);
				boolean equalTriangleR = ChartSeriesElement.equalShapes(series[i].shape, ChartSeriesElement.SHAPE_TRIANGLE_RIGHT);
				boolean equalTriangleL = ChartSeriesElement.equalShapes(series[i].shape, ChartSeriesElement.SHAPE_TRIANGLE_LEFT);
				boolean equalTriangleU = ChartSeriesElement.equalShapes(series[i].shape, ChartSeriesElement.SHAPE_TRIANGLE_UP);
				boolean equalCrux = ChartSeriesElement.equalShapes(series[i].shape, ChartSeriesElement.SHAPE_CRUX);
				boolean equalStar = ChartSeriesElement.equalShapes(series[i].shape, ChartSeriesElement.SHAPE_STAR);
				boolean equalStar2 = ChartSeriesElement.equalShapes(series[i].shape, ChartSeriesElement.SHAPE_STAR2);
				boolean equalPentagon = ChartSeriesElement.equalShapes(series[i].shape, ChartSeriesElement.SHAPE_PENTAGON);

				script.append(""+ sep);
				script.append("! Point shape selection" + sep);
				String mark = "set mark 20 3 "+pointSize+" 90 ! CIRCLE";
				if (equalCrux)
					mark = "set mark 4 1 "+pointSize+" 45 ! CRUX";
				if (equalStar)
					mark = "set mark 5 2 "+pointSize+" 0 ! STAR";
				if (equalStar2)
					mark = "set mark 5 2 "+pointSize+" 45 ! STAR2";
				if (equalPentagon)
					mark = "set mark 5 3 "+pointSize+" 0 ! PENTAGON";
				if (equalPoint)
					mark = "set mark 20 3 0 45 ! POINT";
				if (equalEmpty)
					mark = "set mark 20 3 0 45 ! POINT (FROM EMPTY SHAPE)";
				if (equalEllipse)
					mark += " (FROM ELLIPSE)";
				if (equalDiamond)
					mark = "set mark 4 3 "+pointSize+" 45 ! DIAMOND";
				if (equalSquare)
					mark = "set mark 4 3 "+pointSize+" 90 ! SQUARE";
				if (equalRectangleX)
					mark = "set mark 4 3 "+pointSize+" 90 ! SQUARE (FROM HORIZONTAL RECTANGLE)";
				if (equalRectangleY)
					mark = "set mark 4 3 "+pointSize+" 90 ! SQUARE (FROM VERTICAL RECTANGLE)";
				if (equalTriangleD)
					mark = "set mark 3 3 "+pointSize+" 180 ! TRIANGLE DOWN";
				if (equalTriangleL)
					mark = "set mark 3 3 "+pointSize+" 90 ! TRIANGLE LEFT";
				if (equalTriangleR)
					mark = "set mark 3 3 "+pointSize+" 270 ! TRIANGLE RIGHT";
				if (equalTriangleU)
					mark = "set mark 3 3 "+pointSize+" 0 ! TRIANGLE UP";

				ChartSeriesElement.setShapeSize(ChartSeriesElement.SHAPE_DEFAULT_SIZE);

				if (mark.indexOf("(FROM") >= 0) {
					warningMark = true;
				}

				script.append(mark + sep);
				pointSize = ""+pointSize0;

				script.append(""+ sep);
				script.append("! Read series" + sep);
				String subChart = "subchart"+subChartID;
				if (mainChart) subChart = "";
				script.append("column x 1 y 2 /file "+subChart+"serie"+i+fileName+".jparsec" + sep);
				script.append(""+ sep);
				if (series[i].showShapes) {
					script.append("points" + sep);
				}

				script.append(""+ sep);
				if (series[i].showLines || series[i].regressionType.getShowRegression() && series[i].regressionType != null &&
						(series[i].regressionType == ChartSeriesElement.REGRESSION.LINEAR_INTERPOLATION
						|| series[i].regressionType == ChartSeriesElement.REGRESSION.POLYNOMIAL
								|| series[i].regressionType == ChartSeriesElement.REGRESSION.GENERIC_FIT
								|| series[i].regressionType == ChartSeriesElement.REGRESSION.REGRESSION_CUSTOM
								|| series[i].regressionType == ChartSeriesElement.REGRESSION.LINEAR)) {
					if (!series[i].showShapes) script.append("points " + sep); ///blanking 0 1E300" + sep);
					if (series[i].regressionType != ChartSeriesElement.REGRESSION.LINEAR_INTERPOLATION &&
							series[i].regressionType != ChartSeriesElement.REGRESSION.NONE) {

						ArrayList<Object> vx = DataSet.getDoubleValuesIncludingLimits(series[i].xValues);
						ArrayList<Object> vy = DataSet.getDoubleValuesIncludingLimits(series[i].yValues);
						double x_val[] = (double[]) vx.get(0);
						double y_val[] = (double[]) vy.get(0);
						double dx_val[] = series[i].dxValues;
						double dy_val[] = series[i].dyValues;

						// Fit in LOG scale if necessary
						for (int j = 0; j < x_val.length; j++)
						{
							if (this.chart_elem.xAxisInLogScale && dx_val != null)
								dx_val[j] = dx_val[j] * Math.log10(Math.E) / x_val[j];
							if (this.chart_elem.yAxisInLogScale && dy_val != null)
								dy_val[j] = Math.abs(dy_val[j] * Math.log10(Math.E) / y_val[j]);
							if (this.chart_elem.xAxisInLogScale)
								x_val[j] = Math.log10(x_val[j]);
							if (this.chart_elem.yAxisInLogScale)
								y_val[j] = Math.log10(y_val[j]);
						}

						LinearFit myfit = null; //chart_elem.series[i].regressionType.getLinearFit();
						Polynomial pol = null; //chart_elem.series[i].regressionType.getPolynomialFit();
						GenericFit gf = null;
						String function = null;
						String[] gff = chart_elem.series[i].regressionType.getGenericFitFunctions();
						if (gff != null && gff.length >= 3) gf = new GenericFit(x_val, y_val, gff[0], gff[1], gff[2]);
						if (chart_elem.series[i].regressionType == ChartSeriesElement.REGRESSION.LINEAR) {
							if (myfit == null)
							{
								myfit = new LinearFit(x_val, y_val, dx_val, dy_val);
								myfit.linearFit();
								chart_elem.series[i].regressionType.setEquationFromLinearFit(myfit);
							}
						} else {
							if (chart_elem.series[i].regressionType == ChartSeriesElement.REGRESSION.POLYNOMIAL) {
								if (pol == null) {
									Regression reg = new Regression(x_val, y_val);
									reg.polynomial(chart_elem.series[i].regressionType.getPolynomialDegree());
									chart_elem.series[i].regressionType.setEquationValues(reg.getBestEstimates(), reg.getBestEstimatesErrors());
									pol = new Polynomial(reg.getBestEstimates());
								}
							} else {
								if (chart_elem.series[i].regressionType == ChartSeriesElement.REGRESSION.REGRESSION_CUSTOM) {
									function = chart_elem.series[i].regressionType.getCustomRegressionFitFunction();
									if (!chart_elem.series[i].regressionType.regressionDone()) {
										Regression reg = new Regression(x_val, y_val);
										reg.customFunction(function, chart_elem.series[i].regressionType.getCustomRegressionFitInitialEstimates());
										chart_elem.series[i].regressionType.setEquationValues(reg.getBestEstimates(), reg.getBestEstimatesErrors());
										if (chart_elem.series[i].regressionType.getEquation() == null || chart_elem.series[i].regressionType.getEquation().equals(""))
											chart_elem.series[i].regressionType.setEquation(function);
									}
								} else {
									if (chart_elem.series[i].regressionType == ChartSeriesElement.REGRESSION.GENERIC_FIT) {
										gf.fit();
										if (chart_elem.series[i].regressionType.getEquation() != null) {
											if (chart_elem.series[i].regressionType.getEquation().equals("")) chart_elem.series[i].regressionType.setEquation(gf.getFunction());
										} else {
											chart_elem.series[i].regressionType.setEquation(gf.getFunction());
										}
									}
								}
							}
						}

						double xmaxFit = this.chart_elem.getxMax();
						double xminFit = this.chart_elem.getxMin();
						double ymaxFit = this.chart_elem.getyMax();
						double yminFit = this.chart_elem.getyMin();
						if (yminFit < series[i].yMinimumValue)
							yminFit = series[i].yMinimumValue;
						if (xminFit < series[i].xMinimumValue)
							xminFit = series[i].xMinimumValue;

						int np = 2*chart_elem.series[i].xValues.length;
						double xv[] = DataSet.getSetOfValues(xminFit, xmaxFit, np+1, false);
						if (chart_elem.series[i].regressionType == ChartSeriesElement.REGRESSION.LINEAR) np = 1;
						for (int index = 0; index <np; index ++) {
							double p[] = null;
							if (chart_elem.series[i].regressionType == ChartSeriesElement.REGRESSION.LINEAR) {
								p = CreateChart.getPoint(chart_elem, myfit, i, xminFit, ymaxFit, yminFit);
							} else {
								if (chart_elem.series[i].regressionType == ChartSeriesElement.REGRESSION.POLYNOMIAL) {
									p = CreateChart.getPoint(chart_elem, pol, i, xv[index], ymaxFit, yminFit);
								} else {
									if (chart_elem.series[i].regressionType == ChartSeriesElement.REGRESSION.REGRESSION_CUSTOM) {
										p = CreateChart.getPoint(chart_elem, function, chart_elem.series[i].regressionType.getEquationValues(), i, xv[index], ymaxFit, yminFit);
									} else {
										if (chart_elem.series[i].regressionType == ChartSeriesElement.REGRESSION.GENERIC_FIT)
											p = CreateChart.getPoint(chart_elem, gf, i, xv[index], ymaxFit, yminFit);
									}
								}
							}
							script.append("draw r "+p[0]+" "+p[1]+" /user" + sep);
							if (chart_elem.series[i].regressionType == ChartSeriesElement.REGRESSION.LINEAR) {
								p = CreateChart.getPoint(chart_elem, myfit, i, xmaxFit, ymaxFit, yminFit);
							} else {
								if (chart_elem.series[i].regressionType == ChartSeriesElement.REGRESSION.POLYNOMIAL) {
									p = CreateChart.getPoint(chart_elem, pol, i, xv[index+1], ymaxFit, yminFit);
								} else {
									if (chart_elem.series[i].regressionType == ChartSeriesElement.REGRESSION.REGRESSION_CUSTOM) {
										p = CreateChart.getPoint(chart_elem, function, chart_elem.series[i].regressionType.getEquationValues(), i, xv[index+1], ymaxFit, yminFit);
									} else {
										if (chart_elem.series[i].regressionType == ChartSeriesElement.REGRESSION.GENERIC_FIT)
											p = CreateChart.getPoint(chart_elem, gf, i, xv[index+1], ymaxFit, yminFit);
									}
								}
							}
							script.append("draw line "+p[0]+" "+p[1]+" /user" + sep);
						}
					} else {
						if (series[i].regressionType == ChartSeriesElement.REGRESSION.LINEAR_INTERPOLATION || (series[i].showLines
								&& (!series[i].regressionType.getShowRegression() || series[i].regressionType != ChartSeriesElement.REGRESSION.SPLINE_INTERPOLATION)))
							script.append("connect" + sep);
					}
				}
				if (series[i].regressionType.getShowRegression() &&
						series[i].regressionType == ChartSeriesElement.REGRESSION.SPLINE_INTERPOLATION)
					script.append("curve" + sep);

				if (series[i].showLegend && leyendPosition != CreateChart.GILDAS_LEYEND.NO_LEYEND) {
					script.append(""+ sep);
					script.append("! SHOW LEGEND"+ sep);
					script.append("pen /wei 1"+ sep);
					script.append("set expand 0.7*(box_xmax-box_xmin)/24"+ sep);
					double posY = -2.1;
					double posXi = 0 + 24.0 * i / series.length;
					double posXf = (0 + 24.0 * (i+1) / series.length + posXi) * 0.5;

					// To set the position of the leyend in vertical direction from the
					// given corner. Otherwise, default leyend at the bottom.
					//double xmin = 4.0; //this.chart_elem.getxMin();
					//double xmax = 28.0; //this.chart_elem.getxMax();
					//double ymin = 2.5; //this.chart_elem.getyMin();
					//double ymax = 19.5; //this.chart_elem.getyMax();

					switch (leyendPosition)
					{
					case TOP_RIGHT_CORNER:
						posXi = 18;
						posXf = 19.5;
						posY = 16 - i * 0.6;
						break;
					case TOP_LEFT_CORNER:
						posXi = 0.5;
						posXf = 2.0;
						posY = 16 - i * 0.6;
						break;
					case BOTTOM_RIGHT_CORNER:
						posXi = 18;
						posXf = 19.5;
						posY = 0 + (series.length - i) * 0.6;
						break;
					case BOTTOM_LEFT_CORNER:
						posXi = 0.5;
						posXf = 2.0;
						posY = 0 + (series.length - i) * 0.6;
						break;
					default:
						break;
					}

					String pxi = ""+(posXi/24.0)+"*(box_xmax-box_xmin)";
					String pxf = ""+(posXf/24.0)+"*(box_xmax-box_xmin)";
					String py = ""+(posY/17.0)+"*(box_ymax-box_ymin)";
					script.append("draw r "+pxi+" "+py+" "+ sep);
					if (series[i].showLines) script.append("draw line "+pxf+" "+py+" "+ sep);
					if (series[i].showShapes) script.append("draw mark ("+pxi+"+"+pxf+")*0.5 "+py+" "+ sep);
					script.append("pen /wei 1 /col 0"+ sep);
					script.append("greg1\\draw text "+pxf+"+0.5 "+py+" \""+toGILDASformat(series[i].legend)+"\" 6 0 "+ sep);
					int index = color.indexOf("/col");
					script.append("pen "+color.substring(index, index+6)+ sep);
					script.append("set expand "+lastExpand+"*(box_xmax-box_xmin)/24" + sep);
				}

				script.append(""+ sep);
				script.append("pen /dashed 1 /wei 1 ! SELECT CONTINUOS THIN LINE"+ sep);
				if (series[i].showErrorBars && this.chart_elem.showErrorBars && series[i].dyValues != null) {
					script.append(""+ sep);
					script.append("! Read series with y errorbars" + sep);
					script.append("column x 1 y 2 z 4 /file "+subChart+"serie"+i+fileName+".jparsec" + sep);
					script.append("errorbar y" + sep);
				}
				if (series[i].showErrorBars && this.chart_elem.showErrorBars && series[i].dxValues != null) {
					script.append(""+ sep);
					script.append("! Read series with x errorbars" + sep);
					script.append("column x 1 y 2 z 3 /file "+subChart+"serie"+i+fileName+".jparsec" + sep);
					script.append("errorbar x" + sep);
				}
				script.append(""+ sep);

				if (series[i].showShapes) {
					String arrowSizeXMinus = "x[ii]*0.75/"+((double) series[i].sizeOfArrowInLimits/30.0);
					String arrowSizeXPlus = "x[ii]*1.25*"+((double) series[i].sizeOfArrowInLimits/30.0);
					String arrowSizeYMinus = "y[ii]*0.75/"+((double) series[i].sizeOfArrowInLimits/30.0);
					String arrowSizeYPlus = "y[ii]*1.25*"+((double) series[i].sizeOfArrowInLimits/30.0);
					if (!this.chart_elem.xAxisInLogScale) {
						arrowSizeXMinus = "x[ii]-"+(offsetx * (double) series[i].sizeOfArrowInLimits/22.5);
						arrowSizeXPlus = "x[ii]+"+(offsetx * (double) series[i].sizeOfArrowInLimits/22.5);
					}
					if (!this.chart_elem.yAxisInLogScale) {
						arrowSizeYMinus = "y[ii]-"+(offsety * (double) series[i].sizeOfArrowInLimits/22.5);
						arrowSizeYPlus = "y[ii]+"+(offsety * (double) series[i].sizeOfArrowInLimits/22.5);
					}

					// True to show empty triangles, false for filled ones
					boolean emptyTriangles = true;
					script.append(""+ sep);
					script.append("! Read series with y limits. Set a mark 3 0 ... for empty triangles, or 3 3 ... for filled ones" + sep);
					script.append("column x 1 y 2 z 6 /file "+subChart+"serie"+i+fileName+".jparsec" + sep);
					script.append("for ii 1 to NXY" + sep);
					script.append(" IF z[ii].EQ.1 THEN" + sep);
					if (emptyTriangles) {
						script.append("   set mark 3 0 "+pointSize+" 180 ! TRIANGLE DOWN" + sep);
					} else {
						script.append("   set mark 3 3 "+pointSize+" 180 ! TRIANGLE DOWN" + sep);
					}
					script.append("   draw marker x[ii] "+arrowSizeYMinus+" /user" + sep);
					script.append("   draw line x[ii] y[ii] /user" + sep);
					script.append(" END IF" + sep);
					script.append(" IF z[ii].EQ.-1 THEN" + sep);
					if (emptyTriangles) {
						script.append("   set mark 3 0 "+pointSize+" 0 ! TRIANGLE UP" + sep);
					} else {
						script.append("   set mark 3 3 "+pointSize+" 0 ! TRIANGLE UP" + sep);
					}
					script.append("   draw marker x[ii] "+arrowSizeYPlus+" /user" + sep);
					script.append("   draw line x[ii] y[ii] /user" + sep);
					script.append(" END IF" + sep);
					script.append("next" + sep);
					script.append(""+ sep);

					script.append("! Read series with x limits. Set a mark 3 0 ... for empty triangles, or 3 3 ... for filled ones" + sep);
					script.append("column x 1 y 2 z 5 /file "+subChart+"serie"+i+fileName+".jparsec" + sep);
					script.append("for ii 1 to NXY" + sep);
					script.append(" IF z[ii].EQ.1 THEN" + sep);
					if (emptyTriangles) {
						script.append("   set mark 3 0 "+pointSize+" 90 ! TRIANGLE LEFT" + sep);
					} else {
						script.append("   set mark 3 3 "+pointSize+" 90 ! TRIANGLE LEFT" + sep);
					}
					script.append("   draw marker "+arrowSizeXMinus+" y[ii] /user" + sep);
					script.append("   draw line x[ii] y[ii] /user" + sep);
					script.append(" END IF" + sep);
					script.append(" IF z[ii].EQ.-1 THEN" + sep);
					if (emptyTriangles) {
						script.append("   set mark 3 0 "+pointSize+" 270 ! TRIANGLE RIGHT" + sep);
					} else {
						script.append("   set mark 3 3 "+pointSize+" 270 ! TRIANGLE RIGHT" + sep);
					}
					script.append("   draw marker "+arrowSizeXPlus+" y[ii] /user" + sep);
					script.append("   draw line x[ii] y[ii] /user" + sep);
					script.append(" END IF" + sep);
					script.append("next" + sep);
					script.append(""+ sep);

				}

				int npo = 0;
				try { npo = series[i].pointers.length; } catch (Exception exc) {}
				if (npo > 0) {
					script.append("! Show pointers"+ sep);
					if (mainChart && !warningPointer) {
						script.append("define real px1" + sep);
						script.append("define real py1" + sep);
						script.append("define real px2" + sep);
						script.append("define real py2" + sep);
						script.append("define real cosangle" + sep);
						script.append("define real sinangle" + sep);
						warningPointer = true;
					}

					for (int iii=0; iii<series[i].pointers.length; iii++)
					{
						if (series[i].pointers[iii] != null)
						{
							String pointerMsg = series[i].pointers[iii];
							double pointerX = 0.0, pointerY = 0.0;
							if (pointerMsg.startsWith("(")) {
								int pp = pointerMsg.indexOf(")");
								String ppp = pointerMsg.substring(1, pp);
								int coma = ppp.indexOf(",");
								String p1 = ppp.substring(0, coma);
								String p2 = ppp.substring(coma+1);
								String msg = pointerMsg.substring(pp+1).trim();
								pointerMsg = "";
								for (int ijk=0; ijk<series[i].xValues.length; ijk++)
								{
									if (p1.trim().equals(series[i].xValues[ijk]) &&
											p2.trim().equals(series[i].yValues[ijk])) {
										pointerMsg = msg;
										pointerX = DataSet.getDoubleValueWithoutLimit(p1);
										pointerY = DataSet.getDoubleValueWithoutLimit(p2);

										if (this.chart_elem.chartType == ChartElement.TYPE.CATEGORY_CHART) {
											pointerX = (this.chart_elem.getIndexOfCategoryPoint(p1) + 1.0) / (this.chart_elem.xForCategoryCharts.length + 1.0);
										}
										break;
									}
								}
								if (pointerMsg.equals("")) {
									pointerX = DataSet.getDoubleValueWithoutLimit(p1);
									pointerY = DataSet.getDoubleValueWithoutLimit(p2);
									pointerMsg = msg;
									if (this.chart_elem.chartType == ChartElement.TYPE.CATEGORY_CHART) {
										pointerX = (this.chart_elem.getIndexOfCategoryPoint(p1) + 1.0) / (this.chart_elem.xForCategoryCharts.length + 1.0);
									}
								}
							} else {
								int n = Integer.parseInt(FileIO.getField(1, pointerMsg, " ", true));
								pointerX = DataSet.getDoubleValueWithoutLimit(chart_elem.series[i].xValues[n-1]);
								pointerY = DataSet.getDoubleValueWithoutLimit(chart_elem.series[i].yValues[n-1]);
								pointerMsg = FileIO.getRestAfterField(1, pointerMsg, " ", true);
								if (this.chart_elem.chartType == ChartElement.TYPE.CATEGORY_CHART) {
									pointerX = (this.chart_elem.getIndexOfCategoryPoint(chart_elem.series[i].xValues[n-1]) + 1.0) / (this.chart_elem.xForCategoryCharts.length + 1.0);
								}
							}


								double prMin = 0.3 * series[i].pointersLabelOffsetFactor;
								if (this.chart_elem.chartType == ChartElement.TYPE.CATEGORY_CHART) {
									script.append("draw relocate user_xmin+(user_xmax-user_xmin)*"+pointerX+" "+pointerY+" /user" + sep);
								} else {
									script.append("draw relocate "+pointerX+" "+pointerY+" /user" + sep);
								}
								script.append("let px1 (x_pen-4-12)" + sep);
								script.append("let py1 (y_pen-2.5-8.5)" + sep);

								double angle = 0.0;
								ChartSeriesElement.POINTER_ANGLE ang = chart_elem.series[i].pointersAngle;

								if (pointerMsg.startsWith("@UP")) {
									ang = ChartSeriesElement.POINTER_ANGLE.UPWARDS;
									pointerMsg = pointerMsg.substring(3);
								}
								if (pointerMsg.startsWith("@DOWN")) {
									ang = ChartSeriesElement.POINTER_ANGLE.DOWNWARDS;
									pointerMsg = pointerMsg.substring(5);
								}
								if (pointerMsg.startsWith("@LEFT")) {
									ang = ChartSeriesElement.POINTER_ANGLE.LEFTWARDS;
									pointerMsg = pointerMsg.substring(5);
								}
								if (pointerMsg.startsWith("@RIGHT")) {
									ang = ChartSeriesElement.POINTER_ANGLE.RIGHTWARDS;
									pointerMsg = pointerMsg.substring(6);
								}
								String center = "5";
								if (ang == ChartSeriesElement.POINTER_ANGLE.RIGHTWARDS) center = "6";
								if (ang == ChartSeriesElement.POINTER_ANGLE.LEFTWARDS) center = "4";
								if (ang == ChartSeriesElement.POINTER_ANGLE.UPWARDS) center = "8";
								if (ang == ChartSeriesElement.POINTER_ANGLE.DOWNWARDS) center = "2";
								if (pointerMsg.startsWith("@CENTER")) {
									int c = Integer.parseInt(pointerMsg.substring(7,8));
									center = ""+c;
									pointerMsg = pointerMsg.substring(8);
								}

								if (ang == ChartSeriesElement.POINTER_ANGLE.DOWNWARDS) angle = -Math.PI * 0.5;
								if (ang == ChartSeriesElement.POINTER_ANGLE.UPWARDS) angle = Math.PI * 0.5;
								if (ang == ChartSeriesElement.POINTER_ANGLE.LEFTWARDS) angle = Math.PI;
								if (ang == ChartSeriesElement.POINTER_ANGLE.RIGHTWARDS) angle = 0.0;
								if (ang != ChartSeriesElement.POINTER_ANGLE.DOWNWARDS &&
										ang != ChartSeriesElement.POINTER_ANGLE.UPWARDS &&
										ang != ChartSeriesElement.POINTER_ANGLE.LEFTWARDS &&
										ang != ChartSeriesElement.POINTER_ANGLE.RIGHTWARDS &&
										ang != ChartSeriesElement.POINTER_ANGLE.TO_OUTSIDE &&
										ang != ChartSeriesElement.POINTER_ANGLE.TO_CENTER) angle = 0; //-ang;

								if (ang == ChartSeriesElement.POINTER_ANGLE.TO_OUTSIDE ||
										ang == ChartSeriesElement.POINTER_ANGLE.TO_CENTER) {
									String sign = "";
									if (ang == ChartSeriesElement.POINTER_ANGLE.TO_CENTER) sign = "-";
									script.append("let cosangle "+sign+"px1/sqrt(px1*px1+py1*py1)" + sep);
									script.append("let sinangle "+sign+"py1/sqrt(px1*px1+py1*py1)" + sep);
								} else {
									script.append("let cosangle " + Math.cos(angle) + sep);
									script.append("let sinangle " + Math.sin(angle) + sep);
								}
								script.append("let px1 (x_pen-4)+"+prMin+"*cosangle" + sep);
								script.append("let py1 (y_pen-box_ymin)+"+prMin+"*sinangle" + sep);
								double prMax = 0.8 * series[i].pointersLabelOffsetFactor;
								script.append("let px2 px1+"+prMax+"*cosangle" + sep);
								script.append("let py2 py1+"+prMax+"*sinangle" + sep);
								// Required to recover the correct color
								String text = toGILDASformat(pointerMsg.trim());
								script.append("pen /col "+col+sep);
								String labelPoint = "1";
								if (series[i].showArrowInPointers)
								{
									script.append("draw relocate px2 py2" + sep);
									script.append("draw arrow px1 py1" + sep);
									labelPoint = "2";
								}
								script.append("set expand 0.5*(box_xmax-box_xmin)/24"+sep);
								writeGildasText(script, pointerMsg.trim(), "px"+labelPoint+"+"+(prMin*1.5)+"*cosangle", "py"+labelPoint+"+"+(prMin*1.5)+"*sinangle", center, lastExpand);
						}

					}
					script.append(""+ sep);
				}

				script.append("! END OF COMMANDS FOR DRAWING SERIES NUMBER "+i+" - "+series[i].legend + sep);
				script.append(""+ sep);

				StringBuffer serie = new StringBuffer(1000);
				serie.append("! Dataset for series "+i+" - "+series[i].legend + sep);
				serie.append("! Columns are x, y, dx, dy, x limits (1 = upper, -1 = lower, 0 = none), y limits" + sep);
				for (int j=0; j<series[i].xValues.length; j++)
				{
					String x = DataSet.getStringValueWithoutLimit(series[i].xValues[j]);
					if (this.chart_elem.chartType == ChartElement.TYPE.CATEGORY_CHART) {
						for (int jj=0;jj<this.chart_elem.xForCategoryCharts.length; jj++)
						{
							if (this.chart_elem.xForCategoryCharts[jj].equals(series[i].xValues[j])) {
								x = ""+jj;
								break;
							}
						}
					}
					double y = DataSet.getDoubleValueWithoutLimit(series[i].yValues[j]);
					double dx = 0.0;
					if (series[i].dxValues != null) dx = series[i].dxValues[j];
					double dy = 0.0;
					if (series[i].dyValues != null) dy = series[i].dyValues[j];

					int limitX = 0, limitY = 0;
					if (series[i].xValues[j].startsWith("<")) limitX = 1;
					if (series[i].xValues[j].startsWith(">")) limitX = -1;
					if (series[i].yValues[j].startsWith("<")) limitY = 1;
					if (series[i].yValues[j].startsWith(">")) limitY = -1;
					if (limitX != 0) dx = 0.0;
					if (limitY != 0) dy = 0.0;
					serie.append(x+"  "+y+"  "+dx+"  "+dy+"  "+limitX+"  "+limitY + sep);
				}
				if (mainChart) {
					WriteFile.writeAnyExternalFile(path+"serie"+i+fileName+".jparsec", serie.toString());
				} else {
					WriteFile.writeAnyExternalFile(path+"subchart"+subChartID+"serie"+i+fileName+".jparsec", serie.toString());
				}
			}
		}

		script.append("! SET FONT AND CALCULATE SCALES"+ sep);
		script.append("pen /col 0 /dashed 1 /wei 1" + sep);
		script.append("set font duplex"+ sep);
		if (mainChart) {
			script.append("define real px"+ sep);
			script.append("define real py"+ sep);
			script.append("define real mx"+ sep);
			script.append("define real my"+ sep);
			script.append("define real ratioxy"+ sep);
		}
		//script.append("limits * * * *"+ sep);
		script.append(limit + sep);
		script.append("let mx = user_xmax-user_xmin"+ sep);
		script.append("let my = user_ymax-user_ymin"+ sep);
		script.append(""+ sep);
		script.append("! DRAW X AND Y AXIS LABELS"+ sep);
		script.append("let px user_xmin+mx*0.5"+ sep);
		if (mainChart) {
			script.append("let ratioxy ((box_ymax-box_ymin)/(box_xmax-box_xmin))/0.708"+ sep);
			script.append("IF ratioxy.LT.1 THEN" + sep);
			script.append(" let py user_ymax-ratioxy*(user_ymax-user_ymin)-my*0.08"+ sep);
			script.append("ELSE" + sep);
			script.append(" let py user_ymin-my*0.08"+ sep);
			script.append("END IF" + sep);
		} else {
			script.append("let py user_ymin-my*0.12"+ sep);
		}
		script.append("greg1\\draw text px py \""+CreateChart.toGILDASformat(chart_elem.xLabel.trim())+"\" 5 0 /user"+ sep);
		script.append("let py user_ymin+my*0.5"+ sep);
		if (mainChart) {
			script.append("let px user_xmin-mx*(0.1*character_size/0.6)"+ sep);
		} else {
			script.append("let px user_xmin-mx*(0.14*character_size/0.6)"+ sep);
		}
		script.append("greg1\\draw text px py \""+CreateChart.toGILDASformat(chart_elem.yLabel.trim())+"\" 5 90 /user"+ sep);
		script.append(""+ sep);
		script.append("! DRAW TITLE"+ sep);
		script.append("let px user_xmin+mx*0.5"+ sep);
		script.append("let py user_ymin+my*1.05"+ sep);
		script.append("greg1\\draw text px py \""+CreateChart.toGILDASformat(chart_elem.title.trim())+"\" 5 0 /user"+ sep);
		script.append(""+ sep);
		if (this.chart_elem.chartType == ChartElement.TYPE.CATEGORY_CHART) {
			script.append("! DRAW X AXIS LABELS"+ sep);
			script.append("set font simplex"+ sep);
			int n = this.chart_elem.xForCategoryCharts.length;
			script.append("let py user_ymin-my*0.03"+ sep);
			for (int i=0; i<n; i++)
			{
				double frac = (i + 1.0) / ((double) n + 1.0);
				if (i <= xmax) script.append("greg1\\draw text user_xmin+mx*"+frac+" py \""+toGILDASformat(this.chart_elem.xForCategoryCharts[i].trim())+"\" 5 0 /user"+ sep);
			}
		}
		script.append(""+ sep);
		if (warningColor || warningMark) script.append("! SHOW WARNINGS"+ sep);
		if (warningColor) script.append("SAY \"WARNING: ONE OR MORE COLORS IN THE CHART ARE NOT SUPPORTED BY GILDAS.\""+ sep);
		if (warningMark) script.append("SAY \"WARNING: ONE OR MORE MARKERS IN THE CHART ARE NOT SUPPORTED BY GILDAS.\""+ sep);
		if (warningColor || warningMark) script.append(""+ sep);

		return script;
	}

	// Different text sizes, colors, and angles ?
	private void writeGildasText(StringBuffer script, String text, String px, String py, String center, double lastExpand) {
		String sep = FileIO.getLineSeparator();
		String t = CreateChart.toGILDASformat(text);
		String tt[] = DataSet.toStringArray(t, "@BREAK", true);
		String dx[] = DataSet.toStringArray(textdx, "@BREAK", true);
		String dy[] = DataSet.toStringArray(textdy, "@BREAK", true);
		if (dx.length == 0) dx = new String[] {""};
		if (dy.length == 0) dy = new String[] {""};
		if (textSize != 1.0) script.append("set expand " + (textSize*lastExpand) + sep);
		if (col >= 0) script.append("pen /col "+col+sep);
		String ddx = "", ddy = "";
		for (int i=0; i<tt.length; i++) {
			if (dx.length > i) ddx += dx[i];
			if (dy.length > i) ddy += dy[i];
			script.append("greg1\\draw text "+px+ddx+" "+py+ddy+" \""+tt[i]+"\" "+center+" "+ textAngle + sep);
		}
		script.append("set expand " + lastExpand + sep);
	}

	private static double textAngle = 0, textSize = 1.0;
	private static String textdx = "", textdy = "";
	private static int col = 0;

	/**
	 * Transforms a given string with subindex, superindex, or greek letters
	 * from JPARSEC to GILDAS format.
	 * @param label A string in the JPARSEC format.
	 * @return The string in the GILDAS format.
	 */
	public static String toGILDASformat(String label)
	{
        String commands[] = new String[] {"RED", "GREEN", "BLUE", "SIZE", "BLACK", "WHITE",
       		 "CYAN", "GRAY", "MAGENTA", "ORANGE", "PINK", "YELLOW", "alpha", "beta", "gamma",
       		 "delta", "epsilon", "zeta", "eta", "theta", "iota", "kappa", "lambda", "mu", "nu",
    			 "xi", "omicron", "pi", "rho", "sigma", "tau", "upsilon", "phi", "chi", "psi", "omega",
    			 "sun", "mercury", "venus", "earth", "mars", "jupiter",
    			 "saturn", "uranus", "neptune", "pluto", "aries", "taurus",
    			 "gemini", "cancer", "leo", "virgo", "libra", "scorpius",
    			 "sagittarius", "capricornus", "aquarius", "pisces", "ROTATE", "MOVEX", "MOVEY"};

        String greek = "abgdezhqiklmnxoprstufcyw ";
        String greekCapital = "ABGDEZHQIKLMNXOPRSTUFCYW ";
        String gildas = label;
        int lastColIndex = 0;
        textAngle = 0;
        col = -1;
        textSize = 1.0;
        textdx = "";
        textdy = "";
        for (int i=0; i<commands.length; i++)
        {
        	if (i < 12 && i != 3) {
        		int p = gildas.toLowerCase().indexOf("@"+commands[i].toLowerCase());
        		if (p >= 0 && p >= lastColIndex) {
        			lastColIndex = p;
        			if (i == 4) col = 0;
        			if (i == 7) col = 0;
        			if (i == 0) col = 1;
        			if (i == 1) col = 2;
        			if (i == 2) col = 3;
        			if (i == 6) col = 4;
        			if (i == 5) col = 7;
        			if (i == 8) col = 6;
        			if (i == 9) col = 5;
        			if (i == 10) col = 6;
        			if (i == 11) col = 5;
        		}
        	}

        	String replace = "";
        	String replaceCapital = "";
        	if (i > 11 && i <= 35) replace = "\\g"+greek.substring(i-12, i-11);
        	if (i > 11 && i <= 35) replaceCapital = "\\g"+greekCapital.substring(i-12, i-11);
        	if (i == 36) replace = replaceCapital = "o";
        	if (i > 36) replace = replaceCapital = " ";
        	if (commands[i].equals("SIZE") || commands[i].equals("ROTATE") || commands[i].equals("MOVEX")
        			|| commands[i].equals("MOVEY")) {
        		int p = gildas.toLowerCase().indexOf("@"+commands[i].toLowerCase());
        		if (p >= 0) {
	        		do {
	        			int s = commands[i].length() + 1;
	        			String v = "0123456789.-";
	        			int e = p + s;
	        			do {
	        				e = e + 1;
	        			} while ((e -(p+s)) < 3 && e < gildas.length() && v.indexOf(gildas.substring(e, e+1)) >= 0);
	                	if (commands[i].equals("ROTATE")) {
	                		textAngle = -Double.parseDouble(gildas.substring(p+s, e));
	                	} else {
		                	if (commands[i].equals("MOVEX")) {
		                		textdx += "@BREAK+((box_xmax-box_xmin)*("+Double.parseDouble(gildas.substring(p+s, e))+"/1000.0))";
		                	} else {
			                	if (commands[i].equals("MOVEY")) {
			                		textdy += "@BREAK+((box_xmax-box_xmin)*("+Double.parseDouble(gildas.substring(p+s, e))+"/1000.0))";
			                	} else {
			                		textSize = Double.parseDouble(gildas.substring(p+s, e)) / 12.0;
			                	}
		                	}
	                	}
	        			String after = "";
	        			if (e < gildas.length()) after = gildas.substring(e);
	        			if (commands[i].startsWith("MOVE")) after = "@BREAK" + after;
	        			gildas = gildas.substring(0, p) + after;
	        			p = gildas.toLowerCase().indexOf("@"+commands[i].toLowerCase());
	        		} while (p >= 0);
        		}
        	} else {
	        	gildas = DataSet.replaceAll(gildas, "@"+commands[i].toLowerCase(), replace, true);
	        	gildas = DataSet.replaceAll(gildas, "@"+commands[i].toUpperCase(), replaceCapital, true);
        	}
        }

		int i = gildas.lastIndexOf("^{");
		if (i>= 0)
		{
			do {
				String part1 = gildas.substring(0, i+2);
				String part2 = gildas.substring(i+2);
				int j = part2.indexOf("}");

				gildas = part1.substring(0, part1.length()-2) + "\\\\u" + part2.substring(0, j) + "\\\\d";
				if (j < part2.length()-1) gildas += part2.substring(j+1);
				i = gildas.lastIndexOf("^{");
			} while (i>=0);
		}
		i = gildas.lastIndexOf("_{");
		if (i>= 0)
		{
			do {
				String part1 = gildas.substring(0, i+2);
				String part2 = gildas.substring(i+2);
				int j = part2.indexOf("}");

				gildas = part1.substring(0, part1.length()-2) + "\\\\d" + part2.substring(0, j) + "\\\\u";
				if (j < part2.length()-1) gildas += part2.substring(j+1);
				i = gildas.lastIndexOf("_{");
			} while (i>=0);
		}
		gildas = DataSet.replaceAll(gildas, "^{", "\\\\u", true);
		gildas = DataSet.replaceAll(gildas, "_{", "\\\\d", true);
		gildas = DataSet.replaceAll(gildas, "\"", "''", true);

		return gildas;
	}

	/**
	 * Creates a panel with several charts for GILDAS, with no separation between the
	 * charts and always the same ratio.
	 * @param charts The charts.
	 * @param nx The number of chartx in the x axis.
	 * @param ny The number of charts in the y axis.
	 * @param names The names of the different output files for the charts.
	 * @param superName The name of the main GILDAS panel.
	 * @param dir Path to the output directory.
	 * @param leyendPosition The position of the leyend in the charts.
	 * @param drawTitles True to draw titles slightly inside the charts.
	 * @param autoDestroy True to add commands to autodestroy the script.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void exportAsSuperScriptForGILDAS(CreateChart charts[], int nx, int ny,
			String names[], String superName, String dir, GILDAS_LEYEND leyendPosition, boolean drawTitles,
			boolean autoDestroy)
	throws JPARSECException {
		CreateChart.exportAsSuperScriptForGILDAS(charts, nx, ny, names, superName, dir, leyendPosition, drawTitles, autoDestroy, false, false, true, 0, 0);
	}

	/**
	 * Creates a panel with several charts for GILDAS, with no separation between the
	 * charts and always the same ratio.
	 * @param charts The charts.
	 * @param nx The number of chartx in the x axis.
	 * @param ny The number of charts in the y axis.
	 * @param names The names of the different output files for the charts.
	 * @param superName The name of the main GILDAS panel.
	 * @param dir Path to the output directory.
	 * @param leyendPosition The position of the leyend in the charts.
	 * @param drawTitles True to draw titles slightly inside the charts.
	 * @param autoDestroy True to add commands to autodestroy the script.
	 * @param drawPanelIndex True to draw panel label as a character or number.
	 * @param panelIndexAsNumber True to draw panel label as a character.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void exportAsSuperScriptForGILDAS(CreateChart charts[], int nx, int ny,
			String names[], String superName, String dir, GILDAS_LEYEND leyendPosition, boolean drawTitles,
			boolean autoDestroy, boolean drawPanelIndex, boolean panelIndexAsNumber)
	throws JPARSECException {
		CreateChart.exportAsSuperScriptForGILDAS(charts, nx, ny, names, superName, dir, leyendPosition, drawTitles, autoDestroy, false, false, true, 0, 0);
	}

	/**
	 * Creates a panel with several charts for GILDAS. Titles are drawn outside the chart area.
	 * @param charts The charts.
	 * @param nx The number of chartx in the x axis.
	 * @param ny The number of charts in the y axis.
	 * @param names The names of the different output files for the charts.
	 * @param superName The name of the main GILDAS panel.
	 * @param dir Path to the output directory.
	 * @param leyendPosition The position of the leyend in the charts.
	 * @param drawTitles True to draw titles slightly inside the charts.
	 * @param autoDestroy True to add commands to autodestroy the script.
	 * @param drawPanelIndex True to draw panel label as a character or number.
	 * @param panelIndexAsNumber True to draw panel label as a character.
	 * @param sameRatio true to produce charts with the same ratio always.
	 * @param sepx separation between the charts in x axis, as a percentage of
	 * the width of each chart (0 to 100).
	 * @param sepy separation between the charts in y axis, as a percentage of
	 * the height of each chart (0 to 100).
	 * @throws JPARSECException If an error occurs.
	 */
	public static void exportAsSuperScriptForGILDAS(CreateChart charts[], int nx, int ny,
			String names[], String superName, String dir, GILDAS_LEYEND leyendPosition, boolean drawTitles,
			boolean autoDestroy, boolean drawPanelIndex, boolean panelIndexAsNumber, boolean sameRatio,
			int sepx, int sepy)
	throws JPARSECException {
		CreateChart.exportAsSuperScriptForGILDAS(charts, nx, ny, names, superName, dir, leyendPosition, drawTitles, autoDestroy, drawPanelIndex, panelIndexAsNumber, sameRatio, sepx, sepy, false, false);
	}

	/**
	 * Creates a panel with several charts for GILDAS.
	 * @param charts The charts.
	 * @param nx The number of chartx in the x axis.
	 * @param ny The number of charts in the y axis.
	 * @param names The names of the different output files for the charts.
	 * @param superName The name of the main GILDAS panel.
	 * @param dir Path to the output directory.
	 * @param leyendPosition The position of the leyend in the charts.
	 * @param drawTitles True to draw titles slightly outside or inside (see last input parameter) the chart top limit.
	 * @param autoDestroy True to add commands to autodestroy the script.
	 * @param drawPanelIndex True to draw panel label as a character or number.
	 * @param panelIndexAsNumber True to draw panel label as a character.
	 * @param sameRatio true to produce charts with the same ratio always.
	 * @param sepx separation between the charts in x axis, as a percentage of
	 * the width of each chart (0 to 100).
	 * @param sepy separation between the charts in y axis, as a percentage of
	 * the height of each chart (0 to 100).
	 * @param axisRangeAlways True to show the range of values in the axes always.
	 * @param titlesInside True to show the tittles of each chart slightly inside the
	 * chart area, false to show them outside.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void exportAsSuperScriptForGILDAS(CreateChart charts[], int nx, int ny,
			String names[], String superName, String dir, GILDAS_LEYEND leyendPosition, boolean drawTitles,
			boolean autoDestroy, boolean drawPanelIndex, boolean panelIndexAsNumber, boolean sameRatio,
			int sepx, int sepy, boolean axisRangeAlways, boolean titlesInside)
	throws JPARSECException {
		if (!dir.endsWith(FileIO.getFileSeparator())) dir += FileIO.getFileSeparator();
		if (superName.endsWith(".greg")) superName = superName.substring(0, superName.length()-5);

		double xmin = 4, xmax = 28, ymin = 2.5, ymax = 19.5;
		boolean xaxis[] = new boolean[names.length];
		boolean yaxis[] = new boolean[names.length];
		int maxN = nx;
		if (ny > nx) maxN = ny;
		int maxNx = maxN, maxNy = maxN;
		if (!sameRatio) {
			maxNx = nx;
			maxNy = ny;
		}

		String sep = FileIO.getLineSeparator();
		StringBuffer script = new StringBuffer("! SCRIPT TO DRAW A CHART PANEL WITH GILDAS"+ sep);
		script.append("! AUTOMATICALLY GENERATED BY JPARSEC PACKAGE"+ sep);
		script.append("! ON " + (new Date().toString())+ sep);
		script.append(""+ sep);
		int index = -1;
		for (int y = 1; y<=ny; y++)
		{
			for (int x = 1; x<= nx; x++)
			{
				double bx0 = xmin + (double) (x - 1) * (xmax - xmin) / (double) nx;
				double bxf = bx0 + (xmax - xmin) / (double) maxNx;

				double byf = (ymin + (double) (y - 1) * (ymax - ymin) / (double) ny);
				double by0 = (byf + (ymax - ymin) / (double) maxNy);

				by0 = ymax + ymin - by0;
				byf = ymax + ymin - byf;

				if (sepx > 0 && sepx < 100) bxf = bxf - (bxf - bx0) * sepx / 100.0;
				if (sepy > 0 && sepy < 100) by0 = by0 - (by0 - byf) * sepy / 100.0;

				index ++;
				if (index < names.length)
				{
					script.append("set box "+bx0+" "+bxf+" "+by0+" "+byf + sep);
					script.append("@"+names[index]+".greg" + sep + sep);

					xaxis[index] = yaxis[index] = true;
					if (x > 1 && !axisRangeAlways) yaxis[index] = false;
					if (y < ny && !axisRangeAlways) xaxis[index] = false;
				}
			}
		}

		File ps = new File(dir+superName+".ps");
		if (ps.exists()) script.append("sys \"rm "+superName+".ps\""+ sep);
		script.append("hard "+superName+".ps /dev ps color"+sep);
		if (autoDestroy)
		{
			script.append(""+ sep);
			script.append(""+ sep);
			script.append("! REMOVE ALL UNNECESARY FILES"+ sep);
			script.append("sys \"rm "+superName+".greg\""+ sep);
		}

		WriteFile.writeAnyExternalFile(dir+superName+".greg", script.toString());

		boolean inside = titlesInside; //false;
/*		for (int i=nx; i<names.length; i++)
		{
			if (charts[i].getChartElement().title != null && !charts[i].getChartElement().title.trim().equals("")) {
				inside = false;
				break;
			}
		}
*/
		for (int i=0; i<names.length; i++)
		{
			String fileName = names[i];
			String gscript = charts[i].exportAsScriptForGILDAS(dir+fileName, leyendPosition, autoDestroy);
			if (drawTitles)
			{
				if (inside) {
					// Draw titles inside
					gscript = DataSet.replaceOne(gscript, "let py user_ymin+my*1.05", "let py user_ymin+my*(1.0-0.065*character_size/0.6)", 1);
				} else {
					// Draw titles outside
					gscript = DataSet.replaceOne(gscript, "let py user_ymin+my*1.05", "let py user_ymin+my*(1.0+0.075*character_size/0.6)", 1);
				}
			} else {
				gscript = DataSet.replaceOne(gscript, "greg1\\draw text px py", "!greg1\\draw text px py", 3);
			}

			if (drawPanelIndex)
			{
				String panelIndex = ""+(i+1);
				if (!panelIndexAsNumber && i < 26) {
					try {
						panelIndex = "abcdefghijklmnopqrstuvwxyz".substring(i, i+1);
					} catch (Exception exc) {}
				}
				gscript = DataSet.replaceOne(gscript, "! DRAW TITLE",
						"let py user_ymin+my*(1.0-0.05*character_size/0.6)"+FileIO.getLineSeparator()+
						"let px user_xmin+mx*0.05"+FileIO.getLineSeparator()+
						"greg1\\draw text px py \""+panelIndex+")\" 5 0 /user"+FileIO.getLineSeparator()+"! DRAW TITLE", 1);
			}

			String box = "box ";
			if (!xaxis[i]) {
				box += "N ";
				gscript = DataSet.replaceOne(gscript, "greg1\\draw text px py", "!greg1\\draw text px py", 1);
			} else {
				box += "P ";
				if (ny > 1) gscript = DataSet.replaceOne(gscript, "-my*0.08", "-my*0.08*"+ny, 1);
			}
			if (!yaxis[i]) {
				box += "N ";
				gscript = DataSet.replaceOne(gscript, "greg1\\draw text px py", "!greg1\\draw text px py", 2);
			} else {
				box += "O ";
			}
			gscript = DataSet.replaceOne(gscript, sep+"box", sep+box, 1);
			gscript = DataSet.replaceOne(gscript, sep+"sys ", sep+"!sys ", 1);
			gscript = DataSet.replaceOne(gscript, sep+"hard", sep+"!hard", 1);
			if (gscript.indexOf(sep+"exit") > 0)
				gscript = gscript.substring(0, gscript.indexOf(sep+"exit"));

			WriteFile.writeAnyExternalFile(dir+names[i]+".greg", gscript);
		}
	}

	private double posx[], posy[];
	private ChartSeriesElement.POINTER_ANGLE getMostEmptyDirection(ChartElement chart_elem, int series, int point, XYPlot plot) throws JPARSECException
	{
		double pointerX = DataSet.getDoubleValueWithoutLimit(chart_elem.series[series].xValues[point]);
		double pointerY = DataSet.getDoubleValueWithoutLimit(chart_elem.series[series].yValues[point]);

		ValueAxis y_axis = plot.getRangeAxis();
		ValueAxis x_axis = plot.getDomainAxis();

		double posx = x_axis.valueToJava2D(pointerX, new Rectangle2D.Double(0, 0,
			chart_elem.imageWidth, chart_elem.imageHeight), plot
			.getDomainAxisEdge());
		double posy = y_axis.valueToJava2D(pointerY, new Rectangle2D.Double(0, 0,
			chart_elem.imageWidth, chart_elem.imageHeight), plot
			.getRangeAxisEdge());

		return getMostEmptyDirection(chart_elem, posx, posy, plot);
	}

	private ChartSeriesElement.POINTER_ANGLE getMostEmptyDirection(ChartElement chart_elem, double posx0, double posy0, XYPlot plot)
	{
		ChartSeriesElement.POINTER_ANGLE dir = ChartSeriesElement.POINTER_ANGLE.DOWNWARDS;

		try {
			ValueAxis y_axis = plot.getRangeAxis();
			ValueAxis x_axis = plot.getDomainAxis();

			int ns = chart_elem.series.length;
			int np = 0;
			for (int i=0; i<ns; i++)
			{
				np = np + chart_elem.series[i].xValues.length;
			}
			if (posx == null || posy == null)
			{
				posx = new double[np];
				posy = new double[np];
				int index = -1;
				Rectangle2D rect = new Rectangle2D.Double(0, 0, chart_elem.imageWidth, chart_elem.imageHeight);
				for (int i=0; i<ns; i++)
				{
					for (int j=0; j<chart_elem.series[i].xValues.length; j++)
					{
							double pointerX = DataSet.getDoubleValueWithoutLimit(chart_elem.series[i].xValues[j]);
							double pointerY = DataSet.getDoubleValueWithoutLimit(chart_elem.series[i].yValues[j]);

							index ++;
							posx[index] = x_axis.valueToJava2D(pointerX, rect, plot
								.getDomainAxisEdge());
							posy[index] = y_axis.valueToJava2D(pointerY, rect, plot
								.getRangeAxisEdge());
					}
				}
			}

/*			double pointerX = DataSet.getDoubleValueWithoutLimit(chart_elem.series[series].xValues[point]);
			double pointerY = DataSet.getDoubleValueWithoutLimit(chart_elem.series[series].yValues[point]);
			double px = x_axis.valueToJava2D(pointerX, new Rectangle2D.Double(0, 0,
					chart_elem.imageWidth, chart_elem.imageHeight), plot
					.getDomainAxisEdge());
			double py = y_axis.valueToJava2D(pointerY, new Rectangle2D.Double(0, 0,
					chart_elem.imageWidth, chart_elem.imageHeight), plot
					.getRangeAxisEdge());
*/

			double wei[] = new double[4];
			for (int i=0; i<posx.length; i++)
			{
				double dx = posx0 - posx[i];
				double dy = posy0 - posy[i];
				double r = Math.sqrt(dx * dx + dy * dy);
				if (r != 0.0 && r < 150) {
					double angle = jparsec.ephem.Functions.normalizeDegrees(
							Math.atan2(dy, dx) * jparsec.math.Constant.RAD_TO_DEG);
					int d = 2;
					if (Math.abs(angle-90.0) < 45.0) d = 1;
					if (Math.abs(angle-180.0) < 45.0) d = 0;
					if (Math.abs(angle-270.0) < 45.0) d = 3;
					wei[d] += 1.0/(r*r);
				}
			}
			int min = DataSet.getIndexOfMinimum(wei);
			if (min == 2 && posx0 < chart_elem.imageWidth*0.2) {
				wei = new double[] {wei[0], wei[1], wei[3]};
				min = DataSet.getIndexOfMinimum(wei);
			}
			if (min == 0 && posx0 > chart_elem.imageWidth*0.8) {
				wei = new double[] {wei[1], wei[2], wei[3]};
				min = DataSet.getIndexOfMinimum(wei);
			}
			if (min == 1 && posy0 < chart_elem.imageHeight*0.2) {
				wei = new double[] {wei[0], wei[2], wei[3]};
				min = DataSet.getIndexOfMinimum(wei);
			}
			if (min == 3 && posy0 > chart_elem.imageHeight*0.8) {
				wei = new double[] {wei[0], wei[1], wei[2]};
				min = DataSet.getIndexOfMinimum(wei);
			}
			switch (min)
			{
			case 0:
				dir = ChartSeriesElement.POINTER_ANGLE.RIGHTWARDS;
				break;
			case 1:
				dir = ChartSeriesElement.POINTER_ANGLE.UPWARDS;
				break;
			case 2:
				dir = ChartSeriesElement.POINTER_ANGLE.LEFTWARDS;
				break;
			case 3:
				dir = ChartSeriesElement.POINTER_ANGLE.DOWNWARDS;
				break;
			}

		} catch (Exception exc) {
		}

		return dir;
	}

	/**
	 * @param chart the chart to set
	 */
	private void setChart(JFreeChart chart) {
		this.chart = chart;
	}


	/**
	 * Returns the JFreeChart object.
	 * @return The chart.
	 */
	public JFreeChart getChart() {
		return chart;
	}

	/**
	 * Removes the border of the leyend. Should be the last call
	 * in case text font is also modified.
	 */
	public void removeLeyendBorder() {
		chart.getLegend().setFrame(BlockBorder.NONE);
	}

	/**
	 * Increases the sizes of the fonts in the chart.
	 * @param n The amount of pixel units.
	 * @throws JPARSECException If an error occurs.
	 */
	public void increaseFontSize(int n) throws JPARSECException {
		increaseFontSize += n;
		JFreeChart chart = createChart(chart_elem);
		this.setChart(chart);
		increaseFontSize -= n;
	}

	/**
	 * Increases the sizes of the fonts in the chart.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void increaseFontSize() throws JPARSECException {
		increaseFontSize ++;
	}
	/**
	 * Decreases the sizes of the fonts in the chart.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void decreaseFontSize() throws JPARSECException {
		increaseFontSize --;
	}

	/**
	 * Transforms mouse position in a chart to physical units.
	 * @param panel The ChartPanel object.
	 * @param p Mouse position in the ChartPanel component.
	 * @return Physical position, or null if it is not an x-y chart.
	 */
	public static double[] getPhysicalUnits(ChartPanel panel, Point p)
	{
		XYPlot plot = (XYPlot) panel.getChart().getPlot();
		double x = plot.getDomainAxis().java2DToValue(panel.translateScreenToJava2D(p).getX(),panel.getChartRenderingInfo().getPlotInfo().getDataArea(),plot.getDomainAxisEdge());
		double y = plot.getRangeAxis().java2DToValue(panel.translateScreenToJava2D(p).getY(),panel.getChartRenderingInfo().getPlotInfo().getDataArea(),plot.getRangeAxisEdge());
		return new double[] {x, y};
	}

	private Rectangle2D r = null;
	/**
	 * Transforms mouse position in a chart to physical units.
	 * @param px Mouse X position in an image exported from this instance.
	 * @param py Mouse Y position in an image exported from this instance.
	 * @return Physical position, or null if it is not an x-y chart.
	 */
	public double[] getPhysicalUnits(double px, double py)
	{
		if (!(getChart().getPlot() instanceof XYPlot)) return null;
		XYPlot plot = (XYPlot) getChart().getPlot();

		if (r == null) {
			BufferedImage buf = this.chartAsBufferedImage();
			int w = buf.getWidth(), h = buf.getHeight();
			int bx = -1;
			for (int i=w-1; i>w/2; i--) {
				int c[] = Functions.getColorComponents(buf.getRGB(i, h/2));
				if (c[0] != 255 || c[1] != 255 || c[2] != 255 || c[3] != 255) {
					bx = i;
					break;
				}
			}
			if (bx < w-1 && bx > w/2+1) {
				int c = buf.getRGB(bx, h/2);
				int maxX = bx, minY = -1, maxY = -1, minX = -1;
				for (int j=h/2; j>=0; j--) {
					int c2 = buf.getRGB(bx, j);
					if (c2 != c) {
						minY = j + 1;
						break;
					}
				}
				for (int j=h/2; j<=h; j++) {
					int c2 = buf.getRGB(bx, j);
					if (c2 != c) {
						maxY = j - 1;
						break;
					}
				}
				for (int i=bx; i>=0; i--) {
					int c2 = buf.getRGB(i, maxY);
					if (c2 != c) {
						minX = i + 1;
						break;
					}
				}
				r = new Rectangle2D.Double(minX+1, minY+1, maxX - minX - 1, maxY - minY - 1);
			} else {
				r = new Rectangle2D.Double(0, 0, getChartElement().imageWidth, getChartElement().imageHeight);
			}
		}
		double x = plot.getDomainAxis().java2DToValue(px, r, plot.getDomainAxisEdge());
		double y = plot.getRangeAxis().java2DToValue(py, r, plot.getRangeAxisEdge());
		return new double[] {x, y};
	}
	/**
	 * Transforms physical units to mouse position.
	 * @param px Physical x position.
	 * @param py Physical y position.
	 * @return Mouse position x and y, or null if it is not an x-y chart.
	 */
	public double[] getJava2DUnits(double px, double py)
	{
		if (!(getChart().getPlot() instanceof XYPlot)) return null;
		XYPlot plot = (XYPlot) getChart().getPlot();

		if (r == null) {
			BufferedImage buf = this.chartAsBufferedImage();
			int w = buf.getWidth(), h = buf.getHeight();
			int bx = -1;
			for (int i=w-1; i>w/2; i--) {
				int c[] = Functions.getColorComponents(buf.getRGB(i, h/2));
				if (c[0] != 255 || c[1] != 255 || c[2] != 255 || c[3] != 255) {
					bx = i;
					break;
				}
			}
			if (bx < w-1 && bx > w/2+1) {
				int c = buf.getRGB(bx, h/2);
				int maxX = bx, minY = -1, maxY = -1, minX = -1;
				for (int j=h/2; j>=0; j--) {
					int c2 = buf.getRGB(bx, j);
					if (c2 != c) {
						minY = j + 1;
						break;
					}
				}
				for (int j=h/2; j<=h; j++) {
					int c2 = buf.getRGB(bx, j);
					if (c2 != c) {
						maxY = j - 1;
						break;
					}
				}
				for (int i=bx; i>=0; i--) {
					int c2 = buf.getRGB(i, maxY);
					if (c2 != c) {
						minX = i + 1;
						break;
					}
				}
				r = new Rectangle2D.Double(minX+1, minY+1, maxX - minX - 1, maxY - minY - 1);
			} else {
				r = new Rectangle2D.Double(0, 0, getChartElement().imageWidth, getChartElement().imageHeight);
			}
		}
		double x = plot.getDomainAxis().valueToJava2D(px, r, plot.getDomainAxisEdge());
		double y = plot.getRangeAxis().valueToJava2D(py, r, plot.getRangeAxisEdge());
		return new double[] {x, y};
	}

	/**
	 * Sets the chart limits in x axis. The chart must be of type XY.
	 * @param min Minimum X.
	 * @param max Maximum X.
	 */
	public void setChartLimitsX(double min, double max) {
		XYPlot plot = (XYPlot) chart.getPlot();
		ValueAxis x_axis = plot.getDomainAxis();
		//x_axis.setAutoRange(false);
		x_axis.setLowerBound(min);
		x_axis.setUpperBound(max);
	}

	/**
	 * Sets the chart limits in y axis. The chart must be of type XY.
	 * @param min Minimum Y.
	 * @param max Maximum Y.
	 */
	public void setChartLimitsY(double min, double max) {
		XYPlot plot = (XYPlot) chart.getPlot();
		ValueAxis y_axis = plot.getRangeAxis();
		//y_axis.setAutoRange(false);
		y_axis.setLowerBound(min);
		y_axis.setUpperBound(max);
	}

	/**
	 * Returns the chart limits in x axis. The chart must be of type XY.
	 * @return Minimum and maximum x values in the chart area.
	 */
	public double[] getChartLimitsX() {
		XYPlot plot = (XYPlot) chart.getPlot();
		ValueAxis x_axis = plot.getDomainAxis();
		//x_axis.setAutoRange(false);
		return new double[] {
			x_axis.getLowerBound(), x_axis.getUpperBound()
		};
	}

	/**
	 * Returns the chart limits in y axis. The chart must be of type XY.
	 * @return Minimum and maximum y values in the chart area.
	 */
	public double[] getChartLimitsY() {
		XYPlot plot = (XYPlot) chart.getPlot();
		ValueAxis y_axis = plot.getRangeAxis();
		//y_axis.setAutoRange(false);
		return new double[] {
			y_axis.getLowerBound(), y_axis.getUpperBound()
		};
	}
	
	/**
	 * 'Prepares' a Graphics context so that it can be used to draw on a JFreeChart using
	 * physical and not Java coordinates.
	 * @param g The Graphic instance corresponding to an image created using this class.
	 * @param clip True to clip drawing operations inside the chart area.
	 */
	public void prepareGraphics2D(Graphics2D g, boolean clip) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        if (r == null) getPhysicalUnits(0, 0);
        if (clip) g.setClip((int)r.getMinX(), (int)r.getMinY(), (int)r.getWidth(), (int)r.getHeight());
        double p1[] = getJava2DUnits(0, 0);
        double p2[] = getJava2DUnits(10, 10);
        double sx = (p2[0] - p1[0]) / 10.0;
        double sy = (p2[1] - p1[1]) / 10.0;
        g.translate(p1[0], p1[1]);
        g.scale(sx, sy);
	}

	/**
	 * Creates a basic contour chart in JFreeChart as a workaround for being
	 * impossible to generate png images from SGT library without showing a JFrame.
	 * Note everything done here is experimental and deprecated in JFreeChart.
	 * @param chartElem The chart object.
	 * @throws JPARSECException If an error occurs.
	 */
	public CreateChart(GridChartElement chartElem) throws JPARSECException {
		NumberAxis xAxis = new NumberAxis(chartElem.xLabel);
		NumberAxis yAxis = new NumberAxis(chartElem.yLabel);
		xAxis.setUpperMargin(0.0);
		yAxis.setUpperMargin(0.0);
		ColorBar zColorBar = new ColorBar(chartElem.legend);

	      // Converts the float [][] grid to 3 x Double[]
	      int size =  chartElem.data.length;
	      Double [] oDoubleX = new Double[size*size];
	      Double [] oDoubleY = new Double[size*size];
	      Double [] oDoubleZ = new Double[size*size];
	      int index=0;
	      double x_step = (chartElem.limits[1] - chartElem.limits[0]) / (size - 1.0);
	      double y_step = (chartElem.limits[3] - chartElem.limits[2]) / (size - 1.0);
	      for (int i=0;i<=size-1;i++)
	      {
	         for (int j=0;j<=size-1;j++)
	         {
	            oDoubleX[index]=new Double(chartElem.limits[0] + x_step*i);
	            oDoubleY[index]=new Double(chartElem.limits[2] + y_step*j);
	            oDoubleZ[index]=new Double(chartElem.data[i][j]);
	            index++;
	         }
	      }
	      // then sets up and returns ContourDataSet
	      ContourDataset cds = new DefaultContourDataset("Contouring", oDoubleX, oDoubleY, oDoubleZ);


		ContourPlot cplot = new ContourPlot(cds, xAxis, yAxis, zColorBar);

		JFreeChart chart = new JFreeChart(chartElem.title, null, cplot, false);
		// then customise it a little...
		chart.setBackgroundPaint(Color.WHITE);

		this.chart_elem = new ChartElement(new ChartSeriesElement[] {}, ChartElement.TYPE.XY_CHART, ChartElement.SUBTYPE.XY_SCATTER, chartElem.title, chartElem.xLabel, chartElem.yLabel, false, chartElem.imageWidth, chartElem.imageWidth*2/3);
		this.setChart(chart);
	}
}
