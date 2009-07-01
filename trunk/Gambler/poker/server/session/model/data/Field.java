
package poker.server.session.model.data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import poker.util.xml.XmlObject;


/**
 * A field is a container of either any text scraped from the screen, or a
 * choice between possible states. In either case, the values the field can take
 * on are masked from the input text into extracted variables.
 * 
 * @author lowentropy
 */
public class Field
{

	/** name of field */
	private String		name;

	/** type of field; currently, only 'text' and 'state' are supported */
	private String		type;

	/**
	 * field mask: * is multi-char wildcard, \N maps preceding match to variable
	 * N, and | is a state separator.
	 */
	private String		mask;

	/** the possible states of the field */
	private String[]	states;

	/** the current state of the field */
	private int			state;

	/** the raw text which has been matched against the field */
	private String		text;

	/** the variables extracted from the last text match */
	private String[]	variables;

	/** regex pattern for mask */
	private Pattern		pattern;

	private boolean		idField;

	private String[]	raw;

	private boolean	modified;


	/**
	 * Constructor.
	 * 
	 * @param xml
	 *            xml object
	 */
	public Field(XmlObject xml)
	{
		name = xml.getValue("name");
		type = xml.getValue("type");
		mask = xml.getValue("mask");
		pattern = Pattern.compile(mask);
		idField = Boolean.parseBoolean(xml.getValue("id"));

		if (type.equals("state"))
			states = mask.split("\\|");
	}


	/**
	 * Match field against text scraped from screen.
	 * 
	 * @param text
	 *            text to match
	 */
	private boolean match(String text)
	{
		String ot = this.text;
		this.text = text;
		
		if (text == null)
			return false;

		if (type.equals("state"))
		{
			state = -1;
			for (int i = 0; i < states.length; i++)
				if (states[i].equals(text))
				{
					state = i;
					break;
				}
			if (state == -1)
				return false;
			// TODO: modified = ?
			return true;
		}
		else
		{
			if (text.length() == 0)
				return false;
			Matcher m = pattern.matcher(text);
			if (m.matches())
			{
				variables = new String[m.groupCount()];
				for (int i = 0; i < variables.length; i++)
					variables[i] = m.group(i + 1);
				modified = !seq(ot, text);
				return true;
			}
			else
				return false;
		}
	}


	private boolean seq(String t1, String t2)
	{
		if (t1 == t2)
			return true;
		if (t1 == null || t2 == null)
			return false;
		return t1.equals(t2);
	}


	public boolean match(String[] text)
	{
		raw = text;
		
		if (text.length == 1)
			return match(text[0]);

		String ot = this.text;
		this.text = "";
		boolean v = true;
		boolean a = false;
		for (String t : text)
		{
			if (t == null)
			{
				v = false;
				break;
			}
			if (t.length() > 0)
				a = true;
			this.text += t + '\n';
		}
		
		if (!a)
			v = false;
		
		if (v)
			modified = !seq(this.text, ot);
		
		return v;
	}


	/**
	 * @return field name
	 */
	public String getName()
	{
		return name;
	}


	/**
	 * @return raw match text
	 */
	public String getValue()
	{
		return text;
	}


	public void setFrom(Field data)
	{
		this.text = data.text;
		this.state = data.state;
		this.variables = (data.variables == null) ? null : data.variables.clone();
	}


	public boolean isIdField()
	{
		return this.idField;
	}


	public boolean isBlank()
	{
		return (text == null) || (text.trim().length() == 0);
	}

	public String[] getRaw()
	{
		return raw;
	}


	public boolean wasModified()
	{
		return modified;
	}
}
