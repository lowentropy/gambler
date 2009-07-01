
package poker.server.session.house.impl;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import poker.ai.core.Hand;
import poker.common.HandDist;
import poker.common.Money;
import poker.common.PokerError;
import poker.common.Rect;
import poker.server.base.Move;
import poker.server.base.Player;
import poker.server.log.LogServer;
import poker.server.session.PokerSession;
import poker.server.session.house.GameError;
import poker.server.session.model.data.Field;
import poker.server.session.model.data.List;
import poker.server.session.model.data.ListItem;
import poker.server.session.model.data.ScreenData;
import poker.server.session.model.data.chat.ChatValue;
import poker.server.session.model.visual.Area;
import poker.server.session.model.visual.Component;
import poker.server.session.model.visual.Icon;
import poker.server.session.model.visual.Label;
import poker.server.session.model.visual.Region;
import poker.server.session.model.visual.Screen;
import poker.server.session.model.visual.Style;
import bayes.Distribution;

/**
 * A game built for pokerroom.com.
 * 
 * @author lowentropy
 */
public class PokerroomGame extends XmlGame
{

	private double						aiLooseness		= 0.0;

	private int							posFlatten		= 0;

	private int							aiType			= Player.AI_BAYES;

	private boolean						doReal			= false;

	private static final String			viewerCommand	= "appletviewer -J-classpath -J../bin:../ext:../../bayes/bin -J-Djava.security.policy==all.policy ";

	/** appletviewer PID */
	private int							avpid;

	/** whether an unrecoverable failure has occured */
	private boolean						failed;

	/** failure message */
	private String						failMsg;

	/** failure cause */
	private Throwable					failExc;

	/** whether to leave table */
	private boolean						leaveTable;

	/** positions of chair icon validators */
	private int[][]						chairPos;

	/** positions of dealer token icon validators */
	private int[][]						tokPos;

	/** positions of the buttons (f button selected) */
	private int[][]						btnPos;

	/** components of player names at table */
	private Label[]						nameLabels;

	/** map of whether each seat is empty at the table */
	private boolean[]					emptyMap;

	/** table screen */
	private Screen						table;

	/** lobby screen */
	private Screen						lobby;

	/** position of dealer at table */
	private int							dealerPos;

	/** position chosen to sit down in */
	private int							sitinPos;

	private int							shotCount		= 1;

	private boolean						atTable			= false;

	private String						tableName;

	private boolean						newHandStarted;

	private String						handNum;

	private java.util.List<Message>		messages;

	private boolean						satIn;

	private int							tid				= -1, hid;

	private Money						uiBet;

	private Money						uiPot;

	private Money						uiRake;

	private String[]					uiNames;

	private Money[]						uiMoney;

	private Set<String>					sittingIn;

	private boolean[]					uiValid;

	private boolean						pocketSet;

	private Money						sbBet, bbBet, eBet, lBet;

	private String						llimit, hlimit;

	private int							nextPos;

	private String						nextDealer;

	private String[]					players;

	private Map<String, Integer>		posmap;

	private int[]						pos2pid;

	private Map<String, Money>			winMoney;

	private Map<String, Distribution>	showHands;

	private Set<String>					joins;

	private boolean[]					willFold;

	private Map<String, String>			nameMap;

	private int							numScanned;

	private boolean						inHand;

	private String						aiName			= "low_entropy";

	private String						aiPass			= "zipp0pAsimov";

	private int							roundsPlayed;

	private boolean						anyHandStarted;

	private Hand						pocket;

	private Move						lastMove;

	private boolean						firstAct;

	private boolean						ghost;

	private boolean						playerWaiting;

	private boolean						tableInit;

	private boolean						pocketDone;

	private boolean						handsShown;

	private boolean						prefail			= false;

	private boolean						ndWasSet;
	
	private boolean beforeFirstAction;
	
	private Set<String> becameActive;


	/**
	 * Constructor.
	 * 
	 * @param schema
	 * @param session
	 * @param player
	 * @param log
	 */
	public PokerroomGame(XmlHouse schema, PokerSession session, Player player,
			LogServer ls, int log)
	{
		super(schema, session, player, ls, log);

		emptyMap = new boolean[10];
		table = schema.getScreen("table");
		lobby = schema.getScreen("lobby");

		failed = false;
		initIconPos();
		getDataSets();

		messages = new LinkedList<Message>();
		sittingIn = new HashSet<String>();
		posmap = new HashMap<String, Integer>();
		pos2pid = new int[10];
		winMoney = new HashMap<String, Money>();
		showHands = new HashMap<String, Distribution>();
		joins = new HashSet<String>();
		nameMap = new HashMap<String, String>();
		becameActive = new HashSet<String>();
	}


	/**
	 * Get sets of components for transactions.
	 */
	private void getDataSets()
	{
		nameLabels = new Label[10];
		for (int i = 0; i < 10; i++)
			nameLabels[i] = (Label) table.getComponent("p" + i + "_name");
	}


	/**
	 * Initialize icon validator positions.
	 */
	private void initIconPos()
	{
		chairPos = new int[][] {
				{38, 233, 389, 547, 733, 736, 557, 387, 210, 44},
				{157, 114, 113, 124, 165, 415, 467, 466, 467, 410}};
		tokPos = new int[][] {
				{173, 234, 396, 550, 622, 625, 537, 390, 224, 153},
				{244, 222, 220, 218, 252, 300, 315, 318, 320, 294}};
		btnPos = new int[][] { {145, 255, 360}, {485, 488, 486}};
	}


	/**
	 * @throws GameError
	 * @see poker.server.session.house.Game#chooseTable()
	 */
	public void chooseTable() throws GameError
	{
		try
		{
			int badScans = 0;

			scrollToTop();
			if (doReal)
				scrollDown(30);
			checkFailed();

			while (true)
			{
				// first find the table
				dbgShot("finding table");
				boolean cond = false;
				try
				{
					cond = state.scanGridAssoc("tables", false, "tables",
							"hph", "pot", "ppf");
				}
				catch (Throwable e)
				{
					throw new GameError("throwable in table search", e);
				}

				checkFailed();
				if (!cond)
				{
					badScans++;
					// if (badScans >= 10)
					// throw new GameError("couldn't find a table");

					err("couldn't find a table; trying again");

					if (numScanned == 11)
					{
						scrollDown(10);
						checkFailed();
					}

					continue;
				}
				badScans = 0;

				// then click goto and go to table screen
				dbgShot("clicking goto, wait 5 secs");
				state.click("lc", "goto");
				state.post();
				wait(5000);
				checkFailed();

				// update table status
				session.addActiveTable("pokerroom.com", tableName);

				// now go to table, then wait five seconds and shot
				dbgShot("going to screen table");
				state.gotoScreen("table");
				checkFailed();

				atTable = true;

				// now scan the seats & choose one
				dbgShot("finding a seat");
				state.setTransactionTimeout(5.0f);
				if (state.doTrans(null, null, "val", nameLabels, false, false,
						-1) != 1)
					fail("validating any name at table", new Exception(""));
				checkFailed();

				// now wait 8 seconds, then refresh and find chairs/dealer
				boolean ok = false;
				for (int i = 0; i < 3; i++)
				{
					// killPurple();
					getEmptyChairMap();
					if (!getDealerPos())
					{
						err("couldn't find the dealer");
						continue;
					}
					if (!chooseSeat())
					{
						err("no available seats");
						continue;
					}
					ok = true;
					break;
				}

				if (!ok)
				{
					leaveTable();
					dbg("retrying");
					continue;
				}

				// now click seat and enter buy-in
				dbgShot("sitting down");
				if (!sitDown())
				{
					checkFailed();
					err("sitting down: no OK button (retrying)");
					leaveTable();
					continue;
				}

				break;
			}

			dbgShot("entering buyin");
			enterBuyin();
			checkFailed();

			dbgShot("entered buyin");

			dbgShot("sitting in");
			sitIn();

			dbgShot("getting initial players");
			getInitialUi();

			dbgShot("going to auto mode");
			state.addTrigger("*");
			state.setAuto(true);
		}
		catch (IOException e)
		{
			throw new GameError("VNC error", e);
		}
	}


