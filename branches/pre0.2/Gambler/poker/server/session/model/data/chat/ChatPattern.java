
package poker.server.session.model.data.chat;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A Chat Pattern matches against some text and recursively matches sub-patterns
 * by choosing the group between '|' which is not null, and building a tree of
 * values returned by assigning matches to variable names and patterns to
 * 'function' names. The root pattern is the 'chat' pattern, and the 'function'
 * name is the name of an event that will fire on the xmlgame object.
 * 
 * @author lowentropy
 */
public class ChatPattern
{

	public String			name;
	public String			pattern;
	private int[]			bases;
	private String[]		values;
	private String[][]		args;
	private ChatPattern[][]	subpats;
	private String[][]		targets;
	public int				numGroups;
	private Pattern			regex;


	public ChatPattern(String section, int numGroups, String pattern,
			int[] bases, String[] values, String[][] args,
			ChatPattern[][] subpats, String[][] targets)
	{
		this.name = section;
		this.numGroups = numGroups;
		this.pattern = pattern;
		this.bases = bases;
		this.values = values;
		this.args = args;
		this.subpats = subpats;
		this.targets = targets;
		this.regex = Pattern.compile(pattern);
	}


	/**
	 * Match the given text against the chat format and invoke the given
	 * handler.
	 * 
	 * @param text
	 *            text to match against
	 * @param handler
	 *            handler to invoke
	 */
	public boolean match(String text, ChatEventHandler handler)
	{
		ChatValue val = match(text);

		if (val == null)
		{
			// handler.handleInvalid(text);
			return false;
		}
		else
		{
			handler.handleEvent(val);
			return true;
		}
	}


	/**
	 * Match against text, returning tree value.
	 * 
	 * @param text
	 *            text to match against
	 * @return chat value
	 */
	private ChatValue match(String text)
	{
		Matcher m = regex.matcher(text);
		if (!m.matches())
			return null;

		// System.out.println("root matched text: " + text); // DBG

		Map<String, ChatValue> map = new HashMap<String, ChatValue>();

		for (int i = 0; i < bases.length; i++)
		{
			int base = bases[i];
			String g = m.group(base);
			if (g == null)
				continue;

			// System.out.printf("group %d of %s matched\n", i, name); // DBG

			for (int j = 0; j < subpats[i].length; j++)
			{
				base++;
				if (subpats[i][j] == null && targets[i][j] != null)
				{
					// System.out.printf(
					// "(%d) matching special text '%s' to target %s\n",
					// j, g, targets[i][j]); // DBG
					map.put(targets[i][j], new ChatValue(m.group(base)));
				}
				else if (subpats[i][j] != null)
				{
					// System.out.printf(
					// "(%d) trying to match subpattern %s against '%s'\n",
					// j, subpats[i][j].name, g); // DBG
					if (targets[i][j] != null)
					{
						ChatValue val = subpats[i][j].match(m.group(base));
						map.put(targets[i][j], val);
					}
					base += subpats[i][j].numGroups;
				}
				// else
				// System.out.printf("(%d) empty group\n", j); // DBG
			}

			// System.out.printf("args length: %d\n", args[i].length); // DBG
			ChatValue[] params = new ChatValue[args[i].length];
			for (int j = 0; j < params.length; j++)
			{
				params[j] = map.get(args[i][j]);
				// System.out.printf("arg %d: %s = %s\n", j, args[i][j],
				// params[j]); // DBG
				if (params[j] == null)
					params[j] = new ChatValue(args[i][j]); // constant
			}

			return new ChatValue(values[i], params);
		}

		// never reached
		return null;
	}
}
