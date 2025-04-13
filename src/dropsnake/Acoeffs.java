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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.lang.*;
import ij.*;


public class Acoeffs extends Object {
	
	private double[][] buffer;
	private Curve curve;
	
//----------------------------
// Acoeffs -> Constructor
//----------------------------
	public Acoeffs(Curve curve) {
		this.curve = curve;
		computeBuffer();
	}	
	
//----------------------------
//    Main Routine
//----------------------------
	 public double[][] getAcoeffs(ImageAccess imgin, Curve curve) 
	 
	 	{
		    Evaluatefunction CC = new Evaluatefunction(imgin, curve);
		  double[] curvevalues = CC.computeCurvevalues();
		  double[][] acoeff = computeAcoeffArray(curve, curvevalues);
		    return acoeff;
	 	}	

	
//------------------------------------------------------------------------------------------	
// computeAcoeffArray -> Compute the Acoeff array from the 
// curve values and the buffer
//   Returns an discretized approximation of the integral
//
// 			Acoeff(k,l) = \int_0^M A(t) \beta_p(t-k)\beta'_p(t-l)dt and
// 			
//      A(t) = A(x(t),y(t)), (x(t),y(t)) being the position of the curve at t
//      A(x,y) is the image
//------------------------------------------------------------------------------------------	
	
	public double[][] computeAcoeffArray(Curve curve, double[] curvevalues) {
		
		int i,j,k,m,o;
		int Length;
		int x;
		
		Length = curve.Degree*curve.Nsamples+1;
		x = curve.Npoints*curve.Nsamples;
		
		double Acoeff[][] = new double[curve.Npoints][curve.Npoints];
		
					
		for(i=0; i<curve.Npoints; i++)
		{
			for(j=0; j<curve.Npoints; j++)		
				{
					Acoeff[i][j] = 0.0;
					k = i-j+curve.Degree+1; k += (k<0)? curve.Npoints:(k<2*(curve.Degree+1))? 0: -curve.Npoints; /* Mod operation to restrict in range*/
					if( (k>0) && (k<2*(curve.Degree+1) ))     							/* Still out of range after the mod. Ref Note book.*/
						{  
							for(m=0;m<2*Length-1;m++)
								{
									o = i*curve.Nsamples+m-Length+1;
									o= (o<0) ? o += x: (o>=x)? o -= x:o;
									Acoeff[i][j] += buffer[k][m] * curvevalues[o];
								}
				  		}
				}
			}
		return(Acoeff);
	}
		

//------------------------------------------
// ComputeBuffer -> Initializes the buffer
//------------------------------------------

	public void computeBuffer() { 
		
		double tempSplinevalues;
		double index;
		buffer = new double[2*(curve.Degree+1)+1][2*curve.Degree*curve.Nsamples+1];
		int i,k,m,n=0,o=0;
		
		
		for (i=-(curve.Degree+1);i<=(curve.Degree+1);i++)	
			  {
				 for (k=-curve.Degree*curve.Nsamples;k<=curve.Degree*curve.Nsamples;k++)
					 {
					 	index = (double)k/(double)curve.Nsamples;
					  	tempSplinevalues = 0.0;
					  	for (m=-(curve.Npoints)*(int)(2*(curve.Degree+1)/(curve.Npoints));m<=(curve.Npoints)*(int)(2*(curve.Degree+1)/(curve.Npoints));m+=(curve.Npoints))
					  		{
					  			tempSplinevalues += (curve.BSpline(index+0.5+i+m,curve.Degree-1) 
					  								- curve.BSpline(index-0.5+i+m,curve.Degree-1));
					  		}
					  	buffer[n][o] = curve.BSpline(index,curve.Degree)*tempSplinevalues;
					  	o++;
					  }
				  n++;
				  o=0;	  
				}
	}
}