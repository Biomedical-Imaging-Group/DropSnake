/*====================================================================
| DropSnake Version: March 22, 2005
\===================================================================*/

/*====================================================================
| EPFL/STI/IOA/BIG
| Aurélien Stalder
| aurelien.stalder@gmail.com
|
\===================================================================*/
package dropsnake;

// Based on the plugin SplineSnake by Mathews Jacob; Version: March 1, 2003

// This class is responsible for the derivative calculation of the snake
// It calculates both image energy and internal energy 

import ij.plugin.PlugIn;
import ij.process.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.lang.*;

import ij.*;



public class DirDerrivative extends Object
{
	static private double ST_LG=1.0;	
	static private double ST_LS=1000.0;	
	
	public Image Imagepreprocessed;
	public double[] DC_final;
	public double[] DD_final;
	public Curve curve;
	
	public Acoeffs Acoeff_inst;
	public Bcoeffs Bcoeff_inst;
	
	private double  DC[];
	private double  DD[];
	private double[] DX;
	private double[] DY;
	private double[] tempX;
	private double[] tempY;
	private double Threshold;
	
	
	int Origin = 3;
	
	
	private double[] kernel1 = {1.0/120.0, 13.0/60.0, 11.0/20.0, 13.0/60.0,1.0/120.0 };
	private double weight;
	private int nConstraints;
	private double[] Xconstraints;
	private double[] Yconstraints;
	private int[] ConstraintParameter;
	
	private double yh,xh,Dalpha,Dh;
	
	double weight_int;
	
	double[] curvevalues;
	
	private SplineSnakePreferences param;
//------------	
// Constructor
//------------
	public DirDerrivative(ImagePlus imp, Curve curve, Vector listConstraints, SplineSnakePreferences param, double weight)
	
		 {	
			Imagepreprocessed = new Image(imp, curve, param);
			Acoeff_inst = new Acoeffs(curve);
			Bcoeff_inst = new Bcoeffs(curve);
			DC = new double[curve.Npoints];
			DD = new double[curve.Npoints];
			DC_final = new double[curve.npoints];
			DD_final = new double[curve.npoints];
			DX  = new double[curve.Npoints];
		 	DY  = new double[curve.Npoints];
		 	tempX = new double[curve.Npoints];
		 	tempY = new double[curve.Npoints];
			Threshold = param.SnakeThreshold;
			this.curve = curve;
			this.weight = weight;
			
			weight_int=0.5*weight;
			
			nConstraints = listConstraints.size();
			Xconstraints = new double[nConstraints];
			Yconstraints = new double[nConstraints];
			for(int i=0;i<nConstraints;i++) 
				{
					Point p = (Point)listConstraints.elementAt(i);
					Xconstraints[i] = (double)p.x;
					Yconstraints[i] = (double)p.y;
				}
			ConstraintParameter = new int[nConstraints];
			estimateParameters();
			
			this.param=param;
		}

// weight2 is the normalization norm for the Image energy derivatives. if weight2==0 -> derivatives E image =0
// Eint has weight(constant) for normalization
 
public double getDD(double weight2, boolean looping) 
//weight2 is 0 if recovering from loop or 1 if snaking
	{
		
		Evaluatefunction CC;
		if(param.interpolate){
			CC = new Evaluatefunction(Imagepreprocessed.preprocessedImage,Imagepreprocessed.preprocessedCoeff, curve);
		}
		else {
			CC = new Evaluatefunction(Imagepreprocessed.preprocessedImage,curve);
		}	
		curvevalues = CC.computeCurvevalues();

		double norm,temp;
		int ir;
		CalcDD_Bayes();
		
		
		CalcDD_sym();
	
	
		Dalpha /= ((curve.xa-xh)*Math.cos(curve.alpha));
		
		DD[1] = Dh + Dalpha;
		DD[curve.npoints-2] = Dh - Dalpha;

		norm = Normalize_DD(Threshold, weight2);
	
		for(int i=0; i<curve.npoints; i++)
			{
				DC_final[i] = DC[i];
				DD_final[i] = DD[i];
			}

		if(param.EnergySurfaces)
		{
			CalcDD_Laplace_exact(looping);
			temp = Normalize_DD(Threshold, param.eRatio);
			for(int i=3; i<(curve.npoints-3); i++)		//points of the interface shouldn't be affected by curvilinear constraint
				{
					DC_final[i] += DC[i];
					DD_final[i] += DD[i];
				}
			int i;
			i=2;
			DC_final[i] += 0.5*DC[i];
			DD_final[i] += 0.5*DD[i];
			i=curve.npoints-3;
			DC_final[i] += 0.5*DC[i];
			DD_final[i] += 0.5*DD[i];

			if(param.initialization) {
				i=1;
				DC_final[i] += 0.25*DC[i];
				//DD_final[i] += 0.25*DD[i];
				i=curve.npoints-2;
				DC_final[i] += 0.25*DC[i];
				//DD_final[i] += 0.25*DD[i];
			}
		}

		if(!param.reflectedProfile) {
			//DC_final[1] = 0.0;
			DD_final[1] = 0.0;
			//DC_final[curve.npoints-2] = 0.0;
			DD_final[curve.npoints-2] = 0.0;
			
		}
			
		return(norm);
	}

//------------------------------------------------------------------------------------------		
//	CalcDD_Bayes: Computes the Directional derrivatives for the combined Baye's cost function
//
// 	Cost function involves the integral of the normalized preprocessed image in the curve.
//  Hence, the directional derrivatives are obtained as
// 			dC/dc_k	=  \sum d_l \int_0^M A(t) \beta_p(t-k)\beta'_p(t-l)dt and
// 			dC/dd_k	= -\sum c_l \int_0^M A(t) \beta_p(t-k)\beta'_p(t-l)dt respectively,
//  where
//   	c_k, d_k, k=0...M-1 are the spline coefficients of the curve
//      A(t) = A(x(t),y(t)), (x(t),y(t)) being the position of the curve at t
//      A(x,y) is the image
//------------------------------------------------------------------------------------------


