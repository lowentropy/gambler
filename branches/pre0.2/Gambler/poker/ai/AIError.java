/*
 * AIError.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.ai;

/**
 * Error occurred in AI processing.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class AIError extends Exception
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -6470415786423626654L;


	public AIError(String err)
	{
		super(err);
	}


	public AIError(String err, ArrayIndexOutOfBoundsException e)
	{
		super(err, e);
	}
}
