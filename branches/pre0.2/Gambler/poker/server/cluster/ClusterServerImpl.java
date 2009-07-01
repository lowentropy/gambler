/*
 * ClusterServerImpl.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.server.cluster;

import java.io.File;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

import poker.util.AppletGrabber;

public class ClusterServerImpl extends UnicastRemoteObject implements
		ClusterServer
{

	/** number of milliseconds to wait after executing vncserver */
	private static final int VNC_START_WAIT = 10000;
	
	/** serial version uid */
	private static final long	serialVersionUID	= -7603575674647675149L;

	/** maximum concurrent screens (10's a good global max [5910-5919]) */
	private int					maxScreens;

	/** number of running screens */
	private int					numScreens;

	/** whether each possible screen is running */
	private boolean[]			running;

	/** base port number (for SSL tunnels) */
	private int					basePort;

	/** base screen number */
	private int					baseScreen;

	/** max number of apps per screen */
	private int					maxApps;

	/** number of running apps on each screen */
	private int[]				numApps;

	/** whether each possible app is running */
	private boolean[][]			appRunning;

	/** whether to persist apps across screen uses */
	private boolean[][]			persistApp;

	/** what possible process each app is running */
	private Process[][]			procs;

	/** whether to use TLS tunnels to secure VNC */
	private boolean				useTunnels;

	/** last error */
	private Exception			err					= null;

	/** number of active screens */
	private int					numActive;

	/** map of active screens */
	private boolean[]			active;

	/** runtime used to run programs */
	private Runtime				rt;
	
	/** directory used to run applications; by default, it is BASE/run */
	private File runDir;


	/**
	 * Default constructor.
	 * 
	 * @param maxScreens
	 *            max number of screens
	 * @throws RemoteException
	 */
	protected ClusterServerImpl(boolean useTunnels, int basePort,
			int baseScreen, int maxScreens, int maxApps) throws RemoteException
	{
		super();

		this.useTunnels = useTunnels;
		this.basePort = basePort;
		this.baseScreen = baseScreen;
		this.maxScreens = maxScreens;
		this.maxApps = maxApps;

		numScreens = 0;
		numActive = 0;
		running = new boolean[maxScreens];
		active = new boolean[maxScreens];
		numApps = new int[maxScreens];

		appRunning = new boolean[maxScreens][maxApps];
		procs = new Process[maxScreens][maxApps];
		persistApp = new boolean[maxScreens][maxApps];

		rt = Runtime.getRuntime();
		System.out.println("cluster configured: captured runtime");
		
		runDir = new File("run");
		System.out.println("cluster using run-dir: "+runDir.getAbsolutePath());
	}


	/**
	 * @see poker.server.cluster.ClusterServer#getNumScreens()
	 */
	public int getNumScreens() throws RemoteException
	{
		err = null;
		return numScreens;
	}


	/**
	 * @see poker.server.cluster.ClusterServer#getMaxScreens()
	 */
	public int getMaxScreens() throws RemoteException
	{
		err = null;
		return maxScreens;
	}


	/**
	 * @see poker.server.cluster.ClusterServer#closeAllScreens()
	 */
	public void closeAllScreens() throws RemoteException
	{
		for (int i = 0; i < maxScreens; i++)
		{
			if (!running[i])
				continue;
			closeVnc(i);
			running[i] = false;
		}
		numScreens = 0;
		numActive = 0;
		err = null;
	}


	/**
	 * @see poker.server.cluster.ClusterServer#closeScreen(int)
	 */
	public void closeScreen(int port) throws RemoteException
	{
		int idx = port - basePort;
		if (!running[idx])
		{
			err = new Exception("no screen on port " + port);
			return;
		}

		for (int i = 0; i < maxApps; i++)
			if (appRunning[idx][i])
				closeApp(port, i);

		closeVnc(idx);
		running[idx] = false;
		numScreens--;
		numActive--;
		err = null;
	}


	/**
	 * @see poker.server.cluster.ClusterServer#openScreen()
	 */
	public int openScreen() throws RemoteException
	{
		int port = findInactive();
		if (port != -1)
			return port;

		if (numScreens == maxScreens)
		{
			err = new Exception("maximum screens (" + maxScreens + ") in use!");
			return -1;
		}

		for (int i = 0; i < maxScreens; i++)
			synchronized (running)
			{
				if (!running[i])
				{
					if (!startVnc(i))
						return -1;
					running[i] = true;
					active[i] = true;
					numScreens++;
					err = null;
					return i + basePort;
				}
			}

		err = new Exception("all screens seem to be occupied... odd");
		return -1;
	}


	/**
	 * @return an inactive screen
	 */
	private synchronized int findInactive()
	{
		for (int i = 0; i < maxScreens; i++)
			if (running[i] && !active[i])
			{
				active[i] = true;
				return i + basePort;
			}

		return -1;
	}


	/**
	 * @see poker.server.cluster.ClusterServer#runApp(int)
	 */
	public int runApp(int port, String cmd, boolean persist)
			throws RemoteException
	{
		int idx = port - basePort;
		if (!running[idx])
		{
			err = new Exception("no screen on port " + port);
			return -1;
		}

		if (numApps[idx] == maxApps)
		{
			err = new Exception("max apps (" + maxApps
					+ ") already running on port " + port);
			return -1;
		}

		String toRun = "xterm -display :" + (idx + baseScreen);
		toRun += " -geometry 0x0+1000+700 -e " + cmd;

		log("running app '" + cmd + "' on screen, port " + port);

		try
		{
			Process p = rt.exec(toRun, null, runDir);
			for (int i = 0; i < maxApps; i++)
				if (!appRunning[idx][i])
				{
					procs[idx][i] = p;
					appRunning[idx][i] = true;
					persistApp[idx][i] = persist;
					numApps[idx]++;
					err = null;
					return idx;
				}

			err = new Exception("all apps slots taken... how odd");
			return -1;
		}
		catch (IOException e)
		{
			err = e;
			return -1;
		}
	}


	private void log(String msg)
	{
		System.out.println(msg);
	}


	/**
	 * @see poker.server.cluster.ClusterServer#getRunningApps(int)
	 */
	public int[] getRunningApps(int port) throws RemoteException
	{
		int idx = port - basePort;
		if (!running[idx])
		{
			err = new Exception("no screen on port " + port);
			return null;
		}

		int[] ra = new int[numApps[idx]];
		int j = 0;

		for (int i = 0; i < maxApps; i++)
			if (appRunning[idx][i])
				ra[j++] = i;

		err = null;
		return ra;
	}


	/**
	 * @see poker.server.cluster.ClusterServer#closeApp(int, int)
	 */
	public void closeApp(int port, int pid) throws RemoteException
	{
		int idx = port - basePort;
		if (!running[idx])
		{
			err = new Exception("no screen on port " + port);
			return;
		}

		if (!appRunning[idx][pid])
		{
			err = new Exception("no app in slot " + pid);
			return;
		}

		System.out.printf("closing app on port %d, pid %d\n", port, pid);

		procs[idx][pid].destroy();
		appRunning[idx][pid] = false;
		numApps[idx]--;

		err = null;
	}


	/**
	 * @see poker.server.cluster.ClusterServer#getError()
	 */
	public Exception getError() throws RemoteException
	{
		return err;
	}


	/**
	 * @see poker.server.cluster.ClusterServer#printConfig()
	 */
	public void printConfig() throws RemoteException
	{
		log("cluster settings:");
		log("\ttunneling: " + (this.useTunnels ? "yes" : "no"));
		log("\tbase port: " + this.basePort);
		log("\tmax screens: " + this.maxScreens);
		log("\tmax apps: " + this.maxApps);
	}


	/**
	 * Start vnc server on port i+base
	 * 
	 * @param i
	 * @return whether server started ok
	 * @throws RemoteException
	 */
	private boolean startVnc(int i) throws RemoteException
	{
		if (!useTunnels)
		{
			try
			{
				String cmd = "vncserver -depth 8 -pixelformat BGR233 :" + (i + baseScreen);
				log("executing: " + cmd);
				rt.exec(cmd);
				defWait(VNC_START_WAIT);
				return true;
			}
			catch (IOException e)
			{
				err = e;
				return false;
			}
		}
		else
		{
			log("tunneling on but not implemented!");
			err = new Exception("tunneling on but not implemented!");
			// TODO: tunneling
			return false;
		}
	}


	private void defWait(long w)
	{
		long s = System.currentTimeMillis();
		do
		{
			try
			{
				Thread.sleep(w);
			}
			catch (InterruptedException e)
			{
			}
		} while ((System.currentTimeMillis() - s) < w);
	}


	/**
	 * Close VNC server on port i+base.
	 * 
	 * @param i
	 */
	private void closeVnc(int i)
	{
		if (!useTunnels)
		{
			try
			{
				rt.exec("vncserver -kill :" + (i + baseScreen));
			}
			catch (IOException e)
			{
				err = e;
			}
		}
		else
		{
			// TODO: tunneling
		}
	}


	/**
	 * Main function, starts server.
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
		if (args.length < 1)
			usage();

		try
		{
			Map<String, String> settings = parseArguments(args);

			boolean tunnel = settings.get("tunnel").equalsIgnoreCase("y");
			int baseport = Integer.parseInt(settings.get("base_port"));
			int maxscreens = Integer.parseInt(settings.get("max_screens"));
			int maxapps = Integer.parseInt(settings.get("max_apps"));
			int basescreen = Integer.parseInt(settings.get("base_screen"));

			ClusterServer server = new ClusterServerImpl(tunnel, baseport,
					basescreen, maxscreens, maxapps);
			Naming.rebind("//" + args[0] + "/poker.cluster", server);

			System.out.println("cluster server started");
		}
		catch (Exception e)
		{
			System.err.println("ComputeEngine exception: " + e.getMessage());
			e.printStackTrace();
		}
	}


	/**
	 * Print usage message and exit.
	 */
	private static void usage()
	{
		System.out.println("usage: java ClusterServerImpl HOST [--opt=val...]");
		System.exit(1);
	}


	/**
	 * Parse server options from system arguments
	 * 
	 * @param args
	 * @return
	 */
	private static Map<String, String> parseArguments(String[] args)
	{
		Map<String, String> map = new HashMap<String, String>();
		for (int i = 1; i < args.length; i++)
		{
			String arg = args[i];
			if (arg.charAt(0) == '-' && arg.charAt(1) == '-')
			{
				int idx = arg.indexOf('=');
				if (idx == -1)
				{
					System.out.println("argument '" + arg + "' ignored.");
					continue;
				}
				String name = arg.substring(2, idx);
				String val = arg.substring(idx + 1);
				map.put(name, val);
			}
			else
				System.out.println("argument '" + arg + "' ignored.");
		}
		return map;
	}


	/**
	 * @see poker.server.cluster.ClusterServer#markInactive(int)
	 */
	public void markInactive(int port) throws RemoteException
	{
		int idx = port - basePort;

		for (int i = 0; i < maxApps; i++)
			if (appRunning[idx][i] && !persistApp[idx][i])
				closeApp(port, i);

		active[idx] = false;
		numActive--;
	}


	/**
	 * @see poker.server.cluster.ClusterServer#useDirectory(int,
	 *      java.lang.String)
	 */
	public void useDirectory(int port, String dir) throws RemoteException
	{
		int idx = port - basePort;
		
		if (!active[idx])
			throw new RemoteException("port "+port+" not active");
		
		runDir = new File(dir);
	}


	/**
	 * @see poker.server.cluster.ClusterServer#grabApplet(int, java.lang.String,
	 *      java.lang.String, boolean, java.util.Map, java.lang.String[])
	 */
	public void grabApplet(int port, String site, String file, boolean real,
			Map<String, String> params, String[] actions)
			throws RemoteException
	{
		String fname = runDir.getAbsolutePath()+"/" + file;
		if (!AppletGrabber.grab(site, params, fname))
			throw new RemoteException("applet grab failed");
		if (!AppletGrabber.modify(fname, "applet", real, actions))
			throw new RemoteException("applet modify failed");
	}


	/**
	 * @see poker.server.cluster.ClusterServer#closeAllApps(int)
	 */
	public void closeAllApps(int port) throws RemoteException
	{
		int idx = port - basePort;
		for (int i = 0; i < maxApps; i++)
			if (appRunning[idx][i])
				closeApp(port, i);
	}


	/**
	 * @see poker.server.cluster.ClusterServer#getRunningLoad()
	 */
	public float getRunningLoad() throws RemoteException
	{
		return (float) numScreens / (float) maxScreens;
	}


	/**
	 * @see poker.server.cluster.ClusterServer#getActiveLoad()
	 */
	public float getActiveLoad() throws RemoteException
	{
		return (float) numActive / (float) maxScreens;
	}

}
