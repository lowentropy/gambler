/*
 * Hand.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.ai.core;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bayes.Distribution;

import poker.ai.LazyBind;
import poker.ai.PokerAI;
import poker.ai.bnet.PokerNet;
import poker.common.PokerError;


/**
 * A Poker hand is either a well-defined hand (with definite card values) or it
 * is a mask of poker cards, with optionally specified conditions on the cards
 * in the hand.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class Hand implements Serializable
{

	/** defined cards in hand; each card may be a mask */
	private List<Card>					cards;

	/** whether hand is maskable (true), or is a concrete hand (false) */
	private boolean						maskableHand;

	public static PrintStream			defaultDbgStream	= null;

	private transient PrintStream		dbgStream			= defaultDbgStream;

	/** temporary storage of # outs for given hand draw */
	private transient int[]				outs;

	/** temporary storage for kickers for given hand draw */
	private transient int[][]			kicks;

	/** utility for printOuts to use calculateDrawOdds */
	private static double[][]			drawOdds;

	/** utility for printOuts to use calculateDrawOdds */
	private static Distribution[][][]	drawKickers;

	static
	{
		drawOdds = new double[9][1];
		drawKickers = new Distribution[9][][];
		drawKickers[0] = new Distribution[1][1];
		drawKickers[1] = new Distribution[2][1];
		drawKickers[2] = new Distribution[2][1];
		drawKickers[3] = new Distribution[1][1];
		drawKickers[4] = new Distribution[1][1];
		drawKickers[5] = new Distribution[2][1];
		drawKickers[6] = new Distribution[2][1];
		drawKickers[7] = new Distribution[1][1];
		drawKickers[8] = new Distribution[1][1];
		for (int i = 0; i < 9; i++)
			for (int j = 0; j < drawKickers[i].length; j++)
				drawKickers[i][j][0] = new Distribution("rank", new String[] {
			"2", "3", "4", "5", "6", "7", "8", "9", "T", "J", "Q", "K", "A",},
						new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
			0.0, 0.0, 0.0, 0.0, 0.0});
	}


	public Hand()
	{

	}

	public Hand (Hand h)
	{
		this.maskableHand = h.maskableHand;
		this.cards = new ArrayList<Card>(h.cards.size());
		this.cards.addAll(h.cards);
	}

	/**
	 * Constructor.
	 * 
	 * @param cardStrings
	 *            Strings which are either cards, card masks, a *, or a +.
	 * @param tests
	 *            Specifications on the bound card valus.
	 * @throws PokerError
	 */
	public Hand(List<String> cardStrings, boolean maskable) throws PokerError
	{
		this.maskableHand = maskable;
		this.cards = new ArrayList<Card>(cardStrings.size());
		for (String s : cardStrings)
			cards.add(Card.fromString(s, maskable));
	}


	public Hand(String... cards) throws PokerError
	{
		this.maskableHand = false;
		this.cards = new ArrayList<Card>(cards.length);
		for (String s : cards)
			this.cards.add(Card.fromString(s, false));
	}


	public Hand(Card... cards)
	{
		this.maskableHand = false;
		this.cards = new ArrayList<Card>(cards.length);
		for (Card c : cards)
			this.cards.add(c);
	}


	public List<Card> getCards()
	{
		return cards;
	}


	public boolean contains(Card card, PokerAI ai) throws PokerError
	{
		return contains(new Hand(card), ai);
	}


	public boolean contains(Hand hand, PokerAI ai) throws PokerError
	{
		List<Card> cc = new ArrayList<Card>(hand.cards.size());
		boolean[] used = new boolean[cards.size()];
		for (Card c : hand.cards)
			cc.add(c.resolve(ai));
		List<Map<String, Value>> binds = new ArrayList<Map<String, Value>>();
		boolean b = contains(ai, cc, new HashMap<String, Value>(), binds, used,
				0);
		if (b)
		{
			Map<String, LazyBind> bind = new HashMap<String, LazyBind>();
			for (Map<String, Value> map : binds)
			{
				for (String s : map.keySet())
				{
					if (!bind.containsKey(s))
						bind.put(s, new LazyBind(s));
					Value v = map.get(s);
					bind.get(s).addValue(v);
				}
			}
			for (String s : bind.keySet())
			{
				LazyBind lb = bind.get(s);
				ai.set(s, lb.size() == 1 ? lb.getValues().get(0)
						: Value.fromLazyBind(lb));
			}
		}
		return b;
	}


	private boolean contains(PokerAI ai, List<Card> cc, Map<String, Value> map,
			List<Map<String, Value>> binds, boolean[] used, int n)
	{
		String tab = "  ";
		String prefix = "";
		for (int i = 0; i < (n * 2); i++)
			prefix += tab;

		if (n == cc.size())
		{
			dbg("%sadding binding %s\n", prefix, map.toString());
			binds.add(new HashMap<String, Value>(map));
			return true;
		}
		dbg("%sentered level %d\n", prefix, n);

		boolean f = false;
		Card c = cc.get(n);

		boolean mv_o = c.getValue().isBound();
		boolean ms_o = c.getSuit().isBound();

		dbg("%scard before tryMap(): %s\n", prefix, c.toString());
		c.tryMap(map);
		dbg("%scard after tryMap(): %s\n", prefix, c.toString());

		boolean mv = c.getValue().isBound();
		boolean ms = c.getSuit().isBound();

		for (int i = 0; i < used.length; i++)
		{
			if (used[i])
			{
				dbg("%shand card %d is used, skipping\n", prefix, i);
				continue;
			}

			dbg("%strying hand card %d\n", prefix, i);
			Card t = cards.get(i).resolve(ai);
			dbg("%sattempting resolve by %s: ", prefix + tab, t.toString());

			if (!t.resolvesByMap(c, map))
			{
				dbg("failed.\n");
				continue;
			}
			dbg("succeeded!\n");
			int s2 = map.size();

			used[i] = true;
			if (contains(ai, cc, map, binds, used, n + 1))
				f = true;
			used[i] = false;

			dbg("%sunmapping %s (value? %b) (suit? %b)\n", prefix + tab,
					c.toString(), mv, ms);
			c.unmap(mv, ms);
			if (mv)
				map.remove(c.getValueVariable());
			if (ms)
				map.remove(c.getSuitVariable());
			dbg("%scard back to %s\n", prefix + tab, c.toString());
		}

		dbg("%sfinal unmap %s (value? %b) (suit? %b)\n", prefix, c.toString(),
				mv_o, ms_o);
		c.unmap(mv_o, ms_o);
		dbg("%scard back to %s\n", prefix, c.toString());
		dbg("%sresult successful? %b\n", prefix, f);
		return f;
	}


	public boolean equals(Object o)
	{
		if ((o == null) || !(o instanceof Hand))
			return false;

		Hand h = (Hand) o;
		if (maskableHand != h.maskableHand)
			return false;

		if (cards.size() != h.cards.size())
			return false;

		for (Card c : cards)
			if (!h.cards.contains(c))
				return false;

		for (Card c : h.cards)
			if (!cards.contains(c))
				return false;

		return true;
	}


	public void setDebugOutputStream(PrintStream stream)
	{
		dbgStream = stream;
	}


	private void dbg(String format, Object... args)
	{
		if (dbgStream != null)
			dbgStream.printf(format, args);
	}

	public void addIn(Card c)
	{
		if (!cards.contains(c))
			cards.add(c);
	}
	
	public void remove(Card c)
	{
		cards.remove(c);
	}
	
	public Hand add(Hand h)
	{
		int i = 0;
		Card[] c_ = new Card[this.cards.size() + h.cards.size()];
		for (Card c : cards)
			c_[i++] = c;
		for (Card c : h.cards)
			c_[i++] = c;
		return new Hand(c_);
	}


	public void include(Hand h)
	{
		for (Card c : h.cards)
			if (!cards.contains(c))
				cards.add(c);
	}


	public void clear()
	{
		cards.clear();
	}


	public void calculateDrawOdds(int n, double[][] drawOdds,
			Distribution[][][] kickers, Card c1, Card c2)
	{
		// 0: high card
		// 1: pair
		// 2: two pair
		// 3: set
		// 4: straight
		// 5: flush
		// 6: full house
		// 7: four of a kind
		// 8: straight flush

		int[] hand = new int[cards.size() + 2];
		for (int i = 0; i < cards.size(); i++)
			hand[i + 2] = cards.get(i).getIndex();
		hand[0] = c1.getIndex();
		hand[1] = c2.getIndex();

		int coming = 7 - hand.length;
		int left = 52 - hand.length;

		for (int i = 0; i < 9; i++)
		{
			int numOuts = getOuts(hand, i, hand[0], hand[1]);
			int numkicks = kickers[i].length;

			// product of getOuts():
			// (if hand is 7 long, num outs > 0 means made hand, 0 means no
			// hand, else # outs)
			// o[j] = int[1][j = num different makeable hand i's]
			// k[j][T] = kicker #'s for (t:T) kickers on made hand in
			// o[0][0][j]

			int[] o = outs;
			int[][] k = kicks;

			double P = 1.0;
			for (int j = 0; j < numOuts; j++)
			{
				double p, q = 0.0;
				if (coming == 0)
					p = (o[j] > 0) ? 1.0 : 0.0;
				else
				{
					p = ((double) o[j] / (double) left);
					q = ((double) o[j] / (double) (left - 1));
				}
				if (coming > 1)
					p = 1.0 - ((1.0 - p) * (1.0 - q));
				for (int t = 0; t < numkicks; t++)
				{
					if (j == 0)
						kickers[i][t][n].zero();
					kickers[i][t][n].values[k[t][j]] += p;
				}
				P *= (1.0 - p);
			}
			for (int t = 0; t < numkicks; t++)
				kickers[i][t][n].normalize();
			drawOdds[i][n] = (1.0 - P);
		}
	}


	private int getOuts(int[] hand, int draw, int c1, int c2)
	{
		if (outs == null)
		{
			outs = new int[100];
			kicks = new int[3][100];
		}

		int coming = 7 - hand.length;
		int left = 52 - hand.length;
		int numRanks = 0;
		int[] ranks = new int[hand.length];
		int[] nums = new int[hand.length];
		int numPairs = 0;

		int r1 = rank(c1);
		int r2 = rank(c2);

		// get the ranks of cards in hand,
		// and counts of those ranks in hand
		for (int j, i = 0; i < hand.length; i++)
		{
			for (j = 0; j < numRanks; j++)
				if (rank(hand[i]) == ranks[j])
				{
					if (++nums[j] == 2)
						numPairs++;
					break;
				}
			if (j == numRanks)
			{
				nums[numRanks] = 1;
				ranks[numRanks++] = rank(hand[i]);
			}
		}

		int num = 0;

		switch (draw) {
		/* outs for a high card */
		case 0:
			/* made hands */
			int max = -1;
			for (int i = 0; i < numRanks; i++)
				if ((max == -1) || (ranks[i] > max))
					max = ranks[i];
			outs[num] = left;
			kicks[0][num++] = max;
			/* draws */
			if (coming > 0)
			{
				for (int j, i = max + 1; i < 13; i++)
				{
					for (j = 0; j < numRanks; j++)
						if (i == ranks[j])
							break;
					if (j == numRanks)
					{
						outs[num] = 4;
						kicks[0][num++] = i;
					}
				}
			}
			break;
		/* outs for a pair */
		case 1:
			int hm = -1;
			/* made hands */
			for (int i = 0; i < numRanks; i++)
			{
				if (nums[i] >= 2)
				{
					hm = i;
					outs[num] = left;
					kicks[0][num] = ranks[i];
					if ((r1 == ranks[i]) && (r2 != r1))
						kicks[1][num++] = r2;
					else if ((r2 == ranks[i]) && (r1 != r2))
						kicks[1][num++] = r1;
					else
						kicks[1][num++] = 0;
				}
			}
			/* draws */
			if (coming > 0)
			{
				for (int i = hm + 1; i < numRanks; i++)
				{
					if (nums[i] == 1)
					{
						outs[num] = 3;
						kicks[0][num] = ranks[i];
						if (r1 == ranks[i])
							kicks[1][num++] = r2;
						else if (r2 == ranks[i])
							kicks[1][num++] = r1;
						else if (r2 > r1)
							kicks[1][num++] = r2;
						else
							kicks[1][num++] = r1;
					}
				}
			}
			break;
		/* outs for two pair */
		case 2:
			/* made hands */
			if (numPairs >= 2)
			{
				int l = -1, h = -1;
				for (int i = 0; i < numRanks; i++)
				{
					if (nums[i] < 2)
						continue;
					int r = ranks[i];
					if (r > h)
					{
						l = h;
						h = r;
					}
					else if (r > l)
						l = r;
				}
				outs[num] = left;
				kicks[0][num] = h;
				kicks[1][num++] = l;
			}
			/* draws */
			if (coming > 0)
			{
				int hp = -1;
				for (int i = 0; i < numRanks; i++)
					if ((nums[i] == 2) && ((hp == -1) || (ranks[i] > hp)))
						hp = ranks[i];
				if (hp == -1)
					break;
				for (int j = 0; j < numRanks; j++)
					if (nums[j] == 1)
					{
						int l = hp;
						int h = ranks[j];
						if (l > h)
						{
							int t = l;
							l = h;
							h = t;
						}
						outs[num] = 3;
						kicks[0][num] = h;
						kicks[1][num++] = l;
					}
			}
			break;
		/* outs for set */
		case 3:
			/* made hands */
			for (int i = 0; i < numRanks; i++)
				if (nums[i] >= 3)
				{
					outs[num] = left;
					kicks[0][num++] = ranks[i];
				}
			/* draws */
			if (coming > 0)
				for (int i = 0; i < numRanks; i++)
					if (nums[i] == 2)
					{
						outs[num] = 2;
						kicks[0][num++] = ranks[i];
					}
			break;
		/* outs for straight */
		case 4:
			int[] S = straight(hand, false);

			switch (S[0]) {
			case 0: // made hand
				outs[num] = left;
				kicks[0][num++] = S[1];
				break;
			case 1: // open-ended (or double-belly-buster)
				if (coming > 0)
				{
					outs[num] = 4;
					kicks[0][num++] = S[1];
					outs[num] = 4;
					kicks[0][num++] = S[2];
				}
				break;
			case 2: // inside
				if (coming > 0)
				{
					outs[num] = 4;
					kicks[0][num++] = S[1];
				}
				break;
			}
			break;
		/* outs for flush */
		case 5:
			int[] F = flush(hand); // toflush,high,low
			if (F[0] == 0)
			{
				outs[num] = left;
				kicks[0][num] = F[1];
				kicks[1][num++] = F[2];
			}
			else if ((F[0] == 1) && (coming > 0))
			{
				outs[num] = 9;
				kicks[0][num] = F[1];
				kicks[1][num++] = F[2];
			}
			else if ((F[0] == 2) && (coming == 2))
			{
				outs[num] = 1; // correct within .01%
				kicks[0][num] = F[1];
				kicks[1][num++] = F[2];
			}
			break;
		/* outs for full house */
		case 6:
			/* made hand */
			int h = -1,
			l = -1;
			for (int i = 0; i < numRanks; i++)
			{
				if (nums[i] == 3)
				{
					for (int j = 0; j < numRanks; j++)
					{
						if (j == i)
							continue;
						if (nums[j] == 2)
						{
							outs[num] = left;
							kicks[0][num] = ranks[i];
							kicks[1][num++] = ranks[j];
							return num;
						}
					}
				}
			}
			/* draws */
			if (coming > 0)
			{
				int bn = -1, sn = -1;
				for (int i = 0; i < numRanks; i++)
				{
					if (nums[i] >= 3)
					{
						bn = ranks[i];
						break;
					}
				}
				if (bn != -1)
				{
					for (int i = 0; i < numRanks; i++)
					{
						if (ranks[i] == bn)
							continue;
						outs[num] = 3;
						kicks[0][num] = bn;
						kicks[1][num++] = ranks[i];
					}
				}
				else if (numPairs >= 2)
				{
					l = -1;
					h = -1;
					for (int i = 0; i < numRanks; i++)
					{
						if (nums[i] < 2)
							continue;
						int r = ranks[i];
						if (r > h)
						{
							l = h;
							h = r;
						}
						else if (r > l)
							l = r;
					}
					outs[num] = 2;
					kicks[0][num] = h;
					kicks[1][num++] = l;
					outs[num] = 2;
					kicks[0][num] = l;
					kicks[1][num++] = h;
				}
			}
			break;
		/* outs for four of a kind */
		case 7:
			/* made hands */
			for (int i = 0; i < numRanks; i++)
				if (nums[i] == 4)
				{
					outs[num] = left;
					kicks[0][num++] = ranks[i];
				}
			/* draws */
			if (coming > 0)
			{
				for (int i = 0; i < numRanks; i++)
					if (nums[i] == 3)
					{
						outs[num] = 1;
						kicks[0][num++] = ranks[i];
					}
			}
			break;
		/* outs for straight flush */
		case 8:
			S = straight(hand, true);
			switch (S[0]) {
			case 0: // made hand
				outs[num] = left;
				kicks[0][num++] = S[1];
				break;
			case 1: // open-ended (or double-belly-buster)
				if (coming > 0)
				{
					outs[num] = 2;
					kicks[0][num++] = S[1];
				}
				break;
			case 2: // inside
				if (coming > 0)
				{
					outs[num] = 1;
					kicks[0][num++] = S[1];
				}
				break;
			}
			break;
		default:
			return 0;
		}

		return num;
	}


	private void printArray(int[] s, String string)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i : s)
			sb.append(i + ", ");
		sb.setLength(sb.length() - 2);
		sb.append("]");
		System.out.printf("%s%s\n", string, sb.toString());
	}


	private boolean hasValues(int[] hand, boolean flush, int... vals)
	{
		for (int i : vals)
		{
			int nv = 0;
			for (int j = 0; j < hand.length; j++)
				if (rank(hand[j]) == i)
					nv++;
			if (nv < 1)
				return false;
		}

		if (!flush)
			return true;

		int[] ns = new int[4];
		for (int i : vals)
			for (int j = 0; j < hand.length; j++)
				if (rank(hand[j]) == i)
					ns[suit(hand[j])]++;

		for (int i = 0; i < 4; i++)
			if (ns[i] >= vals.length)
				return true;

		return false;
	}


	private int consec(int[] hand, int num, boolean flush)
	{
		int nc = 1;
		for (int i = 1; i < hand.length; i++)
		{
			if (rank(hand[i]) == (rank(hand[i - 1]) + 1)
					&& (!flush || suit(hand[i]) == suit(hand[i - 1])))
			{
				nc++;
				if (nc == num)
					return i;
			}
			else
				nc = 1;
		}
		return -1;
	}


	private int[] straight(int[] hand, boolean flush)
	{
		// return[0]: -1=none, 0=made, 1=open, 2=inside
		// 0,1,2,3 = made (if 0=2)
		// 0,1,2 = inside (if 0=2 or 2=5)
		// 0,1,2,3,4 = made
		// 0,1,2,3 = open (unless 0=2 or 3=A, then inside)
		// 0,2,3,4,6 = open
		// 0,2,3,4 = inside
		// 0,1,3,4 = inside
		// 0,1,2,4 = inside

		int[] ord = hand.clone();
		Arrays.sort(ord);

		int i = consec(ord, 5, flush);
		if (i != -1)
			return new int[] {0, rank(ord[i])};

		i = consec(ord, 4, flush);
		if (i != -1 && rank(ord[i]) == 12)
			return new int[] {2, 12};
		else if (i > 0)
			return new int[] {1, rank(ord[i]) + 1, rank(ord[i])};

		i = consec(ord, 3, flush);
		if (i != -1 && rank(ord[i]) < 11
				&& hasValues(hand, false, rank(ord[i]) + 2)
				&& (!flush || suit(ord[i]) == suit(ord[i + 1])))
		{
			if (rank(ord[i]) >= 4 && i > 2
					&& hasValues(hand, false, rank(ord[i]) - 4)
					&& (!flush || suit(ord[i]) == suit(ord[i - 3])))
				return new int[] {1, rank(ord[i + 1]), rank(ord[i])};
			else
				return new int[] {2, rank(ord[i + 1])};
		}
		else if (i != -1 && rank(ord[i]) >= 4
				&& hasValues(hand, false, rank(ord[i]) - 4)
				&& (!flush || suit(ord[i]) == suit(ord[i - 3])))
			return new int[] {2, rank(ord[i])};

		i = consec(ord, 2, flush);
		if (i != -1
				&& rank(ord[i]) < 10
				&& hasValues(hand, false, rank(ord[i]) + 2, rank(ord[i]) + 3)
				&& (!flush || (suit(ord[i]) == suit(ord[i + 1]) && suit(ord[i]) == suit(ord[i + 2]))))
			return new int[] {2, rank(ord[i + 2])};

		if (hasValues(hand, flush, 0, 1, 2, 3))
			return new int[] {0, 3};
		else if (hasValues(hand, flush, 1, 2, 3))
			return new int[] {1, 4, 3};
		else if (hasValues(hand, flush, 0, 2, 3))
			return new int[] {2, 3};
		else if (hasValues(hand, flush, 0, 1, 3))
			return new int[] {2, 3};
		else if (hasValues(hand, flush, 0, 1, 2))
			return new int[] {2, 3};

		return new int[] {-1, 0};
		//		
		// int[] ns = new int[4];
		// int[][] sord = flush ? new int[4][] : new int[1][];
		//
		// if (flush)
		// {
		// for (int i = 0; i < ord.length; i++)
		// ns[suit(ord[i])]++;
		// for (int i = 0; i < 4; i++)
		// {
		// sord[i] = new int[ns[i]];
		// for (int j = 0, k = 0; k < ord.length; k++)
		// if (suit(ord[k]) == i)
		// sord[i][j++] = ord[k];
		// }
		// }
		// else
		// sord[0] = ord;
		// // straight patterns (shown above)
		// int[][] find = new int[][] { {1, 1, 1, 1}, {1, 1, 1}, {2, 1, 1, 2},
		// {2, 1, 1}, {1, 2, 1}, {1, 1, 2}};
		//
		// // loop straight patterns
		// for (int i = 0; i < find.length; i++)
		// {
		// int[] F = find[i];
		// // loop suit-hands
		// for (int q = 0; q < sord.length; q++)
		// {
		// ord = sord[q];
		// // loop starting card in ordered hand
		// for (int j = 0; j < (ord.length - F.length); j++)
		// {
		// int r = rank(ord[j]); // rank of first card
		// int nr = r + F[0]; // next expected rank
		//
		// // loop ordered hand cards
		// for (int k = j + 1; k < hand.length; k++)
		// {
		// r = rank(ord[k]); // current card rank
		//
		// if (r != nr)
		// break; // go to next start card (or fail)
		// // if enough cards for straight
		// if ((k - j) == F.length)
		// {
		// switch (i) {
		// case 0:
		// return new int[] {0, r};
		// case 1:
		// if (rank(ord[j]) == 0)
		// return new int[] {2, r + 1};
		// else if (r == 12)
		// return new int[] {2, r};
		// else
		// return new int[] {1, r, r + 1};
		// case 2:
		// return new int[] {1, r - 2, r};
		// default:
		// return new int[] {2, r};
		// }
		// }
		// // go to next card
		// nr = r + F[k - j];
		// }
		// }
		// }
		// }
		// return new int[] {-1, 0};
	}


	private int[] flush(int[] hand)
	{
		int num = 0;
		int[] suits = new int[hand.length];
		int[] ranks = new int[hand.length];
		int[] nums = new int[hand.length];
		int s0 = suit(hand[0]), s1 = suit(hand[1]);
		int r0 = rank(hand[0]), r1 = rank(hand[1]);

		for (int j, i = 0; i < hand.length; i++)
		{
			int r = rank(hand[i]);
			int s = suit(hand[i]);
			for (j = 0; j < num; j++)
				if (suits[j] == s)
				{
					nums[j]++;
					break;
				}
			if (j == num)
			{
				nums[num] = 1;
				suits[num] = s;
				ranks[num++] = r;
			}
		}

		for (int i = 0; i < num; i++)
		{
			if (nums[i] >= 3)
			{
				int toflush = 5 - nums[i];
				if (toflush < 0)
					toflush = 0;
				int l = 0, h = 0;
				if (s0 == suits[i])
					l = r0;
				if (s1 == suits[i])
					h = r1;
				if (l > h)
				{
					int t = l;
					l = h;
					h = t;
				}
				return new int[] {toflush, h, l};
			}
		}
		return new int[] {-1, 0, 0};
	}


	private static String[]	hands	= new String[] {"high card", "a pair",
			"two pair", "a set", "a straight", "a flush", "a full house",
			"four of a kind", "a straight flush"};


	public void printOuts(int draw, int c1, int c2)
	{
		int[] hand = new int[cards.size() + 2];
		for (int i = 0; i < cards.size(); i++)
			hand[i + 2] = cards.get(i).getIndex();
		hand[0] = c1;
		hand[1] = c2;
		int coming = 7 - hand.length;

		System.out.printf("drawing to %s with %d cards to come:\n",
				hands[draw], coming);
		int num = getOuts(hand, draw, c1, c2);
		for (int i = 0; i < num; i++)
			System.out.printf("\t%d outs, kickers are %s, %s, %s\n", outs[i],
					PokerValue.values()[kicks[0][i]],
					PokerValue.values()[kicks[1][i]],
					PokerValue.values()[kicks[2][i]]);
		calculateDrawOdds(0, drawOdds, drawKickers, card(c1), card(c2));
		System.out.printf("\t%6.2f%%\n", drawOdds[draw][0] * 100.0);
		for (int i = 0; i < drawKickers[draw].length; i++)
			System.out.printf("\t\t%s\n", drawKickers[draw][i][0].toString());
		System.out.println();
	}


	private int suit(int card)
	{

		return card % 4;
	}


	private int rank(int card)
	{
		return card / 4;
	}


	private Card card(int card)
	{
		return Card.fromIndex(card);
	}


	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		for (Card c : cards)
			sb.append(c.toString() + ", ");
		if (sb.length() == 0)
			return "<empty>";
		sb.setLength(sb.length() - 2);
		return sb.toString();
	}


	public int numOfValue(PokerValue v)
	{
		int n = 0;
		for (Card c : cards)
			if (c.getValue() == v)
				n++;
		return n;
	}


	public PokerSuit flushSuit(int n)
	{
		PokerSuit s = dominantSuit();
		int m = numOfSuit(s);
		if (m + n >= 5)
			return s;
		else
			return null;
	}


	public int numOfSuit(PokerSuit s)
	{
		int n = 0;
		for (Card c : cards)
			if (c.getSuit() == s)
				n++;
		return n;
	}


	public PokerSuit dominantSuit()
	{
		int max = 0, idx = 0;
		PokerSuit[] suits = PokerSuit.values();

		for (int i = 0; i < 4; i++)
		{
			int n = numOfSuit(suits[i]);
			if (n > max)
			{
				max = n;
				idx = i;
			}
		}

		return suits[idx];
	}


	public PokerValue[] missingToStraight(PokerValue v, boolean flush)
	{
		int n = (v.ordinal() == 3) ? 4 : 5;
		List<PokerValue> m = new ArrayList<PokerValue>();
		PokerSuit s = dominantSuit();
		for (int i = 0; i < n; i++)
		{
			if (flush && !cards.contains(new Card(v, s)))
				m.add(v);
			if (!flush && numOfValue(v) == 0)
				m.add(v);
			if (i < (n - 1))
				v = PokerValue.values()[v.ordinal() - 1];
		}
		return (PokerValue[]) m.toArray(new PokerValue[0]);
	}


	public void readObject(ObjectInputStream stream) throws IOException,
			ClassNotFoundException
	{
		maskableHand = stream.readBoolean();
		int num = stream.readInt();
		cards = new ArrayList<Card>(num);
		for (int i = 0; i < num; i++)
			cards.add((Card) stream.readObject());
	}


	public void writeObject(ObjectOutputStream stream) throws IOException
	{
		stream.writeBoolean(maskableHand);
		stream.writeInt(cards.size());
		for (Card c : cards)
			stream.writeObject(c);
	}


	public String toString(String sep)
	{
		StringBuilder sb = new StringBuilder();
		for (Card c : cards)
			sb.append(c + sep);
		if (sb.length() > 0)
			sb.setLength(sb.length() - sep.length());
		return sb.toString();
	}


	public Card getCard(int i)
	{
		return cards.get(i);
	}

	public int size()
	{
		return cards.size();
	}

	/**
	 * Sort into ascending order by rank then suit.
	 */
	public void sort()
	{
		Card[] c = cards.toArray(new Card[0]);
		Arrays.sort(c, new CardComparator());
		cards.clear();
		for (Card c_ : c)
			cards.add(c_);
	}

	public List<PokerValue> getPairValues()
	{
		List<PokerValue> pvs = new ArrayList<PokerValue>();
		for (Card c : cards)
			if (numOfValue(c.getValue()) == 2)
				if (!pvs.contains(pvs))
					pvs.add(c.getValue());
		return pvs;
	}

	public PokerValue getSetValue()
	{
		for (Card c : cards)
			if (numOfValue(c.getValue()) == 3)
				return c.getValue();
		return null;
	}

}
