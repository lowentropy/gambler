
package poker.server.session.house;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import poker.common.Coord;
import poker.common.Rect;
import poker.server.session.house.impl.XmlHouse;
import poker.server.session.model.data.DataModel;
import poker.server.session.model.data.Field;
import poker.server.session.model.data.List;
import poker.server.session.model.data.ListItem;
import poker.server.session.model.data.ScreenData;
import poker.server.session.model.visual.Component;
import poker.server.session.model.visual.Grid;
import poker.server.session.model.visual.Label;
import poker.server.session.model.visual.Region;
import poker.server.session.model.visual.Screen;
import poker.server.session.model.visual.Scrollbar;
import poker.server.session.model.visual.Verify;
import poker.server.session.model.visual.Window;
import poker.util.vnc.VncClient;


public class VncState
{

	private static final int		DEF_WINDOW_FIND_MAX_RETRIES	= 5;

	private static final int		DEF_TRANS_MAX_RETRIES		= -1;

	private int						padWait						= 200;

	private float					timeoutMultiplier			= 3.0f;

	/** VNC client adapter */
	private VncClient				vnc;

	/** screen object for active screen */
	private Screen					curScreen;

	/** current window on active screen */
	private Window					curWindow;

	/** data associated with active screen */
	private ScreenData				data;

	/** complete house schema for state */
	private XmlHouse				schema;

	/** active triggers */
	private Set<String>				triggers;

	/** map of arranged grid scans */
	private Map<String, String[]>	scansMap;

	/** map of arranged transactions */
	private Map<String, String[]>	transMap;

	/** whether to send automatic screen update requests */
	private boolean					auto;

	/** whether thread is waiting for response to auto update request */
	private boolean					autoSent;

	/** whether an update was recieved that hasn't been processed yet */
	private boolean					updateRecieved;

	/** whether an error occurred in the update send/recieve loop */
	private boolean					updateError;

	/** exception holder */
	private Throwable				exc;

	/** target for state update messages */
	private ScreenEventHandler		handler;

	/** rectangle of screen pixels */
	private Rect					rect;

	/** response time for transactions */
	private int						rtime						= 100;

	/** whether the auto update thread was started */
	private boolean					threadStarted;

	/** thread which spools vnc update requests/responses */
	private VncStateUpdateThread	thread;

	/** map of names to update rectangle specifications */
	private Map<String, Region>		updateRegions;

	/** queue of actions that have not been posted yet */
	private LinkedList<VncAction>	actions;

	/** bounds of last update message */
	private int[]					upd;

	/** timeout for transactions */
	private float					transTimeout;

	/** data model which serves screen data files */
	private DataModel				dataModel;


	/**
	 * Constructor.
	 * 
	 * @param schema
	 * @param data
	 * @param vnc
	 */
	public VncState(XmlHouse schema, DataModel dataModel, VncClient vnc)
	{
		this.auto = false;
		this.vnc = vnc;
		this.schema = schema;
		this.dataModel = dataModel;

		this.rect = vnc.getFramebuffer().getRect();
		this.upd = new int[4];

		scansMap = new HashMap<String, String[]>();
		transMap = new HashMap<String, String[]>();
		triggers = new HashSet<String>();
		updateRegions = new HashMap<String, Region>();
		actions = new LinkedList<VncAction>();

		thread = new VncStateUpdateThread();
		thread.start();
	}


	/**
	 * Get the rectangle of pixels from the vnc client.
	 */
	public void resetRect()
	{
		this.rect = this.vnc.getFramebuffer().getRect();
	}


	/**
	 * Set the current auto-updating mode
	 * 
	 * @param b
	 *            whether to auto-update
	 */
	public void setAuto(boolean b)
	{
		if (auto != b)
		{
			auto = b;
			if (handler != null)
				handler.switchedMode(b);
		}
	}


