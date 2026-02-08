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
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

import jparsec.astronomy.CCDElement;
import jparsec.astronomy.Constellation;
import jparsec.astronomy.Constellation.CONSTELLATION_NAME;
import jparsec.astronomy.CoordinateSystem;
import jparsec.astronomy.CoordinateSystem.COORDINATE_SYSTEM;
import jparsec.astronomy.OcularElement;
import jparsec.astronomy.Star;
import jparsec.astronomy.TelescopeElement;
import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.EphemerisElement.ALGORITHM;
import jparsec.ephem.EphemerisElement.COORDINATES_TYPE;
import jparsec.ephem.Functions;
import jparsec.ephem.Precession;
import jparsec.ephem.RiseSetTransit;
import jparsec.ephem.Target;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.event.EventReport;
import jparsec.ephem.event.MainEvents;
import jparsec.ephem.event.MainEvents.EVENT_TIME;
import jparsec.ephem.event.SimpleEventElement;
import jparsec.ephem.event.SimpleEventElement.EVENT;
import jparsec.ephem.moons.MoonEphem;
import jparsec.ephem.moons.MoonEphemElement;
import jparsec.ephem.planets.EphemElement;
import jparsec.ephem.planets.OrbitEphem;
import jparsec.ephem.planets.OrbitalElement;
import jparsec.ephem.stars.DoubleStarElement;
import jparsec.ephem.stars.StarElement;
import jparsec.ephem.stars.VariableStarElement;
import jparsec.graph.chartRendering.AWTGraphics;
import jparsec.graph.chartRendering.Graphics.ANAGLYPH_COLOR_MODE;
import jparsec.graph.chartRendering.Graphics.FONT;
import jparsec.graph.chartRendering.PlanetRenderElement;
import jparsec.graph.chartRendering.Projection;
import jparsec.graph.chartRendering.RenderEclipse;
import jparsec.graph.chartRendering.RenderPlanet;
import jparsec.graph.chartRendering.RenderSatellite;
import jparsec.graph.chartRendering.RenderSky;
import jparsec.graph.chartRendering.RenderSky.OBJECT;
import jparsec.graph.chartRendering.SatelliteRenderElement.PLANET_MAP;
import jparsec.graph.chartRendering.SkyRenderElement;
import jparsec.graph.chartRendering.SkyRenderElement.COLOR_MODE;
import jparsec.graph.chartRendering.SkyRenderElement.CONSTELLATION_CONTOUR;
import jparsec.graph.chartRendering.SkyRenderElement.FAST_LINES;
import jparsec.graph.chartRendering.SkyRenderElement.HORIZON_TEXTURE;
import jparsec.graph.chartRendering.SkyRenderElement.LEYEND_POSITION;
import jparsec.graph.chartRendering.SkyRenderElement.MILKY_WAY_TEXTURE;
import jparsec.graph.chartRendering.SkyRenderElement.REALISTIC_STARS;
import jparsec.graph.chartRendering.SkyRenderElement.STAR_LABELS;
import jparsec.graph.chartRendering.SkyRenderElement.SUPERIMPOSED_LABELS;
import jparsec.graph.chartRendering.TrajectoryElement;
import jparsec.graph.chartRendering.frame.JTableRendering;
import jparsec.graph.chartRendering.frame.PlanetaryRendering;
import jparsec.graph.chartRendering.frame.SkyRendering;
import jparsec.io.ConsoleReport;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.io.SystemClipboard;
import jparsec.io.device.GenericCamera;
import jparsec.io.device.GenericTelescope;
import jparsec.io.device.GenericTelescope.MOUNT;
import jparsec.io.device.VirtualTelescope;
import jparsec.io.device.implementation.ExternalTelescope;
import jparsec.io.image.Draw;
import jparsec.io.image.Picture;
import jparsec.math.Constant;
import jparsec.math.FastMath;
import jparsec.observer.City;
import jparsec.observer.CityElement;
import jparsec.observer.ExtraterrestrialObserverElement;
import jparsec.observer.LocationElement;
import jparsec.observer.Observatory;
import jparsec.observer.ObservatoryElement;
import jparsec.observer.ObserverElement;
import jparsec.observer.ObserverElement.DST_RULE;
import jparsec.time.AstroDate;
import jparsec.time.SiderealTime;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.time.TimeFormat;
import jparsec.time.TimeScale;
import jparsec.time.calendar.CalendarGenericConversion;
import jparsec.time.calendar.CalendarGenericConversion.CALENDAR;
import jparsec.util.DataBase;
import jparsec.util.JPARSECException;
import jparsec.util.Logger;
import jparsec.util.Logger.LEVEL;
import jparsec.util.Translate;
import jparsec.util.Translate.LANGUAGE;
import jparsec.vo.Feed;
import jparsec.vo.FeedMessageElement;
import jparsec.vo.SimbadElement;

