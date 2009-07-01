
package poker.server.session.model.visual;

import java.util.List;

import poker.common.Rect;
import poker.server.session.model.ModelError;
import poker.server.session.model.data.ScreenData;
import poker.util.xml.XmlObject;


/**
 * A scrollbar controlls the visible rows of a grid.
 * 
 * @author lowentropy
 */
public class Scrollbar extends Component
{

	/** offset of top-left corner of scrollbar from grid anchor */
	private int		offsetX, offsetY;

	/** width of scrollbar */
	private int		width;

	/** offset of 'scroll up' button from top-left of scrollbar */
	private int		upX, upY;

	/** offset of 'scroll dn' button from top-left of scrollbar */
	private int		dnX, dnY;

	/** top offset of scroll region and its height */
	private int		scrollTop, scrollHeight;

	/** parent grid of scrollbar */
	private Grid	grid;


	/**
	 * Constructor.
	 * 
	 * @param xml
	 *            xml object
	 * @throws ModelError
	 */
	public Scrollbar(XmlObject xml, Grid grid) throws ModelError
	{
		width = Integer.parseInt(xml.getValue("width"));
		int[] offset = parseCoord(xml.getValue("offset"));
		scrollTop = Integer.parseInt(xml.getValue("scroll_top"));
		scrollHeight = Integer.parseInt(xml.getValue("scroll_height"));
		int[] up = new int[] {width / 2, 5};
		int[] dn = new int[] {width / 2, scrollHeight + scrollTop + up[1]};
		this.grid = grid;

		offsetX = offset[0];
		offsetY = offset[1];

		upX = up[0];
		upY = up[1];

		dnX = dn[0];
		dnY = dn[1];
	}


	/**
	 * @see poker.server.session.model.visual.Component#updateData(poker.server.session.model.data.ScreenData,
	 *      poker.common.Rect, int, int, int)
	 */
	public boolean updateData(ScreenData data, Rect win, int wx, int wy,
			int ax, int ay)
	{
		return modified(win.sub(ax + offsetX + grid.getOffsetX(), ay + offsetY
				+ grid.getOffsetY() + scrollTop, width, scrollHeight, false));
	}


	/**
	 * @see poker.server.session.model.visual.Component#getRegions(java.util.List,
	 *      int, int)
	 */
	public void getRegions(List<Region> regions, int ax, int ay)
	{
		regions.add(getRegion(ax, ay));
	}


	public Region getRegion(int ax, int ay)
	{
		int x = ax + grid.getOffsetX() + this.offsetX;
		int y = ay + grid.getOffsetY() + this.offsetY;

		return new Region(this, x, y + scrollTop, width, scrollHeight);
	}


	public String getName()
	{
		return "scroll";
	}


	public String getTarget()
	{
		return null;
	}


	public int getDnX()
	{
		return dnX;
	}


	public int getDnY()
	{
		return dnY;
	}


	public int getOffsetX()
	{
		return offsetX;
	}


	public int getOffsetY()
	{
		return offsetY;
	}


	public int getUpX()
	{
		return upX;
	}


	public int getUpY()
	{
		return upY;
	}


	public int getWidth()
	{
		return width;
	}

}
