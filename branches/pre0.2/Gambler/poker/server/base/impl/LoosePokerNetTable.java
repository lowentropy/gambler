
package poker.server.base.impl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import poker.ai.bnet.PokerNet;
import poker.ai.bnet.loose.SPostflopNet;
import poker.ai.bnet.loose.SPreflopNet;
import poker.ai.core.Card;
import poker.ai.core.Hand;
import poker.common.Money;
import poker.common.PokerError;
import poker.server.base.Move;
import poker.server.base.Player;
import bayes.BayesError;
import bayes.Distribution;
import bayes.ProbFunction;

/**
 * Uses nets from poker.ai.bnet.loose to track Poker state.
 * 
 * @author lowentropy
 */
public class LoosePokerNetTable extends StateTable
{

	/** multiple by which new hand estimate is averaged with old estimate */
	private static final double				AGING_MULTIPLIER		= 2.0;

	/** default value by which table looseness is multiplied to get opp looseness */
	private static final double				OPP_DEFAULT_LOOSE_MAX	= .1;

	/**
	 * ratio of sunk cost recognized by AI
	 * 
	 * @deprecated
	 */
	private static final double				AI_SUNK_REC				= 0.0;

	/**
	 * ratio of sunk cost recognized by opponents
	 * 
	 * @deprecated
	 */
	private static final double				OPP_SUNK_REC			= 0.0;

	/**
	 * flattening values for each position
	 * 
	 * @deprecated
	 */
	private Map<String, Integer>			posflat;

	/** preflop network */
	private SPreflopNet						preflop;

	/** postflop network */
	private SPostflopNet					postflop;

	/** early and mid-action aggression */
	private int								action;

	/** indexed by hand,hole */
	private double[][]						drawOdds;

	/** indexed by hand,kicker,hole */
	private Distribution[][][]				kickers;

	/** indexed by player,hand */
	private double[][]						holeDraws;

	/** indexed by player,hole */
	private double[][]						holeDists;

	/** indexed by player,hand,kicker */
	private Distribution[][][]				holeKickers;

	/** all cards for every two-card combination */
	public static Card[][]					allHoles;

	/** observed-node maps */
	private Map<String, String>				omap;

	/** prior-node maps */
	private Map<String, Distribution>		pmap;

	/** query maps */
	private Map<String, Distribution>		qmap;

	/** style profiles */
	private Map<String, Distribution>		profStyle;

	/** bias at table */
	private Distribution					tableBias;

	/** whether raised preflop */
	private boolean							pfRaise;

	/** distribution of pot size */
	private Distribution					potSizeDist;

	/** map of full hole idex to pair index */
	private static int[]					mapFull2Pair;

	/** map of pair index to array of full hole indices */
	private static int[][]					mapPair2Full;

	/** array of replays (one for each player) */
	private Replay[]						replays;

	/** map of player name to list of style updates */
	private Map<String, List<Distribution>>	styleUpdates;

	/** map of name to name for storing profiles */
	private Map<String, String>				profileNameMap;

	/** last calculated opp draw odds */
	private double[]						lastOppDraws;

	/** last calculated opp kicker dists */
	private Distribution[][]				lastOppKicks;

	/** AI types at each seat */
	protected int[]							aiTypes;

	/** whether there was a re-raise preflop */
	private boolean							pfReRaise;
	
	/** all profiles */
	private Map<String,Profile> profiles;

	static
	{
		try
		{
			allHoles = new Card[2][1326];
			int n = 0;
			for (String s : PokerNet.fullHoles)
			{
				allHoles[0][n] = Card.fromString(s.substring(0, 2), false);
				allHoles[1][n++] = Card.fromString(s.substring(2), false);
			}
		}
		catch (PokerError e)
		{
			e.printStackTrace();
		}
		int[] pairC = new int[169];
		int[] pairI = new int[169];
		mapFull2Pair = new int[1326];
		mapPair2Full = new int[169][];

		for (int i = 0; i < 1326; i++)
		{
			String ca = allHoles[0][i].toString();
			String cb = allHoles[1][i].toString();
			int i1 = allHoles[0][i].getValue().ordinal();
			int i2 = allHoles[1][i].getValue().ordinal();
			if (ca.charAt(0) == cb.charAt(0))
				mapFull2Pair[i] = i1;
			else
			{
				String pn;
				if (ca.charAt(1) == cb.charAt(1))
					pn = (i1 > i2) ? ca.substring(0, 1) + cb.substring(0, 1)
							+ "s" : cb.substring(0, 1) + ca.substring(0, 1)
							+ "s";
				else
					pn = (i1 > i2) ? ca.substring(0, 1) + cb.substring(0, 1)
							+ "u" : cb.substring(0, 1) + ca.substring(0, 1)
							+ "u";
				mapFull2Pair[i] = PokerNet.holeIndex(pn);
			}
			pairC[mapFull2Pair[i]]++;
		}
		for (int i = 0; i < 169; i++)
			mapPair2Full[i] = new int[pairC[i]];

		for (int i = 0; i < 1326; i++)
			mapPair2Full[mapFull2Pair[i]][pairI[mapFull2Pair[i]]++] = i;
	}


