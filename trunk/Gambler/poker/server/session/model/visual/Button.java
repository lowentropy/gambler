package poker.server.session.model.visual;

import java.util.List;

import poker.common.Rect;
import poker.server.session.model.ModelError;
import poker.server.session.model.data.ScreenData;
import poker.util.xml.XmlObject;

/**
 * A button is just a clickable location. If a text scrape or state change is
 * requried, use a label overlayed at the same location.
 * 
 * @author lowentropy
 */
public class Button extends Component
{

	/** name of button */
	private String name;

	/** X and Y coordinates of offset from component anchor */
	private int offsetX, offsetY;

	/**
	 * Constructor.
	 * 
	 * @param xml
	 *            xml object
	 * @throws ModelError
	 */
	public Button(XmlObject xml) throws ModelError
	{
		name = xml.getValue("name");
		int[] offset = parseCoord(xml.getValue("offset"));
		offsetX = offset[0];
		offsetY = offset[1];
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
	 * @see poker.server.session.model.visual.Component#getRegions(java.util.List,int,int)
	 */
	public void getRegions(List<Region> regions, int ax, int ay)
	{
		// do nothing
	}

	public String getName()
	{
		return name;
	}
	
	public int getOffsetX()
	{
		return offsetX;
	}
	
	public int getOffsetY()
	{
		return offsetY;
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
