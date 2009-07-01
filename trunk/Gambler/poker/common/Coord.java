/*
 * Coord.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


public class Coord
{

	public int	x, y;


	public Coord(int x, int y)
	{
		this.x = x;
		this.y = y;
	}


	public static Coord read(DataInputStream dis) throws IOException
	{
		return new Coord(dis.readInt(), dis.readInt());
	}


	public void write(DataOutputStream dos) throws IOException
	{
		dos.writeInt(x);
		dos.writeInt(y);
	}


	public void drawOn(Rect rect)
	{
		rect.drawX(x, y, 5, (byte) 0x80);
	}
}