/**
 * An interactive sky chart element.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class SkyChart implements Serializable, KeyListener, MouseMotionListener, MouseListener, MouseWheelListener, ActionListener {

	private static final long serialVersionUID = 1L;

	/**
	 * The key ID for zoom in action. Page down by default.
	 */
	public static int KEY_ZOOM_IN = KeyEvent.VK_PAGE_DOWN;
	/**
	 * The key ID for zoom out action. Page up by default.
	 */
	public static int KEY_ZOOM_OUT = KeyEvent.VK_PAGE_UP;

	/**
	 * The sky rendering object used for this chart.
	 */
    public SkyRendering skyRender;
    /**
     * The Sky object.
     */
    public SkyRenderElement chart;
    /**
     * The Sky object for dragging. Default value is null for
     * an automatic mode.
     */
    public SkyRenderElement chartForDragging;
    
    /** True when the telescope was moved from this component. You can force this to false after 
     * doing something externally. */
    public boolean telescopeMoved = false;

    /**
     * Controls the size of the font used for the popup menu.
     */
	public float menuFontSize = 15f;

    private JTableRendering table = null;
	private int x, y, x0, y0, w0, h0;
	private LEYEND_POSITION originalShowLegend;
 	private String object = null;
 	private boolean showModifyLocTime = true, listShown = false, showLockDrag = true, 
 			showEvents = false, showListObj = true, showCompleteTelescopeMenu = true;
 	private boolean isApplet = false, allowPointSun = false;
 	private Timer timer, timerTel;
 	private boolean now = false, hMode = false; //, invertH = false, invertV = false;
 	private double timeVel = 0.0, posEQ[] = null, posEQvel = 1.0E-3;
 	private int updateTime = 2000; //, updateTime0 = 2000, updateTime1 = 200;
 	private TimeElement time;
 	private ObserverElement obs;
 	private EphemerisElement eph;
 	private int colorMode = -1;
 	private String lastObj, telescopeName[] = new String[0], telescopePort[] = new String[0];
 	private GenericTelescope telescopeControl[] = new GenericTelescope[0];
 	private LocationElement lastLoc;
 	private static final int drawLeyendMinimumWidth = 700, minimumSize = 15;

 	private int imageType = BufferedImage.TYPE_INT_RGB;

	/**
	 * Creates a chart.
	 * @param w Width.
	 * @param h Height.
	 * @param sky The sky rendering object.
	 * @throws JPARSECException If an error occurs.
	 */
	public SkyChart(int w, int h, SkyRendering sky) throws JPARSECException
	{
		setDefaults(w, h, sky);
		originalShowLegend = sky.getRenderSkyObject().render.drawLeyend;
		if (w < drawLeyendMinimumWidth && (originalShowLegend == LEYEND_POSITION.BOTTOM || originalShowLegend == LEYEND_POSITION.TOP)) sky.getRenderSkyObject().render.drawLeyend = LEYEND_POSITION.NO_LEYEND;
		if (h < drawLeyendMinimumWidth && (originalShowLegend == LEYEND_POSITION.LEFT || originalShowLegend == LEYEND_POSITION.RIGHT)) sky.getRenderSkyObject().render.drawLeyend = LEYEND_POSITION.NO_LEYEND;
		this.x = 0;
		this.y = 0;
		x0 = x;
		y0 = y;
		w0 = chart.width;
		h0 = chart.height;
		this.create();

		//this.updateTime = updateTime * 1000;
		this.isApplet = true;
		//speedTested = true;

	}

	/**
	 * Creates a chart.
	 * @param w Width.
	 * @param h Height.
	 * @param sky The sky rendering object.
	 * @param showModifyLocTimeButtons False to disable the modification
	 * of location and time.
	 * @param isApplet True if the component is inside an applet. The only effect is a wait
	 * message when ephemerides of comets/asteroids will be calculated in the next frame.
	 * @param updateTime Time interval to update in seconds for real-time mode. Default
	 * value is 5s.
	 * @param transparency True to use transparency in the sky rendering, allowing to use
	 * transparent backgrounds.
	 * @throws JPARSECException If an error occurs.
	 */
	public SkyChart(int w, int h, SkyRendering sky, boolean showModifyLocTimeButtons, boolean isApplet,
			int updateTime, boolean transparency) throws JPARSECException
	{
		if (transparency) {
			imageType = BufferedImage.TYPE_INT_ARGB;
			AWTGraphics.setBufferedImageType(imageType);
		}
		this.updateTime = updateTime * 1000;
		this.isApplet = isApplet;
		showModifyLocTime = showModifyLocTimeButtons;
		setDefaults(w, h, sky);
		originalShowLegend = sky.getRenderSkyObject().render.drawLeyend;
		if (w < drawLeyendMinimumWidth && (originalShowLegend == LEYEND_POSITION.BOTTOM || originalShowLegend == LEYEND_POSITION.TOP)) sky.getRenderSkyObject().render.drawLeyend = LEYEND_POSITION.NO_LEYEND;
		if (h < drawLeyendMinimumWidth && (originalShowLegend == LEYEND_POSITION.LEFT || originalShowLegend == LEYEND_POSITION.RIGHT)) sky.getRenderSkyObject().render.drawLeyend = LEYEND_POSITION.NO_LEYEND;
		this.x = 0;
		this.y = 0;
		x0 = x;
		y0 = y;
		w0 = chart.width;
		h0 = chart.height;
		this.create();
	}

	/**
	 * Creates a chart.
	 * @param w Width.
	 * @param h Height.
	 * @param sky The sky rendering object.
	 * @param events True to compute events for the year corresponding to the sky rendering object.
	 * @param showModifyLocTimeButtons False to disable the modification
	 * of location and time.
	 * @param isApplet True if the component is inside an applet. The only effect is a wait
	 * message when ephemerides of comets/asteroids will be calculated in the next frame.
	 * @param updateTime Time interval to update in seconds for real-time mode. Default
	 * value is 5s.
	 * @param transparency True to use transparency in the sky rendering, allowing to use
	 * transparent backgrounds.
	 * @throws JPARSECException If an error occurs.
	 */
	public SkyChart(int w, int h, SkyRendering sky, boolean events, boolean showModifyLocTimeButtons, 
			boolean isApplet, int updateTime, boolean transparency) throws JPARSECException
	{
		if (transparency) {
			imageType = BufferedImage.TYPE_INT_ARGB;
			AWTGraphics.setBufferedImageType(imageType);
		}
		this.updateTime = updateTime * 1000;
		this.isApplet = isApplet;
		showModifyLocTime = showModifyLocTimeButtons;
		setDefaults(w, h, sky);
		originalShowLegend = sky.getRenderSkyObject().render.drawLeyend;
		if (w < drawLeyendMinimumWidth && (originalShowLegend == LEYEND_POSITION.BOTTOM || originalShowLegend == LEYEND_POSITION.TOP)) sky.getRenderSkyObject().render.drawLeyend = LEYEND_POSITION.NO_LEYEND;
		if (h < drawLeyendMinimumWidth && (originalShowLegend == LEYEND_POSITION.LEFT || originalShowLegend == LEYEND_POSITION.RIGHT)) sky.getRenderSkyObject().render.drawLeyend = LEYEND_POSITION.NO_LEYEND;
		this.x = 0;
		this.y = 0;
		x0 = x;
		y0 = y;
		w0 = chart.width;
		h0 = chart.height;
		setShowEvents(events);
		this.create();
	}
	
	/**
	 * Creates a chart.
	 * @param w Width.
	 * @param h Height.
	 * @param sky The sky rendering object.
	 * @param showModifyLocTimeButtons False to disable the modification
	 * of location and time.
	 * @param isApplet True if the component is inside an applet. The only effect is a wait
	 * message when ephemerides of comets/asteroids will be calculated in the next frame.
	 * @param updateTime Time interval to update in seconds for real-time mode. Default
	 * value is 5s.
	 * @param increaseSpeed True to increase speed when dragging (fast labels are enabled
	 * and Milky way is not filled). Default value is automatic, measuring speed at runtime.
	 * @param transparency True to use transparency in the sky rendering, allowing to use
	 * transparent backgrounds.
	 * @throws JPARSECException If an error occurs.
	 */
	public SkyChart(int w, int h, SkyRendering sky, boolean showModifyLocTimeButtons, boolean isApplet,
			int updateTime, boolean increaseSpeed, boolean transparency) throws JPARSECException
	{
		if (transparency) {
			imageType = BufferedImage.TYPE_INT_ARGB;
			AWTGraphics.setBufferedImageType(imageType);
		}
		this.updateTime = updateTime * 1000;
		this.isApplet = isApplet;
		showModifyLocTime = showModifyLocTimeButtons;
		setDefaults(w, h, sky);
		originalShowLegend = sky.getRenderSkyObject().render.drawLeyend;
		if (w < drawLeyendMinimumWidth && (originalShowLegend == LEYEND_POSITION.BOTTOM || originalShowLegend == LEYEND_POSITION.TOP)) sky.getRenderSkyObject().render.drawLeyend = LEYEND_POSITION.NO_LEYEND;
		if (h < drawLeyendMinimumWidth && (originalShowLegend == LEYEND_POSITION.LEFT || originalShowLegend == LEYEND_POSITION.RIGHT)) sky.getRenderSkyObject().render.drawLeyend = LEYEND_POSITION.NO_LEYEND;
		this.x = 0;
		this.y = 0;
		x0 = x;
		y0 = y;
		w0 = chart.width;
		h0 = chart.height;
		speedTested = true;
		this.increaseSpeed = increaseSpeed;
		this.create();
	}

	/**
	 * Creates a chart.
	 * @param w Width.
	 * @param h Height.
	 * @param sky The sky rendering object.
	 * @param showModifyLocTimeButtons False to disable the modification
	 * of location and time.
	 * @param isApplet True if the component is inside an applet. The only effect is a wait
	 * message when ephemerides of comets/asteroids will be calculated in the next frame.
	 * @param updateTime Time interval to update in seconds for real-time mode. Default
	 * value is 5s.
	 * @param skyForDragging The properties for the rendering when the sky is being dragged.
	 * @param transparency True to use transparency in the sky rendering, allowing to use
	 * transparent backgrounds.
	 * @throws JPARSECException If an error occurs.
	 */
	public SkyChart(int w, int h, SkyRendering sky, boolean showModifyLocTimeButtons, boolean isApplet,
			int updateTime, SkyRenderElement skyForDragging, boolean transparency) throws JPARSECException
	{
		if (transparency) {
			imageType = BufferedImage.TYPE_INT_ARGB;
			AWTGraphics.setBufferedImageType(imageType);
		}
		this.updateTime = updateTime * 1000;
		this.isApplet = isApplet;
		showModifyLocTime = showModifyLocTimeButtons;
		setDefaults(w, h, sky);
		originalShowLegend = sky.getRenderSkyObject().render.drawLeyend;
		if (w < drawLeyendMinimumWidth && (originalShowLegend == LEYEND_POSITION.BOTTOM || originalShowLegend == LEYEND_POSITION.TOP)) sky.getRenderSkyObject().render.drawLeyend = LEYEND_POSITION.NO_LEYEND;
		if (h < drawLeyendMinimumWidth && (originalShowLegend == LEYEND_POSITION.LEFT || originalShowLegend == LEYEND_POSITION.RIGHT)) sky.getRenderSkyObject().render.drawLeyend = LEYEND_POSITION.NO_LEYEND;
		this.x = 0;
		this.y = 0;
		x0 = x;
		y0 = y;
		w0 = chart.width;
		h0 = chart.height;
		this.chartForDragging = skyForDragging.clone();
		chartForDragging.width = w0;
		chartForDragging.height = h0;
		this.create();
	}

	/**
	 * Adds a telescope to show a mark of its current location on the screen. Can also be 
	 * used to change the port or name of an existing telescope.
	 * @param name Telescope name or label.
	 * @param telescope The telescope instance.
	 * @throws JPARSECException If an error occurs.
	 */
	public void addTelescopeMark(String name, GenericTelescope telescope) throws JPARSECException {
		String port = telescope.getTelescopePort();
		if (port != null) {
			if (DataSet.getIndex(telescopePort, port) >= 0)
				throw new JPARSECException("The telescope is already being controlled");
		}
		GenericTelescope tel[] = getTelescopeControls();
		int index = -1;
		if (tel != null && tel.length > 0) {
			for (int i=0; i<tel.length; i++) {
				if (tel[i].equals(telescope)) {
					index = i;
					break;
				}
			}
		}
		
		if (index < 0) {
			telescopePort = DataSet.addStringArray(telescopePort, new String[] {port});
			telescopeName = DataSet.addStringArray(telescopeName, new String[] {name});
			telescopeControl = (GenericTelescope[]) DataSet.addObjectArray(telescopeControl, new GenericTelescope[] {telescope});
		} else {
			telescopePort[index] = port;
			telescopeName[index] = name;
		}

		if (timerTel == null || timer == null || !timer.isRunning()) {
			updateTimer();
			timerTel.start();
		}
	}

	/**
	 * Returns the names of the telescopes already connected.
	 * @return Array with telescopes names, length 0 in case there
	 * is no one.
	 */
	public String[] getTelescopes() {
		return telescopeName;
	}

	/**
	 * Returns the ports with telescopes already connected.
	 * @return Array with telescope ports, length 0 in case there
	 * is no one.
	 */
	public String[] getTelescopePorts() {
		return telescopePort;
	}

	/**
	 * Returns the control of each of the telescopes connected,
	 * so they can be controled from other programs.
	 * @return Array with telescope controls, length 0 if there is
	 * no one.
	 */
	public GenericTelescope[] getTelescopeControls() {
		return telescopeControl;
	}

	/**
	 * Removes a telescope.
	 * @param name The name of the telescope.
	 * @throws JPARSECException If an error occurs.
	 */
	public void removeTelescopeMark(String name) throws JPARSECException {
		int index = DataSet.getIndex(telescopeName, name);
		if (index >= 0) {
			telescopeName = DataSet.eliminateRowFromTable(telescopeName, index+1);
			telescopeControl = (GenericTelescope[]) DataSet.deleteIndex(telescopeControl, index);
			if (telescopeName.length == 0) updateTimer();
		}
	}
	private synchronized void updateImage() {
	      try {
	    	  if (skyRender.getRenderSkyObject().render.width > 0) {
/*	    		  if (buffer == null) {
	    				panel.createBufferStrategy(2);
	    				buffer = panel.getBufferStrategy();
	    		  }
*/	    		  if (telescopeName != null && telescopeName.length > 0) {
/*		    		  Graphics2D g = (Graphics2D)buffer.getDrawGraphics();
		    		  g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		    		  g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		    		  g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		    		  g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
*/
		    		  skyRender.paintChart(skyImage);
	    			  drawTelescopes(skyImage.createGraphics());
	    		  } else {
		    		  skyRender.paintChart(skyImage);
	    		  }
	    		  //if (!buffer.contentsLost())
	    			  //buffer.show();
					panel.repaint();
	    	  }
	      } catch ( Exception ve ) {
	    	  //Logger.log(LEVEL.ERROR, "Error painting chart. Message was: "+ve.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(ve.getStackTrace()));
	      }
	}

	private void drawTelescopes(Graphics2D g) {
		if (chart.coordinateSystem != COORDINATE_SYSTEM.EQUATORIAL && chart.coordinateSystem != COORDINATE_SYSTEM.HORIZONTAL) return;

		g.setColor(Picture.invertColor(new Color(skyRender.getRenderSkyObject().render.background)));
		float rec[] = skyRender.getRenderSkyObject().getRectangle();
		g.setClip((int)rec[0]+1, (int)rec[1]+1, (int)rec[2]-2, (int)rec[3]-2);
		for (int i=0; i<telescopeName.length; i++) {
			try {
				if (!telescopeControl[i].isConnected()) continue;
				if (telescopeControl[i] instanceof ExternalTelescope) {
					((ExternalTelescope) telescopeControl[i]).setTime(getTimeObject());
				}
				LocationElement loc = null;
				if (eph.equinox == EphemerisElement.EQUINOX_OF_DATE) {
					loc = telescopeControl[i].getApparentEquatorialPosition();
				} else {
					loc = telescopeControl[i].getJ2000EquatorialPosition();					
				}

				float pos[] = skyRender.getRenderSkyObject().getSkyPosition(
						loc,
						true, true, false);
				SkyRenderElement render = skyRender.getRenderSkyObject().render;
				Projection projection = skyRender.getRenderSkyObject().getProjection();
				if (render.telescope.invertHorizontal) pos[0] = render.width - 1 - pos[0];
				if (render.telescope.invertVertical) pos[1] = render.height - 1 - pos[1];
				if (pos != null) {
					int x = (int) (pos[0]+0.5), y = (int) (pos[1]+0.5);
					g.translate(x, y);
					int index = 0;
					boolean titleDrawn  = false;
					do {
						if (index >= telescopeControl[i].getCameras().length) break;
						//if (telescopeControl[i].getFieldOfView(index) == -1) break;
						int r = (int) ((telescopeControl[i].getFieldOfView(index) / skyRender.getRenderSkyObject().render.telescope.getField()) * skyRender.getRenderSkyObject().render.width);
						boolean showLabel = true;
						if (r < 30) {
							r = 30;
							showLabel  = false;
							g.setStroke(AWTGraphics.getStroke(JPARSECStroke.STROKE_LINES_MEDIUM_THIN));
						}
						GenericCamera camera = telescopeControl[i].getCameras()[index];
						double ang = camera.getCameraOrientation();

						double ang2 = projection.getNorthAngleAt(loc, true, false) - Constant.PI_OVER_TWO;
						if (telescopeControl[i].getMount() == MOUNT.AZIMUTHAL) {
							if (chart.coordinateSystem == COORDINATE_SYSTEM.HORIZONTAL) ang2 = 0;
							if (chart.coordinateSystem == COORDINATE_SYSTEM.EQUATORIAL) 
								ang2 = -(projection.getCenitAngleAt(loc, true) - Constant.PI_OVER_TWO);
						}
						
						if (render.telescope.invertHorizontal || render.telescope.invertVertical) {
							if (render.telescope.invertHorizontal) {
								ang2 = (Math.PI - ang2);
							}
							if (render.telescope.invertVertical) {
								ang2 = -ang2;
							}
						}
						ang += ang2;

						if (ang != 0.0) g.rotate(ang);
						int hr = r/2;
						int rh = (int) (r / camera.getWidthHeightRatio());
						if (camera.getCameraModel().hasRoundSensor()) {
							g.drawOval(-hr, -rh/2, r, rh);
						} else {
							g.drawRect(-hr, -rh/2, r, rh);
						}
						/*
						g.drawOval(x, y, r, r);
						int s = 5;
						g.drawLine(x+hr, y-s, x+hr, y+s);
						g.drawLine(x+hr, y+r-s, x+hr, y+r-1+s);
						g.drawLine(x+r-s, y+hr, x+r-1+s, y+hr);
						g.drawLine(x-s, y+hr, x+s, y+hr);
						*/
						if (ang != 0.0) g.rotate(-ang);

						if (!titleDrawn && (showLabel || i == telescopeName.length-1)) {
							String tn = telescopeName[i];
							if (telescopeControl[i] instanceof VirtualTelescope) 
								tn = telescopeControl[i].getTelescopeName();
							TextLabel tl = new TextLabel(tn);
							tl.draw(g, 0, -(12+((ang == 0)? hr:r)), TextLabel.ALIGN.CENTER);
							titleDrawn = true;
						}
						if (showLabel) {
							if (telescopeControl[i].getCameras().length > 1) {
								String s = "- "+camera.getCameraName()+" -";
								TextLabel tl = new TextLabel(s);
								tl.draw(g, 0, 8+g.getFontMetrics().getHeight()+((ang == 0)? hr:r), TextLabel.ALIGN.CENTER);
							}
						}
						index ++;
					} while(true);
					g.translate(-x, -y);
				}
			} catch (Exception exc) {}
		}
	}

    /**
     * Render the scene
     */
    public synchronized void paintImage()
    {
    	if (panel == null || panel.getGraphics() == null) return;
    	if (timeVel == 0.0 && skyRender.getRenderSkyObject().willCalculateForAWhile()) {
    		BufferedImage offscreenImage = new BufferedImage(chart.width, chart.height, imageType);
    		Graphics2D g = offscreenImage.createGraphics();
    		g.setColor(new Color(chart.background));
    		g.fillRect(0, 0, chart.width, chart.height);
    		g.setColor(Picture.invertColor(new Color(chart.background)));
    		g.setFont(new Font(Font.DIALOG, Font.PLAIN, 14));
    		String s = Translate.translate(974);
    		int w = g.getFontMetrics().stringWidth(s);
    		g.drawString(s, chart.width/2-w/2, chart.height/2+7);
    		g.dispose();
    		panel.getGraphics().drawImage(offscreenImage,0,0,null);

			skyRender.getRenderSkyObject().forceUpdateFast();
    	}
    	updateImage();
    }


    /**
     * Render the scene
     */
    private void paintIt()
    {
		try {
			fastField = skyRender.getRenderSkyObject().render.telescope.getField();
			vel = 1.5 * fastField / chart.width;
		} catch (Exception e) {	}

    	if (isApplet) {
    		paintImage();
    		updating = false;
    		return;
    	}

    	updateImage();

		updating = false;
    }
	private void create() {
		setStrings();
		try {
			max = 360 * Constant.DEG_TO_RAD;
			if (chart.height < chart.width) max = (380 * Constant.DEG_TO_RAD * chart.width) / chart.height;
			skyImage = new BufferedImage(chart.width, chart.height, imageType);
			panel = new JLabel(new ImageIcon(skyImage)); /* {
				@Override
				public void paint(Graphics g) {
					try {
						g.drawImage(skyImage, 0, 0, null);
					} catch (Exception e) {}
				}
			};
			*/
			panel.setForeground(null);
			panel.setBackground(null);
	        panel.addKeyListener(this);

			panel.setLocation(x, y);
			panel.setSize(chart.width, chart.height);
			panel.addMouseMotionListener(this);
			panel.addMouseListener(this);
			panel.addMouseWheelListener(this);

			panel.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentResized(ComponentEvent e) {
					update();
						skyRender.getRenderSkyObject().colorSquemeChanged();
						//skyRender.getRenderSkyObject().render.planetRender.textures = false;
						skyRender.getRenderSkyObject().render.drawMilkyWayContoursWithTextures = MILKY_WAY_TEXTURE.NO_TEXTURE;
						skyRender.getRenderSkyObject().render.drawHorizonTexture = HORIZON_TEXTURE.NONE;
						skyImage = new BufferedImage(panel.getWidth(), panel.getHeight(), imageType);
						panel.setIcon(new ImageIcon(skyImage));
				}
			});

            updateTimer();

            if (Runtime.getRuntime().availableProcessors() > 1 && showEvents) {
            	thread = new Thread(new thread0());
    			thread.start();
            }

    		SwingUtilities.invokeLater(new Runnable() {
                public void run() {
        			paintImage();
                }
            });

		} catch (Exception exc) {
			Logger.log(LEVEL.ERROR, "Error creating the chart. Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
		}
	}

	private void updateTimer() {
        if (timer != null) timer.stop();
        timer = new Timer(updateTime, this);
        timer.setRepeats(true);
        if (now) timer.start();
        
        timerTel = new Timer(500, this);
        timerTel.setRepeats(true);
	}

	/**
	 * Returns the chart as an image.
	 * @return The image.
	 */
	public BufferedImage getImage() {
		try {
			return skyRender.getLastRenderedImage();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Returns a JPanel with the sky rendering.
	 * @return The JPanel.
	 */
	public JPanel getComponent() {
		if (panel == null) {
			skyImage = new BufferedImage(chart.width, chart.height, imageType);
			panel = new JLabel(new ImageIcon(skyImage)); /* {
				@Override
				public void paint(Graphics g) {
					try {
						g.drawImage(skyImage, 0, 0, null);
					} catch (Exception e) {}
				}
			};
*/			panel.setForeground(null);
			panel.setBackground(null);
	        panel.addKeyListener(this);

			panel.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentResized(ComponentEvent e) {
					update();
						skyRender.getRenderSkyObject().colorSquemeChanged();
						//skyRender.getRenderSkyObject().render.planetRender.textures = false;
						skyRender.getRenderSkyObject().render.drawMilkyWayContoursWithTextures = MILKY_WAY_TEXTURE.NO_TEXTURE;
						skyRender.getRenderSkyObject().render.drawHorizonTexture = HORIZON_TEXTURE.NONE;
						skyImage = new BufferedImage(panel.getWidth(), panel.getHeight(), imageType);
						panel.setIcon(new ImageIcon(skyImage));
				}
			});
		}
		panel.setSize(chart.width, chart.height);
		paintImage();
		try {
			fastField = skyRender.getRenderSkyObject().render.telescope.getField();
		} catch (Exception e) {	}
		vel = 1.5 * fastField / chart.width;

		JPanel jpanel = new JPanel(null);
		jpanel.add(panel);

		jpanel.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				update();
					skyRender.getRenderSkyObject().colorSquemeChanged();
					//skyRender.getRenderSkyObject().render.planetRender.textures = false;
					skyRender.getRenderSkyObject().render.drawMilkyWayContoursWithTextures = MILKY_WAY_TEXTURE.NO_TEXTURE;
					skyRender.getRenderSkyObject().render.drawHorizonTexture = HORIZON_TEXTURE.NONE;
					skyImage = new BufferedImage(panel.getWidth(), panel.getHeight(), imageType);
					panel.setIcon(new ImageIcon(skyImage));
			}
		});

 		jpanel.setPreferredSize(new Dimension(chart.width, chart.height));
 		jpanel.setBounds(0, 0, chart.width, chart.height);
		return jpanel;
	}
	private JLabel panel;
	private BufferedImage skyImage = null;
	private boolean updating = false;
	//private BufferStrategy buffer;

	/**
	 * Updates the panel to a new size.
	 * @return True if the update process is going on, false it is not
	 * started since another previous one is still going on.
	 */
	public boolean update() {
		if (panel.isVisible() && !updating) {
			updating = true;
			Dimension d = panel.getParent().getSize();
			panel.setSize(d);
	    	w0 = d.width;
	    	h0 = d.height;
			SkyRenderElement ss = skyRender.getRenderSkyObject().render;
			ss.width = d.width;
			ss.height = d.height;
			ss.drawLeyend = this.originalShowLegend;
			if (ss.width < drawLeyendMinimumWidth && (originalShowLegend == LEYEND_POSITION.BOTTOM || originalShowLegend == LEYEND_POSITION.TOP)) ss.drawLeyend = LEYEND_POSITION.NO_LEYEND;
			if (ss.height < drawLeyendMinimumWidth && (originalShowLegend == LEYEND_POSITION.LEFT || originalShowLegend == LEYEND_POSITION.RIGHT)) ss.drawLeyend = LEYEND_POSITION.NO_LEYEND;
			chart = ss;
			if (chartForDragging != null) {
				chartForDragging.width = w0;
				chartForDragging.height = h0;
			}
			try {
				skyRender.getRenderSkyObject().setSkyRenderElement(ss);
			} catch (Exception e) {
				Logger.log(LEVEL.ERROR, "Error updating the chart. Message was: "+e.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e.getStackTrace()));
			}
			SwingUtilities.invokeLater(new Runnable() {
		        public void run() {
		        	paintIt();
		        }
			});
			return true;
		}
		return false;
	}

	/**
	 * Enables the transparency in the main panel.
	 */
	public void enableTransparency() {
		//if (imageType == BufferedImage.TYPE_INT_ARGB) return;
		imageType  = BufferedImage.TYPE_INT_ARGB;
		skyImage = new BufferedImage(panel.getWidth(), panel.getHeight(), imageType);
		AWTGraphics.setBufferedImageType(imageType);
		paintIt();
	}
	/**
	 * Disables the transparency in the main panel.
	 */
	public void disableTransparency() {
		//if (imageType == BufferedImage.TYPE_INT_RGB) return;
		imageType  = BufferedImage.TYPE_INT_RGB;
		skyImage = new BufferedImage(panel.getWidth(), panel.getHeight(), imageType);
		AWTGraphics.setBufferedImageType(imageType);
		paintIt();
	}

	/**
	 * Set if the Sun can be the target to point a telescope. Default is false.
	 * @param allow True or false.
	 */
	public void allowPointSun(boolean allow) {
		allowPointSun = allow;
	}
	
	/**
	 * Updates the panel to a new size.
	 * @param w With.
	 * @param h Height.
	 * @return True if the update process is going on, false it is not
	 * started since another previous one is still going on.
	 */
	public boolean update(int w, int h) {
		if (w0 == w && h0 == h) return false;
		if (panel.isVisible() && !updating) {
			updating = true;
			Dimension d = new Dimension(w, h);
			panel.setBounds(0, 0, w, h);
	    	w0 = d.width;
	    	h0 = d.height;
			SkyRenderElement ss = skyRender.getRenderSkyObject().render;
			ss.width = d.width;
			ss.height = d.height;
			ss.drawLeyend = this.originalShowLegend;
			if (ss.width < drawLeyendMinimumWidth && (originalShowLegend == LEYEND_POSITION.BOTTOM || originalShowLegend == LEYEND_POSITION.TOP)) ss.drawLeyend = LEYEND_POSITION.NO_LEYEND;
			if (ss.height < drawLeyendMinimumWidth && (originalShowLegend == LEYEND_POSITION.LEFT || originalShowLegend == LEYEND_POSITION.RIGHT)) ss.drawLeyend = LEYEND_POSITION.NO_LEYEND;
			chart = ss;
			try {
				skyRender.getRenderSkyObject().setSkyRenderElement(ss);
			} catch (Exception e) {
				Logger.log(LEVEL.ERROR, "Error updating the chart. Message was: "+e.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e.getStackTrace()));
			}
			SwingUtilities.invokeLater(new Runnable() {
		        public void run() {
		        	paintIt();
		        }
			});
			return true;
		}
		return false;
	}

	/**
	 * Updates the chart.
	 * @param sky New sky object.
	 * @param time New time object.
	 * @param obs New observer object.
	 * @param eph New ephemeris object.
	 * @param object The object to center, or null.
	 * @throws JPARSECException If an error occurs.
	 */
	public void update(SkyRenderElement sky, TimeElement time, ObserverElement obs, EphemerisElement eph,
			String object) throws JPARSECException {
		obs.getGeoLat();
		skyRender.getRenderSkyObject().setSkyRenderElement(sky, time, obs, eph);
		fastField = sky.telescope.getField();
		vel = 1.5 * fastField / chart.width;
		paintIt();
		chart = sky.clone();
		this.time = time.clone();
		this.obs = obs.clone();
		this.eph = eph.clone();
		this.eph.correctEquatorialCoordinatesForRefraction = false;
		if (object != null)	setCentralObject(object, null);
	}

	/**
	 * Returns x position, without scaling.
	 * @return x position.
	 */
	public int getX() { return x; }
	/**
	 * Returns y position, without scaling.
	 * @return y position.
	 */
	public int getY() { return y; }

	/**
	 * Returns the width.
	 * @return width.
	 */
	public int getWidth() { return chart.width; }
	/**
	 * Returns the height.
	 * @return height.
	 */
	public int getHeight() { return chart.height; }

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

	private void setDefaults(int w, int h, SkyRendering sky)
	{
        try {
	    	// Ensure we don't stop program execution due to warnings
	    	JPARSECException.treatWarningsAsErrors(false);

	    	skyRender = sky;
	    	chart = sky.getRenderSkyObject().render;
	    	int c[] = Functions.getColorComponents(chart.background);
	    	if (c[3] == 0) chart.background = Functions.getColor(c[0], c[1], c[2], 255);
	    	showTextures = chart.planetRender.textures;
			showFastLabels = chart.drawFastLabels;
			showFilledMW = chart.fillMilkyWay;
			showTexturedMW = chart.drawMilkyWayContoursWithTextures;
			showTexturedHorizon = chart.drawHorizonTexture;
			fillNeb = chart.fillNebulae;
			showTexturedObj = chart.drawDeepSkyObjectsTextures;
			fastLines = chart.drawFastLinesMode;
			skyRender.getRenderSkyObject().render.drawFastLinesMode = FAST_LINES.NONE;
	    	if (chart.getColorMode().ordinal()<3) colorMode = chart.getColorMode().ordinal();
			fastField = chart.telescope.getField();
			vel = 1.5 * fastField / chart.width;
			time = sky.getTimeObject();
			obs = sky.getObserverObject();
			eph = sky.getEphemerisObject().clone();
			this.eph.correctEquatorialCoordinatesForRefraction = false;

			if (chart.telescope.ocular == null)
				chart.telescope.ocular = new OcularElement("eye", 20, 80 * Constant.DEG_TO_RAD, 32);

			//invertH = chart.telescope.invertHorizontal;
			//invertV = chart.telescope.invertVertical;

/*			// Evaluate system performance to determine if high quality rendering is reasonably
			long t0 = System.currentTimeMillis();
			int ncalc = 0;
			double z = 0;
			for (double y=-1000; y<=1E12; y=y+0.0001) {
				z += FastMath.atan2_accurate(y, 1);
				ncalc ++;
				long t1 = System.currentTimeMillis();
				double elapsed = (t1-t0)*0.001;
				if (elapsed > 1) break;
			}
			if (ncalc > 4E6) { // 8E6 in my desktop
				chart.planetRender.highQuality = true;
				Logger.log(LEVEL.WARNING, "You computer seems to be fast, so high quality planetary rendering has been enabled.");
			}
*/
        } catch ( Exception ve ) {
        	// Catch possible errors. In case of an error, the program stops and the message and stack
        	// trace error is shown with the following call
        	Logger.log(LEVEL.ERROR, "Error setting defaults values for the chart. Message was: "+ve.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(ve.getStackTrace()));
        }
	}

    // Mouse things
	double fastField, vel;
	boolean showTextures = true, showFilledMW = true, showTexturedObj = true;
	MILKY_WAY_TEXTURE showTexturedMW = MILKY_WAY_TEXTURE.NO_TEXTURE;
	HORIZON_TEXTURE showTexturedHorizon = HORIZON_TEXTURE.NONE;
	SUPERIMPOSED_LABELS showFastLabels = SUPERIMPOSED_LABELS.AVOID_SUPERIMPOSING_SOFT;
	FAST_LINES fastLines = FAST_LINES.NONE;
    private int lastMouseClickX = 0, lastMouseClickY = 0, ndrags = 0; //, lastMouseClickZ = 0;;
    private long lastMouseTime = -1;
    //private int lastMouseScreenPosX = -1, lastMouseScreenPosY = -1;
    private boolean dragging = false, increaseSpeed = true, speedTested = false, fillNeb = true;
    private boolean draggingLock = false;
    private long t0 = -1, nframes = 0;
    private static double horizonViewModeLatitudeLimit = 15.0 * Constant.DEG_TO_RAD, horizonViewModeFieldLimit = 120 * Constant.DEG_TO_RAD;
	/**
	 * Pans the chart around the sky.
	 */
	public void mouseDragged(MouseEvent e) {
/*		System.gc();
		double freeMB = Runtime.getRuntime().freeMemory() / (1024.0 * 1024.0);
		double totalMB = Runtime.getRuntime().totalMemory() / (1024.0 * 1024.0);
		System.out.println("MB used: "+(float)(totalMB-freeMB)+" / total MB: "+(float) totalMB);

		if (speedTested) {
			nframes ++;
			if (nframes > 200) {
				long t1 = System.currentTimeMillis();
				float fps = nframes/((t1-t0)*0.001f);
				System.out.println("fps: "+fps);
				t0 = t1;
				nframes = 0;
			}

		}
*/

		if (skyRender != null && e.getModifiersEx() == MouseEvent.BUTTON1_DOWN_MASK)
		{
			int dx = e.getX()-lastMouseClickX, dy = e.getY()-lastMouseClickY;

			// Workaround to allow zoom gesture with a finger on top of the leyend or the bottom of the 
			// chart, for touch devices
			if (!dragging && (e.getY() > getHeight() - skyRender.getRenderSkyObject().graphMarginY || 
					(e.getY() < skyRender.getRenderSkyObject().leyendMargin && skyRender.getRenderSkyObject().render.drawLeyend == LEYEND_POSITION.TOP))) {
				if (Math.abs(dx) > 1) zoom(-dx);
				lastMouseTime = -1;
				lastMouseClickX = e.getX();
				lastMouseClickY = e.getY();
				return;
			}
			
			double sc = Math.abs(FastMath.cos(Functions.normalizeRadians(skyRender.getRenderSkyObject().render.centralLatitude)));
			double moveAngleX = vel * dx * (chart.telescope.invertHorizontal? -1:1);
			double moveAngleY = vel * dy * (chart.telescope.invertVertical? -1:1);
			if (sc > 0.2
					&& skyRender.getRenderSkyObject().render.centralLatitude > Constant.PI_OVER_FOUR
					)
				moveAngleX /= sc;
			if (skyRender.getRenderSkyObject().render.coordinateSystem == COORDINATE_SYSTEM.HORIZONTAL) {
				moveAngleX = -moveAngleX;
				int ycenter = 0;
  				if (hMode && (skyRender.getRenderSkyObject().render.projection == Projection.PROJECTION.STEREOGRAPHICAL ||
  					skyRender.getRenderSkyObject().render.projection == Projection.PROJECTION.SPHERICAL) &&
  					skyRender.getRenderSkyObject().render.centralLatitude < horizonViewModeLatitudeLimit &&
  					fastField > horizonViewModeFieldLimit) // Horizon-view mode
  					ycenter = (skyRender.getRenderSkyObject().render.height-100)/2;
  				try {
					skyRender.getRenderSkyObject().setYCenterOffset(ycenter);
				} catch (Exception e1) {
     				Logger.log(LEVEL.ERROR, "Error setting center y value. Message was: "+e1.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e1.getStackTrace()));
				}
			}
			skyRender.getRenderSkyObject().render.centralLongitude += moveAngleX;
			skyRender.getRenderSkyObject().render.centralLatitude += moveAngleY;
			if (skyRender.getRenderSkyObject().render.centralLatitude > Constant.PI_OVER_TWO)
				skyRender.getRenderSkyObject().render.centralLatitude = Constant.PI_OVER_TWO;
			if (skyRender.getRenderSkyObject().render.centralLatitude < -Constant.PI_OVER_TWO)
				skyRender.getRenderSkyObject().render.centralLatitude = -Constant.PI_OVER_TWO;
			if (skyRender.getRenderSkyObject().render.centralLongitude > Constant.TWO_PI)
				skyRender.getRenderSkyObject().render.centralLongitude -= Constant.TWO_PI;
			if (skyRender.getRenderSkyObject().render.centralLongitude < 0)
				skyRender.getRenderSkyObject().render.centralLongitude += Constant.TWO_PI;
			chart.centralLongitude = skyRender.getRenderSkyObject().render.centralLongitude;
			chart.centralLatitude = skyRender.getRenderSkyObject().render.centralLatitude;
			lastMouseClickX = e.getX();
			lastMouseClickY = e.getY();
			if (!dragging) {
				skyRender.getRenderSkyObject().SaveObjectsToAllowSearch = false;
				if (chartForDragging == null) {
					//if (fastField > 10 * Constant.DEG_TO_RAD) skyRender.getRenderSkyObject().render.planetRender.textures = false;
					skyRender.getRenderSkyObject().render.drawObjectsLimitingMagnitude = chart.drawObjectsLimitingMagnitude;
					if (increaseSpeed) {
						if (!skyRender.getRenderSkyObject().render.planetRender.highQuality || RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR < 1.5f) {
							skyRender.getRenderSkyObject().render.drawFastLabels = SUPERIMPOSED_LABELS.FAST;
							//skyRender.getRenderSkyObject().render.fillMilkyWay = false;
							skyRender.getRenderSkyObject().render.drawFastLinesMode = fastLines;
						}
						skyRender.getRenderSkyObject().render.drawMilkyWayContoursWithTextures = MILKY_WAY_TEXTURE.NO_TEXTURE;
						skyRender.getRenderSkyObject().render.drawHorizonTexture = HORIZON_TEXTURE.NONE;
						// It is generaly faster to fill the nebula
						//skyRender.getRenderSkyObject().render.fillNebulae = false;
						skyRender.getRenderSkyObject().resetLeyend(false);
					}
				} else {
					try {
						updateChartForDragging();
						skyRender.getRenderSkyObject().setSkyRenderElement(chartForDragging);
						if (!dragging) skyRender.getRenderSkyObject().setStarsLimitingMagnitude();
					} catch (Exception exc) {}
				}
			}
			dragging = true;
			paintImage();
			if (ndrags > 10) {
				lastMouseTime = -1;
			} else {
				ndrags ++;				
			}

			if (!speedTested && fastField > 50 * Constant.DEG_TO_RAD) {
				if (t0 == -1) {
					t0 = System.currentTimeMillis();
				} else {
					long t1 = System.currentTimeMillis();
					nframes ++;
					double elapsed = (t1-t0) * 0.001;
					double fps = nframes / elapsed;
					if (elapsed > 3) {
						speedTested = true;
						if (fps < 5) increaseSpeed = true;
						nframes = 0;
					}
				}
			}
		}
  	}

	/**
	 * Support for mouse dragging if it is locked.
	 */
	public void mouseMoved(MouseEvent me) {
		if (menu != null) menu = null;

  		if (draggingLock && me != null) {
			int dx = me.getX()-lastMouseClickX, dy = me.getY()-lastMouseClickY;
			if (skyRender != null)
			{
				double moveAngleX = vel * dx * (chart.telescope.invertHorizontal? -1:1);
				double moveAngleY = vel * dy * (chart.telescope.invertVertical? -1:1);
				if (skyRender.getRenderSkyObject().render.coordinateSystem == COORDINATE_SYSTEM.HORIZONTAL) {
					moveAngleX = -moveAngleX;
					int ycenter = 0;
  	  				if (hMode && (skyRender.getRenderSkyObject().render.projection == Projection.PROJECTION.STEREOGRAPHICAL ||
  	  					skyRender.getRenderSkyObject().render.projection == Projection.PROJECTION.SPHERICAL) &&
  	  					skyRender.getRenderSkyObject().render.centralLatitude < horizonViewModeLatitudeLimit &&
  	  					fastField > horizonViewModeFieldLimit) // Horizon-view mode
  	  					ycenter = (skyRender.getRenderSkyObject().render.height-100)/2;
  	  				try {
						skyRender.getRenderSkyObject().setYCenterOffset(ycenter);
					} catch (Exception e1) {
         				Logger.log(LEVEL.ERROR, "Error setting center y value. Message was: "+e1.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e1.getStackTrace()));
					}
				}
				skyRender.getRenderSkyObject().render.centralLongitude += moveAngleX;
				skyRender.getRenderSkyObject().render.centralLatitude += moveAngleY;
				if (skyRender.getRenderSkyObject().render.centralLatitude > Constant.PI_OVER_TWO)
					skyRender.getRenderSkyObject().render.centralLatitude = Constant.PI_OVER_TWO;
				if (skyRender.getRenderSkyObject().render.centralLatitude < -Constant.PI_OVER_TWO)
					skyRender.getRenderSkyObject().render.centralLatitude = -Constant.PI_OVER_TWO;
  				if (skyRender.getRenderSkyObject().render.centralLongitude > Constant.TWO_PI)
  					skyRender.getRenderSkyObject().render.centralLongitude -= Constant.TWO_PI;
  				if (skyRender.getRenderSkyObject().render.centralLongitude < 0)
  					skyRender.getRenderSkyObject().render.centralLongitude += Constant.TWO_PI;
  				chart.centralLongitude = skyRender.getRenderSkyObject().render.centralLongitude;
  				chart.centralLatitude = skyRender.getRenderSkyObject().render.centralLatitude;
				lastMouseClickX = me.getX();
				lastMouseClickY = me.getY();
				if (!dragging) {
					skyRender.getRenderSkyObject().SaveObjectsToAllowSearch = false;
					if (this.chartForDragging == null) {
						//if (fastField > 10 * Constant.DEG_TO_RAD) skyRender.getRenderSkyObject().render.planetRender.textures = false;
						skyRender.getRenderSkyObject().render.drawObjectsLimitingMagnitude = chart.drawObjectsLimitingMagnitude;
						if (increaseSpeed) {
							if (!skyRender.getRenderSkyObject().render.planetRender.highQuality || RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR < 1.5f) {
								skyRender.getRenderSkyObject().render.drawFastLabels = SUPERIMPOSED_LABELS.FAST;
								skyRender.getRenderSkyObject().render.fillMilkyWay = false;
								skyRender.getRenderSkyObject().render.drawFastLinesMode = fastLines;
							}
							skyRender.getRenderSkyObject().render.drawMilkyWayContoursWithTextures = MILKY_WAY_TEXTURE.NO_TEXTURE;
							skyRender.getRenderSkyObject().render.drawHorizonTexture = HORIZON_TEXTURE.NONE;
							skyRender.getRenderSkyObject().resetLeyend(false);
						}
					} else {
						try {
							updateChartForDragging();
							skyRender.getRenderSkyObject().setSkyRenderElement(chartForDragging);
						} catch (Exception exc) {}
					}
				}
				dragging = true;
  				paintImage();
			}
		} else {
			int x = 0, y = 0;
			if (me != null) {
				if (me.getClickCount() > 0) return;
				x = me.getX();
				y = me.getY();
			}
			if (skyRender.getRenderSkyObject().render.anaglyphMode == ANAGLYPH_COLOR_MODE.TRUE_3D_MODE_LEFT_RIGHT_HALF_WIDTH)
				x *= 2;

			Graphics g = panel.getGraphics();
			String msg1 = "";
			int ndec = 3;
			double res = 2 * fastField * Constant.RAD_TO_DEG / skyRender.getRenderSkyObject().render.width;
			if (res > 0.01) {
				ndec = 2;
				if (res > 0.1) {
					ndec = 1;
					if (res > 1) ndec = 0;
				}
			}
			int ndeceq = 3;
			res = res * Constant.RAD_TO_ARCSEC / Constant.RAD_TO_DEG;
			if (res > 0.01) {
				ndeceq = 2;
				if (res > 0.1) {
					ndeceq = 1;
					if (res > 1) {
						ndeceq = 0;
						if (res > 10) {
							ndeceq = -1;
							if (res > 60) ndeceq = -2;
						}
					}
				}
			}

			TARGET target = TARGET.NOT_A_PLANET;
			double ppd = skyRender.getRenderSkyObject().render.width / (fastField * Constant.RAD_TO_DEG * 2);
			if (ppd > 12 && me != null) {
				try {
					target = skyRender.getRenderSkyObject().getPlanetInScreenCoordinates(x, y, true, minimumSize);
				} catch (Exception exc) {}
			}
			if (me != null) {
				if (target == TARGET.NOT_A_PLANET) {
					Object data[] = null;
					try {
						if (ppd > 12) {
							data = skyRender.getRenderSkyObject().getClosestObjectData(x, y, false, false);
							RenderSky.OBJECT type = (OBJECT) data[0];
							if (type != OBJECT.PLANET)
								data = skyRender.getRenderSkyObject().getClosestObjectData(x, y, true, false);
						} else {
							data = skyRender.getRenderSkyObject().getClosestObjectData(x, y, true, false);
						}
					} catch (Exception exc) {}
					if (data != null) {
						RenderSky.OBJECT type = (OBJECT) data[0];
						if (type == OBJECT.DEEPSKY || type == OBJECT.SUPERNOVA || type == OBJECT.NOVA 
								|| type == OBJECT.METEOR_SHOWER) {
							String d[] = (String[]) data[2];
							msg1 = d[0];
							String t = Translate.translate(877);
							if (type == OBJECT.METEOR_SHOWER) t = Translate.translate(1024);
							if (type == OBJECT.DEEPSKY) {
								t = Translate.translate(972);
					  			if (d.length <= 4) t = Translate.translate(79);
							} 
							if (DataSet.isDoubleStrictCheck(d[3])) {
								double mag = Double.parseDouble(d[3]);
								if (type == OBJECT.METEOR_SHOWER) {
									String rate = "var";
									if (mag > 0) rate = ""+(int) mag;
									msg1 += " ("+Translate.translate(1023)+" "+rate+")";							 
								} else {
									if (mag < 99) msg1 += " ("+Functions.formatValue(mag, 1)+"m)";
								}
							}
							if (type == OBJECT.NOVA) t = Translate.translate(1304);
							msg1 = msg1 + " - " + t.toLowerCase();
						} else {
							EphemElement ephem = (EphemElement) data[2];
							msg1 = ephem.name;
							if (type != OBJECT.PROBE) msg1 += " ("+Functions.formatValue(ephem.magnitude, 1)+"m)";
							if (type == OBJECT.STAR) {
								String n = null;
								try {
									n = skyRender.getRenderSkyObject().getStarProperName(ephem.name);
								} catch (Exception exc) {}
								if (n != null) {
									msg1 = n;
								} else {
									int b = msg1.indexOf("(");
									if (b > 0) msg1 = msg1.substring(0, b);
								}
								msg1 += " ("+Functions.formatValue(ephem.magnitude, 1)+"m) - "+Translate.translate(79).toLowerCase();
							} else {
								String st = Translate.translate(878); // SS Obj
								if (type == RenderSky.OBJECT.ARTIFICIAL_SATELLITE) st = Translate.translate(78);
		  	  					if (type == RenderSky.OBJECT.ASTEROID) st = Translate.translate(73);
		  	  					if (type == RenderSky.OBJECT.COMET) st = Translate.translate(74);
		  	  					if (type == RenderSky.OBJECT.NEO) st = Translate.translate(1275);
		  	  					if (type == RenderSky.OBJECT.PROBE) st = Translate.translate(76);
		  	  					if (type == RenderSky.OBJECT.TRANSNEPTUNIAN) st = Translate.translate(1003);

								msg1 += " - "+st.toLowerCase();
							}
							try {
								TARGET t = TARGET.NOT_A_PLANET;
								if (type == OBJECT.PLANET) t = Target.getID(ephem.name);
								if (t.isNaturalSatellite() && type == OBJECT.PLANET) {
									EphemerisElement ephC = eph.clone();
									ephC.targetBody = t.getCentralBody();
									MoonEphemElement m[] = null;
									if (obs.getMotherBody() == TARGET.EARTH) {
										m = skyRender.getRenderSkyObject().render.planetRender.moonephem;
									} else {
										m = MoonEphem.calcAllSatellites(time, obs, ephC, skyRender.getRenderSkyObject().render.planetRender.satellitesAll);
									}
							   	    if (m != null) {
							   	    	String msg = "", sep = ", ";
										for (int i=0; i<m.length; i++) {
											if (m[i] != null && m[i].name.equals(ephem.name)) {
												if (!m[i].mutualPhenomena.trim().equals(""))
													msg += m[i].name+" "+m[i].mutualPhenomena + sep;
												if (m[i].transiting)
													msg += Translate.translate(165) + sep;
												if (m[i].shadowTransiting) {
													try {
														if (obs.getMotherBody() != TARGET.EARTH) {
															double sunAngR = FastMath.atan2_accurate(TARGET.SUN.equatorialRadius, m[i].distanceFromSun);
															TARGET sat = Target.getID(m[i].name);
															ephC.targetBody = sat.getCentralBody();
															EphemElement planetEphem = Ephem.getEphemeris(time, obs, ephC, false);
															double satAngR = FastMath.atan2_accurate(sat.equatorialRadius, LocationElement.getLinearDistance(planetEphem.getEquatorialLocation(), m[i].getEquatorialLocation()));
															if (sunAngR > satAngR) {
																msg += Translate.translate(1084) + sep;
															} else {
																msg += Translate.translate(166) + sep;
															}
														} else {
															msg += Translate.translate(166) + sep;
														}
													} catch (Exception exc) {
														msg += Translate.translate(166) + sep;
													}
												}
												if (m[i].eclipsed)
													msg += Translate.translate(163) + sep;
												if (m[i].occulted)
													msg += Translate.translate(164) + sep;
												break;
											}
										}
										if (!msg.equals("")) msg1 = ephem.name + " ("+msg.substring(0, msg.lastIndexOf(",")).toLowerCase()+")";
							   	    }
								}
							} catch (Exception exc) {}
						}
					}
				} else {
					LocationElement loc = null;
					try {
						loc = skyRender.getRenderSkyObject().getPlanetographicPosition(x, y, 3, true);
					} catch (Exception exc) {}
					msg1 = target.getName();

					if (loc != null) {
						String feature = RenderPlanet.identifyFeature(loc, target, 2); // Closest feature within 2 deg
						if (feature == null) {
							feature = "";
							if (target == TARGET.JUPITER) {
								LocationElement locGRS = new LocationElement(
										skyRender.getRenderSkyObject().getJupiterGRSLongitude(3),
										-22.2 * Constant.DEG_TO_RAD, 1);
								double d = LocationElement.getApproximateAngularDistance(loc, locGRS);
								if (d < 3 * Constant.DEG_TO_RAD) {
									feature = Translate.translate(1070);
								}
								if (!feature.equals("")) feature = " ("+feature.trim()+")";
							}
						} else {
							feature = feature.substring(0, feature.indexOf(") ")+1); // Remove lon, lat info
							feature = " ("+feature.trim()+")";
						}
						if (target == TARGET.JUPITER || target == TARGET.SATURN) // || target == TARGET.URANUS || target == TARGET.NEPTUNE)
							msg1 += " ("+t317+" III)";


						String s = "", s2 = "";

						// Comment this section to use negative longitudes/latitudes. Seems better to give E/W and N/S.
						if (loc.getLongitude() < 0 || loc.getLongitude() > Constant.TWO_PI)
							loc.setLongitude(Functions.normalizeRadians(loc.getLongitude()));
						s = " E";
						s2 = " N";
						if (target.compareTo(TARGET.EARTH) <= 0 || target == TARGET.Moon) {
							if (loc.getLongitude() > Math.PI || loc.getLongitude() < 0) {
								s = " W";
								loc.setLongitude(Constant.TWO_PI-loc.getLongitude());
								if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH) s = " O";
							}
						} else {
							s = " W";
							if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH) s = " O";
							if (loc.getLongitude() > Math.PI || loc.getLongitude() < 0) {
								s = " E";
								loc.setLongitude(Constant.TWO_PI-loc.getLongitude());
							}
						}
						if (loc.getLatitude() < 0) s2 = " S";


						msg1 +=": "+Functions.formatAngleAsDegrees(Math.abs(loc.getLongitude()), ndec)+"\u00b0"+s+", "+Functions.formatAngleAsDegrees(Math.abs(loc.getLatitude()), ndec)+"\u00b0"+s2+feature;
						//msg1 +=": "+Functions.formatDEC(Math.abs(loc.getLongitude()), ndec)+"\u00b0"+s+", "+Functions.formatDEC(Math.abs(loc.getLatitude()), ndec)+"\u00b0"+s2+feature;
					}
				}
				if (msg1.equals("")) {
					msg1 = obs.getName()+", "+time.toString();
					if (time.timeScale == SCALE.LOCAL_TIME) msg1 = msg1.substring(0, msg1.lastIndexOf(" ")) + " " + time.getTimeScale();
					if (obs.getMotherBody() != TARGET.EARTH) msg1 += " ("+Translate.translate(1079)+")";
				}
			} else {
				msg1 = obs.getName()+", "+time.toString();
				if (time.timeScale == SCALE.LOCAL_TIME) msg1 = msg1.substring(0, msg1.lastIndexOf(" ")) + " " + time.getTimeScale();
				if (obs.getMotherBody() != TARGET.EARTH) msg1 += " ("+Translate.translate(1079)+")";
			}

			String msg2 = "";
			if (this.object != null && !this.object.equals("")) msg2 = "["+object+"] ";
			if (me != null) {
				LocationElement loc = null;
				try {
					loc = skyRender.getRenderSkyObject().getSkyLocation(x, y);
				} catch (Exception exc) {}
		  		if (!eph.isTopocentric || eph.ephemType != EphemerisElement.COORDINATES_TYPE.APPARENT || eph.equinox != EphemerisElement.EQUINOX_OF_DATE) {
			  		if (eph.ephemType != EphemerisElement.COORDINATES_TYPE.APPARENT) {
			  			msg2 += "["+coordt[eph.ephemType.ordinal()]+"] ";
			  		}
		  			if (!eph.isTopocentric) msg2 += "[Geo] ";
		  			if (eph.equinox != EphemerisElement.EQUINOX_OF_DATE) {
		  				if (eph.equinox == EphemerisElement.EQUINOX_J2000) {
		  					msg2 += "[J2000] ";
		  				} else {
		  					msg2 += "[eq"+eph.equinox+"] ";
		  				}
		  			}
		  		}
				if (chart.drawSkyCorrectingLocalHorizon) msg2 += "[R] ";
				if (chart.telescope.invertHorizontal && !chart.telescope.invertVertical) msg2 = msg2.trim() + "[H] ";
				if (chart.telescope.invertVertical) {
					if (!chart.telescope.invertHorizontal) {
						msg2 = msg2.trim() + "[V] ";
					} else {
						msg2 = msg2.trim() + "[HV] ";
					}
				}

				if (!msg2.equals("")) msg2 += "/ ";
				if (loc != null) {
					if (Double.isNaN(loc.getLongitude()) || Double.isInfinite(loc.getLongitude())) {
						msg2 += ""+x+" px, "+y+" px";
					} else {
						if (skyRender.getRenderSkyObject().render.coordinateSystem == COORDINATE_SYSTEM.EQUATORIAL) {
							if (ndeceq < 0) {
								int nd = 1;
								if (ndeceq < -1) nd = 0;
								msg2 += ""+x+" px, "+y+" px / " +Functions.formatRAOnlyMinutes(loc.getLongitude(), nd+1)+", "+Functions.formatDECOnlyMinutes(loc.getLatitude(), nd);
							} else {
								msg2 += ""+x+" px, "+y+" px / " +Functions.formatRA(loc.getLongitude(), ndeceq+1)+", "+Functions.formatDEC(loc.getLatitude(), ndeceq);
							}
						} else {
							//msg2 += ""+x+" px, "+y+" px / " +Functions.formatAngleAsDegrees(loc.getLongitude(), ndec)+"\u00b0, "+Functions.formatAngleAsDegrees(loc.getLatitude(), ndec)+"\u00b0";
							if (ndeceq < 0) {
								int nd = 1;
								if (ndeceq < -1) nd = 0;
								msg2 += ""+x+" px, "+y+" px / " +Functions.formatDECOnlyMinutes(loc.getLongitude(), nd)+", "+Functions.formatDECOnlyMinutes(loc.getLatitude(), nd);
							} else {
								msg2 += ""+x+" px, "+y+" px / " +Functions.formatDEC(loc.getLongitude(), ndec)+", "+Functions.formatDEC(loc.getLatitude(), ndec);
							}
						}
					}
				}
			}

			int s = 10 + skyRender.getRenderSkyObject().render.drawCoordinateGridFont.getSize() - 14;
			if (chart.width >= 3000) {
	        	int hugeFactor = Math.max((chart.width/1000) - 2, 4);
	        	s = 10 + hugeFactor * 2; 
			}
        	if (s > 35) s = 35;
			if (g != null) {
				g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, s));
				int x0 = 5;
				int w1 = chart.width/3+2*x0, h = s+3; //g.getFontMetrics().stringWidth(msg1)+2*x0, h = 18;
				int w2 = chart.width/3+2*x0; //g.getFontMetrics().stringWidth(msg2)+2*x0;
				int y0 = chart.height - h;
				int w0 =  chart.width;
				g.setColor(new Color(skyRender.getRenderSkyObject().render.background));
				g.fillRect(0, y0, w1, h);
				g.fillRect(w0-w2, y0, w2, h);
				g.setColor(new Color(skyRender.getRenderSkyObject().render.drawCoordinateGridColor));
				y0 += (h + s) / 2;
				TextLabel tl = new TextLabel(msg1);
				tl.draw(g, x0, y0);
				//g.drawString(msg1, x0, y0);
				w2 = g.getFontMetrics().stringWidth(msg2)+2*x0;
				g.drawString(msg2, w0-w2+x0, y0);
			}
		}
	}

	/**
	 * Support for dragging lock and popup menu.
	 */
	public void mouseClicked(MouseEvent m) {
 		if (skyRender == null) return;

 		int b = m.getButton();
 		int nc = m.getClickCount();
 		if (nc <= 2 && !MouseEvent.getModifiersExText(m.getModifiersEx()).equals("Ctrl")) {
 			try {
	 			int x = m.getX(), y = m.getY();
				if (skyRender.getRenderSkyObject().render.anaglyphMode == ANAGLYPH_COLOR_MODE.TRUE_3D_MODE_LEFT_RIGHT_HALF_WIDTH)
					x *= 2;
	 	    	String s = null;
 				if (b == MouseEvent.BUTTON1) {
		 	  		Object data[] = skyRender.getRenderSkyObject().getClosestObjectData(x, y, false, false);
		 	  		if (data != null) {
		 		  		RenderSky.OBJECT id = (RenderSky.OBJECT) data[0];
		 		  		if (id == RenderSky.OBJECT.STAR)
		 		  			data = skyRender.getRenderSkyObject().getClosestObjectData(x, y, true, false);
		 		  		if (data != null) {
		 			  		id = (RenderSky.OBJECT) data[0];
		 			  		EphemElement ephem = null;
		 			  		StarElement star = null;
		 			  		if (id == RenderSky.OBJECT.DEEPSKY) {
		 			  			s = ((String[]) data[2])[0];
		 			  		} else {
		 			  			if (id == RenderSky.OBJECT.SUPERNOVA || id == OBJECT.NOVA || id == OBJECT.METEOR_SHOWER) {
			 			  			s = ((String[]) data[2])[0];
		 			  			} else {
		 			  				ephem = (EphemElement) data[2];
		 		  		  			s = ephem.name;
		 			  				if (id == RenderSky.OBJECT.STAR) {
		 			  					star = (StarElement) data[3];
		 			  		  			s = star.name;
		 			  		  			String s2 = skyRender.getRenderSkyObject().getStarProperName(star.name);
		 			  		  			if (s2 != null) s = s2;
		 			  				}
		 			  			}
		 			  		}
		 		  		}
		 	  		}
		 	  		if (nc > 1 && s != null) {
		  				setCentralObject(s, data);
		  				paintImage();
		 	  		} else {
		 	  			Object objData = data[2];
		 	  			String newObj = "";
		 	  			LocationElement newLoc = null;
		 	  			if (objData instanceof EphemElement) {
		 	  				newObj = ((EphemElement) objData).name;
		 	  				newLoc = ((EphemElement) objData).getEquatorialLocation();
		 	  			} else {
		 	  				String dd[] = (String[]) objData;
		 	  				newObj = dd[0];
		 	  				newLoc = new LocationElement(Functions.parseRightAscension(dd[1]), Functions.parseDeclination(dd[2]), 1);
		 	  			}
	  	  				if (lastObj != null && lastLoc != null && !lastObj.equals(newObj)) {
	  	  					identifyAndGetData(x, y);
	  	  					String d = Functions.formatAngle(LocationElement.getAngularDistance(newLoc, lastLoc), 3);
		  	  				int ss = 10;
			  	  			Graphics2D g = (Graphics2D) panel.getGraphics();
			  	  			g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, ss));
			  	  			int w1 = chart.width/3+2*x0, h = ss+3; //g.getFontMetrics().stringWidth(msg1)+2*x0, h = 18;
			  	  			int w2 = chart.width/3+2*x0; //g.getFontMetrics().stringWidth(msg2)+2*x0;
			  	  			int y0 = chart.height - h;
			  	  			int w0 =  chart.width;
			  	  			g.setColor(new Color(skyRender.getRenderSkyObject().render.background));
			  	  			g.fillRect(0, y0, w1, h);
			  	  			g.fillRect(w0-w2, y0, w2, h);
			  	  			g.setColor(new Color(skyRender.getRenderSkyObject().render.drawCoordinateGridColor));
			  	  			y0 += (h + ss) / 2;
			  	  			g.drawString(Translate.translate(299)+" ("+lastObj+")-("+newObj+"): "+d, 5, y0);
	  	  				}
	  	  				lastObj = newObj;
	  	  				lastLoc = newLoc;
		 	  		}
	 	  		} else {
  					boolean considerSatellites = true;
  					TARGET target = skyRender.getRenderSkyObject().getPlanetInScreenCoordinates(x, y, considerSatellites, minimumSize);
  	   				String popupText = "";
  	  				if (target != TARGET.NOT_A_PLANET) {
	  					LocationElement loc0 = skyRender.getRenderSkyObject().getPlanetographicPosition(x, y, 3, considerSatellites);
	  					String object = target.getName();
	  					String feature = RenderPlanet.identifyFeature(loc0, target, 2); // Closest feature within 2 deg
	  					if (loc0 != null) {
		  					if (feature != null) {
		  						popupText = replaceVars(Translate.translate(883), new String[] {"%feature", "%object", "%x", "%y", "%lon", "%lat"}, new String[] {""+feature, ""+object, ""+x, ""+y, Functions.formatAngle(loc0.getLongitude(), 1), Functions.formatAngle(loc0.getLatitude(), 1)});
		  					} else {
		  						popupText = replaceVars(Translate.translate(884), new String[] {"%x", "%y", "%lon", "%lat"}, new String[] {""+x, ""+y, Functions.formatAngle(loc0.getLongitude(), 1), Functions.formatAngle(loc0.getLatitude(), 1)});
		  					}
		  	  				popupText += FileIO.getLineSeparator() + FileIO.getLineSeparator();
	  					}
  	  				}
  	  				popupText += identifyAndGetData(x, y);
	  				if (!popupText.equals("")) {
	  					showPopup(s, popupText);
	  				}
	 	  		}
 			} catch (Exception exc) {}
 		}

		if (dragging && b == MouseEvent.BUTTON1) {
			// Reset mouse listener to avoid triggering object identification process above
			dragging = false;
			skyRender.getRenderSkyObject().SaveObjectsToAllowSearch = true;
			if (this.chartForDragging != null) {
				try {
					skyRender.getRenderSkyObject().setSkyRenderElement(chart);
				} catch (Exception exc) {}
			} else {
				skyRender.getRenderSkyObject().render.planetRender.textures = showTextures;
				skyRender.getRenderSkyObject().render.drawObjectsLimitingMagnitude = -chart.drawObjectsLimitingMagnitude;
				if (showTextures) {
					chart.drawMilkyWayContoursWithTextures = showTexturedMW;
					chart.drawHorizonTexture = showTexturedHorizon;
					chart.drawDeepSkyObjectsTextures = showTexturedObj;
					skyRender.getRenderSkyObject().render.drawMilkyWayContoursWithTextures = chart.drawMilkyWayContoursWithTextures;
					skyRender.getRenderSkyObject().render.drawHorizonTexture = chart.drawHorizonTexture;
				}
				if (increaseSpeed) {
					skyRender.getRenderSkyObject().render.drawFastLabels = showFastLabels;
					skyRender.getRenderSkyObject().render.fillMilkyWay = showFilledMW;
					skyRender.getRenderSkyObject().resetLeyend(false);
					skyRender.getRenderSkyObject().render.fillNebulae = fillNeb;
				}
			}
			paintIt();
			return;
		}
  		if (b == MouseEvent.BUTTON3) {
  			if (draggingLock) {
  				draggingLock = false;
  			} else {
  				try {
  					int px = m.getX();
  					if (skyRender.getRenderSkyObject().render.anaglyphMode == ANAGLYPH_COLOR_MODE.TRUE_3D_MODE_LEFT_RIGHT_HALF_WIDTH)
  						px *= 2;
  					this.modifyRendering(px, m.getY());
  				} catch (Exception exc) {
     				Logger.log(LEVEL.ERROR, "Error editing the rendering values. Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
  				}
  			}
  		}
  		if (b == MouseEvent.BUTTON1 && showModifyLocTime && MouseEvent.getModifiersExText(m.getModifiersEx()).equals("Ctrl")) {
  			if (m.getClickCount() == 2) {
  				try {
	  				double pos[] = new double[] {0, 0, 1.0, 0.0, 0.0, 0.0};
	  				posEQ = Ephem.eclipticToEquatorial(pos, TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME), eph);

	  				//updateTime = updateTime1;
	  				//updateTimer();

					this.obs = ObserverElement.parseExtraterrestrialObserver(new ExtraterrestrialObserverElement(Translate.translate(1083), posEQ));
					if (skyRender.getRenderSkyObject().render.telescope.ocular != null) skyRender.getRenderSkyObject().render.telescope.ocular.focalLength = TelescopeElement.getOcularFocalLengthForCertainField(Math.PI, skyRender.getRenderSkyObject().render.telescope);
					if (skyRender.getRenderSkyObject().render.coordinateSystem == COORDINATE_SYSTEM.HORIZONTAL) skyRender.getRenderSkyObject().render.coordinateSystem = COORDINATE_SYSTEM.ECLIPTIC;
					skyRender.getRenderSkyObject().render.background = 255<<24 | 5<<16 | 5<<8 | 12;
  					if (obs.getMotherBody() != TARGET.EARTH) skyRender.getRenderSkyObject().render.background = 255<<24 | 5<<16 | 5<<8 | 5;
					skyRender.getRenderSkyObject().setSkyRenderElement(skyRender.getRenderSkyObject().render, time, obs, eph);
					if (object != null) {
		  				paintIt();
						setCentralObject(object, null);
					}
	  				paintIt();
				} catch (Exception exc) {}
  			} else {
  				//updateTime = updateTime0;
  				//updateTimer();

				LocationElement loc = null;
				int x = m.getX(), y = m.getY();
				if (skyRender.getRenderSkyObject().render.anaglyphMode == ANAGLYPH_COLOR_MODE.TRUE_3D_MODE_LEFT_RIGHT_HALF_WIDTH)
					x *= 2;
				try {
					loc = skyRender.getRenderSkyObject().getPlanetographicPosition(x, y, 3, true);
				} catch (Exception exc) {}
				if (loc != null) {
					TARGET newMother = null;
					try {
						newMother = skyRender.getRenderSkyObject().getPlanetInScreenCoordinates(x, y, true, minimumSize);
					} catch (JPARSECException e) {	}
					if (newMother != null && newMother != TARGET.NOT_A_PLANET) {
						try {
							String locName = RenderPlanet.identifyFeature(loc, newMother, 5);
							if (locName == null) {
								locName = newMother.getName()+" (lon: "+Functions.formatDEC(loc.getLongitude())+", lat: "+Functions.formatDEC(loc.getLatitude())+")";
							} else {
								locName = newMother.getName()+" ("+locName.substring(0, locName.indexOf("(")).trim()+")";
							}
							loc.setRadius(0.0);
							this.obs = ObserverElement.parseExtraterrestrialObserver(new ExtraterrestrialObserverElement(locName, newMother, loc));
							if (skyRender.getRenderSkyObject().render.telescope.ocular != null) skyRender.getRenderSkyObject().render.telescope.ocular.focalLength = TelescopeElement.getOcularFocalLengthForCertainField(Math.PI, skyRender.getRenderSkyObject().render.telescope);
							skyRender.getRenderSkyObject().render.background = 255<<24 | 5<<16 | 5<<8 | 12;
							if (obs.getMotherBody() != TARGET.EARTH) skyRender.getRenderSkyObject().render.background = 255<<24 | 5<<16 | 5<<8 | 5;
							skyRender.getRenderSkyObject().setSkyRenderElement(skyRender.getRenderSkyObject().render, time, obs, eph);
							if (object != null) {
				  				paintIt();
								setCentralObject(object, null);
							}
			  				paintIt();
						} catch (JPARSECException e) {	}
					}
				} else {
					// Check for comet/asteroid
					Object[] newMother = null;
					try {
						newMother = skyRender.getRenderSkyObject().getClosestObjectData(x, y, true, false);
					} catch (JPARSECException e) {	}
					if (newMother != null) {
						OBJECT type = (OBJECT) newMother[0];
						if (type == OBJECT.ASTEROID || type == OBJECT.COMET || type == OBJECT.NEO) {
							EphemElement ephem = (EphemElement) newMother[2];
							try {
								String locName = ephem.name;
								EphemerisElement ephC = eph.clone();
								ephC.ephemType = COORDINATES_TYPE.GEOMETRIC;
								ephC.algorithm = ALGORITHM.ORBIT;
								ephC.isTopocentric = false;
								TARGET target = TARGET.Comet;
								if (type == OBJECT.ASTEROID) {
									target = TARGET.Asteroid;
									target.setIndex(OrbitEphem.getIndexOfAsteroid(ephem.name));
									ephC.orbit = OrbitEphem.getOrbitalElementsOfAsteroid(target.getIndex());
								} else {
									if (type == OBJECT.NEO) {
										target = TARGET.NEO;
										target.setIndex(OrbitEphem.getIndexOfNEO(ephem.name));
										ephC.orbit = OrbitEphem.getOrbitalElementsOfNEO(target.getIndex());
									} else {
										target.setIndex(OrbitEphem.getIndexOfComet(ephem.name));
										ephC.orbit = OrbitEphem.getOrbitalElementsOfComet(target.getIndex());
									}
								}
								double jd = TimeScale.getJD(time, obs, ephC, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
								double[] out = Ephem.eclipticToEquatorial(OrbitEphem.obtainPosition(time, obs, ephC), ephC.getEpoch(jd), eph);

								this.obs = ObserverElement.parseExtraterrestrialObserver(new ExtraterrestrialObserverElement(locName, out));
								if (skyRender.getRenderSkyObject().render.telescope.ocular != null) skyRender.getRenderSkyObject().render.telescope.ocular.focalLength = TelescopeElement.getOcularFocalLengthForCertainField(Math.PI, skyRender.getRenderSkyObject().render.telescope);
								skyRender.getRenderSkyObject().render.background = 255<<24 | 5<<16 | 5<<8 | 12;
								if (obs.getMotherBody() != TARGET.EARTH) skyRender.getRenderSkyObject().render.background = 255<<24 | 5<<16 | 5<<8 | 5;
								skyRender.getRenderSkyObject().setSkyRenderElement(skyRender.getRenderSkyObject().render, time, obs, eph);
								if (object != null) {
					  				paintIt();
									setCentralObject(object, null);
								}
				  				paintIt();
							} catch (Exception e) { }
						}
					}
				}
  			}
  		}
	}

	/**
	 * Nothing.
	 */
	public void mouseEntered(MouseEvent arg0) {
	}

	/**
	 * Nothing (almost).
	 */
	public void mouseExited(MouseEvent arg0) {
    	// Draw rendered image using double buffer capabilities.
		SwingUtilities.invokeLater(new Runnable() {
            public void run() {
    			mouseMoved(null);
            }
        });
	}

	/**
	 * Updates last position of mouse.
	 */
	public void mousePressed(MouseEvent e) {
		lastMouseClickX = e.getX();
		lastMouseClickY = e.getY();
		ndrags = 0;
		if (e.getButton() == MouseEvent.BUTTON1) lastMouseTime = System.currentTimeMillis();
	}

	/**
	 * Selects if lock dragging should appear in the pop up menu.
	 * @param b True or false.
	 */
	public void setShowLockDragging(boolean b) {
		showLockDrag = b;
	}

	/**
	 * Selects if list of events should appear in the pop up menu.
	 * @param b True or false.
	 */
	public void setShowEvents(boolean b) {
		showEvents = b;
	}
	
	/**
	 * Selects if the entire telescope menu (with CCD, ocular, and other options) 
	 * should appear in the pop up menu.
	 * @param b True or false.
	 */
	public void setShowCompleteTelescopeMenu(boolean b) {
		showCompleteTelescopeMenu = b;
	}

	/**
	 * Selects if the list of objects should appear in the pop up menu.
	 * @param b True or false.
	 */
	public void setShowListObj(boolean b) {
		showListObj = b;
	}

	/**
	 * Updates the chart.
	 */
	public void mouseReleased(MouseEvent e) {
		int b = e.getButton();
		if (b == MouseEvent.BUTTON1 && lastMouseTime > 0) {
			long now = System.currentTimeMillis(), dt = now - lastMouseTime;
			if (dt > 750 && (!dragging || dt < 2000))
				try {
					modifyRendering(e.getX(), e.getY());
				} catch (JPARSECException e1) {
					e1.printStackTrace();
				}
			lastMouseTime = -1;
			return;
		}
		
		if (dragging && skyRender != null && b == MouseEvent.BUTTON1) {
			// Reset mouse listener to avoid triggering object identification process above
			dragging = false;
			skyRender.getRenderSkyObject().SaveObjectsToAllowSearch = true;
			if (this.chartForDragging != null) {
				try {
					skyRender.getRenderSkyObject().setSkyRenderElement(chart);
					skyRender.getRenderSkyObject().setStarsLimitingMagnitude();
				} catch (Exception exc) {}
			}
			skyRender.getRenderSkyObject().render.planetRender.textures = showTextures;
			skyRender.getRenderSkyObject().render.drawObjectsLimitingMagnitude = -chart.drawObjectsLimitingMagnitude;
			if (showTextures) {
				if (chart.getColorMode() == COLOR_MODE.BLACK_BACKGROUND) {
					chart.drawHorizonTexture = showTexturedHorizon;
					chart.drawMilkyWayContoursWithTextures = showTexturedMW;
					if (showTexturedMW == MILKY_WAY_TEXTURE.NO_TEXTURE && chart.planetRender.highQuality && RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR >= 1.5f)
						chart.drawMilkyWayContoursWithTextures = MILKY_WAY_TEXTURE.OPTICAL;
					if (chart.drawMilkyWayContoursWithTextures != MILKY_WAY_TEXTURE.NO_TEXTURE) {
						skyRender.getRenderSkyObject().render.background = 255<<24 | 5<<16 | 5<<8 | 5;
					}
				}
				chart.drawDeepSkyObjectsTextures = showTexturedObj;
				skyRender.getRenderSkyObject().render.drawMilkyWayContoursWithTextures = chart.drawMilkyWayContoursWithTextures;
				skyRender.getRenderSkyObject().render.drawHorizonTexture = chart.drawHorizonTexture;
			}

			if (this.chartForDragging == null) {
				skyRender.getRenderSkyObject().render.drawFastLabels = SUPERIMPOSED_LABELS.AVOID_SUPERIMPOSING_VERY_ACCURATE;
				if (increaseSpeed) {
					if (showFastLabels == SUPERIMPOSED_LABELS.FAST) skyRender.getRenderSkyObject().render.drawFastLabels = showFastLabels;
					skyRender.getRenderSkyObject().render.fillMilkyWay = showFilledMW;
					skyRender.getRenderSkyObject().resetLeyend(false);
					skyRender.getRenderSkyObject().render.drawFastLinesMode = FAST_LINES.NONE;
					skyRender.getRenderSkyObject().render.fillNebulae = fillNeb;
				}
			}
			paintImage();
			skyRender.getRenderSkyObject().render.drawFastLabels = showFastLabels;
			if (RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR < 2 || !chart.planetRender.highQuality)
				skyRender.getRenderSkyObject().render.drawFastLinesMode = fastLines; // XXX
		}
	}

	private double min = 1 * Constant.ARCSEC_TO_RAD, max = 360 * Constant.DEG_TO_RAD;
	/**
	 * Zoom support.
	 */
	public void mouseWheelMoved(MouseWheelEvent e) {
  		e.consume(); // Bug on Windows
		if (skyRender != null)
		{
			lastMouseClickX = e.getX();
			lastMouseClickY = e.getY();
			zoom(e.getWheelRotation());
 		}
	}
	
	private synchronized void zoom(int wr) {
		oldTextures = skyRender.getRenderSkyObject().render.planetRender.textures;
		if (wr>0) {
			skyRender.getRenderSkyObject().render.telescope.ocular.focalLength *= 1.2f;
		} else {
			skyRender.getRenderSkyObject().render.telescope.ocular.focalLength /= 1.2f;
		}
		if (wr<0) //fastField > 10 * Constant.DEG_TO_RAD)
			skyRender.getRenderSkyObject().render.planetRender.textures = false;
		try {
				fastField = skyRender.getRenderSkyObject().render.telescope.getField();
				if (fastField < min) {
					skyRender.getRenderSkyObject().render.telescope.ocular.focalLength *= min / fastField;
					fastField = skyRender.getRenderSkyObject().render.telescope.getField();
				}
				if (fastField > max) {
					skyRender.getRenderSkyObject().render.telescope.ocular.focalLength *= max / fastField;
					fastField = skyRender.getRenderSkyObject().render.telescope.getField();
				}
				vel = 1.5 * fastField / chart.width;
				chart.telescope = skyRender.getRenderSkyObject().render.telescope;
		} catch (Exception exc) {
			Logger.log(LEVEL.ERROR, "Error processing mouse wheel. Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
		}
		int ycenter = 0;
		if (hMode && (skyRender.getRenderSkyObject().render.projection == Projection.PROJECTION.STEREOGRAPHICAL ||
					skyRender.getRenderSkyObject().render.projection == Projection.PROJECTION.SPHERICAL) &&
					skyRender.getRenderSkyObject().render.coordinateSystem == COORDINATE_SYSTEM.HORIZONTAL &&
					skyRender.getRenderSkyObject().render.centralLatitude < horizonViewModeLatitudeLimit &&
					fastField > horizonViewModeFieldLimit) // Horizon-view mode
					ycenter = (skyRender.getRenderSkyObject().render.height-100)/2;
		try {
				skyRender.getRenderSkyObject().setYCenterOffset(ycenter);
		} catch (Exception e1) {
				Logger.log(LEVEL.ERROR, "Error setting center y value. Message was: "+e1.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e1.getStackTrace()));
		}
		skyRender.getRenderSkyObject().render.drawObjectsLimitingMagnitude = chart.drawObjectsLimitingMagnitude;
		if (chartForDragging == null) {
			if (increaseSpeed) {
				//skyRender.getRenderSkyObject().render.drawFastLabels = true;
				skyRender.getRenderSkyObject().render.fillMilkyWay = false;
				skyRender.getRenderSkyObject().render.drawMilkyWayContoursWithTextures = MILKY_WAY_TEXTURE.NO_TEXTURE;
				skyRender.getRenderSkyObject().render.drawHorizonTexture = HORIZON_TEXTURE.NONE;
//				skyRender.getRenderSkyObject().resetLeyend(false);
			}
		} else {
			try {
				updateChartForDragging();
				skyRender.getRenderSkyObject().setSkyRenderElement(chartForDragging);
			} catch (Exception exc) {}
		}

		// Faster zoom reduction when big DSO image/s are on the screen
		if (!running) {
			runningTime = System.currentTimeMillis() + runningWaitTime;
			Thread thread1 = new Thread(new thread1());
			thread1.start();
		}
	}

	private static long runningTime = -1, runningWaitTime = 100;
	private static boolean running = false, oldTextures = false;
	private class thread1 implements Runnable {
		public void run() {
			running = true;
			long now = System.currentTimeMillis();
			try {
				while (now < runningTime) {
					Thread.sleep(runningWaitTime/2);
					now = System.currentTimeMillis();
				}
			} catch (Exception exc) {}
			running = false;
			paintImage();
			skyRender.getRenderSkyObject().render.planetRender.textures = oldTextures;
		}
	}


	/**
	 * Writes the object to a binary file.
	 * @param out Output stream.
	 * @throws IOException If an error occurs.
	 */
	private void writeObject(ObjectOutputStream out)
	throws IOException {
		out.writeObject(this.chart);

		out.writeObject(time);
		out.writeObject(obs);
		out.writeObject(eph);

		out.writeInt(this.x0);
		out.writeInt(this.y0);
	}
	/**
	 * Reads the object.
	 * @param in Input stream.
	 * @throws IOException I/O error.
	 * @throws ClassNotFoundException Class not found error.
	 */
	private void readObject(ObjectInputStream in)
	throws IOException, ClassNotFoundException {
		chart = (SkyRenderElement) in.readObject();

		TimeElement time = (TimeElement) in.readObject();
		if (time.astroDate == null) time.astroDate = new AstroDate();

		ObserverElement observer = (ObserverElement) in.readObject();
		EphemerisElement eph = (EphemerisElement) in.readObject();

		x = in.readInt();
		y = in.readInt();

		x0 = x;
		y0 = y;
		w0 = chart.width;
		h0 = chart.height;

    	eph.preferPrecisionInEphemerides = false;
    	eph.correctForEOP = false;

		try {
			skyRender = new SkyRendering(time, observer, eph, chart, "Sky fast render", 0);
		} catch (Exception exc) {
			Logger.log(LEVEL.ERROR, "Error reading object. Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
		}

		this.create();
 	}

	/**
	 * Constructor for an input binary file.
	 * @param in Input stream.
	 * @throws IOException If an error occurs.
	 * @throws ClassNotFoundException If an error occurs.
	 */
	public SkyChart(ObjectInputStream in)
	throws IOException, ClassNotFoundException {
		readObject(in);
 	}

	/**
	 * Returns sky render object.
	 * @return Sky render object.
	 */
	public SkyRenderElement getSkyRenderObject()
	{
		return this.chart;
	}

	/**
	 * Returns sky render object for dragging.
	 * @return Sky render object, or null if it is not defined.
	 */
	public SkyRenderElement getSkyRenderObjectForDragging()
	{
		return this.chartForDragging;
	}

	/**
	 * Returns time object.
	 * @return Time object.
	 */
	public TimeElement getTimeObject()
	{
		return this.time;
	}
	/**
	 * Returns observer object.
	 * @return Observer object.
	 */
	public ObserverElement getObserverObject()
	{
		return this.obs;
	}
	/**
	 * Returns ephemeris object.
	 * @return Ephemeris object.
	 */
	public EphemerisElement getEphemerisObject()
	{
		return this.eph;
	}


	/**
	 * Pan support.
	 */
	public void keyPressed(KeyEvent e) {
		//if (e.getSource() != label && (panel == null || e.getSource() != panel)) return;
		if (menu != null) {
			return;
		}

		if (obs.getMotherBody() == TARGET.NOT_A_PLANET && posEQ != null) {
			LocationElement l = skyRender.getRenderSkyObject().getEquatorialPositionOfRendering();
			if (l != null && (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN)) {
				try {
					l.setRadius(this.posEQvel);
					if (e.getKeyCode() == KeyEvent.VK_UP) {
						this.posEQ = Functions.sumVectors(posEQ, l.getRectangularCoordinates());
					} else {
						this.posEQ = Functions.substract(posEQ, l.getRectangularCoordinates());
					}
					posEQ = new double[] {posEQ[0], posEQ[1], posEQ[2], 0, 0, 0};
					this.obs = ObserverElement.parseExtraterrestrialObserver(new ExtraterrestrialObserverElement(Translate.translate(1083), posEQ));
					skyRender.getRenderSkyObject().setSkyRenderElement(skyRender.getRenderSkyObject().render, time, obs, eph);
					if (object != null) {
		  				paintIt();
						setCentralObject(object, null);
					}
	  				paintIt();
				} catch (Exception exc) {
					exc.printStackTrace();
				}
			} else {
				if (e.getKeyCode() == KeyEvent.VK_LEFT) this.posEQvel /= 1.4;
				if (e.getKeyCode() == KeyEvent.VK_RIGHT) this.posEQvel *= 1.4;
				if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT) {
					String s = "vel = "+(float)posEQvel+" AU";
					System.out.println(s);
					Graphics2D g = (Graphics2D) panel.getGraphics();
					g.setColor(Color.BLACK);
					g.fillRect(0, panel.getHeight()-30, 200, 100);
					g.setColor(Color.WHITE);
					g.drawString(s, 20, panel.getHeight()-15);
				}
			}
			return;
		}

		int dx = 0, dy = 0;
		int speed = 20;
		if (e.getKeyCode() == KeyEvent.VK_LEFT) dx = -speed;
		if (e.getKeyCode() == KeyEvent.VK_RIGHT) dx = +speed;
		if (e.getKeyCode() == KeyEvent.VK_UP) dy = -speed;
		if (e.getKeyCode() == KeyEvent.VK_DOWN) dy = +speed;
		if (skyRender != null && dx != 0 || dy != 0)
		{
			double moveAngleX = vel * dx;
			double moveAngleY = vel * dy;
			if (skyRender.getRenderSkyObject().render.coordinateSystem == jparsec.astronomy.CoordinateSystem.COORDINATE_SYSTEM.HORIZONTAL)
				moveAngleX = -moveAngleX;
			skyRender.getRenderSkyObject().render.centralLongitude += moveAngleX;
			skyRender.getRenderSkyObject().render.centralLatitude += moveAngleY;
			if (skyRender.getRenderSkyObject().render.centralLatitude > Constant.PI_OVER_TWO)
				skyRender.getRenderSkyObject().render.centralLatitude = Constant.PI_OVER_TWO;
			if (skyRender.getRenderSkyObject().render.centralLatitude < -Constant.PI_OVER_TWO)
				skyRender.getRenderSkyObject().render.centralLatitude = -Constant.PI_OVER_TWO;
			if (skyRender.getRenderSkyObject().render.centralLongitude > Constant.TWO_PI)
				skyRender.getRenderSkyObject().render.centralLongitude -= Constant.TWO_PI;
			if (skyRender.getRenderSkyObject().render.centralLongitude < 0)
				skyRender.getRenderSkyObject().render.centralLongitude += Constant.TWO_PI;
			if (!dragging) {
				if (this.chartForDragging == null) {
					//if (fastField > 10 * Constant.DEG_TO_RAD) skyRender.getRenderSkyObject().render.planetRender.textures = false;
					skyRender.getRenderSkyObject().render.drawObjectsLimitingMagnitude = chart.drawObjectsLimitingMagnitude;
					if (increaseSpeed) {
						//skyRender.getRenderSkyObject().render.drawFastLabels = true;
						skyRender.getRenderSkyObject().render.fillMilkyWay = false;
						skyRender.getRenderSkyObject().render.drawMilkyWayContoursWithTextures = MILKY_WAY_TEXTURE.NO_TEXTURE;
						skyRender.getRenderSkyObject().render.drawHorizonTexture = HORIZON_TEXTURE.NONE;
						skyRender.getRenderSkyObject().resetLeyend(false);
					}
				} else {
					try {
						updateChartForDragging();
						skyRender.getRenderSkyObject().setSkyRenderElement(chartForDragging);
					} catch (Exception exc) {}
				}
			}
			dragging = true;
			paintImage();
		}

		int wheel = 0;
		speed = 1;
		if (e.getKeyCode() == KeyEvent.VK_PAGE_UP) wheel = speed;
		if (e.getKeyCode() == KeyEvent.VK_PAGE_DOWN) wheel = -speed;
		if (wheel != 0) {
			if (wheel > 0) {
				skyRender.getRenderSkyObject().render.telescope.ocular.focalLength *= 1.2f;
			} else {
				skyRender.getRenderSkyObject().render.telescope.ocular.focalLength /= 1.2f;
			}
			if (skyRender.getRenderSkyObject().render.telescope.ocular.focalLength < 0.01f)
				skyRender.getRenderSkyObject().render.telescope.ocular.focalLength = 0.01f;
			try {
				fastField = skyRender.getRenderSkyObject().render.telescope.getField();
				if (fastField < min) {
					skyRender.getRenderSkyObject().render.telescope.ocular.focalLength *= min / fastField;
					fastField = skyRender.getRenderSkyObject().render.telescope.getField();
				}
				if (fastField > max) {
					skyRender.getRenderSkyObject().render.telescope.ocular.focalLength *= max / fastField;
					fastField = skyRender.getRenderSkyObject().render.telescope.getField();
				}
			} catch (Exception exc) {
				Logger.log(LEVEL.ERROR, "Error processing key event. Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
			}
			vel = 1.5 * fastField / chart.width;
			//if (fastField > 10 * Constant.DEG_TO_RAD) skyRender.getRenderSkyObject().render.planetRender.textures = false;
			skyRender.getRenderSkyObject().render.drawObjectsLimitingMagnitude = chart.drawObjectsLimitingMagnitude;
			paintImage();
		}
	}

	/**
	 * Zoom support.
	 */
	public void keyReleased(KeyEvent e) {
		//if (e.getSource() != label && (panel == null || e.getSource() != panel)) return;
		if (menu != null) {
			return;
		}

		if (e.getKeyCode() == KeyEvent.VK_M && Translate.getDefaultLanguage() == LANGUAGE.ENGLISH ||
				e.getKeyCode() == KeyEvent.VK_V && Translate.getDefaultLanguage() == LANGUAGE.SPANISH) {
			int next = this.showTexturedMW.ordinal() + 1;
			if (next == SkyRenderElement.MILKY_WAY_TEXTURE.values().length) next = 0;
			showTexturedMW = SkyRenderElement.MILKY_WAY_TEXTURE.values()[next];
			Logger.log(LEVEL.INFO, "Using Milky Way texture "+showTexturedMW);
		}
	}

	/**
	 * Nothing.
	 */
	public void keyTyped(KeyEvent e) {
//		//if (e.getSource() != label && (panel == null || e.getSource() != panel)) return;
//		if (menu != null) {
//			return;
//		}
	}

	private void updateDate(TimeElement time) throws JPARSECException {
		now = false;
		this.time = time;
		Logger.log(LEVEL.INFO, "Selected new date: "+time.toString());
	    double ttminusut1 = TimeScale.getTTminusUT1(time, obs);
		Logger.log(LEVEL.INFO, "TT-UT1 = "+Functions.formatValue(ttminusut1, 3)+" s");
		skyRender.getRenderSkyObject().setSkyRenderElement(skyRender.getRenderSkyObject().render, time, obs, eph);
		skyRender.getRenderSkyObject().dateChanged(true);
		if (object != null) {
			paintIt();
			setCentralObject(object, null);
		}
		paintIt();
	}
	
	private void updateObserver(ObserverElement observer2) throws JPARSECException {
		if (observer2 != null) {
			//updateTime = updateTime0;
			//updateTimer();

			if (obs.getMotherBody() != TARGET.EARTH) obs.forceObserverOnEarth();
			obs.setName(observer2.getName());
			obs.setDSTCode(observer2.getDSTCode());
			obs.setHeight(observer2.getHeight(), true);
			obs.setHumidity(observer2.getHumidity());
			obs.setLatitudeRad(observer2.getLatitudeRad());
			obs.setLongitudeRad(observer2.getLongitudeRad());
			obs.setPressure(observer2.getPressure());
			obs.setTemperature(observer2.getTemperature());
			obs.setTimeZone(observer2.getTimeZone());
		}
		skyRender.getRenderSkyObject().setSkyRenderElement(skyRender.getRenderSkyObject().render, time, obs, eph);
		if (object != null) {
			paintIt();
			setCentralObject(object, null);
		}
		paintIt();
	}
	
    JPopupMenu menu = null;
    private void modifyRendering(final int x, final int y) throws JPARSECException {

		// Identify closest object: give preference to Solar System bodies to identify
  		// them without limiting magnitude. If it is not a Solar System body then
  		// reidentify closest object applying the limiting magnitude
    	String s = null;
  		final Object data0[] = skyRender.getRenderSkyObject().getClosestObjectData(x, y, false, false);
  		Object data[] = data0;
  		if (data != null) {
	  		RenderSky.OBJECT id = (RenderSky.OBJECT) data[0];
	  		if (id == RenderSky.OBJECT.STAR)
	  			data = skyRender.getRenderSkyObject().getClosestObjectData(x, y, true, false);
	  		if (data != null) {
		  		id = (RenderSky.OBJECT) data[0];
		  		EphemElement ephem = null;
		  		String objData[] = null;
		  		StarElement star = null;
		  		if (id == RenderSky.OBJECT.DEEPSKY) {
		  			objData = (String[]) data[2];
		  			s = objData[0];
		  		} else {
		  			if (id == RenderSky.OBJECT.SUPERNOVA || id == OBJECT.NOVA || id == OBJECT.METEOR_SHOWER) {
		  				objData = (String[]) data[2];
		  	  			s = objData[0];
		  			} else {
		  				ephem = (EphemElement) data[2];
	  		  			s = ephem.name;
		  				if (id == RenderSky.OBJECT.STAR) {
		  					star = (StarElement) data[3];
		  		  			s = star.name;
		  		  			String s2 = skyRender.getRenderSkyObject().getStarProperName(star.name);
		  		  			if (s2 != null) s = s2;
		  				}
		  			}
		  		}
	  		}
  		}
  		if (s != null) {
	  		int spaces = s.indexOf("   ");
	  		if (spaces > 0) s = s.substring(0, spaces).trim();
  		}
  		menu = new JPopupMenu();
		menu.setFont(menu.getFont().deriveFont(menuFontSize));

		boolean hasTelescope = telescopeControl != null && telescopeControl.length > 0;
  		final String obj = s;
  		// Center
  		if (s != null) {
	  		JMenuItem center = new JMenuItem(Translate.translate(858)+" "+s);
	  		center.addActionListener(new ActionListener() {
	  			public void actionPerformed(ActionEvent e) {
	  				setCentralObject(obj, data0);
	  				paintIt();
	  			}
	  		});
	  		menu.add(center);

	  		// Details about
	  		JMenuItem details = new JMenuItem(Translate.translate(859)+" "+s);
	  		details.addActionListener(new ActionListener() {
	  			public void actionPerformed(ActionEvent e) {
	  				try {
	  					boolean considerSatellites = true; // Can be set to true, and the object (planet, satellite) can be retrieved with the methods provided
	  					TARGET target = skyRender.getRenderSkyObject().getPlanetInScreenCoordinates(x, y, considerSatellites, minimumSize);
	  	   				String popupText = "";
	  	  				if (target != TARGET.NOT_A_PLANET) {
		  					LocationElement loc0 = skyRender.getRenderSkyObject().getPlanetographicPosition(x, y, 3, considerSatellites);
		  					String object = target.getName();
		  					String feature = RenderPlanet.identifyFeature(loc0, target, 2); // Closest feature within 2 deg
		  					if (loc0 != null) {
			  					if (feature != null) {
			  						popupText = replaceVars(Translate.translate(883), new String[] {"%feature", "%object", "%x", "%y", "%lon", "%lat"}, new String[] {""+feature, ""+object, ""+x, ""+y, Functions.formatAngle(loc0.getLongitude(), 1), Functions.formatAngle(loc0.getLatitude(), 1)});
			  					} else {
			  						popupText = replaceVars(Translate.translate(884), new String[] {"%x", "%y", "%lon", "%lat"}, new String[] {""+x, ""+y, Functions.formatAngle(loc0.getLongitude(), 1), Functions.formatAngle(loc0.getLatitude(), 1)});
			  					}
			  	  				popupText += FileIO.getLineSeparator() + FileIO.getLineSeparator();
		  					}
	  	  				}
	  	  				popupText += identifyAndGetData(x, y);
		  				if (!popupText.equals("")) {
		  					showPopup(obj, popupText);
		  				}
	  				} catch (Exception exc) {
	  					Logger.log(LEVEL.ERROR, "Error creating the details popup. Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
	  				}
	  			}
	  		});
	  		if (!hasTelescope) menu.add(details);

	  		final RenderSky.OBJECT id = (RenderSky.OBJECT) data0[0];
	  		TARGET target = Target.getID(s);
	  		if (//id != OBJECT.ARTIFICIAL_SATELLITE &&
	  				showModifyLocTime && 
	  				id != OBJECT.METEOR_SHOWER && id != OBJECT.NOVA && id != OBJECT.SUPERNOVA && id != OBJECT.DEEPSKY
	  				&& (!target.isNaturalSatellite() || (target.isNaturalSatellite() && target.getCentralBody() == obs.getMotherBody() &&
	  						target.ordinal() >= TARGET.Phobos.ordinal() && target.ordinal() <= TARGET.Oberon.ordinal()))) {
		  		JMenuItem traj = new JMenuItem(Translate.translate(967)+" "+s);
		  		traj.addActionListener(new ActionListener() {
		  			public void actionPerformed(ActionEvent e) {
		  	    		String s = (String)JOptionPane.showInputDialog(
		  	                    null,
		  						Translate.translate(968),
		  	                    Translate.translate(969),
		  	                    JOptionPane.PLAIN_MESSAGE
		  	                    );
						try {
			  	    		if (s == null || s.length() == 0) throw new Exception("cancel trajectory");

							double before = DataSet.parseDouble(FileIO.getField(1, s, ",", false).trim());
							double after = DataSet.parseDouble(FileIO.getField(2, s, ",", false).trim());
							float step = (float) DataSet.parseDouble(FileIO.getField(3, s, ",", false).trim());
							if (before == 0.0 && after == 0.0) throw new Exception("Null trajectory");
		  	  				if (FileIO.getNumberOfFields(s, ",", false) > 4) {
		  	  					String unit = FileIO.getField(5, s, ",", false).trim().toLowerCase();
		  	  					if (unit.equals("s")) {
		  	  						before /= Constant.SECONDS_PER_DAY;
		  	  						after /= Constant.SECONDS_PER_DAY;
		  	  						step /= Constant.SECONDS_PER_DAY;
		  	  					}
		  	  					if (unit.equals("m")) {
		  	  						before /= (Constant.HOURS_PER_DAY*Constant.MINUTES_PER_HOUR);
		  	  						after /= (Constant.HOURS_PER_DAY*Constant.MINUTES_PER_HOUR);
		  	  						step /= (Constant.HOURS_PER_DAY*Constant.MINUTES_PER_HOUR);
		  	  					}
		  	  					if (unit.equals("h")) {
		  	  						before /= Constant.HOURS_PER_DAY;
		  	  						after /= Constant.HOURS_PER_DAY;
		  	  						step /= Constant.HOURS_PER_DAY;
		  	  					}
		  	  					if (unit.equals("y")) {
		  	  						before *= 365.25;
		  	  						after *= 365.25;
		  	  						step *= 365.25;
		  	  					}
		  	  				}

		  	    			SkyRenderElement sky = skyRender.getRenderSkyObject().render;
		  	  				double jd_bdt = TimeScale.getJD(time, obs, eph, TimeElement.SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		  	  				TrajectoryElement.LABELS labelType = TrajectoryElement.LABELS.DAY_MONTH_ABBREVIATION;
		  	  				if (Math.abs(after-before) > 365) {
		  	  					if (before == (int) before) {
		  	  						double jd = TimeScale.getJD(time, obs, eph, TimeElement.SCALE.LOCAL_TIME);
		  	  						jd = Math.floor(jd - 0.5) + 0.5;
				  	  				jd_bdt = TimeScale.getJD(new TimeElement(jd, SCALE.LOCAL_TIME), obs, eph, TimeElement.SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		  	  					}
		  	  					//labelType = TrajectoryElement.LABELS.YEAR_MONTH_DAY;
		  	  				}
		  	  				int labelStep = 3;
		  	  				if (step <= 0.5) labelStep = (int)(0.5 + 1.0 / step);
		  	  				if (FileIO.getNumberOfFields(s, ",", false) >= 4) labelStep = (int)DataSet.parseDouble(FileIO.getField(4, s, ",", false).trim());

		  	  				id.showCometTail = true;
			  	  			TrajectoryElement path = new TrajectoryElement(id, obj, jd_bdt - before,
			  	  				jd_bdt + after, step, true, labelType, labelStep, id != OBJECT.ARTIFICIAL_SATELLITE, id != OBJECT.ARTIFICIAL_SATELLITE);
			  				path.apparentObjectName = ""; //targets[(int) values[1]];
			  				path.drawPathFont = FONT.DIALOG_ITALIC_13;
			  				path.drawPathColor1 = Picture.invertColor(new Color(sky.background)).getRGB(); //Color.BLACK;
			  				path.drawPathColor2 = Color.RED.getRGB();

			  				if (sky.trajectory != null && sky.trajectory.length > 0) {
			  					TrajectoryElement tr[] = new TrajectoryElement[1+sky.trajectory.length];
			  					for (int it = 0; it < tr.length-1; it ++) {
			  						tr[it] = sky.trajectory[it];
			  					}
			  					tr[tr.length-1] = path;
			  					sky.trajectory = tr;
			  				} else {
			  					sky.trajectory = new TrajectoryElement[] {path};
			  				}
							skyRender.getRenderSkyObject().trajectoryChanged();
							skyRender.getRenderSkyObject().setSkyRenderElement(sky);
							updateImage();
							fastField = skyRender.getRenderSkyObject().render.telescope.getField();
							vel = 1.5 * fastField / chart.width;
							chart = skyRender.getRenderSkyObject().render.clone();
						} catch (Exception e1) {
		  	    			SkyRenderElement sky = skyRender.getRenderSkyObject().render;
		  	    			sky.trajectory = null;
		  	    			chart.trajectory = null;
							try {
								skyRender.getRenderSkyObject().setSkyRenderElement(sky);
							} catch (Exception e2) {
								e2.printStackTrace();
							}
						}
		  				paintIt();
		  			}
		  		});
		  		menu.add(traj);
	  		}

	  		if (!hasTelescope && s.equals(TARGET.Moon.getName())) {
		  		JMenuItem lunarEclipse = new JMenuItem(Translate.translate(1117));
		  		lunarEclipse.addActionListener(new ActionListener() {
		  			public void actionPerformed(ActionEvent e) {
		  				try {
		  					PLANET_MAP map = PLANET_MAP.MAP_FLAT;
		  					map.clear();
		  					map.zoomFactor = 1f;
		  					map.EarthMapSource = null;
		  					boolean found = false;
		  					TimeElement newTime = time.clone();
		  					do {
			  					SimpleEventElement s = MainEvents.MoonPhaseOrEclipse(newTime.astroDate.jd()-1, EVENT.MOON_LUNAR_ECLIPSE, EVENT_TIME.NEXT);
			  					newTime = new TimeElement(s.time, time.timeScale);
			  					found = RenderEclipse.lunarEclipseVisible(newTime, obs, eph, false);
			  					if (!found) newTime.add(20);
		  					} while (!found);
		  					RenderSatellite.ALLOW_SPLINE_RESIZING = false; // Improve performance
		  					boolean hq = RenderPlanet.FORCE_HIGHT_QUALITY;
		  					RenderPlanet.FORCE_HIGHT_QUALITY = true;
		  					jparsec.graph.chartRendering.Graphics g = new AWTGraphics(600, 800, ANAGLYPH_COLOR_MODE.NO_ANAGLYPH, false, false);
		  					RenderEclipse.renderLunarEclipse(newTime, obs, eph, g, skyRender.getRenderSkyObject().render.coordinateSystem == COORDINATE_SYSTEM.HORIZONTAL, map);
		  					Picture pic = new Picture((BufferedImage)g.getRendering());
		  					pic.show(Translate.translate(Translate.translate(1117)));
		  					RenderPlanet.FORCE_HIGHT_QUALITY = hq;
		  				} catch (Exception exc) {
		  					Logger.log(LEVEL.ERROR, "Error creating the chart. Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
		  				}
		  			}
		  		});
		  		menu.add(lunarEclipse);
	  		}
	  		if (!hasTelescope && s.equals(TARGET.SUN.getName())) {
		  		JMenuItem solarEclipse = new JMenuItem(Translate.translate(1116));
		  		solarEclipse.addActionListener(new ActionListener() {
		  			public void actionPerformed(ActionEvent e) {
		  				try {
		  					PLANET_MAP map = PLANET_MAP.MAP_SPHERICAL;
		  					map.clear();
		  					map.zoomFactor = 0.9f;
		  					map.EarthMapSource = PLANET_MAP.EARTH_MAP_POLITICAL;
		  					boolean found = false;
		  					TimeElement newTime = time.clone();
		  					do {
			  					SimpleEventElement s = MainEvents.MoonPhaseOrEclipse(newTime.astroDate.jd()-1, EVENT.MOON_SOLAR_ECLIPSE, EVENT_TIME.NEXT);
			  					newTime = new TimeElement(s.time, time.timeScale);
			  					RenderEclipse b = new RenderEclipse(newTime.astroDate);
			  					found = b.isVisible(obs) && b.getGreatestMagnitude(obs) > 0;
			  					if (!found) newTime.add(20);
		  					} while (!found);
		  					RenderPlanet.FORCE_HIGHT_QUALITY = true;
		  					RenderEclipse b = new RenderEclipse(newTime.astroDate);
		  					RenderSatellite.ALLOW_SPLINE_RESIZING = false; // Improve performance
		  					jparsec.graph.chartRendering.Graphics g = new AWTGraphics(450, 800, false, false);
		  					b.renderSolarEclipse(time.timeScale, obs, eph, g, skyRender.getRenderSkyObject().render.coordinateSystem == COORDINATE_SYSTEM.HORIZONTAL, map);
		  					Picture pic = new Picture((BufferedImage)g.getRendering());
		  					pic.show(Translate.translate(Translate.translate(1116)));
		  					map.clear();
		  				} catch (Exception exc) {
		  					Logger.log(LEVEL.ERROR, "Error creating the chart. Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
		  				}
		  			}
		  		});
		  		menu.add(solarEclipse);
	  		}
  		}

  		// Goto with telescope
  		if (hasTelescope) {
  			boolean sunTooClose = false, notNow = false;
  			if (!allowPointSun) {
				try {
					eph.targetBody = TARGET.SUN;
					EphemElement ephemSun = Ephem.getEphemeris(time, obs, eph, false);
					LocationElement loc = skyRender.getRenderSkyObject().getSkyLocation(x, y);
					COORDINATE_SYSTEM cs = skyRender.getRenderSkyObject().render.coordinateSystem;
					if (cs == COORDINATE_SYSTEM.ECLIPTIC) loc = CoordinateSystem.eclipticToEquatorial(loc, time, obs, eph);
					if (cs == COORDINATE_SYSTEM.GALACTIC) loc = CoordinateSystem.galacticToEquatorial(loc, time, obs, eph);
					if (cs == COORDINATE_SYSTEM.HORIZONTAL) loc = CoordinateSystem.horizontalToEquatorial(loc, time, obs, eph);
					if (cs == COORDINATE_SYSTEM.SUPERGALACTIC) {
						loc = CoordinateSystem.supergalacticToGalactic(loc);
						loc = CoordinateSystem.galacticToEquatorial(loc, time, obs, eph);
					}
					double dist2Sun = LocationElement.getAngularDistance(loc, ephemSun.getEquatorialLocation()) * Constant.RAD_TO_DEG;
					if (dist2Sun < 5) sunTooClose = true;
				} catch (Exception exc) {}
  			}
  			double jdPlan = TimeScale.getJD(time, obs, eph, SCALE.UNIVERSAL_TIME_UTC);
  			double jdNow = new AstroDate(System.currentTimeMillis()).jd();
  			if (Math.abs(jdPlan-jdNow) > 1) notNow = true;

			if (!sunTooClose && !notNow) {
	  			JMenuItem tc[] = new JMenuItem[telescopeControl.length];
	  			for (int i=0; i<telescopeControl.length; i++) {
	  				final int index = i;
	  				if (telescopeControl[index].hasGOTO()) {
	  					String ss = s;
	  					if (ss == null) ss = Translate.translate(1331);
				  		tc[index] = new JMenuItem(Translate.translate(1134)+" "+ss+" ("+TextLabel.getSimplifiedString(telescopeName[index])+")");
				  		tc[index].addActionListener(new ActionListener() {
				  			public void actionPerformed(ActionEvent e) {
				  				try {
				  					boolean sbh = chart.drawSkyCorrectingLocalHorizon;
				  					chart.drawSkyCorrectingLocalHorizon = true;
					  				LocationElement oldLoc = new LocationElement(chart.centralLongitude, chart.centralLatitude, 1.0);
					  				setCentralObject(obj, data0);
					  				LocationElement loc = new LocationElement(chart.centralLongitude, chart.centralLatitude, 1.0);
					  				chart.centralLongitude = oldLoc.getLongitude();
					  				chart.centralLatitude = oldLoc.getLatitude();
					  				setCentralObject(null, null);
					  				chart.drawSkyCorrectingLocalHorizon = sbh;
	
					  				if (chart.coordinateSystem != COORDINATE_SYSTEM.EQUATORIAL) {
						  				if (chart.coordinateSystem == COORDINATE_SYSTEM.HORIZONTAL)
						  					loc = CoordinateSystem.horizontalToEquatorial(loc, time, obs, eph);
						  				if (chart.coordinateSystem == COORDINATE_SYSTEM.ECLIPTIC)
						  					loc = CoordinateSystem.eclipticToEquatorial(loc, time, obs, eph);
						  				if (chart.coordinateSystem == COORDINATE_SYSTEM.GALACTIC)
						  					loc = CoordinateSystem.galacticToEquatorial(loc, time, obs, eph);				  					
					  				}
	
					  	  			if (!allowPointSun) {
						  				try {
						  					eph.targetBody = TARGET.SUN;
						  					EphemElement ephemSun = Ephem.getEphemeris(time, obs, eph, false);
						  					double dist2Sun = LocationElement.getAngularDistance(loc, ephemSun.getEquatorialLocation()) * Constant.RAD_TO_DEG;
						  					if (dist2Sun < 5) {
						  						String errMsg = "It is not allowed to point the telescope too close to the Sun";
							  					Logger.log(LEVEL.ERROR, errMsg);
						  						return;
						  					}
						  				} catch (Exception exc) {}
					  	  			}
					  				
					  	  			if (eph.equinox == EphemerisElement.EQUINOX_J2000) 
					  	  				loc = Ephem.fromJ2000ToApparentGeocentricEquatorial(loc, time, obs, eph);
					  	  			
				  					telescopeControl[index].setObjectCoordinates(loc, obj);
				  					telescopeMoved = telescopeControl[index].gotoObject();
				  					if (timerTel != null) timerTel.start();
				  				} catch (Exception exc) {
				  					Logger.log(LEVEL.ERROR, "Error commanding the telescope. Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
				  				}
				  			}
				  		});
				  		menu.add(tc[i]);
	  				}
	  			}
  			}
  		}
		menu.addSeparator();

  		// Lock dragging
  		JMenuItem lock = new JMenuItem(Translate.translate(860));
  		lock.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				draggingLock = true;
  			}
  		});
  		if (showLockDrag) menu.add(lock);

  		// Search object
  		String add = "";
  		if (object != null) add = " ("+object+")";
  		JMenuItem center = new JMenuItem(Translate.translate(861)+add);
  		center.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  	    		String s = (String)JOptionPane.showInputDialog(
  	    				panel.getParent(),
  	                    Translate.translate(885),
  	                    Translate.translate(886),
  	                    JOptionPane.PLAIN_MESSAGE
  	                    );
  	    		if (s != null && s.length() > 0) {
	  				try {
	  					setCentralObject(s, null);
		  				paintIt();
					} catch (Exception e1) {
         				Logger.log(LEVEL.ERROR, "Error setting central object. Message was: "+e1.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e1.getStackTrace()));
					}
  	    		} else {
  	    			object = null;
  	    		}
  			}
  		});
  		menu.add(center);

  		// Export chart
  		JMenuItem export = new JMenuItem(Translate.translate(950));
  		export.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				try {
  					String out = FileIO.fileChooser(false);
  					if (out != null) {
						String fileName = FileIO.getFileNameFromPath(out);
						try {
							int width = Integer.parseInt(fileName);
	  						// To create a very high resolution output
	  						SkyRenderElement ss = skyRender.getRenderSkyObject().render.clone();
	  						int w = ss.width;
	  						ss.width = width;
	  						ss.height = (width*ss.height)/w;
	  						if (ss.width < 800) ss.drawLeyend = LEYEND_POSITION.NO_LEYEND;
	  						ss.drawStarsLimitingMagnitude += 0.001f; // Trick to force calculation of star sizes (required in case output size is width >= 3000 px)
	  						skyRender.getRenderSkyObject().setSkyRenderElement(ss);
	  						skyRender.getRenderSkyObject().resetLeyend(true);
	  						BufferedImage offscreenImage = skyRender.createBufferedImage();
	  						Picture pic = new Picture((BufferedImage) offscreenImage);
	  						try {
	  							pic.write(out+".png");
	  						} catch (JPARSECException exc) {
  	  							SystemClipboard.setClipboard(pic.getImage());
  	  							JOptionPane.showMessageDialog(null, Translate.translate(973), Translate.translate(240), JOptionPane.WARNING_MESSAGE);
  	  							Logger.log(LEVEL.ERROR, "Error exporting chart. Image was copied to clipboard");
	  						}
						} catch (Exception exc) {
	  						Picture pic = new Picture(skyRender.createBufferedImage());
	  						try {
	  							pic.write(out);
	  						} catch (JPARSECException exc3) {
	  	  						try {
	  	  							pic.write(out+".png");
	  	  						} catch (JPARSECException exc2) {
	  	  							SystemClipboard.setClipboard(pic.getImage());
	  	  							JOptionPane.showMessageDialog(null, Translate.translate(973), Translate.translate(240), JOptionPane.WARNING_MESSAGE);
	  	  							Logger.log(LEVEL.ERROR, "Error exporting chart. Image was copied to clipboard");
	  	  						}
	  						}
						}
  						skyRender.getRenderSkyObject().resetLeyend(true);
					}

  					update();
				} catch (Exception e1) {
					String msg = e1.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e1.getStackTrace());
					JOptionPane.showMessageDialog(null, Translate.translate(1002), Translate.translate(230), JOptionPane.ERROR_MESSAGE);
     				Logger.log(LEVEL.ERROR, "Error exporting chart. Message was: "+msg);
				}
  			}
  		});
  		menu.add(export);
  		menu.addSeparator();

  		// Modify date
  		add = time.toString();
  		JMenuItem date = new JMenuItem(Translate.translate(862)+" ("+add+")");
  		date.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				AstroDate astro = time.astroDate;
  				String date = astro.getYear()+", "+astro.getMonth()+", "+astro.getDay()+", "+astro.getHour()+", "+astro.getMinute()+", "+Functions.formatValue(astro.getSeconds(), 3)+", "+time.getTimeScaleAbbreviation();
  	    		String s = (String)JOptionPane.showInputDialog(
  	                    null,
  						replaceVars(Translate.translate(887), new String[] {"%date"}, new String[] {date}),
  	                    Translate.translate(888),
  	                    JOptionPane.PLAIN_MESSAGE, null, null, date
  	                    );
  	    		if (s != null && s.length() > 0) {
  	    			int prevYear = time.astroDate.getYear();
	  				try {
	  					boolean removeObj = false;
	  					if (s.toLowerCase().equals(Translate.translate(889))) {
	  						time.astroDate = new AstroDate();
	  						time.timeScale = TimeElement.SCALE.LOCAL_TIME;
	  						now = true;
	  						timer.start();
	  						timeVel = 100.0;
	  						removeObj = true;
	  					} else {
	  						now = false;
	  						TimeElement oldTime = time.clone();
	  						if ((s.startsWith("+") || s.startsWith("-")) && s.indexOf(",")<0) {
	  							double val = 1.0;
	  							if (s.startsWith("-")) val = -1.0;
	  							if (s.endsWith("h")) {
	  								val /= 24.0;
	  							} else {
		  							if (s.endsWith("m")) {
		  								val /= (24.0 * 60.0);
		  							} else {
			  							if (s.endsWith("s")) {
			  								val /= (24.0 * 3600.0);
			  							} else {
				  							if (!s.endsWith("d")) {
				  								throw new Exception("Cannot understand '"+s+"'.");
				  							}
			  							}
		  							}
	  							}
	  							if (s.startsWith("+") || s.startsWith("-")) s = s.substring(1);
	  							s = s.substring(0, s.length()-1);
	  							val *= DataSet.parseDouble(s);
	  	  						timer.stop();
	  							time.add(val);
	  						} else {
	  							int yr = Integer.parseInt(FileIO.getField(1, s, ",", false).trim());
	  							if (yr == 0) throw new Exception("Year 0 does not exist!");
			  					astro.setYear(yr);
			  					astro.setMonth(Integer.parseInt(FileIO.getField(2, s, ",", false).trim()));
			  					astro.setDay(Integer.parseInt(FileIO.getField(3, s, ",", false).trim()));
			  					int hour = Integer.parseInt(FileIO.getField(4, s, ",", false).trim());
			  					int min = Integer.parseInt(FileIO.getField(5, s, ",", false).trim());
			  					double sec = DataSet.parseDouble(FileIO.getField(6, s, ",", false).trim());
			  					double frac = (sec / 3600.0 + min / 60.0 + hour) / 24.0;
			  					astro.setDayFraction(frac);
			  					time.astroDate = astro;
			  					String ts = FileIO.getField(7, s, ",", false).trim().toLowerCase();
		  						timer.stop();

			  					if (ts.equals("tt")) time.timeScale = TimeElement.SCALE.TERRESTRIAL_TIME;
			  					if (ts.equals("tdb")) time.timeScale = TimeElement.SCALE.BARYCENTRIC_DYNAMICAL_TIME;
			  					if (ts.equals("ut") || ts.equals("utc") || ts.equals("tuc") || ts.equals("tu")) time.timeScale = TimeElement.SCALE.UNIVERSAL_TIME_UTC;
			  					if (ts.equals("ut1") || ts.equals("tu1")) time.timeScale = TimeElement.SCALE.UNIVERSAL_TIME_UT1;
			  					if (ts.equals("lt") || ts.equals("tl")) time.timeScale = TimeElement.SCALE.LOCAL_TIME;
	  						}

	  						if (Math.abs(time.astroDate.getYear()) > 200000) {
	  							time = oldTime;
	  							throw new Exception("Date is limited between years -200000 and 200000!");
	  						}

	  		       			if (time.timeScale != SCALE.TERRESTRIAL_TIME  && time.timeScale != SCALE.BARYCENTRIC_DYNAMICAL_TIME &&
	  		       					(time.astroDate.getYear() < -1000 || time.astroDate.getYear() > 3000)) {
	  							JOptionPane.showMessageDialog(null, Translate.translate(1294), Translate.translate(240), JOptionPane.WARNING_MESSAGE);
	  		       			}
	  					}

	  					// Update observer in case it is located in a non-Earth body which
	  					// is not a planet with a unique TARGET id (comet or asteroid)
	  					if (obs.getMotherBody() == TARGET.NOT_A_PLANET) {
							EphemerisElement ephC = eph.clone();
							ephC.ephemType = COORDINATES_TYPE.GEOMETRIC;
							ephC.algorithm = ALGORITHM.ORBIT;
							ephC.isTopocentric = false;
							TARGET target = TARGET.Comet;
							target.setIndex(OrbitEphem.getIndexOfComet(obs.getName()));
							if (target.getIndex() == -1) {
								target = TARGET.Asteroid;
								target.setIndex(OrbitEphem.getIndexOfAsteroid(obs.getName()));
								if (target.getIndex() >= 0) ephC.orbit = OrbitEphem.getOrbitalElementsOfAsteroid(target.getIndex());
							} else {
								if (target.getIndex() >= 0) ephC.orbit = OrbitEphem.getOrbitalElementsOfComet(target.getIndex());
							}
							if (target.getIndex() >= 0) {
								double jd = TimeScale.getJD(time, obs, ephC, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
								double[] out = Ephem.eclipticToEquatorial(OrbitEphem.obtainPosition(time, obs, ephC), ephC.getEpoch(jd), eph);
								obs = ObserverElement.parseExtraterrestrialObserver(new ExtraterrestrialObserverElement(obs.getName(), out));
							}
	  					}

	  					if (time.astroDate.getYear() != prevYear && Runtime.getRuntime().availableProcessors() > 1
	  							&& time.astroDate.getYear() >= -1000 && time.astroDate.getYear() < 2998 && showEvents
	  							) {
	  						thread = new Thread(new thread0());
	  						thread.start();
	  					}

	  					updateDate(time);
	  					if (removeObj) object = null;
					} catch (Exception e1) {
						String msg = e1.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e1.getStackTrace());
						JOptionPane.showMessageDialog(null, msg, Translate.translate(230), JOptionPane.ERROR_MESSAGE);
         				Logger.log(LEVEL.ERROR, "Error setting time. Message was: "+msg);
					}
  	    		}
  			}
  		});
  		if (showModifyLocTime)	menu.add(date);

  		String realTimeOp[] = new String[] {
  				Translate.translate(979),
  		 		"10d "+Translate.translate(976),
  		 		"1d "+Translate.translate(976),
  		 		"1h "+Translate.translate(976),
  		 		"1m "+Translate.translate(976),
  		 		"1s "+Translate.translate(976),
  		 		Translate.translate(978), // Real time
  		 		"1s "+Translate.translate(977),
  		 		"1m "+Translate.translate(977),
  		 		"1h "+Translate.translate(977),
  		 		"1d "+Translate.translate(977),
  		 		"10d "+Translate.translate(977)
  		};
  		double val[] = new double[] {0, 10, 1, 1.0/24.0, 1.0/1440.0, 1.0/86400, 100, -1.0/86400, -1.0/1440.0, -1.0/24.0, -1, -10};
  		int selected = -1;
  		for (int i=0; i<val.length; i++) {
  			if (timeVel == val[i]) {
  				selected = i;
  				break;
  			}
  		}
  		if (!timer.isRunning()) selected = 0;
  		String sel = "";
  		if (selected != -1) sel = " ("+realTimeOp[selected].toLowerCase()+")";
 		JMenu realTime = new JMenu(Translate.translate(975)+sel);
 		JMenuItem rtItem0 = new JMenuItem(realTimeOp[0]); // stop
 		JMenuItem rtItem1 = new JMenuItem(realTimeOp[1]);
 		JMenuItem rtItem2 = new JMenuItem(realTimeOp[2]);
 		JMenuItem rtItem3 = new JMenuItem(realTimeOp[3]);
 		JMenuItem rtItem4 = new JMenuItem(realTimeOp[4]);
 		JMenuItem rtItem5 = new JMenuItem(realTimeOp[5]);
 		JMenuItem rtItem6 = new JMenuItem(realTimeOp[6]); // Real time
 		JMenuItem rtItem7 = new JMenuItem(realTimeOp[7]);
 		JMenuItem rtItem8 = new JMenuItem(realTimeOp[8]);
 		JMenuItem rtItem9 = new JMenuItem(realTimeOp[9]);
 		JMenuItem rtItem10 = new JMenuItem(realTimeOp[10]);
 		JMenuItem rtItem11 = new JMenuItem(realTimeOp[11]);
 		realTime.add(rtItem0);
 		realTime.add(rtItem1);
 		realTime.add(rtItem2);
 		realTime.add(rtItem3);
 		realTime.add(rtItem4);
 		realTime.add(rtItem5);
 		realTime.add(rtItem6);
 		realTime.add(rtItem7);
 		realTime.add(rtItem8);
 		realTime.add(rtItem9);
 		realTime.add(rtItem10);
 		realTime.add(rtItem11);
 		rtItem0.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
				timeVel = 0.0;
				timer.stop();
  			}
  		});
 		rtItem1.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				timeVel = 10;
  				timer.start();
  			}
  		});
 		rtItem2.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				timeVel = 1;
  				timer.start();
  			}
  		});
 		rtItem3.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				timeVel = 1.0/24.0;
  				timer.start();
  			}
  		});
 		rtItem4.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				timeVel = 1.0/1440.0;
  				timer.start();
  			}
  		});
 		rtItem5.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				timeVel = 1.0/86400;
  				timer.start();
  			}
  		});
 		rtItem6.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				timeVel = 100.0;
  				timer.start();
  			}
  		});
 		rtItem7.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				timeVel = -1.0/86400;
  				timer.start();
  			}
  		});
 		rtItem8.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				timeVel = -1.0/1440.0;
  				timer.start();
  			}
  		});
 		rtItem9.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				timeVel = -1.0/24;
  				timer.start();
  			}
  		});
 		rtItem10.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				timeVel = -1;
  				timer.start();
  			}
  		});
 		rtItem11.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				timeVel = -10;
  				timer.start();
  			}
  		});
  		menu.add(realTime);

  		// Modify observer
  		JMenuItem observer = new JMenuItem(Translate.translate(863)+" ("+obs.getName()+")");
  		observer.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				String loc = obs.getName();
  	    		String s = (String)JOptionPane.showInputDialog(
  	                    null,
  						replaceVars(Translate.translate(890), new String[] {"%loc"}, new String[] {loc}),
  						Translate.translate(891),
  	                    JOptionPane.PLAIN_MESSAGE
  	                    );
  	    		if (s != null && s.length() > 0) {
	  				try {
	  				    CityElement cities[] = City.findAllCities(s);
	  					ObserverElement observer2 = null;
	  					if (cities == null) {
	  						try {
		  						ObservatoryElement obse = null;
	  							int nf = FileIO.getNumberOfFields(s, ",", false);
		  						if (nf != 5) obse = Observatory.findObservatorybyName(s);
		  						if (obse != null) {
		  							observer2 = ObserverElement.parseObservatory(obse);
		  	         				Logger.log(LEVEL.INFO, "Selected new observatory: "+obse.name+", lon "+Functions.formatAngleAsDegrees(observer2.getLongitudeRad(), 3)+"\u00b0, lat "+Functions.formatAngleAsDegrees(observer2.getLatitudeRad(), 3)+"\u00b0");
		  						} else {
		  							// Name, lon, lat (deg), height (m), Time zone (hours)
		  							if (nf == 5) {
		  								try {
		  									String val[] = DataSet.toStringArray(s, ",");
			  								observer2 = new ObserverElement();
			  								observer2.setName(val[0]);
			  								double lat = Double.parseDouble(val[2]);
			  								if (Math.abs(lat) > 89.999) lat = 89.999 * FastMath.sign(lat);
			  								observer2.setLongitudeRad(Double.parseDouble(val[1])*Constant.DEG_TO_RAD);
			  								observer2.setLatitudeRad(lat*Constant.DEG_TO_RAD);
			  								observer2.setHeight((int) Double.parseDouble(val[3]), true);
			  								observer2.setTimeZone(Double.parseDouble(val[4]));
			  								observer2.setDSTCode(DST_RULE.NONE);
			  								observer2.setHumidity(ObserverElement.DEFAULT_HUMIDITY);
			  								observer2.setPressure(ObserverElement.DEFAULT_PRESSURE);
			  								observer2.setTemperature(ObserverElement.DEFAULT_TEMPERATURE);
				  	         				Logger.log(LEVEL.INFO, "Selected new observer: "+observer2.getName()+", lon "+Functions.formatAngleAsDegrees(observer2.getLongitudeRad(), 3)+"\u00b0, lat "+Functions.formatAngleAsDegrees(observer2.getLatitudeRad(), 3)+"\u00b0");
		  								} catch (Exception exc2) {
		  									observer2 = null;
			  								JOptionPane.showMessageDialog(null,
				  			  						replaceVars(Translate.translate(892), new String[] {"%loc"}, new String[] {loc}),
				  			        				Translate.translate(893),
				  			                        JOptionPane.WARNING_MESSAGE
				  			                        );
		  								}
		  							} else {
		  								JOptionPane.showMessageDialog(null,
		  			  						replaceVars(Translate.translate(892), new String[] {"%loc"}, new String[] {loc}),
		  			        				Translate.translate(893),
		  			                        JOptionPane.WARNING_MESSAGE
		  			                        );
		  							}
		  						}
	  						} catch (Exception exc) {
		  						try {
		  							int index = Observatory.searchByNameInMarsdenList(s);
			  						ObservatoryElement obse = null;
			  						if (index >= 0) obse = Observatory.getObservatoryFromMarsdenList(index);
			  						if (obse != null) {
			  							observer2 = ObserverElement.parseObservatory(obse);
			  	         				Logger.log(LEVEL.INFO, "Selected new observatory: "+obse.name+", lon "+Functions.formatAngleAsDegrees(observer2.getLongitudeRad(), 3)+"\u00b0, lat "+Functions.formatAngleAsDegrees(observer2.getLatitudeRad(), 3)+"\u00b0");
			  						} else {
			  			        		JOptionPane.showMessageDialog(null,
			  			  						replaceVars(Translate.translate(892), new String[] {"%loc"}, new String[] {s}),
			  			        				Translate.translate(893),
			  			                        JOptionPane.WARNING_MESSAGE
			  			                        );
			  						}
		  						} catch (Exception exc2) {
	  	  							JOptionPane.showMessageDialog(null, Translate.translate(893), Translate.translate(240), JOptionPane.WARNING_MESSAGE);
		  							Logger.log(LEVEL.ERROR, "Error getting the observatory. Message was: "+exc2.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc2.getStackTrace()));
		  						}
	  						}
	  					} else {
		  					CityElement city = cities[0];
	  					    if (cities.length > 1) {
	  					    	String list[] = new String[cities.length];
	  					    	for (int i=0; i<cities.length; i++) {
	  					    		list[i] = cities[i].name+" ("+cities[i].country+")";
	  					    	}
	  							s = (String)JOptionPane.showInputDialog(
	  					                null,
	  					                Translate.translate(1001),
	  					                Translate.translate(1000),
	  					                JOptionPane.PLAIN_MESSAGE, null,
	  					                list, list[0]
	  					                );
	  							if (s != null && s.length() > 0) {
	  								int i = DataSet.getIndex(list, s);
	  								city = cities[i];
	  							} else {
	  								return;
	  							}
	  					    }

	  						observer2 = ObserverElement.parseCity(city);
  	         				Logger.log(LEVEL.INFO, "Selected new city: "+city.name+" ("+city.country+"), lon "+Functions.formatAngleAsDegrees(observer2.getLongitudeRad(), 3)+"\u00b0, lat "+Functions.formatAngleAsDegrees(observer2.getLatitudeRad(), 3)+"\u00b0");
	  					}
	  					updateObserver(observer2);
					} catch (Exception e1) {
						String msg = e1.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e1.getStackTrace());
						JOptionPane.showMessageDialog(panel.getParent(), msg, Translate.translate(230), JOptionPane.ERROR_MESSAGE);
         				Logger.log(LEVEL.ERROR, "Error setting observer. Message was: "+msg);
					}
  	    		}
  			}
  		});
  		if (showModifyLocTime) menu.add(observer);

  		// Modify projection
  		JMenu projection = new JMenu(Translate.translate(864)+" ("+projt[chart.projection.ordinal()].toLowerCase()+")");
  		JMenuItem pItem1 = new JMenuItem(projt[0]);
  		JMenuItem pItem2 = new JMenuItem(projt[1]);
  		JMenuItem pItem3 = new JMenuItem(projt[2]);
  		JMenuItem pItem4 = new JMenuItem(projt[3]);
  		JMenuItem pItem5 = new JMenuItem(projt[4]);
  		pItem1.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				try {
					chart.projection = Projection.PROJECTION.values()[0];
					skyRender.getRenderSkyObject().setSkyRenderElement(chart);
					if (object != null)	setCentralObject(object, null);
	  				paintIt();
				} catch (Exception e1) {
     				Logger.log(LEVEL.ERROR, "Error setting projection. Message was: "+e1.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e1.getStackTrace()));
				}
  			}
  		});
  		pItem2.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				try {
					chart.projection = Projection.PROJECTION.values()[1];
					skyRender.getRenderSkyObject().setSkyRenderElement(chart);
					if (object != null) setCentralObject(object, null);
	  				paintIt();
				} catch (Exception e1) {
     				Logger.log(LEVEL.ERROR, "Error setting projection. Message was: "+e1.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e1.getStackTrace()));
				}
  			}
  		});
  		pItem3.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				try {
					chart.projection = Projection.PROJECTION.values()[2];
					skyRender.getRenderSkyObject().setSkyRenderElement(chart);
					if (object != null) setCentralObject(object, null);
	  				paintIt();
				} catch (Exception e1) {
     				Logger.log(LEVEL.ERROR, "Error setting projection. Message was: "+e1.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e1.getStackTrace()));
				}
  			}
  		});
  		pItem4.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				try {
					chart.projection = Projection.PROJECTION.values()[3];
					skyRender.getRenderSkyObject().setSkyRenderElement(chart);
					if (object != null) setCentralObject(object, null);
	  				paintIt();
				} catch (Exception e1) {
     				Logger.log(LEVEL.ERROR, "Error setting projection. Message was: "+e1.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e1.getStackTrace()));
				}
  			}
  		});
  		pItem5.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				try {
					chart.projection = Projection.PROJECTION.values()[4];
					skyRender.getRenderSkyObject().setSkyRenderElement(chart);
					if (object != null) setCentralObject(object, null);
	  				paintIt();
				} catch (Exception e1) {
     				Logger.log(LEVEL.ERROR, "Error setting projection. Message was: "+e1.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e1.getStackTrace()));
				}
  			}
  		});
  		projection.add(pItem1);
  		projection.add(pItem2);
  		projection.add(pItem3);
  		projection.add(pItem4);
  		projection.add(pItem5);
  		menu.add(projection);

  		// Modify coordinate system
  		JMenu coordSys = new JMenu(Translate.translate(865) + " ("+coordt[chart.coordinateSystem.ordinal()].toLowerCase()+")");
  		JMenuItem csItem1 = new JMenuItem(coordt[0]);
  		JMenuItem csItem2 = new JMenuItem(coordt[1]);
  		JMenuItem csItem3 = new JMenuItem(coordt[2]);
  		JMenuItem csItem4 = new JMenuItem(coordt[3]);
  		JMenuItem csItem5 = new JMenuItem(coordt[4]);
  		csItem1.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				try {
  					SkyRenderElement sky = chart.clone();
  					LocationElement pos = skyRender.getRenderSkyObject().getEquatorialPositionOfRendering();
  					sky.coordinateSystem = CoordinateSystem.COORDINATE_SYSTEM.values()[0];
  					pos = RenderSky.getPositionInSelectedCoordinateSystem(pos, time, obs, eph, sky, false);
  					sky.centralLongitude = pos.getLongitude();
  					sky.centralLatitude = pos.getLatitude();
  					chart = sky;
					skyRender.getRenderSkyObject().setSkyRenderElement(sky);
					if (object != null) {
		  				paintIt();
						setCentralObject(object, null);
					}
	  				paintIt();
				} catch (Exception e1) {
     				Logger.log(LEVEL.ERROR, "Error setting coordinate system. Message was: "+e1.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e1.getStackTrace()));
				}
  			}
  		});
  		csItem2.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				try {
  					SkyRenderElement sky = chart.clone();
  					LocationElement pos = skyRender.getRenderSkyObject().getEquatorialPositionOfRendering();
  					sky.coordinateSystem = CoordinateSystem.COORDINATE_SYSTEM.values()[1];
  					pos = RenderSky.getPositionInSelectedCoordinateSystem(pos, time, obs, eph, sky, false);
  					sky.centralLongitude = pos.getLongitude();
  					sky.centralLatitude = pos.getLatitude();
  					chart = sky;
					skyRender.getRenderSkyObject().setSkyRenderElement(sky);
					if (object != null) {
		  				paintIt();
						setCentralObject(object, null);
					}
	  				paintIt();
				} catch (Exception e1) {
     				Logger.log(LEVEL.ERROR, "Error setting coordinate system. Message was: "+e1.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e1.getStackTrace()));
				}
  			}
  		});
  		csItem3.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				try {
  					SkyRenderElement sky = chart.clone();
  					LocationElement pos = skyRender.getRenderSkyObject().getEquatorialPositionOfRendering();
  					sky.coordinateSystem = CoordinateSystem.COORDINATE_SYSTEM.values()[2];
  					pos = RenderSky.getPositionInSelectedCoordinateSystem(pos, time, obs, eph, sky, false);
  					sky.centralLongitude = pos.getLongitude();
  					sky.centralLatitude = pos.getLatitude();
  					chart = sky;
					skyRender.getRenderSkyObject().setSkyRenderElement(sky);
					if (object != null) {
		  				paintIt();
						setCentralObject(object, null);
					}
	  				paintIt();
				} catch (Exception e1) {
     				Logger.log(LEVEL.ERROR, "Error setting coordinate system. Message was: "+e1.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e1.getStackTrace()));
				}
  			}
  		});
  		csItem4.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				try {
  					SkyRenderElement sky = chart.clone();
  					LocationElement pos = skyRender.getRenderSkyObject().getEquatorialPositionOfRendering();
  					sky.coordinateSystem = CoordinateSystem.COORDINATE_SYSTEM.values()[3];
  					pos = RenderSky.getPositionInSelectedCoordinateSystem(pos, time, obs, eph, sky, false);
  					sky.centralLongitude = pos.getLongitude();
  					sky.centralLatitude = pos.getLatitude();
  					chart = sky;
					skyRender.getRenderSkyObject().setSkyRenderElement(sky);
					if (object != null) {
		  				paintIt();
						setCentralObject(object, null);
					}
	  				paintIt();
				} catch (Exception e1) {
     				Logger.log(LEVEL.ERROR, "Error setting coordinate system. Message was: "+e1.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e1.getStackTrace()));
				}
  			}
  		});
  		csItem5.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				try {
  					SkyRenderElement sky = chart.clone();
  					LocationElement pos = skyRender.getRenderSkyObject().getEquatorialPositionOfRendering();
  					sky.coordinateSystem = CoordinateSystem.COORDINATE_SYSTEM.values()[4];
  					pos = RenderSky.getPositionInSelectedCoordinateSystem(pos, time, obs, eph, sky, false);
  					sky.centralLongitude = pos.getLongitude();
  					sky.centralLatitude = pos.getLatitude();
  					chart = sky;
					skyRender.getRenderSkyObject().setSkyRenderElement(sky);
					if (object != null) {
		  				paintIt();
						setCentralObject(object, null);
					}
	  				paintIt();
				} catch (Exception e1) {
     				Logger.log(LEVEL.ERROR, "Error setting coordinate system. Message was: "+e1.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e1.getStackTrace()));
				}
  			}
  		});
  		coordSys.add(csItem1);
  		coordSys.add(csItem2);
  		coordSys.add(csItem3);
  		if (obs.getMotherBody() != TARGET.NOT_A_PLANET && eph.isTopocentric)
  			coordSys.add(csItem4);
 		coordSys.add(csItem5);
 		  		menu.add(coordSys);

  		add = "";
  		if (colorMode != -1) add = " ("+cmt[colorMode].toLowerCase()+")";
  		JMenu colorSqueme = new JMenu(Translate.translate(980)+add);
  		JMenuItem csqItem1 = new JMenuItem(cmt[0]);
  		JMenuItem csqItem2 = new JMenuItem(cmt[1]);
  		JMenuItem csqItem3 = new JMenuItem(cmt[2]);
  		JMenuItem csqItem4 = new JMenuItem(cmt[3]);
  		JMenuItem csqItem5 = new JMenuItem(cmt[4]);
  		JMenuItem csqItem6 = new JMenuItem(cmt[5]);
  		JMenuItem csqItem7 = new JMenuItem(cmt[6]);
  		JMenuItem csqItem8 = new JMenuItem(cmt[7]);
  		JMenuItem csqItem9 = new JMenuItem(cmt[8]);
  		csqItem1.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				try {
  					chart.anaglyphMode = ANAGLYPH_COLOR_MODE.NO_ANAGLYPH;
  					chart.setColorMode(COLOR_MODE.WHITE_BACKGROUND);
  					chart.planetRender.anaglyphMode = chart.anaglyphMode;
  					if (chart.drawStarsRealistic == REALISTIC_STARS.NONE_CUTE)
  						chart.drawStarsRealistic = REALISTIC_STARS.NONE;
  					if (chart.trajectory != null) {
  						for (int i=0; i<chart.trajectory.length; i++) {
  							chart.trajectory[i].drawPathColor1 = Picture.invertColor(new Color(chart.background)).getRGB();
  						}
  					}
  	  				if (chartForDragging != null) {
  	  					chartForDragging.setColorMode(chart.getColorMode());
  	  					chartForDragging.drawStarsRealistic = chart.drawStarsRealistic;
  	  					chartForDragging.anaglyphMode = chart.anaglyphMode;
  	  					chartForDragging.planetRender.anaglyphMode = chart.anaglyphMode;
  	  				}
					skyRender.getRenderSkyObject().setSkyRenderElement(chart);
					skyRender.getRenderSkyObject().colorSquemeChanged();
					skyRender.resetGraphics();
	  				paintIt();
	  				colorMode = 0;
				} catch (Exception e1) {
     				Logger.log(LEVEL.ERROR, "Error setting color mode. Message was: "+e1.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e1.getStackTrace()));
				}
  			}
  		});
  		csqItem2.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				try {
  					chart.anaglyphMode = ANAGLYPH_COLOR_MODE.NO_ANAGLYPH;
  					chart.setColorMode(COLOR_MODE.BLACK_BACKGROUND);
  					if (obs.getMotherBody() != TARGET.EARTH) chart.background = 255<<24 | 5<<16 | 5<<8 | 5;
  					chart.planetRender.anaglyphMode = chart.anaglyphMode;
  					if (chart.drawStarsRealistic == REALISTIC_STARS.NONE_CUTE)
  						chart.drawStarsRealistic = REALISTIC_STARS.NONE;
  					if (chart.trajectory != null) {
  						for (int i=0; i<chart.trajectory.length; i++) {
  							chart.trajectory[i].drawPathColor1 = Picture.invertColor(new Color(chart.background)).getRGB();
  						}
  					}
  	  				if (chartForDragging != null) {
  	  					chartForDragging.setColorMode(chart.getColorMode());
  	  					chartForDragging.drawStarsRealistic = chart.drawStarsRealistic;
  	  					chartForDragging.anaglyphMode = chart.anaglyphMode;
  	  					chartForDragging.planetRender.anaglyphMode = chart.anaglyphMode;
  	  				}
					skyRender.getRenderSkyObject().setSkyRenderElement(chart);
					skyRender.getRenderSkyObject().colorSquemeChanged();
					skyRender.resetGraphics();
	  				paintIt();
	  				colorMode = 1;
				} catch (Exception e1) {
     				Logger.log(LEVEL.ERROR, "Error setting color mode. Message was: "+e1.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e1.getStackTrace()));
				}
  			}
  		});
  		csqItem3.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				try {
  					chart.anaglyphMode = ANAGLYPH_COLOR_MODE.NO_ANAGLYPH;
  					chart.setColorMode(COLOR_MODE.NIGHT_MODE);
  					chart.planetRender.anaglyphMode = chart.anaglyphMode;
  					if (chart.drawStarsRealistic == REALISTIC_STARS.NONE_CUTE)
  						chart.drawStarsRealistic = REALISTIC_STARS.NONE;
  					if (chart.trajectory != null) {
  						for (int i=0; i<chart.trajectory.length; i++) {
  							chart.trajectory[i].drawPathColor1 = Picture.invertColor(new Color(chart.background)).getRGB();
  						}
  					}
  	  				if (chartForDragging != null) {
  	  					chartForDragging.setColorMode(chart.getColorMode());
  	  					chartForDragging.drawStarsRealistic = chart.drawStarsRealistic;
  	  					chartForDragging.anaglyphMode = chart.anaglyphMode;
  	  					chartForDragging.planetRender.anaglyphMode = chart.anaglyphMode;
  	  				}
					skyRender.getRenderSkyObject().setSkyRenderElement(chart);
					skyRender.getRenderSkyObject().colorSquemeChanged();
					skyRender.resetGraphics();
	  				paintIt();
	  				colorMode = 2;
				} catch (Exception e1) {
     				Logger.log(LEVEL.ERROR, "Error setting color mode. Message was: "+e1.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e1.getStackTrace()));
				}
  			}
  		});
  		csqItem4.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				try {
  					chart.anaglyphMode = ANAGLYPH_COLOR_MODE.RED_CYAN;
  					chart.setColorMode(COLOR_MODE.WHITE_BACKGROUND_SIMPLE_GREEN_RED_OR_RED_CYAN_ANAGLYPH);
  					chart.planetRender.anaglyphMode = chart.anaglyphMode;
  					//if (chart.drawStarsRealistic == REALISTIC_STARS.NONE_CUTE)
  						chart.drawStarsRealistic = REALISTIC_STARS.NONE;
  					if (chart.trajectory != null) {
  						for (int i=0; i<chart.trajectory.length; i++) {
  							chart.trajectory[i].drawPathColor1 = Picture.invertColor(new Color(chart.background)).getRGB();
  						}
  					}
  	  				if (chartForDragging != null) {
  	  					chartForDragging.setColorMode(chart.getColorMode());
  	  					chartForDragging.drawStarsRealistic = chart.drawStarsRealistic;
  	  					chartForDragging.anaglyphMode = chart.anaglyphMode;
  	  					chartForDragging.planetRender.anaglyphMode = chart.anaglyphMode;
  	  				}
					skyRender.getRenderSkyObject().setSkyRenderElement(chart);
					skyRender.getRenderSkyObject().colorSquemeChanged();
					skyRender.resetGraphics();
	  				paintIt();
	  				colorMode = 3;
				} catch (Exception e1) {
     				Logger.log(LEVEL.ERROR, "Error setting color mode. Message was: "+e1.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e1.getStackTrace()));
				}
  			}
  		});
  		csqItem5.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				try {
  					chart.anaglyphMode = ANAGLYPH_COLOR_MODE.DUBOIS_RED_CYAN;
  					chart.setColorMode(COLOR_MODE.WHITE_BACKGROUND);
  					chart.planetRender.anaglyphMode = chart.anaglyphMode;
  					if (chart.drawStarsRealistic == REALISTIC_STARS.NONE_CUTE)
  						chart.drawStarsRealistic = REALISTIC_STARS.NONE;
  					if (chart.trajectory != null) {
  						for (int i=0; i<chart.trajectory.length; i++) {
  							chart.trajectory[i].drawPathColor1 = Picture.invertColor(new Color(chart.background)).getRGB();
  						}
  					}
  	  				if (chartForDragging != null) {
  	  					chartForDragging.setColorMode(chart.getColorMode());
  	  					chartForDragging.drawStarsRealistic = chart.drawStarsRealistic;
  	  					chartForDragging.anaglyphMode = chart.anaglyphMode;
  	  					chartForDragging.planetRender.anaglyphMode = chart.anaglyphMode;
  	  				}
					skyRender.getRenderSkyObject().setSkyRenderElement(chart);
					skyRender.getRenderSkyObject().colorSquemeChanged();
					skyRender.resetGraphics();
	  				paintIt();
	  				colorMode = 4;
				} catch (Exception e1) {
     				Logger.log(LEVEL.ERROR, "Error setting color mode. Message was: "+e1.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e1.getStackTrace()));
				}
  			}
  		});
  		csqItem6.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				try {
  					chart.anaglyphMode = ANAGLYPH_COLOR_MODE.DUBOIS_RED_CYAN;
  					chart.setColorMode(COLOR_MODE.BLACK_BACKGROUND);
  					chart.planetRender.anaglyphMode = chart.anaglyphMode;
  					if (chart.drawStarsRealistic == REALISTIC_STARS.NONE_CUTE)
  						chart.drawStarsRealistic = REALISTIC_STARS.NONE;
  					if (chart.trajectory != null) {
  						for (int i=0; i<chart.trajectory.length; i++) {
  							chart.trajectory[i].drawPathColor1 = Picture.invertColor(new Color(chart.background)).getRGB();
  						}
  					}
  	  				if (chartForDragging != null) {
  	  					chartForDragging.setColorMode(chart.getColorMode());
  	  					chartForDragging.drawStarsRealistic = chart.drawStarsRealistic;
  	  					chartForDragging.anaglyphMode = chart.anaglyphMode;
  	  					chartForDragging.planetRender.anaglyphMode = chart.anaglyphMode;
  	  				}
					skyRender.getRenderSkyObject().setSkyRenderElement(chart);
					skyRender.getRenderSkyObject().colorSquemeChanged();
					skyRender.resetGraphics();
	  				paintIt();
	  				colorMode = 5;
				} catch (Exception e1) {
     				Logger.log(LEVEL.ERROR, "Error setting color mode. Message was: "+e1.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e1.getStackTrace()));
				}
  			}
  		});
  		csqItem7.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				try {
  					chart.anaglyphMode = ANAGLYPH_COLOR_MODE.DUBOIS_RED_CYAN;
  					chart.setColorMode(COLOR_MODE.NIGHT_MODE);
  					chart.planetRender.anaglyphMode = chart.anaglyphMode;
  					if (chart.drawStarsRealistic == REALISTIC_STARS.NONE_CUTE)
  						chart.drawStarsRealistic = REALISTIC_STARS.NONE;
  					if (chart.trajectory != null) {
  						for (int i=0; i<chart.trajectory.length; i++) {
  							chart.trajectory[i].drawPathColor1 = Picture.invertColor(new Color(chart.background)).getRGB();
  						}
  					}
  	  				if (chartForDragging != null) {
  	  					chartForDragging.setColorMode(chart.getColorMode());
  	  					chartForDragging.drawStarsRealistic = chart.drawStarsRealistic;
  	  					chartForDragging.anaglyphMode = chart.anaglyphMode;
  	  					chartForDragging.planetRender.anaglyphMode = chart.anaglyphMode;
  	  				}
					skyRender.getRenderSkyObject().setSkyRenderElement(chart);
					skyRender.getRenderSkyObject().colorSquemeChanged();
					skyRender.resetGraphics();
	  				paintIt();
	  				colorMode = 6;
				} catch (Exception e1) {
     				Logger.log(LEVEL.ERROR, "Error setting color mode. Message was: "+e1.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e1.getStackTrace()));
				}
  			}
  		});
  		csqItem8.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				try {
  					chart.anaglyphMode = ANAGLYPH_COLOR_MODE.GREEN_RED;
  					chart.setColorMode(COLOR_MODE.WHITE_BACKGROUND_SIMPLE_GREEN_RED_OR_RED_CYAN_ANAGLYPH);
  					chart.planetRender.anaglyphMode = chart.anaglyphMode;
  					//if (chart.drawStarsRealistic == REALISTIC_STARS.NONE_CUTE)
  						chart.drawStarsRealistic = REALISTIC_STARS.NONE;
  					if (chart.trajectory != null) {
  						for (int i=0; i<chart.trajectory.length; i++) {
  							chart.trajectory[i].drawPathColor1 = Picture.invertColor(new Color(chart.background)).getRGB();
  						}
  					}
  	  				if (chartForDragging != null) {
  	  					chartForDragging.setColorMode(chart.getColorMode());
  	  					chartForDragging.drawStarsRealistic = chart.drawStarsRealistic;
  	  					chartForDragging.anaglyphMode = chart.anaglyphMode;
  	  					chartForDragging.planetRender.anaglyphMode = chart.anaglyphMode;
  	  				}
					skyRender.getRenderSkyObject().setSkyRenderElement(chart);
					skyRender.getRenderSkyObject().colorSquemeChanged();
					skyRender.resetGraphics();
	  				paintIt();
	  				colorMode = 7;
				} catch (Exception e1) {
     				Logger.log(LEVEL.ERROR, "Error setting color mode. Message was: "+e1.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e1.getStackTrace()));
				}
  			}
  		});
  		csqItem9.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				try {
  					chart.anaglyphMode = ANAGLYPH_COLOR_MODE.NO_ANAGLYPH;
  					chart.setColorMode(COLOR_MODE.PRINT_MODE);
  					chart.planetRender.anaglyphMode = chart.anaglyphMode;
					chart.drawStarsRealistic = REALISTIC_STARS.NONE_CUTE;
  					if (chart.trajectory != null) {
  						for (int i=0; i<chart.trajectory.length; i++) {
  							chart.trajectory[i].drawPathColor1 = Picture.invertColor(new Color(chart.background)).getRGB();
  						}
  					}
  	  				if (chartForDragging != null) {
  	  					chartForDragging.setColorMode(chart.getColorMode());
  	  					chartForDragging.drawStarsRealistic = chart.drawStarsRealistic;
  	  					chartForDragging.anaglyphMode = chart.anaglyphMode;
  	  					chartForDragging.planetRender.anaglyphMode = chart.anaglyphMode;
  	  				}
					skyRender.getRenderSkyObject().setSkyRenderElement(chart);
					skyRender.getRenderSkyObject().colorSquemeChanged();
					skyRender.resetGraphics();
	  				paintIt();
	  				colorMode = 8;
				} catch (Exception e1) {
     				Logger.log(LEVEL.ERROR, "Error setting color mode. Message was: "+e1.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e1.getStackTrace()));
				}
  			}
  		});
  		colorSqueme.add(csqItem1);
  		colorSqueme.add(csqItem2);
  		colorSqueme.add(csqItem3);
  		colorSqueme.add(csqItem4);
  		colorSqueme.add(csqItem5);
  		colorSqueme.add(csqItem6);
  		colorSqueme.add(csqItem7);
  		colorSqueme.add(csqItem8);
  		colorSqueme.add(csqItem9);
  		menu.add(colorSqueme);

  		String tel[] = new String[] {
  				// none, refractor/newton, mak
  				Translate.translate(1301), Translate.translate(67)+"/Newton", "Mak/SC"
  		};
  		final TelescopeElement tels[] = new TelescopeElement[] {
  				TelescopeElement.BINOCULARS_7x50,
  				TelescopeElement.REFRACTOR_10cm,
  				TelescopeElement.SCHMIDT_CASSEGRAIN_20cm
  		};
  		JMenu telescopeMenu = new JMenu(Translate.translate(478));
  		for (int i=0; i<tel.length; i++) {
  			if (chart.telescope.invertHorizontal == tels[i].invertHorizontal &&
  					chart.telescope.invertVertical == tels[i].invertVertical)
  				telescopeMenu.setText(telescopeMenu.getText()+" ("+tel[i]+")");
  			final int fi = i;
  			JMenuItem item = new JMenuItem(tel[i]);
 	  		item.addActionListener(new ActionListener() {
  	  			public void actionPerformed(ActionEvent e) {
  	  				chart.telescope.invertHorizontal = tels[fi].invertHorizontal;
  	  				chart.telescope.invertVertical = tels[fi].invertVertical;

  	  				AWTGraphics g = new AWTGraphics(100, 100, false, false);
  	  				g.clearDataBase();
  	  				try {
						skyRender.getRenderSkyObject().setSkyRenderElement(chart);
					} catch (JPARSECException e1) {
						e1.printStackTrace();
					}
  	  				skyRender.getRenderSkyObject().colorSquemeChanged();
  	  				skyRender.getRenderSkyObject().resetLeyend(false);
  	  				RenderPlanet.dateChanged();
  	  				paintIt();
  	  			}
 	  		});
 	  		telescopeMenu.add(item);
  		}
  		if (showCompleteTelescopeMenu) {
	  		telescopeMenu.addSeparator();
			JMenu telMenu = new JMenu(Translate.translate(478));
			final TelescopeElement telescope[] = TelescopeElement.getAllAvailableTelescopes();
			for (int i=0; i<telescope.length; i++) {
				final int index = i;
	 			JMenuItem item = new JMenuItem(telescope[i].name);
	 			item.addActionListener(new ActionListener() {
		  			public void actionPerformed(ActionEvent e) {
		  				skyRender.getRenderSkyObject().render.telescope = telescope[index];
		  				skyRender.getRenderSkyObject().render.drawOcularFieldOfView = true;
		  				chart.drawOcularFieldOfView = true;
		  				chart.telescope = telescope[index];
		  				try {
							skyRender.getRenderSkyObject().setSkyRenderElement(skyRender.getRenderSkyObject().render);
						} catch (JPARSECException e1) {
							e1.printStackTrace();
						}
		  				try {
							skyRender.getRenderSkyObject().resetOriginalTelescope();
						} catch (JPARSECException e1) {
							e1.printStackTrace();
						}
	  	  				paintIt();
		  			}
		  		});
	 			telMenu.add(item);
			}
			telescopeMenu.add(telMenu);
			
			JMenu oclMenu = new JMenu(Translate.translate(480));
			final OcularElement ocular[] = OcularElement.getAllAvailableOculars();
			for (int i=0; i<ocular.length; i++) {
				final int index = i;
	 			JMenuItem item = new JMenuItem(ocular[i].name);
	 			item.addActionListener(new ActionListener() {
		  			public void actionPerformed(ActionEvent e) {
		  				skyRender.getRenderSkyObject().render.telescope.attachCCDCamera(null);
		  				chart.telescope.attachCCDCamera(null);
		  				skyRender.getRenderSkyObject().render.telescope.ocular = ocular[index];
		  				skyRender.getRenderSkyObject().render.drawOcularFieldOfView = true;
		  				chart.drawOcularFieldOfView = true;
		  				chart.telescope.ocular = ocular[index];
		  				try {
							skyRender.getRenderSkyObject().setSkyRenderElement(skyRender.getRenderSkyObject().render);
						} catch (JPARSECException e1) {
							e1.printStackTrace();
						}
		  				try {
							skyRender.getRenderSkyObject().resetOriginalTelescope();
						} catch (JPARSECException e1) {
							e1.printStackTrace();
						}
	  	  				paintIt();
		  			}
		  		});
	 			oclMenu.add(item);
			}
			telescopeMenu.add(oclMenu);
			JMenu camMenu = new JMenu(Translate.translate(1173));
			final CCDElement ccd[] = CCDElement.getAllAvailableCCDs();
			for (int i=0; i<ccd.length; i++) {
				final int index = i;
	 			JMenuItem item = new JMenuItem(ccd[i].name);
	 			item.addActionListener(new ActionListener() {
		  			public void actionPerformed(ActionEvent e) {
		  				OcularElement ocl = null;
		  				if (skyRender.getRenderSkyObject().render.telescope.ocular != null)
		  					ocl = skyRender.getRenderSkyObject().render.telescope.ocular.clone();
		  				skyRender.getRenderSkyObject().render.telescope.attachCCDCamera(ccd[index]);
		  				skyRender.getRenderSkyObject().render.telescope.ocular = ocl;
		  				skyRender.getRenderSkyObject().render.drawOcularFieldOfView = true;
		  				chart.telescope.attachCCDCamera(ccd[index]);
		  				chart.telescope.ocular = ocl;
		  				chart.drawOcularFieldOfView = true;
		  				try {
							skyRender.getRenderSkyObject().setSkyRenderElement(skyRender.getRenderSkyObject().render);
						} catch (JPARSECException e1) {
							e1.printStackTrace();
						}
	  	  				paintIt();
		  			}
		  		});
	 			camMenu.add(item);
			}
			telescopeMenu.add(camMenu);

			JMenu camPAMenu = new JMenu(Translate.translate(336));
			final String pa[] = new String[] {"0", "15", "30", "45", "60", "75", "90", "-15", "-30", "-45", "-60", "-75"};
			for (int i=0; i<pa.length; i++) {
				final int index = i;
	 			JMenuItem item = new JMenuItem(pa[i]);
	 			item.addActionListener(new ActionListener() {
		  			public void actionPerformed(ActionEvent e) {
		  				if (skyRender.getRenderSkyObject().render.telescope.ccd != null) {
			  				skyRender.getRenderSkyObject().render.telescope.ccd.cameraPA = Integer.parseInt(pa[index]);
			  				skyRender.getRenderSkyObject().render.drawOcularFieldOfView = true;
			  				chart.telescope.ccd.cameraPA = (float) (Integer.parseInt(pa[index]) * Constant.DEG_TO_RAD);
			  				chart.drawOcularFieldOfView = true;
			  				try {
								skyRender.getRenderSkyObject().setSkyRenderElement(skyRender.getRenderSkyObject().render);
							} catch (JPARSECException e1) {
								e1.printStackTrace();
							}
		  	  				paintIt();
		  				}
		  			}
		  		});
	 			camPAMenu.add(item);
			}
			telescopeMenu.add(camPAMenu);

			JMenu barlowMenu = new JMenu(Translate.translate(1302));
			final String barlow[] = new String[] {"1.5", "2.0", "2.5", "3", "5", "0.75", "0.5", "0.25"};
			for (int i=0; i<barlow.length; i++) {
				final int index = i;
	 			JMenuItem item = new JMenuItem(barlow[i]);
	 			item.addActionListener(new ActionListener() {
		  			public void actionPerformed(ActionEvent e) {
		  				if (skyRender.getRenderSkyObject().render.telescope != null) {
		  					double factor = Double.parseDouble(barlow[index]);
			  				skyRender.getRenderSkyObject().render.telescope.focalLength *= factor;
		  					skyRender.getRenderSkyObject().render.telescope.name += " + "+Translate.translate(1302)+" " + barlow[index];
			  				skyRender.getRenderSkyObject().render.drawOcularFieldOfView = true;
			  				chart.telescope.focalLength *= factor;
			  				chart.telescope.name = skyRender.getRenderSkyObject().render.telescope.name;
			  				chart.drawOcularFieldOfView = true;
			  				try {
								skyRender.getRenderSkyObject().setSkyRenderElement(skyRender.getRenderSkyObject().render);
							} catch (JPARSECException e1) {
								e1.printStackTrace();
							}
		  	  				paintIt();
		  				}
		  			}
		  		});
	 			barlowMenu.add(item);
			}
			telescopeMenu.add(barlowMenu);
  		}
		menu.add(telescopeMenu);

