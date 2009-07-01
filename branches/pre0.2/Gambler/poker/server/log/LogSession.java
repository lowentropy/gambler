
package poker.server.log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import poker.common.Rect;


/**
 * Coordinates log messages into a session. Names don't need to be unique.
 * 
 * @author lowentropy
 */
public class LogSession
{

	/** name of session */
	private String			name;

	/** session start data */
	private Date			startDate;

	/** session end date */
	private Date			closeDate;

	/** stream to write log data to */
	private OutputStream	stream;

	/** log entries */
	private List<LogEntry>	entries;
	
	/** next entry id */
	private int nextEntryId = 1;


	/**
	 * Constructor.
	 * 
	 * @param name
	 * @param date
	 * @param stream
	 */
	public LogSession(String name, Date date, OutputStream stream)
	{
		this.name = name;
		this.startDate = date;
		this.stream = stream;
		this.entries = new ArrayList<LogEntry>();
	}


	/**
	 * Log a message.
	 * 
	 * @param type
	 *            type of message (data|dbg|err)
	 * @param src
	 *            source of message
	 * @param msg
	 *            message text
	 * @param img
	 *            optional image
	 */
	public void log(String type, String src, String msg, Rect img)
	{
		LogEntry entry = new LogEntry(nextEntryId++, type, src, msg, img);
		entries.add(entry);
		entry.writeTo(stream);
	}


	/**
	 * Close the session.
	 */
	public void close()
	{
		try
		{
			closeDate = LogServerImpl.now();
			stream.close();
			stream = null;
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
