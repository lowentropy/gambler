
package poker.test;

import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;

import poker.server.cluster.ClusterServer;
import poker.server.session.SessionServer;


public class PlayMoneyTest
{
	
	private static int num = 2; 

	/**
	 * Tests the whole poker system on play money.
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
		SessionServer ss;
		ClusterServer cs;
		
		int num = Integer.parseInt(args[0]);

		try
		{
			ss = (SessionServer) Naming.lookup("//localhost/poker.session");
			cs = (ClusterServer) Naming.lookup("//localhost/poker.cluster");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return;
		}

		try
		{
			ss.setPlayerHost("localhost");
			ss.addClusterHost("localhost");
			String[] games = new String[num];
			for (int i = 0; i < num; i++)
				games[i] = "pokerroom.com";
			ss.setGameProviders(games);
			ss.start();
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
		}
	}

}