	/**
	 * Constructor.
	 * 
	 * @param tableName
	 *            name of table
	 * @param playerName
	 *            name of AI player
	 * @param sbBet
	 *            small blind bet
	 * @param bbBet
	 *            big blind bet
	 * @param earlyBet
	 *            first two rounds' bets
	 * @param lateBet
	 *            last two rounds' bets
	 * @param rake
	 *            house rake from pot
	 * @throws BayesError
	 */
	public LoosePokerNetTable(String tableName, String playerName, Money sbBet,
			Money bbBet, Money earlyBet, Money lateBet, Money rake)
			throws BayesError
	{
		super(tableName, playerName, sbBet, bbBet, earlyBet, lateBet, rake);

		preflop = new SPreflopNet();
		postflop = new SPostflopNet();

		preflop.buildNetwork();
		preflop.buildQueries();
		postflop.buildNetwork();
		postflop.buildQueries();

		omap = new HashMap<String, String>();
		pmap = new HashMap<String, Distribution>();
		qmap = new HashMap<String, Distribution>();

		profStyle = new HashMap<String, Distribution>();
		profiles = new HashMap<String, Profile>();
		styleUpdates = new HashMap<String, List<Distribution>>();
		profileNameMap = new HashMap<String, String>();
		posflat = new HashMap<String, Integer>();

		tableBias = new Distribution("bias", new String[] {"T", "L"},
				new double[] {1.0, 0.0});

		drawOdds = new double[9][2652];
		kickers = new Distribution[9][][];
		int[] numKicks = new int[] {1, 2, 2, 1, 1, 2, 2, 1, 1};
		for (int i = 0; i < 9; i++)
		{
			kickers[i] = new Distribution[numKicks[i]][2652];
			for (int k = 0; k < numKicks[i]; k++)
				for (int j = 0; j < 2652; j++)
					kickers[i][k][j] = new Distribution("", PokerNet.ranks,
							new double[13]);
		}
	}


	/**
	 * @throws RemoteException
	 * @see poker.server.base.impl.StateTable#beginHand()
	 */
	protected void beginHand() throws RemoteException
	{
		action = 0; // pre: nr, r, rr; post: nb, b, r
		pfRaise = pfReRaise = false;

		// initialize holeDraws, holeDists, holeKickers
		int[] numKicks = new int[] {1, 2, 2, 1, 1, 2, 2, 1, 1};
		betsCalled = new int[numInGame];
		holeDraws = new double[numInGame][9];
		holeDists = new double[numInGame][allHoles[0].length];
		holeKickers = new Distribution[numInGame][9][];
		aiTypes = new int[numInGame];
		for (int i = 0; i < numInGame; i++)
		{
			styleUpdates.put(playerNames[i], new ArrayList<Distribution>());
			for (int j = 0; j < 9; j++)
			{
				holeKickers[i][j] = new Distribution[numKicks[j]];
				for (int k = 0; k < numKicks[j]; k++)
					holeKickers[i][j][k] = new Distribution("kick",
							PokerNet.ranks, new double[13]);
			}
		}

		loadProfiles();
		initReplay(numInGame);
	}


	/**
	 * 0
	 * 
	 * @see poker.server.base.impl.StateTable#userChatted(java.lang.String,
	 *      java.lang.String, boolean)
	 */
	protected String userChatted(String user, String text, boolean whitespace)
	{
		// do nothing!
		return null;
	}


	/**
	 * @see poker.server.base.impl.StateTable#cardsDealt(poker.ai.core.Hand)
	 * @throws RemoteException
	 */
	protected void cardsDealt(Hand cards) throws RemoteException
	{
		calculateDrawOdds();
		removeCardsFromHolePool(cards);
	}


	/**
	 * @see poker.server.base.impl.StateTable#setNormalGame()
	 */
	public void setNormalGame()
	{
		throw new UnsupportedOperationException();
		// preflop.setNormalTable();
		// extra.setNormalTable();
		// postflop.setNormalTable();
	}


	/**
	 * @see poker.server.base.impl.StateTable#setTightGame(double)
	 */
	public void setTightGame(double strength)
	{
		throw new UnsupportedOperationException();
		// preflop.setTightTable(strength);
		// extra.setTightTable(strength);
		// postflop.setTightTable(strength);
	}


	/**
	 * @see poker.server.base.impl.StateTable#setLooseGame(double)
	 */
	public void setLooseGame(double strength)
	{
		throw new UnsupportedOperationException();
		// preflop.setLooseTable(strength);
		// extra.setLooseTable(strength);
		// postflop.setLooseTable(strength);
	}


	/**
	 * @see poker.server.base.impl.StateTable#setPreflopParam(java.lang.String,
	 *      java.lang.Object)
	 */
	public void setPreflopParam(String name, Object value)
	{
		preflop.setParam(name, value);
	}


	/**
	 * @see poker.server.base.impl.StateTable#setPostflopParam(java.lang.String,
	 *      java.lang.Object)
	 */
	public void setPostflopParam(String name, Object value)
	{
		postflop.setParam(name, value);
	}


	private String flatten(int pos, int player)
	{
		String pn = playerNames[player];
		if (!posflat.containsKey(pn))
			return PokerNet.positionState(pos);
		int f = posflat.get(pn).intValue();
		if (pos == Player.POS_MID && f >= 1)
			pos = Player.POS_LATE;
		else if (pos == Player.POS_BIGBLIND && f >= 2)
			pos = Player.POS_LATE;
		else if (pos == Player.POS_SMALLBLIND && f >= 2)
			pos = Player.POS_LATE;
		else if (pos == Player.POS_EARLY && f >= 3)
			pos = Player.POS_LATE;
		else if (pos == Player.POS_UTG && f >= 4)
			pos = Player.POS_UTG;
		return PokerNet.positionState(pos);
	}


