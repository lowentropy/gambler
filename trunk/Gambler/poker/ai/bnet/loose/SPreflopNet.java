
package poker.ai.bnet.loose;

import java.util.Arrays;
import java.util.Map;

import poker.ai.bnet.PokerNet;
import poker.util.SklanskyGrouper;
import bayes.BayesError;
import bayes.BayesNet;
import bayes.BayesNode;
import bayes.Distribution;
import bayes.Query;

/**
 * SPreflopNet implements a PokerNet for preflop actions, based on the book
 * 'Winning Low-Stakes Holdem' by David Sklansky et al.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class SPreflopNet extends PokerNet
{

	/** whether to print debugging output */
	private boolean	debug	= false;

	/** bayesnet nodes for network */
	private BayesNode	hole, bias, style, pos, group, action, strat, in_pot;

	/** queries for network */
	private Query		fwdQuery, holeQuery, biasQuery, grpQuery;

	/** names of S groups */
	private String[]	sGroupNames;

	/** S groups */
	private String[][]	sGroups;

	/** table of valid S groups */
	private String[][]	sTable;


	/**
	 * @see poker.ai.bnet.PokerNet#buildNetwork()
	 */
	public void buildNetwork() throws BayesError
	{
		// get loose groups based on sklansky et al.
		getSGroups();

		// initialize net
		net = new BayesNet("sklansky_preflop");

		// initialize nodes
		hole = new BayesNode("hole", PokerNet.holePairs);
		group = new BayesNode("group", sGroupNames);
		bias = new BayesNode("bias", "T", "L");
		style = new BayesNode("style", "N", "D", "T", "LP", "LA");
		pos = new BayesNode("pos", "E", "M", "L", "SB", "BB");
		action = new BayesNode("action", "NR", "R", "RR");
		in_pot = new BayesNode("in_pot", "not_3", "not_4", "4_more");
		strat = new BayesNode("strat", "F", "C", "R");

		// initialize node links
		group.addParent(hole);
		strat.addParent(style);
		strat.addParent(bias);
		strat.addParent(pos);
		strat.addParent(action);
		strat.addParent(in_pot);
		strat.addParent(group);

		// add nodes to network
		net.addNode(hole); // e/q/p
		net.addNode(style); // p/p/q
		net.addNode(bias); // p/p/p
		net.addNode(pos); // e/e/e
		net.addNode(action); // e/e/e
		net.addNode(in_pot); // e/e/e
		net.addNode(group); // -/-/-
		net.addNode(strat); // q/e/e

		// initialize prior distributions
		initHoleTable();

		// initialize conditional distributions
		initGroupTable();
		initStratTable();

		verifyTables();
	}


	/**
	 * @see poker.ai.bnet.PokerNet#buildQueries()
	 */
	public void buildQueries() throws BayesError
	{
		fwdQuery = new Query("fwd", net);
		fwdQuery.setObserved("hole", "pos", "action", "in_pot");
		fwdQuery.setQueried("strat");
		fwdQuery.setPrior("bias", "style");

		holeQuery = new Query("hole", net);
		holeQuery.setObserved("pos", "action", "in_pot", "strat");
		holeQuery.setQueried("hole");
		holeQuery.setPrior("bias", "style");

		biasQuery = new Query("bias", net);
		biasQuery.setObserved("pos", "action", "in_pot", "strat");
		biasQuery.setPrior("bias", "hole");
		biasQuery.setQueried("style");

		grpQuery = new Query("group", net);
		grpQuery
				.setObserved("hole", "pos", "action", "in_pot", "bias", "style");
		grpQuery.setQueried("group");
		grpQuery.setPrior();
	}


	/**
	 * @see poker.ai.bnet.PokerNet#setParam(java.lang.String, Object)
	 */
	public void setParam(String name, Object value)
	{
		if (name.equals("bias"))
		{
			double[] data = (double[]) value;
			try
			{
				bias.setPrior(data);
			}
			catch (BayesError e)
			{
				throw new IllegalArgumentException("invalid bias", e);
			}
		}
		else
		{
			throw new IllegalArgumentException("invalid parameter name: "
					+ name);
		}
	}


	/**
	 * @see poker.ai.bnet.PokerNet#compute(java.lang.String, java.util.Map)
	 */
	public void compute(String name, Map<String, String> omap,
			Map<String, Distribution> pmap, Map<String, Distribution> qmap)
	{
		Query query;
		if (name.equals("fwd"))
			query = fwdQuery;
		else if (name.equals("hole"))
			query = holeQuery;
		else if (name.equals("style"))
			query = biasQuery;
		else if (name.equals("group"))
			query = grpQuery;
		else
			throw new IllegalArgumentException("invalid computation name: "
					+ name);

		try
		{
			net.clearEvidence();
			for (String v : omap.keySet())
				net.getNode(v).observe(omap.get(v));
			for (String v : pmap.keySet())
			{
				net.getNode(v).setPrior(pmap.get(v).getData());
				query.invalidate();
			}

			if (debug)
				query.showQuery();

			if (name.equals("fwd"))
			{
				net.inference();
				for (String v : qmap.keySet())
					qmap.put(v, net.getNode(v).getMarginal());
			}
			else
			{
				query.solve();

				// if (debug)
				// query.showSolution();

				if (debug)
					query.showTotalDist();

				for (String v : qmap.keySet())
					qmap.put(v, query.getMarginal(v));
			}
		}
		catch (BayesError e)
		{
			e.printStackTrace();
		}
	}


	/**
	 * Verify sanity of conditional distributions.
	 */
	private void verifyTables()
	{
		BayesNode[] nodes = new BayesNode[] {hole, bias, style, pos, group,
				action, strat, in_pot};
		for (BayesNode node : nodes)
			node.getFunction().verify();
	}


	/**
	 * Set the bias distribution.
	 * 
	 * @param dist
	 *            bias distribution
	 */
	public void setBias(Distribution dist)
	{
		try
		{
			bias.setConditional(dist.getData());
		}
		catch (BayesError e)
		{
			throw new IllegalArgumentException("invalid bias distribution", e);
		}

		fwdQuery.invalidate();
		biasQuery.invalidate();
		holeQuery.invalidate();
		grpQuery.invalidate();
	}


	public void setStyle(Distribution dist)
	{
		try
		{
			style.setConditional(dist.getData());
		}
		catch (BayesError e)
		{
			throw new IllegalArgumentException("invalid style distribution", e);
		}

		fwdQuery.invalidate();
		biasQuery.invalidate();
		holeQuery.invalidate();
		grpQuery.invalidate();
	}


	/**
	 * Get the loose groups based on Sklansky et al.
	 */
	private void getSGroups()
	{
		SklanskyGrouper sg = new SklanskyGrouper();
		sg.compute();
		sGroupNames = sg.getGroupNames();
		sGroups = sg.getGroups();
		sTable = sg.getTable();
	}


	/**
	 * Initialize the density function for the hole variable.
	 */
	public void initHoleTable()
	{
		int c = 0;
		double[] dist = hole.getFunction().getData();

		Arrays.fill(dist, 0.0);

		/* pairs */
		for (int i = 0; i < 13; i++)
			dist[c++] = 0.0045248868778280547;

		/* unsuited */
		for (int i = 0; i < 78; i++)
			dist[c++] = 0.0090497737556561094;

		/* suited */
		for (int i = 0; i < 78; i++)
			dist[c++] = 0.0030165912518853697;
	}


	/**
	 * Initialize the density function for the strat variable.
	 */
	private void initStratTable()
	{
		double[] dist = strat.getConditional();
		Arrays.fill(dist, 0.0);

		int f = 0, c = dist.length / 3, r = c * 2;
		int n = 0, d = c / 5, t = d * 2, lp = d * 3, la = d * 4;

		// action, style, bias, pos, action, in_pot, group
		for (int i = 0, k = 0; i < sTable.length; i += 2)
		{
			for (int j = 0; j < sGroups.length; j++, k++)
			{
				if (arrayContains(sTable[i + 1], sGroupNames[j]))
				{
					dist[r + k] = 1.0;
				}
				else if (arrayContains(sTable[i + 0], sGroupNames[j]))
				{
					dist[c + k] = 1.0;
				}
				else
				{
					dist[f + k] = 1.0;
				}
			}
		}

		for (int a = 0; a < dist.length; a += c)
			for (int i = n; i < d; i++)
				dist[a + d + i] = dist[a + i];

		for (int i = t; i < lp; i++)
			dist[f + i] = 1.0;

		for (int i = lp; i < la; i++)
			dist[c + i] = 1.0;

		for (int i = la; i < c; i++)
			dist[r + i] = 1.0;
	}


	/**
	 * Initialize the density function for the group variable.
	 */
	private void initGroupTable()
	{
		double[] dist = group.getConditional();
		Arrays.fill(dist, 0.0);
		for (int i = 0, k = 0; i < sGroups.length; i++)
			for (int j = 0; j < 169; j++, k++)
				dist[k] = arrayContains(sGroups[i], PokerNet.holePairs[j]) ? 1.0
						: 0.0;
	}


	/**
	 * Return whether the given array contains the given string.
	 * 
	 * @param array
	 *            array of strings
	 * @param elem
	 *            string element
	 * @return whether array contains element
	 */
	private static boolean arrayContains(String[] array, String elem)
	{
		for (String s : array)
			if (s.equals(elem))
				return true;
		return false;
	}
}
