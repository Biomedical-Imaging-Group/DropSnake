// Decompiled by Jad v1.5.8c. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   SmallDialogs.java
package dropsnake;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class splineFlowClearAll extends Dialog
	implements ActionListener
{

	public void actionPerformed(ActionEvent actionevent)
	{
		if(actionevent.getActionCommand().equals("Clear All"))
		{
			ph.removePoints();
			ph.curveComputed = false;
			setVisible(false);
		} else
		if(actionevent.getActionCommand().equals("Cancel"))
			setVisible(false);
	}

	public Insets getInsets()
	{
		return new Insets(0, 20, 20, 20);
	}

	splineFlowClearAll(Frame frame, splineSnakeHandler splinesnakehandler, boolean flag)
	{
		super(frame, "Clear Everything", true);
		ph = splinesnakehandler;
		setLayout(new GridLayout(0, 1));
		Button button = new Button("Clear All");
		button.addActionListener(this);
		Button button1 = new Button("Cancel");
		button1.addActionListener(this);
		Label label = new Label("");
		Label label1 = new Label("");
		add(label);
		add(button);
		add(label1);
		add(button1);
		pack();
	}

	private splineSnakeHandler ph;
}
