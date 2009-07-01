
package poker.common;

import java.util.Arrays;
import java.util.List;

import poker.ai.bnet.PokerNet;
import poker.ai.core.Card;
import poker.ai.core.Hand;
import poker.ai.core.PokerSuit;
import poker.ai.core.PokerValue;
import poker.server.base.impl.LoosePokerNetTable;
import poker.server.session.model.data.chat.ChatValue;
import bayes.Distribution;

public class HandDist
{

	private static final String	NOPAIRP		= "NOPAIRP";

	private static final String	NO2PP		= "NO2PP";

	private static final String	NOSETP		= "NOSETP";

	private static final String	NOSTR8P		= "NOSTR8P";

	private static final String	NOFLUSHP	= "NOFLUSHP";

	private static final String	NOFULLP		= "NOFULLP";

	private static final String	NO4KP		= "NO4KP";

	private static final String	NOSFP		= "NOSTR8FP";

	private static final String	NOGT		= "NOGT";

	private static final String	NOPAIRG		= "NOPAIRG";

	private static final String	NO2PG		= "NO2PG";

	private static final String	NOSETG		= "NOSETG";

	private static final String	NOSTR8G		= "NOSTR8G";

	private static final String	NOFLUSHG	= "NOFLUSHG";

	private static final String	NOFULLG		= "NOFULLG";

	private static final String	NO4KG		= "NO4KG";

	private static final String	NOSFG		= "NOSTR8FG";

	private static final String	ONE			= "ONE";

	private static final String	TWO			= "TWO";

	private static final String	SUIT		= "SUIT";

	private Distribution		dist;

	private Card[][]			cards;

	private double[]			data;


	public HandDist()
	{
		dist = new Distribution("hole", PokerNet.fullHoles,
				new double[PokerNet.fullHoles.length]);
		data = dist.values;
		Arrays.fill(dist.values, 1.0);
		cards = LoosePokerNetTable.allHoles;
	}


