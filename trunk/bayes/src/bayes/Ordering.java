/*
 * Ordering.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package bayes;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Contains heuristic methods for ordering network variables for variable/bucket
 * elimination algorithms.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class Ordering
{

	/** ordering of variables */
	private BayesNode[] ord;

	/** network we order for */
	private BayesNet net;

	/**
	 * Constructor
	 * 
	 * @param net
	 *            network
	 */
	public Ordering(BayesNet net)
	{
		this.net = net;
	}

	/**
	 * Order network's nodes.
	 * 
	 * @param qvars
	 *            name of variables to query
	 * @throws BayesError
	 */
	public void order(String... qvars) throws BayesError
	{
		BayesNode[] vars = net.getVars();
		boolean[] qmap = new boolean[vars.length];
		boolean[] vmap = new boolean[qvars.length];

		for (int i = 0; i < vars.length; i++)
		{
			qmap[i] = false;
			for (int j = 0; j < qvars.length; j++)
				if (vars[i].getVariable().equals(qvars[j]))
				{
					qmap[i] = true;
					vmap[j] = true;
					break;
				}
		}

		for (int i = 0; i < qvars.length; i++)
			if (!vmap[i])
				throw new BayesError("network does not contain variable '"
						+ qvars[i] + "'");

		order(vars, qmap);
	}

	/**
	 * Order network's nodes and store to net's heuristic ordering array.
	 * 
	 * @param net
	 *            network
	 * @param qmap
	 *            map of query variables
	 */
	private void order(BayesNode[] vars, boolean[] qmap)
	{
		boolean doq = false;
		int nvto, nq, n, idx, min, val;
		int[] ridx;
		BayesNode[] vto;
		List[] links;

		/* get # of non-evidence vars */
		ord = new BayesNode[vars.length];
		nvto = nq = n = 0;
		for (int i = 0; i < vars.length; i++)
		{
			if (qmap[i])
				nq++;
			if (!vars[i].isObserved())
				nvto++;
			else
				ord[n++] = vars[i];
		}

		/* get array of non-evidence vars */
		vto = new BayesNode[nvto];
		ridx = new int[nvto];
		nvto = 0;
		for (int i = 0; i < vars.length; i++)
			if (!vars[i].isObserved())
			{
				ridx[nvto] = i;
				vars[i].setOrderingIdx(nvto);
				vto[nvto++] = vars[i];
			}

		/* initialize links list */
		links = new List[vars.length];
		for (int i = 0; i < nvto; i++)
		{
			List<BayesNode> tmp = new LinkedList<BayesNode>();
			tmp.add(vto[i]);
			links[ridx[i]] = tmp;
		}

		/* interlink neighborhoods */
		for (int i = 0; i < nvto; i++)
			link(links, ridx, vto[i].getFunction().getVariables());

		/* order nodes heuristically, non-query nodes first */
		while (n < vars.length)
		{
			/* find minimum weight node */
			min = 0;
			idx = -1;
			for (int i = 0; i < vto.length; i++)
			{
				int j = ridx[i];
				if ((links[j] == null) || (!doq && qmap[j]))
					continue;
				val = weight(links[j]);
				if ((idx == -1) || (val < min))
				{
					idx = i;
					min = val;
				}
			}

			if (idx != -1)
			{
				/* interconnect neighbors and clear link list; order node */
				for (int i = 0; i < links.length; i++)
					if (links[i] != null)
						links[i].remove(vto[idx]);
				ord[n++] = vto[idx];
				link(links, ridx, links[ridx[idx]]);
				links[ridx[idx]] = null;
			}

			/* move to next phase */
			if (n == (vars.length - nq))
				doq = true;
		}
	}

	/**
	 * Interconnect group of nodes, each with each.
	 * 
	 * @param links
	 *            array of link vectors, one for each variable in network
	 * @param ridx
	 *            mapping of vto[*] to vars[*]
	 * @param group
	 *            group of nodes to interlink
	 */
	private void link(List[] links, int[] ridx, Collection group)
	{
		Object[] arr = group.toArray();
		for (int i = 0; i < (arr.length - 1); i++)
			for (int j = i + 1; j < arr.length; j++)
				link(links, ridx, (BayesNode) arr[i], (BayesNode) arr[j]);
	}

	/**
	 * Interlink two nodes by adding each to the other's link vector.
	 * 
	 * @param links
	 *            array of link vectors, one for each variable in network
	 * @param ridx
	 *            mapping of vto[*] to vars[*]
	 * @param n1
	 *            first node to link
	 * @param n2
	 *            second node to link
	 */
	@SuppressWarnings("unchecked")
	private void link(List[] links, int[] ridx, BayesNode n1, BayesNode n2)
	{
		List<BayesNode> l1 = n1.isObserved() ? null
				: (List<BayesNode>) links[ridx[n1.getOrderingIdx()]];
		List<BayesNode> l2 = n2.isObserved() ? null
				: (List<BayesNode>) links[ridx[n2.getOrderingIdx()]];

		if ((l1 == null) || (l2 == null))
			return;

		if (!l1.contains(n2))
			l1.add(n2);
		if (!l2.contains(n1))
			l2.add(n1);
	}

	/**
	 * Weight of a node is geometric sum of number of states of variables in
	 * neighborhood of node.
	 * 
	 * @param links
	 *            neighborhood of node
	 * @return weight of node
	 */
	private int weight(List links)
	{
		int w = 1;
		for (Object o : links)
			w *= ((BayesNode) o).numStates();
		return w;
	}

	/**
	 * Print ordering.
	 */
	public void print()
	{
		StringBuilder sb = new StringBuilder();
		for (BayesNode n : ord)
			sb.append(n.getVariable() + ", ");
		sb.setLength(sb.length() - 2);
		System.out.println(sb.toString());
	}

	/**
	 * @return ordering of nodes
	 */
	public BayesNode[] getOrder()
	{
		return ord;
	}
}