	private void scrollToTop()
	{
		dbgShot("scrolling to top");
		try
		{
			state.scroll_to_top(lobby.getGrid("tables"));
		}
		catch (Throwable e)
		{
			fail("scrolling tables to top", e);
		}
		dbgShot("done scrolling up");
		wait(3000);
	}


	private void scrollDown(int i)
	{
		dbgShot("scrolling down " + i + " rows");
		for (int j = 0; j < i; j++)
		{
			try
			{
				state.scroll_dn(lobby.getGrid("tables"));
			}
			catch (Throwable e)
			{
				fail("scrolling tables down", e);
			}
			dbgShot("scrolled down one row");
		}
		wait(3000);
	}


	private void sitIn()
	{
		if (!satIn)
		{
			try
			{
				state.click("lcs", "sit_out");
				satIn = true;
				leaveTable = false;
				state.post();
				joins.add(aiName);
			}
			catch (IOException e)
			{
				fail("sitting in", e);
			}
			wait(1000);
		}
	}


	private void getInitialUi()
	{
		state.refresh();
		state.applyPartialUpdate(this.nameLabels, true);
		ScreenData d = data.getScreenData("table");
		// String n = d.getField("p" + i + "_name").getValue();

		uiNames = new String[10];
		uiMoney = new Money[10];
		uiValid = new boolean[10];
		sittingIn.clear();

		for (int i = 0; i < 10; i++)
		{
			String n = d.getField("p" + i + "_name").getValue();
			String m = d.getField("p" + i + "_money").getValue();

			if (n == null || n.length() < 3)
			{
				uiNames[i] = null;
				uiMoney[i] = null;
				uiValid[i] = false;
			}
			else
			{
				uiNames[i] = n;
				uiMoney[i] = decodeAmount(m);
				uiValid[i] = true;
				sittingIn.add(n);

				log("initial player pos " + i + ": " + n + " (" + m + ")");
			}
		}
	}


	/**
	 * @see poker.server.session.house.Game#playHand()
	 */
	public void playHand() throws GameError
	{
		dbgShot("in playHand()");

		if (!newHandStarted)
			log("waiting for hand to start");

		while (true)
		{
			while (!newHandStarted)
			{
				procMessages();
				checkFailed();
				if (leaveTable)
				{
					log("leaving before hand started");
					return;
				}
			}
			newHandStarted = false;
			playerWaiting = false;
			pocketDone = false;
			handsShown = false;
			beforeFirstAction = true;

			getNewDealerPosition();
			int cp = getCheapAiPid();
			dbg("ndp = " + dealerPos + ", cp = " + cp);
			checkFailed();

			if ((cp < 2) && (roundsPlayed == 0) && !((cp == 0) && ndWasSet))
			{
				log("have to wait " + (cp + 1) + " more rounds");
				continue;
			}

			inHand = true;

			log("waiting for pocket");
			while (!pocketSet)
			{
				procMessages();
				if (leaveTable)
				{
					log("leaving before pocket recieved");
					return;
				}
				else if (playerWaiting)
					break;
				checkFailed();
			}
			if (playerWaiting)
			{
				log("player not really in this round");
				playerWaiting = false;
				inHand = false;
				checkFailed();
				continue;
			}
			checkFailed();
			pocketSet = false;
			pocketDone = true;
			break;
		}
		checkFailed();

		log("started new hand: #" + handNum);

		anyHandStarted = true;
		createPosmap();
		nextPos = -1; // (getCheapAiPid() + 1) % 10;
		dbg("nextPos initialized to " + nextPos);
		firstAct = false;
		tableInit = false;
		checkFailed();

		boolean cont;
		do
		{
			cont = false;

			int pid = (tableInit ? getAiPid() : getCheapAiPid());
			dbg("waiting for AI pid " + pid + " (" + tableInit + ")");
			while (pid != getNextPid() || !roundReady())
			{
				procMessages();
				if (gameOver() && tableInit)
					break;
				pid = (tableInit ? getAiPid() : getCheapAiPid());
				checkFailed();
			}

			log("now it's the AI's turn to act");

			if (!tableInit || !gameOver())
			{
				checkFailed();

				try
				{
					smartInitTable(dealerPos, sitinPos);
					Move move = player.requestMove(tid, hid, player.getRound(
							tid, hid), getAiPid());
					move.printOdds();
					log("AI move: " + move.getPassive());
					perform(move);
					if (move.isFold())
						break;
					nextPos++;
					firstAct = true;
				}
				catch (RemoteException e)
				{
					fail("requesting move", e);
				}
				cont = true;
			}
			else
				log("AI's turn, but game is over");
			checkFailed();
		} while (cont);

		checkFailed();
		while (!gameOver())
			procMessages();

		log("ending hand");
		endHand();
		checkFailed();

		log("waiting for new hand to start");
		while (!newHandStarted)
		{
			procMessages();
			checkFailed();
		}

		if (leaveTable)
		{
			log("leaving the table");
			closeTable();
			state.clearTriggers();
			state.setAuto(false);
			checkFailed();
			log("left the table");
		}
	}


	private boolean roundReady()
	{
		try
		{
			return player.roundCardsDealt(tid, hid);
		}
		catch (RemoteException e)
		{
			err("couldn't find out if round cards dealt");
			return true;
		}
	}


	private void smartInitTable(int dpos, int fpos) throws GameError
	{
		if (tableInit)
			return;
		tableInit = true;

		int pid = getCheapPid(dpos, fpos);

		if (pid < 3)
			createGhost();
		else
			ghost = false;
		getPlayersFromUi();
		getTable();
		startNewHand();
		checkFailed();
	}


	private int getCheapPid(int d, int p)
	{
		int n = 0;
		for (int i = d; i != p; i = (i + 1) % 10)
			if ((uiValid[i] && sittingIn.contains(uiNames[i])) || (i == d))
				n++;
		return n;
	}


