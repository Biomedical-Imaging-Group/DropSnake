// **********************************************************************
// COPYRIGHT NOTICE AND DISCLAIMER
//
// Copyright (c) by Mathews Jacob. All rights reserved.
//
// Permission to copy and use this software and accompanying
// documentation for educational, research, and not-for-profit purposes,
// without fee and without a signed licensing agreement, is hereby
// granted, provided that the above copyright notice, this paragraph and
// the following two paragraphs appear in all copies. The copyright
// holder is free to make upgraded or improved versions of the software
// available for a fee or commercially only. Contact the copyright
// holder (Mathews.Jacob@ieee.org) for commercial licensing
// opportunities.
//
// IN NO EVENT SHALL THE COPYRIGHT HOLDER BE LIABLE TO ANY PARTY FOR
// DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, OF
// ANY KIND WHATSOEVER, ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS
// DOCUMENTATION, EVEN IF HE HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH
// DAMAGE.
//
// THE COPYRIGHT HOLDER SPECIFICALLY DISCLAIMS ANY WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
// AND ACCOMPANYING DOCUMENTATION IS PROVIDED "AS IS". THE COPYRIGHT
// HOLDER HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
// ENHANCEMENTS, OR MODIFICATIONS.
//
// **********************************************************************


/*====================================================================
| Additional help available at http://bigwww.epfl.ch/jacob/splineSnake/
\===================================================================*/
package dropsnake;

import ij.*;
import ij.gui.*;
import ij.measure.Calibration;
import ij.plugin.PlugIn;

import ij.gui.GUI;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.WindowManager;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Event;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;
import ij.text.*;

/*====================================================================
|	splineSnakeHandler
|   This class is responsible for dealing with the list of point
|   coordinates and for their visual appearance.
\===================================================================*/