	/**
	 * @throws RemoteException
	 * @see poker.server.base.impl.StateTable#requestMove(int)
	 */
	protected Move requestMove(int player) throws RemoteException
	{
		if (holes[player] == null)
			throw new RemoteException("invalid player; hole not known");

		// int pos = extraBets ? Player.POS_LATE : position(player);
		int pos = position(player);

		int moveId = 0;
		Hand hole = holes[player];
		double[][] whp = null;
		try
		{
			switch (round) {
			case Player.PREFLOP1:
				// omap: hole, pos, in_pot, action
				// pmap: bias, style
				// qmap: strat
				clearMaps();
				omap.put("hole", holeState(hole, false));
				omap.put("pos", flatten(pos, player));
				omap.put("action", preActionState(action));
				omap.put("in_pot", inPotState());
				pmap.put("bias", getTableBias());
				pmap.put("style", getPlayerStyle(player));
				qmap.put("strat", null);
				preflop.compute("fwd", omap, pmap, qmap);
				moveId = decodeMove(qmap.get("strat"));
				break;
			case Player.PREFLOP2:
				// omap: hole, pos, in_pot, action
				// pmap: bias, style
				// qmap: strat
				clearMaps();
				omap.put("hole", holeState(hole, false));
				omap.put("pos", flatten(pos, player));
				omap.put("action", preActionState(betBackAction(player)));
				omap.put("in_pot", inPotState());
				pmap.put("bias", getTableBias());
				pmap.put("style", getPlayerStyle(player));
				qmap.put("strat", null);
				preflop.compute("fwd", omap, pmap, qmap);
				moveId = decodeMove(qmap.get("strat"));
				break;
			default:
				// omap: hole, action
				// pmap:return pot_size, style
				// qmap: strat
				clearMaps();
				omap.put("hole", holeState(hole, true));
				omap.put("action", postActionState(action));
				pmap.put("style", getPlayerStyle(player));
				pmap.put("bias", getTableBias());
				pmap.put("pot_size", potSize());
				qmap.put("strat", null);
				whp = getHolePoolWinDist(player, true);
				postflop.initHandTable(whp[1]);
				postflop.initProfitTable(whp[2]);
				postflop.compute("fwd", omap, pmap, qmap);
				moveId = decodeMove(qmap.get("strat"));
				break;
			}
		}
		catch (Exception e)
		{
			throw new RemoteException("error in pokernet", e);
		}

		if (!curBet.moreThan(bets[player]))
		{
			if ((moveId == PokerNet.FOLD) || (moveId == PokerNet.CALL))
			{
				// System.out.printf("CHECK: %s", bets[player].toString()); //
				// DBG
				return Move.check();
			}
		}

		if ((moveId == PokerNet.CALL || moveId == PokerNet.RAISE)
				&& curBet.isZero())
			return Move.bet();

		Move m = new Move(moveId);
		int hsi = holeStateIdx(hole, true);
		if (round > Player.PREFLOP2)
			m.setOdds(getLastOppOdds(), getLastOppKicks(),
					getLastUserOdds(hsi), getLastUserKicks(hsi), getLastWins(
							whp[0], hsi), getLastProfit(whp[2], hsi),
					getLastHandDist(whp[1], hsi), postflop.lastMoveDist,
					getToCall(player, true), pot.subtract(rake).toDouble(),
					52 - (board.size() + 2));
		return m;
	}


	private double getToCall(int player, boolean isAi)
	{
		double tc = curBet.subtract(bets[player]).toDouble();
		if (tc == 0)
			tc = (round < Player.POSTTURN) ? earlyBet.toDouble() : lateBet
					.toDouble();
		tc += bets[player].toDouble() * (isAi ? AI_SUNK_REC : OPP_SUNK_REC);
		return tc;
	}


	private double[] getLastWins(double[] ds, int hsi)
	{
		return getLastProfit(ds, hsi);
	}


	private double[] getLastProfit(double[] wh, int hsi)
	{
		int len = wh.length / 3;
		double[] lp = new double[3];
		for (int i = 0; i < 3; i++)
			lp[i] = wh[i * len + hsi];
		return lp;
	}


	private double[] getLastHandDist(double[] wh, int hsi)
	{
		int len = wh.length / 9;
		double[] lh = new double[9];
		for (int i = 0; i < 9; i++)
			lh[i] = wh[i * len + hsi];
		return lh;
	}


	private int[][] getLastOppKicks()
	{
		int[][] ok = new int[9][3];
		for (int i = 0; i < 9; i++)
			for (int j = 0; j < lastOppKicks[i].length; j++)
				ok[i][j] = lastOppKicks[i][j].biggest();
		return ok;
	}


	private double[] getLastUserOdds(int hsi)
	{
		double[] ud = new double[9];
		for (int i = 0; i < 9; i++)
			ud[i] = drawOdds[i][hsi];
		return ud;
	}


	private int[][] getLastUserKicks(int hsi)
	{
		int[][] uk = new int[9][3];
		for (int i = 0; i < 9; i++)
			for (int j = 0; j < kickers[i].length; j++)
				uk[i][j] = kickers[i][j][hsi].biggest();
		return uk;
	}


	private double[] getLastOppOdds()
	{
		double[] oo = new double[9];
		for (int i = 0; i < 8; i++)
			oo[i + 1] = lastOppDraws[i];
		oo[0] = 1.0;
		return oo;
	}


