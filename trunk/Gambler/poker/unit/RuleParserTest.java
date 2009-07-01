/*
 * RuleParserTest.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.unit;

import junit.framework.TestCase;
import poker.ai.core.Program;
import poker.ai.rules.Lexer;


public class RuleParserTest extends TestCase
{

	public void testParseRules()
	{
		try
		{
			Program.load("lang/pokerroom.pkr").print();
		}
		catch (Exception e)
		{
			System.out.println(Lexer.getLocation());
			e.printStackTrace();
			fail();
		}
		catch (Error e)
		{
			System.out.println(Lexer.getLocation());
			e.printStackTrace();
			fail();
		}
	}


	public static void main(String[] args)
	{
	}

}
