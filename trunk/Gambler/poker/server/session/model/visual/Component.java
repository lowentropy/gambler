
package poker.server.session.model.visual;

import java.io.IOException;
import java.util.List;

import poker.common.Rect;
import poker.server.session.model.ModelError;
import poker.server.session.model.data.ScreenData;
import poker.util.xml.XmlObject;


/**
 * Abstract base class containing functionality for all components.
 * 
 * @author lowentropy
 */
public abstract class Component
{

	int foobar;
	
	protected boolean	updated	= false;

	private Rect lastRect = null;

	protected boolean modified(Rect rect)
	{
		boolean m = true;
		if (lastRect != null)
			m = !rect.matches(lastRect);
		if (m && lastRect != null)
		{
			// DBG
			try
			{
				lastRect.write("/home/lowentropy/src/screendump/"+getName()+"-old.img");
				rect.write("/home/lowentropy/src/screendump/"+getName()+"-new.img");
			}
			catch (IOException e)
			{
			}
		}
		lastRect = rect.clone();
		return m;
	}
	
	
	/**
	 * Parse rgb color in format "r,g,b" into fields r, g, and b, and byte
	 * color.
	 * 
	 * @param rgb
	 *            rgb string
	 * @return array of [r,g,b,rgb]
	 * @throws ModelError
	 */
	protected int[] parseColor(String rgb) throws ModelError
	{
		String[] rgba = rgb.split(",");
		if (rgba.length != 3)
			throw new ModelError("invalid rgb color: " + rgb);

		int r = Integer.parseInt(rgba[0]);
		int g = Integer.parseInt(rgba[1]);
		int b = Integer.parseInt(rgba[2]);

		if ((r < 0) || (r > 7))
			throw new ModelError("red value out of bounds: " + r);

		if ((g < 0) || (g > 7))
			throw new ModelError("green value out of bounds: " + g);

		if ((b < 0) || (b > 3))
			throw new ModelError("blue value out of bounds: " + b);

		int color = r | (g << 3) | (b << 6);

		return new int[] {r, g, b, color};
	}


	/**
	 * Parse a coordinate in the form (x,y) into its components.
	 * 
	 * @param coord
	 *            coordinate to parse
	 * @return array of [x,y]
	 * @throws ModelError
	 */
	protected static int[] parseCoord(String coord) throws ModelError
	{
		int idx = coord.indexOf(',');

		if (idx == -1)
			throw new ModelError("invalid coordinate: " + coord);
		if ((coord.charAt(0) != '(')
				|| (coord.charAt(coord.length() - 1) != ')'))
			throw new ModelError("invalid coordinate: " + coord);

		int x = Integer.parseInt(coord.substring(1, idx));
		int y = Integer.parseInt(coord.substring(idx + 1, coord.length() - 1));

		return new int[] {x, y};
	}


	/**
	 * Create a component based on the type of xml object given.
	 * 
	 * @param xml
	 *            xml object
	 * @param grid
	 *            grid container
	 * @return component
	 * @throws ModelError
	 */
	public static Component create(XmlObject xml, Screen screen, Grid grid, int idx)
			throws ModelError
	{
		String type = xml.getTag();

		if (type.equals("label"))
			return new Label(xml, screen, grid, idx);
		else if (type.equals("grid"))
			return new Grid(xml, screen);
		else if (type.equals("style"))
			return new Style(xml);
		else if (type.equals("button"))
			return new Button(xml);
		else if (type.equals("area"))
			return new Area(xml);
		else
			throw new ModelError("invalid create() type: " + type);
	}


	/**
	 * Update the given dataset based on this component's type, using the given
	 * rectangle of pixels and anchor position within that rectangle.
	 * 
	 * @param data
	 *            dataset to update
	 * @param win
	 *            window of pixels
	 * @param ax
	 *            X coordinate of anchor
	 * @param ay
	 *            Y coordinate of anchor
	 *            @return whether data changed
	 */
	public abstract boolean updateData(ScreenData data, Rect win, int wx, int wy, int ax, int ay);


	/**
	 * Set the updated flag.
	 * 
	 * @param b
	 *            new updated flag value
	 */
	public void setUpdated(boolean b)
	{
		updated = b;
	}


	/**
	 * Get the regions defined by the component, and put them into the list.
	 * 
	 * @param regions
	 *            list to put component regions in
	 * @param ax
	 *            x component of anchor
	 * @param ay
	 *            y component of anchor
	 */
	public abstract void getRegions(List<Region> regions, int ax, int ay);


	/**
	 * @return whether component was updated
	 */
	public boolean wasUpdated()
	{
		return updated;
	}


	/**
	 * Get the entire surrounding region of the component.
	 * 
	 * @param ax
	 *            x component of anchor
	 * @param ay
	 *            y component of anchor
	 * @return surrounding region
	 */
	public abstract Region getRegion(int ax, int ay);


	/**
	 * @return name of component
	 */
	public abstract String getName();


	/**
	 * @return name of target data fields
	 */
	public abstract String getTarget();


	public boolean wasModified()
	{
		return false;
	}

}
