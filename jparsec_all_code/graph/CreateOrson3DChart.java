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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import com.orsoncharts.Chart3D;
import com.orsoncharts.Chart3DFactory;
import com.orsoncharts.Chart3DPanel;
import com.orsoncharts.Colors;
import com.orsoncharts.Range;
import com.orsoncharts.TitleAnchor;
import com.orsoncharts.axis.LabelOrientation;
import com.orsoncharts.axis.LogAxis3D;
import com.orsoncharts.axis.NumberAxis3D;
import com.orsoncharts.axis.StandardCategoryAxis3D;
import com.orsoncharts.axis.ValueAxis3D;
import com.orsoncharts.data.DefaultKeyedValues;
import com.orsoncharts.data.category.StandardCategoryDataset3D;
import com.orsoncharts.data.function.Function3D;
import com.orsoncharts.data.xyz.XYZDataset;
import com.orsoncharts.data.xyz.XYZSeries;
import com.orsoncharts.data.xyz.XYZSeriesCollection;
import com.orsoncharts.graphics3d.DefaultDrawable3D;
import com.orsoncharts.graphics3d.Dimension3D;
import com.orsoncharts.graphics3d.Drawable3D;
import com.orsoncharts.graphics3d.Object3D;
import com.orsoncharts.graphics3d.ViewPoint3D;
import com.orsoncharts.graphics3d.World;
import com.orsoncharts.graphics3d.swing.DisplayPanel3D;
import com.orsoncharts.graphics3d.swing.Panel3D;
import com.orsoncharts.label.StandardCategoryItemLabelGenerator;
import com.orsoncharts.label.StandardXYZLabelGenerator;
import com.orsoncharts.legend.LegendAnchor;
import com.orsoncharts.marker.RangeMarker;
import com.orsoncharts.plot.CategoryPlot3D;
import com.orsoncharts.plot.XYZPlot;
import com.orsoncharts.renderer.RainbowScale;
import com.orsoncharts.renderer.xyz.ScatterXYZRenderer;
import com.orsoncharts.renderer.xyz.StandardXYZColorSource;
import com.orsoncharts.renderer.xyz.SurfaceRenderer;
import com.orsoncharts.style.ChartStyler;
import com.orsoncharts.util.Orientation;

import jparsec.graph.ChartElement.TYPE;
import jparsec.graph.chartRendering.AWTGraphics;
import jparsec.io.FileIO;
import jparsec.io.image.Picture;
import jparsec.util.JPARSECException;

