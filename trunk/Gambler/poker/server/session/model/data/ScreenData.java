
package poker.server.session.model.data;

import java.util.HashMap;
import java.util.Map;

import poker.server.session.house.impl.XmlGame;
import poker.server.session.model.ModelError;
import poker.server.session.model.data.chat.Chat;
import poker.server.session.model.data.chat.ChatEventHandler;
import poker.util.xml.XmlObject;


/**
 * Data container model for screen data, and also the data collector class.
 * 
 * TODO: split this class with a new ModelData class. screens create their
 * screendata's, but the xmlhouse can instantiate modeldata objects which contain
 * fields from all screens. the game has the modeldata set. the modeldata also
 * separates all fields by the active screen, so it has an internal hashtable.
 * 
 * @author lowentropy
 */
public class ScreenData
{

	/** map of field name -> field */
	private Map<String, Field>	fields;

	/** map of list name -> list */
	private Map<String, List>	lists;

	/** map of chat name -> chat */
	private Map<String, Chat>	chats;

	/** game which contains this data */
	private XmlGame game;


	/**
	 * Constructor.
	 * 
	 * @param xml
	 *            xml object
	 * @throws ModelError
	 */
	public ScreenData(XmlObject xml) throws ModelError
	{
		fields = new HashMap<String, Field>();
		lists = new HashMap<String, List>();
		chats = new HashMap<String, Chat>();

		for (XmlObject c : xml.getChildren())
		{
			if (c.getTag().equals("field"))
			{
				Field f = new Field(c);
				fields.put(f.getName(), f);
			}
			else if (c.getTag().equals("list"))
			{
				List l = new List(c);
				lists.put(l.getName(), l);
			}
			else if (c.getTag().equals("chat"))
			{
				Chat chat = new Chat(c);
				chats.put(chat.getName(), chat);
			}
			else
				throw new ModelError("invalid data element: " + c.getTag());
		}
	}


	/**
	 * Get the field of the given name.
	 * 
	 * @param name
	 *            name of field
	 * @return field object
	 */
	public Field getField(String name)
	{
		Field f = fields.get(name);
		if (f == null)
			System.err.printf("No such field: %s\n", name);
		return f;
	}


	/**
	 * Get the list of the given name
	 * 
	 * @param name
	 *            name of list
	 * @return list object
	 */
	public List getList(String name)
	{
		return lists.get(name);
	}


	

	public Object getTarget(String target)
	{
		if (chats.containsKey(target))
			return chats.get(target);
		else if (fields.containsKey(target))
			return fields.get(target);
		else
			return null;
	}


	public ChatEventHandler getChatEventHandler()
	{
		return game;
	}


	public void setGame(XmlGame game)
	{
		this.game = game;
	}


	public void clear()
	{
		// clear chats
		for (Chat c : chats.values())
			c.clear();
	}

}
