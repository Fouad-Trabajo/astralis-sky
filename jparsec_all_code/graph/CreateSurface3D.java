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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import jparsec.graph.GridChartElement.COLOR_MODEL;
import jparsec.math.Evaluation;
import jparsec.util.JPARSECException;
import jparsec.util.Logger;
import jparsec.util.Logger.LEVEL;
import net.ericaro.surfaceplotter.DefaultSurfaceModel;
import net.ericaro.surfaceplotter.Mapper;
import net.ericaro.surfaceplotter.beans.JGridBagScrollPane;
import net.ericaro.surfaceplotter.surface.AbstractSurfaceModel;
import net.ericaro.surfaceplotter.surface.JSurface;
import net.ericaro.surfaceplotter.surface.SurfaceModel;
import net.ericaro.surfaceplotter.surface.SurfaceModel.PlotColor;
import net.ericaro.surfaceplotter.surface.SurfaceModel.PlotType;
import net.ericaro.surfaceplotter.surface.VerticalConfigurationPanel;

/**
 * Creates a surface plot using the SurfacePlotter library by Eric
 * Aro and Yanto Suryono. A surface plot can be rotated, zoomed, and
 * resized with the mouse. F2 key allows to show/hide a panel with more
 * options. Ctrl + mouse can be used to pan the surface.
 * @author eric
 * @author T. Alonso Albi - OAN (Spain)
 */
public class CreateSurface3D implements Serializable {

	private static final long serialVersionUID = 1L;

	private GridChartElement chart[];
	private String f[];
	private double[] lim;

	private JPanel panel;
	private JFrame frame;
	private DefaultSurfaceModel sm;

	/**
	 * Creates a surface from a grid chart object. Note certain
	 * properties of the object are not supported in this class,
	 * like pointers, subtitles, or the properties for the levels.
	 * @param grid The grid Object.
	 */
	public CreateSurface3D(GridChartElement grid) {
		init(createDefaultSurfaceModel(grid));
		this.setTitleText(grid.title);
	}

	/**
	 * Creates a surface from two grid chart objects. Note certain
	 * properties of the objects are not supported in this class,
	 * like pointers, subtitles, or the properties for the levels.
	 * @param grid1 The first grid Object.
	 * @param grid2 The second grid Object.
	 */
	public CreateSurface3D(GridChartElement grid1, GridChartElement grid2) {
		init(createDefaultSurfaceModel(new GridChartElement[] {grid1, grid2}));
		this.setTitleText(grid1.title);
	}

	/**
	 * Creates a surface plot from a function. The plot
	 * is restricted to the input limits.
	 * @param f The function f(x, y) in Java notation.
	 * @param lim Limits of the plot: minimum x,
	 * maximum x, minimum y, and maximum y.
	 * @param xLabel the label for x axis.
	 * @param yLabel the label for y axis.
	 * @param zLabel the label for z axis.
	 * @param colorModel The color model.
	 */
	public CreateSurface3D(String f, double lim[], String xLabel, String yLabel, String zLabel,
			COLOR_MODEL colorModel) {
		this.lim = lim;
		init(createDefaultSurfaceModel(f, lim));
		surface.setXLabel(xLabel);
		surface.setYLabel(yLabel);
		surface.setToolTipText(zLabel);
		if (colorModel == GridChartElement.COLOR_MODEL.BLACK_TO_WHITE
				|| colorModel == GridChartElement.COLOR_MODEL.WHITE_TO_BLACK) sm.setPlotColor(PlotColor.GRAYSCALE);
		this.setTitleText(f);
	}

