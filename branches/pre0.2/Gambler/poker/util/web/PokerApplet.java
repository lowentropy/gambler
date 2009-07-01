/*
 * PokerApplet.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.util.web;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;

import javax.swing.ButtonGroup;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.Timer;

import poker.util.vnc.VncClient;


/**
 * Applet to view all three tiers of servers for the poker architecture. Also
 * allows snooping on poker games as well as manual control
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class PokerApplet extends JApplet implements MouseListener, KeyListener,
		ActionListener, MouseMotionListener
{

	/** serial version UID */
	private static final long	serialVersionUID	= 2110552635625928039L;

	/** VNC Client */
	private VncClient			vnc;

	/** VNC Canvas */
	private VncCanvas			canvas;

	/** Swing Timer */
	private Timer				timer;

	private Timer				imgSaveTimer;

	/** radio buttons to select mode */
	private JRadioButton[]		modeRB;

	private JTextField			hostTf;

	private JTextField			portTf;

	private JPasswordField		passTf;
	
	private boolean keyDebug = true;


	public void init()
	{
		try
		{
			javax.swing.SwingUtilities.invokeAndWait(new Runnable()
				{

					public void run()
					{
						_init();
					}
				});
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}


	public void destroy()
	{
		try
		{
			timer.stop();
			vnc.stop();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}


	private void _init()
	{
		vnc = new VncClient();
		setLayout(new BorderLayout());

		canvas = new VncCanvas(vnc.getFramebuffer());
		getContentPane().add(canvas, BorderLayout.SOUTH);

		modeRB = new JRadioButton[2];
		modeRB[0] = new JRadioButton("Interactive", true);
		modeRB[1] = new JRadioButton("Debugging", false);
		modeRB[0].setActionCommand("nodebug");
		modeRB[1].setActionCommand("debug");
		modeRB[0].addActionListener(this);
		modeRB[1].addActionListener(this);
		ButtonGroup bGrp = new ButtonGroup();
		bGrp.add(modeRB[0]);
		bGrp.add(modeRB[1]);

		JPanel cPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		hostTf = new JTextField("localhost", 20);
		portTf = new JTextField("5900", 4);
		passTf = new JPasswordField(8);
		passTf.setText("d3lt4Sigt0");
		cPanel.add(hostTf);
		cPanel.add(portTf);
		cPanel.add(passTf);

		JButton cntBtn = new JButton("Connect");
		JButton dctBtn = new JButton("Disconnect");
		cntBtn.setActionCommand("connect");
		dctBtn.setActionCommand("disconnect");
		cntBtn.addActionListener(this);
		dctBtn.addActionListener(this);
		cPanel.add(cntBtn);
		cPanel.add(dctBtn);

		JPanel bPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		bPanel.add(modeRB[0]);
		bPanel.add(modeRB[1]);

		getContentPane().add(cPanel, BorderLayout.NORTH);
		getContentPane().add(bPanel, BorderLayout.CENTER);

		timer = new Timer(10, this);
		timer.setCoalesce(false);

		imgSaveTimer = new Timer(10000, this);

		canvas.addMouseListener(this);
		canvas.addKeyListener(this);
		canvas.addMouseMotionListener(this);
	}


	public void mouseClicked(MouseEvent e)
	{
		if (vnc.inputMode == VncClient.INTERACTIVE)
		{
			// try
			// {
			// vnc.mouseDown(getBtn(e.getButton()), e.getX(), e.getY());
			// vnc.mouseUp(getBtn(e.getButton()), e.getX(), e.getY());
			// }
			// catch (IOException e1)
			// {
			// e1.printStackTrace();
			//			}
		}
		else
		{
			vnc.altMouseClick(getBtn(e.getButton()), e.getX(), e.getY());
		}
	}


	public void mousePressed(MouseEvent e)
	{
		if (vnc.inputMode == VncClient.INTERACTIVE)
		{
			try
			{
				vnc.mouseDown(getBtn(e.getButton()), e.getX(), e.getY());
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
		}
		else
		{
			vnc.altMouseDown(getBtn(e.getButton()), e.getX(), e.getY());
		}
	}


	public void mouseReleased(MouseEvent e)
	{
		if (vnc.inputMode == VncClient.INTERACTIVE)
		{
			try
			{
				vnc.mouseUp(getBtn(e.getButton()), e.getX(), e.getY());
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
		}
		else
		{
			vnc.altMouseUp(getBtn(e.getButton()), e.getX(), e.getY());
		}
	}


	public void mouseEntered(MouseEvent e)
	{
	}


	public void mouseExited(MouseEvent e)
	{
	}


	public void keyTyped(KeyEvent e)
	{
		if (vnc.inputMode == VncClient.INTERACTIVE)
		{
			// try
			// {
			// vnc.keyEvent(e);
			// }
			// catch (IOException e1)
			// {
			// e1.printStackTrace();
			// }
		}
	}


	public void keyPressed(KeyEvent e)
	{
		if (vnc.inputMode == VncClient.INTERACTIVE && !keyDebug)
		{
			try
			{
				vnc.keyEvent(e);
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
		}
		else
		{
			try
			{
				char keyChar = e.getKeyChar();
				if (keyChar == 's')
					vnc.saveImage();
				else if (keyChar == 'r')
					vnc.setImageCount(0);
				else if (keyChar == 't')
				{
					if (imgSaveTimer.isRunning())
						imgSaveTimer.stop();
					else
						imgSaveTimer.start();
				}

			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
		}

	}


	public void doTimedImageSaves()
	{

	}


	public void keyReleased(KeyEvent e)
	{
		if (vnc.inputMode == VncClient.INTERACTIVE)
		{
			try
			{
				vnc.keyEvent(e);
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
		}
	}


	public void actionPerformed(ActionEvent e)
	{
		String cmd = e.getActionCommand();
		if (cmd == null)
		{
			try
			{
				if (e.getSource() == timer)
					canvas.repaint();
				else if (e.getSource() == imgSaveTimer)
					vnc.saveImage();
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
		}
		else if (cmd.equals("debug"))
		{
			vnc.inputMode = VncClient.DEBUG;
		}
		else if (cmd.equals("nodebug"))
		{
			vnc.inputMode = VncClient.INTERACTIVE;
		}
		else if (cmd.equals("connect"))
		{
			try
			{
				vnc.connect(hostTf.getText(), Integer
						.parseInt(portTf.getText()), passTf.getPassword());
				timer.start();
				//imgSaveTimer.start(); // DBG
			}
			catch (NumberFormatException e1)
			{
				portTf.requestFocusInWindow();
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
		}
		else if (cmd.equals("disconnect"))
		{
			try
			{
				timer.stop();
				vnc.stop();
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
		}
	}


	private int getBtn(int meb)
	{
		switch (meb) {
		case MouseEvent.BUTTON1:
			return 0;
		case MouseEvent.BUTTON2:
			return 1;
		case MouseEvent.BUTTON3:
			return 2;
		}
		return -1;
	}


	public void mouseDragged(MouseEvent e)
	{
		if (vnc.inputMode == VncClient.INTERACTIVE)
		{
			try
			{
				vnc.mouseMoved(e.getX(), e.getY());
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
		}
	}


	public void mouseMoved(MouseEvent e)
	{
		if (vnc.inputMode == VncClient.INTERACTIVE)
		{
			try
			{
				vnc.mouseMoved(e.getX(), e.getY());
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
		}
	}
}
