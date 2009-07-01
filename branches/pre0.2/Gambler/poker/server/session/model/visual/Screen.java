
package poker.server.session.model.visual;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import poker.common.Coord;
import poker.common.Rect;
import poker.server.session.model.ModelError;
import poker.server.session.model.data.ScreenData;
import poker.util.xml.XmlObject;


/**
 * A screen is a stateful view of any part of the program the server wishes to
 * see. At some points, one screen is replaced by another as the active view
 * changes. Curently, a screen consists of only one window.
 * 
 * @author lowentropy
 */
public class Screen
{

	/** xml object which created screen (saved for data xml) */
	private XmlObject				xml;

	/** name of screen */
	private String					name;

	/** whether to auto-update the screen */
	private boolean					autoUpdate;

	/** icons defined for screen */
	private Icon[]					icons;

	/** window defining active area of screen */
	private Window					window;

	/** components of screen */
	private Component[]				components;

	/** map of component name -> component */
	private Map<String, Component>	componentMap;

	/** array of all screen regions */
	private Region[]				regions;

	/** anchor for components */
	private String					anchor;

	/** map of style name -> style */
	private Map<String, Style>		styles;

	/** map of icon name -> icon */
	private Map<String, Icon>		iconMap;

	/** time to wait for screen to load */
	private int						loadTime;

	/** safe mouse location */
	private int						safex, safey;


	/**
	 * Constructor.
	 * 
	 * @param xml
	 *            xml object
	 * @throws ModelError
	 */
	public Screen(XmlObject xml) throws ModelError
	{
		this.xml = xml;
		iconMap = new HashMap<String, Icon>();
		styles = new HashMap<String, Style>();
		componentMap = new HashMap<String, Component>();

		// throw a model error now if data xml is bad
		new ScreenData(xml.getChild("data"));

		name = xml.getValue("name");
		loadTime = Integer.parseInt(xml.getValue("loadtime"));
		autoUpdate = Boolean.parseBoolean(xml.getValue("auto"));

		window = new Window(xml.getChild("window"), this);
		anchor = xml.getChild("components").getValue("anchor");

		int[] safe = Component.parseCoord(xml.getValue("safe"));
		safex = safe[0];
		safey = safe[1];

		XmlObject sxmls = xml.getChild("styles");
		if (sxmls != null)
			for (XmlObject sxml : sxmls.getChildren())
			{
				Style s = new Style(sxml);
				styles.put(s.getName(), s);
			}

		List<XmlObject> cmp = xml.getChild("components").getChildren();
		components = new Component[cmp.size()];

		int i = 0;
		for (XmlObject cxml : cmp)
		{
			components[i] = Component.create(cxml, this, null, 0);
			componentMap.put(components[i].getName(), components[i]);
			i++;
		}

		XmlObject ixmls = xml.getChild("icons");
		if (ixmls != null)
			for (XmlObject ixml : ixmls.getChildren())
			{
				Icon icon = new Icon(ixml);
				iconMap.put(icon.getName(), icon);
			}

		icons = new Icon[iconMap.size()];
		i = 0;
		for (Icon icon : iconMap.values())
			icons[i++] = icon;

		getRegions();
	}


	private void getRegions()
	{
		int ax = window.getAnchorX(anchor);
		int ay = window.getAnchorY(anchor);

		List<Region> regions = new ArrayList<Region>();

		for (Component c : components)
			c.getRegions(regions, ax, ay);
		window.getRegions(regions, 0, 0);

		this.regions = regions.toArray(new Region[0]);
	}


	/**
	 * Update the data in a screen-dataset with the pixels in the given
	 * rectangle.
	 * 
	 * @param data
	 *            dataset to update
	 * @param rect
	 *            pixels of screen
	 * @param h
	 * @param w
	 * @param y
	 * @param x
	 */
	public void updateData(ScreenData data, Rect rect, int x, int y, int w,
			int h)
	{
		int wx = window.getPositionX();
		int wy = window.getPositionY();
		int ax = window.getAnchorX(anchor);
		int ay = window.getAnchorY(anchor);
		Rect sub = window.subRect(rect);
		x -= wx;
		y -= wy;
		for (Region r : regions)
			if (r.intersects(x, y, w, h))
				r.setUpdate(r.getComponent().updateData(data, sub, wx, wy, ax,
						ay));
	}


