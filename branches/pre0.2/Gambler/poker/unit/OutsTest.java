
package poker.unit;

import junit.framework.TestCase;
import poker.ai.core.Card;
import poker.ai.core.Hand;
import poker.common.PokerError;


public class OutsTest extends TestCase
{

	public void testHighCardOuts()
	{
		try
		{
			Hand h = new Hand("Tc", "3s", "5d");
			int c1 = card("8s");
			int c2 = card("Qc");
			h.printOuts(0, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testPairOutsInHole()
	{
		try
		{
			Hand h = new Hand("Tc", "3s", "5d");
			int c1 = card("Qs");
			int c2 = card("Qc");
			h.printOuts(1, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testPairOutsInBoth()
	{
		try
		{
			Hand h = new Hand("Tc", "3s", "5d");
			int c1 = card("Ts");
			int c2 = card("Qc");
			h.printOuts(1, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testPairOutsOnBoard()
	{
		try
		{
			Hand h = new Hand("Tc", "3s", "Td");
			int c1 = card("Qs");
			int c2 = card("9c");
			h.printOuts(1, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testTwoPairInHand()
	{
		try
		{
			Hand h = new Hand("Tc", "3s", "7h");
			int c1 = card("Ts");
			int c2 = card("7d");
			h.printOuts(2, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testTwoPairOneInHand()
	{
		try
		{
			Hand h = new Hand("Tc", "3s", "Th");
			int c1 = card("7s");
			int c2 = card("7d");
			h.printOuts(2, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testTwoPairNoneInHand()
	{
		try
		{
			Hand h = new Hand("Tc", "Ts", "5h", "5c");
			int c1 = card("7s");
			int c2 = card("6d");
			h.printOuts(2, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testSetOutsTwoInHand()
	{
		try
		{
			Hand h = new Hand("Tc", "3s", "4h");
			int c1 = card("Ts");
			int c2 = card("Td");
			h.printOuts(3, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testSetOutsOneInHand()
	{
		try
		{
			Hand h = new Hand("Tc", "3s", "Th");
			int c1 = card("Ts");
			int c2 = card("Qd");
			h.printOuts(3, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testSetOutsZeroInHand()
	{
		try
		{
			Hand h = new Hand("Tc", "Ts", "Th");
			int c1 = card("Qs");
			int c2 = card("3d");
			h.printOuts(3, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}
	
	
	public void testSetDrawOutsZeroInHand()
	{
		try
		{
			Hand h = new Hand("Tc", "Ts", "4h");
			int c1 = card("Qs");
			int c2 = card("3d");
			h.printOuts(3, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}
	
	
	public void testSetDrawOutsOneInHand()
	{
		try
		{
			Hand h = new Hand("Tc", "4s", "Qh");
			int c1 = card("Qs");
			int c2 = card("3d");
			h.printOuts(3, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}
	
	
	
	
	public void testSetDrawOutsTwoInHand()
	{
		try
		{
			Hand h = new Hand("Tc", "5s", "2h");
			int c1 = card("Qs");
			int c2 = card("Qd");
			h.printOuts(3, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testMadeStraight()
	{
		try
		{
			Hand h = new Hand("5c", "6c", "9c");
			int c1 = card("7s");
			int c2 = card("8d");
			h.printOuts(4, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testInsideStraight1()
	{
		try
		{
			/* TODO: BUG: no outs */
			Hand h = new Hand("5c", "Ac", "9c");
			int c1 = card("7s");
			int c2 = card("8d");
			h.printOuts(4, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testInsideStraight2()
	{
		try
		{
			/* TODO: BUG: no outs */
			Hand h = new Hand("5c", "6c", "9c");
			int c1 = card("As");
			int c2 = card("8d");
			h.printOuts(4, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testInsideStraight3()
	{
		try
		{
			/* TODO: BUG: no outs */
			Hand h = new Hand("5c", "6c", "9c");
			int c1 = card("7s");
			int c2 = card("Ad");
			h.printOuts(4, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testOpenStraight()
	{
		try
		{
			Hand h = new Hand("5c", "6c", "Ac");
			int c1 = card("7s");
			int c2 = card("8d");
			h.printOuts(4, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}
	
	
	public void testDoubleStraight()
	{
		try
		{
			Hand h = new Hand("5c", "9c", "Jh");
			int c1 = card("7s");
			int c2 = card("8d");
			h.printOuts(4, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}

	
	public void testDeuceOpenStraight()
	{
		try
		{
			Hand h = new Hand("2c", "3c", "4h");
			int c1 = card("5s");
			int c2 = card("Ad");
			h.printOuts(4, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}
	
	
	public void testAceOpenStraight()
	{
		try
		{
			Hand h = new Hand("Jc", "Qc", "Kh");
			int c1 = card("As");
			int c2 = card("2d");
			h.printOuts(4, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	

	public void testFlush2tocome()
	{
		try
		{
			Hand h = new Hand("5c", "9c", "Jc");
			int c1 = card("7s");
			int c2 = card("8d");
			h.printOuts(5, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testFlush1tocome()
	{
		try
		{
			Hand h = new Hand("5c", "9c", "Jc");
			int c1 = card("7s");
			int c2 = card("8c");
			h.printOuts(5, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testFlushMade()
	{
		try
		{
			Hand h = new Hand("5c", "9c", "Jc");
			int c1 = card("7c");
			int c2 = card("8c");
			h.printOuts(5, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testFullHouseSmallInHand()
	{
		try
		{
			Hand h = new Hand("Jc", "Ac", "Js");
			int c1 = card("7s");
			int c2 = card("7c");
			h.printOuts(6, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testFullHouseTwoBigInHand()
	{
		try
		{
			Hand h = new Hand("7c", "Jh", "6d");
			int c1 = card("Js");
			int c2 = card("Jc");
			h.printOuts(6, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testFullHouseOneOfEachInHand()
	{
		try
		{
			/* TODO: BUG: kickers are A,8 (wrong # outs) */
			Hand h = new Hand("7c", "Ac", "8s");
			int c1 = card("7s");
			int c2 = card("8c");
			h.printOuts(6, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testFullHouseOneOfBigInHand()
	{
		try
		{
			/* TODO: BUG: no kickers */
			Hand h = new Hand("Jc", "Ac", "Js");
			int c1 = card("Jh");
			int c2 = card("7c");
			h.printOuts(6, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testFullHouseOneOfSmallInHand()
	{
		try
		{
			Hand h = new Hand("Jc", "Jh", "Js");
			int c1 = card("7s");
			int c2 = card("2d");
			h.printOuts(6, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testFullHouseNoneInHand()
	{
		try
		{
			Hand h = new Hand("Jc", "Jh", "Js", "3c", "3d");
			int c1 = card("7s");
			int c2 = card("8c");
			h.printOuts(6, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void test4kindMade()
	{
		try
		{
			Hand h = new Hand("7c", "7h", "7d");
			int c1 = card("7s");
			int c2 = card("8c");
			h.printOuts(7, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void test4kindDraw()
	{
		try
		{
			Hand h = new Hand("7c", "7h", "Qd");
			int c1 = card("7s");
			int c2 = card("8c");
			h.printOuts(7, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testStraightFlushMade()
	{
		try
		{
			Hand h = new Hand("7c", "8c", "Tc");
			int c1 = card("9c");
			int c2 = card("Jc");
			h.printOuts(8, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testStraightFlushDraw()
	{
		try
		{
			Hand h = new Hand("7c", "8s", "Tc");
			int c1 = card("9c");
			int c2 = card("Jc");
			h.printOuts(8, c1, c2);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}

	}


	private int card(String c) throws PokerError
	{
		return Card.fromString(c, false).getIndex();
	}
}