	/**
	 * Get the hand held by a player. Returns null if this can't be determined.
	 * 
	 * @param b
	 *            board cards
	 * @param v
	 *            description of hand
	 * @return hole hand, or null
	 */
	public static Distribution getShowDist(Hand b, Hand h, ChatValue v)
	{
		String type = v.function;

		if (type.equals("high"))
		{
			PokerValue pv = PokerValue.fromString(v.args[0].function);
			if (b.numOfValue(pv) > 0)
				return makeHandDist(b, h, NOPAIRP, NOGT, pv);
			else
				return makeHandDist(b, h, NOPAIRP, NOGT, pv, ONE, pv);
		}
		else if (type.equals("pair"))
		{
			PokerValue pv = PokerValue.fromString(v.args[0].function);
			int n = b.numOfValue(pv);
			if (n == 0)
				return makeHandDist(b, h, NO2PP, NOPAIRG, pv, TWO, pv);
			else if (n == 1)
				return makeHandDist(b, h, NO2PP, NOPAIRG, pv, ONE, pv);
			else
				return makeHandDist(b, h, NO2PP, NOPAIRG, pv);
		}
		else if (type.equals("twopair"))
		{
			PokerValue pv1 = PokerValue.fromString(v.args[0].function);
			PokerValue pv2 = PokerValue.fromString(v.args[1].function);
			int n1 = b.numOfValue(pv1);
			int n2 = b.numOfValue(pv2);
			if ((n1 == 2) && (n2 == 2))
				return makeHandDist(b, h, NOSETP, NO2PG, pv1, pv2);
			else if ((n1 == 1) && (n2 == 1))
				return makeHandDist(b, h, NOSETP, NO2PG, pv1, pv2, ONE, pv1,
						ONE, pv2);
			else if ((n1 == 2) && (n2 == 0))
				return makeHandDist(b, h, NOSETP, NO2PG, pv1, pv2, TWO, pv2);
			else if ((n1 == 0) && (n2 == 2))
				return makeHandDist(b, h, NOSETP, NO2PG, pv1, pv2, TWO, pv1);
			else if ((n1 == 2) && (n2 == 1))
				return makeHandDist(b, h, NOSETP, NO2PG, pv1, pv2, ONE, pv2);
			else if ((n1 == 1) && (n2 == 2))
				return makeHandDist(b, h, NOSETP, NO2PG, pv1, pv2, ONE, pv1);
			else
				return null;
		}
		else if (type.equals("set"))
		{
			PokerValue pv = PokerValue.fromString(v.args[0].function);
			int n = b.numOfValue(pv);
			if (n == 1)
				return makeHandDist(b, h, NOSTR8P, NOSETG, pv, TWO, pv);
			else if (n == 2)
				return makeHandDist(b, h, NOSTR8P, NOSETG, pv, ONE, pv);
			else
				return null;
		}
		else if (type.equals("straight"))
		{
			PokerValue pv = PokerValue.fromString(v.args[0].function);
			PokerValue[] m = b.missingToStraight(pv, false);
			if (m.length == 0)
				return makeHandDist(b, h, NOFLUSHP, NOSTR8G, pv);
			else if (m.length == 1)
				return makeHandDist(b, h, NOFLUSHP, NOSTR8G, pv, ONE, m[0]);
			else if (m.length == 2)
				return makeHandDist(b, h, NOFLUSHP, NOSTR8G, pv, ONE, m[0],
						ONE, m[1]);
			else
				return null;
		}
		else if (type.equals("flush"))
		{
			PokerValue pv = PokerValue.fromString(v.args[0].function);
			PokerSuit ps = b.dominantSuit();
			int n = b.numOfSuit(ps);
			if (n == 3)
				return makeHandDist(b, h, NOFULLP, NOFLUSHG, pv, TWO, SUIT, ps);
			else if (n == 4)
				return makeHandDist(b, h, NOFULLP, NOFLUSHG, pv, ONE, SUIT, ps);
			else if (n == 5)
				return makeHandDist(b, h, NOFULLP, NOFLUSHG, pv);
			else
				return null;
		}
		else if (type.equals("fullhouse"))
		{
			PokerValue pv1 = PokerValue.fromString(v.args[0].function);
			PokerValue pv2 = PokerValue.fromString(v.args[1].function);
			int n1 = b.numOfValue(pv1);
			int n2 = b.numOfValue(pv2);
			if ((n1 == 3) && (n2 == 2))
				return makeHandDist(b, h, NO4KP, NOFULLG, pv1, pv2);
			else if ((n1 == 1) && (n2 == 2))
				return makeHandDist(b, h, NO4KP, NOFULLG, pv1, pv2, TWO, pv1);
			else if ((n1 == 2) && (n2 == 1))
				return makeHandDist(b, h, NO4KP, NOFULLG, pv1, pv2, ONE, pv1,
						ONE, pv2);
			else if ((n1 == 3) && (n2 == 0))
				return makeHandDist(b, h, NO4KP, NOFULLG, pv1, pv2, TWO, pv2);
			else if ((n1 == 3) && (n2 == 1))
				return makeHandDist(b, h, NO4KP, NOFULLG, pv1, pv2, ONE, pv2);
			else if ((n1 == 2) && (n2 == 2))
				return makeHandDist(b, h, NO4KP, NOFULLG, pv1, pv2, ONE, pv1);
			else
				return null;
		}
		else if (type.equals("four"))
		{
			PokerValue pv = PokerValue.fromString(v.args[0].function);
			int n = b.numOfValue(pv);
			if (n == 4)
				return makeHandDist(b, h, NOSFP, NO4KG, pv);
			else if (n == 3)
				return makeHandDist(b, h, NOSFP, NO4KG, pv, ONE, pv);
			else if (n == 2)
				return makeHandDist(b, h, NOSFP, NO4KG, pv, TWO, pv);
			else
				return null;
		}
		else if (type.equals("sflush"))
		{
			PokerValue pv = PokerValue.fromString(v.args[0].function);
			PokerSuit ps = b.dominantSuit();
			PokerValue[] m = b.missingToStraight(pv, true);

			if (m.length == 0)
				return makeHandDist(b, h, NOSFG, pv);
			else if (m.length == 1)
				return makeHandDist(b, h, NOSFG, pv, ONE, m[0], SUIT, ps);
			else if (m.length == 2)
				return makeHandDist(b, h, NOSFG, pv, ONE, m[0], SUIT, ps, ONE,
						m[1], SUIT, ps);
			else
				return null;
		}
		else
			return null;
	}


	private static Distribution makeHandDist(Hand b, Hand h, Object... args)
	{
		return new HandDist().calc(b, h, args);
	}


