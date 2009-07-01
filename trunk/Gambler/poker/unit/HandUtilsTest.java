
package poker.unit;

import poker.ai.core.Hand;
import junit.framework.TestCase;
import poker.ai.core.PokerValue;
import poker.ai.core.PokerSuit;
import poker.common.PokerError;
import poker.server.session.house.impl.PokerroomGame;
import poker.server.session.model.data.chat.ChatValue;


public class HandUtilsTest extends TestCase
{

	public void testNumOfValue()
	{
		try
		{
			Hand h = new Hand("3c", "4d", "3s", "6h", "3c");
			assertEquals(3, h.numOfValue(PokerValue.fromString("3")));
			assertEquals(0, h.numOfValue(PokerValue.fromString("A")));
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testNumOfSuit()
	{
		try
		{
			Hand h = new Hand("3c", "4s", "3s", "6h", "3c");
			assertEquals(2, h.numOfSuit(PokerSuit.fromString("c")));
			assertEquals(0, h.numOfSuit(PokerSuit.fromString("d")));
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testDominantSuit()
	{
		try
		{
			Hand h1 = new Hand("3c", "4s", "3s", "6h", "3s");
			Hand h2 = new Hand("3c", "4s", "3h", "6h", "3d");
			PokerSuit s = PokerSuit.fromString("s");
			PokerSuit h = PokerSuit.fromString("h");
			assertEquals(s, h1.dominantSuit());
			assertEquals(h, h2.dominantSuit());
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testFlushSuit()
	{
		try
		{
			Hand h = new Hand("3c", "4s", "3h", "6h", "3d");
			assertEquals(null, h.flushSuit(0));
			assertEquals(null, h.flushSuit(1));
			assertEquals(null, h.flushSuit(2));
			assertEquals(PokerSuit.fromString("h"), h.flushSuit(3));
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


	public void testMissingToStraight()
	{
		try
		{
			Hand h = new Hand("Ac", "4s", "2h", "6h", "7d");
			PokerValue[] m = h.missingToStraight(PokerValue.fromString("7"), false);
			assertEquals(2, m.length);
			assertEquals(PokerValue.fromString("5"), m[0]);
			assertEquals(PokerValue.fromString("3"), m[1]);
			h = new Hand("7c", "4s", "5h", "3h", "6d");
			m = h.missingToStraight(PokerValue.fromString("7"), false);
			assertEquals(0, m.length);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}
	}


//	public void testHandDecode()
//	{
//		try
//		{
//			// high card
//			assertEquals(null, PokerroomGame.getShowHand(new Hand("2c", "3d",
//					"4s", "5c", "6d"), ChatValue.parse("high(A)")));
//			
//			// pair: on board
//			assertEquals(null, PokerroomGame.getShowHand(new Hand("2c", "3d",
//					"4s", "6c", "6d"), ChatValue.parse("pair(6)")));
//			// pair: 1 on board
//			assertEquals(null, PokerroomGame.getShowHand(new Hand("2c", "3d",
//					"4s", "5c", "6d"), ChatValue.parse("pair(6)")));
//			// pair: in pocket
//			assertEquals(new Hand("Ac", "Ad"),PokerroomGame.getShowHand(new Hand("2c", "3d",
//					"4s", "5c", "6d"), ChatValue.parse("pair(A)")));
//			
//			// 2pair: on board
//			assertEquals(null,PokerroomGame.getShowHand(new Hand("2c", "Ad",
//					"5s", "5c", "Ad"), ChatValue.parse("twopair(A,5)")));
//			// 2pair: 1p on board
//			assertEquals(new Hand("5c", "5d"),PokerroomGame.getShowHand(new Hand("2c", "Ad",
//					"7s", "3c", "Ad"), ChatValue.parse("twopair(A,5)")));
//			// 2pair: 1p on board (tests flush avoidance)
//			assertEquals(new Hand("5d", "5h"),PokerroomGame.getShowHand(new Hand("2c", "Ac",
//					"7c", "3c", "Ad"), ChatValue.parse("twopair(A,5)")));
//			// 2pair: 1 of each on board
//			assertEquals(new Hand("5d", "Ac"),PokerroomGame.getShowHand(new Hand("2c", "3d",
//					"7s", "5c", "Ad"), ChatValue.parse("twopair(A,5)")));
//			// 2pair: 1p + 1c on board
//			assertEquals(null,PokerroomGame.getShowHand(new Hand("2c", "3d",
//					"As", "5c", "Ad"), ChatValue.parse("twopair(A,5)")));
//			
//			// set: on board
//			assertEquals(null,PokerroomGame.getShowHand(new Hand("2c", "3d",
//					"As", "Ac", "Ad"), ChatValue.parse("set(A)")));
//			// set: 1 on board
//			assertEquals(new Hand("Ad", "Ac"),PokerroomGame.getShowHand(new Hand("2c", "3d",
//					"7s", "5c", "Ad"), ChatValue.parse("set(A)")));
//			// set: 2 on board
//			assertEquals(null,PokerroomGame.getShowHand(new Hand("2c", "3d",
//					"7s", "Ac", "Ad"), ChatValue.parse("set(A)")));
//			
//			// straight: board
//			assertEquals(null,PokerroomGame.getShowHand(new Hand("2c", "3d",
//					"4s", "5c", "6d"), ChatValue.parse("straight(6)")));
//			// straight: 1 missing
//			assertEquals(null,PokerroomGame.getShowHand(new Hand("2c", "3d",
//					"4s", "5c", "Ad"), ChatValue.parse("straight(6)")));
//			// straight: 2 missing
//			assertEquals(new Hand("5c", "4d"),PokerroomGame.getShowHand(new Hand("2c", "3d",
//					"As", "Ac", "6d"), ChatValue.parse("straight(6)")));
//			
//			// flush
//			assertEquals(null,PokerroomGame.getShowHand(new Hand("2c", "3d",
//					"As", "Ac", "6d"), ChatValue.parse("flush(J)")));
//			
//			// full house: on board
//			assertEquals(null,PokerroomGame.getShowHand(new Hand("3c", "3d",
//					"As", "Ac", "Ad"), ChatValue.parse("fullhouse(A,3)")));
//			// full house: hold pair
//			assertEquals(new Hand("3c", "3d"),PokerroomGame.getShowHand(new Hand("4c", "5d",
//					"As", "Ac", "Ad"), ChatValue.parse("fullhouse(A,3)")));
//			// full house: hold two of set
//			assertEquals(new Hand("Ac", "Ad"),PokerroomGame.getShowHand(new Hand("3c", "3d",
//					"5s", "4c", "Ad"), ChatValue.parse("fullhouse(A,3)")));
//			// full house: hold one of each
//			assertEquals(new Hand("Ac","3d"),PokerroomGame.getShowHand(new Hand("3c", "4d",
//					"As", "Ac", "6d"), ChatValue.parse("fullhouse(A,3)")));
//			// full house: hold one of pair
//			assertEquals(null,PokerroomGame.getShowHand(new Hand("3c", "4d",
//					"As", "Ac", "Ad"), ChatValue.parse("fullhouse(A,3)")));
//			// full house: hold one of set
//			assertEquals(null,PokerroomGame.getShowHand(new Hand("3c", "3d",
//					"As", "Ac", "6d"), ChatValue.parse("fullhouse(A,3)")));
//			
//			// 4kind: on board
//			assertEquals(null,PokerroomGame.getShowHand(new Hand("3c", "Ad",
//					"As", "Ac", "Ad"), ChatValue.parse("four(A)")));
//			// 4kind: 3 on board
//			assertEquals(null,PokerroomGame.getShowHand(new Hand("3c", "3d",
//					"As", "Ac", "Ad"), ChatValue.parse("four(A)")));
//			// 4kind: 2 on board
//			assertEquals(new Hand("Ac","Ad"),PokerroomGame.getShowHand(new Hand("3c", "3d",
//				"As", "Ac", "6d"), ChatValue.parse("four(A)")));
//			
//			// straight flush: on board
//			assertEquals(null,PokerroomGame.getShowHand(new Hand("2c", "3c",
//					"4c", "5c", "6c"), ChatValue.parse("sflush(6)")));
//			// straight flush: 1 missing
//			assertEquals(null,PokerroomGame.getShowHand(new Hand("2c", "3c",
//					"4c", "Ad", "6c"), ChatValue.parse("sflush(6)")));
//			// straight flush: 2 missing
//			assertEquals(new Hand("6c","4c"),PokerroomGame.getShowHand(new Hand("2c", "3c",
//					"Ad", "5c", "Qd"), ChatValue.parse("sflush(6)")));
//		}
//		catch (PokerError e)
//		{
//			e.printStackTrace();
//			fail();
//		}
//	}
}
