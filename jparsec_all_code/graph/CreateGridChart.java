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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Date;

//import javax.vecmath.Point3d;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import gov.noaa.pmel.sgt.Axis;
import gov.noaa.pmel.sgt.CartesianGraph;
import gov.noaa.pmel.sgt.CartesianRenderer;
import gov.noaa.pmel.sgt.ColorMap;
import gov.noaa.pmel.sgt.ContourLevels;
import gov.noaa.pmel.sgt.DefaultContourLineAttribute;
import gov.noaa.pmel.sgt.GridAttribute;
import gov.noaa.pmel.sgt.GridCartesianRenderer;
import gov.noaa.pmel.sgt.IndexedColorMap;
import gov.noaa.pmel.sgt.JPane;
import gov.noaa.pmel.sgt.Layer;
import gov.noaa.pmel.sgt.LinearTransform;
import gov.noaa.pmel.sgt.dm.SGTData;
import gov.noaa.pmel.sgt.dm.SGTMetaData;
import gov.noaa.pmel.sgt.dm.SimpleGrid;
import gov.noaa.pmel.sgt.swing.JClassTree;
import gov.noaa.pmel.sgt.swing.JPlotLayout;
import gov.noaa.pmel.sgt.swing.prop.GridAttributeDialog;
import gov.noaa.pmel.util.Dimension2D;
import gov.noaa.pmel.util.Domain;
import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.SoTDomain;
import gov.noaa.pmel.util.SoTRange;
import jparsec.graph.chartRendering.AWTGraphics;
import jparsec.io.FileIO;
import jparsec.io.WriteFile;
import jparsec.io.image.ImageSplineTransform;
import jparsec.io.image.Picture;
import jparsec.util.JPARSECException;
import jparsec.util.Logger;
import jparsec.util.Logger.LEVEL;
import jparsec.util.Translate;

