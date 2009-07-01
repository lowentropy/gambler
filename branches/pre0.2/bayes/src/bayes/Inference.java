/*
 * Inference.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package bayes;

import java.util.Random;


/**
 * Collection of inference methods.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class Inference
{

	/**
	 * Calculate marginal probability of a node given the state of its
	 * conditioning variables and its conditional probability given those
	 * variables.
	 * 
	 * @param func
	 *            conditional probability function
	 * @param dist
	 *            marginal distribution (holder)
	 * @throws BayesError
	 */
	public static void conditional(ProbFunction func, Distribution dist)
			throws BayesError
	{
		double x, y;
		int t, i, j, k;

		int idx[] = new int[func.numVars() - 1];
		int len[] = func.getLengths();

		double[] cond = func.getData();
		double[][] prev = new double[idx.length][];
		int dlen = cond.length / len[0];

		for (i = 0; i < idx.length; i++)
			prev[i] = func.getVar(i + 1).getPosterior();

		t = 0;
		for (k = 0; k < len[0]; k++)
		{
			y = 0.0;

			for (j = 0; j < idx.length; j++)
				idx[j] = 0;

			for (int u = 0; u < dlen; u++)
			{
				x = 1.0;
				for (i = 0; i < idx.length; i++)
					x *= prev[i][idx[i]];

				j = idx.length - 1;
				while (j >= 0)
					if (++idx[j] == len[j + 1])
					{
						idx[j] = 0;
						j--;
					}
					else
						break;

				y += x * cond[t++];
			}

			dist.values[k] = y;
		}

		dist.normalize(Distribution.tolerance);
	}


	public static int blanket(Random r, ProbFunction func, Distribution dist,
			BayesNode[] mb_child_obj, int[] mb_child_base, int[] mb_child_idx,
			int pt) throws BayesError
	{
		double p, q, sum = 0.0;
		double[] cond = func.getData();
		double[][] cpri = new double[mb_child_obj.length][];
		int jump = cond.length / dist.states.length;
		int base = 0;

		/* get children's conditional distributions */
		for (int i = 0; i < cpri.length; i++)
			cpri[i] = mb_child_obj[i].getConditional();

		/* iterate possible states */
		for (int i = 0; i < dist.states.length; i++)
		{
			/* conditional probability of node given parents */
			p = cond[base + pt];
			base += jump;

			/* conditional probability of children given node */
			for (int j = 0; j < mb_child_idx.length; j++)
			{
				p *= cpri[j][mb_child_idx[j]];
				mb_child_idx[j] += mb_child_base[j];
			}

			/* save to posterior dist; can be queried */
			dist.values[i] = p;
			sum += p;
		}

		/* normalize */
		if (sum > 0.0)
			for (int i = 0; i < dist.values.length; i++)
				dist.values[i] /= sum;

		/* now choose the next state */
		q = r.nextDouble();
		p = 0.0;
		for (int i = 0; i < dist.values.length; i++)
		{
			p += dist.values[i];
			if (p > q)
				return i;
		}

		/* float error got us here */
		return dist.states.length - 1;
	}
}
