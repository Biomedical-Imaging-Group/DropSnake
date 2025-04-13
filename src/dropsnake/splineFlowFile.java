// Decompiled by Jad v1.5.8c. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   SmallDialogs.java
package dropsnake;

import ij.IJ;
import ij.ImagePlus;
import ij.text.TextPanel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.Vector;

class splineFlowFile extends Dialog
	implements ActionListener
{

	public void actionPerformed(ActionEvent actionevent)
	{
		setVisible(false);
		if(actionevent.getActionCommand().equals("Save as"))
		{
			Frame frame = new Frame();
			FileDialog filedialog = new FileDialog(frame, "Pointlist", 1);
			String s3 = imp.getTitle();
			int i = s3.lastIndexOf('.');
			if(i == -1)
			{
				filedialog.setFile(s3 + ".txt");
			} else
			{
				s3 = s3.substring(0, i);
				filedialog.setFile(s3 + ".txt");
			}
			filedialog.setVisible(true);
			String s = filedialog.getDirectory();
			s3 = filedialog.getFile();
			if(s == null || s3 == null)
				return;
			try
			{
				FileWriter filewriter = new FileWriter(s + s3);
				Vector vector1 = ph.getPoints();
				String s14;
				for(s14 = String.valueOf(imp.getCurrentSlice()); s14.length() < 5; s14 = " " + s14);
				filewriter.write("Knots     x     y   slice\n");
				for(int i1 = 0; i1 < vector1.size(); i1++)
				{
					String s9;
					for(s9 = String.valueOf(i1); s9.length() < 5; s9 = " " + s9);
					Point point1 = (Point)vector1.elementAt(i1);
					String s11;
					for(s11 = String.valueOf(point1.x); s11.length() < 5; s11 = " " + s11);
					String s13;
					for(s13 = String.valueOf(point1.y); s13.length() < 5; s13 = " " + s13);
					filewriter.write(s9 + " " + s11 + " " + s13 + " " + s14 + "\n");
				}

				filewriter.close();
			}
			catch(IOException _ex)
			{
				IJ.error("IOException exception");
			}
			catch(SecurityException _ex)
			{
				IJ.error("Security exception");
			}
		} else
		if(actionevent.getActionCommand().equals("Show"))
		{
			Vector vector = ph.getPoints();
			String s7;
			for(s7 = String.valueOf(imp.getCurrentSlice()); s7.length() < 7; s7 = " " + s7);
			IJ.getTextPanel().setFont(new Font("Monospaced", 0, 12));
			IJ.setColumnHeadings(" pointno\t      x\t      y\t  slice");
			for(int j = 0; j < vector.size(); j++)
			{
				String s1;
				for(s1 = String.valueOf(j); s1.length() < 6; s1 = " " + s1);
				Point point = (Point)vector.elementAt(j);
				String s4;
				for(s4 = String.valueOf(point.x); s4.length() < 7; s4 = " " + s4);
				String s6;
				for(s6 = String.valueOf(point.y); s6.length() < 7; s6 = " " + s6);
				IJ.write(s1 + "\t" + s4 + "\t" + s6 + "\t" + s7);
			}

		} else
		if(actionevent.getActionCommand().equals("Open"))
		{
			Frame frame1 = new Frame();
			FileDialog filedialog1 = new FileDialog(frame1, "pointlist", 0);
			filedialog1.setVisible(true);
			String s2 = filedialog1.getDirectory();
			String s5 = filedialog1.getFile();
			if(s2 == null || s5 == null)
				return;
			try
			{
				FileReader filereader = new FileReader(s2 + s5);
				BufferedReader bufferedreader = new BufferedReader(filereader);
				ph.removePoints();
				String s8;
				if((s8 = bufferedreader.readLine()) == null)
				{
					filereader.close();
					return;
				}
				while((s8 = bufferedreader.readLine()) != null) 
				{
					s8 = s8.trim();
					int k = s8.indexOf(' ');
					if(k == -1)
					{
						filereader.close();
						IJ.error("Invalid file");
						return;
					}
					s8 = s8.substring(k);
					s8 = s8.trim();
					k = s8.indexOf(' ');
					if(k == -1)
					{
						filereader.close();
						IJ.error("Invalid file");
						return;
					}
					String s10 = s8.substring(0, k);
					s10 = s10.trim();
					s8 = s8.substring(k);
					s8 = s8.trim();
					k = s8.indexOf(' ');
					if(k == -1)
						k = s8.length();
					String s12 = s8.substring(0, k);
					s12 = s12.trim();
					int l = Integer.parseInt(s10);
					int j1 = Integer.parseInt(s12);
					ph.addPoint(l, j1);
				}
				ph.addPoint(0, 0);
				ph.InitializeClosedCurve();
				filereader.close();
			}
			catch(FileNotFoundException _ex)
			{
				IJ.error("File not found exception");
			}
			catch(IOException _ex)
			{
				IJ.error("IOException exception");
			}
			catch(NumberFormatException _ex)
			{
				IJ.error("Number format exception");
			}
		} else
		{
			actionevent.getActionCommand().equals("Cancel");
		}
	}

	public Insets getInsets()
	{
		return new Insets(0, 20, 20, 20);
	}

	splineFlowFile(Frame frame, splineSnakeHandler splinesnakehandler, ImagePlus imageplus)
	{
		super(frame, "splineFlow List", true);
		ph = splinesnakehandler;
		imp = imageplus;
		setLayout(new GridLayout(0, 1));
		Button button = new Button("Save as");
		Button button1 = new Button("Show");
		Button button2 = new Button("Open");
		Button button3 = new Button("Cancel");
		button.addActionListener(this);
		button1.addActionListener(this);
		button2.addActionListener(this);
		button3.addActionListener(this);
		Label label = new Label("");
		Label label1 = new Label("");
		add(label);
		add(button);
		add(button1);
		add(button2);
		add(label1);
		add(button3);
		pack();
	}

	private final CheckboxGroup choice = new CheckboxGroup();
	private ImagePlus imp;
	private splineSnakeHandler ph;
}