	public	void CalcDD_Bayes()
		{
			
		
		
		int l,lsym;
	
		DC = new double[curve.npoints];
		DD = new double[curve.npoints];
		if(curve.npoints <=5) {IJ.write("cannot process spline with less than 4 knots"); return;}
		
		int a;
		for(a=3;a<curve.npoints-3;a++) {
			DC[a]=0.0;
			DD[a]=0.0;
			
			for(int i=1;i<4*curve.Nsamples;i++)		//at its bounds, spline is 0
				{
					l = (a-1-2)*curve.Nsamples+i;
					if(l<0) {IJ.error("l<0 " +l);}
					if(l>=curve.ncurvepts) {IJ.error("l> " +l +"ncurvepts" +curve.ncurvepts);}
					DC[a] += -curvevalues[l] * curve.CurveDiffY[l]*curve.SplineValues[i];
					DD[a] += curvevalues[l] * curve.CurveDiffX[l]*curve.SplineValues[i];
					//reflected profile:
					if(param.reflectedProfile) {
						lsym=-l+2*curve.ncurvepts;
						DC[a] += -curvevalues[lsym] * curve.CurveDiffY[lsym]*curve.SplineValues[i];
						DD[a] -= curvevalues[lsym] * curve.CurveDiffX[lsym]*curve.SplineValues[i];
					}
				}
		}
	//boundaries
	double[] SplineValues1 = new double[4*curve.Nsamples];
	for(int i=curve.Nsamples; i<2*curve.Nsamples;i++){
		SplineValues1[i]=curve.SplineValues[i]-curve.SplineValues[i+2*curve.Nsamples];
	}
	for(int i=2*curve.Nsamples; i<4*curve.Nsamples;i++){
		SplineValues1[i]=curve.SplineValues[i];
	}

	double[] SplineValues0 = new double[4*curve.Nsamples];
	for(int i=2*curve.Nsamples; i<3*curve.Nsamples;i++){
		SplineValues0[i]=curve.SplineValues[i]+2*curve.SplineValues[i+curve.Nsamples];
	}
	for(int i=3*curve.Nsamples; i<4*curve.Nsamples;i++){
		SplineValues0[i]=curve.SplineValues[i];
	}
	
		
//ok	
	a=2;
	DC[a]=0.0;
	DD[a]=0.0;
			
	for(int i=curve.Nsamples+1;i<4*curve.Nsamples;i++)		//at its bounds, spline is 0
		{
			l = (a-1-2)*curve.Nsamples+i;
			lsym=-l+2*curve.ncurvepts;
				
			if(l<0) {IJ.error("l<0 " +l);}
			if(l>=curve.ncurvepts) {IJ.error("l> " +l +"ncurvepts" +curve.ncurvepts);}
			DC[a] += -curvevalues[l] * curve.CurveDiffY[l]*SplineValues1[i];
			DD[a] += curvevalues[l] * curve.CurveDiffX[l]*SplineValues1[i];
			if(param.reflectedProfile) {
			DC[a] += -curvevalues[lsym] * curve.CurveDiffY[lsym]*SplineValues1[i];
			DD[a] -= curvevalues[lsym] * curve.CurveDiffX[lsym]*SplineValues1[i];
			}
		}
	
	a=curve.npoints-3;
	DC[a]=0.0;
	DD[a]=0.0;
			
	for(int i=1;i<3*curve.Nsamples;i++)		//at its bounds, spline is 0
		{
			l = (a-1-2)*curve.Nsamples+i;
			lsym=-l+2*curve.ncurvepts;
			
			if(l<0) {IJ.error("l<0 " +l);}
			if(l>=curve.ncurvepts) {IJ.error("l> " +l +"ncurvepts" +curve.ncurvepts);}
			DC[a] += -curvevalues[l] * curve.CurveDiffY[l]*SplineValues1[-i+4*curve.Nsamples];
			DD[a] += curvevalues[l] * curve.CurveDiffX[l]*SplineValues1[-i+4*curve.Nsamples];
			if(param.reflectedProfile) {
			DC[a] += -curvevalues[lsym] * curve.CurveDiffY[lsym]*SplineValues1[-i+4*curve.Nsamples];
			DD[a] -= curvevalues[lsym] * curve.CurveDiffX[lsym]*SplineValues1[-i+4*curve.Nsamples];
			}
		}

//ok
	a=1;
	DC[a]=0.0;
	DD[a]=0.0;
			
	for(int i=2*curve.Nsamples+1;i<4*curve.Nsamples;i++)		//at its bounds, spline is 0
		{
			l = (a-1-2)*curve.Nsamples+i;
			lsym=-l+2*curve.ncurvepts;
			
			if(l<0) {IJ.error("l<0 " +l);}
			if(l>=curve.ncurvepts) {IJ.error("l> " +l +"ncurvepts" +curve.ncurvepts);}
			DC[a] += -curvevalues[l] * curve.CurveDiffY[l]*SplineValues0[i];
			DD[a] += curvevalues[l] * curve.CurveDiffX[l]*SplineValues0[i];
			if(param.reflectedProfile) {
			DC[a] += -curvevalues[lsym] * curve.CurveDiffY[lsym]*SplineValues0[i];
			DD[a] -= curvevalues[lsym] * curve.CurveDiffX[lsym]*SplineValues0[i];
			}
		}
	a=curve.npoints-2;
	DC[a]=0.0;
	DD[a]=0.0;
	for(int i=1;i<2*curve.Nsamples;i++)		//at its bounds, spline is 0
		{
			l = (a-1-2)*curve.Nsamples+i;
			lsym=-l+2*curve.ncurvepts;
			
			if(l<0) {IJ.error("l<0 " +l);}
			if(l>=curve.ncurvepts) {IJ.error("l> " +l +"ncurvepts" +curve.ncurvepts);}
			DC[a] += -curvevalues[l] * curve.CurveDiffY[l]*SplineValues0[-i+4*curve.Nsamples];
			DD[a] += curvevalues[l] * curve.CurveDiffX[l]*SplineValues0[-i+4*curve.Nsamples];
			if(param.reflectedProfile) {
			DC[a] += -curvevalues[lsym] * curve.CurveDiffY[lsym]*SplineValues0[-i+4*curve.Nsamples];
			DD[a] -= curvevalues[lsym] * curve.CurveDiffX[lsym]*SplineValues0[-i+4*curve.Nsamples];
			}
		}

	
		
		return;
	}

	
//------------------------------------------------------------------------------------------		
//	CalcDD_Length: Computes the Directional derrivatives for the length

//------------------------------------------------------------------------------------------

		
	public	void CalcDD_Laplace_exact(boolean looping)
	{
		int l;
		double dist=0.0;
		double errordist=0.0,temp,sumerror=0.0;
		double errordistknot, errordistmax=0.0, errordistmin;
		double gamma;
		
		DC = new double[curve.npoints];
		DD = new double[curve.npoints];
		
		Bcoeff_inst= new Bcoeffs(curve);
		
	
		int ndist=1;
		if (looping) dist=0.0;
		else {
			
			dist=curve.lengthNormal*curve.Nsamples/curve.ncurvepts;
		}
		double c,dvar;
		
		
		errordistmin=dist*ndist;
		errordistmin=-dist*ndist;
		curve.imin=curve.imax=curve.npoints/2;
		
//		gamma= param.knotsDistance/dist-1.0;
		gamma= param.gamma;
		
		for(int a=1;a<=curve.npoints-2;a++) {
			
				errordistknot=0.0;
				
				for(int i=1;i<4*curve.Nsamples;i++) {
						l = (a-1-2)*curve.Nsamples+i;
						if(l<0) {;}
						else if(l>=curve.ncurvepts) {;}
						else {
							dvar=1+gamma-2*gamma*Math.abs(2.0*(double)l/(double)curve.ncurvepts-1);
							c=dist*dvar;
							temp=Math.sqrt(curve.CurveDiffX[l]*curve.CurveDiffX[l] + curve.CurveDiffY[l]*curve.CurveDiffY[l]);
							errordist=(1-c/temp);

							DC[a] += errordist * curve.CurveDiffX[l]*Bcoeff_inst.buffer[i];
							DD[a] += errordist * curve.CurveDiffY[l]*Bcoeff_inst.buffer[i];

							if(i>=2*curve.Nsamples && i<3*curve.Nsamples) errordistknot += (temp-c);
							
						}
				}
				if(a!=curve.npoints-2){
					if (errordistknot > errordistmax) {errordistmax = errordistknot; curve.imax=a;}
					if (errordistknot < errordistmin) {errordistmin = errordistknot; curve.imin=a;}
				}
		}
		return;
	
	}
	
//------------------------------------------------------------------------------------------		
//	CalcLength: Computes the length of the curve
// La somme des longueurs au carré???
//
//  
//------------------------------------------------------------------------------------------


public	void CalcLength()
	{
	
		int l,m,n;
		
		
	// Computing the backward finite differences¨
	//-------------------------------------------
		DX[0] = curve.CoeffX[0]-curve.CoeffX[curve.Npoints-1];
		DY[0] = curve.CoeffY[0]-curve.CoeffY[curve.Npoints-1];
		for(int i=1;i<curve.Npoints;i++)
			{
				DX[i] = curve.CoeffX[i]-curve.CoeffX[i-1];
				DY[i] = curve.CoeffY[i]-curve.CoeffY[i-1];
			}
	
	
	curve.Length = 0.0;
	for(int a=0;a<curve.Npoints;a++)
		{
			tempX[a] = 0;tempY[a] = 0;
			for(int i=0;i<5;i++)
				{
					l = a-i+2;
					l=(l<0)? l+=curve.Npoints: (l>=curve.Npoints)? l-=curve.Npoints:l;
					tempX[a] += DX[l]*kernel1[i];
					tempY[a] += DY[l]*kernel1[i];
				}
			curve.Length += DX[a]*tempX[a] + DY[a]*tempY[a];
		}
	
	curve.Length = curve.Length/curve.Npoints;
	
	
		
	return;
	
	
	}

// ---------------------------------------------------------
//	estimateParameters:  
// ---------------------------------------------------------

