
package poker.server.session.model.visual;

import java.io.IOException;
import java.util.List;

import poker.common.Rect;
import poker.server.session.model.ModelError;
import poker.server.session.model.data.ScreenData;
import poker.util.xml.XmlObject;


/**
 * An icon is just a rectangle of pixels that another component may expect to be
 * on some portion of the screen. Right now, the pixels are opaque, but future
 * versions may allow fuzzy matching of pixel data for state variables.
 * 
 * @author lowentropy
 */
public class Icon extends Component
{

	/** name of icon */
	private String	name;

	/** encoded pixel data */
	private String	encoding;

	/** decoded pixel data */
	private byte[]	pixels;

	/** rectangle formed from pixels */
	private Rect	rect;

	/** width and height of icon */
	private int		width, height;


	/**
	 * Constructor.
	 * 
	 * @param xml
	 *            xml object
	 * @throws ModelError
	 */
	public Icon(XmlObject xml) throws ModelError
	{
		name = xml.getValue("name");
		encoding = xml.getText();

		int[] size = parseCoord(xml.getValue("size"));
		width = size[0];
		height = size[1];

		decode();
	}


	/**
	 * Decode hex data from xml stream into array of pixel bytes.
	 */
	private void decode()
	{
		int len = 0;

		String[] lines = encoding.trim().split("\n");

		for (String line : lines)
			len += line.length() / 2;

		pixels = new byte[len];
		int i = 0;

		for (String line : lines)
			for (int p = 0; p < (line.length() - 1);)
			{
				int x = decodeHex(line.substring(p, p + 2));
				if (x > -1)
				{
					pixels[i++] = (byte) x;
					p += 2;
				}
				else
					p++;
			}

		rect = new Rect(pixels, width, height);
	}


	private int decodeHex(String s)
	{
		s = s.toLowerCase();
		int i0 = hc2i(s.charAt(0));
		int i1 = hc2i(s.charAt(1));
		if (i0 < 0 || i1 < 0)
			return -1;
		return (i0 * 16) + i1;
	}


	private int hc2i(char c)
	{
		if ((c >= '0') && (c <= '9'))
			return (c - '0');
		else if ((c >= 'a') && (c <= 'f'))
			return ((c - 'a') + 10);
		else
			return -1;
	}


	/**
	 * @see poker.server.session.model.visual.Component#updateData(poker.server.session.model.data.ScreenData,
	 *      poker.common.Rect, int, int, int)
	 */
	public boolean updateData(ScreenData data, Rect win, int wx, int wy,
			int ax, int ay)
	{
		return false;
	}


	/**
	 * @return name of icon
	 */
	public String getName()
	{
		return name;
	}


	/**
	 * Get the subrectangle of the given pixels which is covered by this icon.
	 * 
	 * @param rect
	 *            rectangle to get subportion of
	 * @param x
	 *            left coordinate of sub-rectangle
	 * @param y
	 *            top coordinate of sub-rectangle
	 * @return sub-rectangle
	 */
	public Rect subRect(Rect rect, int x, int y)
	{
		return rect.sub(x, y, width, height, false);
	}


	public boolean match(Rect rect)
	{
		return this.rect.matches(rect);
	}
	
	/**
	 * Determine whether the icon matches the given rectangle of pixels.
	 * 
	 * @param rect
	 *            rectangle to match against
	 * @param d percent correct required
	 * @param f 
	 * @return whether icon matches
	 */
	public boolean match(Rect rect, int tol, float f)
	{
		boolean b = this.rect.matchesWithinTol(rect, tol, f);
		return b;
	}


	/**
	 * @see poker.server.session.model.visual.Component#getRegions(java.util.List,int,int)
	 */
	public void getRegions(List<Region> regions, int ax, int ay)
	{
		// do nothing
	}


	/**
	 * @return width
	 */
	public int getWidth()
	{
		return width;
	}


	/**
	 * @return height
	 */
	public int getHeight()
	{
		return height;
	}


	public Region getRegion(int ax, int ay)
	{
		return null;
	}


	public String getTarget()
	{
		return null;
	}

}