	public Distribution calc(Hand b, Hand h, Object[] args)
	{
		PokerValue V;
		PokerValue[] VBS;

		for (Card c : b.getCards())
			removeCard(c);

		for (Card c : h.getCards())
			removeCard(c);

		if (nopair(args))
		{
			removePairs();
			for (Card c : b.getCards())
				removeValue(c.getValue());
		}

		if (no2p(args))
		{
			List<PokerValue> pvs = b.getPairValues();
			if (pvs.size() == 0)
				for (int i = 0; i < b.size(); i++)
					for (int j = i + 1; j < b.size(); j++)
						removeCombination(b.getCard(i).getValue(), b.getCard(j)
								.getValue());
			else if (pvs.size() == 1)
				for (int i = 0; i < b.size(); i++)
					removeValue(b.getCard(i).getValue());
		}

		if (noset(args))
		{
			List<PokerValue> pvs = b.getPairValues();
			for (PokerValue pv : pvs)
				removeValue(pv);
			for (Card c : b.getCards())
				if (!pvs.contains(c.getValue()))
					removePair(c.getValue());
		}

		if (nostr8(args))
		{
			for (int i = 3; i < 13; i++)
			{
				PokerValue[] m = b.missingToStraight(PokerValue.values()[i],
						false);
				if (m.length == 1)
					removeValue(m[0]);
				else if (m.length == 2)
					removeCombination(m[0], m[1]);
			}
		}

		if (noflush(args))
		{
			PokerSuit ps = b.dominantSuit();
			int n = b.numOfSuit(ps);
			if (n == 4)
				removeSuit(ps);
			else if (n == 3)
				removeSuit2(ps);
		}

		if (nofull(args))
		{
			PokerValue sv;
			List<PokerValue> pvs = b.getPairValues();
			if (pvs.size() == 1)
			{
				for (Card c : b.getCards())
				{
					if (c.getValue() != pvs.get(0))
						removeCombination(c.getValue(), pvs.get(0));
					removePair(c.getValue());
				}
			}
			else if (pvs.size() == 2)
			{
				removeValue(pvs.get(0));
				removeValue(pvs.get(1));
				for (Card c : b.getCards())
					if (!pvs.contains(c.getValue()))
						removePair(c.getValue());
			}
			else if ((sv = b.getSetValue()) != null)
			{
				for (Card c : b.getCards())
					if (c.getValue() != sv)
						removeValue(c.getValue());
				removePairs();
			}
		}

		if (no4k(args))
		{
			PokerValue sv = b.getSetValue();
			List<PokerValue> pvs = b.getPairValues();
			if (!pvs.isEmpty() || (sv != null))
				removePairs();
			if (sv != null)
				removeValue(sv);
		}

		if (nostr8f(args))
		{
			PokerSuit s = b.dominantSuit();
			for (int i = 3; i < 13; i++)
			{
				PokerValue[] m = b.missingToStraight(PokerValue.values()[i],
						true);
				if (m.length == 1)
					removeCard(new Card(m[0], s));
				else if (m.length == 2)
					removeCombinationSuit(m[0], m[1], s);
			}
		}

		if ((V = nogt(args)) != null)
		{
			while (V.ordinal() < 13)
			{
				V = PokerValue.values()[V.ordinal() + 1];
				removeValue(V);
			}
		}

		if ((V = nopairg(args)) != null)
		{
			for (Card c : b.getCards())
				if (c.getValue().ordinal() > V.ordinal())
					removeValue(c.getValue());
			while (V.ordinal() < 13)
			{
				V = PokerValue.values()[V.ordinal() + 1];
				removePair(V);
			}
		}

		if ((VBS = no2pg(args)) != null)
		{
			PokerValue VB = VBS[0];
			PokerValue VS = VBS[1];

			List<PokerValue> pvs = b.getPairValues();
			for (PokerValue pv : pvs)
			{
				if (pv.ordinal() > VB.ordinal())
					for (Card c : b.getCards())
						if (!pvs.contains(c.getValue()))
							removeValue(c.getValue());
						else
							;
				else if (pv == VB)
				{
					for (int i = VS.ordinal() + 1; i < 13; i++)
						removePair(PokerValue.values()[i]);
					for (Card c : b.getCards())
						if (!pvs.contains(c.getValue())
								&& c.getValue().ordinal() > VS.ordinal())
							removeValue(c.getValue());
				}
				else
				{
					for (int i = VB.ordinal() + 1; i < 13; i++)
						removePair(PokerValue.values()[i]);
					for (Card c : b.getCards())
						if (!pvs.contains(c.getValue())
								&& c.getValue().ordinal() > VB.ordinal())
							removeValue(c.getValue());
				}
			}
		}

		if ((V = nosetg(args)) != null)
		{
			List<PokerValue> pvs = b.getPairValues();
			for (PokerValue pv : pvs)
				if (pv.ordinal() > V.ordinal())
					removeValue(pv);
			for (Card c : b.getCards())
				if (!pvs.contains(c.getValue()))
					removePair(c.getValue());
		}

		if ((V = nostr8g(args)) != null)
		{
			for (int i = V.ordinal() + 1; i < 13; i++)
			{
				PokerValue[] m = b.missingToStraight(PokerValue.values()[i],
						false);
				if (m.length == 1)
					removeValue(m[0]);
				else if (m.length == 2)
					removeCombination(m[0], m[1]);
			}
		}

		if ((V = noflushg(args)) != null)
		{
			PokerSuit s = b.dominantSuit();
			int n = b.numOfSuit(s);
			if (n == 3)
				removeEitherGtBothSuit(V, s);
			else if (n == 4)
				removeGtSuit(V, s);
		}

		if ((VBS = nofullg(args)) != null)
		{
			PokerValue sv = b.getSetValue();
			PokerValue VB = VBS[0];
			PokerValue VS = VBS[1];

			List<PokerValue> pvs = b.getPairValues();

			if (pvs.size() == 1)
			{
				PokerValue pv = pvs.get(0);
				if (pv.ordinal() < VS.ordinal())
					for (Card c : b.getCards())
						if (!pvs.contains(c.getValue())
								&& c.getValue().ordinal() > VB.ordinal())
							removePair(c.getValue());
						else
							;
				else
					for (Card c : b.getCards())
						if (!pvs.contains(c.getValue())
								&& c.getValue().ordinal() >= VB.ordinal())
							removePair(c.getValue());
						else
							;
				if (pv.ordinal() > VB.ordinal())
					for (Card c : b.getCards())
						if (!pvs.contains(c.getValue()))
							removeCombination(pv, c.getValue());
						else
							;
				else if (pv == VB)
					for (Card c : b.getCards())
						if (!pvs.contains(c.getValue())
								&& c.getValue().ordinal() > VS.ordinal())
							removeCombination(pv, c.getValue());
			}
			else if (pvs.size() == 2)
			{
				PokerValue pv1 = pvs.get(0);
				PokerValue pv2 = pvs.get(0);
				for (PokerValue v : pvs)
					if (v.ordinal() > VB.ordinal())
						removeValue(v);
				PokerValue t;
				if (pv2.ordinal() > pv1.ordinal())
				{
					t = pv1;
					pv1 = pv2;
					pv2 = t;
				}
				if (pv2 == VB)
					removeValue(VB);
				if (pv1 == VB)
					if (pv2.ordinal() > VS.ordinal())
						removeValue(VB);
			}
			if (sv != null)
			{
				if (sv.ordinal() > VB.ordinal())
				{
					for (Card c : b.getCards())
						if (c.getValue() != sv)
							removeValue(c.getValue());
						else
							;
					removePairs();
				}
				else if (sv == VB)
				{
					for (Card c : b.getCards())
						if (c.getValue() != sv
								&& c.getValue().ordinal() > VS.ordinal())
							removeValue(c.getValue());
						else
							;
					for (int i = VS.ordinal() + 1; i < 13; i++)
						removePair(PokerValue.values()[i]);
				}
			}
		}

		if ((V = no4kg(args)) != null)
		{
			List<PokerValue> pvs = b.getPairValues();
			for (PokerValue pv : pvs)
				if (pv.ordinal() > V.ordinal())
					removePair(pv);
			PokerValue sv = b.getSetValue();
			if (sv != null && sv.ordinal() > V.ordinal())
				removeValue(sv);
		}

		if ((V = nostr8fg(args)) != null)
		{
			PokerSuit s = b.dominantSuit();
			for (int i = V.ordinal() + 1; i < 13; i++)
			{
				PokerValue[] m = b.missingToStraight(PokerValue.values()[i],
						true);
				if (m.length == 1)
					removeCard(new Card(m[0], s));
				else if (m.length == 2)
					removeCombinationSuit(m[0], m[1], s);
			}
		}

		PokerValue[] ov = numOfVals(ONE, args);
		PokerSuit[] os = numOfSuits(ONE, args);
		PokerValue[] tv = numOfVals(TWO, args);
		PokerSuit[] ts = numOfSuits(TWO, args);

		if (ov.length == 1)
			keepHaving(ov[0], os[0]);
		else if (ov.length == 2)
			keepCombining(ov[0], os[0], ov[1], os[1]);
		else
			keepPair(tv[0], ts[0]);

		dist.normalize();
		return dist;
	}


