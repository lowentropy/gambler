
package poker.server.base.impl;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import bayes.Distribution;

import poker.ai.bnet.PokerNet;
import poker.ai.core.Hand;
import poker.common.Money;
import poker.common.PokerError;
import poker.server.base.Move;
import poker.server.base.Player;

/**
 * StateTable takes care of most Player messages by updating the state of a
 * table, and being able to provide information about the state to descendent
 * class objects. It has abstract methods for implementations which override
 * default behaviors for these messages, and also leaves the requestMove method
 * abstract.
 * 
 * @author lowentropy
 */
public abstract class StateTable
{

	private static boolean	doUtg	= false;

	/** name of the table */
	protected String		tableName;

	/** name of the AI player at the table */
	protected String		playerName;

	/** index of AI player at the table */
	protected int			playerIdx;

	/** ID of the current hand */
	protected int			handId;

	/** whether a hand is currently being played */
	protected boolean		playingHand;

	/** community board cards */
	protected Hand			board;

	/** name of all the players at the table, index 0 = dealer */
	protected String[]		playerNames;

	/** observed holes */
	protected Hand[]		holes;

	/** the current bet by each player in this round */
	protected Money[]		bets;

	/** the amount spent by each player over the whole round */
	protected Money[]		spent;

	/** whether each player has folded in any round */
	protected boolean[]		folded;

	/** whether each player has gone all-in */
	protected boolean[]		allin;

	/** current round ID */
	protected int			round;

	/** whether action has moved around table */
	protected boolean		extraBets;

	/** the index of the next player expected to act in the current round */
	protected int			nextToAct;

	/** number of players in this hand */
	protected int			numInGame;

	/** number of players at the start of the current round */
	protected int			numInRound;

	/** number of players who have folded in any round */
	protected int			numFolded;

	/** number of players who have checked in the current round */
	protected int			numChecked;

	/** number of players who have bet in the current round */
	protected int			numActed;

	/** whether the next player to act is allowed to check */
	protected boolean		canCheck;

	/** the smallest amount any player has bet this round */
	protected Money			minBet;

	/** the current bet which must be called */
	protected Money			curBet;

	/** the amount of the small blind bet */
	protected Money			sbBet;

	/** the amount of the big blind bet */
	protected Money			bbBet;

	/** the bet/raise amount in the preflop/postflop rounds */
	protected Money			earlyBet;

	/** the bet/raise amount in the postturn/postriver rounds */
	protected Money			lateBet;

	/** the amount the house takes from every pot */
	protected Money			rake;

	/** the current amount of the pot */
	protected Money			pot;

	/** the average players-per-flop percentage (0.0-1.0) */
	protected double		avgPpf;

	/** last recorded players-per-flop percentage (0.0-1.0) */
	protected double		lastPpf;

	/** the average amount of the pot when the game is over */
	protected Money			avgPot;

	/** counter for doing running averages */
	protected int			avgCounter;

	/** whether betting is capped in this round */
	protected boolean		betsCapped;

	/** current number of bets (current round) */
	protected int			numBets;

	/** number of bets called by each player (current round) */
	protected int[]			betsCalled;

	/** whether current playerMoved is a result of auto-check-folding */
	private boolean			checkFolding;

	/** number of players who have gone all-in */
	private int				numAllIn;

	/** number of new all-in players this round */
	private int				newNumAllIn;

	/** whether in the current round, new cards have been put down */
	private boolean			gotRoundCards;


	/**
	 * Constructor. Should be called first by all descendent class constructors.
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
	 *            pre-turn bet amount
	 * @param lateBet
	 *            post-turn bet amount
	 * @param rake
	 *            house rake amount
	 */
	public StateTable(String tableName, String playerName, Money sbBet,
			Money bbBet, Money earlyBet, Money lateBet, Money rake)
	{
		this.tableName = tableName;
		this.playerName = playerName;
		this.sbBet = sbBet;
		this.bbBet = bbBet;
		this.earlyBet = earlyBet;
		this.lateBet = lateBet;
		this.rake = rake;
		this.handId = 0;
		this.playingHand = false;
		this.avgPot = new Money(0, 0);
		this.avgPpf = 0.0;
		this.avgCounter = 0;
		this.minBet = new Money(0, 0);
		this.pot = new Money(0, 0);
		this.curBet = new Money(0, 0);

		try
		{
			this.board = new Hand(new String[0]);
		}
		catch (PokerError e)
		{
			e.printStackTrace();
		}
	}


