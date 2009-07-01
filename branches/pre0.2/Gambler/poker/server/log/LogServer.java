package poker.server.log;

import java.rmi.Remote;
import java.rmi.RemoteException;

import poker.common.Rect;


public interface LogServer extends Remote
{

	public void log(int id, String type, String src, String msg, Rect img) throws RemoteException;
	
	public int createSession(String name) throws RemoteException;
	
}