	private int getCheapAiPid()
	{
		return getCheapPid(dealerPos, sitinPos);
	}


	private void perform(Move move)
	{
		// fcb
		// fcr
		String btn = null;
		int i = -1;

		if (move.isFold())
		{
			i = 0;
			btn = "fold";
		}
		else if (move.isCheck() || move.isCall())
		{
			i = 1;
			btn = "check/call";
		}
		else if (move.isBet() || move.isRaise())
		{
			i = 2;
			btn = "bet/raise";
		}

		if (i == 0)
			state.move(btn);

		lastMove = move;
		wait(1500);
		// Icon icon = table.getIcon(btn_icons[i]);
		// Area a = table.getArea("a_"+btn_icons[i]);
		// int ax = table.getActualAnchorX();
		// int ay = table.getActualAnchorY();
		// Rect r = a.getRect(state.getRect(), ax, ay);
		//		
		// boolean ok = false;
		// for (int j = 0; j < 5; j++)
		// {
		// ok = true;
		// if (!icon.match(r, 0, .8f))
		// wait(200);
		// else
		// break;
		// ok = false;
		// }
		//		
		// if (!ok)
		// fail("couldn't find action button", new Exception(""));
		//		
		state.click("lcs", btn);
		try
		{
			state.post();
		}
		catch (IOException e)
		{
			fail("clicking '" + btn + "'", e);
		}
	}


	private void closeTable()
	{
		try
		{
			player.leaveTable(tid);
			tid = -1;
		}
		catch (RemoteException e)
		{
			fail("closing table", e);
		}
	}


	private void endHand()
	{
		inHand = false;
		roundsPlayed++;
		correctNames();

		int i = 0;
		int[] winp = new int[winMoney.size()];
		Money[] winm = new Money[winp.length];

		for (String n : winMoney.keySet())
		{
			winm[i] = winMoney.get(n);
			winp[i++] = getPid(n);
			if (winp[i - 1] == -1)
			{
				err("winner not found: " + n);
				i--;
			}
			else
				log("(" + winp[i - 1] + ") " + n + " wins "
						+ winm[i - 1].toString());
		}

		i = 0;
		int[] showp = new int[showHands.size()];
		Distribution[] showh = new Distribution[showp.length];

		for (String n : showHands.keySet())
		{
			showh[i] = showHands.get(n);
			showp[i++] = getPid(n);
			if (showp[i - 1] == -1)
			{
				err("shower not found: " + n);
				i--;
			}
			else
				log("(" + showp[i - 1] + ") " + n + " holds "
						+ showh[i - 1].toString());
		}

		try
		{
			player.endHand(tid, hid, winp, winm, showp, showh);
		}
		catch (RemoteException e)
		{
			fail("ending hand", e);
		}
	}


	private void correctNames()
	{
		String[] correct = new String[players.length];
		for (int i = 0; i < players.length; i++)
			correct[i] = players[i];
		for (String c : nameMap.keySet())
		{
			int pid = getPid(c);
			if (pid != -1)
				correct[pid] = nameMap.get(c);
		}
		try
		{
			player.correctNames(tid, hid, correct);
		}
		catch (RemoteException e)
		{
			fail("correcting names", e);
		}
	}


	private boolean gameOver()
	{
		return nextPos == -1;

	}


	private int getNextPid()
	{
		return nextPos;
	}


	private void getTable()
	{
		if (tid == -1)
		{
			bbBet = decodeAmount(llimit);
			eBet = new Money(bbBet);
			lBet = decodeAmount(hlimit);
			sbBet = bbBet.divideBy(2);

			log("joining table: " + aiName + ", bets are " + sbBet.toString()
					+ ", " + bbBet.toString());
			try
			{
				tid = player.joinTable(tableName, aiName, sbBet, bbBet, eBet,
						lBet, uiRake);
				player.setBias(tid, guessTableBias());
				player.setTableParam(tid, "style", aiName, getPlayerProfile());
				player.setTableParam(tid, "flat", aiName, new Integer(
						posFlatten));
			}
			catch (RemoteException e)
			{
				fail("joining table", e);
			}
		}
	}


	private Distribution guessTableBias()
	{
		return new Distribution("bias", new String[] {"T", "L"},
				doReal ? new double[] {1.0, 0.0} : new double[] {0.0, 1.0});
	}


	private Distribution getPlayerProfile()
	{
		return new Distribution("style", new String[] {"N", "D", "T", "LP",
				"LA"}, new double[] {1.0 - aiLooseness, 0.0, 0.0, 0.0,
				aiLooseness});
	}


	private void createGhost()
	{

		ghost = true;
		int gp = (dealerPos + 1) % 10;
		uiNames[gp] = "~~ghost~~";
		uiValid[gp] = true;
		sittingIn.add("~~ghost~~");
		posmap.put("~~ghost~~", gp);
		return;

	}


	private void startNewHand()
	{
		winMoney.clear();
		showHands.clear();

		try
		{
			hid = player.beginHand(tid, players, getAntes());
			player.setPocket(tid, hid, getAiPid(), pocket);
			player.setAiType(tid, hid, getAiPid(), aiType);
			joins.clear();
		}
		catch (RemoteException e)
		{
			fail("starting new hand", e);
		}
	}


	private Money[] getAntes()
	{
		Money[] antes = new Money[players.length];

		for (int i = 0; i < antes.length; i++)
			if (i == 1)
				antes[i] = new Money(sbBet);
			else if (i == 2)
				antes[i] = new Money(bbBet);
			else
				antes[i] = new Money(0, 0);
		for (String s : joins)
			if (isPlayer(s))
				for (int i = 0; i < players.length; i++)
					if (players[i].equals(s))
					{
						dbg(s + " has to post ante");
						antes[i] = new Money(eBet);
						break;
					}
		return antes;
	}


	private void getNewDealerPosition()
	{
		ndWasSet = (nextDealer != null);
		if (nextDealer == null)
		{
			dealerPos = (dealerPos + 1) % 10;
			dbg("next dealer was not set (" + dealerPos + ")");
			// while (!uiValid[dealerPos])
			// dealerPos = (dealerPos + 1) % 10;
			nextDealer = uiNames[dealerPos];
		}
		else
		{
			int odp = dealerPos;
			dealerPos = -1;

			String rdn = remap(nextDealer);
			for (int i = 0; i < 10; i++)
				if (uiNames[i] != null && uiNames[i].equals(rdn))
				{
					dealerPos = i;
					break;
				}

			dbg("next dealer was set to " + nextDealer + " (" + dealerPos + ")");

			// take a random stab at it
			if (dealerPos == -1)
				dealerPos = (odp + 1) % 10;
		}
		/*
		 * if (dealerPos == -1) { err("couldn't find dealer position");
		 * fail("getting initial players", new Exception("couldn't find dealer
		 * position")); }
		 */

		while (!uiValid[dealerPos])
		{
			dealerPos = dealerPos - 1;
			if (dealerPos < 0)
				dealerPos += 10;
		}

		dbg("final dealer is " + uiNames[dealerPos] + " (" + dealerPos + ")");

		nextDealer = null;
	}


