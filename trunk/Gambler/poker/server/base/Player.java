/*
 * Player.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.server.base;

import java.rmi.Remote;
import java.rmi.RemoteException;

import bayes.Distribution;

import poker.ai.core.Hand;
import poker.common.Money;

public interface Player extends Remote
{

	public static final int AI_BAYES = 0;
	
	public static final int AI_SIMPLE = 1;
	
	public static final int	AI_MAX	= 1;
	
	/** preflop round before all players have acted */
	public static final int PREFLOP1 = 1;

	/** preflop round if last action was a raise */
	public static final int PREFLOP2 = 2;

	/** postflop round */
	public static final int POSTFLOP = 3;

	/** postturn round */
	public static final int POSTTURN = 4;

	/** postriver round */
	public static final int POSTRIVER = 5;

	/** folded player (not used by nets) */
	public static final int POS_FOLDED = 0;

	/** to the right of dealer */
	public static final int POS_SMALLBLIND = 1;

	/** to the right of small blind */
	public static final int POS_BIGBLIND = 2;

	/** to the right of big blind preflop, or small blind postflop */
	public static final int POS_UTG = 3;

	/** early position (first 3-4) */
	public static final int POS_EARLY = 4;

	/** middle position (next 3-4) */
	public static final int POS_MID = 5;

	/** late position (last 3-4) */
	public static final int POS_LATE = 6;

	/**
	 * Join a table as the given player name.
	 * 
	 * @param tableName
	 *            name of table (for display/log purposes)
	 * @param playerName
	 *            name of player
	 * @param smallBlind
	 *            amount of small blind bet
	 * @param bigBlind
	 *            amount of big blind bet
	 * @param earlyBet
	 *            amount of bets/raises in first two rounds
	 * @param lateBet
	 *            amount of bets/raises in last two rounds
	 * @return table id
	 * @throws RemoteException
	 */
	public int joinTable(String tableName, String playerName, Money smallBlind,
			Money bigBlind, Money earlyBet, Money lateBet, Money rake)
			throws RemoteException;

	/**
	 * Leave a table the player is at.
	 * 
	 * @param table
	 *            table id
	 * @throws RemoteException
	 */
	public void leaveTable(int table) throws RemoteException;

	/**
	 * Sit in on a hand.
	 * 
	 * @param table
	 *            table id
	 * @param pos
	 *            relative position in order of action, starting with 0;
	 *            <code>players[pos]</code> should equal
	 *            <code>playerName</code> from <code>joinTable()</code>.
	 * @param players
	 *            names of all players in hand
	 * @param antes
	 *            antes of all players
	 * @param pocket
	 *            pocket cards player was dealt
	 * @return hand id
	 * @throws RemoteException
	 */
	public int beginHand(int table, String[] players, Money[] antes)
			throws RemoteException;

	/**
	 * Set the known pocket cards for the given player.
	 * 
	 * @param table
	 *            table id
	 * @param player
	 *            player index
	 * @param pocket
	 *            pocket cards
	 * @throws RemoteException
	 */
	public void setPocket(int table, int hand, int player, Hand pocket)
			throws RemoteException;

	/**
	 * Get the index of the next player to act in turn.
	 * 
	 * @return next player index
	 * @throws RemoteException
	 */
	public int getNextToAct(int table, int hand) throws RemoteException;

	/**
	 * Get the current board cards.
	 * 
	 * @param table table id
	 * @param hand hand id
	 * @return hand containing board cards
	 * @throws RemoteException
	 */
	public Hand getBoardCards(int table, int hand) throws RemoteException;
	
	/**
	 * The hand has ended; somebody won, or the pot was split.
	 * 
	 * @param hand
	 *            hand id of hand that ended
	 * @param winners
	 *            the index into <code>players</code> of winners
	 * @param wins
	 *            amount each winner got from pot
	 * @param showers
	 *            the index into <code>players</code> of hand showers
	 * @param showh
	 *            hands which were shown down, in order of <code>showers</code>
	 * @return table action to take
	 * @throws RemoteException
	 */
	public Move endHand(int table, int hand, int[] winners, Money[] wins,
			int[] showers, Distribution[] showh) throws RemoteException;

	/**
	 * A player (other than the bot) moved.
	 * 
	 * @param hand
	 *            hand id
	 * @param player
	 *            player index
	 * @param move
	 *            move taken
	 * @return table action to take
	 * @throws RemoteException
	 */
	public void playerMoved(int table, int hand, int player, Move move)
			throws RemoteException;

	/**
	 * Some user entered chat text.
	 * 
	 * @param hand
	 *            hand where chat happened (or -1 for non-hand chat)
	 * @param player
	 *            name of chatting player
	 * @param text
	 *            chatted text
	 * @param whitespace
	 *            whether chat text includes whitespaces
	 * @return response, or null for none
	 * @throws RemoteException
	 */
	public String userChatted(int table, int hand, String user, String text,
			boolean whitespace) throws RemoteException;

	/**
	 * Request a move decision be made by the player.
	 * 
	 * @param hand
	 *            hand id
	 * @param round
	 *            round id
	 * @return move to take
	 * @throws RemoteException
	 */
	public Move requestMove(int table, int hand, int round, int player)
			throws RemoteException;

	/**
	 * A card or cards were dealt for the given round.
	 * 
	 * @param hand
	 *            hand id
	 * @param round
	 *            round id
	 * @param cards
	 *            card or cards dealt
	 * @return table action to take
	 * @throws RemoteException
	 */
	public void cardsDealt(int table, int hand, int round, Hand cards)
			throws RemoteException;

	/**
	 * Verify that the pot amount is actually $X. If fix is true, the player
	 * should correct the stored pot amount.
	 * 
	 * @param potAmount
	 *            amount which pot should be
	 * @param fix
	 *            whether to fix pot amount, if inequal
	 * @return whether pot amounts matched before fixing
	 * @throws RemoteException
	 */
	public boolean verifyPot(int table, int hand, Money potAmount, boolean fix)
			throws RemoteException;

	/**
	 * Cap the betting in this round.
	 * 
	 * @param table
	 *            table hand is in
	 * @param hand
	 *            hand to cap betting in
	 * @param round
	 *            round of betting to cap
	 * @throws RemoteException 
	 */
	public void capBets(int table, int hand, int round) throws RemoteException;

	/**
	 * @param table
	 *            table id
	 * @param hand
	 *            hand id
	 * @return current round
	 * @throws RemoteException
	 */
	public int getRound(int table, int hand) throws RemoteException;

	/**
	 * Return whether the next player to act is allowed to check.
	 * 
	 * @param table
	 *            table id
	 * @param hand
	 *            hand id
	 * @return whether next player can check
	 * @throws RemoteException
	 */
	public boolean canCheck(int table, int hand) throws RemoteException;

	/**
	 * @param table
	 *            table id
	 * @return average players-per-flop ratio
	 * @throws RemoteException
	 */
	public double getAveragePlayersPerFlop(int table) throws RemoteException;

	/**
	 * @param table
	 *            table id
	 * @return average pot amount
	 * @throws RemoteException
	 */
	public Money getAveragePot(int table) throws RemoteException;

	/**
	 * Return the id of the position which the given player is in.
	 * 
	 * @param table
	 *            table id
	 * @param hand
	 *            hand id
	 * @param player
	 *            player index
	 * @return position id
	 * @throws RemoteException
	 */
	public int getPosition(int table, int hand, int player)
			throws RemoteException;

	/**
	 * Return the name of the position which the given player is in.
	 * 
	 * @param table
	 *            table id
	 * @param hand
	 *            hand id
	 * @param player
	 *            player index
	 * @return name of position
	 * @throws RemoteException
	 */
	public String getPositionName(int table, int hand, int player)
			throws RemoteException;

	/**
	 * The player names might have been wrong to begin with. Correct their names now.
	 * 
	 * @param table table id
	 * @param hand hand id
	 * @param correct correct names, in order of players
	 * @throws RemoteException 
	 */
	public void correctNames(int table, int hand, String[] correct) throws RemoteException;

	public void setBias(int table, Distribution dist) throws RemoteException;

	public void insertPlayer(int tid, int hid, int nextPos, String pname) throws RemoteException;

	public void allIn(int tid, int hid, int player) throws RemoteException;

	public void setTableParam(int tid, String cxt, String name, Object val) throws RemoteException;

	public boolean roundCardsDealt(int tid, int hid) throws RemoteException;
	
	public void setAiType(int tid, int hid, int player, int type) throws RemoteException;
}
