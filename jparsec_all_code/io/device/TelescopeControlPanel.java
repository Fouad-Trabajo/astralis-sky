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
package jparsec.io.device;

import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;

import jparsec.astronomy.Constellation;
import jparsec.astronomy.CoordinateSystem;
import jparsec.astronomy.TelescopeElement;
import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.Functions;
import jparsec.ephem.Target;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.planets.EphemElement;
import jparsec.graph.DataSet;
import jparsec.graph.JPARSECStroke;
import jparsec.graph.SkyChart;
import jparsec.graph.TextLabel;
import jparsec.graph.chartRendering.AWTGraphics;
import jparsec.graph.chartRendering.Graphics.FONT;
import jparsec.graph.chartRendering.PlanetRenderElement;
import jparsec.graph.chartRendering.Projection.PROJECTION;
import jparsec.graph.chartRendering.RenderPlanet;
import jparsec.graph.chartRendering.SkyRenderElement;
import jparsec.graph.chartRendering.SkyRenderElement.FAST_LINES;
import jparsec.graph.chartRendering.SkyRenderElement.LEYEND_POSITION;
import jparsec.graph.chartRendering.SkyRenderElement.MILKY_WAY_TEXTURE;
import jparsec.graph.chartRendering.SkyRenderElement.REALISTIC_STARS;
import jparsec.graph.chartRendering.SkyRenderElement.SUPERIMPOSED_LABELS;
import jparsec.graph.chartRendering.frame.SkyRendering;
import jparsec.io.FileIO;
import jparsec.io.device.GenericCamera;
import jparsec.io.device.GenericCamera.CAMERA_MODEL;
import jparsec.io.device.GenericCamera.FILTER;
import jparsec.io.device.GenericCamera.IMAGE_ID;
import jparsec.io.device.GenericDome;
import jparsec.io.device.GenericDome.DOME_MODEL;
import jparsec.io.device.GenericTelescope;
import jparsec.io.device.GenericTelescope.FOCUS_DIRECTION;
import jparsec.io.device.GenericTelescope.MOUNT;
import jparsec.io.device.GenericTelescope.MOVE_DIRECTION;
import jparsec.io.device.GenericTelescope.MOVE_SPEED;
import jparsec.io.device.GenericTelescope.TELESCOPE_MODEL;
import jparsec.io.device.GenericTelescope.TELESCOPE_TYPE;
import jparsec.io.device.GenericWeatherStation;
import jparsec.io.device.GenericWeatherStation.WEATHER_FORECAST;
import jparsec.io.device.GenericWeatherStation.WEATHER_STATION_MODEL;
import jparsec.io.device.ObservationManager;
import jparsec.io.device.ObservationManager.AVERAGE_METHOD;
import jparsec.io.device.ObservationManager.COMBINATION_METHOD;
import jparsec.io.device.ObservationManager.DRIZZLE;
import jparsec.io.device.ObservationManager.IMAGE_ORIENTATION;
import jparsec.io.device.ObservationManager.INTERPOLATION;
import jparsec.io.device.VirtualCamera;
import jparsec.io.device.VirtualDome;
import jparsec.io.device.VirtualTelescope;
import jparsec.io.device.VirtualWeatherStation;
import jparsec.io.device.implementation.CanonEOS40D50D1000D;
import jparsec.io.device.implementation.CelestronTelescope;
import jparsec.io.device.implementation.MeadeTelescope;
import jparsec.io.device.implementation.SynscanTelescope;
import jparsec.io.device.implementation.WebcamCamera;
import jparsec.io.image.HeaderElement;
import jparsec.io.image.Picture;
import jparsec.math.Constant;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.time.TimeScale;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;
import jparsec.util.Translate.LANGUAGE;
import jparsec.vo.SimbadElement;
import jparsec.vo.SimbadQuery;
import net.miginfocom.swing.MigLayout;

/**
 * A control panel to send/receive commands from the telescope.
 * @author T. Alonso Albi - OAN (Spain)
 */
public final class TelescopeControlPanel extends JPanel implements ActionListener {
	private static final long serialVersionUID = 1L;

	private ObservationManager obsManager;
	private GenericTelescope telescope;
	private GenericDome dome;
	private GenericCamera camera[];
	private GenericWeatherStation weather;
	private boolean cameraShooting[];
	private String cameraStatus[];

	private TimeElement time;
	private ObserverElement obs;
	private EphemerisElement eph;
	private String tname;
	private LocationElement locEq, locHz;
	private SkyChart sc;

	private JTextField raField = new JTextField(14);
	private JTextField decField = new JTextField(14);
	private JTextField azField = new JTextField(14);
	private JTextField objectField = new JTextField(14);
	private JButton gotoButton = new JButton(Translate.translate(1134));
	private JButton syncButton = new JButton(Translate.translate(1144));
	private JButton searchButton = new JButton(Translate.translate(755));

	private JButton connectButton = new JButton(Translate.translate(1145));
	private JButton parkButton = new JButton(Translate.translate(1146));
	private JButton disconnectButton = new JButton(Translate.translate(1147));

	private JButton inButton = new JButton("+");
	private JButton outButton = new JButton("-");
	private Choice focusRateCombo = new Choice();
	private JButton northButton = new JButton("N");
	private JButton eastButton = new JButton("E");
	private JButton southButton = new JButton("S");
	private JButton westButton = new JButton("W");
	private JComboBox slewRateCombo = new JComboBox();
	private JButton leftButton = new JButton(Translate.translate(206));
	private JButton rightButton = new JButton(Translate.translate(207));
	private JButton openButton = new JButton(Translate.translate(1158));
	private JButton closeButton = new JButton(Translate.translate(263));

	private JTextField bulbField[];
	private JTextField orientationField[];
	private JTextField fovField[];
	private Choice iso[], resolution[], shutter[], filter[], imgID[], aperture[];
	private JButton shotButton[], shotButtonMultiple[];

	private JLabel object, raLabel, decLabel, speed, moveLabel, azimuth;
	private JLabel display;
	private Color fc;
	private int updateTime = 1000;
	private boolean telescopePositionUpdated = true;
	private long lastDomet0;
	private boolean firstTime = true;
	private Timer timer;
	/**
	 * Constructs a telescope panel.
	 * @param manager The observation manager with the hardware and reduction properties set.
	 * @param addSky True to add the sky panel at the bottom.
	 * @param addCam True to add camera controls.
	 * @param time Interval in seconds to update the telescope time and location.
	 * @throws JPARSECException If the port is not found or the
	 * telescope cannot be started.
	 */
	public TelescopeControlPanel(ObservationManager manager, boolean addSky, boolean addCam, int time) throws JPARSECException {
		updateTime = time * 1000;
		telescope = null;
		dome = null;
		camera = null;
		weather = null;
		cameraShooting = null;

		TELESCOPE_MODEL telescopeModel = manager.getTelescope();
		DOME_MODEL domeModel = manager.getDome();
		CAMERA_MODEL cameraModel[] = manager.getCameras();
		WEATHER_STATION_MODEL weatherModel = manager.getWeatherStation();
		String telescopePort = manager.getTelescopePort();
		String cameraPort[] = null;
		if (cameraModel != null) {
			cameraPort = new String[cameraModel.length];
			for (int i=0; i<cameraModel.length; i++) {
				cameraPort[i] = manager.getCameraPort(i);
			}
		}
		obsManager = manager;

		try {
			if (telescopeModel.isMeade()) telescope = new MeadeTelescope(telescopeModel, telescopePort);
			if (telescopeModel.isCelestron()) telescope = new CelestronTelescope(telescopeModel, telescopePort);
			if (telescopeModel.isSynscan()) telescope = new SynscanTelescope(telescopeModel, telescopePort);
		} catch (Exception exc) {
			// Fallback to virtual telescope
			telescopeModel = TELESCOPE_MODEL.VIRTUAL_TELESCOPE_EQUATORIAL_MOUNT;
			manager.setTelescope(telescopeModel);
		}
		if (telescopeModel.isVirtual()) telescope = new VirtualTelescope(telescopeModel);
		if (telescope == null) throw new JPARSECException("Could not find/initialize the telescope.");

		if (domeModel != null) {
			if (domeModel.isVirtual()) dome = new VirtualDome(domeModel);
		}
		if (weatherModel != null) {
			if (weatherModel.isVirtual()) weather = new VirtualWeatherStation(null);
		}
		if (cameraModel != null) {
			if (cameraModel.length > 2) throw new JPARSECException("More than 2 cameras not allowed.");
			camera = new GenericCamera[cameraModel.length];
			cameraStatus = new String[camera.length];
			cameraShooting = new boolean[camera.length];
			shotButton = new JButton[cameraModel.length];
			shotButtonMultiple = new JButton[cameraModel.length];
			iso = new Choice[cameraModel.length];
			imgID = new Choice[cameraModel.length];
			aperture = new Choice[cameraModel.length];
			shutter = new Choice[cameraModel.length];
			filter = new Choice[cameraModel.length];
			resolution = new Choice[cameraModel.length];
			bulbField = new JTextField[cameraModel.length];
			orientationField = new JTextField[cameraModel.length];
			fovField = new JTextField[cameraModel.length];
			for (int i=0; i<cameraModel.length; i++) {
				cameraShooting[i] = false;
				iso[i] = new Choice();
				imgID[i] = new Choice();
				aperture[i] = new Choice();
				shutter[i] = new Choice();
				filter[i] = new Choice();
				resolution[i] = new Choice();
				shotButton[i] = new JButton(Translate.translate(1160));
				shotButtonMultiple[i] = new JButton(Translate.translate(1209));
				bulbField[i] = new JTextField(8);
				orientationField[i] = new JTextField(8);
				fovField[i] = new JTextField(8);

				if (cameraModel[i].isVirtual()) {
					camera[i] = new VirtualCamera(cameraModel[i], telescope, i);
				} else {
					if (cameraModel[i] == CAMERA_MODEL.CANON_EOS_40D_400D_50D_500D_1000D) camera[i] = new CanonEOS40D50D1000D(cameraModel[i], null);
					if (cameraModel[i] == CAMERA_MODEL.WEBCAM) camera[i] = new WebcamCamera(cameraModel[i], cameraPort == null ? null: cameraPort[i]);
				}
			}
		}
		telescope.setCameras(camera);

		int vgap = 25;
		fc = Color.lightGray;
		focusRateCombo.add(Translate.translate(1128));
		focusRateCombo.add(Translate.translate(1129));
		
		slewRateCombo.setModel(new DefaultComboBoxModel(new String[] {
				Translate.translate(1130), Translate.translate(1131), Translate.translate(1132), Translate.translate(1133)
		}));
		focusRateCombo.setForeground(fc);
		slewRateCombo.setForeground(fc);
		slewRateCombo.setSelectedIndex(telescope.getMoveSpeed().ordinal());
		focusRateCombo.select(telescope.getFocusSpeed().ordinal());

		// Goto panel
		MigLayout gotoLayout = new MigLayout("wrap 3", "[20%][55%][25%]", "[]"+vgap+"[]");
		JPanel gotoPanel = new JPanel(gotoLayout);
		gotoPanel.setBackground(null);
		gotoPanel.setBorder(getBorder(Translate.translate(1134)));
		object = new JLabel(Translate.translate(1135));
		raLabel = new JLabel(Translate.translate(1136));
		decLabel = new JLabel(Translate.translate(1137));
		object.setForeground(fc);
		raLabel.setForeground(fc);
		decLabel.setForeground(fc);
		gotoPanel.add(object, "align center");
		gotoPanel.add(objectField, "align center");
		gotoPanel.add(searchButton, "align center");
		gotoPanel.add(raLabel, "align center");
		gotoPanel.add(raField, "align center");
		gotoPanel.add(gotoButton, "align center");
		gotoPanel.add(decLabel, "align center");
		gotoPanel.add(decField, "align center");
		gotoPanel.add(syncButton, "align center");

		// Shutdown panel
		MigLayout sdLayout = new MigLayout("wrap 1", "[100%]", "[]"+vgap+"[]");
		JPanel sdPanel = new JPanel(sdLayout);
		sdPanel.setBackground(null);
		sdPanel.setBorder(getBorder(Translate.translate(1138)));
		sdPanel.add(connectButton, "align center");
		//if (telescopeModel.isMeade() && telescopeModel != TELESCOPE_MODEL.MEADE_LX200 && telescopeModel != TELESCOPE_MODEL.MEADE_LX200_16inch)
			sdPanel.add(parkButton, "align center");
		sdPanel.add(disconnectButton, "align center");

		// Focus panel
		MigLayout focusLayout = new MigLayout("wrap 2", "[50%][50%]", "[]"+vgap+"[]");
		JPanel focusPanel = new JPanel(focusLayout);
		focusPanel.setBackground(null);
		focusPanel.setBorder(getBorder(Translate.translate(1139)));
		focusPanel.add(inButton, "span, align center");
		focusPanel.add(outButton, "span, align center");
		speed = new JLabel(Translate.translate(1140));
		speed.setForeground(fc);
		focusPanel.add(speed, "align center");
		focusPanel.add(focusRateCombo, "align center");

		// Dome panel
		MigLayout domeLayout = new MigLayout("wrap 2", "[50%][50%]", "[]"+vgap+"[]");
		JPanel domePanel = new JPanel(domeLayout);
		domePanel.setBackground(null);
		domePanel.setBorder(getBorder(Translate.translate(1157)));
		if (dome != null && dome.getDomeModel().hasLeftRightControl()) {
			domePanel.add(leftButton, "align center");
			domePanel.add(rightButton, "align center");
			azimuth = new JLabel(Translate.translate(28));
			azimuth.setForeground(fc);
			domePanel.add(azimuth, "align center");
			domePanel.add(azField, "align center");
			azField.setEditable(false);
		}
		domePanel.add(openButton, "align center");
		domePanel.add(closeButton, "align center");

		// Camera panel/s
		JPanel cameraPanel[] = null;
		if (cameraModel != null && addCam) {
			cameraPanel = new JPanel[cameraModel.length];
			for (int i=0; i<cameraPanel.length; i++) {
				cameraPanel[i] = getCameraPanel(i, 8);
			}
		}

		// Move panel
		MigLayout moveLayout = new MigLayout("", "[33%][33%][33%]");
		JPanel movePanel = new JPanel(moveLayout);
		movePanel.setBackground(null);
		movePanel.setBorder(getBorder(Translate.translate(1141)));
		movePanel.add(northButton, "cell 1 0, align center");
		movePanel.add(eastButton, "cell 0 1, align center");
		movePanel.add(westButton, "cell 2 1, align center");
		movePanel.add(southButton, "cell 1 2, wrap, align center");
		moveLabel = new JLabel(Translate.translate(1142));
		moveLabel.setForeground(fc);
		movePanel.add(moveLabel, "gaptop 10, align center");
		movePanel.add(slewRateCombo, "span, gaptop 10, align center"); //"cell 1 3 2 1");

		// Global panels
		String constrainColumn = "[48%][30%][22%]";
		if (telescope.hasFocuser() || dome != null) {
			constrainColumn = "[38%][26%][18%][18%]";
			if (telescope.hasFocuser() && dome != null)
				constrainColumn = "[33%][22%][15%][15%][15%]";
		}
		MigLayout globalLayout = new MigLayout("fillx", constrainColumn);
		//setPreferredSize(new Dimension(830, 950));
		setPreferredSize(new Dimension(700, 400));
		this.setLayout(globalLayout);
		this.setBackground(Color.black);
		add(gotoPanel, "growx");
		add(movePanel, "growx");
		if (telescope.hasFocuser()) add(focusPanel, "growx");
		if (dome != null) {
			add(domePanel, "growx");
			add(sdPanel, "growx, wrap");
		} else {
			add(sdPanel, "growx, wrap");
		}
		if (cameraPanel != null)
			for (int i=0; i<cameraPanel.length; i++) { add(cameraPanel[i], "span, growx"); }

		// Override panel height
		int h = getMaxHeight();
		if (h == 0) {
			h = 150;
		} else {
			h += 20;
		}
		setHeight(gotoPanel, h);
		setHeight(movePanel, h);
		setHeight(focusPanel, h);
		setHeight(domePanel, h);
		setHeight(sdPanel, h);

		// Display panel
		Picture pic = new Picture(5, 5); //this.getDisplay());
		display = pic.getAsJLabel();
		add(display, "dock north");
		if (addSky) {
			JPanel sc = getSkyChart().getComponent();
			if (sc != null) add(sc, "dock south");
		}

		// Add listeners
		gotoButton.addActionListener(new GotoActionListener());
		syncButton.addActionListener(new SyncActionListener());
		searchButton.addActionListener(new SearchActionListener());
		inButton.addActionListener(new FocusActionListener(GenericTelescope.FOCUS_DIRECTION.IN));
		outButton.addActionListener(new FocusActionListener(GenericTelescope.FOCUS_DIRECTION.OUT));
		focusRateCombo.addItemListener(new FocusRateItemListener());
		northButton.addActionListener(new SlewActionListener(GenericTelescope.MOVE_DIRECTION.NORTH_UP));
		eastButton.addActionListener(new SlewActionListener(GenericTelescope.MOVE_DIRECTION.EAST_LEFT));
		southButton.addActionListener(new SlewActionListener(GenericTelescope.MOVE_DIRECTION.SOUTH_DOWN));
		westButton.addActionListener(new SlewActionListener(GenericTelescope.MOVE_DIRECTION.WEST_RIGHT));
		slewRateCombo.addItemListener(new SlewRateItemListener());
		connectButton.addActionListener(new ShutdownActionListener(1));
		disconnectButton.addActionListener(new ShutdownActionListener(2));
		parkButton.addActionListener(new ShutdownActionListener(3));
		leftButton.addActionListener(new DomeActionListener(1));
		rightButton.addActionListener(new DomeActionListener(2));
		openButton.addActionListener(new DomeActionListener(3));
		closeButton.addActionListener(new DomeActionListener(4));

		if (cameraPanel != null) {
			for (int i=0; i<cameraPanel.length; i++) {
				iso[i].addItemListener(new CameraActionListener(1, i));
				imgID[i].addItemListener(new CameraActionListener(2, i));
				resolution[i].addItemListener(new CameraActionListener(3, i));
				shutter[i].addItemListener(new CameraActionListener(4, i));
				filter[i].addItemListener(new CameraActionListener(5, i));
				aperture[i].addItemListener(new CameraActionListener(6, i));
				shotButton[i].addActionListener(new CameraActionListener(0, i));
				shotButtonMultiple[i].addActionListener(new CameraActionListener(-1, i));
			}
		}

		timer = new Timer(this.updateTime, this);
		timer.setRepeats(true);
		timer.start();
	}
	private void setChoice(Choice c, String[] val) {
		for (int i=0; i<val.length; i++) {
			c.add(val[i]);
		}
	}
	private int getMaxHeight() {
		int max = 0;
		for (int i = 0; i < this.getComponentCount(); i++) {
			Component c = this.getComponent(i);
			if (c instanceof JPanel) {
				Dimension d = c.getPreferredSize();
				if (d.height > max) max = d.height;
			}
		}
		return max;
	}
	private void setHeight(JPanel panel, int h) {
		Dimension p = panel.getPreferredSize();
		p.height = h;
		panel.setPreferredSize(p);
	}
	
