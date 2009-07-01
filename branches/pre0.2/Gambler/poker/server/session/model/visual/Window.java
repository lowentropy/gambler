package poker.server.session.model.visual;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import poker.common.Rect;
import poker.server.session.model.ModelError;
import poker.server.session.model.data.ScreenData;
import poker.util.xml.XmlObject;

/**
 * A window defines the active region which a screen scrapes for component
 * events. It defines a method to find the window on the whole screen, to close
 * the window, and provides an anchor location for the screen components.
 * 
 * @author lowentropy
 */
public class Window extends Component
{

	/** X and Y coordinates of default location of window */
	private int posX, posY;

	/** width and height of window */
	private int width, height;

	/** mode to find window; currently, only 'fixed' is supported */
	private String findMode;

	/** verifications of window presence */
	private Verify[] verifies;

	/** window anchors */
	private Anchor[] anchors;

	/** map from anchor name to anchor object */
	private Map<String, Anchor> anchorMap;

	/** X and Y coordinates of close button, if it exists */
	private int closeX, closeY;

	/** whether there is a clickable close button */
	private boolean canClose;

	/**
	 * Constructor.
	 * 
	 * @param xml
	 *            xml object
	 * @throws ModelError
	 */
	public Window(XmlObject xml, Screen screen) throws ModelError
	{
		findMode = xml.getValue("find");
		int[] pos = parseCoord(xml.getValue("position"));
		int[] size = parseCoord(xml.getValue("size"));

		posX = pos[0];
		posY = pos[1];

		width = size[0];
		height = size[1];

		List<XmlObject> vxmls = xml.getChildren("verify");
		verifies = new Verify[vxmls.size()];

		int i = 0;
		for (XmlObject vxml : vxmls)
			verifies[i++] = new Verify(vxml, screen);

		List<XmlObject> axmls = xml.getChildren("anchor");
		anchors = new Anchor[axmls.size()];

		i = 0;
		for (XmlObject axml : axmls)
			anchors[i++] = new Anchor(axml);

		anchorMap = new HashMap<String, Anchor>();
		for (Anchor a : anchors)
			anchorMap.put(a.getName(), a);

		XmlObject cxml = xml.getChild("close");
		if (cxml != null)
		{
			int[] close = parseCoord(cxml.getValue("offset"));
			closeX = close[0];
			closeY = close[1];
			canClose = true;
		}
		else
			canClose = false;
	}

	/**
	 * Return the sub-rectangle of the screen pixels which this window covers.
	 * 
	 * @param rect
	 *            screen pixels rectangle
	 * @return window pixels rectangle
	 */
	public Rect subRect(Rect rect)
	{
		Rect r = rect.sub(posX, posY, width, height, false);
		return r;
	}

	/**
	 * Find the window on the screen given the window's find method.
	 * 
	 * @param rect
	 *            screen pixels rectangle
	 */
	public boolean find(Rect rect)
	{
		if (findMode.equals("fixed"))
		{
			return true;
		}
		else
			return false;
	}

	/**
	 * Get the X coordinate of the anchor of the given name.
	 * 
	 * @param anchor
	 *            anchor name
	 * @return X coordinate of anchor
	 */
	public int getAnchorX(String anchor)
	{
		return anchorMap.get(anchor).getX();
	}

	/**
	 * Get the Y coordinate of the anchor of the given name.
	 * 
	 * @param anchor
	 *            anchor name
	 * @return Y coordinate of anchor
	 */
	public int getAnchorY(String anchor)
	{
		return anchorMap.get(anchor).getY();
	}

	/**
	 * @see poker.server.session.model.visual.Component#updateData(poker.server.session.model.data.ScreenData,
	 *      poker.common.Rect, int, int)
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
		// ax and ay should be zero
		for (Verify v : this.verifies)
			v.getRegions(regions, 0, 0);
	}

	public String getFindMode()
	{
		return this.findMode;
	}

	public Verify getVerifyByIcon(String string)
	{
		for (Verify v : this.verifies)
			if (v.getIconName().equals(string))
				return v;
		return null;
	}

	public boolean hasCloseButton()
	{
		return canClose;
	}

	public int closeBtnX()
	{
		return closeX;
	}

	public int closeBtnY()
	{
		return closeY;
	}

	public Verify[] getVerifies()
	{
		return verifies;
	}

	public int getActualAnchorY(String anchor)
	{
		return getAnchorY(anchor) + posY;
	}

	public int getActualAnchorX(String anchor)
	{
		return getAnchorX(anchor) + posX;
	}

	public int getPositionX()
	{
		return posX;
	}
	
	public int getPositionY()
	{
		return posY;
	}

	public Region getRegion(int ax, int ay)
	{
		return null;
	}

	public String getName()
	{
		return null;
	}

	public String getTarget()
	{
		return null;
	}
	
	
}
