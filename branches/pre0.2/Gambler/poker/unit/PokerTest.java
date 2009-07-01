/*
 * PokerTest.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.unit;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import poker.ai.LazyBind;
import poker.ai.PokerAI;
import poker.ai.core.Hand;
import poker.ai.core.PokerValue;
import poker.ai.core.Program;
import poker.ai.core.Value;
import poker.ai.rules.Lexer;
import poker.common.PokerError;


public class PokerTest extends TestCase
{

	private static final boolean	doDebug	= false;


	public void testCreateHand()
	{
		if (doDebug)
			System.out.println("\ntestCreateHand():\n");
		try
		{
			Hand boat = new Hand("3c", "3h", "Js", "Jd", "Jc");
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


/*	public void testPairInBoat()
	{
		if (doDebug)
			System.out.println("\ntestPairInBoat():\n");

		try
		{
			PokerAI ai = new PokerAI();
			Hand boat = new Hand("3c", "3h", "Js", "Kd", "Qc");
			Hand pair = new Hand("N_", "N_");
			if (doDebug)
				boat.setDebugOutputStream(System.out);

			assertEquals(true, boat.contains(pair, ai));
			Map<String, Value> map = ai.copyVars();

			assertEquals(1, map.size());
			assertValues(map, "N", PokerValue.THREE);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testTwoPairInBoat()
	{
		if (doDebug)
			System.out.println("\ntestTwoPairInBoat():\n");
		try
		{
			PokerAI ai = new PokerAI();
			Hand boat = new Hand("3c", "3h", "Js", "Jd", "Jc");
			Hand pair = new Hand("N_", "N_", "M_", "M_");
			if (doDebug)
				boat.setDebugOutputStream(System.out);

			assertEquals(true, boat.contains(pair, ai));
			Map<String, Value> map = ai.copyVars();

			assertEquals(2, map.size());
			assertLazyBinds(map, "N", new PokerValue[]
				{ PokerValue.THREE, PokerValue.JACK }, "M", new PokerValue[]
				{ PokerValue.THREE, PokerValue.JACK });
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testBoatDecomp()
	{
		if (doDebug)
			System.out.println("\ntestBoatDecomp():\n");
		try
		{
			PokerAI ai = new PokerAI();
			Hand boat = new Hand("3c", "3h", "Js", "Jd", "Jc");
			Hand pair = new Hand("N_", "N_", "M_", "M_", "M_");
			if (doDebug)
				boat.setDebugOutputStream(System.out);

			assertEquals(true, boat.contains(pair, ai));
			Map<String, Value> map = ai.copyVars();

			assertEquals(2, map.size());
			assertValues(map, "N", PokerValue.THREE, "M", PokerValue.JACK);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testScript_print()
	{
		doTestScript("print", "hello, world!\n");
	}


	public void testScript_printVar()
	{
		doTestScript("print-var", "K\nd\n");
	}


	public void testScript_pair()
	{
		doTestScript("pair", "4\n");
	}


	public void testScript_eitherPair()
	{
		doTestScript("either-pair", "{N: 4, K}\n");
	}


	public void testScript_twoPairX()
	{
		doTestScript("two-pair-x", "{N: 3, 5}\n{M: 5, 3}\n7\n");
	}


	public void testScript_scopeSafety()
	{
		doTestScript("scope-safety", "3\n5\n");
	}


	public void testScript_lazyBind()
	{
		doTestScript("lazy-bind", "{N: 3, 5}\n");
	}


	public void testScript_lazyBindSpec()
	{
		doTestScript("lazy-bind-spec", "{N: 3, 7}\n7\n3\n{N: 3, 7}\n");
	}


	public void testScript_alias()
	{
		doTestScript("alias", "foo\nbaz\n");
	}


	public void testScript_printMethod()
	{
		doTestScript("print-method", "3\nXXX\n");
	}


	public void testScript_copyOut()
	{
		doTestScript("copy-out", "3\n");
	}


	public void testScript_constOut()
	{
		doTestScript("const-out", "{N: 3, K}\n");
	}


	public void testScript_invocIndep()
	{
		doTestScript("invoc-indep", "A\nB\n");
	}


	public void testScript_follows()
	{
		doTestScript("follows", "5\n6\n");
	}


	public void testScript_hand_boat()
	{
		doTestScript("hand-boat", "start\nA\nend\n", "Ac,As,Jh,Jd,Jc",
				"Ac,As,Jh,Jd,Kc");
	}


	public void testScript_hand_flush()
	{
		doTestScript("hand-flush", "start\nA\nend\n", "4c,6c,9c,Ac,Kc",
				"3h,5d,7s,Ac,Kc");
	}


	public void testScript_hand_straight()
	{
		String[] lines = new String[]
			{ "start", "h1 is straight", "h2 is straight", "h3 is straight",
					"h4 is NOT straight", "h1 is NOT high", "h2 is high",
					"h3 is high", "h4 is NOT high", "h1 is NOT suited",
					"h2 is NOT suited", "h3 is suited", "h4 is NOT suited",
					"end" };

		String expect = "";
		for (String s : lines)
			expect += s + "\n";

		try
		{
			Map<String, Hand> hmap = new HashMap<String, Hand>();
			hmap.put("P1", new Hand("7s", "8c"));
			hmap.put("P2", new Hand("10s", "Jc"));
			hmap.put("P3", new Hand("10c", "Jc"));
			hmap.put("P4", new Hand("6c", "7c"));
			hmap.put("T1", new Hand("9d", "10h", "Jc"));
			hmap.put("T2", new Hand("Qd", "Kh", "Ac"));
			hmap.put("T3", new Hand("Qc", "Kc", "Ac"));
			hmap.put("T4", new Hand("9c", "10c", "Ac"));

			doTestScript("hand-straight", expect, hmap);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testScript_hand_straight_draw()
	{
		String[] lines = new String[]
			{ "start", "h1 is straight draw", "h2 is straight draw ",
					"h3 is straighdrawt", "h4 is NOT straight draw",
					"h1 is NOT high", "h2 is high", "h3 is high",
					"h4 is NOT high", "h1 is NOT suited", "h2 is NOT suited",
					"h3 is suited", "h4 is NOT suited", "end" };

		String expect = "";
		for (String s : lines)
			expect += s + "\n";

		try
		{
			Map<String, Hand> hmap = new HashMap<String, Hand>();
			hmap.put("P1", new Hand("7s", "8c"));
			hmap.put("P2", new Hand("10s", "Jc"));
			hmap.put("P3", new Hand("10c", "Jc"));
			hmap.put("P4", new Hand("6c", "7c"));
			hmap.put("T1", new Hand("9d", "10h"));
			hmap.put("T2", new Hand("Qd", "Kh"));
			hmap.put("T3", new Hand("Qc", "Kc"));
			hmap.put("T4", new Hand("9c", "10c"));

			doTestScript("hand-straight-draw", expect, hmap);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	private void doTestScript(String name, String exp, Map<String, Hand> hmap)
	{
		PipedInputStream pis = new PipedInputStream();
		PipedOutputStream pos = new PipedOutputStream();
		try
		{
			pis.connect(pos);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			fail();
		}

		PrintStream s = new PrintStream(pos);
		InputStreamReader r = new InputStreamReader(pis);

		PokerAI ai = new PokerAI();
		ai.setOutputStream(s);

		Program p = null;
		String fname = "tests/test-" + name + ".pkr";

		try
		{
			p = Program.load(fname);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
		}
		catch (Error e)
		{
			System.out.println(Lexer.getLocation());
			System.out.flush();
			e.printStackTrace();
			fail();
		}

		try
		{
			for (String key : hmap.keySet())
				ai.set(key, Value.fromHand(hmap.get(key)));
			ai.run(p);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}

		expect(r, exp);

		try
		{
			r.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			fail();
		}
		s.close();
	}


	private void doTestScript(String name, String exp, String... handStrs)
	{
		Map<String, Hand> hmap = new HashMap<String, Hand>();
		for (int i = 0; i < handStrs.length; i++)
			try
			{
				hmap.put("HAND" + (i + 1), new Hand(handStrs[i].split(",")));
			}
			catch (PokerError e)
			{
				e.printStackTrace();
				fail();
			}
		doTestScript(name, exp, hmap);
	}


	private void expect(InputStreamReader r, String s)
	{
		char[] buf = new char[s.length()];
		try
		{
			r.read(buf);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			fail();
		}
		assertEquals(s, new String(buf));
	}


	private void assertLazyBinds(Map<String, Value> map, Object... args)
	{
		assertEquals(0, args.length % 2);
		for (int i = 0; i < args.length; i += 2)
		{
			String var = (String) args[i + 0];
			PokerValue[] vals = (PokerValue[]) args[i + 1];

			Value v = map.get(var);
			assertNotNull(v);

			LazyBind bind = v.getLazyBind();
			assertNotNull(bind);

			LazyBind cbnd = new LazyBind(var);
			for (PokerValue val : vals)
				cbnd.addValue(Value.fromCardValue(val));

			assertEquals(cbnd, bind);
		}
	}


	private void assertValues(Map<String, Value> map, Object... args)
	{
		assertEquals(0, args.length % 2);
		for (int i = 0; i < args.length; i += 2)
		{
			String var = (String) args[i + 0];
			PokerValue val = (PokerValue) args[i + 1];

			Value v = map.get(var);
			assertNotNull(v);

			PokerValue tval = v.getCardValue();
			assertNotNull(tval);

			assertEquals(val, tval);
		}
	}
*/}
