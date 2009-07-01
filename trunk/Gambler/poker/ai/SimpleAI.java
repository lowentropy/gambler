/* 
 * SimpleAI.java
 * 
 * created: 16-May-06
 * author: Nathan Matthews
 * email: lowentropy@gmail.com
 * 
 * Copyright (C) 2006
 */

package poker.ai;

import poker.ai.core.Card;
import poker.ai.core.Hand;
import poker.common.Deck;
import poker.server.base.Player;

/**
 * TODO: SimpleAI
 * 
 * @author lowentropy
 */
public class SimpleAI
{

	private static final boolean	DEBUG		= true;

	public static final int			FOLD		= 1;

	public static final int			CALL		= 2;

	public static final int			RAISE		= 3;

	public static final int			BET			= 4;

	public static final int			CHECKFOLD	= 5;

	private static final int		NOPAIR		= 0;

	private static final int		PAIR		= 1;

	private static final int		TWOPAIR		= 2;

	private static final int		TRIPS		= 3;

	private static final int		STRAIGHT	= 4;

	private static final int		FLUSH		= 5;

	private static final int		FULLHOUSE	= 6;

	private static final int		QUADS		= 7;

	private static final int		SFLUSH		= 8;


	public static int getAction(int round, Hand hole, Hand board, double pot,
			double toCall, double bb, int numBets, int numActive,
			int numCommitted)
	{
		Hand h = hole;
		Hand b = board;
		Card c1 = h.getCard(0);
		Card c2 = h.getCard(1);

		dbg(
				"\nround  = %d\nboard = %s\nhole  = %s\npot   = %.2f\ncost  = %.2f\n",
				round, board, hole, pot, toCall);

		if (round < Player.POSTFLOP)
			return getPreflopAction(c1, c2, b, pot, toCall, bb, numBets,
					numActive, numCommitted);
		else
			return getPostflopAction(round, c1, c2, b, pot, toCall, bb,
					numBets, numActive, numCommitted);
	}


	private static void dbg(String fmt, Object... vals)
	{
		if (DEBUG)
			System.out.printf(fmt, vals);
	}


	private static int getPreflopAction(Card c1, Card c2, Hand b, double pot,
			double toCall, double bb, int numBets, int numActive,
			int numCommitted)
	{
		// play all pocket-pairs
		int v1 = c1.getValue().ordinal() + 2;
		int v2 = c2.getValue().ordinal() + 2;

		if (v1 == v2)
		{
			dbg("> pair ");
			if (v1 >= 10 || v1 == 2)
			{
				dbg(" >= Ten: RAISE\n");
				return RAISE;
			}
			dbg(" < Ten: CALL\n");
			return CALL;
		}

		// play all cards where both cards are bigger than Tens
		// and raise if they are suited
		if (v1 >= 10 && v2 >= 10)
		{
			dbg("> Both > Ten");
			if (c1.getSuit() == c2.getSuit())
			{
				dbg(", suited: RAISE\n");
				return RAISE;
			}
			dbg(": CALL\n");
			return CALL;
		}
		// play all suited connectors
		if (c1.getSuit() == c2.getSuit())
		{
			dbg("> suited");
			if (Math.abs(v1 - v2) == 1)
			{
				dbg(" connectors: CALL\n");
				return CALL;
			}
			// raise A2 suited
			if ((v1 == 14 && v2 == 2) || (v2 == 14 && v1 == 2))
			{
				dbg(" A2: RAISE\n");
				return RAISE;
			}
			// call any suited ace
			if ((v1 == 14 || v2 == 14))
			{
				dbg(" Ace: CALL\n");
				return CALL;
			}
		}

		// play anything 5% of the time
		if (toCall <= bb)
		{
			if (Math.random() < 0.05)
			{
				dbg("> 5%%: CALL\n");
				return CALL;
			}
		}

		dbg("> nothing: CHECK-FOLD\n");
		return CHECKFOLD;
	}