	/**
	 * Creates a surface plot with two functions. The plot
	 * is restricted to the input limits.
	 * @param f1 The first function f(x, y) in Java notation.
	 * @param f2 The second function f(x, y) in Java notation. Can be null.
	 * @param lim Limits of the plot: minimum x,
	 * maximum x, minimum y, and maximum y.
	 * @param xLabel the label for x axis.
	 * @param yLabel the label for y axis.
	 * @param zLabel the label for z axis.
	 * @param colorModel The color model.
	 */
	public CreateSurface3D(String f1, String f2, double lim[], String xLabel, String yLabel, String zLabel,
			COLOR_MODEL colorModel) {
		this.lim = lim;
		init(createDefaultSurfaceModel(new String[] {f1, f2}, lim));
		surface.setXLabel(xLabel);
		surface.setYLabel(yLabel);
		surface.setToolTipText(zLabel);
		if (colorModel == GridChartElement.COLOR_MODEL.BLACK_TO_WHITE
				|| colorModel == GridChartElement.COLOR_MODEL.WHITE_TO_BLACK) sm.setPlotColor(PlotColor.GRAYSCALE);
		this.setTitleText(f1+", "+f2);
	}

	/**
	 * Returns the first grid chart object.
	 * @return The chart object.
	 */
	public GridChartElement getChartElement() {
		if (chart == null) return null;
		return chart[0];
	}

	/**
	 * Returns the second grid chart object.
	 * @return The chart object.
	 */
	public GridChartElement getSecondChartElement() {
		if (chart == null || chart.length < 2) return null;
		return chart[1];
	}

	/**
	 * Returns the function/s used for the surface/s.
	 * @return The function/s, or null if it was created from a grid chart.
	 */
	public String[] getFunctions() {
		return f;
	}

	/**
	 * Returns the limits of the visual area for a surface defined by a function.
	 * @return The limits (minimum x, maximum x, minimum y, maximum y),
	 * or null if the limits are inside the grid chart object.
	 */
	public double[] getLimits() {
		return lim;
	}

	private void configureModel(DefaultSurfaceModel sm, GridChartElement grid) {
		if (grid.type == GridChartElement.TYPE.CONTOUR) {
			sm.setMesh(true);
			sm.setPlotType(PlotType.WIREFRAME);
		}
		if (grid.type == GridChartElement.TYPE.RASTER_CONTOUR) {
			sm.setMesh(true);
		}
		if (grid.type == GridChartElement.TYPE.AREA_FILL) {
			sm.setPlotType(PlotType.DENSITY);
		}
		if (grid.type == GridChartElement.TYPE.AREA_FILL_CONTOUR) {
			sm.setPlotType(PlotType.CONTOUR);
			sm.setMesh(true);
		}
		if (grid.colorModel == GridChartElement.COLOR_MODEL.BLACK_TO_WHITE
				|| grid.colorModel == GridChartElement.COLOR_MODEL.WHITE_TO_BLACK) sm.setPlotColor(PlotColor.GRAYSCALE);
	}
	private SurfaceModel createDefaultSurfaceModel(final GridChartElement grid) {
		chart = new GridChartElement[] {grid};
		sm = new DefaultSurfaceModel();

		sm.setPlotFunction2(false);

		sm.setCalcDivisions(50);
		sm.setDispDivisions(50);
		sm.setContourLines(10);

		sm.setXMin((float) grid.limits[0]);
		sm.setXMax((float) grid.limits[1]);
		sm.setYMin((float) grid.limits[2]);
		sm.setYMax((float) grid.limits[3]);

		sm.setBoxed(false);
		sm.setDisplayXY(true);
		sm.setExpectDelay(false);
		sm.setAutoScaleZ(true);
		sm.setDisplayZ(true);
		sm.setMesh(false);
		sm.setPlotType(PlotType.SURFACE);
		sm.setPlotColor(PlotColor.SPECTRUM);

		configureModel(sm, grid);

		sm.setFirstFunctionOnly(true);
		sm.setMapper(new Mapper() {
			public  float f1( float x, float y)
			{
				return (float) grid.getIntensityAt(x, y);
			}

			public  float f2( float x, float y)
			{
				return 0;
			}
		});
		return sm;
	}

