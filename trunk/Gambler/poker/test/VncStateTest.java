
package poker.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import poker.server.cluster.ClusterServer;
import poker.server.session.house.ScreenEventHandler;
import poker.server.session.house.VncState;
import poker.server.session.house.impl.XmlGame;
import poker.server.session.house.impl.XmlHouse;
import poker.server.session.house.impl.XmlHouseLoader;
import poker.server.session.model.data.DataModel;
import poker.server.session.model.data.Field;
import poker.server.session.model.data.List;
import poker.server.session.model.data.ListItem;
import poker.util.ui.VncViewerFrame;
import poker.util.vnc.VncClient;


public class VncStateTest implements ScreenEventHandler
{

	private boolean				displayVnc	= false;

	private boolean				doPersist	= true;

	private static String		macroFile	= "conf/macros";

	private static final int	CLICK		= 1;

	private static final int	MACRO		= 2;

	private static final int	REFRESH		= 3;

	private static final int	POST		= 4;

	private static final int	SCREEN		= 5;

	private static final int	RUN			= 6;

	private static final int	DRAW		= 7;

	private static final int	SCAN		= 8;

	private static final int	TYPE		= 9;

	private static final int	SHOT		= 10;
	private static final int	KILL		= 11;

	private VncState			state;

	private int					imgCount	= 1;


	public static void main(String[] args)
	{
		new VncStateTest().main2(args);
	}


