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

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.jzy3d.chart.AWTChart;
import org.jzy3d.chart.ChartLauncher;
import org.jzy3d.chart.controllers.keyboard.camera.AWTCameraKeyController;
import org.jzy3d.chart.controllers.mouse.camera.AWTCameraMouseController;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.colors.colormaps.ColorMapGrayscale;
import org.jzy3d.colors.colormaps.ColorMapRainbow;
import org.jzy3d.colors.colormaps.ColorMapWhiteRed;
import org.jzy3d.contour.DefaultContourColoringPolicy;
import org.jzy3d.contour.MapperContourMeshGenerator;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.maths.Range;
import org.jzy3d.maths.Rectangle;
import org.jzy3d.plot3d.builder.Builder;
import org.jzy3d.plot3d.builder.Mapper;
import org.jzy3d.plot3d.builder.concrete.OrthonormalGrid;
import org.jzy3d.plot3d.primitives.FlatLine2d;
import org.jzy3d.plot3d.primitives.Scatter;
import org.jzy3d.plot3d.primitives.Shape;
import org.jzy3d.plot3d.primitives.axes.ContourAxeBox;
import org.jzy3d.plot3d.primitives.contour.ContourMesh;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import org.jzy3d.plot3d.rendering.legends.colorbars.AWTColorbarLegend;
import org.jzy3d.plot3d.rendering.view.Renderer2d;

import jparsec.graph.GridChartElement.COLOR_MODEL;
import jparsec.graph.TextLabel.ALIGN;
import jparsec.io.FileIO;
import jparsec.io.image.Picture;
import jparsec.math.Evaluation;
import jparsec.util.JPARSECException;

