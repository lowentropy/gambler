/*
 * BucketTree.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package bayes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * Tree of buckets. Contains the code for processing variable elimination.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class BucketTree
{

	/** network to operate on */
	private BayesNet			net;

	/** ordering of nodes */
	private Ordering			ord;

	/** root bucket (contains query variables) */
	private Bucket				root;

	/** number of query variables */
	private int					numQuery;

	/** pool of network densities */
	private List<ProbFunction>	pool;

	/** query variables */
	private List<BayesNode>		qvars;


	/**
	 * Constructor.
	 * 
	 * @param ord
	 *            ordering
	 * @param numQuery
	 */
	public BucketTree(BayesNet net, Ordering ord, int numQuery)
	{
		this.net = net;
		this.ord = ord;
		this.numQuery = numQuery;

		BayesNode[] all = ord.getOrder();
		qvars = new ArrayList<BayesNode>();
		for (int i = all.length - numQuery; i < all.length; i++)
			qvars.add(all[i]);
	}


	/**
	 * Variable elimination.
	 * 
	 * @return solution density
	 * 
	 * @throws BayesError
	 */
	public ProbFunction eliminate() throws BayesError
	{
		/* add all network densities to pool */
		pool = new LinkedList<ProbFunction>();
		BayesNode[] vars = net.getVars();
		for (BayesNode n : vars)
			pool.add(n.getFunction());

		/* get requisite, non-observed, non-query nodes */
		int nObs = net.numObserved();
		BayesNode[] req = ord.getOrder();
		int N = req.length - numQuery - nObs;
		int B = nObs;
		
		/* iterate in minimum-weight order */
		for (int i = 0; i < N; i++)
		{
			BayesNode v = req[i + B];

			/* v-densities into new bucket, reinsert separator */
			List<ProbFunction> sub = remove(v);
			Bucket b = new Bucket(v, sub);
			b.eliminate();
			pool.add(b.getSeparator());
		}

		/* last bucket contains query-densities */
		List<ProbFunction> qsub = removeQuery();
		root = new Bucket(qsub);
		root.sumOutEvidence();

		return root.getSeparator();
	}


	/**
	 * Remove all densities from the pool which contain the variable 'var',
	 * returning them in a new list.
	 * 
	 * @param pool
	 *            pool to search through
	 * @param var
	 *            variable to check for
	 * @return list of densities containing var
	 */
	private List<ProbFunction> remove(BayesNode var)
	{
		List<ProbFunction> sub = new LinkedList<ProbFunction>();

		for (Iterator<ProbFunction> i = pool.iterator(); i.hasNext();)
		{
			ProbFunction f = i.next();
			if (f.getVariables().contains(var))
			{
				i.remove();
				sub.add(f);
			}
		}

		return sub;
	}


	/**
	 * Remove from the network
	 * 
	 * @return
	 */
	private List<ProbFunction> removeQuery()
	{
		List<ProbFunction> sub = new LinkedList<ProbFunction>();

		for (Iterator<ProbFunction> i = pool.iterator(); i.hasNext();)
		{
			ProbFunction f = i.next();
			for (BayesNode n : f.getVariables())
				if (qvars.contains(n))
				{
					i.remove();
					sub.add(f);
					break;
				}
		}

		return sub;
	}

}
