
package poker.util.xml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;


public class XmlSchema
{

	public static final int		ONE		= 1;
	public static final int		PLUS	= 2;
	public static final int		ANY		= 3;
	public static final int		MAYBE	= 4;

	private Map<String, Object>	sub;
	private Map<String, Object>	smodes;
	private Map<String, Object>	attrs;
	private Map<String, Object>	amodes;
	private Map<String, Object>	defs;
	private String				rootTag;
	
	private static String[] s0 = new String[0];
	private static int[] i0 = new int[0];


	/**
	 * Constructor.
	 * 
	 * @param file
	 *            file to load schema from
	 * @throws XmlSchemaException
	 * @throws IOException
	 */
	public XmlSchema(File file) throws IOException, XmlSchemaException
	{
		sub = new HashMap<String, Object>();
		smodes = new HashMap<String, Object>();
		attrs = new HashMap<String, Object>();
		amodes = new HashMap<String, Object>();
		defs = new HashMap<String, Object>();

		process(file);
		makeArrays();
	}


	/**
	 * Process schema file.
	 * 
	 * @param file
	 *            file to load schema from
	 * @throws IOException
	 * @throws XmlSchemaException
	 */
	private void process(File file) throws IOException, XmlSchemaException
	{
		int tabs = -1;

		String line, tag = null;
		BufferedReader reader = new BufferedReader(new FileReader(file));
		Stack<String> tagStack = new Stack<String>();
		int lineno = 0;
		String last = null;

		while ((line = reader.readLine()) != null)
		{
			lineno++;
			int nt = 0;
			while (line.charAt(nt) == '\t')
				nt++;

			if (nt == (tabs + 1))
			{
				if (tag != null)
					tagStack.push(tag);
				tag = last;
				tabs = nt;
			}

			if (nt == tabs)
				last = processLine(tag, line, nt);
			else if (nt < tabs)
			{
				for (int i = nt; i < tabs; i++)
					tag = tagStack.pop();
				last = processLine(tag, line, nt);
				tabs = nt;
			}
			else
				throw new XmlSchemaException("too many tabs on line " + lineno);
		}
	}


	/**
	 * Process a tag-line.
	 * 
	 * @param parent
	 *            parent tag
	 * @param line
	 *            line containing tag text
	 * @param start
	 *            where tabs stop
	 * @return tag name
	 */
	private String processLine(String parent, String line, int start)
	{
		int idx, mode;
		boolean root = (start == 0);

		switch (line.charAt(start)) {
		case '*':
			mode = ANY;
			start++;
			break;

		case '?':
			mode = MAYBE;
			start++;
			break;

		case '+':
			mode = PLUS;
			start++;
			break;

		default:
			mode = ONE;
		}

		int end = line.indexOf(':');
		if (end == -1)
			end = line.length();

		String tag = line.substring(start, end);
		start = (end == line.length()) ? end : end + 1;

		addChild(parent, tag, mode);
		if (root)
			rootTag = tag;

		String rest = line.substring(start);
		String[] attrs = rest.split(";");

		for (String attr : attrs)
		{
			if (attr.equals(""))
				continue;

			int astart = 0, amode = ONE;
			if (attr.charAt(0) == '?')
			{
				astart = 1;
				amode = MAYBE;
			}

			int aend = attr.length();
			String def = null;
			if ((idx = attr.indexOf('=')) != -1)
			{
				aend = idx;
				amode = MAYBE;
				def = attr.substring(idx + 1);
			}

			addAttr(tag, attr.substring(astart, aend), amode, def);
		}

		return tag;
	}


	/**
	 * Convert maps of lists to maps of arrays.
	 */
	private void makeArrays()
	{
		for (String key : sub.keySet())
			sub.put(key, ((List<String>) sub.get(key)).toArray(s0));
		for (String key : attrs.keySet())
			attrs.put(key, ((List<String>) attrs.get(key)).toArray(s0));
		for (String key : defs.keySet())
			defs.put(key, ((List<String>) defs.get(key)).toArray(s0));
		for (String key : smodes.keySet())
		{
			List<Integer> list = (List<Integer>) smodes.get(key);
			int[] array = new int[list.size()];
			for (int i = 0; i < list.size(); i++)
				array[i] = list.get(i);
			smodes.put(key, array);
		}
		for (String key : amodes.keySet())
		{
			List<Integer> list = (List<Integer>) amodes.get(key);
			int[] array = new int[list.size()];
			for (int i = 0; i < list.size(); i++)
				array[i] = list.get(i);
			amodes.put(key, array);
		}
	}


	/**
	 * Add a parent-child relationship.
	 * 
	 * @param parent
	 * @param child
	 * @param mode
	 */
	private void addChild(String parent, String child, int mode)
	{
		if (!sub.containsKey(parent))
		{
			sub.put(parent, new ArrayList<String>());
			smodes.put(parent, new ArrayList<Integer>());
		}

		((List<String>) sub.get(parent)).add(child);
		((List<Integer>) smodes.get(parent)).add(mode);
	}


	/**
	 * Add a tag attribute.
	 * 
	 * @param tag
	 * @param attr
	 * @param mode
	 * @param def
	 */
	private void addAttr(String tag, String attr, int mode, String def)
	{
		if (!attrs.containsKey(tag))
		{
			attrs.put(tag, new ArrayList<String>());
			amodes.put(tag, new ArrayList<Integer>());
			defs.put(tag, new ArrayList<String>());
		}

		((List<String>) attrs.get(tag)).add(attr);
		((List<Integer>) amodes.get(tag)).add(mode);
		((List<String>) defs.get(tag)).add(def);
	}


	public String rootTag()
	{
		return rootTag;
	}


	public String[] getSubtags(String name)
	{
		return sub.containsKey(name) ? (String[]) sub.get(name) : s0;
	}


	public int[] getSubmodes(String name)
	{
		return smodes.containsKey(name) ? (int[]) smodes.get(name) : i0;
	}


	public String[] getAttributes(String name)
	{
		return attrs.containsKey(name) ? (String[]) attrs.get(name) : s0;
	}


	public int[] getAttributeModes(String name)
	{
		return amodes.containsKey(name) ? (int[]) amodes.get(name) : i0;
	}


	public String[] getDefaults(String name)
	{
		return defs.containsKey(name) ? (String[]) defs.get(name) : s0;
	}

}