	private boolean nopair(Object[] args)
	{
		for (Object o : args)
			if (o.equals(NOPAIRP))
				return true;
		return false;
	}


	private boolean no2p(Object[] args)
	{
		for (Object o : args)
			if (o.equals(NOPAIRP) || o.equals(NO2PP))
				return true;
		return false;
	}


	private boolean noset(Object[] args)
	{
		for (Object o : args)
			if (o.equals(NOPAIRP) || o.equals(NO2PP) || o.equals(NOSETP))
				return true;
		return false;
	}


	private boolean nostr8(Object[] args)
	{
		for (Object o : args)
			if (o.equals(NOPAIRP) || o.equals(NO2PP) || o.equals(NOSETP)
					|| o.equals(NOSTR8P))
				return true;
		return false;
	}


	private boolean noflush(Object[] args)
	{
		for (Object o : args)
			if (o.equals(NOPAIRP) || o.equals(NO2PP) || o.equals(NOSETP)
					|| o.equals(NOSTR8P) || o.equals(NOFLUSHP))
				return true;
		return false;
	}


	private boolean nofull(Object[] args)
	{
		for (Object o : args)
			if (o.equals(NOPAIRP) || o.equals(NO2PP) || o.equals(NOSETP)
					|| o.equals(NOSTR8P) || o.equals(NOFLUSHP)
					|| o.equals(NOFULLP))
				return true;
		return false;
	}


