/*
 * Value.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.ai.core;

import java.util.ArrayList;
import java.util.List;

import poker.ai.LazyBind;
import poker.ai.PokerAI;
import poker.common.Money;
import poker.common.PokerError;


/**
 * A Value is something against which a variable can be compared. These are
 * either card suits or values, currency values, entire hands, single cards, or
 * variables which have been bound to one of these things.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class Value
{

	/** poker hand */
	private Hand		hand		= null;

	/** single card value, maybe masked */
	private Card		card		= null;

	/** suit of card, not masked */
	private PokerSuit	suit		= null;

	/** value of card, not masked */
	private PokerValue	value		= null;

	/** name of variable to compare against */
	private String		variable	= null;

	/** dollar portion of currency amount */
	private int			dollars;

	/** cents portion of currency amount */
	private int			cents;

	/** lazy bind from partial hand match */
	private LazyBind	bind		= null;


	/**
	 * @param suit
	 *            suit to use value of
	 * @return value formed from card suit
	 */
	public static Value fromSuit(PokerSuit suit)
	{
		Value v = new Value();
		v.suit = suit;
		return v;
	}


	/**
	 * @param variable
	 *            variable to use value of
	 * @return value formed from variable
	 */
	public static Value fromVariable(String variable)
	{
		Value v = new Value();
		v.variable = variable;
		return v;
	}


	/**
	 * @return value formed from a variable name, or a single-char representing
	 *         a card suit
	 */
	public static Value fromAtom(String s)
	{
		if (s.length() == 0)
			return null;
		else if (s.length() == 1)
		{
			if (s.equals("K") || s.equals("Q") || s.equals("J")
					|| s.equals("A"))
				return Value.fromCardValue(s);
			else if (s.equals("c") || s.equals("d") || s.equals("h")
					|| s.equals("s"))
				return Value.fromSuit(PokerSuit.fromString(s));
			else
				return Value.fromVariable(s);
		}
		else
			return Value.fromVariable(s);
	}


	/**
	 * @param cardString
	 *            card to use value of
	 * @return value formed from card
	 * @throws PokerError
	 */
	public static Value fromCard(String cardString) throws PokerError
	{
		Value v = new Value();
		v.card = Card.fromString(cardString, true);
		return v;
	}


	/**
	 * @param hand
	 *            hand to use value of
	 * @return value formed from hand
	 */
	public static Value fromHand(Hand hand)
	{
		Value v = new Value();
		v.hand = hand;
		return v;
	}


	/**
	 * @param currencyString
	 *            currency amount to get value of
	 * @return value formed from currency amount
	 */
	public static Value fromCurrency(String currencyString)
	{
		int idx = currencyString.indexOf('.');
		int dollars = Integer.parseInt(currencyString.substring(0, idx));
		int cents = Integer.parseInt(currencyString.substring(idx + 1));
		Value v = new Value();
		v.dollars = dollars;
		v.cents = cents;
		return v;
	}


	public static Value fromCurrency(Money pot)
	{
		Value v = new Value();
		v.dollars = pot.dollars;
		v.cents = pot.cents;
		return v;
	}


	/**
	 * @param cardValue
	 *            card value to get value of
	 * @return value formed from card value
	 */
	public static Value fromCardValue(String cardValue)
	{
		Value v = new Value();
		v.value = PokerValue.fromString(cardValue);
		return v;
	}


	/**
	 * @param cardValue
	 *            card value to get value of
	 * @return value formed from card value
	 */
	public static Value fromCardValue(PokerValue value)
	{
		Value v = new Value();
		v.value = value;
		return v;
	}


	public static Value fromLazyBind(LazyBind bind)
	{
		Value v = new Value();
		v.bind = bind;
		return v;
	}


	/**
	 * Compare values against each other, possibly binding a card variable or
	 * something.
	 * 
	 * @param other
	 * @param op
	 * @param vars
	 * @return
	 * @throws PokerError
	 */
	public boolean compareUsing(Value other, String op, PokerAI ai)
			throws PokerError
	{
		// TODO: need to come up with a better single-char system; the
		// combination of digits, variable-chars, poker-values and suits is
		// ridiculous
		
		if (other.variable != null)
		{
			String var = other.variable;
			other = ai.lookup(var);
			if (other == null)
				throw new PokerError("variable not found: " + var);
		}
		if (other.bind != null)
		{
			return compareToLazyBind(this, other.bind, true, op, ai);
		}
		else if (bind != null)
		{
			return compareToLazyBind(other, bind, false, op, ai);
		}
		else if (hand != null)
		{
			if (op.equals("in"))
				return other.hand.contains(hand, ai);
		}
		else if (card != null)
		{
			if (op.equals("in"))
				return card.isIn(other.hand, ai);
			int c = card.compareTo(other.card);
			if (op.equals("="))
				return c == 0;
			else if (op.equals("!="))
				return c != 0;
			else if (op.equals("<"))
				return c < 0;
			else if (op.equals(">"))
				return c > 0;
			else if (op.equals("<="))
				return c <= 0;
			else if (op.equals(">="))
				return c >= 0;
			throw new PokerError("invalid operator for type: " + op);
		}
		else if (suit != null)
		{
			if (op.equals("="))
				return suit == other.suit;
			else if (op.equals("!="))
				return suit != other.suit;
			else if (op.equals("in"))
				return suit.isIn(other.card);
			throw new PokerError("invalid operator for type: " + op);
		}
		else if (value != null)
		{
			if (op.equals("in"))
				return value.isIn(other.card);
			int c = value.compare(other.value);
			if (op.equals("="))
				return c == 0;
			else if (op.equals("!="))
				return c != 0;
			else if (op.equals("<"))
				return c < 0;
			else if (op.equals(">"))
				return c > 0;
			else if (op.equals("<="))
				return c <= 0;
			else if (op.equals(">="))
				return c >= 0;
			else if (op.equals("follows"))
				return value.follows(other.value);
			throw new PokerError("invalid operator for type: " + op);
		}
		else if (variable != null)
		{
			Value var = ai.lookup(variable);
			if (var == null)
				throw new PokerError("variable not found: " + variable);
			return var.compareUsing(other, op, ai);
		}
		else
		{
			int od = other.dollars, oc = other.cents;
			int c = dollars > od ? 1 : (dollars < od ? -1 : 0);
			if (c == 0)
				c = cents > oc ? 1 : (cents < oc ? -1 : 0);
			if (op.equals("="))
				return c == 0;
			else if (op.equals("!="))
				return c != 0;
			else if (op.equals("<"))
				return c < 0;
			else if (op.equals(">"))
				return c > 0;
			else if (op.equals("<="))
				return c <= 0;
			else if (op.equals(">="))
				return c >= 0;
			throw new PokerError("invalid operator for type: " + op);
		}
		throw new PokerError("invalid comparison value: " + this);
	}


	private static boolean compareToLazyBind(Value val, LazyBind bind,
			boolean valLeft, String op, PokerAI ai) throws PokerError
	{
		if (val.bind != null)
			throw new PokerError("cannot compare two lazy binds");
		List<Value> subset = new ArrayList<Value>();
		for (Value v : bind.getValues())
			if (valLeft ? val.compareUsing(v, op, ai) : v.compareUsing(val, op,
					ai))
				subset.add(v);
		if (subset.size() == 0)
			return false;
		else if (subset.size() == 1)
			ai.set(bind.getVariable(), subset.get(0));
		else
		{
			LazyBind newBind = new LazyBind(bind.getVariable());
			for (Value v : subset)
				newBind.addValue(v);
			ai.set(bind.getVariable(), Value.fromLazyBind(newBind));
		}
		return true;
	}


	public PokerValue getCardValue()
	{
		return value;
	}


	public PokerSuit getSuit()
	{
		return suit;
	}


	public String toString()
	{
		if (bind != null)
			return bind.toString();
		else if (hand != null)
			return hand.toString();
		else if (card != null)
			return card.toString();
		else if (value != null)
			return value.toString();
		else if (suit != null)
			return suit.toString();
		else if (variable != null)
			return variable;
		else
			return "$" + dollars + "." + cents;
	}


	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o)
	{
		if ((o == null) || !(o instanceof Value))
			return false;
		Value v = (Value) o;
		if (bind != null)
			if (v.bind == null)
				return false;
			else
				return bind.equals(v.bind);
		else if (hand != null)
			if (v.hand == null)
				return false;
			else
				return hand.equals(v.hand);
		else if (card != null)
			if (v.card == null)
				return false;
			else
				return card.equals(v.card);
		else if (value != null)
			return value == v.value;
		else if (suit != null)
			return suit == v.suit;
		else if (variable != null)
			if (v.variable == null)
				return false;
			else
				return variable.equals(v.variable);
		else
			return (dollars == v.dollars) && (cents == v.cents);
	}


	public LazyBind getLazyBind()
	{
		return bind;
	}

}