	/**
	 * Get the postflop playing style for this player. If a profile exists, use
	 * that, otherwise return normal style.
	 * 
	 * @param player
	 *            player index
	 * @return style state name{
	 */
	private Distribution getPlayerStyle(int player)
	{
		String name = playerNames[player];
		if (!profStyle.containsKey(name))
			return getProjectedStyle(player);
		else
			return profStyle.get(name);
	}


	/**
	 * Get the style distribution which has been projected for the given player.
	 * 
	 * @param player
	 *            player index
	 * @return style distribution
	 */
	private Distribution getProjectedStyle(int player)
	{
		return getProfile(playerNames[player]).getStyle();
	}


	/**
	 * Get the action state for bets back to the player.
	 * 
	 * @param player
	 *            player index
	 * @return action state index
	 */
	private int betBackAction(int player)
	{
		int action = numBets - betsCalled[player];
		if (action > 2)
			action = 2;
		return action;
	}


	/**
	 * @return table bias
	 */
	private Distribution getTableBias()
	{
		return tableBias;
	}


	/**
	 * Set the table bias.
	 */
	public void setTableBias(Distribution bias)
	{
		tableBias = bias;
	}


	/**
	 * State name for players in pot: not_3, not_4, 4_more.
	 * 
	 * @return name of in_pot state
	 */
	private String inPotState()
	{
		int numEntered = numActed - numFolded;
		if (numEntered < 3)
			return "not_3";
		else if (numEntered < 4)
			return "not_4";
		else
			return "4_more";
	}


	/**
	 * Sets a player profile. Should be called before a hand is begun.
	 * 
	 * @param name
	 *            name of player
	 * @param bias
	 *            preflop bias
	 * @param style
	 *            postflop style
	 */
	public void setPlayerProfile(String name, Distribution style)
	{
		// System.out.printf("IN setPlayerProfile(): %s = %s\n", name, style,
		// style.toString()); // DBG
		profStyle.put(name, style);
	}


	/**
	 * Name of preflop action state.
	 * 
	 * @param action
	 *            action at table
	 * @return state name
	 */
	private String preActionState(int action)
	{
		switch (action) {
		case 0:
			return "NR";
		case 1:
			return "R";
		case 2:
			return "RR";
		default:
			return null;
		}
	}


	/**
	 * Decode the move represented by the distribution. May be a preflop or a
	 * postflop strat distribution, which differ.
	 * 
	 * @param distribution
	 *            posterior distribution
	 * @return move id
	 */
	private int decodeMove(Distribution distribution)
	{
		double[] dist = distribution.getData();
		if (dist[0] > .5)
			return PokerNet.FOLD;
		else if (dist.length == 5 && dist[1] > .5)
			return PokerNet.CHECK;
		int b = (dist.length == 5) ? 2 : 1;
		int maxi = b;
		double max = dist[b];
		for (int i = b + 1; i < dist.length; i++)
			if (dist[i] > max)
			{
				maxi = i;
				max = dist[i];
			}

		String s = distribution.states[maxi];
		if (s.equals("F"))
			return PokerNet.FOLD;
		else if (s.equals("CH"))
			return PokerNet.CHECK;
		else if (s.equals("B"))
			return PokerNet.BET;
		else if (s.equals("C"))
			return PokerNet.CALL;
		else
			return PokerNet.RAISE;
	}


	/**
	 * @return distribution of states of pot_size: small, large
	 */
	private Distribution potSize()
	{
		return potSizeDist;
	}


	/**
	 * @see poker.server.base.impl.StateTable#preflopEnded()
	 */
	protected void preflopEnded()
	{
		/*
		 * ... begin to consider the pot to be large if one of the following is
		 * true: 1. It is six-handed or more preflop. 2. It is raised preflop
		 * and four-handed or more. 3. It is three-bet or more preflop. 4. At
		 * least two of your opponents will usually go to the river. -- p. 145
		 */
		int numPf = numInRound - 2;

		double d;
		if (pfReRaise)
			d = 1.0;
		else if (pfRaise)
			d = (double) numPf / 4.0;
		else
			d = (double) numPf / 6.0;

		if (d > 1.0)
			d = 1.0;

		potSizeDist = new Distribution("pot_size", new String[] {"small",
				"large"}, new double[] {1.0 - d, d});
		action = 0;
	}


	/**
	 * Name of postflop action state.
	 * 
	 * @param action
	 *            action at table
	 * @return state name
	 */
	private String postActionState(int action)
	{
		switch (action) {
		case 0:
			return "NB";
		case 1:
			return "B";
		case 2:
			return "R";
		default:
			return null;
		}
	}


	/**
	 * Clear the network compute() maps.
	 */
	private void clearMaps()
	{
		omap.clear();
		pmap.clear();
		qmap.clear();
	}


	/**
	 * @throws RemoteException
	 * @see poker.server.base.impl.StateTable#endHand(boolean,
	 *      poker.common.Money, java.util.Map)
	 */
	protected Move endHand(boolean won, Money net, Map<String, Distribution> handMap)
			throws RemoteException
	{
		for (String player : handMap.keySet())
			for (int idx = 0; idx < playerNames.length; idx++)
				if (playerNames[idx].equals(player))
					replay(idx, handMap.get(player));

		mergeProjectedStyles();
		storeProfiles();

		return Move.stayAtTable();
	}