/*  		if (invertH || invertV) {
  			boolean enabled = chart.telescope.invertHorizontal || chart.telescope.invertVertical;
  	  		// Show invert image option
  	  		JCheckBox showInvert = new JCheckBox(t1075, enabled);
  	  		showInvert.addActionListener(new ActionListener() {
  	  			public void actionPerformed(ActionEvent e) {
  	  				if (chart.telescope.invertHorizontal || chart.telescope.invertVertical) {
  	  					chart.telescope.invertHorizontal = false;
  	  					chart.telescope.invertVertical = false;
  	  				} else {
  	  					chart.telescope.invertHorizontal = invertH;
  	  					chart.telescope.invertVertical = invertV;
  	  				}
  	  				AWTGraphics g = new AWTGraphics(100, 100, false, false);
  	  				g.clearDataBase();
  	  				try {
						skyRender.getRenderSkyObject().setSkyRenderElement(chart);
					} catch (JPARSECException e1) {
						e1.printStackTrace();
					}
  	  				skyRender.getRenderSkyObject().colorSquemeChanged();
  	  				skyRender.getRenderSkyObject().resetLeyend(false);
  	  				RenderPlanet.dateChanged();
  	  				paintIt();
  	  			}
  	  		});
  	  		menu.add(showInvert);
  		}
*/
  		// Show invert image option
  		JCheckBox showHmode = new JCheckBox(Translate.translate(1298), hMode);
  		showHmode.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				hMode = !hMode;
  			}
  		});
  		menu.add(showHmode);

  		if (showModifyLocTime) menu.addSeparator();

  		// Show sky below horizon
  		JCheckBox skyBelowH = new JCheckBox(Translate.translate(1076), chart.drawSkyBelowHorizon);
  		skyBelowH.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				chart.drawSkyBelowHorizon = !chart.drawSkyBelowHorizon;
  				if (chart.drawSkyBelowHorizon && chart.coordinateSystem != COORDINATE_SYSTEM.HORIZONTAL) {
  					chart.drawSkyCorrectingLocalHorizon = false;
  				} else {
  					chart.drawSkyCorrectingLocalHorizon = true;
  				}
  				try {
					skyRender.getRenderSkyObject().setSkyRenderElement(chart);
				} catch (JPARSECException e1) {
					skyRender.getRenderSkyObject().render = chart;
				}
  				skyRender.getRenderSkyObject().dateChanged(true);
				if (object != null) {
	  				paintIt();
					setCentralObject(object, null);
				}
  				paintIt();
  			}
  		});
  		menu.add(skyBelowH);

  		// Show planets
  		JCheckBox showPlanets = new JCheckBox(Translate.translate(866), chart.drawPlanetsMoonSun);
  		showPlanets.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				chart.drawPlanetsMoonSun = !chart.drawPlanetsMoonSun;
  				if (chartForDragging != null)
  					chartForDragging.drawPlanetsMoonSun = !chartForDragging.drawPlanetsMoonSun;
  				skyRender.getRenderSkyObject().render = chart;
  				skyRender.getRenderSkyObject().resetLeyend(false);
  				paintIt();
  			}
  		});

  		// Show comets
  		JCheckBox showComets = new JCheckBox(Translate.translate(867), chart.drawComets);
  		showComets.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				chart.drawComets = !chart.drawComets;
  				if (chartForDragging != null)
  					chartForDragging.drawComets = !chartForDragging.drawComets;
  				skyRender.getRenderSkyObject().render = chart;
  				skyRender.getRenderSkyObject().resetLeyend(false);
  				paintIt();
  			}
  		});

  		// Show asteroids
  		JCheckBox showAsteroids = new JCheckBox(Translate.translate(868), chart.drawAsteroids);
  		showAsteroids.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				chart.drawAsteroids = !chart.drawAsteroids;
  				if (chartForDragging != null)
  					chartForDragging.drawAsteroids = !chartForDragging.drawAsteroids;
  				skyRender.getRenderSkyObject().render = chart;
  				skyRender.getRenderSkyObject().resetLeyend(false);
  				paintIt();
  			}
  		});

  		// Show probes/sats/transNeps/SNs
  		JCheckBox showOther = new JCheckBox(Translate.translate(869), chart.drawSpaceProbes);
  		showOther.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				chart.drawSpaceProbes = !chart.drawSpaceProbes;
  				chart.drawArtificialSatellites = chart.drawSpaceProbes;
  				chart.drawArtificialSatellitesIridiumFlares = chart.drawSpaceProbes;
  				chart.drawTransNeptunianObjects = chart.drawSpaceProbes;
  				chart.drawSuperNovaAndNovaEvents = chart.drawSpaceProbes;
  				chart.drawMeteorShowers = chart.drawSpaceProbes;
  				chart.planetRender.satellitesAll = chart.drawSpaceProbes;
  				if (chartForDragging != null) {
  					chartForDragging.drawSpaceProbes = !chartForDragging.drawSpaceProbes;
	  	  			chartForDragging.drawArtificialSatellites = chartForDragging.drawSpaceProbes;
	  	  			chartForDragging.drawArtificialSatellitesIridiumFlares = chartForDragging.drawSpaceProbes;
	  	  			chartForDragging.drawTransNeptunianObjects = chartForDragging.drawSpaceProbes;
	  	  			chartForDragging.drawSuperNovaAndNovaEvents = chartForDragging.drawSpaceProbes;
	  	  			chartForDragging.drawMeteorShowers = chartForDragging.drawSpaceProbes;
	  	  			chartForDragging.planetRender.satellitesAll = chartForDragging.drawSpaceProbes;
  				}

  				//skyRender.getRenderSkyObject().render = chart;
  				try {
  					skyRender.getRenderSkyObject().setSkyRenderElement(chart);
  				} catch (JPARSECException e1) { }
  				skyRender.getRenderSkyObject().resetLeyend(false);
  				paintIt();
  			}
  		});

  		// Show satellites
  		JCheckBox showSats = new JCheckBox(Translate.translate(970), chart.planetRender.satellitesMain);
  		showSats.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				chart.planetRender.satellitesMain = !chart.planetRender.satellitesMain;
  				if (chartForDragging != null)
  					chartForDragging.planetRender.satellitesMain = !chartForDragging.planetRender.satellitesMain;
  				skyRender.getRenderSkyObject().render = chart;
  				skyRender.getRenderSkyObject().resetLeyend(false);
  				paintIt();
  			}
  		});

  		JMenu subMenu = new JMenu(Translate.translate(1336));
  		subMenu.add(showPlanets);
  		subMenu.add(showComets);
  		subMenu.add(showAsteroids);
  		subMenu.add(showOther);
  		subMenu.add(showSats);
  		menu.add(subMenu);

  		// Show other catalogs
  		if (chart.getNumberOfExternalCatalogs() >= 0) {
  			JMenu externalMenu = new JMenu(Translate.translate(1081));
  			for (int i=0; i<chart.getNumberOfExternalCatalogs(); i++) {
  		  		JCheckBox showExternal = new JCheckBox(""+(i+1)+" - " + chart.getExternalCatalogName(i), chart.drawExternalCatalogs == null ? true: chart.drawExternalCatalogs[i]);
  		  		showExternal.addActionListener(new ActionListener() {
  		  			public void actionPerformed(ActionEvent e) {
  		  				String name = ((JCheckBox) e.getSource()).getText();
  		  				int index = Integer.parseInt(name.substring(0, name.indexOf("-")).trim()) - 1;
  		  				chart.drawExternalCatalogs[index] = !chart.drawExternalCatalogs[index];
  		  				try {
  							skyRender.getRenderSkyObject().setSkyRenderElement(chart);
  						} catch (JPARSECException e1) { }
  		  				paintIt();
  		  			}
  		  		});
  		  		externalMenu.add(showExternal);
  			}
	  		JCheckBox showDSS = new JCheckBox(""+(chart.getNumberOfExternalCatalogs()+1)+" - "+Translate.translate(1293), skyRender.getRenderSkyObject().render.overlayDSSimageInNextRendering);
	  		showDSS.addActionListener(new ActionListener() {
	  			public void actionPerformed(ActionEvent e) {
	  				chart.overlayDSSimageInNextRendering = !chart.overlayDSSimageInNextRendering;
	  				if (chartForDragging != null)
	  					chartForDragging.overlayDSSimageInNextRendering = !chartForDragging.overlayDSSimageInNextRendering;
	  				try {
						skyRender.getRenderSkyObject().setSkyRenderElement(chart);
					} catch (JPARSECException e1) { }
	  				paintIt();
	  				skyRender.getRenderSkyObject().render.overlayDSSimageInNextRendering = chart.overlayDSSimageInNextRendering;
	  			}
	  		});
	  		externalMenu.add(showDSS);

	  		subMenu.add(externalMenu);
/*
  			String ecadd = "";
  			if (chart.getNumberOfExternalCatalogs() == 1) {
  				ecadd = " ("+chart.getExternalCatalogName(0)+")";
  			}
  			JCheckBox showExternal = new JCheckBox(Translate.translate(1081)+ecadd, chart.drawExternalCatalogs);
	  		showExternal.addActionListener(new ActionListener() {
	  			public void actionPerformed(ActionEvent e) {
	  				chart.drawExternalCatalogs = !chart.drawExternalCatalogs;
	  				try {
						skyRender.getRenderSkyObject().setSkyRenderElement(chart);
					} catch (JPARSECException e1) { }
	  				paintIt();
	  			}
	  		});
	  		subMenu.add(showExternal);
*/
  		}
  		//menu.addSeparator();

  		// Draw grid
		JCheckBox showGrid = new JCheckBox(Translate.translate(870), chart.drawCoordinateGrid);
		showGrid.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				chart.drawCoordinateGrid = !chart.drawCoordinateGrid;
  				chart.drawExternalGrid = false;
  				if (chart.drawCoordinateGrid)
  					chart.drawExternalGrid = true;
  				if (chartForDragging != null) {
  					chartForDragging.drawCoordinateGrid = chart.drawCoordinateGrid;
  					chartForDragging.drawExternalGrid = chart.drawExternalGrid;
  				}
  				skyRender.resetGraphics();
  				skyRender.getRenderSkyObject().render = chart;
  				skyRender.getRenderSkyObject().resetLeyend(false);
  				skyRender.getRenderSkyObject().colorSquemeChanged();
  				update();
  				paintIt();
  			}
  		});

  		// Draw constellation limits
		JCheckBox showLimits = new JCheckBox(Translate.translate(871), chart.drawConstellationLimits);
		showLimits.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				chart.drawConstellationLimits = !chart.drawConstellationLimits;
  				if (chartForDragging != null)
  					chartForDragging.drawConstellationLimits = chart.drawConstellationLimits;
  				skyRender.getRenderSkyObject().render = chart;
  				skyRender.getRenderSkyObject().resetLeyend(false);
  				paintIt();
  			}
  		});

  		// Draw nebula/milky way
		JCheckBox showNebula = new JCheckBox(Translate.translate(872), chart.drawMilkyWayContours);
		showNebula.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				if (chart.drawMilkyWayContours && chart.drawNebulaeContours) {
  					chart.drawNebulaeContours = false;
  				} else {
  	  				if (chart.drawMilkyWayContours && !chart.drawNebulaeContours) {
  	  					chart.drawMilkyWayContours = false;
  	  				} else {
  	  	  				if (!chart.drawMilkyWayContours && !chart.drawNebulaeContours) {
  	    					chart.drawNebulaeContours = true;
  	  	  				} else {
  	  	  	  				if (!chart.drawMilkyWayContours && chart.drawNebulaeContours) {
  	    	  					chart.drawMilkyWayContours = true;
  	  	  	  				}
  	  	  				}
  	  				}
  				}
  				if (chartForDragging != null) {
  					chartForDragging.drawMilkyWayContours = chart.drawMilkyWayContours;
  					chartForDragging.drawNebulaeContours = chart.drawNebulaeContours;
  				}
  				skyRender.getRenderSkyObject().render = chart;
  				skyRender.getRenderSkyObject().resetLeyend(false);
  				paintIt();
  			}
  		});

  		// Draw leyend
		JCheckBox showLeyend = new JCheckBox(Translate.translate(574), chart.drawLeyend != LEYEND_POSITION.NO_LEYEND);
		showLeyend.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				if (chart.drawLeyend == LEYEND_POSITION.NO_LEYEND) {
  					chart.drawLeyend = LEYEND_POSITION.TOP;
  				} else {
  					chart.drawLeyend = LEYEND_POSITION.NO_LEYEND;
  				}
  				originalShowLegend = chart.drawLeyend;
  				if (chartForDragging != null)
  					chartForDragging.drawLeyend = chart.drawLeyend;
  				try {
					skyRender.getRenderSkyObject().setSkyRenderElement(chart);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
  				skyRender.getRenderSkyObject().resetLeyend(false);
  				paintIt();
  			}
  		});

  		// Draw labels
		JCheckBox showLabels = new JCheckBox(Translate.translate(873), chart.drawDeepSkyObjectsLabels);
		showLabels.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				chart.drawDeepSkyObjectsLabels = !chart.drawDeepSkyObjectsLabels;
  				chart.drawCoordinateGridLabels = chart.drawDeepSkyObjectsLabels;
  				chart.drawStarsLabels = chart.drawDeepSkyObjectsLabels ? STAR_LABELS.ONLY_PROPER_NAME:STAR_LABELS.NONE;
  				chart.setStarLabelsAccordingtoCurrentLanguage();
  				chart.drawMinorObjectsLabels = chart.drawDeepSkyObjectsLabels;
  				chart.drawPlanetsLabels = chart.drawDeepSkyObjectsLabels;
  				chart.drawSunSpotsLabels = chart.drawDeepSkyObjectsLabels;
  				chart.drawConstellationNames = chart.drawDeepSkyObjectsLabels;
  				if (chartForDragging != null) {
  					chartForDragging.drawDeepSkyObjectsLabels = chart.drawDeepSkyObjectsLabels;
  					chartForDragging.drawCoordinateGridLabels = chart.drawDeepSkyObjectsLabels;
  					chartForDragging.drawStarsLabels = chart.drawDeepSkyObjectsLabels ? STAR_LABELS.ONLY_PROPER_NAME:STAR_LABELS.NONE;
  					chartForDragging.setStarLabelsAccordingtoCurrentLanguage();
  					chartForDragging.drawMinorObjectsLabels = chart.drawDeepSkyObjectsLabels;
  					chartForDragging.drawPlanetsLabels = chart.drawDeepSkyObjectsLabels;
  					chartForDragging.drawSunSpotsLabels = chart.drawDeepSkyObjectsLabels;
  					chartForDragging.drawConstellationNames = chart.drawDeepSkyObjectsLabels;
  				}
  				skyRender.getRenderSkyObject().render = chart;
  				skyRender.getRenderSkyObject().resetLeyend(false);
  				paintIt();
  			}
  		});

  		add = " ("+Translate.translate(996)+")";
  		if (showTextures) {
  			if (RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR == 0.25f) add = " ("+Translate.translate(999)+")";
  			if (RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR == 0.5f) add = " ("+Translate.translate(995)+")";
  			if (RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR == 1.5f) add = " ("+Translate.translate(997)+")";
  			if (RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR == 2f) add = " ("+Translate.translate(998)+")";
  		} else {
  			add = " ("+Translate.translate(994).toLowerCase()+")";
  		}
  		JMenu textureQ = new JMenu(Translate.translate(993)+add);
  		JMenuItem tqItem0 = new JMenuItem(Translate.translate(994));
  		JMenuItem tqItem1 = new JMenuItem(Translate.translate(995));
  		JMenuItem tqItem2 = new JMenuItem(Translate.translate(996));
  		JMenuItem tqItem3 = new JMenuItem(Translate.translate(997));
  		JMenuItem tqItem4 = new JMenuItem(Translate.translate(998));
  		JMenuItem tqItem5 = new JMenuItem(Translate.translate(999));
  		tqItem0.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				showTextures = false;
				chart.planetRender.textures = showTextures;
				chart.drawStarsRealistic = REALISTIC_STARS.NONE;
				showTexturedMW = MILKY_WAY_TEXTURE.NO_TEXTURE;
				chart.drawMilkyWayContoursWithTextures = showTexturedMW;
				chart.drawDeepSkyObjectsTextures = showTexturedObj = false;
				skyRender.getRenderSkyObject().render = chart;
				//if (chart.planetRender.highQuality) chart.drawStarsRealistic = REALISTIC_STARS.NONE_CUTE;
				RenderPlanet.dateChanged();
				if (chartForDragging != null) {
					chartForDragging.drawStarsRealistic = chart.drawStarsRealistic;
					chartForDragging.planetRender.textures = showTextures;
					chartForDragging.drawDeepSkyObjectsTextures = chart.drawDeepSkyObjectsTextures;
				}
				skyRender.getRenderSkyObject().resetLeyend(true);
				paintIt();
  			}
  		});
  		tqItem1.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				showTextures = true;
				chart.planetRender.textures = showTextures;
				chart.planetRender.highQuality = true;
				chart.drawStarsRealistic = REALISTIC_STARS.DIFFUSED;
				showTexturedMW = MILKY_WAY_TEXTURE.NO_TEXTURE;
				chart.drawMilkyWayContoursWithTextures = showTexturedMW;
				chart.drawDeepSkyObjectsTextures = showTexturedObj = false;
				//chart.drawDeepSkyObjectsTextures = showTexturedObj;
				RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR = 0.5f;
				skyRender.getRenderSkyObject().render = chart;
				RenderPlanet.dateChanged();
				if (chartForDragging != null) {
					chartForDragging.drawStarsRealistic = chart.drawStarsRealistic;
					chartForDragging.planetRender.textures = showTextures;
					chartForDragging.drawDeepSkyObjectsTextures = chart.drawDeepSkyObjectsTextures;
				}
				skyRender.getRenderSkyObject().resetLeyend(true);
				paintIt();
  			}
  		});
  		tqItem2.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				showTextures = true;
				chart.planetRender.textures = showTextures;
				chart.planetRender.highQuality = false;
				chart.drawStarsRealistic = REALISTIC_STARS.STARRED;
				showTexturedMW = MILKY_WAY_TEXTURE.NO_TEXTURE;
				chart.drawMilkyWayContoursWithTextures = showTexturedMW;
				chart.drawDeepSkyObjectsTextures = showTexturedObj = true;
				RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR = 1f;
				skyRender.getRenderSkyObject().render = chart;
				RenderPlanet.dateChanged();
				if (chartForDragging != null) {
					chartForDragging.drawStarsRealistic = chart.drawStarsRealistic;
					chartForDragging.planetRender.textures = showTextures;
					chartForDragging.drawDeepSkyObjectsTextures = chart.drawDeepSkyObjectsTextures;
				}
				skyRender.getRenderSkyObject().resetLeyend(true);
				paintIt();
  			}
  		});
  		tqItem3.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				showTextures = true;
				chart.planetRender.textures = showTextures;
				chart.planetRender.highQuality = true;
				chart.drawStarsRealistic = REALISTIC_STARS.STARRED;
				chart.drawMilkyWayContoursWithTextures = showTexturedMW;
				if (showTexturedMW == MILKY_WAY_TEXTURE.NO_TEXTURE)
					chart.drawMilkyWayContoursWithTextures = MILKY_WAY_TEXTURE.OPTICAL;
				chart.drawDeepSkyObjectsTextures = showTexturedObj = true;
				RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR = 1.5f;
				skyRender.getRenderSkyObject().render = chart;
				RenderPlanet.dateChanged();
				if (chartForDragging != null) {
					chartForDragging.drawStarsRealistic = chart.drawStarsRealistic;
					chartForDragging.planetRender.textures = showTextures;
					chartForDragging.drawDeepSkyObjectsTextures = chart.drawDeepSkyObjectsTextures;
				}
				skyRender.getRenderSkyObject().resetLeyend(true);
				paintIt();
  			}
  		});
  		tqItem4.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				showTextures = true;
				chart.planetRender.textures = showTextures;
				chart.planetRender.highQuality = true;
				chart.drawStarsRealistic = REALISTIC_STARS.STARRED;
				chart.drawMilkyWayContoursWithTextures = showTexturedMW;
				if (showTexturedMW == MILKY_WAY_TEXTURE.NO_TEXTURE)
					chart.drawMilkyWayContoursWithTextures = MILKY_WAY_TEXTURE.OPTICAL;
				chart.drawDeepSkyObjectsTextures = showTexturedObj = true;
				RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR = 2f;
				skyRender.getRenderSkyObject().render = chart;
				RenderPlanet.dateChanged();
				if (chartForDragging != null) {
					chartForDragging.drawStarsRealistic = chart.drawStarsRealistic;
					chartForDragging.planetRender.textures = showTextures;
					chartForDragging.drawDeepSkyObjectsTextures = chart.drawDeepSkyObjectsTextures;
				}
				skyRender.getRenderSkyObject().resetLeyend(true);
				paintIt();
  			}
  		});
  		tqItem5.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				showTextures = true;
				chart.planetRender.textures = showTextures;
				chart.planetRender.highQuality = true;
				showTexturedMW = MILKY_WAY_TEXTURE.NO_TEXTURE;
				chart.drawMilkyWayContoursWithTextures = showTexturedMW;
				chart.drawDeepSkyObjectsTextures = showTexturedObj = false;
				//chart.drawDeepSkyObjectsTextures = showTexturedObj;
				RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR = 0.25f;
				chart.drawStarsRealistic = REALISTIC_STARS.SPIKED;
				skyRender.getRenderSkyObject().render = chart;
				RenderPlanet.dateChanged();
				if (chartForDragging != null) {
					chartForDragging.drawStarsRealistic = chart.drawStarsRealistic;
					chartForDragging.planetRender.textures = showTextures;
					chartForDragging.drawDeepSkyObjectsTextures = chart.drawDeepSkyObjectsTextures;
				}
				skyRender.getRenderSkyObject().resetLeyend(true);
				paintIt();
  			}
  		});
  		textureQ.add(tqItem0);
  		textureQ.add(tqItem5);
  		textureQ.add(tqItem1);
  		textureQ.add(tqItem2);
  		textureQ.add(tqItem3);
  		textureQ.add(tqItem4);
  		
  		JMenu subMenu2 = new JMenu(Translate.translate(1337));
  		subMenu2.add(showGrid);
  		subMenu2.add(showLimits);
  		subMenu2.add(showNebula);
  		subMenu2.add(showLeyend);
  		subMenu2.add(showLabels);
  		subMenu2.add(textureQ);
  		menu.add(subMenu2);
  		menu.addSeparator();

  		// maglim
  		add = " ("+Functions.formatValue(chart.drawStarsLimitingMagnitude, 1)+")";
  		if (!chart.drawStars) add = " ("+Translate.translate(898).toLowerCase()+")";
  		JMenu starLim = new JMenu(Translate.translate(874)+add);
  		JMenuItem slItem0 = new JMenuItem(Translate.translate(898));
  		JMenuItem slItem1 = new JMenuItem("6.0");
  		JMenuItem slItem2 = new JMenuItem("7.0");
  		JMenuItem slItem3 = new JMenuItem("8.5");
  		JMenuItem slItem4 = new JMenuItem("9.5");
  		JMenuItem slItem4b = new JMenuItem("10.5");
  		JMenuItem slItem5 = new JMenuItem("12.0");
  		JMenuItem slItem6 = new JMenuItem("14.0");
  		JMenuItem slItem7 = new JMenuItem("16.0");
  		slItem0.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
				chart.drawStars = false;
				chart.drawConstellationContours = CONSTELLATION_CONTOUR.NONE;
  				if (chartForDragging != null) {
  					chartForDragging.drawStars = chart.drawStars;
  					chartForDragging.drawConstellationContours = chart.drawConstellationContours;
  				}
				skyRender.getRenderSkyObject().render = chart;
				skyRender.getRenderSkyObject().dateChanged(true);
				paintIt();
  			}
  		});
  		slItem1.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
				chart.drawStars = true;
				chart.drawConstellationContours = CONSTELLATION_CONTOUR.DEFAULT;
				chart.drawStarsLimitingMagnitude = 6;
  				if (chartForDragging != null) {
  					chartForDragging.drawStars = chart.drawStars;
  					chartForDragging.drawConstellationContours = chart.drawConstellationContours;
  					chartForDragging.drawStarsLimitingMagnitude = chart.drawStarsLimitingMagnitude;
  				}
  				skyRender.getRenderSkyObject().render = chart;
				skyRender.getRenderSkyObject().dateChanged(true);
  				paintIt();
  			}
  		});
  		slItem2.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
				chart.drawStars = true;
				chart.drawConstellationContours = CONSTELLATION_CONTOUR.DEFAULT;
				chart.drawStarsLimitingMagnitude = 7;
  				if (chartForDragging != null) {
  					chartForDragging.drawStars = chart.drawStars;
  					chartForDragging.drawConstellationContours = chart.drawConstellationContours;
  					chartForDragging.drawStarsLimitingMagnitude = chart.drawStarsLimitingMagnitude;
  				}
  				skyRender.getRenderSkyObject().render = chart;
				skyRender.getRenderSkyObject().dateChanged(true);
  				paintIt();
  			}
  		});
  		slItem3.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
				chart.drawStars = true;
				chart.drawConstellationContours = CONSTELLATION_CONTOUR.DEFAULT;
				chart.drawStarsLimitingMagnitude = 8.5f;
  				if (chartForDragging != null) {
  					chartForDragging.drawStars = chart.drawStars;
  					chartForDragging.drawConstellationContours = chart.drawConstellationContours;
  					chartForDragging.drawStarsLimitingMagnitude = chart.drawStarsLimitingMagnitude;
  				}
  				skyRender.getRenderSkyObject().render = chart;
				skyRender.getRenderSkyObject().dateChanged(true);
  				paintIt();
  			}
  		});
  		slItem4.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
				chart.drawStars = true;
				chart.drawConstellationContours = CONSTELLATION_CONTOUR.DEFAULT;
				chart.drawStarsLimitingMagnitude = 9.5f;
  				if (chartForDragging != null) {
  					chartForDragging.drawStars = chart.drawStars;
  					chartForDragging.drawConstellationContours = chart.drawConstellationContours;
  					chartForDragging.drawStarsLimitingMagnitude = chart.drawStarsLimitingMagnitude;
  				}
  				skyRender.getRenderSkyObject().render = chart;
				skyRender.getRenderSkyObject().dateChanged(true);
  				paintIt();
  			}
  		});
  		slItem4b.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
				chart.drawStars = true;
				chart.drawConstellationContours = CONSTELLATION_CONTOUR.DEFAULT;
				chart.drawStarsLimitingMagnitude = 10.5f;
  				if (chartForDragging != null) {
  					chartForDragging.drawStars = chart.drawStars;
  					chartForDragging.drawConstellationContours = chart.drawConstellationContours;
  					chartForDragging.drawStarsLimitingMagnitude = chart.drawStarsLimitingMagnitude;
  				}
  				skyRender.getRenderSkyObject().render = chart;
				skyRender.getRenderSkyObject().dateChanged(true);
  				paintIt();
  			}
  		});
  		slItem5.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
				chart.drawStars = true;
				chart.drawConstellationContours = CONSTELLATION_CONTOUR.DEFAULT;
				chart.drawStarsLimitingMagnitude = 12;
				chart.drawFaintStars = true;
  				if (chartForDragging != null) {
  					chartForDragging.drawStars = chart.drawStars;
  					chartForDragging.drawConstellationContours = chart.drawConstellationContours;
  					chartForDragging.drawStarsLimitingMagnitude = chart.drawStarsLimitingMagnitude;
  					chartForDragging.drawFaintStars = chart.drawFaintStars;
  				}
  				skyRender.getRenderSkyObject().render = chart;
				skyRender.getRenderSkyObject().dateChanged(true);
  				paintIt();
  			}
  		});
  		slItem6.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
				chart.drawStars = true;
				chart.drawConstellationContours = CONSTELLATION_CONTOUR.DEFAULT;
				chart.drawStarsLimitingMagnitude = 14;
				chart.drawFaintStars = true;
  				if (chartForDragging != null) {
  					chartForDragging.drawStars = chart.drawStars;
  					chartForDragging.drawConstellationContours = chart.drawConstellationContours;
  					chartForDragging.drawStarsLimitingMagnitude = chart.drawStarsLimitingMagnitude;
  					chartForDragging.drawFaintStars = chart.drawFaintStars;
  				}
  				skyRender.getRenderSkyObject().render = chart;
				skyRender.getRenderSkyObject().dateChanged(true);
  				paintIt();
  			}
  		});
  		slItem7.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
				chart.drawStars = true;
				chart.drawConstellationContours = CONSTELLATION_CONTOUR.DEFAULT;
				chart.drawStarsLimitingMagnitude = 16;
				chart.drawFaintStars = true;
  				if (chartForDragging != null) {
  					chartForDragging.drawStars = chart.drawStars;
  					chartForDragging.drawConstellationContours = chart.drawConstellationContours;
  					chartForDragging.drawStarsLimitingMagnitude = chart.drawStarsLimitingMagnitude;
  					chartForDragging.drawFaintStars = chart.drawFaintStars;
  				}
  				skyRender.getRenderSkyObject().render = chart;
				skyRender.getRenderSkyObject().dateChanged(true);
  				paintIt();
  			}
  		});
  		starLim.add(slItem0);
  		starLim.add(slItem1);
  		starLim.add(slItem2);
  		starLim.add(slItem3);
  		starLim.add(slItem4);
  		starLim.add(slItem4b);
  		starLim.add(slItem5);
  		starLim.add(slItem6);
  		starLim.add(slItem7);
  		menu.add(starLim);

  		// maglim objects
  		add = " ("+Functions.formatValue(Math.abs(chart.drawObjectsLimitingMagnitude), 1)+")";
  		if (Math.abs(chart.drawObjectsLimitingMagnitude) > 16) add = " ("+Translate.translate(1292)+")";
  		if (!chart.drawDeepSkyObjects) add = " ("+Translate.translate(901).toLowerCase()+")";
  		JMenu objLim = new JMenu(Translate.translate(875)+add);
  		JMenuItem olItem0 = new JMenuItem(Translate.translate(901));
  		JMenuItem olItem1 = new JMenuItem("7.0");
  		JMenuItem olItem2 = new JMenuItem("8.0");
  		JMenuItem olItem3 = new JMenuItem("9.0");
  		JMenuItem olItem4 = new JMenuItem("10.0");
  		JMenuItem olItem5 = new JMenuItem("12.0");
  		JMenuItem olItem6 = new JMenuItem("14.0");
  		JMenuItem olItem7 = new JMenuItem("16.0");
  		JMenuItem olItem8 = new JMenuItem(Translate.translate(1292));
  		olItem0.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				chart.drawDeepSkyObjects = false;
  				if (chartForDragging != null)
  					chartForDragging.drawDeepSkyObjects = chart.drawDeepSkyObjects;
				skyRender.getRenderSkyObject().render = chart;
  				skyRender.getRenderSkyObject().resetLeyend(false);
				paintIt();
  			}
  		});
  		olItem1.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				chart.drawDeepSkyObjects = true;
  				chart.drawObjectsLimitingMagnitude = 7;
  				chart.drawMinorObjectsLimitingMagnitude = chart.drawObjectsLimitingMagnitude;
  				if (chartForDragging != null) {
  					chartForDragging.drawDeepSkyObjects = chart.drawDeepSkyObjects;
  					chartForDragging.drawObjectsLimitingMagnitude = chart.drawObjectsLimitingMagnitude;
  					chartForDragging.drawMinorObjectsLimitingMagnitude = chart.drawMinorObjectsLimitingMagnitude;
  				}
				skyRender.getRenderSkyObject().render = chart;
  				skyRender.getRenderSkyObject().resetLeyend(false);
				paintIt();
  			}
  		});
  		olItem2.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				chart.drawDeepSkyObjects = true;
  				chart.drawObjectsLimitingMagnitude = 8;
  				chart.drawMinorObjectsLimitingMagnitude = chart.drawObjectsLimitingMagnitude;
  				if (chartForDragging != null) {
  					chartForDragging.drawDeepSkyObjects = chart.drawDeepSkyObjects;
  					chartForDragging.drawObjectsLimitingMagnitude = chart.drawObjectsLimitingMagnitude;
  					chartForDragging.drawMinorObjectsLimitingMagnitude = chart.drawMinorObjectsLimitingMagnitude;
  				}
				skyRender.getRenderSkyObject().render = chart;
  				skyRender.getRenderSkyObject().resetLeyend(false);
				paintIt();
  			}
  		});
  		olItem3.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				chart.drawDeepSkyObjects = true;
  				chart.drawObjectsLimitingMagnitude = 9;
  				chart.drawMinorObjectsLimitingMagnitude = chart.drawObjectsLimitingMagnitude;
  				if (chartForDragging != null) {
  					chartForDragging.drawDeepSkyObjects = chart.drawDeepSkyObjects;
  					chartForDragging.drawObjectsLimitingMagnitude = chart.drawObjectsLimitingMagnitude;
  					chartForDragging.drawMinorObjectsLimitingMagnitude = chart.drawMinorObjectsLimitingMagnitude;
  				}
				skyRender.getRenderSkyObject().render = chart;
  				skyRender.getRenderSkyObject().resetLeyend(false);
				paintIt();
  			}
  		});
  		olItem4.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				chart.drawDeepSkyObjects = true;
  				chart.drawObjectsLimitingMagnitude = 10;
  				chart.drawMinorObjectsLimitingMagnitude = chart.drawObjectsLimitingMagnitude;
  				if (chartForDragging != null) {
  					chartForDragging.drawDeepSkyObjects = chart.drawDeepSkyObjects;
  					chartForDragging.drawObjectsLimitingMagnitude = chart.drawObjectsLimitingMagnitude;
  					chartForDragging.drawMinorObjectsLimitingMagnitude = chart.drawMinorObjectsLimitingMagnitude;
  				}
				skyRender.getRenderSkyObject().render = chart;
  				skyRender.getRenderSkyObject().resetLeyend(false);
				paintIt();
  			}
  		});
  		olItem5.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				chart.drawDeepSkyObjects = true;
  				chart.drawObjectsLimitingMagnitude = 12;
  				chart.drawMinorObjectsLimitingMagnitude = chart.drawObjectsLimitingMagnitude;
  				if (chartForDragging != null) {
  					chartForDragging.drawDeepSkyObjects = chart.drawDeepSkyObjects;
  					chartForDragging.drawObjectsLimitingMagnitude = chart.drawObjectsLimitingMagnitude;
  					chartForDragging.drawMinorObjectsLimitingMagnitude = chart.drawMinorObjectsLimitingMagnitude;
  				}
				skyRender.getRenderSkyObject().render = chart;
  				skyRender.getRenderSkyObject().resetLeyend(false);
				paintIt();
  			}
  		});
  		olItem6.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				chart.drawDeepSkyObjects = true;
  				chart.drawObjectsLimitingMagnitude = 14;
  				chart.drawMinorObjectsLimitingMagnitude = chart.drawObjectsLimitingMagnitude;
  				if (chartForDragging != null) {
  					chartForDragging.drawDeepSkyObjects = chart.drawDeepSkyObjects;
  					chartForDragging.drawObjectsLimitingMagnitude = chart.drawObjectsLimitingMagnitude;
  					chartForDragging.drawMinorObjectsLimitingMagnitude = chart.drawMinorObjectsLimitingMagnitude;
  				}
				skyRender.getRenderSkyObject().render = chart;
  				skyRender.getRenderSkyObject().resetLeyend(false);
				paintIt();
  			}
  		});
  		olItem7.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				chart.drawDeepSkyObjects = true;
  				chart.drawObjectsLimitingMagnitude = 16;
  				chart.drawMinorObjectsLimitingMagnitude = chart.drawObjectsLimitingMagnitude;
  				if (chartForDragging != null) {
  					chartForDragging.drawDeepSkyObjects = chart.drawDeepSkyObjects;
  					chartForDragging.drawObjectsLimitingMagnitude = chart.drawObjectsLimitingMagnitude;
  					chartForDragging.drawMinorObjectsLimitingMagnitude = chart.drawMinorObjectsLimitingMagnitude;
  				}
				skyRender.getRenderSkyObject().render = chart;
  				skyRender.getRenderSkyObject().resetLeyend(false);
				paintIt();
  			}
  		});
  		olItem8.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				chart.drawDeepSkyObjects = true;
  				chart.drawObjectsLimitingMagnitude = 200;
  				chart.drawMinorObjectsLimitingMagnitude = chart.drawObjectsLimitingMagnitude;
  				if (chartForDragging != null) {
  					chartForDragging.drawDeepSkyObjects = chart.drawDeepSkyObjects;
  					chartForDragging.drawObjectsLimitingMagnitude = chart.drawObjectsLimitingMagnitude;
  					chartForDragging.drawMinorObjectsLimitingMagnitude = chart.drawMinorObjectsLimitingMagnitude;
  				}
				skyRender.getRenderSkyObject().render = chart;
  				skyRender.getRenderSkyObject().resetLeyend(false);
				paintIt();
  			}
  		});
  		objLim.add(olItem0);
  		objLim.add(olItem1);
  		objLim.add(olItem2);
  		objLim.add(olItem3);
  		objLim.add(olItem4);
  		objLim.add(olItem5);
  		objLim.add(olItem6);
  		objLim.add(olItem7);
  		objLim.add(olItem8);
  		menu.add(objLim);

  		if (feed != null && showEvents) {
  			ArrayList<FeedMessageElement> list = feed.getMessages();
  			String menuTitle = replaceVars(Translate.translate(1074), new String[] {"%year"}, new String[] {""+YEAR});
  			JMenu event = new JMenu(menuTitle);
  			JMenu month[] = new JMenu[12];
  			for (int i=0; i<12; i++) {
  				month[i] = new JMenu(CalendarGenericConversion.getMonthName(i+1, CALENDAR.GREGORIAN));
  				event.add(month[i]);
  			}
  			for (int i=0; i<list.size(); i++) {
  				FeedMessageElement mes = list.get(i);
  				final String etitle = mes.title+" ("+mes.description+")";
  				JMenuItem e = new JMenuItem(etitle);

  				String edate = FileIO.getTextBeforeField(3, mes.description, " ", true).trim();
  				if (!edate.equals("")) {
  	  				boolean bc = false;
	  				if (edate.startsWith("-")) {
	  					bc = true;
	  					edate = edate.substring(1);
	  				}
	  				String etime = FileIO.getField(2, edate, " ", true);
	  				edate = FileIO.getField(1, edate, " ", true);
	  				int yy = Integer.parseInt(FileIO.getField(1, edate, "-", false));
	  				int mm = Integer.parseInt(FileIO.getField(2, edate, "-", false));
	  				int dd = Integer.parseInt(FileIO.getField(3, edate, "-", false));
	  				int hh = Integer.parseInt(FileIO.getField(1, etime, ":", false));
	  				int mi = Integer.parseInt(FileIO.getField(2, etime, ":", false));
	  				if (bc) yy = -yy;
	  				final AstroDate astroDate = new AstroDate(yy, mm, dd, hh, mi, 0);

	  				month[mm-1].add(e);
	  		 		e.addActionListener(new ActionListener() {
	  		  			public void actionPerformed(ActionEvent e) {
	  		  				try {
		  		  				String s = null;
		  		  				if (etitle.indexOf(TARGET.MERCURY.getName()) >= 0) s = TARGET.MERCURY.getName();
		  		  				if (etitle.indexOf(TARGET.VENUS.getName()) >= 0) s = TARGET.VENUS.getName();
		  		  				if (etitle.indexOf(TARGET.MARS.getName()) >= 0) s = TARGET.MARS.getName();
		  		  				if (etitle.indexOf(TARGET.JUPITER.getName()) >= 0) s = TARGET.JUPITER.getName();
		  		  				if (etitle.indexOf(TARGET.SATURN.getName()) >= 0) s = TARGET.SATURN.getName();
		  		  				if (etitle.indexOf(TARGET.URANUS.getName()) >= 0) s = TARGET.URANUS.getName();
		  		  				if (etitle.indexOf(TARGET.NEPTUNE.getName()) >= 0) s = TARGET.NEPTUNE.getName();
		  		  				if (etitle.indexOf(TARGET.Pluto.getName()) >= 0) s = TARGET.Pluto.getName();
		  		  				if (etitle.indexOf(TARGET.Moon.getName()) >= 0) s = TARGET.Moon.getName();
		  		  				if (etitle.indexOf(TARGET.Moon.getName().toLowerCase()) >= 0) s = TARGET.Moon.getName();
		  		  				TimeElement time = new TimeElement(astroDate, TimeElement.SCALE.LOCAL_TIME);
		  		  				timer.stop();
			         			Logger.log(LEVEL.INFO, "Selected new date: "+time.toString());
			        		    double ttminusut1 = TimeScale.getTTminusUT1(time, obs);
			         			Logger.log(LEVEL.INFO, "TT-UT1 = "+Functions.formatValue(ttminusut1, 3)+" s");
								skyRender.getRenderSkyObject().setSkyRenderElement(skyRender.getRenderSkyObject().render, time, obs, eph);
								object = s;
								if (object != null) {
					  				paintIt();
									setCentralObject(object, null);
								}
								paintIt();
	  		  				} catch (Exception e1) {
	  							String msg = e1.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e1.getStackTrace());
	  							JOptionPane.showMessageDialog(panel.getParent(), msg, Translate.translate(230), JOptionPane.ERROR_MESSAGE);
	  	         				Logger.log(LEVEL.ERROR, "Error selecting event. Message was: "+msg);
	  		  				}
	  		  			}
	  		  		});
  				}
  			}
  			menu.addSeparator();
  			menu.add(event);
  		}

  		JMenuItem objlist = new JMenuItem(Translate.translate(1227));
  		objlist.addActionListener(new ActionListener() {
  			public void actionPerformed(ActionEvent e) {
  				if (listShown && table != null && table.getComponent().isVisible()) return;
  				updateObjTable();
  			}
  		});
  		if (showListObj) {
  	  		if (feed == null) menu.addSeparator();
  			menu.add(objlist);
  		}

		int n = menu.getComponentCount();
		for (int i=0; i<n; i++) {
			Component c = menu.getComponent(i);
			c.setFont(menu.getFont());
			if (c instanceof JMenu) {
				JMenu j = (JMenu) c;
				j.setFont(menu.getFont());
	  			int n2 = j.getItemCount();
	  			for (int i2=0; i2<n2; i2++) {
	  				Component c2 = j.getMenuComponent(i2);
					c2.setFont(menu.getFont());
	  				if (c2 instanceof JMenu) {
	  					JMenu j3 = (JMenu) c2;
						j3.setFont(menu.getFont());
	  		  			int n3 = j3.getItemCount();
	  		  			for (int i3=0; i3<n3; i3++) {
	  		  				Component c3 = j3.getMenuComponent(i3);
	  						c3.setFont(menu.getFont());
	  		  			}
	  				}
	  			}
			}
		}

		Color back = (Color) UIManager.get("PopupMenu.background");
		Color foreg = (Color) UIManager.get("PopupMenu.foreground");
  		if (skyRender.getRenderSkyObject().render.getColorMode() == COLOR_MODE.NIGHT_MODE) {
  			back = new Color(0, 0, 0);
  			foreg = new Color(128, 0, 0);
  		}
		menu.setBackground(back);
		menu.setForeground(foreg); 
		for (int i=0; i<n; i++) {
			Component c = menu.getComponent(i);
			c.setBackground(back);
			c.setForeground(foreg);
			if (c instanceof JMenu) {
				JMenu j = (JMenu) c;
				j.setOpaque(true);
	  			int n2 = j.getItemCount();
	  			for (int i2=0; i2<n2; i2++) {
	  				Component c2 = j.getMenuComponent(i2);
	  				c2.setBackground(back);
	  				c2.setForeground(foreg);
	  				if (c2 instanceof JMenu) {
	  					JMenu j3 = (JMenu) c2;
	  					j3.setOpaque(true);
	  		  			int n3 = j3.getItemCount();
	  		  			for (int i3=0; i3<n3; i3++) {
	  		  				Component c3 = j3.getMenuComponent(i3);
	  		  				c3.setBackground(back);
	  		  				c3.setForeground(foreg);
	  		  			}
	  				}
	  			}
			}
		}
  		
		if (skyRender.getRenderSkyObject().render.anaglyphMode == ANAGLYPH_COLOR_MODE.TRUE_3D_MODE_LEFT_RIGHT_HALF_WIDTH)
		{
	  		menu.show(panel, x/2, y);			
		} else {
	  		menu.show(panel, x, y);			
		}
    }

    private void updateObjTable() {
    	// TODO: Sky view should have low field of view so that the az/el values are updated frequently

		try {
			// objects.add(new Object[] {name, messier, tt, loc, (float)magnitude,
			//    new float[] {(float) maxSize, (float) minSize}, paf, com});
			Object o =  DataBase.getDataForAnyThread("objects", true);
			if (o == null) return;
			Object o2000 =  DataBase.getDataForAnyThread("objectsJ2000", true);
			if (o2000 == null) return;
			ArrayList<Object> objects = new ArrayList<Object>(Arrays.asList((Object[]) o));
			ArrayList<Object> objectsJ2000 = new ArrayList<Object>(Arrays.asList((Object[]) o2000));
			String stable[][] = new String[objects.size()][7];
			String types[] = new String[] {DataSet.capitalize(Translate.translate(819).toLowerCase(), false), DataSet.capitalize(Translate.translate(40), false),
					DataSet.capitalize(Translate.translate(959), false), DataSet.capitalize(Translate.translate(960), false), DataSet.capitalize(Translate.translate(1297), false),
					DataSet.capitalize(Translate.translate(961), false), DataSet.capitalize(Translate.translate(953), false), DataSet.capitalize(Translate.translate(954), false),
					DataSet.capitalize(Translate.translate(955), false), DataSet.capitalize(Translate.translate(956), false), DataSet.capitalize(Translate.translate(957), false),
					DataSet.capitalize(Translate.translate(958), false)};
			for (int i=0; i<objects.size(); i++)
			{
				Object[] obj = (Object[]) objects.get(i);
				if (obj.length <= 3) {
					int index2000 = (Integer) obj[0];
					LocationElement locF = (LocationElement) obj[1];
					obj = (Object[]) objectsJ2000.get(index2000);
					obj[3] = locF;
					if (obj.length == 3) {
						float mag = (Float) obj[2];
						obj[4] = mag;
					}
				}

				LocationElement loc = (LocationElement) obj[3];
				float s[] = (float[]) obj[5];
				String t = "";
				int type = Math.abs((Byte) obj[2]);
				if (type < 11) t = types[type];
				stable[i] = new String[] {
						(String) obj[0],
						(String) obj[1],
						t,
						Functions.formatAngleAsDegrees(loc.getLongitude(), 3),
						Functions.formatAngleAsDegrees(loc.getLatitude(), 3),
						Functions.formatValue((Float) obj[4], 1),
						Functions.formatValue(s[0], 3)+"x"+Functions.formatValue(s[1], 3)
				};
			}

    		String lo = Translate.translate(515), la = Translate.translate(517);
    		if (skyRender.getRenderSkyObject().render.coordinateSystem == COORDINATE_SYSTEM.EQUATORIAL) {
    			lo = Translate.translate(21);
    			la = Translate.translate(22);
    		}
    		if (skyRender.getRenderSkyObject().render.coordinateSystem == COORDINATE_SYSTEM.HORIZONTAL) {
    			lo = Translate.translate(28);
    			la = Translate.translate(29);
    		}
			String columns[] = new String[] {Translate.translate(787), Translate.translate(1295), Translate.translate(486), lo+" (\u00b0)", la+" (\u00b0)", Translate.translate(157), Translate.translate(308)+" (\u00b0)"};

	    	if (!listShown || table == null || !table.getComponent().isVisible()) {
				boolean editable[] = null; // All false except boolean
				Class<?> classes[] = new Class<?>[] {String.class, String.class, String.class, null, null, null, String.class};
				table = new JTableRendering(columns, classes, editable, stable);

				JFrame frame = new JFrame(Translate.translate(38));
				if (panel != null) {
					Container top = panel.getTopLevelAncestor();
					if (top instanceof JFrame) {
						JFrame topf = (JFrame) top;
						if (topf.getIconImage() != null) frame.setIconImage(topf.getIconImage());
					}
				}
				frame.setPreferredSize(new Dimension(400, 300));
				frame.add(new JScrollPane(table.getComponent()));
				frame.pack();
				frame.setVisible(true);
				frame.addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosing(WindowEvent e) {
						super.windowClosing(e);
						listShown = false;
					}
				});

				table.getComponent().addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						super.mouseClicked(e);

						String row = table.getSelectedRow(",");
						String obj = FileIO.getField(1, row, ",", true);
						setCentralObject(obj);
					}
				});
	    	} else {
	    		table.setColumnNames(columns);
	    		table.updateTable(stable, true);
	    	}
	  		if (skyRender.getRenderSkyObject().render.getColorMode() == COLOR_MODE.NIGHT_MODE) {
	  			Color back = new Color(0, 0, 0), foreg = new Color(128, 0, 0);
	  			table.getComponent().setBackground(back);
	  			table.getComponent().setForeground(foreg);
	  			table.getComponent().setShowGrid(false);
	  		}
			listShown = true;
		} catch (Exception exc) {
			exc.printStackTrace();
		}
    }

	private static String replaceVars(String raw, String[] vars, String[] values) {
		if (vars == null) return raw;
		String out = raw;
		for (int i=0; i<vars.length; i++) {
			out = DataSet.replaceAll(out, vars[i], values[i], true);
		}
		return out;
	}

	private void showPopup(String obj, String popupText) throws JPARSECException {
		Picture pic = null;
		try {
			String obj2 = obj.toLowerCase();
			if (obj2.startsWith("ngc") || obj2.startsWith("mel") || obj2.startsWith("ic ")) {
				int n = obj2.lastIndexOf("-");
				if (n > 0) obj2 = obj2.substring(0, n).trim();
				n = obj2.lastIndexOf("m");
				if (n > 0) obj2 = obj2.substring(n);
			}
			if (obj2.indexOf("caldwell")>=0) {
				obj2 = RenderSky.searchDeepSkyObjectReturnMainName(obj2);
				if (obj2 != null) {
					obj2 = obj2.toLowerCase();
				} else {
					obj2 = "";
				}
			}
			if (!obj2.startsWith("ic") && !obj2.startsWith("ngc")) obj2 = DataSet.replaceAll(obj2, " ", "", true);
			String image = FileIO.DATA_TEXTURES_DIRECTORY + "deepsky/"+obj2;
			BufferedImage img = null;
			try {
				img = ReadFile.readImageResource(image+".png");
			} catch (Exception exc) {
				img = ReadFile.readImageResource(image+".jpg");
			}
			if (img != null) {
				pic = new Picture(img);
			}
		} catch (Exception exc) {  }

		int w = 600, h = 600;
		int th = 30, maxImgH = 520;
		if (pic == null) {
			try {
				TARGET target = TARGET.NOT_A_PLANET;
				if (obj != null && !obj.equals("")) target = Target.getID(obj);
				if (target != TARGET.NOT_A_PLANET && (target.isPlanet() || target == TARGET.Pluto || target == TARGET.Moon) && !target.isNaturalSatellite()) {
					/*
					double field = 100 * Constant.ARCSEC_TO_RAD;
					if (target == TARGET.MARS || target == TARGET.MERCURY) field = 30 * Constant.ARCSEC_TO_RAD;
					if (target == TARGET.VENUS) field = 50 * Constant.ARCSEC_TO_RAD;
					if (target.isNaturalSatellite() && target.compareTo(TARGET.URANUS) < 0) {
						target = target.getCentralBody();
						field *= 10;
						if (target == TARGET.MARS) field /= 3;
					}
					if (target.compareTo(TARGET.URANUS) >= 0) field = 10 * Constant.ARCSEC_TO_RAD;
					if (target == TARGET.Moon || target == TARGET.SUN) field = Constant.DEG_TO_RAD;
					*/
					eph.targetBody = target;
					EphemElement ephem = Ephem.getEphemeris(time, obs, eph, false);
					double field = ephem.angularRadius * 4;
					if (target == TARGET.SATURN || target == TARGET.URANUS || target == TARGET.NEPTUNE) field = ephem.angularRadius * 9;

					PlanetRenderElement render = new PlanetRenderElement(w, maxImgH, false, true, true, false, true, false);
					if (skyRender.getRenderSkyObject().render.planetRender.highQuality) render.highQuality = true;
					TelescopeElement telescope = chart.telescope.clone(); //TelescopeElement.SCHMIDT_CASSEGRAIN_20cm;
 					telescope.ocular.focalLength = TelescopeElement.getOcularFocalLengthForCertainField(field, telescope);
					telescope.invertHorizontal = telescope.invertVertical = false;
					render.telescope = telescope;
					render.showLabels = true;
					EphemerisElement eph = this.eph.clone();
					eph.targetBody = target;

					if (target == TARGET.SUN) {
						String co = object;
						double clon = skyRender.getRenderSkyObject().render.centralLongitude;
						double clat = skyRender.getRenderSkyObject().render.centralLatitude;

						SkyRendering skyRender2 = new SkyRendering(time, obs, eph, skyRender.getRenderSkyObject().render.clone(), "", 0);
						skyRender2.getRenderSkyObject().render.drawSkyBelowHorizon = true;
						skyRender2.getRenderSkyObject().forceUpdatePlanets();
						skyRender2.getRenderSkyObject().render.telescope = telescope;
						skyRender2.getRenderSkyObject().render.width = 600;
						skyRender2.getRenderSkyObject().render.height = 600;
						skyRender2.getRenderSkyObject().render.drawPlanetsMoonSun = true;
						skyRender2.getRenderSkyObject().render.drawCoordinateGrid = false;
						skyRender2.getRenderSkyObject().render.drawDeepSkyObjects = skyRender2.getRenderSkyObject().render.drawStars = false;
						skyRender2.getRenderSkyObject().render.drawArtificialSatellites = skyRender2.getRenderSkyObject().render.drawAsteroids = skyRender2.getRenderSkyObject().render.drawComets = false;
						skyRender2.getRenderSkyObject().render.drawNebulaeContours = skyRender2.getRenderSkyObject().render.drawMilkyWayContours = false;
						skyRender2.getRenderSkyObject().render.drawConstellationContours = CONSTELLATION_CONTOUR.NONE;
						skyRender2.getRenderSkyObject().render.drawConstellationLimits = false;
						skyRender2.getRenderSkyObject().render.drawMeteorShowers = skyRender2.getRenderSkyObject().render.drawFaintStars = false;
						skyRender2.getRenderSkyObject().render.drawOcularFieldOfView = skyRender2.getRenderSkyObject().render.drawSpaceProbes = skyRender2.getRenderSkyObject().render.drawSuperNovaAndNovaEvents = false;
						skyRender2.getRenderSkyObject().render.drawCentralCrux = false;
						skyRender2.getRenderSkyObject().render.drawExternalGrid = false;
						skyRender2.getRenderSkyObject().render.setColorMode(COLOR_MODE.BLACK_BACKGROUND);
						skyRender2.getRenderSkyObject().render.background = jparsec.graph.chartRendering.Graphics.COLOR_GRAY_Black;
						skyRender2.getRenderSkyObject().render.drawLeyend = LEYEND_POSITION.NO_LEYEND;
						skyRender2.getRenderSkyObject().setSkyRenderElement(skyRender2.getRenderSkyObject().render);
						skyRender2.getRenderSkyObject().colorSquemeChanged();
						setCentralObject(target.getName());
						skyRender2.getRenderSkyObject().render.centralLongitude = skyRender.getRenderSkyObject().render.centralLongitude;
						skyRender2.getRenderSkyObject().render.centralLatitude = skyRender.getRenderSkyObject().render.centralLatitude;

						pic = new Picture(skyRender2.createBufferedImage());

						object = co;
						skyRender.getRenderSkyObject().render.centralLongitude = clon;
						skyRender.getRenderSkyObject().render.centralLatitude = clat;
						paintIt();
					} else {
						PlanetaryRendering renderPlanet = new PlanetaryRendering(time, obs, eph, render, "Planet rendering");
						pic = new Picture(renderPlanet.createBufferedImage());
					}
				}
			} catch (Exception exc) { exc.printStackTrace(); }
		}

		if (pic == null) {
			String data[] = DataSet.toStringArray(popupText, FileIO.getLineSeparator());
			int typeIndex = DataSet.getIndexStartingWith(data, Translate.translate(675)) + 1;
			String type = data[typeIndex];
 			type = type.substring(type.lastIndexOf(":")+1);
			if (type.toLowerCase().indexOf(Translate.translate(32).toLowerCase()) >= 0) {
				String pos[] = DataSet.getSubArray(data, 1, 11);
				String ra = pos[0].substring(pos[0].lastIndexOf(":")+1).trim();
				String dec = pos[1].substring(pos[1].lastIndexOf(":")+1).trim();
				double mag = Double.parseDouble(pos[10].substring(pos[10].lastIndexOf(":")+1).trim());
				LocationElement loc = new LocationElement(Functions.parseRightAscension(ra), Functions.parseDeclination(dec), 1.0);
				if (skyRender.getRenderSkyObject().render.drawSkyCorrectingLocalHorizon && (obs.getMotherBody() == null || obs.getMotherBody() == TARGET.EARTH))
					loc = Ephem.removeRefractionCorrectionFromEquatorialCoordinates(time, obs, eph, loc);
				LocationElement j2000 = Ephem.toMeanEquatorialJ2000(
						loc,
						time, obs, eph);
				ReadFile re = new ReadFile();
				re.setPath(DoubleStarElement.PATH_VISUAL_DOUBLE_STAR_CATALOG); //.PATH_OLD_VISUAL_DOUBLE_STAR_CATALOG);
				double radius = 60 * Constant.ARCSEC_TO_RAD;
				re.setReadConstraints(null, j2000, radius);
				boolean warn = JPARSECException.DISABLE_WARNINGS;
				JPARSECException.DISABLE_WARNINGS = true;
				re.readFileOfDoubleStars();
				JPARSECException.DISABLE_WARNINGS = warn;
				int index = 0;
				if (re.getNumberOfObjects() > 0) {
					if (re.getNumberOfObjects() > 1) index = re.searchByPosition(j2000, radius);
					DoubleStarElement dstar = re.getDoubleStarElement(index);
					double dif = Math.abs(Double.parseDouble(dstar.magPrimary)-mag);
					if (dif < 1) {
						double jd = TimeScale.getJD(time, obs, eph, TimeElement.SCALE.TERRESTRIAL_TIME);
						dstar.calcEphemeris(time, obs);
						int iw = 400, ih = 400;
						pic = new Picture(dstar.orbit.getOrbitImage("", iw, ih, 0.9, jd, false, true));
						Graphics2D g = pic.getImage().createGraphics();
						AWTGraphics.enableAntialiasing(g);
						g.setColor(Color.BLACK);
						String label1 = "@rho = "+Functions.formatValue(dstar.getDistance(), 3)+"\"";
						String label2 = "PA = "+Functions.formatAngleAsDegrees(dstar.getPositionAngle(), 3)+"\u00b0";
						TextLabel tl1 = new TextLabel(label1);
						tl1.draw(g, 10, ih-40);
						TextLabel tl2 = new TextLabel(label2);
						tl2.draw(g, 10, ih-20);
					pic.invertColors();
					}
				}
			}

			if (data[0].toLowerCase().indexOf(Translate.translate(79).toLowerCase()) >= 0 && (type.indexOf("V")>=0 || type.indexOf("B")>=0)) {
				// TODO: integration of AAVSO light curves
				String pos[] = DataSet.getSubArray(data, 1, 10);
				String ra = pos[0].substring(pos[0].lastIndexOf(":")+1).trim();
				String dec = pos[1].substring(pos[1].lastIndexOf(":")+1).trim();
				LocationElement loc = new LocationElement(Functions.parseRightAscension(ra), Functions.parseDeclination(dec), 1.0);
				if (skyRender.getRenderSkyObject().render.drawSkyCorrectingLocalHorizon && (obs.getMotherBody() == null || obs.getMotherBody() == TARGET.EARTH))
					loc = Ephem.removeRefractionCorrectionFromEquatorialCoordinates(time, obs, eph, loc);
				LocationElement j2000 = Ephem.toMeanEquatorialJ2000(
						loc,
						time, obs, eph);
				ReadFile re = new ReadFile();
				re.setPath(VariableStarElement.getPathBulletinAAVSO(time.astroDate.getYear()-1));
				double radius = 60 * Constant.ARCSEC_TO_RAD;
				re.setReadConstraints(null, j2000, radius);
				try {
					re.readFileOfVariableStars();
					int index = 0;
					if (re.getNumberOfObjects() > 0) {
						if (re.getNumberOfObjects() > 1) index = re.searchByPosition(j2000, radius);
						VariableStarElement vstar = re.getVariableStarElement(index);
						double minima = vstar.getNextMinima(time, obs);
						if (vstar.isEclipsing) {
							vstar.calcEphemeris(time, obs, false);
							System.out.println(vstar.name+" PHASE    "+vstar.getPhase());
							if (minima > 0) System.out.println(vstar.name+" MIN " + TimeFormat.formatJulianDayAsDate(minima));
						} else {
							double maxima = vstar.getNextMaxima(time, obs);
							if (maxima > 0) System.out.println(vstar.name+" MAX " + TimeFormat.formatJulianDayAsDate(maxima));
							if (minima > 0) System.out.println(vstar.name+" MIN " + TimeFormat.formatJulianDayAsDate(minima));
						}
					}
				} catch (Exception exc) {}
			}

			if ((data[0].toLowerCase().indexOf(Translate.translate(74).toLowerCase()) >= 0 || data[0].toLowerCase().indexOf(Translate.translate(73).toLowerCase()) >= 0)
					&& obj != null && !obj.equals("")) {
				try {
					OrbitalElement orbit = null;
					int ast = OrbitEphem.getIndexOfAsteroid(obj);
					if (ast >= 0) orbit = OrbitEphem.getOrbitalElementsOfAsteroid(ast);
					if (orbit == null) {
						int com = OrbitEphem.getIndexOfComet(obj);
						if (com >= 0) orbit = OrbitEphem.getOrbitalElementsOfComet(com);
					}
					if (orbit == null) {
						int tr = OrbitEphem.getIndexOfTransNeptunian(obj);
						if (tr >= 0) orbit = OrbitEphem.getOrbitalElementsOfTransNeptunian(tr);
					}
					if (orbit == null) {
						int neo = OrbitEphem.getIndexOfNEO(obj);
						if (neo >= 0) orbit = OrbitEphem.getOrbitalElementsOfNEO(neo);
					}
					if (orbit != null) {
						TimeElement init = time.clone(), end = time.clone();
						double jd_tdb = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
						init.add(-1*365.25);
						end.add(5*365.25);
						int ow = 300, oh = 300;
						CreateChart ch = orbit.getLightCurveChart(init, end, new ObserverElement(),
								new EphemerisElement(), 200);
						ChartElement chart = ch.getChartElement();
						chart.imageWidth = ow;
						chart.imageHeight = oh;
						ch = new CreateChart(chart);
						jparsec.io.image.Picture p1 = new Picture(ch.chartAsBufferedImage());
						CreateChart ch2 = orbit.getDistanceChart(init, end, new ObserverElement(),
								new EphemerisElement(), 200);
						ChartElement chart2 = ch2.getChartElement();
						chart2.imageWidth = ow;
						chart2.imageHeight = oh;
						ch2 = new CreateChart(chart2);
						jparsec.io.image.Picture p2 = new Picture(ch2.chartAsBufferedImage());
						Picture p3 = new Picture(orbit.getOrbitImage(orbit.name, p1.getWidth()*2, (int)(p1.getWidth()*1.33), 0.7, jd_tdb, true, true));
						Picture p = new Picture(p1.getWidth()*2, p1.getHeight()+p3.getHeight());
						Graphics2D g2 = p.getImage().createGraphics();
						g2.drawImage(p1.getImage(), 0, 0, null);
						g2.drawImage(p2.getImage(), p1.getWidth(), 0, null);
						g2.drawImage(p3.getImage(), 0, p1.getHeight(), null);
						pic = p;
					}
				 } catch (Exception exc) {
//					 exc.printStackTrace();
					 System.out.println(JPARSECException.getTrace(exc.getStackTrace()));
				 }
			}
		}

		Draw draw = new Draw(w, 2000);
		draw.clear(Color.BLACK);
		draw.setPenColor(Color.WHITE);
		draw.setFont(new Font(Font.DIALOG, Font.PLAIN, th));
		draw.setPenRadius(0.005);
		double x = 0.025, y = 0.975, y0 = y;
		if (pic != null) {
			if (pic.getWidth() > draw.getWidth() || pic.getHeight() > maxImgH)
				pic.getScaledInstanceUsingSplines(draw.getWidth(), maxImgH, true);
			y -= pic.getHeight() * 0.5 / draw.getHeight();
			draw.picture(0.5, y, pic.getImage());
			y -= pic.getHeight() * 0.5 / draw.getHeight();
		}
		draw.text(x, y0, obj, false);
		draw.setFont(new Font(Font.DIALOG, Font.PLAIN, th/2));
		y -= th / (double) draw.getHeight();
		draw.line(0, y, 1.0, y);
		y -= th / (double) draw.getHeight();
		String t[] = DataSet.toStringArray(popupText, FileIO.getLineSeparator());
		int init = t[1].indexOf(Translate.translate(21));
		String prefix = "";
		if (init > 0) prefix = t[1].substring(0, init);

		int maxLine1 = 70;
		if (t[0].length() > maxLine1) {
			do {
				String s = t[0];
				if (t[0].length() > maxLine1) s = t[0].substring(0, maxLine1);
				int n = s.lastIndexOf(" ");
				if (t[0].length() > maxLine1 && n > 10) s = s.substring(0, n);
				draw.text(x, y, s, false);
				t[0] = t[0].substring(s.length()).trim();
				y -= th / (double) draw.getHeight();
			} while (t[0].length() > 0);
		} else {
			draw.text(x, y, t[0], false);
		}
		y -= th / (double) draw.getHeight();
		y -= th / (double) draw.getHeight();

		for (int i=1; i<t.length; i++) {
			String columns[] = new String[] {
					FileIO.getField(1, t[i], ":", false),
					FileIO.getRestAfterField(1, t[i], ":", false)
			};
			if (!prefix.equals("") && columns[0].startsWith(prefix)) columns[0] = columns[0].substring(init);
			y0 = y;
			if (columns[0].length() > 30) {
				do {
					String s = columns[0];
					if (columns[0].length() > 30) s = columns[0].substring(0, 30);
					int n = s.lastIndexOf(" ");
					if (columns[0].length() > 30 && n > 10) s = s.substring(0, n);
					draw.text(x, y, s, false);
					columns[0] = columns[0].substring(s.length()).trim();
					y -= th / (double) draw.getHeight();
				} while (columns[0].length() > 0);
			} else {
				draw.text(x, y, columns[0], false);
			}
			double yf = y;
			y = y0;
			if (columns[1].length() > 30) {
				do {
					String s = columns[1];
					if (columns[1].length() > 30) s = columns[1].substring(0, 30);
					int n = s.lastIndexOf(" ");
					if (columns[1].length() > 30 && n > 10) s = s.substring(0, n);
					draw.text(x + 0.5, y, s, false);
					columns[1] = columns[1].substring(s.length()).trim();
					y -= th / (double) draw.getHeight();
				} while (columns[1].length() > 0);
			} else {
				draw.text(x + 0.5, y, columns[1], false);
				y -= th / (double) draw.getHeight();
			}
			if (yf < y) y = yf;
		}
		int endy = (int) ((1.0 - y) * draw.getHeight()) + th;
		final JFrame f = new JFrame(obj+" ("+obs.getName()+", "+time.toString()+")");
		if (pic != null) {
			f.setIconImage(pic.getImage());
		} else {
			if (panel != null) {
				Container top = panel.getTopLevelAncestor();
				if (top instanceof JFrame) {
					JFrame topf = (JFrame) top;
					if (topf.getIconImage() != null) f.setIconImage(topf.getIconImage());
				}
			}
		}
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		if (endy < h) h = endy;
		if (endy > draw.getHeight()) endy = draw.getHeight();
		f.setPreferredSize(new Dimension(w, h));
		final JLabel label = new JLabel(new ImageIcon(draw.getOffScreenImage().getSubimage(0, 0, draw.getWidth(), endy)));
		JScrollPane js = new JScrollPane(label);
		js.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		js.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		js.getVerticalScrollBar().setUnitIncrement(16);
		js.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				f.dispose();
			}
		});
		f.add(js);
		f.setResizable(false);
		f.pack();
		f.setVisible(true);

	}