	private static int getPostflopAction(int round, Card c1, Card c2, Hand b,
			double pot, double toCall, double bb, int numBets, int numActive,
			int numCommitted)
	{
		int np = numActive;
		double tc = toCall;
		double P0 = tc / (pot + tc);
		double HRN = handRank(c1, c2, b, np - 1);
		double PPOT = (round < Player.POSTRIVER) ? ppot1(c1, c2, b) : 0.0;
		int nc = numCommitted;

		if (HRN == 1.0)
			return RAISE;

		if (tc == 0.0)
		{
			if (Math.random() < (HRN * HRN))
				return BET;
			if (Math.random() < PPOT)
				return BET;
		}
		else
		{
			if (Math.random() < Math.pow(HRN, (double) numBets))
				return RAISE;
			if ((HRN * HRN * pot) > tc || PPOT > P0)
				return CALL;
		}
		return CHECKFOLD;
	}


	private static double ppot1(Card c1, Card c2, Hand b)
	{
		double ppot, npot;
		int d1, d2;
		int[][] HP = new int[3][3];
		int[] HPT = new int[3];
		int ur7, or, idx, ur5 = rank(c1, c2, b);
		Deck d = new Deck();
		d.extractCard(c1);
		d.extractCard(c2);
		d.extractHand(b);

		for (int i = 0; i < 52; i++)
		{
			if (d.dealt(i))
				continue;
			Card o1 = d.getCard(i);
			for (int j = i + 1; j < 52; j++)
			{
				if (d.dealt(j))
					continue;
				Card o2 = d.getCard(j);
				or = rank(o1, o2, b);
				if (ur5 > or)
					idx = 0;
				else if (ur5 < or)
					idx = 1;
				else
					idx = 2;
				HPT[idx]++;
				for (int k = 0; k < 52; k++)
				{
					if (k == i || k == j || d.dealt(k))
						continue;
					Card kc = d.getCard(k);
					b.addIn(kc);
					ur7 = rank(c1, c2, b);
					or = rank(o1, o2, b);
					if (ur7 > or)
						HP[idx][0]++;
					else if (ur7 < or)
						HP[idx][1]++;
					else
						HP[idx][2]++;
					b.remove(kc);
				}
			}
		}

		ppot = npot = 0.0;
		d1 = 45 * (HPT[1] + HPT[2] / 2);
		d2 = 45 * (HPT[0] + HPT[2] / 2);
		if (d1 > 0)
			ppot = ((double) HP[1][0] + (double) HP[1][2] / 2.0 + (double) HP[2][0] / 2)
					/ (double) d1;
		if (d2 > 0)
			npot = ((double) HP[0][1] + (double) HP[0][2] / 2.0 + (double) HP[2][1] / 2)
					/ (double) d2;
		return ppot;
	}


	private static double handRank(Card c1, Card c2, Hand b, int np)
	{
		double hr = handRank(c1, c2, b);
		double h = hr;
		for (int i = 0; i < np - 1; i++)
			h *= hr;
		return h;
	}


	private static double handRank(Card c1, Card c2, Hand board)
	{
		Hand my = new Hand(board);
		Hand xx = new Hand(board);
		my.addIn(c1);
		my.addIn(c2);
		int myr = rank(my);
		Deck d = new Deck();
		d.extractHand(my);
		int g = 0, b = 0, t = 0;
		for (int i = 0; i < 52; i++)
		{
			if (d.dealt(i))
				continue;
			Card o1 = d.getCard(i);
			xx.addIn(o1);
			for (int j = i + 1; j < 52; j++)
			{
				if (d.dealt(j))
					continue;
				Card o2 = d.getCard(j);
				xx.addIn(o2);
				int xxr = rank(xx);
				if (myr > xxr)
					g++;
				else if (myr < xxr)
					b++;
				else
					t++;
				xx.remove(o2);
			}
			xx.remove(o1);
		}
		double g_ = (double) g;
		double b_ = (double) b;
		double t_ = (double) t;
		return (g_ + t_ / 2) + (g_ + b_ + t_);
	}


	private static int rank(Card c1, Card c2, Hand h)
	{
		Hand h2 = new Hand(h);
		h2.addIn(c1);
		h2.addIn(c2);
		return rank(h2);
	}


