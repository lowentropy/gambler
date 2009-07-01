/*
 * ClusterClient.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.server.cluster;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;


public class ClusterClient
{

	public static void main(String[] args)
	{
		try
		{
			int curScreen = -1;
			int curApp = -1;

			String cmd, host;
			System.out.print("host: ");

			BufferedReader rdr = new BufferedReader(new InputStreamReader(
					System.in));
			host = rdr.readLine();
			String connect = "//" + host + "/poker.cluster";
			System.out.println("connecting to " + connect);
			ClusterServer server = (ClusterServer) Naming.lookup(connect);

			System.out.print("> ");
			cmd = rdr.readLine();

			while ((cmd != null) && !cmd.equals("exit"))
			{
				if (cmd.equals("start screen"))
					System.out.println(curScreen = server.openScreen());
				else if (cmd.indexOf("kill screen") == 0)
					if (cmd.indexOf("*") != -1)
						server.closeAllScreens();
					else
						server.closeScreen(Integer.parseInt(cmd.substring(12)));
				else if (cmd.indexOf("start app") == 0)
					runApp(Integer.parseInt(cmd.substring(10)), server, rdr);
				else if (cmd.indexOf("kill app") == 0)
					closeApp(cmd.substring(9), server);
				else if (cmd.equals("error"))
				{
					System.out.flush();
					server.getError().printStackTrace();
					System.out.flush();
				}
				else if (cmd.equals("config"))
					server.printConfig();
				else if (cmd.trim().length() == 0)
					;
				else
				{
					System.out.flush();
					System.err.println("Unknown command.");
					System.err.flush();
				}
				System.out.print("> ");
				cmd = rdr.readLine();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}


	private static void runApp(int port, ClusterServer server,
			BufferedReader rdr)
	{
		System.out.print("run: ");
		try
		{
			String app = rdr.readLine();
			System.out.println(server.runApp(port, app, false));
		}
		catch (IOException e)
		{
			System.out.flush();
			e.printStackTrace();
			System.err.flush();
			return;
		}
	}


	private static void closeApp(String rest, ClusterServer server)
			throws RemoteException
	{
		int idx = rest.indexOf(' ');
		int port = Integer.parseInt(rest.substring(0, idx));
		int app = Integer.parseInt(rest.substring(idx + 1));
		server.closeApp(port, app);
	}
}
