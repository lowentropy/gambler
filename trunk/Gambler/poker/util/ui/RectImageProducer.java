/*
 * CharImageProducer.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.util.ui;

import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageProducer;
import java.util.LinkedList;
import java.util.List;

import poker.common.Rect;


public class RectImageProducer implements ImageProducer
{

	/** composition of character parts */
	private Rect				rect;

	/** consumers of image */
	private List<ImageConsumer>	consumers;

	/** colormodel of image */
	private ColorModel			cm;

	/** width of rectangle */
	int							width;

	/** height of rectangle */
	int							height;


	/**
	 * Constructor.
	 * 
	 * @param c
	 * @param width
	 */
	public RectImageProducer(Rect rect)
	{
		this.rect = rect;
		width = rect.getWidth();
		height = rect.getHeight();
		consumers = new LinkedList<ImageConsumer>();
		cm = new DirectColorModel(8, 7, (7 << 3), (3 << 6));
	}


	/**
	 * @see java.awt.image.ImageProducer#addConsumer(java.awt.image.ImageConsumer)
	 */
	public void addConsumer(ImageConsumer ic)
	{
		if (!isConsumer(ic))
			consumers.add(ic);
		ic.setDimensions(width, height);
		ic.setColorModel(cm);
		ic.setHints(ImageConsumer.RANDOMPIXELORDER);
		ic.setPixels(0, 0, width, height, cm, rect.getPixels(), 0, width);
		ic.imageComplete(ImageConsumer.SINGLEFRAMEDONE);
	}


	/**
	 * @see java.awt.image.ImageProducer#isConsumer(java.awt.image.ImageConsumer)
	 */
	public boolean isConsumer(ImageConsumer ic)
	{
		return consumers.contains(ic);
	}


	/**
	 * @see java.awt.image.ImageProducer#removeConsumer(java.awt.image.ImageConsumer)
	 */
	public void removeConsumer(ImageConsumer ic)
	{
		consumers.remove(ic);
	}


	/**
	 * @see java.awt.image.ImageProducer#startProduction(java.awt.image.ImageConsumer)
	 */
	public void startProduction(ImageConsumer ic)
	{
		addConsumer(ic);
	}


	/**
	 * @see java.awt.image.ImageProducer#requestTopDownLeftRightResend(java.awt.image.ImageConsumer)
	 */
	public void requestTopDownLeftRightResend(ImageConsumer ic)
	{
		ic.setPixels(0, 0, width, height, cm, rect.getPixels(), 0, width);
		ic.imageComplete(ImageConsumer.SINGLEFRAMEDONE);
	}

}
