
package poker.util.web;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.BoxLayout;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerListModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import bayes.Distribution;

import poker.ai.core.Hand;
import poker.common.Deck;
import poker.common.Money;
import poker.common.PokerError;
import poker.server.base.Move;
import poker.server.base.Player;
import poker.server.base.impl.LoosePokerNetPlayer;
import poker.server.base.impl.LoosePokerNetTable;
import poker.server.session.house.House;
import poker.server.session.house.HouseLoader;


/**
 * This applet lets the user play interactively with the Gambler.
 * 
 * @author lowentropy
 */
public class GamblerApplet extends JApplet implements ActionListener
{

	/** serial version UID */
	private static final long	serialVersionUID	= 4121129225748165176L;

	/** if true, produces debugging output */
	private boolean				DEBUG				= true;

	/** username field */
	private JTextField			unameField;

	/** password field */
	private JPasswordField		upassField;

	/** login button */
	private JButton				loginButton;

	/** logout button */
	private JButton				logoutButton;

	/** starts a new hand */
	private JButton				newHandButton;

	/** tabs for settings */
	private JTabbedPane			settingsPane;

	/** settings for table */
	private JPanel				tablePanel;

	/** settings for ai */
	private JPanel				aiPanel;

	/** settings for game */
	private JPanel				gamePanel;

	/** actions and info */
	private JPanel				actionPanel;

	/** contains text area and action panel */
	private JPanel				lowerPanel;

	/** contains login stuff */
	private JPanel				loginPanel;

	/** area where game info is printed */
	private JTextArea			outputArea;

	/** view profile button */
	private JButton				viewProfButton;

	/** bankroll amount */
	private JLabel				bankLabel;

	/** pot amount */
	private JLabel				potLabel;

	/** refills the bankroll to $1000 */
	private JButton				refillButton;

	/** fold button */
	private JButton				foldButton;

	/** check button */
	private JButton				checkButton;

	/** bet button */
	private JButton				betButton;

	/** call button */
	private JButton				callButton;

	/** raise button */
	private JButton				raiseButton;

	/** end hand button */
	private JButton				endHandButton;

	/** continuous table bias */
	private JSlider				tableBiasSlider;

	/** AI bias */
	private JSlider				aiBiasSlider;

	/** bias mode spinner */
	private JSpinner			biasModeSpinner;

	/** discrete table bias */
	private JSpinner			tableBiasSpinner;

	/** displays "Bias Mode:" */
	private JLabel				biasModeLabel;

	/** displays "Smart Opponents:" */
	private JLabel				smartLabel;

	/** displays "Loose-Passive Opponents:" */
	private JLabel				lpLabel;

	/** displays "Loose-Aggressive Opponents:" */
	private JLabel				laLabel;

	/** number smart opponents */
	private JTextField			smartField;

	/** number L-P opponents */
	private JTextField			lpField;

	/** number L-A opponents */
	private JTextField			laField;

	/** displays "AI Bias:" */
	private JLabel				aiLooseLabel;

	/** displays "Show AI Types:" */
	private JLabel				showTypesLabel;

	/** displays "All Opponents Show:" */
	private JLabel				allShowLabel;

	/** displays "Cheat Level:" */
	private JLabel				cheatLevelLabel;

	/** show AI types box */
	private JCheckBox			showTypesBox;

	/** all AIs show down box */
	private JCheckBox			allShowBox;

	/** turn off cheats box */
	private JCheckBox			noCheatsBox;

	/** show outs box */
	private JCheckBox			showOutsBox;

	/** show odds box */
	private JCheckBox			showOddsBox;

	/** show suggest box */
	private JCheckBox			showSuggestBox;

	/** show snoop box */
	private JCheckBox			snoopBox;

	/** displays "Limit:" */
	private JLabel				limitLabel;

	/** spins on table limit */
	private JSpinner			limitSpinner;

	/** randomize player positions */
	private JButton				randPosButton;

	/** user name */
	private String				uname;

	/** user password */
	private String				upass;

	/** use continuois bias? */
	private boolean				useContTableBias;

	/** table bias (discrete) is tight? */
	private boolean				tableBiasTight;

	/** continuous table bias */
	private float				tableBias;

	/** number smart opponents */
	private int					smartOpp;

	/** number L-P opponents */
	private int					lpOpp;

	/** number L-A opponents */
	private int					laOpp;

	/** AI looseness */
	private float				aiBias;

	/** show AI types? */
	private boolean				showTypes;

	/** all AIs show down? */
	private boolean				allShow;

	/** cheat: show outs? */
	private boolean				showOuts;

	/** cheat: show odds? */
	private boolean				showOdds;

	/** cheat: suggest? */
	private boolean				showSuggest;

	/** cheat: snoop? */
	private boolean				snoop;

	/** preflop limit */
	private Money				limit;

	/** projected player style */
	private Distribution		playerStyle;

	/** player's bankroll */
	private Money				bank;

	/** player names at the table */
	private String[]			players;

	/** user position at the table */
	private int					userPos;

	/** all usable player names */
	private String[]			allNames;

	/** random number generator */
	private Random				random;