	public void estimateParameters()
		{
			
			for(int i=0;i<nConstraints;i++) 
				ConstraintParameter[i] = curve.getClosestPointOnCurve(Xconstraints[i],Yconstraints[i]);
		}
	
// ---------------------------------------------------------
//	Normalize_DD: Normalize the  directional derrivatives 
// ---------------------------------------------------------

	public double Normalize_DD(double	Threshold,	double Constant)
		{
			int i;
			double norm = 0.0;
			double factor;

			for(i=0;i<curve.npoints;i++) 
				{
					norm += DC[i]*DC[i];
					norm += DD[i]*DD[i];
				}
			if(norm > Threshold)
				{
					factor = Constant/Math.sqrt(norm);
				}
			else
				{
					factor = 0.0;
					norm = 0;
				}
	
			for(i=0;i<curve.npoints;i++) 
				{
					DC[i] = DC[i]*factor;
					DD[i] = DD[i]*factor;
				}	
			
			norm = Math.sqrt(norm)/curve.npoints;
			return(norm);
	
	}
	
	
// ---------------------------------------------------------
//	addKnot:  
// ---------------------------------------------------------

	public void addKnot()
		{
			curve.addKnot();
			
			DC = new double[curve.Npoints];
			DD = new double[curve.Npoints];
			DC_final = new double[curve.Npoints];
			DD_final = new double[curve.Npoints];
			DX  = new double[curve.Npoints];
		 	DY  = new double[curve.Npoints];
		 	tempX = new double[curve.Npoints];
		 	tempY = new double[curve.Npoints];
			
		}
		
// ---------------------------------------------------------
//	deleteKnot:  
// ---------------------------------------------------------

