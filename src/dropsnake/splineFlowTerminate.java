// Decompiled by Jad v1.5.8c. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   SmallDialogs.java
package dropsnake;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class splineFlowTerminate extends Dialog
	implements ActionListener
{

	public void actionPerformed(ActionEvent actionevent)
	{
		setVisible(false);
		if(!actionevent.getActionCommand().equals("Done") && actionevent.getActionCommand().equals("Cancel"))
			cancel = true;
	}

	public boolean choseCancel()
	{
		return cancel;
	}

	public Insets getInsets()
	{
		return new Insets(0, 40, 20, 40);
	}

	splineFlowTerminate(Frame frame)
	{
		super(frame, "Back to ImageJ", true);
		cancel = false;
		setLayout(new GridLayout(0, 1));
		Button button = new Button("Done");
		Button button1 = new Button("Cancel");
		button.addActionListener(this);
		button1.addActionListener(this);
		Label label = new Label("");
		Label label1 = new Label("");
		add(label);
		add(button);
		add(label1);
		add(button1);
		pack();
	}

	private final CheckboxGroup choice = new CheckboxGroup();
	private boolean cancel;
}
