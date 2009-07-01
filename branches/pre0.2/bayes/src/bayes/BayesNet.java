/*
 * BayesNet.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package bayes;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;


/**
 * Bayesian network, constructed as a DAG (no cycles, no undirected edges). The
 * network is capable of performing Monte Carlo Markov Blanket simulation. This
 * normally occurs when some nodes have posterior (evidence) distributions. If
 * only priors are given, simple probabilitic inference is used.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class BayesNet
{

	/** name of network */
	private String					name;

	/** file net was loaded from, or NULL */
	private File					file;

	/** set of all nodes */
	private Set<BayesNode>			nodes;

	/** nodes ordered by DAG */
	private BayesNode[]				topo;

	/** mapping from node name to object */
	private Map<String, BayesNode>	nodeMap;

	/** random number generator */
	private Random					rand;

	/** incremental data log */
	private ArrayList<double[][]>	incrLog;

	/** whether net is being read (suppresses ordering of dag on getNode() */
	private boolean					reading;


	/**
	 * Private constructor used by the static readNet function.
	 */
	private BayesNet()
	{
		nodes = new HashSet<BayesNode>();
		nodeMap = new HashMap<String, BayesNode>();
		rand = new Random(System.currentTimeMillis());
		incrLog = new ArrayList<double[][]>();
	}


	/**
	 * Constructor for new bayesian network. Contains no nodes.
	 * 
	 * @param name
	 *            name of network
	 */
	public BayesNet(String name)
	{
		this(name, null, new HashSet<BayesNode>());
	}


	/**
	 * Constructor for loaded bayesian network.
	 * 
	 * @param name
	 *            name of network
	 * @param file
	 *            load file
	 * @param nodes
	 *            nodes in net
	 */
	public BayesNet(String name, File file, Set<BayesNode> nodes)
	{
		this();

		this.name = name;
		this.file = file;
		this.nodes = nodes;
		this.topo = null;
		for (BayesNode n : nodes)
			nodeMap.put(n.getVariable(), n);
	}


	/**
	 * Get a node by its name.
	 * 
	 * @param name
	 *            name of node
	 * @return node object
	 * @throws BayesError
	 */
	public BayesNode getNode(String name) throws BayesError
	{
		if (!nodeMap.containsKey(name))
			throw new BayesError("no variable '" + name + "' in network '"
					+ name + "'");
		return nodeMap.get(name);
	}


	/**
	 * Get a node by its ordered index.
	 * 
	 * @param idx
	 *            index of node in dag
	 * @return node object
	 * @throws BayesError
	 */
	public BayesNode getNode(int idx) throws BayesError
	{
		if (!reading)
			order();
		return topo[idx];
	}


	/**
	 * Log the current posterior distributions of all nodes.
	 * 
	 * @param logname
	 *            name of log
	 */
	public void record(String logname)
	{
		for (BayesNode n : nodes)
			n.record(logname);
	}


	/**
	 * Reload the saved state of all nodes.
	 * 
	 * @param logname
	 *            name of log
	 * @throws BayesError
	 */
	public void recall(String logname) throws BayesError
	{
		for (BayesNode n : nodes)
			n.recall(logname);
	}


	/**
	 * Incrementally record the core data of the nodes, in DAG order.
	 * 
	 * @throws BayesError
	 */
	public void incRecord() throws BayesError
	{
		order();
		double[][] data = new double[topo.length][];

		for (int i = 0; i < topo.length; i++)
			data[i] = topo[i].getPosterior().clone();

		incrLog.add(data);
	}


	/**
	 * Return indexes into the DAG for the variables given.
	 * 
	 * @param vars
	 *            variables to index
	 * @return indices, in order of arguments
	 * @throws BayesError
	 */
	private int[] getTopologicalIndexes(String[] vars) throws BayesError
	{
		order();

		/* get indexes into DAG */
		int[] idxs = new int[vars.length];
		for (int i = 0; i < vars.length; i++)
		{
			BayesNode n = nodeMap.get(vars[i]);
			if (n == null)
				throw new BayesError("variable '" + vars[i] + "' not found");
			idxs[i] = n.getTopologicalIndex();
		}

		return idxs;
	}


	/**
	 * Dump incremental log data for given nodes in given order.
	 * 
	 * @param vars
	 *            variables to dump
	 * @return array indexed by [iter][var][state]
	 * @throws BayesError
	 */
	public double[][][] incDump(String... vars) throws BayesError
	{
		int[] idxs = getTopologicalIndexes(vars);

		/* now retrieve the data in a new order */
		double[][][] data = new double[incrLog.size()][vars.length][];
		for (int i = 0; i < incrLog.size(); i++)
		{
			double[][] log = incrLog.get(i);
			for (int j = 0; j < vars.length; j++)
				data[i][j] = log[idxs[j]];
		}

		return data;
	}


	/**
	 * Clear the incremental log.
	 */
	public void incClear()
	{
		incrLog.clear();
	}


	/**
	 * Clear evidence (state, not data) from all nodes.
	 */
	public void clearEvidence()
	{
		for (BayesNode n : nodes)
			n.setObserved(false);
	}


	/**
	 * For nodes with no evidence and no parents, map the nodes' computed
	 * average posterior probability distribution onto their prior probability
	 * distribution. Throw an error if there was no average distribution.
	 * 
	 * @throws BayesError
	 */
	public void mapAvgToPrior() throws BayesError
	{
		for (BayesNode n : nodes)
			if (n.isPrior())
				n.mapAvgToPrior();
	}


	/**
	 * Get average posterior distributions of variables which were weighted
	 * throughout approximation.
	 * 
	 * @param vars
	 *            variables to return
	 * @return weighted averages of posterior distributions
	 * @throws BayesError
	 */
	public double[][] getAverages(String... vars) throws BayesError
	{
		int[] idxs = getTopologicalIndexes(vars);
		double[][] data = new double[vars.length][];

		for (int i = 0; i < vars.length; i++)
			data[i] = topo[idxs[i]].getAverage();

		return data;
	}


	/**
	 * Add a node to the network. Duplicate names not allowed.
	 * 
	 * @param node
	 *            new node object
	 * @throws BayesError
	 *             if a node name is duplicated
	 */
	public void addNode(BayesNode node) throws BayesError
	{
		nodes.add(node);
		topo = null;

		if (nodeMap.containsKey(node.getVariable()))
			throw new BayesError("node with name '" + node.getVariable()
					+ "' already exists");

		nodeMap.put(node.getVariable(), node);
	}


	/**
	 * Forces a re-ordering.
	 * 
	 * @throws BayesError
	 */
	public void forceOrder() throws BayesError
	{
		topo = null;
		order();
	}


	/**
	 * Performs a topological sort on the nodes of the network, and adds an
	 * ordered list of nodes to the dag array.
	 * 
	 * @throws BayesError
	 *             if a cycle is detected
	 */
	private void order() throws BayesError
	{
		if ((topo != null) && (topo.length == nodes.size()))
			return;

		int tmp; // number ordered per set loop
		int index = 0; // index into dag
		topo = new BayesNode[nodes.size()];

		for (BayesNode n : nodes)
			n.orderTopologically(-1);

		/* loop until all nodes ordered */
		while (index < topo.length)
		{
			tmp = 0;
			for (Iterator<BayesNode> i = nodes.iterator(); i.hasNext();)
			{
				/* iterate unordered nodes */
				BayesNode n = i.next();
				if (n.isOrderedTopologically())
					continue;

				/* mark node ordered iff all it's parents are ordered */
				n.orderTopologically(index);
				for (BayesNode p : n.getParents())
					if (!p.isOrderedTopologically())
					{
						n.orderTopologically(-1);
						break;
					}

				/* if now ordered, add to dag array */
				if (n.isOrderedTopologically())
				{
					topo[index++] = n;
					tmp++;
				}
			}

			/* if none were added in that pass, there's a cycle */
			if (tmp == 0)
				throw cycleException(nodes);
		}
	}


	/**
	 * Performs forward probalitic inference on nodes with whatever current
	 * evidence settings they have. Nodes with evidence are not allowed to have
	 * parents without evidence.
	 * 
	 * @throws BayesError
	 */
	public void inference() throws BayesError
	{
		order();

		/* make sure all evidence nodes are priors only */
		for (int i = 0; i < topo.length; i++)
			if (topo[i].isObserved())
				for (BayesNode p : topo[i].getParents())
					if (!p.isObserved())
						throw new BayesError("inference error: parent '"
								+ p.getVariable() + "' of evidence node '"
								+ topo[i].getVariable() + "' has no evidence");

		/* dag ordering sufficient for local calculations */
		for (int i = 0; i < topo.length; i++)
			topo[i].inference();
	}


	/**
	 * Perform Markov Blanket simulation via Logic Sampling on the network. The
	 * blanket function on the nodes uses the variable's posterior distribution
	 * as a temporary storage, which should be query-able.
	 * 
	 * @param iterations
	 * @throws BayesError
	 */
	public void markovBlanket(SimControl ctl) throws BayesError
	{
		order();

		/* initialize nodes from prior probabilities */
		for (int i = 0; i < topo.length; i++)
			topo[i].markovInitialize();

		/* perform blanket approx */
		ctl.begin();
		while (true)
		{
			while (!ctl.stop())
			{
				for (int j = 0; j < topo.length; j++)
					if (!topo[j].isObserved())
						topo[j].markovBlanket(rand);
				if (ctl.log())
					incRecord();
				ctl.iterDone();
			}
			if (ctl.done())
				break;
			Thread.yield();
		}
		ctl.end();
	}


	/**
	 * Query the network. For posterior distributions, return the distribution
	 * itself.
	 * 
	 * @param results
	 *            results map where every key should be an existing variable
	 * @throws BayesError
	 */
	public void queryDist(Map<String, Distribution> results) throws BayesError
	{
		queryDist(null, results);
	}


	/**
	 * Query the network. For posterior distributions, return the distribution
	 * itself.
	 * 
	 * @param logname
	 *            name of saved network to query, or null if current
	 * @param results
	 *            results map where every key should be an existing variable
	 * @throws BayesError
	 */
	public void queryDist(String logname, Map<String, Distribution> results)
			throws BayesError
	{
		for (String name : results.keySet())
		{
			BayesNode n = nodeMap.get(name);
			if (n == null)
				throw new BayesError("variable not in network: " + name);

			results.put(name, n.getMarginal(logname));
		}
	}


	/**
	 * Query the network. Return the actual posterior distributions.
	 * 
	 * @param results
	 *            results map where every key should be an existing variable
	 * @throws BayesError
	 */
	public void distQuery(Map<String, Distribution> results) throws BayesError
	{
		for (String name : results.keySet())
		{
			BayesNode n = nodeMap.get(name);
			if (n == null)
				throw new BayesError("variable not in network: " + name);

			results.put(name, n.getMarginal());
		}
	}


	/**
	 * Query the network. For posterior distributions, return the distribution
	 * itself.
	 * 
	 * @param variables
	 *            variables to query
	 * @return distributions of variables, in order of query
	 * @throws BayesError
	 */
	public Distribution[] distQuery(String... variables) throws BayesError
	{
		Distribution[] results = new Distribution[variables.length];

		for (int i = 0; i < variables.length; i++)
		{
			BayesNode n = nodeMap.get(variables[i]);
			if (n == null)
				throw new BayesError("variable not in network: " + variables[i]);

			results[i] = n.getMarginal();
		}

		return results;
	}


	/**
	 * Create a cycle error message including the names of the nodes in the
	 * cycle(s).
	 * 
	 * @param nodes
	 *            nodes to choose from where ordered = false
	 * @return error object
	 */
	private static BayesError cycleException(Set<BayesNode> nodes)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("cycle detected: ");
		for (BayesNode n : nodes)
			if (!n.isOrderedTopologically())
				sb.append(n.getVariable() + ", ");
		sb.setLength(sb.length() - 2);
		return new BayesError(sb.toString());
	}


	/**
	 * Read a network from a file.
	 * 
	 * @param fname
	 *            name of file
	 * @return network object
	 * @throws IOException
	 * @throws BayesError
	 */
	public static BayesNet readFromFile(String fname) throws IOException,
			BayesError
	{
		File file = new File(fname);
		DataInputStream stream = new DataInputStream(new FileInputStream(file));
		BayesNet net = readNet(stream);
		net.file = file;
		return net;
	}


	/**
	 * Write a network to a file.
	 * 
	 * @param fname
	 *            name of file
	 * @throws BayesError
	 * @throws IOException
	 */
	public void writeToFile(String fname) throws BayesError, IOException
	{
		file = new File(fname);
		DataOutputStream stream;
		stream = new DataOutputStream(new FileOutputStream(file));
		write(stream);
	}


	/**
	 * Save file to where it was read from.
	 * 
	 * @return true on success, false if network was never loaded (new network)
	 * @throws IOException
	 * @throws BayesError
	 */
	public boolean save() throws IOException, BayesError
	{
		if (file == null)
			return false;

		DataOutputStream stream;
		try
		{
			stream = new DataOutputStream(new FileOutputStream(file));
		}
		catch (FileNotFoundException e)
		{
			return false;
		}

		write(stream);
		return true;
	}


	/**
	 * Read a network from the input stream.
	 * 
	 * @param stream
	 *            input stream
	 * @return network object
	 * @throws IOException
	 * @throws BayesError
	 */
	public static BayesNet readNet(DataInputStream stream) throws IOException,
			BayesError
	{
		int nnodes;
		BayesNet net = new BayesNet();
		net.reading = true;

		net.name = stream.readUTF();

		nnodes = stream.readInt();
		net.topo = new BayesNode[nnodes];
		for (int i = 0; i < nnodes; i++)
		{
			BayesNode n = BayesNode.readNode(net, stream, i);
			net.topo[i] = n;
			net.nodes.add(n);
			net.nodeMap.put(n.getVariable(), n);
		}

		net.reading = false;
		return net;
	}


	/**
	 * Write the network to the output stream.
	 * 
	 * @param stream
	 *            output stream
	 * @throws BayesError
	 * @throws IOException
	 */
	public void write(DataOutputStream stream) throws BayesError, IOException
	{
		order();

		stream.writeUTF(name);

		stream.writeInt(topo.length);
		for (int i = 0; i < topo.length; i++)
			topo[i].write(stream);
	}


	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o)
	{
		if ((o == null) || !(o instanceof BayesNet))
			return false;
		BayesNet net = (BayesNet) o;

		if (!name.equals(net.name))
			return false;

		if (nodes.size() != net.nodes.size())
			return false;

		for (String key : nodeMap.keySet())
		{
			if (!net.nodeMap.containsKey(key))
				return false;

			BayesNode n1 = nodeMap.get(key);
			BayesNode n2 = net.nodeMap.get(key);

			if (!n1.equalsForNet(n2))
				return false;
		}

		return true;
	}


	public BayesNode[] getVars()
	{
		return nodes.toArray(new BayesNode[0]);
	}


	public int numVars()
	{
		return nodes.size();
	}


	public int numObserved()
	{
		int no = 0;
		for (BayesNode n : nodes)
			if (n.isObserved())
				no++;
		return no;
	}


	/**
	 * Observe the given variable in the given state.
	 * 
	 * @param var
	 *            variable name
	 * @param state
	 *            state
	 * @throws BayesError
	 */
	public void observe(String var, String state) throws BayesError
	{
		if (!nodeMap.containsKey(var))
			throw new BayesError("no variable '" + var + "' in network '"
					+ name + "'");

		BayesNode n = nodeMap.get(var);
		n.observe(state);
	}


	public String getName()
	{
		return name;
	}
}
