/*
 * GraphicsAI.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.ai.ocr;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import poker.ai.AIError;
import poker.ai.Block;
import poker.common.Rect;


/**
 * The GraphicsAI class contains code which is able to recognize any
 * computer-displayed text in a rectangle of pixels.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class GraphicsAI
{

	/** maximum number of pixels in a block outline */
	private static final int		MAX_OUTLINE	= 1000;

	/** font char-sets to match against input characters */
	private Map<String, Charset>	fonts;


	/**
	 * Constructor.
	 */
	public GraphicsAI()
	{
		fonts = new Hashtable<String, Charset>();
	}


	public String decodeText(Rect rect, Charset cs, byte[] colors)
	{
		return decodeText(rect, 0, 0, cs, colors);
	}


	public String decodeText(Rect rect, Charset charset, byte[] bs, int tol)
	{
		return fullDecode(rect, 0, 0, charset, bs, tol, null).text;
	}


	public String decodeText(Rect rect, Charset cs)
	{
		return decodeText(rect, 0, 0, cs, null);
	}


	public String decodeText(Rect rect, int px, int py, Charset cs)
	{
		return decodeText(rect, px, py, cs, null);
	}


	public DecodeResult fullDecode(Rect rect, int px, int py, Charset cs,
			byte[] colors, int tol, String allowable)
	{
		String text = "";
		if (colors == null)
			colors = new byte[] {cs.getForeground()};
		int ci = 0;

		for (int i = 0; i < colors.length; i++)
		{
			byte fg = colors[i];

			List<Block> blocks = null;
			String mtext = null;

			try
			{
				blocks = getBlocks(rect, px, py, fg, tol, false);
				mtext = cs.match(blocks);
			}
			catch (Exception e)
			{
				try
				{
					rect.write("/home/lowentropy/src/screendump/block.img");
				}
				catch (Exception e1)
				{
				}
			}

			if (mtext == null)
				return DecodeResult.invalid();

			if (mtext.length() > text.length())
			{
				text = mtext;
				ci = i;
			}
		}

		return new DecodeResult(text, colors[ci], ci);
	}


	/**
	 * Turn some pixels into text.
	 * 
	 * @param rect
	 *            pixel rectangle
	 * @param px
	 *            logical x coord of rectangle
	 * @param py
	 *            logical y coord of rectangle
	 * @param cs
	 *            charset to use for decoding
	 * @param colors
	 *            foreground colors to try, or null if default
	 * @return best match for decoded text
	 */
	public String decodeText(Rect rect, int px, int py, Charset cs,
			byte[] colors)
	{
		return fullDecode(rect, px, py, cs, colors, cs.getTolerance(), null).text;
	}


	/**
	 * Get blocks of foreground pixels from a rectangle.
	 * 
	 * @param pixels
	 *            pixel data
	 * @param width
	 *            width of rectangle
	 * @param height
	 *            height of rectangle
	 * @param fgcolor
	 *            foreground color
	 * @param fgtol
	 *            foreground color tolerance
	 * @return list of pixel blocks
	 * @throws AIError
	 */
	public List<Block> getBlocks(Rect rect, int px, int py, byte fgcolor,
			int fgtol, boolean leaveFilled) throws AIError
	{
		boolean[] mask = createMask(rect, fgcolor, fgtol);
		// System.out.printf(
		// "DBG: getBlocks(): p=(%d,%d), sz=(%dx%d), fg=%d tol %d\n", px,
		// py, rect.getWidth(), rect.getHeight(), fgcolor, fgtol);
		return getBlocks(mask, px, py, rect.getWidth(), rect.getHeight(),
				leaveFilled);
	}


	/**
	 * Get a list of blocks of masked pixel data from a pixel mask (BW or
	 * fg/bg).
	 * 
	 * @param mask
	 *            pixel mask (fg/bg)
	 * @param w
	 *            width of rectangle
	 * @param h
	 *            height of rectangle
	 * @return list of blocks
	 * @throws AIError
	 */
	private List<Block> getBlocks(boolean[] mask, int px, int py, int w, int h,
			boolean leaveFilled) throws AIError
	{
		boolean[] m = (boolean[]) mask.clone();
		List<Block> blocks = new ArrayList<Block>();

		// DBG
		// printMask(mask, w, h);

		for (int x = 0; x < w; x++)
		{
			for (int y = 0; y < h; y++)
			{
				int i = y * w + x;
				if (m[i])
				{
					Block b = extractBlock(m, px, py, x, y, w, h, leaveFilled);
					if (b != null)
						blocks.add(b);
				}
			}
		}

		return blocks;
	}


	/**
	 * Mask out the portions of the rectangle which are not outlined by a shape
	 * of the given color. This is useful, for instance, for characters which
	 * are outlined in a second color, like white-within-black. First call this
	 * method with black as the outline color, then perform the normal
	 * decodeText with white. The outline mask replaces masked-out regions with
	 * the outline color, the reasoning being that the outline color is already
	 * sufficiently different from whatever fills it.
	 * 
	 * @param rect
	 *            rectangle to mask
	 * @param c
	 *            outline color
	 * @return new rectangle
	 * @throws AIError
	 */
	public Rect outlineMask(Rect rect, byte c) throws AIError
	{
		List<Block> blocks = getBlocks(rect, 0, 0, c, 0, true);
		Block mask = combine(blocks, rect.getWidth(), rect.getHeight());
		this.printMask(mask.mask, mask.width, mask.height); // DBG
		return mask(rect, mask, c);
	}


	/**
	 * Mask a rectangle. All masked-out portions are replaced by the given
	 * color.
	 * 
	 * @param rect
	 * @param mask
	 * @param c
	 * @return masked rectangle
	 */
	private Rect mask(Rect rect, Block mask, byte c)
	{
		int w = rect.getWidth();
		int h = rect.getHeight();
		byte[] o = rect.getPixels();
		byte[] p = new byte[w * h];
		Rect n = new Rect(w, h);
		for (int i = 0; i < p.length; i++)
			p[i] = mask.mask[i] ? o[i] : c;
		return new Rect(p, w, h);
	}


	/**
	 * Combine several blocks into one with an OR operation.
	 * 
	 * @param blocks
	 * @param width
	 * @param height
	 * @return
	 */
	private Block combine(List<Block> blocks, int width, int height)
	{
		boolean[] m = new boolean[width * height];
		for (Block b : blocks)
		{
			System.out.printf("DBG: combine: in %dx%d, a %dx%d at (%d,%d)\n",
					width, height, b.width, b.height, b.x, b.y);
			for (int y = 0, o = b.x + b.y * width, j = 0; y < b.height; y++, o += width)
				for (int x = 0, i = o; x < b.width; x++, i++, j++)
					m[i] = m[i] | b.mask[j];
		}
		return new Block(m, 0, 0, width, height, null);
	}


	/**
	 * Print a boolean mask as +'s or spaces.
	 * 
	 * @param mask
	 * @param w
	 * @param h
	 */
	public static void printMask(boolean[] mask, int w, int h)
	{
		for (int i = 0; i < w + 2; i++)
			System.out.print('-');
		System.out.println();

		for (int y = 0, i = 0; y < h; y++)
		{
			System.out.print('|');
			for (int x = 0; x < w; x++, i++)
				System.out.print(mask[i] ? '+' : ' ');
			System.out.println('|');
		}
		for (int i = 0; i < w + 2; i++)
			System.out.print('-');
		System.out.println();
	}


	/**
	 * Extract a single block from a pixel mask which has a foreground pixel at
	 * the given x,y position. This uses the outline algorithm to pull these
	 * foreground colors out of the mask.
	 * 
	 * @param b
	 *            pixel mask rectangle
	 * @param x
	 *            x coordinate of fg pixel in block
	 * @param y
	 *            y coordinate of fg pixel in block
	 * @param w
	 *            width of pixel rectangle
	 * @param h
	 *            height of pixel rectangle
	 * @return extracted block
	 * @throws AIError
	 */
	private Block extractBlock(boolean[] b, int px, int py, int x, int y,
			int w, int h, boolean leaveFilled) throws AIError
	{
		int[][][] o = drawOutline(b, x, y, w, h);
		int my = -5, My = -5;
		int mx = -5, Mx = -5;

		if (o == null)
			return null;

		// get min/max points
		for (int i = 0; i < o[0][0].length; i++)
		{
			int x_ = o[0][0][i];
			int y_ = o[0][1][i];

			if ((mx == -5) || (x_ < mx))
				mx = x_;
			if ((Mx == -5) || (x_ > Mx))
				Mx = x_;

			if ((my == -5) || (y_ < my))
				my = y_;
			if ((My == -5) || (y_ > My))
				My = y_;
		}

		int b2w = Mx - mx - 1;
		int b2h = My - my - 1;
		boolean[] b2 = new boolean[b2w * b2h];

		int[][] oo = o[0];
		for (int i = 0; i < oo[0].length; i++)
		{
			oo[0][i] -= (mx + 1);
			oo[1][i] -= (my + 1);
		}

		int[][] io = o[1];
		for (int i = 0; i < io[0].length; i++)
		{
			io[0][i] -= (mx + 1);
			io[1][i] -= (my + 1);
		}

		// DBG: print outlines
		/*System.out.printf("\n");
		for (int i = 0; i < oo[0].length; i++)
			System.out.printf("(%d,%d),", oo[0][i], oo[1][i]);
		System.out.printf("\n");
		for (int i = 0; i < io[0].length; i++)
			System.out.printf("(%d,%d),", io[0][i], io[1][i]);
		System.out.printf("\n");

		System.out.printf("min=(%d,%d) max=(%d,%d) p=(%d,%d) s=(%d,%d)\n", mx, my, Mx, My, px, py, w, h);
*/
		outlineFill(io, b2, b2w, b2h);
		extract(b, w, h, b2, b2w, b2h, mx, my, leaveFilled);
		return new Block(b2, mx + px + 1, my + py + 1, b2w, b2h, oo);
	}


	private void outlineFill(int[][] o, boolean[] b, int w, int h)
			throws AIError
	{
		try
		{
			int[][] tmp = new int[w][h];

			// find a group of y = y0 + 1
			int i0 = 0, i = 0;
			while (o[1][i] != 0)
				i0 = ++i;

			// find head of that group
			do
				i = norm(i - 1, o[0].length);
			while ((o[1][i] == 0) && (i != i0));

			// outline is single row
			if (i == i0)
			{
				for (i = 0; i < o[0].length; i++)
					b[(o[1][i] * w) + o[0][i]] = true;
				return;
			}

			// set up first row
			i = i0 = norm(i + 1, o[0].length);
			int gx0 = o[0][i0], gx1 = gx0;
			int ry = o[1][i];
			int oi = 0, y, dy = -1;
			boolean first = true;

			// DBG
			// if (gx1 < 0)
			// {
			// System.err.printf("i=%d, i0=%d, gx0=%d, len=%d, ry=%d\n", i,
			// i0, gx0, o[0].length, ry);
			// }

			// while processing new row
			while (first || (i != i0))
			{
				first = false;

				// find the new row start and mark end of this row
				while ((y = o[1][i]) == ry)
				{
					int x = o[0][i];
					if (x < gx0)
						gx0 = x;
					if (x > gx1)
						gx1 = x;
					i = norm((oi = i) + 1, o[0].length);
				}

				// find last row start/stop
				// gx1 = o[0][oi];
				// int mx0 = (gx0 < gx1) ? gx0 : gx1;
				// int mx1 = (gx0 > gx1) ? gx0 : gx1;

				// make correct mark from mx0 to mx1 on ry row
				int m = ((y - ry) != dy) ? 1 : 2;
				for (int mx = gx0; mx < gx1; mx++)
				{
					int om = tmp[mx][ry];
					tmp[mx][ry] = (om == 1) ? 1 : (om + 1 > 1) ? 2 : 1;
				}

				// DBG
				// int om = tmp[gx1][ry];
				int[] om_t = tmp[gx1];
				int om = om_t[ry];

				tmp[gx1][ry] = (om == m) ? 1 : (om + m > 1) ? 2 : 1;

				// set up new row
				dy = y - ry;
				gx0 = gx1 = o[0][i];
				ry = y;
			}

			// scan mark array and fill mask
			int base = 0;
			for (y = 0; y < h; y++)
			{
				boolean m = false;
				for (int x = 0; x < w; x++)
				{
					int c = tmp[x][y];
					if (m || (c > 0))
						b[base + x] = true;
					if (c > 1)
						m = !m;
				}
				base += w;
			}
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			throw new AIError("array index out of bounds", e);
		}
	}


	private int norm(int x, int n)
	{
		return (x + n) % n;
	}


	/**
	 * Extract a filled portion from the rectangle and into a block; mask it out
	 * on the original, and either take a copy of the original or the filled
	 * outline.
	 * 
	 * @param r
	 * @param rw
	 * @param rh
	 * @param b
	 * @param bw
	 * @param bh
	 * @param mx
	 * @param my
	 * @param leaveFilled
	 */
	private void extract(boolean[] r, int rw, int rh, boolean[] b, int bw,
			int bh, int mx, int my, boolean leaveFilled)
	{
		for (int y = my + 1; y <= my + bh; y++)
		{
			for (int x = mx + 1; x <= mx + bw; x++)
			{
				int bi = (y - my - 1) * bw + (x - mx - 1);
				int ri = y * rw + x;
				if (b[bi])
				{
					if (!leaveFilled)
						b[bi] = r[ri];
					r[ri] = false;
				}
			}
		}
	}


	private void floodFill(boolean[] r, int rw, int rh, int[][] o, int fx,
			int fy, boolean[] b, int bw, int bh, int mx, int my)
	{
		if ((fx <= mx) || (fy <= my) || (fx >= mx + bw + 1)
				|| (fy >= my + bh + 1))
			return;

		int bi = (fy - my - 1) * bw + (fx - mx - 1);

		if (!b[bi] && !inOutline(o, fx, fy))
		{
			b[bi] = true;
			floodFill(r, rw, rh, o, fx + 1, fy, b, bw, bh, mx, my);
			floodFill(r, rw, rh, o, fx, fy + 1, b, bw, bh, mx, my);
			floodFill(r, rw, rh, o, fx - 1, fy, b, bw, bh, mx, my);
			floodFill(r, rw, rh, o, fx, fy - 1, b, bw, bh, mx, my);
			floodFill(r, rw, rh, o, fx + 1, fy + 1, b, bw, bh, mx, my);
			floodFill(r, rw, rh, o, fx + 1, fy - 1, b, bw, bh, mx, my);
			floodFill(r, rw, rh, o, fx - 1, fy + 1, b, bw, bh, mx, my);
			floodFill(r, rw, rh, o, fx - 1, fy - 1, b, bw, bh, mx, my);
		}
	}


	private boolean inOutline(int[][] o, int x, int y)
	{
		for (int i = 0; i < o[0].length; i++)
			if (o[0][i] == x && o[1][i] == y)
				return true;
		return false;
	}


	/**
	 * Draw an outline around some foreground pixels starting at a given x,y
	 * location.
	 * 
	 * @param m
	 *            pixel mask rectangle
	 * @param x
	 *            x coordinate of fg pixel
	 * @param y
	 *            y coordinate of fg pixel
	 * @param w
	 *            width of rectangle
	 * @param h
	 *            height of rectangle
	 * @return list of indices into pixel mask in clockwise order around the
	 *         shape
	 */
	private int[][][] drawOutline(boolean[] m, int x, int y, int w, int h)
	{
		int[][] oo = new int[2][MAX_OUTLINE]; // outter outline
		int[][] io = new int[2][MAX_OUTLINE]; // inner outline
		int noo = 1; // num outline points
		int nio = 1; // num outline points
		int fx = x, fy = y; // first outline point
		int ox = 0, oy = -1; // next whitespace delta
		int nx = cirx(ox, oy), ny = ciry(ox, oy); // next outline delta
		int tmp, nc; // number of circle pts tried
		oo[0][0] = x; // set first outline pt
		oo[1][0] = y - 1;
		io[0][0] = x;
		io[1][0] = y;

		while (true)
		{
			nc = 0;
			while (!isfg(m, w, h, x + nx, y + ny))
			{
				if ((x == fx) && (y == fy) && (nx == 0) && (ny == -1))
					return getMinOutline(oo, noo, io, nio);

				int tx = x + nx;
				int ty = y + ny;

				if ((oo[0][noo - 1] != tx) || (oo[1][noo - 1] != ty))
				{
					if (noo == MAX_OUTLINE)
						return null;
					oo[0][noo] = tx;
					oo[1][noo++] = ty;
				}

				if ((io[0][nio - 1] != x) || (io[1][nio - 1] != y))
				{
					if (nio == MAX_OUTLINE)
						return null;
					io[0][nio] = x;
					io[1][nio++] = y;
				}

				if (nc++ > 8)
					break;

				ox = nx;
				oy = ny;
				tmp = cirx(nx, ny);
				ny = ciry(nx, ny);
				nx = tmp;
			}

			if (nc > 8)
				break;

			x += nx;
			y += ny;
			nx = ox - nx;
			ny = oy - ny;
		}

		// single pixel
		return getMinOutline(oo, noo, io, nio);
	}


	private int[][][] getMinOutline(int[][] oo, int noo, int[][] io, int nio)
	{
		int[][] oo_f = new int[2][noo];
		int[][] io_f = new int[2][nio];
		System.arraycopy(oo[0], 0, oo_f[0], 0, noo);
		System.arraycopy(oo[1], 0, oo_f[1], 0, noo);
		System.arraycopy(io[0], 0, io_f[0], 0, nio);
		System.arraycopy(io[1], 0, io_f[1], 0, nio);
		return new int[][][] {oo_f, io_f};
	}


	private boolean isfg(boolean[] mask, int w, int h, int x, int y)
	{
		int idx = y * w + x;
		if ((x < 0) || (y < 0) || (x >= w) || (y >= h))
			return false;
		else
			return mask[idx];
	}


	/**
	 * Find the next delta-x point in a 9-pixel square (in clockwise order).
	 * 
	 * @param x
	 *            initial x coordinate delta
	 * @param y
	 *            initial y coordinate delta
	 * @return new x coordinate delta
	 */
	private int cirx(int x, int y)
	{
		if (x == y)
			return 0;
		else if (x > y)
			return 1;
		else
			return -1;
	}


	/**
	 * Find the next delta-y point in a 9-pixel square (in clockwise order).
	 * 
	 * @param x
	 *            initial x coordinate delta
	 * @param y
	 *            initial y coordinate delta
	 * @return new x coordinate delta
	 */
	private int ciry(int x, int y)
	{
		if (x == -y)
			return 0;
		else if ((x + y) > 0)
			return 1;
		else
			return -1;
	}


	/**
	 * Create a pixel mask from pixel data by selecting pixels within a given
	 * tolerance of the given foreground color.
	 * 
	 * @param buf
	 *            pixel data
	 * @param fgcolor
	 *            foreground color
	 * @param fgtol
	 *            tolerance
	 * @return pixel mask (true = foreground, false = background)
	 */
	public boolean[] createMask(Rect buf, byte fgcolor, int fgtol)
	{
		// System.out.printf("DBG: creating mask: col=%s, tol=%d\n",
		// getRGBStr(fgcolor), fgtol);
		byte[] pixels = buf.clone().getPixels();
		boolean[] b = new boolean[pixels.length];
		for (int i = 0; i < pixels.length; i++)
			b[i] = withinTol(pixels[i], fgcolor, fgtol);
		return b;
	}


	private static String getRGBStr(byte p)
	{
		int pr = (p >> 0) & 0x7;
		int pg = (p >> 3) & 0x7;
		int pb = (p >> 6) & 0x3;
		return pr + "," + pg + "," + pb;
	}


	/**
	 * Tests whether a pixel is within a given tolerance of a given foreground
	 * color.
	 * 
	 * @param p
	 *            pixel
	 * @param c
	 *            foreground color
	 * @param t
	 *            tolerance
	 * @return true if within tolerance, false otherwise
	 */
	public boolean withinTol(byte p, byte c, int t)
	{
		int pr = (p >> 0) & 0x7;
		int pg = (p >> 3) & 0x7;
		int pb = (p >> 6) & 0x3;
		int cr = (c >> 0) & 0x7;
		int cg = (c >> 3) & 0x7;
		int cb = (c >> 6) & 0x3;
		int dr = Math.abs(pr - cr);
		int dg = Math.abs(pg - cg);
		int db = Math.abs(pb - cb);
		return (dr + dg + db) <= t;
	}
}
