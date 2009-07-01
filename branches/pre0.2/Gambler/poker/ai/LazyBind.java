/*
 * LazyBind.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.ai;

import java.util.ArrayList;
import java.util.List;

import poker.ai.core.Value;


public class LazyBind
{

	private String		variable;

	private List<Value>	values;


	public LazyBind(String variable)
	{
		this.variable = variable;
		values = new ArrayList<Value>();
	}


	public void addValue(Value v)
	{
		if (!values.contains(v))
			values.add(v);
	}


	public List<Value> getValues()
	{
		return values;
	}


	public String getVariable()
	{
		return variable;
	}


	public boolean equals(Object o)
	{
		if ((o == null) || !(o instanceof LazyBind))
			return false;
		LazyBind b = (LazyBind) o;
		if (!variable.equals(b.variable))
			return false;
		if (b.values.size() != values.size())
			return false;
		for (Value v : values)
			if (!b.values.contains(v))
				return false;
		for (Value v : b.values)
			if (!values.contains(v))
				return false;
		return true;
	}


	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("{" + variable + ": ");
		for (Value v : values)
			sb.append(v.toString() + ", ");
		if (values.size() > 1)
			sb.setLength(sb.length() - 2);
		sb.append("}");
		return sb.toString();
	}


	public int size()
	{
		return values.size();
	}
}
