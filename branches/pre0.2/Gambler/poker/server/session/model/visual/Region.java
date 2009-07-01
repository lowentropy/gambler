package poker.server.session.model.visual;

import poker.common.Rect;

public class Region
{

	private Component c;

	private int x1, y1;

	private int w, h;

	private int x2, y2;

	/**
	 * Constructor.
	 * 
	 * @param c
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 */
	public Region(Component c, int x, int y, int w, int h)
	{
		this.c = c;
		this.x1 = x;
		this.y1 = y;
		this.w = w;
		this.h = h;
		this.x2 = x1 + w - 1;
		this.y2 = y1 + h - 1;
	}

	/**
	 * Set the component's updated flag to false.
	 */
	public void clearUpdate()
	{
		c.setUpdated(false);
	}
	
	/**
	 * Set the component's updated flag to true.
	 */
	public void setUpdate(boolean b)
	{
		c.setUpdated(b);
	}

	/**
	 * If the region rectangle intersects the given rectangle, set the
	 * component's update flag to true.
	 * 
	 * @param x1
	 * @param y1
	 * @param w
	 * @param h
	 * @return
	 */
	public boolean intersects(int x1, int y1, int w, int h)
	{
		int x2 = x1 + w - 1;
		int y2 = y1 + h - 1;
		if (x1 > this.x2)
			return false;
		if (y1 > this.y2)
			return false;
		if (x2 < this.x1)
			return false;
		if (y2 < this.y1)
			return false;

		return true;
	}
	
	public int getWidth()
	{
		return w;
	}
	
	public int getHeight()
	{
		return h;
	}

	public int getX()
	{
		return this.x1;
	}
	
	public int getY()
	{
		return this.y1;
	}

	public Component getComponent()
	{
		return this.c;
	}

	public Region offset(int px, int py)
	{
		return new Region(c, x1 + px, y1 + py, w, h);
	}

	public Rect getRect(Rect rect)
	{
		return rect.sub(x1, y1, w, h, false);
	}
}
