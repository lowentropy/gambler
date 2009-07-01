/*
 * TestRule.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.ai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import poker.ai.core.Action;
import poker.ai.core.Rule;
import poker.ai.core.Test;
import poker.ai.core.Value;
import poker.common.PokerError;


public class TestRule extends Test
{

	/** rule which defines behavior of test */
	private Rule				rule;

	/** action which contains properties of test */
	private Action				action;

	/** variables which were active the last time this rule was defined */
	private Map<String, Value>	scope;


	/**
	 * Constructor.
	 * 
	 * @param rule
	 *            test rule
	 * @param name
	 */
	public TestRule(Rule rule, Map<String, Value> scope)
	{
		super(rule.getAction().getTestName(), rule.getAction().getArglist());
		this.rule = rule;
		this.action = rule.getAction();
		this.scope = scope;
	}


	/**
	 * @see poker.ai.core.Test#evaluate(poker.ai.PokerAI, java.util.Map,
	 *      java.util.Map)
	 */
	public boolean evaluate(PokerAI ai, Map<String, Test> tests)
			throws PokerError
	{
		throw new PokerError("Wrong kind of test! DOH!");
	}


	protected boolean call(PokerAI ai, Map<String, Test> tests, List<Value> args)
			throws PokerError
	{
		// load a completely alternate scope, with shadows
		ai.loadTest(scope);
		ai.pushScope();

		// load arguments into middle scope by param name
		List<String> names = action.getArglist();
		for (int i = 0; i < names.size(); i++)
			if (args.get(i) != null)
				ai.set(names.get(i), args.get(i));

		// set up scope to catch written vars
		ai.pushScope();

		Map<String, Value> passBack = new HashMap<String, Value>();
		for (String s : names)
			passBack.put(s, null);

		// do normal ai routine in alternate scope
		boolean b = ai.getTruth(rule.getSubrules(), passBack) ? ai.truthValue()
				: action.getTruthValue();

		// restore AI scope
		ai.popScope();
		ai.popScope();
		ai.unloadTest();

		// by default, do not pass back any values
		for (int i = 0; i < args.size(); i++)
			args.set(i, null);

		// set pass-backs
		int idx;
		if (b)
		{
			for (String pbName : passBack.keySet())
				if ((idx = names.indexOf(pbName)) != -1)
					if (passBack.get(pbName) != null)
						args.set(idx, passBack.get(pbName));
		}
		return b;
	}
}