	private SurfaceModel createDefaultSurfaceModel(final GridChartElement grid[]) {
		if (grid.length == 1) return createDefaultSurfaceModel(grid[0]);

		chart = grid;
		sm = new DefaultSurfaceModel();

		sm.setPlotFunction12(true, true);

		sm.setCalcDivisions(50);
		sm.setDispDivisions(50);
		sm.setContourLines(10);

		sm.setXMin((float) grid[0].limits[0]);
		sm.setXMax((float) grid[0].limits[1]);
		sm.setYMin((float) grid[0].limits[2]);
		sm.setYMax((float) grid[0].limits[3]);

		sm.setBoxed(false);
		sm.setDisplayXY(true);
		sm.setExpectDelay(false);
		sm.setAutoScaleZ(true);
		sm.setDisplayZ(true);
		sm.setMesh(false);
		sm.setPlotType(PlotType.SURFACE);
		sm.setPlotColor(PlotColor.SPECTRUM);

		configureModel(sm, grid[0]);

		sm.setMapper(new Mapper() {
			public  float f1( float x, float y)
			{
				return (float) grid[0].getIntensityAt(x, y);
			}

			public  float f2( float x, float y)
			{
				return (float) grid[1].getIntensityAt(x, y);
			}
		});
		return sm;
	}

	private SurfaceModel createDefaultSurfaceModel(final String f, double lim[]) {
		this.f = new String[] {f};
		sm = new DefaultSurfaceModel();

		sm.setPlotFunction2(false);

		sm.setCalcDivisions(50);
		sm.setDispDivisions(50);
		sm.setContourLines(10);

		sm.setXMin((float) lim[0]);
		sm.setXMax((float) lim[1]);
		sm.setYMin((float) lim[2]);
		sm.setYMax((float) lim[3]);

		sm.setBoxed(false);
		sm.setDisplayXY(true);
		sm.setExpectDelay(false);
		sm.setAutoScaleZ(true);
		sm.setDisplayZ(true);
		sm.setMesh(false);
		sm.setPlotType(PlotType.SURFACE);
		sm.setFirstFunctionOnly(true);
		sm.setPlotColor(PlotColor.SPECTRUM);

		final Evaluation js = new Evaluation();
		sm.setMapper(new Mapper() {
			public  float f1( float x, float y)
			{
				try {
					return (float) js.evaluateMathExpression(f, new String[] {"x "+x, "y "+y});
				} catch (Exception e) {
					return 0;
				}
			}

			public  float f2( float x, float y)
			{
				return 0;
			}
		});
		return sm;
	}

	private SurfaceModel createDefaultSurfaceModel(final String f[], double lim[]) {
		if (f.length == 1) return createDefaultSurfaceModel(f[0], lim);
		this.f = f;

		sm = new DefaultSurfaceModel();

		sm.setPlotFunction12(true, true);

		sm.setCalcDivisions(50);
		sm.setDispDivisions(50);
		sm.setContourLines(10);

		sm.setXMin((float) lim[0]);
		sm.setXMax((float) lim[1]);
		sm.setYMin((float) lim[2]);
		sm.setYMax((float) lim[3]);

		sm.setBoxed(false);
		sm.setDisplayXY(true);
		sm.setExpectDelay(false);
		sm.setAutoScaleZ(true);
		sm.setDisplayZ(true);
		sm.setMesh(false);
		sm.setPlotType(PlotType.SURFACE);

		sm.setPlotColor(PlotColor.SPECTRUM);

		final Evaluation js = new Evaluation();
		sm.setMapper(new Mapper() {
			public  float f1( float x, float y)
			{
				try {
					return (float) js.evaluateMathExpression(f[0], new String[] {"x "+x, "y "+y});
				} catch (Exception e) {
					return 0;
				}
			}

			public  float f2( float x, float y)
			{
				try {
					return (float) js.evaluateMathExpression(f[1], new String[] {"x "+x, "y "+y});
				} catch (Exception e) {
					return 0;
				}
			}
		});
		return sm;
	}

	private void init (SurfaceModel model) {
		panel = new JPanel();
		panel.setLayout(new BorderLayout());
		initComponents();

		String name = (String) configurationToggler.getValue(Action.NAME);
		panel.getActionMap().put(name, configurationToggler);
		panel.getInputMap(JPanel.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), name);