	/**
	 * Preflop round has ended.
	 */
	protected abstract void preflopEnded();


	/**
	 * Postflop round has ended.
	 */
	protected abstract void postflopEnded();


	/**
	 * Postturn round has ended.
	 */
	protected abstract void postturnEnded();


	protected abstract void correctName(int pos, String from, String to);


	/**
	 * @see poker.server.base.Player#userChatted(int, int, java.lang.String,
	 *      java.lang.String, boolean)
	 */
	protected abstract String userChatted(String user, String text,
			boolean whitespace);


	/**
	 * @throws RemoteException
	 * @see poker.server.base.Player#beginHand(int, java.lang.String[],
	 *      poker.common.Money[])
	 */
	protected abstract void beginHand() throws RemoteException;


	/**
	 * @throws RemoteException
	 * @see poker.server.base.Player#cardsDealt(int, int, int,
	 *      poker.ai.core.Hand)
	 */
	protected abstract void cardsDealt(Hand cards) throws RemoteException;


	/**
	 * @see poker.server.base.Player#setNormalGame(int)
	 */
	public abstract void setNormalGame();


	/**
	 * @see poker.server.base.Player#setTightGame(int, double)
	 */
	public abstract void setTightGame(double strength);


	/**
	 * @see poker.server.base.Player#setLooseGame(int, double)
	 */
	public abstract void setLooseGame(double strength);


	/**
	 * @throws RemoteException
	 * @see poker.server.base.Player#requestMove(int, int, int, int)
	 */
	protected abstract Move requestMove(int player) throws RemoteException;


	/**
	 * @throws RemoteException
	 * @see poker.server.base.Player#endHand(int, int, boolean, int[], int[],
	 *      poker.ai.core.Hand[], poker.common.Money)
	 */
	protected abstract Move endHand(boolean won, Money net,
			Map<String, Distribution> handMap) throws RemoteException;


	/**
	 * @see poker.server.base.Player#leaveTable(int)
	 */
	public abstract void leaveTable();


	/**
	 * @throws RemoteException
	 * @see poker.server.base.Player#playerMoved(int, int, int,
	 *      poker.server.base.Move)
	 */
	protected abstract void playerMoved(int player, Move move)
			throws RemoteException;


	/**
	 * Set a parameter on the preflop network.
	 * 
	 * @param name
	 *            param name
	 * @param value
	 *            param value
	 */
	public abstract void setPreflopParam(String name, Object value);


	/**
	 * Set a parameter on the postflop network.
	 * 
	 * @param name
	 *            param name
	 * @param value
	 *            param value
	 */
	public abstract void setPostflopParam(String name, Object value);


	/**
	 * Make sure the given handId (passed from a Player message) is equal to the
	 * current hand's id.
	 * 
	 * @param handId
	 *            passed hand id
	 * @throws RemoteException
	 *             if the hand id is not for the current hand
	 */
	protected void checkHandId(int handId) throws RemoteException
	{
		if (handId != this.handId)
			throw new RemoteException(
					"table message from wrong hand; expected + " + this.handId
							+ ", but was " + handId);
	}


	/**
	 * Make sure the given rouindId (passed from a Playter message) is equal to
	 * the current round.
	 * 
	 * @param roundId
	 *            passed round id
	 * @throws RemoteException
	 *             if the round id is not for the current round
	 */
	private void checkRoundId(int roundId) throws RemoteException
	{
		if (roundId != this.round)
			throw new RemoteException(
					"table message from wrong round; expected " + this.round
							+ ", but was " + roundId);
	}


	/**
	 * The current hand is cut short by a new hand. Perform cleanup/logging.
	 */
	private void terminateHand()
	{
		// TODO: terminateHand()
	}


