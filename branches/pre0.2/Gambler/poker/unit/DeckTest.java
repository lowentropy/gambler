
package poker.unit;

import poker.common.Deck;
import poker.common.PokerError;
import junit.framework.TestCase;


public class DeckTest extends TestCase
{

	public void testDeal52()
	{
		Deck deck = new Deck();
		try
		{
			for (int i = 0; i < 52; i++)
				System.out.printf("%s\n", deck.dealCard().toString());
		}
		catch (PokerError e)
		{
			e.printStackTrace();
			fail();
		}

		boolean ok = false;
		try
		{
			deck.dealCard();
		}
		catch (PokerError e)
		{
			ok = true;
		}
		assertTrue(ok);
	}
}
