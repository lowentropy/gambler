/*
 * CharBlock.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.ai.ocr;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import poker.ai.Block;


public class CharBlock
{

	/** charset we belong to */
	private Charset		cs;

	/** chars containing a copy of this block */
	public List<Char>	chars;

	/** index of global chars */
	public int[]		char_idx;

	/** global char-block index */
	public int			index;

	/** block to match */
	public Block		block;


	private CharBlock()
	{

	}


	/**
	 * Constructor.
	 * 
	 * @param cs
	 * @param b
	 */
	public CharBlock(Charset cs, Block b)
	{
		this.cs = cs;
		this.block = b;
		this.chars = new LinkedList<Char>();
	}


	public static CharBlock read(Charset cs, DataInputStream dis)
			throws IOException
	{
		CharBlock b = new CharBlock();
		b.cs = cs;
		b.block = Block.read(dis);

		b.char_idx = new int[dis.readInt()];
		for (int i = 0; i < b.char_idx.length; i++)
			b.char_idx[i] = dis.readInt();

		return b;
	}


	public void write(DataOutputStream dos) throws IOException
	{
		block.write(dos);
		dos.writeInt(chars.size());
		for (Char c : chars)
			dos.writeInt(c.index);
	}


	public Char match(List<Block> blocks, boolean[] used, int i)
	{
		Block matched = blocks.get(i);
		used[i] = true;
		int[] mu = new int[0];
		Char mc = null;

		for (Char c : chars)
		{
			int[] u = c.matchBase(blocks, used, i, this);
			if (u == null)
				continue;
			if (u.length > mu.length)
			{
				mu = u;
				mc = c;
			}
		}

		for (int ui : mu)
			used[ui] = true;

		return mc;
	}


	public void map()
	{
		chars = new LinkedList<Char>();
		for (int idx : char_idx)
			chars.add(cs.chars.get(idx));
	}


	public void delete(Char c)
	{
		chars.remove(c);
	}


	public void add(Char c)
	{
		chars.add(c);
	}

}
