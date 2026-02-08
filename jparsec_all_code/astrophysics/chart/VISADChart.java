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
package jparsec.astrophysics.chart;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;

import gov.noaa.pmel.sgt.swing.JPlotLayout;
import gov.noaa.pmel.util.Domain;
import jparsec.astronomy.CoordinateSystem;
import jparsec.astrophysics.gildas.LMVCube;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.Functions;
import jparsec.ephem.Target.TARGET;
import jparsec.graph.CreateGridChart;
import jparsec.graph.DataSet;
import jparsec.graph.GridChartElement;
import jparsec.io.FileIO;
import jparsec.math.Constant;
import jparsec.observer.City;
import jparsec.observer.CityElement;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.AstroDate;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.util.JPARSECException;
import jparsec.util.Logger;
import jparsec.util.Logger.LEVEL;
import jparsec.util.Translate;
import visad.util.SelectRangeWidget;
import visad.util.VisADSlider;

/**
 * A VISAD chart element consisting on a grid chart, a VISAD cube, and the control panel, to show
 * in detail a 3d cube of data.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class VISADChart implements Serializable, MouseMotionListener, MouseListener, ActionListener, MouseWheelListener, PropertyChangeListener {

	private static final long serialVersionUID = 1L;

	private int x, y, x0, y0, w0, h0;
	private CreateVISADChart ch;
	private CreateGridChart gridChartObs;
	private JPanel panel;
	/**
	 * The lmv cube used for this chart.
	 */
	public LMVCube lmvObs;
	private SelectRangeWidget rx, ry, rz;
	private boolean fast = false;
	private final int X0 = 10;
	private final int Y0 = 10;
	private int w, h;
	private JCheckBox integratedIntensity;
	private int coordType = 0;
	private JTextField posAndFlux[] = new JTextField[3];
	private float vel;
	private String contours[] = new String[] {}, fluxUnit;
	private double physX, physY;
	private int beamPosition = 4;
	private JPanel controlPanel;
	private Component rangeSliders;
	private JList contourList;
	private JButton addContour, deleteContour;
	private String title;
	private JPanel infoPanel;
	private Color background, background2;
	private JLabel labelContour;
	private JScrollPane sp;
	private JButton resetB;
	private String[] labels;
    private JLabel jlabel[] = new JLabel[3];
    private boolean reduced = false;
    private String path;

	/**
	 * Creates a chart.
	 * @param path Path to the .lmv file.
	 * @param x X position.
	 * @param y Y position
	 * @param w Width.
	 * @throws JPARSECException If an error occurs.
	 */
	public VISADChart(String path, int x, int y, int w) throws JPARSECException
	{
		if (path == null) throw new JPARSECException("no file was selected");
		this.path = path;
		lmvObs = new LMVCube(path);
		this.x = x;
		this.y = y;
		x0 = x;
		y0 = y;
		this.w = w;
		w0 = w;
		this.setHeight();
		title = null;
		background = new Color(214, 217, 223, 255);
		background2 = new Color(214, 217, 223, 255);
		this.create();
	}

	/**
	 * Creates a chart.
	 * @param lmv The lmv cube.
	 * @param x X position.
	 * @param y Y position
	 * @param w Width.
	 * @throws JPARSECException If an error occurs.
	 */
	public VISADChart(LMVCube lmv, int x, int y, int w) throws JPARSECException
	{
		lmvObs = lmv;
		this.x = x;
		this.y = y;
		x0 = x;
		y0 = y;
		this.w = w;
		w0 = w;
		this.setHeight();
		title = null;
		background = new Color(255, 255, 255, 0);
		background2 = new Color(255, 255, 255, 255);
		this.create();
	}

	/**
	 * Creates a chart.
	 * @param path Path to the .lmv file.
	 * @param x X position.
	 * @param y Y position
	 * @param w Width.
	 * @param title Title for the border.
	 * @param background Color for the background.
	 * @throws JPARSECException If an error occurs.
	 */
	public VISADChart(String path, int x, int y, int w, String title, Color background) throws JPARSECException
	{
		if (path == null) throw new JPARSECException("no file was selected");
		this.path = path;
		lmvObs = new LMVCube(path);
		this.x = x;
		this.y = y;
		x0 = x;
		y0 = y;
		this.w = w;
		w0 = w;
		this.setHeight();
		this.title = title;
		this.background = background;
		background2 = new Color(background.getRed(), background.getGreen(), background.getBlue(), 255);
		this.create();
	}

	private void create() {
		if (panel != null) return;
		try {
	        int paramWidth = 260;
	        int borderSize = X0;
	        int panelHeight = h - Y0 * 5;
			int gWidth = ((w - paramWidth)/2 - X0*2);
			reduced = false;
			if (gWidth < 250) {
				gWidth = 250;
				panelHeight = 800 * 7 / 16 - Y0 * 5;
				reduced = true;
			}
			int gHeight = panelHeight*7/8;

			float v0 = lmvObs.getv0();
			float vf = lmvObs.getvf();
    		vel = v0 + (vf - v0) * 0.5f;
			float x0 = (float) (lmvObs.getx0() * Constant.RAD_TO_ARCSEC);
			float xf = (float) (lmvObs.getxf() * Constant.RAD_TO_ARCSEC);
			float y0 = (float) (lmvObs.gety0() * Constant.RAD_TO_ARCSEC);
			float yf = (float) (lmvObs.getyf() * Constant.RAD_TO_ARCSEC);
			fluxUnit = lmvObs.fluxUnit.trim();
			VISADCubeElement cube = new VISADCubeElement(lmvObs.getCubeData(),
					  new float[] {x0, xf, y0, yf, v0, vf},
					  Translate.translate(904), //"OFFSET_RA",
					  VISADCubeElement.UNIT.ARCSEC,
					  Translate.translate(905), // "OFFSET_DEC",
					  VISADCubeElement.UNIT.ARCSEC,
					  Translate.translate(906), // "VELOCITY",
					  VISADCubeElement.UNIT.KILOMETER_PER_SECOND,
					  Translate.translate(907), //"FLUX",
					  VISADCubeElement.UNIT.KELVIN); // Should be Jy/beam, but VISAD don't accept this unit. Anyway it is not shown ...
			if (reduced) cube = new VISADCubeElement(lmvObs.getCubeData(),
					  new float[] {x0, xf, y0, yf, v0, vf},
					  Translate.translate(904), // "RA",
					  VISADCubeElement.UNIT.ARCSEC,
					  Translate.translate(905), // "DEC",
					  VISADCubeElement.UNIT.ARCSEC,
					  Translate.translate(908), // "VEL",
					  VISADCubeElement.UNIT.KILOMETER_PER_SECOND,
					  Translate.translate(907), // "FLUX",
					  VISADCubeElement.UNIT.KELVIN); // Should be Jy/beam, but VISAD don't accept this unit. Anyway it is not shown ...
			if (ch != null) {
				float valx[] = null, valy[] = null, valz[] = null;
				if (rx != null) valx = rx.getMinMaxValues();
				if (ry != null) valy = ry.getMinMaxValues();
				if (rz != null) valz = rz.getMinMaxValues();
				ch.update(cube, vel);
				if (rx != null) {
					rx.setValues(valx[0], valx[1]);
					rx.valuesUpdated();
				}
				if (ry != null) {
					ry.setValues(valy[0], valy[1]);
					ry.valuesUpdated();
				}
				if (rz != null) {
					rz.setValues(valz[0], valz[1]);
					rz.valuesUpdated();
				}
			} else {
				ch = new CreateVISADChart(cube, vel, false);
		    	rx = new SelectRangeWidget( ch.getRangeX());
		    	ry = new SelectRangeWidget( ch.getRangeY());
		    	rz = new SelectRangeWidget( ch.getRangeZ());
				ch.displays[0].setName("obs");
			}

    		float data0[][] = lmvObs.getCubeData(lmvObs.axis3Dim/2);
    		x0 = (float) (lmvObs.getx0() * Constant.RAD_TO_ARCSEC);
    		xf = (float) (lmvObs.getxf() * Constant.RAD_TO_ARCSEC);
    		y0 = (float) (lmvObs.gety0() * Constant.RAD_TO_ARCSEC);
    		yf = (float) (lmvObs.getyf() * Constant.RAD_TO_ARCSEC);
			GridChartElement chart = new GridChartElement("",
					Translate.translate(909)+" (\")", // "RA offset (\")",
					Translate.translate(910)+ " (\")", // "DEC offset (\")",
					"",
					GridChartElement.COLOR_MODEL.BLUE_TO_RED,
					new double[] {x0, xf, y0, yf},  DataSet.toDoubleArray(data0),
					null, 400);
			chart.levelsOrientation = GridChartElement.WEDGE_ORIENTATION.HORIZONTAL_TOP;
			chart.levelsBorderStyle = GridChartElement.WEDGE_BORDER.NO_BORDER;
			gridChartObs = new CreateGridChart(chart);
			gridChartObs.getDisplay().addPropertyChangeListener(this);

			panel = new JPanel();
			panel.setLayout(null);
			panel.setBounds(X0, Y0*3/2 + panelHeight, w - paramWidth - X0*4, panelHeight);
	    	panel.setBackground(background);
	    	panel.setOpaque(false);
	    	if (background != null && background.getAlpha() > 200) panel.setOpaque(true);
			if (title != null) {
				panel.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createTitledBorder(null, title, TitledBorder.LEFT, TitledBorder.TOP,
							new Font("Dialog", Font.BOLD, 16), Color.BLACK),
					BorderFactory.createEmptyBorder(borderSize,borderSize,borderSize,borderSize)));
			}
	    	if (posAndFlux == null) posAndFlux = new JTextField[6];
			posAndFlux[0] = new JTextField(18);
			posAndFlux[1] = new JTextField(18);
			posAndFlux[2] = new JTextField(18);
			if (gridChartObs != null) {
				JPlotLayout e = gridChartObs.getDisplay();
				if (!fast) panel.remove(e);
				int dx = 7, dy = 5;
				e.setBounds(X0-dx, Y0*2+1-dy, gWidth+X0*2+8*dx, gHeight+Y0+dy*2+1);
				e.removeMouseListener(this);
				e.removeMouseListener(this);
				e.removeMouseWheelListener(this);
				e.addMouseListener(this);
				e.addMouseMotionListener(this);
				e.addMouseWheelListener(this);
		    	e.setBackground(background);
				if (!fast) panel.add(e);
			}
			if (ch != null) {
				Component f = ch.displays[0].getComponent();
				f.setBackground(background);
				if (!fast) panel.remove(f);
				f.setBounds(X0*3+gWidth, Y0*3, gWidth, gHeight);
				if (!fast) panel.add(f);
			}

			controlPanel = new JPanel();
			controlPanel.removeAll();
			controlPanel.setLayout(null);
	        controlPanel.setBounds(w - paramWidth-X0 , Y0*2-4, paramWidth, panelHeight);
	        if (reduced) {
	        	controlPanel.setBounds(528 , Y0*2-4, w-528-0, panelHeight);
	        	paramWidth = w-530;
	        }
			controlPanel.setBackground(background);
			if (ch != null) {
		    	VisADSlider s = null;
		    	s = ch.getVelSlider();
				JSlider js = (JSlider) s.getComponent(0);
				js.removeMouseMotionListener(this);
				js.removeMouseListener(this);
				js.addMouseMotionListener(this);
				js.addMouseListener(this);
				s.setBounds(paramWidth/15, panelHeight/12, paramWidth*13/15, panelHeight/10);
				js.setBackground(background);
				js.setOpaque(false);
 				s.setBackground(background);
				controlPanel.add(s);
				if (rangeSliders == null) rangeSliders = createRangeSliders();
				rangeSliders.setBounds(paramWidth/30, panelHeight/5, paramWidth*28/30, panelHeight*3/7);
				rangeSliders.setBackground(background2);
				rangeSliders.setForeground(background2);
				controlPanel.add(rangeSliders);
				int s1 = -1;
				if (contourList != null) s1 = contourList.getSelectedIndex();
				contourList = new JList(contours);
				contourList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				if (s1 >=0) contourList.setSelectedIndex(s1);
				labelContour = new JLabel(Translate.translate(911)); // "Set Contours");
				labelContour.setBounds(paramWidth*11/20, panelHeight*35/56, paramWidth*2/4, panelHeight/10);
				controlPanel.add(labelContour);
				sp = new JScrollPane(contourList);
				sp.setBounds(paramWidth/30, panelHeight*18/28, paramWidth/2-paramWidth*2/15, panelHeight/5);
				sp.setOpaque(false);
				controlPanel.add(sp);
				if (addContour == null) {
					addContour = new JButton(Translate.translate(923)); // "Add");
					addContour.addActionListener(this);
				}
				if (deleteContour == null) {
					deleteContour = new JButton(Translate.translate(924)); // "Delete");
					deleteContour.addActionListener(this);
				}
				addContour.setBounds(paramWidth*2/5, panelHeight*83/112, paramWidth*2/7, panelHeight/10);
				deleteContour.setBounds(paramWidth*69/100, panelHeight*83/112, paramWidth*2/7, panelHeight/10);
				controlPanel.add(addContour);
				controlPanel.add(deleteContour);
				controlPanel.setForeground(background2);
				integratedIntensity = new JCheckBox(Translate.translate(925)); // "Integrated");
				integratedIntensity.setBounds(paramWidth/30, panelHeight*7/8, paramWidth/2-paramWidth*2/30, panelHeight/10);
				integratedIntensity.setSelected(false);
				integratedIntensity.setBackground(background);
				integratedIntensity.setOpaque(true);
				integratedIntensity.addActionListener(this);
				integratedIntensity.addMouseMotionListener(this);
				controlPanel.add(integratedIntensity);
				resetB = createResetButton();
				resetB.setBounds(paramWidth/2-paramWidth/30, panelHeight*7/8, paramWidth/2, panelHeight/10);
				controlPanel.add(resetB);

				panel.add(controlPanel);
		        infoPanel = new JPanel();
				infoPanel.removeAll();
				infoPanel.setLayout(null);
				infoPanel.setBounds(X0, Y0*2 + panelHeight, w - X0*3, Y0*2);
				infoPanel.setBackground(background);

		    	int nlines = 3;
		    	String equinox = "";
		    	if (lmvObs != null && coordType == 0) {
		    		equinox = ""+lmvObs.epoch;
		    		if (lmvObs.epoch >=2000) equinox = "J"+equinox;
		        	equinox = "("+equinox+") ";
		    	}
		    	String cx = Translate.translate(912), // "RA"
		    		cy = FileIO.addSpacesBeforeAString(Translate.translate(913), 13); // "          DEC";
		    	if (coordType == 1) {
		    		cx = Translate.translate(914); // "Ecl. Lon";
		    		cy = Translate.translate(915); // "Ecl. Lat";
		    	}
		    	if (coordType == 2) {
		    		cx = Translate.translate(916); // "Gal. Lon";
		    		cy = Translate.translate(917); // "Gal. Lat";
		    	}
		    	if (coordType == 3) {
		    		cx = Translate.translate(909); // "RA offset";
		    		cy = Translate.translate(910); // "DEC offset";
		    	}
		    	if (coordType == 4) {
		    		cx = Translate.translate(918); // "Grid x";
		    		cy = Translate.translate(919); // "Grid y";
		    	}
		    	labels = new String[] {
		    			equinox+cx, cy,
		    			FileIO.addSpacesBeforeAString(Translate.translate(920), 14) // "     Intensity"
		    	};
		    	if (w < 800) {
		    		cy = cy.substring(3);
		    		labels[2] = labels[2].substring(3);
		    		labels[0] = DataSet.replaceAll(labels[0], ".0", "", false);
		    	}
		    	if (posAndFlux == null) posAndFlux = new JTextField[6];
		    	x = X0;
		    	y = 0;
		    	int boxWidth = (w + X0*2) * 57 /(160*nlines);
		    	int ySep = Y0*5 / (2+1);
		    	int boxHeight = ySep*4/3;
		    	if (jlabel == null) jlabel = new JLabel[3];
		    	for (int i=0; i<nlines;i++)
		    	{
		    		jlabel[i] = new JLabel(labels[i]);
		        	jlabel[i].setBounds(x, y, boxWidth*4/3, boxHeight);
		        	jlabel[i].setBackground(background);
		    		infoPanel.add(jlabel[i]);
		    		posAndFlux[i] = new JTextField(18);
		    		posAndFlux[i].setBounds(x+boxWidth*3/2-X0*5, y, boxWidth*4/3, boxHeight);
		    		posAndFlux[i].setEditable(false);
		    		posAndFlux[i].setBackground(background2);
		    		infoPanel.add(posAndFlux[i]);
		    		x += boxWidth*55/20;
		    	}
		        panel.add(infoPanel);
			}
		} catch (Exception exc) {
			Logger.log(LEVEL.ERROR, "Error creating the chart. Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
		}
	}

	/**
	 * Draws the chart in the panel.
	 */
	public void draw() {
		try {
			panel.setPreferredSize(new java.awt.Dimension(w, h));
		} catch (Exception exc) {
			Logger.log(LEVEL.ERROR, "Error resizing the panel. Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
		}
	}

	/**
	 * Returns the chart as an image.
	 * @param type Type of BufferedImage.
	 * @return The image.
	 */
	public BufferedImage getImage(int type) {
		try {
			this.getComponent();
			BufferedImage buf = new BufferedImage(w, h, type);
			Graphics g = buf.createGraphics();

			// Retrieve the graphics context; this object is used to paint shapes
	        Graphics2D g2d = (Graphics2D)g;

	        // Enable antialiasing for shapes
	        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
	                             RenderingHints.VALUE_ANTIALIAS_ON);

	        // Enable antialiasing for text
	        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
	                             RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			panel.paint(g);
			return buf;
		} catch (Exception exc) {
			Logger.log(LEVEL.ERROR, "Error creating the image (check type!). Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
			return null;
		}
	}

	/**
	 * Returns a JPanel with the element.
	 * @return The JPanel.
	 */
	public JPanel getComponent() {
		panel.setLocation(x, y);
		panel.setSize(w, h);
		return panel;
	}

	/**
	 * Returns x position, without scaling.
	 * @return x.
	 */
	public int getX() { return x; }
	/**
	 * Returns y position, without scaling.
	 * @return y.
	 */
	public int getY() { return y; }

	/**
	 * Returns the width.
	 * @return w.
	 */
	public int getWidth() { return w; }
	/**
	 * Returns the height.
	 * @return h.
	 */
	public int getHeight() { return h; }

	/**
	 * Returns original height (without scaling) when
	 * the component is painted in a given frame for
	 * the first time.
	 * @return h0.
	 */
	public int getHeight0() {
		return h0;
	}

	/**
	 * Returns original width (without scaling) when
	 * the component is painted in a given frame for
	 * the first time.
	 * @return w0.
	 */
	public int getWidth0() {
		return w0;
	}

	/**
	 * Returns original x position (without scaling) when
	 * the component is painted in a given frame for
	 * the first time.
	 * @return x0.
	 */
	public int getX0() {
		return x0;
	}

	/**
	 * Returns original y position (without scaling) when
	 * the component is painted in a given frame for
	 * the first time.
	 * @return y0.
	 */
	public int getY0() {
		return y0;
	}

	/**
	 * Updates the panel when the slider is moved.
	 */
	public void mouseDragged(MouseEvent e) {
    	if (ch != null) {
    		if (e.getSource().equals(ch.getVelSlider().getComponent(0))) {
    			if (!integratedIntensity.isSelected()) updateChart(false, true, -1, -1);
    		}
    	}
    }

	/**
	 * Updates the panel when the mouse is moved on the grid chart.
	 */
	public void mouseMoved(MouseEvent e) {
		done = false;
    	int mod = e.getModifiers();
    	if (gridChartObs != null) {
	    	if (e.getSource().equals(gridChartObs.getDisplay()) && mod == 0) {
	    		int x = e.getX(), y = e.getY();
	    		updatePosAndFluxObs(x, y, true);
	    	}
    	}
    	if (e.getSource() == integratedIntensity || e.getSource() == ch.getVelSlider()
    			|| e.getSource() == ch.getVelSlider().getComponent(0)) {
    		panel.repaint();
    	}
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
	 * Updates the panel when the slider is moved.
	 */
	public void mousePressed(MouseEvent e) {
    	if (ch != null) {
    		if (e.getSource().equals(ch.getVelSlider().getComponent(0))) {
    			updateChart(false, true, -1, -1);
    		}
    	}
    }

	/**
	 * Nothing.
	 */
	public void mouseReleased(MouseEvent e) {
	}

	private boolean done = false;
	/**
	 * Updates the panel when the zoom changes on the grid chart.
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (gridChartObs != null && event.getSource() == gridChartObs.getDisplay()
				&& event.getPropertyName().equals("domainRange") && !done) {
			done = true;
			updateChart(true, true, -1, -1);
			panel.validate();
			panel.repaint();
		}
	}

	/**
	 * Updates the panel when the plane changes on the grid chart.
	 */
	public void mouseWheelMoved(MouseWheelEvent e) {
    	if (gridChartObs != null) {
	    	if (e.getSource().equals(gridChartObs.getDisplay())) {
	    		int mod = e.getModifiers();
	    		int n = e.getWheelRotation();
	    		float v0 = lmvObs.getv0();
	    		float vf = lmvObs.getvf();
	    		float step = (vf - v0)/1000f;
				float vel0 =ch.getVelSliderValue();
				if (mod == 0 && lmvObs.axis3Dim <= 1000) step *= 1000f / lmvObs.axis3Dim;
				vel0 += n*step;
				ch.setVelSliderValue(vel0);
	    		int x = e.getX(), y = e.getY();
				if (!integratedIntensity.isSelected()) updateChart(false, true, x, y);
	    		panel.repaint();
	    	}
    	}

	}

    private void updateChart(boolean anyWay, boolean paint, int x, int y)
    {
    	boolean doneObs = updateChartObs(anyWay, paint);
    	if (ch != null) vel = ch.getVelSliderValue();
		if (doneObs && paint) updatePosAndFluxObs(x, y, false);
    }
    private boolean updateChartObs(boolean anyWay, boolean paint)
    {
    	boolean done = false;
    	if (ch != null) {
			float vel0 =ch.getVelSliderValue();
    		float v0 = lmvObs.getv0();
    		float vf = lmvObs.getvf();
			float step = (lmvObs.axis3Dim-1.0f)/(vf-v0);
			int plane = (int) (step * (vel-v0) + 0.5f);
			int plane0 = (int) (step * (vel0-v0) + 0.5f);
			if (plane0 < 0 || plane0 >= lmvObs.axis3Dim) {
				if (plane0 < 0) plane0 = 0;
				if (plane0 >= lmvObs.axis3Dim) plane0 = lmvObs.axis3Dim-1;
				vel0 = v0 + plane0 / step;
				ch.setVelSliderValue(vel0);
			}
    		if (plane0 != plane || anyWay) {
    			try {
		    		float x0 = (float) (lmvObs.getx0() * Constant.RAD_TO_ARCSEC);
		    		float xf = (float) (lmvObs.getxf() * Constant.RAD_TO_ARCSEC);
		    		float y0 = (float) (lmvObs.gety0() * Constant.RAD_TO_ARCSEC);
		    		float yf = (float) (lmvObs.getyf() * Constant.RAD_TO_ARCSEC);
		    		float data0[][] = lmvObs.getCubeData(plane0);
		    		if (integratedIntensity.isSelected()) {
			    		data0 = lmvObs.getCubeData(0);
			    		float vfac = Math.abs(lmvObs.velResolution);
		    			for (int x=0; x<lmvObs.axis1Dim; x++)
		    			{
			    			for (int y=0; y<lmvObs.axis2Dim; y++)
			    			{
			    				data0[x][y] *= vfac;
			    			}
		    			}
		    			for (int z=1; z<lmvObs.axis3Dim; z++)
		    			{
				    		float data[][] = lmvObs.getCubeData(z);
			    			for (int x=0; x<lmvObs.axis1Dim; x++)
			    			{
				    			for (int y=0; y<lmvObs.axis2Dim; y++)
				    			{
				    				data0[x][y] += data[x][y] * vfac;
				    			}
			    			}
		    			}
		    		}
		    		double contour[] = null;
		    		if (contours.length > 0) {
		    			contour = new double[contours.length];
		    			for (int i=0; i<contour.length;i++)
		    			{
		    				contour[i] = Double.parseDouble(contours[i]);
		    			}
		    		}
					GridChartElement chart = new GridChartElement("",
							Translate.translate(909)+" (\")", // "RA offset (\")",
							Translate.translate(910)+" (\")", // "DEC offset (\")",
							"",
							GridChartElement.COLOR_MODEL.BLUE_TO_RED,
							new double[] {x0, xf, y0, yf},  DataSet.toDoubleArray(data0),
							contour, 400);
					chart.levelsOrientation = GridChartElement.WEDGE_ORIENTATION.HORIZONTAL_TOP;
					chart.levelsBorderStyle = GridChartElement.WEDGE_BORDER.NO_BORDER;
			        int paramWidth = 260;
			        int panelHeight = h - Y0 * 5;
					int gWidth = ((w - paramWidth)/2 - X0*2);
					if (gWidth < 250) {
						gWidth = 250;
						panelHeight = 800 * 7 / 16 - Y0 * 5;
						reduced = true;
					}
					int gHeight = panelHeight*7/8;
					Domain rangeObs = gridChartObs.getDisplay().getRange();
					gridChartObs = new CreateGridChart(chart);
					gridChartObs.getDisplay().addPropertyChangeListener(this);
					gridChartObs.getDisplay().setRange(rangeObs);
					if (paint) {
						Component g = gridChartObs.getDisplay();
				    	g.setBackground(background);
						int dx = 7, dy = 5;
						g.setBounds(X0-dx, Y0*2+1-dy, gWidth+X0*2+8*dx, gHeight+Y0+dy*2+1);
						g.addMouseListener(this);
						g.addMouseMotionListener(this);
						g.addMouseWheelListener(this);
						panel.remove(0);
						panel.add(g, 0);
					}
					done = true;
    			} catch (Exception exc) {
     				Logger.log(LEVEL.ERROR, "Error updating the chart. Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
    			}
    		}
    	}
    	return done;
    }

    private void updatePosAndFluxObs(int x, int y, boolean reCalc)
    {
		try {
			double startX = gridChartObs.getDisplay().getRange().getXRange().start;
			double startY = gridChartObs.getDisplay().getRange().getYRange().start;
			double endX = gridChartObs.getDisplay().getRange().getXRange().end;
			double endY = gridChartObs.getDisplay().getRange().getYRange().end;

			if (startX > endX) {
				double tmp = startX;
				startX = endX;
				endX = tmp;
			}
			if (startY > endY) {
				double tmp = startY;
				startY = endY;
				endY = tmp;
			}

			if (reCalc) {
				physX = gridChartObs.getDisplay().getLayer("Layer 1").getXDtoP(x) - 0.1;    // 0 -> 5.27
				physY = gridChartObs.getDisplay().getLayer("Layer 1").getYDtoP(y-2) - 0.6; // 0 -> 4.77
			}

			// Offsets in arcsec respect to map center
			double physXMap = startX + (endX - startX) * physX / 5.27;
			double physYMap = startY + (endY - startY) * physY / 4.77;
			double rx = physXMap * Constant.ARCSEC_TO_RAD;
			double ry = physYMap * Constant.ARCSEC_TO_RAD;
			double dx = (rx - lmvObs.getx0()) / (lmvObs.getxf() - lmvObs.getx0());
			double dy = (ry - lmvObs.gety0()) / (lmvObs.getyf() - lmvObs.gety0());
			double xp = ((lmvObs.axis1Dim-1.0) * dx);
			double yp = ((lmvObs.axis2Dim-1.0) * (1.0 - dy));
			double six = 1.0, siy = 1.0;
			if (xp < 0.0) six = -1.0;
			if (yp < 0.0) siy = -1.0;
			int px = (int) (xp + 0.5*six), py = lmvObs.axis2Dim - 1 - (int) (yp+0.5*siy);

			// Position respect to sky
			LocationElement loc = lmvObs.wcs.getSkyCoordinates(new java.awt.geom.Point2D.Double(xp, yp));
			if (coordType == 3) {
				loc = new LocationElement(physXMap * Constant.ARCSEC_TO_RAD, physYMap * Constant.ARCSEC_TO_RAD, 1.0);
			} else {
				loc = transformCoord(loc);
			}
			fluxUnit = lmvObs.fluxUnit.trim();
			if (integratedIntensity.isSelected()) fluxUnit+= " km/s";
			if (physXMap >= startX && physXMap <= endX && physYMap >= startY && physYMap <= endY) {
    			if (coordType >= 3) {
        			if (coordType == 3) {
        				posAndFlux[0].setText(Functions.formatAngle(loc.getLongitude(), 2));
        				posAndFlux[1].setText(Functions.formatAngle(loc.getLatitude(), 2));
        			} else {
        				posAndFlux[0].setText(""+(px+1));
        				posAndFlux[1].setText(""+(py+1));
        			}
    			} else {
	    			if (coordType == 0) {
	    				posAndFlux[0].setText(Functions.formatRA(loc.getLongitude(), 3));
	    			} else {
	    				posAndFlux[0].setText(Functions.formatDEC(loc.getLongitude(), 2));
	    			}
	    			posAndFlux[1].setText(Functions.formatDEC(loc.getLatitude(), 2));
    			}
    			posAndFlux[2].setText(Functions.formatValue(gridChartObs.getIntensityAt(physXMap, physYMap), 3)+" "+fluxUnit);
    			if (posAndFlux[2].getText().startsWith("0,000"))
    				posAndFlux[2].setText(""+(float)gridChartObs.getIntensityAt(physXMap, physYMap)+" "+fluxUnit);
			} else {
		    	posAndFlux[0].setText("");
		    	posAndFlux[1].setText("");
		    	posAndFlux[2].setText("");
			}
			posAndFlux[0].setCaretPosition(0);
			posAndFlux[1].setCaretPosition(0);
			posAndFlux[2].setCaretPosition(0);
			Graphics g = gridChartObs.getDisplay().getGraphics();
			if (beamPosition > 0 && gridChartObs != null && g != null) {
				try {
					int np = 200;
					g.setColor(Color.BLACK);
					double angStep = 2.0 * Math.PI / (double) np;
					double sx = 5.27 / (endX - startX);
					double sy = 4.77 / (endY - startY);
					double px0 = 5.27, py0 = 4.77, sx0 = -1.0, sy0= -1.0;
					if (beamPosition == 1 || beamPosition == 3) {
						px0 = 0;
						sx0 = 1.0;
					}
					if (beamPosition == 4 || beamPosition == 3) {
						py0 = 0;
						sy0 = 1.0;
					}
					px0 = px0 + lmvObs.beamMajor * Constant.RAD_TO_ARCSEC * sx * sx0 * 1.2 * 0.5;
					py0 = py0 + lmvObs.beamMajor * Constant.RAD_TO_ARCSEC * sy * sy0 * 1.2 * 0.5;
					for (double ang=0; ang<2.0*Math.PI; ang=ang+angStep)
					{
						double pxb = lmvObs.beamMinor * Constant.RAD_TO_ARCSEC * sx * Math.sin(ang) * 0.5;
						double pyb = lmvObs.beamMajor * Constant.RAD_TO_ARCSEC * sy * Math.cos(ang) * 0.5;
						double a = Math.atan2(pxb, pyb) + lmvObs.beamPA;
						double r = Math.sqrt(pxb*pxb+pyb*pyb);
						pxb = px0 + r * Math.sin(a);
						pyb = py0 + r * Math.cos(a);
						int pxf = gridChartObs.getDisplay().getLayer("Layer 1").getXPtoD(pxb+0.1);    // 0 -> 5.27
						int pyf= gridChartObs.getDisplay().getLayer("Layer 1").getYPtoD(pyb+0.6); // 0 -> 4.77
						g.drawLine(pxf, pyf, pxf, pyf+1);
					}
				} catch (Exception exc) {
     				Logger.log(LEVEL.ERROR, "Error drawing the beam. Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
				}
			}

		} catch (Exception exc) {
			Logger.log(LEVEL.ERROR, "Error updating the chart. Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
		}
    }

    private LocationElement transformCoord(LocationElement loc)
    {
    	LocationElement loc_out = loc.clone();
    	try {
    		float epoch = 2000f;
    		if (lmvObs != null) epoch = lmvObs.epoch;
    		double jd = Constant.J2000 + (epoch - 2000f) * 365.25; // approx., I know ...
    		if (epoch == (int) epoch) jd = new AstroDate((int) epoch, AstroDate.JANUARY, 1.5).jd();

			AstroDate astro = new AstroDate(jd);
			TimeElement time = new TimeElement(astro, SCALE.TERRESTRIAL_TIME);
			CityElement city = City.findCity("Madrid");
			ObserverElement observer = ObserverElement.parseCity(city);
			EphemerisElement eph = new EphemerisElement(TARGET.SUN, EphemerisElement.COORDINATES_TYPE.APPARENT,
					EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.GEOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
					EphemerisElement.FRAME.ICRF);
			if (coordType == 1) loc_out = CoordinateSystem.equatorialToEcliptic(loc, time, observer, eph);
			if (coordType == 2) loc_out = CoordinateSystem.equatorialToGalactic(loc, time, observer, eph);

    	} catch (Exception exc) {
    		Logger.log(LEVEL.ERROR, "Error rotating coordinates. Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
    	}
    	return loc_out;
    }

    private Component createRangeSliders(){
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        p.setBackground(background2);
        p.setOpaque(false);
        try {
        	rx = new SelectRangeWidget( ch.getRangeX());
        	ry = new SelectRangeWidget( ch.getRangeY());
        	rz = new SelectRangeWidget( ch.getRangeZ());
        	p.add(rx);
        	p.add(ry);
        	p.add(rz);
        }
        catch (Exception exc) {
        	Logger.log(LEVEL.ERROR, "Error creating range sliders. Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
        }
        return p;
      }

    private JButton createResetButton(){

        final JButton button = new JButton(Translate.translate(926)); // "Reset Displays");
        button.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            try {
            	updateChart(true, true, -1, -1);
            	if (ch != null) ch.displays[0].getProjectionControl().resetProjection();
            	if (gridChartObs != null) gridChartObs.getDisplay().resetZoom();
    			panel.validate();
            	panel.repaint();
        		panel.requestFocusInWindow();
            }
            catch (Exception exc) {
 				Logger.log(LEVEL.ERROR, "Error processing reset button. Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
            }
          }
        });

        return button;
      }

	/**
	 * Support to add/delete contours and to show integrated intensity.
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == addContour) {
			String s = (String)JOptionPane.showInputDialog(
	                null,
	                Translate.translate(921), // "Please insert the contour level in physical units.",
	                Translate.translate(922), // "Contour Level Input",
	                JOptionPane.PLAIN_MESSAGE
	                );
			if (s != null && s.length() > 0) {
				try {
					int n = contours.length, si = contourList.getSelectedIndex();
					String add[] = DataSet.toStringArray(s, ",");
					for (int i=0; i<add.length; i++) {
						add[i] = add[i].trim();
					}
					contours = DataSet.addStringArray(contours, add);
					contours = DataSet.getDifferentElements(contours);
					contours = DataSet.sortInCrescent(contours);
					contourList.setListData(contours);
					if (contours.length > n) {
						contourList.setSelectedIndex(contours.length-1);
					} else {
						contourList.setSelectedIndex(si);
					}
				} catch (Exception exc) {}
			}
        	updateChart(true, true, -1, -1);
        	panel.validate();
        	panel.repaint();
		}
		if (e.getSource() == deleteContour) {
			int index = contourList.getSelectedIndex();
			if (index >= 0) {
				try {
					contours = DataSet.eliminateRowFromTable(contours, index+1);
					contourList.setListData(contours);
					if (index <= (contours.length-1)) {
						contourList.setSelectedIndex(index);
					} else {
						contourList.setSelectedIndex(contours.length-1);
					}
				} catch (Exception exc) {}
			}
        	updateChart(true, true, -1, -1);
        	panel.validate();
        	panel.repaint();
		}

		if (e.getSource() == integratedIntensity) {
			updateChart(true, true, -1, -1);
			panel.validate();
        	panel.repaint();
		}
	}

	private void setHeight()
	{
		this.h = w * 7 / 16;
	}

	/**
	 * Set certain parameters for the chart.
	 * @param v Initial velocity of the plane shown.
	 * @param integrated Integrated intensity or not.
	 * @param contours Contours.
	 * @param x0 Initial RA offset.
	 * @param xf Final RA offset.
	 * @param y0 Initial DEC offset.
	 * @param yf Final DEC offset.
	 * @param z0 Initial v.
	 * @param zf Final v.
	 */
	public void setPreferences(float v, boolean integrated, String[] contours, float x0, float xf, float y0, float yf, float z0, float zf)
	{
		vel = v;
		integratedIntensity.setSelected(integrated);
		contours = DataSet.getDifferentElements(contours);
		try { contours = DataSet.sortInCrescent(contours); } catch (Exception exc) {}
		contourList.setListData(contours);
		contourList.setSelectedIndex(contours.length-1);
		this.contours = contours;
		rx.setValues(x0, xf);
		rx.valuesUpdated();
		ry.setValues(y0, yf);
		ry.valuesUpdated();
		rz.setValues(z0, zf);
		rz.valuesUpdated();
		float v0 = lmvObs.getv0();
		float vf = lmvObs.getvf();
		float step = (lmvObs.axis3Dim-1.0f)/(vf-v0);
		int plane = (int) (step * (vel-v0) + 0.5f);
		if (plane < 0 || plane >= lmvObs.axis3Dim) {
			if (plane < 0) plane = 0;
			if (plane >= lmvObs.axis3Dim) plane = lmvObs.axis3Dim-1;
		}
		ch.setVelSliderValue(v0 + plane / step);
    	updateChart(true, true, -1, -1);
    	panel.repaint();
	}
	/**
	 * Set certain parameters for the chart.
	 * @param v Initial velocity of the plane shown.
	 * @param integrated Integrated intensity or not.
	 * @param contours Contours.
	 */
	public void setPreferences(float v, boolean integrated, String[] contours)
	{
		vel = v;
		integratedIntensity.setSelected(integrated);
		contours = DataSet.getDifferentElements(contours);
		try { contours = DataSet.sortInCrescent(contours); } catch (Exception exc) {}
		contourList.setListData(contours);
		contourList.setSelectedIndex(contours.length-1);
		this.contours = contours;
		float v0 = lmvObs.getv0();
		float vf = lmvObs.getvf();
		float step = (lmvObs.axis3Dim-1.0f)/(vf-v0);
		int plane = (int) (step * (vel-v0) + 0.5f);
		if (plane < 0 || plane >= lmvObs.axis3Dim) {
			if (plane < 0) plane = 0;
			if (plane >= lmvObs.axis3Dim) plane = lmvObs.axis3Dim-1;
		}
		ch.setVelSliderValue(v0 + plane / step);
    	updateChart(true, true, -1, -1);
    	panel.repaint();
	}
	/**
	 * Set certain parameters for the chart.
	 * @param integrated Integrated intensity or not.
	 * @param contours Contours.
	 */
	public void setPreferences(boolean integrated, String[] contours)
	{
		integratedIntensity.setSelected(integrated);
		contours = DataSet.getDifferentElements(contours);
		try { contours = DataSet.sortInCrescent(contours); } catch (Exception exc) {}
		contourList.setListData(contours);
		contourList.setSelectedIndex(contours.length-1);
		this.contours = contours;
    	updateChart(true, true, -1, -1);
    	panel.repaint();
	}

	/**
	 * Writes the object to a binary file.
	 * @param out Output stream.
	 * @throws IOException If an error occurs.
	 */
	private void writeObject(ObjectOutputStream out)
	throws IOException {
		out.writeInt(this.x0);
		out.writeInt(this.y0);
		out.writeInt(this.w0);
		out.writeInt(this.background.getRGB());
		out.writeObject(this.title);

		out.writeFloat(vel);
		out.writeBoolean(integratedIntensity.isSelected());
		out.writeObject(contours);
		out.writeObject(rx.getMinMaxValues());
		out.writeObject(ry.getMinMaxValues());
		out.writeObject(rz.getMinMaxValues());

		out.writeObject(this.lmvObs);
		out.writeBoolean(reduced);
	}
	/**
	 * Constructor for an input binary file.
	 * @param in Input stream.
	 * @throws IOException If an error occurs.
	 * @throws ClassNotFoundException If an error occurs.
	 */
	public VISADChart(ObjectInputStream in)
	throws IOException, ClassNotFoundException {
		readObject(in);
	}
	/**
	 * Reads the object.
	 * @param in Input stream.
	 * @throws IOException If an error occurs.
	 * @throws ClassNotFoundException If an error occurs.
	 */
	private void readObject(ObjectInputStream in)
	throws IOException, ClassNotFoundException {
		x = in.readInt();
		y = in.readInt();
		w = in.readInt();
		int backg = in.readInt();
		title = (String) in.readObject();

		float vel = in.readFloat();
		boolean ii = in.readBoolean();
		String con[] = (String[]) in.readObject();
		float[] rxVal = (float[]) in.readObject();
		float[] ryVal = (float[]) in.readObject();
		float[] rzVal = (float[]) in.readObject();

		lmvObs = (LMVCube) in.readObject();
 		reduced = in.readBoolean();

		x0 = x;
		y0 = y;
		w0 = w;
		this.setHeight();
		this.background = new Color(backg);
		background2 = new Color(background.getRed(), background.getGreen(), background.getBlue(), 255);

		try {
			this.create();

			this.setPreferences(vel, ii, con, rxVal[0], rxVal[1], ryVal[0], ryVal[1], rzVal[0], rzVal[1]);
		} catch (Exception exc) {
			panel = new JPanel();
		}
 	}

	/**
	 * Updates the panel.
	 */
	public void update() {
		if (gridChartObs == null || ch == null || controlPanel == null) return;

		w = panel.getSize().width;
		int hh = panel.getSize().height;
		setHeight();
		if (h > hh) {
			h = hh;
		}

		int paramWidth = 260;
        int panelHeight = h - Y0 * 5;
		int gWidth = ((w - paramWidth)/2 - X0*2);
		reduced = false;
		if (gWidth < 250) {
			gWidth = 250;
			panelHeight = 800 * 7 / 16 - Y0 * 5;
			reduced = true;
		}
		int gHeight = panelHeight*7/8;

		if (gridChartObs != null) {
			JPlotLayout e = gridChartObs.getDisplay();
			int dx = 7, dy = 5;
			e.setBounds(X0-dx, Y0*2+1-dy, gWidth+X0*2+8*dx, gHeight+Y0+dy*2+1);
		}
		if (ch != null) {
			Component f = ch.displays[0].getComponent();
			f.setBounds(X0*3+gWidth, Y0*3, gWidth, gHeight);
		}

        controlPanel.setBounds(w - paramWidth-X0 , Y0*2-4, paramWidth, panelHeight);
        if (reduced) {
        	controlPanel.setBounds(528 , Y0*2-4, w-528-0, panelHeight);
        	paramWidth = w-530;
        }
		if (ch != null) {
	    	VisADSlider s = null;
	    	s = ch.getVelSlider();
			s.setBounds(paramWidth/15, panelHeight/12, paramWidth*13/15, panelHeight/10);
			rangeSliders.setBounds(paramWidth/30, panelHeight/5, paramWidth*28/30, panelHeight*3/7);
			labelContour.setBounds(paramWidth*11/20, panelHeight*35/56, paramWidth*2/4, panelHeight/10);
			sp.setBounds(paramWidth/30, panelHeight*18/28, paramWidth/2-paramWidth*2/15, panelHeight/5);
			addContour.setBounds(paramWidth*2/5, panelHeight*83/112, paramWidth*2/7, panelHeight/10);
			deleteContour.setBounds(paramWidth*69/100, panelHeight*83/112, paramWidth*2/7, panelHeight/10);
			integratedIntensity.setBounds(paramWidth/30, panelHeight*7/8, paramWidth/2-paramWidth*2/30, panelHeight/10);
			resetB.setBounds(paramWidth/2-paramWidth/30, panelHeight*7/8, paramWidth/2, panelHeight/10);
			infoPanel.setBounds(X0, Y0*2 + panelHeight, w - X0*3, Y0*2);

			int nlines = 3;
			x = X0;
	    	y = 0;
	    	int boxWidth = (w + X0*2) * 57 /(160*nlines);
	    	int ySep = Y0*5 / (2+1);
	    	int boxHeight = ySep*4/3;
	    	for (int i=0; i<nlines;i++)
	    	{
	        	jlabel[i].setBounds(x, y, boxWidth*4/3, boxHeight);
	    		posAndFlux[i].setBounds(x+boxWidth*3/2-X0*5, y, boxWidth*4/3, boxHeight);
	    		x += boxWidth*55/20;
	    	}
		}
		panel.revalidate();
		panel.repaint();
	}

	/**
	 * Returns the path to the input lmv file, if any.
	 * @return The path, or null if the cube was created from scratch.
	 */
	public String getFile() {
		return path;
	}
}
