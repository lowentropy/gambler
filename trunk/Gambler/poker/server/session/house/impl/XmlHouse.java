package poker.server.session.house.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import poker.server.base.Player;
import poker.server.log.LogServer;
import poker.server.session.PokerSession;
import poker.server.session.model.ModelError;
import poker.server.session.model.data.DataModel;
import poker.server.session.model.visual.Screen;
import poker.util.xml.XmlObject;

/**
 * House defined by XML file.
 * 
 * @author lowentropy
 */
public class XmlHouse
{

	/** name of house */
	private String name;

	/** screens defined by house */
	private Screen[] screens;

	/** map from screen name -> screen object */
	private Map<String, Screen> screenMap;

	
	/**
	 * @param xml
	 *            xml object
	 * @throws ModelError
	 */
	public XmlHouse(XmlObject xml) throws ModelError
	{
		name = xml.getValue("name");
		List<XmlObject> sxmls = xml.getChild("screens").getChildren();
		screens = new Screen[sxmls.size()];
		screenMap = new HashMap<String, Screen>();

		for (int i = 0; i < screens.length; i++)
		{
			screens[i] = new Screen(sxmls.get(i));
			screenMap.put(screens[i].getName(), screens[i]);
		}
	}


	/**
	 * Start a new XML game.
	 * 
	 * @param session
	 *            session game is served by
	 * @param player
	 *            connection to AI server
	 * @param log 
	 * @return game object
	 */
	public XmlGame startGame(PokerSession session, Player player, LogServer ls, int log)
	{
		// FIXME: HACK ALERT: specify control class some other way
		prepareScreensCopy();
		XmlGame game = new PokerroomGame(this, session, player, ls, log);
		game.setData(createDataset());
		return game;
	}

	/**
	 * Start a new XML game which will be used for static operation and tests
	 * (it is not served by a session).
	 * 
	 * @return game object
	 */
	public XmlGame startNullGame()
	{
		// FIXME: HACK ALERT: specify control class some other way
		prepareScreensCopy();
		XmlGame game = new PokerroomGame(this, null, null, null, -1);
		game.setData(createDataset());
		return game;
	}
	
	private void prepareScreensCopy()
	{
		for (int i = 0; i < screens.length; i++)
			screens[i] = screens[i].copy();
		screenMap.clear();
		for (Screen s : screens)
			screenMap.put(s.getName(), s);
	}
	
	private DataModel createDataset()
	{
		DataModel model = new DataModel();
		for (Screen s : screens)
			model.addScreenData(s.getName(), s.createDataObject());
		return model;
	}

	public Screen getScreen(String name)
	{
		if (!screenMap.containsKey(name))
			System.err.printf("no table %s in schema %s\n", name, this.name);
		Screen s = screenMap.get(name);
		if (s == null)
			System.err.printf("table %s in schema %s is null\n", name, this.name);
		return s;
	}

}
