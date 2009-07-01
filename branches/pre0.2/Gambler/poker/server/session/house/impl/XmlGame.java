
package poker.server.session.house.impl;

import poker.ai.core.Hand;
import poker.common.PokerError;
import poker.server.base.Player;
import poker.server.log.LogServer;
import poker.server.log.LogSession;
import poker.server.session.PokerSession;
import poker.server.session.house.Game;
import poker.server.session.house.ScreenEventHandler;
import poker.server.session.house.VncState;
import poker.server.session.model.data.DataModel;
import poker.server.session.model.data.Field;
import poker.server.session.model.data.List;
import poker.server.session.model.data.ListItem;
import poker.server.session.model.data.ScreenData;
import poker.server.session.model.data.chat.ChatEventHandler;
import poker.server.session.model.data.chat.ChatValue;


public abstract class XmlGame implements Game, ChatEventHandler,
		ScreenEventHandler
{

	/** poker session which is serving this game */
	protected PokerSession	session;

	/** player AI connection */
	protected Player		player;

	/** data collected on-screen */
	protected DataModel		data;

	/** whether game is passive (non-served) */
	private boolean			passive;

	/** whether a table has been selected */
	private boolean			selectedTable;

	/** state of VNC screen */
	protected VncState		state;

	protected int			log;

	private LogServer		ls;

	protected XmlHouse		schema;


	/**
	 * Constructor.
	 * 
	 * @param session
	 * @param player
	 * @param log
	 */
	public XmlGame(XmlHouse schema, PokerSession session, Player player,
			LogServer ls, int log)
	{
		this.session = session;
		this.player = player;
		this.passive = ((session == null) || (player == null));
		this.selectedTable = false;
		this.log = log;
		this.schema = schema;

		this.state = (session == null) ? null : new VncState(schema, data,
				session.getVncClient());
		if (state != null)
			state.setScreenEventHandler(this);

		this.ls = ls;
	}


	protected LogServer getLogServer()
	{
		return ls;
	}


	/**
	 * TODO: comment XmlGame.getDataset
	 * 
	 * @param screen
	 * @return
	 */
	public ScreenData getDataset(String screen)
	{
		return data.getScreenData(screen);
	}


	/**
	 * TODO: comment XmlGame.getHand
	 * 
	 * @param args
	 * @param start
	 * @return
	 * @throws PokerError
	 */
	protected static Hand getHand(ChatValue[] args, int start)
			throws PokerError
	{
		String[] strs = new String[args.length - start];
		for (int i = 0; i < strs.length; i++)
			strs[i] = args[i + start].function;
		return new Hand(strs);
	}


	/**
	 * TODO: comment XmlGame.setData
	 * 
	 * @param data
	 */
	public void setData(DataModel data)
	{
		this.data = data;
		for (ScreenData d : data.getScreens())
			d.setGame(this);
		if (this.state != null)
			this.state.setData(data);
	}


	/**
	 * TODO: comment XmlGame.getDataModel
	 * 
	 * @return
	 */
	public DataModel getDataModel()
	{
		return data;
	}

}