	private boolean no4k(Object[] args)
	{
		for (Object o : args)
			if (o.equals(NOPAIRP) || o.equals(NO2PP) || o.equals(NOSETP)
					|| o.equals(NOSTR8P) || o.equals(NOFLUSHP)
					|| o.equals(NOFULLP) || o.equals(NO4KP))
				return true;
		return false;
	}


	private boolean nostr8f(Object[] args)
	{
		for (Object o : args)
			if (o.equals(NOPAIRP) || o.equals(NO2PP) || o.equals(NOSETP)
					|| o.equals(NOSTR8P) || o.equals(NOFLUSHP)
					|| o.equals(NOFULLP) || o.equals(NO4KP) || o.equals(NOSFP))
				return true;
		return false;
	}


	private PokerValue nogt(Object[] args)
	{
		for (int i = 0; i < args.length; i++)
			if (args[i].equals(NOGT))
				return (PokerValue) args[i + 1];
		return null;
	}


	private PokerValue nopairg(Object[] args)
	{
		for (int i = 0; i < args.length; i++)
			if (args[i].equals(NOPAIRG))
				return (PokerValue) args[i + 1];
		return null;
	}


	private PokerValue[] no2pg(Object[] args)
	{
		for (int i = 0; i < args.length; i++)
			if (args[i].equals(NO2PG))
				return new PokerValue[] {(PokerValue) args[i + 1],
						(PokerValue) args[i + 2]};
		return null;
	}


	private PokerValue nosetg(Object[] args)
	{
		for (int i = 0; i < args.length; i++)
			if (args[i].equals(NOSETG))
				return (PokerValue) args[i + 1];
		return null;
	}


	private PokerValue nostr8g(Object[] args)
	{
		for (int i = 0; i < args.length; i++)
			if (args[i].equals(NOSTR8G))
				return (PokerValue) args[i + 1];
		return null;
	}


	private PokerValue noflushg(Object[] args)
	{
		for (int i = 0; i < args.length; i++)
			if (args[i].equals(NOFLUSHG))
				return (PokerValue) args[i + 1];
		return null;
	}


