package poker.server.session.model.data;

import java.util.HashMap;
import java.util.Map;

import poker.util.xml.XmlObject;

public class ListItem
{
	
	private List list;
	private XmlObject xml;
	private Map<String, Field> fields;
	private String idField;

	public ListItem(List list, XmlObject xml)
	{
		this.list = list;
		this.xml = xml;
		
		fields = new HashMap<String, Field>();
		for (XmlObject f : xml.getChildren())
		{
			Field field = new Field(f);
			fields.put(field.getName(), field);
			if (field.isIdField())
				idField = field.getName();
		}
	}

	
	public Field getField(String targetName)
	{
		Field f = fields.get(targetName);
		if (f == null)
			System.err.printf("No such list field: %s\n", targetName);
		return f;
	}

	/**
	 * @return whether the id field is empty
	 */
	public boolean isBlank()
	{
		return fields.get(idField).isBlank();
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for (String key : fields.keySet())
			sb.append(key+"=>"+combine(fields.get(key).getRaw())+", ");
		sb.setLength(sb.length() - 2);
		sb.append("}");
		return sb.toString();
	}
	
	private static String combine(String[] strs)
	{
		if (strs == null)
			return "<null>";
		
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for (String s : strs)
			sb.append(s + ", ");
		sb.setLength(sb.length()-2);
		sb.append(")");
		return sb.toString();
	}
}
