/*
 * AlphaNumCharComp.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.ai.ocr;

import java.util.Comparator;


public class AlphaNumCharComp implements Comparator<Char>
{

	public int compare(Char c1, Char c2)
	{
		return c1.getText().compareTo(c2.getText());
	}

}
