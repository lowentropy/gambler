
package poker.server.log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import poker.common.Rect;
import poker.server.session.SessionServer;
import poker.server.session.SessionServerImpl;


public class LogServerImpl  extends UnicastRemoteObject implements LogServer
{

	/**
	 * calendar
	 */
	private static Calendar	cal	= Calendar.getInstance();
	
	/** whether to use stdout */
	private boolean	debug = false;
	
	private List<LogSession> sessions;

	
	/**
	 * Constructor.
	 * 
	 * @throws RemoteException
	 */
	public LogServerImpl() throws RemoteException
	{
		super();
		
		sessions = new ArrayList<LogSession>();
	}

	/**
	 * @see poker.server.log.LogServer#createSession(java.lang.String)
	 */
	public int createSession(String name) throws RemoteException
	{
		Date date = now();
		OutputStream stream;
		try
		{
			stream = getSessionStream(name, date);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			throw new RemoteException("could not get stream", e);
		}
		
		LogSession s = new LogSession(name, now(), stream);
		int id = sessions.size();
		sessions.add(s);
		return id;
	}

	
	public void log(int id, String type, String src, String msg, Rect img)
	{
		sessions.get(id).log(type, src, msg, img);
	}

	/**
	 * Create file output stream for session.
	 * 
	 * @param name
	 * @param date
	 * @return
	 * @throws IOException
	 */
	private OutputStream getSessionStream(String name, Date date)
			throws IOException
	{
		if (debug)
			return System.out;
		
		File file = new File("log/" + name + "/" + dateFmt(date) + ".log");
		file.createNewFile();
		return new FileOutputStream(file);
	}


	/**
	 * Format date as YYYY-MM-DD-hh-mm.
	 * 
	 * @param date
	 * @return
	 */
	public static String dateFmt(Date date)
	{
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.printf("%04d-%02d-%02d-%02d-%02d", date.getYear() + 1900,
				date.getMonth() + 1, date.getDate(), date.getHours(),
				date.getMinutes());
		return sw.getBuffer().toString();
	}


	/**
	 * @return current date
	 */
	public static Date now()
	{
		return Calendar.getInstance().getTime();
	}

	
	/**
	 * Set debug mode.
	 * 
	 * @param dbg
	 */
	private void setDebug(boolean dbg)
	{
		this.debug = dbg;
	}

	
	/** main server function */
	public static void main(String[] args)
	{
		if (args.length < 1)
		{
			System.err.println("usage: java LogServerImpl HOSTNAME");
			return;
		}
		
		boolean dbg = false;
		
		if (args.length > 1 && args[1].equals("debug"))
		{
			dbg = true;
		}

		try
		{
			LogServer server = new LogServerImpl();
			((LogServerImpl) server).setDebug(dbg);
			Naming.rebind("//" + args[0] + "/poker.log", server);
		}
		catch (Exception e)
		{
			System.err.println("log server failed to start:");
			e.printStackTrace();
			return;
		}

		System.out.printf("log server started %s\n", dbg ? "(DEBUG)" : "");
	}
}
