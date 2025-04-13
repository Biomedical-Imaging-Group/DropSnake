/*====================================================================| Version: March 1, 2003\===================================================================*//*====================================================================| EPFL/STI/IOA/BIG| Mathews Jacob| Bldg. BM-Ecublens 4.141| CH-1015 Lausanne VD|| phone (CET): +41(21)693.51.43| fax: +41(21)693.37.01| RFC-822: Mathews.Jacob@epfl.ch|| URL: http://bigwww.epfl.ch/jacob\===================================================================*//*====================================================================| Additional help available at http://bigwww.epfl.ch/jacob/splineSnake/\===================================================================*/package dropsnake;import ij.plugin.PlugIn;import ij.process.*;import java.awt.*;import java.awt.event.*;import java.io.*;import java.util.*;import java.lang.*;import ij.*;public class DisplayCurve extends Object{			public int   Npoints;				// number of input points	public int   Nsamples;				// number of samples between two input points	public int   Degree;				// order of the B-Spline to use for the curve		public int   NCurvepts;				// number of points in the curve	public boolean   Curvelooping;		// is the curve looping		private int   NTemppoints;				// number of input points	private int   NTempCurvepts;				// number of points in the curve	private double TempCurveX[];				// X points of the curve	private double TempCurveY[];				// Y points of the curve		public double CurveX[];				// X points of the curve	public double CurveY[];				// Y points of the curve			public double Xpoints[];			// array with the X coord of the input points	public double Ypoints[];			// array with the Y coord of the input points	public double CoeffX[];				// X coefficients	public double CoeffY[];				// Y coefficients			public double SplineValues[];		/* ---------------------------------------------------------------------------------------- *//*	Constructor                                                                         	*//*	Purpose:	Initializes the Curve parameters											*//* ---------------------------------------------------------------------------------------- */public  DisplayCurve(int nsamples, double[] xpoints, double[] ypoints, int degree) 	{		int i,j;		String n;		String x;		String y;				int npoints = xpoints.length;		if(ypoints.length != npoints)			IJ.error("Error in DisplayCurve");				NTemppoints = npoints+4;		Npoints = npoints;		Nsamples = nsamples;		Degree = degree;		NTempCurvepts = NTemppoints*Nsamples;		NCurvepts = npoints*Nsamples;						Xpoints = new double[npoints+4];		Ypoints = new double[npoints+4];		CoeffX  = new double[npoints+4];		CoeffY  = new double[npoints+4];		TempCurveX  = new double[NTempCurvepts];		TempCurveY  = new double[NTempCurvepts];		CurveX  = new double[NCurvepts];		CurveY  = new double[NCurvepts];					SplineValues = new double[4*Nsamples+1];				if(npoints == 1)			{				Xpoints[0] = xpoints[0];				Ypoints[0] = ypoints[0];			}		else			{				Xpoints[0] = 2*xpoints[0]-xpoints[1];				Ypoints[0] = 2*ypoints[0]-ypoints[1];			}				Xpoints[1] = Xpoints[0];		Ypoints[1] = Ypoints[0];		for (i = 0; i < npoints; i++)			{				Xpoints[i+2] = xpoints[i];				Ypoints[i+2] = ypoints[i];			}					if(npoints == 1)			{				Xpoints[0] = xpoints[npoints-1]; 				Ypoints[0] = ypoints[npoints-1]; 			}		else			{				Xpoints[0] = 2*xpoints[npoints-1]-xpoints[npoints-2];				Ypoints[0] = 2*ypoints[npoints-1]-ypoints[npoints-2];			}				Xpoints[npoints+2] = xpoints[npoints-1]; 		Ypoints[npoints+2] = ypoints[npoints-1]; 				Xpoints[npoints+3] = Xpoints[npoints+2]; 		Ypoints[npoints+3] = Ypoints[npoints+2]; 				/* Loading with the Spline values, increasing Sequence.*//*-----------------------------------------------------*/	j = 0;		for (i=-2*Nsamples;i<=2*Nsamples;i++)			{			  SplineValues[j++] = BSpline((double)i/(double)Nsamples,degree);		}				/* Finding the Interpolation Coefficients.*/	/*----------------------------------------*/		BsplineTransform bspline = new BsplineTransform(BsplineTransform.PERIODIC);				for (i=0;i<NTemppoints;i++)				{				CoeffX[i] = Xpoints[i];				CoeffY[i] = Ypoints[i];			}		bspline.getInterpolationCoefficients(CoeffX, degree);		bspline.getInterpolationCoefficients(CoeffY, degree);					ComputeCurve (SplineValues, TempCurveX, TempCurveY);				for (i=0;i<NCurvepts;i++)				{				CurveX[i] = TempCurveX[i+2*Nsamples];				CurveY[i] = TempCurveY[i+2*Nsamples];			}				}	/* ------------------------------------------------------------------------------------	*//*	Function:	BSpline																	*//*	Purpose:	calculates B-Spline polynomial											*//* ------------------------------------------------------------------------------------	*/public void updateCurve(double[] derX, double[] derY,double gamma){	for(int i=0; i<Npoints; i++)		{			CoeffX[i+2] += gamma*derX[i];			CoeffY[i+2] += gamma*derY[i];		}		ComputeCurve (SplineValues, TempCurveX, TempCurveY);			for (int i=0;i<NCurvepts;i++)			{			CurveX[i] = TempCurveX[i+2*Nsamples];			CurveY[i] = TempCurveY[i+2*Nsamples];		}}/* ------------------------------------------------------------------------------------	*//*	Function:	BSpline																	*//*	Purpose:	calculates B-Spline polynomial											*//* ------------------------------------------------------------------------------------	*/public double BSpline(double x, int degree) 	{			double	BSplineValue;		x = Math.abs(x);		if (degree == 0)			{				if(x<0.5)	BSplineValue=1.0;				else	BSplineValue=(double)(0.0);			}		else if (degree==1)			{				if(x<1.0)	BSplineValue=1.0-Math.abs(x);				else 	BSplineValue=(double)(0.0);			}		else if (degree==2)			{				if(x<0.5 )				BSplineValue=(3.0/4.0)-x*x;				else if(x<1.5) 			{ x -= 1.5; BSplineValue=x*x/2.0; }				else 					BSplineValue=0.0;			}			else if (degree==3)			{				if(x<1.0)					BSplineValue=(x*x*(x-2.0)*(1.0/2.0)+2.0/3.0);				else if(x<2.0) 				{ x -= 2; BSplineValue=(x*x*x*(-1.0/6.0)); }					else 						BSplineValue=0.0;			}		else 			{				BSplineValue = 0.0;				IJ.write("Bspline with this order not supported: Sorry");			}		return(BSplineValue);	}/* ------------------------------------------------------------------------------------	*//*	Function:	ComputeCurve															*//*	Purpose:	calculates the Curve with the give the cubic spline coefficients		*//*  Inputs : Coeff - Spline coefficients												*//*			 Splinevalues - Values of spline functions 									*//*			 Nsamples	  - Resampling rate												*//* ------------------------------------------------------------------------------------	*/public void ComputeCurve (double[] Splinebuffer,double[] TempCurveX, double[] TempCurveY){ int a; int i; int j; double temp;	double bufferX[] = new double[NTemppoints+3];	double bufferY[] = new double[NTemppoints+3];	/********************************************//* Periodising the coefficent sequence		 *//*********************************************/	bufferX[0] = CoeffX[NTemppoints-1];	bufferY[0] = CoeffY[NTemppoints-1];		for (i=0;i< NTemppoints;i++) 		{			bufferX[i+1] = CoeffX[i];			bufferY[i+1] = CoeffY[i];		 }	bufferX[NTemppoints+1] = CoeffX[0];	bufferY[NTemppoints+1] = CoeffY[0];	bufferX[NTemppoints+2] = CoeffX[1];	bufferY[NTemppoints+2] = CoeffY[1];		/*********************************************//* Evaluating the Curve						 *//*********************************************/			a = 1;			for(i=2*Nsamples;i < NTempCurvepts-2*Nsamples;i++)		{			TempCurveX[i] = 0.0;				TempCurveY[i] = 0.0;			if((i%Nsamples)==0L) a+= 1;			for(j=-1;j<3;j+=1)				{					temp = Splinebuffer[i-(a+j)*Nsamples + 2*Nsamples];					TempCurveX[i]+=bufferX[a+j+1]*temp;					TempCurveY[i]+=bufferY[a+j+1]*temp;					}					}}	}