/*	private void showPopupHTML(String obj, String popupText) throws JPARSECException {
			HTMLReport html = new HTMLReport();
			html.writeHeader(obj);
			html.beginBody("#"+HTMLReport.COLOR_BLACK);
			html.setTextColor(HTMLReport.COLOR_WHITE);
			html.writeMainTitle(obj);
			BufferedImage img = null;
			try {
				String obj2 = obj.toLowerCase();
				if (obj2.startsWith("ngc") || obj2.startsWith("mel")) {
					int n = obj2.lastIndexOf("m");
					if (n > 0) obj2 = obj2.substring(n);
				}
				String image = FileIO.DATA_TEXTURES_DIRECTORY + "deepsky/"+DataSet.replaceAll(obj2, " ", "", true)+".png";
				img = ReadFile.readImageResource(image);
				if (img != null) {
					String writeTo = FileIO.getTemporalDirectory() + obj2 + ".png";
					Picture pic = new Picture(img);
					pic.write(writeTo);
					html.writeImageToHTML(""+pic.getWidth(), ""+pic.getHeight(), "center", "0", ""+obj2, "file://"+writeTo);
				}
			} catch (Exception exc) { }
			html.writeHorizontalLine();
			html.writeSmallSkip();
			String t[] = DataSet.toStringArray(popupText, FileIO.getLineSeparator());
			html.writeRawText(t[0]);
			int border = 0, cellspacing = 0, cellpadding = 0;
			String width = "100%";
			html.writeTableHeader(border, cellspacing, cellpadding, width);
			String bgcolor = null, align = null, colspan = null;
			for (int i=1; i<t.length; i++) {
				String columns[] = new String[] {
						FileIO.getField(1, t[i], ":", false),
						FileIO.getRestAfterField(1, t[i], ":", false)
				};
				html.writeRowInTable(columns, bgcolor, align, colspan);
			}
			html.endBody();
			html.endDocument();
			HTMLRendering dlg = new HTMLRendering(obj, html.getCode(), img, true);
			dlg.setSize(600, 600);
			dlg.setModal(false);
			dlg.setVisible(true);
	}
*/
	private int getMinorObjectIndex(String name, EphemerisElement eph) throws JPARSECException {
		int index = OrbitEphem.getIndexOfAsteroid(name);
		if (index >= 0) {
			eph.orbit = OrbitEphem.getOrbitalElementsOfAsteroid(index);
		} else {
			index = OrbitEphem.getIndexOfComet(name);
			if (index >= 0) {
				eph.orbit = OrbitEphem.getOrbitalElementsOfComet(index);
			} else {
    			index = OrbitEphem.getIndexOfTransNeptunian(name);
    			if (index >= 0) {
    				eph.orbit = OrbitEphem.getOrbitalElementsOfTransNeptunian(index);
    			} else {
    				return -1;
    			}
			}
		}
		eph.targetBody.setIndex(index);
		eph.algorithm = ALGORITHM.ORBIT;
		return index;
	}
	private void setCentralObject(SkyRendering skyRender, TARGET targetID, boolean showError) throws JPARSECException {
		boolean fast = false;

	    // Now we set the center position to draw: constellation or a planet
        if (targetID == TARGET.NOT_A_PLANET) {
        	if (object.startsWith("skyloc_")) {
        		String p = object.substring(7);
        		int type = 0;
        		boolean precess = false;
        		if (p.startsWith("ec")) type = 1;
        		if (p.startsWith("ga")) type = 2;
        		if (p.startsWith("ho")) type = 3;
        		if (p.startsWith("eq0") || p.startsWith("ho0") || p.startsWith("ec0") || p.startsWith("ga0")) precess = true;
        		int s = p.indexOf("_");
        		p = p.substring(s+1);
        		s = p.indexOf("_");
        		LocationElement loc = new LocationElement(
        				DataSet.parseDouble(p.substring(0, s)) * Constant.DEG_TO_RAD,
        				DataSet.parseDouble(p.substring(s + 1)) * Constant.DEG_TO_RAD,
        				1.0
        		);
        		if (type == 1) loc = CoordinateSystem.eclipticToEquatorial(loc, time, obs, eph);
        		if (type == 2) loc = CoordinateSystem.galacticToEquatorial(loc, time, obs, eph);
        		if (type == 3) loc = CoordinateSystem.horizontalToEquatorial(loc, time, obs, eph);

        		if (chart.drawSkyCorrectingLocalHorizon) {
        			if (obs.getMotherBody() == null || obs.getMotherBody() == TARGET.EARTH) {
	        			double ast = SiderealTime.apparentSiderealTime(time, obs, eph);
		    			loc = CoordinateSystem.equatorialToHorizontal(loc, ast, obs, eph, false, fast);
		    			loc.setLatitude(Ephem.getGeometricElevation(eph, obs, loc.getLatitude()));
		    			loc = CoordinateSystem.horizontalToEquatorial(loc, ast, obs.getLatitudeRad(), fast);
        			}
        		}

        		if (precess) {
	    		    double jd = TimeScale.getJD(time, obs, eph, TimeElement.SCALE.TERRESTRIAL_TIME);
	        		loc = LocationElement.parseRectangularCoordinates(Precession.precessFromJ2000(jd, loc.getRectangularCoordinates(), eph));
        		}
            	loc = RenderSky.getPositionInSelectedCoordinateSystem(loc, time, obs, eph, chart, fast);
                chart.centralLongitude = loc.getLongitude();
                chart.centralLatitude = loc.getLatitude();
        	} else {
	            LocationElement loc = Constellation.getConstellationPosition(object, time.astroDate.jd(), chart.drawConstellationNamesType);
	            if (loc != null) {
	    			if (obs.getMotherBody() != TARGET.EARTH && obs.getMotherBody() != TARGET.NOT_A_PLANET) {
	    				loc = Ephem.getPositionFromBody(loc, time, obs, eph);
	    			}
	            	loc = RenderSky.getPositionInSelectedCoordinateSystem(loc, time, obs, eph, chart, fast);
	                chart.centralLongitude = loc.getLongitude();
	                chart.centralLatitude = loc.getLatitude();
	            } else {
	            	try {
	            		EphemerisElement eph = this.eph.clone();
	            		int index = getMinorObjectIndex(object, eph);
	            		if (index >= 0) {
	        			    EphemElement ephem = Ephem.getEphemeris(time, obs, eph, false, true);
	                        loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
	                        loc = RenderSky.getPositionInSelectedCoordinateSystem(loc, time, obs, eph, chart, fast);
	                        chart.centralLongitude = loc.getLongitude();
	                        chart.centralLatitude = loc.getLatitude();
	            		} else {
		            		SimbadElement simbad = SimbadElement.searchDeepSkyObject(object);
		            		if (simbad == null) throw new Exception("Cannot find object '"+object+"'");
		            		loc = new LocationElement(simbad.rightAscension, simbad.declination, 1.0);
			    			loc = Ephem.fromJ2000ToApparentGeocentricEquatorial(loc, time, obs, eph);
			    			if (obs.getMotherBody() != TARGET.EARTH && obs.getMotherBody() != TARGET.NOT_A_PLANET) {
			    				loc = Ephem.getPositionFromBody(loc, time, obs, eph);
			    			}
		                	loc = RenderSky.getPositionInSelectedCoordinateSystem(loc, time, obs, eph, chart, fast);
		                    chart.centralLongitude = loc.getLongitude();
		                    chart.centralLatitude = loc.getLatitude();
	            		}
	            	} catch (Exception exc) {
	  					Logger.log(LEVEL.ERROR, "Error setting central object. Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
	    				//appendMsg(language[62]);
						if (showError) JOptionPane.showMessageDialog(panel.getParent(),
		  						replaceVars(Translate.translate(876), new String[] {"%obj"}, new String[] {object}), Translate.translate(230), JOptionPane.ERROR_MESSAGE);
						object = "";
						throw new JPARSECException(exc);
	            	}
	            }
        	}
        } else {
        	try {
        		EphemerisElement eph = this.eph.clone();
        		int index = getMinorObjectIndex(targetID.getEnglishName(), eph);
    			if (index < 0 && eph.targetBody.isNaturalSatellite())
    					eph.algorithm = ALGORITHM.NATURAL_SATELLITE;
			    EphemElement ephem = Ephem.getEphemeris(time, obs, eph, false, true);

                LocationElement loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
                loc = RenderSky.getPositionInSelectedCoordinateSystem(loc, time, obs, eph, chart, fast);
                chart.centralLongitude = loc.getLongitude();
                chart.centralLatitude = loc.getLatitude();
        	} catch (Exception exc) {
        		Logger.log(LEVEL.ERROR, "Error setting central object. Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
				if (showError) JOptionPane.showMessageDialog(panel.getParent(), replaceVars(Translate.translate(876), new String[] {"%obj"}, new String[] {object}), Translate.translate(230), JOptionPane.ERROR_MESSAGE);
				object = "";
				throw new JPARSECException(exc);
        	}
        }
		skyRender.getRenderSkyObject().render.centralLongitude = chart.centralLongitude;
		skyRender.getRenderSkyObject().render.centralLatitude = chart.centralLatitude;
	}

	/**
	 * Sets a central object.
	 * @param s The name of the object.
	 */
	public void setCentralObject(String s) {
		this.setCentralObject(s, null);
	}

	/**
	 * Returns the name of the object currently being tracked.
	 * @return Object name, or null if no one is being tracked.
	 */
	public String getCentralObject() {
		return object;
	}

    private void setCentralObject(String s, Object data[]) {
		if (s == null) {
			object = null;
			return;
		}

    	if (s.toLowerCase().startsWith("m") && DataSet.isDoubleStrictCheck(s.substring(1).trim())) s = "M"+s.substring(1).trim();
    	if (s.toLowerCase().startsWith("ngc")) {
    		String number = s.substring(3);
    		if (DataSet.isDoubleStrictCheck(number) || (number.length() > 4 && DataSet.isDoubleStrictCheck(number.substring(0, 4)))) {
    			if (s.length() < 8) s = "NGC "+s.substring(3).trim();
    		}
    	}
    	if (s.toLowerCase().startsWith("ic")) s = "IC "+s.substring(2).trim();

    	boolean fast = false;
		try {
			try {
				skyRender.getRenderSkyObject().setYCenterOffset(0);
			} catch (Exception e1) {
 				Logger.log(LEVEL.ERROR, "Error setting center y value. Message was: "+e1.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e1.getStackTrace()));
			}
			LocationElement loc = null;
			if (data == null) {
				loc = skyRender.getRenderSkyObject().searchObject(s);
			} else {
        		RenderSky.OBJECT id = (RenderSky.OBJECT) data[0];
				loc = skyRender.getRenderSkyObject().searchObject(s, id);
			}

			if (loc != null) {
				loc = RenderSky.getPositionInSelectedCoordinateSystem(loc, time, obs, eph, skyRender.getRenderSkyObject().render, fast);

				skyRender.getRenderSkyObject().render.centralLongitude = loc.getLongitude();
				skyRender.getRenderSkyObject().render.centralLatitude = loc.getLatitude();
				chart.centralLongitude = loc.getLongitude();
				chart.centralLatitude = loc.getLatitude();
				object = s;
  				paintImage();
			} else {
				try {
					if (data != null) {
	            		RenderSky.OBJECT id = (RenderSky.OBJECT) data[0];
	          			if (id == RenderSky.OBJECT.SUPERNOVA || id == OBJECT.NOVA || id == OBJECT.METEOR_SHOWER) {
	          				String[] objData = (String[]) data[2];
	          				double ra = Functions.parseRightAscension(objData[1]) * Constant.RAD_TO_DEG;
	          				double dec = Functions.parseDeclination(objData[2]) * Constant.RAD_TO_DEG;
	          				s = "skyloc_eq_"+ra+"_"+dec;
	          			}
          			}
					TARGET target = Target.getID(s);
					eph.targetBody = target;
					String oldObj = object;
					object = s;
					try {
						setCentralObject(skyRender, target, true);
					} catch (Exception exc) {
						object = oldObj;
					}
	  				paintImage();
				} catch (JPARSECException e1) {
     				Logger.log(LEVEL.ERROR, "Error setting central object. Message was: "+e1.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e1.getStackTrace()));
				}
			}

		} catch (Exception exc) {
			Logger.log(LEVEL.ERROR, "Error when setting central object. Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
		}
    }

  	// Identifies an object at x,y and returns its data
  	private String identifyAndGetData(int x, int y)
  	throws JPARSECException {
  		String out = "";

  		// Identify closest object: give preference to Solar System bodies to identify
  		// them without limiting magnitude. If it is not a Solar System body then
  		// reidentify closest object applying the limiting magnitude
  		Object data[] = skyRender.getRenderSkyObject().getClosestObjectData(x, y, false, true);
  		RenderSky.OBJECT id = (RenderSky.OBJECT) data[0];
  		if (id == RenderSky.OBJECT.STAR)
  			data = skyRender.getRenderSkyObject().getClosestObjectData(x, y, true, true);
  		if (data == null) return "";
  		id = (RenderSky.OBJECT) data[0];
  		EphemElement ephem = null;
  		String type = null;
  		String objData[] = null;
  		StarElement star = null;

  		if (id == RenderSky.OBJECT.DEEPSKY) {
  			type = Translate.translate(972);
  			objData = ((String[]) data[2]).clone();
  			if (objData.length <= 4) type = Translate.translate(79);
  		} else {
  			if (id == RenderSky.OBJECT.SUPERNOVA || id == OBJECT.NOVA || id == OBJECT.METEOR_SHOWER) {
  				type = Translate.translate(877); // SN
  				if (id == OBJECT.METEOR_SHOWER) type = Translate.translate(1024);
				if (id == OBJECT.NOVA) type = Translate.translate(1304);
  				objData = ((String[]) data[2]).clone();
  			} else {
  				ephem = (EphemElement) data[2];
  				type = Translate.translate(878); // SS obj
  				if (id == RenderSky.OBJECT.STAR) {
  		  			type = Translate.translate(79); // Star
  					star = (StarElement) data[3];
  				} else {
  					if (id == RenderSky.OBJECT.ARTIFICIAL_SATELLITE) {
  			  			type = Translate.translate(78); // Artificial satellite
  					} else {
  	  					if (id == RenderSky.OBJECT.ASTEROID) type = Translate.translate(73);
  	  					if (id == RenderSky.OBJECT.COMET) type = Translate.translate(74);
  	  					if (id == RenderSky.OBJECT.NEO) type = Translate.translate(1275);
  	  					if (id == RenderSky.OBJECT.PROBE) type = Translate.translate(76);
  	  					if (id == RenderSky.OBJECT.TRANSNEPTUNIAN) type = Translate.translate(1003);
  					}
  				}

  			}
  		}

  		float pos[] = ((float[]) data[1]).clone();
  		String position = "";
  		if (pos != Projection.INVALID_POSITION) {
  	  		if (chart.telescope.invertHorizontal) pos[0] = chart.width - 1 - pos[0];
  	  		if (chart.telescope.invertVertical) pos[1] = chart.height - 1 - pos[1];

  			position = " ("+Translate.translate(880)+": "+(float) pos[0]+", "+(float) pos[1]+")";
  		}

  		//            Object found                        screen position
  		out += Translate.translate(879)+": "+type.toLowerCase() + position + FileIO.getLineSeparator();
//  		appendMsg(language[57]+" "+type);
  		if (ephem != null) {
			if (chart.drawSkyCorrectingLocalHorizon && (obs.getMotherBody() == null || obs.getMotherBody() == TARGET.EARTH))
				ephem.setEquatorialLocation(Ephem.correctEquatorialCoordinatesForRefraction(time, obs, eph, ephem.getEquatorialLocation()));
  			boolean isStar = false;
  			if (star != null || ephem.name.equals(TARGET.SUN.getName())) isStar = true;
  			if (Translate.getDefaultLanguage() == LANGUAGE.ENGLISH) ephem.constellation = Constellation.getConstellation(ephem.constellation, CONSTELLATION_NAME.ENGLISH);
  			if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH) ephem.constellation = Constellation.getConstellation(ephem.constellation, CONSTELLATION_NAME.SPANISH);
  			if (type.equals(Translate.translate(878))) {
  				out += ConsoleReport.getFullEphemReport(ephem, isStar, true, 1);
  			} else {
  				out += ConsoleReport.getBasicEphemReport(ephem, isStar, true, 1);
  			}
  		}
  		if (id == RenderSky.OBJECT.DEEPSKY || id == RenderSky.OBJECT.SUPERNOVA || id == OBJECT.NOVA || 
  				id == OBJECT.METEOR_SHOWER) {
  			objData[0] = Translate.translate(506)+ ": " + objData[0];
  			int cal = objData[0].indexOf("CALDWELL");
  			if (id == RenderSky.OBJECT.DEEPSKY && cal >= 0) {
  				String main = RenderSky.searchDeepSkyObjectReturnMainName(objData[0].substring(cal));
  				if (main != null) objData[0] = Translate.translate(506)+ ": " + objData[0].substring(cal)+" ("+main+")";
  			}
  			objData[1] = Translate.translate(21) + ": " + objData[1];
  			objData[2] = Translate.translate(22) + ": " + objData[2];
			try {
				if (id != OBJECT.METEOR_SHOWER && id != OBJECT.PLANET) {
					double mag = DataSet.parseDouble(objData[3]);
					if (mag > 99) {
						objData[3] = "-";
					} else {
						objData[3] = Functions.formatValue(Double.parseDouble(objData[3]), 1);
					}
				}
			} catch (Exception exc) {}
			if (id == OBJECT.METEOR_SHOWER) {
				double mag = DataSet.parseDouble(objData[3]);
				String rate = "var";
				if (mag > 0) rate = ""+(int) mag;
				objData[3] = Translate.translate(1023) + ": " + rate;				
			} else {
				objData[3] = Translate.translate(157) + ": " + objData[3];
			}
  	  		if (id == RenderSky.OBJECT.DEEPSKY) {
  	  			if (objData.length > 4) {
	  			objData[4] = Translate.translate(486) + ": " + objData[4];
	  			String unit = "\u00b0";
	  			int ndec = 1;
	  			try {
	  				int xp = objData[5].indexOf("x");
	  				if (xp < 0) {
	  					double s = DataSet.parseDouble(objData[5]);
	  					if (s < 0.5 && s > 0) {
	  						s *= 60.0;
	  						unit = "'";
	  						if (s < 0.5) {
	  							s *= 60.0;
	  							unit = "\"";
	  							ndec = 0;
	  						}
	  						objData[5] = Functions.formatValue(s, ndec);
	  					} else {
	  						objData[5] = Functions.formatValue(s, 2);
	  					}
	  				} else {
	  					double s1 = DataSet.parseDouble(objData[5].substring(0, xp));
	  					double s2 = DataSet.parseDouble(objData[5].substring(xp+1));
	  					if (s1 < 0.5 && s1 > 0) {
	  						s1 *= 60.0;
	  						s2 *= 60.0;
	  						unit = "'";
	  						if (s1 < 0.5) {
	  							s1 *= 60.0;
		  						s2 *= 60.0;
	  							unit = "\"";
	  							ndec = 0;
	  						}
	  						objData[5] = Functions.formatValue(s1, ndec)+"x"+Functions.formatValue(s2, ndec);
	  					} else {
	  						objData[5] = Functions.formatValue(s1, 2)+"x"+Functions.formatValue(s2, 2);
	  						if (objData[5].equals("0.00x0.00")) objData[5] = "-";
	  					}	  					
	  				}
	  			} catch (Exception exc) {}
	  			if (!objData[5].equals("-")) {
		  			objData[5] = Translate.translate(308) + ": " + objData[5] + unit;
	  			} else {
		  			objData[5] = Translate.translate(308) + ": " + objData[5];
	  			}
	  			if (objData.length > 6) objData[6] = Translate.translate(241) + ": "+objData[6];
	  			if (Translate.getDefaultLanguage() != LANGUAGE.ENGLISH && objData.length > 6)
	  				objData[6] = DataSet.replaceAll(objData[6], "Type", Translate.translate(1296), true);
	  			if (objData.length > 6 && objData[4].indexOf(Translate.translate(1182)) >= 0) objData = DataSet.eliminateRowFromTable(objData, 7);
  	  			}
  	  		} else {
  	  			if (id == OBJECT.NOVA) {
  	  				objData[4] = Translate.translate(462) + " "+Translate.translate(1304).toLowerCase()+": " + objData[4];
  	  			} else {
  	  				if (id == OBJECT.SUPERNOVA) objData[4] = Translate.translate(462) + " SN: " + objData[4];
  	  			}
  	  		}
  	  		ephem = new EphemElement();
  	  		ephem.angularRadius = 0;
  	  		ephem.rightAscension = Functions.parseRightAscension(objData[1].substring(objData[1].indexOf(":")+1).trim());
  	  		ephem.declination = Functions.parseDeclination(objData[2].substring(objData[2].indexOf(":")+1).trim());
  	  		EphemerisElement eph = this.eph.clone();
  	  		eph.algorithm = null;
  	  		ephem = RiseSetTransit.obtainCurrentOrNextRiseSetTransit(time, obs, eph, ephem, RiseSetTransit.TWILIGHT.HORIZON_ASTRONOMICAL_34arcmin);
  	  		ephem = Ephem.horizontalCoordinates(time, obs, eph, ephem);
  			try {
				double jd = TimeScale.getJD(time, obs, eph, TimeElement.SCALE.TERRESTRIAL_TIME);
  				ephem.constellation = Constellation.getConstellationName(ephem.rightAscension, ephem.declination, eph.getEpoch(jd), eph);
  			} catch (Exception exc) {}
  			if (Translate.getDefaultLanguage() == LANGUAGE.ENGLISH) ephem.constellation = Constellation.getConstellation(ephem.constellation, CONSTELLATION_NAME.ENGLISH);
  			if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH) ephem.constellation = Constellation.getConstellation(ephem.constellation, CONSTELLATION_NAME.SPANISH);
  	  		objData = DataSet.addStringArray(objData, new String[] {
  	  			Translate.translate(28) + ": " + Functions.formatAngle(ephem.azimuth, 1),
	  	  		Translate.translate(29) + ": " + Functions.formatAngle(ephem.elevation, 1),
	  	  		Translate.translate(321) + ": " + ephem.constellation
  	  		});
  	  		if (ephem.rise != null)
	  	  		objData = DataSet.addStringArray(objData, new String[] {
	  	  			Translate.translate(295) + ": " + TimeFormat.formatJulianDayAsDateAndTime(ephem.rise[0], SCALE.LOCAL_TIME),
	  	  		});
  	  		if (ephem.transit != null)
	  	  		objData = DataSet.addStringArray(objData, new String[] {
	  	  			Translate.translate(297) + ": " + TimeFormat.formatJulianDayAsDateAndTime(ephem.transit[0], SCALE.LOCAL_TIME),
	  	  			Translate.translate(298) + ": " + Functions.formatAngle(ephem.transitElevation[0], 1)
	  	  		});
  	  		if (ephem.set != null)
	  	  		objData = DataSet.addStringArray(objData, new String[] {
	  	  			Translate.translate(296) + ": " + TimeFormat.formatJulianDayAsDateAndTime(ephem.set[0], SCALE.LOCAL_TIME),
	  	  		});
  		}
  		if (objData != null)
  			out += DataSet.toString(objData, FileIO.getLineSeparator());
  		if (star != null) { // Show basic star data
  			String pn = skyRender.getRenderSkyObject().getStarProperName(star.name);
  			if (pn == null) pn = "";
  	  		out += Translate.translate(506)+": "+star.name + FileIO.getLineSeparator();
  	  		if (!pn.equals("")) out += Translate.translate(881)+": "+pn + FileIO.getLineSeparator();
  	  		out += Translate.translate(675)+": "+star.spectrum + FileIO.getLineSeparator();
  	  		String doubleVariableData[] = DataSet.toStringArray(star.type, ";", false);
  	  		String doubleVariableTitles[] = DataSet.toStringArray(Translate.translate(882), ";", false);
  	  		String stype[] = new String[] {"N", "D", "V", "B"};
  	  		String stype2[] = new String[] {Translate.translate(181), Translate.translate(32), Translate.translate(33), Translate.translate(32)+" "+Translate.translate(162)+" "+Translate.translate(33).toLowerCase()};
  	  		doubleVariableData[0] = stype2[DataSet.getIndex(stype, doubleVariableData[0])];
  	  		for (int i=0; i<doubleVariableData.length; i++) {
  	  			if (!doubleVariableData[i].trim().equals("")) {
  	  				if (i == 2) {
  	  					String vtype = star.getVariableType();
  	  					if (vtype != null) {
  	  						vtype = " ("+vtype+")";
  	  					} else {
  	  						vtype = "";
  	  					}
  	  					out += doubleVariableTitles[i]+": "+doubleVariableData[i] + vtype + FileIO.getLineSeparator();
  	  				} else {
  	  					out += doubleVariableTitles[i]+": "+doubleVariableData[i] + FileIO.getLineSeparator();
  	  				}
  	  			}
  	  		}
  		}

  		if (ephem != null && ephem.name != null && !ephem.name.equals(""))  {
  			TARGET target = Target.getID(ephem.name);
  			if (target == TARGET.SUN) {
				double jd = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
  				int carrington = Star.getCarringtonRotationNumber(jd);
  				out += Translate.translate(1305)+": "+ carrington + FileIO.getLineSeparator();
  			}
  			if (target == TARGET.Moon) {
				double jd = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
  				int brown = Star.getBrownLunationNumber(jd);
  				out += Translate.translate(1306)+": "+ brown + FileIO.getLineSeparator();
  			}
  		}

  		// Mark Identified object on the screen
  		if (pos != Projection.INVALID_POSITION) {
	  		int size = 5;
	  		Graphics g = panel.getGraphics();
	  		g.setColor(Color.RED);
			if (skyRender.getRenderSkyObject().render.anaglyphMode == ANAGLYPH_COLOR_MODE.TRUE_3D_MODE_LEFT_RIGHT_HALF_WIDTH)
				pos[0] /= 2;
	  		g.drawOval((int) pos[0] - size, (int) pos[1] - size, 2*size, 2*size);
  		}

  		return out;
  	}

  	private void updateChartForDragging() {
  		chartForDragging.drawMilkyWayContoursWithTextures = MILKY_WAY_TEXTURE.NO_TEXTURE;
  		chartForDragging.telescope = chart.telescope;
  		chartForDragging.centralLongitude = chart.centralLongitude;
  		chartForDragging.centralLatitude = chart.centralLatitude;
  		chartForDragging.anaglyphMode = chart.anaglyphMode;
  		chartForDragging.coordinateSystem = chart.coordinateSystem;
  		chartForDragging.drawSkyBelowHorizon = chart.drawSkyBelowHorizon;
  		chartForDragging.drawSkyCorrectingLocalHorizon = chart.drawSkyCorrectingLocalHorizon;
		for (int i=0; i<chart.getNumberOfExternalCatalogs(); i++) {
	  		chartForDragging.drawExternalCatalogs[i] = chart.drawExternalCatalogs[i];
		}
  		chartForDragging.setColorMode(chart.getColorMode());
  		chartForDragging.projection = chart.projection;
  	}

  	/**
  	 * Sets the seconds to wait before updating the sky in real time or 
  	 * when showing a telescope.
  	 * @param sec Seconds.
  	 */
  	public void setUpdateTime(float sec) {
  		updateTime = (int) (sec * 1000);
  	}
  	
  	/**
  	 * Forces the update of the telescope position shown in the sky map.
  	 */
  	public void updateTelescopePosition() {
  		if (timerTel != null) timerTel.start();
  	}
  	
  	private long lastUpdateTime = -1;
  	private boolean updatingTime = false;
	public void actionPerformed(ActionEvent e) {
		// For the timer 
		if (skyRender == null || (menu != null && menu.isVisible()) || updatingTime) return;
		boolean moving = false;
		if (e.getSource() == timerTel) {
  			for (int i=0; i<telescopeControl.length; i++) {
  				if (telescopeControl[i].isMoving()) {
  					moving = true;
  					break;
  				}
  			}
  			if (!moving) { // Detailed check
  	  			for (int i=0; i<telescopeControl.length; i++) {
  	  				if (telescopeControl[i].isMoving(0.5f, 30 * Constant.ARCSEC_TO_RAD)) {
  	  					moving = true;
  	  					break;
  	  				}
  	  			}
  	  			if (!moving) {
  	  				timerTel.stop();
  	  				moving = true;
  	  			}
  			}
		}
		
		if (lastUpdateTime == -1) {
			lastUpdateTime = System.currentTimeMillis();
		}

		long elapsed = System.currentTimeMillis() - lastUpdateTime;
		if (elapsed > updateTime || moving) {
			updatingTime = true;
			lastUpdateTime = System.currentTimeMillis();
			try {
				if (now || timeVel == 100) {
					time = new TimeElement();
				} else {
					time.add(timeVel);
				}

					// Update observer in case it is located in a non-Earth body which
					// is not a planet with a unique TARGET id (comet or asteroid)
					if (obs.getMotherBody() == TARGET.NOT_A_PLANET) {
						EphemerisElement ephC = eph.clone();
						ephC.ephemType = COORDINATES_TYPE.GEOMETRIC;
						ephC.algorithm = ALGORITHM.ORBIT;
						ephC.isTopocentric = false;
						TARGET target = TARGET.Comet;
						target.setIndex(OrbitEphem.getIndexOfComet(obs.getName()));
						if (target.getIndex() == -1) {
							target = TARGET.Asteroid;
							target.setIndex(OrbitEphem.getIndexOfAsteroid(obs.getName()));
							if (target.getIndex() >= 0) ephC.orbit = OrbitEphem.getOrbitalElementsOfAsteroid(target.getIndex());
						} else {
							if (target.getIndex() >= 0) ephC.orbit = OrbitEphem.getOrbitalElementsOfComet(target.getIndex());
						}
						if (target.getIndex() >= 0) {
							double jd = TimeScale.getJD(time, obs, ephC, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
							double[] out = Ephem.eclipticToEquatorial(OrbitEphem.obtainPosition(time, obs, ephC), ephC.getEpoch(jd), eph);
							obs = ObserverElement.parseExtraterrestrialObserver(new ExtraterrestrialObserverElement(obs.getName(), out));
						}
					}

				skyRender.getRenderSkyObject().setSkyRenderElement(skyRender.getRenderSkyObject().render, time, obs, eph);
				if (!now && object != null) {
	  				paintIt();
					setCentralObject(object, null);
				}
				this.paintImage();

				Graphics g = panel.getGraphics();
				int s = 10 + skyRender.getRenderSkyObject().render.drawCoordinateGridFont.getSize() - 14;
				if (chart.width >= 3000) {
		        	int hugeFactor = Math.max((chart.width/1000) - 2, 4);
		        	s = 10 + hugeFactor * 2; 
				}
	        	if (s > 35) s = 35;
				g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, s));
				int x0 = 5;
				int w1 = chart.width/3+2*x0, h = 18;
				int w2 = chart.width/3+2*x0;
				int y0 = chart.height - h;
				int w0 =  chart.width;
				g.setColor(new Color(skyRender.getRenderSkyObject().render.background));
				g.fillRect(0, y0, w1, h);
				g.fillRect(w0-w2, y0, w2, h);
				g.setColor(new Color(skyRender.getRenderSkyObject().render.drawCoordinateGridColor));
				y0 += (h + s) / 2;

				String msg1 = "";

				String t = time.toString();
				if (time.timeScale == SCALE.LOCAL_TIME) t = t.substring(0, t.lastIndexOf(" ")) + " " + time.getTimeScale();
				if (obs.isAnObservatory()) {
					msg1 = obs.getName() + ", "+t;
				} else {
					msg1 = obs.getName() + ", "+t;
				}
				g.drawString(msg1, x0, y0);
			} catch (Exception exc) {}

			if (listShown) updateObjTable();
			updatingTime = false;
		}
	}

	private String t211, t317;

	/**
	 * In case the language changes dynamically, calling method will update
	 * the strings to the new language.
	 */
	public void languageChanged() {
		setStrings();
	}
	private String coordt[], cmt[], projt[];
	private void setStrings() {
		t211 = Translate.translate(211);
		if (t211.toLowerCase().equals("ninguna")) t211 = "Ninguno";
		coordt = new String[CoordinateSystem.COORDINATE_SYSTEMS.length];
		for (int i=0; i<coordt.length; i++) {
			coordt[i] = Translate.translate(CoordinateSystem.COORDINATE_SYSTEMS[i]);
		}
		t317 = Translate.translate(317).toLowerCase();
  		String[] COLOR_MODES = new String[] {
  	  			"White background", "Black background", "Night mode", "White background simple red-cyan anaglyph",
  	  			"White background red-cyan anaglyph", "Black background red-cyan anaglyph", "Night mode red-cyan anaglyph", "White background simple green-red anaglyph",
  	  			"Print mode"
  	  		};
		cmt = new String[COLOR_MODES.length];
		for (int i = 0; i< cmt.length; i++) {
			cmt[i] = Translate.translate(COLOR_MODES[i]);
		}
		projt = new String[Projection.PROJECTIONS.length];
		for (int i = 0; i< projt.length; i++) {
			projt[i] = Translate.translate(Projection.PROJECTIONS[i]);
		}
	}

	/**
	 * Updates the sky in real time.
	 * @param real True for real time mode, false for static.
	 */
	public void setRealTimeUpdate(boolean real) {
		if (real) {
			timeVel = 100.0;
			timer.start();
		} else {
			timeVel = 0;
			timer.stop();
		}
	}

