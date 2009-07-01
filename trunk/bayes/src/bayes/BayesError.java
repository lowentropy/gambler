/*
 * BayesError.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package bayes;

/**
 * Bayesian network error.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class BayesError extends Exception
{

	/** serial version UID */
	private static final long	serialVersionUID	= -1610439102417117630L;


	/**
	 * Constructor.
	 * 
	 * @param msg
	 *            error message
	 */
	public BayesError(String msg)
	{
		super(msg);
	}
}
