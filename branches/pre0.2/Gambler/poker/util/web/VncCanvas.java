/*
 * VncCanvas.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.util.web;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;

import poker.common.Rect;
import poker.util.vnc.FbUpdateListener;
import poker.util.vnc.Framebuffer;


public class VncCanvas extends Canvas implements FbUpdateListener
{

	/** serial version UID */
	private static final long	serialVersionUID	= 5993220665860057446L;

	/** framebuffer */
	private Framebuffer			fb;

	/** image containing raw pixels */
	private Image				img;


	/**
	 * Constructor.
	 * 
	 * @param fb
	 *            framebuffer to display
	 */
	public VncCanvas(Framebuffer fb)
	{
		this.fb = fb;
		this.img = this.createImage(fb);
		this.fb.addUpdateListener(this);
	}


	/**
	 * @see java.awt.Canvas#paint(java.awt.Graphics)
	 */
	public void paint(Graphics g)
	{
		g.drawImage(img, 0, 0, this);
	}


	/**
	 * @see java.awt.Canvas#update(java.awt.Graphics)
	 */
	public void update(Graphics g)
	{
		paint(g);
	}


	/**
	 */
	public Dimension minimumSize()
	{
		return new Dimension(fb.getWidth(), fb.getHeight());
	}


	/**
	 */
	public Dimension preferredSize()
	{
		return new Dimension(fb.getWidth(), fb.getHeight());
	}


	public void updated(int x, int y, int w, int h)
	{
		repaint(x, y, w, h);
	}


	public Rect getRect()
	{
		return fb.getRect();
	}


	public Framebuffer getFramebuffer()
	{
		return fb;
	}

}
