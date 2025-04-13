/*====================================================================| DropSnake Version: March 22, 2005\===================================================================*//*====================================================================| EPFL/STI/IOA/BIG| Aur�lien Stalder| aurelien.stalder@gmail.com|\===================================================================*/package dropsnake;// Based on the plugin SplineSnake by Mathews Jacob; Version: March 1, 2003import ij.gui.GUI;import ij.gui.ImageCanvas;import ij.gui.Roi;import ij.gui.Toolbar;import ij.IJ;import ij.ImagePlus;import ij.measure.Calibration;import ij.plugin.PlugIn;import ij.WindowManager;import java.awt.Button;import java.awt.Canvas;import java.awt.CheckboxGroup;import java.awt.Color;import java.awt.Component;import java.awt.Container;import java.awt.Dialog;import java.awt.Event;import java.awt.FileDialog;import java.awt.Font;import java.awt.Frame;import java.awt.Graphics;import java.awt.GridLayout;import java.awt.Insets;import java.awt.Label;import java.awt.Point;import java.awt.event.ActionEvent;import java.awt.event.ActionListener;import java.awt.event.KeyEvent;import java.awt.event.KeyListener;import java.awt.event.MouseEvent;import java.awt.event.MouseListener;import java.awt.event.MouseMotionListener;import java.io.BufferedReader;import java.io.FileNotFoundException;import java.io.FileReader;import java.io.FileWriter;import java.io.IOException;import java.util.Vector;/*====================================================================|	splineSnakeAction\===================================================================*//********************************************************************* * This class is responsible for dealing with the mouse events relative * to the image window. ********************************************************************/public class splineSnakeAction	extends		ImageCanvas	implements		KeyListener,		MouseListener,		MouseMotionListener{ /* begin class splineSnakeAction *//*....................................................................	Public variables....................................................................*/public static final int ADD_CROSS = 0;public static final int REMOVE_CROSS = 1;public static final int REMOVE_ALL = 2;//public static final int ADD_CONSTRAINT = 3;//public static final int REMOVE_CONSTRAINT = 4;public static final int PREF = 5;public static final int SNAKE_CURVE = 6;public static final int ACCEPT = 7;//public static final int GO = 8;public static final int FILE = 9;public static final int MAGNIFIER = 12;public static final int DROP = 14;public static final int DROP2 = 15;public static final int STACK = 10;public static final int TERMINATE = 17;public static final int ABOUT = 18;/*....................................................................	Private variables....................................................................*/private ImagePlus imp;private splineSnakeHandler ph;private splineSnakeToolbar tb;private Point lastPointMovedTo;private boolean frontmost = false;public boolean pointActive = false;private long mouseDownTime = System.currentTimeMillis();/*....................................................................	Public methods....................................................................*//********************************************************************* * Return true if the window is frontmost. ********************************************************************/public boolean isFrontmost () {	return(frontmost);} /* end isFrontmost *//********************************************************************* * Listen to <code>keyPressed</code> events. * * @param e The expected key codes are as follows: * <ul><li><code>KeyEvent.VK_DELETE</code>: remove the current landmark;</li> * <li><code>KeyEvent.VK_BACK_SPACE</code>: remove the current landmark;</li> * <li><code>KeyEvent.VK_DOWN</code>: move down the current landmark;</li> * <li><code>KeyEvent.VK_LEFT</code>: move the current landmark to the left;</li> * <li><code>KeyEvent.VK_RIGHT</code>: move the current landmark to the right;</li> * <li><code>KeyEvent.VK_TAB</code>: activate the next landmark;</li> * <li><code>KeyEvent.VK_UP</code>: move up the current landmark.</li></ul> ********************************************************************/public void keyPressed (	final KeyEvent e) {	frontmost = true;					switch (e.getKeyCode()) {		case  KeyEvent.VK_ESCAPE:			ph.enterPointsOn = !ph.enterPointsOn;			if(ph.enterPointsOn)				{					ph.tb.setTool(ADD_CROSS);					int x = lastPointMovedTo.x;					int y = lastPointMovedTo.y;					double increment = Math.sqrt((x-ph.lastPointEntered.x)*(x-ph.lastPointEntered.x) + (y-ph.lastPointEntered.y)*(y-ph.lastPointEntered.y));					int test = (int)(increment/(ph.preferences.KnotInerval));										if(test >0 & ph.listKnots.size() >0 )						{							if(ph.tempPointAdded)								ph.listBuffer.removeElementAt(ph.listBuffer.size()-1);														double incx = (x - ph.lastPointEntered.x)/((double)test);							double incy = (y - ph.lastPointEntered.y)/((double)test);							double newx = ph.lastPointEntered.x;							double newy = ph.lastPointEntered.y;															for(int i=1; i<=test; i++)								{									newx = newx+incx; newy = newy+incy;																final Point p1 = new Point((int)(newx+0.5), (int)(newy+0.5));									ph.listBuffer.addElement(p1);								}							final Point p1 = new Point((int)(newx+0.5), (int)(newy+0.5));							ph.lastPointEntered = p1;							ph.tempPointAdded = false;							ph.updateInitialCurve(true);						}				}			//else			//	ph.tb.setTool(GO);		break;			}	imp.setRoi(ph);} /* end keyPressed *//********************************************************************* * Listen to <code>keyReleased</code> events. * * @param e Ignored. ********************************************************************/public void keyReleased (	final KeyEvent e) {	frontmost = true;	} /* end keyReleased *//********************************************************************* * Listen to <code>keyTyped</code> events. * * @param e Ignored. ********************************************************************/public void keyTyped (	final KeyEvent e) {	frontmost = true;} /* end keyTyped *//********************************************************************* * Listen to <code>mouseClicked</code> events. * * @param e Ignored. ********************************************************************/public void mouseClicked (	final MouseEvent e) {	frontmost = true;} /* end mouseClicked *//********************************************************************* * Listen to <code>mouseDragged</code> events. Move the current splineFlow * and refresh the image window. * * @param e Event. ********************************************************************/public void mouseDragged (	final MouseEvent e) {	frontmost = true;	final int x = imp.getWindow().getCanvas().offScreenX(e.getX());	final int y = imp.getWindow().getCanvas().offScreenY(e.getY());		if(ph.closest >=0 && ph.closest < 400 && ph.tb.currentTool == ADD_CROSS)		{			ph.moveKnot(x,y);		}	/*if(ph.closest >=800 && ph.tb.currentTool == ADD_CONSTRAINT)		{			ph.moveConstraint(x,y);		}*/	mouseMoved(e);} /* end mouseDragged *//********************************************************************* * Listen to <code>mouseEntered</code> events. * * @param e Ignored. ********************************************************************/public void mouseEntered (	final MouseEvent e) {	frontmost = true;	WindowManager.setCurrentWindow(imp.getWindow());	imp.getWindow().toFront();	imp.setRoi(ph);		} /* end mouseEntered *//********************************************************************* * Listen to <code>mouseExited</code> events. Clear the ImageJ status * bar. * * @param e Event. ********************************************************************/public void mouseExited (	final MouseEvent e) {	frontmost = false;	//imp.getWindow().toBack();	//IJ.getInstance().toFront();	pointActive = false;	imp.draw();	imp.setRoi(ph);	IJ.showStatus("");} /* end mouseExited *//********************************************************************* * Listen to <code>mouseMoved</code> events. Update the ImageJ status * bar. * * @param e Event. ********************************************************************/public void mouseMoved (	final MouseEvent e) {	frontmost = true;	setControl();	final int x = imp.getWindow().getCanvas().offScreenX(e.getX());	final int y = imp.getWindow().getCanvas().offScreenY(e.getY());	IJ.showStatus(imp.getLocationAsString(x, y) + getValueAsString(x, y));					if(ph.listConstraints.size() >0 && !ph.curveClosed && (ph.tb.getCurrentTool() == ADD_CROSS) )		{			ph.addPointsInBuffer(x,y,false);			ph.addPixel(x,y);			ph.imp.draw();		}			/*if(ph.tb.getCurrentTool() == GO)		{			final int x1 = imp.getWindow().getCanvas().offScreenX(e.getX());			final int y1 = imp.getWindow().getCanvas().offScreenY(e.getY());			lastPointMovedTo = new Point(x1,y1);		}*/	ph.findClosest(x, y);	if(ph.closest>-1 && (!ph.enterPointsOn | ph.curveClosed) )		{			pointActive = true;			ph.imp.draw();		}	else if(pointActive)		{			pointActive = false;			ph.imp.draw();		}						} /* end mouseMoved *//********************************************************************* * Listen to <code>mousePressed</code> events. Perform the relevant * action. * * @param e Event. ********************************************************************/public void mousePressed (	final MouseEvent e) {	frontmost = true;	final int x = imp.getWindow().getCanvas().offScreenX(e.getX());	final int y = imp.getWindow().getCanvas().offScreenY(e.getY());	int currentPoint;		if(!ph.started);		//IJ.error("          Please wait\n Initialization underway");	else	{		final boolean doubleClick = ((System.currentTimeMillis() - mouseDownTime) <= 250L);	mouseDownTime = System.currentTimeMillis();			switch (tb.getCurrentTool()) {		case ADD_CROSS:			if(doubleClick)				{					ph.listKnots.removeElementAt(ph.listKnots.size()-1);					Point p = (Point)ph.listKnots.elementAt(0);				 	ph.addPointsInBuffer(p.x,p.y,true);				 	ph.InitializeClosedCurve();				}			else				{					ph.findClosest(x, y);					if(ph.closest == 0 && !ph.curveClosed)						ph.InitializeClosedCurve();					else if(!ph.curveClosed)						{							ph.addPoint(x,y);							ph.lastPointEntered = new Point(x,y);							if(ph.listKnots.size()>0)									ph.updateInitialCurve(true);						}					else if(ph.closest<400)						ph.moveKnot(x,y);					else 						if(ph.closest<800)						ph.addPointSorted(x,y);				}			ph.imp.draw();					break;				/*case ADD_CONSTRAINT:			if(!ph.curveClosed)				IJ.error("Sorry, can add the constraints only on a closed curve\n Please close the curve");			else				{					ph.findClosest(x, y);					if(ph.closest <800)						ph.addConstraint(x,y);					ph.imp.draw();					}		break;				case REMOVE_CONSTRAINT:			if(!ph.started)				IJ.error("          Please wait\n Initialization underway");			else				{					ph.findClosest(x, y);					if(ph.closest >=800)						ph.listConstraints.removeElementAt(ph.closest-800);					ph.imp.draw();					}		break;*/				case REMOVE_CROSS:			ph.findClosest(x, y);			if(ph.closest >0  && ph.closest<400)				ph.removePoint();			break;										case MAGNIFIER:			final int flags = e.getModifiers();			if ((flags & (Event.ALT_MASK | Event.META_MASK | Event.CTRL_MASK)) != 0) {				imp.getWindow().getCanvas().zoomOut(e.getX(), e.getY());//				imp.getWindow().getCanvas().zoomOut(x, y);			}			else {				imp.getWindow().getCanvas().zoomIn(e.getX(), e.getY());//				imp.getWindow().getCanvas().zoomIn(x, y);			}			//ph.tb.setTool(ADD_CROSS);			break;		}	}	imp.setRoi(ph);} /* end mousePressed *//********************************************************************* * Listen to <code>mouseReleased</code> events. * * @param e Ignored. ********************************************************************/public void mouseReleased (	final MouseEvent e) {	frontmost = true;	ph.closest = -1;} /* end mouseReleased *//********************************************************************* * This constructor stores a local copy of its parameters and initializes * the current control. * * @param imp <code>ImagePlus<code> object where points are being picked. * @param splineSnakeHandler1 <code>splineSnakeHandler<code> object that handles operations. * @param tb <code>splineSnakeToolbar<code> object that handles the toolbar. ********************************************************************/public splineSnakeAction (	 ImagePlus imp,	 splineSnakeHandler splineSnakeHandler1,	 splineSnakeToolbar tb) {	super(imp);	this.imp = imp;	this.ph = splineSnakeHandler1;	this.tb = tb;} /* end splineSnakeAction *//*....................................................................	Private methods....................................................................*//*------------------------------------------------------------------*/private String getValueAsString (	final int x,	final int y) {	final Calibration cal = imp.getCalibration();	final int[] v = imp.getPixel(x, y);	switch (imp.getType()) {		case ImagePlus.GRAY8:		case ImagePlus.GRAY16:			final double cValue = cal.getCValue(v[0]);			if (cValue==v[0]) {				return(", value=" + v[0]);			}			else {				return(", value=" + IJ.d2s(cValue) + " (" + v[0] + ")");			}		case ImagePlus.GRAY32:			return(", value=" + Float.intBitsToFloat(v[0]));		case ImagePlus.COLOR_256:			return(", index=" + v[3] + ", value=" + v[0] + "," + v[1] + "," + v[2]);		case ImagePlus.COLOR_RGB:			return(", value=" + v[0] + "," + v[1] + "," + v[2]);		default:			return("");	}} /* end getValueAsString *//*------------------------------------------------------------------*/private void setControl () {	switch (tb.getCurrentTool()) {		case ADD_CROSS:			imp.getWindow().getCanvas().setCursor(crosshairCursor);			break;		case FILE:		case MAGNIFIER:		case REMOVE_CROSS:			imp.getWindow().getCanvas().setCursor(defaultCursor);			break;	}} /* end setControl */private void mouseEnterPoints(int x, int y)	{				}} /* end class splineSnakeAction */