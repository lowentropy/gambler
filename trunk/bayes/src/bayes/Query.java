/*
 * Query.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package bayes;

import java.util.HashMap;
import java.util.Map;


/**
 * The Query class represents a method of achieving inputs (Evidence) into the
 * network, a set way to order the network (which caches the heuristic
 * ordering), and a standard set of nodes to query.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class Query
{

	/** Network to query. */
	private BayesNet					net;

	/** Ordering of nodes for given query set. */
	private Ordering					ord;

	/** set of query variables */
	private String[]					query;
	
	/** set of observed variables */
	private String[] obs;
	
	/** set of priors-changed variables */
	private String[] priors;

	/** bucket tree used to solve network */
	private BucketTree					tree;

	/** solution distribution; contains evidence */
	private ProbFunction				solution	= null;

	/** result distribution; has evidence summed out */
	private ProbFunction				result		= null;

	/** query-map */
	private Map<String, Distribution>	qmap;

	/** whether a solution has been generated */
	private boolean						cached		= false;

	/** whether an approximation has been generated */
	private boolean						approx		= false;

	private String	name;


	/**
	 * Constructor.
	 * 
	 * @param net
	 * @param query
	 * @throws BayesError
	 */
	public Query(String name, BayesNet net, String... query) throws BayesError
	{
		this.name = name;
		this.net = net;
		this.ord = new Ordering(net);
		this.setQueried(query);
	}


	/**
	 * Set query variables. If error occurs, does not set field.
	 * 
	 * @param query
	 *            new query variables
	 * @throws BayesError
	 */
	public void setQuery(String... query) throws BayesError
	{
		this.query = query;
		qmap.clear();
		for (String s : query)
			qmap.put(s, null);
		invalidate();
	}


	/**
	 * Solve the network.
	 * 
	 * @throws BayesError
	 */
	public void solve() throws BayesError
	{
		for (BayesNode node : net.getVars())
			node.setObserved(false);
		for (String var : obs)
			net.getNode(var).setObserved(true);
		
		if (!cached)
		{
			ord.order(query);
			tree = new BucketTree(net, ord, query.length);
			solution = tree.eliminate();
			cached = true;
		}
		
		result = solution.copy();
		result.removeObserved();
		result.normalize();
		queryResult();
	}


	/**
	 * Invalidate the solution; the structure of the network, or the set of
	 * observed variables, has changed.
	 */
	public void invalidate()
	{
		cached = false;
		approx = false;
	}


	/**
	 * Approximate a solution to the network.
	 * 
	 * @throws BayesError
	 */
	public void approx() throws BayesError
	{
		SimControl ctl = new RunOnceControl(100000, false);
		net.markovBlanket(ctl);
		net.queryDist(qmap);
		combineQueries();
		approx = true;
	}


	/**
	 * Take combined solution, and separate out conditional distributions for
	 * each query node, inserting them into the query-map.
	 * 
	 * @throws BayesError
	 */
	private void queryResult() throws BayesError
	{
		for (String name : query)
		{
			BayesNode n = net.getNode(name);
			ProbFunction f = result.sumOutExcept(n);
			f.normalizeConditional();
			qmap.put(name, new Distribution(f));
		}
	}


	/**
	 * Take each query variable's marginal distribution and combine them into a
	 * total density.
	 * @throws BayesError 
	 */
	private void combineQueries() throws BayesError
	{
		ProbFunction[] funcs = new ProbFunction[query.length];
		for (int i = 0; i < query.length; i++)
			funcs[i] = new ProbFunction(qmap.get(query[i]), net
					.getNode(query[i]));
		result = ProbFunction.multiply(funcs);
		result.normalize();
	}


	/**
	 * Show the solution, with evidence still in the distribution.
	 */
	public void showSolution()
	{
		if (cached)
			solution.print();
		else
			System.out.printf("no solution cached");
	}


	/**
	 * Show total distribution for all query variables, given evidence.
	 */
	public void showTotalDist()
	{
		if (cached)
			result.print();
		else
			System.out.printf("no solution cached");
	}


	/**
	 * Display the total distribution, but conditionally for the given variable.
	 * 
	 * @param var
	 *            variable to be conditioned for
	 * @throws BayesError
	 */
	public void showConditionalFor(String var) throws BayesError
	{
		if (cached)
			result.printConditional(var);
		else
			System.out.printf("no solution cached");
	}


	/**
	 * Show distribution for this variable, given other query variables and
	 * evidence.
	 * 
	 * @param var
	 *            variable to show conditional probability for
	 * @throws BayesError
	 */
	public void showDistFor(String var) throws BayesError
	{
		if (cached || approx)
		{
			if (!qmap.containsKey(var))
				throw new BayesError("variable '" + var + "' was not queried");
			qmap.get(var).print();
		}
		else
			System.out.printf("no solution or approximation cached");
	}


	/**
	 * Shows conditional distributions for each query variable, given all other
	 * nodes.
	 */
	public void showDistForAll()
	{
		if (cached || approx)
			for (String name : qmap.keySet())
			{
				Distribution dist = qmap.get(name);
				dist.print();
			}
		else
			System.out.printf("no solution or approximation cached");

	}
	

	/**
	 * Shows setup for query, including network and query name, observed states, and prior distributions.
	 * @throws BayesError 
	 */
	public void showQuery() throws BayesError
	{
		System.out.printf("net %s, query %s:\n", net.getName(), name);
		for (String v : obs)
			System.out.printf("\tEV: %s = %s\n", v, net.getNode(v).getState());
		for (String v : priors)
		{
			System.out.printf("\tPD: %s:\n", v);
			net.getNode(v).getFunction().print();
		}
	}


	/**
	 * Get calculated marginal probability for variable.
	 * 
	 * @param var
	 *            name of variable
	 * @return distribution
	 * @throws BayesError
	 */
	public Distribution getMarginal(String var) throws BayesError
	{
		if (cached || approx)
		{
			if (!qmap.containsKey(var))
				throw new BayesError("variable '" + var + "' was not queried");
			return qmap.get(var);
		}
		else
		{
			System.out.printf("no solution or approximation cached");
			return null;
		}
	}


	public void setObserved(String... vars)
	{
		obs = vars;
		invalidate();
	}


	public void setQueried(String... vars)
	{
		query = vars;
		
		qmap = new HashMap<String, Distribution>();
		for (String s : query)
			qmap.put(s, null);
		
		invalidate();
	}


	public void setPrior(String... vars)
	{
		priors = vars;
		invalidate();
	}

}