	/**
	 * Replay the moves made by a player, with the knowledge of his hand, to
	 * determine a set of new values for his style.
	 * 
	 * @param player
	 *            player index
	 * @param dist
	 *            actual hand
	 * @throws RemoteException
	 */
	private void replay(int player, Distribution dist) throws RemoteException
	{
		Replay r = replays[player];
		int num = r.numStates();
		for (int i = 0; i < num; i++)
		{
			clearMaps();
			omap.putAll(r.getObsMap(i));
			pmap.putAll(r.getPriorMap(i));
			pmap.remove("hole");
			pmap.remove("style");
			boolean pre = r.getPreflop(i);
			pmap.put("hole", convertDistTo(dist, pre));

			if (pre)
			{
				qmap.put("style", null);
				preflop.compute("style", omap, pmap, qmap);
				updateProjectedStyle(player, qmap.get("style"));
			}
			else
			{
				qmap.put("style", null);
				double[][] wh = r.getPostflopDists(i);
				try
				{
					postflop.initProfitTable(wh[0]);
					postflop.initHandTable(wh[1]);
				}
				catch (BayesError e)
				{
					throw new RemoteException("could not replay player", e);
				}
				postflop.compute("style", omap, pmap, qmap);
				updateProjectedStyle(player, qmap.get("style"));
			}
		}
	}


	private Distribution convertDistTo(Distribution dist, boolean pre)
	{
		return pre ? convertDistToPair(dist.values) : convertDistToFull(dist.values); 
	}


	/**
	 * Record an update to the player style. Do not actually process the updates
	 * yet, just put them on a list so that mergeProjectedStyles() can use them.
	 * 
	 * @param player
	 *            player to update style for
	 * @param d
	 *            distribution to merge
	 */
	private void updateProjectedStyle(int player, Distribution d)
	{
		if (d != null)
			styleUpdates.get(playerNames[player]).add(d);
	}


	/**
	 * Merge style updates over the round by averaging them and stacking them on
	 * top of the old projected style with an aging multiplier.
	 */
	private void mergeProjectedStyles()
	{
		for (int player = 0; player < numInGame; player++)
		{
			List<Distribution> list = styleUpdates.get(playerNames[player]);
			if (list.isEmpty())
			{
				makeFlatProjection(player);
				continue;
			}

			Distribution d = list.get(0);
			d = d.copyAndZero();
			for (Distribution e : list)
				d.addInMultiplied(e, 1.0);
			list.clear();
			d.normalize();

			String name = playerNames[player];
			getProfile(name).addEstimate(d, true);
		}
	}

	
	private void makeFlatProjection(int player)
	{
		// TODO
	}


	private Profile getProfile(String name)
	{
		if (!profiles.containsKey(name))
			profiles.put(name, new Profile(name));
		return profiles.get(name);
	}

	/**
	 * @see poker.server.base.impl.StateTable#leaveTable()
	 */
	public void leaveTable()
	{
		// do nothing
	}


	/**
	 * @throws RemoteException
	 * @see poker.server.base.impl.StateTable#playerMoved(int,
	 *      poker.server.base.Move)
	 */
	protected void playerMoved(int player, Move move) throws RemoteException
	{
		int pos = position(player);

		/* modify networks by querying opp hole densities and adding to pool */
		if (!move.isFold())
		{
			if (round < Player.POSTFLOP && move.isRaise())
			{
				if (pfRaise)
					pfReRaise = true;
				else
					pfRaise = true;
			}

			try
			{
				double[][] whp = null;
				switch (round) {
				case Player.PREFLOP1:
					clearMaps();

					// BB check; read like a call
					if (move.isCheck())
						move = move.call();

					omap.put("pos", flatten(pos, player));
					omap.put("action", preActionState(action));
					omap.put("in_pot", inPotState());
					omap.put("strat", move.state());
					pmap.put("bias", getTableBias());
					pmap.put("style", getProjectedStyle(player));
					qmap.put("hole", null);
					preflop.compute("hole", omap, pmap, qmap);
					updateHolePool(player, qmap.get("hole"), false, true);
					break;
				case Player.PREFLOP2:
					clearMaps();
					// omap: pos, action, in_pot, strat
					// pmap: bias, style
					// qmap: hole
					omap.put("pos", flatten(pos, player));
					omap.put("action", preActionState(betBackAction(player)));
					omap.put("in_pot", inPotState());
					omap.put("strat", move.state());
					pmap.put("bias", getTableBias());
					pmap.put("style", getProjectedStyle(player));
					qmap.put("hole", null);
					preflop.compute("hole", omap, pmap, qmap);
					updateHolePool(player, qmap.get("hole"), false, false);
					break;
				default:
					clearMaps();
					// omap: action, strat
					// pmap: pot_size, style
					// qmap: hole
					omap.put("action", postActionState(action));
					omap.put("strat", move.state());
					pmap.put("bias", getTableBias());
					pmap.put("pot_size", potSizeDist);
					pmap.put("style", getProjectedStyle(player));
					qmap.put("hole", null);
					whp = getHolePoolWinDist(player, false);
					postflop.initHandTable(whp[1]);
					postflop.initProfitTable(whp[2]);
					postflop.compute("hole", omap, pmap, qmap);
					updateHolePool(player, qmap.get("hole"), true, false);
					break;
				}
				saveReplay(player, omap, pmap, whp, round < Player.POSTFLOP);
			}
			catch (Exception e)
			{
				throw new RemoteException("error in pokernet", e);
			}

			if (move.isBet() || move.isRaise())
			{
				action++;
				if (action > 2)
					action = 2;
			}
		}
		else if (move.isFold())
			removeFromHolePool(player);
	}


