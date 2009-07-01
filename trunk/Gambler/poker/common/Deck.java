
package poker.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import bayes.Distribution;

import poker.ai.core.Card;
import poker.ai.core.Hand;


/**
 * Standard deck of 52 cards. This class also contains a variety of forms of
 * these cards; names of hole pairs, with and without specific suits, some
 * utility methods for converting between index and object forms, and the
 * ability to deal cards randomly.
 * 
 * @author lowentropy
 * 
 */
public class Deck
{

	/** random number generator */
	private Random	random;

	/** prev pointers for each card */
	private int[]	prev;

	/** next pointers for each card */
	private int[]	next;

	/** size of deck (number of cards left) */
	private int		size;

	/** index of first undealt card */
	private int		start;

	/** whether each card has been dealt */
	private boolean[]	dealt;


	/**
	 * Constructor.
	 */
	public Deck()
	{
		random = new Random(System.currentTimeMillis());
		prev = new int[52];
		next = new int[52];
		dealt = new boolean[52];
		reset();
	}


	/**
	 * Reset the deck to an undealt state.
	 */
	public void reset()
	{
		start = 0;
		size = 52;
		prev[0] = next[51] = -1;
		next[0] = 1;
		prev[51] = 50;
		for (int i = 1; i < 51; i++)
		{
			next[i] = i + 1;
			prev[i] = i - 1;
		}
		for (int i = 0; i < 52; i++)
			dealt[i] = false;
	}


	/**
	 * Deal 'num' cards out of the deck.
	 * 
	 * @param num
	 *            number of cards to deal
	 * @return cards dealt, in a hand
	 * @throws PokerError
	 */
	public Hand deal(int num) throws PokerError
	{
		if (size < num)
			throw new PokerError("not enough cards in deck");

		Card[] cards = new Card[num];
		for (int i = 0; i < num; i++)
			cards[i] = dealCard();

		Hand hand = new Hand(cards);
		return hand;
	}


	/**
	 * Deal one card out of the deck.
	 * 
	 * @return card dealt
	 * @throws PokerError
	 */
	public Card dealCard() throws PokerError
	{
		if (size == 0)
			throw new PokerError("no cards left in deck");

		int ord = random.nextInt(size);
		int idx = getIndex(ord);
		remove(idx);

		return Card.fromIndex(idx);
	}


	/**
	 * Return the index into the cards of the ord'th card in the undealt list.
	 * 
	 * @param ord
	 *            ordinal card number
	 * @return index of card
	 */
	private int getIndex(int ord)
	{
		int idx = start;
		for (int i = 0; i < ord; i++)
			idx = next[idx];
		return idx;
	}


	/**
	 * Remove the card at the given index from the undealt list, and decrement
	 * the deck size.
	 * 
	 * @param idx
	 *            index of card
	 */
	private void remove(int idx)
	{
		if (start == idx)
			start = next[idx];
		else
			next[prev[idx]] = next[idx];
		if (next[idx] != -1)
			prev[next[idx]] = prev[idx];
		
		dealt[idx] = true;
		size--;
	}


	public void extractHand(Hand b)
	{
		for (Card c : b.getCards())
			extractCard(c);
	}


	public void extractCard(Card c)
	{
		int idx = c.getIndex();
		if (dealt[idx])
			return;
		remove(idx);
	}
	
	public int size()
	{
		return size;
	}
	
	public boolean dealt(int idx)
	{
		return dealt[idx];
	}

	public Card getCard(int idx)
	{
		return Card.fromIndex(idx);
	}
	
	public List<Card> getUndealt()
	{
		List<Card> list = new ArrayList<Card>(size());
		int idx = start;
		while (idx != -1)
		{
			list.add(Card.fromIndex(idx));
			idx = next[idx];
		}
		return list;
	}
}
