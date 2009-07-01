/*
 * Bucket.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package bayes;

import java.util.List;


/**
 * A bucket contains a cluster of distributions, a separator distribution, and
 * it's corresponding bucket variable. It is used in a generalized variable
 * elimination algorithm [Generalizing Variable Elimination in Bayesian
 * Networks, by Fabio Gagliardi Cozman].
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class Bucket
{

	/** bucket variable: eliminated in this bucket */
	private BayesNode			variable;

	/** bucket density pool */
	private List<ProbFunction>	pool;

	/** cluster of densities (mult all pool densisites) */
	private ProbFunction		cluster;

	/** separator density (sum out bucket var from cluster) */
	private ProbFunction		separator;


	/**
	 * Constructor.
	 * 
	 * @param v
	 * @param sub
	 */
	public Bucket(BayesNode v, List<ProbFunction> sub)
	{
		variable = v;
		pool = sub;
	}


	/**
	 * @param qsub
	 */
	public Bucket(List<ProbFunction> qsub)
	{
		pool = qsub;
	}


	/**
	 * @return separator
	 */
	public ProbFunction getSeparator()
	{
		return separator;
	}


	/**
	 * For a query node, get cluster, then sum out evidence from cluster.
	 */
	public void sumOutEvidence()
	{
		cluster = separator = ProbFunction.multiply(pool
				.toArray(new ProbFunction[0]));
	}


	/**
	 * Eliminate the bucket variable from the cluster to obtain the separator.
	 * 
	 * @throws BayesError
	 */
	public void eliminate() throws BayesError
	{
		cluster = ProbFunction.multiply(pool.toArray(new ProbFunction[0]));
		separator = cluster.sumOut(variable);
	}
}