	private void updateUiNames()
	{
		state.applyPartialUpdate(this.nameLabels, true);
		ScreenData d = data.getScreenData("table");
		// String n = d.getField("p" + i + "_name").getValue();

		for (int i = 0; i < 10; i++)
		{
			if (uiValid[i])
				continue;
			String n = d.getField("p" + i + "_name").getValue();
			if (n == null || n.length() == 0)
				continue;
			uiNames[i] = n;
			uiValid[i] = true;
		}
		for (String s : sittingIn)
			asyncRemap(s);
	}


	private void createPosmap()
	{
		posmap.clear();

		updateUiNames();
		String dn = uiNames[dealerPos];
		posmap.put(dn, dealerPos);

		for (int i = (dealerPos + 1) % 10; i != dealerPos; i = (i + 1) % 10)
			if (sittingIn.contains(uiNames[i]))
				posmap.put(uiNames[i], i);
	}


	private void getPlayersFromUi()
	{
		java.util.List<String> np = new ArrayList<String>();
		for (int i = 0; i < pos2pid.length; i++)
			pos2pid[i] = -1;
		
		for (String name : becameActive)
		{
			if (!sittingIn.contains(name))
				sittingIn.add(name);
			if (!joins.contains(name))
				joins.add(name);
		}
		becameActive.clear();

		if (!sittingIn.contains(remap(nextDealer)))
		{
			// the next dealer sat out
			while (!uiValid[dealerPos]
					|| !sittingIn.contains(uiNames[dealerPos]))
			{
				dealerPos--;
				if (dealerPos < 0)
					dealerPos += 10;
			}
		}

		pos2pid[dealerPos] = 0;
		String dn = uiNames[dealerPos];
		np.add(dn);

		Set<String> nsi = new HashSet<String>();
		for (String s : sittingIn)
			nsi.add(remap(s));
		sittingIn = nsi;

		int j = 1;
		for (int i = (dealerPos + 1) % 10; i != dealerPos; i = (i + 1) % 10)
			if (sittingIn.contains(uiNames[i]))
			{
				String n = uiNames[i];
				log("player " + n + " in table position " + i + " is player #"
						+ j);
				pos2pid[i] = j++;
				np.add(n);
			}
			else
			{
				// dbg("not including " + uiNames[i]);
			}
		players = (String[]) np.toArray(new String[0]);

		for (int i = 0; i < players.length; i++)
			willFold = new boolean[players.length];
	}


	/**
	 * Sit down at chosen seat.
	 * 
	 * @throws GameError
	 */
	private boolean sitDown() throws GameError
	{
		try
		{
			dbgShot("moving to seat");
			state.move("seat" + (sitinPos + 1));
			state.post();
			wait(1000);

			dbgShot("clicking seat");
			state.click("lcs", "seat" + (sitinPos + 1));
			state.post();

			boolean ok = false;
			for (int i = 0; i < 3; i++)
			{
				wait(3000);
				state.refresh();

				int ax = table.getActualAnchorX();
				int ay = table.getActualAnchorY();

				dbgShot("checking for OK");
				Area a = table.getArea("a_ok");
				Rect r = a.getRect(state.getRect(), ax, ay);
				Style s = table.getStyle("buttons");
				String t = s.getText(r);

				if (t == null || !t.equals("OK"))
					continue;

				ok = true;
				break;
			}

			if (!ok)
				return false;

			wait(1000);

			return true;
		}
		catch (IOException e)
		{
			throw new GameError("error sitting down", e);
		}
	}


	/**
	 * Enter buy-in amount. HACK ALERT: have an intelligent way to choose buy-in
	 * 
	 * @throws GameError
	 */
	private void enterBuyin() throws GameError
	{
		try
		{
			int ax = table.getActualAnchorX();
			int ay = table.getActualAnchorY();

			dbgShot("typing buyin");
			String buyinStr = doReal ? "10" : "500";
			state.type(buyinStr);
			state.post();
			wait(1000);

			dbgShot("clicking ok");
			state.click("lcs", "ok");
			state.post();
			state.refresh();
			wait(3000);

			dbgShot("checking cashier");
			Area a = table.getArea("a_cashier");
			Rect r = a.getRect(state.getRect(), ax, ay);
			Style s = table.getStyle("buttons");
			String t = s.getText(r);

			if (t == null || !t.equals("CASHIER"))
				fail("entering buyin: no cashier button", null);
		}
		catch (IOException e)
		{
			throw new GameError("error entering buy-in", e);
		}
	}


	/**
	 * Scan each seat's chair icon rectangle against the empty icon to see if
	 * the seat is occupied. Put results in emptyMap.
	 */
	private void getEmptyChairMap()
	{
		int ax = table.getActualAnchorX();
		int ay = table.getActualAnchorY();

		for (int i = 0; i < 10; i++)
		{
			Icon icon1 = table.getIcon("chair" + (i + 1) + "empty");
			Icon icon2 = table.getIcon("purple" + (i + 1));

			int x = chairPos[0][i] + ax;
			int y = chairPos[1][i] + ay;

			Rect rect = state
					.getRect(x, y, icon1.getWidth(), icon1.getHeight());
			emptyMap[i] = icon1.match(rect, 0, 0.99f)
					|| icon2.match(rect, 0, 0.99f);
			if (emptyMap[i])
				System.out.printf("chair %d is empty\n", i + 1);
		}
	}


	/**
	 * Find the position of the dealer by scanning the possible dealer token
	 * locations and comparing the subrects against the known dealer token
	 * icons. If none matches, the screen may still be transitioning, so
	 * refresh() until an icon matches.
	 * 
	 * @return whether dealer was found in 10 refreshes
	 */
	private boolean getDealerPos()
	{
		int pos = -1;
		int num = 10;

		int ax = table.getActualAnchorX();
		int ay = table.getActualAnchorY();

		Style dtok = table.getStyle("dtok");

		while (--num > 0)
		{
			for (int i = 0; i < 10; i++)
			{
				int x = tokPos[0][i] + ax;
				int y = tokPos[1][i] + ay;
				Rect rect = state.getRect(x - 5, y - 5, 25, 25);
				String s = dtok.getText(rect);
				if ((s != null) && s.equals("D"))
				{
					pos = i;
					break;
				}
				/*
				 * Icon icon = table.getIcon("tok" + (i + 1)); int x =
				 * tokPos[0][i] + ax; int y = tokPos[1][i] + ay; Rect rect =
				 * state.getRect(x, y, icon.getWidth(), icon.getHeight());
				 * System.out.printf("tok %d pos: (%d, %d)\n%s\n", i, x, y,
				 * rect.toHex(40)); if (icon.match(rect, 0.9f)) { pos = i;
				 * break; }
				 */
			}

			if (pos != -1)
				break;

			wait(500);
			state.refresh();
		}

		log("dealer position is " + pos);
		dealerPos = pos;

		return (num != 0);
	}


