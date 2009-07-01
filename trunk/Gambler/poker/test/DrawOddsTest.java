/* 
 * DrawOddsTest.java
 * 
 * created: 1-May-06
 * author: Nathan Matthews
 * email: lowentropy@gmail.com
 * 
 * Copyright (C) 2006
 */
package poker.test;
import java.io.BufferedReader;
import poker.ai.core.Hand;
import poker.server.base.impl.LoosePokerNetTable;

import java.io.InputStreamReader;
import poker.ai.bnet.PokerNet;
import bayes.Distribution;

/**
 * TODO: DrawOddsTest
 * 
 * @author lowentropy
 * 
 */
public class DrawOddsTest
{

	/**
	 * TODO: comment DrawOddsTest.main
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
		BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
		String line;
		double[][] odds = new double[9][1];
		Distribution[][][] kicks = new Distribution[9][][];
		kicks[0] = new Distribution[1][1];
		kicks[1] = new Distribution[2][1];
		kicks[2] = new Distribution[2][1];
		kicks[3] = new Distribution[1][1];
		kicks[4] = new Distribution[2][1];
		kicks[5] = new Distribution[1][1];
		kicks[6] = new Distribution[2][1];
		kicks[7] = new Distribution[1][1];
		kicks[8] = new Distribution[1][1];
		double[] d = new double[8];
		Distribution[][] k = new Distribution[9][];
		for (int i = 0; i < 9; i++)
		{
			k[i] = new Distribution[kicks[i].length];
			for (int j = 0; j < k[i].length; j++)
				kicks[i][j][0] = new Distribution("kick", PokerNet.ranks, new double[13]);
		}
		
		try {
		while (!(line = r.readLine()).equals("quit"))
		{
			int idx = line.indexOf(":");
			Hand b = new Hand(line.substring(0,idx).split(","));
			Hand p = new Hand(line.substring(idx+1).split(","));
			b.calculateDrawOdds(0, odds, kicks, p.getCards().get(0), p.getCards().get(1));
			for (int i = 0; i < 8; i++)
				d[i] = odds[i+1][0];
			for (int i = 0; i < 9; i++)
				for (int j = 0; j < k[i].length; j++)
					k[i][j] = kicks[i][j][0];
			for (int i = 0; i < 9; i++)
				b.printOuts(i, p.getCards().get(0).getIndex(), p.getCards().get(1).getIndex());
			LoosePokerNetTable.printDrawsAndKickers(0, d, k);
		}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

}
