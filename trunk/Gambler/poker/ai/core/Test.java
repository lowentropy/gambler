/*
 * Test.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.ai.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import poker.ai.PokerAI;
import poker.common.PokerError;


/**
 * A Test checks the value of a variable, assigned variable (with sub-rules, for
 * instance), relationship among bound variables, or pre-defined conditions
 * (like poker round), all of which must evaluate to true or false. In addition,
 * the test may bind an argument of an assigned condition to a varible name
 * which may be used in the subrules of the rule containing this test as a
 * prerequisite.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class Test
{

	/**
	 * if the test is a conjugate of tests (a & b, a | b, etc.), the operator
	 * (!, &, or |). Else, is null.
	 */
	private String			conjugate	= null;

	/**
	 * Left side of conjugate, or subject of NOT.
	 */
	private Test			conjLeft;

	/**
	 * Right side of conjugate.
	 */
	private Test			conjRight;

	/**
	 * left value in comparison
	 */
	private Value			valueLeft	= null;

	/**
	 * right value in comparison
	 */
	private Value			valueRight	= null;

	/**
	 * if variable is non-null, the comparison operation to apply to the
	 * variable and value
	 */
	private String			comparison;

	/**
	 * non-null name of condition to test; might have arguments. if not
	 * condition test, is null
	 */
	private String			condition	= null;

	/**
	 * List of arguments to condition test. List is always non-null, but may
	 * contain zero arguments.
	 */
	private List<String>	arglist;


	/**
	 * Private empty constructor.
	 */
	protected Test()
	{
	}


	/**
	 * Constructor.
	 * 
	 * @param condition
	 *            condition to test
	 * @param arglist
	 *            arguments to bind via assigned test
	 */
	public Test(String condition, List<String> arglist)
	{
		this.condition = condition;
		this.arglist = arglist;
	}


	/**
	 * Constructor.
	 * 
	 * @param variable
	 *            variable to compare
	 * @param comparison
	 *            comparison to perform
	 * @param value
	 *            value to compare against
	 * @throws PokerError
	 */
	public Test(Value valueLeft, String comparison, Value valueRight)
			throws PokerError
	{
		this.comparison = comparison;
		this.valueLeft = valueLeft;
		this.valueRight = valueRight;
	}


	/**
	 * Constructor.
	 * 
	 * @param conjugate
	 *            conjugator operation (either & or |)
	 * @param conjLeft
	 *            left side of operation
	 * @param conjRight
	 *            right side of operation
	 */
	public Test(String conjugate, Test conjLeft, Test conjRight)
	{
		this.conjugate = conjugate;
		this.conjLeft = conjLeft;
		this.conjRight = conjRight;
	}


	/**
	 * @return negation of this test
	 */
	public Test not()
	{
		Test neg = new Test();
		neg.conjugate = "!";
		neg.conjLeft = this;
		return neg;
	}


	public boolean evaluate(PokerAI ai, Map<String, Test> tests)
			throws PokerError
	{
		if (conjugate != null)
		{
			boolean valLeft = conjLeft.evaluate(ai, tests);

			if (conjugate.equals("!"))
				return !valLeft;

			if (conjugate.equals("&"))
				if (!valLeft)
					return false;

			if (conjugate.equals("|"))
				if (valLeft)
					return true;

			boolean valRight = conjRight.evaluate(ai, tests);

			if (conjugate.equals("&"))
				return valLeft && valRight;
			else if (conjugate.equals("|"))
				return valLeft || valRight;
			else
				throw new PokerError("invalid conjugate: " + conjugate);
		}
		else if (condition != null)
		{
			Test test = tests.get(condition);
			if (test == null)
				throw new PokerError("test not found: " + condition);
			if (test.arglist.size() != arglist.size())
				throw new PokerError("test has wrong # of args");
			List<Value> args = new ArrayList<Value>(arglist.size());
			for (String name : arglist)
				args.add(ai.lookup(name));
			if (test.call(ai, tests, args))
			{
				for (int i = 0; i < arglist.size(); i++)
				{
					Value v = args.get(i);
					if (v != null)
						ai.set(arglist.get(i), v);
				}
				return true;
			}
			return false;
		}
		else
		{
			return valueLeft.compareUsing(valueRight, comparison, ai);
		}
	}


	protected boolean call(PokerAI ai, Map<String, Test> tests, List<Value> args)
			throws PokerError
	{
		throw new PokerError("wrong kind of test! DOH! (class="
				+ getClass().toString() + ")");
	}
}