/**
 * A class to create 3d charts using JZY3D library visualization library.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class CreateJZY3DChart {

	private ChartElement3D chart_elem;
	private GridChartElement grid_chart_elem;
	private String fxy;
	private int min, max, n;
	private org.jzy3d.chart.AWTChart chart;

	/**
	 * Creates a 3d chart from a 3d chart object.
	 * @param chart3d The 3d chart object.
	 * @throws JPARSECException If an error occurs.
	 */
	public CreateJZY3DChart(ChartElement3D chart3d) throws JPARSECException {
		this.chart_elem = chart3d.clone();
		init(chart3d);
	}
	/**
	 * Creates a 3d chart from a grid chart object.
	 * @param chart The grid chart object.
	 * @throws JPARSECException If an error occurs.
	 */
	public CreateJZY3DChart(GridChartElement chart) throws JPARSECException {
		this.grid_chart_elem = chart.clone();
		init(chart);
	}
	/**
	 * Creates a 3d chart from a function.
	 * @param fxy A function f(x,y)
	 * @param min The minimum x/y.
	 * @param max The maximum x/y.
	 * @param n The number of points to sample the function in x/y axes.
	 * @throws JPARSECException If an error occurs.
	 */
	public CreateJZY3DChart(String fxy, int min, int max, int n) throws JPARSECException {
		this.fxy = fxy;
		this.min = min;
		this.max = max;
		this.n = n;
		init(fxy, min, max, n);
	}

	private void init(final ChartElement3D chart3d) throws JPARSECException {
        chart = new AWTChart(Quality.Advanced, "awt");
		int size = 0;
		for (int i=0; i<chart3d.series.length; i++) {
			if (!chart3d.series[i].isSurface) {
				int nx = DataSet.getDifferentElements(chart3d.series[i].xValues).length;
				int ny = DataSet.getDifferentElements(chart3d.series[i].yValues).length;
				if (nx != chart3d.series[i].xValues.length || ny != nx) // 3d plot
					size += chart3d.series[i].xValues.length;
			}
		}
    Coord3d[] points = new Coord3d[size];
    Color[]   colors = new Color[size];
    int index = 0;
		for (int i=0; i<chart3d.series.length; i++) {
			double px[] = DataSet.toDoubleValues(chart3d.series[i].xValues);
			double py[] = DataSet.toDoubleValues(chart3d.series[i].yValues);
			double pz[] = null;
			if (chart3d.series[i].isSurface) {
		        // Define range and precision for the function to plot
		        Range range = new Range(chart3d.getxMin(), chart3d.getxMax());
		        int steps = chart3d.series[i].xValues.length;

		        final GridChartElement gridChart = GridChartElement.getSurfaceFromPoints(ChartSeriesElement3D.get3dPointsFromDataSet((double[][])chart3d.series[i].zValues, chart3d.series[i].getLimits()), chart3d.series[i].xValues.length);
		        // Define a function to plot
		        Mapper mapper = new Mapper() {
		            public double f(double x, double y) {
		                return gridChart.getIntensityAt(x, y);
		            }
		        };

		        // Create the object to represent the function over the given range.
		        final Shape surface = Builder.buildOrthonormal(new OrthonormalGrid(range, steps, range, steps), mapper);
	        	surface.setColorMapper(new ColorMapper(new ColorMapRainbow(), surface.getBounds().getZmin(), surface.getBounds().getZmax(), new Color(1, 1, 1, .5f)));
		        surface.setFaceDisplayed(true);
		        surface.setWireframeDisplayed(true);
		        surface.setWireframeColor(Color.BLACK);

		        if (chart3d.showTitle) {
					Renderer2d title = new Renderer2d(){
			        	public void paint(Graphics g){
			    			g.setColor(java.awt.Color.BLACK);
			    			TextLabel tl = new TextLabel(chart3d.title);
			    			tl.draw(g, chart.getView().getCanvas().getRendererWidth()/2, 20, ALIGN.CENTER);
			    	        if (chart3d.zLabel != null && chart3d.zLabel.length() > 0) {
			        			g.setColor(java.awt.Color.BLACK);
			        			tl = new TextLabel(chart3d.zLabel);
			        			tl.draw(g, chart.getView().getCanvas().getRendererWidth(), chart.getView().getCanvas().getRendererHeight()-10, ALIGN.RIGHT);
			    	        }
			        	}
			        };
			        chart.addRenderer(title);
		        }

		        if (chart3d.xLabel != null && chart3d.xLabel.length() > 0 && chart3d.xLabel.indexOf("@") < 0)
		        	chart.getView().getAxe().getLayout().setXAxeLabel(chart3d.xLabel);
		        if (chart3d.yLabel != null && chart3d.yLabel.length() > 0 && chart3d.yLabel.indexOf("@") < 0)
		        	chart.getView().getAxe().getLayout().setYAxeLabel(chart3d.yLabel);
		        if (chart3d.zLabel != null && chart3d.zLabel.length() > 0 && chart3d.zLabel.indexOf("@") < 0)
		        	chart.getView().getAxe().getLayout().setZAxeLabel(chart3d.zLabel);

		        if (chart3d.showLegend) {
		    		surface.setLegend(new AWTColorbarLegend(surface,
							chart.getView().getAxe().getLayout().getZTickProvider(),
							chart.getView().getAxe().getLayout().getZTickRenderer()));
		        }

		        chart.getScene().add(surface);
			} else {
				int nx = DataSet.getDifferentElements(chart3d.series[i].xValues).length;
				int ny = DataSet.getDifferentElements(chart3d.series[i].yValues).length;
				if (nx == chart3d.series[i].xValues.length && ny == nx) { // 2d plot
					FlatLine2d line2d = new FlatLine2d(DataSet.toFloatArray(px), DataSet.toFloatArray(py), 10f);
					line2d.setColorMapper( new ColorMapper( new ColorMapWhiteRed(), 0f, 1f ) );
					line2d.setWireframeDisplayed(true);
					line2d.setWireframeColor(Color.BLACK);
			        chart.getScene().add(line2d);
				} else {
					pz = (double[])chart3d.series[i].zValues;
					for (int j=0; j<chart3d.series[i].xValues.length; j++) {
						points[index] = new Coord3d(px[j], py[j], pz[j]);
						colors[index] = Color.BLACK;
						if (chart3d.series[i].color != null)
							colors[index] = new Color(chart3d.series[i].color.getRed(),
									chart3d.series[i].color.getGreen(), chart3d.series[i].color.getBlue(),
									chart3d.series[i].color.getAlpha());
						index ++;
					}
				}
			}
		}
		if (size > 0) {
	        Scatter scatter = new Scatter(points, colors);
	        scatter.setWidth(3);
	        chart.getScene().add(scatter);
		}
        chart.addController(new AWTCameraKeyController());
        chart.addController(new AWTCameraMouseController());
	}

	private void init(final GridChartElement gridChart) throws JPARSECException {
		if (gridChart.opacity == null) {
	        chart = new AWTChart(Quality.Advanced, "awt");
		} else {
			switch (gridChart.opacity) {
			case OPAQUE:
		        chart = new AWTChart(Quality.Intermediate, "awt");
				break;
			case SEMI_TRANSPARENT:
		        chart = new AWTChart(Quality.Advanced, "awt");
				break;
			case TRANSPARENT:
		        chart = new AWTChart(Quality.Fastest, "awt");
				break;
			case VARIABLE_WITH_Z:
		        chart = new AWTChart(Quality.Nicest, "awt");
				break;
			}
		}

        // Define range and precision for the function to plot
        Range range = new Range(-gridChart.getMaximum(), gridChart.getMaximum());
        int steps = gridChart.data.length;

        // Define a function to plot
        Mapper mapper = new Mapper() {
            public double f(double x, double y) {
                return gridChart.getIntensityAt(x, y);
            }
        };

        // Create the object to represent the function over the given range.
        final Shape surface = Builder.buildOrthonormal(new OrthonormalGrid(range, steps, range, steps), mapper);
        if (gridChart.colorModel == COLOR_MODEL.BLUE_TO_RED || gridChart.colorModel == COLOR_MODEL.RED_TO_BLUE)
        	surface.setColorMapper(new ColorMapper(new ColorMapRainbow(), surface.getBounds().getZmin(), surface.getBounds().getZmax(), new Color(1, 1, 1, .5f)));
        if (gridChart.colorModel == COLOR_MODEL.BLACK_TO_WHITE || gridChart.colorModel == COLOR_MODEL.WHITE_TO_BLACK)
        	surface.setColorMapper(new ColorMapper(new ColorMapGrayscale(), surface.getBounds().getZmin(), surface.getBounds().getZmax(), new Color(1, 1, 1, .5f)));
        surface.setFaceDisplayed(true);
        surface.setWireframeDisplayed(true);
        surface.setWireframeColor(Color.BLACK);

        if (!gridChart.ocultLevels && gridChart.levels != null && gridChart.levels.length > 0) {
/*    		MapperContourPictureGenerator contour = new MapperContourPictureGenerator(mapper, range, range);
    		int nPoints = 1000;
    		double[][] contours = contour.getContourMatrix(nPoints, nPoints, 10); // How to select the levels !!! ???
    		int size = nPoints * nPoints;
    		Coord3d[] points = new Coord3d[size];
    		double step = (range.getMax() - range.getMin()) / (nPoints - 1);
    		for (int x = 0; x < nPoints; x++) {
    			double px = range.getMin() + step * x;
    			for (int y = 0; y < nPoints; y++) {
        			double py = range.getMin() + step * y;
    				if (contours[x][y]>-Double.MAX_VALUE){ // Non contours points are -Double.MAX_VALUE and are not painted
    					points[x*nPoints+y] = new Coord3d((float) px, (float) py, (float)contours[x][y]);
    				} else {
    					points[x*nPoints+y] = new Coord3d((float)px, (float) py, (float)0.0);
    				}
    			}
        	}
    		MultiColorScatter scatter = new MultiColorScatter(points, surface.getColorMapper());
	        chart.getScene().add(scatter);
*/

			// Another, less brute force, but buggy way (and also slow). At least levels can be defined ...
            MapperContourMeshGenerator contour = new MapperContourMeshGenerator(mapper, range, range);
            ContourAxeBox cab = new ContourAxeBox(chart.getView().getBounds());
            ContourMesh mesh = contour.getContourMesh(new DefaultContourColoringPolicy(surface.getColorMapper()), 400, 400, gridChart.levels, 0, false);
            cab.setContourMesh(mesh);
            chart.getView().setAxe(cab);
        }

        if (gridChart.legend != null && gridChart.legend.length() > 0) {
    		surface.setLegend(new AWTColorbarLegend(surface,
					chart.getView().getAxe().getLayout().getZTickProvider(),
					chart.getView().getAxe().getLayout().getZTickRenderer()));
        }

		Renderer2d title = new Renderer2d(){
        	public void paint(Graphics g){
    			g.setColor(java.awt.Color.BLACK);
    			TextLabel tl = new TextLabel(gridChart.title);
    			tl.draw(g, chart.getView().getCanvas().getRendererWidth()/2, 20, ALIGN.CENTER);
    	        if (gridChart.legend != null && gridChart.legend.length() > 0) {
        			g.setColor(java.awt.Color.BLACK);
        			tl = new TextLabel(gridChart.legend);
        			tl.draw(g, chart.getView().getCanvas().getRendererWidth(), chart.getView().getCanvas().getRendererHeight()-10, ALIGN.RIGHT);
    	        }
        	}
        };
        chart.addRenderer(title);

        if (gridChart.xLabel != null && gridChart.xLabel.length() > 0 && gridChart.xLabel.indexOf("@") < 0)
        	chart.getView().getAxe().getLayout().setXAxeLabel(gridChart.xLabel);
        if (gridChart.yLabel != null && gridChart.yLabel.length() > 0 && gridChart.yLabel.indexOf("@") < 0)
        	chart.getView().getAxe().getLayout().setYAxeLabel(gridChart.yLabel);
        if (gridChart.legend != null && gridChart.legend.length() > 0 && gridChart.legend.indexOf("@") < 0)
        	chart.getView().getAxe().getLayout().setZAxeLabel(gridChart.legend);

        chart.getScene().add(surface);
        chart.addController(new AWTCameraKeyController());
        chart.addController(new AWTCameraMouseController());
	}

	private void init(final String fxy, int min, int max, int n) throws JPARSECException {
        chart = new AWTChart(Quality.Intermediate, "awt");

        // Define range and precision for the function to plot
        Range range = new Range(min, max);
        int steps = n;

        // Define a function to plot
        Mapper mapper = new Mapper() {
            public double f(double x, double y) {
                try {
					return Evaluation.evaluate(fxy, new String[] {"x "+x, "y "+y});
				} catch (JPARSECException e) {
					e.printStackTrace();
					return 0;
				}
            }
        };

        // Create the object to represent the function over the given range.
        final Shape surface = Builder.buildOrthonormal(new OrthonormalGrid(range, steps, range, steps), mapper);
       	surface.setColorMapper(new ColorMapper(new ColorMapRainbow(), surface.getBounds().getZmin(), surface.getBounds().getZmax(), new Color(1, 1, 1, .5f)));
        surface.setFaceDisplayed(true);
        surface.setWireframeDisplayed(true);
        surface.setWireframeColor(Color.BLACK);

		surface.setLegend(new AWTColorbarLegend(surface,
				chart.getView().getAxe().getLayout().getZTickProvider(),
				chart.getView().getAxe().getLayout().getZTickRenderer()));

		Renderer2d title = new Renderer2d(){
        	public void paint(Graphics g){
    			g.setColor(java.awt.Color.BLACK);
    			int x = (chart.getView().getCanvas().getRendererWidth() - g.getFontMetrics().stringWidth(fxy)) / 2;
    			g.drawString(fxy, x, 20);
        	}
        };
        chart.addRenderer(title);

        chart.getScene().add(surface);
        chart.addController(new AWTCameraKeyController());
        chart.addController(new AWTCameraMouseController());
	}

	/**
	 * Shows the chart for a given size.
	 * @param i Width.
	 * @param j Height.
	 */
	public void showChart(int i, int j) {
		if (chart_elem == null) {
			if (grid_chart_elem == null) {
				ChartLauncher.openStaticChart(chart, new Rectangle(0, 0, i, j), fxy);
			} else {
				ChartLauncher.openStaticChart(chart, new Rectangle(0, 0, i, j), grid_chart_elem.title);
			}
		} else {
			ChartLauncher.openStaticChart(chart, new Rectangle(0, 0, i, j), chart_elem.title);
		}
	}

	/**
	 * Returns the chart object of this instance.
	 * @return Chart.
	 */
	public ChartElement3D getChartElement()
	{
		return this.chart_elem;
	}

	private void writeObject(ObjectOutputStream out)
	throws IOException {
		out.writeObject(this.chart_elem);
		out.writeObject(this.grid_chart_elem);
		out.writeObject(fxy);
		out.writeInt(min);
		out.writeInt(max);
		out.writeInt(n);
	}
	private void readObject(ObjectInputStream in)
	throws IOException, ClassNotFoundException {
		chart_elem = (ChartElement3D) in.readObject();
		grid_chart_elem = (GridChartElement) in.readObject();
		fxy = (String) in.readObject();
		min = in.readInt();
		max = in.readInt();
		n = in.readInt();
		if (chart_elem == null) {
			if (grid_chart_elem == null) {
				try {
					init(fxy, min, max, n);
				} catch (Exception exc) {}
			} else {
				try {
					init(grid_chart_elem);
				} catch (Exception exc) {}
			}
		} else {
			try {
				init(chart_elem);
			} catch (Exception exc) {}
		}
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
		file_name+=".png";

		Picture p = new Picture(this.chartAsBufferedImage());
		p.write(file_name);
	}

	/**
	 * Returns a BufferedImage instance with the current chart, adequate to
	 * write an image to disk.
	 * @return The image.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public BufferedImage chartAsBufferedImage() throws JPARSECException
	{
		String path = FileIO.getTemporalDirectory() + "chart.png";
		try {
			ChartLauncher.screenshot(chart, path);
		} catch (IOException e) {
			throw new JPARSECException("Cannot return image", e);
		}
		Picture pic = new Picture(path);
		return pic.getImage();
	}

	/**
	 * Returns the 3d chart object in case this object was
	 * used to create this instance.
	 * @return Chart object.
	 */
	public ChartElement3D getChart()
	{
		return chart_elem;
	}

	/**
	 * Returns the grid chart object in case a grid
	 * chart was used to create this instance.
	 * @return Grid chart object.
	 */
	public GridChartElement getGridChart()
	{
		return grid_chart_elem;
	}

/*
TODO:
- Scatter: lines in bar plot, errors, pointers
- drawLines in ChartSeriesElement3D => Lines like in interpolation or AddRemoveElements demos
- Bar plot (histogram demo)
- Use Chromatogram demo ?
- Support text effects also in axes labels ?
- Dates in x axis ?
 */
}
