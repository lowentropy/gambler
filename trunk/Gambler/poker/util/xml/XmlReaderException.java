package poker.util.xml;


/**
 * Error occurred reading xml file.
 * 
 * @author lowentropy
 */
public class XmlReaderException extends Exception
{

	/**
	 * Constructor.
	 * 
	 * @param message error message
	 */
	public XmlReaderException(String message)
	{
		super(message);
	}
	
	
	/**
	 * Constructor.
	 * 
	 * @param t cause of error
	 */
	public XmlReaderException(Throwable t)
	{
		super(t);
	}
}
