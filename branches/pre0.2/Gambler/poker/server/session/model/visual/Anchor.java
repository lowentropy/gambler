package poker.server.session.model.visual;

import java.util.List;

import poker.common.Rect;
import poker.server.session.model.ModelError;
import poker.server.session.model.data.ScreenData;
import poker.util.xml.XmlObject;

/**
 * Named offset inside a window serving as a coordinate root for a subset of the
 * window's components.
 * 
 * @author lowentropy
 */
public class Anchor extends Component
{

	/** name of anchor */
	private String name;

	/** offset from top-left corner of window */
	private int offsetX, offsetY;

	/**
	 * Constructor.
	 * 
	 * @param xml
	 *            xml object
	 * @throws ModelError
	 */
	public Anchor(XmlObject xml) throws ModelError
	{
		name = xml.getValue("name");
		int[] offset = parseCoord(xml.getValue("offset"));
		offsetX = offset[0];
		offsetY = offset[1];
	}

	/**
	 * @return anchor name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @return X offset
	 */
	public int getX()
	{
		return offsetX;
	}

	/**
	 * @return Y offset
	 */
	public int getY()
	{
		return offsetY;
	}

	/**
	 * @see poker.server.session.model.visual.Component#updateData(poker.server.session.model.data.ScreenData,
	 *      poker.common.Rect, int, int, int)
	 */
	public boolean updateData(ScreenData data, Rect win, int wx, int wy, int ax, int ay)
	{
		return false;
	}

	/**
	 * @see poker.server.session.model.visual.Component#getRegions(java.util.List,
	 *      int, int)
	 */
	public void getRegions(List<Region> regions, int ax, int ay)
	{
		// do nothing
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
