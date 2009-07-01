
package poker.server.session.model.data.chat;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import poker.ai.bnet.PokerNet;


/**
 * The ChatFormat class interprets a stream of characters according to a
 * regular-expression-like patterns. Each possible pattern match corresponds to
 * an event, which is fired on the xmlgame object with parameters collected from
 * the expression.
 * 
 * @author lowentropy
 */
public class ChatFormat
{

	private static String				cardSpec;

	private static String				moneySpec;

	private static String				numSpec;

	private static String				anySpec;

	/** filename of chat patterns description */
	private String						fname;

	/** map of pattern name -> pattern */
	private Map<String, ChatPattern>	patterns;

	/** root chat-pattern object */
	private ChatPattern					root;

	static
	{
		initSpec();
	}


	/**
	 * Constructor.
	 * 
	 * @param chatFname
	 *            filename of patterns
	 * @throws IOException
	 */
	public ChatFormat(String chatFname) throws IOException
	{
		fname = chatFname;
		patterns = new HashMap<String, ChatPattern>();

		read();
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
		return root.match(removeWhitespace(text), handler);
	}


	/**
	 * Read the chat format from a file description.
	 * 
	 * @throws IOException
	 */
	private void read() throws IOException
	{
		int idx;
		String line;

		String section = null;
		List<ChatItem> items = null;

		BufferedReader reader = new BufferedReader(new FileReader(fname));
		List<String> inOrder = new ArrayList<String>();
		Map<String, List<ChatItem>> sections = new HashMap<String, List<ChatItem>>();
		Map<String, String> specials = new HashMap<String, String>();

		while ((line = reader.readLine()) != null)
		{
			line = line.trim();
			if (line.length() == 0)
				continue;

			if (line.startsWith("~~"))
			{
				if (section != null)
				{
					sections.put(section, items);
					inOrder.add(section);
				}
				items = new ArrayList<ChatItem>();
				section = line.substring(2).trim();
				if ((idx = section.indexOf(':')) != -1)
				{
					specials.put(section.substring(0, idx),
							section.substring(idx + 1));
					section = null;
				}
			}
			else
			{
				items.add(parseItem(line, specials));
			}
		}

		if (section != null)
		{
			sections.put(section, items);
			inOrder.add(section);
		}

		if (!sections.containsKey("chat"))
			throw new IOException("chat section required");

		for (int i = inOrder.size() - 1; i >= 0; i--)
		{
			section = inOrder.get(i);
			items = sections.get(section);
			makePattern(section, items);
		}

		root = getPattern("chat");
	}


	/**
	 * Parse a chat item line, consisting of patterns, value name and arguments.
	 * 
	 * @param line
	 *            line of text
	 * @param specials
	 *            map of special patterns
	 * @return chat item object
	 * @throws IOException
	 */
	private ChatItem parseItem(String line, Map<String, String> specials)
			throws IOException
	{
		int idx = line.indexOf("~");
		if (idx == -1)
			throw new IOException("no ~ in item of chat group, line follows:\n"
					+ line + "\n");

		String pat = line.substring(0, idx).trim();
		String val = line.substring(idx + 1).trim();
		List<ChatNode> nodes = new ArrayList<ChatNode>();

		int base = 0;
		while (base < pat.length())
		{
			idx = pat.indexOf('<', base);
			while (idx > 0 && pat.charAt(idx - 1) == '\\')
			{
				String tmp = pat.substring(0, idx - 1) + pat.substring(idx);
				idx = pat.indexOf('<', idx + 1) - 1;
				pat = tmp;
			}

			if (idx == -1)
				idx = pat.length();

			// text
			if (base != idx)
			{
				nodes.add(new ChatNode(null, null, null, regexEscape(pat.substring(base,
						idx))));
				base = idx;
			}
			// childnode
			else if (base != pat.length())
			{
				int idx2 = pat.indexOf('>', idx);
				String chld = pat.substring(idx + 1, idx2);
				base = idx2 + 1;

				idx2 = chld.indexOf(':');
				if (idx2 == -1)
					idx2 = chld.length();

				String cname = chld.substring(0, idx2);
				String tgt = (idx2 == chld.length()) ? null : chld.substring(
						idx2 + 1, chld.length());

				if (specials.containsKey(cname))
					nodes.add(new ChatNode(specials.get(cname), tgt, null, null));
				else
					nodes.add(new ChatNode(null, tgt, cname, null));
			}
		}

		String valn;
		idx = val.indexOf('(');
		List<String> args = new ArrayList<String>();
		if (idx != -1)
		{
			valn = val.substring(0, idx);
			for (String s : val.substring(idx + 1, val.length() - 1).split(","))
			{
				String t = s.trim();
				if (t.length() > 0)
					args.add(t);
			}
		}
		else
			valn = val;

		return new ChatItem(nodes, valn, args);
	}