	public void updateLook() {
		Color cb = Color.BLACK;
		Font f = gotoButton.getFont();
		fc = Color.lightGray;
		
		slewRateCombo.setModel(new DefaultComboBoxModel(new String[] {
				Translate.translate(1130), Translate.translate(1131), Translate.translate(1132), Translate.translate(1133)
		}));
		object.setText(Translate.translate(1135));
		raLabel.setText(Translate.translate(1136));
		decLabel.setText(Translate.translate(1137));
		speed.setText(Translate.translate(1140));
		if (azimuth != null) azimuth.setText(Translate.translate(28));
		moveLabel.setText(Translate.translate(1142));
		gotoButton.setText(Translate.translate(1134));
		syncButton.setText(Translate.translate(1144));
		searchButton.setText(Translate.translate(755));
		connectButton.setText(Translate.translate(1145));
		parkButton.setText(Translate.translate(1146));
		disconnectButton.setText(Translate.translate(1147));
		westButton.setText(Translate.getDefaultLanguage() == LANGUAGE.SPANISH ? "O" : "W");
		leftButton.setText(Translate.translate(206));
		rightButton.setText(Translate.translate(207));
		openButton.setText(Translate.translate(1158));
		closeButton.setText(Translate.translate(263));
		
		if (object.getParent() instanceof JPanel) {
			JPanel p1 = (JPanel) object.getParent();
			p1.setBorder(getBorder(Translate.translate(1134)));
		}
		if (connectButton.getParent() instanceof JPanel) {
			JPanel p1 = (JPanel) connectButton.getParent();
			p1.setBorder(getBorder(Translate.translate(1138)));
		}
		if (speed.getParent() instanceof JPanel) {
			JPanel p1 = (JPanel) speed.getParent();
			p1.setBorder(getBorder(Translate.translate(1139)));
		}
		if (northButton.getParent() instanceof JPanel) {
			JPanel p1 = (JPanel) northButton.getParent();
			p1.setBorder(getBorder(Translate.translate(1141)));
		}
		if (azimuth != null && azimuth.getParent() instanceof JPanel) {
			JPanel p1 = (JPanel) azimuth.getParent();
			p1.setBorder(getBorder(Translate.translate(28)));
		}
		tname = null;			
			
		gotoButton.setBackground(cb);
		syncButton.setBackground(cb);
		searchButton.setBackground(cb);
		inButton.setBackground(cb);
		outButton.setBackground(cb);
		focusRateCombo.setBackground(cb);
		northButton.setBackground(cb);
		eastButton.setBackground(cb);
		southButton.setBackground(cb);
		westButton.setBackground(cb);
		slewRateCombo.setBackground(cb);
		connectButton.setBackground(cb);
		disconnectButton.setBackground(cb);
		parkButton.setBackground(cb);
		leftButton.setBackground(cb);
		rightButton.setBackground(cb);
		openButton.setBackground(cb);
		closeButton.setBackground(cb);	
		
		gotoButton.setForeground(fc);
		syncButton.setForeground(fc);
		searchButton.setForeground(fc);
		inButton.setForeground(fc);
		outButton.setForeground(fc);
		focusRateCombo.setForeground(fc);
		northButton.setForeground(fc);
		eastButton.setForeground(fc);
		southButton.setForeground(fc);
		westButton.setForeground(fc);
		slewRateCombo.setForeground(fc);
		connectButton.setForeground(fc);
		disconnectButton.setForeground(fc);
		parkButton.setForeground(fc);
		leftButton.setForeground(fc);
		rightButton.setForeground(fc);
		openButton.setForeground(fc);
		closeButton.setForeground(fc);	
		
		gotoButton.setFont(f);
		syncButton.setFont(f);
		searchButton.setFont(f);
		inButton.setFont(f);
		outButton.setFont(f);
		focusRateCombo.setFont(f);
		northButton.setFont(f);
		eastButton.setFont(f);
		southButton.setFont(f);
		westButton.setFont(f);
		slewRateCombo.setFont(f);
		connectButton.setFont(f);
		disconnectButton.setFont(f);
		parkButton.setFont(f);
		leftButton.setFont(f);
		rightButton.setFont(f);
		openButton.setFont(f);
		closeButton.setFont(f);	
				
		updateAfterResize();
		
		object.setBackground(cb);
		object.setFont(f);
		raLabel.setBackground(cb);
		raLabel.setFont(f);
		decLabel.setBackground(cb);
		decLabel.setFont(f);
		speed.setBackground(cb);
		speed.setFont(f);
		moveLabel.setBackground(cb);
		moveLabel.setFont(f);
		if (azimuth != null) {
			azimuth.setBackground(cb);
			azimuth.setFont(f);
		}
		display.setBackground(cb);
		display.setFont(f);
		
		revalidate();
	}
	
	public void updateAfterResize() {
		int bw2 = northButton.getParent().getWidth() / 4;
		if (bw2 > northButton.getHeight() * 2) bw2 = northButton.getHeight() * 2;
		northButton.setPreferredSize(new Dimension(bw2, bw2));
		eastButton.setPreferredSize(new Dimension(bw2, bw2));
		southButton.setPreferredSize(new Dimension(bw2, bw2));
		westButton.setPreferredSize(new Dimension(bw2, bw2));
	}
	
