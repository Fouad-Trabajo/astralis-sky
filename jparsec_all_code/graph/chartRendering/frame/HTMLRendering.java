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
package jparsec.graph.chartRendering.frame;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serializable;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import jparsec.util.JPARSECException;

/**
 * An HTML dialog to show very simple data in HTML format.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class HTMLRendering extends JDialog implements ActionListener, Serializable, ComponentListener
{
	private static final long serialVersionUID = 1L;

	private JEditorPane jEditorPane_Message;
	private JButton jButton_Ok;
	private JScrollPane scrollpane;
	private int verticalButtonSize = 50;

	/**
	 * Background color.
	 */
	public Color background = Color.WHITE;

	/**
	 * Constructor for a default HTML non-modal and non-visible dialog.
	 * Default initial size is 400x400 pixels.
	 *
	 * @param title Title.
	 * @param message Content of the dialog.
	 * @param icon Frame icon.
	 * @param showOK True to show the ok button.
	 * @param parent The parent JFrame, or null.
	 * @throws JPARSECException If an error occurs.
	 */
	public HTMLRendering(String title, String message, BufferedImage icon, boolean showOK, 
			JFrame parent)
	throws JPARSECException {
		super(parent);
		init(title, message, icon, showOK);
	}
	
	/**
	 * Constructor for a default HTML non-modal and non-visible dialog.
	 * Default initial size is 400x400 pixels.
	 *
	 * @param title Title.
	 * @param message Content of the dialog.
	 * @param icon Frame icon.
	 * @param showOK True to show the ok button.
	 * @throws JPARSECException If an error occurs.
	 */
	public HTMLRendering(String title, String message, BufferedImage icon, boolean showOK)
	throws JPARSECException {
		init(title, message, icon, showOK);
	}

	private void init(String title, String message, BufferedImage icon, boolean showOK) 
	throws JPARSECException {
		this.setVisible(false);
		if (icon != null) this.setIconImage(icon);
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		setSize(400, 400);
		try
		{
			initialize(icon, showOK);
		} catch (Exception e)
		{
			throw new JPARSECException(e);
		}
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setTitle(title);
		if (message.startsWith("file:") || message.startsWith("http:")) {
			try {
				jEditorPane_Message.setEditorKit(JEditorPane.createEditorKitForContentType("text/html"));
				jEditorPane_Message.setEditable(false);
				jEditorPane_Message.setPage(message);
				jEditorPane_Message.addHyperlinkListener(new HyperlinkListener() {
				    public void hyperlinkUpdate(HyperlinkEvent e) {
				        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
							try {
								jEditorPane_Message.setPage(e.getURL());
							} catch (IOException e1) {
								e1.printStackTrace();
							}
				        }
				    }
				});
			} catch (Exception e) { e.printStackTrace(); }
		} else {
			jEditorPane_Message.setText(message);
		}
		this.pack();
	}
	
	// Component initialization
	private void initialize(BufferedImage icon, boolean showOK) throws JPARSECException
	{
		this.setLayout(new FlowLayout());
		Dimension size = getContentPane().getSize();
		size.height = size.height - verticalButtonSize;

		jEditorPane_Message = new JEditorPane();
		jEditorPane_Message.setContentType("text/html");
		jEditorPane_Message.setSize(size);
		jEditorPane_Message.setPreferredSize(size);
		jEditorPane_Message.setMargin(new Insets(0, 0, 0, 0));
		jEditorPane_Message.setForeground(Color.BLACK);
		jEditorPane_Message.setEnabled(true);
		jEditorPane_Message.setBackground(background);
		jEditorPane_Message.setOpaque(true);
		scrollpane = new JScrollPane(jEditorPane_Message);
		this.getContentPane().add(scrollpane);

		jButton_Ok = new JButton();
		jButton_Ok.setBackground(background);
		jButton_Ok.setText("Ok");
		jButton_Ok.addActionListener(this);
		jButton_Ok.requestFocus();

		if (showOK) this.getContentPane().add(jButton_Ok);

		setResizable(true);
		this.addComponentListener(this);
	}

	/**
	 * Returns the JButton of the Dialog.
	 * @return The JButton.
	 */
	public JButton getOkButton() { return jButton_Ok; }
	
	/**
	 * Returns the JEditorPane of the Dialog.
	 * @return The JEditorPane.
	 */
	public JEditorPane getEditorPane() { return jEditorPane_Message; }

	/**
	 * Returns the JScrollPane of the Dialog.
	 * @return The JScrollPane.
	 */
	public JScrollPane getScrollPane() { return scrollpane; }

	// Overridden so we can exit when window is closed
	protected void processWindowEvent(WindowEvent e)
	{
		if (e.getID() == WindowEvent.WINDOW_CLOSING)
		{
			cancel();
		}
		super.processWindowEvent(e);
	}

	// Close the dialog
	void cancel()
	{
		removeAll();
		dispose();
	}

	/**
	 * Close the dialog on a button event.
	 */
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == jButton_Ok)
		{
			cancel();
		}
	}

	@Override
	public void componentHidden(ComponentEvent arg0) {
	}

	@Override
	public void componentMoved(ComponentEvent arg0) {
	}

	@Override
	public void componentResized(ComponentEvent arg0) {
		Dimension size = getContentPane().getSize();
		size.height = size.height-verticalButtonSize;
		scrollpane.setPreferredSize(size);
		repaint();
	}

	@Override
	public void componentShown(ComponentEvent arg0) {
		repaint();
	}
}
