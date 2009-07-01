/* 
 * HandEvalTest.java
 * 
 * created: 16-May-06
 * author: Nathan Matthews
 * email: lowentropy@gmail.com
 * 
 * Copyright (C) 2006
 */

package poker.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.biotools.meerkat.Card;
import com.biotools.meerkat.Hand;
import com.biotools.meerkat.HandEvaluator;


/**
 * TODO: HandEvalTest
 * 
 * @author lowentropy
 */
public class HandEvalTest
{

	/**
	 * TODO: comment HandEvalTest.main
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
		String line;
		BufferedReader r = new BufferedReader(new InputStreamReader(System.in));

		try
		{
			while ((line = r.readLine()) != null)
			{
				Hand h = new Hand(line);
				System.out.print(h);
				Card c2 = h.getLastCard();
				h.removeCard();
				Card c1 = h.getLastCard();
				h.removeCard();
				System.out.printf(" = %s + %s + %s: ", h, c1, c2);
				System.out.printf("%f\n", HandEvaluator.rankHand(c1, c2, h));
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

}
