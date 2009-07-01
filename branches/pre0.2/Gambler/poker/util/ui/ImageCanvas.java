/*
 * CharImageCanvas.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.util.ui;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.ImageObserver;

import poker.common.Rect;


public class ImageCanvas extends Canvas
{

	/** serial version id */
	private static final long	serialVersionUID	= 2389746842915505749L;

	/** image to draw on canvas */
	private Image				image;

	/** dimensions of image */
	private Dimension			dims;


	public ImageCanvas(Rect rect)
	{
		this.image = createImage(new RectImageProducer(rect));
		this.dims = new Dimension(rect.getWidth(), rect.getHeight());
	}


	/**
	 * Constructor.
	 * 
	 * @param image
	 */
	public ImageCanvas(Image image)
	{
		this.image = image;
		this.dims = new Dimension(image.getWidth(this), image.getHeight(this));
	}


	/**
	 * @see java.awt.Component#imageUpdate(java.awt.Image, int, int, int, int,
	 *      int)
	 */
	public boolean imageUpdate(Image img, int infoflags, int x, int y, int w,
			int h)
	{
		if ((infoflags & ImageObserver.WIDTH) != 0)
			dims.width = img.getWidth(this);
		if ((infoflags & ImageObserver.HEIGHT) != 0)
			dims.height = img.getHeight(this);
		return super.imageUpdate(img, infoflags, x, y, w, h);
	}


	/**
	 * @see java.awt.Canvas#paint(java.awt.Graphics)
	 */
	public void paint(Graphics g)
	{
		g.drawImage(image, 0, 0, this);
	}


	/**
	 * @see java.awt.Canvas#update(java.awt.Graphics)
	 */
	public void update(Graphics g)
	{
		paint(g);
	}


	/**
	 * @see java.awt.Component#getMaximumSize()
	 */
	public Dimension getMaximumSize()
	{
		return dims;
	}


	/**
	 * @see java.awt.Component#getMinimumSize()
	 */
	public Dimension getMinimumSize()
	{
		return dims;
	}


	/**
	 * @see java.awt.Component#getPreferredSize()
	 */
	public Dimension getPreferredSize()
	{
		return dims;
	}

}
