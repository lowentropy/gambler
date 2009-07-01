
package poker.ai.bnet.loose;

import java.util.Map;
import java.util.HashMap;

import bayes.BayesError;
import bayes.BayesNet;
import bayes.BayesNode;
import bayes.Distribution;
import bayes.Query;
import poker.ai.bnet.PokerNet;

/*
 e/q/p   hole:
 -/-/-   hand: hole
 -/-/-   profit: hole
 -/-/-   strength: hand
 p/p/p   bias:
 -/-/-   mode: style, bias, profit, strength
 p/p/p   pot_size:
 p/p/q   style:
 -/-/-   pstrat: mode, pot_size
 e/e/e   action:
 q/e/e   strat: pstrat, action
 */

public class SPostflopNet extends PokerNet
{

	private boolean	fwdPrint	= true;

	private boolean	debug		= false;

	/** network nodes */
	private BayesNode	hole, hand, profit, strength, bias, mode, pot_size,
			style, pstrat, action, strat;

	/** queries for network */
	private Query		fwdQuery, holeQuery, biasQuery;

	public double[]		lastMoveDist;


	/**
	 * @see poker.ai.bnet.PokerNet#buildNetwork()
	 */
	public void buildNetwork() throws BayesError
	{
		// initialize net
		net = new BayesNet("sklansky_postflop");

		// initialize nodes
		hole = new BayesNode("hole", PokerNet.fullHoles);
		hand = new BayesNode("hand", PokerNet.hands);
		profit = new BayesNode("profit", "neg", "even", "double+");
		strength = new BayesNode("strength", "weak", "strong");
		bias = new BayesNode("bias", "T", "L");
		mode = new BayesNode("mode", "fold", "call", "raise", "build",
				"protect");
		pot_size = new BayesNode("pot_size", "small", "large");
		style = new BayesNode("style", "N", "D", "T", "LP", "LA");
		pstrat = new BayesNode("pstrat", "F", "C*", "B/C2", "B/R2", "B/R/C");
		action = new BayesNode("action", "NB", "B", "R");
		strat = new BayesNode("strat", "F", "CH", "B", "C", "R");

		// initialize links
		profit.addParent(hole);
		hand.addParent(hole);
		strength.addParent(hand);
		mode.addParent(style);
		mode.addParent(bias);
		mode.addParent(strength);
		mode.addParent(profit);
		pstrat.addParent(mode);
		pstrat.addParent(pot_size);
		strat.addParent(action);
		strat.addParent(pstrat);

		// add nodes to network
		net.addNode(hole);
		net.addNode(hand);
		net.addNode(profit);
		net.addNode(strength);
		net.addNode(mode);
		net.addNode(pot_size);
		net.addNode(style);
		net.addNode(pstrat);
		net.addNode(action);
		net.addNode(strat);
		net.addNode(bias);

		// initialize prior distributions
		initHoleTable();

		// initialize conditional distributions
		initStrengthTable();
		initModeTable();
		initPStratTable();
		initStratTable();
	}


	@Override
	public void buildQueries() throws BayesError
	{
		fwdQuery = new Query("fwd", net);
		fwdQuery.setObserved("hole", "action");
		fwdQuery.setQueried("strat");
		fwdQuery.setPrior("style", "pot_size");

		holeQuery = new Query("hole", net);
		holeQuery.setObserved("action", "strat");
		holeQuery.setQueried("hole");
		holeQuery.setPrior("style", "pot_size");

		biasQuery = new Query("style", net);
		biasQuery.setObserved("action", "strat");
		biasQuery.setQueried("style");
		biasQuery.setPrior("pot_size", "hole");
	}


