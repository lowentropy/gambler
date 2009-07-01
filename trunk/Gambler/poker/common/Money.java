/*
 * Money.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.common;

import java.awt.BorderLayout;
import java.io.Serializable;


public class Money implements Serializable
{

	/** serial uid */
	private static final long	serialVersionUID	= 1428882485047951697L;

	public int					dollars;

	public int					cents;


	public Money(int dollars, int cents)
	{
		this.dollars = dollars;
		this.cents = cents;
	}


	public Money(String desc)
	{
		int idx0 = (desc.charAt(0) == '$') ? 1 : 0;
		int idx1 = desc.indexOf('.');
		if (idx1 == -1)
		{
			dollars = Integer.parseInt(desc.substring(idx0));
			cents = 0;
		}
		else
		{
			dollars = Integer.parseInt(desc.substring(idx0, idx1));
			cents = Integer.parseInt(desc.substring(idx1 + 1));
		}
	}


	public Money(Money m)
	{
		setTo(m);
	}


	public Money neg()
	{
		return new Money(-dollars, -cents);
	}


	public Money subtract(Money m)
	{
		int d = dollars - m.dollars;
		int c = cents - m.cents;
		fix();
		return new Money(d, c);
	}


	private synchronized void fix()
	{
		while (cents < -99)
		{
			dollars -= 1;
			cents += 100;
		}
		while (cents > 99)
		{
			dollars += 1;
			cents -= 100;
		}
	}


	public synchronized void addIn(Money amount)
	{
		dollars += amount.dollars;
		cents += amount.cents;
		fix();
	}


	public int inCents()
	{
		return (dollars * 100) + cents;
	}


	public boolean equals(Object o)
	{
		if ((o == null) || !(o instanceof Money))
			return false;
		Money m = (Money) o;
		return (dollars == m.dollars) && (cents == m.cents);
	}


	public synchronized void setTo(Money m)
	{
		dollars = m.dollars;
		cents = m.cents;
	}


	public boolean moreThan(Money m)
	{
		if (dollars > m.dollars)
			return true;
		else if (dollars < m.dollars)
			return false;
		else
			return (cents > m.cents);
	}


	public boolean isZero()
	{
		return (dollars == 0) && (cents == 0);
	}


	public Money divideBy(int s)
	{
		Money m = new Money(0, inCents() / s);
		m.fix();
		return m;
	}


	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		int d = (dollars < 0) ? -dollars : dollars;
		String s = (dollars < 0) ? "-$" : "$";
		s += d;
		if (cents != 0)
		{
			s += ".";
			if (cents >= 10)
				s += cents;
			else
				s += "0" + cents;
		}
		return s;
	}


	public void zero()
	{
		dollars = cents = 0;
	}


	public double getQuotient(Money m)
	{
		double m1 = (double) dollars + ((double) cents) / 100.0;
		double m2 = (double) m.dollars + ((double) m.cents) / 100.0;
		return m1 / m2;
	}


	public static Money parse(String value)
	{
		return new Money(value);
	}


	public Money multiplyBy(int s)
	{
		Money m = new Money(dollars * 2, cents * 2);
		m.fix();
		return m;
	}


	public double toDouble()
	{
		return (double) dollars + ((double) cents / 100.0);
	}
}
