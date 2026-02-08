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
package jparsec.graph.chartRendering;

/**
 * A simple Rectangle class to mimic the AWT one. Note this class cannot extends
 * the AWT one since it is used in both AWT and Android.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Rectangle {
    /**
     * Parameters of the rectangle.
     */
    private float x, y, width, height;
    private float maxx = -1.0f, maxy = - 1.0f;

    /**
     * Empty constructor.
     */
    public Rectangle() {
        maxx = -1;
        maxy = -1;
    }

    /**
     * The constructor.
     *
     * @param x      X.
     * @param y      Y.
     * @param width  Width.
     * @param height Height.
     */
    public Rectangle(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        maxx = x + width - 1.0f;
        maxy = y + height - 1.0f;
    }

    /**
     * Returns x.
     * @return Minimum x;
     */
    public float getMinX() {
        return x;
    }

    /**
     * Returns x + width.
     * @return Maximum x;
     */
    public float getMaxX() {
        return maxx;
    }

    /**
     * Returns y.
     * @return Minimum y.
     */
    public float getMinY() {
        return y;
    }

    /**
     * Returns y + height.
     * @return Maximum y.
     */
    public float getMaxY() {
        return maxy;
    }

    /**
     * Returns width.
     * @return The width.
     */
    public float getWidth() {
        return width;
    }

    /**
     * Returns height.
     * @return The height.
     */
    public float getHeight() {
        return height;
    }

    /**
     * Returns if this rectangle contains a given point.
     * @param x The x position.
     * @param y The y position.
     * @return True or false.
     */
    public boolean contains(float x, float y) {
        return (x >= this.x) &&
               (x <= maxx) &&
               (y >= this.y) &&
               (y <= maxy);
    }

    /**
     * Check if a specified line intersects a specified rectangle.
     *
     * @param lx0        X coordinate of 1st end point of line
     * @param ly0        Y coordinate of 1st end point of line
     * @param lx1        X coordinate of 2nd end point of line
     * @param ly1        Y coordinate of 2nd end point of line
     * @return True if the line intersects the rectangle,
     * false otherwise.
     */
    public boolean isLineIntersectingRectangle(float lx0, float ly0, float lx1, float ly1) {
        float x0 = x, y0 = y, x1 = x + width, y1 = y + height;

        // Is one of the line endpoints inside the rectangle
        if (isPointInsideRectangle(x0, y0, x1, y1, lx0, ly0) ||
                isPointInsideRectangle(x0, y0, x1, y1, lx1, ly1))
            return true;

        // If it intersects it goes through. Need to check three sides only.

        // Check against top rectangle line
        if (isLineIntersectingLine(lx0, ly0, lx1, ly1, x0, y0, x1, y0))
            return true;

        // Check against left rectangle line
        if (isLineIntersectingLine(lx0, ly0, lx1, ly1, x0, y0, x0, y1))
            return true;

        // Check against bottom rectangle line
        if (isLineIntersectingLine(lx0, ly0, lx1, ly1, x0, y1, x1, y1))
            return true;

        return false;
    }

    private boolean isLineIntersectingLine(float x0, float y0, float x1, float y1,
                                           float x2, float y2, float x3, float y3) {
        int s1 = sameSide(x0, y0, x1, y1, x2, y2, x3, y3);
        int s2 = sameSide(x2, y2, x3, y3, x0, y0, x1, y1);

        return s1 <= 0 && s2 <= 0;
    }

    private int sameSide(float x0, float y0, float x1, float y1,
                         float px0, float py0, float px1, float py1) {
        int sameSide = 0;

        double dx = x1 - x0;
        double dy = y1 - y0;
        double dx1 = px0 - x0;
        double dy1 = py0 - y0;
        double dx2 = px1 - x1;
        double dy2 = py1 - y1;

        // Cross product of the vector from the endpoint of the line to the point
        double c1 = dx * dy1 - dy * dx1;
        double c2 = dx * dy2 - dy * dx2;

        if (c1 != 0 && c2 != 0)
            sameSide = c1 < 0 != c2 < 0 ? -1 : 1;
        else if (dx == 0 && dx1 == 0 && dx2 == 0)
            sameSide = !isBetween(y0, y1, py0) && !isBetween(y0, y1, py1) ? 1 : 0;
        else if (dy == 0 && dy1 == 0 && dy2 == 0)
            sameSide = !isBetween(x0, x1, px0) && !isBetween(x0, x1, px1) ? 1 : 0;

        return sameSide;
    }

    private boolean isPointInsideRectangle(float x0, float y0, float x1, float y1, float x, float y) {
        return x >= x0 && x < x1 && y >= y0 && y < y1;
    }

    private boolean isBetween(float a, float b, float c) {
        return b > a ? c >= a && c <= b : c >= b && c <= a;
    }
}