	private PokerValue[] nofullg(Object[] args)
	{
		for (int i = 0; i < args.length; i++)
			if (args[i].equals(NOFULLG))
				return new PokerValue[] {(PokerValue) args[i + 1],
						(PokerValue) args[i + 2]};
		return null;
	}


	private PokerValue no4kg(Object[] args)
	{
		for (int i = 0; i < args.length; i++)
			if (args[i].equals(NO4KG))
				return (PokerValue) args[i + 1];
		return null;
	}


	private PokerValue nostr8fg(Object[] args)
	{
		for (int i = 0; i < args.length; i++)
			if (args[i].equals(NOSFG))
				return (PokerValue) args[i + 1];
		return null;
	}


	private PokerValue[] numOfVals(String num, Object[] args)
	{
		int n = 0, c = 0;
		for (int i = 0; i < args.length; i++)
			if (args[i].equals(num))
				n++;
		PokerValue[] v = new PokerValue[n];
		for (int i = 0; i < args.length; i++)
			if (args[i].equals(num))
				v[c++] = (PokerValue) args[i + 1];
		return v;
	}


	private PokerSuit[] numOfSuits(String num, Object[] args)
	{
		int n = 0, c = 0;
		for (int i = 0; i < args.length; i++)
			if (args[i].equals(num))
				n++;
		PokerSuit[] s = new PokerSuit[n];
		for (int i = 0; i < args.length; i++)
			if (args[i].equals(num))
				if (i < (args.length - 3) && args[i + 2].equals(SUIT))
					s[c++] = (PokerSuit) args[i + 3];
				else
					s[c++] = null;
		return s;
	}


	private void removeValue(PokerValue v)
	{
		for (int i = 0; i < cards[0].length; i++)
		{
			Card c1 = cards[0][i];
			Card c2 = cards[1][i];
			PokerValue v1 = c1.getValue();
			PokerSuit s1 = c1.getSuit();
			PokerValue v2 = c2.getValue();
			PokerSuit s2 = c2.getSuit();

			if (v1 == v || v2 == v)
				data[i] = 0.0;
		}
	}


	private void removePair(PokerValue v)
	{
		for (int i = 0; i < cards[0].length; i++)
		{
			Card c1 = cards[0][i];
			Card c2 = cards[1][i];
			PokerValue v1 = c1.getValue();
			PokerSuit s1 = c1.getSuit();
			PokerValue v2 = c2.getValue();
			PokerSuit s2 = c2.getSuit();

			if (v1 == v && v2 == v)
				data[i] = 0.0;
		}
	}


	private void removeCard(Card c)
	{
		for (int i = 0; i < cards[0].length; i++)
		{
			Card c1 = cards[0][i];
			Card c2 = cards[1][i];
			PokerValue v1 = c1.getValue();
			PokerSuit s1 = c1.getSuit();
			PokerValue v2 = c2.getValue();
			PokerSuit s2 = c2.getSuit();

			if (c.equals(c1) || c.equals(c2))
				data[i] = 0.0;
		}
	}


	private void removePairs()
	{
		for (int i = 0; i < cards[0].length; i++)
		{
			Card c1 = cards[0][i];
			Card c2 = cards[1][i];
			PokerValue v1 = c1.getValue();
			PokerSuit s1 = c1.getSuit();
			PokerValue v2 = c2.getValue();
			PokerSuit s2 = c2.getSuit();

			if (v1 == v2)
				data[i] = 0.0;
		}
	}


	private void removeSuit(PokerSuit s)
	{
		for (int i = 0; i < cards[0].length; i++)
		{
			Card c1 = cards[0][i];
			Card c2 = cards[1][i];
			PokerValue v1 = c1.getValue();
			PokerSuit s1 = c1.getSuit();
			PokerValue v2 = c2.getValue();
			PokerSuit s2 = c2.getSuit();

			if (s1 == s || s2 == s)
				data[i] = 0.0;
		}
	}


	private void removeSuit2(PokerSuit s)
	{
		for (int i = 0; i < cards[0].length; i++)
		{
			Card c1 = cards[0][i];
			Card c2 = cards[1][i];
			PokerValue v1 = c1.getValue();
			PokerSuit s1 = c1.getSuit();
			PokerValue v2 = c2.getValue();
			PokerSuit s2 = c2.getSuit();

			if (s1 == s && s2 == s)
				data[i] = 0.0;
		}
	}