private Thread thread = null;
private Feed feed = null;
private int YEAR = -1;
private class thread0 implements Runnable {
	public void run() {
		try {
			try {
				feed = null;
				if (obs.getMotherBody() != TARGET.EARTH) return;
				AstroDate astroi = new AstroDate(time.astroDate.getYear(), 1, 1, 0, 0, 0);
				AstroDate astrof = new AstroDate(time.astroDate.getYear(), 12, 31, 23, 59, 59);
				YEAR = time.astroDate.getYear();
				TimeElement init = new TimeElement(astroi, SCALE.LOCAL_TIME);
				TimeElement end = new TimeElement(astrof, SCALE.LOCAL_TIME);
				EphemerisElement eph = new EphemerisElement(TARGET.NOT_A_PLANET, EphemerisElement.COORDINATES_TYPE.APPARENT,
						EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.TOPOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
						EphemerisElement.FRAME.DYNAMICAL_EQUINOX_J2000);
				eph.optimizeForSpeed();

				EventReport.setEverythingTo(false);
				EventReport.eclipses = true;
				EventReport.planetaryEvents = true;
				EventReport.MercuryVenusTransits = true;
				EventReport.lunarPhases = true;
				EventReport.equinoxesAndSolstices = true;
				EventReport.meteorShowers = true;

				EventReport.craters = true;
				EventReport.cratersOnlyLunarX = true;
				EventReport.EarthPerihelionAphelion = true;
				EventReport.lunarPerigeeApogee = true;
				EventReport.NEOs = false;
				//EventReport.calendar = true;
				//EventReport.dichotomyAndQuadrature = true;
				//if (chart.drawComets || chart.drawAsteroids)
					EventReport.cometAsteroidVisibleNakedEye = true;


				ObserverElement observer = obs.clone();
				ArrayList<SimpleEventElement> list = EventReport.getEvents(init, end, observer, eph);
				feed = EventReport.getFeed(list, observer);
			} catch (Exception exc) {
				exc.printStackTrace();
			}
		} catch (Exception exc) {}
	}
}
}
