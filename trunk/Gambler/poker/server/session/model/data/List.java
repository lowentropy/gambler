package poker.server.session.model.data;

import java.util.ArrayList;

import poker.server.session.house.impl.XmlGame;
import poker.util.xml.XmlObject;


public class List
{

	private XmlObject xml;
	private String name;
	private ArrayList<ListItem> items;
	
	public List(XmlObject xml)
	{
		this.xml = xml;
		name = xml.getValue("name");
		items = new ArrayList<ListItem>();
		
	}

	public String getName()
	{
		return name;
	}

	
	public ListItem getItem(int idx)
	{
		while (idx >= items.size())
			addNullItem();
		return items.get(idx);
	}

	public void setNumRows(int i)
	{
		while (i < items.size())
			items.remove(items.size() - 1);
		while (i > items.size())
			addNullItem();
	}

	private void addNullItem()
	{
		items.add(new ListItem(this, xml));
	}

	public int getNumRows()
	{
		return items.size();
	}

	public void print()
	{
		System.out.printf("Items of list '%s':\n", this.name);
		for (ListItem item : items)
			System.out.printf("\t%s\n", item.toString());
	}

}