	/** number of players in hand */
	private int					numPlayers;

	/** player object */
	private LoosePokerNetPlayer	player;

	/** poker table object */
	private LoosePokerNetTable	table;

	/** single poker deck */
	private Deck				deck;

	/** all players' hands */
	private Hand[]				hands;

	/** current hand id */
	private int					handId;

	/** biases of all players */
	private Distribution[]		biases;

	/** styles of all players */
	private Distribution[]		styles;

	/** antes of all players */
	private Money[]				antes;


	/**
	 * @see java.applet.Applet#init()
	 */
	public void init()
	{
		try
		{
			javax.swing.SwingUtilities.invokeAndWait(new Runnable()
			{

				public void run()
				{
					try
					{
						_init();
					}
					catch (RemoteException e)
					{
						e.printStackTrace();
					}
				}
			});
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}


	/**
	 * Initialization function. Initializes the interface only; game/system
	 * initialization is done by logging in.
	 * @throws RemoteException 
	 */
	private void _init() throws RemoteException
	{
		initObjects();
		initLayout();
		getAllPlayerNames();

		random = new Random(System.currentTimeMillis());
		player = new LoosePokerNetPlayer();
		deck = new Deck();
		
		// DBG
		unameField.setText("lowentropy");
		upassField.setText("lowentropy");
		doLogin();
		// doNewHand();
	}


	/**
	 * Initialize components.
	 */
	private void initObjects()
	{
		/* create text fields */
		unameField = makeField(15);
		upassField = makePassField(15);
		smartField = makeField(2);
		lpField = makeField(2);
		laField = makeField(2);

		/* create tabbed pane */
		settingsPane = new JTabbedPane();
		settingsPane.setEnabled(true);

		/* create settings panels */
		tablePanel = new JPanel();
		aiPanel = new JPanel();
		gamePanel = new JPanel();
		loginPanel = new JPanel();
		lowerPanel = new JPanel();
		actionPanel = new JPanel();

		/* create text area */
		outputArea = new JTextArea(16, 70);
		outputArea.setEnabled(false);

		/* create labels */
		biasModeLabel = makeLabel("Bias Mode:");
		smartLabel = makeLabel("Smart Opponents:");
		lpLabel = makeLabel("Loose-Passive:");
		laLabel = makeLabel("Loose-Aggressive:");
		aiLooseLabel = makeLabel("AI Looseness:");
		showTypesLabel = makeLabel("Show AI Types:");
		allShowLabel = makeLabel("All AI's Show Down:");
		cheatLevelLabel = makeLabel("Cheat Level:");
		bankLabel = makeLabel("Bankroll:");
		potLabel = makeLabel("Pot:");
		limitLabel = makeLabel("Limit:");

		/* create buttons */
		loginButton = makeButton("Log In", "login");
		logoutButton = makeButton("Log Out", "logout");
		viewProfButton = makeButton("View Profile", "viewprof");
		refillButton = makeButton("Refill", "refill");
		foldButton = makeButton("Fold", "fold");
		checkButton = makeButton("Check", "check");
		betButton = makeButton("Bet", "bet");
		callButton = makeButton("Call", "call");
		raiseButton = makeButton("Raise", "raise");
		endHandButton = makeButton("End Hand", "endhand");
		newHandButton = makeButton("New Hand", "newhand");
		randPosButton = makeButton("Randomize Positions", "rand");

		/* create spinners */
		biasModeSpinner = makeSpinner("Discrete", "Continuous");
		tableBiasSpinner = makeSpinner("Loose", "Tight");
		limitSpinner = makeSpinner("$1-$2", "$2-$4", "$4-$8", "$10-$20",
				"$50-$100", "$100-$200");

		/* create sliders */
		tableBiasSlider = makeSlider(100);
		aiBiasSlider = makeSlider(100);

		/* create check boxes */
		showTypesBox = makeCheckbox(false, null, "");
		allShowBox = makeCheckbox(false, null, "");
		noCheatsBox = makeCheckbox(true, "none", "nocheats");
		showOutsBox = makeCheckbox(false, "show outs", "showouts");
		showOddsBox = makeCheckbox(false, "show win odds", "showodds");
		showSuggestBox = makeCheckbox(false, "suggest move", "showsuggest");
		snoopBox = makeCheckbox(false, "peek", "snoop");

		/* enable the login components */
		loginButton.setEnabled(true);
		unameField.setEnabled(true);
		upassField.setEnabled(true);
	}


	/**
	 * Add all objects to the screen.
	 */
	private void initLayout()
	{
		initTablePanel();
		initAiPanel();
		initGamePanel();
		initSettingsPane();
		initLoginPanel();
		initActionPanel();
		initLowerPanel();

		setLayout(new BorderLayout());
		getContentPane().add(loginPanel, BorderLayout.NORTH);
		getContentPane().add(settingsPane, BorderLayout.CENTER);
		getContentPane().add(lowerPanel, BorderLayout.SOUTH);

		setSize(1000, 600);
	}


	/**
	 * Initialize the table settings panel.
	 */
	private void initTablePanel()
	{
		JPanel cPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		cPanel.add(new JLabel("Tight"));
		cPanel.add(tableBiasSlider);
		cPanel.add(new JLabel("Loose"));

		JPanel lPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		lPanel.add(limitLabel);
		lPanel.add(limitSpinner);

		tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));
		tablePanel.add(biasModeLabel);
		tablePanel.add(biasModeSpinner);
		tablePanel.add(tableBiasSpinner);
		tablePanel.add(cPanel);
		tablePanel.add(lPanel);
	}


	/**
	 * Initialize the AI settings panel.
	 */
	private void initAiPanel()
	{
		aiPanel.setLayout(new GridLayout(4, 2));

		JPanel cPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		cPanel.add(new JLabel("Tight"));
		cPanel.add(aiBiasSlider);
		cPanel.add(new JLabel("Loose"));

		aiPanel.add(smartLabel);
		aiPanel.add(smartField);
		aiPanel.add(lpLabel);
		aiPanel.add(lpField);
		aiPanel.add(laLabel);
		aiPanel.add(laField);
		aiPanel.add(aiLooseLabel);
		aiPanel.add(cPanel);
	}


	/**
	 * Initialize the game panel.
	 */
	private void initGamePanel()
	{
		gamePanel.setLayout(new GridLayout(7, 2));

		gamePanel.add(showTypesLabel);
		gamePanel.add(showTypesBox);
		gamePanel.add(allShowLabel);
		gamePanel.add(allShowBox);
		gamePanel.add(cheatLevelLabel);
		gamePanel.add(noCheatsBox);
		gamePanel.add(new JLabel(""));
		gamePanel.add(showOutsBox);
		gamePanel.add(new JLabel(""));
		gamePanel.add(showOddsBox);
		gamePanel.add(new JLabel(""));
		gamePanel.add(showSuggestBox);
		gamePanel.add(new JLabel(""));
		gamePanel.add(snoopBox);
	}


	/**
	 * Initialize the game settings table.
	 */
	private void initSettingsPane()
	{
		settingsPane.addTab("Table", tablePanel);
		settingsPane.addTab("AI", aiPanel);
		settingsPane.addTab("Game", gamePanel);
	}


	/**
	 * Initialize the login panel.
	 */
	private void initLoginPanel()
	{
		loginPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		loginPanel.add(unameField);
		loginPanel.add(upassField);
		loginPanel.add(loginButton);
		loginPanel.add(logoutButton);
		loginPanel.add(randPosButton);
		loginPanel.add(newHandButton);
	}


	/**
	 * Initialize the action panel.
	 */
	private void initActionPanel()
	{
		actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));
		actionPanel.add(viewProfButton);
		actionPanel.add(refillButton);
		actionPanel.add(bankLabel);
		actionPanel.add(potLabel);
		actionPanel.add(foldButton);
		actionPanel.add(checkButton);
		actionPanel.add(betButton);
		actionPanel.add(callButton);
		actionPanel.add(raiseButton);
		actionPanel.add(endHandButton);
	}


	/**
	 * Initialize the lower panel
	 */
	private void initLowerPanel()
	{
		lowerPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		lowerPanel.add(outputArea);
		lowerPanel.add(actionPanel);
	}


	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent evt)
	{
		// ACTIONS

		String cmd = evt.getActionCommand();

		if (cmd.equals("login"))
		{
			doLogin();
		}
		else if (cmd.equals("logout"))
		{
			doLogout();
		}
		else if (cmd.equals("nocheats"))
		{
			showOutsBox.setSelected(false);
			showOddsBox.setSelected(false);
			showSuggestBox.setSelected(false);
			snoopBox.setSelected(false);
			showOuts = showOdds = showSuggest = snoop = false;
		}
		else if (cmd.equals("showouts"))
		{
			noCheatsBox.setSelected(false);
			showOuts = true;
		}
		else if (cmd.equals("showodds"))
		{
			noCheatsBox.setSelected(false);
			showOdds = true;
		}
		else if (cmd.equals("showsuggest"))
		{
			noCheatsBox.setSelected(false);
			showSuggest = true;
		}
		else if (cmd.equals("snoop"))
		{
			noCheatsBox.setSelected(false);
			snoop = true;
		}
		else if (cmd == null)
			;
		else if (cmd.equals("newhand"))
			doNewHand();
		else if (cmd.equals("rand"))
			randomizePlayers();
		else if (cmd.equals("viewprof"))
			doViewProfile();
		else if (cmd.equals("refill"))
			doRefill();
		else if (cmd.equals("endhand"))
			doEndHand();
		else if (cmd.equals("fold"))
			doMove(userPos, Move.fold());
		else if (cmd.equals("check"))
			doMove(userPos, Move.check());
		else if (cmd.equals("bet"))
			doMove(userPos, Move.bet());
		else if (cmd.equals("call"))
			doMove(userPos, Move.call());
		else if (cmd.equals("raise"))
			doMove(userPos, Move.raise());

		else
			error("illegal action: " + cmd, null);
	}


	/**
	 * Perform the move at this position. After, update table information if the
	 * round changes.
	 * 
	 * @param pos
	 *            position of mover
	 * @param move
	 *            move to take
	 */
	private void doMove(int pos, Move move)
	{
		try {
			move.printOdds();
		dbg(players[pos]+" "+move.getPassive()+" with "+this.hands[pos]+" with board "+table.getBoardCards(handId).toString());
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		
		try
		{
			int oldRound = table.getRound(handId);
			table.playerMoved(handId, pos, move);
			int newRound = table.getRound(handId);
			
			if (table.numBets() == 4)
				table.capBets(handId, newRound);

			if (oldRound != newRound)
			{
				if (newRound == Player.POSTFLOP)
				{
					table.cardsDealt(handId, newRound, deck.deal(3));
					System.out.printf("board now: %s\n", table.getBoardCards(handId));
				}
				else if (newRound > Player.POSTFLOP)
				{
					table.cardsDealt(handId, newRound, deck.deal(1));
					System.out.printf("board now: %s\n", table.getBoardCards(handId));
				}
				
			}
		}
		catch (Exception e)
		{
			error("Error: " + e.getMessage(), e);
			doEndHand();
		}
		
		if (pos == userPos)
			runOpponents();
	}


	/**
	 * Display a message showing user profile information.
	 */
	private void doViewProfile()
	{
		// User: <name> (<bank>)
		// 
		// Preflop: X% Tight, Y% Loose, Z% Drunk
		// Postflop: X% T-A, Y% L-P, Z% L-A
		// 
		// Rating: X/10

		int[] pof = new int[] {(int) (playerStyle.values[0] * 100.0),
				(int) (playerStyle.values[1] * 100.0),
				(int) (playerStyle.values[2] * 100.0)};

		String msg = "User: " + uname + " (" + bank.toString() + ")\n\n";
		msg += "Style: " + pof[0] + "% T-A, " + pof[1] + "% L-P, " + pof[2]
				+ "% L-A\n\n";

		int i = (useContTableBias ? (tableBias > 0.5 ? 1 : 0)
				: (tableBiasTight ? 0 : 1));
		int r = pof[0] * 10;

		msg += "Rating: " + r + " / 10";

		JOptionPane.showMessageDialog(this, msg);
	}


	private void doRefill()
	{
		// TODO Auto-generated method stub

	}


	/**
	 * Start a new hand.
	 */
	private void doNewHand()
	{
		setSettingsScreenEnabled(false);
		setGameScreenEnabled(true);
		readSettings();

		boolean ok = true;

		do
		{
			rotatePlayers();
			assignBiases();

			if (!dealHands())
			{
				ok = false;
				break;
			}

			if (!setupTable())
			{
				ok = false;
				break;
			}

			runOpponents();

		} while (false);

		if (!ok)
			doEndHand();
	}


	/**
	 * Assign biases to AI's.
	 */
	private void assignBiases()
	{
		int[] ntypes = new int[] {0, 0, 0};
		int[] max = new int[] {smartOpp, lpOpp, laOpp};
		String[] st = new String[] {"N", "LP", "LA"};
		
		styles = new Distribution[numPlayers];
		
		for (int i = 0; i < numPlayers; i++)
		{
			int j;

			if (i == userPos)
				j = 0;

			else
			{
				do
				{
					j = random.nextInt(3);
				} while (ntypes[j] == max[j]);

				ntypes[j]++;
			}

			System.out.printf("%d: %s: %s\n", i, players[i], st[j]); // DBG
			if (j == 0)
				styles[i] = new Distribution("style", st, new double[] {1.0,0.0,0.0});
			else if (j == 1)
				styles[i] = new Distribution("style", st, new double[] {1.0-aiBias,aiBias,0.0});
			else
				styles[i] = new Distribution("style", st, new double[] {1.0-aiBias,0.0,aiBias});
			
			System.out.printf("style: %s\n\n", styles[i].toString()); // DBG
		}
		System.out.printf("user pos: %d\n", userPos); // DBG
	}


	/**
	 * Get player bias based on table bias.
	 * 
	 * @return
	 */
	private Distribution getTableBiasDist()
	{
		if (useContTableBias)
			return new Distribution("bias", new String[] {"T","L"}, new double[] {
					tableBias, 1.0 - tableBias});
		else
			return new Distribution("bias", new String[] {"T","L"},
					new double[] {tableBiasTight ? 1.0 : 0.0,
							tableBiasTight ? 0.0 : 1.0});
	}




	/**
	 * Reset deck and deal hands to all players
	 * 
	 * @return
	 */
	private boolean dealHands()
	{
		try
		{
			deck.reset();
			hands = new Hand[numPlayers];
			for (int i = 0; i < numPlayers; i++){
				hands[i] = deck.deal(2);
			System.out.printf("%s holds %s\n", players[i], hands[i].toString()); // DBG
			}
		}
		catch (PokerError e)
		{
			error("Error dealing hands.", null);
			return false;
		}

		return true;
	}


	/**
	 * Set up the table for play using current settings.
	 * 
	 * @return
	 */
	private boolean setupTable()
	{
			try
		{
			// get antes
			antes = new Money[numPlayers];
			for (int i = 0; i < numPlayers; i++)
				antes[i] = new Money(0, 0);
			antes[1] = new Money(limit.divideBy(2));
			antes[2] = new Money(limit);
			
			// init table, hand
			int tableId = player.joinTable("table", uname, limit.divideBy(2),
					limit, limit, limit.multiplyBy(2), new Money(0, 0));
			table = (LoosePokerNetTable) player.getTable(tableId);
			handId = table.beginHand(players, antes);

			// hands, biases
			for (int i = 0; i < numPlayers; i++)
			{
				table.setPocket(handId, i, hands[i]);

				if (i == userPos)
					continue;

				table.setPlayerProfile(players[i], styles[i]);
			}
			
			table.setTableBias(getTableBiasDist());
		}
		catch (RemoteException e)
		{
			error("Error setting up table.", e);
			return false;
		}

		return true;
	}


	/**
	 * Run opponents until game is over or it's the player's turn.
	 */
	private void runOpponents()
	{
		try
		{
			do
			{
				int next = table.getNextToAct(handId);

				if (next == -1)
					handOver();
				else if (next == userPos)
					userTurn();
				else
				{
					Move move = table.requestMove(handId,
							table.getRound(handId), next);
					doMove(next, move);
					continue;
				}
				break;
			} while (true);
		}
		catch (RemoteException e)
		{
			error("Error: " + e.getMessage(), e);
			doEndHand();
		}
	}


	/**
	 * Opponents are done acting. Retrieve their states and print the output
	 * message. Then find out what actions are available to the user, and enable
	 * those buttons only.
	 */
	private void userTurn()
	{
//		boolean canCheck = table.canCheck(handId);
//		boolean canCall = table.canCall(handId);
//		boolean canRaise = table.canRaise(handId);
//		boolean canBet = table.canBet(handId);
		try {
		System.out.printf("It's your turn now (%d bets), you have " + hands[this.userPos] + " with board " + table.getBoardCards(handId).toString() + ".\n", table.numBets());
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}


	/**
	 * The hand is over. Calculate the winner and display the results, then end
	 * the hand.
	 */
	private void handOver()
	{
		System.out.printf("hand is over\n");
	}


	/**false
	 * End the hand, either because the end hand button was pressed, or because
	 * the hand is just over.
	 */
	private void doEndHand()
	{
		setGameScreenEnabled(false);
		setSettingsScreenEnabled(true);
	}


	/**
	 * Randomize player positions.
	 */
	private void randomizePlayers()
	{
		readSettings();
		players = new String[numPlayers];

		userPos = random.nextInt(numPlayers);
		players[userPos] = uname;

		List<String> used = new ArrayList<String>();

		for (int i = 0; i < numPlayers; i++)
		{
			if (i == userPos)
				continue;

			String opp = null;
			do
			{
				int j = random.nextInt(allNames.length);
				opp = allNames[j];
			} while (used.contains(opp));

			players[i] = opp;
			used.add(opp);
		}
	}


	/**
	 * Rotate the dealter token. If the number of players at the table changed,
	 * just randomize all the positions.
	 */
	private void rotatePlayers()
	{
		if ((players == null) || (numPlayers != players.length))
		{
			randomizePlayers();
			return;
		}

		userPos = (userPos - 1) % numPlayers;
		String p0 = players[0];
		for (int i = 0; i < players.length - 1; i++)
			players[i] = players[i + 1];
		players[numPlayers - 1] = p0;
	}


	/**
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	public void stateChanged(ChangeEvent evt)
	{
		System.out.println(evt.getClass().toString());

	}


	/**
	 * Perform login. This should either load or create a player profile, and
	 * initialize the table settings, including default values for all fields.
	 * It should then enable all visual components. It does NOT set up initial
	 * table conditions, as no hand has been started yet.
	 */
	private void doLogin()
	{
		uname = unameField.getText();
		upass = upassField.getText();

		if (!loadPlayer())
			return;

		unameField.setText("");
		upassField.setText("");
		unameField.setEnabled(false);
		upassField.setEnabled(false);
		loginButton.setEnabled(false);
		logoutButton.setEnabled(true);

		setSettingsScreenEnabled(true);
		initSettings();
	}


	/**
	 * Load the player's profile.
	 * 
	 * @return true if player loaded or created, false if passwords don't match
	 */
	private boolean loadPlayer()
	{
		String fname = "./data/" + uname + ".player";
		File file = new File(fname);
		if (!file.exists())
			if (!createPlayer(file))
				return false;

		String upass_tmp = upass;
		if (!loadProfile(file))
			return false;

		if (!upass_tmp.equals(upass))
		{
			error("Invalid password.", null);
			return false;
		}

		return true;
	}


	/**
	 * Parse a comma-separated list of double-precision floats.
	 * 
	 * @param s
	 *            string to parse
	 * @return array of doubles
	 */
	private static double[] readDoubles(String s)
	{
		String[] strs = s.split(",");
		double[] d = new double[strs.length];
		for (int i = 0; i < d.length; i++)
			d[i] = Double.parseDouble(strs[i].trim());
		return d;
	}


	/**
	 * Print a double array as a comma-separated list.
	 * 
	 * @param D
	 *            array of doubles
	 * @return string containing list
	 */
	private static String writeDoubles(double[] D)
	{
		StringBuilder sb = new StringBuilder();
		for (double d : D)
			sb.append(Double.toString(d) + ", ");
		sb.setLength(sb.length() - 2);
		return sb.toString();
	}


	/**
	 * Read a file of fields in the formap "field: value" into a map.
	 * 
	 * @param file
	 *            file to read
	 * @return map of field->value
	 * @throws IOException
	 */
	private static Map<String, String> readPlayerFile(File file)
			throws IOException
	{
		FileInputStream fis = new FileInputStream(file);
		Map<String, String> fields = new HashMap<String, String>();
		BufferedReader r = new BufferedReader(new InputStreamReader(fis));

		String line;
		while ((line = r.readLine()) != null)
		{
			int idx = line.indexOf(':');
			fields.put(line.substring(0, idx).trim(),
					line.substring(idx + 1).trim());
		}

		r.close();
		return fields;
	}


	/**
	 * Write a mapping of field->value to a file in the format "field: value",
	 * one on each line.
	 * 
	 * @param file
	 *            file to write to
	 * @param fields
	 *            map of field->value
	 * @throws FileNotFoundException
	 */
	private static void writePlayerFile(File file, Map<String, String> fields)
			throws FileNotFoundException
	{
		FileOutputStream fos = new FileOutputStream(file);
		PrintWriter w = new PrintWriter(new OutputStreamWriter(fos));

		for (String field : fields.keySet())
		{
			w.print(field);
			w.print(": ");
			w.println(fields.get(field));
		}

		w.close();
	}


	/**
	 * Create a default player profile.
	 */
	private boolean createPlayer(File file)
	{
		try
		{
			if (!file.createNewFile())
			{
				error("Could not create player profile: already exists.", null);
				return false;
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
			error("Could not create player profile: " + e.getMessage(), e);
			return false;
		}

		setDefaultProfile();
		if (!dumpProfile())
			return false;

		return true;
	}


	/**
	 * Set default values for all profile fields.
	 */
	private void setDefaultProfile()
	{
		playerStyle = new Distribution("style", new String[] {"N", "LP", "LA"},
				new double[] {1.0, 0.0, 0.0});
		useContTableBias = false;
		tableBiasTight = false;
		tableBias = 1.0f;
		limit = new Money(1, 0);
		smartOpp = 2;
		lpOpp = 3;
		laOpp = 4;
		aiBias = 0.5f;
		showTypes = true;
		allShow = false;
		showOuts = false;
		showOdds = false;
		showSuggest = false;
		snoop = false;
		bank = new Money(1000, 0);
	}


	/**
	 * Write the current player profile to a file.
	 * 
	 * @return whether write was successful
	 */
	private boolean dumpProfile()
	{
		Map<String, String> fields = new HashMap<String, String>();
		File file = new File("./data/" + uname + ".player");

		fields.put("user", uname);
		fields.put("pass", upass);
		fields.put("player_style", writeDoubles(playerStyle.values));
		fields.put("use_cont_table_bias", Boolean.toString(useContTableBias));
		fields.put("table_bias_disc", tableBiasTight ? "tight" : "loose");
		fields.put("table_bias_cont", Float.toString(tableBias));
		fields.put("limit", limit.toString());
		fields.put("num_smart_opp", Integer.toString(smartOpp));
		fields.put("num_lp_opp", Integer.toString(lpOpp));
		fields.put("num_la_opp", Integer.toString(laOpp));
		fields.put("ai_bias", Float.toString(aiBias));
		fields.put("show_ai_types", Boolean.toString(showTypes));
		fields.put("all_show_down", Boolean.toString(allShow));
		fields.put("show_outs", Boolean.toString(showOuts));
		fields.put("show_odds", Boolean.toString(showOdds));
		fields.put("show_suggest", Boolean.toString(showSuggest));
		fields.put("snoop", Boolean.toString(snoop));
		fields.put("bankroll", bank.toString());

		try
		{
			writePlayerFile(file, fields);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
			error("Could not write player profile.", e);
			return false;
		}

		return true;
	}


	/**
	 * Load the user profile.
	 * 
	 * @param file
	 *            file to load from
	 * @return whether load was successful
	 */
	private boolean loadProfile(File file)
	{
		Map<String, String> fields;
		try
		{
			fields = readPlayerFile(file);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			error("Could not read player profile.", e);
			return false;
		}

		uname = fields.get("user");
		upass = fields.get("pass");
		playerStyle = new Distribution("style", new String[] {"N", "LP", "LA"},
				readDoubles(fields.get("player_style")));
		useContTableBias = Boolean.parseBoolean(fields.get("use_cont_table_bias"));
		tableBiasTight = fields.get("table_bias_disc").equals("tight");
		tableBias = Float.parseFloat(fields.get("table_bias_cont"));
		limit = Money.parse(fields.get("limit"));
		smartOpp = Integer.parseInt(fields.get("num_smart_opp"));
		lpOpp = Integer.parseInt(fields.get("num_lp_opp"));
		laOpp = Integer.parseInt(fields.get("num_la_opp"));
		aiBias = Float.parseFloat(fields.get("ai_bias"));
		showTypes = Boolean.parseBoolean(fields.get("show_ai_types"));
		allShow = Boolean.parseBoolean(fields.get("all_show_down"));
		showOuts = Boolean.parseBoolean(fields.get("show_outs"));
		showOdds = Boolean.parseBoolean(fields.get("show_odds"));
		showSuggest = Boolean.parseBoolean(fields.get("show_suggest"));
		snoop = Boolean.parseBoolean(fields.get("snoop"));
		bank = Money.parse(fields.get("bankroll"));

		return true;
	}


	/**
	 * Clear all visual fields then disable them. Dump the player profile to the
	 * backend.
	 */
	private void doLogout()
	{
		readSettings();
		dumpProfile();
		clearSettings();
		clearGame();
		setGameScreenEnabled(false);
		setSettingsScreenEnabled(false);

		unameField.setEnabled(true);
		upassField.setEnabled(true);
		loginButton.setEnabled(true);
		logoutButton.setEnabled(false);
	}


	/**
	 * Clear all configuration widgets to default position.
	 */
	private void clearSettings()
	{
		biasModeSpinner.setValue("Discrete");
		tableBiasSpinner.setValue("Loose");
		tableBiasSlider.setValue(100);
		smartField.setText("2");
		lpField.setText("3");
		laField.setText("4");
		aiBiasSlider.setValue(50);
		showTypesBox.setSelected(true);
		allShowBox.setSelected(false);
		noCheatsBox.setSelected(true);
		showOutsBox.setSelected(false);
		showOddsBox.setSelected(false);
		showSuggestBox.setSelected(false);
		snoopBox.setSelected(false);
		limitSpinner.setValue("$1-$2");
		setBankroll("");
	}


	/**
	 * Initialize widget positions to profile settings.
	 */
	private void initSettings()
	{
		biasModeSpinner.setValue(useContTableBias ? "Continuous" : "Discrete");
		tableBiasSpinner.setValue(tableBiasTight ? "Tight" : "Loose");
		tableBiasSlider.setValue((int) (tableBias * 100.0f));
		smartField.setText(Integer.toString(smartOpp));
		lpField.setText(Integer.toString(lpOpp));
		laField.setText(Integer.toString(laOpp));
		aiBiasSlider.setValue((int) (aiBias * 100.0f));
		showTypesBox.setSelected(showTypes);
		allShowBox.setSelected(allShow);
		noCheatsBox.setSelected(!showOuts && !showOdds && !showSuggest
				&& !snoop);
		showOutsBox.setSelected(showOuts);
		showOddsBox.setSelected(showOdds);
		showSuggestBox.setSelected(showSuggest);
		snoopBox.setSelected(snoop);
		limitSpinner.setValue(toLimitStr(limit));
		setBankroll(bank.toString());
	}


	/**
	 * Read settings from visual widget positions.
	 */
	private void readSettings()
	{
		useContTableBias = biasModeSpinner.getValue().equals("Continuous");
		tableBiasTight = tableBiasSpinner.equals("Tight");
		tableBias = (float) tableBiasSlider.getValue() / 100.0f;
		smartOpp = Integer.parseInt(smartField.getText());
		lpOpp = Integer.parseInt(lpField.getText());
		laOpp = Integer.parseInt(laField.getText());
		aiBias = (float) aiBiasSlider.getValue() / 100.0f;
		showTypes = showTypesBox.isSelected();
		allShow = allShowBox.isSelected();
		limit = toLimitMoney((String) limitSpinner.getValue());
		numPlayers = smartOpp + lpOpp + laOpp + 1;
	}


	/**
	 * Clear the game widgets (the output area and pot, but not the bankroll).
	 */
	private void clearGame()
	{
		outputArea.setText("");
		setPot("");
	}


	/**
	 * Set pot amount in label.
	 * 
	 * @param s
	 *            pot amount string
	 */
	private void setPot(String s)
	{
		potLabel.setText("Pot: " + s);
	}


	/**
	 * Set the bankroll in the label.
	 * 
	 * @param s
	 *            bank amount string
	 */
	private void setBankroll(String s)
	{
		bankLabel.setText("Bankroll: " + s);
	}


	/**
	 * Enable or disable all game widgets.
	 * 
	 * @param b
	 */
	private void setGameScreenEnabled(boolean b)
	{
		outputArea.setEnabled(b);
		viewProfButton.setEnabled(b);
		bankLabel.setEnabled(b);
		potLabel.setEnabled(b);
		refillButton.setEnabled(b);
		foldButton.setEnabled(b);
		checkButton.setEnabled(b);
		betButton.setEnabled(b);
		callButton.setEnabled(b);
		raiseButton.setEnabled(b);
		endHandButton.setEnabled(b);
	}


	/**
	 * Enable or disable all settings widgets
	 * 
	 * @param b
	 */
	private void setSettingsScreenEnabled(boolean b)
	{
		biasModeSpinner.setEnabled(b);
		tableBiasSpinner.setEnabled(b);
		tableBiasSlider.setEnabled(b);
		limitLabel.setEnabled(b);
		limitSpinner.setEnabled(b);

		smartField.setEnabled(b);
		lpField.setEnabled(b);
		laField.setEnabled(b);
		aiBiasSlider.setEnabled(b);

		showTypesBox.setEnabled(b);
		allShowBox.setEnabled(b);
		noCheatsBox.setEnabled(b);
		showOutsBox.setEnabled(b);
		showOddsBox.setEnabled(b);
		showSuggestBox.setEnabled(b);
		snoopBox.setEnabled(b);

		newHandButton.setEnabled(b);
		randPosButton.setEnabled(b);
	}


	/**
	 * Pop up a dialog box notifying the error occurred.
	 * 
	 * @param reason
	 *            description of error
	 */
	private void error(String reason, Throwable t)
	{
		String msg = "error: " + reason;
		if (DEBUG && (t != null))
			t.printStackTrace();
		JOptionPane.showMessageDialog(this, msg);
	}


	/**
	 * Create a button. Calls actionPerformed() on container applet.
	 * 
	 * @param text
	 *            display text
	 * @param action
	 *            action command name
	 * @return button object
	 */
	private JButton makeButton(String text, String action)
	{
		JButton button = new JButton(text);
		button.setActionCommand(action);
		button.addActionListener(this);
		button.setEnabled(false);
		return button;
	}


	/**
	 * Create a text field, initially blank.
	 * 
	 * @param width
	 *            width of text field
	 * @return field object
	 */
	private JTextField makeField(int width)
	{
		JTextField field = new JTextField(width);
		field.setEnabled(false);
		return field;
	}


	/**
	 * Create a password text field, initially blank.
	 * 
	 * @param width
	 *            width of text field
	 * @return field object
	 */
	private JPasswordField makePassField(int width)
	{
		JPasswordField field = new JPasswordField(width);
		field.setEnabled(false);
		return field;
	}


	/**
	 * Create a label with the given text.
	 * 
	 * @param text
	 *            text of label
	 * @return label object
	 */
	private JLabel makeLabel(String text)
	{
		JLabel label = new JLabel(text);
		label.setEnabled(false);
		return label;
	}


	/**
	 * Create a spinner with the given options, first is default.
	 * 
	 * @param options
	 *            options on spinner
	 * @return spinner object
	 */
	private JSpinner makeSpinner(String... options)
	{
		SpinnerListModel model = new SpinnerListModel(options);
		JSpinner spinner = new JSpinner(model);
		spinner.setMaximumSize(new Dimension(300, 50));
		spinner.setEnabled(false);
		return spinner;
	}


	/**
	 * Create a slider with min=0, max=100, value=100.
	 * 
	 * @param width
	 *            width of slider (min and max, not preferred).
	 * @return slider object
	 */
	private JSlider makeSlider(int width)
	{
		JSlider slider = new JSlider(0, 100, 100);
		slider.setMinimumSize(new Dimension(width, 0));
		slider.setMaximumSize(new Dimension(width, 1000));
		slider.setEnabled(false);
		return slider;
	}


	/**
	 * Create a checkbox.
	 * 
	 * @param selected
	 *            whether the checkbox should be selected by default
	 * @param cmd
	 *            action command name
	 * @return checkbox object
	 */
	private JCheckBox makeCheckbox(boolean selected, String text, String cmd)
	{
		JCheckBox box = new JCheckBox();
		box.setSelected(selected);
		box.setActionCommand(cmd);
		box.addActionListener(this);
		box.setEnabled(false);
		box.setText(text == null ? "" : text);
		return box;
	}


	/**
	 * Print debugging output.
	 * 
	 * @param s
	 *            string to print
	 */
	private void dbg(String s)
	{
		if (DEBUG)
			System.out.println(s);
	}


	/**
	 * Get all possible player names.
	 */
	private void getAllPlayerNames()
	{
		String allOpp = "Opponents:\n\n";

		try
		{
			File file = new File("./data/opponents.txt");
			BufferedReader r = new BufferedReader(new InputStreamReader(
					new FileInputStream(file)));

			String line;
			List<String> names = new ArrayList<String>();

			while ((line = r.readLine()) != null)
			{
				names.add(line);
				allOpp += line + "\n";
			}

			allNames = names.toArray(new String[0]);
			r.close();
		}
		catch (IOException e)
		{
			error("Could not load opponent list.", e);
		}

		outputArea.setText(allOpp);
	}


	/**
	 * Convert lower limit amount to limit string.
	 * 
	 * @param limit
	 *            amount of lower limit
	 * @return $X-$[2X]
	 */
	private static String toLimitStr(Money limit)
	{
		return limit.toString() + "-" + limit.multiplyBy(2).toString();
	}


	/**
	 * Convert limit string to lower limit amount.
	 * 
	 * @param str
	 *            $X-$[2X]
	 * @return limit amount
	 */
	private static Money toLimitMoney(String str)
	{
		return Money.parse(str.substring(0, str.indexOf('-')));
	}
}
