/* 
 * CompoundTable.java
 * 
 * created: 16-May-06
 * author: Nathan Matthews
 * email: lowentropy@gmail.com
 * 
 * Copyright (C) 2006
 */

package poker.server.base.impl;

import java.rmi.RemoteException;

import poker.ai.SimpleAI;
import poker.common.Money;
import poker.server.base.Move;
import poker.server.base.Player;
import bayes.BayesError;


/**
 * TODO: CompoundTable
 * 
 * @author lowentropy
 */
public class CompoundTable extends LoosePokerNetTable
{

	/**
	 * Constructor. TODO: comment
	 * 
	 * @param tableName
	 * @param playerName
	 * @param sbBet
	 * @param bbBet
	 * @param earlyBet
	 * @param lateBet
	 * @param rake
	 * @throws BayesError
	 */
	public CompoundTable(String tableName, String playerName, Money sbBet,
			Money bbBet, Money earlyBet, Money lateBet, Money rake)
			throws BayesError
	{
		super(tableName, playerName, sbBet, bbBet, earlyBet, lateBet, rake);
	}


	/**
	 * @see poker.server.base.impl.LoosePokerNetTable#requestMove(int)
	 */
	protected Move requestMove(int player) throws RemoteException
	{
		if (aiTypes[player] == Player.AI_BAYES)
			return super.requestMove(player);
		
		if (holes[player] == null)
			throw new RemoteException("invalid player; hole not known");

		int m = SimpleAI.getAction(round, holes[player],
				board, pot.toDouble(), curBet.subtract(
						bets[player]).toDouble(), bbBet.toDouble(), numBets,
				numInRound - numFolded, numActed - numFolded);

		if (m == SimpleAI.BET)
			return Move.bet();
		else if (m == SimpleAI.FOLD)
			return Move.fold();
		else if (m == SimpleAI.CALL)
			return Move.call();
		else if (m == SimpleAI.RAISE)
			return Move.raise();
		else if (m == SimpleAI.CHECKFOLD)
			if (canCheck(this.handId))
				return Move.check();
			else
				return Move.fold();
		else
			return null;
	}

}
