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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.rmi.RemoteException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;

import jparsec.ephem.Functions;
import jparsec.graph.GridChartElement;
import jparsec.util.JPARSECException;
import jparsec.util.Logger;
import jparsec.util.Logger.LEVEL;
import jparsec.util.Translate;

import visad.CellImpl;
import visad.ColorControl;
import visad.ConstantMap;
import visad.DataReference;
import visad.DataReferenceImpl;
import visad.Display;
import visad.DisplayEvent;
import visad.DisplayImpl;
import visad.DisplayListener;
import visad.FlatField;
import visad.FunctionType;
import visad.GraphicsModeControl;
import visad.Gridded3DSet;
import visad.Linear2DSet;
import visad.MathType;
import visad.Real;
import visad.RealTupleType;
import visad.RealType;
import visad.SI;
import visad.ScalarMap;
import visad.Set;
import visad.VisADException;
//import visad.java2d.DisplayImplJ2D;
//import visad.java2d.DirectManipulationRendererJ2D;
import visad.java3d.DirectManipulationRendererJ3D;
import visad.java3d.DisplayImplJ3D;
import visad.util.SelectRangeWidget;
import visad.util.Util;
import visad.util.VisADSlider;

/**
 * A class to show cubes of data and surfaces using VisAD. A chart created with
 * VisAD can be rotated with the mouse. Zoom is possible by dragging the chart
 * with the mouse (like in the rotation) but with shift key pressed. A pan mode
 * is possible doing the same with Ctrl key pressed.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class CreateVISADChart implements DisplayListener, Serializable {

	private static final long serialVersionUID = 1;

  // Declare variables

  // The quantities to be displayed in x- and y-axes
  private RealType rightAscension, declination, velocity, flux;

  // lat and lon form a domain2D
  private RealTupleType domain2D;

  // (lat, lon, alt) form a domain2D
  private RealTupleType domain3D;

  // A Tuple of Reals (a subclass of VisAD Data)
  // which will hold cursor data.
  private Real cursorCoords;

  // and this FlatField will hold the surface
  private FlatField cubeFF;

  // The temperature plane
  private FlatField temperPlane;

  // The grey plane
  private Set greyPlane;

  // The DataReferences from the data to display
  private DataReferenceImpl cursorDataRef, cubeDataRef;
  private DataReferenceImpl greyPlaneRef, tPlaneDataRef;

  private Real nPoints;

  // data reference for the number of points
  private DataReference nPointsRef;

  // The 2D display, and its the maps
  /**
   * Holds the displays.
   */
  public DisplayImpl[] displays;
  private ScalarMap eastMap, northMap, altMap, rgbMap;
  private ScalarMap rangeX, rangeY, rangeZ;
  private double initVelocity;
  private VISADCubeElement cube;
  private VisADSlider latSlider;
  private boolean displaysAreLinked = true, showVPlane;
  private GridChartElement gridChart;
  private boolean surfaceMode = false;
  //private RealTuple cursorCoordinates;

  /**
   * Display panel.
   */
  public JPanel dispPanel;
  /**
   * GUI panel.
   */
  public JPanel guiPanel;
  /**
   * Complete panel.
   */
  public JPanel panel;
  /**
   * Constructor for a standard VISAD datacube using right ascension,
   * declination, and velocity.
   * @param initVelocity Initial velocity to show.
   * @param cube The cube to show.
   * @param showVPlane True to show a second panel with the flux
   * in a given velocity plain.
   * @throws JPARSECException If an exception occurs.
   */
  public CreateVISADChart (VISADCubeElement cube, double initVelocity, boolean showVPlane)
    throws JPARSECException {
	    start(cube, initVelocity, showVPlane);
  }

  /**
   * Constructor for a standard VISAD datacube using right ascension,
   * declination, and velocity.
   * @param cube The cube to show.
   * @throws JPARSECException If an exception occurs.
   */
  public CreateVISADChart (VISADCubeElement cube)
    throws JPARSECException {
	    start(cube, (cube.finalZ + cube.initZ) / 2.0, true);
  }

  /**
   * Updates the chart.
   * @param initVelocity Initial velocity to show.
   * @param cube The cube to show.
   * @throws JPARSECException If an exception occurs.
   */
  public void update(VISADCubeElement cube, double initVelocity)
  throws JPARSECException {
	  CreateVISADChart c = new CreateVISADChart(cube, initVelocity, showVPlane);
	  try {
		  cubeFF.setSamples(cube.getSamples(cube.getCube()), false);
		  rangeZ.setRange(Math.min(cube.initZ, cube.finalZ), Math.max(cube.initZ, cube.finalZ));
		  rangeX.setRange(Math.min(cube.initX, cube.finalX), Math.max(cube.initX, cube.finalX));
		  rangeY.setRange(Math.min(cube.initY, cube.finalY), Math.max(cube.initY, cube.finalY));
		  cursorDataRef.setData(c.cursorDataRef.getData());
		  cubeDataRef.setData(c.cubeDataRef.getData());
		  greyPlaneRef.setData(c.greyPlaneRef.getData());
		  double scale = (Math.max(cube.initZ, cube.finalZ)-Math.min(cube.initZ, cube.finalZ))/ (double) (cube.getNLevels()-1);
		  scale = scale / 100.0;
		  int min = (int) Functions.roundDownToPlace(Math.min(cube.initZ, cube.finalZ)/scale, 0);
		  int max = (int) Functions.roundUpToPlace(Math.max(cube.initZ, cube.finalZ)/scale, 0);
		  latSlider = new VisADSlider(velocity.getName(), min, max, 1, scale, cursorDataRef, declination, false);
		  this.setVelSliderValue((float) this.initVelocity);

		  this.cube = c.cube;
		  this.cube.initZ = cube.initZ;
		  this.cube.finalZ = cube.finalZ;
		  this.cube.initX = cube.initX;
		  this.cube.finalX = cube.finalX;
		  this.cube.initY = cube.initY;
		  this.cube.finalY = cube.finalY;
		  eastMap.setRange(cube.initX, cube.finalX);
		  northMap.setRange(cube.initY, cube.finalY);
		  altMap.setRange(cube.initZ, cube.finalZ);
		  displays[0].reAutoScale();
		  displays[0].reDisplayAll();
	  } catch (RemoteException exc)
	  {
		  throw new JPARSECException("remote exception.", exc);
	  } catch (Exception ex)
	  {
		  throw new JPARSECException("VisAD exception.", ex);
	  }
  }

  private void start(VISADCubeElement cube, double initVelocity, boolean showVPlane)
  throws JPARSECException{
	  try {
		  this.showVPlane = showVPlane;
		    // Create the quantities
		    rightAscension = cube.rightAscension;
		    declination = cube.declination;
		    velocity = cube.velocity;
		    flux = cube.flux;
		    this.initVelocity = initVelocity;
		    this.cube = cube;
		    if (showVPlane) {
		    	init();
		    } else {
		    	init2();
		    }
		  } catch (RemoteException exc)
		  {
			  throw new JPARSECException("remote exception.", exc);
		  }
		  catch (VisADException ex)
		  {
			  throw new JPARSECException("VisAD exception.", ex);
		  }
  }

    private void init()
    throws RemoteException, VisADException {
    //...the domain2D
    domain2D = new RealTupleType(rightAscension, declination);

    // and the domain3D
    domain3D = new RealTupleType(rightAscension, declination, velocity);

    // The cursor
    cursorCoords  = new Real(velocity, Math.min(cube.initZ, cube.finalZ));

    // Create the DataReference
    cursorDataRef = new DataReferenceImpl("cursorDataRef");

    // ...and initialize it with the RealTuple
    cursorDataRef.setData( cursorCoords );

    // More Data: create a Cube object and get its data
    cubeFF = cube.getData();

    cubeDataRef = new DataReferenceImpl("cubeDataRef");
    cubeDataRef.setData(cubeFF);

    // number of points as a real and its reference
    nPoints = new Real(100.0);
    nPointsRef = new DataReferenceImpl("nPointsRef");
    nPointsRef.setData(nPoints);

    // Create the white line
    // with so many points
    int numberOfPoints = (int) nPoints.getValue();

    // we really want to simplify and tak the root of numberOfPoints
    // this is then the number per dimension:
    greyPlane = makePlaneSet(initVelocity, numberOfPoints*numberOfPoints, cube.initY*cube.overScanZ, cube.finalY*cube.overScanZ);

    // Create the line's data ref and set data
    greyPlaneRef = new DataReferenceImpl("greyPlaneRef");
    greyPlaneRef.setData(greyPlane);

    // Create the temperature plane to be shown on display
    temperPlane = (FlatField) cubeFF.resample(greyPlane);

    // create and set data reference
    tPlaneDataRef = new DataReferenceImpl("tPlaneDataRef");
    tPlaneDataRef.setData(temperPlane);

    CellImpl cell = new CellImpl() {
      public void doAction() throws RemoteException, VisADException {

        // get the data object from the reference. We know it's a RealTuple
        Real lat = (Real) cursorDataRef.getData();

        // test if cursor postion (northing) has changed significantly
        if( Util.isApproximatelyEqual(  lat.getValue(),
                                        cursorCoords.getValue(),
                                        0.1 ) &&

            Util.isApproximatelyEqual(  nPoints.getValue(),
                                        ((Real) nPointsRef.getData()).getValue(),
                                        1 )

         ){

          return; // leave method and thus don't update line
        }

        double latValue = lat.getValue();
        // make a new line for display 1: will have only 2 points
        int nOfPoints = 100;
        greyPlane = makePlaneSet(latValue, nOfPoints*nOfPoints, cube.initY*cube.overScanZ, cube.finalY*cube.overScanZ);

        // Re-set Data, will update display
        greyPlaneRef.setData(greyPlane);

        // now create a larger white line set to compute the temperature line
        nOfPoints = (int) ((Real) nPointsRef.getData()).getValue();
        nOfPoints = (int) Math.sqrt((double)nOfPoints);

        greyPlane = makePlaneSet(latValue, nOfPoints*nOfPoints, cube.initX*cube.overScanZ, cube.finalX*cube.overScanZ);

        // function will have this type
        String funcStr = "( ("+rightAscension.getName()+", "+declination.getName()+", "+velocity.getName()+") -> "+flux.getName()+" )";

        // create Function (FlatField) and set the data
        temperPlane =  new FlatField( (FunctionType) MathType.stringToType(  funcStr ),
                                      greyPlane );

        temperPlane.setSamples( cubeFF.resample( greyPlane ).getFloats(false), false);
        // and update ist data reference -> will update display
        tPlaneDataRef.setData(temperPlane);

        // assign current cursor position to old cursor position
        cursorCoords = lat;
      }
    };

    // link cursor to cell
    // so that doAction gets called whenever cursor moves
    cell.addReference(cursorDataRef);
    cell.addReference(nPointsRef);

    // create a slider, to show and control northing values
    double scale = (Math.max(cube.initZ, cube.finalZ)-Math.min(cube.initZ, cube.finalZ))/ (double) (cube.getNLevels()-1);
    scale = scale / 100.0;
    int min = (int) Functions.roundDownToPlace(Math.min(cube.initZ, cube.finalZ)/scale, 0);
    int max = (int) Functions.roundUpToPlace(Math.max(cube.initZ, cube.finalZ)/scale, 0);
    latSlider = new VisADSlider(velocity.getName(), min, max, 1, scale, cursorDataRef, declination, false);
    this.setVelSliderValue((float) this.initVelocity);

     // this slider will control the number of points
    VisADSlider pointsSlider = new VisADSlider(nPointsRef, 1000, 50000, 10000, RealType.Generic, Translate.translate(945)); //"Points in plane");

    // Create the Displays and their maps

    // Two 2D displays
    displays = new DisplayImpl[2];

    for( int i = 0; i<2;i++){
      displays[i] = new DisplayImplJ3D("display" + i);
    }

    // Get display's graphics mode control draw scales
    for( int i = 0; i<2;i++){
      GraphicsModeControl dispGMC = displays[i].getGraphicsModeControl();
      dispGMC.setScaleEnable(true);
    }

    displays[1].getGraphicsModeControl().setTextureEnable(false);

    // Create the ScalarMaps
    eastMap = new ScalarMap( rightAscension, Display.XAxis );
    northMap = new ScalarMap( declination, Display.YAxis );
    altMap = new ScalarMap( velocity, Display.ZAxis );
    rgbMap = new ScalarMap( flux, Display.RGB );

    // Set maps ranges
    eastMap.setRange(cube.initX, cube.finalX);
    northMap.setRange(cube.initY, cube.finalY);
    altMap.setRange(cube.initZ, cube.finalZ);

    rangeX = new ScalarMap(rightAscension, Display.SelectRange );
    rangeY = new ScalarMap(declination, Display.SelectRange );
    rangeZ = new ScalarMap(velocity, Display.SelectRange );
    rangeZ.setRange(Math.min(cube.initZ, cube.finalZ), Math.max(cube.initZ, cube.finalZ));

    // Add maps to display
    displays[0].addMap( eastMap );
    displays[0].addMap( northMap );
    displays[0].addMap( rgbMap );
    displays[0].addMap( altMap );

    displays[0].addMap( rangeX );
    displays[0].addMap( rangeY );
    displays[0].addMap( rangeZ );

   // Copy those maps and add to second display

    // but choose only two of the maps
    displays[1].addMap( (ScalarMap) eastMap.clone() );
    displays[1].addMap( (ScalarMap) northMap.clone() );

    // set ranges of rgb's and z-axis' maps of 2nd display
    // so that they math the ranges of 1st display
    ScalarMap anotherRGBMap = (ScalarMap) rgbMap.clone();
    double[] rgbRange = rgbMap.getRange();
    anotherRGBMap.setRange(rgbRange[0],rgbRange[1]);
    displays[1].addMap( anotherRGBMap );

    ScalarMap anotherZMap = (ScalarMap) altMap.clone();
    anotherZMap.setRange(Math.min(cube.initZ, cube.finalZ), Math.max(cube.initZ, cube.finalZ));
    displays[1].addMap( anotherZMap );

    // Also create constant maps to define cursor size, color, etc...
    ConstantMap[] cMaps = { new ConstantMap( 1.0f, Display.Red ),
                           new ConstantMap( 1.0f, Display.Green ),
                           new ConstantMap( 1.0f, Display.Blue ),
                           new ConstantMap( -1.0f, Display.XAxis ),
                           new ConstantMap( -1.0f, Display.YAxis ),
                           new ConstantMap( 3.50f, Display.PointSize )  };

    // ...and constant maps to make cutting plane grey and transparent
    ConstantMap[] wLineMaps = {  new ConstantMap( 0.75f, Display.Red ),
                                 new ConstantMap( 0.75f, Display.Green ),
                                 new ConstantMap( 0.75f, Display.Blue ),
                                 new ConstantMap( 0.75f, Display.Alpha ) };

    // Now Add reference to display
    // But using a direct manipulation renderer

    // display 1
    displays[0].addReferences( new DirectManipulationRendererJ3D(), cursorDataRef, cMaps );

    displays[0].addReference(cubeDataRef);

    displays[0].addReference(greyPlaneRef, wLineMaps);

    // display 2
    displays[1].addReference(tPlaneDataRef, null);

    displays[0].addDisplayListener(this);

    // Create application window, put display into it
    panel = new JPanel();
    panel.setLayout(new BorderLayout());

    dispPanel = new JPanel( new GridLayout(1,2) );
    dispPanel.add(displays[0].getComponent());
    dispPanel.add(displays[1].getComponent());
    panel.add(dispPanel, BorderLayout.CENTER);

    guiPanel = new JPanel( new GridLayout(1,2) );
    guiPanel.add(latSlider);
    guiPanel.add(pointsSlider);

    JPanel guiPanel2 = new JPanel( new GridLayout(1,2) );
    guiPanel2.add(createRangeSliders());
    JPanel pp = new JPanel();
    pp.add(createSyncCheck());
    pp.add(createResetButton());
    guiPanel2.add(pp);

    JPanel southPanel = new JPanel();
    southPanel.setLayout(new BoxLayout(southPanel,BoxLayout.Y_AXIS));
    southPanel.add(guiPanel);
    southPanel.add(guiPanel2);

    panel.add(southPanel, BorderLayout.SOUTH);
  }

    private JButton createResetButton(){

        final JButton button = new JButton(Translate.translate(926)); //"Reset Display");

        button.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            try {
              displays[0].getProjectionControl().resetProjection();
            } catch (Exception ex) {    }
	        panel.requestFocusInWindow();
          }
        });

        return button;
      }

  private Set makePlaneSet( double altitudeValue, int pointsPerPlane, float init, float end)
    throws VisADException, RemoteException
    {
      // arbitrary easting end values of the line
      float lowVal =  init;
      float hiVal =  end;

      float[][] domain2DSamples = new float[3][pointsPerPlane];

      int ptsPerDim = (int) Math.sqrt((double)pointsPerPlane);

      for(int x=0;x<ptsPerDim*ptsPerDim;x++){
        domain2DSamples[2][x] = (float) altitudeValue;
      }

      Linear2DSet tempor2DSet = new Linear2DSet(domain2D, lowVal,hiVal,ptsPerDim,lowVal,hiVal,ptsPerDim);
      domain2DSamples[0] = tempor2DSet.getSamples(false)[0];
      domain2DSamples[1] = tempor2DSet.getSamples(false)[1];

      return new Gridded3DSet( domain3D, domain2DSamples, ptsPerDim,ptsPerDim);
  }

  private Component createRangeSliders(){
    JPanel p = new JPanel();
    p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
    try {
      p.add( new SelectRangeWidget( rangeX ) );
      p.add( new SelectRangeWidget( rangeY ) );
      p.add( new SelectRangeWidget( rangeZ ) );
    }
    catch (Exception ex) {   }
    return p;
  }

  private JCheckBox createSyncCheck(){
	    JCheckBox cb = new JCheckBox(Translate.translate(927), true); //"Link Displays",true);
	    cb.addItemListener(new ItemListener() {
	      public void itemStateChanged(ItemEvent e) {
	        displaysAreLinked = (e.getStateChange() == ItemEvent.SELECTED);
	        panel.requestFocusInWindow();
	      }
	    });
	    return cb;
	  }

  /**
   * Synchronizes the displays if necessary.
   */
  public void displayChanged(DisplayEvent e)
  throws VisADException, RemoteException {
	  if (e.getId() == DisplayEvent.FRAME_DONE) {
	    doSynchronize();
	  }
}

  private void doSynchronize(){
	  if (panel.isFocusOwner()) {
		  panel.requestFocusInWindow();
	  } else {
		  if (surfaceMode && panel.getX() == 0 && panel.getWidth() == panel.getParent().getWidth() && panel.getHeight() == panel.getParent().getHeight())
			  panel.requestFocusInWindow();
	  }
	    try{
	      if(displaysAreLinked){
	        displays[1].getProjectionControl().setMatrix(displays[0].getProjectionControl().getMatrix());
	      }
	    } catch(Exception ex){  }
	  }

  /**
   * Shows the chart in a JFrame.
   * @param width Width in pixels.
   * @param height Height in pixels.
   */
  public void show(int width, int height)
  {
		JFrame aframe = new JFrame("");
		aframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		aframe.getContentPane().add(panel);
		aframe.setPreferredSize(new Dimension(width, height));
		aframe.pack();
		aframe.setVisible(true);
  }

  /**
   * Sets the color table of the VISAD chart to the same color
   * table as a grid chart.
   * @param chart The grid chart object.
   */
  public void setColorTable(GridChartElement chart) {
	  // Create a different color table
	  // Note: table has red, green and blue components
	  int tableLength = chart.colorModelResolution;
	  float[][] myColorTable = new float[3][tableLength];
	  for(int i=0;i<tableLength;i++){
	    myColorTable[0][i]= chart.getColorModelRedArray()[i];
	    myColorTable[1][i]= chart.getColorModelGreenArray()[i];
	    myColorTable[2][i]= chart.getColorModelBlueArray()[i];
	  }

	  // Get the ColorControl from the altitude RGB map
	  ColorControl colCont = (ColorControl) rgbMap.getControl();

	  // Set the table
	  try {
		colCont.setTable(myColorTable );
	  } catch (Exception exc) {
			Logger.log(LEVEL.ERROR, "Error setting the table. Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
	  }
  }

  /**
   * Constructor for a 3d surface. The labels for x, y, and z axis (legend) should
   * not contain any strange characters (spaces, point, comma, ...).
   * @param chart The surface element object.
   * @throws JPARSECException If an error occurs.
   */
	  public CreateVISADChart (GridChartElement chart)
	  throws JPARSECException
	  {
		  surfaceMode = true;
		  gridChart = chart.clone();
		  init(gridChart);
	  }
	  private void init(GridChartElement chart) throws JPARSECException
	  {
		  try {
			  // Create the quantities
			  // Use RealType(String name, Unit unit, Set set);
			  declination = RealType.getRealType(chart.yLabel, SI.meter, null);
			  rightAscension = RealType.getRealType(chart.xLabel, SI.meter, null);
			  domain2D = new RealTupleType(declination, rightAscension);
			  velocity = RealType.getRealType(chart.legend, null, null);

			  // Create a FunctionType (domain_tuple -> range_tuple )
			  // Use FunctionType(MathType domain, MathType range)
			  FunctionType func_domain_alt = new FunctionType( domain2D, velocity);

			  // Create the domain Set using an
			  // LinearDSet(MathType type, double first1, double last1, int lengthX,
			  //				 double first2, double last2, int lengthY)
			  // note the "inverted" first and last values of latitude
			  int NCOLS = chart.data[0].length;
			  int NROWS = chart.data.length;
			  greyPlane = new Linear2DSet(domain2D, chart.limits[2], chart.limits[3], NROWS,
			  					          chart.limits[0],  chart.limits[1], NCOLS);

			  // Our 'flat' array
			  double[][] flat_samples = new double[1][NCOLS * NROWS];

			  // Fill our 'flat' array with the altitude values
			  // by looping over NCOLS and NROWS

			  // Note the use of an index variable, indicating the order of the samples
			  int index = 0;
			  for(int c = 0; c < NCOLS; c++)
			    for(int r = 0; r < NROWS; r++){

				      // set altitude altitude
				      flat_samples[0][ index ] = chart.data[r][c];

				      // increment index
				      index++;
			    }

			  // Create a FlatField
			  // Use FlatField(FunctionType type, Set domain_set)
			  temperPlane = new FlatField( func_domain_alt, greyPlane);

			  // ...and put the altitude values above into it
			  // Note the argument false, meaning that the array won't be copied
			  temperPlane.setSamples( flat_samples , false );

			  // Create Display and its maps
			  displays = new DisplayImpl[1];
			  displays[0] = new DisplayImplJ3D("display1");

			  // Get display's graphics mode control and draw scales
			  GraphicsModeControl dispGMC = displays[0].getGraphicsModeControl();
			  dispGMC.setScaleEnable(true);

			  // Also enable Texture
			  dispGMC.setTextureEnable(false);

			  // Create the ScalarMaps: latitude to XAxis, longitude to YAxis and
			  // altitude to ZAxis and to RGB
			  // Use ScalarMap(ScalarType scalar, DisplayRealType display_scalar)
			  northMap = new ScalarMap( declination,    Display.YAxis );
			  eastMap = new ScalarMap( rightAscension, Display.XAxis );

			  rgbMap = new ScalarMap( velocity,  Display.RGB );
			  altMap = new ScalarMap( velocity,  Display.ZAxis );

			  // Add maps to display
			  displays[0].addMap( northMap );
			  displays[0].addMap( eastMap );

			  displays[0].addMap( altMap );
			  displays[0].addMap( rgbMap );

			  // Create a different color table
			  setColorTable(chart);

			  // Create a data reference and set the FlatField as our data
			  greyPlaneRef = new DataReferenceImpl("greyPlaneRef");
			  greyPlaneRef.setData( temperPlane );

			  if (chart.opacity == GridChartElement.OPACITY.VARIABLE_WITH_Z) {
				  ScalarMap altAlphaMap = new ScalarMap( velocity,  Display.Alpha );
					displays[0].addMap( altAlphaMap );
					displays[0].addReference( greyPlaneRef);
			  } else {
				  float opacity = 0.25f;
				  if (chart.opacity == GridChartElement.OPACITY.OPAQUE) opacity = 1f;
				  if (chart.opacity == GridChartElement.OPACITY.SEMI_TRANSPARENT) opacity = 0.5f;
				  ConstantMap[] constAlpha_CMap = { new ConstantMap( opacity, Display.Alpha) };
				  displays[0].addReference( greyPlaneRef, constAlpha_CMap);
			  }

			  displays[0].addDisplayListener(this);

			  // Set maps ranges
			  eastMap.setRange(chart.limits[0], chart.limits[1]);
			  northMap.setRange(chart.limits[2], chart.limits[3]);
			  altMap.setRange(chart.getMinimum(), chart.getMaximum());

			  // Create application window and add display to window
			  panel = new JPanel();
			  panel.setLayout(new BorderLayout());
			  panel.add(displays[0].getComponent());
		  } catch (RemoteException exc)
		  {
			  throw new JPARSECException("remote exception.", exc);
		  }
		  catch (VisADException ex)
		  {
			  throw new JPARSECException("VisAD exception.", ex);
		  }
	}

	  /**
	   * Returns the VISAD cube object.
	   * @return VISAD cube object.
	   */
	  public VISADCubeElement getCube()
	  {
		  return this.cube;
	  }
  private void init2()
  throws RemoteException, VisADException {
  //...the domain2D
  domain2D = new RealTupleType(rightAscension, declination);

  // and the domain3D
  domain3D = new RealTupleType(rightAscension, declination, velocity);

  // The cursor
  cursorCoords  = new Real(velocity, Math.min(cube.initZ, cube.finalZ));

  // Create the DataReference
  cursorDataRef = new DataReferenceImpl("cursorDataRef");

  // ...and initialize it with the RealTuple
  cursorDataRef.setData( cursorCoords );

  // More Data: create a Cube object and get its data
  cubeFF = cube.getData();

  cubeDataRef = new DataReferenceImpl("cubeDataRef");
  cubeDataRef.setData(cubeFF);

  // number of points as a real and its reference
  nPoints = new Real(100.0);
  nPointsRef = new DataReferenceImpl("nPointsRef");
  nPointsRef.setData(nPoints);

  // Create the white line
  // with so many points
  int numberOfPoints = (int) nPoints.getValue();

  // we really want to simplify and tak the root of numberOfPoints
  // this is then the number per dimension:
  greyPlane = makePlaneSet(initVelocity, numberOfPoints*numberOfPoints, cube.initY*cube.overScanZ, cube.finalY*cube.overScanZ);

  // Create the line's data ref and set data
  greyPlaneRef = new DataReferenceImpl("greyPlaneRef");
  greyPlaneRef.setData(greyPlane);

  // Create the temperature plane to be shown on display
  temperPlane = (FlatField) cubeFF.resample(greyPlane);

  // create and set data reference
  tPlaneDataRef = new DataReferenceImpl("tPlaneDataRef");
  tPlaneDataRef.setData(temperPlane);

  CellImpl cell = new CellImpl() {
    public void doAction() throws RemoteException, VisADException {

      // get the data object from the reference. We know it's a RealTuple
      Real lat = (Real) cursorDataRef.getData();

      // test if cursor postion (northing) has changed significantly
      if( Util.isApproximatelyEqual(  lat.getValue(),
                                      cursorCoords.getValue(),
                                      0.1 ) &&

          Util.isApproximatelyEqual(  nPoints.getValue(),
                                      ((Real) nPointsRef.getData()).getValue(),
                                      1 )

       ){

        return; // leave method and thus don't update line
      }

      double latValue = lat.getValue();
      // make a new line for display 1: will have only 2 points
      int nOfPoints = 100;
      greyPlane = makePlaneSet(latValue, nOfPoints*nOfPoints, cube.initY*cube.overScanZ, cube.finalY*cube.overScanZ);

      // Re-set Data, will update display
      greyPlaneRef.setData(greyPlane);

      // now create a larger white line set to compute the temperature line
      nOfPoints = (int) ((Real) nPointsRef.getData()).getValue();
      nOfPoints = (int) Math.sqrt((double)nOfPoints);

      greyPlane = makePlaneSet(latValue, nOfPoints*nOfPoints, cube.initX*cube.overScanZ, cube.finalX*cube.overScanZ);

      // function will have this type
      String funcStr = "( ("+rightAscension.getName()+", "+declination.getName()+", "+velocity.getName()+") -> "+flux.getName()+" )";

      // create Function (FlatField) and set the data
      temperPlane =  new FlatField( (FunctionType) MathType.stringToType(  funcStr ),
                                    greyPlane );

      temperPlane.setSamples( cubeFF.resample( greyPlane ).getFloats(false), false);
      // and update ist data reference -> will update display
      tPlaneDataRef.setData(temperPlane);

      // assign current cursor position to old cursor position
      cursorCoords = lat;
    }
  };

  // link cursor to cell
  // so that doAction gets called whenever cursor moves
  cell.addReference(cursorDataRef);
  cell.addReference(nPointsRef);

  // create a slider, to show and control northing values
  double scale = (Math.max(cube.initZ, cube.finalZ)-Math.min(cube.initZ, cube.finalZ)) / (double) (cube.getNLevels()-1);
  scale = scale / 100.0;
  int min = (int) Functions.roundDownToPlace(Math.min(cube.initZ, cube.finalZ)/scale, 0);
  int max = (int) Functions.roundUpToPlace(Math.max(cube.initZ, cube.finalZ)/scale, 0);
  latSlider = new VisADSlider(velocity.getName(), min, max, 1, scale, cursorDataRef, declination, false);
  this.setVelSliderValue((float) this.initVelocity);

  // Create the Displays and their maps

  // One 2D display
  displays = new DisplayImpl[1];
  displaysAreLinked = false;

  for( int i = 0; i<displays.length;i++){
    displays[i] = new DisplayImplJ3D("display" + i);
  }

  // Get display's graphics mode control draw scales
  for( int i = 0; i<displays.length;i++){
    GraphicsModeControl dispGMC = displays[i].getGraphicsModeControl();
    dispGMC.setScaleEnable(true);
  }

  // Create the ScalarMaps
  eastMap = new ScalarMap( rightAscension, Display.XAxis );
  northMap = new ScalarMap( declination, Display.YAxis );
  altMap = new ScalarMap( velocity, Display.ZAxis );
  rgbMap = new ScalarMap( flux, Display.RGB );

  // Set maps ranges
  eastMap.setRange(cube.initX, cube.finalX);
  northMap.setRange(cube.initY, cube.finalY);
  altMap.setRange(cube.initZ, cube.finalZ);

  rangeX = new ScalarMap(rightAscension, Display.SelectRange );
  rangeY = new ScalarMap(declination, Display.SelectRange );
  rangeZ = new ScalarMap(velocity, Display.SelectRange );
  rangeZ.setRange(Math.min(cube.initZ, cube.finalZ), Math.max(cube.initZ, cube.finalZ));

  // Add maps to display
  displays[0].addMap( eastMap );
  displays[0].addMap( northMap );
  displays[0].addMap( rgbMap );
  displays[0].addMap( altMap );

  displays[0].addMap( rangeX );
  displays[0].addMap( rangeY );
  displays[0].addMap( rangeZ );

  // Also create constant maps to define cursor size, color, etc...
  ConstantMap[] cMaps = { new ConstantMap( 1.0f, Display.Red ),
                         new ConstantMap( 1.0f, Display.Green ),
                         new ConstantMap( 1.0f, Display.Blue ),
                         new ConstantMap( -1.0f, Display.XAxis ),
                         new ConstantMap( -1.0f, Display.YAxis ),
                         new ConstantMap( 3.50f, Display.PointSize )  };

  // ...and constant maps to make cutting plane grey and transparent
  ConstantMap[] wLineMaps = {  new ConstantMap( 0.75f, Display.Red ),
                               new ConstantMap( 0.75f, Display.Green ),
                               new ConstantMap( 0.75f, Display.Blue ),
                               new ConstantMap( 0.75f, Display.Alpha ) };

  // Now Add reference to display
  // But using a direct manipulation renderer

  displays[0].addReferences( new DirectManipulationRendererJ3D(), cursorDataRef, cMaps );
  displays[0].addReference(cubeDataRef);
  displays[0].addReference(greyPlaneRef, wLineMaps);
  displays[0].addDisplayListener(this);

  // Create application window, put display into it
  panel = new JPanel();
  panel.setLayout(new BorderLayout());

  dispPanel = new JPanel(new GridLayout(1,1));
  dispPanel.add(displays[0].getComponent());
  panel.add(dispPanel, BorderLayout.CENTER);

  guiPanel = new JPanel();
  guiPanel.add(latSlider);
  guiPanel.add(createRangeSliders());
  guiPanel.add(createSyncCheck());
  guiPanel.add(createResetButton());

  panel.add(guiPanel, BorderLayout.SOUTH);
}

  /**
   * Returns the velocity slider.
   * @return Velocity slider.
   */
  public VisADSlider getVelSlider()
  {
	  return this.latSlider;
  }
  /**
   * Returns the velocity slider value.
   * @return Velocity slider value.
   */
  public float getVelSliderValue()
  {
	  JSlider sl = (JSlider) (latSlider.getComponent(0));
	  int v = sl.getValue();
	  float init = Math.min(this.getCube().initZ, this.getCube().finalZ);
	  float fin = Math.max(this.getCube().initZ, this.getCube().finalZ);
	  double step = (fin - init) / (double) sl.getMaximum();
	  float vel = init + (float) (step * (double) v);
	  return vel;
  }
  /**
   * Sets the velocity slider value.
   * @param v Velocity slider value.
   */
  public void setVelSliderValue(float v)
  {
	  JSlider sl = (JSlider) (latSlider.getComponent(0));
	  float init = Math.min(this.getCube().initZ, this.getCube().finalZ);
	  float fin = Math.max(this.getCube().initZ, this.getCube().finalZ);
	  double step = (fin - init) / (double) sl.getMaximum();
	  int vel = (int) ((v-init)/step + 0.5);
	  sl.setValue(vel);
  }

  /**
   * Return X range of the map.
   * @return X range.
   */
  public ScalarMap getRangeX()
  {
	  return this.rangeX;
  }
  /**
   * Return Y range of the map.
   * @return Y range.
   */
  public ScalarMap getRangeY()
  {
	  return this.rangeY;
  }
  /**
   * Return Z range of the map.
   * @return Z range.
   */
  public ScalarMap getRangeZ()
  {
	  return this.rangeZ;
  }

 	private void writeObject(ObjectOutputStream out)
	throws IOException {
 		boolean grid3d = false;
 		if (gridChart != null) grid3d = true;
		out.writeBoolean(grid3d);
		if (grid3d) {
			out.writeObject(this.gridChart);
		} else {
			out.writeFloat(this.getVelSliderValue());
			out.writeObject(this.cube);
			out.writeBoolean(showVPlane);
		}
	}
	private void readObject(ObjectInputStream in)
	throws IOException, ClassNotFoundException {
		boolean grid3d = in.readBoolean();
		if (grid3d) {
			this.gridChart = (GridChartElement) in.readObject();
			surfaceMode = true;
			try {
				init(gridChart);
			} catch (Exception exc) {
				throw new IOException("file cannot be read");
			}
		} else {
			float v = in.readFloat();
			this.cube = (VISADCubeElement) in.readObject();
			showVPlane = in.readBoolean();
			try {
				start(cube, v, showVPlane);
				this.setVelSliderValue(v);
			} catch (Exception exc) {
				throw new IOException("file cannot be read");
			}
		}
 	}

	/**
	 * Returns true if the velocity plane is shown in the
	 * VISAD chart. Null if a 3d chart is shown.
	 * @return True or false.
	 */
	public boolean isVPlaneShown() {
		return this.showVPlane;
	}
	/**
	 * Returns the chart object. A {@linkplain GridChartElement}
	 * for a 3d chart, else a {@linkplain VISADCubeElement} object.
	 * @return Chart object.
	 */
	public Object getChartElement() {
		if (gridChart != null) return gridChart;
		return cube;
	}
	/**
	 * Returns the JPanel with this component.
	 * @return The VISAD panel.
	 */
	public JPanel getComponent() {
		return panel;
	}
}
