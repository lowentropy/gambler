/*
 * BayesNode.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package bayes;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

/**
 * Single node of network, representing a variable of the total joint
 * probability. Contains a distribution for probabilitic inference, but can also
 * be iterated in a Markov Chain.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class BayesNode
{

	/** printable name of node */
	private String variable;

	/** parent links */
	private ArrayList<BayesNode> parents;

	/** children links */
	private ArrayList<BayesNode> children;

	/** posterior (marginal or total) distribution */
	private Distribution dist;

	/** distribution function (either direct or conditional) */
	private ProbFunction func;

	/** index into values (for markov chain simulation) */
	private int state;

	/** evidence value, whether 'evidence' true or false */
	private int ev_state;

	/** whether node has evidence */
	private boolean observed;

	/** node index in topological order (used for storing/loading and ) */
	private int topoIndex;

	/** the increment in each child's dist[x] which our 'state' indexes by */
	private int[] mb_child_base;

	/** the index int each child's dist[x] of our markov blanket */
	private int[] mb_child_idx;

	/** child object pointers */
	private BayesNode[] mb_child_obj;

	/** counts number of times each state is sampled on this node */
	private int[] stateCounter;

	private int ordIdx;

	/**
	 * Empty private constructor used by the static readNode() function.
	 */
	private BayesNode()
	{
	}

	/**
	 * Constructs an unconnected node with no distribution information.
	 * 
	 * @param name
	 *            node name
	 * @param states
	 *            state names
	 * @throws BayesError
	 */
	public BayesNode(String name, String... states) throws BayesError
	{
		this();

		if (states.length < 1)
			throw new BayesError("must have at least one state");

		this.variable = name;
		this.parents = new ArrayList<BayesNode>();
		this.children = new ArrayList<BayesNode>();
		this.dist = new Distribution(variable, states, 0);
		this.func = new ProbFunction(this);
		this.stateCounter = new int[states.length];
		this.state = -1;
		this.observed = false;
		this.topoIndex = -1;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode()
	{
		return variable.hashCode();
	}

	/**
	 * @return
	 */
	public boolean isOrderedTopologically()
	{
		return (topoIndex != -1);
	}

	/**
	 * @param index
	 */
	public void orderTopologically(int index)
	{
		topoIndex = index;
	}

	/**
	 * @return
	 */
	public int getTopologicalIndex()
	{
		return topoIndex;
	}

	/**
	 * @return
	 */
	public Collection<BayesNode> getParents()
	{
		return parents;
	}

	/**
	 * @return
	 */
	public Collection<BayesNode> getChildren()
	{
		return children;
	}

	/**
	 * @return
	 */
	public String getVariable()
	{
		return variable;
	}

	/**
	 * @return
	 */
	public boolean isObserved()
	{
		return observed;
	}

	/**
	 * Log the current posterior distribution.
	 * 
	 * @param logname
	 *            name of log
	 */
	public void record(String logname)
	{
		dist.record(logname);
	}

	/**
	 * Reload the saved state.
	 * 
	 * @param logname
	 *            name of log
	 * @throws BayesError
	 */
	public void recall(String logname) throws BayesError
	{
		dist.recall(logname);
	}

	/**
	 * Add a parent node. The distributions are copied for each of the parents'
	 * states (the parent is being added to the beginning).
	 * 
	 * @param p
	 *            parent node
	 * @throws BayesError
	 */
	public void addParent(BayesNode p) throws BayesError
	{
		p.children.add(this);
		func.addVariable(p);
		parents.add(p);
	}

	/**
	 * Remove the given parent, choosing the distribution of the given state to
	 * fill in (as opposed to averaging the distribution across its states, or
	 * something).
	 * 
	 * @param p
	 *            parent node
	 * @param state
	 *            state of parent to choose distribution from
	 * @throws BayesError
	 */
	public void removeParent(BayesNode p, String state) throws BayesError
	{
		int idx, chc;

		for (idx = 0; idx < parents.size(); idx++)
			if (parents.get(idx) == p)
				break;

		if (idx == parents.size())
			throw new BayesError("node '" + p.variable + "' not a parent of '"
					+ variable + "'");

		p.children.remove(this);

		for (chc = 0; chc < p.numStates(); chc++)
			if (p.dist.states[chc].equals(state))
				break;

		if (chc == p.numStates())
			throw new BayesError("removal parent '" + p.variable
					+ "' has no state '" + state + "'");

		func.removeVariable(idx + 1, chc);
	}

	/**
	 * Add a state to the node. Modifies children nodes' distributions
	 * appropriately. Their distribution is copied from the last existing
	 * distribution based on this parent node. The parent's prior or conditional
	 * distribution gives the new state a 0 probability.
	 * 
	 * @param state
	 *            new state name
	 * @throws BayesError
	 */
	public void addState(String state) throws BayesError
	{
		dist.addState(state);
		func.addState(0);

		for (BayesNode c : children)
			c.parentAddedState(this);
	}

	/**
	 * Removes the given state. Modifies children nodes by removing this
	 * distribution. This node's prior or conditional distribution is
	 * renormalized.
	 * 
	 * @param state
	 *            state to remove
	 * @throws BayesError
	 */
	public void removeState(String state) throws BayesError
	{
		int idx = dist.removeState(state);
		func.removeState(0, idx);

		for (BayesNode c : children)
			c.parentRemovedState(this, idx);
	}

	/**
	 * A parent added a state; copy the distribution for the last state of that
	 * variable to the new state.
	 * 
	 * @param p
	 *            parent node
	 * @throws BayesError
	 */
	private void parentAddedState(BayesNode p) throws BayesError
	{
		int idx;

		for (idx = 0; idx < parents.size(); idx++)
			if (parents.get(idx) == p)
				break;

		func.addState(idx + 1);
	}

	/**
	 * Parent node removed a state. Remove it from our conditional distribution.
	 * 
	 * @param p
	 *            parent node
	 * @param idx
	 *            index of state in old parent node's states
	 * @throws BayesError
	 */
	private void parentRemovedState(BayesNode p, int sidx) throws BayesError
	{
		int pidx;

		for (pidx = 0; pidx < parents.size(); pidx++)
			if (parents.get(pidx) == p)
				break;

		func.removeState(pidx + 1, sidx);
	}

	/**
	 * Perform local probalitic inference on distributions.
	 * 
	 * @throws BayesError
	 */
	public void inference() throws BayesError
	{
		/* evidence node posterior dist; state = 1.0, others = 0.0 */
		if (observed)
			dist.choose(state);

		/* priors with no evidence copy prior to posterior */
		else if (parents.size() == 0)
			dist.copy(func);

		else
			Inference.conditional(func, dist);
	}

	/**
	 * Initialize the node with a likely state, and prepare indices for blanket
	 * calculation.
	 */
	public void markovInitialize()
	{
		if (observed)
			return;

		/* initialize state to most likely */
		state = func.mostLikely();

		/* clear state counters */
		for (int i = 0; i < numStates(); i++)
			stateCounter[i] = 0;

		/* prepare common variables for blanket function */
		mb_child_base = new int[children.size()];
		mb_child_idx = new int[children.size()];
		mb_child_obj = new BayesNode[children.size()];
		for (int i = 0; i < mb_child_base.length; i++)
		{
			BayesNode c = mb_child_obj[i] = children.get(i);
			mb_child_base[i] = 1;
			for (int j = c.parents.size(); j >= 1; j--)
				if (c.func.getVar(j) == this)
					break;
				else
					mb_child_base[i] *= c.func.getVar(j).numStates();
		}
	}

	/**
	 * Perform local markov blanket calculations. A markov blanket includes a
	 * node's direct parents, children, and nodes which share a child with it.
	 * 
	 * @param r
	 *            random object
	 * @throws BayesError
	 */
	public void markovBlanket(Random r) throws BayesError
	{
		/* calculate pt up here; it won't change */
		int pt = distIndex();

		/* get initial child indices */
		state = 0;
		for (int j = 0; j < mb_child_idx.length; j++)
		{
			BayesNode c = mb_child_obj[j];
			int off = (c.getConditional().length / c.numStates()) * c.state;
			int idx = c.distIndex();
			mb_child_idx[j] = idx + off;
		}

		state = Inference.blanket(r, func, dist, mb_child_obj, mb_child_base,
				mb_child_idx, pt);
		stateCounter[state]++;
	}

	/**
	 * @return index into dist[x] given parent states
	 */
	private int distIndex()
	{
		int idx = 0;
		int base = 1;

		for (int i = parents.size() - 1; i >= 0; i--)
		{
			BayesNode p = parents.get(i);
			idx += base * p.state;
			base *= p.numStates();
		}

		return idx;
	}

	/**
	 * @return posterior distribution
	 * @throws BayesError
	 */
	public Distribution getMarginal()
	{
		try
		{
			return getMarginal(null);
		} catch (BayesError e)
		{
			return null;
		}
	}

	/**
	 * @param logname
	 *            name of saved node to query, or null if current
	 * @return posterior distribution
	 * @throws BayesError
	 */
	public Distribution getMarginal(String logname) throws BayesError
	{
		return dist.retrieve(logname);
	}

	/**
	 * Reload evidence value and mark node as having it.
	 */
	public void reloadEvidence()
	{
		observed = true;
		state = ev_state;
	}

	/**
	 * Read an input node from the stream.
	 * 
	 * @param net
	 *            net to which node belongs
	 * @param stream
	 *            input stream
	 * @param index
	 *            topological index
	 * @return reconstructed bayes node
	 * @throws IOException
	 * @throws BayesError
	 */
	public static BayesNode readNode(BayesNet net, DataInputStream stream,
			int index) throws IOException, BayesError
	{
		int nparents;
		BayesNode n = new BayesNode();

		n.topoIndex = index;
		n.variable = stream.readUTF();
		n.observed = stream.readBoolean();
		n.ev_state = stream.readInt();

		if (n.observed)
			n.state = n.ev_state;

		nparents = stream.readInt();
		n.parents = new ArrayList<BayesNode>(nparents);
		n.children = new ArrayList<BayesNode>();

		for (int i = 0; i < nparents; i++)
		{
			BayesNode p = net.getNode(stream.readInt());
			n.parents.add(p);
			p.children.add(n);
		}

		n.dist = new Distribution(stream, n.variable);
		n.func = new ProbFunction(stream, n, n.parents);

		n.stateCounter = new int[n.dist.states.length];

		return n;
	}

	/**
	 * Write this node on the output stream.
	 * 
	 * @param stream
	 *            output stream
	 * @throws IOException
	 */
	public void write(DataOutputStream stream) throws IOException
	{
		stream.writeUTF(variable);
		stream.writeBoolean(observed);
		stream.writeInt(ev_state);

		stream.writeInt(parents.size());
		for (BayesNode p : parents)
			stream.writeInt(p.topoIndex);

		dist.write(stream);
		func.write(stream);
	}

	/**
	 * Sets the conditional distribution, checking it is a valid distribution of
	 * correct dimensions.
	 * 
	 * @param ndist
	 *            new distribution
	 * @throws BayesError
	 */
	public void setConditional(double[] ndist) throws BayesError
	{
		func.setData(ndist);
	}

	/**
	 * @return the conditional distribution
	 */
	public double[] getConditional()
	{
		return func.getData();
	}

	/**
	 * Set prior distribution.
	 * 
	 * @param dist
	 *            prior distribution
	 * @throws BayesError
	 */
	public void setPrior(double... ndist) throws BayesError
	{
		func.setPrior(ndist);
	}

	/**
	 * @return prior distribution
	 * @throws BayesError
	 */
	public double[] getPrior() throws BayesError
	{
		return func.getPrior();
	}

	/**
	 * @return posterior distribution (raw form)
	 */
	public double[] getPosterior()
	{
		return dist.getData();
	}

	/**
	 * 
	 * @param state
	 *            state to set node to
	 * @throws BayesError
	 */
	public void observe(String state) throws BayesError
	{
		int idx;

		for (idx = 0; idx < numStates(); idx++)
			if (dist.states[idx].equals(state))
				break;

		if (idx == numStates())
			throw new BayesError("no state '" + state + "' of variable '"
					+ variable + "' exists");

		ev_state = this.state = idx;
		observed = true;
	}

	/**
	 * Determine if this node is equal to the given node, but only insofar as
	 * its distribution, states, and parents' variable names are equal.
	 * 
	 * @param n
	 *            node to check against
	 * @return whether nodes are net-equals
	 */
	public boolean equalsForNet(BayesNode n)
	{
		if (!variable.equals(n.variable)) // not strictly necessary
			return false;

		/* check evidence value / truth */
		if (observed != n.observed)
			return false;

		if (ev_state != n.ev_state)
			return false;

		/* check states */
		if (!dist.equalsForNet(n.dist))
			return false;

		/* check parents */
		if (parents.size() != n.parents.size())
			return false;

		for (int i = 0; i < parents.size(); i++)
			if (!parents.get(i).variable.equals(n.parents.get(i).variable))
				return false;

		if (!func.equals(n.func))
			return false;

		return true;
	}

	/**
	 * @return computed average posterior distribution
	 */
	public double[] getAverage()
	{
		if (observed)
			return dist.getData();

		double sum = 0.0;
		for (int i = 0; i < stateCounter.length; i++)
			sum += (double) stateCounter[i];

		double[] avg = new double[stateCounter.length];
		for (int i = 0; i < avg.length; i++)
			avg[i] = stateCounter[i] / sum;

		return avg;
	}

	/**
	 * @param b
	 */
	public void setObserved(boolean b)
	{
		observed = b;
	}

	/**
	 * @return whether node is prior node
	 */
	public boolean isPrior()
	{
		return (observed == false) && (parents.size() == 0);
	}

	/**
	 * Map computed average approximated posterior distribution onto the prior
	 * distribution. Throw an error if no average was computed.
	 * 
	 * @throws BayesError
	 */
	public void mapAvgToPrior() throws BayesError
	{
		double[] avg = getAverage();
		func.setPrior(avg);
	}

	/**
	 * @return number of states
	 */
	public int numStates()
	{
		return dist.states.length;
	}

	/**
	 * @return observed state
	 */
	public String getState()
	{
		if (state == -1)
			return "(invalid)";
		return dist.states[state];
	}

	/**
	 * @return observed state index
	 */
	public int getStateIdx()
	{
		return state;
	}

	/**
	 * @return temp ordering index
	 */
	public int getOrderingIdx()
	{
		return ordIdx;
	}

	/**
	 * @param idx
	 */
	public void setOrderingIdx(int idx)
	{
		ordIdx = idx;
	}

	/**
	 * @return probability function
	 */
	public ProbFunction getFunction()
	{
		return func;
	}

	public Distribution getDistribution()
	{
		return this.dist;
	}

	public String getName()
	{
		return this.variable;
	}
}
