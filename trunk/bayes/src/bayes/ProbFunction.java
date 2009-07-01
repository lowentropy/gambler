/*
 * ProbFunction.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package bayes;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * Probability function (acts either as prior or conditional).
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class ProbFunction
{

	private static StringWriter	sWriter;

	private static PrintWriter	pWriter;

	/** topological variable indexes */
	private List<BayesNode>		vars;

	/** conditional distribution */
	private double[]			dist;

	static
	{
		sWriter = new StringWriter();
		pWriter = new PrintWriter(sWriter);
	}


	/**
	 * Constructor.
	 * 
	 * @param variable
	 * @param nstates
	 */
	public ProbFunction(BayesNode var)
	{
		dist = new double[var.numStates()];
		for (int i = 0; i < var.numStates(); i++)
			dist[i] = (i == 0) ? 1.0 : 0.0;
		vars = new ArrayList<BayesNode>();
		vars.add(var);
	}


	/**
	 * Constructor.
	 * 
	 * @param vars
	 * @param dist
	 */
	public ProbFunction(List<BayesNode> vars, double[] dist)
	{
		this.vars = vars;
		this.dist = dist;
	}


	/**
	 * @param stream
	 * @param nstates
	 * @throws IOException
	 */
	public ProbFunction(DataInputStream stream, BayesNode firstVar,
			Collection<BayesNode> otherVars) throws IOException
	{
		vars = new ArrayList<BayesNode>();
		vars.add(firstVar);

		int mult = firstVar.numStates();
		for (BayesNode n : otherVars)
		{
			vars.add(n);
			mult *= n.numStates();
		}

		dist = new double[mult];
		for (int i = 0; i < mult; i++)
			dist[i] = stream.readDouble();
	}


	/**
	 * Create a probability density out of a marginal distribution.
	 * 
	 * @param dist
	 *            marginal probability density
	 */
	public ProbFunction(Distribution dist, BayesNode var)
	{
		this(var);
		this.dist = dist.values;
	}


	/**
	 * Add a variable to the distribution.
	 * 
	 * @param p
	 */
	public void addVariable(BayesNode p)
	{
		int nlen = p.numStates();

		double[] ndist = new double[dist.length * nlen];

		for (int i = 0, j = 0; i < dist.length; i++, j += nlen)
			Arrays.fill(ndist, j, j + nlen, dist[i]);

		dist = ndist;
		vars.add(p);
	}


	/**
	 * Remove a variable from the function. Choose one of that parents' states
	 * to decide the remaining distribution.
	 * 
	 * @param idx
	 *            index of parent to remove
	 * @param chc
	 *            index of state in parent to choose for replacement
	 */
	public void removeVariable(int idx, int chc)
	{
		int base, dest;
		int bsz, off, chk, num, nst;

		nst = vars.get(idx).numStates();

		bsz = 1;
		for (int i = idx + 1; i < vars.size(); i++)
			bsz *= vars.get(i).numStates();

		num = 1;
		for (int i = 0; i < idx; i++)
			num *= vars.get(i).numStates();

		chk = bsz * nst;
		off = bsz * chc;

		double[] ndist = new double[dist.length / nst];

		base = 0;
		dest = 0;
		for (int i = 0; i < num; i++)
		{
			System.arraycopy(dist, base + off, ndist, dest, bsz);
			base += chk;
			dest += bsz;
		}

		dist = ndist;
		vars.remove(idx);
	}


	/**
	 * Add a state to the parent variable at given index.
	 * 
	 * @param idx
	 *            index of variable
	 */
	public void addState(int idx)
	{
		int base, dest;
		int nlen, chk, bsz, num, nst;

		nst = vars.get(idx).numStates(); // this is the NEW # states

		num = 1;
		for (int i = 0; i < idx; i++)
			num *= vars.get(i).numStates();

		bsz = 1;
		for (int i = idx + 1; i < vars.size(); i++)
			bsz *= vars.get(i).numStates();

		chk = bsz * (nst - 1);
		nlen = dist.length + (num * bsz);

		double[] ndist = new double[nlen];

		base = 0;
		dest = 0;
		for (int j = 0; j < num; j++)
		{
			System.arraycopy(dist, base, ndist, dest, chk);
			System.arraycopy(dist, base + chk - bsz, ndist, dest + chk, bsz);
			base += chk;
			dest += chk + bsz;
		}

		dist = ndist;
	}


	/**
	 * Remove the given state from the given parent variable.
	 * 
	 * @param pidx
	 *            index of parent variable
	 * @param sidx
	 *            index of state in parent to remove
	 */
	public void removeState(int pidx, int sidx)
	{
		int base, dest;
		int num, bsz, chk, off, rem, nlen;

		BayesNode p = vars.get(pidx);

		num = 1;
		for (int i = 0; i < pidx; i++)
			num *= vars.get(i).numStates();

		bsz = 1;
		for (int i = pidx + 1; i < vars.size(); i++)
			bsz *= vars.get(i).numStates();

		chk = bsz * (p.numStates() + 1);
		off = bsz * sidx;
		rem = chk - (off + bsz);
		nlen = dist.length - (num * bsz);

		double[] ndist = new double[nlen];

		base = 0;
		dest = 0;
		for (int i = 0; i < num; i++)
		{
			System.arraycopy(dist, base, ndist, dest, off);
			System.arraycopy(dist, base + off + bsz, ndist, dest + off, rem);
			base += chk;
			dest += (chk - bsz);
		}

		dist = ndist;
		normalizeConditional();
	}


	/**
	 * @return the most likely state, given a discretely chosen state of
	 *         conditioning variables
	 */
	public int mostLikely()
	{
		int base = 0;
		int mult = 1;

		for (int i = vars.size() - 1; i > 0; i--)
		{
			base += mult * vars.get(i).getStateIdx();
			mult *= vars.get(i).numStates();
		}

		int idx = 0, maxi = -1;
		double max = 0.0;

		for (int i = base; i < dist.length; i += mult, idx++)
		{
			if ((maxi == -1) || (dist[i] > max))
			{
				max = dist[i];
				maxi = idx;
			}
		}

		return maxi;
	}


	/**
	 * Write probability function to output.
	 * 
	 * @param stream
	 *            output stream
	 * @throws IOException
	 */
	public void write(DataOutputStream stream) throws IOException
	{
		for (int i = 0; i < dist.length; i++)
			stream.writeDouble(dist[i]);
	}


	/**
	 * Set the raw distribution data.
	 * 
	 * @param ndist
	 * @throws BayesError
	 */
	public void setData(double[] ndist) throws BayesError
	{
		if (ndist.length != dist.length)
			throw new BayesError(
					"new distribution has wrong number of dimensions ("
							+ ndist.length + " should be " + dist.length + ")");

		dist = ndist;
	}


	/**
	 * @return raw distribution data
	 */
	public double[] getData()
	{
		return dist;
	}


	/**
	 * Set raw (prior) distribution data.
	 * 
	 * @param ndist
	 * @throws BayesError
	 */
	public void setPrior(double[] ndist) throws BayesError
	{
		setData(ndist);
	}


	/**
	 * @return raw (prior) distribution data (error if not prior dist)
	 * @throws BayesError
	 */
	public double[] getPrior() throws BayesError
	{
		if (vars.size() != 1)
			throw new BayesError(
					"node has conditional distribution (dist length: "
							+ dist.length + ", parents: " + vars.size() + ")");

		return dist;
	}


	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o)
	{
		if ((o == null) || !(o instanceof ProbFunction))
			return false;

		return Arrays.equals(dist, ((ProbFunction) o).dist);
	}


	/**
	 * Return distribution variable of given index.
	 * 
	 * @param idx
	 *            index of variable
	 * @return conditioning variable
	 */
	public BayesNode getVar(int idx)
	{
		return vars.get(idx);
	}


	/**
	 * @return number of distribution variables
	 */
	public int numVars()
	{
		return vars.size();
	}


	/**
	 * @return array of each variables' number of states
	 */
	public int[] getLengths()
	{
		int[] len = new int[vars.size()];
		for (int i = 0; i < len.length; i++)
			len[i] = vars.get(i).numStates();
		return len;
	}


	/**
	 * Normalize the distribution conditional on the first variable.
	 */
	public void normalizeConditional()
	{
		double s;
		int n = vars.get(0).numStates();
		int j = dist.length / n;

		for (int i = 0; i < j; i++)
		{
			s = 0.0;
			for (int k = 0; k < n; k++)
				s += dist[i + k * j];
			if (s > 0.0)
				for (int k = 0; k < n; k++)
					dist[i + k * j] /= s;
		}
	}


	/**
	 * Normalize conditional on the idx'th variable.
	 * 
	 * @param idx
	 *            index of variable to normalize for.
	 */
	private void normalizeConditional(int idx)
	{
		int incm, prem = 1, postm = 1, ns = 0;
		for (int i = 0; i < vars.size(); i++)
			if (i < idx)
				prem *= vars.get(i).numStates();
			else if (i > idx)
				postm *= vars.get(i).numStates();
			else
				ns = vars.get(i).numStates();
		incm = postm * ns;

		for (int i = 0, l0 = 0; i < prem; i++, l0 += incm)
			for (int j = 0, l1 = l0; j < postm; j++, l1++)
			{
				double s = 0.0;
				for (int k = 0, l2 = l1; k < ns; k++, l2 += postm)
					s += dist[l2];
				if (s > 0.0)
					for (int k = 0, l2 = l1; k < ns; k++, l2 += postm)
						dist[l2] /= s;
			}
	}


	/**
	 * Normalize the total distribution.
	 */
	public void normalize()
	{
		double s = 0.0;
		for (int i = 0; i < dist.length; i++)
			s += dist[i];
		if (s > 0.0)
			for (int i = 0; i < dist.length; i++)
				dist[i] /= s;
	}


	/**
	 * @return probability variables
	 */
	public Collection<BayesNode> getVariables()
	{
		return vars;
	}


	/**
	 * Multiply many distributions together.
	 * 
	 * @param funcs
	 *            distributions to multiply
	 * @return denormalized, multiplied distribution
	 */
	public static ProbFunction multiply(ProbFunction... funcs)
	{
		/* these are the fields of the new distribution */
		double[] dist;
		List<BayesNode> vars = new ArrayList<BayesNode>();

		/* collect all variables into list */
		int mult = 1;
		for (ProbFunction f : funcs)
			for (BayesNode n : f.vars)
				if (!vars.contains(n))
				{
					mult *= n.numStates();
					vars.add(n);
				}

		/* initialize var array and distribution */
		BayesNode[] vara = vars.toArray(new BayesNode[0]);
		dist = new double[mult];

		/* map vars into indexes to facilitate next step */
		Map<String, Integer> vmap = new HashMap<String, Integer>();
		for (int i = 0; i < vara.length; i++)
			vmap.put(vara[i].getVariable(), i);

		/* set up function base/idx to speed up mult loop */
		int[] fidx = new int[funcs.length];
		int[][] fbase = new int[funcs.length][vara.length];
		for (int i = 0; i < funcs.length; i++)
		{
			mult = 1;
			for (int j = funcs[i].vars.size() - 1; j >= 0; j--)
			{
				int idx = vmap.get(funcs[i].vars.get(j).getVariable());
				fbase[i][idx] = mult;
				mult *= vara[idx].numStates();
			}
		}

		/* initialize state counter as array of indices, lengths */
		int[] idx = new int[vars.size()];
		int[] len = new int[vars.size()];
		mult = 1;
		for (int i = 0; i < len.length; i++)
			mult *= len[i] = vara[i].numStates();

		/* iterate possible states of idx */
		for (int i = 0; i < mult; i++)
		{
			double p = 1.0;

			/* multiply p by each function's sub-index of 'idx' */
			for (int j = 0; j < funcs.length; j++)
				p *= funcs[j].dist[fidx[j]];
			dist[i] = p;

			/* iterate index from least to greatest */
			int j = idx.length - 1;
			while (j >= 0)
			{
				/* recursive update of index */
				if (++idx[j] == len[j])
				{
					for (int k = 0; k < funcs.length; k++)
						fidx[k] -= ((idx[j] - 1) * fbase[k][j]);
					idx[j--] = 0;
				}
				else
				{
					for (int k = 0; k < funcs.length; k++)
						fidx[k] += fbase[k][j];
					break;
				}
			}
		}

		/* return new distribution */
		return new ProbFunction(vars, dist);
	}


	/**
	 * Sum out some variables from this distribution.
	 * 
	 * @param nodes
	 *            nodes to sum out
	 * @return denormalized distribution with given variables summed out
	 * @throws BayesError
	 */
	public ProbFunction sumOut(BayesNode... nodes) throws BayesError
	{
		List<BayesNode> list = new ArrayList<BayesNode>(nodes.length);
		for (BayesNode n : nodes)
			list.add(n);
		return sumOut(list);
	}


	/**
	 * Sum out variables except the ones given from this distribution.
	 * 
	 * @param nodes
	 *            nodes NOT to sum out
	 * @return denormalized distribution with given variables summed out
	 * @throws BayesError
	 */
	public ProbFunction sumOutExcept(BayesNode... nodes) throws BayesError
	{
		List<BayesNode> list = new ArrayList<BayesNode>(nodes.length);
		for (BayesNode n : vars)
		{
			int j = 0;
			for (j = 0; j < nodes.length; j++)
				if (n == nodes[j])
					break;
			if (j == nodes.length)
				list.add(n);
		}
		return sumOut(list);
	}


	/**
	 * Sum out some variables from this distribution.
	 * 
	 * @param outs
	 *            variables to sum out
	 * @return denormalized distribution with given variables summed out
	 * @throws BayesError
	 */
	public ProbFunction sumOut(Collection<BayesNode> outs) throws BayesError
	{
		for (BayesNode n : outs)
			if (!this.vars.contains(n))
				throw new BayesError("can't sum out '" + n.getVariable()
						+ "', not in distribution");

		int imult = 1, omult = 1;
		int num_out = outs.size();
		int num_in = vars.size() - num_out;
		List<BayesNode> ins = new ArrayList<BayesNode>(num_in);

		/* get lengths and idx bases */
		int[] len = getLengths();
		int[] base = new int[len.length];
		System.arraycopy(len, 1, base, 0, len.length - 1);
		base[len.length - 1] = 1;
		for (int i = base.length - 2; i >= 0; i--)
			base[i] *= base[i + 1];

		/* get ins/outs indexes */
		int vi = 0, ii = 0, oo = 0;
		int[] iidx = new int[num_in];
		int[] oidx = new int[num_out];
		for (BayesNode n : vars)
			if (outs.contains(n))
			{
				oidx[oo++] = vi++;
				omult *= n.numStates();
			}
			else
			{
				iidx[ii++] = vi++;
				imult *= n.numStates();
				ins.add(n);
			}

		/* initialize positions */
		int ictr = 0, octr;
		int[] ipos = new int[num_in];
		int[] opos = new int[num_out];
		double[] ndist = new double[imult];

		/* iterate left-in states */
		for (int i = 0; i < imult; i++)
		{
			double sum = 0.0;
			octr = ictr;

			/* sum left-out states */
			for (int j = 0; j < omult; j++)
			{
				sum += dist[octr];

				/* increment 'out' index-array and counter */
				int k = opos.length - 1;
				while (k >= 0)
				{
					if (++opos[k] == len[oidx[k]])
					{
						octr = octr - ((opos[k] - 1) * base[oidx[k]]);
						opos[k--] = 0;
					}
					else
					{
						octr += base[oidx[k]];
						break;
					}
				}
			}

			ndist[i] = sum; // store sum

			/* increment 'in' index-array and counter */
			int k = ipos.length - 1;
			while (k >= 0)
			{
				if (++ipos[k] == len[iidx[k]])
				{
					ictr = ictr - ((ipos[k] - 1) * base[iidx[k]]);
					ipos[k--] = 0;
				}
				else
				{
					ictr += base[iidx[k]];
					break;
				}
			}
		}

		return new ProbFunction(ins, ndist);
	}


	/**
	 * Remove observed variables by choosing their observed states to replace
	 * their distribution.
	 */
	public void removeObserved()
	{
		for (int i = 0; i < vars.size();)
		{
			BayesNode v = vars.get(i);
			if (v.isObserved())
				removeVariable(i, v.getStateIdx());
			else
				i++;
		}
	}


	/**
	 * @return copy of distribution
	 */
	public ProbFunction copy()
	{
		List<BayesNode> nvars = new LinkedList<BayesNode>();
		for (BayesNode n : vars)
			nvars.add(n);

		return new ProbFunction(nvars, dist.clone());
	}


	/**
	 * Print the distribution.
	 */
	public void print()
	{
		if (vars.size() == 2)
			printConditional(0, false);
		else
			printTable();
	}


	/**
	 * Print probability distribution table.
	 */
	private void printTable()
	{
		System.out.printf(" prob      |");

		/* get all variable names */
		String[] vn = new String[vars.size()];
		for (int i = 0; i < vn.length; i++)
			vn[i] = vars.get(i).getVariable();

		/* get all state names */
		String[][] vst = new String[vars.size()][];
		for (int i = 0; i < vst.length; i++)
			vst[i] = vars.get(i).getMarginal().states;

		/* get column widths */
		int[] sl = new int[vst.length];
		for (int i = 0; i < sl.length; i++)
		{
			int maxl = vn[i].length();
			for (int j = 0; j < vst[i].length; j++)
				if (vst[i][j].length() > maxl)
					maxl = vst[i][j].length();
			if (maxl < 9)
				maxl = 9;
			sl[i] = maxl;
		}

		/* initialize recursive counter */
		int[] idx = new int[vst.length];
		int[] len = new int[vst.length];
		for (int i = 0; i < vst.length; i++)
			len[i] = vars.get(i).numStates();

		/* print column headings */
		for (int i = 0; i < vn.length; i++)
			printw("| %s ", sl[i] + 3, vn[i]);
		System.out.printf("\n");

		/* print columns */
		for (int i = 0; i < dist.length; i++)
		{
			System.out.printf(" %8.4f%% |", dist[i] * 100.0);
			for (int j = 0; j < vst.length; j++)
				printw("| %s ", sl[j] + 3, vst[j][idx[j]]);
			System.out.printf("\n");
			int j = vst.length - 1;
			while (j >= 0)
				if (++idx[j] == len[j])
					idx[j--] = 0;
				else
					break;
		}
	}


	/**
	 * Print distribution when normalized for the given variable.
	 * 
	 * @param var
	 *            variable to condition on
	 * @throws BayesError
	 */
	public void printConditional(String var) throws BayesError
	{
		int i = 0;
		for (BayesNode n : vars)
		{
			if (n.getVariable().equals(var))
				break;
			i++;
		}
		if (i == vars.size())
			throw new BayesError("variable '" + var
					+ "' not part of distribution");
		printConditional(i, true);
	}


	/**
	 * Internal conditional print method. Conditional for given variable index,
	 * and may or not normalize.
	 * 
	 * @param idx
	 *            index of variable to print conditionally for
	 * @param norm
	 *            whether to normalize for that variable
	 */
	private void printConditional(int idx, boolean norm)
	{
		int[] sl;
		double[] backup = null;

		/* save unnormalized density and renormalize */
		if (norm)
		{
			backup = dist.clone();
			normalizeConditional(idx);
		}

		/* calculate pre-mult, post-mult, inc-mult, and num states */
		int lm = 1, rm = 1, im, ns = 0;
		for (int i = 0; i < vars.size(); i++)
			if (i < idx)
				lm *= vars.get(i).numStates();
			else if (i > idx)
				rm *= vars.get(i).numStates();
			else
				ns = vars.get(i).numStates();
		im = rm * ns;

		/* get all state names */
		String[] states = null;
		states = vars.get(idx).getMarginal().states;
		String[][] vst = new String[ns][];
		vst[idx] = states;

		/* get column widths on left side */
		sl = new int[states.length + vars.size() - 1];
		for (int i = 0; i < ns; i++)
		{
			sl[i] = states[i].length();
			if (sl[i] < 9)
				sl[i] = 9;
		}

		/* get column widths on right side */
		for (int i = 0, c = ns; i < vars.size(); i++)
		{
			if (i == idx)
				continue;

			BayesNode n = vars.get(i);
			vst[i] = n.getMarginal().states;

			int maxl = n.getVariable().length();

			for (int j = 0; j < vst[i].length; j++)
				if (vst[i][j].length() > maxl)
					maxl = vst[i][j].length();

			sl[c++] = maxl;
		}

		/* print top header */
		System.out.printf("states of '%s' (%d):\n",
				vars.get(idx).getVariable(), ns);

		/* print col headers */
		for (int i = 0; i < ns; i++)
			printw(" %s |", sl[i] + 3, states[i]);
		for (int i = 0, c = ns; i < vars.size(); i++)
		{
			if (i == idx)
				continue;
			printw("| %s ", sl[c++] + 3, vars.get(i).getVariable());
		}
		System.out.printf("\n");

		/* initialize recursive index counter */
		int[] idxs = new int[vars.size()];
		int[] lens = new int[vars.size()];
		for (int i = 0; i < vars.size(); i++)
			lens[i] = (i == idx) ? 1 : vars.get(i).numStates();

		/* iterate data */
		for (int i = 0, l0 = 0; i < lm; i++, l0 += im)
		{
			for (int j = 0, l1 = l0; j < rm; j++, l1++)
			{
				/* print left side */
				for (int k = 0, l2 = l1; k < ns; k++, l2 += rm)
					printw(" %8.4f%% |", sl[k] + 3, dist[l2] * 100.0);

				/* print right side */
				for (int k = 0, c = ns; k < vars.size(); k++)
				{
					if (k == idx)
						continue;
					printw("| %s ", sl[c++] + 3, vst[k][idxs[k]]);
				}
				System.out.printf("\n");

				/* update recursive index */
				int k = vars.size() - 1;
				while (k >= 0)
					if (++idxs[k] == lens[k])
						idxs[k--] = 0;
					else
						break;
			}
		}

		/* restore unnormalized density */
		if (norm)
			dist = backup;
	}


	/**
	 * Print formatted string and make sure it's the given width.
	 * 
	 * @param fmt
	 *            format of string
	 * @param len
	 *            length of string (total)
	 * @param vars
	 *            varables to pass to formatter
	 */
	private void printw(String fmt, int len, Object... vars)
	{
		StringBuffer buf = sWriter.getBuffer();
		buf.setLength(0);
		pWriter.printf(fmt, vars);
		int bl = buf.length();
		for (int i = 0; i < (len - bl); i++)
			buf.insert(0, ' ');
		System.out.print(buf.toString());
	}


	/**
	 * Set conditional data for state 0, assumed to be 'True', and set the only
	 * other state, assumed to be 'False', to the inverse.
	 * 
	 * @param data
	 *            data to set for 'True'
	 * @throws BayesError
	 */
	public void setTrueData(double[] data) throws BayesError
	{
		if ((data.length * 2) != dist.length)
			throw new BayesError(
					"invalid distribution for truth values (expected "
							+ (dist.length / 2) + ", was " + data.length + ")");

		System.arraycopy(data, 0, dist, 0, data.length);
		for (int i = 0; i < data.length; i++)
			dist[data.length + i] = 1.0 - data[i];
	}


	/**
	 * Set the data for conditional values, given the parents are in the given
	 * states.
	 * 
	 * @param data
	 *            data for child
	 * @param states
	 *            states of parents
	 * @throws BayesError
	 */
	public void setStateData(double[] data, int... states) throws BayesError
	{
		if (data.length != vars.get(0).numStates())
			throw new BayesError(
					"data array wrong length for # condition states");
		if (states.length != (vars.size() - 1))
			throw new BayesError("wrong # of states given for # of parents");

		int mult = 1;
		int off = 0;
		for (int i = vars.size(); i > 0; i--)
		{
			off += states[i - 1] * mult;
			mult *= vars.get(i).numStates();
		}

		for (int i = 0, base = off; i < data.length; i++, base += mult)
			dist[base] = data[i];
	}


	public void verify()
	{
		int n = vars.get(0).numStates();
		int d = dist.length / n;
		for (int i = 0; i < d; i++)
		{
			double s = 0.0;
			for (int j = i; j < dist.length; j += d)
			{
				//System.err.printf("%f + %f = %f\n", s, dist[j], s + dist[j]); // DBG
				s += dist[j];
			}
			// System.err.printf("\n"); // DBG
			if (Math.abs(1.0 - s) > 0.00001)
			{
				System.err.printf("cdist for '%s' invalid (sum=%f, i=%d)\n", vars.get(0).getName(), s, i);
				break;
			}
		}
	}
}
