/*
 * ClusterServer.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.server.cluster;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

/**
 * The ClusterServer class runs on a UNIX/Linux machine and waits for simple
 * authenticated instructions to do things like start and stop applications on
 * displays running on those servers.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public interface ClusterServer extends Remote
{

	/**
	 * @return the number of running VNC servers (screens)
	 * @throws RemoteException
	 */
	public int getNumScreens() throws RemoteException;


	/**
	 * @return maximum number of screens runnable on cluster
	 * @throws RemoteException
	 */
	public int getMaxScreens() throws RemoteException;


	/**
	 * close all running screens
	 * 
	 * @throws RemoteException
	 */
	public void closeAllScreens() throws RemoteException;


	/**
	 * close given screen
	 * 
	 * @param port
	 *            port of screen to close
	 * @throws RemoteException
	 */
	public void closeScreen(int port) throws RemoteException;


	/**
	 * Open a new screen for use, or return an inactive screen.
	 * 
	 * @return port of new screen, or -1 if full
	 * @throws RemoteException
	 */
	public int openScreen() throws RemoteException;


	/**
	 * Mark the screen on the given port as inactive (making it reclaimable).
	 * 
	 * @param port
	 *            port on which screen is running
	 * @throws RemoteException
	 */
	public void markInactive(int port) throws RemoteException;


	/**
	 * Run an application on a screen.
	 * 
	 * @param port
	 *            port of screen to run on
	 * @param cmd
	 *            command to run
	 * @param persist
	 *            whether to persist app across screen uses
	 * @return process id or -1 if failed
	 * @throws RemoteException
	 */
	public int runApp(int port, String cmd, boolean persist)
			throws RemoteException;


	/**
	 * Get PIDs of all running apps on screen.
	 * 
	 * @param port
	 *            port of screen to check
	 * @return array of PIDs
	 * @throws RemoteException
	 */
	public int[] getRunningApps(int port) throws RemoteException;


	/**
	 * Kill the application given by pid
	 * 
	 * @param pid
	 *            process id
	 * @throws RemoteException
	 */
	public void closeApp(int port, int pid) throws RemoteException;


	/**
	 * @return error which caused last return value
	 * @throws RemoteException
	 */
	public Exception getError() throws RemoteException;


	/**
	 * Print configuration.
	 * 
	 * @throws RemoteException
	 */
	public void printConfig() throws RemoteException;


	/**
	 * @return the load of running screens on the server
	 * @throws RemoteException
	 */
	public float getRunningLoad() throws RemoteException;


	/**
	 * @return the load of active screens on the server
	 * @throws RemoteException
	 */
	public float getActiveLoad() throws RemoteException;


	/**
	 * Close all apps running on this port.
	 * 
	 * @param port
	 *            port to close apps on
	 * @throws RemoteException
	 */
	public void closeAllApps(int port) throws RemoteException;


	/**
	 * Change the current working directory
	 * 
	 * @param port
	 * @param dir
	 * @throws RemoteException
	 */
	public void useDirectory(int port, String dir) throws RemoteException;


	/**
	 * Grab an applet to a file and manipulate it (special command).
	 * 
	 * @param port
	 * @param site
	 * @param file
	 * @param real
	 * @param params
	 * @param actions
	 * @throws RemoteException
	 */
	public void grabApplet(int port, String site, String file, boolean real,
			Map<String, String> params, String[] actions)
			throws RemoteException;
}
