/*
 * Action.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.ai.core;

import java.util.List;

import poker.ai.PokerAI;
import poker.common.PokerError;


/**
 * An Action is either a poker action (like betting, etc.), or it is an
 * assignment of a new condition variable, or it is TRUE/FALSE.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class Action
{

	/**
	 * this will be the non-null action to take in the poker hand; if this is
	 * not a poker action, it will be null
	 */
	private String			pokerMove;

	/**
	 * If the action is a poker action, check controls whether the player
	 * attempts to check first.
	 */
	private boolean			check		= false;

	/**
	 * If the action is a print of const, the string to print.
	 */
	private String			printString	= null;

	/**
	 * If the action is a print of var, the varname to print.
	 */
	private String			printVar	= null;

	/**
	 * Whether to stop the program (in debug mode)
	 */
	private boolean			doStop		= false;

	/**
	 * this will be the non-null name of the condition variable to assign, if
	 * this is an assignment action; it is null otherwise
	 */
	private String			testName;

	/**
	 * argument list for assignment action; never null, but might have zero
	 * entries
	 */
	private List<String>	arglist;

	/**
	 * if both testName and pokerMove are true, this is the value of the
	 * true/false action.
	 */
	private boolean			truthValue;

	private List<Rule>		subrules;


	/**
	 * Constructor.
	 * 
	 * @param truthValue
	 *            true or false, to set result of assigned condition test
	 */
	public Action(boolean truthValue)
	{
		this.pokerMove = null;
		this.testName = null;
		this.truthValue = truthValue;
	}


	/**
	 * Constructor.
	 * 
	 * @param pokerMove
	 *            poker move to act on
	 */
	public Action(String pokerMove)
	{
		this.pokerMove = pokerMove;
		this.testName = null;
	}


	/**
	 * Constructor.
	 * 
	 * @param pokerMovePrimary
	 *            primary move (must be "bet")
	 * @param pokerMoveAlt
	 *            alternate move (any normal move, if not first bet)
	 */
	public Action(String pokerMovePrimary, String pokerMoveAlt)
	{
		this.pokerMove = pokerMovePrimary + "/" + pokerMoveAlt;
		this.testName = null;
	}


	/**
	 * Constructor.
	 * 
	 * @param testName
	 *            name of defined condition (test)
	 * @param arglist
	 *            argument list of test (may be empty)
	 * @param truthValue
	 *            default truth value of test, may be excepted by containing
	 *            rules' subrules, which must have true/false or assignment
	 *            actions themselves
	 */
	public Action(String testName, List<String> arglist, boolean truthValue,
			List<Rule> subrules)
	{
		this.pokerMove = null;
		this.testName = testName;
		this.arglist = arglist;
		this.truthValue = truthValue;
		this.subrules = subrules;
	}


	private Action()
	{
	}


	public static Action makeStopAction()
	{
		Action a = new Action();
		a.doStop = true;
		return a;
	}


	public static Action makePrintAction(String printString)
	{
		Action a = new Action();
		a.printString = printString;
		return a;
	}


	public static Action makePrintVarAction(String printVar)
	{
		Action a = new Action();
		a.printVar = printVar;
		return a;
	}


	/**
	 * Set whether to check before acting.
	 * 
	 * @param check
	 *            whether player checks
	 */
	public void setCheck(boolean check)
	{
		this.check = check;
	}


	public boolean isAssignment()
	{
		return testName != null;
	}


	public String getTestName()
	{
		return testName;
	}


	public List<String> getArglist()
	{
		return arglist;
	}


	public boolean getTruthValue()
	{
		return truthValue;
	}


	public boolean isTrueOrFalse()
	{
		return pokerMove == null && testName == null;
	}


	public List<Rule> getAssignmentRules()
	{
		return subrules;
	}


	public void perform(PokerAI ai) throws PokerError
	{
		if (pokerMove != null)
		{
			ai.makeMove(pokerMove);
		}
		else if (printString != null)
		{
			ai.print(printString);
		}
		else if (printVar != null)
		{
			Value v = ai.lookup(printVar);
			if (v == null)
				throw new PokerError("variable not found: " + printVar);
			ai.print(v.toString());
		}
		else if (doStop)
		{
			ai.stop();
		}
	}


	public boolean isPrintAction()
	{
		return printString != null || printVar != null;
	}


	public boolean isExitAction()
	{
		return doStop;
	}
}
