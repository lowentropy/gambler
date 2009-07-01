/*
 * PokerSuit.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.ai.core;

public enum PokerSuit
{

	CLUBS('c'), DIAMONDS('d'), HEARTS('h'), SPADES('s'), UNDEFINED(true, false), BOUND(
			true, true);

	/** printable character for toString() */
	private char	printableChar	= '_';

	/** whether suit is undefined */
	private boolean	undefined;

	/** whether suit is bound to variable */
	private boolean	bound;


	/**
	 * Constructor.
	 * 
	 * @param c
	 *            printable char
	 */
	private PokerSuit(char c)
	{
		printableChar = c;
	}


	/**
	 * Constructor.
	 * 
	 * @param undefined
	 * @param bound
	 */
	private PokerSuit(boolean undefined, boolean bound)
	{
		this.undefined = undefined;
		this.bound = bound;
	}


	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return new String(new char[]
			{ printableChar });
	}


	/**
	 * @param suitChar
	 * @return
	 */
	public static PokerSuit fromString(String suitChar)
	{
		if (suitChar.length() != 1)
			return null;
		char c = suitChar.charAt(0);
		if (c == '_')
			return UNDEFINED;
		for (PokerSuit s : values())
			if (s.printableChar == c)
				return s;
		return BOUND;
	}


	public boolean isBound()
	{
		return bound;
	}


	public boolean isDefined()
	{
		return !undefined;
	}


	public boolean isIn(Card card)
	{
		if (undefined)
			return true;
		PokerSuit s = card.getSuit();
		if (s.undefined)
			return true;
		return this == s;
	}
}