	/**
	 * Perform a screen transition.
	 */
	public boolean gotoScreen(String name)
	{
		Screen s = schema.getScreen(name);
		if (s == null)
		{
			fail("goto screen " + name + ": doesn't exist");
			return false;
		}

		// clear activity from last screen
		setAuto(false);
		// eatUpdate();
		clearTriggers();

		// get and initialize new screen
		Screen oldScreen = curScreen;
		curScreen = s;
		data = dataModel.getScreenData(name);
		data.clear();
		boolean b = initScreen(name);
		
		// if the transition failed
		if (!b)
		{
			curScreen = oldScreen;
			if (oldScreen != null)
				data = dataModel.getScreenData(oldScreen.getName());
			data.clear();
		}

		return b;
	}


	/**
	 * Initialize the given screen.
	 * 
	 * @param name
	 * @return
	 */
	private boolean initScreen(String name)
	{
		if (handler == null)
			return defInitScreen(name);
		else
			return handler.initScreen(name);
	}


	/**
	 * Default screen init; wait the load time, then initialize the window.
	 * 
	 * @param name
	 * @return
	 */
	public boolean defInitScreen(String name)
	{
		_wait(curScreen.getLoadTime());
		curWindow = curScreen.getWindow();
		return initWindow();
	}


	/**
	 * Initialize the current screen's window.
	 * 
	 * @return
	 */
	private boolean initWindow()
	{
		if (handler == null)
			return defInitWindow();
		else
			return handler.initWindow();
	}
	
	
	/**
	 * Get an update. CANNOT fail, but might not actually get an update afte 1 second. 
	 */
	private synchronized boolean getUpdate()
	{
		try
		{
			vnc.requestUpdate();
			boolean updated = false;
			
			long now = System.currentTimeMillis();
			while (!vnc.tryUpdate())
			{
				long elapsed = System.currentTimeMillis() - now;
				if (elapsed >= 1000)
					return false;
				try
				{
					Thread.sleep(10);
				}
				catch (InterruptedException e)
				{
				}
			}
			vnc.getUpdateBounds(upd);
			return true;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
	}


	/**
	 * Get a VNC update.
	 * 
	 * @return whether update was actually received
	 */
	public boolean refresh()
	{
		return getUpdate();
		// System.out.printf("DBG: refresh(): start (%s)\n", stamp());
		//
		// while (!thread.emptyBuf)
		// try
		// {
		// Thread.sleep(100);
		// System.out.printf("in refresh loop\n"); // DBG
		// }
		// catch (InterruptedException e1)
		// {
		//				
		// }
		//			
		// System.out.printf("DBG: refresh(): emptyBuf (%s)\n", stamp());
		//
		// thread.addUpdate("full");
		//
		// try
		// {
		// System.out.printf("DBG: refresh(): waiting... (%s)\n", stamp());
		// waitForUpdate((float) -1.0);
		// }
		// catch (Throwable e)
		// {
		// updateRecieved = false;
		// return false;
		// }
		//
		// return true;
	}


	/**
	 * Get a timestamp.
	 * 
	 * @return
	 */
	public String stamp()
	{
		return Long.toString(System.currentTimeMillis());
	}


	/**
	 * Default window init: finds the window and processes all its verifies.
	 * 
	 * @return
	 */
	public boolean defInitWindow()
	{
		boolean ok = true;

		if (!curWindow.getFindMode().equals("fixed"))
		{
			ok = refresh();
			while (ok && !curWindow.find(rect))
				ok = refresh();
		}

		String msg = "refresh";
		int rc = 1;
		if (ok)
		{
			Verify[] ver = getVerifies(curWindow);
			float ot = transTimeout;
			transTimeout = 5.0f;
			rc = doTrans(null, null, "val", ver, true, false, -1);
			transTimeout = ot;
			if (rc != 1)
			{
				ok = false;
				msg = "verify";
			}
		}

		if (!ok)
		{
			fail("failed to initialize window ("+msg+")");
			return false;
		}

		if (curScreen.doAutoUpdate())
			setAuto(true);

		return true;
	}


	/**
	 * Tell the handler about a failure.
	 * 
	 * @param task
	 */
	private void fail(String task)
	{
		if (handler != null)
			handler.fail(task, exc);
	}


	/**
	 * Apply the last update to all components.
	 */
	private void eatUpdate()
	{
		updateRecieved = false;
		applyUpdate();
	}


	/**
	 * Apply the last update to all components.
	 */
	private void applyUpdate()
	{
		if (curScreen != null)
		{
			curScreen.clearUpdated();
			curScreen.updateData(data, rect, upd[0], upd[1], upd[2], upd[3]);
		}
	}


	/**
	 * Apply the last update to only some components.
	 * 
	 * @param toUpdate
	 * @param force
	 */
	public void applyPartialUpdate(Object[] toUpdate, boolean force)
	{
		updateRecieved = false;
		if (curScreen != null)
		{
			curScreen.clearUpdated();
			curScreen.updatePartialData(data, rect, toUpdate, upd[0], upd[1],
					upd[2], upd[3], force);
		}
	}


	/**
	 * Get a window's verifies.
	 * 
	 * @param w
	 * @return
	 */
	private Verify[] getVerifies(Window w)
	{
		return w.getVerifies();
	}


	/**
	 * Determine if all the given verifies are valid.
	 * 
	 * @param vfy
	 * @return
	 */
	private boolean valid(Verify[] vfy)
	{
		for (Verify v : vfy)
			if (!v.isValid())
				return false;
		return true;
	}


	/**
	 * Do a transition. Has various modes.
	 * 
	 * @param clickMode
	 * @param clickTgt
	 * @param mode
	 * @param data
	 * @param updAll
	 * @param updPad
	 * @param maxTries
	 * @return
	 */
	public int doTrans(String clickMode, String clickTgt, String mode,
			Object[] data, boolean updAll, boolean updPad, int maxTries)
	{
		boolean oldAuto = auto;

		// switch to manual mode
		if (auto)
		{
			setAuto(false);
			eatUpdate();
		}

		// initialize retries
		int maxRetries = (maxTries < 1) ? 1000 : maxTries - 1;
		int retries = 0;

		// perform action
		if (clickTgt != null)
		{
			click(clickMode, clickTgt);
			try
			{
				post();
			}
			catch (IOException e)
			{
				exc = e;
				setAuto(oldAuto);
				return -1;
			}
		}

		// create update reception flags
		boolean[] mask = new boolean[data.length];
		boolean first = true;
		boolean skipUpd;
		long start = System.currentTimeMillis();

		do
		{
			// leave if there's no tries left
			if (retries++ > maxRetries)
			{
				setAuto(oldAuto);
				return 0;
			}

			// null actions skip the first update
			skipUpd = first && (clickMode == null);
			boolean skipCheck = false;
			int ortime = rtime;

			// any-mode return check
			if (!first && !updAll)
			{
				// see if any components updated
				boolean any = false;
				for (boolean b : mask)
					if (b)
					{
						any = true;
						break;
					}
				if (any)
					// optional pad
					if (updPad)
					{
						skipCheck = true;
						rtime = padWait;
					}
					else
						break;
			}
			first = false;

			// see if time's up
			long now = System.currentTimeMillis();
			if (transTimeout > 0)
			{
				if ((((float) (now - start)) / 1000.0) >= (transTimeout * timeoutMultiplier))
				{
					setAuto(oldAuto);
//					System.out.printf(
//							"DBG: doTrans failed: too long (%f > %f) (%s)\n",
//							(((float) (now - start)) / 1000.0), transTimeout
//									* timeoutMultiplier, stamp());
					rtime = ortime;
					return 0;
				}
			}

			// get an update
			if (!skipUpd)
			{
				_wait(rtime);
				if (!getUpdate())
				{
					//System.out.printf("there's no updated in doTrans()\n"); // DBG
					continue;
				}
//				thread.addUpdate("full");
//				try
//				{
//					if (!waitForUpdate(transTimeout))
//					{
//						setAuto(oldAuto);
//						System.out.printf("DBG: doTrans got no update (%s)\n",
//								stamp());
//						rtime = ortime;
//						cleanup();
//						return 0;
//					}
//				}
//				catch (Throwable exc)
//				{
//					setAuto(oldAuto);
//					System.out.printf("DBG: doTrans got error: %s (%s)\n",
//							exc.getMessage(), stamp());
//					rtime = ortime;
//					return -1;
//				}
			}

			// there might be a pad update
			if (skipCheck)
			{
				applyPartialUpdate(data, true);
				rtime = ortime;
				break;
			}

			// check if condition is satisfied
		} while (!processTransactionUpdate(mode, data, mask, skipUpd));

		// reset to old condition
		setAuto(oldAuto);
		return 1;
	}


	/**
	 * DEPRECATED.
	 */
	private void cleanup()
	{
		System.out.printf("in cleanup\n"); // DBG
		try
		{
			while (waitForUpdate(0.5f))
				updateRecieved = false;
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
	}


	/**
	 * Move the mouse.
	 * 
	 * @param tgt
	 */
	public void move(String tgt)
	{
		Coord c = getClickTgt(tgt);
		VncAction a = new VncAction(c.x, c.y);
		synchronized (actions)
		{
			actions.add(a);
		}
	}


	/**
	 * Click the mouse.
	 * 
	 * @param clickMode
	 * @param clickTgt
	 */
	public void click(String clickMode, String clickTgt)
	{
		int btn = -1;
		int m = -1;
		Coord c = getClickTgt(clickTgt);

		switch (clickMode.charAt(0)) {
		case 'l':
		case 'L':
			btn = 0;
			break;
		case 'r':
		case 'R':
			btn = 2;
			break;
		}

		switch (clickMode.charAt(1)) {
		case 'd':
		case 'D':
			m = VncAction.CLICK_MODE_DOWN;
			break;
		case 'u':
		case 'U':
			m = VncAction.CLICK_MODE_UP;
			break;
		case 'c':
		case 'C':
			m = VncAction.CLICK_MODE_CLICK;
			break;
		}

		VncAction a = new VncAction(m, btn, c.x, c.y);
		int sx = curScreen.getSafeX();
		int sy = curScreen.getSafeY();

		synchronized (actions)
		{
			if (clickMode.endsWith("d"))
				a.setDeep(true);
			actions.add(a);
			if (clickMode.endsWith("s"))
				actions.add(new VncAction(sx, sy));
		}

	}


	/**
	 * Get the coordinate of a click target.
	 * 
	 * @param clickTgt
	 * @return
	 */
	private Coord getClickTgt(String clickTgt)
	{
		int idx = clickTgt.indexOf(',');
		if (idx == -1)
			return curScreen.getButtonCoord(clickTgt);
		String a = clickTgt.substring(0, idx);
		String b = clickTgt.substring(idx + 1);
		return new Coord(Integer.parseInt(a), Integer.parseInt(b));
	}


	/**
	 * Type some text.
	 * 
	 * @param text
	 */
	public void type(String text)
	{
		VncAction a = new VncAction(text);
		synchronized (actions)
		{
			actions.add(a);
		}
	}


	/**
	 * Perform a named transaction.
	 * 
	 * @param name
	 * @return
	 * @throws Throwable
	 */
	public boolean trans(String name) throws Throwable
	{
		String[] args = transMap.get(name);
		String cm = args[0];
		String ct = args[1];
		String m = args[2];

		Object[] data = new Object[args.length - 3];

		if (m.equals("val"))
		{
			for (int i = 0; i < data.length; i++)
				data[i] = curWindow.getVerifyByIcon(args[i + 3]);
		}
		else
		{
			for (int i = 0; i < data.length; i++)
				data[i] = curScreen.getComponent(args[i + 3]);
		}

		for (int i = 0; i < data.length; i++)
			if (data[i] == null)
				throw new Exception("no such component: " + args[i + 3]);

		int rc = doTrans(cm, ct, m, data, true, false, DEF_TRANS_MAX_RETRIES);
		if (rc < 0)
			throw exc;
		else
			return (rc == 1);
	}


	/**
	 * Create named transition.
	 * 
	 * @param name
	 * @param args
	 */
	public void createTrans(String name, String... args)
	{
		transMap.put(name, args);
	}


	/**
	 * Process an automatic update.
	 */
	private void processTriggerUpdate()
	{
		eatUpdate();

		if (handler != null)
		{
			for (String name : triggers)
			{
				if (name.equals("*"))
				{
					for (Region r : curScreen.getAllRegions())
						if (r.getComponent().wasModified())
							handler.trigger(r.getComponent().getName());
					break;
				}
				if (curScreen.getComponent(name).wasModified())
					handler.trigger(name);
			}
		}
	}


	/**
	 * Process the update returning from a transition.
	 * 
	 * @param mode
	 * @param data
	 * @param mask
	 * @param skipUpd
	 * @return
	 */
	private boolean processTransactionUpdate(String mode, Object[] data,
			boolean[] mask, boolean skipUpd)
	{
		// System.out.printf("DBG: in PTU() (%s)\n", stamp());
		applyPartialUpdate(data, false);

		boolean any = data.length == 0;
		// System.out.printf("DBG: ate partial update (%s)\n", stamp());

		for (int i = 0; i < data.length; i++)
		{
			if (mask[i])
				continue;

			boolean upd = false;

			Object o = data[i];
//			System.out.printf("DBG: PTU %d: %s (%s)\n", i,
//			o.getClass().getName(), stamp());
			if (mode.equals("val"))
			{
				if (o instanceof Verify)
				{
//					System.out.printf("DBG: PTU %d: it's a val-verify (%s)\n",
//					i, stamp());
					if (!skipUpd && !((Verify) o).wasUpdated())
					{
						//System.out.println("VAL: WASN'T UPDATED"); // DBG
						continue;
					}
					if (!((Verify) o).isValid())
					{
//						System.out.printf("DBG: PTU %d: it's not valid (%s)\n",
//						i, stamp());
//					((Verify) o).printActual(); // DBG
//						System.out.println("VAL: INVALID"); // DBG
						// DBG
						continue;
					}
				}
				else
				{
					if (!skipUpd && !((Label) o).wasUpdated())
					{
						//System.out.println("LABEL: WASN'T UPDATED"); // DBG
						continue;
					}
					if (!((Label) o).isValid())
					{
//						System.out.printf("DBG: PTU %d: it's not valid (%s)\n",
//						i, stamp());
//						System.out.println("LABEL: INVALID"); // DBG
						continue;
					}
				}
			}
			else
			{
				// System.out.printf("DBG: PTU %d: it's not a val (%s)\n",
				// i, stamp());
				if (!((Component) o).wasUpdated())
				{
					// System.out.println("WASN'T UPDATED"); // DBG
					continue;
				}
			}

			// succeeded
			mask[i] = true;
			any = true;
		}

		// d System.out.printf("DBG: end PTU() (%s)\n", stamp());

		return any;
	}


	/**
	 * Post the action buffer.
	 * 
	 * @throws IOException
	 */
	public void post() throws IOException
	{
		synchronized (actions)
		{
			while (actions.size() > 0)
			{
				VncAction a = actions.removeFirst();
				a.post(vnc);
				_wait(100);
			}
		}
	}


	public void addTrigger(String name)
	{
		triggers.add(name);
	}


	public void clearTriggers()
	{
		triggers.clear();
		if (handler != null)
			handler.triggersCleared();
	}


	private void _wait(int w)
	{
		if (handler == null)
			defWait(w);
		else
			handler.wait(w);
	}


	public void defWait(long w)
	{
		// System.out.println("DBG: CMD wait "+w);

		if (w > 0)
		{
			long s = System.currentTimeMillis();
			do
			{
				try
				{
					Thread.sleep(w);
				}
				catch (InterruptedException e)
				{
				}
			} while ((System.currentTimeMillis() - s) < w);
		}
	}


	/**
	 * DEPRECATED
	 * 
	 * @param timeout
	 * @return
	 * @throws Throwable
	 */
	private boolean waitForUpdate(float timeout) throws Throwable
	{
		// System.out.printf("DBG: waiting for update (%s)\n", stamp());
		long start = System.currentTimeMillis();
		while (!updateRecieved)
		{
			if (updateError)
				throw exc;
			long now = System.currentTimeMillis();
			if (timeout > 0.0f)
			{
				if ((((float) (now - start)) / 1000.0) >= timeout)
				{
					System.out.printf("DBG: didn't get update (%s)\n", stamp());
					return false;
				}
				else
					System.out.printf("%d\n", now - start); // DBG
			}
			else
				System.out.printf("no timeout\n"); // DBG
			Thread.sleep(100);
		}
		System.out.printf("DBG: got update (%s)\n", stamp());
		return true;
	}


	/**
	 * Store the value of the given field to the given target. The default
	 * behavior actually makes the storage.
	 * 
	 * @param target
	 *            target field
	 * @param src
	 *            source object (either label or validator)
	 * @param value
	 *            value of source
	 */
	public void store(Field target, Object src, Object value)
	{
		if (handler == null)
			defStore(target, src, value);
		else
			handler.store(target, src, value);
	}


	/**
	 * Store the value of the given field to the given target. The default
	 * behavior actually makes the storage.
	 * 
	 * @param target
	 *            target field
	 * @param src
	 *            source object (currently must be a label)
	 * @param value
	 *            value of source
	 */
	public void defStore(Field target, Object src, Object value)
	{
		target.match((String[]) value);
	}


	/**
	 * Store the given set of values (updated for the given set of sources) into
	 * the given list at the given index. The default behavior uses the target
	 * names from the source objects to select fields within the list item.
	 * 
	 * @param target
	 *            target list
	 * @param idx
	 *            index of list to store in
	 * @param src
	 *            source objects
	 * @param value
	 *            values
	 */
	public void defStore(List target, int idx, Object[] src, Object[] value)
	{
		ListItem item = target.getItem(idx);
		for (int i = 0; i < src.length; i++)
		{
			Label l = (Label) src[i];
			item.getField(l.getTargetName()).match((String[]) value[i]);
		}
	}


	/**
	 * Store the given set of values (updated for the given set of sources) into
	 * the given list at the given index. The default behavior uses the target
	 * names from the source objects to select fields within the list item.
	 * 
	 * @param target
	 *            target list
	 * @param idx
	 *            index of list to store in
	 * @param src
	 *            source objects
	 * @param value
	 *            values
	 */
	public void store(List target, int idx, Object[] src, Object[] value)
	{
		if (handler == null)
			defStore(target, idx, src, value);
		else
			handler.store(target, idx, src, value);
	}


	private int	logNum;


	private void resetVisLog()
	{
		logNum = 1;
	}


	private void addVisLog(String msg)
	{
		/*
		 * String fname = ""; try { fname =
		 * "/home/lowentropy/src/screendump/log-" + (logNum++) + ".img";
		 * this.rect.save(new File(fname)); System.out.println("saved log -> " +
		 * fname + ": " + msg); } catch (IOException e) {
		 * System.out.println("failed to save log -> " + fname + ": " + msg); }
		 */}


	public boolean scanGridAssoc(String gridName, boolean doScroll,
			String listName, String... assocNames) throws Throwable
	{
		// reset visual log, get initial log
		resetVisLog();
		addVisLog("before scanGrid()");

		// set up wait times
		int gridClickWait = 500;

		// get grid and scroll to top
		Grid g = curScreen.getGrid(gridName);
		if (doScroll)
			scroll_to_top(g);

		// get list and assoc data
		List l = data.getList(listName);
		Component[] data = new Component[assocNames.length];
		for (int i = 0; i < assocNames.length; i++)
			data[i] = curScreen.getComponent(assocNames[i]);

		// loop variables
		int i;
		Coord c = null;

		// timeouts for transactions
		float to = getTransactionTimeout();
		setTransactionTimeout((float) 2.0); // XXX: unknown value

		// get root coordinate
		int ax = curScreen.getActualAnchorX();
		int ay = curScreen.getActualAnchorY();

		boolean doStop = false;
		boolean condStop = false;

		for (i = 0; i < g.numRows();)
		{
			// get row select data
			c = g.getRowSelectCoord(i);
			c.x += ax;
			c.y += ay;
			int lr = g.getSelectedRow();

			// select row, wait for assoc data to update
			if (i != lr)
			{
				g.setSelectedRow(i);
				//System.out.printf("calling doTrans for row %d\n", i);
				int rc = doTrans("lcs", c.x + "," + c.y, "upd", data, false,
						true, -1);
				//System.out.printf("doTrans returned %d\n", rc);
				if (rc < 0)
				{
					// System.out.println("DBG: doTrans failed in SCAN");
					throw exc;
				}
				else if (rc == 0)
					break;
				_wait(gridClickWait);
			}
			else
			{
				applyPartialUpdate(data, true);
			}

			// vis log
			addVisLog("after grid row click");

			// apply updates to grid row
			Object[] rowFields = g.getRowComponents(i);
			applyPartialUpdate(rowFields, true);

			// store grid row and assoc to list row
			g.storeRow(i, l, i);
			ListItem item = l.getItem(i);
			for (Component d : data)
				item.getField(d.getTarget()).setFrom(
						(Field) this.data.getTarget(d.getTarget()));

			// if the last list item was blank, that's the whole list
			if (item.isBlank())
			{
				doStop = true;
				break;
			}
			else if (handler.stopAfter(item))
			{
				doStop = condStop = true;
				i++;
			//	System.out.printf("DBG: stopping for condition\n");
				break;
			}

			i++;
		}

		// if at bottom of grid, scroll down for more
		if (i == g.numRows() && doScroll && !doStop)
		{
			// scroll down one row, till the end
			while (scroll_dn(g))
			{
			//	addVisLog("after scrolling down (outside)");

				// click the bottom row, wait a bit
				int rc = doTrans("lcs", c.x + "," + c.y, "upd", data, false,
						true, -1);
				if (rc < 0)
					throw exc;
				else if (rc == 0)
					break;
				_wait(gridClickWait);

				addVisLog("after bottom row grid click");

				// store grid row and assoc to list row
				g.storeRow(g.numRows() - 1, l, i);
				ListItem item = l.getItem(i);
				for (Component d : data)
					item.getField(d.getTarget()).setFrom(
							(Field) this.data.getTarget(d.getTarget()));

				// if scrolled past end of list, stop
				if (item.isBlank())
					break;

				i++;
			}
		}

	//	System.out.printf("after for loop\n"); // DBG

		// set the list size (might not include empty row)
		l.setNumRows(i);

		// clean up
		setTransactionTimeout(to);

		// inform handler of scan completion
		if (handler != null)
			handler.scanDone(l.getName(), l.getNumRows());

	//	System.out.printf("returning from scan\n");

		return condStop;
	}


	public void scroll_to_top(Grid g) throws Throwable
	{
		resetVisLog();

		int clickPad = 2;
		Scrollbar s = g.getScrollbar();
		Object[] S = new Object[] {s};

		if (s == null)
			return;

		int ax = curScreen.getActualAnchorX();
		int ay = curScreen.getActualAnchorY();
		int x = ax + s.getUpX() + s.getOffsetX() + g.getOffsetX();
		int y = ay + s.getUpY() + s.getOffsetY() + g.getOffsetY();

		float to = getTransactionTimeout();
		setTransactionTimeout((float) 1.0);

		int rc;

		while ((rc = doTrans("lcs", x + "," + y, "upd", S, true, false, 3)) == 1)
		{
			addVisLog("after scroll up");
			// g.setSelectedRow(g.getSelectedRow()+1);
			defWait(500);
		}

		if (rc < 0)
			throw exc;

		for (int i = 0; i < clickPad; i++)
		{
			defWait(500);
			click("lc", x + "," + y);
			post();
			addVisLog("after scroll up pad click");
		}
		defWait(500);

		setTransactionTimeout(to);
	}


	public boolean scroll_dn(Grid g) throws Throwable
	{
		Scrollbar s = g.getScrollbar();
		int ax = curScreen.getActualAnchorX();
		int ay = curScreen.getActualAnchorY();
		int x = ax + s.getDnX() + s.getOffsetX() + g.getOffsetX();
		int y = ay + s.getDnY() + s.getOffsetY() + g.getOffsetY();

		float to = getTransactionTimeout();
		setTransactionTimeout((float) 1.0);
		int rc = doTrans("lc", x + "," + y, "upd", new Object[] {s}, true,
				false, 3);
		defWait(500);

		g.setSelectedRow(g.getSelectedRow() - 1);
		addVisLog("after scroll down (inside)");

		setTransactionTimeout(to);

		return rc == 1;
	}


	public float getTransactionTimeout()
	{
		return transTimeout;
	}


	public void setTransactionTimeout(float to)
	{
		transTimeout = to;
	}


	public void setRtime(int milli)
	{
		rtime = milli;
	}


	public class VncStateUpdateThread extends Thread
	{

		public boolean				quit		= false;

		public boolean				emptyBuf	= false;

		// public LinkedList<String>	updates;


		public VncStateUpdateThread()
		{
			super();
			// updates = new LinkedList<String>();
		}


		public void run()
		{
			while (!quit)
			{
				if (!auto)
					yield();
				else
				{
					if (getUpdate())
						processTriggerUpdate();
					yield();
				}
//				System.out.printf("thread loop\n"); // DBG
//				String toProc = null;
//				synchronized (updates)
//				{
//					if (updates.size() > 0)
//						toProc = updates.removeFirst();
//				}
//				if (toProc == null)
//				{
//					if (auto)
//						addUpdate("auto");
//					else
//					{
//						if (quit)
//							break;
//						emptyBuf = true;
//						try
//						{
//							Thread.sleep(50);
//						}
//						catch (InterruptedException e)
//						{
//						}
//					}
//				}
//				else
//				{
//					System.out.printf("proc\n");
//
//					try
//					{
//						if (toProc.equals("auto"))
//							vnc.requestUpdate();
//						else
//						{
//							if (toProc.equals("full"))
//								vnc.requestUpdate();
//							else
//							{
//								Region r = updateRegions.get(toProc);
//								vnc.requestUpdate(r.getX(), r.getY(),
//										r.getWidth(), r.getHeight());
//							}
//						}
//						vnc.recieveUpdate();
//						vnc.getUpdateBounds(upd);
//						updateRecieved = true;
//
//						if (toProc.equals("auto") && auto)
//							processTriggerUpdate();
//
//						try
//						{
//							Thread.sleep(50);
//						}
//						catch (InterruptedException e)
//						{
//						}
//					}
//					catch (IOException e)
//					{
//						exc = e;
//						updateError = true;
//					}
//				}
//				emptyBuf = false;
			}
		}


		public void addUpdate(String type)
		{
//			synchronized (updates)
//			{
//				updates.add(type);
//			}
		}

	}


	public void stop()
	{
		this.thread.quit = true;
	}


	public void setScreenEventHandler(ScreenEventHandler handler)
	{
		this.handler = handler;
	}


	public Screen getCurScreen()
	{
		return curScreen;
	}


	public Rect getRect(int x, int y, int width, int height)
	{
		return rect.sub(x, y, width, height, false);
	}


	public void setData(DataModel data2)
	{
		this.dataModel = data2;
	}


	public Rect getRect()
	{
		return rect;
	}


	public boolean inAuto()
	{
		return auto;
	}

}
