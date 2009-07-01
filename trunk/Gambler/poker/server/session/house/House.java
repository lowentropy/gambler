/*
 * House.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.server.session.house;

import poker.server.base.Player;
import poker.server.log.LogServer;
import poker.server.session.PokerSession;


public interface House
{

	Game startGame(String houseName, PokerSession session, Player player, LogServer ls, int log);

}
