/*
 * RunOnceControl.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package bayes;

/**
 * Simulation runner which just runs through a set number of iterations, once.
 * It then resets itself so it can be used over again. The number of iterations
 * can also be changed, but not while iterations are running.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class RunOnceControl implements SimControl
{

	/** number of iterations to run */
	private int		iterations;

	/** whether to log */
	private boolean	log;

	/**
	 * Current iteration, or -1 if not running.
	 */
	private int		current;


	/**
	 * Constructor.
	 * 
	 * @param iterations
	 * @param log
	 */
	public RunOnceControl(int iterations, boolean log)
	{
		this.iterations = iterations;
		this.log = log;
		this.current = -1;
	}


	/**
	 * Set max iterations.
	 * 
	 * @param iterations
	 *            number of iterations
	 * @throws BayesError
	 */
	public void setIterations(int iterations) throws BayesError
	{
		if (current != -1)
			throw new BayesError("can't reset max iterations while running");

		this.iterations = iterations;
	}


	/**
	 * @see bayes.SimControl#stop()
	 */
	public boolean stop()
	{
		return current == iterations;
	}


	/**
	 * @see bayes.SimControl#done()
	 */
	public boolean done()
	{
		return current == iterations;
	}


	/**
	 * @see bayes.SimControl#log()
	 */
	public boolean log()
	{
		return log;
	}


	/**
	 * @see bayes.SimControl#iterDone()
	 */
	public void iterDone()
	{
		current++;
	}


	/**
	 * @see bayes.SimControl#begin()
	 */
	public void begin()
	{
		current = 0;
	}


	/**
	 * @see bayes.SimControl#end()
	 */
	public void end()
	{
		current = -1;
	}

}
