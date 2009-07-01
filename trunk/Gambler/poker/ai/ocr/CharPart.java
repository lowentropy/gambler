/*
 * CharPart.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.ai.ocr;

public class CharPart
{

	public CharBlock	block;

	public int			relx;

	public int			rely;


	public CharPart(CharBlock block, int relx, int rely)
	{
		this.block = block;
		this.relx = relx;
		this.rely = rely;
	}
}
