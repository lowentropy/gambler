/*
 * Rule.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.ai.core;

import java.util.ArrayList;
import java.util.List;


/**
 * A Rule may contain a test, action, or both. A Rule may also contain subrules.
 * The action of the rule, if it is of type "true/false", forces all descendant
 * rules to also have actions of type "true/false" or of type "assign". In
 * addition, actions of type "assign", force their subrules (if any) to also
 * contain actions of type "true/false" or "assign".
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class Rule
{

	/** test of rule; null if rule is an unconditional assignment */
	private Test	test;

	/**
	 * action of rule; either true/false, assignment, or poker action. if null,
	 * rule is a precondition for all subrules, not excepted by them
	 */
	private Action	action;

	/**
	 * subrules; either excepting parent rule, in addition to parent rule
	 * (action is null), or excepting value of assignment.
	 */
	List<Rule>		subrules;


	/**
	 * Constructor which creates a rule which always processes its action.
	 * 
	 * @param action
	 *            action to process
	 */
	public Rule(Action action)
	{
		this.test = null;
		this.action = action;
		if (action.isAssignment())
			this.subrules = action.getAssignmentRules();
		else
			this.subrules = new ArrayList<Rule>(0);
	}


	/**
	 * Constructor.
	 * 
	 * @param test
	 *            test to perform action
	 * @param action
	 *            action to perform
	 */
	public Rule(Test test, Action action)
	{
		this.test = test;
		this.action = action;
		this.subrules = new ArrayList<Rule>(0);
	}


	/**
	 * Constructor.
	 * 
	 * @param test
	 *            test to perform action
	 * @param action
	 *            action to perform
	 * @param subrules
	 *            excepting rules
	 */
	public Rule(Test test, Action action, List<Rule> subrules)
	{
		this.test = test;
		this.action = action;
		this.subrules = subrules;
	}


	/**
	 * Constructor.
	 * 
	 * @param test
	 *            test of whether to iterate subrules
	 * @param subrules
	 *            subrules to conditionally iterate
	 */
	public Rule(Test test, List<Rule> subrules)
	{
		this.test = test;
		this.action = null;
		this.subrules = subrules;
	}


	public boolean hasTest()
	{
		return test != null;
	}


	public boolean hasAction()
	{
		return action != null;
	}


	public boolean isAssignment()
	{
		return action.isAssignment();
	}


	public List<Rule> getSubrules()
	{
		return subrules;
	}


	public Action getAction()
	{
		return action;
	}


	public Test getTest()
	{
		return test;
	}

}