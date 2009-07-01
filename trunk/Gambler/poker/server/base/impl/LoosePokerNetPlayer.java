
package poker.server.base.impl;

import java.rmi.RemoteException;

import bayes.BayesError;
import bayes.Distribution;
import poker.ai.core.Hand;
import poker.common.Money;


public class LoosePokerNetPlayer extends StatePlayer
{

	private static final long	serialVersionUID	= 3257845467898788146L;


	public LoosePokerNetPlayer() throws RemoteException
	{
		super();
	}


	/**
	 * Create new table.
	 */
	protected LoosePokerNetTable newTable(String tableName, String playerName,
			Money smallBlind, Money bigBlind, Money earlyBet, Money lateBet,
			Money rake)
	{
		try
		{
			// return new LoosePokerNetTable(tableName, playerName, smallBlind,
			// bigBlind, earlyBet, lateBet, rake);
			return new CompoundTable(tableName, playerName, smallBlind,
					bigBlind, earlyBet, lateBet, rake);
		}
		catch (BayesError e)
		{
			return null;
		}
	}

}