	/**
	 * Get the minimum bet any player has made this round.
	 */
	private void getMinBet()
	{
		boolean first = true;

		for (int i = 0; i < numInGame; i++)
		{
			if (folded[i] || allin[i])
				continue;

			if (first || (minBet.moreThan(bets[i])))
				minBet.setTo(bets[i]);

			first = false;
		}
	}


	/**
	 * Get the position of the given player: small blind, big blind, early,
	 * middle, or late.
	 * 
	 * @param player
	 *            player index
	 * @return position id
	 */
	protected int position(int player)
	{
		if (round < Player.POSTFLOP)
		{
			if (player == 0)
				return Player.POS_LATE;
			else if (player == 1)
				return Player.POS_SMALLBLIND;
			else if (player == 2)
				return Player.POS_BIGBLIND;
			else if (doUtg && player == 3)
				return Player.POS_UTG;

			if (player > 2)
				player += (10 - numInGame);

			if (player < 6)
				return Player.POS_EARLY;
			else if (player < 9)
				return Player.POS_MID;
			else
				return Player.POS_LATE;
		}
		else
		{
			if (player == 0)
				return Player.POS_LATE;
			else if (doUtg && player == 1)
				return Player.POS_UTG;

			if (player > 0)
				player += (10 - numInGame);

			if (player < 5)
				return Player.POS_EARLY;
			else if (player < 8)
				return Player.POS_MID;
			else
				return Player.POS_LATE;
		}
	}


	/**
	 * Increment the nextToAct index, skipping folds.
	 */
	private void incNextToAct()
	{
		nextToAct = (nextToAct + 1) % numInGame;
		while (folded[nextToAct] || allin[nextToAct])
			nextToAct = (nextToAct + 1) % numInGame;
	}


	/**
	 * From the nextToAct player, check-fold to the given player.
	 * 
	 * @param player
	 *            player who should act after the check-folds
	 * @throws RemoteException
	 */
	private void checkFoldTo(int player) throws RemoteException
	{
		checkFolding = true;
		while (nextToAct != player)
		{
			if (canCheck)
				playerMoved(handId, nextToAct, Move.check());
			else
				playerMoved(handId, nextToAct, Move.fold());
		}
		checkFolding = false;
	}


	/**
	 * Return the amount of the current bet or raise.
	 * 
	 * @return bet/raise amount
	 */
	private Money roundBet()
	{
		if (round < Player.POSTTURN)
			return earlyBet;
		else
			return lateBet;
	}


	/**
	 * @throws RemoteException
	 * @see poker.server.base.Player#beginHand(int, java.lang.String[],
	 *      poker.common.Money[])
	 */
	public int beginHand(String[] players, Money[] antes)
			throws RemoteException
	{
		if (playingHand)
			terminateHand();

		handId++;

		// get player index
		for (playerIdx = 0; playerIdx < players.length; playerIdx++)
			if (players[playerIdx].equals(playerName))
				break;

		if (playerIdx == players.length)
			playerIdx = -1;

		// initialize hand information
		numInGame = players.length;
		bets = new Money[antes.length];
		spent = new Money[antes.length];
		betsCalled = new int[numInGame];
		for (int i = 0; i < antes.length; i++)
		{
			bets[i] = new Money(antes[i]);
			spent[i] = new Money(antes[i]);
			betsCalled[i] = 1;

			System.out.printf("DBG: player %d : %s : %s\n", i, players[i],
					antes[i].toString()); // DBG
		}
		playerNames = players.clone();
		folded = new boolean[numInGame];
		allin = new boolean[numInGame];
		Arrays.fill(folded, false);
		playingHand = true;
		round = Player.PREFLOP1;
		holes = new Hand[numInGame];

		// initialize preflop round information
		gotRoundCards = true;
		board.clear();
		numInRound = numInGame;
		numActed = 0;
		numFolded = 0;
		numAllIn = 0;
		newNumAllIn = 0;
		numChecked = 0;
		canCheck = false;
		extraBets = false;
		nextToAct = 3;
		betsCapped = false;
		numBets = 1;

		// initialize bets
		getMinBet();
		curBet.setTo(bbBet);
		pot.zero();
		for (Money m : antes)
			pot.addIn(m);

		beginHand();

		return handId;
	}


