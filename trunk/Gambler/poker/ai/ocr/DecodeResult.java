/*
 * DecodedText.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.ai.ocr;

public class DecodeResult
{

	public String	text;

	public byte		color;

	public int		colorIdx;


	public DecodeResult(String text, byte color, int colorIdx)
	{
		this.text = text;
		this.color = color;
		this.colorIdx = colorIdx;
	}


	public static DecodeResult invalid()
	{
		return new DecodeResult(null, (byte) 0, 0);
	}
}