	/**
	 * Decide where to sit, by finding an open chair which will be available in
	 * the next round (if possible).
	 */
	private boolean chooseSeat()
	{
		int pos = -1;

		for (int i = 0; i < 10; i++)
		{
			if (!emptyMap[i])
				continue;

			pos = i;
			int n = 0;
			for (int j = dealerPos; j != i; j = (j + 1) % 10)
				if (!emptyMap[j])
					n++;

			if (n > 2)
				break;

			// if dpos == pos, n=0
			if (n == 0)
				break;

			// for later, prefer n=1 over n=2
		}

		// if no open seats, return false
		if (pos == -1)
			return false;

		log("chose seat position " + pos);
		sitinPos = pos;
		return true;
	}


	private void procMessages() throws GameError
	{
		Message m;
		boolean a = false;
		while ((m = getMessage()) != null)
		{
			boolean b = process(m);
			checkFailed();
			if (b)
				return;
			a = true;
		}
		checkFailed();

		if (!a)
			Thread.yield();
	}


	/**
	 * Process a trigger or chat message.
	 * 
	 * @param m
	 * @throws GameError
	 */
	private boolean process(Message m) throws GameError
	{
		// dbgShot("processing : " + m.toString());

		if (m.type.equals("chat"))
			return processChat((ChatValue) m.arg);
		else if (m.type.equals("invalid"))
			processInvalid((String) m.arg);
		else if (m.type.equals("trigger"))
			processTrigger((String) m.arg);
		else
			err("invalid message type: " + m.type);
		return false;
	}