	/**
	 * @see poker.server.base.Player#getNextToAct(int, int)
	 * @throws RemoteException
	 */
	public int getNextToAct(int hand) throws RemoteException
	{
		checkHandId(hand);
		return nextToAct;
	}


	/**
	 * @see poker.server.base.Player#getPosition(int, int, int)
	 * @throws RemoteException
	 */
	public int getPosition(int hand, int player) throws RemoteException
	{
		checkHandId(hand);
		return position(player);
	}


	/**
	 * @see poker.server.base.Player#getPositionName(int, int, int)
	 * @throws RemoteException
	 */
	public String getPositionName(int hand, int player) throws RemoteException
	{
		checkHandId(hand);
		return PokerNet.posNames[position(player)];
	}


	/**
	 * @see poker.server.base.Player#getRound(int, int)
	 * @throws RemoteException
	 */
	public int getRound(int hand) throws RemoteException
	{
		checkHandId(hand);
		return round;
	}


	/**
	 * @see poker.server.base.Player#canCheck(int, int)
	 * @throws RemoteException
	 */
	public boolean canCheck(int hand) throws RemoteException
	{
		checkHandId(hand);
		return canCheck || !bets[nextToAct].moreThan(curBet);
	}


	/**
	 * @see poker.server.base.Player#setPocket(int, int, int,
	 *      poker.ai.core.Hand)
	 * @throws RemoteException
	 */
	public void setPocket(int hand, int player, Hand pocket)
			throws RemoteException
	{
		checkHandId(hand);
		holes[player] = pocket;
	}


	/**
	 * @see poker.server.base.Player#userChatted(int, int, java.lang.String,
	 *      java.lang.String, boolean)
	 * @throws RemoteException
	 */
	public String userChatted(int hand, String user, String text,
			boolean whitespace) throws RemoteException
	{
		checkHandId(hand);
		return userChatted(user, text, whitespace);
	}


	/**
	 * @see poker.server.base.Player#verifyPot(int, int, poker.common.Money,
	 *      boolean)
	 * @throws RemoteException
	 */
	public boolean verifyPot(int hand, Money potAmount, boolean fix)
			throws RemoteException
	{
		checkHandId(hand);

		if (pot.equals(potAmount))
			return true;
		else if (fix)
			pot.setTo(potAmount);
		return false;
	}


	/**
	 * @see poker.server.base.Player#cardsDealt(int, int, int,
	 *      poker.ai.core.Hand)
	 * @throws RemoteException
	 */
	public void cardsDealt(int hand, int round, Hand cards)
			throws RemoteException
	{
		checkHandId(hand);

		if (round == this.round + 1)
			forceEndRound();
		else
			checkRoundId(round);

		gotRoundCards = true;

		board = board.add(cards);
		cardsDealt(cards);
	}


	private void forceEndRound()
	{
		nextToAct = 1;
		while (folded[nextToAct] || allin[nextToAct])
			nextToAct = (nextToAct + 1) % numInGame;

		if (round == Player.PREFLOP1)
			round += 2;
		else
			round++;
		canCheck = true;

		numInRound -= numFolded;
		numActed = 0;
		numFolded = 0;
		numChecked = 0;
		extraBets = false;
		betsCapped = false;
		curBet.zero();
		minBet.zero();
		numBets = 0;
		gotRoundCards = false;

		for (int i = 0; i < numInGame; i++)
		{
			bets[i].zero();
			betsCalled[i] = 0;
		}

		if (round == Player.POSTFLOP)
			lastPpf = (double) numInRound / (double) numInGame;

		if (round == Player.POSTFLOP)
			preflopEnded();
		else if (round == Player.POSTTURN)
			postflopEnded();
		else if (round == Player.POSTRIVER)
			postturnEnded();

	}


