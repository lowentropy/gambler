/*
 * Charset.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.ai.ocr;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import poker.ai.Block;

/**
 * A charset contains the extracted data from a set of characters which will be
 * matched against. This method is more reliable than a general-purpose OCR
 * algorithm because the pixels of characters should be more or less the same
 * each time (except for a changing background color).
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class Charset
{

	/** name of charset */
	private String name;

	/** foreground color of characters */
	private byte color;

	/** tolerance of foreground */
	private int tolerance;

	/** characters and ligatures */
	public List<Char> chars;

	/** all blocks; their data is shared */
	public List<CharBlock> blocks;

	/** name of file from which charset was loaded */
	private String fname;

	/** whether charset has been modified since loading */
	private boolean modified;

	/**
	 * Constructs a new charset with given parameters. It will be saved to
	 * 'charsetname'.cs.
	 * 
	 * @param name
	 *            name of charset
	 * @param fg
	 *            foreground color (default)
	 * @param tol
	 *            tolerance for matching
	 */
	public Charset(String name, byte fg, int tol)
	{
		this.name = name;
		this.fname = new File("charsets").getAbsolutePath() + File.separator
				+ name + ".cs";
		this.color = fg;
		this.tolerance = tol;
		this.chars = new LinkedList<Char>();
		this.blocks = new ArrayList<CharBlock>();
		this.modified = true;
	}

	/**
	 * @return foreground color
	 */
	public byte getForeground()
	{
		return color;
	}

	/**
	 * @return foreground tolerance
	 */
	public int getTolerance()
	{
		return tolerance;
	}

	/**
	 * @param tolerance
	 *            new tolerance
	 */
	public void setTolerance(int tolerance)
	{
		this.tolerance = tolerance;
	}

	/**
	 * Deletes the given characters from the charset.
	 * 
	 * @param charsToDelete
	 *            list of characters to delete
	 */
	public void deleteChars(List<Char> charsToDelete)
	{
		if (!charsToDelete.isEmpty())
			modified = true;
		for (Char c : charsToDelete)
			c.delete();
		chars.removeAll(charsToDelete);
	}

	/**
	 * Delete a caharacter from the charset.
	 * 
	 * @param c
	 *            char to delete
	 */
	public void deleteChar(Char c)
	{
		if (c == null)
			return;
		modified = true;
		c.delete();
		chars.remove(c);
	}

	/**
	 * Load a charset from a file.
	 * 
	 * @param f
	 *            file to load from
	 * @return charset
	 * @throws IOException
	 */
	public static Charset load(File f) throws IOException
	{
		Charset cs = new Charset(null, (byte) 0, 0);
		DataInputStream dis = new DataInputStream(new FileInputStream(f));

		cs.modified = false;
		cs.fname = f.getAbsolutePath();
		cs.name = dis.readUTF();
		cs.color = dis.readByte();
		cs.tolerance = dis.readInt();

		int num_blocks = dis.readInt();
		cs.blocks = new ArrayList<CharBlock>(num_blocks);
		for (int i = 0; i < num_blocks; i++)
			cs.blocks.add(CharBlock.read(cs, dis));

		int num_chars = dis.readInt();
		cs.chars = new LinkedList<Char>();
		for (int i = 0; i < num_chars; i++)
			cs.chars.add(Char.read(cs, dis));

		for (CharBlock cb : cs.blocks)
			cb.map();

		for (Char c : cs.chars)
			c.map();

		return cs;
	}

	/**
	 * @return whether charset has been modified since loading
	 */
	public boolean wasModified()
	{
		return modified;
	}

	/**
	 * Save the charset back to the file it was loaded from.
	 * 
	 * @throws IOException
	 */
	public void save() throws IOException
	{
		File f = new File(fname);
		write(f);
		modified = false;
	}

	/**
	 * Write the charset to the given file.
	 * 
	 * @param f
	 *            file to write to
	 * @throws IOException
	 */
	public void write(File f) throws IOException
	{
		DataOutputStream dos = new DataOutputStream(new FileOutputStream(f));

		dos.writeUTF(name);
		dos.writeByte(color);
		dos.writeInt(tolerance);

		sort();
		reIndex();
		fixCharOrigins();

		dos.writeInt(blocks.size());
		for (CharBlock b : blocks)
			b.write(dos);

		dos.writeInt(chars.size());
		for (Char c : chars)
			c.write(dos);

		dos.close();
	}

	/**
	 * Re-orient chars so that 0,0 of their parts are at the min x/y bounds of
	 * all parts. Used for backwards compatibility.
	 */
	private void fixCharOrigins()
	{
		for (Char c : chars)
			c.fixOrigins();
	}

	/**
	 * Sort the characters alphanumerically.
	 */
	private void sort()
	{
		Char[] ca = chars.toArray(new Char[0]);
		Arrays.sort(ca, new AlphaNumCharComp());
		chars.clear();
		for (Char c : ca)
			chars.add(c);

		CharBlock[] cb = blocks.toArray(new CharBlock[0]);
		Arrays.sort(cb, new BlockSizeComp());
		blocks.clear();
		for (CharBlock b : cb)
			blocks.add(b);
	}

	/**
	 * Set the index field on all chars and blocks.
	 */
	private void reIndex()
	{
		for (int i = 0; i < blocks.size(); i++)
			blocks.get(i).index = i;
		for (int i = 0; i < chars.size(); i++)
			chars.get(i).index = i;
	}

	/**
	 * Use the given encoding to add characters to the charset, matching the
	 * given blocks. The encoding is a sequence of characters whose position
	 * corresponds to the position of the mapped block. To handle
	 * multi-character blocks (ligands), the escape \nX followed by X characters
	 * is used. For disjoint characters, the first block looks like \gXc, where
	 * c is the character and X is a group name. Following blocks of that
	 * character use \rX without a following character. To ignore a block, use
	 * \., and to obtain a backslash character, use \\
	 * 
	 * @param blocks
	 *            blocks which encoding references
	 * @param encoded
	 *            encoded mappings
	 * @return
	 */
	public String addMappings(List<Block> blocks, String encoded)
	{
		boolean[] used = new boolean[blocks.size()];
		String[] chars = new String[blocks.size()];
		String[] grps = new String[blocks.size()];

		String msg = parseEncoding(encoded, used, chars, grps);
		if (msg != null)
			return msg;

		int numChars = 0;
		CharPart[] cps = addBlocks(blocks, used);
		Map<String, List<CharPart>> grpBlocks = new HashMap<String, List<CharPart>>();
		Map<String, String> grpChars = new HashMap<String, String>();

		for (int i = 0; i < chars.length; i++)
		{
			if (grps[i] != null)
			{
				if (chars[i] != null)
					grpChars.put(grps[i], chars[i]);
				if (!grpBlocks.containsKey(grps[i]))
					grpBlocks.put(grps[i], new ArrayList<CharPart>());
				grpBlocks.get(grps[i]).add(cps[i]);
			}
			else if (chars[i] != null)
				if (addChar(chars[i], new CharPart[] { cps[i] }))
					numChars++;
		}
		for (String key : grpChars.keySet())
		{
			String text = grpChars.get(key);
			CharPart[] parts = grpBlocks.get(key).toArray(new CharPart[0]);
			if (addChar(text, parts))
				numChars++;
		}

		if (numChars > 0)
			modified = true;

		return "Added " + numChars + " new chars.";
	}

	/**
	 * Add a char if it does not already exist.
	 * 
	 * @param text
	 *            text of char
	 * @param parts
	 *            blocks which will become parts of char
	 * @return
	 */
	private boolean addChar(String text, CharPart[] parts)
	{
		Char c = new Char(this, text, parts);
		if (chars.contains(c))
			return false;
		chars.add(c);
		c.addToBlocks();
		return true;
	}

	/**
	 * Add new blocks, or match against existing blocks.
	 * 
	 * @param blocks
	 *            new blocks to add
	 * @param used
	 *            map of which blocks are actually needed
	 * @return charblocks in new, sorted block array
	 */
	private CharPart[] addBlocks(List<Block> blocks, boolean[] used)
	{
		CharPart[] csParts = new CharPart[blocks.size()];

		for (int i = 0; i < blocks.size(); i++)
			csParts[i] = used[i] ? findOrAddBlock(blocks.get(i)) : null;

		return csParts;
	}

	/**
	 * Parse the encoding string.
	 * 
	 * @param encoded
	 * @param used
	 * @param chars
	 * @param grps
	 * @return msg if failed, or null
	 */
	private String parseEncoding(String encoded, boolean[] used,
			String[] chars, String[] grps)
	{
		try
		{
			int idx = 0;

			String g = null;

			int i = 0;
			while (i < used.length)
			{
				char c = encoded.charAt(idx++);
				String x = null;
				String r = null;

				if (c == '\\')
				{
					char d = encoded.charAt(idx++);
					if (d == '.')
					{
						i++;
						continue;
					}
					else if (d == '\\')
						x = "\\";
					else if (d == 'n')
					{
						int n = encoded.charAt(idx++) - '0';
						x = encoded.substring(idx, idx + n);
						idx += n;
					}
					else if (d == 'g')
					{
						g = new String(new char[] { encoded.charAt(idx++) });
						continue;
					}
					else if (d == 'r')
						r = new String(new char[] { encoded.charAt(idx++) });
				}
				else
					x = new String(new char[] { c });

				used[i] = true;

				if (x != null)
					chars[i] = x;
				if (g != null)
					grps[i] = g;
				if (r != null)
					grps[i] = r;
				g = null;
				i++;
			}

			if (idx < encoded.length())
				return "Encoding string too long!";

			return null;
		} catch (StringIndexOutOfBoundsException e)
		{
			return "Encoding string too short!";
		}
	}

	/**
	 * Find the complete text which matches the given screen blocks by comparing
	 * to charset characters.
	 * 
	 * @param scrBlocks
	 *            blocks to match
	 * @return text of match
	 */
	public String match(List<Block> scrBlocks)
	{
		String text = "";
		int[] cbIdx = getCharBlockIndices(scrBlocks);
		boolean[] used = new boolean[cbIdx.length];

		for (int i = 0; i < cbIdx.length; i++)
		{
			if (used[i] || (cbIdx[i] == -1))
				continue;
			
			Char c = blocks.get(cbIdx[i]).match(scrBlocks, used, i);
			if (c != null)
				text += c.getText();
		}

		return text;
	}

	/**
	 * For the given blocks, find the indices into the charset's blocks for
	 * blocks that match in size and value. -1 is used as the index of any block
	 * which is not found.
	 * 
	 * @param scrBlocks
	 *            blocks to match against
	 * @return list of indices into charset blocks
	 */
	private int[] getCharBlockIndices(List<Block> scrBlocks)
	{
		int[] idx = new int[2];
		int[] indices = new int[scrBlocks.size()];

		boolean[] used = new boolean[scrBlocks.size()];

		for (int j = 0; j < scrBlocks.size(); j++)
		{
			Block b = scrBlocks.get(j);

			idx[0] = 0;
			idx[1] = blocks.size();
			binarySizeSearch(b, idx);

			indices[j] = -1;

			if (idx[0] == -1)
				continue;

			for (int i = idx[0]; i <= idx[1]; i++)
				if (maskCmp(blocks.get(i).block.mask, b.mask))
				{
					indices[j] = i;
					break;
				}
		}

		return indices;
	}

	/**
	 * Find the given block's mask in a char block. If not found, add it (in
	 * sorted order) and return it.
	 * 
	 * @param b
	 *            block to search for
	 * @return old or new char block
	 */
	private CharPart findOrAddBlock(Block b)
	{
		CharBlock cb = findBlock(b);
		if (cb == null)
			cb = addBlock(b);
		return new CharPart(cb, b.x, b.y);
	}

	/**
	 * Add a new char block in sorted order.
	 * 
	 * @param b
	 * @return
	 */
	private CharBlock addBlock(Block b)
	{
		int i = 0;
		CharBlock cb = new CharBlock(this, b);

		for (; i < blocks.size(); i++)
		{
			if (sizeCmp(b, blocks.get(i).block) < 0)
				break;
		}

		blocks.add(i, cb);
		return cb;
	}

	/**
	 * Find the given charblock which has an equivalent mask.
	 * 
	 * @param block
	 *            block to search for
	 * @return matching char block, or null if not found
	 */
	private CharBlock findBlock(Block b)
	{
		int[] idx = new int[] { 0, blocks.size() - 1 };
		binarySizeSearch(b, idx);

		if (idx[0] == -1)
			return null;

		for (int i = idx[0]; i <= idx[1]; i++)
		{
			if (maskCmp(b.mask, blocks.get(i).block.mask))
				return blocks.get(i);
		}

		return null;
	}

	/**
	 * Search through the charset's blocks for a block of size equal to the
	 * given block. The method is recursive. When it returns, the two-element
	 * idx parameter will contain the starting/inclusive and ending/exclusive
	 * indices of a sequence of equal-sized blocks.
	 * 
	 * @param b
	 *            block whose size to search for
	 * @param idx
	 *            recursive l/r parameter and return parameter
	 */
	private void binarySizeSearch(Block b, int[] idx)
	{
		if (blocks.size() == 0)
		{
			idx[0] = idx[1] = -1;
			return;
		}

		int c, i = (idx[0] + idx[1]) / 2;
		if (i >= blocks.size())
		{
			idx[0] = idx[1] = -1;
			return;
		}

		CharBlock cb = blocks.get(i);

		if ((c = sizeCmp(b, cb.block)) == 0)
		{
			idx[0] = idx[1] = i;
			while (idx[0] > 0 && sizeCmp(blocks.get(idx[0] - 1).block, b) == 0)
				idx[0]--;
			while (idx[1] < (blocks.size() - 1)
					&& sizeCmp(blocks.get(idx[1] + 1).block, b) == 0)
				idx[1]++;
			return;
		}

		if (idx[0] == idx[1])
		{
			idx[0] = idx[1] = -1;
			return;
		}

		if (c < 0)
			idx[1] = i;
		else
			idx[0] = i + 1;

		binarySizeSearch(b, idx);
	}

	/**
	 * Compare masks.
	 * 
	 * @param mask1
	 * @param mask2
	 * @return whether masks are equal
	 */
	public static boolean maskCmp(boolean[] mask1, boolean[] mask2)
	{
		return Arrays.equals(mask1, mask2);
	}

	/**
	 * Compare blocks' sizes, first by width and then by height.
	 * 
	 * @param b1
	 *            first block
	 * @param b2
	 *            second block
	 * @return comparison value; -1 if first block is ordered before second, 1
	 *         if it is ordered after, or 0 if the blocks are of equal size
	 */
	public static int sizeCmp(Block b1, Block b2)
	{
		if (b1.width < b2.width)
			return -1;
		else if (b1.width > b2.width)
			return 1;
		else if (b1.height < b2.height)
			return -1;
		else if (b1.height > b2.height)
			return 1;
		else
			return 0;
	}

	/**
	 * @return name of charset
	 */
	public String getName()
	{
		return this.name;
	}

	/**
	 * Delete the file from which the charset was loaded.
	 */
	public void delete()
	{
		new File(fname).delete();
	}

	public static Charset load(String csName, String csRoot) throws IOException
	{
		return load(new File(csRoot + File.separator + csName + ".cs"));
	}

	public static Charset load(String csName) throws IOException
	{
		return load(csName, "charsets");
	}

}
