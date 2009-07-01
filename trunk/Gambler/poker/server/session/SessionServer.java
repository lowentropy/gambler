/*
 * SessionServer.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.server.session;

import java.rmi.Remote;
import java.rmi.RemoteException;

import poker.server.cluster.ClusterServer;


/**
 * The SessionServer interface is a server for a number of VNC clusters running
 * poker games. This server is able to translate the game-bases's commands into
 * pointer commands in the appropriate screen on some cluster, which the session
 * server multiplexes. It also detects screen updates from the VNC servers and
 * translates these into appropriate game events (via the graphics AI) and
 * communicates these back to the game-base server.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public interface SessionServer extends Remote
{

	/**
	 * Set the name of game schemas for each of desired number of simultaneous
	 * games. The order does not matter, only the number and ratio. A call to
	 * this method should not force tables to stop, but any new hand should be
	 * played in a table for a provider which evens the new ratio.
	 * 
	 * @param games
	 *            provider names
	 */
	public void setGameProviders(String[] games) throws RemoteException;


	/**
	 * Sets the hostname of the game player. Current connections are valid
	 * through the end of game hands.
	 * 
	 * @param host
	 *            hostname of poker player server
	 */
	public void setPlayerHost(String host) throws RemoteException;


	/**
	 * Add a host on which session can run screens.
	 * 
	 * @param host
	 *            hostname
	 * @throws RemoteException
	 */
	public void addClusterHost(String host) throws RemoteException;


	/**
	 * Remote a host on which screens had been run. This causes a soft stop on
	 * screens with active games.
	 * 
	 * @param host
	 *            hostname
	 * @throws RemoteException
	 */
	public void removeClusterHost(String host) throws RemoteException;


	/**
	 * Start the session server, if stopped.
	 */
	public void start() throws RemoteException;


	/**
	 * Soft stop: finish all active hands and do cleanup; do not close screens.
	 */
	public void stopSoft() throws RemoteException;


	/**
	 * Hard stop: close all apps and screens.
	 */
	public void stopHard() throws RemoteException;


	/**
	 * Kill the server immediately.
	 */
	public void stopPanic() throws RemoteException;


	/**
	 * @return least balanced cluster
	 * @throws RemoteException 
	 */
	public String getBestClusterHost() throws RemoteException;


	/**
	 * @param host
	 *            hostname
	 * @return cluster on host
	 */
	public ClusterServer getCluster(String host) throws RemoteException;

	
	/**
	 * @return whether server is running
	 * @throws RemoteException
	 */
	public boolean isRunning() throws RemoteException;
	
	/**
	 * Some table of the given type is now occupied.
	 * 
	 * @param type type of table (house)
	 * @param table table identifier
	 * @throws RemoteException
	 */
	public void addActiveTable(String type, String table) throws RemoteException;

	/**
	 * Find out whether the given table is active (occupied).
	 * 
	 * @param type type of table (house)
	 * @param table table identifier
	 * @return whether table is active
	 * @throws RemoteException
	 */
	public boolean isActiveTable(String type, String table) throws RemoteException;
	
	/**
	 * The given table is no longer occupied.
	 * 
	 * @param type type of table (house)
	 * @param table table identifier
	 * @throws RemoteException
	 */
	public void removeActiveTable(String type, String table) throws RemoteException;
}