	/**
	 * Convert hole distribution data to pair (preflop) form. Wrap in
	 * distribution object.
	 * 
	 * @param dist
	 *            distribution data
	 * @return distribution object
	 */
	private Distribution convertDistToPair(double[] dist)
	{
		Distribution d = new Distribution("hole", PokerNet.holePairs, null);
		if (dist.length == 169)
			d.values = dist;
		else
		{
			d.values = new double[169];
			for (int i = 0; i < dist.length; i++)
				d.values[mapFull2Pair[i]] += dist[i];
		}
		return d;
	}


	/**
	 * Convert hole distribution data to full (postflop) form. Wrap in
	 * distribution object.
	 * 
	 * @param dist
	 *            distribution data
	 * @return distribution object
	 */
	private Distribution convertDistToFull(double[] dist)
	{
		Distribution d = new Distribution("hole", PokerNet.fullHoles, null);
		if (dist.length == 1326)
			d.values = dist;
		else
		{
			d.values = new double[1326];
			for (int i = 0; i < dist.length; i++)
			{
				int n = mapPair2Full[i].length;
				for (int k = 0; k < n; k++)
				{
					int j = mapPair2Full[i][k];
					d.values[j] = dist[i] / (double) n;
				}
			}
		}
		return d;
	}


	public static void printDrawsAndKickers(int p, double[] oppDraws,
			Distribution[][] oppKicks)
	{
		System.out.printf("Draw odds/kickers for player %d:\n", p);
		System.out.printf("\thigh: 100.00%%\n");
		for (Distribution d : oppKicks[0])
			d.print("\t\t");
		for (int i = 0; i < 8; i++)
		{
			System.out.printf("\t%s: %6.2f%%\n", PokerNet.hands[i + 1],
					oppDraws[i]);
			for (Distribution d : oppKicks[i + 1])
				d.print("\t\t");
		}

	}


	/**
	 * Get the win distribution for the given player (equal to the conditional
	 * distribution of winning the pot based on the hole cards of that player).
	 * 
	 * @param player
	 *            player index
	 * @return array of win odds given hole combination
	 */
	private double[][] getHolePoolWinDist(int player, boolean isAi)
	{
		int len = allHoles[0].length;
		double[] wins = new double[len * 3];
		double[] hands = new double[len * 9];
		double[] oppDraws = getHolePoolOppDraws(player);
		Distribution[][] oppKicks = getHolePoolOppKickers(player);
		lastOppDraws = oppDraws;
		lastOppKicks = oppKicks;

		double[] b = new double[9];
		double[] s = new double[9];
		double[] h = new double[9];
		double[] g = new double[9];
		double[] og = new double[9];
		double[] ag = new double[9];

		double[] aw = new double[9];
		double[] al = new double[9];
		double wt = 0.0, lt = 0.0;

		for (int i = 0; i < len; i++)
		{
			for (int j = 0; j < 9; j++)
			{
				h[j] = 0.0;
				double e = b[j] = 0.0;
				int nk = kickers[j].length;
				double[] u0 = kickers[j][0][i].values;
				double[] o0 = oppKicks[j][0].values;
				for (int i0 = 0; i0 < 13; i0++)
				{
					for (int t0 = 0; t0 < i0; t0++)
						b[j] += u0[i0] * o0[t0];
					double base0 = u0[i0] * o0[i0];
					if (nk > 1)
					{
						double[] u1 = kickers[j][1][i].values;
						double[] o1 = oppKicks[j][1].values;
						for (int i1 = 0; i1 < 13; i1++)
						{
							for (int t1 = 0; t1 < i1; t1++)
								b[j] += base0 * u1[i1] * o1[t1];
							double base1 = base0 * u1[i1] * o1[i1];
							if (nk > 2)
							{
								double[] u2 = kickers[j][2][i].values;
								double[] o2 = oppKicks[j][2].values;
								for (int i2 = 0; i2 < 13; i2++)
								{
									for (int t2 = 0; t2 < i2; t2++)
										b[j] += base1 * u2[i2] * o2[t2];
									double base2 = base1 * u2[i2] * o2[i2];
									e += base2;
								}
							}
							else
								e += base1;
						}
					}
					else
						e += base0;
				}

				s[j] = 1.0 - (b[j] + e);
				// bcheck(s[j], b[j]);
			}

			double L = 0.0, W = 0.0;
			double ht = 0.0;

			double _g = 1.0, _og = 1.0;
			for (int j = 8; j >= 0; j--)
			{
				double _t = drawOdds[j][i];
				ag[j] += g[j] = _t * _g;
				_g *= (1.0 - _t);
				_t = (j == 0) ? 1.0 : oppDraws[j - 1];
				og[j] = _t * _og;
				_og *= (1.0 - _t);
			}
			// redo

			for (int j = 0; j < 9; j++)
			{
				double p = 1.0;
				double q = 1.0;
				double u = g[j];
				double o = og[j];
				for (int k = j + 1; k < 9; k++)
				{
					q *= (1.0 - drawOdds[k][i]);
					p *= (1.0 - oppDraws[k - 1]);
				}

				double w = (1.0 - o) * p + o * p * b[j];
				double l = (1.0 - p) + o * p * s[j];

				// w *= q;
				// l *= q;

				// bcheck(p,q,u,o,w,l);

				aw[j] += w;
				al[j] += l;

				L += l * g[j];
				W += w * g[j];
				ht += h[j] = g[j];
			}
			double E = 1.0 - W - L;
			wins[(0 * len) + i] = L;
			wins[(1 * len) + i] = E;
			wins[(2 * len) + i] = W;

			for (int j = 0; j < 9; j++)
				hands[(j * len) + i] = h[j] / ht;
		}

		double tc = getToCall(player, isAi);
		double tw = pot.subtract(rake).toDouble();
		double po = tc / (tw + tc);
		double[] profit = calculateProfit(wins, po);

		return new double[][] {wins, hands, profit};
	}