/**
 * Creates 3d charts using Orson Charts library. Most types of charts are supported from the 
 * generic data model in JPARSEC, with the exception of pie charts.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class CreateOrson3DChart {

	/**
	 * Creates a category plot using Orson Charts.
	 * @param chartElem The chart object representing a category chart.
	 * @return The 3d chart.
	 */
	public static Chart3D createCategoryPlot(ChartElement chartElem) {
		if (chartElem.chartType != TYPE.CATEGORY_CHART) return null;
        StandardCategoryDataset3D<String, String, String> dataset = new StandardCategoryDataset3D<String, String, String>();
        for (int i=0; i<chartElem.series.length; i++) {
        	ChartSeriesElement series = chartElem.series[i];
	        DefaultKeyedValues<String, Number> s = new DefaultKeyedValues<String, 
	                Number>();
	        for (int j=0; j<series.xValues.length; j++) {
	        	s.put(series.xValues[j], Double.parseDouble(series.yValues[j]));
	        }
	        dataset.addSeriesAsRow(series.legend, s);
        }

        String title = chartElem.title, subtitle = "";
        if (title.indexOf(FileIO.getLineSeparator()) > 0) {
        	subtitle = FileIO.getRestAfterField(1, title, FileIO.getLineSeparator(), true);
        	title = FileIO.getField(1, title, FileIO.getLineSeparator(), true);
        }
        Chart3D chart = null;
        switch (chartElem.subType) {
        case CATEGORY_BAR:
        case CATEGORY_BAR_3D:
        case CATEGORY_STACKED_BAR:
        case CATEGORY_STACKED_BAR_3D:
            chart = Chart3DFactory.createBarChart(
            		title, subtitle, dataset, 
            		null, chartElem.xLabel, chartElem.yLabel);
            break;
        case CATEGORY_AREA:
        case CATEGORY_STACKED_AREA:
        	chart = Chart3DFactory.createAreaChart(
        			title, subtitle, dataset, 
        			null, chartElem.xLabel, chartElem.yLabel);
        	break;
        case CATEGORY_LINE:
        case CATEGORY_LINE_3D:
        	chart = Chart3DFactory.createLineChart(
        			title, subtitle, dataset, 
        			null, chartElem.xLabel, chartElem.yLabel);
        	break;
		default:
			return null;
        };
        
        chart.setTitleAnchor(TitleAnchor.TOP_CENTER);
        chart.setLegendPosition(LegendAnchor.BOTTOM_CENTER, 
                Orientation.HORIZONTAL);
        chart.getViewPoint().panLeftRight(-Math.PI / 60);
        CategoryPlot3D plot = (CategoryPlot3D) chart.getPlot();
        StandardCategoryAxis3D xAxis = (StandardCategoryAxis3D) 
                plot.getColumnAxis();
        NumberAxis3D yAxis = (NumberAxis3D) plot.getValueAxis();
        StandardCategoryAxis3D zAxis = (StandardCategoryAxis3D) 
                plot.getRowAxis();
        plot.setGridlineStrokeForValues(new BasicStroke(0.0f));
        xAxis.setLineColor(new Color(0, 0, 0, 0));
        yAxis.setLineColor(new Color(0, 0, 0, 0));
        zAxis.setLineColor(new Color(0, 0, 0, 0));
        plot.getRenderer().setColors(Colors.createPastelColors());
        plot.setToolTipGenerator(new StandardCategoryItemLabelGenerator(
                "%2$s (%3$s) = %4$s degrees"));        
        return chart;    
	}
	
	/**
	 * Creates a surface plot using Orson Charts.
	 * @param grid The grid chart object.
	 * @param function The 3d function, or null to create it automatically from the grid data.
	 * @return The 3d chart.
	 */
	public static Chart3D createSurfacePlot(final GridChartElement grid, Function3D function) {
		if (function == null)
			function = new Function3D() {
				private static final long serialVersionUID = 1L;
				@Override
	            public double getValue(double x, double z) {
	            	return grid.getIntensityAt(x, z);
	            }
	        };
        
        Chart3D chart = Chart3DFactory.createSurfaceChart(
                grid.title, grid.subTitle, 
                function, grid.xLabel, grid.legend, grid.yLabel);
        XYZPlot plot = (XYZPlot) chart.getPlot();
        plot.setDimensions(new Dimension3D(10, 5, 10));
        ValueAxis3D xAxis = plot.getXAxis();
        xAxis.setRange(grid.limits[0], grid.limits[1]);
        ValueAxis3D zAxis = plot.getZAxis();
        zAxis.setRange(grid.limits[2], grid.limits[3]);
        SurfaceRenderer renderer = (SurfaceRenderer) plot.getRenderer();
        renderer.setColorScale(new RainbowScale(new Range(-1.0, 1.0)));
        //renderer.setColorScale(new GradientColorScale(new Range(-1.0, 1.0), 
        //        Color.RED, Color.YELLOW));
        renderer.setDrawFaceOutlines(false);
        chart.setTitleAnchor(TitleAnchor.TOP_CENTER);
        chart.setLegendPosition(LegendAnchor.BOTTOM_CENTER, 
                Orientation.HORIZONTAL);
        return chart;
	}
	
	/**
	 * Creates a scatter plot using Orson Charts.
	 * @param chart3d The chart object with the data.
	 * @return The 3d chart.
	 */
	public static Chart3D createScatterPlot(ChartElement3D chart3d) {
        XYZSeriesCollection<String> dataset = new XYZSeriesCollection<String>();
        
        Color col[] = new Color[chart3d.series.length];
        for (int j=0; j<chart3d.series.length; j++) {
        	ChartSeriesElement3D s0 = chart3d.series[j];
            XYZSeries<String> s = new XYZSeries<String>(s0.legend);
            for (int i = 0; i < s0.xValues.length; i++) {
                s.add(Double.parseDouble(s0.xValues[i]), Double.parseDouble(s0.yValues[i]), 
                		((double[]) s0.zValues) [i]);
            }
            dataset.add(s);
            col[j] = s0.color;
            if (col[j] == null) col[j] = Color.BLACK;
        }

        String title = chart3d.title, subtitle = "";
        if (title.indexOf(FileIO.getLineSeparator()) > 0) {
        	subtitle = FileIO.getRestAfterField(1, title, FileIO.getLineSeparator(), true);
        	title = FileIO.getField(1, title, FileIO.getLineSeparator(), true);
        }
        Chart3D chart = Chart3DFactory.createScatterChart(title, 
        		subtitle, dataset, chart3d.xLabel, chart3d.yLabel, chart3d.zLabel);
        
        //TextElement title1 = new TextElement(title);
        //title1.setFont(StandardChartStyle.createDefaultFont(Font.PLAIN, 16));
        //chart.setTitle(title1);
        chart.setTitleAnchor(TitleAnchor.TOP_CENTER);
        chart.setLegendAnchor(LegendAnchor.BOTTOM_CENTER);
        //chart.setLegendOrientation(Orientation.VERTICAL);

        XYZPlot plot = (XYZPlot) chart.getPlot();
        plot.setDimensions(new Dimension3D(10.0, 4.0, 4.0));
        int f = 80;
        if (chart3d.imageWidth > 0 || chart3d.imageHeight > 0) 
            plot.setDimensions(new Dimension3D(chart3d.imageWidth/f, chart3d.imageHeight/f, 
            		Math.max(chart3d.imageWidth, chart3d.imageHeight)/f));
        plot.setLegendLabelGenerator(new StandardXYZLabelGenerator(
                StandardXYZLabelGenerator.COUNT_TEMPLATE));
        ScatterXYZRenderer renderer = (ScatterXYZRenderer) plot.getRenderer();
        renderer.setSize(0.1);
        renderer.setColors(col);
        if (chart3d.xAxisInLogScale) {
            LogAxis3D xAxis = new LogAxis3D(chart3d.xLabel);
            xAxis.setTickLabelOrientation(LabelOrientation.PERPENDICULAR);
            xAxis.receive(new ChartStyler(chart.getStyle()));
            plot.setYAxis(xAxis);
        }
        if (chart3d.yAxisInLogScale) {
            LogAxis3D yAxis = new LogAxis3D(chart3d.yLabel);
            yAxis.setTickLabelOrientation(LabelOrientation.PERPENDICULAR);
            yAxis.receive(new ChartStyler(chart.getStyle()));
            plot.setYAxis(yAxis);
        }
        if (chart3d.zAxisInLogScale) {
            LogAxis3D zAxis = new LogAxis3D(chart3d.zLabel);
            zAxis.setTickLabelOrientation(LabelOrientation.PERPENDICULAR);
            zAxis.receive(new ChartStyler(chart.getStyle()));
            plot.setYAxis(zAxis);
        }
        chart.setViewPoint(ViewPoint3D.createAboveLeftViewPoint(40));
        
        return chart;
	}
	
	/**
	 * Creates a scatter 'world' with no axes, just the points.
	 * @param chart3d The chart object with the data.
	 * @return The drawable object representing the 3d charts.
	 * @throws JPARSECException If an error occurs.
	 */
	public static Drawable3D createScatterWorld(ChartElement3D chart3d) throws JPARSECException {
        World world = new World();
        for (int i=0; i<chart3d.series.length; i++) {
        	Color col = chart3d.series[i].color;
        	if (col == null) col = Color.BLACK;
        	for (int j=0; j<chart3d.series[i].xValues.length; j++) {
        		double x = Double.parseDouble(chart3d.series[i].xValues[j]);
        		double y = Double.parseDouble(chart3d.series[i].yValues[j]);
        		double z = ((double[]) chart3d.series[i].zValues)[j];
                world.add(Object3D.createCube(0.1, x, y, z, col));
        	}
        }
        DefaultDrawable3D drawable = new DefaultDrawable3D(world);
        return drawable;
	}
	
	/**
	 * Marks a given range in the axes of a given 3d scatter chart. Several 
	 * markers can be added.
	 * @param chart The 3d chart.
	 * @param minX Minimum x to mark.
	 * @param maxX Maximum x to mark.
	 * @param minY Minimum y to mark.
	 * @param maxY Maximum y to mark.
	 * @param minZ Minimum z to mark.
	 * @param maxZ Maximum z to mark.
	 * @param fillCol The set of colors to mark, 4 values: marker color for the x axis, 
	 * y axis, z axis, and fill color for the selected points in the range covered by the marker.
	 * Null can be used for any of them.
	 */
	public static void addMarkers(Chart3D chart, String minX, String maxX, String minY, String maxY, 
			String minZ, String maxZ, Color fillCol[]) {
        XYZPlot plot = (XYZPlot) chart.getPlot();
        ChartStyler styler = new ChartStyler(chart.getStyle());

        NumberAxis3D xAxis = (NumberAxis3D) plot.getXAxis();
        int id = 1;
        if (xAxis.getMarker("X"+id) != null) {
        	while(true) {
        		id ++;
        		if (xAxis.getMarker("X"+id) == null) break;
        	}
        }
        
        RangeMarker xMarker1 = new RangeMarker(Double.parseDouble(minX), Double.parseDouble(maxX), "X: "+minX+" -> "+maxX);
        xMarker1.receive(styler);
        if (fillCol[0] != null) xMarker1.setFillColor(fillCol[0]);
        //xMarker1.setLabelAnchor(Anchor2D.BOTTOM_LEFT);
        xAxis.setMarker("X"+id, xMarker1);
        NumberAxis3D yAxis = (NumberAxis3D) plot.getYAxis();
        RangeMarker yMarker1 = new RangeMarker(Double.parseDouble(minY), Double.parseDouble(maxY), "Y: "+minY+" -> "+maxY);
        yMarker1.receive(styler);
        if (fillCol[1] != null) yMarker1.setFillColor(fillCol[1]);
        yAxis.setMarker("Y"+id, yMarker1);
        NumberAxis3D zAxis = (NumberAxis3D) plot.getZAxis();
        RangeMarker zMarker1 = new RangeMarker(Double.parseDouble(minZ), Double.parseDouble(maxZ), "Z: "+minZ+" -> "+maxZ);
        //zMarker1.setLabelAnchor(Anchor2D.TOP_LEFT);
        zMarker1.receive(styler);
        if (fillCol[2] != null)  zMarker1.setFillColor(fillCol[2]);
        zAxis.setMarker("Z"+id, zMarker1);
        ScatterXYZRenderer renderer = (ScatterXYZRenderer) plot.getRenderer();
        renderer.setSize(0.15);
        if (fillCol[3] != null) {
	        HighlightXYZColorSource colorSource = new HighlightXYZColorSource(
	                plot.getDataset(), fillCol[3], xMarker1.getRange(), 
	                yMarker1.getRange(), zMarker1.getRange(), 
	                chart.getStyle().getStandardColors());
	        renderer.setColorSource(colorSource);
        }
        //StandardXYZItemLabelGenerator generator = new StandardXYZItemLabelGenerator();
        //XYZDataItemSelection selection = new StandardXYZDataItemSelection();
        //generator.setItemSelection(selection);
        //renderer.setItemLabelGenerator(generator);
	}
	
	/**
	 * Returns some axis for a given 3d chart.
	 * @param index Axis index: x = 0, y = 1, z = 2.
	 * @param chart The 3d chart.
	 * @return The axis.
	 */
	public static ValueAxis3D getAxis(int index, Chart3D chart) {
        XYZPlot plot = (XYZPlot) chart.getPlot();
        if (index == 0) return plot.getXAxis();
        if (index == 1) return plot.getYAxis();
        if (index == 2) return plot.getZAxis();
        return null;
	}
	
	/**
	 * Shows a 3d chart or a drawable 3d on the screen.
	 * @param draw The 3d chart or drawable.
	 * @param w The width in pixels.
	 * @param h Height.
	 * @param winTitle Title of the window, or null to show no window on the screen.
	 * @param controls True to show panel controls.
	 * @return The 3d panel, will be an instance of {@linkplain Chart3DPanel} 
	 * if the input is a {@linkplain Chart3D} object.
	 */
	public static Panel3D showChart(Drawable3D draw, int w, int h, String winTitle, boolean controls) {
		if (draw instanceof Chart3D) {
			Chart3D chart = (Chart3D) draw;
	        Chart3DPanel chartPanel = new Chart3DPanel(chart);
	        chartPanel.setPreferredSize(new Dimension(w, h));
	        chartPanel.zoomToFit(new Dimension(w, h));
	
	        if (winTitle != null) {
				JFrame aframe = new JFrame(winTitle);
				aframe.setIconImage(new ImageIcon(getPicture(chart, w, h).getImage()).getImage());
				aframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				if (controls) {
					aframe.getContentPane().add(new DisplayPanel3D(chartPanel));
				} else {
					aframe.getContentPane().add(chartPanel);				
				}
				//aframe.setIconImage(this.chartAsBufferedImage(w, h));
				aframe.pack();
				aframe.setVisible(true);
	        }
			return chartPanel;
		}
		
        Panel3D chartPanel = new Panel3D(draw);
        chartPanel.setPreferredSize(new Dimension(w, h));
        //ViewPoint3D vp = new ViewPoint3D(new Point3D(chart3d.getxMax(), chart3d.getyMax(), chart3d.getzMax()), 0);
        //chartPanel.setViewPoint(vp);
        if (winTitle != null) {
			JFrame aframe = new JFrame(winTitle);
			aframe.setIconImage(new ImageIcon(getPicture(draw, w, h).getImage()).getImage());
			aframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			if (controls) {
				aframe.getContentPane().add(new DisplayPanel3D(chartPanel));
			} else {
				aframe.getContentPane().add(chartPanel);				
			}
			//aframe.setIconImage(this.chartAsBufferedImage(w, h));
			aframe.pack();
			aframe.setVisible(true);
        }
		return chartPanel;

	}
	
	/**
	 * Returns a picture of a given 3d chart or drawable.
	 * @param draw The 3d chart or drawable.
	 * @param width Width in pixels.
	 * @param height Height.
	 * @return The picture object.
	 */
	public static Picture getPicture(Drawable3D draw, int width, int height) {
		Picture pic = new Picture(width, height);
		Graphics2D g = pic.getImage().createGraphics();
		AWTGraphics.enableAntialiasing(g);
		if (draw instanceof Chart3D) ((Chart3D) draw).setElementHinting(true);
        draw.draw(g, new Rectangle(width, height));
        g.dispose();
        return pic;
	}
	
	/**
	 * Writes the chart to svg using FreeHEP library.
	 * @param path Path to the output file.
	 * @param chart The 3d chart or drawable.
	 * @param width Width in pixels.
	 * @param height Height.
	 * @throws JPARSECException If an error occurs.
	 */
    public static void writeChartAsSVG(String path, Drawable3D chart, int width, int height) 
    		throws JPARSECException {
		File plotFile = new File(path);

		final Dimension size = new Dimension(width, height);
		try
		{
			// Using reflection so that everything will work without freehep in classpath
			Class<?> c = Class.forName("org.freehep.graphicsio.svg.SVGGraphics2D");
			Constructor<?> cc = c.getConstructor(new Class[] { File.class, Dimension.class });
			Object svgGraphics = cc.newInstance(new Object[] { plotFile, size });
			Method m = c.getMethod("startExport");
			m.invoke(svgGraphics);
	        chart.draw((Graphics2D) svgGraphics, new Rectangle(width, height));
			Method mm = c.getMethod("endExport");
			mm.invoke(svgGraphics);
		} catch (Exception e) {
			throw new JPARSECException("cannot write to file.", e);
		}
    }
    
	@SuppressWarnings("serial")
	private static class HighlightXYZColorSource extends StandardXYZColorSource {
	    
	    @SuppressWarnings("rawtypes")
		private XYZDataset dataset;
	    
	    /** The range of x-values for the highlight region. */
	    private Range xRange;
	    
	    /** The range of y-values for the highlight region. */
	    private Range yRange;
	    
	    /** The range of z-values for the highlight region. */
	    private Range zRange;
	    
	    /** The highlight color. */
	    private Color highlightColor;
	    
	    /**
	     * Creates a new instance.
	     * 
	     * @param dataset  the dataset.
	     * @param highlightColor
	     * @param xRange
	     * @param yRange
	     * @param zRange
	     * @param colors 
	     */
	    @SuppressWarnings("rawtypes")
		public HighlightXYZColorSource(XYZDataset dataset, Color highlightColor, 
	            Range xRange, Range yRange, Range zRange, Color... colors) {
	        super(colors);
	        this.dataset = dataset;
	        this.xRange = xRange;
	        this.yRange = yRange;
	        this.zRange = zRange;
	        this.highlightColor = highlightColor;
	    }

	    @Override
	    public Color getColor(int series, int item) {
	        double x = this.dataset.getX(series, item);
	        double y = this.dataset.getY(series, item);
	        double z = this.dataset.getZ(series, item);
	        if (this.xRange.contains(x) && this.yRange.contains(y) 
	                && this.zRange.contains(z)) {
	            return this.highlightColor; 
	        } else {
	            return super.getColor(series, item);
	        }
	    }
	}
}