	private void removeCombination(PokerValue v1, PokerValue v2)
	{
		for (int i = 0; i < cards[0].length; i++)
		{
			Card c1 = cards[0][i];
			Card c2 = cards[1][i];
			PokerValue _v1 = c1.getValue();
			PokerSuit s1 = c1.getSuit();
			PokerValue _v2 = c2.getValue();
			PokerSuit s2 = c2.getSuit();

			if ((v1 == _v1 && v2 == _v2) || (v1 == _v2 && v2 == _v1))
				data[i] = 0.0;
		}
	}


	private void removeCombinationSuit(PokerValue v1, PokerValue v2, PokerSuit s)
	{
		for (int i = 0; i < cards[0].length; i++)
		{
			Card c1 = cards[0][i];
			Card c2 = cards[1][i];
			PokerValue _v1 = c1.getValue();
			PokerSuit s1 = c1.getSuit();
			PokerValue _v2 = c2.getValue();
			PokerSuit s2 = c2.getSuit();

			if (s1 != s || s2 != s)
				continue;

			if ((v1 == _v1 && v2 == _v2) || (v1 == _v2 && v2 == _v1))
				data[i] = 0.0;
		}
	}


	private void removeEitherGtBothSuit(PokerValue v, PokerSuit s)
	{
		for (int i = 0; i < cards[0].length; i++)
		{
			Card c1 = cards[0][i];
			Card c2 = cards[1][i];
			PokerValue v1 = c1.getValue();
			PokerSuit s1 = c1.getSuit();
			PokerValue v2 = c2.getValue();
			PokerSuit s2 = c2.getSuit();

			if (s1 != s || s2 != s)
				continue;

			if (v1.ordinal() > v.ordinal() || v2.ordinal() > v.ordinal())
				data[i] = 0.0;
		}
	}


	private void removeGtSuit(PokerValue v, PokerSuit s)
	{
		for (int i = 0; i < cards[0].length; i++)
		{
			Card c1 = cards[0][i];
			Card c2 = cards[1][i];
			PokerValue v1 = c1.getValue();
			PokerSuit s1 = c1.getSuit();
			PokerValue v2 = c2.getValue();
			PokerSuit s2 = c2.getSuit();

			if (s1 == s && v1.ordinal() > v.ordinal())
				data[i] = 0.0;

			if (s2 == s && v2.ordinal() > v.ordinal())
				data[i] = 0.0;
		}
	}


	private void keepHaving(PokerValue v, PokerSuit s)
	{
		for (int i = 0; i < cards[0].length; i++)
		{
			Card c1 = cards[0][i];
			Card c2 = cards[1][i];
			PokerValue v1 = c1.getValue();
			PokerSuit s1 = c1.getSuit();
			PokerValue v2 = c2.getValue();
			PokerSuit s2 = c2.getSuit();

			if (v1 == v && (s == null || s1 == s))
				continue;

			if (v2 == v && (s == null || s2 == s))
				continue;

			data[i] = 0.0;
		}
	}


	private void keepCombining(PokerValue v1, PokerSuit s1, PokerValue v2,
			PokerSuit s2)
	{
		for (int i = 0; i < cards[0].length; i++)
		{
			Card c1 = cards[0][i];
			Card c2 = cards[1][i];
			PokerValue _v1 = c1.getValue();
			PokerSuit _s1 = c1.getSuit();
			PokerValue _v2 = c2.getValue();
			PokerSuit _s2 = c2.getSuit();

			if ((_v1 == v1 && (s1 == null || _s1 == s1))
					&& (_v2 == v2 && (s2 == null || _s2 == s2)))
				continue;

			if ((_v1 == v2 && (s2 == null || _s1 == s2))
					&& (_v2 == v1 && (s1 == null || _s2 == s1)))
				continue;

			data[i] = 0.0;
		}
	}


	private void keepPair(PokerValue v, PokerSuit s)
	{
		for (int i = 0; i < cards[0].length; i++)
		{
			Card c1 = cards[0][i];
			Card c2 = cards[1][i];
			PokerValue v1 = c1.getValue();
			PokerSuit s1 = c1.getSuit();
			PokerValue v2 = c2.getValue();
			PokerSuit s2 = c2.getSuit();

			if (v1 == v && v2 == v && (s == null || (s1 == s && s2 == s)))
				continue;

			data[i] = 0.0;
		}
	}
}
