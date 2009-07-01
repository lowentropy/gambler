/*
 * SessionServerImpl.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.server.session;

import java.io.PrintStream;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import poker.server.base.Player;
import poker.server.cluster.ClusterServer;
import poker.server.cluster.ClusterServerImpl;

/**
 * Implements SessionServer.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class SessionServerImpl extends UnicastRemoteObject implements SessionServer
{

	private static final long	serialVersionUID	= 3257001064392964406L;

	/** whether server is running */
	private boolean running;

	/** requested games */
	private Map<String, Integer> reqGames;

	/** actual games */
	private Map<String, Integer> actGames;

	/** poker sessions active */
	private List<PokerSession> sessions;

	/** requested poker player */
	private Player reqPlayer;

	/** clusters on which we're allowed to run screens */
	private Map<String, ClusterServer> clusters;

	/** logging stream */
	private PrintStream logStream;

	/** whether server has been ordered to soft stop */
	private boolean stopping;
	
	/** map of sets of active tables for each house type */
	private Map<String,Set<String>> active;

	/**
	 * Constructor.
	 */
	public SessionServerImpl() throws RemoteException
	{
		running = false;
		reqGames = new HashMap<String, Integer>();
		actGames = new HashMap<String, Integer>();
		sessions = new LinkedList<PokerSession>();
		reqPlayer = null;
		logStream = System.out;
		clusters = new HashMap<String, ClusterServer>();
		active = new HashMap<String,Set<String>>();
	}

	/**
	 * @see poker.old.server.session.SessionServer#setGameProviders(java.lang.String[])
	 */
	public void setGameProviders(String[] games) throws RemoteException
	{
		countGamesRequest(games);
	}

	/**
	 * @see poker.old.server.session.SessionServer#addClusterHost(java.lang.String)
	 */
	public void addClusterHost(String host) throws RemoteException
	{
		if (clusters.containsKey(host))
			return;
		try
		{
			ClusterServer s = (ClusterServer) Naming.lookup("//" + host
					+ "/poker.cluster");
			clusters.put(host, s);
		} catch (Exception e)
		{
			e.printStackTrace(logStream);
		}
	}

	/**
	 * @see poker.old.server.session.SessionServer#removeClusterHost(java.lang.String)
	 */
	public void removeClusterHost(String host) throws RemoteException
	{
		clusters.remove(host);
		for (PokerSession s : sessions)
			if (s.getClusterHost().equals(host))
				s.leaveScreen();
	}

	/**
	 * @see poker.old.server.session.SessionServer#setPlayerHost(java.lang.String)
	 */
	public void setPlayerHost(String host) throws RemoteException
	{
		try
		{
			reqPlayer = (Player) Naming.lookup("//" + host + "/poker.base");
		} catch (Exception e)
		{
			e.printStackTrace(logStream);
		}

		for (PokerSession s : sessions)
			s.usePlayer(reqPlayer);
	}

	/**
	 * @see poker.old.server.session.SessionServer#start()
	 */
	public void start() throws RemoteException
	{
		if (reqPlayer == null)
			throw new RemoteException("player host not bound!");
		new Thread() {

			public void run()
			{
				try
				{
					running = true;
					mainLoop();
				}
				catch (RemoteException e)
				{
					e.printStackTrace();
				}
			}

		}.start();
	}

	/**
	 * @see poker.old.server.session.SessionServer#stopSoft()
	 */
	public void stopSoft() throws RemoteException
	{
		reqGames.clear();
		for (PokerSession s : sessions)
			s.stopSoft();
		stopping = true;
	}

	/**
	 * @see poker.old.server.session.SessionServer#stopHard()
	 */
	public void stopHard() throws RemoteException
	{
		throw new RemoteException("hard stopping not implemented.");
	}

	/**
	 * @see poker.old.server.session.SessionServer#stopPanic()
	 */
	public void stopPanic() throws RemoteException
	{
		throw new RemoteException("panicky stopping not implemented.");
	}

	/**
	 * Count the games requested and put them in string->int map.
	 * 
	 * @param games
	 *            games requested
	 */
	private void countGamesRequest(String[] games)
	{
		Map<String, Integer> map = new HashMap<String, Integer>();
		for (String s : games)
			if (!map.containsKey(s))
				map.put(s, 1);
			else
				map.put(s, map.get(s) + 1);
		for (String type : reqGames.keySet())
			if (!map.containsKey(type))
				map.put(type, 0);
		reqGames = map;
	}

	/**
	 * @return least loaded cluster, for balancing
	 * @throws RemoteException 
	 */
	private String getBestCluster() throws RemoteException
	{
		String host = null;
		float load, minLoad = 0;

		for (String h : clusters.keySet())
		{
			ClusterServer s = clusters.get(h);
			load = s.getActiveLoad();
			if ((host == null) || (load < minLoad))
			{
				minLoad = load;
				host = h;
			}
		}

		return host;
	}

	/**
	 * Main loop of server, run in separate thread.
	 * @throws RemoteException 
	 */
	private void mainLoop() throws RemoteException
	{
		while (!stopping && running)
		{
			// remove dead sessions
			for (Iterator<PokerSession> i = sessions.iterator(); i.hasNext();)
				if (i.next().isDead())
					i.remove();

			// balance actual to required games
			for (String type : reqGames.keySet())
			{
				if (!actGames.containsKey(type))
					actGames.put(type, 0);
				int req = reqGames.get(type);
				int act = actGames.get(type);
				if (req < act)
					stopGames(type, act - req);
				else if (req > act)
					startGames(type, req - act);
				actGames.put(type, req);
			}

			// yield to session threads
			Thread.yield();
		}
		
		// stop all sessions
		if (stopping)
		{
			for (PokerSession s : sessions)
				s.stopSoft();
			// wait till they all stopped
			while (!sessions.isEmpty())
			{
				for (Iterator<PokerSession> i = sessions.iterator(); i
						.hasNext();)
					if (i.next().isDead())
						i.remove();
				Thread.yield();
			}
		}
		// mark that we've finished
		stopping = false;
		running = false;
	}

	/**
	 * Start num games of this type.
	 * 
	 * @param type
	 *            type of game
	 * @param num
	 *            number to start
	 * @throws RemoteException 
	 */
	private void startGames(String type, int num) throws RemoteException
	{
		for (int i = 0; i < num; i++)
		{
			PokerSession s = makeSession(type);
			if (s != null)
				sessions.add(s);
			else
				logStream.printf("failed to create session of type: %s", type);
		}
	}

	/**
	 * Soft stop num games of given type.
	 * 
	 * @param type
	 *            type of game
	 * @param num
	 *            number to stop
	 */
	private void stopGames(String type, int num)
	{
		for (PokerSession s : sessions)
		{
			if (s.isStopping())
				continue;
			if (!s.getType().equals(type))
				continue;
			s.stopSoft();
			if (--num == 0)
				return;
		}

		logStream.printf("failed to stop %d sessions of type %s; ran out", num,
				type);
	}

	/**
	 * Create a new session of the given type.
	 * 
	 * @param type
	 *            type of game (provider)
	 * @return new session
	 * @throws RemoteException 
	 */
	private PokerSession makeSession(String type) throws RemoteException
	{
		String host = getBestCluster();
		ClusterServer cs = clusters.get(host);
		PokerSession ps = new PokerSession(this, type, reqPlayer, host, cs);
		try
		{
			ps.start();
		} catch (SessionException e)
		{
			e.printStackTrace(logStream);
			return null;
		}

		return ps;
	}

	/**
	 * @throws RemoteException 
	 * @see poker.old.server.session.SessionServer#getBestClusterHost()
	 */
	public String getBestClusterHost() throws RemoteException
	{
		return getBestCluster();
	}

	/**
	 * @see poker.old.server.session.SessionServer#getCluster(java.lang.String)
	 */
	public ClusterServer getCluster(String host)
	{
		return clusters.get(host);
	}
	
	public static void main(String[] args)
	{
		if (args.length < 1)
		{
			System.err.println("usage: java SessionServerImpl HOSTNAME");
			return;
		}
		
		SessionServer server;
		try
		{
			server = new SessionServerImpl();
		}
		catch (RemoteException e1)
		{
			e1.printStackTrace();
			return;
		}
		
		try
		{
			Naming.rebind("//" + args[0] + "/poker.session", server);
		}
		catch (Exception e)
		{
			System.err.println("session server failed to start:");
			e.printStackTrace();
			return;
		}

		System.out.println("session server started");
	}

	/**
	 * @see poker.server.session.SessionServer#isRunning()
	 */
	public boolean isRunning() throws RemoteException
	{
		return running;
	}

	/**
	 * @see poker.server.session.SessionServer#addActiveTable(java.lang.String, java.lang.String)
	 */
	public void addActiveTable(String type, String table) throws RemoteException
	{
		if (!active.containsKey(type))
			active.put(type, new HashSet<String>());
		active.get(type).add(table);
	}

	/**
	 * @see poker.server.session.SessionServer#isActiveTable(java.lang.String, java.lang.String)
	 */
	public boolean isActiveTable(String type, String table) throws RemoteException
	{
		if (!active.containsKey(type))
			return false;
		return active.get(type).contains(table);
	}

	/**
	 * @see poker.server.session.SessionServer#removeActiveTable(java.lang.String, java.lang.String)
	 */
	public void removeActiveTable(String type, String table) throws RemoteException
	{
		if (!active.containsKey(type))
			return;
		active.get(type).remove(table);
	}
}
