/*====================================================================
| Version: March 1, 2003
\===================================================================*/

/*====================================================================
| EPFL/STI/IOA/BIG
| Mathews Jacob
| Bldg. BM-Ecublens 4.141
| CH-1015 Lausanne VD
|
| phone (CET): +41(21)693.51.43
| fax: +41(21)693.37.01
| RFC-822: Mathews.Jacob@epfl.ch
|| URL: http://bigwww.epfl.ch/jacob
\===================================================================*/

/*====================================================================
| Additional help available at http://bigwww.epfl.ch/jacob/splineSnake/
\===================================================================*/
package dropsnake;

import ij.plugin.PlugIn;
import ij.process.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.lang.*;
import ij.*;



public class Bcoeffs extends Object {
	
	public double[] buffer;
	

//----------------------------
// Bcoeffs -> Constructor
//----------------------------
	
	public Bcoeffs(Curve curve) {
		buffer = computeBuffer(curve);
	}	

//----------------------------
//    Main Routine
//----------------------------

	public double[] getBcoeffs(ImageAccess imgin, Curve curve) 
	
		{
			Evaluatefunction CC = new Evaluatefunction(imgin, curve);
		  	double[] curvevalues = CC.computeCurvevalues();
			double[] bcoeff = computeBcoeffArray(curve, curvevalues);
			return bcoeff;
		}	

	
//------------------------------------------------------------
//	 computeBcoeffArray -> Compute the Bcoeff array from the 
//	 curve values and the buffer
//------------------------------------------------------------
	
	public double[] computeBcoeffArray(Curve curve, double[] curvevalues) {
		int length = 2*curve.Nsamples+1;
		
		int k, x;
		x = curve.NCurvepts;
		double Bcoeff[] = new double[curve.Npoints];
					
		for(int i=0; i<curve.Npoints; i++) 
		{
			Bcoeff[i] = 0.0;
			for(int j=0;j<2*length-1;j++)
				{
					k = (i*curve.Nsamples+j-length); k = (k<0)? k+=x:(k>=x)? k-=x:k;
					Bcoeff[i] += curvevalues[k]*buffer[j];
				}
		}	
		return Bcoeff;
		
	}

//------------------------------------------
//	 ComputeBuffer -> Initializes the buffer
//------------------------------------------


	public double[] computeBuffer(Curve curve) { 
		
		double[] buffer = new double[4*curve.Nsamples+1];
		int j=0;
		for(int i=-2*curve.Nsamples; i<=2*curve.Nsamples; i++) {
			buffer[j++]=curve.BSpline((double)i/(double)curve.Nsamples+0.5, curve.Degree-1)-curve.BSpline((double)i/(double)curve.Nsamples-0.5, curve.Degree-1);
		}
		return buffer;
	}
}