	private String chatPlayerName(String n)
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < n.length(); i++)
		{
			char c = n.charAt(i);
			if (c != ' ' && c != '-')
				sb.append(c);
		}
		return sb.toString();
	}


	private boolean processChat(ChatValue v) throws GameError
	{
		dbgShot("chat : " + v.toString());

		try
		{
			String f = v.function;

			if (f.equals("hurry"))
			{
				String pname = chatPlayerName(v.args[0].function);
				if (!inHand)
				{
					asyncRemap(pname);
					return false;
				}
				sequentialRemap(pname);

				log(remap(pname) + " should hurry!");
				if (pname.equals(aiName))
					aiLostTrack();
			}
			else if (f.equals("standby"))
			{
				String pname = chatPlayerName(v.args[0].function);
				if (pname.equals(this.aiName))
					playerWaiting = true;
			}
			else if (f.equals("allin"))
			{
				String pname = chatPlayerName(v.args[0].function);
				asyncRemap(pname);
				if (!inHand)
					return false;
				log(remap(pname) + " is going all-in");
				int pid = getPid(pname);
				if (pid != -1)
					player.allIn(tid, hid, pid);
			}
			else if (f.equals("action"))
			{
				String pname = chatPlayerName(v.args[0].function);
				if (!inHand)
				{
					asyncRemap(pname);
					return false;
				}
				sequentialRemap(pname);
				asyncRemap(pname);

				Move m = Move.fromChat(v.args[1]);
				checkFailed(); // DBG

				if (handsShown && m.isFold())
				{
					// here we assume that after everybody has actually acted,
					// and one or more hands are shown, that extranneous folds
					// might still happen before the next hand
					dbg("skipping action, hand already shown");
					return false;
				}

				if (!pocketDone)
				{
					// here we assume that we are not really in the round, and
					// that soon a standby(ai) message will come. just return.
					dbg("skipping action, expecting standby(" + aiName + ")");
					return false;
				}
				
				beforeFirstAction = false;

				int pos = posmap.get(remap(pname));
				smartInitTable(dealerPos, pos);

				int pid = getPid(pname);
				if (pid == -1)
				{
					err("player not found: " + pname);
					dbgShot("shot (for not found)");

					addPlayer(pname);
					player.insertPlayer(tid, hid, nextPos, pname);
					pid = nextPos;
				}

				// dbg("calling playerMoved: " + pname + " (" + pid + ") = "
				// + m.getPassive());
				player.playerMoved(tid, hid, pid, m);
				checkFailed(); // DBG
				log(pname + " " + m.getPassive() + " "
						+ (v.args.length > 2 ? v.args[2].function : ""));
				setNextPid(player.getNextToAct(tid, hid));

				if (ghost && nextPos == 1)
				{
					dbg("folding ghost player");
					player.playerMoved(tid, hid, 1, Move.fold());
					setNextPid(player.getNextToAct(tid, hid));
					sitOut("~~ghost~~");
					ghost = false;
				}

				procHaveFolded();
			}
			else if (f.equals("ignore"))
			{

			}
			else if (f.equals("notattable"))
			{
				String pname = remap(chatPlayerName(v.args[0].function));
				dbg(pname + " leaves the table");
				removeFromUI(pname);
			}
			else if (f.equals("deal"))
			{
				if (!inHand)
					return false;

				String r = v.args[0].function;
				int rnd;
				if (r.equals("FLOP"))
					rnd = Player.POSTFLOP;
				else if (r.equals("TURN"))
					rnd = Player.POSTTURN;
				else
					rnd = Player.POSTRIVER;
				String[] cards = new String[v.args.length - 1];
				for (int i = 0; i < cards.length; i++)
					cards[i] = v.args[i + 1].function;
				Hand h = new Hand(cards);

				log("cards dealt in " + r + ": " + h.toString());
				player.cardsDealt(tid, hid, rnd, h);
				setNextPid(player.getNextToAct(tid, hid));
			}
			else if (f.equals("attable"))
			{
			}
			else if (f.equals("sitin"))
			{
				String pname = chatPlayerName(v.args[0].function);
				asyncRemap(pname);
				log(pname + " sits in");
				sitIn(remap(pname), true);
			}
			else if (f.equals("sitout"))
			{
				String pname = chatPlayerName(v.args[0].function);
				asyncRemap(pname);
				log(pname + " sits out");
				sitOut(remap(pname));
			}
			else if (f.equals("maxbets"))
			{
				log("betting is capped");
				if (!inHand)
					return false;
				int r = player.getRound(tid, hid);
				player.capBets(tid, hid, r);
			}
			else if (f.equals("showhand"))
			{
				String pname = chatPlayerName(v.args[0].function);
				log(pname + " shows hand " + v.args[1].toString());
				asyncRemap(pname);
				if (!inHand)
					return false;
				addShowHand(remap(pname), v.args[1]);
				handsShown = true;
			}
			else if (f.equals("dealer"))
			{
				String pname = chatPlayerName(v.args[0].function);
				log("next dealer will be " + pname);
				asyncRemap(pname);
				setNextDealer(remap(pname));
			}
			else if (f.equals("wins"))
			{
				String pname = chatPlayerName(v.args[0].function);
				asyncRemap(pname);
				pname = remap(pname);
				if (!anyHandStarted)
					return false;
				int i = (v.args.length == 2) ? 1 : 2;
				Money m = decodeAmount(v.args[i].function);
				log(pname + " wins " + v.args[i].function);
				addWinner(pname, m);
			}
			else if (f.equals("newhand"))
			{
				handNum = v.args[0].function;
				newHandStarted = true;
				nextPos = -1;
				return true;
			}
			else if (f.equals("pocket"))
			{
				if (!inHand)
					return true;
				String[] cards = new String[2];
				for (int i = 0; i < 2; i++)
					cards[i] = v.args[i].function;
				pocket = new Hand(cards);
				log("pocket cards: " + pocket.toString());
				pocketSet = true;
				return true;
			}
			else if (f.equals("chat"))
			{
				if (inHand)
					player.userChatted(tid, hid,
							chatPlayerName(v.args[0].function),
							v.args[1].function, false);
				log("chat: " + v.args[0].function + " said: "
						+ v.args[1].function);
			}
			else
				err("invalid chat value: " + v.toString());
		}
		catch (RemoteException e)
		{
			fail("play of hand", e);
		}
		catch (PokerError e)
		{
			fail("play of hand", e);
		}
		return false;
	}


	private void addPlayer(String pname)
	{
		int p = nextPos - 1;
		if (p < 0)
			p += players.length;
		int pos1 = posmap.get(remap(players[p]));
		int pos2 = posmap.get(remap(players[nextPos]));
		int n = (pos1 + 1) % 10;
		if (n == pos2)
		{
			// err; no space
		}
		posmap.put(pname, n);
		pos2pid[n] = nextPos;
		sittingIn.add(pname);
	}


	private void removeFromUI(String name)
	{
		for (int i = 0; i < 10; i++)
		{
			if (uiNames[i] != null && uiNames[i].equals(name))
			{
				uiNames[i] = null;
				uiValid[i] = false;
				break;
			}
		}

	}


	private void aiLostTrack()
	{
		if (inHand)
		{
			if (firstAct)
				perform(lastMove);
			else
				nextPos = getCheapAiPid(); // hack to get playHand to proceed
		}
		else
		{
			err("AI lost track of state");
			fail("play of hand", new Exception("AI lost track of state"));
		}
	}


	private int getAiPid()
	{
		return pos2pid[sitinPos];
	}


	private void addWinner(String pname, Money m)
	{
		if (!winMoney.containsKey(pname))
			winMoney.put(pname, new Money(0, 0));
		winMoney.get(pname).addIn(m);
	}


	private void setNextDealer(String string)
	{
		nextDealer = string;
		// setNextDealer = true;
		// sitIn(string, true);
	}


	private void addShowHand(String pname, ChatValue v)
	{
		try
		{
			showHands.put(pname, HandDist.getShowDist(player.getBoardCards(tid,
					hid), pocket, v));
		}
		catch (RemoteException e)
		{
			fail("getting board cards", e);
		}
	}


	private void sitOut(String pname)
	{
		int pos = pos(pname);
		if (pos != -1)
		{
			// uiNames[pos] = null;
			uiMoney[pos] = null;
			uiValid[pos] = false;
		}
		if (sittingIn.contains(pname))
		{
			sittingIn.remove(pname);

			if (satIn && sittingIn.size() == 6)
			{
				log("now too few players");
				sitOut();
			}
		}

		if (inHand)
		{
			if (isPlayer(pname))
				willFold(pname);
		}
	}


	private void willFold(String pname)
	{
		int i = getPid(pname);
		if (i == -1)
			err("player not found (willFold): " + pname);
		else
			willFold[getPid(pname)] = true;
	}


	private void procHaveFolded()
	{
		try
		{
			int pid = player.getNextToAct(tid, hid);
			while ((pid != -1) && willFold[pid])
			{
				log("folding " + players[pid] + ", who sat out");
				player.playerMoved(tid, hid, pid, Move.fold());
				willFold[pid] = false;
				pid = player.getNextToAct(tid, hid);
			}
		}
		catch (RemoteException e)
		{
			fail("processing sat-out as folded", e);
		}

	}


	private boolean isPlayer(String pname)
	{
		return posmap.containsKey(remap(pname));
	}


	private void asyncRemap(String p)
	{
		if (this.nameMap.containsKey(p))
			return;
		for (int i = 0; i < 10; i++)
			if (tryRemap(i, p))
				break;
	}


	private boolean tryRemap(int i, String p)
	{
		if (!uiValid[i])
			return false;
		String u = uiNames[i];

		if (u.equals(p))
			return true;

		if (u.startsWith(p) || p.startsWith(u))
		{
			doRemap(i, u, p);
			return true;
		}

		int max_dif = 2;
		int pl = p.length();
		int ul = u.length();
		int n = pl - ul;
		if ((n > 0) && (n <= max_dif))
		{
			String[] difs = findDiffs(u, p, max_dif);
			if (difs == null)
				return false;

			doRemap(i, u, p);
			for (String dif : difs)
				err("likely bad name decode portion: '" + dif + "'");
			return true;
		}
		return false;
	}


	private void doRemap(int i, String u, String p)
	{
		nameMap.put(p, u);
		log("remapping '" + p + "' to '" + u + "' (partial match)");
		dbgShot("remapped window", nameLabels[i]);
	}


	private void dbgShot(String msg, Component c)
	{
		Region r = c.getRegion(table.getActualAnchorX(), table
				.getActualAnchorY());
		dbgShot(msg, r.getRect(state.getRect()));
	}


	private String[] findDiffs(String a, String b, int max)
	{
		java.util.List<String> difs = new ArrayList<String>();
		int d = 0; // added to b idx

		for (int i = 0; i < a.length(); i++)
		{
			char c1 = a.charAt(i);
			char c2 = b.charAt(i + d);

			if (c1 == c2)
				continue;

			else if (i + d + 1 >= b.length())
				return null;

			else if (b.charAt(i + d + 1) == c1)
			{
				if (max-- == 0)
					return null;
				difs.add(new String(new char[] {c2}));
				d++;
			}

			else if (i + d + 2 >= b.length())
				return null;

			else if (b.charAt(i + d + 2) == c1)
			{
				if (max == 0)
					return null;
				difs.add(new String(new char[] {c2, b.charAt(i + d + 1)}));
				d += 2;
				max -= 2;
			}

			else
				return null;
		}

		int n = b.length() - (a.length() + d);
		if (n > 0)
		{
			if ((max - n) < 0)
				return null;
			difs.add(b.substring(b.length() - n));
		}

		return (String[]) difs.toArray(new String[0]);
	}


	private void sequentialRemap(String pname)
	{
		if (!inHand)
			return;
		if (this.nameMap.containsKey(pname))
			return;

		int pid = nextPos;
		int pos = -1;
		// this is stupid
		for (int i = 0; i < pos2pid.length; i++)
			if (pos2pid[i] == pid)
			{
				pos = i;
				break;
			}
		if (pos != -1)
			tryRemap(pos, pname);
	}


	private String remap(String pname)
	{
		String aname = nameMap.get(pname);
		return (aname == null) ? pname : aname;
	}


	private void sitIn(String name, boolean b)
	{
		if (b || !sittingIn.contains(name))
		{
			if (b)
			{
				joins.add(name);
				sittingIn.add(name);
				log(name + " sits in");
			}
			else
			{
				sittingIn.add(name);
				log(name + " now valid");
				if (!satIn && sittingIn.size() == 7)
					sitIn();
			}
		}
	}


	private void setNextPid(int nextToAct)
	{
		this.nextPos = nextToAct;
	}


	private int getPid(String pname)
	{
		String aname = remap(pname);
		int pos = pos(aname);
		if (pos == -1)
			return -1;
		return pos2pid(pos);
	}


	private int pos(String aname)
	{
		return posmap.containsKey(aname) ? posmap.get(aname) : -1;
	}


	private int pos2pid(int pos)
	{
		return pos2pid[pos];
	}


	private String getField(String name)
	{
		return data.getScreenData("table").getField(name).getValue();
	}


	private void processTrigger(String name)
	{
		// bet, pot, rake
		// pX_name, pX_money
		// status
		// hand_no

		if (name.equals("bet"))
		{
			uiBet = decodeAmount(getField("bet"));
		}
		else if (name.equals("pot"))
		{
			uiPot = decodeAmount(getField("pot"));
		}
		else if (name.equals("rake"))
		{
			uiRake = decodeAmount(getField("rake"));
		}
		else if (name.endsWith("_name"))
		{
			String s = name.substring(1, name.length() - 5);
			int i = Integer.parseInt(s);
			replaceName(i, getField(name));
		}
		else if (name.endsWith("_money"))
		{
			String s = name.substring(1, name.length() - 6);
			int i = Integer.parseInt(s);
			replaceMoney(i, getField(name));
		}
		else if (name.equals("status"))
		{
		}
		else if (name.equals("hand_no"))
		{
		}
		else if (name.equals("imback"))
		{
			if (getField("imback").equals("I'MBACK"))
				graphicalError();
		}
		else
			err("invalid trigger target: " + name);
	}


	/**
	 * IMBACK button detected, i.e. we are screwed.
	 */
	private void graphicalError()
	{
		dbgShot("AI out of loop, got IMBACK");
		log("sitting out because of IMBACK");
		fail("detection of IMBACK", new Exception("detection of IMBACK"));
	}


	private void replaceName(int i, String name)
	{
		if (name == null || name.length() == 0)
			return;
		if (uiNames[i] == null || !uiNames[i].equals(name))
			dbgShot("inserting " + name + " at position " + i);
		uiNames[i] = name;
		uiValid[i] = true;
		if (!anyHandStarted)
			sitIn(name, false);
		// TODO: bfa set after newhand msg, cleared at first action
		// TODO: ba cleared after newhand msg
		else if (beforeFirstAction)
			becameActive.add(name);
	}


	private void replaceMoney(int i, String m)
	{
		if (m == null || m.length() == 0)
			return;
		if (m.equals("ALLIN"))
			try
			{
				dbg("setting player " + uiNames[i] + " (" + pos2pid[i]
						+ ") all-in");
				player.allIn(tid, hid, pos2pid[i]);
			}
			catch (Exception e)
			{
				err("problem setting player " + uiNames[i] + " all-in");
			}
		uiMoney[i] = decodeAmount(m);
	}


	private Money decodeAmount(String m)
	{
		if (m == null || m.length() == 0)
			return null;
		if (m.equals("ALLIN"))
			return new Money(0, 0); // TODO: handle all-in players
		try
		{
			return Money.parse(m);
		}
		catch (Exception e)
		{
			err("invalid money string: " + m);
			return new Money(0, 0);
		}
	}


	private void processInvalid(String text)
	{
		dbgShot("screenshot of invalid text");
		err("invalid chat: " + text);
	}


	private Message getMessage()
	{
		synchronized (messages)
		{
			if (messages.size() == 0)
				return null;
			return messages.remove(0);
		}
	}


	private void addMessage(Message m)
	{
		synchronized (messages)
		{
			messages.add(m);
		}
	}


	/**
	 * @see poker.server.session.house.Game#isStale()
	 */
	public boolean isStale()
	{
		return leaveTable;
	}


	public void sitOut()
	{
		dbg("in sitOut()");

		if (satIn)
		{
			log("sitting out");

			new Error().printStackTrace(); // DBG

			leaveTable = true;

			roundsPlayed = 0;
			state.click("lcs", "sit_out");
			satIn = false;
			try
			{
				state.post();
			}
			catch (IOException e)
			{
				fail("sitting out", e);
			}
		}
	}


	/**
	 * @see poker.server.session.house.Game#leaveTable()
	 */
	public void leaveTable()
	{
		dbg("leaving table");

		new Exception().printStackTrace(System.out); // DBG

		anyHandStarted = newHandStarted = false;

		if (state.inAuto())
		{
			dbg("have to leave auto-mode first");
			state.setAuto(false);
		}

		satIn = false;
		if (atTable)
		{
			state.click("lc", "leave");
			try
			{
				state.post();
			}
			catch (IOException e1)
			{
				fail("leaving table", e1);
			}
		}

		try
		{
			session.removeActiveTable("pokerroom.com", tableName);
		}
		catch (RemoteException e)
		{
			fail("leaving table", e);
		}
		atTable = false;
		wait(10000);

		state.gotoScreen("lobby");
		dbgShot("left table");

		leaveTable = false;
	}


	/**
	 * @see poker.server.session.house.Game#setPlayer(poker.server.base.Player)
	 */
	public void setPlayer(Player newPlayer)
	{
		player = newPlayer;
	}


	/**
	 * @throws GameError
	 * @see poker.server.session.house.Game#initApps()
	 */
	public void initApps() throws GameError
	{
		// HACK ALERT: game needs to be passed PokerPlayerId.
		// this could be part of a cluster config file
		int port = session.getVncClient().getPort();
		String playerId = "poker" + (port - 5900);

		// session has now connected VNC client, so make
		// VncState retrieve the pixel window
		state.resetRect();

		dbgShot("beginning app init");

		String appletFile = playerId + ".applet";

		String site = "https://www.pokerroom.com/games/play/poker4/";
		Map<String, String> params = new HashMap<String, String>();
		params.put("extras", "p4 TexasHoldem");
		params.put("nickname", aiName);
		params.put("password", aiPass);
		params.put("rememberNickname", "0");
		params.put("autoLogin", "0");
		String[] actions = new String[] {"applet:delattr:codebase",
				"param:deltag:name:noncachedurl",
				"param:modattr:name:isRealMoney:value:doReal"};

		try
		{
			dbgShot("about to grab applet");
			session.grabApplet(site, appletFile, doReal, params, actions);
			dbgShot("applet grabbed");
		}
		catch (RemoteException e)
		{
			fail("grabbing applet", e);
			checkFailed();
		}

		// start applet viewer
		dbgShot("starting appletviewer");
		avpid = session.startApp(viewerCommand + appletFile);

		wait(5000);
		dbgShot("going to lobby");
		if (!state.gotoScreen("lobby"))
		{
			checkFailed();
			fail("couldn't find the lobby", new Exception(""));
		}
		checkFailed();

		dbgShot("now at lobby; waiting for init");
		wait(10000);
		dbgShot("done with init");
	}


	/**
	 * Check whether failure was recieved, and if it was, throw an error with
	 * the failure message.
	 * 
	 * @throws GameError
	 */
	private void checkFailed() throws GameError
	{
		if (failed)
		{
			failed = false;
			GameError e = new GameError(failMsg, failExc);
			if (prefail)
			{
				dbgShot("preemptive fail print");
				e.printStackTrace();
			}
			throw e;
		}
	}


	/**
	 * Log a debug message.
	 * 
	 * @param msg
	 */
	private void dbg(String msg)
	{
		try
		{
			getLogServer().log(log, "dbg", "game", msg, null);
			System.out.printf("LOG: %s : %s : %s\n", "dbg", "game", msg);
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
		}
	}


	private void log(String msg)
	{
		try
		{
			getLogServer().log(log, "log", "game", msg, null);
			System.out.printf("LOG: %s : %s : %s\n", "log", "game", msg);
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
		}
	}


	/**
	 * Log a debug message with a screenshot.
	 * 
	 * @param msg
	 */
	private void dbgShot(String msg)
	{
		if (state.inAuto())
		{
			dbgShot(msg, state.getRect());
		}
		else
		{
			Rect rect = state.getRect();
			if (rect == null)
				System.out.println("NULL RECT!!!"); // DBG
			boolean upd = state.refresh(); // DBG
			dbgShot(msg + (upd ? " (+)" : " (-)"), rect);
		}
		// state.refresh();
		// log.log("dbg", "game", msg, state.getRect());
	}


	private void softShot()
	{
		Rect rect = state.getRect();
		dbgShot("soft shot", rect);
	}


	private void dbgShot(String msg, Rect rect)
	{
		try
		{
			getLogServer().log(log, "dbg", "game", msg, null);
			System.out.printf("SHOT: %s : %s : %s (%s)\n", "dbg", "game", msg,
					"dbg-" + shotCount + ".img");
			rect.write("/home/lowentropy/src/screendump/dbg-" + (shotCount++)
					+ ".img");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}


	/**
	 * Log an error.
	 * 
	 * @param msg
	 */
	private void err(String msg)
	{
		try
		{
			getLogServer().log(log, "err", "game", msg, null);
			System.out.printf("LOG: %s : %s : %s\n", "err", "game", msg);
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
		}
	}


	/**
	 * @see poker.server.session.house.Game#clearApps()
	 */
	public void clearApps()
	{
		if (atTable)
			leaveTable();

		session.killApp(avpid);
		avpid = -1;

		dbgShot("closed ff, waiting");
		wait(5000);
		dbgShot("done waiting");
	}


	/**
	 * @see poker.server.session.model.data.chat.ChatEventHandler#handleEvent(poker.server.session.model.data.chat.ChatValue)
	 */
	public void handleEvent(ChatValue value)
	{
		addMessage(new Message("chat", value));
		// dbg("CHAT: " + value.toString());
	}


	/**
	 * @see poker.server.session.model.data.chat.ChatEventHandler#handleInvalid(java.lang.String)
	 */
	public void handleInvalid(String text)
	{
		softShot();
		addMessage(new Message("invalid", text));
	}


	/**
	 * @see poker.server.session.house.ScreenEventHandler#trigger(java.lang.String)
	 */
	public void trigger(String name)
	{
		addMessage(new Message("trigger", name));
	}


	/**
	 * @see poker.server.session.house.ScreenEventHandler#initScreen(java.lang.String)
	 */
	public boolean initScreen(String name)
	{
		return state.defInitScreen(name);
	}


	/**
	 * @see poker.server.session.house.ScreenEventHandler#initWindow()
	 */
	public boolean initWindow()
	{
		return state.defInitWindow();
	}


	/**
	 * @see poker.server.session.house.ScreenEventHandler#messagesSent(java.lang.String[])
	 */
	public void messagesSent(String[] msgs)
	{
	}


	/**
	 * @see poker.server.session.house.ScreenEventHandler#messagesRecieved(java.lang.String[])
	 */
	public void messagesRecieved(String[] msgs)
	{
	}


	/**
	 * @see poker.server.session.house.ScreenEventHandler#wait(int)
	 */
	public void wait(int milli)
	{
		state.defWait(milli);
	}


	/**
	 * @see poker.server.session.house.ScreenEventHandler#switchedMode(boolean)
	 */
	public void switchedMode(boolean auto)
	{
		dbg("switched mode: " + auto);
	}


	/**
	 * @see poker.server.session.house.ScreenEventHandler#store(poker.server.session.model.data.Field,
	 *      java.lang.Object, java.lang.Object)
	 */
	public void store(Field target, Object src, Object value)
	{
		state.defStore(target, src, value);
	}


	/**
	 * @see poker.server.session.house.ScreenEventHandler#store(poker.server.session.model.data.List,
	 *      int, java.lang.Object[], java.lang.Object[])
	 */
	public void store(List target, int idx, Object[] src, Object[] value)
	{
		state.defStore(target, idx, src, value);
	}


	/**
	 * @see poker.server.session.house.ScreenEventHandler#scanDone(java.lang.String,
	 *      int)
	 */
	public void scanDone(String name, int num)
	{
		// dbg("in scanDone()");
		dbgShot("SCAN DONE: " + name + " : " + num);
		numScanned = num;
	}


	/**
	 * @see poker.server.session.house.ScreenEventHandler#triggersCleared()
	 */
	public void triggersCleared()
	{
	}


	/**
	 * @see poker.server.session.house.ScreenEventHandler#fail(java.lang.String,
	 *      java.lang.Throwable)
	 */
	public void fail(String task, Throwable exc)
	{
		failMsg = "error during " + task;
		failExc = exc;
		failed = true;
	}


	/**
	 * @see poker.server.session.house.ScreenEventHandler#stopAfter(poker.server.session.model.data.ListItem)
	 */
	public boolean stopAfter(ListItem item)
	{
		String fill = item.getField("fill").getValue();
		String type = item.getField("type").getValue();
		String limit = item.getField("limit").getValue();
		String tn = item.getField("name").getValue();

		dbgShot("scanned: name = " + tn + ", fill = " + fill + ", type = "
				+ type + ", limit = " + limit);

		if (!fill.equals("8/10") && !fill.equals("9/10"))
			return false;
		// if (!type.equals("Play"))
		// return false;
		if (limit.indexOf("-") == -1)
			return false;

		try
		{
			if (session.isActiveTable("pokerroom.com", tn))
				return false;
		}
		catch (RemoteException e)
		{
			fail("checking for active table status", e);
		}

		tableName = tn;
		int idx = limit.indexOf("-");
		llimit = limit.substring(0, idx);
		hlimit = limit.substring(idx + 1);

		if (doReal)
		{
			if (!llimit.equals("$0.50") || !hlimit.equals("$1"))
				return false;
			if (tn.equals("Catawba"))
				return false;
		}
		log("chose table: " + tn);

		return true;
	}

}
