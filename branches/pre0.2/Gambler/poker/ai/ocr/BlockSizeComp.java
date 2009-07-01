/*
 * BlockSizeComp.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.ai.ocr;

import java.util.Comparator;


public class BlockSizeComp implements Comparator<CharBlock>
{

	public int compare(CharBlock b1, CharBlock b2)
	{
		return Charset.sizeCmp(b1.block, b2.block);
	}

}
