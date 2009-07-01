/*
 * Card.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.ai.core;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;

import poker.ai.PokerAI;
import poker.common.PokerError;


/**
 * A Card is a mask for one of any of the 52 cards in a poker deck (no, there
 * are no Jokers). The value or suit are independently specified, and either or
 * both can be left unspecified to create a mask. In addition, either the value
 * or suit can be bound to a variable (a test, or condition).
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class Card implements Serializable
{

	/** value of poker card; may be undefined or bound as well */
	private PokerValue	value;

	/** suit of poker card; may be undefined or bound as well */
	private PokerSuit	suit;

	/** if poker value bound, name of bound variable; else null */
	private String		valueVar;

	/** if poker suit bound, name of bound variable; else null */
	private String		suitVar;
	
	
	public Card()
	{
		
	}

	
	public Card(PokerValue value, PokerSuit suit)
	{
		this(value, suit, null, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param value
	 * @param suit
	 * @param valueVar
	 * @param suitVar
	 */
	private Card(PokerValue value, PokerSuit suit, String valueVar,
			String suitVar)
	{
		this.value = value;
		this.suit = suit;
		this.valueVar = valueVar;
		this.suitVar = suitVar;
	}


	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		String v = value.isBound() ? valueVar : value.toString();
		String s = suit.isBound() ? suitVar : suit.toString();
		return v + s;
	}


	/**
	 * Form a card from a two-char string, possibly a mask, if allowed.
	 * 
	 * @param cardString
	 *            string representing card or mask
	 * @param allowMask
	 *            allow card to be a mask
	 * @return card formed from string
	 * @throws PokerError
	 */
	public static Card fromString(String cardString, boolean allowMask)
			throws PokerError
	{
		if (cardString.substring(0,2).equals("10"))
				cardString = "T" + cardString.substring(2);
		
		if (cardString.length() != 2)
			throw new PokerError("invalid card: " + cardString);
		String vs = cardString.substring(0, 1);
		String ss = cardString.substring(1);
		PokerValue v = PokerValue.fromString(vs);
		PokerSuit s = PokerSuit.fromString(ss);
		if (!allowMask && (!v.isDefined() || !s.isDefined()))
			throw new PokerError("masked or bound card not allowed: "
					+ cardString);
		return new Card(v, s, v.isBound() ? vs : null, s.isBound() ? ss : null);
	}


	public boolean isIn(Hand hand, PokerAI ai) throws PokerError
	{
		return hand.contains(this, ai);
	}


	private boolean hasBinds()
	{
		return suit.isBound() || value.isBound();
	}


	public int compareTo(Card card) throws PokerError
	{
		if (!value.isDefined() || !card.value.isDefined())
			throw new PokerError("cannot directly compare masked card values");
		return value.compare(card.value);
	}


	public boolean fullyDefined()
	{
		return suit.isDefined() && value.isDefined();
	}


	public PokerSuit getSuit()
	{
		return suit;
	}


	public PokerValue getValue()
	{
		return value;
	}


	public boolean resolves(Card card, PokerAI ai) throws PokerError
	{
		if (value.isDefined())
		{
			if (card.value.isDefined())
			{
				if (value != card.value)
					return false;
			}
			else
				ai.set(card.valueVar, Value.fromCardValue(value));
		}
		if (suit.isDefined())
		{
			if (card.suit.isDefined())
			{
				if (suit != card.suit)
					return false;
			}
			else
				ai.set(card.suitVar, Value.fromSuit(suit));
		}
		return true;
	}


	public boolean resolvesByMap(Card card, Map<String, Value> map)
	{
		if (value.isDefined())
		{
			if (card.value.isDefined())
			{
				if (value != card.value)
					return false;
			}
			else if (card.value.isBound())
				map.put(card.valueVar, Value.fromCardValue(value));
		}
		if (suit.isDefined())
		{
			if (card.suit.isDefined())
			{
				if (suit != card.suit)
					return false;
			}
			else if (card.suit.isBound())
				map.put(card.suitVar, Value.fromSuit(suit));
		}
		return true;
	}


	public Card resolve(PokerAI ai)
	{
		Card c = copy();
		if (value.isBound())
		{
			Value v = ai.lookup(valueVar);
			if ((v != null) && (v.getCardValue() != null))
				c.value = v.getCardValue();
		}
		if (suit.isBound())
		{
			Value v = ai.lookup(valueVar);
			if ((v != null) && (v.getSuit() != null))
				c.suit = v.getSuit();
		}
		return c;
	}


	public Card copy()
	{
		return new Card(value, suit, valueVar, suitVar);
	}


	public String getValueVariable()
	{
		return valueVar;
	}


	public String getSuitVariable()
	{
		return suitVar;
	}


	public boolean tryMap(Map<String, Value> map)
	{
		boolean usedMap = false;
		if (value.isBound())
		{
			Value v = map.get(valueVar);
			if ((v != null) && (v.getCardValue() != null))
			{
				value = v.getCardValue();
				usedMap = true;
			}
		}
		if (suit.isBound())
		{
			Value v = map.get(suitVar);
			if ((v != null) && (v.getSuit() != null))
			{
				suit = v.getSuit();
				usedMap = true;
			}
		}
		return usedMap;
	}


	public void unmap(boolean mv, boolean ms)
	{
		if (mv)
			value = PokerValue.BOUND;
		if (ms)
			suit = PokerSuit.BOUND;
	}


	public boolean equals(Object o)
	{
		if ((o == null) || !(o instanceof Card))
			return false;
		Card c = (Card) o;
		if (value != c.value || suit != c.suit)
			return false;
		if (value.isBound())
			if (!valueVar.equals(c.valueVar))
				return false;
		if (suit.isBound())
			if (!suitVar.equals(c.suitVar))
				return false;
		return true;
	}


	/**
	 * Return index which would give card if constructed by PokerNet, in order
	 * of rank and then suit, which is in order c,d,h,s.
	 * 
	 * 
	 * @return
	 */
	public int getIndex()
	{
		return (value.ordinal() * 4) + suit.ordinal();
	}


	public static Card fromIndex(int card)
	{
		int rank = card / 4;
		int suit = card % 4;
		return new Card(PokerValue.values()[rank], PokerSuit.values()[suit], null, null);
	}
	
	
	public void readObject(ObjectInputStream stream) throws IOException
	{
		valueVar = stream.readUTF();
		suitVar = stream.readUTF();
		value = PokerValue.values()[stream.readInt()];
		suit = PokerSuit.values()[stream.readInt()];
	}
	
	
	public void writeObject(ObjectOutputStream stream) throws IOException
	{
		stream.writeUTF(valueVar);
		stream.writeUTF(suitVar);
		stream.writeInt(value.ordinal());
		stream.writeInt(suit.ordinal());
	}
}