	/**
	 * Returns a titled border for a given title.
	 * @param title The title.
	 * @return The border.
	 */
	public static TitledBorder getBorder(String title) {
		Font font = new Font("Default", Font.BOLD, 18);
		return BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY, 2), title,
				TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, font, Color.ORANGE);
	}
	private JPanel getCameraPanel(int index, int vgap) {
		MigLayout cameraLayout = new MigLayout("wrap 6", "[15%][15%][20%][20%][20%][10%]", "[]"+vgap+"[]");
		JPanel cameraPanel = new JPanel(cameraLayout);
		cameraPanel.setBackground(null);
		String title = camera[index].getCameraName(); //Translate.translate(1173);
		if (camera.length == 1 && camera[index].getCameraModel().isVirtual()) title = Translate.translate(1187);
		//if (camera.length > 1) title += " "+(index+1)+" ("+camera[index].getCameraName()+")";
		cameraPanel.setBorder(getBorder(title));
		if (camera != null) {
			JLabel limgID = new JLabel(Translate.translate(1161));
			limgID.setForeground(fc);
			JLabel lfilter = new JLabel(Translate.translate(1162));
			lfilter.setForeground(fc);
			JLabel limgRES = new JLabel(Translate.translate(1163));
			limgRES.setForeground(fc);
			setChoice(resolution[index], camera[index].getPossibleResolutionModes());
			setChoice(imgID[index], Translate.translate(GenericCamera.IMAGE_IDS));
			setChoice(filter[index], GenericCamera.FILTER.getFilterNames());
			resolution[index].select(camera[index].getResolutionMode());
			imgID[index].select(camera[index].getImageID().ordinal());
			filter[index].select(camera[index].getFilter().ordinal());
			JLabel orientation = new JLabel(Translate.translate(1170)+" (\u00b0)");
			orientation.setForeground(fc);

			if (camera[index].getCameraModel().isDLSR()) {
				JLabel liso = new JLabel("ISO");
				liso.setForeground(fc);
				cameraPanel.add(liso, "align left");
				setChoice(iso[index], camera[index].getPossibleISOs());
				iso[index].select(camera[index].getISO());
				cameraPanel.add(iso[index], "align left");
				cameraPanel.add(limgRES, "align left");
				cameraPanel.add(resolution[index], "align left");
				JLabel laper = new JLabel(Translate.translate(1176));
				laper.setForeground(fc);
				cameraPanel.add(laper, "align left");
				setChoice(aperture[index], camera[index].getPossibleApertures());
				aperture[index].select(camera[index].getAperture());
				cameraPanel.add(aperture[index], "align left");


				setChoice(shutter[index], camera[index].getPossibleExpositionTimes());
				shutter[index].select(camera[index].getExpositionTime());
				JLabel lshutter = new JLabel(Translate.translate(180)+" (s)");
				lshutter.setForeground(fc);
				cameraPanel.add(lshutter, "align left");
				cameraPanel.add(shutter[index], "align left");
				JLabel ltime = new JLabel(Translate.translate(1169)+" (s)");
				ltime.setForeground(fc);
				cameraPanel.add(ltime, "align left");
				cameraPanel.add(bulbField[index], "align left");
				cameraPanel.add(orientation, "align left");
				cameraPanel.add(orientationField[index], "align left, wrap");

				cameraPanel.add(limgID, "align left");
				cameraPanel.add(imgID[index], "align left");
				cameraPanel.add(lfilter, "align left");
				cameraPanel.add(filter[index], "align left");
			} else {
				cameraPanel.add(limgID, "align left");
				cameraPanel.add(imgID[index], "align left");
				JLabel ltime = null;
				if (camera[index].getCameraModel().isWebcam()) {
					ltime = new JLabel(Translate.translate(1186));
				} else {
					ltime = new JLabel(Translate.translate(180)+" (s)");
				}
				ltime.setForeground(fc);
				cameraPanel.add(ltime, "align left");
				cameraPanel.add(bulbField[index], "align left");
				cameraPanel.add(orientation, "align left");
				cameraPanel.add(orientationField[index], "align left");

				cameraPanel.add(limgRES, "align left");
				cameraPanel.add(resolution[index], "align left");
				cameraPanel.add(lfilter, "align left");
				cameraPanel.add(filter[index], "align left");
			}
			JLabel lfov = new JLabel(Translate.translate(1181));
			lfov.setForeground(fc);
			cameraPanel.add(lfov, "align left");
			cameraPanel.add(fovField[index], "align left");

			fovField[index].setText("0.5");
			orientationField[index].setText(""+camera[index].getCameraOrientation());
			bulbField[index].setText(""+camera[index].getCCDorBulbModeTime());

			if (obsManager.reductionPossible()) {
				cameraPanel.add(shotButton[index], "align center, span 3");
				cameraPanel.add(shotButtonMultiple[index], "align center, span 3");
			} else {
				cameraPanel.add(shotButton[index], "align center, span");
			}
		}
		return cameraPanel;
	}


	private class GotoActionListener implements ActionListener {
		public void actionPerformed(ActionEvent evt) {
			gotoObject();
		}
	}
	private boolean gotoObject() {
		if (!telescope.isConnected()) return false;

		if (telescope.isMoving()) {
			telescope.stopMoving();
			gotoButton.setText(Translate.translate(1134));
			gotoButton.setForeground(fc);
			northButton.setForeground(fc);	
			eastButton.setForeground(fc);
			westButton.setForeground(fc);
			southButton.setForeground(fc);
			
			SlewActionListener na = (SlewActionListener) northButton.getActionListeners()[0];
			SlewActionListener sa = (SlewActionListener) southButton.getActionListeners()[0];
			SlewActionListener ea = (SlewActionListener) eastButton.getActionListeners()[0];
			SlewActionListener wa = (SlewActionListener) westButton.getActionListeners()[0];
			na.slewing = false;
			sa.slewing = false;
			ea.slewing = false;
			wa.slewing = false;
			return false;
		} else {
			try {
				telescope.setObjectCoordinates(new LocationElement(Functions.parseRightAscension(raField.getText()),
					Functions.parseDeclination(decField.getText()), 1), objectField.getText());
			} catch (Exception exc) {
				JOptionPane.showMessageDialog(null, Translate.translate(1156), Translate.translate(240), JOptionPane.WARNING_MESSAGE);
				return false;
			}

			boolean go = false;
			LocationElement locHz = new LocationElement();
			try {
				locHz = CoordinateSystem.equatorialToHorizontal(telescope.getObjectCoordinates(), telescope.getTime(), telescope.getObserver(), eph);
			} catch (JPARSECException e) {
			}
			if (locHz.getLatitude() > 0 ) {
				go = telescope.gotoObject();
				//String pos = Functions.formatRAOnlyMinutes(telescope.getObjectCoordinates().getLongitude(), 1)+
				//		", "+Functions.formatDECOnlyMinutes(telescope.getObjectCoordinates().getLatitude(), 0);
			}
			if (go) {
				if (dome != null && dome.getDomeModel().hasLeftRightControl()) {
					dome.sync(locHz);
				}
				gotoButton.setForeground(Color.MAGENTA);
				gotoButton.setText(Translate.translate(1143));
				Thread gotot = new Thread(new Runnable() {
					@Override
					public void run() {
						double tolerance = 30 * Constant.ARCSEC_TO_RAD;
						try {
							boolean domeReady = true;
							do {
								Thread.sleep(2000);
								if (dome != null && dome.isMoving()) domeReady = false;
							} while (telescope.isMoving(1.0f, tolerance) || !domeReady);
							gotoButton.setText(Translate.translate(1134));
							gotoButton.setForeground(fc);
							
							actionPerformed(null);
						} catch (Exception exc) {
							exc.printStackTrace();
						}
					}
				});
				gotot.start();
			} else {
				JOptionPane.showMessageDialog(null, Translate.translate(1156), Translate.translate(240), JOptionPane.WARNING_MESSAGE);
			}
			return go;
		}
	}
	
	private class SyncActionListener implements ActionListener {
		public void actionPerformed(ActionEvent evt) {
			if (telescope.isConnected()) {
				telescope.setObjectCoordinates(new LocationElement(Functions.parseRightAscension(raField.getText()),
						Functions.parseDeclination(decField.getText()), 1), objectField.getText());
				telescope.sync();
			}
		}
	}
	private class SearchActionListener implements ActionListener {
		public void actionPerformed(ActionEvent evt) {
			String s = objectField.getText();
			if (s == null || s.equals("")) {
				LocationElement loc = telescope.getObjectCoordinates();
				raField.setText(Functions.formatRA(loc.getLongitude(), 1));
				decField.setText(Functions.formatDEC(loc.getLatitude(), 0));
			}

			String error = validateObject(s);
			if (error != null && !error.equals(""))
				objectField.setText(error);
		}
	}
	private String validateObject(String s) {
		if (s.startsWith("+") || s.startsWith("-")) {
			try {
				String offRA = FileIO.getField(1, s, ",", false);
				String offDEC = FileIO.getField(2, s, ",", false);

				double dDEC = 0, dRA = 0;
				if (offDEC != null && !offDEC.equals("")) dDEC = Functions.parseDeclination(offDEC);
				if (offRA != null && !offRA.equals("")) {
					try {
						dRA = Functions.parseRightAscension(offRA);
					} catch (Exception exc2) {
						dRA = Functions.parseDeclination(offRA);
					}
				}

				double ra = Functions.parseRightAscension(raField.getText());
				double dec = Functions.parseDeclination(decField.getText());

				dec += dDEC;
				ra += dRA / Math.cos(dec);
				raField.setText(Functions.formatRA(ra, 1));
				decField.setText(Functions.formatDEC(dec, 0));
				return null;
			} catch (Exception exc) {
				return "";
			}
		}

		TARGET t = TARGET.NOT_A_PLANET;
		try {
			t = Target.getID(s);
			if (t == TARGET.NOT_A_PLANET && Translate.getDefaultLanguage() != LANGUAGE.ENGLISH)
				t = jparsec.ephem.Target.getIDFromEnglishName(s);
			if (t == TARGET.SUN) 
				JOptionPane.showMessageDialog(null, Translate.translate(1154), Translate.translate(240), JOptionPane.WARNING_MESSAGE);
		} catch (Exception e) { }

		try {
			LocationElement loc = new LocationElement(s, true); //!telescope.getTelescopeModel().isJ2000());
			if (t != null && t != TARGET.NOT_A_PLANET) {
				EphemElement ephem = new EphemElement();
				ephem.setEquatorialLocation(loc);
				ephem = Ephem.topocentricCorrection(time, obs, eph, ephem);
				loc = ephem.getEquatorialLocation();
			}
			loc = Ephem.correctEquatorialCoordinatesForRefraction(time, obs, eph, loc);
			raField.setText(Functions.formatRA(loc.getLongitude(), 1));
			decField.setText(Functions.formatDEC(loc.getLatitude(), 0));
		} catch (Exception exc) {
			try {
				SimbadElement se = SimbadQuery.query(s);
				LocationElement loc = se.getLocation();
				loc = Ephem.fromJ2000ToApparentGeocentricEquatorial(loc, time, obs, eph);
				loc = Ephem.correctEquatorialCoordinatesForRefraction(time, obs, eph, loc);
				raField.setText(Functions.formatRA(loc.getLongitude(), 1));
				decField.setText(Functions.formatDEC(loc.getLatitude(), 0));
			} catch (Exception exc2) {
				return DataSet.replaceAll(Translate.translate(1155), "%obj", s, true);
			}
		}
		return null;
	}
	private class FocusActionListener implements ActionListener {
		private final FOCUS_DIRECTION direction;
		private boolean focusing=false;
		public FocusActionListener(FOCUS_DIRECTION dir) {
			direction=dir;
		}
		public void actionPerformed(ActionEvent evt) {
			if (!telescope.isConnected()) return;

			if(focusing) {
				telescope.stopFocus();
				focusing=false;
				if (direction == FOCUS_DIRECTION.IN) {
					inButton.setForeground(Color.BLACK);
				} else {
					outButton.setForeground(Color.BLACK);
				}
			} else {
				telescope.startFocus(direction);
				focusing=true;
				if (direction == FOCUS_DIRECTION.IN) {
					inButton.setForeground(Color.MAGENTA);
				} else {
					outButton.setForeground(Color.MAGENTA);
				}
			}
		}
	}
	private class FocusRateItemListener implements ItemListener {
		public void itemStateChanged(ItemEvent evt) {
			telescope.setFocusSpeed(GenericTelescope.FOCUS_SPEED.values()[focusRateCombo.getSelectedIndex()]);
		}
	}
	private class SlewActionListener implements ActionListener {
		private final MOVE_DIRECTION direction;
		private boolean slewing=false;
		public SlewActionListener(MOVE_DIRECTION dir) {
			direction=dir;
		}
		public void actionPerformed(ActionEvent evt) {
			if (!telescope.isConnected()) return;

			if(slewing) {
				telescope.stopMove(direction);
				slewing=false;
				if (direction == MOVE_DIRECTION.NORTH_UP) northButton.setForeground(fc);
				if (direction == MOVE_DIRECTION.EAST_LEFT) eastButton.setForeground(fc);
				if (direction == MOVE_DIRECTION.WEST_RIGHT) westButton.setForeground(fc);
				if (direction == MOVE_DIRECTION.SOUTH_DOWN) southButton.setForeground(fc);
			} else {
				telescope.startMove(direction);
				slewing=true;
				if (direction == MOVE_DIRECTION.NORTH_UP) northButton.setForeground(Color.MAGENTA);
				if (direction == MOVE_DIRECTION.EAST_LEFT) eastButton.setForeground(Color.MAGENTA);
				if (direction == MOVE_DIRECTION.WEST_RIGHT) westButton.setForeground(Color.MAGENTA);
				if (direction == MOVE_DIRECTION.SOUTH_DOWN) southButton.setForeground(Color.MAGENTA);
			}
			if (telescope.isMoving()) {
				gotoButton.setText(Translate.translate(1143));
				gotoButton.setForeground(Color.MAGENTA);
			} else {
				gotoButton.setText(Translate.translate(1134));
				gotoButton.setForeground(fc);
			}
		}
	}
	private class ShutdownActionListener implements ActionListener {
		private final int id;
		public ShutdownActionListener(int i) {
			id = i;
		}
		public void actionPerformed(ActionEvent evt) {
			if (id == 1) {
				try {
					telescope.connect();
				} catch (JPARSECException exc) {
					exc.printStackTrace();
				}
			}
			if (id == 2) telescope.disconnect();
			if (id == 3) {
				if (telescope.isConnected()) {
					telescope.setParkPosition(obsManager.getTelescopeParkPosition());
					if (telescope.park()) {
						//String pos = Functions.formatRAOnlyMinutes(obsManager.getTelescopeParkPosition().getLongitude(), 1)+", "+
						//		Functions.formatDECOnlyMinutes(obsManager.getTelescopeParkPosition().getLatitude(), 0);
			        	
						parkButton.setText(Translate.translate(1225));
					}
				} else {
					if (telescope.unpark()) parkButton.setText(Translate.translate(1146));
				}
			}
		}
	}
	private class DomeActionListener implements ActionListener {
		private static final double DOME_ANGLE = 0.5 * Constant.DEG_TO_RAD;
		private final int id;
		public DomeActionListener(int i) {
			id = i;
		}
		public void actionPerformed(ActionEvent evt) {
			int mod = evt.getModifiers();
			double factor = 1;
			if ((mod & ActionEvent.ALT_MASK) == ActionEvent.ALT_MASK) factor += 1;
			if ((mod & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK) factor += 2;
			if ((mod & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK) factor += 4;
			if (id == 1) dome.rotateLeft(factor * DOME_ANGLE);
			if (id == 2) dome.rotateRight(factor * DOME_ANGLE);
			if (id == 3) {
				if (dome.isOpen()) {
					try {
						LocationElement locHz = telescope.getHorizontalPosition();
						dome.sync(locHz);
					} catch (Exception exc) {
						exc.printStackTrace();
					}
				} else {
					dome.open();
				}
			}
			if (id == 4) dome.close();
		}
	}
	private class CameraActionListener implements ActionListener, ItemListener {
		private final int id, cameraIndex;
		public CameraActionListener(int i, int cameraIndex) {
			id = i;
			this.cameraIndex = cameraIndex;
		}
		@Override
		public void actionPerformed(ActionEvent evt) {
			if ((dome != null && (!dome.isOpen() || !dome.isSync(telescope.getHorizontalPosition())) && camera[cameraIndex].getImageID() == IMAGE_ID.ON_SOURCE) ||
					camera[cameraIndex].isShooting() || ((camera[cameraIndex].getImageID() == IMAGE_ID.FLAT || camera[cameraIndex].getImageID() == IMAGE_ID.ON_SOURCE) && (telescope.isMoving() || !telescope.isTracking()))) {
				JOptionPane.showMessageDialog(null, Translate.translate(1175), Translate.translate(1174), JOptionPane.WARNING_MESSAGE);
			} else {
				try { camera[cameraIndex].setCCDorBulbModeTime(Integer.parseInt(bulbField[cameraIndex].getText())); } catch (Exception exc) {}

				int nshots = 1;
				if (id == -1) {
					if (camera[cameraIndex].getImageID() == IMAGE_ID.TEST) {
						JOptionPane.showMessageDialog(null, Translate.translate(1215), Translate.translate(1216), JOptionPane.WARNING_MESSAGE);
						return;
					}

					try {
						String options[] = new String[] {
								"3 "+Translate.translate(1214),
								"5 "+Translate.translate(1214),
								"10 "+Translate.translate(1214),
								"15 "+Translate.translate(1214),
								"25 "+Translate.translate(1214),
								"50 "+Translate.translate(1214),
						};
						int result = JOptionPane.showOptionDialog(null, Translate.translate(1212), Translate.translate(1213), JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
								null, //new ImageIcon(ReadFile.readImageResource(FileIO.DATA_IMAGES_DIRECTORY+"planetaryNeb_transparentOK.png")),
								options, options[0]);
						if (result < 0) return;

						nshots = (new int[] {3, 5, 10, 15, 25, 50})[result];

						if (obsManager.getCombineMethod() == null && obsManager.reductionEnabled()) {
							result = JOptionPane.showOptionDialog(null, Translate.translate(1210), Translate.translate(1211), JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
									null, //new ImageIcon(ReadFile.readImageResource(FileIO.DATA_IMAGES_DIRECTORY+"planetaryNeb_transparentOK.png")),
									ObservationManager.COMBINATION_METHODS, ObservationManager.COMBINATION_METHODS[0]);
							obsManager.setCombineMethod(COMBINATION_METHOD.values()[result]);
						}
					} catch (Exception exc) {
						exc.printStackTrace();
					}
/*				} else {
					// FIXME: just to test commands
					String command = JOptionPane.showInputDialog("Command:");
					if (command == null || command.equals("")) return;
					if (executedCommandLog() != null) System.out.println(executedCommandLog());
					try {
						executeCommand(new String[] {command});
					} catch (Exception exc) {
						if (exc instanceof JPARSECException) {
							System.err.println(((JPARSECException) exc).getMessage());
						} else {
							exc.printStackTrace();
						}
					}
					return;
*/				}

				try {
				boolean ok = camera[cameraIndex].shotAndDownload(false);
				if (ok) {
					setCameraShooting(cameraIndex, true);
					cameraStatus[cameraIndex] = ""+nshots+" "+obsManager.reductionEnabled() + " " + obsManager.getAutoReduceOnFramesEnabled();
					if (nshots > 1 && obsManager.reductionEnabled() &&
							!imgID[cameraIndex].getSelectedItem().equals(Translate.translate(GenericCamera.IMAGE_IDS[GenericCamera.IMAGE_ID.ON_SOURCE.ordinal()]))) obsManager.setReductionEnabled(false);
				}
				} catch (Exception exc) {
					exc.printStackTrace();
				}
			}
		}

		@Override
		public void itemStateChanged(ItemEvent arg0) {
			if (id == 1) camera[cameraIndex].setISO(iso[cameraIndex].getSelectedItem());
			if (id == 2) camera[cameraIndex].setImageID(IMAGE_ID.values()[imgID[cameraIndex].getSelectedIndex()]);
			if (id == 3) camera[cameraIndex].setResolutionMode(resolution[cameraIndex].getSelectedItem());
			if (id == 4) camera[cameraIndex].setExpositionTime(shutter[cameraIndex].getSelectedItem());
			if (id == 5) camera[cameraIndex].setFilter(FILTER.values()[filter[cameraIndex].getSelectedIndex()]);
			if (id == 6) camera[cameraIndex].setAperture(aperture[cameraIndex].getSelectedItem());
		}
	}
	private class SlewRateItemListener implements ItemListener {
		public void itemStateChanged(ItemEvent evt) {
			telescope.setMoveSpeed(MOVE_SPEED.values()[slewRateCombo.getSelectedIndex()]);
		}
	}
	@Override
	public void actionPerformed(ActionEvent arg0) {
		if (firstTime) {
			long t0 = System.currentTimeMillis();
			lastDomet0 = t0;
			if (dome != null && dome.getDomeModel().hasLeftRightControl()) {
				try {
					LocationElement locHz = telescope.getHorizontalPosition();
					dome.sync(locHz);
				} catch (Exception exc) {
					exc.printStackTrace();
				}
			}
			firstTime = false;
		}

		try {
/*			if (Runtime.getRuntime().availableProcessors() >= 2) {
				if (displayImg == null || !displayImg.isAlive()) {
					displayImg = new Thread(new displayThread());
					displayImg.start();
				}
			} else {
*/				display.setIcon(new ImageIcon(getDisplay()));
//			}

			if (camera != null) {
				for (int i=0; i<camera.length; i++) {
					try { telescope.setFieldOfView(Constant.DEG_TO_RAD * Double.parseDouble(fovField[i].getText()), i); } catch (Exception exc) {}
					try { camera[i].setCameraOrientation(Constant.DEG_TO_RAD * Double.parseDouble(orientationField[i].getText())); } catch (Exception exc) {}
				}
			}

			if (!checkWeatherConditions()) {
				if (((dome != null && dome.isOpen()) || (telescope != null && telescope.isConnected()))) {
					if (dome != null) dome.close();
					if (telescope != null) telescope.disconnect();
					JOptionPane.showMessageDialog(null, Translate.translate(1179), Translate.translate(240), JOptionPane.WARNING_MESSAGE);
				} else {
					if (dome != null) dome.close();
					if (telescope != null) telescope.disconnect();
				}
			}

			if (dome != null && dome.getDomeModel().hasLeftRightControl()) {
				azField.setText(Functions.formatAngleAsDegrees(dome.getAzimuth(), 3)+"\u00b0");
				long t1 = System.currentTimeMillis();
				if (t1 - lastDomet0 > dome.getSyncTime()*1000 && dome.isOpen() && !dome.isMoving()) {
					try {
						LocationElement locHz = telescope.getHorizontalPosition();
						dome.sync(locHz);
					} catch (Exception exc) {
						exc.printStackTrace();
					}
					lastDomet0 = t1;
				}
			}
		} catch (Exception exc) {
			System.out.println("Could not update telescope status!");
		}
	}

	/**
	 * Returns if the weather conditions are fine to continue observations or not.
	 * @param weather The weather station.
	 * @return True if everything is fine, false if alarm should be launched.
	 */
	private boolean checkWeatherConditions() {
		boolean ok = true;
		double c[] = obsManager.getWeatherAlarmConditions();
		double humidityLimit = c[0], windSpeedLimit = c[1], temperatureMaxLimit = c[2], temperatureMinLimit = c[3];
		if (weather != null) {
			if (weather.isRaining()) {
				ok = false;
			} else {
				if (weather.getHumidity() > humidityLimit || weather.getHumidityInside() > humidityLimit) {
					ok = false;
				} else {
					if (weather.getWindSpeed() > windSpeedLimit) {
						ok = false;
					} else {
						if (weather.getTemperature() > temperatureMaxLimit || weather.getTemperatureInside() > temperatureMaxLimit) ok = false;
						if (weather.getTemperature() < temperatureMinLimit || weather.getTemperatureInside() < temperatureMinLimit) ok = false;
					}
				}
			}
		}
		return ok;
	}

	/**
	 * Returns the instance to the telescope object.
	 * @return The telescope instance.
	 */
	public GenericTelescope getTelescopeInstance() {
		return telescope;
	}

	/**
	 * Returns the telescope name.
	 * @return Telescope name.
	 */
	public String getTelescopeName() {
		return tname;
	}
	
	/**
	 * Returns an image with the display.
	 * @return The image.
	 * @throws JPARSECException If an error occurs.
	 */
	public BufferedImage getDisplay() throws JPARSECException {
		Dimension p = getPreferredSize();
 
		locEq = telescope.getEquatorialPosition();
		
		ObserverElement obs0 = null;
		if (obs != null) obs0 = obs.clone();
		time = telescope.getTime();
		if (obs == null) obs = telescope.getObserver();
		if (eph == null) {
			eph = new EphemerisElement(TARGET.NOT_A_PLANET, EphemerisElement.COORDINATES_TYPE.APPARENT,
					EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.TOPOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
					EphemerisElement.FRAME.DYNAMICAL_EQUINOX_J2000,
					EphemerisElement.ALGORITHM.MOSHIER);
			eph.preferPrecisionInEphemerides = false;
			eph.correctForEOP = false;
			eph.correctForPolarMotion = false;
		}
		if (sc != null && obs0 != null && !obs.equals(obs0))
			sc.update(sc.chart, time, obs, eph, null);

		if (tname == null) tname = telescope.getTelescopeName();
		LocationElement loc = telescope.getApparentEquatorialPosition();
		// XXX
		locHz = CoordinateSystem.equatorialToHorizontal(loc, time, obs, eph); 
		//locHz = telescope.getHorizontalPosition();
		boolean isMoving = telescope.isMoving(), isTracking = telescope.isTracking(), isAligned = telescope.isAligned(),
				isConnected = telescope.isConnected(), isEqMount = telescope.getMount() == MOUNT.EQUATORIAL;

		if (isEqMount) {
			northButton.setText("N");
			eastButton.setText("E");
			southButton.setText("S");
			westButton.setText("W");
			if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH)
				westButton.setText("O");
		} else {
			northButton.setText(Translate.translate(204));
			eastButton.setText(Translate.translate(206));
			southButton.setText(Translate.translate(205));
			westButton.setText(Translate.translate(207));
		}
		if (dome != null && dome.getDomeModel().hasLeftRightControl()) {
			if (dome.isOpen()) {
				openButton.setText(Translate.translate(1144));
			} else {
				openButton.setText(Translate.translate(1158));
			}
		}
		if (sc != null && (isMoving || !telescopePositionUpdated)) {
			sc.setCentralObject(null);
			sc.setCentralObject("skyloc_eq_"+(loc.getLongitude()*Constant.RAD_TO_DEG)+"_"+(loc.getLatitude()*Constant.RAD_TO_DEG));
			telescopePositionUpdated = false;
			if (!isMoving) {
				telescopePositionUpdated = true;
			}
			sc.setCentralObject(null);
		}

		int w = p.width, h = 200;
		if (getSize().width > w) w = getSize().width;
		if (w < 700) w = 700;
		int fs = 34, fsObserver = 20;
		if (w < 750) {
			fs -= 2;
			fsObserver -= 4;
		}
		if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH && fs > 35) fs = 35;
		String prefix = "@SIZE"+fs, clockend = "}", clockStartWithoutBlur = "@CLOCK{", clockStart = //"@BOLD"+
				clockStartWithoutBlur; // Blur disabled
		Color radec = Color.YELLOW, azel = Color.CYAN, datetime = Color.WHITE;
		String J2000 = "";
/*		if (!telescope.getTelescopeModel().isCorrectedForRefraction()) {
			J2000 = ", "+Translate.translate(1178);
			if (!telescope.getTelescopeModel().isJ2000()) J2000 = "@SIZE"+(fs-24)+" _{("+J2000.substring(2)+")}"+prefix;
		}
*/
		if (telescope.getTelescopeModel().isJ2000()) J2000 = "@SIZE"+(fs-24)+" _{(J2000"+J2000+")}"+prefix;
		String ral = Translate.translate(1136)+J2000+"@SPACE@SPACE@SPACE@SPACE", decl = Translate.translate(1137)+J2000+"@SPACE@SPACE",
				azl = Translate.translate(1148)+"@SPACE@SPACE", ell = Translate.translate(1149)+"@SPACE@SPACE@SPACE",
				lonl = Translate.translate(1150)+"@SPACE@SPACE", latl = Translate.translate(1151)+"@SPACE@SPACE@SPACE",
				tzl = Translate.translate(1152)+"@SPACE@SPACE", dstl = Translate.translate(1153)+"@SPACE@SPACE", name = "";


		String unknownRA = "--h --m --.-s", unknownDEC = "---\u00b0 --' --\"";
		String ra = unknownRA, dec = unknownDEC, az = unknownDEC, el = unknownDEC;
		if (isConnected) {
			ra =  Functions.formatRA(locEq.getLongitude(), 1);
			dec =  Functions.formatDEC(locEq.getLatitude(), 0);
			az =  Functions.formatDEC(locHz.getLongitude(), 1);
			el =  " "+Functions.formatDEC(locHz.getLatitude(), 1);
		}
		String date = time.astroDate.toStringDate(false); //"@SIZE40@clock{12:78:00tdb}";
		String hour = Functions.fmt(time.astroDate.getHour(), 2, ':')+Functions.fmt(time.astroDate.getMinute(), 2, ':')+Functions.fmt((int)time.astroDate.getSeconds(), 2);
		String lon =  Functions.formatDEC(obs.getLongitudeRad(), 0);
		String lat =  Functions.formatDEC(obs.getLatitudeRad(), 0);
		String tz = Functions.formatValue(obs.getTimeZone(), 1);
		double JD_UT = TimeScale.getJD(time, obs, eph, SCALE.UNIVERSAL_TIME_UTC);
		String dst = Functions.formatValue(TimeScale.getDST(JD_UT, obs), 0);


		date = prefix + date;
		hour = "@SIZE" + (fs+2) + clockStartWithoutBlur + hour + time.getTimeScaleAbbreviation() + clockend;
		ra = prefix + ral + clockStart + ra + clockend;
		az = prefix + azl + clockStart + az + clockend;
		dec = prefix + decl + clockStart + dec + clockend;
		el = prefix + ell + clockStart + el + clockend;

		prefix = "@SIZE"+fsObserver;
		name = "@SIZE" + (fsObserver+2) + "@BOLD" + tname;
		lon = prefix + lonl + clockStartWithoutBlur + lon + clockend;
		lat = prefix + latl + clockStartWithoutBlur + lat + clockend;
		tz = prefix + tzl + clockStartWithoutBlur + tz + "h" + clockend;
		dst = prefix + dstl + clockStartWithoutBlur + dst + "h" + clockend;

		jparsec.graph.chartRendering.Graphics g = new AWTGraphics(w, h, false, false);
		TextLabel.setDigitalClockOutColor(new Color(128, 128, 128, 40));
		g.setColor(Color.BLACK.getRGB(), false);
		g.fillRect(0, 0, w,  h);

		g.setColor(datetime.getRGB(), true);
		g.setFont(FONT.getDerivedFont(g.getFont(), fs));
		int x = 20, y = fs, gapy = fs + 10, radecY = 145, gapyObserver = fsObserver + 10;
		int xm0 = w/2 + x, xm = xm0 + fs;

		//int dw = (int) g.getStringWidth(date);
		g.drawString(hour, w-180, y+2);
		//g.drawString(date, w-dw-10, y += gapy+10);
		g.drawString(date, xm, y += gapy+10);

		y = radecY;
		g.setColor(radec.getRGB(), true);
		g.drawString(ra, x, y);
		g.setColor(azel.getRGB(), true);
		g.drawString(az, xm, y);
		g.setColor(radec.getRGB(), true);
		g.drawString(dec, x, y += gapy-2);
		g.setColor(azel.getRGB(), true);
		g.drawString(el, xm, y);

		y = 5;
		gapy += 3;
		g.setColor(255, 255, 255, 255);
		g.setStroke(JPARSECStroke.STROKE_DEFAULT_LINE_THICK);
		g.drawLine(xm0, gapy, xm0, h-fs/4, false);
		g.drawString(name, x, y += gapyObserver);
		y += 10;
		g.drawString(lon, x, y += gapyObserver);
		g.drawString(tz, x + (xm0-fs)/2, y);
		g.drawString(lat, x, y += gapyObserver-3);
		g.drawString(dst, x + (xm0-fs)/2, y);

		y = 8;
		int gapx = 45;
		x = xm0 + 6 - (gapx * 5)/2;
		if (dome != null) x -= gapx/2;
		if (weather != null) x-= gapx/2;
		g.drawLine(x-gapx/2, 0, x-gapx/2, gapy, false);
		g.drawLine(xm0*2-(x-gapx/2), 0, xm0*2-(x-gapx/2), gapy, false);
		g.drawLine(xm0*2-(x-gapx/2), gapy, x-gapx/2, gapy, false);
		drawSymbol(g, SYMBOL.CONNECTED, isConnected, x, y);
		drawSymbol(g, SYMBOL.ALIGNED, isAligned, x+=gapx, y);
		drawSymbol(g, SYMBOL.EQ_MOUNT, isEqMount, x+=gapx, y);
		drawSymbol(g, SYMBOL.TRACKING, isTracking, x+=gapx, y);
		drawSymbol(g, SYMBOL.MOVING, isMoving, x+=gapx, y);
		if (dome != null) drawSymbol(g, SYMBOL.DOME, dome.isOpen(), x+=gapx, y);
		if (weather != null) drawSymbol(g, SYMBOL.WEATHER, !weather.isRaining(), x+=gapx, y);

		Picture pic = new Picture((BufferedImage) g.getRendering());
		//pic.makeTransparent(0, Color.BLACK);

		setCameraShooting(-1, false);
		return pic.getImage();
	}

	/** The set of icons to draw. */
	private enum SYMBOL {
		/** The different icons to be drawn. */
		MOVING, TRACKING, ALIGNED, CONNECTED, EQ_MOUNT, DOME, WEATHER
	}
	private void drawSymbol(jparsec.graph.chartRendering.Graphics g, SYMBOL symbol, boolean active, int x, int y) {
		g.setColor(255, 0, 0, 128);
		if (active || symbol == SYMBOL.EQ_MOUNT) g.setColor(0, 255, 0, 255);
		int c = g.getColor();
		g.setStroke(JPARSECStroke.STROKE_DEFAULT_LINE_THICK);

		switch (symbol) {
		case CONNECTED:
			y += 6;
			g.drawOval(x, y, 30, 24, false);
			g.setColor(0, 0, 0, 255);
			g.fillRect(x + 10, y-10, 9, 15);
			g.setColor(c, true);
			g.drawLine(x + 14, y - 8, x + 14, y + 10, false);
			break;
		case ALIGNED:
			g.drawRect(x, y, 30, 30);
			g.setFont(jparsec.graph.chartRendering.Graphics.FONT.getDerivedFont(g.getFont(), 14, 1));
			String ss = "P";
			g.drawString(ss, x + 15 - g.getStringWidth(ss)/2, y + 15 + g.getFont().getSize() / 2 - 3);
			break;
		case EQ_MOUNT:
			g.setFont(jparsec.graph.chartRendering.Graphics.FONT.getDerivedFont(g.getFont(), 20, 1));
			ss = active ? "EQ" : "AZ";
			g.drawString(ss, x+15-g.getStringWidth(ss)/2, y+15);
			g.drawLine(x+15, y+18, x+15, y+30, false);
			g.drawLine(x, y+30, x+30, y+30, false);
			break;
		case TRACKING:
			g.drawOval(x, y, 30, 30, false);
			g.fillOval(x+11, y+11, 7, 7, false);
			int s = 5;
			g.setStroke(JPARSECStroke.STROKE_DEFAULT_LINE);
			g.drawLine(x+14, y-s, x+14, y+s, false);
			g.drawLine(x+14, y+30-s, x+14, y+29+s, false);
			g.drawLine(x+30-s, y+14, x+29+s, y+14, false);
			g.drawLine(x-s, y+14, x+s, y+14, false);
			break;
		case MOVING:
			g.drawOval(x, y, 30, 30, false);
			int n = 6;
			g.setStroke(JPARSECStroke.STROKE_DEFAULT_LINE);
			for (int i=1; i<=n; i++) {
				g.drawLine(x-i, y+10+i, x+i, y+10+i, false);
			}
			break;
		case DOME:
			c = g.getColor();
			g.fillOval(x, y, 30, 30, false);
			g.setColor(0, 0, 0, 255);
			g.fillRect(x, y+16, 30, 15);
			g.fillRect(x+15-3, y, 6, 12);
			g.setColor(c, true);
			g.fillRect(x, y+17, 30, 15);
			break;
		case WEATHER:
			int t = (int) (0.5 + weather.getTemperature()), h = (int) (0.5 + weather.getHumidity());
			if (weather != null && obs != null) {
				try {
					obs.setHumidity(h);
					obs.setTemperature(t);
					obs.setPressure((int)weather.getPressure());
				} catch (Exception exc) {}
			}

			int size = 9;
			g.setFont(jparsec.graph.chartRendering.Graphics.FONT.getDerivedFont(g.getFont(), size, 1));
			g.drawString(""+t+"\u00b0", x, y+size);
			g.drawString(""+h+"%", x+20, y+size);
			WEATHER_FORECAST w = WEATHER_FORECAST.SUNNY;
			
			WEATHER_FORECAST wf[] = weather.getForecastInFollowingDays();
			if (wf != null && wf.length > 0) w = weather.getForecastInFollowingDays()[0];
			
			g.setStroke(JPARSECStroke.STROKE_DEFAULT_LINE);
			switch (w) {
			case SUNNY:
				g.fillOval(x + 10, y + 15, 10, 10, false);
				g.drawLine(x+15, y+13, x+15, y+10, false);
				g.drawLine(x+15, y+27, x+15, y+30, false);
				g.drawLine(x+8, y+20, x+5, y+20, false);
				g.drawLine(x+22, y+20, x+25, y+20, false);
				break;
			case SOME_CLOUDS:
				g.fillOval(x + 10, y + 15, 10, 10, false);
				g.drawLine(x+15, y+13, x+15, y+10, false);
				g.drawLine(x+8, y+20, x+5, y+20, false);
				g.drawLine(x+22, y+20, x+25, y+20, false);
				g.fillOval(x, y + 22, 30, 8, false);
				break;
			case CLOUDY:
				g.fillOval(x, y + 15, 30, 15, false);
				break;
			case RAINY:
				g.fillOval(x, y + 10, 30, 15, false);
				g.fillOval(x, y + 27, 1, 1, false);
				g.fillOval(x + 10, y + 27, 1, 1, false);
				g.fillOval(x + 20, y + 27, 1, 1, false);
				g.fillOval(x + 30, y + 27, 1, 1, false);
				g.fillOval(x+5, y + 30, 1, 1, false);
				g.fillOval(x+5 + 10, y + 30, 1, 1, false);
				g.fillOval(x+5 + 20, y + 30, 1, 1, false);
				break;
			case SNOWY:
				g.fillOval(x, y + 10, 30, 15, false);
				g.fillOval(x, y + 27, 4, 4, false);
				g.fillOval(x + 10, y + 27, 4, 4, false);
				g.fillOval(x + 20, y + 27, 4, 4, false);
				g.fillOval(x + 30, y + 27, 4, 4, false);
				g.fillOval(x+5, y + 30, 4, 4, false);
				g.fillOval(x+5 + 10, y + 30, 4, 4, false);
				g.fillOval(x+5 + 20, y + 30, 4, 4, false);
				break;
			}
			break;
		}
	}

	private SkyChart getSkyChart() {
		Dimension p = getPreferredSize();

		int w = p.width;
		if (w < 700) w = 700;
		int h = (w * 3) / 4;
		PlanetRenderElement render = new PlanetRenderElement(false, true, true, true, false);
		TelescopeElement telescope = TelescopeElement.HUMAN_EYE;

		SkyRenderElement sky = new SkyRenderElement(jparsec.astronomy.CoordinateSystem.COORDINATE_SYSTEM.EQUATORIAL,
				PROJECTION.STEREOGRAPHICAL, 0, 0.0, w, h, render, telescope);

		sky.setColorMode(SkyRenderElement.COLOR_MODE.BLACK_BACKGROUND); //.WHITE_BACKGROUND_SIMPLE_GREEN_RED_OR_RED_CYAN_ANAGLYPH);

		sky.drawConstellationNamesType = Constellation.CONSTELLATION_NAME.SPANISH;
		sky.drawObjectsLimitingMagnitude = 12.5f;
		sky.drawPlanetsMoonSun = true;
		sky.drawSpaceProbes = false;
		sky.drawStarsLabels = SkyRenderElement.STAR_LABELS.ONLY_PROPER_NAME_SPANISH;
		sky.drawStarsGreekSymbols = true;
		sky.drawStarsGreekSymbolsOnlyIfHasProperName = false;
		sky.drawTransNeptunianObjects = false;
		sky.drawStarsLimitingMagnitude = 16f;
		sky.drawStarsLabelsLimitingMagnitude = sky.drawStarsLimitingMagnitude-2;
		sky.drawArtificialSatellites = false;
		sky.drawAsteroids = false;
		sky.drawComets = false;
		sky.drawStarsSymbols = true;

		sky.drawConstellationLimits = true;
		sky.drawDeepSkyObjects = true;
		sky.drawSkyCorrectingLocalHorizon = true;
		sky.drawSkyBelowHorizon = false;
		sky.drawFastLabels = SUPERIMPOSED_LABELS.AVOID_SUPERIMPOSING_SOFT;
		sky.drawFastLabelsInWideFields = false;
		sky.fillMilkyWay = false;
		sky.drawSuperNovaAndNovaEvents = true;
		sky.drawMilkyWayContoursWithTextures = MILKY_WAY_TEXTURE.NO_TEXTURE;

		sky.drawClever = true;
		sky.drawStarsPositionAngleInDoubles = true;

		sky.drawFastLinesMode = FAST_LINES.GRID_AND_MILKY_WAY_AND_CONSTELLATIONS;
		sky.drawFastLinesMode.setFastOvals(true);
		sky.drawConstellationContoursMarginBetweenLineAndStar = 30;
		RenderPlanet.ALLOW_SPLINE_RESIZING = false;

		sky.drawStarsLimitingMagnitude = 7.5f;
		sky.drawObjectsLimitingMagnitude = 9f;
		sky.drawConstellationLimits = false;
		sky.planetRender.textures = true;
		sky.drawFaintStars = true;
		sky.drawStarsRealistic = REALISTIC_STARS.STARRED;
		sky.drawLeyend = LEYEND_POSITION.TOP;
		sky.drawDeepSkyObjectsAllMessierAndCaldwell = true;
		LocationElement loc = this.telescope.getApparentEquatorialPosition();
		sky.centralLongitude = loc.getLongitude();
		sky.centralLatitude = loc.getLatitude();

		try
		{
			SkyRendering skyRender = new SkyRendering(time, obs, eph, sky, "Sky render", 0);
			sc = new SkyChart(w, h, skyRender, true, false, updateTime/1000, false);

			sc.addTelescopeMark(tname, this.telescope);
			sc.setRealTimeUpdate(true);
			return sc;
		} catch (Exception exc) {
			return null;
		}
	}

	public void addTelescopeMark(SkyChart sc) {
		try {
			String marks[] = sc.getTelescopes();
			int index = DataSet.getIndex(marks, tname);
			if (index >= 0) return;
			sc.addTelescopeMark(//"@BOLD@RED"+
					tname, this.telescope);
		} catch (Exception exc) {}		
	}
	
	private void setCameraShooting(int cameraIndex, boolean shooting) {
		if (cameraShooting == null) return;
		if (cameraIndex == -1) {
			for (int i=0; i<cameraShooting.length; i++) {
				if (!camera[i].isShooting() && cameraShooting[i]) {
					shooting = false;
					cameraIndex = i;
					break;
				}
			}
			if (cameraIndex == -1) return;
		}
		camera[cameraIndex].setMinimumIntervalBetweenShots(obsManager.getCameraMinimumIntervalBetweenShots(cameraIndex));
		if (shooting) {
			cameraShooting[cameraIndex] = true;
			iso[cameraIndex].setEnabled(false);
			imgID[cameraIndex].setEnabled(false);
			resolution[cameraIndex].setEnabled(false);
			shutter[cameraIndex].setEnabled(false);
			filter[cameraIndex].setEnabled(false);
			aperture[cameraIndex].setEnabled(false);
			shotButton[cameraIndex].setEnabled(false);
			shotButtonMultiple[cameraIndex].setEnabled(false);
			return;
		}

		String status = cameraStatus[cameraIndex];
		int n = Integer.parseInt(FileIO.getField(1, status, " ", true));
		n --;
		status = ""+n+" "+FileIO.getRestAfterField(1, status, " ", true);
		boolean b2 = false;
		if (n == 0) {
			boolean b1 = Boolean.parseBoolean(FileIO.getField(2, status, " ", true));
			b2 = Boolean.parseBoolean(FileIO.getField(3, status, " ", true));
			status = null;
			obsManager.setReductionEnabled(b1);
			//obsManager.setAutoReduceOnFramesEnabled(true); // Ons may need to be stacked/sligned first

			cameraShooting[cameraIndex] = false;
			iso[cameraIndex].setEnabled(true);
			imgID[cameraIndex].setEnabled(true);
			resolution[cameraIndex].setEnabled(true);
			shutter[cameraIndex].setEnabled(true);
			filter[cameraIndex].setEnabled(true);
			aperture[cameraIndex].setEnabled(true);
			shotButton[cameraIndex].setEnabled(true);
			shotButtonMultiple[cameraIndex].setEnabled(true);
		}

		String path = camera[cameraIndex].getPathOfLastDownloadedImage();
		cameraStatus[cameraIndex] = status;

		if (path != null) {
			String p[] = DataSet.toStringArray(path, ",");
			if (obsManager.reductionPossible()) {
				try {
					HeaderElement header[] = telescope.getFitsHeader(cameraIndex);
					if (dome != null) header = HeaderElement.addHeaderEntry(header, dome.getFitsHeader());
					if (weather != null) header = HeaderElement.addHeaderEntry(header, weather.getFitsHeader());
					String project[] = obsManager.getProjectInfo();
					String inst = obsManager.getTelescopeInstitute();

					if (project[0] != null) header = HeaderElement.addHeaderEntry(header, new HeaderElement("PROJECT", project[0], "Project name"));
					if (project[1] != null) header = HeaderElement.addHeaderEntry(header, new HeaderElement("OBSERVER", project[1], "Observer name"));
					if (project[2] != null) {
						String des[] = DataSet.toStringArray(project[2], 20);
						for (int i=0; i<des.length; i++) {
							header = HeaderElement.addHeaderEntry(header, new HeaderElement[] {new HeaderElement("DESCRIP"+(i+1), des[i], "Project description ("+(i+1)+"/"+des.length+")")});
						}
					}
					if (inst != null) header = HeaderElement.addHeaderEntry(header, new HeaderElement("ORIGIN", inst, "Institute responsible for the telescope"));
					header = HeaderElement.addHeaderEntry(header, new HeaderElement("DATE", ""+(new TimeElement()).toString(), "fits file creation date and time"));
					header = HeaderElement.addHeaderEntry(header, new HeaderElement("CAMPOSER", ""+obsManager.getCameraPositionError(cameraIndex), "Camera position error respect telescope (radians)"));

					obsManager.offerFrame(camera[cameraIndex].getImageID(), p, header, cameraIndex);
				} catch (JPARSECException exc) {}
			} else {
				for (int i=0; i<p.length; i++) {
					if (p[i].endsWith(".jpg") || p[i].endsWith(".png")) {
						try {
							Picture pic = new Picture(p[i]);
							pic.show(800, 600, p[i], true,	true, true);
						} catch (Exception exc) {}
					}
				}
			}
		}

		if (n == 0) obsManager.setAutoReduceOnFramesEnabled(b2);

		if (obsManager.reductionPossible() && n > 0) {
			if (dome != null) {
				LocationElement telHz = telescope.getHorizontalPosition();
				if (!dome.isSync(telHz)) {
					if (!dome.isMoving()) dome.sync(telHz);
					System.out.println("Waiting for dome synchronization ... ");
					long t0 = System.currentTimeMillis();
					long t1 = t0;
					try {
						do {
							Thread.sleep(1000 * (1 + (int) dome.getSyncTime() / 10));
							t1 = System.currentTimeMillis();
						} while (!dome.isSync(telHz) && (t1-t0) < 600000);
					} catch (Exception exc) {}
					if ((t1-t0) >= 600000)
						System.out.println("ERROR: Could not synchronize the dome with azimuth "+Functions.formatAngleAsDegrees(telHz.getLongitude(), 3));
				}
			}

			camera[cameraIndex].shotAndDownload(false);
		}
	}

	/**
	 * Executes a set of commands defined in a very simple language described below.
	 * The commands are checked before starting execution, so in case of error an
	 * exception is returned and no execution is done. Execution is done in a new thread.<P>
	 * The commands are defined by an uppercase keyword defining the command and
	 * a value for that parameter:
	 * <pre>
	 * KEYWORKD      POSSIBLE VALUES
	 * -----------------------------
	 *
	 * PROJECT       a project name
	 * OBSERVER      observer name
	 * DESCRIPTION   description of the project
	 * TELESCOPE     SC,Newton,refractor,terrestrial. Selects the telescope type, terrestrial does not invert the image
	 * DARKDIR       directory name to hold darks of the selected camera
	 * FLATDIR       directory name to hold flats of the selected camera
	 * ONDIR         directory name to hold on source frames of the selected camera
	 * REDUCEDDIR    directory name to hold reduced frames of the selected camera
	 * STACKEDDIR    directory name to hold stacked frames of the selected camera
	 * MAXHUM        maximum humidity allowed (before a weather alarm) from 0 to 100. Default is 80
	 * MAXWIND       maximum wind speed in km/s allowed before a weather alarm. Default is 100
	 * MAXTEMP       maximum temperature in C allowed before a weather alarm. Default is 50
	 * MINTEMP       minimum temperature in C allowed before a weather alarm. Default is -20
	 * CAMERA        from 1 to the number of cameras (selects a camera). Default value is 1
	 * SHIFT         value in degrees representing the position shift between the telescope and the selected camera. 0 for primary focus, &gt;0 for piggy back camera
	 * GOTO          object name (internal databases in JPARSEC or a valid Simbad identifier). Offset from current position is also allowed
	 * PARK          Nothing. Parks the telescope.
	 * UNPARK        Nothing. Unparks the telescope.
	 * ISO           an ISO value valid for the camera
	 * TIME          a TIME value valid for the camera (except 'bulb') or an integer (exposition time in seconds)
	 * IMAGE         Dark, Flat, On, Test
	 * FILTER        the name of the filter used for the image.
	 * APERTURE      a valid APERTURE value for the camera
	 * RESOLUTION    a valid RESOLUTION value for the camera
	 * SHOT          any integer number representing the number of consecutive shots to take, between 1 and 100
	 * ORIENTATION   a value in degrees indicating the orientation of the camera respect north (eq mount) or cenit (hz mount). Positive value for clockwise rotation
	 * FIELD         a value in degrees indicating the field of view of the camera
	 * COLDTIME      an integer value in seconds representing the time needed for the camera to get cold (minimum time between shots)
	 * AUTOREDUCE    on/off, or yes/no, to switch on or off the autoreduce process after taking n shots
	 * CONNECT       (nothing) connects to the telescope and opens the dome
	 * DISCONNECT    (nothing) disconnects from the telescope and closes the dome
	 * COMBINE       Median,Average,Maximum,Kappa. Selects the combine method for combining multiple frames
	 * INTERPOLATION Nearest,Bilinear,Bicubic. Selects the interpolation method when resampling images for stacking.
	 * STACK         Nearest,Ponderation. Selects the stack method after images are resampled. Ponderation considers al points and their distances for the average, Nearest takes only the closest to the image position.
	 * DRIZZLE       1,2,3,0.5. Drizzle method. 2 and 3 produces an output image greater than the original one (1), 0.5 reduces the size of the output image.
	 * INVERSION     HV,H,N. Select the axes inverted in the output processed image. Default value is N (no inversion), produces images with North (zenith) upwards and East towards left.
	 * SEXTRACTOR    four numerical values should be given: minimum number of pixels for detection, sigma (intensity ratio respect background for detection), object type (0 are stars, 1 are extended objects, so 0 will return all and 0.5 only star-like objects), and maximum number of sources for astrometric/photometric calibration (0 to use all). Default values are 3 5 0.5 50
	 * </pre>
	 * A log string is created if everything is fine, otherwise an error is launched. In case the
	 * log is generated an error could still happen, but it is unlikely. One such possibility
	 * is a weather alarm.
	 * @param command The set of commands to execute.
	 * @throws JPARSECException If an error occurs. The error will contain useful info about the problem.
	 * For instance, if you set an invalid time value the error will contain all possible correct values
	 * for this keyword.
	 */
	public void executeCommand(final String command[]) throws JPARSECException {
		if (lastLog == null) throw new JPARSECException("A previous command is still running");

		String error = checkCommand(command);
		if (error != null) throw new JPARSECException(error);
		Thread script = new Thread(new Runnable() {
			@Override
			public void run() {
				lastLog = "";
				try {
					lastLog = execute(command);
				} catch (Exception exc) {}
			}
		});
		script.start();
	}
	private String lastLog = "";
	/**
	 * Return the log of the last executed command. In case the script is still
	 * in execution, null is returned.
	 * @return The last log.
	 */
	public String executedCommandLog() {
		return lastLog;
	}

	/**
	 * Checks a set of commands.
	 * @param command The commands.
	 * @return A null string if they are correct, otherwise an error message.
	 */
	public String checkCommand(String command[]) {
		if (command == null || command.length == 0) return "Command cannot be null or empty";

		String error = null;
		int i = -1, cameraIndex = 0;
		try {
			for (i=0; i<command.length; i++) {
				if (command[i].startsWith("PROJECT ")) {
					continue;
				}
				if (command[i].startsWith("OBSERVER ")) {
					continue;
				}
				if (command[i].startsWith("DESCRIPTION ")) {
					continue;
				}
				if (command[i].startsWith("SHIFT ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					if (!DataSet.isDoubleFastCheck(val)) {
						error = "SHIFT "+val+" is not a valid SHIFT value";
						break;
					}
					continue;
				}
				if (command[i].startsWith("DARKDIR ")) {
					continue;
				}
				if (command[i].startsWith("FLATDIR ")) {
					continue;
				}
				if (command[i].startsWith("ONDIR ")) {
					continue;
				}
				if (command[i].startsWith("REDUCEDDIR ")) {
					continue;
				}
				if (command[i].startsWith("STACKEDDIR ")) {
					continue;
				}
				if (command[i].startsWith("MAXHUM ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					if (!DataSet.isDoubleFastCheck(val)) {
						error = "MAXHUM "+val+" is not a valid MAXHUM value";
						break;
					}
					continue;
				}
				if (command[i].startsWith("MAXWIND ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					if (!DataSet.isDoubleFastCheck(val)) {
						error = "MAXWIND "+val+" is not a valid MAXWIND value";
						break;
					}
					continue;
				}
				if (command[i].startsWith("MAXTEMP ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					if (!DataSet.isDoubleFastCheck(val)) {
						error = "MAXTEMP "+val+" is not a valid MAXTEMP value";
						break;
					}
					continue;
				}
				if (command[i].startsWith("MINTEMP ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					if (!DataSet.isDoubleFastCheck(val)) {
						error = "MINTEMP "+val+" is not a valid MINTEMP value";
						break;
					}
					continue;
				}
				if (command[i].startsWith("SEXTRACTOR ")) {
					int n = FileIO.getNumberOfFields(command[i], " ", true);
					if (n != 5) {
						error = "This command requires 4 numerical values";
						break;
					} else {
						for (int ii=2; ii<= 5; ii++) {
							String s = FileIO.getField(ii, command[i], " ", true);
							if (!DataSet.isDoubleFastCheck(s)) {
								error = "This command requires 4 numerical values";
								break;
							}
						}
					}
					continue;
				}

				if (command[i].startsWith("CAMERA ")) {
					int n = Integer.parseInt(FileIO.getRestAfterField(1, command[i], " ", true).trim());
					if (n < 1 || n > camera.length) {
						error = "Camera index "+n+" out of range 1-"+camera.length;
						break;
					}
					cameraIndex = n - 1;
					continue;
				}
				if (command[i].startsWith("GOTO ")) {
					String obj = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					String err = validateObject(obj);
					if (err != null && !err.equals("")) {
						error = "Cannot identify object +obj";
						break;
					}
					continue;
				}
				if (command[i].startsWith("PARK")) {
					String obj = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					if (obj != null && !obj.equals("")) {
						error = "This command should have no parameters";
						break;
					}
					continue;
				}
				if (command[i].startsWith("UNPARK")) {
					String obj = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					if (obj != null && !obj.equals("")) {
						error = "This command should have no parameters";
						break;
					}
					continue;
				}
				if (command[i].startsWith("ISO ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					String isos[] = camera[cameraIndex].getPossibleISOs();
					int index = DataSet.getIndex(isos, val);
					if (index < 0) {
						error = "ISO "+val+" is not a valid ISO value for camera #"+(cameraIndex+1)+". Correct values are: "+DataSet.toString(isos, ",");
						break;
					}
					continue;
				}
				if (command[i].startsWith("TIME ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					String times[] = camera[cameraIndex].getPossibleExpositionTimes();
					int index = DataSet.getIndex(times, val);
					if (index < 0) {
						try {
							Integer.parseInt(val);
							continue;
						} catch (Exception exc2) {
							error = "TIME "+val+" is not a valid TIME value for camera #"+(cameraIndex+1)+". Correct values are (besides any given integer number of seconds): "+DataSet.toString(times, ",");
							break;
						}
					}
					if (!DataSet.isDoubleFastCheck(times[index])) {
						error = "TIME "+val+" is not valid, must be number.";
						break;
					}
					continue;
				}
				if (command[i].startsWith("IMAGE ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					int index = DataSet.getIndex(GenericCamera.IMAGE_IDS, val);
					if (index < 0) {
						error = "IMAGE "+val+" is not a valid IMAGE value. Correct values are: "+DataSet.toString(GenericCamera.IMAGE_IDS, ",");
						break;
					}
					continue;
				}
				if (command[i].startsWith("FILTER ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					int index = DataSet.getIndex(GenericCamera.FILTER.getFilterNames(), val);
					if (index < 0) {
						error = "FILTER "+val+" is not a valid FILTER value. Correct values are: "+DataSet.toString(GenericCamera.FILTER.getFilterNames(), ",");
						break;
					}
					continue;
				}
				if (command[i].startsWith("APERTURE ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					String aper[] = camera[cameraIndex].getPossibleApertures();
					int index = DataSet.getIndex(aper, val);
					if (index < 0) {
						error = "APERTURE "+val+" is not a valid APERTURE value for camera #"+(cameraIndex+1)+". Correct values are: "+DataSet.toString(aper, ",");
						break;
					}
					continue;
				}
				if (command[i].startsWith("RESOLUTION ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					String res[] = camera[cameraIndex].getPossibleResolutionModes();
					int index = DataSet.getIndex(res, val);
					if (index < 0) {
						error = "RESOLUTION "+val+" is not a valid RESOLUTION value for camera #"+(cameraIndex+1)+". Correct values are: "+DataSet.toString(res, ",");
						break;
					}
					continue;
				}
				if (command[i].startsWith("ORIENTATION ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					if (!DataSet.isDoubleFastCheck(val)) {
						error = "ORIENTATION "+val+" is not a valid ORIENTATION value";
						break;
					}
					continue;
				}
				if (command[i].startsWith("FIELD ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					if (!DataSet.isDoubleFastCheck(val)) {
						error = "FIELD "+val+" is not a valid FIELD value";
						break;
					}
					continue;
				}
				if (command[i].startsWith("TELESCOPE ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					String tels[] = new String[] {"SC", "Newton", "refractor", "terrestrial"};
					int index = DataSet.getIndex(tels, val);
					if (index < 0) {
						error = "TELESCOPE "+val+" is not a valid TELESCOPE value. Correct values are: "+DataSet.toString(tels, ",");
						break;
					}
					continue;
				}
				if (command[i].startsWith("INVERSION ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					String tels[] = new String[] {"HV", "H", "N"};
					int index = DataSet.getIndex(tels, val);
					if (index < 0) {
						error = "INVERSION "+val+" is not a valid INVERSION value. Correct values are: "+DataSet.toString(tels, ",");
						break;
					}
					continue;
				}
				if (command[i].startsWith("COLDTIME ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					if (!DataSet.isDoubleFastCheck(val) || val.indexOf(".") >= 0 || val.toLowerCase().indexOf("e") >= 0) {
						error = "COLD TIME "+val+" is not a valid integer value";
						break;
					}
					continue;
				}
				if (command[i].startsWith("SHOT ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					try {
						int n = Integer.parseInt(val);
						if (n < 1 || n > 100) {
							error = "The number of shots must be between 1 and 100";
							break;
						}
					} catch (Exception exc2) {
						error = "You must provide a valid integer value representing the number of shots to take";
						break;
					}
					continue;
				}
				if (command[i].startsWith("AUTOREDUCE ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					String values[] = new String[] {"yes", "no", "on", "off"};
					int index = DataSet.getIndex(values, val);
					if (index < 0) {
						error = "Value "+val+" is invalid. Must be yes/no, or on/off";
						break;
					}
					continue;
				}
				if (command[i].startsWith("COMBINE ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					String values[] = new String[] {"Median", "Average", "Maximum", "Kappa"};
					int index = DataSet.getIndex(values, val);
					if (index < 0) {
						error = "Value "+val+" is invalid. Must be "+DataSet.toString(values, "/");
						break;
					}
					continue;
				}
				if (command[i].startsWith("INTERPOLATION ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					String values[] = new String[] {"Nearest", "Bilinear", "Bicubic"};
					int index = DataSet.getIndex(values, val);
					if (index < 0) {
						error = "Value "+val+" is invalid. Must be "+DataSet.toString(values, "/");
						break;
					}
					continue;
				}
				if (command[i].startsWith("STACK ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					String values[] = new String[] {"Nearest", "Ponderation"};
					int index = DataSet.getIndex(values, val);
					if (index < 0) {
						error = "Value "+val+" is invalid. Must be "+DataSet.toString(values, "/");
						break;
					}
					continue;
				}
				if (command[i].startsWith("DRIZZLE ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					String values[] = new String[] {"1", "2", "3", "0.5"};
					int index = DataSet.getIndex(values, val);
					if (index < 0) {
						error = "Value "+val+" is invalid. Must be "+DataSet.toString(values, "/");
						break;
					}
					continue;
				}
				if (command[i].equals("CONNECT") || command[i].equals("DISCONNECT")) continue;

				error = "Cannot recognize command "+command[i];
				break;
			}
		} catch (Exception exc) {
			if (error == null || error.equals("")) {
				if (i >= 0) return "Could not parse command "+command[i];
				return DataSet.toString(JPARSECException.toStringArray(exc.getStackTrace()), FileIO.getLineSeparator());
			}
		}
		return error;
	}

	private static final String BAD_WEATHER_STOP_MESSAGE = "STOPPING DUE TO BAD WEATHER CONDITIONS ...";
	private String execute(String command[]) {
		StringBuffer log = new StringBuffer("");
		String sep = FileIO.getLineSeparator();
		int cameraIndex = 0;

		if (obsManager.getCombineMethod() == null && obsManager.reductionEnabled())
			obsManager.setCombineMethod(COMBINATION_METHOD.MEDIAN);

		try {
			for (int i=0; i<command.length; i++) {
				log.append(time.toString()+": executing "+command[i]+" ... ");
				if (!checkWeatherConditions()) {
					if (dome != null) dome.close();
					if (telescope != null) telescope.disconnect();
					log.append(sep + BAD_WEATHER_STOP_MESSAGE);
					break;
				}

				if (command[i].startsWith("PROJECT ")) {
					String pr = FileIO.getRestAfterField(1, command[i], " ", true);
					String prInfo[] = obsManager.getProjectInfo();
					obsManager.setProjectInfo(pr, prInfo[1], prInfo[2]);
					log.append("OK"+sep);
					continue;
				}
				if (command[i].startsWith("OBSERVER ")) {
					String pr = FileIO.getRestAfterField(1, command[i], " ", true);
					String prInfo[] = obsManager.getProjectInfo();
					obsManager.setProjectInfo(prInfo[0], pr, prInfo[2]);
					log.append("OK"+sep);
					continue;
				}
				if (command[i].startsWith("DESCRIPTION ")) {
					String pr = FileIO.getRestAfterField(1, command[i], " ", true);
					String prInfo[] = obsManager.getProjectInfo();
					obsManager.setProjectInfo(prInfo[0], prInfo[1], pr);
					log.append("OK"+sep);
					continue;
				}
				if (command[i].startsWith("DARKDIR ")) {
					String pr = FileIO.getRestAfterField(1, command[i], " ", true);
					obsManager.setDarkDir(cameraIndex, pr);
					log.append("OK"+sep);
					continue;
				}
				if (command[i].startsWith("FLATDIR ")) {
					String pr = FileIO.getRestAfterField(1, command[i], " ", true);
					obsManager.setFlatDir(cameraIndex, pr);
					log.append("OK"+sep);
					continue;
				}
				if (command[i].startsWith("ONDIR ")) {
					String pr = FileIO.getRestAfterField(1, command[i], " ", true);
					obsManager.setOnDir(cameraIndex, pr);
					log.append("OK"+sep);
					continue;
				}
				if (command[i].startsWith("REDUCEDDIR ")) {
					String pr = FileIO.getRestAfterField(1, command[i], " ", true);
					obsManager.setReducedDir(cameraIndex, pr);
					log.append("OK"+sep);
					continue;
				}
				if (command[i].startsWith("STACKEDDIR ")) {
					String pr = FileIO.getRestAfterField(1, command[i], " ", true);
					obsManager.setStackedDir(cameraIndex, pr);
					log.append("OK"+sep);
					continue;
				}
				if (command[i].startsWith("TELESCOPE ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					String tels[] = new String[] {"SC", "Newton", "refractor", "terrestrial"};
					int index = DataSet.getIndex(tels, val);
					if (index < 0) {
						log.append("ERROR! (Telescope "+val+" cannot be recognized)"+sep);
					} else {
						obsManager.setTelescopeType(TELESCOPE_TYPE.values()[index]);
					}
					log.append("OK"+sep);
					continue;
				}
				if (command[i].startsWith("INVERSION ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					String tels[] = new String[] {"HV", "H", "N"};
					int index = DataSet.getIndex(tels, val);
					if (index < 0) {
						log.append("ERROR! (Inversion "+val+" cannot be recognized)"+sep);
					} else {
						obsManager.setImageOrientation(IMAGE_ORIENTATION.values()[index]);
					}
					log.append("OK"+sep);
					continue;
				}
				if (command[i].startsWith("CAMERA ")) {
					int n = Integer.parseInt(FileIO.getField(2, command[i], " ", true));
					cameraIndex = n - 1;
					log.append("OK"+sep);
					continue;
				}
				if (command[i].startsWith("GOTO ")) {
					if (!telescope.isConnected() || telescope.isMoving()) {
						if (!telescope.isConnected()) {
							log.append("ERROR! The telescope is not connected"+sep);
						} else {
							log.append("ERROR! The telescope is moving"+sep);
						}
						continue;
					}
					String obj = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					String err = validateObject(obj);
					if (err == null) {
						boolean go = gotoObject();
						if (!go) {
							log.append("ERROR! The telescope rejected the goto command"+sep);
							continue;
						} else {
							try {
								Thread.sleep(5000);
								String endString = Translate.translate(1134);
								do {
									String s = gotoButton.getText();
									if (s.equals(endString)) break;
									Thread.sleep(5000);
								} while (true);
							} catch (Exception exc2) {
								log.append("ERROR! "+exc2.getMessage()+sep);
								continue;
							}
						}
					}
					log.append("OK"+sep);
					continue;
				}
				if (command[i].startsWith("PARK")) {
					if (telescope.isConnected()) {
						telescope.setParkPosition(obsManager.getTelescopeParkPosition());
						telescope.park();
						log.append("OK"+sep);
					} else {
						log.append("ERROR (Telescope not connected)"+sep);
					}
					continue;
				}
				if (command[i].startsWith("UNPARK")) {
					if (telescope.isConnected()) {
						log.append("ERROR (Telescope connected)"+sep);
					} else {
						telescope.unpark();
						log.append("OK"+sep);
					}
					continue;
				}
				if (command[i].startsWith("ISO ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					iso[cameraIndex].select(val);
					camera[cameraIndex].setISO(val);
					log.append("OK"+sep);
					continue;
				}
				if (command[i].startsWith("TIME ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					String times[] = camera[cameraIndex].getPossibleExpositionTimes();
					int index = DataSet.getIndex(times, val);
					if (index < 0) {
						try {
							int bulbIndex = DataSet.getIndexStartingWith(times, "b");
							camera[cameraIndex].setExpositionTime(times[bulbIndex]);
							int time = Integer.parseInt(val);
							camera[cameraIndex].setCCDorBulbModeTime(time);
							shutter[cameraIndex].select(times[bulbIndex]);
							bulbField[cameraIndex].setText(""+time);
						} catch (Exception exc2) {
							log.append("ERROR! "+exc2.getMessage()+sep);
						}
					} else {
						camera[cameraIndex].setExpositionTime(val);
					}
					log.append("OK"+sep);
					continue;
				}
				if (command[i].startsWith("IMAGE ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					int index = DataSet.getIndex(GenericCamera.IMAGE_IDS, val);
					camera[cameraIndex].setImageID(IMAGE_ID.values()[index]);
					imgID[cameraIndex].select(index);
					log.append("OK"+sep);
					continue;
				}
				if (command[i].startsWith("FILTER ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					int index = DataSet.getIndex(GenericCamera.FILTER.getFilterNames(), val);
					camera[cameraIndex].setFilter(FILTER.values()[index]);
					filter[cameraIndex].select(index);
					log.append("OK"+sep);
					continue;
				}
				if (command[i].startsWith("APERTURE ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					String aper[] = camera[cameraIndex].getPossibleApertures();
					int index = DataSet.getIndex(aper, val);
					camera[cameraIndex].setAperture(aper[index]);
					aperture[cameraIndex].select(index);
					log.append("OK"+sep);
					continue;
				}
				if (command[i].startsWith("RESOLUTION ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					String res[] = camera[cameraIndex].getPossibleResolutionModes();
					int index = DataSet.getIndex(res, val);
					camera[cameraIndex].setResolutionMode(res[index]);
					resolution[cameraIndex].select(index);
					log.append("OK"+sep);
					continue;
				}
				if (command[i].startsWith("ORIENTATION ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					camera[cameraIndex].setCameraOrientation(Double.parseDouble(val)*Constant.DEG_TO_RAD);
					orientationField[cameraIndex].setText(val);
					log.append("OK"+sep);
					continue;
				}
				if (command[i].startsWith("FIELD ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					telescope.setFieldOfView(Double.parseDouble(val)*Constant.DEG_TO_RAD, cameraIndex);
					fovField[cameraIndex].setText(val);
					log.append("OK"+sep);
					continue;
				}
				if (command[i].startsWith("COLDTIME ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					obsManager.setCameraMinimumIntervalBetweenShots(cameraIndex, Integer.parseInt(val));
					log.append("OK"+sep);
					continue;
				}
				if (command[i].startsWith("SHIFT ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					obsManager.setCameraPositionError(cameraIndex, Double.parseDouble(val) * Constant.DEG_TO_RAD);
					log.append("OK"+sep);
					continue;
				}
				if (command[i].startsWith("MAXHUM ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					double c[] = obsManager.getWeatherAlarmConditions();
					obsManager.setWeatherAlarmConditions(Double.parseDouble(val), c[1], c[2], c[3]);
					log.append("OK"+sep);
					continue;
				}
				if (command[i].startsWith("MAXWIND ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					double c[] = obsManager.getWeatherAlarmConditions();
					obsManager.setWeatherAlarmConditions(c[0], Double.parseDouble(val), c[2], c[3]);
					log.append("OK"+sep);
					continue;
				}
				if (command[i].startsWith("MAXTEMP ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					double c[] = obsManager.getWeatherAlarmConditions();
					obsManager.setWeatherAlarmConditions(c[0], c[1], Double.parseDouble(val), c[3]);
					log.append("OK"+sep);
					continue;
				}
				if (command[i].startsWith("MINTEMP ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					double c[] = obsManager.getWeatherAlarmConditions();
					obsManager.setWeatherAlarmConditions(c[0], c[1], c[2], Double.parseDouble(val));
					log.append("OK"+sep);
					continue;
				}
				if (command[i].startsWith("SEXTRACTOR ")) {
					int n = FileIO.getNumberOfFields(command[i], " ", true);
					double val[] = new double[4];
					if (n != 5) {
						log.append("ERROR ! This command requires 4 numerical values");
						continue;
					} else {
						for (int ii=2; ii<= 5; ii++) {
							String s = FileIO.getField(ii, command[i], " ", true);
							if (!DataSet.isDoubleFastCheck(s)) {
								log.append("ERROR ! This command requires 4 numerical values");
								continue;
							}
							val[ii-2] = Double.parseDouble(s);
						}
					}
					obsManager.setSExtractorValues((int) val[0], (int) val[1], val[2], (int) val[3]);
					log.append("OK"+sep);
					continue;
				}
				if (command[i].startsWith("SHOT ")) {
					if (dome != null && !dome.isOpen() && camera[cameraIndex].getImageID() == IMAGE_ID.ON_SOURCE) {
						log.append("ERROR! The dome is closed, you must call connect first "+sep);
					} else {
						if (camera[cameraIndex].isShooting() || ((camera[cameraIndex].getImageID() == IMAGE_ID.FLAT || camera[cameraIndex].getImageID() == IMAGE_ID.ON_SOURCE) && (telescope.isMoving() || !telescope.isTracking()))) {
							log.append("ERROR! Camera is busy or telescope not ready "+sep);
						} else {
							String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
							int n = Integer.parseInt(val);

							if (dome != null) {
								LocationElement telHz = telescope.getHorizontalPosition();
								if (!dome.isSync(telHz)) {
									if (!dome.isMoving()) dome.sync(telHz);
									log.append("WAITING FOR DOME SYNCHRONIZATION ... ");
									long t0 = System.currentTimeMillis();
									long t1 = t0;
									do {
										Thread.sleep(1000 * (1 + (int) dome.getSyncTime() / 10));
										t1 = System.currentTimeMillis();
									} while (!dome.isSync(telHz) && (t1-t0) < 600000);
									if ((t1-t0) >= 600000)
										System.out.println("ERROR: Could not synchronize the dome with azimuth "+Functions.formatAngleAsDegrees(telHz.getLongitude(), 3));
								}
							}

							boolean ok = camera[cameraIndex].shotAndDownload(false);
							if (ok) {
								setCameraShooting(cameraIndex, true);
								cameraStatus[cameraIndex] = ""+n+" "+obsManager.reductionEnabled() + " " + obsManager.getAutoReduceOnFramesEnabled();
								if (n > 1 && obsManager.reductionEnabled()
										&& !imgID[cameraIndex].getSelectedItem().equals(Translate.translate(GenericCamera.IMAGE_IDS[GenericCamera.IMAGE_ID.ON_SOURCE.ordinal()]))) obsManager.setReductionEnabled(false);
								try {
									do {
										if (cameraStatus[cameraIndex] == null) break;
										Thread.sleep(5000);
									} while (true);
								} catch (Exception exc2) {
									log.append("ERROR! "+exc2.getMessage()+sep);
									continue;
								}
								log.append("OK"+sep);
							} else {
								log.append("ERROR! Could not shot the camera"+sep);
							}
						}
					}
					continue;
				}
				if (command[i].startsWith("AUTOREDUCE ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					if (val.equals("yes") || val.equals("on")) obsManager.setReductionEnabled(true);
					if (val.equals("no") || val.equals("off")) obsManager.setReductionEnabled(false);
					log.append("OK"+sep);
					continue;
				}
				if (command[i].startsWith("COMBINE ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					String values[] = new String[] {"Median", "Average", "Maximum", "Kappa"};
					int index = DataSet.getIndex(values, val);
					if (index < 0) {
						log.append("ERROR! "+val+" is invalid"+sep);
						continue;
					}
					if (index == 0) obsManager.setCombineMethod(COMBINATION_METHOD.MEDIAN);
					if (index == 1) obsManager.setCombineMethod(COMBINATION_METHOD.MEAN_AVERAGE);
					if (index == 2) obsManager.setCombineMethod(COMBINATION_METHOD.MAXIMUM);
					if (index == 3) obsManager.setCombineMethod(COMBINATION_METHOD.KAPPA_SIGMA);
					log.append("OK"+sep);
					continue;
				}
				if (command[i].startsWith("INTERPOLATION ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					String values[] = new String[] {"Nearest", "Bilinear", "Bicubic"};
					int index = DataSet.getIndex(values, val);
					if (index < 0) {
						log.append("ERROR! "+val+" is invalid"+sep);
						continue;
					}
					if (index == 0) obsManager.setInterpolationMethod(INTERPOLATION.NEAREST_NEIGHBOR);
					if (index == 1) obsManager.setInterpolationMethod(INTERPOLATION.BILINEAR);
					if (index == 2) obsManager.setInterpolationMethod(INTERPOLATION.BICUBIC);
					log.append("OK"+sep);
					continue;
				}
				if (command[i].startsWith("STACK ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					String values[] = new String[] {"Nearest", "Ponderation"};
					int index = DataSet.getIndex(values, val);
					if (index < 0) {
						log.append("ERROR! "+val+" is invalid"+sep);
						continue;
					}
					if (index == 0) obsManager.setAverageMethod(AVERAGE_METHOD.CLOSEST_POINT);
					if (index == 1) obsManager.setAverageMethod(AVERAGE_METHOD.PONDERATION);
					log.append("OK"+sep);
					continue;
				}
				if (command[i].startsWith("DRIZZLE ")) {
					String val = FileIO.getRestAfterField(1, command[i], " ", true).trim();
					String values[] = new String[] {"1", "2", "3", "0.5"};
					int index = DataSet.getIndex(values, val);
					if (index < 0) {
						log.append("ERROR! "+val+" is invalid"+sep);
						continue;
					}
					if (index == 0) obsManager.setDrizzleMethod(DRIZZLE.NO_DRIZZLE);
					if (index == 1) obsManager.setDrizzleMethod(DRIZZLE.DRIZZLE_2);
					if (index == 2) obsManager.setDrizzleMethod(DRIZZLE.DRIZZLE_3);
					if (index == 3) obsManager.setDrizzleMethod(DRIZZLE.DRIZZLE_HALF);
					log.append("OK"+sep);
					continue;
				}

				if (command[i].equals("CONNECT")) {
					if (dome != null) dome.open();
					telescope.connect();
					log.append("OK"+sep);
				}
				if (command[i].equals("DISCONNECT")) {
					if (dome != null) dome.close();
					telescope.disconnect();
					log.append("OK"+sep);
				}
				log.append("WARNING: this command was ignored "+sep);
			}
		} catch (Exception exc) {
			log.append("ERROR! "+exc.getMessage()+sep);
		}
		return log.toString();
	}
}
