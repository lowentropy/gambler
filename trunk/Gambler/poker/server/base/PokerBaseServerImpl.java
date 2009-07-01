/*
 * PokerBaseServer.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.server.base;

import java.rmi.Naming;
import java.rmi.RemoteException;

import poker.server.base.impl.LoosePokerNetPlayer;


/**
 * The PokerBaseServer maintains a profile for different online poker providers.
 * A set of variables which are directly read from poker screens by the session
 * server tie into a calculation model to a complete set of variables, which the
 * PokerAI uses to send commands back to the session server.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class PokerBaseServerImpl
{

	public static void main(String[] args)
	{
		if (args.length < 1)
		{
			System.err.println("usage: java BaseServerImpl HOSTNAME");
			return;
		}
		
		Player server;
		try
		{
			server = new LoosePokerNetPlayer();
		}
		catch (RemoteException e1)
		{
			e1.printStackTrace();
			return;
		}
		
		try
		{
			Naming.rebind("//" + args[0] + "/poker.base", server);
		}
		catch (Exception e)
		{
			System.err.println("base server failed to start:");
			e.printStackTrace();
			return;
		}

		System.out.println("base server started");

	}
}
