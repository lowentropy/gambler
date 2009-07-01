
package poker.server.base.impl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.TreeMap;

import bayes.Distribution;

import poker.ai.core.Hand;
import poker.common.Money;
import poker.server.base.Move;
import poker.server.base.Player;


/**
 * StatePlayer defines a container for objects extending the abstract StateTable
 * class.
 * 
 * @author lowentropy
 */
public abstract class StatePlayer extends UnicastRemoteObject implements Player
{

	/** next assignable table id */
	private int							nextTableId;

	/** map from table id to table object */
	private Map<Integer, StateTable>	tables;


	/**
	 * Constructor. Should be called first by all descendent classes.
	 */
	public StatePlayer() throws RemoteException
	{
		nextTableId = 1;
		tables = new TreeMap<Integer, StateTable>();
	}


	/**
	 * Get the table object mapped to the given table id. If there is no such
	 * table, throw a RemoteException.
	 * 
	 * @param tableId
	 *            table id of object
	 * @return table object
	 * @throws RemoteException
	 *             if there is no table mapped to tableId
	 */
	public StateTable getTable(int tableId) throws RemoteException
	{
		if (!tables.containsKey(tableId))
			throw new RemoteException("request for nonexistent table id: "
					+ tableId);
		return tables.get(tableId);
	}


	/**
	 * Create a new table object. Descendent player classes must create objects
	 * which override the StateTable class.
	 * 
	 * @param tableName
	 *            name of table
	 * @param playerName
	 *            name of player at table
	 * @param smallBlind
	 *            amount of small blind bet
	 * @param bigBlind
	 *            amount of big blind bet
	 * @param earlyBet
	 *            bet/raise amount in preflop and postflop rounds
	 * @param lateBet
	 *            bet/raise amount in postturn and postriver rounds
	 * @return table object
	 */
	protected abstract StateTable newTable(String tableName, String playerName,
			Money smallBlind, Money bigBlind, Money earlyBet, Money lateBet,
			Money rake);


	/**
	 * @see poker.server.base.Player#beginHand(int, java.lang.String[],
	 *      poker.common.Money[])
	 */
	public int beginHand(int table, String[] players, Money[] antes)
			throws RemoteException
	{
		return getTable(table).beginHand(players, antes);
	}


	/**
	 * @see poker.server.base.Player#cardsDealt(int, int, int,
	 *      poker.ai.core.Hand)
	 */
	public void cardsDealt(int table, int hand, int round, Hand cards)
			throws RemoteException
	{
		getTable(table).cardsDealt(hand, round, cards);
	}


	/**
	 * @see poker.server.base.Player#getBoardCards(int, int)
	 */
	public Hand getBoardCards(int table, int hand) throws RemoteException
	{
		return getTable(table).getBoardCards(hand);
	}


	/**
	 * @see poker.server.base.Player#endHand(int, int, boolean, int[], int[],
	 *      poker.ai.core.Hand[], poker.common.Money)
	 */
	public Move endHand(int table, int hand, int[] winners, Money[] wins,
			int[] showers, Distribution[] hands) throws RemoteException
	{
		return getTable(table).endHand(hand, winners, wins, showers, hands);
	}


	/**
	 * @see poker.server.base.Player#getAveragePlayersPerFlop(int)
	 */
	public double getAveragePlayersPerFlop(int table) throws RemoteException
	{
		return getTable(table).getAveragePlayersPerFlop();
	}


	/**
	 * @see poker.server.base.Player#getAveragePot(int)
	 */
	public Money getAveragePot(int table) throws RemoteException
	{
		return getTable(table).getAveragePot();
	}


	/**
	 * @see poker.server.base.Player#getNextToAct(int, int)
	 */
	public int getNextToAct(int table, int hand) throws RemoteException
	{
		return getTable(table).getNextToAct(hand);
	}


	/**
	 * @see poker.server.base.Player#getPosition(int, int, int)
	 */
	public int getPosition(int table, int hand, int player)
			throws RemoteException
	{
		return getTable(table).getPosition(hand, player);
	}


	/**
	 * @see poker.server.base.Player#getPositionName(int, int, int)
	 */
	public String getPositionName(int table, int hand, int player)
			throws RemoteException
	{
		return getTable(table).getPositionName(hand, player);
	}


	/**
	 * @see poker.server.base.Player#getRound(int, int)
	 */
	public int getRound(int table, int hand) throws RemoteException
	{
		return getTable(table).getRound(hand);
	}