	public void main2(String[] args)
	{
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		VncClient vnc;
		VncViewerFrame frame;

		XmlHouseLoader loader;
		try
		{
			loader = new XmlHouseLoader();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return;
		}

		XmlHouse house = loader.getHouse("pokerroom.com");
		XmlGame game = house.startNullGame();
		DataModel model = game.getDataModel();
		String connect = "//localhost/poker.cluster";
		ClusterServer server;
		int port;

		try
		{
			server = (ClusterServer) Naming.lookup(connect);
			server.printConfig();
			port = server.openScreen();
			System.out.printf("port: %d\n", port);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return;
		}

		try
		{
			vnc = new VncClient();
			vnc.updateMode = VncClient.MANUAL;
			vnc.connect("localhost", port, new char[] {'d', '3', 'l', 't', '4',
					'S', 'i', 'g'});
		}
		catch (IOException e)
		{
			e.printStackTrace();
			try
			{
				server.markInactive(port);
			}
			catch (RemoteException e1)
			{
				e1.printStackTrace();
			}
			return;
		}

		frame = null;
		if (displayVnc)
		{
			frame = new VncViewerFrame(vnc);
			frame.setVisible(true);
		}

		state = new VncState(house, model, vnc);
		state.setScreenEventHandler(this);

		Map<String, String> macros;
		try
		{
			macros = loadMacros();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			try
			{
				server.markInactive(port);
			}
			catch (RemoteException e1)
			{
				e1.printStackTrace();
			}
			return;
		}

		while (true)
		{
			System.out.print(">> ");

			String line;
			try
			{
				line = in.readLine();
			}
			catch (IOException e)
			{
				e.printStackTrace();
				continue;
			}

			line = line.trim();
			if (line.length() == 0)
				continue;

			boolean useMacro = false;
			String cmode = null;
			int cmd = -1;

			if (line.charAt(0) == '!')
			{
				useMacro = true;
				line = line.substring(1);
			}

			if (line.startsWith("macro"))
			{
				cmd = MACRO;
			}
			else if (line.startsWith("lc"))
			{
				cmd = CLICK;
				cmode = "lc";
			}
			else if (line.startsWith("ld"))
			{
				cmd = CLICK;
				cmode = "ld";
			}
			else if (line.startsWith("lu"))
			{
				cmd = CLICK;
				cmode = "lu";
			}
			else if (line.startsWith("rc"))
			{
				cmd = CLICK;
				cmode = "rc";
			}
			else if (line.startsWith("rd"))
			{
				cmd = CLICK;
				cmode = "rd";
			}
			else if (line.startsWith("ru"))
			{
				cmd = CLICK;
				cmode = "ru";
			}
			else if (line.startsWith("refresh"))
				cmd = REFRESH;
			else if (line.startsWith("post"))
				cmd = POST;
			else if (line.startsWith("screen"))
				cmd = SCREEN;
			else if (line.startsWith("app"))
				cmd = RUN;
			else if (line.startsWith("draw"))
				cmd = DRAW;
			else if (line.startsWith("scan"))
				cmd = SCAN;
			else if (line.startsWith("type"))
				cmd = TYPE;
			else if (line.startsWith("exit"))
				break;
			else if (line.startsWith("shot"))
				cmd = SHOT;
			else if (line.startsWith("kill"))
				cmd = KILL;

			String arg = line.substring(line.indexOf(' ') + 1);
			if (useMacro)
				arg = macros.get(arg);

			try
			{
				switch (cmd) {
				case MACRO:
					int idx = arg.indexOf(' ');
					if (idx == -1)
					{
						invalid(line);
						break;
					}
					macros.put(arg.substring(0, idx), arg.substring(idx).trim());
					break;
				case CLICK:
					state.click(cmode, arg);
					break;
				case REFRESH:
					state.refresh();
					break;
				case POST:
					state.post();
					break;
				case SCREEN:
					state.gotoScreen(arg);
					break;
				case RUN:
					server.runApp(port, arg, doPersist);
					break;
				case KILL:
					server.closeAllApps(port);
					break;
				case DRAW:
					if (displayVnc)
					{
						frame.drawSchema(state.getCurScreen());
					}
					break;
				case TYPE:
					state.type(line.substring(5));
					break;
				case SHOT:
					vnc.getFramebuffer().getRect().save(
							new File(
									"/home/lowentropy/src/screendump/screenshot-"
											+ (imgCount++) + ".img"));
					break;
				case SCAN:
					String[] strs = arg.split("[ \t]+");
					String gridName = strs[0];
					String listName = strs[1];
					String[] assoc = new String[strs.length - 2];
					for (int i = 0; i < assoc.length; i++)
						assoc[i] = strs[i + 2];
					try
					{
						state.scanGridAssoc(gridName, line.startsWith("scans"), listName, assoc);
						model.getScreenData(state.getCurScreen().getName()).getList(
								listName).print();
					}
					catch (Throwable e)
					{
						e.printStackTrace();
					}
					break;
				default:
					invalid(line);
					break;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				try
				{
					server.markInactive(port);
				}
				catch (RemoteException e1)
				{
					e1.printStackTrace();
				}
			}
		}

		try
		{
			state.stop();
			vnc.stop();
			if (displayVnc)
				frame.dispose();
			try
			{
				server.markInactive(port);
			}
			catch (RemoteException e1)
			{
				e1.printStackTrace();
			}
		}
		catch (IOException e1)
		{
			try
			{
				server.markInactive(port);
			}
			catch (RemoteException e2)
			{
				e2.printStackTrace();
			}
			e1.printStackTrace();
		}

		try
		{
			saveMacros(macros);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
			return;
		}
	}


	private static void invalid(String cmd)
	{
		System.out.printf("invalid command: %s\n", cmd);
	}


	private static void saveMacros(Map<String, String> macros)
			throws FileNotFoundException
	{
		PrintStream out = new PrintStream(new FileOutputStream(macroFile));
		for (String name : macros.keySet())
		{
			out.print(name);
			out.print(":");
			out.println(macros.get(name));
		}
		out.close();
	}


	private static Map<String, String> loadMacros() throws IOException
	{
		Map<String, String> macros = new HashMap<String, String>();
		BufferedReader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(macroFile)));
		String line;
		while ((line = in.readLine()) != null)
		{
			int idx = line.indexOf(':');
			macros.put(line.substring(0, idx), line.substring(idx + 1));
		}
		in.close();
		return macros;
	}


	/**
	 * @see poker.server.session.house.ScreenEventHandler#trigger(java.lang.String)
	 */
	public void trigger(String name)
	{
		System.out.printf("trigger: %s\n", name);
	}


	/**
	 * @see poker.server.session.house.ScreenEventHandler#initScreen(java.lang.String)
	 */
	public boolean initScreen(String name)
	{
		System.out.printf("screen init START: %s\n", name);
		boolean b = state.defInitScreen(name);
		System.out.printf("screen init  END : %s\n", name);
		return b;
	}


	/**
	 * @see poker.server.session.house.ScreenEventHandler#initWindow()
	 */
	public boolean initWindow()
	{
		System.out.println("window init START");
		boolean b = state.defInitWindow();
		System.out.println("window init  END");
		return b;
	}


	/**
	 * @see poker.server.session.house.ScreenEventHandler#messagesSent(java.lang.String[])
	 */
	public void messagesSent(String[] msgs)
	{
		for (String msg : msgs)
			System.out.printf("SENT: %s\n", msg);
	}


	/**
	 * @see poker.server.session.house.ScreenEventHandler#messagesRecieved(java.lang.String[])
	 */
	public void messagesRecieved(String[] msgs)
	{
		for (String msg : msgs)
			System.out.printf("RECV: %s\n", msg);
	}


	/**
	 * @see poker.server.session.house.ScreenEventHandler#wait(int)
	 */
	public void wait(int milli)
	{
		System.out.printf("waiting %d milliseconds\n", milli);
		state.defWait(milli);
	}


	/**
	 * @see poker.server.session.house.ScreenEventHandler#switchedMode(boolean)
	 */
	public void switchedMode(boolean auto)
	{
		System.out.printf("switch mode: auto = %s\n", auto ? "true" : "false");
	}


	/**
	 * @see poker.server.session.house.ScreenEventHandler#store(poker.server.session.model.data.Field,
	 *      java.lang.Object, java.lang.Object)
	 */
	public void store(Field target, Object src, Object value)
	{
		System.out.printf("storing to %s\n", target.getName());
		state.defStore(target, src, value);
	}


	/**
	 * @see poker.server.session.house.ScreenEventHandler#store(poker.server.session.model.data.List,
	 *      int, java.lang.Object[], java.lang.Object[])
	 */
	public void store(List target, int idx, Object[] src, Object[] value)
	{
		System.out.printf("storing to %s[%d]\n", target.getName(), idx);
		state.defStore(target, idx, src, value);
	}


	/**
	 * @see poker.server.session.house.ScreenEventHandler#scanDone(java.lang.String,
	 *      int)
	 */
	public void scanDone(String name, int num)
	{
		System.out.printf("scan of list %s done: %d rows\n", name, num);
	}


	/**
	 * @see poker.server.session.house.ScreenEventHandler#triggersCleared()
	 */
	public void triggersCleared()
	{
		System.out.printf("triggers cleared\n");
	}


	/**
	 * @see poker.server.session.house.ScreenEventHandler#fail(java.lang.String,
	 *      java.lang.Throwable)
	 */
	public void fail(String task, Throwable exc)
	{
		System.out.printf("failed at task: %s\n", task);
		if (exc != null)
			exc.printStackTrace(System.out);
	}
	
	public boolean stopAfter(ListItem item)
	{
		String type = item.getField("type").getValue();
		String fill = item.getField("fill").getValue();
		
		if (!type.equals("Play"))
			return false;
		
		if (!fill.equals("9/10") && !fill.equals("8/10"))
			return false;
		
		return true;
	}

}