	/**
	 * @see poker.server.base.Player#playerMoved(int, int, int,
	 *      poker.server.base.Move)
	 * @throws RemoteException
	 */
	public void playerMoved(int hand, int player, Move move)
			throws RemoteException
	{
		checkHandId(hand);

		System.out
				.printf("in playerMoved, p = %d (%s), nta = %d, m = %s\n",
						player, getSafePlayerName(player), nextToAct, move
								.getPassive()); // DBG

		if (nextToAct == -1)
			throw new RemoteException("game over");

		if (!checkFolding && player != nextToAct)
			checkFoldTo(player);

		if (move.isCheck() && (!canCheck && curBet.moreThan(bets[player])))
		{
			// assume an ante was incorrect
			pot.addIn(curBet);
			bets[player].setTo(curBet);
			canCheck = true;
		}

		// first raise preflop is sometimes called a bet
		if ((round == Player.PREFLOP1) && curBet.equals(bbBet) && move.isBet())
			move = Move.raise();

		playerMoved(player, move);

		if (move.isFold())
		{
			folded[player] = true;
			numFolded++;
			numActed++;
			getMinBet();
		}
		else if (move.isCall())
		{
			pot.addIn(curBet.subtract(bets[player]));
			bets[player].setTo(curBet);
			numActed++;
			betsCalled[player] = numBets;
			getMinBet();
			canCheck = false;
		}
		else if (move.isRaise())
		{
			if (curBet.isZero())
				throw new RemoteException("cannot raise; no bet has been made");
			curBet.addIn(roundBet());
			pot.addIn(curBet.subtract(bets[player]));
			bets[player].setTo(curBet);
			numActed++;
			betsCalled[player] = ++numBets;
			getMinBet();
			canCheck = false;
		}
		else if (move.isBet())
		{
			if (!curBet.isZero())
				throw new RemoteException("cannot bet; raise instead");
			curBet.setTo(roundBet());
			bets[player].setTo(curBet);
			pot.addIn(curBet);
			numActed++;
			betsCalled[player] = ++numBets;
			getMinBet();
			canCheck = false;
		}
		else if (move.isCheck())
		{
			if (!canCheck)
				numActed++;
			numChecked++;
		}

		if ((numInRound - numFolded) == 1)
		{
			nextToAct = -1;
			System.out.println("game over because 1 left");
		}
		else
		{
			if (numActed + numChecked + numAllIn == numInRound)
				extraBets = true;

			if ((numActed + numAllIn < numInRound)
					&& (numChecked + numFolded + numAllIn < numInRound))
			{
				incNextToAct();
			}
			else if (curBet.moreThan(minBet))
			{
				if (round == Player.PREFLOP1)
					round++;
				incNextToAct();
			}
			else
			{
				nextToAct = 1;
				while (folded[nextToAct] || allin[nextToAct])
					nextToAct = (nextToAct + 1) % numInGame;

				if (round == Player.PREFLOP1)
					round += 2;
				else
					round++;
				canCheck = true;
				gotRoundCards = false;

				if (round > Player.POSTRIVER)
				{
					nextToAct = -1;
					System.out.println("game ended because river over");// DBG
				}
				else
				{
					numInRound -= numFolded;
					numActed = 0;
					numFolded = 0;
					numChecked = 0;
					numAllIn += newNumAllIn;
					newNumAllIn = 0;
					extraBets = false;
					betsCapped = false;
					curBet.zero();
					minBet.zero();
					numBets = 0;

					for (int i = 0; i < numInGame; i++)
					{
						bets[i].zero();
						betsCalled[i] = 0;
					}

					if (round == Player.POSTFLOP)
						lastPpf = (double) numInRound / (double) numInGame;

					if (round == Player.POSTFLOP)
						preflopEnded();
					else if (round == Player.POSTTURN)
						postflopEnded();
					else if (round == Player.POSTRIVER)
						postturnEnded();
				}
			}
		}

		System.out.printf("nextToAct now %d: %s\n", nextToAct,
				getSafePlayerName(nextToAct)); // DBG
	}


	private String getSafePlayerName(int player)
	{
		if (player == -1)
			return "<over>";
		else
			return playerNames[player];
	}


	/**
	 * @see poker.server.base.Player#requestMove(int, int, int, int)
	 * @throws RemoteException
	 */
	public Move requestMove(int hand, int roundId, int player)
			throws RemoteException
	{
		checkHandId(hand);
		checkRoundId(roundId);

		if (!checkFolding && player != nextToAct)
			checkFoldTo(player);

		Move m = requestMove(player);

		if (betsCapped && m.isRaise())
			return Move.call();

		return m;
	}


