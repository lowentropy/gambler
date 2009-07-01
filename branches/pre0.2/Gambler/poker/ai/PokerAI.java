/*
 * PokerAI.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.ai;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import poker.ai.core.Action;
import poker.ai.core.Program;
import poker.ai.core.Rule;
import poker.ai.core.Test;
import poker.ai.core.Value;
import poker.common.PokerError;
import poker.server.base.Move;


/**
 * The PokerAI class contains code to apply a combination of a coded knowledge
 * base on poker strategy with configurable parameters pertaining to the playing
 * of poker. Based on a simple strategy with a limited number of visual inputs,
 * the Poker AI is able to determine whether to bet, whether to fold, call, or
 * whether to check.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class PokerAI
{

	private boolean								truthValue;

	private Map<String, Value>					vars;

	private Stack<Map<String, Value>>			scope;

	private Stack<Stack<Map<String, Value>>>	tscopes;

	private Map<String, Test>					tests;

	private PrintStream							os			= System.out;

	private boolean								cmdStop;

	private Map<String, Value>					passback	= null;

	private String								pokerMove;


	public PokerAI()
	{
		vars = new HashMap<String, Value>();
		scope = new Stack<Map<String, Value>>();
		tscopes = new Stack<Stack<Map<String, Value>>>();
		tests = new HashMap<String, Test>();
	}


	public void run(Program prog) throws PokerError
	{
		cmdStop = false;
		while (!cmdStop)
			loop(prog);
	}


	public Move getMove(Program prog) throws PokerError
	{
		pokerMove = null;
		while (pokerMove == null)
			loop(prog);
		return Move.parse(pokerMove);
	}


	public void loop(Program prog) throws PokerError
	{
		Action a = getAction(prog.getRules());
		if (a != null)
			a.perform(this);
	}


	private Action getAction(List<Rule> rules) throws PokerError
	{
		Action toPerform = null;

		for (Rule r : rules)
		{
			if (r.hasTest() && !evaluate(r))
				continue;
			if (!r.hasAction())
				if ((toPerform = getAction(r.getSubrules())) != null)
				{
					if (r.hasTest())
						popScope();
					break;
				}
				else
					;
			else if (r.isAssignment())
				doAssignment(r);
			else if (r.getAction().isPrintAction())
				r.getAction().perform(this);
			else if (r.getAction().isExitAction())
				r.getAction().perform(this);
			else
			{
				toPerform = (toPerform = getAction(r.getSubrules())) != null ? toPerform
						: r.getAction();
				if (r.hasTest())
					popScope();
				break;
			}
			if (r.hasTest())
				popScope();
		}
		return toPerform;
	}


	public boolean getTruth(List<Rule> rules, Map<String, Value> passBack)
			throws PokerError
	{
		setPassBackMap(passBack);
		boolean b = getTruth(rules);
		setPassBackMap(null);
		return b;
	}


	private void setPassBackMap(Map<String, Value> passBack)
	{
		this.passback = passBack;
	}


	public boolean getTruth(List<Rule> rules) throws PokerError
	{
		for (Rule r : rules)
		{
			if (r.hasTest() && !evaluate(r))
				continue;
			if (!r.hasAction())
				if (getTruth(r.getSubrules()))
				{
					if (r.hasTest())
						popScope();
					return true;
				}
				else
					;
			else if (r.isAssignment())
				doAssignment(r);
			else if (r.getAction().isPrintAction())
				r.getAction().perform(this);
			else if (!r.getAction().isTrueOrFalse())
				throw new PokerError("invalid assignment action");
			else
			{
				if (getTruth(r.getSubrules()))
				{
					if (r.hasTest())
						popScope();
					return true;
				}
				truthValue = r.getAction().getTruthValue();
				if (r.hasTest())
					popScope();
				return true;
			}
			if (r.hasTest())
				popScope();
		}
		return false;
	}


	private void doAssignment(Rule r)
	{
		tests.put(r.getAction().getTestName(), new TestRule(r, copyVars()));
	}


	/**
	 * Evaluate must leave variable stack unaltered if returning false, else it
	 * must push the stack exactly once.
	 * 
	 * @param r
	 * @return
	 * @throws PokerError
	 */
	private boolean evaluate(Rule r) throws PokerError
	{
		pushScope();
		boolean b = r.getTest().evaluate(this, tests);
		if (!b)
			popScope();
		return b;
	}


	public boolean truthValue()
	{
		return truthValue;
	}


	public Map<String, Value> copyVars()
	{
		return new HashMap<String, Value>(vars);
	}


	public void pushScope()
	{
		scope.push(vars);
		vars = new HashMap<String, Value>();
	}


	public void popScope()
	{
		vars = scope.pop();
	}


	public Value lookup(String var)
	{
		Value v;
		if ((v = vars.get(var)) != null)
			return v;
		for (int i = scope.size() - 1; i >= 0; i--)
			if ((v = scope.get(i).get(var)) != null)
				return v;
		return null;
	}


	public void set(String name, Value val) throws PokerError
	{
		vars.put(name, val);
		if (val == null)
			throw new PokerError("variable " + name + " cannot be null");
		if (passback != null && passback.containsKey(name))
			passback.put(name, val);
	}


	public void unloadTest()
	{
		scope = tscopes.pop();
		vars = scope.pop();
	}


	public void loadTest(Map<String, Value> newScope)
	{
		scope.push(vars);
		tscopes.push(scope);
		scope = new Stack<Map<String, Value>>();
		vars = newScope;
	}


	public void setOutputStream(PrintStream s)
	{
		this.os = s;
	}


	public void makeMove(String pokerMove)
	{
		this.pokerMove = pokerMove;
	}


	public void print(String printString)
	{
		if (this.os != null)
			this.os.print(printString + "\n");
	}


	public void stop()
	{
		this.cmdStop = true;
	}


	public boolean continuePlaying(TableStats stats)
	{
		// TODO Auto-generated method stub
		return false;
	}


	public void clear()
	{
		// TODO Auto-generated method stub

	}


	public void insertTest(String name, boolean result)
	{
		// TODO Auto-generated method stub

	}


	public void insertValue(String name, Value value)
	{
		// TODO Auto-generated method stub

	}


	public String chat(String user, String text)
	{
		// TODO Auto-generated method stub
		return null;
	}
}
