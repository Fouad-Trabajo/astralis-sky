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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.TreeMap;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jparsec.astrophysics.gildas.Gildas30m;
import jparsec.astrophysics.gildas.LMVCube;
import jparsec.astrophysics.gildas.Spectrum30m;
import jparsec.astrophysics.gildas.Spectrum30m.XUNIT;
import jparsec.astrophysics.gildas.SpectrumHeader30m;
import jparsec.ephem.Functions;
import jparsec.graph.ChartElement;
import jparsec.graph.ChartSeriesElement;
import jparsec.graph.CreateChart;
import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.io.image.HeaderElement;
import jparsec.io.image.Picture;
import jparsec.math.Constant;
import jparsec.util.JPARSECException;
import jparsec.util.Logger;
import jparsec.util.Logger.LEVEL;
import jparsec.util.Translate;

/**
 * Implements a panel to show 30m and lmv spectra for different sources and molecules.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class SpectraChart implements ActionListener, ListSelectionListener, ComponentListener, Serializable {

	private static final long serialVersionUID = 1L;
	JPanel panel;
	private JPanel control;
	private int w, h;
	private String[] f;

	/**
	 * The width of the spectrum in km/s, 0 for the entire spectrum.
	 */
	public int velwidth = 0;
	/**
	 * The separation between consecutive map positions in arcseconds.
	 */
	public double mapSep = 12;
	/**
	 * Name of the initial source selected, or null.
	 */
	public String source = null;
	/**
	 * Name of the initial molecule selected, or null.
	 */
	public String molecule = null;
	/**
	 * Map mode flag.
	 */
	public boolean mapMode = false;

	/**
	 * Horizontal orientation flag.
	 */
	public boolean horizontal = false;

	private TreeMap<String,Spectrum30m[]> spectra = null;
	private static final int CONTROL_WIDTH = 165;

	/**
	 * Basic constructor with 10 km/s of vel width, 10 arcsec of
	 * separation, first source and molecule found, and no map
	 * at startup.
	 * @param files 30m or lmv files to be scanned.
	 * @param w Width.
	 * @param h Height.
	 */
	public SpectraChart(String files[], int w, int h) {
		this.f = files;
		this.w = w;
		this.h = h;
		create();
	}

	/**
	 * Full constructor.
	 * @param files 30m or lmv files to be scanned.
	 * @param w Width.
	 * @param h Height.
	 * @param sep Separation between spectra in a map in arcsec.
	 * @param vw Velocity width in km/s, should be greater than line width.
	 * @param s Source name to be selected at startup.
	 * @param m Molecule to be selected at startup.
	 * @param map True to show a map at startup instead of a spectrum.
	 * @param horiz True to show controls in horizontal, false for vertical.
	 */
	public SpectraChart(String files[], int w, int h, double sep, int vw,
			String s, String m, boolean map, boolean horiz) {
		this.f = files;
		this.w = w;
		this.h = h;
		this.horizontal = horiz;

		this.source = s;
		this.mapSep = sep;
		this.molecule = m;
		this.velwidth = vw;
		this.mapMode = map;
		create();
	}

	/**
	 * Writes the object to a binary file. Only 30m files
	 * are serialized using Spectrum30m class (Gildas30m is not
	 * serializable).
	 * @param out Output stream.
	 * @throws IOException If an error occurs.
	 */
	private void writeObject(ObjectOutputStream out)
	throws IOException {
		out.writeInt(this.f.length);
		for (int i=0; i<f.length; i++) {
        	try {
				Gildas30m g30m = new Gildas30m(f[i]);
				out.writeObject(this.f[i]);
				int list[] = g30m.getListOfSpectrums(true);
				out.writeInt(list.length);
				for (int j=0; j<list.length; j++) {
					Spectrum30m s = g30m.getSpectrum(list[j]);
					out.writeObject(s);
				}
			} catch (Exception e) {
				out.writeObject(this.f[i]);
			}
		}
		out.writeInt(this.w);
		out.writeInt(this.h);
		out.writeBoolean(this.horizontal);
		out.writeObject(this.source);
		out.writeDouble(mapSep);
		out.writeObject(this.molecule);
		out.writeInt(velwidth);
		out.writeBoolean(this.mapMode);
	}
	/**
	 * Reads the object.
	 * @param in Input stream.
	 * @throws IOException I/O error.
	 * @throws ClassNotFoundException Class not found error.
	 */
	private void readObject(ObjectInputStream in)
	throws IOException, ClassNotFoundException {
		int n = in.readInt();
		f = new String[n];
		spectra = new TreeMap<String,Spectrum30m[]>();
		for (int i=0; i<n; i++) {
			f[i] = (String) in.readObject();
			if (f[i].toLowerCase().endsWith(".30m")) {
				int jmax = in.readInt();
				Spectrum30m s[] = new Spectrum30m[jmax];
				for (int j=0; j<jmax; j++) {
					s[j] = (Spectrum30m) in.readObject();
				}
				spectra.put(f[i], s);
			}
		}
		w = in.readInt();
		h = in.readInt();
		horizontal = in.readBoolean();
		source = (String) in.readObject();
		mapSep = in.readDouble();
		molecule = (String) in.readObject();
		velwidth = in.readInt();
		mapMode = in.readBoolean();
		this.create2();
 	}

	/**
	 * Returns the component.
	 * @return The JPanel.
	 */
	public JPanel getComponent() {
		return panel;
	}

	/**
	 * Returns the input files.
	 * @return Input files.
	 */
	public String[] getFiles() {
		return f;
	}
	JCheckBox mapM, area;
	JTextField velW, mStep;
	JButton update;
	JPanel chart;
	String sources[], molecules[][];
	CombinedListItemElement2 cli;
	JComboBox xUnit;
	JLabel label;
	Spectrum30m.XUNIT xUnitID = Spectrum30m.XUNIT.VELOCITY_KMS;
	boolean areaChart = false;
	private void create() {
		readFiles();

		panel = new JPanel();
		panel.setLayout(null);
		panel.setBounds(0, 0, w, h);
		panel.setBackground(new Color(214, 217, 223, 255));
		panel.setOpaque(true);

		cli = new CombinedListItemElement2(
				Translate.translate(928), // "Source",
				Translate.translate(929), // "Line (backend)",
				sources, molecules, panel.getBackground());
		cli.setPositionAndSize(0, 0, w, h, w/2);
		int index = DataSet.getIndex(sources, this.source);
		if (index >= 0) {
			cli.setSelectedIndex1(index);
			index = DataSet.getIndex(molecules[index], this.molecule);
			if (index >= 0) cli.setSelectedIndex2(index);
		}
		control = new JPanel();
		control.setLayout(new FlowLayout());
		try {
			cli.addItem(control);
			cli.ta1.addActionListener(this);
			cli.ta2.addListSelectionListener(this);
			if (!horizontal) cli.ta2.setVisibleRowCount(5);
		} catch (Exception e) {
			Logger.log(LEVEL.ERROR, "Error creating the combine list item element 2. Message was: "+e.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e.getStackTrace()));
		}

		velW = new JTextField(""+this.velwidth, 7);
		JLabel label = new JLabel(Translate.translate(930)+" (km/s)"); // "Velocity width (km/s)");
		control.add(label);
		control.add(velW);

		mStep = new JTextField(""+this.mapSep, 5);
		JLabel label1 = new JLabel(Translate.translate(931)+" (\")"); // "Map separation (\")");
		control.add(label1);
		control.add(mStep);

		JLabel label2 = new JLabel(Translate.translate(932)); // "Select x axis unit");
		xUnit = new JComboBox(new String[] {
				Translate.translate(292), // "Velocity",
				Translate.translate(293), // "Channel number",
				Translate.translate(294) // "Frequency"
		});
		xUnit.addActionListener(this);
		control.add(label2);
		control.add(xUnit);

		mapM = new JCheckBox(Translate.translate(933)); // "Map mode");
		mapM.setSelected(mapMode);
		mapM.addActionListener(this);
		control.add(mapM);

		area = new JCheckBox(Translate.translate(934)); // "Area chart");
		area.setSelected(false);
		area.addActionListener(this);
		control.add(area);

		update = new JButton(Translate.translate(935)); // "Update");
		update.addActionListener(this);
		control.add(update);

		this.label = new JLabel();
		control.add(this.label);

		chart = new JPanel();
		chart.setLayout(new FlowLayout());
		if (horizontal) {
			int ch = 90;
			if (w >= 900) ch = 60;
			if (w < 500) ch = 120;
			control.setBounds(0, 0, w, ch);
			chart.setBounds(0, ch, w, h-ch);
		} else {
			int cw = CONTROL_WIDTH;
			control.setBounds(w - cw, 0, cw, h);
			chart.setBounds(0, 0, w-cw, h);
		}
		panel.add(chart);
		panel.add(control);

		source = cli.getSelectedIndexText1();
		molecule = cli.getSelectedIndexText2()[0];
		generateChart(true);

		panel.validate();
		panel.addComponentListener(this);
        panel.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
            	if (e.getSource() != panel) return;

          	   if (e.getKeyCode() == KeyEvent.VK_DOWN) {
         		   try {
         			   CreateChart.decreaseFontSize();
         		   } catch (Exception e1) {
         				Logger.log(LEVEL.ERROR, "Error increasing font size. Message was: "+e1.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e1.getStackTrace()));
         		   }
	   				SwingUtilities.invokeLater(new Runnable() {
				        public void run() {
				        	generateChart(true);
				        }
	   				});
         	   }
         	   if (e.getKeyCode() == KeyEvent.VK_UP) {
     			   try {
        			   CreateChart.increaseFontSize();
         		   } catch (Exception e1) { }
	   				SwingUtilities.invokeLater(new Runnable() {
				        public void run() {
				        	generateChart(true);
				        }
	   				});
         	   }
            }
          });
	}

	private void create2() {
		readFiles2();

		panel = new JPanel();
		panel.setLayout(null);
		panel.setBounds(0, 0, w, h);
		panel.setBackground(new Color(214, 217, 223, 255));
		panel.setOpaque(true);

		cli = new CombinedListItemElement2(
				Translate.translate(928), // "Source",
				Translate.translate(929), // "Molecule",
				sources, molecules, panel.getBackground());
		cli.setPositionAndSize(0, 0, w, h, w/2);
		int index = DataSet.getIndex(sources, this.source);
		if (index >= 0) {
			cli.setSelectedIndex1(index);
			index = DataSet.getIndex(molecules[index], this.molecule);
			if (index >= 0) cli.setSelectedIndex2(index);
		}
		control = new JPanel();
		control.setLayout(new FlowLayout());
		try {
			cli.addItem(control);
			cli.ta1.addActionListener(this);
			cli.ta2.addListSelectionListener(this);
			if (!horizontal) cli.ta2.setVisibleRowCount(5);
		} catch (Exception e) {
			Logger.log(LEVEL.ERROR, "Error creating the combine list item element 2. Message was: "+e.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e.getStackTrace()));
		}

		velW = new JTextField(""+this.velwidth, 7);
		JLabel label = new JLabel(Translate.translate(930)); // "Velocity width (km/s)");
		control.add(label);
		control.add(velW);

		mStep = new JTextField(""+this.mapSep, 5);
		JLabel label1 = new JLabel(Translate.translate(931)); // "Map separation (\")");
		control.add(label1);
		control.add(mStep);

		mapM = new JCheckBox(Translate.translate(933)); // "Map mode");
		mapM.setSelected(mapMode);
		mapM.addActionListener(this);
		control.add(mapM);

		JLabel label2 = new JLabel(Translate.translate(932)); // "Select x unit");
		xUnit = new JComboBox(new String[] {
				Translate.translate(292), // "Velocity",
				Translate.translate(293), // "Channel number",
				Translate.translate(294) // "Frequency"
		});
		xUnit.addActionListener(this);
		control.add(label2);
		control.add(xUnit);

		area = new JCheckBox(Translate.translate(934)); // "Area chart");
		area.setSelected(false);
		area.addActionListener(this);
		control.add(area);

		update = new JButton(Translate.translate(935)); // "Update");
		update.addActionListener(this);
		control.add(update);

		this.label = new JLabel();
		control.add(this.label);

		chart = new JPanel();
		chart.setLayout(new FlowLayout());
		if (horizontal) {
			int ch = 90;
			if (w >= 900) ch = 60;
			if (w < 500) ch = 120;
			control.setBounds(0, 0, w, ch);
			chart.setBounds(0, ch, w, h-ch);
		} else {
			int cw = CONTROL_WIDTH;
			control.setBounds(w - cw, 0, cw, h);
			chart.setBounds(0, 0, w-cw, h);
		}
		panel.add(chart);
		panel.add(control);

		source = cli.getSelectedIndexText1();
		molecule = cli.getSelectedIndexText2()[0];
		generateChart(true);

		panel.validate();
		panel.addComponentListener(this);
        panel.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
            	if (e.getSource() != panel) return;

     		   if (e.getKeyCode() == KeyEvent.VK_DOWN) {
         		   try {
         			   CreateChart.decreaseFontSize();
         		   } catch (Exception e1) {
        				Logger.log(LEVEL.ERROR, "Error increasing font size. Message was: "+e1.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e1.getStackTrace()));
         		   }
	   				SwingUtilities.invokeLater(new Runnable() {
				        public void run() {
				        	generateChart(true);
				        }
	   				});
         	   }
         	   if (e.getKeyCode() == KeyEvent.VK_UP) {
     			   try {
        			   CreateChart.increaseFontSize();
         		   } catch (Exception e1) { }
	   				SwingUtilities.invokeLater(new Runnable() {
				        public void run() {
				        	generateChart(true);
				        }
	   				});
         	   }
            }
          });
	}

	private void readFiles() {
		ArrayList<String> s = new ArrayList<String>();
		ArrayList<String> m = new ArrayList<String>();
		for (int i=0; i<f.length; i++) {
			try {
	        	Gildas30m g30m = new Gildas30m(f[i]);
	        	int list[] = g30m.getListOfSpectrums(true);

	        	for (int index=0; index<list.length; index++) {
	            	Spectrum30m s30m = g30m.getSpectrum(list[index]);
	            	// Header
	            	SpectrumHeader30m sh = s30m.getHeader();
	            	HeaderElement header[] = sh.getHeaderParameters();

	            	String line = header[SpectrumHeader30m.HEADER.LINE.ordinal()].getAsString().trim();
	            	String source = header[SpectrumHeader30m.HEADER.SOURCE.ordinal()].getAsString().trim();
	            	String teles = header[SpectrumHeader30m.HEADER.TELES.ordinal()].getAsString().trim().toUpperCase();
	            	line += " ("+teles+")";

            		int n = s.indexOf(source);
	            	if (n < 0) {
	            		s.add(source);
	            		m.add(line);
	            	} else {
	            		String mm = m.get(n);
	            		int nn = mm.indexOf(line);
	            		if (nn < 0) {
		            		mm += ","+line;
		            		m.set(n, mm);
	            		}
	            	}
	        	}
			} catch (Exception e) {
				try {
					LMVCube lmv = new LMVCube(f[i]);

	            	String line = "LMV: "+lmv.line.trim();
	            	String source = lmv.sourceName.trim();

            		int n = s.indexOf(source);
	            	if (n < 0) {
	            		s.add(source);
	            		m.add(line);
	            	} else {
	            		String mm = m.get(n);
	            		int nn = mm.indexOf(line);
	            		if (nn < 0) {
		            		mm += ","+line;
		            		m.set(n, mm);
	            		}
	            	}

				} catch (Exception e2) {}
			}
		}

		sources = new String[s.size()];
		molecules = new String[s.size()][];
		for (int i=0; i<s.size(); i++) {
			sources[i] = s.get(i);
			molecules[i] = DataSet.toStringArray(m.get(i), ",");
		}
	}

	private void readFiles2() {
		ArrayList<String> s = new ArrayList<String>();
		ArrayList<String> m = new ArrayList<String>();
		f = new String[spectra.keySet().size()];
		spectra.keySet().toArray(f);
		for (int i=0; i<f.length; i++) {
			try {
				Spectrum30m ss[] = spectra.get(f[i]);

	        	for (int index=0; index<ss.length; index++) {
	            	Spectrum30m s30m = ss[index];
	            	// Header
	            	SpectrumHeader30m sh = s30m.getHeader();
	            	HeaderElement header[] = sh.getHeaderParameters();

	            	String line = header[SpectrumHeader30m.HEADER.LINE.ordinal()].getAsString().trim();
	            	String source = header[SpectrumHeader30m.HEADER.SOURCE.ordinal()].getAsString().trim();
	            	String teles = header[SpectrumHeader30m.HEADER.TELES.ordinal()].getAsString().trim().toUpperCase();
	            	line += " ("+teles+")";

            		int n = s.indexOf(source);
	            	if (n < 0) {
	            		s.add(source);
	            		m.add(line);
	            	} else {
	            		String mm = m.get(n);
	            		int nn = mm.indexOf(line);
	            		if (nn < 0) {
		            		mm += ","+line;
		            		m.set(n, mm);
	            		}
	            	}
	        	}
			} catch (Exception e) {
 				Logger.log(LEVEL.ERROR, "Error reading file "+f[i]+". Message was: "+e.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e.getStackTrace()));
			}
		}

		sources = new String[s.size()];
		molecules = new String[s.size()][];
		for (int i=0; i<s.size(); i++) {
			sources[i] = s.get(i);
			molecules[i] = DataSet.toStringArray(m.get(i), ",");
		}
	}

	CreateChart ch = null;
	private Color col[] = new Color[] {
			Color.BLACK, Color.RED, Color.GREEN, Color.BLUE, Color.ORANGE, Color.MAGENTA,
			Color.PINK, Color.YELLOW, Color.CYAN, Color.GRAY
	};
	boolean setSep = false;
	double searchOffset1 = 0, searchOffset2 = 0;
	private void generateChart(boolean draw) {
		chart.removeAll();

		String m[] = cli.getSelectedIndexText2();
		if ((m == null || m.length < 1) && draw) {
			chart.validate();
			chart.paintImmediately(0, 0, w, h);
			return;
		}

		if (m.length == 1) setSep = true;
		ch = getChart(m, searchOffset1, searchOffset2);
		setSep = false;
		if ((!mapMode && ch == null) && draw) {
			chart.validate();
			chart.paintImmediately(0, 0, w, h);
			return;
		}

		if (!mapMode) {
			if (ch == null) return;
			chart.setLayout(new FlowLayout());
			try {
				chart.add(ch.getComponent());
			} catch (Exception e) {
 				Logger.log(LEVEL.ERROR, "Error adding chart component to panel. Message was: "+e.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e.getStackTrace()));
			}
		} else {
			double sep = this.mapSep;
			try {
				sep = Double.parseDouble(this.mStep.getText());
				mapSep = sep;
			} catch (Exception exc) {}

			int nmax1 = 0, nmax2 = 0, nmax3 = 0, nmax4 = 0;
			int imax = 5;
			CreateChart ch;
			for (int i=1; i<imax+1; i++) {
				ch = null;
				if (nmax1 == 0) ch = getChart(m, i*sep, 0);
				if ((ch == null || i==imax) && nmax1 == 0) nmax1 = i;
				ch = null;
				if (nmax2 == 0) ch = getChart(m, -i*sep, 0);
				if ((ch == null || i==imax) && nmax2 == 0) nmax2 = i;
				ch = null;
				if (nmax3 == 0) ch = getChart(m, 0, i*sep);
				if ((ch == null || i==imax) && nmax3 == 0) nmax3 = i;
				ch = null;
				if (nmax4 == 0) ch = getChart(m, 0, -i*sep);
				if ((ch == null || i==imax) && nmax4 == 0) nmax4 = i;
			}
			int nm = Math.max(nmax1, nmax2);
			nm = Math.max(nm, nmax3);
			nm = Math.max(nm, nmax4);
			nm--;
			chart.setLayout(new GridLayout(2*nm+1, 2*nm+1));
			for (int ny = nm; ny>= -nm; ny--) {
				for (int nx = -nm; nx<= nm; nx++) {
					ch = getChart(m, nx*sep, ny*sep);
					if (ch == null) {
						chart.add(new JPanel());
					} else {
						try {
							chart.add(ch.getComponent());
						} catch (Exception e) {
	         				Logger.log(LEVEL.ERROR, "Error adding chart "+nx+", "+ny+" to panel. Message was: "+e.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e.getStackTrace()));
						}
					}
				}
			}
		}

		chart.validate();
		if (draw) chart.paintImmediately(0, 0, w, h);
		searchOffset1 = 0;
		searchOffset2 = 0;

		if (mapMode) {
			BufferedImage buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			Graphics g = buf.createGraphics();
			panel.paintAll(g);
			Picture p = new Picture(buf.getSubimage(0, 0, chart.getWidth(), chart.getHeight()));
			p.getScaledInstance(125, 125, true);
			Dimension d = p.getSize();
			label.setSize(d.width, d.height);
			label.setIcon(new ImageIcon(p.getImage()));
			if (label.getMouseListeners().length == 0) {
		        label.addMouseListener(new MouseAdapter() {
		        	private int lastN = 0;
		        	@Override
		        	public void mouseClicked(MouseEvent e) {
		        		Point p = e.getLocationOnScreen();
		        		Point p2 = label.getLocationOnScreen();
		    			int x = p.x - p2.x, y = p.y - p2.y, w = label.getWidth(), h = label.getHeight();
		        		if (x > 0 && y > 0 && x < w && y < h) {
		        			mapMode = false;
		        			int n = (int) Math.sqrt(chart.getComponentCount());
		        			if (n == 1 && lastN > n) n = lastN;
		        			searchOffset1 = (double) x * (double) n / (double) w;
		        			if (searchOffset1 >= n) searchOffset1 = n-1;
		        			searchOffset2 = n - (double) y * (double) n / (double) h;
		        			if (searchOffset2 >= n) searchOffset2 = n-1;
		        			if (searchOffset2 < 0) searchOffset2 = 0;

		        			int p0 = n / 2;
		        			searchOffset1 = ((int)searchOffset1 - p0) * mapSep;
		        			searchOffset2 = ((int)searchOffset2 - p0) * mapSep;
		        			if (n > 1) lastN = n;
		        			generateChart(true);
		        		}
		        	}
		        });
			}
		}
	}

	private boolean areSimilar(double v1, double v2) {
		boolean similar = false;
		if (Math.abs(v1-v2) < 0.5) similar = true;
		return similar;
	}
	private LMVCube lmv;
	private Gildas30m g30m;
	private String lastLMV, last30m;
	private CreateChart getChart(String[] m, double searchOffset1, double searchOffset2) {
		int width = chart.getWidth(), height = chart.getHeight();
		String s = cli.getSelectedIndexText1();

		CreateChart ch = null;
		boolean sourceFound = false, spectrumFound = false;
		int nsp = 1;
    	int lastID[] = new int[m.length];
		for (int i=0; i<lastID.length; i++) {
			lastID[i] = -1;
		}
		boolean somelmv = false;
		for (int i=0; i<m.length; i++) {
			if (m[i].startsWith("LMV")) somelmv = true;
		}
		for (int i=0; i<f.length; i++) {
			try {
				if (m.length == 1 && somelmv && lastLMV != null) {
					int in = DataSet.getIndex(f, lastLMV);
					if (in >= 0) i = in;
					throw new Exception("read the lmv directly...");
				}
				if (!somelmv && f[i].toLowerCase().indexOf(".lmv") > 0) {
					continue;
				}

				if (g30m == null || !f[i].equals(last30m)) {
		        	g30m = new Gildas30m(f[i]);
		        	g30m.getListOfSpectrums(true);
					last30m = f[i];
				}
	        	int list[] = g30m.getListOfSpectrums(true);

            	if (setSep) this.mStep.setText("12");

    			boolean hasData = false;
    			if (m.length > 1) {
		        	for (int index=0; index < list.length; index++) {
		            	Spectrum30m s30m = g30m.getSpectrum(list[index]);
		            	HeaderElement header[] = s30m.getHeader().getHeaderParameters();

		            	double off1 = header[SpectrumHeader30m.HEADER.OFFSET1.ordinal()].getAsDouble() * Constant.RAD_TO_ARCSEC;
		            	double off2 = header[SpectrumHeader30m.HEADER.OFFSET2.ordinal()].getAsDouble() * Constant.RAD_TO_ARCSEC;
		            	String line = header[SpectrumHeader30m.HEADER.LINE.ordinal()].getAsString().trim();
		            	String source = header[SpectrumHeader30m.HEADER.SOURCE.ordinal()].getAsString().trim();
		            	String teles = header[SpectrumHeader30m.HEADER.TELES.ordinal()].getAsString().trim().toUpperCase();
		            	line += " ("+teles+")";

		            	for (int j=0; j<m.length; j++) {
			            	if (s.equals(source) && m[j].equals(line) && areSimilar(off1, searchOffset1) && areSimilar(off2, searchOffset2)) {
			            		lastID[j] = index;
			            	}
		            	}
		            	if (s.equals(source)) sourceFound = true;
		            	if (!sourceFound) break;

		            	if (s.equals(source) && areSimilar(off1, searchOffset1) && areSimilar(off2, searchOffset2))
		            		hasData = true;
		        	}
	    			if (!hasData) continue;
    			}

	        	for (int index=0; index<list.length; index++) {
	            	Spectrum30m s30m = g30m.getSpectrum(list[index]);
	            	HeaderElement header[] = s30m.getHeader().getHeaderParameters();

	            	double off1 = header[SpectrumHeader30m.HEADER.OFFSET1.ordinal()].getAsDouble() * Constant.RAD_TO_ARCSEC;
	            	double off2 = header[SpectrumHeader30m.HEADER.OFFSET2.ordinal()].getAsDouble() * Constant.RAD_TO_ARCSEC;
	            	String line = header[SpectrumHeader30m.HEADER.LINE.ordinal()].getAsString().trim();
	            	String source = header[SpectrumHeader30m.HEADER.SOURCE.ordinal()].getAsString().trim();
	            	String teles = header[SpectrumHeader30m.HEADER.TELES.ordinal()].getAsString().trim().toUpperCase();
	            	line += " ("+teles+")";

	            	if (s.equals(source) && areSimilar(off1, searchOffset1) && areSimilar(off2, searchOffset2)) {
		            	for (int k=0; k<m.length; k++) {
			            	if (m[k].equals(line)) {
			            		sourceFound = true;
			            		if (spectrumFound) {
				            		CreateChart ch2 = s30m.getChart(width, height-5, xUnitID);
				            		String leyend2 = ch2.getChartElement().series[0].legend;

				            		ChartElement chart = ch.getChartElement();
				            		int n = -1;
				            		for (int j=0; j<chart.series.length; j++) {
				            			if (chart.series[j].legend.equals(leyend2)) {
				            				n = j;
				            				break;
				            			}
				            		}
				            		if (n >= 0) {
				            			ChartSeriesElement s2 = ch2.getChartElement().series[0];
				            			ChartSeriesElement s0 = chart.series[n];
				            			double x0[] = DataSet.getDoubleValuesExcludingLimits(s0.xValues);
				            			double y0[] = DataSet.getDoubleValuesExcludingLimits(s0.yValues);
				            			double x2[] = DataSet.getDoubleValuesExcludingLimits(s2.xValues);
				            			double y2[] = DataSet.getDoubleValuesExcludingLimits(s2.yValues);
				            			if (x0.length == x2.length) {
					            			nsp ++;
					            			for (int j=0;j<x0.length; j++) {
					            				if (x0[j] == x2[j]) y0[j] = (y0[j] + y2[j]);
					            				if (index == lastID[k]) y0[j] /= nsp;
					            			}
					            			s0.yValues = DataSet.toStringValues(y0);
					            			chart.series[n] = s0;
				            			}
				            		} else {
				            			ChartSeriesElement cs = ch2.getChartElement().series[0];
				            			int p = chart.series.length % col.length;
				            			cs.color = col[p];
					            		chart.addSeries(cs);
				            		}
			            			ch = new CreateChart(chart);
			            		} else {
				            		spectrumFound = true;
				            		ch = s30m.getChart(width, height-5, xUnitID);
			            		}
		            		}
		            	}
	        		}
	            	if (s.equals(source)) sourceFound = true;
	            	if (!sourceFound) break;
	        	}
			} catch (Exception e) {
				// Try as lmv
				try {
					if (lmv == null || !f[i].equals(lastLMV)) {
						lmv = new LMVCube(f[i]);
						lmv.setCubeData(lmv.getCubeData());
						lastLMV = f[i];
					}
	            	String line = "LMV: "+lmv.line.trim();
	            	String source = lmv.sourceName.trim();

	            	double x0 = lmv.getx0() * Constant.RAD_TO_ARCSEC, dx = lmv.conversionFormula[2] * Constant.RAD_TO_ARCSEC; // xf = lmv.getxf() * Constant.RAD_TO_ARCSEC
	            	double y0 = lmv.gety0() * Constant.RAD_TO_ARCSEC, dy = lmv.conversionFormula[5] * Constant.RAD_TO_ARCSEC; // yf = lmv.getyf() * Constant.RAD_TO_ARCSEC
	            	double ix = (searchOffset1 - x0) / dx;
	            	double iy = (searchOffset2 - y0) / dy;
	            	int px = (int) (ix+0.5);
	            	int py = (int) (iy+0.5);
	            	int pm = DataSet.getIndex(m, line);
	            	if (setSep) this.mStep.setText(Functions.formatValue(Math.abs(lmv.conversionFormula[2]) * Constant.RAD_TO_ARCSEC, 2));
	            	if (pm >= 0 && px >= 0 && py >= 0 && px < lmv.axis1Dim && py < lmv.axis2Dim && s.equals(source)) {
	            		sourceFound = true;
	            		if (spectrumFound) {
		            		CreateChart ch2 = lmv.getChart(px, py, width, height-5, xUnitID);
	            			ChartSeriesElement cs = ch2.getChartElement().series[0];
		            		ChartElement chart = ch.getChartElement();
	            			int p = chart.series.length % col.length;
	            			cs.color = col[p];
		            		chart.addSeries(cs);
	            			ch = new CreateChart(chart);
	            		} else {
		            		spectrumFound = true;
		            		ch = lmv.getChart(px, py, width, height-5, xUnitID);
	            		}
	            	}
				} catch (Exception e2) { }
			}
        	if (sourceFound) break;
		}

		if (ch == null) return ch;

		ChartElement chart = ch.getChartElement();
		if (areaChart) chart.subType = ChartElement.SUBTYPE.XY_AREA;
		if (m.length == 1) chart.series[0].showLegend = false;
		if (m.length > 1) {
			String t = chart.title;
			int a = t.indexOf(" (");
			if (a > 0) {
				t = t.substring(0, a);
				chart.title = t;
			}
		}
		if (mapMode) chart.title = null;
		if ((searchOffset1 != 0.0 || searchOffset2 != 0.0) || mapMode) {
			if (chart.title == null) {
				chart.title = Translate.translate(936) + " "+Functions.formatValue(searchOffset1, 2)+" "+Functions.formatValue(searchOffset2, 2);
			} else {
				chart.title += " ("+Translate.translate(936)+" "+Functions.formatValue(searchOffset1, 2)+" "+Functions.formatValue(searchOffset2, 2)+")";
			}
		}
		int velocityW = 0;
		try {
			velocityW = Integer.parseInt("0"+this.velW.getText());
		} catch (Exception exc) {}
		if (velocityW > 0 || this.velW.getText().indexOf("/") > 0) {
			int vel0 = -velocityW/2, velf = velocityW/2;
			if (velocityW == 0) {
				vel0 = Integer.parseInt(FileIO.getField(1, velW.getText(), "/", false));
				velf = Integer.parseInt(FileIO.getField(2, velW.getText(), "/", false));
			}
			for (int i=0; i<chart.series.length; i++) {
				try {
					ChartSeriesElement cse = chart.series[i].clone();
	    			double x0[] = DataSet.getDoubleValuesExcludingLimits(cse.xValues);
	    			double y0[] = DataSet.getDoubleValuesExcludingLimits(cse.yValues);
	    			ArrayList<double[]> al = DataSet.sortInCrescent(x0, y0, false);
	    			x0 = al.get(0);
	    			y0 = al.get(1);
	    			int i0 = -1, i1 = -1;
	    			for (int j=0; j<x0.length; j++) {
	    				if (x0[j] < vel0) i0 = j;
	    				if (x0[j] > velf && i1 == -1) i1 = j-1;
	    			}
	    			if (i0 != -1 && i1 != -1) {
	    				x0 = DataSet.getSubArray(x0, i0, i1);
	    				y0 = DataSet.getSubArray(y0, i0, i1);
            			cse.xValues = DataSet.toStringValues(x0);
            			cse.yValues = DataSet.toStringValues(y0);
	    				chart.series[i] = cse;
	    			}
				} catch (Exception exc) {
     				Logger.log(LEVEL.ERROR, "Error constructing series "+i+". Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
				}
			}
		}
		try {
			ch = new CreateChart(chart);
		} catch (Exception e) {
			Logger.log(LEVEL.ERROR, "Error creating chart. Message was: "+e.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e.getStackTrace()));
		}
		return ch;
	}
	/**
	 * Updates the chart after some button/box is clicked.
	 */
	public void actionPerformed(ActionEvent e) {
		xUnitID = XUNIT.values()[xUnit.getSelectedIndex()];
		mapMode = mapM.isSelected();
		areaChart = area.isSelected();
		label.setVisible(true);
		if (!mapMode) label.setVisible(false);
		source = cli.getSelectedIndexText1();
		if (cli.getSelectedIndexText2() == null) {
			cli.setSelectedIndex2(0);
			cli.ta2.setSelectedIndex(0);
		}
		molecule = cli.getSelectedIndexText2()[0];
		generateChart(true);
		panel.requestFocusInWindow();
	}

	/**
	 * Updates the panel after a new item is selected.
	 */
	public void valueChanged(ListSelectionEvent e) {
		xUnitID = XUNIT.values()[xUnit.getSelectedIndex()];
		mapMode = mapM.isSelected();
		areaChart = area.isSelected();
		label.setVisible(true);
		if (!mapMode) label.setVisible(false);
		source = cli.getSelectedIndexText1();
		if (cli.getSelectedIndexText2() == null) {
			cli.setSelectedIndex2(0);
			cli.ta2.setSelectedIndex(0);
		}
		molecule = cli.getSelectedIndexText2()[0];
		generateChart(true);
	}

	/**
	 * To be called when the panel/frame is resized.
	 * @param w Width.
	 * @param h Height.
	 */
	public void resized(int w, int h) {
		this.w = w;
		this.h = h;
		if (horizontal) {
			int ch = 90;
			if (w >= 900) ch = 60;
			if (w < 500) ch = 120;
			control.setBounds(0, 0, w, ch);
			chart.setBounds(0, ch, w, h-ch);
		} else {
			int cw = CONTROL_WIDTH;
			control.setBounds(w - cw, 0, cw, h);
			chart.setBounds(0, 0, w-cw, h);
		}

		generateChart(false);
		panel.revalidate();
		panel.repaint();
	}
	/**
	 * Nothing.
	 */
	public void componentHidden(ComponentEvent arg0) {
	}

	/**
	 * Nothing.
	 */
	public void componentMoved(ComponentEvent arg0) {
	}

	/**
	 * Updates the panel when the size changes.
	 */
	public void componentResized(ComponentEvent e) {
		Dimension d = e.getComponent().getSize();
		resized(d.width, d.height);
	}

	/**
	 * Nothing.
	 */
	public void componentShown(ComponentEvent arg0) {
	}
}