public class splineSnakeHandler
	extends
		Roi
{
private splineSnakeAction pa;
public splineSnakeToolbar tb;
public SplineSnakePreferences preferences;

private int currentPoint = -1;
public boolean started = false;


public final Vector listConstraints = new Vector(0, 16);
public final Vector listBuffer = new Vector(0, 16);
public final Vector listKnots = new Vector(0, 16);
public final Vector listPixels = new Vector(0, 16);

public int closest = -1;
public ImagePlus imp;
public boolean curveComputed = false;
public boolean curveClosed = false;
public Point lastPointEntered;
public boolean tempPointAdded= false;
public boolean copyEntireBuffer = false;
public boolean enterPointsOn = true;

public DisplayCurve Initialcurve;
public Curve InitialClosedCurve;
public Curve FinalClosedCurve;
public ImageDisplay imageD;
public boolean Automatic=false;
public TextWindow CATextWindow;
public int IterationsCounter;
private double CALENGTH=75;

/*********************************************************************
* This constructor stores a local copy of its parameters. It also creates 
* the object that takes care of
* the interactive work.
/********************************************************************/
public splineSnakeHandler (
	final ImagePlus imp,
	final splineSnakeToolbar tb
) {
	super(0, 0, imp.getWidth(), imp.getHeight(), imp);
	this.imp = imp;
	this.tb = tb;
	pa = new splineSnakeAction(imp, this, tb);
	final ImageCanvas ic = imp.getWindow().getCanvas();
		
	ic.removeKeyListener(IJ.getInstance());
	ic.removeMouseListener(ic);
	ic.removeMouseMotionListener(ic);
	ic.addMouseMotionListener(pa);
	ic.addMouseListener(pa);
	ic.addKeyListener(pa);
	imp.setRoi(this);
	
	preferences = new SplineSnakePreferences(this);
	imageD = new ImageDisplay(imp,preferences.GUISigma);
					
	tb.setTool(splineSnakeAction.ADD_CROSS);
	started = true;
	
	CALENGTH = 0.2*Math.sqrt(imp.getWidth()*imp.getWidth() +imp.getHeight()*imp.getHeight());
	
	CATextWindow=new TextWindow("Final curves", "Measure	CA[°] L	CA[°] R	Meridian Surf.[mm2]	Meridian Length[mm]	Contac Radius[mm]	Interface tilt[°]	Knot Spacing L R [pix] ","", 500,300);
	
} /* end splineSnakeHandler */


/*********************************************************************
 * This method adds a new constraint to the list. The
 * points are stored in pixel units rather than canvas units to cope
 * for different zooming factors.
 *
********************************************************************/
public void addPoint (
	final int x,
	final int y
) {
		final Point p = new Point(x, y);
		listConstraints.addElement(p);
		if(listConstraints.size()==1)
			addSubPoint(x,y);
		else
			{
				if(tempPointAdded)
					{
						tempPointAdded = false;
						listBuffer.removeElementAt(listBuffer.size()-1);
					}
				listBuffer.addElement(p);
				copyEntireBuffer = true;
			}
} /* end addPoint */

/*********************************************************************
 * This method adds a new constraint to the list. The
 * points are stored in pixel units rather than canvas units to cope
 * for different zooming factors.
 *
********************************************************************/
public void addConstraint (
	final int x,
	final int y
) {
		final Point p = new Point(x, y);
		listConstraints.addElement(p);
} /* end addConstraint */

/*********************************************************************
 * This method adds a new knot to the list
 ********************************************************************/
public void addSubPoint (
	final int x,
	final int y
) {
		final Point p = new Point(x, y);
		listKnots.addElement(p);
		
} /* end addSubPoint */


/*********************************************************************
 * This method adds a new point to the Buffer
 ********************************************************************/
public void addPointsInBuffer (
	final int x,
	final int y,
	boolean doubleclick
) {
		
		double increment = Math.sqrt((x-lastPointEntered.x)*(x-lastPointEntered.x) + (y-lastPointEntered.y)*(y-lastPointEntered.y));
		int test = (int)(increment/(preferences.KnotInerval));
		if(test >0 && listKnots.size() >0 && preferences.autoKnots)
				{
					if(tempPointAdded && listBuffer.size()>0)
						listBuffer.removeElementAt(listBuffer.size()-1);
					
					double incx = (x - lastPointEntered.x)/((double)test);
					double incy = (y - lastPointEntered.y)/((double)test);
					double newx = lastPointEntered.x;
					double newy = lastPointEntered.y;
						
					if(!doubleclick)		//do not insert several points after a double click
						for(int i=1; i<=test; i++)
						{
							newx = newx+incx; newy = newy+incy;
							final Point p1 = new Point((int)(newx+0.5), (int)(newy+0.5));
							listBuffer.addElement(p1);
						}
						
						
						
					final Point p1 = new Point((int)(newx+0.5), (int)(newy+0.5));
					lastPointEntered = p1;
					tempPointAdded = false;
					updateInitialCurve(true);
				}
			else
				{	
					if(tempPointAdded)
						listBuffer.removeElementAt(listBuffer.size()-1);
					final Point p1 = new Point(x, y);
					listBuffer.addElement(p1);
					tempPointAdded = true;
				}
} 


/*********************************************************************
 * This method adds a new Pixel to the list
 ********************************************************************/
public void addPixel (
	final int x,
	final int y
) {
		final Point p = new Point(x, y);
		listPixels.addElement(p);
		
} /* end addPoint */

/*********************************************************************
 * Modify the location of the current Knot. Clip the admissible range
 * to the image size.
 *
 * @param x Desired new horizontal coordinate in canvas units.
 * @param y Desired new vertical coordinate in canvas units.
 ********************************************************************/
public void moveKnot(
	int x,
	int y) 
	
	{
		if (0 <= closest) {
		x = (x < 0) ? (0) : (x);
		x = (imp.getWidth() <= x) ? (imp.getWidth() - 1) : (x);
		y = (y < 0) ? (0) : (y);
		y = (imp.getHeight() <= y) ? (imp.getHeight() - 1) : (y);
		
		
		if(closest>399)
		{
			listBuffer.removeElementAt(closest-400);
			final Point p = new Point(x, y);
			listBuffer.insertElementAt(p, closest-400);
		}
		else
		{
			Point test = (Point)listKnots.elementAt(closest);
			listKnots.removeElementAt(closest);
			final Point p = new Point(x, y);
			listKnots.insertElementAt(p, closest);
		}
		if(!curveClosed)
			updateInitialCurve(false);
		else
			{
				double[] xpoints = new double[listKnots.size()];
				double[] ypoints = new double[listKnots.size()];
				
				for (int k = 0; k < listKnots.size(); k++) 
					{	
						Point p1 = (Point)listKnots.elementAt(k);
						xpoints[k] = (double)p1.x;
						ypoints[k] = (double)p1.y;
					}	
				InitialClosedCurve = new Curve(listKnots.size(),preferences.Nresample, xpoints, ypoints, 3, true);
			}
		imp.draw();
	}
} /* end moveKnot */


/*********************************************************************
 * Modify the location of the current Constraint. Clip the admissible range
 * to the image size.
 *
 * @param x Desired new horizontal coordinate in canvas units.
 * @param y Desired new vertical coordinate in canvas units.
 ********************************************************************/
public void moveConstraint(
	int x,
	int y) 
	
	{
		if (0 <= closest) {
		x = (x < 0) ? (0) : (x);
		x = (imp.getWidth() <= x) ? (imp.getWidth() - 1) : (x);
		y = (y < 0) ? (0) : (y);
		y = (imp.getHeight() <= y) ? (imp.getHeight() - 1) : (y);
		
		
		if(closest>799)
		{
			listConstraints.removeElementAt(closest-800);
			final Point p = new Point(x, y);
			listConstraints.insertElementAt(p, closest-800);
		}
		imp.draw();
	}
} /* end moveConstraint */



/*********************************************************************
 * addPointSorted
 ********************************************************************/
public void addPointSorted(
	int x,
	int y) 
	
	{
		int index = (int)((double)(closest-500)/(double)preferences.Nresample)+1;
		if(index == listConstraints.size())
			{
				final Point p = new Point(x, y);
				listConstraints.addElement(p);
				closest = index;
			}
		else
			{
				final Point p = new Point(x, y);
				listConstraints.insertElementAt(p, index);
				closest = index;
			}
			

} /* end addPointSorted */


/*********************************************************************
 * Remove the current splineFlow. Make its color available again.
 ********************************************************************/
public void removePoint (
) {
	if (listKnots.size() >0 ) 
	{
		if(closest<400)
			listKnots.removeElementAt(closest);
		else
			listBuffer.removeElementAt(closest-400);
	}
	currentPoint = listKnots.size() - 1;
	if(!curveClosed)
			updateInitialCurve(false);
	else
		{
			double[] xpoints = new double[listKnots.size()];
			double[] ypoints = new double[listKnots.size()];
			
			for (int k = 0; k < listKnots.size(); k++) 
				{	
					Point p1 = (Point)listKnots.elementAt(k);
					xpoints[k] = (double)p1.x;
					ypoints[k] = (double)p1.y;
				}	
			InitialClosedCurve = new Curve(listKnots.size(),preferences.Nresample, xpoints, ypoints, 3, true);
		}
		imp.draw();
	
} /* end removePoint */

/*********************************************************************
 * Remove all points and make every color available.
 ********************************************************************/
public void removePoints (
) {
	listConstraints.removeAllElements();
	listBuffer.removeAllElements();
	listKnots.removeAllElements();
	listPixels.removeAllElements();
	currentPoint = -1;
	curveClosed = false;
	curveComputed = false;
	tempPointAdded = false;
	enterPointsOn = true;
	
	tb.setTool(pa.ADD_CROSS);
	imp.setRoi(this);
} /* end removePoints */

/*********************************************************************
 * Remove all points and make every color available.
 ********************************************************************/

/*********************************************************************
 * Restore the listeners
 ********************************************************************/
public void cleanUp (
) {
	listConstraints.removeAllElements();
	listBuffer.removeAllElements();
	listKnots.removeAllElements();
	listPixels.removeAllElements();
	
	final ImageCanvas ic = imp.getWindow().getCanvas();
	ic.removeKeyListener(pa);
	ic.removeMouseListener(pa);
	ic.removeMouseMotionListener(pa);
	ic.addMouseMotionListener(ic);
	ic.addMouseListener(ic);
	ic.addKeyListener(IJ.getInstance());
} /* end cleanUp */

/*********************************************************************
 * Does the drawing on the current ImagePlus object
 ********************************************************************/
public void draw (
	final Graphics g
) {
	if (started) {
		
			
		final float mag = (float)ic.getMagnification();
		final int dx = (int)(mag / 2.0);
		final int dy = (int)(mag / 2.0);
		
		showDisplayCurve(g);
		showMousePath(g);
		showDisplayKnots(g);
		showConstraints(g);
			
		// Drawing the Initial Closed curve
		//---------------------------------
		if (curveClosed )
				{
					g.setColor(Color.blue);
					boolean displayActiveKnots = pa.pointActive &&(tb.currentTool == pa.ADD_CROSS |tb.currentTool == pa.REMOVE_CROSS );
					showClosedCurve(g,InitialClosedCurve,preferences.showKnots|displayActiveKnots);
					
					if(closest>-1 && closest <800 && displayActiveKnots && closest<listKnots.size())
							{	
								Point p2 = (Point)listKnots.elementAt(closest);
								g.setColor(Color.magenta);
								g.fillOval(ic.screenX(p2.x)-3,ic.screenY(p2.y)-3,6,6);
							}
				}
				
				
		// Drawing the Final Closed curve
		//---------------------------------		
		if (curveComputed)
				{
					g.setColor(Color.red);
					showClosedCurve(g,FinalClosedCurve,preferences.showKnots);
				}
		
		if(listKnots.size()>0&&curveClosed) {
			g.setColor(Color.blue);		
			g.drawString("CA Left = " +IJ.d2s(InitialClosedCurve.CA[0],3) +" Right = " +IJ.d2s(InitialClosedCurve.CA[1],3) ,10,20);	
			
				
			if (curveComputed) { 
				g.setColor(Color.red);
				g.drawString("CA Left = " +IJ.d2s(FinalClosedCurve.CA[0],3) +" Right = " +IJ.d2s(FinalClosedCurve.CA[1],3) ,10,40);	
			}
	
		}
			
		if (updateFullWindow) {
			updateFullWindow = false;
			imp.draw();
		}
		

	}
} /* end draw */




//----------------------------------------------------------------------------
// Display the knotPoints if showKnots is set or cursor near one of the knots
//----------------------------------------------------------------------------

private void showDisplayKnots(Graphics g)
{
		
								// subpoints
		if((preferences.showKnots |pa.pointActive)  && listKnots.size()>0 && !curveClosed)
			{	
				g.setColor(Color.blue);
				for (int k = 1; (k < listKnots.size()); k++) {
					g.fillOval(ic.screenX((int)Initialcurve.CurveX[k*preferences.Nresample])-3,ic.screenY((int)Initialcurve.CurveY[k*preferences.Nresample])-3,6,6);
					//g.fillOval(ic.screenX((int)Initialcurve.CoeffX[k]),ic.screenY((int)Initialcurve.CoeffY[k]),4,4);
				}
			}
				
								// buffer
		if((preferences.showKnots |pa.pointActive) && (listBuffer.size()>0) && !curveClosed)
			{	
				g.setColor(Color.blue);
				for (int k = listKnots.size(); (k < listKnots.size()+listBuffer.size()-1); k++) 
					g.fillOval(ic.screenX((int)Initialcurve.CurveX[k*preferences.Nresample])-3,ic.screenY((int)Initialcurve.CurveY[k*preferences.Nresample])-3,6,6);
			}
								// lastPoint in magenta
		if(listBuffer.size()>0 && !curveClosed)
			{
				g.setColor(Color.magenta);
				Point p2 = (Point)listBuffer.elementAt(listBuffer.size()-1);
				g.fillOval(ic.screenX(p2.x)-3,ic.screenY(p2.y)-3,6,6);
			}	
			
								// first point in red
		if(listKnots.size() >0 & !curveClosed)
		{
			g.setColor(Color.red);
			final Point p = (Point)listKnots.elementAt(0);
			g.fillOval(ic.screenX(p.x)-3,ic.screenY(p.y)-3,6,6);
		}
				
}


//----------------------------------------------------------------------------
// Display the mousepath if showPath is set.
//----------------------------------------------------------------------------

private void showMousePath(Graphics g)
{
			
		if(preferences.showPath && listPixels.size()>2)
			{
				g.setColor(Color.magenta);
				for (int k = 0; (k < listPixels.size()-2); k++) 
					{
						Point p1 = (Point)listPixels.elementAt(k);
						Point p2 = (Point)listPixels.elementAt(k+1);
						g.drawLine(ic.screenX(p1.x) ,ic.screenY(p1.y),ic.screenX(p2.x),ic.screenY(p2.y));
					}
			}
}
	

//----------------------------------------------------------------------------
// show the display curve 
//----------------------------------------------------------------------------
private void showDisplayCurve(Graphics g)
{
		if (listKnots.size()+listBuffer.size()>2 && !curveClosed)
			{
				g.setColor(Color.blue);
				drawDispplayCurve(Initialcurve,g);
			}
		
		// Connects the end of the display curve to current cursor position with a straight line
		//---------------------------------------------------------------------------------------
		/*
		if(tempPointAdded && listBuffer.size()>0 && !curveClosed)
			{
				g.setColor(Color.blue);
				Point p1 = (Point)listBuffer.elementAt(listBuffer.size()-1);
				Point p2;
				if(listBuffer.size()>1)
					p2 = (Point)listBuffer.elementAt(listBuffer.size()-2);
				else
					p2 = (Point)listKnots.elementAt(listKnots.size()-1);
					
				g.drawLine(ic.screenX(p1.x) ,ic.screenY(p1.y),ic.screenX(p2.x),ic.screenY(p2.y));
			}
		*/	
}
	

//--------------------------------------
// Display the constraints if they exist
//--------------------------------------
private void showConstraints(Graphics g)
{				
		if(listConstraints.size() >0 && curveClosed)
		{
			g.setColor(Color.cyan);
			for (int k = 0; (k < listConstraints.size()); k++) 
				{
					Point p1 = (Point)listConstraints.elementAt(k);
					g.fillOval(ic.screenX(p1.x)-3,ic.screenY(p1.y)-3,6,6);
					int parameter = InitialClosedCurve.getClosestPointOnCurve((double)p1.x, (double)p1.y);
					g.drawLine(ic.screenX(p1.x) ,ic.screenY(p1.y),ic.screenX((int)(InitialClosedCurve.CurveX[parameter]+0.5)),ic.screenX((int)(InitialClosedCurve.CurveY[parameter]+0.5)));
				}
		}
}		


//-------------------------------------------------------------------
// Evolve the Display curve if evolvecurve is set and display it
//-------------------------------------------------------------------
		
public void updateInitialCurve(boolean evolveCurve) 
{
		
		// Creating the curve
		//--------------------			

		double xpoints[] = new double[listKnots.size()+listBuffer.size()]; 
		double ypoints[] = new double[listKnots.size()+listBuffer.size()]; 
		Point p = new Point();

		int n = 0;
		for (int k = 0; k < listKnots.size(); k++) 
			{	
				p = (Point)listKnots.elementAt(k);
				xpoints[n] = (double)p.x;
				ypoints[n] = (double)p.y;
				n++;
			}	
		
		if(listBuffer.size()>0)
		for (int k = 0; k < listBuffer.size(); k++) 
			{	
				p = (Point)listBuffer.elementAt(k);
				xpoints[n] = (double)p.x;
				ypoints[n] = (double)p.y;	
				n++;
			}		
	
		Initialcurve=new DisplayCurve(preferences.Nresample, xpoints, ypoints, 3);
		
		
		// Evolve the curve
		//--------------------			
	
		if(listKnots.size() + listBuffer.size()>1 && started && evolveCurve)
			{
				evolveCurveDisplay ec = new evolveCurveDisplay();
				ec.evolveCurve(Initialcurve,imageD,preferences.GUISigma, preferences.GUIMaxiter,listKnots.size(), 0);
			} 
		
		
		// If copyEntireBuffer is set copy buffer to listKnots
		//---------------------------------------------------------
		
		int temp = listBuffer.size();
		if(copyEntireBuffer && (temp > 0))
			{
				for(int i=0; i< temp; i++)
					{
						int x = (int)(Initialcurve.CurveX[(listKnots.size())*preferences.Nresample]+0.5);
						int y = (int)(Initialcurve.CurveY[(listKnots.size())*preferences.Nresample]+0.5);
						final Point p1 = new Point(x,y);
						listKnots.addElement(p1);
					}
				
				for(int i=0; i< temp; i++)
					listBuffer.removeElementAt(0);
				
				copyEntireBuffer = false;
			}

		// Else copy the early points while keeping SizeBuffer points in buffer
		//---------------------------------------------------------------------
		
		temp = listBuffer.size();
		if(temp > preferences.SizeBuffer);
				{
					for(int i=0; i< temp - preferences.SizeBuffer; i++)
						{
							int x = (int)(Initialcurve.CurveX[(listKnots.size())*preferences.Nresample]+0.5);
							int y = (int)(Initialcurve.CurveY[(listKnots.size())*preferences.Nresample]+0.5);
							final Point p1 = new Point(x,y);
							listKnots.addElement(p1);
						}
					
					for(int i=0; i< temp - preferences.SizeBuffer; i++)
						listBuffer.removeElementAt(0);
						
				}
				
} 

//--------------------------
// Show the closed curve 
//-----------------------

public void showClosedCurve(Graphics g, Curve Curvin,boolean showKnots) 
{
		drawClosedCurve(Curvin,g);
		if(showKnots)
		{
			float mag = (float)ic.getMagnification();
			Point p = (Point)listKnots.elementAt(0);
			int deltax = ic.screenX(p.x) - (int)(mag*p.x);
			int deltay = ic.screenY(p.y) - (int)(mag*p.y);
	
			for (int k = 0; (k <= Curvin.ncurvepts); k+= Curvin.Nsamples) 
				g.fillOval((int)(mag*Curvin.CurveX[k]+0.5)+deltax-3,(int)(mag*Curvin.CurveY[k]+0.5)+deltay-3,6,6);
			
			for (int k = 1; (k < Curvin.npoints-1); k++) 
				g.fillOval((int)(mag*Curvin.CoeffX[k]+0.5)+deltax-1,(int)(mag*Curvin.CoeffY[k]+0.5)+deltay-1,2,2);
			
			if(preferences.reflectedProfile){
				for (int k =Curvin.ncurvepts ; (k < Curvin.NCurvepts); k+= Curvin.Nsamples) 
					g.fillOval((int)(mag*Curvin.CurveX[k]+0.5)+deltax-3,(int)(mag*Curvin.CurveY[k]+0.5)+deltay-3,6,6);
				
				for (int k = 1; (k < Curvin.npoints-1); k++) 
					g.fillOval((int)(mag*Curvin.CoeffXsym[k]+0.5)+deltax-1,(int)(mag*Curvin.CoeffYsym[k]+0.5)+deltay-1,2,2);
			}
				
		}
		
} 

public void drawClosedCurve (Curve curve, Graphics g) 
{
	float mag = (float)ic.getMagnification();
	Point p = (Point)listKnots.elementAt(0);
	int deltax = ic.screenX(p.x) - (int)(mag*p.x);
	int deltay = ic.screenY(p.y) - (int)(mag*p.y);
	
	for (int k = 1; k<= curve.ncurvepts; k++) {	
		g.drawLine((int)(mag*curve.CurveX[k-1]+0.5)+deltax, (int)(mag*curve.CurveY[k-1]+0.5)+deltay,
			(int)(mag*curve.CurveX[k]+0.5)+deltax, (int)(mag*curve.CurveY[k]+0.5)+deltay);
	}
	
	if(preferences.reflectedProfile){
		for (int k = curve.ncurvepts; k< curve.NCurvepts; k++) {	
			g.drawLine((int)(mag*curve.CurveX[k-1]+0.5)+deltax, (int)(mag*curve.CurveY[k-1]+0.5)+deltay,
				(int)(mag*curve.CurveX[k]+0.5)+deltax, (int)(mag*curve.CurveY[k]+0.5)+deltay);
		}
		g.drawLine((int)(mag*curve.CurveX[0]+0.5)+deltax, (int)(mag*curve.CurveY[0]+0.5)+deltay,
				(int)(mag*curve.CurveX[curve.NCurvepts-1]+0.5)+deltax, (int)(mag*curve.CurveY[curve.NCurvepts-1]+0.5)+deltay);
				
	}
//	g.drawLine((int)(mag*curve.CurveX[0]+0.5)+deltax, (int)(mag*curve.CurveY[0]+0.5)+deltay,
//			(int)(mag*curve.CurveX[2]+0.5)+deltax, (int)(mag*curve.CurveY[2]+0.5)+deltay);
	
	/*
	g.drawLine((int)(mag*curve.xa+0.5)+deltax, (int)(mag*curve.ya+0.5)+deltay,
			(int)(mag*curve.xb+0.5)+deltax, (int)(mag*curve.yb+0.5)+deltay);
	*/			
	
	double xl,yl,xr,yr;
	xl=(curve.CurveX[0]);
	yl=(curve.CurveY[0]);
	xr=(curve.CurveX[curve.ncurvepts]);
	yr=(curve.CurveY[curve.ncurvepts]);
	
	g.drawLine((int)(mag*xl+0.5)+deltax,(int)(mag*yl+0.5)+deltay,
				(int)(mag*(xl+CALENGTH*Math.cos(curve.CArad[0]))+0.5)+deltax,(int)(mag*(yl-CALENGTH*Math.sin(curve.CArad[0]))+0.5)+deltay);
	g.drawLine((int)(mag*xr+0.5)+deltax,(int)(mag*yr+0.5)+deltay,
				(int)(mag*(xr-CALENGTH*Math.cos(curve.CArad[1]))+0.5)+deltax,(int)(mag*(yr-CALENGTH*Math.sin(curve.CArad[1]))+0.5)+deltay);
	
	
			
} 


//-----------------------------
// Show the open display curve 
//-----------------------------

public void drawDispplayCurve (DisplayCurve curve, Graphics g) 
{
	float mag = (float)ic.getMagnification();
	Point p = (Point)listKnots.elementAt(0);
	int deltax = ic.screenX(p.x)-(int)(mag*p.x);
	int deltay = ic.screenY(p.y)-(int)(mag*p.y);
	
	for (int k = 2; k< curve.NCurvepts; k+=2) 
		{	
			g.drawLine((int)(mag*curve.CurveX[k-2]+0.5)+deltax, (int)(mag*curve.CurveY[k-2]+0.5)+deltay,
			(int)(mag*curve.CurveX[k]+0.5)+deltax, (int)(mag*curve.CurveY[k]+0.5)+deltay);
		}
		
} 



/*********************************************************************
 * Create the initial closed curve
 ********************************************************************/


public void InitializeClosedCurve ()
{
	
	/*if( listKnots.size()< 4) {
		IJ.error("Please place at least 4 knots before closing the curve");
		return;
	}
	*/
	double[] xpoints;
	double[] ypoints;
	
	if(listBuffer.size()>0)
		{
			xpoints = new double[listKnots.size()+listBuffer.size()-1];
			ypoints = new double[listKnots.size()+listBuffer.size()-1];
		}
	else
		{
			xpoints = new double[listKnots.size()];
			ypoints = new double[listKnots.size()];
		}
	
	for (int k = 0; k < listKnots.size(); k++) 
		{	
			Point p = (Point)listKnots.elementAt(k);
			xpoints[k] = (double)p.x;
			ypoints[k] = (double)p.y;
		}	
	if(listBuffer.size()>0)
		{
			
		for (int k = 0; k < listBuffer.size()-1; k++) 
			{	
				Point p = (Point)listBuffer.elementAt(k);
				xpoints[k+listKnots.size()] = (double)p.x;
				ypoints[k+listKnots.size()] = (double)p.y;
			}
		}
	InitialClosedCurve = new Curve(xpoints.length,preferences.Nresample, xpoints, ypoints, 3,true);
	curveClosed = true;
//	evolveCurveDisplay ec = new evolveCurveDisplay();
//	ec.evolveCurveClosed(imp, InitialClosedCurve,imageD,preferences.GUIGamma, preferences.GUIMaxiter,false);
	listKnots.removeAllElements();

/*	for (int k = 1; k < InitialClosedCurve.npoints-1; k++)		//only Npoints, because only points from the top part can be moved 
		{	
			int x = (int)(InitialClosedCurve.CoeffX[k]+0.5);
			int y = (int)(InitialClosedCurve.CoeffY[k]+0.5);
			final Point p = new Point(x, y);
			listKnots.addElement(p);
		}			
*/
	for (int k = 0; k <= InitialClosedCurve.ncurvepts; k += InitialClosedCurve.Nsamples)		//only Npoints, because only points from the top part can be moved 
		{	
			int x = (int)(InitialClosedCurve.CurveX[k]+0.5);
			int y = (int)(InitialClosedCurve.CurveY[k]+0.5);
			final Point p = new Point(x, y);
			listKnots.addElement(p);
		}			

	listBuffer.removeAllElements();
	
	if(!preferences.mousePress)
		listConstraints.removeAllElements();
			
}


/*********************************************************************
 * Let the point that is closest to the given coordinates become the
 * current landmark.
 ********************************************************************/
public void findClosest (int x,int y) 

	{
		double proximitylimit = 16.0;
		
		if (listKnots.size() == 0) 
			{
				closest = -1;
				return;
			}
			
			
		double distance = 100.0; 
		Point p;
		double candidate;
		closest = -1;
		for (int k = 0; (k < listKnots.size()); k++) 
			{
				p = (Point)listKnots.elementAt(k);
				candidate = Math.abs(x - p.x) +  Math.abs(y - p.y); 

				if (candidate < distance) 
					{
						distance = candidate;
						if(distance < proximitylimit)
							{
								closest = k;
							}
					}
				
				}
				
		if(listBuffer.size() > 0 && !curveClosed && !enterPointsOn)
			{
				for (int k = 0; (k < listBuffer.size()); k++) 
				{
					p = (Point)listBuffer.elementAt(k);
					candidate = Math.abs(x - p.x) +  Math.abs(y - p.y); 

					if (candidate < distance) 
						{
							distance = candidate;
							if(distance < proximitylimit)
								{
									closest = 400+k;
								}
						}
					
					}
			}
		
		/*if((tb.currentTool == pa.ADD_CONSTRAINT) | (tb.currentTool == pa.REMOVE_CONSTRAINT))
			distance = 100.0; */
			
		for (int k = 0; (k < listConstraints.size()); k++) 
			{
				p = (Point)listConstraints.elementAt(k);
				candidate = Math.abs(x - p.x) +  Math.abs(y - p.y); 

				if (candidate < distance) 
					{
						distance = candidate;
						if(distance < proximitylimit)
							{
								closest = 800+k;
							}
					}
				
				}
	return;
}
/* end findClosest */



/*********************************************************************
 * Return the current Point as a <code>splineFlow</code> object.
 ********************************************************************/
public Point getPoint (
) {
	return((0 <= currentPoint) ? (Point)listConstraints.elementAt(currentPoint) : (null));
} /* end getPoint */

/*********************************************************************
 * Return the list of points.
 ********************************************************************/
public Vector getPoints (
) {
	return(listKnots);
} /* end getPoints */


		
} /* end class splineSnakeHandler */



