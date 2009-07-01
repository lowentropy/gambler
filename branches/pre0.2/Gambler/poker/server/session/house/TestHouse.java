/*
 * TestHouse.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.server.session.house;

import poker.server.base.Player;
import poker.server.log.LogServer;
import poker.server.log.LogSession;
import poker.server.session.PokerSession;


public class TestHouse implements House
{

	public Game startGame(String houseName, PokerSession session, Player player, LogServer ls, int log)
	{
		return null;
	}

}