	/**
	 * Calculate profitability of every hand.
	 * 
	 * @param wins
	 * @param po
	 * @return
	 */
	private double[] calculateProfit(double[] wins, double po)
	{
		int len = wins.length / 3;
		double[] profit = new double[wins.length];

		for (int i = 0; i < len; i++)
		{
			double N, E, D;
			double l = wins[(0 * len) + i];
			double e = wins[(1 * len) + i];
			double w = wins[(2 * len) + i];
			double W = w + (e / 2.0);
			if (W >= po)
			{
				N = 0.0;
				double U = po * (Math.pow(2.0, numBets + 1));
				if (W < U)
				{
					E = (U - W) / (U - po);
					D = (W - po) / (U - po);
				}
				else
				{
					E = 0.0;
					D = 1.0;
				}
			}
			else
			{
				D = 0.0;
				if (W < (po / 2.0))
				{
					N = 1.0;
					E = 0.0;
				}
				else
				{
					// soft-neg up to half loss
					N = 0.5 + ((po - W) / po);
					E = 1.0 - N;
				}
			}
			profit[(0 * len) + i] = N;
			profit[(1 * len) + i] = E;
			profit[(2 * len) + i] = D;
		}
		return profit;
	}


	private void bcheck(double... D)
	{
		for (int i = 0; i < D.length; i++)
		{
			if (D[i] < -0.0 || D[i] > 1.0)
				System.err.printf("arg %d of bcheck is %8.4f\n", i, D[i]);
		}
	}


	/**
	 * Given the current board, calculate draw odds for each hand, for each
	 * pocket combination.
	 */
	private void calculateDrawOdds()
	{
		for (int i = 0; i < allHoles[0].length; i++)
		{
			board.calculateDrawOdds(i, drawOdds, kickers, allHoles[0][i],
					allHoles[1][i]);
		}
	}


	/**
	 * Get opp draws from the perspective of the given player by combining the
	 * opp draws of each remaining opponent.
	 * 
	 * @param player
	 *            player index
	 * @return opp draw odds
	 */
	private double[] getHolePoolOppDraws(int player)
	{
		// num hands is 8, plus high card
		double[] draws = new double[8];

		/* first calculate total draw odds */
		for (int i = 0; i < 8; i++)
		{
			double d = 1.0;
			for (int j = 0; j < numInGame; j++)
				if ((j != player) && !folded[j])
					d *= (1.0 - holeDraws[j][i + 1]);
			draws[i] = (1.0 - d);
		}
		return draws;
	}


	/**
	 * Get kicker distributions for opponents for every hand, by taking a
	 * weighted average of individual opponents' kicker distributions given
	 * their probability of making that hand.
	 * 
	 * @param player
	 *            player index
	 * @return opp kicker distributions (indexed by hand,kicker)
	 */
	private Distribution[][] getHolePoolOppKickers(int player)
	{
		if (folded[player])
			throw new Error("getHolePoolOppKickers() for folded player "
					+ player);

		/* TODO: use a global array of kickers */
		Distribution[][] kickers = new Distribution[9][];

		for (int i = 0; i < 9; i++)
		{
			kickers[i] = new Distribution[holeKickers[player][i].length];

			for (int j = 0; j < kickers[i].length; j++)
			{
				kickers[i][j] = holeKickers[player][i][j].copyAndZero();

				for (int k = 0; k < numInGame; k++)
				{
					if ((k != player) && !folded[k])
						kickers[i][j].addInMultiplied(holeKickers[k][i][j],
								holeDraws[k][i]);
				}

				kickers[i][j].normalize();
			}
		}

		return kickers;
	}


	/**
	 * Update the hole distribution for the given player and recalculate that
	 * player's draw odds and kickers.
	 * 
	 * @param player
	 *            player index
	 * @param holeDensity
	 *            density of player's hole cards
	 */
	private void updateHolePool(int player, Distribution holeDensity,
			boolean updateDraws, boolean firstUpdate)
	{
		if (holeDensity.values.length != 1326)
			holeDensity = convertDistToFull(holeDensity.values);

		if (firstUpdate)
			for (int i = 0; i < allHoles[0].length; i++)
				holeDists[player][i] = holeDensity.values[i];
		else
			for (int i = 0; i < allHoles[0].length; i++)
				holeDists[player][i] = (holeDists[player][i] + holeDensity.values[i]) / 2.0;

		if (updateDraws)
			updateDraws(player);
	}