	/**
	 * @see poker.ai.bnet.PokerNet#setParam(java.lang.String, java.lang.Object)
	 */
	public void setParam(String name, Object value)
	{
		throw new IllegalArgumentException("invalid parameter name: " + name);
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

			// if (fwdPrint && query == fwdQuery)
			// {
			// net.inference();
			// Map<String,Distribution> dm = new HashMap<String,Distribution>();
			// for (String s : new String[]
			// {"hand","profit","mode","pot_size","style","pstrat","action","strat"})
			// dm.put(s,null);
			// net.distQuery(dm);
			// lastProfit = dm.get("profit").values.clone();
			// }

			query.solve();

			if (debug)
				query.showTotalDist();

			for (String v : qmap.keySet())
				qmap.put(v, query.getMarginal(v));

			if (query == fwdQuery)
				lastMoveDist = qmap.get("strat").values.clone();
		}
		catch (BayesError e)
		{
			e.printStackTrace();
		}
	}


	/**
	 * Initialize the density function for the hole variable.
	 */
	private void initHoleTable()
	{
		int c = 0;
		double[] dist = hole.getFunction().getData();

		for (int i = 0; i < dist.length; i++)
			dist[i] = 0.00037707390648567121;
	}


	/**
	 * Initialize the density function for the strength variable.
	 */
	private void initStrengthTable()
	{
		double[] input = new double[] {0, .2, .3, .4, .5, .5, .8, 1.0, 1.0};
		double[] dist = strength.getFunction().getData();
		for (int i = 0; i < input.length; i++)
		{
			dist[i] = 1.0 - input[i];
			dist[i + input.length] = input[i];
		}
	}


	/**
	 * Initialize the density function for the mode variable.
	 */
	private void initModeTable()
	{
		double[] dist = mode.getFunction().getData();

		// style, bias, strength, profit
		// 5 * 2 * 2 * 3
		int[] m = new int[] {0, 1, 3, 0, 1, 4, 0, 1, 3, 0, 1, 1, 0, 1, 1, 0, 2,
				2, 0, 1, 1, 0, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1,
				1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
				2};

		int i = 0;
		int b = 60;
		
		for (int s = 0; s < 5; s++)
			for (int j = 0; j < b; j++)
				dist[i++] = (m[j] == s) ? 1.0 : 0.0;
	}


	/**
	 * Initialize the density function for the pstrat variable.
	 */
	private void initPStratTable()
	{
		double[] dist = pstrat.getFunction().getData();
		
		// mode, pot_size
		// 5 * 2
		int[] input = new int[] {0, 0, 1, 1, 3, 3, 1, 3, 4, 2};
		
		int i = 0;
		int b = 10;
		
		for (int s = 0; s < 5; s++)
			for (int j = 0; j < b; j++)
				dist[i++] = (input[j] == s) ? 1.0 : 0.0;

		// f,* = f (0)
		// c,* = c* (1)
		// r,* = b/r2 (3)
		// b,s = c* (1)
		// b,l = b/r2 (3)
		// p,s = b/r/c (4)
		// p,l = b/c2 (2)
	}


	/**
	 * Initialize the density function for the strat variable.
	 */
	private void initStratTable()
	{
		double[] dist = strat.getFunction().getData();
		int[] input = new int[] {1, 1, 2, 2, 2, 0, 3, 3, 4, 4, 0, 3, 3, 4, 3};
		int b = dist.length / 5;
		for (int i = 0; i < dist.length; i++)
			dist[i] = 0.0;
		for (int i = 0; i < input.length; i++)
			dist[b * input[i] + i] = 1.0;
		// n,f = ch
		// n,c* = ch
		// n,b* = b
		// b,f = f
		// b,c* = c
		// b,b/c2 = c
		// b,b/r* = r
		// r,f = f
		// r,c* = c
		// r,b/c2 = c
		// r,b/r2 = r
		// r,b/r/c = c
	}


	public BayesNode getHoleNode()
	{
		return hole;
	}


	public void initHandTable(double[] dist) throws BayesError
	{
		hand.getFunction().setData(dist);
	}


	public void initProfitTable(double[] dist) throws BayesError
	{
		profit.getFunction().setData(dist);
	}
}
