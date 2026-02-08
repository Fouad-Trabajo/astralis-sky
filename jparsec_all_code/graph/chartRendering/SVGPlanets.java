package jparsec.graph.chartRendering;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;

import javax.swing.Icon;

import jparsec.ephem.Target.TARGET;

/**
 * An implementation of SVG planetary icons (vector graphics) in AWT,
 * using SVG2Java library.
 * @author T. Alonso Albi - OAN (Spain)
 */
public class SVGPlanets {

	/**
	 * Draws the planetary icon.
	 * @param target Target (planets, Pluto, Sun).
	 * @param g2 The graphics instance.
	 * @param px0 Center x position.
	 * @param py0 Center y position.
	 * @param zoom Zoom factor.
	 */
	public static void drawIcon(TARGET target, Object g2, double px0, double py0, double zoom)
	{
		int px = 0, py = 0;
		Graphics2D g = (Graphics2D)((Graphics2D) g2).create();
		g.translate(px0, py0);
		g.scale(zoom, zoom);
		switch (target) {
		case SUN:
			Sun sun = new Sun();
	    	px = (int)(sun.getOrigX()+sun.getOrigWidth()/2f);
	    	py = (int)(sun.getOrigY()+sun.getOrigHeight()/2f);
	    	sun.paintIcon(null, g, -px, -py);
			break;
		case MERCURY:
			Mercury mercury = new Mercury();
	    	px = (int)(mercury.getOrigX()+mercury.getOrigWidth()/2f);
	    	py = (int)(mercury.getOrigY()+mercury.getOrigHeight()/2f);
	    	mercury.paintIcon(null, g, -px, -py);
			break;
		case VENUS:
			Venus venus = new Venus();
	    	px = (int)(venus.getOrigX()+venus.getOrigWidth()/2f);
	    	py = (int)(venus.getOrigY()+venus.getOrigHeight()/2f);
	    	venus.paintIcon(null, g, -px, -py);
			break;
		case EARTH:
			Earth earth = new Earth();
	    	px = (int)(earth.getOrigX()+earth.getOrigWidth()/2f);
	    	py = (int)(earth.getOrigY()+earth.getOrigHeight()/2f);
	    	earth.paintIcon(null, g, -px, -py);
			break;
		case MARS:
			Mars mars = new Mars();
	    	px = (int)(mars.getOrigX()+mars.getOrigWidth()/2f);
	    	py = (int)(mars.getOrigY()+mars.getOrigHeight()/2f);
	    	mars.paintIcon(null, g, -px, -py);
			break;
		case JUPITER:
			Jupiter jupiter = new Jupiter();
	    	px = (int)(jupiter.getOrigX()+jupiter.getOrigWidth()/2f);
	    	py = (int)(jupiter.getOrigY()+jupiter.getOrigHeight()/2f);
	    	jupiter.paintIcon(null, g, -px, -py);
			break;
		case SATURN:
			Saturn saturn = new Saturn();
	    	px = (int)(saturn.getOrigX()+saturn.getOrigWidth()/2f);
	    	py = (int)(saturn.getOrigY()+saturn.getOrigHeight()/2f);
	    	saturn.paintIcon(null, g, -px, -py);
			break;
		case URANUS:
			Uranus uranus = new Uranus();
	    	px = (int)(uranus.getOrigX()+uranus.getOrigWidth()/2f);
	    	py = (int)(uranus.getOrigY()+uranus.getOrigHeight()/2f);
	    	uranus.paintIcon(null, g, -px, -py);
			break;
		case NEPTUNE:
			Neptune neptune = new Neptune();
	    	px = (int)(neptune.getOrigX()+neptune.getOrigWidth()/2f);
	    	py = (int)(neptune.getOrigY()+neptune.getOrigHeight()/2f);
	    	neptune.paintIcon(null, g, -px, -py);
			break;
		case Pluto:
			Pluto pluto = new Pluto();
	    	px = (int)(pluto.getOrigX()+pluto.getOrigWidth()/2f);
	    	py = (int)(pluto.getOrigY()+pluto.getOrigHeight()/2f);
	    	pluto.paintIcon(null, g, -px, -py);
			break;
		default:
			break;
		}
	}

	/**
	 * Creates an Image of the planetary icon.
	 * @param target Target (planets, Pluto, Sun).
	 * @param zoom Zoom factor.
	 * @return The image, white background.
	 */
	public static BufferedImage drawIcon(TARGET target, float zoom)
	{
		int px = 0, py = 0, w = 0, h = 0;
		BufferedImage image = null;

		switch (target) {
		case SUN:
			Sun sun = new Sun();
			sun.width = (int) (sun.width*zoom);
			sun.height = (int) (sun.height*zoom);
	    	px = (int)((sun.getOrigX()-1)*zoom);
	    	py = (int)((sun.getOrigY()-1)*zoom);
	    	w = (int)((sun.getOrigWidth()+2)*zoom);
	    	h = (int)((sun.getOrigHeight()+2)*zoom);
	    	image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
	    	Graphics2D g = image.createGraphics();
	    	g.setColor(Color.BLACK);
	    	sun.paintIcon(null, g, -px, -py);
	    	g.dispose();
			break;
		case MERCURY:
			Mercury mercury = new Mercury();
			mercury.width = (int) (mercury.width*zoom);
			mercury.height = (int) (mercury.height*zoom);
	    	px = (int)((mercury.getOrigX()-1)*zoom);
	    	py = (int)((mercury.getOrigY()-1)*zoom);
	    	w = (int)((mercury.getOrigWidth()+2)*zoom);
	    	h = (int)((mercury.getOrigHeight()+2)*zoom);
	    	image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
	    	g = image.createGraphics();
	    	g.setColor(Color.BLACK);
	    	mercury.paintIcon(null, g, -px, -py);
	    	g.dispose();
			break;
		case VENUS:
			Venus venus = new Venus();
			venus.width = (int) (venus.width*zoom);
			venus.height = (int) (venus.height*zoom);
	    	px = (int)((venus.getOrigX()-1)*zoom);
	    	py = (int)((venus.getOrigY()-1)*zoom);
	    	w = (int)((venus.getOrigWidth()+2)*zoom);
	    	h = (int)((venus.getOrigHeight()+2)*zoom);
	    	image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
	    	g = image.createGraphics();
	    	g.setColor(Color.BLACK);
	    	venus.paintIcon(null, g, -px, -py);
	    	g.dispose();
			break;
		case EARTH:
			Earth earth = new Earth();
			earth.width = (int) (earth.width*zoom);
			earth.height = (int) (earth.height*zoom);
	    	px = (int)((earth.getOrigX()-1)*zoom);
	    	py = (int)((earth.getOrigY()-1)*zoom);
	    	w = (int)((earth.getOrigWidth()+2)*zoom);
	    	h = (int)((earth.getOrigHeight()+2)*zoom);
	    	image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
	    	g = image.createGraphics();
	    	g.setColor(Color.BLACK);
	    	earth.paintIcon(null, g, -px, -py);
	    	g.dispose();
			break;
		case MARS:
			Mars mars = new Mars();
			mars.width = (int) (mars.width*zoom);
			mars.height = (int) (mars.height*zoom);
	    	px = (int)((mars.getOrigX()-1)*zoom);
	    	py = (int)((mars.getOrigY()-1)*zoom);
	    	w = (int)((mars.getOrigWidth()+2)*zoom);
	    	h = (int)((mars.getOrigHeight()+2)*zoom);
	    	image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
	    	g = image.createGraphics();
	    	g.setColor(Color.BLACK);
	    	mars.paintIcon(null, g, -px, -py);
	    	g.dispose();
			break;
		case JUPITER:
			Jupiter jupiter = new Jupiter();
			jupiter.width = (int) (jupiter.width*zoom);
			jupiter.height = (int) (jupiter.height*zoom);
	    	px = (int)((jupiter.getOrigX()-1)*zoom);
	    	py = (int)((jupiter.getOrigY()-1)*zoom);
	    	w = (int)((jupiter.getOrigWidth()+2)*zoom);
	    	h = (int)((jupiter.getOrigHeight()+2)*zoom);
	    	image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
	    	g = image.createGraphics();
	    	g.setColor(Color.BLACK);
	    	jupiter.paintIcon(null, g, -px, -py);
	    	g.dispose();
			break;
		case SATURN:
			Saturn saturn = new Saturn();
			saturn.width = (int) (saturn.width*zoom);
			saturn.height = (int) (saturn.height*zoom);
	    	px = (int)((saturn.getOrigX()-1)*zoom);
	    	py = (int)((saturn.getOrigY()-1)*zoom);
	    	w = (int)((saturn.getOrigWidth()+2)*zoom);
	    	h = (int)((saturn.getOrigHeight()+2)*zoom);
	    	image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
	    	g = image.createGraphics();
	    	g.setColor(Color.BLACK);
	    	saturn.paintIcon(null, g, -px, -py);
	    	g.dispose();
			break;
		case URANUS:
			Uranus uranus = new Uranus();
			uranus.width = (int) (uranus.width*zoom);
			uranus.height = (int) (uranus.height*zoom);
	    	px = (int)((uranus.getOrigX()-1)*zoom);
	    	py = (int)((uranus.getOrigY()-1)*zoom);
	    	w = (int)((uranus.getOrigWidth()+2)*zoom);
	    	h = (int)((uranus.getOrigHeight()+2)*zoom);
	    	image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
	    	g = image.createGraphics();
	    	g.setColor(Color.BLACK);
	    	uranus.paintIcon(null, g, -px, -py);
	    	g.dispose();
			break;
		case NEPTUNE:
			Neptune neptune = new Neptune();
			neptune.width = (int) (neptune.width*zoom);
			neptune.height = (int) (neptune.height*zoom);
	    	px = (int)((neptune.getOrigX()-1)*zoom);
	    	py = (int)((neptune.getOrigY()-1)*zoom);
	    	w = (int)((neptune.getOrigWidth()+2)*zoom);
	    	h = (int)((neptune.getOrigHeight()+2)*zoom);
	    	image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
	    	g = image.createGraphics();
	    	g.setColor(Color.BLACK);
	    	neptune.paintIcon(null, g, -px, -py);
	    	g.dispose();
			break;
		case Pluto:
			Pluto pluto = new Pluto();
			pluto.width = (int) (pluto.width*zoom);
			pluto.height = (int) (pluto.height*zoom);
	    	px = (int)((pluto.getOrigX()-1)*zoom);
	    	py = (int)((pluto.getOrigY()-1)*zoom);
	    	w = (int)((pluto.getOrigWidth()+2)*zoom);
	    	h = (int)((pluto.getOrigHeight()+2)*zoom);
	    	image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
	    	g = image.createGraphics();
	    	g.setColor(Color.BLACK);
	    	pluto.paintIcon(null, g, -px, -py);
	    	g.dispose();
			break;
		default:
			break;
		}
		return image;
	}
}

