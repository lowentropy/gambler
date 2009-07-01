
package poker.server.session.model.visual;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import poker.common.Coord;
import poker.common.Rect;
import poker.server.session.model.ModelError;
import poker.server.session.model.data.Field;
import poker.server.session.model.data.ListItem;
import poker.server.session.model.data.ScreenData;
import poker.util.xml.XmlObject;


/**
 * A grid is a multi-row, multi-column container of sub-components, sometimes
 * with a scrollbar that controls the visible rows.
 * 
 * @author lowentropy
 */
public class Grid extends Component
{

	/** target data container for grid (a list) */
	private String						target;

	/** X and Y coordinates of offset from component anchor */
	private int							offsetX, offsetY;

	/** total width of grid */
	private int							width;

	/** height is equal to (delta * rows) */
	private int							height;

	/** height of a row including separator */
	private int							delta;

	/** unusable space in a row (at the bottom) */
	private int							sep;

	/** number of rows in grid */
	private int							rows;

	/** styles defining text extraction based on selection */
	private Map<String, Style>			styles;

	/** components in grid, offset by top-left corner */
	private Map<String, Component[]>	columns;

	/** internal store of grid cell data */
	private Map<String, String[][]>		gridData;

	/** scrollbar controlling grid (null if none) */
	private Scrollbar					scroll;

	/** index of row which is currently selected, or -1 if none is */
	private int							selectedRow	= 0;


	/**
	 * Constructor.
	 * 
	 * @param xml
	 *            xml object
	 * @param screen
	 *            container
	 * @throws ModelError
	 */
	public Grid(XmlObject xml, Screen screen) throws ModelError
	{
		styles = new HashMap<String, Style>();

		target = xml.getValue("target");
		width = Integer.parseInt(xml.getValue("width"));
		rows = Integer.parseInt(xml.getValue("rows"));
		delta = Integer.parseInt(xml.getValue("delta"));
		sep = Integer.parseInt(xml.getValue("sep"));
		height = delta * rows;

		int[] offset = parseCoord(xml.getValue("offset"));
		offsetX = offset[0];
		offsetY = offset[1];

		List<XmlObject> cols = xml.getChild("columns").getChildren();
		columns = new HashMap<String, Component[]>();
		gridData = new HashMap<String, String[][]>();

		int i = 0;
		for (XmlObject col : cols)
		{
			Component[] ccol = new Component[rows];
			for (int j = 0; j < rows; j++)
				ccol[j] = Component.create(col, screen, this, j);
			columns.put(ccol[0].getName(), ccol);
			gridData.put(ccol[0].getName(), new String[rows][]);
		}

		scroll = null;
		for (XmlObject sb : xml.getChildren("scrollbar"))
			scroll = new Scrollbar(sb, this);

		for (XmlObject sxml : xml.getChildren("style"))
		{
			Style s = new Style(sxml);
			styles.put(s.getName(), s);
		}
	}


	/**
	 * Get the style for the component identified by the given id. If there is
	 * no special style to use for the given component, query the screen for the
	 * style name.
	 * 
	 * @param styleName
	 *            name of style, if no grid style needed
	 * @param gridId
	 *            id of component in grid
	 * @return
	 */
	public Style getStyle(String styleName, int row)
	{
		if (row == selectedRow)
			return styles.get("selected");
		else
			return styles.get("default");
	}


	/**
	 * @see poker.server.session.model.visual.Component#updateData(poker.server.session.model.data.ScreenData,
	 *      poker.common.Rect, int, int, int)
	 */
	public boolean updateData(ScreenData data, Rect win, int wx, int wy, int ax, int ay)
	{
		// for right now, lists must be updated using scanGrid
		// so all the grid does is cache row data in its own rectangular array,
		// waiting for a call to storeRow. the cells of the grid have visual
		// regions which should update data to the grid. this method should
		// never be called anyway, as there is no grid 'container region'
		return false;
	}


	/**
	 * @return row delta
	 */
	public int getDelta()
	{
		return delta;
	}


	/**
	 * @return target name
	 */
	public String getTarget()
	{
		return target;
	}


	/**
	 * @see poker.server.session.model.visual.Component#wasUpdated()
	 */
	public boolean wasUpdated()
	{
		return true;
	}


	/**
	 * @see poker.server.session.model.visual.Component#getRegions(java.util.List,
	 *      int, int)
	 */
	public void getRegions(List<Region> regions, int ax, int ay)
	{
		if (scroll != null)
			scroll.getRegions(regions, ax, ay);
		for (Component[] col : columns.values())
			for (Component c : col)
				c.getRegions(regions, ax, ay);
	}


	public int getWidth()
	{
		return width;
	}


	public int getHeight()
	{
		return height;
	}


	public int getSep()
	{
		return sep;
	}


	public int getOffsetX()
	{
		return offsetX;
	}


	public int getOffsetY()
	{
		return offsetY;
	}


	public Scrollbar getScrollbar()
	{
		return scroll;
	}


	public Component getComponent(String name)
	{
		if (name.equals("scroll"))
			return scroll;

		int idx = name.indexOf("[");
		String cn = name.substring(0, idx);
		String ci = name.substring(idx + 1, name.length() - 1);
		int i = Integer.parseInt(ci);
		return columns.get(cn)[i];
	}


	public void storeRow(int gridRow, poker.server.session.model.data.List l,
			int listRow)
	{
		ListItem item = l.getItem(listRow);
		for (String col : columns.keySet())
		{
			//System.out.printf("DBG: reading grid row %d, col %s\n", gridRow, col);
			String[] data = gridData.get(col)[gridRow];
			String target = columns.get(col)[0].getTarget();
			item.getField(target).match(data);
		}
	}


	public Coord getRowSelectCoord(int row)
	{
		// for now, the middle of a row
		int x = offsetX + width / 2;
		int y = offsetY + delta * row + delta / 2;
		return new Coord(x, y);
	}


	public int numRows()
	{
		return rows;
	}


	public void setSelectedRow(int i)
	{
		selectedRow = i;
	}


	public void setCellData(String colName, int row, String[] data)
	{
		//System.out.printf("DBG: writing grid row %d, col %s\n", row, colName);
		//if (data == null)
			//System.out.println("(and it's null)");
		//else
		//{
//			for (String s : data)
				//System.out.printf("%s,", s);
			//System.out.println();
		//}
		gridData.get(colName)[row] = data;
	}


	public Region getRegion(int ax, int ay)
	{
		return new Region(this, ax + offsetX, ay + offsetY, width, height);
	}


	public String getName()
	{
		return target;
	}


	public Component[] getRowComponents(int i)
	{
		Component[] row = new Component[columns.size()];
		int c = 0;
		for (String s : columns.keySet())
			row[c++] = columns.get(s)[i];
		return row;
	}


	public int getSelectedRow()
	{
		return selectedRow;
	}

}