	/**
	 * Update the data in a screen-dataset with the pixels in the given
	 * rectangle. Only update given objects.
	 * 
	 * @param data
	 *            dataset to update
	 * @param rect
	 *            pixels of screen
	 * @param h
	 * @param w
	 * @param y
	 * @param x
	 */
	public void updatePartialData(ScreenData data, Rect rect,
			Object[] toUpdate, int x, int y, int w, int h, boolean force)
	{
		int wx = window.getPositionX();
		int wy = window.getPositionY();
		int ax = window.getAnchorX(anchor);
		int ay = window.getAnchorY(anchor);
		Rect sub = window.subRect(rect);
		x -= wx;
		y -= wy;
		for (Region r : regions)
		{
			boolean ok = false;
			for (Object o : toUpdate)
				if (o == r.getComponent())
				{
					ok = true;
					break;
				}
			if (ok && (force || r.intersects(x, y, w, h)))
				r.setUpdate(r.getComponent().updateData(data, sub, wx, wy, ax,
						ay));
		}
	}


	/**
	 * Get the icon of the given name.
	 * 
	 * @param iconName
	 *            name of icon
	 * @return icon object
	 */
	public Icon getIcon(String iconName)
	{
		return iconMap.get(iconName);
	}


	/**
	 * Get style of given name.
	 * 
	 * @param styleName
	 *            name of style
	 * @return style object
	 */
	public Style getStyle(String styleName)
	{
		return styles.get(styleName);
	}


	/**
	 * @return screen name
	 */
	public String getName()
	{
		return name;
	}


	public int getLoadTime()
	{
		return loadTime;
	}


	public Window getWindow()
	{
		return window;
	}


	public boolean doAutoUpdate()
	{
		return autoUpdate;
	}


	public ScreenData createDataObject()
	{
		try
		{
			return new ScreenData(xml.getChild("data"));
		}
		catch (ModelError e)
		{
			e.printStackTrace();
			return null;
		}
	}


	public Region getActualRegion(String name)
	{
		return getComponent(name).getRegion(window.getActualAnchorX(anchor),
				window.getActualAnchorY(anchor));
	}


	public Component getComponent(String name)
	{
		if (name.endsWith(".scroll"))
			return ((Grid) componentMap.get(name.substring(0, name.length() - 7))).getScrollbar();
		Component c = componentMap.get(name);
		if (c == null)
			System.err.printf("no component %s in screen %s\n", name, this.name);
		return c;
	}


	public void clearUpdated()
	{
		for (Region r : regions)
			r.clearUpdate();
	}


	public Grid getGrid(String gridName)
	{
		Component c = getComponent(gridName);
		if ((c == null) || !(c instanceof Grid))
			return null;
		return (Grid) c;
	}


	public int getActualAnchorX()
	{
		return window.getActualAnchorX(anchor);
	}


	public int getActualAnchorY()
	{
		return window.getActualAnchorY(anchor);
	}


	public Screen createCopy()
	{
		try
		{
			return new Screen(xml);
		}
		catch (ModelError e)
		{
			e.printStackTrace();
			return null;
		}
	}


	public Coord getButtonCoord(String clickTgt)
	{
		Component c = getComponent(clickTgt);
		if ((c == null) || !(c instanceof Button))
			return null;
		int x = ((Button) c).getOffsetX();
		int y = ((Button) c).getOffsetY();
		x += getActualAnchorX();
		y += getActualAnchorY();
		return new Coord(x, y);
	}


	public Region[] getAllRegions()
	{
		int px = window.getPositionX();
		int py = window.getPositionY();
		Region[] rs = new Region[regions.length];
		for (int i = 0; i < regions.length; i++)
			rs[i] = regions[i].offset(px, py);
		return rs;
	}


	public int getSafeX()
	{
		return safex;
	}


	public int getSafeY()
	{
		return safey;
	}


	public Screen copy()
	{
		try
		{
			return new Screen(xml);
		}
		catch (ModelError e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public Area getArea(String name)
	{
		return (Area) getComponent(name);
	}

}