	private static String regexEscape(String s)
	{
		return Pattern.quote(s);
	}


	/**
	 * Given the section name and the items of that section, create the regular
	 * expression for the section name.
	 * 
	 * @param section
	 *            name of section (not chat)
	 * @param items
	 *            items in section
	 * @throws IOException
	 */
	private void makePattern(String section, List<ChatItem> items)
			throws IOException
	{
		String[] values = new String[items.size()];
		String[][] targets = new String[items.size()][];
		int[] bases = new int[items.size()];
		ChatPattern[][] subpats = new ChatPattern[items.size()][];
		String[][] args = new String[items.size()][];
		StringBuilder sb = new StringBuilder();

		int N = 1;

		// System.out.printf("constructing pattern %s\n", section); // DBG

		for (int i = 0; i < items.size(); i++)
		{
			// System.out.printf("processing item %d\n", i); // DBG

			ChatItem item = items.get(i);
			values[i] = item.value;

			bases[i] = N;
			sb.append("(");

			int numGrps = 0;
			for (ChatNode node : item.nodes)
				if (node.text == null)
					numGrps++;

			// System.out.printf("\tnum groups: %d\n", numGrps); // DBG

			subpats[i] = new ChatPattern[numGrps];
			targets[i] = new String[numGrps];
			args[i] = item.args.toArray(new String[0]);

			int j = 0;
			for (ChatNode node : item.nodes)
			{
				if (node.text != null)
				{
					sb.append(removeWhitespace(node.text));
					// System.out.printf("\tadded text node: %s\n", node.text);
					// // DBG
				}

				else if (node.specName != null)
				{
					// System.out.printf("\tadded special node: %s\n",
					// node.specName); // DBG
					// if (node.target != null)
					// System.out.printf("\ttarget for last node: %s\n",
					// node.target); // DBG

					sb.append("(");
					sb.append(specText(node.specName));
					sb.append(")");

					subpats[i][j] = null;
					targets[i][j] = node.target;

					j++;
					N++;
				}

				else
				{
					ChatPattern subpat = getPattern(node.pattern);
					if (subpat == null)
						throw new IOException("can't find pattern: "
								+ node.pattern);

					// System.out.printf("\tadded pattern node: %s\n",
					// node.pattern); // DBG
					// if (node.target != null)
					// System.out.printf("\ttarget for last node: %s\n",
					// node.target); // DBG

					sb.append("(");
					sb.append(subpat.pattern);
					sb.append(")");

					subpats[i][j] = subpat;
					targets[i][j] = node.target;

					j++;
					N += (subpat.numGroups + 1);
				}
			}

			sb.append(")|");
			N++;
		}

		sb.setLength(sb.length() - 1);

		int numGroups = N - 1;
		String pattern = sb.toString();
		ChatPattern pat = new ChatPattern(section, numGroups, pattern, bases,
				values, args, subpats, targets);
		addPattern(pat);
	}


	/**
	 * Add a global pattern.
	 * 
	 * @param pat
	 *            pattern to add
	 */
	private void addPattern(ChatPattern pat)
	{
		patterns.put(pat.name, pat);
	}


	/**
	 * Get a global pattern by name.
	 * 
	 * @param pattern
	 *            name of pattern
	 * @return pattern object
	 */
	private ChatPattern getPattern(String pattern)
	{
		return patterns.get(pattern);
	}


	/**
	 * Return the special-type regex text for the given type.
	 * 
	 * @param specType
	 *            type of special regex: *, money, card, or number
	 * @return special text regex
	 * @throws IOException
	 */
	private String specText(String specType) throws IOException
	{
		if (specType.equals("*"))
			return anySpec;
		else if (specType.equals("money"))
			return moneySpec;
		else if (specType.equals("card"))
			return cardSpec;
		else if (specType.equals("number"))
			return numSpec;
		else
			return specType;
	}


	/**
	 * Initialize the special-regex strings.
	 */
	private static void initSpec()
	{
		anySpec = ".+";
		moneySpec = "\\$[0-9]+\\.?[0-9]?[0-9]?";
		numSpec = "[0-9]+";

		StringBuilder sb = new StringBuilder();
		for (String c : PokerNet.cards)
			sb.append(c + "|");
		sb.append("10c|10d|10h|10s");
		//sb.setLength(sb.length() - 1);

		cardSpec = sb.toString();
	}


	/**
	 * Remove whitespace from a string.
	 * 
	 * @param text
	 *            input text
	 * @return text with whitespace removed
	 */
	private static String removeWhitespace(String text)
	{
		char[] chars = new char[text.length()];
		text.getChars(0, text.length(), chars, 0);

		StringBuilder sb = new StringBuilder();
		for (char c : chars)
			if (c != ' ' && c != '\t' && c != '\r' && c != '\n')
				sb.append(c);
		return sb.toString();
	}

}
