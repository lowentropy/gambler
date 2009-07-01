/*
 * Distribution.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package bayes;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/**
 * A value distribution which contains the array and query form of distributed
 * probability for a variable.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class Distribution implements Serializable
{
	/** randomly generated id */
	private static final long serialVersionUID = -1450573449539851424L;

	/** tolerance for normalizing floating-point error */
	public static double			tolerance	= 0.00005;

	/** name of variable this distribution represents */
	public String					variable;

	/** state names */
	public String[]					states;

	/** distribution */
	public double[]					values;

	/** to record/recall states */
	private transient Map<String, double[]>	log;

	
	/**
	 * Constructor.
	 */
	public Distribution()
	{
		log = new HashMap<String, double[]>();
	}


	/**
	 * Constructor.
	 * 
	 * @param states
	 * @param values
	 */
	public Distribution(String variable, String[] states, double[] values)
	{
		this();

		this.variable = variable;
		this.states = states;
		this.values = values;
	}


	/**
	 * Constructor.
	 * 
	 * @param states
	 * @param chosen
	 */
	public Distribution(String variable, String[] states, int chosen)
	{
		this();

		this.variable = variable;
		this.states = states;
		this.values = new double[states.length];
		for (int i = 0; i < states.length; i++)
			this.values[i] = 0.0;
		this.values[chosen] = 1.0;
	}


	/**
	 * Constructor.
	 * 
	 * @param stream
	 * @param variable
	 * @param states
	 * @throws IOException
	 */
	public Distribution(DataInputStream stream, String variable)
			throws IOException
	{
		this();

		this.variable = variable;
		this.states = new String[stream.readInt()];
		this.values = new double[states.length];
		for (int i = 0; i < states.length; i++)
			states[i] = stream.readUTF();
		for (int i = 0; i < states.length; i++)
			values[i] = stream.readDouble();
	}


	/**
	 * Create a distribution from a probability function of one variable.
	 * 
	 * @param f
	 *            function of single variable
	 * @throws BayesError
	 */
	public Distribution(ProbFunction f) throws BayesError
	{
		this();

		if (f.numVars() != 1)
			throw new BayesError("can't create dist from prob func of "
					+ f.numVars() + " vars");

		BayesNode n = f.getVar(0);
		variable = n.getVariable();
		values = f.getData().clone();
		states = n.getMarginal().states;
	}


	/**
	 * Record distribution.
	 * 
	 * @param logname
	 */
	public void record(String logname)
	{
		log.put(logname, values.clone());
	}


	/**
	 * Return copy of distribution from log.
	 * 
	 * @param logname
	 * @return
	 * @throws BayesError
	 */
	public Distribution retrieve(String logname) throws BayesError
	{
		if (logname == null)
			return this;

		if (!log.containsKey(logname))
			throw new BayesError("no log '" + logname + "' for var '"
					+ variable + "'");

		return new Distribution(variable, states, log.get(logname));
	}


	/**
	 * Recall distribution from log.
	 * 
	 * @param logname
	 * @throws BayesError
	 */
	public void recall(String logname) throws BayesError
	{
		if (logname == null)
			return;

		if (!log.containsKey(logname))
			throw new BayesError("no log '" + logname + "' for var '"
					+ variable + "'");

		values = log.get(logname);
	}


	/**
	 * @param name
	 *            new state name
	 */
	public void addState(String name)
	{
		String[] nstates = new String[states.length + 1];
		System.arraycopy(states, 0, nstates, 0, states.length);
		nstates[states.length] = name;
		states = nstates;

		double[] nvalues = new double[values.length + 1];
		System.arraycopy(values, 0, nvalues, 0, values.length);
		nvalues[values.length] = 0.0;
		values = nvalues;
	}


	/**
	 * @param state
	 * @return
	 * @throws BayesError
	 */
	public int removeState(String state) throws BayesError
	{
		int idx;

		for (idx = 0; idx < states.length; idx++)
			if (states[idx].equals(state))
				break;

		if (idx == states.length)
			throw new BayesError("cannot remove state '" + state + "' from '"
					+ variable + "': does not exist");

		if (states.length == 1)
			throw new BayesError("cannot remove last state '" + state
					+ "' from '" + variable + "'");

		String[] nstates = new String[states.length - 1];
		System.arraycopy(states, 0, nstates, 0, idx);
		System.arraycopy(states, idx + 1, nstates, idx, states.length - idx - 1);
		states = nstates;

		double[] nvalues = new double[values.length - 1];
		System.arraycopy(values, 0, nvalues, 0, idx);
		System.arraycopy(values, idx + 1, nvalues, idx, states.length - idx - 1);
		values = nvalues;

		normalize();

		return idx;
	}


	/**
	 * Set state to 1.0, all others to 0.0.
	 * 
	 * @param state
	 */
	public void choose(int state)
	{
		for (int i = 0; i < states.length; i++)
			values[i] = (i == state) ? 1.0 : 0.0;
	}


	/**
	 * Copy prior distribution to posterior.
	 * 
	 * @param func
	 */
	public void copy(ProbFunction func)
	{
		values = func.getData().clone();
	}


	/**
	 * Write distribution to output.
	 * 
	 * @param stream
	 * @throws IOException
	 */
	public void write(DataOutputStream stream) throws IOException
	{
		stream.writeInt(states.length);
		for (int i = 0; i < states.length; i++)
			stream.writeUTF(states[i]);
		for (int i = 0; i < states.length; i++)
			stream.writeDouble(values[i]);
	}


	/**
	 * @return distribution data
	 */
	public double[] getData()
	{
		return values;
	}


	/**
	 * Normalize any distribution.
	 */
	public void normalize()
	{
		try
		{
			normalize(-1.0);
		}
		catch (BayesError e)
		{
		}
	}


	/**
	 * Multiplication errors may make posterior distributions slightly
	 * denormalized. Check that this is within bounds given, and try to correct
	 * it. (Also can be called on intentionally denormalized distributions.
	 * 
	 * @throws BayesError
	 */
	public void normalize(double tol) throws BayesError
	{
		double sum = 0.0;
		for (int i = 0; i < values.length; i++)
			sum += values[i];

		double diff = Math.abs(1.0 - sum);
		if ((tol >= 0.0) && (diff > tol))
			throw new BayesError("denormalized posterior distribution of '"
					+ variable + "': " + sum);

		if (sum > 0.0)
			for (int j = 0; j < values.length; j++)
				values[j] /= sum;
	}


	/**
	 * @param dist
	 * @return
	 */
	public boolean equalsForNet(Distribution dist)
	{
		if (states.length != dist.states.length)
			return false;

		for (int i = 0; i < states.length; i++)
			if (!states[i].equals(dist.states[i]))
				return false;

		return true;
	}


	/**
	 * Print the distribution.
	 */
	public void print()
	{
		print("");
	}
	
	
	public void print(String pre)
	{
		int nl = variable.length();
		char[] spca = new char[nl + 1];
		Arrays.fill(spca, ' ');
		String spc = new String(spca, 0, nl + 1);

		for (int i = 0; i < states.length; i++)
			System.out.printf("%s%s %8.4f%% = %s\n", pre, (i == 0) ? variable + ":"
					: spc, values[i] * 100.0, states[i]);
	}
	
	
	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for (int i = 0; i < states.length; i++)
			sb.append(states[i]+"="+(values[i]*100.0)+", ");
		sb.setLength(sb.length()-2);
		sb.append("}");
		return sb.toString();
	}


	/**
	 * @return a copy of the distribution with zeroed values
	 */
	public Distribution copyAndZero()
	{
		Distribution d = new Distribution(variable, states.clone(),
				values.clone());
		Arrays.fill(d.values, 0.0);
		return d;
	}


	/**
	 * Scalar multiply a distribution by the given multiplier and sum into this
	 * distribution.
	 * 
	 * @param d
	 *            distribution to sum in
	 * @param s
	 *            scalar multiplier
	 */
	public void addInMultiplied(Distribution d, double s)
	{
		for (int i = 0; i < values.length; i++)
			values[i] += d.values[i] * s;
	}


	/**
	 * Zero the values of the distribution.
	 */
	public void zero()
	{
		Arrays.fill(values, 0.0);
	}


	public String highest()
	{
		int idx = 0;
		double max = values[0];
		for (int i = 1; i < values.length; i++)
			if (values[i] > max)
			{
				idx = i;
				max = values[i];
			}
		return states[idx];
	}


	public int biggest()
	{
		int idx = 0;
		double max = values[0];
		for (int i = 1; i < values.length; i++)
			if (values[i] > max)
			{
				idx = i;
				max = values[i];
			}
		return idx;
	}
	
	
	public void readObject(ObjectInputStream stream) throws IOException
	{
		log = new HashMap<String, double[]>();
		int size = stream.readInt();
		states = new String[size];
		values = new double[size];
		for (int i = 0; i < size; i++)
			states[i] = stream.readUTF();
		for (int i = 0; i < size; i++)
			values[i] = stream.readDouble();
	}
	
	public void writeObject(ObjectOutputStream stream) throws IOException
	{
		stream.writeInt(states.length);
		for (String s : states)
			stream.writeUTF(s);
		for (double d : values)
			stream.writeDouble(d);
	}


	public void avgMerge(double s, Distribution d)
	{
		if (values.length != d.values.length)
			return;
		
		for (int i = 0; i < values.length; i++)
			values[i] = (values[i] * s) + (d.values[i] / s);
		
		normalize();
	}


}
