/* 
 * Area.java
 * 
 * created: 23-Apr-06
 * author: Nathan Matthews
 * email: lowentropy@gmail.com
 * 
 * Copyright (C) 2006
 */
package poker.server.session.model.visual;

import java.util.List;

import poker.common.Rect;
import poker.server.session.model.ModelError;
import poker.server.session.model.data.ScreenData;
import poker.util.xml.XmlObject;


/**
 * TODO: Area
 * 
 * @author lowentropy
 * 
 */
public class Area extends Component
{

	private String name;
	
	private int offsetX, offsetY;
	
	private int width, height;
	
	
	public Area(XmlObject xml) throws ModelError
	{
		name = xml.getValue("name");
		int[] offset = parseCoord(xml.getValue("offset"));
		int[] size = parseCoord(xml.getValue("size"));
		offsetX = offset[0];
		offsetY = offset[1];
		width = size[0];
		height = size[1];
	}
	
	/**
	 * @see poker.server.session.model.visual.Component#updateData(poker.server.session.model.data.ScreenData, poker.common.Rect, int, int, int, int)
	 */
	public boolean updateData(ScreenData data, Rect win, int wx, int wy,
			int ax, int ay)
	{
		return false;
	}


	/**
	 * @see poker.server.session.model.visual.Component#getRegions(java.util.List, int, int)
	 */
	public void getRegions(List<Region> regions, int ax, int ay)
	{
	}


	/**
	 * @see poker.server.session.model.visual.Component#getRegion(int, int)
	 */
	public Region getRegion(int ax, int ay)
	{
		return null;
	}


	/**
	 * @see poker.server.session.model.visual.Component#getName()
	 */
	public String getName()
	{
		return name;
	}


	/**
	 * @see poker.server.session.model.visual.Component#getTarget()
	 */
	public String getTarget()
	{
		return null;
	}

	
	public int getOffsetX()
	{
		return offsetX;
	}
	
	public int getOffsetY()
	{
		return offsetY;
	}
	
	public int getWidth()
	{
		return width;
	}
	
	public int getHeight()
	{
		return height;
	}

	public Rect getRect(Rect rect, int ax, int ay)
	{
		return rect.sub(ax + offsetX, ay + offsetY, width, height, false);
	}
}