		setModel(model);
	}

	private void setModel(SurfaceModel model) {
		if (model instanceof AbstractSurfaceModel)
			configurationPanel.setModel((AbstractSurfaceModel) model);
		else {
			scrollpane.setVisible(false);
			configurationPanel.setModel(null);
		}
		surface.setModel(model);
	}


	/**
	 * Sets title font.
	 * @param font The font.
	 */
	public void setTitleFont(Font font) {
		title.setFont(font);
	}

	/**
	 * Sets title icon.
	 * @param icon An icon.
	 */
	public void setTitleIcon(Icon icon) {
		title.setIcon(icon);
	}

	/**
	 * Sets the title text.
	 * @param text Title text.
	 */
	public void setTitleText(String text) {
		title.setText(text);
	}

	/**
	 * Gets the title text.
	 * @return The title.
	 */
	public String getTitleText() {
		return title.getText();
	}

	/**
	 * Returns if the configuration panel is visible.
	 * @return True or false.
	 */
	public boolean isConfigurationVisible() {
		return scrollpane.isVisible();
	}

	/**
	 * Shows or hides the configuration panel.
	 * @param aFlag True or false.
	 */
	public void setConfigurationVisible(boolean aFlag) {
		scrollpane.setVisible(aFlag);
		panel.invalidate();
		panel.revalidate();
	}


	private void toggleConfiguration() {
		setConfigurationVisible(!isConfigurationVisible());
		if ( !isConfigurationVisible())
			panel.requestFocusInWindow();
	}

	/**
	 * Returns the surface object for advanced
	 * operations, for instance the SVG export (doExport methods).
	 * @return The surface object.
	 */
	public JSurface getSurface() {
		return surface;
	}

	private void surfaceMouseClicked(MouseEvent e) {
		if (e.getClickCount()>=2)
			toggleConfiguration();
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		ResourceBundle bundle = ResourceBundle.getBundle("net.ericaro.surfaceplotter.JSurfacePanel");
		title = new JLabel();
		surface = new JSurface();
		scrollpane = new JGridBagScrollPane();
		configurationPanel = new VerticalConfigurationPanel();
		configurationToggler = new AbstractAction(){public void actionPerformed(ActionEvent e){toggleConfiguration();}};

		String xLabel = "x", yLabel = "y", zLabel = "z";
		if (chart != null) {
			xLabel = chart[0].xLabel;
			yLabel = chart[0].yLabel;
			zLabel = chart[0].legend;
			if (chart.length > 1) zLabel += ", "+chart[1].legend;
		}
		surface.setXLabel(xLabel);
		surface.setYLabel(yLabel);
		surface.setToolTipText(zLabel);
		surface.setFont(new Font("Dialog", Font.PLAIN, 12)); // Default font allows to show special Unicode symbols

		//======== this ========
		panel.setName("this");
		panel.setLayout(new GridBagLayout());
		((GridBagLayout)panel.getLayout()).columnWidths = new int[] {0, 0, 0};
		((GridBagLayout)panel.getLayout()).rowHeights = new int[] {0, 0, 0};
		((GridBagLayout)panel.getLayout()).columnWeights = new double[] {1.0, 0.0, 1.0E-4};
		((GridBagLayout)panel.getLayout()).rowWeights = new double[] {0.0, 1.0, 1.0E-4};

		//---- title ----
		title.setText(bundle.getString("title.text"));
		title.setHorizontalTextPosition(SwingConstants.CENTER);
		title.setHorizontalAlignment(SwingConstants.CENTER);
		title.setBackground(Color.white);
		title.setOpaque(true);
		title.setFont(title.getFont().deriveFont(title.getFont().getSize() + 4f));
		title.setFont(new Font("Dialog", Font.BOLD, 16)); // Default font allows to show special Unicode symbols
		title.setName("title");
		panel.add(title, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.BOTH,
			new Insets(0, 0, 0, 0), 0, 0));

		//---- surface ----
		surface.setToolTipText(bundle.getString("surface.toolTipText"));
		surface.setInheritsPopupMenu(true);
		surface.setName("surface");
		surface.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				surfaceMouseClicked(e);
			}
			@Override
			public void mousePressed(MouseEvent e) {
				panel.requestFocusInWindow();
			}
		});
		panel.add(surface, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.BOTH,
			new Insets(0, 0, 0, 0), 0, 0));

		//======== scrollpane ========
		{
			scrollpane.setWidthFixed(true);
			scrollpane.setName("scrollpane");

			//---- configurationPanel ----
			configurationPanel.setNextFocusableComponent(panel);
			configurationPanel.setName("configurationPanel");
			scrollpane.setViewportView(configurationPanel);
		}
		panel.add(scrollpane, new GridBagConstraints(1, 0, 1, 2, 0.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.BOTH,
			new Insets(0, 0, 0, 0), 0, 0));

		//---- configurationToggler ----
		configurationToggler.putValue(Action.NAME, bundle.getString("configurationToggler.Name"));
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
		if (chart != null)
			surface.setSize(chart[0].imageWidth, chart[0].imageHeight);
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private JLabel title;
	private JSurface surface;
	private JGridBagScrollPane scrollpane;
	private VerticalConfigurationPanel configurationPanel;
	private AbstractAction configurationToggler;
	// JFormDesigner - End of variables declaration  //GEN-END:variables

	private boolean isSurfaceCalculated = false;
	private void calc() {
		if (!isSurfaceCalculated) sm.plot().execute();
		isSurfaceCalculated = true;
	}

	/**
	 * Returns the component.
	 * @return The panel.
	 */
	public JPanel getComponent() {
		calc();
		return panel;
	}

	/**
	 * Shows the surface in a JFrame.
	 * @param w The width.
	 * @param h The height.
	 * @param title The title of the window.
	 * @param showConfig True to show the configuration panel.
	 */
	public void show(int w, int h, String title, boolean showConfig) {
		setConfigurationVisible(showConfig);

		frame = new JFrame(title);
		frame.setBackground(Color.WHITE);
		frame.add(getComponent());
		frame.pack();
		frame.setResizable(true);
		frame.setVisible(true);
		frame.setSize(new Dimension(w, h));
	}

	/**
	 * Returns the surface model that allows extra control
	 * on the plot appearance.
	 * @return The surface model.
	 */
	public DefaultSurfaceModel getModel() {
		return sm;
	}

	private BufferedImage buf;
	/**
	 * Returns the chart as an image.
	 * @return The image.
	 * @throws JPARSECException If an error occurs.
	 */
	public BufferedImage chartAsBufferedImage()
	throws JPARSECException{
		try {
			calc();
			int w = 650+50, h = 600+34;
			if (chart != null) {
				w = chart[0].imageWidth;
				h = chart[0].imageHeight;
			}
			final boolean isShown = surface.isValid();
			if (!isShown && frame == null) this.show(w, h, "", false);
			w = frame.getWidth();
			h = frame.getHeight();
		    buf = new BufferedImage(w-10, h-34, BufferedImage.TYPE_INT_RGB);
			Graphics g = buf.createGraphics();
			g.setColor(Color.WHITE);
			g.clearRect(0, 0, w, h);
			g.dispose();
		    Thread t = new Thread(new Runnable() {
		    	public void run() {
	    			try {
	    				Thread.sleep(1000);
			    		do {
			    			try {
			    				Thread.sleep(1000);
			    			} catch (InterruptedException e) {
			    				Logger.log(LEVEL.ERROR, "Error sleeping thread. Message was: "+e.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e.getStackTrace()));
			    			}
		    			} while (!surface.isValid() || !surface.isEnabled());
	    				Graphics g = buf.createGraphics();
	    				panel.paintAll(g);
	    			} catch (Exception e) {
	    				Logger.log(LEVEL.ERROR, "Error painting chart to image. Message was: "+e.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e.getStackTrace()));
	    			}
	    			if (!isShown) frame.dispose();
		    	}
			});
		    t.start();
		    return buf;
		} catch (Exception e) {
			throw new JPARSECException("could not get the image.", e);
		}
	}

	/**
	 * Exports the chart as an SVG file.
	 * @param file_name Path to output file.
	 * @throws JPARSECException If an error occurs.
	 */
	public void chartAsSVGFile(String file_name)
	throws JPARSECException{
		int ext = file_name.toLowerCase().lastIndexOf(".svg");
		if (ext > 0) file_name = file_name.substring(0, ext);

		File plotFile = new File(file_name + ".svg");

		try
		{
			calc();
			int w = 650+50, h = 600+34;
			if (chart != null) {
				w = chart[0].imageWidth;
				h = chart[0].imageHeight;
			}
			final boolean isShown = surface.isValid();
			if (!isShown && frame == null) this.show(w, h, "", false);
			w = frame.getWidth();
			h = frame.getHeight();
			final Dimension size = new Dimension(w, h);

			// Using reflection so that everything will work without freehep in classpath
			final Class c = Class.forName("org.freehep.graphicsio.svg.SVGGraphics2D");
			Constructor cc = c.getConstructor(new Class[] {plotFile.getClass(), size.getClass()});
			final Object svgGraphics = cc.newInstance(new Object[] {plotFile, size});
			Method m = c.getMethod("startExport");
			m.invoke(svgGraphics);

		    Thread t = new Thread(new Runnable() {
		    	public void run() {
				try {
					Thread.sleep(1000);
					do {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							Logger.log(LEVEL.ERROR, "Error sleeping thread. Message was: "+e.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e.getStackTrace()));
						}
					} while (!surface.isValid() || !surface.isEnabled());
					panel.paintComponents((Graphics2D) (svgGraphics));

					Method mm = c.getMethod("endExport");
					mm.invoke(svgGraphics);
				} catch (Exception e) {
					Logger.log(LEVEL.ERROR, "Error painting chart to image. Message was: "+e.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e.getStackTrace()));
				}
				if (!isShown) frame.dispose();
			    	}
			});

			t.start();
		} catch (Exception e)
		{
			throw new JPARSECException("cannot write to file.", e);
		}
	}

	/**
	 * Exports the chart as an EPS file.
	 * @param file_name Path to output file.
	 * @throws JPARSECException If an error occurs.
	 */
	public void chartAsEPSFile(String file_name)
	throws JPARSECException{
		int ext = file_name.toLowerCase().lastIndexOf(".eps");
		if (ext > 0) file_name = file_name.substring(0, ext);

		File plotFile = new File(file_name + ".eps");

		try
		{
			calc();
			int w = 650+50, h = 600+34;
			if (chart != null) {
				w = chart[0].imageWidth;
				h = chart[0].imageHeight;
			}
			final boolean isShown = surface.isValid();
			if (!isShown && frame == null) this.show(w, h, "", false);
			w = frame.getWidth();
			h = frame.getHeight();
			final Dimension size = new Dimension(w, h);

			// Using reflection so that everything will work without freehep in classpath
			final Class c = Class.forName("org.freehep.graphicsio.ps.PSGraphics2D");
			Constructor cc = c.getConstructor(new Class[] {plotFile.getClass(), size.getClass()});
			final Object psGraphics = cc.newInstance(new Object[] {plotFile, size});
			Method m = c.getMethod("startExport");
			m.invoke(psGraphics, null);

		    Thread t = new Thread(new Runnable() {
		    	public void run() {
	    			try {
	    				Thread.sleep(1000);
			    		do {
			    			try {
			    				Thread.sleep(1000);
			    			} catch (InterruptedException e) {
			    				Logger.log(LEVEL.ERROR, "Error sleeping thread. Message was: "+e.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e.getStackTrace()));
			    			}
		    			} while (!surface.isValid() || !surface.isEnabled());
	    				panel.paintComponents((Graphics2D) (psGraphics));

	    				Method mm = c.getMethod("endExport");
	    				mm.invoke(psGraphics);
	    			} catch (Exception e) {
	    				Logger.log(LEVEL.ERROR, "Error painting chart to image. Message was: "+e.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e.getStackTrace()));
	    			}
	    			if (!isShown) frame.dispose();
		    	}
			});

		    t.start();
		} catch (Exception e)
		{
			throw new JPARSECException("cannot write to file.", e);
		}
	}

	/**
	 * Exports the chart as an PDF file.
	 * @param file_name Path to output file.
	 * @throws JPARSECException If an error occurs.
	 */
	public void chartAsPDFFile(String file_name)
	throws JPARSECException{
		int ext = file_name.toLowerCase().lastIndexOf(".pdf");
		if (ext > 0) file_name = file_name.substring(0, ext);

		File plotFile = new File(file_name + ".pdf");

		try
		{
			calc();
			int w = 650+50, h = 600+34;
			if (chart != null) {
				w = chart[0].imageWidth;
				h = chart[0].imageHeight;
			}
			final boolean isShown = surface.isValid();
			if (!isShown && frame == null) this.show(w, h, "", false);
			w = frame.getWidth();
			h = frame.getHeight();
			final Dimension size = new Dimension(w, h);

			// Using reflection so that everything will work without freehep in classpath
			final Class c = Class.forName("org.freehep.graphicsio.pdf.PDFGraphics2D");
			Constructor cc = c.getConstructor(new Class[] {plotFile.getClass(), size.getClass()});
			final Object pdfGraphics = cc.newInstance(new Object[] {plotFile, size});
			Method m = c.getMethod("startExport");
			m.invoke(pdfGraphics);

		    Thread t = new Thread(new Runnable() {
		    	public void run() {
				try {
					Thread.sleep(1000);
					do {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							Logger.log(LEVEL.ERROR, "Error sleeping thread. Message was: "+e.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e.getStackTrace()));
						}
					} while (!surface.isValid() || !surface.isEnabled());
					panel.paintComponents((Graphics2D) (pdfGraphics));

					Method mm = c.getMethod("endExport");
					mm.invoke(pdfGraphics);
				} catch (Exception e) {
					Logger.log(LEVEL.ERROR, "Error painting chart to image. Message was: "+e.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e.getStackTrace()));
				}
				if (!isShown) frame.dispose();
			    	}
			});

		    t.start();
		} catch (Exception e)
		{
			throw new JPARSECException("cannot write to file.", e);
		}
	}

	private void writeObject(ObjectOutputStream out)
	throws IOException {
		out.writeObject(this.chart);
		String f[] = this.f;
		if (f != null) {
			if (f.length == 1) f = DataSet.addStringArray(f, new String[] {null});
			f = DataSet.addStringArray(f, new String[] {surface.getXLabel(), surface.getYLabel(), surface.getToolTipText(),
					sm.getPlotColor().name()});
		}
		out.writeObject(f);
		out.writeObject(this.lim);
		out.writeObject(this.title.getText());
		out.writeBoolean(this.isConfigurationVisible());
	}
	private void readObject(ObjectInputStream in)
	throws IOException, ClassNotFoundException {
		this.chart = (GridChartElement[]) in.readObject();
		String f[] = (String[]) in.readObject();
		this.f = new String[] {f[0], f[1]};
		this.lim = (double[]) in.readObject();
		String t = (String) in.readObject();
		boolean c = in.readBoolean();

		if (chart != null) {
			init(createDefaultSurfaceModel(chart));
		} else {
			init(createDefaultSurfaceModel(f, lim));
			if (f.length > 2) {
				surface.setXLabel(f[2]);
				surface.setYLabel(f[3]);
				surface.setToolTipText(f[4]);
				sm.setPlotColor(PlotColor.valueOf(f[5]));
			}
		}
		this.setTitleText(t);
		this.setConfigurationVisible(c);
 	}
}
