/*
 * Program.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.ai.core;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import poker.ai.rules.PreProc;
import poker.ai.rules.parser;


/**
 * A Poker Program defines a complete strategy for playing poker. At least one
 * of the toplevel rules of the program must be guaranteed to fire with a
 * non-assignment action, each time an action is requried of the poker player.
 * The program can also contain top-level actions without conditions, including
 * assignments and simple actions (for instance, a poker-program could be the
 * single line "bet/raise").
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class Program
{

	/**
	 * all toplevel rules of poker program; rules with null tests are
	 * always-true actions
	 */
	List<Rule>	rules;


	/**
	 * Constructor.
	 * 
	 * @param firstRule
	 *            first of program's rules.
	 */
	public Program(Rule firstRule)
	{
		rules = new ArrayList<Rule>();
		rules.add(firstRule);
	}


	/**
	 * Add a rule.
	 * 
	 * @param rule
	 *            rule to add
	 */
	public void addRule(Rule rule)
	{
		rules.add(rule);
	}


	/**
	 * Load a poker program from the given file.
	 * 
	 * @param fname
	 *            file name
	 * @return poker program
	 * @throws Exception
	 */
	public static Program load(String fname) throws Exception
	{
		String ppFname = fname + ".pp";
		PreProc.process(fname, ppFname);
		FileReader reader = new FileReader(ppFname);
		parser p = parser.parseMain(fname, reader);
		return (Program) p.result;
	}


	/**
	 * Print the program in text form.
	 */
	public void print()
	{
		// TODO Auto-generated method stub
	}


	public List<Rule> getRules()
	{
		return this.rules;
	}
}