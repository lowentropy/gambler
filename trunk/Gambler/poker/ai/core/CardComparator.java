package poker.ai.core;

import java.util.Comparator;


public class CardComparator implements Comparator<Card>
{

	public int compare(Card o1, Card o2)
	{
		int a = o1.getIndex();
		int b = o2.getIndex();
		return a < b ? -1 : (a > b ? 1 : 0);
	}

}