	/**
	 * @see poker.server.base.Player#canCheck(int, int)
	 */
	public boolean canCheck(int table, int hand) throws RemoteException
	{
		return getTable(table).canCheck(hand);
	}


	/**
	 * @see poker.server.base.Player#joinTable(java.lang.String,
	 *      java.lang.String, poker.common.Money, poker.common.Money,
	 *      poker.common.Money, poker.common.Money)
	 */
	public int joinTable(String tableName, String playerName, Money smallBlind,
			Money bigBlind, Money earlyBet, Money lateBet, Money rake)
			throws RemoteException
	{
		StateTable table = newTable(tableName, playerName, smallBlind,
				bigBlind, earlyBet, lateBet, rake);
		tables.put(nextTableId, table);
		return nextTableId++;
	}


	/**
	 * @see poker.server.base.Player#leaveTable(int)
	 */
	public void leaveTable(int table) throws RemoteException
	{
		getTable(table).leaveTable();
		tables.remove(table);
	}


	/**
	 * @see poker.server.base.Player#playerMoved(int, int, int,
	 *      poker.server.base.Move)
	 */
	public void playerMoved(int table, int hand, int player, Move move)
			throws RemoteException
	{
		getTable(table).playerMoved(hand, player, move);
	}


	/**
	 * @see poker.server.base.Player#requestMove(int, int, int, int)
	 */
	public Move requestMove(int table, int hand, int round, int player)
			throws RemoteException
	{
		return getTable(table).requestMove(hand, round, player);
	}


	/**
	 * @see poker.server.base.Player#setLooseGame(int, double)
	 */
	public void setLooseGame(int table, double strength) throws RemoteException
	{
		getTable(table).setLooseGame(strength);
	}


	/**
	 * @see poker.server.base.Player#setPocket(int, int, int,
	 *      poker.ai.core.Hand)
	 */
	public void setPocket(int table, int hand, int player, Hand pocket)
			throws RemoteException
	{
		getTable(table).setPocket(hand, player, pocket);
	}


	/**
	 * @see poker.server.base.Player#setTightGame(int, double)
	 */
	public void setTightGame(int table, double strength) throws RemoteException
	{
		getTable(table).setTightGame(strength);
	}


	/**
	 * @see poker.server.base.Player#setNormalGame(int)
	 */
	public void setNormalGame(int table) throws RemoteException
	{
		getTable(table).setNormalGame();
	}


	/**
	 * @see poker.server.base.Player#userChatted(int, int, java.lang.String,
	 *      java.lang.String, boolean)
	 */
	public String userChatted(int table, int hand, String user, String text,
			boolean whitespace) throws RemoteException
	{
		return getTable(table).userChatted(hand, user, text, whitespace);
	}


	/**
	 * @see poker.server.base.Player#verifyPot(int, int, poker.common.Money,
	 *      boolean)
	 */
	public boolean verifyPot(int table, int hand, Money potAmount, boolean fix)
			throws RemoteException
	{
		return getTable(table).verifyPot(hand, potAmount, fix);
	}


	/**
	 * @throws RemoteException
	 * @see poker.server.base.Player#capBets(int, int, int)
	 */
	public void capBets(int table, int hand, int round) throws RemoteException
	{
		getTable(table).capBets(hand, round);
	}


	/**
	 * @see poker.server.base.Player#correctNames(int, int, java.lang.String[])
	 */
	public void correctNames(int table, int hand, String[] correct)
			throws RemoteException
	{
		getTable(table).correctNames(hand, correct);
	}


	public void setBias(int table, Distribution dist) throws RemoteException
	{
		getTable(table).setBias(dist);
	}


	public void insertPlayer(int table, int hand, int player, String name) throws RemoteException
	{
		getTable(table).insertPlayer(hand, player, name);
	}

	public void allIn(int table, int hand, int player) throws RemoteException
	{
		getTable(table).allIn(hand, player);
	}
	
	public void setTableParam(int tid, String cxt, String name, Object val) throws RemoteException
	{
		getTable(tid).setTableParam(cxt, name, val);
	}
	
	public boolean roundCardsDealt(int tid, int hid) throws RemoteException
	{
		return getTable(tid).roundCardsDealt(hid);
	}
	
	public void setAiType(int tid, int hid, int player, int type) throws RemoteException
	{
		getTable(tid).setAiType(hid, player, type);
	}
}
