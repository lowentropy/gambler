
package poker.server.session.model.visual;

import java.io.IOException;
import java.util.List;

import poker.common.Rect;
import poker.server.session.model.ModelError;
import poker.server.session.model.data.ScreenData;
import poker.util.xml.XmlObject;


/**
 * A verify contains an offset, icon, and mode. If the given icon is at the
 * given offset, and mode=require, then the verification succeeds. If
 * mode=reject, then the verify fails, and the message parameter is used.
 * 
 * @author lowentropy
 */
public class Verify extends Component
{

	/** name of icon to look for */
	private String	iconName;

	/** offset from window position to look for icon */
	private int		offsetX, offsetY;

	/** verify mode (currently only require and reject are allowed) */
	private String	mode;

	/** message to report if rejected or failed, or null if none */
	@SuppressWarnings("unused")
	private String	message;

	/** screen object which is parent of verify */
	private Screen	screen;

	/** whether verify is valid */
	private boolean	valid;

	/** actual verify pixels */
	private Rect	actual;


	/**
	 * Constructor.
	 * 
	 * @param xml
	 *            xml object
	 * @throws ModelError
	 */
	public Verify(XmlObject xml, Screen screen) throws ModelError
	{
		this.screen = screen;

		iconName = xml.getValue("icon");
		mode = xml.getValue("mode");
		message = xml.getValue("message");

		int[] offset = parseCoord(xml.getValue("offset"));
		offsetX = offset[0];
		offsetY = offset[1];

		valid = false;
	}


	/**
	 * @see poker.server.session.model.visual.Component#updateData(poker.server.session.model.data.ScreenData,
	 *      poker.common.Rect, int, int, int)
	 */
	public boolean updateData(ScreenData data, Rect win, int wx, int wy, int ax, int ay)
	{
		int x = offsetX;
		int y = offsetY;

		Icon icon = screen.getIcon(iconName);
		actual = icon.subRect(win, x, y).clone();

		valid = false;

		if (icon.match(actual))
		{
			if (mode.equals("reject"))
				fail("icon found, rejected: ");
			else
				valid = true;
		}
		else if (mode.equals("require"))
			fail("icon not found, required: ");
		else
			valid = true;

		return modified(actual);
	}


	/**
	 * Fail; use given message prefix.
	 * 
	 * @param prefix
	 *            message prefix
	 */
	private void fail(String prefix)
	{
		// do nothing
	}


	/**
	 * @see poker.server.session.model.visual.Component#getRegions(java.util.List,
	 *      int, int)
	 */
	public void getRegions(List<Region> regions, int ax, int ay)
	{
		regions.add(getRegion(ax, ay));
	}


	public String getIconName()
	{
		return iconName;
	}


	public boolean isValid()
	{
		return valid;
	}


	public Region getRegion(int ax, int ay)
	{
		Icon icon = screen.getIcon(iconName);
		return new Region(this, ax + offsetX, ay + offsetY, icon.getWidth(),
				icon.getHeight());
	}


	public String getName()
	{
		return this.iconName;
	}


	public String getTarget()
	{
		return null;
	}


	public void printActual()
	{
		System.out.printf("VERIFY (%d x %d):\n%s\n", actual.getWidth(),
				actual.getHeight(), actual.toHex(40));
		try
		{
			actual.write("/home/lowentropy/src/screendump/verify.img");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
