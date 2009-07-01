/*
 * PokerValue.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.ai.core;

import poker.common.PokerError;


public enum PokerValue
{

	TWO(2), THREE(3), FOUR(4), FIVE(5), SIX(6), SEVEN(7), EIGHT(8), NINE(9), TEN(
			'T', 10), JACK('J', 11), QUEEN('Q', 12), KING('K', 13), ACE('A', 14), UNDEFINED(
			true, false), BOUND(true, true);

	/** value of card; used to compare cards */
	private int		value			= -1;

	/** printable character for value */
	private char	printableChar	= '_';

	/** whether value is undefined */
	private boolean	undefined;

	/** whether value is bound */
	private boolean	bound;


	/**
	 * Constructor.
	 * 
	 * @param undefined
	 * @param bound
	 */
	private PokerValue(boolean undefined, boolean bound)
	{
		this.undefined = undefined;
		this.bound = bound;
	}


	/**
	 * Constructor.
	 * 
	 * @param value
	 */
	private PokerValue(int value)
	{
		this.value = value;
		printableChar = (char) (value + '0');
	}


	/**
	 * Constructor.
	 * 
	 * @param c
	 * @param value
	 */
	private PokerValue(char c, int value)
	{
		this.value = value;
		this.printableChar = c;
	}


	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return new String(new char[] {printableChar});
	}


	public static PokerValue fromString(String valueChar)
	{
		if (valueChar.length() != 1)
			return null;
		char c = valueChar.charAt(0);
		if (c == '_')
			return UNDEFINED;
		for (PokerValue v : values())
			if (v.printableChar == c)
				return v;
		return BOUND;
	}


	public int compare(PokerValue v) throws PokerError
	{
		if (undefined || v.undefined)
			throw new PokerError("cannot compare undefined poker value: "
					+ this + " to " + v);
		return value > v.value ? 1 : (value < v.value ? -1 : 0);
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
		PokerValue v = card.getValue();
		if (v.undefined)
			return true;
		return this == v;
	}


	public boolean follows(PokerValue v)
	{
		return (value == (v.value + 1));
	}
}