/**
 * Creates grid charts (x-y maps with the variation of a third
 * variable in color levels). This class uses the NOAA Scientific
 * Graphic Toolkit called SGT. The array of data should be square and
 * fully populated of data to properly create the chart.<P>
 * The SGT version used in JPARSEC is a modified version of the original.
 * An error when drawing color key in vertical orientation is fixed.
 * Also support was added to superscripts, subscripts, Greek characters,
 * and text labels or pointers.
 * <P>The chart can be zoomed/resized with the mouse. The different elements in
 * the chart can be selected and edited interactively.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class CreateGridChart implements Serializable, ComponentListener, PropertyChangeListener, MouseWheelListener, MouseListener
{
	private static final long serialVersionUID = 1L;

	private JFrame frame;
	  private JPlotLayout rpl_;
	  private GridAttribute gridAttr_;
	  private JButton edit_;
	  private JButton space_ = null;
	  private JButton tree_;
	  private JButton export;
	  private GridChartElement gridChart;
	  private boolean showButtons;
	  private int width = -1, height = -1;
	  private JPanel main = new JPanel(), button = new JPanel();
	  private ColorMap cmap;
	  private int[] limits = null;

		private void writeObject(ObjectOutputStream out)
		throws IOException {
			out.writeObject(this.gridChart);
		}
		private void readObject(ObjectInputStream in)
		throws IOException, ClassNotFoundException {
			this.gridChart = (GridChartElement) in.readObject();
			if (!GraphicsEnvironment.isHeadless()) {
				CreateGridChart c = new CreateGridChart(this.gridChart);
				this.button = c.button;
				this.cmap = c.cmap;
				this.showButtons = c.showButtons;
				this.width = c.width;
				this.height = c.height;
				this.main = c.main;
				this.tree_ = c.tree_;
				this.export = c.export;
				this.rpl_ = c.rpl_;
				this.gridAttr_ = c.gridAttr_;
				this.edit_ = c.edit_;
				this.space_ = c.space_;
				this.limits = c.limits;
			}
	 	}

	  /**
	   * Constructor of a grid chart.
	   * @param chart The chart.
	   */
	  public CreateGridChart(GridChartElement chart)
	  {
		  frame = new JFrame(chart.title);
		  gridChart = chart.clone();
		  rpl_ = new JPlotLayout(true, false, false, "test layout", null, chart.ocultLevels);
		  init();
	  }

	  private void init()
	  {
		  if (!gridChart.invertXaxis) {
			  if (gridChart.limits[0] > gridChart.limits[1])  gridChart.invertXaxis = true;
		  }
		  if (!gridChart.invertYaxis) {
			  if (gridChart.limits[2] > gridChart.limits[3])  gridChart.invertYaxis = true;
		  }

		  makeGraph(this.gridChart);
	  }

	  /**
	   * Shows the chart in a default SGT panel.
	   * @param showButtons True to show the buttons.
	   * @throws JPARSECException If an error occurs.
	   */
	  public void showChartInSGTpanel(boolean showButtons)
	  throws JPARSECException {
		  showChartInSGTpanel(showButtons, true);
	  }

	  /**
	   * Shows the chart in a default SGT panel.
	   * @param showButtons True to show the buttons.
	   * @param showJFrame True to show the JFrame.
	   * @throws JPARSECException If an error occurs.
	   */
	  public void showChartInSGTpanel(boolean showButtons, boolean showJFrame)
	  throws JPARSECException {
		  main.removeAll();
		  button.removeAll();
		  this.showButtons = showButtons;
		  frame.getContentPane().setLayout(new BorderLayout(1, 2));
		  frame.setBackground(Color.WHITE);
		  frame.setSize(gridChart.imageWidth, gridChart.imageHeight);
		  makeButtonPanel();
		  rpl_.setBatch(true);
		  main.add(rpl_, BorderLayout.NORTH);
		  frame.setTitle(TextLabel.getSimplifiedString(gridChart.title));

		  frame.add(main, BorderLayout.NORTH);
		  if (showButtons) frame.add(button, BorderLayout.SOUTH);
		  rpl_.setBatch(false);
		  frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		  frame.addComponentListener(this);
		  rpl_.addPropertyChangeListener(this);
		  rpl_.addMouseWheelListener(this);
		  rpl_.addMouseListener(this);
		  frame.pack();
		  frame.setVisible(showJFrame);

		  width = frame.getWidth();
		  height = frame.getHeight();
		  drawPointers();
		  frame.setIconImage(this.chartAsBufferedImage());
	  }

	  /**
	   * Returns the JPanel.
	   * @param showButtons True to show the buttons.
	   * @return The JPanel.
	   * @throws JPARSECException If an error occurs.
	   */
	  public JPanel getComponent(boolean showButtons)
	  throws JPARSECException {
		  main.removeAll();
		  button.removeAll();
		  this.showButtons = showButtons;
		  makeButtonPanel();
		  rpl_.setBatch(true);
		  main.add(rpl_, BorderLayout.NORTH);
		  JPanel frame = new JPanel();
		  frame.add(main, BorderLayout.NORTH);
		  if (showButtons) frame.add(button, BorderLayout.SOUTH);

		  main.setBackground(rpl_.getBackground());
		  frame.setBackground(rpl_.getBackground());

		  rpl_.addPropertyChangeListener(this);
		  rpl_.addMouseWheelListener(this);
		  rpl_.addMouseListener(this);
		  rpl_.setBatch(false);
		  width = frame.getWidth();
		  height = frame.getHeight();

		  frame.addComponentListener(new ComponentAdapter() {
  		    public void componentResized(ComponentEvent evt) {
  		    	update();
  		    }
  			});
		  return frame;
	  }

	  /**
	   * Updates the chart.
	   */
	  public void update() {
		  t = null;
		  Dimension newSize = new Dimension(gridChart.imageWidth, gridChart.imageHeight);
		  if (main.getParent() != null) {
			  newSize = main.getParent().getSize();
			  gridChart.imageWidth = newSize.width;
			  if (newSize.width > 500) gridChart.imageWidth = newSize.width-newSize.width/10;
			  gridChart.imageHeight = newSize.height+80; //gridChart.imageWidth;
			  if (gridChart.imageHeight > gridChart.imageWidth) gridChart.imageHeight = gridChart.imageWidth;
		  }
		  rpl_.clear();
		  init();
		  if (main.getParent() != null) {
			  main.getParent().validate();
		  } else {
			  main.repaint();
		  }
		  drawPointers();
	  }

		/**
		 * Returns the chart as an image.
		 * @return The image.
		 * @throws JPARSECException If an error occurs.
		 */
		public BufferedImage chartAsBufferedImage()
		throws JPARSECException{
			boolean isShown = frame.isVisible();

			if (!isShown) this.showChartInSGTpanel(false);
		    BufferedImage buf = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
			Graphics g = buf.createGraphics();
			g.fillRect(0, 0, buf.getWidth(), buf.getHeight());
			try {
				if (this.rpl_.isEnabled())
					this.rpl_.draw(g, buf.getWidth(), buf.getHeight());
			} catch (Exception exc)
			{
 				Logger.log(LEVEL.ERROR, "Error drawing the chart to an image. Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
			}
			drawPointers(buf);
			if (!isShown) frame.dispose();
			return buf;
		}

		/**
		 * Exports the chart as an EPS file.
		 *
		 * @param file_name File name.
		 * @throws JPARSECException If an error occurs.
		 */
		public void chartAsEPSFile(String file_name) throws JPARSECException
		{
			int ext = file_name.toLowerCase().lastIndexOf(".eps");
			if (ext > 0) file_name = file_name.substring(0, ext);

			File plotFile = new File(file_name + ".eps");
			int size_x = gridChart.imageWidth, size_y = gridChart.imageWidth - 100;
			final Dimension size = new Dimension(size_x, size_y);

			boolean isShown = frame.isVisible();
			if (!isShown) this.showChartInSGTpanel(false);
			try
			{
				// Using reflection so that everything will work without freehep in classpath
				Class c = Class.forName("org.freehep.graphicsio.ps.PSGraphics2D");
				Constructor cc = c.getConstructor(new Class[] {plotFile.getClass(), size.getClass()});
				Object psGraphics = cc.newInstance(new Object[] {plotFile, size});
				Method m = c.getMethod("startExport");
				m.invoke(psGraphics);

				try {
					if (this.rpl_.isEnabled())
						this.rpl_.draw((Graphics2D) (psGraphics), gridChart.imageWidth, gridChart.imageWidth-100);
				} catch (Exception exc)
				{
	 				Logger.log(LEVEL.ERROR, "Error drawing the chart to an image. Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
				}

				Method mm = c.getMethod("endExport");
				mm.invoke(psGraphics);

				if (!isShown) frame.dispose();
			} catch (Exception e)
			{
				throw new JPARSECException("cannot write to file.", e);
			}
		}

		/**
		 * Exports the chart as an PDF file.
		 *
		 * @param file_name File name.
		 * @throws JPARSECException If an error occurs.
		 */
		public void chartAsPDFFile(String file_name) throws JPARSECException
		{
			int ext = file_name.toLowerCase().lastIndexOf(".pdf");
			if (ext > 0) file_name = file_name.substring(0, ext);

			File plotFile = new File(file_name + ".pdf");
			int size_x = gridChart.imageWidth, size_y = gridChart.imageWidth - 100;
			final Dimension size = new Dimension(size_x, size_y);

			boolean isShown = frame.isVisible();
			if (!isShown) this.showChartInSGTpanel(false);
			try
			{
				// Using reflection so that everything will work without freehep in classpath
				Class c = Class.forName("org.freehep.graphicsio.pdf.PDFGraphics2D");
				Constructor cc = c.getConstructor(new Class[] {plotFile.getClass(), size.getClass()});
				Object pdfGraphics = cc.newInstance(new Object[] {plotFile, size});
				Method m = c.getMethod("startExport");
				m.invoke(pdfGraphics);

				try {
					if (this.rpl_.isEnabled())
						this.rpl_.draw((Graphics2D) (pdfGraphics), gridChart.imageWidth, gridChart.imageWidth-100);
				} catch (Exception exc)
				{
	 				Logger.log(LEVEL.ERROR, "Error drawing the chart to an image. Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
				}

				Method mm = c.getMethod("endExport");
				mm.invoke(pdfGraphics);

				if (!isShown) frame.dispose();
			} catch (Exception e)
			{
				throw new JPARSECException("cannot write to file.", e);
			}
		}

		/**
		 * Exports the chart as an SVG file.
		 *
		 * @param file_name File name.
		 * @throws JPARSECException If an error occurs.
		 */
		public void chartAsSVGFile(String file_name) throws JPARSECException
		{
			int ext = file_name.toLowerCase().lastIndexOf(".svg");
			if (ext > 0) file_name = file_name.substring(0, ext);

			File plotFile = new File(file_name + ".svg");
			int size_x = gridChart.imageWidth, size_y = gridChart.imageWidth - 100;
			final Dimension size = new Dimension(size_x, size_y);

			boolean isShown = frame.isVisible();
			if (!isShown) this.showChartInSGTpanel(false);
			try
			{
				// Using reflection so that everything will work without freehep in classpath
				Class c = Class.forName("org.freehep.graphicsio.svg.SVGGraphics2D");
				Constructor cc = c.getConstructor(new Class[] {plotFile.getClass(), size.getClass()});
				Object svgGraphics = cc.newInstance(new Object[] {plotFile, size});
				Method m = c.getMethod("startExport");
				m.invoke(svgGraphics);

				try {
					if (this.rpl_.isEnabled())
						this.rpl_.draw((Graphics2D) (svgGraphics), gridChart.imageWidth, gridChart.imageWidth-100);
				} catch (Exception exc)
				{
	 				Logger.log(LEVEL.ERROR, "Error drawing the chart to an image. Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
				}

				Method mm = c.getMethod("endExport");
				mm.invoke(svgGraphics);

				if (!isShown) frame.dispose();

			} catch (Exception e)
			{
				throw new JPARSECException("cannot write to file.", e);
			}
		}

		/**
		 * Returns the chart object.
		 * @return Grid chart object.
		 */
		public GridChartElement getChartElement()
		{
			return this.gridChart;
		}

		private void makeGraph(GridChartElement chart) {
		    /*
		     * This example uses a pre-created "Layout" for raster time
		     * series to simplify the construction of a plot. The
		     * JPlotLayout can plot a single grid with
		     * a ColorKey, time series with a LineKey, point collection with a
		     * PointCollectionKey, and general X-Y plots with a
		     * LineKey. JPlotLayout supports zooming, object selection, and
		     * object editing.
		     */
		    SGTData newData;
		    ContourLevels clevels;
		    /*
		     * Create a test grid with sinusoidal-ramp data.
		     */
		    Range2D xr = new Range2D(chart.limits[0], chart.limits[1], 1f);
		    Range2D yr = new Range2D(chart.limits[2], chart.limits[3], 1f);
		    newData = this.getSGTData(xr, yr, chart);

		    newData.getXMetaData().setName(chart.xLabel);
		    newData.getYMetaData().setName(chart.yLabel);
		    newData.getXMetaData().setUnits(null);
		    newData.getYMetaData().setUnits(null);

		    /*
		     * Create the layout without a Logo image and with the
		     * ColorKey on a separate Pane object.
		     */
		    rpl_.setEditClasses(true);
		    /*
		     * Create a GridAttribute for CONTOUR style.
		     */
		    double max = chart.getMaximum(), min = chart.getMinimum();
		    Range2D datar = new Range2D(min, max, (max-min)/(double)chart.colorModelResolution);
		    cmap = createColorMap(datar, chart);
		    if (chart.levels != null) {
			    clevels = ContourLevels.getDefault(chart.levels);
			    gridAttr_ = new GridAttribute(clevels);
			    gridAttr_.setColorMap(cmap);
			    gridAttr_.setStyle(chart.type.ordinal());

			    try {
			    	DefaultContourLineAttribute dcla = gridAttr_.getContourLevels().getDefaultContourLineAttribute();
			    	dcla.setSignificantDigits(chart.significantDigits);
			    	dcla.setLabelEnabled(!chart.ocultLevelLabels);
			    	gridAttr_.getContourLevels().setDefaultContourLineAttribute(dcla);
			    } catch (Exception exc) {}

		    } else {
			    gridAttr_ = new GridAttribute(ContourLevels.getDefault(new double[] {}));
			    gridAttr_.setColorMap(cmap);
			    gridAttr_.setStyle(chart.type.ordinal());
		    }
		    /*
		     * Add the grid to the layout and give a label for
		     * the ColorKey.
		     */
		    String l = chart.legend;
		    if (l == null) l = "";
		    rpl_.addData(newData, gridAttr_, l);

		    /*
		     * Change the layout's three title lines.
		     */
		    String l1 = chart.title;
		    if (l1 == null) l1 = "";
		    String l2 = chart.subTitle;
		    if (l2 == null) l2 = "";
		    rpl_.setTitles(l1, l2, "");
		    rpl_.setOpaque(false);
		    rpl_.setClipping(true);
		    /*
		     * Resize the graph  and place in the "Center" of the frame.
		     */
		    rpl_.setSize(new Dimension(chart.imageWidth, chart.imageHeight));
		    rpl_.setPageScaleMode(JPane.SHRINK_TO_FIT);

		    /*
		     * Resize the key Pane, both the device size and the physical
		     * size. Set the size of the key in physical units and place
		     * the key pane at the "South" of the frame.
		     */
		    rpl_.setKeyBorderStyle(chart.levelsBorderStyle.ordinal());
		    if (chart.levelsOrientation == GridChartElement.WEDGE_ORIENTATION.HORIZONTAL_BOTTOM) {
			    rpl_.setSize(new Dimension((chart.imageWidth+chart.imageWidth/10), (chart.imageHeight)));
			    Dimension2D d = rpl_.getLayerSizeP();
			    rpl_.setAxesOriginP(new Point2D.Double(1, 1.5));
			    rpl_.setLayerSizeP(d);
		    	rpl_.setKeyBoundsP(new gov.noaa.pmel.util.Rectangle2D.Double(3.2, 0.05, 4.7, 0.9));
		    } else {
			    if (chart.levelsOrientation == GridChartElement.WEDGE_ORIENTATION.VERTICAL_RIGHT) {
				    rpl_.setSize(new Dimension(chart.imageWidth+chart.imageWidth/10, chart.imageHeight-100));
			    	rpl_.setKeyBoundsP(new gov.noaa.pmel.util.Rectangle2D.Double(6, 0.55, 0.9, 4.87));
			    	rpl_.setKeyOrientation(0); // 0 = | rms ; 1 = bottom
			    } else {
				    rpl_.setSize(new Dimension(chart.imageWidth+chart.imageWidth/10, chart.imageHeight-100));
			    	rpl_.setKeyBoundsP(new gov.noaa.pmel.util.Rectangle2D.Double(2.75, 5.0, 5.6, 0.9));
			    }
		    }
		}

		private void edit_actionPerformed(java.awt.event.ActionEvent e) {
		    /*
		     * Create a GridAttributeDialog and set the renderer.
		     */
		    GridAttributeDialog gad = new GridAttributeDialog();
		    gad.setJPane(rpl_);
		    CartesianRenderer rend = ((CartesianGraph)rpl_.getFirstLayer().getGraph()).getRenderer();
		    gad.setGridCartesianRenderer((GridCartesianRenderer)rend);
		    gad.setVisible(true);
		}

		private void tree_actionPerformed(java.awt.event.ActionEvent e) {
			/*
			 * Create a JClassTree for the JPlotLayout objects
			 */
			JClassTree ct = new JClassTree();
			ct.setModal(false);
			ct.setJPane(rpl_);
			ct.show();
		}

		private ColorMap createColorMap(Range2D datar, GridChartElement chart) {
		    IndexedColorMap cmap = new IndexedColorMap(chart.red, chart.green, chart.blue);
		    LinearTransform ctrans =
		        new LinearTransform(0.0, (double)chart.red.length, datar.start, datar.end);
		    ((IndexedColorMap)cmap).setTransform(ctrans);
		    return cmap;
		}

		private JPopupMenu result;
	    private JMenuItem treeViewItem, editGridItem, resetZoomItem, exportItem;
	    /**
	     * Creates a popup menu for the panel.
	     *
	     * @return The popup menu.
	     */
	    protected JPopupMenu createPopupMenu() {
		    MyAction myAction = new MyAction();
	        result = new JPopupMenu(Translate.translate(946)); //"Chart:");

            treeViewItem = new JMenuItem(Translate.translate(947)); //"Tree view");
            treeViewItem.addActionListener(myAction);
            result.add(treeViewItem);
            editGridItem = new JMenuItem(Translate.translate(948)); //"Edit grid");
            editGridItem.addActionListener(myAction);
            result.add(editGridItem);
            resetZoomItem = new JMenuItem(Translate.translate(949)); //"Reset zoom");
            resetZoomItem.addActionListener(myAction);
            result.add(resetZoomItem);
            exportItem = new JMenuItem(Translate.translate(950)); //"Export");
            exportItem.addActionListener(myAction);
            result.add(exportItem);

	        return result;
	    }

		private JPanel makeButtonPanel() {
			createPopupMenu();
		    button.setLayout(new FlowLayout());
		    tree_ = new JButton(Translate.translate(947)); //"Tree View");
		    MyAction myAction = new MyAction();
		    tree_.addActionListener(myAction);
		    button.add(tree_);
		    edit_ = new JButton(Translate.translate(948)); //"Edit GridAttribute");
		    edit_.addActionListener(myAction);
		    button.add(edit_);

		    space_ = new JButton(Translate.translate(949)); //"Reset Zoom");
		    space_.addActionListener(myAction);
		    button.add(space_);

		    export = new JButton(Translate.translate(950)); //"Export");
		    export.addActionListener(myAction);
		    button.add(export);

		    return button;
		}

		private SGTData getSGTData(Range2D range1, Range2D range2, GridChartElement chart)
		{
			    SimpleGrid sg;
			    SGTMetaData xMeta;
			    SGTMetaData yMeta;

			    double[] axis1, axis2;
			    double[] values;
			    int count;

			    int num1 = chart.data.length;
			    axis1 = new double[num1];
			    int num2 = chart.data[0].length;
			    axis2 = new double[num2];

			    range1.delta = (range1.end - range1.start) / (num1 - 1.0);
			    range2.delta = (range2.end - range2.start) / (num2 - 1.0);
			    for(count=0; count < num1; count++) {
			      axis1[count] = range1.start + count*range1.delta;
			    }
			    for(count=0; count < num2; count++) {
			      axis2[count] = range2.start + count*range2.delta;
			    }

			    values = new double[num1*num2];
			    count = 0;
			      for(int count1=0; count1 < num1; count1++) {
			          for(int count2=0; count2 < num2; count2++) {
			            values[count] = 0.0;
			            if (chart.data[count1][count2] != null) values[count] = chart.data[count1][count2].doubleValue();
			            count++;
			          }
			        }

			    xMeta = new SGTMetaData("x", "x", chart.invertXaxis, false);
			    yMeta = new SGTMetaData("y", "y", chart.invertYaxis, false);
			    sg = new SimpleGrid(values, axis1, axis2, "Test Series");
			    sg.setXMetaData(xMeta);
			    sg.setYMetaData(yMeta);
			    sg.setId("GRIDCHART");
			    return sg;
		}

		/**
		 *  This method is called after the component's size changes.
		 */
		public void componentResized(ComponentEvent evt) {
			// Get new size
			Dimension newSize = frame.getSize();
			if (frame.isValid() && frame.isActive() && frame.isVisible() && newSize.width != width && width > 0 && height > 0)
		    {
				gridChart.imageWidth = newSize.width;
				if (newSize.width > 500) {
					gridChart.imageWidth = newSize.width-newSize.width/10;
				}
				gridChart.imageHeight = newSize.height+80; //gridChart.imageWidth;
				if (gridChart.imageHeight > gridChart.imageWidth) gridChart.imageHeight = gridChart.imageWidth;
				makeGraph(gridChart);
		    }
		}
		/**
		 * This method is called after the component is moved.
		 */
		public void componentMoved(ComponentEvent evt) {}
		/**
		 * This method is called after the component is hidden.
		 */
		public void componentHidden(ComponentEvent evt) {}
		/**
		 * This method is called after the component is shown.
		 */
		public void componentShown(ComponentEvent evt) {}

		/**
		 * Returns the color for a given intensity.
		 * @param intensity The intensity.
		 * @return The color.
		 */
		public Color getColor(double intensity)
		{
			return cmap.getColor(intensity);
		}

		/**
		 * Creates a script to draw the current chart using GILDAS. A file
		 * with extension .greg is created.
		 * @param fileName Name of the postscript file to be created, with path.
		 * @param xlog True to create a chart with the x axis in log scale.
		 * @param ylog True to create a chart with the y axis in log scale.
		 * @throws JPARSECException If an error occurs.
		 * @return The contents of the script.
		 */
		public String exportAsScriptForGILDAS(String fileName, boolean xlog, boolean ylog)
		throws JPARSECException {
			double limits[] = this.getChartElement().limits;
			int ext = fileName.toLowerCase().lastIndexOf(".ps");
			if (ext < 0) ext = fileName.toLowerCase().lastIndexOf(".greg");
			if (ext > 0) fileName = fileName.substring(0, ext);

			String auxF = fileName+".jparsec";
			StringBuffer data = new StringBuffer(10*gridChart.data.length*gridChart.data[0].length);
			for (int i=0; i<gridChart.data.length; i++)
			{
				for (int j=0; j<gridChart.data[i].length; j++)
				{
					double fx = (double) i / (double) (gridChart.data.length - 1.0);
					double fy = (double) j / (double) (gridChart.data[i].length - 1.0);
					double x = limits[0] + (limits[1] - limits[0]) * fx;
					double y = limits[2] + (limits[3] - limits[2]) * fy;
					if (gridChart.data[i][j] != null)
						data.append(""+x+" "+y+ " "+gridChart.data[i][j].doubleValue() + FileIO.getLineSeparator());
				}
			}
			WriteFile.writeAnyExternalFile(auxF, data.toString());
			String sep = FileIO.getLineSeparator();
			StringBuffer script = new StringBuffer(1000);
			script.append("set font duplex"+sep);

			script.append( "define real px"+ sep);
			script.append( "define real py"+ sep);
			script.append( "define real mx"+ sep);
			script.append( "define real my"+ sep);
			script.append( "let mx = box_xmax-box_xmin"+ sep);
			script.append( "let my = box_ymax-box_ymin"+ sep);
			script.append( "let px mx*0.5"+ sep);
			script.append( "let py my*0.5"+ sep);
			script.append( "column x 1 y 2 z 3 /file "+FileIO.getFileNameFromPath(auxF)+sep);
			script.append( "limits "+limits[0]+" "+limits[1]+" "+limits[2]+" "+limits[3]);
			if (xlog)  script.append(" /XLOG");
			if (ylog)  script.append(" /YLOG");
			script.append(sep);
			script.append( "set blanking 0 0"+sep);
			script.append( "box"+sep);
			script.append( "RGDATA ! random "+gridChart.data.length+" "+gridChart.data.length+" /neighbours 4"+sep);
			script.append( "limits /rg"+sep);
			script.append( "lut white"+sep);
			script.append( "plot /scaling lin 0 rgmax"+sep);
			script.append( "wedge"+sep);
			script.append( "draw text px  -1.0 \""+CreateChart.toGILDASformat(this.getChartElement().xLabel)+"\""+sep);
			script.append( "draw text -1.8 py \""+CreateChart.toGILDASformat(this.getChartElement().yLabel)+"\" 5 90"+sep);
			String title = "Grid Chart Title";
			if (this.getChartElement().title != null && !this.getChartElement().title.equals("")) title = this.getChartElement().title;
			if (this.getChartElement().subTitle != null && !this.getChartElement().subTitle.equals("")) title += " - "+this.getChartElement().subTitle;
			if (this.getChartElement().legend != null && !this.getChartElement().legend.equals("")) title += " ("+this.getChartElement().legend+")";
			script.append( "draw text px 17.5 \""+CreateChart.toGILDASformat(title)+"\""+sep);
			script.append( "! DRAW TITLE"+sep);

			if (this.getChartElement().levels != null)
			{
				double levels[] = this.getChartElement().levels;
				for (int i=0; i<levels.length; i++)
				{
					script.append( "lev "+levels[i]+sep);
					script.append( "rgm"+sep);
				}
			}

			script.append( "sys \"rm "+FileIO.getFileNameFromPath(fileName)+".ps\""+sep);
			script.append( "hard "+FileIO.getFileNameFromPath(fileName)+".ps /dev ps grey"+sep);

			String s = script.toString();
			WriteFile.writeAnyExternalFile(fileName+".greg", s);
			return s;
		}

		/**
		 * Creates a panel with several charts for GILDAS.
		 * @param charts The charts.
		 * @param nx The number of chartx in the x axis.
		 * @param ny The number of charts in the y axis.
		 * @param names The names of the different output files for the charts.
		 * @param superName The name of the main GILDAS panel.
		 * @param dir Path to the output directory.
		 * @param drawTitles True to draw titles slightly inside the charts.
		 * @throws JPARSECException If an error occurs.
		 */
		public static void exportAsSuperScriptForGILDAS(CreateGridChart charts[], int nx, int ny,
				String names[], String superName, String dir, boolean drawTitles)
		throws JPARSECException {
			CreateGridChart.exportAsSuperScriptForGILDAS(charts, nx, ny, names, superName, dir, drawTitles, false, false, false, false);
		}

		/**
		 * Creates a panel with several charts for GILDAS.
		 * @param charts The charts.
		 * @param nx The number of chartx in the x axis.
		 * @param ny The number of charts in the y axis.
		 * @param names The names of the different output files for the charts.
		 * @param superName The name of the main GILDAS panel.
		 * @param dir Path to the output directory.
		 * @param drawTitles True to draw titles slightly inside the charts.
		 * @param drawPanelIndex True to draw panel label as a character or number.
		 * @param panelIndexAsNumber True to draw panel label as a character.
		 * @param xlog True to draw x axis in log scale.
		 * @param ylog True to draw y axis in log scale.
		 * @throws JPARSECException If an error occurs.
		 */
		public static void exportAsSuperScriptForGILDAS(CreateGridChart charts[], int nx, int ny,
				String names[], String superName, String dir, boolean drawTitles,
				boolean drawPanelIndex, boolean panelIndexAsNumber, boolean xlog, boolean ylog)
		throws JPARSECException {
			if (!dir.endsWith(FileIO.getFileSeparator())) dir += FileIO.getFileSeparator();
			double xmin = 4, xmax = 28, ymin = 2.5, ymax = 19.5;
			boolean xaxis[] = new boolean[names.length];
			boolean yaxis[] = new boolean[names.length];
			int maxN = nx;
			if (ny > nx) maxN = ny;

			String sep = FileIO.getLineSeparator();
			StringBuffer script = new StringBuffer(1000);
			script.append("! SCRIPT TO DRAW A CHART PANEL WITH GILDAS"+ sep);
			script.append("! AUTOMATICALLY GENERATED BY JPARSEC PACKAGE"+ sep);
			script.append("! ON " + (new Date().toString())+ sep);
			script.append(""+ sep);
			int index = -1;
			for (int y = 1; y<=ny; y++)
			{
				for (int x = 1; x<= nx; x++)
				{
					double bx0 = xmin + (double) (x - 1) * (double) (xmax - xmin) / (double) maxN;
					double bxf = bx0 + (double) (xmax - xmin) / (double) maxN;

					double byf = (ymin + (double) (y - 1) * (double) (ymax - ymin) / (double) maxN);
					double by0 = (byf + (double) (ymax - ymin) / (double) maxN);

					by0 = ymax + ymin - by0;
					byf = ymax + ymin - byf;

					index ++;
					if (index < charts.length)
					{
						script.append("set box "+bx0+" "+bxf+" "+by0+" "+byf + sep);
						script.append("@"+names[index]+".greg" + sep + sep);

						xaxis[index] = yaxis[index] = true;
						if (x > 1) yaxis[index] = false;
						if (y < ny) xaxis[index] = false;
					}
				}
			}

			script.append("sys \"rm "+superName+".ps\""+ sep);
			script.append("hard "+superName+".ps /dev ps color"+sep);
/*			if (autoDestroy)
			{
				script.append(""+ sep);
				//script.append("PAUSE Continue to remove all unnecesary files. Otherwise quit/exit here."+ sep);
				script.append(""+ sep);
				script.append("! REMOVE ALL UNNECESARY FILES"+ sep);
				script.append("sys \"rm "+superName+".greg\""+ sep);
			}
*/			//script.append("exit"+sep);

			WriteFile.writeAnyExternalFile(dir+superName+".greg", script.toString());

			for (int i=0; i<charts.length; i++)
			{
				String fileName = names[i];
				String gscript = charts[i].exportAsScriptForGILDAS(dir+fileName, xlog, ylog);
				gscript = DataSet.replaceOne(gscript, "wedge", "!wedge", 1);
				if (i > 0) gscript = DataSet.replaceOne(gscript, "plot /scaling lin", "plot /scaling lin LCUT HCUT !", 1);
				if (i > 0) gscript = DataSet.replaceOne(gscript, "lut white", "!lut white", 1);

				if (!drawTitles)
				{
					gscript = DataSet.replaceOne(gscript, "draw text", "!draw text", 3);
				}

				if (drawPanelIndex)
				{
					String panelIndex = ""+(i+1);
					if (!panelIndexAsNumber) {
						try {
							panelIndex = "abcdefghijklmnopqrstuvwxyz".substring(i, i+1);
						} catch (Exception exc) {}
					}
					gscript = DataSet.replaceOne(gscript, "! DRAW TITLE",
							"let mx = user_xmax-user_xmin"+ sep+
							"let my = user_ymax-user_ymin"+ sep+
							"let py user_ymin+my*0.95"+sep+
							"let px user_xmin+mx*0.05"+sep+
							"draw text px py \""+panelIndex+")\" 5 0 /user"+sep+"! DRAW TITLE", 1);
				}

				String box = "box ";
				if (!xaxis[i]) {
					box += "N ";
					gscript = DataSet.replaceOne(gscript, "draw text", "!draw text", 1);
				} else {
					box += "P ";
				}
				if (!yaxis[i]) {
					box += "N ";
					gscript = DataSet.replaceOne(gscript, "draw text", "!draw text", 2);
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

		private ImageSplineTransform t = null;

		/**
		 * Returns the intensity at certain position using 2d spline interpolation.
		 * Note the returned value will not agree
		 * exactly with the contour lines generated by SGT, but the difference
		 * should be always small and inside the level of uncertainty expected.
		 * @param x X position in physical units.
		 * @param y Y position in physical units.
		 * @return The intensity, or 0 if the input point is outside the image.
		 */
		public double getIntensityAt(double x, double y)
		{
			if (t == null) t = new ImageSplineTransform(GridChartElement.ObjectToDoubleArray(gridChart.data, 0.0));
			int pointsX = this.gridChart.data.length;
			int pointsY = this.gridChart.data[0].length;
			double px = (x - this.gridChart.limits[0]) * ((double) (pointsX - 1.0)) / (this.gridChart.limits[1] - this.gridChart.limits[0]);
			double py = (y - this.gridChart.limits[2]) * ((double) (pointsY - 1.0)) / (this.gridChart.limits[3] - this.gridChart.limits[2]);
			double data = 0.0;
			try {
				if (!t.isOutOfImage(px, py)) data = t.interpolate(px, py);
			} catch (Exception exc) {
			}

			return data;
		}

		/**
		 * Returns the intensity at certain position using 2d spline interpolation.
		 * Note the returned value will not agree
		 * exactly with the contour lines generated by SGT, but the difference
		 * should be always small and inside the level of uncertainty expected.
		 * @param x X position in index units, from 0 to width-1.
		 * @param y Y position in index units, from 0 to height-1.
		 * @return The intensity, or 0 if the input point is outside the image.
		 */
		public double getIntensityAtUsingIndexPosition(double x, double y)
		{
			if (t == null) t = new ImageSplineTransform(GridChartElement.ObjectToDoubleArray(gridChart.data, 0.0));
			double data = 0.0;
			try {
				if (!t.isOutOfImage(x, y)) data = t.interpolate(x, y);
			} catch (Exception exc) {
			}

			return data;
		}

		/**
		 * Returns the display component of the instance.
		 * @return The graphic component.
		 */
		public JPlotLayout getDisplay()
		{
			return this.rpl_;
		}

		/**
		 * Transforms pixel coordinates in the chart to physical coordinates.
		 * @param x X pixel coordinate.
		 * @param y Y position.
		 * @return Array with x and y position in physical coordinates, or null if
		 * an error occurs. Error should never occur anyway.
		 */
		public double[] screenCoordinatesToPhysicalCoordinates(int x, int y)
		{
			int count = 0;
			double posx = 0, posy = 0;
			Component[] c = rpl_.getComponents();
			for (int i=0; i<c.length; i++)
			{
				if (c[i] instanceof Layer) {
					posx = ((Layer) c[i]).getXDtoP(x);
					posy = ((Layer) c[i]).getYDtoP(y);

					double refX = 0.1, refY = 0.6;
					if (gridChart.levelsOrientation == GridChartElement.WEDGE_ORIENTATION.HORIZONTAL_BOTTOM) {
						refX += 0.9;
						refY += 0.9;
					}

					double x0 = rpl_.getRange().getXRange().start;
					double xf = rpl_.getRange().getXRange().end;
					double y0 = rpl_.getRange().getYRange().start;
					double yf = rpl_.getRange().getYRange().end;
					posx = x0 + (xf - x0) * (posx - refX) / (5.4 - refX);   // 0.1, 5.4, ... Constant values in the SGT library,
					posy = y0 + (yf - y0) * (posy - refY) / (5.375 - refY); // called 'physical units' by his author
					count ++;
				}
			}
			if (count > 1) return null; // Should never occur, just one chart or 'layer'
			return new double[] {posx, posy};
		}

		/**
		 * Transforms physical coordinates in the chart to pixel position.
		 * @param x X physical position.
		 * @param y Y physical position.
		 * @return Array with x and y pixel positions, or null if
		 * an error occurs. Error should never occur anyway.
		 */
		public double[] physicalCoordinatesToScreenCoordinates(double x, double y)
		{
			int count = 0;
			double posx = 0, posy = 0;
			Component[] c = rpl_.getComponents();
			for (int i=0; i<c.length; i++)
			{
				if (c[i] instanceof Layer) {
					// First physical to SGT 'physical'
					double refX = 0.1, refY = 0.6;
					if (gridChart.levelsOrientation == GridChartElement.WEDGE_ORIENTATION.HORIZONTAL_BOTTOM) {
						refX += 0.9;
						refY += 0.9;
					}

					double x0 = rpl_.getRange().getXRange().start;
					double xf = rpl_.getRange().getXRange().end;
					double y0 = rpl_.getRange().getYRange().start;
					double yf = rpl_.getRange().getYRange().end;
					posx = refX + (5.4 - refX) * (x - x0) / (xf - x0);
					posy = refY + (5.375 - refY) * (y - y0) / (yf - y0);

					posx = ((Layer) c[i]).getXPtoD(posx);
					posy = ((Layer) c[i]).getYPtoD(posy);
					count ++;
				}
			}
			if (count > 1) return null; // Should never occur, just one chart or 'layer'
			return new double[] {posx, posy};
		}

		/**
		 * Returns if a given physical position is inside the chart area.
		 * @param x X physical position.
		 * @param y Y physical position.
		 * @return True or false.
		 */
		public boolean isPhysicalPositionInsideChartArea(double x, double y)
		{
			int count = 0;
			double posx = 0, posy = 0;
			Component[] c = rpl_.getComponents();
			boolean inside = false;
			for (int i=0; i<c.length; i++)
			{
				if (c[i] instanceof Layer) {
					// First physical to SGT 'physical'
					double x0 = rpl_.getRange().getXRange().start;
					double xf = rpl_.getRange().getXRange().end;
					double y0 = rpl_.getRange().getYRange().start;
					double yf = rpl_.getRange().getYRange().end;
					posx = 0.1 + (5.4 - 0.1) * (x - x0) / (xf - x0);
					posy = 0.6 + (5.375 - 0.6) * (y - y0) / (yf - y0);

					if (posx > 0.1 && posx < 5.4 && posy > 0.6 && posy < 5.375) inside = true;
					count ++;
				}
			}
			if (count > 1) return false; // Should never occur, just one chart or 'layer'
			return inside;
		}

		private void drawPointers() {
			if (gridChart.pointers == null || gridChart.pointers.length == 0 || !rpl_.isBatch()) return;

			int n = rpl_.getComponentCount();
			if (n == 1) {
				JLabel label = new JLabel();
				label.setFocusable(false);
				label.setOpaque(false);
				label.setBackground(new Color(0, 0, 0, 0));
				rpl_.add(label, 0);
			}

			BufferedImage buf = new BufferedImage(rpl_.getWidth(), rpl_.getHeight(), BufferedImage.TYPE_INT_ARGB);
			drawPointers(buf);

			JLabel label = (JLabel) rpl_.getComponent(0);
			label.setIcon(new ImageIcon(buf));
			label.setSize(buf.getWidth(), buf.getHeight());
			label.setPreferredSize(new Dimension(buf.getWidth(), buf.getHeight()));
			label.setLocation(0, 0);
		}

		private void drawPointers(BufferedImage buf) {
			if (gridChart.pointers == null || gridChart.pointers.length == 0) return;

			Graphics2D g = buf.createGraphics();
			AWTGraphics.enableAntialiasing(g);
			g.setColor(new Color(0, 0, 0, 0));
			g.fillRect(0, 0, buf.getWidth(), buf.getHeight());
			g.setColor(Color.BLACK);
			for (int i=0; i<gridChart.pointers.length; i++) {
				String p = gridChart.pointers[i];
				double px1 = Double.parseDouble(FileIO.getField(1, p, " ", true));
				double py1 = Double.parseDouble(FileIO.getField(2, p, " ", true));
				String val3 = FileIO.getField(3, p, " ", true).toUpperCase();
				String label = FileIO.getRestAfterField(4, p, " ", true);
				boolean inside1 = this.isPhysicalPositionInsideChartArea(px1, py1);
				if (!inside1) continue;
				double sp1[] = this.physicalCoordinatesToScreenCoordinates(px1, py1);
				double py2 = Double.parseDouble(FileIO.getField(4, p, " ", true));

				if (DataSet.isDoubleStrictCheck(val3)) {
					double px2 = Double.parseDouble(val3);

					boolean inside2 = this.isPhysicalPositionInsideChartArea(px2, py2);

					if (inside2) {
						double sp2[] = this.physicalCoordinatesToScreenCoordinates(px2, py2);

						// This code is to generate a little offset between the end point
						// and the position of the label, following the line initial-end point
						if (sp2[0] != sp1[0]) {
							double m = (sp2[1] - sp1[1]) / (sp2[0] - sp1[0]);
							double n = sp2[1] - m * sp2[0];
							double b = -2*sp2[0]+2*m*n-2*sp2[1]*m;
							double a = 1 + m*m;
							double c = sp2[0]*sp2[0]+n*n+sp2[1]*sp2[1]-2*sp2[1]*n-buf.getWidth()*buf.getWidth()/(40.0*40.0);
							double s = 1;
							if (sp2[0] < sp1[0]) s = -1;
							double extendedX = (-b + s * Math.sqrt(b*b-4*a*c))*0.5/a;
							double sp3[] = new double[] {
									extendedX,
									m * extendedX + n + g.getFont().getSize()/2f,
							};
							TextLabel tl = new TextLabel(label);
							tl.draw(g, (int) sp3[0], (int) sp3[1], TextLabel.ALIGN.CENTER);
							if (px1 != px2 || py1 != py2)
								g.drawLine((int) sp1[0], (int) sp1[1], (int) sp2[0], (int) sp2[1]);
						} else { // Support for bullets or points in case of the same initial/final position
							TextLabel tl = new TextLabel(label);

							int s = g.getFont().getSize()/2;
							g.fillOval((int) sp1[0]-s, (int) sp1[1]-s, 2*s+1, 2*s+1);
							tl.draw(g, (int) sp2[0], (int) sp2[1] + s+4+g.getFont().getSize(), TextLabel.ALIGN.CENTER);
						}
					}
				} else {
					if (val3.indexOf("C") < 0) continue;
					TextLabel tl = new TextLabel(label);

					g.setStroke(AWTGraphics.getStroke(JPARSECStroke.STROKE_DEFAULT_LINE));
					if (val3.indexOf("@RED") >= 0) g.setColor(Color.RED);
					if (val3.indexOf("@GREEN") >= 0) g.setColor(Color.GREEN);
					if (val3.indexOf("@BLUE") >= 0) g.setColor(Color.BLUE);
					if (val3.indexOf("@BLACK") >= 0) g.setColor(Color.BLACK);
					if (val3.indexOf("@WHITE") >= 0) g.setColor(Color.WHITE);
					if (val3.indexOf("@CYAN") >= 0) g.setColor(Color.CYAN);
					if (val3.indexOf("@GRAY") >= 0) g.setColor(Color.GRAY);
					if (val3.indexOf("@MAGENTA") >= 0) g.setColor(Color.MAGENTA);
					if (val3.indexOf("@ORANGE") >= 0) g.setColor(Color.ORANGE);
					if (val3.indexOf("@PINK") >= 0) g.setColor(Color.PINK);
					if (val3.indexOf("@YELLOW") >= 0) g.setColor(Color.YELLOW);
					if (py2 != 0) {
						double sp1b[] = this.physicalCoordinatesToScreenCoordinates(px1, py1+py2/2);
						int s = (int) (0.5+Math.abs(sp1[1]-sp1b[1]));
						if (val3.indexOf("F") < 0) {
							g.drawOval((int) sp1[0]-s, (int) sp1[1]-s, 2*s+1, 2*s+1);
						} else {
							g.fillOval((int) sp1[0]-s, (int) sp1[1]-s, 2*s+1, 2*s+1);
						}
					}
					tl.draw(g, (int) sp1[0], (int) sp1[1] + 4+(g.getFont().getSize()*3)/2, TextLabel.ALIGN.CENTER);
				}
			}
			g.dispose();
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

			double refX = 0.1, refY = 0.6;
			if (gridChart.levelsOrientation == GridChartElement.WEDGE_ORIENTATION.HORIZONTAL_BOTTOM) {
				refX += 0.9;
				refY += 0.9;
			}

			double left = 0, right = 0, up = 0, down = 0;
			Component[] c = rpl_.getComponents();
			for (int i=0; i<c.length; i++)
			{
				if (c[i] instanceof Layer) {
					left = ((Layer) c[i]).getXPtoD(refX);
					right = ((Layer) c[i]).getXPtoD(5.4);
					down = ((Layer) c[i]).getYPtoD(refY);
					up = ((Layer) c[i]).getYPtoD(5.375);
					break;
				}
			}
	        if (clip) g.setClip((int)left, (int)up, (int)(right-left), (int)(down-up));
	        double p1[] = physicalCoordinatesToScreenCoordinates(0, 0);
	        double p2[] = physicalCoordinatesToScreenCoordinates(10, 10);
	        double sx = (p2[0] - p1[0]) / 10.0;
	        double sy = (p2[1] - p1[1]) / 10.0;
	        g.translate(p1[0], p1[1]);
	        g.scale(sx, sy);
		}

		/**
		 * Returns the scaling factor for the fonts inside the graph, 1 by default.
		 * @return Scaling factor.
		 */
		public static double getLabelFontScalingFactor() {
			return Axis.FONT_SCALE;
		}
		/**
		 * Sets the scaling factor for the text in the graph.
		 * @param scale Scaling factor, 1 by default.
		 */
		public static void setLabelFontScalingFactor(double scale) {
			Axis.FONT_SCALE = scale;
		}
		private class MyAction implements java.awt.event.ActionListener {
			public void actionPerformed(java.awt.event.ActionEvent event) {
				Object obj = event.getSource();
				if(obj == edit_ || obj == editGridItem) edit_actionPerformed(event);
				if(obj == space_ || obj == resetZoomItem) rpl_.resetZoom();
				if(obj == tree_ || obj == treeViewItem) tree_actionPerformed(event);

		        if (obj == export || obj == exportItem) {
		        	try {
			        	String name = FileIO.fileChooser(false);
			        	Picture pio = new Picture(chartAsBufferedImage());
			        	if (name != null) pio.write(name);
		        	} catch (JPARSECException e)
		        	{
						JOptionPane.showMessageDialog(null,
								Translate.translate(229),
								Translate.translate(230), JOptionPane.ERROR_MESSAGE);
		        	}
		        }
			}
		}

		/**
		 * Updates the dynamic pointers in case the zoom changes.
		 */
		public void propertyChange(PropertyChangeEvent e) {
	    	if (e.getPropertyName().equals("domainRange")) {
				SoTDomain rangeObs = getDisplay().getGraphDomain();

				boolean change = false;
				if (getChartElement().invertXaxis) {
					SoTRange r = rangeObs.getXRange();
					double delta = (Double) r.getDeltaObject();
					if (delta > 0) {
						change = true;
						r.flipStartAndEnd(); // this also inverts y axis ...
						if (getChartElement().invertYaxis) { // Y axis inversion not supported
							r = rangeObs.getYRange();
							r.flipStartAndEnd();
						}
					}
				}
				if (change) {
			    	try {
						getDisplay().setRange(rangeObs);
					} catch (PropertyVetoException exc) {
         				Logger.log(LEVEL.ERROR, "Error setting display range. Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
					}
				}
	    	}

	    	makeGraph(gridChart);
	    	drawPointers();
		}
		/**
		 * Zoom support.
		 */
		public void mouseWheelMoved(MouseWheelEvent e) {
			int x = e.getX();
			int y = e.getY();
			int z = e.getWheelRotation();
			double pos[] = this.screenCoordinatesToPhysicalCoordinates(x, y);
			try {
				Domain domain = rpl_.getRange();
				Range2D rx = domain.getXRange();
				Range2D ry = domain.getYRange();
				double zoom_x = z * ((rx.end - rx.start) / 10.0);
				double px = (0 + 1 * (pos[0] - rx.start) / (rx.end - rx.start));
				double py = (0 + 1 * (pos[1] - ry.start) / (ry.end - ry.start));
				if (px > 1.0) px = 1.0;
				if (px < 0.0) px = 0.0;
				if (py > 1.0) py = 1.0;
				if (py < 0.0) py = 0.0;
				rx.start -= zoom_x * Math.abs(px*2);
				rx.end += zoom_x * Math.abs((px-1)*2);
				double zoom_y = z * Math.abs((ry.end - ry.start) / 10.0);
				ry.start -= zoom_y * Math.abs(py*2);
				ry.end += zoom_y * Math.abs((py-1)*2);
				domain.setXRange(rx);
				domain.setYRange(ry);
				rpl_.setRange(domain);
			} catch (Exception exc) {
 				Logger.log(LEVEL.ERROR, "Error setting range. Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
			}

			drawPointers();
		}
		/**
		 * Nothing.
		 */
		public void mouseClicked(MouseEvent arg0) {
		}
		/**
		 * Nothing.
		 */
		public void mouseEntered(MouseEvent arg0) {
		}
		/**
		 * Nothing.
		 */
		public void mouseExited(MouseEvent arg0) {
		}
		/**
		 * Launches the pop up panel.
		 */
		public void mousePressed(MouseEvent e) {
			if (e.isPopupTrigger() && result != null) {
				result.show(rpl_, e.getX(), e.getY());
			}
		}
		/**
		 * Nothing.
		 */
		public void mouseReleased(MouseEvent arg0) {
		}
}