/**
 * This class has been automatically generated using svg2java
 *
 */
class Jupiter implements Icon {

	private float origAlpha = 1.0f;

	/**
	 * Paints the transcoded SVG image on the specified graphics context. You
	 * can install a custom transformation on the graphics context to scale the
	 * image.
	 *
	 * @param g
	 *            Graphics context.
	 */
	public void paint(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        origAlpha = 1.0f;
        Composite origComposite = g.getComposite();
        if (origComposite instanceof AlphaComposite) {
            AlphaComposite origAlphaComposite =
                (AlphaComposite)origComposite;
            if (origAlphaComposite.getRule() == AlphaComposite.SRC_OVER) {
                origAlpha = origAlphaComposite.getAlpha();
            }
        }

		// _0
		AffineTransform trans_0 = g.getTransform();
		paintRootGraphicsNode_0(g);
		g.setTransform(trans_0);

	}

	private static GeneralPath shape0 = null, shape1 = null, shape2 = null, shape3 = null;
	private void paintShapeNode_0_0_0_0(Graphics2D g) {
		if (shape0 == null) {
			shape0 = new GeneralPath();
			shape0.moveTo(358.5, 512.8622);
			shape0.lineTo(358.5, 551.8622);
		}
		//g.setPaint(new Color(0, 0, 0, 255));
		g.setStroke(new BasicStroke(2.0f,0,0,4.0f,null,0.0f));
		g.draw(shape0);
	}

	private void paintShapeNode_0_0_0_1(Graphics2D g) {
		if (shape1 == null) {
			shape1 = new GeneralPath();
			shape1.moveTo(364.5, 541.8622);
			shape1.lineTo(335.5, 541.8622);
		}
		g.draw(shape1);
	}

	private void paintShapeNode_0_0_0_2(Graphics2D g) {
		if (shape2 == null) {
			shape2 = new GeneralPath();
			shape2.moveTo(338.5, 526.8622);
			shape2.curveTo(337.5, 526.8622, 335.5, 525.8622, 335.5, 521.8622);
			shape2.curveTo(335.5, 517.8622, 339.5, 513.8622, 343.5, 513.8622);
			shape2.curveTo(347.5, 513.8622, 351.5, 516.8622, 351.5, 523.8622);
			shape2.curveTo(351.5, 530.8622, 346.5, 541.8622, 336.5, 541.8622);
		}
		g.draw(shape2);
	}

	private void paintShapeNode_0_0_0_3(Graphics2D g) {
		if (shape3 == null) {
			shape3 = new GeneralPath();
			shape3.moveTo(156.0, 109.5);
			shape3.curveTo(156.0, 109.776146, 155.77614, 110.0, 155.5, 110.0);
			shape3.curveTo(155.22386, 110.0, 155.0, 109.776146, 155.0, 109.5);
			shape3.curveTo(155.0, 109.223854, 155.22386, 109.0, 155.5, 109.0);
			shape3.curveTo(155.77614, 109.0, 156.0, 109.223854, 156.0, 109.5);
			shape3.closePath();
		}
		g.fill(shape3);
	}

	private void paintCompositeGraphicsNode_0_0_0(Graphics2D g) {
		// _0_0_0_0
		AffineTransform trans_0_0_0_0 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintShapeNode_0_0_0_0(g);
		g.setTransform(trans_0_0_0_0);
		// _0_0_0_1
		AffineTransform trans_0_0_0_1 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintShapeNode_0_0_0_1(g);
		g.setTransform(trans_0_0_0_1);
		// _0_0_0_2
		AffineTransform trans_0_0_0_2 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintShapeNode_0_0_0_2(g);
		g.setTransform(trans_0_0_0_2);
		// _0_0_0_3
		AffineTransform trans_0_0_0_3 = g.getTransform();
		g.transform(new AffineTransform(3.8543200492858887f, 0.0f, 0.0f, 3.9999799728393555f, -260.8468017578125f, 87.86438751220703f));
		paintShapeNode_0_0_0_3(g);
		g.setTransform(trans_0_0_0_3);
	}

	private void paintCanvasGraphicsNode_0_0(Graphics2D g) {
		// _0_0_0
		AffineTransform trans_0_0_0 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintCompositeGraphicsNode_0_0_0(g);
		g.setTransform(trans_0_0_0);
	}

	private void paintRootGraphicsNode_0(Graphics2D g) {
		// _0_0
		g.setComposite(AlphaComposite.getInstance(3, 1.0f * origAlpha));
		AffineTransform trans_0_0 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintCanvasGraphicsNode_0_0(g);
		g.setTransform(trans_0_0);
	}



    /**
     * Returns the X of the bounding box of the original SVG image.
     * @return The X of the bounding box of the original SVG image.
     */
    public int getOrigX() {
        return 335;
    }

    /**
     * Returns the Y of the bounding box of the original SVG image.
     * @return The Y of the bounding box of the original SVG image.
     */
    public int getOrigY() {
        return 513;
    }

    /**
     * Returns the width of the bounding box of the original SVG image.
     * @return The width of the bounding box of the original SVG image.
     */
    public int getOrigWidth() {
        return 30;
    }

    /**
     * Returns the height of the bounding box of the original SVG image.
     * @return The height of the bounding box of the original SVG image.
     */
    public int getOrigHeight() {
        return 39;
    }


	/**
	 * The current width of this resizable icon.
	 */
	int width;

	/**
	 * The current height of this resizable icon.
	 */
	int height;

