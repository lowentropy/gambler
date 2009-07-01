package poker.server.session.model;

import java.io.IOException;

/**
 * @author lowentropy
 */
public class ModelError extends Exception
{

	/** serial UID */
	private static final long serialVersionUID = 2288823851718514843L;

	/**
	 * Constructor.
	 * 
	 * @param message
	 *            error message
	 */
	public ModelError(String message)
	{
		super(message);
	}

	public ModelError(String message, IOException e)
	{
		super(message, e);
	}
}
