/*====================================================================
| DropSnake Version: March 22, 2005
\===================================================================*/

/*====================================================================
| EPFL/STI/IOA/BIG
| Aurélien Stalder
| aurelien.stalder@gmail.com
|
\===================================================================*/

// Based on the plugin SplineSnake by Mathews Jacob; Version: March 1, 2003


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

import dropsnake.*;

/*====================================================================
|	splineSnake_
|   This class is the only one that is accessed directly by imageJ;
|   it attaches listeners and dies. Note that it implements
|  <code>PlugIn</code> rather than <code>PlugInFilter</code>.
\===================================================================*/

public class DropSnake_
	implements
		PlugIn
{ 
public static boolean running = false;


/**
 * This main function serves for development purposes.
 * It allows you to run the plugin immediately out of
 * your integrated development environment (IDE).
 *
 * @param args whatever, it's ignored
 * @throws Exception
 */
public static void main(final String... args) throws Exception {
	ImageJ ij = new ImageJ();
	ImagePlus imp = IJ.createImage("test", 500, 500, 1, 8);
	imp = IJ.openImage("/Users/dsage/Desktop/test.tif");
	imp.show();
	new DropSnake_().run("");
}


/*..................................................................*/
/* Public methods													*/
/*..................................................................*/

public void run (
	final String arg
) {
	if(running)
		return;
	
	running = true;
	final ImagePlus imp = WindowManager.getCurrentImage();
	if (imp == null) {
		IJ.noImage();
		return;
	}
	ImageCanvas ic = imp.getWindow().getCanvas();
	splineSnakeToolbar tb = new splineSnakeToolbar(Toolbar.getInstance());
	splineSnakeHandler ph = new splineSnakeHandler(imp, tb);
	tb.setWindow(ph, imp);
	tb.toolBaractive = true;
} /* end run */

} /* end class splineSnake_ */



