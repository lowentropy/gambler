/*
 * Game.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.server.session.house;

import poker.server.base.Player;


public interface Game
{

	/**
	 * Choose a table to play.
	 */
	public void chooseTable() throws GameError;


	/**
	 * Play a hand at the current table.
	 */
	public void playHand() throws GameError;


	/**
	 * @return whether the player wants to leave the table
	 */
	public boolean isStale();


	/**
	 * Leave the table. Don't go to a new one, yet.
	 */
	public void leaveTable() throws GameError;


	/**
	 * A different player AI is at the table, now.
	 * 
	 * @param newPlayer
	 *            new player
	 */
	public void setPlayer(Player newPlayer);


	/**
	 * Initialize application windows.
	 */
	public void initApps() throws GameError;


	/**
	 * Clear all apps. Leave table and close windows.
	 */
	public void clearApps();


	public void sitOut();

}