	/**
	 * @see poker.server.base.Player#endHand(int, int, boolean, int[], int[],
	 *      poker.ai.core.Hand[], poker.common.Money)
	 * @throws RemoteException
	 */
	public Move endHand(int hand, int[] winners, Money[] wins, int[] showers,
			Distribution[] hands) throws RemoteException
	{
		checkHandId(hand);

		// create show hands map
		Map<String, Distribution> handMap = new HashMap<String, Distribution>();
		for (int i = 0; i < showers.length; i++)
			handMap.put(playerNames[showers[i]], hands[i]);

		// get whether player won, and his winnings
		int wonIdx = -1;
		boolean won = false;
		for (int i = 0; i < winners.length; i++)
		{
			if (winners[i] == playerIdx)
			{
				won = true;
				wonIdx = i;
			}
		}
		// TODO: keep track of spent[]
		// get net win/loss

		Money net = (playerIdx == -1) ? new Money(0, 0) : spent[playerIdx]
				.neg();
		if (won)
			net.addIn(wins[wonIdx]);

		gotRoundCards = true;
		// recompute averages
		avgPpf += lastPpf;
		avgPot.addIn(pot);
		avgCounter++;

		return endHand(won, net, handMap);
	}


	/**
	 * @see poker.server.base.Player#getAveragePlayersPerFlop(int)
	 */
	public double getAveragePlayersPerFlop()
	{
		return avgPpf / (double) avgCounter;
	}


	/**
	 * @see poker.server.base.Player#getAveragePot(int)
	 */
	public Money getAveragePot()
	{
		return avgPot.divideBy(avgCounter);
	}


	/**
	 * @see poker.server.base.Player#capBets(int, int, int)
	 */
	public void capBets(int hand, int round)
	{
		betsCapped = true;
	}


	/**
	 * @return current number of bets, including blinds/antes
	 */
	public int numBets()
	{
		return this.numBets;
	}


	public Hand getBoardCards(int hand) throws RemoteException
	{
		checkHandId(hand);
		return this.board;
	}


	public void correctNames(int hand, String[] correct) throws RemoteException
	{
		checkHandId(hand);
		for (int i = 0; i < correct.length; i++)
		{
			if (!playerNames[i].equals(correct[i]))
			{
				correctName(i, playerNames[i], correct[i]);
				playerNames[i] = correct[i];
			}
		}
	}


	public abstract void setBias(Distribution dist);


	public void insertPlayer(int hand, int player, String name)
			throws RemoteException
	{
		checkHandId(hand);
		Money[] nb = new Money[numInGame + 1];
		Money[] ns = new Money[numInGame + 1];
		int[] bc = new int[numInGame + 1];
		String[] pn = new String[numInGame + 1];
		boolean[] nf = new boolean[numInGame + 1];
		Hand[] nh = new Hand[numInGame + 1];
		boolean[] na = new boolean[numInGame + 1];

		int j = 0;
		for (int i = 0; i < numInGame + 1; i++)
		{
			if (i == player)
			{
				nb[i] = new Money(0, 0);
				ns[i] = new Money(0, 0);
				bc[i] = 0;
				pn[i] = name;
				nf[i] = false;
				nh[i] = null;
				na[i] = false;
				j = 1;
			}
			else
			{
				nb[i] = bets[i - j];
				ns[i] = spent[i - j];
				bc[i] = betsCalled[i - j];
				pn[i] = playerNames[i - j];
				nf[i] = folded[i - j];
				nh[i] = holes[i - j];
				na[i] = allin[i - j];
			}
		}

		bets = nb;
		spent = ns;
		betsCalled = bc;
		playerNames = pn;
		folded = nf;
		holes = nh;
		allin = na;
		numInRound++;
		numInGame++;
		minBet.zero();

	}


	public void allIn(int hand, int player) throws RemoteException
	{
		checkHandId(hand);
		allin[player] = true;
		newNumAllIn++;
	}


	public abstract void setTableParam(String cxt, String name, Object val);


	public boolean roundCardsDealt(int hid) throws RemoteException
	{
		checkHandId(hid);
		return gotRoundCards;
	}


	public abstract void setAiType(int hid, int player, int type)
			throws RemoteException;
}