	private static int rank(Hand h)
	{
		int nc = h.size();
		int[] nr = new int[13];
		int[] ns = new int[4];
		int ra = -1, rb = -1;
		int na = 0, nb = 0;
		int ncr = 0;
		int ncs = 0;
		int cs = -1;
		h.sort();
		
		for (int i = 0; i < nc; i++)
		{
			Card c = h.getCard(i);
			int r = c.getValue().ordinal();
			int s = c.getSuit().ordinal();
			nr[r]++;
			ns[s]++;
			if (i > 0 && (r - h.getCard(i - 1).getValue().ordinal()) == 1)
			{
				ncr++;
				if (c.getSuit() == h.getCard(i - 1).getSuit())
				{
					ncs++;
					cs = s;
				}
				if (ncr == 5)
				{
					if (ncs == 5)
						return makeRank(SFLUSH, r);
					else
						return makeRank(STRAIGHT, r);
				}
			}
		}
		for (int i = 0; i < 4; i++)
		{
			if (ns[i] >= 5)
			{
				int[] top = findTopRanks(h, 5, i);
				return makeRank(FLUSH, top);
			}
		}
		// find straight and flush (or both)
		// find dups, try for set/quad, mark A
		// find other dups, try for set, mark B
		// class as one pair, two pair, set, full house, or quads
		// get tops for each and rest for each
		// else, get top five and nopair
		for (int i = 0; i < 13; i++)
		{
			if (nr[i] == 4)
				return makeRank(QUADS, i, findTopRanks(h, 1, -1, i));
			else if (nr[i] >= 2)
			{
				if (ra == -1 || i > ra)
				{
					rb = ra;
					nb = na;
					ra = i;
					na = nr[i];
				}
				else if (rb == -1 || i > rb)
				{
					rb = i;
					nb = nr[i];
				}
			}
		}
		if (ra == -1)
			return makeRank(NOPAIR, findTopRanks(h, 5, -1));
		if (rb == -1)
		{
			if (na == 2)
				return makeRank(PAIR, ra, findTopRanks(h, 3, -1, ra));
			else if (na == 3)
				return makeRank(TRIPS, ra, findTopRanks(h, 2, -1, ra));
			else
			{
				dbg("logic error!\n");
				return -1;
			}
		}
		else
		{
			if (na == 3)
				return makeRank(FULLHOUSE, ra, rb);
			else if (nb == 3)
				return makeRank(FULLHOUSE, rb, ra);
			else
				return makeRank(TWOPAIR, ra, rb, findTopRanks(h, 1, -1, ra, rb));
		}
	}


	private static int makeRank(int hand, int[] ranks)
	{
		int val = hand;
		int[] nr = new int[5];
		for (int i = 0; i < ranks.length; i++)
			nr[i] = ranks[i];
		for (int i = 0; i < 5; i++)
			val = val * 13 + nr[i];
		return val;
	}


	private static int makeRank(int hand, int rank1)
	{
		return makeRank(hand, new int[] {rank1});
	}


	private static int makeRank(int hand, int rank1, int rank2)
	{
		return makeRank(hand, new int[] {rank1, rank2});
	}


	private static int makeRank(int hand, int rank1, int[] ranks)
	{
		int[] nr = new int[ranks.length + 1];
		nr[0] = rank1;
		for (int i = 0; i < ranks.length; i++)
			nr[i + 1] = ranks[i];
		return makeRank(hand, nr);
	}


	private static int makeRank(int hand, int rank1, int rank2, int[] ranks)
	{
		int[] nr = new int[ranks.length + 2];
		nr[0] = rank1;
		nr[1] = rank2;
		for (int i = 0; i < ranks.length; i++)
			nr[i + 2] = ranks[i];
		return makeRank(hand, nr);
	}


	private static int[] findTopRanks(Hand h, int num, int suit, int... avoid)
	{
		int[] ret = new int[num];
		for (int i = 0; i < h.size(); i++)
		{
			Card c = h.getCard(i);
			int r = c.getValue().ordinal();
			int s = c.getSuit().ordinal();
			if (suit != -1 && s != suit)
				continue;
			boolean ok = true;
			for (int a : avoid)
				if (r == a)
				{
					ok = false;
					break;
				}
			if (!ok)
				continue;
			insert(r, ret, num);
		}
		return ret;
	}


	private static void insert(int r, int[] ret, int n)
	{
		for (int i = 0; i < n; i++)
			if (r > ret[i])
			{
				for (int j = i + 1; j < n; j++)
					ret[j] = ret[j - 1];
				ret[i] = r;
				return;
			}
	}
}
