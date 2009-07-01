/*
 * Char.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.ai.ocr;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import poker.ai.Block;
import poker.common.Rect;

public class Char
{

	/** charset we belong to */
	private Charset cs;

	/** blocks composing character or ligature */
	private int[] block_idx;

	/** character parts; block plus offset */
	private CharPart[] parts;

	/** text of character or ligature */
	private String text;

	/** global char index */
	public int index;

	private Char()
	{

	}

	/**
	 * Constructor.
	 * 
	 * @param cs
	 * @param text
	 * @param parts2
	 */
	public Char(Charset cs, String text, CharPart[] parts)
	{
		this.cs = cs;
		this.text = text;
		makeCharParts(parts);
	}

	/**
	 * Standardize char-parts so that min part (y first) is at 0, 0.
	 * 
	 * @param parts2
	 */
	private void makeCharParts(CharPart[] csParts)
	{
		parts = csParts;
		fixOrigins();
	}

	/**
	 * @return text
	 */
	public String getText()
	{
		return text;
	}

	public void setText(String text)
	{
		this.text = text;
	}

	/**
	 * Read char from a stream.
	 * 
	 * @param cs
	 *            charset
	 * @param dis
	 *            stream
	 * @return char
	 * @throws IOException
	 */
	public static Char read(Charset cs, DataInputStream dis) throws IOException
	{
		Char c = new Char();
		c.cs = cs;
		c.text = dis.readUTF();

		c.block_idx = new int[dis.readInt()];
		for (int i = 0; i < c.block_idx.length; i++)
			c.block_idx[i] = dis.readInt();
		c.parts = new CharPart[c.block_idx.length];
		for (int i = 0; i < c.block_idx.length; i++)
			c.parts[i] = new CharPart(null, dis.readInt(), dis.readInt());

		return c;
	}

	/**
	 * Write char to output stream.
	 * 
	 * @param dos
	 *            output stream
	 * @throws IOException
	 */
	public void write(DataOutputStream dos) throws IOException
	{
		dos.writeUTF(text);

		dos.writeInt(parts.length);
		for (int i = 0; i < parts.length; i++)
			dos.writeInt(parts[i].block.index);
		for (int i = 0; i < parts.length; i++)
		{
			dos.writeInt(parts[i].relx);
			dos.writeInt(parts[i].rely);
		}

	}

	public int[] matchBase(List<Block> blocks, boolean[] used, int bi,
			CharBlock ocb)
	{
		List<Integer> idxs = new ArrayList<Integer>();
		Block scrb = blocks.get(bi);
		int ocpi = -1;

		CharPart ocp = null;
		for (int i = 0; i < parts.length; i++)
		{
			CharPart cp = parts[i];
			if (cp.block == ocb)
			{
				ocp = cp;
				ocpi = i;
				idxs.add(bi);
				break;
			}
		}

		if (ocp == null)
			return null;

		for (int i = 0; i < this.parts.length; i++)
		{
			if (i == ocpi)
				continue;

			CharPart cp = parts[i];
			int tx = scrb.x + cp.relx - ocp.relx;
			int ty = scrb.y + cp.rely - ocp.rely;

			int j;
			for (j = 0; j < blocks.size(); j++)
			{
				if (used[j] || idxs.contains(j))
					continue;

				// no need for binary size search, since x, y comparison is fast
				Block b = blocks.get(j);
				if (b.x == tx && b.y == ty
						&& Charset.maskCmp(b.mask, cp.block.block.mask))
				{
					idxs.add(j);
					break;
				}
			}

			if (j == blocks.size())
				return null;
		}

		int[] idxa = new int[idxs.size()];
		for (int i = 0; i < idxa.length; i++)
			idxa[i] = idxs.get(i);

		return idxa;
	}

	public void map()
	{
		for (int i = 0; i < parts.length; i++)
			parts[i].block = cs.blocks.get(block_idx[i]);
	}

	public void delete()
	{
		for (CharPart cp : parts)
			cp.block.delete(this);
	}

	public Rect draw()
	{
		return draw(0, 0, (byte) 0xff, (byte) 0x00);
	}

	public Rect draw(byte bg, byte fg)
	{
		return draw(0, 0, bg, fg);
	}

	public Rect draw(int width, int height)
	{
		return draw(width, height, (byte) 0xff, (byte) 0x00);
	}

	/**
	 * Compute rectangle of pixels formed from character and width.
	 */
	public Rect draw(int width, int height, byte bgcolor, byte fgcolor)
	{
		int mx = -1, Mx = -1, my = -1, My = -1;

		for (CharPart p : parts)
		{
			int x = p.relx, y = p.rely;
			if (mx == -1 || x < mx)
				mx = x;
			if (my == -1 || y < my)
				my = y;

			x = x + p.block.block.width - 1;
			y = y + p.block.block.height - 1;
			if (Mx == -1 || x > Mx)
				Mx = x;
			if (My == -1 || y > My)
				My = y;
		}

		int cw = Mx - mx + 1;
		int ch = My - my + 1;
		if (cw > width)
			width = cw;
		if (ch > height)
			height = ch;
		int dx = (width - cw) / 2;
		int dy = (height - ch) / 2;

		Rect rect = new Rect(width, height);
		rect.fill(bgcolor);

		for (CharPart p : parts)
		{
			int tx = p.relx - mx + dx;
			int ty = p.rely - my + dy;
			int w = p.block.block.width;
			int h = p.block.block.height;
			boolean[] mask = p.block.block.mask;
			int base = 0;

			int i = 0;
			for (int y = 0; y < h; y++)
				for (int x = 0; x < w; x++)
					if (mask[i++])
						rect.setPixel(tx + x, ty + y, fgcolor);
		}

		return rect;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o)
	{
		if ((o == null) || !(o instanceof Char))
			return false;
		Char c = (Char) o;

		if (cs != c.cs)
			return false;
		
		if (!text.equals(c.text))
			return false;
		
		if (parts.length != c.parts.length)
			return false;
		
		for (CharPart p : parts)
			if (!c.containsPart(p))
				return false;
		
		return true;
	}

	private boolean containsPart(CharPart op)
	{
		for (CharPart tp : parts)
		{
			if (tp.block == op.block && tp.relx == op.relx
					&& tp.rely == op.rely)
				return true;
		}
		return false;
	}

	public void addToBlocks()
	{
		for (CharPart p : parts)
			p.block.add(this);
	}

	/**
	 * Re-orient char so that 0,0 of its parts are at the min x/y bounds of all
	 * parts. Used for backwards compatibility.
	 */
	public void fixOrigins()
	{
		boolean fx = true, fy = true;
		int mx = 0, my = 0;

		for (CharPart p : parts)
		{
			if (fx || (p.relx < mx))
			{
				mx = p.relx;
				fx = false;
			}
			if (fy || (p.rely < my))
			{
				my = p.rely;
				fy = false;
			}
		}

		for (CharPart p : parts)
		{
			p.relx -= mx;
			p.rely -= my;
		}
	}

	public void printParts()
	{
		System.out.printf("Parts for char '%s':\n", text);
		for (CharPart p : parts)
			System.out.printf("\trel = (%d, %d)\t\tblock = %d x %d\n", p.relx,
					p.rely, p.block.block.width, p.block.block.height);
	}

	public int numParts()
	{
		return parts.length;
	}
}
