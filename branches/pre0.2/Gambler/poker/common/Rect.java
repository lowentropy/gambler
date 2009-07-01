/*
 * Rect.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import poker.ai.Block;
import poker.ai.ocr.GraphicsAI;


/**
 * Rectangle of 8-bit pixels.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class Rect implements Serializable
{

	public static final int	HORIZONTAL	= 1;

	public static final int	VERTICAL	= 2;

	private static GraphicsAI ai = new GraphicsAI();
	
	/** pixel data; this rectangle may only be a window of the array */
	private transient byte[]			pixels;

	/** scan width of array */
	private transient int				scanwidth;

	/** offset into first pixel of data array */
	private transient int				offset;

	/** width of rectangle */
	private int				width;

	/** height of rectangle */
	private int				height;


	public Rect(int width, int height)
	{
		this(new byte[width * height], width, height);
	}


	public Rect(int width, int height, DataInputStream stream)
			throws IOException
	{
		this(width, height);
		this.readFrom(stream);
	}


	public Rect(byte[] pixels, int width, int height)
	{
		this.pixels = pixels;
		this.scanwidth = width;
		this.width = width;
		this.height = height;
		this.offset = 0;
	}


	private Rect(byte[] pixels, int scanwidth, int offset, int width, int height)
	{
		this.pixels = pixels;
		this.scanwidth = scanwidth;
		this.offset = offset;
		this.width = width;
		this.height = height;
	}


	public boolean mapOnto(Rect r, int tx, int ty, boolean trunc)
	{
		int newWidth = width;
		int newHeight = height;
		int newOffset;
		int newScanwidth;

		if (tx < 0)
		{
			if (!trunc)
				return false;
			newWidth += tx;
			tx = 0;
		}

		if (ty < 0)
		{
			if (!trunc)
				return false;
			newHeight += ty;
			ty = 0;
		}

		if ((tx >= r.width) || (ty >= r.height))
			return false;

		if ((tx + width) > r.width)
		{
			if (!trunc)
				return false;
			newWidth = r.width - tx;
		}

		if ((ty + height) > r.height)
		{
			if (!trunc)
				return false;
			newHeight = r.height - ty;
		}

		newScanwidth = r.scanwidth;
		newOffset = (ty * r.scanwidth) + tx + r.offset;

		if (pixels != null)
		{
			int base1 = offset;
			int base2 = newOffset;
			for (int i = 0; i < newHeight; i++)
			{
				System.arraycopy(pixels, base1, r.pixels, base2, newWidth);
				base1 += scanwidth;
				base2 += newScanwidth;
			}
		}

		pixels = r.pixels;
		scanwidth = newScanwidth;
		offset = newOffset;
		width = newWidth;
		height = newHeight;

		return true;
	}


	public boolean drawOnto(Rect r, int tx, int ty, boolean trunc)
	{
		int newWidth = width;
		int newHeight = height;
		int newOffset;
		int newScanwidth;

		if (tx < 0 || ty < 0)
			return false;

		if ((tx >= r.width) || (ty >= r.height))
			return false;

		if ((tx + width) > r.width)
		{
			if (!trunc)
				return false;
			newWidth = r.width - tx;
		}

		if ((ty + height) > r.height)
		{
			if (!trunc)
				return false;
			newHeight = r.height - ty;
		}

		if (pixels == null)
			return true;

		newScanwidth = r.scanwidth;
		newOffset = (ty * r.scanwidth) + tx;

		int base1 = offset;
		int base2 = newOffset;
		for (int i = 0; i < newHeight; i++)
		{
			System.arraycopy(pixels, base1, r.pixels, base2, newWidth);
			base1 += scanwidth;
			base2 += newScanwidth;
		}

		return true;
	}


	public Rect copyOnto(Rect r, int tx, int ty, boolean trunc)
	{
		int newWidth = width;
		int newHeight = height;
		int newOffset;
		int newScanwidth;

		if (tx < 0 || ty < 0)
			return null;

		if ((tx >= r.width) || (ty >= r.height))
			return null;

		if ((tx + width) > r.width)
		{
			if (!trunc)
				return null;
			newWidth = r.width - tx;
		}

		if ((ty + height) > r.height)
		{
			if (!trunc)
				return null;
			newHeight = r.height - ty;
		}

		newScanwidth = r.scanwidth;
		newOffset = (ty * r.scanwidth) + tx;

		if (pixels != null)
		{
			int base1 = offset;
			int base2 = newOffset;
			for (int i = 0; i < newHeight; i++)
			{
				System.arraycopy(pixels, base1, r.pixels, base2, newWidth);
				base1 += scanwidth;
				base2 += newScanwidth;
			}
		}

		return new Rect(r.pixels, newScanwidth, newOffset, newWidth, newHeight);
	}


	public void readFrom(DataInputStream stream) throws IOException
	{
		int base = offset;
		for (int i = 0; i < height; i++)
		{
			stream.readFully(pixels, base, width);
			base += scanwidth;
		}
	}


	public Rect sub(int x, int y, int w, int h, boolean trunc)
	{
		Rect tmp = new Rect(null, w, h);

		if (!tmp.mapOnto(this, x, y, trunc))
			System.err.printf(
					"could not map %d x %d on a %d x %d at (%d, %d)\n", w, h,
					width, height, x, y); // DBG

		return tmp;
	}


	public void fill(byte color)
	{
		fill(0, 0, width, height, color);
	}


	public void fill(int x, int y, int w, int h, byte color)
	{
		int base = offset + x;
		for (int i = y; i < y + h; i++)
		{
			Arrays.fill(pixels, base, base + w, color);
			base += scanwidth;
		}

	}


	public boolean copyInto(Rect r)
	{
		if (r.width != width || r.height != height)
			return false;

		int base1, base2, gap1, gap2;

		if ((r.pixels == pixels) && (r.offset > offset))
		{ // copy from bottom up
			base1 = (height - 1) * scanwidth + offset;
			base2 = (r.height - 1) * r.scanwidth + r.offset;
			gap1 = -scanwidth;
			gap2 = -r.scanwidth;
		}
		else
		{ // copy from top down
			base1 = offset;
			base2 = r.offset;
			gap1 = scanwidth;
			gap2 = r.scanwidth;
		}
		
		if (pixels == null)
			System.out.printf("THIS.copyInto's pixels are NULL!\n");
		if (r.pixels == null)
			System.out.printf("RECT.copyInto's pixels are NULL!\n");

		for (int i = 0; i < height; i++)
		{
			System.arraycopy(pixels, base1, r.pixels, base2, width);
			base1 += gap1;
			base2 += gap2;
		}

		return true;
	}


	public byte[] getPixels()
	{
		return pixels;
	}


	public byte getPixel(int x, int y)
	{
		return pixels[y * scanwidth + x + offset];
	}


	/**
	 * @see java.lang.Object#clone()
	 */
	public Rect clone()
	{
		Rect r = new Rect(width, height);
		this.copyInto(r);
		return r;
	}


	public void setPixel(int x, int y, byte c)
	{
		int i = y * scanwidth + x + offset;
		if ((i < 0) || (i >= pixels.length))
			return;
		pixels[i] = c;
	}


	public int getWidth()
	{
		return width;
	}


	public int getHeight()
	{
		return height;
	}


	public static Rect fromMask(boolean[] mask, int width, int height, byte fg,
			byte bg)
	{
		byte[] pixels = new byte[width * height];
		for (int i = 0; i < mask.length; i++)
			pixels[i] = mask[i] ? fg : bg;
		return new Rect(pixels, width, height);
	}


	public void drawPixels(int[][] pixels, int dx, int dy, byte c)
	{
		for (int i = 0; i < pixels[0].length; i++)
		{
			int x = pixels[0][i] + dx;
			int y = pixels[1][i] + dy;
			int j = y * scanwidth + x + offset;
			if (j < 0 || j >= this.pixels.length)
				continue;
			this.pixels[j] = c;
		}
	}


	public boolean drawBox(int x1, int y1, int x2, int y2, byte c, boolean trunc)
	{
		if ((x1 > x2) || (y1 > y2))
			return false;

		if (!trunc)
		{
			if ((x1 < 0) || (y1 < 0) || (x2 < 0) || (y2 < 0))
				return false;
			if ((x1 >= width) || (y1 >= height) || (x2 >= width)
					|| (y2 >= height))
				return false;
		}

		int i;

		for (int x = x1; x <= x2; x++)
		{
			i = y1 * scanwidth + offset + x;
			if ((i >= 0) && (i < pixels.length))
				pixels[i] = c;
			i = y2 * scanwidth + offset + x;
			if ((i >= 0) && (i < pixels.length))
				pixels[i] = c;
		}

		for (int y = y1; y <= y2; y++)
		{
			i = y * scanwidth + offset + x1;
			if ((i >= 0) && (i < pixels.length))
				pixels[i] = c;
			i = y * scanwidth + offset + x2;
			if ((i >= 0) && (i < pixels.length))
				pixels[i] = c;
		}

		return true;
	}


	public boolean frameOnto(Rect rect, int x, int y, byte c, boolean trunc)
	{
		if ((x < 0) || (y < 0))
			return false;

		int w = width + 2;
		int h = height + 2;
		int x0 = x - 1;
		int y0 = y - 1;

		if (x0 < 0)
		{
			if (!trunc)
				return false;
			x0 = 0;
		}

		if (y0 < 0)
		{
			if (!trunc)
				return false;
			y0 = 0;
		}

		if ((x + width) > rect.width)
		{
			if (!trunc)
				return false;
			w = rect.width - x - 1;
		}

		if ((y + height) > rect.height)
		{
			if (!trunc)
				return false;
			h = rect.height - y - 1;
		}

		if (y > 0)
			for (int x_ = x0; x_ < (x + w - 1); x_++)
				rect.pixels[(y - 1) * rect.scanwidth + rect.offset + x_] = c;

		if ((y + height + 1) <= rect.height)
			for (int x_ = x0; x_ < (x + w - 1); x_++)
				rect.pixels[(y + height) * rect.scanwidth + rect.offset + x_] = c;

		if (x > 0)
			for (int y_ = y0; y_ < (y + h - 1); y_++)
				rect.pixels[y_ * rect.scanwidth + rect.offset + x - 1] = c;

		if ((x + width + 1) <= rect.width)
			for (int y_ = y0; y_ < (y + h - 1); y_++)
				rect.pixels[y_ * rect.scanwidth + rect.offset + x + width] = c;

		return true;
	}


	public static Rect mask(boolean[] mask, int width, int height, byte fg,
			byte bg)
	{
		Rect r = new Rect(width, height);
		for (int i = 0; i < mask.length; i++)
			r.pixels[i] = mask[i] ? fg : bg;
		return r;
	}


	public boolean increaseSize(int xl, int xr, int yt, int yb, boolean trunc)
	{
		int w = scanwidth;
		int h = pixels.length / scanwidth;
		int x0 = offset % w;
		int y0 = offset / w;
		int x1 = x0 + width - 1;
		int y1 = y0 + height - 1;

		if ((x0 - xl) < 0)
		{
			if (!trunc)
				return false;
			xl = x0;
		}
		if ((y0 - yt) < 0)
		{
			if (!trunc)
				return false;
			yt = y0;
		}
		if ((x1 + xr) >= w)
		{
			if (!trunc)
				return false;
			xr = w - x1 - 1;
		}
		if ((y1 + yb) >= h)
		{
			if (!trunc)
				return false;
			yb = h - y1 - 1;
		}

		width += (xl + xr);
		height += (yt + yb);

		offset -= yt * scanwidth;
		offset -= xl;

		return true;
	}


	public void write(String fname) throws IOException
	{
		FileOutputStream fos = new FileOutputStream(fname);
		DataOutputStream dos = new DataOutputStream(fos);
		dos.writeInt(width);
		dos.writeInt(height);
		int base = offset;
		for (int i = 0; i < height; i++)
		{
			dos.write(pixels, base, width);
			base += scanwidth;
		}
		dos.close();
	}


	public static Rect combine(List<Rect> rects, int direction, int gap,
			byte gapColor, byte bgColor)
	{
		if (rects.size() == 0)
			return null;

		int width = 0, height = 0;

		for (Rect r : rects)
		{
			if (direction == HORIZONTAL)
			{
				width += r.width + gap;
				if (r.height > height)
					height = r.height;
			}
			else
			{
				height += r.height + gap;
				if (r.width > width)
					width = r.width;
			}
		}

		if (direction == HORIZONTAL)
			width -= gap;
		else
			height -= gap;

		Rect c = new Rect(width, height);
		c.fill(bgColor);
		int x = 0, y = 0;

		for (Rect r : rects)
		{
			if (r.copyOnto(c, x, y, false) == null)
				System.err.printf(
						"could not copy %d x %d onto %d x %d at %d, %d\n",
						r.width, r.height, width, height, x, y);
			if (direction == HORIZONTAL)
			{
				x += r.width;
				if (x < width)
					c.fill(x, y, gap, height, gapColor);
				x += gap;
			}
			else
			{
				y += r.height;
				if (y < height)
					c.fill(x, y, width, gap, gapColor);
				y += gap;
			}
		}

		return c;
	}


	public void printMap(byte b, int i)
	{
		GraphicsAI ai = new GraphicsAI();
		boolean[] map = ai.createMask(this, b, i);
		new Block(map, 0, 0, width, height, null).printMap();
	}


	public void drawX(int x, int y, int xw, byte b)
	{
		int c, r, n = (xw - 1) / 2;
		for (int i = 0; i < xw; i++)
		{
			c = x + i - n;
			r = y + i - n;
			pixels[(r * scanwidth) + offset + c] = b;
			c = x + (xw - i) - n - 1;
			pixels[(r * scanwidth) + offset + c] = b;
		}
	}


	public int[] getBounds()
	{
		return new int[] {offset % scanwidth, offset / scanwidth, width, height};
	}


	public Rect sub(int[] b, boolean trunc)
	{
		return this.sub(b[0], b[1], b[2], b[3], trunc);
	}


	public void shift(int x, int y)
	{
		offset = offset + x - (y * scanwidth);
	}


	public String toHex(int width)
	{
		StringBuilder sb = new StringBuilder();
		for (int y = 0, i = offset; y < height; y++, i += (scanwidth - this.width))
			for (int x = 0; x < this.width; x++, i++)
			{
				byte b = pixels[i];
				String s = Integer.toHexString(0xff & b);
				if (s.length() == 1)
					sb.append('0');
				sb.append(s);
			}

		for (int i = 0; i <= sb.length(); i += width)
			sb.insert(i++, '\n');

		return sb.toString();
	}


	public boolean matches(Rect rect)
	{
		if (width != rect.width)
			return false;
		if (height != rect.height)
			return false;
		for (int i = 0, a = offset, b = rect.offset; i < height; i++, a += scanwidth, b += rect.scanwidth)
			for (int j = 0, c = a, d = b; j < width; j++, c++, d++)
				if (pixels[c] != rect.pixels[d])
					return false;
		return true;

	}
	


	public boolean matches(Rect rect, float p)
	{
		if (width != rect.width)
			return false;
		if (height != rect.height)
			return false;
		int n = 0;
		for (int i = 0, a = offset, b = rect.offset; i < height; i++, a += scanwidth, b += rect.scanwidth)
			for (int j = 0, c = a, d = b; j < width; j++, c++, d++)
				if (pixels[c] != rect.pixels[d])
					n++;
		return ((float) n / (float) (width * height)) <= (1 - p);
	}
	
	
	public boolean matchesWithinTol(Rect rect, int tol, float p)
	{
		if (width != rect.width)
			return false;
		if (height != rect.height)
			return false;
		int n = 0;
		for (int i = 0, a = offset, b = rect.offset; i < height; i++, a += scanwidth, b += rect.scanwidth)
			for (int j = 0, c = a, d = b; j < width; j++, c++, d++)
				if (!ai.withinTol(pixels[c], rect.pixels[d], tol))
					n++;
		return ((float) n / (float) (width * height)) <= (1 - p);
	}


	public static Rect load(File file) throws IOException
	{
		DataInputStream dis = new DataInputStream(new FileInputStream(file));
		int width = dis.readInt();
		int height = dis.readInt();
		byte[] pixels = new byte[width * height];
		dis.readFully(pixels);
		dis.close();
		return new Rect(pixels, width, height);
	}


	public void save(File file) throws IOException
	{
		DataOutputStream dos = new DataOutputStream(new FileOutputStream(file));
		dos.writeInt(width);
		dos.writeInt(height);
		int base = offset;
		for (int i = 0; i < height; i++, base += scanwidth)
			dos.write(pixels, base, scanwidth);
		dos.close();
	}


	public String printHex(int w)
	{
		return "(" + width + "x"+height+")" + toHex(w);
	}
	
	public void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException
	{
		stream.defaultReadObject();
		
		scanwidth = width;
		pixels = new byte[width * height];
		stream.readFully(pixels);
	}
	
	public void writeObject(ObjectOutputStream stream) throws IOException
	{
		stream.defaultWriteObject();
		stream.write(pixels);
	}

}