	public void deleteKnot()
		{
			curve.deleteKnot();
			
			DC = new double[curve.Npoints];
			DD = new double[curve.Npoints];
			DC_final = new double[curve.Npoints];
			DD_final = new double[curve.Npoints];
			DX  = new double[curve.Npoints];
		 	DY  = new double[curve.Npoints];
		 	tempX = new double[curve.Npoints];
		 	tempY = new double[curve.Npoints];
			
		}
		
	private void computeSymParam() {
	
	xh=0.5*(curve.xa+curve.xb);
	yh=0.5*(curve.ya+curve.yb);
	

	}
	
	
// ------------------------------------------------------------------------------------	
//	Function:	CalcDD_sym
//	Purpose:	Compute the vertical and angular derivative of the reflected profile
//              
// ------------------------------------------------------------------------------------	

	private void CalcDD_sym(){
	
			computeSymParam();
		
			int i,j;
			
			Dh=0.0;
			Dalpha = 0.0;
			
			double curveDx,curveDy,Dx,Dy;
			for (i=curve.ncurvepts; i< curve.NCurvepts; i++){
				curveDx= curvevalues[i]*curve.CurveDiffX[i];
				curveDy= curvevalues[i]*curve.CurveDiffY[i];
				
				Dh -= curveDx;
				Dx=curve.CurveX[i]-xh;
				Dy=curve.CurveY[i]-yh;
				Dalpha -= curveDx*Dx;
				Dalpha -= curveDy*Dy;
			}
			
			Dh *= -2*(Math.cos(curve.alpha))*(Math.cos(curve.alpha));
			Dalpha *= -2*Math.cos(2*curve.alpha);
			
			return;
	}
	
	
	
//END OF METHODS ACTUALLY USED	
	
	
//------------------------------------------------------------------------------------------		
//	NOT IMPLEMENTED IN THE FINAL VERSION
//	CalcDD_Drop: Computes the Directional derrivatives for the laplace energy
//
//------------------------------------------------------------------------------------------

