
package poker.server.session.house.impl;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import poker.server.base.Player;
import poker.server.log.LogServer;
import poker.server.log.LogSession;
import poker.server.session.PokerSession;
import poker.server.session.house.Game;
import poker.server.session.house.House;
import poker.server.session.model.ModelError;
import poker.util.xml.XmlReader;
import poker.util.xml.XmlReaderException;
import poker.util.xml.XmlSchema;
import poker.util.xml.XmlSchemaException;


public class XmlHouseLoader implements House
{

	private String					housesLocation;

	private Map<String, XmlHouse>	houses;

	private XmlSchema				schema;


	public XmlHouseLoader() throws IOException, XmlSchemaException
	{
		houses = new HashMap<String, XmlHouse>();
		housesLocation = "houses/";
		schema = new XmlSchema(new File("conf/xml-house-schema"));
	}


	public Game startGame(String houseName, PokerSession session, Player player, LogServer ls, int log)
	{
		return getHouse(houseName).startGame(session, player, ls, log);
	}
	
	public XmlHouse getHouse(String houseName)
	{
		if (!houses.containsKey(houseName))
		{
			File f = new File(housesLocation + houseName + ".xml");
			if (f.exists())
				try
				{
					houses.put(houseName, new XmlHouse(XmlReader.read(f, schema)));
				}
				catch (ModelError e)
				{
					e.printStackTrace();
				}
				catch (XmlReaderException e)
				{
					e.printStackTrace();
				}
		}
		return houses.get(houseName);
	}

}
