/*
 * UtilCanvas.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.util.ui;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageProducer;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

import poker.ai.AIError;
import poker.ai.Block;
import poker.ai.ocr.GraphicsAI;
import poker.common.Coord;
import poker.common.Rect;


public class UtilCanvas extends Canvas implements ImageProducer, MouseListener
{

	private static final long	serialVersionUID	= 6035288573185195003L;

	private int					mx1					= -1, mx2 = -1, my1 = -1,
			my2 = -1;

	private Rect				rect, backupRect;

	private Image				img;

	private int					width, height;

	private ColorModel			cm;

	private List<ImageConsumer>	consumers;

	private static final byte	RED					= (byte) 0x7;

	private static final byte	GREEN				= (byte) 0x38;

	private static final byte	BLUE				= (byte) 0xC0;

	private static final byte	WHITE				= (byte) 0xff;

	private static final byte	BLACK				= (byte) 0x00;

	private byte				fgColor				= 0x0;

	private byte				olColor				= GREEN;

	private int					fgTol				= 1;

	private byte				bdColor				= RED;

	private byte				cursorColor			= RED;

	private boolean				doPaste;

	private Rect				buf;

	private List<Block>			exBlocks;

	private Rect				classRect;

	private List<Rect>			exRects;

	private int					clickx				= -1;

	private int					clicky				= -1;

	private List<Coord>			pts;


	/**
	 * Constructor.
	 */
	public UtilCanvas()
	{
		cm = new DirectColorModel(8, 7, (7 << 3), (3 << 6));
		consumers = new LinkedList<ImageConsumer>();
		img = createImage(this);
		pts = new ArrayList<Coord>();
	}


	/**
	 * Load an image file into this producer.
	 * 
	 * @param file
	 * @throws IOException
	 */
	public void loadImage(File file, boolean raw) throws IOException
	{
		byte[] pixels;
		if (raw)
		{
			DataInputStream dis = new DataInputStream(new FileInputStream(file));
			width = dis.readInt();
			height = dis.readInt();
			pixels = new byte[width * height];
			dis.readFully(pixels);
			dis.close();
		}
		else
		{
			BufferedImage bimg = ImageIO.read(file);
			width = bimg.getWidth();
			height = bimg.getHeight();
			pixels = new byte[width * height];
			int[] data = new int[width * height];
			bimg.getRGB(0, 0, width, height, data, 0, width);
			for (int i = 0; i < data.length; i++)
			{
				int p = data[i];
				int r = (p >>> 16) & 0xff;
				int g = (p >>> 8) & 0xff;
				int b = (p >>> 0) & 0xff;
				pixels[i] = insertRGB(r, g, b);
			}
		}
		rect = new Rect(pixels, width, height);
		backupRect = rect.clone();
		for (ImageConsumer ic : consumers)
		{
			ic.setDimensions(width, height);
			ic.setColorModel(cm);
			ic.setHints(ImageConsumer.RANDOMPIXELORDER);
			ic.setPixels(0, 0, width, height, cm, pixels, 0, width);
			ic.imageComplete(ImageConsumer.SINGLEFRAMEDONE);
		}
	}


	public void resetMarks()
	{
		backupRect.copyInto(rect);
		update(0, 0, width, height);
		pts.clear();
		mx1 = my1 = mx2 = my2 = -1;
	}


	public int[] mark(int x, int y)
	{
		pts.add(new Coord(x, y));
		if ((x < mx1) || (mx1 == -1))
			mx1 = x;
		if ((x > mx2) || (mx2 == -1))
			mx2 = x;
		if ((y < my1) || (my1 == -1))
			my1 = y;
		if ((y > my2) || (my2 == -1))
			my2 = y;
		return new int[] {mx1, my1, mx2 - mx1 + 1, my2 - my1 + 1};
	}


	public String getRGB(int x, int y)
	{
		int[] c = extractRGB(backupRect.getPixel(x, y));
		return "(" + c[0] + "," + c[1] + "," + c[2] + ")";
	}


	private void update(int x, int y, int w, int h)
	{
		if (x < 0)
		{
			w -= x;
			x = 0;
		}
		if (y < 0)
		{
			h -= y;
			y = 0;
		}
		if ((x + w) > rect.getWidth())
			w = rect.getWidth() - x;

		if ((y + h) > rect.getHeight())
			h = rect.getHeight() - y;

		for (ImageConsumer ic : consumers)
		{
			byte[] p = rect.getPixels();
			ic.setPixels(x, y, w, h, cm, p, y * width + x, width);
			ic.imageComplete(ImageConsumer.SINGLEFRAMEDONE);
		}
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


	public void addConsumer(ImageConsumer ic)
	{
		if (ic != null)
		{
			if (!isConsumer(ic))
				consumers.add(ic);
			ic.setDimensions(width, height);
			ic.setColorModel(cm);
			ic.setHints(ImageConsumer.RANDOMPIXELORDER);
			ic.setPixels(0, 0, width, height, cm, rect.getPixels(), 0, width);
			ic.imageComplete(ImageConsumer.SINGLEFRAMEDONE);
		}
	}


	public boolean isConsumer(ImageConsumer ic)
	{
		return consumers.contains(ic);
	}


	public void removeConsumer(ImageConsumer ic)
	{
		consumers.remove(ic);
	}


	public void startProduction(ImageConsumer ic)
	{
		addConsumer(ic);
	}


	public void requestTopDownLeftRightResend(ImageConsumer ic)
	{
		// do nothing
	}


	public void mask(int tol)
	{
		getAnyBuffer();
		createMask(tol);
		paste(mx1, my1, false);
	}


	private void getAnyBuffer()
	{
		getBuffer();
		if (buf == null)
		{
			mx1 = 0;
			my1 = 0;
			mx2 = rect.getWidth() - 1;
			my2 = rect.getHeight() - 1;
			getBuffer();
		}
	}


	public void extract(int tol) throws AIError
	{
		getAnyBuffer();
		extractChars(tol);
	}


	private void createMask(int tol)
	{
		GraphicsAI ai = new GraphicsAI();

		boolean[] mask = ai.createMask(buf, fgColor, tol);
		int w = buf.getWidth();
		int h = buf.getHeight();
		setBuffer(Rect.fromMask(mask, w, h, BLACK, WHITE));
	}


	public int width()
	{
		return rect.getWidth();
	}


	public int height()
	{
		return rect.getHeight();
	}


	private void extractChars(int tol) throws AIError
	{
		GraphicsAI ai = new GraphicsAI();
		List<Block> blocks = ai.getBlocks(buf, mx1, my1, fgColor, tol, false);
		for (Block b : blocks)
		{
			rect.drawPixels(b.outline, b.x, b.y, olColor);
			update(b.x - 1, b.y - 1, b.width + 2, b.height + 2);
		}
		this.exBlocks = blocks;
	}


	private void setBuffer(Rect rect)
	{
		buf = rect;
	}


	private void getBuffer()
	{
		if (mx1 < 0)
			mx1 = 0;
		if (my1 < 0)
			my1 = 0;
		if (mx2 >= width)
			mx2 = width - 1;
		if (my2 >= height)
			my2 = height - 1;

		if ((mx1 == mx2) && (my1 == my2))
		{
			buf = null;
			return;
		}

		buf = rect.sub(mx1, my1, mx2 - mx1 + 1, my2 - my1 + 1, false).clone();
	}


	public void mouseClicked(MouseEvent e)
	{
		clickx = e.getX();
		clicky = e.getY();
		if (doPaste)
			paste(clickx, clicky, false);
		else
		{
			printColor(clickx, clicky);
			setForegroundFrom(clickx, clicky);
		}
		doPaste = false;
	}


	void setForegroundFrom(int x, int y)
	{
		fgColor = rect.getPixel(x, y);
	}


	private void printColor(int x, int y)
	{
		int[] c = extractRGB(rect.getPixel(x, y));
		System.out.printf("pixel at (%d, %d), RGB = %d %d %d\n", x, y, c[0],
				c[1], c[2]);
	}


	private void paste(int x, int y, boolean outline)
	{
		buf.copyOnto(rect, x, y, true);

		if (outline)
			buf.frameOnto(rect, x, y, bdColor, true);
		update(x - 1, y - 1, buf.getWidth() + 1, buf.getHeight() + 1);
	}


	public void mousePressed(MouseEvent e)
	{
		mx1 = e.getX();
		my1 = e.getY();
	}


	public void mouseReleased(MouseEvent e)
	{
		int t;
		mx2 = e.getX();
		my2 = e.getY();
		if (mx1 > mx2)
		{
			t = mx1;
			mx1 = mx2;
			mx2 = t;
		}
		if (my1 > my2)
		{
			t = my1;
			my1 = my2;
			my2 = t;
		}
		if ((mx1 != mx2) || (my1 != my2))
			drawRect();
	}


	private void drawRect()
	{
		new Rect(null, mx2 - mx1 + 1, my2 - my1 + 1).frameOnto(rect, mx1, my1,
				bdColor, true);
		update(mx1 - 1, my1 - 1, mx2 - mx1 + 3, my2 - my1 + 3);
	}


	public void mouseEntered(MouseEvent e)
	{
	}


	public void mouseExited(MouseEvent e)
	{
	}


	private byte insertRGB(int r, int g, int b)
	{
		return (byte) ((r & 0x7) | ((g & 0x7) << 3) | ((b & 0x3) << 6));
	}


	private int[] extractRGB(byte rgb)
	{
		return new int[] {rgb & 0x7, (rgb >>> 3) & 0x7, (rgb >>> 6) & 0x3};
	}


	public void classify(int tol)
	{
		if (this.exBlocks == null)
			return;

		classRect = backupRect.sub(mx1, my1, buf.getWidth(), buf.getHeight(),
				false).clone();
		backupRect.sub(mx1 - 1, my1 - 1, buf.getWidth() + 2,
				buf.getHeight() + 2, true).copyOnto(rect, mx1 - 1, my1 - 1,
				true);

		List<Rect> rects = new ArrayList<Rect>(exBlocks.size());

		for (Block b : this.exBlocks)
			rects.add(Rect.mask(b.mask, b.width, b.height, BLACK, WHITE));

		new CharsetFrame(tol, fgColor, rects, exBlocks, classRect).setVisible(true);
	}


	public int getTolerance()
	{
		return this.fgTol;
	}


	public String dump()
	{
		getAnyBuffer();
		return buf.toHex(40);
	}


	/**
	 * @see java.awt.Component#getMinimumSize()
	 */
	public Dimension getMinimumSize()
	{
		return new Dimension(rect.getWidth(), rect.getHeight());
	}


	/**
	 * @see java.awt.Component#getPreferredSize()
	 */
	public Dimension getPreferredSize()
	{
		return new Dimension(rect.getWidth(), rect.getHeight());
	}


	/**
	 * @see java.awt.Component#getMaximumSize()
	 */
	public Dimension getMaximumSize()
	{
		return new Dimension(rect.getWidth(), rect.getHeight());
	}


	public void updateAll()
	{
		update(0, 0, rect.getWidth(), rect.getHeight());
	}


	public void clearBuffer()
	{
		mx1 = mx2 = my1 = my2 = 0;
	}


	public byte getFGColor()
	{
		return this.fgColor;
	}


	public Rect getLastClassifiedRect()
	{
		return classRect;
	}


	public List<Rect> getExtractedRects()
	{
		return exRects;
	}


	public List<Block> getExtractedBlocks()
	{
		return exBlocks;
	}


	public Rect getRect()
	{
		return this.rect;
	}


	public boolean getSelectedRange(int[] p)
	{
		if (mx1 == -1)
			return false;

		p[0] = mx1;
		p[1] = my1;
		p[2] = mx2 - mx1 + 1;
		p[3] = my2 - my1 + 1;

		return true;
	}


	public boolean getSelectedPoint(int[] p)
	{
		if (clickx == -1)
			return false;

		p[0] = clickx;
		p[1] = clicky;

		return true;
	}


	public void crop(int x, int y, int w, int h)
	{
		backupRect = backupRect.sub(x, y, w, h, false).clone();
		rect = backupRect.clone();
		width = w;
		height = h;
		for (ImageConsumer ic : consumers)
			ic.setDimensions(w, h);

		update(0, 0, w, h);
	}


	private int	prevCX	= -1, prevCY;

	public void drawBox(int x1, int y1, int x2, int y2, byte c)
	{
		rect.drawBox(x1, y1, x2, y2, c, true);
		update(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
	}

	public void redrawCursor(int cx, int cy)
	{
		if (prevCX != -1)
		{
			int tx = prevCX - 3;
			if (tx < 0)
				tx = 0;
			int ty = prevCY - 3;
			if (ty < 0)
				ty = 0;

			backupRect.sub(prevCX - 3, prevCY - 3, 7, 7, true).copyOnto(rect,
					tx, ty, false);

			update(prevCX - 3, prevCY - 3, 7, 7);
		}

		prevCX = cx;
		prevCY = cy;

		int w = rect.getWidth();
		int h = rect.getHeight();

		if ((cy - 1) >= 0)
			for (int x = cx - 3; x <= cx + 3; x++)
				if ((x >= 0) && (x < w) && (x != cx))
					rect.setPixel(x, cy - 1, cursorColor);
		if ((cy + 1) < h)
			for (int x = cx - 3; x <= cx + 3; x++)
				if ((x >= 0) && (x < w) && (x != cx))
					rect.setPixel(x, cy + 1, cursorColor);
		if ((cx - 1) >= 0)
			for (int y = cy - 3; y <= cy + 3; y++)
				if ((y >= 0) && (y < h) && (y != cy))
					rect.setPixel(cx - 1, y, cursorColor);
		if ((cx + 1) < w)
			for (int y = cy - 3; y <= cy + 3; y++)
				if ((y >= 0) && (y < h) && (y != cy))
					rect.setPixel(cx + 1, y, cursorColor);

		update(cx - 3, cy - 3, 7, 7);
	}
}
