package poker.server.session.model.visual;

import java.util.List;

import poker.common.Rect;
import poker.server.session.model.ModelError;
import poker.server.session.model.data.Field;
import poker.server.session.model.data.ScreenData;
import poker.server.session.model.data.chat.Chat;
import poker.util.xml.XmlObject;

/**
 * A label is a screen region containing text.
 * 
 * @author lowentropy
 */
public class Label extends Component
{

	/** target data structure for label text */
	private String target;

	/** X and Y offsets from component anchor */
	private int offsetX, offsetY;

	/** width and height of label */
	private int width, height;

	/** number of lines (labels stacked vertically with 0 separator) */
	private int lines;

	/** name of style to use to extract text */
	private String styleName;

	/** screen object which is label's parent */
	private Screen screen;

	/** grid containing label, or null */

	private Grid grid;

	private int	gridIdx;
	
	private boolean valid = false;

	private boolean	modified;

	/**
	 * Constructor.
	 * 
	 * @param xml
	 *            xml object
	 * @param idx 
	 * @throws ModelError
	 */
	public Label(XmlObject xml, Screen screen, Grid grid, int idx) throws ModelError
	{
		this.screen = screen;
		this.grid = grid;
		this.gridIdx = idx;

		target = xml.getValue("target");
		styleName = xml.getValue("style");
		lines = Integer.parseInt(xml.getValue("lines"));

		int[] offset = parseCoord(xml.getValue("offset"));
		int[] size = parseCoord(xml.getValue("size"));

		offsetX = offset[0];
		offsetY = offset[1];
		width = size[0];
		height = size[1];
	}

	/**
	 * @see poker.server.session.model.visual.Component#updateData(poker.server.session.model.data.ScreenData,
	 *      poker.common.Rect, int, int)
	 */
	public boolean updateData(ScreenData data, Rect win, int wx, int wy, int ax, int ay)
	{
		int x = offsetX + ax;
		int y = offsetY + ay;

		if (grid != null)
		{
			x += grid.getOffsetX();
			y += grid.getOffsetY();
			y += gridIdx * grid.getDelta();
		}
		
		Rect srect = win.sub(x, y, width, height * lines, false);
		if (srect.getPixels() == null)
			System.out.printf("bad win.sub on label tgt: %s\n", target); // DBG
		boolean mod = modified(srect);
		
		if (!mod)
			return false;

		Style style = (grid != null) ? grid.getStyle(styleName, gridIdx) : screen
				.getStyle(styleName);
		
		String[] text = new String[lines];

		for (int i = 0; i < lines; i++, y += height)
		{
			Rect rect = win.sub(x, y, width, height, false);
			text[i] = style.getText(rect);
			// System.out.printf("DBG: label %s line %d = %s\n", target, i, text[i]);
		}

		if (grid == null)
		{
			modified = false;
			Object o = data.getTarget(target);
			boolean ov = valid;
			
			if (o instanceof Field)
			{
				Field f = (Field) o;
				valid = f.match(text);
				if (ov != valid)
					modified = true;
				else if (valid && f.wasModified())
					modified = true;
			}
			else
			{
				Chat c = (Chat) o;
				valid = c.match(text, data);
				if (ov != valid)
					modified = true;
				else if (valid && c.wasModified())
					modified = true;
			}
		}
		else
			grid.setCellData(target, gridIdx, text);
		
		return true;
	}
	
	
	public boolean wasModified()
	{
		return modified && updated;
	}


	/**
	 * @see poker.server.session.model.visual.Component#getRegions(java.util.List,int,int)
	 */
	public void getRegions(List<Region> regions, int ax, int ay)
	{
		regions.add(getRegion(ax, ay));
	}

	public String getTargetName()
	{
		return target;
	}

	public Region getRegion(int ax, int ay)
	{
		int x = offsetX + ax;
		int y = offsetY + ay;
		int w = width;
		int h = height * lines;
		if (grid != null)
		{
			x += grid.getOffsetX();
			y += grid.getOffsetY();
			y += grid.getDelta() * this.gridIdx;
		}
		return new Region(this, x, y, w, h);
	}

	public String getName()
	{
		return target;
	}

	public String getTarget()
	{
		return target;
	}
	
	public boolean isValid()
	{
		return valid;
	}

	public int getOffsetX()
	{
		return this.offsetX;
	}

	public int getOffsetY()
	{
		return this.offsetY;
	}

}