	/**
	 * Creates a new transcoded SVG image.
	 */
	public Jupiter() {
        this.width = getOrigWidth();
        this.height = getOrigHeight();
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.Icon#getIconHeight()
	 */
    @Override
	public int getIconHeight() {
		return height;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.Icon#getIconWidth()
	 */
    @Override
	public int getIconWidth() {
		return width;
	}

	/*
	 * Set the dimension of the icon.
	 */

	public void setDimension(Dimension newDimension) {
		this.width = newDimension.width;
		this.height = newDimension.height;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.Icon#paintIcon(java.awt.Component, java.awt.Graphics, int, int)
	 */
    @Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		Graphics2D g2d = (Graphics2D) g.create();
		g2d.translate(x, y);

		double coef1 = (double) this.width / (double) getOrigWidth();
		double coef2 = (double) this.height / (double) getOrigHeight();
		double coef = Math.min(coef1, coef2);
		g2d.scale(coef, coef);
		paint(g2d);
		g2d.dispose();
	}
}

/**
 * This class has been automatically generated using svg2java
 *
 */
class Mars implements Icon {

	private float origAlpha = 1.0f;

	/**
	 * Paints the transcoded SVG image on the specified graphics context. You
	 * can install a custom transformation on the graphics context to scale the
	 * image.
	 *
	 * @param g
	 *            Graphics context.
	 */
	public void paint(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        origAlpha = 1.0f;
        Composite origComposite = g.getComposite();
        if (origComposite instanceof AlphaComposite) {
            AlphaComposite origAlphaComposite =
                (AlphaComposite)origComposite;
            if (origAlphaComposite.getRule() == AlphaComposite.SRC_OVER) {
                origAlpha = origAlphaComposite.getAlpha();
            }
        }

		// _0
		AffineTransform trans_0 = g.getTransform();
		paintRootGraphicsNode_0(g);
		g.setTransform(trans_0);

	}

	private static GeneralPath shape0 = null, shape1 = null, shape2 = null;
	private void paintShapeNode_0_0_0_0(Graphics2D g) {
		if (shape0 == null) {
			shape0 = new GeneralPath();
			shape0.moveTo(110.0, 115.5);
			shape0.curveTo(110.0, 121.85127, 104.85127, 127.0, 98.5, 127.0);
			shape0.curveTo(92.14873, 127.0, 87.0, 121.85127, 87.0, 115.5);
			shape0.curveTo(87.0, 109.14873, 92.14873, 104.0, 98.5, 104.0);
			shape0.curveTo(104.85127, 104.0, 110.0, 109.14873, 110.0, 115.5);
			shape0.closePath();
		}
		//g.setPaint(new Color(0, 0, 0, 255));
		g.setStroke(new BasicStroke(1.9230769f,0,0,4.0f,null,0.0f));
		g.draw(shape0);
	}

	private void paintShapeNode_0_0_0_1(Graphics2D g) {
		if (shape1 == null) {
			shape1 = new GeneralPath();
			shape1.moveTo(356.8659, 526.97363);
			shape1.lineTo(365.72516, 522.3354);
		}
		g.setStroke(new BasicStroke(1.9999993f,0,0,4.0f,null,0.0f));
		g.draw(shape1);
	}

	private void paintShapeNode_0_0_0_2(Graphics2D g) {
		if (shape2 == null) {
			shape2 = new GeneralPath();
			shape2.moveTo(358.9764, 520.22485);
			shape2.lineTo(365.72516, 522.3353);
			shape2.lineTo(363.61465, 529.0841);
		}
		g.draw(shape2);
	}

	private void paintCompositeGraphicsNode_0_0_0(Graphics2D g) {
		// _0_0_0_0
		AffineTransform trans_0_0_0_0 = g.getTransform();
		g.transform(new AffineTransform(1.0399999618530273f, 0.0f, 0.0f, 1.0399999618530273f, 243.7948455810547f, 412.4195251464844f));
		paintShapeNode_0_0_0_0(g);
		g.setTransform(trans_0_0_0_0);
		// _0_0_0_1
		AffineTransform trans_0_0_0_1 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintShapeNode_0_0_0_1(g);
		g.setTransform(trans_0_0_0_1);
		// _0_0_0_2
		AffineTransform trans_0_0_0_2 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintShapeNode_0_0_0_2(g);
		g.setTransform(trans_0_0_0_2);
	}

	private void paintCanvasGraphicsNode_0_0(Graphics2D g) {
		// _0_0_0
		AffineTransform trans_0_0_0 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintCompositeGraphicsNode_0_0_0(g);
		g.setTransform(trans_0_0_0);
	}

	private void paintRootGraphicsNode_0(Graphics2D g) {
		// _0_0
		g.setComposite(AlphaComposite.getInstance(3, 1.0f * origAlpha));
		AffineTransform trans_0_0 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintCanvasGraphicsNode_0_0(g);
		g.setTransform(trans_0_0);
	}



    /**
     * Returns the X of the bounding box of the original SVG image.
     * @return The X of the bounding box of the original SVG image.
     */
    public int getOrigX() {
        return 334;
    }

    /**
     * Returns the Y of the bounding box of the original SVG image.
     * @return The Y of the bounding box of the original SVG image.
     */
    public int getOrigY() {
        return 520;
    }

    /**
     * Returns the width of the bounding box of the original SVG image.
     * @return The width of the bounding box of the original SVG image.
     */
    public int getOrigWidth() {
        return 34;
    }

    /**
     * Returns the height of the bounding box of the original SVG image.
     * @return The height of the bounding box of the original SVG image.
     */
    public int getOrigHeight() {
        return 27;
    }


	/**
	 * The current width of this resizable icon.
	 */
	int width;

	/**
	 * The current height of this resizable icon.
	 */
	int height;

	/**
	 * Creates a new transcoded SVG image.
	 */
	public Mars() {
        this.width = getOrigWidth();
        this.height = getOrigHeight();
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.Icon#getIconHeight()
	 */
    @Override
	public int getIconHeight() {
		return height;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.Icon#getIconWidth()
	 */
    @Override
	public int getIconWidth() {
		return width;
	}

	/*
	 * Set the dimension of the icon.
	 */

	public void setDimension(Dimension newDimension) {
		this.width = newDimension.width;
		this.height = newDimension.height;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.Icon#paintIcon(java.awt.Component, java.awt.Graphics, int, int)
	 */
    @Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		Graphics2D g2d = (Graphics2D) g.create();
		g2d.translate(x, y);

		double coef1 = (double) this.width / (double) getOrigWidth();
		double coef2 = (double) this.height / (double) getOrigHeight();
		double coef = Math.min(coef1, coef2);
		g2d.scale(coef, coef);
		paint(g2d);
		g2d.dispose();
	}
}

/**
 * This class has been automatically generated using svg2java
 *
 */
class Mercury implements Icon {

	private float origAlpha = 1.0f;

	/**
	 * Paints the transcoded SVG image on the specified graphics context. You
	 * can install a custom transformation on the graphics context to scale the
	 * image.
	 *
	 * @param g
	 *            Graphics context.
	 */
	public void paint(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        origAlpha = 1.0f;
        Composite origComposite = g.getComposite();
        if (origComposite instanceof AlphaComposite) {
            AlphaComposite origAlphaComposite =
                (AlphaComposite)origComposite;
            if (origAlphaComposite.getRule() == AlphaComposite.SRC_OVER) {
                origAlpha = origAlphaComposite.getAlpha();
            }
        }

		// _0
		AffineTransform trans_0 = g.getTransform();
		paintRootGraphicsNode_0(g);
		g.setTransform(trans_0);

	}

	private static GeneralPath shape0 = null, shape1 = null, shape2 = null, shape3 = null;
	private void paintShapeNode_0_0_0_0(Graphics2D g) {
		if (shape0 == null) {
			shape0 = new GeneralPath();
			shape0.moveTo(112.0, 36.5);
			shape0.curveTo(112.0, 42.851276, 106.85127, 48.0, 100.5, 48.0);
			shape0.curveTo(94.14873, 48.0, 89.0, 42.851276, 89.0, 36.5);
			shape0.curveTo(89.0, 30.148726, 94.14873, 25.0, 100.5, 25.0);
			shape0.curveTo(106.85127, 25.0, 112.0, 30.148726, 112.0, 36.5);
			shape0.closePath();
		}
		//g.setPaint(new Color(0, 0, 0, 255));
		g.setStroke(new BasicStroke(2.0833333f,0,0,4.0f,null,0.0f));
		g.draw(shape0);
	}

	private void paintShapeNode_0_0_0_1(Graphics2D g) {
		if (shape1 == null) {
			shape1 = new GeneralPath();
			shape1.moveTo(111.9469, 37.603863);
			shape1.curveTo(111.37808, 43.502377, 106.42019, 48.002922, 100.49431, 47.99999);
			shape1.curveTo(94.568436, 47.997055, 89.615, 43.491604, 89.05202, 37.592533);
		}
		g.draw(shape1);
	}

	private void paintShapeNode_0_0_0_2(Graphics2D g) {
		if (shape2 == null) {
			shape2 = new GeneralPath();
			shape2.moveTo(350.0, 542.8578);
			shape2.lineTo(350.0, 554.8578);
		}
		g.setStroke(new BasicStroke(2.0f,0,0,4.0f,null,0.0f));
		g.draw(shape2);
	}

	private void paintShapeNode_0_0_0_3(Graphics2D g) {
		if (shape3 == null) {
			shape3 = new GeneralPath();
			shape3.moveTo(345.0, 549.8578);
			shape3.lineTo(355.0, 549.8578);
		}
		g.draw(shape3);
	}

	private void paintCompositeGraphicsNode_0_0_0(Graphics2D g) {
		// _0_0_0_0
		AffineTransform trans_0_0_0_0 = g.getTransform();
		g.transform(new AffineTransform(0.9599999785423279f, 0.0f, 0.0f, 0.9599999785423279f, 253.52000427246094f, 496.8177795410156f));
		paintShapeNode_0_0_0_0(g);
		g.setTransform(trans_0_0_0_0);
		// _0_0_0_1
		AffineTransform trans_0_0_0_1 = g.getTransform();
		g.transform(new AffineTransform(0.9599999785423279f, 0.0f, 0.0f, 0.9599999785423279f, 253.55999755859375f, 473.77777099609375f));
		paintShapeNode_0_0_0_1(g);
		g.setTransform(trans_0_0_0_1);
		// _0_0_0_2
		AffineTransform trans_0_0_0_2 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintShapeNode_0_0_0_2(g);
		g.setTransform(trans_0_0_0_2);
		// _0_0_0_3
		AffineTransform trans_0_0_0_3 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintShapeNode_0_0_0_3(g);
		g.setTransform(trans_0_0_0_3);
	}

	private void paintCanvasGraphicsNode_0_0(Graphics2D g) {
		// _0_0_0
		AffineTransform trans_0_0_0 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintCompositeGraphicsNode_0_0_0(g);
		g.setTransform(trans_0_0_0);
	}

	private void paintRootGraphicsNode_0(Graphics2D g) {
		// _0_0
		g.setComposite(AlphaComposite.getInstance(3, 1.0f * origAlpha));
		AffineTransform trans_0_0 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintCanvasGraphicsNode_0_0(g);
		g.setTransform(trans_0_0);
	}



    /**
     * Returns the X of the bounding box of the original SVG image.
     * @return The X of the bounding box of the original SVG image.
     */
    public int getOrigX() {
        return 338;
    }

    /**
     * Returns the Y of the bounding box of the original SVG image.
     * @return The Y of the bounding box of the original SVG image.
     */
    public int getOrigY() {
        return 510;
    }

    /**
     * Returns the width of the bounding box of the original SVG image.
     * @return The width of the bounding box of the original SVG image.
     */
    public int getOrigWidth() {
        return 25;
    }

    /**
     * Returns the height of the bounding box of the original SVG image.
     * @return The height of the bounding box of the original SVG image.
     */
    public int getOrigHeight() {
        return 46;
    }


	/**
	 * The current width of this resizable icon.
	 */
	int width;

	/**
	 * The current height of this resizable icon.
	 */
	int height;

	/**
	 * Creates a new transcoded SVG image.
	 */
	public Mercury() {
        this.width = getOrigWidth();
        this.height = getOrigHeight();
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.Icon#getIconHeight()
	 */
    @Override
	public int getIconHeight() {
		return height;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.Icon#getIconWidth()
	 */
    @Override
	public int getIconWidth() {
		return width;
	}

	/*
	 * Set the dimension of the icon.
	 */

	public void setDimension(Dimension newDimension) {
		this.width = newDimension.width;
		this.height = newDimension.height;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.Icon#paintIcon(java.awt.Component, java.awt.Graphics, int, int)
	 */
    @Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		Graphics2D g2d = (Graphics2D) g.create();
		g2d.translate(x, y);

		double coef1 = (double) this.width / (double) getOrigWidth();
		double coef2 = (double) this.height / (double) getOrigHeight();
		double coef = Math.min(coef1, coef2);
		g2d.scale(coef, coef);
		paint(g2d);
		g2d.dispose();
	}
}

/**
 * This class has been automatically generated using svg2java
 *
 */
class Neptune implements Icon {

	private float origAlpha = 1.0f;

	/**
	 * Paints the transcoded SVG image on the specified graphics context. You
	 * can install a custom transformation on the graphics context to scale the
	 * image.
	 *
	 * @param g
	 *            Graphics context.
	 */
	public void paint(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        origAlpha = 1.0f;
        Composite origComposite = g.getComposite();
        if (origComposite instanceof AlphaComposite) {
            AlphaComposite origAlphaComposite =
                (AlphaComposite)origComposite;
            if (origAlphaComposite.getRule() == AlphaComposite.SRC_OVER) {
                origAlpha = origAlphaComposite.getAlpha();
            }
        }

		// _0
		AffineTransform trans_0 = g.getTransform();
		paintRootGraphicsNode_0(g);
		g.setTransform(trans_0);

	}

	private static GeneralPath shape0 = null, shape1 = null, shape2 = null;
	private void paintShapeNode_0_0_0_0(Graphics2D g) {
		if (shape0 == null) {
			shape0 = new GeneralPath();
			shape0.moveTo(338.0, 512.8622);
			shape0.curveTo(335.0, 532.8622, 340.0, 535.8622, 350.0, 535.8622);
			shape0.curveTo(360.0, 535.8622, 365.0, 532.8622, 362.0, 512.8622);
		}
		//g.setPaint(new Color(0, 0, 0, 255));
		g.setStroke(new BasicStroke(2.0f,0,0,4.0f,null,0.0f));
		g.draw(shape0);
	}

	private void paintShapeNode_0_0_0_1(Graphics2D g) {
		if (shape1 == null) {
			shape1 = new GeneralPath();
			shape1.moveTo(350.0, 514.8622);
			shape1.lineTo(350.0, 551.8622);
		}
		g.draw(shape1);
	}

	private void paintShapeNode_0_0_0_2(Graphics2D g) {
		if (shape2 == null) {
			shape2 = new GeneralPath();
			shape2.moveTo(342.0, 543.8622);
			shape2.lineTo(358.0, 543.8622);
		}
		g.draw(shape2);
	}

	private void paintCompositeGraphicsNode_0_0_0(Graphics2D g) {
		AffineTransform trans_0_0_0_0 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintShapeNode_0_0_0_0(g);
		g.setTransform(trans_0_0_0_0);
		// _0_0_0_1
		AffineTransform trans_0_0_0_1 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintShapeNode_0_0_0_1(g);
		g.setTransform(trans_0_0_0_1);
		// _0_0_0_2
		AffineTransform trans_0_0_0_2 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintShapeNode_0_0_0_2(g);
		g.setTransform(trans_0_0_0_2);
	}

	private void paintCanvasGraphicsNode_0_0(Graphics2D g) {
		// _0_0_0
		AffineTransform trans_0_0_0 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintCompositeGraphicsNode_0_0_0(g);
		g.setTransform(trans_0_0_0);
	}

	private void paintRootGraphicsNode_0(Graphics2D g) {
		// _0_0
		g.setComposite(AlphaComposite.getInstance(3, 1.0f * origAlpha));
		AffineTransform trans_0_0 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintCanvasGraphicsNode_0_0(g);
		g.setTransform(trans_0_0);
	}



    /**
     * Returns the X of the bounding box of the original SVG image.
     * @return The X of the bounding box of the original SVG image.
     */
    public int getOrigX() {
        return 336;
    }

    /**
     * Returns the Y of the bounding box of the original SVG image.
     * @return The Y of the bounding box of the original SVG image.
     */
    public int getOrigY() {
        return 513;
    }

    /**
     * Returns the width of the bounding box of the original SVG image.
     * @return The width of the bounding box of the original SVG image.
     */
    public int getOrigWidth() {
        return 29;
    }

    /**
     * Returns the height of the bounding box of the original SVG image.
     * @return The height of the bounding box of the original SVG image.
     */
    public int getOrigHeight() {
        return 40;
    }


	/**
	 * The current width of this resizable icon.
	 */
	int width;

	/**
	 * The current height of this resizable icon.
	 */
	int height;

	/**
	 * Creates a new transcoded SVG image.
	 */
	public Neptune() {
        this.width = getOrigWidth();
        this.height = getOrigHeight();
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.Icon#getIconHeight()
	 */
    @Override
	public int getIconHeight() {
		return height;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.Icon#getIconWidth()
	 */
    @Override
	public int getIconWidth() {
		return width;
	}

	/*
	 * Set the dimension of the icon.
	 */

	public void setDimension(Dimension newDimension) {
		this.width = newDimension.width;
		this.height = newDimension.height;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.Icon#paintIcon(java.awt.Component, java.awt.Graphics, int, int)
	 */
    @Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		Graphics2D g2d = (Graphics2D) g.create();
		g2d.translate(x, y);

		double coef1 = (double) this.width / (double) getOrigWidth();
		double coef2 = (double) this.height / (double) getOrigHeight();
		double coef = Math.min(coef1, coef2);
		g2d.scale(coef, coef);
		paint(g2d);
		g2d.dispose();
	}
}

/**
 * This class has been automatically generated using svg2java
 *
 */
class Pluto implements Icon {

	private float origAlpha = 1.0f;

	/**
	 * Paints the transcoded SVG image on the specified graphics context. You
	 * can install a custom transformation on the graphics context to scale the
	 * image.
	 *
	 * @param g
	 *            Graphics context.
	 */
	public void paint(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        origAlpha = 1.0f;
        Composite origComposite = g.getComposite();
        if (origComposite instanceof AlphaComposite) {
            AlphaComposite origAlphaComposite =
                (AlphaComposite)origComposite;
            if (origAlphaComposite.getRule() == AlphaComposite.SRC_OVER) {
                origAlpha = origAlphaComposite.getAlpha();
            }
        }

		// _0
		AffineTransform trans_0 = g.getTransform();
		paintRootGraphicsNode_0(g);
		g.setTransform(trans_0);

	}

	private static GeneralPath shape0 = null, shape1 = null, shape2 = null, shape3 = null;
	private void paintShapeNode_0_0_0_0(Graphics2D g) {
		if (shape0 == null) {
			shape0 = new GeneralPath();
			shape0.moveTo(342.0, 542.3622);
			shape0.lineTo(358.0, 542.3622);
		}
		//g.setPaint(new Color(0, 0, 0, 255));
		g.setStroke(new BasicStroke(2.0f,0,0,4.0f,null,0.0f));
		g.draw(shape0);
	}

	private void paintShapeNode_0_0_0_1(Graphics2D g) {
		if (shape1 == null) {
			shape1 = new GeneralPath();
			shape1.moveTo(350.0, 550.3622);
			shape1.lineTo(350.0, 533.3622);
		}
		g.draw(shape1);
	}

	private void paintShapeNode_0_0_0_2(Graphics2D g) {
		if (shape3 == null) {
			shape2 = new GeneralPath();
			shape2.moveTo(172.0, 184.0);
			shape2.curveTo(172.0, 187.866, 168.866, 191.0, 165.0, 191.0);
			shape2.curveTo(161.134, 191.0, 158.0, 187.866, 158.0, 184.0);
			shape2.curveTo(158.0, 180.134, 161.134, 177.0, 165.0, 177.0);
			shape2.curveTo(168.866, 177.0, 172.0, 180.134, 172.0, 184.0);
			shape2.closePath();
		}
		g.draw(shape2);
	}

	private void paintShapeNode_0_0_0_3(Graphics2D g) {
		if (shape3 == null) {
			shape3 = new GeneralPath();
			shape3.moveTo(177.0, 184.0);
			shape3.curveTo(177.0, 190.62741, 171.62741, 196.0, 165.0, 196.0);
			shape3.curveTo(158.37259, 196.0, 153.0, 190.62741, 153.0, 184.0);
		}
		g.draw(shape3);
	}

	private void paintCompositeGraphicsNode_0_0_0(Graphics2D g) {
		// _0_0_0_0
		AffineTransform trans_0_0_0_0 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintShapeNode_0_0_0_0(g);
		g.setTransform(trans_0_0_0_0);
		// _0_0_0_1
		AffineTransform trans_0_0_0_1 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintShapeNode_0_0_0_1(g);
		g.setTransform(trans_0_0_0_1);
		// _0_0_0_2
		AffineTransform trans_0_0_0_2 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 185.0f, 337.3621826171875f));
		paintShapeNode_0_0_0_2(g);
		g.setTransform(trans_0_0_0_2);
		// _0_0_0_3
		AffineTransform trans_0_0_0_3 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 185.0f, 337.3621826171875f));
		paintShapeNode_0_0_0_3(g);
		g.setTransform(trans_0_0_0_3);
	}

	private void paintCanvasGraphicsNode_0_0(Graphics2D g) {
		// _0_0_0
		AffineTransform trans_0_0_0 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintCompositeGraphicsNode_0_0_0(g);
		g.setTransform(trans_0_0_0);
	}

	private void paintRootGraphicsNode_0(Graphics2D g) {
		// _0_0
		g.setComposite(AlphaComposite.getInstance(3, 1.0f * origAlpha));
		AffineTransform trans_0_0 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintCanvasGraphicsNode_0_0(g);
		g.setTransform(trans_0_0);
	}



    /**
     * Returns the X of the bounding box of the original SVG image.
     * @return The X of the bounding box of the original SVG image.
     */
    public int getOrigX() {
        return 337;
    }

    /**
     * Returns the Y of the bounding box of the original SVG image.
     * @return The Y of the bounding box of the original SVG image.
     */
    public int getOrigY() {
        return 514;
    }

    /**
     * Returns the width of the bounding box of the original SVG image.
     * @return The width of the bounding box of the original SVG image.
     */
    public int getOrigWidth() {
        return 26;
    }

    /**
     * Returns the height of the bounding box of the original SVG image.
     * @return The height of the bounding box of the original SVG image.
     */
    public int getOrigHeight() {
        return 37;
    }


	/**
	 * The current width of this resizable icon.
	 */
	int width;

	/**
	 * The current height of this resizable icon.
	 */
	int height;

	/**
	 * Creates a new transcoded SVG image.
	 */
	public Pluto() {
        this.width = getOrigWidth();
        this.height = getOrigHeight();
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.Icon#getIconHeight()
	 */
    @Override
	public int getIconHeight() {
		return height;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.Icon#getIconWidth()
	 */
    @Override
	public int getIconWidth() {
		return width;
	}

	/*
	 * Set the dimension of the icon.
	 */

	public void setDimension(Dimension newDimension) {
		this.width = newDimension.width;
		this.height = newDimension.height;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.Icon#paintIcon(java.awt.Component, java.awt.Graphics, int, int)
	 */
    @Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		Graphics2D g2d = (Graphics2D) g.create();
		g2d.translate(x, y);

		double coef1 = (double) this.width / (double) getOrigWidth();
		double coef2 = (double) this.height / (double) getOrigHeight();
		double coef = Math.min(coef1, coef2);
		g2d.scale(coef, coef);
		paint(g2d);
		g2d.dispose();
	}
}

/**
 * This class has been automatically generated using svg2java
 *
 */
class Saturn implements Icon {

	private float origAlpha = 1.0f;

	/**
	 * Paints the transcoded SVG image on the specified graphics context. You
	 * can install a custom transformation on the graphics context to scale the
	 * image.
	 *
	 * @param g
	 *            Graphics context.
	 */
	public void paint(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        origAlpha = 1.0f;
        Composite origComposite = g.getComposite();
        if (origComposite instanceof AlphaComposite) {
            AlphaComposite origAlphaComposite =
                (AlphaComposite)origComposite;
            if (origAlphaComposite.getRule() == AlphaComposite.SRC_OVER) {
                origAlpha = origAlphaComposite.getAlpha();
            }
        }

		// _0
		AffineTransform trans_0 = g.getTransform();
		paintRootGraphicsNode_0(g);
		g.setTransform(trans_0);

	}

	private static GeneralPath shape0 = null, shape1 = null, shape2 = null;
	private void paintShapeNode_0_0_0_0(Graphics2D g) {
		if (shape0 == null) {
			shape0 = new GeneralPath();
			shape0.moveTo(344.5, 513.8622);
			shape0.lineTo(344.5, 542.8622);
		}
		//g.setPaint(new Color(0, 0, 0, 255));
		g.setStroke(new BasicStroke(2.0f,0,0,4.0f,null,0.0f));
		g.draw(shape0);
	}

	private void paintShapeNode_0_0_0_1(Graphics2D g) {
		if (shape1 == null) {
			shape1 = new GeneralPath();
			shape1.moveTo(340.5, 519.8622);
			shape1.lineTo(352.5, 519.8622);
		}
		g.draw(shape1);
	}

	private void paintShapeNode_0_0_0_2(Graphics2D g) {
		if (shape2 == null) {
			shape2 = new GeneralPath();
			shape2.moveTo(358.5, 548.8622);
			shape2.curveTo(357.5, 549.8622, 356.5, 550.8622, 355.5, 550.8622);
			shape2.curveTo(354.5, 550.8622, 352.5, 549.8622, 352.5, 547.8622);
			shape2.curveTo(352.5, 545.8622, 353.5, 543.8622, 355.5, 541.8622);
			shape2.curveTo(357.5, 539.8622, 359.5, 535.8622, 359.5, 531.8622);
			shape2.curveTo(359.5, 527.8622, 357.5, 523.8622, 353.5, 523.8622);
			shape2.curveTo(349.7168, 523.8622, 346.5, 525.8622, 344.5, 529.8622);
		}
		g.draw(shape2);
	}

	private void paintCompositeGraphicsNode_0_0_0(Graphics2D g) {
		// _0_0_0_0
		AffineTransform trans_0_0_0_0 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintShapeNode_0_0_0_0(g);
		g.setTransform(trans_0_0_0_0);
		// _0_0_0_1
		AffineTransform trans_0_0_0_1 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintShapeNode_0_0_0_1(g);
		g.setTransform(trans_0_0_0_1);
		// _0_0_0_2
		AffineTransform trans_0_0_0_2 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintShapeNode_0_0_0_2(g);
		g.setTransform(trans_0_0_0_2);
	}

	private void paintCanvasGraphicsNode_0_0(Graphics2D g) {
		// _0_0_0
		AffineTransform trans_0_0_0 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintCompositeGraphicsNode_0_0_0(g);
		g.setTransform(trans_0_0_0);
	}

	private void paintRootGraphicsNode_0(Graphics2D g) {
		// _0_0
		g.setComposite(AlphaComposite.getInstance(3, 1.0f * origAlpha));
		AffineTransform trans_0_0 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintCanvasGraphicsNode_0_0(g);
		g.setTransform(trans_0_0);
	}



    /**
     * Returns the X of the bounding box of the original SVG image.
     * @return The X of the bounding box of the original SVG image.
     */
    public int getOrigX() {
        return 341;
    }

    /**
     * Returns the Y of the bounding box of the original SVG image.
     * @return The Y of the bounding box of the original SVG image.
     */
    public int getOrigY() {
        return 514;
    }

    /**
     * Returns the width of the bounding box of the original SVG image.
     * @return The width of the bounding box of the original SVG image.
     */
    public int getOrigWidth() {
        return 20;
    }

    /**
     * Returns the height of the bounding box of the original SVG image.
     * @return The height of the bounding box of the original SVG image.
     */
    public int getOrigHeight() {
        return 38;
    }


	/**
	 * The current width of this resizable icon.
	 */
	int width;

	/**
	 * The current height of this resizable icon.
	 */
	int height;

	/**
	 * Creates a new transcoded SVG image.
	 */
	public Saturn() {
        this.width = getOrigWidth();
        this.height = getOrigHeight();
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.Icon#getIconHeight()
	 */
    @Override
	public int getIconHeight() {
		return height;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.Icon#getIconWidth()
	 */
    @Override
	public int getIconWidth() {
		return width;
	}

	/*
	 * Set the dimension of the icon.
	 */

	public void setDimension(Dimension newDimension) {
		this.width = newDimension.width;
		this.height = newDimension.height;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.Icon#paintIcon(java.awt.Component, java.awt.Graphics, int, int)
	 */
    @Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		Graphics2D g2d = (Graphics2D) g.create();
		g2d.translate(x, y);

		double coef1 = (double) this.width / (double) getOrigWidth();
		double coef2 = (double) this.height / (double) getOrigHeight();
		double coef = Math.min(coef1, coef2);
		g2d.scale(coef, coef);
		paint(g2d);
		g2d.dispose();
	}
}

/**
 * This class has been automatically generated using svg2java
 *
 */
class Sun implements Icon {

	private float origAlpha = 1.0f;

	/**
	 * Paints the transcoded SVG image on the specified graphics context. You
	 * can install a custom transformation on the graphics context to scale the
	 * image.
	 *
	 * @param g
	 *            Graphics context.
	 */
	public void paint(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        origAlpha = 1.0f;
        Composite origComposite = g.getComposite();
        if (origComposite instanceof AlphaComposite) {
            AlphaComposite origAlphaComposite =
                (AlphaComposite)origComposite;
            if (origAlphaComposite.getRule() == AlphaComposite.SRC_OVER) {
                origAlpha = origAlphaComposite.getAlpha();
            }
        }

		// _0
		AffineTransform trans_0 = g.getTransform();
		paintRootGraphicsNode_0(g);
		g.setTransform(trans_0);

	}

	private static GeneralPath shape0 = null, shape1 = null;
	private void paintShapeNode_0_0_0_0(Graphics2D g) {
		if (shape0 == null) {
			shape0 = new GeneralPath();
			shape0.moveTo(49.396477, 36.70454);
			shape0.curveTo(49.396477, 45.71735, 42.40141, 53.023674, 33.772552, 53.023674);
			shape0.curveTo(25.1437, 53.023674, 18.148632, 45.71735, 18.148632, 36.70454);
			shape0.curveTo(18.148632, 27.691732, 25.1437, 20.385406, 33.772552, 20.385406);
			shape0.curveTo(42.40141, 20.385406, 49.396477, 27.691732, 49.396477, 36.70454);
			shape0.closePath();
		}
		//g.setPaint(new Color(0, 0, 0, 255));
		g.setStroke(new BasicStroke(1.9984733f,0,0,4.0f,null,0.0f));
		g.draw(shape0);
	}

	private void paintShapeNode_0_0_0_1(Graphics2D g) {
		if (shape1 == null) {
			shape1 = new GeneralPath();
			shape1.moveTo(38.0, 37.0);
			shape1.curveTo(38.0, 39.761425, 36.20914, 42.0, 34.0, 42.0);
			shape1.curveTo(31.790861, 42.0, 30.0, 39.761425, 30.0, 37.0);
			shape1.curveTo(30.0, 34.238575, 31.790861, 32.0, 34.0, 32.0);
			shape1.curveTo(36.20914, 32.0, 38.0, 34.238575, 38.0, 37.0);
			shape1.closePath();
		}
		g.fill(shape1);
	}

	private void paintCompositeGraphicsNode_0_0_0(Graphics2D g) {
		// _0_0_0_0
		AffineTransform trans_0_0_0_0 = g.getTransform();
		g.transform(new AffineTransform(1.022387981414795f, 0.0f, 0.0f, 0.9795969724655151f, 315.4713439941406f, 496.4065246582031f));
		paintShapeNode_0_0_0_0(g);
		g.setTransform(trans_0_0_0_0);
		// _0_0_0_1
		AffineTransform trans_0_0_0_1 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 0.800000011920929f, 316.0036315917969f, 502.77716064453125f));
		paintShapeNode_0_0_0_1(g);
		g.setTransform(trans_0_0_0_1);
	}

	private void paintCanvasGraphicsNode_0_0(Graphics2D g) {
		// _0_0_0
		AffineTransform trans_0_0_0 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintCompositeGraphicsNode_0_0_0(g);
		g.setTransform(trans_0_0_0);
	}

	private void paintRootGraphicsNode_0(Graphics2D g) {
		// _0_0
		g.setComposite(AlphaComposite.getInstance(3, 1.0f * origAlpha));
		AffineTransform trans_0_0 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintCanvasGraphicsNode_0_0(g);
		g.setTransform(trans_0_0);
	}



    /**
     * Returns the X of the bounding box of the original SVG image.
     * @return The X of the bounding box of the original SVG image.
     */
    public int getOrigX() {
        return 334;
    }

    /**
     * Returns the Y of the bounding box of the original SVG image.
     * @return The Y of the bounding box of the original SVG image.
     */
    public int getOrigY() {
        return 516;
    }

    /**
     * Returns the width of the bounding box of the original SVG image.
     * @return The width of the bounding box of the original SVG image.
     */
    public int getOrigWidth() {
        return 34;
    }

    /**
     * Returns the height of the bounding box of the original SVG image.
     * @return The height of the bounding box of the original SVG image.
     */
    public int getOrigHeight() {
        return 34;
    }


	/**
	 * The current width of this resizable icon.
	 */
	int width;

	/**
	 * The current height of this resizable icon.
	 */
	int height;

	/**
	 * Creates a new transcoded SVG image.
	 */
	public Sun() {
        this.width = getOrigWidth();
        this.height = getOrigHeight();
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.Icon#getIconHeight()
	 */
    @Override
	public int getIconHeight() {
		return height;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.Icon#getIconWidth()
	 */
    @Override
	public int getIconWidth() {
		return width;
	}

	/*
	 * Set the dimension of the icon.
	 */

	public void setDimension(Dimension newDimension) {
		this.width = newDimension.width;
		this.height = newDimension.height;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.Icon#paintIcon(java.awt.Component, java.awt.Graphics, int, int)
	 */
    @Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		Graphics2D g2d = (Graphics2D) g.create();
		g2d.translate(x, y);

		double coef1 = (double) this.width / (double) getOrigWidth();
		double coef2 = (double) this.height / (double) getOrigHeight();
		double coef = Math.min(coef1, coef2);
		g2d.scale(coef, coef);
		paint(g2d);
		g2d.dispose();
	}
}

/**
 * This class has been automatically generated using svg2java
 *
 */
class Earth implements Icon {

	private float origAlpha = 1.0f;

	/**
	 * Paints the transcoded SVG image on the specified graphics context. You
	 * can install a custom transformation on the graphics context to scale the
	 * image.
	 *
	 * @param g
	 *            Graphics context.
	 */
	public void paint(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        origAlpha = 1.0f;
        Composite origComposite = g.getComposite();
        if (origComposite instanceof AlphaComposite) {
            AlphaComposite origAlphaComposite =
                (AlphaComposite)origComposite;
            if (origAlphaComposite.getRule() == AlphaComposite.SRC_OVER) {
                origAlpha = origAlphaComposite.getAlpha();
            }
        }

		// _0
		AffineTransform trans_0 = g.getTransform();
		paintRootGraphicsNode_0(g);
		g.setTransform(trans_0);

	}

	private static GeneralPath shape0 = null, shape1 = null, shape2 = null;
	private void paintShapeNode_0_0_0_0(Graphics2D g) {
		if (shape0 == null) {
			shape0 = new GeneralPath();
			shape0.moveTo(49.0, 116.5);
			shape0.curveTo(49.0, 125.06042, 42.060413, 132.0, 33.5, 132.0);
			shape0.curveTo(24.939587, 132.0, 18.0, 125.06042, 18.0, 116.5);
			shape0.curveTo(18.0, 107.93958, 24.939587, 101.0, 33.5, 101.0);
			shape0.curveTo(42.060413, 101.0, 49.0, 107.93958, 49.0, 116.5);
			shape0.closePath();
		}
		//g.setPaint(new Color(0, 0, 0, 255));
		g.setStroke(new BasicStroke(1.9411764f,0,0,4.0f,null,0.0f));
		g.draw(shape0);
	}

	private void paintShapeNode_0_0_0_1(Graphics2D g) {
		if (shape1 == null) {
			shape1 = new GeneralPath();
			shape1.moveTo(334.0, 532.3622);
			shape1.lineTo(366.0, 532.3622);
		}
		g.setStroke(new BasicStroke(2.0f,0,0,4.0f,null,0.0f));
		g.draw(shape1);
	}

	private void paintShapeNode_0_0_0_2(Graphics2D g) {
		if (shape2 == null) {
			shape2 = new GeneralPath();
			shape2.moveTo(350.0, 516.3622);
			shape2.lineTo(350.0, 548.3622);
		}
		g.draw(shape2);
	}

	private void paintCompositeGraphicsNode_0_0_0(Graphics2D g) {
		// _0_0_0_0
		AffineTransform trans_0_0_0_0 = g.getTransform();
		g.transform(new AffineTransform(1.0303030014038086f, 0.0f, 0.0f, 1.0303030014038086f, 315.48486328125f, 412.3318786621094f));
		paintShapeNode_0_0_0_0(g);
		g.setTransform(trans_0_0_0_0);
		// _0_0_0_1
		AffineTransform trans_0_0_0_1 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintShapeNode_0_0_0_1(g);
		g.setTransform(trans_0_0_0_1);
		// _0_0_0_2
		AffineTransform trans_0_0_0_2 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintShapeNode_0_0_0_2(g);
		g.setTransform(trans_0_0_0_2);
	}

	private void paintCanvasGraphicsNode_0_0(Graphics2D g) {
		// _0_0_0
		AffineTransform trans_0_0_0 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintCompositeGraphicsNode_0_0_0(g);
		g.setTransform(trans_0_0_0);
	}

	private void paintRootGraphicsNode_0(Graphics2D g) {
		// _0_0
		g.setComposite(AlphaComposite.getInstance(3, 1.0f * origAlpha));
		AffineTransform trans_0_0 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintCanvasGraphicsNode_0_0(g);
		g.setTransform(trans_0_0);
	}



    /**
     * Returns the X of the bounding box of the original SVG image.
     * @return The X of the bounding box of the original SVG image.
     */
    public int getOrigX() {
        return 334;
    }

    /**
     * Returns the Y of the bounding box of the original SVG image.
     * @return The Y of the bounding box of the original SVG image.
     */
    public int getOrigY() {
        return 516;
    }

    /**
     * Returns the width of the bounding box of the original SVG image.
     * @return The width of the bounding box of the original SVG image.
     */
    public int getOrigWidth() {
        return 34;
    }

    /**
     * Returns the height of the bounding box of the original SVG image.
     * @return The height of the bounding box of the original SVG image.
     */
    public int getOrigHeight() {
        return 34;
    }


	/**
	 * The current width of this resizable icon.
	 */
	int width;

	/**
	 * The current height of this resizable icon.
	 */
	int height;

	/**
	 * Creates a new transcoded SVG image.
	 */
	public Earth() {
        this.width = getOrigWidth();
        this.height = getOrigHeight();
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.Icon#getIconHeight()
	 */
    @Override
	public int getIconHeight() {
		return height;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.Icon#getIconWidth()
	 */
    @Override
	public int getIconWidth() {
		return width;
	}

	/*
	 * Set the dimension of the icon.
	 */

	public void setDimension(Dimension newDimension) {
		this.width = newDimension.width;
		this.height = newDimension.height;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.Icon#paintIcon(java.awt.Component, java.awt.Graphics, int, int)
	 */
    @Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		Graphics2D g2d = (Graphics2D) g.create();
		g2d.translate(x, y);

		double coef1 = (double) this.width / (double) getOrigWidth();
		double coef2 = (double) this.height / (double) getOrigHeight();
		double coef = Math.min(coef1, coef2);
		g2d.scale(coef, coef);
		paint(g2d);
		g2d.dispose();
	}
}

/**
 * This class has been automatically generated using svg2java
 *
 */
class Venus implements Icon {

	private float origAlpha = 1.0f;

	/**
	 * Paints the transcoded SVG image on the specified graphics context. You
	 * can install a custom transformation on the graphics context to scale the
	 * image.
	 *
	 * @param g
	 *            Graphics context.
	 */
	public void paint(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        origAlpha = 1.0f;
        Composite origComposite = g.getComposite();
        if (origComposite instanceof AlphaComposite) {
            AlphaComposite origAlphaComposite =
                (AlphaComposite)origComposite;
            if (origAlphaComposite.getRule() == AlphaComposite.SRC_OVER) {
                origAlpha = origAlphaComposite.getAlpha();
            }
        }

		// _0
		AffineTransform trans_0 = g.getTransform();
		paintRootGraphicsNode_0(g);
		g.setTransform(trans_0);

	}

	private static GeneralPath shape0 = null, shape1 = null, shape2 = null;
	private void paintShapeNode_0_0_0_0(Graphics2D g) {
		if (shape0 == null) {
			shape0 = new GeneralPath();
			shape0.moveTo(176.0, 33.0);
			shape0.curveTo(176.0, 39.07513, 171.07513, 44.0, 165.0, 44.0);
			shape0.curveTo(158.92487, 44.0, 154.0, 39.07513, 154.0, 33.0);
			shape0.curveTo(154.0, 26.924868, 158.92487, 22.0, 165.0, 22.0);
			shape0.curveTo(171.07513, 22.0, 176.0, 26.924868, 176.0, 33.0);
			shape0.closePath();
		}
		//g.setPaint(new Color(0, 0, 0, 255));
		g.setStroke(new BasicStroke(2.0f,0,0,4.0f,null,0.0f));
		g.draw(shape0);
	}

	private void paintShapeNode_0_0_0_1(Graphics2D g) {
		if (shape1 == null) {
			shape1 = new GeneralPath();
			shape1.moveTo(350.0, 536.3622);
			shape1.lineTo(350.0, 550.3622);
		}
		g.draw(shape1);
	}

	private void paintShapeNode_0_0_0_2(Graphics2D g) {
		if (shape2 == null) {
			shape2 = new GeneralPath();
			shape2.moveTo(344.0, 544.3622);
			shape2.lineTo(356.0, 544.3622);
		}
		g.draw(shape2);
	}

	private void paintCompositeGraphicsNode_0_0_0(Graphics2D g) {
		// _0_0_0_0
		AffineTransform trans_0_0_0_0 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 185.0f, 492.3621826171875f));
		paintShapeNode_0_0_0_0(g);
		g.setTransform(trans_0_0_0_0);
		// _0_0_0_1
		AffineTransform trans_0_0_0_1 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintShapeNode_0_0_0_1(g);
		g.setTransform(trans_0_0_0_1);
		// _0_0_0_2
		AffineTransform trans_0_0_0_2 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintShapeNode_0_0_0_2(g);
		g.setTransform(trans_0_0_0_2);
	}

	private void paintCanvasGraphicsNode_0_0(Graphics2D g) {
		// _0_0_0
		AffineTransform trans_0_0_0 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintCompositeGraphicsNode_0_0_0(g);
		g.setTransform(trans_0_0_0);
	}

	private void paintRootGraphicsNode_0(Graphics2D g) {
		// _0_0
		g.setComposite(AlphaComposite.getInstance(3, 1.0f * origAlpha));
		AffineTransform trans_0_0 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintCanvasGraphicsNode_0_0(g);
		g.setTransform(trans_0_0);
	}



    /**
     * Returns the X of the bounding box of the original SVG image.
     * @return The X of the bounding box of the original SVG image.
     */
    public int getOrigX() {
        return 338;
    }

    /**
     * Returns the Y of the bounding box of the original SVG image.
     * @return The Y of the bounding box of the original SVG image.
     */
    public int getOrigY() {
        return 514;
    }

    /**
     * Returns the width of the bounding box of the original SVG image.
     * @return The width of the bounding box of the original SVG image.
     */
    public int getOrigWidth() {
        return 24;
    }

    /**
     * Returns the height of the bounding box of the original SVG image.
     * @return The height of the bounding box of the original SVG image.
     */
    public int getOrigHeight() {
        return 37;
    }


	/**
	 * The current width of this resizable icon.
	 */
	int width;

	/**
	 * The current height of this resizable icon.
	 */
	int height;

	/**
	 * Creates a new transcoded SVG image.
	 */
	public Venus() {
        this.width = getOrigWidth();
        this.height = getOrigHeight();
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.Icon#getIconHeight()
	 */
    @Override
	public int getIconHeight() {
		return height;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.Icon#getIconWidth()
	 */
    @Override
	public int getIconWidth() {
		return width;
	}

	/*
	 * Set the dimension of the icon.
	 */

	public void setDimension(Dimension newDimension) {
		this.width = newDimension.width;
		this.height = newDimension.height;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.Icon#paintIcon(java.awt.Component, java.awt.Graphics, int, int)
	 */
    @Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		Graphics2D g2d = (Graphics2D) g.create();
		g2d.translate(x, y);

		double coef1 = (double) this.width / (double) getOrigWidth();
		double coef2 = (double) this.height / (double) getOrigHeight();
		double coef = Math.min(coef1, coef2);
		g2d.scale(coef, coef);
		paint(g2d);
		g2d.dispose();
	}
}

/**
 * This class has been automatically generated using svg2java
 *
 */
class Uranus implements Icon {

	private float origAlpha = 1.0f;

	/**
	 * Paints the transcoded SVG image on the specified graphics context. You
	 * can install a custom transformation on the graphics context to scale the
	 * image.
	 *
	 * @param g
	 *            Graphics context.
	 */
	public void paint(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        origAlpha = 1.0f;
        Composite origComposite = g.getComposite();
        if (origComposite instanceof AlphaComposite) {
            AlphaComposite origAlphaComposite =
                (AlphaComposite)origComposite;
            if (origAlphaComposite.getRule() == AlphaComposite.SRC_OVER) {
                origAlpha = origAlphaComposite.getAlpha();
            }
        }

		// _0
		AffineTransform trans_0 = g.getTransform();
		paintRootGraphicsNode_0(g);
		g.setTransform(trans_0);

	}

	private static GeneralPath shape0 = null, shape1 = null, shape2 = null, shape3 = null, shape4 = null;
	private void paintShapeNode_0_0_0_0(Graphics2D g) {
		if (shape0 == null) {
			shape0 = new GeneralPath();
			shape0.moveTo(339.0, 538.8622);
			shape0.lineTo(332.0, 538.8622);
			shape0.lineTo(332.0, 537.8622);
			shape0.lineTo(337.0, 536.8622);
			shape0.lineTo(337.0, 516.8622);
			shape0.lineTo(332.0, 515.8622);
			shape0.lineTo(332.0, 514.8622);
			shape0.lineTo(339.0, 514.8622);
			shape0.lineTo(339.0, 538.8622);
			shape0.closePath();
		}
		//g.setPaint(new Color(0, 0, 0, 255));
		g.fill(shape0);
	}

	private void paintShapeNode_0_0_0_1(Graphics2D g) {
		if (shape1 == null) {
			shape1 = new GeneralPath();
			shape1.moveTo(361.0, 538.8622);
			shape1.lineTo(368.0, 538.8622);
			shape1.lineTo(368.0, 537.8622);
			shape1.lineTo(363.0, 536.8622);
			shape1.lineTo(363.0, 516.8622);
			shape1.lineTo(368.0, 515.8622);
			shape1.lineTo(368.0, 514.8622);
			shape1.lineTo(361.0, 514.8622);
			shape1.lineTo(361.0, 538.8622);
			shape1.closePath();
		}
		g.fill(shape1);
	}

	private void paintShapeNode_0_0_0_2(Graphics2D g) {
		if (shape2 == null) {
			shape2 = new GeneralPath();
			shape2.moveTo(338.0, 526.8622);
			shape2.lineTo(362.0, 526.8622);
		}
		g.setStroke(new BasicStroke(2.0f,0,0,4.0f,null,0.0f));
		g.draw(shape2);
	}

	private void paintShapeNode_0_0_0_3(Graphics2D g) {
		if (shape3 == null) {
			shape3 = new GeneralPath();
			shape3.moveTo(350.0, 514.8622);
			shape3.lineTo(350.0, 540.8622);
		}
		g.draw(shape3);
	}

	private void paintShapeNode_0_0_0_4(Graphics2D g) {
		if (shape4 == null) {
			shape4 = new GeneralPath();
			shape4.moveTo(40.0, 211.0);
			shape4.curveTo(40.0, 213.20914, 38.20914, 215.0, 36.0, 215.0);
			shape4.curveTo(33.79086, 215.0, 32.0, 213.20914, 32.0, 211.0);
			shape4.curveTo(32.0, 208.79086, 33.79086, 207.0, 36.0, 207.0);
			shape4.curveTo(38.20914, 207.0, 40.0, 208.79086, 40.0, 211.0);
			shape4.closePath();
		}
		g.draw(shape4);
	}

	private void paintCompositeGraphicsNode_0_0_0(Graphics2D g) {
		// _0_0_0_0
		AffineTransform trans_0_0_0_0 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintShapeNode_0_0_0_0(g);
		g.setTransform(trans_0_0_0_0);
		// _0_0_0_1
		AffineTransform trans_0_0_0_1 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintShapeNode_0_0_0_1(g);
		g.setTransform(trans_0_0_0_1);
		// _0_0_0_2
		AffineTransform trans_0_0_0_2 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintShapeNode_0_0_0_2(g);
		g.setTransform(trans_0_0_0_2);
		// _0_0_0_3
		AffineTransform trans_0_0_0_3 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintShapeNode_0_0_0_3(g);
		g.setTransform(trans_0_0_0_3);
		// _0_0_0_4
		AffineTransform trans_0_0_0_4 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 314.0f, 334.8621826171875f));
		paintShapeNode_0_0_0_4(g);
		g.setTransform(trans_0_0_0_4);
	}

	private void paintCanvasGraphicsNode_0_0(Graphics2D g) {
		// _0_0_0
		AffineTransform trans_0_0_0 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintCompositeGraphicsNode_0_0_0(g);
		g.setTransform(trans_0_0_0);
	}

	private void paintRootGraphicsNode_0(Graphics2D g) {
		// _0_0
		g.setComposite(AlphaComposite.getInstance(3, 1.0f * origAlpha));
		AffineTransform trans_0_0 = g.getTransform();
		g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
		paintCanvasGraphicsNode_0_0(g);
		g.setTransform(trans_0_0);
	}



    /**
     * Returns the X of the bounding box of the original SVG image.
     * @return The X of the bounding box of the original SVG image.
     */
    public int getOrigX() {
        return 332;
    }

    /**
     * Returns the Y of the bounding box of the original SVG image.
     * @return The Y of the bounding box of the original SVG image.
     */
    public int getOrigY() {
        return 515;
    }

    /**
     * Returns the width of the bounding box of the original SVG image.
     * @return The width of the bounding box of the original SVG image.
     */
    public int getOrigWidth() {
        return 36;
    }

    /**
     * Returns the height of the bounding box of the original SVG image.
     * @return The height of the bounding box of the original SVG image.
     */
    public int getOrigHeight() {
        return 36;
    }


	/**
	 * The current width of this resizable icon.
	 */
	int width;

	/**
	 * The current height of this resizable icon.
	 */
	int height;

	/**
	 * Creates a new transcoded SVG image.
	 */
	public Uranus() {
        this.width = getOrigWidth();
        this.height = getOrigHeight();
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.Icon#getIconHeight()
	 */
    @Override
	public int getIconHeight() {
		return height;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.Icon#getIconWidth()
	 */
    @Override
	public int getIconWidth() {
		return width;
	}

	/*
	 * Set the dimension of the icon.
	 */

	public void setDimension(Dimension newDimension) {
		this.width = newDimension.width;
		this.height = newDimension.height;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.Icon#paintIcon(java.awt.Component, java.awt.Graphics, int, int)
	 */
    @Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		Graphics2D g2d = (Graphics2D) g.create();
		g2d.translate(x, y);

		double coef1 = (double) this.width / (double) getOrigWidth();
		double coef2 = (double) this.height / (double) getOrigHeight();
		double coef = Math.min(coef1, coef2);
		g2d.scale(coef, coef);
		paint(g2d);
		g2d.dispose();
	}
}
