/*
 * Block.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.ai;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


/**
 * A block contains a subset of a pixel mask.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class Block
{

	/** pixel mask */
	public boolean[]	mask;

	/** width of pixel rectangle */
	public int			width;

	/** height of pixel rectangle */
	public int			height;
	
	/** x location of block */
	public int x;
	
	/** y location of block */
	public int y;
	
	/** block outline */
	public int[][] outline;

	
	
	public Block(boolean[] mask, int x, int y, int width, int height, int[][] outline)
	{
		this.mask = mask;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.outline = outline;
	}


	private Block()
	{
	}


	public void frame(byte fg, byte bg, byte bd, byte[] r, int c, int w)
	{
		// draws top and bottom borders
		for (int b : new int[] {c, w * (height + 1) + c})
			for (int i = b; i < b + width + 2; i++)
				r[i] = bd;
		// draws left and right borders
		for (int y = 0; y < height + 2; y++)
		{
			r[y * w + c] = bd;
			r[y * w + c + width + 1] = bd;
		}
		// copies pixel data
		for (int y = 0; y < height; y++)
			for (int x = 0; x < width; x++)
				r[(y + 1) * w + c + x + 1] = mask[y * width + x] ? fg : bg;
	}
	
	public void write(DataOutputStream dos) throws IOException
	{
		dos.writeInt(x);
		dos.writeInt(y);
		dos.writeInt(width);
		dos.writeInt(height);
		for (int i = 0; i < mask.length; i++)
			dos.writeBoolean(mask[i]);
		if (outline == null)
			dos.writeInt(0);
		else
		{
			dos.writeInt(outline[0].length);
			for (int i = 0; i < outline[0].length; i++)
			{
				dos.writeInt(outline[0][i]);
				dos.writeInt(outline[1][i]);
			}
		}
	}
	
	public static Block read(DataInputStream dis) throws IOException
	{
		Block b = new Block();
		
		b.x = dis.readInt();
		b.y = dis.readInt();
		b.width = dis.readInt();
		b.height = dis.readInt();
		b.mask = new boolean[b.width * b.height];
		for (int i = 0; i < b.mask.length; i++)
			b.mask[i] = dis.readBoolean();
		b.outline = null;
		
		int ol = dis.readInt();
		if (ol != 0)
		{
			b.outline = new int[2][ol];
			for (int i = 0; i < ol; i++)
			{
				b.outline[0][i] = dis.readInt();
				b.outline[1][i] = dis.readInt();
			}
		}
		
		return b;
	}


	public void printMap()
	{
		int i = 0;
		System.out.print("\n");
		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
				System.out.print(mask[i++] ? '+' : ' ');
			System.out.print("\n");
		}
		System.out.print("\n");
	}


	public Block sub(int x, int y, int w, int h)
	{
		boolean[] b = new boolean[w * h];
		int base = x;
		for (int i = y, k = 0; i < y + h; i++, base += width)
			for (int j = 0; j < w; j++)
				b[k++] = mask[base + j];
		return new Block(b, this.x + x, this.y + y, w, h, outline);
	}
}

