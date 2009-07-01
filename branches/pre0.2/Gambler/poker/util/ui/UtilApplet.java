/*
 * UtilApplet.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.util.ui;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JTextField;


public class UtilApplet extends JApplet implements ActionListener
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -9165934034106051742L;

	private JTextField			file;


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


	/**
	 * @see java.applet.Applet#destroy()
	 */
	@Override
	public void destroy()
	{
		super.destroy();

		setLastFile();
	}


	private void _init()
	{
		setLayout(new FlowLayout());
		file = new JTextField(getLastFile(), 40);
		JButton b = new JButton("Load");
		b.setActionCommand("load");
		b.addActionListener(this);
		getContentPane().add(file);
		getContentPane().add(b);
		b = new JButton("Load Raw");
		b.setActionCommand("raw");
		b.addActionListener(this);
		getContentPane().add(b);
		this.resize(450, 100);
	}


	public void actionPerformed(ActionEvent e)
	{
		String cmd = e.getActionCommand();
		if (cmd.equals("load"))
		{
			File f = new File(file.getText());
			if (!f.canRead())
			{
				System.err.println("bad filename");
				return;
			}
			try
			{
				new ImageViewerFrame(f).setVisible(true);
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
		}
		else if (cmd.equals("raw"))
		{
			File f = new File(file.getText());
			if (!f.canRead())
			{
				System.err.println("bad filename");
				return;
			}
			try
			{
				new ImageViewerFrame(f).setVisible(true);
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
		}
	}


	private String getLastFile()
	{
		try
		{
			FileInputStream fis = new FileInputStream("conf/lastimg");
			BufferedReader r = new BufferedReader(new InputStreamReader(fis));
			String s = r.readLine();
			r.close();
			return s;
		}
		catch (IOException e)
		{
			return "";
		}
	}


	private void setLastFile()
	{
		FileOutputStream fos = null;
		try
		{
			fos = new FileOutputStream("conf/lastimg");
		}
		catch (FileNotFoundException e)
		{
			try
			{
				new File("conf/lastimg").createNewFile();
				fos = new FileOutputStream("conf/lastimg");
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
				return;
			}
		}
		
		PrintWriter w = new PrintWriter(new OutputStreamWriter(fos));
		w.println(file.getText());
		w.close();	
	}
}
