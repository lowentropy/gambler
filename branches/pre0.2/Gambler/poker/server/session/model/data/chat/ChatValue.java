
package poker.server.session.model.data.chat;

import java.util.ArrayList;
import java.util.List;


public class ChatValue
{

	/** value 'function' name */
	public String		function;

	/** arguments of 'function' */
	public ChatValue[]	args;


	/**
	 * Constructor.
	 * 
	 * @param function
	 * @param args
	 */
	public ChatValue(String function, ChatValue[] args)
	{
		this.function = function;
		this.args = args;
	}


	public ChatValue(String function)
	{
		this(function, new ChatValue[0]);
	}


	public static ChatValue parse(String tree)
	{
		int idx = tree.indexOf('(');
		if (idx == -1)
			return new ChatValue(tree);

		String func = tree.substring(0, idx);
		String inner = tree.substring(idx + 1, tree.length() - 1);

		List<String> argss = split(inner);
		ChatValue[] args = new ChatValue[argss.size()];

		for (int i = 0; i < argss.size(); i++)
			args[i] = ChatValue.parse(argss.get(i));

		return new ChatValue(func, args);
	}


	private static List<String> split(String inner)
	{
		List<String> parts = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();

		int depth = 0;
		boolean esc = false;
		for (int i = 0; i < inner.length(); i++)
		{
			char c = inner.charAt(i);
			if (c == '\\')
			{
				if (esc)
				{
					sb.append(c);
					esc = false;
				}
				else
					esc = true;
			}
			else if (c == ',')
			{
				if (!esc && depth == 0)
				{
					parts.add(sb.toString());
					sb.setLength(0);
				}
				else
					sb.append(c);
				esc = false;
			}
			else
			{
				sb.append(c);
				if (c == '(')
					depth++;
				else if (c == ')')
					depth--;
				esc = false;
			}
		}
		parts.add(sb.toString());
		return parts;
	}


	public boolean equals(Object o)
	{
		if ((o == null) || !(o instanceof ChatValue))
			return false;

		ChatValue v = (ChatValue) o;

		if (!function.equals(v.function))
			return false;

		if (args.length != v.args.length)
			return false;

		for (int i = 0; i < args.length; i++)
			if (!args[i].equals(v.args[i]))
				return false;

		return true;
	}


	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(function);
		if (args.length > 0)
		{
			sb.append("(");
			for (ChatValue v : args)
			{
				sb.append(v.toString());
				sb.append(",");
			}
			sb.setLength(sb.length() - 1);
			sb.append(")");
		}
		return sb.toString();
	}
}