	/**
	 * Update the draw odds and kickers for the given player.
	 * 
	 * @param player
	 *            player index
	 */
	private void updateDraws(int player)
	{
		// System.out.printf("udpated draws and kickers for player %d:\n",
		// player);
		for (int i = 0; i < 9; i++)
		{
			int K = holeKickers[player][i].length;
			holeDraws[player][i] = 0.0;
			for (int k = 0; k < K; k++)
				holeKickers[player][i][k].zero();
			double d = 0.0;
			for (int j = 0; j < allHoles[0].length; j++)
			{
				double t = drawOdds[i][j] * holeDists[player][j];
				d += t;
				for (int k = 0; k < K; k++)
				{ // DBG
					if (holeKickers[player][i][k] == null)
						throw new Error();
					else if (kickers[i][k][j] == null)
						throw new Error();
					holeKickers[player][i][k].addInMultiplied(kickers[i][k][j],
							t);
				}
			}
			holeDraws[player][i] = d;
			for (int k = 0; k < K; k++)
				holeKickers[player][i][k].normalize();

			// DBG
			// System.out.printf("\t%6.2f%%\n", holeDraws[player][i] * 100.0);
			// for (int j = 0; j < holeKickers[player][i].length; j++)
			// System.out.printf("\t\t%s\n",
			// holeKickers[player][i][j].toString());
		}
	}


	/**
	 * Remove from every player's hole card densities the given cards, which are
	 * seen and so are not part of any player's hand. For the AI player, do not
	 * remove the visible hole cards, as these cards are invisible to opponents
	 * and don't factor into their calculations. After cards are removed,
	 * reculculate players' draw odds (but not total draw odds).
	 * 
	 * @param cards
	 *            cards to remove from all distributions
	 */
	private void removeCardsFromHolePool(Hand cards)
	{
		ProbFunction f = postflop.getHoleNode().getFunction();

		for (Card c : cards.getCards())
		{
			for (int i = 0; i < numInGame; i++)
			{
				if (folded[i])
					continue;
				for (int j = 0; j < allHoles[0].length; j++)
				{
					if (allHoles[0][j].equals(c) || allHoles[1][j].equals(c))
						holeDists[i][j] = 0.0;
				}

				// normalize player's hole dists
				Distribution d = new Distribution("", PokerNet.fullHoles,
						holeDists[i]);
				d.normalize();
			}

			for (int j = 0; j < allHoles[0].length; j++)
			{
				if (allHoles[0][j].equals(c) || allHoles[1][j].equals(c))
					f.getData()[j] = 0.0;
			}
		}

		for (int i = 0; i < numInGame; i++)
			if (!folded[i])
				updateDraws(i);

		f.normalize();
	}


	/**
	 * Remove the given player from the hole pool altogether, as that player has
	 * folded.
	 * 
	 * @param player
	 *            player index
	 */
	private void removeFromHolePool(int player)
	{
		holeDists[player] = null;
		holeDraws[player] = null;
		holeKickers[player] = null;
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

			return PokerNet.cards[i1] + PokerNet.cards[i2];
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
				return PokerNet.ranks[r1] + PokerNet.ranks[r2];

			int s1 = i1 % 4;
			int s2 = i2 % 4;
			return PokerNet.ranks[r1] + PokerNet.ranks[r2]
					+ (s1 == s2 ? "s" : "u");
		}
	}


	private int holeStateIdx(Hand hole, boolean suits)
	{
		String hs = holeState(hole, suits);
		String[] arr = suits ? PokerNet.fullHoles : PokerNet.holePairs;
		for (int i = 0; i < arr.length; i++)
			if (arr[i].equals(hs))
				return i;
		return -1;
	}


	private void initReplay(int numInGame)
	{
		replays = new Replay[numInGame];
		for (int i = 0; i < numInGame; i++)
			replays[i] = new Replay();
	}


	private void saveReplay(int player, Map<String, String> omap,
			Map<String, Distribution> pmap, double[][] wh, boolean preflop)
	{
		replays[player].add(omap, pmap, wh, preflop);
	}


	/**
	 * @see poker.server.base.impl.StateTable#correctName(int, java.lang.String,
	 *      java.lang.String)
	 */
	protected void correctName(int pos, String from, String to)
	{
		profileNameMap.put(from, to);
	}


	/**
	 * Load profiles for all players in this round into the projected styles.
	 * 
	 * @throws RemoteException
	 */
	private void loadProfiles() throws RemoteException
	{
		for (String name : playerNames)
			getProfile(name);
	}


	/**
	 * Store all accumulated projected styles, then flush them.
	 * 
	 * @throws RemoteException
	 */
	private void storeProfiles() throws RemoteException
	{
		for (String on : profileNameMap.keySet())
		{
			String nn = profileNameMap.get(on);
			Profile p = profiles.get(on);
			if (p == null)
				continue;
			
			getProfile(nn).merge(p);
			profiles.remove(on);
			p.delete();
		}
		
		for (Profile p : profiles.values())
			p.save();
		
		profileNameMap.clear();
	}


	public void setBias(Distribution dist)
	{
		this.setTableBias(dist);
	}


	@Override
	protected void postflopEnded()
	{
		action = 0;
	}


	@Override
	protected void postturnEnded()
	{
		action = 0;
	}


	@Override
	public void setTableParam(String cxt, String name, Object val)
	{
		if (cxt.equals("pre"))
			setPreflopParam(name, val);
		else if (cxt.equals("post"))
			setPostflopParam(name, val);
		else if (cxt.equals("style"))
			setPlayerProfile(name, (Distribution) val);
		else if (cxt.equals("flat"))
			setPlayerPositionFlatten(name, (Integer) val);
	}


	private void setPlayerPositionFlatten(String name, Integer f)
	{
		posflat.put(name, f);
	}


	public void setAiType(int hid, int player, int type) throws RemoteException
	{
		checkHandId(hid);
		if (player < 0 || player >= numInGame)
			throw new RemoteException("illegal player: " + player);
		if (type > Player.AI_MAX)
			throw new RemoteException("illegal AI: " + type);
		aiTypes[player] = type;
	}
}
