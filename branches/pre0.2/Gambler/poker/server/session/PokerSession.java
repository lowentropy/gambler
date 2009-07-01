/*
 * PokerSession.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.server.session;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Map;

import poker.server.base.Player;
import poker.server.cluster.ClusterServer;
import poker.server.log.LogServer;
import poker.server.log.LogSession;
import poker.server.session.house.Game;
import poker.server.session.house.GameError;
import poker.server.session.house.House;
import poker.server.session.house.HouseLoader;
import poker.util.vnc.Framebuffer;
import poker.util.vnc.VncClient;

/**
 * Stores information about a single poker session, such as which VNC cluster
 * the game is being run on and which screen name it is using. This class also
 * contains the VNC client and associated framebuffer for that game.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class PokerSession
{

	private boolean			recover		= false;

	/** password to vnc servers */
	private static String	vncPass;

	/** server hosting this session */
	private SessionServer	server;

	/** type of session */
	private String			type;

	/** actual poker player */
	private Player			player;

	/** requested poker player */
	private Player			reqPlayer;

	/** hostname of cluster we're using */
	private String			clusterHost;

	/** cluster server we're using */
	private ClusterServer	cluster;

	/** port on cluster of active screen */
	private int				screenPort;

	/** if true, ask the server for a new cluster host after this hand */
	private boolean			switchScreens;

	/** whether session will stop after current hand */
	private boolean			stopping;

	/** whether session has been stopped */
	private boolean			dead;

	/** 'house rules' this session will use */
	private House			house;

	/** active poker game */
	private Game			game;

	/** VNC adapter attached to screen */
	private VncClient		vnc;

	private LogServer		ls			= null;

	private boolean			firstScreen	= true;

	private int				log;


	/**
	 * Constructor.
	 * 
	 * @param server
	 * @param type
	 * @param reqPlayer
	 * @param clusterHost
	 * @param cluster
	 */
	public PokerSession(SessionServer server, String type, Player reqPlayer,
			String clusterHost, ClusterServer cluster)
	{
		this.server = server;
		this.type = type;
		this.reqPlayer = reqPlayer;
		this.clusterHost = clusterHost;
		this.cluster = cluster;
		this.switchScreens = true;
		this.stopping = false;
		this.dead = false;
		this.vnc = new VncClient();
		this.vnc.updateMode = VncClient.MANUAL;
	}


	/**
	 * After this hand, switch to another player.
	 * 
	 * @param newPlayer
	 *            new player
	 */
	public void usePlayer(Player newPlayer)
	{
		reqPlayer = newPlayer;
	}


	/**
	 * @return current cluster host
	 */
	public String getClusterHost()
	{
		return clusterHost;
	}


	/**
	 * After the hand is done, ask the session server for a new screen to run
	 * on.
	 */
	public void leaveScreen()
	{
		switchScreens = true;
		game.sitOut();
	}


	/**
	 * Stop and leave the screen and table after this hand.
	 */
	public void stopSoft()
	{
		stopping = true;
		game.sitOut();
	}


	/**
	 * @return whether this is a dead session
	 */
	public boolean isDead()
	{
		return dead;
	}


	/**
	 * @return whether session has been ordered to soft stop
	 */
	public boolean isStopping()
	{
		return stopping;
	}


	/**
	 * @return type of game session is running
	 */
	public String getType()
	{
		return type;
	}


	/**
	 * Start the session. Gets a screen and starts the house script.
	 */
	public void start() throws SessionException
	{
		if ((house = HouseLoader.getHouse(type)) == null)
			throw new SessionException("nonpresent session type: " + type);

		log = getLogSession();
		game = house.startGame(type, this, reqPlayer, ls, log);

		new Thread()
		{

			public void run()
			{
				mainLoop();
				dead = true;
			}
		}.start();
	}


	/**
	 * Create a log session.
	 * 
	 * @return log session
	 */
	private int getLogSession()
	{
		try
		{
			if (ls == null)
				ls = (LogServer) Naming.lookup("//localhost/poker.log");
			// TODO: HACK ALERT: get log server from somewhere else
			return ls.createSession(this.type);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return -1;
		}
	}


	/**
	 * The main loop of a sesssion: repeatedly asks the game to join a table,
	 * play until that table is stale, and find another table. The game should
	 * coordinate with the House to determine which game to choose.
	 */
	private void mainLoop()
	{
		boolean died;

		do
		{
			died = false;
			getScreen();

			try
			{
				game.initApps();
			}
			catch (GameError e)
			{
				fail("initializing apps", e);
				if (recover)
					continue;
				return;
			}

			try
			{
				game.chooseTable();
			}
			catch (GameError e)
			{
				game.clearApps();
				fail("choosing table", e);
				if (recover)
					continue;
				return;
			}

			while (!stopping)
			{
				if (switchScreens)
					break;
				getPlayer();

				boolean failed = false;

				try
				{
					game.playHand();
				}
				catch (GameError e)
				{
					e.printStackTrace();
					failed = true;
				}

				if (failed || game.isStale())
				{
					try
					{
						game.leaveTable();
					}
					catch (GameError e)
					{
						game.clearApps();
						fail("leaving table after hand", e);
						if (recover)
						{
							died = true;
							break;
						}
						return;
					}

					try
					{
						game.chooseTable();
					}
					catch (GameError e)
					{
						game.clearApps();
						fail("choosing new table", e);
						if (recover)
						{
							died = true;
							break;
						}
						return;
					}
				}
			}

			if (died)
				continue;

			try
			{
				game.leaveTable();
			}
			catch (GameError e)
			{
				game.clearApps();
				fail("leaving final table", e);
				if (recover)
					continue;
				return;
			}

			game.clearApps();
			closeScreen();
		} while (!stopping);
	}


	private void fail(String string, GameError e)
	{
		System.err.printf("Session error while %s:\n", string);
		e.printStackTrace();

		// TODO: set failure status

		closeScreen();
	}


	/**
	 * Close the vnc client and screen.
	 */
	private void closeScreen()
	{
		try
		{
			try
			{
				vnc.stop();
			}
			catch (IOException e)
			{
				// TODO: log
				e.printStackTrace();
			}
			cluster.markInactive(screenPort);
		}
		catch (RemoteException e)
		{
			// TODO: log
			e.printStackTrace();
		}
	}


	/**
	 * Set the requested player on the game.
	 */
	private void getPlayer()
	{
		game.setPlayer(reqPlayer);
	}


	/**
	 * Get the screen information and initialize the VNC client.
	 */
	private void getScreen()
	{
		if (switchScreens)
		{
			switchScreens = false;

			if (!firstScreen)
			{
				try
				{
					cluster.markInactive(screenPort);
				}
				catch (RemoteException e)
				{
					try
					{
						ls.log(log, "err", "session", e.getMessage(), null);
					}
					catch (RemoteException e1)
					{
						e1.printStackTrace();
					}
				}
			}
			firstScreen = false;

			try
			{
				clusterHost = server.getBestClusterHost();
				cluster = server.getCluster(clusterHost);
				screenPort = cluster.openScreen();
			}
			catch (RemoteException e)
			{
				// TODO: log
				e.printStackTrace();
			}

			if (screenPort == -1)
				; // TODO: fail

			try
			{
				vnc.connect(clusterHost, screenPort, getPass());
			}
			catch (IOException e)
			{
				// TODO: log
				e.printStackTrace();
				// TODO: fail
			}
		}
	}


	/**
	 * @return password chars for vnc server
	 */
	private static char[] getPass()
	{
		if (vncPass == null)
		{
			// TODO: get this from a file
			vncPass = "d3lt4Sigt0";
		}
		return vncPass.toCharArray();
	}


	/**
	 * Start an app on the screen.
	 * 
	 * @param cmd
	 *            command to run
	 * @return PID
	 */
	public int startApp(String cmd)
	{
		try
		{
			return cluster.runApp(screenPort, cmd, false);
		}
		catch (RemoteException e)
		{
			// TODO: log
			e.printStackTrace();
			return -1;
		}
	}


	public Framebuffer getFramebuffer()
	{
		return vnc.getFramebuffer();
	}


	public VncClient getVncClient()
	{
		return vnc;
	}


	public void killApp(int pid)
	{
		try
		{
			cluster.closeApp(screenPort, pid);
		}
		catch (RemoteException e)
		{
			// TODO: handle
			e.printStackTrace();
		}
	}


	public void addActiveTable(String type, String table)
			throws RemoteException
	{
		server.addActiveTable(type, table);
	}


	public boolean isActiveTable(String type, String table)
			throws RemoteException
	{
		return server.isActiveTable(type, table);
	}


	public void removeActiveTable(String type, String table)
			throws RemoteException
	{
		server.removeActiveTable(type, table);
	}


	public void useDirectory(String dir) throws RemoteException
	{
		cluster.useDirectory(screenPort, dir);
	}


	public void grabApplet(String site, String file, boolean real,
			Map<String, String> params, String[] actions) throws RemoteException
	{
		cluster.grabApplet(screenPort, site, file, real, params, actions);
	}
}
