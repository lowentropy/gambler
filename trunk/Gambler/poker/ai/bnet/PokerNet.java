/*
 * PokerNet.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.ai.bnet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import poker.ai.core.Card;
import poker.ai.core.Hand;
import bayes.BayesError;
import bayes.BayesNet;
import bayes.Distribution;
import bayes.Query;

/**
 * Bayesian network container with methods to perform poker-related operations.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public abstract class PokerNet
{

	public static final String[] posNames = new String[] { "folded",
			"small blind", "big blind", "under the gun", "early", "middle",
			"late" };

	/** state names corresponding to position ID */
	public static String[] positionStates = new String[] { "F", "SB", "BB",
			"U", "E", "M", "L" };

	/** loose bias */
	public static final int LOOSE = 0;

	/** no bias */
	public static final int NORMAL = 1;

	/** tight bias */
	public static final int TIGHT = 2;

	/** names of states indexed by ID of bias mode */
	public static final String[] biasStates = new String[] { "L", "N", "T" };

	/** folding move */
	public static final int FOLD = 1;

	/** calling move */
	public static final int CALL = 2;

	/** raising move */
	public static final int RAISE = 3;
	
	/** betting move */
	public static final int BET = 4;
	
	/** checking move */
	public static final int CHECK = 5;
	

	/** names of states indexed by action ID */
	public static final String[] actionStates = new String[] { null, "F", "C",
			"R" };

	/** fraction by which bias raises fold to call or call to raise, or back down */
	protected double bias_strength = 0.5;

	/** table mode (loose, normal, tight) */
	protected int tableMode = NORMAL;

	/** names of card ranks */
	public static String[] ranks;

	/** names of card suits */
	public static String[] suits;

	/** names of cards, full suit */
	public static String[] cards;

	/** all possible combinations of two cards, rank and suit included */
	public static String[] fullHoles;

	/** all group numbers from index of full hole */
	public static int[] fullGroups;

	/** names of possible hole card combinations */
	public static String[] holePairs;

	/** possible poker hands, in order of strength */
	public static String[] hands;

	/** map of hole pair name to group number (from 1 - 5) */
	private static Map<String, Integer> groupMap;
	
	/** map of hole pair name to index */
	private static Map<String, Integer> holeMap;

	/** the bayesian network */
	protected BayesNet net;

	/** query the player uses to determine tbe action */
	protected Query playerQuery;

	/** query the ai uses to determine opponent hands */
	protected Query oppQuery;

	/* static initialization */
	static
	{
		initHolePairs();
		initFullHoles();
		initHands();
		initGroupMap();
	}

	/**
	 * Build the poker network from known, fixed probabilistic data.
	 */
	public abstract void buildNetwork() throws BayesError;

	/**
	 * Build the queries for the network; the player and the opponent queries.
	 */
	public abstract void buildQueries() throws BayesError;

	/**
	 * Set a parameter. Implementation-dependent.
	 * 
	 * @param name
	 *            name of parameter
	 * @param value
	 *            value of parameter
	 */
	public abstract void setParam(String name, Object value);

	/**
	 * Perform a network computation.
	 * 
	 * @param name
	 *            name of computation
	 * @param omap
	 *            map of observed variables
	 * @param pmap
	 *            map of prior distributions
	 * @param qmap
	 *            map of query variables
	 */
	public abstract void compute(String name, Map<String, String> omap,
			Map<String, Distribution> pmap, Map<String, Distribution> qmap);

	/**
	 * Initialize total, with-suit hole combinations.
	 */
	private static void initFullHoles()
	{
		fullHoles = new String[1326];

		for (int i = 0, c = 0; i < 51; i++)
			for (int j = i + 1; j < 52; j++, c++)
				fullHoles[c] = cards[j] + cards[i];
	}

	/**
	 * Initialize possible hands.
	 */
	private static void initHands()
	{
		hands = new String[] { "high_card", "pair", "two_pair",
				"three_ofakind", "straight", "flush", "full_house",
				"four_ofakind", "straight_flush" };
	}

	/**
	 * Initialize names of hole combinations.
	 */
	private static void initHolePairs()
	{
		ranks = new String[] { "2", "3", "4", "5", "6", "7", "8", "9", "T",
				"J", "Q", "K", "A" };
		suits = new String[] { "c", "d", "h", "s" };

		holeMap = new HashMap<String, Integer>();
		
		cards = new String[52];
		for (int i = 0, c = 0; i < 13; i++)
			for (int j = 0; j < 4; j++, c++)
				cards[c] = ranks[i] + suits[j];

		int idx = 0;
		holePairs = new String[13 + 78 + 78];

		for (int i = 0; i < 13; i++)
			holePairs[idx++] = ranks[i] + ranks[i];
		
		for (int i = 0; i < 12; i++)
			for (int j = i + 1; j < 13; j++)
				holePairs[idx++] = ranks[j] + ranks[i] + "u";

		for (int i = 0; i < 12; i++)
			for (int j = i + 1; j < 13; j++)
				holePairs[idx++] = ranks[j] + ranks[i] + "s";
		
		for (int i = 0; i < holePairs.length; i++)
			holeMap.put(holePairs[i], i);
	}

	/**
	 * Initialize the mapping from hole cards to power group.
	 */
	private static void initGroupMap()
	{
		String[][] groups = new String[6][];
		groupMap = new HashMap<String, Integer>();

		groups[0] = new String[] { "AA", "KK" };
		groups[1] = new String[] { "QQ", "AKs" };
		groups[2] = new String[] { "JJ", "AQs", "KQs", "AJs", "KJs", "TT",
				"QJs", "ATs", "AKu" };
		groups[3] = new String[] { "JTs", "QTs", "KTs", "99", "A9s", "KQu",
				"AQu", "88", "T9s", "J9s", "Q9s", "K9s", "A8s", "A5s", "A3s",
				"A4s", "A6s", "A7s", "QJu", "KJu", "AJu", "66", "77", "98s",
				"T8s", "J8s", "Q8s", "K8s", "K7s", "A2s", "JTu", "KTu", "ATu" };
		groups[4] = new String[] { "55", "87s", "97s", "K5s", "K6s", "QTu",
				"22", "33", "44", "65s", "76s", "86s", "T7s", "J7s", "Q7s",
				"Q6s", "K2s", "K3s", "K4s", "54s", "64s", "75s", "96s", "T6s",
				"Q3s", "Q4s", "Q5s", "T9u", "A9u" };

		for (int i = 0; i < 5; i++)
			for (String hole : groups[i])
				groupMap.put(hole, i + 1);

		for (String hole : holePairs)
			if (!groupMap.containsKey(hole))
				groupMap.put(hole, 6);

		fullGroups = new int[fullHoles.length];
		for (int i = 0; i < fullHoles.length; i++)
		{
			String s = fullHoles[i];
			char c0 = s.charAt(0);
			char c1 = s.charAt(1);
			char c2 = s.charAt(2);
			char c3 = s.charAt(3);

			if (c0 == c2)
				fullGroups[i] = groupMap.get(new String(new char[] { c0, c0 }));
			else if (c1 == c3)
				fullGroups[i] = groupMap.get(new String(new char[] { c0, c2,
						's' }));
			else
				fullGroups[i] = groupMap.get(new String(new char[] { c0, c2,
						'u' }));
		}
	}

	/**
	 * @return bayesian network
	 */
	public BayesNet getNetwork()
	{
		return net;
	}

	/**
	 * @return player query
	 */
	public Query getPlayerQuery()
	{
		return playerQuery;
	}

	/**
	 * @return opponent query
	 */
	public Query getOpponentQuery()
	{
		return oppQuery;
	}

	/**
	 * The table is playing loose.
	 * 
	 * @param strength
	 *            bias strength
	 * @throws BayesError
	 */
	public void setLooseTable(double strength)
	{
		tableMode = LOOSE;
		bias_strength = strength;
		setParam("bias", "L");
	}

	/**
	 * The table is playing tight.
	 * 
	 * @param strength
	 *            bias strength
	 * @throws BayesError
	 */
	public void setTightTable(double strength)
	{
		tableMode = TIGHT;
		bias_strength = strength;
		setParam("bias", "T");
	}

	/**
	 * The table is playing without bias.
	 * 
	 * @throws BayesError
	 */
	public void setNormalTable()
	{
		tableMode = NORMAL;
		bias_strength = 0.5;
		setParam("bias", "N");
	}

	/**
	 * Return the group number (from 1 to 6) of the given card combination.
	 * 
	 * @param hole
	 *            hole combination
	 * @return group number
	 */
	protected int groupOf(String hole)
	{
		if (!groupMap.containsKey(hole))
			return 6;
		else
			return groupMap.get(hole);
	}

	/**
	 * Get state name corresponding to move ID.
	 * 
	 * @param move
	 *            move id
	 * @return state name
	 */
	protected String actionState(int move)
	{
		return actionStates[move];
	}

	/**
	 * Get state name corresponding to position ID.
	 * 
	 * @param pos
	 *            position id
	 * @return state name
	 */
	public static String positionState(int pos)
	{
		return positionStates[pos];
	}

	/**
	 * Get the name of the hole state for the given hand, which should contain
	 * two cards.
	 * 
	 * @param hole
	 *            hand containing hole cards
	 * @param suits
	 *            true if full suit is included, false otherwise
	 * @return name of hole node state
	 */
	protected String holeState(Hand hole, boolean suits)
	{
		List<Card> clist = hole.getCards();
		Card c1 = clist.get(0);
		Card c2 = clist.get(1);
		int i1 = c1.getIndex();
		int i2 = c2.getIndex();

		if (suits)
		{
			if (i1 < i2)
			{
				int t = i1;
				i1 = i2;
				i2 = t;
			}

			return cards[i1] + cards[i2];
		}
		else
		{
			int r1 = i1 / 4;
			int r2 = i2 / 4;
			if (r1 < r2)
			{
				int t = r1;
				r1 = r2;
				r2 = t;
			}

			if (r1 == r2)
				return ranks[r1] + ranks[r2];

			int s1 = i1 % 4;
			int s2 = i2 % 4;
			return ranks[r1] + ranks[r2] + (s1 == s2 ? "s" : "u");
		}
	}

	/**
	 * Get the move ID which results from the given action distribution. Simply
	 * pick the state with highest probability. TODO: comment PreflopNet.getMove
	 * 
	 * @param dist
	 *            distribution of actions
	 * @return move ID
	 */
	protected int getMove(Distribution dist)
	{
		int idx = -1;
		double max = 0.0;

		for (int i = 0; i < dist.states.length; i++)
			if ((idx == -1) || (max < dist.values[i]))
			{
				idx = i;
				max = dist.values[i];
			}

		return idx + 1; // FIXME: relies on F,C,R=1,2,3
	}

	public static String positionName(int pos)
	{
		return posNames[pos];
	}

	public static int holeIndex(String pn)
	{
		return holeMap.get(pn);
	}

}