		private double[][][] KernelCG={{{-1./855360, -1./362880, 1./285120, 1./2395008, 0, 0, 0}, {-5./342144, -53./997920, 5./88704, 17./1496880, 0, 0, 0}, 
  {-17./1496880, -5./88704, 53./997920, 5./342144, 0, 0, 0}, {-1./2395008, -1./285120, 1./362880, 1./855360, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0}, 
  {0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0}}, {{-5./342144, -53./997920, 5./88704, 17./1496880, 0, 0, 0}, 
  {-541./1995840, -503./187110, 79./55440, 1./660, 23./1197504, 0, 0}, {-1159./3991680, -919./142560, 0, 919./142560, 1159./3991680, 0, 0}, 
  {-23./1197504, -1./660, -79./55440, 503./187110, 541./1995840, 0, 0}, {0, -17./1496880, -5./88704, 53./997920, 5./342144, 0, 0}, 
  {0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0}}, {{-17./1496880, -5./88704, 53./997920, 5./342144, 0, 0, 0}, 
  {-1159./3991680, -919./142560, 0, 919./142560, 1159./3991680, 0, 0}, {-251./665280, -6623./249480, -45803./1197504, 19477./362880, 2861./249480, 
   383./11975040, 0}, {-383./11975040, -2861./249480, -19477./362880, 45803./1197504, 6623./249480, 251./665280, 0}, 
  {0, -1159./3991680, -919./142560, 0, 919./142560, 1159./3991680, 0}, {0, 0, -5./342144, -53./997920, 5./88704, 17./1496880, 0}, 
  {0, 0, 0, 0, 0, 0, 0}}, {{-1./2395008, -1./285120, 1./362880, 1./855360, 0, 0, 0}, {-23./1197504, -1./660, -79./55440, 503./187110, 541./1995840, 
   0, 0}, {-383./11975040, -2861./249480, -19477./362880, 45803./1197504, 6623./249480, 251./665280, 0}, 
  {-1./285120, -503./62370, -45803./399168, 0, 45803./399168, 503./62370, 1./285120}, {0, -251./665280, -6623./249480, -45803./1197504, 
   19477./362880, 2861./249480, 383./11975040}, {0, 0, -541./1995840, -503./187110, 79./55440, 1./660, 23./1197504}, 
  {0, 0, 0, -1./855360, -1./362880, 1./285120, 1./2395008}}, {{0, 0, 0, 0, 0, 0, 0}, {0, -17./1496880, -5./88704, 53./997920, 5./342144, 0, 0}, 
  {0, -1159./3991680, -919./142560, 0, 919./142560, 1159./3991680, 0}, {0, -251./665280, -6623./249480, -45803./1197504, 19477./362880, 
   2861./249480, 383./11975040}, {0, -383./11975040, -2861./249480, -19477./362880, 45803./1197504, 6623./249480, 251./665280}, 
  {0, 0, -1159./3991680, -919./142560, 0, 919./142560, 1159./3991680}, {0, 0, 0, -5./342144, -53./997920, 5./88704, 17./1496880}}, 
 {{0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0}, {0, 0, -5./342144, -53./997920, 5./88704, 17./1496880, 0}, 
  {0, 0, -541./1995840, -503./187110, 79./55440, 1./660, 23./1197504}, {0, 0, -1159./3991680, -919./142560, 0, 919./142560, 1159./3991680}, 
  {0, 0, -23./1197504, -1./660, -79./55440, 503./187110, 541./1995840}, {0, 0, 0, -17./1496880, -5./88704, 53./997920, 5./342144}}, 
 {{0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, -1./855360, -1./362880, 1./285120, 1./2395008}, 
  {0, 0, 0, -5./342144, -53./997920, 5./88704, 17./1496880}, {0, 0, 0, -17./1496880, -5./88704, 53./997920, 5./342144}, 
  {0, 0, 0, -1./2395008, -1./285120, 1./362880, 1./855360}}};

public	void CalcDD_Drop()
	{
	
		int l2,m2,n2;
		int l1,m1,n1;
		
		int apex=(int)((curve.iapex+0.5*curve.Nsamples)/curve.Nsamples)-1;		//conversion from ncurvepts to npoints
		//IJ.write("apex" +apex);
		double[] CGx= new double[curve.npoints];
		double[] CGy= new double[curve.npoints];
		
		double[] CX= new double[curve.npoints];
		double[] CY= new double[curve.npoints];
	
	
		//DX contain the equivalent coefficients values for the half drop system: all x positives, y starting up from the interface
	for(int i=0;i<curve.npoints;i++) {
		CX[i] = curve.CoeffX[i]-curve.xapex;
		CY[i] = yh-curve.CoeffY[i];
	}
			
	//kernel is 7*7  but using only 5*5 kernel gives good results
	for(int a=0;a<apex;a++) {
		for(int i=0;i<5;i++) {			//index table ijK must be positive, but in reality, for bspline centered
			l2 = a+i-2;
			if(l2<0) l2=0;
			for(int j=0;j<5;j++) {
				m2 = a+j-2;
				if(m2<0) m2=0;
				for(int K=0;K<5;K++) {
					n2 = a+K-2;
					if(n2<0) n2=0;
								
					CGx[a] += -CY[l2]*CY[m2]*CX[n2] * KernelCG[i+1][j+1][K+1];
					       
					CGy[a] +=  -CX[l2]*CX[n2]*CY[m2]*KernelCG[i+1][j+1][K+1];
										        
					l1=-(K-2) + a;
					m1=i-2 -(K-2) + a;
					n1=j-2 -(K-2) + a;
					
					if(l1>=curve.npoints) l1=curve.npoints-1;
					if(m1>=curve.npoints) m1=curve.npoints-1;
					if(n1>=curve.npoints) n1=curve.npoints-1;								
					
					if(l1<0) l1=0;
					if(m1<0) m1=0;
					if(n1<0) n1=0;
					
					CGx[a] += -(CY[l1]*CY[m1]*CX[n1]) * KernelCG[i+1][j+1][K+1];
				}
			}
		}
	}

	for(int a=apex;a<curve.npoints;a++) {
		for(int i=0;i<5;i++) {			//index table ijK must be positive, but in reality, for bspline centered
			l2 = a+i-2;
			if(l2<0) l2=0;
			if(l2>=curve.npoints) l2=curve.npoints-1;
			for(int j=0;j<5;j++) {
				m2 = a+j-2;
				if(m2>=curve.npoints) m2=curve.npoints-1;
				if(m2<0) m2=0;
				for(int K=0;K<5;K++) {
					n2 = a+K-2;
					if(n2>=curve.npoints) n2=curve.npoints-1;
					if(n2<0) n2=0;
								
					CGx[a] += CY[l2]*CY[m2]*CX[n2] * KernelCG[i+1][j+1][K+1];
					       
					CGy[a] +=  CX[l2]*CX[n2]*CY[m2]*KernelCG[i+1][j+1][K+1];
					
					l1=-(K-2) + a;
					m1=i-2 -(K-2) + a;
					n1=j-2 -(K-2) + a;
					if(l1>=curve.npoints) l1=curve.npoints-1;
					if(m1>=curve.npoints) m1=curve.npoints-1;
					if(n1>=curve.npoints) n1=curve.npoints-1;								
					
					if(l1<0) l1=0;
					if(m1<0) m1=0;
					if(n1<0) n1=0;
					
					CGx[a] += (CY[l1]*CY[m1]*CX[n1]) * KernelCG[i+1][j+1][K+1];
					
				}
			}
		}		
	}
		double S1x= ST_LG*2 * Math.PI*curve.lengthNormal;
		//S1x=0.0;
		for(int i=0;i<apex;i++)
			{
				DC[i] = CGx[i]*Math.PI - S1x;
				DD[i] = -CGy[i]*2*Math.PI;
			}
		for(int i=apex;i<curve.npoints;i++)
			{
				DC[i] = CGx[i]*Math.PI + S1x;
				DD[i] = -CGy[i]*2*Math.PI;
			}
		DC[0] += ST_LS*Math.PI*CX[0]; 
		DC[curve.npoints-1] += ST_LS*Math.PI*CX[curve.npoints-1];
	return;
}
	
	/*private int checkBounds(int k, int kmax) {
		if(k<0) return 0;
		if(k>=kmax) return kmax-1;
		return k;
	}*/

} //end of class