/**
 * Defines a combined list to be used in dialogs.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
class CombinedListItemElement2 implements Serializable, ActionListener, ListSelectionListener {
	static final long serialVersionUID = 1L;

	private JPanel panel;
	private int panelC;

	/**
	 * Item id.
	 */
	public String itemID;
	/**
	 * Label 1.
	 */
	public String label1;
	/**
	 * Label 2.
	 */
	public String label2;
	/**
	 * Background.
	 */
	public Color backGround;
	/**
	 * Values for the list 1.
	 */
	public String[] values1;
	/**
	 * Values for the list 2.
	 */
	public String[][] values2;
	/**
	 * Tooltip.
	 */
	public String toolTip;
	/**
	 * X position.
	 */
	public int posX;
	/**
	 * Y position.
	 */
	public int posY;
	/**
	 *  Width for list 1.
	 */
	public int width;
	/**
	 *  Width for list 2.
	 */
	public int width2;
	/**
	 * Height
	 */
	public int height;
	/**
	 * Selected index 1. 0 by default.
	 */
	public int selectedIndex1 = 0;
	/**
	 * Selected index 2. 0 by default.
	 */
	public int selectedIndex2[] = new int[] {0};

	public JComboBox ta1 = new JComboBox();
	public JList ta2 = new JList();

	/**
	 * Empty constructor.
	 */
	public CombinedListItemElement2()	{	}

	/**
	 * Constructor for a combined list.
	 * @param label1 Label 1.
	 * @param label2 Label 2.
	 * @param values1 List 1 items.
	 * @param values2 List 2 items.
	 * @param backGround Background.
	 */
	public CombinedListItemElement2(String label1, String label2, String values1[], String values2[][], Color backGround)
	{
		this.label1 = label1;
		this.label2 = label2;
		this.values1 = values1;
		this.values2 = values2;
		if (backGround != null) this.backGround = backGround;
		this.toolTip = "";
		this.itemID = "";
	}
	/**
	 * Constructor for a combined list.
	 * @param ID Item id.
	 * @param label1 Label 1.
	 * @param label2 Label 2.
	 * @param values1 List 1 items.
	 * @param values2 List 2 items.
	 * @param toolTip Tooltip.
	 * @param backGround Background.
	 */
	public CombinedListItemElement2(String ID, String label1, String label2, String values1[], String values2[][], String toolTip, Color backGround)
	{
		this.label1 = label1;
		this.label2 = label2;
		this.values1 = values1;
		this.values2 = values2;
		if (backGround != null) this.backGround = backGround;
		this.toolTip = toolTip;
		this.itemID = ID;
	}

	/**
	 * Sets the position and size.
	 * @param x X position.
	 * @param y Y position.
	 * @param w Width.
	 * @param h Height.
	 * @param w2 Width for list 2.
	 */
	public void setPositionAndSize(int x, int y, int w, int h, int w2)
	{
		this.posX = x;
		this.posY = y;
		this.width = w;
		this.height = h;
		this.width2 = w2;
	}

	/**
	 * Adds the item to a panel.
	 * @param panel Panel.
	 * @throws JPARSECException If an error occurs.
	 */
	public void addItem(JPanel panel)
	throws JPARSECException {
		JLabel label = new JLabel(this.label1);
		if (!this.toolTip.equals("")) label.setToolTipText(this.toolTip);

		panel.add(label);

		JComboBox list = new JComboBox(this.values1);
		if (this.backGround != null) list.setBackground(this.backGround);
		list.setSelectedIndex(selectedIndex1);
		if (this.toolTip != null) {
			if (!this.toolTip.equals("")) list.setToolTipText(this.toolTip);
		}
		panel.add(list);
		ta1 = list;

		JLabel label2 = new JLabel(this.label2);
		if (!this.toolTip.equals("")) label2.setToolTipText(this.toolTip);
		panel.add(label2);

		JList list2 = new JList(this.eliminateNulls(this.values2[this.selectedIndex1]));
		if (this.backGround != null) list2.setBackground(this.backGround);
		list2.setSelectedIndices(selectedIndex2);
		list2.setVisibleRowCount(3);
		list2.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		if (this.toolTip != null) {
			if (!this.toolTip.equals("")) list2.setToolTipText(this.toolTip);
		}
		list2.setCellRenderer(new MyCellRenderer());
		JScrollPane scrollPane = new JScrollPane(list2);
		panel.add(scrollPane);
		ta2 = list2;

		ta1.addActionListener(this);
		ta2.addListSelectionListener(this);
		this.panel = panel;
		this.panelC = panel.getComponentCount();
	}

	/**
	 * Returns the selected index of list 1.
	 * @return Selected index.
	 */
	public int getSelectedIndex1()
	{
		return ta1.getSelectedIndex();
	}
	/**
	 * Returns the selected index of list 2.
	 * @return Selected index.
	 */
	public int[] getSelectedIndex2()
	{
		return ta2.getSelectedIndices();
	}

	/**
	 * Gets the text of the currently selected index of list 1.
	 * @return The text of the selected index.
	 */
	public String getSelectedIndexText1()
	{
		int index = ta1.getSelectedIndex();
		if (index < 0 || index > values1.length) return null;
		return values1[index];
	}
	/**
	 * Gets the text of the currently selected index of list 2.
	 * @return The text of the selected index.
	 */
	public String[] getSelectedIndexText2()
	{
		int index[] = ta2.getSelectedIndices();
		if (index.length < 1) return null;
		String out[] = new String[index.length];

		if (getSelectedIndex1() > values2.length) return null;

		for (int i=0; i<out.length; i++) {
			if (index[i] > values2[getSelectedIndex1()].length) return null;
			out[i] = values2[getSelectedIndex1()][index[i]];;
		}
		return out;
	}

	/**
	 * Sets the selected index of list 1.
	 * @param index Index to set.
	 */
	public void setSelectedIndex1(int index)
	{
		selectedIndex1 = index;
	}
	/**
	 * Sets the selected index of list 2.
	 * @param index Index to set.
	 */
	public void setSelectedIndex2(int index)
	{
		selectedIndex2 = new int[] {index};
	}
	/**
	 * Returns the index of a given item from its name, for list 1.
	 * @param name Item name.
	 * @return The index in the list, or -1 if no match is found.
	 */
	public int getItemFromName1(String name)
	{
		int index = -1;
		for (int i=0; i<this.values1.length; i++)
	    {
	    	if (this.values1[i].equals(name))
	    	{
	    		index = i;
	    		break;
	    	}
	    }
		return index;
	}
	/**
	 * Returns the index of a given item from its name, for list 2.
	 * @param name1 Item name for list 1.
	 * @param name2 Item name for list 2.
	 * @return The index in the list, or -1 if no match is found.
	 */
	public int getItemFromName2(String name1, String name2)
	{
		int index1 = this.getItemFromName1(name1);
		int index = -1;
		for (int i=0; i<this.values2[index1].length; i++)
	    {
	    	if (this.values2[index1][i].equals(name2))
	    	{
	    		index = i;
	    		break;
	    	}
	    }
		return index;
	}

	private String[] eliminateNulls(String[] list)
	{
		int l = list.length;
		int index = l-1;
		for (int i=l-1; i>=0; i--)
		{
			if (list[i] != null) {
				index = i;
				break;
			}
		}
		String out[] = new String[index+1];
		for (int i=0; i<out.length; i++)
		{
			out[i] = list[i];
		}
		return out;
	}

	/**
	 * Action to be taken when a button is pressed.
	 */
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == ta1) {
			int selected = selectedIndex1;
			selectedIndex1 = ((JComboBox) this.panel.getComponent(this.panelC-3)).getSelectedIndex();
			if (selected != selectedIndex1)
			{
				ta2.getSelectionModel().clearSelection();
				ta2.setListData(this.values2[selectedIndex1]);
				selectedIndex2 = new int[] {0};
				ta2.setSelectedIndices(selectedIndex2);
			}
		}
		if (e.getSource() == ta2) {
			selectedIndex2 = ta2.getSelectedIndices();
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent arg0) {
		selectedIndex2 = ta2.getSelectedIndices();
	}

	/**
	 * Returns the size of a string in a given panel. The returned width
	 * and height is computed according to the Java API. As described in
	 * the javadoc, the returned values could not enclose the whole text,
	 * so it is recommended to manually increase the value by, let's say,
	 * around 10 pixels in both width and height.
	 * @param panel The panel object.
	 * @param text The text to measure.
	 * @param font The font to be used.
	 * @return The size of the text.
	 */
	public static Dimension getTextSize(JPanel panel, String text, Font font)
	{
		JFrame frame = new JFrame("nothing");
		frame.add(panel);
		frame.pack();

        FontRenderContext frc = ((Graphics2D) frame.getGraphics()).getFontRenderContext();
        TextLayout tl = new TextLayout(text, font, frc);
        int twidth = (int) tl.getBounds().getWidth();
        int theight = (int) tl.getBounds().getHeight();

		frame.setEnabled(false);
		frame.setVisible(false);
		frame.removeAll();
		frame.dispose();

        return new Dimension(twidth, theight);
	}
}

class MyCellRenderer extends JLabel implements ListCellRenderer {
	private static final long serialVersionUID = 1L;

	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
	 // Get text to display.
	 String s = value.toString();
	 // Set the text.
	 setText(s);
	 // Get the font.
	 Font f = new Font(Font.DIALOG, Font.PLAIN, 9);
	 if (isSelected) f = new Font(Font.DIALOG, Font.BOLD, 9);
	 // Set the font.
	 setFont(f);

	 return this;
	 }
}
