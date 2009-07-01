/*
 * Move.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.server.base;

import java.io.Serializable;

import poker.ai.bnet.PokerNet;
import poker.common.Money;
import poker.server.session.model.data.chat.ChatValue;


public class Move implements Serializable
{

	/** serial uid */
	private static final long		serialVersionUID	= -7800272512738748453L;

	public static final int			FOLD				= 1;
	public static final int			CALL				= 2;
	public static final int			RAISE				= 3;
	public static final int			BET					= 4;
	public static final int			CHECK				= 5;
	public static final int			LEAVE				= 6;
	public static final int			STAY				= 7;
	public static final int			ERROR				= 8;

	public static final String[]	actions				= new String[] {
			"folds", "calls", "raises", "bets", "checks", "leaves the table",
			"stays at the table", "- error occured"		};

	private int						id					= 0;

	private double[]				oppOdds				= null;

	private int[][]					oppKicks			= null;

	private double[]				userOdds			= null;

	private int[][]					userKicks			= null;

	private double[]				wins				= null;

	private double[]				profit				= null;

	private double[]				handDist			= null;

	private double[]				moveDist			= null;

	private double					cost;

	private double					reward;

	private int	remCards;


	public Move()
	{

	}


	public Move(int moveId)
	{
		id = moveId;
	}


	public void printOdds()
	{
		if (oppOdds == null)
			return;

		System.out.printf("\n      high   pair   2pair  set    str8   flush  full   4kind  str8-f ");
		System.out.printf("\nopp:  ");
		for (int i = 0; i < 9; i++)
			System.out.printf("%6.2f%%", oppOdds[i] * 100.0);
		System.out.printf("\n      ");
		for (int i = 0; i < 9; i++)
			for (int j = 0; j < 3; j++)
				System.out.printf("%s%s", PokerNet.ranks[oppKicks[i][j]],
						j < 2 ? "," : "  ");
		System.out.printf("\nuser: ");
		for (int i = 0; i < 9; i++)
			System.out.printf("%6.2f%%", userOdds[i] * 100.0);
		System.out.printf("\n      ");
		for (int i = 0; i < 9; i++)
			for (int j = 0; j < 3; j++)
				System.out.printf("%s%s", PokerNet.ranks[userKicks[i][j]],
						j < 2 ? "," : "  ");
		System.out.printf("\nhand: ");
		for (int i = 0; i < 9; i++)
			System.out.printf("%6.2f%%", handDist[i] * 100.0);
		System.out.printf("\nodds: lose=%6.2f%%, even=%6.2f%%, win=%6.2f%%",
				wins[0] * 100.0, wins[1] * 100.0, wins[2] * 100.0);
		System.out.printf(
				"\nprofit: neg=%6.2f%%, even=%6.2f%%, double+=%6.2f%%\n",
				profit[0] * 100.0, profit[1] * 100.0, profit[2] * 100.0);
		System.out.printf(
				"move: fold=%6.2f%%, check=%6.2f%%, bet=%6.2f%%, call=%6.2f%%, raise=%6.2f%%\n",
				moveDist[0] * 100.0, moveDist[1] * 100.0, moveDist[2] * 100.0,
				moveDist[3] * 100.0, moveDist[4] * 100.0);
		System.out.printf("PO = %6.2f%% (cost = %.2f, reward = %.2f, outs = %.2f)\n\n",
				(cost / reward) * 100.0, cost, reward, (cost * remCards / reward));
	}


	public static Move parse(String pokerMove)
	{
		// FIXME: remove PokerAI and hand masking
		return null;
	}


	public boolean isLeaveTable()
	{
		return (id == LEAVE);
	}


	public static Move leaveTable()
	{
		return new Move(LEAVE);
	}


	public static Move stayAtTable()
	{
		return new Move(STAY);
	}


	public Money getAmount()
	{
		// TODO Auto-generated method stub
		return null;
	}


	public boolean isRaise()
	{
		return (id == RAISE);
	}


	public boolean isFold()
	{
		return (id == FOLD);
	}


	public boolean isBet()
	{
		return (id == BET);
	}


	public boolean isCall()
	{
		return (id == CALL);
	}


	public static Move check()
	{
		return new Move(CHECK);
	}


	public static Move bet()
	{
		return new Move(BET);
	}


	public static Move error()
	{
		return new Move(ERROR);
	}


	public static Move fold()
	{
		return new Move(FOLD);
	}


	public boolean isCheck()
	{
		return (id == CHECK);
	}


	public int getFCR_Id()
	{
		if (isBet())
			return RAISE;
		else if (isCheck())
			return CALL;
		else
			return id;
	}


	public void printAction(String player)
	{
		System.out.printf("%s %s\n", player, actions[id - 1]);
	}


	public String getPassive()
	{
		return actions[id - 1];
	}


	public static Move call()
	{
		return new Move(CALL);
	}


	public String state()
	{
		switch (id) {
		case FOLD:
			return "F";
		case CHECK:
			return "CH";
		case CALL:
			return "C";
		case BET:
			return "B";
		case RAISE:
			return "R";
		default:
			return null;
		}
	}


	public static Move raise()
	{
		return new Move(RAISE);
	}


	public static Move fromChat(ChatValue value)
	{
		String s = value.function;
		if (s.equals("FOLD"))
			return fold();
		else if (s.equals("BET"))
			return bet();
		else if (s.equals("CHECK"))
			return check();
		else if (s.equals("RAISE"))
			return raise();
		else if (s.equals("CALL"))
			return call();
		return null;
	}


	public void setOdds(double[] lastOppOdds, int[][] lastOppKicks,
			double[] lastUserOdds, int[][] lastUserKicks, double[] ws,
			double[] ds, double[] ds2, double[] lastMoveDist, double cost,
			double pot, int remCards)
	{
		oppOdds = lastOppOdds;
		oppKicks = lastOppKicks;
		userOdds = lastUserOdds;
		userKicks = lastUserKicks;
		wins = ws;
		profit = ds;
		handDist = ds2;
		moveDist = lastMoveDist;
		this.cost = cost;
		this.reward = pot + cost;
		this.remCards = remCards;
	}
}
